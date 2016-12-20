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
#include "CBoxPlotDataJsonWriterTest.h"

#include <api/CBoxPlotDataJsonWriter.h>
#include <model/CBoxPlotData.h>
#include <model/ModelTypes.h>

#include <rapidjson/document.h>
#include <rapidjson/prettywriter.h>
#include <rapidjson/stringbuffer.h>

CppUnit::Test* CBoxPlotDataJsonWriterTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CBoxPlotDataJsonWriterTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CBoxPlotDataJsonWriterTest>(
                                   "CBoxPlotDataJsonWriterTest::testWriteFlat",
                                   &CBoxPlotDataJsonWriterTest::testWriteFlat) );

    return suiteOfTests;
}

void CBoxPlotDataJsonWriterTest::testWriteFlat(void)
{
    std::ostringstream sstream;
    {
        prelert::api::CBoxPlotDataJsonWriter writer(sstream);

        prelert::model::CBoxPlotData plotData(1ul, "pName", "pValue", "", "bName");
        plotData.get(prelert::model_t::E_IndividualCountByBucketAndPerson, "bName") = prelert::model::CBoxPlotData::SByFieldData(1.0,  2.0, 3.0);

        writer.writeFlat("job-id", plotData);
    }

    rapidjson::Document doc;
    doc.Parse<rapidjson::kParseDefaultFlags>(sstream.str().c_str());
    CPPUNIT_ASSERT(doc.HasMember("model_debug_output"));
    const rapidjson::Value &modelDebug = doc["model_debug_output"];
    CPPUNIT_ASSERT(modelDebug.HasMember("job_id"));
    CPPUNIT_ASSERT_EQUAL(std::string("job-id"), std::string(modelDebug["job_id"].GetString()));
    CPPUNIT_ASSERT(modelDebug.HasMember("debug_feature"));
    CPPUNIT_ASSERT_EQUAL(std::string("'count per bucket by person'"),
        std::string(modelDebug["debug_feature"].GetString()));
    CPPUNIT_ASSERT(modelDebug.HasMember("timestamp"));
    CPPUNIT_ASSERT_EQUAL(int64_t(1000), modelDebug["timestamp"].GetInt64());
    CPPUNIT_ASSERT(modelDebug.HasMember("partition_field_name"));
    CPPUNIT_ASSERT_EQUAL(std::string("pName"), std::string(modelDebug["partition_field_name"].GetString()));
    CPPUNIT_ASSERT(modelDebug.HasMember("partition_field_value"));
    CPPUNIT_ASSERT_EQUAL(std::string("pValue"), std::string(modelDebug["partition_field_value"].GetString()));
    CPPUNIT_ASSERT(modelDebug.HasMember("by_field_name"));
    CPPUNIT_ASSERT_EQUAL(std::string("bName"), std::string(modelDebug["by_field_name"].GetString()));
    CPPUNIT_ASSERT(modelDebug.HasMember("by_field_value"));
    CPPUNIT_ASSERT_EQUAL(std::string("bName"), std::string(modelDebug["by_field_value"].GetString()));
    CPPUNIT_ASSERT(modelDebug.HasMember("debug_lower"));
    CPPUNIT_ASSERT_DOUBLES_EQUAL(1.0, modelDebug["debug_lower"].GetDouble(), 0.01);
    CPPUNIT_ASSERT(modelDebug.HasMember("debug_upper"));
    CPPUNIT_ASSERT_DOUBLES_EQUAL(2.0, modelDebug["debug_upper"].GetDouble(), 0.01);
    CPPUNIT_ASSERT(modelDebug.HasMember("debug_median"));
    CPPUNIT_ASSERT_DOUBLES_EQUAL(3.0, modelDebug["debug_median"].GetDouble(), 0.01);
}
