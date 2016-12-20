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
#ifndef INCLUDED_prelert_api_CMultiFileDataAdder_h
#define INCLUDED_prelert_api_CMultiFileDataAdder_h

#include <core/CDataAdder.h>

#include <api/ImportExport.h>

#include <string>

namespace prelert
{
namespace api
{

//! \brief
//! A file based persister for writing Prelert models.
//!
//! DESCRIPTION:\n
//! Output file paths are a concatenation of the baseFilename
//! passed to the constructor, the "index" argument to the
//! persistence method, the "sourceType" argument to the
//! persistence method, the "key" argument to the persistence
//! method and the file extension passed to the constructor (default
//! '.json').
//!
//! IMPLEMENTATION DECISIONS:\n
//! Only stream-based methods are presented here, as persistence
//! with large models can run to many gigabytes of data
//!
//! Data is added in multiple streams, each written to a different
//! file.  For an option to persist an entire model to a single
//! file, use the CSingleStreamDataAdder class.
//!
class API_EXPORT CMultiFileDataAdder : public core::CDataAdder
{
    public:
        //! Default file extension for persisted files.
        static const std::string JSON_FILE_EXT;

    public:
        //! Constructor uses the pass-by-value-and-swap idiom
        CMultiFileDataAdder(std::string baseFilename,
                            std::string fileExtension = JSON_FILE_EXT);

        //! Add streamed data
        //! \param key File name (without extension)
        //! \param sourceType Sub-directory name
        virtual TOStreamP addStreamed(const std::string &index,
                                      const std::string &sourceType,
                                      const std::string &key);

        //! Clients that get a stream using addStreamed() must call this
        //! method one they've finished sending data to the stream.
        virtual bool streamComplete(TOStreamP &strm,
                                    bool force);

        //! For file persistence just use the current document number as the
        //! document ID - assume the index and type (which correspond to
        //! sub-directories in the file system) provide enough differentiation
        //! between different persists.
        virtual std::string makeCurrentDocId(const std::string &baseId,
                                             size_t currentDocNum) const;

    private:
        //! Make a file name of the form base/index/sourceType/key.extension
        std::string makeFilename(const std::string &index,
                                 const std::string &sourceType,
                                 const std::string &key) const;

    private:
        //! Name of the file to serialise models to
        std::string m_BaseFilename;

        //! The extension for the peristed files
        std::string m_FileExtension;
};


}
}

#endif // INCLUDED_prelert_api_CMultiFileDataAdder_h

