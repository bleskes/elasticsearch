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
#ifndef INCLUDED_CBasicStatisticsTest_h
#define INCLUDED_CBasicStatisticsTest_h

#include <cppunit/extensions/HelperMacros.h>


class CBasicStatisticsTest : public CppUnit::TestFixture
{
    public:
        void testMean(void);
        void testVmr(void);
        void testSd(void);
        void testCentralMoments(void);
        void testCovariances(void);
        void testCovariancesLedoitWolf(void);
        void testMedian(void);
        void testQuartiles(void);
        void testQuartileRelationship(void);
        void testOrderStatistics(void);

        static CppUnit::Test *suite(void);
};

#endif // INCLUDED_CBasicStatisticsTest_h

