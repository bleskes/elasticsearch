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
#include "CLicenseValidatorTest.h"

#include <core/CLicenseValidator.h>
#include <core/CProcess.h>
#include <core/CStringUtils.h>


CppUnit::Test *CLicenseValidatorTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CLicenseValidatorTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CLicenseValidatorTest>(
                                   "CLicenseValidatorTest::testValidate",
                                   &CLicenseValidatorTest::testValidate) );

    return suiteOfTests;
}

void CLicenseValidatorTest::testValidate(void)
{
    ml::core::CProcess::TPid ppid(ml::core::CProcess::instance().parentId());

    CPPUNIT_ASSERT(ml::core::CLicenseValidator::validate(ml::core::CStringUtils::typeToString(ppid)));
    CPPUNIT_ASSERT(!ml::core::CLicenseValidator::validate(ml::core::CStringUtils::typeToString(ppid + 1)));
    CPPUNIT_ASSERT(!ml::core::CLicenseValidator::validate(ml::core::CStringUtils::typeToString(ppid - 1)));
    CPPUNIT_ASSERT(ml::core::CLicenseValidator::validate(ml::core::CStringUtils::typeToString(ppid + 926213)));
    CPPUNIT_ASSERT(ml::core::CLicenseValidator::validate(ml::core::CStringUtils::typeToString(ppid + 77 * 926213)));
    CPPUNIT_ASSERT(!ml::core::CLicenseValidator::validate(""));
    CPPUNIT_ASSERT(!ml::core::CLicenseValidator::validate(" "));
    CPPUNIT_ASSERT(!ml::core::CLicenseValidator::validate("0"));
    CPPUNIT_ASSERT(!ml::core::CLicenseValidator::validate("a"));
    CPPUNIT_ASSERT(!ml::core::CLicenseValidator::validate(ml::core::CStringUtils::typeToString(ppid) + "a"));
}

