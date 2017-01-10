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

#include <model/CAnomalyDetector.h>

#include <core/CContainerPrinter.h>
#include <core/CLogger.h>
#include <core/CMemory.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>
#include <core/CStatistics.h>

#include <maths/CIntegerTools.h>
#include <maths/COrderings.h>
#include <maths/CSampling.h>
#include <maths/CTrendTests.h>

#include <model/CAnomalyScore.h>
#include <model/CBoxPlotData.h>
#include <model/CDataGatherer.h>
#include <model/CModel.h>
#include <model/CSearchKey.h>
#include <model/CStringStore.h>

#include <boost/bind.hpp>

#include <limits>
#include <sstream>
#include <vector>


namespace ml
{
namespace model
{

// We obfuscate the XML element names to avoid giving away too much information
// about our model
namespace
{

const std::string FIRST_TIME_TAG("a");
const std::string MODELS_TAG("b");
const std::string PARTITION_FIELD_VALUE_TAG("c");
const std::string KEY_TAG("d");
//const std::string ANOMALY_CHAIN_TAG("e");
const std::string SIMPLE_COUNT_STATICS("f");

// classes containing static members needing persistence
const std::string RANDOMIZED_PERIODIC_TAG("a");
const std::string STATISTICS_TAG("b");
const std::string SAMPLING_TAG("c");

}

// Increment this every time a change to the state is made that requires
// existing state to be discarded
const std::string CAnomalyDetector::STATE_VERSION("32");

const std::string CAnomalyDetector::COUNT_NAME("count");
const std::string CAnomalyDetector::DISTINCT_COUNT_NAME("distinct_count");
const std::string CAnomalyDetector::RARE_NAME("rare");
const std::string CAnomalyDetector::INFO_CONTENT_NAME("info_content");
const std::string CAnomalyDetector::MEAN_NAME("mean");
const std::string CAnomalyDetector::MEDIAN_NAME("median");
const std::string CAnomalyDetector::MIN_NAME("min");
const std::string CAnomalyDetector::MAX_NAME("max");
const std::string CAnomalyDetector::VARIANCE_NAME("varp");
const std::string CAnomalyDetector::SUM_NAME("sum");
const std::string CAnomalyDetector::LAT_LONG_NAME("lat_long");
const std::string CAnomalyDetector::EMPTY_STRING;


CAnomalyDetector::CAnomalyDetector(int identifier,
                                   CLimits &limits,
                                   const CModelConfig &modelConfig,
                                   const std::string &partitionFieldValue,
                                   core_t::TTime firstTime,
                                   const TModelFactoryCPtr &modelFactory)
    : m_Limits(limits),
      m_Identifier(identifier),
      m_ModelConfig(modelConfig),
      m_PartitionFieldValue(CStringStore::names().get(partitionFieldValue)),
      m_LastBucketEndTime(maths::CIntegerTools::ceil(firstTime,
                                                     modelConfig.bucketLength())),
      m_Models(modelConfig,
               modelFactory,
               m_LastBucketEndTime),
      m_IsForPersistence(false)
{
    limits.resourceMonitor().registerComponent(m_Models);
    LOG_DEBUG("CAnomalyDetector(): " << this->description()
              << " for '" << *m_PartitionFieldValue << "'"
              << ", first time = " << firstTime
              << ", bucketLength = " << modelConfig.bucketLength()
              << ", m_LastBucketEndTime = " << m_LastBucketEndTime);
}

CAnomalyDetector::CAnomalyDetector(bool isForPersistence,
                                   const CAnomalyDetector &other)
    : m_Limits(other.m_Limits),
      m_Identifier(other.m_Identifier),
      m_ModelConfig(other.m_ModelConfig),
      m_PartitionFieldValue(other.m_PartitionFieldValue),
      // Empty result function is fine in this case
      // Empty result count function is fine in this case
      m_LastBucketEndTime(other.m_LastBucketEndTime),
      m_Models(isForPersistence, other.m_Models),
      // Empty message propagation function is fine in this case
      m_IsForPersistence(isForPersistence)
{
    if (!isForPersistence)
    {
        LOG_ABORT("This constructor only creates clones for persistence");
    }
}

CAnomalyDetector::~CAnomalyDetector(void)
{
    if (!m_IsForPersistence)
    {
        m_Limits.resourceMonitor().unRegisterComponent(m_Models);
    }
}

size_t CAnomalyDetector::numberActivePeople(void) const
{
    return m_Models.numberActivePeople();
}

size_t CAnomalyDetector::numberActiveAttributes(void) const
{
    return m_Models.numberActiveAttributes();
}

size_t CAnomalyDetector::maxDimension(void) const
{
    return m_Models.maxDimension();
}

void CAnomalyDetector::zeroModelsToTime(core_t::TTime time)
{
    // If there has been a big gap in the times, we might need to sample
    // many buckets; if there has been no gap, the loop may legitimately
    // have no iterations.

    core_t::TTime bucketLength = m_ModelConfig.bucketLength();

    while (time >= (m_LastBucketEndTime + bucketLength))
    {
        core_t::TTime bucketStartTime = m_LastBucketEndTime;
        m_LastBucketEndTime += bucketLength;

        LOG_TRACE("sample: m_DetectorKey = '" << this->description()
                  << "', bucketStartTime = " << bucketStartTime
                  << ", m_LastBucketEndTime = " << m_LastBucketEndTime);

        // Update the statistical models.
        m_Models.sample(bucketStartTime,
                        m_LastBucketEndTime,
                        m_Limits.resourceMonitor());
    }
}

bool CAnomalyDetector::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    m_Models.clearModels();

    // We expect tags immediately below the root storing the first time the
    // models were created and the models IN THAT ORDER.

    do
    {
        const std::string &name = traverser.name();
        if (name == MODELS_TAG)
        {
            if (traverser.traverseSubLevel(boost::bind(&CModelEnsemble::acceptRestoreTraverser,
                                                       &m_Models,
                                                       _1)) == false)
            {
                LOG_ERROR("Invalid models in " << traverser.value());
                return false;
            }
        }
        else if (name == SIMPLE_COUNT_STATICS)
        {
            if (traverser.traverseSubLevel(boost::bind(&CAnomalyDetector::staticsAcceptRestoreTraverser,
                                                        this,
                                                       _1)) == false)
            {
                LOG_ERROR("Invalid simple count statics in " << traverser.value());
                return false;
            }
        }
        // TODO comment in when enabling anomaly chaining.
        //else if (name == ANOMALY_CHAIN_TAG)
        //{
        //    if (traverser.traverseSubLevel(boost::bind(CAnomalyChain::acceptRestoreTraverser,
        //                                               &m_AnomalyChain,
        //                                               _1)) == false)
        //    {
        //        LOG_ERROR("Invalid anomaly chain in " << traverser.value());
        //        return false;
        //    }
        //}
    }
    while (traverser.next());

    return true;
}

bool CAnomalyDetector::staticsAcceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        if (name == RANDOMIZED_PERIODIC_TAG)
        {
            if (traverser.traverseSubLevel(
                    &maths::CTrendTests::CRandomizedPeriodicity::staticsAcceptRestoreTraverser) == false)
            {
                LOG_ERROR("Failed to restore randomized periodic test state");
                return false;
            }
        }
        else if (name == STATISTICS_TAG)
        {
            if (traverser.traverseSubLevel(
                    &core::CStatistics::staticsAcceptRestoreTraverser) == false)
            {
                LOG_ERROR("Failed to restore statistics");
                return false;
            }
        }
        else if (name == SAMPLING_TAG)
        {
            if (traverser.traverseSubLevel(
                    &maths::CSampling::staticsAcceptRestoreTraverser) == false)
            {
                LOG_ERROR("Failed to restore sampling state");
                return false;
            }
        }
    }
    while (traverser.next());

    return true;
}


bool CAnomalyDetector::partitionFieldAcceptRestoreTraverser(core::CStateRestoreTraverser &traverser,
                                                            std::string &partitionFieldValue)
{
    do
    {
        const std::string &name = traverser.name();
        if (name == PARTITION_FIELD_VALUE_TAG)
        {
            partitionFieldValue = traverser.value();
            return true;
        }
    }
    while (traverser.next());

    return false;
}

bool CAnomalyDetector::keyAcceptRestoreTraverser(core::CStateRestoreTraverser &traverser,
                                                 CSearchKey &key)
{
    do
    {
        const std::string &name = traverser.name();
        if (name == KEY_TAG)
        {
            bool successful(true);
            key = CSearchKey(traverser, successful);
            if (successful == false)
            {
                LOG_ERROR("Invalid key in " << traverser.value());
                return false;
            }
            return true;
        }
    }
    while (traverser.next());

    return false;
}

void CAnomalyDetector::keyAcceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    CSearchKey searchKey(m_Models.searchKey());
    inserter.insertLevel(KEY_TAG,
                         boost::bind(&CSearchKey::acceptPersistInserter,
                                     &searchKey,
                                     _1));
}

void CAnomalyDetector::partitionFieldAcceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertValue(PARTITION_FIELD_VALUE_TAG, *m_PartitionFieldValue);
}

void CAnomalyDetector::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    // Persist static members only once within the simple count detector
    // and do this first so that other model components can use
    // static strings
    if (this->isSimpleCount())
    {
        inserter.insertLevel(SIMPLE_COUNT_STATICS,
                             boost::bind(&CAnomalyDetector::staticsAcceptPersistInserter,
                                         this,
                                         _1));
    }

    inserter.insertLevel(MODELS_TAG, boost::bind(&CModelEnsemble::acceptPersistInserter, &m_Models, _1));
    // TODO comment in when enabling anomaly chaining.
    //inserter.insertLevel(ANOMALY_CHAIN_TAG,
    //                     boost::bind(&CAnomalyChain::acceptPersistInserter,
    //                                 &m_AnomalyChain,
    //                                 _1));

    // These may be redundant if keyAcceptPersistInserter() and/or
    // partitionFieldAcceptPersistInserter() have been called, but the Splunk
    // app still uses them at this level so don't remove them (unless you've
    // changed the Splunk app code accordingly)
    inserter.insertValue(PARTITION_FIELD_VALUE_TAG, *m_PartitionFieldValue);
    CSearchKey searchKey(m_Models.searchKey());
    inserter.insertLevel(KEY_TAG, boost::bind(&CSearchKey::acceptPersistInserter, &searchKey, _1));

}

void CAnomalyDetector::staticsAcceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertLevel(RANDOMIZED_PERIODIC_TAG,
                         &maths::CTrendTests::CRandomizedPeriodicity::staticsAcceptPersistInserter);
    inserter.insertLevel(STATISTICS_TAG, &core::CStatistics::staticsAcceptPersistInserter);
    inserter.insertLevel(SAMPLING_TAG, &maths::CSampling::staticsAcceptPersistInserter);
}

const CAnomalyDetector::TStrVec &CAnomalyDetector::fieldsOfInterest(void) const
{
    return m_Models.fieldsOfInterest();
}

void CAnomalyDetector::addRecord(core_t::TTime time,
                                 const TStrCPtrVec &fieldValues,
                                 boost::any &extraData)
{
    typedef boost::optional<std::size_t> TOptionalSize;

    const TStrCPtrVec &processedFieldValues = this->preprocessFieldValues(fieldValues);

    CEventData eventData;
    eventData.time(time);

    if (m_Models.addArrival(processedFieldValues, eventData, m_Limits.resourceMonitor()))
    {
        if (TOptionalSize pid = eventData.personId())
        {
            m_Models.extraData(time, *pid, extraData);
        }
    }
}

void CAnomalyDetector::addRecord(core_t::TTime time,
                                 const TStrCPtrVec &fieldValues)
{
    boost::any extraData;
    this->addRecord(time, fieldValues, extraData);
}

const CAnomalyDetector::TStrCPtrVec &
CAnomalyDetector::preprocessFieldValues(const TStrCPtrVec &fieldValues)
{
    return fieldValues;
}

void CAnomalyDetector::gatherAndOutputStats(core_t::TTime bucketStartTime,
                                            core_t::TTime bucketEndTime,
                                            const CModel::TBucketStatsOutputFunc &outputFunc)
{
    m_Models.gatherAndOutputStats(bucketStartTime,
                                  bucketEndTime,
                                  *m_PartitionFieldValue,
                                  outputFunc,
                                  m_Limits.resourceMonitor());
}

void CAnomalyDetector::buildResults(core_t::TTime bucketStartTime,
                                    core_t::TTime bucketEndTime,
                                    CHierarchicalResults &results)
{
    core_t::TTime bucketLength = m_ModelConfig.bucketLength();
    if (m_ModelConfig.bucketResultsDelay())
    {
        bucketLength /= 2;
    }
    bucketStartTime = maths::CIntegerTools::floor(bucketStartTime, bucketLength);
    bucketEndTime = maths::CIntegerTools::floor(bucketEndTime, bucketLength);
    if (bucketEndTime <= m_LastBucketEndTime)
    {
        return;
    }
    this->buildResultsHelper(bucketStartTime,
                             bucketEndTime,
                             boost::bind(&CModelEnsemble::sample,
                                         &m_Models,
                                         _1,
                                         _2,
                                         boost::ref(m_Limits.resourceMonitor())),
                             boost::bind(&CAnomalyDetector::updateLastSampledBucket,
                                         this,
                                         _1),
                             results);
}

void CAnomalyDetector::generateModelDebugData(core_t::TTime bucketStartTime,
                                             core_t::TTime bucketEndTime,
                                             double boundsPercentile,
                                             const TStrSet &terms,
                                             TBoxPlotDataVec &boxPlots) const
{
    if (   terms.empty()
        || (*m_PartitionFieldValue).empty()
        || terms.find(*m_PartitionFieldValue) != terms.end())
    {
        const CModel *model = m_Models.model(bucketStartTime);
        if (model == 0)
        {
            return;
        }

        const CSearchKey &key = m_Models.searchKey();

        m_Models.generateModelDebugDataInfo(bucketStartTime,
                                            bucketEndTime,
                                            boundsPercentile,
                                            terms,
                                            key.partitionFieldName(),
                                            *m_PartitionFieldValue,
                                            key.overFieldName(),
                                            key.byFieldName(),
                                            boxPlots);
    }
}

void CAnomalyDetector::buildInterimResults(core_t::TTime bucketStartTime,
                                           core_t::TTime bucketEndTime,
                                           CHierarchicalResults &results)
{
    this->buildResultsHelper(bucketStartTime,
                             bucketEndTime,
                             boost::bind(&CModelEnsemble::sampleBucketStatistics,
                                         &m_Models,
                                         _1,
                                         _2,
                                         boost::ref(m_Limits.resourceMonitor())),
                             boost::bind(&CAnomalyDetector::noUpdateLastSampledBucket,
                                         this,
                                         _1),
                             results);
}

void CAnomalyDetector::startNewDataSet(core_t::TTime firstTime)
{
    m_LastBucketEndTime = maths::CIntegerTools::ceil(firstTime,
                                                     m_ModelConfig.bucketLength());
    m_Models.startTime(m_LastBucketEndTime);
    LOG_TRACE("bucketLength = " << m_ModelConfig.bucketLength()
              << ", m_LastBucketEndTime = " << m_LastBucketEndTime);
}

void CAnomalyDetector::pruneModels(void)
{
    // Purge out any ancient models which are effectively dead.
    CModel *model = m_Models.model(m_LastBucketEndTime);
    if (model)
    {
        m_Models.pruneModels(m_LastBucketEndTime, model->defaultPruneWindow());
    }
}

void CAnomalyDetector::resetBucket(core_t::TTime bucketStart)
{
    this->models().resetBucket(bucketStart);
}

void CAnomalyDetector::showMemoryUsage(std::ostream &stream) const
{
    core::CMemoryUsage mem;
    mem.setName("Anomaly Detector Memory Usage");
    this->models().debugMemoryUsage(mem.addChild());
    CStringStore::debugMemoryUsage(mem.addChild());
    mem.compress();
    mem.print(stream);
    if (mem.usage() != this->models().memoryUsage())
    {
        LOG_ERROR("Discrepancy in memory report: " << mem.usage()
                  << " from debug, but " << this->models().memoryUsage()
                  << " from normal");
    }
}

std::size_t CAnomalyDetector::memoryUsage(void)
{
    return this->models().memoryUsage();
}

const core_t::TTime &CAnomalyDetector::lastBucketEndTime(void) const
{
    return m_LastBucketEndTime;
}

core_t::TTime &CAnomalyDetector::lastBucketEndTime(void)
{
    return m_LastBucketEndTime;
}

core_t::TTime CAnomalyDetector::modelBucketLength(void) const
{
    return m_Models.bucketLength();
}

std::string CAnomalyDetector::description(void) const
{
    CModelEnsemble::TStrVecCItr beginInfluencers = m_Models.beginInfluencers();
    CModelEnsemble::TStrVecCItr endInfluencers = m_Models.endInfluencers();
    return   m_Models.description()
           + ((*m_PartitionFieldValue).empty() ? "" : "/")
           + *m_PartitionFieldValue +
           (beginInfluencers != endInfluencers ? (" " +
           core::CContainerPrinter::print(beginInfluencers, endInfluencers)) : "");
}

const CModelEnsemble &CAnomalyDetector::models(void) const
{
    return m_Models;
}

CModelEnsemble &CAnomalyDetector::models(void)
{
    return m_Models;
}

void CAnomalyDetector::timeNow(core_t::TTime time)
{
    m_Models.timeNow(time);
}

void CAnomalyDetector::skipSampling(core_t::TTime endTime)
{
    m_Models.skipSampling(endTime);
    m_LastBucketEndTime = endTime;
}

template<typename SAMPLE_FUNC,
         typename LAST_SAMPLED_BUCKET_UPDATE_FUNC>
void CAnomalyDetector::buildResultsHelper(core_t::TTime bucketStartTime,
                                          core_t::TTime bucketEndTime,
                                          SAMPLE_FUNC sampleFunc,
                                          LAST_SAMPLED_BUCKET_UPDATE_FUNC lastSampledBucketUpdateFunc,
                                          CHierarchicalResults &results)
{
    core_t::TTime bucketLength = m_ModelConfig.bucketLength();
    typedef std::vector<std::size_t> TSizeVec;

    LOG_TRACE("sample: m_DetectorKey = '" << this->description()
              << "', bucketStartTime = " << bucketStartTime
              << ", bucketEndTime = " << bucketEndTime);

    // Update the statistical models.
    sampleFunc(bucketStartTime, bucketEndTime);

    CModel *model = m_Models.model(bucketStartTime);
    if (model == 0)
    {
        return;
    }

    LOG_TRACE("detect: m_DetectorKey = '" << this->description()
              << "', batch = " << m_Models.batch(bucketEndTime));

    CSearchKey key = m_Models.searchKey();
    TSizeVec personIds;
    if (!model->bucketStatsAvailable(bucketStartTime))
    {
        LOG_TRACE("No stats available for time " << bucketStartTime);
        return;
    }
    model->currentBucketPersonIds(bucketStartTime, personIds);

    LOG_TRACE("OutputResults, for " << key.toCue() << ", with people: " << personIds.size());
    for (std::size_t i = 0u; i < personIds.size(); ++i)
    {
        // Add the probability of seeing an "as unlikely"
        // sample for the model and person.
        model->addResult(m_Identifier,
                         *m_PartitionFieldValue,
                         personIds[i],
                         bucketStartTime,
                         bucketEndTime,
                         10, // TODO max number of attributes
                         results);
    }

    if (bucketEndTime % bucketLength == 0)
    {
        lastSampledBucketUpdateFunc(bucketEndTime);
    }
}

void CAnomalyDetector::updateLastSampledBucket(core_t::TTime bucketEndTime)
{
    m_LastBucketEndTime = std::max(m_LastBucketEndTime, bucketEndTime);
}

void CAnomalyDetector::noUpdateLastSampledBucket(core_t::TTime /*bucketEndTime*/) const
{
    // Do nothing
}

std::string CAnomalyDetector::toCue(void) const
{
    return m_Models.searchKey().toCue() + m_Models.searchKey().CUE_DELIMITER + *m_PartitionFieldValue;
}

std::string CAnomalyDetector::debug(void) const
{
    return m_Models.searchKey().debug() + '/' + *m_PartitionFieldValue;
}

bool CAnomalyDetector::isSimpleCount(void) const
{
    return false;
}

std::ostream &operator<<(std::ostream &strm, const CAnomalyDetector &detector)
{
    strm << detector.m_Models.searchKey()
         << '/'
         << *detector.m_PartitionFieldValue;
    return strm;
}

CModelView::CModelView(const CAnomalyDetector &detector)
    : m_Detector(&detector)
{
}

const CModelEnsemble &CModelView::models(void) const
{
    return m_Detector->models();
}

}
}
