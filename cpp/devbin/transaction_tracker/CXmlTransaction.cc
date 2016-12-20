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
#include "CXmlTransaction.h"

#include <core/CLogger.h>

#include <set>


namespace prelert
{
namespace devbin
{


CXmlTransaction::CXmlTransaction(void)
{
}

void CXmlTransaction::addXmlOperation(const boost::posix_time::ptime &time, const CXmlOperation &op)
{
    m_Operations.insert(TTimeXmlOperationMMap::value_type(time, op));
}

void CXmlTransaction::debug(void) const
{
    boost::posix_time::time_duration totalDuration(0,0,0,0);

    typedef std::multiset<std::string> TStrMSet;
    typedef TStrMSet::const_iterator   TStrMSetCItr;

    TStrMSet transString;
    std::string reqId;

    for (TTimeXmlOperationMMapCItr itr = m_Operations.begin(); itr != m_Operations.end(); ++itr)
    {
        std::string fullMsg;

        if (itr->second.getValue("cname", fullMsg) == false)
        {
            LOG_ERROR("Inconsistent " << itr->second.getTime());
        }
        if (reqId.empty())
        {
            if (itr->second.getValue("reqid", reqId) == false)
            {
                LOG_ERROR("Inconsistent " << itr->second.getTime());
            }
        }

        LOG_DEBUG("Time=" << itr->first << " " << itr->second.getMsg() << " " << itr->second.getVm() << " " << itr->second.getDuration());

        totalDuration += itr->second.getDuration();

        transString.insert(fullMsg);
    }

    if (m_Operations.empty() == true)
    {
        LOG_ERROR("Empty transaction");
        return;
    }

    TTimeXmlOperationMMapCItr begin = m_Operations.begin();
    TTimeXmlOperationMMapCRItr end = m_Operations.rbegin();

    //LOG_DEBUG("ts_duration " << end->first - begin->first);
    //LOG_DEBUG("total_duration " << totalDuration);

    typedef std::map<std::string, boost::posix_time::time_duration> TStrDurationMap;
    typedef TStrDurationMap::iterator                               TStrDurationMapItr;
    typedef TStrDurationMap::const_iterator                         TStrDurationMapCItr;

    typedef std::map<std::string, int>                              TStrIntMap;
    typedef TStrIntMap::iterator                               TStrIntMapItr;
    typedef TStrIntMap::const_iterator                         TStrIntMapCItr;

    TStrIntMap msgCounts;
    int        msgCount(0);

    TStrDurationMap msgDurations;
    boost::posix_time::time_duration msgDuration(0,0,0,0);

    msgDurations.insert(TStrDurationMap::value_type("SERVLET END;", msgDuration));
    msgDurations.insert(TStrDurationMap::value_type("ACTION END", msgDuration));
    msgDurations.insert(TStrDurationMap::value_type("METHOD END;", msgDuration));
    msgDurations.insert(TStrDurationMap::value_type("SERVICE END;", msgDuration));
    msgDurations.insert(TStrDurationMap::value_type("RELATED SERVICE END;", msgDuration));
    msgDurations.insert(TStrDurationMap::value_type("", msgDuration));
    msgDurations.insert(TStrDurationMap::value_type("Finished service:", msgDuration));
    msgDurations.insert(TStrDurationMap::value_type("JCB END;", msgDuration));

    msgCounts.insert(TStrIntMap::value_type("SERVLET END;", msgCount));
    msgCounts.insert(TStrIntMap::value_type("ACTION END", msgCount));
    msgCounts.insert(TStrIntMap::value_type("METHOD END;", msgCount));
    msgCounts.insert(TStrIntMap::value_type("SERVICE END;", msgCount));
    msgCounts.insert(TStrIntMap::value_type("RELATED SERVICE END;", msgCount));
    msgCounts.insert(TStrIntMap::value_type("", msgCount));
    msgCounts.insert(TStrIntMap::value_type("Finished service:", msgCount));
    msgCounts.insert(TStrIntMap::value_type("JCB END;", msgCount));

    boost::posix_time::time_duration wa5_v94_36400Duration(0,0,0,0);
    boost::posix_time::time_duration wa5_v94_36403Duration(0,0,0,0);

    bool wa5_v94_36400Exists = false;
    bool wa5_v94_36403Exists = false;

    for (TTimeXmlOperationMMapCItr itr = m_Operations.begin(); itr != m_Operations.end(); ++itr)
    {
        /*
        Note wa5_ ops are not always last (e.g. 00f99f54-79bb-11e1-bf3d-a93800f38e30:189)
        if (wa5_v94_36400Exists || wa5_v94_36403Exists)
        {
            LOG_ERROR("Inconsistent " << itr->first);
            continue;
        }
        */

        TStrDurationMapItr jtr = msgDurations.find(itr->second.getMsg());
        if (jtr == msgDurations.end())
        {
            LOG_ERROR("Inconsistent " << itr->second.getMsg() << " " << itr->first);
            continue;
        }
        jtr->second += itr->second.getDuration();

        TStrIntMapItr ktr = msgCounts.find(itr->second.getMsg());
        if (ktr == msgCounts.end())
        {
            LOG_ERROR("Inconsistent " << itr->second.getMsg() << " " << itr->first);
            continue;
        }
        ktr->second++;

        if (itr->second.getVm() == "wa5_v94_36400_chvj408ld702_server")
        {
            // Can happen fdd9435a-79ae-11e1-bf3d-a93800f38e30:337
            if (wa5_v94_36400Exists || wa5_v94_36403Exists)
            {
                // Multiple wa5 - take last
                LOG_ERROR("Multiple wa5" << itr->first);
            }

            {
                wa5_v94_36400Exists = true;
                wa5_v94_36400Duration = itr->second.getDuration();
            }
        }
        else if (itr->second.getVm() == "wa5_v94_36403_chvj408ld702_server")
        {
            // Can happen ef319c9f-79ab-11e1-bf23-a93800f38e33:207
            if (wa5_v94_36400Exists || wa5_v94_36403Exists)
            {
                // Multiple wa5 - take last
                LOG_ERROR("Multiple wa5 " << itr->first);
            }

            {
                wa5_v94_36403Exists = true;
                wa5_v94_36403Duration = itr->second.getDuration();
            }
        }
    }

    boost::posix_time::time_duration servletDuration = msgDurations["SERVLET END;"];
    boost::posix_time::time_duration actionDuration = msgDurations["ACTION END"];
    boost::posix_time::time_duration methodDuration = msgDurations["METHOD END;"];
    boost::posix_time::time_duration serviceDuration = msgDurations["SERVICE END;"];
    boost::posix_time::time_duration relatedServiceDuration = msgDurations["RELATED SERVICE END;"];
    boost::posix_time::time_duration emptyDuration = msgDurations[""];
    boost::posix_time::time_duration finishedDuration = msgDurations["Finished service:"];
    boost::posix_time::time_duration jcbDuration = msgDurations["JCB END;"];

    int servletCount = msgCounts["SERVLET END;"];
    int actionCount = msgCounts["ACTION END"];
    int methodCount = msgCounts["METHOD END;"];
    int serviceCount = msgCounts["SERVICE END;"];
    int relatedServiceCount = msgCounts["RELATED SERVICE END;"];
    int emptyCount = msgCounts[""];
    int finishedCount = msgCounts["Finished service:"];
    int jcbCount = msgCounts["JCB END;"];

    boost::posix_time::ptime epoch(boost::gregorian::date(1970, 1, 1));

    std::string tmp;

    for (TStrMSetCItr itr = transString.begin(); itr != transString.end(); ++itr)
    {
        tmp += *itr + "|";
    }
    
    std::cout << "INSERT INTO trans VALUES (" << 
            (begin->first - epoch).total_milliseconds() << "," << 
            (end->first - epoch).total_milliseconds() << "," << 
            (end->first - begin->first).total_milliseconds() << "," <<
            wa5_v94_36400Duration.total_milliseconds() << "," << 
            wa5_v94_36403Duration.total_milliseconds() << "," << 
            servletDuration.total_milliseconds() << "," << 
            actionDuration.total_milliseconds() << "," << 
            methodDuration.total_milliseconds() << "," << 
            relatedServiceDuration.total_milliseconds() << "," << 
            serviceDuration.total_milliseconds() << "," << 
            emptyDuration.total_milliseconds() << "," << 
            finishedDuration.total_milliseconds() << "," << 
            jcbDuration.total_milliseconds() << "," <<
            servletCount << "," << 
            actionCount << "," << 
            methodCount << "," << 
            relatedServiceCount << "," << 
            serviceCount << "," << 
            emptyCount << "," << 
            finishedCount << "," << 
            jcbCount << "," <<
            "\"" << reqId << "\"" << ","
            "\"" << tmp << "\"" <<
            ");" << std::endl;

/*
    LOG_WARN(serviceCount << " " << tmp);

    LOG_INFO(
            "TIME," << 
            (begin->first - epoch).total_milliseconds() << "," << 
            (end->first - epoch).total_milliseconds() << "," << 
            (end->first - begin->first).total_milliseconds() << "," <<
            wa5_v94_36400Duration.total_milliseconds() << "," << 
            wa5_v94_36403Duration.total_milliseconds() << "," << 
            servletDuration.total_milliseconds() << "," << 
            actionDuration.total_milliseconds() << "," << 
            methodDuration.total_milliseconds() << "," << 
            relatedServiceDuration.total_milliseconds() << "," << 
            serviceDuration.total_milliseconds() << "," << 
            emptyDuration.total_milliseconds() << "," << 
            finishedDuration.total_milliseconds() << "," << 
            jcbDuration.total_milliseconds() << "," <<
            servletCount << "," << 
            actionCount << "," << 
            methodCount << "," << 
            relatedServiceCount << "," << 
            serviceCount << "," << 
            emptyCount << "," << 
            finishedCount << "," << 
            jcbCount);
*/
}

bool CXmlTransaction::messageChain(const std::string &name, std::string &values) const
{
    values.clear();

    for (TTimeXmlOperationMMapCItr itr = m_Operations.begin(); itr != m_Operations.end(); ++itr)
    {
        std::string value;

        if (itr->second.getValue(name, value) == false)
        {
            LOG_ERROR("Can not get " << name);
            return false;
        }

        values += value;
        values += "|";  // add separator
    }

    return true;
}





}
}
