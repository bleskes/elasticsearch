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

#include "CCmdLineParser.h"

#include <core/CTimeUtils.h>

#include <boost/program_options.hpp>

#include <iostream>
#include <vector>

namespace ml
{
namespace model_visualiser
{

const std::string CCmdLineParser::DESCRIPTION =
"Usage: model_visualiser [options] <modelfile>\n"
"Options:";

bool CCmdLineParser::parse(int argc,
                           const char * const *argv,
                           std::string &modelFile,
                           std::string &logProperties,
                           core_t::TTime &epochTime,
                           std::string &byClause,
                           std::string &overClause)
{
    typedef std::vector<std::string> TStrVec;

    try
    {
        boost::program_options::options_description desc(DESCRIPTION);
        desc.add_options()
            ("help", "Display this information and exit")
            ("logProperties", boost::program_options::value<std::string>(),
                        "Optional logger properties file")
            ("time", boost::program_options::value<std::string>(),
                        "Optional local time at which to visualise models (in MM/DD/YY HH:MM:SS format)")
            ("by", boost::program_options::value<std::string>(),
                         "Optional value of interest for the command \"by\" clause field")
            ("over", boost::program_options::value<std::string>(),
                         "Optional value of interest for the \"over\" clause field")
        ;

        boost::program_options::variables_map vm;
        boost::program_options::parsed_options parsed =
                boost::program_options::command_line_parser(argc, argv).options(desc).allow_unregistered().run();
        boost::program_options::store(parsed, vm);

        if (vm.count("help") > 0)
        {
            std::cerr << desc << std::endl;
            return false;
        }
        if (vm.count("logProperties") > 0)
        {
            logProperties = vm["logProperties"].as<std::string>();
        }
        if (vm.count("time") > 0)
        {
            std::string format("%m/%d/%y %H:%M:%S");
            core::CTimeUtils::strptime(format,
                                       vm["time"].as<std::string>(),
                                       epochTime);
        }
        else
        {
            // Dubious, but there isn't much better we can do.
            // Generally, the time should be specified if someone
            // is visualising historical models. Note that this
            // is effectively only used to select the model batch
            // so may disappear when we have new periodicity
            // support.
            epochTime = core::CTimeUtils::now();
        }
        if (vm.count("by") > 0)
        {
            byClause = vm["by"].as<std::string>();
        }
        if (vm.count("over") > 0)
        {
            overClause = vm["over"].as<std::string>();
        }

        TStrVec remainder = boost::program_options::collect_unrecognized(parsed.options,
                                                                         boost::program_options::include_positional);
        if (remainder.size() != 1)
        {
            std::string file;
            for (std::size_t i = 0u; i < remainder.size(); ++i)
            {
                file.append(remainder[i]);
            }
            std::cerr << "Bad model file name: \"" << file << "\"" << std::endl;
            return false;
        }
        modelFile = remainder[0];
    }
    catch (std::exception &e)
    {
        std::cerr << "Error processing command line: " << e.what() << std::endl;
        return false;
    }

    return true;
}


}
}

