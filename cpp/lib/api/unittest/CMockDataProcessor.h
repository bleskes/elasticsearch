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
#ifndef INCLUDED_prelert_api_CMockDataProcessor_h
#define INCLUDED_prelert_api_CMockDataProcessor_h

#include <core/CoreTypes.h>

#include <api/CDataProcessor.h>

#include <string>

#include <stdint.h>


namespace prelert
{
namespace api
{
class COutputHandler;
}
}

//! \brief
//! Mock object for unit tests
//!
//! DESCRIPTION:\n
//! Mock object for testing the OutputChainer class.
//!
//! IMPLEMENTATION DECISIONS:\n
//! Only the minimal set of required functions are implemented.
//!
class CMockDataProcessor : public prelert::api::CDataProcessor
{
    public:
        CMockDataProcessor(prelert::api::COutputHandler &outputHandler);

        //! We're going to be writing to a new output stream
        virtual void newOutputStream(void);

        virtual bool handleSettings(const TStrStrUMap &settings);

        virtual bool handleRecord(bool isDryRun,
                                  const TStrVec &fieldNames,
                                  const TStrStrUMap &dataRowFields);

        virtual void finalise(void);

        //! Restore previously saved state
        virtual bool restoreState(prelert::core::CDataSearcher &restoreSearcher,
                                  prelert::core_t::TTime &completeToTime);

        //! Persist current state
        virtual bool persistState(prelert::core::CDataAdder &persister);

        //! How many records did we handle?
        virtual uint64_t numRecordsHandled(void) const;

        //! Access the output handler
        virtual prelert::api::COutputHandler &outputHandler(void);

    private:
        prelert::api::COutputHandler &m_OutputHandler;

        //! Empty field overrides
        TStrStrUMap                  m_FieldOverrides;

        uint64_t                     m_NumRecordsHandled;

        bool                         m_WriteFieldNames;
};


#endif // INCLUDED_prelert_api_CMockDataProcessor_h

