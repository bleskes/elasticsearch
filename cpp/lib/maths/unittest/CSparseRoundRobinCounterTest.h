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
#ifndef INCLUDED_CSparseRoundRobinCounterTest_h
#define INCLUDED_CSparseRoundRobinCounterTest_h

#include <cppunit/extensions/HelperMacros.h>


class CSparseRoundRobinCounterTest : public CppUnit::TestFixture
{
    public:
        void testTotalCount(void);
        void testCountFromTime(void);
        void testLarge(void);
        void testNotificationBursts(void);
        void testNotificationBursts2(void);
        void testNotificationBurstsLarge(void);
        void testNotificationBurstsLarge2(void);
        void testRoundIntervalDuplication(void);

        static CppUnit::Test *suite();
};

#endif // INCLUDED_CSparseRoundRobinCounterTest_h

