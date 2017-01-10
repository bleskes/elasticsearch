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

#ifndef INCLUDED_ml_model_CEventRatePopulationModel_h
#define INCLUDED_ml_model_CEventRatePopulationModel_h

#include <core/CMemory.h>

#include <maths/CMultinomialConjugate.h>

#include <model/CMemoryUsageEstimator.h>
#include <model/CPopulationModel.h>
#include <model/CModelTools.h>
#include <model/ImportExport.h>
#include <model/ModelTypes.h>

#include <map>
#include <utility>
#include <vector>

namespace
{
class CMockPopulationEventRateModel;
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

//! \brief The model for computing the anomalousness of the rate at which
//! each person in a population generates events in a data stream.
//!
//! DESCRIPTION:\n
//! This model computes the probability of the rate at which a person
//! generates events in the bucketing time interval given the typical
//! rates at which all people generate events in that interval for each
//! person in a population. There are two distinct types of probability
//! that it can compute: a probability on the current bucketing interval
//! and a probability based on all the person's interactions to date.
//! The later uses (statistical) models of the person's event rates for
//! each attribute for which they generate events, and is only available
//! if person models are created.
//!
//! Various other features of the data can be modeled. These include the
//! the rarity of each attribute across the population, which is defined,
//! informally, as the proportion of people which generate events for
//! that attribute, and the frequency with which a person generates events
//! in each attribute. These are both categorical data and this model
//! maintains multinomial distributions (across all attributes) for them.
//!
//! The rarity of attributes enters in to the probability calculation
//! by using a naive Bayes assumption that probability (function) of
//! each attribute count feature is conditionally independent on the
//! attribute. The model, approximately, computes the probability of
//! seeing a less likely set of attributes and counts for the person.
//! If the frequency with which a person generates events in an attribute
//! is modeled then the expectation of this probability is taken w.r.t.
//! the multinomial distribution describing the probability of a person
//! generating a given combination of attributes.
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
class MODEL_EXPORT CEventRatePopulationModel : public CPopulationModel
{
    friend class CEventRatePopulationModelDetailsView;
    friend class ::CMockPopulationEventRateModel;

    public:
        typedef std::pair<TSizeSizePr, uint64_t> TSizeSizePrUInt64Pr;
        typedef std::vector<TSizeSizePrUInt64Pr> TSizeSizePrUInt64PrVec;
        typedef SEventRateFeatureData TFeatureData;
        typedef std::pair<TSizeSizePr, TFeatureData> TSizeSizePrFeatureDataPr;
        typedef std::vector<TSizeSizePrFeatureDataPr> TSizeSizePrFeatureDataPrVec;
        typedef std::map<model_t::EFeature, TSizeSizePrFeatureDataPrVec> TFeatureSizeSizePrFeatureDataPrVecMap;
        typedef TFeatureSizeSizePrFeatureDataPrVecMap::const_iterator TFeatureSizeSizePrFeatureDataPrVecMapCItr;
        typedef CModelTools::CLessLikelyProbability TLessLikelyProbability;

        //! The statistics we maintain about a bucketing interval.
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
        CEventRatePopulationModel(const SModelParams &params,
                                  const TDataGathererPtr &dataGatherer,
                                  const TFeaturePriorPtrPrVec &newPriors,
                                  const TFeatureMultivariatePriorPtrPrVec &newMultivariatePriors,
                                  const TFeatureDecompositionCPtrVecPrVec &newDecompositions,
                                  const TFeatureInfluenceCalculatorCPtrPrVecVec &influenceCalculators);

        //! Constructor used for restoring persisted models.
        //!
        //! \note The current bucket statistics are left default initialized
        //! and so must be sampled for before this model can be used.
        CEventRatePopulationModel(const SModelParams &params,
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
        CEventRatePopulationModel(bool isForPersistence,
                                  const CEventRatePopulationModel &other);
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
        //@{
        //! Get the value of \p feature for the person identified
        //! by \p pid and the attribute identified by \p cid in the
        //! bucketing interval containing \p time.
        //!
        //! \param[in] feature The feature of interest.
        //! \param[in] pid The identifier of the person of interest.
        //! \param[in] cid The identifier of the attribute of interest.
        //! \param[in] time The time of interest.
        virtual TDouble1Vec currentBucketValue(model_t::EFeature feature,
                                               std::size_t pid,
                                               std::size_t cid,
                                               core_t::TTime time) const;

        //! Get the population baseline mean of \p feature for the
        //! attribute identified by \p cid as of the start of the
        //! current bucketing interval.
        //!
        //! \param[in] feature The feature of interest
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

        //! Update the model with the samples of the various processes
        //! in the time interval [\p startTime, \p endTime].
        //!
        //! \param[in] startTime The start of the time interval to sample.
        //! \param[in] endTime The end of the time interval to sample.
        virtual void sample(core_t::TTime startTime,
                            core_t::TTime endTime,
                            CResourceMonitor &resourceMonitor);

        //! Prune any data for people and attributes which haven't been
        //! seen for a sufficiently long period. This is based on the
        //! prior decay rates and the number of batches into which we
        //! are partitioning time.
        virtual void prune(std::size_t maximumAge);
        //@}

        //! \name Probability
        //@{
        //! Compute the probability of seeing \p person's attribute values
        //! for the buckets in the interval [\p startTime, \p endTime].
        //!
        //! \param[in] pid The identifier of the person of interest.
        //! \param[in] startTime The start of the interval of interest.
        //! \param[in] endTime The end of the interval of interest.
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

        //! Clears \p probability and \p attributeProbabilities.
        virtual bool computeTotalProbability(const std::string &person,
                                             std::size_t numberAttributeProbabilities,
                                             TOptionalDouble &probability,
                                             TAttributeProbability1Vec &attributeProbabilities) const;
        //@}

        //! Output the current bucket statistics by repeatedly calling the
        //! supplied function.  ALL bucket statistics are output, not just those
        //! considered anomalous.  This is used as the implementation of the
        //! mlstats command in the Splunk app.
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

        //! Get the static size of this object - used for virtual hierarchies
        virtual std::size_t staticSize(void) const;

        //! Get the non-estimated memory used by this model.
        virtual std::size_t computeMemoryUsage(void) const;

        //! Estimate the memory usage of the model based on number of people,
        //! attributes and correlations.
        virtual std::size_t estimateMemoryUsage(std::size_t numberPeople,
                                                std::size_t numberAttributes,
                                                std::size_t numberCorrelations) const;

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
        typedef std::vector<maths::CMultinomialConjugate> TMultinomialVec;

    private:
        //! Initialize the time series models for "n" newly observed people
        //! and "m" attributes.
        virtual void createNewModels(std::size_t n, std::size_t m);

        //! Initialize the time series models for recycled attributes and/or people.
        virtual void updateRecycledModels(void);

        //! Clear out large state objects for people/attributes that are pruned
        virtual void clearPrunedResources(const TSizeVec &people,
                                          const TSizeVec &attributes);

        //! Get the univariate prior for \p feature and the attribute
        //! identified by \p cid.
        virtual const maths::CPrior *prior(model_t::EFeature feature,
                                           std::size_t cid) const;

        //! Get the multivariate prior for \p feature and the attribute
        //! identified by \p cid.
        virtual const maths::CMultivariatePrior *multivariatePrior(model_t::EFeature feature,
                                                                   std::size_t cid) const;

        //! Reinitialize the population prior for \p feature and the
        //! attribute identified by \p cid.
        //!
        //! \param[in] feature The feature of interest.
        //! \param[in] cid The identifier of the attribute of interest.
        //! \return True if the prior was reset.
        virtual bool resetPrior(model_t::EFeature feature, std::size_t cid);

        //! Get the priors for the univariate feature \p feature.
        const TPriorPtrVec &priors(model_t::EFeature feature) const;

        //! Get the priors for the multivariate feature \p feature.
        const TMultivariatePriorPtrVec &multivariatePriors(model_t::EFeature feature) const;

        //! Get the attribute probabilities at the start of the current
        //! bucketing interval.
        const TLessLikelyProbability &attributeProbabilities(void) const;

        //! Get the current bucket person counts.
        virtual const TSizeUInt64PrVec &personCounts(void) const;

        //! Set the current bucket total count.
        virtual void currentBucketTotalCount(uint64_t totalCount);
    private:
        //! The statistics we maintain about the bucket. Note that we do
        //! not need, and therefore don't bother, to persist the current
        //! bucket statistics since we separately persist the results.
        SBucketStats m_CurrentBucketStats;

        //! The initial prior for attributes' probabilities.
        maths::CMultinomialConjugate m_NewAttributeProbabilityPrior;

        //! The prior for the probabilities of the attributes we are modeling.
        maths::CMultinomialConjugate m_AttributeProbabilityPrior;

        //! A cache of the attribute probabilities as of the start of the
        //! for the bucketing interval.
        TLessLikelyProbability m_AttributeProbabilities;

        //! The priors for the attribute event rate univariate feature.
        TFeaturePriorPtrVecMap m_AttributePriors;

        //! The priors for the attribute event rate multivariate feature.
        TFeatureMultivariatePriorPtrVecMap m_AttributeMultivariatePriors;

        //! The memory estimator.
        mutable CMemoryUsageEstimator m_MemoryEstimator;
};

}
}

#endif // INCLUDED_ml_model_CEventRatePopulationModel_h
