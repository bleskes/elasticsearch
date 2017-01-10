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

#include <model/CMetricModelFactory.h>

#include <core/CStateRestoreTraverser.h>

#include <maths/CConstantPrior.h>
#include <maths/CGammaRateConjugate.h>
#include <maths/CLogNormalMeanPrecConjugate.h>
#include <maths/CMultimodalPrior.h>
#include <maths/CNormalMeanPrecConjugate.h>
#include <maths/COneOfNPrior.h>
#include <maths/CPrior.h>
#include <maths/CXMeansOnline1d.h>

#include <model/CDataGatherer.h>
#include <model/CMetricModel.h>
#include <model/CModelConfig.h>

#include <boost/make_shared.hpp>

namespace ml
{
namespace model
{

CMetricModelFactory::CMetricModelFactory(const SModelParams &params,
                                         model_t::ESummaryMode summaryMode,
                                         const std::string &summaryCountFieldName) :
        CModelFactory(params),
        m_Identifier(),
        m_SummaryMode(summaryMode),
        m_SummaryCountFieldName(summaryCountFieldName),
        m_UseNull(false),
        m_BucketLength(CModelConfig::DEFAULT_BUCKET_LENGTH),
        m_BucketResultsDelay(0)
{
}

CMetricModelFactory *CMetricModelFactory::clone(void) const
{
    return new CMetricModelFactory(*this);
}

CModel *CMetricModelFactory::makeModel(const SModelInitializationData &initData) const
{
    TDataGathererPtr dataGatherer = initData.s_DataGatherer;
    if (!dataGatherer)
    {
        LOG_ERROR("NULL data gatherer");
        return 0;
    }
    const TFeatureVec &features = dataGatherer->features();

    TFeatureInfluenceCalculatorCPtrPrVecVec influenceCalculators;
    influenceCalculators.reserve(m_InfluenceFieldNames.size());
    for (std::size_t i = 0u; i < m_InfluenceFieldNames.size(); ++i)
    {
        influenceCalculators.push_back(this->defaultInfluenceCalculators(m_InfluenceFieldNames[i],
                                                                         features));
    }

    return new CMetricModel(this->modelParams(),
                            dataGatherer,
                            !initData.s_Priors.empty() ?
                                initData.s_Priors : this->defaultPriors(features),
                            !initData.s_MultivariatePriors.empty() ?
                                initData.s_MultivariatePriors : this->defaultMultivariatePriors(features),
                            !initData.s_CorrelatePriors.empty() ?
                                initData.s_CorrelatePriors : this->defaultCorrelatePriors(features),
                            this->defaultDecompositions(features, initData.s_DataGatherer->bucketLength()),
                            influenceCalculators);
}

CModel *CMetricModelFactory::makeModel(const SModelInitializationData &initData,
                                       core::CStateRestoreTraverser &traverser) const
{
    TDataGathererPtr dataGatherer = initData.s_DataGatherer;
    if (!dataGatherer)
    {
        LOG_ERROR("NULL data gatherer");
        return 0;
    }
    const TFeatureVec &features = dataGatherer->features();

    TFeatureInfluenceCalculatorCPtrPrVecVec influenceCalculators;
    influenceCalculators.reserve(m_InfluenceFieldNames.size());
    for (std::size_t i = 0u; i < m_InfluenceFieldNames.size(); ++i)
    {
        influenceCalculators.push_back(this->defaultInfluenceCalculators(m_InfluenceFieldNames[i],
                                                                         features));
    }

    return new CMetricModel(this->modelParams(),
                            dataGatherer,
                            !initData.s_Priors.empty() ?
                                initData.s_Priors : this->defaultPriors(features),
                            !initData.s_MultivariatePriors.empty() ?
                                initData.s_MultivariatePriors : this->defaultMultivariatePriors(features),
                            !initData.s_CorrelatePriors.empty() ?
                                initData.s_CorrelatePriors : this->defaultCorrelatePriors(features),
                            this->defaultDecompositions(features, initData.s_DataGatherer->bucketLength()),
                            influenceCalculators,
                            traverser);
}

CDataGatherer *CMetricModelFactory::makeDataGatherer(const SGathererInitializationData &initData) const
{
    return new CDataGatherer(model_t::E_Metric,
                             m_SummaryMode,
                             this->modelParams(),
                             m_SummaryCountFieldName,
                             m_PartitionFieldName,
                             m_PersonFieldName,
                             EMPTY_STRING,
                             m_ValueFieldName,
                             m_InfluenceFieldNames,
                             m_UseNull,
                             this->searchKey(),
                             m_Features,
                             initData.s_StartTime,
                             initData.s_SampleOverrideCount);
}

CDataGatherer *CMetricModelFactory::makeDataGatherer(core::CStateRestoreTraverser &traverser) const
{
    return new CDataGatherer(model_t::E_Metric,
                             m_SummaryMode,
                             this->modelParams(),
                             m_SummaryCountFieldName,
                             m_PartitionFieldName,
                             m_PersonFieldName,
                             EMPTY_STRING,
                             m_ValueFieldName,
                             m_InfluenceFieldNames,
                             m_UseNull,
                             this->searchKey(),
                             traverser);
}

CMetricModelFactory::TPriorPtr
    CMetricModelFactory::defaultPrior(model_t::EFeature feature,
                                      double offset,
                                      double decayRate,
                                      double minimumModeFraction,
                                      double minimumModeCount,
                                      double minimumCategoryCount) const
{
    // Categorical data all use the multinomial prior. The creation
    // of these priors is managed by defaultCategoricalPrior.
    if (model_t::isCategorical(feature))
    {
        return TPriorPtr();
    }

    // If the feature data only ever takes a single value we use a
    // special lightweight prior.
    if (model_t::isConstant(feature))
    {
        return boost::make_shared<maths::CConstantPrior>();
    }

    // The data will be arbitrary metric values. Metrics with negative values
    // will be handled by adjusting offsets in the gamma and log-normal priors
    // on the fly. We start off with a small non-zero offset for the log-normal
    // prior because the p.d.f. is zero at zero and for the gamma because the
    // p.d.f. is badly behaved at zero (either zero or infinity), and
    //   1) we expect that zero values are relatively common, and
    //   2) we don't want to adjust the offset if we can avoid this since it
    //      is expensive and results in some loss of information.

    maths_t::EDataType dataType = this->dataType();

    maths::CGammaRateConjugate gammaPrior =
            maths::CGammaRateConjugate::nonInformativePrior(dataType, offset, decayRate);

    maths::CNormalMeanPrecConjugate normalPrior =
            maths::CNormalMeanPrecConjugate::nonInformativePrior(dataType, decayRate);

    maths::CLogNormalMeanPrecConjugate logNormalPrior =
            maths::CLogNormalMeanPrecConjugate::nonInformativePrior(dataType, offset, decayRate);

    // Create the component priors.
    TPriorPtrVec priors;
    priors.reserve(minimumModeFraction <= 0.5 ? 4u : 3u);
    priors.push_back(TPriorPtr(gammaPrior.clone()));
    priors.push_back(TPriorPtr(logNormalPrior.clone()));
    priors.push_back(TPriorPtr(normalPrior.clone()));
    if (minimumModeFraction <= 0.5)
    {
        // Create the multimode prior.
        TPriorPtrVec modePriors;
        modePriors.reserve(3u);
        modePriors.push_back(TPriorPtr(gammaPrior.clone()));
        modePriors.push_back(TPriorPtr(logNormalPrior.clone()));
        modePriors.push_back(TPriorPtr(normalPrior.clone()));
        maths::COneOfNPrior modePrior(modePriors, dataType, decayRate);
        maths::CXMeansOnline1d clusterer(dataType,
                                         maths::CAvailableModeDistributions::ALL,
                                         maths_t::E_ClustersFractionWeight,
                                         decayRate,
                                         minimumModeFraction,
                                         minimumModeCount,
                                         minimumCategoryCount);
        maths::CMultimodalPrior multimodalPrior(dataType, clusterer, modePrior, decayRate);
        priors.push_back(TPriorPtr(multimodalPrior.clone()));
    }

    return boost::make_shared<maths::COneOfNPrior>(priors, dataType, decayRate);
}

CMetricModelFactory::TMultivariatePriorPtr
    CMetricModelFactory::defaultMultivariatePrior(model_t::EFeature feature,
                                                  double /*offset*/,
                                                  double decayRate,
                                                  double minimumModeFraction,
                                                  double minimumModeCount,
                                                  double minimumCategoryCount) const
{
    std::size_t dimension = model_t::dimension(feature);

    // Gaussian mixture for modeling (latitude, longitude).
    if (model_t::isLatLong(feature))
    {
        return this->latLongPrior(decayRate);
    }

    TMultivariatePriorPtrVec priors;
    priors.reserve(minimumModeFraction <= 0.5 ? 2u : 1u);
    TMultivariatePriorPtr multivariateNormal = this->multivariateNormalPrior(dimension, decayRate);
    priors.push_back(multivariateNormal);
    if (minimumModeFraction <= 0.5)
    {
        priors.push_back(this->multivariateMultimodalPrior(dimension,
                                                           decayRate,
                                                           minimumModeFraction,
                                                           minimumModeCount,
                                                           minimumCategoryCount,
                                                           *multivariateNormal));
    }

    return this->multivariateOneOfNPrior(dimension, decayRate, priors);
}

CMetricModelFactory::TMultivariatePriorPtr
    CMetricModelFactory::defaultCorrelatePrior(model_t::EFeature /*feature*/,
                                               double /*offset*/,
                                               double decayRate,
                                               double minimumModeFraction,
                                               double minimumModeCount,
                                               double minimumCategoryCount) const
{
    TMultivariatePriorPtrVec priors;
    priors.reserve(minimumModeFraction <= 0.5 ? 2u : 1u);
    TMultivariatePriorPtr multivariateNormal = this->multivariateNormalPrior(2, decayRate);
    priors.push_back(multivariateNormal);
    if (minimumModeFraction <= 0.5)
    {
        priors.push_back(this->multivariateMultimodalPrior(2, // dimension
                                                           decayRate,
                                                           minimumModeFraction,
                                                           minimumModeCount,
                                                           minimumCategoryCount,
                                                           *multivariateNormal));
    }
    return this->multivariateOneOfNPrior(2, decayRate, priors);
}

const CSearchKey &CMetricModelFactory::searchKey(void) const
{
    if (!m_SearchKeyCache)
    {
        m_SearchKeyCache.reset(CSearchKey(m_Identifier,
                                          function_t::function(m_Features),
                                          m_UseNull,
                                          this->modelParams().s_ExcludeFrequent,
                                          m_ValueFieldName,
                                          m_PersonFieldName,
                                          "",
                                          m_PartitionFieldName,
                                          m_InfluenceFieldNames));
    }
    return *m_SearchKeyCache;
}

bool CMetricModelFactory::isSimpleCount(void) const
{
    return false;
}

model_t::ESummaryMode CMetricModelFactory::summaryMode(void) const
{
    return m_SummaryMode;
}

maths_t::EDataType CMetricModelFactory::dataType(void) const
{
    return maths_t::E_ContinuousData;
}

void CMetricModelFactory::identifier(int identifier)
{
    m_Identifier = identifier;
    m_SearchKeyCache.reset();
}

void CMetricModelFactory::fieldNames(const std::string &partitionFieldName,
                                     const std::string &/*overFieldName*/,
                                     const std::string &byFieldName,
                                     const std::string &valueFieldName,
                                     const TStrVec &influenceFieldNames)
{
    m_PartitionFieldName = partitionFieldName;
    m_PersonFieldName = byFieldName;
    m_ValueFieldName = valueFieldName;
    m_InfluenceFieldNames = influenceFieldNames;
    m_SearchKeyCache.reset();
}

void CMetricModelFactory::useNull(bool useNull)
{
    m_UseNull = useNull;
    m_SearchKeyCache.reset();
}

void CMetricModelFactory::features(const TFeatureVec &features)
{
    m_Features = features;
    m_SearchKeyCache.reset();
}

void CMetricModelFactory::bucketLength(core_t::TTime bucketLength)
{
    m_BucketLength = bucketLength;
}

void CMetricModelFactory::bucketResultsDelay(std::size_t bucketResultsDelay)
{
    m_BucketResultsDelay = bucketResultsDelay;
}

CMetricModelFactory::TStrCRefVec CMetricModelFactory::partitioningFields(void) const
{
    TStrCRefVec result;
    result.reserve(2);
    if (!m_PartitionFieldName.empty())
    {
        result.push_back(TStrCRef(m_PartitionFieldName));
    }
    if (!m_PersonFieldName.empty())
    {
        result.push_back(TStrCRef(m_PersonFieldName));
    }
    return result;
}

}
}
