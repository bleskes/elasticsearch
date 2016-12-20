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
#include <core/CDetachedProcessSpawner.h>

#include <core/CLogger.h>
#include <core/CShellArgQuoter.h>
#include <core/CWindowsError.h>
#include <core/WindowsSafe.h>


namespace prelert
{
namespace core
{


CDetachedProcessSpawner::CDetachedProcessSpawner(const TStrVec &permittedProcessPaths)
    : m_PermittedProcessPaths(permittedProcessPaths)
{
}

CDetachedProcessSpawner::~CDetachedProcessSpawner(void)
{
}

bool CDetachedProcessSpawner::spawn(const std::string &processPath, const TStrVec &args)
{
    if (std::find(m_PermittedProcessPaths.begin(),
                  m_PermittedProcessPaths.end(),
                  processPath) == m_PermittedProcessPaths.end())
    {
        LOG_ERROR("Spawning process '" << processPath << "' is not permitted");
        return false;
    }

    bool processPathHasExeExt(processPath.length() > 4 &&
                              processPath.compare(processPath.length() - 4, 4, ".exe") == 0);

    // Windows takes command lines as a single string
    std::string cmdLine(CShellArgQuoter::quote(processPath));
    for (size_t index = 0; index < args.size(); ++index)
    {
        cmdLine += ' ';
        cmdLine += CShellArgQuoter::quote(args[index]);
    }

    STARTUPINFO startupInfo;
    ::memset(&startupInfo, 0, sizeof(STARTUPINFO));
    startupInfo.cb = sizeof(STARTUPINFO);

    PROCESS_INFORMATION processInformation;
    ::memset(&processInformation, 0, sizeof(PROCESS_INFORMATION));

    if (CreateProcess((processPathHasExeExt ? processPath : processPath + ".exe").c_str(),
                      const_cast<char *>(cmdLine.c_str()),
                      0,
                      0,
                      FALSE,
                      CREATE_NEW_PROCESS_GROUP | DETACHED_PROCESS,
                      0,
                      0,
                      &startupInfo,
                      &processInformation) == FALSE)
    {
        LOG_ERROR("Failed to spawn '" << processPath << "': " << CWindowsError());
        return false;
    }

    LOG_DEBUG("Spawned '" << processPath << "' with PID " <<
              GetProcessId(processInformation.hProcess));

    CloseHandle(processInformation.hProcess);
    CloseHandle(processInformation.hThread);

    return true;
}


}
}

