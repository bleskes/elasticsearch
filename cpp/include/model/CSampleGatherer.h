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

#ifndef INCLUDED_prelert_model_CSampleGatherer_h
#define INCLUDED_prelert_model_CSampleGatherer_h

#include <core/CMemory.h>
#include <core/CoreTypes.h>

#include <core/CContainerPrinter.h>
#include <core/CLogger.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>

#include <maths/CBasicStatistics.h>
#include <maths/CChecksum.h>
#include <maths/COrderings.h>

#include <model/CBucketQueue.h>
#include <model/CMetricPartialStatistic.h>
#include <model/CMetricStatisticWrappers.h>
#include <model/CModelParams.h>
#include <model/CSampleQueue.h>
#include <model/CSeriesClassifier.h>
#include <model/CStringStore.h>
#include <model/ImportExport.h>
#include <model/ModelTypes.h>

#include <boost/bind.hpp>
#include <boost/optional.hpp>
#include <boost/make_shared.hpp>
#include <boost/ref.hpp>

#include <iterator>
#include <vector>

namespace prelert
{
namespace model
{

//! \brief Metric statistic gatherer.
//!
//! DESCRIPTION:\n
//! Wraps up the functionality to sample a statistic of a fixed
//! number of metric values, which is supplied to the add function,
//! for a latency window.
//!
//! This also computes the statistic value of all metric values
//! and for each influencing field values for every bucketing
//! interval in the latency window.
//!
//! \tparam STATISTIC This must satisfy the requirements imposed
//! by CMetricPartialStatistic.
template<typename STATISTIC, model_t::EFeature FEATURE>
class CSampleGatherer
{
    public:
        typedef core::CSmallVector<double, 1> TDouble1Vec;
        typedef std::vector<std::string> TStrVec;
        typedef TStrVec::const_iterator TStrVecCItr;
        typedef boost::reference_wrapper<const std::string> TStrCRef;
        typedef std::pair<TDouble1Vec, double> TDouble1VecDoublePr;
        typedef std::pair<TStrCRef, TDouble1VecDoublePr> TStrCRefDouble1VecDoublePrPr;
        typedef std::vector<TStrCRefDouble1VecDoublePrPr> TStrCRefDouble1VecDoublePrPrVec;
        typedef std::vector<TStrCRefDouble1VecDoublePrPrVec> TStrCRefDouble1VecDoublePrPrVecVec;
        typedef maths::CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;
        typedef CSampleQueue<STATISTIC> TSampleQueue;
        typedef typename TSampleQueue::TSampleVec TSampleVec;
        typedef CMetricPartialStatistic<STATISTIC> TMetricPartialStatistic;
        typedef CBucketQueue<TMetricPartialStatistic> TStatBucketQueue;
        typedef typename TStatBucketQueue::const_reverse_iterator TStatBucketQueueCRItr;
        typedef boost::shared_ptr<const std::string> TStrPtr;
        typedef std::vector<TStrPtr> TStrPtrVec;
        typedef boost::unordered_map<TStrPtr, STATISTIC, core::CHashing::CMurmurHash2String> TStrPtrStatUMap;
        typedef typename TStrPtrStatUMap::const_iterator TStrPtrStatUMapCItr;
        typedef CBucketQueue<TStrPtrStatUMap> TStrPtrStatUMapBucketQueue;
        typedef typename TStrPtrStatUMapBucketQueue::const_reverse_iterator TStrPtrStatUMapBucketQueueCRItr;
        typedef std::vector<TStrPtrStatUMapBucketQueue> TStrPtrStatUMapBucketQueueVec;

    public:
        static const std::string CLASSIFIER_TAG;
        static const std::string SAMPLE_STATS_TAG;
        static const std::string BUCKET_STATS_TAG;
        static const std::string INFLUENCER_BUCKET_STATS_TAG;
        static const std::string DIMENSION_TAG;

    public:
        CSampleGatherer(const SModelParams &params,
                        std::size_t dimension,
                        core_t::TTime startTime,
                        core_t::TTime bucketLength,
                        TStrVecCItr beginInfluencers,
                        TStrVecCItr endInfluencers) :
            m_Dimension(dimension),
            m_Classifier(),
            m_SampleStats(dimension,
                          params.s_SampleCountFactor,
                          params.s_LatencyBuckets,
                          params.s_SampleQueueGrowthFactor,
                          bucketLength),
            m_BucketStats(params.s_LatencyBuckets,
                          bucketLength,
                          startTime,
                          TMetricPartialStatistic(dimension)),
            m_InfluencerBucketStats(std::distance(beginInfluencers, endInfluencers),
                                    TStrPtrStatUMapBucketQueue(params.s_LatencyBuckets + 3,
                                                            bucketLength,
                                                            startTime,
                                                            TStrPtrStatUMap(1))),
            m_Samples()
        {
        }

        //! \name Persistence
        //@{
        //! Persist state by passing information to the supplied inserter.
        void acceptPersistInserter(core::CStatePersistInserter &inserter) const
        {
            inserter.insertValue(DIMENSION_TAG, m_Dimension);
            inserter.insertLevel(CLASSIFIER_TAG,
                                 boost::bind(&CSeriesClassifier::acceptPersistInserter,
                                             &m_Classifier,
                                             _1));
            if (m_SampleStats.size() > 0)
            {
                inserter.insertLevel(SAMPLE_STATS_TAG,
                                     boost::bind(&TSampleQueue::acceptPersistInserter,
                                                 &m_SampleStats,
                                                 _1));
            }
            if (m_BucketStats.size() > 0)
            {
                inserter.insertLevel(BUCKET_STATS_TAG,
                                     boost::bind<void>(TStatBucketQueueSerializer(
                                                           TMetricPartialStatistic(m_Dimension)),
                                                       boost::cref(m_BucketStats),
                                                       _1));
            }
            for (std::size_t i = 0u; i < m_InfluencerBucketStats.size(); ++i)
            {
                inserter.insertLevel(INFLUENCER_BUCKET_STATS_TAG,
                                     boost::bind<void>(TStrPtrStatUMapBucketQueueSerializer(
                                                           TStrPtrStatUMap(1),
                                                           CStrPtrStatUMapSerializer(m_Dimension)),
                                                       boost::cref(m_InfluencerBucketStats[i]),
                                                       _1));
            }
        }

        //! Create from part of a state document.
        bool acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
        {
            std::size_t i = 0u;
            do
            {
                const std::string &name = traverser.name();
                TMetricPartialStatistic stat(m_Dimension);
                if (name == DIMENSION_TAG)
                {
                    if (core::CStringUtils::stringToType(traverser.value(), m_Dimension) == false)
                    {
                        LOG_ERROR("Invalid dimension in " << traverser.value());
                        return false;
                    }
                }
                else if (name == CLASSIFIER_TAG)
                {
                    if (traverser.traverseSubLevel(boost::bind(&CSeriesClassifier::acceptRestoreTraverser,
                                                               &m_Classifier,
                                                               _1)) == false)
                    {
                        LOG_ERROR("Invalid classifier");
                        return false;
                    }
                }
                else if (name == SAMPLE_STATS_TAG)
                {
                    if (traverser.traverseSubLevel(boost::bind(&TSampleQueue::acceptRestoreTraverser,
                                                               &m_SampleStats,
                                                               _1)) == false)
                    {
                        LOG_ERROR("Invalid sample queue");
                        return false;
                    }
                }
                else if (name == BUCKET_STATS_TAG)
                {
                    if (traverser.traverseSubLevel(boost::bind<bool>(TStatBucketQueueSerializer(
                                                                         TMetricPartialStatistic(m_Dimension)),
                                                                     boost::ref(m_BucketStats),
                                                                     _1)) == false)
                    {
                        LOG_ERROR("Invalid bucket statistics");
                        return false;
                    }
                }
                else if (name == INFLUENCER_BUCKET_STATS_TAG)
                {
                    if (   i >= m_InfluencerBucketStats.size()
                        || traverser.traverseSubLevel(boost::bind<bool>(TStrPtrStatUMapBucketQueueSerializer(
                                                                            TStrPtrStatUMap(1),
                                                                            CStrPtrStatUMapSerializer(m_Dimension)),
                                                                        boost::ref(m_InfluencerBucketStats[i++]),
                                                                        _1)) == false)
                    {
                        LOG_ERROR("Invalid influencer bucket statistics");
                        return false;
                    }
                }
            }
            while (traverser.next());

            return true;
        }
        //@}

        //! Get the dimension of the underlying statistic.
        std::size_t dimension(void) const
        {
            return m_Dimension;
        }

        //! Get the feature data for the bucketing interval containing
        //! \p time.
        //!
        //! \param[in] time The start time of the sampled bucket.
        //! \param[in] effectiveSampleCount The effective historical
        //! number of measurements in a sample.
        SMetricFeatureData featureData(core_t::TTime time, core_t::TTime /*bucketLength*/,
                                       double effectiveSampleCount) const
        {
            const TMetricPartialStatistic &bucketPartial = m_BucketStats.get(time);
            double count = bucketPartial.count();
            if (count > 0.0)
            {
                core_t::TTime bucketTime = bucketPartial.sampleTime();
                TDouble1Vec bucketValue = bucketPartial.sampleValue();
                TStrCRefDouble1VecDoublePrPrVecVec influenceValues(m_InfluencerBucketStats.size());
                for (std::size_t i = 0u; i < m_InfluencerBucketStats.size(); ++i)
                {
                    const TStrPtrStatUMap &influencerStats = m_InfluencerBucketStats[i].get(time);
                    influenceValues[i].reserve(influencerStats.size());
                    for (TStrPtrStatUMapCItr j = influencerStats.begin(); j != influencerStats.end(); ++j)
                    {
                        influenceValues[i].push_back(TStrCRefDouble1VecDoublePrPr(
                                boost::cref(*j->first),
                                TDouble1VecDoublePr(CMetricStatisticWrappers::value(j->second),
                                                    CMetricStatisticWrappers::count(j->second))));
                    }
                }
                return SMetricFeatureData(bucketTime,
                                          bucketValue,
                                          model_t::varianceScale(FEATURE,
                                                                 effectiveSampleCount,
                                                                 count),
                                          count,
                                          influenceValues,
                                          m_Classifier.isInteger(),
                                          m_Samples);
            }
            return SMetricFeatureData(m_Classifier.isInteger(), m_Samples);
        }

        //! Create samples if possible for the given bucket.
        //!
        //! \param[in] time The start time of the sampled bucket.
        //! \param[in] sampleCount The measurement count in a sample.
        //! \return True if there are new samples and false otherwise.
        bool sample(core_t::TTime time, unsigned int sampleCount)
        {
            if (sampleCount > 0 && m_SampleStats.canSample(time))
            {
                TSampleVec newSamples;
                m_SampleStats.sample(time, sampleCount, FEATURE, newSamples);
                for (std::size_t i = 0; i < newSamples.size(); ++i)
                {
                    m_Samples.push_back(newSamples[i]);
                }
                return !newSamples.empty();
            }
            return false;
        }

        //! Update the state with a new measurement.
        //!
        //! \param[in] time The time of \p value.
        //! \param[in] value The measurement value.
        //! \param[in] sampleCount The measurement count in a sample.
        //! \param[in] influences The influencing field values which
        //! label \p value.
        inline void add(core_t::TTime time,
                        const TDouble1Vec &value,
                        unsigned int sampleCount,
                        const TStrPtrVec &influences)
        {
            this->add(time, value, 1, sampleCount, influences);
        }

        //! Update the state with a new mean statistic.
        //!
        //! \param[in] time The approximate time of \p statistic.
        //! \param[in] statistic The statistic value.
        //! \param[in] count The number of measurements in \p statistic.
        //! \param[in] sampleCount The measurement count in a sample.
        //! \param[in] influences The influencing field values which
        //! label \p value.
        void add(core_t::TTime time,
                 const TDouble1Vec &statistic,
                 unsigned int count,
                 unsigned int sampleCount,
                 const TStrPtrVec &influences)
        {
            if (sampleCount > 0)
            {
                m_SampleStats.add(time, statistic, count, sampleCount);
            }
            m_BucketStats.get(time).add(statistic, time, count);
            m_Classifier.add(FEATURE, statistic, count);
            for (std::size_t i = 0u, n = std::min(influences.size(),
                                                  m_InfluencerBucketStats.size());
                 i < n;
                 ++i)
            {
                if (!influences[i])
                {
                    continue;
                }
                TStrPtrStatUMap &stats = m_InfluencerBucketStats[i].get(time);
                typename TStrPtrStatUMap::iterator itr =
                        stats.emplace(influences[i],
                                      CMetricStatisticWrappers::template make<STATISTIC>(m_Dimension)).first;
                CMetricStatisticWrappers::add(statistic, count, itr->second);
            }
        }

        //! Update the state to represent the start of a new bucket.
        void startNewBucket(core_t::TTime time)
        {
            m_BucketStats.push(TMetricPartialStatistic(m_Dimension), time);
            for (std::size_t i = 0u; i < m_InfluencerBucketStats.size(); ++i)
            {
                m_InfluencerBucketStats[i].push(TStrPtrStatUMap(1), time);
            }
            m_Samples.clear();
        }

        //! Reset the bucket state for the bucket containing \p bucketStart.
        void resetBucket(core_t::TTime bucketStart)
        {
            m_BucketStats.get(bucketStart) = TMetricPartialStatistic(m_Dimension);
            for (std::size_t i = 0u; i < m_InfluencerBucketStats.size(); ++i)
            {
                m_InfluencerBucketStats[i].get(bucketStart) = TStrPtrStatUMap();
            }
            m_SampleStats.resetBucket(bucketStart);
        }

        //! Get the checksum of this gatherer.
        uint64_t checksum(void) const
        {
            uint64_t seed = static_cast<uint64_t>(m_Classifier.isInteger());
            seed = maths::CChecksum::calculate(seed, m_SampleStats);
            seed = maths::CChecksum::calculate(seed, m_BucketStats);
            return maths::CChecksum::calculate(seed, m_InfluencerBucketStats);
        }

        //! Debug the memory used by this gatherer.
        void debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
        {
            mem->setName("CSampleGatherer", sizeof(*this));
            core::CMemoryDebug::dynamicSize("m_SampleStats", m_SampleStats, mem);
            core::CMemoryDebug::dynamicSize("m_BucketStats", m_BucketStats, mem);
            core::CMemoryDebug::dynamicSize("m_InfluencerBucketStats",
                                            m_InfluencerBucketStats,
                                            mem);
            core::CMemoryDebug::dynamicSize("m_Samples", m_Samples, mem);
        }

        //! Get the memory used by this gatherer.
        std::size_t memoryUsage(void) const
        {
            return   sizeof(*this)
                   + core::CMemory::dynamicSize(m_SampleStats)
                   + core::CMemory::dynamicSize(m_BucketStats)
                   + core::CMemory::dynamicSize(m_InfluencerBucketStats)
                   + core::CMemory::dynamicSize(m_Samples);
        }

        //! Print this gatherer for debug.
        std::string print(void) const
        {
            std::ostringstream result;
            result << m_Classifier.isInteger()
                   << ' ' << m_BucketStats.print()
                   << ' ' << m_SampleStats.print()
                   << ' ' << core::CContainerPrinter::print(m_Samples)
                   << ' ' << core::CContainerPrinter::print(m_InfluencerBucketStats);
            return result.str();
        }

    private:
        static const std::string MAP_KEY_TAG;
        static const std::string MAP_VALUE_TAG;

    private:
        //! \brief Manages persistence of bucket statistics.
        struct SStatSerializer
        {
            void operator()(const TMetricPartialStatistic &stat,
                            core::CStatePersistInserter &inserter) const
            {
                stat.persist(inserter);
            }

            bool operator()(TMetricPartialStatistic &stat,
                            core::CStateRestoreTraverser &traverser) const
            {
                return stat.restore(traverser);
            }
        };
        typedef typename TStatBucketQueue::template CSerializer<SStatSerializer> TStatBucketQueueSerializer;

        //! \brief Manages persistence of influence bucket statistics.
        class CStrPtrStatUMapSerializer
        {
            public:
                CStrPtrStatUMapSerializer(std::size_t dimension) :
                        m_Initial(CMetricStatisticWrappers::template make<STATISTIC>(dimension))
                {}

                void operator()(const TStrPtrStatUMap &map,
                                core::CStatePersistInserter &inserter) const
                {
                    typedef boost::reference_wrapper<const STATISTIC> TStatCRef;
                    typedef std::pair<TStrCRef, TStatCRef> TStrCRefStatCRefPr;
                    typedef std::vector<TStrCRefStatCRefPr> TStrCRefStatCRefPrVec;
                    TStrCRefStatCRefPrVec ordered;
                    ordered.reserve(map.size());
                    for (TStrPtrStatUMapCItr i = map.begin(); i != map.end(); ++i)
                    {
                        ordered.push_back(std::make_pair(TStrCRef(*i->first),
                                                         TStatCRef(i->second)));
                    }
                    std::sort(ordered.begin(), ordered.end(), maths::COrderings::SFirstLess());
                    for (std::size_t i = 0u; i < ordered.size(); ++i)
                    {
                        inserter.insertValue(MAP_KEY_TAG, ordered[i].first);
                        CMetricStatisticWrappers::persist(ordered[i].second.get(),
                                                          MAP_VALUE_TAG,
                                                          inserter);
                    }
                }

                bool operator()(TStrPtrStatUMap &map, core::CStateRestoreTraverser &traverser) const
                {
                    std::string key;
                    do
                    {
                        const std::string &name = traverser.name();
                        if (name == MAP_KEY_TAG)
                        {
                            key = traverser.value();
                        }
                        else if (name == MAP_VALUE_TAG)
                        {
                            if (CMetricStatisticWrappers::restore(
                                    traverser,
                                    map.insert(std::make_pair(CStringStore::influencers().get(key),
                                                              m_Initial)
                                              ).first->second) == false)
                            {
                                LOG_ERROR("Invalid statistic in " << traverser.value());
                                return false;
                            }
                        }
                    }
                    while (traverser.next());
                    return true;
                }

            private:
                STATISTIC m_Initial;
        };
        typedef typename TStrPtrStatUMapBucketQueue::template CSerializer<CStrPtrStatUMapSerializer> TStrPtrStatUMapBucketQueueSerializer;

    private:
        //! The dimension of the statistic being gathered.
        std::size_t m_Dimension;

        //! Classifies the statistic series.
        CSeriesClassifier m_Classifier;

        //! The queue holding the partial aggregate statistics within
        //! latency window used for building samples.
        TSampleQueue m_SampleStats;

        //! The aggregation of the measurements received for each
        //! bucket within latency window.
        TStatBucketQueue m_BucketStats;

        //! The aggregation of the measurements received for each
        //! bucket and influencing field within latency window.
        TStrPtrStatUMapBucketQueueVec m_InfluencerBucketStats;

        //! The samples of the aggregate statistic in the current
        //! bucketing interval.
        TSampleVec m_Samples;
};

template<typename STATISTIC, model_t::EFeature FEATURE>
const std::string CSampleGatherer<STATISTIC, FEATURE>::CLASSIFIER_TAG("a");
template<typename STATISTIC, model_t::EFeature FEATURE>
const std::string CSampleGatherer<STATISTIC, FEATURE>::SAMPLE_STATS_TAG("b");
template<typename STATISTIC, model_t::EFeature FEATURE>
const std::string CSampleGatherer<STATISTIC, FEATURE>::BUCKET_STATS_TAG("c");
template<typename STATISTIC, model_t::EFeature FEATURE>
const std::string CSampleGatherer<STATISTIC, FEATURE>::INFLUENCER_BUCKET_STATS_TAG("d");
template<typename STATISTIC, model_t::EFeature FEATURE>
const std::string CSampleGatherer<STATISTIC, FEATURE>::DIMENSION_TAG("e");
template<typename STATISTIC, model_t::EFeature FEATURE>
const std::string CSampleGatherer<STATISTIC, FEATURE>::MAP_KEY_TAG("a");
template<typename STATISTIC, model_t::EFeature FEATURE>
const std::string CSampleGatherer<STATISTIC, FEATURE>::MAP_VALUE_TAG("b");


//! Overload print operator for feature data.
MODEL_EXPORT
inline std::ostream &operator<<(std::ostream &o,
                                const SMetricFeatureData &fd)
{
    return o << fd.print();
}

}
}

#endif // INCLUDED_prelert_model_CSampleGatherer_h
