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
#include <core/CTextFileWatcher.h>

#include <core/CLogger.h>
#include <core/CStringUtils.h>

#include <algorithm>

#include <errno.h>
#include <string.h>


namespace prelert
{
namespace core
{


// Initialise the read buffer size to 16k
const size_t CTextFileWatcher::READ_BUFFER_SIZE(16384);

const size_t CTextFileWatcher::DEFAULT_RENAMED_FILE_ATTEMPTS(10);


CTextFileWatcher::CTextFileWatcher(size_t renamedFileRetries)
    : m_Fd(-1),
      m_Offset(0),
      m_Inode(0),
      m_RenamedFileRetries(renamedFileRetries),
      m_RenamedFileRetriesRemaining(renamedFileRetries)
{
}

CTextFileWatcher::~CTextFileWatcher(void)
{
    this->destroy();
}

bool CTextFileWatcher::init(const std::string &fileName,
                            const std::string &delimiter,
                            EPosition pos)
{
    return this->init(fileName, CDelimiter(delimiter), pos);
}

bool CTextFileWatcher::init(const std::string &fileName,
                            const CDelimiter &delimiter,
                            EPosition pos)
{
    if (m_Fd >= 0)
    {
        LOG_ERROR("Trying to initialise file watcher twice");
        this->destroy();
    }

    // Since we lseek() within the file, we MUST open it in binary mode.
    // Also, since we're only watching, we tolerate another process renaming the
    // file.
    int oFlags(COsFileFuncs::RDONLY | COsFileFuncs::BINARY | COsFileFuncs::RENAMABLE);
    int fd(COsFileFuncs::open(fileName.c_str(), oFlags));
    if (fd < 0)
    {
        LOG_ERROR("Could not open file [" << fileName << "] " <<
                  ::strerror(errno));
        return false;
    }

    COsFileFuncs::TOffset offset(0);

    switch (pos)
    {
        case E_Start:
            break;
        case E_End:
            if (CTextFileWatcher::statSize(fd, offset) == false)
            {
                LOG_ERROR("Could not open file [" << fileName << ']');
                return false;
            }
            break;
    }

    // Get the inode
    ino_t inode;

    if (CTextFileWatcher::statInode(fd, inode) == false)
    {
        LOG_ERROR("Could not open file [" << fileName << ']');
        return false;
    }

    // Success, set all
    m_FileName = fileName;
    m_Delimiter = delimiter;
    m_Fd = fd;
    m_Offset = offset;
    m_Inode = inode;
    m_LastRead.clear();

    return true;
}

bool CTextFileWatcher::statSize(int fd, COsFileFuncs::TOffset &offset)
{
    if (fd < 0)
    {
        LOG_ERROR("Cannot stat uninitialised file");
        return false;
    }

    COsFileFuncs::TStat statBuf;

    if (COsFileFuncs::fstat(fd, &statBuf) < 0)
    {
        LOG_ERROR("Cannot stat file " << ::strerror(errno));
        return false;
    }

    offset = statBuf.st_size;

    return true;
}

bool CTextFileWatcher::statInode(int fd, ino_t &inode)
{
    if (fd < 0)
    {
        LOG_ERROR("Cannot stat uninitialised file");
        return false;
    }

    COsFileFuncs::TStat statBuf;

    if (COsFileFuncs::fstat(fd, &statBuf) < 0)
    {
        LOG_ERROR("Cannot stat file " << ::strerror(errno));
        return false;
    }

    inode = statBuf.st_ino;

    return true;
}

void CTextFileWatcher::recheckLastRead(const TFunc &f, std::string &lastRead)
{
    // Avoid processing in the simple (and likely) case of nothing left over
    if (lastRead.size() > 0)
    {
        CStringUtils::TStrVec tokens;
        std::string           remainder;
        std::string           exampleDelimiter;

        m_Delimiter.tokenise(lastRead,
                             true,
                             tokens,
                             exampleDelimiter,
                             remainder);

        // If tokens is empty, we can avoid the assignment to lastRead
        if (!tokens.empty())
        {
            for (CStringUtils::TStrVecCItr itr = tokens.begin();
                 itr != tokens.end();
                 ++itr)
            {
                // Given that this is just a recheck, we'd expect it to find 0 or 1 tokens.
                // If there are more, we'd expect all but the last to have been
                // found in the previous parsing.
                if (itr != tokens.begin())
                {
                    LOG_WARN("Recheck of last read found more than 1 token");
                }

                if (f(*itr) == false)
                {
                    LOG_ERROR("Visitor throw error whilst rechecking last read");
                }
            }

            lastRead = remainder;
            this->updateExampleDelimiter(exampleDelimiter);
        }
    }
}

bool CTextFileWatcher::changes(const TFunc &f)
{
    COsFileFuncs::TOffset newOffset(0);

    if (CTextFileWatcher::statSize(m_Fd, newOffset) == false)
    {
        LOG_ERROR("Unable to stat file " << m_FileName);
        return false;
    }

    if (newOffset == m_Offset)
    {
        // If the offset hasn't changed, we first re-check any leftover text
        // from the last read.  Then we check we have got to the end of the
        // file, then we check to see if this file has moved.
        // For example, if this is a log file it may have moved.
        // In this case, we attempt to reopen the file, compare inodes and close
        // existing file

        this->recheckLastRead(f, m_LastRead);

        char c;

        COsFileFuncs::TSignedSize bytes(COsFileFuncs::read(m_Fd, &c, sizeof(c)));
        if (bytes == 0)
        {
            if (this->checkForMovedFile() == false)
            {
                LOG_ERROR("Error checking for rotated file " << m_FileName);
                return false;
            }
        }

        return true;
    }

    if (newOffset < m_Offset)
    {
        LOG_WARN("File has shrunk in size " << m_FileName << ' ' <<
                 newOffset << " < " << m_Offset);
        return false;
    }

    // Seek to the end of the last position
    if (COsFileFuncs::lseek(m_Fd, m_Offset, SEEK_SET) < 0)
    {
        LOG_WARN("Cannot seek file " << m_FileName << ' ' << ::strerror(errno));
        return false;
    }

    // Initialise read string with last read data
    std::string s(m_LastRead);
    char        buffer[READ_BUFFER_SIZE];

    // Now read lines
    for (;;)
    {
        COsFileFuncs::TSignedSize bytes(COsFileFuncs::read(m_Fd, buffer, READ_BUFFER_SIZE));
        //LOG_DEBUG(m_Offset << ' ' << bytes);
        if (bytes > 0)
        {
            s.append(buffer, bytes);

            CStringUtils::TStrVec tokens;
            std::string           remainder;
            std::string           exampleDelimiter;

            m_Delimiter.tokenise(s, false, tokens, exampleDelimiter, remainder);
            for (CStringUtils::TStrVecCItr itr = tokens.begin();
                 itr != tokens.end();
                 ++itr)
            {
                if (f(*itr) == false)
                {
                    // Let the caller do the logging (if appropriate)
                    return false;
                }
            }

            s = remainder;
            this->updateExampleDelimiter(exampleDelimiter);

            m_Offset += bytes;
        }
        else if (bytes == 0)
        {
            // EOF - store any trailing characters
            m_LastRead = s;
            break;
        }
        else if (bytes == -1)
        {
            LOG_ERROR("Unable to read file " << m_FileName << ' ' <<
                      ::strerror(errno));
            m_LastRead = s;
            break;
        }
    }

    return true;
}

bool CTextFileWatcher::checkForMovedFile(void)
{
    if (m_Fd < 0)
    {
        LOG_ERROR("Unable to check unopen file");
        return false;
    }

    // Try to open the file again
    int oFlags(COsFileFuncs::RDONLY | COsFileFuncs::BINARY | COsFileFuncs::RENAMABLE);
    int fd(COsFileFuncs::open(m_FileName.c_str(), oFlags));
    if (fd < 0)
    {
        if (m_RenamedFileRetriesRemaining == 0)
        {
            LOG_ERROR("Could not reopen file [" << m_FileName << "] " <<
                      ::strerror(errno));
            return false;
        }

        LOG_WARN("Could not reopen file [" << m_FileName << "] " <<
                 ::strerror(errno));
        LOG_WARN("Will retry " << m_RenamedFileRetriesRemaining << " more times");
        --m_RenamedFileRetriesRemaining;
        return true;
    }

    // Compare inodes
    ino_t inode;

    if (this->statInode(fd, inode) == false)
    {
        LOG_ERROR("Could not reopen file [" << m_FileName << ']');
        return false;
    }

    if (m_Inode == inode)
    {
        // File not moved
        // Close the temporary stream
        COsFileFuncs::close(fd);

        return true;
    }

    LOG_DEBUG("File " << m_FileName << " has moved");

    // File has moved
    COsFileFuncs::close(m_Fd);

    m_Fd = fd;
    m_Inode = inode;

    // Reset the offset to the beginning of the file
    m_Offset = 0;

    // Reset the retries for next time this happens
    m_RenamedFileRetriesRemaining = m_RenamedFileRetries;

    return true;
}

bool CTextFileWatcher::readAllLines(const TFunc &f, std::string &lastRead)
{
    if (COsFileFuncs::lseek(m_Fd, 0, SEEK_SET) < 0)
    {
        LOG_WARN("Cannot seek file " << m_FileName << ' ' << ::strerror(errno));
        return false;
    }

    // Now read lines
    std::string s;
    char        buffer[READ_BUFFER_SIZE];

    for (;;)
    {
        COsFileFuncs::TSignedSize bytes(COsFileFuncs::read(m_Fd, buffer, READ_BUFFER_SIZE));
        if (bytes > 0)
        {
            // Replace any NULL characters in the newly read data with spaces,
            // because NULLs will screw up the regular expression library
            std::replace(buffer, buffer + bytes, '\0', ' ');

            s.append(buffer, bytes);

            CStringUtils::TStrVec tokens;
            std::string           remainder;
            std::string           exampleDelimiter;

            m_Delimiter.tokenise(s, false, tokens, exampleDelimiter, remainder);
            for (CStringUtils::TStrVecCItr itr = tokens.begin();
                 itr != tokens.end();
                 ++itr)
            {
                if (f(*itr) == false)
                {
                    // Let the caller do the logging (if appropriate)
                    return false;
                }
            }

            s = remainder;
            this->updateExampleDelimiter(exampleDelimiter);
        }
        else if (bytes == 0)
        {
            lastRead = s;
            break;
        }
        else if (bytes == -1)
        {
            LOG_ERROR("Unable to read file " << m_FileName << ' ' <<
                      ::strerror(errno));
            lastRead = s;
            break;
        }
    }

    return true;
}

bool CTextFileWatcher::readFirstLine(const TFunc &f)
{
    if (COsFileFuncs::lseek(m_Fd, 0, SEEK_SET) < 0)
    {
        LOG_WARN("Cannot seek file " << m_FileName << ' ' << ::strerror(errno));
        return false;
    }

    // Now read a line
    std::string s;
    char        buffer[READ_BUFFER_SIZE];

    for (;;)
    {
        COsFileFuncs::TSignedSize bytes(COsFileFuncs::read(m_Fd, buffer, READ_BUFFER_SIZE));
        if (bytes > 0)
        {
            s.append(buffer, bytes);

            CStringUtils::TStrVec tokens;
            std::string           remainder;

            m_Delimiter.tokenise(s, false, tokens, remainder);
            if (!tokens.empty())
            {
                if (f(tokens.front()) == false)
                {
                    LOG_ERROR("Visitor throw error whilst reading first line");
                    return false;
                }

                // Stop after first line
                break;
            }

            s = remainder;
        }
        else if (bytes == 0)
        {
            // If we get here we've read to the end of the file without having
            // found a complete line, so set the offset to 0 so that next time
            // we check for changes we'll start at the beginning again.
            m_Offset = 0;
            break;
        }
        else if (bytes == -1)
        {
            LOG_ERROR("Unable to read file " << m_FileName << ' ' <<
                      ::strerror(errno));
            break;
        }
    }

    return true;
}

void CTextFileWatcher::destroy(void)
{
    if (m_Fd >= 0)
    {
        COsFileFuncs::close(m_Fd);
        m_Fd = -1;
    }

    m_Offset = 0;
    m_FileName = "";
}

const std::string &CTextFileWatcher::fileName(void) const
{
    return m_FileName;
}

const std::string &CTextFileWatcher::exampleDelimiter(void) const
{
    return m_ExampleDelimiter;
}

void CTextFileWatcher::updateExampleDelimiter(const std::string &exampleDelimiter)
{
    if (!exampleDelimiter.empty() && m_ExampleDelimiter.empty())
    {
        m_ExampleDelimiter = exampleDelimiter;
    }
}


}
}

