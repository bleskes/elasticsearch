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
#include "CMockDataProcessor.h"

#include <core/CLogger.h>

#include <api/COutputHandler.h>


CMockDataProcessor::CMockDataProcessor(prelert::api::COutputHandler &outputHandler)
    : m_OutputHandler(outputHandler),
      m_NumRecordsHandled(0),
      m_WriteFieldNames(true)
{
}

void CMockDataProcessor::newOutputStream(void)
{
    m_OutputHandler.newOutputStream();
}

bool CMockDataProcessor::handleSettings(const TStrStrUMap &settings)
{
    // Pass on the settings in case we're chained
    m_WriteFieldNames = true;
    return m_OutputHandler.settings(settings);
}

bool CMockDataProcessor::handleRecord(bool isDryRun,
                                      const TStrVec &fieldNames,
                                      const TStrStrUMap &dataRowFields)
{
    // First time through we output the field names
    if (m_WriteFieldNames)
    {
        if (m_OutputHandler.fieldNames(fieldNames) == false)
        {
            LOG_ERROR("Unable to set field names for output:\n" <<
                      this->debugPrintRecord(fieldNames, dataRowFields));
            return false;
        }
        m_WriteFieldNames = false;
    }

    if (m_OutputHandler.writeRow(isDryRun, dataRowFields, m_FieldOverrides) == false)
    {
        LOG_ERROR("Unable to write output");
        return false;
    }

    if (!isDryRun)
    {
        ++m_NumRecordsHandled;
    }

    return true;
}

void CMockDataProcessor::finalise(void)
{
}

bool CMockDataProcessor::restoreState(prelert::core::CDataSearcher &restoreSearcher,
                                      prelert::core_t::TTime &completeToTime)
{
    // Pass on the request in case we're chained
    if (m_OutputHandler.restoreState(restoreSearcher,
                                     completeToTime) == false)
    {
        return false;
    }

    return true;
}

bool CMockDataProcessor::persistState(prelert::core::CDataAdder &persister)
{
    // Pass on the request in case we're chained
    if (m_OutputHandler.persistState(persister) == false)
    {
        return false;
    }

    return true;
}

uint64_t CMockDataProcessor::numRecordsHandled(void) const
{
    return m_NumRecordsHandled;
}

prelert::api::COutputHandler &CMockDataProcessor::outputHandler(void)
{
    return m_OutputHandler;
}

