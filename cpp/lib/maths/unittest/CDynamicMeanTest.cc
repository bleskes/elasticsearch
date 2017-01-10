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
#include "CDynamicMeanTest.h"

#include <core/CLogger.h>

#include <maths/CDynamicMean.h>


CppUnit::Test *CDynamicMeanTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CDynamicMeanTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CDynamicMeanTest>(
                                   "CDynamicMeanTest::testAll",
                                   &CDynamicMeanTest::testAll) );

    return suiteOfTests;
}

void CDynamicMeanTest::testAll(void)
{
    //double   sample[] = { 0.9, 10.0, 5.6, 1.23, -12.3, 445.2, 0.0, 1.2 };
    double   sample1[] = { 1,1,3,3,1,1,3,1,1,4,1,1,1,1,4,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2,5 };
    double   sample2[] = { 1,1,3,3,1,1,3,1,1,4,1,1,1,1,4,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2 };
    double   sample3[] = { 1,1,3,3,1,1,3,1,1,4,1,1,1,1,4,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1 };
    //double   sample[] = { 1,2,3,4,5,6,7,8,9,10 };

    typedef ml::maths::CDynamicMean<int> TIntMean;
    TIntMean v;

    double mean(0.0);
    double var(0.0);

    for (size_t i = 0; i < sizeof(sample1)/sizeof(double); ++i)
    {
        v.newData(static_cast<int>(sample1[i]), mean, var);
        //LOG_DEBUG(sample1[i] << " " << mean << " " << var << " " << var/mean);
    }

    double expectedMean(0.0);
    double expectedVar(0.0);

    TDoubleVec sampleVec1(sample1, sample1+sizeof(sample1)/sizeof(sample1[0]));
    CDynamicMeanTest::expectedValues(sampleVec1, expectedMean, expectedVar);
    CPPUNIT_ASSERT_DOUBLES_EQUAL(mean, expectedMean, double(0.0001));
    CPPUNIT_ASSERT_DOUBLES_EQUAL(var, expectedVar, double(0.0001));

    // Now take off the end values
    v.delData(5, mean, var);

    TDoubleVec sampleVec2(sample2, sample2+sizeof(sample2)/sizeof(sample2[0]));
    CDynamicMeanTest::expectedValues(sampleVec2, expectedMean, expectedVar);
    CPPUNIT_ASSERT_DOUBLES_EQUAL(mean, expectedMean, double(0.0001));
    CPPUNIT_ASSERT_DOUBLES_EQUAL(var, expectedVar, double(0.0001));

    v.delData(2, mean, var);

    TDoubleVec sampleVec3(sample3, sample3+sizeof(sample3)/sizeof(sample3[0]));
    CDynamicMeanTest::expectedValues(sampleVec3, expectedMean, expectedVar);
    CPPUNIT_ASSERT_DOUBLES_EQUAL(mean, expectedMean, double(0.0001));
    CPPUNIT_ASSERT_DOUBLES_EQUAL(var, expectedVar, double(0.0001));
}

void CDynamicMeanTest::expectedValues(const TDoubleVec &sample,
                                      double &mean,
                                      double &var)
{
    CPPUNIT_ASSERT(!sample.empty());

    double sum(0.0);

    for (TDoubleVecCItr itr = sample.begin(); itr != sample.end(); ++itr)
    {
        sum += (*itr);
    }

    mean = sum/double(sample.size());

    sum = 0.0;

    for (TDoubleVecCItr itr = sample.begin(); itr != sample.end(); ++itr)
    {
        double temp = (*itr) - mean;
        sum += temp * temp;
    }

    // Compute variance
    if (sample.size() > 1)
    {
        var = sum/double(sample.size()-1);
    }
    else
    {
        var = 0.0;
    }
}

