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
#include "CTimeConverter.h"

#include <core/CLogger.h>


namespace ml
{
namespace devbin
{


CTimeConverter::CTimeConverter(const std::string &format)
{
    boost::posix_time::time_input_facet *input_facet = new boost::posix_time::time_input_facet;

    input_facet->format(format.c_str());

    // TODO validate no memory leak on this
    m_Stream.imbue(std::locale(m_Stream.getloc(), input_facet));

    // Turn on exception handling for this stream
    m_Stream.exceptions(std::ios_base::failbit);
}

bool CTimeConverter::getTime(const std::string &input, boost::posix_time::ptime &ptime)
{
    try
    {
        m_Stream.str(input);

        m_Stream >> ptime;
    }
    catch (std::exception &e)
    {
        LOG_ERROR("Can not parse " << input << " " << e.what());
        return false;
    }

    return true;
}


}
}
