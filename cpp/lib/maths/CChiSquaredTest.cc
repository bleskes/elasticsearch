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
#include <maths/CChiSquaredTest.h>

#include <maths/CBasicStatistics.h>

#include <core/CLogger.h>

#include <boost/math/special_functions/gamma.hpp>


namespace prelert
{
namespace maths
{


bool CChiSquaredTest::oneSampleTestVsMean(const TDoubleVec &sample, double &probability)
{
    double dummy;

    return CChiSquaredTest::oneSampleTestVsMean(sample, probability, dummy);
}

bool CChiSquaredTest::oneSampleTestVsMean(const TDoubleVec &sample, double &probability, double &mean)
{
    probability = 0.0;

    if (sample.size() <= 1)
    {
        LOG_ERROR("Unable to compute Chi-squared on sample with <= 1 entry " << sample.size());
        return false;
    }

    // Check sample range > 0
    for (TDoubleVecCItr itr = sample.begin(); itr != sample.end(); ++itr)
    {
        if ((*itr) < 0.0)
        {
            LOG_ERROR("Cannot perform Chi-Squared test with sample value <= 0 " << *itr);
            return false;
        }
    }

    // Not currently an optimal test
    mean = CBasicStatistics::mean(sample);

    // Special case of all 0.0 distribution, in this case we
    // return probability is 1.0 as all values must be 0.0
    if (mean == 0.0)
    {
        probability = 1.0;
        return true;
    }

    double chiSquared(0.0);

    for (TDoubleVecCItr itr = sample.begin(); itr != sample.end(); ++itr)
    {
        if ((*itr) < 0.0)
        {
            LOG_ERROR("Cannot perform Chi-Squared test with sample value <= 0 " << *itr);
            return false;
        }

        double temp = (*itr) - mean;

        chiSquared += (temp * temp) / mean;
    }

    // boost throws exceptions
    try
    {
        // Now compute the probability of this
        probability = boost::math::gamma_q(double(0.5)*double(sample.size()-1), double(0.5)*chiSquared);
    }
    catch (const std::exception &e)
    {
        LOG_ERROR(e.what());
        return false;
    }

    return true;
}


}
}

