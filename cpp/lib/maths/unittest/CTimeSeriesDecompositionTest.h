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

#ifndef INCLUDED_CTimeSeriesDecompositionTest_h
#define INCLUDED_CTimeSeriesDecompositionTest_h

#include <cppunit/extensions/HelperMacros.h>

namespace prelert
{
namespace maths
{
class CTimeSeriesDecomposition;
}
}

class CTimeSeriesDecompositionTest : public CppUnit::TestFixture
{
    public:
        void testSuperpositionOfSines(void);
        void testBlueCoat(void);
        void testMinimizeSlope(void);
        void testWeekend(void);
        void testSinglePeriodicity(void);
        void testSeasonalOnset(void);
        void testVarianceScale(void);
        void testEmcProblemCase(void);
        void testBlueCoatProblemCase(void);
        void testUMichProblemCase(void);
        void testShift(void);
        void testSwap(void);
        void testPersist(void);

        static CppUnit::Test *suite(void);
};

#endif // INCLUDED_CTimeSeriesDecompositionTest_h
