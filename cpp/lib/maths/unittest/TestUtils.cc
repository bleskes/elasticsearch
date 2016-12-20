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

#include "TestUtils.h"

#include <maths/CEqualWithTolerance.h>
#include <maths/CPriorUtils.h>
#include <maths/CTools.h>

namespace prelert
{
using namespace handy_typedefs;

namespace
{

//! \brief Computes the c.d.f. of the prior minus the target supplied
//! to its constructor at specific locations.
class CCdf : public std::unary_function<double, double>
{
    public:
        typedef core::CSmallVector<double, 1> TDouble1Vec;

        enum EStyle
        {
            E_Lower,
            E_Upper,
            E_GeometricMean
        };

    public:
        CCdf(EStyle style, const maths::CPrior &prior, double target) :
                m_Style(style),
                m_Prior(&prior),
                m_Target(target),
                m_X(1u)
        {}

        double operator()(double x) const
        {
            double lowerBound, upperBound;

            m_X[0] = x;
            if (!m_Prior->minusLogJointCdf(maths::CConstantWeights::COUNT_VARIANCE,
                                           m_X,
                                           maths::CConstantWeights::SINGLE_UNIT,
                                           lowerBound, upperBound))
            {
                // We have no choice but to throw because this is
                // invoked inside a boost root finding function.

                LOG_ERROR("Failed to evaluate c.d.f. at " << x);
                throw std::runtime_error("Failed to evaluate c.d.f.");
            }

            switch (m_Style)
            {
            case E_Lower:         return ::exp(-lowerBound) - m_Target;
            case E_Upper:         return ::exp(-upperBound) - m_Target;
            case E_GeometricMean: return ::exp(-(lowerBound + upperBound) / 2.0) - m_Target;
            }
            return ::exp(-(lowerBound + upperBound) / 2.0) - m_Target;
        }

    private:
        EStyle m_Style;
        const maths::CPrior *m_Prior;
        double m_Target;
        mutable TDouble1Vec m_X;
};

//! Set \p result to \p x.
bool feedthrough(double x, double &result)
{
    result = x;
    return true;
}

//! Computes the residual from a specified mean.
class CResidual
{
    public:
        typedef double result_type;

    public:
        CResidual(double mean) : m_Mean(mean) {}

        bool operator()(double x, double &result) const
        {
            result = (x - m_Mean) * (x - m_Mean);
            return true;
        }

    private:
        double m_Mean;
};
}

CPriorTestInterface::CPriorTestInterface(maths::CPrior &prior) :
        m_Prior(&prior)
{
}

void CPriorTestInterface::addSamples(const TDouble1Vec &samples)
{
    TDouble4Vec1Vec weights(samples.size(), TWeights::UNIT);
    m_Prior->addSamples(TWeights::COUNT, samples, weights);
}

maths_t::EFloatingPointErrorStatus
CPriorTestInterface::jointLogMarginalLikelihood(const TDouble1Vec &samples,
                                                double &result) const
{
    TDouble4Vec1Vec weights(samples.size(), TWeights::UNIT);
    return m_Prior->jointLogMarginalLikelihood(TWeights::COUNT, samples, weights, result);
}

bool CPriorTestInterface::minusLogJointCdf(const TDouble1Vec &samples,
                                           double &lowerBound,
                                           double &upperBound) const
{
    TDouble4Vec1Vec weights(samples.size(), TWeights::UNIT);
    return m_Prior->minusLogJointCdf(TWeights::COUNT,
                                     samples,
                                     weights,
                                     lowerBound, upperBound);
}

bool CPriorTestInterface::minusLogJointCdfComplement(const TDouble1Vec &samples,
                                                     double &lowerBound,
                                                     double &upperBound) const
{
    TDouble4Vec1Vec weights(samples.size(), TWeights::UNIT);
    return m_Prior->minusLogJointCdfComplement(TWeights::COUNT,
                                               samples,
                                               weights,
                                               lowerBound, upperBound);
}

bool CPriorTestInterface::probabilityOfLessLikelySamples(maths_t::EProbabilityCalculation calculation,
                                                         const TDouble1Vec &samples,
                                                         double &lowerBound,
                                                         double &upperBound) const
{
    TDouble4Vec1Vec weights(samples.size(), TWeights::UNIT);
    maths_t::ETail tail;
    return m_Prior->probabilityOfLessLikelySamples(calculation,
                                                   TWeights::COUNT,
                                                   samples,
                                                   weights,
                                                   lowerBound, upperBound, tail);
}

bool CPriorTestInterface::anomalyScore(maths_t::EProbabilityCalculation calculation,
                                       const TDouble1Vec &samples,
                                       double &result) const
{
    TDoubleDoublePr1Vec weightedSamples;
    weightedSamples.reserve(samples.size());
    for (std::size_t i = 0u; i < samples.size(); ++i)
    {
        weightedSamples.push_back(std::make_pair(samples[i], 1.0));
    }
    return this->anomalyScore(calculation, maths_t::E_SampleCountWeight, weightedSamples, result);
}

bool CPriorTestInterface::anomalyScore(maths_t::EProbabilityCalculation calculation,
                                       maths_t::ESampleWeightStyle weightStyle,
                                       const TDoubleDoublePr1Vec &samples,
                                       double &result) const
{
    result = 0.0;

    TWeightStyleVec weightStyles(1, weightStyle);
    TDouble1Vec samples_(samples.size());
    TDouble4Vec1Vec weights(samples.size(), TWeights::UNIT);
    for (std::size_t i = 0u; i < samples.size(); ++i)
    {
        samples_[i] = samples[i].first;
        weights[i][0] = samples[i].second;
    }

    double lowerBound, upperBound;
    maths_t::ETail tail;
    if (!m_Prior->probabilityOfLessLikelySamples(calculation,
                                                 weightStyles,
                                                 samples_,
                                                 weights,
                                                 lowerBound,
                                                 upperBound,
                                                 tail))
    {
        LOG_ERROR("Failed computing probability of less likely samples");
        return false;
    }

    result = maths::CTools::deviation((lowerBound + upperBound) / 2.0);

    return true;
}

bool CPriorTestInterface::marginalLikelihoodQuantileForTest(double percentage,
                                                            double eps,
                                                            double &result) const
{

    result = 0.0;

    percentage /= 100.0;
    double step = 1.0;
    TDoubleDoublePr bracket(0.0, step);

    try
    {
        CCdf cdf(percentage < 0.5 ? CCdf::E_Lower : CCdf::E_Upper, *m_Prior, percentage);
        TDoubleDoublePr fBracket(cdf(bracket.first), cdf(bracket.second));

        std::size_t maxIterations = 100u;
        for (/**/;
             fBracket.first * fBracket.second > 0.0 && maxIterations > 0;
             --maxIterations)
        {
            step *= 2.0;
            if (fBracket.first > 0.0)
            {
                bracket.first -= step;
                fBracket.first = cdf(bracket.first);
            }
            else if (fBracket.second < 0.0)
            {
                bracket.second += step;
                fBracket.second = cdf(bracket.second);
            }
        }

        maths::CEqualWithTolerance<double> equal(maths::CToleranceTypes::E_AbsoluteTolerance, 2.0 * eps);

        maths::CSolvers::solve(bracket.first, bracket.second,
                               fBracket.first, fBracket.second,
                               cdf, maxIterations, equal, result);
    }
    catch (const std::exception &e)
    {
        LOG_ERROR("Failed to compute quantile: " << e.what()
                  << ", quantile = " << percentage);
        return false;
    }

    return true;
}

bool CPriorTestInterface::marginalLikelihoodMeanForTest(double &result) const
{
    typedef maths::CCompositeFunctions::CExp<const maths::CPriorUtils::CLogMarginalLikelihood<maths::CPrior>&> TPdf;
    typedef maths::CCompositeFunctions::CProduct<bool (*)(double, double&), TPdf> TXTimesPdf;

    const double eps = 1e-3;
    unsigned int steps = 100u;

    result = 0.0;

    double a, b;
    if (   !this->marginalLikelihoodQuantileForTest(0.001, eps, a)
        || !this->marginalLikelihoodQuantileForTest(99.999, eps, b))
    {
        LOG_ERROR("Unable to compute mean likelihood");
        return false;
    }

    if (m_Prior->dataType() == maths_t::E_IntegerData)
    {
        b = ::ceil(b);
        a = ::floor(a);
        steps = static_cast<unsigned int>(b - a) + 1;
    }

    maths::CPriorUtils::CLogMarginalLikelihood<maths::CPrior> logPdf(*m_Prior);
    TXTimesPdf xTimesPdf(feedthrough, TPdf(logPdf));

    double x = a;
    double step = (b - a) / static_cast<double>(steps);

    for (unsigned int i = 0; i < steps; ++i, x += step)
    {
        double integral;
        if (!maths::CIntegration::gaussLegendre<maths::CIntegration::OrderThree>(xTimesPdf, x, x + step, integral))
        {
            return false;
        }
        result += integral;
    }

    return true;
}

bool CPriorTestInterface::marginalLikelihoodVarianceForTest(double &result) const
{
    typedef maths::CCompositeFunctions::CExp<const maths::CPriorUtils::CLogMarginalLikelihood<maths::CPrior>&> TPdf;
    typedef maths::CCompositeFunctions::CProduct<CResidual, TPdf> TResidualTimesPdf;

    const double eps = 1e-3;
    unsigned int steps = 100u;

    result = 0.0;

    double a, b;
    if (   !this->marginalLikelihoodQuantileForTest(0.001, eps, a)
        || !this->marginalLikelihoodQuantileForTest(99.999, eps, b))
    {
        LOG_ERROR("Unable to compute mean likelihood");
        return false;
    }

    if (m_Prior->dataType() == maths_t::E_IntegerData)
    {
        b = ::ceil(b);
        a = ::floor(a);
        steps = static_cast<unsigned int>(b - a) + 1;
    }

    maths::CPriorUtils::CLogMarginalLikelihood<maths::CPrior> logPdf(*m_Prior);
    TResidualTimesPdf residualTimesPdf(CResidual(m_Prior->marginalLikelihoodMean()), TPdf(logPdf));

    double x = a;
    double step = (b - a) / static_cast<double>(steps);
    for (unsigned int i = 0; i < steps; ++i, x += step)
    {
        double integral;
        if (!maths::CIntegration::gaussLegendre<maths::CIntegration::OrderThree>(residualTimesPdf, x, x + step, integral))
        {
            return false;
        }
        result += integral;
    }

    return true;
}

}

