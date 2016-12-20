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

#include <algorithm>

#include <errno.h>
#include <fcntl.h>
#include <signal.h>
#include <spawn.h>
#include <stdlib.h>
#include <string.h>
#include <sys/resource.h>
#include <sys/stat.h>
#include <unistd.h>

// environ is a global variable from the C runtime library
extern char **environ;


namespace
{

//! Attempt to close all file descriptors except the standard ones.  The
//! standard file descriptors will be reopened on /dev/null in the spawned
//! process.  Returns false and sets errno if the actions cannot be initialised
//! at all, but other errors are ignored.
bool setupFileActions(posix_spawn_file_actions_t *fileActions)
{
    if (::posix_spawn_file_actions_init(fileActions) != 0)
    {
        return false;
    }

    struct rlimit rlim;
    ::memset(&rlim, 0, sizeof(struct rlimit));
    if (::getrlimit(RLIMIT_NOFILE, &rlim) != 0)
    {
        rlim.rlim_cur = 36; // POSIX default
    }

    // Assume a limit on file descriptors that is greater than a million really
    // means "unlimited".  In this case we would ideally pick up the compiled-in
    // limit of the OS, but this would be another OS dependent piece of code and
    // in reality it's unlikely that any file descriptors above a million will
    // be open at the time this function is called.
    int maxFd(rlim.rlim_cur > 1000000 ? 1000000 : static_cast<int>(rlim.rlim_cur));
    for (int fd = 0; fd <= maxFd; ++fd)
    {
        if (fd == STDIN_FILENO)
        {
            ::posix_spawn_file_actions_addopen(fileActions, fd, "/dev/null", O_RDONLY, S_IRUSR);
        }
        else if (fd == STDOUT_FILENO || fd == STDERR_FILENO)
        {
            ::posix_spawn_file_actions_addopen(fileActions, fd, "/dev/null", O_WRONLY, S_IWUSR);
        }
        else
        {
            // Close other files that are open.  There is a race condition here,
            // in that files could be opened or closed between this code running
            // and the posix_spawn() function being called.  However, this would
            // violate the restrictions stated in the contract detailed in the
            // Doxygen description of this class.
            if (::fcntl(fd, F_GETFL) != -1)
            {
                ::posix_spawn_file_actions_addclose(fileActions, fd);
            }
        }
    }

    return true;
}

}

namespace prelert
{
namespace core
{


CDetachedProcessSpawner::CDetachedProcessSpawner(const TStrVec &permittedProcessPaths)
    : m_PermittedProcessPaths(permittedProcessPaths)
{
    struct sigaction sa;
    sigemptyset(&sa.sa_mask);
    sa.sa_handler = SIG_IGN;
    sa.sa_flags = SA_RESTART | SA_NOCLDSTOP | SA_NOCLDWAIT;
    if (::sigaction(SIGCHLD, &sa, 0) == -1)
    {
        LOG_ERROR("Failed to remove the need to wait for SIGCHLD");
    }
}

CDetachedProcessSpawner::~CDetachedProcessSpawner(void)
{
    struct sigaction sa;
    sigemptyset(&sa.sa_mask);
    sa.sa_handler = SIG_DFL;
    sa.sa_flags = SA_RESTART;
    if (::sigaction(SIGCHLD, &sa, 0) == -1)
    {
        LOG_ERROR("Failed to revert default SIGCHLD handler");
    }
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

    if (::access(processPath.c_str(), X_OK) != 0)
    {
        LOG_ERROR("Cannot execute '" << processPath << "': " <<
                  ::strerror(errno));
        return false;
    }

    typedef std::vector<char *> TCharPVec;
    // Size of argv is two bigger than the number of arguments because:
    // 1) We add the program name at the beginning
    // 2) The list of arguments must be terminated by a NULL pointer
    TCharPVec argv;
    argv.reserve(args.size() + 2);

    // These const_casts may cause const data to get modified BUT only in the
    // child post-fork, so this won't corrupt parent process data
    argv.push_back(const_cast<char *>(processPath.c_str()));
    for (size_t index = 0; index < args.size(); ++index)
    {
        argv.push_back(const_cast<char *>(args[index].c_str()));
    }
    argv.push_back(static_cast<char *>(0));

    posix_spawn_file_actions_t fileActions;
    if (setupFileActions(&fileActions) == false)
    {
        LOG_ERROR("Failed to set up file actions prior to spawn of '" <<
                  processPath << "': " << ::strerror(errno));
        return false;
    }
    posix_spawnattr_t spawnAttributes;
    if (::posix_spawnattr_init(&spawnAttributes) != 0)
    {
        LOG_ERROR("Failed to set up spawn attributes prior to spawn of '" <<
                  processPath << "': " << ::strerror(errno));
        return false;
    }
    ::posix_spawnattr_setflags(&spawnAttributes, POSIX_SPAWN_SETPGROUP);

    pid_t childPid(0);
    int err(::posix_spawn(&childPid,
                          processPath.c_str(),
                          &fileActions,
                          &spawnAttributes,
                          &argv[0],
                          environ));

    ::posix_spawn_file_actions_destroy(&fileActions);
    ::posix_spawnattr_destroy(&spawnAttributes);

    if (err != 0)
    {
        LOG_ERROR("Failed to spawn '" << processPath << "': " <<
                  ::strerror(err));
        return false;
    }

    LOG_DEBUG("Spawned '" << processPath << "' with PID " << childPid);

    return true;
}


}
}

