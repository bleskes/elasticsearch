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

#include <model/CEventRateModel.h>

#include <core/CContainerPrinter.h>
#include <core/CFunctional.h>
#include <core/CLogger.h>
#include <core/CoreTypes.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>
#include <core/CStatistics.h>

#include <maths/CChecksum.h>
#include <maths/CDistributionRestoreParams.h>
#include <maths/CMultivariatePrior.h>
#include <maths/COrderings.h>
#include <maths/CPrior.h>
#include <maths/CTools.h>
#include <maths/ProbabilityAggregators.h>

#include <model/CAnnotatedProbabilityBuilder.h>
#include <model/CDataGatherer.h>
#include <model/CIndividualModelDetail.h>
#include <model/CInterimBucketCorrector.h>
#include <model/CModelDetailsView.h>
#include <model/CProbabilityAndInfluenceCalculator.h>
#include <model/CResourceMonitor.h>
#include <model/CModelTools.h>
#include <model/FrequencyPredicates.h>

#include <boost/bind.hpp>
#include <boost/iterator/counting_iterator.hpp>
#include <boost/ref.hpp>

#include <algorithm>
#include <string>
#include <utility>
#include <vector>

#include <stdint.h>

namespace prelert
{
namespace model
{

namespace
{

typedef core::CSmallVector<double, 1> TDouble1Vec;
typedef core::CSmallVector<double, 4> TDouble4Vec;
typedef core::CSmallVector<TDouble4Vec, 1> TDouble4Vec1Vec;

//! \brief Wraps up the sampled data for a feature.
struct SFeatureSampleData
{
    void fill(const TDouble1Vec &sample, const TDouble4Vec1Vec &weights, double interval, double multiplier)
    {
        s_Sample     = sample;
        s_Weights    = weights;
        s_Interval   = interval;
        s_Multiplier = multiplier;
    }

    std::string print(void) const
    {
        return core::CContainerPrinter::print(s_Sample);
    }

    TDouble1Vec s_Sample;
    TDouble4Vec1Vec s_Weights;
    double s_Interval;
    double s_Multiplier;
};

// We obfuscate the XML element names to avoid giving away too much
// information about our model.
const std::string INDIVIDUAL_STATE_TAG("a");
const std::string PROBABILITY_PRIOR_TAG("b");

const maths_t::ESampleWeightStyle SAMPLE_WEIGHT_STYLES_[] =
    {
        maths_t::E_SampleCountWeight,
        maths_t::E_SampleWinsorisationWeight
    };
const maths_t::TWeightStyleVec SAMPLE_WEIGHT_STYLES(boost::begin(SAMPLE_WEIGHT_STYLES_),
                                                    boost::end(SAMPLE_WEIGHT_STYLES_));
const maths_t::TWeightStyleVec PROBABILITY_WEIGHT_STYLES(1, maths_t::E_SampleSeasonalVarianceScaleWeight);

}

CEventRateModel::CEventRateModel(const SModelParams &params,
                                 const TDataGathererPtr &dataGatherer,
                                 const TFeaturePriorPtrPrVec &newPriors,
                                 const TFeatureMultivariatePriorPtrPrVec &newMultivariatePriors,
                                 const TFeatureMultivariatePriorPtrPrVec &newCorrelatePriors,
                                 const maths::CMultinomialConjugate &probabilityPrior,
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
        m_CurrentBucketStats(CModel::TIME_UNSET),
        m_ProbabilityPrior(probabilityPrior)
{
}

CEventRateModel::CEventRateModel(const SModelParams &params,
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
    traverser.traverseSubLevel(boost::bind(&CEventRateModel::acceptRestoreTraverser,
                                           this, boost::ref(params.s_ExtraDataRestoreFunc), _1));
}

CEventRateModel::CEventRateModel(bool isForPersistence, const CEventRateModel &other) :
        CIndividualModel(isForPersistence, other),
        m_CurrentBucketStats(0) // Not needed for persistence so minimally constructed
{
    if (!isForPersistence)
    {
        LOG_ABORT("This constructor only creates clones for persistence");
    }
}

void CEventRateModel::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertLevel(INDIVIDUAL_STATE_TAG, boost::bind(&CEventRateModel::doAcceptPersistInserter, this, _1));
    inserter.insertLevel(PROBABILITY_PRIOR_TAG,
                         boost::bind(&maths::CMultinomialConjugate::acceptPersistInserter,
                                     &m_ProbabilityPrior, _1));
}

bool CEventRateModel::acceptRestoreTraverser(const model_t::TAnyRestoreFunc &extraDataRestoreFunc,
                                             core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        if (name == INDIVIDUAL_STATE_TAG)
        {
            if (traverser.traverseSubLevel(boost::bind(&CEventRateModel::doAcceptRestoreTraverser,
                                                       this, boost::cref(extraDataRestoreFunc), _1)) == false)
            {
                // Logging handled already.
                return false;
            }
        }
        else if (name == PROBABILITY_PRIOR_TAG)
        {
            maths::CMultinomialConjugate prior(
                    this->params().distributionRestoreParams(maths_t::E_DiscreteData), traverser);
            m_ProbabilityPrior.swap(prior);
        }
    }
    while (traverser.next());

    return true;
}

CModel *CEventRateModel::cloneForPersistence(void) const
{
    return new CEventRateModel(true, *this);
}

model_t::EModelType CEventRateModel::category(void) const
{
    return model_t::E_EventRateOnline;
}

bool CEventRateModel::isEventRate(void) const
{
    return true;
}

bool CEventRateModel::isMetric(void) const
{
    return false;
}

void CEventRateModel::currentBucketPersonIds(core_t::TTime time, TSizeVec &result) const
{
    this->CIndividualModel::currentBucketPersonIds(time, m_CurrentBucketStats.s_FeatureData, result);
}

CEventRateModel::TOptionalDouble
    CEventRateModel::baselineBucketCount(std::size_t /*pid*/) const
{
    return TOptionalDouble();
}

CEventRateModel::TDouble1Vec
    CEventRateModel::currentBucketValue(model_t::EFeature feature,
                                        std::size_t pid,
                                        std::size_t /*cid*/,
                                        core_t::TTime time) const
{
    const TFeatureData *data = this->featureData(feature, pid, time);
    if (data)
    {
        return TDouble1Vec(1, static_cast<double>(data->s_Count));
    }
    return TDouble1Vec();
}

CEventRateModel::TDouble1Vec
    CEventRateModel::baselineBucketMean(model_t::EFeature feature,
                                        std::size_t pid,
                                        std::size_t cid,
                                        model_t::CResultType type,
                                        const TSizeDoublePr1Vec &correlated,
                                        core_t::TTime time) const
{
    if (model_t::isDiurnal(feature))
    {
        const maths::CPrior *prior = this->prior(feature, pid);
        if (!prior)
        {
            LOG_ERROR("Missing prior for person " << this->personName(pid));
            return TDouble1Vec();
        }

        TDouble1Vec value = this->currentBucketValue(feature, pid, cid, time);
        if (value.empty())
        {
            return TDouble1Vec(1, prior->marginalLikelihoodMean());
        }

        // TODO needs to work with conditional distributions.
        return TDouble1Vec(1, prior->nearestMarginalLikelihoodMean(value[0]));
    }

    typedef core::CSmallVector<std::size_t, 10> TSize10Vec;
    typedef core::CSmallVector<TSizeDoublePr, 10> TSizeDoublePr10Vec;

    double scale = 1.0 - this->probabilityBucketEmpty(feature, pid);
    TDecompositionCPtr1Vec trend = this->trend(feature, pid);
    double correlateCorrection = this->correctBaselineForCorrelated(feature, pid, type, correlated);

    double probability = 1.0;
    if (model_t::isConstant(feature) && !m_Probabilities.lookup(pid, probability))
    {
        probability = 1.0;
    }

    TDouble1Vec result;
    if (model_t::dimension(feature) == 1)
    {
        const maths::CPrior *prior = this->prior(feature, pid);
        if (!prior)
        {
            return TDouble1Vec();
        }

        double seasonalOffset = 0.0;
        if (!trend.empty() && trend[0]->initialized())
        {
            seasonalOffset = maths::CBasicStatistics::mean(trend[0]->baseline(time, 0.0));
        }

        double median = prior->isNonInformative() ?
                        prior->marginalLikelihoodMean() :
                        maths::CBasicStatistics::mean(prior->marginalLikelihoodConfidenceInterval(0.0));
        result.assign(1, probability * model_t::inverseOffsetCountToZero(
                                           feature, scale * (seasonalOffset + median + correlateCorrection)));
    }
    else
    {
        const maths::CMultivariatePrior *prior = this->multivariatePrior(feature, pid);
        if (!prior)
        {
            return TDouble1Vec();
        }

        TSize10Vec marginalize(boost::make_counting_iterator(std::size_t(1)),
                               boost::make_counting_iterator(prior->dimension()));
        static const TSizeDoublePr10Vec CONDITION;

        result.reserve(prior->dimension());
        TDouble10Vec mean;
        if (prior->isNonInformative())
        {
            mean = prior->marginalLikelihoodMean();
        }
        for (std::size_t i = 0u; i < result.size(); --marginalize[i], ++i)
        {
            double seasonalOffset = 0.0;
            if (i < trend.size() && trend[i]->initialized())
            {
                seasonalOffset = maths::CBasicStatistics::mean(trend[i]->baseline(time, 0.0));
            }

            double median = prior->isNonInformative() ?
                            mean[i] :
                            maths::CBasicStatistics::mean(
                                    prior->univariate(marginalize, CONDITION).first->marginalLikelihoodConfidenceInterval(0.0));
            result.push_back(probability * model_t::inverseOffsetCountToZero(
                                               feature, scale * (seasonalOffset + median + correlateCorrection)));
        }
    }

    this->correctBaselineForInterim(feature, pid, type, correlated,
                                    this->currentBucketInterimCorrections(), result);

    TDouble1VecDouble1VecPr support = model_t::support(feature);
    return maths::CTools::truncate(result, support.first, support.second);
}

void CEventRateModel::outputCurrentBucketStatistics(const std::string &partitionFieldValue,
                                                    const TBucketStatsOutputFunc &outputFunc) const
{
    const CDataGatherer &gatherer = this->dataGatherer();
    const std::string &partitionFieldName = gatherer.partitionFieldName();
    const std::string &personFieldName = gatherer.personFieldName();

    const TFeatureSizeFeatureDataPrVecPrVec &featureData = m_CurrentBucketStats.s_FeatureData;
    for (std::size_t i = 0u; i < featureData.size(); ++i)
    {
        const std::string &funcName = model_t::outputFunctionName(featureData[i].first);
        const TSizeFeatureDataPrVec &data = featureData[i].second;
        for (std::size_t j = 0u; j < data.size(); ++j)
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
                    EMPTY_STRING,
                    funcName,
                    static_cast<double>(data[j].second.s_Count),
                    true
                )
            );
        }
    }
}

void CEventRateModel::sampleBucketStatistics(core_t::TTime startTime,
                                             core_t::TTime endTime,
                                             CResourceMonitor &resourceMonitor)
{
    m_CurrentBucketStats.s_InterimCorrections.clear();
    this->CIndividualModel::sampleBucketStatistics(startTime, endTime,
                                                   this->personFilter(),
                                                   m_CurrentBucketStats.s_FeatureData,
                                                   resourceMonitor);
}

void CEventRateModel::sample(core_t::TTime startTime,
                             core_t::TTime endTime,
                             CResourceMonitor &resourceMonitor)
{
    typedef boost::unordered_map<std::size_t, SFeatureSampleData> TSizeFeatureSampleDataUMap;
    typedef TSizeFeatureSampleDataUMap::iterator TSizeFeatureSampleDataUMapItr;

    CDataGatherer &gatherer = this->dataGatherer();
    core_t::TTime bucketLength = gatherer.bucketLength();

    if (!gatherer.validateSampleTimes(startTime, endTime))
    {
        return;
    }

    this->createUpdateNewModels(startTime, resourceMonitor);
    m_CurrentBucketStats.s_InterimCorrections.clear();

    static const std::size_t N = SAMPLE_WEIGHT_STYLES.size();

    // Declared outside loop to minimize number of times they are created.
    TDouble1Vec sample(1, 0.0);
    TDouble1Vec4Vec trendWeight(N, TDouble1Vec(1));
    TDouble4Vec1Vec weights(1, TDouble4Vec(N, 1.0));
    TDouble4Vec1Vec seasonalWeight(1, TDouble4Vec(1));
    TDouble10Vec1Vec multivariateSamples(1);
    TDouble10Vec4Vec1Vec multivariateWeights(1, TDouble10Vec4Vec(2));
    TDouble1Vec prediction;
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

            switch (feature)
            {
            case model_t::E_IndividualCountByBucketAndPerson:
            case model_t::E_IndividualNonZeroCountByBucketAndPerson:
                break;

            case model_t::E_IndividualTotalBucketCountByPerson:
                for (std::size_t j = 0u; j < data.size(); ++j)
                {
                    if (data[j].second.s_Count == 0)
                    {
                        LOG_TRACE("Ignoring person with zero count = "
                                  << this->personName(data[j].first))
                        continue;
                    }
                    LOG_TRACE("person = " << this->personName(data[j].first));
                    sample[0] = static_cast<double>(data[j].first);
                    m_ProbabilityPrior.addSamples(maths::CConstantWeights::COUNT,
                                                  sample,
                                                  maths::CConstantWeights::SINGLE_UNIT);
                }
                if (!data.empty())
                {
                    m_ProbabilityPrior.propagateForwardsByTime(1.0);
                }
                continue;

            case model_t::E_IndividualIndicatorOfBucketPerson:
            case model_t::E_IndividualLowCountsByBucketAndPerson:
            case model_t::E_IndividualHighCountsByBucketAndPerson:
            case model_t::E_IndividualArrivalTimesByPerson:
            case model_t::E_IndividualLongArrivalTimesByPerson:
            case model_t::E_IndividualShortArrivalTimesByPerson:
            case model_t::E_IndividualLowNonZeroCountByBucketAndPerson:
            case model_t::E_IndividualHighNonZeroCountByBucketAndPerson:
            case model_t::E_IndividualUniqueCountByBucketAndPerson:
            case model_t::E_IndividualLowUniqueCountByBucketAndPerson:
            case model_t::E_IndividualHighUniqueCountByBucketAndPerson:
            case model_t::E_IndividualInfoContentByBucketAndPerson:
            case model_t::E_IndividualHighInfoContentByBucketAndPerson:
            case model_t::E_IndividualLowInfoContentByBucketAndPerson:
            case model_t::E_IndividualTimeOfDayByBucketAndPerson:
            case model_t::E_IndividualTimeOfWeekByBucketAndPerson:
                break;

            CASE_INDIVIDUAL_METRIC:
            CASE_POPULATION_COUNT:
            CASE_POPULATION_METRIC:
            CASE_PEERS_COUNT:
            CASE_PEERS_METRIC:
                LOG_ERROR("Unexpected feature " << model_t::print(feature));
                continue;
            }

            applyFilter(model_t::E_XF_By, true, this->personFilter(), data);

            TSizeSizePrMultivariatePriorPtrDoublePrUMap *correlationPriors = this->correlatePriors(feature);
            maths::CKMostCorrelated *correlations = this->correlations(feature);
            sampleData.clear();

            for (std::size_t j = 0u; j < data.size(); ++j)
            {
                std::size_t pid = data[j].first;
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

                core_t::TTime t = model_t::sampleTime(feature, time, bucketLength);
                TDouble1Vec value(1, model_t::offsetCountToZero(feature, static_cast<double>(data[j].second.s_Count)));
                double derate = this->derate(pid, t);
                double interval = (1.0 + (this->params().s_InitialDecayRateMultiplier - 1.0) * derate) * emptyBucketWeight;
                std::size_t dimension = model_t::dimension(feature);

                LOG_TRACE("Bucket = " << this->printCurrentBucket()
                          << ", feature = " << model_t::print(feature)
                          << ", count = " << data[j].second.s_Count
                          << ", person = " << this->personName(pid)
                          << ", empty bucket weight = " << emptyBucketWeight
                          << ", derate = " << derate
                          << ", interval = " << interval);

                if (dimension == 1)
                {
                    maths::CPrior *prior = this->prior(feature, pid);
                    const TDecompositionPtr1Vec &trend = this->trend(feature, pid);

                    seasonalWeight[0] = maths::CBasicStatistics::mean(
                                            this->seasonalVarianceScale(feature, pid, t, 0.0));
                    weights[0][0] = trendWeight[0][0] = emptyBucketWeight * this->learnRate(feature);
                    weights[0][1] = trendWeight[1][0] = winsorisingWeight(
                                                            *prior,
                                                            maths::CConstantWeights::SEASONAL_VARIANCE,
                                                            this->detrend(feature, pid, t, 0.0, value),
                                                            seasonalWeight, derate);
                    sample = this->updateTrend(feature, pid, t, value, SAMPLE_WEIGHT_STYLES, trendWeight);

                    CModelTools::updatePrior(SAMPLE_WEIGHT_STYLES, sample, weights, interval, *prior);

                    double multiplier = 1.0;
                    if (boost::optional<TDouble1Vec> residual =
                            CModelTools::predictionResidual(interval, *prior, sample))
                    {
                        prediction.assign(1, prior->marginalLikelihoodMean());
                        multiplier = this->decayRateMultiplier(E_PriorControl,
                                                               feature, pid,
                                                               prediction, *residual);
                        prior->decayRate(multiplier * prior->decayRate());
                        LOG_TRACE("prior decay rate = " << prior->decayRate());
                    }
                    if (boost::optional<TDouble1Vec> residual =
                            CModelTools::predictionResidual(trend, sample))
                    {
                        prediction.assign(1, trend[0]->mean());
                        trend[0]->decayRate(this->decayRateMultiplier(E_TrendControl,
                                                                      feature, pid,
                                                                      prediction, *residual)
                                            * trend[0]->decayRate());
                        LOG_TRACE("trend decay rate = " << trend[0]->decayRate());
                    }

                    if (SFeatureSampleData *personSampleData = correlationPriors ? &sampleData[pid] : 0)
                    {
                        personSampleData->fill(sample, weights, interval, multiplier);
                    }
                    if (correlations)
                    {
                        correlations->add(pid, sample[0]);
                    }

                    LOG_TRACE(this->personName(pid) << " prior:" << core_t::LINE_ENDING << prior->print());
                }
                else
                {
                    // TODO support multivariate features.
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
                    TSizeFeatureSampleDataUMapItr j1 = sampleData.find((*j)->first.first);
                    TSizeFeatureSampleDataUMapItr j2 = sampleData.find((*j)->first.second);
                    if (j1 == sampleData.end() || j2 == sampleData.end())
                    {
                        continue;
                    }
                    const TMultivariatePriorPtr &prior = (*j)->second.first;
                    SFeatureSampleData &samples1 = j1->second;
                    SFeatureSampleData &samples2 = j2->second;
                    double multiplier = samples1.s_Multiplier * samples2.s_Multiplier;
                    multivariateSamples.assign(1, TDouble10Vec(2));
                    multivariateSamples[0][0] = samples1.s_Sample[0];
                    multivariateSamples[0][1] = samples2.s_Sample[0];
                    multivariateWeights.assign(1, TDouble10Vec4Vec(N, TDouble10Vec(2)));
                    for (std::size_t w = 0u; w < N; ++w)
                    {
                        multivariateWeights[0][w][0] = samples1.s_Weights[0][w];
                        multivariateWeights[0][w][1] = samples2.s_Weights[0][w];
                    }
                    CModelTools::updatePrior(SAMPLE_WEIGHT_STYLES,
                                             multivariateSamples,
                                             multivariateWeights,
                                             std::min(samples1.s_Interval, samples2.s_Interval),
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

        m_Probabilities = TLessLikelyProbability(m_ProbabilityPrior);
    }
}

bool CEventRateModel::computeProbability(std::size_t pid,
                                         core_t::TTime startTime,
                                         core_t::TTime endTime,
                                         CPartitioningFields &partitioningFields,
                                         std::size_t /*numberAttributeProbabilities*/,
                                         SAnnotatedProbability &result) const
{
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

    CAnnotatedProbabilityBuilder resultBuilder(result,
                                               1, // # attribute probabilities
                                               function_t::function(gatherer.features()),
                                               gatherer.numberActivePeople());

    CProbabilityAndInfluenceCalculator pJoint(this->params().s_InfluenceCutoff);
    pJoint.addAggregator(maths::CJointProbabilityOfLessLikelySamples());

    CProbabilityAndInfluenceCalculator pFeatures(this->params().s_InfluenceCutoff);
    pFeatures.addAggregator(maths::CJointProbabilityOfLessLikelySamples());
    pFeatures.addAggregator(maths::CProbabilityOfExtremeSample());

    // Declared outside loop to minimize number of times they are created.
    TStrCRefDouble1VecDouble1VecPrPrVecVecVec correlateInfluenceValues;
    TParams params(PROBABILITY_WEIGHT_STYLES, partitioningFields);
    TCorrelateParams correlateParams(PROBABILITY_WEIGHT_STYLES, partitioningFields);
    params.s_Weights.resize(1, TDouble4Vec(1, 1.0));
    TOptionalUInt64 count = this->currentBucketCount(pid, startTime);
    params.s_BucketEmpty = (!count || *count == 0);

    bool addPersonProbability = false;

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

            params.s_ProbabilityBucketEmpty = this->probabilityBucketEmpty(feature, pid);
            params.s_Trend = this->trend(feature, pid);
            params.s_Value.assign(1, model_t::offsetCountToZero(feature, static_cast<double>(data->s_Count)));
            core_t::TTime sampleTime  = model_t::sampleTime(feature, startTime, bucketLength);
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

            addPersonProbability = true;

            // Check for correlations.
            this->correlateData(feature, pid, params,
                                startTime, bucketLength, result.isInterim(),
                                data->s_InfluenceValues,
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
                              << " and feature " << model_t::print(feature));
                    return false;
                }

                params.s_Feature = feature;
                params.s_Prior = prior;
                params.s_ElapsedTime = elapsedTime;
                params.s_Time  = sampleTime;
                params.s_Count = 1.0;
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

                this->addProbabilityAndInfluences(pid, params, data->s_InfluenceValues, pFeatures, resultBuilder);
            }
        }
        else
        {
            // TODO support multivariate features.
        }
    }

    pJoint.add(pFeatures);
    if (addPersonProbability && !params.s_BucketEmpty)
    {
        double p;
        if (m_Probabilities.lookup(pid, p))
        {
            LOG_TRACE("P(" << gatherer.personName(pid) << ") = " << p);
            pJoint.addProbability(p);
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
    bool everSeenBefore = this->firstBucketTimes()[pid] != startTime;
    resultBuilder.personFrequency(this->personFrequency(pid), everSeenBefore);
    resultBuilder.build();

    return true;
}

uint64_t CEventRateModel::checksum(bool includeCurrentBucketStats) const
{
    typedef std::map<TStrCRef, uint64_t, maths::COrderings::SLess> TStrCRefUInt64Map;

    uint64_t seed = this->CIndividualModel::checksum(includeCurrentBucketStats);

#define KEY(pid) boost::cref(this->personName(static_cast<std::size_t>(pid)))

    TStrCRefUInt64Map hashes;
    const TDoubleVec &categories = m_ProbabilityPrior.categories();
    const TDoubleVec &concentrations = m_ProbabilityPrior.concentrations();
    for (std::size_t i = 0u; i < categories.size(); ++i)
    {
        uint64_t &hash = hashes[KEY(categories[i])];
        hash = maths::CChecksum::calculate(hash, concentrations[i]);
    }
    if (includeCurrentBucketStats)
    {
        const TFeatureSizeFeatureDataPrVecPrVec &featureData = m_CurrentBucketStats.s_FeatureData;
        for (std::size_t i = 0u; i < featureData.size(); ++i)
        {
            for (std::size_t j = 0u; j < featureData[i].second.size(); ++j)
            {
                uint64_t &hash = hashes[KEY(featureData[i].second[j].first)];
                hash = maths::CChecksum::calculate(hash, featureData[i].second[j].second.s_Count);
            }
        }
    }

#undef KEY

    LOG_TRACE("seed = " << seed);
    LOG_TRACE("hashes = " << core::CContainerPrinter::print(hashes));

    return maths::CChecksum::calculate(seed, hashes);
}

void CEventRateModel::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CEventRateModel");
    this->CIndividualModel::debugMemoryUsage(mem->addChild());
    core::CMemoryDebug::dynamicSize("m_CurrentBucketStats.s_PersonCounts",
                                    m_CurrentBucketStats.s_PersonCounts, mem);
    core::CMemoryDebug::dynamicSize("m_CurrentBucketStats.s_FeatureData",
                                    m_CurrentBucketStats.s_FeatureData, mem);
    core::CMemoryDebug::dynamicSize("m_CurrentBucketStats.s_InterimCorrections",
                                    m_CurrentBucketStats.s_InterimCorrections, mem);
    core::CMemoryDebug::dynamicSize("s_Probabilities", m_Probabilities, mem);
    core::CMemoryDebug::dynamicSize("m_ProbabilityPrior", m_ProbabilityPrior, mem);
}

std::size_t CEventRateModel::memoryUsage(void) const
{
    return this->CIndividualModel::memoryUsage();
}

std::size_t CEventRateModel::staticSize(void) const
{
    return sizeof(*this);
}

std::size_t CEventRateModel::computeMemoryUsage(void) const
{
    std::size_t mem = this->CIndividualModel::computeMemoryUsage();
    mem += core::CMemory::dynamicSize(m_CurrentBucketStats.s_PersonCounts);
    mem += core::CMemory::dynamicSize(m_CurrentBucketStats.s_FeatureData);
    mem += core::CMemory::dynamicSize(m_CurrentBucketStats.s_InterimCorrections);
    mem += core::CMemory::dynamicSize(m_Probabilities);
    mem += core::CMemory::dynamicSize(m_ProbabilityPrior);
    return mem;
}

CEventRateModel::CModelDetailsViewPtr CEventRateModel::details(void) const
{
    return CModelDetailsViewPtr(new CEventRateModelDetailsView(*this));
}

const CEventRateModel::TFeatureData *CEventRateModel::featureData(model_t::EFeature feature,
                                                                  std::size_t pid,
                                                                  core_t::TTime time) const
{
    return this->CIndividualModel::featureData(feature, pid, time, m_CurrentBucketStats.s_FeatureData);
}

void CEventRateModel::createNewModels(std::size_t n, std::size_t m)
{
    this->CIndividualModel::createNewModels(n, m);
}

void CEventRateModel::updateRecycledModels(void)
{
    this->CIndividualModel::updateRecycledModels();
}

void CEventRateModel::clearPrunedResources(const TSizeVec &people,
                                           const TSizeVec &attributes)
{
    CDataGatherer &gatherer = this->dataGatherer();

    // Stop collecting for these people and add them to the free list.
    gatherer.recyclePeople(people);
    if (gatherer.dataAvailable(m_CurrentBucketStats.s_StartTime))
    {
        gatherer.featureData(m_CurrentBucketStats.s_StartTime,
                             gatherer.bucketLength(),
                             m_CurrentBucketStats.s_FeatureData);
    }

    TDoubleVec categoriesToRemove;
    categoriesToRemove.reserve(people.size());
    for (std::size_t i = 0u; i < people.size(); ++i)
    {
        categoriesToRemove.push_back(static_cast<double>(people[i]));
    }
    m_ProbabilityPrior.removeCategories(categoriesToRemove);
    m_Probabilities = TLessLikelyProbability(m_ProbabilityPrior);

    this->CIndividualModel::clearPrunedResources(people, attributes);
}

void CEventRateModel::sampleCounts(core_t::TTime /*startTime*/, core_t::TTime /*endTime*/)
{
}

void CEventRateModel::correlateData(model_t::EFeature feature,
                                    std::size_t pid,
                                    const CProbabilityAndInfluenceCalculator::SParams &univariateParams,
                                    core_t::TTime time,
                                    core_t::TTime bucketLength,
                                    bool isInterim,
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
    const TDouble1Vec &value = univariateParams.s_Value;
    const TDouble1Vec &sample = univariateParams.s_Sample;
    const TDouble4Vec1Vec &weights = univariateParams.s_Weights;

    params.s_Correlated = this->correlated(feature, pid);
    this->correlatePriors(feature, pid, params.s_Correlated, params.s_Variables, params.s_Priors);
    params.s_BucketEmpty.push_back(univariateParams.s_BucketEmpty);
    params.s_ProbabilityBucketEmpty.push_back(univariateParams.s_ProbabilityBucketEmpty);

    static const std::size_t N = PROBABILITY_WEIGHT_STYLES.size();

    core_t::TTime sampleTime  = model_t::sampleTime(feature, time, bucketLength);
    core_t::TTime elapsedTime = sampleTime - this->firstBucketTimes()[pid];

    // Declared outside loop to minimize number of times they are created.
    TDouble1Vec influenceValue;
    TDouble1Vec influenceCount(2);

    for (std::size_t i = 0u, end = 0u; i < params.s_Correlated.size(); ++i)
    {
        std::size_t correlatePid = params.s_Correlated[i];

        const TFeatureData *datai;
        TDouble1Vec si;
        TDouble4Vec1Vec wi(1);
        if (!this->sampleAndWeights(feature, correlatePid, time, bucketLength, datai, si, wi))
        {
            continue;
        }
        TDecompositionCPtr1Vec ti = this->trend(feature, correlatePid);
        core_t::TTime eti = sampleTime - this->firstBucketTimes()[correlatePid];
        const TStrCRefDouble1VecDoublePrPrVecVec &influenceValuesi = datai->s_InfluenceValues;

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
        params.s_Priors[end]     = params.s_Priors[i];
        params.s_Values[end][v0] = value[0];
        params.s_Values[end][v1] = model_t::offsetCountToZero(feature, static_cast<double>(datai->s_Count));
        params.s_Counts[end][v0] = params.s_Counts[end][v1] = 1.0;
        params.s_ElapsedTimes[end][v0] = elapsedTime;
        params.s_ElapsedTimes[end][v1] = eti;
        params.s_Times[end][v0]   = params.s_Times[end][v1] = sampleTime;
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

bool CEventRateModel::sampleAndWeights(model_t::EFeature feature,
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

    time = model_t::sampleTime(feature, time, bucketLength);
    double count = static_cast<double>(data->s_Count);
    TDouble1Vec value(1, model_t::offsetCountToZero(feature, count));

    sample = this->detrend(feature, pid, time, SEASONAL_CONFIDENCE_INTERVAL, value);
    weights[0] = this->seasonalVarianceScale(feature, pid, time, SEASONAL_CONFIDENCE_INTERVAL).second;

    return true;
}

core_t::TTime CEventRateModel::currentBucketStartTime(void) const
{
    return m_CurrentBucketStats.s_StartTime;
}

void CEventRateModel::currentBucketStartTime(core_t::TTime time)
{
    m_CurrentBucketStats.s_StartTime = time;
}

CIndividualModel::TFeatureSizeSizeTripleDouble1VecUMap &
    CEventRateModel::currentBucketInterimCorrections(void) const
{
    return m_CurrentBucketStats.s_InterimCorrections;
}

uint64_t CEventRateModel::currentBucketTotalCount(void) const
{
    return m_CurrentBucketStats.s_TotalCount;
}

const CEventRateModel::TSizeUInt64PrVec &CEventRateModel::currentBucketPersonCounts(void) const
{
    return m_CurrentBucketStats.s_PersonCounts;
}

CEventRateModel::TSizeUInt64PrVec &CEventRateModel::currentBucketPersonCounts(void)
{
    return m_CurrentBucketStats.s_PersonCounts;
}

void CEventRateModel::currentBucketTotalCount(uint64_t totalCount)
{
    m_CurrentBucketStats.s_TotalCount = totalCount;
}

////////// CEventRateModel::SBucketStats Implementation //////////

CEventRateModel::SBucketStats::SBucketStats(core_t::TTime startTime) :
        s_StartTime(startTime),
        s_TotalCount(0),
        s_InterimCorrections(1)
{
}

}
}
