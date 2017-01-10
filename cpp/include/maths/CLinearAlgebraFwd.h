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

#ifndef INCLUDED_ml_maths_CLinearAlgebraFwd_h
#define INCLUDED_ml_maths_CLinearAlgebraFwd_h

#include <maths/ImportExport.h>

#include <cstddef>

namespace ml
{
namespace maths
{

//! Types of symmetric matrices constructed with a vector.
enum ESymmetricMatrixType
{
    E_OuterProduct,
    E_Diagonal
};

//! \brief Common types used by the vector and matrix classes.
class MATHS_EXPORT CLinearAlgebra
{
    public:
        static const char DELIMITER = ',';
};

template<typename T, std::size_t> class CVectorNx1;
template<typename T, std::size_t N> class CSymmetricMatrixNxN;
template<typename T> class CVector;
template<typename T> class CSymmetricMatrix;
template<typename VECTOR, typename ANNOTATION> class CAnnotatedVector;

}
}

#endif // INCLUDED_ml_maths_CLinearAlgebraFwd_h
