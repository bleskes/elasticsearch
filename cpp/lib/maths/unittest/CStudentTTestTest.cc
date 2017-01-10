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
#include "CStudentTTestTest.h"

#include <core/CLogger.h>

#include <maths/CBasicStatistics.h>
#include <maths/CStudentTTest.h>

#include <iostream>


CppUnit::Test   *CStudentTTestTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CStudentTTestTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CStudentTTestTest>(
                                   "CStudentTTestTest::testSamples",
                                   &CStudentTTestTest::testSamples) );

    return suiteOfTests;
}

void    CStudentTTestTest::testSamples(void)
{

{
    double  sample[] = { 0,0,0,0,0,0,1,0,0,0,0,0,0 };

    ml::maths::CStudentTTest::TDoubleVec sampleVec(sample, sample+sizeof(sample)/sizeof(sample[0]));

    double  probability(0.0);

    CPPUNIT_ASSERT(ml::maths::CStudentTTest::oneSampleTest(sampleVec, probability));

    LOG_DEBUG(probability);
}

}
