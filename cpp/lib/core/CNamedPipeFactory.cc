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
#include <core/CNamedPipeFactory.h>

#include <core/CLogger.h>
#include <core/COsFileFuncs.h>

#include <boost/iostreams/device/file_descriptor.hpp>
#include <boost/iostreams/stream.hpp>

#include <errno.h>
#include <paths.h>
#include <signal.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>


namespace
{

//! fclose() doesn't check for NULL pointers, so wrap it for use as a shared_ptr
//! deleter
void safeFClose(FILE *file)
{
    if (file != 0)
    {
        ::fclose(file);
    }
}

//! Ignore SIGPIPE, as we don't ever want a process (or even a different thread
//! in the same process) at the other end of one of our named pipes to abruptly
//! terminate our processes.  Instead we should handle remote reader death by
//! gracefully reacting to write failures.
bool ignoreSigPipe(void)
{
    struct sigaction sa;
    sigemptyset(&sa.sa_mask);
    sa.sa_handler = SIG_IGN;
    sa.sa_flags = 0;
    // Error reporting is deferred, as the logger won't be logging to the right
    // place when this function runs
    return ::sigaction(SIGPIPE, &sa, 0) == 0;
}

const bool SIGPIPE_IGNORED(ignoreSigPipe());

}

namespace ml
{
namespace core
{


// Initialise static
const char CNamedPipeFactory::TEST_CHAR('\n');


CNamedPipeFactory::TIStreamP CNamedPipeFactory::openPipeStreamRead(const std::string &fileName)
{
    TPipeHandle fd = CNamedPipeFactory::initPipeHandle(fileName, false);
    if (fd == -1)
    {
        return TIStreamP();
    }
    typedef boost::iostreams::stream<boost::iostreams::file_descriptor_source> TFileDescriptorSourceStream;
    return TIStreamP(new TFileDescriptorSourceStream(
            boost::iostreams::file_descriptor_source(fd, boost::iostreams::close_handle)));
}

CNamedPipeFactory::TOStreamP CNamedPipeFactory::openPipeStreamWrite(const std::string &fileName)
{
    TPipeHandle fd = CNamedPipeFactory::initPipeHandle(fileName, true);
    if (fd == -1)
    {
        return TOStreamP();
    }
    typedef boost::iostreams::stream<boost::iostreams::file_descriptor_sink> TFileDescriptorSinkStream;
    return TOStreamP(new TFileDescriptorSinkStream(
            boost::iostreams::file_descriptor_sink(fd, boost::iostreams::close_handle)));
}

CNamedPipeFactory::TFileP CNamedPipeFactory::openPipeFileRead(const std::string &fileName)
{
    TPipeHandle fd = CNamedPipeFactory::initPipeHandle(fileName, false);
    if (fd == -1)
    {
        return TFileP();
    }
    return TFileP(::fdopen(fd, "r"), safeFClose);
}

CNamedPipeFactory::TFileP CNamedPipeFactory::openPipeFileWrite(const std::string &fileName)
{
    TPipeHandle fd = CNamedPipeFactory::initPipeHandle(fileName, true);
    if (fd == -1)
    {
        return TFileP();
    }
    return TFileP(::fdopen(fd, "w"), safeFClose);
}

bool CNamedPipeFactory::isNamedPipe(const std::string &fileName)
{
    COsFileFuncs::TStat statbuf;
    if (COsFileFuncs::stat(fileName.c_str(), &statbuf) < 0)
    {
        return false;
    }

    return (statbuf.st_mode & S_IFIFO) != 0;
}

std::string CNamedPipeFactory::defaultPath(void)
{
    // In production this needs to match the setting of java.io.tmpdir.  We rely
    // on the JVM that spawns our controller daemon setting TMPDIR in the
    // environment of the spawned process.  For unit testing and adhoc testing
    // $TMPDIR is generally set on Mac OS X (to something like
    // /var/folders/k5/5sqcdlps5sg3cvlp783gcz740000h0/T/) and not set on other
    // platforms.
    const char *tmpDir(::getenv("TMPDIR"));

    // Make sure path ends with a slash so it's ready to have a file name
    // appended.  (_PATH_VARTMP already has this on all platforms I've seen,
    // but a user-defined $TMPDIR might not.)
    std::string path((tmpDir == 0) ? _PATH_VARTMP : tmpDir);
    if (path[path.length() - 1] != '/')
    {
        path += '/';
    }
    return path;
}

CNamedPipeFactory::TPipeHandle CNamedPipeFactory::initPipeHandle(const std::string &fileName, bool forWrite)
{
    if (!SIGPIPE_IGNORED)
    {
        LOG_WARN("Failed to ignore SIGPIPE - this process will not terminate "
                 "gracefully if a process it is writing to via a named pipe dies");
    }

    bool madeFifo(false);

    // If the name already exists, ensure it refers to a named pipe
    COsFileFuncs::TStat statbuf;
    if (COsFileFuncs::stat(fileName.c_str(), &statbuf) == 0)
    {
        if ((statbuf.st_mode & S_IFIFO) == 0)
        {
            LOG_ERROR("Unable to create named pipe " << fileName << " - a file "
                      "of this name already exists, but it is not a FIFO");
            return -1;
        }
        if ((statbuf.st_mode & (S_IRGRP | S_IWGRP | S_IXGRP | S_IROTH | S_IWOTH | S_IXOTH)) != 0)
        {
            LOG_ERROR("Will not use pre-existing named pipe " << fileName <<
                      " - it has permissions that are too open");
            return -1;
        }
    }
    else
    {
        // The file didn't exist, so create a new FIFO for it, with permissions
        // for the current user only
        if (::mkfifo(fileName.c_str(), S_IRUSR | S_IWUSR) == -1)
        {
            LOG_ERROR("Unable to create named pipe " << fileName <<
                      ": " << ::strerror(errno));
            return -1;
        }
        madeFifo = true;
    }

    // The open call here will block if there is no other connection to the
    // named pipe
    int fd = COsFileFuncs::open(fileName.c_str(),
                                forWrite ? COsFileFuncs::WRONLY : COsFileFuncs::RDONLY);
    if (fd == -1)
    {
        LOG_ERROR("Unable to open named pipe " << fileName <<
                  (forWrite ? " for writing: " : " for reading: ") << ::strerror(errno));
    }
    else
    {
        // Write a test character to the pipe - this is really only necessary on
        // Windows, but doing it on *nix too will mean the inability of the Java
        // code to tolerate the test character will be discovered sooner.
        if (forWrite &&
            COsFileFuncs::write(fd, &TEST_CHAR, sizeof(TEST_CHAR)) <= 0)
        {
            LOG_ERROR("Unable to test named pipe " << fileName << ": " <<
                      ::strerror(errno));
            COsFileFuncs::close(fd);
            fd = -1;
        }
    }

    // Since the open call above blocked until the other end of the pipe
    // was connected or failed, we can unlink the file name from the directory
    // structure at this point.  This avoids the need to unlink it later.  A
    // deleted file should still be accessible on *nix to the file handles that
    // already had it open when it was deleted.
    if (madeFifo)
    {
        ::unlink(fileName.c_str());
    }

    return fd;
}


}
}

