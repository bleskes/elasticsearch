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
//! \brief
//! Analyse event rates and metrics
//!
//! DESCRIPTION:\n
//! Expects to be streamed CSV or length encoded data on STDIN or a named pipe,
//! and sends its JSON results to STDOUT or another named pipe.
//!
//! IMPLEMENTATION DECISIONS:\n
//! Standalone program.
//!
#include <core/CDataAdder.h>
#include <core/CDataSearcher.h>
#include <core/CLicenseValidator.h>
#include <core/CLogger.h>
#include <core/CoreTypes.h>
#include <core/CStatistics.h>

#include <ver/CBuildInfo.h>

#include <model/CLimits.h>
#include <model/CModelConfig.h>
#include <model/ModelTypes.h>

#include <api/CAnomalyDetector.h>
#include <api/CBackgroundPersister.h>
#include <api/CCmdSkeleton.h>
#include <api/CCsvInputParser.h>
#include <api/CFieldConfig.h>
#include <api/CFieldDataTyper.h>
#include <api/CIoManager.h>
#include <api/CJsonOutputWriter.h>
#include <api/CLengthEncodedInputParser.h>
#include <api/COutputChainer.h>
#include <api/CSingleStreamDataAdder.h>
#include <api/CSingleStreamSearcher.h>

#include "CCmdLineParser.h"

#include <boost/bind.hpp>
#include <boost/scoped_ptr.hpp>

#include <iostream>
#include <string>

#include <stdio.h>
#include <stdlib.h>


namespace
{

class CFirstProcessorPeriodicPersist
{
    public:
        CFirstProcessorPeriodicPersist(ml::api::CDataProcessor &firstProcessor,
                                       ml::core::CDataAdder &persister)
            : m_FirstProcessor(firstProcessor),
              m_Persister(persister)
        {
        }

        bool operator()(void) const
        {
            return m_FirstProcessor.periodicPersistState(m_Persister);
        }

    private:
        ml::api::CDataProcessor &m_FirstProcessor;
        ml::core::CDataAdder    &m_Persister;
};

}


int main(int argc, char **argv)
{
    typedef ml::autodetect::CCmdLineParser::TStrVec TStrVec;

    // Read command line options
    std::string            limitConfigFile;
    std::string            modelConfigFile;
    std::string            fieldConfigFile;
    std::string            modelDebugConfigFile;
    std::string            jobId;
    std::string            logProperties;
    std::string            logPipe;
    ml::core_t::TTime bucketSpan(0);
    ml::core_t::TTime batchSpan(ml::model::CModelConfig::DEFAULT_BATCH_LENGTH);
    ml::core_t::TTime latency(0);
    std::size_t            period(ml::model::CModelConfig::APERIODIC);
    std::string            summaryCountFieldName;
    char                   delimiter('\t');
    bool                   lengthEncodedInput(false);
    std::string            timeField(ml::api::CAnomalyDetector::DEFAULT_TIME_FIELD_NAME);
    std::string            timeFormat;
    std::string            quantilesStateFile;
    bool                   deleteStateFiles(false);
    ml::core_t::TTime persistInterval(-1);
    ml::core_t::TTime maxQuantileInterval(-1);
    std::string            inputFileName;
    bool                   isInputFileNamedPipe(false);
    std::string            outputFileName;
    bool                   isOutputFileNamedPipe(false);
    std::string            restoreFileName;
    bool                   isRestoreFileNamedPipe(false);
    std::string            persistFileName;
    bool                   isPersistFileNamedPipe(false);
    size_t                 maxAnomalyRecords(100u);
    std::string            license;
    bool                   memoryUsage(false);
    std::size_t            bucketResultsDelay(0);
    bool                   ignoreDowntime(false);
    bool                   multivariateByFields(false);
    std::string            multipleBucketspans;
    bool                   perPartitionNormalization(false);
    TStrVec                clauseTokens;
    if (ml::autodetect::CCmdLineParser::parse(argc,
                                                   argv,
                                                   limitConfigFile,
                                                   modelConfigFile,
                                                   fieldConfigFile,
                                                   modelDebugConfigFile,
                                                   jobId,
                                                   logProperties,
                                                   logPipe,
                                                   bucketSpan,
                                                   batchSpan,
                                                   latency,
                                                   period,
                                                   summaryCountFieldName,
                                                   delimiter,
                                                   lengthEncodedInput,
                                                   timeField,
                                                   timeFormat,
                                                   quantilesStateFile,
                                                   deleteStateFiles,
                                                   persistInterval,
                                                   maxQuantileInterval,
                                                   inputFileName,
                                                   isInputFileNamedPipe,
                                                   outputFileName,
                                                   isOutputFileNamedPipe,
                                                   restoreFileName,
                                                   isRestoreFileNamedPipe,
                                                   persistFileName,
                                                   isPersistFileNamedPipe,
                                                   maxAnomalyRecords,
                                                   license,
                                                   memoryUsage,
                                                   bucketResultsDelay,
                                                   ignoreDowntime,
                                                   multivariateByFields,
                                                   multipleBucketspans,
                                                   perPartitionNormalization,
                                                   clauseTokens) == false)
    {
        return EXIT_FAILURE;
    }

    // Construct the IO manager before reconfiguring the logger, as it performs
    // std::ios actions that only work before first use
    ml::api::CIoManager ioMgr(inputFileName,
                                   isInputFileNamedPipe,
                                   outputFileName,
                                   isOutputFileNamedPipe,
                                   restoreFileName,
                                   isRestoreFileNamedPipe,
                                   persistFileName,
                                   isPersistFileNamedPipe);

    if (ml::core::CLogger::instance().reconfigure(logPipe, logProperties) == false)
    {
        LOG_FATAL("Could not reconfigure logging");
        return EXIT_FAILURE;
    }

    // Log the program version immediately after reconfiguring the logger.  This
    // must be done from the program, and NOT a shared library, as each program
    // statically links its own version library.
    LOG_INFO(ml::ver::CBuildInfo::fullInfo());

    if (ioMgr.initIo() == false)
    {
        LOG_FATAL("Failed to initialise IO");
        return EXIT_FAILURE;
    }

    if (ml::core::CLicenseValidator::validate(license) == false)
    {
        LOG_FATAL("Invalid license");
        return EXIT_FAILURE;
    }

    if (jobId.empty())
    {
        LOG_FATAL("No job ID specified");
        return EXIT_FAILURE;
    }

    ml::model::CLimits limits;
    if (!limitConfigFile.empty() && limits.init(limitConfigFile) == false)
    {
        LOG_FATAL("Ml limit config file '" << limitConfigFile <<
                  "' could not be loaded");
        return EXIT_FAILURE;
    }

    ml::api::CFieldConfig fieldConfig;

    ml::model_t::ESummaryMode summaryMode(summaryCountFieldName.empty() ? ml::model_t::E_None
                                                                             : ml::model_t::E_Manual);
    ml::model::CModelConfig modelConfig =
            ml::model::CModelConfig::defaultConfig(bucketSpan,
                                                        batchSpan,
                                                        period,
                                                        summaryMode,
                                                        summaryCountFieldName,
                                                        latency,
                                                        bucketResultsDelay,
                                                        multivariateByFields,
                                                        multipleBucketspans);
    modelConfig.perPartitionNormalization(perPartitionNormalization);
    modelConfig.detectionRules(ml::model::CModelConfig::TIntDetectionRuleVecUMapCRef(fieldConfig.detectionRules()));
    if (!modelConfigFile.empty() && modelConfig.init(modelConfigFile) == false)
    {
        LOG_FATAL("Ml model config file '" << modelConfigFile <<
                  "' could not be loaded");
        return EXIT_FAILURE;
    }

    if (!modelDebugConfigFile.empty() && modelConfig.configureDebug(modelDebugConfigFile) == false)
    {
        LOG_FATAL("Ml model debug config file '" << modelDebugConfigFile <<
                  "' could not be loaded");
        return EXIT_FAILURE;
    }

    typedef boost::scoped_ptr<ml::core::CDataSearcher> TScopedDataSearcherP;
    TScopedDataSearcherP restoreSearcher;
    if (ioMgr.restoreStream() != 0)
    {
        restoreSearcher.reset(new ml::api::CSingleStreamSearcher(ioMgr.restoreStream()));
    }

    typedef boost::scoped_ptr<ml::core::CDataAdder> TScopedDataAdderP;
    TScopedDataAdderP persister;
    if (ioMgr.persistStream() != 0)
    {
        persister.reset(new ml::api::CSingleStreamDataAdder(ioMgr.persistStream()));
    }

    typedef boost::scoped_ptr<ml::api::CBackgroundPersister> TScopedBackgroundPersisterP;
    TScopedBackgroundPersisterP periodicPersister;
    if (persistInterval >= 0)
    {
        if (persister == 0)
        {
            LOG_FATAL("Periodic persistence cannot be enabled using the 'persistInterval' argument "
                      "unless a place to persist to has been specified using the 'persist' argument");
            return EXIT_FAILURE;
        }

        periodicPersister.reset(new ml::api::CBackgroundPersister(*persister));
    }

    typedef boost::scoped_ptr<ml::api::CInputParser> TScopedInputParserP;
    TScopedInputParserP inputParser;
    if (lengthEncodedInput)
    {
        inputParser.reset(new ml::api::CLengthEncodedInputParser(ioMgr.inputStream()));
    }
    else
    {
        inputParser.reset(new ml::api::CCsvInputParser(ioMgr.inputStream(),
                                                            delimiter));
    }

    ml::api::CJsonOutputWriter outputWriter(jobId, ioMgr.outputStream());
    outputWriter.limitNumberRecords(maxAnomalyRecords);

    if (fieldConfig.initFromCmdLine(fieldConfigFile,
                                    clauseTokens) == false)
    {
        LOG_FATAL("Field config could not be interpreted");
        return EXIT_FAILURE;
    }

    // The anomaly detector knows how to detect anomalies
    ml::api::CAnomalyDetector detector(jobId,
                                            limits,
                                            fieldConfig,
                                            modelConfig,
                                            outputWriter,
                                            boost::bind(&ml::api::CJsonOutputWriter::reportPersistComplete,
                                                        &outputWriter,
                                                        _1,
                                                        _2,
                                                        _3,
                                                        _4,
                                                        _5,
                                                        _6,
                                                        _7,
                                                        _8),
                                            periodicPersister.get(),
                                            persistInterval,
                                            maxQuantileInterval,
                                            timeField,
                                            timeFormat,
                                            ignoreDowntime);

    if (!quantilesStateFile.empty())
    {
        if (detector.initNormalizer(quantilesStateFile) == false)
        {
            LOG_FATAL("Failed to restore quantiles and initialize normalizer");
            return EXIT_FAILURE;
        }
        if (deleteStateFiles)
        {
            ::remove(quantilesStateFile.c_str());
        }
    }

    ml::api::CDataProcessor *firstProcessor(&detector);

    // Chain the detector's input
    ml::api::COutputChainer outputChainer(detector);

    // The typer knows how to assign categories to records
    ml::api::CFieldDataTyper typer(jobId, fieldConfig, limits, outputChainer, outputWriter);

    if (fieldConfig.fieldNameSuperset().count(ml::api::CFieldDataTyper::MLCATEGORY_NAME) > 0)
    {
        firstProcessor = &typer;
    }

    if (persister != 0)
    {
        detector.firstProcessorPeriodicPersistFunc(
                CFirstProcessorPeriodicPersist(*firstProcessor, *persister));
    }

    // The skeleton avoids the need to duplicate a lot of boilerplate code
    ml::api::CCmdSkeleton skeleton(restoreSearcher.get(),
                                        persister.get(),
                                        *inputParser,
                                        *firstProcessor);
    bool ioLoopSucceeded(skeleton.ioLoop());

    // Unfortunately we cannot rely on destruction to finalise the output writer
    // as it must be finalised before the skeleton is destroyed, and C++
    // destruction order means the skeleton will be destroyed before the output
    // writer as it was constructed last.
    outputWriter.finalise();

    if (!ioLoopSucceeded)
    {
        LOG_FATAL("Ml anomaly detection failed");
        return EXIT_FAILURE;
    }

    if (memoryUsage)
    {
        detector.descriptionAndDebugMemoryUsage();
    }

    // Print out the runtime stats generated during this execution context
    LOG_DEBUG(ml::core::CStatistics::instance());

    // This message makes it easier to spot process crashes in a log file - if
    // this isn't present in the log for a given PID and there's no other log
    // message indicating early exit then the process has probably core dumped
    LOG_INFO("Ml anomaly detector exiting");

    return EXIT_SUCCESS;
}
