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

#ifndef INCLUDED_ml_model_CMetricModel_h
#define INCLUDED_ml_model_CMetricModel_h

#include <core/CMemory.h>
#include <core/CoreTypes.h>

#include <model/CFeatureData.h>
#include <model/CIndividualModel.h>
#include <model/CProbabilityAndInfluenceCalculator.h>
#include <model/ImportExport.h>
#include <model/ModelTypes.h>

#include <string>
#include <utility>
#include <vector>

#include <stdint.h>


namespace
{
class CMockMetricModel;
}
namespace ml
{
namespace core
{
class CStatePersistInserter;
class CStateRestoreTraverser;
}
namespace model
{
class CModelFactory;

//! \brief The metric model common functionality.
//!
//! DESCRIPTION:\n
//! It holds various statistics about the current bucketing time interval.
//! This model is used for computing the probability of each new sample
//! of some specified features of a set of metric data as it is continuously
//! streamed to the model in time order.
//!
//! IMPLEMENTATION DECISIONS:\n
//! The data about the current bucketing interval is stored on the model
//! so that the data gatherer objects can be shared by multiple models.
//! This is to reduce the model memory footprint when the event data is
//! being batched by time to support comparison in which case all models
//! share a data gatherer.
//!
//! It assumes data are supplied in time order since this means minimal
//! state can be maintained.
class MODEL_EXPORT CMetricModel : public CIndividualModel
{
    public:
        friend class CMetricModelDetailsView;

        typedef SMetricFeatureData TFeatureData;
        typedef std::pair<std::size_t, TFeatureData> TSizeFeatureDataPr;
        typedef std::vector<TSizeFeatureDataPr> TSizeFeatureDataPrVec;
        typedef std::pair<model_t::EFeature, TSizeFeatureDataPrVec> TFeatureSizeFeatureDataPrVecPr;
        typedef std::vector<TFeatureSizeFeatureDataPrVecPr> TFeatureSizeFeatureDataPrVecPrVec;
        typedef TFeatureSizeFeatureDataPrVecPrVec::iterator TFeatureSizeFeatureDataPrVecPrVecItr;
        typedef TFeatureSizeFeatureDataPrVecPrVec::const_iterator TFeatureSizeFeatureDataPrVecPrVecCItr;

        //! The statistics we maintain about a bucketing interval.
        struct MODEL_EXPORT SBucketStats
        {
            explicit SBucketStats(core_t::TTime startTime);

            //! The start time of this bucket.
            core_t::TTime s_StartTime;
            //! The non-zero person counts in the current bucket.
            TSizeUInt64PrVec s_PersonCounts;
            //! The total count in the current bucket.
            uint64_t s_TotalCount;
            //! The feature data samples for the current bucketing interval.
            TFeatureSizeFeatureDataPrVecPrVec s_FeatureData;
            //! A cache of the corrections applied to interim results.
            //! The key is <feature, pid, pid> for non-correlated corrections
            //! or <feature, pid, correlated_pid> for correlated corrections
            mutable TFeatureSizeSizeTripleDouble1VecUMap s_InterimCorrections;
        };

    public:
        //! \name Life-cycle
        //@{
        //! \param[in] params The global configuration parameters.
        //! \param[in] dataGatherer The object that gathers time series data.
        //! \param[in] newPriors The priors used to model new people for each
        //! univariate feature.
        //! \param[in] newMultivariatePriors The priors used to model new
        //! people for each multivariate feature.
        //! \param[in] newCorrelatePriors The priors used to model new pairs
        //! of correlated people for each univariate feature.
        //! \param[in] newDecompositions The time series periodic decompositions
        //! used for new people for each feature.
        //! \param[in] influenceCalculators The influence calculators to use
        //! for each feature.
        CMetricModel(const SModelParams &params,
                     const TDataGathererPtr &dataGatherer,
                     const TFeaturePriorPtrPrVec &newPriors,
                     const TFeatureMultivariatePriorPtrPrVec &newMultivariatePriors,
                     const TFeatureMultivariatePriorPtrPrVec &newCorrelatePriors,
                     const TFeatureDecompositionCPtrVecPrVec &newDecompositions,
                     const TFeatureInfluenceCalculatorCPtrPrVecVec &influenceCalculators);

        //! Constructor used for restoring persisted models.
        //!
        //! \note The current bucket statistics are left default initialized
        //! and so must be sampled for before this model can be used.
        CMetricModel(const SModelParams &params,
                     const TDataGathererPtr &dataGatherer,
                     const TFeaturePriorPtrPrVec &newPriors,
                     const TFeatureMultivariatePriorPtrPrVec &newMultivariatePriors,
                     const TFeatureMultivariatePriorPtrPrVec &newCorrelatePriors,
                     const TFeatureDecompositionCPtrVecPrVec &newDecompositions,
                     const TFeatureInfluenceCalculatorCPtrPrVecVec &influenceCalculators,
                     core::CStateRestoreTraverser &traverser);

        //! Create a copy that will result in the same persisted state as the
        //! original.  This is effectively a copy constructor that creates a
        //! copy that's only valid for a single purpose.  The boolean flag is
        //! redundant except to create a signature that will not be mistaken
        //! for a general purpose copy constructor.
        CMetricModel(bool isForPersistence, const CMetricModel &other);
        //@}

        //! \name Persistence
        //@{
        //! Persist state by passing information to \p inserter.
        virtual void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

        //! Restore reading state from \p traverser.
        virtual bool acceptRestoreTraverser(const model_t::TAnyRestoreFunc &extraDataRestoreFunc,
                                            core::CStateRestoreTraverser &traverser);

        //! Create a clone of this model that will result in the same persisted
        //! state.  The clone may be incomplete in ways that do not affect the
        //! persisted representation, and must not be used for any other
        //! purpose.
        //! \warning The caller owns the object returned.
        virtual CModel *cloneForPersistence(void) const;
        //@}

        //! Get the model category.
        virtual model_t::EModelType category(void) const;

        //! Returns false.
        virtual bool isEventRate(void) const;

        //! Returns true.
        virtual bool isMetric(void) const;

        //! \name Bucket Statistics
        //@{
        //! Returns null.
        virtual TOptionalDouble baselineBucketCount(std::size_t pid) const;

        //! Get the value of \p feature for the person identified
        //! by \p pid in the bucketing interval containing \p time.
        //!
        //! \param[in] feature The feature of interest.
        //! \param[in] pid The identifier of the person of interest.
        //! \param[in] cid Ignored.
        //! \param[in] time The time of interest.
        virtual TDouble1Vec currentBucketValue(model_t::EFeature feature,
                                               std::size_t pid,
                                               std::size_t cid,
                                               core_t::TTime time) const;

        //! Get the baseline bucket value of \p feature for the person
        //! identified by \p pid as of the start of the current bucketing
        //! interval.
        //!
        //! \param[in] feature The feature of interest.
        //! \param[in] pid The identifier of the person of interest.
        //! \param[in] cid Ignored.
        //! \param[in] type A description of the type of result for which
        //! to get the baseline. See CResultType for more details.
        //! \param[in] correlated The correlated series' identifiers and
        //! their values if any.
        //! \param[in] time The time of interest.
        virtual TDouble1Vec baselineBucketMean(model_t::EFeature feature,
                                               std::size_t pid,
                                               std::size_t cid,
                                               model_t::CResultType type,
                                               const TSizeDoublePr1Vec &correlated,
                                               core_t::TTime time) const;

        //! Output the current bucket statistics by repeatedly calling the
        //! supplied function.  ALL bucket statistics are output, not just those
        //! considered anomalous.  This is used as the implementation of the
        //! mlstats command in the Splunk app.
        virtual void outputCurrentBucketStatistics(const std::string &partitionFieldValue,
                                                   const TBucketStatsOutputFunc &outputFunc) const;
        //@}

        //! \name Person
        //@{
        //! Get the person unique identifiers which have a feature value
        //! in the bucketing time interval including \p time.
        virtual void currentBucketPersonIds(core_t::TTime time, TSizeVec &result) const;
        //@}

        //! \name Update
        //@{
        //! Sample any state needed by computeProbablity in the time
        //! interval [\p startTime, \p endTime] but do not update the
        //! model. This is needed by the results preview.
        //!
        //! \param[in] startTime The start of the time interval to sample.
        //! \param[in] endTime The end of the time interval to sample.
        virtual void sampleBucketStatistics(core_t::TTime startTime,
                                            core_t::TTime endTime,
                                            CResourceMonitor &resourceMonitor);

        //! Update the model with features samples from the time interval
        //! [\p startTime, \p endTime].
        //!
        //! \param[in] startTime The start of the time interval to sample.
        //! \param[in] endTime The end of the time interval to sample.
        virtual void sample(core_t::TTime startTime,
                            core_t::TTime endTime,
                            CResourceMonitor &resourceMonitor);
        //@}

        //! \name Probability
        //@{
        //! Compute the probability of seeing the metric values in the
        //! time interval [\p startTime, \p endTime] for the person
        //! identified by \p pid.
        //!
        //! \param[in] pid The identifier of the person of interest.
        //! \param[in] startTime The start of the time interval of interest.
        //! \param[in] endTime The end of the time interval of interest.
        //! \param[in] partitioningFields The partitioning field (name, value)
        //! pairs for which to compute the the probability.
        //! \param[in] numberAttributeProbabilities Ignored.
        //! \param[out] result A structure containing the probability,
        //! the smallest \p numberAttributeProbabilities attribute
        //! probabilities, the influences and any extra descriptive data
        virtual bool computeProbability(std::size_t pid,
                                        core_t::TTime startTime,
                                        core_t::TTime endTime,
                                        CPartitioningFields &partitioningFields,
                                        std::size_t numberAttributeProbabilities,
                                        SAnnotatedProbability &result) const;
        //@}

        //! Get the checksum of this model.
        //!
        //! \param[in] includeCurrentBucketStats If true then include
        //! the current bucket statistics. (This is designed to handle
        //! serialization, for which we don't serialize the current
        //! bucket statistics.)
        virtual uint64_t checksum(bool includeCurrentBucketStats = true) const;

        //! Debug the memory used by this model.
        virtual void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const;

        //! Get the memory used by this model.
        virtual std::size_t memoryUsage(void) const;

        //! Get the static size of this object - used for virtual hierarchies.
        virtual std::size_t staticSize(void) const;

        //! Get the non-estimated value of the the memory used by this model.
        virtual std::size_t computeMemoryUsage(void) const;

        //! Get a view of the internals of the model for visualization.
        virtual CModelDetailsViewPtr details(void) const;

        //! Get the value of the \p feature of the person identified
        //! by \p pid for the bucketing interval containing \p time.
        const TFeatureData *featureData(model_t::EFeature feature,
                                        std::size_t pid,
                                        core_t::TTime time) const;

    protected:
        //! Get the start time of the current bucket.
        virtual core_t::TTime currentBucketStartTime(void) const;

        //! Set the start time of the current bucket.
        virtual void currentBucketStartTime(core_t::TTime time);

        //! Get the total count of the current bucket.
        uint64_t currentBucketTotalCount(void) const;

        //! Get the interim corrections of the current bucket.
        TFeatureSizeSizeTripleDouble1VecUMap &currentBucketInterimCorrections(void) const;

    private:
        typedef boost::optional<CSample> TOptionalSample;
        typedef core::CSmallVector<core_t::TTime, 2> TTime2Vec;
        typedef maths::CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;
        typedef core::CSmallVector<TMeanAccumulator, 1> TMeanAccumulator1Vec;

    private:
        //! Get the person counts in the current bucket.
        virtual const TSizeUInt64PrVec &currentBucketPersonCounts(void) const;

        //! Get writable person counts in the current bucket.
        virtual TSizeUInt64PrVec &currentBucketPersonCounts(void);

        //! Set the current bucket total count.
        virtual void currentBucketTotalCount(uint64_t totalCount);

        //! Create the time series models for "n" newly observed people.
        virtual void createNewModels(std::size_t n, std::size_t m);

        //! Reinitialize the time series models for recycled people.
        virtual void updateRecycledModels(void);

        //! Clear out large state objects for people that are pruned.
        virtual void clearPrunedResources(const TSizeVec &people,
                                          const TSizeVec &attributes);

        //! No-op.
        virtual void sampleCounts(core_t::TTime startTime, core_t::TTime endTime);

        //! Get the data needed for computing probabilities for correlates.
        void correlateData(model_t::EFeature feature,
                           std::size_t pid,
                           const CProbabilityAndInfluenceCalculator::SParams &univariateParams,
                           core_t::TTime time,
                           core_t::TTime bucketLength,
                           bool isInterim,
                           const TOptionalSample &bucket,
                           const TStrCRefDouble1VecDoublePrPrVecVec &influenceValues,
                           CProbabilityAndInfluenceCalculator::SCorrelateParams &params,
                           TStrCRefDouble1VecDouble1VecPrPrVecVecVec &correlateInfluenceValues) const;

        //! Get the detrended univariate sample and weights for the probability
        //! calculation.
        bool sampleAndWeights(model_t::EFeature feature,
                              std::size_t pid,
                              core_t::TTime time,
                              core_t::TTime bucketLength,
                              const TFeatureData *&data,
                              TDouble1Vec &sample,
                              TDouble4Vec1Vec &weights) const;

        //! Get the detrended multivariate sample and weights for the probability
        //! calculation.
        bool sampleAndWeights(model_t::EFeature feature,
                              std::size_t dimension,
                              std::size_t pid,
                              core_t::TTime time,
                              core_t::TTime bucketLength,
                              const TFeatureData *&data,
                              TDouble10Vec1Vec &sample,
                              TDouble10Vec4Vec1Vec &weights) const;

        //! Get the trend and prior prediction residuals.
        template<typename PRIOR, typename VECTOR>
        void residuals(double interval,
                       const TDecompositionPtr1Vec &trend,
                       const PRIOR &prior,
                       const VECTOR &sample,
                       TMeanAccumulator1Vec (&result)[2]) const;

    private:
        //! The statistics we maintain about the bucket. Note that we do
        //! not need to and therefore don't bother to persist the current
        //! bucket statistics since we separately persist the results.
        SBucketStats m_CurrentBucketStats;

        friend class ::CMockMetricModel;
};

}
}

#endif // INCLUDED_ml_model_CMetricModel_h
