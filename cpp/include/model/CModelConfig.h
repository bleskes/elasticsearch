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

#ifndef INCLUDED_prelert_model_CModelConfig_h
#define INCLUDED_prelert_model_CModelConfig_h

#include <core/CoreTypes.h>

#include <model/CSearchKey.h>
#include <model/FunctionTypes.h>
#include <model/ImportExport.h>
#include <model/ModelTypes.h>

#include <boost/array.hpp>
#include <boost/property_tree/ptree_fwd.hpp>
#include <boost/ref.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/unordered_map.hpp>

#include <cstddef>
#include <map>
#include <set>
#include <utility>
#include <vector>

namespace prelert
{
namespace model
{
class CDetectionRule;
class CSearchKey;
class CModelAutoConfigurer;
class CModelFactory;

//! \brief Holds configuration for the anomaly detection models.
//!
//! DESCRIPTION:\n
//! Holds configuration state for the event rate anomaly detection
//! and model objects. In particular this configures whether the
//! models are learning, detecting anomalies and batching data.
//!
//! IMPLEMENTATION:\n
//! This wraps up the configuration of the models to encapsulate
//! the details from the calling code. It is intended that at least
//! some of this information will be exposed to the user via a
//! configuration file.
//!
//! Default settings for various modes of operation are provided
//! by the default* factory methods.

class MODEL_EXPORT CModelConfig
{
    public:
        //! The possible factory types.
        enum EFactoryType
        {
            E_EventRateFactory = 0,
            E_MetricFactory = 1,
            E_EventRatePopulationFactory = 2,
            E_MetricPopulationFactory = 3,
            E_CountingFactory = 4,
            E_EventRatePeersFactory = 5,
            E_UnknownFactory,
            E_BadFactory
        };

        //! Possible destinations for model debug plots.
        enum EDebugDestination
        {
            E_File = 0,
            E_DataStore = 1
        };

        typedef std::set<std::string> TStrSet;
        typedef std::vector<std::size_t> TSizeVec;
        typedef std::vector<core_t::TTime> TTimeVec;
        typedef TTimeVec::const_iterator TTimeVecCItr;
        typedef std::pair<double, double> TDoubleDoublePr;
        typedef std::vector<TDoubleDoublePr> TDoubleDoublePrVec;
        typedef model_t::TFeatureVec TFeatureVec;
        typedef std::vector<std::string> TStrVec;
        typedef TStrVec::const_iterator TStrVecCItr;
        typedef boost::shared_ptr<CModelFactory> TModelFactoryPtr;
        typedef boost::shared_ptr<const CModelFactory> TModelFactoryCPtr;
        typedef std::map<EFactoryType, TModelFactoryPtr> TFactoryTypeFactoryPtrMap;
        typedef TFactoryTypeFactoryPtrMap::iterator TFactoryTypeFactoryPtrMapItr;
        typedef TFactoryTypeFactoryPtrMap::const_iterator TFactoryTypeFactoryPtrMapCItr;
        typedef std::map<CSearchKey, TModelFactoryCPtr> TSearchKeyFactoryCPtrMap;

        // Const ref to detection rules map
        typedef std::vector<CDetectionRule> TDetectionRuleVec;
        typedef boost::reference_wrapper<const TDetectionRuleVec> TDetectionRuleVecCRef;
        typedef boost::unordered_map<int, TDetectionRuleVec> TIntDetectionRuleVecUMap;
        typedef boost::reference_wrapper<const TIntDetectionRuleVecUMap> TIntDetectionRuleVecUMapCRef;
        typedef TIntDetectionRuleVecUMap::const_iterator TIntDetectionRuleVecUMapCItr;

    public:
        //! The default value used to separate components of a multivariate feature
        //! in its string value.
        static const std::string DEFAULT_MULTIVARIATE_COMPONENT_DELIMITER;

        //! Bucket length if none is specified on the command line.
        static const core_t::TTime DEFAULT_BUCKET_LENGTH;

        //! Default maximum number of buckets for receiving out of order records.
        static const std::size_t DEFAULT_LATENCY_BUCKETS;

        //! Default amount by which metric sample count is reduced for fine-grained
        //! sampling when there is no latency.
        static const std::size_t DEFAULT_SAMPLE_COUNT_FACTOR_NO_LATENCY;

        //! Default amount by which metric sample count is reduced for fine-grained
        //! sampling when there is latency.
        static const std::size_t DEFAULT_SAMPLE_COUNT_FACTOR_WITH_LATENCY;

        //! Default amount by which the metric sample queue expands when it is full.
        static const double DEFAULT_SAMPLE_QUEUE_GROWTH_FACTOR;

        //! The default number of buckets to batch.
        static const core_t::TTime DEFAULT_BATCH_LENGTH;

        //! The default overlap between adjacent batches (in buckets).
        static const std::size_t DEFAULT_OVERLAP;

        //! The "period" of aperiodic data.
        static const std::size_t APERIODIC;

        //! Bucket length corresponding to the default decay and learn rates.
        static const core_t::TTime STANDARD_BUCKET_LENGTH;

        //! The default rate at which the model priors decay to non-informative
        //! per standard bucket length.
        static const double DEFAULT_DECAY_RATE;

        //! The initial rate, as a multiple of the default decay rate, at which
        //! the model priors decay to non-informative per standard bucket length.
        static const double DEFAULT_INITIAL_DECAY_RATE_MULTIPLIER;

        //! The rate at which information accrues in the model per standard
        //! bucket length elapsed.
        static const double DEFAULT_LEARN_RATE;

        //! The default minimum permitted fraction of points in a distribution
        //! mode for individual modeling.
        static const double DEFAULT_INDIVIDUAL_MINIMUM_MODE_FRACTION;

        //! The default minimum permitted fraction of points in a distribution
        //! mode for population modeling.
        static const double DEFAULT_POPULATION_MINIMUM_MODE_FRACTION;

        //! The default minimum count in a cluster we'll permit in a cluster.
        static const double DEFAULT_MINIMUM_CLUSTER_SPLIT_COUNT;

        //! The default minimum frequency of non-empty buckets at which we model
        //! all buckets.
        static const double DEFAULT_CUTOFF_TO_MODEL_EMPTY_BUCKETS;

        //! The default proportion of initial count at which we'll delete a
        //! category from the sketch to cluster.
        static const double DEFAULT_CATEGORY_DELETE_FRACTION;

        //! The default size of the seasonal components we will model.
        static const std::size_t DEFAULT_COMPONENT_SIZE;

        //! The default number of times to sample a person model when computing
        //! total probabilities for population models.
        static const std::size_t DEFAULT_TOTAL_PROBABILITY_CALC_SAMPLING_SIZE;

        //! The maximum number of times we'll update a model in a bucketing
        //! interval. This only applies to our metric statistics, which are
        //! computed on a fixed number of measurements rather than a fixed
        //! time interval. A value of zero implies no constraint.
        static const double DEFAULT_MAXIMUM_UPDATES_PER_BUCKET;

        //! The default minimum value for the influence for which an influencing
        //! field value is judged to have any influence on a feature value.
        static const double DEFAULT_INFLUENCE_CUTOFF;

        //! The default scale factor of the decayRate that determines the minimum
        //! size of the sliding prune window for purging older entries from the
        //! model.
        static const double DEFAULT_PRUNE_WINDOW_SCALE_MINIMUM;

        //! The default scale factor of the decayRate that determines the maximum
        //! size of the sliding prune window for purging older entries from the
        //! model.
        static const double DEFAULT_PRUNE_WINDOW_SCALE_MAXIMUM;

        //! The default factor increase in priors used to model correlations.
        static const double DEFAULT_CORRELATION_MODELS_OVERHEAD;

        //! The default threshold for the Pearson correlation coefficient at
        //! which a correlate will be modeled.
        static const double DEFAULT_MINIMUM_SIGNIFICANT_CORRELATION;

        //! The default number of half buckets to store before choosing which
        //! overlapping bucket has the biggest anomaly
        static const std::size_t DEFAULT_BUCKET_RESULTS_DELAY;

        //! \name Anomaly Score Calculation
        //@{
        //! The default values for the aggregation styles' parameters.
        static const double DEFAULT_AGGREGATION_STYLE_PARAMS[model_t::NUMBER_AGGREGATION_STYLES][model_t::NUMBER_AGGREGATION_PARAMS];

        //! The default maximum probability which is deemed to be anomalous.
        static const double DEFAULT_MAXIMUM_ANOMALOUS_PROBABILITY;
        //@}

        //! \name Anomaly Score Normalization
        //@{
        //! The default historic anomaly score percentile for which lower
        //! values are classified as noise.
        static const double DEFAULT_NOISE_PERCENTILE;

        //! The default multiplier applied to the noise level score in
        //! order to be classified as anomalous.
        static const double DEFAULT_NOISE_MULTIPLIER;

        //! We use a piecewise linear mapping between the raw anomaly score
        //! and the normalized anomaly score with these default knot points.
        //! In particular, if we define the percentile of a raw score \f$s\f$
        //! as \f$f_q(s)\f$ and \f$a = \max\{x \le f_q(s)\}\f$ and
        //! \f$b = \min{x \ge f_q(s)}\f$ where \f$x\f$ ranges over the knot point
        //! X- values then the normalized score would be:\n
        //! <pre class="fragment">
        //!   \f$\displaystyle \bar{s} = \frac{(y(b) - y(a))(f_q(s) - a)}{b - a}\f$
        //! </pre>
        //! Here, \f$y(.)\f$ denote the corresponding knot point Y- values.
        static const TDoubleDoublePr DEFAULT_NORMALIZED_SCORE_KNOT_POINTS[9];
        //@}

        //! For priors whose support isn't the whole real line we use an
        //! offset which is just a translation of the prior to extend the
        //! range over which it is valid. This is the default offset
        //! of the left pillar point of the prior relative to zero or the
        //! smallest metric value observed.
        static const double DEFAULT_PRIOR_OFFSET;

        //! The maximum number of samples we use when re-sampling a prior.
        static const std::size_t DEFAULT_RESAMPLING_MAX_SAMPLES;

    public:
        //! Create the default configuration.
        //!
        //! \param[in] bucketLength The bucketing interval length.
        //! \param[in] batchLength The number of buckets to batch when detecting
        //! and reporting anomalies. If a value of zero is supplied the default
        //! (= DEFAULT_BATCH_SIZE * bucketLength) is used.
        //! \param[in] period The "period" of aperiodic data.
        //! \param[in] summaryMode Indicates whether the data being gathered
        //! are already summarized by an external aggregation process, such
        //! as the Splunk statistics command.
        //! \param[in] summaryCountFieldName If \p summaryMode is E_Manual
        //! then this is the name of the field holding the summary count.
        //! \param[in] latency The amount of time records are buffered for, to
        //! allow out-of-order records to be seen by the models in order.
        //! \param[in] bucketResultsDelay The number of half-bucket results
        //! to sit on before giving a definitive result.
        //! \param[in] multivariateByFields Should multivariate analysis of
        //! correlated 'by' fields be performed?
        //! \param[in] multipleBucketLengths If specified, set multiple bucket
        //! lengths to be analysed (CSV string of time values)
        static CModelConfig defaultConfig(core_t::TTime bucketLength,
                                          core_t::TTime batchLength,
                                          std::size_t period,
                                          model_t::ESummaryMode summaryMode,
                                          const std::string &summaryCountFieldName,
                                          core_t::TTime latency,
                                          std::size_t bucketResultsDelay,
                                          bool multivariateByFields,
                                          const std::string &multipleBucketLengths);

        //! Overload using defaults.
        static CModelConfig defaultConfig(core_t::TTime bucketLength = DEFAULT_BUCKET_LENGTH,
                                          core_t::TTime batchLength = DEFAULT_BATCH_LENGTH,
                                          std::size_t period = APERIODIC,
                                          model_t::ESummaryMode summaryMode = model_t::E_None,
                                          const std::string &summaryCountFieldName = "")
        {
            return defaultConfig(bucketLength,
                                 batchLength,
                                 period,
                                 summaryMode,
                                 summaryCountFieldName,
                                 DEFAULT_LATENCY_BUCKETS * bucketLength,
                                 DEFAULT_BUCKET_RESULTS_DELAY,
                                 false,
                                 "");
        }

        //! Get the factor to normalize all bucket lengths to the default
        //! bucket length.
        static double bucketNormalizationFactor(core_t::TTime bucketLength);

        //! Get the decay rate to use for the time series decomposition given
        //! the model decay rate \p modelDecayRate.
        static double trendDecayRate(double modelDecayRate, core_t::TTime bucketLength);

        //! Parse and verify the multiple bucket lengths - these should all be
        //! multiples of the standard bucket length.
        static TTimeVec multipleBucketLengths(core_t::TTime bucketLength,
                                              const std::string &multipleBucketLengths);

    public:
        CModelConfig(void);

        //! Set the data bucketing interval.
        void bucketLength(core_t::TTime length);
        //! Set the batch length.
        void batchLength(core_t::TTime length);
        //! Set the amount in bucket lengths to overlap batches.
        void batchOverlap(std::size_t overlap);
        //! Set the periodic repeat to use if modeling seasonality by batch models.
        void period(std::size_t period);
        //! Set the number of buckets to delay finalizing out-of-phase buckets.
        void bucketResultsDelay(std::size_t delay);

        //! Set whether multivariate analysis of correlated 'by' fields should
        //! be performed.
        void multivariateByFields(bool enabled);
        //! Set the model factories.
        void factories(const TFactoryTypeFactoryPtrMap &factories);
        //! Set the style and parameter value for raw score aggregation.
        bool aggregationStyleParams(model_t::EAggregationStyle style,
                                    model_t::EAggregationParam param,
                                    double value);
        //! Set the maximum anomalous probability.
        void maximumAnomalousProbability(double probability);
        //! Set the noise level as a percentile of historic raw anomaly scores.
        bool noisePercentile(double percentile);
        //! Set the noise multiplier to use when derating normalized scores
        //! based on the noise score level.
        bool noiseMultiplier(double multiplier);
        //! Set the normalized score knot points for the piecewise linear curve
        //! between historic raw score percentiles and normalized scores.
        bool normalizedScoreKnotPoints(const TDoubleDoublePrVec &points);

        //! Populate the parameters from a configuration file.
        bool init(const std::string &configFile);

        //! Populate the parameters from a configuration file, also retrieving
        //! the raw property tree created from the config file.  (The raw
        //! property tree is only valid if the method returns true.)
        bool init(const std::string &configFile,
                  boost::property_tree::ptree &propTree);

        //! Populate the parameters from a property tree.
        bool init(const boost::property_tree::ptree &propTree);

        //! Configure modelDebugConfig params from file
        bool configureDebug(const std::string &debugConfigFile);

        //! Configure modelDebugConfig params from a property tree
        //! expected to contain two properties: 'boundsPercentile' and 'terms'
        bool configureDebug(const boost::property_tree::ptree &propTree);

        //! Get the factory for new models.
        //!
        //! \param[in] key The key of the detector for which the factory will be
        //! used.
        //! \param[in] extraDataPersistFunc The function to convert any extra data
        //! annotated on the models to part of a state document.
        //! \param[in] extraDataRestoreFunc The function to convert any extra data
        //! annotated on the models from part of a state document.
        //! \param[in] extraDataMemoryFunc The function used to register a memory
        //! callback for the extra data.
        TModelFactoryCPtr factory(const CSearchKey &key,
                                  const model_t::TAnyPersistFunc &extraDataPersistFunc = model_t::TAnyPersistFunc(),
                                  const model_t::TAnyRestoreFunc &extraDataRestoreFunc = model_t::TAnyRestoreFunc(),
                                  const model_t::TAnyMemoryFunc &extraDataMemoryFunc = model_t::TAnyMemoryFunc()) const;

        //! Get the factory for new models.
        //!
        //! \param[in] identifier The identifier of the search for which to get a model
        //! factory.
        //! \param[in] function The function being invoked.
        //! \param[in] useNull If true then we will process missing fields as if their
        //! value is equal to the empty string where possible.
        //! \param[in] excludeFrequent Whether to discard frequent results
        //! \param[in] personFieldName The name of the over field.
        //! \param[in] attributeFieldName The name of the by field.
        //! \param[in] valueFieldName The name of the field containing metric values.
        //! \param[in] influenceFieldNames The list of influence field names.
        //! \param[in] extraDataPersistFunc The function to convert any extra data
        //! annotated on the models to part of a state document.
        //! \param[in] extraDataRestoreFunc The function to convert any extra data
        //! annotated on the models from part of a state document.
        //! \param[in] extraDataMemoryFunc The function used to register a memory
        //! callback for the extra data.
        TModelFactoryCPtr factory(int identifier,
                                  function_t::EFunction function,
                                  bool useNull = false,
                                  model_t::EExcludeFrequent excludeFrequent = model_t::E_XF_None,
                                  const std::string &partitionFieldName = std::string(),
                                  const std::string &personFieldName = std::string(),
                                  const std::string &attributeFieldName = std::string(),
                                  const std::string &valueFieldName = std::string(),
                                  const CSearchKey::TStrPtrVec &influenceFieldNames = CSearchKey::TStrPtrVec(),
                                  const model_t::TAnyPersistFunc &extraDataPersistFunc = model_t::TAnyPersistFunc(),
                                  const model_t::TAnyRestoreFunc &extraDataRestoreFunc = model_t::TAnyRestoreFunc(),
                                  const model_t::TAnyMemoryFunc &extraDataMemoryFunc = model_t::TAnyMemoryFunc()) const;

        //! Get the rate at which the models lose information.
        double decayRate(void) const;

        //! Get the length of the baseline.
        core_t::TTime baselineLength(void) const;

        //! Get the bucket length.
        core_t::TTime bucketLength(void) const;

        //! Get the maximum latency in the arrival of out of order data.
        core_t::TTime latency(void) const;

        //! Get the maximum latency in the arrival of out of order data in
        //! numbers of buckets.
        std::size_t latencyBuckets(void) const;

        //! Get the interval of a batch.
        core_t::TTime batchLength(void) const;

        //! Get the number of buckets in a batch.
        std::size_t batchSize(void) const;

        //! Get the number of buckets by which batches overlap.
        std::size_t batchOverlap(void) const;

        //! Get the data period.
        std::size_t period(void) const;

        //! Get the bucket result delay window.
        std::size_t bucketResultsDelay(void) const;

        //! Get the multiple bucket lengths.
        const TTimeVec &multipleBucketLengths(void) const;

        //! Should multivariate analysis of correlated 'by' fields be performed?
        bool multivariateByFields(void) const;

        //! \name Model Debug Data
        //@{
        //! Set the destination for model debug plots.
        void modelDebugDestination(EDebugDestination destination);

        //! Set the destination for model debug plots, based on a string.
        //! Any string that is not understood is ignored.
        void modelDebugDestination(const std::string &destination);

        //! Get the destination for model debug plots.
        EDebugDestination modelDebugDestination(void) const;

        //! Set the central confidence interval for the model debug plot
        //! to \p percentage.
        //!
        //! This controls upper and lower confidence interval error bars
        //! returned by the model debug plot.
        //! \note \p percentile should be in the range [0.0, 100.0).
        void modelDebugBoundsPercentile(double percentile);

        //! Get the central confidence interval for the model debug plot.
        double modelDebugBoundsPercentile(void) const;

        //! Set terms (by, over, or partition field values) to filter
        //! model debug data. When empty, no filtering is applied.
        void modelDebugTerms(TStrSet terms);

        //! Get the terms (by, over, or partition field values)
        //! used to filter model debug data. Empty when no filtering applies.
        const TStrSet &modelDebugTerms(void) const;
        //@}

        //! \name Anomaly Score Calculation
        //@{
        //! Get the value of the aggregation style parameter identified by
        //! \p style and \p param.
        double aggregationStyleParam(model_t::EAggregationStyle style,
                                     model_t::EAggregationParam param) const;

        //! Get the maximum anomalous probability.
        double maximumAnomalousProbability(void) const;
        //@}

        //! \name Anomaly Score Normalization
        //@{
        //! Get the historic anomaly score percentile for which lower
        //! values are classified as noise.
        double noisePercentile(void) const;

        //! Get the multiplier applied to the noise level score in order
        //! to be classified as anomalous.
        double noiseMultiplier(void) const;

        //! Get the normalized anomaly score knot points.
        const TDoubleDoublePrVec &normalizedScoreKnotPoints(void) const;
        //@}

        //! Check if we should create one normalizer per partition field value.
        bool perPartitionNormalization(void) const;

        //! Set whether we should create one normalizer per partition field value.
        void perPartitionNormalization(bool value);

        //! Sets the reference to the detection rules map
        void detectionRules(TIntDetectionRuleVecUMapCRef detectionRules);

        //! Process the stanza properties corresponding \p stanzaName.
        //!
        //! \param[in] propertyTree The properties of the stanza called
        //! \p stanzaName.
        bool processStanza(const boost::property_tree::ptree &propertyTree);

        //! Get the factor to normalize all bucket lengths to the default
        //! bucket length.
        double bucketNormalizationFactor(void) const;

    private:
        //! Bucket length.
        core_t::TTime m_BucketLength;

        //! Batch length.
        core_t::TTime m_BatchLength;

        //! The number of buckets by which the batches overlap when sampling.
        std::size_t m_BatchOverlap;

        //! The data period expressed as a multiple of the batch length.
        std::size_t m_Period;

        //! Get the bucket result delay window: The numer of half buckets to
        //! store before choosing which overlapping bucket has the biggest anomaly
        std::size_t m_BucketResultsDelay;

        //! Should multivariate analysis of correlated 'by' fields be performed?
        bool m_MultivariateByFields;

        //! The new model factories for each data type.
        TFactoryTypeFactoryPtrMap m_Factories;

        //! A cache of customized factories requested from this config.
        mutable TSearchKeyFactoryCPtrMap m_FactoryCache;

        //! \name Model Debug Data
        //@{
        //! The destination for model debug plots.
        EDebugDestination m_ModelDebugDestination;

        //! The central confidence interval for the model debug plot.
        double m_ModelDebugBoundsPercentile;

        //! Terms (by, over, or partition field values) used to filter model
        //! debug data. Empty when no filtering applies.
        TStrSet m_ModelDebugTerms;
        //@}

        //! \name Anomaly Score Calculation
        //@{
        //! The values for the aggregation styles' parameters.
        double m_AggregationStyleParams[model_t::NUMBER_AGGREGATION_STYLES][model_t::NUMBER_AGGREGATION_PARAMS];

        //! The maximum probability which is deemed to be anomalous.
        double m_MaximumAnomalousProbability;
        //@}

        //! \name Anomaly Score Normalization
        //@{
        //! The historic anomaly score percentile for which lower values
        //! are classified as noise.
        double m_NoisePercentile;

        //! The multiplier applied to the noise level score in order to
        //! be classified as anomalous.
        double m_NoiseMultiplier;

        //! We use a piecewise linear mapping between the raw anomaly score
        //! and the normalized anomaly score with these knot points.
        //! \see DEFAULT_NORMALIZED_SCORE_KNOT_POINTS for details.
        TDoubleDoublePrVec m_NormalizedScoreKnotPoints;
        //@}

        bool m_PerPartitionNormalisation;

        //! A reference to the map containing detection rules per
        //! detector key. Note that the owner of the map is CFieldConfig.
        TIntDetectionRuleVecUMapCRef m_DetectionRules;
};

}
}

#endif // INCLUDED_prelert_model_CModelConfig_h
