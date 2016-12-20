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
#ifndef INCLUDED_CResultNormalizerTest_h
#define INCLUDED_CResultNormalizerTest_h

#include <cppunit/extensions/HelperMacros.h>


//! This test class uses the following input files:
//! 1) testfiles/sysChangeState.xml
//! 2) testfiles/unusualState.xml
//!
//! These are exported from Splunk for the results of a real-time analysis of
//! the Citizens Introscope data for a 3 day period in March 2011, so a
//! relatively realistic input data.
//!
class CResultNormalizerTest : public CppUnit::TestFixture
{
    public:
        void testInitNormalizer(void);

        static CppUnit::Test *suite();
};

#endif // INCLUDED_CResultNormalizerTest_h

