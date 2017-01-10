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

#ifndef INCLUDED_ml_model_CModelFactory_h
#define INCLUDED_ml_model_CModelFactory_h

#include <core/CoreTypes.h>
#include <core/CNonCopyable.h>

#include <maths/COrderings.h>
#include <maths/MathsTypes.h>

#include <model/CModelParams.h>
#include <model/CSearchKey.h>
#include <model/ImportExport.h>
#include <model/ModelTypes.h>

#include <boost/optional.hpp>
#include <boost/ref.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/unordered_map.hpp>

#include <map>
#include <utility>
#include <vector>

namespace ml
{
namespace core
{
class CStateRestoreTraverser;
}

namespace maths
{
class CBjkstUniqueValues;
class CCountMinSketch;
class CMultinomialConjugate;
class CMultivariatePrior;
class CPrior;
class CTimeSeriesDecompositionInterface;
}

namespace model
{
class CDataGatherer;
class CDetectionRule;
class CInfluenceCalculator;
class CModel;
class CSearchKey;

//! \brief A factory class interface for the CModel hierarchy.
//!
//! DESCRIPTION:\n
//! The interface for the factory classes for making concrete objects
//! in the CModel hierarchy.
//!
//! IMPLEMENTATION DECISIONS:\n
//! The CModelConfig object is able to dynamically configure the
//! anomaly detection classes:
//!   -# CAnomalyDetector,
//!   -# CModelEnsemble,
//!      ...
//!
//! to either compute online or delta probabilities for log messages,
//! metric values, etc. This hierarchy implements the factory pattern
//! for the CModel hierarchy for this purpose.
class MODEL_EXPORT CModelFactory
{
    public:
        typedef std::vector<double> TDoubleVec;
        typedef std::vector<std::size_t> TSizeVec;
        typedef std::vector<core_t::TTime> TTimeVec;
        typedef std::vector<model_t::EFeature> TFeatureVec;
        typedef std::vector<std::string> TStrVec;
        typedef boost::optional<unsigned int> TOptionalUInt;
        typedef boost::reference_wrapper<const std::string> TStrCRef;
        typedef std::vector<TStrCRef> TStrCRefVec;
        typedef boost::shared_ptr<CDataGatherer> TDataGathererPtr;
        typedef boost::shared_ptr<maths::CPrior> TPriorPtr;
        typedef std::vector<TPriorPtr> TPriorPtrVec;
        typedef std::pair<model_t::EFeature, TPriorPtr> TFeaturePriorPtrPr;
        typedef std::vector<TFeaturePriorPtrPr> TFeaturePriorPtrPrVec;
        typedef boost::shared_ptr<maths::CMultivariatePrior> TMultivariatePriorPtr;
        typedef std::vector<TMultivariatePriorPtr> TMultivariatePriorPtrVec;
        typedef std::pair<model_t::EFeature, TMultivariatePriorPtr> TFeatureMultivariatePriorPtrPr;
        typedef std::vector<TFeatureMultivariatePriorPtrPr> TFeatureMultivariatePriorPtrPrVec;
        typedef boost::shared_ptr<CModel> TModelPtr;
        typedef boost::shared_ptr<const CModel> TModelCPtr;
        typedef boost::shared_ptr<const maths::CTimeSeriesDecompositionInterface> TDecompositionCPtr;
        typedef std::vector<TDecompositionCPtr> TDecompositionCPtrVec;
        typedef std::pair<model_t::EFeature, TDecompositionCPtrVec> TFeatureDecompositionCPtrVecPr;
        typedef std::vector<TFeatureDecompositionCPtrVecPr> TFeatureDecompositionCPtrVecPrVec;
        typedef boost::shared_ptr<const CInfluenceCalculator> TInfluenceCalculatorCPtr;
        typedef std::pair<model_t::EFeature, TInfluenceCalculatorCPtr> TFeatureInfluenceCalculatorCPtrPr;
        typedef std::vector<TFeatureInfluenceCalculatorCPtrPr> TFeatureInfluenceCalculatorCPtrPrVec;
        typedef std::vector<TFeatureInfluenceCalculatorCPtrPrVec> TFeatureInfluenceCalculatorCPtrPrVecVec;
        typedef std::vector<maths::CCountMinSketch> TCountMinSketchVec;
        typedef std::vector<maths::CBjkstUniqueValues> TBjkstUniqueValuesVec;
        typedef std::vector<CDetectionRule> TDetectionRuleVec;
        typedef boost::reference_wrapper<const TDetectionRuleVec> TDetectionRuleVecCRef;

    public:
        //! Wrapper around the model initialization data.
        //!
        //! IMPLEMENTATION DECISIONS:\n
        //! We wrap up the initialization data in an object so we don't
        //! need to change the signature of every factory function each
        //! time we need extra data to initialize a model.
        struct MODEL_EXPORT SModelInitializationData
        {
            explicit SModelInitializationData(const TDataGathererPtr &dataGatherer,
                                              const TModelCPtr &referenceModel = TModelCPtr(),
                                              const TFeaturePriorPtrPrVec &priors = TFeaturePriorPtrPrVec(),
                                              const TFeatureMultivariatePriorPtrPrVec &multivariatePriors = TFeatureMultivariatePriorPtrPrVec(),
                                              const TFeatureMultivariatePriorPtrPrVec &correlatePriors = TFeatureMultivariatePriorPtrPrVec());

            TDataGathererPtr s_DataGatherer;
            TModelCPtr s_ReferenceModel;
            TFeaturePriorPtrPrVec s_Priors;
            TFeatureMultivariatePriorPtrPrVec s_MultivariatePriors;
            TFeatureMultivariatePriorPtrPrVec s_CorrelatePriors;
        };

        //! Wrapper around the data gatherer initialization data.
        //!
        //! IMPLEMENTATION DECISIONS:\n
        //! We wrap up the initialization data in an object so we don't
        //! need to change the signature of every factory function each
        //! time we need extra data to initialize a data gatherer.
        struct MODEL_EXPORT SGathererInitializationData
        {
            SGathererInitializationData(const core_t::TTime &startTime,
                                        unsigned int sampleOverrideCount = 0u);

            core_t::TTime s_StartTime;
            unsigned int s_SampleOverrideCount;
        };

    public:
        static const std::string EMPTY_STRING;

    public:
        CModelFactory(const SModelParams &params);
        virtual ~CModelFactory(void);

        //! Create a copy of the factory owned by the calling code.
        virtual CModelFactory *clone(void) const = 0;

        //! \name Factory Methods
        //@{
        //! Make a new model.
        //!
        //! \param[in] initData The parameters needed to initialize the model.
        //! \warning It is owned by the calling code.
        virtual CModel *makeModel(const SModelInitializationData &initData) const = 0;

        //! Make a new model from part of a state document.
        //!
        //! \param[in] initData Additional parameters needed to initialize
        //! the model.
        //! \param[in,out] traverser A state document traverser.
        //! \warning It is owned by the calling code.
        virtual CModel *makeModel(const SModelInitializationData &initData,
                                  core::CStateRestoreTraverser &traverser) const = 0;

        //! Make a new data gatherer.
        //!
        //! \param[in] initData The parameters needed to initialize the
        //! data gatherer.
        //! \warning It is owned by the calling code.
        virtual CDataGatherer *makeDataGatherer(const SGathererInitializationData &initData) const = 0;

        //! Make a new data gatherer from part of a state document.
        //!
        //! \param[in,out] traverser A state document traverser.
        //! \warning It is owned by the calling code.
        virtual CDataGatherer *makeDataGatherer(core::CStateRestoreTraverser &traverser) const = 0;
        //@}

        //! \name Defaults
        //@{
        //! Get a collection of default priors to use for the univariate
        //! features in \p features.
        const TFeaturePriorPtrPrVec &defaultPriors(const TFeatureVec &features) const;

        //! Get a collection of default priors to use for the multivariate
        //! features in \p features.
        const TFeatureMultivariatePriorPtrPrVec &defaultMultivariatePriors(const TFeatureVec &features) const;

        //! Get a collection of default correlate priors to use for correlated
        //! pairs of time series for the  features in \p features.
        const TFeatureMultivariatePriorPtrPrVec &defaultCorrelatePriors(const TFeatureVec &features) const;

        //! Get the default prior to use for \p feature.
        TPriorPtr defaultPrior(model_t::EFeature feature) const;

        //! Get the default prior to use for multivariate \p feature.
        TMultivariatePriorPtr defaultMultivariatePrior(model_t::EFeature feature) const;

        //! Get the default prior to use for correlared pairs of time
        //! series for univariate \p feature.
        TMultivariatePriorPtr defaultCorrelatePrior(model_t::EFeature feature) const;

        //! Get the default prior for \p feature.
        //!
        //! \param[in] feature The feature for which to get the prior.
        //! \param[in] offset The fixed offset to apply to all samples.
        //! \param[in] decayRate Controls the rate at which the prior
        //! returns to non-informative.
        //! \param[in] minimumModeFraction The minimum permitted fraction
        //! of points in a distribution mode.
        //! \param[in] minimumModeCount The minimum permitted count of
        //! points in a distribution mode.
        virtual TPriorPtr defaultPrior(model_t::EFeature feature,
                                       double offset,
                                       double decayRate,
                                       double minimumModeFraction,
                                       double minimumModeCount,
                                       double minimumCategoryCount) const = 0;

        //! Get the default prior for multivariate \p feature.
        //!
        //! \param[in] feature The feature for which to get the prior.
        //! \param[in] offset The fixed offset to apply to all samples.
        //! \param[in] decayRate Controls the rate at which the prior
        //! returns to non-informative.
        //! \param[in] minimumModeFraction The minimum permitted fraction
        //! of points in a distribution mode.
        //! \param[in] minimumModeCount The minimum permitted count of
        //! points in a distribution mode.
        virtual TMultivariatePriorPtr defaultMultivariatePrior(model_t::EFeature feature,
                                                               double offset,
                                                               double decayRate,
                                                               double minimumModeFraction,
                                                               double minimumModeCount,
                                                               double minimumCategoryCount) const = 0;

        //! Get the default prior for pairs of correlated time series
        //! of \p feature.
        //!
        //! \param[in] feature The feature for which to get the prior.
        //! \param[in] offset The fixed offset to apply to all samples.
        //! \param[in] decayRate Controls the rate at which the prior
        //! returns to non-informative.
        //! \param[in] minimumModeFraction The minimum permitted fraction
        //! of points in a distribution mode.
        //! \param[in] minimumModeCount The minimum permitted count of
        //! points in a distribution mode.
        virtual TMultivariatePriorPtr defaultCorrelatePrior(model_t::EFeature feature,
                                                            double offset,
                                                            double decayRate,
                                                            double minimumModeFraction,
                                                            double minimumModeCount,
                                                            double minimumCategoryCount) const = 0;

        //! Get a multivariate normal prior with dimension \p dimension.
        //!
        //! \param[in] dimension The dimension.
        //! \param[in] decayRate Controls the rate at which the prior
        //! returns to non-informative.
        //! \warning Up to ten dimensions are supported.
        TMultivariatePriorPtr multivariateNormalPrior(std::size_t dimension,
                                                      double decayRate) const;

        //! Get a multivariate multimodal prior with dimension \p dimension.
        //!
        //! \param[in] dimension The dimension.
        //! \param[in] decayRate Controls the rate at which the prior
        //! returns to non-informative.
        //! \param[in] minimumModeFraction The minimum permitted fraction
        //! of points in a distribution mode.
        //! \param[in] minimumModeCount The minimum permitted count of
        //! points in a distribution mode.
        //! \param[in] modePrior The prior used for modeling individual
        //! modes of the distribution.
        //! \warning Up to ten dimensions are supported.
        TMultivariatePriorPtr multivariateMultimodalPrior(std::size_t dimension,
                                                          double decayRate,
                                                          double minimumModeFraction,
                                                          double minimumModeCount,
                                                          double minimumCategoryCount,
                                                          const maths::CMultivariatePrior &modePrior) const;

        //! Get a multivariate 1-of-n prior with dimension \p dimension.
        //!
        //! \param[in] dimension The dimension.
        //! \param[in] decayRate Controls the rate at which the prior
        //! returns to non-informative.
        //! \param[in] models The component models to select between.
        TMultivariatePriorPtr multivariateOneOfNPrior(std::size_t dimension,
                                                      double decayRate,
                                                      const TMultivariatePriorPtrVec &models) const;

        //! Get the default prior for time-of-day and time-of-week modeling.
        //! This is just a mixture of normals which allows more modes than
        //! we typically do.
        //!
        //! \param[in] decayRate Controls the rate at which the prior
        //! returns to non-informative.
        TPriorPtr timeOfDayPrior(double decayRate) const;

        //! Get the default prior for latitude and longitude modeling.
        //! This is just a mixture of correlate normals which allows more
        //! modes than we typically do.
        //!
        //! \param[in] decayRate Controls the rate at which the prior
        //! returns to non-informative.
        TMultivariatePriorPtr latLongPrior(double decayRate) const;

        //! Get the default prior to use for categorical data.
        maths::CMultinomialConjugate defaultCategoricalPrior(void) const;

        //! Get the default time series decomposition.
        //!
        //! \param[in] features The feature for which to get the decomposition.
        //! \param[in] bucketLength The data bucketing length.
        const TFeatureDecompositionCPtrVecPrVec &
            defaultDecompositions(const TFeatureVec &features,
                                  core_t::TTime bucketLength) const;

        //! Get the influence calculators to use for each feature in \p features.
        const TFeatureInfluenceCalculatorCPtrPrVec &
            defaultInfluenceCalculators(const std::string &influencerName,
                                        const TFeatureVec &features) const;
        //@}

        //! Get the search key corresponding to this factory.
        virtual const CSearchKey &searchKey(void) const = 0;

        //! Check if this makes the model used for a simple counting search.
        virtual bool isSimpleCount(void) const = 0;

        //! Check the pre-summarisation mode for this factory.
        virtual model_t::ESummaryMode summaryMode(void) const = 0;

        //! Get the default data type for models from this factory.
        virtual maths_t::EDataType dataType(void) const = 0;

        //! \name Customization by a specific search
        //@{
        //! Set the identifier of the search for which this generates models.
        virtual void identifier(int identifier) = 0;

        //! Set the record field names which will be modeled.
        virtual void fieldNames(const std::string &partitionFieldName,
                                const std::string &overFieldName,
                                const std::string &byFieldName,
                                const std::string &valueFieldName,
                                const TStrVec &influenceFieldNames) = 0;

        //! Set whether the model should process missing field values.
        virtual void useNull(bool useNull) = 0;

        //! Set the features which will be modeled.
        virtual void features(const TFeatureVec &features) = 0;

        //! Set the amount by which metric sample count is reduced for
        //! fine-grained sampling when there is latency.
        void sampleCountFactor(std::size_t sampleCountFactor);

        //! Set the bucket results delay
        virtual void bucketResultsDelay(std::size_t bucketResultsDelay) = 0;

        //! Set whether the model should exclude frequent hitters from the
        //! calculations.
        void excludeFrequent(model_t::EExcludeFrequent excludeFrequent);

        //! Set the functions to convert extra data to and from a state
        //! document.
        void extraDataConversionFuncs(const model_t::TAnyPersistFunc &persistFunc,
                                      const model_t::TAnyRestoreFunc &restoreFunc,
                                      const model_t::TAnyMemoryFunc &memoryFunc);

        //! Set the detection rules for a detector.
        void detectionRules(TDetectionRuleVecCRef detectionRules);
        //@}

        //! Get the function used for converting extra data on restoration.
        const model_t::TAnyRestoreFunc &extraDataRestoreFunc(void) const;

        //! \name Customization by mlmodel.conf
        //@{
        //! Set the learn rate used for initializing models.
        void learnRate(double learnRate);

        //! Set the decay rate used for initializing the models.
        void decayRate(double decayRate);

        //! Set the initial decay rate multiplier used for initializing
        //! models.
        void initialDecayRateMultiplier(double multiplier);

        //! Set the maximum number of times we'll update a person's model
        //! in a bucketing interval.
        void maximumUpdatesPerBucket(double maximumUpdatesPerBucket);

        //! Set the prune window scale factor minimum
        void pruneWindowScaleMinimum(double factor);

        //! Set the prune window scale factor maximum
        void pruneWindowScaleMaximum(double factor);

        //! Set the number of times we sample the people's attribute
        //! distributions to compute raw total probabilities for population
        //! models.
        void totalProbabilityCalcSamplingSize(std::size_t samplingSize);

        //! Set whether multivariate analysis of correlated 'by' fields should
        //! be performed.
        void multivariateByFields(bool enabled);

        //! Set the minimum mode fraction used for initializing the models.
        void minimumModeFraction(double minimumModeFraction);

        //! Set the minimum mode count used for initializing the models.
        void minimumModeCount(double minimumModeCount);

        //! Set the periods and the number of points we'll use to model
        //! of the seasonal components in the data.
        void componentSize(std::size_t componentSize);
        //@}

        //! Update the bucket length, for ModelAutoConfig's benefit
        void updateBucketLength(core_t::TTime length);

        //! Get global model configuration parameters.
        const SModelParams &modelParams(void) const;

        //! Get the minimum mode fraction used for initializing the models.
        double minimumModeFraction(void) const;

        //! Set the minimum mode count used for initializing the models.
        double minimumModeCount(void) const;

        //! Get the number of points to use for approximating each seasonal
        //! component.
        std::size_t componentSize(void) const;

    protected:
        typedef boost::optional<CSearchKey> TOptionalSearchKey;

    protected:
        //! Efficiently swap the contents of this and other.
        //!
        //! \note This only swaps the state held on this base class.
        void swap(CModelFactory &other);

    private:
        typedef std::map<TFeatureVec, TFeaturePriorPtrPrVec> TFeatureVecPriorCPtrMap;
        typedef TFeatureVecPriorCPtrMap::iterator TFeatureVecPriorCPtrMapItr;
        typedef std::map<TFeatureVec, TFeatureMultivariatePriorPtrPrVec> TFeatureVecMultivariatePriorCPtrMap;
        typedef TFeatureVecMultivariatePriorCPtrMap::iterator TFeatureVecMultivariatePriorCPtrMapItr;
        typedef std::map<TFeatureVec, TFeatureDecompositionCPtrVecPrVec> TFeatureVecDecompositionCPtrVecMap;
        typedef std::pair<std::string, TFeatureVec> TStrFeatureVecPr;
        typedef std::map<TStrFeatureVecPr,
                         TFeatureInfluenceCalculatorCPtrPrVec,
                         maths::COrderings::SLess> TStrFeatureVecPrInfluenceCalculatorCPtrMap;

    private:
        //! Get the field values which partition the data for modeling.
        virtual TStrCRefVec partitioningFields(void) const = 0;

    private:
        //! The global model configuration parameters.
        SModelParams m_ModelParams;

        //! A cache of priors for collections of univariate features.
        mutable TFeatureVecPriorCPtrMap m_PriorCache;

        //! A cache of priors for collections of multivariate features.
        mutable TFeatureVecMultivariatePriorCPtrMap m_MultivariatePriorCache;

        //! A cache of priors for collections of correlate features.
        mutable TFeatureVecMultivariatePriorCPtrMap m_CorrelatePriorCache;

        //! A cache of decompositions for collections of features.
        mutable TFeatureVecDecompositionCPtrVecMap m_DecompositionCache;

        //! A cache of influence calculators for collections of features.
        mutable TStrFeatureVecPrInfluenceCalculatorCPtrMap m_InfluenceCalculatorCache;
};

}
}

#endif // INCLUDED_ml_model_CModelFactory_h

