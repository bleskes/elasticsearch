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
#include <maths/CAnomalyFactorCalculator.h>

#include <core/CLogger.h>

#include <limits>

#include <math.h>


namespace ml
{
namespace maths
{


int32_t CAnomalyFactorCalculator::fromDistributionProb(double deviation)
{
    if (deviation < 0.0 || deviation > 1.0)
    {
        LOG_ERROR("Deviation not in range [0, 1] : " << deviation);
        return 0;
    }

    if (deviation == 0.0)
    {
        // If we're here then the deviation is so small that when represented
        // as a double it's 0.  So, just return the maximum anomaly factor.
        return 100;
    }

    // We want to scale the deviation non-linearly to get the anomaly factor,
    // because we want to distinguish between, say, 0.1e-10 and 0.1e-100, both
    // of which would round to the same number, even after multiplying by a
    // billion.  We could have used a logarithm here, but that's slow and since
    // we'd round the result anyway, we might as well just extract the (base 2)
    // exponent from the double.
    int exponent(0);
    ::frexp(deviation, &exponent);

    // The lowest (base 2) exponent is -1022, so use this to scale the result to
    // the range [1, 100].
    return 100 * exponent / std::numeric_limits<double>::min_exponent;
}


}
}

