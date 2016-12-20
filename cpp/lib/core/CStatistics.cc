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

#include <core/CStatistics.h>

#include <core/CLogger.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>
#include <core/CStringUtils.h>

#include <rapidjson/document.h>
#include <rapidjson/linewriter.h>
#include <rapidjson/GenericWriteStream.h>

#include <ostream>
#include <string>

namespace prelert
{
namespace core
{

namespace
{

typedef rapidjson::LineWriter<rapidjson::GenericWriteStream> TGenericLineWriter;

static const std::string NAME_TYPE("name");
static const std::string DESCRIPTION_TYPE("description");
static const std::string STAT_TYPE("value");

//! Persistence tags
static const std::string KEY_TAG("a");
static const std::string VALUE_TAG("b");

//! Helper function to add a string/int pair to JSON writer
void addStringInt(TGenericLineWriter &writer,
                  const std::string &name,
                  const std::string &description,
                  uint64_t stat)
{
    writer.StartObject();

    writer.String(NAME_TYPE.c_str(),
                  static_cast<rapidjson::SizeType>(NAME_TYPE.length()));
    writer.String(name.c_str(),
                  static_cast<rapidjson::SizeType>(name.length()));

    writer.String(DESCRIPTION_TYPE.c_str(),
                  static_cast<rapidjson::SizeType>(DESCRIPTION_TYPE.length()));
    writer.String(description.c_str(),
                  static_cast<rapidjson::SizeType>(description.length()));

    writer.String(STAT_TYPE.c_str(),
                  static_cast<rapidjson::SizeType>(STAT_TYPE.length()));
    writer.Uint64(stat);

    writer.EndObject();
}

}

CStatistics::CStatistics(void)
{
}

CStatistics &CStatistics::instance(void)
{
    return ms_Instance;
}

CStat &CStatistics::stat(int index)
{
    if (static_cast<std::size_t>(index) >= ms_Instance.m_Stats.size())
    {
        LOG_ABORT("Bad index " << index);
    }
    return ms_Instance.m_Stats[index];
}

void CStatistics::staticsAcceptPersistInserter(CStatePersistInserter &inserter)
{
    for (int i = 0; i < stat_t::E_LastEnumStat; ++i)
    {
        inserter.insertValue(KEY_TAG, i);
        inserter.insertValue(VALUE_TAG, stat(i).value());
    }
}

bool CStatistics::staticsAcceptRestoreTraverser(CStateRestoreTraverser &traverser)
{
    uint64_t value = 0;
    int key = 0;
    do
    {
        const std::string &name = traverser.name();
        if (name == KEY_TAG)
        {
            value = 0;
            if (CStringUtils::stringToType(traverser.value(),
                                           key) == false)
            {
                LOG_ERROR("Invalid key value in " << traverser.value());
                return false;
            }
        }
        else if (name == VALUE_TAG)
        {
            if (CStringUtils::stringToType(traverser.value(),
                                           value) == false)
            {
                LOG_ERROR("Invalid stat value in " << traverser.value());
                return false;
            }
            stat(key).set(value);
            key = 0;
            value = 0;
        }
    }
    while (traverser.next());

    return true;
}

CStatistics CStatistics::ms_Instance;


std::ostream &operator<<(std::ostream &o, const CStatistics &/*stats*/)
{
    rapidjson::GenericWriteStream writeStream(o);
    TGenericLineWriter writer(writeStream);

    writer.StartArray();

    addStringInt(writer,
                 "E_NumberNewPeopleNotAllowed",
                 "Number of new people not allowed",
                 CStatistics::stat(stat_t::E_NumberNewPeopleNotAllowed).value());

    addStringInt(writer,
                 "E_NumberNewPeople",
                 "Number of new people created",
                 CStatistics::stat(stat_t::E_NumberNewPeople).value());

    addStringInt(writer,
                 "E_NumberNewPeopleRecycled",
                 "Number of new people recycled into existing space",
                 CStatistics::stat(stat_t::E_NumberNewPeopleRecycled).value());

    addStringInt(writer,
                 "E_NumberApiRecordsHandled",
                 "Number of records successfully ingested into the engine API",
                 CStatistics::stat(stat_t::E_NumberApiRecordsHandled).value());

    addStringInt(writer,
                 "E_MemoryUsage",
                 "The estimated memory currently used by the engine and models",
                 CStatistics::stat(stat_t::E_MemoryUsage).value());

    addStringInt(writer,
                 "E_NumberMemoryUsageChecks",
                 "Number of times a model memory usage check has been carried out",
                 CStatistics::stat(stat_t::E_NumberMemoryUsageChecks).value());

    addStringInt(writer,
                 "E_NumberMemoryUsageEstimates",
                 "Number of times a partial memory usage estimate has been carried out",
                 CStatistics::stat(stat_t::E_NumberMemoryUsageEstimates).value());

    addStringInt(writer,
                 "E_NumberRecordsNoTimeField",
                 "Number of records that didn't contain a Time field",
                 CStatistics::stat(stat_t::E_NumberRecordsNoTimeField).value());

    addStringInt(writer,
                 "E_NumberTimeFieldConversionErrors",
                 "Number of records where the format of the time field could not be converted",
                 CStatistics::stat(stat_t::E_NumberTimeFieldConversionErrors).value());

    addStringInt(writer,
                 "E_NumberTimeOrderErrors",
                 "Number of records not in ascending time order",
                 CStatistics::stat(stat_t::E_NumberTimeOrderErrors).value());

    addStringInt(writer,
                 "E_NumberNewAttributesNotAllowed",
                 "Number of new attributes not allowed",
                 CStatistics::stat(stat_t::E_NumberNewAttributesNotAllowed).value());

    addStringInt(writer,
                 "E_NumberNewAttributes",
                 "Number of new attributes created",
                 CStatistics::stat(stat_t::E_NumberNewAttributes).value());

    addStringInt(writer,
                 "E_NumberNewAttributesRecycled",
                 "Number of new attributes recycled into existing space",
                 CStatistics::stat(stat_t::E_NumberNewAttributesRecycled).value());

    addStringInt(writer,
                 "E_NumberByFields",
                 "Number of 'by' fields within the model",
                 CStatistics::stat(stat_t::E_NumberByFields).value());

    addStringInt(writer,
                 "E_NumberOverFields",
                 "Number of 'over' fields within the model",
                 CStatistics::stat(stat_t::E_NumberOverFields).value());

    addStringInt(writer,
                 "E_NumberExcludedFrequentInvocations",
                 "The number of times 'ExcludeFrequent' has been invoked by the model",
                 CStatistics::stat(stat_t::E_NumberExcludedFrequentInvocations).value());

    addStringInt(writer,
                 "E_NumberSamplesOutsideLatencyWindow",
                 "The number of samples received outside the latency window",
                 CStatistics::stat(stat_t::E_NumberSamplesOutsideLatencyWindow).value());

    addStringInt(writer,
                 "E_NumberMemoryLimitModelCreationFailures",
                 "The number of model creation failures from being over memory limit",
                 CStatistics::stat(stat_t::E_NumberMemoryLimitModelCreationFailures).value());

    addStringInt(writer,
                 "E_NumberPrunedItems",
                 "The number of old people or attributes pruned from the models",
                 CStatistics::stat(stat_t::E_NumberPrunedItems).value());

    writer.EndArray();
    writeStream.Flush();

    return o;
}

} // core
} // prelert

