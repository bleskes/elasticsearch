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
#ifndef INCLUDED_prelert_core_CLicenseValidator_h
#define INCLUDED_prelert_core_CLicenseValidator_h

#include <core/CNonInstantiatable.h>
#include <core/ImportExport.h>

#include <string>


namespace prelert
{
namespace core
{

//! \brief
//! Class to check the --licenseValidation argument.
//!
//! DESCRIPTION:\n
//! Checks whether the value of the --licenseValidation argument
//! is acceptable.
//!
//! IMPLEMENTATION DECISIONS:\n
//! To avoid shipping any sort of encryption library this functionality
//! is incredibly simple and won't be very hard to hack.  It's intended
//! to make it obvious to somebody who is trying to subvert the Java-side
//! licensing by directly running the C++ processes that they're doing
//! this.
//!
//! The check is simply that the "license" must be convertible to a number
//! and that number must be equal to the parent process ID modulo a large
//! magic number that is also included in the Java code.  This means that
//! those in the know can run programs at a shell prompt using:
//!
//! ./my_program --licenseValidation=$$
//!
class CORE_EXPORT CLicenseValidator : private CNonInstantiatable
{
    public:
        //! Validate the supplied "license".
        static bool validate(const std::string &license);
};


}
}

#endif // INCLUDED_prelert_core_CLicenseValidator_h

