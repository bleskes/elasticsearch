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
#include "CNamedPipeFactoryTest.h"

#include <core/CNamedPipeFactory.h>
#include <core/CSleep.h>
#include <core/CThread.h>

#include <fstream>

#include <stdio.h>


namespace
{

const uint32_t SLEEP_TIME_MS = 100;
const uint32_t PAUSE_TIME_MS = 10;
const size_t   MAX_ATTEMPTS = 100;
const size_t   TEST_SIZE = 10000;
const char     TEST_CHAR = 'a';
#ifdef Windows
const char     *TEST_PIPE_NAME = "\\\\.\\pipe\\testpipe";
#else
const char     *TEST_PIPE_NAME = "testfiles/testpipe";
#endif

class CThreadDataWriter : public prelert::core::CThread
{
    public:
        CThreadDataWriter(const std::string &fileName, size_t size)
            : m_FileName(fileName),
              m_Size(size)
        {
        }

    protected:
        virtual void run(void)
        {
            // Wait for the file to exist
            prelert::core::CSleep::sleep(SLEEP_TIME_MS);

            std::ofstream strm(m_FileName.c_str());
            for (size_t i = 0; i < m_Size && strm.good(); ++i)
            {
                strm << TEST_CHAR;
            }
        }

        virtual void shutdown(void)
        {
        }

    private:
        std::string m_FileName;
        size_t      m_Size;
};

class CThreadDataReader : public prelert::core::CThread
{
    public:
        CThreadDataReader(const std::string &fileName)
            : m_FileName(fileName)
        {
        }

        const std::string &data(void) const
        {
            return m_Data;
        }

    protected:
        virtual void run(void)
        {
            m_Data.clear();

            std::ifstream strm;

            // Try to open the file repeatedly to allow time for the other
            // thread to create it
            size_t attempt(1);
            do
            {
                CPPUNIT_ASSERT(attempt++ <= MAX_ATTEMPTS);
                prelert::core::CSleep::sleep(PAUSE_TIME_MS);
                strm.open(m_FileName.c_str());
            }
            while (!strm.is_open());

            static const std::streamsize BUF_SIZE = 512;
            char buffer[BUF_SIZE];
            while (strm.good())
            {
                strm.read(buffer, BUF_SIZE);
                CPPUNIT_ASSERT(!strm.bad());
                if (strm.gcount() > 0)
                {
                    // This code deals with the test character we write to
                    // detect the short-lived connection problem on Windows
                    const char *copyFrom = buffer;
                    size_t copyLen = static_cast<size_t>(strm.gcount());
                    if (m_Data.empty() &&
                        *buffer == prelert::core::CNamedPipeFactory::TEST_CHAR)
                    {
                        ++copyFrom;
                        --copyLen;
                    }
                    if (copyLen > 0)
                    {
                        m_Data.append(copyFrom, copyLen);
                    }
                }
            }
        }

        virtual void shutdown(void)
        {
        }

    private:
        std::string m_FileName;
        std::string m_Data;
};

class CThreadBlockCanceller : public prelert::core::CThread
{
    public:
        CThreadBlockCanceller(prelert::core::CThread::TThreadId threadId)
            : m_ThreadId(threadId)
        {
        }

    protected:
        virtual void run(void)
        {
            // Wait for the file to exist
            prelert::core::CSleep::sleep(SLEEP_TIME_MS);

            // Cancel the open() or read() operation on the file
            CPPUNIT_ASSERT(prelert::core::CThread::cancelBlockedIo(m_ThreadId));
        }

        virtual void shutdown(void)
        {
        }

    private:
        prelert::core::CThread::TThreadId m_ThreadId;
};

}

CppUnit::Test *CNamedPipeFactoryTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CNamedPipeFactoryTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CNamedPipeFactoryTest>(
                                   "CNamedPipeFactoryTest::testServerIsCppReader",
                                   &CNamedPipeFactoryTest::testServerIsCppReader) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CNamedPipeFactoryTest>(
                                   "CNamedPipeFactoryTest::testServerIsCReader",
                                   &CNamedPipeFactoryTest::testServerIsCReader) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CNamedPipeFactoryTest>(
                                   "CNamedPipeFactoryTest::testServerIsCppWriter",
                                   &CNamedPipeFactoryTest::testServerIsCppWriter) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CNamedPipeFactoryTest>(
                                   "CNamedPipeFactoryTest::testServerIsCWriter",
                                   &CNamedPipeFactoryTest::testServerIsCWriter) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CNamedPipeFactoryTest>(
                                   "CNamedPipeFactoryTest::testCancelBlock",
                                   &CNamedPipeFactoryTest::testCancelBlock) );

    return suiteOfTests;
}

void CNamedPipeFactoryTest::testServerIsCppReader(void)
{
    CThreadDataWriter threadWriter(TEST_PIPE_NAME, TEST_SIZE);
    CPPUNIT_ASSERT(threadWriter.start());

    prelert::core::CNamedPipeFactory::TIStreamP strm = prelert::core::CNamedPipeFactory::openPipeStreamRead(TEST_PIPE_NAME);
    CPPUNIT_ASSERT(strm);

    static const std::streamsize BUF_SIZE = 512;
    std::string readData;
    char buffer[BUF_SIZE];
    do
    {
        strm->read(buffer, BUF_SIZE);
        CPPUNIT_ASSERT(!strm->bad());
        if (strm->gcount() > 0)
        {
            readData.append(buffer, static_cast<size_t>(strm->gcount()));
        }
    }
    while (!strm->eof());

    CPPUNIT_ASSERT_EQUAL(TEST_SIZE, readData.length());
    CPPUNIT_ASSERT_EQUAL(std::string(TEST_SIZE, TEST_CHAR), readData);

    CPPUNIT_ASSERT(threadWriter.stop());

    strm.reset();
}

void CNamedPipeFactoryTest::testServerIsCReader(void)
{
    CThreadDataWriter threadWriter(TEST_PIPE_NAME, TEST_SIZE);
    CPPUNIT_ASSERT(threadWriter.start());

    prelert::core::CNamedPipeFactory::TFileP file = prelert::core::CNamedPipeFactory::openPipeFileRead(TEST_PIPE_NAME);
    CPPUNIT_ASSERT(file);

    static const size_t BUF_SIZE = 512;
    std::string readData;
    char buffer[BUF_SIZE];
    do
    {
        size_t charsRead = ::fread(buffer, sizeof(char), BUF_SIZE, file.get());
        CPPUNIT_ASSERT(!::ferror(file.get()));
        if (charsRead > 0)
        {
            readData.append(buffer, charsRead);
        }
    }
    while (!::feof(file.get()));

    CPPUNIT_ASSERT_EQUAL(TEST_SIZE, readData.length());
    CPPUNIT_ASSERT_EQUAL(std::string(TEST_SIZE, TEST_CHAR), readData);

    CPPUNIT_ASSERT(threadWriter.stop());

    file.reset();
}

void CNamedPipeFactoryTest::testServerIsCppWriter(void)
{
    CThreadDataReader threadReader(TEST_PIPE_NAME);
    CPPUNIT_ASSERT(threadReader.start());

    prelert::core::CNamedPipeFactory::TOStreamP strm = prelert::core::CNamedPipeFactory::openPipeStreamWrite(TEST_PIPE_NAME);
    CPPUNIT_ASSERT(strm);

    size_t charsLeft(TEST_SIZE);
    size_t blockSize(7);
    while (charsLeft > 0)
    {
        if (blockSize > charsLeft)
        {
            blockSize = charsLeft;
        }
        (*strm) << std::string(blockSize, TEST_CHAR);
        CPPUNIT_ASSERT(!strm->bad());
        charsLeft -= blockSize;
    }

    strm.reset();

    CPPUNIT_ASSERT(threadReader.stop());

    CPPUNIT_ASSERT_EQUAL(TEST_SIZE, threadReader.data().length());
    CPPUNIT_ASSERT_EQUAL(std::string(TEST_SIZE, TEST_CHAR), threadReader.data());
}

void CNamedPipeFactoryTest::testServerIsCWriter(void)
{
    CThreadDataReader threadReader(TEST_PIPE_NAME);
    CPPUNIT_ASSERT(threadReader.start());

    prelert::core::CNamedPipeFactory::TFileP file = prelert::core::CNamedPipeFactory::openPipeFileWrite(TEST_PIPE_NAME);
    CPPUNIT_ASSERT(file);

    size_t charsLeft(TEST_SIZE);
    size_t blockSize(7);
    while (charsLeft > 0)
    {
        if (blockSize > charsLeft)
        {
            blockSize = charsLeft;
        }
        CPPUNIT_ASSERT(::fputs(std::string(blockSize, TEST_CHAR).c_str(), file.get()) >= 0);
        charsLeft -= blockSize;
    }

    file.reset();

    CPPUNIT_ASSERT(threadReader.stop());

    CPPUNIT_ASSERT_EQUAL(TEST_SIZE, threadReader.data().length());
    CPPUNIT_ASSERT_EQUAL(std::string(TEST_SIZE, TEST_CHAR), threadReader.data());
}

void CNamedPipeFactoryTest::testCancelBlock(void)
{
    CThreadBlockCanceller cancellerThread(prelert::core::CThread::currentThreadId());
    CPPUNIT_ASSERT(cancellerThread.start());

    prelert::core::CNamedPipeFactory::TOStreamP strm = prelert::core::CNamedPipeFactory::openPipeStreamWrite(TEST_PIPE_NAME);
    CPPUNIT_ASSERT(strm == 0);

    CPPUNIT_ASSERT(cancellerThread.stop());
}

