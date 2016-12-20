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

#include <model/CModel.h>

#include <core/CAllocationStrategy.h>
#include <core/CFunctional.h>
#include <core/CLogger.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>
#include <core/CStatistics.h>
#include <core/RestoreMacros.h>

#include <maths/CChecksum.h>
#include <maths/CConstantPrior.h>
#include <maths/CDistributionRestoreParams.h>
#include <maths/CMathsFuncs.h>
#include <maths/CMultivariateConstantPrior.h>
#include <maths/CMultivariatePrior.h>
#include <maths/COrderings.h>
#include <maths/CPrior.h>
#include <maths/CPriorStateSerialiser.h>
#include <maths/CTimeSeriesDecomposition.h>
#include <maths/CTimeSeriesDecompositionStateSerialiser.h>
#include <maths/CTimeSeriesDecompositionStub.h>
#include <maths/CTools.h>

#include <model/CAnnotatedProbabilityBuilder.h>
#include <model/CDataGatherer.h>
#include <model/CDecayRateController.h>
#include <model/CDetectionRule.h>
#include <model/CHierarchicalResults.h>
#include <model/CInterimBucketCorrector.h>
#include <model/CProbabilityAndInfluenceCalculator.h>
#include <model/CModelConfig.h>
#include <model/CSearchKey.h>
#include <model/FrequencyPredicates.h>

#include <boost/bind.hpp>

#include <algorithm>

namespace prelert
{
namespace model
{

namespace
{

typedef CModel::TSizeSizePr TSizeSizePr;
typedef CModel::TMultivariatePriorPtr TMultivariatePriorPtr;
typedef CModel::TMultivariatePriorPtrDoublePr TMultivariatePriorPtrDoublePr;
typedef std::pair<const TSizeSizePr, TMultivariatePriorPtrDoublePr> TSizeSizePrMultivariatePriorPtrDoublePrPr;
typedef CModel::TSizeSizePrMultivariatePriorPtrDoublePrUMap TSizeSizePrMultivariatePriorPtrDoublePrUMap;
typedef CDataGatherer::TSizeSizePrUInt64UMap TSizeSizePrUInt64UMap;
typedef CDataGatherer::TSizeSizePrUInt64UMapCItr TSizeSizePrUInt64UMapCItr;

const std::string DIMENSION_TAG("a");
const std::string FEATURE_TAG("b");
const std::string CORRELATION_TAG("c");
const std::string FIRST_ID_TAG("d");
const std::string SECOND_ID_TAG("e");
const std::string PRIOR_TAG("f");
const std::string CORRELATION_COEFF_TAG("g");

//! Read a feature out of the traverser value and add an entry to \p result.
template<typename T>
bool readFeature(core::CStateRestoreTraverser &traverser,
                 std::size_t &index,
                 std::vector<std::pair<model_t::EFeature, T> > &result)
{
    int feature;
    if (core::CStringUtils::stringToType(traverser.value(), feature) == false || feature < 0)
    {
        LOG_ERROR("Invalid feature in " << traverser.value());
        return false;
    }

    index = result.size();
    for (size_t i = 0; i < result.size(); ++i)
    {
        if (result[i].first == static_cast<model_t::EFeature>(feature))
        {
            index = i;
            break;
        }
    }
    if (index == result.size())
    {
        result.push_back(std::pair<model_t::EFeature, T>(static_cast<model_t::EFeature>(feature), T()));
    }
    return true;
}

//! Restore a vector of feature prior pairs.
template<typename T>
bool restoreFeaturePriors(const maths::SDistributionRestoreParams &params,
                          const std::string &featureTag,
                          const std::string &priorTag,
                          core::CStateRestoreTraverser &traverser,
                          std::vector<std::pair<model_t::EFeature, T> > &result)
{
    int feature_ = -1;

    do
    {
        const std::string &name = traverser.name();
        if (name == featureTag)
        {
            if (core::CStringUtils::stringToType(traverser.value(), feature_) == false || feature_ < 0)
            {
                LOG_ERROR("Invalid feature in " << traverser.value());
                return false;
            }
        }
        else if (name == priorTag)
        {
            if (feature_ < 0)
            {
                LOG_ERROR("Invalid XML: seen prior before feature");
                return false;
            }

            T prior;
            if (traverser.traverseSubLevel(boost::bind<bool>(maths::CPriorStateSerialiser(),
                                                             boost::cref(params),
                                                             boost::ref(prior),
                                                             _1)) == false || prior == 0)
            {
                LOG_ERROR("Invalid prior");
                return false;
            }

            model_t::EFeature feature = static_cast<model_t::EFeature>(feature_);
            result.insert(std::lower_bound(result.begin(), result.end(),
                                           feature, maths::COrderings::SFirstLess()),
                          std::pair<model_t::EFeature, T>(feature, prior));

        }
    }
    while (traverser.next());

    return feature_ >= 0;
}

//! Restore a map from feature to a collection of priors.
template<typename PRIORS>
bool restoreFeaturePriorsMap(const maths::SDistributionRestoreParams &params,
                             const std::string &featureTag,
                             const std::string &priorTag,
                             core::CStateRestoreTraverser &traverser,
                             std::map<model_t::EFeature, PRIORS> &result)
{
    PRIORS *priors(0);

    do
    {
        const std::string &name = traverser.name();
        if (name == featureTag)
        {
            int feature(-1);
            if (core::CStringUtils::stringToType(traverser.value(), feature) == false || feature < 0)
            {
                LOG_ERROR("Invalid feature in " << traverser.value());
                return false;
            }
            priors = &result[static_cast<model_t::EFeature>(feature)];
        }
        else if (name == priorTag)
        {
            if (priors == 0)
            {
                LOG_ERROR("Invalid XML: seen prior before feature");
                return false;
            }
            typename PRIORS::value_type prior;
            if (traverser.traverseSubLevel(boost::bind<bool>(maths::CPriorStateSerialiser(),
                                                             boost::cref(params),
                                                             boost::ref(prior),
                                                             _1)) == false || prior == 0)
            {
                LOG_ERROR("Invalid prior");
                return false;
            }
            priors->push_back(prior);
        }
    }
    while (traverser.next());

    return (priors != 0);
}

//! Restore a correlated pair and corresponding correlate prior.
bool restoreCorrelatedPairPrior(const maths::SDistributionRestoreParams &params,
                                TSizeSizePrMultivariatePriorPtrDoublePrUMap &priors,
                                core::CStateRestoreTraverser &traverser)
{
    unsigned int restored = 0;
    std::size_t first = 0;
    std::size_t second = 0;
    do
    {
        const std::string &name = traverser.name();
        if (name == FIRST_ID_TAG)
        {
            if (core::CStringUtils::stringToType(traverser.value(), first) == false)
            {
                LOG_ERROR("Invalid identifier in " << traverser.value());
                return false;
            }
            restored |= 1;
        }
        else if (name == SECOND_ID_TAG)
        {
            if (core::CStringUtils::stringToType(traverser.value(), second) == false)
            {
                LOG_ERROR("Invalid identifier in " << traverser.value());
                return false;
            }
            restored |= 2;
        }
        else if (name == PRIOR_TAG)
        {
            if (restored != 3)
            {
                LOG_ERROR("Invalid XML: seen prior before identifiers");
                return false;
            }
            TMultivariatePriorPtr &prior = priors[std::make_pair(first, second)].first;
            if (traverser.traverseSubLevel(boost::bind<bool>(maths::CPriorStateSerialiser(),
                                                             boost::cref(params),
                                                             boost::ref(prior),
                                                             _1)) == false || prior == 0)
            {
                LOG_ERROR("Invalid prior");
                return false;
            }
            restored |= 4;
        }
        else if (name == CORRELATION_COEFF_TAG)
        {
            if (restored != 7)
            {
                LOG_ERROR("Invalid XML: seen correlation coefficient too early");
                return false;
            }
            double correlationCoeff;
            if (core::CStringUtils::stringToType(traverser.value(), correlationCoeff) == false)
            {
                LOG_ERROR("Invalid correlation coefficient in " << traverser.value());
                return false;
            }
            priors[std::make_pair(first, second)].second = correlationCoeff;
        }
    }
    while (traverser.next());

    return restored == 7;
}

//! Restore a multivariate trend object.
bool restoreMultivariateTrend(double decayRate,
                              core_t::TTime bucketLength,
                              CModel::TDecompositionPtr1Vec &trend,
                              core::CStateRestoreTraverser &traverser)
{
    do
    {
        if (traverser.name() == DIMENSION_TAG)
        {
            CModel::TDecompositionPtr ti;
            if (traverser.traverseSubLevel(boost::bind(&maths::CTimeSeriesDecompositionStateSerialiser::acceptRestoreTraverser,
                                                       CModelConfig::trendDecayRate(decayRate, bucketLength),
                                                       bucketLength,
                                                       maths::CTimeSeriesDecomposition::DEFAULT_COMPONENT_SIZE,
                                                       boost::ref(ti), _1)) == false)
            {
                LOG_ERROR("Invalid trend");
                return false;
            }
            trend.push_back(ti);
        }
    }
    while (traverser.next());

    return true;
}

//! Restore a feature's correlated pairs.
bool restoreCorrelation(CModel::TFeatureKMostCorrelatedPrVec &correlations,
                        core::CStateRestoreTraverser &traverser)
{
    int feature_ = -1;
    do
    {
        const std::string &name = traverser.name();
        if (name == FEATURE_TAG)
        {
            if (!core::CStringUtils::stringToType(traverser.value(), feature_) || feature_ < 0)
            {
                LOG_ERROR("Invalid feature in " << traverser.value());
                return false;
            }
        }
        else if (name == CORRELATION_TAG)
        {
            if (feature_ < 0)
            {
                LOG_ERROR("Invalid XML: seen prior before feature");
                return false;
            }

            model_t::EFeature feature = static_cast<model_t::EFeature>(feature_);
            CModel::TFeatureKMostCorrelatedPrVecItr i =
                    std::lower_bound(correlations.begin(), correlations.end(),
                                     feature, maths::COrderings::SFirstLess());
            if (i == correlations.end() || i->first != feature)
            {
                LOG_ERROR("Unexpected feature " << model_t::print(feature));
                return false;
            }

            if (traverser.traverseSubLevel(boost::bind(&maths::CKMostCorrelated::acceptRestoreTraverser,
                                                       &i->second, _1)) == false)
            {
                LOG_ERROR("Invalid correlation");
                return false;
            }
        }
    }
    while (traverser.next());

    return feature_ >= 0;
}

//! Persist a prior.
template<typename PRIOR>
void persistPrior(const std::string &priorTag,
                  const PRIOR &prior,
                  core::CStatePersistInserter &inserter)
{
    inserter.insertLevel(priorTag, boost::bind<void>(maths::CPriorStateSerialiser(),
                                                     boost::cref(prior), _1));
}

//! Persist a collection of priors.
template<typename PRIORS>
void persistPriors(const std::string &priorTag,
                   const PRIORS &priors,
                   core::CStatePersistInserter &inserter)
{
    for (std::size_t id = 0; id < priors.size(); ++id)
    {
        inserter.insertLevel(priorTag, boost::bind<void>(maths::CPriorStateSerialiser(),
                                                         boost::cref(*priors[id]), _1));
    }
}

//! Persist correlated pair and corresponding correlate prior.
void persistCorrelatedPairPrior(const TSizeSizePrMultivariatePriorPtrDoublePrPr &prior,
                                core::CStatePersistInserter &inserter)
{
    inserter.insertValue(FIRST_ID_TAG, prior.first.first);
    inserter.insertValue(SECOND_ID_TAG, prior.first.second);
    inserter.insertLevel(PRIOR_TAG, boost::bind<void>(maths::CPriorStateSerialiser(),
                                                      boost::cref(*prior.second.first), _1));
    inserter.insertValue(CORRELATION_COEFF_TAG, prior.second.second);
}

//! Persist a multivariate trend.
void persistMultivariateTrend(const CModel::TDecompositionPtr1Vec &trend,
                              core::CStatePersistInserter &inserter)
{
    for (std::size_t i = 0u; i < trend.size(); ++i)
    {
        inserter.insertLevel(DIMENSION_TAG,
                             boost::bind(&maths::CTimeSeriesDecompositionStateSerialiser::acceptPersistInserter,
                                         boost::cref(*trend[i]), _1));
    }
}

//! Persist the feature's correlated pairs.
void persistCorrelation(const CModel::TFeatureKMostCorrelatedPr &correlation,
                        core::CStatePersistInserter &inserter)
{
    inserter.insertValue(FEATURE_TAG, static_cast<int>(correlation.first));
    inserter.insertLevel(CORRELATION_TAG, boost::bind(&maths::CKMostCorrelated::acceptPersistInserter,
                                                      &correlation.second, _1));
}

}

CModel::CModel(const SModelParams &params,
               const TDataGathererPtr &dataGatherer,
               const TFeatureInfluenceCalculatorCPtrPrVecVec &influenceCalculators,
               bool isForRestore) :
        m_Params(params),
        m_DataGatherer(dataGatherer),
        m_BucketCount(0.0),
        m_InfluenceCalculators(influenceCalculators),
        m_InterimBucketCorrector(new CInterimBucketCorrector(dataGatherer->bucketLength()))
{
    if (!m_DataGatherer)
    {
        LOG_ABORT("Must provide a data gatherer");
    }

    const model_t::TFeatureVec &features = dataGatherer->features();
    for (std::size_t i = 0u; i < features.size(); ++i)
    {
        model_t::EFeature feature = features[i];
        if (   this->params().s_MultivariateByFields
            && !model_t::isCategorical(feature)
            && !model_t::isConstant(feature)
            &&  model_t::dimension(feature) == 1)
        {
            m_Correlations.push_back(TFeatureKMostCorrelatedPr(
                                         feature, maths::CKMostCorrelated(MAXIMUM_CORRELATIONS,
                                                                          params.s_DecayRate,
                                                                          !isForRestore)));
            m_CorrelatedLookup.push_back(TFeatureSizeSize1VecUMapPr(feature, TSizeSize1VecUMap(1)));
        }
    }
    std::sort(m_Correlations.begin(), m_Correlations.end(), maths::COrderings::SFirstLess());
    std::sort(m_CorrelatedLookup.begin(), m_CorrelatedLookup.end(), maths::COrderings::SFirstLess());

    for (std::size_t i = 0u; i < m_InfluenceCalculators.size(); ++i)
    {
        std::sort(m_InfluenceCalculators[i].begin(),
                  m_InfluenceCalculators[i].end(),
                  maths::COrderings::SFirstLess());
    }
}

CModel::~CModel(void)
{
}

CModel::CModel(bool isForPersistence, const CModel &other) :
        // The copy of m_DataGatherer is a shallow copy.  This would be unacceptable
        // if we were going to persist the data gatherer from within this class.
        // We don't, so that's OK, but the next issue is that another thread will be
        // modifying the data gatherer m_DataGatherer points to whilst this object
        // is being persisted.  Therefore, persistence must only call methods on the
        // data gatherer that are invariant.
        m_Params(other.m_Params),
        m_DataGatherer(other.m_DataGatherer),
        m_PersonBucketCounts(other.m_PersonBucketCounts),
        m_BucketCount(other.m_BucketCount),
        m_Correlations(other.m_Correlations),
        m_CorrelatedLookup(other.m_CorrelatedLookup),
        m_InfluenceCalculators(),
        m_InterimBucketCorrector(new CInterimBucketCorrector(*other.m_InterimBucketCorrector)),
        m_ExtraData(other.m_ExtraData)
{
    if (!isForPersistence)
    {
        LOG_ABORT("This constructor only creates clones for persistence");
    }
}

std::string CModel::description(void) const
{
    return m_DataGatherer->description();
}

const std::string &CModel::personName(std::size_t pid) const
{
    return m_DataGatherer->personName(pid, core::CStringUtils::typeToString(pid));
}

const std::string &CModel::personName(std::size_t pid,
                                      const std::string &fallback) const
{
    return m_DataGatherer->personName(pid, fallback);
}

std::string CModel::printPeople(const TSizeVec &pids, std::size_t limit) const
{
    if (pids.empty())
    {
        return std::string();
    }
    if (limit == 0)
    {
        return core::CStringUtils::typeToString(pids.size()) + " in total";
    }
    std::string result = this->personName(pids[0]);
    for (std::size_t i = 1u; i < std::min(limit, pids.size()); ++i)
    {
        result += ' ';
        result += this->personName(pids[i]);
    }
    if (limit < pids.size())
    {
        result += " and ";
        result += core::CStringUtils::typeToString(pids.size() - limit);
        result += " others";
    }
    return result;
}

std::size_t CModel::numberOfPeople(void) const
{
    return m_DataGatherer->numberActivePeople();
}

const std::string &CModel::attributeName(std::size_t cid) const
{
    return m_DataGatherer->attributeName(cid, core::CStringUtils::typeToString(cid));
}

const std::string &CModel::attributeName(std::size_t cid,
                                         const std::string &fallback) const
{
    return m_DataGatherer->attributeName(cid, fallback);
}

std::string CModel::printAttributes(const TSizeVec &cids, std::size_t limit) const
{
    if (cids.empty())
    {
        return std::string();
    }
    if (limit == 0)
    {
        return core::CStringUtils::typeToString(cids.size()) + " in total";
    }
    std::string result = this->attributeName(cids[0]);
    for (std::size_t i = 1u; i < std::min(limit, cids.size()); ++i)
    {
        result += ' ';
        result += this->attributeName(cids[i]);
    }
    if (limit < cids.size())
    {
        result += " and ";
        result += core::CStringUtils::typeToString(cids.size() - limit);
        result += " others";
    }
    return result;
}

void CModel::sampleBucketStatistics(core_t::TTime startTime,
                                    core_t::TTime endTime,
                                    CResourceMonitor &/*resourceMonitor*/)
{
    const CDataGatherer &gatherer = this->dataGatherer();
    core_t::TTime bucketLength = this->bucketLength();
    for (core_t::TTime bucketStart = startTime;
         bucketStart < endTime;
         bucketStart += bucketLength)
    {
        const TSizeSizePrUInt64UMap &counts = gatherer.bucketCounts(bucketStart);
        std::size_t totalBucketCount = 0;
        for (TSizeSizePrUInt64UMapCItr i = counts.begin(); i != counts.end(); ++i)
        {
            totalBucketCount += i->second;
        }
        this->currentBucketTotalCount(totalBucketCount);
    }
}

void CModel::sample(core_t::TTime startTime,
                    core_t::TTime endTime,
                    CResourceMonitor &/*resourceMonitor*/)
{
    typedef boost::unordered_set<std::size_t> TSizeUSet;

    const CDataGatherer &gatherer = this->dataGatherer();

    core_t::TTime bucketLength = this->bucketLength();
    for (core_t::TTime time = startTime; time < endTime; time += bucketLength)
    {
        const TSizeSizePrUInt64UMap &counts = gatherer.bucketCounts(time);
        std::size_t totalBucketCount = 0;

        TSizeUSet uniquePeople;
        for (TSizeSizePrUInt64UMapCItr i = counts.begin(); i != counts.end(); ++i)
        {
            std::size_t pid = CDataGatherer::extractPersonId(*i);
            if (uniquePeople.insert(pid).second)
            {
                m_PersonBucketCounts[pid] += 1.0;
            }
            totalBucketCount += i->second;
        }

        m_InterimBucketCorrector->update(time, totalBucketCount);
        this->currentBucketTotalCount(totalBucketCount);
        m_BucketCount += 1.0;

        double alpha = ::exp(-this->params().s_DecayRate * 1.0);
        for (std::size_t pid = 0u; pid < m_PersonBucketCounts.size(); ++pid)
        {
            m_PersonBucketCounts[pid] *= alpha;
        }
        m_BucketCount *= alpha;
    }
}

void CModel::skipSampling(core_t::TTime endTime)
{
    CDataGatherer &gatherer = this->dataGatherer();
    core_t::TTime startTime = gatherer.earliestBucketStartTime();

    if (!gatherer.validateSampleTimes(startTime, endTime))
    {
        return;
    }

    gatherer.skipSampleNow(endTime);
    this->doSkipSampling(startTime, endTime);
    this->currentBucketStartTime(endTime - gatherer.bucketLength());
}

void CModel::addResult(int detector,
                       const std::string &partitionFieldValue,
                       std::size_t pid,
                       core_t::TTime startTime,
                       core_t::TTime endTime,
                       std::size_t numberAttributeProbabilities,
                       CHierarchicalResults &results) const
{
    CPartitioningFields partitioningFields(m_DataGatherer->partitionFieldName(), partitionFieldValue);

    if (this->category() == model_t::E_Counting)
    {
        SAnnotatedProbability annotatedProbability;
        if (this->computeProbability(pid, startTime, endTime, partitioningFields,
                                     numberAttributeProbabilities, annotatedProbability))
        {
            results.addSimpleCountResult(annotatedProbability, this, startTime);
        }
    }
    else
    {
        LOG_TRACE("AddResult, for time " << startTime << " -> " << endTime);
        for (CDataGatherer::TStrVecCItr i = m_DataGatherer->beginInfluencers();
             i != m_DataGatherer->endInfluencers();
             ++i)
        {
            results.addInfluencer(*i);
        }
        partitioningFields.add(m_DataGatherer->personFieldName(), this->personName(pid));
        SAnnotatedProbability annotatedProbability;
        annotatedProbability.s_ResultType = results.resultType();
        if (this->computeProbability(pid, startTime, endTime, partitioningFields,
                                     numberAttributeProbabilities, annotatedProbability))
        {
            function_t::EFunction function = m_DataGatherer->function();
            results.addModelResult(detector,
                                   this->isPopulation(),
                                   function_t::name(function),
                                   function,
                                   m_DataGatherer->partitionFieldName(),
                                   partitionFieldValue,
                                   m_DataGatherer->personFieldName(),
                                   this->personName(pid),
                                   m_DataGatherer->valueFieldName(),
                                   annotatedProbability,
                                   this,
                                   startTime);
        }
    }
}

std::size_t CModel::defaultPruneWindow(void) const
{
    // The longest we'll consider keeping priors for is 1M buckets.
    double decayRate = this->params().s_DecayRate;
    double factor = this->params().s_PruneWindowScaleMaximum;
    return (decayRate == 0.0) ?
           MAXIMUM_PERMITTED_AGE :
           std::min(static_cast<std::size_t>(factor / decayRate), MAXIMUM_PERMITTED_AGE);
}

std::size_t CModel::minimumPruneWindow(void) const
{
    double decayRate = this->params().s_DecayRate;
    double factor = this->params().s_PruneWindowScaleMinimum;
    return (decayRate == 0.0) ?
           MAXIMUM_PERMITTED_AGE :
           std::min(static_cast<std::size_t>(factor / decayRate), MAXIMUM_PERMITTED_AGE);
}

void CModel::prune(void)
{
    this->prune(this->defaultPruneWindow());
}

uint64_t CModel::checksum(bool /*includeCurrentBucketStats*/) const
{
    typedef std::map<TStrCRef, uint64_t, maths::COrderings::SLess> TStrCRefUInt64Map;
    uint64_t seed = m_DataGatherer->checksum();
    seed = maths::CChecksum::calculate(seed, m_Params);
    seed = maths::CChecksum::calculate(seed, m_BucketCount);
    TStrCRefUInt64Map hashes;
    for (std::size_t pid = 0u; pid < m_PersonBucketCounts.size(); ++pid)
    {
        if (m_DataGatherer->isPersonActive(pid))
        {
            uint64_t &hash = hashes[boost::cref(m_DataGatherer->personName(pid))];
            hash = maths::CChecksum::calculate(hash, m_PersonBucketCounts[pid]);
        }
    }
    LOG_TRACE("seed = " << seed);
    LOG_TRACE("checksums = " << core::CContainerPrinter::print(hashes));
    return maths::CChecksum::calculate(seed, hashes);
}

void CModel::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    // Note that the DataGatherer has shared ownership so its size
    // is accounted for in CModelEnsemble.

    mem->setName("CModel");
    core::CMemoryDebug::dynamicSize("m_Params", m_Params, mem);
    core::CMemoryDebug::dynamicSize("m_PersonBucketCounts", m_PersonBucketCounts, mem);
    core::CMemoryDebug::dynamicSize("m_Correlations", m_Correlations, mem);
    core::CMemoryDebug::dynamicSize("m_CorrelatedLookup", m_CorrelatedLookup, mem);
    core::CMemoryDebug::dynamicSize("m_InfluenceCalculators", m_InfluenceCalculators, mem);
    core::CMemory::CAnyVisitor &visitor = core::CMemory::anyVisitor();
    visitor.registerCallback<std::string>();
    if (!this->params().s_ExtraDataMemoryFunc.empty())
    {
        this->params().s_ExtraDataMemoryFunc(visitor);
    }
    core::CMemoryDebug::dynamicSize("m_InterimBucketCorrector", m_InterimBucketCorrector, mem);
    core::CMemoryDebug::dynamicSize("m_ExtraData", m_ExtraData, mem);
}

std::size_t CModel::memoryUsage(void) const
{
    // Note that the DataGatherer has shared ownership so its size
    // is accounted for in CModelEnsemble.

    std::size_t mem = core::CMemory::dynamicSize(m_Params);
    mem += core::CMemory::dynamicSize(m_PersonBucketCounts);
    mem += core::CMemory::dynamicSize(m_Correlations);
    mem += core::CMemory::dynamicSize(m_CorrelatedLookup);
    mem += core::CMemory::dynamicSize(m_InfluenceCalculators);
    core::CMemory::CAnyVisitor &visitor = core::CMemory::anyVisitor();
    visitor.registerCallback<std::string>();
    if (!this->params().s_ExtraDataMemoryFunc.empty())
    {
        this->params().s_ExtraDataMemoryFunc(visitor);
    }
    mem += core::CMemory::dynamicSize(m_InterimBucketCorrector);
    mem += core::CMemory::dynamicSize(m_ExtraData);
    return mem;
}

const CDataGatherer &CModel::dataGatherer(void) const
{
    return *m_DataGatherer;
}

CDataGatherer &CModel::dataGatherer(void)
{
    return *m_DataGatherer;
}

core_t::TTime CModel::bucketLength(void) const
{
    return m_DataGatherer->bucketLength();
}

const boost::any &CModel::extraData(std::size_t pid) const
{
    if (pid >= m_ExtraData.size())
    {
        return EMPTY_ANY;
    }
    return m_ExtraData[pid];
}

void CModel::extraData(std::size_t pid, boost::any &value)
{
    if (pid >= m_ExtraData.size())
    {
        core::CAllocationStrategy::resize(m_ExtraData, pid + 1);
    }
    m_ExtraData[pid].swap(value);
}

double CModel::personFrequency(std::size_t pid) const
{
    if (m_BucketCount <= 0.0)
    {
        return 0.0;
    }
    return m_PersonBucketCounts[pid] / m_BucketCount;
}

double CModel::winsorisingWeight(const maths::CPrior &prior,
                                 const maths_t::TWeightStyleVec &weightStyles,
                                 const TDouble1Vec &sample,
                                 const TDouble4Vec1Vec &weights,
                                 double derate)
{
    static const double WINSORISED_FRACTION = 1e-4;
    static const double MINIMUM_WEIGHT_FRACTION = 1e-16;
    static const double MINIMUM_WEIGHT = 0.05;
    static const double MINUS_LOG_TOLERANCE =
            -::log(1.0 - 100.0 * std::numeric_limits<double>::epsilon());

    double deratedMinimumWeight =  MINIMUM_WEIGHT
                                 + (0.5 - MINIMUM_WEIGHT)
                                   * maths::CTools::truncate(derate, 0.0, 1.0);

    double lowerBound;
    double upperBound;
    if (!prior.minusLogJointCdf(weightStyles,
                                sample,
                                weights,
                                lowerBound, upperBound))
    {
        return 1.0;
    }
    if (   upperBound < MINUS_LOG_TOLERANCE
        && !prior.minusLogJointCdfComplement(weightStyles,
                                             sample,
                                             weights,
                                             lowerBound, upperBound))
    {
        return 1.0;
    }

    double f = ::exp(-(lowerBound + upperBound) / 2.0);
    f = std::min(f, 1.0 - f);
    if (f >= WINSORISED_FRACTION)
    {
        return 1.0;
    }
    if (f <= MINIMUM_WEIGHT_FRACTION)
    {
        return deratedMinimumWeight;
    }

    // We interpolate between 1.0 and the minimum weight on the
    // interval [WINSORISED_FRACTION, MINIMUM_WEIGHT_FRACTION]
    // by fitting (f / WF)^(-c log(f)) where WF is the Winsorised
    // fraction and c is determined by solving:
    //   MW = (MWF / WF)^(-c log(MWF))

    static const double EXPONENT = -::log(MINIMUM_WEIGHT)
                                  / ::log(MINIMUM_WEIGHT_FRACTION)
                                  / ::log(MINIMUM_WEIGHT_FRACTION / WINSORISED_FRACTION);
    static const double LOG_WINSORISED_FRACTION = ::log(WINSORISED_FRACTION);

    double deratedExponent = EXPONENT;
    if (deratedMinimumWeight != MINIMUM_WEIGHT)
    {
        deratedExponent =  -::log(deratedMinimumWeight)
                          / ::log(MINIMUM_WEIGHT_FRACTION)
                          / ::log(MINIMUM_WEIGHT_FRACTION / WINSORISED_FRACTION);
    }

    double logf = log(f);
    double weight = ::exp(-deratedExponent * logf * (logf - LOG_WINSORISED_FRACTION));

    if (maths::CMathsFuncs::isNan(weight))
    {
        LOG_ERROR("winsorisingWeight was nan - forcing to 1:"
                  " sample = " << core::CContainerPrinter::print(sample) << ","
                  " lowerBound = " << lowerBound << ","
                  " upperBound = " << upperBound << ","
                  " f = " << f << ","
                  " weight = " << weight);
        return 1.0;
    }

    LOG_TRACE("sample = " << core::CContainerPrinter::print(sample)
              << " min(F, 1-F) = " << f
              << ", weight = " << weight);

    return weight;
}

CModel::TDouble10Vec CModel::winsorisingWeight(const maths::CMultivariatePrior &prior,
                                               const maths_t::TWeightStyleVec &weightStyles,
                                               const TDouble10Vec &sample,
                                               const TDouble10Vec4Vec &weights,
                                               double derate)
{
    typedef std::pair<std::size_t, double> TSizeDoublePr;
    typedef std::vector<TSizeDoublePr> TSizeDoublePrVec;

    std::size_t d = sample.size();

    TDouble10Vec result;
    result.reserve(d);

    // Declared outside the loop to minimize the number of times they are created.
    static const TSizeVec EMPTY;
    TSizeDoublePrVec condition(d - 1);
    TDouble1Vec component(1);
    TDouble4Vec1Vec weight(1, TDouble4Vec(weights.size()));

    for (std::size_t i = 0u; i < d; ++i)
    {
        for (std::size_t j = 0u, k = 0u; j < d; ++j)
        {
            if (j != i)
            {
                condition[k++] = std::make_pair(j, sample[j]);
            }
        }
        component[0] = sample[i];
        for (std::size_t j = 0u; j < weights.size(); ++j)
        {
            weight[0][j] = weights[j][i];
        }
        boost::shared_ptr<maths::CPrior> conditional(prior.univariate(EMPTY, condition).first);
        result.push_back(winsorisingWeight(*conditional, weightStyles, component, weight, derate));
    }

    LOG_TRACE("weights = " << core::CContainerPrinter::print(result));

    return result;
}

bool CModel::isTimeUnset(core_t::TTime time)
{
    return time == TIME_UNSET;
}

CPersonFrequencyGreaterThan CModel::personFilter(void) const
{
    return CPersonFrequencyGreaterThan(*this, m_Params.get().s_ExcludePersonFrequency);
}

CAttributeFrequencyGreaterThan CModel::attributeFilter(void) const
{
    return CAttributeFrequencyGreaterThan(*this, m_Params.get().s_ExcludeAttributeFrequency);
}

const SModelParams &CModel::params(void) const
{
    return m_Params;
}

double CModel::learnRate(model_t::EFeature feature) const
{
    return model_t::learnRate(feature, m_Params);
}

CModel::TFeatureKMostCorrelatedPrVec &CModel::correlations(void)
{
    return m_Correlations;
}

const maths::CKMostCorrelated *CModel::correlations(model_t::EFeature feature) const
{
    return const_cast<CModel*>(this)->correlations(feature);
}

maths::CKMostCorrelated *CModel::correlations(model_t::EFeature feature)
{
    TFeatureKMostCorrelatedPrVecItr result = std::lower_bound(m_Correlations.begin(),
                                                              m_Correlations.end(),
                                                              feature,
                                                              maths::COrderings::SFirstLess());
    return result != m_Correlations.end() && result->first == feature ? &result->second : 0;
}

void CModel::refreshCorrelated(model_t::EFeature feature,
                               const TSizeSizePrMultivariatePriorPtrDoublePrUMap &priors)
{
    TFeatureSizeSize1VecUMapPrVecItr lookup = std::lower_bound(m_CorrelatedLookup.begin(),
                                                               m_CorrelatedLookup.end(),
                                                               feature,
                                                               maths::COrderings::SFirstLess());
   if (lookup != m_CorrelatedLookup.end() && lookup->first == feature)
   {
       lookup->second.clear();
       for (TSizeSizePrMultivariatePriorPtrDoublePrUMapCItr i = priors.begin(); i != priors.end(); ++i)
       {
           lookup->second[i->first.first].push_back(i->first.second);
           lookup->second[i->first.second].push_back(i->first.first);
       }
       for (TSizeSize1VecUMapItr i = lookup->second.begin(); i != lookup->second.end(); ++i)
       {
           std::sort(i->second.begin(), i->second.end());
       }
   }
}

const CModel::TSize1Vec &CModel::correlated(model_t::EFeature feature, std::size_t id) const
{
    static const TSize1Vec EMPTY;
    TFeatureSizeSize1VecUMapPrVecCItr lookup = std::lower_bound(m_CorrelatedLookup.begin(),
                                                                m_CorrelatedLookup.end(),
                                                                feature,
                                                                maths::COrderings::SFirstLess());
    if (lookup != m_CorrelatedLookup.end() && lookup->first == feature)
    {
        TSizeSize1VecUMapCItr result = lookup->second.find(id);
        if (result != lookup->second.end())
        {
            return result->second;
        }
    }
    return EMPTY;
}

const CInfluenceCalculator *CModel::influenceCalculator(model_t::EFeature feature,
                                                        std::size_t iid) const
{
    if (iid >= m_InfluenceCalculators.size())
    {
        LOG_ERROR("Influencer identifier " << iid << " out of range");
        return 0;
    }
    const TFeatureInfluenceCalculatorCPtrPrVec &calculators = m_InfluenceCalculators[iid];
    TFeatureInfluenceCalculatorCPtrPrVecCItr result = std::lower_bound(calculators.begin(),
                                                                       calculators.end(),
                                                                       feature,
                                                                       maths::COrderings::SFirstLess());
    return result != calculators.end() && result->first == feature ? result->second.get() : 0;
}

CModel::TAnyVec &CModel::extraData(void)
{
    return m_ExtraData;
}

const CModel::TDoubleVec &CModel::personBucketCounts(void) const
{
    return m_PersonBucketCounts;
}

CModel::TDoubleVec &CModel::personBucketCounts(void)
{
    return m_PersonBucketCounts;
}

void CModel::windowBucketCount(double windowBucketCount)
{
    m_BucketCount = windowBucketCount;
}

double CModel::windowBucketCount(void) const
{
    return m_BucketCount;
}

void CModel::createNewModels(std::size_t n, std::size_t /*m*/)
{
    if (n > 0)
    {
        n += m_PersonBucketCounts.size();
        core::CAllocationStrategy::resize(m_PersonBucketCounts, n, 0.0);
        core::CAllocationStrategy::resize(this->extraData(), n);
    }
}

void CModel::updateRecycledModels(void)
{
    TSizeVec &recycledPeople = m_DataGatherer->recycledPersonIds();
    for (std::size_t i = 0u; i < recycledPeople.size(); ++i)
    {
        std::size_t pid = recycledPeople[i];
        m_PersonBucketCounts[pid] = 0.0;
        boost::any empty;
        m_ExtraData[pid].swap(empty);
    }
    recycledPeople.clear();
}

void CModel::clearPrunedResources(const TSizeVec &people,
                                  const TSizeVec &/*attributes*/)
{
    for (std::size_t i = 0u; i < people.size(); ++i)
    {
        std::size_t pid = people[i];
        if (pid < m_ExtraData.size())
        {
            boost::any empty;
            m_ExtraData[pid].swap(empty);
        }
    }
}

const CInterimBucketCorrector &CModel::interimValueCorrector(void) const
{
    return *m_InterimBucketCorrector;
}

bool CModel::shouldIgnoreResult(model_t::EFeature feature,
                                model_t::CResultType resultType,
                                const std::string &partitionFieldValue,
                                std::size_t pid,
                                std::size_t cid,
                                core_t::TTime time) const
{
    bool isIgnored = false;
    for (std::size_t i = 0; i < this->params().s_DetectionRules.get().size(); ++i)
    {
        isIgnored |= this->params().s_DetectionRules.get()[i].apply(
                CDetectionRule::E_FILTER_RESULTS,
                boost::cref(*this),
                feature,
                resultType,
                partitionFieldValue,
                pid,
                cid,
                time);
    }
    return isIgnored;
}

bool CModel::featurePriorAcceptRestoreTraverser(maths_t::EDataType dataType,
                                                const std::string &featureTag,
                                                const std::string &priorTag,
                                                core::CStateRestoreTraverser &traverser,
                                                TFeaturePriorPtrPrVec &result) const
{
    return restoreFeaturePriors(m_Params.get().distributionRestoreParams(dataType),
                                featureTag, priorTag, traverser, result);
}

bool CModel::featureMultivariatePriorAcceptRestoreTraverser(maths_t::EDataType dataType,
                                                            const std::string &featureTag,
                                                            const std::string &priorTag,
                                                            core::CStateRestoreTraverser &traverser,
                                                            TFeatureMultivariatePriorPtrPrVec &result) const
{
    return restoreFeaturePriors(m_Params.get().distributionRestoreParams(dataType),
                                featureTag, priorTag, traverser, result);
}

bool CModel::featurePriorsAcceptRestoreTraverser(maths_t::EDataType dataType,
                                                 const std::string &featureTag,
                                                 const std::string &priorTag,
                                                 core::CStateRestoreTraverser &traverser,
                                                 TFeaturePriorPtrVecMap &result) const
{
    maths::SDistributionRestoreParams params = m_Params.get().distributionRestoreParams(dataType);
    return restoreFeaturePriorsMap(params, featureTag, priorTag, traverser, result);
}

bool CModel::featureMultivariatePriorsAcceptRestoreTraverser(maths_t::EDataType dataType,
                                                             const std::string &featureTag,
                                                             const std::string &priorTag,
                                                             core::CStateRestoreTraverser &traverser,
                                                             TFeatureMultivariatePriorPtrVecMap &result) const
{
    return restoreFeaturePriorsMap(m_Params.get().distributionRestoreParams(dataType),
                                   featureTag, priorTag, traverser, result);
}

bool CModel::featureCorrelatePriorsAcceptRestoreTraverser(maths_t::EDataType dataType,
                                                          const std::string &featureTag,
                                                          const std::string &priorTag,
                                                          core::CStateRestoreTraverser &traverser,
                                                          TFeatureSizeSizePrMultivariatePriorPtrDoublePrUMapMap &result) const
{
    TSizeSizePrMultivariatePriorPtrDoublePrUMap *priors = 0;
    do
    {
        const std::string &name = traverser.name();
        if (name == featureTag)
        {
            int feature(-1);
            if (core::CStringUtils::stringToType(traverser.value(), feature) == false || feature < 0)
            {
                LOG_ERROR("Invalid feature in " << traverser.value());
                return false;
            }
            priors = &result[static_cast<model_t::EFeature>(feature)];
        }
        else if (name == priorTag)
        {
            if (priors == 0)
            {
                LOG_ERROR("Invalid XML: seen prior before feature");
                return false;
            }
            maths::SDistributionRestoreParams params = m_Params.get().distributionRestoreParams(dataType);
            if (traverser.traverseSubLevel(boost::bind<bool>(restoreCorrelatedPairPrior,
                                                             boost::cref(params),
                                                             boost::ref(*priors), _1)) == false)
            {
                LOG_ERROR("Invalid priors");
                return false;
            }
        }
    }
    while (traverser.next());

    return priors != 0;
}

bool CModel::featureDecompositionsAcceptRestoreTraverser(const std::string &featureTag,
                                                         const std::string &trendTag,
                                                         core::CStateRestoreTraverser &traverser,
                                                         TFeatureDecompositionPtr1VecVecPrVec &result) const
{
    std::size_t index = result.size();
    do
    {
        const std::string &name = traverser.name();
        RESTORE(featureTag, readFeature(traverser, index, result))
        if (name == trendTag)
        {
            if (index == result.size())
            {
                LOG_ERROR("Missing the feature");
                return false;
            }
            TDecompositionPtr1Vec trend;
            if (traverser.traverseSubLevel(boost::bind(&restoreMultivariateTrend,
                                                       m_Params.get().s_DecayRate,
                                                       this->bucketLength(),
                                                       boost::ref(trend), _1)) == false)
            {
                return false;
            }
            result[index].second.push_back(trend);
            continue;
        }
    }
    while (traverser.next());

    return true;
}

bool CModel::featureControllersAcceptRestoreTraverser(const std::string &featureTag,
                                                      const std::string &controllerTag,
                                                      core::CStateRestoreTraverser &traverser,
                                                      TFeatureDecayRateControllerArrayVecPrVec &result) const
{
    std::size_t index = result.size();
    do
    {
        const std::string &name = traverser.name();
        RESTORE(featureTag, readFeature(traverser, index, result))
        if (name == controllerTag)
        {
            if (index >= result.size())
            {
                LOG_ERROR("Missing the feature");
                return false;
            }
            if (core::CPersistUtils::restore(controllerTag, result[index].second, traverser) == false)
            {
                return false;
            }
            continue;
        }
    }
    while (traverser.next());

    return true;
}

bool CModel::featureCorrelationsAcceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    if (traverser.traverseSubLevel(boost::bind(&restoreCorrelation,
                                               boost::ref(m_Correlations), _1)) == false)
    {
        LOG_ERROR("Invalid correlations");
        return false;
    }
    return true;
}

bool CModel::interimBucketCorrectorAcceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    if (traverser.traverseSubLevel(boost::bind(&CInterimBucketCorrector::acceptRestoreTraverser,
                                               m_InterimBucketCorrector.get(), _1)) == false)
    {
        LOG_ERROR("Invalid interim bucket corrector");
        return false;
    }
    return true;
}

bool CModel::extraDataAcceptRestoreTraverser(const model_t::TAnyRestoreFunc &extraDataRestoreFunc,
                                             core::CStateRestoreTraverser &traverser)
{
    if (extraDataRestoreFunc.empty())
    {
        LOG_WARN("No conversion function for extra data in " << traverser.value());
    }
    else
    {
        m_ExtraData.push_back(extraDataRestoreFunc(traverser));
        if (m_ExtraData.back().empty())
        {
            LOG_ERROR("Invalid extra data in " << traverser.value());
            return false;
        }
    }
    return true;
}

void CModel::featurePriorAcceptPersistInserter(const std::string &featureTag,
                                               model_t::EFeature feature,
                                               const std::string &priorTag,
                                               const maths::CPrior &prior,
                                               core::CStatePersistInserter &inserter)
{
    inserter.insertValue(featureTag, static_cast<int>(feature));
    persistPrior(priorTag, prior, inserter);
}

void CModel::featureMultivariatePriorAcceptPersistInserter(const std::string &featureTag,
                                                           model_t::EFeature feature,
                                                           const std::string &priorTag,
                                                           const maths::CMultivariatePrior &prior,
                                                           core::CStatePersistInserter &inserter)
{
    inserter.insertValue(featureTag, static_cast<int>(feature));
    persistPrior(priorTag, prior, inserter);
}

void CModel::featurePriorsAcceptPersistInserter(const std::string &featureTag,
                                                model_t::EFeature feature,
                                                const std::string &priorTag,
                                                const TPriorPtrVec &priors,
                                                core::CStatePersistInserter &inserter)
{
    inserter.insertValue(featureTag, static_cast<int>(feature));
    persistPriors(priorTag, priors, inserter);
}

void CModel::featureMultivariatePriorsAcceptPersistInserter(const std::string &featureTag,
                                                            model_t::EFeature feature,
                                                            const std::string &priorTag,
                                                            const TMultivariatePriorPtrVec &priors,
                                                            core::CStatePersistInserter &inserter)
{
    inserter.insertValue(featureTag, static_cast<int>(feature));
    persistPriors(priorTag, priors, inserter);
}

void CModel::featureCorrelatePriorsAcceptPersistInserter(const std::string &featureTag,
                                                         model_t::EFeature feature,
                                                         const std::string &priorTag,
                                                         const TSizeSizePrMultivariatePriorPtrDoublePrUMap &priors,
                                                         core::CStatePersistInserter &inserter)
{
    inserter.insertValue(featureTag, static_cast<int>(feature));
    std::vector<TSizeSizePrMultivariatePriorPtrDoublePrUMapCItr> ordered;
    ordered.reserve(priors.size());
    for (TSizeSizePrMultivariatePriorPtrDoublePrUMapCItr i = priors.begin(); i != priors.end(); ++i)
    {
        ordered.push_back(i);
    }
    std::sort(ordered.begin(), ordered.end(),
              core::CFunctional::SDereference<maths::COrderings::SFirstLess>());
    for (std::size_t i = 0u; i < ordered.size(); ++i)
    {
        inserter.insertLevel(priorTag, boost::bind(&persistCorrelatedPairPrior,
                                                   boost::cref(*ordered[i]), _1));
    }
}

void CModel::featureDecompositionsAcceptPersistInserter(const std::string &featureTag,
                                                        model_t::EFeature feature,
                                                        const std::string &trendTag,
                                                        const TDecompositionPtr1VecVec &trends,
                                                        core::CStatePersistInserter &inserter)
{
    inserter.insertValue(featureTag, static_cast<int>(feature));
    for (std::size_t id = 0; id < trends.size(); ++id)
    {
        inserter.insertLevel(trendTag, boost::bind(&persistMultivariateTrend,
                                                   boost::cref(trends[id]), _1));
    }
}

void CModel::featureControllersAcceptPersistInserter(const std::string &featureTag,
                                                    model_t::EFeature feature,
                                                    const std::string &controllerTag,
                                                    const TDecayRateControllerArrayVec &controllers,
                                                    core::CStatePersistInserter &inserter)
{
    inserter.insertValue(featureTag, static_cast<int>(feature));
    core::CPersistUtils::persist(controllerTag, controllers, inserter);
}

void CModel::featureCorrelationsAcceptPersistInserter(const std::string &tag,
                                                      core::CStatePersistInserter &inserter) const
{
    for (std::size_t i = 0u; i < m_Correlations.size(); ++i)
    {
        inserter.insertLevel(tag, boost::bind(&persistCorrelation,
                                              boost::cref(m_Correlations[i]), _1));
    }
}

void CModel::interimBucketCorrectorAcceptPersistInserter(const std::string &tag,
                                                         core::CStatePersistInserter &inserter) const
{
    inserter.insertLevel(tag, boost::bind(&CInterimBucketCorrector::acceptPersistInserter,
                                          m_InterimBucketCorrector.get(), _1));
}

void CModel::extraDataAcceptPersistInserter(const std::string &tag,
                                            core::CStatePersistInserter &inserter) const
{
    const model_t::TAnyPersistFunc &extraDataPersistFunc = m_Params.get().s_ExtraDataPersistFunc;

    // It's valid to have no extra data
    if (!extraDataPersistFunc.empty())
    {
        for (std::size_t pid = 0; pid < m_ExtraData.size(); ++pid)
        {
            extraDataPersistFunc(tag, m_ExtraData[pid], inserter);
        }
    }
}

maths::CPrior *CModel::tinyPrior(void)
{
    return new maths::CConstantPrior;
}

maths::CMultivariatePrior *CModel::tinyPrior(std::size_t dimension)
{
    return new maths::CMultivariateConstantPrior(dimension);
}

maths::CTimeSeriesDecompositionInterface *CModel::tinyDecomposition(void)
{
    return new maths::CTimeSeriesDecompositionStub;
}

CModel::SOutputStats::SOutputStats(core_t::TTime time,
                                   bool isPopulationResult,
                                   bool isAllTimeResult,
                                   const std::string &partitionFieldName,
                                   const std::string &partitionFieldValue,
                                   const std::string &overFieldName,
                                   const std::string &overFieldValue,
                                   const std::string &byFieldName,
                                   const std::string &byFieldValue,
                                   const std::string &metricFieldName,
                                   const std::string &functionName,
                                   double functionValue,
                                   bool isInteger) :
        s_Time(time),
        s_IsPopulationResult(isPopulationResult),
        s_IsAllTimeResult(isAllTimeResult),
        s_PartitionFieldName(partitionFieldName),
        s_PartitionFieldValue(partitionFieldValue),
        s_OverFieldName(overFieldName),
        s_OverFieldValue(overFieldValue),
        s_ByFieldName(byFieldName),
        s_ByFieldValue(byFieldValue),
        s_MetricFieldName(metricFieldName),
        s_FunctionName(functionName),
        s_FunctionValue(functionValue),
        s_IsInteger(isInteger)
{
}

const std::size_t   CModel::MAXIMUM_PERMITTED_AGE(1000000);
const std::size_t   CModel::MAXIMUM_CORRELATIONS(5000);
const double        CModel::SEASONAL_CONFIDENCE_INTERVAL(50.0);
const double        CModel::MINIMUM_CORRELATE_PRIOR_SAMPLE_COUNT(24.0);
const core_t::TTime CModel::TIME_UNSET(-1);
const std::string   CModel::EMPTY_STRING;
const boost::any    CModel::EMPTY_ANY;

}
}
