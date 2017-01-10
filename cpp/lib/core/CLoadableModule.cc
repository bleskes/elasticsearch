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
#include <core/CLoadableModule.h>

#include <core/CLogger.h>
#include <core/CStringUtils.h>


namespace ml
{
namespace core
{


const std::string CLoadableModule::CREATE_FN_NAME("createInstance");


CLoadableModule::CLoadableModule(const std::string &moduleName)
    : m_ModuleName(moduleName)
{
}

bool CLoadableModule::loadModule(void)
{
    if (m_ModuleName.empty())
    {
        LOG_ERROR("Cannot load module with no name");
        return false;
    }

    std::string path(CLoadableModule::createPath(m_ModuleName));

    if (this->load(path) == false)
    {
        LOG_ERROR("Cannot load module " << m_ModuleName);
        return false;
    }

    return true;
}

std::string CLoadableModule::createPath(const std::string &moduleName)
{
    std::string path(CRuntimeLoadedLibrary::mlLibDir());
    path += "/lib";
    path += moduleName;
    path += STRINGIFY_MACRO(DYNAMIC_LIB_EXT);
    return path;
}

}
}

