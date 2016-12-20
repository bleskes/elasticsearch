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
//! \brief
//! Test the idea of frequency counting fields in delimited files
//!
//! DESCRIPTION:\n
//! It may be possible to categorise records in delimited files
//! by frequency counting the different fields, and using those
//! with relatively low variability to define the categories.
//! This program will help to test whether this strategy works.
//!
//! IMPLEMENTATION DECISIONS:\n
//! Quick and dirty experiment.
//!
#include <core/CDelimiter.h>
#include <core/CLogger.h>
#include <core/CStringUtils.h>
#include <core/CTextFileWatcher.h>

#include <boost/bind.hpp>

#include <set>
#include <sstream>
#include <string>
#include <vector>

#include <stdlib.h>
#include <string.h>

using namespace prelert;

namespace
{

typedef std::set<std::string>      TStrSet;
typedef TStrSet::const_iterator    TStrSetCItr;
typedef std::vector<TStrSet>       TStrSetVec;
typedef TStrSetVec::const_iterator TStrSetVecCItr;

const size_t MAX_DISTINCT_VALS(20);

}


// Process a line from a file
bool getLine(const std::string &line,
             const core::CDelimiter &delim,
             TStrSetVec &fieldValues,
             size_t &numResults)
{
    // Tokenise the line
    core::CStringUtils::TStrVec tokens;
    if (!fieldValues.empty())
    {
        tokens.reserve(fieldValues.size());
    }
    std::string remainder;
    delim.tokenise(line, tokens, remainder);
    tokens.push_back(remainder);

    if (tokens.size() > fieldValues.size())
    {
        fieldValues.resize(tokens.size());
    }

    for (size_t index = 0; index < tokens.size(); ++index)
    {
        TStrSet &thisIndexVals = fieldValues[index];

        // If this field has relatively few distinct values,
        // store this one
        if (thisIndexVals.size() <= MAX_DISTINCT_VALS)
        {
            thisIndexVals.insert(tokens[index]);
        }
    }

    ++numResults;
    if (numResults % 100 == 0)
    {
        LOG_DEBUG("Read " << numResults << " records");
    }

    return true;
}

int main(int argc, char **argv)
{
    // Input file should have one record per line consisting of delimited fields

    if (argc != 3 && argc != 5)
    {
        LOG_FATAL("Usage " << argv[0] <<
                  ": filename field-delimiter [ quote-character escape-character ]");
        return EXIT_FAILURE;
    }

    std::string delimStr(argv[2]);
    if (delimStr.length() < 1 || delimStr.length() > 5)
    {
        LOG_FATAL("Field delimiter should contain between 1 and 5 characters");
        return EXIT_FAILURE;
    }

    core::CDelimiter delim(delimStr);

    if (argc == 5)
    {
        if (::strlen(argv[3]) != 1)
        {
            LOG_FATAL("Quote character must be a single character");
            return EXIT_FAILURE;
        }
        if (::strlen(argv[4]) != 1)
        {
            LOG_FATAL("Escape character must be a single character");
            return EXIT_FAILURE;
        }

        delim.quote(argv[3][0], argv[4][0]);
    }

    core::CTextFileWatcher file;

    // Use new line as delimiter - this seems to be ok with break in _si field
    if (file.init(argv[1], "\r?\n", core::CTextFileWatcher::E_Start) == false)
    {
        LOG_FATAL("Unable to read " << argv[1]);
        return EXIT_FAILURE;
    }

    TStrSetVec  fieldValues;
    size_t      numResults(0);
    std::string remainder;

    LOG_DEBUG("Reading data from file " << argv[1]);

    if (file.readAllLines(boost::bind(getLine,
                                      _1,
                                      boost::ref(delim),
                                      boost::ref(fieldValues),
                                      boost::ref(numResults)), remainder) == false)
    {
        LOG_FATAL("Unable to read " << argv[1]);
        return EXIT_FAILURE;
    }

    if (numResults % 100 != 0)
    {
        LOG_DEBUG("Read " << numResults << " records");
    }

    LOG_DEBUG("Maximum number of fields in any record: " << fieldValues.size());

    std::ostringstream strm;
    strm << "Results:\n";

    size_t count(0);
    for (TStrSetVecCItr fieldIter = fieldValues.begin();
         fieldIter != fieldValues.end();
         ++fieldIter)
    {
        strm << "Field " << ++count << " has ";
        if (fieldIter->size() > MAX_DISTINCT_VALS)
        {
            strm << "too many distinct values to be useful for categorisation\n";
        }
        else
        {
            if (fieldIter->size() == 1)
            {
                strm << "1 distinct value:\n";
            }
            else
            {
                strm << fieldIter->size() << " distinct values:\n";
            }

            for (TStrSetCItr valIter = fieldIter->begin();
                 valIter != fieldIter->end();
                 ++valIter)
            {
                strm << '\t' << *valIter << '\n';
            }
        }
    }

    LOG_DEBUG(strm.str());

    return EXIT_SUCCESS;
}

