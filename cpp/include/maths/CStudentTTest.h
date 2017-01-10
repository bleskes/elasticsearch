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
#ifndef INCLUDED_ml_maths_CStudentTTest_h
#define INCLUDED_ml_maths_CStudentTTest_h

#include <maths/ImportExport.h>

#include <vector>


namespace ml
{
namespace maths
{


//! \brief
//! Implementation of Student T test
//!
//! DESCRIPTION:\n
//! Implementation of Student T test
//!
//! IMPLEMENTATION DECISIONS:\n
//! Uses boost distributions.
//!
class MATHS_EXPORT CStudentTTest
{
    public:
        typedef std::vector<double>         TDoubleVec;
        typedef TDoubleVec::const_iterator  TDoubleVecCItr;
    public:

        //! This test computes the mean and sample SD first
        //! and returns the 'independent one-sample t-test'
        //! of the distribution.
        static bool oneSampleTest(const TDoubleVec &sample, double &probability);
};


}
}

#endif // INCLUDED_ml_maths_CStudentTTest_h

