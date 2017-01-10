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
#include "CConfigUpdaterTest.h"

#include <model/CDetectionRule.h>
#include <model/CModelConfig.h>
#include <model/CRuleCondition.h>

#include <api/CConfigUpdater.h>
#include <api/CFieldConfig.h>

#include <string>


using namespace ml;
using namespace api;

CppUnit::Test *CConfigUpdaterTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CConfigUpdaterTest");
    suiteOfTests->addTest(new CppUnit::TestCaller<CConfigUpdaterTest>(
                           "CConfigUpdaterTest::testUpdateGivenUpdateCannotBeParsed",
                           &CConfigUpdaterTest::testUpdateGivenUpdateCannotBeParsed) );
    suiteOfTests->addTest(new CppUnit::TestCaller<CConfigUpdaterTest>(
                           "CConfigUpdaterTest::testUpdateGivenUnknownStanzas",
                           &CConfigUpdaterTest::testUpdateGivenUnknownStanzas) );
    suiteOfTests->addTest(new CppUnit::TestCaller<CConfigUpdaterTest>(
                           "CConfigUpdaterTest::testUpdateGivenModelDebugConfig",
                           &CConfigUpdaterTest::testUpdateGivenModelDebugConfig) );
    suiteOfTests->addTest(new CppUnit::TestCaller<CConfigUpdaterTest>(
                           "CConfigUpdaterTest::testUpdateGivenDetectorRules",
                           &CConfigUpdaterTest::testUpdateGivenDetectorRules) );
    suiteOfTests->addTest(new CppUnit::TestCaller<CConfigUpdaterTest>(
                           "CConfigUpdaterTest::testUpdateGivenRulesWithInvalidDetectorIndex",
                           &CConfigUpdaterTest::testUpdateGivenRulesWithInvalidDetectorIndex) );
    return suiteOfTests;
}

void CConfigUpdaterTest::testUpdateGivenUpdateCannotBeParsed(void)
{
    CFieldConfig fieldConfig;
    model::CModelConfig modelConfig = model::CModelConfig::defaultConfig();
    CConfigUpdater configUpdater(fieldConfig, modelConfig);
    CPPUNIT_ASSERT(configUpdater.update("this is invalid") == false);
}

void CConfigUpdaterTest::testUpdateGivenUnknownStanzas(void)
{
    CFieldConfig fieldConfig;
    model::CModelConfig modelConfig = model::CModelConfig::defaultConfig();
    CConfigUpdater configUpdater(fieldConfig, modelConfig);
    CPPUNIT_ASSERT(configUpdater.update("[unknown1]\na = 1\n[unknown2]\nb = 2\n") == false);
}
void CConfigUpdaterTest::testUpdateGivenModelDebugConfig(void)
{
    typedef model::CModelConfig::TStrSet TStrSet;

    CFieldConfig fieldConfig;
    model::CModelConfig modelConfig = model::CModelConfig::defaultConfig();
    modelConfig.modelDebugBoundsPercentile(95.0);
    TStrSet terms;
    terms.insert(std::string("a"));
    terms.insert(std::string("b"));
    modelConfig.modelDebugTerms(terms);

    std::string configUpdate("[modelDebugConfig]\nboundspercentile = 83.5\nterms = c,d\n");

    CConfigUpdater configUpdater(fieldConfig, modelConfig);

    CPPUNIT_ASSERT(configUpdater.update(configUpdate));
    CPPUNIT_ASSERT_EQUAL(model::CModelConfig::E_File, modelConfig.modelDebugDestination());
    CPPUNIT_ASSERT_EQUAL(83.5, modelConfig.modelDebugBoundsPercentile());

    terms = modelConfig.modelDebugTerms();
    CPPUNIT_ASSERT_EQUAL(std::size_t(2), terms.size());
    CPPUNIT_ASSERT(terms.find(std::string("c")) != terms.end());
    CPPUNIT_ASSERT(terms.find(std::string("d")) != terms.end());
}

void CConfigUpdaterTest::testUpdateGivenDetectorRules(void)
{
    typedef model::CModelConfig::TStrSet TStrSet;

    model::CDetectionRule rule;
    model::CRuleCondition condition;
    rule.addCondition(condition);
    CFieldConfig fieldConfig;
    std::string originalRules0("[{\"ruleAction\":\"FILTER_RESULTS\",\"conditionsConnective\":\"OR\",");
    originalRules0 += "\"ruleConditions\":[{\"conditionType\":\"NUMERICAL_ACTUAL\",\"condition\":{\"operator\":\"LT\",\"value\":\"5\"}}]}]";
    std::string originalRules1("[{\"ruleAction\":\"FILTER_RESULTS\",\"conditionsConnective\":\"OR\",");
    originalRules1 += "\"ruleConditions\":[{\"conditionType\":\"NUMERICAL_ACTUAL\",\"condition\":{\"operator\":\"GT\",\"value\":\"5\"}}]}]";
    fieldConfig.parseRules(0, originalRules0);
    fieldConfig.parseRules(1, originalRules1);

    model::CModelConfig modelConfig = model::CModelConfig::defaultConfig();
    modelConfig.modelDebugBoundsPercentile(95.0);
    TStrSet terms;
    terms.insert(std::string("a"));
    terms.insert(std::string("b"));
    modelConfig.modelDebugTerms(terms);

    std::string configUpdate0("[detectorRules]\ndetectorIndex = 0\nrulesJson = []\n");
    std::string configUpdate1("[detectorRules]\ndetectorIndex = 1\nrulesJson = [{\"ruleAction\":\"FILTER_RESULTS\",\"conditionsConnective\":\"OR\",\"ruleConditions\":[{\"conditionType\":\"NUMERICAL_TYPICAL\",\"condition\":{\"operator\":\"LT\",\"value\":\"15\"}}]}]");

    CConfigUpdater configUpdater(fieldConfig, modelConfig);

    CPPUNIT_ASSERT(configUpdater.update(configUpdate0));
    CPPUNIT_ASSERT(configUpdater.update(configUpdate1));

    CFieldConfig::TIntDetectionRuleVecUMap::const_iterator itr = fieldConfig.detectionRules().find(0);
    CPPUNIT_ASSERT(itr->second.empty());
    itr = fieldConfig.detectionRules().find(1);
    CPPUNIT_ASSERT_EQUAL(std::size_t(1), itr->second.size());
    CPPUNIT_ASSERT_EQUAL(std::string("FILTER_RESULTS IF TYPICAL < 15.000000"), itr->second[0].print());
}

void CConfigUpdaterTest::testUpdateGivenRulesWithInvalidDetectorIndex(void)
{
    typedef model::CModelConfig::TStrSet TStrSet;

    model::CDetectionRule rule;
    model::CRuleCondition condition;
    rule.addCondition(condition);
    CFieldConfig fieldConfig;
    std::string originalRules("[{\"ruleAction\":\"FILTER_RESULTS\",\"conditionsConnective\":\"OR\",");
    originalRules += "\"ruleConditions\":[{\"conditionType\":\"NUMERICAL_ACTUAL\",\"condition\":{\"operator\":\"LT\",\"value\":\"5\"}}]}]";
    fieldConfig.parseRules(0, originalRules);

    model::CModelConfig modelConfig = model::CModelConfig::defaultConfig();
    modelConfig.modelDebugBoundsPercentile(95.0);
    TStrSet terms;
    terms.insert(std::string("a"));
    terms.insert(std::string("b"));
    modelConfig.modelDebugTerms(terms);

    std::string configUpdate("[detectorRules]\ndetectorIndex = invalid\nrulesJson = []\n");

    CConfigUpdater configUpdater(fieldConfig, modelConfig);

    CPPUNIT_ASSERT(configUpdater.update(configUpdate) == false);
}
