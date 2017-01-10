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
#include "CDetectionRulesJsonParserTest.h"

#include <core/CLogger.h>
#include <core/CPatternSet.h>

#include <api/CDetectionRulesJsonParser.h>

#include <boost/unordered_map.hpp>

using namespace ml;
using namespace api;

namespace
{
typedef CDetectionRulesJsonParser::TStrPatternSetUMap TStrPatternSetUMap;
TStrPatternSetUMap EMPTY_VALUE_LIST_MAP;
}

CppUnit::Test *CDetectionRulesJsonParserTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CDetectionRulesJsonParserTest");

    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRulesJsonParserTest>(
           "CDetectionRulesJsonParserTest::testParseRulesGivenEmptyString",
           &CDetectionRulesJsonParserTest::testParseRulesGivenEmptyString));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRulesJsonParserTest>(
           "CDetectionRulesJsonParserTest::testParseRulesGivenEmptyArray",
           &CDetectionRulesJsonParserTest::testParseRulesGivenEmptyArray));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRulesJsonParserTest>(
           "CDetectionRulesJsonParserTest::testParseRulesGivenArrayContainsStrings",
           &CDetectionRulesJsonParserTest::testParseRulesGivenArrayContainsStrings));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRulesJsonParserTest>(
           "CDetectionRulesJsonParserTest::testParseRulesGivenMissingRuleAction",
           &CDetectionRulesJsonParserTest::testParseRulesGivenMissingRuleAction));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRulesJsonParserTest>(
           "CDetectionRulesJsonParserTest::testParseRulesGivenInvalidRuleAction",
           &CDetectionRulesJsonParserTest::testParseRulesGivenInvalidRuleAction));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRulesJsonParserTest>(
           "CDetectionRulesJsonParserTest::testParseRulesGivenMissingConditionsConnective",
           &CDetectionRulesJsonParserTest::testParseRulesGivenMissingConditionsConnective));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRulesJsonParserTest>(
           "CDetectionRulesJsonParserTest::testParseRulesGivenInvalidConditionsConnective",
           &CDetectionRulesJsonParserTest::testParseRulesGivenInvalidConditionsConnective));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRulesJsonParserTest>(
           "CDetectionRulesJsonParserTest::testParseRulesGivenMissingRuleConditions",
           &CDetectionRulesJsonParserTest::testParseRulesGivenMissingRuleConditions));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRulesJsonParserTest>(
           "CDetectionRulesJsonParserTest::testParseRulesGivenRuleConditionsIsNotArray",
           &CDetectionRulesJsonParserTest::testParseRulesGivenRuleConditionsIsNotArray));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRulesJsonParserTest>(
           "CDetectionRulesJsonParserTest::testParseRulesGivenMissingConditionOperator",
           &CDetectionRulesJsonParserTest::testParseRulesGivenMissingConditionOperator));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRulesJsonParserTest>(
           "CDetectionRulesJsonParserTest::testParseRulesGivenInvalidConditionOperator",
           &CDetectionRulesJsonParserTest::testParseRulesGivenInvalidConditionOperator));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRulesJsonParserTest>(
           "CDetectionRulesJsonParserTest::testParseRulesGivenNumericalActualRuleWithConnectiveOr",
           &CDetectionRulesJsonParserTest::testParseRulesGivenNumericalActualRuleWithConnectiveOr));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRulesJsonParserTest>(
           "CDetectionRulesJsonParserTest::testParseRulesGivenNumericalTypicalAndDiffAbsRuleWithConnectiveAnd",
           &CDetectionRulesJsonParserTest::testParseRulesGivenNumericalTypicalAndDiffAbsRuleWithConnectiveAnd));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRulesJsonParserTest>(
           "CDetectionRulesJsonParserTest::testParseRulesGivenMultipleRules",
           &CDetectionRulesJsonParserTest::testParseRulesGivenMultipleRules));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRulesJsonParserTest>(
           "CDetectionRulesJsonParserTest::testParseRulesGivenCategoricalRule",
           &CDetectionRulesJsonParserTest::testParseRulesGivenCategoricalRule));
    return suiteOfTests;
}

void CDetectionRulesJsonParserTest::testParseRulesGivenEmptyString(void)
{
    LOG_DEBUG("*** testParseRulesGivenEmptyString ***");

    CDetectionRulesJsonParser parser(EMPTY_VALUE_LIST_MAP);
    CDetectionRulesJsonParser::TDetectionRuleVec rules;
    std::string rulesJson = "";

    CPPUNIT_ASSERT(parser.parseRules(rulesJson, rules) == false);

    CPPUNIT_ASSERT(rules.empty());
}

void CDetectionRulesJsonParserTest::testParseRulesGivenEmptyArray(void)
{
    LOG_DEBUG("*** testParseRulesGivenEmptyArray ***");

    CDetectionRulesJsonParser parser(EMPTY_VALUE_LIST_MAP);
    CDetectionRulesJsonParser::TDetectionRuleVec rules;
    std::string rulesJson = "[]";

    CPPUNIT_ASSERT(parser.parseRules(rulesJson, rules));

    CPPUNIT_ASSERT(rules.empty());
}

void CDetectionRulesJsonParserTest::testParseRulesGivenArrayContainsStrings(void)
{
    LOG_DEBUG("*** testParseRulesGivenArrayContainsStrings ***");

    CDetectionRulesJsonParser parser(EMPTY_VALUE_LIST_MAP);
    CDetectionRulesJsonParser::TDetectionRuleVec rules;
    std::string rulesJson = "[\"a\", \"b\"]";

    CPPUNIT_ASSERT(parser.parseRules(rulesJson, rules) == false);

    CPPUNIT_ASSERT(rules.empty());
}

void CDetectionRulesJsonParserTest::testParseRulesGivenMissingRuleAction(void)
{
    LOG_DEBUG("*** testParseRulesGivenMissingRuleAction ***");

    CDetectionRulesJsonParser parser(EMPTY_VALUE_LIST_MAP);
    CDetectionRulesJsonParser::TDetectionRuleVec rules;
    std::string rulesJson = "[";
    rulesJson += "{";
    rulesJson += "  \"conditionsConnective\":\"OR\",";
    rulesJson += "  \"ruleConditions\": [";
    rulesJson += "    {\"conditionType\":\"NUMERICAL_ACTUAL\", \"condition\":{\"operator\":\"LT\",\"value\":\"5\"}}";
    rulesJson += "  ]";
    rulesJson += "}";
    rulesJson += "]";

    CPPUNIT_ASSERT(parser.parseRules(rulesJson, rules) == false);
    CPPUNIT_ASSERT(rules.empty());
}

void CDetectionRulesJsonParserTest::testParseRulesGivenInvalidRuleAction(void)
{
    LOG_DEBUG("*** testParseRulesGivenInvalidRuleAction ***");

    CDetectionRulesJsonParser parser(EMPTY_VALUE_LIST_MAP);
    CDetectionRulesJsonParser::TDetectionRuleVec rules;
    std::string rulesJson = "[";
    rulesJson += "{";
    rulesJson += "  \"ruleAction\":\"something_invalid\",";
    rulesJson += "  \"conditionsConnective\":\"OR\",";
    rulesJson += "  \"ruleConditions\": [";
    rulesJson += "    {\"conditionType\":\"NUMERICAL_ACTUAL\", \"condition\":{\"operator\":\"LT\",\"value\":\"5\"}}";
    rulesJson += "  ]";
    rulesJson += "}";
    rulesJson += "]";

    CPPUNIT_ASSERT(parser.parseRules(rulesJson, rules) == false);
    CPPUNIT_ASSERT(rules.empty());
}

void CDetectionRulesJsonParserTest::testParseRulesGivenMissingConditionsConnective(void)
{
    LOG_DEBUG("*** testParseRulesGivenMissingConditionsConnective ***");

    CDetectionRulesJsonParser parser(EMPTY_VALUE_LIST_MAP);
    CDetectionRulesJsonParser::TDetectionRuleVec rules;
    std::string rulesJson = "[";
    rulesJson += "{";
    rulesJson += "  \"ruleAction\":\"FILTER_RESULTS\",";
    rulesJson += "  \"ruleConditions\": [";
    rulesJson += "    {\"conditionType\":\"NUMERICAL_ACTUAL\", \"condition\":{\"operator\":\"LT\",\"value\":\"5\"}}";
    rulesJson += "  ]";
    rulesJson += "}";
    rulesJson += "]";

    CPPUNIT_ASSERT(parser.parseRules(rulesJson, rules) == false);
    CPPUNIT_ASSERT(rules.empty());
}

void CDetectionRulesJsonParserTest::testParseRulesGivenInvalidConditionsConnective(void)
{
    LOG_DEBUG("*** testParseRulesGivenInvalidConditionsConnective ***");

    CDetectionRulesJsonParser parser(EMPTY_VALUE_LIST_MAP);
    CDetectionRulesJsonParser::TDetectionRuleVec rules;
    std::string rulesJson = "[";
    rulesJson += "{";
    rulesJson += "  \"ruleAction\":\"FILTER_RESULTS\",";
    rulesJson += "  \"conditionsConnective\":\"XOR\",";
    rulesJson += "  \"ruleConditions\": [";
    rulesJson += "    {\"conditionType\":\"NUMERICAL_ACTUAL\", \"condition\":{\"operator\":\"LT\",\"value\":\"5\"}}";
    rulesJson += "  ]";
    rulesJson += "}";
    rulesJson += "]";

    CPPUNIT_ASSERT(parser.parseRules(rulesJson, rules) == false);
    CPPUNIT_ASSERT(rules.empty());
}

void CDetectionRulesJsonParserTest::testParseRulesGivenMissingRuleConditions(void)
{
    LOG_DEBUG("*** testParseRulesGivenMissingRuleConditions ***");

    CDetectionRulesJsonParser parser(EMPTY_VALUE_LIST_MAP);
    CDetectionRulesJsonParser::TDetectionRuleVec rules;
    std::string rulesJson = "[";
    rulesJson += "{";
    rulesJson += "  \"ruleAction\":\"FILTER_RESULTS\",";
    rulesJson += "  \"conditionsConnective\":\"OR\",";
    rulesJson += "}";
    rulesJson += "]";

    CPPUNIT_ASSERT(parser.parseRules(rulesJson, rules) == false);
    CPPUNIT_ASSERT(rules.empty());
}

void CDetectionRulesJsonParserTest::testParseRulesGivenRuleConditionsIsNotArray(void)
{
    LOG_DEBUG("*** testParseRulesGivenRuleConditionsIsNotArray ***");

    CDetectionRulesJsonParser parser(EMPTY_VALUE_LIST_MAP);
    CDetectionRulesJsonParser::TDetectionRuleVec rules;
    std::string rulesJson = "[";
    rulesJson += "{";
    rulesJson += "  \"ruleAction\":\"FILTER_RESULTS\",";
    rulesJson += "  \"conditionsConnective\":\"OR\",";
    rulesJson += "  \"ruleConditions\": {}";
    rulesJson += "}";
    rulesJson += "]";

    CPPUNIT_ASSERT(parser.parseRules(rulesJson, rules) == false);
    CPPUNIT_ASSERT(rules.empty());
}

void CDetectionRulesJsonParserTest::testParseRulesGivenMissingConditionOperator(void)
{
    LOG_DEBUG("*** testParseRulesGivenMissingConditionOperator ***");

    CDetectionRulesJsonParser parser(EMPTY_VALUE_LIST_MAP);
    CDetectionRulesJsonParser::TDetectionRuleVec rules;
    std::string rulesJson = "[";
    rulesJson += "{";
    rulesJson += "  \"ruleAction\":\"FILTER_RESULTS\",";
    rulesJson += "  \"conditionsConnective\":\"OR\",";
    rulesJson += "  \"ruleConditions\": [";
    rulesJson += "    {\"conditionType\":\"NUMERICAL_ACTUAL\", \"condition\":{\"value\":\"5\"}}";
    rulesJson += "  ]";
    rulesJson += "}";
    rulesJson += "]";

    CPPUNIT_ASSERT(parser.parseRules(rulesJson, rules) == false);
    CPPUNIT_ASSERT(rules.empty());
}

void CDetectionRulesJsonParserTest::testParseRulesGivenInvalidConditionOperator(void)
{
    LOG_DEBUG("*** testParseRulesGivenInvalidConditionOperator ***");

    CDetectionRulesJsonParser parser(EMPTY_VALUE_LIST_MAP);
    CDetectionRulesJsonParser::TDetectionRuleVec rules;
    std::string rulesJson = "[";
    rulesJson += "{";
    rulesJson += "  \"ruleAction\":\"FILTER_RESULTS\",";
    rulesJson += "  \"conditionsConnective\":\"OR\",";
    rulesJson += "  \"ruleConditions\": [";
    rulesJson += "    {\"conditionType\":\"NUMERICAL_ACTUAL\", \"condition\":{\"operator\":\"HA\",\"value\":\"5\"}}";
    rulesJson += "  ]";
    rulesJson += "}";
    rulesJson += "]";

    CPPUNIT_ASSERT(parser.parseRules(rulesJson, rules) == false);
    CPPUNIT_ASSERT(rules.empty());
}

void CDetectionRulesJsonParserTest::testParseRulesGivenNumericalActualRuleWithConnectiveOr(void)
{
    LOG_DEBUG("*** testParseRulesGivenNumericalActualRuleWithConnectiveOr ***");

    CDetectionRulesJsonParser parser(EMPTY_VALUE_LIST_MAP);
    CDetectionRulesJsonParser::TDetectionRuleVec rules;
    std::string rulesJson = "[";
    rulesJson += "{";
    rulesJson += "  \"ruleAction\":\"FILTER_RESULTS\",";
    rulesJson += "  \"conditionsConnective\":\"OR\",";
    rulesJson += "  \"ruleConditions\": [";
    rulesJson += "    {\"conditionType\":\"NUMERICAL_ACTUAL\", \"condition\":{\"operator\":\"LT\",\"value\":\"5\"}},";
    rulesJson += "    {\"conditionType\":\"NUMERICAL_ACTUAL\", \"fieldName\":\"metric\", \"condition\":{\"operator\":\"LTE\",\"value\":\"2.3\"}}";
    rulesJson += "  ]";
    rulesJson += "}";
    rulesJson += "]";

    CPPUNIT_ASSERT(parser.parseRules(rulesJson, rules));

    CPPUNIT_ASSERT_EQUAL(std::size_t(1), rules.size());
    CPPUNIT_ASSERT_EQUAL(std::string("FILTER_RESULTS IF ACTUAL < 5.000000 OR ACTUAL(metric) <= 2.300000"), rules[0].print());
}

void CDetectionRulesJsonParserTest::testParseRulesGivenNumericalTypicalAndDiffAbsRuleWithConnectiveAnd(void)
{
    LOG_DEBUG("*** testParseRulesGivenNumericalTypicalAndDiffAbsRuleWithConnectiveAnd ***");

    CDetectionRulesJsonParser parser(EMPTY_VALUE_LIST_MAP);
    CDetectionRulesJsonParser::TDetectionRuleVec rules;
    std::string rulesJson = "[";
    rulesJson += "{";
    rulesJson += "  \"ruleAction\":\"FILTER_RESULTS\",";
    rulesJson += "  \"conditionsConnective\":\"AND\",";
    rulesJson += "  \"ruleConditions\": [";
    rulesJson += "    {\"conditionType\":\"NUMERICAL_TYPICAL\", \"condition\":{\"operator\":\"GT\",\"value\":\"5\"}},";
    rulesJson += "    {\"conditionType\":\"NUMERICAL_DIFF_ABS\", \"fieldName\":\"metric\", \"fieldValue\":\"cpu\",\"condition\":{\"operator\":\"GTE\",\"value\":\"2.3\"}}";
    rulesJson += "  ]";
    rulesJson += "}";
    rulesJson += "]";

    CPPUNIT_ASSERT(parser.parseRules(rulesJson, rules));

    CPPUNIT_ASSERT_EQUAL(std::size_t(1), rules.size());
    CPPUNIT_ASSERT_EQUAL(std::string("FILTER_RESULTS IF TYPICAL > 5.000000 AND DIFF_ABS(metric:cpu) >= 2.300000"), rules[0].print());
}

void CDetectionRulesJsonParserTest::testParseRulesGivenMultipleRules(void)
{
    LOG_DEBUG("*** testParseRulesGivenMultipleRules ***");

    CDetectionRulesJsonParser parser(EMPTY_VALUE_LIST_MAP);
    CDetectionRulesJsonParser::TDetectionRuleVec rules;
    std::string rulesJson = "[";
    rulesJson += "{";
    rulesJson += "  \"ruleAction\":\"FILTER_RESULTS\",";
    rulesJson += "  \"conditionsConnective\":\"OR\",";
    rulesJson += "  \"targetFieldName\":\"id\",";
    rulesJson += "  \"targetFieldValue\":\"foo\",";
    rulesJson += "  \"ruleConditions\": [";
    rulesJson += "    {\"conditionType\":\"NUMERICAL_ACTUAL\", \"condition\":{\"operator\":\"LT\",\"value\":\"1\"}}";
    rulesJson += "  ]";
    rulesJson += "},";
    rulesJson += "{";
    rulesJson += "  \"ruleAction\":\"FILTER_RESULTS\",";
    rulesJson += "  \"conditionsConnective\":\"AND\",";
    rulesJson += "  \"targetFieldName\":\"id\",";
    rulesJson += "  \"targetFieldValue\":\"42\",";
    rulesJson += "  \"ruleConditions\": [";
    rulesJson += "    {\"conditionType\":\"NUMERICAL_ACTUAL\", \"condition\":{\"operator\":\"LT\",\"value\":\"2\"}}";
    rulesJson += "  ]";
    rulesJson += "}";
    rulesJson += "]";

    CPPUNIT_ASSERT(parser.parseRules(rulesJson, rules));

    CPPUNIT_ASSERT_EQUAL(std::size_t(2), rules.size());
    CPPUNIT_ASSERT_EQUAL(std::string("FILTER_RESULTS (id:foo) IF ACTUAL < 1.000000"), rules[0].print());
    CPPUNIT_ASSERT_EQUAL(std::string("FILTER_RESULTS (id:42) IF ACTUAL < 2.000000"), rules[1].print());
}

void CDetectionRulesJsonParserTest::testParseRulesGivenCategoricalRule(void)
{
    LOG_DEBUG("*** testParseRulesGivenCategoricalRule ***");

    TStrPatternSetUMap listsById;
    core::CPatternSet list;
    list.initFromJson("[\"b\", \"a\"]");
    listsById["list1"] = list;

    CDetectionRulesJsonParser parser(listsById);
    CDetectionRulesJsonParser::TDetectionRuleVec rules;
    std::string rulesJson = "[";
    rulesJson += "{";
    rulesJson += "  \"ruleAction\":\"FILTER_RESULTS\",";
    rulesJson += "  \"conditionsConnective\":\"OR\",";
    rulesJson += "  \"ruleConditions\": [";
    rulesJson += "    {\"conditionType\":\"CATEGORICAL\", \"fieldName\":\"foo\", \"valueList\":\"list1\"}";
    rulesJson += "  ]";
    rulesJson += "}";
    rulesJson += "]";

    CPPUNIT_ASSERT(parser.parseRules(rulesJson, rules));

    CPPUNIT_ASSERT_EQUAL(std::size_t(1), rules.size());
    CPPUNIT_ASSERT_EQUAL(std::string("FILTER_RESULTS IF (foo) IN LIST"), rules[0].print());
}
