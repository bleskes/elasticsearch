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

#include <maths/CTimeSeriesDecompositionStub.h>

#include <maths/CSeasonalComponent.h>

namespace prelert
{
namespace maths
{

CTimeSeriesDecompositionStub *CTimeSeriesDecompositionStub::clone(void) const
{
    return new CTimeSeriesDecompositionStub(*this);
}

void CTimeSeriesDecompositionStub::decayRate(double /*decayRate*/)
{
}

double CTimeSeriesDecompositionStub::decayRate(void) const
{
    return 0.0;
}

void CTimeSeriesDecompositionStub::forecast(void)
{
}

bool CTimeSeriesDecompositionStub::initialized(void) const
{
    return false;
}

core_t::TTime CTimeSeriesDecompositionStub::startOfWeek(void) const
{
    return 0;
}

core_t::TTime CTimeSeriesDecompositionStub::period(void) const
{
    return std::numeric_limits<core_t::TTime>::max();
}

bool CTimeSeriesDecompositionStub::addPoint(core_t::TTime /*time*/,
                                            double /*value*/,
                                            const TWeightStyleVec &/*weightStyles*/,
                                            const TDouble4Vec &/*weights*/)
{
    return false;
}

bool CTimeSeriesDecompositionStub::testAndInterpolate(core_t::TTime /*time*/)
{
    return false;
}

double CTimeSeriesDecompositionStub::mean(void) const
{
    return 0.0;
}

double CTimeSeriesDecompositionStub::level(void) const
{
    return 0.0;
}

CTimeSeriesDecompositionStub::TDoubleDoublePr
    CTimeSeriesDecompositionStub::baseline(core_t::TTime /*time*/,
                                           double /*confidence*/,
                                           bool /*smooth*/) const
{
    return TDoubleDoublePr(0.0, 0.0);
}

double CTimeSeriesDecompositionStub::detrend(core_t::TTime /*time*/,
                                             double value,
                                             double /*confidence*/) const
{
    return value;
}

double CTimeSeriesDecompositionStub::meanVariance(void) const
{
    return 0.0;
}

CTimeSeriesDecompositionStub::TDoubleDoublePr
    CTimeSeriesDecompositionStub::scale(core_t::TTime /*time*/,
                                        double /*variance*/,
                                        double /*confidence*/) const
{
    return TDoubleDoublePr(1.0, 1.0);
}

void CTimeSeriesDecompositionStub::propagateForwardsTo(core_t::TTime /*time*/)
{
}

void CTimeSeriesDecompositionStub::describe(const std::string &/*indent*/,
                                            std::string &result) const
{
    result = "-";
}

void CTimeSeriesDecompositionStub::skipTime(core_t::TTime /*skipInterval*/)
{
}

uint64_t CTimeSeriesDecompositionStub::checksum(uint64_t seed) const
{
    return seed;
}

void CTimeSeriesDecompositionStub::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CTimeSeriesDecompositionStub");
}

std::size_t CTimeSeriesDecompositionStub::memoryUsage(void) const
{
    return 0;
}

std::size_t CTimeSeriesDecompositionStub::staticSize(void) const
{
    return sizeof(*this);
}

const CTimeSeriesDecompositionStub::TComponentVec &
    CTimeSeriesDecompositionStub::seasonalComponents(void) const
{
    static const TComponentVec NO_COMPONENTS;
    return NO_COMPONENTS;
}

}
}
