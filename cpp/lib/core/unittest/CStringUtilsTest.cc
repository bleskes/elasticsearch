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
#include "CStringUtilsTest.h"

#include <core/CLogger.h>
#include <core/CStopWatch.h>
#include <core/CStringUtils.h>
#include <core/CStrTokR.h>

#include <boost/lexical_cast.hpp>

#include <set>
#include <vector>

#include <stdint.h>
#include <stdlib.h>
#include <string.h>


CppUnit::Test *CStringUtilsTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CStringUtilsTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CStringUtilsTest>(
                                   "CStringUtilsTest::testNumMatches",
                                   &CStringUtilsTest::testNumMatches) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CStringUtilsTest>(
                                   "CStringUtilsTest::testReplace",
                                   &CStringUtilsTest::testReplace) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CStringUtilsTest>(
                                   "CStringUtilsTest::testReplaceFirst",
                                   &CStringUtilsTest::testReplaceFirst) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CStringUtilsTest>(
                                   "CStringUtilsTest::testTypeToString",
                                   &CStringUtilsTest::testTypeToString) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CStringUtilsTest>(
                                   "CStringUtilsTest::testTypeToStringPrecise",
                                   &CStringUtilsTest::testTypeToStringPrecise) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CStringUtilsTest>(
                                   "CStringUtilsTest::testTypeToStringPretty",
                                   &CStringUtilsTest::testTypeToStringPretty) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CStringUtilsTest>(
                                   "CStringUtilsTest::testStringToType",
                                   &CStringUtilsTest::testStringToType) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CStringUtilsTest>(
                                   "CStringUtilsTest::testTokeniser",
                                   &CStringUtilsTest::testTokeniser) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CStringUtilsTest>(
                                   "CStringUtilsTest::testTrim",
                                   &CStringUtilsTest::testTrim) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CStringUtilsTest>(
                                   "CStringUtilsTest::testJoin",
                                   &CStringUtilsTest::testJoin) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CStringUtilsTest>(
                                   "CStringUtilsTest::testLower",
                                   &CStringUtilsTest::testLower) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CStringUtilsTest>(
                                   "CStringUtilsTest::testUpper",
                                   &CStringUtilsTest::testUpper) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CStringUtilsTest>(
                                   "CStringUtilsTest::testNarrowWiden",
                                   &CStringUtilsTest::testNarrowWiden) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CStringUtilsTest>(
                                   "CStringUtilsTest::testEscape",
                                   &CStringUtilsTest::testEscape) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CStringUtilsTest>(
                                   "CStringUtilsTest::testUnEscape",
                                   &CStringUtilsTest::testUnEscape) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CStringUtilsTest>(
                                   "CStringUtilsTest::testLongestSubstr",
                                   &CStringUtilsTest::testLongestSubstr) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CStringUtilsTest>(
                                   "CStringUtilsTest::testLongestSubseq",
                                   &CStringUtilsTest::testLongestSubseq) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CStringUtilsTest>(
                                   "CStringUtilsTest::testNormaliseWhitespace",
                                   &CStringUtilsTest::testNormaliseWhitespace) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CStringUtilsTest>(
                                   "CStringUtilsTest::testPerformance",
                                   &CStringUtilsTest::testPerformance) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CStringUtilsTest>(
                                   "CStringUtilsTest::testUtf8ByteType",
                                   &CStringUtilsTest::testUtf8ByteType) );

    return suiteOfTests;
}

void CStringUtilsTest::testNumMatches(void)
{
    {
        std::string str("%d %M %Y %f %D  %t");

        CPPUNIT_ASSERT_EQUAL(size_t(6), prelert::core::CStringUtils::numMatches(str, "%"));
        CPPUNIT_ASSERT_EQUAL(size_t(0), prelert::core::CStringUtils::numMatches(str, "q"));
    }
}

void CStringUtilsTest::testReplace(void)
{
    {
        std::string in("%d%M%Y%f%D%t");
        const std::string out(" %d %M %Y %f %D %t");

        CPPUNIT_ASSERT_EQUAL(size_t(6), prelert::core::CStringUtils::replace("%", " %", in));

        CPPUNIT_ASSERT_EQUAL(out, in);
    }
    {
        std::string in("%d%M%Y%f%D%t");
        const std::string out("%d%M%Y%f%D%t");

        CPPUNIT_ASSERT_EQUAL(size_t(0), prelert::core::CStringUtils::replace("X", "Y", in));

        CPPUNIT_ASSERT_EQUAL(out, in);
    }
}

void CStringUtilsTest::testReplaceFirst(void)
{
    {
        std::string in("%d%M%Y%f%D%t");
        const std::string out(" %d%M%Y%f%D%t");

        CPPUNIT_ASSERT_EQUAL(size_t(1), prelert::core::CStringUtils::replaceFirst("%", " %", in));

        CPPUNIT_ASSERT_EQUAL(out, in);
    }
    {
        std::string in("%d%M%Y%f%D%t");
        const std::string out("%d%M%Y%f%D%t");

        CPPUNIT_ASSERT_EQUAL(size_t(0), prelert::core::CStringUtils::replaceFirst("X", "Y", in));

        CPPUNIT_ASSERT_EQUAL(out, in);
    }
}

void CStringUtilsTest::testTypeToString(void)
{
    {
        uint64_t    i(18446744073709551615ULL);
        std::string expected("18446744073709551615");

        std::string actual = prelert::core::CStringUtils::typeToString(i);
        CPPUNIT_ASSERT_EQUAL(expected, actual);

        uint64_t    j(0);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType(actual, j));
        CPPUNIT_ASSERT_EQUAL(i, j);
    }
    {
        uint32_t    i(123456U);
        std::string expected("123456");

        std::string actual = prelert::core::CStringUtils::typeToString(i);
        CPPUNIT_ASSERT_EQUAL(expected, actual);

        uint32_t    j(0);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType(actual, j));
        CPPUNIT_ASSERT_EQUAL(i, j);
    }
    {
        uint16_t    i(12345U);
        std::string expected("12345");

        std::string actual = prelert::core::CStringUtils::typeToString(i);
        CPPUNIT_ASSERT_EQUAL(expected, actual);

        uint16_t    j(0);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType(actual, j));
        CPPUNIT_ASSERT_EQUAL(i, j);
    }
    {
        int32_t    i(123456);
        std::string expected("123456");

        std::string actual = prelert::core::CStringUtils::typeToString(i);
        CPPUNIT_ASSERT_EQUAL(expected, actual);

        int32_t    j(0);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType(actual, j));
        CPPUNIT_ASSERT_EQUAL(i, j);
    }
    {
        double      i(0.123456);
        std::string expected("0.123456");

        std::string actual = prelert::core::CStringUtils::typeToString(i);
        CPPUNIT_ASSERT_EQUAL(expected, actual);
    }
    {
        double      i(0.123456e10);
        std::string expected("1234560000.000000");

        std::string actual = prelert::core::CStringUtils::typeToString(i);
        CPPUNIT_ASSERT_EQUAL(expected, actual);
    }
}

void CStringUtilsTest::testTypeToStringPrecise(void)
{
    {
        double      i(1.0);
        std::string expected("1");

        std::string actual = prelert::core::CStringUtils::typeToStringPrecise(i, prelert::core::CIEEE754::E_SinglePrecision);
        CPPUNIT_ASSERT_EQUAL(expected, actual);
    }
    {
        double      i(1.0);
        std::string expected("1");

        std::string actual = prelert::core::CStringUtils::typeToStringPrecise(i, prelert::core::CIEEE754::E_DoublePrecision);
        CPPUNIT_ASSERT_EQUAL(expected, actual);
    }
    {
        double      i(0.123456);
        std::string expected("1.23456e-1");

        std::string actual = prelert::core::CStringUtils::typeToStringPrecise(i, prelert::core::CIEEE754::E_SinglePrecision);
        CPPUNIT_ASSERT_EQUAL(expected, actual);
    }
    {
        double      i(0.123456);
        std::string expected("1.23456e-1");

        std::string actual = prelert::core::CStringUtils::typeToStringPrecise(i, prelert::core::CIEEE754::E_DoublePrecision);
        CPPUNIT_ASSERT_EQUAL(expected, actual);
    }
    {
        double      i(0.123456e10);
        std::string expected("1.23456e9");

        std::string actual = prelert::core::CStringUtils::typeToStringPrecise(i, prelert::core::CIEEE754::E_SinglePrecision);
        CPPUNIT_ASSERT_EQUAL(expected, actual);
    }
    {
        double      i(0.123456e10);
        std::string expected("1234560000");

        std::string actual = prelert::core::CStringUtils::typeToStringPrecise(i, prelert::core::CIEEE754::E_DoublePrecision);
        CPPUNIT_ASSERT_EQUAL(expected, actual);
    }
    {
        double      i(0.123456e-10);
        std::string expected("1.23456e-11");

        std::string actual = prelert::core::CStringUtils::typeToStringPrecise(i, prelert::core::CIEEE754::E_SinglePrecision);
        CPPUNIT_ASSERT_EQUAL(expected, actual);
    }
    {
        double      i(0.123456e-10);
        std::string expected("1.23456e-11");

        std::string actual = prelert::core::CStringUtils::typeToStringPrecise(i, prelert::core::CIEEE754::E_DoublePrecision);
        CPPUNIT_ASSERT_EQUAL(expected, actual);
    }
    {
        double      i(0.123456787654321e-10);
        std::string expected("1.234568e-11");

        std::string actual = prelert::core::CStringUtils::typeToStringPrecise(i, prelert::core::CIEEE754::E_SinglePrecision);
        CPPUNIT_ASSERT_EQUAL(expected, actual);
    }
    {
        double      i(0.123456787654321e-10);
        std::string expected("1.23456787654321e-11");

        std::string actual = prelert::core::CStringUtils::typeToStringPrecise(i, prelert::core::CIEEE754::E_DoublePrecision);
        CPPUNIT_ASSERT_EQUAL(expected, actual);
    }
    {
        double      i(0.00000000012345678765432123456);
        std::string expected("1.234568e-10");

        std::string actual = prelert::core::CStringUtils::typeToStringPrecise(i, prelert::core::CIEEE754::E_SinglePrecision);
        CPPUNIT_ASSERT_EQUAL(expected, actual);
    }
    {
        double      i(0.00000000012345678765432123456);
        std::string expected("1.23456787654321e-10");

        std::string actual = prelert::core::CStringUtils::typeToStringPrecise(i, prelert::core::CIEEE754::E_DoublePrecision);
        CPPUNIT_ASSERT_EQUAL(expected, actual);
    }
    {
        double      i(123456787654321.23456);
        std::string expected("1.234568e14");

        std::string actual = prelert::core::CStringUtils::typeToStringPrecise(i, prelert::core::CIEEE754::E_SinglePrecision);
        CPPUNIT_ASSERT_EQUAL(expected, actual);
    }
    {
        double      i(123456787654321.23456);
        std::string expected("123456787654321");

        std::string actual = prelert::core::CStringUtils::typeToStringPrecise(i, prelert::core::CIEEE754::E_DoublePrecision);
        CPPUNIT_ASSERT_EQUAL(expected, actual);
    }
}

void CStringUtilsTest::testTypeToStringPretty(void)
{
    // This doesn't assert because the format differs between operating systems
    LOG_DEBUG("1.0 -> " << prelert::core::CStringUtils::typeToStringPretty(1.0));
    LOG_DEBUG("0.123456 -> " << prelert::core::CStringUtils::typeToStringPretty(0.123456));
    LOG_DEBUG("0.123456e10 -> " << prelert::core::CStringUtils::typeToStringPretty(0.123456e10));
    LOG_DEBUG("0.123456e-10 -> " << prelert::core::CStringUtils::typeToStringPretty(0.123456e-10));
    LOG_DEBUG("0.123456787654321e-10 -> " << prelert::core::CStringUtils::typeToStringPretty(0.123456787654321e-10));
    LOG_DEBUG("0.00000000012345678765432123456 -> " << prelert::core::CStringUtils::typeToStringPretty(0.00000000012345678765432123456));
    LOG_DEBUG("123456787654321.23456 -> " << prelert::core::CStringUtils::typeToStringPretty(123456787654321.23456));
}

void CStringUtilsTest::testStringToType(void)
{
    {
        // All good conversions
        bool ret;
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("yes", ret));
        CPPUNIT_ASSERT(ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("no", ret));
        CPPUNIT_ASSERT(!ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("yES", ret));
        CPPUNIT_ASSERT(ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("NO", ret));
        CPPUNIT_ASSERT(!ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("true", ret));
        CPPUNIT_ASSERT(ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("false", ret));
        CPPUNIT_ASSERT(!ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("TRUE", ret));
        CPPUNIT_ASSERT(ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("False", ret));
        CPPUNIT_ASSERT(!ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("on", ret));
        CPPUNIT_ASSERT(ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("off", ret));
        CPPUNIT_ASSERT(!ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("On", ret));
        CPPUNIT_ASSERT(ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("OFF", ret));
        CPPUNIT_ASSERT(!ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("y", ret));
        CPPUNIT_ASSERT(ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("n", ret));
        CPPUNIT_ASSERT(!ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("Y", ret));
        CPPUNIT_ASSERT(ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("N", ret));
        CPPUNIT_ASSERT(!ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("t", ret));
        CPPUNIT_ASSERT(ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("f", ret));
        CPPUNIT_ASSERT(!ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("T", ret));
        CPPUNIT_ASSERT(ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("F", ret));
        CPPUNIT_ASSERT(!ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("1", ret));
        CPPUNIT_ASSERT(ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("10", ret));
        CPPUNIT_ASSERT(ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("0", ret));
        CPPUNIT_ASSERT(!ret);
    }
    {
        // All good conversions
        int32_t ret;
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("1000", ret));
        CPPUNIT_ASSERT_EQUAL(int32_t(1000), ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("-1000", ret));
        CPPUNIT_ASSERT_EQUAL(int32_t(-1000), ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("0", ret));
        CPPUNIT_ASSERT_EQUAL(int32_t(0), ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("0x1000", ret));
        CPPUNIT_ASSERT_EQUAL(int32_t(0x1000), ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("2147483647", ret));
        CPPUNIT_ASSERT_EQUAL(int32_t(2147483647), ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("-2147483647", ret));
        CPPUNIT_ASSERT_EQUAL(int32_t(-2147483647), ret);
    }
    {
        // All good conversions
        uint64_t ret;
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("1000", ret));
        CPPUNIT_ASSERT_EQUAL(uint64_t(1000), ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("0", ret));
        CPPUNIT_ASSERT_EQUAL(uint64_t(0), ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("0x1000", ret));
        CPPUNIT_ASSERT_EQUAL(uint64_t(0x1000), ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("18446744073709551615", ret));
        CPPUNIT_ASSERT_EQUAL(uint64_t(18446744073709551615ULL), ret);
    }
    {
        // All good conversions
        uint32_t ret;
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("1000", ret));
        CPPUNIT_ASSERT_EQUAL(uint32_t(1000), ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("0", ret));
        CPPUNIT_ASSERT_EQUAL(uint32_t(0), ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("0x1000", ret));
        CPPUNIT_ASSERT_EQUAL(uint32_t(0x1000), ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("2147483650", ret));
        CPPUNIT_ASSERT_EQUAL(uint32_t(2147483650UL), ret);
    }
    {
        // All good conversions
        uint16_t ret;
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("1000", ret));
        CPPUNIT_ASSERT_EQUAL(uint16_t(1000), ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("0", ret));
        CPPUNIT_ASSERT_EQUAL(uint16_t(0), ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("0x1000", ret));
        CPPUNIT_ASSERT_EQUAL(uint16_t(0x1000), ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("65535", ret));
        CPPUNIT_ASSERT_EQUAL(uint16_t(65535), ret);
    }
    {
        // All good conversions
        double  ret;
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("50.256", ret));
        CPPUNIT_ASSERT_EQUAL(50.256, ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("-50.256", ret));
        CPPUNIT_ASSERT_EQUAL(-50.256, ret);
        CPPUNIT_ASSERT(prelert::core::CStringUtils::stringToType("0", ret));
        CPPUNIT_ASSERT_EQUAL(0.0, ret);
    }
    {
        // All bad conversions
        bool    ret;
        CPPUNIT_ASSERT(!prelert::core::CStringUtils::stringToType("tr", ret));
        CPPUNIT_ASSERT(!prelert::core::CStringUtils::stringToType("fa", ret));
        CPPUNIT_ASSERT(!prelert::core::CStringUtils::stringToType("s1235sd", ret));
    }
    {
        // All bad conversions
        int64_t ret;
        CPPUNIT_ASSERT(!prelert::core::CStringUtils::stringToType("", ret));
        CPPUNIT_ASSERT(!prelert::core::CStringUtils::stringToType("abc", ret));
        CPPUNIT_ASSERT(!prelert::core::CStringUtils::stringToType("9223372036854775808", ret));
        CPPUNIT_ASSERT(!prelert::core::CStringUtils::stringToType("-9223372036854775809", ret));
    }
    {
        // All bad conversions
        int32_t ret;
        CPPUNIT_ASSERT(!prelert::core::CStringUtils::stringToType("abc", ret));
        CPPUNIT_ASSERT(!prelert::core::CStringUtils::stringToType("2147483648", ret));
        CPPUNIT_ASSERT(!prelert::core::CStringUtils::stringToType("-2147483649", ret));
    }
    {
        // All bad conversions
        int16_t ret;
        CPPUNIT_ASSERT(!prelert::core::CStringUtils::stringToType("abc", ret));
        CPPUNIT_ASSERT(!prelert::core::CStringUtils::stringToType("32768", ret));
        CPPUNIT_ASSERT(!prelert::core::CStringUtils::stringToType("-32769", ret));
    }
    {
        // All bad conversions
        uint64_t ret;
        CPPUNIT_ASSERT(!prelert::core::CStringUtils::stringToType("abc", ret));
        CPPUNIT_ASSERT(!prelert::core::CStringUtils::stringToType("18446744073709551616", ret));
    }
    {
        // All bad conversions
        uint32_t ret;
        CPPUNIT_ASSERT(!prelert::core::CStringUtils::stringToType("abc", ret));
        CPPUNIT_ASSERT(!prelert::core::CStringUtils::stringToType("4294967296", ret));
    }
    {
        // All bad conversions
        uint16_t ret;
        CPPUNIT_ASSERT(!prelert::core::CStringUtils::stringToType("", ret));
        CPPUNIT_ASSERT(!prelert::core::CStringUtils::stringToType("abc", ret));
        CPPUNIT_ASSERT(!prelert::core::CStringUtils::stringToType("65536", ret));
    }
}

void CStringUtilsTest::testTokeniser(void)
{
    std::string str = "sadcasd csac asdcasdc asdc asdc sadc sadc asd csdc ewwef f sdf sd f sdf  sdfsadfasdf\n"
                      "adscasdcadsc\n"
                      "asdfcasdcadsds<ENDsa wefasdsadc<END>asdcsadcadsc\n"
                      "asdcasdcsdcasdc\n"
                      "sdcsdacsdac\n"
                      "sdcasdacs<END>";

    // Note: the test is done with strtok, which uses ANY ONE character in the
    // delimiter string to split on, so the delimiters for this test have to be
    // one character
    this->testTokeniser(">", str);
    this->testTokeniser("\n", str);
    this->testTokeniser("f", str);
}

void CStringUtilsTest::testTokeniser(const std::string &delim, const std::string &str)
{
    // Tokenise using prelert
    prelert::core::CStringUtils::TStrVec    tokens;
    std::string                             remainder;

    prelert::core::CStringUtils::tokenise(delim, str, tokens, remainder);

    LOG_DEBUG(str << " DELIM = '" << delim << "' REMAINDER = '" << remainder << "'");

    for (prelert::core::CStringUtils::TStrVecItr itr = tokens.begin(); itr != tokens.end(); ++itr)
    {
        LOG_DEBUG("'" << *itr << "'");
    }

    // Tokenise using strtok
    char *test = ::strdup(str.c_str());
    CPPUNIT_ASSERT(test);

    prelert::core::CStringUtils::TStrVec strtokVec;

    // Note: strtok, uses ANY ONE character in the delimiter string to split on,
    // so the delimiters for this test have to be one character
    char *brk = 0;
    for (char *line = prelert::core::CStrTokR::strTokR(test, delim.c_str(), &brk);
         line != 0;
         line = prelert::core::CStrTokR::strTokR(0, delim.c_str(), &brk))
    {
        strtokVec.push_back(line);
        LOG_DEBUG("'" << line << "'");
    }

    free(test);
    test = 0;

    if (remainder.empty() == false)
    {
        tokens.push_back(remainder);
    }

    std::string::size_type pos = str.rfind(delim);
    if (pos != std::string::npos)
    {
        std::string remainderExpected = str.substr(pos+delim.size());

        CPPUNIT_ASSERT_EQUAL(remainderExpected, remainder);
    }

    // Compare prelert to strtok
    CPPUNIT_ASSERT_EQUAL(strtokVec.size(), tokens.size());
    CPPUNIT_ASSERT(strtokVec == tokens);
}

void CStringUtilsTest::testTrim(void)
{
    std::string testStr;

    testStr = "  hello\r\n";
    prelert::core::CStringUtils::trimWhitespace(testStr);
    CPPUNIT_ASSERT_EQUAL(std::string("hello"), testStr);

    testStr = "  hello world ";
    prelert::core::CStringUtils::trimWhitespace(testStr);
    CPPUNIT_ASSERT_EQUAL(std::string("hello world"), testStr);

    testStr = "\t  hello \t world \t\n";
    prelert::core::CStringUtils::trimWhitespace(testStr);
    CPPUNIT_ASSERT_EQUAL(std::string("hello \t world"), testStr);

    testStr = " ";
    prelert::core::CStringUtils::trimWhitespace(testStr);
    CPPUNIT_ASSERT_EQUAL(std::string(""), testStr);

    testStr = "\t ";
    prelert::core::CStringUtils::trimWhitespace(testStr);
    CPPUNIT_ASSERT_EQUAL(std::string(""), testStr);

    testStr = "\t  hello \t world \t\n";
    prelert::core::CStringUtils::trim(" \th", testStr);
    CPPUNIT_ASSERT_EQUAL(std::string("ello \t world \t\n"), testStr);

    testStr = "\t h h \t  \thhh";
    prelert::core::CStringUtils::trim(" \th", testStr);
    CPPUNIT_ASSERT_EQUAL(std::string(""), testStr);
}

void CStringUtilsTest::testJoin(void)
{
    LOG_DEBUG("*** testJoin ***")
    using namespace prelert;
    using namespace core;
    typedef std::vector<std::string> TStrVec;
    typedef std::set<std::string> TStrSet;

    TStrVec strVec;

    LOG_DEBUG("Test empty container")
    CPPUNIT_ASSERT_EQUAL(std::string(""), CStringUtils::join(strVec, std::string(",")));

    LOG_DEBUG("Test container has empty strings")
    strVec.push_back(std::string());
    strVec.push_back(std::string());
    CPPUNIT_ASSERT_EQUAL(std::string(","), CStringUtils::join(strVec, std::string(",")));

    LOG_DEBUG("Test container has empty strings and delimiter is also empty")
    CPPUNIT_ASSERT_EQUAL(std::string(""), CStringUtils::join(strVec, std::string("")));

    strVec.clear();

    LOG_DEBUG("Test only one item")
    strVec.push_back(std::string("aaa"));
    CPPUNIT_ASSERT_EQUAL(std::string("aaa"), CStringUtils::join(strVec, std::string(",")));

    LOG_DEBUG("Test three items")
    strVec.push_back(std::string("bbb"));
    strVec.push_back(std::string("ccc"));

    CPPUNIT_ASSERT_EQUAL(std::string("aaa,bbb,ccc"), CStringUtils::join(strVec, std::string(",")));

    LOG_DEBUG("Test delimiter has more than one characters")
    CPPUNIT_ASSERT_EQUAL(std::string("aaa::bbb::ccc"), CStringUtils::join(strVec, std::string("::")));

    LOG_DEBUG("Test set instead of vector")
    TStrSet strSet;
    strSet.insert(std::string("aaa"));
    strSet.insert(std::string("bbb"));
    strSet.insert(std::string("ccc"));
    CPPUNIT_ASSERT_EQUAL(std::string("aaa,bbb,ccc"), CStringUtils::join(strSet, std::string(",")));
}

void CStringUtilsTest::testLower(void)
{
    CPPUNIT_ASSERT_EQUAL(std::string("hello"), prelert::core::CStringUtils::toLower("hello"));
    CPPUNIT_ASSERT_EQUAL(std::string("hello"), prelert::core::CStringUtils::toLower("Hello"));
    CPPUNIT_ASSERT_EQUAL(std::string("hello"), prelert::core::CStringUtils::toLower("HELLO"));

    CPPUNIT_ASSERT_EQUAL(std::string("123hello"), prelert::core::CStringUtils::toLower("123hello"));
    CPPUNIT_ASSERT_EQUAL(std::string("hello  "), prelert::core::CStringUtils::toLower("Hello  "));
    CPPUNIT_ASSERT_EQUAL(std::string("_-+hello"), prelert::core::CStringUtils::toLower("_-+HELLO"));
}

void CStringUtilsTest::testUpper(void)
{
    CPPUNIT_ASSERT_EQUAL(std::string("HELLO"), prelert::core::CStringUtils::toUpper("hello"));
    CPPUNIT_ASSERT_EQUAL(std::string("HELLO"), prelert::core::CStringUtils::toUpper("Hello"));
    CPPUNIT_ASSERT_EQUAL(std::string("HELLO"), prelert::core::CStringUtils::toUpper("HELLO"));

    CPPUNIT_ASSERT_EQUAL(std::string("123HELLO"), prelert::core::CStringUtils::toUpper("123hello"));
    CPPUNIT_ASSERT_EQUAL(std::string("HELLO  "), prelert::core::CStringUtils::toUpper("Hello  "));
    CPPUNIT_ASSERT_EQUAL(std::string("_-+HELLO"), prelert::core::CStringUtils::toUpper("_-+HELLO"));
}

void CStringUtilsTest::testNarrowWiden(void)
{
    std::string hello1("Hello");
    std::wstring hello2(L"Hello");

    CPPUNIT_ASSERT_EQUAL(hello1.length(), prelert::core::CStringUtils::narrowToWide(hello1).length());
    CPPUNIT_ASSERT_EQUAL(hello2.length(), prelert::core::CStringUtils::wideToNarrow(hello2).length());

    CPPUNIT_ASSERT(prelert::core::CStringUtils::narrowToWide(hello1) == hello2);
    CPPUNIT_ASSERT(prelert::core::CStringUtils::wideToNarrow(hello2) == hello1);
}

void CStringUtilsTest::testEscape(void)
{
    const std::string toEscape("\"'\\");

    const std::string escaped1("\\\"quoted\\\"");
    std::string unEscaped1("\"quoted\"");

    prelert::core::CStringUtils::escape('\\', toEscape, unEscaped1);
    CPPUNIT_ASSERT_EQUAL(escaped1, unEscaped1);

    const std::string escaped2("\\\\\\\"with escaped quotes\\\\\\\"");
    std::string unEscaped2("\\\"with escaped quotes\\\"");

    prelert::core::CStringUtils::escape('\\', toEscape, unEscaped2);
    CPPUNIT_ASSERT_EQUAL(escaped2, unEscaped2);
}

void CStringUtilsTest::testUnEscape(void)
{
    std::string escaped1("\\\"quoted\\\"");
    const std::string unEscaped1("\"quoted\"");

    prelert::core::CStringUtils::unEscape('\\', escaped1);
    CPPUNIT_ASSERT_EQUAL(unEscaped1, escaped1);

    std::string escaped2("\\\\\\\"with escaped quotes\\\\\\\"");
    const std::string unEscaped2("\\\"with escaped quotes\\\"");

    prelert::core::CStringUtils::unEscape('\\', escaped2);
    CPPUNIT_ASSERT_EQUAL(unEscaped2, escaped2);

    // This should print a warning about the last character being an escape
    std::string dodgy("\\\"dodgy\\");
    prelert::core::CStringUtils::unEscape('\\', dodgy);
}

void CStringUtilsTest::testLongestSubstr(void)
{
    {
        std::string str1;
        std::string str2;

        std::string common(prelert::core::CStringUtils::longestCommonSubstr(str1, str2));

        CPPUNIT_ASSERT_EQUAL(std::string(""), common);

        LOG_DEBUG("Longest common substring of '" <<
                  str1 << "' and '" << str2 << "' is '" << common << "'");
    }
    {
        std::string str1("Hello world");
        std::string str2;

        std::string common(prelert::core::CStringUtils::longestCommonSubstr(str1, str2));

        CPPUNIT_ASSERT_EQUAL(std::string(""), common);

        LOG_DEBUG("Longest common substring of '" <<
                  str1 << "' and '" << str2 << "' is '" << common << "'");
    }
    {
        std::string str1("Hello world");
        std::string str2("Hello mum");

        std::string common(prelert::core::CStringUtils::longestCommonSubstr(str1, str2));

        CPPUNIT_ASSERT_EQUAL(std::string("Hello "), common);

        LOG_DEBUG("Longest common substring of '" <<
                  str1 << "' and '" << str2 << "' is '" << common << "'");
    }
    {
        std::string str1("Hello world");
        std::string str2("Say hello");

        std::string common(prelert::core::CStringUtils::longestCommonSubstr(str1, str2));

        CPPUNIT_ASSERT_EQUAL(std::string("ello"), common);

        LOG_DEBUG("Longest common substring of '" <<
                  str1 << "' and '" << str2 << "' is '" << common << "'");
    }
    {
        std::string str1("abc");
        std::string str2("def");

        std::string common(prelert::core::CStringUtils::longestCommonSubstr(str1, str2));

        CPPUNIT_ASSERT_EQUAL(std::string(""), common);

        LOG_DEBUG("Longest common substring of '" <<
                  str1 << "' and '" << str2 << "' is '" << common << "'");
    }
    {
        std::string str1("abc xyz defgv hij");
        std::string str2("abc w defgtu hij");

        std::string common(prelert::core::CStringUtils::longestCommonSubstr(str1, str2));

        CPPUNIT_ASSERT_EQUAL(std::string(" defg"), common);

        LOG_DEBUG("Longest common substring of '" <<
                  str1 << "' and '" << str2 << "' is '" << common << "'");
    }
    {
        std::string str1("Source LOTS on 33080:842 has shut down.");
        std::string str2("Source D1INTERN_IPT on 33080:1260 has shut down.");

        std::string common(prelert::core::CStringUtils::longestCommonSubstr(str1, str2));

        CPPUNIT_ASSERT_EQUAL(std::string(" has shut down."), common);

        LOG_DEBUG("Longest common substring of '" <<
                  str1 << "' and '" << str2 << "' is '" << common << "'");
    }
    {
        std::string str1("No filter list defined for .");
        std::string str2("No filter list defined for prism_int.");

        std::string common(prelert::core::CStringUtils::longestCommonSubstr(str1, str2));

        CPPUNIT_ASSERT_EQUAL(std::string("No filter list defined for "), common);

        LOG_DEBUG("Longest common substring of '" <<
                  str1 << "' and '" << str2 << "' is '" << common << "'");
    }
}

void CStringUtilsTest::testLongestSubseq(void)
{
    {
        std::string str1;
        std::string str2;

        std::string common(prelert::core::CStringUtils::longestCommonSubsequence(str1, str2));

        CPPUNIT_ASSERT_EQUAL(std::string(""), common);

        LOG_DEBUG("Longest common subsequence of '" <<
                  str1 << "' and '" << str2 << "' is '" << common << "'");
    }
    {
        std::string str1("Hello world");
        std::string str2;

        std::string common(prelert::core::CStringUtils::longestCommonSubsequence(str1, str2));

        CPPUNIT_ASSERT_EQUAL(std::string(""), common);

        LOG_DEBUG("Longest common subsequence of '" <<
                  str1 << "' and '" << str2 << "' is '" << common << "'");
    }
    {
        std::string str1("Hello world");
        std::string str2("Hello mum");

        std::string common(prelert::core::CStringUtils::longestCommonSubsequence(str1, str2));

        CPPUNIT_ASSERT_EQUAL(std::string("Hello "), common);

        LOG_DEBUG("Longest common subsequence of '" <<
                  str1 << "' and '" << str2 << "' is '" << common << "'");
    }
    {
        std::string str1("Hello world");
        std::string str2("Say hello");

        std::string common(prelert::core::CStringUtils::longestCommonSubsequence(str1, str2));

        CPPUNIT_ASSERT_EQUAL(std::string("ello"), common);

        LOG_DEBUG("Longest common subsequence of '" <<
                  str1 << "' and '" << str2 << "' is '" << common << "'");
    }
    {
        std::string str1("abc");
        std::string str2("def");

        std::string common(prelert::core::CStringUtils::longestCommonSubsequence(str1, str2));

        CPPUNIT_ASSERT_EQUAL(std::string(""), common);

        LOG_DEBUG("Longest common subsequence of '" <<
                  str1 << "' and '" << str2 << "' is '" << common << "'");
    }
    {
        std::string str1("abc xyz defgv hij");
        std::string str2("abc w defgtu hij");

        std::string common(prelert::core::CStringUtils::longestCommonSubsequence(str1, str2));

        CPPUNIT_ASSERT_EQUAL(std::string("abc  defg hij"), common);

        LOG_DEBUG("Longest common subsequence of '" <<
                  str1 << "' and '" << str2 << "' is '" << common << "'");
    }
    {
        std::string str1("Source LOTS on 33080:842 has shut down.");
        std::string str2("Source D1INTERN_IPT on 33080:1260 has shut down.");

        std::string common(prelert::core::CStringUtils::longestCommonSubsequence(str1, str2));

        CPPUNIT_ASSERT_EQUAL(std::string("Source T on 33080:2 has shut down."), common);

        LOG_DEBUG("Longest common subsequence of '" <<
                  str1 << "' and '" << str2 << "' is '" << common << "'");
    }
    {
        std::string str1("No filter list defined for .");
        std::string str2("No filter list defined for prism_int.");

        std::string common(prelert::core::CStringUtils::longestCommonSubsequence(str1, str2));

        CPPUNIT_ASSERT_EQUAL(std::string("No filter list defined for ."), common);

        LOG_DEBUG("Longest common subsequence of '" <<
                  str1 << "' and '" << str2 << "' is '" << common << "'");
    }
}

void CStringUtilsTest::testNormaliseWhitespace(void)
{
    std::string spacey(" what\ta   lot \tof\n"
                       "spaces");
    std::string normalised(" what a lot of spaces");

    CPPUNIT_ASSERT_EQUAL(normalised, prelert::core::CStringUtils::normaliseWhitespace(spacey));
}

void CStringUtilsTest::testPerformance(void)
{
    static const size_t TEST_SIZE(1000000);
    static const double TEST_SIZE_D(static_cast<double>(TEST_SIZE));

    prelert::core::CStopWatch stopWatch;

    {
        LOG_DEBUG("Before CStringUtils::typeToString integer test");

        stopWatch.start();
        for (size_t count = 0; count < TEST_SIZE; ++count)
        {
            std::string result(prelert::core::CStringUtils::typeToString(count));
            prelert::core::CStringUtils::stringToType(result, count);
        }
        uint64_t timeMs(stopWatch.stop());
        LOG_DEBUG("After CStringUtils::typeToString integer test");
        LOG_DEBUG("CStringUtils::typeToString integer test took " << timeMs << "ms");
    }

    stopWatch.reset();

    {
        LOG_DEBUG("Before boost::lexical_cast integer test");
        stopWatch.start();
        for (size_t count = 0; count < TEST_SIZE; ++count)
        {
            std::string result(boost::lexical_cast<std::string>(count));
            count = boost::lexical_cast<size_t>(result);
        }
        uint64_t timeMs(stopWatch.stop());
        LOG_DEBUG("After boost::lexical_cast integer test");
        LOG_DEBUG("boost::lexical_cast integer test took " << timeMs << "ms");
    }

    stopWatch.reset();

    {
        LOG_DEBUG("Before CStringUtils::typeToString floating point test");

        stopWatch.start();
        for (double count = 0.0; count < TEST_SIZE_D; count += 1.41)
        {
            std::string result(prelert::core::CStringUtils::typeToString(count));
            prelert::core::CStringUtils::stringToType(result, count);
        }
        uint64_t timeMs(stopWatch.stop());
        LOG_DEBUG("After CStringUtils::typeToString floating point test");
        LOG_DEBUG("CStringUtils::typeToString floating point test took " << timeMs << "ms");
    }

    stopWatch.reset();

    {
        LOG_DEBUG("Before boost::lexical_cast floating point test");
        stopWatch.start();
        for (double count = 0.0; count < TEST_SIZE_D; count += 1.41)
        {
            std::string result(boost::lexical_cast<std::string>(count));
            count = boost::lexical_cast<double>(result);
        }
        uint64_t timeMs(stopWatch.stop());
        LOG_DEBUG("After boost::lexical_cast floating point test");
        LOG_DEBUG("boost::lexical_cast floating point test took " << timeMs << "ms");
    }
}

void CStringUtilsTest::testUtf8ByteType(void)
{
    std::string testStr;
    // single byte UTF-8 character
    testStr += "a";
    // two byte UTF-8 character
    testStr += "é";
    // three byte UTF-8 character
    testStr += "中";
    // four byte UTF-8 character
    testStr += "𩸽";
    CPPUNIT_ASSERT_EQUAL(size_t(10), testStr.length());
    CPPUNIT_ASSERT_EQUAL(1, prelert::core::CStringUtils::utf8ByteType(testStr[0]));
    CPPUNIT_ASSERT_EQUAL(2, prelert::core::CStringUtils::utf8ByteType(testStr[1]));
    CPPUNIT_ASSERT_EQUAL(-1, prelert::core::CStringUtils::utf8ByteType(testStr[2]));
    CPPUNIT_ASSERT_EQUAL(3, prelert::core::CStringUtils::utf8ByteType(testStr[3]));
    CPPUNIT_ASSERT_EQUAL(-1, prelert::core::CStringUtils::utf8ByteType(testStr[4]));
    CPPUNIT_ASSERT_EQUAL(-1, prelert::core::CStringUtils::utf8ByteType(testStr[5]));
    CPPUNIT_ASSERT_EQUAL(4, prelert::core::CStringUtils::utf8ByteType(testStr[6]));
    CPPUNIT_ASSERT_EQUAL(-1, prelert::core::CStringUtils::utf8ByteType(testStr[7]));
    CPPUNIT_ASSERT_EQUAL(-1, prelert::core::CStringUtils::utf8ByteType(testStr[8]));
    CPPUNIT_ASSERT_EQUAL(-1, prelert::core::CStringUtils::utf8ByteType(testStr[9]));
}

