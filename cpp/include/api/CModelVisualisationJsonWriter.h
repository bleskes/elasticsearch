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
#ifndef INCLUDED_ml_api_CModelVisualisationJsonWriter_h
#define INCLUDED_ml_api_CModelVisualisationJsonWriter_h

#include <core/CNonCopyable.h>

#include <api/CModelInspector.h>
#include <api/ImportExport.h>

#include <rapidjson/document.h>
#include <rapidjson/linewriter.h>
#include <rapidjson/prettywriter.h>
#include <rapidjson/GenericWriteStream.h>

#include <boost/shared_ptr.hpp>

#include <iosfwd>
#include <sstream>
#include <string>
#include <vector>

#include <stdint.h>


namespace ml
{
namespace api
{

//! \brief
//! Write visualisation data as a JSON document
//!
//! DESCRIPTION:\n
//! JSON is either written to the output stream or an internal
//! string stream. The various writeXXX functions convert the
//! arguments to a JSON doc and write to the stream then flush
//! the stream. If the object is constructed without an
//! outputstream then the doc can be read via the internalString
//! function.
//!
//! IMPLEMENTATION DECISIONS:\n
//! The stream is flushed after at the end of each of the public
//! write.... functions.
//!
class API_EXPORT CModelVisualisationJsonWriter : private core::CNonCopyable
{
    public:

        static const std::string MODEL;
        static const std::string TIME;
        static const std::string BASELINE;
        static const std::string UPPER_INTERVAL;
        static const std::string LOWER_INTERVAL;
        static const std::string FEATURE;
        static const std::string PERSON_FIELD_NAME;
        static const std::string PERSON_FIELD_VALUE;
        static const std::string PERSON_PROBABILITY;
        static const std::string DISTRIBUTION;
        static const std::string CONFIDENCE_INTERVALS;
        static const std::string PARTITION_NAME;
        static const std::string DETECTOR_NAME;
        static const std::string NUM_TIMESERIES;
        static const std::string NUM_ATTRIBUTES;
        static const std::string MEM_USAGE;
        static const std::string IS_POPULATION;
        static const std::string PEOPLE;
        static const std::string DESCRIPTION;
        static const std::string CATEGORY;
        static const std::string FREQUENCY;
        static const std::string CATEGORYFREQUENCIES;
        static const std::string IS_INFORMATIVE;
        static const std::string PRIOR_DESCRIPTION;

    public:
        typedef boost::shared_ptr<rapidjson::Document> TDocumentPtr;

        typedef std::vector<TDocumentPtr>              TDocumentPtrVec;
        typedef TDocumentPtrVec::iterator              TDocumentPtrVecItr;
        typedef TDocumentPtrVec::const_iterator        TDocumentPtrVecCItr;

        typedef std::vector<double>                    TDoubleVec;

        //! Size of the fixed buffer to allocate
        static const size_t FIXED_BUFFER_SIZE = 4096;



    public:
        //! Constructor that causes output to be written to the internal string
        //! stream
        CModelVisualisationJsonWriter();

        //! Constructor that causes output to be written to the specified stream
        CModelVisualisationJsonWriter(std::ostream &strmOut);

        virtual ~CModelVisualisationJsonWriter(void);

        void startArray(const std::string &name);

        void endArray(void);

        void startObject(void);

        void endObject(void);

        void string(const std::string &label);

        void uint64(uint64_t value);

        void boolean(bool value);

        void stringInt(const std::string &label, uint64_t value);


        //! Write the dump of complete data
        //void writeVisualistionDump();

        //! Write the array of result data
        void writeVisualisationData(const CModelInspector::SVisualisationData &data);

        //! Write the array of partition information
        void writePartitions(const CModelInspector::TPartitionInfoVec &partitions);

        //! Write the list of detectors
        void writeDetectors(const CModelInspector::TDetectorInfoVec &detectors);

        void writeDetectorPeople(const CModelInspector::SDetectorPeople &detectorPeople);

        //! Get the contents of the internal string stream - for use with the
        //! zero argument constructor
        std::string internalString(void) const;

    private:
        //! If we've been initialised without a specific stream, output is
        //! written to this string stream
        std::ostringstream               m_StringOutputBuf;

        //! JSON writer ostream wrapper
        rapidjson::GenericWriteStream    m_WriteStream;

        typedef rapidjson::LineWriter<rapidjson::GenericWriteStream> TGenericLineWriter;
        typedef rapidjson::PrettyWriter<rapidjson::GenericWriteStream> TPrettyWriter;

        //! JSON writer
        TPrettyWriter                    m_Writer;

        //! A buffer to initialise the rapidjson allocator with so that it
        //! doesn't have to allocate memory every time it's cleared
        char                             m_FixedBuffer[FIXED_BUFFER_SIZE];

        //! Use the same rapidjson allocator for all docs
        rapidjson::MemoryPoolAllocator<> m_JsonPoolAllocator;
};


}
}

#endif // INCLUDED_ml_api_CModelVisualisationJsonWriter_h

