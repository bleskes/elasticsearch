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

#ifndef INCLUDED_ml_maths_CTimeSeriesDecompositionStub_h
#define INCLUDED_ml_maths_CTimeSeriesDecompositionStub_h

#include <maths/CTimeSeriesDecompositionInterface.h>
#include <maths/ImportExport.h>

namespace ml
{
namespace maths
{

//! \brief Stub out the interface if it is known that the time series
//! being modeled can't have seasonality.
//!
//! DESCRIPTION:\n
//! This is a lightweight (empty) class which implements the interface
//! for the case that the time series being modeled is known a-priori
//! not to have seasonality.
class MATHS_EXPORT CTimeSeriesDecompositionStub : public CTimeSeriesDecompositionInterface
{
    public:
        //! Clone this decomposition.
        virtual CTimeSeriesDecompositionStub *clone(void) const;

        //! No-op.
        virtual void decayRate(double decayRate);

        //! Get the decay rate.
        virtual double decayRate(void) const;

        //! No-op.
        virtual void forecast(void);

        //! Returns false.
        virtual bool initialized(void) const;

        //! Returns zero.
        virtual core_t::TTime startOfWeek(void) const;

        //! Returns maximum core_t::TTime.
        virtual core_t::TTime period(void) const;

        //! No-op returning false.
        virtual bool addPoint(core_t::TTime time,
                              double value,
                              const TWeightStyleVec &weightStyles = TWeights::COUNT,
                              const TDouble4Vec &weights = TWeights::UNIT);

        //! No-op returning false.
        virtual bool testAndInterpolate(core_t::TTime time);

        //! Returns 0.
        virtual double mean(void) const;

        //! Returns (0.0, 0.0).
        virtual TDoubleDoublePr baseline(core_t::TTime time,
                                         double confidence,
                                         EComponents components = E_All,
                                         bool smooth = true) const;

        //! Returns \p value.
        virtual double detrend(core_t::TTime time, double value, double confidence) const;

        //! Returns 0.0.
        virtual double meanVariance(void) const;

        //! Returns (1.0, 1.0).
        virtual TDoubleDoublePr scale(core_t::TTime time, double variance, double confidence) const;

        //! No-op.
        virtual void propagateForwardsTo(core_t::TTime time);

        //! Get a human readable description of the trend.
        //!
        //! \param[in] indent The indent to use at the start of new lines.
        //! \param[in,out] result Filled in with the description.
        virtual void describe(const std::string &indent, std::string &result) const;

        //! No-op.
        virtual void skipTime(core_t::TTime skipInterval);

        //! Get a checksum for this object.
        virtual uint64_t checksum(uint64_t seed = 0) const;

        //! Debug the memory used by this object.
        virtual void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const;

        //! Get the memory used by this object.
        virtual std::size_t memoryUsage(void) const;

        //! Get the static size of this object.
        virtual std::size_t staticSize(void) const;

        //! Get the seasonal components.
        virtual const TComponentVec &seasonalComponents(void) const;
};

}
}

#endif // INCLUDED_ml_maths_CTimeSeriesDecompositionStub_h
