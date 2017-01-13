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

#include <maths/CSeasonalComponentAdaptiveBucketing.h>

#include <core/CContainerPrinter.h>
#include <core/CHashing.h>
#include <core/CLogger.h>
#include <core/CPersistUtils.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>
#include <core/CStringUtils.h>
#include <core/RestoreMacros.h>

#include <maths/CChecksum.h>
#include <maths/COrderings.h>
#include <maths/CRegression.h>
#include <maths/CSeasonalTime.h>
#include <maths/CTools.h>

#include <boost/range.hpp>

#include <algorithm>
#include <cstddef>
#include <numeric>
#include <utility>
#include <vector>

namespace ml
{
namespace maths
{
namespace
{

typedef CSeasonalComponentAdaptiveBucketing::TRegression TRegression;

const double MINIMUM_AGE_TO_PREDICT = 2.5;
const double MINIMUM_DECAY_RATE(0.001);

//! Clear a vector and recover its memory.
template<typename T>
void clearAndShrink(std::vector<T> &vector)
{
    std::vector<T> empty;
    empty.swap(vector);
}

//! Get the predicted value of \p r at \p t.
double predict(const TRegression &r, double t, double age)
{
    if (age < MINIMUM_AGE_TO_PREDICT)
    {
        return r.mean();
    }
    return CRegression::predict(r, t);
}

//! Compute values scaled appropriately for \p period.
double scale(double x, core_t::TTime period)
{
    double scale =  static_cast<double>(CSeasonalComponentAdaptiveBucketing::timescale())
                  / static_cast<double>(period);
    return x * std::min(1.0, scale);
}

const std::string TIME_TAG("a");
const std::string INITIAL_TIME_TAG("b");
const std::string DECAY_RATE_TAG("c");
const std::string ENDPOINT_TAG("d");
const std::string REGRESSION_TAG("e");
const std::string VARIANCES_TAG("f");
const std::string LP_FORCE_TAG("g");
const std::string FORCE_TAG("h");
const std::string EMPTY_STRING;

}

CSeasonalComponentAdaptiveBucketing::CSeasonalComponentAdaptiveBucketing(void) :
        m_InitialTime(boost::numeric::bounds<core_t::TTime>::lowest()),
        m_DecayRate(MINIMUM_DECAY_RATE),
        m_MinimumBucketLength(0.0)
{}

CSeasonalComponentAdaptiveBucketing::CSeasonalComponentAdaptiveBucketing(const CSeasonalTime &time,
                                                                         double decayRate,
                                                                         double minimumBucketLength) :
        m_Time(time.clone()),
        m_InitialTime(boost::numeric::bounds<core_t::TTime>::lowest()),
        m_DecayRate(std::max(decayRate, MINIMUM_DECAY_RATE)),
        m_MinimumBucketLength(minimumBucketLength)
{}

CSeasonalComponentAdaptiveBucketing::CSeasonalComponentAdaptiveBucketing(const CSeasonalComponentAdaptiveBucketing &other) :
        m_Time(other.m_Time->clone()),
        m_InitialTime(other.m_InitialTime),
        m_DecayRate(other.m_DecayRate),
        m_MinimumBucketLength(other.m_MinimumBucketLength),
        m_Endpoints(other.m_Endpoints),
        m_Regressions(other.m_Regressions),
        m_Variances(other.m_Variances),
        m_LpForce(other.m_LpForce),
        m_Force(other.m_Force)
{}

CSeasonalComponentAdaptiveBucketing::CSeasonalComponentAdaptiveBucketing(double decayRate,
                                                                         double minimumBucketLength,
                                                                         core::CStateRestoreTraverser &traverser) :
        m_DecayRate(std::max(decayRate, MINIMUM_DECAY_RATE)),
        m_MinimumBucketLength(minimumBucketLength)
{
    traverser.traverseSubLevel(boost::bind(&CSeasonalComponentAdaptiveBucketing::acceptRestoreTraverser, this, _1));
}

const CSeasonalComponentAdaptiveBucketing &
CSeasonalComponentAdaptiveBucketing::operator=(const CSeasonalComponentAdaptiveBucketing &rhs)
{
    if (&rhs != this)
    {
        CSeasonalComponentAdaptiveBucketing tmp(rhs);
        this->swap(tmp);
    }
    return *this;
}

void CSeasonalComponentAdaptiveBucketing::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertLevel(TIME_TAG, boost::bind(&CSeasonalTimeStateSerializer::acceptPersistInserter,
                                               boost::cref(*m_Time), _1));
    inserter.insertValue(INITIAL_TIME_TAG, m_InitialTime);
    inserter.insertValue(DECAY_RATE_TAG, m_DecayRate, core::CIEEE754::E_SinglePrecision);
    inserter.insertValue(ENDPOINT_TAG, core::CPersistUtils::toString(m_Endpoints, CFloatStorage::CToString()));
    for (std::size_t i = 0u; i < m_Regressions.size(); ++i)
    {
        inserter.insertLevel(REGRESSION_TAG, boost::bind(&TRegression::acceptPersistInserter,
                                                         &m_Regressions[i], _1));
    }
    inserter.insertValue(VARIANCES_TAG, core::CPersistUtils::toString(m_Variances, CFloatStorage::CToString()));
    inserter.insertValue(LP_FORCE_TAG, m_LpForce.toDelimited());
    inserter.insertValue(FORCE_TAG, m_Force.toDelimited());
}

void CSeasonalComponentAdaptiveBucketing::swap(CSeasonalComponentAdaptiveBucketing &other)
{
    m_Time.swap(other.m_Time);
    std::swap(m_InitialTime, other.m_InitialTime);
    std::swap(m_DecayRate, other.m_DecayRate);
    std::swap(m_MinimumBucketLength, other.m_MinimumBucketLength);
    m_Endpoints.swap(other.m_Endpoints);
    m_Regressions.swap(other.m_Regressions);
    m_Variances.swap(other.m_Variances);
    std::swap(m_LpForce, other.m_LpForce);
    std::swap(m_Force, other.m_Force);
}

core_t::TTime CSeasonalComponentAdaptiveBucketing::timescale(void)
{
    return core::constants::DAY;
}

bool CSeasonalComponentAdaptiveBucketing::initialized(void) const
{
    return m_Endpoints.size() > 0;
}

bool CSeasonalComponentAdaptiveBucketing::initialize(double a, double b, std::size_t n)
{
    if (b <= a)
    {
        LOG_ERROR("Bad interval = [" << a << "," << b << "]");
        return false;
    }
    if (n == 0)
    {
        LOG_ERROR("Must have at least one bucket");
        return false;
    }

    if (m_MinimumBucketLength > 0.0)
    {
        // Handle the case that the minimum bucket length is
        // longer than the period.
        m_MinimumBucketLength = std::min(m_MinimumBucketLength, b - a);
        double B = (b - a) / m_MinimumBucketLength;
        n = std::min(n, static_cast<std::size_t>(std::min(std::max(0.8 * B, 7.0), B)));
    }

    m_Endpoints.clear();
    m_Endpoints.reserve(n+1);
    double h = (b - a) / static_cast<double>(n);
    for (std::size_t i = 0u; i < n+1; ++i)
    {
        m_Endpoints.push_back(a + static_cast<double>(i) * h);
    }
    m_Regressions.clear();
    m_Regressions.resize(n);
    m_Variances.clear();
    m_Variances.resize(n);

    return true;
}

void CSeasonalComponentAdaptiveBucketing::initialValues(core_t::TTime startTime,
                                                        core_t::TTime endTime,
                                                        const TTimeTimePrMeanVarPrVec &values)
{
    if (!this->initialized())
    {
        return;
    }

    this->shiftTime(startTime);

    core_t::TTime time   = m_Time->startOfRepeat(endTime);
    core_t::TTime repeat = endTime - startTime;

    m_InitialTime = time;

    for (std::size_t i = 0u; i < values.size(); ++i)
    {
        double ai = m_Time->periodic(values[i].first.first);
        double bi = m_Time->periodic(values[i].first.second - 1) + 1.0;
        const TDoubleMeanVarAccumulator &vi = values[i].second;

        std::size_t n = m_Endpoints.size();
        if (ai < m_Endpoints[0] || bi > m_Endpoints[n-1])
        {
            LOG_ERROR("t = [" << ai << "," << bi << "]"
                      << " out of range [" << m_Endpoints[0]
                      << "," << m_Endpoints[n-1] << ")");
            return;
        }

        std::size_t ka = std::upper_bound(m_Endpoints.begin(),
                                          m_Endpoints.end(), ai) - m_Endpoints.begin();
        std::size_t kb = std::lower_bound(m_Endpoints.begin(),
                                          m_Endpoints.end(), bi) - m_Endpoints.begin();

        double length = bi - ai;
        for (std::size_t k = ka; k <= kb; ++k)
        {
            double xl = m_Endpoints[k-1];
            double xr = m_Endpoints[k];
            double ak = std::max(ai, xl);
            double bk = std::min(bi, xr);
            double w  = (bk - ak) / length;
            if (w > 0.0)
            {
                core_t::TTime tk = startTime + (time + static_cast<core_t::TTime>(xl) - startTime) % repeat;
                TDoubleMeanVarAccumulator vk = vi;
                vk.age(w * w);
                TDoubleMeanVarAccumulator variance =
                        CBasicStatistics::accumulator(m_Regressions[k-1].count(),
                                                      m_Regressions[k-1].mean(),
                                                      static_cast<double>(m_Variances[k-1])) + vk;

                m_Regressions[k-1].add(m_Time->regression(tk),
                                       CBasicStatistics::mean(vk),
                                       CBasicStatistics::count(vk));
                m_Variances[k-1] = CBasicStatistics::maximumLikelihoodVariance(variance);
            }
        }
    }
}

std::size_t CSeasonalComponentAdaptiveBucketing::size(void) const
{
    return m_Regressions.size();
}

void CSeasonalComponentAdaptiveBucketing::clear(void)
{
    clearAndShrink(m_Endpoints);
    clearAndShrink(m_Regressions);
    clearAndShrink(m_Variances);
}

void CSeasonalComponentAdaptiveBucketing::shiftValue(double shift)
{
    for (std::size_t i = 0u; i < m_Regressions.size(); ++i)
    {
        m_Regressions[i].shiftOrdinate(shift);
    }
}

void CSeasonalComponentAdaptiveBucketing::add(core_t::TTime time, double value, double weight)
{
    std::size_t i = 0;
    if (!this->initialized() || !this->bucket(time, i))
    {
        return;
    }

    this->shiftTime(time);
    double t = m_Time->regression(time);

    TRegression &regression = m_Regressions[i];
    CFloatStorage &variance = m_Variances[i];

    TDoubleMeanVarAccumulator moments =
            CBasicStatistics::accumulator(regression.count(),
                                          predict(regression, t, this->bucketingAgeAt(time)),
                                          static_cast<double>(variance));

    regression.add(t, value, weight);
    moments.add(value, weight);
    variance = CBasicStatistics::maximumLikelihoodVariance(moments);
}

const CSeasonalTime &CSeasonalComponentAdaptiveBucketing::time(void) const
{
    return *m_Time;
}

void CSeasonalComponentAdaptiveBucketing::decayRate(double value)
{
    m_DecayRate = std::max(value, MINIMUM_DECAY_RATE);
}

double CSeasonalComponentAdaptiveBucketing::decayRate(void) const
{
    return m_DecayRate;
}

void CSeasonalComponentAdaptiveBucketing::propagateForwardsByTime(double time, bool meanRevert)
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

    double alpha  = ::exp(-m_DecayRate * time);
    double alpha2 = alpha * alpha;

    for (std::size_t i = 0u; i < m_Regressions.size(); ++i)
    {
        m_Regressions[i].age(alpha, meanRevert);
    }
    m_LpForce.age(alpha2);
    m_Force.age(alpha2);
}

void CSeasonalComponentAdaptiveBucketing::refine(core_t::TTime time)
{
    typedef std::pair<double, std::size_t> TDoubleSizePr;
    typedef CBasicStatistics::COrderStatisticsStack<TDoubleSizePr, 1> TMinAccumulator;
    typedef CBasicStatistics::COrderStatisticsStack<TDoubleSizePr, 1, std::greater<TDoubleSizePr> > TMaxAccumulator;

    static const double SMOOTHING_FUNCTION[] = { 0.25, 0.5, 0.25 };
    static const std::size_t WIDTH = boost::size(SMOOTHING_FUNCTION) / 2;
    static const double ALPHA = 0.25;
    static const double EPS = std::numeric_limits<double>::epsilon();

    LOG_TRACE("refine");

    std::size_t n = m_Endpoints.size();
    if (n < 2)
    {
        return;
    }
    --n;

    double a = m_Endpoints[0];
    double b = m_Endpoints[n];

    // Extract the bucket means.
    TDoubleVec values;
    values.reserve(n);
    for (std::size_t i = 0u; i < m_Regressions.size(); ++i)
    {
        const TRegression &regression = m_Regressions[i];
        if (regression.count() > 0.0)
        {
            double ai = m_Endpoints[i];
            double bi = m_Endpoints[i+1];
            double ti = m_Time->regression(time + static_cast<core_t::TTime>(0.5 * (ai + bi)));
            values.push_back(predict(regression, ti, this->bucketingAgeAt(time)));
        }
    }
    if (values.size() < n)
    {
        // We only refine if all the buckets have samples.
        return;
    }
    LOG_TRACE("values = " << core::CContainerPrinter::print(values));

    // Compute the function range in each bucket, imposing periodic
    // boundary conditions at the start and end of the interval.
    TDoubleVec ranges;
    ranges.reserve(n);
    for (std::size_t i = 0u; i < n; ++i)
    {
        static const double WEIGHTS[] = { 1.0, 1.0, 1.0, 0.75, 0.5 };
        double v[] =
            {
                values[(n + i - 2) % n],
                values[(n + i - 1) % n],
                values[(n + i + 0) % n],
                values[(n + i + 1) % n],
                values[(n + i + 2) % n]
            };

        TMinAccumulator min;
        TMaxAccumulator max;
        for (std::size_t j = 0u; j < sizeof(v)/sizeof(v[0]); ++j)
        {
            min.add(TDoubleSizePr(v[j], j));
            max.add(TDoubleSizePr(v[j], j));
        }

        ranges.push_back(WEIGHTS[max[0].second > min[0].second ?
                                 max[0].second - min[0].second :
                                 min[0].second - max[0].second]
                         * ::pow(max[0].first - min[0].first, 0.75));
    }

    // Smooth the ranges by convolving with a smoothing function.
    // We do this in the "time" domain because the smoothing
    // function is narrow. Estimate the averaging error in each
    // bucket by multiplying the smoothed range by the bucket width.
    double totalAveragingError = 0.0;
    TDoubleVec averagingErrors;
    averagingErrors.reserve(n);
    for (std::size_t i = 0u; i < n; ++i)
    {
        double ai = m_Endpoints[i];
        double bi = m_Endpoints[i+1];

        double error = 0.0;
        for (std::size_t j = 0; j < boost::size(SMOOTHING_FUNCTION); ++j)
        {
            error += SMOOTHING_FUNCTION[j] * ranges[(n + i + j - WIDTH) % n];
        }

        double h = bi - ai;
        error *= h / (b - a);

        averagingErrors.push_back(error);
        totalAveragingError += error;
        LOG_TRACE("interval = [" << ai << "," << bi << "] "
                  << " averagingError = " << error);
    }
    LOG_TRACE("averagingErrors = "
              << core::CContainerPrinter::print(averagingErrors));
    LOG_TRACE("totalAveragingError = " << totalAveragingError);

    double n_ = static_cast<double>(n);
    double step = (1 - n_ * EPS) * totalAveragingError / n_;
    TFloatVec endpoints(m_Endpoints);
    LOG_TRACE("step = " << step);

    // If all the function values are identical then the end points
    // should be equidistant. We check step in case of underflow.
    if (step == 0.0)
    {
        m_Endpoints[0] = a;
        for (std::size_t i = 0u; i < n; ++i)
        {
            m_Endpoints[i] = (b - a) * static_cast<double>(i) / n_;
        }
        m_Endpoints[n] = b;
    }
    else
    {
        // Noise in the bucket mean values creates a "high"
        // frequency mean zero driving force on the buckets'
        // end points desired positions. Once they have stabilized
        // on their desired location for the trend, we are able
        // to detect this by comparing an IIR low pass filtered
        // force and the total force. The lower the ratio the
        // smaller the force we actually apply. Note we want to
        // damp the noise out because the process of adjusting
        // the buckets values loses a small amount of information,
        // see the comments at the start of refresh for more
        // information.
        double alpha = scale(ALPHA, m_Time->period())
                           * (CBasicStatistics::mean(m_Force) == 0.0 ?
                              1.0 : ::fabs(CBasicStatistics::mean(m_LpForce))
                                         / CBasicStatistics::mean(m_Force));
        double force = 0.0;

        // Linearly interpolate between the current end points
        // and points separated by equal total averaging error.
        // Interpolating is equivalent to adding a drag term in
        // the differential equation governing the end point
        // dynamics and damps any oscillatory behavior which
        // might otherwise occur.
        double error = 0.0;
        for (std::size_t i = 0u, j = 1u; i < n && j < n+1; ++i)
        {
            double ai = endpoints[i];
            double bi = endpoints[i+1];
            double h = bi - ai;
            double e = averagingErrors[i];
            double te = e;
            error += te;
            for (double e_ = step - (error - te);
                 error >= step;
                 e_ += step, error -= step)
            {
                double x = h * e_ / averagingErrors[i];
                m_Endpoints[j] = endpoints[j] + alpha * (ai + x - endpoints[j]);
                force += (ai + x) - endpoints[j];
                LOG_TRACE("interval averaging error = " << te
                          << ", a(i) = " << ai
                          << ", x = " << x
                          << ", endpoint " << endpoints[j]
                          << " -> " << ai + x);
                ++j;
            }
        }
        if (m_MinimumBucketLength > 0.0)
        {
            CTools::spread(a, b, m_MinimumBucketLength, m_Endpoints);
        }

        // By construction, the first and last end point should be
        // close "a" and "b", respectively, but we snap them to "a"
        // and "b" so that the total interval is unchanged.
        m_Endpoints[0] = a;
        m_Endpoints[n] = b;
        LOG_TRACE("refinedEndpoints = " << core::CContainerPrinter::print(m_Endpoints));

        m_LpForce.add(force);
        m_Force.add(::fabs(force));
    }

    this->refresh(endpoints);
}

double CSeasonalComponentAdaptiveBucketing::count(core_t::TTime time) const
{
    const TRegression *regression = this->regression(time);
    return regression ? regression->count() : 0.0;
}

std::size_t CSeasonalComponentAdaptiveBucketing::emptyBucketCount(void) const
{
    std::size_t result = 0u;
    for (std::size_t i = 0u; i < m_Regressions.size(); ++i)
    {
        if (m_Regressions[i].count() == 0.0)
        {
            ++result;
        }
    }
    return result;
}

const CSeasonalComponentAdaptiveBucketing::TRegression *
CSeasonalComponentAdaptiveBucketing::regression(core_t::TTime time) const
{
    if (!this->initialized())
    {
        return 0;
    }
    std::size_t i = 0u;
    this->bucket(time, i);
    i = CTools::truncate(i, std::size_t(0), m_Regressions.size() - 1);
    return &m_Regressions[i];
}

void CSeasonalComponentAdaptiveBucketing::knots(core_t::TTime time,
                                                CSplineTypes::EBoundaryCondition boundary,
                                                TDoubleVec &knots,
                                                TDoubleVec &values,
                                                TDoubleVec &variances) const
{
    knots.clear();
    values.clear();
    variances.clear();

    std::size_t n = m_Regressions.size();
    for (std::size_t i = 0u; i < n; ++i)
    {
        if (m_Regressions[i].count() > 0.0)
        {
            double wide = 3.0 * (m_Endpoints[n] - m_Endpoints[0]) / static_cast<double>(n);
            LOG_TRACE("period " << m_Endpoints[n] - m_Endpoints[0]
                      << ", # buckets = " << n
                      << ", wide = " << wide);

            // We get two points for each wide bucket but at most
            // half the buckets can be wide. In this case we have
            // 2 * n/3 + 2*n/3 knot points.
            knots.reserve(4 * n / 3);
            values.reserve(4 * n / 3);
            variances.reserve(4 * n / 3);

            double a = m_Endpoints[i];
            double b = m_Endpoints[i+1];
            double t = m_Time->regression(time + static_cast<core_t::TTime>(0.5 * (a + b)));
            knots.push_back(m_Endpoints[0]);
            values.push_back(predict(m_Regressions[i], t, this->bucketingAgeAt(time)));
            variances.push_back(m_Variances[i]);
            for (/**/; i < n; ++i)
            {
                if (m_Regressions[i].count() > 0.0)
                {
                    a = m_Endpoints[i];
                    b = m_Endpoints[i+1];
                    t = m_Time->regression(time + static_cast<core_t::TTime>(0.5 * (a + b)));
                    double m = predict(m_Regressions[i], t, this->bucketingAgeAt(time));
                    double v = m_Variances[i];
                    if (b - a > wide)
                    {
                        knots.push_back((3.0 * a + b) / 4.0);
                        values.push_back(m);
                        variances.push_back(v);
                        knots.push_back((a + 3.0 * b) / 4.0);
                        values.push_back(m);
                        variances.push_back(v);
                    }
                    else
                    {
                        knots.push_back((a + b) / 2.0);
                        values.push_back(m);
                        variances.push_back(v);
                    }
                }
            }

            switch (boundary)
            {
            case CSplineTypes::E_Natural:
            case CSplineTypes::E_ParabolicRunout:
                knots.push_back(m_Endpoints[n]);
                values.push_back(values.back());
                variances.push_back(variances.back());
                break;

            case CSplineTypes::E_Periodic:
                values[0] = (values[0] + values.back()) / 2.0;
                variances[0] = (variances[0] + variances.back()) / 2.0;
                knots.push_back(m_Endpoints[n]);
                values.push_back(values[0]);
                variances.push_back(variances[0]);
                break;
            }
        }
    }
}

uint64_t CSeasonalComponentAdaptiveBucketing::checksum(uint64_t seed) const
{
    seed = m_Time->checksum(seed);
    seed = CChecksum::calculate(seed, m_InitialTime);
    seed = CChecksum::calculate(seed, m_DecayRate);
    seed = CChecksum::calculate(seed, m_MinimumBucketLength);
    seed = CChecksum::calculate(seed, m_Endpoints);
    seed = CChecksum::calculate(seed, m_Regressions);
    return CChecksum::calculate(seed, m_Variances);
}

void CSeasonalComponentAdaptiveBucketing::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CSeasonalComponentAdaptiveBucketing");
    core::CMemoryDebug::dynamicSize("m_Endpoints", m_Endpoints, mem);
    core::CMemoryDebug::dynamicSize("m_Regressions", m_Regressions, mem);
    core::CMemoryDebug::dynamicSize("m_Variances", m_Variances, mem);
}

std::size_t CSeasonalComponentAdaptiveBucketing::memoryUsage(void) const
{
    std::size_t mem = core::CMemory::dynamicSize(m_Endpoints);
    mem += core::CMemory::dynamicSize(m_Regressions);
    mem += core::CMemory::dynamicSize(m_Variances);
    return mem;
}

const CSeasonalComponentAdaptiveBucketing::TFloatVec &CSeasonalComponentAdaptiveBucketing::endpoints(void) const
{
    return m_Endpoints;
}

double CSeasonalComponentAdaptiveBucketing::count(void) const
{
    double result = 0.0;
    for (std::size_t i = 0u; i < m_Regressions.size(); ++i)
    {
        result += m_Regressions[i].count();
    }
    return result;
}

CSeasonalComponentAdaptiveBucketing::TDoubleVec CSeasonalComponentAdaptiveBucketing::values(core_t::TTime time) const
{
    TDoubleVec result;
    result.reserve(m_Regressions.size());
    for (std::size_t i = 0u; i < m_Regressions.size(); ++i)
    {
        double a = m_Endpoints[i];
        double b = m_Endpoints[i+1];
        double t = m_Time->regression(time + static_cast<core_t::TTime>(0.5 * (a + b)));
        result.push_back(predict(m_Regressions[i], t, this->bucketingAgeAt(time)));
    }
    return result;
}

CSeasonalComponentAdaptiveBucketing::TDoubleVec CSeasonalComponentAdaptiveBucketing::variances(void) const
{
    TDoubleVec result;
    result.reserve(m_Variances.size());
    for (std::size_t i = 0u; i < m_Variances.size(); ++i)
    {
        result.push_back(m_Variances[i]);
    }
    return result;
}

bool CSeasonalComponentAdaptiveBucketing::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        RESTORE(TIME_TAG, traverser.traverseSubLevel(boost::bind(&CSeasonalTimeStateSerializer::acceptRestoreTraverser,
                                                                 boost::ref(m_Time), _1)))
        RESTORE_BUILT_IN(INITIAL_TIME_TAG, m_InitialTime)
        RESTORE_BUILT_IN(DECAY_RATE_TAG, m_DecayRate)
        RESTORE(ENDPOINT_TAG, core::CPersistUtils::fromString(traverser.value(),
                                                              CFloatStorage::CFromString(),
                                                              m_Endpoints))
        RESTORE_SETUP_TEARDOWN(REGRESSION_TAG,
                               TRegression regression,
                               traverser.traverseSubLevel(boost::bind(&TRegression::acceptRestoreTraverser,
                                                                      &regression, _1)),
                               m_Regressions.push_back(regression))
        RESTORE(VARIANCES_TAG, core::CPersistUtils::fromString(traverser.value(),
                                                               CFloatStorage::CFromString(),
                                                               m_Variances))
        RESTORE(LP_FORCE_TAG, m_LpForce.fromDelimited(traverser.value()))
        RESTORE(FORCE_TAG, m_Force.fromDelimited(traverser.value()))
    }
    while (traverser.next());

    TRegressionVec(m_Regressions).swap(m_Regressions);

    return true;
}

bool CSeasonalComponentAdaptiveBucketing::bucket(core_t::TTime time, std::size_t &result) const
{
    double t = m_Time->periodic(time);

    std::size_t i = std::upper_bound(m_Endpoints.begin(),
                                     m_Endpoints.end(), t) - m_Endpoints.begin();
    std::size_t n = m_Endpoints.size();
    if (t < m_Endpoints[0] || i == n)
    {
        LOG_ERROR("t = " << t
                  << " out of range [" << m_Endpoints[0]
                  << "," << m_Endpoints[n-1] << ")");
        return false;
    }

    result = i - 1;
    return true;
}

void CSeasonalComponentAdaptiveBucketing::refresh(const TFloatVec &endpoints)
{
    // Values are assigned based on their intersection with each
    // bucket in the previous configuration. The regression and
    // variance are computed using the appropriate combination
    // rules. Note that the count is weighted by the square of
    // the fractional intersection between the old and new buckets.
    // This means that the effective weight of buckets whose end
    // points change significantly is reduced. This is reasonable
    // because the periodic trend is assumed to be unchanging
    // throughout the interval, when of course it is varying, so
    // adjusting the end points introduces error in the bucket
    // value, which we handle by reducing its significance in the
    // new bucket values.
    //
    // A better approximation is to assume that it the trend is
    // continuous. In fact, this can be done by using a spline
    // with the constraint that the mean of the spline in each
    // interval is equal to the mean value. We can additionally
    // decompose the variance into a contribution from noise and
    // a contribution from the trend. Under these assumptions it
    // is then possible (but not trivial) to update the bucket
    // means and variances based on the new end point positions.
    // This might be worth considering at some point.

    std::size_t m = m_Regressions.size();
    std::size_t n = endpoints.size();
    if (m+1 != n)
    {
        LOG_ERROR("Inconsistent end points and regressions");
        return;
    }

    TRegressionVec regressions;
    TFloatVec variances;
    regressions.reserve(m);
    variances.reserve(m);

    for (std::size_t i = 1u; i < n; ++i)
    {
        std::size_t r = std::lower_bound(endpoints.begin(),
                                         endpoints.end(),
                                         m_Endpoints[i]) - endpoints.begin();
        r = CTools::truncate(r, std::size_t(1), n - 1);

        std::size_t l = std::upper_bound(endpoints.begin(),
                                         endpoints.end(),
                                         m_Endpoints[i-1]) - endpoints.begin();
        l = CTools::truncate(l, std::size_t(1), r);

        LOG_TRACE("interval = [" << m_Endpoints[i-1]
                  << "," << m_Endpoints[i] << "]");
        LOG_TRACE("l = " << l << ", r = " << r);
        LOG_TRACE("[x(l), x(r)] = [" << endpoints[l-1]
                  << "," << endpoints[r] << "]");

        double xl = endpoints[l-1];
        double xr = endpoints[l];
        if (l == r)
        {
            double interval = m_Endpoints[i] - m_Endpoints[i-1];
            double w = CTools::truncate(interval / (xr - xl), 0.0, 1.0);
            regressions.push_back(m_Regressions[l-1].scaled(w * w));
            variances.push_back(m_Variances[l-1]);
        }
        else
        {
            double interval = xr - m_Endpoints[i-1];
            double w = CTools::truncate(interval / (xr - xl), 0.0, 1.0);
            TDoubleRegression regression = m_Regressions[l-1].scaled(w);
            TDoubleMeanVarAccumulator variance =
                    CBasicStatistics::accumulator(w * m_Regressions[l-1].count(),
                                                  m_Regressions[l-1].mean(),
                                                  static_cast<double>(m_Variances[l-1]));
            double count = w * w * m_Regressions[l-1].count();
            while (++l < r)
            {
                regression += m_Regressions[l-1];
                variance += CBasicStatistics::accumulator(m_Regressions[l-1].count(),
                                                          m_Regressions[l-1].mean(),
                                                          static_cast<double>(m_Variances[l-1]));
                count += m_Regressions[l-1].count();
            }
            xl = endpoints[l-1];
            xr = endpoints[l];
            interval = m_Endpoints[i] - xl;
            w = CTools::truncate(interval / (xr - xl), 0.0, 1.0);
            regression += m_Regressions[l-1].scaled(w);
            variance += CBasicStatistics::accumulator(w * m_Regressions[l-1].count(),
                                                      m_Regressions[l-1].mean(),
                                                      static_cast<double>(m_Variances[l-1]));
            count += w * w * m_Regressions[l-1].count();
            double scale = count / regression.count();
            regressions.push_back(regression.scaled(scale));
            variances.push_back(CBasicStatistics::maximumLikelihoodVariance(variance));
        }
    }

    // We want all regressions assign the same weight to the values
    // in one period otherwise they respond at different rates to
    // changes in the trend. To achieve this we should assign them
    // a weight equal to the number of points they will receive.
    double count = 0.0;
    for (std::size_t i = 0u; i < m; ++i)
    {
        count += regressions[i].count();
    }
    count /= endpoints[m] - endpoints[0];
    for (std::size_t i = 0u; i < m; ++i)
    {
        double c = regressions[i].count();
        if (c > 0.0)
        {
            regressions[i].scale(count * (endpoints[i+1] - endpoints[i]) / c);
        }
    }

    LOG_TRACE("old endpoints   = " << core::CContainerPrinter::print(endpoints));
    LOG_TRACE("old regressions = " << core::CContainerPrinter::print(m_Regressions));
    LOG_TRACE("old variances   = " << core::CContainerPrinter::print(m_Variances));
    LOG_TRACE("new endpoints   = " << core::CContainerPrinter::print(m_Endpoints));
    LOG_TRACE("new regressions = " << core::CContainerPrinter::print(regressions));
    LOG_TRACE("new variances   = " << core::CContainerPrinter::print(variances));
    m_Regressions.swap(regressions);
    m_Variances.swap(variances);
}

void CSeasonalComponentAdaptiveBucketing::shiftTime(core_t::TTime time)
{
    core_t::TTime shiftTime = static_cast<core_t::TTime>(
            0.5 * static_cast<double>(timescale()) / m_DecayRate);
    shiftTime = std::max(shiftTime, core::constants::WEEK);

    if (time >= m_Time->regressionOrigin() + shiftTime)
    {
        core_t::TTime shift = m_Time->startOfWindow(time);
        double dx = -m_Time->regression(shift);
        for (std::size_t i = 0u; i < m_Regressions.size(); ++i)
        {
            m_Regressions[i].shiftAbscissa(dx);
        }
        m_Time->regressionOrigin(shift);
    }
}

double CSeasonalComponentAdaptiveBucketing::bucketingAgeAt(core_t::TTime time) const
{
    return static_cast<double>(time - m_InitialTime) / static_cast<double>(7 * timescale());
}

}
}
