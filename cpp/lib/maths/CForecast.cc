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

#include <maths/CForecast.h>

#include <maths/CBasicStatistics.h>
#include <maths/CPrior.h>
#include <maths/CTimeSeriesDecomposition.h>

#include <utility>

namespace ml
{
namespace maths
{

void forecast(const CTimeSeriesDecompositionInterface *trend,
              const CPrior &prior,
              core_t::TTime startTime,
              core_t::TTime endTime,
              core_t::TTime bucketLength,
              double confidence,
              double decayRate,
              TErrorBarVec &result)
{
    typedef std::pair<double, double> TDoubleDoublePr;
    typedef core::CSmallVector<double, 4> TDouble4Vec;
    typedef boost::scoped_ptr<CTimeSeriesDecompositionInterface> TDecompositionPtr;
    typedef boost::scoped_ptr<CPrior> TPriorPtr;

    result.clear();

    if (endTime <= startTime)
    {
        return;
    }

    result.reserve((endTime - startTime) / bucketLength + 1);

    TPriorPtr forecastPrior(prior.clone());
    forecastPrior->decayRate(decayRate);

    if (trend && trend->initialized())
    {
        TDecompositionPtr forecastTrend(trend->clone());
        forecastTrend->decayRate(0.2 * decayRate);
        forecastTrend->forecast();

        TDouble4Vec weight(1, 1.0);
        for (core_t::TTime time = startTime; time < endTime; time += bucketLength)
        {
            forecastTrend->testAndInterpolate(time);
            forecastTrend->propagateForwardsTo(time);
            forecastPrior->propagateForwardsByTime(1.0);
            double variance = forecastPrior->marginalLikelihoodVariance();

            TDoubleDoublePr baseline = forecastTrend->baseline(time, confidence);
            weight[0] = CBasicStatistics::mean(forecastTrend->scale(time, variance, 0.0));
            TDoubleDoublePr interval = forecastPrior->marginalLikelihoodConfidenceInterval(
                                           confidence,
                                           maths::CConstantWeights::SEASONAL_VARIANCE,
                                           weight);
            double mean = forecastPrior->marginalLikelihoodMean();

            SErrorBar errorBar;
            errorBar.s_Time       = time;
            errorBar.s_LowerBound = baseline.first  + (interval.first - mean);
            errorBar.s_Predicted  = CBasicStatistics::mean(baseline);
            errorBar.s_UpperBound = baseline.second + (interval.second - mean);
            result.push_back(errorBar);
        }
    }
    else
    {
        for (core_t::TTime time = startTime; time < endTime; time += bucketLength)
        {
            double mean = forecastPrior->marginalLikelihoodMean();
            TDoubleDoublePr interval = forecastPrior->marginalLikelihoodConfidenceInterval(confidence);

            SErrorBar errorBar;
            errorBar.s_Time       = time;
            errorBar.s_LowerBound = interval.first;
            errorBar.s_Predicted  = mean;
            errorBar.s_UpperBound = interval.second;
            result.push_back(errorBar);

            forecastPrior->propagateForwardsByTime(1.0);
        }
    }
}

}
}
