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

#include <model/CModelFactory.h>

#include <core/Constants.h>
#include <core/CStateRestoreTraverser.h>

#include <maths/CMultimodalPrior.h>
#include <maths/CMultinomialConjugate.h>
#include <maths/CMultivariateNormalConjugateFactory.h>
#include <maths/CMultivariateMultimodalPriorFactory.h>
#include <maths/CMultivariateOneOfNPriorFactory.h>
#include <maths/CNormalMeanPrecConjugate.h>
#include <maths/COneOfNPrior.h>
#include <maths/CPrior.h>
#include <maths/CPriorStateSerialiser.h>
#include <maths/CTimeSeriesDecomposition.h>
#include <maths/CTimeSeriesDecompositionStateSerialiser.h>
#include <maths/CTimeSeriesDecompositionStub.h>
#include <maths/CXMeansOnline1d.h>

#include <model/CModel.h>
#include <model/CModelConfig.h>
#include <model/CProbabilityAndInfluenceCalculator.h>

#include <boost/bind.hpp>
#include <boost/make_shared.hpp>

namespace ml
{
namespace model
{

typedef std::vector<core_t::TTime> TTimeVec;
typedef boost::optional<model_t::EFeature> TOptionalFeature;

const std::string CModelFactory::EMPTY_STRING("");

CModelFactory::CModelFactory(const SModelParams &params) :
        m_ModelParams(params)
{
}

CModelFactory::~CModelFactory(void)
{
}

const CModelFactory::TFeaturePriorPtrPrVec &
    CModelFactory::defaultPriors(const TFeatureVec &features) const
{
    std::pair<TFeatureVecPriorCPtrMapItr, bool> result =
            m_PriorCache.insert(std::make_pair(features, TFeaturePriorPtrPrVec()));

    if (result.second)
    {
        result.first->second.reserve(features.size());
        for (std::size_t i = 0u; i < features.size(); ++i)
        {
            model_t::EFeature feature = features[i];
            if (model_t::isCategorical(feature))
            {
                continue;
            }
            if (model_t::dimension(feature) > 1)
            {
                continue;
            }
            TFeaturePriorPtrPr featurePrior(features[i], this->defaultPrior(feature));
            result.first->second.push_back(featurePrior);
        }
    }

    return result.first->second;
}

const CModelFactory::TFeatureMultivariatePriorPtrPrVec &
    CModelFactory::defaultMultivariatePriors(const TFeatureVec &features) const
{
    std::pair<TFeatureVecMultivariatePriorCPtrMapItr, bool> result =
            m_MultivariatePriorCache.insert(std::make_pair(features, TFeatureMultivariatePriorPtrPrVec()));

    if (result.second)
    {
        result.first->second.reserve(features.size());
        for (std::size_t i = 0u; i < features.size(); ++i)
        {
            model_t::EFeature feature = features[i];
            if (model_t::isCategorical(feature))
            {
                continue;
            }
            if (model_t::dimension(feature) == 1)
            {
                continue;
            }
            TFeatureMultivariatePriorPtrPr featurePrior(features[i], this->defaultMultivariatePrior(feature));
            result.first->second.push_back(featurePrior);
        }
    }

    return result.first->second;
}

const CModelFactory::TFeatureMultivariatePriorPtrPrVec &
    CModelFactory::defaultCorrelatePriors(const TFeatureVec &features) const
{
    std::pair<TFeatureVecMultivariatePriorCPtrMapItr, bool> result =
            m_CorrelatePriorCache.insert(std::make_pair(features, TFeatureMultivariatePriorPtrPrVec()));

    if (result.second)
    {
        result.first->second.reserve(features.size());
        for (std::size_t i = 0u; i < features.size(); ++i)
        {
            model_t::EFeature feature = features[i];
            if (model_t::isCategorical(feature))
            {
                continue;
            }
            if (model_t::dimension(feature) > 1)
            {
                continue;
            }
            TFeatureMultivariatePriorPtrPr featurePrior(features[i], this->defaultCorrelatePrior(feature));
            result.first->second.push_back(featurePrior);
        }
    }

    return result.first->second;
}

CModelFactory::TPriorPtr
    CModelFactory::defaultPrior(model_t::EFeature feature) const
{
    return this->defaultPrior(feature,
                              CModelConfig::DEFAULT_PRIOR_OFFSET,
                              m_ModelParams.s_DecayRate,
                              m_ModelParams.s_MinimumModeFraction,
                              m_ModelParams.s_MinimumModeCount,
                              CModelConfig::DEFAULT_CATEGORY_DELETE_FRACTION * m_ModelParams.s_LearnRate);
}

CModelFactory::TMultivariatePriorPtr
    CModelFactory::defaultMultivariatePrior(model_t::EFeature feature) const
{
    return this->defaultMultivariatePrior(feature,
                                          CModelConfig::DEFAULT_PRIOR_OFFSET,
                                          m_ModelParams.s_DecayRate,
                                          m_ModelParams.s_MinimumModeFraction,
                                          m_ModelParams.s_MinimumModeCount,
                                          CModelConfig::DEFAULT_CATEGORY_DELETE_FRACTION * m_ModelParams.s_LearnRate);
}

CModelFactory::TMultivariatePriorPtr
    CModelFactory::defaultCorrelatePrior(model_t::EFeature feature) const
{
    return this->defaultCorrelatePrior(feature,
                                       CModelConfig::DEFAULT_PRIOR_OFFSET,
                                       m_ModelParams.s_DecayRate,
                                       m_ModelParams.s_MinimumModeFraction,
                                       m_ModelParams.s_MinimumModeCount,
                                       CModelConfig::DEFAULT_CATEGORY_DELETE_FRACTION * m_ModelParams.s_LearnRate);
}

CModelFactory::TMultivariatePriorPtr
    CModelFactory::multivariateNormalPrior(std::size_t dimension, double decayRate) const
{
    return maths::CMultivariateNormalConjugateFactory::nonInformative(dimension, this->dataType(), decayRate);
}

CModelFactory::TMultivariatePriorPtr
    CModelFactory::multivariateMultimodalPrior(std::size_t dimension,
                                               double decayRate,
                                               double minimumModeFraction,
                                               double minimumModeCount,
                                               double minimumCategoryCount,
                                               const maths::CMultivariatePrior &modePrior) const
{
    return maths::CMultivariateMultimodalPriorFactory::nonInformative(dimension,
                                                                      this->dataType(),
                                                                      decayRate,
                                                                      maths_t::E_ClustersFractionWeight,
                                                                      minimumModeFraction,
                                                                      minimumModeCount,
                                                                      minimumCategoryCount,
                                                                      modePrior);
}

CModelFactory::TMultivariatePriorPtr
    CModelFactory::multivariateOneOfNPrior(std::size_t dimension,
                                           double decayRate,
                                           const TMultivariatePriorPtrVec &models) const
{
    return maths::CMultivariateOneOfNPriorFactory::nonInformative(dimension,
                                                                  this->dataType(),
                                                                  decayRate,
                                                                  models);
}

CModelFactory::TPriorPtr CModelFactory::timeOfDayPrior(double decayRate) const
{
    maths_t::EDataType dataType = this->dataType();

    maths::CNormalMeanPrecConjugate normalPrior =
            maths::CNormalMeanPrecConjugate::nonInformativePrior(dataType, decayRate);

    // Create a multimodal prior with purely normal distributions
    // - don't bother with long-tail distributions

    TPriorPtrVec modePriors;
    modePriors.reserve(1u);
    modePriors.push_back(TPriorPtr(normalPrior.clone()));
    maths::COneOfNPrior modePrior(modePriors, dataType, decayRate);
    maths::CXMeansOnline1d clusterer(dataType,
                                     maths::CAvailableModeDistributions::NORMAL,
                                     maths_t::E_ClustersFractionWeight,
                                     decayRate,
                                     0.03, // minimumClusterFraction
                                     4,    // minimumClusterCount
                                     CModelConfig::DEFAULT_CATEGORY_DELETE_FRACTION);

    return boost::make_shared<maths::CMultimodalPrior>(dataType, clusterer, modePrior, decayRate);
}

CModelFactory::TMultivariatePriorPtr CModelFactory::latLongPrior(double decayRate) const
{
    maths_t::EDataType dataType = this->dataType();
    TMultivariatePriorPtr modePrior =
            maths::CMultivariateNormalConjugateFactory::nonInformative(2, dataType, decayRate);
    return maths::CMultivariateMultimodalPriorFactory::nonInformative(
                      2,    // dimension
                      dataType, decayRate,
                      maths_t::E_ClustersFractionWeight,
                      0.03, // minimumClusterFraction
                      4,    // minimumClusterCount
                      CModelConfig::DEFAULT_CATEGORY_DELETE_FRACTION,
                      *modePrior);
}

maths::CMultinomialConjugate CModelFactory::defaultCategoricalPrior(void) const
{
    return maths::CMultinomialConjugate::nonInformativePrior(std::numeric_limits<std::size_t>::max(),
                                                             m_ModelParams.s_DecayRate);
}

const CModelFactory::TFeatureDecompositionCPtrVecPrVec &
    CModelFactory::defaultDecompositions(const TFeatureVec &features,
                                         core_t::TTime bucketLength) const
{
    TFeatureDecompositionCPtrVecPrVec &result = m_DecompositionCache[features];

    if (result.empty())
    {
        result.reserve(features.size());
        for (std::size_t i = 0u; i < features.size(); ++i)
        {
            model_t::EFeature feature = features[i];
            if (model_t::isCategorical(feature))
            {
                continue;
            }
            else
            {
                result.push_back(TFeatureDecompositionCPtrVecPr(feature, TDecompositionCPtrVec()));
                TDecompositionCPtrVec &trend = result.back().second;

                std::size_t dimension = model_t::dimension(feature);
                trend.reserve(dimension);

                double decayRate = CModelConfig::trendDecayRate(m_ModelParams.s_DecayRate, bucketLength);

                for (std::size_t j = 0u; j < dimension; ++j)
                {
                    if (model_t::isDiurnal(feature) || model_t::isConstant(feature))
                    {
                        trend.push_back(boost::make_shared<maths::CTimeSeriesDecompositionStub>());
                    }
                    else
                    {
                        trend.push_back(boost::make_shared<maths::CTimeSeriesDecomposition>(decayRate,
                                                                                            bucketLength,
                                                                                            m_ModelParams.s_ComponentSize));
                    }
                }
            }
        }
    }

    return result;
}

const CModelFactory::TFeatureInfluenceCalculatorCPtrPrVec &
    CModelFactory::defaultInfluenceCalculators(const std::string &influencerName,
                                               const TFeatureVec &features) const
{
    TFeatureInfluenceCalculatorCPtrPrVec &result =
            m_InfluenceCalculatorCache[TStrFeatureVecPr(influencerName, features)];

    if (result.empty())
    {
        result.reserve(features.size());

        TStrCRefVec partitioningFields = this->partitioningFields();
        std::sort(partitioningFields.begin(),
                  partitioningFields.end(),
                  maths::COrderings::SReferenceLess());

        for (std::size_t i = 0u; i < features.size(); ++i)
        {
            model_t::EFeature feature = features[i];
            if (model_t::isCategorical(feature))
            {
                continue;
            }
            if (std::binary_search(partitioningFields.begin(),
                                   partitioningFields.end(),
                                   influencerName,
                                   maths::COrderings::SReferenceLess()))
            {
                result.push_back(TFeatureInfluenceCalculatorCPtrPr(
                                     feature,
                                     boost::make_shared<CIndicatorInfluenceCalculator>()));
            }
            else
            {
                result.push_back(TFeatureInfluenceCalculatorCPtrPr(
                                     feature,
                                     model_t::influenceCalculator(features[i])));
            }
        }
    }

    return result;
}

void CModelFactory::sampleCountFactor(std::size_t sampleCountFactor)
{
    m_ModelParams.s_SampleCountFactor = sampleCountFactor;
}

void CModelFactory::excludeFrequent(model_t::EExcludeFrequent excludeFrequent)
{
    m_ModelParams.s_ExcludeFrequent = excludeFrequent;
}

void CModelFactory::extraDataConversionFuncs(const model_t::TAnyPersistFunc &persistFunc,
                                             const model_t::TAnyRestoreFunc &restoreFunc,
                                             const model_t::TAnyMemoryFunc &memoryFunc)
{
    m_ModelParams.s_ExtraDataPersistFunc = persistFunc;
    m_ModelParams.s_ExtraDataRestoreFunc = restoreFunc;
    m_ModelParams.s_ExtraDataMemoryFunc = memoryFunc;
}

void CModelFactory::detectionRules(TDetectionRuleVecCRef detectionRules)
{
    m_ModelParams.s_DetectionRules = detectionRules;
}

const model_t::TAnyRestoreFunc &CModelFactory::extraDataRestoreFunc(void) const
{
    return m_ModelParams.s_ExtraDataRestoreFunc;
}

void CModelFactory::learnRate(double learnRate)
{
    m_ModelParams.s_LearnRate = learnRate;
}

void CModelFactory::decayRate(double decayRate)
{
    m_ModelParams.s_DecayRate = decayRate;
}

void CModelFactory::initialDecayRateMultiplier(double multiplier)
{
    m_ModelParams.s_InitialDecayRateMultiplier = multiplier;
}

void CModelFactory::maximumUpdatesPerBucket(double maximumUpdatesPerBucket)
{
    m_ModelParams.s_MaximumUpdatesPerBucket = maximumUpdatesPerBucket;
}

void CModelFactory::pruneWindowScaleMinimum(double factor)
{
    m_ModelParams.s_PruneWindowScaleMinimum = factor;
}

void CModelFactory::pruneWindowScaleMaximum(double factor)
{
    m_ModelParams.s_PruneWindowScaleMaximum = factor;
}

void CModelFactory::totalProbabilityCalcSamplingSize(std::size_t samplingSize)
{
    m_ModelParams.s_TotalProbabilityCalcSamplingSize = samplingSize;
}

void CModelFactory::multivariateByFields(bool enabled)
{
    m_ModelParams.s_MultivariateByFields = enabled;
}

void CModelFactory::minimumModeFraction(double minimumModeFraction)
{
    m_ModelParams.s_MinimumModeFraction = minimumModeFraction;
}

void CModelFactory::minimumModeCount(double minimumModeCount)
{
    m_ModelParams.s_MinimumModeCount = minimumModeCount;
}

void CModelFactory::componentSize(std::size_t componentSize)
{
    m_ModelParams.s_ComponentSize = componentSize;
}

double CModelFactory::minimumModeFraction(void) const
{
    return m_ModelParams.s_MinimumModeFraction;
}

double CModelFactory::minimumModeCount(void) const
{
    return m_ModelParams.s_MinimumModeCount;
}

std::size_t CModelFactory::componentSize(void) const
{
    return m_ModelParams.s_ComponentSize;
}

void CModelFactory::updateBucketLength(core_t::TTime length)
{
    m_ModelParams.s_BucketLength = length;
}

void CModelFactory::swap(CModelFactory &other)
{
    std::swap(m_ModelParams, other.m_ModelParams);
    m_PriorCache.swap(other.m_PriorCache);
    m_MultivariatePriorCache.swap(other.m_MultivariatePriorCache);
    m_CorrelatePriorCache.swap(other.m_CorrelatePriorCache);
    m_DecompositionCache.swap(other.m_DecompositionCache);
    m_InfluenceCalculatorCache.swap(other.m_InfluenceCalculatorCache);
}

const SModelParams &CModelFactory::modelParams(void) const
{
    return m_ModelParams;
}

CModelFactory::SModelInitializationData::SModelInitializationData(const TDataGathererPtr &dataGatherer,
                                                                  const TModelCPtr &referenceModel,
                                                                  const TFeaturePriorPtrPrVec &priors,
                                                                  const TFeatureMultivariatePriorPtrPrVec &multivariatePriors,
                                                                  const TFeatureMultivariatePriorPtrPrVec &correlatePriors) :
        s_DataGatherer(dataGatherer),
        s_ReferenceModel(referenceModel),
        s_Priors(priors),
        s_MultivariatePriors(multivariatePriors),
        s_CorrelatePriors(correlatePriors)
{
}

CModelFactory::SGathererInitializationData::SGathererInitializationData(const core_t::TTime &startTime,
                                                                        unsigned int sampleOverrideCount) :
        s_StartTime(startTime),
        s_SampleOverrideCount(sampleOverrideCount)
{
}

}
}
