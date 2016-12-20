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
#include <core/CLicenseValidator.h>

#include <core/CProcess.h>
#include <core/CStringUtils.h>

#include <stdint.h>


namespace prelert
{
namespace core
{


bool CLicenseValidator::validate(const std::string &license)
{
    // This must match the number used in the ProcessCtrl class in the Java code
    static const int64_t VALIDATION_NUMBER(926213);

    int64_t val(0);
    if (CStringUtils::stringToTypeSilent(license, val) == false)
    {
        return false;
    }

    // PID is unsigned 32 bit on Windows and signed 32 bit on *nix, so use
    // int64_t as that covers both ranges
    return (val % VALIDATION_NUMBER) == (int64_t(CProcess::instance().parentId()) % VALIDATION_NUMBER);
}


}
}

