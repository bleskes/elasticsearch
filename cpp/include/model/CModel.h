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

#ifndef INCLUDED_prelert_model_CModel_h
#define INCLUDED_prelert_model_CModel_h

#include <core/CMemory.h>
#include <core/CNonCopyable.h>
#include <core/CoreTypes.h>
#include <core/CSmallVector.h>
#include <core/CStatistics.h>

#include <maths/CKMostCorrelated.h>
#include <maths/MathsTypes.h>

#include <model/CModelParams.h>
#include <model/CPartitioningFields.h>
#include <model/ImportExport.h>
#include <model/ModelTypes.h>

#include <boost/any.hpp>
#include <boost/optional.hpp>
#include <boost/ref.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/unordered_map.hpp>

#include <string>
#include <limits>
#include <map>
#include <memory>
#include <utility>
#include <vector>

#include <stdint.h>


namespace prelert
{
namespace core
{
class CStatePersistInserter;
class CStateRestoreTraverser;
}

namespace maths
{
class CCounter;
class CMultivariatePrior;
class CPrior;
class CTimeSeriesDecompositionInterface;
}

namespace model
{
class CAttributeFrequencyGreaterThan;
class CInterimBucketCorrector;
class CDataGatherer;
class CDecayRateController;
class CHierarchicalResults;
class CModelDetailsView;
class CPersonFrequencyGreaterThan;
class CResourceMonitor;
struct SAnnotatedProbability;
struct SAttributeProbability;

//! \brief The model interface.
//!
//! DESCRIPTION:\n
//! This defines the interface common to all (statistical) models of
//! the (random) processes which describe system state. It declares
//! core functions used by the anomaly detection code to:
//!   -# Retrieve information about the categories and people of the
//!      processes being modeled.
//!   -# Sample the processes in a specified time interval and update
//!      the model.
//!   -# Manage the model life-cycle.
//!   -# Compute the probability of the samples of the process in a
//!      specified time interval.
//!
//! The raw events can be partitioned by attribute and/or person (for
//! population analysis). These are just two labels which can be
//! annotated on the events and induce equivalence relations on the
//! set of all events. The events in subsets comprise (some of)
//! the raw events for (one of) the processes we model. For example,
//! in temporal analysis we would model the history of all events
//! for which the labels are equal for each distinct value of the
//! label.
//!
//! There are two main types of analysis:
//!   -# Individual analysis.
//!   -# Population analysis.
//!   -# Peer group analysis.
//!
//! Individual analysis looks at the historical values of various
//! features on a single time series' events and detects significant
//! changes in those values. Population analysis looks at similar
//! features, but on a whole collection of processes in conjunction
//! (induced by the person label equivalence relation). Peer group
//! analysis is similar to population analysis, but assigns each
//! person to a peer group and looks for unusual behaviour w.r.t.
//! its peer group and not the population as a whole. The concrete
//! implementations of this class include more detailed descriptions.
//! This object also maintains the state to find the most correlated
//! pairs of time series.
//!
//! The extraction of the features from the raw process events is
//! managed by a separate object. These include a number of simple
//! statistics such as the count of events in a time interval, the
//! mean of a certain number of event values, the minimum of a
//! certain number of event values and so on. (See model::CDataGatherer
//! for more details.)
//!
//! The model hierarchy is also able to compare two time intervals
//! in which case a model really comprises two distinct models of the
//! underlying random process one for each time interval: see the
//! computeProbability for more details.
//!
//! IMPLEMENTATION DECISIONS:\n
//! The model hierarchy has been abstracted to allow the code to detect
//! anomalies to be reused for different types of data, log messages,
//! metrics, etc, to perform different types of analysis on that data,
//! and to handle the case that data are continuously streamed to the
//! object or the case that two different data sets are to be compared.
//!
//! Extra data can be annotated on the model as a convenient mechanism
//! to associate data which might be needed by the calling code. The type
//! is boost::any (i.e. effectively void*) to make this as flexible as
//! possible. (For example, in the model library this is used to
//! associate the meta data used to look up messages in the text based
//! categories which we identify.)
//!
//! All models can be serialized to/from text representation.
//!
//! The hierarchy is non-copyable because we don't currently need to be
//! able to copy models and the "correct" copy semantics are not obvious.
class MODEL_EXPORT CModel : private core::CNonCopyable
{
    friend class CModelDetailsView;

    public:
        //! The decay rate controllers we maintain.
        enum EDecayRateController
        {
            E_TrendControl = 0,
            E_PriorControl,
            E_NumberControls
        };

        typedef std::vector<std::size_t> TSizeVec;
        typedef std::vector<double> TDoubleVec;
        typedef core::CSmallVector<double, 1> TDouble1Vec;
        typedef core::CSmallVector<double, 4> TDouble4Vec;
        typedef core::CSmallVector<double, 10> TDouble10Vec;
        typedef core::CSmallVector<TDouble1Vec, 4> TDouble1Vec4Vec;
        typedef core::CSmallVector<TDouble4Vec, 1> TDouble4Vec1Vec;
        typedef core::CSmallVector<TDouble10Vec, 1> TDouble10Vec1Vec;
        typedef core::CSmallVector<TDouble10Vec, 4> TDouble10Vec4Vec;
        typedef core::CSmallVector<TDouble10Vec4Vec, 1> TDouble10Vec4Vec1Vec;
        typedef std::pair<TDouble1Vec, TDouble1Vec> TDouble1VecDouble1VecPr;
        typedef std::pair<std::size_t, double> TSizeDoublePr;
        typedef core::CSmallVector<TSizeDoublePr, 1> TSizeDoublePr1Vec;
        typedef core::CSmallVector<std::size_t, 2> TSize2Vec;
        typedef core::CSmallVector<TSize2Vec, 1> TSize2Vec1Vec;
        typedef std::pair<double, double> TDoubleDoublePr;
        typedef std::vector<TDoubleDoublePr> TDoubleDoublePrVec;
        typedef std::pair<std::size_t, std::size_t> TSizeSizePr;
        typedef std::vector<std::string> TStrVec;
        typedef boost::optional<double> TOptionalDouble;
        typedef std::vector<TOptionalDouble> TOptionalDoubleVec;
        typedef boost::optional<uint64_t> TOptionalUInt64;
        typedef std::vector<boost::any> TAnyVec;
        typedef core::CSmallVector<SAttributeProbability, 1> TAttributeProbability1Vec;
        typedef boost::reference_wrapper<const std::string> TStrCRef;
        typedef boost::shared_ptr<maths::CPrior> TPriorPtr;
        typedef std::pair<model_t::EFeature, TPriorPtr> TFeaturePriorPtrPr;
        typedef std::vector<TFeaturePriorPtrPr> TFeaturePriorPtrPrVec;
        typedef TFeaturePriorPtrPrVec::iterator TFeaturePriorPtrPrVecItr;
        typedef TFeaturePriorPtrPrVec::const_iterator TFeaturePriorPtrPrVecCItr;
        typedef std::vector<TPriorPtr> TPriorPtrVec;
        typedef std::map<model_t::EFeature, TPriorPtrVec> TFeaturePriorPtrVecMap;
        typedef TFeaturePriorPtrVecMap::iterator TFeaturePriorPtrVecMapItr;
        typedef TFeaturePriorPtrVecMap::const_iterator TFeaturePriorPtrVecMapCItr;
        typedef std::pair<const maths::CMultivariatePrior*, std::size_t> TMultivariatePriorCPtrSizePr;
        typedef core::CSmallVector<TMultivariatePriorCPtrSizePr, 1> TMultivariatePriorCPtrSizePr1Vec;
        typedef boost::shared_ptr<maths::CMultivariatePrior> TMultivariatePriorPtr;
        typedef std::pair<model_t::EFeature, TMultivariatePriorPtr> TFeatureMultivariatePriorPtrPr;
        typedef std::pair<TMultivariatePriorPtr, double> TMultivariatePriorPtrDoublePr;
        typedef std::vector<TFeatureMultivariatePriorPtrPr> TFeatureMultivariatePriorPtrPrVec;
        typedef TFeatureMultivariatePriorPtrPrVec::iterator TFeatureMultivariatePriorPtrPrVecItr;
        typedef TFeatureMultivariatePriorPtrPrVec::const_iterator TFeatureMultivariatePriorPtrPrVecCItr;
        typedef std::vector<TMultivariatePriorPtr> TMultivariatePriorPtrVec;
        typedef std::map<model_t::EFeature, TMultivariatePriorPtrVec> TFeatureMultivariatePriorPtrVecMap;
        typedef TFeatureMultivariatePriorPtrVecMap::iterator TFeatureMultivariatePriorPtrVecMapItr;
        typedef TFeatureMultivariatePriorPtrVecMap::const_iterator TFeatureMultivariatePriorPtrVecMapCItr;
        typedef boost::unordered_map<TSizeSizePr, TMultivariatePriorPtrDoublePr> TSizeSizePrMultivariatePriorPtrDoublePrUMap;
        typedef TSizeSizePrMultivariatePriorPtrDoublePrUMap::iterator TSizeSizePrMultivariatePriorPtrDoublePrUMapItr;
        typedef TSizeSizePrMultivariatePriorPtrDoublePrUMap::const_iterator TSizeSizePrMultivariatePriorPtrDoublePrUMapCItr;
        typedef std::vector<TSizeSizePrMultivariatePriorPtrDoublePrUMapCItr> TSizeSizePrMultivariatePriorPtrDoublePrUMapCItrVec;
        typedef TSizeSizePrMultivariatePriorPtrDoublePrUMapCItrVec::const_iterator TSizeSizePrMultivariatePriorPtrDoublePrUMapCItrVecCItr;
        typedef std::map<model_t::EFeature, TSizeSizePrMultivariatePriorPtrDoublePrUMap> TFeatureSizeSizePrMultivariatePriorPtrDoublePrUMapMap;
        typedef TFeatureSizeSizePrMultivariatePriorPtrDoublePrUMapMap::iterator TFeatureSizeSizePrMultivariatePriorPtrDoublePrUMapMapItr;
        typedef TFeatureSizeSizePrMultivariatePriorPtrDoublePrUMapMap::const_iterator TFeatureSizeSizePrMultivariatePriorPtrDoublePrUMapMapCItr;
        typedef boost::unordered_map<std::size_t, TPriorPtr> TSizePriorPtrUMap;
        typedef TSizePriorPtrUMap::iterator TSizePriorPtrUMapItr;
        typedef TSizePriorPtrUMap::const_iterator TSizePriorPtrUMapCItr;
        typedef std::vector<TSizePriorPtrUMap> TSizePriorPtrUMapVec;
        typedef std::map<model_t::EFeature, TSizePriorPtrUMapVec> TFeatureSizePriorPtrUMapVecMap;
        typedef TFeatureSizePriorPtrUMapVecMap::iterator TFeatureSizePriorPtrUMapVecMapItr;
        typedef TFeatureSizePriorPtrUMapVecMap::const_iterator TFeatureSizePriorPtrUMapVecMapCItr;
        typedef core::CSmallVector<const maths::CTimeSeriesDecompositionInterface *, 1> TDecompositionCPtr1Vec;
        typedef boost::shared_ptr<const maths::CTimeSeriesDecompositionInterface> TDecompositionCPtr;
        typedef std::vector<TDecompositionCPtr> TDecompositionCPtrVec;
        typedef std::pair<model_t::EFeature, TDecompositionCPtrVec> TFeatureDecompositionCPtrVecPr;
        typedef std::vector<TFeatureDecompositionCPtrVecPr> TFeatureDecompositionCPtrVecPrVec;
        typedef boost::shared_ptr<maths::CTimeSeriesDecompositionInterface> TDecompositionPtr;
        typedef core::CSmallVector<TDecompositionPtr, 1> TDecompositionPtr1Vec;
        typedef TDecompositionPtr1Vec::const_iterator TDecompositionPtr1VecCItr;
        typedef std::vector<TDecompositionPtr1Vec> TDecompositionPtr1VecVec;
        typedef std::pair<model_t::EFeature, TDecompositionPtr1VecVec> TFeatureDecompositionPtr1VecVecPr;
        typedef std::vector<TFeatureDecompositionPtr1VecVecPr> TFeatureDecompositionPtr1VecVecPrVec;
        typedef TFeatureDecompositionPtr1VecVecPrVec::const_iterator TFeatureDecompositionPtr1VecVecPrVecCItr;
        typedef boost::array<CDecayRateController, E_NumberControls> TDecayRateControllerArray;
        typedef std::vector<TDecayRateControllerArray> TDecayRateControllerArrayVec;
        typedef std::pair<model_t::EFeature, TDecayRateControllerArrayVec> TFeatureDecayRateControllerArrayVecPr;
        typedef std::vector<TFeatureDecayRateControllerArrayVecPr> TFeatureDecayRateControllerArrayVecPrVec;
        typedef boost::shared_ptr<const CInfluenceCalculator> TInfluenceCalculatorCPtr;
        typedef std::pair<model_t::EFeature, TInfluenceCalculatorCPtr> TFeatureInfluenceCalculatorCPtrPr;
        typedef std::vector<TFeatureInfluenceCalculatorCPtrPr> TFeatureInfluenceCalculatorCPtrPrVec;
        typedef TFeatureInfluenceCalculatorCPtrPrVec::const_iterator TFeatureInfluenceCalculatorCPtrPrVecCItr;
        typedef std::vector<TFeatureInfluenceCalculatorCPtrPrVec> TFeatureInfluenceCalculatorCPtrPrVecVec;
        typedef std::pair<model_t::EFeature, maths::CKMostCorrelated> TFeatureKMostCorrelatedPr;
        typedef std::vector<TFeatureKMostCorrelatedPr> TFeatureKMostCorrelatedPrVec;
        typedef TFeatureKMostCorrelatedPrVec::iterator TFeatureKMostCorrelatedPrVecItr;
        typedef TFeatureKMostCorrelatedPrVec::const_iterator TFeatureKMostCorrelatedPrVecCItr;
        typedef boost::shared_ptr<CDataGatherer> TDataGathererPtr;
        typedef boost::shared_ptr<CModel> TModelPtr;
        typedef boost::shared_ptr<const CModel> TModelCPtr;
        typedef std::auto_ptr<CModelDetailsView> CModelDetailsViewPtr;
        typedef boost::shared_ptr<const std::string> TStrPtr;

        //! Used to pass lots of values to TBucketStatsOutputFunc functions
        struct MODEL_EXPORT SOutputStats
        {
            SOutputStats(core_t::TTime time,
                         bool isPopulationResult,
                         bool isAllTimeResult,
                         const std::string &partitionFieldName,
                         const std::string &partitionFieldValue,
                         const std::string &overFieldName,
                         const std::string &overFieldValue,
                         const std::string &byFieldName,
                         const std::string &byFieldValue,
                         const std::string &metricFieldName,
                         const std::string &functionName,
                         double functionValue,
                         bool isInteger);

            core_t::TTime     s_Time;
            bool              s_IsPopulationResult;
            bool              s_IsAllTimeResult;
            const std::string &s_PartitionFieldName;
            const std::string &s_PartitionFieldValue;
            const std::string &s_OverFieldName;
            const std::string &s_OverFieldValue;
            const std::string &s_ByFieldName;
            const std::string &s_ByFieldValue;
            const std::string &s_MetricFieldName;
            const std::string &s_FunctionName;
            double            s_FunctionValue;
            bool              s_IsInteger;
        };

        //! Function for outputting current bucket stats (without probabilities)
        typedef boost::function1<bool, const SOutputStats &> TBucketStatsOutputFunc;

    public:
        //! The confidence interval to compute for the seasonal trend and
        //! variation. We detrend to the nearest point in the confidence
        //! interval and use the upper confidence interval variance when
        //! scaling the likelihood function so that we don't get transient
        //! anomalies after detecting a periodic trend. This accounts for
        //! the expected error in the estimated trend and variance.
        static const double SEASONAL_CONFIDENCE_INTERVAL;

        //! The minimum number of samples added for a correlate prior to be
        //! use for computing the probability.
        static const double MINIMUM_CORRELATE_PRIOR_SAMPLE_COUNT;

        //! A value used to indicate a time variable is unset
        static const core_t::TTime TIME_UNSET;

    public:
        //! \name Life-cycle.
        //@{
        //! \param[in] params The global configuration parameters.
        //! \param[in] dataGatherer The object that gathers time series data.
        //! \param[in] influenceCalculators The influence calculators to use
        //! for each feature.
        //! \param[in] isForRestore True if this constructor being used for restore.
        CModel(const SModelParams &params,
               const TDataGathererPtr &dataGatherer,
               const TFeatureInfluenceCalculatorCPtrPrVecVec &influenceCalculators,
               bool isForRestore);

        //! Create a copy that will result in the same persisted state as the
        //! original.  This is effectively a copy constructor that creates a
        //! copy that's only valid for a single purpose.  The boolean flag is
        //! redundant except to create a signature that will not be mistaken for
        //! a general purpose copy constructor.
        CModel(bool isForPersistence, const CModel &other);

        virtual ~CModel(void);
        //@}

        //! Get a human understandable description of the model for debugging.
        std::string description(void) const;

        //! \name Persistence
        //@{
        //! Persist state by passing information to the supplied inserter.
        virtual void acceptPersistInserter(core::CStatePersistInserter &inserter) const = 0;

        //! Restore the model reading state from the supplied traverser.
        virtual bool acceptRestoreTraverser(const model_t::TAnyRestoreFunc &extraDataRestoreFunc,
                                            core::CStateRestoreTraverser &traverser) = 0;

        //! Create a clone of this model that will result in the same persisted
        //! state.  The clone may be incomplete in ways that do not affect the
        //! persisted representation, and must not be used for any other
        //! purpose.
        //! \warning The caller owns the object returned.
        virtual CModel *cloneForPersistence(void) const = 0;
        //@}

        //! Get the model category.
        virtual model_t::EModelType category(void) const = 0;

        //! True if this is a population model.
        virtual bool isPopulation(void) const = 0;

        //! Check if this is an event rate model.
        virtual bool isEventRate(void) const = 0;

        //! Check if this is a metric model.
        virtual bool isMetric(void) const = 0;

        //! \name Bucket Statistics
        //!@{
        //! Get the count of the bucketing interval containing \p time
        //! for the person identified by \p pid.
        //!
        //! \param[in] pid The identifier of the person of interest.
        //! \param[in] time The time of interest.
        //! \return The count in the bucketing interval at \p time for the
        //! person identified by \p pid if available and null otherwise.
        virtual TOptionalUInt64 currentBucketCount(std::size_t pid,
                                                   core_t::TTime time) const = 0;

        //! Get the mean count of the person identified by \p pid in the
        //! reference data set (for comparison).
        //!
        //! \param[in] pid The identifier of the person of interest.
        virtual TOptionalDouble baselineBucketCount(std::size_t pid) const = 0;

        //! Get the bucket value of \p feature for the person identified
        //! by \p pid and the attribute identified by \p cid in the
        //! bucketing interval including \p time.
        //!
        //! \param[in] feature The feature of interest.
        //! \param[in] pid The identifier of the person of interest.
        //! \param[in] cid The identifier of the attribute of interest.
        //! \param[in] time The time of interest.
        //! \return The value of \p feature in the bucket containing
        //! \p time if available and empty otherwise.
        virtual TDouble1Vec currentBucketValue(model_t::EFeature feature,
                                               std::size_t pid,
                                               std::size_t cid,
                                               core_t::TTime time) const = 0;

        //! Get the appropriate baseline bucket value of \p feature for
        //! the person identified by \p pid and the attribute identified
        //! by \p cid as of the start of the current bucketing interval.
        //! This has subtly different meanings dependent on the model.
        //!
        //! \param[in] feature The feature of interest.
        //! \param[in] pid The identifier of the person of interest.
        //! \param[in] cid The identifier of the attribute of interest.
        //! \param[in] type A description of the type of result for which
        //! to get the baseline. See CResultType for more details.
        //! \param[in] correlated The correlated series' identifiers and
        //! their values if any.
        //! \param[in] time The time of interest.
        //! \return The baseline mean value of \p feature if available
        //! and empty otherwise.
        virtual TDouble1Vec baselineBucketMean(model_t::EFeature feature,
                                               std::size_t pid,
                                               std::size_t cid,
                                               model_t::CResultType type,
                                               const TSizeDoublePr1Vec &correlated,
                                               core_t::TTime time) const = 0;

        //! Check if bucket statistics are available for the specified time.
        virtual bool bucketStatsAvailable(core_t::TTime time) const = 0;
        //@}

        //! \name Person
        //@{
        //! Get the name of the person identified by \p pid. This returns
        //! a default fallback string if the person doesn't exist.
        const std::string &personName(std::size_t pid) const;

        //! As above but with a specified fallback.
        const std::string &personName(std::size_t pid, const std::string &fallback) const;

        //! Print the people identified by \p pids.
        //! Optionally, this may be limited to return a string of the form:
        //! A B C and n others
        std::string printPeople(const TSizeVec &pids,
                                size_t limit = std::numeric_limits<size_t>::max()) const;

        //! Get the person unique identifiers which have a feature value
        //! in the bucketing time interval including \p time.
        //!
        //! \param[in] time The time of interest.
        //! \param[out] result Filled in with the person identifiers
        //! in the bucketing time interval of interest.
        virtual void currentBucketPersonIds(core_t::TTime time, TSizeVec &result) const = 0;

        // TODO this needs to be renamed to numberOfActivePeople, and
        // the places where it is used carefully checked
        // (currently only CModelInspector)
        //! Get the total number of people currently being modeled.
        std::size_t numberOfPeople(void) const;
        //@}

        //! \name Attribute
        //@{
        //! Get the name of the attribute identified by \p cid. This returns
        //! a default fallback string if the attribute doesn't exist.
        //!
        //! \param[in] cid The identifier of the attribute of interest.
        const std::string &attributeName(std::size_t cid) const;

        //! As above but with a specified fallback.
        const std::string &attributeName(std::size_t cid,
                                         const std::string &fallback) const;

        //! Print the attributes identified by \p cids.
        //! Optionally, this may be limited to return a string of the form:
        //! A B C and n others
        std::string printAttributes(const TSizeVec &cids,
                                    size_t limit = std::numeric_limits<size_t>::max()) const;
        //@}

        //! \name Update
        //@{
        //! This samples the bucket statistics, and any state needed
        //! by computeProbablity, in the time interval [\p startTime,
        //! \p endTime], but does not update the model. This is needed
        //! by the results preview.
        //!
        //! \param[in] startTime The start of the time interval to sample.
        //! \param[in] endTime The end of the time interval to sample.
        virtual void sampleBucketStatistics(core_t::TTime startTime,
                                            core_t::TTime endTime,
                                            CResourceMonitor &resourceMonitor) = 0;

        //! Update the model with the samples of the process in the
        //! time interval [\p startTime, \p endTime].
        //!
        //! \param[in] startTime The start of the time interval to sample.
        //! \param[in] endTime The end of the time interval to sample.
        virtual void sample(core_t::TTime startTime,
                            core_t::TTime endTime,
                            CResourceMonitor &resourceMonitor) = 0;

        //! This samples the bucket statistics, and any state needed
        //! by computeProbablity, in the time interval [\p startTime,
        //! \p endTime], but does not update the model. This is needed
        //! by the results preview.
        //!
        //! \param[in] startTime The start of the time interval to sample.
        //! \param[in] endTime The end of the time interval to sample.
        virtual void sampleOutOfPhase(core_t::TTime startTime,
                                      core_t::TTime endTime,
                                      CResourceMonitor &resourceMonitor) = 0;

        //! Rolls time to \p endTime while skipping sampling the models for
        //! buckets within the gap.
        //!
        //! \param[in] endTime The end of the time interval to skip sampling.
        void skipSampling(core_t::TTime endTime);

        //! Prune any person models which haven't been updated for a
        //! specified period.
        virtual void prune(std::size_t maximumAge) = 0;

        //! Prune any person models which haven't been updated for a
        //! sufficiently long period, based on the prior decay rates.
        void prune(void);

        //! Calculate the maximum permitted prune window for this model
        std::size_t defaultPruneWindow(void) const;

        //! Calculate the minimum permitted prune window for this model
        std::size_t minimumPruneWindow(void) const;
        //@}

        //! \name Probability
        //@{
        //! Compute the probability of seeing the samples of the process
        //! for the person identified by \p pid in the time interval
        //! [\p startTime, \p endTime].
        //!
        //! \param[in] pid The unique identifier of the person of interest.
        //! \param[in] startTime The start of the time interval of interest.
        //! \param[in] endTime The end of the time interval of interest.
        //! \param[in] partitioningFields The partitioning field (name, value)
        //! pairs for which to compute the the probability.
        //! \param[in] numberAttributeProbabilities The maximum number of
        //! attribute probabilities to retrieve.
        //! \param[out] result A structure containing the probability,
        //! the smallest \p numberAttributeProbabilities attribute
        //! probabilities, the influences and any extra descriptive data.
        virtual bool computeProbability(std::size_t pid,
                                        core_t::TTime startTime,
                                        core_t::TTime endTime,
                                        CPartitioningFields &partitioningFields,
                                        std::size_t numberAttributeProbabilities,
                                        SAnnotatedProbability &result) const = 0;

        //! Update the results with this model's probability.
        //!
        //! \param[in] detector An identifier of the detector generating this
        //! result.
        //! \param[in] partitionFieldValue The model's partition field value.
        //! \param[in] pid The unique identifier of the person of interest.
        //! \param[in] startTime The start of the time interval of interest.
        //! \param[in] endTime The end of the time interval of interest.
        //! \param[in] numberAttributeProbabilities The maximum number of
        //! attribute probabilities to retrieve.
        //! \param[in,out] results The model results are added.
        void addResult(int detector,
                       const std::string &partitionFieldValue,
                       std::size_t pid,
                       core_t::TTime startTime,
                       core_t::TTime endTime,
                       std::size_t numberAttributeProbabilities,
                       CHierarchicalResults &results) const;

        //! Compute the probability of seeing \p person's attribute processes
        //! so far given the population distributions.
        //!
        //! \param[in] person The person of interest.
        //! \param[in] numberAttributeProbabilities The maximum number of
        //! attribute probabilities to retrieve.
        //! \param[out] probability Filled in with the probability of seeing
        //! the person's processes given the population processes.
        //! \param[out] attributeProbabilities Filled in with the smallest
        //! \p numberAttributeProbabilities attribute probabilities and
        //! associated data describing the calculation.
        virtual bool computeTotalProbability(const std::string &person,
                                             std::size_t numberAttributeProbabilities,
                                             TOptionalDouble &probability,
                                             TAttributeProbability1Vec &attributeProbabilities) const = 0;
        //@}

        //! Output the current bucket statistics by repeatedly calling the
        //! supplied function.  ALL bucket statistics are output, not just those
        //! considered anomalous.  This is used as the implementation of the
        //! prelertstats command in the Splunk app.
        virtual void outputCurrentBucketStatistics(const std::string &partitionFieldValue,
                                                   const TBucketStatsOutputFunc &outputFunc) const = 0;

        //! Get the checksum of this model.
        //!
        //! \param[in] includeCurrentBucketStats If true then include
        //! the current bucket statistics. (This is designed to handle
        //! serialization, for which we don't serialize the current
        //! bucket statistics.)
        virtual uint64_t checksum(bool includeCurrentBucketStats = true) const = 0;

        //! Get the memory used by this model
        virtual void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const = 0;

        //! Get the memory used by this model
        virtual std::size_t memoryUsage(void) const = 0;

        //! Get the static size of this object - used for virtual hierarchies
        virtual std::size_t staticSize(void) const = 0;

        //! Get the time series data gatherer.
        const CDataGatherer &dataGatherer(void) const;
        //! Get the time series data gatherer.
        CDataGatherer &dataGatherer(void);

        //! Get the length of the time interval used to aggregate data.
        core_t::TTime bucketLength(void) const;

        //! Get a view of the internals of the model for visualization.
        virtual CModelDetailsViewPtr details(void) const = 0;

        //! Get the extra data for the person identified by \p pid.
        const boost::any &extraData(std::size_t pid) const;
        //! Set the extra data for the person identified by \p pid.
        //! (This is swapped into place.)
        void extraData(std::size_t pid, boost::any &value);

        //! Get the frequency of the person identified by \p pid.
        double personFrequency(std::size_t pid) const;
        //! Get the frequency of the attribute identified by \p cid.
        virtual double attributeFrequency(std::size_t cid) const = 0;

        //! Get the Winsorisation weight to apply to statistical outliers.
        //!
        //! \param[in] prior The model.
        //! \param[in] weightStyles The model weight styles.
        //! \param[in] sample The sample from \p model for which to compute
        //! a Winsorisation factor.
        //! \param[in] weights The weights to apply when computing the c.d.f.
        //! \param[in] derate The derate to apply to minimum weight. The
        //! larger the derate the larger the minimum weight.
        static double winsorisingWeight(const maths::CPrior &prior,
                                        const maths_t::TWeightStyleVec &weightStyles,
                                        const TDouble1Vec &sample,
                                        const TDouble4Vec1Vec &weights,
                                        double derate);

        //! Get the Winsorisation weight to apply to statistical outliers
        //! for multivariate features.
        //!
        //! \param[in] prior The model.
        //! \param[in] weightStyles The model weight styles.
        //! \param[in] sample The sample from \p model for which to compute
        //! a Winsorisation factor.
        //! \param[in] weights The weights to apply when computing the
        //! generalized c.d.f.
        //! \param[in] derate The derate to apply to minimum weight. The
        //! larger the derate the larger the minimum weight.
        static TDouble10Vec winsorisingWeight(const maths::CMultivariatePrior &prior,
                                              const maths_t::TWeightStyleVec &weightStyles,
                                              const TDouble10Vec &sample,
                                              const TDouble10Vec4Vec &weights,
                                              double derate);

        //! Returns true if the the \p is an unset first bucket time
        static bool isTimeUnset(core_t::TTime);

    protected:
        typedef core::CSmallVector<std::size_t, 1> TSize1Vec;
        typedef boost::unordered_map<std::size_t, TSize1Vec> TSizeSize1VecUMap;
        typedef TSizeSize1VecUMap::iterator TSizeSize1VecUMapItr;
        typedef TSizeSize1VecUMap::const_iterator TSizeSize1VecUMapCItr;
        typedef std::pair<model_t::EFeature, TSizeSize1VecUMap> TFeatureSizeSize1VecUMapPr;
        typedef std::vector<TFeatureSizeSize1VecUMapPr> TFeatureSizeSize1VecUMapPrVec;
        typedef TFeatureSizeSize1VecUMapPrVec::iterator TFeatureSizeSize1VecUMapPrVecItr;
        typedef TFeatureSizeSize1VecUMapPrVec::const_iterator TFeatureSizeSize1VecUMapPrVecCItr;

    protected:
        //! The maximum time a person or attribute is allowed to live
        //! without update.
        static const std::size_t MAXIMUM_PERMITTED_AGE;

        //! The maximum number of correlations we'll look for.
        static const std::size_t MAXIMUM_CORRELATIONS;

        //! Convenience for persistence.
        static const std::string EMPTY_STRING;

        //! Convenience for persistence.
        static const boost::any  EMPTY_ANY;

    protected:
        //! Remove heavy hitting people from the \p data if necessary.
        template<typename T, typename FILTER>
        void applyFilter(model_t::EExcludeFrequent exclude,
                         bool updateStatistics,
                         const FILTER &filter,
                         T &data) const
        {
            if (this->params().s_ExcludeFrequent & exclude)
            {
                std::size_t initialSize = data.size();
                data.erase(std::remove_if(data.begin(), data.end(), filter), data.end());
                if (updateStatistics && data.size() != initialSize)
                {
                    core::CStatistics::stat(stat_t::E_NumberExcludedFrequentInvocations).increment(1);
                }
            }
        }

        //! Get the predicate used for removing heavy hitting people.
        CPersonFrequencyGreaterThan personFilter(void) const;

        //! Get the predicate used for removing heavy hitting attributes.
        CAttributeFrequencyGreaterThan attributeFilter(void) const;

        //! Get the global configuration parameters.
        const SModelParams &params(void) const;

        //! Get the LearnRate parameter from the model configuration -
        //! this may be affected by the current feature being used
        virtual double learnRate(model_t::EFeature feature) const;

        //! Get the writable correlations.
        TFeatureKMostCorrelatedPrVec &correlations(void);

        //! Get the correlations for \p feature.
        const maths::CKMostCorrelated *correlations(model_t::EFeature feature) const;

        //! Get the writable correlations for \p feature.
        maths::CKMostCorrelated *correlations(model_t::EFeature feature);

        //! Get the writable correlated time series lookup for \p feature.
        void refreshCorrelated(model_t::EFeature feature,
                               const TSizeSizePrMultivariatePriorPtrDoublePrUMap &priors);

        //! Get the time series correlated with \p id for \p feature.
        const TSize1Vec &correlated(model_t::EFeature feature, std::size_t id) const;

        //! Get the start time of the current bucket.
        virtual core_t::TTime currentBucketStartTime(void) const = 0;

        //! Set the start time of the current bucket.
        virtual void currentBucketStartTime(core_t::TTime time) = 0;

        //! Get the influence calculator for the influencer field identified
        //! by \p iid and the \p feature.
        const CInfluenceCalculator *influenceCalculator(model_t::EFeature feature,
                                                        std::size_t iid) const;

        //! Writable access to the entire collection of extra data.
        TAnyVec &extraData(void);

        //! Get the person bucket counts.
        const TDoubleVec &personBucketCounts(void) const;
        //! Writable access to the person bucket counts.
        TDoubleVec &personBucketCounts(void);
        //! Set the total count of buckets in the window.
        void windowBucketCount(double windowBucketCount);
        //! Get the total count of buckets in the window.
        double windowBucketCount(void) const;

        //! Create the time series models for "n" newly observed people
        //! and "m" newly observed attributes.
        virtual void createNewModels(std::size_t n, std::size_t m) = 0;

        //! Reinitialize the time series models for recycled people and/or
        //! attributes.
        virtual void updateRecycledModels(void) = 0;

        //! Clear out large state objects for people/attributes that are pruned
        virtual void clearPrunedResources(const TSizeVec &people,
                                          const TSizeVec &attributes) = 0;

        //! Get the objects which calculates corrections for interim buckets.
        const CInterimBucketCorrector &interimValueCorrector(void) const;

        //! Check if any of the result-filtering detection rules apply to this series.
        bool shouldIgnoreResult(model_t::EFeature feature,
                                model_t::CResultType resultType,
                                const std::string &partitionFieldValue,
                                std::size_t pid,
                                std::size_t cid,
                                core_t::TTime time) const;

        //! Extract a feature and univariate prior pair from a state document.
        //!
        //! \param[in] dataType The data type.
        //! \param[in] featureTag The tag for the feature.
        //! \param[in] priorTag The tag for the prior.
        //! \param[in,out] traverser A state document traverser.
        //! \param[in,out] result The feature and prior are appended if they can
        //! be extracted from \p traverser.
        bool featurePriorAcceptRestoreTraverser(maths_t::EDataType dataType,
                                                const std::string &featureTag,
                                                const std::string &priorTag,
                                                core::CStateRestoreTraverser &traverser,
                                                TFeaturePriorPtrPrVec &result) const;

        //! Extract a feature and multivariate prior pair from a state document.
        //!
        //! \param[in] dataType The data type.
        //! \param[in] featureTag The tag for the feature.
        //! \param[in] priorTag The tag for the prior.
        //! \param[in,out] traverser A state document traverser.
        //! \param[in,out] result The feature and prior are appended if they can
        //! be extracted from \p traverser.
        bool featureMultivariatePriorAcceptRestoreTraverser(maths_t::EDataType dataType,
                                                            const std::string &featureTag,
                                                            const std::string &priorTag,
                                                            core::CStateRestoreTraverser &traverser,
                                                            TFeatureMultivariatePriorPtrPrVec &result) const;

        //! Extract a feature and collection of univariate priors from a state
        //! document.
        //!
        //! \param[in] dataType The data type.
        //! \param[in] featureTag The tag for the feature.
        //! \param[in] priorTag The tag for the prior.
        //! \param[in,out] traverser A state document traverser.
        //! \param[in,out] result Updated to include an element for the prior
        //! collection for a feature if they can be extracted from \p traverser.
        bool featurePriorsAcceptRestoreTraverser(maths_t::EDataType dataType,
                                                 const std::string &featureTag,
                                                 const std::string &priorTag,
                                                 core::CStateRestoreTraverser &traverser,
                                                 TFeaturePriorPtrVecMap &result) const;

        //! Extract a feature and collection of multivariate priors from a state
        //! document.
        //!
        //! \param[in] dataType The data type.
        //! \param[in] featureTag The tag for the feature.
        //! \param[in] priorTag The tag for the prior.
        //! \param[in,out] traverser A state document traverser.
        //! \param[in,out] result Updated to include an element for the prior
        //! collection for a feature if they can be extracted from \p traverser.
        bool featureMultivariatePriorsAcceptRestoreTraverser(maths_t::EDataType dataType,
                                                             const std::string &featureTag,
                                                             const std::string &priorTag,
                                                             core::CStateRestoreTraverser &traverser,
                                                             TFeatureMultivariatePriorPtrVecMap &result) const;

        //! Extract a feature and a map of correlated pairs to correlate priors
        //! from a state document.
        //!
        //! \param[in] dataType The data type.
        //! \param[in] featureTag The tag for the feature.
        //! \param[in] priorTag The tag for the prior.
        //! \param[in,out] traverser A state document traverser.
        //! \param[in,out] result Updated to include an element for the prior
        //! collection for a feature if they can be extracted from \p traverser.
        bool featureCorrelatePriorsAcceptRestoreTraverser(maths_t::EDataType dataType,
                                                          const std::string &featureTag,
                                                          const std::string &priorTag,
                                                          core::CStateRestoreTraverser &traverser,
                                                          TFeatureSizeSizePrMultivariatePriorPtrDoublePrUMapMap &result) const;

        //! Create a collection of time series decompositions by decoding
        //! a state document.
        //!
        //! \param[in] featureTag The tag for the feature.
        //! \param[in] trendTag The tag for each decomposition.
        //! \param[in,out] traverser A state document traverser.
        //! \param[in,out] result The collection is appended if it can be
        //! extracted from \p traverser.
        bool featureDecompositionsAcceptRestoreTraverser(const std::string &featureTag,
                                                         const std::string &trendTag,
                                                         core::CStateRestoreTraverser &traverser,
                                                         TFeatureDecompositionPtr1VecVecPrVec &result) const;

        //! Create a collection of decay rate controller objects by decoding
        //! a state document.
        //!
        //! \param[in] featureTag The tag for the feature.
        //! \param[in] controllerTag The tag for each controller.
        //! \param[in,out] traverser A state document traverser.
        //! \param[in,out] result The controller collection is appended if
        //! it can be extracted from \p traverser.
        bool featureControllersAcceptRestoreTraverser(const std::string &featureTag,
                                                      const std::string &controllerTag,
                                                      core::CStateRestoreTraverser &traverser,
                                                      TFeatureDecayRateControllerArrayVecPrVec &result) const;

        //! Restore some feature correlations.
        bool featureCorrelationsAcceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

        //! Restore interim bucket corrector.
        bool interimBucketCorrectorAcceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

        //! Restore some extra data.
        bool extraDataAcceptRestoreTraverser(const model_t::TAnyRestoreFunc &extraDataRestoreFunc,
                                             core::CStateRestoreTraverser &traverser);

        //! Persist a single feature univariate prior.
        static void featurePriorAcceptPersistInserter(const std::string &featureTag,
                                                      model_t::EFeature feature,
                                                      const std::string &priorTag,
                                                      const maths::CPrior &prior,
                                                      core::CStatePersistInserter &inserter);

        //! Persist a single feature multivariate prior.
        static void featureMultivariatePriorAcceptPersistInserter(const std::string &featureTag,
                                                                  model_t::EFeature feature,
                                                                  const std::string &priorTag,
                                                                  const maths::CMultivariatePrior &prior,
                                                                  core::CStatePersistInserter &inserter);

        //! Persist a vector of univariate priors for a feature.
        static void featurePriorsAcceptPersistInserter(const std::string &featureTag,
                                                       model_t::EFeature feature,
                                                       const std::string &priorTag,
                                                       const TPriorPtrVec &priors,
                                                       core::CStatePersistInserter &inserter);

        //! Persist a vector of multivariate priors for a feature.
        static void featureMultivariatePriorsAcceptPersistInserter(const std::string &featureTag,
                                                                   model_t::EFeature feature,
                                                                   const std::string &priorTag,
                                                                   const TMultivariatePriorPtrVec &priors,
                                                                   core::CStatePersistInserter &inserter);

        //! Persist a map of correlated pairs to correlate priors for a feature.
        static void featureCorrelatePriorsAcceptPersistInserter(const std::string &featureTag,
                                                                model_t::EFeature feature,
                                                                const std::string &priorTag,
                                                                const TSizeSizePrMultivariatePriorPtrDoublePrUMap &priors,
                                                                core::CStatePersistInserter &inserter);

        //! Persist a vector of time series decompositions for a feature.
        static void featureDecompositionsAcceptPersistInserter(const std::string &featureTag,
                                                               model_t::EFeature feature,
                                                               const std::string &trendTag,
                                                               const TDecompositionPtr1VecVec &trends,
                                                               core::CStatePersistInserter &inserter);

        //! Persist a vector of decay rate controllers.
        static void featureControllersAcceptPersistInserter(const std::string &featureTag,
                                                            model_t::EFeature feature,
                                                            const std::string &controllerTag,
                                                            const TDecayRateControllerArrayVec &controllers,
                                                            core::CStatePersistInserter &inserter);

        //! Persist the feature correlations.
        void featureCorrelationsAcceptPersistInserter(const std::string &tag,
                                                      core::CStatePersistInserter &inserter) const;

        //! Persist the interim bucket corrector.
        void interimBucketCorrectorAcceptPersistInserter(const std::string &tag,
                                                         core::CStatePersistInserter &inserter) const;

        //! Persist the extra data.
        void extraDataAcceptPersistInserter(const std::string &tag,
                                            core::CStatePersistInserter &inserter) const;

        //! Create a stub version of CPrior for use when pruning people or
        //! attributes to free memory resource.
        static maths::CPrior *tinyPrior(void);

        //! Create a stub version of CMultivariatePrior for use when pruning
        //! people or attributes to free memory resource.
        static maths::CMultivariatePrior *tinyPrior(std::size_t dimension);

        //! Create a stub version of CTimeSeriesDecompositionInterface for
        //! use when pruning people or attributes to free memory resource.
        static maths::CTimeSeriesDecompositionInterface *tinyDecomposition(void);

    private:
        typedef boost::reference_wrapper<const SModelParams> TModelParamsCRef;
        typedef boost::shared_ptr<CInterimBucketCorrector> TInterimBucketCorrectorPtr;

    private:
        //! Set the current bucket total count.
        virtual void currentBucketTotalCount(uint64_t totalCount) = 0;

        //! Perform derived class specific operations to accomplish skipping sampling
        virtual void doSkipSampling(core_t::TTime startTime, core_t::TTime endTime) = 0;

    private:
        //! The global configuration parameters.
        TModelParamsCRef m_Params;

        //! The data gatherer. (This is not persisted by the model hierarchy.)
        TDataGathererPtr m_DataGatherer;

        //! The bucket count of each person in the exponentially decaying
        //! window with decay rate equal to m_DecayRate.
        TDoubleVec m_PersonBucketCounts;

        //! The total number of buckets in the exponentially decaying window
        //! with decay rate equal to m_DecayRate.
        double m_BucketCount;

        //! Maintains the correlations between time series for each feature.
        TFeatureKMostCorrelatedPrVec m_Correlations;

        //! A lookup by time series identifier for correlated time series for
        //! each feature.
        TFeatureSizeSize1VecUMapPrVec m_CorrelatedLookup;

        //! The influence calculators to use for each feature which is being
        //! modeled.
        TFeatureInfluenceCalculatorCPtrPrVecVec m_InfluenceCalculators;

        //! A corrector that calculates adjustments for values of interim buckets.
        TInterimBucketCorrectorPtr m_InterimBucketCorrector;

        //! Extra data annotated on this model.
        TAnyVec m_ExtraData;
};

}
}

#endif // INCLUDED_prelert_model_CModel_h
