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

#include <model/CModelEnsemble.h>

#include <core/CLogger.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>

#include <maths/CIntegerTools.h>

#include <model/CBoxPlotData.h>
#include <model/CDataGatherer.h>
#include <model/CModelConfig.h>
#include <model/CModelDetailsView.h>
#include <model/CResourceMonitor.h>
#include <model/CSearchKey.h>

#include <boost/bind.hpp>

namespace prelert
{
namespace model
{

namespace
{

namespace detail
{
typedef boost::shared_ptr<CDataGatherer> TDataGathererPtr;
typedef boost::shared_ptr<const CModelFactory> TModelFactoryCPtr;
typedef CModel::CModelDetailsViewPtr TModelDetailsViewPtr;

std::size_t batch(core_t::TTime time,
                  core_t::TTime startTime,
                  core_t::TTime batchLength)
{
    if (time < startTime)
    {
        return 0;
    }

    // Batches are open above so if time falls on a batch boundary it is
    // included in the later batch.
    core_t::TTime elapsedTime = time - maths::CIntegerTools::floor(startTime, batchLength);
    return static_cast<std::size_t>(elapsedTime / batchLength);
}

TDataGathererPtr dataGatherer(const TModelFactoryCPtr &factory,
                              core_t::TTime startTime)
{
    CModelFactory::SGathererInitializationData initData(startTime);
    return TDataGathererPtr(factory->makeDataGatherer(initData));
}

}

const std::string SIMPLE_COUNT_DETECTOR_PERSON_NAME("count");
const std::string DATA_GATHERER_TAG("a");
const std::string LIVE_MODELS_TAG("b");
const std::string REFERENCE_MODELS_TAG("c");
const std::string MODEL_TAG("d");
const std::string START_TIME_TAG("e");
const std::string EMPTY_STRING;

}

CModelEnsemble::CModelEnsemble(const CModelConfig &modelConfig,
                               const TModelFactoryCPtr &modelFactory,
                               core_t::TTime startTime) :
        m_LiveModels(modelConfig,
                     modelFactory,
                     detail::dataGatherer(modelFactory, startTime),
                     startTime),
        m_ReferenceModels()
{
}

CModelEnsemble::CModelEnsemble(bool isForPersistence,
                               const CModelEnsemble &other)
    : m_LiveModels(isForPersistence, other.m_LiveModels),
      m_ReferenceModels()
{
    if (!isForPersistence)
    {
        LOG_ABORT("This constructor only creates clones for persistence");
    }

    if (other.m_ReferenceModels)
    {
        m_ReferenceModels = CImpl(isForPersistence, *(other.m_ReferenceModels));
    }
}

bool CModelEnsemble::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    TDataGathererPtr dataGatherer;
    m_LiveModels.clearModels();
    m_ReferenceModels.reset();

    do
    {
        const std::string &name = traverser.name();
        if (name == DATA_GATHERER_TAG)
        {
            dataGatherer.reset(m_LiveModels.modelFactory()->makeDataGatherer(traverser));
            if (!dataGatherer)
            {
                LOG_ERROR("Failed to restore the data gatherer from " << traverser.value());
                return false;
            }
        }
        else if (name == REFERENCE_MODELS_TAG)
        {
            m_ReferenceModels.reset(CImpl());

            // This call will overwrite the parts of the copied reference model
            // that relate to the live model
            if (traverser.traverseSubLevel(boost::bind(&CImpl::acceptRestoreTraverser,
                                                       m_ReferenceModels.get(),
                                                       boost::cref(dataGatherer),
                                                       _1)) == false)
            {
                LOG_ERROR("Failed to restore reference models from " << traverser.value());
                return false;
            }
        }
        else if (name == LIVE_MODELS_TAG)
        {
            if (traverser.traverseSubLevel(boost::bind(&CImpl::acceptRestoreTraverser,
                                                       &m_LiveModels,
                                                       boost::cref(dataGatherer),
                                                       _1)) == false)
            {
                LOG_ERROR("Failed to restore live models from " << traverser.value());
                return false;
            }
        }
    }
    while (traverser.next());

    return true;
}

void CModelEnsemble::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    // Because the data gatherers are shared by both models their
    // persistence is managed at this level.
    inserter.insertLevel(DATA_GATHERER_TAG,
                         boost::bind(&CDataGatherer::acceptPersistInserter,
                                     &m_LiveModels.dataGatherer(),
                                     _1));

    if (m_ReferenceModels)
    {
        inserter.insertLevel(REFERENCE_MODELS_TAG,
                             boost::bind(&CImpl::acceptPersistInserter,
                                         &(*m_ReferenceModels),
                                         _1));
    }

    inserter.insertLevel(LIVE_MODELS_TAG,
                         boost::bind(&CImpl::acceptPersistInserter,
                                     &m_LiveModels,
                                     _1));
}

core_t::TTime CModelEnsemble::bucketLength(void) const
{
    return m_LiveModels.dataGatherer().bucketLength();
}

void CModelEnsemble::startComparison(const CModelConfig &modelConfig,
                                     core_t::TTime startTime)
{
    // If the live data set exceeds the length of reference data set
    // then the comparison wraps; in particular, it compares against
    // offset from start of reference data set modulo the reference
    // data set length.

    // In the extreme case where there are no batches in the comparison data,
    // set the period to 1 because 0 will cause a divide-by-zero error later
    m_LiveModels.period(std::max(m_LiveModels.numberBatches(), size_t(1)));
    m_ReferenceModels.reset(CImpl(modelConfig,
                                  m_LiveModels.modelFactory(),
                                  m_LiveModels.dataGathererPtr(),
                                  startTime));
    m_LiveModels.swap(*m_ReferenceModels);
}

function_t::EFunction CModelEnsemble::function(void) const
{
    return m_LiveModels.dataGatherer().function();
}

const CSearchKey &CModelEnsemble::searchKey(void) const
{
    return m_LiveModels.dataGatherer().searchKey();
}

CModelEnsemble::TStrVecCItr CModelEnsemble::beginInfluencers(void) const
{
    return m_LiveModels.dataGatherer().beginInfluencers();
}

CModelEnsemble::TStrVecCItr CModelEnsemble::endInfluencers(void) const
{
    return m_LiveModels.dataGatherer().endInfluencers();
}

const CModelEnsemble::TStrVec &CModelEnsemble::fieldsOfInterest(void) const
{
    return m_LiveModels.dataGatherer().fieldsOfInterest();
}

std::string CModelEnsemble::description(void) const
{
    return m_LiveModels.dataGatherer().description();
}

const std::string &CModelEnsemble::personName(std::size_t pid) const
{
    return m_LiveModels.dataGatherer().personName(pid);
}

std::size_t CModelEnsemble::numberActivePeople(void) const
{
    return m_LiveModels.dataGatherer().numberActivePeople();
}

std::size_t CModelEnsemble::numberActiveAttributes(void) const
{
    return m_LiveModels.dataGatherer().numberActiveAttributes();
}

std::size_t CModelEnsemble::maxDimension(void) const
{
    return m_LiveModels.dataGatherer().maxDimension();
}

std::size_t CModelEnsemble::numberByFieldValues(void) const
{
    return m_LiveModels.dataGatherer().numberByFieldValues();
}

std::size_t CModelEnsemble::numberOverFieldValues(void) const
{
    return m_LiveModels.dataGatherer().numberOverFieldValues();
}

void CModelEnsemble::initSimpleCounting(CResourceMonitor &resourceMonitor)
{
    bool addedPerson = false;
    m_LiveModels.dataGatherer().addPerson(SIMPLE_COUNT_DETECTOR_PERSON_NAME, resourceMonitor, addedPerson);
}

bool CModelEnsemble::processFields(const TStrCPtrVec &fieldValues,
                                   CEventData &result,
                                   CResourceMonitor &resourceMonitor)
{
    return m_LiveModels.dataGatherer().processFields(fieldValues, result, resourceMonitor);
}

bool CModelEnsemble::addArrival(const TStrCPtrVec &fieldValues,
                                CEventData &eventData,
                                CResourceMonitor &resourceMonitor)
{
    return m_LiveModels.dataGatherer().addArrival(fieldValues, eventData, resourceMonitor);
}

void CModelEnsemble::extraData(core_t::TTime time,
                               std::size_t pid,
                               boost::any &extraData)
{
    m_LiveModels.refreshBatches(time, m_ReferenceModels);
    m_LiveModels.extraData(time, pid, extraData);
}

void CModelEnsemble::startTime(core_t::TTime startTime)
{
    m_LiveModels.startTime(startTime);
}

void CModelEnsemble::period(std::size_t period)
{
    m_LiveModels.period(period);
}

std::size_t CModelEnsemble::numberBatches(void) const
{
    return m_LiveModels.numberBatches();
}

std::size_t CModelEnsemble::batch(core_t::TTime time) const
{
    return m_LiveModels.batch(time);
}

core_t::TTime CModelEnsemble::comparedTime(core_t::TTime time) const
{
    if (!m_ReferenceModels)
    {
        return time;
    }

    // If we have wrapped the reference data we need to account for this
    // by computing the reference time modulo the reference period.
    return m_ReferenceModels->moduloPeriod(
               m_ReferenceModels->referenceTime(m_LiveModels, time));
}

void CModelEnsemble::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CModelEnsemble");
    // The data gatherers are shared between all models in the ensemble
    // so only count their dynamic size once here.
    m_LiveModels.dataGatherer().debugMemoryUsage(mem->addChild());
    m_LiveModels.debugMemoryUsage(mem->addChild());
    if (m_ReferenceModels)
    {
        m_ReferenceModels->debugMemoryUsage(mem->addChild());
    }
}

std::size_t CModelEnsemble::memoryUsage(void) const
{
    // The data gatherers are shared between all models in the ensemble
    // so only count their dynamic size once here.
    std::size_t mem = m_LiveModels.dataGatherer().memoryUsage();
    mem += m_LiveModels.memoryUsage();
    if (m_ReferenceModels)
    {
        mem += m_ReferenceModels->memoryUsage();
    }
    return mem;
}

CModel *CModelEnsemble::model(core_t::TTime time)
{
    m_LiveModels.refreshBatches(time, m_ReferenceModels);
    return m_LiveModels.model(time).get();
}

const CModel *CModelEnsemble::model(core_t::TTime time) const
{
    return m_LiveModels.model(time).get();
}

void CModelEnsemble::clearModels(void)
{
    m_LiveModels.clearModels();
    if (m_ReferenceModels)
    {
        m_ReferenceModels.reset();
    }
}

void CModelEnsemble::resetBucket(core_t::TTime bucketStart)
{
    m_LiveModels.resetBucket(bucketStart);
}

void CModelEnsemble::sample(core_t::TTime startTime,
                            core_t::TTime endTime,
                            CResourceMonitor &resourceMonitor)
{
    core_t::TTime bucketLength = m_LiveModels.bucketLength();
    for (core_t::TTime time = startTime;
         time < endTime;
         time += bucketLength)
    {
        m_LiveModels.refreshBatches(time, m_ReferenceModels);
    }

    if (endTime % bucketLength == 0)
    {
        LOG_TRACE("Going to do end-of-bucket sample");
        m_LiveModels.sample(startTime, endTime, resourceMonitor);
    }
    else
    {
        LOG_TRACE("Going to do out-of-phase sampleBucketStatistics");
        // This is an offset, out-of-phase, bucket
        m_LiveModels.sampleOutOfPhase(startTime, endTime, resourceMonitor);
    }

    if ((endTime / bucketLength) % 10 == 0)
    {
        // Even if memory limiting is disabled, force a refresh every 10 buckets
        // so the user has some idea what's going on with memory.  (Note: the
        // 10 bucket interval is inexact as sampling may not take place for
        // every bucket.  However, it's probably good enough.)
        resourceMonitor.forceRefresh(*this, startTime);
    }
    else
    {
        resourceMonitor.refresh(*this, startTime);
    }
}

void CModelEnsemble::sampleBucketStatistics(core_t::TTime startTime,
                                            core_t::TTime endTime,
                                            CResourceMonitor &resourceMonitor)
{
    for (core_t::TTime time = startTime,
                       bucketLength = m_LiveModels.bucketLength();
         time < endTime;
         time += bucketLength)
    {
        m_LiveModels.refreshBatches(time, m_ReferenceModels);
    }
    m_LiveModels.sampleBucketStatistics(startTime, endTime, resourceMonitor);
    resourceMonitor.refresh(*this, startTime);
}

void CModelEnsemble::skipSampling(core_t::TTime endTime)
{
    m_LiveModels.skipSampling(endTime);
}

void CModelEnsemble::gatherAndOutputStats(core_t::TTime startTime,
                                          core_t::TTime endTime,
                                          const std::string &partitionFieldValue,
                                          const CModel::TBucketStatsOutputFunc &outputFunc,
                                          CResourceMonitor &resourceMonitor)
{
    for (core_t::TTime time = startTime,
                       bucketLength = m_LiveModels.bucketLength();
         time < endTime;
         time += bucketLength)
    {
        m_LiveModels.refreshBatches(time, m_ReferenceModels);
    }
    m_LiveModels.gatherAndOutputStats(startTime,
                                      endTime,
                                      partitionFieldValue,
                                      outputFunc,
                                      resourceMonitor);
    resourceMonitor.refresh(*this, startTime);
}

void CModelEnsemble::pruneModels(core_t::TTime time, std::size_t maximumAge)
{
    m_LiveModels.pruneModels(time, maximumAge);
}

CModelEnsemble::CImpl::CImpl(void) :
        m_DataGatherer(),
        m_ModelFactory(),
        m_Models(),
        m_StartTime(0),
        m_BucketLength(0),
        m_BatchLength(0),
        m_BatchOverlap(0),
        m_Period(CModelConfig::APERIODIC)
{
    m_Models.reserve(m_Period);
}

CModelEnsemble::CImpl::CImpl(const CModelConfig &modelConfig,
                             const TModelFactoryCPtr &modelFactory,
                             const TDataGathererPtr &dataGatherer,
                             core_t::TTime startTime) :
        m_DataGatherer(dataGatherer),
        m_ModelFactory(modelFactory),
        m_Models(),
        m_StartTime(startTime),
        m_BucketLength(modelConfig.bucketLength()),
        m_BatchLength(modelConfig.batchLength()),
        m_BatchOverlap(modelConfig.batchOverlap()),
        m_Period(modelConfig.period())
{
    m_Models.reserve(std::min(m_Period, std::size_t(168)));
}

CModelEnsemble::CImpl::CImpl(bool isForPersistence,
                             const CImpl &other) :
        m_DataGatherer(other.m_DataGatherer->cloneForPersistence()),
        m_ModelFactory(other.m_ModelFactory), // Shallow copy of model factory is OK
        m_Models(),
        m_StartTime(other.m_StartTime),
        m_BucketLength(other.m_BucketLength),
        m_BatchLength(other.m_BatchLength),
        m_BatchOverlap(other.m_BatchOverlap),
        m_Period(other.m_Period)
{
    if (!isForPersistence)
    {
        LOG_ABORT("This constructor only creates clones for persistence");
    }

    m_Models.reserve(other.m_Models.size());
    for (std::size_t i = 0u; i < other.m_Models.size(); ++i)
    {
        m_Models.push_back(TModelPtr(other.m_Models[i]->cloneForPersistence()));
    }
}

void CModelEnsemble::CImpl::swap(CImpl &other)
{
    m_DataGatherer.swap(other.m_DataGatherer);
    std::swap(m_ModelFactory, other.m_ModelFactory);
    m_Models.swap(other.m_Models);
    std::swap(m_StartTime, other.m_StartTime);
    std::swap(m_BucketLength, other.m_BucketLength);
    std::swap(m_BatchLength, other.m_BatchLength);
    std::swap(m_BatchOverlap, other.m_BatchOverlap);
    std::swap(m_Period, other.m_Period);
}

void CModelEnsemble::CImpl::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    core::CPersistUtils::persist(START_TIME_TAG, m_StartTime, inserter);
    for (std::size_t i = 0u; i < m_Models.size(); ++i)
    {
        inserter.insertLevel(MODEL_TAG,
                             boost::bind(&CModel::acceptPersistInserter,
                                         m_Models[i].get(), _1));
    }
}

bool CModelEnsemble::CImpl::acceptRestoreTraverser(const TDataGathererPtr &dataGatherer,
                                                   core::CStateRestoreTraverser &traverser)
{
    if (!dataGatherer)
    {
        LOG_ERROR("No data gatherer supplied");
        return false;
    }

    m_DataGatherer = dataGatherer;
    m_Models.clear();

    do
    {
        const std::string &name = traverser.name();
        if (name == START_TIME_TAG)
        {
            if (!core::CPersistUtils::restore(START_TIME_TAG, m_StartTime, traverser))
            {
                return false;
            }
        }
        else if (name == MODEL_TAG)
        {
            CModelFactory::SModelInitializationData initData(m_DataGatherer, TModelPtr());
            TModelPtr model(m_ModelFactory->makeModel(initData, traverser));
            if (!model)
            {
                LOG_ERROR("Failed to extract batch model from " << traverser.value());
                return false;
            }
            m_Models.push_back(model);
        }
    }
    while (traverser.next());

    return true;
}

core_t::TTime CModelEnsemble::CImpl::startTime(void) const
{
    return m_StartTime;
}

void CModelEnsemble::CImpl::startTime(core_t::TTime startTime)
{
    // Reset the start time.
    m_StartTime = startTime;
    m_DataGatherer->currentBucketStartTime(m_StartTime);
}

core_t::TTime CModelEnsemble::CImpl::bucketLength(void) const
{
    return m_BucketLength;
}

void CModelEnsemble::CImpl::period(std::size_t period)
{
    m_Period = period;
}

core_t::TTime CModelEnsemble::CImpl::moduloPeriod(core_t::TTime time) const
{
    if (m_Period == CModelConfig::APERIODIC)
    {
        return time;
    }

    core_t::TTime periodicInterval =
            static_cast<core_t::TTime>(m_Period) * m_BatchLength;

    return m_StartTime + (time - m_StartTime) % periodicInterval;
}

void CModelEnsemble::CImpl::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CModelEnsemble::CImpl");
    core::CMemoryDebug::dynamicSize("m_Models", m_Models, mem);
}

std::size_t CModelEnsemble::CImpl::memoryUsage(void) const
{
    std::size_t mem = core::CMemory::dynamicSize(m_Models);
    return mem;
}

core_t::TTime CModelEnsemble::CImpl::referenceTime(const CImpl &live,
                                                   core_t::TTime time) const
{
    return time - live.m_StartTime + m_StartTime;
}

std::size_t CModelEnsemble::CImpl::numberBatches(void) const
{
    return m_Models.size();
}

std::size_t CModelEnsemble::CImpl::batch(core_t::TTime time) const
{
    std::size_t batch = detail::batch(time, m_StartTime, m_BatchLength);
    return batch % m_Period;
}

CDataGatherer &CModelEnsemble::CImpl::dataGatherer(void) const
{
    return *m_DataGatherer;
}

CModelEnsemble::TDataGathererPtr CModelEnsemble::CImpl::dataGathererPtr(void) const
{
    return m_DataGatherer;
}

CModelEnsemble::TModelFactoryCPtr CModelEnsemble::CImpl::modelFactory(void) const
{
    return m_ModelFactory;
}

CModelEnsemble::TModelPtr CModelEnsemble::CImpl::model(core_t::TTime time) const
{
    std::size_t batch = this->batch(time);
    return batch < m_Models.size() ? m_Models[batch] : TModelPtr();
}

void CModelEnsemble::CImpl::extraData(core_t::TTime time,
                                      std::size_t pid,
                                      boost::any &extraData) const
{
    TSizeSizePr raw = this->overlappingBatches(time);
    for (std::size_t i = raw.first, j = 0u;
         i < raw.second && j < m_Period;
         ++i, ++j)
    {
        std::size_t batch = i % m_Period;
        m_Models[batch]->extraData(pid, extraData);
    }
}

void CModelEnsemble::CImpl::clearModels(void)
{
    m_DataGatherer->clear();
    TModelPtrVec empty;
    empty.swap(m_Models);
}

void CModelEnsemble::CImpl::resetBucket(core_t::TTime bucketStart)
{
    m_DataGatherer->resetBucket(bucketStart);
}

void CModelEnsemble::CImpl::sample(core_t::TTime startTime,
                                   core_t::TTime endTime,
                                   CResourceMonitor &resourceMonitor)
{
    this->sampleHelper(startTime, endTime, resourceMonitor, &CModel::sample);
}

void CModelEnsemble::CImpl::sampleBucketStatistics(core_t::TTime startTime,
                                                   core_t::TTime endTime,
                                                   CResourceMonitor &resourceMonitor)
{
    this->sampleHelper(startTime, endTime, resourceMonitor, &CModel::sampleBucketStatistics);
}

void CModelEnsemble::CImpl::sampleOutOfPhase(core_t::TTime startTime,
                                             core_t::TTime endTime,
                                             CResourceMonitor &resourceMonitor)
{
    this->sampleHelper(startTime, endTime, resourceMonitor, &CModel::sampleOutOfPhase);
}

void CModelEnsemble::CImpl::skipSampling(core_t::TTime endTime)
{
    for (std::size_t i = 0; i < m_Models.size(); ++i)
    {
        m_Models[i]->skipSampling(endTime);
    }
}

void CModelEnsemble::CImpl::gatherAndOutputStats(core_t::TTime startTime,
                                                 core_t::TTime endTime,
                                                 const std::string &partitionFieldValue,
                                                 const CModel::TBucketStatsOutputFunc &outputFunc,
                                                 CResourceMonitor &resourceMonitor)
{
    if (endTime <= startTime)
    {
        // Nothing to sample.
        return;
    }

    if (m_Models.empty())
    {
        LOG_ERROR("No models exist");
        return;
    }

    for (core_t::TTime time = startTime; time < endTime; time += m_BucketLength)
    {
        m_Models.front()->sampleBucketStatistics(time, time + m_BucketLength, resourceMonitor);
        m_Models.front()->outputCurrentBucketStatistics(partitionFieldValue, outputFunc);
    }
}

void CModelEnsemble::CImpl::pruneModels(core_t::TTime time, std::size_t maximumAge)
{
    std::size_t batch = this->batch(time);
    if (batch >= m_Models.size())
    {
        LOG_ERROR("Models don't exist for " << batch);
        return;
    }
    LOG_TRACE("batch = " << batch);
    m_Models[batch]->prune(maximumAge);
}

void CModelEnsemble::generateModelDebugDataInfo(core_t::TTime startTime,
                                                core_t::TTime endTime,
                                                double boundsPercentile,
                                                const TStrSet &terms,
                                                const std::string &partitionFieldName,
                                                const std::string &partitionFieldValue,
                                                const std::string &overFieldName,
                                                const std::string &byFieldName,
                                                TBoxPlotDataVec &boxPlots) const
{
    m_LiveModels.generateModelDebugDataInfo(startTime, endTime, boundsPercentile, terms,
                                            partitionFieldName, partitionFieldValue,
                                            overFieldName, byFieldName, boxPlots);
}

void CModelEnsemble::CImpl::generateModelDebugDataInfo(core_t::TTime startTime,
                                                       core_t::TTime endTime,
                                                       double boundsPercentile,
                                                       const TStrSet &terms,
                                                       const std::string &partitionFieldName,
                                                       const std::string &partitionFieldValue,
                                                       const std::string &overFieldName,
                                                       const std::string &byFieldName,
                                                       TBoxPlotDataVec &boxPlots) const
{
    if (endTime <= startTime)
    {
        // Nothing to sample.
        return;
    }

    for (core_t::TTime time = startTime; time < endTime; time += m_BucketLength)
    {
        TSizeSizePr raw = this->overlappingBatches(time);

        for (std::size_t i = raw.first, j = 0u;
             i < raw.second && j < m_Period;
             ++i, ++j)
        {
            std::size_t batch = i % m_Period;
            if (batch >= m_Models.size())
            {
                LOG_ERROR("Models don't exist for " << batch);
                continue;
            }

            LOG_TRACE("batch = " << batch);

            detail::TModelDetailsViewPtr view = m_Models[batch].get()->details();
            if (view.get())
            {
                boxPlots.push_back(CBoxPlotData(time,
                                                partitionFieldName,
                                                partitionFieldValue,
                                                overFieldName,
                                                byFieldName));
                view->boxPlot(time, boundsPercentile, terms, boxPlots.back());
            }
        }
    }
}

void CModelEnsemble::timeNow(core_t::TTime time)
{
    m_LiveModels.dataGatherer().timeNow(time);
}

void CModelEnsemble::CImpl::refreshBatches(core_t::TTime time,
                                           const TOptionalImpl &referenceModels)
{
    TSizeSizePr raw = this->overlappingBatches(time);
    std::size_t size = std::min(raw.second, m_Period);

    // Make sure we've got slots available for all the batches.
    for (std::size_t i = m_Models.size(); i < size; ++i)
    {
        TModelPtr referenceModel;
        if (referenceModels)
        {
            referenceModel = referenceModels->referenceModel(*this, time);
        }
        CModelFactory::SModelInitializationData initData(m_DataGatherer,
                                                         referenceModel);
        m_Models.push_back(TModelPtr(m_ModelFactory->makeModel(initData)));
    }
}

CModelEnsemble::TSizeSizePr
CModelEnsemble::CImpl::overlappingBatches(core_t::TTime time) const
{
    core_t::TTime overlap =
            static_cast<core_t::TTime>(m_BatchOverlap) * m_BucketLength;
    return TSizeSizePr(detail::batch(time - overlap,
                                     m_StartTime,
                                     m_BatchLength),
                       detail::batch(time + overlap,
                                     m_StartTime,
                                     m_BatchLength) + 1u);
}

std::size_t CModelEnsemble::CImpl::referenceBatch(const CImpl &live,
                                                  core_t::TTime time) const
{
    return this->batch(this->referenceTime(live, time));
}

CModelEnsemble::TModelPtr CModelEnsemble::CImpl::referenceModel(const CImpl &live,
                                                                core_t::TTime time) const
{
    return this->model(this->referenceTime(live, time));
}

void CModelEnsemble::CImpl::sampleHelper(core_t::TTime startTime,
                                         core_t::TTime endTime,
                                         CResourceMonitor &resourceMonitor,
                                         TModelMemFunP func)
{
    if (endTime <= startTime)
    {
        // Nothing to sample.
        return;
    }

    for (core_t::TTime time = startTime; time < endTime; time += m_BucketLength)
    {
        TSizeSizePr raw = this->overlappingBatches(time);
        LOG_TRACE("Bucket = [" << time << "," << (time + m_BucketLength) << ")"
                  << ", raw batches = [" << raw.first << "," << raw.second << ")"
                  << ", period = " << m_Period);

        for (std::size_t i = raw.first, j = 0u;
             i < raw.second && j < m_Period;
             ++i, ++j)
        {
            std::size_t batch = i % m_Period;
            if (batch >= m_Models.size())
            {
                LOG_ERROR("Models don't exist for " << batch);
                continue;
            }

            LOG_TRACE("batch = " << batch);

            (m_Models[batch].get()->*func)(time, time + m_BucketLength, resourceMonitor);
        }
    }
}

}
}
