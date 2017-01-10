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

#ifndef INCLUDED_ml_model_CPopulationModel_h
#define INCLUDED_ml_model_CPopulationModel_h

#include <core/CMemory.h>
#include <core/CNonCopyable.h>
#include <core/CoreTypes.h>
#include <core/CStatistics.h>
#include <core/CTriple.h>

#include <maths/CBjkstUniqueValues.h>
#include <maths/CCountMinSketch.h>
#include <maths/CMultivariatePrior.h>
#include <maths/COrderings.h>

#include <model/CDecayRateController.h>
#include <model/CFeatureData.h>
#include <model/CModel.h>
#include <model/ImportExport.h>
#include <model/ModelTypes.h>

#include <map>
#include <string>
#include <utility>
#include <vector>

namespace ml
{
namespace core
{
class CStatePersistInserter;
class CStateRestoreTraverser;
}
namespace maths
{
class CPrior;
}
namespace model
{

//! \brief The most basic population model interface.
//!
//! DESCRIPTION:\n
//! This defines the interface common to all probabilistic models of the
//! (random) processes which describe person and population's state. It
//! declares core functions used by the anomaly detection code to:
//!   -# Sample the processes in a specified time interval and update
//!      the models.
//!   -# Compute the probability of a person's processes in a specified
//!      time interval.
//!
//! IMPLEMENTATION DECISIONS:\n
//! The population model hierarchy has been abstracted to gather up the
//! implementation which can be shared by the event rate and metric models
//! to avoid unnecessary code duplication.
//!
//! It assumes data are supplied in time order since this means minimal
//! state can be maintained.
class MODEL_EXPORT CPopulationModel : public CModel
{
    public:
        typedef std::vector<TDouble1Vec> TDouble1VecVec;
        typedef std::vector<core_t::TTime> TTimeVec;
        typedef TTimeVec::const_iterator TTimeVecCItr;
        typedef std::pair<std::size_t, uint64_t> TSizeUInt64Pr;
        typedef std::vector<TSizeUInt64Pr> TSizeUInt64PrVec;
        typedef std::vector<maths::CCountMinSketch> TCountMinSketchVec;
        typedef std::vector<maths::CBjkstUniqueValues> TBjkstUniqueValuesVec;
        typedef std::pair<TStrCRef, TStrCRef> TStrCRefStrCRefPr;
        typedef std::map<TStrCRefStrCRefPr,
                         uint64_t,
                         maths::COrderings::SLess> TStrCRefStrCRefPrUInt64Map;
        typedef core::CTriple<model_t::EFeature, std::size_t, std::size_t> TFeatureSizeSizeTriple;
        typedef boost::unordered_map<TFeatureSizeSizeTriple, TDouble1Vec> TFeatureSizeSizeTripleDouble1VecUMap;
        typedef TFeatureSizeSizeTripleDouble1VecUMap::const_iterator TFeatureSizeSizeTripleDouble1VecUMapCItr;


        //! Lift the overloads of baselineBucketMean into the class scope.
        using CModel::baselineBucketMean;

        //! Lift the overloads of acceptPersistInserter into the class scope.
        using CModel::acceptPersistInserter;

    public:
        //! \name Life-cycle.
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
        //! \param[in] isForRestore True if this constructor being used for restore.
        CPopulationModel(const SModelParams &params,
                         const TDataGathererPtr &dataGatherer,
                         const TFeaturePriorPtrPrVec &newPriors,
                         const TFeatureMultivariatePriorPtrPrVec &newMultivariatePriors,
                         const TFeatureDecompositionCPtrVecPrVec &newDecompositions,
                         const TFeatureInfluenceCalculatorCPtrPrVecVec &influenceCalculators,
                         bool isForRestore);

        //! Create a copy that will result in the same persisted state as the
        //! original.  This is effectively a copy constructor that creates a
        //! copy that's only valid for a single purpose.  The boolean flag is
        //! redundant except to create a signature that will not be mistaken
        //! for a general purpose copy constructor.
        CPopulationModel(bool isForPersistence,
                         const CPopulationModel &other);
        //@}

        //! Returns true.
        virtual bool isPopulation(void) const;

        //! \name Bucket Statistics
        //@{
        //! Get the count of the bucketing interval containing \p time
        //! for the person identified by \p pid.
        virtual TOptionalUInt64 currentBucketCount(std::size_t pid,
                                                   core_t::TTime time) const;

        //! Returns null.
        virtual TOptionalDouble baselineBucketCount(std::size_t pid) const;

    protected:
        //! Find the person attribute pair identified by \p pid and \p cid,
        //! respectively, in \p data if it exists. Returns the end of the
        //! vector if it doesn't.
        template<typename T>
        static typename T::const_iterator find(const T &data, std::size_t pid, std::size_t cid);

        //! Extract the bucket value for count feature data.
        static inline TDouble1Vec extractValue(model_t::EFeature /*feature*/,
                                               const std::pair<TSizeSizePr, SEventRateFeatureData> &data);
        //! Extract the bucket value for metric feature data.
        static inline TDouble1Vec extractValue(model_t::EFeature feature,
                                               const std::pair<TSizeSizePr, SMetricFeatureData> &data);

        //! Get the current bucket value for \p feature, \p pid and \p cid
        //! from \p featureData.
        template<typename T>
        TDouble1Vec currentBucketValue(const T &featureData,
                                       model_t::EFeature feature,
                                       std::size_t pid,
                                       std::size_t cid,
                                       const TDouble1Vec &fallback) const;
        //@}

    public:
        //! \name Person
        //@{
        //! Get the person unique identifiers which are present in the
        //! bucketing time interval including \p time.
        //!
        //! \param[in] time The time of interest.
        //! \param[out] result Filled in with the person identifiers
        //! in the bucketing time interval of interest.
        virtual void currentBucketPersonIds(core_t::TTime time,
                                            TSizeVec &result) const;
        //@}

        //! \name Update
        //@{
        //! Sample any state needed by computeProbablity for the out-
        //! of-phase bucket in the time interval [\p startTime, \p endTime]
        //! but do not update the model.
        //!
        //! \param[in] startTime The start of the time interval to sample.
        //! \param[in] endTime The end of the time interval to sample.
        virtual void sampleOutOfPhase(core_t::TTime startTime,
                                      core_t::TTime endTime,
                                      CResourceMonitor &resourceMonitor);

        //! Update the rates for \p feature and \p people.
        virtual void sample(core_t::TTime startTime,
                            core_t::TTime endTime,
                            CResourceMonitor &resourceMonitor) = 0;
        //@}

    private:
        //! Updates the checksums with \p priors.
        template<typename PRIORS>
        void checksumsImpl(const PRIORS &priors,
                           CPopulationModel::TStrCRefStrCRefPrUInt64Map &checksums) const;

    protected:
        //! Update the checksums with \p populationPriors.
        void checksums(const TFeaturePriorPtrVecMap &populationPriors,
                       TStrCRefStrCRefPrUInt64Map &checksums) const;

        //! Update the checksums with \p populationMultivariatePriors.
        void checksums(const TFeatureMultivariatePriorPtrVecMap &populationMultivariatePriors,
                       TStrCRefStrCRefPrUInt64Map &checksums) const;

        //! Update the checksums with \p personCounts.
        void checksums(const TSizeUInt64PrVec &personCounts,
                       TStrCRefStrCRefPrUInt64Map &checksums) const;

        //! Get the start time of the current bucket.
        virtual core_t::TTime currentBucketStartTime(void) const = 0;

        //! Set the start time of the current bucket.
        virtual void currentBucketStartTime(core_t::TTime time) = 0;

    public:
        //! Get the checksum of this model.
        //!
        //! \param[in] includeCurrentBucketStats If true then include the
        //! current bucket statistics. (This is designed to handle serialization,
        //! for which we don't serialize the current bucket statistics.)
        virtual uint64_t checksum(bool includeCurrentBucketStats = true) const = 0;

        //! Debug the memory used by this model.
        virtual void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const = 0;

        //! Get the memory used by this model.
        virtual std::size_t memoryUsage(void) const = 0;

        //! Get the static size of this object - used for virtual hierarchies
        virtual std::size_t staticSize(void) const = 0;

        //! Get the non-estimated value of the the memory used by this model.
        virtual std::size_t computeMemoryUsage(void) const = 0;

        //! Estimate the memory usage of the model based on number of people,
        //! attributes and correlations.
        virtual std::size_t estimateMemoryUsage(std::size_t numberPeople,
                                                std::size_t numberAttributes,
                                                std::size_t numberCorrelations) const = 0;

        //! Get the new prior for \p feature.
        const maths::CPrior *newPrior(model_t::EFeature feature) const;

        //! Get the new prior for \p feature.
        maths::CPrior *newPrior(model_t::EFeature feature);

        //! Get the new prior for multivariate \p feature.
        const maths::CMultivariatePrior *newMultivariatePrior(model_t::EFeature feature) const;

        //! Get the new prior for multivariate \p feature.
        maths::CMultivariatePrior *newMultivariatePrior(model_t::EFeature feature);

        //! Get the frequency of the attribute identified by \p cid.
        virtual double attributeFrequency(std::size_t cid) const;

        //! Get the weight for \p feature and the person identified by
        //! \p pid based on their sample rate.
        double sampleRateWeight(std::size_t pid, std::size_t cid) const;

    protected:
        typedef std::vector<TDouble1Vec4Vec> TDouble1Vec4VecVec;

        //! Wraps up the sampled data for a feature.
        struct MODEL_EXPORT SFeatureSampleData
        {
            SFeatureSampleData(void) : s_IsInteger(true) {}
            TDouble1VecVec s_BucketValues;
            TTimeVec s_Times;
            TDouble1VecVec s_Samples;
            TDouble1Vec4VecVec s_Weights;
            bool s_IsInteger;
        };

        typedef boost::unordered_map<std::size_t, SFeatureSampleData> TSizeFeatureSampleDataUMap;
        typedef TSizeFeatureSampleDataUMap::iterator TSizeFeatureSampleDataUMapItr;
        typedef std::map<model_t::EFeature, TSizeFeatureSampleDataUMap> TFeatureSizeFeatureSampleDataUMapMap;
        typedef TFeatureSizeFeatureSampleDataUMapMap::iterator TFeatureSizeFeatureSampleDataUMapMapItr;

    protected:
        //! Persist state by passing information to the supplied inserter.
        void doAcceptPersistInserter(core::CStatePersistInserter &inserter) const;

        //! Restore the model reading state from the supplied traverser.
        bool doAcceptRestoreTraverser(const model_t::TAnyRestoreFunc &extraDataRestoreFunc,
                                      core::CStateRestoreTraverser &traverser);

        //! Convert a person's prior for a particular feature and attribute
        //! to a state document.
        static void personFeaturePriorAcceptPersistInserter(const std::string &featureTag,
                                                            const std::string &personTag,
                                                            const std::string &attributeTag,
                                                            const std::string &priorTag,
                                                            const TFeatureSizePriorPtrUMapVecMapCItr &iter,
                                                            core::CStatePersistInserter &inserter);

        //! Convert a prior for a particular attribute to a state document.
        static void attributePriorAcceptPersistInserter(const std::string &attributeTag,
                                                        const std::string &priorTag,
                                                        const TSizePriorPtrUMap &attributeData,
                                                        core::CStatePersistInserter &inserter);

        //! Monitor the resource usage while creating new models
        void createUpdateNewModels(core_t::TTime time, CResourceMonitor &resourceMonitor);

        //! Initialize the time series models for "n" newly observed people
        //! and "m" newly observed attributes.
        virtual void createNewModels(std::size_t n, std::size_t m) = 0;

        //! Reset the population priors for the attributes identified by
        //! \p attributes to non-informative.
        void reinitializeAttributePriors(const TSizeVec &attributes,
                                         TFeaturePriorPtrVecMap &priors,
                                         TFeatureMultivariatePriorPtrVecMap &multivariatePriors);

        //! Reset the population priors for the attributes identified by
        //! \p attributes to the stub small prior.
        void clearAttributePriors(const TSizeVec &attributes,
                                  TFeaturePriorPtrVecMap &priors,
                                  TFeatureMultivariatePriorPtrVecMap &multivariatePriors);

        //! Initialize the time series models for recycled attributes
        //! and/or people.
        virtual void updateRecycledModels(void) = 0;

        //! Clear out large state objects for people/attributes that are pruned.
        virtual void clearPrunedResources(const TSizeVec &people,
                                          const TSizeVec &attributes) = 0;

        //! Update the correlation models.
        void refreshCorrelationModels(std::size_t resourceLimit,
                                      CResourceMonitor &resourceMonitor);

        //! Update the population priors.
        void updatePriors(const maths_t::TWeightStyleVec &weightStyles,
                          TFeatureSizeFeatureSampleDataUMapMap &data,
                          TFeaturePriorPtrVecMap &priors,
                          TFeatureMultivariatePriorPtrVecMap &multivariatePriors);

        //! Get the univariate prior for \p feature and the attribute
        //! identified by \p cid.
        virtual const maths::CPrior *prior(model_t::EFeature feature, std::size_t cid) const = 0;

        //! Get the multivariate prior for \p feature and the attribute
        //! identified by \p cid.
        virtual const maths::CMultivariatePrior *multivariatePrior(model_t::EFeature feature,
                                                                   std::size_t cid) const = 0;


        //! Reinitialize the prior(s) for \p feature and the attribute
        //! identified by \p cid.
        //!
        //! \param[in] feature The feature of interest.
        //! \param[in] cid The identifier of the attribute of interest.
        //! \return True if the prior was reset.
        virtual bool resetPrior(model_t::EFeature feature, std::size_t cid) = 0;

        //! Correct \p baseline with \p corrections for interim results.
        void correctBaselineForInterim(model_t::EFeature feature,
                                       std::size_t pid,
                                       std::size_t cid,
                                       model_t::CResultType type,
                                       const TFeatureSizeSizeTripleDouble1VecUMap &corrections,
                                       TDouble1Vec &baseline) const;

        //! Get the time by which to propagate the priors on a sample.
        double propagationTime(std::size_t cid, core_t::TTime) const;

        //! Get the current bucket person counts.
        virtual const TSizeUInt64PrVec &personCounts(void) const = 0;

        //! Check if bucket statistics are available for the specified time.
        virtual bool bucketStatsAvailable(core_t::TTime time) const = 0;

        //! Get the trend for \p feature and the attribute identified by \p cid.
        //!
        //! \param[in] feature The feature of interest.
        //! \param[in] cid The identifier of the attribute of interest.
        TDecompositionCPtr1Vec trend(model_t::EFeature feature, std::size_t cid) const;

        //! Non-const overload.
        const TDecompositionPtr1Vec &trend(model_t::EFeature feature, std::size_t cid);

        //! Remove the trend for \p feature and \p cid from \p value.
        //!
        //! \param[in] feature The value's feature.
        //! \param[in] cid The value's attribute identifier.
        //! \param[in] time The time of the value.
        //! \param[in] confidence The symmetric confidence interval to
        //! compute for the trend.
        //! \param[in] value The value to detrend.
        TDouble1Vec detrend(model_t::EFeature feature,
                            std::size_t cid,
                            core_t::TTime time,
                            double confidence,
                            const TDouble1Vec &value) const;

        //! Compute the variance scale for \p feature and \p pid at
        //! \p time.
        //!
        //! \param[in] feature The value's feature.
        //! \param[in] cid The value's attribute identifier.
        //! \param[in] time The time of interest.
        //! \param[in] confidence The symmetric confidence to compute
        //! for the variance scale.
        TDouble1VecDouble1VecPr seasonalVarianceScale(model_t::EFeature feature,
                                                      std::size_t cid,
                                                      core_t::TTime time,
                                                      double confidence) const;

        //! Update the trend at \p time with the samples \p samples.
        //! Update the trend with the values \p values for \p feature
        //! and \p cid.
        //!
        //! \param[in] feature The value's feature.
        //! \param[in] cid The value's attribute identifier.
        //! \param[in] times The times of the values.
        //! \param[in] values The values with which to update the trend.
        void updateTrend(model_t::EFeature feature,
                         std::size_t cid,
                         const TTimeVec &times,
                         const TDouble1VecVec &values);

        //! Get the decay rate \p controller for \p feature and \p cid.
        CDecayRateController *decayRateController(EDecayRateController controller,
                                                  model_t::EFeature feature,
                                                  std::size_t cid);

        //! Get the decay rate multiplier to apply to \p feature and \p cid
        //! and update the controller state.
        double decayRateMultiplier(EDecayRateController controller,
                                   model_t::EFeature feature,
                                   std::size_t cid,
                                   const TDouble1Vec &prediction,
                                   const TDouble1Vec &value);

        //! Remove heavy hitting people and attributes from the feature
        //! data if necessary.
        template<typename T, typename PERSON_FILTER, typename ATTRIBUTE_FILTER>
        void applyFilters(model_t::EFeature feature,
                          bool updateStatistics,
                          const PERSON_FILTER &personFilter,
                          const ATTRIBUTE_FILTER &attributeFilter,
                          T &data) const;

        //! Writable access to the new feature priors.
        TFeaturePriorPtrPrVec &newPriors(void);
        //! Writable access to the new feature priors.
        TFeatureMultivariatePriorPtrPrVec &newMultivariatePriors(void);

        //! Get the first time each attribute was seen.
        const TTimeVec &attributeFirstBucketTimes(void) const;
        //! Get the last time each attribute was seen.
        const TTimeVec &attributeLastBucketTimes(void) const;

        //! Get the people and attributes to remove if any.
        void peopleAndAttributesToRemove(core_t::TTime time,
                                         std::size_t maximumAge,
                                         TSizeVec &peopleToRemove,
                                         TSizeVec &attributesToRemove) const;

        //! Remove the \p people.
        void removePeople(const TSizeVec &peopleToRemove);

    protected:
        //! The minimum permitted variance scale due to seasonal variation
        //! in the time series variance.
        static const double MINIMUM_SEASONAL_VARIANCE_SCALE;

    private:
        typedef boost::optional<maths::CCountMinSketch> TOptionalCountMinSketch;
        typedef maths::CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;
        typedef core::CSmallVector<TMeanAccumulator, 1> TMeanAccumulator1Vec;

    private:
        //! Perform derived class specific operations to accomplish skipping sampling
        virtual void doSkipSampling(core_t::TTime startTime, core_t::TTime endTime);

        //! Get the trend and prior prediction residuals.
        template<typename PRIOR>
        void residuals(double interval,
                       const TDecompositionPtr1Vec &trend,
                       const PRIOR &prior,
                       const TDouble1Vec &sample,
                       TMeanAccumulator1Vec (&result)[2]) const;

        //! Set the current bucket total count.
        virtual void currentBucketTotalCount(uint64_t totalCount) = 0;

    private:
        //! The last time each person was seen.
        TTimeVec m_PersonLastBucketTimes;

        //! The first time each attribute was seen.
        TTimeVec m_AttributeFirstBucketTimes;

        //! The last time each attribute was seen.
        TTimeVec m_AttributeLastBucketTimes;

        //! The initial prior for a newly observed attribute for each univariate
        //! feature.
        TFeaturePriorPtrPrVec m_NewFeaturePriors;

        //! The initial prior for a newly observed attribute for each multivariate
        //! feature.
        TFeatureMultivariatePriorPtrPrVec m_NewMultivariateFeaturePriors;

        //! The initial time series periodic decomposition for a newly observed
        //! attribute for each feature.
        TFeatureDecompositionCPtrVecPrVec m_NewDecompositions;

        //! The decomposition of the feature time series into periodic components.
        TFeatureDecompositionPtr1VecVecPrVec m_Decompositions;

        //! The controllers for the person model decay rates.
        TFeatureDecayRateControllerArrayVecPrVec m_DecayRateControllers;

        //! The initial sketch to use for estimating the number of distinct people.
        maths::CBjkstUniqueValues m_NewDistinctPersonCounts;

        //! The number of distinct people generating each attribute.
        TBjkstUniqueValuesVec m_DistinctPersonCounts;

        //! The initial sketch to use for estimating person bucket counts.
        TOptionalCountMinSketch m_NewPersonBucketCounts;

        //! The bucket count of each (person, attribute) pair in the exponentially
        //! decaying window with decay rate equal to CModel::m_DecayRate.
        TCountMinSketchVec m_PersonAttributeBucketCounts;
};

}
}

#endif // INCLUDED_ml_model_CPopulationModel_h
