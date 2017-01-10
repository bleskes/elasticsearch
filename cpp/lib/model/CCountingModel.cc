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

#include <model/CCountingModel.h>

#include <core/CAllocationStrategy.h>

#include <maths/CChecksum.h>

#include <model/CAnnotatedProbabilityBuilder.h>
#include <model/CDataGatherer.h>
#include <model/CModelDetailsView.h>

namespace ml
{
namespace model
{

namespace
{
typedef std::pair<std::size_t, double> TSizeDoublePr;
typedef core::CSmallVector<TSizeDoublePr, 1> TSizeDoublePr1Vec;
typedef boost::reference_wrapper<const std::string> TStrCRef;
typedef std::vector<TStrCRef> TStrCRefVec;

const std::string WINDOW_BUCKET_COUNT_TAG("a");
const std::string PERSON_BUCKET_COUNT_TAG("b");
const std::string MEAN_COUNT_TAG("c");
const std::string EXTRA_DATA_TAG("d");
const std::string FEATURE_CORRELATIONS_TAG("e");
const std::string INTERIM_BUCKET_CORRECTOR_TAG("f");
const TStrCRefVec NO_CORRELATED_ATTRIBUTES;
const TSizeDoublePr1Vec NO_CORRELATES;

}

CCountingModel::CCountingModel(const SModelParams &params,
                               const TDataGathererPtr &dataGatherer,
                               const TModelCPtr &referenceModel) :
        CModel(params, dataGatherer, TFeatureInfluenceCalculatorCPtrPrVecVec(), false),
        m_ReferenceModel(referenceModel),
        m_StartTime(CModel::TIME_UNSET),
        m_Counts(),
        m_MeanCounts()
{
}

CCountingModel::CCountingModel(const SModelParams &params,
                               const TDataGathererPtr &dataGatherer,
                               core::CStateRestoreTraverser &traverser) :
        CModel(params, dataGatherer, TFeatureInfluenceCalculatorCPtrPrVecVec(), true),
        m_ReferenceModel(),
        m_StartTime(CModel::TIME_UNSET),
        m_Counts(),
        m_MeanCounts()
{
    traverser.traverseSubLevel(boost::bind(&CCountingModel::acceptRestoreTraverser,
                                           this,
                                           boost::ref(params.s_ExtraDataRestoreFunc),
                                           _1));
}


CCountingModel::CCountingModel(bool isForPersistence,
                               const CCountingModel &other) :
        CModel(isForPersistence, other),
        m_ReferenceModel(),
        m_StartTime(0),
        m_Counts(),
        m_MeanCounts(other.m_MeanCounts)
{
    if (!isForPersistence)
    {
        LOG_ABORT("This constructor only creates clones for persistence");
    }
}

void CCountingModel::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertValue(WINDOW_BUCKET_COUNT_TAG, this->windowBucketCount(), core::CIEEE754::E_SinglePrecision);
    core::CPersistUtils::persist(PERSON_BUCKET_COUNT_TAG, this->personBucketCounts(), inserter);
    core::CPersistUtils::persist(MEAN_COUNT_TAG, m_MeanCounts, inserter);
    this->featureCorrelationsAcceptPersistInserter(FEATURE_CORRELATIONS_TAG, inserter);
    this->interimBucketCorrectorAcceptPersistInserter(INTERIM_BUCKET_CORRECTOR_TAG, inserter);
    this->extraDataAcceptPersistInserter(EXTRA_DATA_TAG, inserter);
}

bool CCountingModel::acceptRestoreTraverser(const model_t::TAnyRestoreFunc &extraDataRestoreFunc,
                                            core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        if (name == WINDOW_BUCKET_COUNT_TAG)
        {
            double count;
            if (core::CStringUtils::stringToType(traverser.value(), count) == false)
            {
                LOG_ERROR("Invalid bucket count in " << traverser.value());
                return false;
            }
            this->windowBucketCount(count);
        }
        else if (name == PERSON_BUCKET_COUNT_TAG)
        {
            if (core::CPersistUtils::restore(name, this->personBucketCounts(), traverser) == false)
            {
                LOG_ERROR("Invalid bucket counts in " << traverser.value());
                return false;
            }
        }
        else if (name == MEAN_COUNT_TAG)
        {
            if (core::CPersistUtils::restore(name, m_MeanCounts, traverser) == false)
            {
                LOG_ERROR("Invalid mean counts");
                return false;
            }
        }
        else if (name == FEATURE_CORRELATIONS_TAG)
        {
            if (this->featureCorrelationsAcceptRestoreTraverser(traverser) == false)
            {
                return false;
            }
        }
        else if (name == INTERIM_BUCKET_CORRECTOR_TAG)
        {
            if (this->interimBucketCorrectorAcceptRestoreTraverser(traverser) == false)
            {
                return false;
            }
        }
        else if (name == EXTRA_DATA_TAG)
        {
            if (this->extraDataAcceptRestoreTraverser(extraDataRestoreFunc, traverser) == false)
            {
                return false;
            }
        }
    }
    while (traverser.next());

    return true;
}

CModel *CCountingModel::cloneForPersistence(void) const
{
    return new CCountingModel(true, *this);
}

model_t::EModelType CCountingModel::category(void) const
{
    return model_t::E_Counting;
}

bool CCountingModel::isPopulation(void) const
{
    return false;
}

bool CCountingModel::isEventRate(void) const
{
    return false;
}

bool CCountingModel::isMetric(void) const
{
    return false;
}

CCountingModel::TOptionalUInt64
    CCountingModel::currentBucketCount(std::size_t pid, core_t::TTime time) const
{
    typedef TSizeUInt64PrVec::const_iterator TSizeUInt64PrVecCItr;

    if (!this->bucketStatsAvailable(time))
    {
        LOG_ERROR("No statistics at " << time
                  << ", current bucket = " << this->printCurrentBucket());
        return TOptionalUInt64();
    }

    TSizeUInt64PrVecCItr result = std::lower_bound(m_Counts.begin(),
                                                   m_Counts.end(),
                                                   pid,
                                                   maths::COrderings::SFirstLess());

    return result != m_Counts.end() && result->first == pid ?
           result->second : static_cast<uint64_t>(0);
}

CCountingModel::TOptionalDouble
    CCountingModel::baselineBucketCount(std::size_t pid) const
{
    if (!m_ReferenceModel)
    {
        return pid < m_MeanCounts.size() ?
               maths::CBasicStatistics::mean(m_MeanCounts[pid]) : 0.0;
    }
    return m_ReferenceModel->baselineBucketCount(pid);
}

CCountingModel::TDouble1Vec
    CCountingModel::currentBucketValue(model_t::EFeature /*feature*/,
                                       std::size_t pid,
                                       std::size_t /*cid*/,
                                       core_t::TTime time) const
{
    TOptionalUInt64 count = this->currentBucketCount(pid, time);
    return count ? TDouble1Vec(1, static_cast<double>(*count)) : TDouble1Vec();
}

CCountingModel::TDouble1Vec
    CCountingModel::baselineBucketMean(model_t::EFeature feature,
                                       std::size_t pid,
                                       std::size_t cid,
                                       model_t::CResultType type,
                                       const TSizeDoublePr1Vec &correlated,
                                       core_t::TTime time) const
{
    if (!m_ReferenceModel)
    {
        TOptionalDouble count = this->baselineBucketCount(pid);
        return count ? TDouble1Vec(1, *count) : TDouble1Vec();
    }
    return m_ReferenceModel->baselineBucketMean(feature, pid, cid, type, correlated, time);
}

void CCountingModel::currentBucketPersonIds(core_t::TTime time, TSizeVec &result) const
{
    typedef boost::unordered_set<std::size_t> TSizeUSet;

    result.clear();

    if (!this->bucketStatsAvailable(time))
    {
        LOG_ERROR("No statistics at " << time
                  << ", current bucket = " << this->printCurrentBucket());
        return;
    }

    TSizeUSet people;
    for (std::size_t i = 0u; i < m_Counts.size(); ++i)
    {
        people.insert(m_Counts[i].first);
    }
    result.reserve(people.size());
    result.assign(people.begin(), people.end());
}

void CCountingModel::sampleOutOfPhase(core_t::TTime startTime,
                                      core_t::TTime endTime,
                                      CResourceMonitor &resourceMonitor)
{
    this->sampleBucketStatistics(startTime, endTime, resourceMonitor);
}

void CCountingModel::sampleBucketStatistics(core_t::TTime startTime,
                                            core_t::TTime endTime,
                                            CResourceMonitor &resourceMonitor)
{
    CDataGatherer &gatherer = this->dataGatherer();
    core_t::TTime bucketLength = gatherer.bucketLength();

    if (!gatherer.dataAvailable(startTime))
    {
        return;
    }

    for (core_t::TTime bucketStartTime = startTime;
         bucketStartTime < endTime;
         bucketStartTime += bucketLength)
    {
        this->CModel::sampleBucketStatistics(bucketStartTime,
                                             bucketStartTime + bucketLength,
                                             resourceMonitor);
        gatherer.timeNow(bucketStartTime);
        this->updateCurrentBucketsStats(bucketStartTime);
    }
}

void CCountingModel::sample(core_t::TTime startTime,
                            core_t::TTime endTime,
                            CResourceMonitor &resourceMonitor)
{
    CDataGatherer &gatherer = this->dataGatherer();
    core_t::TTime bucketLength = gatherer.bucketLength();

    if (!gatherer.validateSampleTimes(startTime, endTime))
    {
        return;
    }

    this->createUpdateNewModels(startTime, resourceMonitor);

    for (core_t::TTime bucketStartTime = startTime;
         bucketStartTime < endTime;
         bucketStartTime += bucketLength)
    {
        gatherer.sampleNow(bucketStartTime);
        this->CModel::sample(bucketStartTime,
                             bucketStartTime + bucketLength,
                             resourceMonitor);

        this->updateCurrentBucketsStats(bucketStartTime);

        for (std::size_t i = 0u; i < m_Counts.size(); ++i)
        {
            m_MeanCounts[m_Counts[i].first].add(static_cast<double>(m_Counts[i].second));
        }
    }
}

void CCountingModel::currentBucketTotalCount(uint64_t /*totalCount*/)
{
    // Do nothing
}

void CCountingModel::doSkipSampling(core_t::TTime /*startTime*/, core_t::TTime /*endTime*/)
{
    // Do nothing
}

void CCountingModel::prune(std::size_t /*maximumAge*/)
{
}

bool CCountingModel::computeProbability(std::size_t pid,
                                        core_t::TTime startTime,
                                        core_t::TTime endTime,
                                        CPartitioningFields &/*partitioningFields*/,
                                        std::size_t /*numberAttributeProbabilities*/,
                                        SAnnotatedProbability &result) const
{
    result = SAnnotatedProbability(1.0);
    result.s_CurrentBucketCount  = this->currentBucketCount(pid, (startTime + endTime) / 2);
    result.s_BaselineBucketCount = this->baselineBucketCount(pid);
    return true;
}

bool CCountingModel::computeTotalProbability(const std::string &/*person*/,
                                             std::size_t /*numberAttributeProbabilities*/,
                                             TOptionalDouble &probability,
                                             TAttributeProbability1Vec &attributeProbabilities) const
{
    probability.reset(1.0);
    attributeProbabilities.clear();
    return true;
}

void CCountingModel::outputCurrentBucketStatistics(const std::string &partitionFieldValue,
                                                   const TBucketStatsOutputFunc &outputFunc) const
{
    const CDataGatherer &gatherer = this->dataGatherer();
    const std::string &partitionFieldName = gatherer.partitionFieldName();
    const std::string &personFieldName = gatherer.personFieldName();
    const std::string &funcName = model_t::outputFunctionName(model_t::E_IndividualCountByBucketAndPerson);
    for (std::size_t i = 0u; i < m_Counts.size(); ++i)
    {
        outputFunc(
            SOutputStats(
                m_StartTime,
                false,
                false,
                partitionFieldName,
                partitionFieldValue,
                EMPTY_STRING,
                EMPTY_STRING,
                personFieldName,
                gatherer.personName(m_Counts[i].first, EMPTY_STRING),
                EMPTY_STRING,
                funcName,
                static_cast<double>(m_Counts[i].second),
                true
            )
        );
    }
}

uint64_t CCountingModel::checksum(bool includeCurrentBucketStats) const
{
    uint64_t result = this->CModel::checksum(includeCurrentBucketStats);
    if (m_ReferenceModel)
    {
        result = core::CHashing::hashCombine(result, m_ReferenceModel->checksum(includeCurrentBucketStats));
    }
    result = maths::CChecksum::calculate(result, m_MeanCounts);
    if (includeCurrentBucketStats)
    {
        result = maths::CChecksum::calculate(result, m_StartTime);
        result = maths::CChecksum::calculate(result, m_Counts);
    }
    return result;
}

void CCountingModel::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CCountingModel");
    this->CModel::debugMemoryUsage(mem->addChild());
    core::CMemoryDebug::dynamicSize("m_Counts", m_Counts, mem);
    core::CMemoryDebug::dynamicSize("m_MeanCounts", m_MeanCounts, mem);
}

std::size_t CCountingModel::memoryUsage(void) const
{
    return this->CModel::memoryUsage()
          + core::CMemory::dynamicSize(m_Counts)
          + core::CMemory::dynamicSize(m_MeanCounts);
}

std::size_t CCountingModel::staticSize(void) const
{
    return sizeof(*this);
}

CCountingModel::CModelDetailsViewPtr CCountingModel::details(void) const
{
    return CModelDetailsViewPtr();
}

core_t::TTime CCountingModel::currentBucketStartTime(void) const
{
    return m_StartTime;
}

void CCountingModel::currentBucketStartTime(core_t::TTime time)
{
    m_StartTime = time;
}

double CCountingModel::attributeFrequency(std::size_t /*cid*/) const
{
    return 1.0;
}

void CCountingModel::createUpdateNewModels(core_t::TTime /*time*/,
                                           CResourceMonitor &/*resourceMonitor*/)
{
    this->updateRecycledModels();
    CDataGatherer &gatherer = this->dataGatherer();
    std::size_t numberNewPeople = gatherer.numberPeople();
    std::size_t numberExistingPeople = m_MeanCounts.size();
    numberNewPeople = numberNewPeople > numberExistingPeople ?
                      numberNewPeople - numberExistingPeople : 0;
    if (numberNewPeople > 0)
    {
        LOG_TRACE("Creating " << numberNewPeople << " new people");
        this->createNewModels(numberNewPeople, 0);
    }
}

void CCountingModel::createNewModels(std::size_t n, std::size_t m)
{
    if (n > 0)
    {
        core::CAllocationStrategy::resize(m_MeanCounts, m_MeanCounts.size() + n);
    }
    this->CModel::createNewModels(n, m);
}

void CCountingModel::updateCurrentBucketsStats(core_t::TTime time)
{
    CDataGatherer &gatherer = this->dataGatherer();

    // Currently, we only remember one bucket.
    m_StartTime = time;
    gatherer.personNonZeroCounts(time, m_Counts);

    // Results are only output if currentBucketPersonIds is
    // not empty. Therefore, we need to explicitly set the
    // count to 0 so that we output results.
    if (m_Counts.empty())
    {
        m_Counts.push_back(TSizeUInt64Pr(0, 0));
    }
}

void CCountingModel::updateRecycledModels(void)
{
    const TSizeVec &recycledPeople = this->dataGatherer().recycledPersonIds();
    for (std::size_t i = 0u; i < recycledPeople.size(); ++i)
    {
        m_MeanCounts[recycledPeople[i]] = TMeanAccumulator();
    }
    this->CModel::updateRecycledModels();
}

void CCountingModel::clearPrunedResources(const TSizeVec &people, const TSizeVec &attributes)
{
    this->CModel::clearPrunedResources(people, attributes);
}

bool CCountingModel::bucketStatsAvailable(core_t::TTime time) const
{
    return time >= m_StartTime && time < m_StartTime + this->bucketLength();
}

std::string CCountingModel::printCurrentBucket(void) const
{
    std::ostringstream result;
    result << "[" << m_StartTime << ","
           << m_StartTime + this->bucketLength() << ")";
    return result.str();
}

}
}


