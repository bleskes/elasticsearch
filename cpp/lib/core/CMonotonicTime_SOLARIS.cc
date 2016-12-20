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
#include <core/CMonotonicTime.h>

#include <sys/time.h>


namespace prelert
{
namespace core
{


CMonotonicTime::CMonotonicTime(void)
    // Scaling factors never vary for gethrtime()
    : m_ScalingFactor1(0),
      m_ScalingFactor2(0),
      m_ScalingFactor3(0)
{
}

uint64_t CMonotonicTime::milliseconds(void) const
{
    return static_cast<uint64_t>(::gethrtime()) / 1000000ULL;
}

uint64_t CMonotonicTime::nanoseconds(void) const
{
    return static_cast<uint64_t>(::gethrtime());
}


}
}

