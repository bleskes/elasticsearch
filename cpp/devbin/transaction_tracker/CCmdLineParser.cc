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

#include <ver/CBuildInfo.h>

#include <boost/program_options.hpp>

#include <iostream>

namespace ml
{
namespace devbin
{


const std::string CCmdLineParser::DESCRIPTION =
"Usage: transaction_tracker [options]\n"
"Options:";


bool CCmdLineParser::parse(int argc,
                           const char * const *argv,
                           std::string &fileName)
{
    try
    {
        boost::program_options::options_description desc(DESCRIPTION);
        desc.add_options()
            ("help", "Display this information and exit")
            ("version", "Display version information and exit")
            ("filename", boost::program_options::value<std::string>(),
                        "File to read from")
        ;

        boost::program_options::variables_map vm;
        boost::program_options::store(boost::program_options::parse_command_line(argc, argv, desc), vm);
        boost::program_options::notify(vm);

        if (vm.count("help") > 0)
        {
            std::cerr << desc << std::endl;
            return false;
        }
        if (vm.count("version") > 0)
        {
            std::cerr << ml::ver::CBuildInfo::fullInfo() << std::endl;
            return false;
        }
        if (vm.count("filename") > 0)
        {
            fileName = vm["filename"].as<std::string>();
        }
        else
        {
            std::cerr << desc << std::endl;
            return false;
        }
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
