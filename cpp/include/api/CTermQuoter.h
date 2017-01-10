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
#ifndef INCLUDED_ml_api_CTermQuoter_h
#define INCLUDED_ml_api_CTermQuoter_h

#include <core/CNonInstantiatable.h>

#include <api/ImportExport.h>

#include <string>


namespace ml
{
namespace api
{


//! \brief
//! Class to quote and escape a term or name
//!
//! DESCRIPTION:\n
//! Encapsulates the knowledge of how to quote a Splunk search
//! term or name.
//!
//! Can also be used for other cases where quoting is required
//! that adheres to Splunk rules.
//!
//! Splunk requires that terms be quoted using double quotes
//! and names be quoted using double quotes in some places and
//! single quotes in other places!
//!
//! The escape character is always a backslash.  Both quotes (of
//! the type being used for quoting) and backslashes within the
//! terms/names are escaped using backslashes.
//!
//! IMPLEMENTATION DECISIONS:\n
//! This is a static class with no state.
//!
//! For information about which characters need escaping in Splunk, refer to:
//! http://docs.splunk.com/Documentation/Splunk/latest/SearchReference/search#Quotes_and_escaping_characters
//!
class API_EXPORT CTermQuoter : private core::CNonInstantiatable
{
    public:
        //! String containing all the characters that need escaping
        //! when using double quotes
        static const std::string DOUBLE_QUOTE_ESCAPABLES;

        //! String containing all the characters that need escaping
        //! when using single quotes
        static const std::string SINGLE_QUOTE_ESCAPABLES;

        //! Search term quote character
        static const char        DOUBLE_QUOTE;

        //! Eval/where name quote character
        static const char        SINGLE_QUOTE;

        //! Search term escape character
        static const char        ESCAPE;

    public:
        //! Quote a search term using double quotes
        static std::string doubleQuote(const std::string &toQuote,
                                       bool unconditional = false);

        //! Quote an eval/where name using single quotes
        static std::string singleQuote(const std::string &toQuote,
                                       bool unconditional = false);

        //! Quote using specified characters
        static std::string quote(const std::string &escapeables,
                                 char quote,
                                 char escape,
                                 const std::string &toQuote,
                                 bool unconditional = false);
};


}
}

#endif // INCLUDED_ml_api_CTermQuoter_h

