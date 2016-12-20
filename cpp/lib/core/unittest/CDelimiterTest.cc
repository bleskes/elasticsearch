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
#include "CDelimiterTest.h"

#include <core/CDelimiter.h>
#include <core/CLogger.h>
#include <core/CXmlNodeWithChildren.h>
#include <core/CXmlParser.h>

#include <algorithm>
#include <sstream>


CppUnit::Test *CDelimiterTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CDelimiterTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CDelimiterTest>(
                                   "CDelimiterTest::testSimpleTokenise",
                                   &CDelimiterTest::testSimpleTokenise) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CDelimiterTest>(
                                   "CDelimiterTest::testRegexTokenise",
                                   &CDelimiterTest::testRegexTokenise) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CDelimiterTest>(
                                   "CDelimiterTest::testQuotedTokenise",
                                   &CDelimiterTest::testQuotedTokenise) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CDelimiterTest>(
                                   "CDelimiterTest::testQuotedEscapedTokenise",
                                   &CDelimiterTest::testQuotedEscapedTokenise) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CDelimiterTest>(
                                   "CDelimiterTest::testInvalidQuotedTokenise",
                                   &CDelimiterTest::testInvalidQuotedTokenise) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CDelimiterTest>(
                                   "CDelimiterTest::testQuoteEqualsEscapeTokenise",
                                   &CDelimiterTest::testQuoteEqualsEscapeTokenise) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CDelimiterTest>(
                                   "CDelimiterTest::testXmlParse",
                                   &CDelimiterTest::testXmlParse) );
    return suiteOfTests;
}

void CDelimiterTest::testSimpleTokenise(void)
{
    std::string testData(
        "Oct 12, 2008 8:38:51 AM org.apache.tomcat.util.http.Parameters processParameters\n"
        "WARNING: Parameters: Invalid chunk ignored.\n"
        "Oct 12, 2008 8:38:52 AM org.apache.tomcat.util.http.Parameters processParameters\n"
        "WARNING: Parameters: Invalid chunk ignored.\n"
        "Oct 12, 2008 8:38:53 AM org.apache.tomcat.util.http.Parameters processParameters\n"
        "WARNING: Parameters: Invalid chunk ignored.\n"
        "Oct 12, 2008 8:39:03 AM org.apache.tomcat.util.http.Parameters processParameters\n"
        "WARNING: Parameters: Invalid chunk ignored.\n"
        "Oct 12, 2008 8:39:04 AM org.apache.tomcat.util.http.Parameters processParameters\n"
        "WARNING: Parameters: Invalid chunk ignored.\n"
    );

    LOG_DEBUG("Input data:\n" << testData << '\n');

    prelert::core::CDelimiter delimiter("\n", "\\w+\\s+\\d+,\\s+\\d+\\s+\\d+:\\d+:\\d+\\s+\\w+", true);

    prelert::core::CStringUtils::TStrVec delimited;
    std::string remainder;

    delimiter.tokenise(testData, false, delimited, remainder);

    std::ostringstream strm1;
    std::copy(delimited.begin(), delimited.end(), TStrOStreamItr(strm1, "\n"));
    LOG_DEBUG("First output data:\nNumber of lines = " << delimited.size() << "\nLines are:\n" << strm1.str());
    LOG_DEBUG("First remainder:\n" << remainder << '\n');

    CPPUNIT_ASSERT_EQUAL(size_t(4), delimited.size());
    CPPUNIT_ASSERT(remainder.size() > 0);

    delimited.clear();

    delimiter.tokenise(testData, true, delimited, remainder);

    std::ostringstream strm2;
    std::copy(delimited.begin(), delimited.end(), TStrOStreamItr(strm2, "\n"));
    LOG_DEBUG("Second output data:\nNumber of lines = " << delimited.size() << "\nLines are:\n" << strm2.str());
    LOG_DEBUG("Second remainder:\n" << remainder << '\n');

    CPPUNIT_ASSERT_EQUAL(size_t(5), delimited.size());
    CPPUNIT_ASSERT_EQUAL(size_t(0), remainder.size());
}

void CDelimiterTest::testRegexTokenise(void)
{
    // Some of the lines here are Windows text format, and others Unix text
    std::string testData(
        "Oct 12, 2008 8:38:51 AM org.apache.tomcat.util.http.Parameters processParameters\r\n"
        "WARNING: Parameters: Invalid chunk ignored.\r\n"
        "Oct 12, 2008 8:38:52 AM org.apache.tomcat.util.http.Parameters processParameters\r\n"
        "WARNING: Parameters: Invalid chunk ignored.\n"
        "Oct 12, 2008 8:38:53 AM org.apache.tomcat.util.http.Parameters processParameters\n"
        "WARNING: Parameters: Invalid chunk ignored.\r\n"
        "Oct 12, 2008 8:39:03 AM org.apache.tomcat.util.http.Parameters processParameters\r\n"
        "WARNING: Parameters: Invalid chunk ignored.\n"
        "Oct 12, 2008 8:39:04 AM org.apache.tomcat.util.http.Parameters processParameters\n"
        "WARNING: Parameters: Invalid chunk ignored.\n"
    );

    LOG_DEBUG("Input data:\n" << testData << '\n');

    // Regex matches line terminator for either Windows or Unix text
    prelert::core::CDelimiter delimiter("\r?\n", "\\w+\\s+\\d+,\\s+\\d+\\s+\\d+:\\d+:\\d+\\s+\\w+", true);

    prelert::core::CStringUtils::TStrVec delimited;
    std::string remainder;

    delimiter.tokenise(testData, false, delimited, remainder);

    std::ostringstream strm1;
    std::copy(delimited.begin(), delimited.end(), TStrOStreamItr(strm1, "\n"));
    LOG_DEBUG("First output data:\nNumber of lines = " << delimited.size() << "\nLines are:\n" << strm1.str());
    LOG_DEBUG("First remainder:\n" << remainder << '\n');

    CPPUNIT_ASSERT_EQUAL(size_t(4), delimited.size());
    CPPUNIT_ASSERT(remainder.size() > 0);

    delimited.clear();

    delimiter.tokenise(testData, true, delimited, remainder);

    std::ostringstream strm2;
    std::copy(delimited.begin(), delimited.end(), TStrOStreamItr(strm2, "\n"));
    LOG_DEBUG("Second output data:\nNumber of lines = " << delimited.size() << "\nLines are:\n" << strm2.str());
    LOG_DEBUG("Second remainder:\n" << remainder << '\n');

    CPPUNIT_ASSERT_EQUAL(size_t(5), delimited.size());
    CPPUNIT_ASSERT_EQUAL(size_t(0), remainder.size());
}

void CDelimiterTest::testQuotedTokenise(void)
{
    // NB: The backslashes here escape the quotes for the benefit of the C++ compiler
    std::string testData(
        "5,1,1271776508.400482140,8479710,0x00000001,0x00000002,\"PUB_UPDATE\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",0x0000000000000000,0x0000000000000000,\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\""
    );

    LOG_DEBUG("Input data:\n" << testData << '\n');

    prelert::core::CDelimiter delimiter(",");
    delimiter.quote('"');

    prelert::core::CStringUtils::TStrVec delimited;
    std::string remainder;

    delimiter.tokenise(testData, false, delimited, remainder);

    delimited.push_back(remainder);

    std::ostringstream strm;
    std::copy(delimited.begin(), delimited.end(), TStrOStreamItr(strm, "\n"));
    LOG_DEBUG("Quoted output data:\nNumber of lines = " << delimited.size() << "\nLines are:\n" << strm.str());

    // 40 fields (most blank)
    CPPUNIT_ASSERT_EQUAL(size_t(40), delimited.size());
}

void CDelimiterTest::testQuotedEscapedTokenise(void)
{
    // Similar to previous test, but there are four values with escaped quotes in AFTER
    // pre-processing by the C++ compiler 
    std::string testData(
        "5,1,1271776508.400482140,8479710,0x00000001,0x00000002,\"PUB_UPDATE\",\"\",\"\\\"\",\"\",\"\",\"\",\"\",\"\",\"A \\\"middling\\\" one\",\"\",\"\",\"\",\"\",0x0000000000000000,0x0000000000000000,\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\\\"start\",\"\",\"\",\"end\\\"\",\"\",\"\",\"\",\"\",\"\",\"\\\"both\\\"\",\"\",\"\""
    );

    LOG_DEBUG("Input data:\n" << testData << '\n');

    prelert::core::CDelimiter delimiter(",");
    delimiter.quote('"');

    prelert::core::CStringUtils::TStrVec delimited;
    std::string remainder;

    delimiter.tokenise(testData, false, delimited, remainder);

    delimited.push_back(remainder);

    std::ostringstream strm;
    std::copy(delimited.begin(), delimited.end(), TStrOStreamItr(strm, "\n"));
    LOG_DEBUG("Quoted output data:\nNumber of lines = " << delimited.size() << "\nLines are:\n" << strm.str());

    // 40 fields (most blank)
    CPPUNIT_ASSERT_EQUAL(size_t(40), delimited.size());
}

void CDelimiterTest::testInvalidQuotedTokenise(void)
{
    // Invalid quoting (e.g. mismatched) mustn't cause the tokeniser to go into
    // an infinite loop
    std::string testData(
        "4/26/2011 4:19,mwine.nkulu@za.didata.com,\"32111\",\"/cmn_notif_message.do?sysparm_collection=sys_user&sysparm_modify_check=true&sysparm_record_target=sys_user&sysparm_collection_key=user&sysparm_encoded_record=SBSvOy+GbxSsfF+OWJTvXJPWR8dBy4SU7R08j6go5cMW4bPALaJIz/vRN47yVhEBh+h65r6vs9cV"
    );

    LOG_DEBUG("Input data:\n" << testData << '\n');

    prelert::core::CDelimiter delimiter(",");
    delimiter.quote('"');

    prelert::core::CStringUtils::TStrVec delimited;
    std::string remainder;

    delimiter.tokenise(testData, false, delimited, remainder);

    CPPUNIT_ASSERT_EQUAL(size_t(3), delimited.size());
    CPPUNIT_ASSERT_EQUAL(std::string("/cmn_notif_message.do?sysparm_collection=sys_user&sysparm_modify_check=true&sysparm_record_target=sys_user&sysparm_collection_key=user&sysparm_encoded_record=SBSvOy+GbxSsfF+OWJTvXJPWR8dBy4SU7R08j6go5cMW4bPALaJIz/vRN47yVhEBh+h65r6vs9cV"), remainder);
}

void CDelimiterTest::testQuoteEqualsEscapeTokenise(void)
{
    // In this example, double quotes are used for quoting, but they are escaped
    // by doubling them up, so the escape character is the same as the quote
    // character
    std::string testData(
        "May 24 22:02:13 1,2012/05/24 22:02:13,01606001116,THREAT,url,1,2012/04/10 02:53:17,192.168.0.3,69.171.228.13,0.0.0.0,0.0.0.0,rule1,counselor,,facebook-posting,vsys1,trust,untrust,ethernet1/2,ethernet1/1,forwardAll,2012/04/10 02:53:19,27555,1,8450,80,0,0,0x200000,tcp,alert,\"www.facebook.com/ajax/pagelet/generic.php/WebEgoPane?__a=1&data={\"\"pid\"\":34,\"\"data\"\":[\"\"a.468913596604.267218.603261604\"\",true,false]}&__user=857280013\",(9999),social-networking,informational,client-to-server,0,0x0,192.168.0.0-192.168.255.255,United States,0,application/x-javascript"
    );

    LOG_DEBUG("Input data:\n" << testData << '\n');

    prelert::core::CDelimiter delimiter(",");
    delimiter.quote('"', '"');

    prelert::core::CStringUtils::TStrVec delimited;
    std::string remainder;

    delimiter.tokenise(testData, false, delimited, remainder);

    delimited.push_back(remainder);

    std::ostringstream strm;
    std::copy(delimited.begin(), delimited.end(), TStrOStreamItr(strm, "\n"));
    LOG_DEBUG("Quoted output data:\nNumber of lines = " << delimited.size() << "\nLines are:\n" << strm.str());

    // 42 fields - in particular, the JSON data at index 31 in the vector should
    // still contain commas and double quotes
    CPPUNIT_ASSERT_EQUAL(size_t(42), delimited.size());
    CPPUNIT_ASSERT(delimited[31].find(',') != std::string::npos);
    CPPUNIT_ASSERT(delimited[31].find('"') != std::string::npos);
}

void CDelimiterTest::testXmlParse(void)
{
    {
        // Test invalid constructor
        prelert::core::CDelimiter delimiter("(");
        CPPUNIT_ASSERT(!delimiter.valid());
    }
    {
        // Test invalid constructor
        prelert::core::CDelimiter delimiter("(", "(");
        CPPUNIT_ASSERT(!delimiter.valid());
    }
    {
        prelert::core::CXmlParser parser;
        std::string xml = "<delimiter quote=\"'\" escape=\"^\" \
delimiter_followed_by_regex=\"start.*more.*end\" \
delimiter_followed_by_time=\"true\"><![CDATA[\
start.*middle.*end]]></delimiter>";

        CPPUNIT_ASSERT(parser.parseString(xml));

        prelert::core::CXmlNodeWithChildren::TXmlNodeWithChildrenP rootNodePtr;
        CPPUNIT_ASSERT(parser.toNodeHierarchy(rootNodePtr));
        CPPUNIT_ASSERT(rootNodePtr != 0);

        prelert::core::CDelimiter delimiter;
        CPPUNIT_ASSERT(delimiter.parse(*rootNodePtr));

        prelert::core::CXmlNode node = delimiter.toXmlNode();
        CPPUNIT_ASSERT_EQUAL(std::string("name=delimiter;value=start.*middle.*end;quote=';escape=^;delimiter_followed_by_regex=start.*more.*end;delimiter_followed_by_time=true;"),
                             node.dump());

    }
}


