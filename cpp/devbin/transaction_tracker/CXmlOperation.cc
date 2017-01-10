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
#include "CXmlOperation.h"

#include <core/CLogger.h>


namespace ml
{
namespace devbin
{


CXmlOperation::CXmlOperation(const core::CXmlNodeWithChildren::TXmlNodeWithChildrenP &dataPtr) : m_DataPtr(dataPtr)
{
}

bool CXmlOperation::_getValue(const core::CXmlNodeWithChildren::TXmlNodeWithChildrenP &dataPtr, const std::string &name, int value)
{
    if (dataPtr->name() == name)
    {
        return core::CStringUtils::stringToType(dataPtr->value(), value);
    }

    if (dataPtr->children().empty())
    {
        LOG_DEBUG("Can not find " << name);
        return false;
    }

    for (core::CXmlNodeWithChildren::TChildNodePVecCItr childIter = dataPtr->children().begin();
         childIter != dataPtr->children().end();
         ++childIter)
    {
        if (CXmlOperation::_getValue(*childIter, name, value) == true)
        {
            return true;
        }
    }

    return false;
}

bool CXmlOperation::_getValue(const core::CXmlNodeWithChildren::TXmlNodeWithChildrenP &dataPtr, const std::string &name, std::string &value)
{
    if (dataPtr->name() == name)
    {
        value = dataPtr->value();

        return true;
    }

    if (dataPtr->children().empty())
    {
        return false;
    }

    for (core::CXmlNodeWithChildren::TChildNodePVecCItr childIter = dataPtr->children().begin();
         childIter != dataPtr->children().end();
         ++childIter)
    {
        if (CXmlOperation::_getValue(*childIter, name, value) == true)
        {
            return true;
        }
    }

    return false;
}

void CXmlOperation::setTime(const boost::posix_time::ptime &time)
{
    m_Time = time;
}

void CXmlOperation::setDuration(const boost::posix_time::time_duration &duration)
{
    m_Duration = duration;
}

void CXmlOperation::setMsg(const std::string &msg)
{
    m_Msg = msg;
}

void CXmlOperation::setVm(const std::string &vm)
{
    m_Vm = vm;
}

const boost::posix_time::ptime &CXmlOperation::getTime(void) const
{
    return m_Time;
}

const boost::posix_time::time_duration &CXmlOperation::getDuration(void) const
{
    return m_Duration;
}

const std::string &CXmlOperation::getMsg(void) const
{
    return m_Msg;
}

const std::string &CXmlOperation::getVm(void) const
{
    return m_Vm;
}


}
}
