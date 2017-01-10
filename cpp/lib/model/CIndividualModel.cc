/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */

#include <model/CIndividualModel.h>

#include <core/CAllocationStrategy.h>
#include <core/CContainerPrinter.h>
#include <core/CFunctional.h>
#include <core/CLogger.h>
#include <core/CStatistics.h>
#include <core/RestoreMacros.h>

#include <maths/CChecksum.h>
#include <maths/CMultivariatePrior.h>
#include <maths/COrderings.h>
#include <maths/CPrior.h>
#include <maths/CTimeSeriesDecomposition.h>

#include <model/CAnnotatedProbabilityBuilder.h>
#include <model/CDataGatherer.h>
#include <model/CModelDetailsView.h>
#include <model/CResourceMonitor.h>
#include <model/CModelTools.h>
#include <model/FrequencyPredicates.h>

#include <algorithm>
#include <map>

namespace ml
{
namespace model
{

namespace
{

typedef boost::reference_wrapper<const std::string> TStrCRef;
typedef std::map<TStrCRef, uint64_t, maths::COrderings::SLess> TStrCRefUInt64Map;
typedef std::pair<TStrCRef, TStrCRef> TStrCRefStrCRefPr;
typedef std::map<TStrCRefStrCRefPr, uint64_t, maths::COrderings::SLess> TStrCRefStrCRefPrUInt64Map;

//! \brief Orders values by decreasing magnitude.
struct SAbsGreater
{
    bool operator()(double lhs, double rhs) const
    {
        return ::fabs(lhs) > ::fabs(rhs);
    }
};

//! Check we have a consistent set of new priors and features.
template<typename U, typename V>
bool checkNewPriors(const U &features, const V &newPriors)
{
    if (newPriors.size() > features.size())
    {
        LOG_ERROR("Unexpected new priors: # number features = " << features.size()
                  << ", # new priors = " << newPriors.size());
        return false;
    }
    return true;
}

//! Check we have a consistent set of new priors and priors.
template<typename U, typename V>
bool checkPriors(const U &newPriors, const V &priors)
{
    if (newPriors.size() != priors.size())
    {
        LOG_ERROR("Inconsistent priors: # new priors = " << newPriors.size()
                  << ", # number priors = " << priors.size());
        return false;
    }
    return true;
}

//! Get the new prior for \p feature if it exists.
template<typename RESULT, typename PRIORS>
const RESULT *newPriors(const PRIORS &priors, model_t::EFeature feature)
{
    typename PRIORS::const_iterator result = std::lower_bound(priors.begin(),
                                                              priors.end(),
                                                              feature,
                                                              maths::COrderings::SFirstLess());
    return result == priors.end() ? 0 : result->second.get();
}

//! Hash the active person priors \p priors.
template<typename T>
static void hashPriors(const CDataGatherer &gatherer,
                       const T &priors,
                       TStrCRefUInt64Map &hashes)
{
    for (typename T::const_iterator i = priors.begin(); i != priors.end(); ++i)
    {
        for (std::size_t pid = 0u; pid < i->second.size(); ++pid)
        {
            if (gatherer.isPersonActive(pid))
            {
                uint64_t &hash = hashes[boost::cref(gatherer.personName(pid))];
                hash = maths::CChecksum::calculate(hash, i->second[pid]);
            }
        }
    }
}

//! Update \p hashes with the hash of the active people in \p values.
template<typename T>
void hashActive(const CDataGatherer &gatherer,
                const std::vector<T> &values,
                TStrCRefUInt64Map &hashes)
{
    for (std::size_t pid = 0u; pid < values.size(); ++pid)
    {
        if (gatherer.isPersonActive(pid))
        {
            uint64_t &hash = hashes[boost::cref(gatherer.personName(pid))];
            hash = maths::CChecksum::calculate(hash, values[pid]);
        }
    }
}

//! Update \p hashes with the hash of the active people in \p values.
template<typename T>
void hashActive(const CDataGatherer &gatherer,
                const std::vector<std::pair<model_t::EFeature, std::vector<T> > > &values,
                TStrCRefUInt64Map &hashes)
{
    for (std::size_t i = 0u; i < values.size(); ++i)
    {
        hashActive(gatherer, values[i].second, hashes);
    }
}

const std::size_t CHUNK_SIZE = 500u;

// We obfuscate the element names to avoid giving away too much
// information about our model.
const std::string WINDOW_BUCKET_COUNT_TAG("a");
const std::string PERSON_BUCKET_COUNT_TAG("b");
const std::string FIRST_BUCKET_TIME_TAG("c");
const std::string LAST_BUCKET_TIME_TAG("d");
const std::string NEW_FEATURE_PRIOR_TAG("e");
const std::string NEW_FEATURE_MULTIVARIATE_PRIOR_TAG("f");
const std::string FEATURE_PRIORS_TAG("g");
const std::string FEATURE_MULTIVARIATE_PRIORS_TAG("h");
const std::string FEATURE_CORRELATE_PRIORS_TAG("i");
const std::string FEATURE_CORRELATIONS_TAG("j");
const std::string EXTRA_DATA_TAG("k");
const std::string INTERIM_BUCKET_CORRECTOR_TAG("l");
const std::string FEATURE_DECOMPOSITION_TAG("m");
const std::string FEATURE_DECAY_RATE_CONTROLLER_TAG("n");
const std::string MEMORY_ESTIMATOR_TAG("r");

// Nested tags.
const std::string FEATURE_TAG("a");
const std::string VALUE_TAG("b");

//! Persist a collection of feature new priors.
template<typename F, typename T>
void persistNewPriors(const std::string &tag,
                      F persist,
                      const std::vector<std::pair<model_t::EFeature, T> > &priors,
                      core::CStatePersistInserter &inserter)
{
    for (std::size_t i = 0u; i < priors.size(); ++i)
    {
        if (!model_t::newPriorFixed(priors[i].first))
        {
            inserter.insertLevel(tag, boost::bind(persist,
                                                  boost::cref(FEATURE_TAG),
                                                  priors[i].first,
                                                  boost::cref(VALUE_TAG),
                                                  boost::cref(*priors[i].second), _1));
        }
    }
}

//! Persist a collection of feature priors.
template<typename F, typename T>
void persistPriors(const std::string &tag,
                   F persist,
                   const std::map<model_t::EFeature, T> &priors,
                   core::CStatePersistInserter &inserter)
{
    for (typename std::map<model_t::EFeature, T>::const_iterator i = priors.begin(); i != priors.end(); ++i)
    {
        inserter.insertLevel(tag, boost::bind(persist,
                                              boost::cref(FEATURE_TAG),
                                              i->first,
                                              boost::cref(VALUE_TAG),
                                              boost::cref(i->second), _1));
    }
}

//! Persist a collection of feature values.
template<typename F, typename T>
void persistValues(const std::string &tag,
                   F persist,
                   const std::vector<std::pair<model_t::EFeature, T> > &values,
                   core::CStatePersistInserter &inserter)
{
    for (std::size_t i = 0u; i < values.size(); ++i)
    {
        inserter.insertLevel(tag, boost::bind(persist,
                                              boost::cref(FEATURE_TAG),
                                              values[i].first,
                                              boost::cref(VALUE_TAG),
                                              boost::cref(values[i].second), _1));
    }
}

}

CIndividualModel::CIndividualModel(const SModelParams &params,
                                   const TDataGathererPtr &dataGatherer,
                                   const TFeaturePriorPtrPrVec &newPriors,
                                   const TFeatureMultivariatePriorPtrPrVec &newMultivariatePriors,
                                   const TFeatureMultivariatePriorPtrPrVec &newCorrelatePriors,
                                   const TFeatureInfluenceCalculatorCPtrPrVecVec &influenceCalculators,
                                   const TFeatureDecompositionCPtrVecPrVec &newDecompositions,
                                   bool isForRestore) :
        CModel(params, dataGatherer, influenceCalculators, isForRestore),
        m_NewDecompositions(newDecompositions)
{
    if (isForRestore)
    {
        CModelTools::shallowCopyConstantNewPriors(newPriors, m_NewFeaturePriors);
        CModelTools::shallowCopyConstantNewPriors(newMultivariatePriors, m_NewMultivariateFeaturePriors);
    }
    else
    {
        CModelTools::copyAllNewPriors(newPriors, m_NewFeaturePriors);
        CModelTools::copyAllNewPriors(newMultivariatePriors, m_NewMultivariateFeaturePriors);
    }
    if (this->params().s_MultivariateByFields)
    {
        m_NewCorrelateFeaturePriors = newCorrelatePriors;
        std::sort(m_NewCorrelateFeaturePriors.begin(),
                  m_NewCorrelateFeaturePriors.end(),
                  maths::COrderings::SFirstLess());
    }

    for (std::size_t i = 0u; i < m_NewFeaturePriors.size(); ++i)
    {
        model_t::EFeature feature = m_NewFeaturePriors[i].first;
        m_Priors.insert(std::make_pair(feature, TPriorPtrVec()));
    }
    for (std::size_t i = 0u; i < m_NewMultivariateFeaturePriors.size(); ++i)
    {
        model_t::EFeature feature = m_NewMultivariateFeaturePriors[i].first;
        m_MultivariatePriors.insert(std::make_pair(feature, TMultivariatePriorPtrVec()));
    }
    if (this->params().s_MultivariateByFields)
    {
        for (std::size_t i = 0u; i < m_NewCorrelateFeaturePriors.size(); ++i)
        {
            model_t::EFeature feature = m_NewCorrelateFeaturePriors[i].first;
            m_CorrelatePriors.insert(std::make_pair(feature, TSizeSizePrMultivariatePriorPtrDoublePrUMap()));
        }
    }
    std::sort(m_NewDecompositions.begin(), m_NewDecompositions.end(), maths::COrderings::SFirstLess());
    m_Decompositions.reserve(m_NewDecompositions.size());
    m_DecayRateControllers.reserve(m_NewDecompositions.size());
    for (std::size_t i = 0u; i < m_NewDecompositions.size(); ++i)
    {
        model_t::EFeature feature = m_NewDecompositions[i].first;
        m_Decompositions.push_back(TFeatureDecompositionPtr1VecVecPr(feature, TDecompositionPtr1VecVec()));
        if (this->params().s_ControlDecayRate && !model_t::isConstant(feature))
        {
            m_DecayRateControllers.push_back(
                    TFeatureDecayRateControllerArrayVecPr(feature, TDecayRateControllerArrayVec()));
        }
    }
}

CIndividualModel::CIndividualModel(bool isForPersistence, const CIndividualModel &other) :
        CModel(isForPersistence, other),
        m_FirstBucketTimes(other.m_FirstBucketTimes),
        m_LastBucketTimes(other.m_LastBucketTimes),
        m_DecayRateControllers(other.m_DecayRateControllers),
        m_MemoryEstimator(other.m_MemoryEstimator)
{
    if (!isForPersistence)
    {
        LOG_ABORT("This constructor only creates clones for persistence");
    }

    CModelTools::cloneVaryingNewPriors(other.m_NewFeaturePriors, m_NewFeaturePriors);
    CModelTools::cloneVaryingNewPriors(other.m_NewMultivariateFeaturePriors, m_NewMultivariateFeaturePriors);
    CModelTools::clonePriors(other.m_Priors, m_Priors);
    CModelTools::clonePriors(other.m_MultivariatePriors, m_MultivariatePriors);
    for (TFeatureSizeSizePrMultivariatePriorPtrDoublePrUMapMapCItr i = other.m_CorrelatePriors.begin();
         i != other.m_CorrelatePriors.end();
         ++i)
    {
        TSizeSizePrMultivariatePriorPtrDoublePrUMap &featureCorrelatedPeoplePriors = m_CorrelatePriors[i->first];
        for (TSizeSizePrMultivariatePriorPtrDoublePrUMapCItr j = i->second.begin(); j != i->second.end(); ++j)
        {
            TMultivariatePriorPtrDoublePr &prior = featureCorrelatedPeoplePriors[j->first];
            prior.first.reset(j->second.first->clone());
            prior.second = j->second.second;
        }
    }

    m_Decompositions.reserve(other.m_Decompositions.size());
    for (std::size_t i = 0u; i < other.m_Decompositions.size(); ++i)
    {
        model_t::EFeature feature = other.m_Decompositions[i].first;
        const TDecompositionPtr1VecVec &otherDecompositions = other.m_Decompositions[i].second;

        m_Decompositions.push_back(
                TFeatureDecompositionPtr1VecVecPr(feature, TDecompositionPtr1VecVec()));
        TDecompositionPtr1VecVec &decompositions = m_Decompositions.back().second;
        decompositions.reserve(otherDecompositions.size());

        for (std::size_t pid = 0u; pid < otherDecompositions.size(); ++pid)
        {
            decompositions.push_back(TDecompositionPtr1Vec());
            decompositions.back().reserve(otherDecompositions[pid].size());
            for (std::size_t j = 0u; j < otherDecompositions[pid].size(); ++j)
            {
                decompositions.back().push_back(
                        TDecompositionPtr(otherDecompositions[pid][j]->clone()));
            }
        }
    }
}

bool CIndividualModel::isPopulation(void) const
{
    return false;
}

CIndividualModel::TOptionalUInt64
   CIndividualModel::currentBucketCount(std::size_t pid, core_t::TTime time) const
{
    if (!this->bucketStatsAvailable(time))
    {
        LOG_ERROR("No statistics at " << time
                  << ", current bucket = " << this->printCurrentBucket());
        return TOptionalUInt64();
    }

    TSizeUInt64PrVecCItr result =
            std::lower_bound(this->currentBucketPersonCounts().begin(),
                             this->currentBucketPersonCounts().end(),
                             pid, maths::COrderings::SFirstLess());

    return result != this->currentBucketPersonCounts().end() && result->first == pid ?
           result->second : static_cast<uint64_t>(0);
}

bool CIndividualModel::bucketStatsAvailable(core_t::TTime time) const
{
    return   time >= this->currentBucketStartTime()
          && time < this->currentBucketStartTime() + this->bucketLength();
}

void CIndividualModel::sampleBucketStatistics(core_t::TTime startTime,
                                              core_t::TTime endTime,
                                              CResourceMonitor &resourceMonitor)
{
    CDataGatherer &gatherer = this->dataGatherer();

    if (!gatherer.dataAvailable(startTime))
    {
        return;
    }

    for (core_t::TTime time = startTime, bucketLength = gatherer.bucketLength();
         time < endTime;
         time += bucketLength)
    {
        this->CModel::sampleBucketStatistics(time, time + bucketLength, resourceMonitor);

        // Currently, we only remember one bucket.
        this->currentBucketStartTime(time);
        TSizeUInt64PrVec &personCounts = this->currentBucketPersonCounts();
        gatherer.personNonZeroCounts(time, personCounts);
        this->applyFilter(model_t::E_XF_By, false, this->personFilter(), personCounts);
    }
}

void CIndividualModel::sampleOutOfPhase(core_t::TTime startTime,
                                        core_t::TTime endTime,
                                        CResourceMonitor &resourceMonitor)
{
    CDataGatherer &gatherer = this->dataGatherer();
    if (!gatherer.dataAvailable(startTime))
    {
        return;
    }

    for (core_t::TTime time = startTime, bucketLength = gatherer.bucketLength();
         time < endTime;
         time += bucketLength)
    {
        gatherer.sampleNow(time);
        this->sampleBucketStatistics(time, time + bucketLength, resourceMonitor);
    }
}

void CIndividualModel::sample(core_t::TTime startTime,
                              core_t::TTime endTime,
                              CResourceMonitor &resourceMonitor)
{
    const CDataGatherer &gatherer = this->dataGatherer();

    for (core_t::TTime time = startTime, bucketLength = gatherer.bucketLength();
         time < endTime;
         time += bucketLength)
    {
        this->CModel::sample(time, time + bucketLength, resourceMonitor);

        this->currentBucketStartTime(time);
        TSizeUInt64PrVec &personCounts = this->currentBucketPersonCounts();
        gatherer.personNonZeroCounts(time, personCounts);
        for (std::size_t i = 0u; i < personCounts.size(); ++i)
        {
            std::size_t pid = personCounts[i].first;
            if (CModel::isTimeUnset(m_FirstBucketTimes[pid]))
            {
                m_FirstBucketTimes[pid] = time;
            }
            m_LastBucketTimes[pid] = time;
        }
        this->applyFilter(model_t::E_XF_By, true, this->personFilter(), personCounts);

        this->sampleCounts(time, time + bucketLength);

        // Refresh the new feature priors to account for this bucket.
        CModelTools::updateNewPriorsWithEmptyBucket(this->params(), m_NewFeaturePriors);
        CModelTools::updateNewPriorsWithEmptyBucket(this->params(), m_NewMultivariateFeaturePriors);
    }
}

void CIndividualModel::prune(std::size_t maximumAge)
{
    core_t::TTime time = this->currentBucketStartTime();

    if (time <= 0)
    {
        return;
    }

    CDataGatherer &gatherer = this->dataGatherer();

    TSizeVec peopleToRemove;
    for (std::size_t pid = 0u; pid < m_LastBucketTimes.size(); ++pid)
    {
        if (gatherer.isPersonActive(pid) && !CModel::isTimeUnset(m_LastBucketTimes[pid]))
        {
            std::size_t bucketsSinceLastEvent = static_cast<std::size_t>(
                    (time - m_LastBucketTimes[pid]) / gatherer.bucketLength());
            if (bucketsSinceLastEvent > maximumAge)
            {
                LOG_TRACE(gatherer.personName(pid)
                          << ", bucketsSinceLastEvent = " << bucketsSinceLastEvent
                          << ", maximumAge = " << maximumAge);
                peopleToRemove.push_back(pid);
            }
        }
    }

    if (peopleToRemove.empty())
    {
        return;
    }

    std::sort(peopleToRemove.begin(), peopleToRemove.end());
    LOG_DEBUG("Removing people {" << this->printPeople(peopleToRemove, 20) << '}');

    // We clear large state objects from removed people's model
    // and reinitialize it when they are recycled.
    this->clearPrunedResources(peopleToRemove, TSizeVec());
}

bool CIndividualModel::computeTotalProbability(const std::string &/*person*/,
                                               std::size_t /*numberAttributeProbabilities*/,
                                               TOptionalDouble &probability,
                                               TAttributeProbability1Vec &attributeProbabilities) const
{
    probability = TOptionalDouble();
    attributeProbabilities.clear();
    return true;
}

uint64_t CIndividualModel::checksum(bool includeCurrentBucketStats) const
{
    uint64_t seed = this->CModel::checksum(includeCurrentBucketStats);
    seed = maths::CChecksum::calculate(seed, m_NewFeaturePriors);
    seed = maths::CChecksum::calculate(seed, m_NewMultivariateFeaturePriors);
    seed = maths::CChecksum::calculate(seed, m_NewCorrelateFeaturePriors);
    seed = maths::CChecksum::calculate(seed, m_NewDecompositions);

    TStrCRefUInt64Map hashes1;
    const CDataGatherer &gatherer = this->dataGatherer();
    hashActive(gatherer, m_FirstBucketTimes, hashes1);
    hashActive(gatherer, m_LastBucketTimes, hashes1);
    hashPriors(gatherer, m_Priors, hashes1);
    hashPriors(gatherer, m_MultivariatePriors, hashes1);
    hashActive(gatherer, m_Decompositions, hashes1);
    hashActive(gatherer, m_DecayRateControllers, hashes1);

#define KEY(pid) boost::cref(this->personName(pid))

    TStrCRefStrCRefPrUInt64Map hashes2;
    for (TFeatureSizeSizePrMultivariatePriorPtrDoublePrUMapMapCItr i = m_CorrelatePriors.begin();
         i != m_CorrelatePriors.end();
         ++i)
    {
        for (TSizeSizePrMultivariatePriorPtrDoublePrUMapCItr j = i->second.begin();
             j != i->second.end();
             ++j)
        {
            std::size_t pid1 = j->first.first;
            std::size_t pid2 = j->first.second;
            if (gatherer.isPersonActive(pid1) && gatherer.isPersonActive(pid2))
            {
                uint64_t &hash = hashes2[std::make_pair(KEY(pid1), KEY(pid2))];
                hash = maths::CChecksum::calculate(hash, j->second);
            }
        }
    }
    if (includeCurrentBucketStats)
    {
        seed = maths::CChecksum::calculate(seed, this->currentBucketStartTime());
        const TSizeUInt64PrVec &personCounts = this->currentBucketPersonCounts();
        for (std::size_t i = 0u; i < personCounts.size(); ++i)
        {
            uint64_t &hash = hashes1[KEY(personCounts[i].first)];
            hash = maths::CChecksum::calculate(hash, personCounts[i].second);
        }
    }

#undef KEY

    LOG_TRACE("seed = " << seed);
    LOG_TRACE("hashes1 = " << core::CContainerPrinter::print(hashes1));
    LOG_TRACE("hashes2 = " << core::CContainerPrinter::print(hashes2));

    seed = maths::CChecksum::calculate(seed, hashes1);
    return maths::CChecksum::calculate(seed, hashes2);
}

void CIndividualModel::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CIndividualModel");
    this->CModel::debugMemoryUsage(mem->addChild());
    core::CMemoryDebug::dynamicSize("m_FirstBucketTimes", m_FirstBucketTimes, mem);
    core::CMemoryDebug::dynamicSize("m_LastBucketTimes", m_LastBucketTimes, mem);
    CModelTools::debugNewPriorsMemoryUsage("m_NewFeaturePriors", m_NewFeaturePriors, mem);
    CModelTools::debugNewPriorsMemoryUsage("m_NewMultivariateFeaturePriors", m_NewMultivariateFeaturePriors, mem);
    core::CMemoryDebug::dynamicSize("m_Priors", m_Priors, mem);
    core::CMemoryDebug::dynamicSize("m_MultivariatePriors", m_MultivariatePriors, mem);
    core::CMemoryDebug::dynamicSize("m_CorrelatePriors", m_CorrelatePriors, mem);
    core::CMemoryUsage::SMemoryUsage usage(  std::string("m_NewDecompositions::")
                                           + typeid(TFeatureDecompositionCPtrVecPr).name(),
                                           sizeof(TFeatureDecompositionCPtrVecPr) * m_NewDecompositions.capacity(),
                                           sizeof(TFeatureDecompositionCPtrVecPr) * (  m_NewDecompositions.capacity()
                                                                                     - m_NewDecompositions.size()));
    mem->addChild()->setName(usage);
    core::CMemoryDebug::dynamicSize("m_Decompositions", m_Decompositions, mem);
    core::CMemoryDebug::dynamicSize("m_DecayRateControllers", m_DecayRateControllers, mem);
    core::CMemoryDebug::dynamicSize("m_MemoryEstimator", m_MemoryEstimator, mem);
}

std::size_t CIndividualModel::memoryUsage(void) const
{
    const CDataGatherer &gatherer = this->dataGatherer();
    return this->estimateMemoryUsage(gatherer.numberActivePeople(),
                                     gatherer.numberActiveAttributes(),
                                     this->numberCorrelations());
}

std::size_t CIndividualModel::computeMemoryUsage(void) const
{
    std::size_t mem = this->CModel::memoryUsage();
    mem += core::CMemory::dynamicSize(m_FirstBucketTimes);
    mem += core::CMemory::dynamicSize(m_LastBucketTimes);
    mem += CModelTools::newPriorsMemoryUsage(m_NewFeaturePriors);
    mem += CModelTools::newPriorsMemoryUsage(m_NewMultivariateFeaturePriors);
    mem += core::CMemory::dynamicSize(m_Priors);
    mem += core::CMemory::dynamicSize(m_MultivariatePriors);
    mem += core::CMemory::dynamicSize(m_CorrelatePriors);
    mem += sizeof(TFeatureDecompositionCPtrVecPr) * m_NewDecompositions.capacity();
    mem += core::CMemory::dynamicSize(m_Decompositions);
    mem += core::CMemory::dynamicSize(m_DecayRateControllers);
    mem += core::CMemory::dynamicSize(m_MemoryEstimator);
    return mem;
}

std::size_t CIndividualModel::estimateMemoryUsage(std::size_t numberPeople,
                                                  std::size_t numberAttributes,
                                                  std::size_t numberCorrelations) const
{
    CMemoryUsageEstimator::TSizeArray predictors;
    predictors[CMemoryUsageEstimator::E_People]       = numberPeople;
    predictors[CMemoryUsageEstimator::E_Attributes]   = numberAttributes;
    predictors[CMemoryUsageEstimator::E_Correlations] = numberCorrelations;
    CMemoryUsageEstimator::TOptionalSize guess = m_MemoryEstimator.estimate(predictors);
    if (guess)
    {
        return guess.get();
    }
    else
    {
        std::size_t mem = this->computeMemoryUsage();
        m_MemoryEstimator.addValue(predictors, mem);
        return mem;
    }
}

std::size_t CIndividualModel::staticSize(void) const
{
    return sizeof(*this);
}

void CIndividualModel::doAcceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertValue(WINDOW_BUCKET_COUNT_TAG,
                         this->windowBucketCount(),
                         core::CIEEE754::E_SinglePrecision);
    core::CPersistUtils::persist(PERSON_BUCKET_COUNT_TAG, this->personBucketCounts(), inserter);
    core::CPersistUtils::persist(FIRST_BUCKET_TIME_TAG, m_FirstBucketTimes, inserter);
    core::CPersistUtils::persist(LAST_BUCKET_TIME_TAG, m_LastBucketTimes, inserter);
    persistNewPriors(NEW_FEATURE_PRIOR_TAG,
                     &featurePriorAcceptPersistInserter,
                     m_NewFeaturePriors, inserter);
    persistNewPriors(NEW_FEATURE_MULTIVARIATE_PRIOR_TAG,
                     &featureMultivariatePriorAcceptPersistInserter,
                     m_NewMultivariateFeaturePriors, inserter);
    persistPriors(FEATURE_PRIORS_TAG,
                  &featurePriorsAcceptPersistInserter,
                  m_Priors, inserter);
    persistPriors(FEATURE_MULTIVARIATE_PRIORS_TAG,
                  &featureMultivariatePriorsAcceptPersistInserter,
                  m_MultivariatePriors, inserter);
    persistPriors(FEATURE_CORRELATE_PRIORS_TAG,
                  &featureCorrelatePriorsAcceptPersistInserter,
                  m_CorrelatePriors, inserter);
    this->featureCorrelationsAcceptPersistInserter(FEATURE_CORRELATIONS_TAG, inserter);
    this->extraDataAcceptPersistInserter(EXTRA_DATA_TAG, inserter);
    this->interimBucketCorrectorAcceptPersistInserter(INTERIM_BUCKET_CORRECTOR_TAG, inserter);
    persistValues(FEATURE_DECOMPOSITION_TAG,
                  &featureDecompositionsAcceptPersistInserter,
                  m_Decompositions, inserter);
    persistValues(FEATURE_DECAY_RATE_CONTROLLER_TAG,
                  &featureControllersAcceptPersistInserter,
                  m_DecayRateControllers, inserter);
    core::CPersistUtils::persist(MEMORY_ESTIMATOR_TAG, m_MemoryEstimator, inserter);
}

bool CIndividualModel::doAcceptRestoreTraverser(const model_t::TAnyRestoreFunc &extraDataRestoreFunc,
                                                core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        RESTORE_SETUP_TEARDOWN(WINDOW_BUCKET_COUNT_TAG,
                               double count,
                               core::CStringUtils::stringToType(traverser.value(), count),
                               this->windowBucketCount(count))
        RESTORE(PERSON_BUCKET_COUNT_TAG, core::CPersistUtils::restore(name, this->personBucketCounts(), traverser))
        RESTORE(FIRST_BUCKET_TIME_TAG, core::CPersistUtils::restore(name, m_FirstBucketTimes, traverser))
        RESTORE(LAST_BUCKET_TIME_TAG, core::CPersistUtils::restore(name, m_LastBucketTimes, traverser))
        RESTORE(NEW_FEATURE_PRIOR_TAG,
                traverser.traverseSubLevel(boost::bind(&CIndividualModel::featurePriorAcceptRestoreTraverser,
                                                       this, this->isMetric() ? maths_t::E_ContinuousData :
                                                                                maths_t::E_IntegerData,
                                                       boost::cref(FEATURE_TAG),
                                                       boost::cref(VALUE_TAG),
                                                       _1, boost::ref(m_NewFeaturePriors))))
        RESTORE(NEW_FEATURE_MULTIVARIATE_PRIOR_TAG,
                traverser.traverseSubLevel(boost::bind(&CIndividualModel::featureMultivariatePriorAcceptRestoreTraverser,
                                                       this, this->isMetric() ? maths_t::E_ContinuousData :
                                                                                maths_t::E_IntegerData,
                                                       boost::cref(FEATURE_TAG),
                                                       boost::cref(VALUE_TAG),
                                                       _1, boost::ref(m_NewMultivariateFeaturePriors))))
        RESTORE(FEATURE_PRIORS_TAG,
                traverser.traverseSubLevel(boost::bind(&CIndividualModel::featurePriorsAcceptRestoreTraverser,
                                                       this, this->isMetric() ? maths_t::E_ContinuousData :
                                                                                maths_t::E_IntegerData,
                                                       boost::cref(FEATURE_TAG),
                                                       boost::cref(VALUE_TAG),
                                                       _1, boost::ref(m_Priors))))
        RESTORE(FEATURE_MULTIVARIATE_PRIORS_TAG,
                traverser.traverseSubLevel(boost::bind(&CIndividualModel::featureMultivariatePriorsAcceptRestoreTraverser,
                                                       this, this->isMetric() ? maths_t::E_ContinuousData :
                                                                                maths_t::E_IntegerData,
                                                       boost::cref(FEATURE_TAG),
                                                       boost::cref(VALUE_TAG),
                                                       _1, boost::ref(m_MultivariatePriors))))
        RESTORE(FEATURE_CORRELATE_PRIORS_TAG,
                traverser.traverseSubLevel(boost::bind(&CIndividualModel::featureCorrelatePriorsAcceptRestoreTraverser,
                                                       this, this->isMetric() ? maths_t::E_ContinuousData :
                                                                                maths_t::E_IntegerData,
                                                       boost::cref(FEATURE_TAG),
                                                       boost::cref(VALUE_TAG),
                                                       _1, boost::ref(m_CorrelatePriors))))
        RESTORE(FEATURE_CORRELATIONS_TAG, this->featureCorrelationsAcceptRestoreTraverser(traverser))
        RESTORE(EXTRA_DATA_TAG, this->extraDataAcceptRestoreTraverser(extraDataRestoreFunc, traverser))
        RESTORE(INTERIM_BUCKET_CORRECTOR_TAG, this->interimBucketCorrectorAcceptRestoreTraverser(traverser))
        RESTORE(FEATURE_DECOMPOSITION_TAG,
                traverser.traverseSubLevel(boost::bind(&CIndividualModel::featureDecompositionsAcceptRestoreTraverser,
                                                       this,
                                                       boost::cref(FEATURE_TAG),
                                                       boost::cref(VALUE_TAG),
                                                       _1, boost::ref(m_Decompositions))))
        RESTORE(FEATURE_DECAY_RATE_CONTROLLER_TAG,
                traverser.traverseSubLevel(boost::bind(&CIndividualModel::featureControllersAcceptRestoreTraverser,
                                                       this,
                                                       boost::cref(FEATURE_TAG),
                                                       boost::cref(VALUE_TAG),
                                                       _1, boost::ref(m_DecayRateControllers))))
        RESTORE(MEMORY_ESTIMATOR_TAG, core::CPersistUtils::restore(MEMORY_ESTIMATOR_TAG, m_MemoryEstimator, traverser))
    }
    while (traverser.next());

    for (TFeatureSizeSizePrMultivariatePriorPtrDoublePrUMapMapCItr i = m_CorrelatePriors.begin();
         i != m_CorrelatePriors.end();
         ++i)
    {
        this->refreshCorrelated(i->first, i->second);
    }

    // Sanity checks.
    const CDataGatherer &gatherer = this->dataGatherer();
    return    checkNewPriors(gatherer.features(), m_NewFeaturePriors)
           && checkNewPriors(gatherer.features(), m_NewMultivariateFeaturePriors)
           && checkPriors(m_NewFeaturePriors, m_Priors)
           && checkPriors(m_NewMultivariateFeaturePriors, m_MultivariatePriors);
}

void CIndividualModel::createUpdateNewModels(core_t::TTime time, CResourceMonitor &resourceMonitor)
{
    this->updateRecycledModels();

    CDataGatherer &gatherer = this->dataGatherer();

    std::size_t numberExistingPeople = m_FirstBucketTimes.size();
    std::size_t numberCorrelations = this->numberCorrelations();
    std::size_t ourUsage = this->estimateMemoryUsage(std::min(numberExistingPeople,
                                                              gatherer.numberActivePeople()),
                                                     0, // # attributes
                                                     numberCorrelations);
    std::size_t resourceLimit = ourUsage + resourceMonitor.allocationLimit();
    std::size_t numberNewPeople = gatherer.numberPeople();
    numberNewPeople = numberNewPeople > numberExistingPeople ?
                      numberNewPeople - numberExistingPeople : 0;

    if (resourceMonitor.areAllocationsAllowed())
    {
        // If there are fewer than 500 people to be created, and
        // allocations are allowed, then go ahead and create them.
        // For more than 500, create models in chunks and test
        // usage after each chunk.

        if (numberNewPeople > 0)
        {
            if (numberNewPeople < CHUNK_SIZE || resourceMonitor.haveNoLimit())
            {
                LOG_TRACE("Creating " << numberNewPeople << " new people");
                this->createNewModels(numberNewPeople, 0);
                numberNewPeople = 0;
            }
            else
            {
                while (numberNewPeople > 0 && ourUsage < resourceLimit)
                {
                    std::size_t numberToCreate = std::min(numberNewPeople, CHUNK_SIZE);
                    LOG_TRACE("Creating batch of " << numberToCreate << " people. "
                              << resourceLimit - ourUsage << " free bytes remaining");
                    this->createNewModels(numberToCreate, 0);
                    numberExistingPeople += numberToCreate;
                    ourUsage = this->estimateMemoryUsage(numberExistingPeople, 0, numberCorrelations);
                    numberNewPeople -= numberToCreate;
                }
            }
        }
    }

    if (numberNewPeople > 0)
    {
        resourceMonitor.acceptAllocationFailureResult(time);
        LOG_DEBUG("Not enough memory to create models");
        core::CStatistics::instance().stat(stat_t::E_NumberMemoryLimitModelCreationFailures).
            increment(numberNewPeople);
        std::size_t toRemove = gatherer.numberPeople() - numberNewPeople;
        gatherer.removePeople(toRemove);
    }

    this->refreshCorrelationModels(resourceLimit, resourceMonitor);
}

void CIndividualModel::createNewModels(std::size_t n, std::size_t m)
{
    if (n > 0)
    {
        std::size_t newN = m_FirstBucketTimes.size() + n;
        core::CAllocationStrategy::resize(this->extraData(), newN);
        core::CAllocationStrategy::resize(m_FirstBucketTimes, newN, CModel::TIME_UNSET);
        core::CAllocationStrategy::resize(m_LastBucketTimes, newN, CModel::TIME_UNSET);
        CModelTools::createPriors(n, m_NewFeaturePriors, m_Priors);
        CModelTools::createPriors(n, m_NewMultivariateFeaturePriors, m_MultivariatePriors);
        for (std::size_t i = 0u; i < m_Decompositions.size(); ++i)
        {
            const TDecompositionCPtrVec &newDecompositions = m_NewDecompositions[i].second;
            TDecompositionPtr1VecVec &trends = m_Decompositions[i].second;
            std::size_t pid = trends.size();
            core::CAllocationStrategy::resize(trends, newN);
            for (/**/; pid < trends.size(); ++pid)
            {
                trends[pid].reserve(newDecompositions.size());
                for (std::size_t j = 0u; j < newDecompositions.size(); ++j)
                {
                    trends[pid].push_back(TDecompositionPtr(newDecompositions[j]->clone()));
                }
            }
        }
        for (std::size_t i = 0u; i < m_DecayRateControllers.size(); ++i)
        {
            std::size_t dimension = model_t::dimension(m_DecayRateControllers[i].first);
            TDecayRateControllerArray initial;
            std::fill_n(initial.begin(),
                        static_cast<std::size_t>(E_NumberControls),
                        CDecayRateController(dimension));
            core::CAllocationStrategy::resize(m_DecayRateControllers[i].second, newN, initial);
        }
        for (std::size_t i = 0u; i < this->correlations().size(); ++i)
        {
            (this->correlations())[i].second.addVariables(newN);
        }
    }
    this->CModel::createNewModels(n, m);
}

void CIndividualModel::updateRecycledModels(void)
{
    const TSizeVec &recycledPeople = this->dataGatherer().recycledPersonIds();
    for (std::size_t i = 0u; i < recycledPeople.size(); ++i)
    {
        std::size_t pid = recycledPeople[i];
        m_FirstBucketTimes[pid] = CModel::TIME_UNSET;
        m_LastBucketTimes[pid]  = CModel::TIME_UNSET;
        CModelTools::resetRecycledPriors(pid, m_NewFeaturePriors, m_Priors);
        CModelTools::resetRecycledPriors(pid, m_NewMultivariateFeaturePriors, m_MultivariatePriors);
        for (std::size_t j = 0u; j < m_Decompositions.size(); ++j)
        {
            const TDecompositionCPtrVec &newDecompositions = m_NewDecompositions[j].second;
            TDecompositionPtr1VecVec &trends = m_Decompositions[j].second;
            if (pid < trends.size())
            {
                for (std::size_t k = 0u; k < trends[pid].size(); ++k)
                {
                    trends[pid][k].reset(newDecompositions[k]->clone());
                }
            }
        }
        for (std::size_t j = 0u; j < m_DecayRateControllers.size(); ++j)
        {
            std::size_t dimension = model_t::dimension(m_DecayRateControllers[j].first);
            if (pid < m_DecayRateControllers[j].second.size())
            {
                std::fill_n(m_DecayRateControllers[j].second[pid].begin(),
                            static_cast<std::size_t>(E_NumberControls),
                            CDecayRateController(dimension));
            }
        }
    }
    for (std::size_t i = 0u; i < this->correlations().size(); ++i)
    {
        (this->correlations())[i].second.removeVariables(recycledPeople);
    }
    this->CModel::updateRecycledModels();
}

void CIndividualModel::refreshCorrelationModels(std::size_t resourceLimit,
                                                CResourceMonitor &resourceMonitor)
{
    typedef std::vector<TSizeSizePr> TSizeSizePrVec;

    std::size_t numberPeople = this->dataGatherer().numberActivePeople();

    double maxNumberCorrelations =  this->params().s_CorrelationModelsOverhead
                                  * static_cast<double>(this->numberOfPeople());
    std::size_t maxNumberCorrelations_ = static_cast<std::size_t>(maxNumberCorrelations + 0.5);

    for (TFeatureSizeSizePrMultivariatePriorPtrDoublePrUMapMapItr i = m_CorrelatePriors.begin();
         i != m_CorrelatePriors.end();
         ++i)
    {
        model_t::EFeature feature = i->first;

        const maths::CKMostCorrelated *correlations = this->correlations(feature);
        if (correlations && correlations->changed())
        {
            TSizeSizePrMultivariatePriorPtrDoublePrUMap &priors = i->second;

            TSizeSizePrVec correlated;
            TDoubleVec correlationCoeffs;
            correlations->mostCorrelated(static_cast<std::size_t>(1.2 * maxNumberCorrelations),
                                         correlated,
                                         &correlationCoeffs);
            LOG_TRACE("correlated = " << core::CContainerPrinter::print(correlated));
            LOG_TRACE("correlationCoeffs = " << core::CContainerPrinter::print(correlationCoeffs));

            ptrdiff_t cutoff = std::upper_bound(correlationCoeffs.begin(), correlationCoeffs.end(),
                                                0.5 * this->params().s_MinimumSignificantCorrelation,
                                                SAbsGreater()) - correlationCoeffs.begin();
            LOG_TRACE("cutoff = " << cutoff);

            correlated.erase(correlated.begin() + cutoff, correlated.end());

            if (correlated.empty())
            {
                priors.clear();
                this->refreshCorrelated(feature, priors);
                continue;
            }

            // Extract the correlated pairs which are and aren't already
            // being modeled.
            TSizeSizePrVec present;
            TSizeVec presentRank;
            TSizeSizePrVec missing;
            TSizeVec missingRank;
            std::size_t np = static_cast<std::size_t>(
                                 std::max(0.9 * static_cast<double>(correlated.size()), 1.0));
            std::size_t nm = static_cast<std::size_t>(
                                 std::max(0.1 * static_cast<double>(correlated.size()), 1.0));
            present.reserve(np);
            presentRank.reserve(np);
            missing.reserve(nm);
            missingRank.reserve(nm);
            for (std::size_t j = 0u; j < correlated.size(); ++j)
            {
                bool isPresent = priors.count(correlated[j]) > 0;
                (isPresent ? present : missing).push_back(correlated[j]);
                (isPresent ? presentRank : missingRank).push_back(j);
            }

            // Remove any weakly correlated models.
            std::size_t initial = priors.size();
            maths::COrderings::simultaneousSort(present, presentRank);
            for (TSizeSizePrMultivariatePriorPtrDoublePrUMapItr j = priors.begin(); j != priors.end(); /**/)
            {
                std::size_t k = std::lower_bound(present.begin(),
                                                 present.end(),
                                                 j->first) - present.begin();
                if (k == present.size() || j->first != present[k])
                {
                    j = priors.erase(j);
                }
                else
                {
                    j->second.second = correlationCoeffs[presentRank[k]];
                    ++j;
                }
            }

            // Remove the remaining most weakly correlated models subject
            // to the capacity constraint.
            maths::COrderings::simultaneousSort(presentRank, present, std::greater<std::size_t>());
            for (std::size_t j = 0u; priors.size() > maxNumberCorrelations_; ++j)
            {
                priors.erase(present[j]);
            }

            if (resourceMonitor.areAllocationsAllowed())
            {
                for (std::size_t j = 0u, nextChunk = std::min(maxNumberCorrelations_, initial + CHUNK_SIZE);
                        priors.size() < maxNumberCorrelations_
                     && j < missing.size()
                     && (  resourceMonitor.haveNoLimit()
                         || priors.size() <= initial
                         || this->estimateMemoryUsage(numberPeople, 0, this->numberCorrelations()) < resourceLimit);
                     nextChunk = std::min(maxNumberCorrelations_, nextChunk + CHUNK_SIZE))
                {
                    for (/**/; j < missing.size() && priors.size() < nextChunk; ++j)
                    {
                        priors.insert(std::make_pair(
                                          missing[j],
                                          std::make_pair(TMultivariatePriorPtr(this->newCorrelatePrior(feature)->clone()),
                                                         correlationCoeffs[missingRank[j]])));
                    }
                }
            }

            this->refreshCorrelated(feature, priors);
        }
    }
}

void CIndividualModel::clearPrunedResources(const TSizeVec &people, const TSizeVec &attributes)
{
    for (std::size_t i = 0u; i < this->correlations().size(); ++i)
    {
        (this->correlations())[i].second.removeVariables(people);
    }
    for (std::size_t i = 0u; i < people.size(); ++i)
    {
        std::size_t pid = people[i];
        for (TFeaturePriorPtrVecMapItr j = m_Priors.begin(); j != m_Priors.end(); ++j)
        {
            if (pid < j->second.size())
            {
                j->second[pid].reset(this->tinyPrior());
            }
        }
        for (TFeatureMultivariatePriorPtrVecMapItr j = m_MultivariatePriors.begin();
             j != m_MultivariatePriors.end();
             ++j)
        {
            if (pid < j->second.size())
            {
                j->second[pid].reset(this->tinyPrior(j->second[pid]->dimension()));
            }
        }
        for (TFeatureSizeSizePrMultivariatePriorPtrDoublePrUMapMapItr j = m_CorrelatePriors.begin();
             j != m_CorrelatePriors.end();
             ++j)
        {
            const TSize1Vec &correlated = this->correlated(j->first, pid);
            for (std::size_t k = 0u; k < correlated.size(); ++k)
            {
                j->second.erase(std::make_pair(pid, correlated[k]));
                j->second.erase(std::make_pair(correlated[k], pid));
            }
            this->refreshCorrelated(j->first, j->second);
        }
        for (std::size_t j = 0u; j < m_Decompositions.size(); ++j)
        {
            TDecompositionPtr1VecVec &trends = m_Decompositions[j].second;
            if (pid < trends.size())
            {
                for (std::size_t k = 0u; k < trends[pid].size(); ++k)
                {
                    trends[pid][k].reset(this->tinyDecomposition());
                }
            }
        }
    }
    this->CModel::clearPrunedResources(people, attributes);
}

double CIndividualModel::emptyBucketWeight(model_t::EFeature feature,
                                           std::size_t pid,
                                           core_t::TTime time) const
{
    double result = 1.0;
    if (model_t::countsEmptyBuckets(feature))
    {
        TOptionalUInt64 count = this->currentBucketCount(pid, time);
        if (!count || *count == 0)
        {
            double frequency = this->personFrequency(pid);
            result = model_t::emptyBucketCountWeight(feature, frequency,
                                                     this->params().s_CutoffToModelEmptyBuckets);
        }
    }
    return result;
}

double CIndividualModel::probabilityBucketEmpty(model_t::EFeature feature, std::size_t pid) const
{
    double result = 0.0;
    if (model_t::countsEmptyBuckets(feature))
    {
        double frequency = this->personFrequency(pid);
        double emptyBucketWeight = model_t::emptyBucketCountWeight(
                                       feature, frequency,
                                       this->params().s_CutoffToModelEmptyBuckets);
        result = (1.0 - frequency) * (1.0 - emptyBucketWeight);
    }
    return result;
}

bool CIndividualModel::hasPrior(model_t::EFeature feature, std::size_t pid) const
{
    if (model_t::dimension(feature) == 1)
    {
        const TPriorPtrVec *priors = this->priors(feature);
        return priors && pid < priors->size();
    }
    else
    {
        const TMultivariatePriorPtrVec *priors = this->multivariatePriors(feature);
        return priors && pid < priors->size();
    }
}

const maths::CPrior *CIndividualModel::prior(model_t::EFeature feature, std::size_t pid) const
{
    return const_cast<CIndividualModel*>(this)->prior(feature, pid);
}

maths::CPrior *CIndividualModel::prior(model_t::EFeature feature, std::size_t pid)
{
    TPriorPtrVec *priors = this->priors(feature);
    return (!priors || pid >= priors->size()) ? 0 : (*priors)[pid].get();
}

const maths::CMultivariatePrior *
    CIndividualModel::multivariatePrior(model_t::EFeature feature, std::size_t pid) const
{
    return const_cast<CIndividualModel*>(this)->multivariatePrior(feature, pid);
}

maths::CMultivariatePrior *
    CIndividualModel::multivariatePrior(model_t::EFeature feature, std::size_t pid)
{
    TMultivariatePriorPtrVec *priors = this->multivariatePriors(feature);
    return (!priors || pid >= priors->size()) ? 0 : (*priors)[pid].get();
}

void CIndividualModel::correlatePriors(model_t::EFeature feature,
                                       std::size_t pid,
                                       TSize1Vec &correlated,
                                       TSize2Vec1Vec &variables,
                                       TMultivariatePriorCPtrSizePr1Vec &priors) const
{
    variables.clear();
    priors.clear();

    if (correlated.empty())
    {
        return;
    }

    const TSizeSizePrMultivariatePriorPtrDoublePrUMap *correlatePriors = this->correlatePriors(feature);
    if (!correlatePriors)
    {
        return;
    }

    variables.reserve(correlated.size());
    priors.reserve(correlated.size());
    for (std::size_t i = 0u, end = 0u; i < correlated.size(); ++i)
    {
        TSizeSizePrMultivariatePriorPtrDoublePrUMapCItr j = correlatePriors->find(std::make_pair(pid, correlated[i]));
        std::size_t vi[] = { 0, 1 };
        if (j == correlatePriors->end())
        {
            j = correlatePriors->find(std::make_pair(correlated[i], pid));
            std::swap(vi[0], vi[1]);
        }
        if (j == correlatePriors->end())
        {
            LOG_ERROR("Unexpectedly missing prior for correlation (" << this->personName(pid)
                      << "," << this->personName(correlated[i]) << ")");
            continue;
        }
        if (::fabs(j->second.second) < this->params().s_MinimumSignificantCorrelation)
        {
            LOG_TRACE("Correlation " << j->second.second << " is too small to model");
            continue;
        }
        if (j->second.first->numberSamples() < MINIMUM_CORRELATE_PRIOR_SAMPLE_COUNT)
        {
            LOG_TRACE("Too few samples in correlate model");
            continue;
        }
        correlated[end] = correlated[i];
        variables.push_back(TSize2Vec(vi, vi + 2));
        priors.push_back(std::make_pair(j->second.first.get(), vi[0]));
        ++end;
    }
    correlated.resize(variables.size());
}

double CIndividualModel::correctBaselineForCorrelated(model_t::EFeature feature,
                                                      std::size_t pid,
                                                      model_t::CResultType type,
                                                      const TSizeDoublePr1Vec &correlated) const
{
    typedef core::CSmallVector<std::size_t, 10> TSize10Vec;
    typedef core::CSmallVector<TSizeDoublePr, 10> TSizeDoublePr10Vec;

    double result = 0.0;
    switch (type.asConditionalOrUnconditional())
    {
    case model_t::CResultType::E_Unconditional:
        break;
    case model_t::CResultType::E_Conditional:
        if (!correlated.empty())
        {
            TSize1Vec correlated_(1, correlated[0].first);
            TSize2Vec1Vec variables;
            TMultivariatePriorCPtrSizePr1Vec correlatePriors;
            this->correlatePriors(feature, pid, correlated_, variables, correlatePriors);
            if (!correlatePriors.empty())
            {
                static const TSize10Vec NOTHING_TO_MARGINALIZE;
                static const TSizeDoublePr10Vec NOTHING_TO_CONDITION;
                TSize10Vec marginalize(1, variables[0][1]);
                TSizeDoublePr10Vec condition(1, std::make_pair(variables[0][1], correlated[0].second));
                TPriorPtr margin = correlatePriors[0].first->univariate(marginalize, NOTHING_TO_CONDITION).first;
                TPriorPtr conditional = correlatePriors[0].first->univariate(NOTHING_TO_MARGINALIZE, condition).first;
                result = conditional->marginalLikelihoodMean() - margin->marginalLikelihoodMean();
            }
        }
        break;
    }
    return result;
}

void CIndividualModel::correctBaselineForInterim(model_t::EFeature feature,
                                                 std::size_t pid,
                                                 model_t::CResultType type,
                                                 const TSizeDoublePr1Vec &correlated,
                                                 const TFeatureSizeSizeTripleDouble1VecUMap &corrections,
                                                 TDouble1Vec &result) const
{
    if (type.isInterim() && model_t::requiresInterimResultAdjustment(feature))
    {
        TFeatureSizeSizeTriple key(feature, pid, pid);
        switch (type.asConditionalOrUnconditional())
        {
        case model_t::CResultType::E_Unconditional:
            break;
        case model_t::CResultType::E_Conditional:
            if (!correlated.empty())
            {
                key.third = correlated[0].first;
            }
            break;
        }
        TFeatureSizeSizeTripleDouble1VecUMapCItr correction = corrections.find(key);
        if (correction != corrections.end())
        {
            result -= correction->second;
        }
    }
}

const CIndividualModel::TTimeVec &CIndividualModel::firstBucketTimes(void) const
{
    return m_FirstBucketTimes;
}

const CIndividualModel::TPriorPtrVec *CIndividualModel::priors(model_t::EFeature feature) const
{
    return const_cast<CIndividualModel*>(this)->priors(feature);
}

CIndividualModel::TPriorPtrVec *CIndividualModel::priors(model_t::EFeature feature)
{
    TFeaturePriorPtrVecMapItr result = m_Priors.find(feature);
    return result == m_Priors.end() ? 0 : &result->second;
}

const maths::CPrior *CIndividualModel::newPrior(model_t::EFeature feature) const
{
    return newPriors<maths::CPrior>(m_NewFeaturePriors, feature);
}

const CIndividualModel::TMultivariatePriorPtrVec *
    CIndividualModel::multivariatePriors(model_t::EFeature feature) const
{
    return const_cast<CIndividualModel*>(this)->multivariatePriors(feature);
}

CIndividualModel::TMultivariatePriorPtrVec *
    CIndividualModel::multivariatePriors(model_t::EFeature feature)
{
    TFeatureMultivariatePriorPtrVecMapItr result = m_MultivariatePriors.find(feature);
    return result == m_MultivariatePriors.end() ? 0 : &result->second;
}

const maths::CMultivariatePrior *CIndividualModel::newMultivariatePrior(model_t::EFeature feature) const
{
    return newPriors<maths::CMultivariatePrior>(m_NewMultivariateFeaturePriors, feature);
}

const CIndividualModel::TSizeSizePrMultivariatePriorPtrDoublePrUMap *
    CIndividualModel::correlatePriors(model_t::EFeature feature) const
{
    return const_cast<CIndividualModel*>(this)->correlatePriors(feature);
}

CIndividualModel::TSizeSizePrMultivariatePriorPtrDoublePrUMap *
    CIndividualModel::correlatePriors(model_t::EFeature feature)
{
    TFeatureSizeSizePrMultivariatePriorPtrDoublePrUMapMapItr result = m_CorrelatePriors.find(feature);
    return result == m_CorrelatePriors.end() ? 0 : &result->second;
}

const maths::CMultivariatePrior *CIndividualModel::newCorrelatePrior(model_t::EFeature feature) const
{
    return newPriors<maths::CMultivariatePrior>(m_NewCorrelateFeaturePriors, feature);
}

std::size_t CIndividualModel::numberCorrelations(void) const
{
    std::size_t result = 0u;
    for (TFeatureSizeSizePrMultivariatePriorPtrDoublePrUMapMapCItr i = m_CorrelatePriors.begin();
         i != m_CorrelatePriors.end();
         ++i)
    {
        result += i->second.size();
    }
    return result;
}

CIndividualModel::TDecompositionCPtr1Vec
    CIndividualModel::trend(model_t::EFeature feature, std::size_t pid) const
{
    const TDecompositionPtr1Vec &trend = const_cast<CIndividualModel*>(this)->trend(feature, pid);

    // For the usual case it is faster to not get both iterators.
    if (trend.size() == 1)
    {
        return TDecompositionCPtr1Vec(1, trend[0].get());
    }

    TDecompositionCPtr1Vec result;
    for (TDecompositionPtr1VecCItr i = trend.begin(); i != trend.end(); ++i)
    {
        result.push_back(i->get());
    }
    return result;
}

const CIndividualModel::TDecompositionPtr1Vec &
    CIndividualModel::trend(model_t::EFeature feature, std::size_t pid)
{
    static const TDecompositionPtr1Vec NO_TREND;
    std::size_t i = static_cast<std::size_t>(
                        std::lower_bound(m_Decompositions.begin(),
                                         m_Decompositions.end(),
                                         feature, maths::COrderings::SFirstLess())
                      - m_Decompositions.begin());
    if (i == m_Decompositions.size())
    {
        LOG_ERROR("No decomposition for feature " << model_t::print(feature));
        return NO_TREND;
    }
    if (pid >= m_Decompositions[i].second.size())
    {
        LOG_ERROR("No decomposition for person " << this->personName(pid));
        return NO_TREND;
    }
    return m_Decompositions[i].second[pid];
}

CIndividualModel::TDouble1Vec CIndividualModel::detrend(model_t::EFeature feature,
                                                        std::size_t pid,
                                                        core_t::TTime time,
                                                        double confidence,
                                                        const TDouble1Vec &value) const
{
    TDecompositionCPtr1Vec trend = this->trend(feature, pid);
    if (trend.empty())
    {
        return value;
    }

    std::size_t dimension = trend.size();
    if (value.size() != dimension)
    {
        LOG_ERROR("dimension mismatch: '" << value.size() << " != " << dimension << "'");
        return value;
    }

    TDouble1Vec detrended;
    detrended.reserve(dimension);
    for (std::size_t i = 0u; i < dimension; ++i)
    {
        detrended.push_back(trend[i]->detrend(time, value[i], confidence));
    }
    LOG_TRACE("time = " << time
              << ", value = " << core::CContainerPrinter::print(value)
              << ", detrended = " << core::CContainerPrinter::print(detrended));

    return detrended;
}

CIndividualModel::TDouble1VecDouble1VecPr
    CIndividualModel::seasonalVarianceScale(model_t::EFeature feature,
                                            std::size_t pid,
                                            core_t::TTime time,
                                            double confidence) const
{
    TDecompositionCPtr1Vec trend = this->trend(feature, pid);
    std::size_t dimension = model_t::dimension(feature);
    if (trend.empty())
    {
        return TDouble1VecDouble1VecPr(TDouble1Vec(dimension, 1.0),
                                       TDouble1Vec(dimension, 1.0));
    }

    TDouble1Vec variance;
    if (dimension == 1)
    {
        if (const maths::CPrior *prior = this->prior(feature, pid))
        {
            variance.assign(1, prior->marginalLikelihoodVariance());
        }
    }
    else if (const maths::CMultivariatePrior *prior = this->multivariatePrior(feature, pid))
    {
        variance = prior->marginalLikelihoodVariances();
    }
    if (variance.empty())
    {
        return TDouble1VecDouble1VecPr(TDouble1Vec(dimension, 1.0),
                                       TDouble1Vec(dimension, 1.0));
    }

    TDouble1VecDouble1VecPr scale;
    scale.first.reserve(dimension);
    scale.second.reserve(dimension);
    for (std::size_t i = 0u; i < dimension; ++i)
    {
        TDoubleDoublePr si = trend[i]->scale(time, variance[i], confidence);
        si.first  = std::max(si.first, MINIMUM_SEASONAL_VARIANCE_SCALE);
        si.second = std::max(si.second, MINIMUM_SEASONAL_VARIANCE_SCALE);
        scale.first.push_back(si.first);
        scale.second.push_back(si.second);
    }
    LOG_TRACE("time = " << time << ", scale = " << core::CContainerPrinter::print(scale));

    return scale;
}

CIndividualModel::TDouble1Vec CIndividualModel::updateTrend(model_t::EFeature feature,
                                                            std::size_t pid,
                                                            core_t::TTime time,
                                                            const TDouble1Vec &value,
                                                            const maths_t::TWeightStyleVec &weightStyles,
                                                            const TDouble1Vec4Vec &weights)
{
    const TDecompositionPtr1Vec &trend = this->trend(feature, pid);
    if (trend.empty())
    {
        return value;
    }

    std::size_t dimension = trend.size();
    if (value.size() != dimension)
    {
        LOG_ERROR("Dimension mismatch: '" << value.size() << " != " << dimension << "'");
        return value;
    }

    TDouble1Vec detrended;
    detrended.reserve(dimension);

    bool reinitialise = false;
    TDouble4Vec weight(weights.size());
    for (std::size_t i = 0u; i < dimension; ++i)
    {
        trend[i]->propagateForwardsTo(time);
        for (std::size_t j = 0u; j < weight.size(); ++j)
        {
            weight[j] = weights[j][i];
        }
        if (trend[i]->addPoint(time, value[i], weightStyles, weight))
        {
            reinitialise = true;
        }
        detrended.push_back(trend[i]->detrend(time, value[i], 0.0));
    }

    if (reinitialise)
    {
        // We have different periodic components.

        if (dimension == 1)
        {
            maths::CPrior *prior = this->prior(feature, pid);
            if (prior && maths::initializePrior(this->bucketLength(),
                                                this->learnRate(feature),
                                                *trend[0], *prior))
            {
                LOG_DEBUG("Reinitialized model for feature " << model_t::print(feature)
                          << " and person '" << this->personName(pid) << "'");
                this->dataGatherer().resetSampleCount(pid);
                prior->decayRate(this->newPrior(feature)->decayRate());
            }
            if (TSizeSizePrMultivariatePriorPtrDoublePrUMap *correlatePriors = this->correlatePriors(feature))
            {
                const TSize1Vec &correlated = this->correlated(feature, pid);
                for (std::size_t i = 0u; i < correlated.size(); ++i)
                {
                    LOG_DEBUG("Removing correlate model for (" << this->personName(pid)
                              << "," << this->personName(correlated[i]) << ")");
                    correlatePriors->erase(std::make_pair(pid, correlated[i]));
                    correlatePriors->erase(std::make_pair(correlated[i], pid));
                }
                this->refreshCorrelated(feature, *correlatePriors);
            }
            if (CDecayRateController *controller =
                    this->decayRateController(E_TrendControl, feature, pid))
            {
                controller->reset();
            }
            if (CDecayRateController *controller =
                    this->decayRateController(E_PriorControl, feature, pid))
            {
                controller->reset();
            }
        }
        else
        {
            maths::CMultivariatePrior *prior = this->multivariatePrior(feature, pid);
            if (prior && maths::initializePrior(this->bucketLength(),
                                                this->learnRate(feature),
                                                trend, *prior))
            {
                LOG_DEBUG("Reinitialized model for feature " << model_t::print(feature)
                          << " and person '" << this->personName(pid) << "'");
                this->dataGatherer().resetSampleCount(pid);
            }
        }

        if (maths::CKMostCorrelated *correlations = this->correlations(feature))
        {
            correlations->removeVariables(TSizeVec(1, pid));
        }
    }
    LOG_TRACE("time = " << time
              << ", value = " << core::CContainerPrinter::print(value)
              << ", detrended = " << core::CContainerPrinter::print(detrended));

    return detrended;
}

CDecayRateController *CIndividualModel::decayRateController(EDecayRateController controller,
                                                            model_t::EFeature feature,
                                                            std::size_t pid)
{
    if (model_t::isConstant(feature) || !this->params().s_ControlDecayRate)
    {
        return 0;
    }
    std::size_t i = static_cast<std::size_t>(
                        std::lower_bound(m_DecayRateControllers.begin(),
                                         m_DecayRateControllers.end(),
                                         feature, maths::COrderings::SFirstLess())
                      - m_DecayRateControllers.begin());
    if (i == m_DecayRateControllers.size())
    {
        LOG_ERROR("No controller for feature " << model_t::print(feature));
        return 0;
    }
    if (pid >= m_DecayRateControllers[i].second.size())
    {
        LOG_ERROR("No controller for person " << this->personName(pid));
        return 0;
    }
    return &m_DecayRateControllers[i].second[pid][controller];
}

double CIndividualModel::decayRateMultiplier(EDecayRateController controller_,
                                             model_t::EFeature feature,
                                             std::size_t pid,
                                             const TDouble1Vec &prediction,
                                             const TDouble1Vec &residual)
{
    double result = 1.0;
    if (CDecayRateController *controller = this->decayRateController(controller_, feature, pid))
    {
        result = controller->multiplier(prediction, residual,
                                        this->bucketLength(),
                                        this->learnRate(feature),
                                        this->params().s_DecayRate);
    }
    return result;
}

double CIndividualModel::derate(std::size_t pid, core_t::TTime time) const
{
    return std::max(1.0 -  static_cast<double>(time - m_FirstBucketTimes[pid])
                         / static_cast<double>(3 * core::constants::WEEK), 0.0);
}

std::string CIndividualModel::printCurrentBucket(void) const
{
    std::ostringstream result;
    result << "[" << this->currentBucketStartTime() << ","
           << this->currentBucketStartTime() + this->bucketLength() << ")";
    return result.str();
}

double CIndividualModel::attributeFrequency(std::size_t /*cid*/) const
{
    return 1.0;
}

void CIndividualModel::doSkipSampling(core_t::TTime startTime, core_t::TTime endTime)
{
    const CDataGatherer &gatherer = this->dataGatherer();
    core_t::TTime gapDuration = endTime - startTime;
    for (std::size_t pid = 0u; pid < m_LastBucketTimes.size(); ++pid)
    {
        if (gatherer.isPersonActive(pid) && !CModel::isTimeUnset(m_LastBucketTimes[pid]))
        {
            m_LastBucketTimes[pid] = m_LastBucketTimes[pid] + gapDuration;
        }
    }

    for (std::size_t i = 0u; i < m_Decompositions.size(); ++i)
    {
        TDecompositionPtr1VecVec &trends = m_Decompositions[i].second;
        for (std::size_t pid = 0; pid < trends.size(); ++pid)
        {
            TDecompositionPtr1Vec &trend = trends[pid];
            for (std::size_t j = 0; j < trend.size(); ++j)
            {
                trend[j]->skipTime(gapDuration);
            }
        }
    }
}

const double CIndividualModel::MINIMUM_SEASONAL_VARIANCE_SCALE(0.2);

}
}
