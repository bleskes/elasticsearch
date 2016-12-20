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
#include "COutputChainerTest.h"

#include <model/CLimits.h>

#include <api/CAnomalyDetector.h>
#include <api/CCsvInputParser.h>
#include <api/CFieldConfig.h>
#include <api/CJsonOutputWriter.h>
#include <api/COutputChainer.h>

#include <test/CTestTmpDir.h>

#include "CMockDataProcessor.h"

#include <fstream>


CppUnit::Test *COutputChainerTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("COutputChainerTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<COutputChainerTest>(
                                   "COutputChainerTest::testChaining",
                                   &COutputChainerTest::testChaining) );

    return suiteOfTests;
}

void COutputChainerTest::testChaining(void)
{
    static const prelert::core_t::TTime BUCKET_SIZE(3600);

    std::string inputFileName("testfiles/big_ascending.txt");
    std::string outputFileName(prelert::test::CTestTmpDir::tmpDir() + "/chainerOutput.txt");

    {
        // Open the input and output files
        std::ifstream inputStrm(inputFileName.c_str());
        CPPUNIT_ASSERT(inputStrm.is_open());

        std::ofstream outputStrm(outputFileName.c_str());
        CPPUNIT_ASSERT(outputStrm.is_open());

        // Set up the processing chain as:
        // big.txt -> typer -> chainer -> detector -> chainerOutput.txt

        prelert::model::CLimits limits;
        prelert::api::CFieldConfig fieldConfig;
        CPPUNIT_ASSERT(fieldConfig.initFromFile("testfiles/new_prelertfields.conf"));

        prelert::model::CModelConfig modelConfig =
                prelert::model::CModelConfig::defaultConfig(BUCKET_SIZE);

        prelert::api::CJsonOutputWriter outputWriter("job", outputStrm);

        prelert::api::CAnomalyDetector detector("job",
                                                limits,
                                                fieldConfig,
                                                modelConfig,
                                                outputWriter);

        prelert::api::COutputChainer outputChainer(detector);

        CMockDataProcessor mockProcessor(outputChainer);

        prelert::api::CCsvInputParser parser(inputStrm);

        CPPUNIT_ASSERT(parser.readStream(false,
                                         boost::bind(&CMockDataProcessor::handleSettings,
                                                     &mockProcessor,
                                                     _1),
                                         boost::bind(&CMockDataProcessor::handleRecord,
                                                     &mockProcessor,
                                                     _1,
                                                     _2,
                                                     _3)));
    }

    // Check the results by re-reading the output file
    std::ifstream reReadStrm(outputFileName.c_str());
    std::string line;
    std::string modelSizeString("\"" + prelert::api::CJsonOutputWriter::MODEL_BYTES + "\":");

    std::string expectedLineStart("{\"bucket\":{\"job_id\":\"job\",\"timestamp\":1347199200000,");

    while (line.length() == 0 || line.find(modelSizeString) != std::string::npos)
    {
        std::getline(reReadStrm, line);
        LOG_DEBUG("Read line: " << line);
    }

    // The first character of "line" will either be "[" or ","
    line = line.substr(1);
    // We don't care what the exact output is for this test
    // only that it is present and looks valid
    CPPUNIT_ASSERT_EQUAL(expectedLineStart, line.substr(0, expectedLineStart.length()));

    // TODO add more checks

    reReadStrm.close();
    CPPUNIT_ASSERT_EQUAL(0, ::remove(outputFileName.c_str()));
}

