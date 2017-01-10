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
#include "CChiSquaredTestTest.h"

#include <core/CLogger.h>

#include <maths/CBasicStatistics.h>
#include <maths/CChiSquaredTest.h>

#include <iostream>


CppUnit::Test   *CChiSquaredTestTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CChiSquaredTestTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CChiSquaredTestTest>(
                                   "CChiSquaredTestTest::testSamples",
                                   &CChiSquaredTestTest::testSamples) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CChiSquaredTestTest>(
                                   "CChiSquaredTestTest::testBadSamples",
                                   &CChiSquaredTestTest::testBadSamples) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CChiSquaredTestTest>(
                                   "CChiSquaredTestTest::testEdgeCases",
                                   &CChiSquaredTestTest::testEdgeCases) );

    return suiteOfTests;
}

void    CChiSquaredTestTest::testSamples(void)
{

{
/*
> p
[1] 9.714286 9.714286 9.714286 9.714286 9.714286 9.714286 9.714286
> x<-c(9,9,10,8,9,15,8)
> x
[1]  9  9 10  8  9 15  8
> chisq.test(x,p=p,rescale.p=TRUE)

    Chi-squared test for given probabilities

data:  x 
X-squared = 3.6471, df = 6, p-value = 0.7243
*/
    double  sample[] = { 9, 9, 10, 8, 9, 15, 8 };

    ml::maths::CChiSquaredTest::TDoubleVec sampleVec(sample, sample+sizeof(sample)/sizeof(sample[0]));

    double  probability(0.0);

    CPPUNIT_ASSERT(ml::maths::CChiSquaredTest::oneSampleTestVsMean(sampleVec, probability));

    CPPUNIT_ASSERT_DOUBLES_EQUAL(probability, double(0.724312), double(0.000001));
}
{
/*
> x<-c(9,9,10,8,9,10,8)
> p=array(mean(x),dim=c(7))
> chisq.test(x,p=p,rescale.p=TRUE)

    Chi-squared test for given probabilities

data:  x 
X-squared = 0.4444, df = 6, p-value = 0.9985

*/
    double  sample[] = { 9, 9, 10, 8, 9, 10, 8 };

    ml::maths::CChiSquaredTest::TDoubleVec sampleVec(sample, sample+sizeof(sample)/sizeof(sample[0]));

    double  probability(0.0);

    CPPUNIT_ASSERT(ml::maths::CChiSquaredTest::oneSampleTestVsMean(sampleVec, probability));

    LOG_DEBUG(probability << " " << (1.0-probability)*100.0);

    CPPUNIT_ASSERT_DOUBLES_EQUAL(probability, double(0.9985), double(0.0001));
}

}


void    CChiSquaredTestTest::testBadSamples(void)
{

{
    ml::maths::CChiSquaredTest::TDoubleVec sampleVec;

    double  probability(0.0);

    CPPUNIT_ASSERT(!ml::maths::CChiSquaredTest::oneSampleTestVsMean(sampleVec, probability));
}
{
    double  sample[] = { 9 };

    ml::maths::CChiSquaredTest::TDoubleVec sampleVec(sample, sample+sizeof(sample)/sizeof(sample[0]));

    double  probability(0.0);

    CPPUNIT_ASSERT(!ml::maths::CChiSquaredTest::oneSampleTestVsMean(sampleVec, probability));
}
{
    double  sample[] = { 1, -1 };

    ml::maths::CChiSquaredTest::TDoubleVec sampleVec(sample, sample+sizeof(sample)/sizeof(sample[0]));

    double  probability(0.0);

    CPPUNIT_ASSERT(!ml::maths::CChiSquaredTest::oneSampleTestVsMean(sampleVec, probability));
}
{
    double  sample[] = { 2, 1, -1 };

    ml::maths::CChiSquaredTest::TDoubleVec sampleVec(sample, sample+sizeof(sample)/sizeof(sample[0]));

    double  probability(0.0);

    CPPUNIT_ASSERT(!ml::maths::CChiSquaredTest::oneSampleTestVsMean(sampleVec, probability));
}

}

void    CChiSquaredTestTest::testEdgeCases(void)
{

{
    double  sample[] = { 0, 0, 0, 0, 0 };

    ml::maths::CChiSquaredTest::TDoubleVec sampleVec(sample, sample+sizeof(sample)/sizeof(sample[0]));

    double  probability(0.0);

    CPPUNIT_ASSERT(ml::maths::CChiSquaredTest::oneSampleTestVsMean(sampleVec, probability));

    CPPUNIT_ASSERT_EQUAL(probability, 1.0);
}
{
    double  sample[] = { 0, 0, 0, 0.00001, 0 };

    ml::maths::CChiSquaredTest::TDoubleVec sampleVec(sample, sample+sizeof(sample)/sizeof(sample[0]));

    double  probability(0.0);

    CPPUNIT_ASSERT(ml::maths::CChiSquaredTest::oneSampleTestVsMean(sampleVec, probability));

    CPPUNIT_ASSERT_DOUBLES_EQUAL(probability, double(1.000000), double(0.000001));
}

}
