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
#ifndef INCLUDED_ml_devbin_CTransactionStore_h
#define INCLUDED_ml_devbin_CTransactionStore_h

#include <string>

#include <core/CRegex.h>
#include <core/CXmlNodeWithChildrenPool.h>

#include "CTimeConverter.h"
#include "CXmlTransaction.h"


namespace ml
{
namespace devbin
{


//! \brief
//!
//! DESCRIPTION:\n
//!
//! IMPLEMENTATION DECISIONS:\n
//!
class CTransactionStore
{
    public:
        CTransactionStore(void);

        //! Add a record
        bool addRecord(const std::string &);

        //! Analyse results
        void analyse(void);

    private:
        //! Memory pool for parsing XML
        core::CXmlNodeWithChildrenPool m_MemoryPool;

        //! Some converters
        core::CRegex                   m_DurationRegex;
        core::CRegex                   m_MsgRegex;
        CTimeConverter                 m_TimeConverter;

        //! The transactions
        typedef std::map<std::string, CXmlTransaction> TStrXmlTransactionMap;
        typedef TStrXmlTransactionMap::iterator        TStrXmlTransactionMapItr;
        typedef TStrXmlTransactionMap::const_iterator  TStrXmlTransactionMapCItr;

        TStrXmlTransactionMap          m_Transactions;

        //! Debug
        int                            m_Count;
};


}
}

#endif // INCLUDED_ml_devbin_CTransactionStore_h
