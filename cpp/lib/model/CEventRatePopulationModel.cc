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

#include <model/CEventRatePopulationModel.h>

#include <core/CAllocationStrategy.h>
#include <core/CContainerPrinter.h>
#include <core/CLogger.h>
#include <core/CStatePersistInserter.h>
#include <core/CStatistics.h>

#include <maths/CBasicStatistics.h>
#include <maths/CCategoricalTools.h>
#include <maths/CChecksum.h>
#include <maths/CDistributionRestoreParams.h>
#include <maths/COrderings.h>
#include <maths/CTimeSeriesDecomposition.h>
#include <maths/CTimeSeriesDecompositionStateSerialiser.h>
#include <maths/CTools.h>
#include <maths/ProbabilityAggregators.h>

#include <model/CAnnotatedProbabilityBuilder.h>
#include <model/CEventRateBucketGatherer.h>
#include <model/CInterimBucketCorrector.h>
#include <model/CModelDetailsView.h>
#include <model/CModelTools.h>
#include <model/CPopulationModelDetail.h>
#include <model/CProbabilityAndInfluenceCalculator.h>
#include <model/FrequencyPredicates.h>

#include <boost/bind.hpp>

#include <algorithm>

namespace prelert
{
namespace model
{

namespace
{

typedef std::pair<std::size_t, std::size_t> TSizeSizePr;
typedef CEventRatePopulationModel::TSizeSizePrFeatureDataPrVec TSizeSizePrFeatureDataPrVec;
typedef TSizeSizePrFeatureDataPrVec::const_iterator TSizeSizePrFeatureDataPrVecCItr;
typedef std::pair<model_t::EFeature, TSizeSizePrFeatureDataPrVec> TFeatureSizeSizePrFeatureDataPrVecPr;
typedef std::vector<TFeatureSizeSizePrFeatureDataPrVecPr> TFeatureSizeSizePrFeatureDataPrVecPrVec;
typedef TFeatureSizeSizePrFeatureDataPrVecPrVec::const_iterator TFeatureSizeSizePrFeatureDataPrVecPrVecCItr;
typedef SAttributeProbability::TDescriptiveDataDoublePr TDescriptiveDataDoublePr;

const maths_t::TWeightStyleVec COUNT_WEIGHT(1, maths_t::E_SampleCountWeight);

// We obfuscate the element names to avoid giving away too much
// information about our model.
const std::string POPULATION_STATE_TAG("a");
const std::string NEW_ATTRIBUTE_PROBABILITY_PRIOR_TAG("b");
const std::string ATTRIBUTE_PROBABILITY_PRIOR_TAG("c");
const std::string POPULATION_FEATURE_PRIOR_TAG("d");
const std::string POPULATION_FEATURE_MULTIVARIATE_PRIOR_TAG("e");
const std::string MEMORY_ESTIMATOR_TAG("f");
// Nested tags
const std::string FEATURE_TAG("a");
const std::string PRIOR_TAG("b");
const std::string DECOMPOSITION_TAG("c");

const std::string EMPTY_STRING("");

const maths_t::TWeightStyleVec SEASONAL_VARIANCE_WEIGHT(1, maths_t::E_SampleSeasonalVarianceScaleWeight);

}

CEventRatePopulationModel::CEventRatePopulationModel(const SModelParams &params,
                                                     const TDataGathererPtr &dataGatherer,
                                                     const TFeaturePriorPtrPrVec &newPriors,
                                                     const TFeatureMultivariatePriorPtrPrVec &newMultivariatePriors,
                                                     const TFeatureDecompositionCPtrVecPrVec &newDecompositions,
                                                     const TFeatureInfluenceCalculatorCPtrPrVecVec &influenceCalculators) :
        CPopulationModel(params,
                         dataGatherer,
                         newPriors,
                         newMultivariatePriors,
                         newDecompositions,
                         influenceCalculators,
                         false),
        m_CurrentBucketStats(  dataGatherer->currentBucketStartTime()
                             - dataGatherer->bucketLength()),
        m_NewAttributeProbabilityPrior(
                maths::CMultinomialConjugate::nonInformativePrior(
                        std::numeric_limits<int>::max(), params.s_DecayRate)),
        m_AttributeProbabilityPrior(
                maths::CMultinomialConjugate::nonInformativePrior(
                        std::numeric_limits<int>::max(), params.s_DecayRate))
{
    for (std::size_t i = 0u; i < this->newPriors().size(); ++i)
    {
        model_t::EFeature feature = this->newPriors()[i].first;
        m_AttributePriors.insert(std::make_pair(feature, TPriorPtrVec()));
    }
    for (std::size_t i = 0u; i < this->newMultivariatePriors().size(); ++i)
    {
        model_t::EFeature feature = this->newMultivariatePriors()[i].first;
        m_AttributeMultivariatePriors.insert(std::make_pair(feature, TMultivariatePriorPtrVec()));
    }
}

CEventRatePopulationModel::CEventRatePopulationModel(const SModelParams &params,
                                                     const TDataGathererPtr &dataGatherer,
                                                     const TFeaturePriorPtrPrVec &newPriors,
                                                     const TFeatureMultivariatePriorPtrPrVec &newMultivariatePriors,
                                                     const TFeatureDecompositionCPtrVecPrVec &newDecompositions,
                                                     const TFeatureInfluenceCalculatorCPtrPrVecVec &influenceCalculators,
                                                     core::CStateRestoreTraverser &traverser) :
        CPopulationModel(params,
                         dataGatherer,
                         newPriors,
                         newMultivariatePriors,
                         newDecompositions,
                         influenceCalculators,
                         true),
        m_CurrentBucketStats(  dataGatherer->currentBucketStartTime()
                             - dataGatherer->bucketLength())
{
    traverser.traverseSubLevel(boost::bind(&CEventRatePopulationModel::acceptRestoreTraverser,
                                           this,
                                           boost::ref(params.s_ExtraDataRestoreFunc),
                                           _1));
}

CEventRatePopulationModel::CEventRatePopulationModel(bool isForPersistence,
                                                     const CEventRatePopulationModel &other) :
        CPopulationModel(isForPersistence, other),
        m_CurrentBucketStats(0), // Not needed for persistence so minimally constructed
        m_NewAttributeProbabilityPrior(other.m_NewAttributeProbabilityPrior),
        m_AttributeProbabilityPrior(other.m_AttributeProbabilityPrior),
        m_MemoryEstimator(other.m_MemoryEstimator)
{
    if (!isForPersistence)
    {
        LOG_ABORT("This constructor only creates clones for persistence");
    }
    CModelTools::clonePriors(other.m_AttributePriors, m_AttributePriors);
    CModelTools::clonePriors(other.m_AttributeMultivariatePriors, m_AttributeMultivariatePriors);
}

void CEventRatePopulationModel::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertLevel(POPULATION_STATE_TAG,
                         boost::bind(&CEventRatePopulationModel::doAcceptPersistInserter, this, _1));
    inserter.insertLevel(NEW_ATTRIBUTE_PROBABILITY_PRIOR_TAG,
                         boost::bind(&maths::CMultinomialConjugate::acceptPersistInserter,
                                     &m_NewAttributeProbabilityPrior, _1));
    inserter.insertLevel(ATTRIBUTE_PROBABILITY_PRIOR_TAG,
                         boost::bind(&maths::CMultinomialConjugate::acceptPersistInserter,
                                     &m_AttributeProbabilityPrior, _1));
    for (TFeaturePriorPtrVecMapCItr i = m_AttributePriors.begin();
         i != m_AttributePriors.end();
         ++i)
    {
        inserter.insertLevel(POPULATION_FEATURE_PRIOR_TAG,
                             boost::bind(&featurePriorsAcceptPersistInserter,
                                         boost::cref(FEATURE_TAG),
                                         i->first,
                                         boost::cref(PRIOR_TAG),
                                         boost::cref(i->second), _1));
    }
    for (TFeatureMultivariatePriorPtrVecMapCItr i = m_AttributeMultivariatePriors.begin();
         i != m_AttributeMultivariatePriors.end();
         ++i)
    {
        inserter.insertLevel(POPULATION_FEATURE_MULTIVARIATE_PRIOR_TAG,
                             boost::bind(&featureMultivariatePriorsAcceptPersistInserter,
                                         boost::cref(FEATURE_TAG),
                                         i->first,
                                         boost::cref(PRIOR_TAG),
                                         boost::cref(i->second), _1));
    }
    core::CPersistUtils::persist(MEMORY_ESTIMATOR_TAG, m_MemoryEstimator, inserter);
}

bool CEventRatePopulationModel::acceptRestoreTraverser(const model_t::TAnyRestoreFunc &extraDataRestoreFunc,
                                                       core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        if (name == POPULATION_STATE_TAG)
        {
            if (traverser.traverseSubLevel(boost::bind(&CEventRatePopulationModel::doAcceptRestoreTraverser,
                                                       this, boost::cref(extraDataRestoreFunc), _1)) == false)
            {
                // Logging handled already.
                return false;
            }
        }
        else if (name == NEW_ATTRIBUTE_PROBABILITY_PRIOR_TAG)
        {
            maths::CMultinomialConjugate restored(
                    this->params().distributionRestoreParams(maths_t::E_DiscreteData), traverser);
            m_NewAttributeProbabilityPrior.swap(restored);
        }
        else if (name == ATTRIBUTE_PROBABILITY_PRIOR_TAG)
        {
            maths::CMultinomialConjugate restored(
                    this->params().distributionRestoreParams(maths_t::E_DiscreteData), traverser);
            m_AttributeProbabilityPrior.swap(restored);
        }
        else if (name == POPULATION_FEATURE_PRIOR_TAG)
        {
            if (traverser.traverseSubLevel(boost::bind(&CEventRatePopulationModel::featurePriorsAcceptRestoreTraverser,
                                                       this,
                                                       maths_t::E_IntegerData,
                                                       boost::cref(FEATURE_TAG),
                                                       boost::cref(PRIOR_TAG),
                                                       _1, boost::ref(m_AttributePriors))) == false)
            {
                return false;
            }
        }
        else if (name == POPULATION_FEATURE_MULTIVARIATE_PRIOR_TAG)
        {
            if (traverser.traverseSubLevel(boost::bind(&CEventRatePopulationModel::featureMultivariatePriorsAcceptRestoreTraverser,
                                                       this,
                                                       maths_t::E_IntegerData,
                                                       boost::cref(FEATURE_TAG),
                                                       boost::cref(PRIOR_TAG),
                                                       _1, boost::ref(m_AttributeMultivariatePriors))) == false)
            {
                return false;
            }
        }
        else if (name == MEMORY_ESTIMATOR_TAG)
        {
            if (core::CPersistUtils::restore(MEMORY_ESTIMATOR_TAG, m_MemoryEstimator, traverser) == false)
            {
                return false;
            }
        }
    }
    while (traverser.next());

    return true;
}

CModel *CEventRatePopulationModel::cloneForPersistence(void) const
{
    return new CEventRatePopulationModel(true, *this);
}

model_t::EModelType CEventRatePopulationModel::category(void) const
{
    return model_t::E_EventRateOnline;
}

bool CEventRatePopulationModel::isEventRate(void) const
{
    return true;
}

bool CEventRatePopulationModel::isMetric(void) const
{
    return false;
}

CEventRatePopulationModel::TDouble1Vec
    CEventRatePopulationModel::currentBucketValue(model_t::EFeature feature,
                                                  std::size_t pid,
                                                  std::size_t cid,
                                                  core_t::TTime time) const
{
    return this->currentBucketValue(this->featureData(feature, time),
                                    feature, pid, cid, TDouble1Vec(1, 0.0));
}

CEventRatePopulationModel::TDouble1Vec
    CEventRatePopulationModel::baselineBucketMean(model_t::EFeature feature,
                                                  std::size_t pid,
                                                  std::size_t cid,
                                                  model_t::CResultType type,
                                                  const TSizeDoublePr1Vec &/*correlated*/,
                                                  core_t::TTime time) const
{
    if (!model_t::isAttributeConditional(feature))
    {
        cid = 0u;
    }

    const maths::CPrior *prior = this->prior(feature, cid);
    if (!prior || prior->isNonInformative())
    {
        return TDouble1Vec();
    }

    if (model_t::isDiurnal(feature))
    {
        TDouble1Vec value = this->currentBucketValue(feature, pid, cid, time);
        if (value.empty())
        {
            return TDouble1Vec(1, prior->marginalLikelihoodMean());
        }
        return TDouble1Vec(1, prior->nearestMarginalLikelihoodMean(value[0]));
    }

    TDouble1Vec result;

    TDecompositionCPtr1Vec trend = this->trend(feature, cid);
    if (model_t::dimension(feature) == 1)
    {
        double probability = 1.0;
        if (model_t::isConstant(feature) && !this->attributeProbabilities().lookup(cid, probability))
        {
            probability = 1.0;
        }

        double seasonalOffset = 0.0;
        if (!trend.empty() && trend[0]->initialized())
        {
            seasonalOffset = maths::CBasicStatistics::mean(trend[0]->baseline(time, 0.0)) - trend[0]->level();
        }

        double median = maths::CBasicStatistics::mean(prior->marginalLikelihoodConfidenceInterval(0.0));
        result.assign(1, probability * model_t::inverseOffsetCountToZero(feature, seasonalOffset + median));
    }
    else
    {
        // TODO support multivariate features.
        return TDouble1Vec();
    }

    this->correctBaselineForInterim(feature, pid, cid, type, this->currentBucketInterimCorrections(), result);
    TDouble1VecDouble1VecPr support = model_t::support(feature);
    return maths::CTools::truncate(result, support.first, support.second);
}

void CEventRatePopulationModel::sampleBucketStatistics(core_t::TTime startTime,
                                                       core_t::TTime endTime,
                                                       CResourceMonitor &resourceMonitor)
{
    CDataGatherer &gatherer = this->dataGatherer();
    core_t::TTime bucketLength = gatherer.bucketLength();

    if (!gatherer.dataAvailable(startTime))
    {
        return;
    }

    m_CurrentBucketStats.s_InterimCorrections.clear();
    for (core_t::TTime bucketStartTime = startTime;
         bucketStartTime < endTime;
         bucketStartTime += bucketLength)
    {
        this->CModel::sampleBucketStatistics(bucketStartTime, bucketStartTime + bucketLength, resourceMonitor);

        // Currently, we only remember one bucket.
        m_CurrentBucketStats.s_StartTime = bucketStartTime;
        TSizeUInt64PrVec &personCounts = m_CurrentBucketStats.s_PersonCounts;
        gatherer.personNonZeroCounts(bucketStartTime, personCounts);
        this->applyFilter(model_t::E_XF_Over, false, this->personFilter(), personCounts);

        TFeatureSizeSizePrFeatureDataPrVecPrVec featureData;
        gatherer.featureData(bucketStartTime, bucketLength, featureData);
        for (std::size_t i = 0u; i < featureData.size(); ++i)
        {
            model_t::EFeature feature = featureData[i].first;
            TSizeSizePrFeatureDataPrVec &data = m_CurrentBucketStats.s_FeatureData[feature];
            data.swap(featureData[i].second);
            LOG_TRACE(model_t::print(feature)
                      << " data = " << core::CContainerPrinter::print(data));
            this->applyFilters(feature, false, this->personFilter(), this->attributeFilter(), data);
        }
    }
}

void CEventRatePopulationModel::sample(core_t::TTime startTime,
                                       core_t::TTime endTime,
                                       CResourceMonitor &resourceMonitor)
{
    CDataGatherer &gatherer = this->dataGatherer();
    core_t::TTime bucketLength = gatherer.bucketLength();
    if (!gatherer.validateSampleTimes(startTime, endTime))
    {
        return;
    }
    this->createUpdateNewModels(startTime, resourceMonitor);
    m_CurrentBucketStats.s_InterimCorrections.clear();

    // We gather up the data and update the models at the end.
    TFeatureSizeFeatureSampleDataUMapMap sampleData;

    for (core_t::TTime bucketStartTime = startTime;
         bucketStartTime < endTime;
         bucketStartTime += bucketLength)
    {
        LOG_TRACE("Sampling [" << bucketStartTime
                  << "," << bucketStartTime + bucketLength << ")");

        gatherer.sampleNow(bucketStartTime);

        this->CPopulationModel::sample(bucketStartTime,
                                       bucketStartTime + bucketLength,
                                       resourceMonitor);

        // Currently, we only remember one bucket.
        m_CurrentBucketStats.s_StartTime = bucketStartTime;
        TSizeUInt64PrVec &personCounts = m_CurrentBucketStats.s_PersonCounts;
        gatherer.personNonZeroCounts(bucketStartTime, personCounts);
        this->applyFilter(model_t::E_XF_By, true, this->personFilter(), personCounts);

        // Declared outside loop to minimize number of times they are created.
        TDouble1Vec sample(1);
        TDouble1Vec4Vec weight(1, TDouble1Vec(1));

        // Update the person and population count feature models.
        TFeatureSizeSizePrFeatureDataPrVecPrVec featureData;
        gatherer.featureData(bucketStartTime, bucketLength, featureData);
        for (std::size_t i = 0u; i < featureData.size(); ++i)
        {
            model_t::EFeature feature = featureData[i].first;
            TSizeSizePrFeatureDataPrVec &data = m_CurrentBucketStats.s_FeatureData[feature];
            data.swap(featureData[i].second);
            LOG_TRACE(model_t::print(feature)
                      << " data = " << core::CContainerPrinter::print(data));

            switch (feature)
            {
            CASE_INDIVIDUAL_COUNT:
            CASE_INDIVIDUAL_METRIC:
                LOG_ERROR("Unexpected feature = " << model_t::print(feature));
                continue;

            case model_t::E_PopulationAttributeTotalCountByPerson:
                continue;
            case model_t::E_PopulationCountByBucketPersonAndAttribute:
            case model_t::E_PopulationIndicatorOfBucketPersonAndAttribute:
                break;
            case model_t::E_PopulationUniquePersonCountByAttribute:
                {
                    TDoubleVec categories;
                    TDoubleVec concentrations;
                    categories.reserve(data.size());
                    concentrations.reserve(data.size());
                    for (std::size_t j = 0u; j < data.size(); ++j)
                    {
                        const TSizeSizePrFeatureDataPr &tuple = data[j];
                        categories.push_back(static_cast<double>(CDataGatherer::extractAttributeId(tuple)));
                        concentrations.push_back(static_cast<double>(CDataGatherer::extractData(tuple).s_Count));
                    }
                    maths::CMultinomialConjugate prior(std::numeric_limits<int>::max(),
                                                       categories,
                                                       concentrations);
                    m_AttributeProbabilityPrior.swap(prior);
                }
                continue;
            case model_t::E_PopulationUniqueCountByBucketPersonAndAttribute:
            case model_t::E_PopulationLowUniqueCountByBucketPersonAndAttribute:
            case model_t::E_PopulationHighUniqueCountByBucketPersonAndAttribute:
            case model_t::E_PopulationLowCountsByBucketPersonAndAttribute:
            case model_t::E_PopulationHighCountsByBucketPersonAndAttribute:
            case model_t::E_PopulationInfoContentByBucketPersonAndAttribute:
            case model_t::E_PopulationLowInfoContentByBucketPersonAndAttribute:
            case model_t::E_PopulationHighInfoContentByBucketPersonAndAttribute:
            case model_t::E_PopulationTimeOfDayByBucketPersonAndAttribute:
            case model_t::E_PopulationTimeOfWeekByBucketPersonAndAttribute:
                break;

            CASE_POPULATION_METRIC:
            CASE_PEERS_COUNT:
            CASE_PEERS_METRIC:
                LOG_ERROR("Unexpected feature = " << model_t::print(feature));
                continue;
            }

            this->applyFilters(feature, true, this->personFilter(), this->attributeFilter(), data);

            core_t::TTime sampleTime = model_t::sampleTime(feature,
                                                           bucketStartTime,
                                                           bucketLength);

            TSizeFeatureSampleDataUMap &featureSampleData = sampleData[feature];

            for (std::size_t j = 0u; j < data.size(); ++j)
            {
                std::size_t pid = CDataGatherer::extractPersonId(data[j]);
                std::size_t cid = CDataGatherer::extractAttributeId(data[j]);
                uint64_t count = CDataGatherer::extractData(data[j]).s_Count;
                double adjustedCount = model_t::offsetCountToZero(
                                           feature, static_cast<double>(count));
                LOG_TRACE("Adding " << adjustedCount
                          << " for person = " << gatherer.personName(pid)
                          << " and attribute = " << gatherer.attributeName(cid));

                SFeatureSampleData &attributeSampleData = featureSampleData[cid];

                sample[0] = adjustedCount;
                weight[0][0] = this->sampleRateWeight(pid, cid) * this->learnRate(feature);
                attributeSampleData.s_Times.push_back(sampleTime);
                attributeSampleData.s_Samples.push_back(sample);
                attributeSampleData.s_Weights.push_back(weight);
            }
        }

        m_AttributeProbabilities = TLessLikelyProbability(m_AttributeProbabilityPrior);
    }

    this->updatePriors(COUNT_WEIGHT, sampleData, m_AttributePriors, m_AttributeMultivariatePriors);
}

void CEventRatePopulationModel::prune(std::size_t maximumAge)
{
    CDataGatherer &gatherer = this->dataGatherer();

    TSizeVec peopleToRemove;
    TSizeVec attributesToRemove;
    this->peopleAndAttributesToRemove(m_CurrentBucketStats.s_StartTime,
                                      maximumAge,
                                      peopleToRemove,
                                      attributesToRemove);

    if (peopleToRemove.empty() && attributesToRemove.empty())
    {
        return;
    }

    std::sort(peopleToRemove.begin(), peopleToRemove.end());
    std::sort(attributesToRemove.begin(), attributesToRemove.end());
    LOG_DEBUG("Removing people {" << this->printPeople(peopleToRemove, 20) << '}');
    LOG_DEBUG("Removing attributes {" << this->printAttributes(attributesToRemove, 20) << '}');

    // Stop collecting for these people/attributes and add them
    // to the free list.
    gatherer.recyclePeople(peopleToRemove);
    gatherer.recycleAttributes(attributesToRemove);

    if (gatherer.dataAvailable(m_CurrentBucketStats.s_StartTime))
    {
        TFeatureSizeSizePrFeatureDataPrVecPrVec featureData;
        gatherer.featureData(m_CurrentBucketStats.s_StartTime, gatherer.bucketLength(), featureData);
        for (std::size_t i = 0u; i < featureData.size(); ++i)
        {
            model_t::EFeature feature = featureData[i].first;
            TSizeSizePrFeatureDataPrVec &data = m_CurrentBucketStats.s_FeatureData[feature];
            data.swap(featureData[i].second);
        }
    }

    TDoubleVec categoriesToRemove;
    categoriesToRemove.reserve(attributesToRemove.size());
    for (std::size_t i = 0u; i < attributesToRemove.size(); ++i)
    {
        categoriesToRemove.push_back(static_cast<double>(attributesToRemove[i]));
    }
    std::sort(categoriesToRemove.begin(), categoriesToRemove.end());
    m_AttributeProbabilityPrior.removeCategories(categoriesToRemove);
    m_AttributeProbabilities = TLessLikelyProbability(m_AttributeProbabilityPrior);

    this->clearPrunedResources(peopleToRemove, attributesToRemove);
    this->removePeople(peopleToRemove);
}

bool CEventRatePopulationModel::computeProbability(std::size_t pid,
                                                   core_t::TTime startTime,
                                                   core_t::TTime endTime,
                                                   CPartitioningFields &partitioningFields,
                                                   std::size_t numberAttributeProbabilities,
                                                   SAnnotatedProbability &result) const
{
    const CDataGatherer &gatherer = this->dataGatherer();
    core_t::TTime bucketLength = gatherer.bucketLength();

    if (endTime != startTime + bucketLength)
    {
        LOG_ERROR("Can only compute probability for single bucket");
        return false;
    }
    if (pid > gatherer.numberPeople())
    {
        LOG_TRACE("No person for pid = " << pid);
        return false;
    }

    LOG_TRACE("computeProbability(" << gatherer.personName(pid) << ")");

    typedef boost::shared_ptr<const std::string> TStrPtr;
    typedef core::CSmallVector<TStrPtr, 1> TStrPtr1Vec;
    typedef boost::unordered_map<std::size_t, CProbabilityAndInfluenceCalculator> TSizeProbabilityAndInfluenceUMap;
    typedef TSizeProbabilityAndInfluenceUMap::const_iterator TSizeProbabilityAndInfluenceUMapCItr;
    typedef std::pair<double, model_t::EFeature> TDoubleFeaturePr;
    typedef maths::CBasicStatistics::COrderStatisticsStack<TDoubleFeaturePr, 1u> TDoubleFeaturePrMinAccumulator;
    typedef boost::unordered_map<std::size_t, TDoubleFeaturePrMinAccumulator> TSizeDoubleFeaturePrMinAccumulatorUMap;
    typedef TSizeDoubleFeaturePrMinAccumulatorUMap::const_iterator TSizeDoubleFeaturePrMinAccumulatorUMapCItr;

    partitioningFields.add(gatherer.attributeFieldName(), EMPTY_STRING);

    CProbabilityAndInfluenceCalculator pJoint(this->params().s_InfluenceCutoff);
    pJoint.addAggregator(maths::CJointProbabilityOfLessLikelySamples());

    CProbabilityAndInfluenceCalculator pConditionalTemplate(this->params().s_InfluenceCutoff);
    pConditionalTemplate.addAggregator(maths::CJointProbabilityOfLessLikelySamples());
    pConditionalTemplate.addAggregator(maths::CProbabilityOfExtremeSample());
    TSizeProbabilityAndInfluenceUMap pConditional;

    TSizeDoubleFeaturePrMinAccumulatorUMap features;
    maths::CMultinomialConjugate personAttributeProbabilityPrior(m_NewAttributeProbabilityPrior);

    CAnnotatedProbabilityBuilder resultBuilder(result,
                                               std::max(numberAttributeProbabilities, std::size_t(1)),
                                               function_t::function(gatherer.features()),
                                               gatherer.numberActivePeople());
    resultBuilder.attributeProbabilityPrior(&m_AttributeProbabilityPrior);
    resultBuilder.personAttributeProbabilityPrior(&personAttributeProbabilityPrior);

    // Declared outside loop to minimize number of times they are created.
    TDouble1Vec category(1);
    static const TStrPtr1Vec NO_CORRELATED_ATTRIBUTES;
    static const TSizeDoublePr1Vec NO_CORRELATES;
    CProbabilityAndInfluenceCalculator::SParams params(SEASONAL_VARIANCE_WEIGHT, partitioningFields);
    params.s_Weights.resize(1, TDouble4Vec(1));

    for (std::size_t i = 0u; i < gatherer.numberFeatures(); ++i)
    {
        model_t::EFeature feature = gatherer.feature(i);
        LOG_TRACE("feature = " << model_t::print(feature));

        if (feature == model_t::E_PopulationAttributeTotalCountByPerson)
        {
            const TSizeSizePrFeatureDataPrVec &data = this->featureData(feature, startTime);
            TSizeSizePr range = CModelTools::personRange(data, pid);
            for (std::size_t j = range.first; j < range.second; ++j)
            {
                category[0] = static_cast<double>(CDataGatherer::extractAttributeId(data[j]));
                params.s_Weights[0][0] = static_cast<double>(CDataGatherer::extractData(data[j]).s_Count);
                personAttributeProbabilityPrior.addSamples(COUNT_WEIGHT, category, params.s_Weights);
            }
            continue;
        }
        else if (model_t::isCategorical(feature))
        {
            continue;
        }

        const TSizeSizePrFeatureDataPrVec &featureData = this->featureData(feature, startTime);
        TSizeSizePr range = CModelTools::personRange(featureData, pid);
        core_t::TTime sampleTime = model_t::sampleTime(feature, startTime, bucketLength);

        for (std::size_t j = range.first; j < range.second; ++j)
        {
            // 1) Sample the person's feature for the bucket.
            // 2) Compute the probability of the sample for the
            //    population model of the corresponding attribute.
            // 3) Update the attribute probability.
            // 4) Update the attribute influences.

            std::size_t cid = CDataGatherer::extractAttributeId(featureData[j]);
            if (cid >= this->attributeFirstBucketTimes().size())
            {
                LOG_TRACE("No first time for attribute " << gatherer.attributeName(cid)
                          << " and feature " << model_t::print(feature));
                continue;
            }

            if (this->shouldIgnoreResult(feature,
                                         result.s_ResultType,
                                         partitioningFields.partitionFieldValue(),
                                         pid,
                                         cid,
                                         sampleTime))
            {
                continue;
            }

            partitioningFields.back().second = TStrCRef(gatherer.attributeName(cid));
            const TFeatureData &dj = CDataGatherer::extractData(featureData[j]);
            uint64_t count = dj.s_Count;
            core_t::TTime elapsedTime = sampleTime - this->attributeFirstBucketTimes()[cid];

            std::size_t dimension = model_t::dimension(feature);
            if (dimension == 1)
            {
                const maths::CPrior *prior = this->prior(feature, cid);
                if (!prior)
                {
                    LOG_ERROR("No prior for " << gatherer.attributeName(cid)
                              << " and feature " << model_t::print(feature));
                    continue;
                }

                params.s_Feature = feature;
                params.s_Trend = this->trend(feature, cid);
                params.s_Prior = prior;
                params.s_ElapsedTime = elapsedTime;
                params.s_Time  = sampleTime;
                params.s_Value.assign(1, model_t::offsetCountToZero(feature, static_cast<double>(count)));
                params.s_Count = 1.0;
                params.s_Sample = this->detrend(feature, cid, sampleTime, SEASONAL_CONFIDENCE_INTERVAL, params.s_Value);
                params.s_Weights[0] = this->seasonalVarianceScale(feature, cid, sampleTime, SEASONAL_CONFIDENCE_INTERVAL).second;
                params.s_BucketEmpty = false;
                params.s_ProbabilityBucketEmpty = 0.0;
                params.s_Confidence = SEASONAL_CONFIDENCE_INTERVAL;
                if (result.isInterim() && model_t::requiresInterimResultAdjustment(feature))
                {
                    double mode = prior->marginalLikelihoodMode(SEASONAL_VARIANCE_WEIGHT, params.s_Weights[0]);
                    TDouble1Vec corrections(1, this->interimValueCorrector().corrections(
                                                             sampleTime,
                                                             this->currentBucketTotalCount(),
                                                             mode, params.s_Sample[0]));
                    params.s_Value  += corrections;
                    params.s_Sample += corrections;
                    this->currentBucketInterimCorrections().emplace(core::make_triple(feature, pid, cid), corrections);
                }

                CProbabilityAndInfluenceCalculator *calculator = 0;
                params.s_Probability = 1.0;
                params.s_Tail = maths_t::E_UndeterminedTail;
                if (   model_t::isAttributeConditional(feature)
                    && pConditional.emplace(cid, pConditionalTemplate).first->second.addProbability(feature, *prior, elapsedTime,
                                                                                                    SEASONAL_VARIANCE_WEIGHT,
                                                                                                    params.s_Sample,
                                                                                                    params.s_Weights,
                                                                                                    params.s_BucketEmpty,
                                                                                                    params.s_ProbabilityBucketEmpty,
                                                                                                    params.s_Probability,
                                                                                                    params.s_Tail))
                {
                    LOG_TRACE("P(" << params.describe()
                              << ", attribute = " << gatherer.attributeName(cid)
                              << ", person = " << gatherer.personName(pid) << ") = "
                              << params.s_Probability);
                    calculator = &pConditional.emplace(cid, pConditionalTemplate).first->second;
                    features[cid].add(TDoubleFeaturePr(params.s_Probability, feature));
                }
                else if (  !model_t::isAttributeConditional(feature)
                         && pJoint.addProbability(feature, *prior, elapsedTime,
                                                  SEASONAL_VARIANCE_WEIGHT,
                                                  params.s_Sample,
                                                  params.s_Weights,
                                                  params.s_BucketEmpty,
                                                  params.s_ProbabilityBucketEmpty,
                                                  params.s_Probability,
                                                  params.s_Tail))
                {
                    LOG_TRACE("P(" << params.describe()
                              << ", person = " << gatherer.personName(pid) << ") = "
                              << params.s_Probability);
                    calculator = &pJoint;
                    resultBuilder.addAttributeProbability(cid, gatherer.attributeNamePtr(cid),
                                                          1.0, params.s_Probability,
                                                          model_t::CResultType::E_Unconditional,
                                                          feature,
                                                          NO_CORRELATED_ATTRIBUTES, NO_CORRELATES);
                }
                else
                {
                    LOG_ERROR("Unable to compute P(" << params.describe()
                              << ", attribute = " << gatherer.attributeName(cid)
                              << ", person = " << gatherer.personName(pid) << ")");
                }

                if (calculator && !dj.s_InfluenceValues.empty())
                {
                    for (std::size_t k = 0u; k < dj.s_InfluenceValues.size(); ++k)
                    {
                        if (const CInfluenceCalculator *influenceCalculator = this->influenceCalculator(feature, k))
                        {
                            calculator->plugin(*influenceCalculator);
                            calculator->addInfluences(*(gatherer.beginInfluencers() + k), dj.s_InfluenceValues[k], params);
                        }
                    }
                }
            }
            else
            {
                // TODO support multivariate features.
            }
        }
    }

    const CModelTools::CLessLikelyProbability &pAttributes = this->attributeProbabilities();
    for (TSizeProbabilityAndInfluenceUMapCItr itr = pConditional.begin();
         itr != pConditional.end();
         ++itr)
    {
        std::size_t cid = itr->first;

        CProbabilityAndInfluenceCalculator pPersonAndAttribute(this->params().s_InfluenceCutoff);
        pPersonAndAttribute.addAggregator(maths::CJointProbabilityOfLessLikelySamples());
        pPersonAndAttribute.add(itr->second);
        double pAttribute;
        if (pAttributes.lookup(cid, pAttribute))
        {
            pPersonAndAttribute.addProbability(pAttribute);
        }
        LOG_TRACE("P(" << gatherer.attributeName(cid) << ") = " << pAttribute);

        // The idea is we imagine drawing n samples from the person's total
        // attribute set, where n is the size of the person's attribute set,
        // and we weight each sample according to the probability it occurs
        // assuming the attributes are distributed according to the supplied
        // multinomial distribution.
        double w = 1.0;
        double pAttributeGivenPerson;
        if (personAttributeProbabilityPrior.probability(static_cast<double>(cid),
                                                        pAttributeGivenPerson))
        {
            w = maths::CCategoricalTools::probabilityOfCategory(pConditional.size(),
                                                                pAttributeGivenPerson);
        }
        LOG_TRACE("w = " << w);

        pJoint.add(pPersonAndAttribute, w);

        TSizeDoubleFeaturePrMinAccumulatorUMapCItr featureItr = features.find(cid);
        if (featureItr == features.end())
        {
            LOG_ERROR("No feature for " << gatherer.attributeName(cid));
            continue;
        }
        double p;
        pPersonAndAttribute.calculate(p);

        resultBuilder.addAttributeProbability(cid, gatherer.attributeNamePtr(cid),
                                              pAttribute, p,
                                              model_t::CResultType::E_Unconditional,
                                              (featureItr->second)[0].second,
                                              NO_CORRELATED_ATTRIBUTES, NO_CORRELATES);
    }

    if (pJoint.empty())
    {
        LOG_TRACE("No samples in [" << startTime << "," << endTime << ")");
        return false;
    }

    double p;
    if (!pJoint.calculate(p, result.s_Influences))
    {
        LOG_ERROR("Failed to compute probability of " << this->personName(pid));
        return false;
    }
    LOG_TRACE("probability(" << this->personName(pid) << ") = " << p);
    resultBuilder.probability(p);
    resultBuilder.build();

    return true;
}

bool CEventRatePopulationModel::computeTotalProbability(const std::string &/*person*/,
                                                        std::size_t /*numberAttributeProbabilities*/,
                                                        TOptionalDouble &probability,
                                                        TAttributeProbability1Vec &attributeProbabilities) const
{
    probability = TOptionalDouble();
    attributeProbabilities.clear();
    return true;
}

void CEventRatePopulationModel::outputCurrentBucketStatistics(const std::string &partitionFieldValue,
                                                              const TBucketStatsOutputFunc &outputFunc) const
{
    const CDataGatherer &gatherer = this->dataGatherer();
    const std::string &partitionFieldName = gatherer.partitionFieldName();
    const std::string &personFieldName = gatherer.personFieldName();
    const std::string &attributeFieldName = gatherer.attributeFieldName();

    const TFeatureSizeSizePrFeatureDataPrVecMap &featureData = m_CurrentBucketStats.s_FeatureData;
    for (TFeatureSizeSizePrFeatureDataPrVecMapCItr itr = featureData.begin();
         itr != featureData.end();
         ++itr)
    {
        const std::string &funcName = model_t::outputFunctionName(itr->first);
        const TSizeSizePrFeatureDataPrVec &data = itr->second;
        for (std::size_t i = 0u; i < data.size(); ++i)
        {
            outputFunc(
                SOutputStats(
                    m_CurrentBucketStats.s_StartTime,
                    true,
                    false,
                    partitionFieldName,
                    partitionFieldValue,
                    personFieldName,
                    gatherer.personName(CDataGatherer::extractPersonId(data[i]), EMPTY_STRING),
                    attributeFieldName,
                    gatherer.attributeName(CDataGatherer::extractAttributeId(data[i]), EMPTY_STRING),
                    EMPTY_STRING,
                    funcName,
                    static_cast<double>(data[i].second.s_Count),
                    true
                )
            );
        }
    }
}

uint64_t CEventRatePopulationModel::checksum(bool includeCurrentBucketStats) const
{
    uint64_t seed = this->CPopulationModel::checksum(includeCurrentBucketStats);
    seed = maths::CChecksum::calculate(seed, m_NewAttributeProbabilityPrior);
    if (includeCurrentBucketStats)
    {
        seed = maths::CChecksum::calculate(seed, m_CurrentBucketStats.s_StartTime);
    }

    TStrCRefStrCRefPrUInt64Map hashes;
    const TDoubleVec &categories = m_AttributeProbabilityPrior.categories();
    const TDoubleVec &concentrations = m_AttributeProbabilityPrior.concentrations();
    for (std::size_t i = 0u; i < categories.size(); ++i)
    {
#define ATTRIBUTE_KEY(cid) TStrCRefStrCRefPr(boost::cref(EMPTY_STRING), \
                                             boost::cref(this->attributeName(static_cast<std::size_t>(cid))))
        uint64_t &hash = hashes[ATTRIBUTE_KEY(categories[i])];
#undef ATTRIBUTE_KEY
        hash = maths::CChecksum::calculate(hash, concentrations[i]);
    }
    this->checksums(m_AttributePriors, hashes);
    this->checksums(m_AttributeMultivariatePriors, hashes);

    if (includeCurrentBucketStats)
    {
        this->checksums(this->personCounts(), hashes);
        const TFeatureSizeSizePrFeatureDataPrVecMap &featureData = m_CurrentBucketStats.s_FeatureData;
        for (TFeatureSizeSizePrFeatureDataPrVecMapCItr itr = featureData.begin();
             itr != featureData.end();
             ++itr)
        {
            for (std::size_t i = 0u; i < itr->second.size(); ++i)
            {
#define KEY(pid, cid) TStrCRefStrCRefPr(boost::cref(this->personName(pid)), \
                                        boost::cref(this->attributeName(cid)))
                uint64_t &hash = hashes[KEY(CDataGatherer::extractPersonId(itr->second[i]),
                                            CDataGatherer::extractAttributeId(itr->second[i]))];
#undef KEY
                hash = maths::CChecksum::calculate(
                           hash,
                           CDataGatherer::extractData(itr->second[i]).s_Count);
            }
        }
    }

    LOG_TRACE("seed = " << seed);
    LOG_TRACE("hashes = " << core::CContainerPrinter::print(hashes));

    return maths::CChecksum::calculate(seed, hashes);
}

void CEventRatePopulationModel::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CEventRatePopulationModel");
    this->CPopulationModel::debugMemoryUsage(mem->addChild());
    core::CMemoryDebug::dynamicSize("m_CurrentBucketStats.s_PersonCounts",
                                    m_CurrentBucketStats.s_PersonCounts, mem);
    core::CMemoryDebug::dynamicSize("m_CurrentBucketStats.s_FeatureData",
                                    m_CurrentBucketStats.s_FeatureData, mem);
    core::CMemoryDebug::dynamicSize("m_CurrentBucketStats.s_InterimCorrections",
                                    m_CurrentBucketStats.s_InterimCorrections, mem);
    core::CMemoryDebug::dynamicSize("m_AttributeProbabilities",
                                    m_AttributeProbabilities, mem);
    core::CMemoryDebug::dynamicSize("m_NewPersonAttributePrior",
                                    m_NewAttributeProbabilityPrior, mem);
    core::CMemoryDebug::dynamicSize("m_AttributeProbabilityPrior",
                                    m_AttributeProbabilityPrior, mem);
    core::CMemoryDebug::dynamicSize("m_AttributePriors", m_AttributePriors, mem);
    core::CMemoryDebug::dynamicSize("m_AttributeMultivariatePriors",
                                    m_AttributeMultivariatePriors, mem);
    core::CMemoryDebug::dynamicSize("m_MemoryEstimator", m_MemoryEstimator, mem);
}

std::size_t CEventRatePopulationModel::memoryUsage(void) const
{
    const CDataGatherer &gatherer = this->dataGatherer();
    return this->estimateMemoryUsage(gatherer.numberActivePeople(),
                                     gatherer.numberActiveAttributes(),
                                     0); // # correlations
}

std::size_t CEventRatePopulationModel::computeMemoryUsage(void) const
{
    std::size_t mem = this->CPopulationModel::memoryUsage();
    mem += core::CMemory::dynamicSize(m_CurrentBucketStats.s_PersonCounts);
    mem += core::CMemory::dynamicSize(m_CurrentBucketStats.s_FeatureData);
    mem += core::CMemory::dynamicSize(m_CurrentBucketStats.s_InterimCorrections);
    mem += core::CMemory::dynamicSize(m_AttributeProbabilities);
    mem += core::CMemory::dynamicSize(m_NewAttributeProbabilityPrior);
    mem += core::CMemory::dynamicSize(m_AttributeProbabilityPrior);
    mem += core::CMemory::dynamicSize(m_AttributePriors);
    mem += core::CMemory::dynamicSize(m_AttributeMultivariatePriors);
    mem += core::CMemory::dynamicSize(m_MemoryEstimator);
    return mem;
}

std::size_t CEventRatePopulationModel::estimateMemoryUsage(std::size_t numberPeople,
                                                           std::size_t numberAttributes,
                                                           std::size_t numberCorrelations) const
{
    CMemoryUsageEstimator::TSizeArray predictors;
    predictors[CMemoryUsageEstimator::E_People]       = numberPeople;
    predictors[CMemoryUsageEstimator::E_Attributes]   = numberAttributes;
    predictors[CMemoryUsageEstimator::E_Correlations] = numberCorrelations;
    CMemoryUsageEstimator::TOptionalSize guess = m_MemoryEstimator.estimate(predictors);
    if (guess)
    {
        return guess.get();
    }
    else
    {
        std::size_t mem = this->computeMemoryUsage();
        m_MemoryEstimator.addValue(predictors, mem);
        return mem;
    }
}

std::size_t CEventRatePopulationModel::staticSize(void) const
{
    return sizeof(*this);
}

CEventRatePopulationModel::CModelDetailsViewPtr
    CEventRatePopulationModel::details(void) const
{
    return CModelDetailsViewPtr(new CEventRatePopulationModelDetailsView(*this));
}

const CEventRatePopulationModel::TSizeSizePrFeatureDataPrVec &
    CEventRatePopulationModel::featureData(model_t::EFeature feature,
                                           core_t::TTime time) const
{
    static const TSizeSizePrFeatureDataPrVec EMPTY;
    if (!this->bucketStatsAvailable(time))
    {
        LOG_ERROR("No statistics at " << time
                  << ", current bucket = [" << m_CurrentBucketStats.s_StartTime
                  << "," << m_CurrentBucketStats.s_StartTime + this->bucketLength() << ")");
        return EMPTY;
    }
    const TFeatureSizeSizePrFeatureDataPrVecMap &features = m_CurrentBucketStats.s_FeatureData;
    TFeatureSizeSizePrFeatureDataPrVecMapCItr result = features.find(feature);
    return result == features.end() ? EMPTY : result->second;
}

core_t::TTime CEventRatePopulationModel::currentBucketStartTime(void) const
{
    return m_CurrentBucketStats.s_StartTime;
}

void CEventRatePopulationModel::currentBucketStartTime(core_t::TTime startTime)
{
    m_CurrentBucketStats.s_StartTime = startTime;
}

uint64_t CEventRatePopulationModel::currentBucketTotalCount(void) const
{
    return m_CurrentBucketStats.s_TotalCount;
}

CEventRatePopulationModel::TFeatureSizeSizeTripleDouble1VecUMap &
    CEventRatePopulationModel::currentBucketInterimCorrections(void) const
{
    return m_CurrentBucketStats.s_InterimCorrections;
}

void CEventRatePopulationModel::createNewModels(std::size_t n, std::size_t m)
{
    if (m > 0)
    {
        CModelTools::createPriors(m, this->newPriors(), m_AttributePriors);
        CModelTools::createPriors(m, this->newMultivariatePriors(), m_AttributeMultivariatePriors);
    }
    this->CPopulationModel::createNewModels(n, m);
}

void CEventRatePopulationModel::updateRecycledModels(void)
{
    CDataGatherer &gatherer = this->dataGatherer();
    const TSizeVec &recycledAttributes = gatherer.recycledAttributeIds();
    this->reinitializeAttributePriors(recycledAttributes,
                                      m_AttributePriors,
                                      m_AttributeMultivariatePriors);
    this->CPopulationModel::updateRecycledModels();
}

void CEventRatePopulationModel::clearPrunedResources(const TSizeVec &people,
                                                     const TSizeVec &attributes)
{
    this->clearAttributePriors(attributes,
                               m_AttributePriors,
                               m_AttributeMultivariatePriors);
    this->CPopulationModel::clearPrunedResources(people, attributes);
}

const maths::CPrior *CEventRatePopulationModel::prior(model_t::EFeature feature,
                                               std::size_t cid) const
{
    const TPriorPtrVec &priors = this->priors(feature);
    return cid < priors.size() ? priors[cid].get() : 0;
}

const maths::CMultivariatePrior *
    CEventRatePopulationModel::multivariatePrior(model_t::EFeature feature, std::size_t cid) const
{
    const TMultivariatePriorPtrVec &multivariatePriors = this->multivariatePriors(feature);
    return cid < multivariatePriors.size() ? multivariatePriors[cid].get() : 0;
}

bool CEventRatePopulationModel::resetPrior(model_t::EFeature feature,
                                           std::size_t cid)
{
#define SET_TO_NON_INFORMATIVE(priors, cid, newPrior) if (cid >= priors.size())                                  \
                                                      {                                                          \
                                                          return false;                                          \
                                                      }                                                          \
                                                      if (!newPrior || !priors[cid])                             \
                                                      {                                                          \
                                                          return false;                                          \
                                                      }                                                          \
                                                      priors[cid]->setToNonInformative(newPrior->offsetMargin(), \
                                                                                       newPrior->decayRate())
    if (model_t::dimension(feature) == 1)
    {
        const TPriorPtrVec &priors = this->priors(feature);
        maths::CPrior *newPrior = this->newPrior(feature);
        SET_TO_NON_INFORMATIVE(priors, cid, newPrior);
    }
    else
    {
        const TMultivariatePriorPtrVec &priors = this->multivariatePriors(feature);
        maths::CMultivariatePrior *newPrior = this->newMultivariatePrior(feature);
        SET_TO_NON_INFORMATIVE(priors, cid, newPrior);
    }
#undef SET_TO_NON_INFORMATIVE

    return true;
}

const CEventRatePopulationModel::TPriorPtrVec &
    CEventRatePopulationModel::priors(model_t::EFeature feature) const
{
    static const TPriorPtrVec EMPTY;
    TFeaturePriorPtrVecMapCItr result = m_AttributePriors.find(feature);
    return result == m_AttributePriors.end() ? EMPTY : result->second;
}

const CEventRatePopulationModel::TMultivariatePriorPtrVec &
    CEventRatePopulationModel::multivariatePriors(model_t::EFeature feature) const
{
    static const TMultivariatePriorPtrVec EMPTY;
    TFeatureMultivariatePriorPtrVecMapCItr result = m_AttributeMultivariatePriors.find(feature);
    return result == m_AttributeMultivariatePriors.end() ? EMPTY : result->second;
}

const CEventRatePopulationModel::TLessLikelyProbability &
    CEventRatePopulationModel::attributeProbabilities(void) const
{
    return m_AttributeProbabilities;
}

const CEventRatePopulationModel::TSizeUInt64PrVec &
    CEventRatePopulationModel::personCounts(void) const
{
    return m_CurrentBucketStats.s_PersonCounts;
}

void CEventRatePopulationModel::currentBucketTotalCount(uint64_t totalCount)
{
    m_CurrentBucketStats.s_TotalCount = totalCount;
}

bool CEventRatePopulationModel::bucketStatsAvailable(core_t::TTime time) const
{
    return    time >= m_CurrentBucketStats.s_StartTime
           && time < m_CurrentBucketStats.s_StartTime + this->bucketLength();
}

////////// CEventRatePopulationModel::SBucketStats Implementation //////////

CEventRatePopulationModel::SBucketStats::SBucketStats(core_t::TTime startTime) :
        s_StartTime(startTime),
        s_PersonCounts(),
        s_TotalCount(0),
        s_FeatureData(),
        s_InterimCorrections(1)
{
}


}
}
