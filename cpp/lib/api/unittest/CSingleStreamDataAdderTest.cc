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
#include "CSingleStreamDataAdderTest.h"

#include <core/CoreTypes.h>
#include <core/COsFileFuncs.h>
#include <core/CStringUtils.h>

#include <maths/CModelWeight.h>

#include <model/CLimits.h>
#include <model/CModelConfig.h>

#include <api/CAnomalyDetector.h>
#include <api/CCsvInputParser.h>
#include <api/CFieldConfig.h>
#include <api/CJsonOutputWriter.h>
#include <api/CSingleStreamDataAdder.h>
#include <api/CSingleStreamSearcher.h>

#include <boost/bind.hpp>
#include <boost/ref.hpp>

#include <fstream>
#include <sstream>
#include <string>

namespace
{

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

CppUnit::Test *CSingleStreamDataAdderTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CSingleStreamDataAdderTest");
    suiteOfTests->addTest(new CppUnit::TestCaller<CSingleStreamDataAdderTest>(
                                   "CSingleStreamDataAdderTest::testDetectorPersistBy",
                                   &CSingleStreamDataAdderTest::testDetectorPersistBy) );
    suiteOfTests->addTest(new CppUnit::TestCaller<CSingleStreamDataAdderTest>(
                                   "CSingleStreamDataAdderTest::testDetectorPersistOver",
                                   &CSingleStreamDataAdderTest::testDetectorPersistOver) );
    suiteOfTests->addTest(new CppUnit::TestCaller<CSingleStreamDataAdderTest>(
                                   "CSingleStreamDataAdderTest::testDetectorPersistPartition",
                                   &CSingleStreamDataAdderTest::testDetectorPersistPartition) );
    suiteOfTests->addTest(new CppUnit::TestCaller<CSingleStreamDataAdderTest>(
                                   "CSingleStreamDataAdderTest::testDetectorPersistDc",
                                   &CSingleStreamDataAdderTest::testDetectorPersistDc) );
    suiteOfTests->addTest(new CppUnit::TestCaller<CSingleStreamDataAdderTest>(
                                   "CSingleStreamDataAdderTest::testDetectorPersistCount",
                                   &CSingleStreamDataAdderTest::testDetectorPersistCount) );
    return suiteOfTests;
}

void CSingleStreamDataAdderTest::testDetectorPersistBy(void)
{
    ml::maths::CScopeDisableNormalizeOnRestore disabler;

    this->detectorPersistHelper("testfiles/new_mlfields.conf",
                                "testfiles/big_ascending.txt",
                                0);
}

void CSingleStreamDataAdderTest::testDetectorPersistOver(void)
{
    ml::maths::CScopeDisableNormalizeOnRestore disabler;

    this->detectorPersistHelper("testfiles/new_mlfields_over.conf",
                                "testfiles/big_ascending.txt",
                                0);
}

void CSingleStreamDataAdderTest::testDetectorPersistPartition(void)
{
    ml::maths::CScopeDisableNormalizeOnRestore disabler;

    this->detectorPersistHelper("testfiles/new_mlfields_partition.conf",
                                "testfiles/big_ascending.txt",
                                0);
}

void CSingleStreamDataAdderTest::testDetectorPersistDc(void)
{
    this->detectorPersistHelper("testfiles/new_persist_dc.conf",
                                "testfiles/files_users_programs.csv",
                                5);
}

void CSingleStreamDataAdderTest::testDetectorPersistCount(void)
{
    this->detectorPersistHelper("testfiles/new_persist_count.conf",
                                "testfiles/files_users_programs.csv",
                                5);
}

void CSingleStreamDataAdderTest::detectorPersistHelper(const std::string &configFileName,
                                                       const std::string &inputFilename,
                                                       int latencyBuckets)
{
    // Start by creating a detector with non-trivial state
    static const ml::core_t::TTime BUCKET_SIZE(3600);

    // Open the input and output files
    std::ifstream inputStrm(inputFilename.c_str());
    CPPUNIT_ASSERT(inputStrm.is_open());

    std::ofstream outputStrm(ml::core::COsFileFuncs::NULL_FILENAME);
    CPPUNIT_ASSERT(outputStrm.is_open());

    ml::model::CLimits limits;
    ml::api::CFieldConfig fieldConfig;
    CPPUNIT_ASSERT(fieldConfig.initFromFile(configFileName));

    ml::model::CModelConfig modelConfig =
            ml::model::CModelConfig::defaultConfig(BUCKET_SIZE,
                                                        ml::model::CModelConfig::DEFAULT_BATCH_LENGTH,
                                                        ml::model::CModelConfig::APERIODIC,
                                                        ml::model_t::E_None,
                                                        "",
                                                        BUCKET_SIZE * latencyBuckets,
                                                        0,
                                                        false,
                                                        "");

    ml::api::CJsonOutputWriter outputWriter("job", outputStrm);

    std::string origSnapshotId;
    std::size_t numOrigDocs(0);
    ml::api::CAnomalyDetector origDetector("job",
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

    ml::api::CCsvInputParser parser(inputStrm);

    CPPUNIT_ASSERT(parser.readStream(false,
                                     boost::bind(&ml::api::CAnomalyDetector::handleSettings,
                                                 &origDetector,
                                                 _1),
                                     boost::bind(&ml::api::CAnomalyDetector::handleRecord,
                                                 &origDetector,
                                                 _1,
                                                 _2,
                                                 _3)));

    // Persist the detector state to a stringstream

    std::string origPersistedState;
    {
        std::ostringstream *strm(0);
        ml::api::CSingleStreamDataAdder::TOStreamP ptr(strm = new std::ostringstream());
        ml::api::CSingleStreamDataAdder persister(ptr);
        CPPUNIT_ASSERT(origDetector.persistState(persister));
        origPersistedState = strm->str();
    }

    // Now restore the state into a different detector

    std::string restoredSnapshotId;
    std::size_t numRestoredDocs(0);
    ml::api::CAnomalyDetector restoredDetector("job",
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
        ml::core_t::TTime completeToTime(0);

        ml::api::CSingleStreamSearcher::TIStreamP strm(new std::istringstream(this->mapPersistFormatToRestoreFormat(origPersistedState)));
        ml::api::CSingleStreamSearcher retriever(strm);
        CPPUNIT_ASSERT(restoredDetector.restoreState(retriever, completeToTime));
        CPPUNIT_ASSERT(completeToTime > 0);
    }

    // Finally, persist the new detector state to a file

    std::string newPersistedState;
    {
        std::ostringstream *strm(0);
        ml::api::CSingleStreamDataAdder::TOStreamP ptr(strm = new std::ostringstream());
        ml::api::CSingleStreamDataAdder persister(ptr);
        CPPUNIT_ASSERT(restoredDetector.persistState(persister));
        newPersistedState = strm->str();
    }

    // The snapshot ID can be different between the two persists, so replace the
    // first occurrence of it (which is in the bulk metadata)
    CPPUNIT_ASSERT_EQUAL(size_t(1), ml::core::CStringUtils::replaceFirst(origSnapshotId,
                                                                              "snap",
                                                                              origPersistedState));
    CPPUNIT_ASSERT_EQUAL(size_t(1), ml::core::CStringUtils::replaceFirst(restoredSnapshotId,
                                                                              "snap",
                                                                              newPersistedState));

    CPPUNIT_ASSERT_EQUAL(origPersistedState, newPersistedState);
}

std::string CSingleStreamDataAdderTest::mapPersistFormatToRestoreFormat(const std::string &persistedData)
{
    LOG_TRACE("Persisted:" << ml::core_t::LINE_ENDING << persistedData);

    // Persist format is:
    // { bulk metadata }
    // { document source }
    // '\0'
    //
    // Restore format is:
    // { Elasticsearch get response }
    // '\0'

    std::istringstream input(persistedData);
    std::ostringstream output;

    std::string line;
    while (std::getline(input, line))
    {
        if (line.empty())
        {
            continue;
        }

        if (line[0] == '\0')
        {
            if (line.length() == 1)
            {
                continue;
            }
            line.erase(0, 1);
        }

        if (line.compare(0, 19, "{\"index\":{\"_index\":") == 0)
        {
            // Strip the leading {"index": and the two closing braces
            line.erase(0, 9);
            for (size_t count = 0; count < 2; ++count)
            {
                size_t lastBrace(line.find_last_of('}'));
                if (lastBrace != std::string::npos)
                {
                    line.erase(lastBrace);
                }
            }
            output << line << ",\"_version\":1,\"found\":true,\"_source\":";
        }
        else
        {
            output << line << "}\0";
        }
    }

    const std::string &restoredData = output.str();
    LOG_TRACE("Restored:" << ml::core_t::LINE_ENDING << restoredData);
    return restoredData;
}

