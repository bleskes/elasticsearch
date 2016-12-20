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
#include <api/CDataProcessor.h>

#include <core/CLogger.h>

namespace prelert
{
namespace api
{

CDataProcessor::CDataProcessor(void)
{
}

CDataProcessor::~CDataProcessor(void)
{
    // Most compilers put the vtable in the object file containing the
    // definition of the first non-inlined virtual function, so DON'T move this
    // empty definition to the header file!
}

std::string CDataProcessor::debugPrintRecord(const TStrVec &fieldNames,
                                             const TStrStrUMap &dataRowFields)
{
    if (fieldNames.empty())
    {
        return "<EMPTY RECORD>";
    }

    std::string result;

    // We want to print the field names on one line, followed by the field
    // values on the next line in the SAME ORDER as the field names (container
    // order will NOT be the same)

    for (TStrVecCItr fieldNameIter = fieldNames.begin();
         fieldNameIter != fieldNames.end();
         ++fieldNameIter)
    {
        if (fieldNameIter != fieldNames.begin())
        {
            result += ',';
        }
        result += *fieldNameIter;
    }

    result += core_t::LINE_ENDING;

    for (TStrVecCItr fieldNameIter = fieldNames.begin();
         fieldNameIter != fieldNames.end();
         ++fieldNameIter)
    {
        if (fieldNameIter != fieldNames.begin())
        {
            result += ',';
        }
        TStrStrUMapCItr fieldValueIter = dataRowFields.find(*fieldNameIter);
        if (fieldValueIter == dataRowFields.end())
        {
            result += "<MISSING>";
        }
        else
        {
            result += fieldValueIter->second;
        }
    }

    return result;
}

bool CDataProcessor::periodicPersistState(core::CDataAdder &/*persister*/)
{
    // No-op
    return true;
}

void CDataProcessor::restCredentials(const std::string &/*mgmtUri*/,
                                     const std::string &/*sessionKey*/)
{
}


}
}

