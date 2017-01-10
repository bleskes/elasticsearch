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

#ifndef INCLUDED_ml_model_CIndividualModelDetail_h
#define INCLUDED_ml_model_CIndividualModelDetail_h

#include <model/CDataGatherer.h>
#include <model/CIndividualModel.h>
#include <model/CProbabilityAndInfluenceCalculator.h>
#include "CStringStore.h"

namespace ml
{
namespace model
{

template<typename T>
void CIndividualModel::currentBucketPersonIds(core_t::TTime time,
                                              const T &featureData,
                                              TSizeVec &result) const
{
    typedef boost::unordered_set<std::size_t> TSizeUSet;

    result.clear();

    if (!this->bucketStatsAvailable(time))
    {
        LOG_ERROR("No statistics at " << time
                  << ", current bucket = " << this->printCurrentBucket());
        return;
    }

    TSizeUSet people;
    for (std::size_t i = 0u; i < featureData.size(); ++i)
    {
        for (std::size_t j = 0u; j < featureData[i].second.size(); ++j)
        {
            people.insert(featureData[i].second[j].first);
        }
    }
    result.reserve(people.size());
    result.assign(people.begin(), people.end());
}

template<typename T>
const T *CIndividualModel::featureData(model_t::EFeature feature,
                                       std::size_t pid,
                                       core_t::TTime time,
                                       const std::vector<std::pair<model_t::EFeature,
                                       std::vector<std::pair<std::size_t, T> > > > &featureData) const
{
    if (!this->bucketStatsAvailable(time))
    {
        LOG_ERROR("No statistics at " << time
                  << ", current bucket = " << this->printCurrentBucket());
        return 0;
    }

    typename std::vector<std::pair<model_t::EFeature,
                                   std::vector<std::pair<std::size_t, T> > > >::const_iterator i =
            std::lower_bound(featureData.begin(),
                             featureData.end(),
                             feature,
                             maths::COrderings::SFirstLess());

    if (i == featureData.end() || i->first != feature)
    {
        LOG_ERROR("No data for feature " << model_t::print(feature));
        return 0;
    }

    typename std::vector<std::pair<std::size_t, T> >::const_iterator j =
            std::lower_bound(i->second.begin(),
                             i->second.end(),
                             pid,
                             maths::COrderings::SFirstLess());
    return (j != i->second.end() && j->first == pid) ? &j->second : 0;
}

template<typename T, typename FILTER>
void CIndividualModel::sampleBucketStatistics(core_t::TTime startTime,
                                              core_t::TTime endTime,
                                              const FILTER &filter,
                                              std::vector<std::pair<model_t::EFeature, T> > &featureData,
                                              CResourceMonitor &resourceMonitor)
{
    CDataGatherer &gatherer = this->dataGatherer();

    if (!gatherer.dataAvailable(startTime))
    {
        return;
    }

    for (core_t::TTime bucketStartTime = startTime, bucketLength = gatherer.bucketLength();
         bucketStartTime < endTime;
         bucketStartTime += bucketLength)
    {
        this->CIndividualModel::sampleBucketStatistics(bucketStartTime,
                                                       bucketStartTime + bucketLength,
                                                       resourceMonitor);

        gatherer.featureData(bucketStartTime, bucketLength, featureData);
        for (std::size_t i = 0u; i < featureData.size(); ++i)
        {
            model_t::EFeature feature = featureData[i].first;
            T &data = featureData[i].second;
            LOG_TRACE(model_t::print(feature) << " data = " << core::CContainerPrinter::print(data));
            this->applyFilter(model_t::E_XF_By, false, filter, data);
        }
    }
}

//! Add the probability and influences for \p feature and \p pid.
template<typename PARAMS, typename INFLUENCES>
bool CIndividualModel::addProbabilityAndInfluences(std::size_t pid,
                                                   PARAMS &params,
                                                   const INFLUENCES &influences,
                                                   CProbabilityAndInfluenceCalculator &pJoint,
                                                   CAnnotatedProbabilityBuilder &builder) const
{
    params.s_Confidence = SEASONAL_CONFIDENCE_INTERVAL;

    if (!pJoint.addAttributeProbability(CStringStore::names().get(EMPTY_STRING),
                                        model_t::INDIVIDUAL_ANALYSIS_ATTRIBUTE_ID,
                                        1.0, // attribute probability
                                        params, builder))
    {
        LOG_ERROR("Failed to compute P(" << params.describe()
                  << ", person = " << this->personName(pid) << ")");
        return false;
    }
    else
    {
        LOG_TRACE("P(" << params.describe()
                  << ", person = " << this->personName(pid) << ") = "
                  << params.s_Probability);
    }

    if (!influences.empty())
    {
        const CDataGatherer &gatherer = this->dataGatherer();
        for (std::size_t j = 0u; j < influences.size(); ++j)
        {
            if (const CInfluenceCalculator *influenceCalculator = this->influenceCalculator(params.s_Feature, j))
            {
                pJoint.plugin(*influenceCalculator);
                pJoint.addInfluences(*(gatherer.beginInfluencers() + j), influences[j], params);
            }
        }
    }
    return true;
}

}
}

#endif // INCLUDED_ml_model_CIndividualModelDetail_h
