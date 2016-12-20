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

#ifndef INCLUDED_CMetricModelTest_h
#define INCLUDED_CMetricModelTest_h

#include <cppunit/extensions/HelperMacros.h>

class CMetricModelTest : public CppUnit::TestFixture
{
    public:
        void testOnlineSample(void);
        void testOnlineMultivariateSample(void);
        void testOnlineProbabilityCalculationForMetric(void);
        void testOnlineProbabilityCalculationForMedian(void);
        void testOnlineProbabilityCalculationForLowMean(void);
        void testOnlineProbabilityCalculationForHighMean(void);
        void testOnlineProbabilityCalculationForLowSum(void);
        void testOnlineProbabilityCalculationForHighSum(void);
        void testOnlineProbabilityCalculationForLatLong(void);
        void testMinInfluence(void);
        void testMeanInfluence(void);
        void testMaxInfluence(void);
        void testSumInfluence(void);
        void testLatLongInfluence(void);
        void testPrune(void);
        void testSkipSampling(void);
        void testExplicitNulls(void);
        void testKey(void);
        void testVarp(void);
        void testInterimCorrections(void);
        void testInterimCorrectionsWithCorrelations(void);
        void testCorrelatePersist(void);
        void testSummaryCountZeroRecordsAreIgnored(void);
        void testDecayRateControl(void);

        static CppUnit::Test *suite(void);
};

#endif // INCLUDED_CMetricModelTest_h
