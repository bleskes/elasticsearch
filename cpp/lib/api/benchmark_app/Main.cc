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
//! \brief
//! A benchmarker for the Splunk app processor
//!
//! DESCRIPTION:\n
//! A benchmarker for the Splunk app processor
//!
//! Data from Splunk dumped in JSON format (via prelertdump)
//! is feed into the Prelert analysis methods.  The results
//! of the Prelert analysis is then compared with manually
//! determined categories defined by mutally exclusive
//! regular expressions.
//!
//! IMPLEMENTATION DECISIONS:\n
//! JSON used as interchange format between python and C++
//!
#include <core/CLogger.h>
#include <core/CRegexFilter.h>
#include <core/CStringUtils.h>
#include <core/CTextFileWatcher.h>
#include <core/CWordDictionary.h>

#include <api/CBenchMarker.h>
#include <api/CDataTyper.h>
#include <api/CTokenListDataTyper.h>

#include <rapidjson/document.h>

#include <boost/bind.hpp>
#include <boost/scoped_array.hpp>

#include <fstream>
#include <iterator>
#include <string>

#include <stdlib.h>
#include <string.h>

using namespace prelert;

typedef boost::scoped_array<char> TScopedCharArray;


// Utility
template<typename TYPE>
bool getValue(const api::CDataTyper::TStrStrUMap &map,
              const std::string &key,
              TYPE &value)
{
    api::CDataTyper::TStrStrUMapCItr itr = map.find(key);
    if (itr == map.end())
    {
        LOG_ERROR("Cannot find " << key);
        return false;
    }

    if (core::CStringUtils::stringToType(itr->second, value) == false)
    {
        LOG_ERROR("Inconsistent type for " << key << ' ' << itr->second);
        return false;
    }

    return true;
}

// Type a record
bool typeRecord(const api::CDataTyper::TStrStrUMap &record,
                api::CDataTyper &dataTyper,
                api::CBenchMarker &benchMarker,
                const core::CRegexFilter &categorizationFilter)
{
    std::string raw;
    if (getValue(record, "_raw", raw) == false)
    {
        LOG_ERROR("Inconsistent data - missing _raw");
        return false;
    }
    size_t origRawLen(raw.length());

    size_t timestartpos(0);
    size_t timeendpos(0);
    if (getValue(record, "timestartpos", timestartpos) == false ||
        getValue(record, "timeendpos", timeendpos) == false)
    {
        // TODO - this often indicates a Splunk issue parsing the timestamps
        // for now these records are passed through as is
        LOG_ERROR("Inconsistent timestamps in data " << raw);
    }
    else
    {
        // Trim string by removing time
        //
        // <L_MSG MN="up02212" PID="w010_managed1" TID="asyncDelivery20" DT="2011/10/21 18:30:22:714" PT="ERROR" AP="wts" DN="" SN="" SR="com.ntrs.wts.session.ejb.FxCoverSessionBean">javax.ejb.FinderException - findFxCover([]): null</L_MSG>
        // to
        // <L_MSG MN="up02212" PID="w010_managed1" TID="asyncDelivery20" DT="" PT="ERROR" AP="wts" DN="" SN="" SR="com.ntrs.wts.session.ejb.FxCoverSessionBean">javax.ejb.FinderException - findFxCover([]): null</L_MSG>

        raw.erase(timestartpos, timeendpos - timestartpos);
    }

    //LOG_DEBUG(raw);

    int type = -1;
    if (categorizationFilter.empty())
    {
        type = dataTyper.computeType(false, record, raw, origRawLen);
    }
    else
    {
        std::string filtered = categorizationFilter.apply(raw);
        type = dataTyper.computeType(false, record, filtered, origRawLen);
    }
    benchMarker.addResult(raw, type);

    return true;
}

// Process a line from a file
bool getLine(const std::string &line,
             TScopedCharArray &workSpace,
             size_t &workSpaceSize,
             api::CDataTyper::TStrStrUMap &record,
             api::CDataTyper &dataTyper,
             api::CBenchMarker &benchMarker,
             const core::CRegexFilter &categorizationFilter,
             size_t &numResults)
{
    // Copy the constant string to the working array - Rapidjson will change
    // the working array contents as it parses the data in-situ
    size_t requiredSize(line.length() + 1);
    if (workSpaceSize < requiredSize)
    {
        workSpace.reset(new char[requiredSize]);
        workSpaceSize = requiredSize;
    }
    ::memcpy(workSpace.get(), line.c_str(), requiredSize);

    // Parse JSON string using Rapidjson
    rapidjson::Document document;
    if (document.ParseInsitu<0>(&workSpace[0]).HasParseError())
    {
        LOG_ERROR("JSON parse error: " << document.GetParseError());
        return false;
    }

    if (!document.IsObject())
    {
        LOG_ERROR("Expected JSON document to contain an object: " << line);
        return false;
    }

    // Convert to a hash map
    if (!record.empty())
    {
        // Assume that if the previous record had the same number of fields as
        // the current one then the field names are identical
        size_t memberCount(std::distance(document.MemberBegin(),
                                         document.MemberEnd()));
        if (memberCount != record.size())
        {
            record.clear();
        }
    }

    std::string name;
    for (rapidjson::Value::MemberIterator iter = document.MemberBegin();
         iter != document.MemberEnd();
         ++iter)
    {
        name.assign(iter->name.GetString(), iter->name.GetStringLength());
        std::string &value = record[name];
        switch (iter->value.GetType())
        {
            case rapidjson::kNullType:
                break;
            case rapidjson::kFalseType:
                value = "false";
                break;
            case rapidjson::kTrueType:
                value = "true";
                break;
            case rapidjson::kObjectType:
            case rapidjson::kArrayType:
                LOG_WARN("Complex JSON types not supported in this program");
                break;
            case rapidjson::kStringType:
                value.assign(iter->value.GetString(), iter->value.GetStringLength());
                break;
            case rapidjson::kNumberType:
                value = core::CStringUtils::typeToString(iter->value.GetDouble());
                break;
        }
    }

    typeRecord(record, dataTyper, benchMarker, categorizationFilter);

    ++numResults;
    if (numResults % 1000 == 0)
    {
        LOG_DEBUG("Read " << numResults << " records");
    }

    return true;
}

bool configureRegexFilter(const std::string &filename, core::CRegexFilter &filter)
{
    std::ifstream ifs(filename.c_str());
    if (!ifs.is_open())
    {
        return false;
    }

    typedef core::CRegexFilter::TStrVec TStrVec;
    TStrVec regexVec;
    std::string line;
    while (std::getline(ifs, line))
    {
        regexVec.push_back(line);
    }
    return filter.configure(regexVec);;
}

int main(int argc, char **argv)
{
    // Supply file name in format
/*
{"DN": "", "D_MATUR": "", "C_SET_CURR": "", "A_DAY_ACCR_INT": "", "F_OPEN_IND": "", "I_TRADE_SEQ": "", "timestartpos": "66", "R_TRD_EXCH": "", "A_TRADE_PRC": "", "C_EV_TYPE": "", "date_zone": "local", "TID": "asyncDelivery21", "I_TRD_BKR": "", "date_minute": "30", "_raw": "<L_MSG MN=\"up02212\" PID=\"w010_managed1\" TID=\"asyncDelivery21\" DT=\"2011/10/21 18:30:22:767\" PT=\"ERROR\" AP=\"wts\" DN=\"\" SN=\"\" SR=\"com.ntrs.wts.session.ejb.FxCoverSessionBean\">javax.ejb.FinderException - findFxCover([]): null</L_MSG>", "F_NET_SET_IND": "", "A_BUY_AMT": "", "A_TX": "", "DT": "2011/10/21 18:30:22:767", "D_FRST_INST_PERD": "", "D_SET": "", "index": "main", "date_mday": "21", "I_CLRG_BKR": "", "PT": "ERROR", "A_DAY_CNT_FRACT": "", "PID": "w010_managed1", "linecount": "1", "R_TRD_INT": "", "A_ACCR_INT": "", "splunk_server": "Stephen-Dodsons-MacBook-Air.local", "source": "/Users/steve/Desktop/Log File Analysis/ntrs/Logs_Serv1.txt", "C_SELL_CURR": "", "version": "", "F_CLOSE_IND": "", "date_year": "2011", "D_TRADE": "", "date_month": "october", "C_INV_STRAT": "", "I_TRD_CUSTD": "", "date_hour": "18", "A_JSE_LEVY": "", "date_second": "22", "date_wday": "friday", "A_NOM": "", "A_LEVY": "", "A_OTHER_CHRG": "", "C_STAT": "", "I_ACCT": "", "AP": "wts", "host": "Stephen-Dodsons-MacBook-Air.local", "C_BUY_CURR": "", "I_TRD_INV_MGR": "", "_kv": "1", "A_CLR_FEE": "", "A_CNTRT_STAMP": "", "sourcetype": "ntrs", "_si": "Stephen-Dodsons-MacBook-Air.local\nmain", "C_TRAN_TYPE": "", "A_EXCH_FEE": "", "C_INSTM_TYPE": "", "punct": "<_=\"\"_=\"\"_=\"\"_=\"//_:::\"_=\"\"_=\"\"_=\"\"_=\"\"_=\".....\">.", "A_LOCAL_CHRG": "", "SR": "com.ntrs.wts.session.ejb.FxCoverSessionBean", "MN": "up02212", "eventtype": "", "_subsecond": ".767", "C_BLIM_REF": "", "_time": "1319218222.767", "_sourcetype": "ntrs", "SN": "", "timeendpos": "89", "A_COMSN": "", "_indextime": "1337935935", "_serial": "0", "A_SELL_AMT": "", "_cd": "0:432708"}
{"DN": "", "D_MATUR": "", "C_SET_CURR": "", "A_DAY_ACCR_INT": "", "F_OPEN_IND": "", "I_TRADE_SEQ": "", "timestartpos": "66", "R_TRD_EXCH": "", "A_TRADE_PRC": "", "C_EV_TYPE": "", "date_zone": "local", "TID": "asyncDelivery21", "I_TRD_BKR": "", "date_minute": "30", "_raw": "<L_MSG MN=\"up02212\" PID=\"w010_managed1\" TID=\"asyncDelivery21\" DT=\"2011/10/21 18:30:22:764\" PT=\"ERROR\" AP=\"wts\" DN=\"\" SN=\"\" SR=\"com.ntrs.wts.session.ejb.FxCoverSessionBean\">javax.ejb.FinderException - findFxCover([]): null</L_MSG>", "F_NET_SET_IND": "", "A_BUY_AMT": "", "A_TX": "", "DT": "2011/10/21 18:30:22:764", "D_FRST_INST_PERD": "", "D_SET": "", "index": "main", "date_mday": "21", "I_CLRG_BKR": "", "PT": "ERROR", "A_DAY_CNT_FRACT": "", "PID": "w010_managed1", "linecount": "1", "R_TRD_INT": "", "A_ACCR_INT": "", "splunk_server": "Stephen-Dodsons-MacBook-Air.local", "source": "/Users/steve/Desktop/Log File Analysis/ntrs/Logs_Serv1.txt", "C_SELL_CURR": "", "version": "", "F_CLOSE_IND": "", "date_year": "2011", "D_TRADE": "", "date_month": "october", "C_INV_STRAT": "", "I_TRD_CUSTD": "", "date_hour": "18", "A_JSE_LEVY": "", "date_second": "22", "date_wday": "friday", "A_NOM": "", "A_LEVY": "", "A_OTHER_CHRG": "", "C_STAT": "", "I_ACCT": "", "AP": "wts", "host": "Stephen-Dodsons-MacBook-Air.local", "C_BUY_CURR": "", "I_TRD_INV_MGR": "", "_kv": "1", "A_CLR_FEE": "", "A_CNTRT_STAMP": "", "sourcetype": "ntrs", "_si": "Stephen-Dodsons-MacBook-Air.local\nmain", "C_TRAN_TYPE": "", "A_EXCH_FEE": "", "C_INSTM_TYPE": "", "punct": "<_=\"\"_=\"\"_=\"\"_=\"//_:::\"_=\"\"_=\"\"_=\"\"_=\"\"_=\".....\">.", "A_LOCAL_CHRG": "", "SR": "com.ntrs.wts.session.ejb.FxCoverSessionBean", "MN": "up02212", "eventtype": "", "_subsecond": ".764", "C_BLIM_REF": "", "_time": "1319218222.764", "_sourcetype": "ntrs", "SN": "", "timeendpos": "89", "A_COMSN": "", "_indextime": "1337935935", "_serial": "1", "A_SELL_AMT": "", "_cd": "0:432699"}
*/
    if (argc < 3 || argc > 4)
    {
        LOG_FATAL("Usage " << argv[0] << ": regex_filename json_filename [filters_filename]");
        return EXIT_FAILURE;
    }

    api::CBenchMarker benchMarker;
    if (benchMarker.init(argv[1]) == false)
    {
        LOG_FATAL("Unable to init regexes from " << argv[1]);
        return EXIT_FAILURE;
    }

    core::CTextFileWatcher file;

    // Use new line as delimiter - this seems to be ok with break in _si field
    if (file.init(argv[2], "\r?\n", core::CTextFileWatcher::E_Start) == false)
    {
        LOG_FATAL("Unable to read " << argv[2]);
        return EXIT_FAILURE;
    }

    core::CRegexFilter categorizationFilter;
    if (argc == 4)
    {
        if (configureRegexFilter(argv[3], categorizationFilter) == false)
        {
            LOG_FATAL("Unable to configure categorization filter from " << argv[3]);
            return EXIT_FAILURE;
        }
    }

    // A type of token list data typer that DOESN'T exclude fields from its
    // analysis
    typedef api::CTokenListDataTyper<true,  // Warping
                                     true,  // Underscores
                                     true,  // Dots
                                     true,  // Dashes
                                     true,  // Ignore leading digit
                                     true,  // Ignore hex
                                     true,  // Ignore date words
                                     false, // Ignore field names
                                     2,     // Min dictionary word length
                                     core::CWordDictionary::TWeightVerbs5Other2>
            TTokenListDataTyperKeepsFields;

    TTokenListDataTyperKeepsFields::TTokenListReverseSearchCreatorIntfCPtr noReverseSearchCreator;

    // Used to deduce types
    TTokenListDataTyperKeepsFields dataTyper(noReverseSearchCreator, 0.7);

    TScopedCharArray             workSpace;
    size_t                       workSpaceSize(0);
    api::CDataTyper::TStrStrUMap record;
    size_t                       numResults(0);
    std::string                  remainder;

    LOG_DEBUG("Reading data from file " << argv[1]);

    if (file.readAllLines(boost::bind(getLine,
                                      _1,
                                      boost::ref(workSpace),
                                      boost::ref(workSpaceSize),
                                      boost::ref(record),
                                      boost::ref(dataTyper),
                                      boost::ref(benchMarker),
                                      boost::cref(categorizationFilter),
                                      boost::ref(numResults)), remainder) == false)
    {
        LOG_FATAL("Unable to read " << argv[1]);
        return EXIT_FAILURE;
    }

    if (numResults % 100 != 0)
    {
        LOG_DEBUG("Read " << numResults << " records");
    }

    benchMarker.dumpResults();

    return EXIT_SUCCESS;
}

