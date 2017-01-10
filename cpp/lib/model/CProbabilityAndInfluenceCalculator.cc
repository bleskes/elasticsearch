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

#include <model/CProbabilityAndInfluenceCalculator.h>

#include <core/CLogger.h>

#include <maths/CBasicStatistics.h>
#include <maths/CMultivariatePrior.h>
#include <maths/COrderings.h>
#include <maths/CPrior.h>
#include <maths/CTimeSeriesDecomposition.h>
#include <maths/CTools.h>

#include <model/CAnnotatedProbabilityBuilder.h>
#include <model/CStringStore.h>

namespace ml
{
namespace model
{

namespace
{

typedef CProbabilityAndInfluenceCalculator::TDouble1Vec TDouble1Vec;
typedef CProbabilityAndInfluenceCalculator::TDouble2Vec TDouble2Vec;
typedef CProbabilityAndInfluenceCalculator::TDouble4Vec TDouble4Vec;
typedef CProbabilityAndInfluenceCalculator::TDouble4Vec1Vec TDouble4Vec1Vec;
typedef CProbabilityAndInfluenceCalculator::TDouble10Vec TDouble10Vec;
typedef CProbabilityAndInfluenceCalculator::TDouble10Vec1Vec TDouble10Vec1Vec;
typedef CProbabilityAndInfluenceCalculator::TDouble10Vec4Vec TDouble10Vec4Vec;
typedef CProbabilityAndInfluenceCalculator::TDouble10Vec4Vec1Vec TDouble10Vec4Vec1Vec;
typedef CProbabilityAndInfluenceCalculator::TDouble1VecDoublePr TDouble1VecDoublePr;
typedef CProbabilityAndInfluenceCalculator::TBool2Vec TBool2Vec;
typedef CProbabilityAndInfluenceCalculator::TTime2Vec TTime2Vec;
typedef CProbabilityAndInfluenceCalculator::TStrPtr TStrPtr;
typedef CProbabilityAndInfluenceCalculator::TStrCRefDouble1VecDoublePrPr TStrCRefDouble1VecDoublePrPr;
typedef CProbabilityAndInfluenceCalculator::TStrCRefDouble1VecDoublePrPrVec TStrCRefDouble1VecDoublePrPrVec;
typedef CProbabilityAndInfluenceCalculator::TStrCRefDouble1VecDouble1VecPrPr TStrCRefDouble1VecDouble1VecPrPr;
typedef CProbabilityAndInfluenceCalculator::TStrCRefDouble1VecDouble1VecPrPrVec TStrCRefDouble1VecDouble1VecPrPrVec;
typedef CProbabilityAndInfluenceCalculator::TStrPtrStrPtrPr TStrPtrStrPtrPr;
typedef CProbabilityAndInfluenceCalculator::TStrPtrStrPtrPrDoublePr TStrPtrStrPtrPrDoublePr;
typedef CProbabilityAndInfluenceCalculator::TStrPtrStrPtrPrDoublePrVec TStrPtrStrPtrPrDoublePrVec;
typedef CProbabilityAndInfluenceCalculator::TMultivariatePriorCPtrSizePr1Vec TMultivariatePriorCPtrSizePr1Vec;
typedef CProbabilityAndInfluenceCalculator::TDecompositionCPtr TDecompositionCPtr;
typedef CProbabilityAndInfluenceCalculator::TDecompositionCPtr1Vec TDecompositionCPtr1Vec;
typedef CProbabilityAndInfluenceCalculator::TTail10Vec TTail10Vec;
typedef core::CSmallVector<TDouble10Vec, 2> TDouble10Vec2Vec;
typedef core::CSmallVector<std::size_t, 10> TSize10Vec;
typedef core::CSmallVector<maths_t::EProbabilityCalculation, 10> TProbabilityCalculation10Vec;

const double EFFECTIVE_COUNT[] = { 1.0, 0.8, 0.7, 0.65, 0.6, 0.57, 0.54, 0.52, 0.51 };

//! Get the effective count per correlate model for calibrating aggregation.
double effectiveCount(std::size_t n)
{
    return n <= boost::size(EFFECTIVE_COUNT) ? EFFECTIVE_COUNT[n-1] : 0.5;
}

//! Check if \p lhs is less than or equal to \p rhs.
bool lessThanEqual(double lhs, double rhs)
{
    return lhs <= rhs;
}

//! Check if all the elements of \p lhs are less than or equal to the \p rhs.
bool lessThanEqual(const TDouble10Vec &lhs, double rhs)
{
    for (std::size_t i = 0u; i < lhs.size(); ++i)
    {
        if (lhs[i] > rhs)
        {
            return false;
        }
    }
    return true;
}

//! Check if \p rhs is greater than or equal to \p rhs.
bool greaterThanEqual(double lhs, double rhs)
{
    return lhs >= rhs;
}

//! Check if all the elements of \p lhs are less than or equal to the \p rhs.
bool greaterThanEqual(const TDouble10Vec &lhs, double rhs)
{
    for (std::size_t i = 0u; i < lhs.size(); ++i)
    {
        if (lhs[i] < rhs)
        {
            return false;
        }
    }
    return true;
}

//! Get the correction to apply to the one-sided probability calculations.
template<typename VALUE>
double oneSidedEmptyBucketCorrection(maths_t::EProbabilityCalculation calculation,
                                     const VALUE &value,
                                     double probabilityEmptyBucket)
{
    switch (calculation)
    {
    case maths_t::E_OneSidedBelow:
        return greaterThanEqual(value, 0.0) ? 2.0 * probabilityEmptyBucket : 0.0;
    case maths_t::E_OneSidedAbove:
        return lessThanEqual(value, 0.0)    ? 2.0 * probabilityEmptyBucket : 0.0;
    case maths_t::E_TwoSided:
        break;
    }
    return 0.0;
}

//! Correct \p probability with \p probabilityEmptyBucket.
double correctForEmptyBucket(maths_t::EProbabilityCalculation calculation,
                             double value,
                             bool bucketEmpty,
                             double probabilityEmptyBucket,
                             double probability)
{
    double pCorrected = (1.0 - probabilityEmptyBucket) * probability;

    if (!bucketEmpty)
    {
        double pOneSided = oneSidedEmptyBucketCorrection(calculation, value, probabilityEmptyBucket);
        return std::min(pOneSided + pCorrected, 1.0);
    }

    return probabilityEmptyBucket + pCorrected;
}

//! Correct \p probability with \p probabilityEmptyBucket.
double correctForEmptyBucket(maths_t::EProbabilityCalculation calculation,
                             const TDouble10Vec &value,
                             bool bucketEmpty,
                             double probabilityEmptyBucket,
                             double probability)
{
    double pCorrected = (1.0 - probabilityEmptyBucket) * probability;

    if (!bucketEmpty)
    {
        double pOneSided = oneSidedEmptyBucketCorrection(calculation, value, probabilityEmptyBucket);
        return std::min(pOneSided + pCorrected, 1.0);
    }

    return probabilityEmptyBucket + pCorrected;
}

//! Correct \p probability with \p probabilityEmptyBucket.
double correctForEmptyBucket(maths_t::EProbabilityCalculation calculation,
                             double value,
                             const TBool2Vec &bucketEmpty,
                             const TDouble2Vec &probabilityEmptyBucket,
                             double probability)
{
    if (!bucketEmpty[0] && !bucketEmpty[1])
    {
        double pState = (1.0 - probabilityEmptyBucket[0]) * (1.0 - probabilityEmptyBucket[1]);
        double pOneSided = oneSidedEmptyBucketCorrection(calculation, value, (1.0 - pState));
        return std::min(pOneSided + pState * probability, 1.0);
    }

    if (!bucketEmpty[0])
    {
        double pState = (1.0 - probabilityEmptyBucket[0]) * probabilityEmptyBucket[1];
        double pOneSided = oneSidedEmptyBucketCorrection(calculation, value, probabilityEmptyBucket[0]);
        return std::min(pOneSided + pState + (1.0 - pState) * probability, 1.0);
    }

    if (!bucketEmpty[1])
    {
        double pState = probabilityEmptyBucket[0] * (1.0 - probabilityEmptyBucket[1]);
        double pOneSided = oneSidedEmptyBucketCorrection(calculation, value, probabilityEmptyBucket[1]);
        return std::min(pOneSided + pState + (1.0 - pState) * probability, 1.0);

    }

    double pState = probabilityEmptyBucket[0] * probabilityEmptyBucket[1];
    return pState + (1.0 - pState) * probability;
}

//! Compute the probability for a collection of correlates.
//!
//! \param[in] calculation The style of probability calculation.
//! \param[in] priors Any correlate priors for \p value if available.
//! \param[in] weightStyles Controls the interpretation of \p weights.
//! \param[in] values The correlate values including \p value.
//! \param[in] weights The weights to apply when computing correlate
//! probabilities.
//! \param[out] probability Filled in with the probability.
//! \param[out] tail Filled in with the tail.
//! \param[out] type Filled in with the type of anomaly, i.e. is the
//! value anomalous in its own right or as a result of conditioning
//! on a correlated variable.
//! \param[out] mostAnomalousCorrelate Filled in with the index of
//! the most anomalous correlate.
bool computeProbability(maths_t::EProbabilityCalculation calculation,
                        const TMultivariatePriorCPtrSizePr1Vec &priors,
                        const maths_t::TWeightStyleVec &weightStyles,
                        const TDouble10Vec1Vec &values,
                        const TDouble10Vec4Vec1Vec &weights,
                        const TBool2Vec &bucketEmpty,
                        const TDouble2Vec &probabilityBucketEmpty,
                        double &probability,
                        maths_t::ETail &tail,
                        model_t::CResultType &type,
                        std::size_t &mostAnomalousCorrelate)
{
    probability = 1.0;
    tail = maths_t::E_UndeterminedTail;
    type.set(model_t::CResultType::E_Unconditional);
    mostAnomalousCorrelate = 0;

    std::size_t n = priors.size();
    if (n > 0)
    {
        double neff = effectiveCount(n);
        maths::CProbabilityOfExtremeSample probabilityCalculator;
        maths::CBasicStatistics::COrderStatisticsStack<double, 1> minProbability;

        // Declared outside the loop to minimize the number of times they are created.
        TSize10Vec coordinate(1);
        TDouble10Vec1Vec value(1);
        TDouble10Vec4Vec1Vec weight(1);
        TBool2Vec bucketEmptyi(2, bucketEmpty[0]);
        TDouble2Vec probabilityBucketEmptyi(2, probabilityBucketEmpty[0]);
        TDouble10Vec2Vec li, ui;
        TTail10Vec ti;

        for (std::size_t i = 0u; i < n; ++i)
        {
            coordinate[0] = priors[i].second;
            value[0]  = values[i];
            weight[0] = weights[i];
            if (priors[i].first->probabilityOfLessLikelySamples(
                                         calculation,
                                         weightStyles, value, weight, coordinate,
                                         li, ui, ti))
            {
                LOG_TRACE("Marginal P(" << core::CContainerPrinter::print(value)
                          << " | weight = " << core::CContainerPrinter::print(weight)
                          << ", coordinate = " << core::CContainerPrinter::print(coordinate)
                          << ") = " << (li[0][0] + ui[0][0]) / 2.0);
                LOG_TRACE("Conditional P(" << core::CContainerPrinter::print(value)
                          << " | weight = " << core::CContainerPrinter::print(weight)
                          << ", coordinate = " << core::CContainerPrinter::print(coordinate)
                          << ") = " << (li[1][0] + ui[1][0]) / 2.0);
            }
            else
            {
                LOG_ERROR("Failed to compute P(" << core::CContainerPrinter::print(value)
                          << " | weight = " << core::CContainerPrinter::print(weight)
                          << ", coordinate = " << core::CContainerPrinter::print(coordinate)
                          << ")");
                continue;
            }

            bucketEmptyi[1] = bucketEmpty[i+1];
            probabilityBucketEmptyi[1] = probabilityBucketEmpty[i+1];
            double pl = ::sqrt(li[0][0] * li[1][0]);
            double pu = ::sqrt(ui[0][0] * ui[1][0]);
            double p = correctForEmptyBucket(calculation, value[0][coordinate[i]],
                                             bucketEmptyi,
                                             probabilityBucketEmpty,
                                             (pl + pu) / 2.0);
            probabilityCalculator.add(p, neff);
            if (minProbability.add(p))
            {
                tail = ti[0];
                mostAnomalousCorrelate = i;
                double pm = (li[0][0] + ui[0][0]) / 2.0;
                double pc = (li[1][0] + ui[1][0]) / 2.0;
                type.set(pm < pc ? model_t::CResultType::E_Unconditional :
                                   model_t::CResultType::E_Conditional);
            }
        }
        probabilityCalculator.calculate(probability);
    }

    return true;
}

//! Get the canonical influence string pointer.
TStrPtr canonical(const std::string &influence)
{
    return CStringStore::influencers().get(influence);
}

//! \brief Orders two value influences by decreasing influence.
class CDecreasingValueInfluence
{
    public:
        CDecreasingValueInfluence(maths_t::ETail tail) : m_Tail(tail) {}

        bool operator()(const TStrCRefDouble1VecDoublePrPr &lhs,
                        const TStrCRefDouble1VecDoublePrPr &rhs) const
        {
            return m_Tail == maths_t::E_LeftTail ?
                   lhs.second.first < rhs.second.first :
                   lhs.second.first > rhs.second.first;
        }

    private:
        maths_t::ETail m_Tail;
};

//! \brief Orders two mean influences by decreasing influence.
class CDecreasingMeanInfluence
{
    public:
        typedef maths::CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;

    public:
        CDecreasingMeanInfluence(maths_t::ETail tail, const TDouble1Vec &value, double count) :
                m_Tail(tail),
                m_Mean(maths::CBasicStatistics::accumulator(count, value[0]))
        {}

        bool operator()(const TStrCRefDouble1VecDoublePrPr &lhs,
                        const TStrCRefDouble1VecDoublePrPr &rhs) const
        {
            TMeanAccumulator l = m_Mean - maths::CBasicStatistics::accumulator(lhs.second.second,
                                                                              lhs.second.first[0]);
            TMeanAccumulator r = m_Mean - maths::CBasicStatistics::accumulator(rhs.second.second,
                                                                               rhs.second.first[0]);
            double ml = maths::CBasicStatistics::mean(l);
            double nl = maths::CBasicStatistics::count(l);
            double mr = maths::CBasicStatistics::mean(r);
            double nr = maths::CBasicStatistics::count(r);
            return m_Tail == maths_t::E_LeftTail ?
                   maths::COrderings::lexicographical_compare(mr, nl, ml, nr) :
                   maths::COrderings::lexicographical_compare(ml, nl, mr, nr);
        }

    private:
        maths_t::ETail m_Tail;
        TMeanAccumulator m_Mean;
};

//! \brief Orders two variance influences by decreasing influence.
class CDecreasingVarianceInfluence
{
    public:
         typedef maths::CBasicStatistics::SSampleMeanVar<double>::TAccumulator TMeanVarAccumulator;

    public:
        CDecreasingVarianceInfluence(maths_t::ETail tail, const TDouble1Vec &value, double count) :
                m_Tail(tail),
                m_Variance(maths::CBasicStatistics::accumulator(count, value[1], value[0]))
        {}

        bool operator()(const TStrCRefDouble1VecDoublePrPr &lhs,
                        const TStrCRefDouble1VecDoublePrPr &rhs) const
        {
            TMeanVarAccumulator l = m_Variance - maths::CBasicStatistics::accumulator(lhs.second.second,
                                                                                      lhs.second.first[1],
                                                                                      lhs.second.first[0]);
            TMeanVarAccumulator r = m_Variance - maths::CBasicStatistics::accumulator(rhs.second.second,
                                                                                      rhs.second.first[1],
                                                                                      rhs.second.first[0]);
            double vl = maths::CBasicStatistics::maximumLikelihoodVariance(l);
            double nl = maths::CBasicStatistics::count(l);
            double vr = maths::CBasicStatistics::maximumLikelihoodVariance(r);
            double nr = maths::CBasicStatistics::count(r);
            return m_Tail == maths_t::E_LeftTail ?
                   maths::COrderings::lexicographical_compare(vr, nl, vl, nr) :
                   maths::COrderings::lexicographical_compare(vl, nl, vr, nr);
        }

    private:
        maths_t::ETail m_Tail;
        TMeanVarAccumulator m_Variance;
};

//! A safe ratio function \p numerator / \p denominator dealing
//! with the case \p n and/or \p d are zero.
double ratio(double numerator,
             double denominator,
             double zeroDividedByZero)
{
    if (denominator == 0.0)
    {
        if (numerator == 0.0)
        {
            return zeroDividedByZero;
        }
        return numerator < 0.0 ? -std::numeric_limits<double>::max() :
                                  std::numeric_limits<double>::max();
    }
    return numerator / denominator;
}

//! \brief Implements common value difference functionality.
class CDifference
{
    public:
        typedef core::CSmallVector<core_t::TTime, 2> TTime2Vec;

    public:
        CDifference(const TDecompositionCPtr1Vec &trend,
                    core_t::TTime time,
                    double confidence) :
                m_Trend(&trend), m_Time(1, time), m_Confidence(confidence)
        {}
        CDifference(const TDecompositionCPtr1Vec &trend,
                    const TTime2Vec &time,
                    double confidence) :
                m_Trend(&trend), m_Time(time), m_Confidence(confidence)
        {}

        double detrend(double v) const
        {
            return this->detrend(0, v);
        }
        double detrend(std::size_t i, double v) const
        {
            core_t::TTime time = m_Time.size() == 1 ? m_Time[0] : m_Time[i];
            return m_Trend->empty() || !(*m_Trend)[i] ?
                   v : (*m_Trend)[i]->detrend(time, v, m_Confidence);
        }

    private:
        const TDecompositionCPtr1Vec *m_Trend;
        TTime2Vec m_Time;
        double m_Confidence;

};

//! \brief Computes the value of summed statistics on the set difference.
class CValueDifference : private CDifference
{
    public:
        CValueDifference(const TDecompositionCPtr1Vec &trend,
                         core_t::TTime time,
                         double confidence) :
                CDifference(trend, time, confidence)
        {}
        CValueDifference(const TDecompositionCPtr1Vec &trend,
                         const TTime2Vec &time,
                         double confidence) :
                CDifference(trend, time, confidence)
        {}

        //! Univariate features.
        void operator()(const TDouble1Vec &v,
                        double /*n*/,
                        const TDouble1Vec &vi,
                        double /*ni*/,
                        const maths_t::TWeightStyleVec &/*weightStyles*/,
                        double &sample,
                        TDouble4Vec &/*weights*/) const
        {
            sample = this->detrend(v[0] - vi[0]);
        }

        //! Correlates.
        void operator()(const TDouble10Vec &v,
                        const TDouble10Vec &/*n*/,
                        const TDouble1Vec &vi,
                        const TDouble1Vec &/*ni*/,
                        const maths_t::TWeightStyleVec &/*weightStyles*/,
                        TDouble10Vec &sample,
                        TDouble10Vec4Vec &/*weights*/) const
        {
            for (std::size_t i = 0u; i < v.size(); ++i)
            {
                sample[i] = this->detrend(i, v[i] - vi[i]);
            }
        }

        //! Multivariate features.
        void operator()(const TDouble10Vec &v,
                        double /*n*/,
                        const TDouble1Vec &vi,
                        double /*ni*/,
                        const maths_t::TWeightStyleVec &/*weightStyles*/,
                        TDouble10Vec &sample,
                        TDouble10Vec4Vec &/*weights*/) const
        {
            for (std::size_t i = 0u; i < v.size(); ++i)
            {
                sample[i] = this->detrend(i, v[i] - vi[i]);
            }
        }
};

//! \brief Computes the value of min, max, dc, etc on the set intersection.
class CValueIntersection : private CDifference
{
    public:
        CValueIntersection(const TDecompositionCPtr1Vec &trend,
                           core_t::TTime time,
                           double confidence) :
                CDifference(trend, time, confidence)
        {}
        CValueIntersection(const TDecompositionCPtr1Vec &trend,
                           const TTime2Vec &time,
                           double confidence) :
                CDifference(trend, time, confidence)
        {}

        //! Univariate features.
        void operator()(const TDouble1Vec &/*v*/,
                        double /*n*/,
                        const TDouble1Vec &vi,
                        double /*ni*/,
                        const maths_t::TWeightStyleVec &/*weightStyles*/,
                        double &sample,
                        TDouble4Vec &/*weights*/) const
        {
            sample = this->detrend(vi[0]);
        }

        //! Correlates.
        void operator()(const TDouble10Vec &/*v*/,
                        const TDouble10Vec &/*n*/,
                        const TDouble1Vec &vi,
                        const TDouble1Vec &/*ni*/,
                        const maths_t::TWeightStyleVec &/*weightStyles*/,
                        TDouble10Vec &sample,
                        TDouble10Vec4Vec &/*weights*/) const
        {
            for (std::size_t i = 0u; i < vi.size(); ++i)
            {
                sample[i] = this->detrend(i, vi[i]);
            }
        }

        //! Multivariate features.
        void operator()(const TDouble10Vec &/*v*/,
                        double /*n*/,
                        const TDouble1Vec &vi,
                        double /*ni*/,
                        const maths_t::TWeightStyleVec &/*weightStyles*/,
                        TDouble10Vec &sample,
                        TDouble10Vec4Vec &/*weights*/) const
        {
            for (std::size_t i = 0u; i < vi.size(); ++i)
            {
                sample[i] = this->detrend(i, vi[i]);
            }
        }
};

//! \brief Computes the value of the mean statistic on a set difference.
class CMeanDifference : private CDifference
{
    public:
        CMeanDifference(const TDecompositionCPtr1Vec &trend,
                        core_t::TTime time,
                        double confidence) :
                CDifference(trend, time, confidence)
        {}
        CMeanDifference(const TDecompositionCPtr1Vec &trend,
                        const TTime2Vec &time,
                        double confidence) :
                CDifference(trend, time, confidence)
        {}

        //! Univariate features.
        void operator()(const TDouble1Vec &v,
                        double n,
                        const TDouble1Vec &vi,
                        double ni,
                        const maths_t::TWeightStyleVec &weightStyles,
                        double &sample,
                        TDouble4Vec &weights) const
        {
            double d = maths::CBasicStatistics::mean(  maths::CBasicStatistics::accumulator( n,  v[0])
                                                     - maths::CBasicStatistics::accumulator(ni, vi[0]));
            sample = this->detrend(d);
            for (std::size_t i = 0u; i < weightStyles.size(); ++i)
            {
                switch (weightStyles[i])
                {
                case maths_t::E_SampleCountWeight: break;
                case maths_t::E_SampleSeasonalVarianceScaleWeight: break;
                case maths_t::E_SampleCountVarianceScaleWeight: weights[i] *= n / (n - ni); break;
                case maths_t::E_SampleWinsorisationWeight: break;
                }
            }
        }

        //! Correlates.
        void operator()(const TDouble10Vec &v,
                        const TDouble10Vec &n,
                        const TDouble1Vec &vi,
                        const TDouble1Vec &ni,
                        const maths_t::TWeightStyleVec &weightStyles,
                        TDouble10Vec &sample,
                        TDouble10Vec4Vec &weights) const
        {
            for (std::size_t i = 0u; i < v.size(); ++i)
            {
                double d = maths::CBasicStatistics::mean(  maths::CBasicStatistics::accumulator( n[i],  v[i])
                                                         - maths::CBasicStatistics::accumulator(ni[i], vi[i]));
                sample[i] = this->detrend(i, d);
                for (std::size_t j = 0u; j < weightStyles.size(); ++j)
                {
                    switch (weightStyles[j])
                    {
                    case maths_t::E_SampleCountWeight: break;
                    case maths_t::E_SampleSeasonalVarianceScaleWeight: break;
                    case maths_t::E_SampleCountVarianceScaleWeight: weights[j][i] *= n[i] / (n[i] - ni[i]); break;
                    case maths_t::E_SampleWinsorisationWeight: break;
                    }
                }
            }
        }

        //! Multivariate features.
        void operator()(const TDouble10Vec &v,
                        double n,
                        const TDouble1Vec &vi,
                        double ni,
                        const maths_t::TWeightStyleVec &weightStyles,
                        TDouble10Vec &sample,
                        TDouble10Vec4Vec &weights) const
        {
            for (std::size_t i = 0u; i < v.size(); ++i)
            {
                double d = maths::CBasicStatistics::mean(  maths::CBasicStatistics::accumulator( n,  v[i])
                                                         - maths::CBasicStatistics::accumulator(ni, vi[i]));
                sample[i] = this->detrend(i, d);
                for (std::size_t j = 0u; j < weightStyles.size(); ++j)
                {
                    switch (weightStyles[j])
                    {
                    case maths_t::E_SampleCountWeight: break;
                    case maths_t::E_SampleSeasonalVarianceScaleWeight: break;
                    case maths_t::E_SampleCountVarianceScaleWeight: weights[j][i] *= n / (n - ni); break;
                    case maths_t::E_SampleWinsorisationWeight: break;
                    }
                }
            }
        }
};

//! \brief Computes the value of the variance statistic on a set difference.
class CVarianceDifference : private CDifference
{
    public:
        CVarianceDifference(const TDecompositionCPtr1Vec &trend,
                            core_t::TTime time,
                            double confidence) :
                CDifference(trend, time, confidence)
        {}
        CVarianceDifference(const TDecompositionCPtr1Vec &trend,
                            const TTime2Vec &time,
                            double confidence) :
                CDifference(trend, time, confidence)
        {}

        //! Univariate features.
        void operator()(const TDouble1Vec &v,
                        double n,
                        const TDouble1Vec &vi,
                        double ni,
                        const maths_t::TWeightStyleVec &weightStyles,
                        double &sample,
                        TDouble4Vec &weights) const
        {
            double d = maths::CBasicStatistics::maximumLikelihoodVariance(
                               maths::CBasicStatistics::accumulator( n,  v[1],  v[0])
                             - maths::CBasicStatistics::accumulator(ni, vi[1], vi[0]));
            sample = this->detrend(d);
            for (std::size_t i = 0u; i < weightStyles.size(); ++i)
            {
                switch (weightStyles[i])
                {
                case maths_t::E_SampleCountWeight: break;
                case maths_t::E_SampleSeasonalVarianceScaleWeight: break;
                case maths_t::E_SampleCountVarianceScaleWeight: weights[i] *= n / (n - ni); break;
                case maths_t::E_SampleWinsorisationWeight: break;
                }
            }
        }

        //! Correlates.
        void operator()(const TDouble10Vec &v,
                        const TDouble10Vec &n,
                        const TDouble1Vec &vi,
                        const TDouble1Vec &ni,
                        const maths_t::TWeightStyleVec &weightStyles,
                        TDouble10Vec &sample,
                        TDouble10Vec4Vec &weights) const
        {
            std::size_t dimension = v.size();
            for (std::size_t i = 0u; i < dimension; ++i)
            {
                double d = maths::CBasicStatistics::mean(
                                   maths::CBasicStatistics::accumulator( n[i],  v[dimension + i],  v[i])
                                 - maths::CBasicStatistics::accumulator(ni[i], vi[dimension + i], vi[i]));
                sample[i] = this->detrend(i, d);
                for (std::size_t j = 0u; j < weightStyles.size(); ++j)
                {
                    switch (weightStyles[j])
                    {
                    case maths_t::E_SampleCountWeight: break;
                    case maths_t::E_SampleSeasonalVarianceScaleWeight: break;
                    case maths_t::E_SampleCountVarianceScaleWeight: weights[j][i] *= n[i] / (n[i] - ni[i]); break;
                    case maths_t::E_SampleWinsorisationWeight: break;
                    }
                }
            }
        }

        //! Multivariate features.
        void operator()(const TDouble10Vec &v,
                        double n,
                        const TDouble1Vec &vi,
                        double ni,
                        const maths_t::TWeightStyleVec &weightStyles,
                        TDouble10Vec &sample,
                        TDouble10Vec4Vec &weights) const
        {
            std::size_t dimension = v.size();
            for (std::size_t i = 0u; i < dimension; ++i)
            {
                double d = maths::CBasicStatistics::mean(
                                   maths::CBasicStatistics::accumulator( n,  v[dimension + i],  v[i])
                                 - maths::CBasicStatistics::accumulator(ni, vi[dimension + i], vi[i]));
                sample[i] = this->detrend(i, d);
                for (std::size_t j = 0u; j < weightStyles.size(); ++j)
                {
                    switch (weightStyles[j])
                    {
                    case maths_t::E_SampleCountWeight: break;
                    case maths_t::E_SampleSeasonalVarianceScaleWeight: break;
                    case maths_t::E_SampleCountVarianceScaleWeight: weights[j][i] *= n / (n - ni); break;
                    case maths_t::E_SampleWinsorisationWeight: break;
                    }
                }
            }
        }
};

//! Sets all influences to one.
//!
//! \param[in] influencerName The name of the influencer field.
//! \param[in] influencerValues The feature values for the intersection
//! of the records in \p value with distinct values of \p influenceName.
//! \param[out] result Filled in with the influences of \p value.
template<typename INFLUENCER_VALUES>
void doComputeIndicatorInfluences(TStrPtr influencerName,
                                  const INFLUENCER_VALUES &influencerValues,
                                  TStrPtrStrPtrPrDoublePrVec &result)
{
    result.reserve(influencerValues.size());
    for (std::size_t i = 0u; i < influencerValues.size(); ++i)
    {
        result.push_back(TStrPtrStrPtrPrDoublePr(
                             TStrPtrStrPtrPr(influencerName, canonical(influencerValues[i].first)), 1.0));
    }
}

//! Implement the influence calculation for univariate features using
//! \p computeSample to get the statistics and \p computeInfluence to
//! compute the influences from the corresponding probabilities.
//!
//! \param[in] computeInfluencedValue The function to compute the
//! influenced feature value for which to compute the probability.
//! \param[in] computeInfluence The function to compute influence.
//! \param[in] prior The model to use to compute the probability.
//! \param[in] calculation The side of the probability calculation.
//! \param[in] weightStyles Controls the interpretation of \p weights.
//! \param[in] value The influenced feature value.
//! \param[in] weights The weights to apply when computing probability.
//! \param[in] count The measurement count in \p value.
//! \param[in] probabilityBucketEmpty The probability the feature value
//! is present.
//! \param[in] probability The probability of \p value.
//! \param[in] influencerName The name of the influencer field.
//! \param[in] influencerValues The feature values for the intersection
//! of the records in \p value with distinct values of \p influenceName.
//! \param[in] cutoff The value at which there is no influence.
//! \param[in] includeCutoff If true then add in values for influences
//! less than the cutoff with estimated influence.
//! \param[out] result Filled in with the influences of \p value.
template<typename COMPUTE_INFLUENCED_VALUE, typename COMPUTE_INFLUENCE>
void doComputeInfluences(model_t::EFeature feature,
                         COMPUTE_INFLUENCED_VALUE computeInfluencedValue,
                         COMPUTE_INFLUENCE computeInfluence,
                         const maths::CPrior &prior,
                         core_t::TTime elapsedTime,
                         maths_t::EProbabilityCalculation calculation,
                         const TDouble1Vec &value,
                         double count,
                         const maths_t::TWeightStyleVec &weightStyles,
                         const TDouble4Vec1Vec &weights,
                         double probabilityBucketEmpty,
                         double probability,
                         TStrPtr influencerName,
                         const TStrCRefDouble1VecDoublePrPrVec &influencerValues,
                         double cutoff,
                         bool includeCutoff,
                         TStrPtrStrPtrPrDoublePrVec &result)
{
    if (influencerValues.size() == 1)
    {
        result.push_back(TStrPtrStrPtrPrDoublePr(
                             TStrPtrStrPtrPr(influencerName, canonical(influencerValues[0].first)), 1.0));
        return;
    }
    if (probability == 1.0)
    {
        doComputeIndicatorInfluences(influencerName, influencerValues, result);
        return;
    }

    result.reserve(influencerValues.size());

    // Declared outside the loop to minimize the number of times they are created.
    TDouble1Vec influencedValue(1);
    TDouble4Vec1Vec influencedWeight(1);
    double logp = ::log(probability);
    for (std::size_t i = 0u; i < influencerValues.size(); ++i)
    {
        const TStrCRefDouble1VecDoublePrPr &influencerValue = influencerValues[i];

        influencedWeight[0] = weights[0];
        computeInfluencedValue(value, count,
                               influencerValue.second.first,
                               influencerValue.second.second,
                               weightStyles, influencedValue[0], influencedWeight[0]);

        double lb, ub;
        maths_t::ETail tail;
        if (!prior.probabilityOfLessLikelySamples(calculation,
                                                  weightStyles, influencedValue, influencedWeight,
                                                  lb, ub, tail))
        {
            LOG_ERROR("Failed to compute P(" << influencedValue[0]
                      << " | weight = " << core::CContainerPrinter::print(influencedWeight)
                      << ", weightStyles = " << core::CContainerPrinter::print(weightStyles)
                      << ", influencer = " << core::CContainerPrinter::print(influencerValue) << ")");
            continue;
        }
        double pi = maths::CTools::truncate((lb + ub) / 2.0, maths::CTools::smallestProbability(), 1.0);
        bool bucketEmpty = (count - influencerValue.second.second == 0.0);
        pi = correctForEmptyBucket(model_t::probabilityCalculation(feature),
                                   influencedValue[0], bucketEmpty, probabilityBucketEmpty, pi);
        pi = model_t::adjustProbability(feature, elapsedTime, pi);

        double influence = computeInfluence(logp, ::log(pi));

        LOG_TRACE("log(p) = " << logp
                  << ", tail = " << tail
                  << ", v(i) = " << core::CContainerPrinter::print(influencedValue)
                  << ", log(p(i)) = " << ::log(pi)
                  << ", weight = " << core::CContainerPrinter::print(influencedWeight)
                  << ", influence = " << influence
                  << ", influencer field value = " << influencerValues[i].first.get());

        if (influence >= cutoff)
        {
            result.push_back(TStrPtrStrPtrPrDoublePr(
                                 TStrPtrStrPtrPr(influencerName, canonical(influencerValues[i].first)),
                                 influence));
        }
        else if (includeCutoff)
        {
            result.push_back(TStrPtrStrPtrPrDoublePr(
                                 TStrPtrStrPtrPr(influencerName, canonical(influencerValues[i].first)),
                                 influence));
            for (++i; i < influencerValues.size(); ++i)
            {
                result.push_back(TStrPtrStrPtrPrDoublePr(
                                     TStrPtrStrPtrPr(influencerName, canonical(influencerValues[i].first)),
                                     0.5 * influence));
            }
        }
        else
        {
            break;
        }
    }
}

//! Implement the influence calculation for multivariate features using
//! \p computeSample to get the statistics and \p computeInfluence to
//! compute the influences from the corresponding probabilities.
template<typename COMPUTE_INFLUENCED_VALUE, typename COMPUTE_INFLUENCE>
void doComputeCorrelateInfluences(model_t::EFeature feature,
                                  COMPUTE_INFLUENCED_VALUE computeInfluencedValue,
                                  COMPUTE_INFLUENCE computeInfluence,
                                  const maths::CMultivariatePrior &prior,
                                  const TTime2Vec &elapsedTimes,
                                  const TSize10Vec &coordinate,
                                  maths_t::EProbabilityCalculation calculation,
                                  const TDouble10Vec &value,
                                  const TDouble10Vec &count,
                                  const maths_t::TWeightStyleVec &weightStyles,
                                  const TDouble10Vec4Vec1Vec &weights,
                                  const TBool2Vec &bucketEmpty,
                                  const TDouble2Vec &probabilityBucketEmpty,
                                  double probability,
                                  const maths::CProbabilityOfExtremeSample &probabilityCalculator,
                                  double neff,
                                  TStrPtr influencerName,
                                  const TStrCRefDouble1VecDouble1VecPrPrVec &influencerValues,
                                  double cutoff,
                                  bool includeCutoff,
                                  TStrPtrStrPtrPrDoublePrVec &result)
{
    core_t::TTime elapsedTime = *std::min_element(elapsedTimes.begin(), elapsedTimes.end());

    if (influencerValues.size() == 1)
    {
        result.push_back(TStrPtrStrPtrPrDoublePr(
                             TStrPtrStrPtrPr(influencerName, canonical(influencerValues[0].first)), 1.0));
        return;
    }
    if (probability == 1.0)
    {
        doComputeIndicatorInfluences(influencerName, influencerValues, result);
        return;
    }

    std::size_t d = prior.dimension();

    result.reserve(influencerValues.size());

    // Declared outside the loop to minimize the number of times they are created.
    TDouble10Vec1Vec influencedValue(1, TDouble10Vec(d));
    TDouble10Vec4Vec1Vec influencedWeight(1);
    TDouble10Vec2Vec lb, ub;
    TTail10Vec tail;
    double logp = ::log(probability);
    for (std::size_t i = 0u; i < influencerValues.size(); ++i)
    {
        const TStrCRefDouble1VecDouble1VecPrPr &influencerValue = influencerValues[i];

        influencedWeight[0] = weights[0];
        computeInfluencedValue(value, count,
                               influencerValue.second.first,
                               influencerValue.second.second,
                               weightStyles, influencedValue[0], influencedWeight[0]);

        if (!prior.probabilityOfLessLikelySamples(calculation,
                                                  weightStyles, influencedValue, influencedWeight,
                                                  coordinate, lb, ub, tail))
        {
            LOG_ERROR("Failed to compute P(" << core::CContainerPrinter::print(influencedValue)
                      << " | weight = " << core::CContainerPrinter::print(influencedWeight)
                      << ", weightStyles = " << core::CContainerPrinter::print(weightStyles)
                      << ", influencer = " << core::CContainerPrinter::print(influencerValue) << ")");
            continue;
        }
        double pl = ::sqrt(lb[0][0] * lb[1][0]);
        double pu = ::sqrt(ub[0][0] * ub[1][0]);
        double pi = maths::CTools::truncate((pl + pu) / 2.0, maths::CTools::smallestProbability(), 1.0);
        pi = correctForEmptyBucket(model_t::probabilityCalculation(feature),
                                   influencedValue[0][coordinate[0]],
                                   bucketEmpty, probabilityBucketEmpty, pi);
        pi = model_t::adjustProbability(feature, elapsedTime, pi);

        maths::CProbabilityOfExtremeSample pci = probabilityCalculator;
        pci.add(pi, neff);
        pci.calculate(pi);

        double influence = computeInfluence(logp, ::log(pi));

        LOG_TRACE("log(p) = " << logp
                  << ", v(i) = " << core::CContainerPrinter::print(influencedValue)
                  << ", log(p(i)) = " << ::log(pi)
                  << ", weight(i) = " << core::CContainerPrinter::print(influencedWeight)
                  << ", influence = " << influence
                  << ", influencer field value = " << influencerValues[i].first.get());

        if (includeCutoff || influence >= cutoff)
        {
            result.push_back(TStrPtrStrPtrPrDoublePr(
                                 TStrPtrStrPtrPr(influencerName, canonical(influencerValues[i].first)),
                                 influence));
        }
    }
}

//! Implement the influence calculation for multivariate features using
//! \p computeSample to get the statistics and \p computeInfluence to
//! compute the influences from the corresponding probabilities.
template<typename COMPUTE_INFLUENCED_VALUE, typename COMPUTE_INFLUENCE>
void doComputeMultivariateInfluences(model_t::EFeature feature,
                                     COMPUTE_INFLUENCED_VALUE computeInfluencedValue,
                                     COMPUTE_INFLUENCE computeInfluence,
                                     const maths::CMultivariatePrior &prior,
                                     core_t::TTime elapsedTime,
                                     const TSize10Vec &coordinates,
                                     const TProbabilityCalculation10Vec &calculations,
                                     const TDouble10Vec &value,
                                     double count,
                                     const maths_t::TWeightStyleVec &weightStyles,
                                     const TDouble10Vec4Vec1Vec &weights,
                                     double probabilityBucketEmpty,
                                     double probability,
                                     TStrPtr influencerName,
                                     const TStrCRefDouble1VecDoublePrPrVec &influencerValues,
                                     double cutoff,
                                     bool includeCutoff,
                                     TStrPtrStrPtrPrDoublePrVec &result)
{
    if (influencerValues.size() == 1)
    {
        result.push_back(TStrPtrStrPtrPrDoublePr(
                             TStrPtrStrPtrPr(influencerName, canonical(influencerValues[0].first)), 1.0));
        return;
    }
    if (probability == 1.0)
    {
        doComputeIndicatorInfluences(influencerName, influencerValues, result);
        return;
    }

    std::size_t d = prior.dimension();

    result.reserve(influencerValues.size());

    // Declared outside the loop to minimize the number of times they are created.
    TDouble10Vec1Vec influencedValue(1, TDouble10Vec(d));
    TDouble10Vec4Vec1Vec influencedWeight(1);
    TSize10Vec coordinate(1);
    TDouble10Vec2Vec lb, ub;
    TTail10Vec tail;
    double logp = ::log(probability);
    for (std::size_t i = 0u; i < influencerValues.size(); ++i)
    {
        const TStrCRefDouble1VecDoublePrPr &influencerValue = influencerValues[i];

        influencedWeight[0] = weights[0];
        computeInfluencedValue(value, count,
                               influencerValue.second.first,
                               influencerValue.second.second,
                               weightStyles, influencedValue[0], influencedWeight[0]);

        maths::CJointProbabilityOfLessLikelySamples pis_[2];

        for (std::size_t j = 0u; j < coordinates.size(); ++j)
        {
            coordinate[0] = coordinates[j];
            if (!prior.probabilityOfLessLikelySamples(calculations[j],
                                                      weightStyles, influencedValue, influencedWeight,
                                                      coordinate, lb, ub, tail))
            {
                LOG_ERROR("Failed to compute P(" << core::CContainerPrinter::print(influencedValue)
                          << " | weight = " << core::CContainerPrinter::print(influencedWeight)
                          << ", weightStyles = " << core::CContainerPrinter::print(weightStyles)
                          << ", influencer = " << core::CContainerPrinter::print(influencerValue) << ")");
                continue;
            }
            pis_[0].add(maths::CTools::truncate((lb[0][0] + ub[0][0]) / 2.0,
                                                maths::CTools::smallestProbability(), 1.0));
            pis_[1].add(maths::CTools::truncate((lb[1][0] + ub[1][0]) / 2.0,
                                                maths::CTools::smallestProbability(), 1.0));
        }

        double pis[2] = { 1.0, 1.0 };
        if (!pis_[0].calculate(pis[0]) || !pis_[0].calculate(pis[1]))
        {
            LOG_ERROR("Failed to compute P(" << core::CContainerPrinter::print(influencedValue)
                      << " | weights = " << core::CContainerPrinter::print(influencedWeight)
                      << ", influencer = " << core::CContainerPrinter::print(influencerValue) << ")");
            continue;
        }
        double pi = maths::CTools::truncate(::sqrt(pis[0] * pis[1]), maths::CTools::smallestProbability(), 1.0);
        bool bucketEmpty = (count - influencerValue.second.second == 0.0);
        pi = correctForEmptyBucket(model_t::probabilityCalculation(feature),
                                   influencedValue[0], bucketEmpty, probabilityBucketEmpty, pi);
        pi = model_t::adjustProbability(feature, elapsedTime, pi);

        double influence = computeInfluence(logp, ::log(pi));

        LOG_TRACE("log(p) = " << logp
                  << ", v(i) = " << core::CContainerPrinter::print(influencedValue)
                  << ", log(p(i)) = " << ::log(pi)
                  << ", weight(i) = " << core::CContainerPrinter::print(influencedWeight)
                  << ", influence = " << influence
                  << ", influencer field value = " << influencerValues[i].first.get());

        if (includeCutoff || influence >= cutoff)
        {
            result.push_back(TStrPtrStrPtrPrDoublePr(
                                 TStrPtrStrPtrPr(influencerName, canonical(influencerValues[i].first)),
                                 influence));
        }
    }
}

}

CProbabilityAndInfluenceCalculator::CProbabilityAndInfluenceCalculator(double cutoff) :
        m_Cutoff(cutoff),
        m_InfluenceCalculator(0),
        m_ProbabilityTemplate(CModelTools::CProbabilityAggregator::E_Min),
        m_Probability(CModelTools::CProbabilityAggregator::E_Min)
{
}

bool CProbabilityAndInfluenceCalculator::empty(void) const
{
    return m_Probability.empty();
}

double CProbabilityAndInfluenceCalculator::cutoff(void) const
{
    return m_Cutoff;
}

void CProbabilityAndInfluenceCalculator::plugin(const CInfluenceCalculator &influenceCalculator)
{
    m_InfluenceCalculator = &influenceCalculator;
}

void CProbabilityAndInfluenceCalculator::addAggregator(const maths::CJointProbabilityOfLessLikelySamples &aggregator)
{
    m_ProbabilityTemplate.add(aggregator);
    m_Probability.add(aggregator);
}

void CProbabilityAndInfluenceCalculator::addAggregator(const maths::CProbabilityOfExtremeSample &aggregator)
{
    m_ProbabilityTemplate.add(aggregator);
    m_Probability.add(aggregator);
}

void CProbabilityAndInfluenceCalculator::add(const CProbabilityAndInfluenceCalculator &other, double weight)
{
    double p = 0.0;
    if (!other.m_Probability.calculate(p))
    {
        return;
    }
    if (!other.m_Probability.empty())
    {
        m_Probability.add(p, weight);
    }

    for (CModelTools::TStrPtrStrPtrPrProbabilityAggregatorUMapCItr i = other.m_InfluencerProbabilities.begin();
         i != other.m_InfluencerProbabilities.end();
         ++i)
    {
        if (!i->second.calculate(p))
        {
            continue;
        }

        CModelTools::CProbabilityAggregator &probability =
                m_InfluencerProbabilities.emplace(i->first, other.m_ProbabilityTemplate).first->second;
        if (!i->second.empty())
        {
            probability.add(p, weight);
        }
    }
}

bool CProbabilityAndInfluenceCalculator::addAttributeProbability(const TStrPtr &attribute,
                                                                 std::size_t cid,
                                                                 double pAttribute,
                                                                 SParams &params,
                                                                 CAnnotatedProbabilityBuilder &builder,
                                                                 double weight)
{
    if (this->addProbability(params.s_Feature,
                             *params.s_Prior,
                             params.s_ElapsedTime,
                             params.s_WeightStyles,
                             params.s_Sample,
                             params.s_Weights,
                             params.s_BucketEmpty,
                             params.s_ProbabilityBucketEmpty,
                             params.s_Probability,
                             params.s_Tail, weight))
    {
        static const TStrPtr1Vec NO_CORRELATED_ATTRIBUTES;
        static const TSizeDoublePr1Vec NO_CORRELATES;
        builder.addAttributeProbability(cid, attribute,
                                        pAttribute, params.s_Probability,
                                        model_t::CResultType::E_Unconditional,
                                        params.s_Feature,
                                        NO_CORRELATED_ATTRIBUTES, NO_CORRELATES);
        return true;
    }
    return false;
}

bool CProbabilityAndInfluenceCalculator::addAttributeProbability(const TStrPtr &attribute,
                                                                 std::size_t cid,
                                                                 double pAttribute,
                                                                 SCorrelateParams &params,
                                                                 CAnnotatedProbabilityBuilder &builder,
                                                                 double weight)
{
    params.s_MostAnomalousCorrelate.clear();
    model_t::CResultType type;
    if (this->addProbability(params.s_Feature,
                             params.s_Priors,
                             params.s_ElapsedTimes,
                             params.s_WeightStyles,
                             params.s_Samples,
                             params.s_Weights,
                             params.s_BucketEmpty,
                             params.s_ProbabilityBucketEmpty,
                             params.s_Probability,
                             params.s_Tail,
                             type,
                             params.s_MostAnomalousCorrelate,
                             weight))
    {
        TStrPtr1Vec correlatedLabels_;
        TSizeDoublePr1Vec correlated_;
        if (!params.s_MostAnomalousCorrelate.empty())
        {
            std::size_t i = params.s_MostAnomalousCorrelate[0];
            correlatedLabels_.push_back(params.s_CorrelatedLabels[i]);
            correlated_.push_back(std::make_pair(params.s_Correlated[i],
                                                 params.s_Values[i][params.s_Variables[i][1]]));
        }
        builder.addAttributeProbability(cid, attribute,
                                        pAttribute, params.s_Probability,
                                        type, params.s_Feature,
                                        correlatedLabels_, correlated_);
        return true;
    }
    return false;
}

bool CProbabilityAndInfluenceCalculator::addAttributeProbability(const TStrPtr &attribute,
                                                                 std::size_t cid,
                                                                 double pAttribute,
                                                                 SMultivariateParams &params,
                                                                 CAnnotatedProbabilityBuilder &builder,
                                                                 double weight)
{
    if (this->addProbability(params.s_Feature,
                             *params.s_Prior,
                             params.s_ElapsedTime,
                             params.s_WeightStyles,
                             params.s_Sample,
                             params.s_Weights,
                             params.s_BucketEmpty,
                             params.s_ProbabilityBucketEmpty,
                             params.s_Probability,
                             params.s_Tail, weight))
    {
        static const TStrPtr1Vec NO_CORRELATED_ATTRIBUTES;
        static const TSizeDoublePr1Vec NO_CORRELATES;
        builder.addAttributeProbability(cid, attribute,
                                        pAttribute, params.s_Probability,
                                        model_t::CResultType::E_Unconditional,
                                        params.s_Feature,
                                        NO_CORRELATED_ATTRIBUTES, NO_CORRELATES);
        return true;
    }
    return false;
}

bool CProbabilityAndInfluenceCalculator::addProbability(model_t::EFeature feature,
                                                        const maths::CPrior &prior,
                                                        core_t::TTime elapsedTime,
                                                        const maths_t::TWeightStyleVec &weightStyles,
                                                        const TDouble1Vec &value,
                                                        const TDouble4Vec1Vec &weights,
                                                        bool bucketEmpty,
                                                        double probabilityBucketEmpty,
                                                        double &probability,
                                                        maths_t::ETail &tail,
                                                        double weight)
{
    double lowerBound;
    double upperBound;
    maths_t::EProbabilityCalculation calculation = model_t::probabilityCalculation(feature);
    if (prior.probabilityOfLessLikelySamples(calculation,
                                             weightStyles, value, weights,
                                             lowerBound, upperBound, tail))
    {
        probability = correctForEmptyBucket(calculation, value[0],
                                            bucketEmpty,
                                            probabilityBucketEmpty,
                                            (lowerBound + upperBound) / 2.0);
        if (!model_t::isConstant(feature))
        {
            probability = model_t::adjustProbability(feature, elapsedTime, probability);
            m_Probability.add(probability, weight);
        }
        return true;
    }
    return false;
}

bool CProbabilityAndInfluenceCalculator::addProbability(model_t::EFeature feature,
                                                        const TMultivariatePriorCPtrSizePr1Vec &priors,
                                                        const TTime2Vec1Vec &elapsedTimes,
                                                        const maths_t::TWeightStyleVec &weightStyles,
                                                        const TDouble10Vec1Vec &values,
                                                        const TDouble10Vec4Vec1Vec &weights,
                                                        const TBool2Vec &bucketEmpty,
                                                        const TDouble2Vec &probabilityBucketEmpty,
                                                        double &probability,
                                                        maths_t::ETail &tail,
                                                        model_t::CResultType &type,
                                                        TSize1Vec &mostAnomalousCorrelate,
                                                        double weight)
{
    std::size_t i;
    if (computeProbability(model_t::probabilityCalculation(feature),
                           priors, weightStyles, values, weights,
                           bucketEmpty, probabilityBucketEmpty,
                           probability, tail, type, i))
    {
        if (!model_t::isConstant(feature))
        {
            probability = model_t::adjustProbability(feature,
                                                     *std::min(elapsedTimes[i].begin(),
                                                               elapsedTimes[i].end()),
                                                     probability);
            mostAnomalousCorrelate.push_back(i);
            m_Probability.add(probability, weight);
        }
        return true;
    }
    return false;
}

bool CProbabilityAndInfluenceCalculator::addProbability(model_t::EFeature feature,
                                                        const maths::CMultivariatePrior &prior,
                                                        core_t::TTime elapsedTime,
                                                        const maths_t::TWeightStyleVec &weightStyles,
                                                        const TDouble10Vec1Vec &value,
                                                        const TDouble10Vec4Vec1Vec &weights,
                                                        bool bucketEmpty,
                                                        double probabilityBucketEmpty,
                                                        double &probability,
                                                        TTail10Vec &tail,
                                                        double weight)
{
    double lowerBound;
    double upperBound;
    maths_t::EProbabilityCalculation calculation = model_t::probabilityCalculation(feature);
    if (prior.probabilityOfLessLikelySamples(calculation,
                                             weightStyles, value, weights,
                                             lowerBound, upperBound, tail))
    {
        probability = correctForEmptyBucket(calculation, value[0],
                                            bucketEmpty,
                                            probabilityBucketEmpty,
                                            (lowerBound + upperBound) / 2.0);
        if (!model_t::isConstant(feature))
        {
            probability = model_t::adjustProbability(feature, elapsedTime, probability);
            m_Probability.add(probability, weight);
        }
        return true;
    }
    return false;
}

void CProbabilityAndInfluenceCalculator::addProbability(double probability, double weight)
{
    m_Probability.add(probability, weight);
    for (CModelTools::TStrPtrStrPtrPrProbabilityAggregatorUMapItr i = m_InfluencerProbabilities.begin();
         i != m_InfluencerProbabilities.end();
         ++i)
    {
        i->second.add(probability, weight);
    }
}

void CProbabilityAndInfluenceCalculator::addInfluences(const std::string &influencerName,
                                                       const TStrCRefDouble1VecDoublePrPrVec &influencerValues,
                                                       SParams &params,
                                                       double weight)
{
    if (!m_InfluenceCalculator)
    {
        LOG_ERROR("No influence calculator plug-in: can't compute influence");
        return;
    }

    const std::string *influencerValue = 0;
    if (influencerValues.empty())
    {
        for (std::size_t i = 0u; i < params.s_PartitioningFields.size(); ++i)
        {
            if (params.s_PartitioningFields[i].first.get() == influencerName)
            {
                influencerValue = params.s_PartitioningFields[i].second.get_pointer();
                break;
            }
        }
        if (!influencerValue)
        {
            return;
        }
    }

    double logp = ::log(std::max(params.s_Probability, maths::CTools::smallestProbability()));

    params.s_InfluencerName   = canonical(influencerName);
    params.s_InfluencerValues = influencerValues;
    params.s_Cutoff = 0.5 / std::max(-logp, 1.0);
    params.s_IncludeCutoff = true;

    m_InfluenceCalculator->computeInfluences(params);
    m_Influences.swap(params.s_Influences);
    if (m_Influences.empty() && influencerValue)
    {
        m_Influences.push_back(TStrPtrStrPtrPrDoublePr(
                                   TStrPtrStrPtrPr(params.s_InfluencerName,
                                                   canonical(*influencerValue)), 1.0));
    }
    this->commitInfluences(params.s_Feature, logp, weight);
}

void CProbabilityAndInfluenceCalculator::addInfluences(const std::string &influencerName,
                                                       const TStrCRefDouble1VecDouble1VecPrPrVecVec &influencerValues,
                                                       SCorrelateParams &params,
                                                       double weight)
{
    if (!m_InfluenceCalculator)
    {
        LOG_ERROR("No influence calculator plug-in: can't compute influence");
        return;
    }

    const std::string *influencerValue = 0;
    if (influencerValues.empty())
    {
        for (std::size_t i = 0u; i < params.s_PartitioningFields.size(); ++i)
        {
            if (params.s_PartitioningFields[i].first.get() == influencerName)
            {
                influencerValue = params.s_PartitioningFields[i].second.get_pointer();
                break;
            }
        }
        if (!influencerValue)
        {
            return;
        }
    }

    double logp = ::log(std::max(params.s_Probability, maths::CTools::smallestProbability()));

    std::size_t i = params.s_MostAnomalousCorrelate[0];
    params.s_InfluencerName   = canonical(influencerName);
    params.s_InfluencerValues = influencerValues[i];
    params.s_Cutoff = 0.5 / std::max(-logp, 1.0);
    params.s_IncludeCutoff = true;

    m_InfluenceCalculator->computeInfluences(params);
    m_Influences.swap(params.s_Influences);
    if (m_Influences.empty() && influencerValue)
    {
        m_Influences.push_back(TStrPtrStrPtrPrDoublePr(
                                   TStrPtrStrPtrPr(params.s_InfluencerName,
                                                   canonical(*influencerValue)), 1.0));
    }
    this->commitInfluences(params.s_Feature, logp, weight);
}

void CProbabilityAndInfluenceCalculator::addInfluences(const std::string &influencerName,
                                                       const TStrCRefDouble1VecDoublePrPrVec &influencerValues,
                                                       SMultivariateParams &params,
                                                       double weight)
{
    if (!m_InfluenceCalculator)
    {
        LOG_ERROR("No influence calculator plug-in: can't compute influence");
        return;
    }

    const std::string *influencerValue = 0;
    if (influencerValues.empty())
    {
        for (std::size_t i = 0u; i < params.s_PartitioningFields.size(); ++i)
        {
            if (params.s_PartitioningFields[i].first.get() == influencerName)
            {
                influencerValue = params.s_PartitioningFields[i].second.get_pointer();
                break;
            }
        }
        if (!influencerValue)
        {
            return;
        }
    }

    std::size_t d = params.s_Prior->dimension();
    if (params.s_Value.size() != d || (params.s_Trend.size() > 0 && params.s_Trend.size() != d))
    {
        LOG_ERROR("Inconsistent dimension");
        return;
    }

    double logp = ::log(std::max(params.s_Probability, maths::CTools::smallestProbability()));

    params.s_InfluencerName  = canonical(influencerName);
    params.s_InfluencerValues = influencerValues;
    params.s_Cutoff = 0.5 / std::max(-logp, 1.0);
    params.s_IncludeCutoff = true;

    m_InfluenceCalculator->computeInfluences(params);
    m_Influences.swap(params.s_Influences);
    if (m_Influences.empty() && influencerValue)
    {
        m_Influences.push_back(TStrPtrStrPtrPrDoublePr(
                                   TStrPtrStrPtrPr(params.s_InfluencerName,
                                                   canonical(*influencerValue)), 1.0));
    }
    this->commitInfluences(params.s_Feature, logp, weight);
}

bool CProbabilityAndInfluenceCalculator::calculate(double &probability) const
{
    return m_Probability.calculate(probability);
}

bool CProbabilityAndInfluenceCalculator::calculate(double &probability,
                                                   TStrPtrStrPtrPrDoublePrVec &influences) const
{
    if (!m_Probability.calculate(probability))
    {
        return false;
    }
    LOG_TRACE("probability = " << probability);

    double logp = ::log(probability);

    influences.reserve(m_InfluencerProbabilities.size());
    for (CModelTools::TStrPtrStrPtrPrProbabilityAggregatorUMapCItr itr = m_InfluencerProbabilities.begin();
         itr != m_InfluencerProbabilities.end();
         ++itr)
    {
        double pi;
        if (!itr->second.calculate(pi))
        {
            LOG_ERROR("Couldn't calculate probability for influencer "
                      << core::CContainerPrinter::print(itr->first));
        }
        LOG_TRACE("influence probability = " << pi);

        double influence = CInfluenceCalculator::intersectionInfluence(logp, ::log(pi));
        if (influence >= m_Cutoff)
        {
            influences.push_back(TStrPtrStrPtrPrDoublePr(itr->first, influence));
        }
    }
    std::sort(influences.begin(), influences.end(), maths::COrderings::SSecondGreater());

    return true;
}

void CProbabilityAndInfluenceCalculator::commitInfluences(model_t::EFeature feature,
                                                          double logp,
                                                          double weight)
{
    LOG_TRACE("influences = " << core::CContainerPrinter::print(m_Influences));

    if (!m_Influences.empty())
    {
        for (std::size_t i = 0u; i < m_Influences.size(); ++i)
        {
            CModelTools::CProbabilityAggregator &aggregator =
                    m_InfluencerProbabilities.emplace(m_Influences[i].first,
                                                      m_ProbabilityTemplate).first->second;
            if (!model_t::isConstant(feature))
            {
                double pi = ::exp(m_Influences[i].second * logp);
                LOG_TRACE("Adding = " << m_Influences[i].first.second.get() << " " << pi);
                aggregator.add(pi, weight);
            }
        }
    }
}

CProbabilityAndInfluenceCalculator::SParams::SParams(const maths_t::TWeightStyleVec &weightStyles,
                                                     const CPartitioningFields &partitioningFields) :
        s_Feature(),
        s_Trend(0),
        s_Prior(0),
        s_ElapsedTime(0),
        s_Time(0),
        s_Count(0.0),
        s_WeightStyles(weightStyles),
        s_BucketEmpty(false),
        s_ProbabilityBucketEmpty(0.0),
        s_Probability(1.0),
        s_Tail(maths_t::E_UndeterminedTail),
        s_Confidence(0.0),
        s_PartitioningFields(partitioningFields),
        s_Cutoff(1.0),
        s_IncludeCutoff(false)
{}

std::string CProbabilityAndInfluenceCalculator::SParams::describe(void) const
{
    return core::CContainerPrinter::print(s_Sample)
           + " | feature = " + model_t::print(s_Feature)
           + ", value = " + core::CContainerPrinter::print(s_Value)
           + ", weights = " + core::CContainerPrinter::print(s_Weights)
           + ", @ " + core::CStringUtils::typeToString(s_Time)
           + ", elapsedTime = " + core::CStringUtils::typeToString(s_ElapsedTime);
}

CProbabilityAndInfluenceCalculator::SCorrelateParams::SCorrelateParams(const maths_t::TWeightStyleVec &weightStyles,
                                                                       const CPartitioningFields &partitioningFields) :
        s_Feature(),
        s_WeightStyles(weightStyles),
        s_Probability(1.0),
        s_Tail(maths_t::E_UndeterminedTail),
        s_Confidence(0.0),
        s_PartitioningFields(partitioningFields),
        s_Cutoff(1.0),
        s_IncludeCutoff(false)
{}

void CProbabilityAndInfluenceCalculator::SCorrelateParams::clear(void)
{
    s_Trends.clear();
    s_Times.clear();
    s_ElapsedTimes.clear();
    s_Values.clear();
    s_Counts.clear();
    s_Samples.clear();
    s_Weights.clear();
    s_Variables.clear();
    s_CorrelatedLabels.clear();
    s_Correlated.clear();
    s_BucketEmpty.clear();
    s_ProbabilityBucketEmpty.clear();
}

std::string CProbabilityAndInfluenceCalculator::SCorrelateParams::describe(void) const
{
    return core::CContainerPrinter::print(s_Samples)
           + " | feature = " + model_t::print(s_Feature)
           + ", value = " + core::CContainerPrinter::print(s_Values)
           + ", weights = " + core::CContainerPrinter::print(s_Weights)
           + ", @ " + core::CContainerPrinter::print(s_Times)
           + ", elapsedTime = " + core::CContainerPrinter::print(s_ElapsedTimes);
}

CProbabilityAndInfluenceCalculator::SMultivariateParams::SMultivariateParams(const maths_t::TWeightStyleVec &weightStyles,
                                                                             const CPartitioningFields &partitioningFields) :
        s_Feature(),
        s_Prior(0),
        s_ElapsedTime(0),
        s_Time(0),
        s_Count(0.0),
        s_WeightStyles(weightStyles),
        s_BucketEmpty(false),
        s_ProbabilityBucketEmpty(0.0),
        s_Probability(1.0),
        s_Confidence(0.0),
        s_PartitioningFields(partitioningFields),
        s_Cutoff(1.0),
        s_IncludeCutoff(false)
{}

std::string CProbabilityAndInfluenceCalculator::SMultivariateParams::describe(void) const
{
    return core::CContainerPrinter::print(s_Sample)
           + " | feature = " + model_t::print(s_Feature)
           + ", value = " + core::CContainerPrinter::print(s_Value)
           + ", weights = " + core::CContainerPrinter::print(s_Weights)
           + ", @ " + core::CStringUtils::typeToString(s_Time)
           + ", elapsedTime = " + core::CStringUtils::typeToString(s_ElapsedTime);
}


////// CInfluenceCalculator //////

CInfluenceCalculator::~CInfluenceCalculator(void)
{
}

double CInfluenceCalculator::intersectionInfluence(double logp, double logpi)
{
    return maths::CTools::truncate(ratio(logpi, logp, 1.0), 0.0, 1.0);
}

double CInfluenceCalculator::complementInfluence(double logp, double logpi)
{
    return maths::CTools::truncate(1.0 - ratio(logpi, logp, 0.0), 0.0, 1.0);
}

////// CInfluenceUnavailableCalculator //////

void CInfluenceUnavailableCalculator::computeInfluences(TParams &params) const
{
    params.s_Influences.clear();
}

void CInfluenceUnavailableCalculator::computeInfluences(TCorrelateParams &params) const
{
    params.s_Influences.clear();
}

void CInfluenceUnavailableCalculator::computeInfluences(TMultivariateParams &params) const
{
    params.s_Influences.clear();
}

////// CIndicatorInfluenceCalculator //////

void CIndicatorInfluenceCalculator::computeInfluences(TParams &params) const
{
    params.s_Influences.clear();
    doComputeIndicatorInfluences(params.s_InfluencerName,
                                 params.s_InfluencerValues,
                                 params.s_Influences);
}

void CIndicatorInfluenceCalculator::computeInfluences(TCorrelateParams &params) const
{
    params.s_Influences.clear();
    doComputeIndicatorInfluences(params.s_InfluencerName,
                                 params.s_InfluencerValues,
                                 params.s_Influences);
}

void CIndicatorInfluenceCalculator::computeInfluences(TMultivariateParams &params) const
{
    params.s_Influences.clear();
    doComputeIndicatorInfluences(params.s_InfluencerName,
                                 params.s_InfluencerValues,
                                 params.s_Influences);
}

////// CLogProbabilityComplementInfluenceCalculator //////

#define SETUP_CORRELATE_INFLUENCES_ARGUMENTS                                       \
        std::size_t i = params.s_MostAnomalousCorrelate[0];                        \
        TSize10Vec coordinate(1, params.s_Priors[i].second);                       \
        double neff = effectiveCount(params.s_Values.size());                      \
        maths::CProbabilityOfExtremeSample probabilityCalculator;                  \
        for (std::size_t j = 1u; j < params.s_Values.size(); ++j)                  \
        {                                                                          \
            probabilityCalculator.add(1.0, neff);                                  \
        }                                                                          \
        TBool2Vec bucketEmpty(2, params.s_BucketEmpty[0]);                         \
        bucketEmpty[1] = params.s_BucketEmpty[i+1];                                \
        TDouble2Vec probabilityBucketEmpty(2, params.s_ProbabilityBucketEmpty[0]); \
        probabilityBucketEmpty[1] = params.s_ProbabilityBucketEmpty[i+1]

void CLogProbabilityComplementInfluenceCalculator::computeInfluences(TParams &params) const
{
    params.s_Influences.clear();

    TStrCRefDouble1VecDoublePrPrVec &influencerValues = params.s_InfluencerValues;
    if (params.s_Tail == maths_t::E_RightTail)
    {
        std::sort(influencerValues.begin(), influencerValues.end(),
                  CDecreasingValueInfluence(maths_t::E_RightTail));
        LOG_TRACE("influencerValues = " << core::CContainerPrinter::print(influencerValues));
        doComputeInfluences(params.s_Feature,
                            CValueDifference(params.s_Trend, params.s_Time, params.s_Confidence),
                            complementInfluence,
                            *params.s_Prior, params.s_ElapsedTime,
                            maths_t::E_OneSidedAbove,
                            params.s_Value, params.s_Count,
                            params.s_WeightStyles, params.s_Weights,
                            params.s_ProbabilityBucketEmpty, params.s_Probability,
                            params.s_InfluencerName, influencerValues,
                            params.s_Cutoff, params.s_IncludeCutoff, params.s_Influences);
    }
}

void CLogProbabilityComplementInfluenceCalculator::computeInfluences(TCorrelateParams &params) const
{
    params.s_Influences.clear();

    if (params.s_Tail == maths_t::E_RightTail)
    {
        SETUP_CORRELATE_INFLUENCES_ARGUMENTS;
        LOG_TRACE("influencerValues = " << core::CContainerPrinter::print(params.s_InfluencerValues));
        doComputeCorrelateInfluences(params.s_Feature,
                                     CValueDifference(params.s_Trends[i], params.s_Times[i], params.s_Confidence),
                                     complementInfluence,
                                     *params.s_Priors[i].first, params.s_ElapsedTimes[i],
                                     coordinate, maths_t::E_OneSidedAbove,
                                     params.s_Values[i], params.s_Counts[i],
                                     params.s_WeightStyles, TDouble10Vec4Vec1Vec(1, params.s_Weights[i]),
                                     bucketEmpty, probabilityBucketEmpty,
                                     params.s_Probability, probabilityCalculator, neff,
                                     params.s_InfluencerName, params.s_InfluencerValues,
                                     params.s_Cutoff, params.s_IncludeCutoff, params.s_Influences);
    }
}

void CLogProbabilityComplementInfluenceCalculator::computeInfluences(TMultivariateParams &params) const
{
    params.s_Influences.clear();

    TSize10Vec coordinates;
    TProbabilityCalculation10Vec calculations;
    for (std::size_t i = 0u; i < params.s_Tail.size(); ++i)
    {
        if (params.s_Tail[i] == maths_t::E_RightTail)
        {
            coordinates.push_back(i);
            calculations.push_back(maths_t::E_OneSidedAbove);
        }
    }

    if (coordinates.empty())
    {
        return;
    }

    LOG_TRACE("influencerValues = " << core::CContainerPrinter::print(params.s_InfluencerValues));
    doComputeMultivariateInfluences(params.s_Feature,
                                    CValueDifference(params.s_Trend, params.s_Time, params.s_Confidence),
                                    complementInfluence,
                                    *params.s_Prior, params.s_ElapsedTime,
                                    coordinates, calculations,
                                    params.s_Value, params.s_Count,
                                    params.s_WeightStyles, params.s_Weights,
                                    params.s_ProbabilityBucketEmpty, params.s_Probability,
                                    params.s_InfluencerName, params.s_InfluencerValues,
                                    params.s_Cutoff, params.s_IncludeCutoff, params.s_Influences);
}

////// CLogProbabilityInfluenceCalculator //////

void CLogProbabilityInfluenceCalculator::computeInfluences(TParams &params) const
{
    params.s_Influences.clear();

    TStrCRefDouble1VecDoublePrPrVec &influencerValues = params.s_InfluencerValues;
    switch (params.s_Tail)
    {
    case maths_t::E_LeftTail:
        std::sort(influencerValues.begin(), influencerValues.end(),
                  CDecreasingValueInfluence(maths_t::E_LeftTail));
        LOG_TRACE("influencerValues = " << core::CContainerPrinter::print(influencerValues));
        doComputeInfluences(params.s_Feature,
                            CValueIntersection(params.s_Trend, params.s_Time, params.s_Confidence),
                            intersectionInfluence,
                            *params.s_Prior, params.s_ElapsedTime,
                            maths_t::E_OneSidedBelow,
                            params.s_Value, params.s_Count,
                            params.s_WeightStyles, params.s_Weights,
                            params.s_ProbabilityBucketEmpty, params.s_Probability,
                            params.s_InfluencerName, influencerValues,
                            params.s_Cutoff, params.s_IncludeCutoff, params.s_Influences);
        break;
    case maths_t::E_RightTail:
        std::sort(influencerValues.begin(), influencerValues.end(),
                  CDecreasingValueInfluence(maths_t::E_RightTail));
        LOG_TRACE("influencerValues = " << core::CContainerPrinter::print(influencerValues));
        doComputeInfluences(params.s_Feature,
                            CValueIntersection(params.s_Trend, params.s_Time, params.s_Confidence),
                            intersectionInfluence,
                            *params.s_Prior, params.s_ElapsedTime,
                            maths_t::E_OneSidedAbove,
                            params.s_Value, params.s_Count,
                            params.s_WeightStyles, params.s_Weights,
                            params.s_ProbabilityBucketEmpty, params.s_Probability,
                            params.s_InfluencerName, influencerValues,
                            params.s_Cutoff, params.s_IncludeCutoff, params.s_Influences);
        break;
    case maths_t::E_MixedOrNeitherTail:
    case maths_t::E_UndeterminedTail:
        break;
    }
}

void CLogProbabilityInfluenceCalculator::computeInfluences(TCorrelateParams &params) const
{
    params.s_Influences.clear();

    switch (params.s_Tail)
    {
    case maths_t::E_LeftTail:
    {
        SETUP_CORRELATE_INFLUENCES_ARGUMENTS;
        LOG_TRACE("influencerValues = " << core::CContainerPrinter::print(params.s_InfluencerValues));
        doComputeCorrelateInfluences(params.s_Feature,
                                     CValueIntersection(params.s_Trends[i], params.s_Times[i], params.s_Confidence),
                                     intersectionInfluence,
                                     *params.s_Priors[i].first, params.s_ElapsedTimes[i],
                                     coordinate, maths_t::E_OneSidedBelow,
                                     params.s_Values[i], params.s_Counts[i],
                                     params.s_WeightStyles, params.s_Weights,
                                     bucketEmpty, probabilityBucketEmpty,
                                     params.s_Probability, probabilityCalculator, neff,
                                     params.s_InfluencerName, params.s_InfluencerValues,
                                     params.s_Cutoff, params.s_IncludeCutoff, params.s_Influences);
        break;
    }
    case maths_t::E_RightTail:
    {
        SETUP_CORRELATE_INFLUENCES_ARGUMENTS;
        LOG_TRACE("influencerValues = " << core::CContainerPrinter::print(params.s_InfluencerValues));
        doComputeCorrelateInfluences(params.s_Feature,
                                     CValueIntersection(params.s_Trends[i], params.s_Times[i], params.s_Confidence),
                                     intersectionInfluence,
                                     *params.s_Priors[i].first, params.s_ElapsedTimes[i],
                                     coordinate, maths_t::E_OneSidedAbove,
                                     params.s_Values[i], params.s_Counts[i],
                                     params.s_WeightStyles, params.s_Weights,
                                     bucketEmpty, probabilityBucketEmpty,
                                     params.s_Probability, probabilityCalculator, neff,
                                     params.s_InfluencerName, params.s_InfluencerValues,
                                     params.s_Cutoff, params.s_IncludeCutoff, params.s_Influences);
        break;
    }
    case maths_t::E_MixedOrNeitherTail:
    case maths_t::E_UndeterminedTail:
        break;
    }
}

void CLogProbabilityInfluenceCalculator::computeInfluences(TMultivariateParams &params) const
{
    params.s_Influences.clear();

    TSize10Vec coordinates;
    TProbabilityCalculation10Vec calculations;
    for (std::size_t i = 0u; i < params.s_Tail.size(); ++i)
    {
        switch (params.s_Tail[i])
        {
        case maths_t::E_LeftTail:
            coordinates.push_back(i);
            calculations.push_back(maths_t::E_OneSidedBelow);
            break;
        case maths_t::E_RightTail:
            coordinates.push_back(i);
            calculations.push_back(maths_t::E_OneSidedAbove);
            break;
        case maths_t::E_MixedOrNeitherTail:
        case maths_t::E_UndeterminedTail:
            break;
        }
    }

    if (coordinates.empty())
    {
        return;
    }

    doComputeMultivariateInfluences(params.s_Feature,
                                    CValueIntersection(params.s_Trend, params.s_Time, params.s_Confidence),
                                    intersectionInfluence,
                                    *params.s_Prior, params.s_ElapsedTime,
                                    coordinates, calculations,
                                    params.s_Value, params.s_Count,
                                    params.s_WeightStyles, params.s_Weights,
                                    params.s_ProbabilityBucketEmpty, params.s_Probability,
                                    params.s_InfluencerName, params.s_InfluencerValues,
                                    params.s_Cutoff, params.s_IncludeCutoff, params.s_Influences);
}

////// CMeanInfluenceCalculator //////

void CMeanInfluenceCalculator::computeInfluences(TParams &params) const
{
    params.s_Influences.clear();

    TStrCRefDouble1VecDoublePrPrVec &influencerValues = params.s_InfluencerValues;
    switch (params.s_Tail)
    {
    case maths_t::E_LeftTail:
        std::sort(influencerValues.begin(), influencerValues.end(),
                  CDecreasingMeanInfluence(maths_t::E_LeftTail, params.s_Value, params.s_Count));
        LOG_TRACE("influencerValues = " << core::CContainerPrinter::print(influencerValues));
        doComputeInfluences(params.s_Feature,
                            CMeanDifference(params.s_Trend, params.s_Time, params.s_Confidence),
                            complementInfluence,
                            *params.s_Prior, params.s_ElapsedTime,
                            maths_t::E_OneSidedBelow,
                            params.s_Value, params.s_Count,
                            params.s_WeightStyles, params.s_Weights,
                            params.s_ProbabilityBucketEmpty, params.s_Probability,
                            params.s_InfluencerName, influencerValues,
                            params.s_Cutoff, params.s_IncludeCutoff, params.s_Influences);
        break;
    case maths_t::E_RightTail:
        std::sort(influencerValues.begin(), influencerValues.end(),
                  CDecreasingMeanInfluence(maths_t::E_RightTail, params.s_Value, params.s_Count));
        LOG_TRACE("influencerValues = " << core::CContainerPrinter::print(influencerValues));
        doComputeInfluences(params.s_Feature,
                            CMeanDifference(params.s_Trend, params.s_Time, params.s_Confidence),
                            complementInfluence,
                            *params.s_Prior, params.s_ElapsedTime,
                            maths_t::E_OneSidedAbove,
                            params.s_Value, params.s_Count,
                            params.s_WeightStyles, params.s_Weights,
                            params.s_ProbabilityBucketEmpty, params.s_Probability,
                            params.s_InfluencerName, influencerValues,
                            params.s_Cutoff, params.s_IncludeCutoff, params.s_Influences);
        break;
    case maths_t::E_MixedOrNeitherTail:
    case maths_t::E_UndeterminedTail:
        break;
    }
}

void CMeanInfluenceCalculator::computeInfluences(TCorrelateParams &params) const
{
    params.s_Influences.clear();

    switch (params.s_Tail)
    {
    case maths_t::E_LeftTail:
    {
        SETUP_CORRELATE_INFLUENCES_ARGUMENTS;
        LOG_TRACE("influencerValues = " << core::CContainerPrinter::print(params.s_InfluencerValues));
        doComputeCorrelateInfluences(params.s_Feature,
                                     CMeanDifference(params.s_Trends[i], params.s_Times[i], params.s_Confidence),
                                     complementInfluence,
                                     *params.s_Priors[i].first, params.s_ElapsedTimes[i],
                                     coordinate, maths_t::E_OneSidedBelow,
                                     params.s_Values[i], params.s_Counts[i],
                                     params.s_WeightStyles, params.s_Weights,
                                     bucketEmpty, probabilityBucketEmpty,
                                     params.s_Probability, probabilityCalculator, neff,
                                     params.s_InfluencerName, params.s_InfluencerValues,
                                     params.s_Cutoff, params.s_IncludeCutoff, params.s_Influences);
        break;
    }
    case maths_t::E_RightTail:
    {
        SETUP_CORRELATE_INFLUENCES_ARGUMENTS;
        LOG_TRACE("influencerValues = " << core::CContainerPrinter::print(params.s_InfluencerValues));
        doComputeCorrelateInfluences(params.s_Feature,
                                     CMeanDifference(params.s_Trends[i], params.s_Times[i], params.s_Confidence),
                                     complementInfluence,
                                     *params.s_Priors[i].first, params.s_ElapsedTimes[i],
                                     coordinate, maths_t::E_OneSidedAbove,
                                     params.s_Values[i], params.s_Counts[i],
                                     params.s_WeightStyles, params.s_Weights,
                                     bucketEmpty, probabilityBucketEmpty,
                                     params.s_Probability, probabilityCalculator, neff,
                                     params.s_InfluencerName, params.s_InfluencerValues,
                                     params.s_Cutoff, params.s_IncludeCutoff, params.s_Influences);
        break;
    }
    case maths_t::E_MixedOrNeitherTail:
    case maths_t::E_UndeterminedTail:
        break;
    }
}

void CMeanInfluenceCalculator::computeInfluences(TMultivariateParams &params) const
{
    params.s_Influences.clear();

    TSize10Vec coordinates;
    TProbabilityCalculation10Vec calculations;
    for (std::size_t i = 0u; i < params.s_Tail.size(); ++i)
    {
        switch (params.s_Tail[i])
        {
        case maths_t::E_LeftTail:
            coordinates.push_back(i);
            calculations.push_back(maths_t::E_OneSidedBelow);
            break;
        case maths_t::E_RightTail:
            coordinates.push_back(i);
            calculations.push_back(maths_t::E_OneSidedAbove);
            break;
        case maths_t::E_MixedOrNeitherTail:
        case maths_t::E_UndeterminedTail:
            break;
        }
    }

    if (coordinates.empty())
    {
        return;
    }

    LOG_TRACE("influencerValues = " << core::CContainerPrinter::print(params.s_InfluencerValues));
    doComputeMultivariateInfluences(params.s_Feature,
                                    CMeanDifference(params.s_Trend, params.s_Time, params.s_Confidence),
                                    complementInfluence,
                                    *params.s_Prior, params.s_ElapsedTime,
                                    coordinates, calculations,
                                    params.s_Value, params.s_Count,
                                    params.s_WeightStyles, params.s_Weights,
                                    params.s_ProbabilityBucketEmpty, params.s_Probability,
                                    params.s_InfluencerName, params.s_InfluencerValues,
                                    params.s_Cutoff, params.s_IncludeCutoff, params.s_Influences);
}

////// CVarianceInfluenceCalculator //////

void CVarianceInfluenceCalculator::computeInfluences(TParams &params) const
{
    params.s_Influences.clear();

    TStrCRefDouble1VecDoublePrPrVec &influencerValues = params.s_InfluencerValues;
    switch (params.s_Tail)
    {
    case maths_t::E_LeftTail:
        std::sort(influencerValues.begin(), influencerValues.end(),
                  CDecreasingVarianceInfluence(maths_t::E_LeftTail, params.s_Value, params.s_Count));
        LOG_TRACE("influencerValues = " << core::CContainerPrinter::print(influencerValues));
        doComputeInfluences(params.s_Feature,
                            CVarianceDifference(params.s_Trend, params.s_Time, params.s_Confidence),
                            complementInfluence,
                            *params.s_Prior, params.s_ElapsedTime,
                            maths_t::E_OneSidedBelow,
                            params.s_Value, params.s_Count,
                            params.s_WeightStyles, params.s_Weights,
                            params.s_ProbabilityBucketEmpty, params.s_Probability,
                            params.s_InfluencerName, influencerValues,
                            params.s_Cutoff, params.s_IncludeCutoff, params.s_Influences);
        break;
    case maths_t::E_RightTail:
        std::sort(influencerValues.begin(), influencerValues.end(),
                  CDecreasingVarianceInfluence(maths_t::E_RightTail, params.s_Value, params.s_Count));
        LOG_TRACE("influencerValues = " << core::CContainerPrinter::print(influencerValues));
        doComputeInfluences(params.s_Feature,
                            CVarianceDifference(params.s_Trend, params.s_Time, params.s_Confidence),
                            complementInfluence,
                            *params.s_Prior, params.s_ElapsedTime,
                            maths_t::E_OneSidedAbove,
                            params.s_Value, params.s_Count,
                            params.s_WeightStyles, params.s_Weights,
                            params.s_ProbabilityBucketEmpty, params.s_Probability,
                            params.s_InfluencerName, influencerValues,
                            params.s_Cutoff, params.s_IncludeCutoff, params.s_Influences);
        break;
    case maths_t::E_MixedOrNeitherTail:
    case maths_t::E_UndeterminedTail:
        break;
    }
}

void CVarianceInfluenceCalculator::computeInfluences(TCorrelateParams &params) const
{
    params.s_Influences.clear();

    switch (params.s_Tail)
    {
    case maths_t::E_LeftTail:
    {
        SETUP_CORRELATE_INFLUENCES_ARGUMENTS;
        LOG_TRACE("influencerValues = " << core::CContainerPrinter::print(params.s_InfluencerValues));
        doComputeCorrelateInfluences(params.s_Feature,
                                     CVarianceDifference(params.s_Trends[i], params.s_Times[i], params.s_Confidence),
                                     complementInfluence,
                                     *params.s_Priors[i].first, params.s_ElapsedTimes[i],
                                     coordinate, maths_t::E_OneSidedBelow,
                                     params.s_Values[i], params.s_Counts[i],
                                     params.s_WeightStyles, params.s_Weights,
                                     bucketEmpty, probabilityBucketEmpty,
                                     params.s_Probability, probabilityCalculator, neff,
                                     params.s_InfluencerName, params.s_InfluencerValues,
                                     params.s_Cutoff, params.s_IncludeCutoff, params.s_Influences);
        break;
    }
    case maths_t::E_RightTail:
    {
        SETUP_CORRELATE_INFLUENCES_ARGUMENTS;
        LOG_TRACE("influencerValues = " << core::CContainerPrinter::print(params.s_InfluencerValues));
        doComputeCorrelateInfluences(params.s_Feature,
                                     CVarianceDifference(params.s_Trends[i], params.s_Times[i], params.s_Confidence),
                                     complementInfluence,
                                     *params.s_Priors[i].first, params.s_ElapsedTimes[i],
                                     coordinate, maths_t::E_OneSidedAbove,
                                     params.s_Values[i], params.s_Counts[i],
                                     params.s_WeightStyles, params.s_Weights,
                                     bucketEmpty, probabilityBucketEmpty,
                                     params.s_Probability, probabilityCalculator, neff,
                                     params.s_InfluencerName, params.s_InfluencerValues,
                                     params.s_Cutoff, params.s_IncludeCutoff, params.s_Influences);
        break;
    }
    case maths_t::E_MixedOrNeitherTail:
    case maths_t::E_UndeterminedTail:
        break;
    }
}

void CVarianceInfluenceCalculator::computeInfluences(TMultivariateParams &params) const
{
    params.s_Influences.clear();

    TSize10Vec coordinates;
    TProbabilityCalculation10Vec calculations;
    for (std::size_t i = 0u; i < params.s_Tail.size(); ++i)
    {
        switch (params.s_Tail[i])
        {
        case maths_t::E_LeftTail:
            coordinates.push_back(i);
            calculations.push_back(maths_t::E_OneSidedBelow);
            break;
        case maths_t::E_RightTail:
            coordinates.push_back(i);
            calculations.push_back(maths_t::E_OneSidedAbove);
            break;
        case maths_t::E_MixedOrNeitherTail:
        case maths_t::E_UndeterminedTail:
            break;
        }
    }

    if (coordinates.empty())
    {
        return;
    }

    LOG_TRACE("influencerValues = " << core::CContainerPrinter::print(params.s_InfluencerValues));
    doComputeMultivariateInfluences(params.s_Feature,
                                    CMeanDifference(params.s_Trend, params.s_Time, params.s_Confidence),
                                    complementInfluence,
                                    *params.s_Prior, params.s_ElapsedTime,
                                    coordinates, calculations,
                                    params.s_Value, params.s_Count,
                                    params.s_WeightStyles, params.s_Weights,
                                    params.s_ProbabilityBucketEmpty, params.s_Probability,
                                    params.s_InfluencerName, params.s_InfluencerValues,
                                    params.s_Cutoff, params.s_IncludeCutoff, params.s_Influences);
}

#undef SETUP_CORRELATE_INFLUENCES_ARGUMENTS

}
}
