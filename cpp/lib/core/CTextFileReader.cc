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
#include <core/CTextFileReader.h>

#include <core/CLogger.h>
#include <core/COsFileFuncs.h>

#include <errno.h>
#include <string.h>


namespace ml
{
namespace core
{


CTextFileReader::CTextFileReader(bool translateCrLf)
    : m_TranslateCrLf(translateCrLf)
{
}

bool CTextFileReader::readFileToText(const std::string &fileName,
                                     std::string &text)
{
    int oFlags(COsFileFuncs::RDONLY);
    oFlags |= (m_TranslateCrLf ? COsFileFuncs::TEXT : COsFileFuncs::BINARY);
    int fd(COsFileFuncs::open(fileName.c_str(), oFlags));
    if (fd == -1)
    {
        LOG_ERROR("Opening " << fileName << ": " << ::strerror(errno));
        return false;
    }

    static const size_t SIZE(16384);
    char buf[SIZE] = { '\0' };

    COsFileFuncs::TSignedSize bytesRead(0);
    while ((bytesRead = COsFileFuncs::read(fd, buf, SIZE)) > 0)
    {
        text.append(buf, bytesRead);
    }

    bool success(true);

    if (bytesRead < 0)
    {
        LOG_ERROR("Error reading from " << fileName << ": " << ::strerror(errno));

        success = false;
    }

    if (COsFileFuncs::close(fd) == -1)
    {
        LOG_WARN("Closing " << fileName << ": " << ::strerror(errno));
    }

    return success;
}


}
}

