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

#ifndef INCLUDED_prelert_maths_CSignal_h
#define INCLUDED_prelert_maths_CSignal_h

#include <maths/ImportExport.h>

#include <complex>
#include <vector>

namespace prelert
{
namespace maths
{

//! \brief Useful functions from signal processing.
class MATHS_EXPORT CSignal
{
    public:
        typedef std::complex<double> TComplex;
        typedef std::vector<TComplex> TComplexVec;

    public:
        //! Compute the conjugate of \p f.
        static void conj(TComplexVec &f);

        //! Compute the Hadamard product of \p fx and \p fy.
        static void hadamard(const TComplexVec &fx, TComplexVec &fy);

        //! Cooley-Tukey fast DFT transform implementation.
        //!
        //! \note This is a simple implementation radix 2 DIT which uses the chirp-z
        //! idea to handle the case that the length of \p fx is not a power of 2. As
        //! such it is definitely not a highly optimized FFT implementation. It should
        //! be sufficiently fast for our needs.
        static void fft(TComplexVec &f);

        //! This uses conjugate of the conjugate of the series is the inverse DFT trick
        //! to compute this using fft.
        static void ifft(TComplexVec &f);
};

}
}

#endif // INCLUDED_prelert_maths_CSignal_h
