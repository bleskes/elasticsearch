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

#include <model/CDecayRateController.h>

#include <core/CContainerPrinter.h>
#include <core/CLogger.h>
#include <core/CFunctional.h>
#include <core/CPersistUtils.h>
#include <core/RestoreMacros.h>

#include <maths/CChecksum.h>
#include <maths/CTools.h>

#include <model/CModelConfig.h>

#include <boost/range.hpp>

#include <algorithm>
#include <math.h>

namespace ml
{
namespace model
{
namespace
{

const std::string MULTIPLIER_TAG("a");
const std::string PREDICTION_MEAN_TAG("b");
const std::string BIAS_TAG("c");
const std::string SHORT_TERM_ABS_RESIDUAL_TAG("d");
const std::string LONG_TERM_ABS_RESIDUAL_TAG("e");

//! The factor by which we'll increase the decay rate per bucket.
const double INCREASE_RATE = 1.2;
//! The factor by which we'll decrease the decay rate per bucket.
const double DECREASE_RATE = 1.0 / INCREASE_RATE;
//! The minimum ratio between the prediction bias and error which
//! causes us to increase decay rate.
const double LARGE_BIAS = 0.5;
//! The maximum ratio between the prediction bias and error which
//! causes us to decrease decay rate if permitted by the short to
//! long term error ratio test.
const double SMALL_BIAS = 0.3;
//! The maximum ratio between the prediction short and long term
//! errors which causes us to increase decay rate.
const double LARGE_RATIO = 2.0;
//! The minimum ratio between the prediction short and long term
//! errors which causes us to increase decay rate if permitted by
//! the bias to error ratio test.
const double SMALL_RATIO = 1.2;
//! The minimum number of prediction residuals we need to see before
//! we'll attempt to control the decay rate.
const double MINIMUM_COUNT_TO_CONTROL = 772.0;
//! The minimum coefficient of variation for the prediction error
//! at which we'll bother to control decay rate.
const double MINIMUM_COV_TO_CONTROL = 1e-4;
//! The minimum decay rate multiplier permitted.
const double MINIMUM_MULTIPLIER = 0.2;
//! The maximum decay rate multiplier permitted.
const double MAXIMUM_MULTIPLIER = 10.0;

//! Compute the effective decay rate adjusted for \p bucketLength.
double effectiveRate(double rate, core_t::TTime bucketLength)
{
    return ::pow(rate, std::min(  static_cast<double>(bucketLength)
                                / static_cast<double>(CModelConfig::STANDARD_BUCKET_LENGTH), 1.0));
}

}

CDecayRateController::CDecayRateController(void) : m_Multiplier(1.0) {}
CDecayRateController::CDecayRateController(std::size_t dimension) :
        m_Multiplier(1.0),
        m_PredictionMean(dimension),
        m_Bias(dimension),
        m_ShortTermAbsResidual(dimension),
        m_LongTermAbsResidual(dimension)
{}

void CDecayRateController::reset(void)
{
    m_Multiplier           = 1.0;
    m_PredictionMean       = TMeanAccumulator1Vec(m_PredictionMean.size());
    m_Bias                 = TMeanAccumulator1Vec(m_Bias.size());
    m_ShortTermAbsResidual = TMeanAccumulator1Vec(m_ShortTermAbsResidual.size());
    m_LongTermAbsResidual  = TMeanAccumulator1Vec(m_LongTermAbsResidual.size());
}

bool CDecayRateController::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        RESTORE_BUILT_IN(MULTIPLIER_TAG, m_Multiplier)
        RESTORE(PREDICTION_MEAN_TAG, core::CPersistUtils::restore(PREDICTION_MEAN_TAG, m_PredictionMean, traverser));
        RESTORE(BIAS_TAG, core::CPersistUtils::restore(BIAS_TAG, m_Bias, traverser))
        RESTORE(SHORT_TERM_ABS_RESIDUAL_TAG,
                core::CPersistUtils::restore(SHORT_TERM_ABS_RESIDUAL_TAG, m_ShortTermAbsResidual, traverser))
        RESTORE(LONG_TERM_ABS_RESIDUAL_TAG,
                core::CPersistUtils::restore(LONG_TERM_ABS_RESIDUAL_TAG, m_LongTermAbsResidual, traverser))
    }
    while (traverser.next());
    return true;
}

void CDecayRateController::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertValue(MULTIPLIER_TAG, m_Multiplier);
    core::CPersistUtils::persist(PREDICTION_MEAN_TAG, m_PredictionMean, inserter);
    core::CPersistUtils::persist(BIAS_TAG, m_Bias, inserter);
    core::CPersistUtils::persist(SHORT_TERM_ABS_RESIDUAL_TAG, m_ShortTermAbsResidual, inserter);
    core::CPersistUtils::persist(LONG_TERM_ABS_RESIDUAL_TAG, m_LongTermAbsResidual, inserter);
}

double CDecayRateController::multiplier(const TDouble1Vec &prediction,
                                        const TDouble1Vec &residual,
                                        core_t::TTime bucketLength,
                                        double learnRate,
                                        double decayRate)
{
    // We could estimate the, presumably non-linear, function describing
    // the dynamics of the various error quantities and minimize the bias
    // and short term absolute prediction error using the decay rate as a
    // control variable. In practice, we want to bound the decay rate in
    // a range around the target decay rate and increase it when we detect
    // a bias or that the short term prediction error is significantly
    // greater than long term prediction error and vice versa. Using bang-
    // bang control, with some hysteresis, on the rate of change of the
    // decay rate does this.

    double result = 1.0;

    double count = this->count();
    double slow  = ::exp(-       decayRate);
    double fast  = ::exp(-10.0 * decayRate);
    TDouble1Vec a, b;
    a.reserve(m_Bias.size());
    b.reserve(m_Bias.size());
    for (std::size_t i = 0u; i < m_Bias.size(); ++i)
    {
        double bias  = maths::CBasicStatistics::mean(m_Bias[i]);
        double width = 10.0 * maths::CBasicStatistics::mean(m_LongTermAbsResidual[i]);
        a.push_back(bias - width);
        b.push_back(bias + width);
    }

    TMeanAccumulator1Vec *values[] =
        {
            &m_PredictionMean, &m_Bias, &m_ShortTermAbsResidual, &m_LongTermAbsResidual
        };
    double factors[] = { slow, slow, fast, slow };
    TMeanAccumulator msv[4];

    for (std::size_t i = 0u; i < prediction.size(); ++i)
    {
        double ri = count == 0.0 ? residual[i] : maths::CTools::truncate(residual[i], a[i], b[i]);
        double vi[] = { prediction[i], ri, ::fabs(ri), ::fabs(ri) };
        for (std::size_t j = 0u; j < boost::size(vi); ++j)
        {
            TMeanAccumulator1Vec &vj = *values[j];
            vj[i].add(vi[j], learnRate);
            vj[i].age(factors[j]);
            msv[j].add(::pow(maths::CBasicStatistics::mean(vj[i]), 2.0));
        }
    }

    double residuals[] =
        {
            ::sqrt(maths::CBasicStatistics::mean(msv[1])),
            ::sqrt(maths::CBasicStatistics::mean(msv[2])),
            ::sqrt(maths::CBasicStatistics::mean(msv[3]))
        };
    LOG_TRACE("residuals = " << core::CContainerPrinter::print(residuals));
    LOG_TRACE("count     = " << count);
    LOG_TRACE("cov       = " << this->cov());

    if (count > MINIMUM_COUNT_TO_CONTROL && this->cov() > MINIMUM_COV_TO_CONTROL)
    {
        if (   residuals[0] > LARGE_BIAS * residuals[2]
            || residuals[1] > LARGE_RATIO * residuals[2])
        {
            result = effectiveRate(INCREASE_RATE, bucketLength);
        }
        else if (   residuals[0] < SMALL_BIAS * residuals[2]
                 || residuals[1] < SMALL_RATIO * residuals[2])
        {
            result = effectiveRate(DECREASE_RATE, bucketLength);
        }
    }
    result = maths::CTools::truncate(m_Multiplier * result,
                                     MINIMUM_MULTIPLIER,
                                     MAXIMUM_MULTIPLIER) / m_Multiplier;
    m_Multiplier *= result;

    LOG_TRACE("multiplier = " << result);

    return result;
}

void CDecayRateController::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CDecayRateController");
    core::CMemoryDebug::dynamicSize("m_PredictionMean", m_PredictionMean, mem);
    core::CMemoryDebug::dynamicSize("m_Bias", m_Bias, mem);
    core::CMemoryDebug::dynamicSize("m_ShortTermAbsResidual", m_ShortTermAbsResidual, mem);
    core::CMemoryDebug::dynamicSize("m_LongTermAbsResidual", m_LongTermAbsResidual, mem);
}

std::size_t CDecayRateController::memoryUsage(void) const
{
    std::size_t mem = core::CMemory::dynamicSize(m_PredictionMean);
    mem += core::CMemory::dynamicSize(m_Bias);
    mem += core::CMemory::dynamicSize(m_ShortTermAbsResidual);
    mem += core::CMemory::dynamicSize(m_LongTermAbsResidual);
    return mem;
}

uint64_t CDecayRateController::checksum(uint64_t seed) const
{
    seed = maths::CChecksum::calculate(seed, m_PredictionMean);
    seed = maths::CChecksum::calculate(seed, m_Bias);
    seed = maths::CChecksum::calculate(seed, m_ShortTermAbsResidual);
    return maths::CChecksum::calculate(seed, m_LongTermAbsResidual);
}

double CDecayRateController::count(void) const
{
    return maths::CBasicStatistics::count(m_LongTermAbsResidual[0]);
}

double CDecayRateController::cov(void) const
{
    TMeanAccumulator result;
    for (std::size_t i = 0u; i < m_LongTermAbsResidual.size(); ++i)
    {
        result.add(  maths::CBasicStatistics::mean(m_LongTermAbsResidual[i])
                   / ::fabs(maths::CBasicStatistics::mean(m_PredictionMean[i])));
    }
    return maths::CBasicStatistics::mean(result);
}

}
}
