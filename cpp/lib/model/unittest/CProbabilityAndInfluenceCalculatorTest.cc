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

#include "CProbabilityAndInfluenceCalculatorTest.h"

#include <core/CLogger.h>

#include <maths/CMultivariateNormalConjugate.h>
#include <maths/CMultivariateNormalConjugateFactory.h>
#include <maths/CNormalMeanPrecConjugate.h>
#include <maths/CTimeSeriesDecomposition.h>

#include <model/CPartitioningFields.h>
#include <model/CProbabilityAndInfluenceCalculator.h>
#include <model/CStringStore.h>

#include <test/CRandomNumbers.h>

#include <boost/range.hpp>
#include <boost/scoped_ptr.hpp>

#include <string>
#include <utility>
#include <vector>

using namespace prelert;

namespace
{

typedef std::pair<double, double> TDoubleDoublePr;
typedef std::vector<double> TDoubleVec;
typedef std::vector<TDoubleVec> TDoubleVecVec;
typedef core::CSmallVector<double, 1> TDouble1Vec;
typedef core::CSmallVector<double, 4> TDouble4Vec;
typedef core::CSmallVector<double, 10> TDouble10Vec;
typedef core::CSmallVector<TDouble4Vec, 1> TDouble4Vec1Vec;
typedef core::CSmallVector<TDouble10Vec, 1> TDouble10Vec1Vec;
typedef core::CSmallVector<TDouble10Vec, 2> TDouble10Vec2Vec;
typedef core::CSmallVector<TDouble10Vec, 4> TDouble10Vec4Vec;
typedef core::CSmallVector<TDouble10Vec4Vec, 1> TDouble10Vec4Vec1Vec;
typedef core::CSmallVector<std::size_t, 1> TSize1Vec;
typedef core::CSmallVector<std::size_t, 10> TSize10Vec;
typedef model::CProbabilityAndInfluenceCalculator::TTime2Vec TTime2Vec;
typedef core::CSmallVector<maths_t::ETail, 10> TTail10Vec;
typedef model::CProbabilityAndInfluenceCalculator::TStrCRef TStrCRef;
typedef model::CProbabilityAndInfluenceCalculator::TStrPtr TStrPtr;
typedef model::CProbabilityAndInfluenceCalculator::TDouble1VecDoublePr TDouble1VecDoublePr;
typedef model::CProbabilityAndInfluenceCalculator::TDouble1VecDouble1VecPr TDouble1VecDouble1VecPr;
typedef model::CProbabilityAndInfluenceCalculator::TStrCRefDouble1VecDoublePrPr TStrCRefDouble1VecDoublePrPr;
typedef model::CProbabilityAndInfluenceCalculator::TStrCRefDouble1VecDoublePrPrVec TStrCRefDouble1VecDoublePrPrVec;
typedef model::CProbabilityAndInfluenceCalculator::TStrCRefDouble1VecDouble1VecPrPr TStrCRefDouble1VecDouble1VecPrPr;
typedef model::CProbabilityAndInfluenceCalculator::TStrCRefDouble1VecDouble1VecPrPrVec TStrCRefDouble1VecDouble1VecPrPrVec;
typedef model::CProbabilityAndInfluenceCalculator::TStrPtrStrPtrPrDoublePrVec TStrPtrStrPtrPrDoublePrVec;
typedef model::CProbabilityAndInfluenceCalculator::TMultivariatePriorCPtrSizePr TMultivariatePriorCPtrSizePr;
typedef boost::shared_ptr<maths::CTimeSeriesDecompositionInterface> TDecompositionPtr;
typedef std::vector<TDecompositionPtr> TDecompositionPtrVec;
typedef TDecompositionPtrVec::const_iterator TDecompositionPtrVecCItr;
typedef model::CProbabilityAndInfluenceCalculator::TDecompositionCPtr1Vec TDecompositionCPtr1Vec;
typedef model::CProbabilityAndInfluenceCalculator::TDecompositionCPtr TDecompositionCPtr;
typedef model::CProbabilityAndInfluenceCalculator::TDecompositionCPtrVec TDecompositionCPtrVec;
typedef boost::shared_ptr<const model::CInfluenceCalculator> TInfluenceCalculatorCPtr;

const std::string EMPTY_STRING;

TDouble1VecDoublePr make_pair(double first, double second)
{
    return TDouble1VecDoublePr(TDouble1Vec(1, first), second);
}

TDouble1VecDoublePr make_pair(double first1, double first2, double second)
{
    TDouble1Vec first;
    first.push_back(first1);
    first.push_back(first2);
    return TDouble1VecDoublePr(first, second);
}

TDouble1VecDouble1VecPr make_pair(double first1, double first2, double second1, double second2)
{
    TDouble1Vec first;
    first.push_back(first1);
    first.push_back(first2);
    TDouble1Vec second;
    second.push_back(second1);
    second.push_back(second2);
    return TDouble1VecDouble1VecPr(first, second);
}

const std::string I("I");
const std::string i1("i1");
const std::string i2("i2");
const std::string i3("i3");
const maths_t::TWeightStyleVec COUNT_WEIGHT(1, maths_t::E_SampleCountWeight);
const TSize1Vec NO_MOST_ANOMALOUS_CORRELATE;

template<typename CALCULATOR>
void computeInfluences(CALCULATOR &calculator,
                       model_t::EFeature feature,
                       const TDecompositionCPtr &trend,
                       const maths::CPrior &prior,
                       core_t::TTime time,
                       double value,
                       double varianceScale,
                       double count,
                       double probability,
                       maths_t::ETail tail,
                       double confidence,
                       const std::string &influencerName,
                       const TStrCRefDouble1VecDoublePrPrVec &influencerValues,
                       TStrPtrStrPtrPrDoublePrVec &result)
{
    maths_t::TWeightStyleVec weightStyles;
    weightStyles.push_back(maths_t::E_SampleSeasonalVarianceScaleWeight);
    weightStyles.push_back(maths_t::E_SampleCountVarianceScaleWeight);
    model::CPartitioningFields partitioningFields(EMPTY_STRING, EMPTY_STRING);
    TDouble1Vec value_(1, value);
    TDouble4Vec1Vec weight(1, TDouble4Vec(2, 1.0));
    weight[0][0] = varianceScale;
    model::CProbabilityAndInfluenceCalculator::SParams params(weightStyles, partitioningFields);
    params.s_Feature = feature;
    if (trend)
    {
        params.s_Trend.assign(1, trend.get());
    }
    params.s_Prior = &prior;
    params.s_Time = time;
    params.s_Value = value_;
    params.s_Count = count;
    params.s_Weights = weight;
    params.s_Probability = probability;
    params.s_Tail = tail;
    params.s_Confidence = confidence;
    params.s_InfluencerName = model::CStringStore::influencers().get(influencerName);
    params.s_InfluencerValues = influencerValues;
    params.s_Cutoff = 0.5;
    calculator.computeInfluences(params);
    result.swap(params.s_Influences);
}

template<typename CALCULATOR>
void computeInfluences(CALCULATOR &calculator,
                       model_t::EFeature feature,
                       const TDecompositionCPtr1Vec &trends,
                       const maths::CMultivariatePrior &prior,
                       const core_t::TTime (&times)[2],
                       const double (&values)[2],
                       const TDouble10Vec4Vec1Vec &weights,
                       const double (&counts)[2],
                       double probability,
                       maths_t::ETail tail,
                       std::size_t coordinate,
                       double confidence,
                       const std::string &influencerName,
                       const TStrCRefDouble1VecDouble1VecPrPrVec &influencerValues,
                       TStrPtrStrPtrPrDoublePrVec &result)
{
    maths_t::TWeightStyleVec weightStyles;
    weightStyles.push_back(maths_t::E_SampleSeasonalVarianceScaleWeight);
    weightStyles.push_back(maths_t::E_SampleCountVarianceScaleWeight);
    model::CPartitioningFields partitioningFields(EMPTY_STRING, EMPTY_STRING);
    TTime2Vec times_(&times[0], &times[2]);
    TDouble10Vec values_(&values[0], &values[2]);
    TDouble10Vec counts_(&counts[0], &counts[2]);
    model::CProbabilityAndInfluenceCalculator::SCorrelateParams params(weightStyles, partitioningFields);
    params.s_Feature = feature;
    params.s_Trends.push_back(trends);
    params.s_Priors.push_back(TMultivariatePriorCPtrSizePr(&prior, coordinate));
    params.s_ElapsedTimes.push_back(TTime2Vec(static_cast<std::size_t>(2), 0));
    params.s_Times.push_back(times_);
    params.s_Values.push_back(values_);
    params.s_Counts.push_back(counts_);
    params.s_Weights = weights;
    params.s_Weights[0].resize(weightStyles.size(), TDouble10Vec(2, 1.0));
    params.s_Probability = probability;
    params.s_Tail = tail;
    params.s_MostAnomalousCorrelate.push_back(0);
    params.s_Confidence = confidence;
    params.s_InfluencerName = model::CStringStore::influencers().get(influencerName);
    params.s_InfluencerValues = influencerValues;
    params.s_Cutoff = 0.5;
    calculator.computeInfluences(params);
    result.swap(params.s_Influences);
}

template<std::size_t N, std::size_t P>
void univariateTestProbabilityAndGetInfluences(model_t::EFeature feature,
                                               const maths::CNormalMeanPrecConjugate &prior,
                                               double values[N][2],
                                               TStrCRefDouble1VecDoublePrPr influencerValues[N][P],
                                               TStrPtrStrPtrPrDoublePrVec &influences)
{
    LOG_DEBUG("univariate");

    maths_t::TWeightStyleVec weightStyles;
    weightStyles.push_back(maths_t::E_SampleSeasonalVarianceScaleWeight);
    weightStyles.push_back(maths_t::E_SampleCountVarianceScaleWeight);
    model::CPartitioningFields partitioningFields(EMPTY_STRING, EMPTY_STRING);

    model::CProbabilityAndInfluenceCalculator calculator(0.3);
    calculator.addAggregator(maths::CJointProbabilityOfLessLikelySamples());
    calculator.addAggregator(maths::CProbabilityOfExtremeSample());
    TInfluenceCalculatorCPtr influenceCalculator(model_t::influenceCalculator(feature));
    calculator.plugin(*influenceCalculator);

    maths::CJointProbabilityOfLessLikelySamples pJoint;
    maths::CProbabilityOfExtremeSample pExtreme;

    for (std::size_t i = 0u; i < N; ++i)
    {
        TDouble1Vec value(1, values[i][0]);
        TDouble4Vec1Vec weight(1, TDouble4Vec(2, 1.0));
        weight[0][0] = values[i][1];

        double p = 0.0;
        maths_t::ETail tail;
        calculator.addProbability(feature,
                                  prior,
                                  0/*elapsedTime*/,
                                  weightStyles, value, weight,
                                  false, 0.0, p, tail);
        LOG_DEBUG("  p = " << p);

        double count = 0.0;
        for (std::size_t j = 0u; j < P; ++j)
        {
            count += influencerValues[i][j].second.second;
        }

        pJoint.add(p);
        pExtreme.add(p);
        model::CProbabilityAndInfluenceCalculator::SParams params(weightStyles, partitioningFields);
        params.s_Feature = feature;
        params.s_Prior = &prior;
        params.s_Value = value;
        params.s_Count = count;
        params.s_Weights = weight;
        params.s_Probability = p;
        params.s_Tail = tail;
        calculator.addInfluences(I, TStrCRefDouble1VecDoublePrPrVec(&influencerValues[i][0],
                                                                    &influencerValues[i][P]), params);
    }

    double probability;
    CPPUNIT_ASSERT(calculator.calculate(probability, influences));

    double pj, pe;
    CPPUNIT_ASSERT(pJoint.calculate(pj));
    CPPUNIT_ASSERT(pExtreme.calculate(pe));

    LOG_DEBUG("  probability = " << probability
              << ", expected probability = " << std::min(pj, pe));
    CPPUNIT_ASSERT_DOUBLES_EQUAL(std::min(pe, pj), probability, 1e-10);
}

template<std::size_t N, std::size_t P>
void latLongTestProbabilityAndGetInfluences(const maths::CMultivariateNormalConjugate<2> &prior,
                                            double values[N][3],
                                            TStrCRefDouble1VecDoublePrPr influencerValues[N][P],
                                            TStrPtrStrPtrPrDoublePrVec &influences)
{
    LOG_DEBUG("multivariate");

    maths_t::TWeightStyleVec weightStyles;
    weightStyles.push_back(maths_t::E_SampleSeasonalVarianceScaleWeight);
    weightStyles.push_back(maths_t::E_SampleCountVarianceScaleWeight);
    model::CPartitioningFields partitioningFields(EMPTY_STRING, EMPTY_STRING);

    model::CProbabilityAndInfluenceCalculator calculator(0.3);
    calculator.addAggregator(maths::CJointProbabilityOfLessLikelySamples());
    calculator.addAggregator(maths::CProbabilityOfExtremeSample());
    TInfluenceCalculatorCPtr influenceCalculator(
            model_t::influenceCalculator(model_t::E_IndividualMeanLatLongByPerson));
    calculator.plugin(*influenceCalculator);

    maths::CJointProbabilityOfLessLikelySamples pJoint;
    maths::CProbabilityOfExtremeSample pExtreme;

    for (std::size_t i = 0u; i < N; ++i)
    {
        TDouble10Vec1Vec value(1, TDouble10Vec(&values[i][0], &values[i][2]));
        TDouble10Vec4Vec1Vec weight(1, TDouble10Vec4Vec(2, TDouble10Vec(2, 1.0)));
        weight[0][0][0] = values[i][2];
        weight[0][1][0] = values[i][2];

        double p = 0.0;
        TTail10Vec tail;
        calculator.addProbability(model_t::E_IndividualMeanLatLongByPerson,
                                  prior,
                                  0/*elapsedTime*/,
                                  weightStyles, value, weight,
                                  false, 0.0, p, tail);
        LOG_DEBUG("  p = " << p);

        double count = 0.0;
        for (std::size_t j = 0u; j < P; ++j)
        {
            count += influencerValues[i][j].second.second;
        }

        pJoint.add(p);
        pExtreme.add(p);
        model::CProbabilityAndInfluenceCalculator::SMultivariateParams params(weightStyles, partitioningFields);
        params.s_Feature = model_t::E_IndividualMeanLatLongByPerson;
        params.s_Prior = &prior;
        params.s_Value = value[0];
        params.s_Count = count;
        params.s_Weights = weight;
        params.s_Probability = p;
        params.s_Tail = tail;
        calculator.addInfluences(I, TStrCRefDouble1VecDoublePrPrVec(&influencerValues[i][0],
                                                                    &influencerValues[i][P]), params);
    }

    double probability;
    CPPUNIT_ASSERT(calculator.calculate(probability, influences));

    double pj, pe;
    CPPUNIT_ASSERT(pJoint.calculate(pj));
    CPPUNIT_ASSERT(pExtreme.calculate(pe));

    LOG_DEBUG("  probability = " << probability
              << ", expected probability = " << std::min(pj, pe));
    CPPUNIT_ASSERT_DOUBLES_EQUAL(std::min(pe, pj), probability, 1e-10);
}

}

void CProbabilityAndInfluenceCalculatorTest::testInfluenceUnavailableCalculator(void)
{
    LOG_DEBUG("*** testInfluenceUnavailableCalculator ***");

    test::CRandomNumbers rng;

    {
        LOG_DEBUG("Test univariate");

        model::CInfluenceUnavailableCalculator calculator;
        maths::CNormalMeanPrecConjugate prior =
                maths::CNormalMeanPrecConjugate::nonInformativePrior(maths_t::E_ContinuousData);

        TDoubleVec samples;
        rng.generateNormalSamples(10.0, 1.0, 50, samples);
        TDouble4Vec1Vec weights(samples.size(), TDouble4Vec(1, 1.0));
        prior.addSamples(COUNT_WEIGHT, samples, weights);

        TStrCRefDouble1VecDoublePrPrVec influencerValues;
        influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(11.0, 1.0)));
        influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair(11.0, 1.0)));
        influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i3), make_pair(15.0, 1.0)));

        TStrPtrStrPtrPrDoublePrVec influences;
        computeInfluences(calculator,
                          model_t::E_IndividualLowCountsByBucketAndPerson, TDecompositionCPtr(), prior,
                          0/*time*/, 15.0/*value*/, 1.0/*varianceScale*/, 1.0/*count*/,
                          0.001/*probability*/, maths_t::E_RightTail, 0.0/*confidence*/,
                          I, influencerValues, influences);

        LOG_DEBUG("influences = " << core::CContainerPrinter::print(influences));
        CPPUNIT_ASSERT(influences.empty());
    }
    {
        LOG_DEBUG("Test correlated");

        model::CInfluenceUnavailableCalculator calculator;

        maths::CMultivariateNormalConjugateFactory::TPriorPtr prior =
                maths::CMultivariateNormalConjugateFactory::nonInformative(2, maths_t::E_ContinuousData, 0.0);

        TDoubleVec samples_;
        rng.generateNormalSamples(10.0, 1.0, 50, samples_);
        TDouble10Vec1Vec samples;
        for (std::size_t i = 0u; i < samples_.size(); ++i)
        {
            samples.push_back(TDouble10Vec(2, samples_[i]));
        }
        TDouble10Vec4Vec1Vec weights(samples.size(), TDouble10Vec4Vec(1, TDouble10Vec(2, 1.0)));
        prior->addSamples(COUNT_WEIGHT, samples, weights);

        core_t::TTime times[] = { 0, 0 };
        double values[] = { 15.0, 15.0 };
        double counts[] = { 1.0, 1.0 };
        TStrCRefDouble1VecDouble1VecPrPrVec influencerValues;
        influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i1), make_pair(11.0, 11.0, 1.0, 1.0)));
        influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i2), make_pair(11.0, 11.0, 1.0, 1.0)));
        influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i3), make_pair(15.0, 15.0, 1.0, 1.0)));

        TStrPtrStrPtrPrDoublePrVec influences;
        computeInfluences(calculator,
                          model_t::E_IndividualLowCountsByBucketAndPerson, TDecompositionCPtr1Vec(), *prior,
                          times, values, TDouble10Vec4Vec1Vec(1, TDouble10Vec4Vec(1, TDouble10Vec(2, 1.0))), counts,
                          0.1/*probability*/, maths_t::E_RightTail, 0, 0.0/*confidence*/,
                          I, influencerValues, influences);

        LOG_DEBUG("influences = " << core::CContainerPrinter::print(influences));
        CPPUNIT_ASSERT(influences.empty());
    }
}

void CProbabilityAndInfluenceCalculatorTest::testLogProbabilityComplementInfluenceCalculator(void)
{
    LOG_DEBUG("*** testLogProbabilityComplementInfluenceCalculator ***");

    test::CRandomNumbers rng;

    maths_t::TWeightStyleVec weightStyle(1, maths_t::E_SampleSeasonalVarianceScaleWeight);

    model::CLogProbabilityComplementInfluenceCalculator calculator;

    {
        LOG_DEBUG("Test univariate");
        {
            LOG_DEBUG("One influencer value");

            maths::CNormalMeanPrecConjugate prior =
                    maths::CNormalMeanPrecConjugate::nonInformativePrior(maths_t::E_ContinuousData);

            TDoubleVec samples;
            rng.generateNormalSamples(10.0, 1.0, 50, samples);
            TDouble4Vec1Vec weights(samples.size(), TDouble4Vec(1, 1.0));
            prior.addSamples(COUNT_WEIGHT, samples, weights);

            double lb, ub;
            maths_t::ETail tail;
            TDouble1Vec sample(1, 20.0);
            TDouble4Vec1Vec weight(1, TDouble4Vec(1, 1.0));
            prior.probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                 weightStyle,
                                                 sample,
                                                 weight,
                                                 lb, ub, tail);

            TStrCRefDouble1VecDoublePrPrVec influencerValues;
            influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(10.0, 1.0)));

            TStrPtrStrPtrPrDoublePrVec influences;
            computeInfluences(calculator,
                              model_t::E_IndividualCountByBucketAndPerson, TDecompositionCPtr(), prior,
                              0/*time*/, 20.0/*value*/, 1.0/*varianceScale*/, 1.0/*count*/,
                              0.5*(lb+ub), tail, 0.0/*confidence*/,
                              I, influencerValues, influences);

            LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
            CPPUNIT_ASSERT_EQUAL(std::string("[((I, i1), 1)]"),
                                 core::CContainerPrinter::print(influences));
        }
        {
            LOG_DEBUG("No trend");

            maths::CNormalMeanPrecConjugate prior =
                    maths::CNormalMeanPrecConjugate::nonInformativePrior(maths_t::E_ContinuousData);

            TDoubleVec samples;
            rng.generateNormalSamples(10.0, 1.0, 50, samples);
            TDouble4Vec1Vec weights(samples.size(), TDouble4Vec(1, 1.0));
            prior.addSamples(COUNT_WEIGHT, samples, weights);

            double lb, ub;
            maths_t::ETail tail;
            TDouble1Vec sample(1, 20.0);
            TDouble4Vec1Vec weight(1, TDouble4Vec(1, 1.0));
            prior.probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                 weightStyle,
                                                 sample,
                                                 weight,
                                                 lb, ub, tail);

            TStrCRefDouble1VecDoublePrPrVec influencerValues;
            influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair( 1.0, 1.0)));
            influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair( 1.0, 1.0)));
            influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i3), make_pair(18.0, 1.0)));

            TStrPtrStrPtrPrDoublePrVec influences;
            computeInfluences(calculator,
                              model_t::E_IndividualCountByBucketAndPerson, TDecompositionCPtr(), prior,
                              0/*time*/, 20.0/*value*/, 1.0/*varianceScale*/, 1.0/*count*/,
                              0.5*(lb+ub), tail, 0.0/*confidence*/,
                              I, influencerValues, influences);

            LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
            CPPUNIT_ASSERT_EQUAL(std::string("[((I, i3), 1)]"),
                                 core::CContainerPrinter::print(influences));
        }
        {
            LOG_DEBUG("Trend");

            TDoubleVec samples;
            rng.generateNormalSamples(0.0, 100.0, 10 * 86400 / 600, samples);
            TDouble4Vec1Vec weights(samples.size(), TDouble4Vec(1, 1.0));

            TDecompositionPtr trend(new maths::CTimeSeriesDecomposition);
            maths::CNormalMeanPrecConjugate prior =
                    maths::CNormalMeanPrecConjugate::nonInformativePrior(maths_t::E_ContinuousData);

            for (core_t::TTime time = 0, i = 0; time < 10 * 86400; time += 600, ++i)
            {
                double y = 100.0 + 100.0 * ::sin(2.0 * 3.1416 * static_cast<double>(time) / 86400.0) + samples[i];
                trend->addPoint(time, y);
            }
            prior.addSamples(COUNT_WEIGHT, samples, weights);

            core_t::TTime testTimes[] = { 0, 86400 / 4, 86400 / 2, (3 * 86400) / 4 };

            TStrCRefDouble1VecDoublePrPrVec influencerValues;
            influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(70.0, 1.0)));
            influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair(50.0, 1.0)));

            std::string expectedInfluencerValues[] = { "i1", "i2" };
            double expectedInfluences[][2] =
                {
                    { 1.0, 1.0 },
                    { 0.0, 0.0 },
                    { 1.0, 1.0 },
                    { 0.8, 0.6 }
                };

            for (std::size_t i = 0u; i < boost::size(testTimes); ++i)
            {
                core_t::TTime time = testTimes[i];
                LOG_DEBUG("  time = " << time);
                LOG_DEBUG("  baseline = " << core::CContainerPrinter::print(trend->baseline(time, 0.0)));

                double detrended = trend->detrend(time, 120.0, 0.0);
                TDoubleDoublePr vs = trend->scale(time, prior.marginalLikelihoodVariance(), 0.0);
                LOG_DEBUG("  detrended = " << detrended
                          << ", vs = " << core::CContainerPrinter::print(vs));

                double lb, ub;
                maths_t::ETail tail;
                TDouble1Vec sample(1, detrended);
                TDouble4Vec1Vec weight(1, TDouble4Vec(1, vs.second));
                prior.probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                     weightStyle,
                                                     sample,
                                                     weight,
                                                     lb, ub, tail);
                LOG_DEBUG("  p = " << 0.5*(lb+ub) << ", tail = " << tail);

                TStrPtrStrPtrPrDoublePrVec influences;
                computeInfluences(calculator,
                                  model_t::E_IndividualCountByBucketAndPerson, trend, prior,
                                  time, 120.0/*value*/, vs.second, 1.0/*count*/,
                                  0.5*(lb+ub), tail, 0.0/*confidence*/,
                                  I, influencerValues, influences);

                LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
                for (std::size_t j = 0u; j < influences.size(); ++j)
                {
                    CPPUNIT_ASSERT_EQUAL(expectedInfluencerValues[j], *influences[j].first.second);
                    CPPUNIT_ASSERT_DOUBLES_EQUAL(expectedInfluences[i][j], influences[j].second, 0.06);
                }
            }
        }
    }
    {
        LOG_DEBUG("Test correlated");

        double counts[] = { 1.0, 1.0 };

        {
            LOG_DEBUG("One influencer value");

            maths::CMultivariateNormalConjugateFactory::TPriorPtr prior =
                    maths::CMultivariateNormalConjugateFactory::nonInformative(2, maths_t::E_ContinuousData, 0.0);

            TDoubleVec mean(2, 10.0);
            TDoubleVecVec covariances(2, TDoubleVec(2));
            covariances[0][0] = covariances[1][1] = 5.0;
            covariances[0][1] = covariances[1][0] = 4.0;
            TDoubleVecVec samples_;
            rng.generateMultivariateNormalSamples(mean, covariances, 50, samples_);
            TDouble10Vec1Vec samples;
            for (std::size_t i = 0u; i < samples_.size(); ++i)
            {
                samples.push_back(samples_[i]);
            }
            TDouble10Vec4Vec1Vec weights(samples.size(), TDouble10Vec4Vec(1, TDouble10Vec(2, 1.0)));
            prior->addSamples(COUNT_WEIGHT, samples, weights);

            core_t::TTime times[] = { 0, 0 };
            double values[] = { 15.0, 15.0 };
            double vs[] = { 1.0, 1.0 };
            double lb, ub;
            TTail10Vec tail;
            TDouble10Vec1Vec sample(1, TDouble10Vec(&values[0], &values[2]));
            TDouble10Vec4Vec1Vec weight(1, TDouble10Vec4Vec(1, TDouble10Vec(&vs[0], &vs[2])));
            prior->probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                  weightStyle,
                                                  sample,
                                                  weight,
                                                  lb, ub, tail);
            TStrCRefDouble1VecDouble1VecPrPrVec influencerValues;
            influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i1), make_pair(15.0, 15.0, 1.0, 1.0)));

            TStrPtrStrPtrPrDoublePrVec influences;
            computeInfluences(calculator,
                              model_t::E_IndividualCountByBucketAndPerson, TDecompositionCPtr1Vec(), *prior,
                              times, values, weight, counts,
                              0.5*(lb+ub), tail[0], 0, 0.0/*confidence*/,
                              I, influencerValues, influences);

            LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
            CPPUNIT_ASSERT_EQUAL(std::string("[((I, i1), 1)]"), core::CContainerPrinter::print(influences));
        }
        {
            LOG_DEBUG("No trend");

            maths::CMultivariateNormalConjugateFactory::TPriorPtr prior =
                    maths::CMultivariateNormalConjugateFactory::nonInformative(2, maths_t::E_ContinuousData, 0.0);

            TDoubleVec mean(2, 10.0);
            TDoubleVecVec covariances(2, TDoubleVec(2));
            covariances[0][0] = covariances[1][1] = 5.0;
            covariances[0][1] = covariances[1][0] = 4.0;
            TDoubleVecVec samples_;
            rng.generateMultivariateNormalSamples(mean, covariances, 50, samples_);
            TDouble10Vec1Vec samples;
            for (std::size_t i = 0u; i < samples_.size(); ++i)
            {
                samples.push_back(samples_[i]);
            }
            TDouble10Vec4Vec1Vec weights(samples.size(), TDouble10Vec4Vec(1, TDouble10Vec(2, 1.0)));
            prior->addSamples(COUNT_WEIGHT, samples, weights);

            core_t::TTime times[] = { 0, 0 };
            double values[] = { 20.0, 10.0 };
            double vs[] = { 1.0, 1.0 };
            TSize10Vec coordinates(std::size_t(1), 0);
            TDouble10Vec2Vec lbs, ubs;
            TTail10Vec tail;
            TDouble10Vec1Vec sample(1, TDouble10Vec(&values[0], &values[2]));
            TDouble10Vec4Vec1Vec weight(1, TDouble10Vec4Vec(1, TDouble10Vec(&vs[0], &vs[2])));
            prior->probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                  weightStyle,
                                                  sample,
                                                  weight,
                                                  coordinates,
                                                  lbs, ubs, tail);
            double lb = ::sqrt(lbs[0][0] * lbs[1][0]);
            double ub = ::sqrt(ubs[0][0] * ubs[1][0]);
            TStrCRefDouble1VecDouble1VecPrPrVec influencerValues;
            influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i1), make_pair( 1.0, 1.0, 1.0, 1.0)));
            influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i2), make_pair( 1.0, 1.0, 1.0, 1.0)));
            influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i3), make_pair(18.0, 8.0, 1.0, 1.0)));

            TStrPtrStrPtrPrDoublePrVec influences;
            computeInfluences(calculator,
                              model_t::E_IndividualCountByBucketAndPerson, TDecompositionCPtr1Vec(), *prior,
                              times, values, weight, counts,
                              0.5*(lb+ub), tail[0], coordinates[0], 0.0/*confidence*/,
                              I, influencerValues, influences);

            LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
            CPPUNIT_ASSERT_EQUAL(std::string("[((I, i3), 1)]"), core::CContainerPrinter::print(influences));
        }
        {
            LOG_DEBUG("Trend");

            TDecompositionPtrVec trend;
            trend.push_back(TDecompositionPtr(new maths::CTimeSeriesDecomposition));
            trend.push_back(TDecompositionPtr(new maths::CTimeSeriesDecomposition));
            maths::CMultivariateNormalConjugateFactory::TPriorPtr prior =
                    maths::CMultivariateNormalConjugateFactory::nonInformative(2, maths_t::E_ContinuousData, 0.0);
            {
                TDoubleVec mean(2, 0.0);
                TDoubleVecVec covariances(2, TDoubleVec(2));
                covariances[0][0] = covariances[1][1] = 100.0;
                covariances[0][1] = covariances[1][0] =  80.0;
                TDoubleVecVec samples;
                rng.generateMultivariateNormalSamples(mean, covariances, 10 * 86400 / 600, samples);
                TDouble10Vec4Vec1Vec weight(1, TDouble10Vec4Vec(1, TDouble10Vec(2, 1.0)));
                for (core_t::TTime time = 0, i = 0; time < 10 * 86400; time += 600, ++i)
                {
                    double y = 100.0 + 100.0 * ::sin(2.0 * 3.1416 * static_cast<double>(time) / 86400.0);
                    trend[0]->addPoint(time, y + samples[i][0]);
                    trend[1]->addPoint(time, y + samples[i][0]);
                    prior->addSamples(COUNT_WEIGHT, TDouble10Vec1Vec(1, TDouble10Vec(samples[i])), weight);
                }
            }

            core_t::TTime testTimes[] = { 0, 86400 / 4, 86400 / 2, (3 * 86400) / 4 };

            TStrCRefDouble1VecDouble1VecPrPrVec influencerValues;
            influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i1), make_pair(70.0, 70.0, 1.0, 1.0)));
            influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i2), make_pair(50.0, 50.0, 1.0, 1.0)));

            std::string expectedInfluencerValues[] = { "i1", "i2" };
            double expectedInfluences[][2] =
                {
                    { 1.0, 1.0 },
                    { 0.0, 0.0 },
                    { 1.0, 1.0 },
                    { 0.8, 0.65 }
                };

            for (std::size_t i = 0u; i < boost::size(testTimes); ++i)
            {
                core_t::TTime time = testTimes[i];
                LOG_DEBUG("  time = " << time);
                LOG_DEBUG("  baseline[0] = " << core::CContainerPrinter::print(trend[0]->baseline(time, 0.0)));
                LOG_DEBUG("  baseline[1] = " << core::CContainerPrinter::print(trend[1]->baseline(time, 0.0)));

                core_t::TTime times[] = { time, time };
                double values[] = { 120.0, 120.0 };
                double detrended[] =
                    {
                        trend[0]->detrend(time, values[0], 0.0),
                        trend[1]->detrend(time, values[1], 0.0)
                    };
                double vs[] =
                    {
                        trend[0]->scale(time, prior->marginalLikelihoodVariances()[0], 0.0).second,
                        trend[1]->scale(time, prior->marginalLikelihoodVariances()[1], 0.0).second
                    };
                LOG_DEBUG("  detrended = " << core::CContainerPrinter::print(detrended)
                          << ", vs = " << core::CContainerPrinter::print(vs));
                TSize10Vec coordinates(std::size_t(1), 0);
                TDouble10Vec2Vec lbs, ubs;
                TTail10Vec tail;
                TDouble10Vec1Vec sample(1, TDouble10Vec(&detrended[0], &detrended[2]));
                TDouble10Vec4Vec1Vec weight(1, TDouble10Vec4Vec(1, TDouble10Vec(&vs[0], &vs[2])));
                prior->probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                      weightStyle,
                                                      sample,
                                                      weight,
                                                      coordinates,
                                                      lbs, ubs, tail);
                double lb = ::sqrt(lbs[0][0] * lbs[1][0]);
                double ub = ::sqrt(ubs[0][0] * ubs[1][0]);
                LOG_DEBUG("  p = " << 0.5*(lb+ub) << ", tail = " << tail);

                TStrPtrStrPtrPrDoublePrVec influences;
                TDecompositionCPtr1Vec trends;
                for (TDecompositionPtrVecCItr itr = trend.begin(); itr != trend.end(); ++itr)
                {
                    trends.push_back(itr->get());
                }
                computeInfluences(calculator,
                                  model_t::E_IndividualCountByBucketAndPerson,
                                  trends, *prior,
                                  times, values, weight, counts,
                                  0.5*(lb+ub), tail[0], coordinates[0], 0.0/*confidence*/,
                                  I, influencerValues, influences);

                LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
                for (std::size_t j = 0u; j < influences.size(); ++j)
                {
                    CPPUNIT_ASSERT_EQUAL(expectedInfluencerValues[j], *influences[j].first.second);
                    CPPUNIT_ASSERT_DOUBLES_EQUAL(expectedInfluences[i][j], influences[j].second, 0.05);
                }
            }
        }
    }
}

void CProbabilityAndInfluenceCalculatorTest::testMeanInfluenceCalculator(void)
{
    LOG_DEBUG("*** testMeanInfluenceCalculator ***");

    test::CRandomNumbers rng;

    model::CMeanInfluenceCalculator calculator;

    maths_t::TWeightStyleVec weightStyles;
    weightStyles.push_back(maths_t::E_SampleSeasonalVarianceScaleWeight);
    weightStyles.push_back(maths_t::E_SampleCountVarianceScaleWeight);

    {
        LOG_DEBUG("Test univariate");
        {
            LOG_DEBUG("One influencer value");

            maths::CNormalMeanPrecConjugate prior =
                    maths::CNormalMeanPrecConjugate::nonInformativePrior(maths_t::E_ContinuousData);

            TDoubleVec samples;
            rng.generateNormalSamples(10.0, 1.0, 50, samples);
            TDouble4Vec1Vec weights(samples.size(), TDouble4Vec(1, 1.0));
            prior.addSamples(COUNT_WEIGHT, samples, weights);

            double lb, ub;
            maths_t::ETail tail;
            TDouble1Vec sample(1, 5.0);
            TDouble4Vec1Vec weight(1, TDouble4Vec(2 , 1.0));
            prior.probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                 weightStyles,
                                                 sample,
                                                 weight,
                                                 lb, ub, tail);
            TStrCRefDouble1VecDoublePrPrVec influencerValues;
            influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(5.0, 1.0)));

            TStrPtrStrPtrPrDoublePrVec influences;
            computeInfluences(calculator,
                              model_t::E_IndividualMeanByPerson, TDecompositionCPtr(), prior,
                              0/*time*/, 5.0/*value*/, 1.0/*varianceScale*/, 1.0/*count*/,
                              0.5*(lb+ub), tail, 0.0/*confidence*/,
                              I, influencerValues, influences);

            LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
            CPPUNIT_ASSERT_EQUAL(std::string("[((I, i1), 1)]"),
                                 core::CContainerPrinter::print(influences));
        }
        {
            LOG_DEBUG("No trend");

            maths::CNormalMeanPrecConjugate prior =
                    maths::CNormalMeanPrecConjugate::nonInformativePrior(maths_t::E_ContinuousData);

            TDoubleVec samples;
            rng.generateNormalSamples(10.2, 1.0, 50, samples);
            TDouble4Vec1Vec weights(samples.size(), TDouble4Vec(1, 1.0));
            prior.addSamples(COUNT_WEIGHT, samples, weights);

            {
                LOG_DEBUG("Right tail, one clear influence");

                double lb, ub;
                maths_t::ETail tail;
                TDouble1Vec sample(1, 12.5);
                TDouble4Vec1Vec weight(1, TDouble4Vec(2, 1.0));
                prior.probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                     weightStyles,
                                                     sample,
                                                     weight,
                                                     lb, ub, tail);
                TStrCRefDouble1VecDoublePrPrVec influencerValues;
                influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(20.0, 5.0)));
                influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair(10.0, 7.0)));
                influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i3), make_pair(10.0, 8.0)));

                TStrPtrStrPtrPrDoublePrVec influences;
                computeInfluences(calculator,
                                  model_t::E_IndividualMeanByPerson, TDecompositionCPtr(), prior,
                                  0/*time*/, 12.5/*value*/, 1.0/*varianceScale*/, 20.0/*count*/,
                                  0.5*(lb+ub), tail, 0.0/*confidence*/,
                                  I, influencerValues, influences);

                LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
                CPPUNIT_ASSERT_EQUAL(std::string("[((I, i1), 1)]"),
                                     core::CContainerPrinter::print(influences));
            }
            {
                LOG_DEBUG("Right tail, no clear influences");

                double lb, ub;
                maths_t::ETail tail;
                TDouble1Vec sample(1, 15.0);
                TDouble4Vec1Vec weight(1, TDouble4Vec(2, 1.0));
                prior.probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                     weightStyles,
                                                     sample,
                                                     weight,
                                                     lb, ub, tail);
                TStrCRefDouble1VecDoublePrPrVec influencerValues;
                influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(15.0, 5.0)));
                influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair(15.0, 6.0)));

                TStrPtrStrPtrPrDoublePrVec influences;
                computeInfluences(calculator,
                                  model_t::E_IndividualMeanByPerson, TDecompositionCPtr(), prior,
                                  0/*time*/, 15.0/*value*/, 1.0/*varianceScale*/, 11.0/*count*/,
                                  0.5*(lb+ub), tail, 0.0/*confidence*/,
                                  I, influencerValues, influences);

                LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
                CPPUNIT_ASSERT(influences.empty());
            }
            {
                LOG_DEBUG("Left tail, no clear influences");

                double lb, ub;
                maths_t::ETail tail;
                TDouble1Vec sample(1, 5.0);
                TDouble4Vec1Vec weight(1, TDouble4Vec(2, 1.0));
                prior.probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                     weightStyles,
                                                     sample,
                                                     weight,
                                                     lb, ub, tail);
                TStrCRefDouble1VecDoublePrPrVec influencerValues;
                influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(5.0, 5.0)));
                influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair(5.0, 6.0)));

                TStrPtrStrPtrPrDoublePrVec influences;
                computeInfluences(calculator,
                                  model_t::E_IndividualMeanByPerson, TDecompositionCPtr(), prior,
                                  0/*time*/, 5.0/*value*/, 1.0/*varianceScale*/, 11.0/*count*/,
                                  0.5*(lb+ub), tail, 0.0/*confidence*/,
                                  I, influencerValues, influences);

                LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
                CPPUNIT_ASSERT(influences.empty());
            }
            {
                LOG_DEBUG("Left tail, two influences");

                TDouble1Vec sample(1, 8.0);
                TDouble4Vec1Vec weight(1, TDouble4Vec(2, 1.0));
                double lb, ub;
                maths_t::ETail tail;
                prior.probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                     weightStyles,
                                                     sample,
                                                     weight,
                                                     lb, ub, tail);
                TStrCRefDouble1VecDoublePrPrVec influencerValues;
                influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(5.0,  9.0)));
                influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair(11.0, 20.0)));
                influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i3), make_pair(5.0,  11.0)));

                TStrPtrStrPtrPrDoublePrVec influences;
                computeInfluences(calculator,
                                  model_t::E_IndividualMeanByPerson, TDecompositionCPtr(), prior,
                                  0/*time*/, 8.0/*value*/, 1.0/*varianceScale*/, 40.0/*count*/,
                                  0.5*(lb+ub), tail, 0.0/*confidence*/,
                                  I, influencerValues, influences);

                LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
                CPPUNIT_ASSERT_EQUAL(std::size_t(2), influences.size());
                CPPUNIT_ASSERT_EQUAL(i3, *influences[0].first.second);
                CPPUNIT_ASSERT_DOUBLES_EQUAL(0.7, influences[0].second, 0.04);
                CPPUNIT_ASSERT_EQUAL(i1, *influences[1].first.second);
                CPPUNIT_ASSERT_DOUBLES_EQUAL(0.6, influences[1].second, 0.03);
            }
        }
    }
    {
        LOG_DEBUG("Test correlated");

        core_t::TTime times[] = { 0, 0 };

        {
            LOG_DEBUG("One influencer value");

            maths::CMultivariateNormalConjugateFactory::TPriorPtr prior =
                    maths::CMultivariateNormalConjugateFactory::nonInformative(2, maths_t::E_ContinuousData, 0.0);
            {
                TDoubleVec mean(2, 10.0);
                TDoubleVecVec covariances(2, TDoubleVec(2));
                covariances[0][0] = covariances[1][1] = 5.0;
                covariances[0][1] = covariances[1][0] = 4.0;
                TDoubleVecVec samples_;
                rng.generateMultivariateNormalSamples(mean, covariances, 50, samples_);
                TDouble10Vec1Vec samples;
                for (std::size_t i = 0u; i < samples_.size(); ++i)
                {
                    samples.push_back(samples_[i]);
                }
                TDouble10Vec4Vec1Vec weights(samples.size(), TDouble10Vec4Vec(1, TDouble10Vec(2, 1.0)));
                prior->addSamples(COUNT_WEIGHT, samples, weights);
            }

            double values[] = { 5.0, 5.0 };
            double counts[] = { 1.0, 1.0 };
            double lb, ub;
            TTail10Vec tail;
            TDouble10Vec1Vec sample(1, TDouble10Vec(&values[0], &values[2]));
            TDouble10Vec4Vec1Vec weights(1, TDouble10Vec4Vec(2, TDouble10Vec(2, 1.0)));
            prior->probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                  weightStyles,
                                                  sample,
                                                  weights,
                                                  lb, ub, tail);
            TStrCRefDouble1VecDouble1VecPrPrVec influencerValues;
            influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i1), make_pair(5.0, 5.0, 1.0, 1.0)));

            TStrPtrStrPtrPrDoublePrVec influences;
            computeInfluences(calculator,
                              model_t::E_IndividualMeanByPerson, TDecompositionCPtr1Vec(), *prior,
                              times, values, weights, counts,
                              0.5*(lb+ub), tail[0], 0, 0.0/*confidence*/,
                              I, influencerValues, influences);

            LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
            CPPUNIT_ASSERT_EQUAL(std::string("[((I, i1), 1)]"),
                                 core::CContainerPrinter::print(influences));
        }
        {
            LOG_DEBUG("No trend");

            maths::CMultivariateNormalConjugateFactory::TPriorPtr prior =
                    maths::CMultivariateNormalConjugateFactory::nonInformative(2, maths_t::E_ContinuousData, 0.0);
            {
                TDoubleVec mean(2, 10.0);
                TDoubleVecVec covariances(2, TDoubleVec(2));
                covariances[0][0] = covariances[1][1] = 5.0;
                covariances[0][1] = covariances[1][0] = 4.0;
                TDoubleVecVec samples_;
                rng.generateMultivariateNormalSamples(mean, covariances, 50, samples_);
                TDouble10Vec1Vec samples;
                for (std::size_t i = 0u; i < samples_.size(); ++i)
                {
                    samples.push_back(samples_[i]);
                }
                TDouble10Vec4Vec1Vec weights(samples.size(), TDouble10Vec4Vec(1, TDouble10Vec(2, 1.0)));
                prior->addSamples(COUNT_WEIGHT, samples, weights);
            }
            {
                LOG_DEBUG("Right tail, one clear influence");

                double values[] = { 10.0, 12.15 };
                double counts[] = { 20.0, 10.0 };
                TSize10Vec coordinates(std::size_t(1), 1);
                TDouble10Vec2Vec lbs, ubs;
                TTail10Vec tail;
                TDouble10Vec1Vec sample(1, TDouble10Vec(&values[0], &values[2]));
                TDouble10Vec4Vec1Vec weights(1, TDouble10Vec4Vec(2, TDouble10Vec(2, 1.0)));
                prior->probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                      weightStyles,
                                                      sample,
                                                      weights,
                                                      coordinates,
                                                      lbs, ubs, tail);
                double lb = ::sqrt(lbs[0][0] * lbs[1][0]);
                double ub = ::sqrt(ubs[0][0] * ubs[1][0]);
                TStrCRefDouble1VecDouble1VecPrPrVec influencerValues;
                influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i1), make_pair(10.0, 20.0, 5.0, 2.5)));
                influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i2), make_pair( 9.0,  9.0, 7.0, 3.5)));
                influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i3), make_pair(10.0, 10.0, 8.0, 4.0)));

                TStrPtrStrPtrPrDoublePrVec influences;
                computeInfluences(calculator,
                                  model_t::E_IndividualMeanByPerson, TDecompositionCPtr1Vec(), *prior,
                                  times, values, weights, counts,
                                  0.5*(lb+ub), tail[0], coordinates[0], 0.0/*confidence*/,
                                  I, influencerValues, influences);

                LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
                CPPUNIT_ASSERT_EQUAL(std::string("[((I, i1), 1)]"),
                                     core::CContainerPrinter::print(influences));
            }
            {
                LOG_DEBUG("Right tail, no clear influences");

                double values[] = { 11.0, 15.0 };
                double counts[] = {  4.0, 11.0 };
                TSize10Vec coordinates(std::size_t(1), 1);
                TDouble10Vec2Vec lbs, ubs;
                TTail10Vec tail;
                TDouble10Vec1Vec sample(1, TDouble10Vec(&values[0], &values[2]));
                TDouble10Vec4Vec1Vec weight(1, TDouble10Vec4Vec(2, TDouble10Vec(2, 1.0)));
                prior->probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                      weightStyles,
                                                      sample,
                                                      weight,
                                                      coordinates,
                                                      lbs, ubs, tail);
                double lb = ::sqrt(lbs[0][0] * lbs[1][0]);
                double ub = ::sqrt(ubs[0][0] * ubs[1][0]);
                TStrCRefDouble1VecDouble1VecPrPrVec influencerValues;
                influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i1), make_pair(10.0, 15.0, 2.0, 5.0)));
                influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i2), make_pair(12.0, 15.0, 2.0, 6.0)));

                TStrPtrStrPtrPrDoublePrVec influences;
                computeInfluences(calculator,
                                  model_t::E_IndividualMeanByPerson, TDecompositionCPtr1Vec(), *prior,
                                  times, values, weight, counts,
                                  0.5*(lb+ub), tail[0], coordinates[0], 0.0/*confidence*/,
                                  I, influencerValues, influences);

                LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
                CPPUNIT_ASSERT(influences.empty());
            }
            {
                LOG_DEBUG("Left tail, no clear influences");

                double values[] = {  5.0,  5.0 };
                double counts[] = { 11.0, 11.0 };
                TSize10Vec coordinates(std::size_t(1), 0);
                TDouble10Vec2Vec lbs, ubs;
                TTail10Vec tail;
                TDouble10Vec1Vec sample(1, TDouble10Vec(&values[0], &values[2]));
                TDouble10Vec4Vec1Vec weight(1, TDouble10Vec4Vec(2, TDouble10Vec(2, 1.0)));
                prior->probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                      weightStyles,
                                                      sample,
                                                      weight,
                                                      coordinates,
                                                      lbs, ubs, tail);
                double lb = ::sqrt(lbs[0][0] * lbs[1][0]);
                double ub = ::sqrt(ubs[0][0] * ubs[1][0]);
                TStrCRefDouble1VecDouble1VecPrPrVec influencerValues;
                influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i1), make_pair(5.0, 5.0, 5.0, 5.0)));
                influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i2), make_pair(5.0, 5.0, 6.0, 5.0)));

                TStrPtrStrPtrPrDoublePrVec influences;
                computeInfluences(calculator,
                                  model_t::E_IndividualMeanByPerson, TDecompositionCPtr1Vec(), *prior,
                                  times, values, weight, counts,
                                  0.5*(lb+ub), tail[0], coordinates[0], 0.0/*confidence*/,
                                  I, influencerValues, influences);

                LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
                CPPUNIT_ASSERT(influences.empty());
            }
            {
                LOG_DEBUG("Left tail, two influences");

                double values[] = {  8.0, 10.0 };
                double counts[] = { 40.0, 12.0 };
                TSize10Vec coordinates(std::size_t(1), 0);
                TDouble10Vec2Vec lbs, ubs;
                TTail10Vec tail;
                TDouble10Vec1Vec sample(1, TDouble10Vec(&values[0], &values[2]));
                TDouble10Vec4Vec1Vec weight(1, TDouble10Vec4Vec(2, TDouble10Vec(2, 1.0)));
                prior->probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                      weightStyles,
                                                      sample,
                                                      weight,
                                                      coordinates,
                                                      lbs, ubs, tail);
                double lb = ::sqrt(lbs[0][0] * lbs[1][0]);
                double ub = ::sqrt(ubs[0][0] * ubs[1][0]);
                TStrCRefDouble1VecDouble1VecPrPrVec influencerValues;
                influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i1), make_pair( 4.5, 10.0,  9.0, 4.0)));
                influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i2), make_pair(11.5, 11.0, 20.0, 4.0)));
                influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i3), make_pair( 4.5,  9.0, 11.0, 4.0)));

                TStrPtrStrPtrPrDoublePrVec influences;
                computeInfluences(calculator,
                                  model_t::E_IndividualMeanByPerson, TDecompositionCPtr1Vec(), *prior,
                                  times, values, weight, counts,
                                  0.5*(lb+ub), tail[0], coordinates[0], 0.0/*confidence*/,
                                  I, influencerValues, influences);

                LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
                CPPUNIT_ASSERT_EQUAL(std::size_t(2), influences.size());
                CPPUNIT_ASSERT_EQUAL(i1, *influences[0].first.second);
                CPPUNIT_ASSERT_DOUBLES_EQUAL(0.6, influences[0].second, 0.04);
                CPPUNIT_ASSERT_EQUAL(i3, *influences[1].first.second);
                CPPUNIT_ASSERT_DOUBLES_EQUAL(0.6, influences[1].second, 0.08);
            }
        }
    }
}

void CProbabilityAndInfluenceCalculatorTest::testLogProbabilityInfluenceCalculator(void)
{
    LOG_DEBUG("*** testLogProbabilityInfluenceCalculator ***");

    test::CRandomNumbers rng;

    model::CLogProbabilityInfluenceCalculator calculator;

    maths_t::TWeightStyleVec weightStyle(1, maths_t::E_SampleSeasonalVarianceScaleWeight);

    {
        LOG_DEBUG("Test univariate");
        {
            LOG_DEBUG("One influencer value");

            maths::CNormalMeanPrecConjugate prior =
                    maths::CNormalMeanPrecConjugate::nonInformativePrior(maths_t::E_ContinuousData);

            TDoubleVec samples;
            rng.generateNormalSamples(10.0, 1.0, 50, samples);
            TDouble4Vec1Vec weights(samples.size(), TDouble4Vec(1, 1.0));
            prior.addSamples(COUNT_WEIGHT, samples, weights);

            TStrCRefDouble1VecDoublePrPrVec influencerValues;
            influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(5.0, 1.0)));

            double lb, ub;
            maths_t::ETail tail;
            TDouble1Vec sample(1, 5.0);
            TDouble4Vec1Vec weight(1, TDouble4Vec(1, 1.0));
            prior.probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                 weightStyle,
                                                 sample,
                                                 weight,
                                                 lb, ub, tail);

            TStrPtrStrPtrPrDoublePrVec influences;
            computeInfluences(calculator,
                              model_t::E_IndividualUniqueCountByBucketAndPerson, TDecompositionCPtr(), prior,
                              0/*time*/, 5.0/*value*/, 1.0/*varianceScale*/, 1.0/*count*/,
                              0.5*(lb+ub), tail, 0.0/*confidence*/,
                              I, influencerValues, influences);

            LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
            CPPUNIT_ASSERT_EQUAL(std::string("[((I, i1), 1)]"),
                                 core::CContainerPrinter::print(influences));
        }
        {
            LOG_DEBUG("No trend");

            maths::CNormalMeanPrecConjugate prior =
                    maths::CNormalMeanPrecConjugate::nonInformativePrior(maths_t::E_ContinuousData);

            TDoubleVec samples;
            rng.generateNormalSamples(10.0, 1.0, 50, samples);
            TDouble4Vec1Vec weights(samples.size(), TDouble4Vec(1, 1.0));
            prior.addSamples(COUNT_WEIGHT, samples, weights);

            TStrCRefDouble1VecDoublePrPrVec influencerValues;
            influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(9.0, 1.0)));
            influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair(6.0, 1.0)));
            influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i3), make_pair(6.0, 1.0)));

            double lb, ub;
            maths_t::ETail tail;
            TDouble1Vec sample(1, 6.0);
            TDouble4Vec1Vec weight(1, TDouble4Vec(1, 1.0));
            prior.probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                 weightStyle,
                                                 sample,
                                                 weight,
                                                 lb, ub, tail);

            TStrPtrStrPtrPrDoublePrVec influences;
            computeInfluences(calculator,
                              model_t::E_IndividualUniqueCountByBucketAndPerson, TDecompositionCPtr(), prior,
                              0/*time*/, 6.0/*value*/, 1.0/*varianceScale*/, 1.0/*count*/,
                              0.5*(lb+ub), tail, 0.0/*confidence*/,
                              I, influencerValues, influences);

            LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
            CPPUNIT_ASSERT_EQUAL(std::string("[((I, i2), 1), ((I, i3), 1)]"),
                                 core::CContainerPrinter::print(influences));
        }
        {
            LOG_DEBUG("Trend");

            TDoubleVec samples;
            rng.generateNormalSamples(0.0, 100.0, 10 * 86400 / 600, samples);
            TDouble4Vec1Vec weights(samples.size(), TDouble4Vec(1, 1.0));

            TDecompositionPtr trend(new maths::CTimeSeriesDecomposition);
            maths::CNormalMeanPrecConjugate prior =
                    maths::CNormalMeanPrecConjugate::nonInformativePrior(maths_t::E_ContinuousData);

            for (core_t::TTime time = 0, i = 0; time < 10 * 86400; time += 600, ++i)
            {
                double y = 100.0 + 100.0 * ::sin(2.0 * 3.1416 * static_cast<double>(time) / 86400.0) + samples[i];
                trend->addPoint(time, y);
            }
            prior.addSamples(COUNT_WEIGHT, samples, weights);

            core_t::TTime testTimes[] = { 0, 86400 / 4, 86400 / 2, (3 * 86400) / 4 };

            TStrCRefDouble1VecDoublePrPrVec influencerValues;
            influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(60.0, 1.0)));
            influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair(50.0, 1.0)));

            std::string expectedInfluencerValues[] = { "i1", "i2" };
            double expectedInfluences[][2] =
                {
                    { 1.0, 1.0 },
                    { 1.0, 1.0 },
                    { 1.0, 1.0 },
                    { 1.0, 0.7 }
                };

            for (std::size_t i = 0u; i < boost::size(testTimes); ++i)
            {
                core_t::TTime time = testTimes[i];
                LOG_DEBUG("  time = " << time);
                LOG_DEBUG("  baseline = " << core::CContainerPrinter::print(trend->baseline(time, 0.0)));

                double detrended = trend->detrend(time, 60.0, 0.0);
                TDoubleDoublePr vs = trend->scale(time, prior.marginalLikelihoodVariance(), 0.0);
                LOG_DEBUG("  detrended = " << detrended
                          << ", vs = " << core::CContainerPrinter::print(vs));

                double lb, ub;
                maths_t::ETail tail;
                TDouble1Vec sample(1, detrended);
                TDouble4Vec1Vec weight(1, TDouble4Vec(1, vs.second));
                prior.probabilityOfLessLikelySamples(maths_t::E_OneSidedAbove,
                                                     weightStyle,
                                                     sample,
                                                     weight,
                                                     lb, ub, tail);
                LOG_DEBUG("  p = " << 0.5*(lb+ub) << ", tail = " << tail);

                TStrPtrStrPtrPrDoublePrVec influences;
                computeInfluences(calculator,
                                  model_t::E_IndividualHighUniqueCountByBucketAndPerson, trend, prior,
                                  time, 60.0/*value*/, vs.second, 1.0/*count*/,
                                  0.5*(lb+ub), tail, 0.0/*confidence*/,
                                  I, influencerValues, influences);

                LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
                for (std::size_t j = 0u; j < influences.size(); ++j)
                {
                    CPPUNIT_ASSERT_EQUAL(expectedInfluencerValues[j],
                                         *influences[j].first.second);
                    CPPUNIT_ASSERT_DOUBLES_EQUAL(expectedInfluences[i][j], influences[j].second, 0.03);
                }
            }
        }
    }
    {
        LOG_DEBUG("Test correlated");

        double counts[] = { 1.0, 1.0 };

        {
            LOG_DEBUG("One influencer value");

            maths::CMultivariateNormalConjugateFactory::TPriorPtr prior =
                    maths::CMultivariateNormalConjugateFactory::nonInformative(2, maths_t::E_ContinuousData, 0.0);
            {
                TDoubleVec mean(2, 10.0);
                TDoubleVecVec covariances(2, TDoubleVec(2));
                covariances[0][0] = covariances[1][1] = 1.0;
                covariances[0][1] = covariances[1][0] = 0.9;
                TDoubleVecVec samples_;
                rng.generateMultivariateNormalSamples(mean, covariances, 50, samples_);
                TDouble10Vec1Vec samples;
                for (std::size_t i = 0u; i < samples_.size(); ++i)
                {
                    samples.push_back(samples_[i]);
                }
                TDouble10Vec4Vec1Vec weights(samples.size(), TDouble10Vec4Vec(1, TDouble10Vec(2, 1.0)));
                prior->addSamples(COUNT_WEIGHT, samples, weights);
            }

            core_t::TTime times[] = { 0, 0 };
            double values[] = { 5.0, 5.0 };
            double lb, ub;
            TTail10Vec tail;
            TDouble10Vec1Vec sample(1, TDouble10Vec(&values[0], &values[2]));
            TDouble10Vec4Vec1Vec weight(1, TDouble10Vec4Vec(2, TDouble10Vec(2, 1.0)));
            prior->probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                  weightStyle,
                                                  sample,
                                                  weight,
                                                  lb, ub, tail);
            TStrCRefDouble1VecDouble1VecPrPrVec influencerValues;
            influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i1), make_pair(5.0, 10.0, 1.0, 1.0)));

            TStrPtrStrPtrPrDoublePrVec influences;
            computeInfluences(calculator,
                              model_t::E_IndividualUniqueCountByBucketAndPerson, TDecompositionCPtr1Vec(), *prior,
                              times, values, weight, counts,
                              0.5*(lb+ub), tail[0], 0, 0.0/*confidence*/,
                              I, influencerValues, influences);

            LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
            CPPUNIT_ASSERT_EQUAL(std::string("[((I, i1), 1)]"),
                                 core::CContainerPrinter::print(influences));
        }
        {
            LOG_DEBUG("No trend");

            maths::CMultivariateNormalConjugateFactory::TPriorPtr prior =
                    maths::CMultivariateNormalConjugateFactory::nonInformative(2, maths_t::E_ContinuousData, 0.0);

            {
                TDoubleVec mean(2, 10.0);
                TDoubleVecVec covariances(2, TDoubleVec(2));
                covariances[0][0] = covariances[1][1] = 1.0;
                covariances[0][1] = covariances[1][0] = -0.9;
                TDoubleVecVec samples_;
                rng.generateMultivariateNormalSamples(mean, covariances, 50, samples_);
                TDouble10Vec1Vec samples;
                for (std::size_t i = 0u; i < samples_.size(); ++i)
                {
                    samples.push_back(samples_[i]);
                }
                TDouble10Vec4Vec1Vec weights(samples.size(), TDouble10Vec4Vec(1, TDouble10Vec(2, 1.0)));
                prior->addSamples(COUNT_WEIGHT, samples, weights);
            }

            core_t::TTime times[] = { 0, 0 };
            double values[] = { 10.0, 6.0 };
            TSize10Vec coordinates(std::size_t(1), 1);
            TDouble10Vec2Vec lbs, ubs;
            TTail10Vec tail;
            TDouble10Vec1Vec sample(1, TDouble10Vec(&values[0], &values[2]));
            TDouble10Vec4Vec1Vec weight(1, TDouble10Vec4Vec(2, TDouble10Vec(2, 1.0)));
            prior->probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                  weightStyle,
                                                  sample,
                                                  weight,
                                                  coordinates,
                                                  lbs, ubs, tail);
            double lb = ::sqrt(lbs[0][0] * lbs[1][0]);
            double ub = ::sqrt(ubs[0][0] * ubs[1][0]);
            TStrCRefDouble1VecDouble1VecPrPrVec influencerValues;
            influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i1), make_pair(11.0, 9.0, 1.0, 1.0)));
            influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i2), make_pair(10.0, 6.0, 1.0, 1.0)));
            influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i3), make_pair( 9.0, 6.0, 1.0, 1.0)));

            TStrPtrStrPtrPrDoublePrVec influences;
            computeInfluences(calculator,
                              model_t::E_IndividualUniqueCountByBucketAndPerson, TDecompositionCPtr1Vec(), *prior,
                              times, values, weight, counts,
                              0.5*(lb+ub), tail[0], coordinates[0], 0.0/*confidence*/,
                              I, influencerValues, influences);

            LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
            CPPUNIT_ASSERT_EQUAL(std::string("[((I, i2), 1), ((I, i3), 1)]"),
                                 core::CContainerPrinter::print(influences));
        }
        {
            LOG_DEBUG("Trend");

            TDecompositionPtrVec trend;
            trend.push_back(TDecompositionPtr(new maths::CTimeSeriesDecomposition));
            trend.push_back(TDecompositionPtr(new maths::CTimeSeriesDecomposition));
            maths::CMultivariateNormalConjugateFactory::TPriorPtr prior =
                    maths::CMultivariateNormalConjugateFactory::nonInformative(2, maths_t::E_ContinuousData, 0.0);
            {
                TDoubleVec mean(2, 0.0);
                TDoubleVecVec covariances(2, TDoubleVec(2));
                covariances[0][0] = covariances[1][1] = 100.0;
                covariances[0][1] = covariances[1][0] =  80.0;
                TDoubleVecVec samples;
                rng.generateMultivariateNormalSamples(mean, covariances, 10 * 86400 / 600, samples);
                TDouble10Vec4Vec1Vec weight(1, TDouble10Vec4Vec(1, TDouble10Vec(2, 1.0)));
                for (core_t::TTime time = 0, i = 0; time < 10 * 86400; time += 600, ++i)
                {
                    double y[] =
                        {
                            200.0 + 200.0 * ::sin(2.0 * 3.1416 * static_cast<double>(time) / 86400.0),
                            100.0 + 100.0 * ::sin(2.0 * 3.1416 * static_cast<double>(time) / 86400.0)
                        };
                    trend[0]->addPoint(time, y[0] + samples[i][0]);
                    trend[1]->addPoint(time, y[1] + samples[i][1]);
                    prior->addSamples(COUNT_WEIGHT, TDouble10Vec1Vec(1, TDouble10Vec(samples[i])), weight);
                }
            }

            core_t::TTime testTimes[] = { 0, 86400 / 4, 86400 / 2, (3 * 86400) / 4 };

            TStrCRefDouble1VecDouble1VecPrPrVec influencerValues;
            influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i1), make_pair(60.0, 60.0, 1.0, 1.0)));
            influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i2), make_pair(50.0, 50.0, 1.0, 1.0)));

            std::string expectedInfluencerValues[] = { "i1", "i2" };
            double expectedInfluences[][2] =
                {
                    { 1.0, 1.0 },
                    { 1.0, 1.0 },
                    { 1.0, 1.0 },
                    { 1.0, 0.8 }
                };

            for (std::size_t i = 0u; i < boost::size(testTimes); ++i)
            {
                core_t::TTime time = testTimes[i];
                LOG_DEBUG("  time = " << time);
                LOG_DEBUG("  baseline[0] = " << core::CContainerPrinter::print(trend[0]->baseline(time, 0.0)));
                LOG_DEBUG("  baseline[1] = " << core::CContainerPrinter::print(trend[1]->baseline(time, 0.0)));

                core_t::TTime times[] = { time, time };
                double values[] = { 120.0, 60.0 };
                double detrended[] =
                    {
                        trend[0]->detrend(time, values[0], 0.0),
                        trend[1]->detrend(time, values[1], 0.0)
                    };
                double vs[] =
                    {
                        trend[0]->scale(time, prior->marginalLikelihoodVariances()[0], 0.0).second,
                        trend[1]->scale(time, prior->marginalLikelihoodVariances()[1], 0.0).second
                    };
                LOG_DEBUG("  detrended = " << core::CContainerPrinter::print(detrended)
                          << ", vs = " << core::CContainerPrinter::print(vs));
                TSize10Vec coordinates(std::size_t(1), i % 2);
                TDouble10Vec2Vec lbs, ubs;
                TTail10Vec tail;
                TDouble10Vec1Vec sample(1, TDouble10Vec(&detrended[0], &detrended[2]));
                TDouble10Vec4Vec1Vec weight(1, TDouble10Vec4Vec(1, TDouble10Vec(&vs[0], &vs[2])));
                prior->probabilityOfLessLikelySamples(maths_t::E_OneSidedAbove,
                                                      weightStyle,
                                                      sample,
                                                      weight,
                                                      coordinates,
                                                      lbs, ubs, tail);
                double lb = ::sqrt(lbs[0][0] * lbs[1][0]);
                double ub = ::sqrt(ubs[0][0] * ubs[1][0]);
                LOG_DEBUG("  p = " << 0.5*(lb+ub) << ", tail = " << tail);

                TStrPtrStrPtrPrDoublePrVec influences;
                TDecompositionCPtr1Vec trends;
                for (TDecompositionPtrVecCItr itr = trend.begin(); itr != trend.end(); ++itr)
                {
                    trends.push_back(itr->get());
                }
                computeInfluences(calculator,
                                  model_t::E_IndividualHighUniqueCountByBucketAndPerson,
                                  trends, *prior,
                                  times, values, weight, counts,
                                  0.5*(lb+ub), tail[0], coordinates[0], 0.0/*confidence*/,
                                  I, influencerValues, influences);

                LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
                for (std::size_t j = 0u; j < influences.size(); ++j)
                {
                    CPPUNIT_ASSERT_EQUAL(expectedInfluencerValues[j],
                                         *influences[j].first.second);
                    CPPUNIT_ASSERT_DOUBLES_EQUAL(expectedInfluences[i][j], influences[j].second, 0.04);
                }
            }
        }
    }
}

void CProbabilityAndInfluenceCalculatorTest::testIndicatorInfluenceCalculator(void)
{
    LOG_DEBUG("*** testIndicatorInfluenceCalculator ***");

    {
        LOG_DEBUG("Test univariate");

        model::CIndicatorInfluenceCalculator calculator;

        maths::CNormalMeanPrecConjugate prior =
                maths::CNormalMeanPrecConjugate::nonInformativePrior(maths_t::E_ContinuousData);

        TStrCRefDouble1VecDoublePrPrVec influencerValues;
        influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(1.0, 1.0)));
        influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair(1.0, 1.0)));
        influencerValues.push_back(TStrCRefDouble1VecDoublePrPr(TStrCRef(i3), make_pair(1.0, 1.0)));

        TStrPtrStrPtrPrDoublePrVec influences;
        computeInfluences(calculator,
                          model_t::E_IndividualIndicatorOfBucketPerson, TDecompositionCPtr(), prior,
                          0/*time*/, 1.0/*value*/, 1.0/*varianceScale*/, 1.0/*count*/,
                          0.1/*probability*/, maths_t::E_RightTail, 0.0/*confidence*/,
                          I, influencerValues, influences);

        LOG_DEBUG("influences = " << core::CContainerPrinter::print(influences));
        CPPUNIT_ASSERT_EQUAL(std::string("[((I, i1), 1), ((I, i2), 1), ((I, i3), 1)]"),
                             core::CContainerPrinter::print(influences));
    }
    {
        LOG_DEBUG("Test correlated");

        model::CIndicatorInfluenceCalculator calculator;

        maths::CMultivariateNormalConjugateFactory::TPriorPtr prior =
                maths::CMultivariateNormalConjugateFactory::nonInformative(2, maths_t::E_ContinuousData, 0.0);

        core_t::TTime times[] = { 0, 0 };
        double values[] = { 1.0, 1.0 };
        double counts[] = { 1.0, 1.0 };
        TStrCRefDouble1VecDouble1VecPrPrVec influencerValues;
        influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i1), make_pair(1.0, 1.0, 1.0, 1.0)));
        influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i2), make_pair(1.0, 1.0, 1.0, 1.0)));
        influencerValues.push_back(TStrCRefDouble1VecDouble1VecPrPr(TStrCRef(i3), make_pair(1.0, 1.0, 1.0, 1.0)));

        TStrPtrStrPtrPrDoublePrVec influences;
        computeInfluences(calculator,
                          model_t::E_IndividualIndicatorOfBucketPerson, TDecompositionCPtr1Vec(), *prior,
                          times, values, TDouble10Vec4Vec1Vec(1, TDouble10Vec4Vec(1, TDouble10Vec(2, 1.0))), counts,
                          0.1/*probability*/, maths_t::E_RightTail, 0, 0.0/*confidence*/,
                          I, influencerValues, influences);

        LOG_DEBUG("influences = " << core::CContainerPrinter::print(influences));
        CPPUNIT_ASSERT_EQUAL(std::string("[((I, i1), 1), ((I, i2), 1), ((I, i3), 1)]"),
                             core::CContainerPrinter::print(influences));
    }
}

void CProbabilityAndInfluenceCalculatorTest::testProbabilityAndInfluenceCalculator(void)
{
    LOG_DEBUG("*** testProbabilityAndInfluenceCalculator ***");

    test::CRandomNumbers rng;

    maths::CNormalMeanPrecConjugate prior =
            maths::CNormalMeanPrecConjugate::nonInformativePrior(maths_t::E_ContinuousData);
    maths::CMultivariateNormalConjugate<2> multivariatePrior =
            maths::CMultivariateNormalConjugate<2>::nonInformativePrior(maths_t::E_ContinuousData);

    maths_t::TWeightStyleVec countWeight(1, maths_t::E_SampleCountWeight);

    TDoubleVec samples;
    rng.generateNormalSamples(10.0, 1.0, 50, samples);
    TDouble4Vec1Vec weights(samples.size(), TDouble4Vec(1, 1.0));
    prior.addSamples(countWeight, samples, weights);

    double m[]    = { 10.0, 15.0 };
    double c[][2] = { { 1.0, 0.5 }, { 0.5, 2.0 } };
    TDoubleVec mean(m, m + 2);
    TDoubleVecVec covariances;
    for (std::size_t i = 0u; i < 2; ++i)
    {
        covariances.push_back(TDoubleVec(c[i], c[i] + 2));
    }
    TDoubleVecVec multivariateSamples;
    rng.generateMultivariateNormalSamples(mean, covariances, 50, multivariateSamples);

    TDouble10Vec4Vec1Vec multivariateWeights(multivariateSamples.size(), TDouble10Vec4Vec(1, TDouble10Vec(2, 1.0)));
    multivariatePrior.addSamples(countWeight, multivariateSamples, multivariateWeights);

    maths_t::TWeightStyleVec weightStyles;
    weightStyles.push_back(maths_t::E_SampleSeasonalVarianceScaleWeight);
    weightStyles.push_back(maths_t::E_SampleCountVarianceScaleWeight);
    model::CPartitioningFields partitioningFields(EMPTY_STRING, EMPTY_STRING);

    {
        LOG_DEBUG("error case");

        model::CProbabilityAndInfluenceCalculator calculator(0.5);
        calculator.addAggregator(maths::CJointProbabilityOfLessLikelySamples());
        calculator.addAggregator(maths::CProbabilityOfExtremeSample());

        double values[][2] =
            {
                { 12.0, 1.0 },
                { 15.0, 1.0 },
                {  7.0, 1.5 },
                {  9.0, 1.0 },
                { 17.0, 2.0 }
            };
        double multivariateValues[][3] =
            {
                { 12.0, 17.0, 1.0 },
                { 15.0, 20.0, 1.0 },
                {  7.0, 12.0, 1.5 },
                { 15.0, 10.0, 1.0 },
                { 17.0, 22.0, 2.0 }
            };

        TStrCRefDouble1VecDoublePrPr influencerValues[] =
            {
                 TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair(12.0, 1.0)),
                 TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(15.0, 1.0)),
                 TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair( 7.0, 1.0)),
                 TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair( 9.0, 1.0)),
                 TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(17.0, 1.0))
            };
        TStrCRefDouble1VecDoublePrPr multivariateInfluencerValues[] =
            {
                 TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair(12.0, 17.0, 1.0)),
                 TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(15.0, 20.0, 1.0)),
                 TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair( 7.0, 12.0, 1.0)),
                 TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair( 9.0, 14.0, 1.0)),
                 TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(17.0, 22.0, 1.0))
            };

        maths::CJointProbabilityOfLessLikelySamples pJoint;
        maths::CProbabilityOfExtremeSample pExtreme;

        for (std::size_t i = 0u; i < boost::size(values); ++i)
        {
            TDouble1Vec value(1, values[i][0]);
            TDouble4Vec1Vec weight(1, TDouble4Vec(2, 1.0));
            weight[0][0] = values[i][1];
            TDouble10Vec1Vec multivariateValue(1, TDouble10Vec(&multivariateValues[i][0],
                                                               &multivariateValues[i][2]));
            TDouble10Vec4Vec1Vec multivariateWeight(1, TDouble10Vec4Vec(2, TDouble10Vec(2, 1.0)));
            multivariateWeight[0][0][0] = multivariateValues[i][2];
            multivariateWeight[0][1][0] = multivariateValues[i][2];

            double p = 0.0;
            maths_t::ETail tail;
            calculator.addProbability(model_t::E_IndividualSumByBucketAndPerson,
                                      prior,
                                      0/*elapsedTime*/,
                                      weightStyles, value, weight,
                                      false, 0.0, p, tail);
            double pMultivariate = 0.0;
            TTail10Vec multivariateTails;
            calculator.addProbability(model_t::E_IndividualMeanLatLongByPerson,
                                      multivariatePrior,
                                      0/*elapsedTime*/,
                                      weightStyles, multivariateValue, multivariateWeight,
                                      false, 0.0, pMultivariate, multivariateTails);
            LOG_DEBUG("  p = " << p);
            LOG_DEBUG("  multivariate p = " << pMultivariate);

            pJoint.add(p);
            pExtreme.add(p);
            pJoint.add(pMultivariate);
            pExtreme.add(pMultivariate);
            {
                model::CProbabilityAndInfluenceCalculator::SParams params(weightStyles, partitioningFields);
                params.s_Feature = model_t::E_IndividualSumByBucketAndPerson;
                params.s_Prior = &prior;
                params.s_Value = value;
                params.s_Count = 1.0;
                params.s_Weights = weight;
                params.s_Probability = p;
                calculator.addInfluences(I, TStrCRefDouble1VecDoublePrPrVec(1, influencerValues[i]), params);
            }
            {
                model::CProbabilityAndInfluenceCalculator::SMultivariateParams params(weightStyles, partitioningFields);
                params.s_Feature = model_t::E_IndividualMeanLatLongByPerson;
                params.s_Prior = &multivariatePrior;
                params.s_Value = multivariateValue[0];
                params.s_Count = 1.0;
                params.s_Weights = multivariateWeight;
                params.s_Probability = pMultivariate;
                params.s_Tail = multivariateTails;
                calculator.addInfluences(I, TStrCRefDouble1VecDoublePrPrVec(1, multivariateInfluencerValues[i]), params);
            }
        }

        calculator.addProbability(0.02);
        pJoint.add(0.02);
        pExtreme.add(0.02);

        double probability;
        TStrPtrStrPtrPrDoublePrVec influences;
        CPPUNIT_ASSERT(calculator.calculate(probability, influences));

        double pj, pe;
        CPPUNIT_ASSERT(pJoint.calculate(pj));
        CPPUNIT_ASSERT(pExtreme.calculate(pe));

        LOG_DEBUG("  probability = " << probability
                  << ", expected probability = " << std::min(pj, pe));
        CPPUNIT_ASSERT_DOUBLES_EQUAL(std::min(pe, pj), probability, 1e-10);

        LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
        CPPUNIT_ASSERT(influences.empty());
    }

    {
        LOG_DEBUG("influencing joint probability");

        double values[][2] =
            {
                { 12.0, 1.0 },
                { 15.0, 1.0 },
                { 7.0,  1.5 },
                { 9.0,  1.0 },
                { 17.0, 2.0 }
            };
        double multivariateValues[][3] =
            {
                { 12.0, 17.0, 1.0 },
                { 15.0, 20.0, 1.0 },
                {  7.0, 12.0, 1.5 },
                {  9.0, 14.0, 1.0 },
                { 17.0, 22.0, 2.0 }
            };

        TStrCRefDouble1VecDoublePrPr influencerValues[][1] =
            {
                 { TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair(12.0, 1.0)) },
                 { TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(15.0, 1.0)) },
                 { TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair( 7.0, 1.0)) },
                 { TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair( 9.0, 1.0)) },
                 { TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(17.0, 1.0)) }
            };
        TStrCRefDouble1VecDoublePrPr multivariateInfluencerValues[][1] =
            {
                 { TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair(12.0, 17.0, 1.0)) },
                 { TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(15.0, 20.0, 1.0)) },
                 { TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair( 7.0, 12.0, 1.0)) },
                 { TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair( 9.0, 14.0, 1.0)) },
                 { TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(17.0, 22.0, 1.0)) }
            };
        {
            TStrPtrStrPtrPrDoublePrVec influences;
            univariateTestProbabilityAndGetInfluences<5, 1>(model_t::E_IndividualHighSumByBucketAndPerson,
                                                            prior, values, influencerValues, influences);
            LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
            CPPUNIT_ASSERT_EQUAL(std::size_t(1), influences.size());
            CPPUNIT_ASSERT_EQUAL(i1, *influences[0].first.second);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(1.0, influences[0].second, 0.03);
        }
        {
            TStrPtrStrPtrPrDoublePrVec influences;
            latLongTestProbabilityAndGetInfluences<5, 1>(multivariatePrior,
                                                         multivariateValues,
                                                         multivariateInfluencerValues,
                                                         influences);
            LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
            CPPUNIT_ASSERT_EQUAL(std::size_t(1), influences.size());
            CPPUNIT_ASSERT_EQUAL(i1, *influences[0].first.second);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(1.0, influences[0].second, 1e-3);
        }
    }

    {
        LOG_DEBUG("influencing extreme probability");

        double values[][2] =
            {
                { 11.0, 1.0 },
                { 10.5, 1.0 },
                { 8.5,  1.5 },
                { 10.8, 1.5 },
                { 19.0, 1.0 }
            };
        double multivariateValues[][3] =
            {
                { 11.0, 16.0, 1.0 },
                { 10.5, 15.5, 1.0 },
                {  8.5, 13.5, 1.5 },
                { 10.8, 15.8, 1.5 },
                { 19.0, 24.0, 1.0 }
            };

        TStrCRefDouble1VecDoublePrPr influencerValues[][1] =
            {
                 { TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(11.0, 1.0)) },
                 { TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(10.5, 1.0)) },
                 { TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair( 8.5, 1.0)) },
                 { TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(10.8, 1.0)) },
                 { TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair(19.0, 1.0)) }
            };
        TStrCRefDouble1VecDoublePrPr multivariateInfluencerValues[][1] =
            {
                 { TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(11.0, 16.0, 1.0)) },
                 { TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(10.5, 15.5, 1.0)) },
                 { TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair( 8.5, 13.5, 1.0)) },
                 { TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(10.8, 15.8, 1.0)) },
                 { TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair(19.0, 24.0, 1.0)) }
            };

        {
            TStrPtrStrPtrPrDoublePrVec influences;
            univariateTestProbabilityAndGetInfluences<5, 1>(model_t::E_IndividualHighSumByBucketAndPerson,
                                                            prior, values, influencerValues, influences);
            LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
            CPPUNIT_ASSERT_EQUAL(std::size_t(1), influences.size());
            CPPUNIT_ASSERT_EQUAL(i2, *influences[0].first.second);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(1.0, influences[0].second, 0.03);
        }
        {
            TStrPtrStrPtrPrDoublePrVec influences;
            latLongTestProbabilityAndGetInfluences<5, 1>(multivariatePrior,
                                                         multivariateValues,
                                                         multivariateInfluencerValues,
                                                         influences);
            LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
            CPPUNIT_ASSERT_EQUAL(std::size_t(1), influences.size());
            CPPUNIT_ASSERT_EQUAL(i2, *influences[0].first.second);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(1.0, influences[0].second, 1e-3);
        }
    }

    {
        LOG_DEBUG("marginal influence");

        double values[][2] =
            {
                { 11.0, 1.0 },
                { 10.5, 1.0 },
                {  7.0, 1.0 },
                { 10.8, 1.0 },
                { 14.0, 1.0 }
            };
        double multivariateValues[][3] =
            {
                { 11.0, 16.0, 1.0 },
                { 10.5, 15.5, 1.0 },
                {  8.0, 13.0, 1.0 },
                { 10.8, 15.8, 1.0 },
                { 14.0, 19.0, 1.0 }
            };

        TStrCRefDouble1VecDoublePrPr influencerValues[][2] =
            {
                 {
                     TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(12.0, 1.0)),
                     TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair(10.0, 1.0))
                 },
                 {
                     TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(10.5, 1.0)),
                     TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair(10.5, 1.0))
                 },
                 {
                     TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair( 9.0, 1.0)),
                     TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair( 7.0, 1.0))
                 },
                 {
                     TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(11.0, 1.0)),
                     TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair(10.6, 1.0))
                 },
                 {
                     TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(16.0, 1.0)),
                     TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair(12.0, 1.0))
                 },
            };
        TStrCRefDouble1VecDoublePrPr multivariateInfluencerValues[][2] =
            {
                 {
                     TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(12.0, 17.0, 1.0)),
                     TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair(10.0, 15.0, 1.0))
                 },
                 {
                     TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(10.5, 15.5, 1.0)),
                     TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair(10.5, 15.5, 1.0))
                 },
                 {
                     TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair( 9.0, 14.0, 1.0)),
                     TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair( 7.0, 12.0, 1.0))
                 },
                 {
                     TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(11.0, 16.0, 1.0)),
                     TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair(10.6, 15.6, 1.0))
                 },
                 {
                     TStrCRefDouble1VecDoublePrPr(TStrCRef(i1), make_pair(16.0, 21.0, 1.0)),
                     TStrCRefDouble1VecDoublePrPr(TStrCRef(i2), make_pair(12.0, 17.0, 1.0))
                 },
            };

        {
            TStrPtrStrPtrPrDoublePrVec influences;
            univariateTestProbabilityAndGetInfluences<5, 2>(model_t::E_IndividualMeanByPerson,
                                                            prior, values, influencerValues, influences);
            LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
            CPPUNIT_ASSERT_EQUAL(std::size_t(1), influences.size());
            CPPUNIT_ASSERT_EQUAL(i1, *influences[0].first.second);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(0.6, influences[0].second, 0.03);
        }
        {
            TStrPtrStrPtrPrDoublePrVec influences;
            latLongTestProbabilityAndGetInfluences<5, 2>(multivariatePrior,
                                                         multivariateValues,
                                                         multivariateInfluencerValues,
                                                         influences);
            LOG_DEBUG("  influences = " << core::CContainerPrinter::print(influences));
            CPPUNIT_ASSERT_EQUAL(std::size_t(2), influences.size());
            CPPUNIT_ASSERT_EQUAL(i2, *influences[0].first.second);
            CPPUNIT_ASSERT_EQUAL(i1, *influences[1].first.second);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(1.0, influences[0].second, 1e-3);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(1.0, influences[1].second, 1e-3);
        }
    }
}

CppUnit::Test *CProbabilityAndInfluenceCalculatorTest::suite(void)
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CProbabilityAndInfluenceCalculatorTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CProbabilityAndInfluenceCalculatorTest>(
                                   "CProbabilityAndInfluenceCalculatorTest::testInfluenceUnavailableCalculator",
                                   &CProbabilityAndInfluenceCalculatorTest::testInfluenceUnavailableCalculator) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CProbabilityAndInfluenceCalculatorTest>(
                                   "CProbabilityAndInfluenceCalculatorTest::testLogProbabilityComplementInfluenceCalculator",
                                   &CProbabilityAndInfluenceCalculatorTest::testLogProbabilityComplementInfluenceCalculator) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CProbabilityAndInfluenceCalculatorTest>(
                                   "CProbabilityAndInfluenceCalculatorTest::testMeanInfluenceCalculator",
                                   &CProbabilityAndInfluenceCalculatorTest::testMeanInfluenceCalculator) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CProbabilityAndInfluenceCalculatorTest>(
                                   "CProbabilityAndInfluenceCalculatorTest::testLogProbabilityInfluenceCalculator",
                                   &CProbabilityAndInfluenceCalculatorTest::testLogProbabilityInfluenceCalculator) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CProbabilityAndInfluenceCalculatorTest>(
                                   "CProbabilityAndInfluenceCalculatorTest::testIndicatorInfluenceCalculator",
                                   &CProbabilityAndInfluenceCalculatorTest::testIndicatorInfluenceCalculator) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CProbabilityAndInfluenceCalculatorTest>(
                                   "CProbabilityAndInfluenceCalculatorTest::testProbabilityAndInfluenceCalculator",
                                   &CProbabilityAndInfluenceCalculatorTest::testProbabilityAndInfluenceCalculator) );

    return suiteOfTests;
}
