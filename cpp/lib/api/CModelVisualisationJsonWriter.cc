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
#include <api/CModelVisualisationJsonWriter.h>

#include <core/CJsonDocUtils.h>
#include <core/CStringUtils.h>
#include <core/CTimeUtils.h>

#include <ostream>


namespace ml
{
namespace api
{

// JSON field names
const std::string CModelVisualisationJsonWriter::MODEL("model");
const std::string CModelVisualisationJsonWriter::TIME("time");
const std::string CModelVisualisationJsonWriter::BASELINE("baseline");
const std::string CModelVisualisationJsonWriter::UPPER_INTERVAL("upperInterval");
const std::string CModelVisualisationJsonWriter::LOWER_INTERVAL("lowerInterval");
const std::string CModelVisualisationJsonWriter::FEATURE("feature");
const std::string CModelVisualisationJsonWriter::DISTRIBUTION("distribution");
const std::string CModelVisualisationJsonWriter::CONFIDENCE_INTERVALS("confidenceIntervals");
const std::string CModelVisualisationJsonWriter::PARTITION_NAME("partitionName");
const std::string CModelVisualisationJsonWriter::DETECTOR_NAME("detectorName");
const std::string CModelVisualisationJsonWriter::NUM_TIMESERIES("numberOfTimeSeries");
const std::string CModelVisualisationJsonWriter::NUM_ATTRIBUTES("numberOfAttributes");
const std::string CModelVisualisationJsonWriter::MEM_USAGE("memoryUsage");
const std::string CModelVisualisationJsonWriter::IS_POPULATION("isPopulation");
const std::string CModelVisualisationJsonWriter::PEOPLE("people");
const std::string CModelVisualisationJsonWriter::PERSON_FIELD_NAME("personFieldName");
const std::string CModelVisualisationJsonWriter::PERSON_FIELD_VALUE("personFieldValue");
const std::string CModelVisualisationJsonWriter::PERSON_PROBABILITY("personProbability");
const std::string CModelVisualisationJsonWriter::DESCRIPTION("description");
const std::string CModelVisualisationJsonWriter::CATEGORYFREQUENCIES("categoryFrequencies");
const std::string CModelVisualisationJsonWriter::CATEGORY("category");
const std::string CModelVisualisationJsonWriter::FREQUENCY("frequency");
const std::string CModelVisualisationJsonWriter::IS_INFORMATIVE("isInformative");
const std::string CModelVisualisationJsonWriter::PRIOR_DESCRIPTION("priorDescription");



CModelVisualisationJsonWriter::CModelVisualisationJsonWriter(void)
    : m_WriteStream(m_StringOutputBuf),
      m_Writer(m_WriteStream),
      m_JsonPoolAllocator(m_FixedBuffer, FIXED_BUFFER_SIZE)
{
    // Don't write any output in the constructor because, the way things work at
    // the moment, the output stream might be redirected after construction
}

CModelVisualisationJsonWriter::CModelVisualisationJsonWriter(std::ostream &strmOut)
    : m_WriteStream(strmOut),
      m_Writer(m_WriteStream),
      m_JsonPoolAllocator(m_FixedBuffer, FIXED_BUFFER_SIZE)
{
    // Don't write any output in the constructor because, the way things work at
    // the moment, the output stream might be redirected after construction
}

CModelVisualisationJsonWriter::~CModelVisualisationJsonWriter(void)
{
}

void CModelVisualisationJsonWriter::startArray(const std::string &name)
{
    m_Writer.String(name.c_str(),
        static_cast<rapidjson::SizeType>(name.length()));

    m_Writer.StartArray();
}

void CModelVisualisationJsonWriter::endArray(void)
{
    m_Writer.EndArray();
}

void CModelVisualisationJsonWriter::startObject(void)
{
    m_Writer.StartObject();
}

void CModelVisualisationJsonWriter::endObject(void)
{
    m_Writer.EndObject();
}

void CModelVisualisationJsonWriter::string(const std::string &label)
{
    m_Writer.String(label.c_str(),
        static_cast<rapidjson::SizeType>(label.length()));
}

void CModelVisualisationJsonWriter::uint64(uint64_t value)
{
    m_Writer.Uint64(value);
}

void CModelVisualisationJsonWriter::boolean(bool value)
{
    m_Writer.Bool(value);
}

void CModelVisualisationJsonWriter::stringInt(const std::string &label, uint64_t value)
{
    this->string(label);
    this->uint64(value);
}

void CModelVisualisationJsonWriter::writePartitions(
                                const CModelInspector::TPartitionInfoVec &partitions)
{
    rapidjson::Value array(rapidjson::kArrayType);

    for (CModelInspector::TPartitionInfoVecCItr itr = partitions.begin();
         itr != partitions.end();
         itr++)
    {
        rapidjson::Value doc(rapidjson::kObjectType);

        core::CJsonDocUtils::addStringFieldToObj(PARTITION_NAME, itr->s_PartitionName, doc,
                                                m_JsonPoolAllocator);
        core::CJsonDocUtils::addUIntFieldToObj(NUM_TIMESERIES, itr->s_NumberTimeSeries, doc,
                                                m_JsonPoolAllocator);
        core::CJsonDocUtils::addUIntFieldToObj(NUM_ATTRIBUTES, itr->s_NumberAttributes, doc,
                                                m_JsonPoolAllocator);
        core::CJsonDocUtils::addUIntFieldToObj(MEM_USAGE, itr->s_MemoryUsage, doc,
                                                m_JsonPoolAllocator);

        if (itr->s_IsInformative)
        {
            core::CJsonDocUtils::addBoolFieldToObj(IS_INFORMATIVE, *(itr->s_IsInformative),
                                                 doc, m_JsonPoolAllocator);
        }

        array.PushBack(doc, m_JsonPoolAllocator);
    }

    array.Accept(m_Writer);
    m_WriteStream.Flush();
}

void CModelVisualisationJsonWriter::writeDetectors(
                                const CModelInspector::TDetectorInfoVec &detectors)
{
    rapidjson::Value array(rapidjson::kArrayType);

    for (CModelInspector::TDetectorInfoVecCItr itr = detectors.begin();
         itr != detectors.end();
         itr++)
    {
        rapidjson::Value doc(rapidjson::kObjectType);

        core::CJsonDocUtils::addStringFieldToObj(DETECTOR_NAME, itr->s_DetectorName, doc,
                                                    m_JsonPoolAllocator);
        core::CJsonDocUtils::addUIntFieldToObj(NUM_TIMESERIES, itr->s_NumberTimeSeries, doc,
                                                    m_JsonPoolAllocator);
        core::CJsonDocUtils::addUIntFieldToObj(NUM_ATTRIBUTES, itr->s_NumberAttributes, doc,
                                                    m_JsonPoolAllocator);
        core::CJsonDocUtils::addUIntFieldToObj(MEM_USAGE, itr->s_MemoryUsage, doc,
                                                    m_JsonPoolAllocator);
        if (itr->s_IsInformative)
        {
            core::CJsonDocUtils::addBoolFieldToObj(IS_INFORMATIVE, *(itr->s_IsInformative),
                                                 doc, m_JsonPoolAllocator);
        }

        array.PushBack(doc, m_JsonPoolAllocator);
    }

    array.Accept(m_Writer);
    m_WriteStream.Flush();
}

void CModelVisualisationJsonWriter::writeDetectorPeople(
                        const CModelInspector::SDetectorPeople &detectorPeople)
{
    rapidjson::Value doc(rapidjson::kObjectType);

    core::CJsonDocUtils::addStringFieldToObj(DESCRIPTION, detectorPeople.s_DetectorName, doc,
                                                m_JsonPoolAllocator);
    core::CJsonDocUtils::addStringFieldToObj(PERSON_FIELD_NAME, detectorPeople.s_PersonFieldName,
                                                doc, m_JsonPoolAllocator);
    core::CJsonDocUtils::addBoolFieldToObj(IS_POPULATION, detectorPeople.s_IsPopulation, doc,
                                                m_JsonPoolAllocator);
    core::CJsonDocUtils::addStringArrayFieldToObj(PEOPLE, detectorPeople.s_PeopleNames, doc,
                                                m_JsonPoolAllocator);

    if (detectorPeople.s_PersonProbabilities)
    {
        core::CJsonDocUtils::addDoubleArrayFieldToObj(PERSON_PROBABILITY,
                                                *detectorPeople.s_PersonProbabilities,
                                                doc, m_JsonPoolAllocator);
    }

    if (detectorPeople.s_Distributions)
    {
        rapidjson::Value array(rapidjson::kArrayType);
        array.Reserve(static_cast<rapidjson::SizeType>(detectorPeople.s_Distributions->size()),
                    m_JsonPoolAllocator);

        for (CModelInspector::TStrPopulationDistributionVecPrVec::const_iterator iter =
                                            detectorPeople.s_Distributions->begin();
             iter != detectorPeople.s_Distributions->end();
             ++iter)
        {
            rapidjson::Value feature(rapidjson::kObjectType);

            core::CJsonDocUtils::addStringFieldToObj(FEATURE, iter->first, feature,
                                                    m_JsonPoolAllocator);

            rapidjson::Value attributeDistArray(rapidjson::kArrayType);

            CModelInspector::TPopulationDistributionVec::const_iterator attributeIter = iter->second.begin();
            for (; attributeIter != iter->second.end(); ++attributeIter)
            {
                rapidjson::Value attr(rapidjson::kObjectType);
                core::CJsonDocUtils::addStringFieldToObj(PERSON_FIELD_NAME,
                                attributeIter->s_EntityName, attr, m_JsonPoolAllocator, true);

                core::CJsonDocUtils::addDoubleFieldToObj(LOWER_INTERVAL,
                                attributeIter->s_LowerBound, attr, m_JsonPoolAllocator);
                core::CJsonDocUtils::addDoubleFieldToObj(UPPER_INTERVAL,
                                attributeIter->s_UpperBound, attr, m_JsonPoolAllocator);

                core::CJsonDocUtils::addDoubleArrayFieldToObj(DISTRIBUTION,
                                                            attributeIter->s_Distribution,
                                                            attr, m_JsonPoolAllocator);

                attributeDistArray.PushBack(attr, m_JsonPoolAllocator);
            }

            feature.AddMember("attributes", attributeDistArray, m_JsonPoolAllocator);

            array.PushBack(feature, m_JsonPoolAllocator);
        }

        doc.AddMember("distributions", array, m_JsonPoolAllocator);
    }


    if (detectorPeople.s_CategoryFrequencies)
    {
        rapidjson::Value array(rapidjson::kArrayType);
        array.Reserve(static_cast<rapidjson::SizeType>(detectorPeople.s_CategoryFrequencies->size()),
                                    m_JsonPoolAllocator);

        for (CModelInspector::TStrDoublePrVec::const_iterator iter =
                                            detectorPeople.s_CategoryFrequencies->begin();
             iter != detectorPeople.s_CategoryFrequencies->end();
             ++iter)
        {
            rapidjson::Value obj(rapidjson::kObjectType);
            core::CJsonDocUtils::addStringFieldToObj(CATEGORY, iter->first, obj,
                                                    m_JsonPoolAllocator);

            core::CJsonDocUtils::addDoubleFieldToObj(FREQUENCY, iter->second, obj,
                                                    m_JsonPoolAllocator);

            array.PushBack(obj, m_JsonPoolAllocator);
        }

        doc.AddMember(CATEGORYFREQUENCIES.c_str(), array, m_JsonPoolAllocator);
        core::CJsonDocUtils::addStringFieldToObj("categoryDescription",
                            *detectorPeople.s_CategoriesDescription, doc, m_JsonPoolAllocator);
    }


    doc.Accept(m_Writer);
    m_WriteStream.Flush();
}

void CModelVisualisationJsonWriter::writeVisualisationData(
                                    const CModelInspector::SVisualisationData &data)
{
    rapidjson::Value doc(rapidjson::kObjectType);

    core::CJsonDocUtils::addStringFieldToObj(FEATURE, model_t::print(data.s_Feature), doc,
                                                m_JsonPoolAllocator);
    core::CJsonDocUtils::addBoolFieldToObj(IS_INFORMATIVE, data.s_IsInformative, doc,
                                                m_JsonPoolAllocator);
    core::CJsonDocUtils::addStringFieldToObj(PRIOR_DESCRIPTION, data.s_PriorDescription, doc,
                                                m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleArrayFieldToObj(BASELINE, data.s_Baseline, doc,
                                                    m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleArrayFieldToObj(LOWER_INTERVAL, data.s_LowerBound, doc,
                                            m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleArrayFieldToObj(UPPER_INTERVAL, data.s_UpperBound, doc,
                                            m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleArrayFieldToObj(DISTRIBUTION, data.s_Distribution, doc,
                                                m_JsonPoolAllocator);
    core::CJsonDocUtils::addTimeArrayFieldToObj(TIME, data.s_Time, doc, m_JsonPoolAllocator);
    core::CJsonDocUtils::addDoubleDoubleDoublePrPrArrayFieldToObj(CONFIDENCE_INTERVALS,
                                data.s_ConfidenceIntervals, doc, m_JsonPoolAllocator);

    doc.Accept(m_Writer);
    m_WriteStream.Flush();
}

std::string CModelVisualisationJsonWriter::internalString(void) const
{
    // This is only of any value if the first constructor was used - it's up to
    // the caller to know this
    const_cast<rapidjson::GenericWriteStream &>(m_WriteStream).Flush();
    return m_StringOutputBuf.str();
}


}
}

