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

#ifndef INCLUDED_ml_model_CEventRatePeersModel_h
#define INCLUDED_ml_model_CEventRatePeersModel_h

#include <core/CMemory.h>

#include <maths/CBasicStatistics.h>
#include <maths/CFloatStorage.h>
#include <maths/CMultinomialConjugate.h>
#include <maths/CPrior.h>

#include <model/CMemoryUsageEstimator.h>
#include <model/CPopulationModel.h>
#include <model/ImportExport.h>
#include <model/ModelTypes.h>

#include <map>
#include <utility>
#include <vector>

namespace ml
{
namespace model
{

//! \brief The model for computing the anomalousness of the rate at which
//! each people in a population generates events in a data stream w.r.t.
//! their peer group.
//!
//! DESCRIPTION:\n
//! This model computes the probability of the rate at which a person
//! generates events in the bucketing time interval given the typical
//! rates at which similar people in a population generate events in
//! that interval. There are two distinct types of probability that it
//! can compute: a probability on the current bucketing interval
//! and a probability based on all the person's interactions to date.
//! The later uses (statistical) models of the person's event rates for
//! each attribute for which they generate events.
//!
//! IMPLEMENTATION DECISIONS:\n
//! The data about the current bucketing interval is stored on the model
//! so that the data gatherer objects can be shared by multiple models.
//! This is to reduce the model memory footprint when the event data is
//! being batched by time to support comparison in which case all models
//! for the same attribute share a data gatherer.
//!
//! It assumes data are supplied in time order since this means minimal
//! state can be maintained.
class MODEL_EXPORT CEventRatePeersModel : public CPopulationModel
{
    public:
        friend class CEventRatePeersModelDetailsView;

        typedef maths::CBasicStatistics::SSampleMeanVar<double>::TAccumulator TDoubleMoments;
        typedef std::vector<TDoubleMoments> TDoubleMomentsVec;
        typedef maths::CBasicStatistics::SSampleMeanVar<maths::CFloatStorage>::TAccumulator TMoments;
        typedef std::vector<TMoments> TMomentsVec;
        typedef SEventRateFeatureData TFeatureData;
        typedef std::pair<TSizeSizePr, TFeatureData> TSizeSizePrFeatureDataPr;
        typedef std::vector<TSizeSizePrFeatureDataPr> TSizeSizePrFeatureDataPrVec;
        typedef TSizeSizePrFeatureDataPrVec::const_iterator TSizeSizePrFeatureDataPrVecCItr;
        typedef std::map<model_t::EFeature, TSizeSizePrFeatureDataPrVec> TFeatureSizeSizePrFeatureDataPrVecMap;
        typedef TFeatureSizeSizePrFeatureDataPrVecMap::const_iterator TFeatureSizeSizePrFeatureDataPrVecMapCItr;
        typedef boost::unordered_map<std::size_t, TMoments> TSizeMomentsUMap;
        typedef TSizeMomentsUMap::const_iterator TSizeMomentsUMapCItr;
        typedef std::vector<TSizeMomentsUMap> TSizeMomentsUMapVec;
        typedef std::map<model_t::EFeature, TSizeMomentsUMapVec> TFeatureSizeMomentsUMapVecMap;
        typedef TFeatureSizeMomentsUMapVecMap::iterator TFeatureSizeMomentsUMapVecMapItr;
        typedef TFeatureSizeMomentsUMapVecMap::const_iterator TFeatureSizeMomentsUMapVecMapCItr;
        typedef boost::unordered_map<std::size_t, std::size_t> TSizeSizeUMap;
        typedef TSizeSizeUMap::iterator TSizeSizeUMapItr;
        typedef TSizeSizeUMap::const_iterator TSizeSizeUMapCItr;
        typedef std::vector<TSizeSizeUMap> TSizeSizeUMapVec;
        typedef std::map<model_t::EFeature, TSizeSizeUMapVec> TFeatureSizeSizeUMapVecMap;
        typedef TFeatureSizeSizeUMapVecMap::iterator TFeatureSizeSizeUMapVecMapItr;
        typedef TFeatureSizeSizeUMapVecMap::const_iterator TFeatureSizeSizeUMapVecMapCItr;
        typedef std::vector<TPriorPtrVec> TPriorPtrVecVec;
        typedef std::map<model_t::EFeature, TPriorPtrVecVec> TFeaturePriorPtrVecVecMap;
        typedef TFeaturePriorPtrVecVecMap::iterator TFeaturePriorPtrVecVecMapItr;
        typedef TFeaturePriorPtrVecVecMap::const_iterator TFeaturePriorPtrVecVecMapCItr;
        typedef boost::unordered_map<std::size_t, double> TSizeDoubleUMap;
        typedef TSizeDoubleUMap::iterator TSizeDoubleUMapItr;
        typedef TSizeDoubleUMap::const_iterator TSizeDoubleUMapCItr;
        typedef std::vector<TSizeDoubleUMap> TSizeDoubleUMapVec;
        typedef std::map<model_t::EFeature, TSizeDoubleUMapVec> TFeatureSizeDoubleUMapVecMap;
        typedef TFeatureSizeDoubleUMapVecMap::iterator TFeatureSizeDoubleUMapVecMapItr;
        typedef TFeatureSizeDoubleUMapVecMap::const_iterator TFeatureSizeDoubleUMapVecMapCItr;

        //! \brief The statistics we maintain about a bucketing interval.
        struct MODEL_EXPORT SBucketStats
        {
            explicit SBucketStats(core_t::TTime startTime);

            //! The start time of this bucket.
            core_t::TTime s_StartTime;
            //! The non-zero counts of messages by people in the bucketing
            //! interval.
            TSizeUInt64PrVec s_PersonCounts;
            //! The total count in the current bucket.
            uint64_t s_TotalCount;
            //! The count features we are modeling.
            TFeatureSizeSizePrFeatureDataPrVecMap s_FeatureData;
            //! A cache of the corrections applied to interim results.
            mutable TFeatureSizeSizeTripleDouble1VecUMap s_InterimCorrections;
        };

        //! Lift the overloads of currentBucketValue into the class scope.
        using CPopulationModel::currentBucketValue;

        //! Lift the overloads of baselineBucketMean into the class scope.
        using CModel::baselineBucketMean;

        //! Lift the overloads of acceptPersistInserter into the class scope.
        using CPopulationModel::acceptPersistInserter;

    public:
        //! \name Life-cycle
        //@{
        //! \param[in] params The global configuration parameters.
        //! \param[in] dataGatherer The object that gathers time series data.
        //! \param[in] newPriors The priors used to model new attributes for
        //! each univariate feature.
        //! \param[in] newMultivariatePriors The priors used to model new
        //! attributes for each multivariate feature.
        //! \param[in] newDecompositions The time series periodic decompositions
        //! used for new attributes for each feature.
        //! \param[in] influenceCalculators The influence calculators to use
        //! for each feature.
        //! \note The current bucket statistics are left default initialized
        //! and so must be sampled for before this model can be used.
        CEventRatePeersModel(const SModelParams &params,
                             const TDataGathererPtr &dataGatherer,
                             const TFeaturePriorPtrPrVec &newPriors,
                             const TFeatureMultivariatePriorPtrPrVec &newMultivariatePriors,
                             const TFeatureDecompositionCPtrVecPrVec &newDecompositions,
                             const TFeatureInfluenceCalculatorCPtrPrVecVec &influenceCalculators);

        //! Constructor used for restoring persisted models.
        //!
        //! \note The current bucket statistics are left default initialized
        //! and so must be sampled for before this model can be used.
        CEventRatePeersModel(const SModelParams &params,
                             const TDataGathererPtr &dataGatherer,
                             const TFeaturePriorPtrPrVec &newPriors,
                             const TFeatureMultivariatePriorPtrPrVec &newMultivariatePriors,
                             const TFeatureDecompositionCPtrVecPrVec &newDecompositions,
                             const TFeatureInfluenceCalculatorCPtrPrVecVec &influenceCalculators,
                             core::CStateRestoreTraverser &traverser);

        //! Create a copy that will result in the same persisted state as the
        //! original.  This is effectively a copy constructor that creates a
        //! copy that's only valid for a single purpose.  The boolean flag is
        //! redundant except to create a signature that will not be mistaken
        //! for a general purpose copy constructor.
        CEventRatePeersModel(bool isForPersistence,
                             const CEventRatePeersModel &other);
        //@}

        //! \name Persistence
        //@{
        //! Persist state by passing information to the supplied inserter
        virtual void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

        //! Add to the contents of the object.
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

        //! Returns true.
        virtual bool isEventRate(void) const;

        //! Returns false.
        virtual bool isMetric(void) const;

        //! \name Bucket Statistics
        //!@{
        //! Get the bucket value of \p feature for the person identified
        //! by \p pid and the attribute identified by \p cid in the
        //! bucketing interval including \p time.
        //!
        //! \param[in] feature The feature of interest.
        //! \param[in] pid The identifier of the person of interest.
        //! \param[in] cid The identifier of the attribute of interest.
        //! \param[in] time The time of interest.
        virtual TDouble1Vec currentBucketValue(model_t::EFeature feature,
                                               std::size_t pid,
                                               std::size_t cid,
                                               core_t::TTime time) const;

        //! Get the appropriate baseline bucket value of \p feature for
        //! the person identified by \p pid and the attribute identified
        //! by \p cid as of the start of the current bucketing interval.
        //! This has subtly different meanings dependent on the model.
        //!
        //! \param[in] feature The feature of interest.
        //! \param[in] pid The identifier of the person of interest.
        //! \param[in] cid The identifier of the attribute of interest.
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

        //! Check if bucket statistics are available for the specified time.
        virtual bool bucketStatsAvailable(core_t::TTime time) const;
        //@}

        //! \name Update
        //@{
        //! This samples the bucket statistics, and any state needed
        //! by computeProbablity, in the time interval [\p startTime,
        //! \p endTime], but does not update the model. This is needed
        //! by the results preview.
        //!
        //! \param[in] startTime The start of the time interval to sample.
        //! \param[in] endTime The end of the time interval to sample.
        virtual void sampleBucketStatistics(core_t::TTime startTime,
                                            core_t::TTime endTime,
                                            CResourceMonitor &resourceMonitor);

        //! Update the model with the samples of the process in the
        //! time interval [\p startTime, \p endTime].
        //!
        //! \param[in] startTime The start of the time interval to sample.
        //! \param[in] endTime The end of the time interval to sample.
        virtual void sample(core_t::TTime startTime,
                            core_t::TTime endTime,
                            CResourceMonitor &resourceMonitor);

        //! Prune any person models which haven't been updated for a
        //! specified period
        virtual void prune(std::size_t maximumAge);
        //@}

        //! \name Probability
        //@{
        //! Compute the probability of seeing the samples of the process
        //! for the person identified by \p pid in the time interval
        //! [\p startTime, \p endTime].
        //!
        //! \param[in] pid The unique identifier of the person of interest.
        //! \param[in] startTime The start of the time interval of interest.
        //! \param[in] endTime The end of the time interval of interest.
        //! \param[in] partitioningFields The partitioning field (name, value)
        //! pairs for which to compute the the probability.
        //! \param[in] numberAttributeProbabilities The maximum number of
        //! attribute probabilities to retrieve.
        //! \param[out] result A structure containing the probability,
        //! the smallest \p numberAttributeProbabilities attribute
        //! probabilities, the influences and any extra descriptive data
        virtual bool computeProbability(std::size_t pid,
                                        core_t::TTime startTime,
                                        core_t::TTime endTime,
                                        CPartitioningFields &partitioningFields,
                                        std::size_t numberAttributeProbabilities,
                                        SAnnotatedProbability &result) const;

        //! Compute the probability of seeing \p person's attribute processes
        //! so far given the population distributions.
        //!
        //! \param[in] person The person of interest.
        //! \param[in] numberAttributeProbabilities The maximum number of
        //! attribute probabilities to retrieve.
        //! \param[out] probability Filled in with the probability of seeing
        //! the person's processes given the population processes.
        //! \param[out] attributeProbabilities Filled in with the smallest
        //! \p numberAttributeProbabilities attribute probabilities and
        //! associated data describing the calculation.
        virtual bool computeTotalProbability(const std::string &person,
                                             std::size_t numberAttributeProbabilities,
                                             TOptionalDouble &probability,
                                             TAttributeProbability1Vec &attributeProbabilities) const;
        //@}

        //! Output the current bucket statistics by repeatedly calling the
        //! supplied function.  ALL bucket statistics are output, not just
        //! those considered anomalous.  This is used as the implementation
        //! of the mlstats command in the Splunk app.
        virtual void outputCurrentBucketStatistics(const std::string &partitionFieldValue,
                                                   const TBucketStatsOutputFunc &outputFunc) const;

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

        //! Get the non-estimated memory used by this model.
        virtual std::size_t computeMemoryUsage(void) const;

        //! Estimate the memory usage of the model based on number of people,
        //! attributes and correlations.
        virtual std::size_t estimateMemoryUsage(std::size_t numberPeople,
                                                std::size_t numberAttributes,
                                                std::size_t numberCorrelations) const;

        //! Get the static size of this object - used for virtual hierarchies
        virtual std::size_t staticSize(void) const;

        //! Get a view of the internals of the model for visualization.
        virtual CModelDetailsViewPtr details(void) const;

        //! Get the feature data corresponding to \p feature at \p time.
        const TSizeSizePrFeatureDataPrVec &featureData(model_t::EFeature feature,
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
        //! Initialize the time series models for "n" newly observed people
        //! and "m" newly observed attributes.
        virtual void createNewModels(std::size_t n, std::size_t m);

        //! Re-initialize the time series models for recycled people
        //! and/or attributes.
        virtual void updateRecycledModels(void);

        //! Clear out large state objects for people and/or attributes
        //! that which have been pruned.
        virtual void clearPrunedResources(const TSizeVec &people,
                                          const TSizeVec &attributes);

        //! Clear out large state objects for people and/or attributes
        //! stored on this class.
        void clear(const TSizeVec &people, const TSizeVec &attributes);

        //! Recompute peer groups and the corresponding mixture models.
        void updatePeerGroups(core_t::TTime time);

        //! Returns null.
        virtual const maths::CPrior *prior(model_t::EFeature feature,
                                           std::size_t cid) const;

        //! Returns null.
        virtual const maths::CMultivariatePrior *multivariatePrior(model_t::EFeature feature,
                                                                   std::size_t cid) const;

        //! Reinitialize the peer group priors for \p feature and the
        //! attribute identified by \p cid.
        //!
        //! \param[in] feature The feature of interest.
        //! \param[in] cid The identifier of the attribute of interest.
        //! \return True if the prior was reset.
        bool resetPrior(model_t::EFeature feature, std::size_t cid);

        //! Get the peer group priors for each attribute of \p feature.
        const TPriorPtrVecVec &peerGroupPriors(model_t::EFeature feature) const;

        //! Get the person peer groups for each attribute of \p feature.
        const TSizeSizeUMapVec &personPeerGroups(model_t::EFeature feature) const;

        //! Get the moments for \p feature and the attribute identified
        //! by \p cid.
        const TSizeMomentsUMap &attributeMoments(model_t::EFeature feature,
                                                 std::size_t cid) const;

        //! Get the current bucket person counts.
        virtual const TSizeUInt64PrVec &personCounts(void) const;

        //! Set the current bucket total count.
        virtual void currentBucketTotalCount(uint64_t totalCount);
    private:
        //! The time between batch updates of the peer groups.
        static const core_t::TTime BATCH_UPDATE_PERIOD;
        //! Number of bootstrap runs used for clustering.
        static const std::size_t NUMBER_BOOTSTRAPS;
        //! The number of iterations of k-means to use for a single
        //! round of improve parameters in the x-means algorithm.
        static const std::size_t IMPROVE_PARAMS_KMEANS_ITERATIONS;
        //! The number of random splits to try for a single round
        //! of improve structure in the x-means algorithm.
        static const std::size_t IMPROVE_STRUCTURE_CLUSTER_SEEDS;
        //! The number of iterations of the k-means algorithm to use
        //! for a single round of improve structure in the x-means
        //! algorithm.
        static const std::size_t IMPROVE_STRUCTURE_KMEANS_ITERATIONS;
        //! The overlap threshold, i.e. the Szymkiewicz-Simpson
        //! coefficient, for which we combine clusters in different
        //! bootstrap runs.
        static const double OVERLAP_THRESHOLD;
        //! The degree to which overlapping clusters in different
        //! bootstrap samples of the data are chained together.
        static const double CHAINING_FACTOR;
        //! The maximum number of peer groups we will create.
        static const std::size_t MAX_PEER_GROUPS;
        //! The number of times we will sample the person models when
        //! updating the peer group priors.
        static const std::size_t PERSON_SAMPLES;
        //! The number of times we will sample the peer group models
        //! when updating the peer group priors.
        static const std::size_t PEER_GROUP_SAMPLES;

    private:
        //! The statistics we maintain about the bucket. Note that we
        //! do not need, and therefore don't bother, to persist the
        //! current bucket statistics since we separately persist the
        //! results.
        SBucketStats m_CurrentBucketStats;

        //! The initial prior for attributes' probabilities.
        maths::CMultinomialConjugate m_NewAttributeProbabilityPrior;

        //! The time of the last batch update of the peer group models.
        core_t::TTime m_LastBatchUpdateTime;

        //! The moments for each feature and (person, attribute) pair.
        TFeatureSizeMomentsUMapVecMap m_FeatureMoments;

        //! The peer groups to which each (person, attribute) pair belongs.
        TFeatureSizeSizeUMapVecMap m_FeaturePeerGroups;

        //! The attribute peer group models.
        TFeaturePriorPtrVecVecMap m_FeaturePeerGroupPriors;

        //! A measure of the stability with which each person is assigned
        //! to a cluster for each attribute.
        TFeatureSizeDoubleUMapVecMap m_FeatureClusterStability;

        //! The memory usage estimator
        mutable CMemoryUsageEstimator m_MemoryEstimator;
};

}
}

#endif // INCLUDED_ml_model_CEventRatePeersModel_h
