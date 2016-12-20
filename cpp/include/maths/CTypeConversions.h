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

#ifndef INCLUDED_prelert_maths_CTypeConversions_h
#define INCLUDED_prelert_maths_CTypeConversions_h

#include <maths/CDoublePrecisionStorage.h>
#include <maths/CFloatStorage.h>
#include <maths/CLinearAlgebraFwd.h>

#include <boost/type_traits/is_floating_point.hpp>

namespace prelert
{
namespace maths
{

//! \brief Defines the promoted type.
template<typename T>
struct SPromoted
{
    typedef T Type;
};

//! \brief Defines the promoted type for float.
template<>
struct SPromoted<float>
{
    typedef double Type;
};

//! \brief Defines the promoted type for CFloatStorage.
template<>
struct SPromoted<CFloatStorage>
{
    typedef double Type;
};

//! \brief Defines the promoted type for a CVectorNx1.
template<typename T, std::size_t N>
struct SPromoted<CVectorNx1<T, N> >
{
     typedef CVectorNx1<typename SPromoted<T>::Type, N> Type;
};

//! \brief Defines the promoted type for a CVector.
template<typename T>
struct SPromoted<CVector<T> >
{
     typedef CVector<typename SPromoted<T>::Type> Type;
};

//! \brief Defines the promoted type for a CSymmetricMatrixNxN.
template<typename T, std::size_t N>
struct SPromoted<CSymmetricMatrixNxN<T, N> >
{
     typedef CSymmetricMatrixNxN<typename SPromoted<T>::Type, N> Type;
};

//! \brief Defines the promoted type for a CSymmetricMatrix.
template<typename T>
struct SPromoted<CSymmetricMatrix<T> >
{
     typedef CSymmetricMatrix<typename SPromoted<T>::Type> Type;
};

//! \brief Defines the promoted type for a CAnnotatedVector.
template<typename VECTOR, typename ANNOTATION>
struct SPromoted<CAnnotatedVector<VECTOR, ANNOTATION> >
{
    typedef CAnnotatedVector<typename SPromoted<VECTOR>::Type, ANNOTATION> Type;
};

namespace type_conversion_detail
{

//! \brief Chooses between T and U based on the checks for
//! integral and floating point types.
template<typename T, typename U, bool FLOATING_POINT>
struct SSelector
{
    typedef U Type;
};
template<typename T, typename U>
struct SSelector<T, U, true>
{
    typedef T Type;
};

} // type_conversion_detail::

//! \brief Defines a suitable floating point type.
template<typename T, typename U>
struct SFloatingPoint
{
    typedef typename type_conversion_detail::SSelector<
                         T,
                         U,
                         boost::is_floating_point<T>::value>::Type Type;
};

//! \brief Defines CVectorNx1 on a suitable floating point
//! type.
template<typename T, std::size_t N, typename U>
struct SFloatingPoint<CVectorNx1<T, N>, U>
{
     typedef CVectorNx1<typename SFloatingPoint<T, U>::Type, N> Type;
};

//! \brief Defines CVector on a suitable floating point type.
template<typename T, typename U>
struct SFloatingPoint<CVector<T>, U>
{
     typedef CVector<typename SFloatingPoint<T, U>::Type> Type;
};

//! \brief Defines CSymmetricMatrixNxN on a suitable floating
//! point type.
template<typename T, std::size_t N, typename U>
struct SFloatingPoint<CSymmetricMatrixNxN<T, N>, U>
{
     typedef CSymmetricMatrixNxN<typename SFloatingPoint<T, U>::Type, N> Type;
};

//! \brief Defines CSymmetricMatrix on a suitable floating
//! point type.
template<typename T, typename U>
struct SFloatingPoint<CSymmetricMatrix<T>, U>
{
     typedef CSymmetricMatrix<typename SFloatingPoint<T, U>::Type> Type;
};

//! \brief Defines CAnnotatedVector on a suitable floating
//! point type.
template<typename VECTOR, typename ANNOTATION, typename U>
struct SFloatingPoint<CAnnotatedVector<VECTOR, ANNOTATION>, U>
{
    typedef CAnnotatedVector<typename SFloatingPoint<VECTOR, U>::Type, ANNOTATION> Type;
};


//! \brief Extracts the coordinate type for a point.
template<typename T>
struct SCoordinate
{
    typedef T Type;
};

//! \brief Extracts the coordinate type for CVectorNx1.
template<typename T, std::size_t N>
struct SCoordinate<CVectorNx1<T, N> >
{
    typedef T Type;
};

//! \brief Extracts the coordinate type for CVector.
template<typename T>
struct SCoordinate<CVector<T> >
{
    typedef T Type;
};

//! \brief Extracts the coordinate type for the underlying
//! vector type.
template<typename VECTOR, typename ANNOTATION>
struct SCoordinate<CAnnotatedVector<VECTOR, ANNOTATION> >
{
    typedef typename SCoordinate<VECTOR>::Type Type;
};

//! \brief Extracts the coordinate type for CSymmetricMatrixNxN.
template<typename T, std::size_t N>
struct SCoordinate<CSymmetricMatrixNxN<T, N> >
{
    typedef T Type;
};

//! \brief Extracts the coordinate type for CSymmetricMatrix.
template<typename T>
struct SCoordinate<CSymmetricMatrix<T> >
{
    typedef T Type;
};


//! \brief Extracts the conformable matrix type for a point.
template<typename POINT>
struct SConformableMatrix
{
    typedef POINT Type;
};

//! \brief Extracts the conformable matrix type for a CVectorNx1.
template<typename T, std::size_t N>
struct SConformableMatrix<CVectorNx1<T, N> >
{
    typedef CSymmetricMatrixNxN<T, N> Type;
};

//! \brief Extracts the conformable matrix type for a CVector.
template<typename T>
struct SConformableMatrix<CVector<T> >
{
    typedef CSymmetricMatrix<T> Type;
};

//! \brief Extracts the conformable matrix type for the underlying
//! vector type.
template<typename VECTOR, typename ANNOTATION>
struct SConformableMatrix<CAnnotatedVector<VECTOR, ANNOTATION> >
{
    typedef typename SConformableMatrix<VECTOR>::Type Type;
};


//! \brief Defines a type which strips off any annotation from
//! a vector. This is the raw vector type by default.
template<typename VECTOR>
struct SStripped
{
    typedef VECTOR Type;
};

//! \brief Specialisation for annotated vectors. This is the
//! underlying vector type.
template<typename VECTOR, typename ANNOTATION>
struct SStripped<CAnnotatedVector<VECTOR, ANNOTATION> >
{
    typedef VECTOR Type;

};

}
}

#endif // INCLUDED_prelert_maths_CTypeConversions_h
