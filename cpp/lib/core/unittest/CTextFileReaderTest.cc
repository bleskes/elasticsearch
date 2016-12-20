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
#include "CTextFileReaderTest.h"

#include <core/CoreTypes.h>
#include <core/CTextFileReader.h>

#include <fstream>


CppUnit::Test *CTextFileReaderTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CTextFileReaderTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CTextFileReaderTest>(
                                   "CTextFileReaderTest::testRead",
                                   &CTextFileReaderTest::testRead) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTextFileReaderTest>(
                                   "CTextFileReaderTest::testReadBinary",
                                   &CTextFileReaderTest::testReadBinary) );

    return suiteOfTests;
}

void CTextFileReaderTest::testRead(void)
{
    std::string actual;

    // Here we're telling the file reader to translate CRLF to simply \n as
    // is the convention for strings stored within C/C++ programs
    prelert::core::CTextFileReader fileReader(true);
    CPPUNIT_ASSERT(fileReader.readFileToText(
                        "testfiles/CTextFileReaderTest.txt", actual));

    std::ifstream ifs("testfiles/CTextFileReaderTest.txt");
    CPPUNIT_ASSERT(ifs.is_open());
    std::string expected;

    std::string line;
    while (std::getline(ifs, line))
    {
        expected += line;
        expected += '\n';
    }

    CPPUNIT_ASSERT_EQUAL(expected, actual);
}

void CTextFileReaderTest::testReadBinary(void)
{
    // NB: This test relies on msysgit translating the linefeeds in the test
    // file to carriage return + linefeed on Windows machines.  This test may
    // need to be redesigned if we change to a source code control system that
    // doesn't do LF -> CRLF mapping automatically on Windows.

    std::string actual;

    // Here we're telling the file reader NOT to translate CRLF when it reads
    // the file
    prelert::core::CTextFileReader fileReader(false);
    CPPUNIT_ASSERT(fileReader.readFileToText(
                        "testfiles/CTextFileReaderTest.txt", actual));

    std::ifstream ifs("testfiles/CTextFileReaderTest.txt");
    CPPUNIT_ASSERT(ifs.is_open());
    std::string expected;

    std::string line;
    while (std::getline(ifs, line))
    {
        expected += line;
        expected += prelert::core_t::LINE_ENDING;
    }

    CPPUNIT_ASSERT_EQUAL(expected, actual);
}

