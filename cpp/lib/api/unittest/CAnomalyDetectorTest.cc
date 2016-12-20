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
#include "CAnomalyDetectorTest.h"

#include <core/CLogger.h>
#include <core/CRegex.h>

#include <model/CDataGatherer.h>
#include <model/CLimits.h>
#include <model/CModelConfig.h>

#include <api/CAnomalyDetector.h>
#include <api/CCsvInputParser.h>
#include <api/CFieldConfig.h>
#include <api/CHierarchicalResultsWriter.h>
#include <api/CJsonOutputWriter.h>

#include <boost/tuple/tuple.hpp>

#include <fstream>
#include <sstream>

namespace
{

//! \brief
//! Mock object for unit tests
//!
//! DESCRIPTION:\n
//! Mock object for gathering anomaly results.
//!
//! IMPLEMENTATION DECISIONS:\n
//! Only the minimal set of required functions are implemented.
//!
class CMockOutputWriter : public prelert::api::CJsonOutputWriter
{
    public:
        typedef std::vector<double> TResultsVec;
        typedef TResultsVec::const_iterator TResultsVecCItr;

    public:
        //! Constructor
        CMockOutputWriter(void) :
            prelert::api::CJsonOutputWriter("job", m_Writer)
        { }

        //! Destructor
        virtual ~CMockOutputWriter(void)
        {
            // Finalise the Json writer so it doesn't try and write to
            // m_Writer which will have been destroyed first
            prelert::api::CJsonOutputWriter::finalise();
        }

        //! Override for handling the anomaly results
        virtual bool acceptResult(const prelert::api::CHierarchicalResultsWriter::SResults &results)
        {
            if ((results.s_ByFieldName != "count") && (results.s_ResultType == prelert::api::CHierarchicalResultsWriter::E_Result))
            {
                m_Results.push_back(results.s_CurrentMean[0]);
            }
            return true;
        }

        //! Accessor for the collection of results
        const TResultsVec &results(void) const
        {
            return m_Results;
        }

        std::string printInternal(void)
        {
            return m_Writer.str();
        }

    private:
        //! Dummy stream buffer to keep the CJsonOutputWriter happy
        std::ostringstream m_Writer;

        //! Collection of results received
        TResultsVec m_Results;
};

class CSingleResultVisitor : public prelert::model::CHierarchicalResultsVisitor
{
    public:
        CSingleResultVisitor(void) : m_LastResult(0.0)
        { }

        virtual ~CSingleResultVisitor(void)
        { }

        virtual void visit(const prelert::model::CHierarchicalResults &/*results*/,
                           const TNode &node,
                           bool /*pivot*/)
        {
            if (!this->isSimpleCount(node) && this->isLeaf(node))
            {
                if (node.s_AnnotatedProbability.s_AttributeProbabilities.size() == 0)
                {
                    return;
                }
                if (!node.s_Model)
                {
                    return;
                }
                const prelert::model::SAttributeProbability &attribute =
                    node.s_AnnotatedProbability.s_AttributeProbabilities[0];

                m_LastResult = node.s_Model->currentBucketValue(attribute.s_Feature,
                                                                0, 0, node.s_BucketStartTime)[0];
            }
        }

        double lastResults(void) const
        {
            return m_LastResult;
        }

    private:
        double m_LastResult;
};

class CMultiResultVisitor : public prelert::model::CHierarchicalResultsVisitor
{
    public:
        CMultiResultVisitor(void) : m_LastResult(0.0)
        { }

        virtual ~CMultiResultVisitor(void)
        { }

        virtual void visit(const prelert::model::CHierarchicalResults &/*results*/,
                           const TNode &node,
                           bool /*pivot*/)
        {
            if (!this->isSimpleCount(node) && this->isLeaf(node))
            {
                if (node.s_AnnotatedProbability.s_AttributeProbabilities.size() == 0)
                {
                    return;
                }
                if (!node.s_Model)
                {
                    return;
                }
                std::size_t pid;
                const prelert::model::CDataGatherer &gatherer = node.s_Model->dataGatherer();
                if (!gatherer.personId(*node.s_Spec.s_PersonFieldValue, pid))
                {
                    LOG_ERROR("No identifier for '"
                        << *node.s_Spec.s_PersonFieldValue << "'");
                    return;
                }
                for (std::size_t i = 0; i < node.s_AnnotatedProbability.s_AttributeProbabilities.size(); ++i)
                {
                    const prelert::model::SAttributeProbability &attribute =
                        node.s_AnnotatedProbability.s_AttributeProbabilities[i];
                    m_LastResult += node.s_Model->currentBucketValue(attribute.s_Feature,
                                            pid, attribute.s_Cid, node.s_BucketStartTime)[0];
                }
            }
        }

        double lastResults(void) const
        {
            return m_LastResult;
        }

    private:
        double m_LastResult;
};

class CResultsScoreVisitor : public prelert::model::CHierarchicalResultsVisitor
{
    public:
        CResultsScoreVisitor(int score) : m_Score(score)
        { }

        virtual ~CResultsScoreVisitor(void)
        { }

        virtual void visit(const prelert::model::CHierarchicalResults &/*results*/,
                           const TNode &node,
                           bool /*pivot*/)
        {
            if (this->isRoot(node))
            {
                node.s_NormalizedAnomalyScore = m_Score;
            }
        }

    private:
        int m_Score;
};

bool findLine(const std::string &regex, const prelert::core::CRegex::TStrVec &lines)
{
    prelert::core::CRegex rx;
    rx.init(regex);
    std::size_t pos = 0;
    for (prelert::core::CRegex::TStrVecCItr i = lines.begin(); i != lines.end(); ++i)
    {
        if (rx.search(*i, pos))
        {
            return true;
        }
    }
    return false;
}


const prelert::core_t::TTime BUCKET_SIZE(3600);

}

using namespace prelert;

void CAnomalyDetectorTest::testBadTimes(void)
{
    {
        // Test with no time field
        model::CLimits limits;
        api::CFieldConfig fieldConfig;
        api::CFieldConfig::TStrVec clauses;
        clauses.push_back("value");
        clauses.push_back("partitionfield=greenhouse");
        fieldConfig.initFromClause(clauses);
        model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(BUCKET_SIZE);
        std::stringstream outputStrm;
        api::CJsonOutputWriter outputWriter("job", outputStrm);

        api::CAnomalyDetector detector("job", limits, fieldConfig, modelConfig,
                outputWriter);

        api::CAnomalyDetector::TStrVec fieldNames;
        api::CAnomalyDetector::TStrStrUMap dataRows;
        fieldNames.push_back("_time");
        fieldNames.push_back("value");
        fieldNames.push_back("greenhouse");

        dataRows["wibble"] = "12345678";
        dataRows["value"] = "1.0";
        dataRows["greenhouse"] = "spongebob";

        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(uint64_t(0), detector.numRecordsHandled());
    }
    {
        // Test with bad time field
        model::CLimits limits;
        api::CFieldConfig fieldConfig;
        api::CFieldConfig::TStrVec clauses;
        clauses.push_back("value");
        clauses.push_back("partitionfield=greenhouse");
        fieldConfig.initFromClause(clauses);
        model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(BUCKET_SIZE);
        std::stringstream outputStrm;
        api::CJsonOutputWriter outputWriter("job", outputStrm);

        api::CAnomalyDetector detector("job", limits, fieldConfig, modelConfig,
                outputWriter,
                api::CAnomalyDetector::TPersistCompleteFunc(), 0, -1, -1, "_time", "");

        api::CAnomalyDetector::TStrVec fieldNames;
        api::CAnomalyDetector::TStrStrUMap dataRows;
        fieldNames.push_back("_time");
        fieldNames.push_back("value");
        fieldNames.push_back("greenhouse");

        dataRows["_time"] = "hello";
        dataRows["value"] = "1.0";
        dataRows["greenhouse"] = "spongebob";

        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(uint64_t(0), detector.numRecordsHandled());
    }
    {
        // Test with bad time field format
        model::CLimits limits;
        api::CFieldConfig fieldConfig;
        api::CFieldConfig::TStrVec clauses;
        clauses.push_back("value");
        clauses.push_back("partitionfield=greenhouse");
        fieldConfig.initFromClause(clauses);
        model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(BUCKET_SIZE);
        std::stringstream outputStrm;
        api::CJsonOutputWriter outputWriter("job", outputStrm);

        api::CAnomalyDetector detector("job", limits, fieldConfig, modelConfig,
                outputWriter,
                api::CAnomalyDetector::TPersistCompleteFunc(), 0, -1, -1, "_time", "%Y%m%m%H%M%S");

        api::CAnomalyDetector::TStrVec fieldNames;
        api::CAnomalyDetector::TStrStrUMap dataRows;
        fieldNames.push_back("_time");
        fieldNames.push_back("value");
        fieldNames.push_back("greenhouse");

        dataRows["_time"] = "hello world";
        dataRows["value"] = "1.0";
        dataRows["greenhouse"] = "spongebob";

        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(uint64_t(0), detector.numRecordsHandled());
    }
}

void CAnomalyDetectorTest::testOutOfSequence(void)
{
    {
        // Test out of sequence record
        model::CLimits limits;
        api::CFieldConfig fieldConfig;
        api::CFieldConfig::TStrVec clauses;
        clauses.push_back("value");
        clauses.push_back("partitionfield=greenhouse");
        fieldConfig.initFromClause(clauses);
        model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(BUCKET_SIZE);
        std::stringstream outputStrm;
        api::CJsonOutputWriter outputWriter("job", outputStrm);

        api::CAnomalyDetector detector("job", limits, fieldConfig, modelConfig,
                outputWriter);

        detector.description();
        detector.descriptionAndDebugMemoryUsage();

        // add records which create partitions
        api::CAnomalyDetector::TStrVec fieldNames;
        api::CAnomalyDetector::TStrStrUMap dataRows;
        fieldNames.push_back("_time");
        fieldNames.push_back("value");
        fieldNames.push_back("greenhouse");

        dataRows["_time"] = "12345678";
        dataRows["value"] = "1.0";
        dataRows["greenhouse"] = "spongebob";

        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(uint64_t(1), detector.numRecordsHandled());

        dataRows["_time"] = "1234567";

        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(uint64_t(1), detector.numRecordsHandled());
        detector.finalise();
    }
}

void CAnomalyDetectorTest::testControlMessages(void)
{
    {
        // Test control messages
        model::CLimits limits;
        api::CFieldConfig fieldConfig;
        api::CFieldConfig::TStrVec clauses;
        clauses.push_back("value");
        clauses.push_back("partitionfield=greenhouse");
        fieldConfig.initFromClause(clauses);
        model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(BUCKET_SIZE);
        std::stringstream outputStrm;
        api::CJsonOutputWriter outputWriter("job", outputStrm);

        api::CAnomalyDetector detector("job", limits, fieldConfig, modelConfig,
                outputWriter);

        api::CAnomalyDetector::TStrVec fieldNames;
        api::CAnomalyDetector::TStrStrUMap dataRows;
        fieldNames.push_back("_time");
        fieldNames.push_back("value");
        fieldNames.push_back("greenhouse");

        dataRows["."] = " ";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(uint64_t(0), detector.numRecordsHandled());

        dataRows["."] = ".";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(uint64_t(0), detector.numRecordsHandled());

        dataRows["."] = "f";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(uint64_t(0), detector.numRecordsHandled());

        dataRows["."] = "f1";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(uint64_t(0), detector.numRecordsHandled());
    }
    {
        // Test reset bucket
        model::CLimits limits;
        api::CFieldConfig fieldConfig;
        api::CFieldConfig::TStrVec clauses;
        clauses.push_back("count");
        clauses.push_back("partitionfield=greenhouse");
        fieldConfig.initFromClause(clauses);
        model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(BUCKET_SIZE);
        CMockOutputWriter outputWriter;

        api::CAnomalyDetector detector("job", limits, fieldConfig, modelConfig,
                outputWriter);

        api::CAnomalyDetector::TStrVec fieldNames;
        api::CAnomalyDetector::TStrStrUMap dataRows;
        fieldNames.push_back("_time");
        fieldNames.push_back("value");
        fieldNames.push_back("greenhouse");

        dataRows["value"] = "2.0";
        dataRows["greenhouse"] = "spongebob";

        core_t::TTime time = 12345678;
        for (std::size_t i = 0; i < 50; i++, time += (BUCKET_SIZE / 2))
        {
            std::stringstream ss;
            ss << time;
            dataRows["_time"] = ss.str();
            if (i == 40)
            {
                for (std::size_t j = 0; j < 100; j++)
                {
                    CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
                }
            }
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        }
        // expect the OutputWriter to have result 102
        CPPUNIT_ASSERT_EQUAL(std::size_t(1), outputWriter.results().size());
        CPPUNIT_ASSERT_EQUAL(102.0, outputWriter.results().back());

        for (std::size_t i = 0; i < 50; i++, time += (BUCKET_SIZE / 2))
        {
            std::stringstream ss;
            ss << time;
            dataRows["_time"] = ss.str();
            if (i == 40)
            {
                for (std::size_t j = 0; j < 100; j++)
                {
                    CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
                }
            }
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            if (i == 40)
            {
                api::CAnomalyDetector::TStrStrUMap rows;
                rows["."] = "r" + ss.str() + " " + ss.str();
                CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, rows));
                for (std::size_t j = 0; j < 100; j++)
                {
                    CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
                }
            }
        }
        // expect the result 101
        CPPUNIT_ASSERT_EQUAL(std::size_t(2), outputWriter.results().size());
        CPPUNIT_ASSERT_EQUAL(101.0, outputWriter.results().back());
    }
}

void CAnomalyDetectorTest::testOutOfPhase(void)
{
    // Ensure the right data ends up in the right buckets
    // First we test that it works as expected for non-out-of-phase,
    // then we crank in the out-of-phase

    // Ensure that gaps in a bucket's data do not cause errors or problems

    // Ensure that we can start at a variety of times,
    // and finish at a variety of times, and get the
    // right output always

    // The code is pretty verbose here, but executes quickly
    {
        LOG_DEBUG("*** testing non-out-of-phase metric ***");
        core_t::TTime bucketSize = 100;
        model::CLimits limits;
        api::CFieldConfig fieldConfig;
        api::CFieldConfig::TStrVec clauses;
        clauses.push_back("mean(value)");
        fieldConfig.initFromClause(clauses);
        model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(bucketSize);
        std::stringstream outputStrm;

        api::CJsonOutputWriter outputWriter("job", outputStrm);

        api::CAnomalyDetector detector("job", limits, fieldConfig, modelConfig,
                outputWriter);

        api::CAnomalyDetector::TStrVec fieldNames;
        api::CAnomalyDetector::TStrStrUMap dataRows;
        fieldNames.push_back("_time");
        fieldNames.push_back("value");

        CPPUNIT_ASSERT_EQUAL(core_t::TTime(99), detector.m_ResultsQueue.latestBucketEnd());
        dataRows["_time"] = "10000";
        dataRows["value"] = "1.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(99), detector.m_ResultsQueue.latestBucketEnd());

        dataRows["_time"] = "10050";
        dataRows["value"] = "3.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(99), detector.m_ResultsQueue.latestBucketEnd());

        dataRows["_time"] = "10100";
        dataRows["value"] = "1.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(10099), detector.m_ResultsQueue.latestBucketEnd());
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(2.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10200";
        dataRows["value"] = "0.0005";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(1.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10300";
        dataRows["value"] = "5.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(0.0005, visitor.lastResults(), 0.000005);
        }

        dataRows["_time"] = "10400";
        dataRows["value"] = "5.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(5.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10500";
        dataRows["value"] = "5.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(5.0, visitor.lastResults(), 0.0005);
        }

        // Bucket at 10600 not present

        dataRows["_time"] = "10700";
        dataRows["value"] = "50";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(0.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10700";
        dataRows["value"] = "80";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));

        dataRows["_time"] = "10700";
        dataRows["value"] = "20";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));

        dataRows["_time"] = "10800";
        dataRows["value"] = "5.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(50.0, visitor.lastResults(), 0.005);
        }

        dataRows["_time"] = "10800";
        dataRows["value"] = "6.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(10799), detector.m_ResultsQueue.latestBucketEnd());
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(50.0, visitor.lastResults(), 0.005);
        }
        detector.finalise();
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(10799), detector.m_ResultsQueue.latestBucketEnd());
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(50.0, visitor.lastResults(), 0.005);
        }
    }
    {
        LOG_DEBUG("*** testing non-out-of-phase metric ***");
        // Same as previous test but starting not on a bucket boundary
        core_t::TTime bucketSize = 100;
        model::CLimits limits;
        api::CFieldConfig fieldConfig;
        api::CFieldConfig::TStrVec clauses;
        clauses.push_back("mean(value)");
        fieldConfig.initFromClause(clauses);
        model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(bucketSize);
        std::stringstream outputStrm;

        api::CJsonOutputWriter outputWriter("job", outputStrm);

        api::CAnomalyDetector detector("job", limits, fieldConfig, modelConfig,
                outputWriter);

        api::CAnomalyDetector::TStrVec fieldNames;
        api::CAnomalyDetector::TStrStrUMap dataRows;
        fieldNames.push_back("_time");
        fieldNames.push_back("value");

        // The first two values are in an incomplete bucket and should be ignored
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(99), detector.m_ResultsQueue.latestBucketEnd());
        dataRows["_time"] = "10001";
        dataRows["value"] = "1.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(99), detector.m_ResultsQueue.latestBucketEnd());

        dataRows["_time"] = "10051";
        dataRows["value"] = "3.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT(detector.m_ResultsQueue.latest().empty());

        // This next bucket should be the first valid one
        dataRows["_time"] = "10101";
        dataRows["value"] = "1.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT(detector.m_ResultsQueue.latest().empty());

        dataRows["_time"] = "10201";
        dataRows["value"] = "0.0005";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(10199), detector.m_ResultsQueue.latestBucketEnd());
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(1.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10301";
        dataRows["value"] = "5.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(0.0005, visitor.lastResults(), 0.000005);
        }

        dataRows["_time"] = "10401";
        dataRows["value"] = "5.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(5.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10501";
        dataRows["value"] = "5.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(5.0, visitor.lastResults(), 0.0005);
        }

        // Bucket at 10600 not present

        dataRows["_time"] = "10701";
        dataRows["value"] = "50";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(0.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10701";
        dataRows["value"] = "80";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));

        dataRows["_time"] = "10701";
        dataRows["value"] = "20";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));

        dataRows["_time"] = "10801";
        dataRows["value"] = "5.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(50.0, visitor.lastResults(), 0.005);
        }

        dataRows["_time"] = "10895";
        dataRows["value"] = "6.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));

        detector.finalise();
    }
    {
        LOG_DEBUG("*** testing non-out-of-phase count ***");
        core_t::TTime bucketSize = 100;
        model::CLimits limits;
        api::CFieldConfig fieldConfig;
        api::CFieldConfig::TStrVec clauses;
        clauses.push_back("count");
        fieldConfig.initFromClause(clauses);
        model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(bucketSize);
        std::stringstream outputStrm;

        api::CJsonOutputWriter outputWriter("job", outputStrm);

        api::CAnomalyDetector detector("job", limits, fieldConfig, modelConfig,
                outputWriter);

        api::CAnomalyDetector::TStrVec fieldNames;
        api::CAnomalyDetector::TStrStrUMap dataRows;
        fieldNames.push_back("_time");

        CPPUNIT_ASSERT_EQUAL(core_t::TTime(99), detector.m_ResultsQueue.latestBucketEnd());
        dataRows["_time"] = "10000";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(99), detector.m_ResultsQueue.latestBucketEnd());

        dataRows["_time"] = "10050";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(99), detector.m_ResultsQueue.latestBucketEnd());

        dataRows["_time"] = "10100";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10110";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10120";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(10099), detector.m_ResultsQueue.latestBucketEnd());
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(2.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10200";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(3.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10300";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10300";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(1.0, visitor.lastResults(), 0.000005);
        }

        dataRows["_time"] = "10400";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10401";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10402";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10403";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(2.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10500";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(4.0, visitor.lastResults(), 0.0005);
        }

        // Bucket at 10600 not present

        dataRows["_time"] = "10700";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(0.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10700";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));

        dataRows["_time"] = "10700";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));

        dataRows["_time"] = "10800";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(3.0, visitor.lastResults(), 0.005);
        }

        dataRows["_time"] = "10895";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(10799), detector.m_ResultsQueue.latestBucketEnd());
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(3.0, visitor.lastResults(), 0.005);
        }
        detector.finalise();
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(10799), detector.m_ResultsQueue.latestBucketEnd());
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(3.0, visitor.lastResults(), 0.005);
        }
    }
    {
        LOG_DEBUG("*** testing non-out-of-phase count ***");
        core_t::TTime bucketSize = 100;
        model::CLimits limits;
        api::CFieldConfig fieldConfig;
        api::CFieldConfig::TStrVec clauses;
        clauses.push_back("count");
        fieldConfig.initFromClause(clauses);
        model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(bucketSize);
        std::stringstream outputStrm;

        api::CJsonOutputWriter outputWriter("job", outputStrm);

        api::CAnomalyDetector detector("job", limits, fieldConfig, modelConfig,
                outputWriter);

        api::CAnomalyDetector::TStrVec fieldNames;
        api::CAnomalyDetector::TStrStrUMap dataRows;
        fieldNames.push_back("_time");

        CPPUNIT_ASSERT_EQUAL(core_t::TTime(99), detector.m_ResultsQueue.latestBucketEnd());
        dataRows["_time"] = "10088";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(99), detector.m_ResultsQueue.latestBucketEnd());

        dataRows["_time"] = "10097";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(99), detector.m_ResultsQueue.latestBucketEnd());

        dataRows["_time"] = "10100";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10110";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10120";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT(detector.m_ResultsQueue.latest().empty());

        dataRows["_time"] = "10200";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(3.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10300";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10300";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(1.0, visitor.lastResults(), 0.000005);
        }

        dataRows["_time"] = "10400";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10401";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10402";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10403";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(2.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10500";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(4.0, visitor.lastResults(), 0.0005);
        }

        // Bucket at 10600 not present

        dataRows["_time"] = "10700";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(0.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10700";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));

        dataRows["_time"] = "10700";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));

        dataRows["_time"] = "10800";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(3.0, visitor.lastResults(), 0.005);
        }

        dataRows["_time"] = "10805";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(10799), detector.m_ResultsQueue.latestBucketEnd());
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(3.0, visitor.lastResults(), 0.005);
        }
        detector.finalise();
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(10799), detector.m_ResultsQueue.latestBucketEnd());
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(3.0, visitor.lastResults(), 0.005);
        }
    }
    // Now we come to the real meat and potatoes of the test, the out-of-phase buckets
    {
        LOG_DEBUG("*** testing out-of-phase metric ***");
        core_t::TTime bucketSize = 100;
        model::CLimits limits;
        api::CFieldConfig fieldConfig;
        api::CFieldConfig::TStrVec clauses;
        clauses.push_back("mean(value)");
        fieldConfig.initFromClause(clauses);

        // 2 delay buckets
        model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(bucketSize,
                bucketSize, 1u, model_t::E_None, "", 0, 2, false, "");
        std::stringstream outputStrm;

        api::CJsonOutputWriter outputWriter("job", outputStrm);

        api::CAnomalyDetector detector("job", limits, fieldConfig, modelConfig,
                outputWriter);

        api::CAnomalyDetector::TStrVec fieldNames;
        api::CAnomalyDetector::TStrStrUMap dataRows;
        fieldNames.push_back("_time");
        fieldNames.push_back("value");

        // main bucket should start at 10000 -> 10100
        // out-of-phase bucket start at 10050 -> 10150
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(49), detector.m_ResultsQueue.latestBucketEnd());
        dataRows["_time"] = "10000";
        dataRows["value"] = "5.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(49), detector.m_ResultsQueue.latestBucketEnd());

        dataRows["_time"] = "10050";
        dataRows["value"] = "3.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));

        dataRows["_time"] = "10100";
        dataRows["value"] = "1.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(4.0, visitor.lastResults(), 0.0005);
        }
        dataRows["_time"] = "10150";
        dataRows["value"] = "4.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(10099), detector.m_ResultsQueue.latestBucketEnd());
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(2.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10200";
        dataRows["value"] = "0.0005";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(10149), detector.m_ResultsQueue.latestBucketEnd());
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(2.5, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10300";
        dataRows["value"] = "5.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(0.0005, visitor.lastResults(), 0.000005);
        }

        dataRows["_time"] = "10499";
        dataRows["value"] = "5.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        LOG_DEBUG("Result time is " << ( detector.m_ResultsQueue.latestBucketEnd() - 49 ));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(0.0, visitor.lastResults(), 0.0000005);
        }

        dataRows["_time"] = "10500";
        dataRows["value"] = "5.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(5.0, visitor.lastResults(), 0.0005);
        }

        // Bucket at 10600 not present

        dataRows["_time"] = "10700";
        dataRows["value"] = "50";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(0.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10720";
        dataRows["value"] = "80";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));

        dataRows["_time"] = "10760";
        dataRows["value"] = "20";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(65.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10780";
        dataRows["value"] = "80";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));

        dataRows["_time"] = "10800";
        dataRows["value"] = "5.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(57.5, visitor.lastResults(), 0.005);
        }

        // 10895, triggers bucket  10750->10850
        dataRows["_time"] = "10895";
        dataRows["value"] = "6.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        LOG_DEBUG("Result time is " << ( detector.m_ResultsQueue.latestBucketEnd() ));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(10799), detector.m_ResultsQueue.latestBucketEnd());
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(35.0, visitor.lastResults(), 0.005);
        }
        LOG_DEBUG("Finalising detector");
        detector.finalise();
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(10799), detector.m_ResultsQueue.latestBucketEnd());
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(35.0, visitor.lastResults(), 0.005);
        }
    }
    {
        LOG_DEBUG("*** testing out-of-phase metric ***");
        core_t::TTime bucketSize = 100;
        model::CLimits limits;
        api::CFieldConfig fieldConfig;
        api::CFieldConfig::TStrVec clauses;
        clauses.push_back("mean(value)");
        fieldConfig.initFromClause(clauses);

        // 2 delay buckets
        model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(bucketSize,
                bucketSize, 1u, model_t::E_None, "", 0, 2, false, "");
        std::stringstream outputStrm;

        api::CJsonOutputWriter outputWriter("job", outputStrm);

        api::CAnomalyDetector detector("job", limits, fieldConfig, modelConfig,
                outputWriter);

        api::CAnomalyDetector::TStrVec fieldNames;
        api::CAnomalyDetector::TStrStrUMap dataRows;
        fieldNames.push_back("_time");
        fieldNames.push_back("value");

        CPPUNIT_ASSERT_EQUAL(core_t::TTime(49), detector.m_ResultsQueue.latestBucketEnd());
        dataRows["_time"] = "10045";
        dataRows["value"] = "5.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(49), detector.m_ResultsQueue.latestBucketEnd());

        dataRows["_time"] = "10050";
        dataRows["value"] = "3.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));

        // This is the first complete bucket
        dataRows["_time"] = "10100";
        dataRows["value"] = "1.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(0.0, visitor.lastResults(), 0.0005);
        }
        dataRows["_time"] = "10150";
        dataRows["value"] = "4.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(10099), detector.m_ResultsQueue.latestBucketEnd());
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(0.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10200";
        dataRows["value"] = "0.0005";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(10149), detector.m_ResultsQueue.latestBucketEnd());
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(2.5, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10300";
        dataRows["value"] = "5.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(0.0005, visitor.lastResults(), 0.000005);
        }

        dataRows["_time"] = "10499";
        dataRows["value"] = "5.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        LOG_DEBUG("Result time is " << ( detector.m_ResultsQueue.latestBucketEnd() - 49 ));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(0.0, visitor.lastResults(), 0.0000005);
        }

        dataRows["_time"] = "10500";
        dataRows["value"] = "5.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(5.0, visitor.lastResults(), 0.0005);
        }

        // Bucket at 10600 not present

        dataRows["_time"] = "10700";
        dataRows["value"] = "50";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(0.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10720";
        dataRows["value"] = "80";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));

        dataRows["_time"] = "10760";
        dataRows["value"] = "20";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(65.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10780";
        dataRows["value"] = "80";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));

        dataRows["_time"] = "10800";
        dataRows["value"] = "5.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(57.5, visitor.lastResults(), 0.005);
        }

        // 10895, triggers bucket  10750->10850
        dataRows["_time"] = "10895";
        dataRows["value"] = "6.0";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        LOG_DEBUG("Result time is " << ( detector.m_ResultsQueue.latestBucketEnd() ));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(10799), detector.m_ResultsQueue.latestBucketEnd());
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(35.0, visitor.lastResults(), 0.005);
        }
        LOG_DEBUG("Finalising detector");
        detector.finalise();
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(10799), detector.m_ResultsQueue.latestBucketEnd());
        {
            CSingleResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(35.0, visitor.lastResults(), 0.005);
        }
    }
    {
        LOG_DEBUG("*** testing out-of-phase eventrate ***");
        core_t::TTime bucketSize = 100;
        model::CLimits limits;
        api::CFieldConfig fieldConfig;
        api::CFieldConfig::TStrVec clauses;
        clauses.push_back("high_count");
        clauses.push_back("by");
        clauses.push_back("person");
        fieldConfig.initFromClause(clauses);

        // 2 delay buckets
        model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(bucketSize,
                bucketSize, 1u, model_t::E_None, "", 0, 2, false, "");
        std::stringstream outputStrm;

        api::CJsonOutputWriter outputWriter("job", outputStrm);

        api::CAnomalyDetector detector("job", limits, fieldConfig, modelConfig,
                outputWriter);

        api::CAnomalyDetector::TStrVec fieldNames;
        api::CAnomalyDetector::TStrStrUMap dataRows;
        fieldNames.push_back("_time");
        fieldNames.push_back("person");

        // main bucket should start at 10000 -> 10100
        // out-of-phase bucket start at 10050 -> 10150
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(49), detector.m_ResultsQueue.latestBucketEnd());
        dataRows["_time"] = "10000";
        dataRows["person"] = "Candice";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10001";
        dataRows["person"] = "Behati";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10002";
        dataRows["person"] = "Cara";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10003";
        dataRows["person"] = "Kate";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10004";
        dataRows["person"] = "Gisele";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(49), detector.m_ResultsQueue.latestBucketEnd());

        dataRows["_time"] = "10050";
        dataRows["person"] = "Behati";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));

        dataRows["_time"] = "10070";
        dataRows["person"] = "Candice";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));

        dataRows["_time"] = "10101";
        dataRows["person"] = "Cara";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CMultiResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(7.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10110";
        dataRows["person"] = "Kate";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));

        dataRows["_time"] = "10150";
        dataRows["person"] = "Gisele";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(10099), detector.m_ResultsQueue.latestBucketEnd());
        {
            CMultiResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(4.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10201";
        dataRows["person"] = "Behati";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10201";
        dataRows["person"] = "Candice";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10201";
        dataRows["person"] = "Gisele";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(10149), detector.m_ResultsQueue.latestBucketEnd());
        {
            CMultiResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(3.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10300";
        dataRows["person"] = "Cara";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10300";
        dataRows["person"] = "Kate";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10300";
        dataRows["person"] = "Gisele the imposter";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10301";
        dataRows["person"] = "Cara";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CMultiResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(3.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10490";
        dataRows["person"] = "Gisele";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10492";
        dataRows["person"] = "Kate";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10494";
        dataRows["person"] = "Behati";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        dataRows["_time"] = "10499";
        dataRows["person"] = "Cara";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        LOG_DEBUG("Result time is " << ( detector.m_ResultsQueue.latestBucketEnd() - 49 ));
        {
            CMultiResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(0.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10500";
        dataRows["person"] = "Cara";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CMultiResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(4.0, visitor.lastResults(), 0.0005);
        }

        // Bucket at 10600 not present

        dataRows["_time"] = "10700";
        dataRows["person"] = "Behati";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CMultiResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(0.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10720";
        dataRows["person"] = "Kate";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));

        dataRows["_time"] = "10760";
        dataRows["person"] = "Behati";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CMultiResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(2.0, visitor.lastResults(), 0.0005);
        }

        dataRows["_time"] = "10780";
        dataRows["person"] = "Cara";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));

        dataRows["_time"] = "10800";
        dataRows["person"] = "Candice";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        {
            CMultiResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(4.0, visitor.lastResults(), 0.005);
        }

        // 10895, triggers bucket  10750->10850
        dataRows["_time"] = "10895";
        dataRows["person"] = "Cara";
        CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
        LOG_DEBUG("Result time is " << ( detector.m_ResultsQueue.latestBucketEnd() ));
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(10799), detector.m_ResultsQueue.latestBucketEnd());
        {
            CMultiResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(3.0, visitor.lastResults(), 0.005);
        }
        LOG_DEBUG("Finalising detector");
        detector.finalise();
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(10799), detector.m_ResultsQueue.latestBucketEnd());
        {
            CMultiResultVisitor visitor;
            detector.m_ResultsQueue.latest().topDownBreadthFirst(visitor);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(3.0, visitor.lastResults(), 0.005);
        }
    }
}

void CAnomalyDetectorTest::testBucketSelection(void)
{
    LOG_DEBUG("*** testBucketSelection ***");
    core_t::TTime bucketSize = 100;
    model::CLimits limits;
    api::CFieldConfig fieldConfig;
    api::CFieldConfig::TStrVec clauses;
    clauses.push_back("mean(value)");
    fieldConfig.initFromClause(clauses);

    // 2 delay buckets
    model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(bucketSize,
            bucketSize, 1u, model_t::E_None, "", 0, 2, false, "");
    std::stringstream outputStrm;

    api::CJsonOutputWriter outputWriter("job", outputStrm);

    api::CAnomalyDetector detector("job", limits, fieldConfig, modelConfig,
            outputWriter);

    detector.m_ResultsQueue.reset(950);
    {
        model::SAnnotatedProbability prob(1.0);

        model::CHierarchicalResults results;
        results.addModelResult(0, false, "mean", model::function_t::E_IndividualMetricMean,
                               "", "", "", "", "value", prob, 0, 1000);
        CResultsScoreVisitor visitor(10);
        results.topDownBreadthFirst(visitor);
        detector.m_ResultsQueue.push(results, 1000);
        LOG_DEBUG("Adding 10 at 1000");
    }
    {
        model::SAnnotatedProbability prob(1.0);

        model::CHierarchicalResults results;
        results.addModelResult(0, false, "mean", model::function_t::E_IndividualMetricMean,
                               "", "", "", "", "value", prob, 0, 1000);
        CResultsScoreVisitor visitor(20);
        results.topDownBreadthFirst(visitor);
        detector.m_ResultsQueue.push(results, 1050);
        LOG_DEBUG("Adding 20 at 1050");
    }
    {
        model::SAnnotatedProbability prob(1.0);

        model::CHierarchicalResults results;
        results.addModelResult(0, false, "mean", model::function_t::E_IndividualMetricMean,
                               "", "", "", "", "value", prob, 0, 1000);
        CResultsScoreVisitor visitor(15);
        results.topDownBreadthFirst(visitor);
        detector.m_ResultsQueue.push(results, 1100);
        LOG_DEBUG("Adding 15 at 1100");
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(0), detector.m_ResultsQueue.chooseResultTime(1100, bucketSize, results));
    }
    {
        model::SAnnotatedProbability prob(1.0);

        model::CHierarchicalResults results;
        results.addModelResult(0, false, "mean", model::function_t::E_IndividualMetricMean,
                               "", "", "", "", "value", prob, 0, 1000);
        CResultsScoreVisitor visitor(20);
        results.topDownBreadthFirst(visitor);
        detector.m_ResultsQueue.push(results, 1150);
        LOG_DEBUG("Adding 20 at 1150");
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(0), detector.m_ResultsQueue.chooseResultTime(1150, bucketSize, results));
    }
    {
        model::SAnnotatedProbability prob(1.0);

        model::CHierarchicalResults results;
        results.addModelResult(0, false, "mean", model::function_t::E_IndividualMetricMean,
                               "", "", "", "", "value", prob, 0, 1000);
        CResultsScoreVisitor visitor(25);
        results.topDownBreadthFirst(visitor);
        detector.m_ResultsQueue.push(results, 1200);
        LOG_DEBUG("Adding 25 at 1200");
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(1100), detector.m_ResultsQueue.chooseResultTime(1200, bucketSize, results));
    }
    {
        model::SAnnotatedProbability prob(1.0);

        model::CHierarchicalResults results;
        results.addModelResult(0, false, "mean", model::function_t::E_IndividualMetricMean,
                               "", "", "", "", "value", prob, 0, 1000);
        CResultsScoreVisitor visitor(0);
        results.topDownBreadthFirst(visitor);
        detector.m_ResultsQueue.push(results, 1250);
        LOG_DEBUG("Adding 0 at 1250");
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(0), detector.m_ResultsQueue.chooseResultTime(1250, bucketSize, results));
    }
    {
        model::SAnnotatedProbability prob(1.0);

        model::CHierarchicalResults results;
        results.addModelResult(0, false, "mean", model::function_t::E_IndividualMetricMean,
                               "", "", "", "", "value", prob, 0, 1000);
        CResultsScoreVisitor visitor(5);
        results.topDownBreadthFirst(visitor);
        detector.m_ResultsQueue.push(results, 1300);
        LOG_DEBUG("Adding 5 at 1300");
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(1200), detector.m_ResultsQueue.chooseResultTime(1300, bucketSize, results));
    }
    {
        model::SAnnotatedProbability prob(1.0);

        model::CHierarchicalResults results;
        results.addModelResult(0, false, "mean", model::function_t::E_IndividualMetricMean,
                               "", "", "", "", "value", prob, 0, 1000);
        CResultsScoreVisitor visitor(5);
        results.topDownBreadthFirst(visitor);
        detector.m_ResultsQueue.push(results, 1350);
        LOG_DEBUG("Adding 5 at 1350");
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(0), detector.m_ResultsQueue.chooseResultTime(1350, bucketSize, results));
    }
    {
        model::SAnnotatedProbability prob(1.0);

        model::CHierarchicalResults results;
        results.addModelResult(0, false, "mean", model::function_t::E_IndividualMetricMean,
                               "", "", "", "", "value", prob, 0, 1000);
        CResultsScoreVisitor visitor(1);
        results.topDownBreadthFirst(visitor);
        detector.m_ResultsQueue.push(results, 1400);
        LOG_DEBUG("Adding 1 at 1400");
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(1300), detector.m_ResultsQueue.chooseResultTime(1400, bucketSize, results));
    }
}

void CAnomalyDetectorTest::testModelDebugOutput(void)
{
    LOG_DEBUG("*** testModelDebugOutput ***");
    {
        // Test non-overlapping buckest
        core_t::TTime bucketSize = 10000;
        model::CLimits limits;
        api::CFieldConfig fieldConfig;
        api::CFieldConfig::TStrVec clauses;
        clauses.push_back("mean(value)");
        clauses.push_back("by");
        clauses.push_back("animal");
        fieldConfig.initFromClause(clauses);

        model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(bucketSize,
                bucketSize, 1u, model_t::E_None, "", 0, 0, false, "");
        modelConfig.modelDebugBoundsPercentile(1.0);
        modelConfig.modelDebugDestination(model::CModelConfig::E_DataStore);
        std::stringstream outputStrm;

        {
            api::CJsonOutputWriter outputWriter("job", outputStrm);

            api::CAnomalyDetector detector("job", limits, fieldConfig, modelConfig,
                    outputWriter);

            api::CAnomalyDetector::TStrVec fieldNames;
            api::CAnomalyDetector::TStrStrUMap dataRows;
            fieldNames.push_back("_time");
            fieldNames.push_back("value");
            fieldNames.push_back("animal");

            dataRows["_time"] = "10000000";
            dataRows["value"] = "2.0";
            dataRows["animal"] = "baboon";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["value"] = "5.0";
            dataRows["animal"] = "shark";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10010000";
            dataRows["value"] = "2.0";
            dataRows["animal"] = "baboon";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["value"] = "5.0";
            dataRows["animal"] = "shark";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10020000";
            dataRows["value"] = "2.0";
            dataRows["animal"] = "baboon";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["value"] = "5.0";
            dataRows["animal"] = "shark";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10030000";
            dataRows["value"] = "2.0";
            dataRows["animal"] = "baboon";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["value"] = "5.0";
            dataRows["animal"] = "shark";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10040000";
            dataRows["value"] = "3.0";
            dataRows["animal"] = "baboon";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["value"] = "5.0";
            dataRows["animal"] = "shark";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            detector.finalise();
        }

        std::string output = outputStrm.str();
        LOG_TRACE("Output has yielded: " << output);
        core::CRegex regex;
        regex.init("\n");
        core::CRegex::TStrVec lines;
        regex.split(output, lines);
        CPPUNIT_ASSERT(findLine("debug_feature.*timestamp.*10000000.*baboon", lines));
        CPPUNIT_ASSERT(findLine("debug_feature.*timestamp.*10000000.*shark", lines));
        CPPUNIT_ASSERT(findLine("debug_feature.*timestamp.*10010000.*baboon", lines));
        CPPUNIT_ASSERT(findLine("debug_feature.*timestamp.*10010000.*shark", lines));
        CPPUNIT_ASSERT(findLine("debug_feature.*timestamp.*10020000.*baboon", lines));
        CPPUNIT_ASSERT(findLine("debug_feature.*timestamp.*10020000.*shark", lines));
        CPPUNIT_ASSERT(findLine("debug_feature.*timestamp.*10030000.*baboon", lines));
        CPPUNIT_ASSERT(findLine("debug_feature.*timestamp.*10030000.*shark", lines));
    }
    {
        // Test overlapping buckets
        core_t::TTime bucketSize = 10000;
        model::CLimits limits;
        api::CFieldConfig fieldConfig;
        api::CFieldConfig::TStrVec clauses;
        clauses.push_back("max(value)");
        fieldConfig.initFromClause(clauses);

        // 2 delay buckets
        model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(bucketSize,
                bucketSize, 1u, model_t::E_None, "", 0, 2, false, "");
        modelConfig.modelDebugBoundsPercentile(1.0);
        modelConfig.modelDebugDestination(model::CModelConfig::E_DataStore);

        std::stringstream outputStrm;
        {
            api::CJsonOutputWriter outputWriter("job", outputStrm);

            api::CAnomalyDetector detector("job", limits, fieldConfig, modelConfig,
                    outputWriter);

            api::CAnomalyDetector::TStrVec fieldNames;
            api::CAnomalyDetector::TStrStrUMap dataRows;
            fieldNames.push_back("_time");
            fieldNames.push_back("value");

            // Data contains 3 anomalies
            dataRows["_time"] = "10000000";
            dataRows["value"] = "2.0";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10010000";
            dataRows["value"] = "2.1";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10020000";
            dataRows["value"] = "2.0";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10030000";
            dataRows["value"] = "2.3";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10040000";
            dataRows["value"] = "2.2";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10055500";
            dataRows["value"] = "2.0";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10060000";
            dataRows["value"] = "2.0";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10077700";
            dataRows["value"] = "2.1";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10080000";
            dataRows["value"] = "2.4";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10090000";
            dataRows["value"] = "2.1";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10094400";
            dataRows["value"] = "2.0003";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10110000";
            dataRows["value"] = "2.01";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10120000";
            dataRows["value"] = "2.03";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10140000";
            dataRows["value"] = "2.001";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10150000";
            dataRows["value"] = "2.1";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10167000";
            dataRows["value"] = "200.0";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10170000";
            dataRows["value"] = "2.0";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10183000";
            dataRows["value"] = "400.0";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10190000";
            dataRows["value"] = "2.0";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10200000";
            dataRows["value"] = "2.0";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10210000";
            dataRows["value"] = "2.0";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));
            dataRows["_time"] = "10230000";
            dataRows["value"] = "2.0";
            CPPUNIT_ASSERT(detector.handleRecord(false, fieldNames, dataRows));

            detector.finalise();
        }

        std::string output = outputStrm.str();
        LOG_TRACE("Output has yielded: " << output);
        core::CRegex regex;
        regex.init("\n");
        core::CRegex::TStrVec lines;
        regex.split(output, lines);
        CPPUNIT_ASSERT(findLine("debug_feature.*timestamp.*10000000000", lines));
        CPPUNIT_ASSERT(findLine("debug_feature.*timestamp.*10010000000", lines));
        CPPUNIT_ASSERT(findLine("debug_feature.*timestamp.*10020000000", lines));
        CPPUNIT_ASSERT(findLine("debug_feature.*timestamp.*10075000000.*actual..2\\.4", lines));
        CPPUNIT_ASSERT(findLine("debug_feature.*timestamp.*10160000000.*actual..200", lines));
        CPPUNIT_ASSERT(findLine("debug_feature.*timestamp.*10175000000.*actual..400", lines));
    }
}


CppUnit::Test* CAnomalyDetectorTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CAnomalyDetectorTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CAnomalyDetectorTest>(
                                   "CAnomalyDetectorTest::testBadTimes",
                                   &CAnomalyDetectorTest::testBadTimes) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CAnomalyDetectorTest>(
                                   "CAnomalyDetectorTest::testOutOfSequence",
                                   &CAnomalyDetectorTest::testOutOfSequence) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CAnomalyDetectorTest>(
                                   "CAnomalyDetectorTest::testControlMessages",
                                   &CAnomalyDetectorTest::testControlMessages) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CAnomalyDetectorTest>(
                                   "CAnomalyDetectorTest::testOutOfPhase",
                                   &CAnomalyDetectorTest::testOutOfPhase) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CAnomalyDetectorTest>(
                                   "CAnomalyDetectorTest::testBucketSelection",
                                   &CAnomalyDetectorTest::testBucketSelection) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CAnomalyDetectorTest>(
                                   "CAnomalyDetectorTest::testModelDebugOutput",
                                   &CAnomalyDetectorTest::testModelDebugOutput) );
    return suiteOfTests;
}
