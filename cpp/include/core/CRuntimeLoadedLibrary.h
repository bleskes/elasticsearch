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
#ifndef INCLUDED_prelert_core_CRuntimeLoadedLibrary_h
#define INCLUDED_prelert_core_CRuntimeLoadedLibrary_h

#include <core/CNonCopyable.h>
#include <core/ImportExport.h>
#include <core/WindowsSafe.h>

#include <string>

#include <stdint.h>


namespace prelert
{
namespace core
{


//! \brief
//! Manages dynamic loading of a shared library
//!
//! DESCRIPTION:\n
//! Manages dynamic loading of a shared library from a specified path.
//!
//! IMPLEMENTATION DECISIONS:\n
//! Function pointers looked-up in the library are returned as intptr_t.  The
//! caller must know the correct function pointer type to cast this to, and do
//! the cast.
//!
//! intptr_t is used because C++ doesn't allow casting between function pointers
//! and non-function pointers.  So we go via an intermediate integral type
//! that's the same size as a pointer.
//!
//! It is up to the caller to ensure that any functions provided by the library
//! are not called after the library is closed.
//!
class CORE_EXPORT CRuntimeLoadedLibrary : private CNonCopyable
{
    public:
        CRuntimeLoadedLibrary(void);
        virtual ~CRuntimeLoadedLibrary(void);

        //! Load the library from the specified path
        bool load(const std::string &libraryPath);

        //! Unload the library
        bool unload(void);

        //! Is the library loaded?
        bool isLoaded(void) const;

        //! Path to the library that was loaded.  (Empty string if no library is
        //! loaded.)
        const std::string &loadedPath(void) const;

        //! Get the address of a function from the loaded library.
        //! The caller must know the correct function pointer type to cast this
        //! to, and do the cast.
        intptr_t funcAddr(const std::string &funcName) const;

        //! Get the default library directory for Prelert libraries.
        static std::string prelertLibDir(void);

    private:
        //! Handle to the loaded library
#ifdef Windows
        HMODULE     m_LibraryHandle;
#else
        void        *m_LibraryHandle;
#endif

        //! Path of library that was loaded
        std::string m_LibraryPath;
};


}
}

#endif // INCLUDED_prelert_core_CRuntimeLoadedLibrary_h

