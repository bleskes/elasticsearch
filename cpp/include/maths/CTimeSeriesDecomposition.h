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

#ifndef INCLUDED_prelert_maths_CTimeSeriesDecomposition_h
#define INCLUDED_prelert_maths_CTimeSeriesDecomposition_h

#include <maths/CBasicStatistics.h>
#include <maths/CTimeSeriesDecompositionDetail.h>
#include <maths/CTimeSeriesDecompositionInterface.h>
#include <maths/ImportExport.h>

#include <boost/shared_ptr.hpp>

class CTimeSeriesDecompositionTest;

namespace prelert
{
namespace core
{
class CStatePersistInserter;
class CStateRestoreTraverser;
}
namespace maths
{

//! \brief Decomposes a time series into a linear combination
//! of periodic functions and a stationary random process.
//!
//! DESCRIPTION:\n
//! This manages the decomposition of a times series into a linear
//! combination of periodic terms. In particular, it assumes that
//! a set of time series points, comprising the set of pairs
//! \f$\{(t, f(t))\}\f$, is described by:
//! <pre class="fragment">
//!   \f$f(t) = \sum_i{ g_i(t | T_i) } + R\f$
//! </pre>
//!
//! Here, \f$g_i(t | T_i)\f$ denotes an arbitrary periodic function
//! with period \f$T_i\f$, i.e.
//! <pre class="fragment">
//!   \f$g_i(t | T_i) = g_i(t + T_i | T_i)\f$
//! </pre>
//!
//! and \f$R\f$ is a stationary random process, i.e. its distribution
//! doesn't change over (short) time periods.
//!
//! By default this assumes the data has one day and one week
//! periodicity, i.e. \f${ T_i } = { 86400, 604800 }\f$.
class MATHS_EXPORT CTimeSeriesDecomposition : public CTimeSeriesDecompositionInterface,
                                              private CTimeSeriesDecompositionDetail
{
    public:
        typedef std::vector<std::size_t> TSizeVec;
        typedef std::vector<CSeasonalComponent> TSeasonalComponentVec;
        typedef CBasicStatistics::COrderStatisticsStack<double, 1u> TMinAccumulator;
        typedef CBasicStatistics::COrderStatisticsStack<double, 1u, std::greater<double> > TMaxAccumulator;

    public:
        //! The default size to use for the seasonal components.
        static const std::size_t DEFAULT_COMPONENT_SIZE;

    public:
        //! \param[in] decayRate The rate at which information is lost.
        //! \param[in] bucketLength The data bucketing length.
        //! \param[in] seasonalComponentSize The number of buckets to
        //! use estimate a seasonal component.
        explicit CTimeSeriesDecomposition(double decayRate = 0.0,
                                          core_t::TTime bucketLength = 0,
                                          std::size_t seasonalComponentSize = DEFAULT_COMPONENT_SIZE);

        //! Construct from part of a state document.
        CTimeSeriesDecomposition(double decayRate,
                                 core_t::TTime bucketLength,
                                 std::size_t seasonalComponentSize,
                                 core::CStateRestoreTraverser &traverser);

        //! Deep copy.
        CTimeSeriesDecomposition(const CTimeSeriesDecomposition &other);

        //! An efficient swap of the state of this and \p other.
        void swap(CTimeSeriesDecomposition &other);

        //! Assign this object (using deep copy).
        CTimeSeriesDecomposition &operator=(const CTimeSeriesDecomposition &other);

        //! Persist state by passing information to the supplied inserter.
        void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

        //! Clone this decomposition.
        virtual CTimeSeriesDecomposition *clone(void) const;

        //! Set the decay rate.
        virtual void decayRate(double decayRate);

        //! Get the decay rate.
        virtual double decayRate(void) const;

        //! Start forecasting.
        virtual void forecast(void);

        //! Check if the decomposition has any initialized components.
        virtual bool initialized(void) const;

        //! Get the start of the week.
        virtual core_t::TTime startOfWeek(void) const;

        //! Get the period.
        virtual core_t::TTime period(void) const;

        //! Adds a time series point \f$(t, f(t))\f$.
        //!
        //! \param[in] time The time of the function point.
        //! \param[in] value The function value at \p time.
        //! \param[in] weightStyles The styles of \p weights. Both the count
        //! and the Winsorisation weight styles have an effect. See also
        //! maths_t::ESampleWeightStyle for more details.
        //! \param[in] weights The weights of \p value. The smaller
        //! the count weight the less influence \p value has on the trend
        //! and it's local variance.
        //! \return True if number of estimated components changed
        //! and false otherwise.
        virtual bool addPoint(core_t::TTime time,
                              double value,
                              const TWeightStyleVec &weightStyles = TWeights::COUNT,
                              const TDouble4Vec &weights = TWeights::UNIT);

        //! May be test to see if there are any new seasonal components
        //! and interpolate.
        //!
        //! \param[in] time The current time.
        //! \return True if the number of seasonal components changed
        //! and false otherwise.
        virtual bool testAndInterpolate(core_t::TTime time);

        //! Get the mean value of the baseline.
        virtual double mean(void) const;

        //! Get the value of the time series baseline at \p time.
        //!
        //! \param[in] time The time of interest.
        //! \param[in] confidence The symmetric confidence interval for the
        //! baseline as a percentage.
        //! \param[in] components The components to include in the baseline.
        virtual TDoubleDoublePr baseline(core_t::TTime time,
                                         double confidence,
                                         EComponents components = E_All,
                                         bool smooth = true) const;

        //! Detrend \p value from the time series being modeled by removing
        //! any trend and periodic component at \p time.
        virtual double detrend(core_t::TTime time, double value, double confidence) const;

        //! Get the mean variance of the baseline.
        virtual double meanVariance(void) const;

        //! Compute the variance scale at \p time.
        //!
        //! \param[in] time The time of interest.
        //! \param[in] variance The variance of the distribution
        //! to scale.
        //! \param[in] confidence The symmetric confidence interval
        //! for the variance scale as a percentage.
        virtual TDoubleDoublePr scale(core_t::TTime time, double variance, double confidence) const;

        //! Propagate the decomposition forwards to \p time.
        virtual void propagateForwardsTo(core_t::TTime time);

        //! Get a human readable description of the trend.
        //!
        //! \param[in] indent The indent to use at the start of new lines.
        //! \param[in,out] result Filled in with the description.
        virtual void describe(const std::string &indent, std::string &result) const;

        //! Roll time forwards by \p skipInterval.
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

    private:
        typedef boost::shared_ptr<CMediator> TMediatorPtr;

    private:
        //! Set up the communication mediator.
        void initializeMediator(void);

        //! Create from part of a state document.
        bool acceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

        //! The correction to produce a smooth join between baselines.
        TDoubleDoublePr smoothing(core_t::TTime time, double confidence) const;

    private:
        //! The time over which discontinuities between weekdays
        //! and weekends are smoothed out.
        static const core_t::TTime SMOOTHING_INTERVAL;

    private:
        //! Controls the rate at which information is lost.
        double m_DecayRate;

        //! The time of the latest point added so far.
        core_t::TTime m_LastTime;

        //! The time of the last propagate forwards by time.
        core_t::TTime m_LastPropagateForwardsTime;

        //! Handles the communication between the various tests and
        //! components.
        TMediatorPtr m_Mediator;

        //! The test for a large time scale smooth trend.
        CLongTermTrendTest m_LongTermTrendTest;

        //! The test for daily and weekly periodic components.
        CDailyWeeklyTest m_DailyWeeklyTest;

        //! The state for modeling the components of the decomposition.
        CComponents m_Components;

        friend class ::CTimeSeriesDecompositionTest;
};

}
}

#endif // INCLUDED_prelert_maths_CTimeSeriesDecomposition_h
