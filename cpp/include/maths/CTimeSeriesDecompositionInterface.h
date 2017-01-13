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

#ifndef INCLUDED_ml_maths_CTimeSeriesDecompositionInterface_h
#define INCLUDED_ml_maths_CTimeSeriesDecompositionInterface_h

#include <core/CMemory.h>
#include <core/CoreTypes.h>
#include <core/CSmallVector.h>

#include <maths/Constants.h>
#include <maths/ImportExport.h>
#include <maths/MathsTypes.h>

#include <cstddef>
#include <string>
#include <utility>
#include <vector>
#include <stdint.h>

namespace ml
{
namespace maths
{
class CMultivariatePrior;
class CPrior;
class CSeasonalComponent;

//! \brief A collection of typedefs to be used by the time series
//! decomposition hierarchy.
class MATHS_EXPORT CTimeSeriesDecompositionTypedefs
{
    public:
        typedef std::pair<double, double> TDoubleDoublePr;
        typedef core::CSmallVector<double, 4> TDouble4Vec;
        typedef maths_t::TWeightStyleVec TWeightStyleVec;
        typedef std::vector<CSeasonalComponent> TComponentVec;
};

//! \brief The interface for decomposing times series into periodic,
//! calendar periodic and trend components.
class MATHS_EXPORT CTimeSeriesDecompositionInterface : virtual public CTimeSeriesDecompositionTypedefs
{
    public:
        typedef CConstantWeights TWeights;

        enum EComponents
        {
            E_Seasonal = 0x1,
            E_Trend    = 0x2,
            E_All      = 0x3
        };

    public:
        virtual ~CTimeSeriesDecompositionInterface(void);

        //! Clone this decomposition.
        virtual CTimeSeriesDecompositionInterface *clone(void) const = 0;

        //! Set the decay rate.
        virtual void decayRate(double decayRate) = 0;

        //! Get the decay rate.
        virtual double decayRate(void) const = 0;

        //! Use this trend to forecast.
        //!
        //! \warning This is an irreversible action so if the trend
        //! is still need it should be copied first.
        virtual void forecast(void) = 0;

        //! Check if this is initialized.
        virtual bool initialized(void) const = 0;

        //! Get the period.
        virtual core_t::TTime period(void) const = 0;

        //! Adds a time series point \f$(t, f(t))\f$.
        //!
        //! \param[in] time The time of the function point.
        //! \param[in] value The function value at \p time.
        //! \param[in] weightStyles The styles of \p weights. Both the
        //! count and the Winsorisation weight styles have an effect.
        //! See maths_t::ESampleWeightStyle for more details.
        //! \param[in] weights The weights of \p value. The smaller
        //! the product count weight the less influence \p value has
        //! on the trend and it's local variance.
        //! \return True if number of estimated components changed
        //! and false otherwise.
        virtual bool addPoint(core_t::TTime time,
                              double value,
                              const TWeightStyleVec &weightStyles = TWeights::COUNT,
                              const TDouble4Vec &weights = TWeights::UNIT) = 0;

        //! May be test to see if there are any new seasonal components
        //! and interpolate.
        //!
        //! \param[in] time The current time.
        //! \return True if the number of seasonal components changed
        //! and false otherwise.
        virtual bool testAndInterpolate(core_t::TTime time) = 0;

        //! Get the mean value of the baseline.
        virtual double mean(void) const = 0;

        //! Get the value of the time series baseline at \p time.
        //!
        //! \param[in] time The time of interest.
        //! \param[in] confidence The symmetric confidence interval for the
        //! baseline as a percentage.
        //! \param[in] components The components to include in the baseline.
        virtual TDoubleDoublePr baseline(core_t::TTime time,
                                         double confidence,
                                         EComponents components = E_All,
                                         bool smooth = true) const = 0;

        //! Detrend \p value from the time series being modeled by removing
        //! any periodic component at \p time.
        //!
        //! \note That detrending preserves the time series mean.
        virtual double detrend(core_t::TTime time, double value, double confidence) const = 0;

        //! Get the mean variance of the baseline.
        virtual double meanVariance(void) const = 0;

        //! Compute the variance scale at \p time.
        //!
        //! \param[in] time The time of interest.
        //! \param[in] variance The variance of the distribution to scale.
        //! \param[in] confidence The symmetric confidence interval for the
        //! variance scale as a percentage.
        virtual TDoubleDoublePr scale(core_t::TTime time, double variance, double confidence) const = 0;

        //! Propagate the state forwards to \p time.
        virtual void propagateForwardsTo(core_t::TTime time) = 0;

        //! Get a human readable description of the trend.
        std::string describe(void) const;

        //! Get a human readable description of the trend.
        //!
        //! \param[in] indent The indent to use at the start of new lines.
        //! \param[in,out] result Filled in with the description.
        virtual void describe(const std::string &indent, std::string &result) const = 0;

        //! Roll time forwards by \p skipInterval.
        virtual void skipTime(core_t::TTime skipInterval) = 0;

        //! Get a checksum for this object.
        virtual uint64_t checksum(uint64_t seed = 0) const = 0;

        //! Get the memory used by this instance
        virtual void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const = 0;

        //! Get the memory used by this instance
        virtual std::size_t memoryUsage(void) const = 0;

        //! Get the static size of this object.
        virtual std::size_t staticSize(void) const = 0;

        //! Get the seasonal components.
        virtual const TComponentVec &seasonalComponents(void) const = 0;
};

typedef boost::shared_ptr<maths::CTimeSeriesDecompositionInterface> TDecompositionPtr;
typedef std::vector<TDecompositionPtr> TDecompositionPtrVec;

//! Initialize a univariate prior to match the moments of \p decomposition.
MATHS_EXPORT
bool initializePrior(core_t::TTime bucketLength,
                     double learnRate,
                     const CTimeSeriesDecompositionInterface &decomposition,
                     CPrior &prior);

//! Initialize a multivariate prior to match the moments of \p decomposition.
MATHS_EXPORT
bool initializePrior(core_t::TTime bucketLength,
                     double learnRate,
                     const TDecompositionPtrVec &decomposition,
                     CMultivariatePrior &prior);

}
}

#endif // INCLUDED_ml_maths_CTimeSeriesDecompositionInterface_h
