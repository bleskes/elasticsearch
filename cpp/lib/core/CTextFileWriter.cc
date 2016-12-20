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
#include <core/CTextFileWriter.h>

#include <core/CLogger.h>
#include <core/CoreTypes.h>
#include <core/COsFileFuncs.h>

#include <errno.h>
#include <fcntl.h>
#include <string.h>


namespace prelert
{
namespace core
{


CTextFileWriter::CTextFileWriter(bool translateCrLf, bool allowRename)
    : m_TranslateCrLf(translateCrLf),
      m_AllowRename(allowRename),
      m_Fd(-1)
{
}

CTextFileWriter::~CTextFileWriter(void)
{
    this->close();
}

bool CTextFileWriter::init(const std::string &fileName, EPosition pos)
{
    if (m_Fd != -1)
    {
        LOG_ERROR("Trying to initialise file watcher twice");
        return false;
    }

    int oFlags(COsFileFuncs::WRONLY);
    switch (pos)
    {
        case E_Start:
            oFlags |= COsFileFuncs::CREAT | COsFileFuncs::TRUNC;
            break;
        case E_End:
            oFlags |= COsFileFuncs::CREAT | COsFileFuncs::APPEND;
            break;
    }
    oFlags |= (m_TranslateCrLf ? COsFileFuncs::TEXT : COsFileFuncs::BINARY);
    if (m_AllowRename)
    {
       oFlags |= COsFileFuncs::RENAMABLE;
    }

    // Let the umask restrict who can read/write this file
    static const COsFileFuncs::TMode PRE_UMASK_FILE_MODE(0666);
    int fd(COsFileFuncs::open(fileName.c_str(), oFlags, PRE_UMASK_FILE_MODE));
    if (fd < 0)
    {
        LOG_ERROR("Could not open file [" << fileName << "] " << ::strerror(errno));
        return false;
    }

    // Success, set all
    m_FileName = fileName;
    m_Fd = fd;

    return true;
}

bool CTextFileWriter::writeLine(const std::string &str)
{
    // If the file layer is translating LF to CRLF then we shouldn't do it here
    if (m_TranslateCrLf)
    {
        return this->write(str + '\n');
    }

    // The file layer isn't translating LF to CRLF, so we should use the
    // standard platform line ending
    return this->write(str + core_t::LINE_ENDING);
}

bool CTextFileWriter::write(const std::string &str)
{
    if (m_Fd == -1)
    {
        LOG_ERROR("Cannot write to an uninitialised file " << m_FileName);
        return false;
    }

    if (COsFileFuncs::write(m_Fd, str.c_str(), str.size()) < 0)
    {
        LOG_ERROR("Unable to write to " << m_FileName << ' ' << ::strerror(errno));
        return false;
    }

    return true;
}

const std::string &CTextFileWriter::fileName(void) const
{
    return m_FileName;
}

void CTextFileWriter::close(void)
{
    if (m_Fd != -1)
    {
        if (COsFileFuncs::close(m_Fd) != 0)
        {
            LOG_ERROR("Error closing file " << m_FileName << ' ' << ::strerror(errno));
        }
        m_Fd = -1;
    }
}

bool CTextFileWriter::isOpen(void) const
{
    if (m_Fd == -1)
    {
        return false;
    }

    return true;
}


}
}

