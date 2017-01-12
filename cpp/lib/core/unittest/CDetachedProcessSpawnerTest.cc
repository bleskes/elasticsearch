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
#include "CDetachedProcessSpawnerTest.h"

#include <core/CDetachedProcessSpawner.h>
#include <core/COsFileFuncs.h>
#include <core/CSleep.h>
#include <core/CStringUtils.h>

#include <boost/range.hpp>

#include <stdio.h>
#include <stdlib.h>


namespace
{
const std::string OUTPUT_FILE("withNs.xml");
#ifdef Windows
// Unlike Windows NT system calls, copy's command line cannot cope with
// forward slash path separators
const std::string INPUT_FILE("testfiles\\withNs.xml");
// File size is different on Windows due to CRLF line endings
const size_t EXPECTED_FILE_SIZE(30463);
const char *winDir(::getenv("windir"));
const std::string PROCESS_PATH(winDir != 0 ? std::string(winDir) + "\\System32\\cmd" : std::string("C:\\Windows\\System32\\cmd"));
const std::string PROCESS_ARGS[] = { "/C",
                                     "copy " + INPUT_FILE + " ." };
#else
const std::string INPUT_FILE("testfiles/withNs.xml");
const size_t EXPECTED_FILE_SIZE(29770);
const std::string PROCESS_PATH("/bin/dd");
const std::string PROCESS_ARGS[] = { "if=" + INPUT_FILE,
                                     "of=" + OUTPUT_FILE,
                                     "bs=1",
                                     "count=" + ml::core::CStringUtils::typeToString(EXPECTED_FILE_SIZE) };
#endif
}

CppUnit::Test *CDetachedProcessSpawnerTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CDetachedProcessSpawnerTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CDetachedProcessSpawnerTest>(
                                   "CDetachedProcessSpawnerTest::testSpawn",
                                   &CDetachedProcessSpawnerTest::testSpawn) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CDetachedProcessSpawnerTest>(
                                   "CDetachedProcessSpawnerTest::testPermitted",
                                   &CDetachedProcessSpawnerTest::testPermitted) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CDetachedProcessSpawnerTest>(
                                   "CDetachedProcessSpawnerTest::testNonExistent",
                                   &CDetachedProcessSpawnerTest::testNonExistent) );

    return suiteOfTests;
}

void CDetachedProcessSpawnerTest::testSpawn(void)
{
    // The intention of this test is to copy a file by spawning an external
    // program and then make sure the file has been copied

    // Remove any output file left behind by a previous failed test, but don't
    // check the return code as this will usually fail
    ::remove(OUTPUT_FILE.c_str());

    ml::core::CDetachedProcessSpawner::TStrVec permittedPaths(1, PROCESS_PATH);
    ml::core::CDetachedProcessSpawner spawner(permittedPaths);

    ml::core::CDetachedProcessSpawner::TStrVec args(PROCESS_ARGS, PROCESS_ARGS + boost::size(PROCESS_ARGS));

    CPPUNIT_ASSERT(spawner.spawn(PROCESS_PATH, args));

    // Expect the copy to complete in less than 1 second
    ml::core::CSleep::sleep(1000);

    ml::core::COsFileFuncs::TStat statBuf;
    CPPUNIT_ASSERT_EQUAL(0, ml::core::COsFileFuncs::stat(OUTPUT_FILE.c_str(), &statBuf));
    CPPUNIT_ASSERT_EQUAL(EXPECTED_FILE_SIZE, static_cast<size_t>(statBuf.st_size));

    CPPUNIT_ASSERT_EQUAL(0, ::remove(OUTPUT_FILE.c_str()));
}

void CDetachedProcessSpawnerTest::testPermitted(void)
{
    ml::core::CDetachedProcessSpawner::TStrVec permittedPaths(1, PROCESS_PATH);
    ml::core::CDetachedProcessSpawner spawner(permittedPaths);

    // Should fail as ml_test is not on the permitted processes list
    CPPUNIT_ASSERT(!spawner.spawn("./ml_test", ml::core::CDetachedProcessSpawner::TStrVec()));
}

void CDetachedProcessSpawnerTest::testNonExistent(void)
{
    ml::core::CDetachedProcessSpawner::TStrVec permittedPaths(1, "./does_not_exist");
    ml::core::CDetachedProcessSpawner spawner(permittedPaths);

    // Should fail as even though it's a permitted process as the file doesn't exist
    CPPUNIT_ASSERT(!spawner.spawn("./does_not_exist", ml::core::CDetachedProcessSpawner::TStrVec()));
}

