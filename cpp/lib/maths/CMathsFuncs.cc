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
#include <maths/CMathsFuncs.h>

#include <boost/math/special_functions/fpclassify.hpp>

#ifdef isnan
#undef isnan
#endif

#ifdef isinf
#undef isinf
#endif

namespace prelert
{
namespace maths
{

bool CMathsFuncs::isNan(double val)
{
    return boost::math::isnan(val);
}

bool CMathsFuncs::isInf(double val)
{
    return boost::math::isinf(val);
}

bool CMathsFuncs::isFinite(double val)
{
    return boost::math::isfinite(val);
}

maths_t::EFloatingPointErrorStatus CMathsFuncs::fpStatus(double val)
{
    if (isNan(val))
    {
        return maths_t::E_FpFailed;
    }
    if (isInf(val))
    {
        return maths_t::E_FpOverflowed;
    }
    return maths_t::E_FpNoErrors;
}

}
}

