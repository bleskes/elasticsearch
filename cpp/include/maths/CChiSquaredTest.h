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
#ifndef INCLUDED_ml_maths_CChiSquaredTest_h
#define INCLUDED_ml_maths_CChiSquaredTest_h

#include <maths/ImportExport.h>

#include <vector>


namespace ml
{
namespace maths
{


//! \brief
//! Implementation of Chi squared test.
//!
//! DESCRIPTION:\n
//! Implementation of Chi squared test.
//!
//! IMPLEMENTATION DECISIONS:\n
//! Uses boost distributions.
//!
class MATHS_EXPORT CChiSquaredTest
{
    public:
        typedef std::vector<double>         TDoubleVec;
        typedef TDoubleVec::const_iterator  TDoubleVecCItr;
    public:
        //! Perform a simple one sample CHI squared test with
        //! the expected values being the mean of the sample.
        //! The probability returned is the significance of the
        //! sample with respect to the mean. I.e.
        //! probability >= 0.95 good match (within 5%)
        //! probability <= 0.95 bad match (> 5% deviation)
        static bool oneSampleTestVsMean(const TDoubleVec &sample, double &probability);

        //! Do the same but return the mean as well
        static bool oneSampleTestVsMean(const TDoubleVec &sample, double &probability, double &mean);
};


}
}

#endif // INCLUDED_ml_maths_CChiSquaredTest_h

