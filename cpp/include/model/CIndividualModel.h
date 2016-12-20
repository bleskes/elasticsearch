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

#ifndef INCLUDED_prelert_model_CIndividualModel_h
#define INCLUDED_prelert_model_CIndividualModel_h

#include <core/CMemory.h>
#include <core/CoreTypes.h>
#include <core/CStatistics.h>
#include <core/CTriple.h>

#include <model/CDecayRateController.h>
#include <model/CMemoryUsageEstimator.h>
#include <model/CModel.h>
#include <model/ImportExport.h>

#include <boost/unordered_set.hpp>

#include <cstddef>
#include <utility>
#include <vector>

#include <stdint.h>


namespace prelert
{
namespace model
{
class CAnnotatedProbabilityBuilder;
class CProbabilityAndInfluenceCalculator;

//! \brief The most basic individual model interface.
//!
//! DESCRIPTION:\n
//! This implements or stubs out the common portion of the CModel
//! interface for all individual models. It holds the maths:: and
//! objects which are used to describe the R.V.s that we use to
//! model individual time series' features.
//!
//! IMPLEMENTATION DECISIONS:\n
//! This gathers up the implementation which can be shared by all event
//! rate and metric individual times series models to avoid unnecessary
//! code duplication.
//!
//! It assumes data are supplied in time order since this means minimal
//! state can be maintained.
class MODEL_EXPORT CIndividualModel : public CModel
{
    public:
        typedef std::vector<core_t::TTime> TTimeVec;
        typedef std::pair<std::size_t, uint64_t> TSizeUInt64Pr;
        typedef std::vector<TSizeUInt64Pr> TSizeUInt64PrVec;
        typedef TSizeUInt64PrVec::const_iterator TSizeUInt64PrVecCItr;
        typedef core::CTriple<model_t::EFeature, std::size_t, std::size_t> TFeatureSizeSizeTriple;
        typedef boost::unordered_map<TFeatureSizeSizeTriple, TDouble1Vec> TFeatureSizeSizeTripleDouble1VecUMap;
        typedef TFeatureSizeSizeTripleDouble1VecUMap::const_iterator TFeatureSizeSizeTripleDouble1VecUMapCItr;

    public:
        //! \name Life-cycle
        //@{
        //! \param[in] params The global configuration parameters.
        //! \param[in] dataGatherer The object to gather time series data.
        //! \param[in] newPriors The priors used to model new people for each
        //! univariate feature.
        //! \param[in] newMultivariatePriors The priors used to model new
        //! people for each multivariate feature.
        //! \param[in] newCorrelatePriors The priors used to model new pairs
        //! of correlated people for each univariate feature.
        //! \param[in] isForRestore True if this constructor being used for restore.
        //! \param[in] newDecompositions The time series periodic decompositions
        //! used for new people for each feature.
        //! \note The current bucket statistics are left default initialized
        //! and so must be sampled for before this model can be used.
        CIndividualModel(const SModelParams &params,
                         const TDataGathererPtr &dataGatherer,
                         const TFeaturePriorPtrPrVec &newPriors,
                         const TFeatureMultivariatePriorPtrPrVec &newMultivariatePriors,
                         const TFeatureMultivariatePriorPtrPrVec &newCorrelatePriors,
                         const TFeatureInfluenceCalculatorCPtrPrVecVec &influenceCalculators,
                         const TFeatureDecompositionCPtrVecPrVec &newDecompositions,
                         bool isForRestore);

        //! Create a copy that will result in the same persisted state as the
        //! original.  This is effectively a copy constructor that creates a
        //! copy that's only valid for a single purpose.  The boolean flag is
        //! redundant except to create a signature that will not be mistaken
        //! for a general purpose copy constructor.
        CIndividualModel(bool isForPersistence, const CIndividualModel &other);
        //@}

        //! Returns false.
        virtual bool isPopulation(void) const;

        //! \name Bucket Statistics
        //@{
        //! Get the count in the bucketing interval containing \p time
        //! for the person identified by \p pid.
        //!
        //! \param[in] pid The person of interest.
        //! \param[in] time The time of interest.
        virtual TOptionalUInt64 currentBucketCount(std::size_t pid,
                                                   core_t::TTime time) const;

        //! Check if bucket statistics are available for the specified time.
        virtual bool bucketStatsAvailable(core_t::TTime time) const;
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
                                            CResourceMonitor &resourceMonitor) = 0;

        //! Sample any state needed by computeProbablity for the out-
        //! of-phase bucket in the time interval [\p startTime, \p endTime]
        //! but do not update the model.
        //!
        //! \param[in] startTime The start of the time interval to sample.
        //! \param[in] endTime The end of the time interval to sample.
        virtual void sampleOutOfPhase(core_t::TTime startTime,
                                      core_t::TTime endTime,
                                      CResourceMonitor &resourceMonitor);

        //! Update the model with features samples from the time interval
        //! [\p startTime, \p endTime].
        //!
        //! \param[in] startTime The start of the time interval to sample.
        //! \param[in] endTime The end of the time interval to sample.
        virtual void sample(core_t::TTime startTime,
                            core_t::TTime endTime,
                            CResourceMonitor &resourceMonitor) = 0;

        //! Prune any person models which haven't been updated for a
        //! specified period.
        virtual void prune(std::size_t maximumAge);
        //@}

        //! \name Probability
        //@{
        //! Clears \p probability and \p attributeProbabilities.
        virtual bool computeTotalProbability(const std::string &person,
                                             std::size_t numberAttributeProbabilities,
                                             TOptionalDouble &probability,
                                             TAttributeProbability1Vec &attributeProbabilities) const;
        //@}

        //! Get the checksum of this model.
        //!
        //! \param[in] includeCurrentBucketStats If true then include
        //! the current bucket statistics. (This is designed to handle
        //! serialization, for which we don't serialize the current
        //! bucket statistics.)
        virtual uint64_t checksum(bool includeCurrentBucketStats = true) const = 0;

        //! Debug the memory used by this model.
        virtual void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const = 0;

        //! Get the memory used by this model.
        virtual std::size_t memoryUsage(void) const = 0;

        //! Get the static size of this object - used for virtual hierarchies.
        virtual std::size_t staticSize(void) const = 0;

        //! Get the non-estimated value of the the memory used by this model.
        virtual std::size_t computeMemoryUsage(void) const = 0;

        //! Estimate the memory usage of the model based on number of people,
        //! attributes and correlations.
        virtual std::size_t estimateMemoryUsage(std::size_t numberPeople,
                                                std::size_t numberAttributes,
                                                std::size_t numberCorrelations) const;

    protected:
        typedef std::vector<TStrCRef> TStrCRefVec;
        typedef std::pair<TDouble1Vec, double> TDouble1VecDoublePr;
        typedef std::pair<TStrCRef, TDouble1VecDoublePr> TStrCRefDouble1VecDoublePrPr;
        typedef std::vector<TStrCRefDouble1VecDoublePrPr> TStrCRefDouble1VecDoublePrPrVec;
        typedef std::vector<TStrCRefDouble1VecDoublePrPrVec> TStrCRefDouble1VecDoublePrPrVecVec;
        typedef std::pair<TStrCRef, TDouble1VecDouble1VecPr> TStrCRefDouble1VecDouble1VecPrPr;
        typedef std::vector<TStrCRefDouble1VecDouble1VecPrPr> TStrCRefDouble1VecDouble1VecPrPrVec;
        typedef std::vector<TStrCRefDouble1VecDouble1VecPrPrVec> TStrCRefDouble1VecDouble1VecPrPrVecVec;
        typedef std::vector<TStrCRefDouble1VecDouble1VecPrPrVecVec> TStrCRefDouble1VecDouble1VecPrPrVecVecVec;

        //! \brief Tests if two influence values are for the same influence.
        struct SInfluenceEqual
        {
            bool operator()(const TStrCRefDouble1VecDoublePrPr &lhs,
                            const TStrCRefDouble1VecDoublePrPr &rhs)
            {
                return lhs.first.get() == rhs.first.get();
            }
            bool operator()(const TStrCRefDouble1VecDoublePrPr &lhs,
                            const TStrCRef &rhs)
            {
                return lhs.first.get() == rhs.get();
            }
            bool operator()(const TStrCRef &lhs,
                            const TStrCRefDouble1VecDoublePrPr &rhs)
            {
                return lhs.get() == rhs.first.get();
            }
        };

    protected:
        //! The minimum permitted variance scale due to seasonal variation
        //! in the time series variance.
        static const double MINIMUM_SEASONAL_VARIANCE_SCALE;

    protected:
        //! Persist state by passing information to the supplied inserter.
        void doAcceptPersistInserter(core::CStatePersistInserter &inserter) const;

        //! Restore the model reading state from the supplied traverser.
        bool doAcceptRestoreTraverser(const model_t::TAnyRestoreFunc &extraDataRestoreFunc,
                                      core::CStateRestoreTraverser &traverser);

        //! Get the start time of the current bucket.
        virtual core_t::TTime currentBucketStartTime(void) const = 0;

        //! Set the start time of the current bucket.
        virtual void currentBucketStartTime(core_t::TTime time) = 0;

        //! Monitor the resource usage while creating new models.
        void createUpdateNewModels(core_t::TTime time,
                                   CResourceMonitor &resourceMonitor);

        //! Create the time series models for "n" newly observed people.
        virtual void createNewModels(std::size_t n, std::size_t m) = 0;

        //! Reinitialize the time series models for recycled people.
        virtual void updateRecycledModels(void) = 0;

        //! Update the correlation models.
        void refreshCorrelationModels(std::size_t resourceLimit,
                                      CResourceMonitor &resourceMonitor);

        //! Clear out large state objects for people that are pruned.
        virtual void clearPrunedResources(const TSizeVec &people,
                                          const TSizeVec &attributes) = 0;

        //! Get the person unique identifiers which have a feature value
        //! in the bucketing time interval including \p time.
        template<typename T>
        void currentBucketPersonIds(core_t::TTime time, const T &featureData, TSizeVec &result) const;

        //! Get the value of the \p feature of the person identified
        //! by \p pid for the bucketing interval containing \p time.
        template<typename T>
        const T *featureData(model_t::EFeature feature,
                             std::size_t pid,
                             core_t::TTime time,
                             const std::vector<std::pair<model_t::EFeature,
                             std::vector<std::pair<std::size_t, T> > > > &featureData) const;

        //! Sample the bucket statistics and write the results in to
        //! \p featureData.
        template<typename T, typename FILTER>
        void sampleBucketStatistics(core_t::TTime startTime,
                                    core_t::TTime endTime,
                                    const FILTER &filter,
                                    std::vector<std::pair<model_t::EFeature, T> > &featureData,
                                    CResourceMonitor &resourceMonitor);

        //! Add the probability and influences for \p feature and \p pid.
        template<typename PARAMS, typename INFLUENCES>
        bool addProbabilityAndInfluences(std::size_t pid,
                                         PARAMS &params,
                                         const INFLUENCES &influences,
                                         CProbabilityAndInfluenceCalculator &pJoint,
                                         CAnnotatedProbabilityBuilder &builder) const;

        //! Get the weight associated with an update to the prior from an empty bucket
        //! for features which count empty buckets.
        double emptyBucketWeight(model_t::EFeature feature,
                                 std::size_t pid,
                                 core_t::TTime time) const;

        //! Get the "probability the bucket is empty" to use to correct probabilities
        //! for features which count empty buckets.
        double probabilityBucketEmpty(model_t::EFeature feature, std::size_t pid) const;

        //! Check if there is a prior for \p feature and the person
        //! identified by \p pid.
        bool hasPrior(model_t::EFeature feature, std::size_t pid) const;

        //! Get the univariate prior corresponding to \p feature
        //! for the person identified by \p pid.
        const maths::CPrior *prior(model_t::EFeature feature, std::size_t pid) const;

        //! Get the univariate prior corresponding to \p feature
        //! for the person identified by \p pid.
        maths::CPrior *prior(model_t::EFeature feature, std::size_t pid);

        //! Get the multivariate prior corresponding to \p feature
        //! for the person identified by \p pid.
        const maths::CMultivariatePrior *multivariatePrior(model_t::EFeature feature,
                                                           std::size_t pid) const;

        //! Get the multivariate prior corresponding to \p feature
        //! for the person identified by \p pid.
        maths::CMultivariatePrior *multivariatePrior(model_t::EFeature feature,
                                                     std::size_t pid);

        //! Get the multivariate prior corresponding to \p feature
        //! for the correlated defined by \p pid and \p correlated.
        void correlatePriors(model_t::EFeature feature,
                             std::size_t pid,
                             TSize1Vec &correlated,
                             TSize2Vec1Vec &variables,
                             TMultivariatePriorCPtrSizePr1Vec &priors) const;

        //! Get the correction to apply to the mean to account for
        //! the specified value of the correlated random variable.
        double correctBaselineForCorrelated(model_t::EFeature feature,
                                            std::size_t pid,
                                            model_t::CResultType type,
                                            const TSizeDoublePr1Vec &correlated) const;

        //! Correct \p baseline with \p corrections for interim results.
        void correctBaselineForInterim(model_t::EFeature feature,
                                       std::size_t pid,
                                       model_t::CResultType type,
                                       const TSizeDoublePr1Vec &correlated,
                                       const TFeatureSizeSizeTripleDouble1VecUMap &corrections,
                                       TDouble1Vec &baseline) const;

        //! Get the first time each person was seen.
        const TTimeVec &firstBucketTimes(void) const;

        //! Get all the univariate priors for \p feature.
        const TPriorPtrVec *priors(model_t::EFeature feature) const;

        //! Writable access for all the univariate priors for \p feature.
        TPriorPtrVec *priors(model_t::EFeature feature);

        //! Get the new univariate prior for \p feature.
        const maths::CPrior *newPrior(model_t::EFeature feature) const;

        //! Get all the multivariate priors for \p feature.
        const TMultivariatePriorPtrVec *multivariatePriors(model_t::EFeature feature) const;

        //! Writable access for all the multivariate priors for \p feature.
        TMultivariatePriorPtrVec *multivariatePriors(model_t::EFeature feature);

        //! Get the new multivariate prior for \p feature.
        const maths::CMultivariatePrior *newMultivariatePrior(model_t::EFeature feature) const;

        //! Get all the correlate priors for \p feature.
        const TSizeSizePrMultivariatePriorPtrDoublePrUMap *correlatePriors(model_t::EFeature feature) const;

        //! Writable access for all the correlate priors for \p feature.
        TSizeSizePrMultivariatePriorPtrDoublePrUMap *correlatePriors(model_t::EFeature feature);

        //! Get the new correlate prior for \p feature.
        const maths::CMultivariatePrior *newCorrelatePrior(model_t::EFeature feature) const;

        //! Get the total number of correlation models.
        std::size_t numberCorrelations(void) const;

        //! Get the trend for \p feature and \p pid.
        //!
        //! \param[in] feature The feature of interest.
        //! \param[in] pid The identifier of the person of interest.
        TDecompositionCPtr1Vec trend(model_t::EFeature feature, std::size_t pid) const;

        //! Non-const overload.
        const TDecompositionPtr1Vec &trend(model_t::EFeature feature, std::size_t pid);

        //! Remove the trend for \p feature and \p pid from \p value.
        //!
        //! \param[in] feature The value's feature.
        //! \param[in] pid The value's person identifier.
        //! \param[in] time The time of the value.
        //! \param[in] confidence The symmetric confidence interval to
        //! compute for the trend.
        //! \param[in] value The value to detrend.
        TDouble1Vec detrend(model_t::EFeature feature,
                            std::size_t pid,
                            core_t::TTime time,
                            double confidence,
                            const TDouble1Vec &value) const;

        //! Get the variance scale for \p feature and \p pid at \p time.
        //!
        //! \param[in] feature The value's feature.
        //! \param[in] pid The value's person identifier.
        //! \param[in] time The time of interest.
        //! \param[in] confidence The symmetric confidence to compute
        //! for the variance scale.
        TDouble1VecDouble1VecPr seasonalVarianceScale(model_t::EFeature feature,
                                                      std::size_t pid,
                                                      core_t::TTime time,
                                                      double confidence) const;

        //! Update the trend with the value \p value for \p feature and
        //! \p pid and return the detrended value, i.e. the residual
        //! after removing the trend and offset so its mean is equal to
        //! the process mean.
        //!
        //! \param[in] feature The value's feature.
        //! \param[in] pid The value's person identifier.
        //! \param[in] time The time of the value.
        //! \param[in] value The value with which to update the trend.
        //! \param[in] weightStyles The styles of \p weights. Both the
        //! count and the Winsorisation weight styles have an effect.
        //! See maths_t::ESampleWeightStyle for more details.
        //! \param[in] weights The weights of \p value. The smaller
        //! the product count weight the less influence \p value has
        //! on the trend and it's local variance. Note that weights
        //! should be greater than zero.
        TDouble1Vec updateTrend(model_t::EFeature feature,
                                std::size_t pid,
                                core_t::TTime time,
                                const TDouble1Vec &value,
                                const maths_t::TWeightStyleVec &weightStyles,
                                const TDouble1Vec4Vec &weights);

        //! Get the decay rate \p controller for \p feature and \p pid.
        CDecayRateController *decayRateController(EDecayRateController controller,
                                                  model_t::EFeature feature,
                                                  std::size_t pid);

        //! Get the decay rate multiplier to apply to \p feature and \p pid
        //! and update the controller state.
        double decayRateMultiplier(EDecayRateController controller,
                                   model_t::EFeature feature,
                                   std::size_t pid,
                                   const TDouble1Vec &prediction,
                                   const TDouble1Vec &value);

        //! Get the amount by which to derate the initial decay rate
        //! and the minimum Winsorisation weight for \p pid at \p time.
        double derate(std::size_t pid, core_t::TTime time) const;

        //! Print the current bucketing interval.
        std::string printCurrentBucket(void) const;

    private:
        //! Get the person counts in the current bucket.
        virtual const TSizeUInt64PrVec &currentBucketPersonCounts(void) const = 0;

        //! Get writable person counts in the current bucket.
        virtual TSizeUInt64PrVec &currentBucketPersonCounts(void) = 0;

        //! Set the current bucket total count.
        virtual void currentBucketTotalCount(uint64_t totalCount) = 0;

        //! Update the mean count with all non-zero counts.
        virtual void sampleCounts(core_t::TTime startTime, core_t::TTime endTime) = 0;

        //! Returns one.
        virtual double attributeFrequency(std::size_t cid) const;

        //! Perform derived class specific operations to accomplish skipping sampling
        virtual void doSkipSampling(core_t::TTime startTime, core_t::TTime endTime);

    private:
        //! The time that each person was first seen.
        TTimeVec m_FirstBucketTimes;

        //! The last time that each person was seen.
        TTimeVec m_LastBucketTimes;

        //! The initial prior for a newly observed person for each
        //! univariate feature.
        TFeaturePriorPtrPrVec m_NewFeaturePriors;

        //! The initial prior for a newly observed person for each
        //! multivariate feature.
        TFeatureMultivariatePriorPtrPrVec m_NewMultivariateFeaturePriors;

        //! The initial prior for a newly found correlated pairs of
        //! people for each univariate feature.
        TFeatureMultivariatePriorPtrPrVec m_NewCorrelateFeaturePriors;

        //! The person priors for each univariate feature.
        TFeaturePriorPtrVecMap m_Priors;

        //! The person priors for each multivariate feature.
        TFeatureMultivariatePriorPtrVecMap m_MultivariatePriors;

        //! The models used for modeling pairs of person values which
        //! we have determined display significant correlation.
        TFeatureSizeSizePrMultivariatePriorPtrDoublePrUMapMap m_CorrelatePriors;

        //! The initial time series periodic decomposition for a newly observed
        //! person for each feature.
        TFeatureDecompositionCPtrVecPrVec m_NewDecompositions;

        //! The decomposition of the feature time series into periodic components.
        TFeatureDecompositionPtr1VecVecPrVec m_Decompositions;

        //! The controllers for the person model decay rates.
        TFeatureDecayRateControllerArrayVecPrVec m_DecayRateControllers;

        //! The memory estimator.
        mutable CMemoryUsageEstimator m_MemoryEstimator;
};

}
}

#endif // INCLUDED_prelert_model_CIndividualModel_h
