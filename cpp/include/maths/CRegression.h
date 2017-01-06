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

#ifndef INCLUDED_prelert_maths_CRegression_h
#define INCLUDED_prelert_maths_CRegression_h

#include <core/CContainerPrinter.h>
#include <core/CLogger.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>

#include <maths/CBasicStatistics.h>
#include <maths/CFloatStorage.h>
#include <maths/CLinearAlgebra.h>
#include <maths/ImportExport.h>

#include <boost/array.hpp>
#include <boost/math/special_functions/gamma.hpp>
#include <boost/operators.hpp>

#include <cstddef>

#include <stdint.h>


namespace prelert
{
namespace maths
{

namespace regression_detail
{

//! Used for getting the default maximum condition number to use
//! when computing parameters.
template<typename T>
struct CMaxCondition
{
    static const double VALUE;
};
template<typename T> const double CMaxCondition<T>::VALUE = 1e15;

//! Used for getting the default maximum condition number to use
//! when computing parameters.
template<>
struct MATHS_EXPORT CMaxCondition<CFloatStorage>
{
    static const double VALUE;
};

}

//! \brief A collection of various types of regression.
//!
//! IMPLEMENTATION DECISIONS:\n
//! This class is really just a proxy for a namespace, but a object has
//! been intentionally used to force a single point for the declaration
//! and definition. As such all member functions should be static and it
//! should be state-less. If your functionality doesn't fit this pattern
//! just make it a nested class.
class MATHS_EXPORT CRegression
{
    public:
        //! DESCRIPTION:\n
        //! A very lightweight online weighted least squares regression to
        //! fit degree N polynomials to a collection of points \f$\{(x_i, y_i)\}\f$,
        //! i.e. to find the \f$y = c_0 + c_1 x + ... + c_N x^N\f$ s.t. the
        //! weighted sum of the square residuals is minimized. Formally, we
        //! are looking for \f$\theta^*\f$ defined as
        //! <pre class="fragment">
        //!   \f$\theta^* = arg\min_{\theta}{(y - X\theta)^tDiag(w)(y - X\theta)}\f$
        //! </pre>
        //! Here, \f$X\f$ denotes the information matrix and for a polynomial
        //! takes the form \f$[X]_{ij} = x_i^{j-1}\f$. This is solved using
        //! the Moore-Penrose pseudoinverse.
        //!
        //! We are able to maintain \f$2N-1\f$ sufficient statistics to
        //! construct \f$X^tDiag(w)X\f$ and also the \f$N\f$ components of
        //! the vector \f$X^tDiag(w)y\f$ online.
        //!
        //!
        //! IMPLEMENTATION DECISIONS:\n
        //! This uses float storage and requires \f$3N\f$ floats. In total
        //! this therefore uses \f$12N\f$ bytes. This is because it is
        //! intended for use in cases where space is at a premium. *DO NOT*
        //! use floats unless doing so gives a significant overall space
        //! improvement to the *program* footprint. Note also that the
        //! interface to this class is double precision. If floats are used
        //! they should be used for storage only and transparent to the rest
        //! of the code base.
        //!
        //! Note that this constructs the Gramian \f$X^tDiag(w)X\f$ when
        //! computing the least squares solution. This is because holding
        //! sufficient statistics for constructing this matrix is the most
        //! space efficient representation to compute online. However, the
        //! condition of this matrix is the square of the condition of the
        //! information matrix and so this approach doesn't have good numerics.
        //!
        //! A much more robust scheme is to use incremental QR factorization
        //! and for large problems that approach should be used in preference.
        //! However, much can be done by using an affine transformation of
        //! \f$x_i\f$ to improve the numerics of this approach and the intention
        //! is that it is used for the case where \f$N\f$ is small and space
        //! is at a premium.
        //!
        //! \tparam N_ The degree of the polynomial.
        template<std::size_t N_, typename T = CFloatStorage>
        class CLeastSquaresOnline : boost::addable< CLeastSquaresOnline<N_, T> >
        {
            public:
                static const std::size_t N = N_+1;
                typedef boost::array<double, N> TArray;
                typedef CVectorNx1<T, 3*N-1> TVector;
                typedef CSymmetricMatrixNxN<double, N> TMatrix;
                typedef typename CBasicStatistics::SSampleMean<TVector>::TAccumulator TVectorMeanAccumulator;

            public:
                static const std::string STATISTIC_TAG;

            public:
                CLeastSquaresOnline(void) : m_S() {}
                template<typename U>
                CLeastSquaresOnline(const CLeastSquaresOnline<N_, U> &other) :
                    m_S(other.statistic())
                {}

                //! Restore by traversing a state document.
                bool acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
                {
                    do
                    {
                        const std::string &name = traverser.name();
                        if (name == STATISTIC_TAG)
                        {
                            if (m_S.fromDelimited(traverser.value()) == false)
                            {
                                LOG_ERROR("Invalid statistic in " << traverser.value());
                                return false;
                            }
                        }
                    }
                    while (traverser.next());

                    return true;
                }

                //! Persist by passing information to the supplied inserter.
                void acceptPersistInserter(core::CStatePersistInserter &inserter) const
                {
                    inserter.insertValue(STATISTIC_TAG, m_S.toDelimited());
                }

                //! Add in the point \f$(x, y(x))\f$ with weight \p weight.
                //!
                //! \param[in] x The abscissa of the point.
                //! \param[in] y The ordinate of the point.
                //! \param[in] weight The residual weight at the point.
                void add(double x, double y, double weight = 1.0)
                {
                    TVector d;
                    double xi = 1.0;
                    for (std::size_t i = 0u; i < N; ++i, xi *= x)
                    {
                        d(i)       = xi;
                        d(i+2*N-1) = xi * y;
                    }
                    for (std::size_t i = N; i < 2*N-1; ++i, xi *= x)
                    {
                        d(i)       = xi;
                    }
                    m_S.add(d, weight);
                }

                //! Set the statistics from \p rhs.
                template<typename U>
                const CLeastSquaresOnline operator=(const CLeastSquaresOnline<N_, U> &rhs)
                {
                    m_S = rhs.statistic();
                    return *this;
                }

                //! Combines two regressions.
                //!
                //! This creates the regression fit on the points fit with
                //! \p lhs and the points fit with this regression.
                //!
                //! \param[in] rhs The regression fit to combine.
                template<typename U>
                const CLeastSquaresOnline &operator+=(const CLeastSquaresOnline<N_, U> &rhs)
                {
                    m_S += rhs.statistic();
                    return *this;
                }

                //! In order to get reasonable accuracy, one typically needs to
                //! use an affine transform of the abscissa.
                //!
                //! In particular, one will typically use \f$x \mapsto a(x - b)\f$
                //! rather than \f$x\f$ directly, since \f$a\f$ and \f$b\f$ can be
                //! adjusted to improve the condition of the Gramian.
                //!
                //! If this is running online, then as x increases one wants to
                //! allow the shift \f$a b\f$ to increase. This function computes
                //! the impact of a change in \f$a b\f$ on the stored statistics.
                //!
                //! \param[in] dx The shift that will subsequently be applied to
                //! the abscissa.
                void shiftAbscissa(double dx)
                {
                    if (CBasicStatistics::count(m_S) == 0.0)
                    {
                        return;
                    }

                    // The update scheme is as follows:
                    //
                    // 1/n sum_i{ (t(i) + dx)^i }
                    //   -> 1/n * (  sum_i{ t(i)^i }
                    //             + sum_j{ (i j) * dx^(i - j) * sum_i{ t(i)^j } } )
                    //
                    // 1/n sum_i{ (t(i) + dx)^i * y(i) }
                    //   -> 1/n * (  sum_i{ t(i)^i * y(i) }
                    //             + sum_j{ (i j) * dx^(i - j) * sum_i{ t(i)^j y(i) } } )

                    double d[2*N-2] = { dx };
                    for (std::size_t i = 1u; i < 2*N-2; ++i)
                    {
                        d[i] = d[i-1] * dx;
                    }
                    LOG_TRACE("d = " << core::CContainerPrinter::print(d));

                    LOG_TRACE("S(before) " << CBasicStatistics::mean(m_S));
                    for (std::size_t i = 2*N-2; i > 0; --i)
                    {
                        LOG_TRACE("i = " << i);
                        for (std::size_t j = 0u; j < i; ++j)
                        {
                            double bij = this->binom(i, j) * d[i-j-1];
                            LOG_TRACE("bij = " << bij);
                            CBasicStatistics::moment<0>(m_S)(i) += bij * CBasicStatistics::mean(m_S)(j);
                            if (i >= N)
                            {
                                continue;
                            }
                            std::size_t yi = i + 2*N-1;
                            std::size_t yj = j + 2*N-1;
                            LOG_TRACE("yi = " << yi << ", yj = " << yj);
                            CBasicStatistics::moment<0>(m_S)(yi) += bij * CBasicStatistics::mean(m_S)(yj);
                        }
                    }
                    LOG_TRACE("S(after) = " << CBasicStatistics::mean(m_S));
                }

                //! Translate the ordinates by \p dy.
                //!
                //! \param[in] dy The shift that will subsequently be applied to
                //! the ordinates.
                void shiftOrdinate(double dy)
                {
                    double n = CBasicStatistics::count(m_S);

                    if (n == 0.0)
                    {
                        return;
                    }

                    const TVector &s = CBasicStatistics::mean(m_S);
                    for (std::size_t i = 0u; i < N; ++i)
                    {
                        CBasicStatistics::moment<0>(m_S)(i+2*N-1) += s(i) * dy;
                    }
                }

                //! Multiply the statistics' count by \p scale.
                CLeastSquaresOnline scaled(double scale) const
                {
                    CLeastSquaresOnline result(*this);
                    return result.scale(scale);
                }

                //! Scale the statistics' count by \p scale.
                const CLeastSquaresOnline &scale(double scale)
                {
                    CBasicStatistics::count(m_S) *= scale;
                    return *this;
                }

                //! Get the regression parameters.
                //!
                //! i.e. The intercept, slope, curvature, etc.
                //!
                //! \param[in] maxCondition The maximum condition number for
                //! the Gramian this will consider solving. If the condition
                //! is worse than this it'll fit a lower order polynomial.
                //! \param[out] result Filled in with the regression parameters.
                bool parameters(TArray &result,
                                double maxCondition = regression_detail::CMaxCondition<T>::VALUE) const
                {
                    result.fill(0.0);

                    // Search for non-singular solution.
                    std::size_t n = N+1;
                    while (--n > 0)
                    {
                        switch (n)
                        {
                        case 1:
                        {
                            result[0] = CBasicStatistics::mean(m_S)(2*N-1);
                            return true;
                        }
                        case N:
                        {
                            Eigen::Matrix<double, N, N> x;
                            Eigen::Matrix<double, N, 1> y;
                            if (this->parameters(N, x, y, maxCondition, result))
                            {
                                return true;
                            }
                            break;
                        }
                        default:
                        {
                            Eigen::MatrixXd x(n, n);
                            Eigen::VectorXd y(n);
                            if (this->parameters(n, x, y, maxCondition, result))
                            {
                                return true;
                            }
                            break;
                        }
                        }
                    }
                    return false;
                }

                //! Get the covariance matrix of the regression parameters.
                //!
                //! To compute this assume the data to fit are described by
                //! \f$y_i = \sum_{j=0}{N} c_j x_i^j + Y_i\f$ where \f$Y_i\f$
                //! are IID and \f$N(0, \sigma)\f$ whence
                //! <pre class="fragment">
                //!   \f$C = (X^t X)^{-1}X^t E[YY^t] X (X^t X)^{-1}\f$
                //! </pre>
                //!
                //! Since \f$E[YY^t] = \sigma^2 I\f$ it follows that
                //! <pre class="fragment">
                //!   \f$C = \sigma^2 (X^t X)^{-1}\f$
                //! </pre>
                //!
                //! \param[in] variance The variance of the data residuals.
                //! \param[in] maxCondition The maximum condition number for
                //! the Gramian this will consider solving. If the condition
                //! is worse than this it'll fit a lower order polynomial.
                //! \param[out] result Filled in with the covariance matrix.
                bool covariances(double variance,
                                 TMatrix &result,
                                 double maxCondition = regression_detail::CMaxCondition<T>::VALUE) const
                {
                    result = TMatrix(0.0);

                    // Search for the covariance matrix of a non-singular subproblem.
                    std::size_t n = N+1;
                    while (--n > 0)
                    {
                        switch (n)
                        {
                        case 1:
                        {
                            result(0,0) = variance / CBasicStatistics::count(m_S);
                            return true;
                        }
                        case N:
                        {
                            Eigen::Matrix<double, N, N> x;
                            if (!this->covariances(N, x, variance, maxCondition, result))
                            {
                                continue;
                            }
                            break;
                        }
                        default:
                        {
                            Eigen::MatrixXd x(n, n);
                            if (!this->covariances(n, x, variance, maxCondition, result))
                            {
                                continue;
                            }
                            break;
                        }
                        }
                        return true;
                    }
                    return false;
                }

                //! Get the safe prediction horizon based on the spread of
                //! the abscissa added to the model so far.
                double range(void) const
                {
                    double x1 = CBasicStatistics::mean(m_S)(1);
                    double x2 = CBasicStatistics::mean(m_S)(2);
                    return ::sqrt(12.0 * std::max(x2 - x1 * x1, 0.0));
                }

                //! Age out the old points.
                void age(double factor, bool meanRevert = false)
                {
                    if (meanRevert)
                    {
                        TVector &s = CBasicStatistics::moment<0>(m_S);
                        for (std::size_t i = 1u; i < N; ++i)
                        {
                            s(i+2*N-1) =         factor  * s(i+2*N-1)
                                        + (1.0 - factor) * s(i) * s(2*N-1);
                        }
                    }
                    m_S.age(factor);
                }

                //! Get the effective number of points being fitted.
                double count(void) const
                {
                    return CBasicStatistics::count(m_S);
                }

                //! Get the mean value of the ordinates.
                double mean(void) const
                {
                    return CBasicStatistics::mean(m_S)(2*N-1);
                }

                //! Get the mean in the interval [\p a, \p b].
                double mean(double a, double b) const
                {
                    double result = 0.0;

                    double interval = b - a;

                    TArray params;
                    this->parameters(params);

                    if (interval == 0.0)
                    {
                        result = params[0];
                        double xi = a;
                        for (std::size_t i = 1u; i < params.size(); ++i, xi *= a)
                        {
                            result += params[i] * xi;
                        }
                        return result;
                    }

                    for (std::size_t i = 0u; i < N; ++i)
                    {
                        for (std::size_t j = 0u; j <= i; ++j)
                        {
                            result +=  this->binom(i+1, j+1)
                                     * params[i] / static_cast<double>(i+1)
                                     * ::pow(a, static_cast<double>(i-j))
                                     * ::pow(interval, static_cast<double>(j+1));
                        }
                    }

                    return result / interval;
                }

                //! Get the vector statistic.
                const TVectorMeanAccumulator &statistic(void) const
                {
                    return m_S;
                }

                //! Get a checksum for this object.
                uint64_t checksum(void) const
                {
                    return CBasicStatistics::checksum(m_S);
                }

                //! Print this regression out to debug.
                std::string print(void) const
                {
                    TArray params;
                    if (this->parameters(params))
                    {
                        std::string result;
                        for (std::size_t i = params.size()-1; i > 0; --i)
                        {
                            result += core::CStringUtils::typeToStringPretty(params[i])
                                      + " x^"
                                      + core::CStringUtils::typeToStringPretty(i)
                                      + " + ";
                        }
                        result += core::CStringUtils::typeToStringPretty(params[0]);
                        return result;
                    }
                    return std::string("bad");
                }

            private:
                //! The binomial coefficient (n m).
                double binom(std::size_t n, std::size_t m) const
                {
                    if (m == n || m == 0)
                    {
                        return 1.0;
                    }
                    double n_ = static_cast<double>(n);
                    double m_ = static_cast<double>(m);
                    return ::exp(  boost::math::lgamma(n_ + 1.0)
                                 - boost::math::lgamma(m_ + 1.0)
                                 - boost::math::lgamma(n_ - m_ + 1.0));
                }

                //! Get the first \p n regression parameters.
                template<typename MATRIX, typename VECTOR>
                bool parameters(std::size_t n,
                                MATRIX &x,
                                VECTOR &y,
                                double maxCondition,
                                TArray &result) const
                {
                    if (n == 1)
                    {
                        result[0] = CBasicStatistics::mean(m_S)(2*N-1);
                        return true;
                    }

                    this->gramian(n, x);
                    for (std::size_t i = 0u; i < n; ++i)
                    {
                        y(i) = CBasicStatistics::mean(m_S)(i+2*N-1);
                    }
                    LOG_TRACE("S = " << CBasicStatistics::mean(m_S));
                    LOG_TRACE("x =\n" << x);
                    LOG_TRACE("y =\n" << y);

                    Eigen::JacobiSVD<MATRIX> x_(x.template selfadjointView<Eigen::Upper>(),
                                                Eigen::ComputeFullU | Eigen::ComputeFullV);
                    if (x_.singularValues()(0) > maxCondition * x_.singularValues()(n-1))
                    {
                        LOG_TRACE("singular values = " << x_.singularValues());
                        return false;
                    }

                    // Don't bother checking the solution since we check
                    // the matrix condition above.
                    VECTOR r = x_.solve(y);
                    for (std::size_t i = 0u; i < n; ++i)
                    {
                        result[i] = r(i);
                    }

                    return true;
                }

                //! Compute the covariance matrix of the regression parameters.
                template<typename MATRIX>
                bool covariances(std::size_t n,
                                 MATRIX &x,
                                 double variance,
                                 double maxCondition,
                                 TMatrix &result) const
                {
                    if (n == 1)
                    {
                        x(0) = variance / CBasicStatistics::count(m_S);
                        return true;
                    }

                    this->gramian(n, x);
                    Eigen::JacobiSVD<MATRIX> x_(x.template selfadjointView<Eigen::Upper>(),
                                                Eigen::ComputeFullU | Eigen::ComputeFullV);
                    if (x_.singularValues()(0) > maxCondition * x_.singularValues()(n-1))
                    {
                        LOG_TRACE("singular values = " << x_.singularValues());
                        return false;
                    }

                    // Don't bother checking for division by zero since
                    // we check the matrix condition above.
                    x =  (  x_.matrixV()
                          * x_.singularValues().cwiseInverse().asDiagonal()
                          * x_.matrixU().transpose())
                        * variance / CBasicStatistics::count(m_S);
                    for (std::size_t i = 0u; i < N; ++i)
                    {
                        result(i,i) = x(i,i);
                        for (std::size_t j = 0u; j < i; ++j)
                        {
                            result(i,j) = x(i,j);
                        }
                    }

                    return true;
                }

                //! Get the gramian of the design matrix.
                template<typename MATRIX>
                void gramian(std::size_t n, MATRIX &x) const
                {
                    for (std::size_t i = 0u; i < n; ++i)
                    {
                        x(i,i) = CBasicStatistics::mean(m_S)(i+i);
                        for (std::size_t j = i+1; j < n; ++j)
                        {
                            x(i,j) = CBasicStatistics::mean(m_S)(i+j);
                        }
                    }
                }

            private:
                //! Sufficient statistics for computing the least squares
                //! regression. There are 3N - 1 in total, for the distinct
                //! values in the information matrix and vector.
                TVectorMeanAccumulator m_S;
        };

        //! Get the predicted value of \p r at \p t.
        template<std::size_t N, typename T>
        static double predict(const CLeastSquaresOnline<N, T> &r, double x)
        {
            if (r.range() < MINIMUM_RANGE_TO_PREDICT)
            {
                return r.mean();
            }

            typename CLeastSquaresOnline<N, T>::TArray params;
            r.parameters(params);

            double result = params[0];
            double xi = x;
            for (std::size_t i = 1u; i < params.size(); ++i, xi *= x)
            {
                result += params[i] * xi;
            }
            return result;
        }

    private:
        //! The minimum range of the predictor variable for which we'll
        //! forward predict using the higher order terms.
        static const double MINIMUM_RANGE_TO_PREDICT;
};

template<std::size_t N_, typename T>
const std::string CRegression::CLeastSquaresOnline<N_, T>::STATISTIC_TAG("a");

}
}

#endif // INCLUDED_prelert_maths_CRegression_h
