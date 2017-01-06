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

#include <maths/CSeasonalComponent.h>

#include <core/CLogger.h>
#include <core/Constants.h>
#include <core/CPersistUtils.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>
#include <core/RestoreMacros.h>

#include <maths/CChecksum.h>
#include <maths/CIntegerTools.h>

#include <boost/bind.hpp>
#include <boost/math/distributions/chi_squared.hpp>
#include <boost/math/distributions/normal.hpp>

#include <ios>
#include <vector>

namespace prelert
{
namespace maths
{
namespace
{

typedef std::pair<double, double> TDoubleDoublePr;
typedef std::vector<double> TDoubleVec;

const std::string SPACE_TAG("a");
const std::string BOUNDARY_CONDITION_TAG("b");
std::string BUCKETING_TAG("c");
const std::string SPLINES_TAG("d");

// Nested tags
const std::string ESTIMATED_TAG("a");
const std::string KNOTS_TAG("b");
const std::string VALUES_TAG("c");
const std::string VARIANCES_TAG("d");

const std::string EMPTY_STRING;

}

CSeasonalComponent::CSeasonalComponent(const TTime &time,
                                       std::size_t space,
                                       double decayRate,
                                       double minimumBucketLength,
                                       CSplineTypes::EBoundaryCondition boundaryCondition,
                                       CSplineTypes::EType valueInterpolationType,
                                       CSplineTypes::EType varianceInterpolationType) :
        m_Space(space),
        m_Bucketing(time, decayRate, minimumBucketLength),
        m_BoundaryCondition(boundaryCondition),
        m_Splines(valueInterpolationType, varianceInterpolationType),
        m_MeanValue(0.0),
        m_MeanVariance(0.0)
{
    LOG_TRACE("period = " << time.period()
              << ", window = " << time.window()
              << ", decayRate = " << decayRate);
}

CSeasonalComponent::CSeasonalComponent(double decayRate,
                                       double minimumBucketLength,
                                       core::CStateRestoreTraverser &traverser,
                                       CSplineTypes::EType valueInterpolationType,
                                       CSplineTypes::EType varianceInterpolationType) :
        m_Space(0),
        m_Bucketing(TTime(), decayRate, minimumBucketLength),
        m_BoundaryCondition(CSplineTypes::E_Periodic),
        m_Splines(valueInterpolationType, varianceInterpolationType),
        m_MeanValue(0.0),
        m_MeanVariance(0.0)
{
    traverser.traverseSubLevel(boost::bind(&CSeasonalComponent::acceptRestoreTraverser,
                                           this, decayRate, minimumBucketLength, _1));
}

bool CSeasonalComponent::acceptRestoreTraverser(double decayRate,
                                                double minimumBucketLength,
                                                core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        RESTORE_BUILT_IN(SPACE_TAG, m_Space)
        RESTORE_SETUP_TEARDOWN(BOUNDARY_CONDITION_TAG,
                               int boundaryCondition,
                               core::CStringUtils::stringToType(traverser.value(), boundaryCondition),
                               m_BoundaryCondition = static_cast<CSplineTypes::EBoundaryCondition>(boundaryCondition))
        if (name == BUCKETING_TAG)
        {
            CSeasonalComponentAdaptiveBucketing bucketing(decayRate, minimumBucketLength, traverser);
            m_Bucketing.swap(bucketing);
            continue;
        }
        RESTORE(SPLINES_TAG, traverser.traverseSubLevel(boost::bind(&CPackedSplines::acceptRestoreTraverser,
                                                                    &m_Splines, m_BoundaryCondition, _1)))
    }
    while (traverser.next());

    if (this->initialized())
    {
        m_MeanValue = this->valueSpline().mean();
        m_MeanVariance = this->varianceSpline().mean();
    }

    return true;
}

void CSeasonalComponent::swap(CSeasonalComponent &other)
{
    std::swap(m_Space, other.m_Space);
    std::swap(m_BoundaryCondition, other.m_BoundaryCondition);
    m_Bucketing.swap(other.m_Bucketing);
    std::swap(m_MeanValue, other.m_MeanValue);
    std::swap(m_MeanVariance, other.m_MeanVariance);
    m_Splines.swap(other.m_Splines);
}

void CSeasonalComponent::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertValue(SPACE_TAG, m_Space);
    inserter.insertValue(BOUNDARY_CONDITION_TAG, static_cast<int>(m_BoundaryCondition));
    inserter.insertLevel(BUCKETING_TAG, boost::bind(
                             &CSeasonalComponentAdaptiveBucketing::acceptPersistInserter, &m_Bucketing, _1));
    inserter.insertLevel(SPLINES_TAG, boost::bind(&CPackedSplines::acceptPersistInserter, &m_Splines, _1));
}

bool CSeasonalComponent::initialized(void) const
{
    return m_Splines.initialized();
}

bool CSeasonalComponent::initialize(core_t::TTime startTime,
                                    core_t::TTime endTime,
                                    const TTimeTimePrMeanVarPrVec &values)
{
    this->clear();

    double a = 0.0;
    double b = static_cast<double>(std::min(this->time().window(),
                                            this->time().period()));
    if (!m_Bucketing.initialize(a, b, std::max(m_Space, MINIMUM_SPACE)))
    {
        LOG_ERROR("Bad inputs:"
                  << " period = " << this->time().period()
                  << ", window = " << this->time().window()
                  << ", space = " << m_Space);
        return false;
    }

    m_Bucketing.initialValues(startTime, endTime, values);

    return true;
}

void CSeasonalComponent::clear(void)
{
    if (m_Splines.initialized())
    {
        m_Splines.clear();
    }
    if (m_Bucketing.initialized())
    {
        m_Bucketing.clear();
        m_MeanValue = 0.0;
        m_MeanVariance = 0.0;
    }
}

void CSeasonalComponent::shift(double shift)
{
    m_Bucketing.shiftValue(shift);
    m_Splines.shift(CPackedSplines::E_Value, shift);
    m_MeanValue += shift;
}

void CSeasonalComponent::add(core_t::TTime time, double value, double weight)
{
    m_Bucketing.add(time, value, weight);
}

void CSeasonalComponent::interpolate(core_t::TTime time, bool refine)
{
    if (refine)
    {
        m_Bucketing.refine(time);
    }

    if (m_Bucketing.emptyBucketCount() > 0)
    {
        return;
    }

    TDoubleVec knots;
    TDoubleVec values;
    TDoubleVec variances;
    m_Bucketing.knots(time, m_BoundaryCondition, knots, values, variances);
    m_Splines.interpolate(knots, values, variances, m_BoundaryCondition);
    m_MeanValue = this->valueSpline().mean();
    m_MeanVariance = this->varianceSpline().mean();
}

double CSeasonalComponent::decayRate(void) const
{
    return m_Bucketing.decayRate();
}

void CSeasonalComponent::decayRate(double decayRate)
{
    return m_Bucketing.decayRate(decayRate);
}

void CSeasonalComponent::propagateForwardsByTime(double time, bool meanRevert)
{
    m_Bucketing.propagateForwardsByTime(time, meanRevert);
}

const CSeasonalComponent::TTime &CSeasonalComponent::time(void) const
{
    return m_Bucketing.time();
}

TDoubleDoublePr CSeasonalComponent::value(core_t::TTime time, double confidence) const
{
    // In order to compute a confidence interval we need to know
    // the distribution of the samples. In practice, as long as
    // they are independent, then the sample mean will be
    // asymptotically normal with mean equal to the sample mean
    // and variance equal to the sample variance divided by root
    // of the number of samples.

    if (this->initialized())
    {
        double time_ = this->time().periodic(time);
        double m = this->valueSpline().value(time_);
        if (confidence == 0.0)
        {
            return TDoubleDoublePr(m, m);
        }

        double n = std::max(m_Bucketing.count(time), 0.01);
        double sd = ::sqrt(std::max(this->varianceSpline().value(time_), 0.0) / n);
        if (sd == 0.0)
        {
            return TDoubleDoublePr(m, m);
        }

        try
        {
            boost::math::normal_distribution<> normal(m, sd);
            double ql = boost::math::quantile(normal, (100.0 - confidence) / 200.0);
            double qu = boost::math::quantile(normal, (100.0 + confidence) / 200.0);
            return TDoubleDoublePr(ql, qu);
        }
        catch (const std::exception &e)
        {
            LOG_ERROR("Failed calculating confidence interval: " << e.what()
                      << ", n = " << n
                      << ", m = " << m
                      << ", sd = " << sd
                      << ", confidence = " << confidence);
        }
        return TDoubleDoublePr(m, m);
    }

    return TDoubleDoublePr(m_MeanValue, m_MeanValue);
}

bool CSeasonalComponent::covariances(core_t::TTime time, TMatrix &result) const
{
    result = TMatrix(0.0);

    if (!this->initialized())
    {
        return false;
    }

    if (const CSeasonalComponentAdaptiveBucketing::TRegression *r = m_Bucketing.regression(time))
    {
        double variance = CBasicStatistics::mean(this->variance(time, 0.0));
        return r->covariances(variance, result);
    }
    return false;
}

double CSeasonalComponent::meanValue(void) const
{
    return m_MeanValue;
}

TDoubleDoublePr CSeasonalComponent::variance(core_t::TTime time, double confidence) const
{
    // In order to compute a confidence interval we need to know
    // the distribution of the samples. In practice, as long as
    // they are independent, then the sample variance will be
    // asymptotically chi-squared with number of samples minus
    // one degrees of freedom.

    if (this->initialized())
    {
        double n = std::max(m_Bucketing.count(time), 2.0);
        double v = this->varianceSpline().value(this->time().periodic(time));
        try
        {
            boost::math::chi_squared_distribution<> chi(n - 1.0);
            double ql = boost::math::quantile(chi, (100.0 - confidence) / 200.0);
            double qu = boost::math::quantile(chi, (100.0 + confidence) / 200.0);
            return TDoubleDoublePr(ql * v / (n - 1.0), qu * v / (n - 1.0));
        }
        catch (const std::exception &e)
        {
            LOG_ERROR("Failed calculating confidence interval: " << e.what()
                      << ", n = " << n
                      << ", confidence = " << confidence);
        }
        return TDoubleDoublePr(v, v);
    }
    return TDoubleDoublePr(m_MeanVariance, m_MeanVariance);
}

double CSeasonalComponent::meanVariance(void) const
{
    return m_MeanVariance;
}

CSeasonalComponent::TSplineCRef CSeasonalComponent::valueSpline(void) const
{
    return m_Splines.spline(CPackedSplines::E_Value);
}

CSeasonalComponent::TSplineCRef CSeasonalComponent::varianceSpline(void) const
{
    return m_Splines.spline(CPackedSplines::E_Variance);
}

void CSeasonalComponent::describe(const std::string &indent, std::string &result) const
{
    result += core_t::LINE_ENDING + indent + "period = "
             + core::CStringUtils::typeToStringPretty(this->time().period()) + "s ";
    if (!this->initialized())
    {
        result += "no seasonality";
        return;
    }
    result += "trend:";
    this->valueSpline().describe(indent + " ", result);
}

uint64_t CSeasonalComponent::checksum(uint64_t seed) const
{
    seed = CChecksum::calculate(seed, m_Space);
    seed = CChecksum::calculate(seed, m_BoundaryCondition);
    seed = CChecksum::calculate(seed, m_Bucketing);
    seed = CChecksum::calculate(seed, m_Splines);
    seed = CChecksum::calculate(seed, m_MeanValue);
    return CChecksum::calculate(seed, m_MeanVariance);
}

void CSeasonalComponent::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CSeasonalComponent");
    core::CMemoryDebug::dynamicSize("m_Bucketing", m_Bucketing, mem);
    core::CMemoryDebug::dynamicSize("m_Splines", m_Splines, mem);
}

std::size_t CSeasonalComponent::memoryUsage(void) const
{
    std::size_t mem = core::CMemory::dynamicSize(m_Bucketing);
    mem += core::CMemory::dynamicSize(m_Splines);
    return mem;
}

std::string CSeasonalComponent::print(void) const
{
    static const core_t::TTime NUMBER_POINTS = 50;

    std::ostringstream tt;
    std::ostringstream ff;
    std::ostringstream ww;
    tt << "t = [";
    ff << "f = [";
    ww << "w = [";

    for (core_t::TTime t = 0, dt = this->time().period() / NUMBER_POINTS;
         t <= this->time().period();
         t += dt)
    {
        TDoubleDoublePr interval = this->value(t, 50.0);
        tt << " " << t;
        ff << " " << (interval.first + interval.second) / 2.0;
        ww << " " << (interval.second - interval.first) / 2.0;

    }

    tt << "];" << core_t::LINE_ENDING;
    ff << "];" << core_t::LINE_ENDING;
    ww << "];" << core_t::LINE_ENDING << "errorbar(t, f, w);";

    return tt.str() + ff.str() + ww.str();
}

const std::size_t CSeasonalComponent::MINIMUM_SPACE(2u);


////// CSeasonalComponent::CPackedSplines //////

CSeasonalComponent::CPackedSplines::CPackedSplines(CSplineTypes::EType valueInterpolationType,
                                                   CSplineTypes::EType varianceInterpolationType)
{
    m_Types[static_cast<std::size_t>(E_Value)] = valueInterpolationType;
    m_Types[static_cast<std::size_t>(E_Variance)] = varianceInterpolationType;
}

bool CSeasonalComponent::CPackedSplines::acceptRestoreTraverser(CSplineTypes::EBoundaryCondition boundary,
                                                                core::CStateRestoreTraverser &traverser)
{
    int estimated = 0;
    TDoubleVec knots;
    TDoubleVec values;
    TDoubleVec variances;

    do
    {
        const std::string &name = traverser.name();
        RESTORE_BUILT_IN(ESTIMATED_TAG, estimated)
        RESTORE(KNOTS_TAG, core::CPersistUtils::fromString(traverser.value(), knots))
        RESTORE(VALUES_TAG, core::CPersistUtils::fromString(traverser.value(), values))
        RESTORE(VARIANCES_TAG, core::CPersistUtils::fromString(traverser.value(), variances))
    }
    while (traverser.next());

    if (estimated == 1)
    {
        this->interpolate(knots, values, variances, boundary);
    }

    return true;
}

void CSeasonalComponent::CPackedSplines::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertValue(ESTIMATED_TAG, static_cast<int>(this->initialized()));
    if (this->initialized())
    {
        inserter.insertValue(KNOTS_TAG, core::CPersistUtils::toString(m_Knots));
        inserter.insertValue(VALUES_TAG, core::CPersistUtils::toString(m_Values[0]));
        inserter.insertValue(VARIANCES_TAG, core::CPersistUtils::toString(m_Values[1]));
    }
}

void CSeasonalComponent::CPackedSplines::swap(CPackedSplines &other)
{
    std::swap(m_Types, other.m_Types);
    m_Knots.swap(other.m_Knots);
    m_Values[0].swap(other.m_Values[0]);
    m_Values[1].swap(other.m_Values[1]);
    m_Curvatures[0].swap(other.m_Curvatures[0]);
    m_Curvatures[1].swap(other.m_Curvatures[1]);
}

bool CSeasonalComponent::CPackedSplines::initialized(void) const
{
    return m_Knots.size() > 0;
}

void CSeasonalComponent::CPackedSplines::clear(void)
{
    this->spline(E_Value).clear();
    this->spline(E_Variance).clear();
}

void CSeasonalComponent::CPackedSplines::shift(ESpline spline, double shift)
{
    for (std::size_t i = 0u; i < m_Values[static_cast<std::size_t>(spline)].size(); ++i)
    {
        m_Values[static_cast<std::size_t>(spline)][i] += shift;
    }
}

CSeasonalComponent::TSplineCRef CSeasonalComponent::CPackedSplines::spline(ESpline spline) const
{
    return TSplineCRef(m_Types[static_cast<std::size_t>(spline)],
                       boost::cref(m_Knots),
                       boost::cref(m_Values[static_cast<std::size_t>(spline)]),
                       boost::cref(m_Curvatures[static_cast<std::size_t>(spline)]));
}

CSeasonalComponent::TSplineRef CSeasonalComponent::CPackedSplines::spline(ESpline spline)
{
    return TSplineRef(m_Types[static_cast<std::size_t>(spline)],
                      boost::ref(m_Knots),
                      boost::ref(m_Values[static_cast<std::size_t>(spline)]),
                      boost::ref(m_Curvatures[static_cast<std::size_t>(spline)]));
}

const CSeasonalComponent::TFloatVec &CSeasonalComponent::CPackedSplines::knots(void) const
{
    return m_Knots;
}

void CSeasonalComponent::CPackedSplines::interpolate(const TDoubleVec &knots,
                                                     const TDoubleVec &values,
                                                     const TDoubleVec &variances,
                                                     CSplineTypes::EBoundaryCondition boundary)
{
    CPackedSplines oldSpline(m_Types[0], m_Types[1]);
    this->swap(oldSpline);
    TSplineRef valueSpline = this->spline(E_Value);
    TSplineRef varianceSpline = this->spline(E_Variance);
    if (!valueSpline.interpolate(knots, values, boundary))
    {
        this->swap(oldSpline);
    }
    else if (!varianceSpline.interpolate(knots, variances, boundary))
    {
        this->swap(oldSpline);
    }
    LOG_TRACE("types = " << core::CContainerPrinter::print(m_Types));
    LOG_TRACE("knots = " << core::CContainerPrinter::print(m_Knots));
    LOG_TRACE("values = " << core::CContainerPrinter::print(m_Values));
    LOG_TRACE("curvatures = " << core::CContainerPrinter::print(m_Curvatures));
}

uint64_t CSeasonalComponent::CPackedSplines::checksum(uint64_t seed) const
{
    seed = CChecksum::calculate(seed, m_Types);
    seed = CChecksum::calculate(seed, m_Knots);
    seed = CChecksum::calculate(seed, m_Values);
    return CChecksum::calculate(seed, m_Curvatures);
}

void CSeasonalComponent::CPackedSplines::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CPackedSplines");
    core::CMemoryDebug::dynamicSize("m_Knots", m_Knots, mem);
    core::CMemoryDebug::dynamicSize("m_Values[0]", m_Values[0], mem);
    core::CMemoryDebug::dynamicSize("m_Values[1]", m_Values[1], mem);
    core::CMemoryDebug::dynamicSize("m_Curvatures[0]", m_Curvatures[0], mem);
    core::CMemoryDebug::dynamicSize("m_Curvatures[1]", m_Curvatures[1], mem);
}

std::size_t CSeasonalComponent::CPackedSplines::memoryUsage(void) const
{
    std::size_t mem = core::CMemory::dynamicSize(m_Knots);
    mem += core::CMemory::dynamicSize(m_Values[0]);
    mem += core::CMemory::dynamicSize(m_Values[1]);
    mem += core::CMemory::dynamicSize(m_Curvatures[0]);
    mem += core::CMemory::dynamicSize(m_Curvatures[1]);
    return mem;
}

}
}
