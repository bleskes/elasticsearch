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
#ifndef INCLUDED_prelert_core_CLocalTimeR_h
#define INCLUDED_prelert_core_CLocalTimeR_h

#include <core/CNonInstantiatable.h>
#include <core/ImportExport.h>

#include <time.h>


namespace prelert
{
namespace core
{


//! \brief
//! Portable wrapper for the localtime_r() function.
//!
//! DESCRIPTION:\n
//! Portable wrapper for the localtime_r() function.
//!
//! IMPLEMENTATION DECISIONS:\n
//! This has been broken into a class of its own because Windows has a
//! localtime_s() function with slightly different semantics to Unix's
//! localtime_r().
//!
class CORE_EXPORT CLocalTimeR : private CNonInstantiatable
{
    public:
        static struct tm *localTimeR(const time_t *clock,
                                     struct tm *result);
};


}
}

#endif // INCLUDED_prelert_core_CLocalTimeR_h

