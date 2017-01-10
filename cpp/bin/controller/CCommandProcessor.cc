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
#include "CCommandProcessor.h"

#include <core/CDetachedProcessSpawner.h>
#include <core/CLogger.h>
#include <core/CStringUtils.h>

#include <algorithm>
#include <istream>


namespace
{
const std::string TAB(1, '\t');
const std::string EMPTY_STRING;
}

namespace ml
{
namespace controller
{


// Initialise statics
const std::string CCommandProcessor::START("start");


CCommandProcessor::CCommandProcessor(const TStrVec &permittedProcessPaths)
    : m_Spawner(permittedProcessPaths)
{
}

void CCommandProcessor::processCommands(std::istream &stream)
{
    std::string command;
    while (std::getline(stream, command))
    {
        if (!command.empty())
        {
            this->handleCommand(command);
        }
    }
}

bool CCommandProcessor::handleCommand(const std::string &command)
{
    // Command lines must be tab-separated
    TStrVec     tokens;
    std::string remainder;
    core::CStringUtils::tokenise(TAB, command, tokens, remainder);
    if (!remainder.empty())
    {
        tokens.push_back(remainder);
    }

    // Multiple consecutive tabs might have caused empty tokens
    tokens.erase(std::remove(tokens.begin(), tokens.end(), EMPTY_STRING),
                 tokens.end());

    if (tokens.empty())
    {
        LOG_DEBUG("Ignoring empty command");
        return false;
    }

    // Split into verb and other tokens
    std::string verb(tokens[0]);
    tokens.erase(tokens.begin());

    if (verb == START)
    {
        return this->handleStart(tokens);
    }

    LOG_ERROR("Did not understand verb '" << verb << '\'');
    return false;
}

bool CCommandProcessor::handleStart(TStrVec &tokens)
{
    std::string processPath;
    processPath.swap(tokens[0]);
    tokens.erase(tokens.begin());

    if (m_Spawner.spawn(processPath, tokens) == false)
    {
        LOG_ERROR("Failed to start process '" << processPath << '\'');
        return false;
    }

    return true;
}


}
}

