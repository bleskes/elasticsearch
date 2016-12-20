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

#ifndef INCLUDED_prelert_maths_CTools_h
#define INCLUDED_prelert_maths_CTools_h

#include <core/CoreTypes.h>
#include <core/CIEEE754.h>
#include <core/Constants.h>
#include <core/CNonInstantiatable.h>

#include <maths/CBasicStatistics.h>
#include <maths/CCompositeFunctions.h>
#include <maths/CIntegration.h>
#include <maths/CLogTDistribution.h>
#include <maths/CMixtureDistribution.h>
#include <maths/COrderings.h>
#include <maths/ImportExport.h>
#include <maths/MathsTypes.h>

#include <boost/array.hpp>
#include <boost/math/constants/constants.hpp>
#include <boost/math/distributions/beta.hpp>
#include <boost/math/distributions/binomial.hpp>
#include <boost/math/distributions/chi_squared.hpp>
#include <boost/math/distributions/gamma.hpp>
#include <boost/math/distributions/lognormal.hpp>
#include <boost/math/distributions/negative_binomial.hpp>
#include <boost/math/distributions/normal.hpp>
#include <boost/math/distributions/poisson.hpp>
#include <boost/math/distributions/students_t.hpp>
#include <boost/optional.hpp>
#include <boost/static_assert.hpp>

#include <iosfwd>
#include <list>
#include <set>
#include <sstream>
#include <string>
#include <vector>

#include <string.h>

namespace prelert
{
namespace maths
{

//! \brief A collection of utility functionality.
//!
//! DESCRIPTION:\n
//! A collection of utility functions primarily intended for use within the
//! maths library.
//!
//! IMPLEMENTATION DECISIONS:\n
//! This class is really just a proxy for a namespace, but a object has
//! been intentionally used to force a single point for the declaration
//! and definition of utility functions within the maths library. As such
//! all member functions should be static and it should be state-less.
//! If your functionality doesn't fit this pattern just make it a nested
//! class.
class MATHS_EXPORT CTools : private core::CNonInstantiatable
{
    public:
        typedef std::pair<double, double> TDoubleDoublePr;
        typedef std::vector<TDoubleDoublePr> TDoubleDoublePrVec;
        typedef boost::optional<TDoubleDoublePr> TOptionalDoubleDoublePr;
        typedef std::vector<double> TDoubleVec;

        //! The c.d.f. value for all x for an improper distribution.
        static const double IMPROPER_CDF;

        //! \brief A tag for an improper distribution, which is 0 everywhere.
        struct SImproperDistribution {};

        //! \brief Computes minus the log of the c.d.f. of a specified sample
        //! of an R.V. for various distributions.
        struct MATHS_EXPORT SMinusLogCdf
        {
            double operator()(const SImproperDistribution &,
                              double x) const;

            double operator()(const boost::math::normal_distribution<> &normal,
                              double x) const;

            double operator()(const boost::math::students_t_distribution<> &students,
                              double x) const;

            double operator()(const boost::math::negative_binomial_distribution<> &negativeBinomial,
                              double x) const;

            double operator()(const boost::math::lognormal_distribution<> &logNormal,
                              double x) const;

            double operator()(const CLogTDistribution &logt,
                              double x) const;

            double operator()(const boost::math::gamma_distribution<> &gamma,
                              double x) const;

            double operator()(const boost::math::beta_distribution<> &beta,
                              double x) const;
        };

        //! \brief Computes minus the log of the 1 - c.d.f. of a specified
        //! sample of an R.V. for various distributions using full double
        //! precision, i.e. these do not lose precision when the result is
        //! close to 1 and the smallest value is the minimum double rather
        //! than epsilon.
        struct MATHS_EXPORT SMinusLogCdfComplement
        {
            double operator()(const SImproperDistribution &,
                              double) const;

            double operator()(const boost::math::normal_distribution<> &normal,
                              double x) const;

            double operator()(const boost::math::students_t_distribution<> &students,
                              double x) const;

            double operator()(const boost::math::negative_binomial_distribution<> &negativeBinomial,
                              double x) const;

            double operator()(const boost::math::lognormal_distribution<> &logNormal,
                              double x) const;

            double operator()(const CLogTDistribution &logt,
                              double x) const;

            double operator()(const boost::math::gamma_distribution<> &gamma,
                              double x) const;

            double operator()(const boost::math::beta_distribution<> &beta,
                              double x) const;
        };

        //! \brief Computes the probability of seeing a more extreme sample
        //! of an R.V. for various distributions.
        //!
        //! The one sided below calculation computes the probability of the set:
        //! <pre class="fragment">
        //!   \f$\{y\ |\ y \leq x\}\f$
        //! </pre>
        //!
        //! and normalizes the result so that it equals one at the distribution
        //! median.
        //!
        //! The two sided calculation computes the probability of the set:
        //! <pre class="fragment">
        //!   \f$\{y\ |\ f(y) \leq f(x)\}\f$
        //! </pre>
        //!
        //! where,\n
        //!   \f$f(.)\f$ is the p.d.f. of the random variable.
        //!
        //! The one sided above calculation computes the probability of the set:
        //! <pre class="fragment">
        //!   \f$\{y\ |\ y \geq x\}\f$
        //! </pre>
        //!
        //! and normalizes the result so that it equals one at the distribution
        //! median.
        class MATHS_EXPORT CProbabilityOfLessLikelySample
        {
            public:
                CProbabilityOfLessLikelySample(maths_t::EProbabilityCalculation calculation);

                double operator()(const SImproperDistribution &,
                                  double,
                                  maths_t::ETail &tail) const;

                double operator()(const boost::math::normal_distribution<> &normal,
                                  double x,
                                  maths_t::ETail &tail) const;

                double operator()(const boost::math::students_t_distribution<> &students,
                                  double x,
                                  maths_t::ETail &tail) const;

                double operator()(const boost::math::negative_binomial_distribution<> &negativeBinomial,
                                  double x,
                                  maths_t::ETail &tail) const;

                double operator()(const boost::math::lognormal_distribution<> &logNormal,
                                  double x,
                                  maths_t::ETail &tail) const;

                double operator()(const CLogTDistribution &logt,
                                  double x,
                                  maths_t::ETail &tail) const;

                double operator()(const boost::math::gamma_distribution<> &gamma,
                                  double x,
                                  maths_t::ETail &tail) const;

                double operator()(const boost::math::beta_distribution<> &beta,
                                  double x,
                                  maths_t::ETail &tail) const;

            private:
                //! Check the value is supported.
                bool check(const TDoubleDoublePr &support,
                           double x,
                           double &px,
                           maths_t::ETail &tail) const;

                //! Update the tail.
                void tail(double x,
                          double mode,
                          maths_t::ETail &tail) const;

                //! The style of calculation which, i.e. one or two tail.
                maths_t::EProbabilityCalculation m_Calculation;
        };

        //! \brief Computes the probability of seeing a more extreme sample
        //! from a mixture model.
        //!
        //! \sa CProbabilityOfLessLikelySample
        class MATHS_EXPORT CMixtureProbabilityOfLessLikelySample
        {
            public:
                //! Computes the value of the smooth kernel of an integral
                //! which approximates the probability of less likely samples.
                //!
                //! In particular, we write the integral as
                //! <pre class="fragment">
                //!   \f$P(\{s : f(s) < f(x)\}) = \int{I(f(s) < f(x)) f(s)}ds\f$
                //! </pre>
                //!
                //! and approximate the indicator function as
                //! <pre class="fragment">
                //!   \f$\displaystyle I(f(s) < f(x)) \approx (1+e^{-k}) \frac{e^{-k(f(s)/f(x)-1)}}{1+e^{-k(f(s)/f(x)-1)}}\f$
                //! </pre>
                //!
                //! Note that the larger the value of \f$k\f$ the better the
                //! approximation. Note also that this computes the scaled
                //! kernel, i.e. \f$k'(s) = k(s)/f(x)\f$ so the output must
                //! be scaled by \f$f(x)\f$ to recover the true probability.
                template<typename LOGF>
                class CSmoothedKernel : private core::CNonCopyable
                {
                    public:
                        CSmoothedKernel(LOGF logf,
                                        double logF0,
                                        double k) :
                                m_LogF(logf),
                                m_LogF0(logF0),
                                m_K(k),
                                m_Scale(::exp(m_LogF0) * (1.0 + ::exp(-k)))
                        {
                        }

                        void k(double k)
                        {
                            double f0 = m_Scale / (1.0 + ::exp(-m_K));
                            m_K = k;
                            m_Scale = f0 * (1.0 + ::exp(-k));
                        }

                        bool operator()(double x, double &result) const
                        {
                            // We use the fact that if:
                            //   1 + exp(-k(f(x)/f0 - 1)) < (1 + eps) * exp(-k(f(x)/f0 - 1))
                            //
                            // then the kernel = scale to working precision. Canceling
                            // O(1) terms in the exponential, taking logs and using the
                            // fact that the log is monotonic increasing function this
                            // reduces to
                            //   0 < -k(f(x)/f0 - 1) + log(eps)
                            //
                            // which implies that we can simplify if
                            //   f(x)/f0 < 1 + log(eps)/k

                            result = 0.0;
                            double logFx;
                            if (!m_LogF(x, logFx))
                            {
                                LOG_ERROR("Failed to calculate likelihood at " << x);
                                return false;
                            }
                            logFx -= m_LogF0;
                            if (m_K * (logFx - 1.0) >= core::constants::LOG_MAX_DOUBLE)
                            {
                                return true;
                            }
                            double fx = ::exp(logFx);
                            if (fx < 1.0 + core::constants::LOG_DOUBLE_EPSILON / m_K)
                            {
                                result = m_Scale * fx;
                                return true;
                            }
                            result = m_Scale / (1.0 + ::exp(m_K * (fx - 1.0))) * fx;
                            return true;
                        }

                    private:
                        LOGF m_LogF;
                        double m_LogF0;
                        double m_K;
                        double m_Scale;
                };

            public:
                //! \param[in] n The number of modes.
                //! \param[in] x The sample.
                //! \param[in] logFx The log of the p.d.f. at the sample.
                //! \param[in] a The left end of the interval to integrate.
                //! \param[in] b The left end of the interval to integrate.
                CMixtureProbabilityOfLessLikelySample(std::size_t n,
                                                      double x,
                                                      double logFx,
                                                      double a,
                                                      double b);

                //! Reinitialize the object for computing the the probability
                //! of \f$\{y : f(y) <= f(x)\}\f$.
                //!
                //! \param[in] x The sample.
                //! \param[in] logFx The log of the p.d.f. at the sample.
                void reinitialize(double x, double logFx);

                //! Add a mode of the distribution with mean \p mean and
                //! standard deviation \p sd with normalized weight \p weight.
                //!
                //! \param[in] weight The mode weight, i.e. the proportion of
                //! samples in the mode.
                //! \param[in] modeMean The mode mean.
                //! \param[in] modeSd The mode standard deviation.
                //! \param[in] mean The mean of the mixture (of modes).
                //! \param[in] scale The square root of the variance scale.
                void addMode(double weight,
                             double modeMean,
                             double modeSd,
                             double mean,
                             double scale);

                //! Find the left tail argument with the same p.d.f. value as
                //! the sample.
                //!
                //! \param[in] logf The function which computes the log of the
                //! mixture p.d.f.
                //! \param[in] iterations The number of maximum number of
                //! evaluations of the logf function.
                //! \param[in] equal The function to test if two argument values
                //! are equal.
                //! \param[out] result Filled in with the argument with the same
                //! p.d.f. value as the sample in the left tail.
                //!
                //! \tparam LOGF The type of the function (object) which computes
                //! the log of the mixture p.d.f. It is expected to have a function
                //! like signature double (double).
                template<typename LOGF, typename EQUAL>
                bool leftTail(const LOGF &logf,
                              std::size_t iterations,
                              const EQUAL &equal,
                              double &result) const
                {
                    if (m_X <= m_A)
                    {
                        result = m_X;
                        return true;
                    }

                    CCompositeFunctions::CMinusConstant<const LOGF&, double> f(logf, m_LogFx);

                    try
                    {
                        double xr = m_A;
                        double fr = f(xr);
                        if (fr < 0.0)
                        {
                            result = m_A;
                            return true;
                        }
                        double xl = xr;
                        double fl = fr;
                        if (m_MaxDeviation.count() > 0)
                        {
                            xl = xr - m_MaxDeviation[0];
                            fl = f(xl);
                        }

                        iterations = std::max(iterations, std::size_t(4));
                        std::size_t n = iterations - 2;
                        if (!CSolvers::leftBracket(xl, xr, fl, fr, f, n))
                        {
                            result = xl;
                            return false;
                        }
                        n = iterations - n;
                        CSolvers::solve(xl, xr, fl, fr, f, n, equal, result);
                    }
                    catch (const std::exception &e)
                    {
                        LOG_ERROR("Failed to find left root: " << e.what()
                                  << ", a = " << m_A
                                  << ", logf(x) = " << m_LogFx
                                  << ", logf(a) = " << logf(m_A)
                                  << ", max deviation = " << (m_MaxDeviation.count() > 0 ? m_MaxDeviation[0] : 0.0));
                        return false;
                    }
                    return true;
                }

                //! Find the right tail argument with the same p.d.f. value
                //! as the sample.
                //!
                //! \param[in] logf The function which computes the log of the
                //! mixture p.d.f.
                //! \param[in] iterations The number of maximum number of
                //! evaluations of the logf function.
                //! \param[in] equal The function to test if two argument values
                //! are equal.
                //! \param[out] result Filled in with the argument with the same
                //! p.d.f. value as the sample in the right tail.
                //!
                //! \tparam LOGF The type of the function (object) which computes
                //! the log of the mixture p.d.f. It is expected to have a function
                //! like signature double (double).
                template<typename LOGF, typename EQUAL>
                bool rightTail(const LOGF &logf,
                               std::size_t iterations,
                               const EQUAL &equal,
                               double &result) const
                {
                    if (m_X >= m_B)
                    {
                        result = m_X;
                        return true;
                    }

                    CCompositeFunctions::CMinusConstant<const LOGF&, double> f(logf, m_LogFx);

                    try
                    {
                        double xl = m_B;
                        double fl = f(xl);
                        if (fl < 0.0)
                        {
                            result = m_B;
                            return true;
                        }
                        double xr = xl;
                        double fr = fl;
                        if (m_MaxDeviation.count() > 0)
                        {
                            xr = xl + m_MaxDeviation[0];
                            fr = f(xr);
                        }

                        iterations = std::max(iterations, std::size_t(4));
                        std::size_t n = iterations - 2;
                        if (!CSolvers::rightBracket(xl, xr, fl, fr, f, n))
                        {
                            result = xr;
                            return false;
                        }
                        n = iterations - n;
                        CSolvers::solve(xl, xr, fl, fr, f, n, equal, result);
                    }
                    catch (const std::exception &e)
                    {
                        LOG_ERROR("Failed to find right root: " << e.what()
                                  << ",b = " << m_B
                                  << ", logf(x) = " << m_LogFx
                                  << ", logf(b) = " << logf(m_B));
                        return false;
                    }
                    return true;
                }

                //! Compute the probability of a less likely sample.
                //!
                //! \param[in] logf The function which computes the log of the
                //! mixture p.d.f.
                //! \param[in] pTails The probability in the distribution tails,
                //! which can be found from the c.d.f., and is not account for
                //! by the integration.
                //!
                //! \tparam LOGF The type of the function (object) which computes
                //! the log of the mixture p.d.f. It is expected to have a function
                //! like signature bool (double, double &) where the first argument
                //! is the p.d.f. argument and the second argument is filled in
                //! with the log p.d.f. at the first argument.
                template<typename LOGF>
                double calculate(const LOGF &logf, double pTails)
                {
                    TDoubleDoublePrVec intervals;
                    this->intervals(intervals);

                    double p = 0.0;
                    TDoubleVec pIntervals(intervals.size(), 0.0);
                    CSmoothedKernel<const LOGF&> kernel(logf, m_LogFx, 3.0);
                    for (std::size_t i = 0u; i < intervals.size(); ++i)
                    {
                        if (!CIntegration::gaussLegendre<CIntegration::OrderFour>(kernel,
                                                                                  intervals[i].first,
                                                                                  intervals[i].second,
                                                                                  pIntervals[i]))
                        {
                            LOG_ERROR("Couldn't integrate kernel over "
                                      << core::CContainerPrinter::print(intervals[i]));
                        }
                    }

                    p += pTails;
                    kernel.k(15.0);
                    CIntegration::adaptiveGaussLegendre<CIntegration::OrderTwo>(kernel,
                                                                                intervals,
                                                                                pIntervals,
                                                                                2,    // refinements
                                                                                3,    // splits
                                                                                1e-2, // tolerance
                                                                                p);
                    return truncate(p - pTails, 0.0, 1.0);
                }

            private:
                typedef CBasicStatistics::COrderStatisticsStack<double, 1, std::greater<double> > TMaxAccumulator;

            private:
                static const double LOG_ROOT_TWO_PI;

            private:
                //! Compute the seed integration intervals.
                void intervals(TDoubleDoublePrVec &intervals);

            private:
                //! The sample.
                double m_X;
                //! The log p.d.f. of the sample for which to compute the
                //! probability.
                double m_LogFx;
                //! The integration interval [a, b].
                double m_A, m_B;
                //! Filled in with the end points of the seed intervals for
                //! adaptive quadrature.
                TDoubleVec m_Endpoints;
                //! The maximum deviation of the sample from any mode.
                TMaxAccumulator m_MaxDeviation;
        };

        //! \brief Computes the expectation conditioned on a particular interval.
        //!
        //! DESCRIPTION:\n
        //! Computes the expectation of various R.V.s on the condition that the
        //! variable is in a specified interval. In particular, this is the
        //! quantity:
        //! <pre class="fragment">
        //!   \f$E[ X 1{[a,b]} ] / E[ 1{a,b]} ]\f$
        //! </pre>
        struct MATHS_EXPORT SIntervalExpectation
        {
            double operator()(const boost::math::normal_distribution<> &normal,
                              double a,
                              double b) const;

            double operator()(const boost::math::lognormal_distribution<> &logNormal,
                              double a,
                              double b) const;

            double operator()(const boost::math::gamma_distribution<> &gamma,
                              double a,
                              double b) const;
        };

        //! The smallest value of probability we permit.
        //!
        //! This is used to stop calculations under/overflowing if we
        //! allow the probability to be zero (for example).
        static double smallestProbability(void);

        //! \name Safe Probability Density Function
        //! Unfortunately, boost::math::pdf and boost::math::cdf don't
        //! handle values outside of the distribution support very well.
        //! By default they throw and if you suppress this behaviour
        //! they return 0.0 for the cdf! This wraps up the pdf and cdf
        //! calls and does the appropriate checking. The functions are
        //! extended to the whole real line in the usual way by treating
        //! them as continuous.
        //@{
        static double safePdf(const boost::math::normal_distribution<> &normal, double x);
        static double safePdf(const boost::math::students_t_distribution<> &students, double x);
        static double safePdf(const boost::math::poisson_distribution<> &poisson, double x);
        static double safePdf(const boost::math::negative_binomial_distribution<> &negativeBinomial, double x);
        static double safePdf(const boost::math::lognormal_distribution<> &logNormal, double x);
        static double safePdf(const boost::math::gamma_distribution<> &gamma, double x);
        static double safePdf(const boost::math::beta_distribution<> &beta, double x);
        static double safePdf(const boost::math::binomial_distribution<> &binomial, double x);
        static double safePdf(const boost::math::chi_squared_distribution<> &chi2, double x);
        //@}

        //! \name Safe Cumulative Density Function
        //! Wrappers around the boost::math::cdf functions which extend
        //! them to the whole real line.
        //! \see safePdf for details.
        //@{
        static double safeCdf(const boost::math::normal_distribution<> &normal, double x);
        static double safeCdf(const boost::math::students_t_distribution<> &students, double x);
        static double safeCdf(const boost::math::poisson_distribution<> &poisson, double x);
        static double safeCdf(const boost::math::negative_binomial_distribution<> &negativeBinomial, double x);
        static double safeCdf(const boost::math::lognormal_distribution<> &logNormal, double x);
        static double safeCdf(const boost::math::gamma_distribution<> &gamma, double x);
        static double safeCdf(const boost::math::beta_distribution<> &beta, double x);
        static double safeCdf(const boost::math::binomial_distribution<> &binomial, double x);
        static double safeCdf(const boost::math::chi_squared_distribution<> &chi2, double x);
        //@}

        //! \name Safe Cumulative Density Function Complement
        //! Wrappers around the boost::math::cdf functions for complement
        //! distributions which extend them to the whole real line.
        //! \see safePdf for details.
        //@{
        static double safeCdfComplement(const boost::math::normal_distribution<> &normal, double x);
        static double safeCdfComplement(const boost::math::students_t_distribution<> &students, double x);
        static double safeCdfComplement(const boost::math::poisson_distribution<> &poisson, double x);
        static double safeCdfComplement(const boost::math::negative_binomial_distribution<> &negativeBinomial, double x);
        static double safeCdfComplement(const boost::math::lognormal_distribution<> &logNormal, double x);
        static double safeCdfComplement(const boost::math::gamma_distribution<> &gamma, double x);
        static double safeCdfComplement(const boost::math::beta_distribution<> &beta, double x);
        static double safeCdfComplement(const boost::math::binomial_distribution<> &binomial, double x);
        static double safeCdfComplement(const boost::math::chi_squared_distribution<> &chi2, double x);
        //@}

        //! Compute the deviation from the probability of seeing a more
        //! extreme event for a distribution, i.e. for a sample \f$x\f$
        //! from a R.V. the probability \f$P(R)\f$ of the set:
        //! <pre class="fragment">
        //!   \f$ R = \{y\ |\ f(y) \leq f(x)\} \f$
        //! </pre>
        //! where,\n
        //!   \f$f(.)\f$ is the p.d.f. of the random variable.\n\n
        //! This is a monotonically decreasing function of \f$P(R)\f$ and
        //! is chosen so that for \f$P(R)\f$ near one it is zero and as
        //! \f$P(R) \rightarrow 0\f$ it saturates at 100.
        static double deviation(double p);

        //! The inverse of the deviation function.
        static double inverseDeviation(double deviation);

        //! \name Differential Entropy
        //! Compute the differential entropy of the specified distribution.\n\n
        //! The differential entropy of an R.V. is defined as:
        //! <pre class="fragment">
        //!   \f$ -E[\log(f(x))] \f$
        //! </pre>
        //! where,\n
        //!   \f$f(x)\f$ is the probability density function.\n\n
        //! This computes the differential entropy in units of "nats",
        //! i.e. the logarithm is the natural logarithm.
        //@{
        static double differentialEntropy(const boost::math::poisson_distribution<> &poisson);
        static double differentialEntropy(const boost::math::normal_distribution<> &normal);
        static double differentialEntropy(const boost::math::lognormal_distribution<> &logNormal);
        static double differentialEntropy(const boost::math::gamma_distribution<> &gamma);
        template<typename T>
        class CDifferentialEntropyKernel
        {
            public:
                CDifferentialEntropyKernel(const CMixtureDistribution<T> &mixture) :
                        m_Mixture(&mixture)
                {
                }

                inline bool operator()(double x, double &result) const
                {
                    double fx = pdf(*m_Mixture, x);
                    result = fx == 0.0 ? 0.0 : -fx * ::log(fx);
                    return true;
                }

            private:
                const CMixtureDistribution<T> *m_Mixture;
        };
        template<typename T>
        static double differentialEntropy(const CMixtureDistribution<T> &mixture)
        {
            typedef typename CMixtureDistribution<T>::TDoubleVec TDoubleVec;
            typedef typename CMixtureDistribution<T>::TModeVec TModeVec;
            typedef std::vector<TDoubleDoublePr> TDoubleDoublePrVec;

            static const double EPS = 1e-5;
            static const std::size_t INTERVALS = 8u;

            const TDoubleVec &weights = mixture.weights();
            const TModeVec &modes = mixture.modes();

            if (weights.empty())
            {
                return 0.0;
            }

            TDoubleDoublePrVec range;
            for (std::size_t i = 0u; i < modes.size(); ++i)
            {
                range.push_back(TDoubleDoublePr(quantile(modes[i], EPS),
                                                quantile(modes[i], 1.0 - EPS)));
            }
            std::sort(range.begin(), range.end(), COrderings::SFirstLess());
            LOG_TRACE("range = " << core::CContainerPrinter::print(range));
            std::size_t left = 0u;
            for (std::size_t i = 1u; i < range.size(); ++i)
            {
                if (range[left].second < range[i].first)
                {
                    ++left;
                    std::swap(range[left], range[i]);
                }
                else
                {
                    range[left].second = std::max(range[left].second,
                                                  range[i].second);
                }
            }
            range.erase(range.begin() + left + 1, range.end());
            LOG_TRACE("range = " << core::CContainerPrinter::print(range));

            double result = 0.0;

            CDifferentialEntropyKernel<T> kernel(mixture);
            for (std::size_t i = 0u; i < range.size(); ++i)
            {
                double a = range[i].first;
                double d = (range[i].second - range[i].first)
                           / static_cast<double>(INTERVALS);

                for (std::size_t j = 0u; j < INTERVALS; ++j, a += d)
                {
                    double integral;
                    if (CIntegration::gaussLegendre<CIntegration::OrderFive>(kernel,
                                                                             a, a+d,
                                                                             integral))
                    {
                        result += integral;
                    }
                }
            }

            LOG_TRACE("result = " << result);
            return result;
        }
        //@}

        //! Check if \p log will underflow the smallest positive value of T.
        //!
        //! \tparam T must be a floating point type.
        template<typename T>
        static bool logWillUnderflow(T log)
        {
            static const T LOG_DENORM_MIN = ::log(std::numeric_limits<T>::min());
            return log < LOG_DENORM_MIN;
        }

        //! \name Fast Log
    private:
        //! The precision to use for fastLog, which gives good runtime
        //! accuracy tradeoff.
        static const int FAST_LOG_PRECISION = 14;

        //! Shift used to index the lookup table in fastLog.
        static const std::size_t FAST_LOG_SHIFT = 52 - FAST_LOG_PRECISION;

        //! \brief Creates a lookup table for log2(x) with specified
        //! accuracy.
        //!
        //! DESCRIPTION:\n
        //! This implements a singleton lookup table for all values
        //! of log base 2 of x for the mantissa of x in the range
        //! [0, 2^52-1]. The specified accuracy, \p N, determines the
        //! size of the lookup table, and values are equally spaced,
        //! i.e. the separation is 2^52 / 2^N. This is used by fastLog
        //! to read off the log base 2 to the specified precision.
        //!
        //! This is taken from the approach given in
        //! http://www.icsi.berkeley.edu/pubs/techreports/TR-07-002.pdf
        template<int BITS>
        class CLookupTableForFastLog
        {
            public:
                static const std::size_t BINS = 1 << BITS;

            public:
                typedef boost::array<double, BINS> TArray;

            public:
                //! Builds the table.
                CLookupTableForFastLog(void)
                {
                    // Notes:
                    //   1) The shift is the maximum mantissa / BINS.
                    //   2) The sign bit is set to 0 which is positive.
                    //   3) The exponent is set to 1022, which is 0 in two's
                    //      complement.
                    //   4) This implementation is endian neutral because it
                    //      is constructing a look up from the mantissa value
                    //      (interpreted as an integer) to the corresponding
                    //      double value and fastLog uses the same approach
                    //      to extract the mantissa.
                    uint64_t dx = 0x10000000000000ull / BINS;
                    core::CIEEE754::SDoubleRep x;
                    x.s_Sign = 0;
                    x.s_Mantissa = dx / 2;
                    x.s_Exponent = 1022;
                    for (std::size_t i = 0u; i < BINS; ++i, x.s_Mantissa += dx)
                    {
                        double value;
                        BOOST_STATIC_ASSERT(sizeof(double) == sizeof(core::CIEEE754::SDoubleRep));
                        // Use memcpy() rather than union to adhere to strict
                        // aliasing rules
                        ::memcpy(&value, &x, sizeof(double));
                        m_Table[i] = ::log2(value);
                    }
                }

                //! Lookup log2 for a given mantissa.
                const double &operator[](uint64_t mantissa) const
                {
                    return m_Table[mantissa >> FAST_LOG_SHIFT];
                }

            private:
                //! The quantized log base 2 for the mantissa range.
                TArray m_Table;
        };

        //! The table used for computing fast log.
        static const CLookupTableForFastLog<FAST_LOG_PRECISION> FAST_LOG_TABLE;

    public:
        //! Approximate implementation of log(\p x), which is accurate
        //! to FAST_LOG_PRECISION bits of precision.
        //!
        //! \param[in] x The value for which to compute the natural log.
        //! \note This is taken from the approach given in
        //! http://www.icsi.berkeley.edu/pubs/techreports/TR-07-002.pdf
        static double fastLog(double x)
        {
            uint64_t mantissa;
            int log2;
            core::CIEEE754::decompose(x, mantissa, log2);
            return 0.693147180559945 * (FAST_LOG_TABLE[mantissa] + log2);
        }
        //@}

    private:
        //! Get the location of the point \p x.
        template<typename T>
        static double location(T x)
        {
            return x;
        }
        //! Set \p x to \p y.
        template<typename T>
        static void setLocation(T &x, double y)
        {
            x = static_cast<T>(y);
        }
        //! Get a writable location of the point \p x.
        template<typename T>
        static double location(const typename CBasicStatistics::SSampleMean<T>::TAccumulator &x)
        {
            return CBasicStatistics::mean(x);
        }
        //! Set the mean of \p x to \p y.
        template<typename T>
        static void setLocation(typename CBasicStatistics::SSampleMean<T>::TAccumulator &x, double y)
        {
            x.s_Moments[0] = static_cast<T>(y);
        }

        //! \brief Utility class to represent points which are adjacent
        //! in the spreading algorithm.
        class MATHS_EXPORT CGroup
        {
            public:
                typedef CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;

            public:
                //! Create a new points group.
                template<typename T>
                CGroup(std::size_t index, const T &points) :
                        m_A(index),
                        m_B(index),
                        m_Centre()
                {
                     m_Centre.add(location(points[index]));
                }

                //! Merge this group and \p other group.
                void merge(const CGroup &other,
                           double separation,
                           double min,
                           double max);

                //! Check if this group and \p other group overlap.
                bool overlap(const CGroup &other,
                             double separation) const;

                //! Update the locations of the points in this group based
                //! on its centre position.
                template<typename T>
                bool spread(double separation, T &points) const
                {
                    if (m_A == m_B)
                    {
                        return false;
                    }
                    bool result = false;
                    double x = this->leftEndpoint(separation);
                    for (std::size_t i = m_A; i <= m_B; ++i, x += separation)
                    {
                        if (location(points[i]) != x)
                        {
                            setLocation(points[i], x);
                            result = true;
                        }
                    }
                    return result;
                }

            private:
                //! Get the position of the left end point of this group.
                double leftEndpoint(double separation) const;

                //! Get the position of the right end point of this group.
                double rightEndpoint(double separation) const;

                std::size_t m_A;
                std::size_t m_B;
                TMeanAccumulator m_Centre;
        };

        //! \brief Orders two points by their position.
        class CPointLess
        {
            public:
                template<typename T>
                bool operator()(const T &lhs, const T &rhs) const
                {
                    return location(lhs) < location(rhs);
                }
        };

    public:
        //! \brief Ensure the points are at least \p separation apart.\n\n
        //! This solves the problem of finding the new positions for the
        //! points \f$\{x_i\}\f$ such that there is no pair of points for
        //! which \f$\left \|x_j - x_i \right \| < s\f$ where \f$s\f$
        //! denotes the minimum separation \p separation and the total
        //! square distance the points move, i.e.
        //! <pre class="fragment">
        //!   \f$ \sum_i{(x_i' - x_i)^2} \f$
        //! </pre>
        //! is minimized.
        //!
        //! \param[in] a The left end point of the interval containing
        //! the shifted points.
        //! \param[in] b The right end point of the interval containing
        //! the shifted points.
        //! \param[in] separation The minimum permitted separation between
        //! points.
        //! \param[in,out] points The points to spread.
        template<typename T>
        static void spread(double a, double b, double separation, T &points)
        {
            if (points.empty())
            {
                return;
            }
            if (b <= a)
            {
                LOG_ERROR("Bad interval [" << a << "," << b << "]");
                return;
            }

            std::size_t n = points.size() - 1;
            if (b - a <= separation * static_cast<double>(n + 1))
            {
                for (std::size_t i = 0u; i <= n; ++i)
                {
                    setLocation(points[i], a + (b - a) * static_cast<double>(i)
                                                       / static_cast<double>(n));
                }
                return;
            }

            // We can do this in n * log(n) complexity with at most log(n)
            // passes through the points. Provided the minimum separation
            // is at least "interval" / "# centres" the problem is feasible.
            //
            // We want to find the solution which minimizes the sum of the
            // distances the points move. This is possible by repeatedly
            // merging points in to clusters which are within the minimum
            // separation and then placing clusters at the mean of the
            // points in the cluster. We repeat this process until no
            // clusters merge and ensure that the cluster end points are
            // in [0, interval]. Note that by alternating the direction
            // of traversal through the points we avoid the worst case n
            // traversals of the points.

            for (std::size_t i = 0u; a > 0.0 && i <= n; ++i)
            {
                points[i] -= a;
            }
            std::sort(points.begin(), points.end(), CPointLess());

            bool moved = false;
            std::size_t iteration = 0u;
            do
            {
                moved = false;
                bool forward = (iteration++ % 2 == 0);
                LOG_TRACE((forward ? "forward" : "backward"));
                CGroup last(forward ? 0 : n, points);
                for (std::size_t i = 1u; i <= n; ++i)
                {
                    CGroup test(forward ? i : n - i, points);
                    if (last.overlap(test, separation))
                    {
                        last.merge(test, separation, 0.0, b-a);
                    }
                    else
                    {
                        moved |= last.spread(separation, points);
                        last = test;
                    }
                }
                moved |= last.spread(separation, points);
            }
            while (moved && iteration <= n);

            for (std::size_t i = 0u; a > 0.0 && i <= n; ++i)
            {
                points[i] += a;
            }

            LOG_TRACE("# iterations = " << iteration
                      << " # points = " << n + 1);
        }

        //! Compute the sign of \p x and return T(-1) if it is negative and T(1)
        //! otherwise.
        //!
        //! \param[in] x The value for which to check the sign.
        //! \note Conversion of 0 and -1 to T should be well defined.
        //! \note Zero maps to 1.
        template<typename T>
        static T sign(const T &x)
        {
            return x < T(0) ? T(-1) : T(1);
        }

        //! Truncate \p x to the range [\p a, \p b].
        //!
        //! \tparam T Must support operator<.
        template<typename T>
        static const T &truncate(const T &x, const T &a, const T &b)
        {
            return x < a ? a : (b < x ? b : x);
        }

        //! Component-wise truncation of stack vectors.
        template<typename T, std::size_t N>
        static CVectorNx1<T, N> truncate(const CVectorNx1<T, N> &x,
                                         const CVectorNx1<T, N> &a,
                                         const CVectorNx1<T, N> &b)
        {
            CVectorNx1<T, N> result(x);
            for (std::size_t i = 0u; i < N; ++i)
            {
                result(i) = truncate(result(i), a(i), b(i));
            }
            return result;
        }

        //! Component-wise truncation of heap vectors.
        template<typename T>
        static CVector<T> truncate(const CVector<T> &x,
                                   const CVector<T> &a,
                                   const CVector<T> &b)
        {
            CVector<T> result(x);
            for (std::size_t i = 0u; i < result.dimension(); ++i)
            {
                result(i) = truncate(result(i), a(i), b(i));
            }
            return result;
        }

        //! Component-wise truncation of small vector.
        template<typename T, std::size_t N>
        static core::CSmallVector<T, N> truncate(const core::CSmallVector<T, N> &x,
                                                 const core::CSmallVector<T, N> &a,
                                                 const core::CSmallVector<T, N> &b)
        {
            core::CSmallVector<T, N> result(x);
            for (std::size_t i = 0u; i < result.size(); ++i)
            {
                result[i] = truncate(result[i], a[i], b[i]);
            }
            return result;
        }

        //! Shift \p x to the left by \p eps times \p x.
        static double shiftLeft(double x, double eps = std::numeric_limits<double>::epsilon());

        //! Shift \p x to the right by \p eps times \p x.
        static double shiftRight(double x, double eps = std::numeric_limits<double>::epsilon());
};

}
}

#endif // INCLUDED_prelert_maths_CTools_h
