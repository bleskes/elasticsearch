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

namespace prelert
{
namespace maths
{
namespace
{
const std::string DECAY_RATE_TAG("a");
const std::string LAST_TIME_TAG("b");
const std::string LAST_PROPAGATE_FORWARDS_BY_TIME_TAG("c");
const std::string DAILY_WEEKLY_TEST_TAG("d");
const std::string LEVEL_SHIFT_TEST_TAG("e");
const std::string SEASONAL_COMPONENTS_TAG("f");
const std::string EMPTY_STRING;
}

CTimeSeriesDecomposition::CTimeSeriesDecomposition(double decayRate,
                                                   core_t::TTime bucketLength,
                                                   std::size_t seasonalComponentSize) :
        m_DecayRate(decayRate),
        m_LastTime(boost::numeric::bounds<core_t::TTime>::lowest()),
        m_LastPropagateForwardsTime(0),
        m_DailyWeeklyTest(decayRate, bucketLength),
        m_LevelShiftTest(decayRate),
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
        m_DailyWeeklyTest(decayRate, bucketLength),
        m_LevelShiftTest(decayRate),
        m_Components(decayRate, bucketLength, seasonalComponentSize)
{
    traverser.traverseSubLevel(boost::bind(&CTimeSeriesDecomposition::acceptRestoreTraverser, this, _1));
    this->initializeMediator();
}

CTimeSeriesDecomposition::CTimeSeriesDecomposition(const CTimeSeriesDecomposition &other) :
        m_DecayRate(other.m_DecayRate),
        m_LastTime(other.m_LastTime),
        m_LastPropagateForwardsTime(other.m_LastPropagateForwardsTime),
        m_DailyWeeklyTest(other.m_DailyWeeklyTest),
        m_LevelShiftTest(other.m_LevelShiftTest),
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
        RESTORE(DAILY_WEEKLY_TEST_TAG, traverser.traverseSubLevel(boost::bind(&CDailyWeeklyTest::acceptRestoreTraverser,
                                                                              &m_DailyWeeklyTest, _1)));
        RESTORE(LEVEL_SHIFT_TEST_TAG, traverser.traverseSubLevel(boost::bind(&CLevelShiftTest::acceptRestoreTraverser,
                                                                             &m_LevelShiftTest, _1)))
        RESTORE(SEASONAL_COMPONENTS_TAG,
                traverser.traverseSubLevel(boost::bind(&CComponents::acceptRestoreTraverser, &m_Components, _1)))
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
    m_DailyWeeklyTest.swap(other.m_DailyWeeklyTest);
    m_LevelShiftTest.swap(other.m_LevelShiftTest);
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
    inserter.insertLevel(DAILY_WEEKLY_TEST_TAG, boost::bind(&CDailyWeeklyTest::acceptPersistInserter,
                                                            &m_DailyWeeklyTest, _1));
    inserter.insertLevel(LEVEL_SHIFT_TEST_TAG, boost::bind(&CLevelShiftTest::acceptPersistInserter,
                                                           &m_LevelShiftTest, _1));
    inserter.insertLevel(SEASONAL_COMPONENTS_TAG,
                         boost::bind(&CComponents::acceptPersistInserter, &m_Components, _1));
}

CTimeSeriesDecomposition *CTimeSeriesDecomposition::clone(void) const
{
    return new CTimeSeriesDecomposition(*this);
}

void CTimeSeriesDecomposition::decayRate(double decayRate)
{
    // Note that the tests use global fixed decay rates.
    m_DecayRate = decayRate;
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

    SAddValueMessage message(time, value, weightStyles, weights, m_Components.meanValue(time));
    m_LevelShiftTest.handle(message);
    m_Components.handle(message);
    m_DailyWeeklyTest.handle(message);
    m_DailyWeeklyTest.test(message);

    return result.changed();
}

bool CTimeSeriesDecomposition::testAndInterpolate(core_t::TTime time)
{
    CComponents::CScopeNotifyOnStateChange result(m_Components);
    SMessage message(time);
    m_LevelShiftTest.test(message);
    m_DailyWeeklyTest.test(message);
    m_Components.interpolate(message);
    return result.changed();
}

double CTimeSeriesDecomposition::mean(void) const
{
    return m_Components.meanValue();
}

double CTimeSeriesDecomposition::level(void) const
{
    return m_Components.level();
}

CTimeSeriesDecomposition::TDoubleDoublePr
    CTimeSeriesDecomposition::baseline(core_t::TTime time, double confidence, bool smooth) const
{
    if (!this->initialized())
    {
        double mean = this->mean();
        return TDoubleDoublePr(mean, mean);
    }

    const TComponentVec &components = m_Components.seasonal();

    TDoubleDoublePr baseline(0.0, 0.0);

    for (std::size_t i = 0u; i < components.size(); ++i)
    {
        const CSeasonalComponent &component = components[i];
        if (component.initialized() && component.time().inWindow(time))
        {
            double t = component.time().regression(time);
            TDoubleDoublePr interval = component.value(time, confidence);
            if (m_Components.forecasting())
            {
                CSeasonalComponent::TMatrix m;
                if (component.covariances(time, m))
                {
                    double lpp = component.time().regression(m_LastTime + m_Components.interpolateInterval());
                    boost::math::normal_distribution<> normal(0.0, ::sqrt(m(1,1)));
                    interval.first  += boost::math::quantile(normal, (100.0 - confidence) / 200.0)
                                     * std::max(t - lpp, 0.0);
                    interval.second += boost::math::quantile(normal, (100.0 + confidence) / 200.0)
                                     * std::max(t - lpp, 0.0);
                }
            }
            baseline.first  += interval.first;
            baseline.second += interval.second;
        }
    }

    if (smooth)
    {
        TDoubleDoublePr smoothing = this->smoothing(time, confidence);
        baseline.first  += smoothing.first;
        baseline.second += smoothing.second;
    }

    return baseline;
}

double CTimeSeriesDecomposition::detrend(core_t::TTime time, double value, double confidence) const
{
    if (!this->initialized())
    {
        return value;
    }
    TDoubleDoublePr interval = this->baseline(time, confidence);
    return this->level() + std::min(value - interval.first, 0.0)
                         + std::max(value - interval.second, 0.0);
}

double CTimeSeriesDecomposition::meanVariance(void) const
{
    return m_Components.meanVariance();
}

CTimeSeriesDecomposition::TDoubleDoublePr
    CTimeSeriesDecomposition::scale(core_t::TTime time, double variance, double confidence) const
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
    TDoubleDoublePr scale(0.0, 0.0);
    for (std::size_t i = 0u; i < components.size(); ++i)
    {
        const CSeasonalComponent &component = components[i];
        if (component.initialized() && component.time().inWindow(time))
        {
            TDoubleDoublePr vi = component.variance(time, confidence);
            scale.first  += vi.first;
            scale.second += vi.second;
        }
    }
    LOG_TRACE("variance = " << core::CContainerPrinter::print(scale));

    double bias = std::min(2.0 * mean / variance, 1.0);
    scale.first  /= mean;
    scale.first   = 1.0 + bias * (scale.first  - 1.0);
    scale.second /= mean;
    scale.second  = 1.0 + bias * (scale.second - 1.0);

    return scale;
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
        m_LevelShiftTest.propagateForwardsByTime(elapsedTime);
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
    m_DailyWeeklyTest.skipTime(skipInterval);
    m_LevelShiftTest.skipTime(skipInterval);
    m_Components.skipTime(skipInterval);
}

uint64_t CTimeSeriesDecomposition::checksum(uint64_t seed) const
{
    seed = CChecksum::calculate(seed, m_DecayRate);
    seed = CChecksum::calculate(seed, m_LastTime);
    seed = CChecksum::calculate(seed, m_LastPropagateForwardsTime);
    seed = CChecksum::calculate(seed, m_DailyWeeklyTest);
    seed = CChecksum::calculate(seed, m_LevelShiftTest);
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
    m_Mediator->registerHandler(m_DailyWeeklyTest);
    m_Mediator->registerHandler(m_LevelShiftTest);
    m_Mediator->registerHandler(m_Components);
}

CTimeSeriesDecomposition::TDoubleDoublePr
    CTimeSeriesDecomposition::smoothing(core_t::TTime time, double confidence) const
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
            TDoubleDoublePr baselineMinusEps = this->baseline(discontinuity - 1, confidence, false);
            TDoubleDoublePr baselinePlusEps  = this->baseline(discontinuity + 1, confidence, false);
            double scale = 0.5 * (1.0 - static_cast<double>(time - discontinuity)
                                      / static_cast<double>(SMOOTHING_INTERVAL));
            return TDoubleDoublePr(scale * (baselineMinusEps.first  - baselinePlusEps.first),
                                   scale * (baselineMinusEps.second - baselinePlusEps.second));
        }
        if (times.inWindow(time + SMOOTHING_INTERVAL))
        {
            core_t::TTime discontinuity = component.time().startOfWindow(time + SMOOTHING_INTERVAL);
            TDoubleDoublePr baselinePlusEps  = this->baseline(discontinuity + 1, confidence, false);
            TDoubleDoublePr baselineMinusEps = this->baseline(discontinuity - 1, confidence, false);
            double scale = 0.5 * (1.0 - static_cast<double>(discontinuity - time)
                                      / static_cast<double>(SMOOTHING_INTERVAL));
            return TDoubleDoublePr(scale * (baselinePlusEps.first  - baselineMinusEps.first),
                                   scale * (baselinePlusEps.second - baselineMinusEps.second));
        }
    }

    return TDoubleDoublePr(0.0, 0.0);
}

const core_t::TTime CTimeSeriesDecomposition::SMOOTHING_INTERVAL(3600);
const std::size_t CTimeSeriesDecomposition::DEFAULT_COMPONENT_SIZE(36u);

}
}
