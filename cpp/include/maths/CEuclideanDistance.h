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
#ifndef INCLUDED_ml_maths_CEuclideanDistance_h
#define INCLUDED_ml_maths_CEuclideanDistance_h

#include <maths/ImportExport.h>

#include <vector>


namespace ml
{
namespace maths
{

//! \brief
//! Calculate Euclidean distance between two sequences.
//!
//! DESCRIPTION:\n
//! The Euclidean distance between two sequences is the square root of
//! the sum of the squares of the differences between values at the same
//! position in the two sequences.
//!
//! IMPLEMENTATION DECISIONS:\n
//! Uses double - could be templatised to use extended or other value.
//!
class MATHS_EXPORT CEuclideanDistance
{
    public:
        typedef std::vector<double>         TDoubleVec;
        typedef TDoubleVec::const_iterator  TDoubleVecCItr;

    public:
        //! Compute the euclidean distance between two series
        static bool euclideanDistance(const TDoubleVecCItr &series1Begin,
                                      const TDoubleVecCItr &series1End,
                                      const TDoubleVecCItr &series2Begin,
                                      const TDoubleVecCItr &series2End,
                                      double &distance);

    private:
        //! Utility class, hence hidden constructors
        CEuclideanDistance(void);
        CEuclideanDistance(const CEuclideanDistance &);
};


}
}

#endif // INCLUDED_ml_maths_CEuclideanDistance_h

