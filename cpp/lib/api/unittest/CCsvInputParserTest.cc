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
#include "CCsvInputParserTest.h"

#include <core/CLogger.h>
#include <core/CoreTypes.h>
#include <core/CStringUtils.h>
#include <core/CTimeUtils.h>
#include <core/CTimezone.h>

#include <api/CAnomalyDetector.h>
#include <api/CCsvInputParser.h>

#include <boost/range.hpp>
#include <boost/ref.hpp>

#include <algorithm>
#include <fstream>
#include <vector>


CppUnit::Test *CCsvInputParserTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CCsvInputParserTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CCsvInputParserTest>(
                                   "CCsvInputParserTest::testSimpleDelims",
                                   &CCsvInputParserTest::testSimpleDelims) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CCsvInputParserTest>(
                                   "CCsvInputParserTest::testComplexDelims",
                                   &CCsvInputParserTest::testComplexDelims) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CCsvInputParserTest>(
                                   "CCsvInputParserTest::testThroughput",
                                   &CCsvInputParserTest::testThroughput) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CCsvInputParserTest>(
                                   "CCsvInputParserTest::testDateParse",
                                   &CCsvInputParserTest::testDateParse) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CCsvInputParserTest>(
                                   "CCsvInputParserTest::testQuoteParsing",
                                   &CCsvInputParserTest::testQuoteParsing) );

    return suiteOfTests;
}

namespace
{


class CVisitor
{
    public:
        CVisitor(void)
            : m_Fast(true),
              m_RecordCount(0)
        {
        }

        CVisitor(const prelert::api::CCsvInputParser::TStrVec &expectedFieldNames)
            : m_Fast(false),
              m_RecordCount(0),
              m_ExpectedFieldNames(expectedFieldNames)
        {
        }

        //! Handle settings
        bool operator()(const prelert::api::CCsvInputParser::TStrStrUMap &/*splunkSettings*/)
        {
            // CSV input parser does not process settings
            CPPUNIT_ASSERT(false);

            return false;
        }

        //! Handle a record
        bool operator()(bool isDryRun,
                        const prelert::api::CCsvInputParser::TStrVec &fieldNames,
                        const prelert::api::CCsvInputParser::TStrStrUMap &dataRowFields)
        {
            if (!isDryRun)
            {
                ++m_RecordCount;
            }

            // For the throughput test, the assertions below will skew the
            // results, so bypass them
            if (m_Fast)
            {
                return true;
            }

            // Check the field names
            for (size_t index = 0; index < fieldNames.size(); ++index)
            {
                CPPUNIT_ASSERT(index < m_ExpectedFieldNames.size());
                CPPUNIT_ASSERT_EQUAL(m_ExpectedFieldNames[index], fieldNames[index]);
            }

            CPPUNIT_ASSERT_EQUAL(m_ExpectedFieldNames.size(), fieldNames.size());

            // Now check the actual fields
            for (prelert::api::CCsvInputParser::TStrStrUMapCItr iter = dataRowFields.begin();
                 iter != dataRowFields.end();
                 ++iter)
            {
                LOG_DEBUG("Field " << iter->first << " is " << iter->second);
                CPPUNIT_ASSERT(std::find(fieldNames.begin(), fieldNames.end(), iter->first) != fieldNames.end());
            }

            CPPUNIT_ASSERT_EQUAL(fieldNames.size(), dataRowFields.size());

            // Check the line count is consistent with the _raw field
            prelert::api::CCsvInputParser::TStrStrUMapCItr rawIter = dataRowFields.find("_raw");
            CPPUNIT_ASSERT(rawIter != dataRowFields.end());
            prelert::api::CCsvInputParser::TStrStrUMapCItr lineCountIter = dataRowFields.find("linecount");
            CPPUNIT_ASSERT(lineCountIter != dataRowFields.end());

            size_t expectedLineCount(1 + std::count(rawIter->second.begin(),
                                                    rawIter->second.end(),
                                                    '\n'));
            size_t lineCount(0);
            CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType(lineCountIter->second, lineCount));
            CPPUNIT_ASSERT_EQUAL(expectedLineCount, lineCount);

            return true;
        }

        size_t recordCount(void) const
        {
            return m_RecordCount;
        }

    private:
        bool                                   m_Fast;
        size_t                                 m_RecordCount;
        prelert::api::CCsvInputParser::TStrVec m_ExpectedFieldNames;
};

class CTimeCheckingVisitor
{
    public:
        typedef std::vector<prelert::core_t::TTime> TTimeVec;

    public:
        CTimeCheckingVisitor(const std::string &timeField,
                             const std::string &timeFormat,
                             const TTimeVec &expectedTimes)
            : m_RecordCount(0),
              m_TimeField(timeField),
              m_TimeFormat(timeFormat),
              m_ExpectedTimes(expectedTimes)
        {
        }

        //! Handle settings
        bool operator()(const prelert::api::CCsvInputParser::TStrStrUMap &/*splunkSettings*/)
        {
            // CSV input parser does not process settings
            CPPUNIT_ASSERT(false);

            return false;
        }

        //! Handle a record
        bool operator()(bool isDryRun,
                        const prelert::api::CCsvInputParser::TStrVec &fieldNames,
                        const prelert::api::CCsvInputParser::TStrStrUMap &dataRowFields)
        {
            if (isDryRun)
            {
                return true;
            }

            // Check the time field exists
            CPPUNIT_ASSERT(m_RecordCount < m_ExpectedTimes.size());

            prelert::api::CCsvInputParser::TStrVecCItr fieldNameIter =
                    std::find(fieldNames.begin(),
                              fieldNames.end(),
                              m_TimeField);
            CPPUNIT_ASSERT(fieldNameIter != fieldNames.end());

            CPPUNIT_ASSERT_EQUAL(fieldNames.size(), dataRowFields.size());

            // Now check the actual time
            prelert::api::CCsvInputParser::TStrStrUMapCItr fieldIter = dataRowFields.find(m_TimeField);
            CPPUNIT_ASSERT(fieldIter != dataRowFields.end());
            prelert::core_t::TTime timeVal(0);
            if (m_TimeFormat.empty())
            {
                CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType(fieldIter->second,
                                                                         timeVal));
            }
            else
            {
                CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(m_TimeFormat,
                                                                   fieldIter->second,
                                                                   timeVal));
                LOG_DEBUG("Converted " << fieldIter->second <<
                          " to " << timeVal <<
                          " using format " << m_TimeFormat);
            }
            CPPUNIT_ASSERT_EQUAL(m_ExpectedTimes[m_RecordCount], timeVal);

            ++m_RecordCount;

            return true;
        }

        size_t recordCount(void) const
        {
            return m_RecordCount;
        }

    private:
        size_t      m_RecordCount;
        std::string m_TimeField;
        std::string m_TimeFormat;
        TTimeVec    m_ExpectedTimes;
};

class CQuoteCheckingVisitor
{
    public:
        CQuoteCheckingVisitor(void)
            : m_RecordCount(0)
        {
        }

        //! Handle settings
        bool operator()(const prelert::api::CCsvInputParser::TStrStrUMap &/*splunkSettings*/)
        {
            // CSV input parser does not process settings
            CPPUNIT_ASSERT(false);

            return false;
        }

        //! Handle a record
        bool operator()(bool isDryRun,
                        const prelert::api::CCsvInputParser::TStrVec &fieldNames,
                        const prelert::api::CCsvInputParser::TStrStrUMap &dataRowFields)
        {
            if (isDryRun)
            {
                return true;
            }

            CPPUNIT_ASSERT_EQUAL(fieldNames.size(), dataRowFields.size());

            // Now check quoted fields
            prelert::api::CCsvInputParser::TStrStrUMapCItr fieldIter = dataRowFields.find("q1");
            CPPUNIT_ASSERT(fieldIter != dataRowFields.end());
            CPPUNIT_ASSERT_EQUAL(std::string(""), fieldIter->second);

            fieldIter = dataRowFields.find("q2");
            CPPUNIT_ASSERT(fieldIter != dataRowFields.end());
            CPPUNIT_ASSERT_EQUAL(std::string(""), fieldIter->second);

            fieldIter = dataRowFields.find("q3");
            CPPUNIT_ASSERT(fieldIter != dataRowFields.end());
            CPPUNIT_ASSERT_EQUAL(std::string("\""), fieldIter->second);

            fieldIter = dataRowFields.find("q4");
            CPPUNIT_ASSERT(fieldIter != dataRowFields.end());
            CPPUNIT_ASSERT_EQUAL(std::string("\"\""), fieldIter->second);

            ++m_RecordCount;

            return true;
        }

        size_t recordCount(void) const
        {
            return m_RecordCount;
        }

    private:
        size_t m_RecordCount;
};


}

void CCsvInputParserTest::testSimpleDelims(void)
{
    std::ifstream simpleStrm("testfiles/simple.txt");
    CPPUNIT_ASSERT(simpleStrm.is_open());

    prelert::api::CCsvInputParser parser(simpleStrm);

    prelert::api::CCsvInputParser::TStrVec expectedFieldNames;
    expectedFieldNames.push_back("_cd");
    expectedFieldNames.push_back("_indextime");
    expectedFieldNames.push_back("_kv");
    expectedFieldNames.push_back("_raw");
    expectedFieldNames.push_back("_serial");
    expectedFieldNames.push_back("_si");
    expectedFieldNames.push_back("_sourcetype");
    expectedFieldNames.push_back("_time");
    expectedFieldNames.push_back("date_hour");
    expectedFieldNames.push_back("date_mday");
    expectedFieldNames.push_back("date_minute");
    expectedFieldNames.push_back("date_month");
    expectedFieldNames.push_back("date_second");
    expectedFieldNames.push_back("date_wday");
    expectedFieldNames.push_back("date_year");
    expectedFieldNames.push_back("date_zone");
    expectedFieldNames.push_back("eventtype");
    expectedFieldNames.push_back("host");
    expectedFieldNames.push_back("index");
    expectedFieldNames.push_back("linecount");
    expectedFieldNames.push_back("punct");
    expectedFieldNames.push_back("source");
    expectedFieldNames.push_back("sourcetype");
    expectedFieldNames.push_back("splunk_server");
    expectedFieldNames.push_back("timeendpos");
    expectedFieldNames.push_back("timestartpos");

    CVisitor visitor(expectedFieldNames);

    CPPUNIT_ASSERT(parser.readStream(false,
                                     boost::ref(visitor),
                                     boost::ref(visitor)));

    CPPUNIT_ASSERT_EQUAL(size_t(15), visitor.recordCount());
}

void CCsvInputParserTest::testComplexDelims(void)
{
    std::ifstream complexStrm("testfiles/complex.txt");
    CPPUNIT_ASSERT(complexStrm.is_open());

    prelert::api::CCsvInputParser parser(complexStrm);

    prelert::api::CCsvInputParser::TStrVec expectedFieldNames;
    expectedFieldNames.push_back("_cd");
    expectedFieldNames.push_back("_indextime");
    expectedFieldNames.push_back("_kv");
    expectedFieldNames.push_back("_raw");
    expectedFieldNames.push_back("_serial");
    expectedFieldNames.push_back("_si");
    expectedFieldNames.push_back("_sourcetype");
    expectedFieldNames.push_back("_time");
    expectedFieldNames.push_back("date_hour");
    expectedFieldNames.push_back("date_mday");
    expectedFieldNames.push_back("date_minute");
    expectedFieldNames.push_back("date_month");
    expectedFieldNames.push_back("date_second");
    expectedFieldNames.push_back("date_wday");
    expectedFieldNames.push_back("date_year");
    expectedFieldNames.push_back("date_zone");
    expectedFieldNames.push_back("eventtype");
    expectedFieldNames.push_back("host");
    expectedFieldNames.push_back("index");
    expectedFieldNames.push_back("linecount");
    expectedFieldNames.push_back("punct");
    expectedFieldNames.push_back("source");
    expectedFieldNames.push_back("sourcetype");
    expectedFieldNames.push_back("splunk_server");
    expectedFieldNames.push_back("timeendpos");
    expectedFieldNames.push_back("timestartpos");

    CVisitor visitor(expectedFieldNames);

    // Test doing a dry run this time
    CPPUNIT_ASSERT(parser.readStream(true,
                                     boost::ref(visitor),
                                     boost::ref(visitor)));
}

void CCsvInputParserTest::testThroughput(void)
{
    std::ifstream ifs("testfiles/simple.txt");
    CPPUNIT_ASSERT(ifs.is_open());

    std::string line;

    std::string header;
    if (std::getline(ifs, line).good())
    {
        header = line;
        header += '\n';
    }

    std::string restOfFile;
    size_t nonHeaderLines(0);
    while (std::getline(ifs, line).good())
    {
        if (line.empty())
        {
            break;
        }
        ++nonHeaderLines;
        restOfFile += line;
        restOfFile += '\n';
    }

    // Assume there are two lines per record in the input file
    CPPUNIT_ASSERT((nonHeaderLines % 2) == 0);
    size_t recordsPerBlock(nonHeaderLines / 2);

    // Construct a large test input
    static const size_t TEST_SIZE(10000);
    std::string input(header);
    for (size_t count = 0; count < TEST_SIZE; ++count)
    {
        input += restOfFile;
    }
    LOG_DEBUG("Input size is " << input.length());

    prelert::api::CCsvInputParser parser(input);

    CVisitor visitor;

    prelert::core_t::TTime start(prelert::core::CTimeUtils::now());
    LOG_INFO("Starting throughput test at " <<
             prelert::core::CTimeUtils::toTimeString(start));

    CPPUNIT_ASSERT(parser.readStream(false,
                                     boost::ref(visitor),
                                     boost::ref(visitor)));

    prelert::core_t::TTime end(prelert::core::CTimeUtils::now());
    LOG_INFO("Finished throughput test at " <<
             prelert::core::CTimeUtils::toTimeString(end));

    CPPUNIT_ASSERT_EQUAL(recordsPerBlock * TEST_SIZE, visitor.recordCount());

    LOG_INFO("Parsing " << visitor.recordCount() <<
             " records took " << (end - start) << " seconds");
}

void CCsvInputParserTest::testDateParse(void)
{
    static const prelert::core_t::TTime EXPECTED_TIMES[] = {
        1359331200,
        1359331200,
        1359331207,
        1359331220,
        1359331259,
        1359331262,
        1359331269,
        1359331270,
        1359331272,
        1359331296,
        1359331301,
        1359331311,
        1359331314,
        1359331315,
        1359331316,
        1359331321,
        1359331328,
        1359331333,
        1359331349,
        1359331352,
        1359331370,
        1359331382,
        1359331385,
        1359331386,
        1359331395,
        1359331404,
        1359331416,
        1359331416,
        1359331424,
        1359331429
    };

    CTimeCheckingVisitor::TTimeVec expectedTimes(boost::begin(EXPECTED_TIMES),
                                                 boost::end(EXPECTED_TIMES));

    // Ensure we are in UK timewise
    CPPUNIT_ASSERT(prelert::core::CTimezone::setTimezone("Europe/London"));

    {
        std::ifstream csvStrm("testfiles/s.csv");
        CPPUNIT_ASSERT(csvStrm.is_open());

        CTimeCheckingVisitor visitor("_time",
                                     "",
                                     expectedTimes);

        prelert::api::CCsvInputParser parser(csvStrm);

        CPPUNIT_ASSERT(parser.readStream(false,
                                         boost::ref(visitor),
                                         boost::ref(visitor)));
    }
    {
        std::ifstream csvStrm("testfiles/bdYIMSp.csv");
        CPPUNIT_ASSERT(csvStrm.is_open());

        CTimeCheckingVisitor visitor("date",
                                     "%b %d %Y %I:%M:%S %p",
                                     expectedTimes);

        prelert::api::CCsvInputParser parser(csvStrm);

        CPPUNIT_ASSERT(parser.readStream(false,
                                         boost::ref(visitor),
                                         boost::ref(visitor)));
    }
    {
        std::ifstream csvStrm("testfiles/YmdHMS.csv");
        CPPUNIT_ASSERT(csvStrm.is_open());

        CTimeCheckingVisitor visitor("_time",
                                     "%Y-%m-%d %H:%M:%S",
                                     expectedTimes);

        prelert::api::CCsvInputParser parser(csvStrm);

        CPPUNIT_ASSERT(parser.readStream(false,
                                         boost::ref(visitor),
                                         boost::ref(visitor)));
    }
    {
        std::ifstream csvStrm("testfiles/YmdHMSZ_GMT.csv");
        CPPUNIT_ASSERT(csvStrm.is_open());

        CTimeCheckingVisitor visitor("mytime",
                                     "%Y-%m-%d %H:%M:%S %Z",
                                     expectedTimes);

        prelert::api::CCsvInputParser parser(csvStrm);

        CPPUNIT_ASSERT(parser.readStream(false,
                                         boost::ref(visitor),
                                         boost::ref(visitor)));
    }

    // Switch to US Eastern time for this test
    CPPUNIT_ASSERT(prelert::core::CTimezone::setTimezone("America/New_York"));

    {
        std::ifstream csvStrm("testfiles/YmdHMSZ_EST.csv");
        CPPUNIT_ASSERT(csvStrm.is_open());

        CTimeCheckingVisitor visitor("datetime",
                                     "%Y-%m-%d %H:%M:%S %Z",
                                     expectedTimes);

        prelert::api::CCsvInputParser parser(csvStrm);

        CPPUNIT_ASSERT(parser.readStream(false,
                                         boost::ref(visitor),
                                         boost::ref(visitor)));
    }

    // Set the timezone back to nothing, i.e. let the operating system decide
    // what to use
    CPPUNIT_ASSERT(prelert::core::CTimezone::setTimezone(""));
}

void CCsvInputParserTest::testQuoteParsing(void)
{
    // Expect:
    // q1 =
    // q2 =
    // q3 = "
    // q4 = ""
    std::string input(
        "b,q1,q2,q3,q4,e\n"
        "x,,\"\",\"\"\"\",\"\"\"\"\"\",x\n"
    );

    prelert::api::CCsvInputParser parser(input);

    CQuoteCheckingVisitor visitor;

    CPPUNIT_ASSERT(parser.readStream(false,
                                     boost::ref(visitor),
                                     boost::ref(visitor)));

    CPPUNIT_ASSERT_EQUAL(size_t(1), visitor.recordCount());
}

