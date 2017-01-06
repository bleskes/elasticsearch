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

#include <model/CMetricPopulationModel.h>

#include <core/CAllocationStrategy.h>
#include <core/CContainerPrinter.h>
#include <core/CLogger.h>
#include <core/CStatePersistInserter.h>
#include <core/CStatistics.h>

#include <maths/CIntegerTools.h>
#include <maths/CChecksum.h>
#include <maths/COrderings.h>
#include <maths/CPrior.h>
#include <maths/CTimeSeriesDecomposition.h>
#include <maths/CTimeSeriesDecompositionStateSerialiser.h>
#include <maths/CTools.h>
#include <maths/ProbabilityAggregators.h>

#include <model/CAnnotatedProbabilityBuilder.h>
#include <model/CDataGatherer.h>
#include <model/CGathererTools.h>
#include <model/CInterimBucketCorrector.h>
#include <model/CModelConfig.h>
#include <model/CModelDetailsView.h>
#include <model/CModelTools.h>
#include <model/CPopulationModelDetail.h>
#include <model/CProbabilityAndInfluenceCalculator.h>
#include <model/FrequencyPredicates.h>

#include <boost/bind.hpp>
#include <boost/iterator/counting_iterator.hpp>
#include <boost/optional.hpp>
#include <boost/range.hpp>
#include <boost/ref.hpp>
#include <boost/tuple/tuple.hpp>
#include <boost/unordered_map.hpp>

namespace prelert
{
namespace model
{

namespace
{

typedef std::pair<std::size_t, std::size_t> TSizeSizePr;
typedef boost::optional<CSample> TOptionalSample;
typedef CMetricPopulationModel::TFeatureSizeSizePrFeatureDataPrVecMap TFeatureSizeSizePrFeatureDataPrVecMap;
typedef TFeatureSizeSizePrFeatureDataPrVecMap::const_iterator TFeatureSizeSizePrFeatureDataPrVecMapCItr;
typedef CMetricPopulationModel::TSizeSizePrFeatureDataPrVec TSizeSizePrFeatureDataPrVec;
typedef TSizeSizePrFeatureDataPrVec::const_iterator TSizeSizePrFeatureDataPrVecCItr;
typedef std::pair<model_t::EFeature, TSizeSizePrFeatureDataPrVec> TFeatureSizeSizePrFeatureDataPrVecPr;
typedef std::vector<TFeatureSizeSizePrFeatureDataPrVecPr> TFeatureSizeSizePrFeatureDataPrVecPrVec;

// We obfuscate the XML element names to avoid giving away too much
// information about our model.
const std::string POPULATION_STATE_TAG("a");
const std::string POPULATION_FEATURE_PRIOR_TAG("b");
const std::string POPULATION_FEATURE_MULTIVARIATE_PRIOR_TAG("c");
const std::string MEMORY_ESTIMATOR_TAG("d");
// Nested tags
const std::string FEATURE_TAG("a");
const std::string PRIOR_TAG("b");

const maths_t::ESampleWeightStyle WEIGHT_STYLES_[] =
    {
        maths_t::E_SampleSeasonalVarianceScaleWeight,
        maths_t::E_SampleCountVarianceScaleWeight
    };
const maths_t::TWeightStyleVec WEIGHT_STYLES(boost::begin(WEIGHT_STYLES_),
                                             boost::end(WEIGHT_STYLES_));

} // unnamed::

CMetricPopulationModel::CMetricPopulationModel(const SModelParams &params,
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
                             - dataGatherer->bucketLength())
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

CMetricPopulationModel::CMetricPopulationModel(const SModelParams &params,
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
    traverser.traverseSubLevel(boost::bind(&CMetricPopulationModel::acceptRestoreTraverser,
                                           this,
                                           boost::ref(params.s_ExtraDataRestoreFunc),
                                           _1));
}

CMetricPopulationModel::CMetricPopulationModel(bool isForPersistence,
                                               const CMetricPopulationModel &other) :
        CPopulationModel(isForPersistence, other),
        m_CurrentBucketStats(0), // Not needed for persistence so minimally constructed
        m_MemoryEstimator(other.m_MemoryEstimator)
{
    if (!isForPersistence)
    {
        LOG_ABORT("This constructor only creates clones for persistence");
    }
    CModelTools::clonePriors(other.m_AttributePriors, m_AttributePriors);
    CModelTools::clonePriors(other.m_AttributeMultivariatePriors, m_AttributeMultivariatePriors);
}

bool CMetricPopulationModel::isEventRate(void) const
{
    return false;
}

bool CMetricPopulationModel::isMetric(void) const
{
    return true;
}

void CMetricPopulationModel::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertLevel(POPULATION_STATE_TAG,
                         boost::bind(&CMetricPopulationModel::doAcceptPersistInserter, this, _1));
    for (TFeaturePriorPtrVecMapCItr i = m_AttributePriors.begin();
         i != m_AttributePriors.end();
         ++i)
    {
        inserter.insertLevel(POPULATION_FEATURE_PRIOR_TAG,
                             boost::bind(&featurePriorsAcceptPersistInserter,
                                         boost::cref(FEATURE_TAG),
                                         i->first,
                                         boost::cref(PRIOR_TAG),
                                         boost::cref(i->second),
                                         _1));
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
                                         boost::cref(i->second),
                                         _1));
    }
    core::CPersistUtils::persist(MEMORY_ESTIMATOR_TAG, m_MemoryEstimator, inserter);
}

bool CMetricPopulationModel::acceptRestoreTraverser(const model_t::TAnyRestoreFunc &extraDataRestoreFunc,
                                                    core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        if (name == POPULATION_STATE_TAG)
        {
            if (traverser.traverseSubLevel(boost::bind(&CMetricPopulationModel::doAcceptRestoreTraverser,
                                                       this, boost::cref(extraDataRestoreFunc), _1)) == false)
            {
                // Logging handled already.
                return false;
            }
        }
        else if (name == POPULATION_FEATURE_PRIOR_TAG)
        {
            if (traverser.traverseSubLevel(boost::bind(&CMetricPopulationModel::featurePriorsAcceptRestoreTraverser,
                                                       this,
                                                       maths_t::E_ContinuousData,
                                                       boost::cref(FEATURE_TAG),
                                                       boost::cref(PRIOR_TAG),
                                                       _1, boost::ref(m_AttributePriors))) == false)
            {
                return false;
            }
        }
        else if (name == POPULATION_FEATURE_MULTIVARIATE_PRIOR_TAG)
        {
            if (traverser.traverseSubLevel(boost::bind(&CMetricPopulationModel::featureMultivariatePriorsAcceptRestoreTraverser,
                                                       this,
                                                       maths_t::E_ContinuousData,
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

CModel *CMetricPopulationModel::cloneForPersistence(void) const
{
    return new CMetricPopulationModel(true, *this);
}

model_t::EModelType CMetricPopulationModel::category(void) const
{
    return model_t::E_MetricOnline;
}

CMetricPopulationModel::TDouble1Vec
    CMetricPopulationModel::currentBucketValue(model_t::EFeature feature,
                                               std::size_t pid,
                                               std::size_t cid,
                                               core_t::TTime time) const
{
    return this->currentBucketValue(this->featureData(feature, time),
                                    feature, pid, cid, TDouble1Vec());
}

CMetricPopulationModel::TDouble1Vec
    CMetricPopulationModel::baselineBucketMean(model_t::EFeature feature,
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

    typedef core::CSmallVector<std::size_t, 10> TSize10Vec;
    typedef core::CSmallVector<TSizeDoublePr, 10> TSizeDoublePr10Vec;

    TDouble1Vec result;

    TDecompositionCPtr1Vec trend = this->trend(feature, cid);
    if (model_t::dimension(feature) == 1)
    {
        const maths::CPrior *prior = this->prior(feature, cid);
        if (!prior || prior->isNonInformative())
        {
            return TDouble1Vec();
        }

        double seasonalOffset = 0.0;
        if (!trend.empty() && trend[0]->initialized())
        {
            seasonalOffset = maths::CBasicStatistics::mean(trend[0]->baseline(time, 0.0));
        }

        double median = maths::CBasicStatistics::mean(prior->marginalLikelihoodConfidenceInterval(0.0));
        result.assign(1, seasonalOffset + median);
    }
    else
    {
        const maths::CMultivariatePrior *prior = this->multivariatePrior(feature, cid);
        if (!prior || prior->isNonInformative())
        {
            return TDouble1Vec();
        }

        TSize10Vec marginalize(boost::make_counting_iterator(std::size_t(1)),
                               boost::make_counting_iterator(prior->dimension()));
        static const TSizeDoublePr10Vec CONDITION;

        result.reserve(prior->dimension());
        for (std::size_t i = 0u; i < result.size(); --marginalize[i], ++i)
        {
            double seasonalOffset = 0.0;
            if (i < trend.size() && trend[i]->initialized())
            {
                seasonalOffset = maths::CBasicStatistics::mean(trend[i]->baseline(time, 0.0));
            }

            double median = maths::CBasicStatistics::mean(
                                prior->univariate(marginalize, CONDITION).first->marginalLikelihoodConfidenceInterval(0.0));
            result.push_back(seasonalOffset + median);
        }
    }

    this->correctBaselineForInterim(feature, pid, cid, type,
                                    this->currentBucketInterimCorrections(), result);
    TDouble1VecDouble1VecPr support = model_t::support(feature);
    return maths::CTools::truncate(result, support.first, support.second);
}

void CMetricPopulationModel::sampleBucketStatistics(core_t::TTime startTime,
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
            LOG_TRACE(model_t::print(feature) << " data = " << core::CContainerPrinter::print(data));
            this->applyFilters(feature, false, this->personFilter(), this->attributeFilter(), data);
        }
    }
}

void CMetricPopulationModel::sample(core_t::TTime startTime,
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

    // Declared outside loop to minimize number of times it is created.
    static const maths_t::ESampleWeightStyle WEIGHT_STYLES_[] =
        {
            maths_t::E_SampleCountWeight,
            maths_t::E_SampleCountVarianceScaleWeight
        };
    static const maths_t::TWeightStyleVec WEIGHT_STYLES(boost::begin(WEIGHT_STYLES_),
                                                        boost::end(WEIGHT_STYLES_));

    // We gather up the data and update the models at the end.
    TFeatureSizeFeatureSampleDataUMapMap sampleData;

    for (core_t::TTime time = startTime; time < endTime; time += bucketLength)
    {
        LOG_TRACE("Sampling [" << time << "," << time + bucketLength << ")");

        gatherer.sampleNow(time);

        this->CPopulationModel::sample(time, time + bucketLength, resourceMonitor);

        // Currently, we only remember one bucket.
        m_CurrentBucketStats.s_StartTime = time;
        TSizeUInt64PrVec &personCounts = m_CurrentBucketStats.s_PersonCounts;
        gatherer.personNonZeroCounts(time, personCounts);
        this->applyFilter(model_t::E_XF_Over, true, this->personFilter(), personCounts);

        // Declared outside loop to minimize number of times it is created.
        TDouble1Vec4Vec weights(2);

        TFeatureSizeSizePrFeatureDataPrVecPrVec featureData;
        gatherer.featureData(time, bucketLength, featureData);
        const TTimeVec &attributeLastBucketTimes = this->attributeLastBucketTimes();

        for (std::size_t i = 0u; i < featureData.size(); ++i)
        {
            model_t::EFeature feature = featureData[i].first;
            std::size_t dimension = model_t::dimension(feature);
            TSizeSizePrFeatureDataPrVec &data = m_CurrentBucketStats.s_FeatureData[feature];
            data.swap(featureData[i].second);
            LOG_TRACE(model_t::print(feature) << " data = " << core::CContainerPrinter::print(data));
            this->applyFilters(feature, true, this->personFilter(), this->attributeFilter(), data);

            TSizeFeatureSampleDataUMap &featureSampleData = sampleData[feature];

            for (std::size_t j = 0u; j < data.size(); ++j)
            {
                std::size_t pid = CDataGatherer::extractPersonId(data[j]);
                std::size_t cid = CDataGatherer::extractAttributeId(data[j]);
                const TFeatureData &dj = CDataGatherer::extractData(data[j]);

                core_t::TTime cutoff = attributeLastBucketTimes[cid] - 2 * core::constants::DAY;
                SFeatureSampleData &attributeSampleData = featureSampleData[cid];
                const CGathererTools::TSampleVec &samples = dj.s_Samples;
                LOG_TRACE("Adding " << dj
                          << " for person = " << gatherer.personName(pid)
                          << " and attribute = " << gatherer.attributeName(cid));

                std::size_t n = 0u;
                for (std::size_t k = 0u; k < samples.size(); ++k)
                {
                    n += samples[k].time() < cutoff ? 0 : 1;
                }
                double updatesPerBucket = this->params().s_MaximumUpdatesPerBucket;
                double countWeight =  this->sampleRateWeight(pid, cid)
                                    * this->learnRate(feature)
                                    * (updatesPerBucket > 0.0 && n > 0 ?
                                       updatesPerBucket / static_cast<double>(n) : 1.0);
                LOG_TRACE("countWeight = " << countWeight);

                if (dj.s_BucketValue)
                {
                    attributeSampleData.s_BucketValues.push_back(
                            this->detrend(feature, cid,
                                          dj.s_BucketValue->time(), 0.0,
                                          dj.s_BucketValue->value(dimension)));
                }
                for (std::size_t k = 0u; k < samples.size(); ++k)
                {
                    if (samples[k].time() >= cutoff)
                    {
                        weights[0].assign(dimension, countWeight);
                        weights[1].assign(dimension, samples[k].varianceScale());
                        attributeSampleData.s_Times.push_back(samples[k].time());
                        attributeSampleData.s_Samples.push_back(samples[k].value(dimension));
                        attributeSampleData.s_Weights.push_back(weights);
                    }
                }
                attributeSampleData.s_IsInteger &= dj.s_IsInteger;
            }
        }
    }

    this->updatePriors(WEIGHT_STYLES, sampleData, m_AttributePriors, m_AttributeMultivariatePriors);
}

void CMetricPopulationModel::prune(std::size_t maximumAge)
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

    this->clearPrunedResources(peopleToRemove, attributesToRemove);
    this->removePeople(peopleToRemove);
}

bool CMetricPopulationModel::computeProbability(std::size_t pid,
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

    typedef boost::shared_ptr<const std::string> TStrPtr;
    typedef core::CSmallVector<TStrPtr, 1> TStrPtr1Vec;
    typedef CProbabilityAndInfluenceCalculator::SParams TParams;
    typedef CProbabilityAndInfluenceCalculator::SMultivariateParams TMultivariateParams;

    partitioningFields.add(gatherer.attributeFieldName(), EMPTY_STRING);

    CAnnotatedProbabilityBuilder resultBuilder(result,
                                               std::max(numberAttributeProbabilities, std::size_t(1)),
                                               function_t::function(gatherer.features()),
                                               gatherer.numberActivePeople());

    LOG_TRACE("computeProbability(" << gatherer.personName(pid) << ")");

    CProbabilityAndInfluenceCalculator pJoint(this->params().s_InfluenceCutoff);
    pJoint.addAggregator(maths::CJointProbabilityOfLessLikelySamples());
    pJoint.addAggregator(maths::CProbabilityOfExtremeSample());

    // Declared outside loop to minimize number of times they are created.
    static const TStrPtr1Vec NO_CORRELATED_ATTRIBUTES;
    static const TSizeDoublePr1Vec NO_CORRELATES;
    TParams params(WEIGHT_STYLES, partitioningFields);
    TMultivariateParams multivariateParams(WEIGHT_STYLES, partitioningFields);
    params.s_Weights.resize(1, TDouble4Vec(WEIGHT_STYLES.size()));
    multivariateParams.s_Sample.resize(1);
    multivariateParams.s_Weights.resize(1, TDouble10Vec4Vec(WEIGHT_STYLES.size()));

    for (std::size_t i = 0u; i < gatherer.numberFeatures(); ++i)
    {
        model_t::EFeature feature = gatherer.feature(i);
        if (model_t::isCategorical(feature))
        {
            continue;
        }
        LOG_TRACE("feature = " << model_t::print(feature));

        const TSizeSizePrFeatureDataPrVec &featureData = this->featureData(feature, startTime);
        TSizeSizePr range = CModelTools::personRange(featureData, pid);

        for (std::size_t j = range.first; j < range.second; ++j)
        {
            // 1) Sample the person's feature for the bucket.
            // 2) Compute the probability of the sample for the
            //    population model of the corresponding attribute.
            // 3) Update the person's probabilities.

            std::size_t cid = CDataGatherer::extractAttributeId(featureData[j]);
            if (cid >= this->attributeFirstBucketTimes().size())
            {
                LOG_TRACE("No first time for attribute = " << gatherer.attributeName(cid)
                          << " and feature = " << model_t::print(feature));
                continue;
            }

            partitioningFields.back().second = TStrCRef(gatherer.attributeName(cid));
            const TFeatureData &dj = CDataGatherer::extractData(featureData[j]);
            const TOptionalSample &bucket = dj.s_BucketValue;
            if (!bucket)
            {
                LOG_ERROR("Expected a value for feature = " << model_t::print(feature)
                          << ", person = " << gatherer.personName(pid)
                          << ", attribute = " << gatherer.attributeName(cid));
                continue;
            }

            core_t::TTime sampleTime = model_t::sampleTime(feature, startTime, bucketLength, bucket->time());
            core_t::TTime elapsedTime = sampleTime - this->attributeFirstBucketTimes()[cid];

            if (this->shouldIgnoreResult(feature,
                                         result.s_ResultType,
                                         partitioningFields.partitionFieldValue(),
                                         pid,
                                         cid,
                                         sampleTime))
            {
                continue;
            }

#define ADD_PROBABILITY_AND_INFLUENCE(params)                                            \
            if (!pJoint.addProbability(feature, *params.s_Prior, elapsedTime,            \
                                       WEIGHT_STYLES, params.s_Sample, params.s_Weights, \
                                       params.s_BucketEmpty, params.s_ProbabilityBucketEmpty, \
                                       params.s_Probability, params.s_Tail))             \
            {                                                                            \
                LOG_ERROR("Failed to compute P(" << params.describe()                    \
                          << ", attribute = "<< gatherer.attributeName(cid)              \
                          << ", person = " << this->personName(pid) << ")");             \
                continue;                                                                \
            }                                                                            \
            else                                                                         \
            {                                                                            \
                LOG_TRACE("P(" << params.describe()                                      \
                          << ", attribute = "<< gatherer.attributeName(cid)              \
                          << ", person = " << this->personName(pid) << ") = " << params.s_Probability); \
            }                                                                            \
            if (!dj.s_InfluenceValues.empty())                                           \
            {                                                                            \
                for (std::size_t k = 0u; k < dj.s_InfluenceValues.size(); ++k)           \
                {                                                                        \
                    if (const CInfluenceCalculator *influenceCalculator = this->influenceCalculator(feature, k)) \
                    {                                                                    \
                        pJoint.plugin(*influenceCalculator);                             \
                        pJoint.addInfluences(*(gatherer.beginInfluencers() + k), dj.s_InfluenceValues[k], params); \
                    }                                                                    \
                }                                                                        \
            }                                                                            \
            resultBuilder.addAttributeProbability(cid, gatherer.attributeNamePtr(cid),   \
                                                  1.0, params.s_Probability,             \
                                                  model_t::CResultType::E_Unconditional, \
                                                  feature,                               \
                                                  NO_CORRELATED_ATTRIBUTES, NO_CORRELATES)


            std::size_t dimension = model_t::dimension(feature);
            if (dimension == 1)
            {
                const maths::CPrior *prior = this->prior(feature, cid);
                if (!prior)
                {
                    LOG_ERROR("No prior for " << this->attributeName(cid)
                              << " and feature " << model_t::print(feature));
                    return false;
                }

                params.s_Feature = feature;
                params.s_Trend = this->trend(feature, cid);
                params.s_Prior = prior;
                params.s_ElapsedTime = elapsedTime;
                params.s_Time  = sampleTime;
                params.s_Value = bucket->value();
                params.s_Count = bucket->count();
                params.s_Sample = this->detrend(feature, cid, sampleTime,
                                                SEASONAL_CONFIDENCE_INTERVAL,
                                                bucket->value(1));
                params.s_Weights[0][0] = this->seasonalVarianceScale(feature, cid, sampleTime,
                                                                     SEASONAL_CONFIDENCE_INTERVAL).second[0];
                params.s_Weights[0][1] = bucket->varianceScale();
                if (result.isInterim() && model_t::requiresInterimResultAdjustment(feature))
                {
                    double mode = prior->marginalLikelihoodMode(WEIGHT_STYLES, params.s_Weights[0]);
                    TDouble1Vec corrections(1, this->interimValueCorrector().corrections(
                                                             sampleTime,
                                                             this->currentBucketTotalCount(),
                                                             mode, params.s_Sample[0]));
                    params.s_Value  += corrections;
                    params.s_Sample += corrections;
                    this->currentBucketInterimCorrections().emplace(core::make_triple(feature, pid, cid), corrections);
                }

                params.s_Probability = 1.0;
                params.s_Tail = maths_t::E_UndeterminedTail;
                ADD_PROBABILITY_AND_INFLUENCE(params);
            }
            else
            {
                const maths::CMultivariatePrior *prior = this->multivariatePrior(feature, cid);
                if (!prior)
                {
                    LOG_ERROR("No prior for " << this->attributeName(cid)
                              << " and feature " << model_t::print(feature));
                    return false;
                }

                multivariateParams.s_Feature = feature;
                multivariateParams.s_Trend = this->trend(feature, cid);
                multivariateParams.s_Prior = prior;
                multivariateParams.s_ElapsedTime = elapsedTime;
                multivariateParams.s_Time  = sampleTime;
                multivariateParams.s_Value = bucket->value();
                multivariateParams.s_Count = bucket->count();
                multivariateParams.s_Sample[0] = this->detrend(feature, cid, sampleTime,
                                                               SEASONAL_CONFIDENCE_INTERVAL,
                                                               bucket->value(dimension));
                multivariateParams.s_Weights[0][0] = this->seasonalVarianceScale(feature, cid, sampleTime,
                                                                                 SEASONAL_CONFIDENCE_INTERVAL).second;
                multivariateParams.s_Weights[0][1].assign(dimension, bucket->varianceScale());
                if (result.isInterim() && model_t::requiresInterimResultAdjustment(feature))
                {
                    TDouble10Vec modes = prior->marginalLikelihoodMode(WEIGHT_STYLES, multivariateParams.s_Weights[0]);
                    TDouble10Vec corrections = this->interimValueCorrector().corrections(
                                                             sampleTime,
                                                             this->currentBucketTotalCount(),
                                                             modes, multivariateParams.s_Sample[0]);
                    multivariateParams.s_Value     += corrections;
                    multivariateParams.s_Sample[0] += corrections;
                    this->currentBucketInterimCorrections().emplace(core::make_triple(feature, pid, cid), corrections);
                }

                multivariateParams.s_Probability = 1.0;
                multivariateParams.s_Tail.assign(dimension, maths_t::E_UndeterminedTail);
                ADD_PROBABILITY_AND_INFLUENCE(multivariateParams);
            }

#undef ADD_PROBABILITY_AND_INFLUENCE
        }
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

bool CMetricPopulationModel::computeTotalProbability(const std::string &/*person*/,
                                                     std::size_t /*numberAttributeProbabilities*/,
                                                     TOptionalDouble &probability,
                                                     TAttributeProbability1Vec &attributeProbabilities) const
{
    probability = TOptionalDouble();
    attributeProbabilities.clear();
    return true;
}

void CMetricPopulationModel::outputCurrentBucketStatistics(const std::string &partitionFieldValue,
                                                           const TBucketStatsOutputFunc &outputFunc) const
{
    const CDataGatherer &gatherer = this->dataGatherer();
    const std::string &partitionFieldName = gatherer.partitionFieldName();
    const std::string &personFieldName = gatherer.personFieldName();
    const std::string &attributeFieldName = gatherer.attributeFieldName();
    const std::string &valueFieldName = gatherer.valueFieldName();

    const TFeatureSizeSizePrFeatureDataPrVecMap &featureData = m_CurrentBucketStats.s_FeatureData;
    for (TFeatureSizeSizePrFeatureDataPrVecMapCItr i = featureData.begin();
         i != featureData.end();
         ++i)
    {
        model_t::EFeature feature = i->first;
        std::size_t dimension = model_t::dimension(feature);
        const std::string &funcName = model_t::outputFunctionName(feature);
        const TSizeSizePrFeatureDataPrVec &data = i->second;
        for (std::size_t j = 0u; j < data.size(); ++j)
        {
            const TOptionalSample &value = CDataGatherer::extractData(data[j]).s_BucketValue;
            if (value)
            {
                outputFunc(
                    SOutputStats(
                        m_CurrentBucketStats.s_StartTime,
                        true,
                        false,
                        partitionFieldName,
                        partitionFieldValue,
                        personFieldName,
                        gatherer.personName(CDataGatherer::extractPersonId(data[j]), EMPTY_STRING),
                        attributeFieldName,
                        gatherer.attributeName(CDataGatherer::extractAttributeId(data[j]), EMPTY_STRING),
                        valueFieldName,
                        funcName,
                        (value->value(dimension))[0], // FIXME output vector value.
                        data[j].second.s_IsInteger
                    )
                );
            }
        }
    }
}

uint64_t CMetricPopulationModel::checksum(bool includeCurrentBucketStats) const
{
    uint64_t seed = this->CPopulationModel::checksum(includeCurrentBucketStats);
    if (includeCurrentBucketStats)
    {
        seed = maths::CChecksum::calculate(seed, m_CurrentBucketStats.s_StartTime);
    }

    TStrCRefStrCRefPrUInt64Map hashes;
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
                const TFeatureData &data = CDataGatherer::extractData(itr->second[i]);
                hash = maths::CChecksum::calculate(hash, data.s_BucketValue);
                hash = maths::CChecksum::calculate(hash, data.s_IsInteger);
                hash = maths::CChecksum::calculate(hash, data.s_Samples);
            }
        }
    }

    LOG_TRACE("seed = " << seed);
    LOG_TRACE("hashes = " << core::CContainerPrinter::print(hashes));

    return maths::CChecksum::calculate(seed, hashes);
}

void CMetricPopulationModel::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CMetricPopulationModel");
    this->CPopulationModel::debugMemoryUsage(mem->addChild());
    core::CMemoryDebug::dynamicSize("m_CurrentBucketStats.s_PersonCounts",
                                    m_CurrentBucketStats.s_PersonCounts, mem);
    core::CMemoryDebug::dynamicSize("m_CurrentBucketStats.s_FeatureData",
                                    m_CurrentBucketStats.s_FeatureData, mem);
    core::CMemoryDebug::dynamicSize("m_CurrentBucketStats.s_InterimCorrections",
                                    m_CurrentBucketStats.s_InterimCorrections, mem);
    core::CMemoryDebug::dynamicSize("m_AttributePriors", m_AttributePriors, mem);
    core::CMemoryDebug::dynamicSize("m_AttributeMultivariatePriors",
                                    m_AttributeMultivariatePriors, mem);
    core::CMemoryDebug::dynamicSize("m_MemoryEstimator", m_MemoryEstimator, mem);
}

std::size_t CMetricPopulationModel::memoryUsage(void) const
{
    const CDataGatherer &gatherer = this->dataGatherer();
    return this->estimateMemoryUsage(gatherer.numberActivePeople(),
                                     gatherer.numberActiveAttributes(),
                                     0); // # correlations
}

std::size_t CMetricPopulationModel::computeMemoryUsage(void) const
{
    std::size_t mem = this->CPopulationModel::memoryUsage();
    mem += core::CMemory::dynamicSize(m_CurrentBucketStats.s_PersonCounts);
    mem += core::CMemory::dynamicSize(m_CurrentBucketStats.s_FeatureData);
    mem += core::CMemory::dynamicSize(m_CurrentBucketStats.s_InterimCorrections);
    mem += core::CMemory::dynamicSize(m_AttributePriors);
    mem += core::CMemory::dynamicSize(m_AttributeMultivariatePriors);
    mem += core::CMemory::dynamicSize(m_MemoryEstimator);
    return mem;
}

std::size_t CMetricPopulationModel::estimateMemoryUsage(std::size_t numberPeople,
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

std::size_t CMetricPopulationModel::staticSize(void) const
{
    return sizeof(*this);
}

CMetricPopulationModel::CModelDetailsViewPtr
    CMetricPopulationModel::details(void) const
{
    return CModelDetailsViewPtr(new CMetricPopulationModelDetailsView(*this));
}

const TSizeSizePrFeatureDataPrVec &
    CMetricPopulationModel::featureData(model_t::EFeature feature, core_t::TTime time) const
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

core_t::TTime CMetricPopulationModel::currentBucketStartTime(void) const
{
    return m_CurrentBucketStats.s_StartTime;
}

void CMetricPopulationModel::currentBucketStartTime(core_t::TTime startTime)
{
    m_CurrentBucketStats.s_StartTime = startTime;
}

uint64_t CMetricPopulationModel::currentBucketTotalCount(void) const
{
    return m_CurrentBucketStats.s_TotalCount;
}

CPopulationModel::TFeatureSizeSizeTripleDouble1VecUMap &
CMetricPopulationModel::currentBucketInterimCorrections(void) const
{
    return m_CurrentBucketStats.s_InterimCorrections;
}

void CMetricPopulationModel::createNewModels(std::size_t n, std::size_t m)
{
    if (m > 0)
    {
        CModelTools::createPriors(m, this->newPriors(), m_AttributePriors);
        CModelTools::createPriors(m, this->newMultivariatePriors(), m_AttributeMultivariatePriors);
    }
    this->CPopulationModel::createNewModels(n, m);
}

void CMetricPopulationModel::updateRecycledModels(void)
{
    CDataGatherer &gatherer = this->dataGatherer();
    const TSizeVec &recycledAttributes = gatherer.recycledAttributeIds();
    this->reinitializeAttributePriors(recycledAttributes,
                                      m_AttributePriors,
                                      m_AttributeMultivariatePriors);
    this->CPopulationModel::updateRecycledModels();
}

void CMetricPopulationModel::clearPrunedResources(const TSizeVec &people,
                                                  const TSizeVec &attributes)
{
    this->clearAttributePriors(attributes,
                               m_AttributePriors,
                               m_AttributeMultivariatePriors);
    this->CPopulationModel::clearPrunedResources(people, attributes);
}

const maths::CPrior *CMetricPopulationModel::prior(model_t::EFeature feature,
                                                   std::size_t cid) const
{
    const TPriorPtrVec &priors = this->priors(feature);
    return cid < priors.size() ? priors[cid].get() : 0;
}

const maths::CMultivariatePrior *
    CMetricPopulationModel::multivariatePrior(model_t::EFeature feature, std::size_t cid) const
{
    const TMultivariatePriorPtrVec &multivariatePriors = this->multivariatePriors(feature);
    return cid < multivariatePriors.size() ? multivariatePriors[cid].get() : 0;
}

bool CMetricPopulationModel::resetPrior(model_t::EFeature feature, std::size_t cid)
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

const CMetricPopulationModel::TPriorPtrVec &
    CMetricPopulationModel::priors(model_t::EFeature feature) const
{
    static const TPriorPtrVec EMPTY;
    TFeaturePriorPtrVecMapCItr result = m_AttributePriors.find(feature);
    return result == m_AttributePriors.end() ? EMPTY : result->second;
}

const CMetricPopulationModel::TMultivariatePriorPtrVec &
    CMetricPopulationModel::multivariatePriors(model_t::EFeature feature) const
{
    static const TMultivariatePriorPtrVec EMPTY;
    TFeatureMultivariatePriorPtrVecMapCItr result = m_AttributeMultivariatePriors.find(feature);
    return result == m_AttributeMultivariatePriors.end() ? EMPTY : result->second;
}

const CMetricPopulationModel::TSizeUInt64PrVec &
    CMetricPopulationModel::personCounts(void) const
{
    return m_CurrentBucketStats.s_PersonCounts;
}

void CMetricPopulationModel::currentBucketTotalCount(uint64_t totalCount)
{
    m_CurrentBucketStats.s_TotalCount = totalCount;
}

bool CMetricPopulationModel::bucketStatsAvailable(core_t::TTime time) const
{
    return    time >= m_CurrentBucketStats.s_StartTime
           && time < m_CurrentBucketStats.s_StartTime + this->bucketLength();
}

////////// CMetricPopulationModel::SBucketStats Implementation //////////

CMetricPopulationModel::SBucketStats::SBucketStats(core_t::TTime startTime) :
        s_StartTime(startTime),
        s_PersonCounts(),
        s_TotalCount(0),
        s_InterimCorrections(1)
{
}


}
}
