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
#include <maths/CEuclideanDistance.h>

#include <core/CLogger.h>

#include <math.h>


namespace ml
{
namespace maths
{


bool CEuclideanDistance::euclideanDistance(const TDoubleVecCItr &series1Begin,
                                           const TDoubleVecCItr &series1End,
                                           const TDoubleVecCItr &series2Begin,
                                           const TDoubleVecCItr &series2End,
                                           double &distance)
{
    double sumDiffSquares(0.0);

    if ((series1End - series1Begin) != (series2End - series2Begin))
    {
        LOG_ERROR("Series 1 and series 2 must be the same length, but are " <<
                  (series1End - series1Begin) << " and " <<
                  (series2End - series2Begin) << " respectively");

        return false;
    }

    TDoubleVecCItr iter2 = series2Begin;
    for (TDoubleVecCItr iter1 = series1Begin; iter1 != series1End; ++iter1, ++iter2)
    {
        double temp = *iter1 - *iter2;
        sumDiffSquares += temp * temp;
    }

    distance = ::sqrt(sumDiffSquares);

    return true;
}


}
}

