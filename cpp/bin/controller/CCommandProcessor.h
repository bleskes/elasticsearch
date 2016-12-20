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
#ifndef INCLUDED_prelert_controller_CCommandProcessor_h
#define INCLUDED_prelert_controller_CCommandProcessor_h

#include <core/CDetachedProcessSpawner.h>

#include <iosfwd>
#include <string>
#include <vector>


namespace prelert
{
namespace controller
{

//! \brief
//! Processes commands received on a C++ stream.
//!
//! DESCRIPTION:\n
//! Reads from the supplied stream until end-of-file is reached.
//!
//! Each (newline terminated) line is assumed to be a tab-separated
//! command to be executed.
//!
//! Each command has the following format:
//! verb arguments...
//!
//! Available verbs are:
//! 1) start - in this case the arguments consist of the process name
//!    followed by the arguments to pass to the new process
//!
//! IMPLEMENTATION DECISIONS:\n
//! Commands are case sensitive.
//!
//! Only processes on a supplied list may be executed; requests to
//! start other processes are ignored.
//!
class CCommandProcessor
{
    public:
        typedef std::vector<std::string> TStrVec;

    public:
        //! The start command
        static const std::string START;

    public:
        CCommandProcessor(const TStrVec &permittedProcessPaths);

        //! Action commands read from the supplied \p stream until end-of-file
        //! is reached.
        void processCommands(std::istream &stream);

        //! Parse and handle a single command.
        bool handleCommand(const std::string &command);

    private:
        //! Handle a start command.
        //! \param tokens Tokens to the command excluding the verb.  Passed
        //!               non-const so that this method can manipulate the
        //!               tokens without having to copy.
        bool handleStart(TStrVec &tokens);

    private:
        //! Used to spawn the requested processes.
        core::CDetachedProcessSpawner m_Spawner;
};


}
}

#endif // INCLUDED_prelert_controller_CCommandProcessor_h
