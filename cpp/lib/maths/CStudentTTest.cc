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
#include <maths/CStudentTTest.h>

#include <maths/CBasicStatistics.h>

#include <core/CLogger.h>

#include <limits>

#include <math.h>



namespace ml
{
namespace maths
{


bool    CStudentTTest::oneSampleTest(const TDoubleVec &sample, double &probability)
{
    probability = 0.0;

    double  mean = CBasicStatistics::mean(sample);
    
    if(sample.size() < 2)
    {
        LOG_ERROR("Failed to compute t-value, sample size must be > 2 " << sample.size());
        return false;
    }

    // y = a + bx
    // 
    // a = mean
    // b = 1

    // sum of squares of residuals == sum(x - mean)^2

    double  sse = 0.0;

    for(TDoubleVecCItr itr = sample.begin(); itr != sample.end(); ++itr)
    {
        double  temp = (*itr - mean);
        
        sse += temp * temp;
    }

    if(sse == 0.0)
    {
        probability = boost::numeric::bounds<double>::highest();
    }
    else
    {
        probability = ::sqrt(double(sample.size() - 2)) / sse;
    }

    return true;
}


}
}
