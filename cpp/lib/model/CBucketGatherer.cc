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

#include <model/CBucketGatherer.h>

#include <core/CContainerPrinter.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>
#include <core/CStatistics.h>
#include <core/CStringUtils.h>

#include <maths/CChecksum.h>
#include <maths/CIntegerTools.h>
#include <maths/COrderings.h>

#include <model/CDataGatherer.h>
#include <model/CStringStore.h>

#include <boost/bind.hpp>
#include <boost/make_shared.hpp>

#include <algorithm>

namespace prelert
{
namespace model
{

namespace
{

const std::string BUCKET_START_TAG("b");
const std::string BUCKET_LENGTH_TAG("c");
const std::string PERSON_ATTRIBUTE_COUNT_TAG("f");
const std::string BUCKET_COUNT_TAG("k");
const std::string INFLUENCERS_COUNT_TAG("l");
const std::string BUCKET_EXPLICIT_NULLS_TAG("m");
// Nested tags
const std::string INFLUENCE_ITEM_TAG("a");
const std::string INFLUENCE_COUNT_TAG("b");
const std::string EMPTY_STRING;

namespace detail
{

typedef std::pair<std::size_t, std::size_t> TSizeSizePr;
typedef std::pair<TSizeSizePr, uint64_t> TSizeSizePrUInt64Pr;
typedef CBucketGatherer::TSizeSizePrStrPtrPrUInt64UMap TSizeSizePrStrPtrPrUInt64UMap;
typedef CBucketGatherer::TSizeSizePrStrPtrPrUInt64UMapCItr TSizeSizePrStrPtrPrUInt64UMapCItr;

const std::string PERSON_UID_TAG("a");
const std::string ATTRIBUTE_UID_TAG("b");
const std::string COUNT_TAG("c");
const std::string INFLUENCER_TAG("d");

//! \brief Orders two influencer count map iterators by
//! their person then attribute then influencer field
//! value.
struct SInfluencerCountLess
{
    bool operator()(TSizeSizePrStrPtrPrUInt64UMapCItr lhs,
                    TSizeSizePrStrPtrPrUInt64UMapCItr rhs) const
    {
        return maths::COrderings::lexicographical_compare(lhs->first.first,
                                                          *lhs->first.second,
                                                          lhs->second,
                                                          rhs->first.first,
                                                          *rhs->first.second,
                                                          rhs->second);
    }
};

//! Persist a person, attribute and count tuple.
void insertPersonAttributeCounts(const TSizeSizePrUInt64Pr &tuple,
                                 core::CStatePersistInserter &inserter)
{
    inserter.insertValue(PERSON_UID_TAG, CDataGatherer::extractPersonId(tuple));
    inserter.insertValue(ATTRIBUTE_UID_TAG, CDataGatherer::extractAttributeId(tuple));
    inserter.insertValue(COUNT_TAG, CDataGatherer::extractData(tuple));
}

//! Restore a person, attribute and count.
bool restorePersonAttributeCounts(core::CStateRestoreTraverser &traverser,
                                  TSizeSizePr &key,
                                  uint64_t &count)
{
    do
    {
        const std::string &name = traverser.name();
        if (name == PERSON_UID_TAG)
        {
            if (core::CStringUtils::stringToType(traverser.value(),
                                                 key.first) == false)
            {
                LOG_ERROR("Invalid person uid in " << traverser.value());
                return false;
            }
        }
        else if (name == ATTRIBUTE_UID_TAG)
        {
            if (core::CStringUtils::stringToType(traverser.value(),
                                                 key.second) == false)
            {
                LOG_ERROR("Invalid attribute uid in " << traverser.value());
                return false;
            }
        }
        else if (name == COUNT_TAG)
        {
            if (core::CStringUtils::stringToType(traverser.value(),
                                                 count) == false)
            {
                LOG_ERROR("Invalid count in " << traverser.value());
                return false;
            }
        }
    }
    while (traverser.next());

    return true;
}

//! Persist a collection of influencer person and attribute counts.
void insertInfluencerPersonAttributeCounts(const TSizeSizePrStrPtrPrUInt64UMap &map,
                                           core::CStatePersistInserter &inserter)
{
    typedef std::vector<TSizeSizePrStrPtrPrUInt64UMapCItr> TSizeSizePrStrPtrPrUInt64UMapCItrVec;

    TSizeSizePrStrPtrPrUInt64UMapCItrVec ordered;
    ordered.reserve(map.size());
    for (CBucketGatherer::TSizeSizePrStrPtrPrUInt64UMapCItr i = map.begin(); i != map.end(); ++i)
    {
        ordered.push_back(i);
    }
    std::sort(ordered.begin(), ordered.end(), SInfluencerCountLess());

    for (std::size_t i = 0u; i < ordered.size(); ++i)
    {
        inserter.insertValue(PERSON_UID_TAG, CDataGatherer::extractPersonId(ordered[i]->first));
        inserter.insertValue(ATTRIBUTE_UID_TAG, CDataGatherer::extractAttributeId(ordered[i]->first));
        inserter.insertValue(INFLUENCER_TAG, *CDataGatherer::extractData(ordered[i]->first));
        inserter.insertValue(COUNT_TAG, ordered[i]->second);
    }
}

//! Restore a collection of influencer person and attribute counts.
bool restoreInfluencerPersonAttributeCounts(core::CStateRestoreTraverser &traverser,
                                            TSizeSizePrStrPtrPrUInt64UMap &map)
{
    std::size_t person = 0;
    std::size_t attribute = 0;
    std::string influence = "";
    uint64_t count = 0;
    do
    {
        const std::string name = traverser.name();
        if (name == PERSON_UID_TAG)
        {
            if (core::CStringUtils::stringToType(traverser.value(),
                                                 person) == false)
            {
                LOG_ERROR("Invalid tag in " << traverser.value());
                return false;
            }
        }
        else if (name == ATTRIBUTE_UID_TAG)
        {
            if (core::CStringUtils::stringToType(traverser.value(),
                                                 attribute) == false)
            {
                LOG_ERROR("Invalid tag in " << traverser.value());
                return false;
            }
        }
        else if (name == INFLUENCER_TAG)
        {
            influence = traverser.value();
        }
        else if (name == COUNT_TAG)
        {
            if (core::CStringUtils::stringToType(traverser.value(),
                                                 count) == false)
            {
                LOG_ERROR("Invalid tag in " << traverser.value());
                return false;
            }
            map[CBucketGatherer::TSizeSizePrStrPtrPr(std::make_pair(person, attribute),
                    CStringStore::influencers().get(influence))] = count;
        }
    }
    while (traverser.next());
    return true;
}

//! \brief Manages persistence of bucket counts.
struct SBucketCountsPersister
{
    typedef CBucketGatherer::TSizeSizePrUInt64UMap TSizeSizePrUInt64UMap;

    void operator()(const TSizeSizePrUInt64UMap &bucketCounts,
                    core::CStatePersistInserter &inserter)
    {
        CBucketGatherer::TSizeSizePrUInt64PrVec personAttributeCounts;
        personAttributeCounts.reserve(bucketCounts.size());
        personAttributeCounts.assign(bucketCounts.begin(), bucketCounts.end());
        std::sort(personAttributeCounts.begin(), personAttributeCounts.end());
        for (std::size_t i = 0; i < personAttributeCounts.size(); ++i)
        {
            inserter.insertLevel(PERSON_ATTRIBUTE_COUNT_TAG,
                                     boost::bind(&detail::insertPersonAttributeCounts,
                                                 boost::cref(personAttributeCounts[i]),
                                                 _1));
        }
    }

    bool operator()(TSizeSizePrUInt64UMap &bucketCounts,
                    core::CStateRestoreTraverser &traverser)
    {
        do
        {
            TSizeSizePr key;
            uint64_t count;
            if (!traverser.hasSubLevel())
            {
                continue;
            }
            if (traverser.traverseSubLevel(boost::bind(&detail::restorePersonAttributeCounts,
                                                       _1,
                                                       boost::ref(key),
                                                       boost::ref(count))) == false)
            {
                LOG_ERROR("Invalid person attribute count");
                continue;
            }
            bucketCounts[key] = count;
        }
        while (traverser.next());
        return true;
    }
};

//! \brief Manages persistence influencer bucket counts.
struct SInfluencerCountsPersister
{
    typedef CBucketGatherer::TSizeSizePrStrPtrPrUInt64UMapVec TSizeSizePrStrPtrPrUInt64UMapVec;

    void operator()(const TSizeSizePrStrPtrPrUInt64UMapVec &data,
                    core::CStatePersistInserter &inserter)
    {
        for (std::size_t i = 0; i < data.size(); ++i)
        {
            inserter.insertValue(INFLUENCE_COUNT_TAG, i);
            inserter.insertLevel(INFLUENCE_ITEM_TAG,
                                 boost::bind(&detail::insertInfluencerPersonAttributeCounts,
                                             boost::cref(data[i]),
                                             _1));
        }
    }

    bool operator()(TSizeSizePrStrPtrPrUInt64UMapVec &data,
                    core::CStateRestoreTraverser &traverser) const
    {
        std::size_t i = 0;
        do
        {
            const std::string name = traverser.name();
            if (name == INFLUENCE_COUNT_TAG)
            {
                if (core::CStringUtils::stringToType(traverser.value(), i) == false)
                {
                    LOG_DEBUG("Bad index in " << traverser.value());
                    return false;
                }
            }
            else if (name == INFLUENCE_ITEM_TAG)
            {
                if (i >= data.size())
                {
                    data.push_back(CBucketGatherer::TSizeSizePrStrPtrPrUInt64UMap());
                }
                if (traverser.traverseSubLevel(boost::bind(&detail::restoreInfluencerPersonAttributeCounts,
                                                           _1,
                                                           boost::ref(data[i]))) == false)
                {
                    LOG_ERROR("Invalid influencer person attribute counts");
                    return false;
                }
            }
        }
        while (traverser.next());
        return true;
    }
};

} // detail::

// Hack to get Solaris to link.
#ifdef __clang__
#pragma clang diagnostic ignored "-Wunused-const-variable"
#endif
typedef void (CBucketGatherer::TSizeSizePrUSet::*TSizeSizePrUSetReserve)(std::size_t);
const TSizeSizePrUSetReserve SIZE_SIZE_PR_USET_RESERVE = &CBucketGatherer::TSizeSizePrUSet::reserve;

} // unnamed::

const std::string CBucketGatherer::EVENTRATE_BUCKET_GATHERER_TAG("a");
const std::string CBucketGatherer::METRIC_BUCKET_GATHERER_TAG("b");

CBucketGatherer::CBucketGatherer(CDataGatherer &dataGatherer,
                                 core_t::TTime startTime) :
        m_DataGatherer(dataGatherer),
        m_EarliestTime(startTime),
        m_BucketStart(startTime),
        m_PersonAttributeCounts(dataGatherer.params().s_LatencyBuckets,
                                dataGatherer.params().s_BucketLength,
                                startTime,
                                TSizeSizePrUInt64UMap(1)),
        m_PersonAttributeExplicitNulls(dataGatherer.params().s_LatencyBuckets,
                                       dataGatherer.params().s_BucketLength,
                                       startTime,
                                       TSizeSizePrUSet(1)),
        m_InfluencerCounts(dataGatherer.params().s_LatencyBuckets + 3,
                           dataGatherer.params().s_BucketLength,
                           startTime)
{
}

CBucketGatherer::CBucketGatherer(bool isForPersistence,
                                 const CBucketGatherer &other) :
        m_DataGatherer(other.m_DataGatherer),
        m_EarliestTime(other.m_EarliestTime),
        m_BucketStart(other.m_BucketStart),
        m_PersonAttributeCounts(other.m_PersonAttributeCounts),
        m_MultiBucketPersonAttributeCounts(other.m_MultiBucketPersonAttributeCounts),
        m_PersonAttributeExplicitNulls(other.m_PersonAttributeExplicitNulls),
        m_MultiBucketPersonAttributeExplicitNulls(other.m_MultiBucketPersonAttributeExplicitNulls),
        m_InfluencerCounts(other.m_InfluencerCounts),
        m_MultiBucketInfluencerCounts(other.m_MultiBucketInfluencerCounts)
{
    if (!isForPersistence)
    {
        LOG_ABORT("This constructor only creates clones for persistence");
    }
}

CBucketGatherer::~CBucketGatherer(void)
{
}

bool CBucketGatherer::addEventData(CEventData &data)
{
    core_t::TTime time = data.time();

    if (time < this->earliestBucketStartTime())
    {
        // Ignore records that are out of the latency window
        // Records in an incomplete first bucket will end up here
        LOG_TRACE("Ignored = " << time);
        return false;
    }

    this->timeNow(time);

    if (!data.personId() || !data.attributeId() || !data.count())
    {
        // The record was incomplete.
        return false;
    }

    std::size_t pid   = *data.personId();
    std::size_t cid   = *data.attributeId();
    std::size_t count = *data.count();
    if (   (pid != CDynamicStringIdRegistry::INVALID_ID)
        && (cid != CDynamicStringIdRegistry::INVALID_ID))
    {
        // Has the person/attribute been deleted from the gatherer?
        if (!m_DataGatherer.isPersonActive(pid))
        {
            LOG_DEBUG("Not adding value for deleted person " << pid);
            return false;
        }
        if (m_DataGatherer.isPopulation() && !m_DataGatherer.isAttributeActive(cid))
        {
            LOG_DEBUG("Not adding value for deleted attribute " << cid);
            return false;
        }

        TSizeSizePr pidCid = std::make_pair(pid, cid);

        // If record is explicit null just note that a null record has been seen
        // for the given (pid, cid) pair.
        if (data.isExplicitNull())
        {
            TSizeSizePrUSet &bucketExplicitNulls = m_PersonAttributeExplicitNulls.get(time);
            bucketExplicitNulls.insert(pidCid);
            return true;
        }

        TSizeSizePrUInt64UMap &bucketCounts = m_PersonAttributeCounts.get(time);
        if (count > 0)
        {
            bucketCounts[pidCid] += count;
        }

        const CEventData::TOptionalStrVec influences = data.influences();
        TSizeSizePrStrPtrPrUInt64UMapVec &influencerCounts = m_InfluencerCounts.get(time);
        influencerCounts.resize(influences.size());
        TStrPtrVec canonicalInfluences(influences.size());

        for (std::size_t i = 0u; i < influences.size(); ++i)
        {
            const CEventData::TOptionalStr &influence = influences[i];
            if (influence)
            {
                TStrPtr inf = CStringStore::influencers().get(*influence);
                canonicalInfluences[i] = inf;
                if (count > 0)
                {
                    influencerCounts[i].emplace(boost::unordered::piecewise_construct,
                                                boost::make_tuple(pidCid, inf),
                                                boost::make_tuple(uint64_t(0))).first->second += count;
                }
            }
        }

        this->addValue(pid, cid, time, data.values(), count, data.stringValue(), canonicalInfluences);
    }
    return true;
}

void CBucketGatherer::timeNow(core_t::TTime time)
{
    this->hiddenTimeNow(time, false);
}

void CBucketGatherer::hiddenTimeNow(core_t::TTime time, bool skipUpdates)
{
    m_EarliestTime = std::min(m_EarliestTime, time);
    core_t::TTime n = (time - m_BucketStart) / this->bucketLength();
    if (n <= 0)
    {
        return;
    }

    core_t::TTime newBucketStart = m_BucketStart;
    for (core_t::TTime i = 0; i < n; ++i)
    {
        newBucketStart += this->bucketLength();

        // The order here is important. While starting new buckets
        // the gatherers may finalise the earliest bucket within
        // the latency window, thus we push a new count bucket only
        // after startNewBucket has been called.
        this->startNewBucket(newBucketStart, skipUpdates);
        m_PersonAttributeCounts.push(TSizeSizePrUInt64UMap(1), newBucketStart);
        m_PersonAttributeExplicitNulls.push(TSizeSizePrUSet(1), newBucketStart);
        m_InfluencerCounts.push(TSizeSizePrStrPtrPrUInt64UMapVec(), newBucketStart);
        const TTimeVec &multipleBucketLengths = m_DataGatherer.params().s_MultipleBucketLengths;
        for (TTimeVecCItr j = multipleBucketLengths.begin(); j != multipleBucketLengths.end(); ++j)
        {
            if (newBucketStart % *j == 0)
            {
                LOG_TRACE("For bucket start time " << newBucketStart << ", multi bucket length " <<
                    *j << " can be reset too");
                m_MultiBucketInfluencerCounts.clear();
                m_MultiBucketPersonAttributeCounts.clear();
                m_MultiBucketPersonAttributeExplicitNulls.clear();
            }
        }
        m_BucketStart = newBucketStart;
    }
}

void CBucketGatherer::sampleNow(core_t::TTime sampleBucketStart)
{
    core_t::TTime timeNow =   sampleBucketStart
                           + (m_DataGatherer.params().s_LatencyBuckets + 1) * this->bucketLength() - 1;
    this->timeNow(timeNow);
    this->sample(sampleBucketStart);
}

void CBucketGatherer::skipSampleNow(core_t::TTime sampleBucketStart)
{
    core_t::TTime timeNow =   sampleBucketStart
                           + (m_DataGatherer.params().s_LatencyBuckets + 1) * this->bucketLength() - 1;
    this->hiddenTimeNow(timeNow, true);
}

void CBucketGatherer::sample(core_t::TTime time)
{
    // Merge the current bucket's samples into mutliple bucket carriers, if any
    const TTimeVec &multipleBucketLengths = m_DataGatherer.params().s_MultipleBucketLengths;
    for (TTimeVecCItr i = multipleBucketLengths.begin(); i != multipleBucketLengths.end(); ++i)
    {
        const TSizeSizePrUInt64UMap &personAttributeCounts = m_PersonAttributeCounts.get(time);
        TSizeSizePrUInt64UMap &multipleBucketPersonAttributeCounts = m_MultiBucketPersonAttributeCounts[*i];
        for (TSizeSizePrUInt64UMapCItr j = personAttributeCounts.begin(); j != personAttributeCounts.end(); ++j)
        {
            multipleBucketPersonAttributeCounts[j->first] += j->second;
        }

        const TSizeSizePrUSet &personAttributeExplicitNulls = m_PersonAttributeExplicitNulls.get(time);
        TSizeSizePrUSet &multipleBucketPersonAttributeExplicitNulls = m_MultiBucketPersonAttributeExplicitNulls[*i];
        for (TSizeSizePrUSetCItr j = personAttributeExplicitNulls.begin(); j != personAttributeExplicitNulls.end(); ++j)
        {
            multipleBucketPersonAttributeExplicitNulls.insert(*j);
        }

        const TSizeSizePrStrPtrPrUInt64UMapVec &influencerCounts = m_InfluencerCounts.get(time);
        TSizeSizePrStrPtrPrUInt64UMapVec &multiBucketInfluencerCounts = m_MultiBucketInfluencerCounts[*i];
        multiBucketInfluencerCounts.resize(influencerCounts.size());
        for (std::size_t j = 0; j < influencerCounts.size(); ++j)
        {
            for (TSizeSizePrStrPtrPrUInt64UMapCItr k = influencerCounts[j].begin(); k != influencerCounts[j].end(); ++k)
            {
                multiBucketInfluencerCounts[j][k->first] += k->second;
            }
        }
    }
}

void CBucketGatherer::personNonZeroCounts(core_t::TTime time, TSizeUInt64PrVec &result) const
{
    typedef std::map<std::size_t, uint64_t> TSizeUInt64Map;

    result.clear();

    if (!this->dataAvailable(time))
    {
        LOG_ERROR("No statistics at " << time
                  << ", current bucket = " << this->printCurrentBucket());
        return;
    }

    TSizeUInt64Map personCounts;
    const TSizeSizePrUInt64UMap &bucketCounts = this->bucketCounts(time);

    for (TSizeSizePrUInt64UMapCItr itr = bucketCounts.begin();
         itr != bucketCounts.end();
         ++itr)
    {
        personCounts[CDataGatherer::extractPersonId(*itr)] += CDataGatherer::extractData(*itr);
    }
    result.reserve(personCounts.size());
    result.assign(personCounts.begin(), personCounts.end());
}

void CBucketGatherer::recyclePeople(const TSizeVec &peopleToRemove)
{
    if (peopleToRemove.empty())
    {
        return;
    }

    remove(peopleToRemove, CDataGatherer::SExtractPersonId(), m_PersonAttributeCounts);
    remove(peopleToRemove, CDataGatherer::SExtractPersonId(), m_PersonAttributeExplicitNulls);
    remove(peopleToRemove, CDataGatherer::SExtractPersonId(), m_InfluencerCounts);
}

void CBucketGatherer::removePeople(std::size_t lowestPersonToRemove)
{
    if (lowestPersonToRemove >= m_DataGatherer.numberPeople())
    {
        return;
    }

    TSizeVec peopleToRemove;
    std::size_t maxPersonId = m_DataGatherer.numberPeople();
    peopleToRemove.reserve(maxPersonId - lowestPersonToRemove);
    for (std::size_t pid = lowestPersonToRemove; pid < maxPersonId; ++pid)
    {
        peopleToRemove.push_back(pid);
    }
    remove(peopleToRemove, CDataGatherer::SExtractPersonId(), m_PersonAttributeCounts);
    remove(peopleToRemove, CDataGatherer::SExtractPersonId(), m_PersonAttributeExplicitNulls);
    remove(peopleToRemove, CDataGatherer::SExtractPersonId(), m_InfluencerCounts);
}

void CBucketGatherer::recycleAttributes(const TSizeVec &attributesToRemove)
{
    if (attributesToRemove.empty())
    {
        return;
    }

    remove(attributesToRemove, CDataGatherer::SExtractAttributeId(), m_PersonAttributeCounts);
    remove(attributesToRemove, CDataGatherer::SExtractAttributeId(), m_PersonAttributeExplicitNulls);
    remove(attributesToRemove, CDataGatherer::SExtractAttributeId(), m_InfluencerCounts);
}

void CBucketGatherer::removeAttributes(std::size_t lowestAttributeToRemove)
{
    if (lowestAttributeToRemove >= m_DataGatherer.numberAttributes())
    {
        return;
    }

    TSizeVec attributesToRemove;
    const std::size_t numAttributes = m_DataGatherer.numberAttributes();
    attributesToRemove.reserve(numAttributes - lowestAttributeToRemove);
    for (std::size_t cid = lowestAttributeToRemove; cid < numAttributes; ++cid)
    {
        attributesToRemove.push_back(cid);
    }
    remove(attributesToRemove, CDataGatherer::SExtractAttributeId(), m_PersonAttributeCounts);
    remove(attributesToRemove, CDataGatherer::SExtractAttributeId(), m_PersonAttributeExplicitNulls);
    remove(attributesToRemove, CDataGatherer::SExtractAttributeId(), m_InfluencerCounts);
}

core_t::TTime CBucketGatherer::currentBucketStartTime(void) const
{
    return m_BucketStart;
}

void CBucketGatherer::currentBucketStartTime(core_t::TTime time)
{
    m_BucketStart = time;
}

core_t::TTime CBucketGatherer::earliestBucketStartTime(void) const
{
    return   this->currentBucketStartTime()
          - (m_DataGatherer.params().s_LatencyBuckets * this->bucketLength());
}

core_t::TTime CBucketGatherer::bucketLength(void) const
{
    return m_DataGatherer.params().s_BucketLength;
}

bool CBucketGatherer::dataAvailable(core_t::TTime time) const
{
    return time >= m_EarliestTime && time >= this->earliestBucketStartTime();
}

bool CBucketGatherer::validateSampleTimes(core_t::TTime &startTime,
                                          core_t::TTime endTime) const
{
    // Sanity checks:
    //   1) The start and end times are aligned to bucket boundaries.
    //   2) The end time is greater than the start time,
    //   3) The start time is greater than or equal to the start time
    //      of the current bucket of the counter,
    //   4) The start time is greater than or equal to the start time
    //      of the last sampled bucket

    if (!maths::CIntegerTools::aligned(startTime, this->bucketLength()))
    {
        LOG_ERROR("Sample start time " << startTime << " is not bucket aligned");
        LOG_ERROR("However, my bucketStart time is " << m_BucketStart);
        return false;
    }
    if (!maths::CIntegerTools::aligned(endTime, this->bucketLength()))
    {
        LOG_ERROR("Sample end time " << endTime << " is not bucket aligned");
        return false;
    }
    if (endTime <= startTime)
    {
        LOG_ERROR("End time " << endTime << " is not greater than the start time " << startTime);
        return false;
    }
    for (/**/; startTime < endTime; startTime += this->bucketLength())
    {
        if (!this->dataAvailable(startTime))
        {
            LOG_ERROR("No counts available at " << startTime
                      << ", current bucket = " << this->printCurrentBucket());
            continue;
        }
        return true;
    }

    return false;
}

const CDataGatherer &CBucketGatherer::dataGatherer(void) const
{
    return m_DataGatherer;
}

std::string CBucketGatherer::printCurrentBucket(void) const
{
    std::ostringstream result;
    result << "[" << m_BucketStart << "," << m_BucketStart + this->bucketLength() << ")";
    return result.str();
}

const CBucketGatherer::TSizeSizePrUInt64UMap &CBucketGatherer::bucketCounts(core_t::TTime time) const
{
    return m_PersonAttributeCounts.get(time);
}

const CBucketGatherer::TSizeSizePrStrPtrPrUInt64UMapVec &
    CBucketGatherer::influencerCounts(core_t::TTime time) const
{
    return m_InfluencerCounts.get(time);
}

bool CBucketGatherer::hasExplicitNullsOnly(core_t::TTime time, std::size_t pid, std::size_t cid) const
{
    const TSizeSizePrUSet &bucketExplicitNulls = m_PersonAttributeExplicitNulls.get(time);
    if (bucketExplicitNulls.empty())
    {
        return false;
    }
    const TSizeSizePrUInt64UMap &bucketCounts = m_PersonAttributeCounts.get(time);
    TSizeSizePr pidCid = std::make_pair(pid, cid);
    return   bucketExplicitNulls.find(pidCid) != bucketExplicitNulls.end()
          && bucketCounts.find(pidCid) == bucketCounts.end();
}

uint64_t CBucketGatherer::checksum(void) const
{
    typedef boost::reference_wrapper<const std::string> TStrCRef;
    typedef std::pair<TStrCRef, TStrCRef> TStrCRefStrCRefPr;
    typedef std::vector<TStrCRefStrCRefPr> TStrCRefStrCRefPrVec;
    typedef std::pair<TStrCRefStrCRefPr, uint64_t> TStrCRefStrCRefPrUInt64Pr;
    typedef std::vector<TStrCRefStrCRefPrUInt64Pr> TStrCRefStrCRefPrUInt64PrVec;

    uint64_t result = maths::CChecksum::calculate(0, m_BucketStart);

    result = maths::CChecksum::calculate(result, m_PersonAttributeCounts.latestBucketEnd());
    for (TSizeSizePrUInt64UMapQueueCItr i = m_PersonAttributeCounts.begin();
         i != m_PersonAttributeCounts.end();
         ++i)
    {
        const TSizeSizePrUInt64UMap &bucketCounts = *i;
        TStrCRefStrCRefPrUInt64PrVec personAttributeCounts;
        personAttributeCounts.reserve(bucketCounts.size());
        for (TSizeSizePrUInt64UMapCItr j = bucketCounts.begin(); j != bucketCounts.end(); ++j)
        {
            std::size_t pid = CDataGatherer::extractPersonId(*j);
            std::size_t cid = CDataGatherer::extractAttributeId(*j);
            TStrCRefStrCRefPr key(TStrCRef(m_DataGatherer.personName(pid)),
                                  TStrCRef(m_DataGatherer.attributeName(cid)));
            personAttributeCounts.push_back(std::make_pair(key, CDataGatherer::extractData(*j)));
        }
        std::sort(personAttributeCounts.begin(),
                  personAttributeCounts.end(),
                  maths::COrderings::SLexicographicalCompare());
        result = maths::CChecksum::calculate(result, personAttributeCounts);
    }

    result = maths::CChecksum::calculate(result, m_PersonAttributeExplicitNulls.latestBucketEnd());
    for (TSizeSizePrUSetQueueCItr i = m_PersonAttributeExplicitNulls.begin();
         i != m_PersonAttributeExplicitNulls.end();
         ++i)
    {
        const TSizeSizePrUSet &bucketExplicitNulls = *i;
        TStrCRefStrCRefPrVec personAttributeExplicitNulls;
        personAttributeExplicitNulls.reserve(bucketExplicitNulls.size());
        for (TSizeSizePrUSetCItr j = bucketExplicitNulls.begin(); j != bucketExplicitNulls.end(); ++j)
        {
            std::size_t pid = j->first;
            std::size_t cid = j->second;
            TStrCRefStrCRefPr key(TStrCRef(m_DataGatherer.personName(pid)),
                                  TStrCRef(m_DataGatherer.attributeName(cid)));
            personAttributeExplicitNulls.push_back(key);
        }
        std::sort(personAttributeExplicitNulls.begin(),
                  personAttributeExplicitNulls.end(),
                  maths::COrderings::SLexicographicalCompare());
        result = maths::CChecksum::calculate(result, personAttributeExplicitNulls);
    }

    LOG_TRACE("checksum = " << result);

    return result;
}

void CBucketGatherer::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CBucketGatherer");
    core::CMemoryDebug::dynamicSize("m_PersonAttributeCounts", m_PersonAttributeCounts, mem);
    core::CMemoryDebug::dynamicSize("m_PersonAttributeExplicitNulls", m_PersonAttributeExplicitNulls, mem);
    core::CMemoryDebug::dynamicSize("m_Influencers", m_InfluencerCounts, mem);
}

std::size_t CBucketGatherer::memoryUsage(void) const
{
    std::size_t mem = core::CMemory::dynamicSize(m_PersonAttributeCounts);
    mem += core::CMemory::dynamicSize(m_PersonAttributeExplicitNulls);
    mem += core::CMemory::dynamicSize(m_InfluencerCounts);
    return mem;
}

void CBucketGatherer::clear(void)
{
    m_PersonAttributeCounts.clear(TSizeSizePrUInt64UMap(1));
    m_PersonAttributeExplicitNulls.clear(TSizeSizePrUSet(1));
    m_InfluencerCounts.clear();
}

bool CBucketGatherer::resetBucket(core_t::TTime bucketStart)
{
    if (!maths::CIntegerTools::aligned(bucketStart, this->bucketLength()))
    {
        LOG_ERROR("Bucket start time " << bucketStart << " is not bucket aligned");
        return false;
    }

    if (  !this->dataAvailable(bucketStart)
        || bucketStart >= this->currentBucketStartTime() + this->bucketLength())
    {
        LOG_WARN("No data available at " << bucketStart
                  << ", current bucket = " << this->printCurrentBucket());
        return false;
    }

    LOG_TRACE("Resetting bucket starting at " << bucketStart);
    m_PersonAttributeCounts.get(bucketStart).clear();
    m_PersonAttributeExplicitNulls.get(bucketStart).clear();
    m_InfluencerCounts.get(bucketStart).clear();
    return true;
}

void CBucketGatherer::baseAcceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertValue(BUCKET_START_TAG, m_BucketStart);

    inserter.insertLevel(BUCKET_COUNT_TAG,
                         boost::bind<void>(TSizeSizePrUInt64UMapQueue::CSerializer<detail::SBucketCountsPersister>(),
                                           boost::cref(m_PersonAttributeCounts),
                                           _1));

    inserter.insertLevel(INFLUENCERS_COUNT_TAG,
                         boost::bind<void>(TSizeSizePrStrPtrPrUInt64UMapVecQueue::CSerializer<detail::SInfluencerCountsPersister>(),
                                           boost::cref(m_InfluencerCounts),
                                           _1));

    core::CPersistUtils::persist(BUCKET_EXPLICIT_NULLS_TAG, m_PersonAttributeExplicitNulls, inserter);
}

bool CBucketGatherer::baseAcceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    this->clear();
    do
    {
        const std::string &name = traverser.name();
        if (name == BUCKET_START_TAG)
        {
            if (core::CStringUtils::stringToType(traverser.value(), m_BucketStart) == false)
            {
                LOG_ERROR("Invalid bucket start in " << traverser.value());
                return false;
            }
        }
        else if (name == BUCKET_COUNT_TAG)
        {
            m_PersonAttributeCounts = TSizeSizePrUInt64UMapQueue(m_DataGatherer.params().s_LatencyBuckets,
                                                                 this->bucketLength(),
                                                                 m_BucketStart,
                                                                 TSizeSizePrUInt64UMap(1));
            traverser.traverseSubLevel(
                          boost::bind<bool>(TSizeSizePrUInt64UMapQueue::CSerializer<detail::SBucketCountsPersister>(TSizeSizePrUInt64UMap(1)),
                                            boost::ref(m_PersonAttributeCounts),
                                            _1));
        }
        else if (name == INFLUENCERS_COUNT_TAG)
        {
            m_InfluencerCounts = TSizeSizePrStrPtrPrUInt64UMapVecQueue(m_DataGatherer.params().s_LatencyBuckets + 3,
                                                                       this->bucketLength(),
                                                                       m_BucketStart);
            traverser.traverseSubLevel(
                          boost::bind<bool>(TSizeSizePrStrPtrPrUInt64UMapVecQueue::CSerializer<detail::SInfluencerCountsPersister>(),
                                            boost::ref(m_InfluencerCounts),
                                            _1));
        }
        else if (name == BUCKET_EXPLICIT_NULLS_TAG)
        {
            m_PersonAttributeExplicitNulls = TSizeSizePrUSetQueue(m_DataGatherer.params().s_LatencyBuckets,
                                                                  this->bucketLength(),
                                                                  m_BucketStart,
                                                                  TSizeSizePrUSet(1));
            if (core::CPersistUtils::restore(BUCKET_EXPLICIT_NULLS_TAG,
                                             m_PersonAttributeExplicitNulls,
                                             traverser) == false)
            {
                LOG_ERROR("Could not restore bucket explicit nulls queue: " << traverser.value());
                return false;
            }
        }
    }
    while (traverser.next());
    return true;
}

}
}
