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

#ifndef INCLUDED_ml_maths_CLinearAlgebraTools_h
#define INCLUDED_ml_maths_CLinearAlgebraTools_h

#include <core/CContainerPrinter.h>
#include <core/CLogger.h>

#include <maths/CLinearAlgebra.h>
#include <maths/CTools.h>
#include <maths/ImportExport.h>

#include <boost/math/distributions/normal.hpp>

#include <cstddef>
#include <limits>
#include <vector>

namespace ml
{
namespace maths
{

typedef std::vector<std::size_t> TSizeVec;

namespace linear_algebra_tools_detail
{

#define INVERSE_QUADRATIC_PRODUCT(T, N)                                                                 \
MATHS_EXPORT                                                                                            \
maths_t::EFloatingPointErrorStatus inverseQuadraticProduct(std::size_t d,                               \
                                                           const CSymmetricMatrixNxN<T, N> &covariance, \
                                                           const CVectorNx1<T, N> &residual,            \
                                                           double &result,                              \
                                                           bool ignoreSingularSubspace)
INVERSE_QUADRATIC_PRODUCT(CFloatStorage, 2);
INVERSE_QUADRATIC_PRODUCT(CFloatStorage, 3);
INVERSE_QUADRATIC_PRODUCT(CFloatStorage, 4);
INVERSE_QUADRATIC_PRODUCT(CFloatStorage, 5);
INVERSE_QUADRATIC_PRODUCT(CFloatStorage, 6);
INVERSE_QUADRATIC_PRODUCT(CFloatStorage, 7);
INVERSE_QUADRATIC_PRODUCT(CFloatStorage, 8);
INVERSE_QUADRATIC_PRODUCT(CFloatStorage, 9);
INVERSE_QUADRATIC_PRODUCT(double, 2);
INVERSE_QUADRATIC_PRODUCT(double, 3);
INVERSE_QUADRATIC_PRODUCT(double, 4);
INVERSE_QUADRATIC_PRODUCT(double, 5);
INVERSE_QUADRATIC_PRODUCT(double, 6);
INVERSE_QUADRATIC_PRODUCT(double, 7);
INVERSE_QUADRATIC_PRODUCT(double, 8);
INVERSE_QUADRATIC_PRODUCT(double, 9);
#undef INVERSE_QUADRATIC_PRODUCT
MATHS_EXPORT
maths_t::EFloatingPointErrorStatus inverseQuadraticProduct(std::size_t d,
                                                           const CSymmetricMatrix<CFloatStorage> &covariance,
                                                           const CVector<CFloatStorage> &residual,
                                                           double &result,
                                                           bool ignoreSingularSubspace);
MATHS_EXPORT
maths_t::EFloatingPointErrorStatus inverseQuadraticProduct(std::size_t d,
                                                           const CSymmetricMatrix<double> &covariance,
                                                           const CVector<double> &residual,
                                                           double &result,
                                                           bool ignoreSingularSubspace);


#define GAUSSIAN_LOG_LIKELIHOOD(T, N)                                                                 \
MATHS_EXPORT                                                                                          \
maths_t::EFloatingPointErrorStatus gaussianLogLikelihood(std::size_t d,                               \
                                                         const CSymmetricMatrixNxN<T, N> &covariance, \
                                                         const CVectorNx1<T, N> &residual,            \
                                                         double &result,                              \
                                                         bool ignoreSingularSubspace)
GAUSSIAN_LOG_LIKELIHOOD(CFloatStorage, 2);
GAUSSIAN_LOG_LIKELIHOOD(CFloatStorage, 3);
GAUSSIAN_LOG_LIKELIHOOD(CFloatStorage, 4);
GAUSSIAN_LOG_LIKELIHOOD(CFloatStorage, 5);
GAUSSIAN_LOG_LIKELIHOOD(CFloatStorage, 6);
GAUSSIAN_LOG_LIKELIHOOD(CFloatStorage, 7);
GAUSSIAN_LOG_LIKELIHOOD(CFloatStorage, 8);
GAUSSIAN_LOG_LIKELIHOOD(CFloatStorage, 9);
GAUSSIAN_LOG_LIKELIHOOD(double, 2);
GAUSSIAN_LOG_LIKELIHOOD(double, 3);
GAUSSIAN_LOG_LIKELIHOOD(double, 4);
GAUSSIAN_LOG_LIKELIHOOD(double, 5);
GAUSSIAN_LOG_LIKELIHOOD(double, 6);
GAUSSIAN_LOG_LIKELIHOOD(double, 7);
GAUSSIAN_LOG_LIKELIHOOD(double, 8);
GAUSSIAN_LOG_LIKELIHOOD(double, 9);
#undef GAUSSIAN_LOG_LIKELIHOOD
MATHS_EXPORT
maths_t::EFloatingPointErrorStatus gaussianLogLikelihood(std::size_t d,
                                                         const CSymmetricMatrix<CFloatStorage> &covariance,
                                                         const CVector<CFloatStorage> &residual,
                                                         double &result,
                                                         bool ignoreSingularSubspace);
MATHS_EXPORT
maths_t::EFloatingPointErrorStatus gaussianLogLikelihood(std::size_t d,
                                                         const CSymmetricMatrix<double> &covariance,
                                                         const CVector<double> &residual,
                                                         double &result,
                                                         bool ignoreSingularSubspace);

//! Shared implementation of Gaussian sampling.
#define SAMPLE_GAUSSIAN(T, N)                                    \
MATHS_EXPORT                                                     \
void sampleGaussian(std::size_t n,                               \
                    const CVectorNx1<T, N> &mean,                \
                    const CSymmetricMatrixNxN<T, N> &covariance, \
                    std::vector<CVectorNx1<double, N> > &result)
SAMPLE_GAUSSIAN(CFloatStorage, 2);
SAMPLE_GAUSSIAN(CFloatStorage, 3);
SAMPLE_GAUSSIAN(CFloatStorage, 4);
SAMPLE_GAUSSIAN(CFloatStorage, 5);
SAMPLE_GAUSSIAN(CFloatStorage, 6);
SAMPLE_GAUSSIAN(CFloatStorage, 7);
SAMPLE_GAUSSIAN(CFloatStorage, 8);
SAMPLE_GAUSSIAN(CFloatStorage, 9);
SAMPLE_GAUSSIAN(double, 2);
SAMPLE_GAUSSIAN(double, 3);
SAMPLE_GAUSSIAN(double, 4);
SAMPLE_GAUSSIAN(double, 5);
SAMPLE_GAUSSIAN(double, 6);
SAMPLE_GAUSSIAN(double, 7);
SAMPLE_GAUSSIAN(double, 8);
SAMPLE_GAUSSIAN(double, 9);
#undef SAMPLE_GAUSSIAN
MATHS_EXPORT
void sampleGaussian(std::size_t n,
                    const CVector<CFloatStorage> &mean,
                    const CSymmetricMatrix<CFloatStorage> &covariance,
                    std::vector<CVector<double> > &result);
MATHS_EXPORT
void sampleGaussian(std::size_t n,
                    const CVector<double> &mean,
                    const CSymmetricMatrix<double> &covariance,
                    std::vector<CVector<double> > &result);

//! Shared implementation of the log-determinant function.
#define LOG_DETERMINANT(T, N)                                                              \
MATHS_EXPORT                                                                               \
maths_t::EFloatingPointErrorStatus logDeterminant(std::size_t d,                           \
                                                  const CSymmetricMatrixNxN<T, N> &matrix, \
                                                  double &result,                          \
                                                  bool ignoreSingularSubspace)
LOG_DETERMINANT(CFloatStorage, 2);
LOG_DETERMINANT(CFloatStorage, 3);
LOG_DETERMINANT(CFloatStorage, 4);
LOG_DETERMINANT(CFloatStorage, 5);
LOG_DETERMINANT(CFloatStorage, 6);
LOG_DETERMINANT(CFloatStorage, 7);
LOG_DETERMINANT(CFloatStorage, 8);
LOG_DETERMINANT(CFloatStorage, 9);
LOG_DETERMINANT(double, 2);
LOG_DETERMINANT(double, 3);
LOG_DETERMINANT(double, 4);
LOG_DETERMINANT(double, 5);
LOG_DETERMINANT(double, 6);
LOG_DETERMINANT(double, 7);
LOG_DETERMINANT(double, 8);
LOG_DETERMINANT(double, 9);
#undef LOG_DETERMINANT
MATHS_EXPORT
maths_t::EFloatingPointErrorStatus logDeterminant(std::size_t d,
                                                  const CSymmetricMatrix<CFloatStorage> &matrix,
                                                  double &result,
                                                  bool ignoreSingularSubspace);
MATHS_EXPORT
maths_t::EFloatingPointErrorStatus logDeterminant(std::size_t d,
                                                  const CSymmetricMatrix<double> &matrix,
                                                  double &result,
                                                  bool ignoreSingularSubspace);

}

//! Compute the inverse quadratic form \f$x^tC^{-1}x\f$.
//!
//! \param[in] covariance The matrix.
//! \param[in] residual The vector.
//! \param[out] result Filled in with the log likelihood.
//! \param[in] ignoreSingularSubspace If true then we ignore the
//! residual on a singular subspace of m. Otherwise the result is
//! minus infinity in this case.
template<typename T, std::size_t N>
maths_t::EFloatingPointErrorStatus inverseQuadraticForm(const CSymmetricMatrixNxN<T, N> &covariance,
                                                        const CVectorNx1<T, N> &residual,
                                                        double &result,
                                                        bool ignoreSingularSubspace = true)
{
    return linear_algebra_tools_detail::inverseQuadraticProduct(N, covariance, residual,
                                                                result,
                                                                ignoreSingularSubspace);
}

//! Compute the log-likelihood for the residual \p x and covariance
//! matrix \p m.
//!
//! \param[in] covariance The matrix.
//! \param[in] residual The vector.
//! \param[out] result Filled in with the log likelihood.
//! \param[in] ignoreSingularSubspace If true then we ignore the
//! residual on a singular subspace of m. Otherwise the result is
//! minus infinity in this case.
template<typename T, std::size_t N>
maths_t::EFloatingPointErrorStatus gaussianLogLikelihood(const CSymmetricMatrixNxN<T, N> &covariance,
                                                         const CVectorNx1<T, N> &residual,
                                                         double &result,
                                                         bool ignoreSingularSubspace = true)
{
    return linear_algebra_tools_detail::gaussianLogLikelihood(N, covariance, residual,
                                                              result,
                                                              ignoreSingularSubspace);
}

//! Sample from a Gaussian with \p mean and \p covariance in such
//! a way as to preserve the mean, covariance matrix and some of
//! the quantiles of the generalised c.d.f.
//!
//! \param[in] n The desired number of samples.
//! \param[in] mean The mean of the Gaussian.
//! \param[in] covariance The covariance matrix of the Gaussian.
//! \param[out] result Filled in with the samples.
template<typename T, typename U, std::size_t N>
void sampleGaussian(std::size_t n,
                    const CVectorNx1<T, N> &mean,
                    const CSymmetricMatrixNxN<T, N> &covariance,
                    std::vector<CVectorNx1<U, N> > &result)
{
    return linear_algebra_tools_detail::sampleGaussian(n, mean, covariance, result);
}

//! Compute the log-determinant of the symmetric matrix \p m.
//!
//! \param[in] matrix The matrix.
//! \param[in] ignoreSingularSubspace If true then we ignore any
//! singular subspace of m. Otherwise, the result is minus infinity.
template<typename T, std::size_t N>
maths_t::EFloatingPointErrorStatus logDeterminant(const CSymmetricMatrixNxN<T, N> &matrix,
                                                  double &result,
                                                  bool ignoreSingularSubspace = true)
{
    return linear_algebra_tools_detail::logDeterminant(N, matrix, result, ignoreSingularSubspace);
}


//! Compute the inverse quadratic form \f$x^tC^{-1}x\f$.
//!
//! \param[in] covariance The matrix.
//! \param[in] residual The vector.
//! \param[out] result Filled in with the log likelihood.
//! \param[in] ignoreSingularSubspace If true then we ignore the
//! residual on a singular subspace of m. Otherwise the result is
//! minus infinity in this case.
template<typename T>
maths_t::EFloatingPointErrorStatus inverseQuadraticForm(const CSymmetricMatrix<T> &covariance,
                                                        const CVector<T> &residual,
                                                        double &result,
                                                        bool ignoreSingularSubspace = true)
{
    return linear_algebra_tools_detail::inverseQuadraticProduct(covariance.rows(),
                                                                covariance, residual,
                                                                result,
                                                                ignoreSingularSubspace);
}

//! Compute the log-likelihood for the residual \p x and covariance
//! matrix \p m.
//!
//! \param[in] covariance The covariance matrix.
//! \param[in] residual The residual, i.e. x - mean.
//! \param[out] result Filled in with the log likelihood.
//! \param[in] ignoreSingularSubspace If true then we ignore the
//! residual on a singular subspace of m. Otherwise the result is
//! minus infinity in this case.
template<typename T>
maths_t::EFloatingPointErrorStatus gaussianLogLikelihood(const CSymmetricMatrix<T> &covariance,
                                                         const CVector<T> &residual,
                                                         double &result,
                                                         bool ignoreSingularSubspace = true)
{
    return linear_algebra_tools_detail::gaussianLogLikelihood(covariance.rows(),
                                                              covariance, residual,
                                                              result,
                                                              ignoreSingularSubspace);
}

//! Sample from a Gaussian with \p mean and \p covariance in such
//! a way as to preserve the mean, covariance matrix and some of
//! the quantiles of the generalised c.d.f.
//!
//! \param[in] n The desired number of samples.
//! \param[in] mean The mean of the Gaussian.
//! \param[in] covariance The covariance matrix of the Gaussian.
//! \param[out] result Filled in with the samples.
template<typename T, typename U>
void sampleGaussian(std::size_t n,
                    const CVector<T> &mean,
                    const CSymmetricMatrix<T> &covariance,
                    std::vector<CVector<U> > &result)
{
    return linear_algebra_tools_detail::sampleGaussian(n, mean, covariance, result);
}

//! Compute the log-determinant of the symmetric matrix \p m.
//!
//! \param[in] matrix The matrix.
//! \param[in] ignoreSingularSubspace If true then we ignore any
//! singular subspace of m. Otherwise, the result is minus infinity.
template<typename T>
maths_t::EFloatingPointErrorStatus logDeterminant(const CSymmetricMatrix<T> &matrix,
                                                  double &result,
                                                  bool ignoreSingularSubspace = true)
{
    return linear_algebra_tools_detail::logDeterminant(matrix.rows(), matrix, result, ignoreSingularSubspace);
}

//! Project the matrix on to \p subspace.
template<typename MATRIX>
inline Eigen::Matrix<double, Eigen::Dynamic, Eigen::Dynamic>
    projectedMatrix(const TSizeVec &subspace,
                    const MATRIX &matrix)
{
    std::size_t d = subspace.size();
    Eigen::Matrix<double, Eigen::Dynamic, Eigen::Dynamic> result(d, d);
    for (std::size_t i = 0u; i < d; ++i)
    {
        for (std::size_t j = 0u; j < d; ++j)
        {
            result(i,j) = matrix(subspace[i], subspace[j]);
        }
    }
    return result;
}

//! Project the vector on to \p subspace.
template<typename VECTOR>
inline Eigen::Matrix<double, Eigen::Dynamic, 1>
    projectedVector(const TSizeVec &subspace,
                    const VECTOR &vector)
{
    std::size_t d = subspace.size();
    Eigen::Matrix<double, Eigen::Dynamic, 1> result(d);
    for (std::size_t i = 0u; i < d; ++i)
    {
        result(i) = vector(subspace[i]);
    }
    return result;
}

}
}

#endif // INCLUDED_ml_maths_CLinearAlgebraTools_h
