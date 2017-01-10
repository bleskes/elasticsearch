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

#include <maths/CTimeSeriesDecomposition.h>

#include <core/CContainerPrinter.h>
#include <core/CLogger.h>
#include <core/Constants.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>
#include <core/RestoreMacros.h>

#include <maths/CBasicStatistics.h>
#include <maths/CChecksum.h>
#include <maths/CIntegerTools.h>
#include <maths/CTools.h>

#include <boost/bind.hpp>
#include <boost/container/flat_map.hpp>
#include <boost/numeric/conversion/bounds.hpp>
#include <boost/random/normal_distribution.hpp>

#include <string>
#include <utility>
#include <vector>

namespace ml
{
namespace maths
{
namespace
{

typedef std::pair<double, double> TDoubleDoublePr;
typedef CVectorNx1<double, 2> TVector2x1;

//! Convert a double pair to a 2x1 vector.
TVector2x1 vector2x1(const TDoubleDoublePr &p)
{
    TVector2x1 result;
    result(0) = p.first;
    result(1) = p.second;
    return result;
}

//! Convert a 2x1 vector to a double pair.
TDoubleDoublePr pair(const TVector2x1 &v)
{
    return std::make_pair(v(0), v(1));
}

//! Add on errors due to uncertainty in the regression coefficients.
template<std::size_t N>
void forecastErrors(const CSymmetricMatrixNxN<double, N> &m,
                    double dt, double confidence,
                    TVector2x1 &result)
{
    double ti = dt;
    for (std::size_t i = 1; dt > 0.0 && i < m.rows(); ++i, ti *= dt)
    {
        boost::math::normal_distribution<> normal(0.0, ::sqrt(m(i,i)));
        result(0) += boost::math::quantile(normal, (100.0 - confidence) / 200.0) * ti;
        result(1) += boost::math::quantile(normal, (100.0 + confidence) / 200.0) * ti;
    }
}

const std::string DECAY_RATE_TAG("a");
const std::string LAST_TIME_TAG("b");
const std::string LAST_PROPAGATE_FORWARDS_BY_TIME_TAG("c");
const std::string LONG_TERM_TREND_TEST_TAG("d");
const std::string DAILY_WEEKLY_TEST_TAG("e");
const std::string SEASONAL_COMPONENTS_TAG("f");
const std::string EMPTY_STRING;

}

CTimeSeriesDecomposition::CTimeSeriesDecomposition(double decayRate,
                                                   core_t::TTime bucketLength,
                                                   std::size_t seasonalComponentSize) :
        m_DecayRate(decayRate),
        m_LastTime(boost::numeric::bounds<core_t::TTime>::lowest()),
        m_LastPropagateForwardsTime(0),
        m_LongTermTrendTest(decayRate),
        m_DailyWeeklyTest(decayRate, bucketLength),
        m_Components(decayRate, bucketLength, seasonalComponentSize)
{
    this->initializeMediator();
}

CTimeSeriesDecomposition::CTimeSeriesDecomposition(double decayRate,
                                                   core_t::TTime bucketLength,
                                                   std::size_t seasonalComponentSize,
                                                   core::CStateRestoreTraverser &traverser) :
        m_DecayRate(decayRate),
        m_LastTime(boost::numeric::bounds<core_t::TTime>::lowest()),
        m_LastPropagateForwardsTime(0),
        m_LongTermTrendTest(decayRate),
        m_DailyWeeklyTest(decayRate, bucketLength),
        m_Components(decayRate, bucketLength, seasonalComponentSize)
{
    traverser.traverseSubLevel(boost::bind(&CTimeSeriesDecomposition::acceptRestoreTraverser, this, _1));
    this->initializeMediator();
}

CTimeSeriesDecomposition::CTimeSeriesDecomposition(const CTimeSeriesDecomposition &other) :
        m_DecayRate(other.m_DecayRate),
        m_LastTime(other.m_LastTime),
        m_LastPropagateForwardsTime(other.m_LastPropagateForwardsTime),
        m_LongTermTrendTest(other.m_LongTermTrendTest),
        m_DailyWeeklyTest(other.m_DailyWeeklyTest),
        m_Components(other.m_Components)
{
    this->initializeMediator();
}

bool CTimeSeriesDecomposition::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        RESTORE_BUILT_IN(DECAY_RATE_TAG, m_DecayRate);
        RESTORE_BUILT_IN(LAST_TIME_TAG, m_LastTime)
        RESTORE_BUILT_IN(LAST_PROPAGATE_FORWARDS_BY_TIME_TAG, m_LastPropagateForwardsTime)
        RESTORE(LONG_TERM_TREND_TEST_TAG, traverser.traverseSubLevel(
                                              boost::bind(&CLongTermTrendTest::acceptRestoreTraverser,
                                                          &m_LongTermTrendTest, _1)));
        RESTORE(DAILY_WEEKLY_TEST_TAG, traverser.traverseSubLevel(
                                           boost::bind(&CDailyWeeklyTest::acceptRestoreTraverser,
                                                       &m_DailyWeeklyTest, _1)));
        RESTORE(SEASONAL_COMPONENTS_TAG, traverser.traverseSubLevel(
                                             boost::bind(&CComponents::acceptRestoreTraverser,
                                                         &m_Components, _1)))
    }
    while (traverser.next());

    m_Components.decayRate(m_DecayRate);

    return true;
}

void CTimeSeriesDecomposition::swap(CTimeSeriesDecomposition &other)
{
    std::swap(m_DecayRate, other.m_DecayRate);
    std::swap(m_LastTime, other.m_LastTime);
    std::swap(m_LastPropagateForwardsTime, other.m_LastPropagateForwardsTime);
    m_LongTermTrendTest.swap(other.m_LongTermTrendTest);
    m_DailyWeeklyTest.swap(other.m_DailyWeeklyTest);
    m_Components.swap(other.m_Components);
}

CTimeSeriesDecomposition &CTimeSeriesDecomposition::operator=(const CTimeSeriesDecomposition &other)
{
    if (this != &other)
    {
        CTimeSeriesDecomposition copy(other);
        this->swap(copy);
    }
    return *this;
}

void CTimeSeriesDecomposition::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertValue(DECAY_RATE_TAG, this->decayRate(), core::CIEEE754::E_SinglePrecision);
    inserter.insertValue(LAST_TIME_TAG, m_LastTime);
    inserter.insertValue(LAST_PROPAGATE_FORWARDS_BY_TIME_TAG, m_LastPropagateForwardsTime);
    inserter.insertLevel(LONG_TERM_TREND_TEST_TAG, boost::bind(&CLongTermTrendTest::acceptPersistInserter,
                                                               &m_LongTermTrendTest, _1));
    inserter.insertLevel(DAILY_WEEKLY_TEST_TAG, boost::bind(&CDailyWeeklyTest::acceptPersistInserter,
                                                            &m_DailyWeeklyTest, _1));
    inserter.insertLevel(SEASONAL_COMPONENTS_TAG, boost::bind(&CComponents::acceptPersistInserter,
                                                              &m_Components, _1));
}

CTimeSeriesDecomposition *CTimeSeriesDecomposition::clone(void) const
{
    return new CTimeSeriesDecomposition(*this);
}

void CTimeSeriesDecomposition::decayRate(double decayRate)
{
    m_DecayRate = decayRate;
    m_LongTermTrendTest.decayRate(decayRate);
    // Daily + weekly and level shift tests use a fixed decay rate.
    m_Components.decayRate(decayRate);
}

double CTimeSeriesDecomposition::decayRate(void) const
{
    return m_DecayRate;
}

void CTimeSeriesDecomposition::forecast(void)
{
    m_Components.forecast();
}

bool CTimeSeriesDecomposition::initialized(void) const
{
    return m_Components.initialized();
}

core_t::TTime CTimeSeriesDecomposition::startOfWeek(void) const
{
    const TComponentVec &components = m_Components.seasonal();
    return !components.empty() ? components.front().time().weekStart() : 0;
}

core_t::TTime CTimeSeriesDecomposition::period(void) const
{
    const CTrendTests::CPeriodicity::CResult &periods = m_DailyWeeklyTest.periods();
    if (!periods.periodic())
    {
        return std::numeric_limits<core_t::TTime>::max();
    }
    return periods.periods(CTrendTests::SELECT_WEEK) == CTrendTests::SELECT_DAILY ?
           core::constants::DAY : core::constants::WEEK;
}

bool CTimeSeriesDecomposition::addPoint(core_t::TTime time,
                                        double value,
                                        const TWeightStyleVec &weightStyles,
                                        const TDouble4Vec &weights)
{
    CComponents::CScopeNotifyOnStateChange result(m_Components);

    m_LastTime = std::max(m_LastTime, time);

    SAddValueMessage message(time, value, weightStyles, weights,
                             CBasicStatistics::mean(this->baseline(time, 0.0, E_Trend)),
                             CBasicStatistics::mean(this->baseline(time, 0.0, E_Seasonal)));
    m_Components.handle(message);
    m_LongTermTrendTest.handle(message);
    m_DailyWeeklyTest.handle(message);

    return result.changed();
}

bool CTimeSeriesDecomposition::testAndInterpolate(core_t::TTime time)
{
    CComponents::CScopeNotifyOnStateChange result(m_Components);

    SMessage message(time);
    m_LongTermTrendTest.test(message);
    m_DailyWeeklyTest.test(message);
    m_Components.interpolate(message);

    return result.changed();
}

double CTimeSeriesDecomposition::mean(void) const
{
    return m_Components.meanValue();
}

TDoubleDoublePr CTimeSeriesDecomposition::baseline(core_t::TTime time,
                                                   double confidence,
                                                   EComponents components,
                                                   bool smooth) const
{
    if (!this->initialized())
    {
        return TDoubleDoublePr(0.0, 0.0);
    }

    core_t::TTime ii = m_Components.interpolateInterval();

    TVector2x1 baseline(0.0);

    if (components & E_Trend)
    {
        CTrendCRef trend = m_Components.trend();

        baseline += vector2x1(trend.prediction(time, confidence));

        if (m_Components.forecasting())
        {
            CTrendCRef::TMatrix m;
            if (trend.covariances(m))
            {
                double dt = std::max(trend.time(time) - trend.time(m_LastTime + ii), 0.0);
                forecastErrors(m, dt, confidence, baseline);
            }
        }
    }

    if (components & E_Seasonal)
    {
        const TComponentVec &components_ = m_Components.seasonal();

        for (std::size_t i = 0u; i < components_.size(); ++i)
        {
            const CSeasonalComponent &component = components_[i];
            if (component.initialized() && component.time().inWindow(time))
            {
                baseline += vector2x1(component.value(time, confidence));
                if (m_Components.forecasting())
                {
                    CSeasonalComponent::TMatrix m;
                    if (component.covariances(time, m))
                    {
                        double dt = std::max(  component.time().regression(time)
                                             - component.time().regression(m_LastTime + ii), 0.0);
                        forecastErrors(m, dt, confidence, baseline);
                    }
                }
            }
        }
    }

    if (smooth)
    {
        baseline += vector2x1(this->smoothing(time, confidence));
    }

    return pair(baseline);
}

double CTimeSeriesDecomposition::detrend(core_t::TTime time, double value, double confidence) const
{
    if (!this->initialized())
    {
        return value;
    }
    TDoubleDoublePr baseline = this->baseline(time, confidence);
    return std::min(value - baseline.first, 0.0) + std::max(value - baseline.second, 0.0);
}

double CTimeSeriesDecomposition::meanVariance(void) const
{
    return m_Components.meanVariance();
}

TDoubleDoublePr CTimeSeriesDecomposition::scale(core_t::TTime time, double variance, double confidence) const
{
    if (!this->initialized())
    {
        return TDoubleDoublePr(1.0, 1.0);
    }

    double mean = this->meanVariance();
    LOG_TRACE("mean = " << mean);
    if (mean == 0.0)
    {
        return TDoubleDoublePr(1.0, 1.0);
    }

    const TComponentVec &components = m_Components.seasonal();
    TVector2x1 scale(0.0);
    for (std::size_t i = 0u; i < components.size(); ++i)
    {
        const CSeasonalComponent &component = components[i];
        if (component.initialized() && component.time().inWindow(time))
        {
            scale += vector2x1(component.variance(time, confidence));
        }
    }
    LOG_TRACE("variance = " << core::CContainerPrinter::print(scale));

    double bias = std::min(2.0 * mean / variance, 1.0);
    scale /= mean;
    scale  = TVector2x1(1.0) + bias * (scale - TVector2x1(1.0));

    return pair(scale);
}

void CTimeSeriesDecomposition::propagateForwardsTo(core_t::TTime time)
{
    if (time < m_LastPropagateForwardsTime)
    {
        return;
    }

    core_t::TTime interval = m_Components.propagationForwardsByTimeInterval();
    time = CIntegerTools::floor(time, interval);

    if (time - m_LastPropagateForwardsTime > 0)
    {
        double elapsedTime = static_cast<double>(time - m_LastPropagateForwardsTime)
                           / static_cast<double>(interval);
        m_DailyWeeklyTest.propagateForwardsByTime(elapsedTime);
        m_Components.propagateForwards(m_LastPropagateForwardsTime, time);
        m_LastPropagateForwardsTime = time;
    }
}

void CTimeSeriesDecomposition::describe(const std::string &indent, std::string &result) const
{
    result += core_t::LINE_ENDING + indent + "decomposition";
    const TComponentVec &components = m_Components.seasonal();
    if (components.empty())
    {
        result += " no components";
        return;
    }
    result += ": sum of seasonal components";
    for (std::size_t i = 0u; i < components.size(); ++i)
    {
        components[i].describe(indent + " ", result);
    }
}

void CTimeSeriesDecomposition::skipTime(core_t::TTime skipInterval)
{
    m_LastPropagateForwardsTime = CIntegerTools::floor(m_LastPropagateForwardsTime + skipInterval,
                                                       m_Components.propagationForwardsByTimeInterval());
    m_LongTermTrendTest.skipTime(skipInterval);
    m_DailyWeeklyTest.skipTime(skipInterval);
    m_Components.skipTime(skipInterval);
}

uint64_t CTimeSeriesDecomposition::checksum(uint64_t seed) const
{
    seed = CChecksum::calculate(seed, m_DecayRate);
    seed = CChecksum::calculate(seed, m_LastTime);
    seed = CChecksum::calculate(seed, m_LastPropagateForwardsTime);
    seed = CChecksum::calculate(seed, m_LongTermTrendTest);
    seed = CChecksum::calculate(seed, m_DailyWeeklyTest);
    return CChecksum::calculate(seed, m_Components);
}

void CTimeSeriesDecomposition::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CTimeSeriesDecomposition");
    core::CMemoryDebug::dynamicSize("m_Mediator", m_Mediator, mem);
    core::CMemoryDebug::dynamicSize("m_DailyWeeklyTest", m_DailyWeeklyTest, mem);
    core::CMemoryDebug::dynamicSize("m_Components", m_Components, mem);
}

std::size_t CTimeSeriesDecomposition::memoryUsage(void) const
{
    return  core::CMemory::dynamicSize(m_Mediator)
          + core::CMemory::dynamicSize(m_DailyWeeklyTest)
          + core::CMemory::dynamicSize(m_Components);
}

std::size_t CTimeSeriesDecomposition::staticSize(void) const
{
    return sizeof(*this);
}

const CTimeSeriesDecomposition::TComponentVec &CTimeSeriesDecomposition::seasonalComponents(void) const
{
    return m_Components.seasonal();
}

void CTimeSeriesDecomposition::initializeMediator(void)
{
    m_Mediator.reset(new CMediator);
    m_Mediator->registerHandler(m_LongTermTrendTest);
    m_Mediator->registerHandler(m_DailyWeeklyTest);
    m_Mediator->registerHandler(m_Components);
}

TDoubleDoublePr CTimeSeriesDecomposition::smoothing(core_t::TTime time, double confidence) const
{
    const TComponentVec &components = m_Components.seasonal();

    for (std::size_t i = 0u; i < components.size(); ++i)
    {
        const CSeasonalComponent &component = components[i];
        if (!component.initialized() || component.time().inWindow(time))
        {
            continue;
        }

        const CSeasonalComponent::TTime &times = component.time();

        if (times.inWindow(time - SMOOTHING_INTERVAL))
        {
            core_t::TTime discontinuity =  times.startOfWindow(time - SMOOTHING_INTERVAL)
                                         + times.window();
            TVector2x1 baselineMinusEps = vector2x1(this->baseline(discontinuity - 1, confidence, E_All, false));
            TVector2x1 baselinePlusEps  = vector2x1(this->baseline(discontinuity + 1, confidence, E_All, false));
            double scale = 0.5 * (1.0 - static_cast<double>(time - discontinuity)
                                      / static_cast<double>(SMOOTHING_INTERVAL));
            return pair(scale * (baselineMinusEps - baselinePlusEps));
        }
        if (times.inWindow(time + SMOOTHING_INTERVAL))
        {
            core_t::TTime discontinuity = component.time().startOfWindow(time + SMOOTHING_INTERVAL);
            TVector2x1 baselinePlusEps  = vector2x1(this->baseline(discontinuity + 1, confidence, E_All, false));
            TVector2x1 baselineMinusEps = vector2x1(this->baseline(discontinuity - 1, confidence, E_All, false));
            double scale = 0.5 * (1.0 - static_cast<double>(discontinuity - time)
                                      / static_cast<double>(SMOOTHING_INTERVAL));
            return pair(scale * (baselinePlusEps - baselineMinusEps));
        }
    }

    return TDoubleDoublePr(0.0, 0.0);
}

const core_t::TTime CTimeSeriesDecomposition::SMOOTHING_INTERVAL(3600);
const std::size_t CTimeSeriesDecomposition::DEFAULT_COMPONENT_SIZE(36u);

}
}
