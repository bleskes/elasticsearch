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
#include <core/CJsonDocUtils.h>


namespace prelert
{
namespace core
{


void CJsonDocUtils::addStringFieldToObj(const std::string &fieldName,
                                        const std::string &value,
                                        rapidjson::Value &obj,
                                        rapidjson::MemoryPoolAllocator<> &allocator,
                                        bool allowEmptyString)
{
    // Don't add empty strings unless explicitly told to
    if (!allowEmptyString && value.empty())
    {
        return;
    }

    rapidjson::Value v(value.c_str(),
                       static_cast<rapidjson::SizeType>(value.length()),
                       allocator);
    obj.AddMember(fieldName.c_str(), v, allocator);
}

void CJsonDocUtils::addDoubleFieldToObj(const std::string &fieldName,
                                        double value,
                                        rapidjson::Value &obj,
                                        rapidjson::MemoryPoolAllocator<> &allocator)
{
    if (!(boost::math::isfinite)(value))
    {
        LOG_ERROR("Adding " << value << " to the \"" <<
                  fieldName << "\" field of a JSON document");
        // Don't return - RapidJSON will defend itself by converting to 0
    }
    rapidjson::Value v(value);
    obj.AddMember(fieldName.c_str(), v, allocator);
}

void CJsonDocUtils::addBoolFieldToObj(const std::string &fieldName,
                                      bool value,
                                      rapidjson::Value &obj,
                                      rapidjson::MemoryPoolAllocator<> &allocator)
{
    rapidjson::Value v(value);
    obj.AddMember(fieldName.c_str(), v, allocator);
}

void CJsonDocUtils::addIntFieldToObj(const std::string &fieldName,
                                     int64_t value,
                                     rapidjson::Value &obj,
                                     rapidjson::MemoryPoolAllocator<> &allocator)
{
    rapidjson::Value v(value);
    obj.AddMember(fieldName.c_str(), v, allocator);
}

void CJsonDocUtils::addUIntFieldToObj(const std::string &fieldName,
                                      uint64_t value,
                                      rapidjson::Value &obj,
                                      rapidjson::MemoryPoolAllocator<> &allocator)
{
    rapidjson::Value v(value);
    obj.AddMember(fieldName.c_str(), v, allocator);
}

void CJsonDocUtils::addStringArrayFieldToObj(const std::string &fieldName,
                                             const TStrVec &values,
                                             rapidjson::Value &obj,
                                             rapidjson::MemoryPoolAllocator<> &allocator)
{
    rapidjson::Value array(rapidjson::kArrayType);
    array.Reserve(static_cast<rapidjson::SizeType>(values.size()), allocator);

    typedef TStrVec::const_iterator TStrVecCItr;
    for (TStrVecCItr iter = values.begin();
         iter != values.end();
         ++iter)
    {
        rapidjson::Value v(iter->c_str(),
                           static_cast<rapidjson::SizeType>(iter->length()),
                           allocator);

        array.PushBack(v, allocator);
    }

    obj.AddMember(fieldName.c_str(), array, allocator);
}

void CJsonDocUtils::addDoubleDoubleDoublePrPrArrayFieldToObj(const std::string &fieldName,
                                                             const TDoubleDoubleDoublePrPrVec &values,
                                                             rapidjson::Value &obj,
                                                             rapidjson::MemoryPoolAllocator<> &allocator)
{
    rapidjson::Value array(rapidjson::kArrayType);
    array.Reserve(static_cast<rapidjson::SizeType>(values.size()), allocator);

    bool considerLogging(true);
    typedef TDoubleDoubleDoublePrPrVec::const_iterator TDoubleDoubleDoublePrPrVecCItr;
    for (TDoubleDoubleDoublePrPrVecCItr iter = values.begin();
         iter != values.end();
         ++iter)
    {
        double firstVal = iter->first;
        CJsonDocUtils::checkArrayNumberFinite(firstVal, fieldName, considerLogging);
        array.PushBack(firstVal, allocator);
        double secondFirstVal = iter->second.first;
        CJsonDocUtils::checkArrayNumberFinite(secondFirstVal, fieldName, considerLogging);
        array.PushBack(secondFirstVal, allocator);
        double secondSecondVal = iter->second.second;
        CJsonDocUtils::checkArrayNumberFinite(secondSecondVal, fieldName, considerLogging);
        array.PushBack(secondSecondVal, allocator);
    }

    obj.AddMember(fieldName.c_str(), array, allocator);
}

void CJsonDocUtils::addDoubleDoublePrArrayFieldToObj(const std::string &firstFieldName,
                                                     const std::string &secondFieldName,
                                                     const TDoubleDoublePrVec &values,
                                                     rapidjson::Value &obj,
                                                     rapidjson::MemoryPoolAllocator<> &allocator)
{
    rapidjson::Value firstArray(rapidjson::kArrayType);
    rapidjson::Value secondArray(rapidjson::kArrayType);
    firstArray.Reserve(static_cast<rapidjson::SizeType>(values.size()), allocator);
    secondArray.Reserve(static_cast<rapidjson::SizeType>(values.size()), allocator);

    bool considerLoggingFirst(true);
    bool considerLoggingSecond(true);
    typedef TDoubleDoublePrVec::const_iterator TDoubleDoublePrVecCItr;
    for (TDoubleDoublePrVecCItr iter = values.begin();
         iter != values.end();
         ++iter)
    {
        double firstVal = iter->first;
        CJsonDocUtils::checkArrayNumberFinite(firstVal, firstFieldName, considerLoggingFirst);
        firstArray.PushBack(firstVal, allocator);
        double secondVal = iter->second;
        CJsonDocUtils::checkArrayNumberFinite(secondVal, secondFieldName, considerLoggingSecond);
        secondArray.PushBack(secondVal, allocator);
    }

    obj.AddMember(firstFieldName.c_str(), firstArray, allocator);
    obj.AddMember(secondFieldName.c_str(), secondArray, allocator);
}

void CJsonDocUtils::addTimeArrayFieldToObj(const std::string &fieldName,
                                           const TTimeVec &values,
                                           rapidjson::Value &obj,
                                           rapidjson::MemoryPoolAllocator<> &allocator)
{
    rapidjson::Value array(rapidjson::kArrayType);
    array.Reserve(static_cast<rapidjson::SizeType>(values.size()), allocator);

    typedef TTimeVec::const_iterator TTimeVecCItr;
    for (TTimeVecCItr iter = values.begin(); iter != values.end(); ++iter)
    {
        array.PushBack(static_cast<uint64_t>(*iter), allocator);
    }

    obj.AddMember(fieldName.c_str(), array, allocator);
}

void CJsonDocUtils::removeMemberIfPresent(const std::string &fieldName, rapidjson::Value &obj)
{
    if (obj.HasMember(fieldName.c_str()))
    {
        obj.RemoveMember(fieldName.c_str());
    }
}

}
}

