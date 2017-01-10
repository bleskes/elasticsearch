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
#include "CAnomalyDetectorLimitTest.h"
#include "CMockDataProcessor.h"

#include <core/CoreTypes.h>

#include <api/CAnomalyDetector.h>
#include <api/CCsvInputParser.h>
#include <api/CFieldConfig.h>
#include <api/CJsonOutputWriter.h>
#include <api/CHierarchicalResultsWriter.h>

#include <model/CLimits.h>
#include <model/CModelConfig.h>

#include <boost/tuple/tuple.hpp>

#include <set>
#include <sstream>
#include <fstream>

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
class CMockOutputWriter : public ml::api::CJsonOutputWriter
{
    public:
        typedef boost::tuple<ml::core_t::TTime, double /* probability */,
            std::string /* byFieldName*/, std::string /* overFieldName */,
            std::string /* partitionFieldName */> TResultsTp;
        typedef std::vector<TResultsTp> TResultsVec;
        typedef TResultsVec::const_iterator TResultsVecCItr;

    public:
        //! Constructor
        CMockOutputWriter(void) :
            ml::api::CJsonOutputWriter("job", m_Writer)
        { }

        //! Destructor
        virtual ~CMockOutputWriter(void)
        {
            // Finalise the Json writer so it doesn't try and write to
            // m_Writer which will have been destroyed first
            ml::api::CJsonOutputWriter::finalise();
        }

        //! Override for handling the anomaly results
        virtual bool acceptResult(const ml::api::CHierarchicalResultsWriter::SResults &results)
        {
            if (results.s_ByFieldName != "count" && !results.s_IsOverallResult)
            {
                m_Results.push_back(TResultsTp(results.s_BucketStartTime,
                                               results.s_Probability,
                                               results.s_ByFieldValue,
                                               results.s_OverFieldValue,
                                               results.s_PartitionFieldValue));
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

}

using namespace ml;

CppUnit::Test* CAnomalyDetectorLimitTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CAnomalyDetectorLimitTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CAnomalyDetectorLimitTest>(
                                   "CAnomalyDetectorLimitTest::testLimit",
                                   &CAnomalyDetectorLimitTest::testLimit) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CAnomalyDetectorLimitTest>(
                                   "CAnomalyDetectorLimitTest::testAccuracy",
                                   &CAnomalyDetectorLimitTest::testAccuracy) );
    return suiteOfTests;
}

void CAnomalyDetectorLimitTest::testAccuracy(void)
{
    // Check that the amount of memory used when we go over the
    // resource limit is close enough to the limit that we specified

    std::size_t nonLimitedUsage;

    {
        // Without limits, this data set should make the models around
        // 1230000 bytes
        // Run the data once to find out what the current platform uses
        api::CFieldConfig fieldConfig;
        api::CFieldConfig::TStrVec clause;
        clause.push_back("value");
        clause.push_back("by");
        clause.push_back("colour");
        clause.push_back("over");
        clause.push_back("species");
        clause.push_back("partitionfield=greenhouse");

        CPPUNIT_ASSERT(fieldConfig.initFromClause(clause));

        model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(3600);

        ::CMockOutputWriter resultsHandler;

        model::CLimits limits;
        //limits.resourceMonitor().m_ByteLimitHigh = 100000;
        //limits.resourceMonitor().m_ByteLimitLow = 90000;

        LOG_TRACE("Setting up detector");
        api::CAnomalyDetector detector("job",
                                       limits,
                                       fieldConfig,
                                       modelConfig,
                                       resultsHandler);

        std::ifstream inputStrm("testfiles/resource_accuracy.csv");
        CPPUNIT_ASSERT(inputStrm.is_open());
        api::CCsvInputParser parser(inputStrm);

        LOG_TRACE("Reading file");
        CPPUNIT_ASSERT(parser.readStream(false,
                                         boost::bind(&api::CAnomalyDetector::handleSettings,
                                                     &detector,
                                                     _1),
                                         boost::bind(&api::CAnomalyDetector::handleRecord,
                                                     &detector,
                                                     _1,
                                                     _2,
                                                     _3)));

        LOG_TRACE("Checking results");

        CPPUNIT_ASSERT_EQUAL(uint64_t(18630), detector.numRecordsHandled());
        LOG_TRACE(resultsHandler.printInternal());

        nonLimitedUsage = limits.resourceMonitor().m_Current;
    }
    {
        // Now run the data with limiting
        api::CFieldConfig fieldConfig;
        api::CFieldConfig::TStrVec clause;
        clause.push_back("value");
        clause.push_back("by");
        clause.push_back("colour");
        clause.push_back("over");
        clause.push_back("species");
        clause.push_back("partitionfield=greenhouse");

        CPPUNIT_ASSERT(fieldConfig.initFromClause(clause));

        model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(3600);

        ::CMockOutputWriter resultsHandler;

        model::CLimits limits;
        limits.resourceMonitor().m_ByteLimitHigh = nonLimitedUsage / 10;
        limits.resourceMonitor().m_ByteLimitLow =
            limits.resourceMonitor().m_ByteLimitHigh - 1024;

        LOG_TRACE("Setting up detector");
        api::CAnomalyDetector detector("job",
                                       limits,
                                       fieldConfig,
                                       modelConfig,
                                       resultsHandler);

        std::ifstream inputStrm("testfiles/resource_accuracy.csv");
        CPPUNIT_ASSERT(inputStrm.is_open());
        api::CCsvInputParser parser(inputStrm);

        LOG_TRACE("Reading file");
        CPPUNIT_ASSERT(parser.readStream(false,
                                         boost::bind(&api::CAnomalyDetector::handleSettings,
                                                     &detector,
                                                     _1),
                                         boost::bind(&api::CAnomalyDetector::handleRecord,
                                                     &detector,
                                                     _1,
                                                     _2,
                                                     _3)));

        LOG_TRACE("Checking results");

        CPPUNIT_ASSERT_EQUAL(uint64_t(18630), detector.numRecordsHandled());
        LOG_TRACE(resultsHandler.printInternal());

        // TODO this limit must be tightened once there is more granular control
        // over the model memory creation
        std::size_t limitedUsage = limits.resourceMonitor().m_Current;
        LOG_DEBUG("Non-limited usage: " << nonLimitedUsage << "; limited: " << limitedUsage);
        CPPUNIT_ASSERT(limitedUsage < nonLimitedUsage);
    }
}

void CAnomalyDetectorLimitTest::testLimit(void)
{
    typedef std::set<std::string> TStrSet;

    ::CMockOutputWriter::TResultsVec firstResults;

    {
        // Run the data without any resource limits and check that
        // all the expected fields are in the results set
        model::CLimits limits;
        api::CFieldConfig fieldConfig;
        api::CFieldConfig::TStrVec clause;
        clause.push_back("value");
        clause.push_back("by");
        clause.push_back("colour");
        clause.push_back("over");
        clause.push_back("species");
        clause.push_back("partitionfield=greenhouse");

        CPPUNIT_ASSERT(fieldConfig.initFromClause(clause));

        model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(3600);

        ::CMockOutputWriter resultsHandler;

        LOG_TRACE("Setting up detector");
        api::CAnomalyDetector detector("job",
                                       limits,
                                       fieldConfig,
                                       modelConfig,
                                       resultsHandler);

        std::ifstream inputStrm("testfiles/resource_limits_3_2over_3partition.csv");
        CPPUNIT_ASSERT(inputStrm.is_open());
        api::CCsvInputParser parser(inputStrm);

        LOG_TRACE("Reading file");
        CPPUNIT_ASSERT(parser.readStream(false,
                                         boost::bind(&api::CAnomalyDetector::handleSettings,
                                                     &detector,
                                                     _1),
                                         boost::bind(&api::CAnomalyDetector::handleRecord,
                                                     &detector,
                                                     _1,
                                                     _2,
                                                     _3)));
        LOG_TRACE("Checking results");
        CPPUNIT_ASSERT_EQUAL(uint64_t(1176), detector.numRecordsHandled());

        // Save the results
        for (::CMockOutputWriter::TResultsVecCItr i = resultsHandler.results().begin();
             i != resultsHandler.results().end(); ++i)
        {
            firstResults.push_back(*i);
        }

        CPPUNIT_ASSERT_EQUAL(std::size_t(12), firstResults.size());

        TStrSet partitions;
        TStrSet people;
        TStrSet attributes;
        for (std::size_t i = 0; i < firstResults.size(); ++i)
        {
            partitions.insert(firstResults[i].get<4>());
            people.insert(firstResults[i].get<3>());
            attributes.insert(firstResults[i].get<2>());
        }
        CPPUNIT_ASSERT_EQUAL(std::size_t(3), partitions.size());
        CPPUNIT_ASSERT_EQUAL(std::size_t(2), people.size());
        CPPUNIT_ASSERT_EQUAL(std::size_t(2), attributes.size());
    }
    {
        // Run the data with some resource limits after the first 4 records and
        // check that we get only anomalies from the first 2 partitions
        model::CLimits limits;
        api::CFieldConfig fieldConfig;
        api::CFieldConfig::TStrVec clause;
        clause.push_back("value");
        clause.push_back("by");
        clause.push_back("colour");
        clause.push_back("over");
        clause.push_back("species");
        clause.push_back("partitionfield=greenhouse");

        CPPUNIT_ASSERT(fieldConfig.initFromClause(clause));

        model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(3600);

        ::CMockOutputWriter resultsHandler;

        LOG_TRACE("Setting up detector");
        api::CAnomalyDetector detector("job",
                                       limits,
                                       fieldConfig,
                                       modelConfig,
                                       resultsHandler);

        std::ifstream inputStrm("testfiles/resource_limits_3_2over_3partition_first8.csv");
        CPPUNIT_ASSERT(inputStrm.is_open());
        api::CCsvInputParser parser(inputStrm);

        LOG_TRACE("Reading file");
        CPPUNIT_ASSERT(parser.readStream(false,
                                         boost::bind(&api::CAnomalyDetector::handleSettings,
                                                     &detector,
                                                     _1),
                                         boost::bind(&api::CAnomalyDetector::handleRecord,
                                                     &detector,
                                                     _1,
                                                     _2,
                                                     _3)));
        // Now turn on the resource limiting
        limits.resourceMonitor().m_ByteLimitHigh = 0;
        limits.resourceMonitor().m_ByteLimitLow = 0;
        limits.resourceMonitor().m_AllowAllocations = false;

        std::ifstream inputStrm2("testfiles/resource_limits_3_2over_3partition_last1169.csv");
        CPPUNIT_ASSERT(inputStrm2.is_open());
        api::CCsvInputParser parser2(inputStrm2);

        LOG_TRACE("Reading second file");
        CPPUNIT_ASSERT(parser2.readStream(false,
                                         boost::bind(&api::CAnomalyDetector::handleSettings,
                                                     &detector,
                                                     _1),
                                         boost::bind(&api::CAnomalyDetector::handleRecord,
                                                     &detector,
                                                     _1,
                                                     _2,
                                                     _3)));
        LOG_TRACE("Checking results");
        CPPUNIT_ASSERT_EQUAL(uint64_t(1180), detector.numRecordsHandled());

        // Save the results
        const ::CMockOutputWriter::TResultsVec &secondResults = resultsHandler.results();

        CPPUNIT_ASSERT_EQUAL(std::size_t(2), secondResults.size());

        TStrSet partitions;
        TStrSet people;
        TStrSet attributes;
        for (std::size_t i = 0; i < secondResults.size(); ++i)
        {
            partitions.insert(secondResults[i].get<4>());
            people.insert(secondResults[i].get<3>());
            attributes.insert(secondResults[i].get<2>());
        }
        CPPUNIT_ASSERT_EQUAL(std::size_t(1), partitions.size());
        CPPUNIT_ASSERT_EQUAL(std::size_t(2), people.size());
        CPPUNIT_ASSERT_EQUAL(std::size_t(1), attributes.size());
    }
}




