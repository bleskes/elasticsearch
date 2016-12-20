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
#ifndef INCLUDED_prelert_devbin_CXmlTransaction_h
#define INCLUDED_prelert_devbin_CXmlTransaction_h

#include <core/CXmlNodeWithChildren.h>

#include "CXmlOperation.h"

namespace prelert
{
namespace devbin
{


//! \brief
//! A representation of a transaction
//!
//! DESCRIPTION:\n
//! A representation of a transaction
//!
//! IMPLEMENTATION DECISIONS:\n
//! HARD-CODED FROM CREDIT SUISSE DATA FOR NOW
//!
class CXmlTransaction
{
    public:
        //! Create this from XML
        CXmlTransaction(void);

        //! Add an operation
        void addXmlOperation(const boost::posix_time::ptime &, const CXmlOperation &);

        //! Get the chain of messages from the transaction
        bool messageChain(const std::string &, std::string &) const;

        //! Debug print
        void debug(void) const;

    private:
        typedef std::multimap<boost::posix_time::ptime, CXmlOperation> TTimeXmlOperationMMap;
        typedef TTimeXmlOperationMMap::const_iterator                  TTimeXmlOperationMMapCItr;
        typedef TTimeXmlOperationMMap::const_reverse_iterator          TTimeXmlOperationMMapCRItr;

        TTimeXmlOperationMMap m_Operations;
};


}
}

#endif // INCLUDED_prelert_devbin_CXmlTransaction_h
