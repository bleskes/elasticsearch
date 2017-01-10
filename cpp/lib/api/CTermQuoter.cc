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
#include <api/CTermQuoter.h>

#include <ctype.h>


namespace ml
{
namespace api
{


// Initialise statics
const std::string CTermQuoter::DOUBLE_QUOTE_ESCAPABLES("\\\"");
const std::string CTermQuoter::SINGLE_QUOTE_ESCAPABLES("\\'");
const char        CTermQuoter::DOUBLE_QUOTE('"');
const char        CTermQuoter::SINGLE_QUOTE('\'');
const char        CTermQuoter::ESCAPE('\\');


std::string CTermQuoter::doubleQuote(const std::string &toQuote,
                                     bool unconditional)
{
    return CTermQuoter::quote(DOUBLE_QUOTE_ESCAPABLES,
                              DOUBLE_QUOTE,
                              ESCAPE,
                              toQuote,
                              unconditional);
}

std::string CTermQuoter::singleQuote(const std::string &toQuote,
                                     bool unconditional)
{
    return CTermQuoter::quote(SINGLE_QUOTE_ESCAPABLES,
                              SINGLE_QUOTE,
                              ESCAPE,
                              toQuote,
                              unconditional);
}

std::string CTermQuoter::quote(const std::string &escapeables,
                               char quote,
                               char escape,
                               const std::string &toQuote,
                               bool unconditional)
{
    if (toQuote.empty())
    {
        // The empty string needs quoting as a pair of quotes with nothing in
        // between
        return std::string(2, quote);
    }

    size_t nonAlnumUnderscoreCount(0);
    for (std::string::const_iterator iter = toQuote.begin();
         iter != toQuote.end();
         ++iter)
    {
        char current(*iter);
        if (!::isalnum(static_cast<unsigned char>(current)) && current != '_')
        {
            ++nonAlnumUnderscoreCount;
        }
    }

    if (!unconditional)
    {
        // If the string is purely alpha-numeric and/or underscores then return
        // it unaltered
        if (nonAlnumUnderscoreCount == 0)
        {
            return toQuote;
        }
    }

    // We'll quote this string
    std::string result;
    result.reserve(2 + toQuote.length() + nonAlnumUnderscoreCount);
    result += quote;

    for (std::string::const_iterator iter = toQuote.begin();
         iter != toQuote.end();
         ++iter)
    {
        char current(*iter);
        if (escapeables.find(current) != std::string::npos)
        {
            result += escape;
        }
        result += current;
    }

    result += quote;
    return result;
}


}
}

