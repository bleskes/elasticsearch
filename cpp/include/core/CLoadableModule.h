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
#ifndef INCLUDED_prelert_core_CLoadableModule_h
#define INCLUDED_prelert_core_CLoadableModule_h

#include <core/CLogger.h>
#include <core/CRuntimeLoadedLibrary.h>
#include <core/ImportExport.h>

#include <boost/shared_ptr.hpp>

#include <string>


namespace prelert
{
namespace core
{


//! \brief
//! Manages dynamic loading of a shared library
//!
//! DESCRIPTION:\n
//! Manages dynamic loading of a shared library that contains
//! the implementation of a particular class.
//!
//! IMPLEMENTATION DECISIONS:\n
//! Libraries loaded by this class must:
//! 1) Be installed in ../lib relative to the program that's loading
//!    them (or the same directory as the program loading them on Windows)
//! 2) Provide a factory function with prototype:
//!    extern "C" void *createInstance(const void *)
//!    which will be used to create an instance of the class.
//!
//! It is up to the caller to ensure that all instances of the module are
//! finished with before the library is closed.
//!
class CORE_EXPORT CLoadableModule : private CRuntimeLoadedLibrary
{
    private:
        //! Type of function we'll use to create the instance
        typedef void *(*TInstanceFuncP)(const void *);

        //! The name of the function we expect to find in the loadable module
        static const std::string CREATE_FN_NAME;

    public:
        //! Module name should NOT include the "lib" prefix, nor any extension
        CLoadableModule(const std::string &moduleName);

        //! Load the module
        bool loadModule(void);

        //! Attempt to create an instance of the class implemented by the module
        template <typename CLASS, typename ARG>
        bool makeInstance(const ARG &arg,
                          boost::shared_ptr<CLASS> &ptr)
        {
            if (!this->isLoaded())
            {
                LOG_ERROR("Module " << m_ModuleName << " is not loaded");
                ptr.reset();
                return false;
            }

            TInstanceFuncP func(reinterpret_cast<TInstanceFuncP>(this->funcAddr(CREATE_FN_NAME)));
            if (func == 0)
            {
                LOG_ERROR("Unable to obtain factory function from module " <<
                          m_ModuleName);
                ptr.reset();
                return false;
            }

            // The argument is passed as a void pointer, so the createInstance()
            // function will have to cast it back to what it should be
            ptr.reset(static_cast<CLASS *>(func(&arg)));
            if (ptr == 0)
            {
                LOG_ERROR("Factory function from module " << m_ModuleName <<
                          " returned NULL");
                return false;
            }

            return true;
        }

        //! Creates the path to the given module
        static std::string createPath(const std::string &moduleName);

    private:
        //! Name of module to be loaded
        std::string m_ModuleName;
};


}
}

#endif // INCLUDED_prelert_core_CLoadableModule_h

