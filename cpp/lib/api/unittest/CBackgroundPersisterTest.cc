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
#include "CBackgroundPersisterTest.h"

#include <core/CoreTypes.h>
#include <core/COsFileFuncs.h>
#include <core/CStringUtils.h>

#include <model/CLimits.h>
#include <model/CModelConfig.h>

#include <api/CAnomalyDetector.h>
#include <api/CBackgroundPersister.h>
#include <api/CCsvInputParser.h>
#include <api/CFieldConfig.h>
#include <api/CJsonOutputWriter.h>
#include <api/CMultiFileDataAdder.h>

#include <test/CTestTmpDir.h>

#include <boost/bind.hpp>
#include <boost/filesystem.hpp>
#include <boost/ref.hpp>

#include <fstream>
#include <ios>
#include <iterator>
#include <string>
#include <vector>

namespace
{

typedef std::vector<std::string> TStrVec;

void reportPersistComplete(ml::core_t::TTime /*snapshotTimestamp*/,
                           const std::string &description,
                           const std::string &snapshotIdIn,
                           size_t numDocsIn,
                           std::string &snapshotIdOut,
                           size_t &numDocsOut)
{
    LOG_DEBUG("Persist complete with description: " << description);
    snapshotIdOut = snapshotIdIn;
    numDocsOut = numDocsIn;
}

}

CppUnit::Test *CBackgroundPersisterTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CBackgroundPersisterTest");
    suiteOfTests->addTest(new CppUnit::TestCaller<CBackgroundPersisterTest>(
                                   "CBackgroundPersisterTest::testDetectorPersistBy",
                                   &CBackgroundPersisterTest::testDetectorPersistBy) );
    suiteOfTests->addTest(new CppUnit::TestCaller<CBackgroundPersisterTest>(
                                   "CBackgroundPersisterTest::testDetectorPersistPartition",
                                   &CBackgroundPersisterTest::testDetectorPersistPartition) );

    return suiteOfTests;
}

void CBackgroundPersisterTest::testDetectorPersistBy(void)
{
    this->foregroundBackgroundComp("testfiles/new_mlfields.conf");
}

void CBackgroundPersisterTest::testDetectorPersistOver(void)
{
    this->foregroundBackgroundComp("testfiles/new_mlfields_over.conf");
}

void CBackgroundPersisterTest::testDetectorPersistPartition(void)
{
    this->foregroundBackgroundComp("testfiles/new_mlfields_partition.conf");
}

void CBackgroundPersisterTest::foregroundBackgroundComp(const std::string &configFileName)
{
    // Start by creating a detector with non-trivial state

    static const ml::core_t::TTime BUCKET_SIZE(3600);

    std::string inputFilename("testfiles/big_ascending.txt");

    // Open the input and output files
    std::ifstream inputStrm(inputFilename.c_str());
    CPPUNIT_ASSERT(inputStrm.is_open());

    std::ofstream outputStrm(ml::core::COsFileFuncs::NULL_FILENAME);
    CPPUNIT_ASSERT(outputStrm.is_open());

    ml::model::CLimits limits;
    ml::api::CFieldConfig fieldConfig;
    CPPUNIT_ASSERT(fieldConfig.initFromFile(configFileName));

    ml::model::CModelConfig modelConfig =
            ml::model::CModelConfig::defaultConfig(BUCKET_SIZE);

    ml::api::CJsonOutputWriter outputWriter("job", outputStrm);

    std::string snapshotId;
    std::size_t numDocs(0);
    ml::api::CAnomalyDetector detector("job",
                                            limits,
                                            fieldConfig,
                                            modelConfig,
                                            outputWriter);
                                            boost::bind(&reportPersistComplete,
                                                        _1,
                                                        _2,
                                                        _3,
                                                        _4,
                                                        boost::ref(snapshotId),
                                                        boost::ref(numDocs));

    ml::api::CCsvInputParser parser(inputStrm);

    CPPUNIT_ASSERT(parser.readStream(false,
                                     boost::bind(&ml::api::CAnomalyDetector::handleSettings,
                                                 &detector,
                                                 _1),
                                     boost::bind(&ml::api::CAnomalyDetector::handleRecord,
                                                 &detector,
                                                 _1,
                                                 _2,
                                                 _3)));

    // Persist the detector state to files in the background
    std::string baseBackgroundOutputFilename(ml::test::CTestTmpDir::tmpDir() + "/background");
    {
        ml::api::CMultiFileDataAdder filePersister(baseBackgroundOutputFilename);
        ml::api::CBackgroundPersister backgroundPersister(filePersister);
        CPPUNIT_ASSERT(detector.backgroundPersistState(backgroundPersister));

        // Wait for the background persist to complete
        LOG_DEBUG("Before waiting for the background persister to be idle");
        CPPUNIT_ASSERT(backgroundPersister.waitForIdle());
        LOG_DEBUG("After waiting for the background persister to be idle");
    }

    TStrVec backgroundFileContents(numDocs);
    for (size_t index = 0; index < numDocs; ++index)
    {
        std::string expectedBackgroundFilename(baseBackgroundOutputFilename);
        expectedBackgroundFilename += '/';
        expectedBackgroundFilename += ml::api::CAnomalyDetector::STATE_TYPE;
        expectedBackgroundFilename += '/';
        expectedBackgroundFilename += snapshotId;
        expectedBackgroundFilename += '_';
        expectedBackgroundFilename += ml::core::CStringUtils::typeToString(1 + index);
        expectedBackgroundFilename += ml::api::CMultiFileDataAdder::JSON_FILE_EXT;
        LOG_DEBUG("Trying to open file: " << expectedBackgroundFilename);
        std::ifstream backgroundFile(expectedBackgroundFilename.c_str());
        CPPUNIT_ASSERT(backgroundFile.is_open());
        std::string state((std::istreambuf_iterator<char>(backgroundFile)),
                          std::istreambuf_iterator<char>());
        backgroundFileContents[index] = state;
    }

    // Now persist the detector in the foreground
    std::string baseForegroundOutputFilename(ml::test::CTestTmpDir::tmpDir() + "/foreground");
    {
        ml::api::CMultiFileDataAdder filePersister(baseForegroundOutputFilename);
        CPPUNIT_ASSERT(detector.persistState(filePersister));
    }

    TStrVec foregroundFileContents(numDocs);
    CPPUNIT_ASSERT_EQUAL(backgroundFileContents.size(), foregroundFileContents.size());
    for (size_t index = 0; index < numDocs; ++index)
    {
        std::string expectedForegroundFilename(baseForegroundOutputFilename);
        expectedForegroundFilename += '/';
        expectedForegroundFilename += ml::api::CAnomalyDetector::STATE_TYPE;
        expectedForegroundFilename += '/';
        expectedForegroundFilename += snapshotId;
        expectedForegroundFilename += '_';
        expectedForegroundFilename += ml::core::CStringUtils::typeToString(1 + index);
        expectedForegroundFilename += ml::api::CMultiFileDataAdder::JSON_FILE_EXT;
        std::ifstream foregroundFile(expectedForegroundFilename.c_str());
        CPPUNIT_ASSERT(foregroundFile.is_open());
        std::string state((std::istreambuf_iterator<char>(foregroundFile)),
                          std::istreambuf_iterator<char>());
        foregroundFileContents[index] = state;

        CPPUNIT_ASSERT_EQUAL(foregroundFileContents[index], backgroundFileContents[index]);
    }

    // Clean up
    boost::filesystem::path backgroundDir(baseBackgroundOutputFilename);
    CPPUNIT_ASSERT_NO_THROW(boost::filesystem::remove_all(backgroundDir));
    boost::filesystem::path foregroundDir(baseForegroundOutputFilename);
    CPPUNIT_ASSERT_NO_THROW(boost::filesystem::remove_all(foregroundDir));
}

