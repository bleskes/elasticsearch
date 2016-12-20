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
#ifndef INCLUDED_CTextFileWatcherTest_h
#define INCLUDED_CTextFileWatcherTest_h

#include <cppunit/extensions/HelperMacros.h>


//! These tests rely on msysgit translating the linefeeds in the test files
//! to carriage return + linefeed on Windows machines.  This test may need
//! to be redesigned if we change to a source code control system that
//! doesn't do LF -> CRLF mapping automatically on Windows.
class CTextFileWatcherTest : public CppUnit::TestFixture
{
    public:
        void testInitStart(void);
        void testInitEnd1(void);
        void testInitEnd2(void);
        void testReadAllLines1(void);
        void testReadAllLines2(void);
        void testMultiLineMessages(void);
        void testFileRename(void);

        static CppUnit::Test *suite();
};

#endif // INCLUDED_CTextFileWatcherTest_h

