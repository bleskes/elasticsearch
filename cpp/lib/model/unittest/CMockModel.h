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

#ifndef INCLUDED_CMockModel_h
#define INCLUDED_CMockModel_h

#include <core/CTriple.h>

#include <model/CModel.h>

#include <boost/unordered_map.hpp>

#include <utility>

namespace prelert
{
namespace model
{

class CMockModel : public CModel
{
    public:
        CMockModel(const SModelParams &params,
                   const TDataGathererPtr &dataGatherer,
                   const TFeatureInfluenceCalculatorCPtrPrVecVec &influenceCalculators);

        virtual void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

        virtual bool acceptRestoreTraverser(const model_t::TAnyRestoreFunc &extraDataRestoreFunc,
                                            core::CStateRestoreTraverser &traverser);

        virtual CModel *cloneForPersistence(void) const;

        virtual model_t::EModelType category(void) const;

        virtual bool isPopulation(void) const;

        virtual bool isEventRate(void) const;

        virtual bool isMetric(void) const;

        virtual TOptionalUInt64 currentBucketCount(std::size_t pid,
                                                   core_t::TTime time) const;

        virtual TOptionalDouble baselineBucketCount(std::size_t pid) const;

        virtual TDouble1Vec currentBucketValue(model_t::EFeature feature,
                                               std::size_t pid,
                                               std::size_t cid,
                                               core_t::TTime time) const;

        virtual TDouble1Vec baselineBucketMean(model_t::EFeature feature,
                                               std::size_t pid,
                                               std::size_t cid,
                                               model_t::CResultType type,
                                               const TSizeDoublePr1Vec &correlated,
                                               core_t::TTime time) const;

        virtual bool bucketStatsAvailable(core_t::TTime time) const;

        virtual void currentBucketPersonIds(core_t::TTime time, TSizeVec &result) const;

        virtual void sampleBucketStatistics(core_t::TTime startTime,
                                            core_t::TTime endTime,
                                            CResourceMonitor &resourceMonitor);

        virtual void sample(core_t::TTime startTime,
                            core_t::TTime endTime,
                            CResourceMonitor &resourceMonitor);

        virtual void sampleOutOfPhase(core_t::TTime startTime,
                                      core_t::TTime endTime,
                                      CResourceMonitor &resourceMonitor);

        virtual void prune(std::size_t maximumAge);

        virtual bool computeProbability(std::size_t pid,
                                        core_t::TTime startTime,
                                        core_t::TTime endTime,
                                        CPartitioningFields &partitioningFields,
                                        std::size_t numberAttributeProbabilities,
                                        SAnnotatedProbability &result) const;

        virtual bool computeTotalProbability(const std::string &person,
                                             std::size_t numberAttributeProbabilities,
                                             TOptionalDouble &probability,
                                             TAttributeProbability1Vec &attributeProbabilities) const;

        virtual void outputCurrentBucketStatistics(const std::string &partitionFieldValue,
                                                   const TBucketStatsOutputFunc &outputFunc) const;

        virtual uint64_t checksum(bool includeCurrentBucketStats = true) const;

        virtual void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const;

        virtual std::size_t memoryUsage(void) const;

        virtual std::size_t staticSize(void) const;

        virtual CModelDetailsViewPtr details(void) const;

        virtual double attributeFrequency(std::size_t cid) const;

        // Setter methods to allow mocking

        void mockPopulation(bool isPopulation);

        void mockAddBucketValue(model_t::EFeature feature,
                                std::size_t pid,
                                std::size_t cid,
                                core_t::TTime time,
                                const TDouble1Vec &value);

        void mockAddBucketBaselineMean(model_t::EFeature feature,
                                       std::size_t pid,
                                       std::size_t cid,
                                       core_t::TTime time,
                                       const TDouble1Vec &value);

    protected:
        virtual core_t::TTime currentBucketStartTime(void) const;
        virtual void currentBucketStartTime(core_t::TTime time);
        virtual void createNewModels(std::size_t n, std::size_t m);
        virtual void updateRecycledModels(void);
        virtual void clearPrunedResources(const TSizeVec &people,
                                          const TSizeVec &attributes);

    private:
        virtual void currentBucketTotalCount(uint64_t totalCount);
        virtual void doSkipSampling(core_t::TTime startTime, core_t::TTime endTime);

    private:
        typedef CModel::TDouble1Vec TDouble1Vec;
        typedef core::CTriple<std::size_t, std::size_t, core_t::TTime> TSizeSizeTimeTriple;
        typedef std::pair<model_t::EFeature, TSizeSizeTimeTriple> TFeatureSizeSizeTimeTriplePr;
        typedef boost::unordered_map<TFeatureSizeSizeTimeTriplePr, TDouble1Vec> TFeatureSizeSizeTimeTriplePrDouble1VecUMap;
        typedef TFeatureSizeSizeTimeTriplePrDouble1VecUMap::const_iterator TFeatureSizeSizeTimeTriplePrDouble1VecUMapCItr;

    private:
        bool m_IsPopulation;
        TFeatureSizeSizeTimeTriplePrDouble1VecUMap m_BucketValues;
        TFeatureSizeSizeTimeTriplePrDouble1VecUMap m_BucketBaselineMeans;
};


}
}

#endif // INCLUDED_CCountingModelTest_h
