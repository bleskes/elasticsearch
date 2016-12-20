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

#ifndef INCLUDED_prelert_model_CPopulationModelDetail_h
#define INCLUDED_prelert_model_CPopulationModelDetail_h

#include <model/CPopulationModel.h>

#include <model/CDataGatherer.h>

namespace prelert
{
namespace model
{

template<typename T>
typename T::const_iterator CPopulationModel::find(const T &data, std::size_t pid, std::size_t cid)
{
    typename T::const_iterator result =
            std::lower_bound(data.begin(), data.end(),
                             std::make_pair(pid, cid),
                             maths::COrderings::SFirstLess());
    if (   result != data.end()
        && (   CDataGatherer::extractPersonId(*result) != pid
            || CDataGatherer::extractAttributeId(*result) != cid))
    {
        result = data.end();
    }
    return result;
}

inline CPopulationModel::TDouble1Vec
CPopulationModel::extractValue(model_t::EFeature /*feature*/,
                              const std::pair<TSizeSizePr, SEventRateFeatureData> &data)
{
    return TDouble1Vec(1, static_cast<double>(CDataGatherer::extractData(data).s_Count));
}

inline CPopulationModel::TDouble1Vec
CPopulationModel::extractValue(model_t::EFeature feature,
                               const std::pair<TSizeSizePr, SMetricFeatureData> &data)
{
    return CDataGatherer::extractData(data).s_BucketValue ?
           CDataGatherer::extractData(data).s_BucketValue->value(model_t::dimension(feature)) :
           TDouble1Vec();
}

template<typename T>
CPopulationModel::TDouble1Vec CPopulationModel::currentBucketValue(const T &featureData,
                                                                   model_t::EFeature feature,
                                                                   std::size_t pid,
                                                                   std::size_t cid,
                                                                   const TDouble1Vec &fallback) const
{
    typename T::const_iterator i =
                 model_t::isAttributeConditional(feature) ?
                 find(featureData, pid, cid) :
                 find(featureData, pid, 0);
    return i != featureData.end() ? extractValue(feature, *i) : fallback;
}

template<typename T, typename PERSON_FILTER, typename ATTRIBUTE_FILTER>
void CPopulationModel::applyFilters(model_t::EFeature feature,
                                    bool updateStatistics,
                                    const PERSON_FILTER &personFilter,
                                    const ATTRIBUTE_FILTER &attributeFilter,
                                    T &data) const
{
    std::size_t initialSize = data.size();
    if (this->params().s_ExcludeFrequent & model_t::E_XF_Over)
    {
        data.erase(std::remove_if(data.begin(), data.end(), personFilter), data.end());
    }
    if (   model_t::isAttributeConditional(feature)
        && (this->params().s_ExcludeFrequent & model_t::E_XF_By))
    {
        data.erase(std::remove_if(data.begin(), data.end(), attributeFilter), data.end());
    }
    if (updateStatistics && data.size() != initialSize)
    {
        core::CStatistics::stat(stat_t::E_NumberExcludedFrequentInvocations).increment(1);
    }
}

}
}

#endif // INCLUDED_prelert_model_CPopulationModelDetail_h
