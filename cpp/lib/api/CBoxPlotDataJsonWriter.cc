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
#include <api/CBoxPlotDataJsonWriter.h>

#include <core/CJsonDocUtils.h>
#include <core/CLogger.h>
#include <core/CTimeUtils.h>

#include <ostream>


namespace prelert
{
namespace api
{

// JSON field names
const std::string CBoxPlotDataJsonWriter::JOB_ID("job_id");
const std::string CBoxPlotDataJsonWriter::MODEL_DEBUG("model_debug_output");
const std::string CBoxPlotDataJsonWriter::PARTITION_FIELD_NAME("partition_field_name");
const std::string CBoxPlotDataJsonWriter::PARTITION_FIELD_VALUE("partition_field_value");
const std::string CBoxPlotDataJsonWriter::TIME("timestamp");
const std::string CBoxPlotDataJsonWriter::FEATURE("debug_feature");
const std::string CBoxPlotDataJsonWriter::BY("by");
const std::string CBoxPlotDataJsonWriter::BY_FIELD_NAME("by_field_name");
const std::string CBoxPlotDataJsonWriter::BY_FIELD_VALUE("by_field_value");
const std::string CBoxPlotDataJsonWriter::OVER_FIELD_NAME("over_field_name");
const std::string CBoxPlotDataJsonWriter::OVER_FIELD_VALUE("over_field_value");
const std::string CBoxPlotDataJsonWriter::LOWER("debug_lower");
const std::string CBoxPlotDataJsonWriter::UPPER("debug_upper");
const std::string CBoxPlotDataJsonWriter::MEDIAN("debug_median");
const std::string CBoxPlotDataJsonWriter::ACTUAL("actual");

CBoxPlotDataJsonWriter::CBoxPlotDataJsonWriter(std::ostream &strmOut)
    : m_InternalWriteStream(new rapidjson::GenericWriteStream(strmOut)),
      m_InternalWriter(new TGenericLineWriter(*m_InternalWriteStream)),
      m_Writer(*m_InternalWriter),
      m_JsonPoolAllocator(m_FixedBuffer, FIXED_BUFFER_SIZE)
{
}

CBoxPlotDataJsonWriter::CBoxPlotDataJsonWriter(TGenericLineWriter &writer)
    : m_Writer(writer),
      m_JsonPoolAllocator(m_FixedBuffer, FIXED_BUFFER_SIZE)
{
}

void CBoxPlotDataJsonWriter::writeFlat(const std::string &jobId, const model::CBoxPlotData &data)
{
    const std::string &partitionFieldName = data.partitionFieldName();
    const std::string &partitionFieldValue = data.partitionFieldValue();
    const std::string &overFieldName = data.overFieldName();
    const std::string &byFieldName = data.byFieldName();
    core_t::TTime time = data.time();

    for (TFeatureStrByFieldDataUMapUMapCItr featureItr = data.begin();
         featureItr != data.end();
         ++featureItr)
    {
        std::string feature = model_t::print(featureItr->first);
        const TStrByFieldDataUMap &byDataMap = featureItr->second;
        for (TStrByFieldDataUMapCItr byItr = byDataMap.begin(); byItr != byDataMap.end(); ++byItr)
        {
            const std::string &byFieldValue = byItr->first;
            const TByFieldData &byData = byItr->second;
            const TStrDoublePrVec &values = byData.s_ValuesPerOverField;
            if (values.empty())
            {
                rapidjson::Value doc(rapidjson::kObjectType);
                this->writeFlatRow(time, jobId, partitionFieldName, partitionFieldValue, feature,
                        byFieldName, byFieldValue, byData, doc);

                rapidjson::Value wrapper(rapidjson::kObjectType);
                wrapper.AddMember(MODEL_DEBUG.c_str(), doc, m_JsonPoolAllocator);
                wrapper.Accept(m_Writer);
            }
            else
            {
                for (std::size_t valueIndex = 0; valueIndex < values.size(); ++valueIndex)
                {
                    const TStrDoublePr &keyValue = values[valueIndex];
                    rapidjson::Value doc(rapidjson::kObjectType);
                    this->writeFlatRow(time, jobId, partitionFieldName, partitionFieldValue, feature,
                            byFieldName, byFieldValue, byData, doc);
                    if (!overFieldName.empty())
                    {
                        core::CJsonDocUtils::addStringFieldToObj(OVER_FIELD_NAME, overFieldName, doc,
                                m_JsonPoolAllocator);
                        core::CJsonDocUtils::addStringFieldToObj(OVER_FIELD_VALUE, keyValue.first, doc,
                                m_JsonPoolAllocator, true);
                    }
                    core::CJsonDocUtils::addDoubleFieldToObj(ACTUAL, keyValue.second, doc,
                            m_JsonPoolAllocator);

                    rapidjson::Value wrapper(rapidjson::kObjectType);
                    wrapper.AddMember(MODEL_DEBUG.c_str(), doc, m_JsonPoolAllocator);
                    wrapper.Accept(m_Writer);
                }
            }
        }
    }

    if (m_InternalWriteStream != 0)
    {
        m_InternalWriteStream->Flush();
    }
}

void CBoxPlotDataJsonWriter::writeFlatRow(core_t::TTime time,
                                          const std::string &jobId,
                                          const std::string &partitionFieldName,
                                          const std::string &partitionFieldValue,
                                          const std::string &feature,
                                          const std::string &byFieldName,
                                          const std::string &byFieldValue,
                                          const TByFieldData &byData,
                                          rapidjson::Value &doc)
{
    core::CJsonDocUtils::addStringFieldToObj(JOB_ID, jobId, doc, m_JsonPoolAllocator, true);
    core::CJsonDocUtils::addStringFieldToObj(FEATURE, feature, doc, m_JsonPoolAllocator, true);
    // time is in Java format - milliseconds since the epoch
    core::CJsonDocUtils::addIntFieldToObj(TIME, time * 1000,
            doc, m_JsonPoolAllocator);
    if (!partitionFieldName.empty())
    {
        core::CJsonDocUtils::addStringFieldToObj(PARTITION_FIELD_NAME, partitionFieldName, doc,
                m_JsonPoolAllocator);
        core::CJsonDocUtils::addStringFieldToObj(PARTITION_FIELD_VALUE, partitionFieldValue, doc,
                m_JsonPoolAllocator, true);
    }
    if (!byFieldName.empty())
    {
        core::CJsonDocUtils::addStringFieldToObj(BY_FIELD_NAME, byFieldName, doc,
                m_JsonPoolAllocator);
        core::CJsonDocUtils::addStringFieldToObj(BY_FIELD_VALUE, byFieldValue, doc,
                m_JsonPoolAllocator, true);
    }
    core::CJsonDocUtils::addDoubleFieldToObj(LOWER, byData.s_LowerBound, doc, m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleFieldToObj(UPPER, byData.s_UpperBound, doc, m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleFieldToObj(MEDIAN, byData.s_Median, doc, m_JsonPoolAllocator);
}

void CBoxPlotDataJsonWriter::writeByData(const std::string &byFieldName,
                                         const TStrByFieldDataUMap &data,
                                         rapidjson::Value &byArray)
{
    for (TStrByFieldDataUMapCItr byItr = data.begin(); byItr != data.end(); ++byItr)
    {
        const TByFieldData &byData = byItr->second;

        rapidjson::Value byDoc(rapidjson::kObjectType);
        if (!byFieldName.empty())
        {
            core::CJsonDocUtils::addStringFieldToObj(BY_FIELD_NAME, byFieldName, byDoc,
                                m_JsonPoolAllocator);
            core::CJsonDocUtils::addStringFieldToObj(BY_FIELD_VALUE, byItr->first, byDoc,
                                m_JsonPoolAllocator, true);
        }
        core::CJsonDocUtils::addDoubleFieldToObj(LOWER, byData.s_LowerBound, byDoc,
                m_JsonPoolAllocator);
        core::CJsonDocUtils::addDoubleFieldToObj(UPPER, byData.s_UpperBound, byDoc,
                            m_JsonPoolAllocator);
        core::CJsonDocUtils::addDoubleFieldToObj(MEDIAN, byData.s_Median, byDoc,
                            m_JsonPoolAllocator);

        rapidjson::Value valuesArray(rapidjson::kArrayType);
        this->writeActualValues(byData.s_ValuesPerOverField, valuesArray);
        byDoc.AddMember(ACTUAL.c_str(), valuesArray, m_JsonPoolAllocator);

        byArray.PushBack(byDoc, m_JsonPoolAllocator);
    }
}

void CBoxPlotDataJsonWriter::writeActualValues(const TStrDoublePrVec &values,
                                               rapidjson::Value &valuesArray)
{
    for (std::size_t valueIndex = 0; valueIndex < values.size(); ++valueIndex)
    {
        rapidjson::Value valueDoc(rapidjson::kObjectType);
        const TStrDoublePr &keyValue = values[valueIndex];
        core::CJsonDocUtils::addDoubleFieldToObj(keyValue.first, keyValue.second, valueDoc,
                m_JsonPoolAllocator);
        valuesArray.PushBack(valueDoc, m_JsonPoolAllocator);
    }
}

}
}
