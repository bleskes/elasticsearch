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

#include <maths/CTrendTests.h>

#include <core/CContainerPrinter.h>
#include <core/CLogger.h>
#include <core/Constants.h>
#include <core/CScopedLock.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>
#include <core/RestoreMacros.h>

#include <maths/CChecksum.h>
#include <maths/CIntegerTools.h>
#include <maths/CSampling.h>
#include <maths/CSignal.h>
#include <maths/CStatisticalTests.h>
#include <maths/CTools.h>

#include <boost/bind.hpp>
#include <boost/circular_buffer.hpp>
#include <boost/math/distributions/chi_squared.hpp>
#include <boost/math/distributions/fisher_f.hpp>
#include <boost/random/uniform_int_distribution.hpp>
#include <boost/random/uniform_real_distribution.hpp>
#include <boost/numeric/conversion/bounds.hpp>
#include <boost/range.hpp>
#include <boost/ref.hpp>

#include <numeric>

namespace ml
{
namespace maths
{

namespace
{

typedef std::vector<double> TDoubleVec;
typedef std::vector<core_t::TTime> TTimeVec;
typedef std::pair<double, double> TDoubleDoublePr;
typedef std::pair<std::size_t, std::size_t> TSizeSizePr;
typedef core::CSmallVector<TSizeSizePr, 2> TSizeSizePr2Vec;
typedef boost::array<CFloatStorage, 2> TFloatArray;
typedef CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;
typedef std::vector<TMeanAccumulator> TMeanAccumulatorVec;
typedef CTrendTests::TMeanVarAccumulator TMeanVarAccumulator;
typedef CTrendTests::TFloatMeanAccumulator TFloatMeanAccumulator;
typedef CTrendTests::TFloatMeanAccumulatorVec TFloatMeanAccumulatorVec;
typedef CTrendTests::CPeriodicity::TTimeTimePr2Vec TTimeTimePr2Vec;
typedef CTrendTests::TTimeTimePrMeanVarAccumulatorPr TTimeTimePrMeanVarAccumulatorPr;

const core_t::TTime HOUR     = core::constants::HOUR;
const core_t::TTime DAY      = core::constants::DAY;
const core_t::TTime WEEKEND  = core::constants::WEEKEND;
const core_t::TTime WEEKDAYS = core::constants::WEEKDAYS;
const core_t::TTime WEEK     = core::constants::WEEK;

//! Compute the \p percentage % for the mean of \p n normal random
//! variables with variance \p variance.
double meanAtPercentile(double mean, double variance, double n, double percentage)
{
    try
    {
        boost::math::normal_distribution<> normal(mean, ::sqrt(variance / n));
        return boost::math::quantile(normal, percentage / 100.0);
    }
    catch (const std::exception &e)
    {
        LOG_ERROR("Bad input: " << e.what()
                  << ", n = " << n
                  << ", percentage = " << percentage);
    }
    return mean;
}

//! Compute the \p percentage % variance for a chi-squared random
//! variance with \p n - 1 degrees of freedom.
double varianceAtPercentile(double variance, double n, double percentage)
{
    try
    {
        boost::math::chi_squared_distribution<> chi(n - 1.0);
        return boost::math::quantile(chi, percentage / 100.0) / (n - 1.0) * variance;
    }
    catch (const std::exception &e)
    {
        LOG_ERROR("Bad input: " << e.what()
                  << ", n = " << n
                  << ", percentage = " << percentage);
    }
    return variance;
}

//! Compute the \p percentage % autocorrelation for a F distributed
//! random autocorrelation with parameters \p n - 1 and \p n - 1.
double autocorrelationAtPercentile(double autocorrelation, double n, double percentage)
{
    try
    {
        boost::math::fisher_f_distribution<> f(n - 1.0, n - 1.0);
        return boost::math::quantile(f, percentage / 100.0) * autocorrelation;
    }
    catch (const std::exception &e)
    {
        LOG_ERROR("Bad input: " << e.what()
                  << ", n = " << n
                  << ", percentage = " << percentage);
    }
    return autocorrelation;
}

//! Generate \p n samples uniformly in the interval [\p a, \p b].
template<typename ITR>
void generateUniformSamples(boost::random::mt19937_64 &rng,
                            double a,
                            double b,
                            std::size_t n,
                            ITR samples)
{
    boost::random::uniform_real_distribution<> uniform(a, b);
    std::generate_n(samples, n, boost::bind(uniform, boost::ref(rng)));
}

//! Force the sample mean to zero.
void zeroMean(TDoubleVec &samples)
{
    TMeanAccumulator mean;
    for (std::size_t j = 0u; j < samples.size(); ++j)
    {
        mean.add(samples[j]);
    }
    for (std::size_t j = 0u; j < samples.size(); ++j)
    {
        samples[j] -= CBasicStatistics::mean(mean);
    }
}

//! Compute the windows of length \p length in \p window.
TTimeTimePr2Vec computeWindows(core_t::TTime start,
                               core_t::TTime end,
                               core_t::TTime length,
                               core_t::TTime longPeriod)
{
    TTimeTimePr2Vec result;
    result.reserve(static_cast<std::size_t>((end - start) / longPeriod));
    for (core_t::TTime time = start; time < end; time += longPeriod)
    {
        result.push_back(std::make_pair(time, time + length));
    }
    return result;
}

//! Get the index ranges corresponding to \p windows.
std::size_t indexWindows(const TTimeTimePr2Vec &windows,
                         core_t::TTime bucketLength,
                         TSizeSizePr2Vec &result)
{
    std::size_t length = 0u;
    result.reserve(windows.size());
    for (std::size_t i = 0u; i < windows.size(); ++i)
    {
        result.push_back(std::make_pair(windows[i].first  / bucketLength,
                                        windows[i].second / bucketLength));
        length += result[i].second - result[i].first;
    }
    return length;
}

//! Compute the periodic trend from \p values falling in \p windows.
template<typename T>
void periodicTrend(const TFloatMeanAccumulatorVec &values,
                   const TSizeSizePr2Vec &windows, T &trend)
{
    if (!trend.empty())
    {
        std::size_t length = values.size();
        std::size_t period = trend.size();
        for (std::size_t i = 0u, j = 0u; i < windows.size(); ++i)
        {
            std::size_t a = windows[i].first;
            std::size_t b = windows[i].second;
            for (std::size_t k = a; k < b; ++j, ++k)
            {
                const TFloatMeanAccumulator &value = values[k % length];
                trend[j % period].add(CBasicStatistics::mean(value),
                                      CBasicStatistics::count(value));
            }
        }
    }
}

//! Compute the periodic trend from \p values falling in \p windows.
template<typename T>
void periodicTrend(const TFloatMeanAccumulatorVec &values,
                   const TTimeTimePr2Vec &windows,
                   core_t::TTime bucketLength, T &trend)
{
    TSizeSizePr2Vec windows_;
    indexWindows(windows, bucketLength, windows_);
    periodicTrend(values, windows_, trend);
}

//! Compute the average of the values at \p times.
void averageValue(const TFloatMeanAccumulatorVec &values,
                  const TTimeVec &times,
                  core_t::TTime bucketLength,
                  TMeanVarAccumulator &value)
{
    for (std::size_t i = 0u; i < times.size(); ++i)
    {
        std::size_t j = static_cast<std::size_t>(times[i] / bucketLength);
        value.add(CBasicStatistics::mean(values[j]),
                  CBasicStatistics::count(values[j]));
    }
}

//! Extract the count.
template<typename T> double count(const T &value)
{
    return CBasicStatistics::count(value);
}

//! Extract the count.
double count(const TTimeTimePrMeanVarAccumulatorPr &value)
{
    return CBasicStatistics::count(value.second);
}

//! Extract the mean.
template<typename T> double mean(const T &value)
{
    return CBasicStatistics::mean(value);
}

//! Extract the mean.
double mean(const TTimeTimePrMeanVarAccumulatorPr &value)
{
    return CBasicStatistics::mean(value.second);
}

//! Compute the variance of the \p trend values.
template<typename T>
double trendVariance(const T &trend)
{
    TMeanVarAccumulator result;
    for (std::size_t i = 0u; i < trend.size(); ++i)
    {
        result.add(mean(trend[i]), count(trend[i]));
    }
    return CBasicStatistics::variance(result);
}

//! Get the maximum residual of \p trend.
template<typename T>
double trendAmplitude(const T &trend)
{
    typedef CBasicStatistics::COrderStatisticsStack<double, 1, std::greater<double> > TMaxAccumulator;

    TMeanAccumulator level;
    for (std::size_t i = 0u; i < trend.size(); ++i)
    {
        level.add(mean(trend[i]), count(trend[i]));
    }

    TMaxAccumulator result;
    result.add(0.0);
    for (std::size_t i = 0u; i < trend.size(); ++i)
    {
        if (count(trend[i]) > 0.0)
        {
            result.add(::fabs(mean(trend[i]) - CBasicStatistics::mean(level)));
        }
    }

    return result[0];
}

//! Extract the residual variance from the mean of a collection
//! of residual variances.
double residualVariance(const TMeanAccumulator &mean)
{
    double n = CBasicStatistics::count(mean);
    return n / (n - 1.0) * CBasicStatistics::mean(mean);
}

//! Extract the residual variance of \p bucket of a trend.
TMeanAccumulator residualVariance(const TMeanVarAccumulator &bucket,
                                  double scale)
{
    return CBasicStatistics::accumulator(scale * CBasicStatistics::count(bucket),
                                         CBasicStatistics::maximumLikelihoodVariance(bucket));
}

//! \brief Partially specialized helper class to get the trend
//! residual variance as a specified type.
template<typename R> struct SResidualVarianceImpl {};

//! \brief Get the residual variance as a double.
template<> struct SResidualVarianceImpl<double>
{
    static double get(const TMeanAccumulator &mean)
    {
        return residualVariance(mean);
    }
};

//! \brief Get the residual variance as a mean accumulator.
template<> struct SResidualVarianceImpl<TMeanAccumulator>
{
    static TMeanAccumulator get(const TMeanAccumulator &mean)
    {
        return mean;
    }
};

//! Compute the residual variance of the trend \p trend.
template<typename R, typename T>
R residualVariance(const T &trend, double scale)
{
    TMeanAccumulator result;
    for (std::size_t i = 0u; i < trend.size(); ++i)
    {
        result.add(CBasicStatistics::maximumLikelihoodVariance(trend[i]),
                   CBasicStatistics::count(trend[i]));
    }
    result.s_Count *= scale;
    return SResidualVarianceImpl<R>::get(result);
}

//! Compute the minimum autocorrelation as a function of the
//! variance in the trend.
double minimumAutocorrelation(double variance,
                              double varianceThreshold,
                              double autocorrelationThreshold)
{
    return CTools::truncate(1.0 - 0.5 * (variance - varianceThreshold)
                                       / varianceThreshold, 0.8, 1.0)
         * autocorrelationThreshold;
}

//! Compute the discrete autocorrelation of \p values for \p offset
//! after subtracting \p trend.
//!
//! \sa CTrendTests::autocorrelation.
template<typename T>
double remainderAutocorrelation(std::size_t offset,
                                core_t::TTime bucketLength,
                                const TFloatMeanAccumulatorVec &values,
                                const std::vector<T> &trend,
                                const TTimeTimePr2Vec &windows)
{
    if (windows.empty())
    {
        return 0.0;
    }

    TSizeSizePr2Vec windows_;
    std::size_t n = indexWindows(windows, bucketLength, windows_);

    TFloatMeanAccumulatorVec residuals(n);
    periodicTrend(values, windows_, residuals);
    for (std::size_t i = 0u; i < residuals.size(); /**/)
    {
        for (std::size_t j = 0u; j < trend.size(); ++i, ++j)
        {
            if (CBasicStatistics::count(residuals[i]) > 0.0)
            {
                residuals[i].s_Moments[0] -= mean(trend[j]);
            }
        }
    }
    return CTrendTests::autocorrelation(offset, residuals);
}

// CTrend
const std::string DECAY_RATE_TAG("a");
const std::string TIME_ORIGIN_TAG("b");
const std::string TREND_TAG("c");
const std::string VARIANCES_TAG("d");

// CStepChange
const std::string RNG_TAG("a");
const std::string BUCKET_LENGTH_TAG("b");
const std::string PROBABILITY_TAG("c");
const std::string NEXT_SAMPLE_TIME_TAG("d");
const std::string SAMPLES_TAG("e");
const std::string LEVEL_TAG("f");
const std::string VARIANCE_TAG("g");

// CRandomizedPeriodicity
// statics
//const std::string RNG_TAG("a");
const std::string DAY_RANDOM_PROJECTIONS_TAG("b");
const std::string DAY_PERIODIC_PROJECTIONS_TAG("c");
const std::string DAY_RESAMPLED_TAG("d");
const std::string WEEK_RANDOM_PROJECTIONS_TAG("e");
const std::string WEEK_PERIODIC_PROJECTIONS_TAG("f");
const std::string WEEK_RESAMPLED_TAG("g");
const std::string ARRAY_INDEX_TAG("h");
// non-statics
const std::string DAY_PROJECTIONS_TAG("a");
const std::string DAY_STATISTICS_TAG("b");
const std::string DAY_REFRESHED_PROJECTIONS_TAG("c");
const std::string WEEK_PROJECTIONS_TAG("d");
const std::string WEEK_STATISTICS_TAG("e");
const std::string WEEK_REFRESHED_PROJECTIONS_TAG("f");

// CPeriodicity
const std::string WINDOW_TAG("a");
//const std::string BUCKET_LENGTH_TAG("b");
const std::string PERIODS_TAG("c");
const std::string PARTITION_TAG("d");
const std::string BUCKET_VALUE_TAG("e");

// CPeriodicity::CResult
const std::string START_OF_PARTITION_TAG("a");
const std::string HAS_PERIODS_TAG("b");

// CScanningPeriodicity
//const std::string BUCKET_LENGTH_TAG("b");
const std::string START_TIME_TAG("c");
//const std::string BUCKET_VALUES_TAG("e");


//! The maximum significance of a test statistic.
const double MAXIMUM_SIGNIFICANCE = 0.005;
//! The confidence interval used for test statistic values.
const double CONFIDENCE_INTERVAL = 80.0;

}

//////// CTrend ////////

CTrendTests::CTrend::CTrend(double decayRate) :
        m_DecayRate(decayRate),
        m_TimeOrigin(boost::numeric::bounds<core_t::TTime>::lowest())
{}

bool CTrendTests::CTrend::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        RESTORE_BUILT_IN(DECAY_RATE_TAG, m_DecayRate)
        RESTORE_BUILT_IN(TIME_ORIGIN_TAG, m_TimeOrigin)
        RESTORE(TREND_TAG, traverser.traverseSubLevel(
                               boost::bind(&TRegression::acceptRestoreTraverser, &m_Trend, _1)))
        RESTORE(VARIANCES_TAG, m_Variances.fromDelimited(traverser.value()))
    }
    while (traverser.next());
    return true;
}

void CTrendTests::CTrend::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertValue(DECAY_RATE_TAG, m_DecayRate);
    inserter.insertValue(TIME_ORIGIN_TAG, m_TimeOrigin);
    inserter.insertLevel(TREND_TAG, boost::bind(&TRegression::acceptPersistInserter, &m_Trend, _1));
    inserter.insertValue(VARIANCES_TAG, m_Variances.toDelimited());
}

void CTrendTests::CTrend::decayRate(double decayRate)
{
    m_DecayRate = decayRate;
}

void CTrendTests::CTrend::propagateForwardsByTime(double time)
{
    if (!CMathsFuncs::isFinite(time) || time < 0.0)
    {
        LOG_ERROR("Bad propagation time " << time);
        return;
    }

    double factor = ::exp(-m_DecayRate * time);

    m_Trend.age(factor);
    m_Variances.age(factor);
}

void CTrendTests::CTrend::add(core_t::TTime time, double value, double weight)
{
    if (time - 3 * WEEK >= m_TimeOrigin)
    {
        LOG_TRACE("shifting");
        m_Trend.shiftAbscissa(-this->time(time));
        m_TimeOrigin = time;
    }
    m_Trend.add(this->time(time), value, weight);
}

void CTrendTests::CTrend::captureVariance(core_t::TTime time, double value, double weight)
{
    double prediction = CRegression::predict(m_Trend, this->time(time));
    TVector values;
    values(0) = value;
    values(1) = value - prediction;
    m_Variances.add(values, weight);
}

void CTrendTests::CTrend::shift(double shift)
{
    m_Trend.shiftOrdinate(shift);
}

bool CTrendTests::CTrend::test(void) const
{
    double n  = CBasicStatistics::count(m_Variances);
    double d0 = n - 1.0;
    double d1 = n - ORDER - 1.0;
    double v0 = CBasicStatistics::maximumLikelihoodVariance(m_Variances)(0);
    double v1 = CBasicStatistics::maximumLikelihoodVariance(m_Variances)(1);
    return   d1 > 0.0
          && varianceAtPercentile(v1, n, 80.0) < HAS_TREND_VARIANCE_RATIO * v0
          && CStatisticalTests::fTest(v1 / v0, d1, d0) <= MAXIMUM_SIGNIFICANCE;
}

const CTrendTests::CTrend::TRegression &CTrendTests::CTrend::trend(void) const
{
    return m_Trend;
}

core_t::TTime CTrendTests::CTrend::origin(void) const
{
    return m_TimeOrigin;
}

double CTrendTests::CTrend::variance(void) const
{
    return CBasicStatistics::maximumLikelihoodVariance(m_Variances)(1);
}

uint64_t CTrendTests::CTrend::checksum(uint64_t seed) const
{
    seed = CChecksum::calculate(seed, m_DecayRate);
    seed = CChecksum::calculate(seed, m_TimeOrigin);
    seed = CChecksum::calculate(seed, m_Trend);
    return CChecksum::calculate(seed, m_Variances);
}

double CTrendTests::CTrend::time(core_t::TTime time) const
{
    return static_cast<double>(time - m_TimeOrigin) / static_cast<double>(WEEK);
}

const double CTrendTests::CTrend::HAS_TREND_VARIANCE_RATIO = 0.5;

//////// CStepChange ////////

CTrendTests::CStepChange::CStepChange(core_t::TTime bucketLength,
                                      std::size_t n,
                                      double p,
                                      double decayRate) :
        m_DecayRate(decayRate),
        m_BucketLength(bucketLength),
        m_P(CTools::truncate(p, 1e-10, 1.0)),
        m_NextSampleTime(boost::numeric::bounds<core_t::TTime>::lowest()),
        m_Samples(std::max(n, std::size_t(8))),
        m_Level(0.0),
        m_Variance(0.0)
{}

void CTrendTests::CStepChange::swap(CStepChange &other)
{
    std::swap(m_Rng, other.m_Rng);
    std::swap(m_DecayRate, other.m_DecayRate);
    std::swap(m_BucketLength, other.m_BucketLength);
    std::swap(m_P, other.m_P);
    std::swap(m_NextSampleTime, other.m_NextSampleTime);
    m_Samples.swap(other.m_Samples);
    std::swap(m_Level, other.m_Level);
    std::swap(m_Variance, other.m_Variance);
}

bool CTrendTests::CStepChange::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        RESTORE(RNG_TAG, m_Rng.fromString(traverser.value()))
        RESTORE_BUILT_IN(BUCKET_LENGTH_TAG, m_BucketLength)
        RESTORE(PROBABILITY_TAG, m_P.fromString(traverser.value()))
        RESTORE_BUILT_IN(NEXT_SAMPLE_TIME_TAG, m_NextSampleTime)
        RESTORE(SAMPLES_TAG, core::CPersistUtils::fromString(traverser.value(),
                                                             CFloatStorage::CFromString(),
                                                             m_Samples))
        RESTORE_BUILT_IN(LEVEL_TAG, m_Level)
        RESTORE(VARIANCE_TAG, m_Variance.fromDelimited(traverser.value()))
    }
    while (traverser.next());
    return true;
}

void CTrendTests::CStepChange::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertValue(RNG_TAG, m_Rng.toString());
    inserter.insertValue(BUCKET_LENGTH_TAG, m_BucketLength);
    inserter.insertValue(PROBABILITY_TAG, m_P.toString());
    inserter.insertValue(NEXT_SAMPLE_TIME_TAG, m_NextSampleTime);
    inserter.insertValue(SAMPLES_TAG, core::CPersistUtils::toString(m_Samples, CFloatStorage::CToString()));
    inserter.insertValue(LEVEL_TAG, m_Level);
    inserter.insertValue(VARIANCE_TAG, m_Variance.toDelimited());
}

void CTrendTests::CStepChange::clear(void)
{
    m_Samples.clear();
}

void CTrendTests::CStepChange::decayRate(double decayRate)
{
    m_DecayRate = decayRate;
}

void CTrendTests::CStepChange::propagateForwardsByTime(double time)
{
    if (time < 0.0)
    {
        LOG_ERROR("Can't propagate bucketing backwards in time");
        return;
    }

    double factor = ::exp(-m_DecayRate * time);
    m_Variance.age(factor);
}

void CTrendTests::CStepChange::add(core_t::TTime time, double value)
{
    if (time > m_NextSampleTime)
    {
        if (m_Samples.size() + 1 == m_Samples.capacity())
        {
            TMeanAccumulator level;
            for (std::size_t i = 0u; i < m_Samples.size(); ++i)
            {
                level.add(m_Samples[i]);
            }
            level.add(value);
            m_Level = CBasicStatistics::mean(level);
        }

        m_Samples.push_back(value);
        m_NextSampleTime = time + static_cast<core_t::TTime>(
                CSampling::uniformSample(m_Rng, 0.5 / m_P, 1.5 / m_P) + 0.5) * m_BucketLength;
    }
}

CTrendTests::CStepChange::CResult CTrendTests::CStepChange::captureVarianceAndTest(void)
{
    std::size_t n = m_Samples.size();

    if (n < m_Samples.capacity())
    {
        return CResult();
    }

    LOG_TRACE("Testing for level shift");

    std::size_t a = n / 8, b = (7 * n) / 8;

    TMeanAccumulator l0;
    TMeanAccumulator l1;
    TMeanVarAccumulator l2;

    for (std::size_t i = 0; i < a; ++i)
    {
        double r = m_Samples[i] - m_Level;
        l0.add(r * r);
        l1.add(r * r);
    }
    for (std::size_t i = a; i < n; ++i)
    {
        double r = m_Samples[i] - m_Level;
        l0.add(r * r);
        l2.add(m_Samples[i]);
    }

    double v0 = CBasicStatistics::mean(l0);
    double v1 = boost::numeric::bounds<double>::highest();
    double l  = 0.0;
    double nl = 0.0;
    double vl = 0.0;
    double n_ = static_cast<double>(n);
    std::size_t knot = 0;

    if (v0 < std::max(1e-4 * m_Level, 1e-12))
    {
        m_Variance += CBasicStatistics::accumulator(1.0, v0);
        return CResult();
    }

    LOG_TRACE("samples = " << core::CContainerPrinter::print(m_Samples));

    for (std::size_t i = a; i < b; ++i)
    {
        double vi = CBasicStatistics::mean(
                        l1 + CBasicStatistics::accumulator(CBasicStatistics::count(l2),
                                                           CBasicStatistics::maximumLikelihoodVariance(l2)));

        if (vi < v1)
        {
            v1 = vi;
            l  = CBasicStatistics::mean(l2);
            nl = CBasicStatistics::count(l2);
            vl = CBasicStatistics::maximumLikelihoodVariance(l2);
            knot = i;
        }
        double r = m_Samples[i] - m_Level;
        l1.add(r * r);
        l2 -= CBasicStatistics::accumulator(1.0, static_cast<double>(m_Samples[i]), 0.0);
    }

    double threshold = 3.0 * ::sqrt(CBasicStatistics::mean(
                                        m_Variance + CBasicStatistics::accumulator(1.0, v1)));

    double sign = CTools::sign(l - m_Level);
    double shift = std::max(sign * meanAtPercentile(l - m_Level, vl, nl,
                                                    50.0 + sign / 2.0 * CONFIDENCE_INTERVAL), 0.0);

    LOG_TRACE("knot = " << knot);
    LOG_TRACE("threshold = " << threshold);
    LOG_TRACE("shift = " << shift);
    LOG_TRACE("v0 = " << v0 << " v1 = " << v1);
    LOG_TRACE("significance = " << CStatisticalTests::fTest(v1 / v0, n_ - 2.0, n_ - 1.0));

    bool even   = knot >= (3 * n) / 8 && knot <= (5 * n) / 8;
    bool passed = shift > threshold && CStatisticalTests::fTest(
                                           v1 / v0, n_ - 2.0, n_ - 1.0) < MAXIMUM_SIGNIFICANCE;

    double oldLevel = m_Level;

    if (even)
    {
        m_Variance += CBasicStatistics::accumulator(1.0, passed ? v1 : v0);
        m_Level     = passed ? l : m_Level;
    }

    return CResult(passed ? (even ? E_True : E_Undetermined) : E_False, m_Level, m_Level - oldLevel);
}

uint64_t CTrendTests::CStepChange::checksum(uint64_t seed) const
{
    seed = CChecksum::calculate(seed, m_DecayRate);
    seed = CChecksum::calculate(seed, m_BucketLength);
    seed = CChecksum::calculate(seed, m_P);
    seed = CChecksum::calculate(seed, m_NextSampleTime);
    seed = CChecksum::calculate(seed, m_Samples);
    seed = CChecksum::calculate(seed, m_Level);
    return CChecksum::calculate(seed, m_Variance);
}

CTrendTests::CStepChange::CResult::CResult(void) :
        m_FoundShift(E_False),
        m_Level(0.0),
        m_Shift(0.0)
{}

CTrendTests::CStepChange::CResult::CResult(ETernaryBool foundShift, double level, double shift) :
        m_FoundShift(foundShift),
        m_Level(level),
        m_Shift(shift)
{}

CTrendTests::ETernaryBool CTrendTests::CStepChange::CResult::value(void) const
{
    return m_FoundShift;
}

double CTrendTests::CStepChange::CResult::level(void) const
{
    return m_Level;
}

double CTrendTests::CStepChange::CResult::shift(void) const
{
    return m_Shift;
}

//////// CRandomizedPeriodicity ////////

CTrendTests::CRandomizedPeriodicity::CRandomizedPeriodicity(void) :
        m_DayRefreshedProjections(-DAY_RESAMPLE_INTERVAL),
        m_WeekRefreshedProjections(-DAY_RESAMPLE_INTERVAL)
{
    resample(0);
}

bool CTrendTests::CRandomizedPeriodicity::staticsAcceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    // Note we require that we only ever do one persistence per process.

    std::size_t index = 0;
    reset();

    core::CScopedLock lock(ms_Lock);

    do
    {
        const std::string &name = traverser.name();

        if (name == RNG_TAG)
        {
            // Replace '_' with space
            std::string value(traverser.value());
            std::replace(value.begin(), value.end(), '_', ' ');
            std::stringstream ss;
            ss << value;
            ss >> ms_Rng;
            continue;
        }
        RESTORE_SETUP_TEARDOWN(DAY_RESAMPLED_TAG,
                               core_t::TTime resampled,
                               core::CStringUtils::stringToType(traverser.value(), resampled),
                               ms_DayResampled.store(resampled))
        RESTORE_SETUP_TEARDOWN(WEEK_RESAMPLED_TAG,
                               core_t::TTime resampled,
                               core::CStringUtils::stringToType(traverser.value(), resampled),
                               ms_WeekResampled.store(resampled))
        RESTORE_BUILT_IN(ARRAY_INDEX_TAG, index)
        RESTORE_SETUP_TEARDOWN(DAY_RANDOM_PROJECTIONS_TAG,
                               double d,
                               core::CStringUtils::stringToType(traverser.value(), d),
                               ms_DayRandomProjections[index].push_back(d))
        RESTORE_SETUP_TEARDOWN(DAY_PERIODIC_PROJECTIONS_TAG,
                               double d,
                               core::CStringUtils::stringToType(traverser.value(), d),
                               ms_DayPeriodicProjections[index].push_back(d))
        RESTORE_SETUP_TEARDOWN(WEEK_RANDOM_PROJECTIONS_TAG,
                               double d,
                               core::CStringUtils::stringToType(traverser.value(), d),
                               ms_WeekRandomProjections[index].push_back(d))
        RESTORE_SETUP_TEARDOWN(WEEK_PERIODIC_PROJECTIONS_TAG,
                               double d,
                               core::CStringUtils::stringToType(traverser.value(), d),
                               ms_WeekPeriodicProjections[index].push_back(d))
    }
    while (traverser.next());

    return true;
}

void CTrendTests::CRandomizedPeriodicity::staticsAcceptPersistInserter(core::CStatePersistInserter &inserter)
{
    // Note we require that we only ever do one persistence per process.

    core::CScopedLock lock(ms_Lock);

    std::ostringstream ss;
    ss << ms_Rng;
    std::string rng(ss.str());
    // Replace spaces else JSON parsers get confused
    std::replace(rng.begin(), rng.end(), ' ', '_');
    inserter.insertValue(RNG_TAG, rng);
    inserter.insertValue(DAY_RESAMPLED_TAG, ms_DayResampled.load());
    inserter.insertValue(WEEK_RESAMPLED_TAG, ms_WeekResampled.load());
    for (std::size_t j = 0; j < N; j++)
    {
        inserter.insertValue(ARRAY_INDEX_TAG, j);
        for (std::size_t i = 0; i < ms_DayRandomProjections[j].size(); i++)
        {
            inserter.insertValue(DAY_RANDOM_PROJECTIONS_TAG, ms_DayRandomProjections[j][i]);
        }
        for (std::size_t i = 0; i < ms_DayPeriodicProjections[j].size(); i++)
        {
            inserter.insertValue(DAY_PERIODIC_PROJECTIONS_TAG, ms_DayPeriodicProjections[j][i]);
        }
        for (std::size_t i = 0; i < ms_WeekRandomProjections[j].size(); i++)
        {
            inserter.insertValue(WEEK_RANDOM_PROJECTIONS_TAG, ms_WeekRandomProjections[j][i]);
        }
        for (std::size_t i = 0; i < ms_WeekPeriodicProjections[j].size(); i++)
        {
            inserter.insertValue(WEEK_PERIODIC_PROJECTIONS_TAG, ms_WeekPeriodicProjections[j][i]);
        }
    }
}

bool CTrendTests::CRandomizedPeriodicity::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();

        RESTORE(DAY_PROJECTIONS_TAG, m_DayProjections.fromDelimited(traverser.value()))
        RESTORE(DAY_STATISTICS_TAG, m_DayStatistics.fromDelimited(traverser.value()))
        RESTORE(DAY_REFRESHED_PROJECTIONS_TAG,
                core::CStringUtils::stringToType(traverser.value(),
                                                 m_DayRefreshedProjections))
        RESTORE(WEEK_PROJECTIONS_TAG, m_WeekProjections.fromDelimited(traverser.value()))
        RESTORE(WEEK_STATISTICS_TAG, m_WeekStatistics.fromDelimited(traverser.value()))
        RESTORE(DAY_STATISTICS_TAG, m_DayStatistics.fromDelimited(traverser.value()))
        RESTORE(WEEK_REFRESHED_PROJECTIONS_TAG,
                core::CStringUtils::stringToType(traverser.value(),
                                                 m_WeekRefreshedProjections))
    }
    while (traverser.next());

    return true;
}

void CTrendTests::CRandomizedPeriodicity::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertValue(DAY_PROJECTIONS_TAG, m_DayProjections.toDelimited());
    inserter.insertValue(DAY_STATISTICS_TAG, m_DayStatistics.toDelimited());
    inserter.insertValue(DAY_REFRESHED_PROJECTIONS_TAG, m_DayRefreshedProjections);
    inserter.insertValue(WEEK_PROJECTIONS_TAG, m_WeekProjections.toDelimited());
    inserter.insertValue(WEEK_STATISTICS_TAG, m_WeekStatistics.toDelimited());
    inserter.insertValue(WEEK_REFRESHED_PROJECTIONS_TAG, m_WeekRefreshedProjections);
}

void CTrendTests::CRandomizedPeriodicity::add(core_t::TTime time, double value)
{
    resample(time);

    if (time >= m_DayRefreshedProjections + DAY_RESAMPLE_INTERVAL)
    {
        LOG_TRACE("Updating day statistics");
        updateStatistics(m_DayProjections, m_DayStatistics);
        m_DayRefreshedProjections = CIntegerTools::floor(time, DAY_RESAMPLE_INTERVAL);
    }
    if (time >= m_WeekRefreshedProjections + WEEK_RESAMPLE_INTERVAL)
    {
        LOG_TRACE("Updating week statistics");
        updateStatistics(m_WeekProjections, m_WeekStatistics);
        m_WeekRefreshedProjections = CIntegerTools::floor(time, WEEK_RESAMPLE_INTERVAL);
    }

    TVector2N daySample;
    TVector2N weekSample;
    std::size_t td = static_cast<std::size_t>( (time % DAY_RESAMPLE_INTERVAL)
                                              / SAMPLE_INTERVAL);
    std::size_t d  = static_cast<std::size_t>( (time % DAY)
                                              / SAMPLE_INTERVAL);
    std::size_t tw = static_cast<std::size_t>( (time % WEEK_RESAMPLE_INTERVAL)
                                              / SAMPLE_INTERVAL);
    std::size_t w  = static_cast<std::size_t>( (time % WEEK)
                                              / SAMPLE_INTERVAL);

    for (std::size_t i = 0u; i < N; ++i)
    {
        daySample(2*i+0)  = ms_DayRandomProjections[i][td] * value;
        daySample(2*i+1)  = ms_DayPeriodicProjections[i][d] * value;
        weekSample(2*i+0) = ms_WeekRandomProjections[i][tw] * value;
        weekSample(2*i+1) = ms_WeekPeriodicProjections[i][w] * value;
    }

    m_DayProjections.add(daySample);
    m_WeekProjections.add(weekSample);
}

bool CTrendTests::CRandomizedPeriodicity::test(void) const
{
    static const double SIGNIFICANCE = 1e-3;

    try
    {
        double nd = CBasicStatistics::count(m_DayStatistics);
        if (nd >= 1.0)
        {
            TVector2 S = CBasicStatistics::mean(m_DayStatistics);
            LOG_TRACE("Day test statistic, S = " << S << ", n = " << nd);
            double ratio = S(0) == S(1) ?
                           1.0 : (S(0) == 0.0 ? boost::numeric::bounds<double>::highest() :
                                                static_cast<double>(S(1) / S(0)));
            double significance = 1.0 - CStatisticalTests::fTest(ratio, nd, nd);
            LOG_TRACE("Daily significance = " << significance);
            if (significance < SIGNIFICANCE)
            {
                return true;
            }
        }

        double nw = CBasicStatistics::count(m_WeekStatistics);
        if (nw >= 1.0)
        {
            TVector2 S = CBasicStatistics::mean(m_WeekStatistics);
            LOG_TRACE("Week test statistic, S = " << S);
            double ratio = S(0) == S(1) ?
                           1.0 : (S(0) == 0.0 ? boost::numeric::bounds<double>::highest() :
                                                static_cast<double>(S(1) / S(0)));
            double significance = 1.0 - CStatisticalTests::fTest(ratio, nw, nw);
            LOG_TRACE("Weekly significance = " << significance);
            if (significance < SIGNIFICANCE)
            {
                return true;
            }
        }
    }
    catch (const std::exception &e)
    {
        LOG_ERROR("Failed to test for periodicity: " << e.what());
    }

    return false;
}

void CTrendTests::CRandomizedPeriodicity::reset(void)
{
    core::CScopedLock lock(ms_Lock);

    ms_Rng = boost::random::mt19937_64();
    for (std::size_t i = 0u; i < N; ++i)
    {
        ms_DayRandomProjections[i].clear();
        ms_DayPeriodicProjections[i].clear();
        ms_WeekRandomProjections[i].clear();
        ms_WeekPeriodicProjections[i].clear();
    }
    ms_DayResampled = -DAY_RESAMPLE_INTERVAL;
    ms_WeekResampled = -WEEK_RESAMPLE_INTERVAL;
}

uint64_t CTrendTests::CRandomizedPeriodicity::checksum(uint64_t seed) const
{
    // This checksum is problematic until we switch to using our
    // own rng for each test.
    //seed = CChecksum::calculate(seed, m_DayProjections);
    //seed = CChecksum::calculate(seed, m_DayStatistics);
    //seed = CChecksum::calculate(seed, m_DayRefreshedProjections);
    //seed = CChecksum::calculate(seed, m_WeekProjections);
    //seed = CChecksum::calculate(seed, m_WeekStatistics);
    //return CChecksum::calculate(seed, m_WeekRefreshedProjections);
    return seed;
}

void CTrendTests::CRandomizedPeriodicity::updateStatistics(TVector2NMeanAccumulator &projections,
                                                           TVector2MeanAccumulator &statistics)
{
    static const double ALPHA = 0.1;

    if (CBasicStatistics::count(projections) > 0.0)
    {
        const TVector2N &mean = CBasicStatistics::mean(projections);
        LOG_TRACE("mean = " << mean);

        TVector2MeanAccumulator statistic;
        for (std::size_t i = 0u; i < N; ++i)
        {
            TVector2 s;
            s(0) = mean(2*i+0) * mean(2*i+0);
            s(1) = mean(2*i+1) * mean(2*i+1);
            statistic.add(s);
        }
        statistics += statistic;
        statistics.age(1.0 - ALPHA);
        LOG_TRACE("statistics = " << statistics);
    }

    projections = TVector2NMeanAccumulator();
}

void CTrendTests::CRandomizedPeriodicity::resample(core_t::TTime time)
{
    if (time >= ms_DayResampled.load(atomic_t::memory_order_acquire) + DAY_RESAMPLE_INTERVAL)
    {
        core::CScopedLock lock(ms_Lock);

        LOG_TRACE("Updating daily random projections at " << time);
        if (time >= ms_DayResampled.load(atomic_t::memory_order_relaxed) + DAY_RESAMPLE_INTERVAL)
        {
            resample(DAY,
                     DAY_RESAMPLE_INTERVAL,
                     ms_DayPeriodicProjections,
                     ms_DayRandomProjections);
            ms_DayResampled.store(CIntegerTools::floor(time, DAY_RESAMPLE_INTERVAL),
                                  atomic_t::memory_order_release);
        }
    }

    if (time >= ms_WeekResampled.load(atomic_t::memory_order_acquire) + WEEK_RESAMPLE_INTERVAL)
    {
        core::CScopedLock lock(ms_Lock);

        LOG_TRACE("Updating weekly random projections at " << time);
        if (time >= ms_WeekResampled.load(atomic_t::memory_order_relaxed) + WEEK_RESAMPLE_INTERVAL)
        {
            resample(WEEK,
                     WEEK_RESAMPLE_INTERVAL,
                     ms_WeekPeriodicProjections,
                     ms_WeekRandomProjections);
            ms_WeekResampled.store(CIntegerTools::floor(time, WEEK_RESAMPLE_INTERVAL),
                                   atomic_t::memory_order_release);
        }
    }
}

void CTrendTests::CRandomizedPeriodicity::resample(core_t::TTime period,
                                                   core_t::TTime resampleInterval,
                                                   TDoubleVec (&periodicProjections)[N],
                                                   TDoubleVec (&randomProjections)[N])
{
    std::size_t n = static_cast<std::size_t>(period / SAMPLE_INTERVAL);
    std::size_t t = static_cast<std::size_t>(resampleInterval / SAMPLE_INTERVAL);
    std::size_t p = static_cast<std::size_t>(resampleInterval / period);
    for (std::size_t i = 0u; i < N; ++i)
    {
        periodicProjections[i].resize(n);
        generateUniformSamples(ms_Rng, -1.0, 1.0, n, periodicProjections[i].begin());
        zeroMean(periodicProjections[i]);
        randomProjections[i].resize(t);
        for (std::size_t j = 0u; j < p; ++j)
        {
            std::copy(periodicProjections[i].begin(),
                      periodicProjections[i].end(),
                      randomProjections[i].begin() + j * n);
            CSampling::random_shuffle(ms_Rng,
                                      randomProjections[i].begin() + j * n,
                                      randomProjections[i].begin() + (j+1) * n);
        }
    }
}

const core_t::TTime CTrendTests::CRandomizedPeriodicity::SAMPLE_INTERVAL(3600);
const core_t::TTime CTrendTests::CRandomizedPeriodicity::DAY_RESAMPLE_INTERVAL(1209600);
const core_t::TTime CTrendTests::CRandomizedPeriodicity::WEEK_RESAMPLE_INTERVAL(2419200);
boost::random::mt19937_64 CTrendTests::CRandomizedPeriodicity::ms_Rng = boost::random::mt19937_64();
TDoubleVec CTrendTests::CRandomizedPeriodicity::ms_DayRandomProjections[N] = {};
TDoubleVec CTrendTests::CRandomizedPeriodicity::ms_DayPeriodicProjections[N] = {};
atomic_t::atomic<core_t::TTime> CTrendTests::CRandomizedPeriodicity::ms_DayResampled(-DAY_RESAMPLE_INTERVAL);
TDoubleVec CTrendTests::CRandomizedPeriodicity::ms_WeekRandomProjections[N] = {};
TDoubleVec CTrendTests::CRandomizedPeriodicity::ms_WeekPeriodicProjections[N] = {};
atomic_t::atomic<core_t::TTime> CTrendTests::CRandomizedPeriodicity::ms_WeekResampled(-WEEK_RESAMPLE_INTERVAL);
core::CMutex CTrendTests::CRandomizedPeriodicity::ms_Lock;

//////// CPeriodicity ////////

CTrendTests::CPeriodicity::CPeriodicity(double decayRate) :
        m_DecayRate(decayRate),
        m_BucketLength(0),
        m_Window(0)
{}

void CTrendTests::CPeriodicity::swap(CPeriodicity &other)
{
    std::swap(m_DecayRate, other.m_DecayRate);
    std::swap(m_BucketLength, other.m_BucketLength);
    std::swap(m_Window, other.m_Window);
    m_Periods.swap(other.m_Periods);
    m_Partition.swap(other.m_Partition);
    m_BucketValues.swap(other.m_BucketValues);
}

bool CTrendTests::CPeriodicity::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        RESTORE_BUILT_IN(BUCKET_LENGTH_TAG, m_BucketLength)
        RESTORE_BUILT_IN(WINDOW_TAG, m_Window)
        RESTORE(PERIODS_TAG, core::CPersistUtils::restore(PERIODS_TAG, m_Periods, traverser))
        RESTORE(PARTITION_TAG, core::CPersistUtils::restore(PARTITION_TAG, m_Partition, traverser))
        RESTORE(BUCKET_VALUE_TAG, CBasicStatistics::restoreSampleCentralMoments(traverser, m_BucketValues))
    }
    while (traverser.next());

    return true;
}

void CTrendTests::CPeriodicity::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertValue(BUCKET_LENGTH_TAG, m_BucketLength);
    inserter.insertValue(WINDOW_TAG, m_Window);
    core::CPersistUtils::persist(PERIODS_TAG, m_Periods, inserter);
    core::CPersistUtils::persist(PARTITION_TAG, m_Partition, inserter);
    CBasicStatistics::persistSampleCentralMoments(m_BucketValues, BUCKET_VALUE_TAG, inserter);
}

bool CTrendTests::CPeriodicity::initialize(core_t::TTime bucketLength,
                                           core_t::TTime window,
                                           TTime2Vec periods,
                                           TTime2Vec partition,
                                           const TFloatMeanAccumulatorVec &initial)
{
    // The following conditions need to hold:
    //   - We're only interested in decomposing into two components,
    //   - The window needs to be at least twice the longest period,
    //   - The window needs to be a multiple of the longest period,
    //   - The periods needs to be multiples of the bucket length,
    //   - We're not interested in components which beat,
    //   - The partition must be of the longest period.
    std::sort(periods.begin(), periods.end());
    if (    periods.size() != 2
        || (partition.size() != 0 && partition.size() != 2)
        ||  window <= periods[1]
        || (window > 2 * periods[1] && (window % periods[1]) != 0)
        || (periods[0] % bucketLength) != 0
        || (periods[1] % bucketLength) != 0
        || (periods[1] % periods[0]) != 0
        || (partition.size() > 0 && std::accumulate(partition.begin(),
                                                    partition.end(),
                                                    core_t::TTime(0)) != periods[1]))
    {
        m_Periods.clear();
        return false;
    }

    m_Window       = window;
    m_Periods      = periods;
    m_Partition    = partition;
    m_BucketLength = bucketLength;
    m_BucketValues.resize(static_cast<std::size_t>(window / m_BucketLength));
    std::copy(initial.begin(),
              initial.begin() + std::min(initial.size(), m_BucketValues.size()),
              m_BucketValues.begin());

    return true;
}

bool CTrendTests::CPeriodicity::initialized(void) const
{
    return m_BucketValues.size() > 0;
}

void CTrendTests::CPeriodicity::decayRate(double decayRate)
{
    m_DecayRate = decayRate;
}

void CTrendTests::CPeriodicity::propagateForwardsByTime(double time)
{
    if (time < 0.0)
    {
        LOG_ERROR("Can't propagate bucketing backwards in time");
        return;
    }

    if (!this->initialized())
    {
        return;
    }

    double factor  = ::exp(-m_DecayRate * time);

    for (std::size_t i = 0u; i < m_BucketValues.size(); ++i)
    {
        m_BucketValues[i].age(factor);
    }
}

void CTrendTests::CPeriodicity::add(core_t::TTime time, double value, double weight)
{
    if (!m_BucketValues.empty())
    {
        std::size_t i = static_cast<std::size_t>((time % m_Window) / m_BucketLength);
        m_BucketValues[i].add(value, weight);
    }
}

const CTrendTests::TTime2Vec &CTrendTests::CPeriodicity::periods(void) const
{
    return m_Periods;
}

double CTrendTests::CPeriodicity::populatedRatio(void) const
{
    double populated = 0.0;
    std::size_t n = m_BucketValues.size();
    if (n > 0)
    {
        for (std::size_t i = 0u; i < n; ++i)
        {
            if (CBasicStatistics::count(m_BucketValues[i]) > 0.0)
            {
                populated += 1.0;
            }
        }
        populated /= static_cast<double>(n);
    }
    return populated;
}

bool CTrendTests::CPeriodicity::seenSufficientData(void) const
{
    return   static_cast<double>(m_BucketValues.size()) * this->populatedRatio()
           > ACCURATE_TEST_POPULATED_FRACTION * static_cast<double>(m_Window / m_BucketLength);
}

CTrendTests::CPeriodicity::CResult CTrendTests::CPeriodicity::test(void) const
{
    // Test whether there are significant trends in the values with either
    // day, week or weekday and weekend patterns.
    //
    // This comprises various variance tests, tests on the amplitude and
    // autocorrelation tests.
    //
    // The variance tests are generally preferred because the test statistics
    // are more predictable, they tend relatively quickly to be F distributed.
    // The amplitude test is used for the case that the variation is mainly
    // due to one large daily or weekly spike. In this case, the influence
    // of the spike on the variance statistics is often small, but its impact
    // on the amplitude is obviously large. We know under the null hypothesis
    // divided by the sample standard deviation this should tend to the minimum
    // to maximum of range number of day, respectively week, buckets of a normal.
    // Autocorrelation is used to sanity check whether the periodic trends are
    // significant w.r.t. the general noise in the values.
    //
    // That said, in all these tests we are not interested just in whether there
    // is definitely a periodic trend, but also whether periodic trends appear
    // to be significant w.r.t. the residual variance. Rather than testing just
    // against a confidence level for the test statistics, which typically become
    // increasingly sensitive as the number of samples per bucket increases,
    // we look at the absolute reduction in variance, or amplitude, due to fitting
    // each type of component. If there is a significant reduction, then we
    // include that period. Note that this strategy can equally be applied when
    // we have decomposed the signal into different periods. In particular,
    // to test whether there is a daily as well as a weekly period we minimize
    // the variance in the weekly component when fitting both periods and then
    // test if the daily variation accounts for a significant component of the
    // total variation.

    CResult result;

    // Preliminaries.
    SStatistics statistics;
    if (!statistics.initialize(m_BucketValues, m_BucketLength,
                               this->windows(E_FullInterval),
                               m_Periods, m_Partition,
                               this->populatedRatio(), this->count()))
    {
        return result;
    }

    LOG_TRACE("short");
    std::size_t period = static_cast<std::size_t>(m_Periods[0] / m_BucketLength);
    bool hasShort =    statistics.canTestForShort(m_BucketLength)
                   && (    this->testComponentUsingUnexplainedVariance(period, statistics)
                       || (this->seenSufficientData() && this->testComponentUsingAmplitude(period, statistics)))
                   && this->testAutocorrelation(period, statistics);
    statistics.commitCandidates(hasShort);

    LOG_TRACE("partitioned");
    bool hasPartition =   statistics.canTestForPartition(m_BucketLength)
                       && this->testForPartitionUsingUnexplainedVariance(statistics);
    statistics.commitCandidates(hasPartition);

    LOG_TRACE("long")
    period = static_cast<std::size_t>(m_Periods[1] / m_BucketLength);
    bool hasLong =    statistics.canTestForLong(m_BucketLength)
                  && (   this->testComponentUsingUnexplainedVariance(period, statistics)
                      || (!hasShort && this->testComponentUsingAmplitude(period, statistics)))
                  && this->testAutocorrelation(period, statistics);
    if (!hasPartition && !hasLong)
    {
        result.addIf(hasShort, E_FullInterval, E_ShortPeriod);
        return result;
    }
    statistics.commitCandidates(hasLong);

    // Decompose the signal and check there is significant variance
    // and autocorrelation in each component we include.

    LOG_TRACE("hasPartition = " << hasPartition);

    CResult candidate;
    candidate.addIf(true, E_FullInterval, E_BothPeriods);
    candidate.addIf(hasPartition, E_FirstInterval, E_BothPeriods);
    candidate.addIf(hasPartition, E_SecondInterval, E_BothPeriods);
    candidate.startOfPartition(statistics.s_StartOfPartition);

    TTimeTimePrMeanVarAccumulatorPrVec trends[6];
    this->trends(candidate, trends);

    if (hasPartition)
    {
        static const EInterval INTERVALS[] = { E_FirstInterval, E_SecondInterval };
        for (std::size_t i = 0u; i < 2; ++i)
        {
            std::size_t shortIndex  = candidate.index(INTERVALS[i], E_ShortPeriod);
            std::size_t longIndex   = candidate.index(INTERVALS[i], E_LongPeriod);
            TTimeTimePr2Vec windows = this->windows(  INTERVALS[i], candidate.startOfPartition());
            result.addIf(this->testComponentUsingExplainedVariance(trends[shortIndex],
                                                                   trends[longIndex],
                                                                   windows, statistics),
                         INTERVALS[i], E_ShortPeriod);
            result.addIf(this->testComponentUsingExplainedVariance(trends[longIndex],
                                                                   trends[shortIndex],
                                                                   windows, statistics),
                         INTERVALS[i], E_LongPeriod);
        }
        if (   (result.periods(E_FirstInterval)  & E_ShortPeriod)
            || (result.periods(E_SecondInterval) & E_ShortPeriod))
        {
            result.startOfPartition(candidate.startOfPartition());
            result.addIf(result.periods(E_FirstInterval)  == E_NoPeriod, E_FirstInterval,  E_LongPeriod);
            result.addIf(result.periods(E_SecondInterval) == E_NoPeriod, E_SecondInterval, E_LongPeriod);
            return result;
        }
    }

    std::size_t shortIndex = candidate.index(E_FullInterval, E_ShortPeriod);
    std::size_t longIndex  = candidate.index(E_FullInterval, E_LongPeriod);
    result = CResult();
    result.addIf(this->testComponentUsingExplainedVariance(trends[shortIndex], trends[longIndex],
                                                           this->windows(E_FullInterval),
                                                           statistics), E_FullInterval, E_ShortPeriod);
    result.addIf(this->testComponentUsingExplainedVariance(trends[longIndex], trends[shortIndex],
                                                           this->windows(E_FullInterval),
                                                           statistics), E_FullInterval, E_LongPeriod);

    return result;
}

void CTrendTests::CPeriodicity::trends(const CResult &periods,
                                       TTimeTimePrMeanVarAccumulatorPrVec (&result)[6]) const
{
    static const EInterval INTERVALS[] =
        {
            E_FirstInterval, E_SecondInterval, E_FullInterval
        };
    for (std::size_t i = 0u; i < boost::size(INTERVALS); ++i)
    {
        this->periodicBucketing(periods.periods(INTERVALS[i]),
                                this->windows(INTERVALS[i], periods.startOfPartition()),
                                result[periods.index(INTERVALS[i], E_ShortPeriod)],
                                result[periods.index(INTERVALS[i], E_LongPeriod)]);
    }
}

uint64_t CTrendTests::CPeriodicity::checksum(uint64_t seed) const
{
    seed = CChecksum::calculate(seed, m_DecayRate);
    seed = CChecksum::calculate(seed, m_BucketLength);
    seed = CChecksum::calculate(seed, m_Window);
    seed = CChecksum::calculate(seed, m_Periods);
    seed = CChecksum::calculate(seed, m_Partition);
    return CChecksum::calculate(seed, m_BucketValues);
}

void CTrendTests::CPeriodicity::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CTrendTests::CPeriodicity");
    core::CMemoryDebug::dynamicSize("m_Periods", m_Periods, mem);
    core::CMemoryDebug::dynamicSize("m_Partition", m_Partition, mem);
    core::CMemoryDebug::dynamicSize("m_BucketValues", m_BucketValues, mem);
}

std::size_t CTrendTests::CPeriodicity::memoryUsage(void) const
{
    return  core::CMemory::dynamicSize(m_Periods)
          + core::CMemory::dynamicSize(m_Partition)
          + core::CMemory::dynamicSize(m_BucketValues);
}

bool CTrendTests::CPeriodicity::testComponentUsingUnexplainedVariance(std::size_t period,
                                                                      SStatistics &statistics) const
{
    double degreesOfFreedom = statistics.s_PopulatedBuckets - static_cast<double>(period);
    double scale = 1.0 / statistics.s_ValuesPerBucket;

    TMeanVarAccumulatorVec trend(period);
    periodicTrend(m_BucketValues, this->windows(E_FullInterval), m_BucketLength, trend);

    statistics.s_CandidateUnexplainedVariance =
            varianceAtPercentile(residualVariance<double>(trend, scale),
                                 degreesOfFreedom,
                                 50.0 + CONFIDENCE_INTERVAL / 2.0);
    statistics.s_CandidateDegreesOfFreedom = degreesOfFreedom;
    LOG_TRACE("  variance          = " << statistics.s_CandidateUnexplainedVariance);
    LOG_TRACE("  varianceThreshold = " << statistics.varianceThreshold());

    return   statistics.s_CandidateUnexplainedVariance <= statistics.varianceThreshold()
          && CStatisticalTests::fTest(statistics.F(),
                                      degreesOfFreedom,
                                      statistics.s_DegreesOfFreedom) <= MAXIMUM_SIGNIFICANCE;
}

bool CTrendTests::CPeriodicity::testComponentUsingExplainedVariance(const TTimeTimePrMeanVarAccumulatorPrVec &trend,
                                                                    const TTimeTimePrMeanVarAccumulatorPrVec &remainderTrend,
                                                                    const TTimeTimePr2Vec &windows,
                                                                    const SStatistics &statistics) const
{
    if (windows.empty())
    {
        return 0.0;
    }

    double varianceThreshold  = 0.75 * statistics.s_ValuesPerBucket
                                     * (1.0 - HAS_PERIOD_VARIANCE_RATIO)
                                     * statistics.s_UnexplainedVariance;
    double amplitudeThreshold = 0.75 * statistics.amplitudeThreshold();
    LOG_TRACE("  varianceThreshold = " << varianceThreshold);
    LOG_TRACE("  amplitudeThreshold = " << amplitudeThreshold);

    std::size_t populatedBuckets = 0u;
    for (std::size_t i = 0u; i < windows.size(); ++i)
    {
        populatedBuckets += (windows[0].second - windows[0].first) / m_BucketLength;
    }
    std::size_t period = trend.size();
    double variance = varianceAtPercentile(trendVariance(trend),
                                           static_cast<double>(populatedBuckets - period),
                                           50.0 + CONFIDENCE_INTERVAL / 2.0);
    LOG_TRACE("  period = " << period);
    LOG_TRACE("  trendVariance = " << variance);
    LOG_TRACE("  trendResidual = " << trendAmplitude(trend));

    if (variance >= varianceThreshold || trendAmplitude(trend) >= amplitudeThreshold)
    {
        double R = autocorrelationAtPercentile(remainderAutocorrelation(period,
                                                                        m_BucketLength,
                                                                        m_BucketValues,
                                                                        remainderTrend, windows),
                                               statistics.s_PopulatedBuckets,
                                               50.0 - CONFIDENCE_INTERVAL / 2.0);
        LOG_TRACE("  autocorrelation = " << R);
        return R > minimumAutocorrelation(variance, varianceThreshold, MINIMUM_AUTOCORRELATION);
    }

    return false;
}

bool CTrendTests::CPeriodicity::testComponentUsingAmplitude(std::size_t period,
                                                            const SStatistics &statistics) const
{
    TMeanVarAccumulatorVec trend(period);
    periodicTrend(m_BucketValues, this->windows(E_FullInterval), m_BucketLength, trend);

    double amplitude          = trendAmplitude(trend);
    LOG_TRACE("  amplitude          = " << amplitude);
    LOG_TRACE("  amplitudeThreshold = " << statistics.amplitudeThreshold());

    return amplitude > statistics.amplitudeThreshold();
}

bool CTrendTests::CPeriodicity::testForPartitionUsingUnexplainedVariance(SStatistics &statistics) const
{
    double degreesOfFreedom =  statistics.s_PopulatedBuckets
                             - static_cast<double>(std::min(m_Partition[0], m_Partition[1]) / m_BucketLength);

    double variance;
    this->partitionVariance(statistics.s_ValuesPerBucket, variance, statistics.s_StartOfPartition);

    statistics.s_CandidateUnexplainedVariance =
            varianceAtPercentile(variance, degreesOfFreedom, 50.0 + CONFIDENCE_INTERVAL / 2.0);
    statistics.s_CandidateDegreesOfFreedom = degreesOfFreedom;
    LOG_TRACE("  variance          = " << statistics.s_CandidateUnexplainedVariance);
    LOG_TRACE("  startOfPartition  = " << statistics.s_StartOfPartition);
    LOG_TRACE("  varianceThreshold = " << statistics.varianceThreshold());

    return   statistics.s_CandidateUnexplainedVariance <=  HAS_PARTITION_VARIANCE_RATIO
                                                         * statistics.s_UnexplainedVariance
          && CStatisticalTests::fTest(statistics.F(),
                                      degreesOfFreedom,
                                      statistics.s_DegreesOfFreedom) <= MAXIMUM_SIGNIFICANCE;
}

bool CTrendTests::CPeriodicity::testAutocorrelation(std::size_t period,
                                                    const SStatistics &statistics) const
{
    double R = autocorrelationAtPercentile(autocorrelation(period, m_BucketValues),
                                           statistics.s_PopulatedBuckets,
                                           50.0 - CONFIDENCE_INTERVAL / 2.0);
    LOG_TRACE("  autocorrelation     = " << R);
    return R > minimumAutocorrelation(statistics.varianceThreshold(),
                                      statistics.s_CandidateUnexplainedVariance,
                                      MINIMUM_AUTOCORRELATION);
}

void CTrendTests::CPeriodicity::partitionVariance(double meanCount,
                                                  double &partitionVariance,
                                                  core_t::TTime &startOfPartition) const
{
    // Find the partition of the data such that the residual variance
    // w.r.t. the short period is minimized.
    //
    // Note that this is implemented O("number buckets in window")
    // by noting that for each shift most of the residual variance
    // doesn't need to be recomputed. We also make use of the fact
    // that it is possible to subtract sample central moment objects.

    typedef std::pair<double, core_t::TTime> TDoubleTimePr;
    typedef std::vector<TDoubleTimePr> TDoubleTimePrVec;
    typedef CBasicStatistics::COrderStatisticsStack<TDoubleTimePr, 1> TMinAccumulator;
    typedef boost::circular_buffer<TMeanVarAccumulator> TMeanVarAccumulatorBuffer;

    TTimeTimePr2Vec windows[] =
        {
            this->windows(E_FirstInterval, 0), this->windows(E_SecondInterval, 0)
        };
    LOG_TRACE("windows = " << core::CContainerPrinter::print(windows));

    TTimeVec deltas[2];
    deltas[0].reserve(static_cast<std::size_t>(  (m_Partition[0] * m_Window)
                                               / (m_Periods[0] * m_Periods[1])));
    deltas[1].reserve(static_cast<std::size_t>(  (m_Partition[1] * m_Window)
                                               / (m_Periods[0] * m_Periods[1])));
    for (std::size_t i = 0u; i < 2; ++i)
    {
        for (std::size_t j = 0u; j < windows[i].size(); ++j)
        {
            core_t::TTime start = windows[i][j].first;
            core_t::TTime end   = windows[i][j].second;
            for (core_t::TTime t = start + m_Periods[0]; t <= end; t += m_Periods[0])
            {
                deltas[i].push_back(t - m_BucketLength);
            }
        }
    }
    LOG_TRACE("deltas = " << core::CContainerPrinter::print(deltas));

    std::size_t n = static_cast<std::size_t>(m_Periods[0] / m_BucketLength);
    TMeanVarAccumulatorBuffer trends[2] =
        {
            TMeanVarAccumulatorBuffer(n, TMeanVarAccumulator()),
            TMeanVarAccumulatorBuffer(n, TMeanVarAccumulator())
        };
    periodicTrend(m_BucketValues, windows[0], m_BucketLength, trends[0]);
    periodicTrend(m_BucketValues, windows[1], m_BucketLength, trends[1]);
    LOG_TRACE("n = " << n);

    double scale = 1.0 / meanCount;
    LOG_TRACE("scale = " << scale);
    TMeanAccumulator variances[] =
        {
            residualVariance<TMeanAccumulator>(trends[0], scale),
            residualVariance<TMeanAccumulator>(trends[1], scale)
        };
    LOG_TRACE("variances = " << core::CContainerPrinter::print(variances));

    TMinAccumulator minimum;
    minimum.add(std::make_pair((  residualVariance(variances[0])
                                + residualVariance(variances[1])) / 2.0, 0));
    TDoubleTimePrVec candidates;
    candidates.reserve(n);

    for (core_t::TTime time = m_BucketLength; time < m_Periods[1]; time += m_BucketLength)
    {
        for (std::size_t i = 0u; i < 2; ++i)
        {
            for (std::size_t j = 0u; j < deltas[i].size(); ++j)
            {
                core_t::TTime t = deltas[i][j] + m_BucketLength;
                deltas[i][j] = t == m_Window ? 0 : t;
            }
            TMeanVarAccumulator oldBucket = trends[i][0];
            trends[i].pop_front();
            TMeanVarAccumulator newBucket;
            averageValue(m_BucketValues, deltas[i], m_BucketLength, newBucket);
            trends[i].push_back(newBucket);
            variances[i] -= residualVariance(oldBucket, scale);
            variances[i] += residualVariance(newBucket, scale);
        }
        double variance = (  residualVariance(variances[0])
                           + residualVariance(variances[1])) / 2.0;
        minimum.add(std::make_pair(variance, time));
        if (variance < 1.05 * minimum[0].first)
        {
            candidates.push_back(std::make_pair(variance, time));
        }
    }

    TMinAccumulator lowest;
    for (std::size_t i = 0u; i < candidates.size(); ++i)
    {
        if (candidates[i].first < 1.05 * minimum[0].first)
        {
            core_t::TTime time = candidates[i].second;
            std::size_t j = static_cast<std::size_t>(time / m_BucketLength);
            lowest.add(std::make_pair(::fabs(CBasicStatistics::mean(m_BucketValues[j])), time));
        }
    }

    partitionVariance = minimum[0].first;
    startOfPartition  = lowest[0].second;
}

double CTrendTests::CPeriodicity::count(void) const
{
    double result = 0.0;
    for (std::size_t i = 0u; i < m_BucketValues.size(); ++i)
    {
        result += CBasicStatistics::count(m_BucketValues[i]);
    }
    return result;
}

void CTrendTests::CPeriodicity::periodicBucketing(int periods,
                                                  const TTimeTimePr2Vec &windows,
                                                  TTimeTimePrMeanVarAccumulatorPrVec &shortTrend,
                                                  TTimeTimePrMeanVarAccumulatorPrVec &longTrend) const
{
    LOG_TRACE("periods = " << periods);
    switch (periods)
    {
    case E_ShortPeriod: this->periodicBucketing(m_Periods[0], windows, shortTrend); break;
    case E_LongPeriod:  this->periodicBucketing(m_Periods[1], windows, longTrend);  break;
    case E_BothPeriods: this->periodicBucketing(windows, shortTrend, longTrend);    break;
    case E_NoPeriod:                                                                break;
    }
}

void CTrendTests::CPeriodicity::periodicBucketing(core_t::TTime period_,
                                                  const TTimeTimePr2Vec &windows,
                                                  TTimeTimePrMeanVarAccumulatorPrVec &trend) const
{
    trend.clear();

    if (windows.empty())
    {
        return;
    }

    period_ = std::min(period_, windows[0].second - windows[0].first);
    std::size_t period = static_cast<std::size_t>(period_ / m_BucketLength);

    initializeBuckets(period, windows, trend);
    TMeanAccumulatorVec varianceScales(trend.size());
    std::size_t length = m_BucketValues.size();
    for (std::size_t i = 0u, j = 0u; i < windows.size(); ++i)
    {
        std::size_t a = static_cast<std::size_t>(windows[i].first  / m_BucketLength);
        std::size_t b = static_cast<std::size_t>(windows[i].second / m_BucketLength);
        for (std::size_t k = a; k < b; ++j, ++k)
        {
            const TFloatMeanAccumulator &bucket = m_BucketValues[k % length];
            double count = CBasicStatistics::count(bucket);
            double mean  = CBasicStatistics::mean(bucket);
            if (count > 0.0)
            {
                std::size_t pj = j % period;
                trend[pj].second.add(mean, count);
                varianceScales[pj].add(1.0 / count);
            }
        }
    }
    for (std::size_t i = 0u; i < trend.size(); ++i)
    {
        trend[i].second.s_Moments[1] /= CBasicStatistics::mean(varianceScales[i]);
    }
}

void CTrendTests::CPeriodicity::periodicBucketing(const TTimeTimePr2Vec &windows,
                                                  TTimeTimePrMeanVarAccumulatorPrVec &shortTrend,
                                                  TTimeTimePrMeanVarAccumulatorPrVec &longTrend) const
{
    // For periods P1 and P2 then, given the window is a whole number
    // of P2, we have that
    //   x(i) = d(i mod P2, j) * m2'(j) + d(i mod P1, j) * m1'(j)
    //
    // where d(.) denotes the Kronecker delta, and m1' and m2' are the
    // adjusted periodic baselines, respectively. This gives an over
    // determined matrix equation which can be solved using the standard
    // least squares approach, i.e. using the Moore-Penrose pseudo-inverse.
    // There is one complication which is that the matrix is singular.
    // This is because there is a degeneracy among possible solutions.
    // Specifically, if we add c(j) to m1'(j) and subtract c(j) from
    // m(j + k * D) we end up with the same total baseline. One strategy
    // is to add eps * I and let eps -> 0 which gives a well defined linear
    // combination of {x(i)} to compute the mean, i.e. for the j'th bucket
    // in period P2
    //   m1'(j) = (N/N1) / (N/N1 + N/N2) * m1(j)
    //   m2'(j) = m2(j) - (N/N1) / (N/N1 + N/N2) * m1(j mod n1)
    //
    // Since we have lower resolution to model m2' we prefer to subsequently
    // adjust m1' to make m2' as smooth as possible.

    shortTrend.clear();
    longTrend.clear();

    if (windows.empty())
    {
        return;
    }

    core_t::TTime window = 0;
    for (std::size_t i = 0u; i < windows.size(); ++i)
    {
        window += windows[i].second - windows[i].first;
    }
    core_t::TTime w0 = windows[0].second - windows[0].first;
    core_t::TTime shortPeriod_ = std::min(m_Periods[0], w0);
    core_t::TTime longPeriod_  = std::min(m_Periods[1], w0);
    std::size_t shortPeriod = static_cast<std::size_t>(shortPeriod_ / m_BucketLength);
    std::size_t longPeriod  = static_cast<std::size_t>(longPeriod_ / m_BucketLength);
    std::size_t length = m_BucketValues.size();
    double S = static_cast<double>(window / shortPeriod_);
    double L = static_cast<double>(window / longPeriod_);
    double scale = S / (S + L);

    TMeanAccumulatorVec trends[] =
        {
            TMeanAccumulatorVec(shortPeriod),
            TMeanAccumulatorVec(longPeriod)
        };
    periodicTrend(m_BucketValues, windows, m_BucketLength, trends[0]);
    periodicTrend(m_BucketValues, windows, m_BucketLength, trends[1]);

    for (std::size_t i = 0u; i < trends[0].size(); ++i)
    {
        trends[0][i].s_Moments[0] *= scale;
    }
    for (std::size_t i = 0u; i < trends[1].size(); /**/)
    {
        for (std::size_t j = 0u; i < trends[1].size() && j < trends[0].size(); ++i, ++j)
        {
            TMeanAccumulator &bucket = trends[1][i];
            if (CBasicStatistics::count(bucket) > 0.0)
            {
                bucket.s_Moments[0] -= CBasicStatistics::mean(trends[0][j]);
            }
        }
    }

    TMeanAccumulatorVec shifts(shortPeriod);
    for (std::size_t i = 0u; i < trends[1].size(); /**/)
    {
        for (std::size_t j = 0u; i < trends[1].size() && j < shifts.size(); ++i, ++j)
        {
            const TMeanAccumulator &bucket = trends[1][i];
            if (CBasicStatistics::count(bucket) > 0.0)
            {
                shifts[j].add(CBasicStatistics::mean(bucket));
            }
        }
    }
    for (std::size_t i = 0u; i < trends[0].size(); ++i)
    {
        double shift = CBasicStatistics::mean(shifts[i]);
        if (shift != 0.0)
        {
            trends[0][i].s_Moments[0] += shift;
            for (std::size_t j = 0u; j < trends[1].size(); j += trends[0].size())
            {
                TMeanAccumulator &bucket = trends[1][i + j];
                if (CBasicStatistics::count(bucket) > 0.0)
                {
                    bucket.s_Moments[0] -= shift;
                }
            }
        }
    }

    initializeBuckets(shortPeriod, windows, shortTrend);
    initializeBuckets(longPeriod, windows, longTrend);
    TMeanAccumulatorVec varianceScales(shortTrend.size());
    for (std::size_t i = 0u, j = 0u, k = 0u; i < windows.size(); ++i)
    {
        std::size_t a = static_cast<std::size_t>(windows[i].first  / m_BucketLength);
        std::size_t b = static_cast<std::size_t>(windows[i].second / m_BucketLength);
        for (std::size_t l = a; l < b; ++j, ++k, ++l)
        {
            const TFloatMeanAccumulator &bucket = m_BucketValues[l % length];
            double count = CBasicStatistics::count(bucket);
            double mean  = CBasicStatistics::mean(bucket);
            if (count > 0.0)
            {
                std::size_t pj = j % shortPeriod;
                std::size_t pk = k % longPeriod;
                shortTrend[pj].second.add(mean - CBasicStatistics::mean(trends[1][pk]), count);
                varianceScales[pj].add(1.0 / count);
            }
        }
    }
    for (std::size_t i = 0u; i < shortTrend.size(); ++i)
    {
        shortTrend[i].second.s_Moments[1] /= CBasicStatistics::mean(varianceScales[i]);
    }
    for (std::size_t i = 0u; i < trends[1].size(); ++i)
    {
        longTrend[i].second.add(CBasicStatistics::mean(trends[1][i]));
    }
}

CTrendTests::CPeriodicity::TTimeTimePr2Vec CTrendTests::CPeriodicity::windows(EInterval interval,
                                                                              core_t::TTime start) const
{
    switch (interval)
    {
    case E_FullInterval:
        return TTimeTimePr2Vec(1, TTimeTimePr(0, m_Window));
    case E_FirstInterval:
        return computeWindows(start, start + m_Window, m_Partition[0], m_Periods[1]);
    case E_SecondInterval:
        return computeWindows(start + m_Partition[0], start + m_Window, m_Partition[1], m_Periods[1]);
    }
    return TTimeTimePr2Vec(1, TTimeTimePr(0, m_Window));
}

void CTrendTests::CPeriodicity::initializeBuckets(std::size_t period,
                                                  const TTimeTimePr2Vec &windows,
                                                  TTimeTimePrMeanVarAccumulatorPrVec &trend) const
{
    trend.resize(period);
    core_t::TTime bucket = windows[0].first;
    for (std::size_t i = 0u; i < trend.size(); ++i, bucket += m_BucketLength)
    {
        trend[i].first = std::make_pair(bucket, bucket + m_BucketLength);
    }
}

const TFloatMeanAccumulatorVec CTrendTests::CPeriodicity::NO_BUCKET_VALUES;
const double CTrendTests::CPeriodicity::ACCURATE_TEST_POPULATED_FRACTION = 0.9;
const double CTrendTests::CPeriodicity::MINIMUM_COEFFICIENT_OF_VARIATION = 1e-4;
const double CTrendTests::CPeriodicity::HAS_PERIOD_VARIANCE_RATIO = 0.7;
const double CTrendTests::CPeriodicity::HAS_PARTITION_VARIANCE_RATIO = 0.5;
const double CTrendTests::CPeriodicity::HAS_PERIOD_AMPLITUDE_IN_SDS = 1.0;
const double CTrendTests::CPeriodicity::MINIMUM_AUTOCORRELATION = 0.5;

CTrendTests::CPeriodicity::CResult::CResult(void) :
        m_StartOfPartition(0),
        m_Periods(0)
{}

bool CTrendTests::CPeriodicity::CResult::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string name = traverser.name();
        RESTORE_BUILT_IN(START_OF_PARTITION_TAG, m_StartOfPartition)
        RESTORE_BUILT_IN(HAS_PERIODS_TAG, m_Periods)
    }
    while (traverser.next());
    return true;
}

void CTrendTests::CPeriodicity::CResult::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertValue(START_OF_PARTITION_TAG, m_StartOfPartition);
    inserter.insertValue(HAS_PERIODS_TAG, m_Periods);
}

bool CTrendTests::CPeriodicity::CResult::operator==(const CResult &other) const
{
    return m_StartOfPartition == other.m_StartOfPartition && m_Periods == other.m_Periods;
}

const CTrendTests::CPeriodicity::CResult &CTrendTests::CPeriodicity::CResult::operator+=(const CResult &other)
{
    if (m_StartOfPartition == other.m_StartOfPartition)
    {
        m_Periods = m_Periods | other.m_Periods;
    }
    return *this;
}

void CTrendTests::CPeriodicity::CResult::addIf(bool hasPeriod, EInterval interval, EPeriod period)
{
    if (hasPeriod)
    {
        m_Periods += static_cast<int>(period) << 2 * static_cast<int>(interval);
    }
}

void CTrendTests::CPeriodicity::CResult::startOfPartition(core_t::TTime time)
{
    m_StartOfPartition = time;
}

core_t::TTime CTrendTests::CPeriodicity::CResult::startOfPartition(void) const
{
    return m_StartOfPartition;
}

bool CTrendTests::CPeriodicity::CResult::periodic(void) const
{
    return m_Periods != E_NoPeriod;
}

CTrendTests::CPeriodicity::EPeriod CTrendTests::CPeriodicity::CResult::periods(EInterval interval) const
{
    return static_cast<EPeriod>((m_Periods >> (2 * static_cast<int>(interval))) & 0x3);
}

std::size_t CTrendTests::CPeriodicity::CResult::index(EInterval interval, EPeriod period) const
{
    return 2 * static_cast<int>(interval) + static_cast<int>(period) - 1;
}

std::string CTrendTests::CPeriodicity::CResult::print(const std::string (&intervals)[2],
                                                      const std::string (&periods)[2]) const
{
    std::string result = "{";
    result += (this->periods(E_FullInterval) & E_ShortPeriod) ?
              std::string(" '")  + periods[0] + "'" : std::string();
    result += (this->periods(E_FullInterval) & E_LongPeriod) ?
              std::string(" '")  + periods[1] + "'": std::string();
    result += (this->periods(E_FirstInterval) & E_ShortPeriod) ?
              std::string(" '")  + intervals[0] + " " + periods[0] + "'" : std::string();
    result += (this->periods(E_FirstInterval) & E_LongPeriod) ?
              std::string(" '")  + intervals[0] + " " + periods[1] + "'" : std::string();
    result += (this->periods(E_SecondInterval) & E_ShortPeriod) ?
              std::string(" '")  + intervals[1] + " " + periods[0] + "'" : std::string();
    result += (this->periods(E_SecondInterval) & E_LongPeriod) ?
              std::string(" '")  + intervals[1] + " " + periods[1] + "'" : std::string();
    result += " }";
    return result;
}

uint64_t CTrendTests::CPeriodicity::CResult::checksum(void) const
{
    return (m_Periods + 1) * m_StartOfPartition + m_Periods;
}

bool CTrendTests::CPeriodicity::SStatistics::initialize(const TFloatMeanAccumulatorVec &values,
                                                        core_t::TTime bucketLength,
                                                        const TTimeTimePr2Vec &windows,
                                                        const TTime2Vec &periods,
                                                        const TTime2Vec &partition,
                                                        double populated, double count)
{
    LOG_TRACE("populated = " << 100.0 * populated << "%");

    core_t::TTime window = (windows[0].second - windows[0].first);
    s_Periods = periods;
    s_BucketsPerShort = static_cast<double>(periods[0] / bucketLength);
    s_BucketsPerLong  = static_cast<double>(periods[1] / bucketLength);
    s_PopulatedBuckets = static_cast<double>(window / bucketLength) * populated;
    s_ValuesPerBucket  = count / s_PopulatedBuckets;
    LOG_TRACE("populatedBuckets = " << s_PopulatedBuckets
              << ", valuesPerBucket = " << s_ValuesPerBucket);

    if (s_PopulatedBuckets <= 2.0)
    {
        return false;
    }

    TMeanVarAccumulatorVec trend(1);
    periodicTrend(values, windows, bucketLength, trend);
    double mean = CBasicStatistics::mean(trend[0]);
    s_UnexplainedVariance = CBasicStatistics::variance(trend[0]);
    LOG_TRACE("mean = " << mean);
    LOG_TRACE("variance = " << s_UnexplainedVariance);
    if (s_UnexplainedVariance <= MINIMUM_COEFFICIENT_OF_VARIATION * mean)
    {
        return false;
    }
    s_UnexplainedVariance = varianceAtPercentile(s_UnexplainedVariance,
                                                 s_PopulatedBuckets - 1.0,
                                                 50.0 + CONFIDENCE_INTERVAL / 2.0);
    s_DegreesOfFreedom = s_PopulatedBuckets - 1.0;
    s_StartOfPartition = 0;
    s_ShortestInterval = partition.empty() ? 0 : *std::min_element(partition.begin(), partition.end());
    return true;
}

void CTrendTests::CPeriodicity::SStatistics::commitCandidates(bool commit)
{
    if (commit)
    {
        s_UnexplainedVariance = s_CandidateUnexplainedVariance;
        s_DegreesOfFreedom    = s_CandidateDegreesOfFreedom;
    }
}

bool CTrendTests::CPeriodicity::SStatistics::canTestForShort(core_t::TTime bucketLength) const
{
    return bucketLength <= s_Periods[0] / 4 && s_PopulatedBuckets > 2.9 * s_BucketsPerShort;
}

bool CTrendTests::CPeriodicity::SStatistics::canTestForPartition(core_t::TTime bucketLength) const
{
    return bucketLength <= s_ShortestInterval / 2 && s_PopulatedBuckets > 1.8 * s_BucketsPerLong;
}

bool CTrendTests::CPeriodicity::SStatistics::canTestForLong(core_t::TTime bucketLength) const
{
    return bucketLength <= s_Periods[1] / 4 && s_PopulatedBuckets > 1.8 * s_BucketsPerLong;
}

double CTrendTests::CPeriodicity::SStatistics::F(void) const
{
    return s_CandidateUnexplainedVariance / s_UnexplainedVariance;
}

double CTrendTests::CPeriodicity::SStatistics::amplitudeThreshold(void) const
{
    return HAS_PERIOD_AMPLITUDE_IN_SDS * ::sqrt(s_ValuesPerBucket * s_UnexplainedVariance);
}

double CTrendTests::CPeriodicity::SStatistics::varianceThreshold(void) const
{
    return HAS_PERIOD_VARIANCE_RATIO * s_UnexplainedVariance;
}

//////// CScanningPeriodicity ////////

CTrendTests::CScanningPeriodicity::CScanningPeriodicity(std::size_t size, core_t::TTime bucketLength) :
        m_BucketLength(bucketLength),
        m_StartTime(boost::numeric::bounds<core_t::TTime>::lowest()),
        m_BucketValues(size % 2 == 0 ? size : size + 1)
{}

void CTrendTests::CScanningPeriodicity::swap(CScanningPeriodicity &other)
{
    std::swap(m_BucketLength, other.m_BucketLength);
    std::swap(m_StartTime, other.m_StartTime);
    m_BucketValues.swap(other.m_BucketValues);
}

bool CTrendTests::CScanningPeriodicity::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        RESTORE_BUILT_IN(BUCKET_LENGTH_TAG, m_BucketLength)
        RESTORE_BUILT_IN(START_TIME_TAG, m_StartTime)
        RESTORE(BUCKET_VALUE_TAG, CBasicStatistics::restoreSampleCentralMoments(traverser, m_BucketValues));
    }
    while (traverser.next());
    return true;
}

void CTrendTests::CScanningPeriodicity::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertValue(BUCKET_LENGTH_TAG, m_BucketLength);
    inserter.insertValue(START_TIME_TAG, m_StartTime);
    CBasicStatistics::persistSampleCentralMoments(m_BucketValues, BUCKET_VALUE_TAG, inserter);
}

void CTrendTests::CScanningPeriodicity::initialize(core_t::TTime time)
{
    m_StartTime = time;
}

void CTrendTests::CScanningPeriodicity::add(core_t::TTime time, double value, double weight)
{
    if (this->needToCompress(time))
    {
        std::size_t n = m_BucketValues.size();
        for (std::size_t i = 0u, j = 0u; i < n; i += 2, ++j)
        {
            m_BucketValues[j] = m_BucketValues[i] + m_BucketValues[i + 1];
        }
        std::fill_n(m_BucketValues.begin() + n / 2, n / 2, TFloatMeanAccumulator());
        m_BucketLength *= 2;
    }
    m_BucketValues[(time - m_StartTime) / m_BucketLength].add(value, weight);
}

bool CTrendTests::CScanningPeriodicity::needToCompress(core_t::TTime time) const
{
    return time >= m_StartTime + static_cast<core_t::TTime>(m_BucketValues.size()) * m_BucketLength;
}

CTrendTests::CScanningPeriodicity::TPeriodicityResultPr CTrendTests::CScanningPeriodicity::test(void) const
{
    typedef std::pair<double, std::size_t> TDoubleSizePr;
    typedef CBasicStatistics::COrderStatisticsStack<TDoubleSizePr, 5, std::greater<TDoubleSizePr> > TMaxAccumulator;

    // Compute the linear autocorrelations padding to the maximum offset
    // to avoid windowing effects.

    std::size_t n = m_BucketValues.size();
    std::size_t pad = n / 3;

    TFloatMeanAccumulatorVec values(m_BucketValues);

    TDoubleVec correlations;
    values.resize(n + pad);
    autocorrelations(values, correlations);
    values.resize(n);

    // We retain the top 5 linear autocorrelations, averaging over offsets
    // which are integer multiples of the period since these should have
    // high autocorrelation if the signal is periodic, so we have a high
    // chance of finding the highest cyclic autocorrelation.

    TMaxAccumulator candidates;
    correlations.resize(pad);
    for (std::size_t p = 4; p < correlations.size(); ++p)
    {
        double correlation = this->meanForPeriodicOffsets(correlations, p);
        LOG_TRACE("correlation(" << p << ") = " << correlation);
        candidates.add(std::make_pair(correlation, p));
    }

    std::size_t periods_[2];
    double correlation = -1.0;

    std::size_t candidatePeriods[5];
    for (std::size_t i = 0u; i < candidates.count(); ++i)
    {
        candidatePeriods[i] = candidates[i].second;
    }
    candidates.clear();
    for (std::size_t i = 0u; i < 5; ++i)
    {
        std::size_t p = candidatePeriods[i];
        this->resize(n - n % p, values);
        candidates.add(std::make_pair(autocorrelation(p, values), p));
    }
    candidates.sort();
    LOG_TRACE("candidate periods = " << candidates.print());
    periods_[0] = candidates[0].second;

    // Find the highest autocorrelation harmonic of the base period.

    candidates.clear();
    for (std::size_t divisor = 2; 4 * divisor <= periods_[0]; ++divisor)
    {
        if (periods_[0] % divisor == 0)
        {
            std::size_t p = periods_[0] / divisor;
            candidates.add(std::make_pair(autocorrelation(p, values), p));
        }
    }
    candidates.sort();
    LOG_TRACE("candidate short periods = " << candidates.print());
    if (candidates.count() > 0)
    {
        periods_[1] = candidates[0].second;
        correlation = candidates[0].first;
    }

    // Find the highest autocorrelation multiple of the base period.

    TMeanAccumulatorVec trend(periods_[0]);
    {
        std::size_t w = n - n % periods_[0];
        this->resize(w, values);
        periodicTrend(values, TSizeSizePr2Vec(1, TSizeSizePr(0, w)), trend);
    }
    LOG_TRACE("periodic trend = " << core::CContainerPrinter::print(values));
    candidates.clear();
    candidates.add(std::make_pair(-1.0, 2 * periods_[0]));
    for (std::size_t p = 2 * periods_[0]; 2 * p <= values.size(); p += periods_[0])
    {
        std::size_t w = n - n % p;
        this->resize(w, values);
        core_t::TTime window = static_cast<core_t::TTime>(w) * m_BucketLength;
        candidates.add(std::make_pair(
                remainderAutocorrelation(p, m_BucketLength, values, trend,
                                         TTimeTimePr2Vec(1, TTimeTimePr(0, window))), p));
    }
    candidates.sort();
    LOG_TRACE("candidate long periods = " << candidates.print());
    if (candidates[0].first >= correlation)
    {
        periods_[1] = candidates[0].second;
    }

    // Configure the full periodicity test.

    if (periods_[1] < periods_[0])
    {
        std::swap(periods_[0], periods_[1]);
    }
    TTime2Vec periods(2);
    periods[0] = static_cast<core_t::TTime>(periods_[0]) * m_BucketLength;
    periods[1] = static_cast<core_t::TTime>(periods_[1]) * m_BucketLength;
    core_t::TTime window = static_cast<core_t::TTime>(
            (n - n % (n > 2 * periods_[1] ? periods_[1] : periods_[0]))) * m_BucketLength;
    LOG_TRACE("bucket length = " << m_BucketLength
              << ", window = " << window
              << ", periods to test = " << core::CContainerPrinter::print(periods)
              << ", # values = " << values.size())
    CPeriodicity test;
    test.initialize(m_BucketLength, window, periods, TTime2Vec(), values);

    return std::make_pair(test, test.test());
}

uint64_t CTrendTests::CScanningPeriodicity::checksum(uint64_t seed) const
{
    seed = CChecksum::calculate(seed, m_BucketLength);
    seed = CChecksum::calculate(seed, m_StartTime);
    return CChecksum::calculate(seed, m_BucketValues);
}

void CTrendTests::CScanningPeriodicity::resize(std::size_t size, TFloatMeanAccumulatorVec &values) const
{
    std::size_t n = values.size();
    values.resize(size);
    for (std::size_t i = n; i < size; ++i)
    {
        values[i] = m_BucketValues[i];
    }
}

double CTrendTests::CScanningPeriodicity::meanForPeriodicOffsets(const TDoubleVec &correlations,
                                                                 std::size_t period) const
{

    TMeanAccumulator result;
    for (std::size_t offset = period; offset < correlations.size(); offset += period)
    {
        result.add(this->correctForPad(correlations[offset], offset));
    }
    return CBasicStatistics::mean(result);
}

double CTrendTests::CScanningPeriodicity::correctForPad(double correlation, std::size_t offset) const
{
    return correlation * static_cast<double>(m_BucketValues.size())
                       / static_cast<double>(m_BucketValues.size() - offset);
}


CTrendTests::CPeriodicity *CTrendTests::dailyAndWeekly(core_t::TTime bucketLength, double decayRate)
{
    static const core_t::TTime PERIODS[]   = { DAY, WEEK };
    static const core_t::TTime PARTITION[] = { WEEKEND, WEEKDAYS };
    static const core_t::TTime PERMITTED_BUCKET_LENGTHS[] =
        {
            60, 300, 1800, 3600, 7200, 10800, 14400, 21600, 28800, 43200, 86400, 172800
        };

    std::size_t n = boost::size(PERMITTED_BUCKET_LENGTHS);
    if (bucketLength > PERMITTED_BUCKET_LENGTHS[n - 1])
    {
        return 0;
    }
    bucketLength = *std::lower_bound(PERMITTED_BUCKET_LENGTHS,
                                     PERMITTED_BUCKET_LENGTHS + n, bucketLength);

    CPeriodicity *result = new CPeriodicity(decayRate);
    core_t::TTime window = 2 * WEEK * (std::max(bucketLength, HOUR) / HOUR);
    TTime2Vec periods(PERIODS, PERIODS + 2);
    TTime2Vec partition(PARTITION, PARTITION + 2);
    result->initialize(std::max(bucketLength, HOUR), window, periods, partition);
    return result;
}

std::string CTrendTests::printDailyAndWeekly(const CPeriodicity::CResult &result)
{
    return result.print(WEEKEND_WEEKDAY_NAMES, DAILY_WEEKLY_NAMES);
}

double CTrendTests::autocorrelation(std::size_t offset, const TFloatMeanAccumulatorVec &values)
{
    std::size_t n = values.size();

    TMeanVarAccumulator moments;
    for (std::size_t i = 0u; i < values.size(); ++i)
    {
        if (CBasicStatistics::count(values[i]) > 0.0)
        {
            moments.add(CBasicStatistics::mean(values[i]));
        }
    }

    TMeanAccumulator autocorrelation;
    double mean = CBasicStatistics::mean(moments);
    for (std::size_t i = 0u; i < values.size(); ++i)
    {
        std::size_t j = (i + offset) % n;
        if (   CBasicStatistics::count(values[i]) > 0.0
            && CBasicStatistics::count(values[j]) > 0.0)
        {
            autocorrelation.add(  (CBasicStatistics::mean(values[i]) - mean)
                                * (CBasicStatistics::mean(values[j]) - mean));
        }
    }

    return CBasicStatistics::mean(autocorrelation) / CBasicStatistics::variance(moments);
}

void CTrendTests::autocorrelations(const TFloatMeanAccumulatorVec &values, TDoubleVec &result)
{
    if (values.empty())
    {
        return;
    }

    std::size_t n = values.size();

    TMeanVarAccumulator moments;
    for (std::size_t i = 0u; i < n; ++i)
    {
        if (CBasicStatistics::count(values[i]) > 0.0)
        {
            moments.add(CBasicStatistics::mean(values[i]));
        }
    }
    double mean = CBasicStatistics::mean(moments);
    double variance = CBasicStatistics::variance(moments);

    CSignal::TComplexVec f;
    f.reserve(n);
    for (std::size_t i = 0u; i < n; ++i)
    {
        std::size_t j = i;
        for (/**/; j < n && CBasicStatistics::count(values[j]) == 0; ++j);
        if (i != j)
        {
            // Infer missing values by linearly interpolating.
            if (j == n)
            {
                f.resize(n, CSignal::TComplex(0.0, 0.0));
                break;
            }
            else if (i == 0)
            {
                f.resize(j - 1, CSignal::TComplex(0.0, 0.0));
            }
            else
            {
                for (std::size_t k = i; k < j; ++k)
                {
                    double alpha = static_cast<double>(k - i + 1) / static_cast<double>(j - i + 1);
                    double real  = CBasicStatistics::mean(values[j]) - mean;
                    f.push_back((1.0 - alpha) * f[i-1] + alpha * CSignal::TComplex(real, 0.0));
                }
            }
            i = j;
        }
        f.push_back(CSignal::TComplex(CBasicStatistics::mean(values[i]) - mean, 0.0));
    }

    CSignal::fft(f);
    CSignal::TComplexVec fConj(f);
    CSignal::conj(fConj);
    CSignal::hadamard(fConj, f);
    CSignal::ifft(f);

    result.reserve(n);
    for (std::size_t i = 1u; i < n; ++i)
    {
        result.push_back(f[i].real() / variance / static_cast<double>(n));
    }
}

const CTrendTests::CPeriodicity::EPeriod CTrendTests::SELECT_DAILY = CTrendTests::CPeriodicity::E_ShortPeriod;
const CTrendTests::CPeriodicity::EPeriod CTrendTests::SELECT_WEEKLY = CTrendTests::CPeriodicity::E_LongPeriod;
const CTrendTests::CPeriodicity::EInterval CTrendTests::SELECT_WEEK = CTrendTests::CPeriodicity::E_FullInterval;
const CTrendTests::CPeriodicity::EInterval CTrendTests::SELECT_WEEKEND = CTrendTests::CPeriodicity::E_FirstInterval;
const CTrendTests::CPeriodicity::EInterval CTrendTests::SELECT_WEEKDAYS = CTrendTests::CPeriodicity::E_SecondInterval;
const std::string CTrendTests::DAILY_WEEKLY_NAMES[2] = { "daily", "weekly" };
const std::string CTrendTests::WEEKEND_WEEKDAY_NAMES[2] = { "weekend", "weekdays" };

}
}
