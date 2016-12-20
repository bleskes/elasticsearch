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
#include "CCsvOutputWriterTest.h"

#include <core/CLogger.h>
#include <core/COsFileFuncs.h>
#include <core/CStringUtils.h>
#include <core/CTimeUtils.h>

#include <api/CCsvOutputWriter.h>

#include <fstream>
#include <sstream>


CppUnit::Test *CCsvOutputWriterTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CCsvOutputWriterTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CCsvOutputWriterTest>(
                                   "CCsvOutputWriterTest::testAdd",
                                   &CCsvOutputWriterTest::testAdd) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CCsvOutputWriterTest>(
                                   "CCsvOutputWriterTest::testOverwrite",
                                   &CCsvOutputWriterTest::testOverwrite) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CCsvOutputWriterTest>(
                                   "CCsvOutputWriterTest::testThroughput",
                                   &CCsvOutputWriterTest::testThroughput) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CCsvOutputWriterTest>(
                                   "CCsvOutputWriterTest::testExcelQuoting",
                                   &CCsvOutputWriterTest::testExcelQuoting) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CCsvOutputWriterTest>(
                                   "CCsvOutputWriterTest::testNonExcelQuoting",
                                   &CCsvOutputWriterTest::testNonExcelQuoting) );

    return suiteOfTests;
}

void CCsvOutputWriterTest::testAdd(void)
{
    // In this test, the output is the input plus an extra field - no input
    // fields are changed

    prelert::api::CCsvOutputWriter writer;

    prelert::api::CCsvOutputWriter::TStrVec splunkFieldNames;
    splunkFieldNames.push_back("_cd");
    splunkFieldNames.push_back("_indextime");
    splunkFieldNames.push_back("_kv");
    splunkFieldNames.push_back("_raw");
    splunkFieldNames.push_back("_serial");
    splunkFieldNames.push_back("_si");
    splunkFieldNames.push_back("_sourcetype");
    splunkFieldNames.push_back("_time");
    splunkFieldNames.push_back("date_hour");
    splunkFieldNames.push_back("date_mday");
    splunkFieldNames.push_back("date_minute");
    splunkFieldNames.push_back("date_month");
    splunkFieldNames.push_back("date_second");
    splunkFieldNames.push_back("date_wday");
    splunkFieldNames.push_back("date_year");
    splunkFieldNames.push_back("date_zone");
    splunkFieldNames.push_back("eventtype");
    splunkFieldNames.push_back("host");
    splunkFieldNames.push_back("index");
    splunkFieldNames.push_back("linecount");
    splunkFieldNames.push_back("punct");
    splunkFieldNames.push_back("source");
    splunkFieldNames.push_back("sourcetype");
    splunkFieldNames.push_back("splunk_server");
    splunkFieldNames.push_back("timeendpos");
    splunkFieldNames.push_back("timestartpos");

    prelert::api::CCsvOutputWriter::TStrVec prelertFieldNames;
    prelertFieldNames.push_back("prelert_type");

    CPPUNIT_ASSERT(writer.fieldNames(splunkFieldNames, prelertFieldNames));

    prelert::api::CCsvOutputWriter::TStrStrUMap originalFields;
    originalFields["_cd"] = "0:3933689";
    originalFields["_indextime"] = "1337698174";
    originalFields["_kv"] = "1";
    originalFields["_raw"] = "2010-02-11 16:11:19+00,service has started,160198,24";
    originalFields["_serial"] = "14";
    originalFields["_si"] = "linux.prelert.com\nmain";
    originalFields["_sourcetype"] = "rmds";
    originalFields["_time"] = "1265904679";
    originalFields["date_hour"] = "16";
    originalFields["date_mday"] = "11";
    originalFields["date_minute"] = "11";
    originalFields["date_month"] = "february";
    originalFields["date_second"] = "19";
    originalFields["date_wday"] = "thursday";
    originalFields["date_year"] = "2010";
    originalFields["date_zone"] = "local";
    originalFields["eventtype"] = "";
    originalFields["host"] = "linux.prelert.com";
    originalFields["index"] = "main";
    originalFields["linecount"] = "1";
    originalFields["punct"] = "--_::+,__,,";
    originalFields["source"] = "/export/temp/cs_10feb_reload";
    originalFields["sourcetype"] = "rmds";
    originalFields["splunk_server"] = "linux.prelert.com";
    originalFields["timeendpos"] = "22";
    originalFields["timestartpos"] = "0";

    prelert::api::CCsvOutputWriter::TStrStrUMap prelertFields;
    prelertFields["prelert_type"] = "75";

    CPPUNIT_ASSERT(writer.writeRow(false, originalFields, prelertFields));

    std::string output(writer.internalString());

    LOG_DEBUG("Output is:\n" << output);

    for (prelert::api::CCsvOutputWriter::TStrVecCItr iter = splunkFieldNames.begin();
         iter != splunkFieldNames.end();
         ++iter)
    {
        LOG_DEBUG("Checking output contains '" << *iter << "'");
        CPPUNIT_ASSERT(output.find(*iter) != std::string::npos);
    }

    for (prelert::api::CCsvOutputWriter::TStrVecCItr iter = prelertFieldNames.begin();
         iter != prelertFieldNames.end();
         ++iter)
    {
        LOG_DEBUG("Checking output contains '" << *iter << "'");
        CPPUNIT_ASSERT(output.find(*iter) != std::string::npos);
    }

    for (prelert::api::CCsvOutputWriter::TStrStrUMapCItr iter = originalFields.begin();
         iter != originalFields.end();
         ++iter)
    {
        LOG_DEBUG("Checking output contains '" << iter->second << "'");
        CPPUNIT_ASSERT(output.find(iter->second) != std::string::npos);
    }

    for (prelert::api::CCsvOutputWriter::TStrStrUMapCItr iter = prelertFields.begin();
         iter != prelertFields.end();
         ++iter)
    {
        LOG_DEBUG("Checking output contains '" << iter->second << "'");
        CPPUNIT_ASSERT(output.find(iter->second) != std::string::npos);
    }
}

void CCsvOutputWriterTest::testOverwrite(void)
{
    // In this test, some fields from the input are changed in the output

    prelert::api::CCsvOutputWriter writer;

    prelert::api::CCsvOutputWriter::TStrVec splunkFieldNames;
    splunkFieldNames.push_back("_cd");
    splunkFieldNames.push_back("_indextime");
    splunkFieldNames.push_back("_kv");
    splunkFieldNames.push_back("_raw");
    splunkFieldNames.push_back("_serial");
    splunkFieldNames.push_back("_si");
    splunkFieldNames.push_back("_sourcetype");
    splunkFieldNames.push_back("_time");
    splunkFieldNames.push_back("date_hour");
    splunkFieldNames.push_back("date_mday");
    splunkFieldNames.push_back("date_minute");
    splunkFieldNames.push_back("date_month");
    splunkFieldNames.push_back("date_second");
    splunkFieldNames.push_back("date_wday");
    splunkFieldNames.push_back("date_year");
    splunkFieldNames.push_back("date_zone");
    splunkFieldNames.push_back("eventtype");
    splunkFieldNames.push_back("host");
    splunkFieldNames.push_back("index");
    splunkFieldNames.push_back("linecount");
    splunkFieldNames.push_back("punct");
    splunkFieldNames.push_back("source");
    splunkFieldNames.push_back("sourcetype");
    splunkFieldNames.push_back("splunk_server");
    splunkFieldNames.push_back("timeendpos");
    splunkFieldNames.push_back("timestartpos");

    prelert::api::CCsvOutputWriter::TStrVec prelertFieldNames;
    prelertFieldNames.push_back("eventtype");
    prelertFieldNames.push_back("prelert_type");

    CPPUNIT_ASSERT(writer.fieldNames(splunkFieldNames, prelertFieldNames));

    prelert::api::CCsvOutputWriter::TStrStrUMap originalFields;
    originalFields["_cd"] = "0:3933689";
    originalFields["_indextime"] = "1337698174";
    originalFields["_kv"] = "1";
    originalFields["_raw"] = "2010-02-11 16:11:19+00,service has started,160198,24";
    originalFields["_serial"] = "14";
    originalFields["_si"] = "linux.prelert.com\nmain";
    originalFields["_sourcetype"] = "rmds";
    originalFields["_time"] = "1265904679";
    originalFields["date_hour"] = "16";
    originalFields["date_mday"] = "11";
    originalFields["date_minute"] = "11";
    originalFields["date_month"] = "february";
    originalFields["date_second"] = "19";
    originalFields["date_wday"] = "thursday";
    originalFields["date_year"] = "2010";
    originalFields["date_zone"] = "local";
    originalFields["eventtype"] = "";
    originalFields["host"] = "linux.prelert.com";
    originalFields["index"] = "main";
    originalFields["linecount"] = "1";
    originalFields["punct"] = "--_::+,__,,";
    originalFields["source"] = "/export/temp/cs_10feb_reload";
    originalFields["sourcetype"] = "rmds";
    originalFields["splunk_server"] = "linux.prelert.com";
    originalFields["timeendpos"] = "22";
    originalFields["timestartpos"] = "0";

    prelert::api::CCsvOutputWriter::TStrStrUMap prelertFields;
    prelertFields["_cd"] = "2:8934689";
    prelertFields["date_zone"] = "GMT";
    prelertFields["prelert_type"] = "42";

    CPPUNIT_ASSERT(writer.writeRow(false, originalFields, prelertFields));

    std::string output(writer.internalString());

    LOG_DEBUG("Output is:\n" << output);

    for (prelert::api::CCsvOutputWriter::TStrVecCItr iter = splunkFieldNames.begin();
         iter != splunkFieldNames.end();
         ++iter)
    {
        LOG_DEBUG("Checking output contains '" << *iter << "'");
        CPPUNIT_ASSERT(output.find(*iter) != std::string::npos);
    }

    for (prelert::api::CCsvOutputWriter::TStrVecCItr iter = prelertFieldNames.begin();
         iter != prelertFieldNames.end();
         ++iter)
    {
        LOG_DEBUG("Checking output contains '" << *iter << "'");
        CPPUNIT_ASSERT(output.find(*iter) != std::string::npos);
    }

    for (prelert::api::CCsvOutputWriter::TStrStrUMapCItr iter = originalFields.begin();
         iter != originalFields.end();
         ++iter)
    {
        // The Prelert fields should override the originals
        if (prelertFields.find(iter->first) == prelertFields.end())
        {
            LOG_DEBUG("Checking output contains '" << iter->second << "'");
            CPPUNIT_ASSERT(output.find(iter->second) != std::string::npos);
        }
        else
        {
            LOG_DEBUG("Checking output does not contain '" << iter->second << "'");
            CPPUNIT_ASSERT(output.find(iter->second) == std::string::npos);
        }
    }

    for (prelert::api::CCsvOutputWriter::TStrStrUMapCItr iter = prelertFields.begin();
         iter != prelertFields.end();
         ++iter)
    {
        LOG_DEBUG("Checking output contains '" << iter->second << "'");
        CPPUNIT_ASSERT(output.find(iter->second) != std::string::npos);
    }
}

void CCsvOutputWriterTest::testThroughput(void)
{
    // In this test, some fields from the input are changed in the output

    // Write to /dev/null (Unix) or nul (Windows)
    std::ofstream ofs(prelert::core::COsFileFuncs::NULL_FILENAME);
    CPPUNIT_ASSERT(ofs.is_open());

    prelert::api::CCsvOutputWriter writer(ofs);

    prelert::api::CCsvOutputWriter::TStrVec splunkFieldNames;
    splunkFieldNames.push_back("_cd");
    splunkFieldNames.push_back("_indextime");
    splunkFieldNames.push_back("_kv");
    splunkFieldNames.push_back("_raw");
    splunkFieldNames.push_back("_serial");
    splunkFieldNames.push_back("_si");
    splunkFieldNames.push_back("_sourcetype");
    splunkFieldNames.push_back("_time");
    splunkFieldNames.push_back("date_hour");
    splunkFieldNames.push_back("date_mday");
    splunkFieldNames.push_back("date_minute");
    splunkFieldNames.push_back("date_month");
    splunkFieldNames.push_back("date_second");
    splunkFieldNames.push_back("date_wday");
    splunkFieldNames.push_back("date_year");
    splunkFieldNames.push_back("date_zone");
    splunkFieldNames.push_back("eventtype");
    splunkFieldNames.push_back("host");
    splunkFieldNames.push_back("index");
    splunkFieldNames.push_back("linecount");
    splunkFieldNames.push_back("punct");
    splunkFieldNames.push_back("source");
    splunkFieldNames.push_back("sourcetype");
    splunkFieldNames.push_back("splunk_server");
    splunkFieldNames.push_back("timeendpos");
    splunkFieldNames.push_back("timestartpos");

    prelert::api::CCsvOutputWriter::TStrVec prelertFieldNames;
    prelertFieldNames.push_back("eventtype");
    prelertFieldNames.push_back("prelert_type");

    prelert::api::CCsvOutputWriter::TStrStrUMap originalFields;
    originalFields["_cd"] = "0:3933689";
    originalFields["_indextime"] = "1337698174";
    originalFields["_kv"] = "1";
    originalFields["_raw"] = "2010-02-11 16:11:19+00,service has started,160198,24";
    originalFields["_serial"] = "14";
    originalFields["_si"] = "linux.prelert.com\nmain";
    originalFields["_sourcetype"] = "rmds";
    originalFields["_time"] = "1265904679";
    originalFields["date_hour"] = "16";
    originalFields["date_mday"] = "11";
    originalFields["date_minute"] = "11";
    originalFields["date_month"] = "february";
    originalFields["date_second"] = "19";
    originalFields["date_wday"] = "thursday";
    originalFields["date_year"] = "2010";
    originalFields["date_zone"] = "local";
    originalFields["eventtype"] = "";
    originalFields["host"] = "linux.prelert.com";
    originalFields["index"] = "main";
    originalFields["linecount"] = "1";
    originalFields["punct"] = "--_::+,__,,";
    originalFields["source"] = "/export/temp/cs_10feb_reload";
    originalFields["sourcetype"] = "rmds";
    originalFields["splunk_server"] = "linux.prelert.com";
    originalFields["timeendpos"] = "22";
    originalFields["timestartpos"] = "0";

    prelert::api::CCsvOutputWriter::TStrStrUMap prelertFields;
    prelertFields["_cd"] = "2:8934689";
    prelertFields["date_zone"] = "GMT";
    prelertFields["prelert_type"] = "42";

    // Write the record this many times
    static const size_t TEST_SIZE(75000);

    prelert::core_t::TTime start(prelert::core::CTimeUtils::now());
    LOG_INFO("Starting throughput test at " <<
             prelert::core::CTimeUtils::toTimeString(start));

    CPPUNIT_ASSERT(writer.fieldNames(splunkFieldNames, prelertFieldNames));

    for (size_t count = 0; count < TEST_SIZE; ++count)
    {
        CPPUNIT_ASSERT(writer.writeRow(false, originalFields, prelertFields));
    }

    prelert::core_t::TTime end(prelert::core::CTimeUtils::now());
    LOG_INFO("Finished throughput test at " <<
             prelert::core::CTimeUtils::toTimeString(end));

    LOG_INFO("Writing " << TEST_SIZE <<
             " records took " << (end - start) << " seconds");
}

void CCsvOutputWriterTest::testExcelQuoting(void)
{
    prelert::api::CCsvOutputWriter writer;

    prelert::api::CCsvOutputWriter::TStrVec fieldNames;
    fieldNames.push_back("no_special");
    fieldNames.push_back("contains_quote");
    fieldNames.push_back("contains_quote_quote");
    fieldNames.push_back("contains_separator");
    fieldNames.push_back("contains_quote_separator");
    fieldNames.push_back("contains_newline");
    fieldNames.push_back("contains_quote_newline");

    CPPUNIT_ASSERT(writer.fieldNames(fieldNames));

    prelert::api::CCsvOutputWriter::TStrStrUMap fieldValues;
    fieldValues["no_special"] = "a";
    fieldValues["contains_quote"] = "\"";
    fieldValues["contains_quote_quote"] = "\"\"";
    fieldValues["contains_separator"] = ",";
    fieldValues["contains_quote_separator"] = "\",";
    fieldValues["contains_newline"] = "\n";
    fieldValues["contains_quote_newline"] = "\"\n";

    CPPUNIT_ASSERT(writer.writeRow(false, fieldValues));

    std::string output(writer.internalString());

    LOG_DEBUG("Output is:\n" << output);

    CPPUNIT_ASSERT_EQUAL(std::string(
                             "no_special,"
                             "contains_quote,"
                             "contains_quote_quote,"
                             "contains_separator,"
                             "contains_quote_separator,"
                             "contains_newline,"
                             "contains_quote_newline\n"
                             "a,"
                             "\"\"\"\","
                             "\"\"\"\"\"\","
                             "\",\","
                             "\"\"\",\","
                             "\"\n\","
                             "\"\"\"\n\"\n"
                         ),
                         output);
}

void CCsvOutputWriterTest::testNonExcelQuoting(void)
{
    prelert::api::CCsvOutputWriter writer(false,
                                          true,
                                          '\\');

    prelert::api::CCsvOutputWriter::TStrVec fieldNames;
    fieldNames.push_back("no_special");
    fieldNames.push_back("contains_quote");
    fieldNames.push_back("contains_escape");
    fieldNames.push_back("contains_escape_quote");
    fieldNames.push_back("contains_separator");
    fieldNames.push_back("contains_escape_separator");
    fieldNames.push_back("contains_newline");
    fieldNames.push_back("contains_escape_newline");

    CPPUNIT_ASSERT(writer.fieldNames(fieldNames));

    prelert::api::CCsvOutputWriter::TStrStrUMap fieldValues;
    fieldValues["no_special"] = "a";
    fieldValues["contains_quote"] = "\"";
    fieldValues["contains_escape"] = "\\";
    fieldValues["contains_escape_quote"] = "\\\"";
    fieldValues["contains_separator"] = ",";
    fieldValues["contains_escape_separator"] = "\\,";
    fieldValues["contains_newline"] = "\n";
    fieldValues["contains_escape_newline"] = "\\\n";

    CPPUNIT_ASSERT(writer.writeRow(false, fieldValues));

    std::string output(writer.internalString());

    LOG_DEBUG("Output is:\n" << output);

    CPPUNIT_ASSERT_EQUAL(std::string(
                             "no_special,"
                             "contains_quote,"
                             "contains_escape,"
                             "contains_escape_quote,"
                             "contains_separator,"
                             "contains_escape_separator,"
                             "contains_newline,"
                             "contains_escape_newline\n"
                             "a,"
                             "\"\\\"\","
                             "\"\\\\\","
                             "\"\\\\\\\"\","
                             "\",\","
                             "\"\\\\,\","
                             "\"\n\","
                             "\"\\\\\n\"\n"
                         ),
                         output);
}

