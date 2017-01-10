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

#ifndef INCLUDED_ml_maths_CBasicStatistics_h
#define INCLUDED_ml_maths_CBasicStatistics_h

#include <core/CContainerPrinter.h>
#include <core/CHashing.h>
#include <core/CLogger.h>
#include <core/CMemory.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>
#include <core/CStringUtils.h>

#include <maths/CDoublePrecisionStorage.h>
#include <maths/CFloatStorage.h>
#include <maths/CLinearAlgebraFwd.h>
#include <maths/CTypeConversions.h>
#include <maths/ImportExport.h>

#include <boost/array.hpp>
#include <boost/bind.hpp>
#include <boost/operators.hpp>
#include <boost/static_assert.hpp>

#include <algorithm>
#include <cmath>
#include <iomanip>
#include <ios>
#include <functional>
#include <string>
#include <vector>

#include <stdint.h>


namespace ml
{
namespace maths
{

namespace basic_statistics_detail
{

//! Function to do conversion from string.
template<typename T>
bool stringToType(const std::string &str, T &value)
{
    return core::CStringUtils::stringToType(str, value);
}
//! Function to do conversion from string to float storage.
inline bool stringToType(const std::string &str, CFloatStorage &value)
{
    return value.fromString(str);
}
//! Function to do conversion from string to double storage.
inline bool stringToType(const std::string &str, CDoublePrecisionStorage &value)
{
    double d;
    if (core::CStringUtils::stringToType<double>(str, d) == false)
    {
        return false;
    }
    value = d;
    return true;
}
//! Function to do conversion from string to a vector.
template<typename T, std::size_t N>
bool stringToType(const std::string &str, CVectorNx1<T, N> &value)
{
    return value.fromDelimited(str);
}
//! Function to do conversion from string to a symmetric matrix.
template<typename T, std::size_t N>
bool stringToType(const std::string &str, CSymmetricMatrixNxN<T, N> &value)
{
    return value.fromDelimited(str);
}

//! Function to do conversion to a string.
template<typename T>
inline std::string typeToString(const T &value)
{
    return core::CStringUtils::typeToStringPrecise(value, core::CIEEE754::E_SinglePrecision);
}
//! Function to do conversion to a string from float storage.
inline std::string typeToString(const CFloatStorage &value)
{
    return value.toString();
}
//! Function to do conversion to a string from double storage.
inline std::string typeToString(const CDoublePrecisionStorage &value)
{
    return core::CStringUtils::typeToStringPrecise(double(value), core::CIEEE754::E_DoublePrecision);
}
//! Function to do conversion to a string from a vector.
template<typename T, std::size_t N>
inline std::string typeToString(const CVectorNx1<T, N> &value)
{
    return value.toDelimited();
}
//! Function to do conversion to a string from a symmetric matrix.
template<typename T, std::size_t N>
inline std::string typeToString(const CSymmetricMatrixNxN<T, N> &value)
{
    return value.toDelimited();
}

//! Default undefined custom add function for points to the covariance
//! estimator.
template<typename POINT>
struct SCovariancesCustomAdd
{
};

//! Default undefined covariance matrix shrinkage estimator.
template<typename POINT>
struct SCovariancesLedoitWolf
{
};

}

//! \brief Some basic stats utilities.
//!
//! DESCRIPTION:\n
//! Some utilities for computing basic sample statistics such
//! as central moments, covariance matrices and so on.
class MATHS_EXPORT CBasicStatistics
{
    public:
        typedef std::pair<double, double> TDoubleDoublePr;
        typedef std::vector<double> TDoubleVec;
        typedef TDoubleVec::const_iterator TDoubleVecCItr;
        typedef std::pair<TDoubleVec, TDoubleVec> TDoubleVecDoubleVecPr;

    public:
        //! Compute the mean of a pair.
        static double mean(const TDoubleDoublePr &samples);

        //! Compute the vector mean of a pair.
        template<typename VECTOR>
        static VECTOR mean(const std::pair<VECTOR, VECTOR> &samples)
        {
            std::size_t n = std::min(samples.first.size(), samples.second.size());
            VECTOR result;
            result.reserve(n);
            for (std::size_t i = 0u; i < n; ++i)
            {
                result.push_back(0.5 * (samples.first[i] + samples.second[i]));
            }
            return result;
        }

        //! Compute the mean of a sample distribution
        static double mean(const TDoubleVec &sample);

        //! Compute the mean of a partially pre-aggregated sample distribution
        static double meanPartialPreSum(uint64_t aggregateCount,
                                        double aggregateSum,
                                        const TDoubleVec &sample);

        //! Compute the mean of a partially pre-aggregated sample distribution
        static double meanPartialPreCalc(uint64_t aggregateCount,
                                         double aggregateMean,
                                         const TDoubleVec &sample);

        //! Standard deviation (sample standard deviation)
        static bool   sampleStandardDeviation(const TDoubleVec &sample,
                                              double &sd);

        //! Partially pre-aggregated standard deviation (sample standard deviation)
        static bool   sampleStandardDeviationPartialPreSum(uint64_t aggregateCount,
                                                           double aggregateSumSquares,
                                                           const TDoubleVec &sample,
                                                             double mean,
                                                             double &sd);

        //! Partially pre-aggregated standard deviation (sample standard deviation)
        static bool   sampleStandardDeviationPartialPreCalc(uint64_t aggregateCount,
                                                            double aggregateMean,
                                                            double aggregateSumDiffMeanSquares,
                                                            const TDoubleVec &sample,
                                                            double &sd);

        //! Variance (sample variance)
        static bool   sampleVariance(const TDoubleVec &sample,
                                     double &var);

        //! Partially pre-aggregated variance (sample variance)
        static bool   sampleVariancePartialPreSum(uint64_t aggregateCount,
                                                  double aggregateSumSquares,
                                                  const TDoubleVec &sample,
                                                    double mean,
                                                    double &var);

        //! Partially pre-aggregated variance (sample variance)
        static bool   sampleVariancePartialPreCalc(uint64_t aggregateCount,
                                                   double aggregateMean,
                                                   double aggregateSumDiffMeanSquares,
                                                   const TDoubleVec &sample,
                                                   double &var);

        //! VMR
        static bool   sampleVarianceToMeanRatio(const TDoubleVec &sample,
                                                double &mean,
                                                double &vmr);

        //! Compute median
        static double median(const TDoubleVec &dataIn);

        //! Compute the first quartile
        static double firstQuartile(const TDoubleVec &dataIn);

        //! Compute the third quartile
        static double thirdQuartile(const TDoubleVec &dataIn);


        /////////////////////////// ACCUMULATORS ///////////////////////////

    //private:
        //! Delimiter used to persist central moments to a string buffer.
        static const char DELIMITER;

        //! \brief An accumulator class for sample central moments.
        //!
        //! DESCRIPTION:\n
        //! This function object accumulates sample central moments for a set
        //! of samples passed to its function operator.
        //!
        //! It is capable of calculating the mean and the 2nd and 3rd central
        //! moments. These can be used to calculate the sample mean, variance
        //! and skewness. Free functions are defined to compute these.
        //!
        //! IMPLEMENTATION DECISIONS:\n
        //! This is templatized to support, for example, float when space is at
        //! a premium or something with higher precision than double when accuracy
        //! is at a premium. The type T must technically be an infinite field
        //! (in the mathematical sense). In particular, any floating point would
        //! do, also custom rational or complex types would work provided they
        //! overload "*" and "/" and define the necessary constructors; however,
        //! integral types won't work.
        //!
        //! The number of moments computed is supplied as a template parameter
        //! so exactly enough memory is allocated to store the results of the
        //! calculations. It is up to the user to not pass bad values for this
        //! parameter. Typedefs are provided to get hold of appropriate objects
        //! for computing the mean, mean and variance and mean, variance and
        //! skewness. Free functions are used to get hold of these quantities.
        //! They are overloaded for explicit values of the number of moments to
        //! provide compile time checking that the moment is available.
        //!
        //! We use recurrence relations for the higher order moments which
        //! minimize the cancellation errors. These can be derived by considering,\n
        //! <pre class="fragment">
        //!   \f$M(n, N) = \sum_{i=1}^{N}{ (x_i - M(1, N))^n } = \sum_{i=1}^{N}{ (x_i - M(1, N-1) + (M(1, N-1) - M(1, N)))^n }\f$
        //! </pre>
        //!
        //! where,\n
        //!   \f$M(1, N)\f$ is defined to be the sample mean of \f$N\f$ samples,\n
        //!   \f$n > 1\f$ for the higher order central moments, and\n
        //!
        //! Using these relations means that the lower order moments are used to
        //! calculate the higher order moments, so we only allow the user to select
        //! the highest order moment to compute.
        //!
        //! Note, that this is loosely modeled on the boost accumulator statistics
        //! library. This makes use of boost::mpl which doesn't compile on all our
        //! supported platforms. Also, it isn't very good at managing cancellation
        //! errors.
        //!
        //! \tparam T The "floating point" type.
        //! \tparam ORDER The highest order moment to gather.
        template<typename T, unsigned int ORDER>
        struct SSampleCentralMoments : public std::unary_function<T, void>
        {
            typedef typename SCoordinate<T>::Type TCoordinate;

            //! See core::CMemory.
            static bool dynamicSizeAlwaysZero(void)
            {
                return core::memory_detail::SDynamicSizeAlwaysZero<T>::value();
            }

            explicit SSampleCentralMoments(const T &initial = T(0)) : s_Count(0)
            {
                std::fill_n(s_Moments, ORDER, initial);
            }

            //! Copy construction from implicitly convertible type.
            template<typename U>
            SSampleCentralMoments(const SSampleCentralMoments<U, ORDER> &other) :
                s_Count(other.s_Count)
            {
                std::copy(other.s_Moments, other.s_Moments + ORDER, s_Moments);
            }

            //! Assignment from implicitly convertible type.
            template<typename U>
            const SSampleCentralMoments &
                operator=(const SSampleCentralMoments<U, ORDER> &other)
            {
                s_Count = other.s_Count;
                std::copy(other.s_Moments, other.s_Moments + ORDER, s_Moments);
                return *this;
            }

            //! \name Persistence
            //@{
            //! Create from delimited values.
            bool fromDelimited(const std::string &str)
            {
                return this->fromDelimitedPart(str, 0);
            }
            //! Create from delimited values.
            bool fromDelimitedPart(const std::string &str, size_t startPos)
            {
                if (str.empty())
                {
                    LOG_ERROR("Empty accumulator representation");
                    return false;
                }

                // Reuse this same string to avoid as many memory allocations as
                // possible.  The reservation is 15 because:
                // a) For string implementations using the short string
                //    optimisation we don't want to cause an unnecessary
                //    allocation
                // b) The count comes first and it's likely to be shorter than
                //    the subsequent moments
                std::string token;
                token.reserve(15);
                size_t delimPos(str.find(DELIMITER, startPos));
                if (delimPos == std::string::npos)
                {
                    token.assign(str, startPos, str.length() - startPos);
                }
                else
                {
                    token.assign(str, startPos, delimPos - startPos);
                }

                if (!basic_statistics_detail::stringToType(token, s_Count))
                {
                    LOG_ERROR("Invalid count : element " << token << " in " << str);
                    return false;
                }

                size_t lastDelimPos(delimPos);
                size_t index(0);
                while (lastDelimPos != std::string::npos)
                {
                    delimPos = str.find(DELIMITER, lastDelimPos + 1);
                    if (delimPos == std::string::npos)
                    {
                        token.assign(str, lastDelimPos + 1, str.length() - lastDelimPos);
                    }
                    else
                    {
                        token.assign(str, lastDelimPos + 1, delimPos - lastDelimPos - 1);
                    }

                    if (!basic_statistics_detail::stringToType(token, s_Moments[index++]))
                    {
                        LOG_ERROR("Invalid moment " << index
                                  << " : element " << token << " in " << str);
                        return false;
                    }

                    lastDelimPos = delimPos;
                }

                return true;
            }

            //! Persist state to delimited values
            std::string toDelimited(void) const
            {
                std::string result(basic_statistics_detail::typeToString(s_Count));
                for (std::size_t index = 0; index < ORDER; ++index)
                {
                    result += DELIMITER;
                    result += basic_statistics_detail::typeToString(s_Moments[index]);
                }

                return result;
            }

            //! Pop a delimited value from the back of the string \p buffer.
            //!
            //! \note This is intended to be used in conjunction with pushDelimited
            //! to persist multiple moments to the same string buffer. For example,
            //! if one has a vector of moments objects then the following code would
            //! persist and restore all the objects to a single string buffer. Note
            //! in particular, the objects are persisted in reverse order, this is
            //! because they are popped from the back of the string on restore, but
            //! appended to the vector. This minimizes the number of copies.
            //!
            //! \code{cpp}
            //!   std::vector<SSampleCentralMoments<double, 2> > moments;
            //!
            //!   ...
            //!
            //!   std::string buffer;
            //!   for (std::size_t i = moments.size(); i-- > 0; /**/)
            //!   {
            //!       moments[i].pushDelimited(buffer);
            //!   }
            //!
            //!   std::vector<SSampleCentralMoments<double, 2> > restored;
            //!   while (!buffer.empty())
            //!   {
            //!       restored.push_back(SSampleCentralMoments<double, 2>());
            //!       restored.back().popDelimited(buffer);
            //!   }
            //! \endcode
            bool popDelimited(std::string &buffer)
            {
                if (buffer.empty())
                {
                    LOG_ERROR("Buffer is empty");
                    return false;
                }
                std::size_t i = buffer.size();
                for (std::size_t n = 0u; i > 0; --i)
                {
                    if (buffer[i - 1] == DELIMITER && ++n == ORDER + 2)
                    {
                        break;
                    }
                }
                buffer.erase(buffer.size() - 1);
                bool result = this->fromDelimitedPart(buffer, i);
                buffer.erase(i);
                return result;
            }

            //! Append a delimited value to the back of the string \p buffer.
            //!
            //! \note This is intended to be used in conjunction with popDelimited.
            //! See that function for more details on usage.
            void pushDelimited(std::string &buffer) const
            {
                buffer += toDelimited() + DELIMITER;
            }
            //@}

            //! Total order based on count then lexicographical less of moments.
            bool operator<(const SSampleCentralMoments &rhs) const
            {
                return    s_Count < rhs.s_Count
                       || (   s_Count == rhs.s_Count
                           && std::lexicographical_compare(s_Moments, s_Moments + ORDER,
                                                           rhs.s_Moments, rhs.s_Moments + ORDER));
            }

            //! \name Update
            //@{
            //! Define a function operator for use with std:: algorithms.
            inline void operator()(const T &x)
            {
                this->add(x);
            }

            //! Update the moments with the collection \p x.
            template<typename U>
            void add(const std::vector<U> &x)
            {
                for (std::size_t i = 0u; i < x.size(); ++i)
                {
                    this->add(x[i]);
                }
            }

            //! Update with a generic value \p x.
            template<typename U>
            void add(const U &x, const TCoordinate &n = TCoordinate(1));

            //! Update the moments with \p x. \p n is the optional number
            //! of times to add \p x.
            void add(const T &x, const TCoordinate &n, int)
            {
                if (n == TCoordinate(0))
                {
                    return;
                }

                s_Count += n;

                // Note we don't trap the case alpha is less than epsilon,
                // because then we'd have to compute epsilon and it is very
                // unlikely the count will get big enough.
                TCoordinate alpha = n / s_Count;
                TCoordinate beta = TCoordinate(1) - alpha;

                T mean(s_Moments[0]);
                s_Moments[0] = beta * mean + alpha * x;

                if (ORDER > 1)
                {
                    T r(x - s_Moments[0]);
                    T r2(r * r);

                    T dMean(mean - s_Moments[0]);
                    T dMean2(dMean * dMean);

                    T variance(s_Moments[1]);

                    s_Moments[1] = beta * (variance + dMean2) + alpha * r2;

                    if (ORDER > 2)
                    {
                        T skew(s_Moments[2]);
                        T dSkew((TCoordinate(3) * variance + dMean2) * dMean);

                        s_Moments[2] = beta * (skew + dSkew) + alpha * r2 * r;
                    }
                }
            }

            //! Combine two moments. This is equivalent to running
            //! a single accumulator on the entire collection.
            template<typename U>
            const SSampleCentralMoments &operator+=(const SSampleCentralMoments<U, ORDER> &rhs)
            {
                if (rhs.s_Count == TCoordinate(0))
                {
                    return *this;
                }

                s_Count = s_Count + rhs.s_Count;

                // Note we don't trap the case alpha is less than epsilon,
                // because then we'd have to compute epsilon and it is very
                // unlikely the count will get big enough.
                TCoordinate alpha = rhs.s_Count / s_Count;
                TCoordinate beta = TCoordinate(1) - alpha;

                T meanLhs(s_Moments[0]);
                T meanRhs(rhs.s_Moments[0]);

                s_Moments[0] = beta * meanLhs + alpha * meanRhs;

                if (ORDER > 1)
                {
                    T dMeanLhs(meanLhs - s_Moments[0]);
                    T dMean2Lhs(dMeanLhs * dMeanLhs);
                    T varianceLhs(s_Moments[1]);

                    T dMeanRhs(meanRhs - s_Moments[0]);
                    T dMean2Rhs(dMeanRhs * dMeanRhs);
                    T varianceRhs(rhs.s_Moments[1]);

                    s_Moments[1] =    beta * (varianceLhs + dMean2Lhs)
                                   + alpha * (varianceRhs + dMean2Rhs);

                    if (ORDER > 2)
                    {
                        T skewLhs(s_Moments[2]);
                        T dSkewLhs((TCoordinate(3) * varianceLhs + dMean2Lhs) * dMeanLhs);

                        T skewRhs(rhs.s_Moments[2]);
                        T dSkewRhs((TCoordinate(3) * varianceRhs + dMean2Rhs) * dMeanRhs);

                        s_Moments[2] =    beta * (skewLhs + dSkewLhs)
                                       + alpha * (skewRhs + dSkewRhs);
                    }
                }

                return *this;
            }

            //! Combine two moments. This is equivalent to running
            //! a single accumulator on the entire collection.
            template<typename U>
            SSampleCentralMoments operator+(const SSampleCentralMoments<U, ORDER> &rhs) const
            {
                SSampleCentralMoments result(*this);
                return result += rhs;
            }

            //! Subtract \p rhs from these.
            //!
            //! \note That this isn't always well defined. For example,
            //! the count and variance of these moments must be larger
            //! than \p rhs. The caller must ensure that these conditions
            //! are satisfied.
            template<typename U>
            const SSampleCentralMoments &operator-=(const SSampleCentralMoments<U, ORDER> &rhs)
            {
                if (rhs.s_Count == TCoordinate(0))
                {
                    return *this;
                }

                using std::max;

                s_Count = max(s_Count - rhs.s_Count, TCoordinate(0));

                if (s_Count == 0)
                {
                    std::fill_n(s_Moments, ORDER, T(0));
                    return *this;
                }

                // Note we don't trap the case alpha is less than epsilon,
                // because then we'd have to compute epsilon and it is very
                // unlikely the count will get big enough.
                TCoordinate alpha = rhs.s_Count / s_Count;
                TCoordinate beta = TCoordinate(1) + alpha;

                T meanLhs(s_Moments[0]);
                T meanRhs(rhs.s_Moments[0]);

                s_Moments[0] = beta * meanLhs - alpha * meanRhs;

                if (ORDER > 1)
                {
                    T dMeanLhs(s_Moments[0] - meanLhs);
                    T dMean2Lhs(dMeanLhs * dMeanLhs);

                    T dMeanRhs(meanRhs - meanLhs);
                    T dMean2Rhs(dMeanRhs * dMeanRhs);
                    T varianceRhs(rhs.s_Moments[1]);

                    s_Moments[1] = max(   beta * (s_Moments[1] - dMean2Lhs)
                                       - alpha * (varianceRhs + dMean2Rhs - dMean2Lhs), T(0));

                    if (ORDER > 2)
                    {
                        T skewLhs(s_Moments[2]);
                        T dSkewLhs((TCoordinate(3) * s_Moments[1] + dMean2Lhs) * dMeanLhs);

                        T skewRhs(rhs.s_Moments[2]);
                        T dSkewRhs((TCoordinate(3) * varianceRhs + dMean2Rhs) * dMeanRhs);

                        s_Moments[2] =    beta * (skewLhs - dSkewLhs)
                                       - alpha * (skewRhs + dSkewRhs - dSkewLhs);
                    }
                }

                return *this;
            }

            //! Subtract \p rhs from these.
            //!
            //! \note That this isn't always well defined. For example,
            //! the count and variance of these moments must be larger
            //! than \p rhs. The caller must ensure that these conditions
            //! are satisfied.
            template<typename U>
            SSampleCentralMoments operator-(const SSampleCentralMoments<U, ORDER> &rhs) const
            {
                SSampleCentralMoments result(*this);
                return result -= rhs;
            }

            //! Age the moments by reducing the count.
            //! \note \p factor should be in the range [0,1].
            //! \note It must be possible to multiply T by double to use
            //! this method.
            void age(double factor)
            {
                s_Count = s_Count * TCoordinate(factor);
            }
            //@}

            TCoordinate s_Count;
            T s_Moments[ORDER];
        };

    //public:
        //! \name Accumulator Typedefs
        //@{
        //! Accumulator object to compute the sample mean.
        template<typename T>
        struct SSampleMean
        {
            typedef SSampleCentralMoments<T, 1u> TAccumulator;
        };

        //! Accumulator object to compute the sample mean and variance.
        template<typename T>
        struct SSampleMeanVar
        {
            typedef SSampleCentralMoments<T, 2u> TAccumulator;
        };

        //! Accumulator object to compute the sample mean, variance and skewness.
        template<typename T>
        struct SSampleMeanVarSkew
        {
            typedef SSampleCentralMoments<T, 3u> TAccumulator;
        };
        //@}

        //! \name Factory Functions
        //@{
        //! Make a mean accumulator.
        template<typename T>
        static SSampleCentralMoments<T, 1u>
            accumulator(const typename SSampleCentralMoments<T, 1u>::TCoordinate &count,
                        const T &m1)
        {
            SSampleCentralMoments<T, 1u> result;
            result.s_Count = count;
            result.s_Moments[0] = m1;
            return result;
        }

        //! Make a mean and variance accumulator.
        template<typename T>
        static SSampleCentralMoments<T, 2u>
            accumulator(const typename SSampleCentralMoments<T, 1u>::TCoordinate &count,
                        const T &m1,
                        const T &m2)
        {
            SSampleCentralMoments<T, 2u> result;
            result.s_Count = count;
            result.s_Moments[0] = m1;
            result.s_Moments[1] = m2;
            return result;
        }

        //! Make a mean, variance and skew accumulator.
        template<typename T>
        static SSampleCentralMoments<T, 3u>
            accumulator(const typename SSampleCentralMoments<T, 1u>::TCoordinate &count,
                        const T &m1,
                        const T &m2,
                        const T &m3)
        {
            SSampleCentralMoments<T, 3u> result;
            result.s_Count = count;
            result.s_Moments[0] = m1;
            result.s_Moments[1] = m2;
            result.s_Moments[2] = m3;
            return result;
        }
        //@}

        //! Get the specified moment provided it exists
        template<unsigned int M, typename T, unsigned int N>
        static const T &moment(const SSampleCentralMoments<T, N> &accumulator)
        {
            BOOST_STATIC_ASSERT(M <= N);
            return accumulator.s_Moments[M];
        }

        //! Get the specified moment provided it exists
        template<unsigned int M, typename T, unsigned int N>
        static T &moment(SSampleCentralMoments<T, N> &accumulator)
        {
            BOOST_STATIC_ASSERT(M <= N);
            return accumulator.s_Moments[M];
        }

        //! Extract the count from an accumulator object.
        template<typename T, unsigned int N>
        static inline const typename SSampleCentralMoments<T, N>::TCoordinate &
            count(const SSampleCentralMoments<T, N> &accumulator)
        {
            return accumulator.s_Count;
        }

        //! Extract the count from an accumulator object.
        template<typename T, unsigned int N>
        static inline typename SSampleCentralMoments<T, N>::TCoordinate &
            count(SSampleCentralMoments<T, N> &accumulator)
        {
            return accumulator.s_Count;
        }

        //! Extract the mean from an accumulator object.
        template<typename T, unsigned int N>
        static inline const T &mean(const SSampleCentralMoments<T, N> &accumulator)
        {
            return accumulator.s_Moments[0];
        }

        //! Extract the variance from an accumulator object.
        //!
        //! \note This is the unbiased form.
        template<typename T, unsigned int N>
        static inline T variance(const SSampleCentralMoments<T, N> &accumulator)
        {
            typedef typename SSampleCentralMoments<T, N>::TCoordinate TCoordinate;

            BOOST_STATIC_ASSERT(N >= 2);

            if (accumulator.s_Count <= TCoordinate(1))
            {
                return T(0);
            }

            TCoordinate bias =   accumulator.s_Count
                                     / (accumulator.s_Count - TCoordinate(1));

            return bias * accumulator.s_Moments[1];
        }

        //! Extract the variance from an accumulator object.
        //!
        //! \note This is the unbiased form.
        template<typename T, unsigned int N>
        static inline const T &maximumLikelihoodVariance(const SSampleCentralMoments<T, N> &accumulator)
        {
            BOOST_STATIC_ASSERT(N >= 2);
            return accumulator.s_Moments[1];
        }

        //! Extract the skewness from an accumulator object.
        //!
        //! \note This is the biased form.
        template<typename T, unsigned int N>
        static inline T skewness(const SSampleCentralMoments<T, N> &accumulator)
        {
            typedef typename SSampleCentralMoments<T, N>::TCoordinate TCoordinate;

            BOOST_STATIC_ASSERT(N >= 3);

            if (accumulator.s_Count <= TCoordinate(2))
            {
                return T(0);
            }

            T normalization = variance(accumulator);
            using std::sqrt;
            normalization = normalization * sqrt(normalization);

            return accumulator.s_Moments[2] == T(0) ? T(0) : accumulator.s_Moments[2] / normalization;
        }

        //! Get the checksum of an accumulator object.
        template<typename T, unsigned int N>
        static inline uint64_t checksum(const SSampleCentralMoments<T, N> &accumulator)
        {
            std::string raw = basic_statistics_detail::typeToString(accumulator.s_Count);
            for (std::size_t i = 0u; i < N; ++i)
            {
                raw += ' ';
                raw += basic_statistics_detail::typeToString(accumulator.s_Moments[i]);
            }
            core::CHashing::CSafeMurmurHash2String64 hasher;
            return hasher(raw);
        }

        //! \name Print Functions
        //@{
        //! Print a mean accumulator.
        template<typename T>
        static inline std::string print(const SSampleCentralMoments<T, 1u> &accumulator)
        {
            std::ostringstream result;
            result << '(' << count(accumulator)
                   << ", " << mean(accumulator) << ')';
            return result.str();
        }
        //! Print a mean and variance accumulator.
        template<typename T>
        static inline std::string print(const SSampleCentralMoments<T, 2u> &accumulator)
        {
            std::ostringstream result;
            result << '(' << count(accumulator)
                   << ", " << mean(accumulator)
                   << ", " << variance(accumulator) << ')';
            return result.str();
        }
        //! Print a mean, variance and skew accumulator.
        template<typename T>
        static inline std::string print(const SSampleCentralMoments<T, 3u> &accumulator)
        {
            std::ostringstream result;
            result << '(' << count(accumulator)
                   << ", " << mean(accumulator)
                   << ", " << variance(accumulator)
                   << ", " << skewness(accumulator) << ')';
            return result.str();
        }
        //@}

        //! \name Persistence Functions
        //@{
        //! Restore a collection of accumulators from a document node value.
        template<typename T, unsigned int N>
        static inline bool restoreSampleCentralMoments(const core::CStateRestoreTraverser &traverser,
                                                       std::vector<SSampleCentralMoments<T, N> > &accumulators)
        {
            accumulators.clear();
            std::string buffer = traverser.value();
            std::size_t n = std::count(buffer.begin(), buffer.end(), DELIMITER) / (N+1);
            accumulators.reserve(n);
            while (!buffer.empty())
            {
                accumulators.push_back(SSampleCentralMoments<T, N>());
                if (accumulators.back().popDelimited(buffer) == false)
                {
                    return false;
                }
            }
            return true;
        }

        //! Convert a collection of accumulator objects to a document node value.
        // TODO remove this!
        template<typename T, unsigned int N>
        static inline void persistSampleCentralMoments(const std::vector<SSampleCentralMoments<T, N> > &accumulators,
                                                       const std::string &tagName,
                                                       core::CStatePersistInserter &inserter)
        {
            std::string buffer;
            for (std::size_t i = accumulators.size(); i > 0; --i)
            {
                accumulators[i-1].pushDelimited(buffer);
            }
            inserter.insertValue(tagName, buffer);
        }
        //@}

        //! \brief An accumulator class for vector sample mean and covariances.
        //!
        //! DESCRIPTION:\n
        //! This function object accumulates sample mean and covariances for a
        //! set of vector samples passed to its function operator.
        //!
        //! Free functions are defined to retrieve the mean vector and covariances
        //! to match the behavior of SSampleCentralMoments.
        //!
        //! IMPLEMENTATION DECISIONS:\n
        //! This is templatized to support, for example, float when space is at
        //! a premium or long double when accuracy is at a premium. The type T
        //! must technically be an infinite field (in the mathematical sense).
        //! In particular, any floating point would do, also custom rational or
        //! complex types would work provided they overload "*" and "/" and define
        //! the necessary constructors; however, integral types won't work.
        //!
        //! This uses the same recurrence relations as SSampleCentralMoments so
        //! see that class for more information on these.
        //!
        //! \tparam T The "floating point" type.
        //! \tparam N The vector dimension.
        template<typename T, std::size_t N>
        struct SSampleCovariances : public std::unary_function<CVectorNx1<T, N>, void>
        {
            //! See core::CMemory.
            static bool dynamicSizeAlwaysZero(void)
            {
                return core::memory_detail::SDynamicSizeAlwaysZero<T>::value();
            }

            typedef CVectorNx1<T, N> TVector;
            typedef CSymmetricMatrixNxN<T, N> TMatrix;

            SSampleCovariances(void) : s_Count(0), s_Mean(0), s_Covariances(0)
            {}

            SSampleCovariances(T count,
                               const TVector &mean,
                               const TMatrix &covariances) :
                    s_Count(count), s_Mean(mean), s_Covariances(covariances)
            {}

            SSampleCovariances(const TVector &count,
                               const TVector &mean,
                               const TMatrix &covariances) :
                    s_Count(count), s_Mean(mean), s_Covariances(covariances)
            {}

            //! Copy construction from implicitly convertible type.
            template<typename U>
            SSampleCovariances(const SSampleCovariances<U, N> &other) :
                    s_Count(other.s_Count),
                    s_Mean(other.s_Mean),
                    s_Covariances(other.s_Covariances)
            {}

            //! Assignment from implicitly convertible type.
            template<typename U>
            const SSampleCovariances &operator=(const SSampleCovariances<U, N> &other)
            {
                s_Count = other.s_Count;
                s_Mean = other.s_Mean;
                s_Covariances = other.s_Covariances;
                return *this;
            }

            //! Create from a delimited string.
            bool fromDelimited(const std::string &str_)
            {
                std::string str = str_;

                std::size_t count = 0u;
                for (std::size_t i = 0u; i < N; ++i)
                {
                    count = str.find_first_of(CLinearAlgebra::DELIMITER, count + 1);
                }
                if (!s_Count.fromDelimited(str.substr(0, count)))
                {
                    LOG_ERROR("Failed to extract counts from " << str.substr(0, count));
                    return false;
                }

                str = str.substr(count + 1);
                std::size_t means = 0u;
                for (std::size_t i = 0u; i < N; ++i)
                {
                    means = str.find_first_of(CLinearAlgebra::DELIMITER, means + 1);
                }
                if (!s_Mean.fromDelimited(str.substr(0, means)))
                {
                    LOG_ERROR("Failed to extract means from " << str.substr(0, means));
                    return false;
                }

                str = str.substr(means + 1);
                if (!s_Covariances.fromDelimited(str))
                {
                    LOG_ERROR("Failed to extract covariances from " << str);
                    return false;
                }

                return true;
            }

            //! Convert to a delimited string.
            std::string toDelimited(void) const
            {
                return  s_Count.toDelimited()
                      + CLinearAlgebra::DELIMITER
                      + s_Mean.toDelimited()
                      + CLinearAlgebra::DELIMITER
                      + s_Covariances.toDelimited();
            }

            //! \name Update
            //@{
            //! Define a function operator for use with std:: algorithms.
            inline void operator()(const TVector &x)
            {
                this->add(x);
            }

            //! Update the moments with the collection \p x.
            template<typename POINT>
            void add(const std::vector<POINT> &x)
            {
                for (std::size_t i = 0u; i < x.size(); ++i)
                {
                    this->add(x[i]);
                }
            }

            //! Update with a generic point \p x.
            template<typename POINT>
            void add(const POINT &x, const POINT &n = POINT(1))
            {
                basic_statistics_detail::SCovariancesCustomAdd<POINT>::add(x, n, *this);
            }

            //! Update the mean and covariances with \p x.
            void add(const TVector &x, const TVector &n, int)
            {
                if (n == TVector(0))
                {
                    return;
                }

                s_Count += n;

                // Note we don't trap the case alpha is less than epsilon,
                // because then we'd have to compute epsilon and it is very
                // unlikely the count will get big enough.
                TVector alpha = n / s_Count;
                TVector beta = TVector(1) - alpha;

                TVector mean(s_Mean);
                s_Mean = beta * mean + alpha * x;

                TVector r(x - s_Mean);
                TMatrix r2(E_OuterProduct, r);
                TVector dMean(mean - s_Mean);
                TMatrix dMean2(E_OuterProduct, dMean);

                s_Covariances += dMean2;
                scaleCovariances(beta, s_Covariances);
                scaleCovariances(alpha, r2);
                s_Covariances += r2;
            }

            //! Combine two moments. This is equivalent to running
            //! a single accumulator on the entire collection.
            template<typename U>
            const SSampleCovariances &operator+=(const SSampleCovariances<U, N> &rhs)
            {
                s_Count = s_Count + rhs.s_Count;
                if (s_Count == TVector(0))
                {
                    return *this;
                }

                // Note we don't trap the case alpha is less than epsilon,
                // because then we'd have to compute epsilon and it is very
                // unlikely the count will get big enough.
                TVector alpha = rhs.s_Count / s_Count;
                TVector beta = TVector(1) - alpha;

                TVector meanLhs(s_Mean);

                s_Mean = beta * meanLhs + alpha * rhs.s_Mean;

                TVector dMeanLhs(meanLhs - s_Mean);
                TMatrix dMean2Lhs(E_OuterProduct, dMeanLhs);
                TVector dMeanRhs(rhs.s_Mean - s_Mean);
                TMatrix dMean2Rhs(E_OuterProduct, dMeanRhs);

                s_Covariances += dMean2Lhs;
                scaleCovariances(beta, s_Covariances);
                dMean2Rhs += rhs.s_Covariances;
                scaleCovariances(alpha, dMean2Rhs);
                s_Covariances += dMean2Rhs;

                return *this;
            }

            //! Combine two moments. This is equivalent to running
            //! a single accumulator on the entire collection.
            template<typename U>
            SSampleCovariances operator+(const SSampleCovariances<U, N> &rhs) const
            {
                SSampleCovariances result(*this);
                return result += rhs;
            }

            //! Subtract \p rhs from these.
            //!
            //! \note That this isn't always well defined. For example,
            //! the count and variance of these covariances must be
            //! larger than \p rhs. The caller must ensure that these
            //! conditions are satisfied.
            template<typename U>
            const SSampleCovariances &operator-=(const SSampleCovariances<U, N> &rhs)
            {
                using std::max;

                s_Count = max(s_Count - rhs.s_Count, TVector(0));
                if (s_Count == TVector(0))
                {
                    s_Mean = TVector(0);
                    s_Covariances = TMatrix(0);
                    return *this;
                }

                // Note we don't trap the case alpha is less than epsilon,
                // because then we'd have to compute epsilon and it is very
                // unlikely the count will get big enough.
                TVector alpha = rhs.s_Count / s_Count;
                TVector beta = TVector(1) + alpha;

                TVector meanLhs(s_Mean);

                s_Mean = beta * meanLhs - alpha * rhs.s_Mean;

                TVector dMeanLhs(s_Mean - meanLhs);
                TMatrix dMean2Lhs(E_OuterProduct, dMeanLhs);
                TVector dMeanRhs(rhs.s_Mean - meanLhs);
                TMatrix dMean2Rhs(E_OuterProduct, dMeanRhs);

                s_Covariances = s_Covariances - dMean2Lhs;
                scaleCovariances(beta, s_Covariances);
                dMean2Rhs += rhs.s_Covariances - dMean2Lhs;
                scaleCovariances(alpha, dMean2Rhs);
                s_Covariances -= dMean2Rhs;

                // If any of the diagonal elements are negative round them
                // up to zero and zero the corresponding row and column.
                for (std::size_t i = 0u; i < N; ++i)
                {
                    if (s_Covariances(i, i) < T(0))
                    {
                        for (std::size_t j = 0u; j < N; ++j)
                        {
                            s_Covariances(i, j) = T(0);
                        }
                    }
                }

                return *this;
            }

            //! Subtract \p rhs from these.
            //!
            //! \note That this isn't always well defined. For example,
            //! the count and variance of these covariances must be
            //! larger than \p rhs. The caller must ensure that these
            //! conditions are satisfied.
            template<typename U>
            SSampleCovariances operator-(const SSampleCovariances<U, N> &rhs)
            {
                SSampleCovariances result(*this);
                return result -= rhs;
            }

            //! Age the mean and covariances by reducing the count.
            //!
            //! \note \p factor should be in the range [0,1].
            //! \note It must be possible to cast double to T to use
            //! this method.
            void age(double factor)
            {
                s_Count = s_Count * T(factor);
            }
            //@}

            TVector s_Count;
            TVector s_Mean;
            TMatrix s_Covariances;
        };

        //! Make a covariances accumulator.
        template<typename T, std::size_t N>
        static inline SSampleCovariances<T, N> accumulator(T count,
                                                           const CVectorNx1<T, N> &mean,
                                                           const CSymmetricMatrixNxN<T, N> &covariances)
        {
            return SSampleCovariances<T, N>(count, mean, covariances);
        }

        //! Make a covariances accumulator.
        template<typename T, std::size_t N>
        static inline SSampleCovariances<T, N> accumulator(const CVectorNx1<T, N> &count,
                                                           const CVectorNx1<T, N> &mean,
                                                           const CSymmetricMatrixNxN<T, N> &covariances)
        {
            return SSampleCovariances<T, N>(count, mean, covariances);
        }

        //! Extract the count from an accumulator object.
        template<typename T, std::size_t N>
        static inline T count(const SSampleCovariances<T, N> &accumulator)
        {
            return accumulator.s_Count.L1() / static_cast<T>(N);
        }

        //! Extract the mean vector from an accumulator object.
        template<typename T, std::size_t N>
        static inline const CVectorNx1<T, N> &mean(const SSampleCovariances<T, N> &accumulator)
        {
            return accumulator.s_Mean;
        }

        //! Extract the covariance matrix from an accumulator object.
        //!
        //! \note This is the unbiased form.
        template<typename T, std::size_t N>
        static inline CSymmetricMatrixNxN<T, N> covariances(const SSampleCovariances<T, N> &accumulator)
        {
            CVectorNx1<T, N> bias(accumulator.s_Count);
            for (std::size_t i = 0u; i < N; ++i)
            {
                if (bias(i) <= T(1))
                {
                    bias(i) = T(0);
                }
            }
            bias /= (bias - CVectorNx1<T, N>(1));
            CSymmetricMatrixNxN<T, N> result(accumulator.s_Covariances);
            scaleCovariances(bias, result);
            return result;
        }

        //! Extract the covariance matrix from an accumulator object.
        //!
        //! \note This is the unbiased form.
        template<typename T, std::size_t N>
        static inline const CSymmetricMatrixNxN<T, N> &maximumLikelihoodCovariances(const SSampleCovariances<T, N> &accumulator)
        {
            return accumulator.s_Covariances;
        }

        //! Get the checksum of a covariances accumulator.
        template<typename T, std::size_t N>
        static inline uint64_t checksum(const SSampleCovariances<T, N> &accumulator)
        {
            std::string raw = basic_statistics_detail::typeToString(accumulator.s_Count);
            raw += ' ';
            raw += basic_statistics_detail::typeToString(accumulator.s_Mean);
            raw += ' ';
            raw += basic_statistics_detail::typeToString(accumulator.s_Covariances);
            core::CHashing::CSafeMurmurHash2String64 hasher;
            return hasher(raw);
        }

        //! Print a covariances accumulator.
        template<typename T, std::size_t N>
        static inline std::string print(const SSampleCovariances<T, N> &accumulator)
        {
            std::ostringstream result;
            result << "\n{\n"
                   << count(accumulator) << ",\n"
                   << mean(accumulator) << ",\n"
                   << covariances(accumulator) << "\n"
                   << "}";
            return result.str();
        }

        //! Interface for Ledoit Wolf shrinkage estimator of the sample
        //! covariance matrix.
        //!
        //! See http://perso.ens-lyon.fr/patrick.flandrin/LedoitWolf_JMA2004.pdf
        //! for the details.
        //!
        //! \param[in] points The points for which to estimate the covariance
        //! matrix.
        //! \param[out] result Filled in with the count, mean and "shrunk"
        //! covariance matrix estimate.
        template<typename POINT, typename T, std::size_t N>
        static void covariancesLedoitWolf(const std::vector<POINT> &points,
                                         SSampleCovariances<T, N> &result)
        {
            basic_statistics_detail::SCovariancesLedoitWolf<POINT> estimator;
            result.add(points);
            estimator.estimate(points, result);
        }

    private:
        //! \brief Implementation of an accumulator class for order statistics.
        //!
        //! DESCRIPTION:\n
        //! This implements the underlying algorithm for determining the first
        //! n order statistics online.
        //!
        //! IMPLEMENTATION:\n
        //! This maintains the statistics in a heap for worst case complexity
        //! \f$O(N log(n))\f$ and typical complexity \f$O(N)\f$ (by checking
        //! against the maximum value) for \f$n << N\f$.
        //!
        //! The container is supplied as a template argument so that a fixed
        //! size array can be used for the case n is small. Similarly the less
        //! function is supplied so that T can be any type which supports a
        //! partial ordering. (T must also have a default constructor.)
        template<typename T, typename CONTAINER, typename LESS>
        class COrderStatisticsImpl : public std::unary_function<T, void>
        {
            public:
                typedef std::vector<T> TVec;
                typedef typename std::vector<T>::const_iterator TVecCItr;
                typedef typename CONTAINER::iterator iterator;
                typedef typename CONTAINER::const_iterator const_iterator;
                typedef typename CONTAINER::reverse_iterator reverse_iterator;
                typedef typename CONTAINER::const_reverse_iterator const_reverse_iterator;

            public:
                COrderStatisticsImpl(const CONTAINER &statistics, const LESS &less) :
                        m_Less(less),
                        m_Statistics(statistics),
                        m_UnusedCount(statistics.size())
                {
                }

                //! \name Persistence
                //@{
                //! Create from a delimited value.
                bool restore(core::CStateRestoreTraverser &traverser)
                {
                    return this->fromString(traverser.value());
                }

                //! Persist to a delimited value.
                void persist(const std::string &tag,
                             core::CStatePersistInserter &inserter) const
                {
                    if (this->count() == 0)
                    {
                        return;
                    }
                    inserter.insertValue(tag, this->toString());
                }

                //! Initialize from \p value if possible.
                bool fromString(const std::string &value)
                {
                    this->clear();

                    if (value.empty())
                    {
                        return true;
                    }

                    T statistic;

                    std::size_t delimPos = value.find(DELIMITER);
                    if (delimPos == std::string::npos)
                    {
                        if (basic_statistics_detail::stringToType(value, statistic) == false)
                        {
                            LOG_ERROR("Invalid statistic in '" << value << "'");
                            return false;
                        }
                        m_Statistics[--m_UnusedCount] = statistic;
                        return true;
                    }

                    m_UnusedCount = m_Statistics.size();

                    std::string statistic_;
                    statistic_.reserve(15);
                    statistic_.assign(value, 0, delimPos);
                    if (basic_statistics_detail::stringToType(statistic_, statistic) == false)
                    {
                        LOG_ERROR("Invalid statistic '" << statistic_ << "' in '" << value << "'");
                        return false;
                    }
                    m_Statistics[--m_UnusedCount] = statistic;

                    while (delimPos != value.size())
                    {
                        std::size_t nextDelimPos = std::min(value.find(DELIMITER, delimPos + 1),
                                                            value.size());
                        statistic_.assign(value,
                                          delimPos + 1,
                                          nextDelimPos - delimPos - 1);
                        if (basic_statistics_detail::stringToType(statistic_, statistic) == false)
                        {
                            LOG_ERROR("Invalid statistic '" << statistic_ << "' in '" << value << "'");
                            return false;
                        }
                        m_Statistics[--m_UnusedCount] = statistic;
                        delimPos = nextDelimPos;
                    }

                    return true;
                }

                //! Convert to a string.
                std::string toString(void) const
                {
                    if (this->count() == 0)
                    {
                        return std::string();
                    }
                    std::size_t i = m_Statistics.size();
                    std::string result = basic_statistics_detail::typeToString(m_Statistics[i - 1]);
                    for (--i; i > m_UnusedCount; --i)
                    {
                        result += DELIMITER;
                        result += basic_statistics_detail::typeToString(m_Statistics[i - 1]);
                    }
                    return result;
                }
                //@}

                //! \name Update
                //@{
                //! Define a function operator for use with std:: algorithms.
                inline bool operator()(const T &x)
                {
                    return this->add(x);
                }

                //! Check if we would add \p x.
                bool wouldAdd(const T &x) const
                {
                    return m_UnusedCount > 0 || m_Less(x, *this->begin());
                }

                //! Update the statistics with the collection \p xs.
                bool add(const TVec &xs)
                {
                    bool result = false;
                    for (TVecCItr i = xs.begin(); i != xs.end(); ++i)
                    {
                        result |= this->add(*i);
                    }
                    return result;
                }

                //! Update the statistic with \p n copies of \p x.
                bool add(const T &x, std::size_t n)
                {
                    bool result = false;
                    for (std::size_t i = 0u; i < std::min(n, m_Statistics.size()); ++i)
                    {
                        result |= this->add(x);
                    }
                    return result;
                }

                //! Update the statistics with \p x.
#if defined(__GNUC__) && (__GNUC__ == 4) && (__GNUC_MINOR__ == 3)
                __attribute__ ((__noinline__))
#endif // defined(__GNUC__) && (__GNUC__ == 4) && (__GNUC_MINOR__ == 3)
                bool add(const T &x)
                {
                    if (m_UnusedCount > 0)
                    {
                        m_Statistics[--m_UnusedCount] = x;

                        if (m_UnusedCount == 0)
                        {
                            // We need a heap for subsequent insertion.
                            std::make_heap(this->begin(), this->end(), m_Less);
                        }
                        return true;
                    }
                    else if (m_Less(x, *this->begin()))
                    {
                        // We need to drop the largest value and update the heap.
                        std::pop_heap(this->begin(), this->end(), m_Less);
                        m_Statistics.back() = x;
                        std::push_heap(this->begin(), this->end(), m_Less);
                        return true;
                    }
                    return false;
                }

                //! An efficient sort of the statistics (which are not stored
                //! in sorted order during accumulation for efficiency).
                void sort(void)
                {
                    if (m_UnusedCount > 0)
                    {
                        std::sort(this->begin(), this->end(), m_Less);
                    }
                    else
                    {
                        std::sort_heap(this->begin(), this->end(), m_Less);
                    }
                }

                //! Age the values by scaling them.
                //! \note \p factor should be in the range (0,1].
                //! \note It must be possible to multiply T by double to use
                //! this method.
                void age(double factor)
                {
                    if (this->count() == 0)
                    {
                        return;
                    }

                    // Check if we need to multiply or divide by the factor.
                    T tmp = (*this)[0];
                    if (m_Less(static_cast<T>(tmp * factor), tmp))
                    {
                        factor = 1.0 / factor;
                    }

                    for (iterator itr = this->begin(); itr != this->end(); ++itr)
                    {
                        *itr = static_cast<T>((*itr) * factor);
                    }
                }
                //@}

                //! \name Access
                //@{
                //! Get the "biggest" in the collection. This depends on the
                //! order predicate and is effectively the first value which
                //! will be removed if a new value displaces it.
                inline const T &biggest(void) const
                {
                    return *this->begin();
                }

                //! Get the number of statistics.
                inline std::size_t count(void) const
                {
                    return m_Statistics.size() - m_UnusedCount;
                }

                //! Get the i'th statistic.
                inline T &operator[](std::size_t i)
                {
                    return m_Statistics[m_UnusedCount + i];
                }
                //! Get the i'th statistic.
                inline const T &operator[](std::size_t i) const
                {
                    return m_Statistics[m_UnusedCount + i];
                }

                //! Get an iterator over the statistics.
                inline iterator begin(void)
                {
                    return m_Statistics.begin() + m_UnusedCount;
                }
                //! Get an iterator over the statistics.
                inline const_iterator begin(void) const
                {
                    return m_Statistics.begin() + m_UnusedCount;
                }

                //! Get a reverse iterator over the order statistics.
                inline reverse_iterator rbegin(void)
                {
                    return m_Statistics.rbegin();
                }
                //! Get a reverse iterator over the order statistics.
                inline const_reverse_iterator rbegin(void) const
                {
                    return m_Statistics.rbegin();
                }

                //! Get an iterator representing the end of the statistics.
                inline iterator end(void)
                {
                    return m_Statistics.end();
                }
                //! Get an iterator representing the end of the statistics.
                inline const_iterator end(void) const
                {
                    return m_Statistics.end();
                }

                //! Get an iterator representing the end of the statistics.
                inline reverse_iterator rend(void)
                {
                    return m_Statistics.rbegin() + m_UnusedCount;
                }
                //! Get an iterator representing the end of the statistics.
                inline const_reverse_iterator rend(void) const
                {
                    return m_Statistics.rbegin() + m_UnusedCount;
                }
                //@}

                //! Remove all statistics.
                void clear(void)
                {
                    std::fill(m_Statistics.begin() + m_UnusedCount, m_Statistics.end(), T());
                    m_UnusedCount = m_Statistics.size();
                }

                //! Get a checksum of this object.
                uint64_t checksum(uint64_t seed = 0) const
                {
                    if (this->count() == 0)
                    {
                        return seed;
                    }
                    TVec sorted(this->begin(), this->end());
                    std::sort(sorted.begin(), sorted.end(), m_Less);
                    std::string raw = basic_statistics_detail::typeToString(sorted[0]);
                    for (std::size_t i = 1u; i < sorted.size(); ++i)
                    {
                        raw += ' ';
                        raw += basic_statistics_detail::typeToString(sorted[i]);
                    }
                    core::CHashing::CSafeMurmurHash2String64 hasher;
                    return hasher(raw);
                }

                //! Print for debug.
                std::string print(void) const
                {
                    return core::CContainerPrinter::print(this->begin(), this->end());
                }

            protected:
                //! Get the statistics.
                CONTAINER &statistics(void) { return m_Statistics; }

            private:
                LESS m_Less;
                CONTAINER m_Statistics;
                //! How many elements of the container are unused?
                std::size_t m_UnusedCount;
        };

    public:
        //! \brief A stack based accumulator class for order statistics.
        //!
        //! DESCRIPTION:\n
        //! This function object accumulates the first n order statistics
        //! for a partially ordered collection when n is relatively small
        //! and known at compile time.
        //!
        //! The ordering function is supplied by the user and so this can
        //! also be used to calculate the maximum statistics of a collection.
        //! For example:\n
        //! \code{cpp}
        //!   COrderStatistics<double, 2, std::greater<double> >
        //! \endcode
        //!
        //! would find the largest two values of a collection.
        //!
        //! IMPLEMENTATION DECISIONS:\n
        //! This is templatized to support any type for which a partial ordering
        //! can be defined. To this end the ordering is a template parameter
        //! which is also supplied to the constructor in the case it doesn't
        //! have default constructor.
        //!
        //! This object has been implemented to give near optimal performance
        //! both in terms of space and complexity when the number of order
        //! statistics being computed is small. As such, the number of statistics
        //! to compute is supplied as a template parameter so that exactly enough
        //! memory is allocated to store the results and so they are stored on
        //! the stack by default.
        //!
        //! \tparam T The numeric type for which the order statistic is being
        //! computed.
        //! \tparam N The number of order statistics being computed.
        //! \tparam LESS The comparison function object type used to test
        //! if one object of type T is less than another.
        template<typename T, std::size_t N, typename LESS = std::less<T> >
        class COrderStatisticsStack : public COrderStatisticsImpl<T, boost::array<T, N>, LESS>,
                                      private boost::addable<COrderStatisticsStack<T, N, LESS> >
        {
            private:
                typedef boost::array<T, N> TArray;
                typedef COrderStatisticsImpl<T, TArray, LESS> TImpl;

            public:
                // Forward typedefs
                typedef typename TImpl::iterator iterator;
                typedef typename TImpl::const_iterator const_iterator;

                //! See core::CMemory.
                static bool dynamicSizeAlwaysZero(void)
                {
                    return core::memory_detail::SDynamicSizeAlwaysZero<T>::value();
                }

            public:
                explicit COrderStatisticsStack(const LESS &less = LESS()) :
                    TImpl(TArray(), less)
                {
                    this->statistics().assign(T());
                }

                explicit COrderStatisticsStack(std::size_t /*n*/, const LESS &less = LESS()) :
                    TImpl(TArray(), less)
                {
                    this->statistics().assign(T());
                }

                //! Combine two statistics. This is equivalent to running a
                //! single accumulator on the entire collection.
                const COrderStatisticsStack &operator+=(const COrderStatisticsStack &rhs)
                {
                    for (const_iterator xItr = rhs.begin(); xItr != rhs.end(); ++xItr)
                    {
                        this->add(*xItr);
                    }
                    return *this;
                }
        };

        //! \brief A heap based accumulator class for order statistics.
        //!
        //! DESCRIPTION:\n
        //! This function object accumulates the first n order statistics
        //! for a partially ordered collection when n is relatively large
        //! or not known at compile time.
        //!
        //! The ordering function is supplied by the user and so this can be
        //! also used to calculate the maximum statistics of a collection.
        //! For example:\n
        //! \code{cpp}
        //!   COrderStatistics<double, std::greater<double> >
        //! \endcode
        //!
        //! would find the largest values of a collection.
        //!
        //! IMPLEMENTATION DECISIONS:\n
        //! This is templatized to support any type for which a partial ordering
        //! can be defined. To this end the ordering is a template parameter
        //! which is also supplied to the constructor in the case it doesn't
        //! have default constructor.
        //!
        //! The statistics are stored on the heap in an up front allocated vector.
        //! Since the number of statistics must be fixed for the life time of
        //! accumulation it makes sense to allocate the memory once up front.
        //! This also initializes the memory since it shares its implementation
        //! with the array based accumulator.
        //!
        //! \tparam T The numeric type for which the order statistic is being
        //! computed.
        //! \tparam LESS The comparison function object type used to test
        //! if one object of type T is less than another.
        template<typename T, typename LESS = std::less<T> >
        class COrderStatisticsHeap : public COrderStatisticsImpl<T, std::vector<T>, LESS>,
                                     private boost::addable<COrderStatisticsHeap<T, LESS> >
        {
            private:
                typedef std::vector<T> TVec;
                typedef COrderStatisticsImpl<T, TVec, LESS> TImpl;

            public:
                // Forward typedefs
                typedef typename TImpl::iterator iterator;
                typedef typename TImpl::const_iterator const_iterator;

            public:
                explicit COrderStatisticsHeap(std::size_t n, const LESS &less = LESS()) :
                    TImpl(TVec(n, T()), less)
                {
                }

                //! Reset the number of statistics to gather to \p n.
                void resize(std::size_t n)
                {
                    this->clear();
                    this->statistics().resize(n);
                }

                //! Combine two statistics. This is equivalent to running a
                //! single accumulator on the entire collection.
                const COrderStatisticsHeap &operator+=(const COrderStatisticsHeap &rhs)
                {
                    for (const_iterator xItr = rhs.begin(); xItr != rhs.end(); ++xItr)
                    {
                        this->add(*xItr);
                    }
                    return *this;
                }
        };

    // Friends
    template<typename T>
    friend std::ostream &operator<<(std::ostream &o,
                                    const CBasicStatistics::SSampleCentralMoments<T, 1u> &);
    template<typename T>
    friend std::ostream &operator<<(std::ostream &o,
                                    const CBasicStatistics::SSampleCentralMoments<T, 2u> &);
    template<typename T>
    friend std::ostream &operator<<(std::ostream &o,
                                    const CBasicStatistics::SSampleCentralMoments<T, 3u> &);
};

template<typename T>
std::ostream &operator<<(std::ostream &o,
                         const CBasicStatistics::SSampleCentralMoments<T, 1u> &accumulator)
{
    return o << CBasicStatistics::print(accumulator);
}

template<typename T>
std::ostream &operator<<(std::ostream &o,
                         const CBasicStatistics::SSampleCentralMoments<T, 2u> &accumulator)
{
    return o << CBasicStatistics::print(accumulator);
}

template<typename T>
std::ostream &operator<<(std::ostream &o,
                         const CBasicStatistics::SSampleCentralMoments<T, 3u> &accumulator)
{
    return o << CBasicStatistics::print(accumulator);
}

template<typename T, std::size_t N, typename LESS>
std::ostream &operator<<(std::ostream &o,
                         const CBasicStatistics::COrderStatisticsStack<T, N, LESS> &accumulator)
{
    return o << accumulator.print();
}

template<typename T, typename LESS>
std::ostream &operator<<(std::ostream &o,
                         const CBasicStatistics::COrderStatisticsHeap<T, LESS> &accumulator)
{
    return o << accumulator.print();
}

namespace basic_statistics_detail
{

//! \brief Default custom add function for values to the central
//! moments estimator.
template<typename U>
struct SCentralMomentsCustomAdd
{
    template<typename T, unsigned int ORDER>
    static inline void add(const U &x,
                           typename SCoordinate<T>::Type n,
                           CBasicStatistics::SSampleCentralMoments<T, ORDER> &moments)
    {
        moments.add(static_cast<T>(x), n, 0);
    }
};

//! \brief Implementation of add stack vector to the covariances
//! estimator.
template<typename T, std::size_t N>
struct SCovariancesCustomAdd<CVectorNx1<T, N> >
{
    static inline void add(const CVectorNx1<T, N> &x,
                           const CVectorNx1<T, N> &n,
                           CBasicStatistics::SSampleCovariances<T, N> &covariances)
    {
        covariances.add(x, n, 0);
    }
};

//! \brief Implementation of a covariance matrix shrinkage estimator
//! for stack vectors.
//!
//! DESCRIPTION:\n
//! This uses a scaled identity shrinkage which is estimated using
//! Ledoit and Wolf's approach.
//!
//! See http://perso.ens-lyon.fr/patrick.flandrin/LedoitWolf_JMA2004.pdf
//! for the details.
template<typename T, std::size_t N>
struct SCovariancesLedoitWolf<CVectorNx1<T, N> >
{
    template<typename U>
    static void estimate(const std::vector<CVectorNx1<T, N> > &points,
                         CBasicStatistics::SSampleCovariances<U, N> &covariances)
    {
        U d = static_cast<U>(N);

        U n = CBasicStatistics::count(covariances);
        const CVectorNx1<U, N> &m = CBasicStatistics::mean(covariances);
        const CSymmetricMatrixNxN<U, N> &s = CBasicStatistics::maximumLikelihoodCovariances(covariances);

        U mn = s.trace() / d;
        U dn = pow2((s - CVectorNx1<U, N>(mn).diagonal()).frobenius()) / d;
        U bn = 0;
        U z = n * n;
        for (std::size_t i = 0u; i < points.size(); ++i)
        {
            CVectorNx1<U, N> ci(points[i]);
            bn += pow2(((ci - m).outer() - s).frobenius()) / d / z;
        }
        bn = std::min(bn, dn);
        LOG_TRACE("m = " << mn << ", d = " << dn << ", b = " << bn);

        covariances.s_Covariances =  CVectorNx1<U, N>(bn / dn * mn).diagonal()
                                   + (U(1) - bn / dn) * covariances.s_Covariances;
    }

    template<typename U> static U pow2(U x) { return x * x; }
};

}

template<typename T, unsigned int ORDER>
template<typename U>
void CBasicStatistics::SSampleCentralMoments<T, ORDER>::add(const U &x,
                                                            const TCoordinate &n)
{
    basic_statistics_detail::SCentralMomentsCustomAdd<U>::add(x, n, *this);
}


}
}

#endif // INCLUDED_ml_maths_CBasicStatistics_h

