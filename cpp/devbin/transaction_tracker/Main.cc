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
#include <core/CLogger.h>
#include <core/CStringUtils.h>
#include <core/CTextFileWatcher.h>

#include <iostream>
#include <string>
#include <vector>

#include <boost/bind.hpp>

#include "CCmdLineParser.h"
#include "CTransactionStore.h"

using namespace prelert;

int main(int argc, char **argv)
{
    std::string fileName;

    if (devbin::CCmdLineParser::parse(argc, argv, fileName) == false)
    {
        return EXIT_FAILURE;
    }
    
    LOG_DEBUG("Starting");

    core::CTextFileWatcher reader;

    if (reader.init(fileName, "\n", core::CTextFileWatcher::E_Start) == false)
    {
        LOG_ABORT("Could not read file");
    }

    std::string remainder;
    
    devbin::CTransactionStore store;

    if (reader.readAllLines(boost::bind(&devbin::CTransactionStore::addRecord, &store, _1), remainder) == false)
    {
        LOG_ABORT("Could not read file");
    }

    // Analyse store
    store.analyse();
    

    LOG_DEBUG("Done");

    return 0;
}
