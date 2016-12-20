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

#ifndef INCLUDED_prelert_model_CModelEnsemble_h
#define INCLUDED_prelert_model_CModelEnsemble_h

#include <core/CNonCopyable.h>
#include <core/CoreTypes.h>
#include <core/CMemory.h>

#include <model/CBoxPlotData.h>
#include <model/CModel.h>
#include <model/CModelConfig.h>
#include <model/CModelFactory.h>
#include <model/FunctionTypes.h>
#include <model/ImportExport.h>
#include <model/ModelTypes.h>

#include <boost/optional.hpp>
#include <boost/ref.hpp>
#include <boost/shared_ptr.hpp>

#include <cstddef>
#include <deque>
#include <string>
#include <utility>
#include <vector>

namespace prelert
{
namespace core
{
class CStatePersistInserter;
class CStateRestoreTraverser;
}
namespace model
{
class CDataGatherer;
class CSearchKey;
class CEventData;
class CResourceMonitor;

//! \brief An ensemble of (CModel) models.
//!
//! DESCRIPTION:\n
//! This encapsulates the functionality to batch data by time
//! to handle periodic data and to compare two different data
//! sets by offset relative to their start times. It also provides
//! various utilities to operate on all models in the ensemble
//! at once: for example, to sample the data.
//!
//! IMPLEMENTATION:\n
//! This is implemented using the CModel hierarchy so that all
//! models can be stored in an ensemble. Currently, since it
//! holds a single factory object, all models stored must have
//! the same type.
class MODEL_EXPORT CModelEnsemble : private core::CNonCopyable
{
    public:
        typedef model_t::TFeatureVec TFeatureVec;
        typedef boost::shared_ptr<CDataGatherer> TDataGathererPtr;
        typedef boost::shared_ptr<CModel> TModelPtr;
        typedef boost::shared_ptr<const CModel> TModelCPtr;
        typedef boost::shared_ptr<const CModelFactory> TModelFactoryCPtr;
        typedef std::vector<std::string> TStrVec;
        typedef TStrVec::const_iterator TStrVecCItr;
        typedef std::vector<const std::string*> TStrCPtrVec;
        typedef boost::reference_wrapper<const std::string> TStrCRef;
        typedef std::pair<std::size_t, TStrCRef> TSizeStrCRefPr;
        typedef std::vector<CBoxPlotData> TBoxPlotDataVec;

        typedef CModelConfig::TStrSet TStrSet;
    public:
        //! \param[in] modelConfig The model configuration.
        //! \param[in] modelFactory The model factory.
        //! \param[in] startTime The start time for the observation period.
        CModelEnsemble(const CModelConfig &modelConfig,
                       const TModelFactoryCPtr &modelFactory,
                       core_t::TTime startTime);

        //! Create a copy that will result in the same persisted state as the
        //! original.  This is effectively a copy constructor that creates a
        //! copy that's only valid for a single purpose.  The boolean flag is
        //! redundant except to create a signature that will not be mistaken
        //! for a general purpose copy constructor.
        CModelEnsemble(bool isForPersistence,
                       const CModelEnsemble &other);

        //! Populate the members that are not determined by the model
        //! config from the supplied XML node hierarchy
        bool acceptRestoreTraverser(core::CStateRestoreTraverser &traverser);

        //! Persist state by passing information to the supplied inserter
        void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

        //! Get the bucketing interval.
        core_t::TTime bucketLength(void) const;

        //! Start collecting data for comparison with the reference data set.
        void startComparison(const CModelConfig &modelConfig,
                             core_t::TTime startTime);

        //! Get the functions which which are being modeled.
        function_t::EFunction function(void) const;

        //! Get the search keys which comprise this model.
        const CSearchKey &searchKey(void) const;

        //! Get an iterator to the first influencer field.
        TStrVecCItr beginInfluencers(void) const;

        //! Get an interator to the end of the influencer fields.
        TStrVecCItr endInfluencers(void) const;

        //! Get the fields which will be modeled.
        const TStrVec &fieldsOfInterest(void) const;

        //! Get a description of the searches being modeled.
        std::string description(void) const;

        //! Get the name of the person identified by \p pid.
        const std::string &personName(std::size_t pid) const;

        //! Get the total number of people which this is modeling.
        std::size_t numberActivePeople(void) const;

        //! Get the total number of attributes which this is modeling.
        std::size_t numberActiveAttributes(void) const;

        //! Get the maximum size of all the member containers.
        std::size_t maxDimension(void) const;

        //! Get the total number of by field values which this is modeling.
        std::size_t numberByFieldValues(void) const;

        //! Get the total number of over fields values which this is modeling.
        std::size_t numberOverFieldValues(void) const;

        //! Process the specified field values.
        bool processFields(const TStrCPtrVec &fieldValues,
                           CEventData &result,
                           CResourceMonitor &resourceMonitor);

        //! Initializes simple counting by adding a person called "count".
        void initSimpleCounting(CResourceMonitor &resourceMonitor);

        //! Add the arrival of a record summarized by \p data.
        bool addArrival(const TStrCPtrVec &fieldValues,
                        CEventData &eventData,
                        CResourceMonitor &resourceMonitor);

        //! Set the extra data for the person identified by \p pid.
        void extraData(core_t::TTime time, std::size_t pid, boost::any &extraData);

        //! Reset the start time for the live models.
        void startTime(core_t::TTime startTime);

        //! Update the period of the data. This is expressed as a
        //! multiple of the batch size.
        void period(std::size_t period);

        //! Get the total number of batches seen to date.
        std::size_t numberBatches(void) const;

        //! Get the batch corresponding to \p time.
        std::size_t batch(core_t::TTime time) const;

        //! Get the time that will be compared with \p time in the
        //! reference data set.
        core_t::TTime comparedTime(core_t::TTime time) const;

        //! Get the model at \p time if available.
        //!
        //! \note This creates the batch which includes \p time if necessary.
        CModel *model(core_t::TTime time);

        //! Get the model at \p time if available.
        const CModel *model(core_t::TTime time) const;

        //! Clear all models.
        void clearModels(void);

        //! Reset bucket.
        void resetBucket(core_t::TTime bucketStart);

        //! Get the memory used by the models
        void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const;

        //! Get the memory used by the models
        std::size_t memoryUsage(void) const;

        //! Sample the models in the interval [\p startTime, \p endTime].
        void sample(core_t::TTime startTime,
                    core_t::TTime endTime,
                    CResourceMonitor &resourceMonitor);

        //! Sample bucket statistics and any other state needed to compute
        //! probabilities in the interval [\p startTime, \p endTime], but
        //! does not update the models.
        void sampleBucketStatistics(core_t::TTime startTime,
                                    core_t::TTime endTime,
                                    CResourceMonitor &resourceMonitor);

        //! Rolls time to \p endTime while skipping sampling the models for
        //! buckets within the gap.
        //!
        //! \param[in] endTime The end of the time interval to skip sampling.
        void skipSampling(core_t::TTime endTime);

        //! Gather bucket statistics for the interval [\p startTime, \p endTime].
        //! Does not update the models or calculate probabilities.
        void gatherAndOutputStats(core_t::TTime startTime,
                                  core_t::TTime endTime,
                                  const std::string &partitionFieldValue,
                                  const CModel::TBucketStatsOutputFunc &outputFunc,
                                  CResourceMonitor &resourceMonitor);

        //! Prune any person models which haven't been updated for a
        //! sufficiently long period. This is based on the prior decay
        //! rates and the number of batches into which we are partitioning
        //! time.
        void pruneModels(core_t::TTime time, std::size_t maximumAge);

        //! Generate a box plot from the model.
        void generateModelDebugDataInfo(core_t::TTime startTime,
                                        core_t::TTime endTime,
                                        double boundsPercentile,
                                        const TStrSet &terms,
                                        const std::string &partitionFieldName,
                                        const std::string &partitionFieldValue,
                                        const std::string &overFieldName,
                                        const std::string &byFieldName,
                                        TBoxPlotDataVec &boxPlots) const;

        //! Roll time forwards to \p time.
        void timeNow(core_t::TTime time);

    private:
        typedef std::pair<std::size_t, std::size_t> TSizeSizePr;
        class CImpl;
        typedef boost::optional<CImpl> TOptionalImpl;
        typedef std::vector<TModelPtr> TModelPtrVec;

        //! The implementation of a model ensemble.
        class CImpl
        {
            public:
                //! \note that this is only used for restoring persisted
                //! models and that the object this creates is unusable.
                CImpl(void);

                //! Create a new collection with no time series.
                //!
                //! \param[in] modelConfig The model configuration.
                //! \param[in] modelFactory The model factory.
                //! \param[in] dataGatherer The model data gatherer.
                //! \param[in] startTime The start time for the observation period.
                CImpl(const CModelConfig &modelConfig,
                      const TModelFactoryCPtr &modelFactory,
                      const TDataGathererPtr &dataGatherer,
                      core_t::TTime startTime);

                //! Create a copy that will result in the same persisted state
                //! as the original.  This is effectively a copy constructor
                //! that creates a copy that's only valid for a single purpose.
                //! The boolean flag is redundant except to create a signature
                //! that will not be mistaken for a general purpose copy
                //! constructor.
                CImpl(bool isForPersistence, const CImpl &other);

                //! Efficiently swap two collections.
                //!
                //! \param[in,out] other The collection to swap.
                //! \note Uses fast implementations of swap where available.
                void swap(CImpl &other);

                //! Persist state by passing information to the supplied inserter
                void acceptPersistInserter(core::CStatePersistInserter &inserter) const;

                //! Populate the members that are not determined by the model
                //! config from the supplied state document.
                bool acceptRestoreTraverser(const TDataGathererPtr &dataGatherer,
                                            core::CStateRestoreTraverser &traverser);

                //! Get the start time for the observation period.
                core_t::TTime startTime(void) const;

                //! Reset the start the time for the component models.
                void startTime(core_t::TTime startTime);

                //! Get the bucketing time interval length.
                core_t::TTime bucketLength(void) const;

                //! Update the period of the data.
                void period(std::size_t period);

                //! Compute time modulo the period of the data.
                core_t::TTime moduloPeriod(core_t::TTime time) const;

                //! Get the time in the reference set corresponding to \p time
                //! in the \p live set. We map live times to the reference set
                //! by using the same offset relative to the start of the data
                //! set (modulo the period).
                //!
                //! \param live The live data set.
                //! \param time The time of interest in the live data set.
                core_t::TTime referenceTime(const CImpl &live,
                                            core_t::TTime time) const;

                //! Get the total number of batches seen to date.
                std::size_t numberBatches(void) const;

                //! Get the batch corresponding to \p time.
                std::size_t batch(core_t::TTime time) const;

                //! Get the memory used by the models
                void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const;

                //! Get the memory used by the models
                std::size_t memoryUsage(void) const;

                //! Get the object used to gatherer time series data.
                CDataGatherer &dataGatherer(void) const;

                //! Get the placeholder for the object used to gatherer time series data.
                TDataGathererPtr dataGathererPtr(void) const;

                //! Get the factory object used to create new models.
                TModelFactoryCPtr modelFactory(void) const;

                //! Get the model for the time series at \p time if it exists.
                TModelPtr model(core_t::TTime time) const;

                //! Set the extra data for the person identified by \p pid.
                void extraData(core_t::TTime time,
                               std::size_t pid,
                               boost::any &extraData) const;

                //! Clear out all the current models.
                void clearModels(void);

                //! Reset bucket.
                void resetBucket(core_t::TTime bucketStart);

                //! Sample the data in the interval [\p startTime, \p endTime].
                //!
                //! \param[in] startTime The start of the time period to sample.
                //! \param[in] endTime The end of the time period to sample.
                void sample(core_t::TTime startTime,
                            core_t::TTime endTime,
                            CResourceMonitor &resourceMonitor);

                //! Sample bucket statistics and any other state needed to
                //! compute probabilities in the interval
                //! [\p startTime, \p endTime], but does not update the models.
                void sampleBucketStatistics(core_t::TTime startTime,
                                            core_t::TTime endTime,
                                            CResourceMonitor &resourceMonitor);

                //! Sample bucket statistics and any other state needed to
                //! compute probabilities in the interval
                //! [\p startTime, \p endTime], but does not update the models.
                void sampleOutOfPhase(core_t::TTime startTime,
                                      core_t::TTime endTime,
                                      CResourceMonitor &resourceMonitor);

                //! Rolls time to \p endTime while skipping sampling the models for
                //! buckets within the gap.
                //!
                //! \param[in] endTime The end of the time interval to skip sampling.
                void skipSampling(core_t::TTime endTime);

                //! Gather bucket statistics for the interval
                //! [\p startTime, \p endTime].  Does not update the models or
                //! calculate probabilities.
                void gatherAndOutputStats(core_t::TTime startTime,
                                          core_t::TTime endTime,
                                          const std::string &partitionFieldValue,
                                          const CModel::TBucketStatsOutputFunc &outputFunc,
                                          CResourceMonitor &resourceMonitor);

                //! Prune any person models which haven't been updated for a
                //! sufficiently long period. This is based on the prior decay
                //! rates and the number of batches into which we are partitioning
                //! time.
                void pruneModels(core_t::TTime time, std::size_t maximumAge);

                //! Add all batches including \p time.
                //!
                //! \param[in] time The time of interest.
                //! \param[in] referenceModels Model of reference data set.
                void refreshBatches(core_t::TTime time,
                                    const TOptionalImpl &referenceModels);

                //! Generate a box plot from the model.
                void generateModelDebugDataInfo(core_t::TTime startTime,
                                                core_t::TTime endTime,
                                                double boundsPercentile,
                                                const TStrSet &terms,
                                                const std::string &partitionFieldName,
                                                const std::string &partitionFieldValue,
                                                const std::string &overFieldName,
                                                const std::string &byFieldName,
                                                TBoxPlotDataVec &boxPlots) const;

            private:
                //! Get batches which overlap \p time.
                //!
                //! \param[in] time The time of interest.
                //! \warning  These are *not* computed modulo the period so
                //! don't index m_Models.
                TSizeSizePr overlappingBatches(core_t::TTime time) const;

                //! Get the batch in these reference models that should be
                //! compared with the time \p time in the \p live data set.
                //!
                //! \param[in] live The live data set models.
                //! \param[in] time The time of interest in the live data set.
                std::size_t referenceBatch(const CImpl &live,
                                           core_t::TTime time) const;

                //! Get the model in these reference models that should be
                //! compared with the model of the time series at the time
                //! \p time in the \p live data set.
                //!
                //! \param[in] live The live data set models.
                //! \param[in] time The time of interest in the live data set.
                TModelPtr referenceModel(const CImpl &live,
                                         core_t::TTime time) const;

                typedef void (CModel::*TModelMemFunP)(core_t::TTime,
                                                      core_t::TTime,
                                                      CResourceMonitor &resourceMonitor);

                //! Contains the skeleton of sample() and
                //! sampleBucketStatistics().  Which of the two functions to
                //! call is passed as a pointer to member function.
                void sampleHelper(core_t::TTime startTime,
                                  core_t::TTime endTime,
                                  CResourceMonitor &resourceMonitor,
                                  TModelMemFunP func);

            private:
                //! The data gatherers.
                TDataGathererPtr m_DataGatherer;

                //! The factory for new data gatherers and models.
                TModelFactoryCPtr m_ModelFactory;

                //! The data models.
                TModelPtrVec m_Models;

                //! The start time for the observation period.
                core_t::TTime m_StartTime;

                //! The period in which to bucket aggregate data.
                core_t::TTime m_BucketLength;

                //! The period, as a multiple of the bucket length, of a batch.
                core_t::TTime m_BatchLength;

                //! The overlap between adjacent batches in multiples of the
                //! bucket length.
                std::size_t m_BatchOverlap;

                //! The period of the data in multiples of the batch length.
                std::size_t m_Period;
        };

    private:
        //! The live model collection.
        CImpl m_LiveModels;

        //! The reference model collection.
        TOptionalImpl m_ReferenceModels;
};

}
}

#endif // INCLUDED_prelert_model_CModelEnsemble_h
