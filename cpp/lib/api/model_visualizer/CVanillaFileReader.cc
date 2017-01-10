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

#include "CVanillaFileReader.h"

#include <api/CAnomalyDetector.h>

#include <fstream>

namespace ml
{
namespace model_visualiser
{

CVanillaFileReader::CVanillaFileReader(std::string filename)
{
    m_Filename.swap(filename);
}

bool CVanillaFileReader::search(const std::string &/*search*/,
                                TStrStrUMapList &results)
{
    std::string fileContents;
    std::ifstream fileStream(m_Filename.c_str(), std::ios::in | std::ios::binary);
    if (!fileStream.is_open())
    {
        LOG_ERROR("Failed to open file " << m_Filename << " for restoring models");
        return false;
    }

    fileStream.seekg(0, std::ios::end);
    fileContents.resize(static_cast<size_t>(fileStream.tellg()));
    fileStream.seekg(0, std::ios::beg);
    fileStream.read(&fileContents[0], fileContents.size());

    if (fileStream.bad())
    {
        LOG_ERROR("Stream is bad after restoring " << m_Filename);
        return false;
    }

    core::CStringUtils::trimWhitespace(fileContents);

    // Contents could be big, so avoid unnecessary copying
    results.push_back(TStrStrUMap());
    results.back()[api::CAnomalyDetector::RAW_NAME].swap(fileContents);

    return true;
}

}
}
