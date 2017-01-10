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

#ifndef INCLUDED_ml_maths_CForecast_h
#define INCLUDED_ml_maths_CForecast_h

#include <core/CoreTypes.h>

#include <maths/ImportExport.h>

#include <vector>

namespace ml
{
namespace maths
{
class CPrior;
class CTimeSeriesDecompositionInterface;

//! \brief Data describing a prediction error bar.
struct MATHS_EXPORT SErrorBar
{
    core_t::TTime s_Time;
    double s_LowerBound;
    double s_Predicted;
    double s_UpperBound;
};

typedef std::vector<SErrorBar> TErrorBarVec;

//! Forecast the time series modelled by \p trend and \p prior
//! over the time range [\p startTime, \p endTime).
//!
//! \param[in] trend Optional object describing the trend in the
//! the time series values.
//! \param[in] prior The object describing the distribution of
//! the residual around \p trend.
//! \param[in] startTime The start of the interval to forecast.
//! \param[in] endTime The end of the interval to forecast.
//! \param[in] bucketLength The bucketing interval.
//! \param[out] result Filled in with the predicted error bars
//! for the time series.
MATHS_EXPORT
void forecast(const CTimeSeriesDecompositionInterface *trend,
              const CPrior &prior,
              core_t::TTime startTime,
              core_t::TTime endTime,
              core_t::TTime bucketLength,
              double confidence,
              double decayRate,
              TErrorBarVec &result);

}
}

#endif // INCLUDED_ml_maths_CForecast_h
