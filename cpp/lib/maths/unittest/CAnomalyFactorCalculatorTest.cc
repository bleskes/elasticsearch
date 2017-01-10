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
#include "CAnomalyFactorCalculatorTest.h"

#include <maths/CAnomalyFactorCalculator.h>


CppUnit::Test *CAnomalyFactorCalculatorTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CAnomalyFactorCalculatorTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CAnomalyFactorCalculatorTest>(
                                   "CAnomalyFactorCalculatorTest::testFromProb",
                                   &CAnomalyFactorCalculatorTest::testFromProb) );

    return suiteOfTests;
}

void CAnomalyFactorCalculatorTest::testFromProb(void)
{
    CPPUNIT_ASSERT_EQUAL(int32_t(0), ml::maths::CAnomalyFactorCalculator::fromDistributionProb(0.5));
    CPPUNIT_ASSERT_EQUAL(int32_t(25), ml::maths::CAnomalyFactorCalculator::fromDistributionProb(1.0e-78));
    CPPUNIT_ASSERT_EQUAL(int32_t(50), ml::maths::CAnomalyFactorCalculator::fromDistributionProb(1.0e-154));
    CPPUNIT_ASSERT_EQUAL(int32_t(75), ml::maths::CAnomalyFactorCalculator::fromDistributionProb(1.0e-231));
    CPPUNIT_ASSERT_EQUAL(int32_t(100), ml::maths::CAnomalyFactorCalculator::fromDistributionProb(1.0e-308));

    // Special case
    CPPUNIT_ASSERT_EQUAL(int32_t(100), ml::maths::CAnomalyFactorCalculator::fromDistributionProb(0.0));

    // These are errors
    CPPUNIT_ASSERT_EQUAL(int32_t(0), ml::maths::CAnomalyFactorCalculator::fromDistributionProb(-0.1));
    CPPUNIT_ASSERT_EQUAL(int32_t(0), ml::maths::CAnomalyFactorCalculator::fromDistributionProb(1.1));
}

