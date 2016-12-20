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
#include <core/CProgName.h>

#include <stdlib.h>
#include <unistd.h>


namespace prelert
{
namespace core
{


std::string CProgName::progName(void)
{
    const char *progPath(::getexecname());
    if (progPath == 0)
    {
        return std::string();
    }
    std::string progName(progPath);
    size_t lastSlash(progName.rfind('/'));
    if (lastSlash != std::string::npos)
    {
        progName.erase(0, lastSlash + 1);
    }
    return progName;
}

std::string CProgName::progDir(void)
{
    static const size_t BUFFER_SIZE(2048);
    std::string path(BUFFER_SIZE, '\0');
    ssize_t len(::readlink("/proc/self/path/a.out", &path[0], BUFFER_SIZE));
    if (len == -1)
    {
        return std::string();
    }
    size_t lastSlash(path.rfind('/', static_cast<size_t>(len)));
    if (lastSlash == std::string::npos)
    {
        return std::string();
    }
    path.resize(lastSlash);
    return path;
}



}
}

