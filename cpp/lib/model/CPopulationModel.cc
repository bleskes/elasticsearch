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

#include <model/CPopulationModel.h>

#include <core/CAllocationStrategy.h>
#include <core/CContainerPrinter.h>
#include <core/Constants.h>
#include <core/CoreTypes.h>
#include <core/CStatePersistInserter.h>
#include <core/RestoreMacros.h>

#include <maths/CChecksum.h>
#include <maths/COrderings.h>
#include <maths/CPrior.h>
#include <maths/CPriorStateSerialiser.h>
#include <maths/CTimeSeriesDecomposition.h>
#include <maths/CTimeSeriesDecompositionStateSerialiser.h>

#include <model/CDataGatherer.h>
#include <model/CModelTools.h>
#include <model/CResourceMonitor.h>

#include <boost/bind.hpp>

#include <algorithm>


namespace prelert
{
namespace model
{

namespace
{

typedef core::CSmallVector<double, 1> TDouble1Vec;
typedef maths::CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;
typedef core::CSmallVector<TMeanAccumulator, 1> TMeanAccumulator1Vec;
typedef std::map<CPopulationModel::TStrCRef, uint64_t, maths::COrderings::SLess> TStrCRefUInt64Map;

enum EEntity
{
    E_Person,
    E_Attribute
};

const std::string EMPTY;

//! Check if \p entity is active.
bool isActive(EEntity entity, const CDataGatherer &gatherer, std::size_t id)
{
    switch (entity)
    {
    case E_Person:    return gatherer.isPersonActive(id);
    case E_Attribute: return gatherer.isAttributeActive(id);
    }
    return false;
}

//! Get \p entity's name.
const std::string &name(EEntity entity, const CDataGatherer &gatherer, std::size_t id)
{
    switch (entity)
    {
    case E_Person:    return gatherer.personName(id);
    case E_Attribute: return gatherer.attributeName(id);
    }
    return EMPTY;
}

//! Update \p hashes with the hash of the active entities in \p values.
template<typename T>
void hashActive(EEntity entity,
                const CDataGatherer &gatherer,
                const std::vector<T> &values,
                TStrCRefUInt64Map &hashes)
{
    for (std::size_t id = 0u; id < values.size(); ++id)
    {
        if (isActive(entity, gatherer, id))
        {
            uint64_t &hash = hashes[boost::cref(name(entity, gatherer, id))];
            hash = maths::CChecksum::calculate(hash, values[id]);
        }
    }
}

//! Update \p hashes with the hash of the active entities in \p values.
template<typename T>
void hashActive(EEntity entity,
                const CDataGatherer &gatherer,
                const std::vector<std::pair<model_t::EFeature, std::vector<T> > > &values,
                TStrCRefUInt64Map &hashes)
{
    for (std::size_t i = 0u; i < values.size(); ++i)
    {
        hashActive(entity, gatherer, values[i].second, hashes);
    }
}

//! Extract the means from \p moments.
void means(const TMeanAccumulator1Vec &moments, TDouble1Vec &result)
{
    result.resize(moments.size());
    for (std::size_t i = 0u; i < moments.size(); ++i)
    {
        result[i] = maths::CBasicStatistics::mean(moments[i]);
    }
}

const std::size_t COUNT_MIN_SKETCH_ROWS = 3u;
const std::size_t COUNT_MIN_SKETCH_COLUMNS = 500u;
const std::size_t BJKST_HASHES = 3u;
const std::size_t BJKST_MAX_SIZE = 100u;
const std::size_t CHUNK_SIZE = 500u;

// We obfuscate the element names to avoid giving away too much
// information about our model
const std::string WINDOW_BUCKET_COUNT_TAG("a");
const std::string PERSON_BUCKET_COUNT_TAG("b");
const std::string NEW_FEATURE_PRIOR_TAG("c");
const std::string NEW_FEATURE_MULTIVARIATE_PRIOR_TAG("d");
const std::string PERSON_LAST_BUCKET_TIME_TAG("e");
const std::string ATTRIBUTE_FIRST_BUCKET_TIME_TAG("f");
const std::string ATTRIBUTE_LAST_BUCKET_TIME_TAG("g");
const std::string PERSON_ATTRIBUTE_BUCKET_COUNT_TAG("h");
const std::string DISTINCT_PERSON_COUNT_TAG("i");
const std::string FEATURE_DECOMPOSITION_TAG("j");
const std::string FEATURE_DECAY_RATE_CONTROLLER_TAG("k");
const std::string FEATURE_CORRELATIONS_TAG("l");
const std::string EXTRA_DATA_TAG("m");
const std::string INTERIM_BUCKET_CORRECTOR_TAG("n");

// Nested tags
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

CPopulationModel::CPopulationModel(const SModelParams &params,
                                   const TDataGathererPtr &dataGatherer,
                                   const TFeaturePriorPtrPrVec &newPriors,
                                   const TFeatureMultivariatePriorPtrPrVec &newMultivariatePriors,
                                   const TFeatureDecompositionCPtrVecPrVec &newDecompositions,
                                   const TFeatureInfluenceCalculatorCPtrPrVecVec &influenceCalculators,
                                   bool isForRestore) :
        CModel(params, dataGatherer, influenceCalculators, isForRestore),
        m_NewDecompositions(newDecompositions),
        m_NewDistinctPersonCounts(BJKST_HASHES, BJKST_MAX_SIZE)
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

    const model_t::TFeatureVec &features = dataGatherer->features();
    for (std::size_t i = 0u; i < features.size(); ++i)
    {
        if (!model_t::isCategorical(features[i]) && !model_t::isConstant(features[i]))
        {
            m_NewPersonBucketCounts.reset(maths::CCountMinSketch(COUNT_MIN_SKETCH_ROWS,
                                                                 COUNT_MIN_SKETCH_COLUMNS));
            break;
        }
    }
}

CPopulationModel::CPopulationModel(bool isForPersistence, const CPopulationModel &other) :
        CModel(isForPersistence, other),
        m_PersonLastBucketTimes(other.m_PersonLastBucketTimes),
        m_AttributeFirstBucketTimes(other.m_AttributeFirstBucketTimes),
        m_AttributeLastBucketTimes(other.m_AttributeLastBucketTimes),
        m_DecayRateControllers(other.m_DecayRateControllers),
        m_NewDistinctPersonCounts(BJKST_HASHES, BJKST_MAX_SIZE),
        m_DistinctPersonCounts(other.m_DistinctPersonCounts),
        m_PersonAttributeBucketCounts(other.m_PersonAttributeBucketCounts)
{
    if (!isForPersistence)
    {
        LOG_ABORT("This constructor only creates clones for persistence");
    }

    CModelTools::cloneVaryingNewPriors(other.m_NewFeaturePriors, m_NewFeaturePriors);
    CModelTools::cloneVaryingNewPriors(other.m_NewMultivariateFeaturePriors, m_NewMultivariateFeaturePriors);

    m_Decompositions.reserve(other.m_Decompositions.size());
    for (std::size_t i = 0u; i < other.m_Decompositions.size(); ++i)
    {
        m_Decompositions.push_back(
                TFeatureDecompositionPtr1VecVecPr(other.m_Decompositions[i].first,
                                                  TDecompositionPtr1VecVec()));
        const TDecompositionPtr1VecVec &otherDecompositions = other.m_Decompositions[i].second;
        TDecompositionPtr1VecVec &decompositions = m_Decompositions.back().second;
        decompositions.reserve(other.m_Decompositions.size());
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

bool CPopulationModel::isPopulation(void) const
{
    return true;
}

CPopulationModel::TOptionalUInt64
    CPopulationModel::currentBucketCount(std::size_t pid,
                                         core_t::TTime time) const
{
    typedef TSizeUInt64PrVec::const_iterator TSizeUInt64PrVecCItr;

    if (!this->bucketStatsAvailable(time))
    {
        LOG_ERROR("No statistics at " << time);
        return TOptionalUInt64();
    }

    const TSizeUInt64PrVec &personCounts = this->personCounts();
    TSizeUInt64PrVecCItr itr =
            std::lower_bound(personCounts.begin(),
                             personCounts.end(),
                             pid, maths::COrderings::SFirstLess());
    return (itr != personCounts.end() && itr->first == pid) ?
           TOptionalUInt64(itr->second) :
           TOptionalUInt64();
}

CPopulationModel::TOptionalDouble
    CPopulationModel::baselineBucketCount(std::size_t /*pid*/) const
{
    return TOptionalDouble();
}

void CPopulationModel::currentBucketPersonIds(core_t::TTime time,
                                              TSizeVec &result) const
{
    result.clear();
    if (!this->bucketStatsAvailable(time))
    {
        LOG_ERROR("No statistics at " << time);
        return;
    }

    const TSizeUInt64PrVec &personCounts = this->personCounts();
    result.reserve(personCounts.size());
    for (std::size_t i = 0u; i < personCounts.size(); ++i)
    {
        result.push_back(personCounts[i].first);
    }
}

void CPopulationModel::sampleOutOfPhase(core_t::TTime startTime,
                                        core_t::TTime endTime,
                                        CResourceMonitor &resourceMonitor)
{
    CDataGatherer &gatherer = this->dataGatherer();

    if (!gatherer.dataAvailable(startTime))
    {
        return;
    }

    for (core_t::TTime bucketStartTime = startTime, bucketLength = gatherer.bucketLength();
         bucketStartTime < endTime;
         bucketStartTime += bucketLength)
    {
        gatherer.sampleNow(bucketStartTime);
        this->sampleBucketStatistics(bucketStartTime, bucketStartTime + bucketLength, resourceMonitor);
    }
}

void CPopulationModel::sample(core_t::TTime startTime,
                              core_t::TTime endTime,
                              CResourceMonitor &resourceMonitor)
{
    typedef CDataGatherer::TSizeSizePrUInt64UMap TSizeSizePrUInt64UMap;
    typedef CDataGatherer::TSizeSizePrUInt64UMapCItr TSizeSizePrUInt64UMapCItr;

    this->CModel::sample(startTime, endTime, resourceMonitor);

    // Refresh the new feature priors to account for this bucket.
    CModelTools::updateNewPriorsWithEmptyBucket(this->params(), m_NewFeaturePriors);
    CModelTools::updateNewPriorsWithEmptyBucket(this->params(), m_NewMultivariateFeaturePriors);

    const CDataGatherer &gatherer = this->dataGatherer();
    const TSizeSizePrUInt64UMap &counts = gatherer.bucketCounts(startTime);
    for (TSizeSizePrUInt64UMapCItr i = counts.begin();
         i != counts.end();
         ++i)
    {
        std::size_t pid = CDataGatherer::extractPersonId(*i);
        std::size_t cid = CDataGatherer::extractAttributeId(*i);
        m_PersonLastBucketTimes[pid] = startTime;
        if (CModel::isTimeUnset(m_AttributeFirstBucketTimes[cid]))
        {
            m_AttributeFirstBucketTimes[cid] = startTime;
        }
        m_AttributeLastBucketTimes[cid] = startTime;
        m_DistinctPersonCounts[cid].add(static_cast<int32_t>(pid));
        if (cid < m_PersonAttributeBucketCounts.size())
        {
            m_PersonAttributeBucketCounts[cid].add(static_cast<int32_t>(pid), 1.0);
        }
    }

    double alpha = ::exp(-this->params().s_DecayRate * 1.0);
    for (std::size_t cid = 0u; cid < m_PersonAttributeBucketCounts.size(); ++cid)
    {
        m_PersonAttributeBucketCounts[cid].age(alpha);
    }
}

template<typename PRIORS>
void CPopulationModel::checksumsImpl(const PRIORS &priors,
                                     TStrCRefStrCRefPrUInt64Map &checksums) const
{
    const CDataGatherer &gatherer = this->dataGatherer();
    for (typename PRIORS::const_iterator i = priors.begin();
         i != priors.end();
         ++i)
    {
        const typename PRIORS::mapped_type &featurePriors = i->second;
        for (std::size_t cid = 0u; cid < featurePriors.size(); ++cid)
        {
            if (gatherer.isAttributeActive(cid))
            {
                uint64_t &checksum = checksums[TStrCRefStrCRefPr(
                                                   TStrCRef(EMPTY_STRING),
                                                   TStrCRef(this->attributeName(cid)))];
                checksum = maths::CChecksum::calculate(checksum, featurePriors[cid]);
            }
        }
    }
}

void CPopulationModel::checksums(const TFeaturePriorPtrVecMap &populationPriors,
                                 TStrCRefStrCRefPrUInt64Map &checksums) const
{
    this->checksumsImpl(populationPriors, checksums);
}

void CPopulationModel::checksums(const TFeatureMultivariatePriorPtrVecMap &populationMultivariatePriors,
                                 TStrCRefStrCRefPrUInt64Map &checksums) const
{
    this->checksumsImpl(populationMultivariatePriors, checksums);
}

void CPopulationModel::checksums(const TSizeUInt64PrVec &personCounts,
                                 TStrCRefStrCRefPrUInt64Map &checksums) const
{
    for (std::size_t i = 0u; i < personCounts.size(); ++i)
    {
        uint64_t &checksum = checksums[TStrCRefStrCRefPr(
                                           TStrCRef(this->personName(personCounts[i].first)),
                                           TStrCRef(EMPTY_STRING))];
        checksum = maths::CChecksum::calculate(checksum, personCounts[i].second);
    }
}

uint64_t CPopulationModel::checksum(bool includeCurrentBucketStats) const
{
    uint64_t seed = this->CModel::checksum(includeCurrentBucketStats);
    seed = maths::CChecksum::calculate(seed, m_NewFeaturePriors.size());
    seed = maths::CChecksum::calculate(seed, m_NewMultivariateFeaturePriors.size());

    const CDataGatherer &gatherer = this->dataGatherer();
    TStrCRefUInt64Map hashes;
    hashActive(E_Attribute, gatherer, m_Decompositions, hashes);
    hashActive(E_Person,    gatherer, m_PersonLastBucketTimes, hashes);
    hashActive(E_Attribute, gatherer, m_AttributeFirstBucketTimes, hashes);
    hashActive(E_Attribute, gatherer, m_AttributeLastBucketTimes, hashes);
    hashActive(E_Attribute, gatherer, m_DecayRateControllers, hashes);

    LOG_TRACE("seed = " << seed);
    LOG_TRACE("hashes = " << core::CContainerPrinter::print(hashes));

    return maths::CChecksum::calculate(seed, hashes);
}

void CPopulationModel::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CPopulationModel");
    this->CModel::debugMemoryUsage(mem->addChild());
    core::CMemoryDebug::dynamicSize("m_PersonLastBucketTimes", m_PersonLastBucketTimes, mem);
    core::CMemoryDebug::dynamicSize("m_AttributeFirstBucketTimes", m_AttributeFirstBucketTimes, mem);
    core::CMemoryDebug::dynamicSize("m_AttributeLastBucketTimes", m_AttributeLastBucketTimes, mem);
    CModelTools::debugNewPriorsMemoryUsage("m_NewFeaturePriors", m_NewFeaturePriors, mem);
    CModelTools::debugNewPriorsMemoryUsage("m_NewMultivariateFeaturePriors", m_NewMultivariateFeaturePriors, mem);
    core::CMemoryUsage::SMemoryUsage usage(  std::string("m_NewDecompositions::")
                                           + typeid(TFeatureDecompositionCPtrVecPr).name(),
                                           sizeof(TFeatureDecompositionCPtrVecPr) *  m_NewDecompositions.capacity(),
                                           sizeof(TFeatureDecompositionCPtrVecPr) * (  m_NewDecompositions.capacity()
                                                                                     - m_NewDecompositions.size()));
    mem->addChild()->setName(usage);
    core::CMemoryDebug::dynamicSize("m_Decompositions", m_Decompositions, mem);
    core::CMemoryDebug::dynamicSize("m_NewDistinctPersonCounts", m_NewDistinctPersonCounts, mem);
    core::CMemoryDebug::dynamicSize("m_DistinctPersonCounts", m_DistinctPersonCounts, mem);
    core::CMemoryDebug::dynamicSize("m_NewPersonBucketCounts", m_NewPersonBucketCounts, mem);
    core::CMemoryDebug::dynamicSize("m_PersonAttributeBucketCounts", m_PersonAttributeBucketCounts, mem);
    core::CMemoryDebug::dynamicSize("m_DecayRateControllers", m_DecayRateControllers, mem);
}

std::size_t CPopulationModel::memoryUsage(void) const
{
    std::size_t mem = this->CModel::memoryUsage();
    mem += core::CMemory::dynamicSize(m_PersonLastBucketTimes);
    mem += core::CMemory::dynamicSize(m_AttributeFirstBucketTimes);
    mem += core::CMemory::dynamicSize(m_AttributeLastBucketTimes);
    mem += CModelTools::newPriorsMemoryUsage(m_NewFeaturePriors);
    mem += CModelTools::newPriorsMemoryUsage(m_NewMultivariateFeaturePriors);
    mem += sizeof(TFeatureDecompositionCPtrVecPr) * m_NewDecompositions.capacity();
    mem += core::CMemory::dynamicSize(m_Decompositions);
    mem += core::CMemory::dynamicSize(m_NewDistinctPersonCounts);
    mem += core::CMemory::dynamicSize(m_DistinctPersonCounts);
    mem += core::CMemory::dynamicSize(m_NewPersonBucketCounts);
    mem += core::CMemory::dynamicSize(m_PersonAttributeBucketCounts);
    mem += core::CMemory::dynamicSize(m_DecayRateControllers);
    return mem;
}

const maths::CPrior *CPopulationModel::newPrior(model_t::EFeature feature) const
{
    return const_cast<CPopulationModel*>(this)->newPrior(feature);
}

maths::CPrior *CPopulationModel::newPrior(model_t::EFeature feature)
{
    TFeaturePriorPtrPrVecCItr itr =
            std::lower_bound(m_NewFeaturePriors.begin(),
                             m_NewFeaturePriors.end(),
                             feature,
                             maths::COrderings::SFirstLess());
    return itr == m_NewFeaturePriors.end() || itr->first != feature ? 0 : itr->second.get();
}
const maths::CMultivariatePrior *CPopulationModel::newMultivariatePrior(model_t::EFeature feature) const
{
    return const_cast<CPopulationModel*>(this)->newMultivariatePrior(feature);
}

maths::CMultivariatePrior *CPopulationModel::newMultivariatePrior(model_t::EFeature feature)
{
    TFeatureMultivariatePriorPtrPrVecCItr itr =
            std::lower_bound(m_NewMultivariateFeaturePriors.begin(),
                             m_NewMultivariateFeaturePriors.end(),
                             feature,
                             maths::COrderings::SFirstLess());
    return itr == m_NewMultivariateFeaturePriors.end() || itr->first != feature ? 0 : itr->second.get();
}

double CPopulationModel::attributeFrequency(std::size_t cid) const
{
    return   static_cast<double>(m_DistinctPersonCounts[cid].number())
           / static_cast<double>(this->dataGatherer().numberActivePeople());
}

double CPopulationModel::sampleRateWeight(std::size_t pid,
                                          std::size_t cid) const
{
    if (   cid >= m_PersonAttributeBucketCounts.size()
        || cid >= m_DistinctPersonCounts.size())
    {
        return 1.0;
    }

    const maths::CCountMinSketch &counts = m_PersonAttributeBucketCounts[cid];
    const maths::CBjkstUniqueValues &distinctPeople = m_DistinctPersonCounts[cid];

    double personCount =  counts.count(static_cast<uint32_t>(pid))
                        - counts.oneMinusDeltaError();
    if (personCount <= 0.0)
    {
        return 1.0;
    }
    LOG_TRACE("personCount = " << personCount);

    double totalCount = counts.totalCount();
    double distinctPeopleCount =
            std::min(static_cast<double>(distinctPeople.number()),
                     static_cast<double>(this->dataGatherer().numberActivePeople()));
    double meanPersonCount = totalCount / distinctPeopleCount;
    LOG_TRACE("meanPersonCount = " << meanPersonCount);

    return std::min(meanPersonCount / personCount, 1.0);
}

void CPopulationModel::doAcceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertValue(WINDOW_BUCKET_COUNT_TAG,
                         this->windowBucketCount(),
                         core::CIEEE754::E_SinglePrecision);
    core::CPersistUtils::persist(PERSON_BUCKET_COUNT_TAG, this->personBucketCounts(), inserter);
    persistNewPriors(NEW_FEATURE_PRIOR_TAG,
                     &featurePriorAcceptPersistInserter,
                     m_NewFeaturePriors,
                     inserter);
    persistNewPriors(NEW_FEATURE_MULTIVARIATE_PRIOR_TAG,
                     &featureMultivariatePriorAcceptPersistInserter,
                     m_NewMultivariateFeaturePriors,
                     inserter);
    core::CPersistUtils::persist(PERSON_LAST_BUCKET_TIME_TAG, m_PersonLastBucketTimes, inserter);
    core::CPersistUtils::persist(ATTRIBUTE_FIRST_BUCKET_TIME_TAG, m_AttributeFirstBucketTimes, inserter);
    core::CPersistUtils::persist(ATTRIBUTE_LAST_BUCKET_TIME_TAG, m_AttributeLastBucketTimes, inserter);
    for (std::size_t cid = 0; cid < m_PersonAttributeBucketCounts.size(); ++cid)
    {
        inserter.insertLevel(PERSON_ATTRIBUTE_BUCKET_COUNT_TAG,
                             boost::bind(&maths::CCountMinSketch::acceptPersistInserter,
                                         &m_PersonAttributeBucketCounts[cid],
                                         _1));
    }
    for (std::size_t cid = 0; cid < m_DistinctPersonCounts.size(); ++cid)
    {
        inserter.insertLevel(DISTINCT_PERSON_COUNT_TAG,
                             boost::bind(&maths::CBjkstUniqueValues::acceptPersistInserter,
                                         &m_DistinctPersonCounts[cid],
                                         _1));
    }
    persistValues(FEATURE_DECOMPOSITION_TAG,
                  &featureDecompositionsAcceptPersistInserter,
                  m_Decompositions,
                  inserter);
    persistValues(FEATURE_DECAY_RATE_CONTROLLER_TAG,
                  &featureControllersAcceptPersistInserter,
                  m_DecayRateControllers, inserter);
    this->featureCorrelationsAcceptPersistInserter(FEATURE_CORRELATIONS_TAG, inserter);
    this->extraDataAcceptPersistInserter(EXTRA_DATA_TAG, inserter);
    this->interimBucketCorrectorAcceptPersistInserter(INTERIM_BUCKET_CORRECTOR_TAG, inserter);
}

bool CPopulationModel::doAcceptRestoreTraverser(const model_t::TAnyRestoreFunc &extraDataRestoreFunc,
                                                core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        RESTORE_SETUP_TEARDOWN(WINDOW_BUCKET_COUNT_TAG,
                               double count,
                               core::CStringUtils::stringToType(traverser.value(), count),
                               this->windowBucketCount(count));
        RESTORE(PERSON_BUCKET_COUNT_TAG,
                core::CPersistUtils::restore(name, this->personBucketCounts(), traverser))
        RESTORE(PERSON_LAST_BUCKET_TIME_TAG,
                core::CPersistUtils::restore(name, m_PersonLastBucketTimes, traverser))
        RESTORE(ATTRIBUTE_FIRST_BUCKET_TIME_TAG,
                core::CPersistUtils::restore(name, m_AttributeFirstBucketTimes, traverser))
        RESTORE(ATTRIBUTE_LAST_BUCKET_TIME_TAG,
                core::CPersistUtils::restore(name, m_AttributeLastBucketTimes, traverser))
        if (name == PERSON_ATTRIBUTE_BUCKET_COUNT_TAG)
        {
            maths::CCountMinSketch sketch(traverser);
            m_PersonAttributeBucketCounts.push_back(maths::CCountMinSketch(0, 0));
            m_PersonAttributeBucketCounts.back().swap(sketch);
            continue;
        }
        if (name == DISTINCT_PERSON_COUNT_TAG)
        {
            maths::CBjkstUniqueValues sketch(traverser);
            m_DistinctPersonCounts.push_back(maths::CBjkstUniqueValues(0, 0));
            m_DistinctPersonCounts.back().swap(sketch);
            continue;
        }
        RESTORE(NEW_FEATURE_PRIOR_TAG,
                traverser.traverseSubLevel(boost::bind(&CPopulationModel::featurePriorAcceptRestoreTraverser,
                                                       this, this->isMetric() ? maths_t::E_ContinuousData : maths_t::E_IntegerData,
                                                       boost::cref(FEATURE_TAG),
                                                       boost::cref(VALUE_TAG),
                                                       _1, boost::ref(m_NewFeaturePriors))))
        RESTORE(NEW_FEATURE_MULTIVARIATE_PRIOR_TAG,
                traverser.traverseSubLevel(boost::bind(&CPopulationModel::featureMultivariatePriorAcceptRestoreTraverser,
                                                       this, this->isMetric() ? maths_t::E_ContinuousData : maths_t::E_IntegerData,
                                                       boost::cref(FEATURE_TAG),
                                                       boost::cref(VALUE_TAG),
                                                       _1, boost::ref(m_NewMultivariateFeaturePriors))))
        RESTORE(FEATURE_DECOMPOSITION_TAG,
                traverser.traverseSubLevel(boost::bind(&CPopulationModel::featureDecompositionsAcceptRestoreTraverser,
                                                       this,
                                                       boost::cref(FEATURE_TAG),
                                                       boost::cref(VALUE_TAG),
                                                       _1, boost::ref(m_Decompositions))))
        RESTORE(FEATURE_DECAY_RATE_CONTROLLER_TAG,
                traverser.traverseSubLevel(boost::bind(&CPopulationModel::featureControllersAcceptRestoreTraverser,
                                                       this,
                                                       boost::cref(FEATURE_TAG),
                                                       boost::cref(VALUE_TAG),
                                                       _1, boost::ref(m_DecayRateControllers))))
        RESTORE(FEATURE_CORRELATIONS_TAG, this->featureCorrelationsAcceptRestoreTraverser(traverser))
        RESTORE(EXTRA_DATA_TAG, this->extraDataAcceptRestoreTraverser(extraDataRestoreFunc, traverser))
        RESTORE(INTERIM_BUCKET_CORRECTOR_TAG, this->interimBucketCorrectorAcceptRestoreTraverser(traverser))
    }
    while (traverser.next());

    return true;
}

void CPopulationModel::personFeaturePriorAcceptPersistInserter(const std::string &featureTag,
                                                               const std::string &personTag,
                                                               const std::string &attributeTag,
                                                               const std::string &priorTag,
                                                               const TFeatureSizePriorPtrUMapVecMapCItr &iter,
                                                               core::CStatePersistInserter &inserter)
{
    model_t::EFeature feature(iter->first);
    inserter.insertValue(featureTag, static_cast<int>(feature));
    const TSizePriorPtrUMapVec &personData = iter->second;
    for (size_t index = 0; index < personData.size(); ++index)
    {
        inserter.insertLevel(personTag,
                             boost::bind(&CPopulationModel::attributePriorAcceptPersistInserter,
                                         boost::cref(attributeTag),
                                         boost::cref(priorTag),
                                         boost::cref(personData[index]),
                                         _1));
    }
}

void CPopulationModel::attributePriorAcceptPersistInserter(const std::string &attributeTag,
                                                           const std::string &priorTag,
                                                           const TSizePriorPtrUMap &attributeData,
                                                           core::CStatePersistInserter &inserter)
{
    // Persist the attribute IDs in sorted order to make it easier to
    // compare state records

    TSizeVec orderedAttributeData;
    orderedAttributeData.reserve(attributeData.size());
    for (TSizePriorPtrUMapCItr iter = attributeData.begin();
         iter != attributeData.end();
         ++iter)
    {
        orderedAttributeData.push_back(iter->first);
    }
    std::sort(orderedAttributeData.begin(), orderedAttributeData.end());

    for (std::size_t i = 0u; i < orderedAttributeData.size(); ++i)
    {
        std::size_t cid = orderedAttributeData[i];
        TSizePriorPtrUMapCItr attributeIter = attributeData.find(cid);
        if (attributeIter == attributeData.end())
        {
            LOG_ERROR("Inconsistency - attribute " << cid << " not found");
            continue;
        }
        const TPriorPtr &prior = attributeIter->second;
        if (prior == 0)
        {
            LOG_ERROR("Unexpected NULL pointer for attribute " << cid);
            continue;
        }
        inserter.insertValue(attributeTag, cid);
        inserter.insertLevel(priorTag,
                             boost::bind<void>(maths::CPriorStateSerialiser(),
                                               boost::cref(*prior),
                                               _1));
    }
}

void CPopulationModel::createUpdateNewModels(core_t::TTime time,
                                             CResourceMonitor &resourceMonitor)
{
    this->updateRecycledModels();

    CDataGatherer &gatherer = this->dataGatherer();

    std::size_t numberExistingPeople = m_PersonLastBucketTimes.size();
    std::size_t numberExistingAttributes = m_AttributeLastBucketTimes.size();
    std::size_t ourUsage = this->estimateMemoryUsage(std::min(numberExistingPeople,
                                                              gatherer.numberActivePeople()),
                                                     std::min(numberExistingAttributes,
                                                              gatherer.numberActiveAttributes()),
                                                     0); // # correlations
    std::size_t resourceLimit = ourUsage + resourceMonitor.allocationLimit();
    std::size_t numberNewPeople = gatherer.numberPeople();
    numberNewPeople = numberNewPeople > numberExistingPeople ?
                      numberNewPeople - numberExistingPeople : 0;
    std::size_t numberNewAttributes = gatherer.numberAttributes();
    numberNewAttributes = numberNewAttributes > numberExistingAttributes ?
                          numberNewAttributes - numberExistingAttributes : 0;

    if (resourceMonitor.areAllocationsAllowed())
    {
        // Create the new models in stages, people first then attributes

        // If there are fewer than 500 people/attributes to be created,
        // and allocations are allowed, then go ahead and create them
        // For more than 500, create models in chunks and test for usage
        // after each chunk

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
                    ourUsage = this->estimateMemoryUsage(numberExistingPeople, numberExistingAttributes, 0);
                    numberNewPeople -= numberToCreate;
                }
            }
        }

        if (numberNewAttributes > 0)
        {
            if (numberNewAttributes < CHUNK_SIZE || resourceMonitor.haveNoLimit())
            {
                LOG_TRACE("Creating " << numberNewAttributes << " new attributes");
                this->createNewModels(0, numberNewAttributes);
                numberNewAttributes = 0;
            }
            else
            {
                while (numberNewAttributes > 0 && ourUsage < resourceLimit)
                {
                    std::size_t numberToCreate = std::min(numberNewAttributes, CHUNK_SIZE);
                    LOG_TRACE("Creating batch of " << numberToCreate << " attributes. "
                              << resourceLimit - ourUsage << " free bytes remaining");
                    this->createNewModels(0, numberToCreate);
                    numberExistingAttributes += numberToCreate;
                    ourUsage = this->estimateMemoryUsage(numberExistingPeople, numberExistingAttributes, 0);
                    numberNewAttributes -= numberToCreate;
                }
            }
        }
    }

    if (numberNewPeople > 0)
    {
        resourceMonitor.acceptAllocationFailureResult(time);
        LOG_DEBUG("Not enough memory to create person models");
        core::CStatistics::instance().stat(stat_t::E_NumberMemoryLimitModelCreationFailures).
            increment(numberNewPeople);
        std::size_t toRemove = gatherer.numberPeople() - numberNewPeople;
        gatherer.removePeople(toRemove);
    }
    if (numberNewAttributes > 0)
    {
        resourceMonitor.acceptAllocationFailureResult(time);
        LOG_DEBUG("Not enough memory to create attribute models");
        core::CStatistics::instance().stat(stat_t::E_NumberMemoryLimitModelCreationFailures).
            increment(numberNewAttributes);
        std::size_t toRemove = gatherer.numberAttributes() - numberNewAttributes;
        gatherer.removeAttributes(toRemove);
    }

    this->refreshCorrelationModels(resourceLimit, resourceMonitor);
}

void CPopulationModel::createNewModels(std::size_t n, std::size_t m)
{
    if (n > 0)
    {
        core::CAllocationStrategy::resize(m_PersonLastBucketTimes,
                                          n + m_PersonLastBucketTimes.size(),
                                          CModel::TIME_UNSET);
    }

    if (m > 0)
    {
        std::size_t newM = m + m_AttributeFirstBucketTimes.size();
        core::CAllocationStrategy::resize(m_AttributeFirstBucketTimes, newM, CModel::TIME_UNSET);
        core::CAllocationStrategy::resize(m_AttributeLastBucketTimes, newM, CModel::TIME_UNSET);
        core::CAllocationStrategy::resize(m_DistinctPersonCounts, newM, m_NewDistinctPersonCounts);
        if (m_NewPersonBucketCounts)
        {
            core::CAllocationStrategy::resize(m_PersonAttributeBucketCounts, newM, *m_NewPersonBucketCounts);
        }
        for (std::size_t i = 0u; i < m_NewDecompositions.size(); ++i)
        {
            model_t::EFeature feature = m_NewDecompositions[i].first;
            const TDecompositionCPtrVec &newDecompositions = m_NewDecompositions[i].second;
            TDecompositionPtr1VecVec &trends = m_Decompositions[i].second;
            std::size_t mi = model_t::isAttributeConditional(feature) ? newM : 1u;
            if (mi > trends.size())
            {
                std::size_t cid = trends.size();
                core::CAllocationStrategy::resize(trends, mi);
                for (/**/; cid < trends.size(); ++cid)
                {
                    trends[cid].reserve(newDecompositions.size());
                    for (std::size_t j = 0u; j < newDecompositions.size(); ++j)
                    {
                        trends[cid].push_back(TDecompositionPtr(newDecompositions[j]->clone()));
                    }
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
            core::CAllocationStrategy::resize(m_DecayRateControllers[i].second, newM, initial);
        }
    }

    this->CModel::createNewModels(n, m);
}

void CPopulationModel::reinitializeAttributePriors(const TSizeVec &attributes,
                                                   TFeaturePriorPtrVecMap &priors,
                                                   TFeatureMultivariatePriorPtrVecMap &multivariatePriors)
{
    for (std::size_t i = 0u; i < attributes.size(); ++i)
    {
        CModelTools::resetRecycledPriors(attributes[i], m_NewFeaturePriors, priors);
        CModelTools::resetRecycledPriors(attributes[i], m_NewMultivariateFeaturePriors, multivariatePriors);
    }
}

void CPopulationModel::clearAttributePriors(const TSizeVec &attributes,
                                            TFeaturePriorPtrVecMap &priors,
                                            TFeatureMultivariatePriorPtrVecMap &multivariatePriors)
{
    for (std::size_t i = 0u; i < attributes.size(); ++i)
    {
        std::size_t cid = attributes[i];
        for (TFeaturePriorPtrVecMapItr j = priors.begin(); j != priors.end(); ++j)
        {
            if (cid < j->second.size())
            {
                j->second[cid].reset(this->tinyPrior());
            }
        }
        for (TFeatureMultivariatePriorPtrVecMapItr j = multivariatePriors.begin();
             j != multivariatePriors.end();
             ++j)
        {
            if (cid < j->second.size())
            {
                j->second[cid].reset(this->tinyPrior(j->second[cid]->dimension()));
            }
        }
    }
}

void CPopulationModel::updateRecycledModels(void)
{
    CDataGatherer &gatherer = this->dataGatherer();
    const TSizeVec &recycledPeople = gatherer.recycledPersonIds();
    for (std::size_t i = 0u; i < recycledPeople.size(); ++i)
    {
        std::size_t pid = recycledPeople[i];
        m_PersonLastBucketTimes[pid] = 0;
    }

    TSizeVec &recycledAttributes = gatherer.recycledAttributeIds();
    for (std::size_t i = 0u; i < recycledAttributes.size(); ++i)
    {
        std::size_t cid = recycledAttributes[i];
        m_AttributeFirstBucketTimes[cid] = CModel::TIME_UNSET;
        m_AttributeLastBucketTimes[cid] = CModel::TIME_UNSET;
        for (std::size_t j = 0u; j < m_Decompositions.size(); ++j)
        {
            const TDecompositionCPtrVec &newDecompositions = m_NewDecompositions[j].second;
            TDecompositionPtr1VecVec &trends = m_Decompositions[j].second;
            if (cid < trends.size())
            {
                for (std::size_t k = 0u; k < trends[cid].size(); ++k)
                {
                    trends[cid][k].reset(newDecompositions[k]->clone());
                }
            }
        }
        for (std::size_t j = 0u; j < m_DecayRateControllers.size(); ++j)
        {
            std::size_t dimension = model_t::dimension(m_DecayRateControllers[j].first);
            if (cid < m_DecayRateControllers[j].second.size())
            {
                std::fill_n(m_DecayRateControllers[j].second[cid].begin(),
                            static_cast<std::size_t>(E_NumberControls),
                            CDecayRateController(dimension));
            }
        }
        m_DistinctPersonCounts[cid] = m_NewDistinctPersonCounts;
        if (m_NewPersonBucketCounts)
        {
            m_PersonAttributeBucketCounts[cid] = *m_NewPersonBucketCounts;
        }
    }
    recycledAttributes.clear();

    this->CModel::updateRecycledModels();
}

void CPopulationModel::clearPrunedResources(const TSizeVec &people,
                                            const TSizeVec &attributes)
{
    for (std::size_t i = 0u; i < attributes.size(); ++i)
    {
        std::size_t cid = attributes[i];
        for (std::size_t j = 0u; j < m_Decompositions.size(); ++j)
        {
            TDecompositionPtr1VecVec &trends = m_Decompositions[j].second;
            if (cid < trends.size())
            {
                for (std::size_t k = 0u; k < trends[cid].size(); ++k)
                {
                    trends[cid][k].reset(this->tinyDecomposition());
                }
            }
        }
    }
    this->CModel::clearPrunedResources(people, attributes);
}

void CPopulationModel::refreshCorrelationModels(std::size_t /*resourceLimit*/,
                                                CResourceMonitor &/*resourceMonitor*/)
{
    // TODO
}

void CPopulationModel::updatePriors(const maths_t::TWeightStyleVec &weightStyles,
                                    TFeatureSizeFeatureSampleDataUMapMap &data,
                                    TFeaturePriorPtrVecMap &priors,
                                    TFeatureMultivariatePriorPtrVecMap &multivariatePriors)
{
    TDouble1Vec prediction;
    TMeanAccumulator1Vec residuals[2];
    TDouble1Vec residual;

    for (TFeatureSizeFeatureSampleDataUMapMapItr i = data.begin(); i != data.end(); ++i)
    {
        model_t::EFeature feature = i->first;
        TSizeFeatureSampleDataUMap &featureData = i->second;

        for (TSizeFeatureSampleDataUMapItr j = featureData.begin(); j != featureData.end(); ++j)
        {
            std::size_t cid = j->first;
            const TDouble1VecVec &bucketValues = j->second.s_BucketValues;
            TTimeVec &times = j->second.s_Times;
            TDouble1VecVec &samples = j->second.s_Samples;
            TDouble1Vec4VecVec &weights = j->second.s_Weights;
            bool integer = j->second.s_IsInteger;
            if (samples.empty() && (!model_t::isSampled(feature) || bucketValues.empty()))
            {
                continue;
            }
            maths::COrderings::simultaneousSort(samples, times, weights);

            core_t::TTime latest = boost::numeric::bounds<core_t::TTime>::lowest();

            this->updateTrend(feature, cid, times, samples);
            for (std::size_t k = 0u; k < samples.size(); ++k)
            {
                samples[k] = this->detrend(feature, cid, times[k], 0.0, samples[k]);
                latest = std::max(latest, times[k]);
            }

            maths_t::EDataType type = integer ? maths_t::E_IntegerData : maths_t::E_ContinuousData;
            std::size_t dimension = model_t::dimension(feature);
            double interval = this->propagationTime(cid, latest);

            residuals[0].clear();
            residuals[1].clear();
            residuals[0].resize(dimension);
            residuals[1].resize(dimension);

            if (dimension == 1)
            {
                TPriorPtr &prior = priors[feature][cid];
                const TDecompositionPtr1Vec &trend = this->trend(feature, cid);

                if (bucketValues.size() > 0)
                {
                    TDouble1Vec bucketValues_;
                    bucketValues_.reserve(bucketValues.size());
                    for (std::size_t k = 0u; k < bucketValues.size(); ++k)
                    {
                        bucketValues_.push_back(bucketValues[k][0]);
                    }
                    prior->adjustOffset(maths::CConstantWeights::COUNT,
                                        bucketValues_,
                                        TDouble4Vec1Vec(bucketValues_.size(), maths::CConstantWeights::UNIT));
                }
                if (samples.empty())
                {
                    continue;
                }

                prior->dataType(type);
                for (std::size_t k = 0u; k < samples.size(); ++k)
                {
                    TDouble4Vec1Vec weight(1);
                    weight.reserve(weights[k].size());
                    for (std::size_t l = 0u; l < weights[k].size(); ++l)
                    {
                        weight[0].push_back(weights[k][l][0]);
                    }
                    prior->addSamples(weightStyles, samples[k], weight);
                    this->residuals(interval, trend, *prior, samples[k], residuals);
                }
                prior->propagateForwardsByTime(interval);

                if (maths::CBasicStatistics::count(residuals[0][0]) > 0.0)
                {
                    prediction.assign(1, prior->marginalLikelihoodMean());
                    residual.assign(1, maths::CBasicStatistics::mean(residuals[0][0]));
                    prior->decayRate(this->decayRateMultiplier(E_PriorControl,
                                                               feature, cid,
                                                               prediction, residual)
                                     * prior->decayRate());
                    LOG_TRACE("prior decay rate = " << prior->decayRate());
               }
                if (maths::CBasicStatistics::count(residuals[1][0]) > 0.0)
                {
                    prediction.assign(1, trend[0]->mean());
                    residual.assign(1, maths::CBasicStatistics::mean(residuals[1][0]));
                    trend[0]->decayRate(this->decayRateMultiplier(E_TrendControl,
                                                                  feature, cid,
                                                                  prediction, residual)
                                        * trend[0]->decayRate());
                    LOG_TRACE("trend decay rate = " << trend[0]->decayRate());
                }
                LOG_TRACE(this->attributeName(cid) << " prior:" << core_t::LINE_ENDING << prior->print());
            }
            else if (!samples.empty())
            {
                TMultivariatePriorPtr &prior = multivariatePriors[feature][cid];
                const TDecompositionPtr1Vec &trend = this->trend(feature, cid);

                prior->dataType(type);
                for (std::size_t k = 0u; k < samples.size(); ++k)
                {
                    prior->addSamples(weightStyles,
                                      TDouble10Vec1Vec(1, samples[k]),
                                      TDouble10Vec4Vec1Vec(1, weights[k]));
                    this->residuals(interval, trend, *prior, samples[k], residuals);
                }
                prior->propagateForwardsByTime(interval);

                if (maths::CBasicStatistics::count(residuals[0][0]) > 0.0)
                {
                    prediction = prior->marginalLikelihoodMean();
                    means(residuals[0], residual);
                    prior->decayRate(this->decayRateMultiplier(E_PriorControl,
                                                               feature, cid,
                                                               prediction, residual)
                                     * prior->decayRate());
                    LOG_TRACE("prior decay rate = " << prior->decayRate());
                }
                if (maths::CBasicStatistics::count(residuals[1][0]) > 0.0)
                {
                    prediction.resize(trend.size());
                    for (std::size_t k = 0u; k < trend.size(); ++k)
                    {
                        prediction[k] = trend[k]->mean();
                    }
                    means(residuals[1], residual);
                    double multiplier = this->decayRateMultiplier(E_TrendControl,
                                                                  feature, cid,
                                                                  prediction, residual);
                    for (std::size_t k = 0u; k < trend.size(); ++k)
                    {
                        trend[k]->decayRate(multiplier * trend[k]->decayRate());
                    }
                    LOG_TRACE("trend decay rate = " << trend[0]->decayRate());
                }
                LOG_TRACE(this->attributeName(cid) << " prior:" << core_t::LINE_ENDING << prior->print());
            }
        }
    }
}

void CPopulationModel::correctBaselineForInterim(model_t::EFeature feature,
                                                 std::size_t pid,
                                                 std::size_t cid,
                                                 model_t::CResultType type,
                                                 const TFeatureSizeSizeTripleDouble1VecUMap &corrections,
                                                 TDouble1Vec &result) const
{
    if (type.isInterim() && model_t::requiresInterimResultAdjustment(feature))
    {
        TFeatureSizeSizeTripleDouble1VecUMapCItr correction = corrections.find(core::make_triple(feature, pid, cid));
        if (correction != corrections.end())
        {
            result -= (*correction).second;
        }
    }
}

double CPopulationModel::propagationTime(std::size_t cid, core_t::TTime time) const
{
    return 1.0 + (this->params().s_InitialDecayRateMultiplier - 1.0)
                 * maths::CTools::truncate(1.0 -  static_cast<double>(time - m_AttributeFirstBucketTimes[cid])
                                                / static_cast<double>(3 * core::constants::WEEK), 0.0, 1.0);
}

CPopulationModel::TDecompositionCPtr1Vec
    CPopulationModel::trend(model_t::EFeature feature, std::size_t cid) const
{
    const TDecompositionPtr1Vec &trend = const_cast<CPopulationModel*>(this)->trend(feature, cid);

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

const CPopulationModel::TDecompositionPtr1Vec &
    CPopulationModel::trend(model_t::EFeature feature, std::size_t cid)
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
    if (cid >= m_Decompositions[i].second.size())
    {
        LOG_ERROR("No decomposition for attribute " << this->attributeName(cid));
        return NO_TREND;
    }
    return m_Decompositions[i].second[cid];
}

CPopulationModel::TDouble1Vec CPopulationModel::detrend(model_t::EFeature feature,
                                                        std::size_t cid,
                                                        core_t::TTime time,
                                                        double confidence,
                                                        const TDouble1Vec &value) const
{
    TDecompositionCPtr1Vec trend = this->trend(feature, cid);
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

CPopulationModel::TDouble1VecDouble1VecPr
    CPopulationModel::seasonalVarianceScale(model_t::EFeature feature,
                                            std::size_t cid,
                                            core_t::TTime time,
                                            double confidence) const
{
    TDecompositionCPtr1Vec trend = this->trend(feature, cid);
    std::size_t dimension = trend.size();
    if (trend.empty())
    {
        return TDouble1VecDouble1VecPr(TDouble1Vec(dimension, 1.0),
                                       TDouble1Vec(dimension, 1.0));
    }

    TDouble1Vec variance;
    if (dimension == 1)
    {
        if (const maths::CPrior *prior = this->prior(feature, cid))
        {
            variance.assign(1, prior->marginalLikelihoodVariance());
        }
    }
    else if (const maths::CMultivariatePrior *prior = this->multivariatePrior(feature, cid))
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

void CPopulationModel::updateTrend(model_t::EFeature feature,
                                   std::size_t cid,
                                   const TTimeVec &times,
                                   const TDouble1VecVec &values)
{
    if (values.empty())
    {
        return;
    }

    if (values.size() != times.size())
    {
        LOG_ERROR("Values mismatch: '" << values.size() << " != " << times.size() << "'");
        return;
    }

    const TDecompositionPtr1Vec &trend = this->trend(feature, cid);
    if (trend.empty())
    {
        return;
    }

    std::size_t dimension = trend.size();
    if (values[0].size() != dimension)
    {
        LOG_ERROR("Dimension mismatch: '" << values[0].size() << " != " << dimension << "'");
        return;
    }

    bool reinitialize = false;
    for (std::size_t i = 0u; i < trend.size(); ++i)
    {
        core_t::TTime latest = boost::numeric::bounds<core_t::TTime>::lowest();
        for (std::size_t j = 0u; j < values.size(); ++j)
        {
            if (trend[i]->addPoint(times[j], values[j][i]))
            {
                reinitialize = true;
            }
            latest = std::max(latest, times[j]);
        }
        trend[i]->propagateForwardsTo(latest);
    }

    if (reinitialize && this->resetPrior(feature, cid))
    {
        LOG_DEBUG("Reinitialized model for feature " << model_t::print(feature)
                  << " and attribute '" << this->attributeName(cid) << "'");
        this->dataGatherer().resetSampleCount(cid);
    }
}

CDecayRateController *CPopulationModel::decayRateController(EDecayRateController controller,
                                                            model_t::EFeature feature,
                                                            std::size_t cid)
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
    if (cid >= m_DecayRateControllers[i].second.size())
    {
        LOG_ERROR("No controller for person " << this->personName(cid));
        return 0;
    }
    return &m_DecayRateControllers[i].second[cid][controller];
}

double CPopulationModel::decayRateMultiplier(EDecayRateController controller_,
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

CPopulationModel::TFeaturePriorPtrPrVec &CPopulationModel::newPriors(void)
{
    return m_NewFeaturePriors;
}

CPopulationModel::TFeatureMultivariatePriorPtrPrVec &CPopulationModel::newMultivariatePriors(void)
{
    return m_NewMultivariateFeaturePriors;
}

const CPopulationModel::TTimeVec &CPopulationModel::attributeFirstBucketTimes(void) const
{
    return m_AttributeFirstBucketTimes;
}

const CPopulationModel::TTimeVec &CPopulationModel::attributeLastBucketTimes(void) const
{
    return m_AttributeLastBucketTimes;
}

void CPopulationModel::peopleAndAttributesToRemove(core_t::TTime time,
                                                   std::size_t maximumAge,
                                                   TSizeVec &peopleToRemove,
                                                   TSizeVec &attributesToRemove) const
{
    if (time <= 0)
    {
        return;
    }

    const CDataGatherer &gatherer = this->dataGatherer();

    for (std::size_t pid = 0u; pid < m_PersonLastBucketTimes.size(); ++pid)
    {
        if ((gatherer.isPersonActive(pid)) &&
            (!CModel::isTimeUnset(m_PersonLastBucketTimes[pid])))
        {
            std::size_t bucketsSinceLastEvent =
                    static_cast<std::size_t>((time - m_PersonLastBucketTimes[pid])
                                             / gatherer.bucketLength());
            if (bucketsSinceLastEvent > maximumAge)
            {
                LOG_TRACE(gatherer.personName(pid)
                          << ", bucketsSinceLastEvent = " << bucketsSinceLastEvent
                          << ", maximumAge = " << maximumAge);
                peopleToRemove.push_back(pid);
            }
        }
    }

    for (std::size_t cid = 0u; cid < m_AttributeLastBucketTimes.size(); ++cid)
    {
        if ((gatherer.isAttributeActive(cid)) &&
            (!CModel::isTimeUnset(m_AttributeLastBucketTimes[cid])))
        {
            std::size_t bucketsSinceLastEvent =
                    static_cast<std::size_t>((time - m_AttributeLastBucketTimes[cid])
                                             / gatherer.bucketLength());
            if (bucketsSinceLastEvent > maximumAge)
            {
                LOG_TRACE(gatherer.attributeName(cid)
                          << ", bucketsSinceLastEvent = " << bucketsSinceLastEvent
                          << ", maximumAge = " << maximumAge);
                attributesToRemove.push_back(cid);
            }
        }
    }
}

void CPopulationModel::removePeople(const TSizeVec &peopleToRemove)
{
    for (std::size_t i = 0u; i < peopleToRemove.size(); ++i)
    {
        uint32_t pid = static_cast<uint32_t>(peopleToRemove[i]);
        for (std::size_t cid = 0u; cid < m_PersonAttributeBucketCounts.size(); ++cid)
        {
            m_PersonAttributeBucketCounts[cid].removeFromMap(pid);
        }
        for (std::size_t cid = 0u; cid < m_DistinctPersonCounts.size(); ++cid)
        {
            m_DistinctPersonCounts[cid].remove(pid);
        }
    }
}

void CPopulationModel::doSkipSampling(core_t::TTime startTime, core_t::TTime endTime)
{
    const CDataGatherer &gatherer = this->dataGatherer();
    core_t::TTime gapDuration = endTime - startTime;

    for (std::size_t pid = 0u; pid < m_PersonLastBucketTimes.size(); ++pid)
    {
        if (gatherer.isPersonActive(pid) && !CModel::isTimeUnset(m_PersonLastBucketTimes[pid]))
        {
            m_PersonLastBucketTimes[pid] = m_PersonLastBucketTimes[pid] + gapDuration;
        }
    }

    for (std::size_t cid = 0u; cid < m_AttributeLastBucketTimes.size(); ++cid)
    {
        if (gatherer.isAttributeActive(cid) && !CModel::isTimeUnset(m_AttributeLastBucketTimes[cid]))
        {
            m_AttributeLastBucketTimes[cid] = m_AttributeLastBucketTimes[cid] + gapDuration;
        }
    }

    for (std::size_t i = 0u; i < m_Decompositions.size(); ++i)
    {
        TDecompositionPtr1VecVec &trends = m_Decompositions[i].second;
        for (std::size_t cid = 0; cid < trends.size(); ++cid)
        {
            TDecompositionPtr1Vec &trend = trends[cid];
            for (std::size_t j = 0; j < trend.size(); ++j)
            {
                trend[j]->skipTime(gapDuration);
            }
        }
    }
}

template<typename PRIOR>
void CPopulationModel::residuals(double interval,
                                 const TDecompositionPtr1Vec &trend,
                                 const PRIOR &prior,
                                 const TDouble1Vec &sample,
                                 TMeanAccumulator1Vec (&result)[2]) const
{
    typedef boost::optional<TDouble1Vec> TOptionalDouble1Vec;

    if (TOptionalDouble1Vec residual = CModelTools::predictionResidual(interval, prior, sample))
    {
        for (std::size_t i = 0u; i < residual->size(); ++i)
        {
            result[0][i].add((*residual)[i]);
        }
    }
    if (TOptionalDouble1Vec residual = CModelTools::predictionResidual(trend, sample))
    {
        for (std::size_t i = 0u; i < residual->size(); ++i)
        {
            result[1][i].add((*residual)[i]);
        }
    }
}

const double CPopulationModel::MINIMUM_SEASONAL_VARIANCE_SCALE(1.0);

}
}
