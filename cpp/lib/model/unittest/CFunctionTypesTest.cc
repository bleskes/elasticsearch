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

#include "CFunctionTypesTest.h"

#include <core/CLogger.h>
#include <core/CContainerPrinter.h>

#include <model/FunctionTypes.h>

using namespace prelert;
using namespace model;

void CFunctionTypesTest::testFeaturesToFunction(void)
{
    model_t::TFeatureVec features;

    {
        // Count.
        features.push_back(model_t::E_IndividualCountByBucketAndPerson);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("count"), function_t::name(function_t::function(features)));
    }
    {
        // (Rare) Count.
        features.clear();
        features.push_back(model_t::E_IndividualCountByBucketAndPerson);
        features.push_back(model_t::E_IndividualTotalBucketCountByPerson);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("count"), function_t::name(function_t::function(features)));
    }
    {
        // Non-Zero Count.
        features.clear();
        features.push_back(model_t::E_IndividualNonZeroCountByBucketAndPerson);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("non_zero_count"), function_t::name(function_t::function(features)));
    }
    {
        // Non-Zero Rare Count.
        features.clear();
        features.push_back(model_t::E_IndividualNonZeroCountByBucketAndPerson);
        features.push_back(model_t::E_IndividualTotalBucketCountByPerson);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("rare_non_zero_count"), function_t::name(function_t::function(features)));
    }
    {
        // Low Count.
        features.clear();
        features.push_back(model_t::E_IndividualLowCountsByBucketAndPerson);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("low_count"), function_t::name(function_t::function(features)));
    }
    {
        // High Count.
        features.clear();
        features.push_back(model_t::E_IndividualHighCountsByBucketAndPerson);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("high_count"), function_t::name(function_t::function(features)));
    }
    {
        // Rare Count.
        features.clear();
        features.push_back(model_t::E_IndividualIndicatorOfBucketPerson);
        features.push_back(model_t::E_IndividualTotalBucketCountByPerson);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("rare"), function_t::name(function_t::function(features)));
    }
    {
        // Min.
        features.clear();
        features.push_back(model_t::E_IndividualMinByPerson);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("min"), function_t::name(function_t::function(features)));
    }
    {
        // Mean.
        features.clear();
        features.push_back(model_t::E_IndividualMeanByPerson);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("mean"), function_t::name(function_t::function(features)));
        features.clear();
        features.push_back(model_t::E_IndividualLowMeanByPerson);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("low_mean"), function_t::name(function_t::function(features)));
        features.clear();
        features.push_back(model_t::E_IndividualHighMeanByPerson);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("high_mean"), function_t::name(function_t::function(features)));
    }
    {
        // Median.
        features.clear();
        features.push_back(model_t::E_IndividualMedianByPerson);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("median"), function_t::name(function_t::function(features)));
        features.clear();
    }
    {
        // Max.
        features.clear();
        features.push_back(model_t::E_IndividualMaxByPerson);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("max"), function_t::name(function_t::function(features)));
    }
    {
        // Sum.
        features.clear();
        features.push_back(model_t::E_IndividualSumByBucketAndPerson);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("sum"), function_t::name(function_t::function(features)));
        features.clear();
        features.push_back(model_t::E_IndividualLowSumByBucketAndPerson);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("low_sum"), function_t::name(function_t::function(features)));
        features.clear();
        features.push_back(model_t::E_IndividualHighSumByBucketAndPerson);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("high_sum"), function_t::name(function_t::function(features)));
    }
    {
        // Non-Zero Sum.
        features.clear();
        features.push_back(model_t::E_IndividualNonNullSumByBucketAndPerson);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("non_null_sum"), function_t::name(function_t::function(features)));
    }
    {
        // Metric.
        features.clear();
        features.push_back(model_t::E_IndividualMeanByPerson);
        features.push_back(model_t::E_IndividualMaxByPerson);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("metric"), function_t::name(function_t::function(features)));
    }
    {
        // Metric.
        features.clear();
        features.push_back(model_t::E_IndividualMinByPerson);
        features.push_back(model_t::E_IndividualMeanByPerson);
        features.push_back(model_t::E_IndividualMaxByPerson);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("metric"), function_t::name(function_t::function(features)));
    }
    {
        // Lat-long.
        features.clear();
        features.push_back(model_t::E_IndividualMeanLatLongByPerson);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("lat_long"), function_t::name(function_t::function(features)));
    }
    {
        // Count.
        features.clear();
        features.push_back(model_t::E_PopulationCountByBucketPersonAndAttribute);
        features.push_back(model_t::E_PopulationUniquePersonCountByAttribute);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("rare_count"), function_t::name(function_t::function(features)));
    }
    {
        // Low Count.
        features.clear();
        features.push_back(model_t::E_PopulationLowCountsByBucketPersonAndAttribute);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("low_count"), function_t::name(function_t::function(features)));
    }
    {
        // High Count.
        features.clear();
        features.push_back(model_t::E_PopulationHighCountsByBucketPersonAndAttribute);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("high_count"), function_t::name(function_t::function(features)));
    }
    {
        // Distinct count.
        features.clear();
        features.push_back(model_t::E_PopulationUniqueCountByBucketPersonAndAttribute);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("distinct_count"), function_t::name(function_t::function(features)));
    }
    {
        // Min.
        features.clear();
        features.push_back(model_t::E_PopulationMinByPersonAndAttribute);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("min"), function_t::name(function_t::function(features)));
    }
    {
        // Mean.
        features.clear();
        features.push_back(model_t::E_PopulationMeanByPersonAndAttribute);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("mean"), function_t::name(function_t::function(features)));
    }
    {
        // Median.
        features.clear();
        features.push_back(model_t::E_PopulationMedianByPersonAndAttribute);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("median"), function_t::name(function_t::function(features)));
    }
    {
        // Max.
        features.clear();
        features.push_back(model_t::E_PopulationMaxByPersonAndAttribute);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("max"), function_t::name(function_t::function(features)));
    }
    {
        // Sum.
        features.clear();
        features.push_back(model_t::E_PopulationSumByBucketPersonAndAttribute);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("sum"), function_t::name(function_t::function(features)));
    }
    {
        // Metric.
        features.clear();
        features.push_back(model_t::E_PopulationMinByPersonAndAttribute);
        features.push_back(model_t::E_PopulationMeanByPersonAndAttribute);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("metric"), function_t::name(function_t::function(features)));
    }
    {
        // Metric.
        features.clear();
        features.push_back(model_t::E_PopulationMinByPersonAndAttribute);
        features.push_back(model_t::E_PopulationMeanByPersonAndAttribute);
        features.push_back(model_t::E_PopulationMaxByPersonAndAttribute);
        LOG_DEBUG("function = '" << function_t::name(function_t::function(features)) << "'");
        CPPUNIT_ASSERT_EQUAL(std::string("metric"), function_t::name(function_t::function(features)));
    }
}

void CFunctionTypesTest::testStatisticFunctions(void)
{
    CPPUNIT_ASSERT(function_t::statisticFunctions(function_t::E_IndividualCount).empty());
    CPPUNIT_ASSERT(function_t::statisticFunctions(function_t::E_IndividualNonZeroCount).empty());
    CPPUNIT_ASSERT(function_t::statisticFunctions(function_t::E_IndividualRareCount).empty());
    CPPUNIT_ASSERT(function_t::statisticFunctions(function_t::E_IndividualRareNonZeroCount).empty());
    CPPUNIT_ASSERT(function_t::statisticFunctions(function_t::E_IndividualRare).empty());
    CPPUNIT_ASSERT(function_t::statisticFunctions(function_t::E_IndividualLowCounts).empty());
    CPPUNIT_ASSERT(function_t::statisticFunctions(function_t::E_IndividualHighCounts).empty());
    CPPUNIT_ASSERT(function_t::statisticFunctions(function_t::E_IndividualDistinctCount).empty());
    CPPUNIT_ASSERT(function_t::statisticFunctions(function_t::E_IndividualInfoContent).empty());

    CPPUNIT_ASSERT(function_t::statisticFunctions(function_t::E_PopulationCount).empty());
    CPPUNIT_ASSERT(function_t::statisticFunctions(function_t::E_PopulationRare).empty());
    CPPUNIT_ASSERT(function_t::statisticFunctions(function_t::E_PopulationRareCount).empty());
    CPPUNIT_ASSERT(function_t::statisticFunctions(function_t::E_PopulationFreqRare).empty());
    CPPUNIT_ASSERT(function_t::statisticFunctions(function_t::E_PopulationFreqRareCount).empty());
    CPPUNIT_ASSERT(function_t::statisticFunctions(function_t::E_PopulationLowCounts).empty());
    CPPUNIT_ASSERT(function_t::statisticFunctions(function_t::E_PopulationHighCounts).empty());
    CPPUNIT_ASSERT(function_t::statisticFunctions(function_t::E_PopulationDistinctCount).empty());
    CPPUNIT_ASSERT(function_t::statisticFunctions(function_t::E_PopulationInfoContent).empty());

    {
        // For E_IndividualMetric
        function_t::TFunctionVec functions = function_t::statisticFunctions(
                function_t::E_IndividualMetric);
        function_t::EFunction expected[] = {function_t::E_IndividualMetricMean,
                                            function_t::E_IndividualMetricMin,
                                            function_t::E_IndividualMetricMax};
        CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(expected),
                core::CContainerPrinter::print(functions));
    }
    {
        // For E_IndividualMetricMean
        function_t::TFunctionVec functions = function_t::statisticFunctions(
                function_t::E_IndividualMetricMean);
        function_t::EFunction expected[] = {function_t::E_IndividualMetricMean};
        CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(expected),
                core::CContainerPrinter::print(functions));
    }
    {
        // For E_IndividualMetricMedian
        function_t::TFunctionVec functions = function_t::statisticFunctions(
                function_t::E_IndividualMetricMedian);
        function_t::EFunction expected[] = {function_t::E_IndividualMetricMedian};
        CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(expected),
                core::CContainerPrinter::print(functions));
    }
    {
        // For E_IndividualMetricMin
        function_t::TFunctionVec functions = function_t::statisticFunctions(
                function_t::E_IndividualMetricMin);
        function_t::EFunction expected[] = {function_t::E_IndividualMetricMin};
        CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(expected),
                core::CContainerPrinter::print(functions));
    }
    {
        // For E_IndividualMetricMax
        function_t::TFunctionVec functions = function_t::statisticFunctions(
                function_t::E_IndividualMetricMax);
        function_t::EFunction expected[] = {function_t::E_IndividualMetricMax};
        CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(expected),
                core::CContainerPrinter::print(functions));
    }
    {
        // For E_IndividualMetricSum
        function_t::TFunctionVec functions = function_t::statisticFunctions(
                function_t::E_IndividualMetricSum);
        function_t::EFunction expected[] = {function_t::E_IndividualMetricSum};
        CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(expected),
                core::CContainerPrinter::print(functions));
    }
    {
        // For E_IndividualMetricNonNullSum
        function_t::TFunctionVec functions = function_t::statisticFunctions(
                function_t::E_IndividualMetricNonNullSum);
        function_t::EFunction expected[] = {function_t::E_IndividualMetricSum};
        CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(expected),
                core::CContainerPrinter::print(functions));
    }
    {
        // For E_IndividualMetricLowMean
        function_t::TFunctionVec functions = function_t::statisticFunctions(
                function_t::E_IndividualMetricLowMean);
        function_t::EFunction expected[] = {function_t::E_IndividualMetricMean};
        CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(expected),
                core::CContainerPrinter::print(functions));
    }
    {
        // For E_IndividualMetricHighMean
        function_t::TFunctionVec functions = function_t::statisticFunctions(
                function_t::E_IndividualMetricHighMean);
        function_t::EFunction expected[] = {function_t::E_IndividualMetricMean};
        CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(expected),
                core::CContainerPrinter::print(functions));
    }
    {
        // For E_IndividualMetricLowNonNullSum
        function_t::TFunctionVec functions = function_t::statisticFunctions(
                function_t::E_IndividualMetricLowNonNullSum);
        function_t::EFunction expected[] = {function_t::E_IndividualMetricSum};
        CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(expected),
                core::CContainerPrinter::print(functions));
    }
    {
        // For E_IndividualMetricHighNonNullSum
        function_t::TFunctionVec functions = function_t::statisticFunctions(
                function_t::E_IndividualMetricHighNonNullSum);
        function_t::EFunction expected[] = {function_t::E_IndividualMetricSum};
        CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(expected),
                core::CContainerPrinter::print(functions));
    }
    {
        // For E_IndividualMeanLatLong
        function_t::TFunctionVec functions = function_t::statisticFunctions(
                function_t::E_IndividualLatLong);
        function_t::EFunction expected[] = {function_t::E_IndividualMetricMean};
        CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(expected),
                core::CContainerPrinter::print(functions));
    }
    {
        // For E_PopulationMetric
        function_t::TFunctionVec functions = function_t::statisticFunctions(
                function_t::E_PopulationMetric);
        function_t::EFunction expected[] = {function_t::E_PopulationMetricMean,
                                            function_t::E_PopulationMetricMin,
                                            function_t::E_PopulationMetricMax};
        CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(expected),
                core::CContainerPrinter::print(functions));
    }
    {
        // For E_PopulationMetricMean
        function_t::TFunctionVec functions = function_t::statisticFunctions(
                function_t::E_PopulationMetricMean);
        function_t::EFunction expected[] = {function_t::E_PopulationMetricMean};
        CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(expected),
                core::CContainerPrinter::print(functions));
    }
    {
        // For E_PopulationMetricMedian
        function_t::TFunctionVec functions = function_t::statisticFunctions(
                function_t::E_PopulationMetricMedian);
        function_t::EFunction expected[] = {function_t::E_PopulationMetricMedian};
        CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(expected),
                core::CContainerPrinter::print(functions));
    }
    {
        // For E_PopulationMetricMin
        function_t::TFunctionVec functions = function_t::statisticFunctions(
                function_t::E_PopulationMetricMin);
        function_t::EFunction expected[] = {function_t::E_PopulationMetricMin};
        CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(expected),
                core::CContainerPrinter::print(functions));
    }
    {
        // For E_PopulationMetricMax
        function_t::TFunctionVec functions = function_t::statisticFunctions(
                function_t::E_PopulationMetricMax);
        function_t::EFunction expected[] = {function_t::E_PopulationMetricMax};
        CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(expected),
                core::CContainerPrinter::print(functions));
    }
    {
        // For E_PopulationMetricSum
        function_t::TFunctionVec functions = function_t::statisticFunctions(
                function_t::E_PopulationMetricSum);
        function_t::EFunction expected[] = {function_t::E_PopulationMetricSum};
        CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(expected),
                core::CContainerPrinter::print(functions));
    }
    {
        // For E_PopulationMetricLowMean
        function_t::TFunctionVec functions = function_t::statisticFunctions(
                function_t::E_PopulationMetricLowMean);
        function_t::EFunction expected[] = {function_t::E_PopulationMetricMean};
        CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(expected),
                core::CContainerPrinter::print(functions));
    }
    {
        // For E_PopulationMetricHighMean
        function_t::TFunctionVec functions = function_t::statisticFunctions(
                function_t::E_PopulationMetricHighMean);
        function_t::EFunction expected[] = {function_t::E_PopulationMetricMean};
        CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(expected),
                core::CContainerPrinter::print(functions));
    }
}

CppUnit::Test* CFunctionTypesTest::suite(void)
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CFunctionTypesTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CFunctionTypesTest>(
                                   "CFunctionTypesTest::testFeaturesToFunction",
                                   &CFunctionTypesTest::testFeaturesToFunction) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CFunctionTypesTest>(
                                   "CFunctionTypesTest::testStatisticFunctions",
                                   &CFunctionTypesTest::testStatisticFunctions) );

    return suiteOfTests;
}
