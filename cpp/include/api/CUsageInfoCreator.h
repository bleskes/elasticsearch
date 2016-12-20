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
#ifndef INCLUDED_prelert_api_CUsageInfoCreator_h
#define INCLUDED_prelert_api_CUsageInfoCreator_h

#include <core/CNonInstantiatable.h>
#include <core/CoreTypes.h>

#include <api/ImportExport.h>

#include <string>


namespace prelert
{
namespace api
{

//! \brief
//! Encapsulate creation of usage logging data.
//!
//! DESCRIPTION:\n
//! Returns information required for usage logging in the required
//! format.
//!
//! The information returned is:
//! 1) Product version number, e.g. 4.2.10
//! 2) Customer ID, e.g. dev@prelert.com
//! 3) Prelert platform name, e.g. linux64
//! 4) Detailed OS version, e.g. 2.6.32-220.el6.x86_64
//!
//! IMPLEMENTATION DECISIONS:\n
//! At present usage information can only be returned in a JSON
//! document, but other formats could be added in future if
//! required.
//!
//! The field names and values are identical to those used in
//! the Python/Javascript code of the Splunk app.  This is
//! important because a single receiving function on
//! www.prelert.com needs to be able to handle usage data from
//! all our products.
//!
class API_EXPORT CUsageInfoCreator : private core::CNonInstantiatable
{
    public:
        static const std::string PRODUCT_VER_NAME;
        static const std::string CUSTOMER_ID_NAME;
        static const std::string PLATFORM_NAME;
        static const std::string OS_VER_NAME;
        static const std::string EXPIRY_TIME_NAME;

    public:
        //! Return a JSON document containing the usage information
        static std::string asJson(const std::string &productVer,
                                  const std::string &customerId,
                                  core_t::TTime expiryTime);
};


}
}

#endif // INCLUDED_prelert_api_CUsageInfoCreator_h

