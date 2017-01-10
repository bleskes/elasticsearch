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
#include "CResultNormalizerTest.h"

#include <core/CLogger.h>

#include <model/CModelConfig.h>

#include <api/CCsvInputParser.h>
#include <api/CCsvOutputWriter.h>
#include <api/CResultNormalizer.h>

#include <fstream>
#include <string>


CppUnit::Test *CResultNormalizerTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CResultNormalizerTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CResultNormalizerTest>(
                                   "CResultNormalizerTest::testInitNormalizer",
                                   &CResultNormalizerTest::testInitNormalizer) );

    return suiteOfTests;
}

void CResultNormalizerTest::testInitNormalizer(void)
{
    ml::model::CModelConfig modelConfig = ml::model::CModelConfig::defaultConfig(3600);

    ml::api::CCsvOutputWriter outputWriter;

    ml::api::CResultNormalizer normalizer(modelConfig, outputWriter);

    CPPUNIT_ASSERT(normalizer.initNormalizer("testfiles/quantilesState.json"));

    std::ifstream inputStrm("testfiles/normalizerInput.csv");
    ml::api::CCsvInputParser inputParser(inputStrm,
                                              ml::api::CCsvInputParser::COMMA);
    CPPUNIT_ASSERT(inputParser.readStream(false,
                                          ml::api::CInputParser::TSettingsFunc(),
                                          boost::bind(&ml::api::CResultNormalizer::handleRecord,
                                                      &normalizer,
                                                      _1,
                                                      _2,
                                                      _3)));

    std::string results(outputWriter.internalString());
    LOG_DEBUG("Results:\n" << results);

    // The maximum bucketTime influencer probability in the Savvis data used to initialise
    // the normaliser is 2.56098e-205, so this should map to the highest normalised
    // score which is 98.28496
    CPPUNIT_ASSERT(results.find("root,,bucketTime,,,2.56098e-205,98.28496") != std::string::npos);
    CPPUNIT_ASSERT(results.find("inflb,,status,,,2.93761e-203,97.26764") != std::string::npos);
    CPPUNIT_ASSERT(results.find("infl,,status,,,5.56572e-204,98.56057") != std::string::npos);
    CPPUNIT_ASSERT(results.find("root,,bucketTime,,,1e-10,31.20283") != std::string::npos);
    CPPUNIT_ASSERT(results.find("root,,bucketTime,,,1,0") != std::string::npos);
    CPPUNIT_ASSERT(results.find("infl,,status,,,1,0") != std::string::npos);
    CPPUNIT_ASSERT(results.find("leaf,,status,count,,1e-300,99.19481") != std::string::npos);
    CPPUNIT_ASSERT(results.find("leaf,,status,count,,1,0") != std::string::npos);
}

