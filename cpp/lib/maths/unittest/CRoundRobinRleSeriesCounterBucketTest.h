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
#ifndef INCLUDED_CRoundRobinRleSeriesCounterBucketTest_h
#define INCLUDED_CRoundRobinRleSeriesCounterBucketTest_h

#include <cppunit/extensions/HelperMacros.h>


class CRoundRobinRleSeriesCounterBucketTest : public CppUnit::TestFixture
{
    public:
        void testRle1(void);
        void testRle2(void);
        void testRle3(void);
        void testRle4(void);
        void testRle5(void);
        void testRle4_10(void);
        void testInit(void);

        static CppUnit::Test *suite();
};

#endif // INCLUDED_CRoundRobinRleSeriesCounterBucketTest_h

