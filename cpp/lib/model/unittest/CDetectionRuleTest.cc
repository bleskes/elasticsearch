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
#include "CDetectionRuleTest.h"

#include "CMockModel.h"

#include <core/CLogger.h>
#include <core/CPatternSet.h>

#include <model/CDataGatherer.h>
#include <model/CDetectionRule.h>
#include <model/CModel.h>
#include <model/CModelParams.h>
#include <model/CResourceMonitor.h>
#include <model/CRuleCondition.h>
#include <model/CSearchKey.h>
#include <model/ModelTypes.h>

#include <string>
#include <vector>

using namespace prelert;
using namespace model;

namespace
{

typedef std::vector<model_t::EFeature> TFeatureVec;
typedef std::vector<std::string> TStrVec;

const std::string EMPTY_STRING;
}

CppUnit::Test *CDetectionRuleTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CDetectionRuleTest");

    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRuleTest>(
           "CDetectionRuleTest::testApplyGivenCategoricalCondition",
           &CDetectionRuleTest::testApplyGivenCategoricalCondition));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRuleTest>(
           "CDetectionRuleTest::testApplyGivenNumericalActualCondition",
           &CDetectionRuleTest::testApplyGivenNumericalActualCondition));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRuleTest>(
           "CDetectionRuleTest::testApplyGivenNumericalTypicalCondition",
           &CDetectionRuleTest::testApplyGivenNumericalTypicalCondition));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRuleTest>(
           "CDetectionRuleTest::testApplyGivenNumericalDiffAbsCondition",
           &CDetectionRuleTest::testApplyGivenNumericalDiffAbsCondition));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRuleTest>(
           "CDetectionRuleTest::testApplyGivenSingleSeriesModelAndConditionWithField",
           &CDetectionRuleTest::testApplyGivenSingleSeriesModelAndConditionWithField));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRuleTest>(
           "CDetectionRuleTest::testApplyGivenNoActualValueAvailable",
           &CDetectionRuleTest::testApplyGivenNoActualValueAvailable));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRuleTest>(
           "CDetectionRuleTest::testApplyGivenDifferentSeriesAndIndividualModel",
           &CDetectionRuleTest::testApplyGivenDifferentSeriesAndIndividualModel));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRuleTest>(
           "CDetectionRuleTest::testApplyGivenDifferentSeriesAndPopulationModel",
           &CDetectionRuleTest::testApplyGivenDifferentSeriesAndPopulationModel));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRuleTest>(
           "CDetectionRuleTest::testApplyGivenMultipleConditionsWithOr",
           &CDetectionRuleTest::testApplyGivenMultipleConditionsWithOr));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRuleTest>(
           "CDetectionRuleTest::testApplyGivenMultipleConditionsWithAnd",
           &CDetectionRuleTest::testApplyGivenMultipleConditionsWithAnd));
    suiteOfTests->addTest(new CppUnit::TestCaller<CDetectionRuleTest>(
           "CDetectionRuleTest::testApplyGivenTargetFieldIsPartitionAndIndividualModel",
           &CDetectionRuleTest::testApplyGivenTargetFieldIsPartitionAndIndividualModel));

    return suiteOfTests;
}

void CDetectionRuleTest::testApplyGivenCategoricalCondition(void)
{
    LOG_DEBUG("*** testApplyGivenCategoricalCondition ***");

    core_t::TTime bucketLength = 100;
    core_t::TTime startTime = 100;
    CSearchKey key;
    SModelParams params(bucketLength);
    CModel::TFeatureInfluenceCalculatorCPtrPrVecVec influenceCalculators;

    TFeatureVec features;
    features.push_back(model_t::E_IndividualMeanByPerson);
    CModel::TDataGathererPtr gathererPtr(
            new CDataGatherer(model_t::E_Metric, model_t::E_None, params,
                              "", "", "", "", "", TStrVec(),
                              false, key, features,
                              startTime, 0));

    CResourceMonitor resourceMonitor;
    std::string person11("p11");
    std::string person12("p12");
    std::string person21("p21");
    std::string person22("p22");
    bool addedPerson = false;
    gathererPtr->addPerson(person11, resourceMonitor, addedPerson);
    gathererPtr->addPerson(person12, resourceMonitor, addedPerson);
    gathererPtr->addPerson(person21, resourceMonitor, addedPerson);
    gathererPtr->addPerson(person22, resourceMonitor, addedPerson);

    CMockModel model(params, gathererPtr, influenceCalculators);
    CModel::TDouble1Vec actual100(1, 4.99);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 100, actual100);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 1, 0, 100, actual100);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 2, 0, 100, actual100);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 3, 0, 100, actual100);

    {
        std::string listJson("[\"p11\",\"p22\"]");
        core::CPatternSet valueList;
        valueList.initFromJson(listJson);

        CRuleCondition condition;
        condition.type(CRuleCondition::E_CATEGORICAL);
        condition.valueList(valueList);
        CDetectionRule rule;
        rule.addCondition(condition);

        model_t::CResultType resultType(model_t::CResultType::E_Final);

        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100));
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 1, 0, 100) == false);
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 2, 0, 100) == false);
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 3, 0, 100));
    }
    {
        std::string listJson("[\"p1*\"]");
        core::CPatternSet valueList;
        valueList.initFromJson(listJson);

        CRuleCondition condition;
        condition.type(CRuleCondition::E_CATEGORICAL);
        condition.valueList(valueList);
        CDetectionRule rule;
        rule.addCondition(condition);

        model_t::CResultType resultType(model_t::CResultType::E_Final);

        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100));
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 1, 0, 100));
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 2, 0, 100) == false);
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 3, 0, 100) == false);
    }
    {
        std::string listJson("[\"*2\"]");
        core::CPatternSet valueList;
        valueList.initFromJson(listJson);

        CRuleCondition condition;
        condition.type(CRuleCondition::E_CATEGORICAL);
        condition.valueList(valueList);
        CDetectionRule rule;
        rule.addCondition(condition);

        model_t::CResultType resultType(model_t::CResultType::E_Final);

        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100) == false);
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 1, 0, 100));
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 2, 0, 100) == false);
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 3, 0, 100));
    }
    {
        std::string listJson("[\"*1*\"]");
        core::CPatternSet valueList;
        valueList.initFromJson(listJson);

        CRuleCondition condition;
        condition.type(CRuleCondition::E_CATEGORICAL);
        condition.valueList(valueList);
        CDetectionRule rule;
        rule.addCondition(condition);

        model_t::CResultType resultType(model_t::CResultType::E_Final);

        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100));
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 1, 0, 100));
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 2, 0, 100));
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 3, 0, 100) == false);
    }
}

void CDetectionRuleTest::testApplyGivenNumericalActualCondition(void)
{
    LOG_DEBUG("*** testApplyGivenNumericalActionCondition ***");

    core_t::TTime bucketLength = 100;
    core_t::TTime startTime = 100;
    CSearchKey key;
    SModelParams params(bucketLength);
    CModel::TFeatureInfluenceCalculatorCPtrPrVecVec influenceCalculators;

    TFeatureVec features;
    features.push_back(model_t::E_IndividualMeanByPerson);
    CModel::TDataGathererPtr gathererPtr(
            new CDataGatherer(model_t::E_Metric, model_t::E_None, params,
                              "", "", "", "", "", TStrVec(),
                              false, key, features,
                              startTime, 0));

    CResourceMonitor resourceMonitor;
    std::string person1("p1");
    bool addedPerson = false;
    gathererPtr->addPerson(person1, resourceMonitor, addedPerson);

    CMockModel model(params, gathererPtr, influenceCalculators);
    CModel::TDouble1Vec actual100(1, 4.99);
    CModel::TDouble1Vec actual200(1, 5.00);
    CModel::TDouble1Vec actual300(1, 5.01);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 100, actual100);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 200, actual200);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 300, actual300);

    {
        // Test rule with condition with operator LT

        CRuleCondition condition;
        condition.type(CRuleCondition::E_NUMERICAL_ACTUAL);
        condition.condition().s_Op = CRuleCondition::E_LT;
        condition.condition().s_Threshold = 5.0;
        CDetectionRule rule;
        rule.addCondition(condition);

        model_t::CResultType resultType(model_t::CResultType::E_Final);

        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100));
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 200) == false);
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 300) == false);
    }

    {
        // Test rule with condition with operator LTE

        CRuleCondition condition;
        condition.type(CRuleCondition::E_NUMERICAL_ACTUAL);
        condition.condition().s_Op = CRuleCondition::E_LTE;
        condition.condition().s_Threshold = 5.0;
        CDetectionRule rule;
        rule.addCondition(condition);

        model_t::CResultType resultType(model_t::CResultType::E_Final);

        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100));
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 200));
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 300) == false);
    }
    {
        // Test rule with condition with operator GT

        CRuleCondition condition;
        condition.type(CRuleCondition::E_NUMERICAL_ACTUAL);
        condition.condition().s_Op = CRuleCondition::E_GT;
        condition.condition().s_Threshold = 5.0;
        CDetectionRule rule;
        rule.addCondition(condition);

        model_t::CResultType resultType(model_t::CResultType::E_Final);

        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100) == false);
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 200) == false);
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 300));
    }
    {
        // Test rule with condition with operator GT

        CRuleCondition condition;
        condition.type(CRuleCondition::E_NUMERICAL_ACTUAL);
        condition.condition().s_Op = CRuleCondition::E_GTE;
        condition.condition().s_Threshold = 5.0;
        CDetectionRule rule;
        rule.addCondition(condition);

        model_t::CResultType resultType(model_t::CResultType::E_Final);

        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100) == false);
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 200));
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 300));
    }
}

void CDetectionRuleTest::testApplyGivenNumericalTypicalCondition(void)
{
    LOG_DEBUG("*** testApplyGivenNumericalTypicalCondition ***");

    core_t::TTime bucketLength = 100;
    core_t::TTime startTime = 100;
    CSearchKey key;
    SModelParams params(bucketLength);
    CModel::TFeatureInfluenceCalculatorCPtrPrVecVec influenceCalculators;

    TFeatureVec features;
    features.push_back(model_t::E_IndividualMeanByPerson);
    CModel::TDataGathererPtr gathererPtr(
            new CDataGatherer(model_t::E_Metric, model_t::E_None, params,
                              "", "", "", "", "", TStrVec(),
                              false, key, features,
                              startTime, 0));

    CResourceMonitor resourceMonitor;
    std::string person1("p1");
    bool addedPerson = false;
    gathererPtr->addPerson(person1, resourceMonitor, addedPerson);

    CMockModel model(params, gathererPtr, influenceCalculators);
    CModel::TDouble1Vec actual100(1, 4.99);
    CModel::TDouble1Vec actual200(1, 5.00);
    CModel::TDouble1Vec actual300(1, 5.01);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 100, actual100);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 200, actual200);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 300, actual300);
    CModel::TDouble1Vec typical100(1, 44.99);
    CModel::TDouble1Vec typical200(1, 45.00);
    CModel::TDouble1Vec typical300(1, 45.01);
    model.mockAddBucketBaselineMean(model_t::E_IndividualMeanByPerson, 0, 0, 100, typical100);
    model.mockAddBucketBaselineMean(model_t::E_IndividualMeanByPerson, 0, 0, 200, typical200);
    model.mockAddBucketBaselineMean(model_t::E_IndividualMeanByPerson, 0, 0, 300, typical300);

    {
        // Test rule with condition with operator LT

        CRuleCondition condition;
        condition.type(CRuleCondition::E_NUMERICAL_TYPICAL);
        condition.condition().s_Op = CRuleCondition::E_LT;
        condition.condition().s_Threshold = 45.0;
        CDetectionRule rule;
        rule.addCondition(condition);

        model_t::CResultType resultType(model_t::CResultType::E_Final);

        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100));
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 200) == false);
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 300) == false);
    }

    {
        // Test rule with condition with operator GT

        CRuleCondition condition;
        condition.type(CRuleCondition::E_NUMERICAL_TYPICAL);
        condition.condition().s_Op = CRuleCondition::E_GT;
        condition.condition().s_Threshold = 45.0;
        CDetectionRule rule;
        rule.addCondition(condition);

        model_t::CResultType resultType(model_t::CResultType::E_Final);

        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100) == false);
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 200) == false);
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 300));
    }
}

void CDetectionRuleTest::testApplyGivenNumericalDiffAbsCondition(void)
{
    LOG_DEBUG("*** testApplyGivenNumericalDiffAbsCondition ***");

    core_t::TTime bucketLength = 100;
    core_t::TTime startTime = 100;
    CSearchKey key;
    SModelParams params(bucketLength);
    CModel::TFeatureInfluenceCalculatorCPtrPrVecVec influenceCalculators;

    TFeatureVec features;
    features.push_back(model_t::E_IndividualMeanByPerson);
    CModel::TDataGathererPtr gathererPtr(
            new CDataGatherer(model_t::E_Metric, model_t::E_None, params,
                              "", "", "", "", "", TStrVec(),
                              false, key, features,
                              startTime, 0));

    CResourceMonitor resourceMonitor;
    std::string person1("p1");
    bool addedPerson = false;
    gathererPtr->addPerson(person1, resourceMonitor, addedPerson);

    CMockModel model(params, gathererPtr, influenceCalculators);
    CModel::TDouble1Vec actual100(1, 8.9);
    CModel::TDouble1Vec actual200(1, 9.0);
    CModel::TDouble1Vec actual300(1, 9.1);
    CModel::TDouble1Vec actual400(1, 10.9);
    CModel::TDouble1Vec actual500(1, 11.0);
    CModel::TDouble1Vec actual600(1, 11.1);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 100, actual100);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 200, actual200);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 300, actual300);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 400, actual400);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 500, actual500);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 600, actual600);
    CModel::TDouble1Vec typical100(1, 10.0);
    CModel::TDouble1Vec typical200(1, 10.0);
    CModel::TDouble1Vec typical300(1, 10.0);
    CModel::TDouble1Vec typical400(1, 10.0);
    CModel::TDouble1Vec typical500(1, 10.0);
    CModel::TDouble1Vec typical600(1, 10.0);
    model.mockAddBucketBaselineMean(model_t::E_IndividualMeanByPerson, 0, 0, 100, typical100);
    model.mockAddBucketBaselineMean(model_t::E_IndividualMeanByPerson, 0, 0, 200, typical200);
    model.mockAddBucketBaselineMean(model_t::E_IndividualMeanByPerson, 0, 0, 300, typical300);
    model.mockAddBucketBaselineMean(model_t::E_IndividualMeanByPerson, 0, 0, 400, typical400);
    model.mockAddBucketBaselineMean(model_t::E_IndividualMeanByPerson, 0, 0, 500, typical500);
    model.mockAddBucketBaselineMean(model_t::E_IndividualMeanByPerson, 0, 0, 600, typical600);

    {
        // Test rule with condition with operator LT

        CRuleCondition condition;
        condition.type(CRuleCondition::E_NUMERICAL_DIFF_ABS);
        condition.condition().s_Op = CRuleCondition::E_LT;
        condition.condition().s_Threshold = 1.0;
        CDetectionRule rule;
        rule.addCondition(condition);

        model_t::CResultType resultType(model_t::CResultType::E_Final);

        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100) == false);
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 200) == false);
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 300));
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 400));
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 500) == false);
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 600) == false);
    }

    {
        // Test rule with condition with operator GT

        CRuleCondition condition;
        condition.type(CRuleCondition::E_NUMERICAL_DIFF_ABS);
        condition.condition().s_Op = CRuleCondition::E_GT;
        condition.condition().s_Threshold = 1.0;
        CDetectionRule rule;
        rule.addCondition(condition);

        model_t::CResultType resultType(model_t::CResultType::E_Final);

        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100));
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 200) == false);
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 300) == false);
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 400) == false);
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 500) == false);
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 600));
    }
}

void CDetectionRuleTest::testApplyGivenSingleSeriesModelAndConditionWithField(void)
{
    LOG_DEBUG("*** testApplyGivenSingleSeriesModelAndConditionWithField ***");

    core_t::TTime bucketLength = 100;
    core_t::TTime startTime = 100;
    CSearchKey key;
    SModelParams params(bucketLength);
    CModel::TFeatureInfluenceCalculatorCPtrPrVecVec influenceCalculators;

    TFeatureVec features;
    features.push_back(model_t::E_IndividualMeanByPerson);
    CModel::TDataGathererPtr gathererPtr(
            new CDataGatherer(model_t::E_Metric, model_t::E_None, params,
                              "", "", "", "", "", TStrVec(),
                              false, key, features,
                              startTime, 0));

    CResourceMonitor resourceMonitor;
    std::string person1("p1");
    bool addedPerson = false;
    gathererPtr->addPerson(person1, resourceMonitor, addedPerson);

    CMockModel model(params, gathererPtr, influenceCalculators);
    CModel::TDouble1Vec actual100(1, 4.99);
    CModel::TDouble1Vec actual200(1, 5.00);
    CModel::TDouble1Vec actual300(1, 5.01);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 100, actual100);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 200, actual200);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 300, actual300);

    CRuleCondition condition;
    condition.type(CRuleCondition::E_NUMERICAL_ACTUAL);
    std::string fieldName("unknownField");
    std::string fieldValue("unknownValue");
    condition.fieldName(fieldName);
    condition.fieldValue(fieldValue);
    condition.condition().s_Op = CRuleCondition::E_LT;
    condition.condition().s_Threshold = 5.0;
    CDetectionRule rule;
    rule.addCondition(condition);

    model_t::CResultType resultType(model_t::CResultType::E_Final);

    CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
            model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100) == false);
    CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
            model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 200) == false);
    CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
            model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 300) == false);
}

void CDetectionRuleTest::testApplyGivenNoActualValueAvailable(void)
{
    LOG_DEBUG("*** testApplyGivenNoActualValueAvailable ***");

    core_t::TTime bucketLength = 100;
    core_t::TTime startTime = 100;
    CSearchKey key;
    SModelParams params(bucketLength);
    CModel::TFeatureInfluenceCalculatorCPtrPrVecVec influenceCalculators;

    TFeatureVec features;
    features.push_back(model_t::E_IndividualMeanByPerson);
    CModel::TDataGathererPtr gathererPtr(
            new CDataGatherer(model_t::E_Metric, model_t::E_None, params,
                              "", "", "", "", "", TStrVec(),
                              false, key, features,
                              startTime, 0));

    CResourceMonitor resourceMonitor;
    std::string person1("p1");
    bool addedPerson = false;
    gathererPtr->addPerson(person1, resourceMonitor, addedPerson);

    CMockModel model(params, gathererPtr, influenceCalculators);
    CModel::TDouble1Vec actual100(1, 4.99);
    CModel::TDouble1Vec actual200(1, 5.00);
    CModel::TDouble1Vec actual300(1, 5.01);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 100, actual100);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 200, actual200);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 300, actual300);

    CRuleCondition condition;
    condition.type(CRuleCondition::E_NUMERICAL_ACTUAL);
    condition.condition().s_Op = CRuleCondition::E_LT;
    condition.condition().s_Threshold = 5.0;
    CDetectionRule rule;
    rule.addCondition(condition);

    model_t::CResultType resultType(model_t::CResultType::E_Final);

    CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
            model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 400) == false);
}

void CDetectionRuleTest::testApplyGivenDifferentSeriesAndIndividualModel(void)
{
    LOG_DEBUG("*** testApplyGivenDifferentSeriesAndIndividualModel ***");

    core_t::TTime bucketLength = 100;
    core_t::TTime startTime = 100;
    CSearchKey key;
    SModelParams params(bucketLength);
    CModel::TFeatureInfluenceCalculatorCPtrPrVecVec influenceCalculators;

    TFeatureVec features;
    features.push_back(model_t::E_IndividualMeanByPerson);
    std::string personFieldName("series");
    CModel::TDataGathererPtr gathererPtr(
            new CDataGatherer(model_t::E_Metric, model_t::E_None, params,
                              "", "", personFieldName, "", "", TStrVec(),
                              false, key, features,
                              startTime, 0));

    CResourceMonitor resourceMonitor;
    std::string person1("p1");
    bool addedPerson = false;
    gathererPtr->addPerson(person1, resourceMonitor, addedPerson);
    std::string person2("p2");
    gathererPtr->addPerson(person2, resourceMonitor, addedPerson);

    CMockModel model(params, gathererPtr, influenceCalculators);
    CModel::TDouble1Vec p1Actual(1, 4.99);
    CModel::TDouble1Vec p2Actual(1, 4.99);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 100, p1Actual);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 1, 0, 100, p2Actual);

    CRuleCondition condition;
    condition.type(CRuleCondition::E_NUMERICAL_ACTUAL);
    condition.fieldName(personFieldName);
    condition.fieldValue(person1);
    condition.condition().s_Op = CRuleCondition::E_LT;
    condition.condition().s_Threshold = 5.0;
    CDetectionRule rule;
    rule.addCondition(condition);

    model_t::CResultType resultType(model_t::CResultType::E_Final);

    CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
            model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100));
    CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
            model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 1, 0, 100) == false);
}

void CDetectionRuleTest::testApplyGivenDifferentSeriesAndPopulationModel(void)
{
    LOG_DEBUG("*** testApplyGivenDifferentSeriesAndPopulationModel ***");

    core_t::TTime bucketLength = 100;
    core_t::TTime startTime = 100;
    CSearchKey key;
    SModelParams params(bucketLength);
    CModel::TFeatureInfluenceCalculatorCPtrPrVecVec influenceCalculators;

    TFeatureVec features;
    features.push_back(model_t::E_IndividualMeanByPerson);
    std::string personFieldName("over");
    std::string attributeFieldName("by");
    CModel::TDataGathererPtr gathererPtr(
            new CDataGatherer(model_t::E_Metric, model_t::E_None, params,
                              "", "", personFieldName, attributeFieldName, "", TStrVec(),
                              false, key, features,
                              startTime, 0));

    CResourceMonitor resourceMonitor;
    std::string person1("p1");
    bool added = false;
    gathererPtr->addPerson(person1, resourceMonitor, added);
    std::string person2("p2");
    gathererPtr->addPerson(person2, resourceMonitor, added);
    std::string attr11("a1_1");
    std::string attr12("a1_2");
    std::string attr21("a2_1");
    std::string attr22("a2_2");
    gathererPtr->addAttribute(attr11, resourceMonitor, added);
    gathererPtr->addAttribute(attr12, resourceMonitor, added);
    gathererPtr->addAttribute(attr21, resourceMonitor, added);
    gathererPtr->addAttribute(attr22, resourceMonitor, added);

    CMockModel model(params, gathererPtr, influenceCalculators);
    model.mockPopulation(true);
    CModel::TDouble1Vec actual(1, 4.99);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 100, actual);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 1, 100, actual);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 1, 2, 100, actual);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 1, 3, 100, actual);

    CRuleCondition condition;
    condition.type(CRuleCondition::E_NUMERICAL_ACTUAL);
    condition.fieldName(attributeFieldName);
    condition.fieldValue(attr12);
    condition.condition().s_Op = CRuleCondition::E_LT;
    condition.condition().s_Threshold = 5.0;
    CDetectionRule rule;
    rule.addCondition(condition);

    model_t::CResultType resultType(model_t::CResultType::E_Final);

    CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
            model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100) == false);
    CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
            model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 1, 100));
    CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
            model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 1, 2, 100) == false);
    CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
            model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 1, 3, 100) == false);
}

void CDetectionRuleTest::testApplyGivenMultipleConditionsWithOr(void)
{
    LOG_DEBUG("*** testApplyGivenMultipleConditionsWithOr ***");

    core_t::TTime bucketLength = 100;
    core_t::TTime startTime = 100;
    CSearchKey key;
    SModelParams params(bucketLength);
    CModel::TFeatureInfluenceCalculatorCPtrPrVecVec influenceCalculators;

    TFeatureVec features;
    features.push_back(model_t::E_IndividualMeanByPerson);
    std::string personFieldName("series");
    CModel::TDataGathererPtr gathererPtr(
            new CDataGatherer(model_t::E_Metric, model_t::E_None, params,
                              "", "", personFieldName, "", "", TStrVec(),
                              false, key, features,
                              startTime, 0));

    CResourceMonitor resourceMonitor;
    std::string person1("p1");
    bool addedPerson = false;
    gathererPtr->addPerson(person1, resourceMonitor, addedPerson);

    CMockModel model(params, gathererPtr, influenceCalculators);
    CModel::TDouble1Vec p1Actual(1, 10.0);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 100, p1Actual);

    {
        // None applies
        CRuleCondition condition1;
        condition1.type(CRuleCondition::E_NUMERICAL_ACTUAL);
        condition1.fieldName(personFieldName);
        condition1.fieldValue(person1);
        condition1.condition().s_Op = CRuleCondition::E_LT;
        condition1.condition().s_Threshold = 9.0;
        CRuleCondition condition2;
        condition2.type(CRuleCondition::E_NUMERICAL_ACTUAL);
        condition2.fieldName(personFieldName);
        condition2.fieldValue(person1);
        condition2.condition().s_Op = CRuleCondition::E_LT;
        condition2.condition().s_Threshold = 9.5;
        CDetectionRule rule;
        rule.addCondition(condition1);
        rule.addCondition(condition2);

        model_t::CResultType resultType(model_t::CResultType::E_Final);

        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100) == false);
    }
    {
        // First applies only
        CRuleCondition condition1;
        condition1.type(CRuleCondition::E_NUMERICAL_ACTUAL);
        condition1.fieldName(personFieldName);
        condition1.fieldValue(person1);
        condition1.condition().s_Op = CRuleCondition::E_LT;
        condition1.condition().s_Threshold = 11.0;
        CRuleCondition condition2;
        condition2.type(CRuleCondition::E_NUMERICAL_ACTUAL);
        condition2.fieldName(personFieldName);
        condition2.fieldValue(person1);
        condition2.condition().s_Op = CRuleCondition::E_LT;
        condition2.condition().s_Threshold = 9.5;
        CDetectionRule rule;
        rule.addCondition(condition1);
        rule.addCondition(condition2);

        model_t::CResultType resultType(model_t::CResultType::E_Final);

        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100));
    }
    {
        // Second applies only
        CRuleCondition condition1;
        condition1.type(CRuleCondition::E_NUMERICAL_ACTUAL);
        condition1.fieldName(personFieldName);
        condition1.fieldValue(person1);
        condition1.condition().s_Op = CRuleCondition::E_LT;
        condition1.condition().s_Threshold = 9.0;
        CRuleCondition condition2;
        condition2.type(CRuleCondition::E_NUMERICAL_ACTUAL);
        condition2.fieldName(personFieldName);
        condition2.fieldValue(person1);
        condition2.condition().s_Op = CRuleCondition::E_LT;
        condition2.condition().s_Threshold = 10.5;
        CDetectionRule rule;
        rule.addCondition(condition1);
        rule.addCondition(condition2);

        model_t::CResultType resultType(model_t::CResultType::E_Final);

        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100));
    }
    {
        // Both apply
        CRuleCondition condition1;
        condition1.type(CRuleCondition::E_NUMERICAL_ACTUAL);
        condition1.fieldName(personFieldName);
        condition1.fieldValue(person1);
        condition1.condition().s_Op = CRuleCondition::E_LT;
        condition1.condition().s_Threshold = 12.0;
        CRuleCondition condition2;
        condition2.type(CRuleCondition::E_NUMERICAL_ACTUAL);
        condition2.fieldName(personFieldName);
        condition2.fieldValue(person1);
        condition2.condition().s_Op = CRuleCondition::E_LT;
        condition2.condition().s_Threshold = 10.5;
        CDetectionRule rule;
        rule.addCondition(condition1);
        rule.addCondition(condition2);

        model_t::CResultType resultType(model_t::CResultType::E_Final);

        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100));
    }
}

void CDetectionRuleTest::testApplyGivenMultipleConditionsWithAnd(void)
{
    LOG_DEBUG("*** testApplyGivenMultipleConditionsWithAnd ***");

    core_t::TTime bucketLength = 100;
    core_t::TTime startTime = 100;
    CSearchKey key;
    SModelParams params(bucketLength);
    CModel::TFeatureInfluenceCalculatorCPtrPrVecVec influenceCalculators;

    TFeatureVec features;
    features.push_back(model_t::E_IndividualMeanByPerson);
    std::string personFieldName("series");
    CModel::TDataGathererPtr gathererPtr(
            new CDataGatherer(model_t::E_Metric, model_t::E_None, params,
                              "", "", personFieldName, "", "", TStrVec(),
                              false, key, features,
                              startTime, 0));

    CResourceMonitor resourceMonitor;
    std::string person1("p1");
    bool addedPerson = false;
    gathererPtr->addPerson(person1, resourceMonitor, addedPerson);

    CMockModel model(params, gathererPtr, influenceCalculators);
    CModel::TDouble1Vec p1Actual(1, 10.0);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 100, p1Actual);

    {
        // None applies
        CRuleCondition condition1;
        condition1.type(CRuleCondition::E_NUMERICAL_ACTUAL);
        condition1.fieldName(personFieldName);
        condition1.fieldValue(person1);
        condition1.condition().s_Op = CRuleCondition::E_LT;
        condition1.condition().s_Threshold = 9.0;
        CRuleCondition condition2;
        condition2.type(CRuleCondition::E_NUMERICAL_ACTUAL);
        condition2.fieldName(personFieldName);
        condition2.fieldValue(person1);
        condition2.condition().s_Op = CRuleCondition::E_LT;
        condition2.condition().s_Threshold = 9.5;
        CDetectionRule rule;
        rule.conditionsConnective(CDetectionRule::E_AND);
        rule.addCondition(condition1);
        rule.addCondition(condition2);

        model_t::CResultType resultType(model_t::CResultType::E_Final);

        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100) == false);
    }
    {
        // First applies only
        CRuleCondition condition1;
        condition1.type(CRuleCondition::E_NUMERICAL_ACTUAL);
        condition1.fieldName(personFieldName);
        condition1.fieldValue(person1);
        condition1.condition().s_Op = CRuleCondition::E_LT;
        condition1.condition().s_Threshold = 11.0;
        CRuleCondition condition2;
        condition2.type(CRuleCondition::E_NUMERICAL_ACTUAL);
        condition2.fieldName(personFieldName);
        condition2.fieldValue(person1);
        condition2.condition().s_Op = CRuleCondition::E_LT;
        condition2.condition().s_Threshold = 9.5;
        CDetectionRule rule;
        rule.conditionsConnective(CDetectionRule::E_AND);
        rule.addCondition(condition1);
        rule.addCondition(condition2);

        model_t::CResultType resultType(model_t::CResultType::E_Final);

        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100) == false);
    }
    {
        // Second applies only
        CRuleCondition condition1;
        condition1.type(CRuleCondition::E_NUMERICAL_ACTUAL);
        condition1.fieldName(personFieldName);
        condition1.fieldValue(person1);
        condition1.condition().s_Op = CRuleCondition::E_LT;
        condition1.condition().s_Threshold = 9.0;
        CRuleCondition condition2;
        condition2.type(CRuleCondition::E_NUMERICAL_ACTUAL);
        condition2.fieldName(personFieldName);
        condition2.fieldValue(person1);
        condition2.condition().s_Op = CRuleCondition::E_LT;
        condition2.condition().s_Threshold = 10.5;
        CDetectionRule rule;
        rule.conditionsConnective(CDetectionRule::E_AND);
        rule.addCondition(condition1);
        rule.addCondition(condition2);

        model_t::CResultType resultType(model_t::CResultType::E_Final);

        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100) == false);
    }
    {
        // Both apply
        CRuleCondition condition1;
        condition1.type(CRuleCondition::E_NUMERICAL_ACTUAL);
        condition1.fieldName(personFieldName);
        condition1.fieldValue(person1);
        condition1.condition().s_Op = CRuleCondition::E_LT;
        condition1.condition().s_Threshold = 12.0;
        CRuleCondition condition2;
        condition2.type(CRuleCondition::E_NUMERICAL_ACTUAL);
        condition2.fieldName(personFieldName);
        condition2.fieldValue(person1);
        condition2.condition().s_Op = CRuleCondition::E_LT;
        condition2.condition().s_Threshold = 10.5;
        CDetectionRule rule;
        rule.conditionsConnective(CDetectionRule::E_AND);
        rule.addCondition(condition1);
        rule.addCondition(condition2);

        model_t::CResultType resultType(model_t::CResultType::E_Final);

        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100));
    }
}

void CDetectionRuleTest::testApplyGivenTargetFieldIsPartitionAndIndividualModel(void)
{
    LOG_DEBUG("*** testApplyGivenTargetFieldIsPartitionAndIndividualModel ***");

    core_t::TTime bucketLength = 100;
    core_t::TTime startTime = 100;
    CSearchKey key;
    SModelParams params(bucketLength);
    CModel::TFeatureInfluenceCalculatorCPtrPrVecVec influenceCalculators;

    TFeatureVec features;
    features.push_back(model_t::E_IndividualMeanByPerson);
    std::string partitionFieldName("partition");
    std::string personFieldName("series");
    CModel::TDataGathererPtr gathererPtr(
            new CDataGatherer(model_t::E_Metric, model_t::E_None, params,
                              "", partitionFieldName, personFieldName, "", "", TStrVec(),
                              false, key, features,
                              startTime, 0));

    CResourceMonitor resourceMonitor;
    std::string person1("p1");
    bool addedPerson = false;
    gathererPtr->addPerson(person1, resourceMonitor, addedPerson);
    std::string person2("p2");
    gathererPtr->addPerson(person2, resourceMonitor, addedPerson);

    CMockModel model(params, gathererPtr, influenceCalculators);
    CModel::TDouble1Vec p1Actual(1, 10.0);
    CModel::TDouble1Vec p2Actual(1, 20.0);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, 100, p1Actual);
    model.mockAddBucketValue(model_t::E_IndividualMeanByPerson, 1, 0, 100, p2Actual);

    {
        // No targetFieldValue
        CRuleCondition condition;
        condition.type(CRuleCondition::E_NUMERICAL_ACTUAL);
        condition.fieldName(personFieldName);
        condition.fieldValue(person1);
        condition.condition().s_Op = CRuleCondition::E_LT;
        condition.condition().s_Threshold = 15.0;
        CDetectionRule rule;
        rule.targetFieldName(partitionFieldName);
        rule.addCondition(condition);

        model_t::CResultType resultType(model_t::CResultType::E_Final);

        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 0, 0, 100));
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, EMPTY_STRING, 1, 0, 100));
    }
    {
        // Matching targetFieldValue
        std::string partitionValue("partition_1");
        CRuleCondition condition;
        condition.type(CRuleCondition::E_NUMERICAL_ACTUAL);
        condition.fieldName(personFieldName);
        condition.fieldValue(person1);
        condition.condition().s_Op = CRuleCondition::E_LT;
        condition.condition().s_Threshold = 15.0;
        CDetectionRule rule;
        rule.targetFieldName(partitionFieldName);
        rule.targetFieldValue(partitionValue);
        rule.addCondition(condition);

        model_t::CResultType resultType(model_t::CResultType::E_Final);

        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, partitionValue, 0, 0, 100));
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, partitionValue, 1, 0, 100));
    }
    {
        // Non-matching targetFieldValue
        std::string partitionValue1("partition_1");
        std::string partitionValue2("partition_2");
        CRuleCondition condition;
        condition.type(CRuleCondition::E_NUMERICAL_ACTUAL);
        condition.fieldName(personFieldName);
        condition.fieldValue(person1);
        condition.condition().s_Op = CRuleCondition::E_LT;
        condition.condition().s_Threshold = 15.0;
        CDetectionRule rule;
        rule.targetFieldName(partitionFieldName);
        rule.targetFieldValue(partitionValue1);
        rule.addCondition(condition);

        model_t::CResultType resultType(model_t::CResultType::E_Final);

        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, partitionValue2, 0, 0, 100) == false);
        CPPUNIT_ASSERT(rule.apply(CDetectionRule::E_FILTER_RESULTS, model,
                model_t::E_IndividualMeanByPerson, resultType, partitionValue2, 1, 0, 100) == false);
    }
}
