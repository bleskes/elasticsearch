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

#include <api/CJsonOutputWriter.h>

#include <core/CJsonDocUtils.h>
#include <core/CScopedLock.h>
#include <core/CStringUtils.h>
#include <core/CTimeUtils.h>

#include <model/CHierarchicalResultsNormalizer.h>
#include <model/ModelTypes.h>

#include <boost/make_shared.hpp>

#include <algorithm>
#include <ostream>

namespace prelert
{
namespace api
{

namespace
{

//! Get the normalizedProbability field from a JSON document.
//! Assumes the document contains a normalizedProbability field.
//! The caller is responsible for ensuring this, and a
//! program crash is likely if this requirement is not met.
inline double normalizedProbabilityFromDocument(const rapidjson::Document &doc)
{
    return doc[CJsonOutputWriter::NORMALIZED_PROBABILITY.c_str()].GetDouble();
}

//! Get the probability field from a JSON document.
//! Assumes the document contains a probability field.
//! The caller is responsible for ensuring this, and a
//! program crash is likely if this requirement is not met.
inline double probabilityFromDocument(const rapidjson::Document &doc)
{
    return doc[CJsonOutputWriter::PROBABILITY.c_str()].GetDouble();
}


inline double initialscoreFromDocument(const rapidjson::Document &doc)
{
    return doc[CJsonOutputWriter::INITIAL_SCORE.c_str()].GetDouble();
}

//! Sort rapidjson documents by the probability lowest to highest
class CProbabilityLess
{
    public:
        bool operator()(const CJsonOutputWriter::TDocumentPtrIntPr &lhs,
                        const CJsonOutputWriter::TDocumentPtrIntPr &rhs) const
        {
            return probabilityFromDocument(*lhs.first) < probabilityFromDocument(*rhs.first);
        }
};

const CProbabilityLess PROBABILITY_LESS = CProbabilityLess();


//! Sort rapidjson documents by detector name first then probability lowest to highest
class CDetectorThenProbabilityLess
{
    public:
        bool operator()(const CJsonOutputWriter::TDocumentPtrIntPr &lhs,
                        const CJsonOutputWriter::TDocumentPtrIntPr &rhs) const
        {
            if (lhs.second == rhs.second)
            {
                return probabilityFromDocument(*lhs.first) < probabilityFromDocument(*rhs.first);
            }
            return lhs.second < rhs.second;
        }
};

const CDetectorThenProbabilityLess DETECTOR_PROBABILITY_LESS = CDetectorThenProbabilityLess();

//! Sort influences from highes to lowest
class CInfluencesLess
{
    public:
        bool operator()(const std::pair<const char*, double> &lhs,
                        const std::pair<const char*, double> &rhs) const
        {
            return lhs.second > rhs.second;
        }
};

const CInfluencesLess INFLUENCE_LESS = CInfluencesLess();

//! Sort influencer from highes to lowest by anomaly score
class CInfluencerLess
{
    public:
    public:
        bool operator()(const CJsonOutputWriter::TDocumentPtr &lhs,
                        const CJsonOutputWriter::TDocumentPtr &rhs) const
        {
            return initialscoreFromDocument(*lhs) > initialscoreFromDocument(*rhs);
        }
};

const CInfluencerLess INFLUENCER_LESS = CInfluencerLess();

}

// JSON field names
const std::string   CJsonOutputWriter::JOB_ID("job_id");
const std::string   CJsonOutputWriter::TIMESTAMP("timestamp");
const std::string   CJsonOutputWriter::BUCKET("bucket");
const std::string   CJsonOutputWriter::LOG_TIME("log_time");
const std::string   CJsonOutputWriter::DETECTOR_INDEX("detector_index");
const std::string   CJsonOutputWriter::RECORDS("records");
const std::string   CJsonOutputWriter::RECORD_COUNT("record_count");
const std::string   CJsonOutputWriter::EVENT_COUNT("event_count");
const std::string   CJsonOutputWriter::IS_INTERIM("is_interim");
const std::string   CJsonOutputWriter::PROBABILITY("probability");
const std::string   CJsonOutputWriter::RAW_ANOMALY_SCORE("raw_anomaly_score");
const std::string   CJsonOutputWriter::ANOMALY_SCORE("anomaly_score");
const std::string   CJsonOutputWriter::NORMALIZED_PROBABILITY("normalized_probability");
const std::string   CJsonOutputWriter::MAX_NORMALIZED_PROBABILITY("max_normalized_probability");
const std::string   CJsonOutputWriter::FIELD_NAME("field_name");
const std::string   CJsonOutputWriter::BY_FIELD_NAME("by_field_name");
const std::string   CJsonOutputWriter::BY_FIELD_VALUE("by_field_value");
const std::string   CJsonOutputWriter::CORRELATED_BY_FIELD_VALUE("correlated_by_field_value");
const std::string   CJsonOutputWriter::TYPICAL("typical");
const std::string   CJsonOutputWriter::ACTUAL("actual");
const std::string   CJsonOutputWriter::CAUSES("causes");
const std::string   CJsonOutputWriter::FUNCTION("function");
const std::string   CJsonOutputWriter::FUNCTION_DESCRIPTION("function_description");
const std::string   CJsonOutputWriter::OVER_FIELD_NAME("over_field_name");
const std::string   CJsonOutputWriter::OVER_FIELD_VALUE("over_field_value");
const std::string   CJsonOutputWriter::PARTITION_FIELD_NAME("partition_field_name");
const std::string   CJsonOutputWriter::PARTITION_FIELD_VALUE("partition_field_value");
const std::string   CJsonOutputWriter::INITIAL_SCORE("initial_anomaly_score");
const std::string   CJsonOutputWriter::INFLUENCER_FIELD_NAME("influencer_field_name");
const std::string   CJsonOutputWriter::INFLUENCER_FIELD_VALUE("influencer_field_value");
const std::string   CJsonOutputWriter::INFLUENCER_FIELD_VALUES("influencer_field_values");
const std::string   CJsonOutputWriter::BUCKET_INFLUENCERS("bucket_influencers");
const std::string   CJsonOutputWriter::INFLUENCERS("influencers");
const std::string   CJsonOutputWriter::FLUSH("flush");
const std::string   CJsonOutputWriter::ID("id");
const std::string   CJsonOutputWriter::QUANTILE_STATE("quantile_state");
const std::string   CJsonOutputWriter::QUANTILES("quantiles");
const std::string   CJsonOutputWriter::MODEL_SIZE_STATS("model_size_stats");
const std::string   CJsonOutputWriter::MODEL_BYTES("model_bytes");
const std::string   CJsonOutputWriter::TOTAL_BY_FIELD_COUNT("total_by_field_count");
const std::string   CJsonOutputWriter::TOTAL_OVER_FIELD_COUNT("total_over_field_count");
const std::string   CJsonOutputWriter::TOTAL_PARTITION_FIELD_COUNT("total_partition_field_count");
const std::string   CJsonOutputWriter::BUCKET_ALLOCATION_FAILURES_COUNT("bucket_allocation_failures_count");
const std::string   CJsonOutputWriter::MEMORY_STATUS("memory_status");
const std::string   CJsonOutputWriter::CATEGORY_ID("category_id");
const std::string   CJsonOutputWriter::CATEGORY_DEFINITION("category_definition");
const std::string   CJsonOutputWriter::TERMS("terms");
const std::string   CJsonOutputWriter::REGEX("regex");
const std::string   CJsonOutputWriter::MAX_MATCHING_LENGTH("max_matching_length");
const std::string   CJsonOutputWriter::EXAMPLES("examples");
const std::string   CJsonOutputWriter::MODEL_SNAPSHOT("model_snapshot");
const std::string   CJsonOutputWriter::SNAPSHOT_ID("snapshot_id");
const std::string   CJsonOutputWriter::SNAPSHOT_DOC_COUNT("snapshot_doc_count");
const std::string   CJsonOutputWriter::RESTORE_PRIORITY("restore_priority");
const std::string   CJsonOutputWriter::DESCRIPTION("description");
const std::string   CJsonOutputWriter::LATEST_RECORD_TIME("latest_record_time_stamp");
const std::string   CJsonOutputWriter::BUCKET_SPAN("bucket_span");
const std::string   CJsonOutputWriter::LATEST_RESULT_TIME("latest_result_time_stamp");
const std::string   CJsonOutputWriter::PROCESSING_TIME("processing_time_ms");
const std::string   CJsonOutputWriter::TIME_INFLUENCER("bucket_time");
const std::string   CJsonOutputWriter::PARTITION_SCORES("partition_scores");
const std::string   CJsonOutputWriter::SEQUENCE_NUM("sequence_num");


CJsonOutputWriter::CJsonOutputWriter(const std::string &jobId)
    : m_JobId(jobId),
      m_WriteStream(m_StringOutputBuf),
      m_Writer(m_WriteStream),
      m_LastNonInterimBucketTime(0),
      m_CloseJsonStructures(false),
      m_Finalised(false),
      m_RecordOutputLimit(0),
      m_JsonPoolAllocator(m_FixedBuffer, FIXED_BUFFER_SIZE)
{
    // Don't write any output in the constructor because, the way things work at
    // the moment, the output stream might be redirected after construction
}

CJsonOutputWriter::CJsonOutputWriter(const std::string &jobId, std::ostream &strmOut)
    : m_JobId(jobId),
      m_WriteStream(strmOut),
      m_Writer(m_WriteStream),
      m_LastNonInterimBucketTime(0),
      m_CloseJsonStructures(false),
      m_Finalised(false),
      m_RecordOutputLimit(0),
      m_JsonPoolAllocator(m_FixedBuffer, FIXED_BUFFER_SIZE)
{
    // Don't write any output in the constructor because, the way things work at
    // the moment, the output stream might be redirected after construction
}

CJsonOutputWriter::~CJsonOutputWriter(void)
{
    finalise();
}

void CJsonOutputWriter::finalise(void)
{
    if (m_Finalised)
    {
        return;
    }

    // All output is in a JSON array, but only start it once
    if (!m_CloseJsonStructures)
    {
        m_Writer.StartArray();
        m_CloseJsonStructures = true;
    }

    this->writeModelSnapshotReports();

    // End the top level array
    m_Writer.EndArray();
    m_CloseJsonStructures = false;

    // Final flush of output
    // NB: This may seem silly in that you'd expect the destructor to flush
    // and close, but the std::ostream which m_WriteStream wraps is held by
    // reference so won't be destroyed here.  Explicitly flushing m_WriteStream
    // cascades through to flushing the underlying std::ostream too, whereas
    // destroying m_WriteStream will simply make sure the contents are passed on
    // to the underlying std::ostream.
    m_WriteStream.Flush();

    m_Finalised = true;
}

bool CJsonOutputWriter::acceptResult(const CHierarchicalResultsWriter::TResults &results)
{
    SBucketData &bucketData = m_BucketDataByTime[results.s_BucketStartTime];

    if (results.s_ResultType == CHierarchicalResultsWriter::E_SimpleCountResult)
    {
        if (!results.s_CurrentRate)
        {
            LOG_ERROR("Simple count detector has no current rate");
            return false;
        }

        bucketData.s_InputEventCount = *results.s_CurrentRate;
        bucketData.s_BucketSpan = results.s_BucketSpan;
        return true;
    }

    TDocumentPtr newDoc;
    if (!results.s_IsOverallResult)
    {
        newDoc = boost::make_shared<rapidjson::Document>(&m_JsonPoolAllocator);
        newDoc->SetObject();
        this->addPopulationCauseFields(results, *newDoc);
        m_NestedDocs.push_back(newDoc);
        return true;
    }

    if (results.s_ResultType == CHierarchicalResultsWriter::E_PartitionResult)
    {
        TDocumentPtr partitionDoc = boost::make_shared<rapidjson::Document>(&m_JsonPoolAllocator);
        partitionDoc->SetObject();
        this->addPartitionScores(results, *partitionDoc);
        bucketData.s_PartitionScoreDocuments.push_back(partitionDoc);
        return true;
    }

    ++bucketData.s_RecordCount;

    TDocumentPtrIntPrVec &detectorDocumentsToWrite = bucketData.s_DocumentsToWrite;

    bool makeHeap(false);
    // If a max number of records to output has not been set or we haven't
    // reached that limit yet just append the new document to the array
    if (m_RecordOutputLimit == 0 ||
        bucketData.s_RecordCount <= m_RecordOutputLimit)
    {
        newDoc = boost::make_shared<rapidjson::Document>(&m_JsonPoolAllocator);
        newDoc->SetObject();
        detectorDocumentsToWrite.push_back(TDocumentPtrIntPr(newDoc, results.s_Identifier));

        // the document array is now full, make a max heap
        makeHeap = bucketData.s_RecordCount == m_RecordOutputLimit;
    }
    else
    {
        // Have reached the limit of records to write so compare the new doc
        // to the highest probability anomaly doc and replace if more anomalous
        if (results.s_Probability >= bucketData.s_HighestProbability)
        {
            // Discard any associated nested docs
            m_NestedDocs.clear();
            return true;
        }

        newDoc = boost::make_shared<rapidjson::Document>(&m_JsonPoolAllocator);
        newDoc->SetObject();

        // remove the highest prob doc and insert new one
        std::pop_heap(detectorDocumentsToWrite.begin(), detectorDocumentsToWrite.end(),
                    PROBABILITY_LESS);
        detectorDocumentsToWrite.pop_back();

        detectorDocumentsToWrite.push_back(TDocumentPtrIntPr(newDoc, results.s_Identifier));

        makeHeap = true;
    }

    // The check for population results must come first because some population
    // results are also metrics
    if (results.s_ResultType == CHierarchicalResultsWriter::E_PopulationResult)
    {
        this->addPopulationFields(results, *newDoc);
    }
    else if (results.s_IsMetric)
    {
        this->addMetricFields(results, *newDoc);
    }
    else
    {
        this->addEventRateFields(results, *newDoc);
    }


    this->addInfluences(results.s_Influences, *newDoc);

    if (makeHeap)
    {
        std::make_heap(detectorDocumentsToWrite.begin(), detectorDocumentsToWrite.end(),
                PROBABILITY_LESS);

        bucketData.s_HighestProbability = probabilityFromDocument(
                                *detectorDocumentsToWrite.front().first);
        makeHeap = false;
    }

    return true;
}

bool CJsonOutputWriter::acceptInfluencer(core_t::TTime time,
                                         const model::CHierarchicalResults::TNode &node,
                                         bool isBucketInfluencer)
{
    TDocumentPtr newDoc = boost::make_shared<rapidjson::Document>(&m_JsonPoolAllocator);
    newDoc->SetObject();

    SBucketData &bucketData = m_BucketDataByTime[time];
    TDocumentPtrVec &documents = (isBucketInfluencer) ? bucketData.s_BucketInfluencerDocuments :
                                                bucketData.s_InfluencerDocuments;

    bool isLimitedWrite(m_RecordOutputLimit > 0);

    if (isLimitedWrite && documents.size() == m_RecordOutputLimit)
    {
        double &lowestScore = (isBucketInfluencer) ? bucketData.s_LowestBucketInfluencerScore :
                                                    bucketData.s_LowestInfluencerScore;

        if (node.s_NormalizedAnomalyScore < lowestScore)
        {
            //  Don't write this influencer
            return true;
        }

        // need to remove the lowest score record
        documents.pop_back();
    }

    this->addInfluencerFields(isBucketInfluencer, node, *newDoc);
    documents.push_back(newDoc);

    bool sortVectorAfterWritingDoc = isLimitedWrite && documents.size() >= m_RecordOutputLimit;

    if (sortVectorAfterWritingDoc)
    {
        std::sort(documents.begin(), documents.end(), INFLUENCER_LESS);
    }

    if (isBucketInfluencer)
    {
        bucketData.s_MaxBucketInfluencerNormalizedAnomalyScore =
                std::max(bucketData.s_MaxBucketInfluencerNormalizedAnomalyScore,
                    node.s_NormalizedAnomalyScore);

        bucketData.s_LowestBucketInfluencerScore = std::min(
                                        bucketData.s_LowestBucketInfluencerScore,
                                        initialscoreFromDocument(*documents.back()));
    }
    else
    {
        bucketData.s_LowestInfluencerScore = std::min(
                                        bucketData.s_LowestInfluencerScore,
                                        initialscoreFromDocument(*documents.back()));
    }

    return true;
}

void CJsonOutputWriter::acceptBucketTimeInfluencer(core_t::TTime time,
                                                   double probability,
                                                   double rawAnomalyScore,
                                                   double normalizedAnomalyScore)
{
    SBucketData &bucketData = m_BucketDataByTime[time];
    if (bucketData.s_RecordCount == 0)
    {
        return;
    }

    TDocumentPtr newDoc = boost::make_shared<rapidjson::Document>(&m_JsonPoolAllocator);
    newDoc->SetObject();
    core::CJsonDocUtils::addStringFieldToObj(INFLUENCER_FIELD_NAME,
                                             TIME_INFLUENCER,
                                             *newDoc, m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleFieldToObj(PROBABILITY,
                                             probability,
                                             *newDoc, m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleFieldToObj(RAW_ANOMALY_SCORE,
                                             rawAnomalyScore,
                                             *newDoc, m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleFieldToObj(INITIAL_SCORE,
                                             normalizedAnomalyScore,
                                             *newDoc, m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleFieldToObj(ANOMALY_SCORE,
                                             normalizedAnomalyScore,
                                             *newDoc, m_JsonPoolAllocator);

    bucketData.s_MaxBucketInfluencerNormalizedAnomalyScore = std::max(
                    bucketData.s_MaxBucketInfluencerNormalizedAnomalyScore, normalizedAnomalyScore);
    bucketData.s_BucketInfluencerDocuments.push_back(newDoc);
}

bool CJsonOutputWriter::endOutputBatch(bool isInterim, uint64_t bucketProcessingTime)
{
    // All output is in a JSON array, but only start it once
    if (!m_CloseJsonStructures)
    {
        m_Writer.StartArray();
        m_CloseJsonStructures = true;
    }

    this->writeModelSnapshotReports();

    for (TTimeBucketDataMapItr iter = m_BucketDataByTime.begin();
         iter != m_BucketDataByTime.end();
         ++iter)
    {
        this->writeBucket(isInterim, iter->first, iter->second, bucketProcessingTime);
        if (!isInterim)
        {
            m_LastNonInterimBucketTime = iter->first;
        }
    }

    // After writing the buckets clear all the bucket data so that we don't
    // accumulate memory.
    m_BucketDataByTime.clear();
    m_NestedDocs.clear();

    // Also clear the allocator, otherwise a lot of the memory referenced by the
    // deleted documents will remain.
    // DANGER: this assumes that the memory allocated by the allocator
    // was ONLY used within the documents in the map and the vector.
    m_JsonPoolAllocator.Clear();

    return true;
}

bool CJsonOutputWriter::fieldNames(const TStrVec &/*fieldNames*/,
                                   const TStrVec &/*extraFieldNames*/)
{
    return true;
}

const CJsonOutputWriter::TStrVec &CJsonOutputWriter::fieldNames(void) const
{
    return EMPTY_FIELD_NAMES;
}

bool CJsonOutputWriter::writeRow(bool isDryRun,
                                 const TStrStrUMap &dataRowFields,
                                 const TStrStrUMap &overrideDataRowFields)
{
    if (isDryRun)
    {
        return true;
    }

    // All output is in a JSON array, but only start it once
    if (!m_CloseJsonStructures)
    {
        m_Writer.StartArray();
        m_CloseJsonStructures = true;
    }

    rapidjson::Document doc;
    doc.SetObject();

    // Write all the fields to the document as strings
    // No need to copy the strings as the doc is written straight away
    for (TStrStrUMapCItr fieldValueIter = dataRowFields.begin();
         fieldValueIter != dataRowFields.end();
         ++fieldValueIter)
    {
        const std::string &name = fieldValueIter->first;
        const std::string &value = fieldValueIter->second;

        // Only output fields that aren't overridden
        if (overrideDataRowFields.find(name) == overrideDataRowFields.end())
        {
            rapidjson::Value v;
            v.SetString(value.c_str(),
                        static_cast<rapidjson::SizeType>(value.size()));
            doc.AddMember(name.c_str(), v, doc.GetAllocator());
        }
    }

    for (TStrStrUMapCItr fieldValueIter = overrideDataRowFields.begin();
         fieldValueIter != overrideDataRowFields.end();
         ++fieldValueIter)
    {
        const std::string &name = fieldValueIter->first;
        const std::string &value = fieldValueIter->second;

        rapidjson::Value v;
        v.SetString(value.c_str(),
                    static_cast<rapidjson::SizeType>(value.size()));
        doc.AddMember(name.c_str(), v, doc.GetAllocator());
    }

    doc.Accept(m_Writer);

    return true;
}

void CJsonOutputWriter::writeBucket(bool isInterim,
                                    core_t::TTime bucketTime,
                                    SBucketData &bucketData,
                                    uint64_t bucketProcessingTime)
{
    double maxUnusualScore(0.0);
    // Each document within the bucket that might need renormalising needs a
    // unique sequence number
    size_t sequenceNum(1);

    // Write records
    if (!bucketData.s_DocumentsToWrite.empty())
    {
        // Sort the results so they are grouped by detector and
        // ordered by probability
        std::sort(bucketData.s_DocumentsToWrite.begin(),
                  bucketData.s_DocumentsToWrite.end(),
                  DETECTOR_PROBABILITY_LESS);

        m_Writer.StartObject();
        m_Writer.String(RECORDS.c_str());
        m_Writer.StartArray();

        // Iterate over the different detectors that we have results for
        for (TDocumentPtrIntPrVecItr detectorIter = bucketData.s_DocumentsToWrite.begin();
             detectorIter != bucketData.s_DocumentsToWrite.end();
             ++detectorIter)
        {
            // Write the document, adding the detectorIndex, unusual score and anomaly score as we go
            int detectorIndex = detectorIter->second;
            rapidjson::Document *docPtr = detectorIter->first.get();

            // Bucket unsual score is defined to be the highest of all
            // the records in the bucket
            maxUnusualScore = std::max(maxUnusualScore, normalizedProbabilityFromDocument(*docPtr));

            core::CJsonDocUtils::addIntFieldToObj(DETECTOR_INDEX, detectorIndex,
                    *docPtr, m_JsonPoolAllocator);
            core::CJsonDocUtils::addDoubleFieldToObj(ANOMALY_SCORE,
                                      bucketData.s_MaxBucketInfluencerNormalizedAnomalyScore,
                                      *docPtr, m_JsonPoolAllocator);
            core::CJsonDocUtils::addIntFieldToObj(BUCKET_SPAN,
                                      bucketData.s_BucketSpan,
                                      *docPtr, m_JsonPoolAllocator);
            core::CJsonDocUtils::addStringFieldToObj(JOB_ID, m_JobId, *docPtr, m_JsonPoolAllocator);
            core::CJsonDocUtils::addIntFieldToObj(TIMESTAMP,
                                                  bucketTime * 1000,
                                                  *docPtr, m_JsonPoolAllocator);
            if (isInterim)
            {
                core::CJsonDocUtils::addBoolFieldToObj(IS_INTERIM,
                                        isInterim,
                                        *docPtr, m_JsonPoolAllocator);
            }
            core::CJsonDocUtils::addUIntFieldToObj(SEQUENCE_NUM, sequenceNum++,
                    *docPtr, m_JsonPoolAllocator);
            docPtr->Accept(m_Writer);
        }
        m_Writer.EndArray();
        m_Writer.EndObject();
    }

    // Write influencers
    if (!bucketData.s_InfluencerDocuments.empty())
    {
        m_Writer.StartObject();
        m_Writer.String(INFLUENCERS.c_str());
        m_Writer.StartArray();
        for (TDocumentPtrVecItr influencerIter = bucketData.s_InfluencerDocuments.begin();
             influencerIter != bucketData.s_InfluencerDocuments.end();
             ++influencerIter)
        {
            rapidjson::Document *docPtr = influencerIter->get();

            core::CJsonDocUtils::addStringFieldToObj(JOB_ID, m_JobId, **influencerIter,
                m_JsonPoolAllocator);
            core::CJsonDocUtils::addIntFieldToObj(TIMESTAMP,
                                                  bucketTime * 1000,
                                                  *docPtr, m_JsonPoolAllocator);
            if (isInterim)
            {
                core::CJsonDocUtils::addBoolFieldToObj(IS_INTERIM,
                                                       isInterim,
                                                       *docPtr, m_JsonPoolAllocator);
            }
            core::CJsonDocUtils::addIntFieldToObj(BUCKET_SPAN,
                                      bucketData.s_BucketSpan,
                                      *docPtr, m_JsonPoolAllocator);
            core::CJsonDocUtils::addUIntFieldToObj(SEQUENCE_NUM, sequenceNum++,
                    *docPtr, m_JsonPoolAllocator);
            docPtr->Accept(m_Writer);
        }
        m_Writer.EndArray();
        m_Writer.EndObject();
    }

    // Write bucket at the end, as some of its values need to iterate over records, etc.
    m_Writer.StartObject();
    m_Writer.String(BUCKET.c_str());

    m_Writer.StartObject();
    m_Writer.String(JOB_ID.c_str(), static_cast<rapidjson::SizeType>(JOB_ID.length()));
    m_Writer.String(m_JobId.c_str(), static_cast<rapidjson::SizeType>(m_JobId.length()));
    m_Writer.String(TIMESTAMP.c_str(),
                    static_cast<rapidjson::SizeType>(TIMESTAMP.length()));
    m_Writer.Int64(bucketTime * 1000);

    m_Writer.String(ANOMALY_SCORE.c_str(),
                    static_cast<rapidjson::SizeType>(ANOMALY_SCORE.length()));
    m_Writer.Double(bucketData.s_MaxBucketInfluencerNormalizedAnomalyScore);
    m_Writer.String(INITIAL_SCORE.c_str(),
                    static_cast<rapidjson::SizeType>(INITIAL_SCORE.length()));
    m_Writer.Double(bucketData.s_MaxBucketInfluencerNormalizedAnomalyScore);
    m_Writer.String(MAX_NORMALIZED_PROBABILITY.c_str(),
                    static_cast<rapidjson::SizeType>(MAX_NORMALIZED_PROBABILITY.length()));
    m_Writer.Double(maxUnusualScore);
    m_Writer.String(RECORD_COUNT.c_str(),
                    static_cast<rapidjson::SizeType>(RECORD_COUNT.length()));
    m_Writer.Uint64(bucketData.s_RecordCount);
    m_Writer.String(EVENT_COUNT.c_str(),
                    static_cast<rapidjson::SizeType>(EVENT_COUNT.length()));
    m_Writer.Uint64(bucketData.s_InputEventCount);
    if (isInterim)
    {
        m_Writer.String(IS_INTERIM.c_str(),
                        static_cast<rapidjson::SizeType>(IS_INTERIM.length()));
        m_Writer.Bool(isInterim);
    }
    m_Writer.String(BUCKET_SPAN.c_str(),
                    static_cast<rapidjson::SizeType>(BUCKET_SPAN.length()));
    m_Writer.Int64(bucketData.s_BucketSpan);

    if (!bucketData.s_BucketInfluencerDocuments.empty())
    {
        // Write the array of influencers
        m_Writer.String(BUCKET_INFLUENCERS.c_str(),
                            static_cast<rapidjson::SizeType>(BUCKET_INFLUENCERS.length()));
        m_Writer.StartArray();
        for (TDocumentPtrVecItr influencerIter = bucketData.s_BucketInfluencerDocuments.begin();
             influencerIter != bucketData.s_BucketInfluencerDocuments.end();
             ++influencerIter)
        {
            rapidjson::Document *docPtr = influencerIter->get();

            core::CJsonDocUtils::addStringFieldToObj(JOB_ID, m_JobId, *docPtr,
                m_JsonPoolAllocator);
            core::CJsonDocUtils::addIntFieldToObj(TIMESTAMP,
                                      bucketTime * 1000,
                                      *docPtr, m_JsonPoolAllocator);
            core::CJsonDocUtils::addIntFieldToObj(BUCKET_SPAN,
                                      bucketData.s_BucketSpan,
                                      *docPtr, m_JsonPoolAllocator);
            if (isInterim)
            {
                core::CJsonDocUtils::addBoolFieldToObj(IS_INTERIM,
                                        isInterim,
                                        *docPtr, m_JsonPoolAllocator);
            }
            core::CJsonDocUtils::addUIntFieldToObj(SEQUENCE_NUM, sequenceNum++,
                    *docPtr, m_JsonPoolAllocator);
            docPtr->Accept(m_Writer);
        }
        m_Writer.EndArray();
    }

    if (!bucketData.s_PartitionScoreDocuments.empty())
    {
        // Write the array of partition-anonaly score pairs
        m_Writer.String(PARTITION_SCORES.c_str(),
                            static_cast<rapidjson::SizeType>(PARTITION_SCORES.length()));
        m_Writer.StartArray();
        for (TDocumentPtrVecItr partitionScoresIter = bucketData.s_PartitionScoreDocuments.begin();
             partitionScoresIter != bucketData.s_PartitionScoreDocuments.end();
             ++partitionScoresIter)
        {
            rapidjson::Document *docPtr = partitionScoresIter->get();

            docPtr->Accept(m_Writer);
        }
        m_Writer.EndArray();
    }

    m_Writer.String(PROCESSING_TIME.c_str(),
                        static_cast<rapidjson::SizeType>(PROCESSING_TIME.length()));
    m_Writer.Uint64(bucketProcessingTime);

    m_Writer.EndObject();
    m_Writer.EndObject();
}

void CJsonOutputWriter::addMetricFields(const CHierarchicalResultsWriter::TResults &results,
                                        rapidjson::Document &doc)
{
    // normalizedProbability, probability, fieldName, byFieldName, byFieldValue, partitionFieldName,
    // partitionFieldValue, function, typical, actual. influences?
    core::CJsonDocUtils::addDoubleFieldToObj(NORMALIZED_PROBABILITY, results.s_NormalizedAnomalyScore, doc,
                                                m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleFieldToObj(PROBABILITY, results.s_Probability, doc,
                                                m_JsonPoolAllocator);
    core::CJsonDocUtils::addStringFieldToObj(FIELD_NAME, results.s_MetricValueField, doc,
                                                m_JsonPoolAllocator);
    if (!results.s_ByFieldName.empty())
    {
        core::CJsonDocUtils::addStringFieldToObj(BY_FIELD_NAME, results.s_ByFieldName, doc,
                                                m_JsonPoolAllocator);
        // If name is present then force output of value too, even when empty
        core::CJsonDocUtils::addStringFieldToObj(BY_FIELD_VALUE, results.s_ByFieldValue, doc,
                                                m_JsonPoolAllocator, true);
        // But allow correlatedByFieldValue to be unset if blank
        core::CJsonDocUtils::addStringFieldToObj(CORRELATED_BY_FIELD_VALUE, results.s_CorrelatedByFieldValue, doc,
                                                m_JsonPoolAllocator);
    }
    if (!results.s_PartitionFieldName.empty())
    {
        core::CJsonDocUtils::addStringFieldToObj(PARTITION_FIELD_NAME, results.s_PartitionFieldName,
                                                doc, m_JsonPoolAllocator);
        // If name is present then force output of value too, even when empty
        core::CJsonDocUtils::addStringFieldToObj(PARTITION_FIELD_VALUE, results.s_PartitionFieldValue,
                                                doc, m_JsonPoolAllocator, true);
    }
    core::CJsonDocUtils::addStringFieldToObj(FUNCTION, results.s_FunctionName, doc,
                                                m_JsonPoolAllocator);
    core::CJsonDocUtils::addStringFieldToObj(FUNCTION_DESCRIPTION, results.s_FunctionDescription, doc,
                                                m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleArrayFieldToObj(TYPICAL, results.s_BaselineMean, doc,
                                                m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleArrayFieldToObj(ACTUAL, results.s_CurrentMean, doc,
                                                m_JsonPoolAllocator);
}

void CJsonOutputWriter::addPopulationFields(const CHierarchicalResultsWriter::TResults &results,
                                            rapidjson::Document &doc)
{
    // normalizedProbability, probability, fieldName, byFieldName,
    // overFieldName, overFieldValue, partitionFieldName, partitionFieldValue,
    // function, causes, influences?
    core::CJsonDocUtils::addDoubleFieldToObj(NORMALIZED_PROBABILITY, results.s_NormalizedAnomalyScore, doc,
                                             m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleFieldToObj(PROBABILITY, results.s_Probability, doc,
                                             m_JsonPoolAllocator);
    core::CJsonDocUtils::addStringFieldToObj(FIELD_NAME, results.s_MetricValueField, doc,
                                             m_JsonPoolAllocator);
    // There are no by field values at this level for population
    // results - they're in the "causes" object
    core::CJsonDocUtils::addStringFieldToObj(BY_FIELD_NAME, results.s_ByFieldName, doc,
                                             m_JsonPoolAllocator);
    if (!results.s_OverFieldName.empty())
    {
        core::CJsonDocUtils::addStringFieldToObj(OVER_FIELD_NAME, results.s_OverFieldName, doc,
                                             m_JsonPoolAllocator);
        // If name is present then force output of value too, even when empty
        core::CJsonDocUtils::addStringFieldToObj(OVER_FIELD_VALUE, results.s_OverFieldValue, doc,
                                             m_JsonPoolAllocator, true);
    }
    if (!results.s_PartitionFieldName.empty())
    {
        core::CJsonDocUtils::addStringFieldToObj(PARTITION_FIELD_NAME, results.s_PartitionFieldName, doc,
                                             m_JsonPoolAllocator);
        // If name is present then force output of value too, even when empty
        core::CJsonDocUtils::addStringFieldToObj(PARTITION_FIELD_VALUE, results.s_PartitionFieldValue, doc,
                                             m_JsonPoolAllocator, true);
    }
    core::CJsonDocUtils::addStringFieldToObj(FUNCTION, results.s_FunctionName, doc,
                                            m_JsonPoolAllocator);
    core::CJsonDocUtils::addStringFieldToObj(FUNCTION_DESCRIPTION,
                results.s_FunctionDescription, doc, m_JsonPoolAllocator);

    // Add nested causes
    if (m_NestedDocs.size() > 0)
    {
        rapidjson::Value causeArray(rapidjson::kArrayType);
        causeArray.Reserve(static_cast<rapidjson::SizeType>(m_NestedDocs.size()),
                           m_JsonPoolAllocator);
        for (size_t index = 0; index < m_NestedDocs.size(); ++index)
        {
            rapidjson::Value &docAsValue = *m_NestedDocs[index];
            causeArray.PushBack(docAsValue, m_JsonPoolAllocator);
        }
        doc.AddMember(CAUSES.c_str(), causeArray, m_JsonPoolAllocator);

        m_NestedDocs.clear();
    }
    else
    {
        LOG_WARN("Expected some causes for a population anomaly but got none");
    }
}

void CJsonOutputWriter::addPopulationCauseFields(const CHierarchicalResultsWriter::TResults &results,
                                                 rapidjson::Document &doc)
{
    // probability, fieldName, byFieldName, byFieldValue,
    // overFieldName, overFieldValue, partitionFieldName, partitionFieldValue,
    // function, typical, actual, influences
    core::CJsonDocUtils::addDoubleFieldToObj(PROBABILITY, results.s_Probability, doc,
                                            m_JsonPoolAllocator);
    core::CJsonDocUtils::addStringFieldToObj(FIELD_NAME, results.s_MetricValueField, doc,
                                            m_JsonPoolAllocator);
    if (!results.s_ByFieldName.empty())
    {
        core::CJsonDocUtils::addStringFieldToObj(BY_FIELD_NAME, results.s_ByFieldName, doc,
                                            m_JsonPoolAllocator);
        // If name is present then force output of value too, even when empty
        core::CJsonDocUtils::addStringFieldToObj(BY_FIELD_VALUE, results.s_ByFieldValue, doc,
                                            m_JsonPoolAllocator, true);
        // But allow correlatedByFieldValue to be unset if blank
        core::CJsonDocUtils::addStringFieldToObj(CORRELATED_BY_FIELD_VALUE, results.s_CorrelatedByFieldValue, doc,
                                            m_JsonPoolAllocator);
    }
    if (!results.s_OverFieldName.empty())
    {
        core::CJsonDocUtils::addStringFieldToObj(OVER_FIELD_NAME, results.s_OverFieldName, doc,
                                            m_JsonPoolAllocator);
        // If name is present then force output of value too, even when empty
        core::CJsonDocUtils::addStringFieldToObj(OVER_FIELD_VALUE, results.s_OverFieldValue, doc,
                                            m_JsonPoolAllocator, true);
    }
    if (!results.s_PartitionFieldName.empty())
    {
        core::CJsonDocUtils::addStringFieldToObj(PARTITION_FIELD_NAME, results.s_PartitionFieldName, doc,
                                            m_JsonPoolAllocator);
        // If name is present then force output of value too, even when empty
        core::CJsonDocUtils::addStringFieldToObj(PARTITION_FIELD_VALUE, results.s_PartitionFieldValue, doc,
                                            m_JsonPoolAllocator, true);
    }
    core::CJsonDocUtils::addStringFieldToObj(FUNCTION, results.s_FunctionName, doc,
                                            m_JsonPoolAllocator);
    core::CJsonDocUtils::addStringFieldToObj(FUNCTION_DESCRIPTION,
                results.s_FunctionDescription, doc, m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleArrayFieldToObj(TYPICAL, results.s_PopulationAverage, doc,
                                            m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleArrayFieldToObj(ACTUAL, results.s_FunctionValue, doc,
                                            m_JsonPoolAllocator);
}

void CJsonOutputWriter::addInfluences(const CHierarchicalResultsWriter::TStrPtrStrPtrPrDoublePrVec &influenceResults,
                                      rapidjson::Document &doc)
{
    if (influenceResults.empty())
    {
        return;
    }

    //! This function takes the raw c_str pointers of the string objects in
    //! influenceResults. These strings must exist up to the time the results
    //! are written

    typedef std::pair<const char *, double>                                 TCharPtrDoublePr;
    typedef std::vector<TCharPtrDoublePr>                                   TCharPtrDoublePrVec;
    typedef TCharPtrDoublePrVec::iterator                                   TCharPtrDoublePrVecIter;
    typedef std::pair<const char *, TCharPtrDoublePrVec>                    TCharPtrCharPtrDoublePrVecPr;
    typedef boost::unordered_map<std::string, TCharPtrCharPtrDoublePrVecPr> TStrCharPtrCharPtrDoublePrVecPrUMap;
    typedef TStrCharPtrCharPtrDoublePrVecPrUMap::iterator                   TStrCharPtrCharPtrDoublePrVecPrUMapIter;


    TStrCharPtrCharPtrDoublePrVecPrUMap influences;

    // group by influence field
    CHierarchicalResultsWriter::TStrPtrStrPtrPrDoublePrVec::const_iterator citer =
                        influenceResults.begin();
    for (; citer != influenceResults.end(); ++citer)
    {
        TCharPtrDoublePrVec values;
        TCharPtrCharPtrDoublePrVecPr infResult(citer->first.first->c_str(), values);
        TStrCharPtrCharPtrDoublePrVecPrUMap::value_type value(*citer->first.first, infResult);
        std::pair<TStrCharPtrCharPtrDoublePrVecPrUMapIter, bool> insert = influences.insert(value);

        insert.first->second.second.push_back(TCharPtrDoublePr(citer->first.second->c_str(), citer->second));
    }

    // Order by influence
    for (TStrCharPtrCharPtrDoublePrVecPrUMapIter iter = influences.begin(); iter != influences.end(); ++iter)
    {
        std::sort(iter->second.second.begin(), iter->second.second.end(), INFLUENCE_LESS);
    }

    rapidjson::Value influencesDoc(rapidjson::kArrayType);
    influencesDoc.Reserve(static_cast<rapidjson::SizeType>(influences.size()), m_JsonPoolAllocator);

    for (TStrCharPtrCharPtrDoublePrVecPrUMapIter iter = influences.begin(); iter != influences.end(); ++iter)
    {
        rapidjson::Value influenceDoc(rapidjson::kObjectType);

        rapidjson::Value values(rapidjson::kArrayType);
        values.Reserve(static_cast<rapidjson::SizeType>(iter->second.second.size()),
                                    m_JsonPoolAllocator);

        for (TCharPtrDoublePrVecIter arrayIter = iter->second.second.begin();
             arrayIter != iter->second.second.end();
             ++arrayIter)
        {
            rapidjson::Value value(arrayIter->first);
            values.PushBack(value, m_JsonPoolAllocator);
        }

        influenceDoc.AddMember(INFLUENCER_FIELD_NAME.c_str(), iter->second.first, m_JsonPoolAllocator);
        influenceDoc.AddMember(INFLUENCER_FIELD_VALUES.c_str(), values, m_JsonPoolAllocator);
        influencesDoc.PushBack(influenceDoc, m_JsonPoolAllocator);
    }

    // Note influences are written using the field name "influencers" to avoid
    // discussions with customers about the difference between influences and
    // influencers
    doc.AddMember(INFLUENCERS.c_str(), influencesDoc, m_JsonPoolAllocator);
}

void CJsonOutputWriter::addEventRateFields(const CHierarchicalResultsWriter::TResults &results,
                                           rapidjson::Document &doc)
{
    // normalizedProbability, probability, fieldName, byFieldName, byFieldValue, partitionFieldName,
    // partitionFieldValue, functionName, typical, actual, influences?

    core::CJsonDocUtils::addDoubleFieldToObj(NORMALIZED_PROBABILITY, results.s_NormalizedAnomalyScore, doc,
                                            m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleFieldToObj(PROBABILITY, results.s_Probability, doc,
                                            m_JsonPoolAllocator);
    core::CJsonDocUtils::addStringFieldToObj(FIELD_NAME, results.s_MetricValueField, doc,
                                            m_JsonPoolAllocator);
    if (!results.s_ByFieldName.empty())
    {
        core::CJsonDocUtils::addStringFieldToObj(BY_FIELD_NAME, results.s_ByFieldName, doc,
                                            m_JsonPoolAllocator);
        // If name is present then force output of value too, even when empty
        core::CJsonDocUtils::addStringFieldToObj(BY_FIELD_VALUE, results.s_ByFieldValue, doc,
                                            m_JsonPoolAllocator, true);
        // But allow correlatedByFieldValue to be unset if blank
        core::CJsonDocUtils::addStringFieldToObj(CORRELATED_BY_FIELD_VALUE, results.s_CorrelatedByFieldValue, doc,
                                            m_JsonPoolAllocator);
    }
    if (!results.s_PartitionFieldName.empty())
    {
        core::CJsonDocUtils::addStringFieldToObj(PARTITION_FIELD_NAME, results.s_PartitionFieldName,
                                            doc, m_JsonPoolAllocator);
        // If name is present then force output of value too, even when empty
        core::CJsonDocUtils::addStringFieldToObj(PARTITION_FIELD_VALUE, results.s_PartitionFieldValue,
                                            doc, m_JsonPoolAllocator, true);
    }
    core::CJsonDocUtils::addStringFieldToObj(FUNCTION, results.s_FunctionName, doc,
                                            m_JsonPoolAllocator);
    core::CJsonDocUtils::addStringFieldToObj(FUNCTION_DESCRIPTION,
                                            results.s_FunctionDescription, doc, m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleArrayFieldToObj(TYPICAL, results.s_BaselineMean, doc, m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleArrayFieldToObj(ACTUAL, results.s_CurrentMean, doc, m_JsonPoolAllocator);
}

void CJsonOutputWriter::addInfluencerFields(bool isBucketInfluencer,
                                            const model::CHierarchicalResults::TNode &node,
                                            rapidjson::Document &doc)
{
    core::CJsonDocUtils::addDoubleFieldToObj(PROBABILITY,
                                             node.probability(),
                                             doc, m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleFieldToObj(INITIAL_SCORE,
                                             node.s_NormalizedAnomalyScore,
                                             doc, m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleFieldToObj(ANOMALY_SCORE,
                                             node.s_NormalizedAnomalyScore,
                                             doc, m_JsonPoolAllocator);
    const std::string &personFieldName = *node.s_Spec.s_PersonFieldName;
    core::CJsonDocUtils::addStringFieldToObj(INFLUENCER_FIELD_NAME,
                                             personFieldName,
                                             doc, m_JsonPoolAllocator);
    if (isBucketInfluencer)
    {
        core::CJsonDocUtils::addDoubleFieldToObj(RAW_ANOMALY_SCORE,
                                                 node.s_RawAnomalyScore,
                                                 doc, m_JsonPoolAllocator);
    }
    else
    {
        if (!personFieldName.empty())
        {
            // If name is present then force output of value too, even when empty
            core::CJsonDocUtils::addStringFieldToObj(INFLUENCER_FIELD_VALUE,
                                             *node.s_Spec.s_PersonFieldValue,
                                             doc, m_JsonPoolAllocator, true);
        }
    }
}

void CJsonOutputWriter::addPartitionScores(const CHierarchicalResultsWriter::TResults &results,
                                                rapidjson::Document &doc)
{
    core::CJsonDocUtils::addDoubleFieldToObj(PROBABILITY,
                                             results.s_Probability,
                                             doc, m_JsonPoolAllocator);
    core::CJsonDocUtils::addStringFieldToObj(PARTITION_FIELD_NAME,
                                             results.s_PartitionFieldName,
                                             doc, m_JsonPoolAllocator);
    core::CJsonDocUtils::addStringFieldToObj(PARTITION_FIELD_VALUE,
                                             results.s_PartitionFieldValue,
                                             doc, m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleFieldToObj(INITIAL_SCORE,
                                            results.s_NormalizedAnomalyScore,
                                            doc, m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleFieldToObj(ANOMALY_SCORE,
                                            results.s_NormalizedAnomalyScore,
                                            doc, m_JsonPoolAllocator);
}

std::string CJsonOutputWriter::internalString(void) const
{
    const_cast<rapidjson::GenericWriteStream &>(m_WriteStream).Flush();

    // This is only of any value if the first constructor was used - it's up to
    // the caller to know this
    return m_StringOutputBuf.str();
}

void CJsonOutputWriter::limitNumberRecords(size_t count)
{
    m_RecordOutputLimit = count;
}

size_t CJsonOutputWriter::limitNumberRecords(void) const
{
    return m_RecordOutputLimit;
}

void CJsonOutputWriter::persistNormalizer(const model::CHierarchicalResultsNormalizer &normalizer,
                                          core_t::TTime &persistTime)
{
    std::string quantilesState;
    normalizer.toJson(m_LastNonInterimBucketTime, "api", quantilesState, true);

    // All output is in a JSON array, but only start it once
    if (!m_CloseJsonStructures)
    {
        m_Writer.StartArray();
        m_CloseJsonStructures = true;
    }

    this->writeModelSnapshotReports();

    m_Writer.StartObject();
    m_Writer.String(QUANTILES.c_str());
    // No need to copy the strings as the doc is written straight away
    writeQuantileState(quantilesState, m_LastNonInterimBucketTime);
    m_Writer.EndObject();

    persistTime = core::CTimeUtils::now();
    LOG_DEBUG("Wrote quantiles state at " << persistTime);
}

CJsonOutputWriter::TGenericLineWriter  &CJsonOutputWriter::hijackJsonWriter(void)
{
    // All output is in a JSON array, but only start it once
    if (!m_CloseJsonStructures)
    {
        m_Writer.StartArray();
        m_CloseJsonStructures = true;
    }

    this->writeModelSnapshotReports();

    return m_Writer;
}

void CJsonOutputWriter::reportMemoryUsage(const model::CResourceMonitor::SResults &results)
{
    // All output is in a JSON array, but only start it once
    if (!m_CloseJsonStructures)
    {
        m_Writer.StartArray();
        m_CloseJsonStructures = true;
    }

    this->writeModelSnapshotReports();

    m_Writer.StartObject();
    this->writeMemoryUsageObject(results);
    m_Writer.EndObject();

    LOG_TRACE("Wrote memory usage results");
}

void CJsonOutputWriter::writeMemoryUsageObject(const model::CResourceMonitor::SResults &results)
{
    m_Writer.String(MODEL_SIZE_STATS.c_str());
    m_Writer.StartObject();

    m_Writer.String(JOB_ID.c_str(), static_cast<rapidjson::SizeType>(JOB_ID.length()));
    m_Writer.String(m_JobId.c_str(), static_cast<rapidjson::SizeType>(m_JobId.length()));
    m_Writer.String(MODEL_BYTES.c_str(),
        static_cast<rapidjson::SizeType>(MODEL_BYTES.length()));
    m_Writer.Uint64(results.s_Usage);

    m_Writer.String(TOTAL_BY_FIELD_COUNT.c_str(),
        static_cast<rapidjson::SizeType>(TOTAL_BY_FIELD_COUNT.length()));
    m_Writer.Uint64(results.s_ByFields);

    m_Writer.String(TOTAL_OVER_FIELD_COUNT.c_str(),
        static_cast<rapidjson::SizeType>(TOTAL_OVER_FIELD_COUNT.length()));
    m_Writer.Uint64(results.s_OverFields);

    m_Writer.String(TOTAL_PARTITION_FIELD_COUNT.c_str(),
        static_cast<rapidjson::SizeType>(TOTAL_PARTITION_FIELD_COUNT.length()));
    m_Writer.Uint64(results.s_PartitionFields);

    m_Writer.String(BUCKET_ALLOCATION_FAILURES_COUNT.c_str(),
        static_cast<rapidjson::SizeType>(BUCKET_ALLOCATION_FAILURES_COUNT.length()));
    m_Writer.Uint64(results.s_AllocationFailures);

    m_Writer.String(MEMORY_STATUS.c_str(),
        static_cast<rapidjson::SizeType>(MEMORY_STATUS.length()));
    m_Writer.String(print(results.s_MemoryStatus).c_str());

    m_Writer.String(TIMESTAMP.c_str(),
        static_cast<rapidjson::SizeType>(TIMESTAMP.length()));
    m_Writer.Int64(results.s_BucketStartTime * 1000);

    m_Writer.String(LOG_TIME.c_str(),
        static_cast<rapidjson::SizeType>(LOG_TIME.length()));
    m_Writer.Int64(core::CTimeUtils::now() * 1000);

    m_Writer.EndObject();
}

void CJsonOutputWriter::reportPersistComplete(core_t::TTime snapshotTimestamp,
                                              const std::string &description,
                                              const std::string &snapshotId,
                                              size_t numDocs,
                                              const model::CResourceMonitor::SResults &modelSizeStats,
                                              const std::string &normalizerState,
                                              core_t::TTime latestRecordTime,
                                              core_t::TTime lastResultsTime)
{
    core::CScopedLock lock(m_ModelSnapshotReportsQueueMutex);

    m_ModelSnapshotReports.push(SModelSnapshotReport(snapshotTimestamp,
                                                     description,
                                                     snapshotId,
                                                     numDocs,
                                                     modelSizeStats,
                                                     normalizerState,
                                                     latestRecordTime,
                                                     lastResultsTime));
}

void CJsonOutputWriter::writeModelSnapshotReports(void)
{
    core::CScopedLock lock(m_ModelSnapshotReportsQueueMutex);

    while (!m_ModelSnapshotReports.empty())
    {
        const SModelSnapshotReport &report = m_ModelSnapshotReports.front();

        m_Writer.StartObject();
        m_Writer.String(MODEL_SNAPSHOT.c_str());
        m_Writer.StartObject();

        m_Writer.String(JOB_ID.c_str(), static_cast<rapidjson::SizeType>(JOB_ID.length()));
        m_Writer.String(m_JobId.c_str(), static_cast<rapidjson::SizeType>(m_JobId.length()));
        m_Writer.String(SNAPSHOT_ID.c_str(),
                        static_cast<rapidjson::SizeType>(SNAPSHOT_ID.length()));
        m_Writer.String(report.s_SnapshotId.c_str(),
                        static_cast<rapidjson::SizeType>(report.s_SnapshotId.length()));

        m_Writer.String(SNAPSHOT_DOC_COUNT.c_str(),
                        static_cast<rapidjson::SizeType>(SNAPSHOT_DOC_COUNT.length()));
        m_Writer.Uint64(report.s_NumDocs);

        // Write as a Java timestamp - ms since the epoch rather than seconds
        int64_t javaTimestamp = int64_t(report.s_SnapshotTimestamp) * 1000;

        m_Writer.String(TIMESTAMP.c_str(),
                        static_cast<rapidjson::SizeType>(TIMESTAMP.length()));
        m_Writer.Int64(javaTimestamp);

        // The restore priority starts off with the same value as the timestamp,
        // but may be updated later
        m_Writer.String(RESTORE_PRIORITY.c_str(),
                        static_cast<rapidjson::SizeType>(RESTORE_PRIORITY.length()));
        m_Writer.Int64(javaTimestamp);

        m_Writer.String(DESCRIPTION.c_str(),
                        static_cast<rapidjson::SizeType>(DESCRIPTION.length()));
        m_Writer.String(report.s_Description.c_str(),
                        static_cast<rapidjson::SizeType>(report.s_Description.length()));

        this->writeMemoryUsageObject(report.s_ModelSizeStats);

        if (report.s_LatestRecordTime > 0)
        {
            javaTimestamp = int64_t(report.s_LatestRecordTime) * 1000;

            m_Writer.String(LATEST_RECORD_TIME.c_str(),
                            static_cast<rapidjson::SizeType>(LATEST_RECORD_TIME.length()));
            m_Writer.Int64(javaTimestamp);
        }
        if (report.s_LatestResultsTime > 0)
        {
            javaTimestamp = int64_t(report.s_LatestResultsTime) * 1000;

            m_Writer.String(LATEST_RESULT_TIME.c_str(),
                            static_cast<rapidjson::SizeType>(LATEST_RESULT_TIME.length()));
            m_Writer.Int64(javaTimestamp);
        }

        // write normalizerState here
        m_Writer.String(QUANTILES.c_str(),
                        static_cast<rapidjson::SizeType>(QUANTILES.length()));

        writeQuantileState(report.s_NormalizerState, report.s_LatestResultsTime);

        m_Writer.EndObject();
        m_Writer.EndObject();

        LOG_DEBUG("Wrote model snapshot report with ID " << report.s_SnapshotId <<
                  " for: " << report.s_Description << ", latest results at " << report.s_LatestResultsTime);

        m_ModelSnapshotReports.pop();
    }
}


void CJsonOutputWriter::writeQuantileState(const std::string &state, core_t::TTime time)
{
    m_Writer.StartObject();
    m_Writer.String(JOB_ID.c_str(), static_cast<rapidjson::SizeType>(JOB_ID.length()));
    m_Writer.String(m_JobId.c_str(), static_cast<rapidjson::SizeType>(m_JobId.length()));
    m_Writer.String(QUANTILE_STATE.c_str(),
                    static_cast<rapidjson::SizeType>(QUANTILE_STATE.length()));
    m_Writer.String(state.c_str(),
                    static_cast<rapidjson::SizeType>(state.length()));
    m_Writer.String(TIMESTAMP.c_str(),
                static_cast<rapidjson::SizeType>(TIMESTAMP.length()));
    m_Writer.Int64(time * 1000);
    m_Writer.EndObject();
}

void CJsonOutputWriter::acknowledgeFlush(const std::string &flushId)
{
    // All output is in a JSON array, but only start it once
    if (!m_CloseJsonStructures)
    {
        m_Writer.StartArray();
        m_CloseJsonStructures = true;
    }

    this->writeModelSnapshotReports();


    m_Writer.StartObject();
    m_Writer.String(FLUSH.c_str());
    m_Writer.StartObject();

    m_Writer.String(ID.c_str(),
                    static_cast<rapidjson::SizeType>(ID.length()));
    m_Writer.String(flushId.c_str(),
                    static_cast<rapidjson::SizeType>(flushId.length()));

    m_Writer.EndObject();
    m_Writer.EndObject();

    m_WriteStream.Flush();

    LOG_TRACE("Wrote flush with ID " << flushId);
}

void CJsonOutputWriter::writeCategoryDefinition(int categoryId,
                                                const std::string &terms,
                                                const std::string &regex,
                                                std::size_t maxMatchingFieldLength,
                                                const TStrSet &examples)
{
    if (!m_CloseJsonStructures)
    {
        m_Writer.StartArray();
        m_CloseJsonStructures = true;
    }

    this->writeModelSnapshotReports();

    m_Writer.StartObject();
    m_Writer.String(CATEGORY_DEFINITION.c_str());
    m_Writer.StartObject();
    m_Writer.String(JOB_ID.c_str(), static_cast<rapidjson::SizeType>(JOB_ID.length()));
    m_Writer.String(m_JobId.c_str(), static_cast<rapidjson::SizeType>(m_JobId.length()));
    m_Writer.String(CATEGORY_ID.c_str(), static_cast<rapidjson::SizeType>(CATEGORY_ID.length()));
    m_Writer.Int(categoryId);
    m_Writer.String(TERMS.c_str(), static_cast<rapidjson::SizeType>(TERMS.length()));
    m_Writer.String(terms.c_str(), static_cast<rapidjson::SizeType>(terms.length()));
    m_Writer.String(REGEX.c_str(), static_cast<rapidjson::SizeType>(REGEX.length()));
    m_Writer.String(regex.c_str(), static_cast<rapidjson::SizeType>(regex.length()));
    m_Writer.String(MAX_MATCHING_LENGTH.c_str(), static_cast<rapidjson::SizeType>(MAX_MATCHING_LENGTH.length()));
    m_Writer.Uint64(maxMatchingFieldLength);
    m_Writer.String(EXAMPLES.c_str(), static_cast<rapidjson::SizeType>(EXAMPLES.length()));
    m_Writer.StartArray();
    for (TStrSetCItr itr = examples.begin(); itr != examples.end(); ++itr)
    {
        const std::string &example = *itr;
        m_Writer.String(example.c_str(), static_cast<rapidjson::SizeType>(example.length()));
    }
    m_Writer.EndArray();
    m_Writer.EndObject();
    m_Writer.EndObject();
}

CJsonOutputWriter::SBucketData::SBucketData(void)
    : s_MaxBucketInfluencerNormalizedAnomalyScore(0.0),
      s_InputEventCount(0),
      s_RecordCount(0),
      s_BucketSpan(0),
      s_HighestProbability(-1),
      s_LowestInfluencerScore(101.0),
      s_LowestBucketInfluencerScore(101.0)
{
}

CJsonOutputWriter::SModelSnapshotReport::SModelSnapshotReport(core_t::TTime snapshotTimestamp,
                                                              const std::string &description,
                                                              const std::string &snapshotId,
                                                              size_t numDocs,
                                                              const model::CResourceMonitor::SResults &modelSizeStats,
                                                              const std::string &normalizerState,
                                                              core_t::TTime latestRecordTime,
                                                              core_t::TTime latestResultsTime)
    : s_SnapshotTimestamp(snapshotTimestamp),
      s_Description(description),
      s_SnapshotId(snapshotId),
      s_NumDocs(numDocs),
      s_ModelSizeStats(modelSizeStats),
      s_NormalizerState(normalizerState),
      s_LatestRecordTime(latestRecordTime),
      s_LatestResultsTime(latestResultsTime)
{
}

}
}

