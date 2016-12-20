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

#include <api/CAnomalyDetectorUtils.h>

#include <model/CSearchKey.h>

#include <api/CFieldConfig.h>

#include <boost/make_shared.hpp>

namespace prelert
{
namespace api
{

const std::string CAnomalyDetectorUtils::EMPTY_STRING;

namespace
{

// Hold this at file scope to avoid problems with compilers that don't support
// thread-safe in-function static initialisation
const CAnomalyDetectorUtils::TKeyVec NO_KEYS;

}

model::CAnomalyDetector::TAnomalyDetectorPtr
CAnomalyDetectorUtils::makeDetector(int identifier,
                                    const model::CModelConfig &modelConfig,
                                    model::CLimits &limits,
                                    const std::string &partitionFieldValue,
                                    core_t::TTime firstTime,
                                    const model::CAnomalyDetector::TModelFactoryCPtr &modelFactory)
{
    return modelFactory->isSimpleCount() ?
           boost::make_shared<model::CSimpleCountDetector>(identifier,
                                                           modelFactory->summaryMode(),
                                                           modelConfig,
                                                           boost::ref(limits),
                                                           partitionFieldValue,
                                                           firstTime,
                                                           modelFactory) :
           boost::make_shared<model::CAnomalyDetector>(identifier,
                                                       boost::ref(limits),
                                                       modelConfig,
                                                       partitionFieldValue,
                                                       firstTime,
                                                       modelFactory);
}

const CAnomalyDetectorUtils::TKeyVec &
CAnomalyDetectorUtils::detectorKeys(const std::string &sourceName,
                                    const CFieldConfig &fieldConfig,
                                    const TStrStrUMap &dataRowFields,
                                    TStrKeyVecUMap &keys,
                                    model::CResourceMonitor &/*resourceMonitor*/)
{
    typedef std::pair<TStrKeyVecUMapItr, bool> TStrSearchKeyVecUMapItrBoolPr;

    TStrStrUMapCItr sourceItr = dataRowFields.find(sourceName);
    const std::string &sourceType(sourceItr == dataRowFields.end() ?
                                  EMPTY_STRING : sourceItr->second);

    TStrSearchKeyVecUMapItrBoolPr sourceKeys = keys.emplace(sourceType, NO_KEYS);
    if (sourceKeys.second)
    {
        const CFieldConfig::TFieldOptionsMIndex &fields = fieldConfig.fieldOptions();

        // Add a key for the simple count detector.
        sourceKeys.first->second.push_back(model::CSearchKey::simpleCountKey());

        for (CFieldConfig::TFieldOptionsMIndexCItr fieldItr = fields.begin();
             fieldItr != fields.end();
             ++fieldItr)
        {

            sourceKeys.first->second.push_back(
                    model::CSearchKey(fieldItr->configKey(),
                                      fieldItr->function(),
                                      fieldItr->useNull(),
                                      fieldItr->excludeFrequent(),
                                      fieldItr->fieldName(),
                                      fieldItr->byFieldName(),
                                      fieldItr->overFieldName(),
                                      fieldItr->partitionFieldName(),
                                      fieldConfig.influencerFieldNames()));
        }
    }

    return sourceKeys.first->second;
}


}
}

