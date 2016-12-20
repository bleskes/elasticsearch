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
#include <core/CTimezone.h>

#include <core/CLogger.h>
#include <core/CScopedFastLock.h>
#include <core/CSetEnv.h>

#include <errno.h>
#include <string.h>


namespace
{
// To ensure the singleton is constructed before multiple threads may require it
// call instance() during the static initialisation phase of the program.  Of
// course, the instance may already be constructed before this if another static
// object has used it.
const prelert::core::CTimezone &DO_NOT_USE_THIS_VARIABLE =
                                           prelert::core::CTimezone::instance();
}

namespace prelert
{
namespace core
{


CTimezone::CTimezone(void)
{
}

CTimezone::~CTimezone(void)
{
}

CTimezone &CTimezone::instance(void)
{
    static CTimezone instance;
    return instance;
}

const std::string &CTimezone::timezoneName(void) const
{
    CScopedFastLock lock(m_Mutex);

    return m_Name;
}

bool CTimezone::timezoneName(const std::string &name)
{
    CScopedFastLock lock(m_Mutex);

    if (CSetEnv::setEnv("TZ", name.c_str(), 1) != 0)
    {
        LOG_ERROR("Unable to set TZ environment variable to " << name <<
                  " : " << ::strerror(errno));

        return false;
    }

    ::tzset();

    m_Name = name;

    return true;
}

bool CTimezone::setTimezone(const std::string &timezone)
{
    return CTimezone::instance().timezoneName(timezone);
}

std::string CTimezone::stdAbbrev(void) const
{
    CScopedFastLock lock(m_Mutex);

    return ::tzname[0];
}

std::string CTimezone::dstAbbrev(void) const
{
    CScopedFastLock lock(m_Mutex);

    return ::tzname[1];
}

core_t::TTime CTimezone::localToUtc(struct tm &localTime) const
{
    return ::mktime(&localTime);
}

bool CTimezone::utcToLocal(core_t::TTime utcTime, struct tm &localTime) const
{
    if (::localtime_r(&utcTime, &localTime) == 0)
    {
        return false;
    }
    return true;
}


}
}

