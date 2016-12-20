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
#include "CMultiFileDataAdderTest.h"

#include <core/CoreTypes.h>
#include <core/COsFileFuncs.h>
#include <core/CStringUtils.h>

#include <maths/CModelWeight.h>

#include <model/CLimits.h>
#include <model/CModelConfig.h>

#include <api/CAnomalyDetector.h>
#include <api/CCsvInputParser.h>
#include <api/CFieldConfig.h>
#include <api/CMultiFileDataAdder.h>
#include <api/CMultiFileSearcher.h>
#include <api/CJsonOutputWriter.h>

#include <test/CTestTmpDir.h>

#include <boost/bind.hpp>
#include <boost/filesystem.hpp>
#include <boost/ref.hpp>

#include <rapidjson/document.h>

#include <fstream>
#include <ios>
#include <iterator>
#include <string>
#include <vector>

namespace
{

typedef std::vector<std::string> TStrVec;

void reportPersistComplete(prelert::core_t::TTime /*snapshotTimestamp*/,
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

CppUnit::Test *CMultiFileDataAdderTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CMultiFileDataAdderTest");
    suiteOfTests->addTest(new CppUnit::TestCaller<CMultiFileDataAdderTest>(
                                   "CMultiFileDataAdderTest::testSimpleWrite",
                                   &CMultiFileDataAdderTest::testSimpleWrite) );
    suiteOfTests->addTest(new CppUnit::TestCaller<CMultiFileDataAdderTest>(
                                   "CMultiFileDataAdderTest::testDetectorPersistBy",
                                   &CMultiFileDataAdderTest::testDetectorPersistBy) );
    suiteOfTests->addTest(new CppUnit::TestCaller<CMultiFileDataAdderTest>(
                                   "CMultiFileDataAdderTest::testDetectorPersistOver",
                                   &CMultiFileDataAdderTest::testDetectorPersistOver) );
    suiteOfTests->addTest(new CppUnit::TestCaller<CMultiFileDataAdderTest>(
                                   "CMultiFileDataAdderTest::testDetectorPersistPartition",
                                   &CMultiFileDataAdderTest::testDetectorPersistPartition) );
    suiteOfTests->addTest(new CppUnit::TestCaller<CMultiFileDataAdderTest>(
                                   "CMultiFileDataAdderTest::testDetectorPersistDc",
                                   &CMultiFileDataAdderTest::testDetectorPersistDc) );
    suiteOfTests->addTest(new CppUnit::TestCaller<CMultiFileDataAdderTest>(
                                   "CMultiFileDataAdderTest::testDetectorPersistCount",
                                   &CMultiFileDataAdderTest::testDetectorPersistCount) );
    return suiteOfTests;
}

void CMultiFileDataAdderTest::testSimpleWrite(void)
{
    static const std::string EVENT("Hello Event");
    static const std::string SUMMARY_EVENT("Hello Summary Event");

    static const std::string EXTENSION(".txt");
    std::string baseOutputFilename(prelert::test::CTestTmpDir::tmpDir() + "/filepersister");

    std::string expectedFilename(baseOutputFilename);
    expectedFilename += "/hello/1";
    expectedFilename += EXTENSION;

    {
        // Clean up any leftovers of previous failures
        boost::filesystem::path workDir(baseOutputFilename);
        CPPUNIT_ASSERT_NO_THROW(boost::filesystem::remove_all(workDir));

        prelert::api::CMultiFileDataAdder persister(baseOutputFilename, EXTENSION);
        prelert::core::CDataAdder::TOStreamP strm = persister.addStreamed("", "hello", "1");
        CPPUNIT_ASSERT(strm);
        (*strm) << EVENT;
        CPPUNIT_ASSERT(persister.streamComplete(strm, true));
    }

    {
        std::ifstream persistedFile(expectedFilename.c_str());

        CPPUNIT_ASSERT(persistedFile.is_open());
        std::string line;
        std::getline(persistedFile, line);
        CPPUNIT_ASSERT_EQUAL(EVENT, line);
    }

    CPPUNIT_ASSERT_EQUAL(0, ::remove(expectedFilename.c_str()));

    expectedFilename = baseOutputFilename;
    expectedFilename += '/';
    expectedFilename += "stash";
    expectedFilename += "/1";
    expectedFilename += EXTENSION;

    {
        prelert::api::CMultiFileDataAdder persister(baseOutputFilename, EXTENSION);
        prelert::core::CDataAdder::TOStreamP strm = persister.addStreamed("", "stash", "1");
        CPPUNIT_ASSERT(strm);
        (*strm) << SUMMARY_EVENT;
        CPPUNIT_ASSERT(persister.streamComplete(strm, true));
    }

    {
        std::ifstream persistedFile(expectedFilename.c_str());

        CPPUNIT_ASSERT(persistedFile.is_open());
        std::string line;
        std::getline(persistedFile, line);
        CPPUNIT_ASSERT_EQUAL(SUMMARY_EVENT, line);
    }

    // Clean up
    boost::filesystem::path workDir(baseOutputFilename);
    CPPUNIT_ASSERT_NO_THROW(boost::filesystem::remove_all(workDir));
}

void CMultiFileDataAdderTest::testDetectorPersistBy(void)
{
    prelert::maths::CScopeDisableNormalizeOnRestore disabler;

    this->detectorPersistHelper("testfiles/new_prelertfields.conf",
                                "testfiles/big_ascending.txt",
                                0);
}

void CMultiFileDataAdderTest::testDetectorPersistOver(void)
{
    prelert::maths::CScopeDisableNormalizeOnRestore disabler;

    this->detectorPersistHelper("testfiles/new_prelertfields_over.conf",
                                "testfiles/big_ascending.txt",
                                0);
}

void CMultiFileDataAdderTest::testDetectorPersistPartition(void)
{
    prelert::maths::CScopeDisableNormalizeOnRestore disabler;

    this->detectorPersistHelper("testfiles/new_prelertfields_partition.conf",
                                "testfiles/big_ascending.txt",
                                0);
}

void CMultiFileDataAdderTest::testDetectorPersistDc(void)
{
    this->detectorPersistHelper("testfiles/new_persist_dc.conf",
                                "testfiles/files_users_programs.csv",
                                5);
}

void CMultiFileDataAdderTest::testDetectorPersistCount(void)
{
    this->detectorPersistHelper("testfiles/new_persist_count.conf",
                                "testfiles/files_users_programs.csv",
                                5);
}

void CMultiFileDataAdderTest::detectorPersistHelper(const std::string &configFileName,
                                                    const std::string &inputFilename,
                                                    int latencyBuckets)
{
    // Start by creating a detector with non-trivial state
    static const prelert::core_t::TTime BUCKET_SIZE(3600);
    static const std::string JOB_ID("job");

    // Open the input and output files
    std::ifstream inputStrm(inputFilename.c_str());
    CPPUNIT_ASSERT(inputStrm.is_open());

    std::ofstream outputStrm(prelert::core::COsFileFuncs::NULL_FILENAME);
    CPPUNIT_ASSERT(outputStrm.is_open());

    prelert::model::CLimits limits;
    prelert::api::CFieldConfig fieldConfig;
    CPPUNIT_ASSERT(fieldConfig.initFromFile(configFileName));

    prelert::model::CModelConfig modelConfig =
            prelert::model::CModelConfig::defaultConfig(BUCKET_SIZE,
                                                        prelert::model::CModelConfig::DEFAULT_BATCH_LENGTH,
                                                        prelert::model::CModelConfig::APERIODIC,
                                                        prelert::model_t::E_None,
                                                        "",
                                                        BUCKET_SIZE * latencyBuckets,
                                                        0,
                                                        false,
                                                        "");

    prelert::api::CJsonOutputWriter outputWriter(JOB_ID, outputStrm);

    std::string origSnapshotId;
    std::size_t numOrigDocs(0);
    prelert::api::CAnomalyDetector origDetector(JOB_ID,
                                                limits,
                                                fieldConfig,
                                                modelConfig,
                                                outputWriter,
                                                boost::bind(&reportPersistComplete,
                                                            _1,
                                                            _2,
                                                            _3,
                                                            _4,
                                                            boost::ref(origSnapshotId),
                                                            boost::ref(numOrigDocs)));

    prelert::api::CCsvInputParser parser(inputStrm);

    CPPUNIT_ASSERT(parser.readStream(false,
                                     boost::bind(&prelert::api::CAnomalyDetector::handleSettings,
                                                 &origDetector,
                                                 _1),
                                     boost::bind(&prelert::api::CAnomalyDetector::handleRecord,
                                                 &origDetector,
                                                 _1,
                                                 _2,
                                                 _3)));

    // Persist the detector state to file(s)

    std::string baseOrigOutputFilename(prelert::test::CTestTmpDir::tmpDir() + "/orig");
    {
        // Clean up any leftovers of previous failures
        boost::filesystem::path origDir(baseOrigOutputFilename);
        CPPUNIT_ASSERT_NO_THROW(boost::filesystem::remove_all(origDir));

        prelert::api::CMultiFileDataAdder persister(baseOrigOutputFilename);
        CPPUNIT_ASSERT(origDetector.persistState(persister));
    }

    std::string temp;
    TStrVec origFileContents(numOrigDocs);
    for (size_t index = 0; index < numOrigDocs; ++index)
    {
        std::string expectedOrigFilename(baseOrigOutputFilename);
        expectedOrigFilename += "/prelertresults-";
        expectedOrigFilename += JOB_ID;
        expectedOrigFilename += '/';
        expectedOrigFilename += prelert::api::CAnomalyDetector::STATE_TYPE;
        expectedOrigFilename += '/';
        expectedOrigFilename += prelert::core::CStringUtils::typeToString(1 + index);
        expectedOrigFilename += prelert::api::CMultiFileDataAdder::JSON_FILE_EXT;
        LOG_DEBUG("Trying to open file: " << expectedOrigFilename);
        std::ifstream origFile(expectedOrigFilename.c_str());
        CPPUNIT_ASSERT(origFile.is_open());
        std::string json((std::istreambuf_iterator<char>(origFile)),
                         std::istreambuf_iterator<char>());
        origFileContents[index] = json;

        // Ensure that the JSON is valid, by parsing string using Rapidjson
        rapidjson::Document document;
        CPPUNIT_ASSERT(!document.Parse<0>(origFileContents[index].c_str()).HasParseError());
        CPPUNIT_ASSERT(document.IsObject());
    }

    // Now restore the state into a different detector

    std::string restoredSnapshotId;
    std::size_t numRestoredDocs(0);
    prelert::api::CAnomalyDetector restoredDetector(JOB_ID,
                                                    limits,
                                                    fieldConfig,
                                                    modelConfig,
                                                    outputWriter,
                                                    boost::bind(&reportPersistComplete,
                                                                _1,
                                                                _2,
                                                                _3,
                                                                _4,
                                                                boost::ref(restoredSnapshotId),
                                                                boost::ref(numRestoredDocs)));

    {
        prelert::core_t::TTime completeToTime(0);

        prelert::api::CMultiFileSearcher retriever(baseOrigOutputFilename);
        CPPUNIT_ASSERT(restoredDetector.restoreState(retriever, completeToTime));
        CPPUNIT_ASSERT(completeToTime > 0);
    }

    // Finally, persist the new detector state to a file

    std::string baseRestoredOutputFilename(prelert::test::CTestTmpDir::tmpDir() + "/restored");
    {
        // Clean up any leftovers of previous failures
        boost::filesystem::path restoredDir(baseRestoredOutputFilename);
        CPPUNIT_ASSERT_NO_THROW(boost::filesystem::remove_all(restoredDir));

        prelert::api::CMultiFileDataAdder persister(baseRestoredOutputFilename);
        CPPUNIT_ASSERT(restoredDetector.persistState(persister));
    }

    for (size_t index = 0; index < numRestoredDocs; ++index)
    {
        std::string expectedRestoredFilename(baseRestoredOutputFilename);
        expectedRestoredFilename += "/prelertresults-";
        expectedRestoredFilename += JOB_ID;
        expectedRestoredFilename += '/';
        expectedRestoredFilename += prelert::api::CAnomalyDetector::STATE_TYPE;
        expectedRestoredFilename += '/';
        expectedRestoredFilename += prelert::core::CStringUtils::typeToString(1 + index);
        expectedRestoredFilename += prelert::api::CMultiFileDataAdder::JSON_FILE_EXT;
        std::ifstream restoredFile(expectedRestoredFilename.c_str());
        CPPUNIT_ASSERT(restoredFile.is_open());
        std::string json((std::istreambuf_iterator<char>(restoredFile)),
                         std::istreambuf_iterator<char>());

        CPPUNIT_ASSERT_EQUAL(origFileContents[index], json);
    }

    // Clean up
    boost::filesystem::path origDir(baseOrigOutputFilename);
    CPPUNIT_ASSERT_NO_THROW(boost::filesystem::remove_all(origDir));
    boost::filesystem::path restoredDir(baseRestoredOutputFilename);
    CPPUNIT_ASSERT_NO_THROW(boost::filesystem::remove_all(restoredDir));
}

