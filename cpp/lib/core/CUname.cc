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
#include <core/CUname.h>

#include <core/CStringUtils.h>

#include <sys/utsname.h>

#include <string.h>


namespace prelert
{
namespace core
{


std::string CUname::sysName(void)
{
    struct utsname name;
    ::uname(&name);

    return name.sysname;
}

std::string CUname::nodeName(void)
{
    struct utsname name;
    ::uname(&name);

    return name.nodename;
}

std::string CUname::release(void)
{
    struct utsname name;
    ::uname(&name);

    return name.release;
}

std::string CUname::version(void)
{
    struct utsname name;
    ::uname(&name);

    return name.version;
}

std::string CUname::machine(void)
{
    struct utsname name;
    ::uname(&name);

    return name.machine;
}

std::string CUname::all(void)
{
    struct utsname name;
    ::uname(&name);

    // This is in the format of "uname -a"
    std::string all(name.sysname);
    all += ' ';
    all += name.nodename;
    all += ' ';
    all += name.release;
    all += ' ';
    all += name.version;
    all += ' ';
    all += name.machine;

    return all;
}

std::string CUname::prelertPlatform(void)
{
    struct utsname name;
    ::uname(&name);

    // Determine the current platform name, in the format used by Kibana
    // downloads.  For *nix platforms this is more-or-less `uname -s`-`uname -m`
    // converted to lower case.  However, for consistency between different
    // operating systems on the same architecture "amd64"/"i86pc" are replaced
    // with "x86_64" and "i386"/"i686" with "x86".
    //
    // Assuming the current platform is supported this name will be one of:
    // - darwin-x86_64
    // - linux-x86_64
    //
    // For an unsupported platform it will be something different, but hopefully
    // still recognisable.

    std::string os(CStringUtils::toLower(name.sysname));
    const std::string &machine = CStringUtils::toLower(name.machine);
    if (machine.length() == 4 && machine[0] == 'i' && machine[2] == '8' && machine[3] == '6')
    {
        return os + "-x86";
    }

    if (machine == "amd64" || machine == "i86pc")
    {
        return os + "-x86_64";
    }

    return os + '-' + machine;
}

std::string CUname::prelertOsVer(void)
{
    return CUname::release();
}


}
}

