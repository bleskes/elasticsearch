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
#ifndef INCLUDED_prelert_devbin_CXmlOperation_h
#define INCLUDED_prelert_devbin_CXmlOperation_h

#include <core/CXmlNodeWithChildren.h>

#include <boost/date_time/posix_time/posix_time.hpp>


namespace prelert
{
namespace devbin
{


//! \brief
//! An individual operation in a transaction
//!
//! DESCRIPTION:\n
//! An individual operation in a transaction
//!
//! IMPLEMENTATION DECISIONS:\n
//! Stores everything for now
//! TODO - refactor this as storage/logic is mixed...
//!
class CXmlOperation
{
    public:
        //! Create this from XML
        CXmlOperation(const core::CXmlNodeWithChildren::TXmlNodeWithChildrenP &dataPtr);

        //! Get a node value by node name in XML - Returns first match
        template<typename T>
        bool getValue(const std::string &name, T &value) const
        {
            return _getValue(m_DataPtr, name, value);
        }

        //! Set values
        void setTime(const boost::posix_time::ptime &);
        void setDuration(const boost::posix_time::time_duration &);
        void setMsg(const std::string &);
        void setVm(const std::string &);

        //! Get values
        const boost::posix_time::ptime         &getTime(void) const;
        const boost::posix_time::time_duration &getDuration(void) const;
        const std::string                      &getMsg(void) const;
        const std::string                      &getVm(void) const;

    private:
        static bool _getValue(const core::CXmlNodeWithChildren::TXmlNodeWithChildrenP &, const std::string &, int);
        static bool _getValue(const core::CXmlNodeWithChildren::TXmlNodeWithChildrenP &, const std::string &, std::string &);
        
        core::CXmlNodeWithChildren::TXmlNodeWithChildrenP m_DataPtr;

        //! Some specific CS values
        boost::posix_time::ptime         m_Time;
        boost::posix_time::time_duration m_Duration;
        std::string                      m_Msg;
        std::string                      m_Vm;
};


}
}

#endif // INCLUDED_prelert_devbin_CXmlOperation_h
