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
#include "COsFileFuncsTest.h"

#include <core/CLogger.h>
#include <core/COsFileFuncs.h>

#include <string.h>


CppUnit::Test *COsFileFuncsTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("COsFileFuncsTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<COsFileFuncsTest>(
                                   "COsFileFuncsTest::testInode",
                                   &COsFileFuncsTest::testInode) );

    return suiteOfTests;
}

void COsFileFuncsTest::testInode(void)
{
    // Windows doesn't have inodes as such, but on NTFS we can simulate a number
    // that fulfils the purpose of determining when a file has been renamed and
    // another one with the original name has been created.

    ml::core::COsFileFuncs::TStat statBuf;

    std::string headerFile("COsFileFuncsTest.h");
    std::string implFile("COsFileFuncsTest.cc");

    ::memset(&statBuf, 0, sizeof(statBuf));
    ino_t headerDirect(0);
    CPPUNIT_ASSERT_EQUAL(0, ml::core::COsFileFuncs::stat(headerFile.c_str(),
                                                              &statBuf));
    headerDirect = statBuf.st_ino;
    LOG_DEBUG("Inode for " << headerFile << " from directory is " <<
              headerDirect);

    ::memset(&statBuf, 0, sizeof(statBuf));
    ino_t headerOpen(0);
    int headerFd(ml::core::COsFileFuncs::open(headerFile.c_str(),
                                                   ml::core::COsFileFuncs::RDONLY));
    CPPUNIT_ASSERT(headerFd != -1);
    CPPUNIT_ASSERT_EQUAL(0, ml::core::COsFileFuncs::fstat(headerFd,
                                                               &statBuf));
    CPPUNIT_ASSERT_EQUAL(0, ml::core::COsFileFuncs::close(headerFd));
    headerOpen = statBuf.st_ino;
    LOG_DEBUG("Inode for " << headerFile << " from open file is " <<
              headerOpen);

    ::memset(&statBuf, 0, sizeof(statBuf));
    ino_t implDirect(0);
    CPPUNIT_ASSERT_EQUAL(0, ml::core::COsFileFuncs::stat(implFile.c_str(),
                                                              &statBuf));
    implDirect = statBuf.st_ino;
    LOG_DEBUG("Inode for " << implFile << " from directory is " <<
              implDirect);

    ::memset(&statBuf, 0, sizeof(statBuf));
    ino_t implOpen(0);
    int implFd(ml::core::COsFileFuncs::open(implFile.c_str(),
                                                 ml::core::COsFileFuncs::RDONLY));
    CPPUNIT_ASSERT(implFd != -1);
    CPPUNIT_ASSERT_EQUAL(0, ml::core::COsFileFuncs::fstat(implFd,
                                                               &statBuf));
    CPPUNIT_ASSERT_EQUAL(0, ml::core::COsFileFuncs::close(implFd));
    implOpen = statBuf.st_ino;
    LOG_DEBUG("Inode for " << implFile << " from open file is " <<
              implOpen);

    CPPUNIT_ASSERT_EQUAL(headerDirect, headerOpen);
    CPPUNIT_ASSERT_EQUAL(implDirect, implOpen);
    CPPUNIT_ASSERT(implDirect != headerDirect);
}

