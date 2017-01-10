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
#include <core/CTimeGm.h>


namespace ml
{
namespace core
{


time_t CTimeGm::timeGm(struct tm *ts)
{
    ts->tm_isdst = 0;

    struct tm copy(*ts);
    time_t t(::mktime(&copy));

    t -= ::timezone;

    ::gmtime_r(&t, ts);

    return t;
}


}
}

