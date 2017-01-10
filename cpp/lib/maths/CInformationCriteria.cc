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

#include <maths/CInformationCriteria.h>

#include <maths/Constants.h>

#include <boost/math/distributions/chi_squared.hpp>

namespace ml
{
namespace maths
{
namespace information_criteria_detail
{

namespace
{

//! \brief Shared implementation of log determinant used for the
//! Gaussian information criterion.
template<std::size_t N>
class CLogDeterminant
{
    public:
        typedef typename SEigenMatrixNxN<N>::Type TEigenMatrix;

    public:
        template<typename MATRIX>
        static double compute(const MATRIX &c, double upper)
        {
            Eigen::JacobiSVD<TEigenMatrix> svd(c.toEigen());
            double result = 0.0;
            double epsilon = svd.threshold() * svd.singularValues()(0);
            for (int i = 0u; i < svd.singularValues().size(); ++i)
            {
                result += ::log(std::max(upper * svd.singularValues()(i), epsilon));
            }
            return result;
        }
};

}

double confidence(double df)
{
    static const double VARIANCE_CONFIDENCE = 0.99;
    boost::math::chi_squared_distribution<> chi(df);
    return boost::math::quantile(chi, VARIANCE_CONFIDENCE) / df;
}

#define LOG_DETERMINANT(N)                                     \
double logDeterminant(const CSymmetricMatrixNxN<double, N> &c, \
                      double upper)                            \
{                                                              \
    return CLogDeterminant<N>::compute(c, upper);              \
}
LOG_DETERMINANT(2)
LOG_DETERMINANT(3)
LOG_DETERMINANT(4)
LOG_DETERMINANT(5)
LOG_DETERMINANT(6)
LOG_DETERMINANT(7)
LOG_DETERMINANT(8)
LOG_DETERMINANT(9)
#undef LOG_DETERMINANT

}
}
}
