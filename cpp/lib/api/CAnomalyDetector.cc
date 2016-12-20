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
#include <api/CAnomalyDetector.h>

#include <core/CDataAdder.h>
#include <core/CDataSearcher.h>
#include <core/CFunctional.h>
#include <core/CLogger.h>
#include <core/CJsonStatePersistInserter.h>
#include <core/CJsonStateRestoreTraverser.h>
#include <core/COsFileFuncs.h>
#include <core/CResourceLocator.h>
#include <core/CStateCompressor.h>
#include <core/CStateDecompressor.h>
#include <core/CStatistics.h>
#include <core/CStringUtils.h>
#include <core/CTimeUtils.h>

#include <maths/CIntegerTools.h>
#include <maths/COrderings.h>
#include <maths/CTools.h>

#include <model/CAnomalyScore.h>
#include <model/CHierarchicalResultsAggregator.h>
#include <model/CHierarchicalResultsPopulator.h>
#include <model/CHierarchicalResultsProbabilityFinalizer.h>
#include <model/CLimits.h>
#include <model/CSearchKey.h>
#include <model/CStringStore.h>

#include <api/CAnomalyDetectorUtils.h>
#include <api/CBackgroundPersister.h>
#include <api/CBoxPlotDataJsonWriter.h>
#include <api/CConfigUpdater.h>
#include <api/CFieldConfig.h>
#include <api/CHierarchicalResultsWriter.h>
#include <api/CJsonOutputWriter.h>


#include <boost/bind.hpp>
#include <boost/optional.hpp>

#include <fstream>
#include <sstream>
#include <string>

namespace prelert
{
namespace api
{

// We obfuscate most JSON field names to avoid giving away too much information
// about our model.
namespace
{
typedef boost::reference_wrapper<const std::string> TStrCRef;

//! Convert a (string, key) pair to something readable.
template<typename T>
inline std::string pairDebug(const T &t)
{
    return boost::unwrap_ref(t.second).debug() + '/' + boost::unwrap_ref(t.first);
}

const std::string TOP_LEVEL_DETECTOR_TAG("detector"); // do not obfuscate this
const std::string RESULTS_AGGREGATOR_TAG("aggregator");
const std::string TIME_TAG("a");
const std::string VERSION_TAG("b");
const std::string KEY_TAG("c");
const std::string PARTITION_FIELD_TAG("d");
const std::string DETECTOR_TAG("e");
const std::string HIERARCHICAL_RESULTS_TAG("f");
const std::string LATEST_RECORD_TIME_TAG("h");
const std::string MODEL_DEBUG_DATA_TAG("i");
const std::string LAST_RESULTS_TIME_TAG("j");

const core_t::TTime PERSIST_INTERVAL_INCREMENT(300); // 5 minutes

const char CONTROL_FIELD_NAME_CHAR('.');
}

// Statics
const std::string CAnomalyDetector::STATE_TYPE("model_state");
const std::string CAnomalyDetector::DEFAULT_TIME_FIELD_NAME("_time");
const std::string CAnomalyDetector::CONTROL_FIELD_NAME(1, CONTROL_FIELD_NAME_CHAR);
const std::string CAnomalyDetector::SOURCETYPE_FIELD_NAME("sourcetype");
const std::string CAnomalyDetector::EMPTY_STRING;

const CAnomalyDetector::TAnomalyDetectorPtr CAnomalyDetector::NULL_DETECTOR;


CAnomalyDetector::CAnomalyDetector(const std::string &jobId,
                                   model::CLimits &limits,
                                   CFieldConfig &fieldConfig,
                                   model::CModelConfig &modelConfig,
                                   CJsonOutputWriter &jsonOutputWriter,
                                   const TPersistCompleteFunc &persistCompleteFunc,
                                   CBackgroundPersister *periodicPersister,
                                   core_t::TTime periodicPersistInterval,
                                   core_t::TTime maxQuantileInterval,
                                   const std::string &timeFieldName,
                                   const std::string &timeFieldFormat,
                                   bool ignoreDowntime)
    : m_JobId(jobId),
      m_Limits(limits),
      m_JsonOutputWriter(jsonOutputWriter),
      m_FieldConfig(fieldConfig),
      m_ModelConfig(modelConfig),
      m_NumRecordsHandled(0),
      m_LastFinalisedBucketEndTime(0),
      m_CurrentSearchOverallFirstTime(0),
      m_PersistCompleteFunc(persistCompleteFunc),
      m_TimeFieldName(timeFieldName),
      m_TimeFieldFormat(timeFieldFormat),
      m_MaxDetectors(std::numeric_limits<size_t>::max()),
      m_ArePartitionsAllowed(true),
      m_SkipSampling(ignoreDowntime),
      m_PeriodicPersister(periodicPersister),
      m_PeriodicPersistInterval(periodicPersistInterval),
      m_MaxQuantileInterval(maxQuantileInterval),
      m_LastPeriodicPersistTime(0),
      m_LastNormalizerPersistTime(core::CTimeUtils::now()),
      m_LatestRecordTime(0),
      m_LastResultsTime(0),
      m_Aggregator(modelConfig),
      m_Normalizer(modelConfig),
      m_ResultsQueue(m_ModelConfig.bucketResultsDelay(),
                     this->effectiveBucketLength()),
      m_ModelDebugDataQueue(m_ModelConfig.bucketResultsDelay(),
                            this->effectiveBucketLength(),
                            0),
      m_FirstProcessortPeriodicPersistFunc()
{
    if (m_PeriodicPersistInterval < PERSIST_INTERVAL_INCREMENT)
    {
        // This may be dynamically increased further depending on how long
        // persistence takes
        m_PeriodicPersistInterval = PERSIST_INTERVAL_INCREMENT;
    }

    if (m_PeriodicPersister != 0)
    {
        m_FirstProcessortPeriodicPersistFunc = boost::bind(
                &CAnomalyDetector::backgroundPersistState, this, boost::ref(*m_PeriodicPersister));
    }

    m_Limits.resourceMonitor().memoryUsageReporter(
        boost::bind(&CJsonOutputWriter::reportMemoryUsage, &m_JsonOutputWriter, _1));
}

CAnomalyDetector::~CAnomalyDetector(void)
{
}

void CAnomalyDetector::newOutputStream(void)
{
    m_JsonOutputWriter.newOutputStream();
}

COutputHandler &CAnomalyDetector::outputHandler(void)
{
    return m_JsonOutputWriter;
}

bool CAnomalyDetector::handleSettings(const TStrStrUMap &settings)
{
    // Pass on the settings in case we're chained
    this->outputHandler().settings(settings);

    // Reset the start time to the beginning of the new search.
    m_CurrentSearchOverallFirstTime = 0;
    m_LastFinalisedBucketEndTime = 0;

    return true;
}

bool CAnomalyDetector::handleRecord(bool /* isDryRun */,
                                    const TStrVec &fieldNames,
                                    const TStrStrUMap &dataRowFields)
{
    // Non-empty control fields take precedence over everything else
    TStrStrUMapCItr iter = dataRowFields.find(CONTROL_FIELD_NAME);
    if (iter != dataRowFields.end() && !iter->second.empty())
    {
        return this->handleControlMessage(iter->second);
    }

    if (m_LastPeriodicPersistTime == 0)
    {
        // This is the first piece of data we've received.  Record this as the
        // time of the last periodic persist so that the next one happens after
        // the configured interval.
        m_LastPeriodicPersistTime = core::CTimeUtils::now();
    }

    core_t::TTime time(0);
    iter = dataRowFields.find(m_TimeFieldName);
    if (iter == dataRowFields.end())
    {
        core::CStatistics::stat(stat_t::E_NumberRecordsNoTimeField).increment();
        LOG_ERROR("Found record with no " << m_TimeFieldName << " field:" << core_t::LINE_ENDING <<
                  this->debugPrintRecord(fieldNames, dataRowFields));
        return true;
    }
    if (m_TimeFieldFormat.empty())
    {
        if (core::CStringUtils::stringToType(iter->second, time) == false)
        {
            core::CStatistics::stat(stat_t::E_NumberTimeFieldConversionErrors).increment();
            LOG_ERROR("Cannot interpret " << m_TimeFieldName << " field in record:" << core_t::LINE_ENDING <<
                      this->debugPrintRecord(fieldNames, dataRowFields));
            return true;
        }
    }
    else
    {
        // Use this library function instead of raw strptime() as it works
        // around many operating system specific issues.
        if (core::CTimeUtils::strptime(m_TimeFieldFormat, iter->second, time) == false)
        {
            core::CStatistics::stat(stat_t::E_NumberTimeFieldConversionErrors).increment();
            LOG_ERROR("Cannot interpret " << m_TimeFieldName <<
                      " field using format " << m_TimeFieldFormat << " in record:" << core_t::LINE_ENDING <<
                      this->debugPrintRecord(fieldNames, dataRowFields));
            return true;
        }
    }

    // Remember the timestamp of the very first record
    if (m_CurrentSearchOverallFirstTime == 0)
    {
        m_CurrentSearchOverallFirstTime = time;
        m_SkipSampling = false;
    }

    // This record must be within the specified latency. If latency
    // is zero, then it should be after the current bucket end. If
    // latency is non-zero, then it should be after the current bucket
    // end minus the latency.
    if (time < m_LastFinalisedBucketEndTime)
    {
        core::CStatistics::stat(stat_t::E_NumberTimeOrderErrors).increment();
        std::ostringstream ss;
        ss << "Records must be in ascending time order. "
           << "Record '" << this->debugPrintRecord(fieldNames, dataRowFields)
           << "' time " << time << " is before bucket time " << m_LastFinalisedBucketEndTime;
        LOG_ERROR(ss.str());
        return true;
    }

    if (m_SkipSampling)
    {
        this->skipSampling(maths::CIntegerTools::ceil(time, m_ModelConfig.bucketLength()));
        m_SkipSampling = false;
    }
    else
    {
        this->outputBucketResultsUntil(time);
    }

    const TKeyVec &keys = CAnomalyDetectorUtils::detectorKeys(SOURCETYPE_FIELD_NAME,
                                                              m_FieldConfig,
                                                              dataRowFields,
                                                              m_DetectorKeys,
                                                              m_Limits.resourceMonitor());
    for (std::size_t i = 0u; i < keys.size(); ++i)
    {
        const std::string &partitionFieldName(keys[i].partitionFieldName());

        // An empty partitionFieldName means no partitioning
        TStrStrUMapCItr itr = partitionFieldName.empty() ?
                              dataRowFields.end() :
                              dataRowFields.find(partitionFieldName);
        const std::string &partitionFieldValue(itr == dataRowFields.end() ?
                                               EMPTY_STRING : itr->second);

        // TODO - should usenull apply to the partition field too?

        const TAnomalyDetectorPtr &detector = this->detectorForKey(time,
                                                                   keys[i],
                                                                   partitionFieldValue,
                                                                   m_Limits.resourceMonitor());
        if (detector == 0)
        {
            // There wasn't enough memory to create the detector
            continue;
        }

        CAnomalyDetectorUtils::detect(boost::bind(&model::CAnomalyDetector::addRecord,
                                                  detector.get(),
                                                  time,
                                                  _1),
                                      detector->fieldsOfInterest(),
                                      dataRowFields);
    }

    if (m_NumRecordsHandled == 0 &&
        !m_ArePartitionsAllowed &&
        m_FieldConfig.havePartitionFields())
    {
        LOG_ERROR("License restriction violated: partition fields are "
                  "configured but your license does not allow this");
        return false;
    }

    core::CStatistics::stat(stat_t::E_NumberApiRecordsHandled).increment();

    ++m_NumRecordsHandled;
    m_LatestRecordTime = std::max(m_LatestRecordTime, time);

    return true;
}

void CAnomalyDetector::finalise(void)
{
    // Persist final state of normalizer
    m_JsonOutputWriter.persistNormalizer(m_Normalizer, m_LastNormalizerPersistTime);

    // Prune the models so that the final persisted state is as neat as possible
    this->pruneAllModels();

    this->refreshMemoryAndReport();

    // Wait for any ongoing periodic persist to complete, so that the data adder
    // is not used by both a periodic periodic persist and final persist at the
    // same time
    if (m_PeriodicPersister != 0)
    {
        m_PeriodicPersister->waitForIdle();
    }
}

bool CAnomalyDetector::initNormalizer(const std::string &quantilesStateFile)
{
    std::ifstream inputStream(quantilesStateFile.c_str());
    return m_Normalizer.fromJsonStream(inputStream) == model::CHierarchicalResultsNormalizer::E_Ok;
}

void CAnomalyDetector::firstProcessorPeriodicPersistFunc(TFirstProcessorPeriodicPersistFunc func)
{
    m_FirstProcessortPeriodicPersistFunc.swap(func);
}

uint64_t CAnomalyDetector::numRecordsHandled(void) const
{
    return m_NumRecordsHandled;
}

void CAnomalyDetector::description(void) const
{
    if (m_Detectors.empty())
    {
        return;
    }

    TKeyCRefAnomalyDetectorPtrPrVec detectors;
    this->sortedDetectors(detectors);

    LOG_INFO("Anomaly detectors:");
    TStrCRef partition = detectors[0].first.first;
    LOG_INFO("\tpartition " << partition.get());
    LOG_INFO("\t\tkey " << detectors[0].first.second.get());
    LOG_INFO("\t\t\t" << detectors[0].second->description());
    for (std::size_t i = 1u; i < detectors.size(); ++i)
    {
        if (detectors[i].first.first.get() != partition.get())
        {
            partition = detectors[i].first.first;
            LOG_INFO("\tpartition " << partition.get());
        }
        LOG_INFO("\t\tkey " << detectors[i].first.second.get());
        LOG_INFO("\t\t\t" << detectors[i].second->description());
    }
}

void CAnomalyDetector::descriptionAndDebugMemoryUsage(void) const
{
    if (m_Detectors.empty())
    {
        LOG_INFO("No detectors");
        return;
    }

    TKeyCRefAnomalyDetectorPtrPrVec detectors;
    this->sortedDetectors(detectors);

    std::ostringstream ss;
    ss << "Anomaly detectors:" << std::endl;
    TStrCRef partition = detectors[0].first.first;
    ss << "\tpartition " << partition.get() << std::endl;
    ss << "\t\tkey " << detectors[0].first.second.get() << std::endl;
    ss << "\t\t\t" << detectors[0].second->description() << std::endl;
    detectors[0].second->showMemoryUsage(ss);

    for (std::size_t i = 1u; i < detectors.size(); ++i)
    {
        ss << std::endl;
        if (detectors[i].first.first.get() != partition.get())
        {
            partition = detectors[i].first.first;
            ss << "\tpartition " << partition.get() << std::endl;
        }
        ss << "\t\tkey " << detectors[i].first.second.get() << std::endl;
        ss << "\t\t\t" << detectors[i].second->description() << std::endl;
        detectors[i].second->showMemoryUsage(ss);
    }
    LOG_INFO(ss.str());
}

const CAnomalyDetector::SRestoredStateDetail &CAnomalyDetector::restoreStateStatus() const
{
    return m_RestoredStateDetail;
}

bool CAnomalyDetector::handleControlMessage(const std::string &controlMessage)
{
    if (controlMessage.empty())
    {
        LOG_ERROR("Programmatic error - handleControlMessage should only be "
                  "called with non-empty control messages");
        return false;
    }

    switch (controlMessage[0])
    {
        case ' ':
            // Spaces are just used to fill the buffers and force prior messages
            // through the system - we don't need to do anything else
            LOG_TRACE("Received space control message of length " <<
                      controlMessage.length());
            break;
        case CONTROL_FIELD_NAME_CHAR:
            // Silent no-op.  This is a simple way to ignore repeated CSV header
            // rows in input.  (See bug 600 in Bugzilla for background.)
            break;
        case 'f':
            // Flush ID comes after the initial f
            this->acknowledgeFlush(controlMessage.substr(1));
            break;
        case 'i':
            this->generateInterimResults(controlMessage);
            break;
        case 'r':
            this->resetBuckets(controlMessage);
            break;
        case 't':
            this->advanceTime(controlMessage.substr(1));
            break;
        case 'u':
            this->updateConfig(controlMessage.substr(1));
            break;
        default:
            LOG_WARN("Ignoring unknown control message of length " <<
                     controlMessage.length() << " beginning with '" <<
                     controlMessage[0] << '\'');
            // Don't return false here (for the time being at least), as it
            // seems excessive to cause the entire job to fail
            break;
    }

    return true;
}

void CAnomalyDetector::acknowledgeFlush(const std::string &flushId)
{
    if (flushId.empty())
    {
        LOG_ERROR("Received flush control message with no ID");
    }
    else
    {
        LOG_DEBUG("Received flush control message with ID " << flushId);
    }
    m_JsonOutputWriter.acknowledgeFlush(flushId);
}

void CAnomalyDetector::updateConfig(const std::string &config)
{
    LOG_DEBUG("Received update config request: " << config);
    CConfigUpdater configUpdater(m_FieldConfig, m_ModelConfig);
    if (configUpdater.update(config) == false)
    {
        LOG_ERROR("Failed to update configuration");
    }
}

void CAnomalyDetector::advanceTime(const std::string &time_)
{
    if (time_.empty())
    {
        LOG_ERROR("Received request to advance time with no time");
        return;
    }

    core_t::TTime time(0);
    if (core::CStringUtils::stringToType(time_, time) == false)
    {
        LOG_ERROR("Received request to advance time to invalid time " << time_);
        return;
    }

    if (m_LastFinalisedBucketEndTime == 0)
    {
        LOG_WARN("Manually advancing time to " << time <<
                 " before any valid data has been seen");
    }
    else
    {
        LOG_DEBUG("Received request to advance time to " << time);
    }

    this->outputBucketResultsUntil(time);

    this->timeNow(time);
}

void CAnomalyDetector::outputBucketResultsUntil(core_t::TTime time)
{
    // If the bucket time has increased, output results for all field names
    core_t::TTime bucketLength = m_ModelConfig.bucketLength();
    core_t::TTime effectiveBucketLength = this->effectiveBucketLength();
    core_t::TTime latency = m_ModelConfig.latency();

    for (core_t::TTime lastBucketEndTime = m_LastFinalisedBucketEndTime;
         m_LastFinalisedBucketEndTime > 0 && lastBucketEndTime + bucketLength + latency <= time;
         lastBucketEndTime += effectiveBucketLength)
    {
        this->outputResults(lastBucketEndTime);

        // Check for periodic persistence immediately after calculating results
        // for the last bucket but before adding the first piece of data for the
        // next bucket
        this->checkBackgroundPersist();
        m_LastFinalisedBucketEndTime = lastBucketEndTime + effectiveBucketLength;
    }

    if (m_LastFinalisedBucketEndTime == 0)
    {
        m_LastFinalisedBucketEndTime =
                std::max(m_LastFinalisedBucketEndTime,
                         maths::CIntegerTools::floor(time, effectiveBucketLength) - latency);
    }
}

void CAnomalyDetector::skipSampling(core_t::TTime endTime)
{
    LOG_INFO("Ignoring downtime to: " << endTime);

    this->flushAndResetResultsQueue(endTime);

    for (TKeyAnomalyDetectorPtrUMapCItr itr = m_Detectors.begin();
         itr != m_Detectors.end();
         ++itr)
    {
        model::CAnomalyDetector *detector(itr->second.get());
        if (detector == 0)
        {
            LOG_ERROR("Unexpected NULL pointer for key '" << pairDebug(itr->first) << '\'');
            continue;
        }
        detector->skipSampling(endTime);
    }
    m_LastFinalisedBucketEndTime = endTime;
}

void CAnomalyDetector::flushAndResetResultsQueue(core_t::TTime startTime)
{
    if (m_ModelConfig.bucketResultsDelay() == 0)
    {
        return;
    }

    core_t::TTime effectiveBucketLength = this->effectiveBucketLength();
    core_t::TTime earliestResultTime =
            m_LastFinalisedBucketEndTime - m_ResultsQueue.size() * effectiveBucketLength;
    for (core_t::TTime bucketStart = earliestResultTime;
         bucketStart < m_LastFinalisedBucketEndTime;
         bucketStart += effectiveBucketLength)
    {
        model::CHierarchicalResults &results = m_ResultsQueue.latest();
        core_t::TTime resultsTime = m_ResultsQueue.chooseResultTime(bucketStart, m_ModelConfig.bucketLength(), results);
        if (resultsTime != 0)
        {
            core::CStopWatch timer(true);
            model::CHierarchicalResults &resultsToOutput = m_ResultsQueue.get(resultsTime);
            this->writeOutResults(false, resultsToOutput, resultsTime, timer, 0l);
            this->writeOutDebugData(resultsTime);
        }
        m_ResultsQueue.push(model::CHierarchicalResults());
    }
    m_ResultsQueue.reset(startTime);
    m_ModelDebugDataQueue.reset(startTime);
}

void CAnomalyDetector::timeNow(core_t::TTime time)
{
    for (TKeyAnomalyDetectorPtrUMapCItr i = m_Detectors.begin(); i != m_Detectors.end(); ++i)
    {
        model::CAnomalyDetector *detector(i->second.get());
        if (detector == 0)
        {
            LOG_ERROR("Unexpected NULL pointer for key '" << pairDebug(i->first) << '\'');
            continue;
        }
        detector->timeNow(time);
    }
}

core_t::TTime CAnomalyDetector::effectiveBucketLength(void) const
{
    return m_ModelConfig.bucketResultsDelay() ? m_ModelConfig.bucketLength() / 2 :
                                                m_ModelConfig.bucketLength();
}

void CAnomalyDetector::generateInterimResults(const std::string &controlMessage)
{
    LOG_DEBUG("Generating interim results");
    if (m_LastFinalisedBucketEndTime == 0)
    {
        LOG_ERROR("Cannot create interim results having seen data for less than one bucket ever");
        return;
    }
    core_t::TTime start = m_LastFinalisedBucketEndTime;
    core_t::TTime end   = m_LastFinalisedBucketEndTime +
                         (m_ModelConfig.latencyBuckets() + 1) * this->effectiveBucketLength();

    if (this->parseTimeRangeInControlMessage(controlMessage, start, end))
    {
        LOG_DEBUG("Time range for results: " << start << " : " << end);
        this->outputResultsWithinRange(true, start, end);
    }
}

bool CAnomalyDetector::parseTimeRangeInControlMessage(const std::string &controlMessage,
                                                      core_t::TTime &start,
                                                      core_t::TTime &end)
{
    typedef core::CStringUtils::TStrVec TStrVec;
    TStrVec tokens;
    std::string remainder;
    core::CStringUtils::tokenise(" ", controlMessage.substr(1, std::string::npos), tokens, remainder);
    if (!remainder.empty())
    {
        tokens.push_back(remainder);
    }
    std::size_t tokensSize = tokens.size();
    if (tokensSize == 0)
    {
        // Default range
        return true;
    }
    if (tokensSize != 2)
    {
        LOG_ERROR("Control message " << controlMessage << " has " << tokensSize <<
                  " parameters when only zero or two are allowed.");
        return false;
    }
    if (core::CStringUtils::stringToType(tokens[0], start)
            && core::CStringUtils::stringToType(tokens[1], end))
    {
        return true;
    }
    LOG_ERROR("Cannot parse control message: " << controlMessage);
    return false;
}

void CAnomalyDetector::outputResults(core_t::TTime bucketStartTime)
{
    static uint64_t cumulativeTime = 0;

    core::CStopWatch timer(true);

    core_t::TTime bucketLength = m_ModelConfig.bucketLength();

    if (m_ModelDebugDataQueue.latestBucketEnd() < bucketLength)
    {
        m_ModelDebugDataQueue.reset(bucketStartTime - m_ModelDebugDataQueue.bucketLength());
    }

    m_ResultsQueue.push(model::CHierarchicalResults(), bucketStartTime);
    model::CHierarchicalResults &results = m_ResultsQueue.get(bucketStartTime);
    m_ModelDebugDataQueue.push(TBoxPlotDataVec(), bucketStartTime);

    std::vector<TKeyAnomalyDetectorPtrUMapCItr> iterators;
    iterators.reserve(m_Detectors.size());
    for (TKeyAnomalyDetectorPtrUMapCItr itr = m_Detectors.begin(); itr != m_Detectors.end(); ++itr)
    {
        iterators.push_back(itr);
    }
    std::sort(iterators.begin(), iterators.end(),
              core::CFunctional::SDereference<maths::COrderings::SFirstLess>());

    for (std::size_t i = 0u; i < iterators.size(); ++i)
    {
        model::CAnomalyDetector *detector(iterators[i]->second.get());
        if (detector == 0)
        {
            LOG_ERROR("Unexpected NULL pointer for key '" << pairDebug(iterators[i]->first) << '\'');
            continue;
        }
        detector->buildResults(bucketStartTime, bucketStartTime + bucketLength, results);

        this->generateModelDebugData(bucketStartTime, bucketStartTime + bucketLength, *detector);
    }

    if (!results.empty())
    {
        results.buildHierarchy();

        this->updateAggregatorAndAggregate(false, results);

        model::CHierarchicalResultsProbabilityFinalizer finalizer;
        results.bottomUpBreadthFirst(finalizer);
        results.pivotsBottomUpBreadthFirst(finalizer);

        model::CHierarchicalResultsPopulator populator(m_Limits);
        results.bottomUpBreadthFirst(populator);
        results.pivotsBottomUpBreadthFirst(populator);

        this->updateQuantilesAndNormalize(false, results);
    }

    core_t::TTime resultsTime = m_ResultsQueue.chooseResultTime(bucketStartTime, bucketLength, results);
    if (resultsTime != 0)
    {
        model::CHierarchicalResults &resultsToOutput = m_ResultsQueue.get(resultsTime);
        this->writeOutResults(false, resultsToOutput, resultsTime, timer, cumulativeTime);
        this->writeOutDebugData(resultsTime);
        cumulativeTime = 0;
    }
    else
    {
        cumulativeTime += timer.stop();
    }

    m_Limits.resourceMonitor().pruneIfRequired(bucketStartTime);
    model::CStringStore::tidyUpNotThreadSafe();
}

void CAnomalyDetector::outputInterimResults(core_t::TTime bucketStartTime)
{
    core::CStopWatch timer(true);

    core_t::TTime bucketLength = m_ModelConfig.bucketLength();

    model::CHierarchicalResults results;
    results.setInterim();

    for (TKeyAnomalyDetectorPtrUMapCItr i = m_Detectors.begin(); i != m_Detectors.end(); ++i)
    {
        model::CAnomalyDetector *detector(i->second.get());
        if (detector == 0)
        {
            LOG_ERROR("Unexpected NULL pointer for key '" << pairDebug(i->first) << '\'');
            continue;
        }
        detector->buildInterimResults(bucketStartTime,
                                      bucketStartTime + bucketLength,
                                      results);
    }

    if (!results.empty())
    {
        results.buildHierarchy();

        this->updateAggregatorAndAggregate(true, results);

        model::CHierarchicalResultsProbabilityFinalizer finalizer;
        results.bottomUpBreadthFirst(finalizer);
        results.pivotsBottomUpBreadthFirst(finalizer);

        model::CHierarchicalResultsPopulator populator(m_Limits);
        results.bottomUpBreadthFirst(populator);
        results.pivotsBottomUpBreadthFirst(populator);

        this->updateQuantilesAndNormalize(true, results);
    }

    // For the case where there are out-of-phase buckets, and there is a gap for an
    // intermediate bucket, output it as interim too
    if (m_ResultsQueue.hasInterimResults())
    {
        core_t::TTime olderTime = bucketStartTime - bucketLength;
        model::CHierarchicalResults &olderResult = m_ResultsQueue.get(olderTime);
        this->writeOutResults(true, olderResult, olderTime, timer, 0l);
    }
    this->writeOutResults(true, results, bucketStartTime, timer, 0l);
}

void CAnomalyDetector::writeOutResults(bool interim, model::CHierarchicalResults &results,
                                       core_t::TTime bucketTime, core::CStopWatch &processingTimer,
                                       uint64_t sumPastProcessingTime)
{
    if (!results.empty())
    {
        LOG_TRACE("Got results object here: " << results.root()->s_RawAnomalyScore << " / " <<
            results.root()->s_NormalizedAnomalyScore << ", count " << results.resultCount()
            << " at " << bucketTime);

        api::CHierarchicalResultsWriter writer(m_Limits,
                                                boost::bind(&CJsonOutputWriter::acceptResult,
                                                            &m_JsonOutputWriter,
                                                            _1),
                                                boost::bind(&CJsonOutputWriter::acceptInfluencer,
                                                            &m_JsonOutputWriter,
                                                            _1, _2, _3));
        results.bottomUpBreadthFirst(writer);
        results.pivotsBottomUpBreadthFirst(writer);

        // Add the bucketTime bucket influencer.
        // Note that the influencer will only be accepted if there are records.
        m_JsonOutputWriter.acceptBucketTimeInfluencer(
            bucketTime,
            results.root()->s_AnnotatedProbability.s_Probability,
            results.root()->s_RawAnomalyScore,
            results.root()->s_NormalizedAnomalyScore);

        if (m_JsonOutputWriter.endOutputBatch(interim,
                    sumPastProcessingTime + processingTimer.stop()) == false)
        {
            LOG_ERROR("Problem writing anomaly output");
        }
        m_LastResultsTime = bucketTime;
    }
}


void CAnomalyDetector::resetBuckets(const std::string &controlMessage)
{
    if (controlMessage.length() == 1)
    {
        LOG_ERROR("Received reset buckets control message without time range");
        return;
    }
    core_t::TTime start = 0;
    core_t::TTime end = 0;
    if (this->parseTimeRangeInControlMessage(controlMessage, start, end))
    {
        core_t::TTime bucketLength = m_ModelConfig.bucketLength();
        core_t::TTime time = maths::CIntegerTools::floor(start, bucketLength);
        core_t::TTime bucketEnd = maths::CIntegerTools::ceil(end, bucketLength);
        while (time < bucketEnd)
        {
            for (TKeyAnomalyDetectorPtrUMapCItr i = m_Detectors.begin(); i != m_Detectors.end(); ++i)
            {
                model::CAnomalyDetector *detector = i->second.get();
                if (detector == 0)
                {
                    LOG_ERROR("Unexpected NULL pointer for key '" << pairDebug(i->first) << '\'');
                    continue;
                }
                LOG_TRACE("Resetting bucket = " << time);
                detector->resetBucket(time);
            }
            time += bucketLength;
        }
    }
}

bool CAnomalyDetector::restoreState(core::CDataSearcher &restoreSearcher,
                                    core_t::TTime &completeToTime)
{
    // Pass on the request in case we're chained
    if (this->outputHandler().restoreState(restoreSearcher,
                                           completeToTime) == false)
    {
        return false;
    }

    size_t numDetectors(0);
    try
    {
        // Restore from Elasticsearch compressed data
        core::CStateDecompressor decompressor(restoreSearcher);
        // TODO: Fix this breach of encapsulation about the results index name once we've decided where we're storing state
        decompressor.setStateRestoreSearch("prelertresults-" + m_JobId, STATE_TYPE);

        core::CDataSearcher::TIStreamP strm(decompressor.search(1, 1));
        if (strm == 0)
        {
            LOG_ERROR("Unable to connect to data store");
            return false;
        }

        if (strm->bad())
        {
            LOG_ERROR("State restoration search returned bad stream");
            return false;
        }

        if (strm->fail())
        {
            // This is not fatal - we just didn't find the given document number
            return true;
        }

        // We're dealing with streaming JSON state
        core::CJsonStateRestoreTraverser traverser(*strm);

        if (this->restoreState(traverser, completeToTime, numDetectors) == false)
        {
            LOG_ERROR("Failed to restore detectors");
            return false;
        }
        LOG_DEBUG("Finished restoration, with " << numDetectors << " detectors");

        if (numDetectors == 1 && m_Detectors.empty())
        {
            // non fatal error
            m_RestoredStateDetail.s_RestoredStateStatus = E_NoDetectorsRecovered;
            return true;
        }

        if (completeToTime > 0)
        {
            core_t::TTime lastBucketEndTime(maths::CIntegerTools::ceil(completeToTime,
                                                                       m_ModelConfig.bucketLength()));

            for (TKeyAnomalyDetectorPtrUMapCItr itr = m_Detectors.begin();
                itr != m_Detectors.end();
                ++itr)
            {
                model::CAnomalyDetector *detector(itr->second.get());
                if (detector == 0)
                {
                    LOG_ERROR("Unexpected NULL pointer for key '" << pairDebug(itr->first) << '\'');
                    continue;
                }

                LOG_DEBUG("Setting lastBucketEndTime to "
                          << lastBucketEndTime << " in detector for '"
                          << detector->description() << '\'');
                detector->lastBucketEndTime() = lastBucketEndTime;
            }
        }
        else
        {
            if (!m_Detectors.empty())
            {
                LOG_ERROR("Inconsistency - " << m_Detectors.size() <<
                          " detectors have been restored but completeToTime is " <<
                          completeToTime);
            }
        }
    }
    catch (std::exception &e)
    {
        LOG_ERROR("Failed to restore state! " << e.what());
        return false;
    }

    return true;
}

bool CAnomalyDetector::restoreState(core::CStateRestoreTraverser &traverser,
                                    core_t::TTime &completeToTime,
                                    std::size_t &numDetectors)
{
    m_RestoredStateDetail.s_RestoredStateStatus = E_Failure;
    m_RestoredStateDetail.s_Extra = boost::none;

    // Call name() to prime the traverser if it hasn't started
    traverser.name();
    if (traverser.isEof())
    {
        m_RestoredStateDetail.s_RestoredStateStatus = E_NoDetectorsRecovered;
        LOG_DEBUG("No data store results - assuming no state exists");
        // This is not an error if no data has been persisted
        return true;
    }

    core_t::TTime lastBucketEndTime(0);
    if (traverser.name() != TIME_TAG ||
        core::CStringUtils::stringToType(traverser.value(),
                                         lastBucketEndTime) == false)
    {
        m_RestoredStateDetail.s_RestoredStateStatus = E_UnexpectedTag;
        LOG_ERROR("Cannot restore anomaly detector - '" << TIME_TAG <<
                  "' element expected but found " <<
                  traverser.name() << '=' << traverser.value());
        return false;
    }
    m_LastFinalisedBucketEndTime = lastBucketEndTime;

    if (lastBucketEndTime > completeToTime)
    {
        LOG_INFO("Processing is already complete to time " << lastBucketEndTime);
        completeToTime = lastBucketEndTime;
    }

    if ((traverser.next() == false) || (traverser.name() != VERSION_TAG))
    {
        m_RestoredStateDetail.s_RestoredStateStatus = E_UnexpectedTag;
        LOG_ERROR("Cannot restore anomaly detector " <<
                  VERSION_TAG << " was expected");
        return false;
    }

    const std::string &stateVersion = traverser.value();
    if (stateVersion != model::CAnomalyDetector::STATE_VERSION)
    {
        m_RestoredStateDetail.s_RestoredStateStatus = E_IncorrectVersion;
        LOG_INFO("Restored anomaly detector state version is " << stateVersion <<
                 " - ignoring it as current state version is " <<
                 model::CAnomalyDetector::STATE_VERSION);

        // This counts as successful restoration
        return true;
    }

    while (traverser.next())
    {
        const std::string &name = traverser.name();
        if (name == TOP_LEVEL_DETECTOR_TAG)
        {
            if (traverser.traverseSubLevel(boost::bind(&CAnomalyDetector::restoreSingleDetector, this, _1)) == false)
            {
                LOG_ERROR("Cannot restore anomaly detector");
                return false;
            }
            ++numDetectors;
        }
        else if (name == RESULTS_AGGREGATOR_TAG)
        {
            if (traverser.traverseSubLevel(boost::bind(&model::CHierarchicalResultsAggregator::acceptRestoreTraverser,
                                                       &m_Aggregator, _1)) == false)
            {
                LOG_ERROR("Cannot restore results aggregator");
                return false;
            }
        }
        else if (name == HIERARCHICAL_RESULTS_TAG)
        {
            core::CPersistUtils::restore(HIERARCHICAL_RESULTS_TAG, m_ResultsQueue, traverser);
        }
        else if (name == MODEL_DEBUG_DATA_TAG)
        {
            core_t::TTime resultsQueueResetTime = m_ModelConfig.bucketResultsDelay() == 0 ?
                                                  m_LastFinalisedBucketEndTime :
                                                  m_LastFinalisedBucketEndTime - this->effectiveBucketLength();
            m_ModelDebugDataQueue.reset(resultsQueueResetTime);
            core::CPersistUtils::restore(MODEL_DEBUG_DATA_TAG, m_ModelDebugDataQueue, traverser);
        }
        else if (name == LATEST_RECORD_TIME_TAG)
        {
            core::CPersistUtils::restore(LATEST_RECORD_TIME_TAG, m_LatestRecordTime, traverser);
        }
        else if (name == LAST_RESULTS_TIME_TAG)
        {
            core::CPersistUtils::restore(LAST_RESULTS_TIME_TAG, m_LastResultsTime, traverser);
        }
    }

    m_RestoredStateDetail.s_RestoredStateStatus = E_Success;

    return true;
}

bool CAnomalyDetector::restoreSingleDetector(core::CStateRestoreTraverser &traverser)
{
    if (traverser.name() != KEY_TAG)
    {
        LOG_ERROR("Cannot restore anomaly detector - " << KEY_TAG <<
                  " element expected but found " << traverser.name() << '=' << traverser.value());

        m_RestoredStateDetail.s_RestoredStateStatus = E_UnexpectedTag;
        return false;
    }

    model::CSearchKey key;
    if (traverser.traverseSubLevel(boost::bind(&model::CAnomalyDetector::keyAcceptRestoreTraverser,
                                               _1,
                                               boost::ref(key))) == false)
    {
        LOG_ERROR("Cannot restore anomaly detector - no key found in " << KEY_TAG);

        m_RestoredStateDetail.s_RestoredStateStatus = E_UnexpectedTag;
        return false;
    }

    if (traverser.next() == false)
    {
        LOG_ERROR("Cannot restore anomaly detector - end of object reached when " <<
                  PARTITION_FIELD_TAG << " was expected");

        m_RestoredStateDetail.s_RestoredStateStatus = E_UnexpectedTag;
        return false;
    }

    if (traverser.name() != PARTITION_FIELD_TAG)
    {
        LOG_ERROR("Cannot restore anomaly detector - " << PARTITION_FIELD_TAG <<
                  " element expected but found " << traverser.name() << '=' << traverser.value());

        m_RestoredStateDetail.s_RestoredStateStatus = E_UnexpectedTag;
        return false;
    }

    std::string partitionFieldValue;
    if (traverser.traverseSubLevel(boost::bind(&model::CAnomalyDetector::partitionFieldAcceptRestoreTraverser,
                                               _1,
                                               boost::ref(partitionFieldValue))) == false)
    {
        LOG_ERROR("Cannot restore anomaly detector - "
                  "no partition field value found in " << PARTITION_FIELD_TAG);

        m_RestoredStateDetail.s_RestoredStateStatus = E_UnexpectedTag;
        return false;
    }

    if (traverser.next() == false)
    {
        LOG_ERROR("Cannot restore anomaly detector - end of object reached when " <<
                  DETECTOR_TAG << " was expected");

        m_RestoredStateDetail.s_RestoredStateStatus = E_UnexpectedTag;
        return false;
    }

    if (traverser.name() != DETECTOR_TAG)
    {
        LOG_ERROR("Cannot restore anomaly detector - " << DETECTOR_TAG <<
                  " element expected but found " <<
                  traverser.name() << '=' << traverser.value());

        m_RestoredStateDetail.s_RestoredStateStatus = E_UnexpectedTag;
        return false;
    }

    if (this->restoreDetectorState(key, partitionFieldValue, traverser) == false ||
        traverser.haveBadState())
    {
        LOG_ERROR("Delegated portion of anomaly detector restore failed");
        m_RestoredStateDetail.s_RestoredStateStatus = E_Failure;
        return false;
    }

    LOG_TRACE("Restored state for " << key.toCue() << "/" << partitionFieldValue);
    return true;
}

bool CAnomalyDetector::restoreDetectorState(const model::CSearchKey &key,
                                            const std::string &partitionFieldValue,
                                            core::CStateRestoreTraverser &traverser)
{
    const TAnomalyDetectorPtr &detector = this->detectorForKey(0, // time reset later
                                                               key,
                                                               partitionFieldValue,
                                                               m_Limits.resourceMonitor());
    if (!detector)
    {
        LOG_ERROR("Detector with key '" <<
                  key.debug() << '/' << partitionFieldValue << "' "
                  "was not recreated on restore - "
                  "memory limit is too low to continue this job");

        m_RestoredStateDetail.s_RestoredStateStatus = E_MemoryLimitReached;
        return false;
    }

    LOG_INFO("Restoring state for detector with key '"
             << key.debug() << '/' << partitionFieldValue << '\'');

    if (traverser.traverseSubLevel(boost::bind(&model::CAnomalyDetector::acceptRestoreTraverser,
                                               detector.get(),
                                               _1)) == false)
    {
        LOG_ERROR("Error restoring anomaly detector for key '" <<
                  key.debug() << '/' << partitionFieldValue << '\'');
        return false;
    }

    return true;
}

bool CAnomalyDetector::persistState(core::CDataAdder &persister)
{
    // Pass on the request in case we're chained
    if (this->outputHandler().persistState(persister) == false)
    {
        return false;
    }

    if (m_LastFinalisedBucketEndTime == 0)
    {
        LOG_INFO("Will not persist detectors as no results have been output");
        return true;
    }

    if (m_PeriodicPersister != 0)
    {
        // This will not happen if finalise() was called before persisting state
        if (m_PeriodicPersister->isBusy())
        {
            LOG_ERROR("Cannot do final persistence of state - periodic "
                      "persister still busy");
            return false;
        }
    }

    TKeyCRefAnomalyDetectorPtrPrVec detectors;
    this->sortedDetectors(detectors);
    std::string normaliserState;
    m_Normalizer.toJson(m_LastResultsTime, "api", normaliserState, true);

    return this->persistState("State persisted due to job close at ",
                              m_ResultsQueue,
                              m_ModelDebugDataQueue,
                              m_LastFinalisedBucketEndTime,
                              detectors,
                              m_Limits.resourceMonitor().createMemoryUsageReport(
                                  m_LastFinalisedBucketEndTime - m_ModelConfig.bucketLength()),
                              m_Aggregator,
                              normaliserState,
                              m_LatestRecordTime,
                              m_LastResultsTime,
                              persister);
}

bool CAnomalyDetector::checkBackgroundPersist(void)
{
    if (m_PeriodicPersister == 0 || m_FirstProcessortPeriodicPersistFunc.empty())
    {
        // It's valid to not have a periodic persister/persist function
        return false;
    }

    core_t::TTime due(m_LastPeriodicPersistTime + m_PeriodicPersistInterval);
    core_t::TTime now(core::CTimeUtils::now());
    if (now < due)
    {
        // Persist is not due
        return false;
    }

    if (m_PeriodicPersister->isBusy())
    {
        m_PeriodicPersistInterval += PERSIST_INTERVAL_INCREMENT;

        LOG_WARN("Periodic persist is due at " << due <<
                 " but previous persist started at " <<
                 m_LastPeriodicPersistTime << " is still in progress - "
                 "increased persistence interval to " <<
                 m_PeriodicPersistInterval << " seconds");

        return false;
    }

    m_LastPeriodicPersistTime = now;

    return m_FirstProcessortPeriodicPersistFunc();
}

bool CAnomalyDetector::backgroundPersistState(CBackgroundPersister &backgroundPersister)
{
    LOG_INFO("Background persist starting data copy");

    // Pass arguments by value: this is what we want for
    // passing to a new thread.
    // Do NOT add boost::ref wrappers around these arguments - they
    // MUST be copied for thread safety
    TBackgroundPersistArgsPtr args(new SBackgroundPersistArgs(m_ResultsQueue,
                                                              m_ModelDebugDataQueue,
                                                              m_LastFinalisedBucketEndTime,
                                                              m_Limits.resourceMonitor().createMemoryUsageReport(
                                                                  m_LastFinalisedBucketEndTime - m_ModelConfig.bucketLength()),
                                                              m_Aggregator,
                                                              m_LatestRecordTime,
                                                              m_LastResultsTime));

    // The normaliser is non-copyable, so we have to make do with JSONifying it now;
    // it should be relatively fast though
    m_Normalizer.toJson(m_LastResultsTime, "api", args->s_NormalizerState, true);

    TKeyCRefAnomalyDetectorPtrPrVec &copiedDetectors = args->s_Detectors;
    copiedDetectors.reserve(m_Detectors.size());

    for (TKeyAnomalyDetectorPtrUMapCItr i = m_Detectors.begin(); i != m_Detectors.end(); ++i)
    {
        model::CAnomalyDetector *detector(i->second.get());
        if (detector == 0)
        {
            LOG_ERROR("Unexpected NULL pointer for key '" << pairDebug(i->first) << '\'');
            continue;
        }
        if (detector->isSimpleCount())
        {
            copiedDetectors.push_back(TKeyCRefAnomalyDetectorPtrPr(
                                          model::CSearchKey::TStrCRefKeyCRefPr(boost::cref(i->first.first),
                                                                               boost::cref(i->first.second)),
                                          TAnomalyDetectorPtr(new model::CSimpleCountDetector(true, *detector))));
        }
        else
        {
            copiedDetectors.push_back(TKeyCRefAnomalyDetectorPtrPr(
                                          model::CSearchKey::TStrCRefKeyCRefPr(boost::cref(i->first.first),
                                                                               boost::cref(i->first.second)),
                                          TAnomalyDetectorPtr(new model::CAnomalyDetector(true, *detector))));
        }
    }
    std::sort(copiedDetectors.begin(), copiedDetectors.end(), maths::COrderings::SFirstLess());

    LOG_INFO("Background persist starting background thread");

    if (backgroundPersister.startPersist(boost::bind(&CAnomalyDetector::runBackgroundPersist,
                                                     this, args, _1)) == false)
    {
        LOG_ERROR("Failed to start background persistence");
        return false;
    }

    return true;
}

bool CAnomalyDetector::runBackgroundPersist(TBackgroundPersistArgsPtr args,
                                            core::CDataAdder &persister)
{
    if (!args)
    {
        LOG_ERROR("Unexpected NULL pointer passed to background persist");
        return false;
    }

    return this->persistState("Periodic background persist at ",
                              args->s_ResultsQueue,
                              args->s_ModelDebugDataQueue,
                              args->s_Time,
                              args->s_Detectors,
                              args->s_ModelSizeStats,
                              args->s_Aggregator,
                              args->s_NormalizerState,
                              args->s_LatestRecordTime,
                              args->s_LastResultsTime,
                              persister);
}

bool CAnomalyDetector::persistState(const std::string &descriptionPrefix,
                                    const model::CResultsQueue &resultsQueue,
                                    const TBoxPlotDataVecQueue &modelDebugDataQueue,
                                    core_t::TTime time,
                                    const TKeyCRefAnomalyDetectorPtrPrVec &detectors,
                                    const model::CResourceMonitor::SResults &modelSizeStats,
                                    const model::CHierarchicalResultsAggregator &aggregator,
                                    const std::string &normalizerState,
                                    core_t::TTime latestRecordTime,
                                    core_t::TTime lastResultsTime,
                                    core::CDataAdder &persister)
{
    // Persist state for each detector separately by streaming
    try
    {
        core::CStateCompressor compressor(persister);

        core_t::TTime snapshotTimestamp(core::CTimeUtils::now());
        const std::string snapShotId(m_JobId + core::CStringUtils::typeToString(snapshotTimestamp));
        // TODO: Fix this breach of encapsulation about the results index name once we've decided where we're storing state
        core::CDataAdder::TOStreamP strm = compressor.addStreamed("prelertresults-" + m_JobId,
                                                                  STATE_TYPE,
                                                                  snapShotId);
        if (strm != 0)
        {
            // IMPORTANT - this method can run in a background thread while the
            // analytics carries on processing new buckets in the main thread.
            // Therefore, this method must NOT access any member variables whose
            // values can change.  There should be no use of m_ variables in the
            // following code block.
            {
                // The JSON inserter must be destructed before the stream is complete
                core::CJsonStatePersistInserter inserter(*strm);
                inserter.insertValue(TIME_TAG, time);
                inserter.insertValue(VERSION_TAG, model::CAnomalyDetector::STATE_VERSION);

                if (resultsQueue.size() > 1)
                {
                    core::CPersistUtils::persist(HIERARCHICAL_RESULTS_TAG, resultsQueue, inserter);
                }
                if (modelDebugDataQueue.size() > 1)
                {
                    core::CPersistUtils::persist(MODEL_DEBUG_DATA_TAG, modelDebugDataQueue, inserter);
                }

                for (std::size_t i = 0u; i < detectors.size(); ++i)
                {
                    const model::CAnomalyDetector *detector(detectors[i].second.get());
                    if (detector == 0)
                    {
                        LOG_ERROR("Unexpected NULL pointer for key '" <<
                                  pairDebug(detectors[i].first) << '\'');
                        continue;
                    }
                    inserter.insertLevel(TOP_LEVEL_DETECTOR_TAG,
                                         boost::bind(&CAnomalyDetector::persistIndividualDetector,
                                                     boost::cref(*detector), _1));

                    LOG_DEBUG("Persisted state for '" << detector->description() << "'");
                }

                inserter.insertLevel(RESULTS_AGGREGATOR_TAG,
                                     boost::bind(&model::CHierarchicalResultsAggregator::acceptPersistInserter,
                                                 &aggregator, _1));

                core::CPersistUtils::persist(LATEST_RECORD_TIME_TAG, latestRecordTime, inserter);
                core::CPersistUtils::persist(LAST_RESULTS_TIME_TAG, lastResultsTime, inserter);
            }

            if (compressor.streamComplete(strm, true) == false || strm->bad())
            {
                LOG_ERROR("Failed to complete last persistence stream");
                return false;
            }

            if (!m_PersistCompleteFunc.empty())
            {
                m_PersistCompleteFunc(snapshotTimestamp,
                                      descriptionPrefix + core::CTimeUtils::toIso8601(snapshotTimestamp),
                                      snapShotId,
                                      compressor.numCompressedDocs(),
                                      modelSizeStats,
                                      normalizerState,
                                      latestRecordTime,
                                      lastResultsTime);
            }
        }
    }
    catch (std::exception &e)
    {
        LOG_ERROR("Failed to persist state! " << e.what());
        return false;
    }

    return true;
}

bool CAnomalyDetector::periodicPersistState(core::CDataAdder &persister)
{
    // Pass on the request in case we're chained
    if (this->outputHandler().periodicPersistState(persister) == false)
    {
        return false;
    }

    if (m_PeriodicPersister == 0)
    {
        // It's valid to not have a periodic persister
        return false;
    }

    // Prune the models so that the persisted state is as neat as possible
    this->pruneAllModels();

    // Make sure model size stats are up to date
    for (TKeyAnomalyDetectorPtrUMapCItr i = m_Detectors.begin(); i != m_Detectors.end(); ++i)
    {
        model::CAnomalyDetector *detector = i->second.get();
        if (detector == 0)
        {
            LOG_ERROR("Unexpected NULL pointer for key '" << pairDebug(i->first) << '\'');
            continue;
        }
        m_Limits.resourceMonitor().forceRefreshNoSend(detector->models());
    }

    return this->backgroundPersistState(*m_PeriodicPersister);
}

void CAnomalyDetector::updateAggregatorAndAggregate(bool isInterim,
                                                    model::CHierarchicalResults &results)
{
    m_Aggregator.refresh(m_ModelConfig);

    m_Aggregator.setJob(model::CHierarchicalResultsAggregator::E_Correct);

    // The equalizers are NOT updated with interim results.
    if (isInterim == false)
    {
        m_Aggregator.setJob(model::CHierarchicalResultsAggregator::E_UpdateAndCorrect);
        m_Aggregator.propagateForwardByTime(1.0);
    }

    results.bottomUpBreadthFirst(m_Aggregator);
    results.createPivots();
    results.pivotsBottomUpBreadthFirst(m_Aggregator);
}

void CAnomalyDetector::updateQuantilesAndNormalize(bool isInterim,
                                                   model::CHierarchicalResults &results)
{
    m_Normalizer.resetBigChange();

    // The normalizers are NOT updated with interim results, in other
    // words interim results are normalized with respect to previous
    // final results.
    if (isInterim == false)
    {
        m_Normalizer.propagateForwardByTime(1.0);
        m_Normalizer.setJob(model::CHierarchicalResultsNormalizer::E_Update);
        results.bottomUpBreadthFirst(m_Normalizer);
        results.pivotsBottomUpBreadthFirst(m_Normalizer);
    }

    m_Normalizer.setJob(model::CHierarchicalResultsNormalizer::E_Normalize);
    results.bottomUpBreadthFirst(m_Normalizer);
    results.pivotsBottomUpBreadthFirst(m_Normalizer);

    if ((isInterim == false &&
         m_Normalizer.hasLastUpdateCausedBigChange()) ||
        (m_MaxQuantileInterval > 0 &&
         core::CTimeUtils::now() > m_LastNormalizerPersistTime + m_MaxQuantileInterval))
    {
        m_JsonOutputWriter.persistNormalizer(m_Normalizer, m_LastNormalizerPersistTime);
    }
}

void CAnomalyDetector::outputResultsWithinRange(bool isInterim,
                                                core_t::TTime start,
                                                core_t::TTime end)
{
    if (m_LastFinalisedBucketEndTime <= 0)
    {
        return;
    }
    if (start < m_LastFinalisedBucketEndTime)
    {
        LOG_WARN("Cannot output results for range (" << start << ", " << m_LastFinalisedBucketEndTime <<
                 "): Start time is before last finalized bucket end time " <<
                 m_LastFinalisedBucketEndTime << '.');
        start = m_LastFinalisedBucketEndTime;
    }
    if (start > end)
    {
        LOG_ERROR("Cannot output results for range (" << start << ", " << end <<
                "): Start time is later than end time.");
        return;
    }
    core_t::TTime bucketLength = m_ModelConfig.bucketLength();
    core_t::TTime time = maths::CIntegerTools::floor(start, bucketLength);
    core_t::TTime bucketEnd = maths::CIntegerTools::ceil(end, bucketLength);
    while (time < bucketEnd)
    {
        if (isInterim)
        {
            this->outputInterimResults(time);
        }
        else
        {
            this->outputResults(time);
        }
        time += bucketLength;
    }
}

void CAnomalyDetector::generateModelDebugData(core_t::TTime startTime,
                                              core_t::TTime endTime,
                                              const model::CAnomalyDetector &detector)
{
    double modelDebugBoundsPercentile(m_ModelConfig.modelDebugBoundsPercentile());
    if (modelDebugBoundsPercentile > 0.0)
    {
        LOG_TRACE("Generating model debug data at " << startTime);
        detector.generateModelDebugData(startTime,
                                        endTime,
                                        m_ModelConfig.modelDebugBoundsPercentile(),
                                        m_ModelConfig.modelDebugTerms(),
                                        m_ModelDebugDataQueue.get(startTime));
    }
}

void CAnomalyDetector::writeOutDebugData(core_t::TTime resultsTime)
{
    double modelDebugBoundsPercentile(m_ModelConfig.modelDebugBoundsPercentile());
    if (modelDebugBoundsPercentile > 0.0)
    {
        LOG_TRACE("Writing debug data at time " << resultsTime);
        switch (m_ModelConfig.modelDebugDestination())
        {
            case model::CModelConfig::E_File:
            {
                std::string file(core::CResourceLocator::logDir() + '/');
                file += m_JobId;
                core::COsFileFuncs::mkdir(file.c_str());
                file += "/modelDebugData.json";
                std::ofstream stream(file.c_str(), std::ios::out | std::ios::app);
                CBoxPlotDataJsonWriter boxPlotWriter(stream);
                this->writeOutDebugData(resultsTime,
                                        boxPlotWriter);
                break;
            }
            case model::CModelConfig::E_DataStore:
            {
                CBoxPlotDataJsonWriter boxPlotWriter(m_JsonOutputWriter.hijackJsonWriter());
                this->writeOutDebugData(resultsTime,
                                        boxPlotWriter);
                break;
            }
        }
    }
}

void CAnomalyDetector::writeOutDebugData(core_t::TTime resultsTime,
                                         CBoxPlotDataJsonWriter &writer)
{
    const TBoxPlotDataVec &modelDebugData = m_ModelDebugDataQueue.get(resultsTime);
    for (TBoxPlotDataVecCItr i = modelDebugData.begin(); i != modelDebugData.end(); ++i)
    {
        writer.writeFlat(m_JobId, *i);
    }
}

void CAnomalyDetector::refreshMemoryAndReport(void)
{
    // Make sure model size stats are up to date and then send a final memory
    // usage report
    for (TKeyAnomalyDetectorPtrUMapCItr i = m_Detectors.begin(); i != m_Detectors.end(); ++i)
    {
        model::CAnomalyDetector *detector = i->second.get();
        if (detector == 0)
        {
            LOG_ERROR("Unexpected NULL pointer for key '" << pairDebug(i->first) << '\'');
            continue;
        }
        m_Limits.resourceMonitor().forceRefreshNoSend(detector->models());
    }
    m_Limits.resourceMonitor().sendMemoryUsageReport(m_LastFinalisedBucketEndTime - m_ModelConfig.bucketLength());
}

void CAnomalyDetector::persistIndividualDetector(const model::CAnomalyDetector &detector,
                                                 core::CStatePersistInserter &inserter)
{
    inserter.insertLevel(KEY_TAG, boost::bind(&model::CAnomalyDetector::keyAcceptPersistInserter, &detector, _1));
    inserter.insertLevel(PARTITION_FIELD_TAG,
                         boost::bind(&model::CAnomalyDetector::partitionFieldAcceptPersistInserter, &detector, _1));
    inserter.insertLevel(DETECTOR_TAG, boost::bind(&model::CAnomalyDetector::acceptPersistInserter, &detector, _1));
}

void CAnomalyDetector::detectors(TAnomalyDetectorPtrVec &detectors) const
{
    detectors.clear();
    detectors.reserve(m_Detectors.size());
    for (TKeyAnomalyDetectorPtrUMapCItr itr = m_Detectors.begin(); itr != m_Detectors.end(); ++itr)
    {
       detectors.push_back(itr->second);
    }
}

void CAnomalyDetector::sortedDetectors(TKeyCRefAnomalyDetectorPtrPrVec &detectors) const
{
    detectors.reserve(m_Detectors.size());
    for (TKeyAnomalyDetectorPtrUMapCItr i = m_Detectors.begin(); i != m_Detectors.end(); ++i)
    {
        detectors.push_back(TKeyCRefAnomalyDetectorPtrPr(
                                model::CSearchKey::TStrCRefKeyCRefPr(boost::cref(i->first.first),
                                                                     boost::cref(i->first.second)),
                                i->second));
    }
    std::sort(detectors.begin(), detectors.end(), maths::COrderings::SFirstLess());
}

const CAnomalyDetector::TKeyAnomalyDetectorPtrUMap &CAnomalyDetector::detectorPartitionMap() const
{
    return m_Detectors;
}

const CAnomalyDetector::TAnomalyDetectorPtr &
CAnomalyDetector::detectorForKey(core_t::TTime time,
                                 const model::CSearchKey &key,
                                 const std::string &partitionFieldValue,
                                 model::CResourceMonitor &resourceMonitor)
{
    // The simple count detector always lives in a special null partition.
    const std::string &partition = key.isSimpleCount() ? EMPTY_STRING : partitionFieldValue;

    // Try and get the detector.
    TKeyAnomalyDetectorPtrUMapCItr itr =
            m_Detectors.find(model::CSearchKey::TStrCRefKeyCRefPr(boost::cref(partition),
                                                                  boost::cref(key)),
                             model::CStrKeyPrHash(),
                             model::CStrKeyPrEqual());

    // Check if we need to and are allowed to create a new detector.
    if (itr == m_Detectors.end() && resourceMonitor.areAllocationsAllowed())
    {
        // Create an placeholder for the anomaly detector.
        model::CAnomalyDetector::TAnomalyDetectorPtr &detector =
                m_Detectors.emplace(model::CSearchKey::TStrKeyPr(partition, key),
                                    TAnomalyDetectorPtr()).first->second;

        LOG_TRACE("Creating new detector for key '"
                  << key.debug() << '/' << partition << '\'' << ", time " << time);
        LOG_TRACE("Detector count " << m_Detectors.size())

        detector = CAnomalyDetectorUtils::makeDetector(key.identifier(),
                                                       m_ModelConfig,
                                                       m_Limits,
                                                       partition,
                                                       time,
                                                       m_ModelConfig.factory(key));
        if (detector == 0)
        {
            // This should never happen as CAnomalyDetectorUtils::makeDetector()
            // contracts to never return NULL
            LOG_ABORT("Failed to create anomaly detector for key '" << key.debug() << '\'');
        }

        detector->zeroModelsToTime(time - m_ModelConfig.latency());

        return detector;
    }
    else if (itr == m_Detectors.end())
    {
        LOG_TRACE("No memory to create new detector for key '"
                  << key.debug() << '/' << partition << '\'');
        return NULL_DETECTOR;
    }

    return itr->second;
}

void CAnomalyDetector::pruneAllModels(void)
{
    LOG_INFO("Pruning all models");

    for (TKeyAnomalyDetectorPtrUMapCItr i = m_Detectors.begin(); i != m_Detectors.end(); ++i)
    {
        model::CAnomalyDetector *detector = i->second.get();
        if (detector == 0)
        {
            LOG_ERROR("Unexpected NULL pointer for key '" << pairDebug(i->first) << '\'');
            continue;
        }
        detector->pruneModels();
    }
}

CAnomalyDetector::SBackgroundPersistArgs::SBackgroundPersistArgs(const model::CResultsQueue &resultsQueue,
                                                                 const TBoxPlotDataVecQueue &modelDebugDataQueue,
                                                                 core_t::TTime time,
                                                                 const model::CResourceMonitor::SResults &modelSizeStats,
                                                                 const model::CHierarchicalResultsAggregator &aggregator,
                                                                 core_t::TTime latestRecordTime,
                                                                 core_t::TTime lastResultsTime) :
        s_ResultsQueue(resultsQueue),
        s_ModelDebugDataQueue(modelDebugDataQueue),
        s_Time(time),
        s_ModelSizeStats(modelSizeStats),
        s_Aggregator(aggregator),
        s_LatestRecordTime(latestRecordTime),
        s_LastResultsTime(lastResultsTime)
{
}


}
}
