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
#ifndef INCLUDED_ml_devbin_CTimeConverter_h
#define INCLUDED_ml_devbin_CTimeConverter_h

#include <boost/date_time/posix_time/posix_time.hpp>

#include <string>
#include <sstream>


namespace ml
{
namespace devbin
{


//! \brief
//! A time conversion wrapper
//!
//! DESCRIPTION:\n
//! A time conversion wrapper
//!
//! IMPLEMENTATION DECISIONS:\n
//! Uses boost - currently no specific time zone support in this class
//! and boost's time zone handling needs to be further investigated
//!
class CTimeConverter
{
    public:
        //! Construct one with a format based on boost::date_time::time_facet format specifiers
        //! e.g. %Y-%b-%d %H:%M:%S%F
        CTimeConverter(const std::string &format);

        //! Get posix_time for an input string
        //! 2005-Oct-15 13:12:11.123
        bool getTime(const std::string &, boost::posix_time::ptime &);

    private:
        std::stringstream m_Stream;
};


}
}

#endif // INCLUDED_ml_devbin_CTimeConverter_h
