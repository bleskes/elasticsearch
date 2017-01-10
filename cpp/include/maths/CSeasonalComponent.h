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

#ifndef INCLUDED_ml_maths_CSeasonalComponent_h
#define INCLUDED_ml_maths_CSeasonalComponent_h

#include <core/CMemory.h>
#include <core/CoreTypes.h>

#include <maths/CSeasonalComponentAdaptiveBucketing.h>
#include <maths/CSpline.h>
#include <maths/ImportExport.h>

#include <boost/optional.hpp>
#include <boost/ref.hpp>

#include <cstddef>
#include <vector>

namespace ml
{
namespace core
{
class CStatePersistInserter;
class CStateRestoreTraverser;
}
namespace maths
{

//! \brief Estimates the seasonal component of a time series.
//!
//! DESCRIPTION:\n
//! This uses an adaptive bucketing strategy to compute the mean
//! value of a function in various subintervals of its period.
//! The intervals are adjusted to minimize the maximum averaging
//! error in any bucket (see CSeasonalComponentAdaptiveBucketing
//! for more details). Estimates of the true function values are
//! obtained by interpolating the bucket values (using cubic
//! spline).
//!
//! The bucketing is aged by relaxing it back towards uniform and
//! aging the counts of the mean value for each bucket as usual.
class MATHS_EXPORT CSeasonalComponent
{
    public:
        typedef std::pair<double, double> TDoubleDoublePr;
        typedef std::vector<double> TDoubleVec;
        typedef std::vector<core_t::TTime> TTimeVec;
        typedef CBasicStatistics::SSampleMeanVar<double>::TAccumulator TMeanVarAccumulator;
        typedef std::pair<core_t::TTime, core_t::TTime> TTimeTimePr;
        typedef std::pair<TTimeTimePr, TMeanVarAccumulator> TTimeTimePrMeanVarPr;
        typedef std::vector<TTimeTimePrMeanVarPr> TTimeTimePrMeanVarPrVec;
        typedef std::vector<CFloatStorage> TFloatVec;
        typedef CSeasonalComponentAdaptiveBucketing::CTime TTime;
        typedef CSymmetricMatrixNxN<double, 2> TMatrix;
        typedef CSpline<boost::reference_wrapper<const TFloatVec>,
                        boost::reference_wrapper<const TFloatVec>,
                        boost::reference_wrapper<const TDoubleVec> > TSplineCRef;
        typedef CSpline<boost::reference_wrapper<TFloatVec>,
                        boost::reference_wrapper<TFloatVec>,
                        boost::reference_wrapper<TDoubleVec> > TSplineRef;

    public:
        //! \param[in] time The time provider.
        //! \param[in] space The space used to model the component if non-zero. Otherwise,
        //! the component will not be initialized and the client code must explicitly call
        //! initialize member function to start estimating the component.
        //! \param[in] decayRate Controls the rate at which information is lost from
        //! its adaptive bucketing.
        //! \param[in] minimumBucketLength The minimum bucket length permitted in the
        //! adaptive bucketing.
        //! \param[in] boundaryCondition The boundary condition to use for the splines.
        //! \param[in] valueInterpolationType The style of interpolation to use for
        //! computing values.
        //! \param[in] varianceInterpolationType The style of interpolation to use for
        //! computing variances.
        CSeasonalComponent(const TTime &time,
                           std::size_t space,
                           double decayRate = 0.0,
                           double minimumBucketLength = 0.0,
                           CSplineTypes::EBoundaryCondition boundaryCondition = CSplineTypes::E_Periodic,
                           CSplineTypes::EType valueInterpolationType = CSplineTypes::E_Cubic,
                           CSplineTypes::EType varianceInterpolationType = CSplineTypes::E_Linear);

        //! Construct by traversing part of an state document.
        CSeasonalComponent(double decayRate,
                           double minimumBucketLength,
                           core::CStateRestoreTraverser &traverser,
                           CSplineTypes::EType valueInterpolationType = CSplineTypes::E_Cubic,
                           CSplineTypes::EType varianceInterpolationType = CSplineTypes::E_Linear);

        //! An efficient swap of the contents of two components.
        void swap(CSeasonalComponent &other);

        //! Persist state by passing information to \p inserter.
        void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

        //! Check if the seasonal component has been estimated.
        bool initialized(void) const;

        //! Initialized the adaptive bucketing.
        bool initialize(core_t::TTime startTime,
                        core_t::TTime endTime,
                        const TTimeTimePrMeanVarPrVec &values);

        //! Clear all data.
        void clear(void);

        //! Shift the component by \p shift.
        void shift(double shift);

        //! Adds a function value \f$(t, f(t))\f$ to this component.
        //!
        //! \param[in] time The time of the function point.
        //! \param[in] value The function value at \p time.
        //! \param[in] weight The weight of \p value. The smaller this is the less
        //! influence it has on the component.
        void add(core_t::TTime time, double value, double weight = 1.0);

        //! Update the interpolation of the bucket values.
        //!
        //! \param[in] time The time at which to interpolate.
        //! \param[in] refine If false disable refining the bucketing.
        void interpolate(core_t::TTime time, bool refine = true);

        //! Get the rate at which the seasonal component loses information.
        double decayRate(void) const;

        //! Set the rate at which the seasonal component loses information.
        void decayRate(double decayRate);

        //! Update the regression coefficients and basis function centres for the
        //! specified elapsed time.
        void propagateForwardsByTime(double time, bool meanRevert = false);

        //! Get the time provider.
        const TTime &time(void) const;

        //! Interpolate the function at \p time.
        //!
        //! \param[in] time The time of interest.
        //! \param[in] confidence The symmetric confidence interval for the variance
        //! as a percentage.
        TDoubleDoublePr value(core_t::TTime time, double confidence) const;

        //! Get the covariance matrix of the regression parameters' at \p time.
        //!
        //! \param[in] time The time of interest.
        //! \param[out] result Filled in with the regression parameters'
        //! covariance matrix.
        bool covariances(core_t::TTime time, TMatrix &result) const;

        //! Get the mean value of the function.
        double meanValue(void) const;

        //! Get the variance of the residual about the function at \p time.
        //!
        //! \param[in] time The time of interest.
        //! \param[in] confidence The symmetric confidence interval for the
        //! variance as a percentage.
        TDoubleDoublePr variance(core_t::TTime time, double confidence) const;

        //! Get the mean variance of the function residuals.
        double meanVariance(void) const;

        //! Get the value spline.
        TSplineCRef valueSpline(void) const;

        //! Get the variance spline.
        TSplineCRef varianceSpline(void) const;

        //! Get a human readable description of this component.
        //!
        //! \param[in] indent The indent to use at the start of new lines.
        //! \param[in,out] result Filled in with the description.
        void describe(const std::string &indent, std::string &result) const;

        //! Get a checksum for this object.
        uint64_t checksum(uint64_t seed = 0) const;

        //! Debug the memory used by this component.
        void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const;

        //! Get the memory used by this component.
        std::size_t memoryUsage(void) const;

        //! Print the best fit function at regular intervals.
        //!
        //! The format is as follows:\n
        //! \code{cpp}
        //!    t = [t1 t2 ... tn ];
        //!    f = [f(t1) f(t2) ... f(tn) ];
        //!    w = [w(t1) w(t2) ... w(tn) ];
        //! \endcode
        //!
        //! i.e. times are space separated on the first line and the best
        //! fit function evaluated at those times are space separated on
        //! the next line. The widths are the 50% confidence intervals.
        std::string print(void) const;

    private:
        //! \brief A low memory representation of the value and variance splines.
        class CPackedSplines
        {
            public:
                enum ESpline
                {
                    E_Value    = 0,
                    E_Variance = 1
                };

            public:
                typedef boost::array<CSplineTypes::EType, 2> TTypeArray;
                typedef boost::array<TFloatVec, 2> TFloatVecArray;
                typedef boost::array<TDoubleVec, 2> TDoubleVecArray;

            public:
                CPackedSplines(CSplineTypes::EType valueInterpolationType,
                               CSplineTypes::EType varianceInterpolationType);

                //! Create by traversing a state document.
                bool acceptRestoreTraverser(CSplineTypes::EBoundaryCondition boundary,
                                            core::CStateRestoreTraverser &traverser);

                //! Persist state by passing information to \p inserter.
                void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

                //! An efficient swap of the contents of two packed splines.
                void swap(CPackedSplines &other);

                //! Check if the splines have been initialized.
                bool initialized(void) const;

                //! Clear the splines.
                void clear(void);

                //! Shift the spline values by \p shift.
                void shift(ESpline spline, double shift);

                //! Get a constant spline reference.
                TSplineCRef spline(ESpline spline) const;

                //! Get a writable spline reference.
                TSplineRef spline(ESpline spline);

                //! Get the splines' knot points.
                const TFloatVec &knots(void) const;

                //! Interpolate the value and variance functions on \p knots.
                void interpolate(const TDoubleVec &knots,
                                 const TDoubleVec &values,
                                 const TDoubleVec &variances,
                                 CSplineTypes::EBoundaryCondition boundary);

                //! Get a checksum for this object.
                uint64_t checksum(uint64_t seed) const;

                //! Debug the memory used by the splines.
                void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const;

                //! Get the memory used by these splines.
                std::size_t memoryUsage(void) const;

            private:
                //! The splines' types.
                TTypeArray m_Types;
                //! The splines' knots.
                TFloatVec m_Knots;
                //! The splines' values.
                TFloatVecArray m_Values;
                //! The splines' curvatures.
                TDoubleVecArray m_Curvatures;
        };

    private:
        //! Create by traversing a state document.
        bool acceptRestoreTraverser(double decayRate,
                                    double minimumBucketLength,
                                    core::CStateRestoreTraverser &traverser);

    private:
        //! The minimum permitted size for the points sketch.
        static const std::size_t MINIMUM_SPACE;

    private:
        //! The number of buckets to use to cover the period.
        std::size_t m_Space;

        //! The mean function values in collection of buckets covering the period.
        CSeasonalComponentAdaptiveBucketing m_Bucketing;

        //! The boundary condition to use for the splines.
        CSplineTypes::EBoundaryCondition m_BoundaryCondition;

        //! The spline we fit through the function points and the function point
        //! residual variances.
        CPackedSplines m_Splines;

        //! The mean value in the period.
        double m_MeanValue;

        //! The mean residual variance in the period.
        double m_MeanVariance;
};

}
}

#endif // INCLUDED_ml_maths_CSeasonalComponent_h
