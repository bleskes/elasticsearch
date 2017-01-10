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
//! Normalise anomaly scores and/or probabilties in results
//!
//! DESCRIPTION:\n
//! Expects to be streamed CSV data on STDIN,
//! and sends its JSON results to STDOUT.
//!
//! IMPLEMENTATION DECISIONS:\n
//! Standalone program.
//!
#include <core/CContainerPrinter.h>
#include <core/CLogger.h>
#include <core/CoreTypes.h>

#include <ver/CBuildInfo.h>

#include <maths/CIntegerTools.h>
#include <maths/COrderings.h>

#include <model/CModelConfig.h>

#include <model/CAnomalyScore.h>

#include <api/CCsvInputParser.h>
#include <api/CCsvOutputWriter.h>

#include "CCmdLineParser.h"

#include <boost/scoped_ptr.hpp>

#include <ios>
#include <iostream>
#include <string>
#include <stdio.h>
#include <stdlib.h>


namespace
{

//! Error reporter
class CStdErrorWriter
{
    public:
        void operator()(const std::string &msg) const
        {
            std::cerr << msg << std::endl;
        }
};

const std::string TIME("timestamp");
const std::string PROBABILITY("probability");
const std::string SCORE("anomalyScore");
const std::string PARTITION("partitionValue");
const std::string BY("byValue");
const std::string ACTUAL("actual");
const std::string TYPICAL("typical");
const std::string METRIC("fieldName");

class CNormaliseStream
{
    public:
        typedef std::vector<double> TDoubleVec;
        typedef boost::optional<double> TOptionalDouble;
        typedef std::vector<TOptionalDouble> TOptionalDoubleVec;
        typedef std::pair<std::string, std::string> TStrStrPr;
        typedef std::vector<std::string> TStrVec;
        typedef boost::unordered_map<std::string, std::string> TStrStrUMap;

    public:
        CNormaliseStream(const ml::model::CModelConfig &modelConfig,
                         ml::api::COutputHandler &outputHandler,
                         ml::core_t::TTime bucketLength) :
                m_ModelConfig(modelConfig),
                m_OutputHandler(outputHandler),
                m_BucketLength(bucketLength)
        {
            TStrVec extraFieldNames;
            extraFieldNames.push_back(TIME);
            extraFieldNames.push_back(PROBABILITY);
            extraFieldNames.push_back(SCORE);
            extraFieldNames.push_back(PARTITION);
            extraFieldNames.push_back(BY);
            extraFieldNames.push_back(ACTUAL);
            extraFieldNames.push_back(TYPICAL);
            extraFieldNames.push_back(METRIC);
            m_OutputHandler.fieldNames(TStrVec(), extraFieldNames);
        }

        bool handleRecord(bool /*isDryRun*/,
                          const TStrVec &fields,
                          const TStrStrUMap &fieldValues)
        {
            SRecord record;
            for (std::size_t i = 0u; i < fields.size(); ++i)
            {
                if (fields[i] == TIME)
                {
                    ml::core::CStringUtils::stringToType(fieldValues.find(fields[i])->second,
                                                              record.s_Time);
                }
                else if (fields[i] == PROBABILITY)
                {
                    ml::core::CStringUtils::stringToType(fieldValues.find(fields[i])->second,
                                                              record.s_Probability);

                }
                else if (fields[i] == PARTITION)
                {
                    record.s_PartitionField = fieldValues.find(fields[i])->second;
                }
                else if (fields[i] == BY)
                {
                    record.s_ByField = fieldValues.find(fields[i])->second;
                }
                else if (fields[i] == ACTUAL)
                {
                    record.s_Actual = fieldValues.find(fields[i])->second;
                }
                else if (fields[i] == TYPICAL)
                {
                    record.s_Typical = fieldValues.find(fields[i])->second;
                }
                else if (fields[i] == METRIC)
                {
                    record.s_Metric = fieldValues.find(fields[i])->second;
                }
            }
            m_Records.push_back(record);

            return true;
        }

        void outputResults(void)
        {
            if (m_Records.empty())
            {
                return;
            }

            std::sort(m_Records.begin(), m_Records.end());
            ml::core_t::TTime firstTime = ml::maths::CIntegerTools::floor(m_Records[0].s_Time, m_BucketLength);
            LOG_TRACE("records = " << ml::core::CContainerPrinter::print(m_Records));

            ml::model::CAnomalyScore::CComputer computer(m_ModelConfig.jointProbabilityWeight(false),
                                                              m_ModelConfig.extremeProbabilityWeight(false),
                                                              m_ModelConfig.maxExtremeSamples(false),
                                                              0.05);
            ml::model::CAnomalyScore::CNormalizer normalizer(m_ModelConfig);

            {
                TOptionalDoubleVec scores;
                ml::core_t::TTime bucketStart = firstTime;
                for (std::size_t i = 0u; i < m_Records.size(); ++i)
                {
                    for (/**/;
                         m_Records[i].s_Time >= bucketStart + m_BucketLength;
                         bucketStart += m_BucketLength)
                    {
                        LOG_TRACE("Aggregating [" << bucketStart << "," << bucketStart + m_BucketLength << "]");
                        LOG_TRACE("probabilities = " << ml::core::CContainerPrinter::print(scores));
                        computer.compute(scores);
                        for (std::size_t j = 0u; j < scores.size(); ++j)
                        {
                            m_Records[i + j - scores.size()].s_Score = *scores[j];
                        }

                        double score = 0.0;
                        for (std::size_t j = 0u; j < scores.size(); ++j)
                        {
                            if (scores[j])
                            {
                                score += *scores[j];
                            }
                        }
                        LOG_TRACE("score = " << score);
                        normalizer.updateQuantiles(score);

                        scores.clear();
                    }
                    scores.push_back(m_Records[i].s_Probability);
                }
            }

            {
                TDoubleVec normalizedScores;
                ml::core_t::TTime bucketStart = firstTime;
                for (std::size_t i = 0u; i < m_Records.size(); ++i)
                {
                    for (/**/;
                         m_Records[i].s_Time >= bucketStart + m_BucketLength;
                         bucketStart += m_BucketLength)
                    {
                        normalizer.normalize(normalizedScores);
                        LOG_TRACE("normalizedScore = " << ml::core::CContainerPrinter::print(normalizedScores));

                        for (std::size_t j = 0u; j < normalizedScores.size(); ++j)
                        {
                            const SRecord &record = m_Records[i + j - normalizedScores.size()];
                            TStrStrUMap columns;
                            columns.insert(TStrStrPr(TIME, ml::core::CStringUtils::typeToString(record.s_Time)));
                            columns.insert(TStrStrPr(PROBABILITY, ml::core::CStringUtils::typeToStringPretty(record.s_Probability)));
                            columns.insert(TStrStrPr(SCORE, ml::core::CStringUtils::typeToString(normalizedScores[j])));
                            columns.insert(TStrStrPr(PARTITION, ml::core::CStringUtils::typeToString(record.s_PartitionField)));
                            columns.insert(TStrStrPr(BY, ml::core::CStringUtils::typeToString(record.s_ByField)));
                            columns.insert(TStrStrPr(ACTUAL, ml::core::CStringUtils::typeToString(record.s_Actual)));
                            columns.insert(TStrStrPr(TYPICAL, ml::core::CStringUtils::typeToString(record.s_Typical)));
                            columns.insert(TStrStrPr(METRIC, ml::core::CStringUtils::typeToString(record.s_Metric)));
                            m_OutputHandler.writeRow(false, TStrStrUMap(), columns);
                        }
                        normalizedScores.clear();
                    }
                    normalizedScores.push_back(m_Records[i].s_Score);
                }
            }

        }

    private:
        struct SRecord
        {
            bool operator<(const SRecord &rhs) const
            {
                return ml::maths::COrderings::lexicographical_compare(s_Time, s_ByField,
                                                                           rhs.s_Time, rhs.s_ByField);
            }
            std::string print(void) const
            {
                return  ml::core::CStringUtils::typeToString(s_Time)
                      + ' '
                      + ml::core::CStringUtils::typeToString(s_Probability)
                      + ' '
                      + s_ByField;
            }
            ml::core_t::TTime s_Time;
            double s_Probability;
            double s_Score;
            std::string s_PartitionField;
            std::string s_ByField;
            std::string s_Actual;
            std::string s_Typical;
            std::string s_Metric;
        };
        typedef std::vector<SRecord> TRecordVec;

    private:
        const ml::model::CModelConfig &m_ModelConfig;
        ml::api::COutputHandler &m_OutputHandler;
        ml::core_t::TTime m_BucketLength;
        TRecordVec m_Records;
};

}


int main(int argc, char **argv)
{
    // On Linux and Mac OS X, tests show that input/output of the textual data is
    // about 20% faster if C and C++ IO functionality is NOT synchronised.  This
    // doesn't seem to make any difference on Solaris or Windows though.
    bool wasSynchronised(std::ios::sync_with_stdio(false));
    if (wasSynchronised)
    {
        LOG_TRACE("C++ streams no longer synchronised with C stdio");
    }

    // Untying std::cin and std::cout improves performance by about 10%, 5%, 8%
    // and 15% on Linux, Mac OS X, Solaris and Windows respectively.
    std::cin.tie(0);
    std::cout.tie(0);

    // For the API, also untie std::cerr as it's used for error reporting.  (In
    // C++11 it's tied to std::cout; in C++03 this doesn't do any good but
    // doesn't do any harm either.)
    std::cerr.tie(0);

    CStdErrorWriter stdErrorWriter;

    // Read command line options
    std::string            modelConfigFile;
    std::string            logProperties;
    ml::core_t::TTime bucketSpan(600);
    if (ml::normalize_results::CCmdLineParser::parse(argc,
                                                          argv,
                                                          modelConfigFile,
                                                          logProperties,
                                                          bucketSpan) == false)
    {
        return EXIT_FAILURE;
    }

    if (!logProperties.empty())
    {
        ml::core::CLogger::instance().reconfigureFromFile(logProperties);
    }
    // Log the program version immediately after reconfiguring the logger.  This
    // must be done from the program, and NOT a shared library, as each program
    // statically links its own version library.
    LOG_INFO(ml::ver::CBuildInfo::fullInfo());

    ml::model::CModelConfig modelConfig =
            ml::model::CModelConfig::defaultOnlineConfig(bucketSpan);
    if (!modelConfigFile.empty() && modelConfig.init(modelConfigFile) == false)
    {
        std::string msg("Ml model config file '" + modelConfigFile +
                        "' could not be loaded");
        LOG_FATAL(msg);
        stdErrorWriter(msg);
        return EXIT_FAILURE;
    }

    // There's a choice of input and output formats for the numbers to be normalised
    typedef boost::scoped_ptr<ml::api::CInputParser> TScopedInputParserP;
    TScopedInputParserP inputParser;
    inputParser.reset(new ml::api::CCsvInputParser(std::cin,
                                                        ml::api::CCsvInputParser::COMMA,
                                                        stdErrorWriter));

    typedef boost::scoped_ptr<ml::api::COutputHandler> TScopedOutputHandlerP;
    TScopedOutputHandlerP outputWriter;
    outputWriter.reset(new ml::api::CCsvOutputWriter(std::cout));

    // This object will do the work
    CNormaliseStream normalizer(modelConfig, *outputWriter, bucketSpan);

    // Now handle the numbers to be normalised from stdin
    if (inputParser->readStream(false,
                                ml::api::CInputParser::TSettingsFunc(),
                                boost::bind(&CNormaliseStream::handleRecord,
                                            &normalizer,
                                            _1,
                                            _2,
                                            _3)) == false)
    {
        std::string msg("Failed to handle input to be normalized");
        LOG_FATAL(msg);
        stdErrorWriter(msg);
        return EXIT_FAILURE;
    }

    normalizer.outputResults();

    // This debug makes it easier to spot process crashes in a log file - if
    // this isn't present in the log for a given PID and there's no other log
    // message indicating early exit then the process has probably core dumped
    LOG_DEBUG("Ml normalizer exiting");

    return EXIT_SUCCESS;
}

