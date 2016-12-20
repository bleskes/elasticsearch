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
#ifndef INCLUDED_prelert_model_CAnomalyDetector_h
#define INCLUDED_prelert_model_CAnomalyDetector_h

#include <core/CNonCopyable.h>
#include <core/CoreTypes.h>
#include <core/CSmallVector.h>

#include <model/CAnomalyChain.h>
#include <model/CBoxPlotData.h>
#include <model/CEventData.h>
#include <model/CLimits.h>
#include <model/CModelEnsemble.h>
#include <model/CModelConfig.h>
#include <model/CModelFactory.h>
#include <model/FunctionTypes.h>
#include <model/ModelTypes.h>
#include <model/ImportExport.h>

#include <boost/any.hpp>
#include <boost/function.hpp>
#include <boost/shared_ptr.hpp>

#include <map>
#include <string>
#include <vector>

#include <stdint.h>


namespace prelert
{
namespace core
{
class CStatePersistInserter;
class CStateRestoreTraverser;
}
namespace model
{
class CSearchKey;

//! \brief
//! Interface for detecting and reporting anomalies in different
//! types of unstructured data.
//!
//! DESCRIPTION:\n
//! Given a stream of data categorised by a particular field name,
//! this reports on anomalies in that data.
//!
//! It is possible to handle multiple disjoint data sets.  It is up
//! to the caller to notify the class when a new data set is being
//! received by calling startNewDataSet.
//!
//! IMPLEMENTATION DECISIONS:\n
//! Data must be received in increasing time order.  (Our Splunk
//! app achieves this for historical searches by manipulating the
//! times passed to this class.)
//!
//! If the field values mean that more than a configurable amount
//! of memory is consumed by the models, for example if the number
//! of by field values is too high such as using "by _cd". (The
//! limit is controlled by prelertlimits.conf.)
//!
//! The different methods used for anomaly detection are largely
//! encapsulated by the CModel class hierarchy.  This means it is
//! possible to implement the function to output anomalies in terms
//! of that interface.
//!
//! This level and below are designed to be more-or-less independent
//! of Splunk or the API and as such we drop the Splunk terminology
//! in this and the classes it uses in favour of our internal terminology.
//! In particular, a Splunk field value either maps to a person or a
//! person's attribute. The person terminology is used because we can
//! choose to analyse these field values either individually or as
//! a population.

class MODEL_EXPORT CAnomalyDetector : private core::CNonCopyable
{
    public:
        typedef std::vector<std::string>                        TStrVec;
        typedef std::vector<const std::string*>                 TStrCPtrVec;
        typedef boost::shared_ptr<const std::string>            TStrPtr;
        typedef std::vector<CBoxPlotData>                       TBoxPlotDataVec;

        typedef boost::shared_ptr<const CModelFactory> TModelFactoryCPtr;

        //! A shared pointer to an instance of this class
        typedef boost::shared_ptr<CAnomalyDetector> TAnomalyDetectorPtr;

        typedef boost::function<void (const std::string &,
                                      const std::string &,
                                      const std::string &,
                                      const std::string &,
                                      const CBoxPlotData &)> TOutputBoxPlotDataFunc;
        typedef CModelConfig::TStrSet TStrSet;

    public:
        //! State version.  This must be incremented every time a change to the
        //! state is made that requires existing state to be discarded
        static const std::string STATE_VERSION;

        //! Name of the count field
        static const std::string COUNT_NAME;

        //! Indicator that the GUI should expect a field name but no field value
        //! (because for a distinct count we're only interested in the number of
        //! different values, not the values themselves)
        static const std::string DISTINCT_COUNT_NAME;

        //! Indicator that the GUI should use a description template based on
        //! rare events rather than numerous events
        static const std::string RARE_NAME;

        //! Indicator that the GUI should use a description template based on
        //! information content of events
        static const std::string INFO_CONTENT_NAME;

        //! Output function names for metric anomalies
        static const std::string MEAN_NAME;
        static const std::string MEDIAN_NAME;
        static const std::string MIN_NAME;
        static const std::string MAX_NAME;
        static const std::string VARIANCE_NAME;
        static const std::string SUM_NAME;
        static const std::string LAT_LONG_NAME;
        static const std::string EMPTY_STRING;


    public:
        CAnomalyDetector(int identifier,
                         CLimits &limits,
                         const CModelConfig &modelConfig,
                         const std::string &partitionFieldValue,
                         core_t::TTime firstTime,
                         const TModelFactoryCPtr &modelFactory);

        //! Create a copy that will result in the same persisted state as the
        //! original.  This is effectively a copy constructor that creates a
        //! copy that's only valid for a single purpose.  The boolean flag is
        //! redundant except to create a signature that will not be mistaken for
        //! a general purpose copy constructor.
        CAnomalyDetector(bool isForPersistence,
                         const CAnomalyDetector &other);

        virtual ~CAnomalyDetector(void);

        //! Get the total number of people which this is modeling.
        size_t numberActivePeople(void) const;

        //! Get the total number of attributes which this is modeling.
        size_t numberActiveAttributes(void) const;

        //! Get the maximum size of all the member containers.
        size_t maxDimension(void) const;

        //! For the operationalised version of the product, we may create models
        //! that need to reflect the fact that no data of a particular type was
        //! seen for a period before the creation of the models, but WITHOUT
        //! reporting any results for the majority of that period.  This method
        //! provides that facility.
        void zeroModelsToTime(core_t::TTime time);

        //! Populate the object from a state document
        bool acceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

        //! Restore state for statics - this is only called from the
        //! simple count detector to ensure singleton behaviour
        bool staticsAcceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

        //! Find the partition field value given part of an state document.
        //!
        //! \note This is static so it can be called before the state is fully
        //! deserialised, because we need this value before to restoring the
        //! detector.
        static bool partitionFieldAcceptRestoreTraverser(core::CStateRestoreTraverser &traverser,
                                                         std::string &partitionFieldValue);

        //! Find the detector keys given part of an state document.
        //!
        //! \note This is static so it can be called before the state is fully
        //! deserialised, because we need these before to restoring the detector.
        static bool keyAcceptRestoreTraverser(core::CStateRestoreTraverser &traverser,
                                              CSearchKey &key);

        //! Persist the detector keys separately to the rest of the state.
        //! This must be done for a 100% streaming state restoration because
        //! the key must be known before a detector object is created into
        //! which other state can be restored.
        void keyAcceptPersistInserter(core::CStatePersistInserter &inserter) const;

        //! Persist the partition field separately to the rest of the state.
        //! This must be done for a 100% streaming state restoration because
        //! the partition field must be known before a detector object is
        //! created into which other state can be restored.
        void partitionFieldAcceptPersistInserter(core::CStatePersistInserter &inserter) const;

        //! Persist state for statics - this is only called from the
        //! simple count detector to ensure singleton behaviour
        void staticsAcceptPersistInserter(core::CStatePersistInserter &inserter) const;

        //! Persist state by passing information to the supplied inserter
        //!
        //! \note Some information is duplicated in keyAcceptPersistInserter()
        //! and partitionFieldAcceptPersistInserter(), but the Splunk app
        //! requires it at this level
        void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

        //! Get the cue for this detector.  This consists of the search key cue
        //! with the partition field value appended.
        std::string toCue(void) const;

        //! Debug representation.  Note that operator<<() is more efficient than
        //! generating this debug string and immediately outputting it to a
        //! stream.
        std::string debug(void) const;

        //! Check if this is a simple count detector.
        virtual bool isSimpleCount(void) const;

        //! Get the fields to extract from a record for processing by this detector.
        const TStrVec &fieldsOfInterest(void) const;

        //! Extract and add the necessary details of an event record.
        void addRecord(core_t::TTime time,
                       const TStrCPtrVec &fieldValues,
                       boost::any &extraData);

        //! Overload with no extra data.
        void addRecord(core_t::TTime time,
                       const TStrCPtrVec &fieldValues);

        //! Used when in stats-only mode to write out the bucket stats for ALL
        //! series (not just those that are anomalous)
        void gatherAndOutputStats(core_t::TTime bucketStartTime,
                                  core_t::TTime bucketEndTime,
                                  const CModel::TBucketStatsOutputFunc &outputFunc);

        //! Update the results with this detector model's results.
        void buildResults(core_t::TTime bucketStartTime,
                          core_t::TTime bucketEndTime,
                          CHierarchicalResults &results);

        //! Update the results with this detector model's results.
        void buildInterimResults(core_t::TTime bucketStartTime,
                                 core_t::TTime bucketEndTime,
                                 CHierarchicalResults &results);

        void generateModelDebugData(core_t::TTime bucketStartTime,
                                    core_t::TTime bucketEndTime,
                                    double boundsPercentile,
                                    const TStrSet &terms,
                                    TBoxPlotDataVec &boxPlots) const;

        //! Restart event rate monitoring for a new live data set.
        //!
        //! This is only important if we are batching data in which
        //! case the batch is computed relative to \p firstTime.
        //!
        //! \param[in] firstTime The start time of the new data set.
        void startNewDataSet(core_t::TTime firstTime);

        //! Remove dead models, i.e. those models that have more-or-less
        //! reverted back to their non-informative state.  BE CAREFUL WHEN
        //! CALLING THIS METHOD that you do not hold pointers to any models
        //! that may be deleted as a result of this call.
        virtual void pruneModels(void);

        //! Reset bucket.
        void resetBucket(core_t::TTime bucketStart);

        //! Print the detector memory usage to the given stream
        void showMemoryUsage(std::ostream &stream) const;

        //! Return the total memory usage
        std::size_t memoryUsage(void);

        //! Get end of the last complete bucket we've observed.
        const core_t::TTime &lastBucketEndTime(void) const;

        //! Get writable end of the last complete bucket we've observed.
        core_t::TTime &lastBucketEndTime(void);

        //! Access to the bucket length being used in the current models.  This
        //! can be used to detect discrepancies between the model config and
        //! existing models.
        core_t::TTime modelBucketLength(void) const;

        //! Get a description of this anomaly detector.
        std::string description(void) const;

        //! Get the model collection.
        const CModelEnsemble &models(void) const;
        //! Get the model collection.
        CModelEnsemble &models(void);

        //! Roll time forwards to \p time.
        void timeNow(core_t::TTime time);

        //! Rolls time to \p endTime while skipping sampling the models for buckets within the gap
        //! \param[in] endTime The end of the time interval to skip sampling.
        void skipSampling(core_t::TTime endTime);

    protected:
        //! This function is called before adding a record allowing
        //! for varied preprocessing.
        virtual const TStrCPtrVec &preprocessFieldValues(const TStrCPtrVec &fieldValues);

    private:
        // Shared code for building results
        template<typename SAMPLE_FUNC,
                 typename LAST_SAMPLED_BUCKET_UPDATE_FUNC>
        void buildResultsHelper(core_t::TTime bucketStartTime,
                                core_t::TTime bucketEndTime,
                                SAMPLE_FUNC sampleFunc,
                                LAST_SAMPLED_BUCKET_UPDATE_FUNC lastSampledBucketUpdateFunc,
                                CHierarchicalResults &results);

        //! Updates the last sampled bucket
        void updateLastSampledBucket(core_t::TTime bucketEndTime);

        //! Does not update the last sampled bucket. To be used
        //! when interim results are calculated.
        void noUpdateLastSampledBucket(core_t::TTime bucketEndTime) const;

    protected:
        //! Configurable limits
        CLimits                 &m_Limits;

    private:
        //! An identifier for the search for which this is detecting anomalies.
        int                     m_Identifier;

        //! Configurable behaviour
        const CModelConfig      &m_ModelConfig;

        //! The value of the partition field for this detector.
        TStrPtr                 m_PartitionFieldValue;

        //! The end of the last complete bucket we've observed.  This is an OPEN
        //! endpoint, i.e. this time is the lowest time NOT in the last bucket.
        core_t::TTime           m_LastBucketEndTime;

        //! The models of the data in which we are detecting anomalies.
        CModelEnsemble          m_Models;

        //! Functionality to chain together compound anomalies.
        CAnomalyChain           m_AnomalyChain;

        //! Is this a cloned detector containing the bare minimum information
        //! necessary to create a valid persisted state?
        bool                    m_IsForPersistence;

    friend class CModelView;

    friend MODEL_EXPORT std::ostream &operator<<(std::ostream &,
                                                 const CAnomalyDetector &);
};

MODEL_EXPORT
std::ostream &operator<<(std::ostream &strm, const CAnomalyDetector &detector);

//! \brief A view into the models of a CAnomalyDetector object.
class MODEL_EXPORT CModelView
{
    public:
        CModelView(const CAnomalyDetector &detector);

        //! Extract the models from the detector.
        const CModelEnsemble &models(void) const;

    private:
        const CAnomalyDetector *m_Detector;
};

}
}

#endif // INCLUDED_prelert_model_CAnomalyDetector_h
