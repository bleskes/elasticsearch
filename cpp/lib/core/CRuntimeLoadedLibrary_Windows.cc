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
#include <core/CRuntimeLoadedLibrary.h>

#include <core/CLogger.h>
#include <core/CProgName.h>
#include <core/CWindowsError.h>


namespace prelert
{
namespace core
{


CRuntimeLoadedLibrary::CRuntimeLoadedLibrary(void)
    : m_LibraryHandle(0)
{
}

CRuntimeLoadedLibrary::~CRuntimeLoadedLibrary(void)
{
    if (this->isLoaded())
    {
        this->unload();
    }
}

bool CRuntimeLoadedLibrary::load(const std::string &libraryPath)
{
    if (this->isLoaded())
    {
        LOG_ERROR("Trying to load library " << libraryPath <<
                  " into object that has already loaded " << m_LibraryPath);
        return false;
    }

    m_LibraryHandle = LoadLibrary(libraryPath.c_str());
    if (m_LibraryHandle == 0)
    {
        LOG_ERROR("Cannot load library " << libraryPath <<
                  ": " << CWindowsError());
        return false;
    }

    m_LibraryPath = libraryPath;

    return true;
}

bool CRuntimeLoadedLibrary::unload(void)
{
    if (m_LibraryHandle == 0)
    {
        LOG_ERROR("Trying to unload a library that is not currently loaded");
        return false;
    }

    if (FreeLibrary(m_LibraryHandle) == FALSE)
    {
        LOG_ERROR("Failed to unload library " << m_LibraryPath <<
                  ": " << CWindowsError());
        return false;
    }

    m_LibraryHandle = 0;
    m_LibraryPath.clear();

    return true;
}

bool CRuntimeLoadedLibrary::isLoaded(void) const
{
    return m_LibraryHandle != 0;
}

const std::string &CRuntimeLoadedLibrary::loadedPath(void) const
{
    return m_LibraryPath;
}

intptr_t CRuntimeLoadedLibrary::funcAddr(const std::string &funcName) const
{
    if (!this->isLoaded())
    {
        LOG_ERROR("Trying to get a pointer to function " << funcName <<
                  " from a library that is not loaded");
        return 0;
    }

    intptr_t func(reinterpret_cast<intptr_t>(GetProcAddress(m_LibraryHandle,
                                                            funcName.c_str())));
    if (func == 0)
    {
        LOG_ERROR("Could not find symbol " << funcName <<
                  " in library " << m_LibraryPath << ": " << CWindowsError());
        return 0;
    }

    return func;
}

std::string CRuntimeLoadedLibrary::prelertLibDir(void)
{
    return CProgName::progDir();
}


}
}

