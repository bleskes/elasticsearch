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

#include <model/CModelConfig.h>

#include <core/CContainerPrinter.h>
#include <core/CStrCaseCmp.h>

#include <maths/Constants.h>
#include <maths/CTools.h>

#include <model/CCountingModelFactory.h>
#include <model/CDetectionRule.h>
#include <model/CEventRateModelFactory.h>
#include <model/CEventRatePeersModelFactory.h>
#include <model/CEventRatePopulationModelFactory.h>
#include <model/CLimits.h>
#include <model/CMetricModelFactory.h>
#include <model/CMetricPopulationModelFactory.h>
#include <model/CSearchKey.h>
#include <model/FunctionTypes.h>
#include <core/CRegex.h>

#include <boost/property_tree/ini_parser.hpp>
#include <boost/property_tree/ptree.hpp>
#include <boost/range.hpp>

#include <fstream>

namespace prelert
{
namespace model
{

namespace
{

typedef std::vector<std::size_t> TSizeVec;
typedef std::vector<core_t::TTime> TTimeVec;

const CModelConfig::TIntDetectionRuleVecUMap EMPTY_RULES_MAP;

namespace detail
{

core_t::TTime validateBucketLength(core_t::TTime length)
{
    // A zero or negative length is used by the individual commands to request
    // the default length - this avoids the need for the commands to know the
    // default length
    return length <= 0 ? CModelConfig::DEFAULT_BUCKET_LENGTH : length;
}

}

}

const std::string CModelConfig::DEFAULT_MULTIVARIATE_COMPONENT_DELIMITER(",");
const core_t::TTime CModelConfig::DEFAULT_BUCKET_LENGTH(300);
const std::size_t CModelConfig::DEFAULT_LATENCY_BUCKETS(0);
const std::size_t CModelConfig::DEFAULT_SAMPLE_COUNT_FACTOR_NO_LATENCY(1);
const std::size_t CModelConfig::DEFAULT_SAMPLE_COUNT_FACTOR_WITH_LATENCY(10);
const double CModelConfig::DEFAULT_SAMPLE_QUEUE_GROWTH_FACTOR(0.1);
const core_t::TTime CModelConfig::DEFAULT_BATCH_LENGTH(3600);
const std::size_t CModelConfig::DEFAULT_OVERLAP(0u);
const std::size_t CModelConfig::APERIODIC(1u);
const core_t::TTime CModelConfig::STANDARD_BUCKET_LENGTH(1800);
const double CModelConfig::DEFAULT_DECAY_RATE(0.001);
const double CModelConfig::DEFAULT_INITIAL_DECAY_RATE_MULTIPLIER(4.0);
const double CModelConfig::DEFAULT_LEARN_RATE(1.0);
const double CModelConfig::DEFAULT_INDIVIDUAL_MINIMUM_MODE_FRACTION(0.05);
const double CModelConfig::DEFAULT_POPULATION_MINIMUM_MODE_FRACTION(0.05);
const double CModelConfig::DEFAULT_MINIMUM_CLUSTER_SPLIT_COUNT(12.0);
const double CModelConfig::DEFAULT_CUTOFF_TO_MODEL_EMPTY_BUCKETS(0.2);
const double CModelConfig::DEFAULT_CATEGORY_DELETE_FRACTION(0.8);
const std::size_t CModelConfig::DEFAULT_COMPONENT_SIZE(36u);
const std::size_t CModelConfig::DEFAULT_TOTAL_PROBABILITY_CALC_SAMPLING_SIZE(10u);
const double CModelConfig::DEFAULT_MAXIMUM_UPDATES_PER_BUCKET(1.0);
const double CModelConfig::DEFAULT_INFLUENCE_CUTOFF(0.5);
const double CModelConfig::DEFAULT_AGGREGATION_STYLE_PARAMS[][model_t::NUMBER_AGGREGATION_PARAMS] =
    {
        { 0.0, 1.0, 1.0, 1.0 },
        { 0.5, 0.5, 1.0, 5.0 },
        { 0.5, 0.5, 1.0, 1.0 }
    };
// The default for maximumanomalousprobability now matches the default
// for unusualprobabilitythreshold in prelertlimits.conf - this avoids
// inconsistencies in output
const double CModelConfig::DEFAULT_MAXIMUM_ANOMALOUS_PROBABILITY(0.035);
const double CModelConfig::DEFAULT_NOISE_PERCENTILE(50.0);
const double CModelConfig::DEFAULT_NOISE_MULTIPLIER(1.0);
const std::size_t CModelConfig::DEFAULT_BUCKET_RESULTS_DELAY(0);
const CModelConfig::TDoubleDoublePr CModelConfig::DEFAULT_NORMALIZED_SCORE_KNOT_POINTS[9] =
    {
        CModelConfig::TDoubleDoublePr(0.0, 0.0),
        CModelConfig::TDoubleDoublePr(70.0, 1.0),
        CModelConfig::TDoubleDoublePr(85.0, 1.2),
        CModelConfig::TDoubleDoublePr(90.0, 1.5),
        CModelConfig::TDoubleDoublePr(95.0, 3.0),
        CModelConfig::TDoubleDoublePr(97.0, 20.0),
        CModelConfig::TDoubleDoublePr(99.0, 50.0),
        CModelConfig::TDoubleDoublePr(99.9, 90.0),
        CModelConfig::TDoubleDoublePr(100.0, 100.0)
    };
const double CModelConfig::DEFAULT_PRIOR_OFFSET(0.2);
const std::size_t CModelConfig::DEFAULT_RESAMPLING_MAX_SAMPLES(40u);
const double CModelConfig::DEFAULT_PRUNE_WINDOW_SCALE_MINIMUM(0.5);
const double CModelConfig::DEFAULT_PRUNE_WINDOW_SCALE_MAXIMUM(4.0);
const double CModelConfig::DEFAULT_CORRELATION_MODELS_OVERHEAD(3.0);
const double CModelConfig::DEFAULT_MINIMUM_SIGNIFICANT_CORRELATION(0.3);

CModelConfig CModelConfig::defaultConfig(core_t::TTime bucketLength,
                                         core_t::TTime batchLength,
                                         std::size_t period,
                                         model_t::ESummaryMode summaryMode,
                                         const std::string &summaryCountFieldName,
                                         core_t::TTime latency,
                                         std::size_t bucketResultsDelay,
                                         bool multivariateByFields,
                                         const std::string &multipleBucketLengths)
{
    bucketLength = detail::validateBucketLength(bucketLength);
    if (batchLength <= 0)
    {
        batchLength = bucketLength;
    }

    double learnRate = DEFAULT_LEARN_RATE * bucketNormalizationFactor(bucketLength);
    double decayRate = DEFAULT_DECAY_RATE * bucketNormalizationFactor(bucketLength);

    SModelParams params(bucketLength);
    params.s_LearnRate = learnRate;
    params.s_DecayRate = decayRate;
    params.s_ExcludeFrequent = model_t::E_XF_None;
    params.configureLatency(latency, bucketLength);
    params.s_BucketResultsDelay = bucketResultsDelay;
    params.s_MultipleBucketLengths = CModelConfig::multipleBucketLengths(bucketLength, multipleBucketLengths);

    TFactoryTypeFactoryPtrMap factories;
    params.s_MinimumModeFraction = DEFAULT_INDIVIDUAL_MINIMUM_MODE_FRACTION;
    factories[E_EventRateFactory].reset(
            new CEventRateModelFactory(params, summaryMode, summaryCountFieldName));
    factories[E_MetricFactory].reset(
            new CMetricModelFactory(params, summaryMode, summaryCountFieldName));
    factories[E_EventRatePopulationFactory].reset(
            new CEventRatePopulationModelFactory(params, summaryMode, summaryCountFieldName));
    params.s_MinimumModeFraction = DEFAULT_POPULATION_MINIMUM_MODE_FRACTION;
    factories[E_MetricPopulationFactory].reset(
            new CMetricPopulationModelFactory(params, summaryMode, summaryCountFieldName));
    factories[E_EventRatePeersFactory].reset(
            new CEventRatePeersModelFactory(params, summaryMode, summaryCountFieldName));
    params.s_MinimumModeFraction = 1.0;
    factories[E_CountingFactory].reset(
            new CCountingModelFactory(params, summaryMode, summaryCountFieldName));

    CModelConfig result;
    result.bucketLength(bucketLength);
    result.batchLength(batchLength);
    result.period(period);
    result.bucketResultsDelay(bucketResultsDelay);
    result.multivariateByFields(multivariateByFields);
    result.factories(factories);
    return result;
}

// De-rates the decay and learn rate to account for differences from the
// standard bucket length.
double CModelConfig::bucketNormalizationFactor(core_t::TTime bucketLength)
{
    return std::min(1.0,  static_cast<double>(bucketLength)
                        / static_cast<double>(STANDARD_BUCKET_LENGTH));
}

// Standard decay rate for time series decompositions given the specified
// model decay rate and bucket length.
double CModelConfig::trendDecayRate(double modelDecayRate, core_t::TTime bucketLength)
{
    return std::min(0.5 * modelDecayRate
                        / bucketNormalizationFactor(bucketLength)
                        * static_cast<double>(core::constants::DAY)
                        / static_cast<double>(std::max(bucketLength, STANDARD_BUCKET_LENGTH)), 0.1);
}

CModelConfig::CModelConfig(void) :
        m_BucketLength(STANDARD_BUCKET_LENGTH),
        m_BatchLength(STANDARD_BUCKET_LENGTH),
        m_BatchOverlap(DEFAULT_OVERLAP),
        // Even though period is a size_t, set its "infinity"
        // value as though it were signed because there are
        // places where it's cast to a core_t::TTime
        m_Period(std::numeric_limits<std::size_t>::max() >> 1),
        m_BucketResultsDelay(DEFAULT_BUCKET_RESULTS_DELAY),
        m_MultivariateByFields(false),
        m_ModelDebugDestination(E_File),
        m_ModelDebugBoundsPercentile(-1.0),
        m_MaximumAnomalousProbability(DEFAULT_MAXIMUM_ANOMALOUS_PROBABILITY),
        m_NoisePercentile(DEFAULT_NOISE_PERCENTILE),
        m_NoiseMultiplier(DEFAULT_NOISE_MULTIPLIER),
        m_NormalizedScoreKnotPoints(boost::begin(DEFAULT_NORMALIZED_SCORE_KNOT_POINTS),
                                    boost::end(DEFAULT_NORMALIZED_SCORE_KNOT_POINTS)),
        m_PerPartitionNormalisation(false),
        m_DetectionRules(EMPTY_RULES_MAP)
{
    for (std::size_t i = 0u; i < model_t::NUMBER_AGGREGATION_STYLES; ++i)
    {
        for (std::size_t j = 0u; j < model_t::NUMBER_AGGREGATION_PARAMS; ++j)
        {
            m_AggregationStyleParams[i][j] = DEFAULT_AGGREGATION_STYLE_PARAMS[i][j];
        }
    }
}

void CModelConfig::bucketLength(core_t::TTime length)
{
    m_BucketLength = length;
    // update factories' bucketlengths too
    //TFactoryTypeFactoryPtrMap;
    for (TFactoryTypeFactoryPtrMapItr i = m_Factories.begin(); i != m_Factories.end(); ++i)
    {
        i->second->updateBucketLength(length);
    }
}

void CModelConfig::batchLength(core_t::TTime length)
{
    m_BatchLength = length;
}

void CModelConfig::batchOverlap(std::size_t overlap)
{
    m_BatchOverlap = overlap;
}

void CModelConfig::period(std::size_t period)
{
    m_Period = period;
}

void CModelConfig::bucketResultsDelay(std::size_t delay)
{
    m_BucketResultsDelay = delay;
}

CModelConfig::TTimeVec CModelConfig::multipleBucketLengths(core_t::TTime bucketLength,
                                                           const std::string &multipleBucketLengths)
{
    TStrVec multiBucketTokens;
    core::CRegex regex;
    regex.init(",");
    regex.split(multipleBucketLengths, multiBucketTokens);
    TTimeVec multiBuckets;
    for (TStrVecCItr itr = multiBucketTokens.begin(); itr != multiBucketTokens.end(); ++itr)
    {
        core_t::TTime t = 0;
        if (core::CStringUtils::stringToType(*itr, t))
        {
            if ((t <= bucketLength) || (t % bucketLength != 0))
            {
                LOG_ERROR("MultipleBucketLength " << t << " must be a multiple of " << bucketLength);
                return TTimeVec();
            }
            multiBuckets.push_back(t);
        }
    }
    std::sort(multiBuckets.begin(), multiBuckets.end());
    return multiBuckets;
}

void CModelConfig::multivariateByFields(bool enabled)
{
    m_MultivariateByFields = enabled;
}

void CModelConfig::factories(const TFactoryTypeFactoryPtrMap &factories)
{
    m_Factories = factories;
}

bool CModelConfig::aggregationStyleParams(model_t::EAggregationStyle style,
                                          model_t::EAggregationParam param,
                                          double value)
{
    switch (param)
    {
    case model_t::E_JointProbabilityWeight:
        if (value < 0.0 || value > 1.0)
        {
            LOG_ERROR("joint probability weight " << value << " out of in range [0,1]");
            return false;
        }
        m_AggregationStyleParams[style][model_t::E_JointProbabilityWeight] = value;
        break;
    case model_t::E_ExtremeProbabilityWeight:
        if (value < 0.0 || value > 1.0)
        {
            LOG_ERROR("extreme probability weight " << value << " out of in range [0,1]");
            return false;
        }
        m_AggregationStyleParams[style][model_t::E_ExtremeProbabilityWeight] = value;
        break;
    case model_t::E_MinExtremeSamples:
        if (value < 1.0 || value > 10.0)
        {
            LOG_ERROR("min extreme samples " << value << " out of in range [0,10]");
            return false;
        }
        m_AggregationStyleParams[style][model_t::E_MinExtremeSamples] = value;
        m_AggregationStyleParams[style][model_t::E_MaxExtremeSamples] =
                std::max(value, m_AggregationStyleParams[style][model_t::E_MaxExtremeSamples]);
        break;
    case model_t::E_MaxExtremeSamples:
        if (value < 1.0 || value > 10.0)
        {
            LOG_ERROR("max extreme samples " << value << " out of in range [0,10]");
            return false;
        }
        m_AggregationStyleParams[style][model_t::E_MaxExtremeSamples] = value;
        m_AggregationStyleParams[style][model_t::E_MinExtremeSamples] =
                std::min(value, m_AggregationStyleParams[style][model_t::E_MinExtremeSamples]);
        break;
    }
    return true;
}

void CModelConfig::maximumAnomalousProbability(double probability)
{
    double minimum = 100 * maths::MINUSCULE_PROBABILITY;
    if (probability < minimum || probability > 1.0)
    {
        LOG_INFO("Maximum anomalous probability " << probability
                 << " out of range [" << minimum << "," << 1.0 << "] truncating");
    }
    m_MaximumAnomalousProbability = maths::CTools::truncate(probability, minimum, 1.0);
}

bool CModelConfig::noisePercentile(double percentile)
{
    if (percentile < 0.0 || percentile > 100.0)
    {
        LOG_ERROR("Noise percentile " << percentile << " out of range [0, 100]");
        return false;
    }
    m_NoisePercentile = percentile;
    return true;
}

bool CModelConfig::noiseMultiplier(double multiplier)
{
    if (multiplier <= 0.0)
    {
        LOG_ERROR("Noise multiplier must be positive");
        return false;
    }
    m_NoiseMultiplier = multiplier;
    return true;
}

bool CModelConfig::normalizedScoreKnotPoints(const TDoubleDoublePrVec &points)
{
    if (points.empty())
    {
        LOG_ERROR("Must provide at least two know points");
        return false;
    }
    if (points[0].first != 0.0 && points[0].second != 0.0)
    {
        LOG_ERROR("First knot point must be (0,0)");
        return false;
    }
    if (points.back().first != 100.0 && points.back().second != 100.0)
    {
        LOG_ERROR("Last knot point must be (100,100)");
        return false;
    }
    for (std::size_t i = 0u; i < points.size(); i += 2)
    {
        if (points[i].first < 0.0 || points[i].first > 100.0)
        {
            LOG_ERROR("Unexpected value " << points[i].first << " for percentile");
            return false;
        }
        if (points[i].second < 0.0 || points[i].second > 100.0)
        {
            LOG_ERROR("Unexpected value " << points[i].second << " for score");
            return false;
        }
    }
    if (!boost::algorithm::is_sorted(points.begin(),
                                     points.end(),
                                     maths::COrderings::SFirstLess()))
    {
        LOG_ERROR("Percentiles must be monotonic increasing "
                  << core::CContainerPrinter::print(points));
        return false;
    }
    if (!boost::algorithm::is_sorted(points.begin(),
                                     points.end(),
                                     maths::COrderings::SSecondLess()))
    {
        LOG_ERROR("Scores must be monotonic increasing "
                  << core::CContainerPrinter::print(points));
        return false;
    }

    m_NormalizedScoreKnotPoints = points;
    m_NormalizedScoreKnotPoints.erase(std::unique(m_NormalizedScoreKnotPoints.begin(),
                                                  m_NormalizedScoreKnotPoints.end()),
                                      m_NormalizedScoreKnotPoints.end());
    return true;
}

bool CModelConfig::init(const std::string &configFile)
{
    boost::property_tree::ptree propTree;
    return this->init(configFile, propTree);
}

bool CModelConfig::init(const std::string &configFile,
                        boost::property_tree::ptree &propTree)
{
    LOG_DEBUG("Reading config file " << configFile);

    try
    {
        std::ifstream strm(configFile.c_str());
        if (!strm.is_open())
        {
            LOG_ERROR("Error opening config file " << configFile);
            return false;
        }
        CLimits::skipUtf8Bom(strm);

        boost::property_tree::ini_parser::read_ini(strm, propTree);
    }
    catch (boost::property_tree::ptree_error &e)
    {
        LOG_ERROR("Error reading config file " << configFile << " : " << e.what());
        return false;
    }

    if (this->init(propTree) == false)
    {
        LOG_ERROR("Error reading config file " << configFile);
        return false;
    }

    return true;
}

bool CModelConfig::init(const boost::property_tree::ptree &propTree)
{
    static const std::string MODEL_STANZA("model");
    static const std::string ANOMALY_SCORE_STANZA("anomalyscore");

    bool result = true;

    for (boost::property_tree::ptree::const_iterator i = propTree.begin();
         i != propTree.end();
         ++i)
    {
        const std::string &stanzaName = i->first;
        const boost::property_tree::ptree &propertyTree = i->second;

        if (stanzaName == MODEL_STANZA)
        {
            if (this->processStanza(propertyTree) == false)
            {
                LOG_ERROR("Error reading model config stanza: " << MODEL_STANZA);
                result = false;
            }
        }
        else if (stanzaName == ANOMALY_SCORE_STANZA)
        {
            if (this->processStanza(propertyTree) == false)
            {
                LOG_ERROR("Error reading model config stanza: " << ANOMALY_SCORE_STANZA);
                result = false;
            }
        }
        else
        {
            LOG_WARN("Ignoring unknown model config stanza: " << stanzaName);
        }
    }

    return result;
}

bool CModelConfig::configureDebug(const std::string &debugConfigFile)
{
    LOG_DEBUG("Reading config file " << debugConfigFile);

    boost::property_tree::ptree propTree;
    try
    {
        std::ifstream strm(debugConfigFile.c_str());
        if (!strm.is_open())
        {
            LOG_ERROR("Error opening config file " << debugConfigFile);
            return false;
        }
        CLimits::skipUtf8Bom(strm);

        boost::property_tree::ini_parser::read_ini(strm, propTree);
    }
    catch (boost::property_tree::ptree_error &e)
    {
        LOG_ERROR("Error reading debug config file " << debugConfigFile << " : " << e.what());
        return false;
    }

    if (this->configureDebug(propTree) == false)
    {
        LOG_ERROR("Error reading debug config file " << debugConfigFile);
        return false;
    }

    return true;
}

namespace
{
// Model debug config properties
const std::string WRITE_TO("writeto");
const std::string BOUNDS_PERCENTILE_PROPERTY("boundspercentile");
const std::string TERMS_PROPERTY("terms");
}

bool CModelConfig::configureDebug(const boost::property_tree::ptree &propTree)
{
    this->modelDebugDestination(propTree.get(WRITE_TO, std::string()));

    try
    {
        std::string valueStr(propTree.get<std::string>(BOUNDS_PERCENTILE_PROPERTY));
        if (core::CStringUtils::stringToType(valueStr, m_ModelDebugBoundsPercentile) == false)
        {
            LOG_ERROR("Cannot parse as double: " << valueStr);
            return false;
        }
    }
    catch (boost::property_tree::ptree_error &)
    {
        LOG_ERROR("Error reading model debug config. Property '"
                << BOUNDS_PERCENTILE_PROPERTY << "' is missing");
        return false;
    }

    m_ModelDebugTerms.clear();
    try
    {
        std::string valueStr(propTree.get<std::string>(TERMS_PROPERTY));

        typedef core::CStringUtils::TStrVec TStrVec;
        TStrVec tokens;
        std::string remainder;
        core::CStringUtils::tokenise(",", valueStr, tokens, remainder);
        if (!remainder.empty())
        {
            tokens.push_back(remainder);
        }
        for (std::size_t i = 0; i < tokens.size(); ++i)
        {
            m_ModelDebugTerms.insert(tokens[i]);
        }
    }
    catch (boost::property_tree::ptree_error &)
    {
        LOG_ERROR("Error reading model debug config. Property '"
                << TERMS_PROPERTY << "' is missing");
        return false;
    }

    return true;
}

CModelConfig::TModelFactoryCPtr
CModelConfig::factory(const CSearchKey &key,
                      const model_t::TAnyPersistFunc &extraDataPersistFunc,
                      const model_t::TAnyRestoreFunc &extraDataRestoreFunc,
                      const model_t::TAnyMemoryFunc &extraDataMemoryFunc) const
{
    TModelFactoryCPtr result = m_FactoryCache[key];
    if (!result)
    {
        result = key.isSimpleCount() ?
                 this->factory(key.identifier(),
                               key.function(),
                               true,
                               key.excludeFrequent(),
                               key.partitionFieldName(),
                               key.overFieldName(),
                               key.byFieldName(),
                               key.fieldName(),
                               key.influenceFieldNames(),
                               model_t::TAnyPersistFunc(),
                               model_t::TAnyRestoreFunc(),
                               model_t::TAnyMemoryFunc()) :
                 this->factory(key.identifier(),
                               key.function(),
                               key.useNull(),
                               key.excludeFrequent(),
                               key.partitionFieldName(),
                               key.overFieldName(),
                               key.byFieldName(),
                               key.fieldName(),
                               key.influenceFieldNames(),
                               extraDataPersistFunc,
                               extraDataRestoreFunc,
                               extraDataMemoryFunc);
    }
    return result;
}

CModelConfig::TModelFactoryCPtr
CModelConfig::factory(int identifier,
                      function_t::EFunction function,
                      bool useNull,
                      model_t::EExcludeFrequent excludeFrequent,
                      const std::string &partitionFieldName,
                      const std::string &overFieldName,
                      const std::string &byFieldName,
                      const std::string &valueFieldName,
                      const CSearchKey::TStrPtrVec &influenceFieldNames,
                      const model_t::TAnyPersistFunc &extraDataPersistFunc,
                      const model_t::TAnyRestoreFunc &extraDataRestoreFunc,
                      const model_t::TAnyMemoryFunc &extraDataMemoryFunc) const
{
    const TFeatureVec &features = function_t::features(function);

    // Simple state machine to deduce the factory type from
    // a collection of features.
    EFactoryType factory = E_UnknownFactory;
    for (std::size_t i = 0u; i < features.size(); ++i)
    {
        switch (factory)
        {
        case E_EventRateFactory:
            switch (model_t::analysisCategory(features[i]))
            {
            case model_t::E_EventRate:
                break;
            case model_t::E_Metric:
                factory = E_MetricFactory;
                break;
            case model_t::E_PopulationEventRate:
            case model_t::E_PopulationMetric:
            case model_t::E_PeersEventRate:
            case model_t::E_PeersMetric:
                factory = E_BadFactory;
                break;
            }
            break;

        case E_MetricFactory:
            switch (model_t::analysisCategory(features[i]))
            {
            case model_t::E_EventRate:
            case model_t::E_Metric:
                break;
            case model_t::E_PopulationEventRate:
            case model_t::E_PopulationMetric:
            case model_t::E_PeersEventRate:
            case model_t::E_PeersMetric:
                factory = E_BadFactory;
                break;
            }
            break;

        case E_EventRatePopulationFactory:
            switch (model_t::analysisCategory(features[i]))
            {
            case model_t::E_EventRate:
            case model_t::E_Metric:
                factory = E_BadFactory;
                break;
            case model_t::E_PopulationEventRate:
                break;
            case model_t::E_PopulationMetric:
            case model_t::E_PeersEventRate:
            case model_t::E_PeersMetric:
                factory = E_BadFactory;
                break;
            }
            break;

        case E_MetricPopulationFactory:
            switch (model_t::analysisCategory(features[i]))
            {
            case model_t::E_EventRate:
            case model_t::E_Metric:
            case model_t::E_PopulationEventRate:
                factory = E_BadFactory;
                break;
            case model_t::E_PopulationMetric:
                factory = E_MetricPopulationFactory;
                break;
            case model_t::E_PeersEventRate:
            case model_t::E_PeersMetric:
                factory = E_BadFactory;
                break;
            }
            break;

        case E_EventRatePeersFactory:
            switch (model_t::analysisCategory(features[i]))
            {
            case model_t::E_EventRate:
            case model_t::E_Metric:
            case model_t::E_PopulationEventRate:
            case model_t::E_PopulationMetric:
                factory = E_BadFactory;
                break;
            case model_t::E_PeersEventRate:
                break;
            case model_t::E_PeersMetric:
                factory = E_BadFactory;
                break;
            }
            break;

        case E_CountingFactory:
            switch (model_t::analysisCategory(features[i]))
            {
            case model_t::E_EventRate:
            case model_t::E_Metric:
            case model_t::E_PopulationEventRate:
            case model_t::E_PopulationMetric:
            case model_t::E_PeersEventRate:
            case model_t::E_PeersMetric:
                factory = E_BadFactory;
                break;
            }
            break;

        case E_UnknownFactory:
            switch (model_t::analysisCategory(features[i]))
            {
            case model_t::E_EventRate:
                factory = CSearchKey::isSimpleCount(function, byFieldName) ?
                          E_CountingFactory :
                          E_EventRateFactory;
                break;
            case model_t::E_Metric:
                factory = E_MetricFactory;
                break;
            case model_t::E_PopulationEventRate:
                factory = E_EventRatePopulationFactory;
                break;
            case model_t::E_PopulationMetric:
                factory = E_MetricPopulationFactory;
                break;
            case model_t::E_PeersEventRate:
                factory = E_EventRatePeersFactory;
                break;
            case model_t::E_PeersMetric:
                // TODO
                factory = E_BadFactory;
                break;
            }
            break;

        case E_BadFactory:
            break;
        }
    }

    TFactoryTypeFactoryPtrMapCItr prototype = m_Factories.find(factory);
    if (prototype == m_Factories.end())
    {
        LOG_ABORT("No factory for features = "
                  << core::CContainerPrinter::print(features));
    }

    TModelFactoryPtr result(prototype->second->clone());
    result->identifier(identifier);
    TStrVec influences;
    influences.reserve(influenceFieldNames.size());
    for (CSearchKey::TStrPtrVec::const_iterator i = influenceFieldNames.begin();
         i != influenceFieldNames.end(); ++i)
    {
        influences.push_back(*(*i));
    }
    result->fieldNames(partitionFieldName,
                       overFieldName,
                       byFieldName,
                       valueFieldName,
                       influences);
    result->useNull(useNull);
    result->excludeFrequent(excludeFrequent);
    result->features(features);
    result->bucketResultsDelay(m_BucketResultsDelay);
    result->multivariateByFields(m_MultivariateByFields);
    result->extraDataConversionFuncs(extraDataPersistFunc,
                                     extraDataRestoreFunc,
                                     extraDataMemoryFunc);
    TIntDetectionRuleVecUMapCItr rulesItr = m_DetectionRules.get().find(identifier);
    if (rulesItr != m_DetectionRules.get().end())
    {
        result->detectionRules(TDetectionRuleVecCRef(rulesItr->second));
    }

    return result;
}

double CModelConfig::decayRate(void) const
{
    return m_Factories.begin()->second->modelParams().s_DecayRate;
}

core_t::TTime CModelConfig::bucketLength(void) const
{
    return m_BucketLength;
}

core_t::TTime CModelConfig::latency(void) const
{
    return m_BucketLength * m_Factories.begin()->second->modelParams().s_LatencyBuckets;
}

std::size_t CModelConfig::latencyBuckets(void) const
{
    return m_Factories.begin()->second->modelParams().s_LatencyBuckets;
}

core_t::TTime CModelConfig::batchLength(void) const
{
    return m_BatchLength;
}

std::size_t CModelConfig::batchSize(void) const
{
    return static_cast<std::size_t>(this->batchLength() / m_BucketLength);
}

std::size_t CModelConfig::batchOverlap(void) const
{
    return m_BatchOverlap;
}

std::size_t CModelConfig::period(void) const
{
    return m_Period;
}

void CModelConfig::modelDebugDestination(EDebugDestination destination)
{
    m_ModelDebugDestination = destination;
}

void CModelConfig::modelDebugDestination(const std::string &destination)
{
    // Empty string explicitly means no change - don't log a warning
    if (destination.empty())
    {
        return;
    }

    if (core::CStrCaseCmp::strCaseCmp(destination.c_str(), "file") == 0)
    {
        m_ModelDebugDestination = E_File;
    }
    else if (core::CStrCaseCmp::strCaseCmp(destination.c_str(), "data_store") == 0)
    {
        m_ModelDebugDestination = E_DataStore;
    }
    else
    {
        LOG_WARN("Model debug destination '" << destination << "' "
                 "not understood - no change made");
    }
}

CModelConfig::EDebugDestination CModelConfig::modelDebugDestination(void) const
{
    return m_ModelDebugDestination;
}

std::size_t CModelConfig::bucketResultsDelay(void) const
{
    return m_BucketResultsDelay;
}

bool CModelConfig::multivariateByFields(void) const
{
    return m_MultivariateByFields;
}

void CModelConfig::modelDebugBoundsPercentile(double percentile)
{
    if (percentile < 0.0 || percentile >= 100.0)
    {
        LOG_ERROR("Bad confidence interval");
        return;
    }
    m_ModelDebugBoundsPercentile = percentile;
}

double CModelConfig::modelDebugBoundsPercentile(void) const
{
    return m_ModelDebugBoundsPercentile;
}

void CModelConfig::modelDebugTerms(TStrSet terms)
{
    m_ModelDebugTerms.swap(terms);
}

const CModelConfig::TStrSet &CModelConfig::modelDebugTerms(void) const
{
    return m_ModelDebugTerms;
}

double CModelConfig::aggregationStyleParam(model_t::EAggregationStyle style,
                                           model_t::EAggregationParam param) const
{
    return m_AggregationStyleParams[style][param];
}

double CModelConfig::maximumAnomalousProbability(void) const
{
    return m_MaximumAnomalousProbability;
}

double CModelConfig::noisePercentile(void) const
{
    return m_NoisePercentile;
}

double CModelConfig::noiseMultiplier(void) const
{
    return m_NoiseMultiplier;
}

const CModelConfig::TDoubleDoublePrVec &
CModelConfig::normalizedScoreKnotPoints(void) const
{
    return m_NormalizedScoreKnotPoints;
}

bool CModelConfig::perPartitionNormalization(void) const
{
    return m_PerPartitionNormalisation;
}

void CModelConfig::perPartitionNormalization(bool value)
{
    m_PerPartitionNormalisation = value;
}

void CModelConfig::detectionRules(TIntDetectionRuleVecUMapCRef detectionRules)
{
    m_DetectionRules = detectionRules;
}

namespace
{
const std::string BATCH_OVERLAP_PROPERTY("batchoverlap");
const std::string ONLINE_LEARN_RATE_PROPERTY("learnrate");
const std::string DECAY_RATE_PROPERTY("decayrate");
const std::string INITIAL_DECAY_RATE_MULTIPLIER_PROPERTY("initialdecayratemultiplier");
const std::string MAXIMUM_UPDATES_PER_BUCKET_PROPERTY("maximumupdatesperbucket");
const std::string TOTAL_PROBABILITY_CALC_SAMPLING_SIZE_PROPERTY("totalprobabilitycalcsamplingsize");
const std::string INDIVIDUAL_MODE_FRACTION_PROPERTY("individualmodefraction");
const std::string POPULATION_MODE_FRACTION_PROPERTY("populationmodefraction");
const std::string PEERS_MODE_FRACTION_PROPERTY("peersmodefraction");
const std::string COMPONENT_SIZE_PROPERTY("componentsize");
const std::string SAMPLE_COUNT_FACTOR_PROPERTY("samplecountfactor");
const std::string PRUNE_WINDOW_SCALE_MINIMUM("prunewindowscaleminimum");
const std::string PRUNE_WINDOW_SCALE_MAXIMUM("prunewindowscalemaximum");
const std::string AGGREGATION_STYLE_PARAMS("aggregationstyleparams");
const std::string MAXIMUM_ANOMALOUS_PROBABILITY_PROPERTY("maximumanomalousprobability");
const std::string NOISE_PERCENTILE_PROPERTY("noisepercentile");
const std::string NOISE_MULTIPLIER_PROPERTY("noisemultiplier");
const std::string NORMALIZED_SCORE_KNOT_POINTS("normalizedscoreknotpoints");
const std::string PER_PARTITION_NORMALIZATION_PROPERTY("perPartitionNormalization");
}

bool CModelConfig::processStanza(const boost::property_tree::ptree &propertyTree)
{
    typedef std::vector<std::string> TStrVec;

    bool result = true;

    for (boost::property_tree::ptree::const_iterator i = propertyTree.begin();
         i != propertyTree.end();
         ++i)
    {
        std::string propName = i->first;
        std::string propValue = i->second.data();
        core::CStringUtils::trimWhitespace(propValue);

        if (propName == BATCH_OVERLAP_PROPERTY)
        {
            int overlap;
            if (core::CStringUtils::stringToType(propValue, overlap) == false || overlap < 0)
            {
                LOG_ERROR("Invalid value for property " << propName << " : " << propValue);
                result = false;
                continue;
            }
            this->batchOverlap(overlap);
        }
        else if (propName == ONLINE_LEARN_RATE_PROPERTY)
        {
            double learnRate = DEFAULT_LEARN_RATE;
            if (core::CStringUtils::stringToType(propValue, learnRate) == false || learnRate <= 0.0)
            {
                LOG_ERROR("Invalid value for property " << propName << " : " << propValue);
                result = false;
                continue;
            }

            learnRate *= bucketNormalizationFactor(this->bucketLength());
            for (TFactoryTypeFactoryPtrMapCItr j = m_Factories.begin(); j != m_Factories.end(); ++j)
            {
                j->second->learnRate(learnRate);
            }
        }
        else if (propName == DECAY_RATE_PROPERTY)
        {
            double decayRate = DEFAULT_DECAY_RATE;
            if (core::CStringUtils::stringToType(propValue, decayRate) == false || decayRate <= 0.0)
            {
                LOG_ERROR("Invalid value for property " << propName << " : " << propValue);
                result = false;
                continue;
            }

            decayRate *= bucketNormalizationFactor(this->bucketLength());
            for (TFactoryTypeFactoryPtrMapCItr j = m_Factories.begin(); j != m_Factories.end(); ++j)
            {
                j->second->decayRate(decayRate);
            }
        }
        else if (propName == INITIAL_DECAY_RATE_MULTIPLIER_PROPERTY)
        {
            double multiplier = DEFAULT_INITIAL_DECAY_RATE_MULTIPLIER;
            if (core::CStringUtils::stringToType(propValue, multiplier) == false || multiplier < 1.0)
            {
                LOG_ERROR("Invalid value for property " << propName << " : " << propValue);
                result = false;
                continue;
            }

            for (TFactoryTypeFactoryPtrMapCItr j = m_Factories.begin(); j != m_Factories.end(); ++j)
            {
                j->second->initialDecayRateMultiplier(multiplier);
            }
        }
        else if (propName == MAXIMUM_UPDATES_PER_BUCKET_PROPERTY)
        {
            double maximumUpdatesPerBucket;
            if (   core::CStringUtils::stringToType(propValue, maximumUpdatesPerBucket) == false
                || maximumUpdatesPerBucket < 0.0)
            {
                LOG_ERROR("Invalid value for property " << propName << " : " << propValue);
                result = false;
                continue;
            }

            for (TFactoryTypeFactoryPtrMapCItr j = m_Factories.begin(); j != m_Factories.end(); ++j)
            {
                j->second->maximumUpdatesPerBucket(maximumUpdatesPerBucket);
            }
        }
        else if (propName == TOTAL_PROBABILITY_CALC_SAMPLING_SIZE_PROPERTY)
        {
            int totalProbabilityCalcSamplingSize;
            if (   core::CStringUtils::stringToType(propValue, totalProbabilityCalcSamplingSize) == false
                || totalProbabilityCalcSamplingSize <= 0)
            {
                LOG_ERROR("Invalid value for property " << propName << " : " << propValue);
                result = false;
                continue;
            }
            for (TFactoryTypeFactoryPtrMapCItr j = m_Factories.begin(); j != m_Factories.end(); ++j)
            {
                j->second->totalProbabilityCalcSamplingSize(totalProbabilityCalcSamplingSize);
            }
        }
        else if (propName == INDIVIDUAL_MODE_FRACTION_PROPERTY)
        {
            double fraction;
            if (   core::CStringUtils::stringToType(propValue, fraction) == false
                || fraction < 0.0
                || fraction > 1.0)
            {
                LOG_ERROR("Invalid value for property " << propName << " : " << propValue);
                result = false;
                continue;
            }

            if (m_Factories.count(E_EventRateFactory) > 0)
            {
                m_Factories[E_EventRateFactory]->minimumModeFraction(fraction);
            }
            if (m_Factories.count(E_MetricFactory) > 0)
            {
                m_Factories[E_MetricFactory]->minimumModeFraction(fraction);
            }
        }
        else if (propName == POPULATION_MODE_FRACTION_PROPERTY)
        {
            double fraction;
            if (   core::CStringUtils::stringToType(propValue, fraction) == false
                || fraction < 0.0
                || fraction > 1.0)
            {
                LOG_ERROR("Invalid value for property " << propName << " : " << propValue);
                result = false;
                continue;
            }

            if (m_Factories.count(E_EventRatePopulationFactory) > 0)
            {
                m_Factories[E_EventRatePopulationFactory]->minimumModeFraction(fraction);
            }
            if (m_Factories.count(E_MetricPopulationFactory) > 0)
            {
                m_Factories[E_MetricPopulationFactory]->minimumModeFraction(fraction);
            }
        }
        else if (propName == PEERS_MODE_FRACTION_PROPERTY)
        {
            double fraction;
            if (   core::CStringUtils::stringToType(propValue, fraction) == false
                || fraction < 0.0
                || fraction > 1.0)
            {
                LOG_ERROR("Invalid value for property " << propName << " : " << propValue);
                result = false;
                continue;
            }

            if (m_Factories.count(E_EventRatePeersFactory) > 0)
            {
                m_Factories[E_EventRatePeersFactory]->minimumModeFraction(fraction);
            }
        }
        else if (propName == COMPONENT_SIZE_PROPERTY)
        {
            int componentSize;
            if (   core::CStringUtils::stringToType(propValue, componentSize) == false
                || componentSize < 0)
            {
                LOG_ERROR("Invalid value of property " << propName << " : " << propValue);
                result = false;
                continue;
            }
            for (TFactoryTypeFactoryPtrMapCItr j = m_Factories.begin(); j != m_Factories.end(); ++j)
            {
                j->second->componentSize(componentSize);
            }
        }
        else if (propName == SAMPLE_COUNT_FACTOR_PROPERTY)
        {
            int factor;
            if (core::CStringUtils::stringToType(propValue, factor) == false || factor < 0)
            {
                LOG_ERROR("Invalid value for property " << propName << " : " << propValue);
                result = false;
                continue;
            }
            for (TFactoryTypeFactoryPtrMapCItr j = m_Factories.begin(); j != m_Factories.end(); ++j)
            {
                j->second->sampleCountFactor(factor);
            }
        }
        else if (propName == PRUNE_WINDOW_SCALE_MINIMUM)
        {
            double factor;
            if (core::CStringUtils::stringToType(propValue, factor) == false)
            {
                LOG_ERROR("Invalid value for property " << propName << " : " << propValue);
                result = false;
                continue;
            }
            for (TFactoryTypeFactoryPtrMapCItr j = m_Factories.begin(); j != m_Factories.end(); ++j)
            {
                j->second->pruneWindowScaleMinimum(factor);
            }
        }
        else if (propName == PRUNE_WINDOW_SCALE_MAXIMUM)
        {
            double factor;
            if (core::CStringUtils::stringToType(propValue, factor) == false)
            {
                LOG_ERROR("Invalid value for property " << propName << " : " << propValue);
                result = false;
                continue;
            }
            for (TFactoryTypeFactoryPtrMapCItr j = m_Factories.begin(); j != m_Factories.end(); ++j)
            {
                j->second->pruneWindowScaleMaximum(factor);
            }
        }
        else if (propName == AGGREGATION_STYLE_PARAMS)
        {
            core::CStringUtils::trimWhitespace(propValue);
            propValue = core::CStringUtils::normaliseWhitespace(propValue);

            TStrVec strings;
            std::string remainder;
            core::CStringUtils::tokenise(" ", propValue, strings, remainder);
            if (!remainder.empty())
            {
                strings.push_back(remainder);
            }
            std::size_t n = model_t::NUMBER_AGGREGATION_STYLES * model_t::NUMBER_AGGREGATION_PARAMS;
            if (strings.size() != n)
            {
                LOG_ERROR("Expected " << n << " values for " << propName);
                result = false;
                continue;
            }
            for (std::size_t j = 0u, l = 0u; j < model_t::NUMBER_AGGREGATION_STYLES; ++j)
            {
                for (std::size_t k = 0u; k < model_t::NUMBER_AGGREGATION_PARAMS; ++k, ++l)
                {
                    double value;
                    if (core::CStringUtils::stringToType(strings[l], value) == false)
                    {
                        LOG_ERROR("Unexpected value " << strings[l] << " in property " << propName);
                        result = false;
                        continue;
                    }

                    this->aggregationStyleParams(static_cast<model_t::EAggregationStyle>(j),
                                                 static_cast<model_t::EAggregationParam>(k),
                                                 value);
                }
            }
        }
        else if (propName == MAXIMUM_ANOMALOUS_PROBABILITY_PROPERTY)
        {
            double probability;
            if (core::CStringUtils::stringToType(propValue, probability) == false)
            {
                LOG_ERROR("Invalid value for property " << propName << " : " << propValue);
                result = false;
                continue;
            }
            this->maximumAnomalousProbability(probability);
        }
        else if (propName == NOISE_PERCENTILE_PROPERTY)
        {
            double percentile;
            if (   core::CStringUtils::stringToType(propValue, percentile) == false
                || this->noisePercentile(percentile) == false)
            {
                LOG_ERROR("Invalid value for property " << propName << " : " << propValue);
                result = false;
                continue;
            }
        }
        else if (propName == NOISE_MULTIPLIER_PROPERTY)
        {
            double multiplier;
            if (   core::CStringUtils::stringToType(propValue, multiplier) == false
                || this->noiseMultiplier(multiplier) == false)
            {
                LOG_ERROR("Invalid value for property " << propName << " : " << propValue);
                result = false;
                continue;
            }
        }
        else if (propName == NORMALIZED_SCORE_KNOT_POINTS)
        {
            core::CStringUtils::trimWhitespace(propValue);
            propValue = core::CStringUtils::normaliseWhitespace(propValue);

            TStrVec strings;
            std::string remainder;
            core::CStringUtils::tokenise(" ", propValue, strings, remainder);
            if (!remainder.empty())
            {
                strings.push_back(remainder);
            }
            if (strings.empty() || (strings.size() % 2) != 0)
            {
                LOG_ERROR("Expected even number of values for property " << propName
                          << " " << core::CContainerPrinter::print(strings));
                result = false;
                continue;
            }

            TDoubleDoublePrVec points;
            points.reserve(strings.size() / 2 + 2);
            points.push_back(TDoubleDoublePr(0.0, 0.0));
            for (std::size_t j = 0u; j < strings.size(); j += 2)
            {
                double rate;
                double score;
                if (core::CStringUtils::stringToType(strings[j], rate) == false)
                {
                    LOG_ERROR("Unexpected value " << strings[j]
                              << " for rate in property " << propName);
                    result = false;
                    continue;
                }
                if (core::CStringUtils::stringToType(strings[j+1], score) == false)
                {
                    LOG_ERROR("Unexpected value " << strings[j+1]
                              << " for score in property " << propName);
                    result = false;
                    continue;
                }
                points.push_back(TDoubleDoublePr(rate, score));
            }
            points.push_back(TDoubleDoublePr(100.0, 100.0));
            this->normalizedScoreKnotPoints(points);
        }
        else if (propName == PER_PARTITION_NORMALIZATION_PROPERTY)
        {

        }
        else
        {
            LOG_WARN("Ignoring unknown property " << propName);
        }
    }

    return result;
}

double CModelConfig::bucketNormalizationFactor(void) const
{
    return bucketNormalizationFactor(m_BucketLength);
}

}
}
