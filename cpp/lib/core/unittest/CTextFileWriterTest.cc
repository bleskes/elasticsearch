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
#include "CTextFileWriterTest.h"

#include <core/CLogger.h>
#include <core/CoreTypes.h>
#include <core/COsFileFuncs.h>
#include <core/CTextFileWriter.h>

#include <test/CTestTmpDir.h>

#include <boost/shared_ptr.hpp>

#include <fstream>
#include <list>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>


CppUnit::Test *CTextFileWriterTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CTextFileWriterTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CTextFileWriterTest>(
                                   "CTextFileWriterTest::testInitNew",
                                   &CTextFileWriterTest::testInitNew) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTextFileWriterTest>(
                                   "CTextFileWriterTest::testInitStart",
                                   &CTextFileWriterTest::testInitStart) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTextFileWriterTest>(
                                   "CTextFileWriterTest::testInitEnd",
                                   &CTextFileWriterTest::testInitEnd) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTextFileWriterTest>(
                                   "CTextFileWriterTest::testFdLimit",
                                   &CTextFileWriterTest::testFdLimit) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTextFileWriterTest>(
                                   "CTextFileWriterTest::testWriteLineEndings",
                                   &CTextFileWriterTest::testWriteLineEndings) );

    return suiteOfTests;
}

void CTextFileWriterTest::setUp(void)
{
    std::string cmd("cp testfiles/CTextFileWriterTest.txt " + ml::test::CTestTmpDir::tmpDir());
    CPPUNIT_ASSERT_EQUAL(0, ::system(cmd.c_str()));
}

void CTextFileWriterTest::tearDown(void)
{
    std::string cmd("rm -f " + ml::test::CTestTmpDir::tmpDir() + "/CTextFileWriterTest.txt");
    CPPUNIT_ASSERT_EQUAL(0, ::system(cmd.c_str()));
}

void CTextFileWriterTest::testInitNew(void)
{
    std::string fileName(ml::test::CTestTmpDir::tmpDir() + "/CTextFileWriterTestNew.txt");
    {
        typedef std::vector<std::string> TStrVec;
        typedef TStrVec::const_iterator  TStrVecCItr;

        ml::core::CTextFileWriter writer(true);

        CPPUNIT_ASSERT(!writer.isOpen());

        CPPUNIT_ASSERT(writer.init(fileName,
                                   ml::core::CTextFileWriter::E_Start));

        CPPUNIT_ASSERT(writer.isOpen());

        writer.close();

        CPPUNIT_ASSERT(writer.init(fileName,
                                   ml::core::CTextFileWriter::E_Start));

        const std::string text[] = { "line1",
                                     "sdcsdac",
                                     "sdcsdccccdss\n",
                                     "sdcasdcsss112 1\n" };

        const TStrVec textVec(text, text+sizeof(text)/sizeof(text[0]));

        std::string expected;
        for (TStrVecCItr itr = textVec.begin(); itr != textVec.end(); ++itr)
        {
            CPPUNIT_ASSERT(writer.write(*itr));
            expected += (*itr);
        }

        std::ifstream ifs(fileName.c_str());
        CPPUNIT_ASSERT(ifs.is_open());
        std::string actual;

        std::string line;

        while (std::getline(ifs, line))
        {
            actual += line;
            actual += '\n';
        }

        CPPUNIT_ASSERT_EQUAL(expected, actual);

        // File will get closed here (otherwise the "rm" below will fail on
        // Windows)
    }

    std::string cmd("rm -f " + fileName);
    CPPUNIT_ASSERT_EQUAL(0, ::system(cmd.c_str()));
}

void CTextFileWriterTest::testInitStart(void)
{
    typedef std::vector<std::string> TStrVec;
    typedef TStrVec::const_iterator  TStrVecCItr;

    ml::core::CTextFileWriter writer(true);

    std::string fileName(ml::test::CTestTmpDir::tmpDir() + "/CTextFileWriterTest.txt");
    CPPUNIT_ASSERT(writer.init(fileName,
                               ml::core::CTextFileWriter::E_Start));

    const std::string text[] = { "line1",
                                 "sdcsdac",
                                 "sdcsdccccdss\n",
                                 "sdcasdcsss112 1\n" };

    const TStrVec textVec(text, text+sizeof(text)/sizeof(text[0]));

    std::string expected;
    for (TStrVecCItr itr = textVec.begin(); itr != textVec.end(); ++itr)
    {
        CPPUNIT_ASSERT(writer.write(*itr));
        expected += (*itr);
    }

    std::ifstream ifs(fileName.c_str());
    CPPUNIT_ASSERT(ifs.is_open());
    std::string actual;

    std::string line;

    while (std::getline(ifs, line))
    {
        actual += line;
        actual += '\n';
    }

    CPPUNIT_ASSERT_EQUAL(expected, actual);
}

void CTextFileWriterTest::testInitEnd(void)
{
    typedef std::vector<std::string> TStrVec;
    typedef TStrVec::const_iterator  TStrVecCItr;

    ml::core::CTextFileWriter writer(true);

    std::string fileName(ml::test::CTestTmpDir::tmpDir() + "/CTextFileWriterTest.txt");
    CPPUNIT_ASSERT(writer.init(fileName,
                               ml::core::CTextFileWriter::E_End));

    std::string actual;
    {
        std::ifstream ifs(fileName.c_str());
        CPPUNIT_ASSERT(ifs.is_open());

        std::string line;
        while (std::getline(ifs, line))
        {
            actual += line;
            actual += '\n';
        }
    }

    const std::string text[] = { "line1",
                                 "sdcsdac",
                                 "sdcsdccccdss\n",
                                 "sdcasdcsss112 1\n" };

    const TStrVec textVec(text, text+sizeof(text)/sizeof(text[0]));

    std::string expected;
    for (TStrVecCItr itr = textVec.begin(); itr != textVec.end(); ++itr)
    {
        CPPUNIT_ASSERT(writer.write(*itr));
        expected += (*itr);
    }

    std::string newActual;
    {
        std::ifstream ifs(fileName.c_str());

        CPPUNIT_ASSERT(ifs.is_open());

        std::string line;
        while (std::getline(ifs, line))
        {
            newActual += line;
            newActual += '\n';
        }
    }
    std::string::size_type pos = newActual.find(actual, 0);

    CPPUNIT_ASSERT(pos == 0);

    std::string newLines = newActual.substr(actual.size(), newActual.size());

    CPPUNIT_ASSERT_EQUAL(expected, newLines);
}

void CTextFileWriterTest::testFdLimit(void)
{
    std::string base(ml::test::CTestTmpDir::tmpDir() + "/ml_testFdLimit.");
    {
        typedef boost::shared_ptr<ml::core::CTextFileWriter> TTextFileWriterP;
        typedef std::list<TTextFileWriterP> TTextFileWriterPList;

        TTextFileWriterPList junk;

        for (int i = 0; i < 128; ++i)
        {
            std::ostringstream strm;

            strm << base << i;

            TTextFileWriterP writer(new ml::core::CTextFileWriter(false));

            CPPUNIT_ASSERT(writer->init(strm.str(),
                                    ml::core::CTextFileWriter::E_End));

            LOG_DEBUG("Opened " << strm.str());

            junk.push_back(writer);
        }

        // Files will get closed here (otherwise the "rm" below will fail on
        // Windows)
    }

    std::string cmd("rm -f " + base + '*');
    CPPUNIT_ASSERT_EQUAL(0, ::system(cmd.c_str()));
}

void CTextFileWriterTest::testWriteLineEndings(void)
{
    std::string testFile(ml::test::CTestTmpDir::tmpDir() + "/mlLineEndingTest.txt");

    size_t lineEndSize(::strlen(ml::core_t::LINE_ENDING));
    ml::core::COsFileFuncs::TStat statBuf;
    size_t fileSize(0);

    // 1 - binary mode, write line with one letter
    {
        ml::core::CTextFileWriter writer(false);
        CPPUNIT_ASSERT(writer.init(testFile,
                                   ml::core::CTextFileWriter::E_Start));
        CPPUNIT_ASSERT(writer.writeLine("a"));
    }

    CPPUNIT_ASSERT_EQUAL(0, ml::core::COsFileFuncs::stat(testFile.c_str(), &statBuf));
    fileSize = static_cast<size_t>(statBuf.st_size);
    CPPUNIT_ASSERT_EQUAL(size_t(1 + lineEndSize), fileSize);

    // 2 - text mode, write line with one letter
    {
        ml::core::CTextFileWriter writer(true);
        CPPUNIT_ASSERT(writer.init(testFile,
                                   ml::core::CTextFileWriter::E_Start));
        CPPUNIT_ASSERT(writer.writeLine("a"));
    }

    CPPUNIT_ASSERT_EQUAL(0, ml::core::COsFileFuncs::stat(testFile.c_str(), &statBuf));
    fileSize = static_cast<size_t>(statBuf.st_size);
    CPPUNIT_ASSERT_EQUAL(size_t(1 + lineEndSize), fileSize);

    // 3 - binary mode, write one letter
    {
        ml::core::CTextFileWriter writer(false);
        CPPUNIT_ASSERT(writer.init(testFile,
                                   ml::core::CTextFileWriter::E_Start));
        CPPUNIT_ASSERT(writer.write("a"));
    }

    CPPUNIT_ASSERT_EQUAL(0, ml::core::COsFileFuncs::stat(testFile.c_str(), &statBuf));
    fileSize = static_cast<size_t>(statBuf.st_size);
    CPPUNIT_ASSERT_EQUAL(size_t(1), fileSize);

    // 4 - text mode, write one letter
    {
        ml::core::CTextFileWriter writer(true);
        CPPUNIT_ASSERT(writer.init(testFile,
                                   ml::core::CTextFileWriter::E_Start));
        CPPUNIT_ASSERT(writer.write("a"));
    }

    CPPUNIT_ASSERT_EQUAL(0, ml::core::COsFileFuncs::stat(testFile.c_str(), &statBuf));
    fileSize = static_cast<size_t>(statBuf.st_size);
    CPPUNIT_ASSERT_EQUAL(size_t(1), fileSize);

    // 5 - binary mode, write a letter followed by a line feed
    {
        ml::core::CTextFileWriter writer(false);
        CPPUNIT_ASSERT(writer.init(testFile,
                                   ml::core::CTextFileWriter::E_Start));
        CPPUNIT_ASSERT(writer.write("a\n"));
    }

    CPPUNIT_ASSERT_EQUAL(0, ml::core::COsFileFuncs::stat(testFile.c_str(), &statBuf));
    fileSize = static_cast<size_t>(statBuf.st_size);
    // NB: here on Windows the file should still have a Unix line ending
    CPPUNIT_ASSERT_EQUAL(size_t(1 + 1), fileSize);

    // 6 - text mode, write a letter followed by a line feed
    {
        ml::core::CTextFileWriter writer(true);
        CPPUNIT_ASSERT(writer.init(testFile,
                                   ml::core::CTextFileWriter::E_Start));
        CPPUNIT_ASSERT(writer.write("a\n"));
    }

    CPPUNIT_ASSERT_EQUAL(0, ml::core::COsFileFuncs::stat(testFile.c_str(), &statBuf));
    fileSize = static_cast<size_t>(statBuf.st_size);
    CPPUNIT_ASSERT_EQUAL(size_t(1 + lineEndSize), fileSize);

    ::remove(testFile.c_str());
}

