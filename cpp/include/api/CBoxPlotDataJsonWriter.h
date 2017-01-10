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
#ifndef INCLUDED_ml_api_CBoxPlotDataJsonWriter_h
#define INCLUDED_ml_api_CBoxPlotDataJsonWriter_h

#include <core/CoreTypes.h>
#include <core/CNonCopyable.h>

#include <api/ImportExport.h>

#include <model/CBoxPlotData.h>

#include <rapidjson/document.h>
#include <rapidjson/linewriter.h>
#include <rapidjson/GenericWriteStream.h>

#include <boost/scoped_ptr.hpp>

#include <iosfwd>
#include <sstream>
#include <string>

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
class API_EXPORT CBoxPlotDataJsonWriter : private core::CNonCopyable
{
    private:

        static const std::string JOB_ID;
        static const std::string MODEL_DEBUG;
        static const std::string PARTITION_FIELD_NAME;
        static const std::string PARTITION_FIELD_VALUE;
        static const std::string TIME;
        static const std::string FEATURE;
        static const std::string BY;
        static const std::string BY_FIELD_NAME;
        static const std::string BY_FIELD_VALUE;
        static const std::string OVER_FIELD_NAME;
        static const std::string OVER_FIELD_VALUE;
        static const std::string LOWER;
        static const std::string UPPER;
        static const std::string MEDIAN;
        static const std::string ACTUAL;

    public:
        typedef model::CBoxPlotData::TStrDoublePrVec TStrDoublePrVec;
        typedef model::CBoxPlotData::SByFieldData TByFieldData;
        typedef model::CBoxPlotData::TStrByFieldDataUMap TStrByFieldDataUMap;
        typedef TStrByFieldDataUMap::const_iterator TStrByFieldDataUMapCItr;
        typedef model::CBoxPlotData::TFeatureStrByFieldDataUMapUMapCItr TFeatureStrByFieldDataUMapUMapCItr;
        typedef model::CBoxPlotData::TStrDoublePr TStrDoublePr;

        typedef rapidjson::LineWriter<rapidjson::GenericWriteStream> TGenericLineWriter;

        //! Size of the fixed buffer to allocate
        static const size_t FIXED_BUFFER_SIZE = 4096;

    public:
        //! Constructor that causes output to be written to the specified stream
        CBoxPlotDataJsonWriter(std::ostream &strmOut);

        //! Constructor that causes output to be written by a pre-existing
        //! output writer
        CBoxPlotDataJsonWriter(TGenericLineWriter &writer);

        void writeFlat(const std::string &jobId, const model::CBoxPlotData &data);

    private:
        void writeFlatRow(core_t::TTime time,
                          const std::string &jobId,
                          const std::string &partitionFieldName,
                          const std::string &partitionFieldValue,
                          const std::string &feature,
                          const std::string &byFieldName,
                          const std::string &byFieldValue,
                          const TByFieldData &byData,
                          rapidjson::Value &doc);
        void writeByData(const std::string &byFieldName,
                         const TStrByFieldDataUMap &data,
                         rapidjson::Value &byArray);
        void writeActualValues(const TStrDoublePrVec &values, rapidjson::Value &valuesArray);

    private:
        typedef boost::scoped_ptr<rapidjson::GenericWriteStream> TScopedGenericWriteStreamPtr;

        //! JSON writer ostream wrapper
        TScopedGenericWriteStreamPtr     m_InternalWriteStream;

        typedef boost::scoped_ptr<TGenericLineWriter> TScopedGenericLineWriterPtr;

        TScopedGenericLineWriterPtr      m_InternalWriter;

        //! Reference to the JSON writer we'll use
        TGenericLineWriter               &m_Writer;

        //! A buffer to initialise the rapidjson allocator with so that it
        //! doesn't have to allocate memory every time it's cleared
        char                             m_FixedBuffer[FIXED_BUFFER_SIZE];

        //! Use the same rapidjson allocator for all docs
        rapidjson::MemoryPoolAllocator<> m_JsonPoolAllocator;
};


}
}

#endif // INCLUDED_ml_api_CBoxPlotDataJsonWriter_h

