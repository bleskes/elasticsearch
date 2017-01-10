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
#include "CTermQuoterTest.h"

#include <api/CTermQuoter.h>


CppUnit::Test *CTermQuoterTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CTermQuoterTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CTermQuoterTest>(
                                   "CTermQuoterTest::testDoubleQuoting",
                                   &CTermQuoterTest::testDoubleQuoting) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTermQuoterTest>(
                                   "CTermQuoterTest::testSingleQuoting",
                                   &CTermQuoterTest::testSingleQuoting) );

    return suiteOfTests;
}

void CTermQuoterTest::testDoubleQuoting(void)
{
    CPPUNIT_ASSERT_EQUAL(std::string("\"\""), ml::api::CTermQuoter::doubleQuote(""));

    CPPUNIT_ASSERT_EQUAL(std::string("simple"), ml::api::CTermQuoter::doubleQuote("simple"));

    CPPUNIT_ASSERT_EQUAL(std::string("\"complex\\\\\""), ml::api::CTermQuoter::doubleQuote("complex\\"));

    CPPUNIT_ASSERT_EQUAL(std::string("\"more \\\"complex\\\"| \""), ml::api::CTermQuoter::doubleQuote("more \"complex\"| "));
}

void CTermQuoterTest::testSingleQuoting(void)
{
    CPPUNIT_ASSERT_EQUAL(std::string("''"), ml::api::CTermQuoter::singleQuote(""));

    CPPUNIT_ASSERT_EQUAL(std::string("simple"), ml::api::CTermQuoter::singleQuote("simple"));

    CPPUNIT_ASSERT_EQUAL(std::string("'complex\\\\'"), ml::api::CTermQuoter::singleQuote("complex\\"));

    CPPUNIT_ASSERT_EQUAL(std::string("'more \"complex\"| '"), ml::api::CTermQuoter::singleQuote("more \"complex\"| "));
}

