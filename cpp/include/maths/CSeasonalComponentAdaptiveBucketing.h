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

#ifndef INCLUDED_ml_maths_CSeasonalComponentAdaptiveBucketing_h
#define INCLUDED_ml_maths_CSeasonalComponentAdaptiveBucketing_h

#include <core/CMemory.h>

#include <maths/CBasicStatistics.h>
#include <maths/CFloatStorage.h>
#include <maths/CRegression.h>
#include <maths/CSpline.h>
#include <maths/CStatisticalTests.h>
#include <maths/ImportExport.h>

#include <vector>

#include <stdint.h>


namespace ml
{
namespace core
{
class CStatePersistInserter;
class CStateRestoreTraverser;
}
namespace maths
{

//! \brief An adaptive bucketing of the value of a periodic function.
//!
//! DESCRIPTION:\n
//! This implements an adaptive bucketing strategy for the points
//! of a periodic function. The idea is to adjust the bucket end
//! points to efficiently capture the function detail in a fixed
//! number of buckets. Function values are assumed to have additive
//! noise. In particular, it is assumed that the observed values
//! \f$(x_i, y_i)\f$ are described by:
//! <pre class="fragment">
//!   \f$\{(x_i, y_i = y(x_i) + Y_i\}\f$
//! </pre>
//!
//! Here, \f$Y_i\f$ are IID mean zero random variables. We are
//! interested in spacing the buckets to minimize the maximum
//! error in approximating the function by its mean in each bucket,
//! i.e. we'd like to minimize:
//! <pre class="fragment">
//!   \f$\displaystyle \max_i\left\{ \int_{[a_i,b_i]}{ \left| y(x) - \left<y\right>_{[a_i,b_i]} \right| }dx \right\} \f$
//! </pre>
//!
//! Here, \f$\left<y\right>_{[a_i,b_i]} = \frac{1}{b_i-a_i}\int_{[a_i,b_i]}{y(x)}dx\f$.
//! It is relatively straightforward to show that if the points are
//! uniformly distributed in the function domain then the mean in
//! each bucket is a unbiased esimator of \f$\left<y\right>\f$ in that
//! bucket. We estimate the error by using the mean smoothed central
//! range of the function in each bucket, given by difference between
//! adjacent function bucket means. The smoothing is achieved by
//! convolution. (This empirically gives better results for smooth
//! functions and is also beneficial for spline interpolation where
//! it is desirable to increase the number of knots _near_ regions
//! of high curvature to control the function.)
//!
//! For sufficiently smooth functions and a given number of buckets
//! the objective is minimized by ensuring that "bucket width" x
//! "function range" is approximately equal in all buckets.
//!
//! The bucketing is aged by relaxing it back towards uniform and
//! aging the counts of the mean value for each bucket as usual.
//!
//! IMPLEMENTATION DECISIONS:\n
//! This class uses float storage for the bucket mean values. This
//! is because it is intended for use in cases where space is at a
//! premium. *DO NOT* use floats unless doing so gives a significant
//! overall space improvement to the *program* footprint. Note also
//! that the interface to this class is double precision. If floats
//! are used they should be used for storage only and transparent to
//! the rest of the code base.
class MATHS_EXPORT CSeasonalComponentAdaptiveBucketing
{
    public:
        typedef std::vector<double> TDoubleVec;
        typedef std::vector<std::size_t> TSizeVec;
        typedef std::vector<CFloatStorage> TFloatVec;
        typedef CRegression::CLeastSquaresOnline<1, double> TDoubleRegression;
        typedef CRegression::CLeastSquaresOnline<1, CFloatStorage> TRegression;
        typedef std::vector<TRegression> TRegressionVec;
        typedef std::pair<core_t::TTime, core_t::TTime> TTimeTimePr;
        typedef CBasicStatistics::SSampleMean<double>::TAccumulator TDoubleMeanAccumulator;
        typedef CBasicStatistics::SSampleMean<CFloatStorage>::TAccumulator TFloatMeanAccumulator;
        typedef std::vector<TFloatMeanAccumulator> TFloatMeanAccumulatorVec;
        typedef CBasicStatistics::SSampleMeanVar<double>::TAccumulator TDoubleMeanVarAccumulator;
        typedef std::pair<TTimeTimePr, TDoubleMeanVarAccumulator> TTimeTimePrMeanVarPr;
        typedef std::vector<TTimeTimePrMeanVarPr> TTimeTimePrMeanVarPrVec;

        //! \brief Provides times to the adaptive bucketing
        class MATHS_EXPORT CTime
        {
            public:
                CTime(void);
                CTime(core_t::TTime startOfWeek,
                      core_t::TTime windowStart,
                      core_t::TTime windowEnd,
                      core_t::TTime period);

                //! Initialize from a string created by persist.
                bool restore(const std::string &value);

                //! Convert to a string.
                std::string persist(void) const;

                //! Extract the time of \p time in the current period.
                double periodic(core_t::TTime time) const;

                //! Extract the time of \p time in the current regression.
                double regression(core_t::TTime time) const;

                //! Get the start of the week.
                core_t::TTime weekStart(void) const;

                //! Get the start of the week containing \p time.
                core_t::TTime startOfWeek(core_t::TTime time) const;

                //! Check if \p time is in the window.
                bool inWindow(core_t::TTime time) const;

                //! Get the window.
                core_t::TTime window(void) const;

                //! Get the start of the window.
                core_t::TTime windowStart(void) const;

                //! Get the end of the window.
                core_t::TTime windowEnd(void) const;

                //! Get the start of the window containing \p time.
                core_t::TTime startOfWindow(core_t::TTime time) const;

                //! Get the period.
                core_t::TTime period(void) const;

                //! Get the origin of the time coordinates.
                core_t::TTime regressionOrigin(void) const;

                //! Set the origin of the time coordinates.
                void regressionOrigin(core_t::TTime time);

                //! Get the decay rate scaled from \p fromPeriod period to
                //! \p toPeriod period.
                //!
                //! \param[in] decayRate The decay rate for \p fromPeriod.
                //! \param[in] fromPeriod The period of \p decayRate.
                //! \param[in] toPeriod The desired period decay rate.
                static double scaleDecayRate(double decayRate,
                                             core_t::TTime fromPeriod,
                                             core_t::TTime toPeriod);

                //! Get a checksum for this object.
                uint64_t checksum(uint64_t seed = 0) const;

            private:
                //! The start of the week.
                core_t::TTime m_StartOfWeek;
                //! The start of the window.
                core_t::TTime m_WindowStart;
                //! The end of the window.
                core_t::TTime m_WindowEnd;
                //! The periodic repeat.
                core_t::TTime m_Period;
                //! The origin of the time coordinates used to maintain
                //! a reasonably conditioned Gramian of the design matrix.
                core_t::TTime m_RegressionOrigin;
        };

    public:
        CSeasonalComponentAdaptiveBucketing(const CTime &time,
                                            double decayRate = 0.0,
                                            double minimumBucketLength = 0.0);

        //! Construct by traversing a state document
        CSeasonalComponentAdaptiveBucketing(double decayRate,
                                            double minimumBucketLength,
                                            core::CStateRestoreTraverser &traverser);

        //! Persist by passing information to the supplied inserter.
        void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

        //! Efficiently swap the contents of two bucketing objects.
        void swap(CSeasonalComponentAdaptiveBucketing &other);

        //! Get time between propagating forwards by one unit of time.
        static core_t::TTime timescale(void);

        //! Check if the bucketing has been initialized.
        bool initialized(void) const;

        //! Create a new uniform bucketing with \p n buckets on the
        //! interval [\p a, \p b].
        //!
        //! \param[in] a The start of the interval to bucket.
        //! \param[in] b The end of the interval to bucket.
        //! \param[in] n The number of buckets.
        bool initialize(double a, double b, std::size_t n);

        //! Add the function moments \f$([a_i,b_i], S_i)\f$ where
        //! \f$S_i\f$ are the means and variances of the function
        //! in the time intervals \f$([a_i,b_i])\f$.
        //!
        //! \param[in] startTime The start of the period including \p values.
        //! \param[in] endTime The end of the period including \p values.
        //! \param[in] values Time ranges and the corresponding function
        //! value moments.
        void initialValues(core_t::TTime startTime,
                           core_t::TTime endTime,
                           const TTimeTimePrMeanVarPrVec &values);

        //! Get the number of buckets.
        std::size_t size(void) const;

        //! Clear the contents of this bucketing and recover any
        //! allocated memory.
        void clear(void);

        //! Shift the regressions' ordinates by \p shift.
        void shiftValue(double shift);

        //! Add the function value at \p time.
        //!
        //! \param[in] time The time of \p value.
        //! \param[in] value The value of the function at \p time.
        //! \param[in] weight The weight of function point. The smaller
        //! this is the less influence it has on the bucket.
        void add(core_t::TTime time, double value, double weight = 1.0);

        //! Get the time provider.
        const CTime &time(void) const;

        //! Set the rate at which the bucketing loses information.
        void decayRate(double value);

        //! Get the rate at which the bucketing loses information.
        double decayRate(void) const;

        //! Age the bucket values to account for \p time elapsed time.
        void propagateForwardsByTime(double time, bool meanRevert = false);

        //! Refine the bucket end points to minimize the maximum averaging
        //! error in any bucket.
        //!
        //! \param[in] time The time at which to refine.
        void refine(core_t::TTime time);

        //! The count in the bucket containing \p time.
        double count(core_t::TTime time) const;

        //! Get the count of buckets with no values.
        std::size_t emptyBucketCount(void) const;

        //! Get the regression to use at \p time.
        const TRegression *regression(core_t::TTime time) const;

        //! Get a set of knot points and knot point values to use for
        //! interpolating the bucket values.
        //!
        //! \param[in] time The time at which to get the knot points.
        //! \param[in] boundary Controls the style of start and end knots.
        //! \param[out] knots Filled in with the knot points to interpolate.
        //! \param[out] values Filled in with the values at \p knots.
        //! \param[out] variances Filled in with the variances at \p knots.
        void knots(core_t::TTime time,
                   CSplineTypes::EBoundaryCondition boundary,
                   TDoubleVec &knots,
                   TDoubleVec &values,
                   TDoubleVec &variances) const;

        //! Get a checksum for this object.
        uint64_t checksum(uint64_t seed = 0) const;

        //! Get the memory used by this component
        void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const;

        //! Get the memory used by this component
        std::size_t memoryUsage(void) const;

        //! \name Test Functions
        //@{
        //! Get the bucket end points.
        const TFloatVec &endpoints(void) const;

        //! Get the total count of in the bucketing.
        double count(void) const;

        //! Get the bucket regressions.
        TDoubleVec values(core_t::TTime time) const;

        //! Get the bucket variances.
        TDoubleVec variances(void) const;
        //@}

    private:
        typedef CBasicStatistics::COrderStatisticsStack<double, 1> TMinAccumulator;
        typedef CBasicStatistics::COrderStatisticsStack<double, 1, std::greater<double> > TMaxAccumulator;

    private:
        //! Restore by traversing a state document
        bool acceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

        //! Compute the index of the bucket to which \p time belongs
        bool bucket(core_t::TTime time, std::size_t &result) const;

        //! Compute the values corresponding to the change in end
        //! points from \p endpoints. The values are assigned based
        //! on their intersection with each bucket in the previous
        //! bucket configuration.
        //!
        //! \param[in] endpoints The old end points.
        void refresh(const TFloatVec &endpoints);

        //! Shift the regressions' abscissas by minus \p time.
        void shiftTime(core_t::TTime time);

        //! Get the age of the bucketing at \p time.
        double bucketingAgeAt(core_t::TTime time) const;

    private:
        //! The time provider.
        CTime m_Time;

        //! The time that the bucketing was initialized.
        core_t::TTime m_InitialTime;

        //! The rate at which information is aged out of the bucket values.
        double m_DecayRate;

        //! The minimum permitted bucket length if non-zero otherwise this
        //! is ignored.
        double m_MinimumBucketLength;

        //! The bucket end points.
        TFloatVec m_Endpoints;

        //! The bucket regressions.
        TRegressionVec m_Regressions;

        //! The bucket variances.
        TFloatVec m_Variances;

        //! An IIR low pass filter for the total desired end point displacement
        //! in refine.
        TFloatMeanAccumulator m_LpForce;

        //! The total desired end point displacement in refine.
        TFloatMeanAccumulator m_Force;
};

}
}

#endif // INCLUDED_ml_maths_CSeasonalComponentAdaptiveBucketing_h
