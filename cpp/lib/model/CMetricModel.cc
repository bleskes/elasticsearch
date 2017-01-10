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

#include <model/CMetricModel.h>

#include <core/CContainerPrinter.h>
#include <core/CFunctional.h>
#include <core/CLogger.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>
#include <core/CStatistics.h>
#include <core/CoreTypes.h>

#include <maths/CChecksum.h>
#include <maths/CMultivariatePrior.h>
#include <maths/COrderings.h>
#include <maths/CPrior.h>
#include <maths/CTools.h>
#include <maths/ProbabilityAggregators.h>

#include <model/CAnnotatedProbabilityBuilder.h>
#include <model/CDataGatherer.h>
#include <model/CGathererTools.h>
#include <model/CIndividualModelDetail.h>
#include <model/CInterimBucketCorrector.h>
#include <model/CModelDetailsView.h>
#include <model/CModelTools.h>
#include <model/CProbabilityAndInfluenceCalculator.h>
#include <model/CSampleGatherer.h>
#include <model/CResourceMonitor.h>
#include <model/FrequencyPredicates.h>

#include <boost/bind.hpp>
#include <boost/iterator/counting_iterator.hpp>
#include <boost/ref.hpp>

#include <algorithm>
#include <string>
#include <utility>
#include <vector>

namespace ml
{
namespace model
{

namespace
{

typedef core::CSmallVector<core_t::TTime, 1> TTime1Vec;
typedef core::CSmallVector<double, 1> TDouble1Vec;
typedef core::CSmallVector<double, 4> TDouble4Vec;
typedef core::CSmallVector<TDouble4Vec, 1> TDouble4Vec1Vec;
typedef maths::CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;
typedef core::CSmallVector<TMeanAccumulator, 1> TMeanAccumulator1Vec;

//! \brief Wraps up the sampled data for a feature.
struct SFeatureSampleData
{
    void fill(bool isInteger,
              const TTime1Vec &times,
              const TDouble1Vec &detrendedSamples,
              const TDouble4Vec1Vec &weights,
              double interval,
              double multiplier)
    {
        s_IsInteger = isInteger;
        s_Times = times;
        s_DetrendedSamples = detrendedSamples;
        s_Weights = weights;
        s_Interval = interval;
        s_Multiplier = multiplier;
    }

    bool s_IsInteger;
    TTime1Vec s_Times;
    TDouble1Vec s_DetrendedSamples;
    TDouble4Vec1Vec s_Weights;
    double s_Interval;
    double s_Multiplier;
};

//! Extract the means from \p moments.
void means(const TMeanAccumulator1Vec &moments, TDouble1Vec &result)
{
    result.resize(moments.size());
    for (std::size_t i = 0u; i < moments.size(); ++i)
    {
        result[i] = maths::CBasicStatistics::mean(moments[i]);
    }
}

// We obfuscate the XML element names to avoid giving away too much
// information about our model.
const std::string INDIVIDUAL_STATE_TAG("a");

static const maths_t::ESampleWeightStyle SAMPLE_WEIGHT_STYLES_[] =
    {
        maths_t::E_SampleCountWeight,
        maths_t::E_SampleWinsorisationWeight,
        maths_t::E_SampleCountVarianceScaleWeight
    };
const maths_t::TWeightStyleVec SAMPLE_WEIGHT_STYLES(boost::begin(SAMPLE_WEIGHT_STYLES_),
                                                    boost::end(SAMPLE_WEIGHT_STYLES_));
const maths_t::ESampleWeightStyle PROBABILITY_WEIGHT_STYLES_[] =
    {
        maths_t::E_SampleSeasonalVarianceScaleWeight,
        maths_t::E_SampleCountVarianceScaleWeight
    };
const maths_t::TWeightStyleVec PROBABILITY_WEIGHT_STYLES(boost::begin(PROBABILITY_WEIGHT_STYLES_),
                                                         boost::end(PROBABILITY_WEIGHT_STYLES_));

}

CMetricModel::CMetricModel(const SModelParams &params,
                           const TDataGathererPtr &dataGatherer,
                           const TFeaturePriorPtrPrVec &newPriors,
                           const TFeatureMultivariatePriorPtrPrVec &newMultivariatePriors,
                           const TFeatureMultivariatePriorPtrPrVec &newCorrelatePriors,
                           const TFeatureDecompositionCPtrVecPrVec &newDecompositions,
                           const TFeatureInfluenceCalculatorCPtrPrVecVec &influenceCalculators) :
        CIndividualModel(params,
                         dataGatherer,
                         newPriors,
                         newMultivariatePriors,
                         newCorrelatePriors,
                         influenceCalculators,
                         newDecompositions,
                         false),
        m_CurrentBucketStats(CModel::TIME_UNSET)
{}

CMetricModel::CMetricModel(const SModelParams &params,
                           const TDataGathererPtr &dataGatherer,
                           const TFeaturePriorPtrPrVec &newPriors,
                           const TFeatureMultivariatePriorPtrPrVec &newMultivariatePriors,
                           const TFeatureMultivariatePriorPtrPrVec &newCorrelatePriors,
                           const TFeatureDecompositionCPtrVecPrVec &newDecompositions,
                           const TFeatureInfluenceCalculatorCPtrPrVecVec &influenceCalculators,
                           core::CStateRestoreTraverser &traverser) :
        CIndividualModel(params,
                         dataGatherer,
                         newPriors,
                         newMultivariatePriors,
                         newCorrelatePriors,
                         influenceCalculators,
                         newDecompositions,
                         true),
        m_CurrentBucketStats(CModel::TIME_UNSET)
{
    traverser.traverseSubLevel(boost::bind(&CMetricModel::acceptRestoreTraverser,
                                           this, boost::ref(params.s_ExtraDataRestoreFunc), _1));
}

CMetricModel::CMetricModel(bool isForPersistence, const CMetricModel &other) :
        CIndividualModel(isForPersistence, other),
        m_CurrentBucketStats(0) // Not needed for persistence so minimally constructed
{
    if (!isForPersistence)
    {
        LOG_ABORT("This constructor only creates clones for persistence");
    }
}

void CMetricModel::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertLevel(INDIVIDUAL_STATE_TAG, boost::bind(&CMetricModel::doAcceptPersistInserter, this, _1));
}

bool CMetricModel::acceptRestoreTraverser(const model_t::TAnyRestoreFunc &extraDataRestoreFunc,
                                          core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        if (name == INDIVIDUAL_STATE_TAG)
        {
            if (traverser.traverseSubLevel(boost::bind(&CMetricModel::doAcceptRestoreTraverser,
                                                       this,
                                                       boost::cref(extraDataRestoreFunc),
                                                       _1)) == false)
            {
                // Logging handled already.
                return false;
            }
        }
    }
    while (traverser.next());

    return true;
}

CModel *CMetricModel::cloneForPersistence(void) const
{
    return new CMetricModel(true, *this);
}

model_t::EModelType CMetricModel::category(void) const
{
    return model_t::E_MetricOnline;
}

bool CMetricModel::isEventRate(void) const
{
    return false;
}

bool CMetricModel::isMetric(void) const
{
    return true;
}

void CMetricModel::currentBucketPersonIds(core_t::TTime time, TSizeVec &result) const
{
    this->CIndividualModel::currentBucketPersonIds(time, m_CurrentBucketStats.s_FeatureData, result);
}

CMetricModel::TOptionalDouble
    CMetricModel::baselineBucketCount(const std::size_t /*pid*/) const
{
    return TOptionalDouble();
}

CMetricModel::TDouble1Vec CMetricModel::currentBucketValue(model_t::EFeature feature,
                                                           std::size_t pid,
                                                           std::size_t /*cid*/,
                                                           core_t::TTime time) const
{
    const TFeatureData *data = this->featureData(feature, pid, time);
    if (data)
    {
        const TOptionalSample &value = data->s_BucketValue;
        return value ? value->value(model_t::dimension(feature)) : TDouble1Vec();
    }
    return TDouble1Vec();
}

CMetricModel::TDouble1Vec CMetricModel::baselineBucketMean(model_t::EFeature feature,
                                                           std::size_t pid,
                                                           std::size_t /*cid*/,
                                                           model_t::CResultType type,
                                                           const TSizeDoublePr1Vec &correlated,
                                                           core_t::TTime time) const
{
    typedef core::CSmallVector<std::size_t, 10> TSize10Vec;
    typedef core::CSmallVector<TSizeDoublePr, 10> TSizeDoublePr10Vec;

    double scale = 1.0 - this->probabilityBucketEmpty(feature, pid);
    TDecompositionCPtr1Vec trend = this->trend(feature, pid);
    double correlateCorrection = this->correctBaselineForCorrelated(feature, pid, type, correlated);

    TDouble1Vec result;
    if (model_t::dimension(feature) == 1)
    {
        const maths::CPrior *prior = this->prior(feature, pid);
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
        result.assign(1, scale * (seasonalOffset + median) + correlateCorrection);
    }
    else
    {
        const maths::CMultivariatePrior *prior = this->multivariatePrior(feature, pid);
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
            result.push_back(scale * (seasonalOffset + median) + correlateCorrection);
        }
    }

    this->correctBaselineForInterim(feature, pid, type, correlated,
                                    this->currentBucketInterimCorrections(), result);

    TDouble1VecDouble1VecPr support = model_t::support(feature);
    return maths::CTools::truncate(result, support.first, support.second);
}

void CMetricModel::outputCurrentBucketStatistics(const std::string &partitionFieldValue,
                                                 const TBucketStatsOutputFunc &outputFunc) const
{
    const CDataGatherer &gatherer = this->dataGatherer();
    const std::string &partitionFieldName = gatherer.partitionFieldName();
    const std::string &personFieldName = gatherer.personFieldName();
    const std::string &valueFieldName = gatherer.valueFieldName();

    const TFeatureSizeFeatureDataPrVecPrVec &featureData = m_CurrentBucketStats.s_FeatureData;
    for (std::size_t i = 0u; i < featureData.size(); ++i)
    {
        model_t::EFeature feature = featureData[i].first;
        std::size_t dimension = model_t::dimension(feature);
        const std::string &funcName = model_t::outputFunctionName(feature);
        const TSizeFeatureDataPrVec &data = featureData[i].second;
        for (std::size_t j = 0u; j < data.size(); ++j)
        {
            const TOptionalSample &value = data[j].second.s_BucketValue;
            if (value)
            {
                outputFunc(
                    SOutputStats(
                        m_CurrentBucketStats.s_StartTime,
                        false,
                        false,
                        partitionFieldName,
                        partitionFieldValue,
                        EMPTY_STRING,
                        EMPTY_STRING,
                        personFieldName,
                        gatherer.personName(data[j].first, EMPTY_STRING),
                        valueFieldName,
                        funcName,
                        value->value(dimension)[0], // FIXME output vector value.
                        data[j].second.s_IsInteger
                    )
                );
            }
        }
    }
}

void CMetricModel::sampleBucketStatistics(core_t::TTime startTime,
                                          core_t::TTime endTime,
                                          CResourceMonitor &resourceMonitor)
{
    m_CurrentBucketStats.s_InterimCorrections.clear();
    this->CIndividualModel::sampleBucketStatistics(startTime, endTime,
                                                   this->personFilter(),
                                                   m_CurrentBucketStats.s_FeatureData,
                                                   resourceMonitor);
}

void CMetricModel::sample(core_t::TTime startTime,
                          core_t::TTime endTime,
                          CResourceMonitor &resourceMonitor)
{
    CDataGatherer &gatherer = this->dataGatherer();
    core_t::TTime bucketLength = gatherer.bucketLength();

    if (!gatherer.validateSampleTimes(startTime, endTime))
    {
        return;
    }

    typedef boost::unordered_map<std::size_t, SFeatureSampleData> TSizeFeatureSampleDataUMap;
    typedef TSizeFeatureSampleDataUMap::iterator TSizeFeatureSampleDataUMapItr;

    this->createUpdateNewModels(startTime, resourceMonitor);
    m_CurrentBucketStats.s_InterimCorrections.clear();

    static const std::size_t N = SAMPLE_WEIGHT_STYLES.size();

    // Declared outside loop to minimize number of times they are created.
    TTime1Vec times;
    TDouble1Vec4Vec trendWeight(N, TDouble1Vec(1));
    TDouble1Vec detrendedSamples;
    TDouble4Vec1Vec weights;
    TDouble4Vec1Vec seasonalWeight(1);
    TDouble10Vec1Vec detrendedMultivariateSamples;
    TDouble10Vec4Vec1Vec multivariateWeights;
    TDouble10Vec4Vec multivariateSeasonalWeight(1);
    TDouble1Vec prediction;
    TMeanAccumulator1Vec residuals[2];
    TDouble1Vec residual;
    TSizeFeatureSampleDataUMap sampleData;

    for (core_t::TTime time = startTime; time < endTime; time += bucketLength)
    {
        LOG_TRACE("Sampling [" << time << "," << time + bucketLength << ")");

        gatherer.sampleNow(time);
        this->CIndividualModel::sample(time, time + bucketLength, resourceMonitor);

        TFeatureSizeFeatureDataPrVecPrVec &featureData = m_CurrentBucketStats.s_FeatureData;
        gatherer.featureData(time, bucketLength, featureData);

        for (std::size_t i = 0u; i < featureData.size(); ++i)
        {
            model_t::EFeature feature = featureData[i].first;
            TSizeFeatureDataPrVec &data = featureData[i].second;
            LOG_TRACE(model_t::print(feature) << " data = " << core::CContainerPrinter::print(data));
            this->applyFilter(model_t::E_XF_By, true, this->personFilter(), data);

            TSizeSizePrMultivariatePriorPtrDoublePrUMap *correlationPriors = this->correlatePriors(feature);
            maths::CKMostCorrelated *correlations = this->correlations(feature);
            sampleData.clear();

            for (std::size_t j = 0u; j < data.size(); ++j)
            {
                std::size_t pid = data[j].first;
                const TFeatureData &dj = data[j].second;

                const CGathererTools::TSampleVec &samples = dj.s_Samples;
                const TOptionalSample &bucketValue = dj.s_BucketValue;
                if (samples.empty() && !(model_t::isSampled(feature) && bucketValue))
                {
                    continue;
                }

                if (!this->hasPrior(feature, pid))
                {
                    LOG_ERROR("Missing prior for " << this->personName(pid));
                    continue;
                }

                double emptyBucketWeight = this->emptyBucketWeight(feature, pid, time);
                if (emptyBucketWeight == 0.0)
                {
                    continue;
                }

                bool isInteger = dj.s_IsInteger;
                maths_t::EDataType type = isInteger ? maths_t::E_IntegerData : maths_t::E_ContinuousData;
                double derate = this->derate(pid, model_t::sampleTime(feature, time, bucketLength));
                double interval = (1.0 + (this->params().s_InitialDecayRateMultiplier - 1.0) * derate) * emptyBucketWeight;
                double count = this->params().s_MaximumUpdatesPerBucket > 0.0 ?
                               this->params().s_MaximumUpdatesPerBucket / static_cast<double>(samples.size()) : 1.0;
                std::size_t dimension = model_t::dimension(feature);

                residuals[0].clear();
                residuals[1].clear();
                residuals[0].resize(dimension);
                residuals[1].resize(dimension);

                LOG_TRACE("Bucket = " << gatherer.printCurrentBucket(time)
                          << ", feature = " << model_t::print(feature)
                          << ", samples = " << core::CContainerPrinter::print(samples)
                          << ", isInteger = " << isInteger
                          << ", person = " << this->personName(pid)
                          << ", count weight = " << count
                          << ", dimension = " << dimension
                          << ", empty bucket weight = " << emptyBucketWeight);

                if (dimension == 1)
                {
                    maths::CPrior *prior = this->prior(feature, pid);
                    const TDecompositionPtr1Vec &trend = this->trend(feature, pid);
                    if (bucketValue)
                    {
                        prior->adjustOffset(maths::CConstantWeights::COUNT,
                                            this->detrend(feature, pid, bucketValue->time(), 0.0, bucketValue->value(1)),
                                            maths::CConstantWeights::SINGLE_UNIT);
                    }
                    if (samples.empty())
                    {
                        continue;
                    }

                    prior->dataType(type);

                    times.resize(samples.size());
                    detrendedSamples.resize(samples.size());
                    weights.resize(samples.size(), TDouble4Vec(N, 1.0));
                    SFeatureSampleData *personSampleData = correlationPriors ?
                            &sampleData.insert(std::make_pair(pid, SFeatureSampleData())).first->second : 0;

                    for (std::size_t k = 0u; k < samples.size(); ++k)
                    {
                        core_t::TTime t = model_t::sampleTime(feature, time, bucketLength, samples[k].time());
                        times[k] = t;

                        double vs = samples[k].varianceScale();
                        seasonalWeight[0] = maths::CBasicStatistics::mean(
                                                this->seasonalVarianceScale(feature, pid, t, 0.0));
                        trendWeight[0][0] = emptyBucketWeight * count * this->learnRate(feature) / vs;
                        trendWeight[1][0] = winsorisingWeight(*prior,
                                                              maths::CConstantWeights::SEASONAL_VARIANCE,
                                                              this->detrend(feature, pid, t, 0.0, samples[k].value(1)),
                                                              seasonalWeight, derate);
                        trendWeight[2][0] = vs;
                        detrendedSamples[k] = this->updateTrend(feature, pid, t,
                                                                samples[k].value(1),
                                                                SAMPLE_WEIGHT_STYLES, trendWeight)[0];
                        weights[k][0] = emptyBucketWeight * count * this->learnRate(feature);
                        weights[k][1] = trendWeight[1][0];
                        weights[k][2] = vs;

                        TDouble1Vec sample(1, detrendedSamples[k]);
                        this->residuals(interval, trend, *prior, sample, residuals);
                    }

                    maths::COrderings::simultaneousSort(detrendedSamples, weights);
                    CModelTools::updatePrior(SAMPLE_WEIGHT_STYLES, detrendedSamples, weights, interval, *prior);
                    if (correlations)
                    {
                        correlations->add(pid, detrendedSamples[detrendedSamples.size() / 2]);
                    }

                    double multiplier = 1.0;
                    if (maths::CBasicStatistics::count(residuals[0][0]) > 0.0)
                    {
                        prediction.assign(1, prior->marginalLikelihoodMean());
                        residual.assign(1, maths::CBasicStatistics::mean(residuals[0][0]));
                        multiplier = this->decayRateMultiplier(E_PriorControl,
                                                               feature, pid,
                                                               prediction, residual);
                        prior->decayRate(multiplier * prior->decayRate());
                        LOG_TRACE("prior decay rate = " << prior->decayRate());
                    }
                    if (maths::CBasicStatistics::count(residuals[1][0]) > 0.0)
                    {
                        prediction.assign(1, trend[0]->mean());
                        residual.assign(1, maths::CBasicStatistics::mean(residuals[1][0]));
                        trend[0]->decayRate(this->decayRateMultiplier(E_TrendControl,
                                                                      feature, pid,
                                                                      prediction, residual)
                                            * trend[0]->decayRate());
                        LOG_TRACE("trend decay rate = " << prior->decayRate());
                    }
                    if (personSampleData)
                    {
                        personSampleData->fill(isInteger, times, detrendedSamples, weights, interval, multiplier);
                    }

                    LOG_TRACE(this->personName(pid) << " prior:" << core_t::LINE_ENDING << prior->print());
                }
                else if (!samples.empty())
                {
                    maths::CMultivariatePrior *multivariatePrior = this->multivariatePrior(feature, pid);
                    multivariatePrior->dataType(type);
                    const TDecompositionPtr1Vec &trend = this->trend(feature, pid);

                    detrendedMultivariateSamples.resize(samples.size());
                    multivariateWeights.resize(samples.size(), TDouble10Vec4Vec(N));

                    for (std::size_t k = 0u; k < samples.size(); ++k)
                    {
                        core_t::TTime t = model_t::sampleTime(feature, time, bucketLength, samples[k].time());

                        double vs = samples[k].varianceScale();
                        multivariateSeasonalWeight[0] = maths::CBasicStatistics::mean(
                                                            this->seasonalVarianceScale(feature, pid, t, 0.0));
                        trendWeight[0].assign(dimension, emptyBucketWeight * count * this->learnRate(feature) / vs);
                        trendWeight[1] = winsorisingWeight(*multivariatePrior,
                                                           maths::CConstantWeights::SEASONAL_VARIANCE,
                                                           this->detrend(feature, pid, t, 0.0, samples[k].value(dimension)),
                                                           multivariateSeasonalWeight,
                                                           derate);
                        trendWeight[2].assign(dimension, vs);
                        detrendedMultivariateSamples[k] = this->updateTrend(feature, pid, t,
                                                                            samples[k].value(dimension),
                                                                            SAMPLE_WEIGHT_STYLES, trendWeight);
                        multivariateWeights[k][0].assign(dimension, emptyBucketWeight * count * this->learnRate(feature));
                        multivariateWeights[k][1] = trendWeight[1];
                        multivariateWeights[k][2].assign(dimension, vs);

                        this->residuals(interval, trend,
                                        *multivariatePrior,
                                        detrendedMultivariateSamples[k], residuals);
                    }

                    CModelTools::updatePrior(SAMPLE_WEIGHT_STYLES,
                                             detrendedMultivariateSamples,
                                             multivariateWeights,
                                             interval,
                                             *multivariatePrior);

                    if (maths::CBasicStatistics::count(residuals[0][0]) > 0.0)
                    {
                        prediction = multivariatePrior->marginalLikelihoodMean();
                        means(residuals[0], residual);
                        double multiplier = this->decayRateMultiplier(E_PriorControl,
                                                                      feature, pid,
                                                                      prediction, residual);
                        multivariatePrior->decayRate(multiplier * multivariatePrior->decayRate());
                        LOG_TRACE("prior decay rate = " << multivariatePrior->decayRate());
                    }
                    if (maths::CBasicStatistics::count(residuals[1][0]) > 0.0)
                    {
                        prediction.resize(trend.size());
                        for (std::size_t k = 0u; k < trend.size(); ++k)
                        {
                            prediction[k] = trend[k]->mean();
                        }
                        means(residuals[1], residual);
                        double multiplier = this->decayRateMultiplier(E_TrendControl,
                                                                      feature, pid,
                                                                      prediction, residual);
                        for (std::size_t k = 0u; k < trend.size(); ++k)
                        {
                            trend[k]->decayRate(multiplier * trend[k]->decayRate());
                        }
                        LOG_TRACE("trend decay rate = " << trend[0]->decayRate());
                    }

                    LOG_TRACE(this->personName(pid) << " prior:"
                              << core_t::LINE_ENDING << multivariatePrior->print());
                }
            }

            if (correlationPriors)
            {
                // The priors use a shared pseudo random number generator which
                // generates a fixed sequence of random numbers. Since the order
                // of the correlated priors map can change across persist and
                // restore we can effectively get a different sequence of random
                // numbers depending on whether we persist and restore or not.
                // We'd like results to be as independent as possible from the
                // action of persisting and restoring so sort the collection to
                // preserve the random number sequence.
                TSizeSizePrMultivariatePriorPtrDoublePrUMapCItrVec iterators;
                iterators.reserve(correlationPriors->size());
                for (TSizeSizePrMultivariatePriorPtrDoublePrUMapCItr j = correlationPriors->begin();
                     j != correlationPriors->end();
                     ++j)
                {
                    iterators.push_back(j);
                }
                std::sort(iterators.begin(), iterators.end(),
                          core::CFunctional::SDereference<maths::COrderings::SFirstLess>());

                for (TSizeSizePrMultivariatePriorPtrDoublePrUMapCItrVecCItr j = iterators.begin();
                     j != iterators.end();
                     ++j)
                {
                    std::size_t pid1 = (*j)->first.first;
                    std::size_t pid2 = (*j)->first.second;
                    TSizeFeatureSampleDataUMapItr j1 = sampleData.find(pid1);
                    TSizeFeatureSampleDataUMapItr j2 = sampleData.find(pid2);
                    if (j1 == sampleData.end() || j2 == sampleData.end())
                    {
                        continue;
                    }

                    const TMultivariatePriorPtr &prior = (*j)->second.first;
                    SFeatureSampleData *samples1 = &j1->second;
                    SFeatureSampleData *samples2 = &j2->second;
                    double multiplier = samples1->s_Multiplier * samples2->s_Multiplier;
                    std::size_t n1 = samples1->s_Times.size();
                    std::size_t n2 = samples2->s_Times.size();
                    std::size_t indices[] = { 0, 1 };
                    if (n1 < n2)
                    {
                        std::swap(samples1, samples2);
                        std::swap(n1, n2);
                        std::swap(indices[0], indices[1]);
                    }
                    detrendedMultivariateSamples.assign(n1, TDouble10Vec(2));
                    multivariateWeights.assign(n1, TDouble10Vec4Vec(N, TDouble10Vec(2)));

                    maths::COrderings::simultaneousSort(samples2->s_Times,
                                                        samples2->s_DetrendedSamples,
                                                        samples2->s_Weights);

                    for (std::size_t k1 = 0u; k1 < n1; ++k1)
                    {
                        std::size_t k2 = 0u;
                        if (n2 > 1)
                        {
                            core_t::TTime t = samples1->s_Times[k1];
                            std::size_t b = maths::CTools::truncate(static_cast<std::size_t>(
                                                std::lower_bound(samples2->s_Times.begin(),
                                                                 samples2->s_Times.end(), t)
                                              - samples2->s_Times.begin()), std::size_t(1), n2 - 1);
                            std::size_t a = b - 1;
                            k2 =  std::abs(samples2->s_Times[a] - t)
                                < std::abs(samples2->s_Times[b] - t) ? a : b;
                        }
                        detrendedMultivariateSamples[k1][indices[0]] = samples1->s_DetrendedSamples[k1];
                        detrendedMultivariateSamples[k1][indices[1]] = samples2->s_DetrendedSamples[k2];
                        for (std::size_t w = 0u; w < N; ++w)
                        {
                            multivariateWeights[k1][w][indices[0]] = samples1->s_Weights[k1][w];
                            multivariateWeights[k1][w][indices[1]] = samples2->s_Weights[k2][w];
                        }
                    }
                    LOG_TRACE("Bucket = " << gatherer.printCurrentBucket(time)
                              << ", feature = " << model_t::print(feature)
                              << ", for (" << this->personName(pid1) << "," << this->personName(pid2) << ")"
                              << ", correlate samples = " << core::CContainerPrinter::print(detrendedMultivariateSamples)
                              << ", correlate weights = " << core::CContainerPrinter::print(multivariateWeights));

                    prior->dataType(samples1->s_IsInteger || samples2->s_IsInteger ?
                                    maths_t::E_IntegerData : maths_t::E_ContinuousData);
                    CModelTools::updatePrior(SAMPLE_WEIGHT_STYLES,
                                             detrendedMultivariateSamples,
                                             multivariateWeights,
                                             std::min(samples1->s_Interval, samples2->s_Interval),
                                             *prior);
                    prior->decayRate(multiplier * prior->decayRate());
                    LOG_TRACE("correlation prior:" << core_t::LINE_ENDING << prior->print());
                    LOG_TRACE("decayRate = " << prior->decayRate());
                }
            }
            if (correlations)
            {
                correlations->capture();
            }
        }
    }
}

bool CMetricModel::computeProbability(const std::size_t pid,
                                      core_t::TTime startTime,
                                      core_t::TTime endTime,
                                      CPartitioningFields &partitioningFields,
                                      const std::size_t /*numberAttributeProbabilities*/,
                                      SAnnotatedProbability &result) const
{
    CAnnotatedProbabilityBuilder resultBuilder(result);

    const CDataGatherer &gatherer = this->dataGatherer();
    core_t::TTime bucketLength = gatherer.bucketLength();

    if (endTime != startTime + bucketLength)
    {
        LOG_ERROR("Can only compute probability for single bucket");
        return false;
    }

    if (pid >= this->firstBucketTimes().size())
    {
        // This is not necessarily an error: the person might have been added
        // only in an out of phase bucket so far
        LOG_TRACE("No first time for person = " << gatherer.personName(pid));
        return false;
    }

    typedef CProbabilityAndInfluenceCalculator::SParams TParams;
    typedef CProbabilityAndInfluenceCalculator::SCorrelateParams TCorrelateParams;
    typedef CProbabilityAndInfluenceCalculator::SMultivariateParams TMultivariateParams;

    CProbabilityAndInfluenceCalculator pJoint(this->params().s_InfluenceCutoff);
    pJoint.addAggregator(maths::CJointProbabilityOfLessLikelySamples());
    pJoint.addAggregator(maths::CProbabilityOfExtremeSample());

    // Declared outside loop to minimize number of times they are created.
    TParams params(PROBABILITY_WEIGHT_STYLES, partitioningFields);
    TCorrelateParams correlateParams(PROBABILITY_WEIGHT_STYLES, partitioningFields);
    TMultivariateParams multivariateParams(PROBABILITY_WEIGHT_STYLES, partitioningFields);
    TStrCRefDouble1VecDouble1VecPrPrVecVecVec correlateInfluenceValues;
    params.s_Weights.resize(1, TDouble4Vec(PROBABILITY_WEIGHT_STYLES.size()));
    multivariateParams.s_Sample.resize(1);
    multivariateParams.s_Weights.resize(1, TDouble10Vec4Vec(2));
    TOptionalUInt64 count = this->currentBucketCount(pid, startTime);
    params.s_BucketEmpty = multivariateParams.s_BucketEmpty = (!count || *count == 0);

    const TFeatureData *data = 0;
    for (std::size_t i = 0u, n = gatherer.numberFeatures(); i < n; ++i)
    {
        model_t::EFeature feature = gatherer.feature(i);
        if (model_t::isCategorical(feature))
        {
            continue;
        }

        std::size_t dimension = model_t::dimension(feature);
        if (dimension == 1)
        {
            if (!this->sampleAndWeights(feature, pid, startTime, bucketLength,
                                        data, params.s_Sample, params.s_Weights))
            {
                continue;
            }

            LOG_TRACE("Compute probability for " << data->print());

            params.s_Trend = this->trend(feature, pid);
            params.s_ProbabilityBucketEmpty = this->probabilityBucketEmpty(feature, pid);
            const TOptionalSample &bucket = data->s_BucketValue;
            core_t::TTime sampleTime  = model_t::sampleTime(feature, startTime, bucketLength, bucket->time());
            core_t::TTime elapsedTime = sampleTime - this->firstBucketTimes()[pid];

            if (this->shouldIgnoreResult(feature,
                                         result.s_ResultType,
                                         partitioningFields.partitionFieldValue(),
                                         pid,
                                         model_t::INDIVIDUAL_ANALYSIS_ATTRIBUTE_ID,
                                         sampleTime))
            {
                continue;
            }

            // Check for correlations.
            this->correlateData(feature, pid, params,
                                startTime, bucketLength, result.isInterim(),
                                bucket, data->s_InfluenceValues,
                                correlateParams, correlateInfluenceValues);

            if (correlateParams.s_Values.size() > 0)
            {
                this->addProbabilityAndInfluences(pid, correlateParams, correlateInfluenceValues, pJoint, resultBuilder);
            }
            else
            {
                const maths::CPrior *prior = this->prior(feature, pid);
                if (!prior)
                {
                    LOG_ERROR("No prior for " << this->personName(pid)
                              << " and feature = " << model_t::print(feature));
                    return false;
                }

                params.s_Feature = feature;
                params.s_Prior = prior;
                params.s_ElapsedTime = elapsedTime;
                params.s_Time  = sampleTime;
                params.s_Value = bucket->value();
                params.s_Count = bucket->count();
                if (result.isInterim() && model_t::requiresInterimResultAdjustment(feature))
                {
                    double mode = prior->marginalLikelihoodMode(PROBABILITY_WEIGHT_STYLES, params.s_Weights[0]);
                    TDouble1Vec correction(1, this->interimValueCorrector().corrections(
                                                            sampleTime,
                                                            this->currentBucketTotalCount(),
                                                            mode, params.s_Sample[0]));
                    params.s_Value  += correction;
                    params.s_Sample += correction;
                    this->currentBucketInterimCorrections().emplace(core::make_triple(feature, pid, pid), correction);
                }
                this->addProbabilityAndInfluences(pid, params, data->s_InfluenceValues, pJoint, resultBuilder);
            }
        }
        else
        {
            if (!this->sampleAndWeights(feature, dimension, pid, startTime, bucketLength,
                                        data, multivariateParams.s_Sample, multivariateParams.s_Weights))
            {
                continue;
            }
            LOG_TRACE("Compute probability for " << data->print());

            const TOptionalSample &bucket = data->s_BucketValue;
            core_t::TTime sampleTime  = model_t::sampleTime(feature, startTime, bucketLength, bucket->time());
            core_t::TTime elapsedTime = sampleTime - this->firstBucketTimes()[pid];

            const maths::CMultivariatePrior *prior = this->multivariatePrior(feature, pid);
            if (!prior)
            {
                LOG_ERROR("No prior for " << this->personName(pid)
                          << " and feature = " << model_t::print(feature));
                return false;
            }

            if (this->shouldIgnoreResult(feature,
                                         result.s_ResultType,
                                         partitioningFields.partitionFieldValue(),
                                         pid,
                                         model_t::INDIVIDUAL_ANALYSIS_ATTRIBUTE_ID,
                                         sampleTime))
            {
                continue;
            }

            multivariateParams.s_Feature = feature;
            multivariateParams.s_Trend = this->trend(feature, pid);
            multivariateParams.s_Prior = prior;
            multivariateParams.s_ElapsedTime = elapsedTime;
            multivariateParams.s_Time  = sampleTime;
            multivariateParams.s_Value = bucket->value();
            multivariateParams.s_Count = bucket->count();
            multivariateParams.s_ProbabilityBucketEmpty = this->probabilityBucketEmpty(feature, pid);
            if (result.isInterim() && model_t::requiresInterimResultAdjustment(feature))
            {
                TDouble10Vec mode = prior->marginalLikelihoodMode(PROBABILITY_WEIGHT_STYLES,
                                                                  multivariateParams.s_Weights[0]);
                TDouble10Vec corrections = this->interimValueCorrector().corrections(
                                                         sampleTime,
                                                         this->currentBucketTotalCount(),
                                                         mode, multivariateParams.s_Sample[0]);
                multivariateParams.s_Value     += corrections;
                multivariateParams.s_Sample[0] += corrections;
                this->currentBucketInterimCorrections().emplace(core::make_triple(feature, pid, pid), corrections);
            }
            this->addProbabilityAndInfluences(pid, multivariateParams, data->s_InfluenceValues, pJoint, resultBuilder);
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
        LOG_ERROR("Failed to compute probability");
        return false;
    }
    LOG_TRACE("probability(" << this->personName(pid) << ") = " << p);

    resultBuilder.probability(p);
    resultBuilder.build();

    return true;
}

uint64_t CMetricModel::checksum(bool includeCurrentBucketStats) const
{
    typedef std::map<TStrCRef, uint64_t, maths::COrderings::SLess> TStrCRefUInt64Map;

    uint64_t seed = this->CIndividualModel::checksum(includeCurrentBucketStats);

#define KEY(pid) boost::cref(this->personName(pid))

    TStrCRefUInt64Map hashes;
    if (includeCurrentBucketStats)
    {
        const TFeatureSizeFeatureDataPrVecPrVec &featureData = m_CurrentBucketStats.s_FeatureData;
        for (std::size_t i = 0u; i < featureData.size(); ++i)
        {
            for (std::size_t j = 0u; j < featureData[i].second.size(); ++j)
            {
                uint64_t &hash = hashes[KEY(featureData[i].second[j].first)];
                const TFeatureData &data = featureData[i].second[j].second;
                hash = maths::CChecksum::calculate(hash, data.s_BucketValue);
                hash = core::CHashing::hashCombine(hash, static_cast<uint64_t>(data.s_IsInteger));
                hash = maths::CChecksum::calculate(hash, data.s_Samples);
            }
        }
    }

#undef KEY

    LOG_TRACE("seed = " << seed);
    LOG_TRACE("hashes = " << core::CContainerPrinter::print(hashes));

    return maths::CChecksum::calculate(seed, hashes);
}

void CMetricModel::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CMetricModel");
    this->CIndividualModel::debugMemoryUsage(mem->addChild());
    core::CMemoryDebug::dynamicSize("m_CurrentBucketStats.s_PersonCounts",
                                    m_CurrentBucketStats.s_PersonCounts, mem);
    core::CMemoryDebug::dynamicSize("m_CurrentBucketStats.s_FeatureData",
                                    m_CurrentBucketStats.s_FeatureData, mem);
    core::CMemoryDebug::dynamicSize("m_CurrentBucketStats.s_InterimCorrections",
                                        m_CurrentBucketStats.s_InterimCorrections, mem);
}

std::size_t CMetricModel::memoryUsage(void) const
{
    return this->CIndividualModel::memoryUsage();
}

std::size_t CMetricModel::computeMemoryUsage(void) const
{
    std::size_t mem = this->CIndividualModel::computeMemoryUsage();
    mem += core::CMemory::dynamicSize(m_CurrentBucketStats.s_PersonCounts);
    mem += core::CMemory::dynamicSize(m_CurrentBucketStats.s_FeatureData);
    mem += core::CMemory::dynamicSize(m_CurrentBucketStats.s_InterimCorrections);
    return mem;
}

std::size_t CMetricModel::staticSize(void) const
{
    return sizeof(*this);
}

CMetricModel::CModelDetailsViewPtr CMetricModel::details(void) const
{
    return CModelDetailsViewPtr(new CMetricModelDetailsView(*this));
}

const CMetricModel::TFeatureData *CMetricModel::featureData(model_t::EFeature feature,
                                                            std::size_t pid,
                                                            core_t::TTime time) const
{
    return this->CIndividualModel::featureData(feature, pid, time, m_CurrentBucketStats.s_FeatureData);
}

void CMetricModel::createNewModels(std::size_t n, std::size_t m)
{
    this->CIndividualModel::createNewModels(n, m);
}

void CMetricModel::updateRecycledModels(void)
{
    this->CIndividualModel::updateRecycledModels();
}

void CMetricModel::clearPrunedResources(const TSizeVec &people,
                                        const TSizeVec &attributes)
{
    CDataGatherer &gatherer = this->dataGatherer();

    // Stop collecting for these people and add them to the free list.
    gatherer.recyclePeople(people);
    if (gatherer.dataAvailable(m_CurrentBucketStats.s_StartTime))
    {
        gatherer.featureData(m_CurrentBucketStats.s_StartTime, gatherer.bucketLength(), m_CurrentBucketStats.s_FeatureData);
    }

    this->CIndividualModel::clearPrunedResources(people, attributes);
}

core_t::TTime CMetricModel::currentBucketStartTime(void) const
{
    return m_CurrentBucketStats.s_StartTime;
}

void CMetricModel::currentBucketStartTime(core_t::TTime time)
{
    m_CurrentBucketStats.s_StartTime = time;
}

uint64_t CMetricModel::currentBucketTotalCount(void) const
{
    return m_CurrentBucketStats.s_TotalCount;
}

CIndividualModel::TFeatureSizeSizeTripleDouble1VecUMap &
    CMetricModel::currentBucketInterimCorrections(void) const
{
    return m_CurrentBucketStats.s_InterimCorrections;
}

const CMetricModel::TSizeUInt64PrVec &CMetricModel::currentBucketPersonCounts(void) const
{
    return m_CurrentBucketStats.s_PersonCounts;
}

CMetricModel::TSizeUInt64PrVec &CMetricModel::currentBucketPersonCounts(void)
{
    return m_CurrentBucketStats.s_PersonCounts;
}

void CMetricModel::currentBucketTotalCount(uint64_t totalCount)
{
    m_CurrentBucketStats.s_TotalCount = totalCount;
}

void CMetricModel::sampleCounts(core_t::TTime /*startTime*/, core_t::TTime /*endTime*/)
{
}

void CMetricModel::correlateData(model_t::EFeature feature,
                                 std::size_t pid,
                                 const CProbabilityAndInfluenceCalculator::SParams &univariateParams,
                                 core_t::TTime time,
                                 core_t::TTime bucketLength,
                                 bool isInterim,
                                 const TOptionalSample &bucket,
                                 const TStrCRefDouble1VecDoublePrPrVecVec &influenceValues,
                                 CProbabilityAndInfluenceCalculator::SCorrelateParams &params,
                                 TStrCRefDouble1VecDouble1VecPrPrVecVecVec &correlateInfluenceValues) const
{
    params.s_Feature = feature;
    params.clear();
    correlateInfluenceValues.resize(influenceValues.size());
    for (std::size_t i = 0u; i < correlateInfluenceValues.size(); ++i)
    {
        correlateInfluenceValues[i].clear();
    }

    if (!this->params().s_MultivariateByFields)
    {
        return;
    }

    const CDataGatherer &gatherer = this->dataGatherer();
    const TDecompositionCPtr1Vec &trend = univariateParams.s_Trend;
    const TDouble1Vec &sample = univariateParams.s_Sample;
    const TDouble4Vec1Vec &weights = univariateParams.s_Weights;

    params.s_Correlated = this->correlated(feature, pid);
    this->correlatePriors(feature, pid, params.s_Correlated, params.s_Variables, params.s_Priors);
    params.s_BucketEmpty.push_back(univariateParams.s_BucketEmpty);
    params.s_ProbabilityBucketEmpty.push_back(univariateParams.s_ProbabilityBucketEmpty);

    static const std::size_t N = PROBABILITY_WEIGHT_STYLES.size();

    core_t::TTime sampleTime  = model_t::sampleTime(feature, time, bucketLength, bucket->time());
    core_t::TTime elapsedTime = sampleTime - this->firstBucketTimes()[pid];

    // Declared outside loop to minimize number of times they are created.
    TDouble1Vec influenceValue;
    TDouble1Vec influenceCount(2);

    for (std::size_t i = 0u, end = 0u; i < params.s_Correlated.size(); ++i)
    {
        std::size_t correlatePid = params.s_Correlated[i];

        const TFeatureData *di;
        TDouble1Vec si;
        TDouble4Vec1Vec wi(1, TDouble4Vec(N));
        if (!this->sampleAndWeights(feature, correlatePid, time, bucketLength, di, si, wi))
        {
            continue;
        }
        TDecompositionCPtr1Vec ti = this->trend(feature, correlatePid);
        const TOptionalSample &bi = di->s_BucketValue;
        core_t::TTime sti = model_t::sampleTime(feature, time, bucketLength, bi->time());
        core_t::TTime eti = sti - this->firstBucketTimes()[correlatePid];
        const TStrCRefDouble1VecDoublePrPrVecVec &influenceValuesi = di->s_InfluenceValues;

        std::size_t v0 = params.s_Variables[i][0];
        std::size_t v1 = params.s_Variables[i][1];

        params.s_Trends.push_back(TDecompositionCPtr1Vec(2));
        params.s_Values.push_back(TDouble10Vec(2));
        params.s_Counts.push_back(TDouble10Vec(2));
        params.s_Times.push_back(TTime2Vec(2));
        params.s_ElapsedTimes.push_back(TTime2Vec(2));
        params.s_Samples.push_back(TDouble10Vec(2));
        params.s_Weights.push_back(TDouble10Vec4Vec(N, TDouble10Vec(2)));
        params.s_CorrelatedLabels.push_back(gatherer.personNamePtr(correlatePid));
        params.s_Trends[end][v0] = trend.empty()  ? 0 : trend[0];
        params.s_Trends[end][v1] = ti.empty() ? 0 : ti[0];
        params.s_Priors[end] = params.s_Priors[i];
        params.s_Values[end][v0] = bucket->value()[0];
        params.s_Values[end][v1] = bi->value()[0];
        params.s_Counts[end][v0] = bucket->count();
        params.s_Counts[end][v1] = bi->count();
        params.s_ElapsedTimes[end][v0] = elapsedTime;
        params.s_ElapsedTimes[end][v1] = eti;
        params.s_Times[end][v0]   = sampleTime;
        params.s_Times[end][v1]   = sti;
        params.s_Samples[end][v0] = sample[0];
        params.s_Samples[end][v1] = si[0];
        for (std::size_t j = 0u; j < N; ++j)
        {
            params.s_Weights[end][j][v0] = weights[0][j];
            params.s_Weights[end][j][v1] = wi[0][j];
        }
        TOptionalUInt64 count = this->currentBucketCount(correlatePid, time);
        params.s_BucketEmpty.push_back(!count || *count == 0);
        params.s_ProbabilityBucketEmpty.push_back(this->probabilityBucketEmpty(feature, correlatePid));
        for (std::size_t j = 0u; j < influenceValuesi.size(); ++j)
        {
            correlateInfluenceValues[j].push_back(TStrCRefDouble1VecDouble1VecPrPrVec());
            for (std::size_t k = 0u; k < influenceValuesi[j].size(); ++k)
            {
                TStrCRef influence[] = { influenceValuesi[j][k].first };
                std::size_t match = static_cast<std::size_t>(
                                        std::find_first_of(influenceValues[j].begin(),
                                                           influenceValues[j].end(),
                                                           influence, influence + 1,
                                                           SInfluenceEqual()) - influenceValues[j].begin());
                if (match < influenceValues[j].size())
                {
                    const TDouble1VecDoublePr &i0 = influenceValues[j][match].second;
                    const TDouble1VecDoublePr &i1 = influenceValuesi[j][k].second;
                    influenceValue.resize(i0.first.size() + i1.first.size());
                    influenceValue[v0] = i0.first[0];
                    influenceValue[v1] = i1.first[0];
                    for (std::size_t l = 1; l < i1.first.size(); ++l)
                    {
                        influenceValue[2*l + v0] = i0.first[l];
                        influenceValue[2*l + v1] = i1.first[l];
                    }
                    influenceCount[v0] = i0.second;
                    influenceCount[v1] = i1.second;
                    correlateInfluenceValues[j][end].push_back(TStrCRefDouble1VecDouble1VecPrPr(
                                                                   influence[0],
                                                                   TDouble1VecDouble1VecPr(influenceValue, influenceCount)));
                }
            }
        }

        if (isInterim && model_t::requiresInterimResultAdjustment(feature))
        {
            TDouble10Vec mode = params.s_Priors[end].first->marginalLikelihoodMode(PROBABILITY_WEIGHT_STYLES,
                                                                                   params.s_Weights[end]);
            TDouble1Vec corrections = this->interimValueCorrector().corrections(
                                                    sampleTime,
                                                    this->currentBucketTotalCount(),
                                                    mode,
                                                    params.s_Samples[end]);
            params.s_Samples[end] += corrections;
            params.s_Values[end]  += corrections;
            this->currentBucketInterimCorrections().emplace(core::make_triple(feature, pid, correlatePid),
                                                            TDouble1Vec(1, corrections[v0]));
        }

        ++end;
    }

    params.s_Priors.resize(params.s_Values.size());
}

bool CMetricModel::sampleAndWeights(model_t::EFeature feature,
                                    std::size_t pid,
                                    core_t::TTime time,
                                    core_t::TTime bucketLength,
                                    const TFeatureData *&data,
                                    TDouble1Vec &sample,
                                    TDouble4Vec1Vec &weights) const
{
    data = this->featureData(feature, pid, time);
    if (!data)
    {
        return false;
    }

    const TOptionalSample &bucket = data->s_BucketValue;
    if (!bucket)
    {
        return false;
    }

    time = model_t::sampleTime(feature, time, bucketLength, bucket->time());
    sample = this->detrend(feature, pid, time, SEASONAL_CONFIDENCE_INTERVAL, bucket->value(1));
    weights[0][0] = this->seasonalVarianceScale(feature, pid, time, 0.0).second[0];
    weights[0][1] = bucket->varianceScale();

    return true;
}

bool CMetricModel::sampleAndWeights(model_t::EFeature feature,
                                    std::size_t dimension,
                                    std::size_t pid,
                                    core_t::TTime time,
                                    core_t::TTime bucketLength,
                                    const TFeatureData *&data,
                                    TDouble10Vec1Vec &sample,
                                    TDouble10Vec4Vec1Vec &weights) const
{
    data = this->featureData(feature, pid, time);
    if (!data)
    {
        return false;
    }

    const TOptionalSample &bucket = data->s_BucketValue;
    if (!bucket)
    {
        return false;
    }

    time = model_t::sampleTime(feature, time, bucketLength, bucket->time());
    sample[0] = this->detrend(feature, pid, time, SEASONAL_CONFIDENCE_INTERVAL, bucket->value(dimension));
    weights[0][0] = this->seasonalVarianceScale(feature, pid, time, SEASONAL_CONFIDENCE_INTERVAL).second;
    weights[0][1].assign(dimension, bucket->varianceScale());

    return true;
}

template<typename PRIOR, typename VECTOR>
void CMetricModel::residuals(double interval,
                             const TDecompositionPtr1Vec &trend,
                             const PRIOR &prior,
                             const VECTOR &sample,
                             TMeanAccumulator1Vec (&result)[2]) const
{
    if (boost::optional<VECTOR> residual = CModelTools::predictionResidual(interval, prior, sample))
    {
        for (std::size_t i = 0u; i < residual->size(); ++i)
        {
            result[0][i].add((*residual)[i]);
        }
    }
    if (boost::optional<VECTOR> residual = CModelTools::predictionResidual(trend, sample))
    {
        for (std::size_t i = 0u; i < residual->size(); ++i)
        {
            result[1][i].add((*residual)[i]);
        }
    }
}

////////// CMetricModel::SBucketStats Implementation //////////

CMetricModel::SBucketStats::SBucketStats(core_t::TTime startTime) :
        s_StartTime(startTime),
        s_PersonCounts(),
        s_TotalCount(0),
        s_FeatureData(),
        s_InterimCorrections(1)
{}

}
}
