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
#include "CLineifiedXmlInputParserTest.h"

#include <core/CLogger.h>
#include <core/CRapidXmlParser.h>
#include <core/CTimeUtils.h>
#include <core/CXmlParser.h>

#include <api/CCsvInputParser.h>
#include <api/CLineifiedXmlInputParser.h>
#include <api/CLineifiedXmlOutputWriter.h>

#include <boost/ref.hpp>

#include <fstream>
#include <sstream>


CppUnit::Test *CLineifiedXmlInputParserTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CLineifiedXmlInputParserTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CLineifiedXmlInputParserTest>(
                                   "CLineifiedXmlInputParserTest::testThroughputArbitraryConformant",
                                   &CLineifiedXmlInputParserTest::testThroughputArbitraryConformant) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CLineifiedXmlInputParserTest>(
                                   "CLineifiedXmlInputParserTest::testThroughputCommonConformant",
                                   &CLineifiedXmlInputParserTest::testThroughputCommonConformant) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CLineifiedXmlInputParserTest>(
                                   "CLineifiedXmlInputParserTest::testThroughputArbitraryRapid",
                                   &CLineifiedXmlInputParserTest::testThroughputArbitraryRapid) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CLineifiedXmlInputParserTest>(
                                   "CLineifiedXmlInputParserTest::testThroughputCommonRapid",
                                   &CLineifiedXmlInputParserTest::testThroughputCommonRapid) );

    return suiteOfTests;
}

namespace
{


class CSetupVisitor
{
    public:
        CSetupVisitor(void)
            : m_RecordsPerBlock(0),
              m_OutputWriter("root")
        {
        }

        //! Handle settings
        bool operator()(const prelert::api::CCsvInputParser::TStrStrUMap &/*splunkSettings*/)
        {
            return false;
        }

        //! Handle a record
        bool operator()(bool isDryRun,
                        const prelert::api::CCsvInputParser::TStrVec &/*fieldNames*/,
                        const prelert::api::CCsvInputParser::TStrStrUMap &dataRowFields)
        {
            if (!isDryRun)
            {
                ++m_RecordsPerBlock;
            }

            CPPUNIT_ASSERT(m_OutputWriter.writeRow(isDryRun, dataRowFields));

            return true;
        }

        std::string input(size_t testSize) const
        {
            const std::string &block = m_OutputWriter.internalString();

            std::string str;
            str.reserve(testSize * block.length());

            // Duplicate the binary data according to the test size
            for (size_t count = 0; count < testSize; ++count)
            {
                str.append(block);
            }

            LOG_DEBUG("Input size is " << str.length());

            return str;
        }

        size_t recordsPerBlock(void) const
        {
            return m_RecordsPerBlock;
        }

    private:
        size_t                                  m_RecordsPerBlock;
        prelert::api::CLineifiedXmlOutputWriter m_OutputWriter;
};

class CVisitor
{
    public:
        CVisitor(void)
            : m_RecordCount(0),
              m_SeenSettings(false)
        {
        }

        //! Handle settings
        bool operator()(const prelert::api::CLineifiedXmlInputParser::TStrStrUMap &/*splunkSettings*/)
        {
            m_SeenSettings = true;

            // XML input parser does not process settings
            CPPUNIT_ASSERT(false);

            return false;
        }

        //! Handle a record
        bool operator()(bool isDryRun,
                        const prelert::api::CLineifiedXmlInputParser::TStrVec &/*fieldNames*/,
                        const prelert::api::CLineifiedXmlInputParser::TStrStrUMap &/*dataRowFields*/)
        {
            if (!isDryRun)
            {
                ++m_RecordCount;
            }

            return true;
        }

        size_t recordCount(void) const
        {
            return m_RecordCount;
        }

        bool seenSettings(void) const
        {
            return m_SeenSettings;
        }

    private:
        size_t m_RecordCount;
        bool   m_SeenSettings;
};


}

void CLineifiedXmlInputParserTest::testThroughputArbitraryConformant(void)
{
    LOG_INFO("Testing using a standards-conformant XML parser assuming arbitrary fields in XML documents");
    this->runTest<prelert::core::CXmlParser>(false);
}

void CLineifiedXmlInputParserTest::testThroughputCommonConformant(void)
{
    LOG_INFO("Testing using a standards-conformant XML parser assuming all XML documents have the same fields");
    this->runTest<prelert::core::CXmlParser>(true);
}

void CLineifiedXmlInputParserTest::testThroughputArbitraryRapid(void)
{
    LOG_INFO("Testing using a rapid XML parser assuming arbitrary fields in XML documents");
    this->runTest<prelert::core::CRapidXmlParser>(false);
}

void CLineifiedXmlInputParserTest::testThroughputCommonRapid(void)
{
    LOG_INFO("Testing using a rapid XML parser assuming all XML documents have the same fields");
    this->runTest<prelert::core::CRapidXmlParser>(true);
}

template <typename PARSER>
void CLineifiedXmlInputParserTest::runTest(bool allDocsSameStructure)
{
    // NB: For fair comparison with the other input formats (CSV and Google
    // Protocol Buffers), the input data and test size must be identical

    LOG_DEBUG("Creating throughput test data");

    std::ifstream ifs("testfiles/simple.txt");
    CPPUNIT_ASSERT(ifs.is_open());

    CSetupVisitor setupVisitor;

    prelert::api::CCsvInputParser setupParser(ifs);

    CPPUNIT_ASSERT(setupParser.readStream(false,
                                          boost::ref(setupVisitor),
                                          boost::ref(setupVisitor)));

    // Construct a large test input
    static const size_t TEST_SIZE(5000);
    std::istringstream input(setupVisitor.input(TEST_SIZE));

    PARSER underlyingParser;
    prelert::api::CLineifiedXmlInputParser parser(underlyingParser,
                                                  input,
                                                  allDocsSameStructure);

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

    CPPUNIT_ASSERT_EQUAL(setupVisitor.recordsPerBlock() * TEST_SIZE, visitor.recordCount());

    LOG_INFO("Parsing " << visitor.recordCount() <<
             " records took " << (end - start) << " seconds");
}

