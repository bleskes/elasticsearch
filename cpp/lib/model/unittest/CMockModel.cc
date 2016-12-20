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

#include "CMockModel.h"

#include <model/CModelDetailsView.h>

namespace prelert
{
namespace model
{

CMockModel::CMockModel(const SModelParams &params,
                       const TDataGathererPtr &dataGatherer,
                       const TFeatureInfluenceCalculatorCPtrPrVecVec &influenceCalculators)
        : CModel(params, dataGatherer, influenceCalculators, false),
          m_IsPopulation(false)
{
}

void CMockModel::acceptPersistInserter(core::CStatePersistInserter &/*inserter*/) const
{
}

bool CMockModel::acceptRestoreTraverser(const model_t::TAnyRestoreFunc &/*extraDataRestoreFunc*/,
                                        core::CStateRestoreTraverser &/*traverser*/)
{
    return false;
}

CModel *CMockModel::cloneForPersistence(void) const
{
    return 0;
}

model_t::EModelType CMockModel::category(void) const
{
    return model_t::E_MetricOnline;
}

bool CMockModel::isPopulation(void) const
{
    return m_IsPopulation;
}

bool CMockModel::isEventRate(void) const
{
    return false;
}

bool CMockModel::isMetric(void) const
{
    return false;
}

CModel::TOptionalUInt64 CMockModel::currentBucketCount(std::size_t /*pid*/,
                                                       core_t::TTime /*time*/) const
{
    CModel::TOptionalUInt64 count;
    return count;
}

CModel::TOptionalDouble CMockModel::baselineBucketCount(std::size_t /*pid*/) const
{
    CModel::TOptionalDouble count;
    return count;
}

CMockModel::TDouble1Vec CMockModel::currentBucketValue(model_t::EFeature feature,
                                                       std::size_t pid,
                                                       std::size_t cid,
                                                       core_t::TTime time) const
{
    TFeatureSizeSizeTimeTriplePrDouble1VecUMapCItr itr =
            m_BucketValues.find(std::make_pair(feature, core::make_triple(pid, cid, time)));
    if (itr != m_BucketValues.end())
    {
        return itr->second;
    }
    return TDouble1Vec();
}

CMockModel::TDouble1Vec CMockModel::baselineBucketMean(model_t::EFeature feature,
                                                       std::size_t pid,
                                                       std::size_t cid,
                                                       model_t::CResultType /*type*/,
                                                       const TSizeDoublePr1Vec &/*correlated*/,
                                                       core_t::TTime time) const
{
    TFeatureSizeSizeTimeTriplePrDouble1VecUMapCItr itr =
             m_BucketBaselineMeans.find(std::make_pair(feature, core::make_triple(pid, cid, time)));
     if (itr != m_BucketBaselineMeans.end())
     {
         return itr->second;
     }
     return TDouble1Vec();
}

bool CMockModel::bucketStatsAvailable(core_t::TTime /*time*/) const
{
    return false;
}

void CMockModel::currentBucketPersonIds(core_t::TTime /*time*/, TSizeVec &/*result*/) const
{
}

void CMockModel::sampleBucketStatistics(core_t::TTime /*startTime*/,
                                        core_t::TTime /*endTime*/,
                                        CResourceMonitor &/*resourceMonitor*/)
{
}

void CMockModel::sample(core_t::TTime /*startTime*/,
                        core_t::TTime /*endTime*/,
                        CResourceMonitor &/*resourceMonitor*/)
{
}

void CMockModel::sampleOutOfPhase(core_t::TTime /*startTime*/,
                                  core_t::TTime /*endTime*/,
                                  CResourceMonitor &/*resourceMonitor*/)
{
}

void CMockModel::prune(std::size_t /*maximumAge*/)
{
}

bool CMockModel::computeProbability(std::size_t /*pid*/,
                                    core_t::TTime /*startTime*/,
                                    core_t::TTime /*endTime*/,
                                    CPartitioningFields &/*partitioningFields*/,
                                    std::size_t /*numberAttributeProbabilities*/,
                                    SAnnotatedProbability &/*result*/) const
{
    return false;
}

bool CMockModel::computeTotalProbability(const std::string &/*person*/,
                                         std::size_t /*numberAttributeProbabilities*/,
                                         TOptionalDouble &/*probability*/,
                                         TAttributeProbability1Vec &/*attributeProbabilities*/) const
{
    return false;
}

void CMockModel::outputCurrentBucketStatistics(const std::string &/*partitionFieldValue*/,
                                               const TBucketStatsOutputFunc &/*outputFunc*/) const
{
}

uint64_t CMockModel::checksum(bool /*includeCurrentBucketStats*/) const
{
    return 0;
}

void CMockModel::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr /*mem*/) const
{
}

std::size_t CMockModel::memoryUsage(void) const
{
    return 0;
}

std::size_t CMockModel::staticSize(void) const
{
    return 0;
}

CModel::CModelDetailsViewPtr CMockModel::details(void) const
{
    CModel::CModelDetailsViewPtr result;
    return result;
}

double CMockModel::attributeFrequency(std::size_t /*cid*/) const
{
    return 0.0;
}

core_t::TTime CMockModel::currentBucketStartTime(void) const
{
    return 0;
}

void CMockModel::currentBucketStartTime(core_t::TTime /*time*/)
{
}

void CMockModel::createNewModels(std::size_t /*n*/, std::size_t /*m*/)
{
}

void CMockModel::updateRecycledModels(void)
{
}

void CMockModel::clearPrunedResources(const TSizeVec &/*people*/,
                                      const TSizeVec &/*attributes*/)
{
}

void CMockModel::currentBucketTotalCount(uint64_t /*totalCount*/)
{
}

void CMockModel::doSkipSampling(core_t::TTime /*startTime*/, core_t::TTime /*endTime*/)
{

}

void CMockModel::mockPopulation(bool isPopulation)
{
    m_IsPopulation = isPopulation;
}

void CMockModel::mockAddBucketValue(model_t::EFeature feature,
                                    std::size_t pid,
                                    std::size_t cid,
                                    core_t::TTime time,
                                    const TDouble1Vec &value)
{
    m_BucketValues[std::make_pair(feature, core::make_triple(pid, cid, time))] = value;
}

void CMockModel::mockAddBucketBaselineMean(model_t::EFeature feature,
                                           std::size_t pid,
                                           std::size_t cid,
                                           core_t::TTime time,
                                           const TDouble1Vec &value)
{
    m_BucketBaselineMeans[std::make_pair(feature, core::make_triple(pid, cid, time))] = value;
}

}
}
