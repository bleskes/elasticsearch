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
#include "CTransactionStore.h"

#include <core/CLogger.h>
#include <core/CRapidXmlParser.h>

#include "CXmlOperation.h"


namespace prelert
{
namespace devbin
{


// ts=2012-04-22 15:00:05.513 MEST
// %Y-%m-%d %H:%M:%S%F
CTransactionStore::CTransactionStore(void) : 
            m_TimeConverter("%Y-%m-%d %H:%M:%S%F"),
            m_Count(0)
{
    // Parse msg
    // SERVLET END; Duration=518ms;ResponseState=OK;
    // Duration\s*=\s*(\d+)
    if (m_DurationRegex.init(".*Duration\\s*=\\s*(\\d+).*") == false)
    {
        LOG_ERROR("Error parsing duration regex");
    }

    // Parse msg
    // SERVLET END; Duration=518ms;ResponseState=OK;
    // (.*)Duration
    if (m_MsgRegex.init("(.+END;|Finished service:|.*).*Duration.*") == false)
    {
        LOG_ERROR("Error parsing msg regex");
    }
}

bool CTransactionStore::addRecord(const std::string &record)
{
    core::CRapidXmlParser parser;

    if (parser.parseString(record) == false)
    {
        LOG_DEBUG("Can not parse " << record);
        return false;
    }

    core::CXmlNodeWithChildren::TXmlNodeWithChildrenP rootNodePtr;

    if (parser.toNodeHierarchy(m_MemoryPool, rootNodePtr) == false)
    {
        LOG_DEBUG("Can not parse " << record);
        return false;
    }

    CXmlOperation operation(rootNodePtr);

    // Get fields
    std::string reqId;
    std::string ts;
    std::string msg;
    std::string vm;

    // TODO - make more efficient as involves iterations across data
    if (operation.getValue("reqid", reqId) == false)
    {
        LOG_DEBUG("Can not get reqid " << record);
        return false;
    }
    if (operation.getValue("ts", ts) == false)
    {
        LOG_DEBUG("Can not get ts " << record);
        return false;
    }
    if (operation.getValue("msg", msg) == false)
    {
        LOG_DEBUG("Can not get msg " << record);
        return false;
    }
    if (operation.getValue("vm", vm) == false)
    {
        LOG_DEBUG("Can not get vm " << record);
        return false;
    }

    // Parse msg
    // SERVLET END; Duration=518ms;ResponseState=OK;
    // Duration\s*=\s*(\d+)
    core::CRegex::TStrVec durationTokens;

    if (m_DurationRegex.tokenise(msg, durationTokens) == false)
    {
        LOG_ERROR("Can not parse msg " << msg);
        return false;
    }

    if (durationTokens.size() != 1)
    {
        LOG_ERROR("Can not parse msg " << msg);
        return false;
    }

    int duration;

    if (core::CStringUtils::stringToType(durationTokens[0], duration) == false)
    {
        LOG_ERROR("Can not parse msg " << durationTokens[0]);
        return false;
    }

    // SERVLET END; Duration=518ms;ResponseState=OK;
    // (.*)Duration
    core::CRegex::TStrVec msgTokens;

    if (m_MsgRegex.tokenise(msg, msgTokens) == false)
    {
        LOG_ERROR("Can not parse msg " << msg);
        return false;
    }

    if (msgTokens.size() != 1)
    {
        LOG_ERROR("Can not parse msg " << msg);
        return false;
    }

    // Parse time
    // ts=2012-04-22 15:00:05.513 MEST
    // %Y-%m-%d %H:%M:%S%F
    boost::posix_time::ptime ptime;

    if (m_TimeConverter.getTime(ts, ptime) == false)
    {
        LOG_ERROR("Can not parse ts " << ts);
        return false;
    }

    operation.setTime(ptime); 
    operation.setDuration(boost::posix_time::milliseconds(duration)); 
    operation.setMsg(msgTokens[0]);
    operation.setVm(vm);

    //LOG_INFO("," << msgTokens[0] << "," << vm);

    // Is there a transaction with this reqid already?
    TStrXmlTransactionMapItr itr = m_Transactions.find(reqId);
    if (itr == m_Transactions.end())
    {
        CXmlTransaction txn;

        itr = m_Transactions.insert(TStrXmlTransactionMap::value_type(reqId, txn)).first;
    }

    itr->second.addXmlOperation(ptime, operation);

    ++m_Count;

    if (m_Count % 1000 == 0)
    {
        LOG_DEBUG("Received " << m_Count << " records");
    }

    return true;
}

void CTransactionStore::analyse(void)
{
    typedef std::map<std::string, int> TStrIntMap;
    typedef TStrIntMap::iterator       TStrIntMapItr;
    typedef TStrIntMap::const_iterator TStrIntMapCItr;

    TStrIntMap results;

    LOG_DEBUG("Collating results");

    int count(0);

    for (TStrXmlTransactionMapCItr itr = m_Transactions.begin(); itr != m_Transactions.end(); ++itr)
    {
/*
        std::string signature;

        itr->second.messageChain("msg", signature); 

        TStrIntMapItr resultsItr = results.find(signature);
        if (resultsItr == results.end())
        {
            resultsItr = results.insert(TStrIntMap::value_type(signature, 0)).first;
        }

        resultsItr->second++;
*/

        itr->second.debug();

        if (count % 1000 == 0)
        {
            LOG_DEBUG("Processed " << count << " records");
        }

        ++count;
    }

    LOG_DEBUG("Done");

    for (TStrIntMapCItr itr = results.begin(); itr != results.end(); ++itr)
    {
        LOG_DEBUG(itr->first << " " << itr->second);
    }
}


}
}
