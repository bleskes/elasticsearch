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
#ifndef INCLUDED_ml_core_CTextFileWatcher_h
#define INCLUDED_ml_core_CTextFileWatcher_h

#include <core/CDelimiter.h>
#include <core/CNonCopyable.h>
#include <core/COsFileFuncs.h>
#include <core/ImportExport.h>

#include <boost/function.hpp>

#include <string>


namespace ml
{
namespace core
{

//! \brief
//! A simple class to report changes to a file.
//!
//! DESCRIPTION:\n
//! A simple class to report changes to a file.
//!
//! IMPLEMENTATION DECISIONS:\n
//! Changes to the file are pushed line by line to a visitor function.
//! This means that if this reads a large file from scratch the file is not read
//! into memory.
//!
//! Incomplete lines are not passed on.
//!
//! The 'changes' to the file manages moved or rotated log files.
//! A limitation of this is if the log files are truncated and reused.
//!
//! The delimiter is used to separate tokens that are passed to
//! the functors.
//!
class CORE_EXPORT CTextFileWatcher : private CNonCopyable
{
    public:
        //! Function prototype. Return false to exit loop.
        typedef boost::function1<bool, std::string> TFunc;

        enum EPosition
        {
            E_Start,
            E_End
        };

    public:
        //! Unlike the CTextFileReader and CTextFileWriter classes, the
        //! CTextFileWatcher does NOT offer the option to translate CRLF to LF
        //! automatically.  This is because it uses lseek() to move around
        //! within the file it's watching, and hence cannot tolerate the
        //! operating system removing characters from the data it reads.  Users
        //! of this class requiring carriage returns be stripped from the data
        //! they see are advised to use "\r?\n" in the delimiter regex passed to
        //! the init() method of this class.
        CTextFileWatcher(size_t renamedFileAttempts = DEFAULT_RENAMED_FILE_ATTEMPTS);
        ~CTextFileWatcher(void);

        //! Initialise the watcher. One or other of these must be called before
        //! any other method.
        bool    init(const std::string &fileName,
                     const std::string &delimiter,
                     EPosition pos);

        bool    init(const std::string &fileName,
                     const CDelimiter &delimiter,
                     EPosition pos);

        //! Recheck the leftovers from the last read to the file, in case
        //! our view on whether it's delimited has changed with the passage
        //! of time.  Call a visitor function if it's a complete token.
        void    recheckLastRead(const TFunc &f, std::string &lastRead);

        //! Return the changed lines in the file and call a visitor
        //! function.  NOTE: the visitor function is called
        //! for complete delimited lines only.
        bool    changes(const TFunc &f);

        //! Read the file from the beginning and call
        //! a visitor function for all
        //! complete delimited lines only.
        //! - remainder are the characters after the last delimiter
        bool    readAllLines(const TFunc &f, std::string &remainder);

        //! Read the first line from the file and call
        //! a visitor function if it is a complete delimited line.
        //! - remainder are the characters after the last delimiter
        bool    readFirstLine(const TFunc &f);

        //! Accessor
        const std::string &fileName(void) const;

        //! Get example delimiter
        const std::string &exampleDelimiter(void) const;

    private:
        void    destroy(void);

        //! Check to see if this file has been moved (e.g log file rotation)
        //! If it has then reset pointers to new file
        bool    checkForMovedFile(void);

        //! Return the size of the file (must be open)
        static bool statSize(int fd, COsFileFuncs::TOffset &offset);

        //! Return the inode of the file (must be open)
        static bool statInode(int fd, COsFileFuncs::TIno &inode);

        //! Update the example delimiter if it's currently unset and we have a
        //! valid example
        void updateExampleDelimiter(const std::string &exampleDelimiter);

    private:
        //! The default read length
        static const size_t READ_BUFFER_SIZE;

        //! If the file we're reading is renamed, how many times should we try
        //! to reopen the original file name before giving up?
        static const size_t DEFAULT_RENAMED_FILE_ATTEMPTS;

    private:
        std::string           m_FileName;
        CDelimiter            m_Delimiter;
        int                   m_Fd;
        COsFileFuncs::TOffset m_Offset;
        COsFileFuncs::TIno    m_Inode;

        //! The characters in the last read (after the last delimiter)
        std::string           m_LastRead;

        //! If the file we're reading is renamed, and the original filename is
        //! not immediately accessible afterwards, how many times should we try
        //! to open the original filename before reporting an error?
        size_t                m_RenamedFileRetries;

        //! Number of retries remaining for reopening our original filename
        //! after a rename
        size_t                m_RenamedFileRetriesRemaining;

        //! An example of an actual delimiter we've encountered (the CDelimiter
        //! class is regex based, so doesn't record exactly which line
        //! delimiters are used in the file)
        std::string           m_ExampleDelimiter;
};


}
}

#endif // INCLUDED_ml_core_CTextFileWatcher_h

