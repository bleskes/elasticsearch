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

#ifndef INCLUDED_prelert_maths_CLinearAlgebra_h
#define INCLUDED_prelert_maths_CLinearAlgebra_h

#include <core/CContainerPrinter.h>
#include <core/CLogger.h>
#include <core/CMemory.h>
#include <core/Constants.h>
#include <core/CPersistUtils.h>
#include <core/CSmallVector.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>
#include <core/CStringUtils.h>

#include <maths/CChecksum.h>
#include <maths/CFloatStorage.h>
#include <maths/CLinearAlgebraFwd.h>
#include <maths/ImportExport.h>

#include <Eigen/Core>
#include <Eigen/Cholesky>
#include <Eigen/LU>
#include <Eigen/QR>
#include <Eigen/SVD>

#include <boost/array.hpp>
#include <boost/geometry.hpp>
#include <boost/geometry/geometries/adapted/boost_array.hpp>
#include <boost/operators.hpp>
#include <boost/numeric/conversion/bounds.hpp>
#include <boost/ref.hpp>

#include <cstddef>
#include <ostream>

#include <math.h>

BOOST_GEOMETRY_REGISTER_BOOST_ARRAY_CS(cs::cartesian)

namespace prelert
{
namespace maths
{

namespace linear_algebra_detail
{

//! SFINAE check that \p N is at least 1.
struct CEmpty {};
template<std::size_t N> struct CBoundsCheck { typedef CEmpty InRange; };
template<> struct CBoundsCheck<0> {};

//! \brief Extracts a vector component / matrix element from a string.
struct SFromString
{
    template<typename T>
    bool operator()(const std::string &token, T &value) const
    {
        return core::CStringUtils::stringToType(token, value);
    }
    bool operator()(const std::string &token, CFloatStorage &value) const
    {
        return value.fromString(token);
    }
};

//! \brief Converts a vector component / matrix element to a string.
struct SToString
{
    template<typename T>
    std::string operator()(const T &value) const
    {
        return core::CStringUtils::typeToString(value);
    }
    std::string operator()(double value) const
    {
        return core::CStringUtils::typeToStringPrecise(value, core::CIEEE754::E_DoublePrecision);
    }
    std::string operator()(CFloatStorage value) const
    {
        return value.toString();
    }
};

//! \brief Common vector functionality for variable storage type.
template<typename STORAGE>
struct SSymmetricMatrix
{
    typedef typename STORAGE::value_type Type;

    //! Get read only reference.
    inline const SSymmetricMatrix &base(void) const { return *this; }

    //! Get writable reference.
    inline SSymmetricMatrix &base(void) { return *this; }

    //! Set this vector equal to \p other.
    template<typename OTHER_STORAGE>
    void assign(const SSymmetricMatrix<OTHER_STORAGE> &other)
    {
        std::copy(other.m_LowerTriangle.begin(),
                  other.m_LowerTriangle.end(),
                  m_LowerTriangle.begin());
    }

    //! Create from a delimited string.
    bool fromDelimited(const std::string &str)
    {
        return core::CPersistUtils::fromString(str,
                                               SFromString(),
                                               m_LowerTriangle,
                                               CLinearAlgebra::DELIMITER);
    }

    //! Convert to a delimited string.
    std::string toDelimited(void) const
    {
        return core::CPersistUtils::toString(m_LowerTriangle,
                                             SToString(),
                                             CLinearAlgebra::DELIMITER);
    }

    //! Get the i,j 'th component (no bounds checking).
    inline Type element(std::size_t i, std::size_t j) const
    {
        if (i < j)
        {
            std::swap(i, j);
        }
        return m_LowerTriangle[i * (i + 1) / 2 + j];
    }

    //! Get the i,j 'th component (no bounds checking).
    inline Type &element(std::size_t i, std::size_t j)
    {
        if (i < j)
        {
            std::swap(i, j);
        }
        return m_LowerTriangle[i * (i + 1) / 2 + j];
    }

    //! Component-wise negative.
    void negative(void)
    {
        for (std::size_t i = 0u; i < m_LowerTriangle.size(); ++i)
        {
            m_LowerTriangle[i] = -m_LowerTriangle[i];
        }
    }

    //! Matrix subtraction.
    void minusEquals(const SSymmetricMatrix &rhs)
    {
        for (std::size_t i = 0u; i < m_LowerTriangle.size(); ++i)
        {
            m_LowerTriangle[i] -= rhs.m_LowerTriangle[i];
        }
    }

    //! Matrix addition.
    void plusEquals(const SSymmetricMatrix &rhs)
    {
        for (std::size_t i = 0u; i < m_LowerTriangle.size(); ++i)
        {
            m_LowerTriangle[i] += rhs.m_LowerTriangle[i];
        }
    }

    //! Component-wise multiplication.
    void multiplyEquals(const SSymmetricMatrix &rhs)
    {
        for (std::size_t i = 0u; i < m_LowerTriangle.size(); ++i)
        {
            m_LowerTriangle[i] *= rhs.m_LowerTriangle[i];
        }
    }

    //! Scalar multiplication.
    void multiplyEquals(Type scale)
    {
        for (std::size_t i = 0u; i < m_LowerTriangle.size(); ++i)
        {
            m_LowerTriangle[i] *= scale;
        }
    }

    //! Scalar division.
    void divideEquals(Type scale)
    {
        for (std::size_t i = 0u; i < m_LowerTriangle.size(); ++i)
        {
            m_LowerTriangle[i] /= scale;
        }
    }

    //! Check if two matrices are identically equal.
    bool equal(const SSymmetricMatrix &other) const
    {
        return m_LowerTriangle == other.m_LowerTriangle;
    }

    //! Lexicographical total ordering.
    bool less(const SSymmetricMatrix &rhs) const
    {
        return m_LowerTriangle < rhs.m_LowerTriangle;
    }

    //! Check if this is zero.
    bool isZero(void) const
    {
        for (std::size_t i = 0u; i < m_LowerTriangle.size(); ++i)
        {
            if (m_LowerTriangle[i] != 0.0)
            {
                return false;
            }
        }
        return true;
    }

    //! Get the matrix diagonal.
    template<typename VECTOR>
    VECTOR diagonal(std::size_t d) const
    {
        VECTOR result(d);
        for (std::size_t i = 0u; i < d; ++i)
        {
            result[i] = this->element(i, i);
        }
        return result;
    }

    //! Get the trace.
    Type trace(std::size_t d) const
    {
        Type result(0);
        for (std::size_t i = 0u; i < d; ++i)
        {
            result += this->element(i, i);
        }
        return result;
    }

    //! The Frobenius norm.
    double frobenius(std::size_t d) const
    {
        double result = 0.0;
        for (std::size_t i = 0u, i_ = 0u; i < d; ++i, ++i_)
        {
            for (std::size_t j = 0u; j < i; ++j, ++i_)
            {
                result += 2.0 * m_LowerTriangle[i_] * m_LowerTriangle[i_];
            }
            result += m_LowerTriangle[i_] * m_LowerTriangle[i_];
        }
        return ::sqrt(result);
    }

    //! Convert to an Eigen symmetric matrix representation.
    template<typename EIGENMATRIX>
    inline EIGENMATRIX &toEigen(std::size_t d, EIGENMATRIX &result) const
    {
        for (std::size_t i = 0u, i_ = 0u; i < d; ++i)
        {
            for (std::size_t j = 0u; j <= i; ++j, ++i_)
            {
                result(i,j) = result(j,i) = m_LowerTriangle[i_];
            }
        }
        return result;
    }

    //! Get a checksum of the elements of this matrix.
    uint64_t checksum(void) const
    {
        uint64_t result = 0u;
        for (std::size_t i = 0u; i < m_LowerTriangle.size(); ++i)
        {
            result = core::CHashing::hashCombine(
                         result,
                         static_cast<uint64_t>(m_LowerTriangle[i]));
        }
        return result;
    }

    STORAGE m_LowerTriangle;
};

} // linear_algebra_detail::


// ******************** EIGEN MATRIX AND VECTOR TYPES *********************


//! \brief Eigen matrix typedef.
//!
//! DESCRIPTION:\n
//! Instantiating many different sizes of Eigen::Matrix really hurts our
//! compile times and executable sizes with debug symbols. The idea of
//! this class is to limit the maximum size of N for which we instantiate
//! different versions. Also, Eigen matrices are always used for calculation
//! for which we want to use double precision.
template<std::size_t N>
struct SEigenMatrixNxN
{
    typedef Eigen::Matrix<double, Eigen::Dynamic, Eigen::Dynamic> Type;
};
//! \brief Use stack matrix for size 2.
template<>
struct SEigenMatrixNxN<2>
{
    typedef Eigen::Matrix<double, 2, 2> Type;
};
//! \brief Use stack matrix for size 3.
template<>
struct SEigenMatrixNxN<3>
{
    typedef Eigen::Matrix<double, 3, 3> Type;
};
//! \brief Use stack matrix for size 4.
template<>
struct SEigenMatrixNxN<4>
{
    typedef Eigen::Matrix<double, 4, 4> Type;
};
//! \brief Eigen matrix type for CSymmetricMatrix.
struct SEigenMatrix
{
    typedef Eigen::Matrix<double, Eigen::Dynamic, Eigen::Dynamic> Type;
};

//! \brief Eigen vector typedef.
//!
//! DESCRIPTION:\n
//! See SEigenMatrix.
template<std::size_t N>
struct SEigenVectorNx1
{
    typedef Eigen::Matrix<double, Eigen::Dynamic, 1> Type;
};
//! \brief Use stack vector for size 2.
template<>
struct SEigenVectorNx1<2>
{
    typedef Eigen::Matrix<double, 2, 1> Type;
};
//! \brief Use stack vector for size 3.
template<>
struct SEigenVectorNx1<3>
{
    typedef Eigen::Matrix<double, 3, 1> Type;
};
//! \brief Use stack vector for size 4.
template<>
struct SEigenVectorNx1<4>
{
    typedef Eigen::Matrix<double, 4, 1> Type;
};
//! \brief Eigen vector type for CVector.
class SEigenVector
{
    public:
        typedef Eigen::Matrix<double, Eigen::Dynamic, 1> Type;
};


// ************************ STACK SYMMETRIC MATRIX ************************

//! \brief A stack based lightweight dense symmetric matrix class.
//!
//! DESCRIPTION:\n
//! This implements a stack based mathematical symmetric matrix object.
//! The idea is to provide a few simple to implement utility functions,
//! however it is primarily intended for storage and is not an alternative
//! to a good linear analysis package implementation. In fact, all utilities
//! for doing any serious linear algebra should convert this to the Eigen
//! library self adjoint representation, an implicit conversion operator
//! for doing this has been supplied. Commonly used operations on matrices
//! for example computing the inverse quadratic product or determinant
//! should be added to this header.
//!
//! IMPLEMENTATION DECISIONS:\n
//! This uses the best possible encoding for space, i.e. packed into a
//! N * (N+1) / 2 length array. This is not the best representation to use
//! for speed as it cuts down on vectorization opportunities. The Eigen
//! library does not support a packed representation for exactly this
//! reason. Our requirements are somewhat different, i.e. we potentially
//! want to store a lot of small matrices with lowest possible space
//! overhead.
//!
//! This also provides a convenience constructor to initialize to a
//! multiple of the ones matrix. Any bounds checking in matrix matrix and
//! matrix vector operations is compile time since the size is a template
//! parameter. The floating point type is templated so that one can use
//! float when space really at a premium.
//!
//! \tparam T The floating point type.
//! \tparam N The matrix dimension.
template<typename T, std::size_t N>
class CSymmetricMatrixNxN : private boost::equality_comparable< CSymmetricMatrixNxN<T, N>,
                                    boost::partially_ordered< CSymmetricMatrixNxN<T, N>,
                                    boost::addable< CSymmetricMatrixNxN<T, N>,
                                    boost::subtractable< CSymmetricMatrixNxN<T, N>,
                                    boost::multipliable< CSymmetricMatrixNxN<T, N>,
                                    boost::multipliable2< CSymmetricMatrixNxN<T, N>, T,
                                    boost::dividable2< CSymmetricMatrixNxN<T, N>, T > > > > > > >,
                            private linear_algebra_detail::SSymmetricMatrix<boost::array<T, N * (N + 1) / 2> >,
                            private linear_algebra_detail::CBoundsCheck<N>::InRange
{
    private:
        typedef linear_algebra_detail::SSymmetricMatrix<boost::array<T, N * (N + 1) / 2> > TBase;
        template<typename U, std::size_t> friend class CSymmetricMatrixNxN;

    public:
        typedef T TArray[N][N];
        typedef std::vector<T> TVec;
        typedef std::vector<TVec> TVecVec;
        typedef typename boost::array<T, N * (N + 1) / 2>::const_iterator TConstIterator;

        //! \brief Computes a hash of the elements of a matrix.
        class CHash
        {
            public:
                std::size_t operator()(const CSymmetricMatrixNxN<T, N> &m) const
                {
                    return static_cast<std::size_t>(boost::unwrap_ref(m).checksum());
                }
        };

        //! \brief Compares two matrices for equality.
        class CEqual
        {
            public:
                bool operator()(const CSymmetricMatrixNxN<T, N> &lhs,
                                const CSymmetricMatrixNxN<T, N> &rhs) const
                {
                    return boost::unwrap_ref(lhs) == boost::unwrap_ref(rhs);
                }
        };

    public:
        //! See core::CMemory.
        static bool dynamicSizeAlwaysZero(void)
        {
            return core::memory_detail::SDynamicSizeAlwaysZero<T>::value();
        }

    public:
        //! Set to multiple of ones matrix.
        explicit CSymmetricMatrixNxN(T v = T(0))
        {
            std::fill_n(&TBase::m_LowerTriangle[0], N * (N + 1) / 2, v);
        }

        //! Construct from C-style array of arrays.
        explicit CSymmetricMatrixNxN(const TArray &m)
        {
            for (std::size_t i = 0u, i_ = 0u; i < N; ++i)
            {
                for (std::size_t j = 0u; j <= i; ++j, ++i_)
                {
                    TBase::m_LowerTriangle[i_] = m[i][j];
                }
            }
        }

        //! Construct from a vector of vectors.
        explicit CSymmetricMatrixNxN(const TVecVec &m)
        {
            for (std::size_t i = 0u, i_ = 0u; i < N; ++i)
            {
                for (std::size_t j = 0u; j <= i; ++j, ++i_)
                {
                    TBase::m_LowerTriangle[i_] = m[i][j];
                }
            }
        }

        //! Construct from a small vector of small vectors.
        template<std::size_t M>
        explicit CSymmetricMatrixNxN(const core::CSmallVector<core::CSmallVector<T, M>, M> &m)
        {
            for (std::size_t i = 0u, i_ = 0u; i < N; ++i)
            {
                for (std::size_t j = 0u; j <= i; ++j, ++i_)
                {
                    TBase::m_LowerTriangle[i_] = m[i][j];
                }
            }
        }

        //! Construct from a forward iterator.
        //!
        //! \warning The user must ensure that the range iterated has
        //! at least N (N+1) / 2 items.
        template<typename ITR>
        CSymmetricMatrixNxN(ITR begin, ITR end)
        {
            for (std::size_t i = 0u;
                 i < N * (N + 1) / 2 && begin != end;
                 ++i, ++begin)
            {
                TBase::m_LowerTriangle[i] = static_cast<T>(*begin);
            }
        }

        explicit CSymmetricMatrixNxN(ESymmetricMatrixType type, const CVectorNx1<T, N> &x);

        //! Implicit construction from a Eigen self adjoint matrix.
        CSymmetricMatrixNxN(const typename SEigenMatrixNxN<N>::Type &m)
        {
            for (std::size_t i = 0u, i_ = 0u; i < N; ++i)
            {
                for (std::size_t j = 0u; j <= i; ++j, ++i_)
                {
                    TBase::m_LowerTriangle[i_] = (m.template selfadjointView<Eigen::Lower>())(i, j);
                }
            }
        }

        //! Copy construction if the underlying type is implicitly
        //! convertible.
        template<typename U>
        CSymmetricMatrixNxN(const CSymmetricMatrixNxN<U, N> &other)
        {
            this->operator=(other);
        }

        //! Assignment if the underlying type is implicitly convertible.
        template<typename U>
        const CSymmetricMatrixNxN &operator=(const CSymmetricMatrixNxN<U, N> &other)
        {
            this->assign(other.base());
            return *this;
        }

        //! \name Persistence
        //@{
        //! Create from a delimited string.
        bool fromDelimited(const std::string &str)
        {
            return this->TBase::fromDelimited(str);
        }

        //! Convert to a delimited string.
        std::string toDelimited(void) const
        {
            return this->TBase::toDelimited();
        }
        //@}

        //! Get the number of rows.
        std::size_t rows(void) const { return N; }

        //! Get the number of columns.
        std::size_t columns(void) const { return N; }

        //! Get the i,j 'th component (no bounds checking).
        inline T operator()(std::size_t i, std::size_t j) const
        {
            return this->element(i, j);
        }

        //! Get the i,j 'th component (no bounds checking).
        inline T &operator()(std::size_t i, std::size_t j)
        {
            return this->element(i, j);
        }

        //! Get an iterator over the elements.
        TConstIterator begin(void) const { return TBase::m_LowerTriangle.begin(); }

        //! Get an iterator to the end of the elements.
        TConstIterator end(void) const { return TBase::m_LowerTriangle.end(); }

        //! Component-wise negation.
        CSymmetricMatrixNxN operator-(void) const
        {
            CSymmetricMatrixNxN result(*this);
            result.negative();
            return result;
        }

        //! Matrix subtraction.
        const CSymmetricMatrixNxN &operator-=(const CSymmetricMatrixNxN &rhs)
        {
            this->minusEquals(rhs.base());
            return *this;
        }

        //! Matrix addition.
        const CSymmetricMatrixNxN &operator+=(const CSymmetricMatrixNxN &rhs)
        {
            this->plusEquals(rhs.base());
            return *this;
        }

        //! Component-wise multiplication.
        //!
        //! \note This is handy in some cases and since symmetric matrices
        //! are not closed under regular matrix multiplication we use
        //! multiplication operator for implementing the Hadamard product.
        const CSymmetricMatrixNxN &operator*=(const CSymmetricMatrixNxN &rhs)
        {
            this->multiplyEquals(rhs);
            return *this;
        }

        //! Scalar multiplication.
        const CSymmetricMatrixNxN &operator*=(T scale)
        {
            this->multiplyEquals(scale);
            return *this;
        }

        //! Scalar division.
        const CSymmetricMatrixNxN &operator/=(T scale)
        {
            this->divideEquals(scale);
            return *this;
        }

        // Matrix multiplication doesn't necessarily produce a symmetric
        // matrix because matrix multiplication is non-commutative.
        // Matrix division requires computing the inverse and is not
        // supported.

        //! Check if two matrices are identically equal.
        bool operator==(const CSymmetricMatrixNxN &other) const
        {
            return this->equal(other.base());
        }

        //! Lexicographical total ordering.
        bool operator<(const CSymmetricMatrixNxN &rhs) const
        {
            return this->less(rhs.base());
        }

        //! Check if this is zero.
        bool isZero(void) const
        {
            return this->TBase::isZero();
        }

        //! Get the matrix diagonal.
        template<typename VECTOR>
        VECTOR diagonal(void) const
        {
            return this->TBase::template diagonal<VECTOR>(N);
        }

        //! Get the trace.
        T trace(void) const
        {
            return this->TBase::trace(N);
        }

        //! Get the Frobenius norm.
        double frobenius(void) const
        {
            return this->TBase::frobenius(N);
        }

        //! Convert to a vector of vectors.
        template<typename VECTOR_OF_VECTORS>
        inline VECTOR_OF_VECTORS toVectors(void) const
        {
            VECTOR_OF_VECTORS result(N);
            for (std::size_t i = 0u; i < N; ++i)
            {
                result[i].resize(N);
            }
            for (std::size_t i = 0u; i < N; ++i)
            {
                result[i][i] = this->operator()(i, i);
                for (std::size_t j = 0u; j < i; ++j)
                {
                    result[i][j] = result[j][i] = this->operator()(i, j);
                }
            }
            return result;
        }

        //! Convert to an Eigen symmetric matrix representation.
        //!
        //! \note The copy should be avoided by RVO.
        inline typename SEigenMatrixNxN<N>::Type toEigen(void) const
        {
            typename SEigenMatrixNxN<N>::Type result(N, N);
            return this->TBase::toEigen(N, result);
        }

        //! Convert to an Eigen heap matrix.
        //!
        //! \note The copy should be avoided by RVO.
        inline Eigen::Matrix<double, Eigen::Dynamic, Eigen::Dynamic> toEigenDynamic(void) const
        {
            Eigen::Matrix<double, Eigen::Dynamic, Eigen::Dynamic> result(N, N);
            return this->TBase::toEigen(N, result);
        }

        //! Get a checksum for the matrix.
        uint64_t checksum(void) const
        {
            return this->TBase::checksum();
        }
};

//! Output for debug.
template<typename T, std::size_t N>
std::ostream &operator<<(std::ostream &o, const CSymmetricMatrixNxN<T, N> &m)
{
    for (std::size_t i = 0u; i < N; ++i)
    {
        o << "\n    ";
        for (std::size_t j = 0u; j < N; ++j)
        {
            std::string element = core::CStringUtils::typeToStringPretty(m(i, j));
            o << element << std::string(15 - element.size(), ' ');
        }
    }
    return o;
}


// ************************ HEAP SYMMETRIC MATRIX ************************

//! \brief A heap based lightweight dense symmetric matrix class.
//!
//! DESCRIPTION:\n
//! This implements a heap based mathematical symmetric matrix object.
//! The idea is to provide a few simple to implement utility functions,
//! however it is primarily intended for storage and is not an alternative
//! to a good linear analysis package implementation. In fact, all utilities
//! for doing any serious linear algebra should convert this to the Eigen
//! library self adjoint representation, an explicit conversion operator
//! for doing this has been supplied. Commonly used operations on matrices
//! for example computing the inverse quadratic product or determinant
//! should be added to this header.
//!
//! IMPLEMENTATION DECISIONS:\n
//! This uses the best possible encoding for space, i.e. packed into a
//! D * (D+1) / 2 length vector where D is the dimension. This is not the
//! best representation to use for speed as it cuts down on vectorization
//! opportunities. The Eigen library does not support a packed representation
//! for exactly this reason. Our requirements are somewhat different, i.e.
//! we potentially want to store a lot of small(ish) matrices with lowest
//! possible space overhead.
//!
//! This also provides a convenience constructor to initialize to a
//! multiple of the ones matrix. There is no bounds checking in matrix
//! matrix and matrix vector operations for speed. The floating point
//! type is templated so that one can use float when space really at a
//! premium.
//!
//! \tparam T The floating point type.
template<typename T>
class CSymmetricMatrix : private boost::equality_comparable< CSymmetricMatrix<T>,
                                 boost::partially_ordered< CSymmetricMatrix<T>,
                                 boost::addable< CSymmetricMatrix<T>,
                                 boost::subtractable< CSymmetricMatrix<T>,
                                 boost::multipliable< CSymmetricMatrix<T>,
                                 boost::multipliable2< CSymmetricMatrix<T>, T,
                                 boost::dividable2< CSymmetricMatrix<T>, T > > > > > > >,
                         private linear_algebra_detail::SSymmetricMatrix<std::vector<T> >
{
    private:
        typedef linear_algebra_detail::SSymmetricMatrix<std::vector<T> > TBase;
        template<typename U> friend class CSymmetricMatrix;

    public:
        typedef std::vector<std::vector<T> > TArray;
        typedef typename std::vector<T>::const_iterator TConstIterator;

        //! \brief Computes a hash of the elements of a matrix.
        class CHash
        {
            public:
                std::size_t operator()(const CSymmetricMatrix<T> &m) const
                {
                    return static_cast<std::size_t>(boost::unwrap_ref(m).checksum());
                }
        };

        //! \brief Compares two matrices for equality.
        class CEqual
        {
            public:
                bool operator()(const CSymmetricMatrix<T> &lhs,
                                const CSymmetricMatrix<T> &rhs) const
                {
                    return boost::unwrap_ref(lhs) == boost::unwrap_ref(rhs);
                }
        };

    public:
        //! Set to multiple of ones matrix.
        explicit CSymmetricMatrix(std::size_t d = 0u, T v = T(0)) : m_D(d)
        {
            if (d > 0)
            {
                TBase::m_LowerTriangle.resize(d * (d + 1) / 2, v);
            }
        }

        //! Construct from C-style array of arrays.
        explicit CSymmetricMatrix(const TArray &m) : m_D(m.size())
        {
            TBase::m_LowerTriangle.resize(m_D * (m_D + 1) / 2);
            for (std::size_t i = 0u, i_ = 0u; i < m_D; ++i)
            {
                for (std::size_t j = 0u; j <= i; ++j, ++i_)
                {
                    TBase::m_LowerTriangle[i_] = m[i][j];
                }
            }
        }

        //! Construct from a small vector of small vectors.
        template<std::size_t M>
        explicit CSymmetricMatrix(const core::CSmallVector<core::CSmallVector<T, M>, M> &m) : m_D(m.size())
        {
            TBase::m_LowerTriangle.resize(m_D * (m_D + 1) / 2);
            for (std::size_t i = 0u, i_ = 0u; i < m_D; ++i)
            {
                for (std::size_t j = 0u; j <= i; ++j, ++i_)
                {
                    TBase::m_LowerTriangle[i_] = m[i][j];
                }
            }
        }

        //! Construct from a forward iterator.
        //!
        //! \warning The user must ensure that the range iterated has
        //! at least N (N+1) / 2 items.
        template<typename ITR>
        CSymmetricMatrix(ITR begin, ITR end)
        {
            m_D = this->dimension(std::distance(begin, end));
            TBase::m_LowerTriangle.resize(m_D * (m_D + 1) / 2);
            for (std::size_t i = 0u;
                 i < m_D * (m_D + 1) / 2 && begin != end;
                 ++i, ++begin)
            {
                TBase::m_LowerTriangle[i] = static_cast<T>(*begin);
            }
        }

        explicit CSymmetricMatrix(ESymmetricMatrixType type, const CVector<T> &x);

        //! Implicit construction from a Eigen self adjoint matrix.
        CSymmetricMatrix(const SEigenMatrix::Type &m)
        {
            m_D = m.rows();
            TBase::m_LowerTriangle.resize(m_D * (m_D + 1) / 2);
            for (std::size_t i = 0u, i_ = 0u; i < m_D; ++i)
            {
                for (std::size_t j = 0u; j <= i; ++j, ++i_)
                {
                    TBase::m_LowerTriangle[i_] = (m.template selfadjointView<Eigen::Lower>())(i, j);
                }
            }
        }

        //! Copy construction if the underlying type is implicitly
        //! convertible.
        template<typename U>
        CSymmetricMatrix(const CSymmetricMatrix<U> &other) : m_D(other.m_D)
        {
            this->operator=(other);
        }

        //! Assignment if the underlying type is implicitly convertible.
        template<typename U>
        const CSymmetricMatrix &operator=(const CSymmetricMatrix<U> &other)
        {
            m_D = other.m_D;
            TBase::m_LowerTriangle.resize(m_D * (m_D + 1) / 2);
            this->assign(other.base());
            return *this;
        }

        //! Efficiently swap the contents of two matrices.
        void swap(CSymmetricMatrix &other)
        {
            std::swap(m_D, other.m_D);
            TBase::m_LowerTriangle.swap(other.TBase::m_LowerTriangle);
        }

        //! \name Persistence
        //@{
        //! Create from a delimited string.
        bool fromDelimited(const std::string &str)
        {
            if (this->TBase::fromDelimited(str))
            {
                m_D = this->dimension(TBase::m_X.size());
                return true;
            }
            return false;
        }

        //! Convert to a delimited string.
        std::string toDelimited(void) const
        {
            return this->TBase::toDelimited();
        }
        //@}

        //! Get the number of rows.
        std::size_t rows(void) const { return m_D; }

        //! Get the number of columns.
        std::size_t columns(void) const { return m_D; }

        //! Get the i,j 'th component (no bounds checking).
        inline T operator()(std::size_t i, std::size_t j) const
        {
            return this->element(i, j);
        }

        //! Get the i,j 'th component (no bounds checking).
        inline T &operator()(std::size_t i, std::size_t j)
        {
            return this->element(i, j);
        }

        //! Get an iterator over the elements.
        TConstIterator begin(void) const { return TBase::m_X.begin(); }

        //! Get an iterator to the end of the elements.
        TConstIterator end(void) const { return TBase::m_X.end(); }

        //! Component-wise negation.
        CSymmetricMatrix operator-(void) const
        {
            CSymmetricMatrix result(*this);
            result.negative();
            return result;
        }

        //! Matrix subtraction.
        const CSymmetricMatrix &operator-=(const CSymmetricMatrix &rhs)
        {
            this->minusEquals(rhs.base());
            return *this;
        }

        //! Matrix addition.
        const CSymmetricMatrix &operator+=(const CSymmetricMatrix &rhs)
        {
            this->plusEquals(rhs.base());
            return *this;
        }

        //! Component-wise multiplication.
        //!
        //! \note This is handy in some cases and since symmetric matrices
        //! are not closed under regular matrix multiplication we use
        //! multiplication operator for implementing the Hadamard product.
        const CSymmetricMatrix &operator*=(const CSymmetricMatrix &rhs)
        {
            this->multiplyEquals(rhs);
            return *this;
        }

        //! Scalar multiplication.
        const CSymmetricMatrix &operator*=(T scale)
        {
            this->multiplyEquals(scale);
            return *this;
        }

        //! Scalar division.
        const CSymmetricMatrix &operator/=(T scale)
        {
            this->divideEquals(scale);
            return *this;
        }

        // Matrix multiplication doesn't necessarily produce a symmetric
        // matrix because matrix multiplication is non-commutative.
        // Matrix division requires computing the inverse and is not
        // supported.

        //! Check if two matrices are identically equal.
        bool operator==(const CSymmetricMatrix &other) const
        {
            return this->equal(other.base());
        }

        //! Lexicographical total ordering.
        bool operator<(const CSymmetricMatrix &rhs) const
        {
            return this->less(rhs.base());
        }

        //! Check if this is zero.
        bool isZero(void) const
        {
            return this->TBase::isZero();
        }

        //! Get the matrix diagonal.
        template<typename VECTOR>
        VECTOR diagonal(void) const
        {
            return this->TBase::template diagonal<VECTOR>(m_D);
        }

        //! Get the trace.
        T trace(void) const
        {
            return this->TBase::trace(m_D);
        }

        //! The Frobenius norm.
        double frobenius(void) const
        {
            return this->TBase::frobenius(m_D);
        }

        //! Convert to a vector of vectors.
        template<typename VECTOR_OF_VECTORS>
        inline VECTOR_OF_VECTORS toVectors(void) const
        {
            VECTOR_OF_VECTORS result(m_D);
            for (std::size_t i = 0u; i < m_D; ++i)
            {
                result[i].resize(m_D);
            }
            for (std::size_t i = 0u; i < m_D; ++i)
            {
                result[i][i] = this->operator()(i, i);
                for (std::size_t j = 0u; j < i; ++j)
                {
                    result[i][j] = result[j][i] = this->operator()(i, j);
                }
            }
            return result;
        }

        //! Convert to an Eigen symmetric matrix representation.
        //!
        //! \note The copy should be avoided by RVO.
        inline SEigenMatrix::Type toEigen(void) const
        {
            SEigenMatrix::Type result(m_D, m_D);
            return this->TBase::toEigen(m_D, result);
        }

        //! Get a checksum for the matrix.
        uint64_t checksum(void) const
        {
            return core::CHashing::hashCombine(this->TBase::checksum(),
                                               static_cast<uint64_t>(m_D));
        }

    private:
        //! Compute the dimension from the number of elements.
        std::size_t dimension(std::size_t n) const
        {
            return static_cast<std::size_t>(
                       (::sqrt(8.0 * static_cast<double>(n) + 1.0) - 1.0) / 2.0 + 0.5);
        }

    private:
        //! The rows (and columns) of this matrix.
        std::size_t m_D;
};

//! Output for debug.
template<typename T>
std::ostream &operator<<(std::ostream &o, const CSymmetricMatrix<T> &m)
{
    for (std::size_t i = 0u; i < m.rows(); ++i)
    {
        o << "\n    ";
        for (std::size_t j = 0u; j < m.columns(); ++j)
        {
            std::string element = core::CStringUtils::typeToStringPretty(m(i, j));
            o << element << std::string(15 - element.size(), ' ');
        }
    }
    return o;
}


namespace linear_algebra_detail
{

//! \brief Common vector functionality for variable storage type.
template<typename STORAGE>
struct SVector
{
    typedef typename STORAGE::value_type Type;

    //! Get read only reference.
    inline const SVector &base(void) const { return *this; }

    //! Get writable reference.
    inline SVector &base(void) { return *this; }

    //! Set this vector equal to \p other.
    template<typename OTHER_STORAGE>
    void assign(const SVector<OTHER_STORAGE> &other)
    {
        std::copy(other.m_X.begin(), other.m_X.end(), m_X.begin());
    }

    //! Create from delimited values.
    bool fromDelimited(const std::string &str)
    {
        return core::CPersistUtils::fromString(str, SFromString(), m_X, CLinearAlgebra::DELIMITER);
    }

    //! Convert to a delimited string.
    std::string toDelimited(void) const
    {
        return core::CPersistUtils::toString(m_X, SToString(), CLinearAlgebra::DELIMITER);
    }

    //! Component-wise negative.
    void negative(void)
    {
        for (std::size_t i = 0u; i < m_X.size(); ++i)
        {
            m_X[i] = -m_X[i];
        }
    }

    //! Vector subtraction.
    void minusEquals(const SVector &lhs)
    {
        for (std::size_t i = 0u; i < m_X.size(); ++i)
        {
            m_X[i] -= lhs.m_X[i];
        }
    }

    //! Vector addition.
    void plusEquals(const SVector &lhs)
    {
        for (std::size_t i = 0u; i < m_X.size(); ++i)
        {
            m_X[i] += lhs.m_X[i];
        }
    }

    //! Component-wise multiplication.
    void multiplyEquals(const SVector &scale)
    {
        for (std::size_t i = 0u; i < m_X.size(); ++i)
        {
            m_X[i] *= scale.m_X[i];
        }
    }

    //! Scalar multiplication.
    void multiplyEquals(Type scale)
    {
        for (std::size_t i = 0u; i < m_X.size(); ++i)
        {
            m_X[i] *= scale;
        }
    }

    //! Component-wise division.
    void divideEquals(const SVector &scale)
    {
        for (std::size_t i = 0u; i < m_X.size(); ++i)
        {
            m_X[i] /= scale.m_X[i];
        }
    }

    //! Scalar division.
    void divideEquals(Type scale)
    {
        for (std::size_t i = 0u; i < m_X.size(); ++i)
        {
            m_X[i] /= scale;
        }
    }

    //! Compare this and \p other for equality.
    bool equal(const SVector &other) const { return m_X == other.m_X; }

    //! Lexicographical total ordering.
    bool less(const SVector &rhs) const { return m_X < rhs.m_X; }

    //! Check if this is zero.
    bool isZero(void) const
    {
        for (std::size_t i = 0u; i < m_X.size(); ++i)
        {
            if (m_X[i] != 0.0)
            {
                return false;
            }
        }
        return true;
    }

    //! Inner product.
    double inner(const SVector &covector) const
    {
        double result = 0.0;
        for (std::size_t i = 0u; i < m_X.size(); ++i)
        {
            result += m_X[i] * covector.m_X[i];
        }
        return result;
    }

    //! Inner product.
    template<typename EIGENVECTOR>
    double inner(const EIGENVECTOR &covector) const
    {
        double result = 0.0;
        for (std::size_t i = 0u; i < m_X.size(); ++i)
        {
            result += m_X[i] * covector(i);
        }
        return result;
    }

    //! The L1 norm of the vector.
    double L1(void) const
    {
        double result = 0.0;
        for (std::size_t i = 0u; i < m_X.size(); ++i)
        {
            result += ::fabs(static_cast<double>(m_X[i]));
        }
        return result;
    }

    //! Convert to an Eigen vector.
    template<typename EIGENVECTOR>
    inline EIGENVECTOR &toEigen(EIGENVECTOR &result) const
    {
        for (std::size_t i = 0u; i < m_X.size(); ++i)
        {
            result(i) = m_X[i];
        }
        return result;
    }

    //! Get a checksum of the components of this vector.
    uint64_t checksum(void) const
    {
        uint64_t result = static_cast<uint64_t>(m_X[0]);
        for (std::size_t i = 1u; i < m_X.size(); ++i)
        {
            result = core::CHashing::hashCombine(
                         result,
                         static_cast<uint64_t>(m_X[i]));
        }
        return result;
    }

    //! The components
    STORAGE m_X;
};

struct VectorTag;
struct MatrixTag;
struct VectorVectorTag;
struct MatrixMatrixTag;
struct VectorScalarTag;
struct MatrixScalarTag;
struct ScalarVectorTag;
struct ScalarMatrixTag;

template<typename TAG> struct SSqrt {};
//! Component-wise sqrt for a vector.
template<>
struct SSqrt<VectorTag>
{
    template<typename VECTOR>
    static void calculate(std::size_t d, VECTOR &result)
    {
        for (std::size_t i = 0u; i < d; ++i)
        {
            result(i) = ::sqrt(result(i));
        }
    }
};
//! Element-wise sqrt for a symmetric matrix.
template<>
struct SSqrt<MatrixTag>
{
    template<typename MATRIX>
    static void calculate(std::size_t d, MATRIX &result)
    {
        for (std::size_t i = 0u; i < d; ++i)
        {
            for (std::size_t j = 0u; j <= i; ++j)
            {
                result(i, j) = ::sqrt(result(i, j));
            }
        }
    }
};

template<typename TAG> struct SMin {};
//! Component-wise minimum for a vector.
template<>
struct SMin<VectorVectorTag>
{
    template<typename VECTOR>
    static void calculate(std::size_t d, const VECTOR &lhs, VECTOR &rhs)
    {
        for (std::size_t i = 0u; i < d; ++i)
        {
            rhs(i) = std::min(lhs(i), rhs(i));
        }
    }
};
//! Component-wise minimum for a vector.
template<>
struct SMin<VectorScalarTag>
{
    template<typename VECTOR, typename T>
    static void calculate(std::size_t d, VECTOR &lhs, const T &rhs)
    {
        for (std::size_t i = 0u; i < d; ++i)
        {
            lhs(i) = std::min(lhs(i), rhs);
        }
    }
};
//! Component-wise minimum for a vector.
template<>
struct SMin<ScalarVectorTag>
{
    template<typename T, typename VECTOR>
    static void calculate(std::size_t d, const T &lhs, VECTOR &rhs)
    {
        for (std::size_t i = 0u; i < d; ++i)
        {
            rhs(i) = std::min(rhs(i), lhs);
        }
    }
};
//! Element-wise minimum for a symmetric matrix.
template<>
struct SMin<MatrixMatrixTag>
{
    template<typename MATRIX>
    static void calculate(std::size_t d, const MATRIX &lhs, MATRIX &rhs)
    {
        for (std::size_t i = 0u; i < d; ++i)
        {
            for (std::size_t j = 0u; j <= i; ++j)
            {
                rhs(i, j) = std::min(lhs(i, j), rhs(i, j));
            }
        }
    }
};
//! Element-wise minimum for a symmetric matrix.
template<>
struct SMin<MatrixScalarTag>
{
    template<typename MATRIX, typename T>
    static void calculate(std::size_t d, MATRIX &lhs, const T &rhs)
    {
        for (std::size_t i = 0u; i < d; ++i)
        {
            for (std::size_t j = 0u; j <= i; ++j)
            {
                lhs(i, j) = std::min(lhs(i, j), rhs);
            }
        }
    }
};
//! Element-wise minimum for a symmetric matrix.
template<>
struct SMin<ScalarMatrixTag>
{
    template<typename T, typename MATRIX>
    static void calculate(std::size_t d, const T &lhs, MATRIX &rhs)
    {
        for (std::size_t i = 0u; i < d; ++i)
        {
            for (std::size_t j = 0u; j <= i; ++j)
            {
                rhs(i, j) = std::min(lhs, rhs(i, j));
            }
        }
    }
};

template<typename TAG> struct SMax {};
//! Component-wise maximum for a vector.
template<>
struct SMax<VectorVectorTag>
{
    template<typename VECTOR>
    static void calculate(std::size_t d, const VECTOR &lhs, VECTOR &rhs)
    {
        for (std::size_t i = 0u; i < d; ++i)
        {
            rhs(i) = std::max(lhs(i), rhs(i));
        }
    }
};
//! Component-wise maximum for a vector.
template<>
struct SMax<VectorScalarTag>
{
    template<typename VECTOR, typename T>
    static void calculate(std::size_t d, VECTOR &lhs, const T &rhs)
    {
        for (std::size_t i = 0u; i < d; ++i)
        {
            lhs(i) = std::max(lhs(i), rhs);
        }
    }
};
//! Component-wise maximum for a vector.
template<>
struct SMax<ScalarVectorTag>
{
    template<typename T, typename VECTOR>
    static void calculate(std::size_t d, const T &lhs, VECTOR &rhs)
    {
        for (std::size_t i = 0u; i < d; ++i)
        {
            rhs(i) = std::max(rhs(i), lhs);
        }
    }
};
//! Element-wise maximum for a symmetric matrix.
template<>
struct SMax<MatrixMatrixTag>
{
    template<typename MATRIX>
    static void calculate(std::size_t d, const MATRIX &lhs, MATRIX &rhs)
    {
        for (std::size_t i = 0u; i < d; ++i)
        {
            for (std::size_t j = 0u; j <= i; ++j)
            {
                rhs(i, j) = std::max(lhs(i, j), rhs(i, j));
            }
        }
    }
};
//! Element-wise maximum for a symmetric matrix.
template<>
struct SMax<MatrixScalarTag>
{
    template<typename MATRIX, typename T>
    static void calculate(std::size_t d, MATRIX &lhs, const T &rhs)
    {
        for (std::size_t i = 0u; i < d; ++i)
        {
            for (std::size_t j = 0u; j <= i; ++j)
            {
                lhs(i, j) = std::max(lhs(i, j), rhs);
            }
        }
    }
};
//! Element-wise maximum for a symmetric matrix.
template<>
struct SMax<ScalarMatrixTag>
{
    template<typename T, typename MATRIX>
    static void calculate(std::size_t d, const T &lhs, MATRIX &rhs)
    {
        for (std::size_t i = 0u; i < d; ++i)
        {
            for (std::size_t j = 0u; j <= i; ++j)
            {
                rhs(i, j) = std::max(lhs, rhs(i, j));
            }
        }
    }
};

template<typename TAG> struct SFabs {};
//! Component-wise fabs for a vector.
template<>
struct SFabs<VectorTag>
{
    template<typename VECTOR>
    static void calculate(std::size_t d, VECTOR &result)
    {
        for (std::size_t i = 0u; i < d; ++i)
        {
            result(i) = ::fabs(result(i));
        }
    }
};
//! Element-wise fabs for a symmetric matrix.
template<>
struct SFabs<MatrixTag>
{
    template<typename MATRIX>
    static void calculate(std::size_t d, MATRIX &result)
    {
        for (std::size_t i = 0u; i < d; ++i)
        {
            for (std::size_t j = 0u; j <= i; ++j)
            {
                result(i, j) = ::fabs(result(i, j));
            }
        }
    }
};

} // linear_algebra_detail::


// ************************ STACK VECTOR ************************

//! \brief A stack based lightweight dense vector class.
//!
//! DESCRIPTION:\n
//! This implements a stack based mathematical vector object. The idea
//! is to provide utility functions and operators which mean that it
//! works with other prelert::maths:: classes, such as the symmetric
//! matrix object and the sample (co)variance accumulators, and keep the
//! memory footprint as small as possible. This is not meant to be an
//! alternative to a good linear analysis package implementation. For
//! example, if you want to any serious linear algebra use the Eigen
//! library. An implicit conversion operator for doing this has been
//! supplied.
//!
//! IMPLEMENTATION DECISIONS:\n
//! Operators follow the Matlab component-wise convention. This provides
//! a constructor to initialize to a multiple of the 1 vector. Bounds
//! checking for vector vector and matrix vector operations is compile
//! time since the size is a template parameter. The floating point type
//! is templated so that one can use float when space is really at a
//! premium.
//!
//! \tparam T The floating point type.
//! \tparam N The vector dimension.
template<typename T, std::size_t N>
class CVectorNx1 : private boost::equality_comparable< CVectorNx1<T, N>,
                           boost::partially_ordered< CVectorNx1<T, N>,
                           boost::addable< CVectorNx1<T, N>,
                           boost::subtractable< CVectorNx1<T, N>,
                           boost::multipliable< CVectorNx1<T, N>,
                           boost::multipliable2< CVectorNx1<T, N>, T,
                           boost::dividable< CVectorNx1<T, N>,
                           boost::dividable2< CVectorNx1<T, N>, T > > > > > > > >,
                   private linear_algebra_detail::SVector<boost::array<T, N> >,
                   private linear_algebra_detail::CBoundsCheck<N>::InRange
{
    private:
        typedef linear_algebra_detail::SVector<boost::array<T, N> > TBase;
        template<typename U, std::size_t> friend class CVectorNx1;

    public:
        typedef T TArray[N];
        typedef std::vector<T> TVec;
        typedef boost::array<T, N> TBoostArray;
        typedef typename TBoostArray::const_iterator TConstIterator;

    public:
        //! \brief Computes a hash of the components of a vector.
        class CHash
        {
            public:
                std::size_t operator()(const CVectorNx1<T, N> &v) const
                {
                    return static_cast<std::size_t>(boost::unwrap_ref(v).checksum());
                }
        };

        //! \brief Compares two vectors for equality.
        class CEqual
        {
            public:
                bool operator()(const CVectorNx1<T, N> &lhs,
                                const CVectorNx1<T, N> &rhs) const
                {
                    return boost::unwrap_ref(lhs) == boost::unwrap_ref(rhs);
                }
        };

        //! See core::CMemory.
        static bool dynamicSizeAlwaysZero(void)
        {
            return core::memory_detail::SDynamicSizeAlwaysZero<T>::value();
        }

    public:
        //! Set to multiple of ones vector.
        explicit CVectorNx1(T v = T(0))
        {
            std::fill_n(&TBase::m_X[0], N, v);
        }

        //! Construct from a C-style array.
        explicit CVectorNx1(const TArray &v)
        {
            for (std::size_t i = 0u; i < N; ++i)
            {
                TBase::m_X[i] = v[i];
            }
        }

        //! Construct from a boost array.
        explicit CVectorNx1(const boost::array<T, N> &a)
        {
            for (std::size_t i = 0u; i < N; ++i)
            {
                TBase::m_X[i] = a[i];
            }
        }

        //! Construct from a vector.
        explicit CVectorNx1(const TVec &v)
        {
            for (std::size_t i = 0u; i < N; ++i)
            {
                TBase::m_X[i] = v[i];
            }
        }

        //! Construct from a vector.
        template<std::size_t M>
        explicit CVectorNx1(const core::CSmallVector<T, M> &v)
        {
            for (std::size_t i = 0u; i < N; ++i)
            {
                TBase::m_X[i] = v[i];
            }
        }

        //! Construct from a forward iterator.
        //!
        //! \warning The user must ensure that the range iterated has
        //! at least N items.
        template<typename ITR>
        CVectorNx1(ITR begin, ITR end)
        {
            if (std::distance(begin, end) != N)
            {
                LOG_ERROR("Bad range");
                return;
            }
            std::copy(begin, end, &TBase::m_X[0]);
        }

        //! Implicit construction from Eigen vector.
        CVectorNx1(const typename SEigenVectorNx1<N>::Type &v)
        {
            for (std::size_t i = 0u; i < N; ++i)
            {
                TBase::m_X[i] = v(i);
            }
        }

        //! Copy construction if the underlying type is implicitly
        //! convertible.
        template<typename U>
        CVectorNx1(const CVectorNx1<U, N> &other)
        {
            this->operator=(other);
        }

        //! Assignment if the underlying type is implicitly convertible.
        template<typename U>
        const CVectorNx1 &operator=(const CVectorNx1<U, N> &other)
        {
            this->assign(other.base());
            return *this;
        }

        //! \name Persistence
        //@{
        //! Create from a delimited string.
        bool fromDelimited(const std::string &str)
        {
            return this->TBase::fromDelimited(str);
        }

        //! Convert to a delimited string.
        std::string toDelimited(void) const
        {
            return this->TBase::toDelimited();
        }
        //@}

        //! Get the dimension.
        std::size_t dimension(void) const { return N; }

        //! Get the i'th component (no bounds checking).
        inline T operator()(std::size_t i) const
        {
            return TBase::m_X[i];
        }

        //! Get the i'th component (no bounds checking).
        inline T &operator()(std::size_t i)
        {
            return TBase::m_X[i];
        }

        //! Get an iterator over the elements.
        TConstIterator begin(void) const { return TBase::m_X.begin(); }

        //! Get an iterator to the end of the elements.
        TConstIterator end(void) const { return TBase::m_X.end(); }

        //! Component-wise negation.
        CVectorNx1 operator-(void) const
        {
            CVectorNx1 result(*this);
            result.negative();
            return result;
        }

        //! Vector subtraction.
        const CVectorNx1 &operator-=(const CVectorNx1 &lhs)
        {
            this->minusEquals(lhs.base());
            return *this;
        }

        //! Vector addition.
        const CVectorNx1 &operator+=(const CVectorNx1 &lhs)
        {
            this->plusEquals(lhs.base());
            return *this;
        }

        //! Component-wise multiplication.
        const CVectorNx1 &operator*=(const CVectorNx1 &scale)
        {
            this->multiplyEquals(scale.base());
            return *this;
        }

        //! Scalar multiplication.
        const CVectorNx1 &operator*=(T scale)
        {
            this->multiplyEquals(scale);
            return *this;
        }

        //! Component-wise division.
        const CVectorNx1 &operator/=(const CVectorNx1 &scale)
        {
            this->divideEquals(scale.base());
            return *this;
        }

        //! Scalar division.
        const CVectorNx1 &operator/=(T scale)
        {
            this->divideEquals(scale);
            return *this;
        }

        //! Check if two vectors are identically equal.
        bool operator==(const CVectorNx1 &other) const
        {
            return this->equal(other.base());
        }

        //! Lexicographical total ordering.
        bool operator<(const CVectorNx1 &rhs) const
        {
            return this->less(rhs.base());
        }

        //! Check if this is zero.
        bool isZero(void) const
        {
            return this->TBase::isZero();
        }

        //! Inner product.
        double inner(const CVectorNx1 &covector) const
        {
            return this->TBase::inner(covector.base());
        }

        //! Inner product.
        double inner(const typename SEigenVectorNx1<N>::Type &covector) const
        {
            return this->TBase::template inner<typename SEigenVectorNx1<N>::Type>(covector);
        }

        //! Outer product.
        //!
        //! \note The copy should be avoided by RVO.
        CSymmetricMatrixNxN<T, N> outer(void) const
        {
            return CSymmetricMatrixNxN<T, N>(E_OuterProduct, *this);
        }

        //! A diagonal matrix.
        //!
        //! \note The copy should be avoided by RVO.
        CSymmetricMatrixNxN<T, N> diagonal(void) const
        {
            return CSymmetricMatrixNxN<T, N>(E_Diagonal, *this);
        }

        //! L1 norm.
        double L1(void) const
        {
            return this->TBase::L1();
        }

        //! Euclidean norm.
        double euclidean(void) const
        {
            return ::sqrt(this->inner(*this));
        }

        //! Convert to a vector on a different underlying type.
        template<typename U>
        inline CVectorNx1<U, N> to(void) const
        {
            return CVectorNx1<U, N>(*this);
        }

        //! Convert to a vector.
        template<typename VECTOR>
        inline VECTOR toVector(void) const
        {
            return VECTOR(this->begin(), this->end());
        }

        //! Convert to a boost array.
        inline TBoostArray toBoostArray(void) const
        {
            return TBase::m_X;
        }

        //! Convert to an Eigen vector.
        //!
        //! \note The copy should be avoided by RVO.
        inline typename SEigenVectorNx1<N>::Type toEigen(void) const
        {
            typename SEigenVectorNx1<N>::Type result(N);
            return this->TBase::toEigen(result);
        }

        //! Convert to an Eigen heap vector.
        //!
        //! \note The copy should be avoided by RVO.
        inline Eigen::Matrix<double, Eigen::Dynamic, 1> toEigenDynamic(void) const
        {
            Eigen::Matrix<double, Eigen::Dynamic, 1> result(N);
            return this->TBase::toEigen(result);
        }

        //! Get a checksum of this vector's components.
        uint64_t checksum(void) const
        {
            return this->TBase::checksum();
        }

        //! Get the smallest possible vector.
        static const CVectorNx1 &smallest(void)
        {
            static const CVectorNx1 result(boost::numeric::bounds<T>::lowest());
            return result;
        }

        //! Get the largest possible vector.
        static const CVectorNx1 &largest(void)
        {
            static const CVectorNx1 result(boost::numeric::bounds<T>::highest());
            return result;
        }
};

//! Output for debug.
template<typename T, std::size_t N>
std::ostream &operator<<(std::ostream &o, const CVectorNx1<T, N> &v)
{
    o << "[";
    for (std::size_t i = 0u; i+1 < N; ++i)
    {
        o << core::CStringUtils::typeToStringPretty(v(i)) << ' ';
    }
    o << core::CStringUtils::typeToStringPretty(v(N-1)) << ']';
    return o;
}

//! Construct from the outer product of a vector with itself.
template<typename T, std::size_t N>
CSymmetricMatrixNxN<T, N>::CSymmetricMatrixNxN(ESymmetricMatrixType type,
                                               const CVectorNx1<T, N> &x)
{
    switch (type)
    {
    case E_OuterProduct:
        for (std::size_t i = 0u, i_ = 0u; i < N; ++i)
        {
            for (std::size_t j = 0u; j <= i; ++j, ++i_)
            {
                TBase::m_LowerTriangle[i_] = x(i) * x(j);
            }
        }
        break;
    case E_Diagonal:
        for (std::size_t i = 0u, i_ = 0u; i < N; ++i)
        {
            for (std::size_t j = 0u; j <= i; ++j, ++i_)
            {
                TBase::m_LowerTriangle[i_] = i == j ? x(i) : T(0);
            }
        }
        break;
    }
}


// ************************ HEAP VECTOR ************************

//! \brief A heap based lightweight dense vector class.
//!
//! DESCRIPTION:\n
//! This implements a heap based mathematical vector object. The idea
//! is to provide utility functions and operators which mean that it
//! works with other prelert::maths:: classes, such as the symmetric
//! matrix object and the sample (co)variance accumulators, and keep the
//! memory footprint as small as possible. This is not meant to be an
//! alternative to a good linear analysis package implementation. For
//! example, if you want to any serious linear algebra use the Eigen
//! library. An implicit conversion operator for doing this has been
//! supplied.
//!
//! IMPLEMENTATION DECISIONS:\n
//! Operators follow the Matlab component-wise convention. This provides
//! a constructor to initialize to a multiple of the 1 vector. There is
//! no bounds checking for efficiency. The floating point type is templated
//! so that one can use float when space is really at a premium.
//!
//! \tparam T The floating point type.
template<typename T>
class CVector : private boost::equality_comparable< CVector<T>,
                        boost::partially_ordered< CVector<T>,
                        boost::addable< CVector<T>,
                        boost::subtractable< CVector<T>,
                        boost::multipliable< CVector<T>,
                        boost::multipliable2< CVector<T>, T,
                        boost::dividable< CVector<T>,
                        boost::dividable2< CVector<T>, T > > > > > > > >,
                private linear_algebra_detail::SVector<std::vector<T> >
{
    private:
        typedef linear_algebra_detail::SVector<std::vector<T> > TBase;
        template<typename U> friend class CVector;

    public:
        typedef std::vector<T> TArray;
        typedef typename TArray::const_iterator TConstIterator;

        //! \brief Computes a hash of the components of a vector.
        class CHash
        {
            public:
                std::size_t operator()(const CVector<T> &v) const
                {
                    return static_cast<std::size_t>(boost::unwrap_ref(v).checksum());
                }
        };

        //! \brief Compares two vectors for equality.
        class CEqual
        {
            public:
                bool operator()(const CVector<T> &lhs,
                                const CVector<T> &rhs) const
                {
                    return boost::unwrap_ref(lhs) == boost::unwrap_ref(rhs);
                }
        };

    public:
        //! Set to multiple of ones vector.
        explicit CVector(std::size_t d = 0u, T v = T(0))
        {
            if (d > 0)
            {
                TBase::m_X.resize(d, v);
            }
        }

        //! Construct from a boost array.
        template<std::size_t N>
        explicit CVector(const boost::array<T, N> &a)
        {
            for (std::size_t i = 0u; i < N; ++i)
            {
                TBase::m_X[i] = a[i];
            }
        }

        //! Construct from a vector.
        explicit CVector(const TArray &v)
        {
            TBase::m_X = v;
        }

        //! Construct from a vector.
        template<std::size_t M>
        explicit CVector(const core::CSmallVector<T, M> &v)
        {
            TBase::m_X.assign(v.begin(), v.end());
        }

        //! Construct from the range [\p begin, \p end).
        template<typename ITR>
        CVector(ITR begin, ITR end)
        {
            TBase::m_X.assign(begin, end);
        }

        //! Implicit construction from Eigen vector.
        CVector(const SEigenVector::Type &v)
        {
            TBase::m_X.resize(v.size());
            for (std::size_t i = 0u; i < TBase::m_X.size(); ++i)
            {
                TBase::m_X[i] = v(i);
            }
        }

        //! Copy construction if the underlying type is implicitly
        //! convertible.
        template<typename U>
        CVector(const CVector<U> &other)
        {
            this->operator=(other);
        }

        //! Assignment if the underlying type is implicitly convertible.
        template<typename U>
        const CVector &operator=(const CVector<U> &other)
        {
            TBase::m_X.resize(other.dimension());
            this->TBase::assign(other.base());
            return *this;
        }

        //! Efficiently swap the contents of two vectors.
        void swap(CVector &other)
        {
            TBase::m_X.swap(other.TBase::m_X);
        }

        //! Reserve enough memory to hold \p d components.
        void reserve(std::size_t d)
        {
            TBase::m_X.reserve(d);
        }

        //! Assign the components from the range [\p begin, \p end).
        template<typename ITR>
        void assign(ITR begin, ITR end)
        {
            TBase::m_X.assign(begin, end);
        }

        //! Extend the vector to dimension \p d adding components
        //! initialized to \p v.
        void extend(std::size_t d, T v = T(0))
        {
            TBase::m_X.resize(this->dimension() + d, v);
        }

        //! Extend the vector adding components initialized to \p v.
        template<typename ITR>
        void extend(ITR begin, ITR end)
        {
            TBase::m_X.insert(TBase::m_X.end(), begin, end);
        }

        //! \name Persistence
        //@{
        //! Create from a delimited string.
        bool fromDelimited(const std::string &str)
        {
            return this->TBase::fromDelimited(str);
        }

        //! Persist state to delimited values.
        std::string toDelimited(void) const
        {
            return this->TBase::toDelimited();
        }
        //@}

        //! Get the dimension.
        std::size_t dimension(void) const { return TBase::m_X.size(); }

        //! Get the i'th component (no bounds checking).
        inline T operator()(std::size_t i) const
        {
            return TBase::m_X[i];
        }

        //! Get the i'th component (no bounds checking).
        inline T &operator()(std::size_t i)
        {
            return TBase::m_X[i];
        }

        //! Get an iterator over the elements.
        TConstIterator begin(void) const { return TBase::m_X.begin(); }

        //! Get an iterator to the end of the elements.
        TConstIterator end(void) const { return TBase::m_X.end(); }

        //! Component-wise negation.
        CVector operator-(void) const
        {
            CVector result(*this);
            result.negative();
            return result;
        }

        //! Vector subtraction.
        const CVector &operator-=(const CVector &lhs)
        {
            this->minusEquals(lhs.base());
            return *this;
        }

        //! Vector addition.
        const CVector &operator+=(const CVector &lhs)
        {
            this->plusEquals(lhs.base());
            return *this;
        }

        //! Component-wise multiplication.
        const CVector &operator*=(const CVector &scale)
        {
            this->multiplyEquals(scale.base());
            return *this;
        }

        //! Scalar multiplication.
        const CVector &operator*=(T scale)
        {
            this->multiplyEquals(scale);
            return *this;
        }

        //! Component-wise division.
        const CVector &operator/=(const CVector &scale)
        {
            this->divideEquals(scale.base());
            return *this;
        }

        //! Scalar division.
        const CVector &operator/=(T scale)
        {
            this->divideEquals(scale);
            return *this;
        }

        //! Check if two vectors are identically equal.
        bool operator==(const CVector &other) const
        {
            return this->equal(other.base());
        }

        //! Lexicographical total ordering.
        bool operator<(const CVector &rhs) const
        {
            return this->less(rhs.base());
        }

        //! Check if this is zero.
        bool isZero(void) const
        {
            return this->TBase::isZero();
        }

        //! Inner product.
        double inner(const CVector &covector) const
        {
            return this->TBase::inner(covector.base());
        }

        //! Inner product.
        double inner(const SEigenVector::Type &covector) const
        {
            return this->TBase::template inner<SEigenVector::Type>(covector);
        }

        //! Outer product.
        //!
        //! \note The copy should be avoided by RVO.
        CSymmetricMatrix<T> outer(void) const
        {
            return CSymmetricMatrix<T>(E_OuterProduct, *this);
        }

        //! A diagonal matrix.
        //!
        //! \note The copy should be avoided by RVO.
        CSymmetricMatrix<T> diagonal(void) const
        {
            return CSymmetricMatrix<T>(E_Diagonal, *this);
        }

        //! L1 norm.
        double L1(void) const
        {
            return this->TBase::L1();
        }

        //! Euclidean norm.
        double euclidean(void) const
        {
            return ::sqrt(this->inner(*this));
        }

        //! Convert to a vector on a different underlying type.
        template<typename U>
        inline CVector<U> to(void) const
        {
            return CVector<U>(*this);
        }

        //! Convert to a vector.
        template<typename VECTOR>
        inline VECTOR toVector(void) const
        {
            return VECTOR(this->begin(), this->end());
        }

        //! Convert to an Eigen vector.
        //!
        //! \note The copy should be avoided by RVO.
        inline SEigenVector::Type toEigen(void) const
        {
            SEigenVector::Type result(this->dimension());
            return this->TBase::toEigen(result);
        }

        //! Get a checksum of this vector's components.
        uint64_t checksum(void) const
        {
            return this->TBase::checksum();
        }

        //! Get the smallest possible vector.
        static const CVector &smallest(std::size_t d)
        {
            static const CVector result(d, boost::numeric::bounds<T>::lowest());
            return result;
        }

        //! Get the largest possible vector.
        static const CVector &largest(std::size_t d)
        {
            static const CVector result(d, boost::numeric::bounds<T>::highest());
            return result;
        }
};

//! Output for debug.
template<typename T>
std::ostream &operator<<(std::ostream &o, const CVector<T> &v)
{
    if (v.dimension() == 0)
    {
        return o << "[]";
    }
    o << "[";
    for (std::size_t i = 0u; i+1 < v.dimension(); ++i)
    {
        o << core::CStringUtils::typeToStringPretty(v(i)) << ' ';
    }
    o << core::CStringUtils::typeToStringPretty(v(v.dimension()-1)) << ']';
    return o;
}

//! Construct from the outer product of a vector with itself.
template<typename T>
CSymmetricMatrix<T>::CSymmetricMatrix(ESymmetricMatrixType type,
                                      const CVector<T> &x)
{
    m_D = x.dimension();
    TBase::m_LowerTriangle.resize(m_D * (m_D + 1) / 2);
    switch (type)
    {
    case E_OuterProduct:
        for (std::size_t i = 0u, i_ = 0u; i < x.dimension(); ++i)
        {
            for (std::size_t j = 0u; j <= i; ++j, ++i_)
            {
                TBase::m_LowerTriangle[i_] = x(i) * x(j);
            }
        }
        break;
    case E_Diagonal:
        for (std::size_t i = 0u, i_ = 0u; i < x.dimension(); ++i)
        {
            for (std::size_t j = 0u; j <= i; ++j, ++i_)
            {
                TBase::m_LowerTriangle[i_] = i == j ? x(i) : T(0);
            }
        }
        break;
    }
}


// ************************ FREE FUNCTIONS ************************

//! Efficiently scale the \p i'th row and column by \p scale.
template<typename T, std::size_t N>
void scaleCovariances(std::size_t i,
                      T scale,
                      CSymmetricMatrixNxN<T, N> &m)
{
    scale = ::sqrt(scale);
    for (std::size_t j = 0u; j < m.columns(); ++j)
    {
        if (i == j)
        {
            m(i, j) *= scale;
        }
        m(i, j) *= scale;
    }
}

//! Efficiently scale the rows and columns by \p scale.
template<typename T, std::size_t N>
void scaleCovariances(const CVectorNx1<T, N> &scale,
                      CSymmetricMatrixNxN<T, N> &m)
{
    for (std::size_t i = 0u; i < scale.dimension(); ++i)
    {
        scaleCovariances(i, scale(i), m);
    }
}

//! Compute the matrix vector product
//! <pre class="fragment">
//!   \(M x\)
//! </pre>
//!
//! \param[in] m The matrix.
//! \param[in] x The vector.
template<typename T, std::size_t N>
CVectorNx1<T, N> operator*(const CSymmetricMatrixNxN<T, N> &m,
                           const CVectorNx1<T, N> &x)
{
    CVectorNx1<T, N> result;
    for (std::size_t i = 0u; i < N; ++i)
    {
        double component = 0.0;
        for (std::size_t j = 0u; j < N; ++j)
        {
            component += m(i, j) * x(j);
        }
        result(i) = component;
    }
    return result;
}

//! Overload sqrt for CVectorNx1.
template<typename T, std::size_t N>
CVectorNx1<T, N> sqrt(const CVectorNx1<T, N> &v)
{
    CVectorNx1<T, N> result(v);
    linear_algebra_detail::SSqrt<linear_algebra_detail::VectorTag>::calculate(N, result);
    return result;
}
//! Overload sqrt for CSymmetricMatrixNxN.
template<typename T, std::size_t N>
CSymmetricMatrixNxN<T, N> sqrt(const CSymmetricMatrixNxN<T, N> &m)
{
    CSymmetricMatrixNxN<T, N> result(m);
    linear_algebra_detail::SSqrt<linear_algebra_detail::MatrixTag>::calculate(N, result);
    return result;
}

//! Overload minimum for CVectorNx1.
template<typename T, std::size_t N>
CVectorNx1<T, N> min(const CVectorNx1<T, N> &lhs,
                     const CVectorNx1<T, N> &rhs)
{
    CVectorNx1<T, N> result(rhs);
    linear_algebra_detail::SMin<linear_algebra_detail::VectorVectorTag>::calculate(N, lhs, result);
    return result;
}
//! Overload minimum for CVectorNx1.
template<typename T, std::size_t N>
CVectorNx1<T, N> min(const CVectorNx1<T, N> &lhs, const T &rhs)
{
    CVectorNx1<T, N> result(lhs);
    linear_algebra_detail::SMin<linear_algebra_detail::VectorScalarTag>::calculate(N, result, rhs);
    return result;
}
//! Overload minimum for CVectorNx1.
template<typename T, std::size_t N>
CVectorNx1<T, N> min(const T &lhs, const CVectorNx1<T, N> &rhs)
{
    CVectorNx1<T, N> result(rhs);
    linear_algebra_detail::SMin<linear_algebra_detail::ScalarVectorTag>::calculate(N, lhs, result);
    return result;
}
//! Overload minimum for CSymmetricMatrixNxN.
template<typename T, std::size_t N>
CSymmetricMatrixNxN<T, N> min(const CSymmetricMatrixNxN<T, N> &lhs,
                              const CSymmetricMatrixNxN<T, N> &rhs)
{
    CSymmetricMatrixNxN<T, N> result(rhs);
    linear_algebra_detail::SMin<linear_algebra_detail::MatrixMatrixTag>::calculate(N, lhs, result);
    return result;
}
//! Overload minimum for CSymmetricMatrixNxN.
template<typename T, std::size_t N>
CSymmetricMatrixNxN<T, N> min(const CSymmetricMatrixNxN<T, N> &lhs,
                              const T &rhs)
{
    CSymmetricMatrixNxN<T, N> result(lhs);
    linear_algebra_detail::SMin<linear_algebra_detail::MatrixScalarTag>::calculate(N, result, rhs);
    return result;
}
//! Overload minimum for CSymmetricMatrixNxN.
template<typename T, std::size_t N>
CSymmetricMatrixNxN<T, N> min(const T &lhs,
                              const CSymmetricMatrixNxN<T, N> &rhs)
{
    CSymmetricMatrixNxN<T, N> result(rhs);
    linear_algebra_detail::SMin<linear_algebra_detail::ScalarMatrixTag>::calculate(N, lhs, result);
    return result;
}

//! Overload maximum for CVectorNx1.
template<typename T, std::size_t N>
CVectorNx1<T, N> max(const CVectorNx1<T, N> &lhs,
                     const CVectorNx1<T, N> &rhs)
{
    CVectorNx1<T, N> result(rhs);
    linear_algebra_detail::SMax<linear_algebra_detail::VectorVectorTag>::calculate(N, lhs, result);
    return result;
}
//! Overload maximum for CVectorNx1.
template<typename T, std::size_t N>
CVectorNx1<T, N> max(const CVectorNx1<T, N> &lhs, const T &rhs)
{
    CVectorNx1<T, N> result(lhs);
    linear_algebra_detail::SMax<linear_algebra_detail::VectorScalarTag>::calculate(N, result, rhs);
    return result;
}
//! Overload maximum for CVectorNx1.
template<typename T, std::size_t N>
CVectorNx1<T, N> max(const T &lhs, const CVectorNx1<T, N> &rhs)
{
    CVectorNx1<T, N> result(rhs);
    linear_algebra_detail::SMax<linear_algebra_detail::ScalarVectorTag>::calculate(N, lhs, result);
    return result;
}
//! Overload maximum for CSymmetricMatrixNxN.
template<typename T, std::size_t N>
CSymmetricMatrixNxN<T, N> max(const CSymmetricMatrixNxN<T, N> &lhs,
                              const CSymmetricMatrixNxN<T, N> &rhs)
{
    CSymmetricMatrixNxN<T, N> result(rhs);
    linear_algebra_detail::SMax<linear_algebra_detail::MatrixMatrixTag>::calculate(N, lhs, result);
    return result;
}
//! Overload maximum for CSymmetricMatrixNxN.
template<typename T, std::size_t N>
CSymmetricMatrixNxN<T, N> max(const CSymmetricMatrixNxN<T, N> &lhs,
                              const T &rhs)
{
    CSymmetricMatrixNxN<T, N> result(lhs);
    linear_algebra_detail::SMax<linear_algebra_detail::MatrixScalarTag>::calculate(N, result, rhs);
    return result;
}
//! Overload maximum for CSymmetricMatrixNxN.
template<typename T, std::size_t N>
CSymmetricMatrixNxN<T, N> max(const T &lhs,
                              const CSymmetricMatrixNxN<T, N> &rhs)
{
    CSymmetricMatrixNxN<T, N> result(rhs);
    linear_algebra_detail::SMax<linear_algebra_detail::ScalarMatrixTag>::calculate(N, lhs, result);
    return result;
}

//! Overload ::fabs for CVectorNx1.
template<typename T, std::size_t N>
CVectorNx1<T, N> fabs(const CVectorNx1<T, N> &v)
{
    CVectorNx1<T, N> result(v);
    linear_algebra_detail::SFabs<linear_algebra_detail::VectorTag>::calculate(N, result);
    return result;
}
//! Overload ::fabs for CSymmetricMatrixNxN.
template<typename T, std::size_t N>
CSymmetricMatrixNxN<T, N> fabs(const CSymmetricMatrixNxN<T, N> &m)
{
    CSymmetricMatrixNxN<T, N> result(m);
    linear_algebra_detail::SFabs<linear_algebra_detail::MatrixTag>::calculate(N, result);
    return result;
}

//! Efficiently scale the \p i'th row and column by \p scale.
template<typename T>
void scaleCovariances(std::size_t i,
                      T scale,
                      CSymmetricMatrix<T> &m)
{
    scale = ::sqrt(scale);
    for (std::size_t j = 0u; j < m.columns(); ++j)
    {
        if (i == j)
        {
            m(i, j) = scale;
        }
        m(i, j) = scale;
    }
}

//! Efficiently scale the rows and columns by \p scale.
template<typename T>
void scaleCovariances(const CVector<T> &scale,
                      CSymmetricMatrix<T> &m)
{
    for (std::size_t i = 0u; i < scale.dimension(); ++i)
    {
        scaleRowAndColumn(i, scale(i), m);
    }
}

//! Compute the matrix vector product
//! <pre class="fragment">
//!   \(M x\)
//! </pre>
//!
//! \param[in] m The matrix.
//! \param[in] x The vector.
template<typename T>
CVector<T> operator*(const CSymmetricMatrix<T> &m,
                     const CVector<T> &x)
{
    CVector<T> result(x.dimension());
    for (std::size_t i = 0u; i < m.rows(); ++i)
    {
        double component = 0.0;
        for (std::size_t j = 0u; j < m.columns(); ++j)
        {
            component += m(i, j) * x(j);
        }
        result(i) = component;
    }
    return result;
}

//! Overload sqrt for CVector.
template<typename T>
CVector<T> sqrt(const CVector<T> &v)
{
    CVector<T> result(v);
    linear_algebra_detail::SSqrt<linear_algebra_detail::VectorTag>::calculate(result.dimension(), result);
    return result;
}
//! Overload sqrt for CSymmetricMatrix.
template<typename T>
CSymmetricMatrix<T> sqrt(const CSymmetricMatrix<T> &m)
{
    CSymmetricMatrix<T> result(m);
    linear_algebra_detail::SSqrt<linear_algebra_detail::MatrixTag>::calculate(result.rows(), result);
    return result;
}

//! Overload minimum for CVector.
template<typename T>
CVector<T> min(const CVector<T> &lhs, const CVector<T> &rhs)
{
    CVector<T> result(rhs);
    linear_algebra_detail::SMin<linear_algebra_detail::VectorVectorTag>::calculate(result.dimension(), lhs, result);
    return result;
}
//! Overload minimum for CVector.
template<typename T>
CVector<T> min(const CVector<T> &lhs, const T &rhs)
{
    CVector<T> result(lhs);
    linear_algebra_detail::SMin<linear_algebra_detail::VectorScalarTag>::calculate(result.dimension(), result, rhs);
    return result;
}
//! Overload minimum for CVector.
template<typename T>
CVector<T> min(const T &lhs, const CVector<T> &rhs)
{
    CVector<T> result(rhs);
    linear_algebra_detail::SMin<linear_algebra_detail::ScalarVectorTag>::calculate(result.dimension(), lhs, result);
    return result;
}
//! Overload minimum for CSymmetricMatrix.
template<typename T>
CSymmetricMatrix<T> min(const CSymmetricMatrix<T> &lhs, const CSymmetricMatrix<T> &rhs)
{
    CSymmetricMatrix<T> result(rhs);
    linear_algebra_detail::SMin<linear_algebra_detail::MatrixMatrixTag>::calculate(result.rows(), lhs, result);
    return result;
}
//! Overload minimum for CSymmetricMatrix.
template<typename T>
CSymmetricMatrix<T> min(const CSymmetricMatrix<T> &lhs, const T &rhs)
{
    CSymmetricMatrix<T> result(lhs);
    linear_algebra_detail::SMin<linear_algebra_detail::MatrixScalarTag>::calculate(result.rows(), result, rhs);
    return result;
}
//! Overload minimum for CSymmetricMatrix.
template<typename T>
CSymmetricMatrix<T> min(const T &lhs, const CSymmetricMatrix<T> &rhs)
{
    CSymmetricMatrix<T> result(rhs);
    linear_algebra_detail::SMin<linear_algebra_detail::ScalarMatrixTag>::calculate(result.rows(), lhs, result);
    return result;
}

//! Overload maximum for CVector.
template<typename T>
CVector<T> max(const CVector<T> &lhs, const CVector<T> &rhs)
{
    CVector<T> result(rhs);
    linear_algebra_detail::SMax<linear_algebra_detail::VectorVectorTag>::calculate(result.dimension(), lhs, result);
    return result;
}
//! Overload maximum for CVector.
template<typename T>
CVector<T> max(const CVector<T> &lhs, const T &rhs)
{
    CVector<T> result(lhs);
    linear_algebra_detail::SMax<linear_algebra_detail::VectorScalarTag>::calculate(result.dimension(), result, rhs);
    return result;
}
//! Overload maximum for CVector.
template<typename T>
CVector<T> max(const T &lhs, const CVector<T> &rhs)
{
    CVector<T> result(rhs);
    linear_algebra_detail::SMax<linear_algebra_detail::ScalarVectorTag>::calculate(result.dimension(), lhs, result);
    return result;
}
//! Overload maximum for CSymmetricMatrix.
template<typename T>
CSymmetricMatrix<T> max(const CSymmetricMatrix<T> &lhs, const CSymmetricMatrix<T> &rhs)
{
    CSymmetricMatrix<T> result(rhs);
    linear_algebra_detail::SMax<linear_algebra_detail::MatrixMatrixTag>::calculate(result.rows(), lhs, result);
    return result;
}
//! Overload maximum for CSymmetricMatrix.
template<typename T>
CSymmetricMatrix<T> max(const CSymmetricMatrix<T> &lhs, const T &rhs)
{
    CSymmetricMatrix<T> result(lhs);
    linear_algebra_detail::SMax<linear_algebra_detail::MatrixScalarTag>::calculate(result.rows(), result, rhs);
    return result;
}
//! Overload maximum for CSymmetricMatrix.
template<typename T>
CSymmetricMatrix<T> max(const T &lhs, const CSymmetricMatrix<T> &rhs)
{
    CSymmetricMatrix<T> result(rhs);
    linear_algebra_detail::SMax<linear_algebra_detail::ScalarMatrixTag>::calculate(result.rows(), lhs, result);
    return result;
}

//! Overload ::fabs for CVector.
template<typename T>
CVector<T> fabs(const CVector<T> &v)
{
    CVector<T> result(v);
    linear_algebra_detail::SFabs<linear_algebra_detail::VectorTag>::calculate(result.dimension(), result);
    return result;
}
//! Overload ::fabs for CSymmetricMatrix.
template<typename T>
CSymmetricMatrix<T> fabs(const CSymmetricMatrix<T> &m)
{
    CSymmetricMatrix<T> result(m);
    linear_algebra_detail::SFabs<linear_algebra_detail::MatrixTag>::calculate(result.dimension(), result);
    return result;
}

}
}

#endif // INCLUDED_prelert_maths_CLinearAlgebra_h
