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

#include <core/CLogger.h>
#include <core/COsFileFuncs.h>

#include <model/CLimits.h>
#include <model/CModel.h>
#include <model/CModelConfig.h>

#include <api/CCsvOutputWriter.h>
#include <api/CFieldConfig.h>

#include "CCmdLineParser.h"
#include "CModelVisualiser.h"

#include <string>
#include <fstream>

#include <stdlib.h>

using namespace ml;

int main(int argc, char **argv)
{
    // Read command line options.
    std::string modelFile;
    std::string logProperties;
    core_t::TTime epochTime = 0;
    std::string byClause;
    std::string overClause;
    if (model_visualiser::CCmdLineParser::parse(argc,
                                                argv,
                                                modelFile,
                                                logProperties,
                                                epochTime,
                                                byClause,
                                                overClause) == false)
    {
        // Output handled by command parser.
        return EXIT_FAILURE;
    }

    if (!logProperties.empty())
    {
        core::CLogger::instance().reconfigureFromFile(logProperties);
    }

    model::CLimits limits;
    api::CFieldConfig fieldConfig;
    model::CModelConfig modelConfig = model::CModelConfig::defaultOnlineConfig();
    std::ofstream outputStrm(core::COsFileFuncs::NULL_FILENAME);
    if (!outputStrm.is_open())
    {
        LOG_ERROR("Failed to open \"/dev/null\"");
        return EXIT_FAILURE;
    }
    api::CCsvOutputWriter outputWriter(outputStrm);

    model_visualiser::CModelVisualiser visualiser(limits,
                                                  fieldConfig,
                                                  modelConfig,
                                                  outputWriter);
    if (!visualiser.load(modelFile))
    {
        LOG_ERROR("Failed to restore state from: " << modelFile);
        return EXIT_FAILURE;
    }

    visualiser.visualise(epochTime, byClause, overClause);

    return EXIT_SUCCESS;
}

