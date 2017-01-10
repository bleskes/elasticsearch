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

#include <model/CEventRateBucketGatherer.h>

#include <core/CCompressUtils.h>
#include <core/CFunctional.h>
#include <core/Constants.h>
#include <core/CStatePersistInserter.h>
#include <core/CStateRestoreTraverser.h>
#include <core/CStatistics.h>

#include <maths/CBasicStatistics.h>
#include <maths/CChecksum.h>
#include <maths/COrderings.h>

#include <model/CDataGatherer.h>
#include <model/CEventData.h>
#include <model/CResourceMonitor.h>
#include <model/CSearchKey.h>
#include <model/CStringStore.h>
#include <model/FunctionTypes.h>

#include <boost/bind.hpp>
#include <boost/ref.hpp>
#include <boost/make_shared.hpp>
#include <boost/unordered_set.hpp>

#include <algorithm>
#include <limits>
#include <map>
#include <string>

namespace ml
{
namespace model
{

namespace
{

typedef std::vector<std::size_t> TSizeVec;
typedef std::vector<std::string> TStrVec;
typedef std::map<std::string, uint64_t> TStrUInt64Map;
typedef std::pair<std::size_t, std::size_t> TSizeSizePr;
typedef std::vector<TSizeSizePr> TSizeSizePrVec;
typedef std::vector<uint64_t> TUInt64Vec;
typedef boost::unordered_set<std::size_t> TSizeUSet;
typedef TSizeUSet::const_iterator TSizeUSetCItr;
typedef std::vector<TSizeUSet> TSizeUSetVec;
typedef maths::CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;
typedef boost::unordered_map<TSizeSizePr, TMeanAccumulator> TSizeSizePrMeanAccumulatorUMap;
typedef TSizeSizePrMeanAccumulatorUMap::iterator TSizeSizePrMeanAccumulatorUMapItr;
typedef TSizeSizePrMeanAccumulatorUMap::const_iterator TSizeSizePrMeanAccumulatorUMapCItr;
typedef std::map<TSizeSizePr, uint64_t> TSizeSizePrUInt64Map;
typedef CBucketQueue<TSizeSizePrMeanAccumulatorUMap> TSizeSizePrMeanAccumulatorUMapQueue;
typedef TSizeSizePrMeanAccumulatorUMapQueue::const_iterator TSizeSizePrMeanAccumulatorUMapQueueCItr;
typedef TSizeSizePrMeanAccumulatorUMapQueue::iterator TSizeSizePrMeanAccumulatorUMapQueueItr;
typedef CEventRateBucketGatherer::TCategoryAnyMap TCategoryAnyMap;
typedef CEventRateBucketGatherer::TCategoryAnyMapItr TCategoryAnyMapItr;
typedef CEventRateBucketGatherer::TCategoryAnyMapCItr TCategoryAnyMapCItr;
typedef CUniqueStringFeatureData TStrData;
typedef boost::unordered_map<TSizeSizePr, TStrData> TSizeSizePrStrDataUMap;
typedef TSizeSizePrStrDataUMap::const_iterator TSizeSizePrStrDataUMapCItr;
typedef TSizeSizePrStrDataUMap::iterator TSizeSizePrStrDataUMapItr;
typedef CBucketQueue<TSizeSizePrStrDataUMap> TSizeSizePrStrDataUMapQueue;
typedef TSizeSizePrStrDataUMapQueue::const_iterator TSizeSizePrStrDataUMapQueueCItr;
typedef TSizeSizePrStrDataUMapQueue::iterator TSizeSizePrStrDataUMapQueueItr;
typedef CBucketGatherer::TStrPtrVec TStrPtrVec;

// We obfuscate the XML element names to avoid giving away too much
// information about our model.
const std::string BASE_TAG("a");
const std::string ATTRIBUTE_PEOPLE_TAG("b");
const std::string UNIQUE_VALUES_TAG("c");
const std::string TIMES_OF_DAY_TAG("d");
const std::string EMPTY_STRING;

// Nested tags.
const std::string ATTRIBUTE_TAG("a");
const std::string PERSON_TAG("b");
const std::string STRING_ITEM_TAG("h");
const std::string MEAN_TIMES_TAG("i");

// Unique strings tags.
const std::string INFLUENCER_UNIQUE_STRINGS_TAG("a");
const std::string UNIQUE_STRINGS_TAG("b");

//! \brief Manages persistence of time-of-day feature data maps.
struct STimesBucketSerializer
{
    void operator()(const TSizeSizePrMeanAccumulatorUMap &times,
                    core::CStatePersistInserter &inserter)
    {
        std::vector<TSizeSizePrMeanAccumulatorUMapCItr> ordered;
        ordered.reserve(times.size());
        for (TSizeSizePrMeanAccumulatorUMapCItr i = times.begin(); i != times.end(); ++i)
        {
            ordered.push_back(i);
        }
        std::sort(ordered.begin(), ordered.end(),
                  core::CFunctional::SDereference<maths::COrderings::SFirstLess>());
        for (std::size_t i = 0u; i < ordered.size(); ++i)
        {
            inserter.insertValue(PERSON_TAG, CDataGatherer::extractPersonId(*ordered[i]));
            inserter.insertValue(ATTRIBUTE_TAG, CDataGatherer::extractAttributeId(*ordered[i]));
            inserter.insertValue(MEAN_TIMES_TAG, CDataGatherer::extractData(*ordered[i]).toDelimited());
        }
    }

    bool operator()(TSizeSizePrMeanAccumulatorUMap &times,
                    core::CStateRestoreTraverser &traverser) const
    {
        std::size_t pid = 0;
        std::size_t cid = 0;
        do
        {
            const std::string &name = traverser.name();
            if (name == PERSON_TAG)
            {
                if (core::CStringUtils::stringToType(traverser.value(), pid) == false)
                {
                    LOG_ERROR("Invalid person ID in " << traverser.value());
                    return false;
                }
            }
            else if (name == ATTRIBUTE_TAG)
            {
                if (core::CStringUtils::stringToType(traverser.value(), cid) == false)
                {
                    LOG_ERROR("Invalid attribute ID in " << traverser.value());
                    return false;
                }
            }
            else if (name == MEAN_TIMES_TAG)
            {
                if (times[std::make_pair(pid, cid)].fromDelimited(traverser.value()) == false)
                {
                    LOG_ERROR("Invalid mean times in " << traverser.value());
                    return false;
                }
            }
        }
        while (traverser.next());

        return true;
    }
};

//! \brief Manages persistence of unique string feature data maps.
struct SStrDataBucketSerializer
{
    void operator()(const TSizeSizePrStrDataUMap &strings,
                    core::CStatePersistInserter &inserter)
    {
        std::vector<TSizeSizePrStrDataUMapCItr> ordered;
        ordered.reserve(strings.size());
        for (TSizeSizePrStrDataUMapCItr i = strings.begin(); i != strings.end(); ++i)
        {
            ordered.push_back(i);
        }
        std::sort(ordered.begin(), ordered.end(),
                  core::CFunctional::SDereference<maths::COrderings::SFirstLess>());
        for (std::size_t i = 0u; i != ordered.size(); ++i)
        {
            inserter.insertValue(PERSON_TAG,
                                 CDataGatherer::extractPersonId(*ordered[i]));
            inserter.insertValue(ATTRIBUTE_TAG,
                                 CDataGatherer::extractAttributeId(*ordered[i]));
            inserter.insertLevel(STRING_ITEM_TAG,
                                 boost::bind(&CUniqueStringFeatureData::acceptPersistInserter,
                                             boost::cref(CDataGatherer::extractData(*ordered[i])),
                                             _1));
        }
    }
    bool operator()(TSizeSizePrStrDataUMap &map,
                    core::CStateRestoreTraverser &traverser) const
    {
        std::size_t pid = 0;
        std::size_t cid = 0;
        do
        {
            const std::string &name = traverser.name();
            if (name == PERSON_TAG)
            {
                if (core::CStringUtils::stringToType(traverser.value(), pid) == false)
                {
                    LOG_ERROR("Invalid person ID in " << traverser.value());
                    return false;
                }
            }
            else if (name == ATTRIBUTE_TAG)
            {
                if (core::CStringUtils::stringToType(traverser.value(), cid) == false)
                {
                    LOG_ERROR("Invalid attribute ID in " << traverser.value());
                    return false;
                }
            }
            else if (name == STRING_ITEM_TAG)
            {
                CUniqueStringFeatureData &data = map[std::make_pair(pid, cid)];
                if (traverser.traverseSubLevel(
                                  boost::bind(&CUniqueStringFeatureData::acceptRestoreTraverser,
                                              boost::ref(data),
                                              _1)) == false)
                {
                    LOG_ERROR("Invalid attribute/people mapping in " << traverser.value());
                    return false;
                }
            }
        }
        while (traverser.next());

        return true;
    }
};

//! Serialize \p data.
void persistAttributePeopleData(const TSizeUSetVec &data,
                                core::CStatePersistInserter &inserter)
{
    // Persist the vector in reverse order, because it means we'll
    // find out the correct size more efficiently on restore.
    std::size_t index = data.size();
    while (index > 0)
    {
        --index;
        inserter.insertValue(ATTRIBUTE_TAG, index);
        const TSizeUSet &people = data[index];

        // Persist the person identifiers in sorted order to make
        // it easier to compare state records.
        TSizeVec orderedPeople(people.begin(), people.end());
        std::sort(orderedPeople.begin(), orderedPeople.end());
        for (std::size_t i = 0u; i < orderedPeople.size(); ++i)
        {
            inserter.insertValue(PERSON_TAG, orderedPeople[i]);
        }
    }
}

//! Serialize \p featureData.
void persistFeatureData(const TCategoryAnyMap &featureData,
                        core::CStatePersistInserter &inserter)
{
    for (TCategoryAnyMapCItr itr = featureData.begin();
         itr != featureData.end();
         ++itr)
    {
        model_t::EEventRateCategory category = itr->first;
        const boost::any &data = itr->second;
        try
        {
            switch (category)
            {
            case model_t::E_DiurnalTimes:
            {
                const TSizeSizePrMeanAccumulatorUMapQueue &times =
                    boost::any_cast<const TSizeSizePrMeanAccumulatorUMapQueue&>(data);
                inserter.insertLevel(TIMES_OF_DAY_TAG,
                                     boost::bind<void>(TSizeSizePrMeanAccumulatorUMapQueue::CSerializer<STimesBucketSerializer>(),
                                                       boost::cref(times),
                                                       _1));
                break;
            }
            case model_t::E_MeanArrivalTimes:
            {
                // TODO
                break;
            }
            case model_t::E_AttributePeople:
            {
                const TSizeUSetVec &attributePeople =
                        boost::any_cast<const TSizeUSetVec&>(data);
                inserter.insertLevel(ATTRIBUTE_PEOPLE_TAG,
                                     boost::bind(&persistAttributePeopleData,
                                                 boost::cref(attributePeople),
                                                 _1));
                break;
            }
            case model_t::E_UniqueValues:
            {
                const TSizeSizePrStrDataUMapQueue &uniqueValues =
                        boost::any_cast<const TSizeSizePrStrDataUMapQueue&>(data);
                inserter.insertLevel(UNIQUE_VALUES_TAG,
                                     boost::bind<void>(TSizeSizePrStrDataUMapQueue::CSerializer<SStrDataBucketSerializer>(),
                                                       boost::cref(uniqueValues),
                                                       _1));
                }
                break;
            }
        }
        catch (const std::exception &e)
        {
            LOG_ERROR("Failed to serialize data for " << category
                      << ": " << e.what());
        }
    }
}

//! Extract \p data from a state document.
bool restoreAttributePeopleData(core::CStateRestoreTraverser &traverser,
                                TSizeUSetVec &data)
{
    size_t lastCid = 0;
    bool seenCid = false;

    do
    {
        const std::string &name = traverser.name();
        if (name == ATTRIBUTE_TAG)
        {
            if (core::CStringUtils::stringToType(traverser.value(),
                                                 lastCid) == false)
            {
                LOG_ERROR("Invalid attribute ID in " << traverser.value());
                return false;
            }
            seenCid = true;
            if (lastCid >= data.size())
            {
                data.resize(lastCid + 1);
            }
        }
        else if (name == PERSON_TAG)
        {
            if (!seenCid)
            {
                LOG_ERROR("Incorrect format - person ID before attribute ID in "
                          << traverser.value());
                return false;
            }
            std::size_t pid = 0;
            if (core::CStringUtils::stringToType(traverser.value(), pid) == false)
            {
                LOG_ERROR("Invalid person ID in " << traverser.value());
                return false;
            }
            data[lastCid].insert(pid);
        }
    }
    while (traverser.next());

    return true;
}

//! Extract \p featureData from a state document.
bool restoreFeatureData(core::CStateRestoreTraverser &traverser,
                        TCategoryAnyMap &featureData,
                        std::size_t latencyBuckets,
                        core_t::TTime bucketLength,
                        core_t::TTime currentBucketStartTime)
{
    const std::string &name = traverser.name();
    if (name == ATTRIBUTE_PEOPLE_TAG)
    {
        TSizeUSetVec *data(boost::unsafe_any_cast<TSizeUSetVec>(&featureData.insert(
                               TCategoryAnyMap::value_type(model_t::E_AttributePeople,
                                                           boost::any(TSizeUSetVec()))).first->second));

        if (traverser.traverseSubLevel(boost::bind(&restoreAttributePeopleData,
                                                   _1,
                                                   boost::ref(*data))) == false)
        {
            LOG_ERROR("Invalid attribute/people mapping in " << traverser.value());
            return false;
        }
    }
    else if (name == UNIQUE_VALUES_TAG)
    {
        if (featureData.count(model_t::E_UniqueValues) != 0)
        {
            featureData.erase(model_t::E_UniqueValues);
        }
        featureData.insert(TCategoryAnyMap::value_type(model_t::E_UniqueValues,
                                                       boost::any(TSizeSizePrStrDataUMapQueue(
                                                                  latencyBuckets,
                                                                  bucketLength,
                                                                  currentBucketStartTime,
                                                                  TSizeSizePrStrDataUMap(1)))));

        TSizeSizePrStrDataUMapQueue *data =
                boost::unsafe_any_cast<TSizeSizePrStrDataUMapQueue>(&featureData[model_t::E_UniqueValues]);

        if (traverser.traverseSubLevel(
                boost::bind<bool>(TSizeSizePrStrDataUMapQueue::CSerializer<SStrDataBucketSerializer>(TSizeSizePrStrDataUMap(1)),
                                  boost::ref(*data),
                                  _1)) == false)
        {
            LOG_ERROR("Invalid unique value mapping in " << traverser.value());
            return false;
        }
    }
    else if (name == TIMES_OF_DAY_TAG)
    {
        if (featureData.count(model_t::E_DiurnalTimes) == 0)
        {
            featureData.erase(model_t::E_DiurnalTimes);
        }
        featureData.insert(TCategoryAnyMap::value_type(model_t::E_DiurnalTimes,
                                                       boost::any(TSizeSizePrMeanAccumulatorUMapQueue(
                                                                  latencyBuckets,
                                                                  bucketLength,
                                                                  currentBucketStartTime))));

        TSizeSizePrMeanAccumulatorUMapQueue *data =
                boost::unsafe_any_cast<TSizeSizePrMeanAccumulatorUMapQueue>(&featureData[model_t::E_DiurnalTimes]);

        if (traverser.traverseSubLevel(
                boost::bind<bool>(TSizeSizePrMeanAccumulatorUMapQueue::CSerializer<STimesBucketSerializer>(),
                                  boost::ref(*data),
                                  _1)) == false)
        {
            LOG_ERROR("Invalid times mapping in " << traverser.value());
            return false;
        }
    }
    return true;
}

//! Get the by field name.
const std::string &byField(bool population, const TStrVec &fieldNames)
{
    return population ? fieldNames[1] : fieldNames[0];
}

//! Get the over field name.
const std::string &overField(bool population, const TStrVec &fieldNames)
{
    return population ? fieldNames[0] : EMPTY_STRING;
}

template<typename ITR, typename T> struct SMaybeConst {};
template<typename T> struct SMaybeConst<TCategoryAnyMapItr, T> { typedef T &TRef; };
template<typename T> struct SMaybeConst<TCategoryAnyMapCItr, T> { typedef const T &TRef; };

//! Apply a function \p f to all the data held in [\p begin, \p end).
template<typename ITR, typename F>
void apply(ITR begin, ITR end, const F &f)
{
    for (ITR itr = begin; itr != end; ++itr)
    {
        model_t::EEventRateCategory category = itr->first;
        try
        {
            switch (category)
            {
            case model_t::E_DiurnalTimes:
            {
                f(boost::any_cast<typename SMaybeConst<ITR, TSizeSizePrMeanAccumulatorUMapQueue>::TRef>(itr->second));
                break;
            }
            case model_t::E_MeanArrivalTimes:
            {
                // TODO
                break;
            }
            case model_t::E_AttributePeople:
            {
                f(boost::any_cast<typename SMaybeConst<ITR, TSizeUSetVec>::TRef>(itr->second));
                break;
            }
            case model_t::E_UniqueValues:
                f(boost::any_cast<typename SMaybeConst<ITR, TSizeSizePrStrDataUMapQueue>::TRef>(itr->second));
                break;
            }
        }
        catch (const std::exception &e)
        {
            LOG_ERROR("Apply failed for " << category << ": " << e.what());
        }
    }
}

//! Apply a function \p f to all the data held in \p featureData.
template<typename T, typename F>
void apply(T &featureData, const F &f)
{
    apply(featureData.begin(), featureData.end(), f);
}

//! \brief Removes people from the feature data.
struct SRemovePeople
{
    void operator()(TSizeUSetVec &attributePeople,
                    std::size_t lowestPersonToRemove,
                    std::size_t endPeople) const
    {
        for (std::size_t cid = 0u; cid < attributePeople.size(); ++cid)
        {
            for (std::size_t pid = lowestPersonToRemove;
                 pid < endPeople;
                 ++pid)
            {
                attributePeople[cid].erase(pid);
            }
        }
    }
    void operator()(TSizeUSetVec &attributePeople,
                    const TSizeVec &peopleToRemove) const
    {
        for (std::size_t cid = 0u; cid < attributePeople.size(); ++cid)
        {
            for (std::size_t i = 0u; i < peopleToRemove.size(); ++i)
            {
                attributePeople[cid].erase(peopleToRemove[i]);
            }
        }
    }
    void operator()(TSizeSizePrStrDataUMapQueue &peopleAttributeUniqueValues,
                    std::size_t lowestPersonToRemove,
                    std::size_t endPeople) const
    {
        for (TSizeSizePrStrDataUMapQueueItr itr = peopleAttributeUniqueValues.begin();
             itr != peopleAttributeUniqueValues.end();
             ++itr)
        {
            TSizeSizePrStrDataUMap &bucket = *itr;
            for (TSizeSizePrStrDataUMapItr i = bucket.begin(); i != bucket.end(); /* */)
            {
                if (CDataGatherer::extractPersonId(*i) >= lowestPersonToRemove &&
                    CDataGatherer::extractPersonId(*i) < endPeople)
                {
                    i = bucket.erase(i);
                }
                else
                {
                    ++i;
                }
            }
        }
    }
    void operator()(TSizeSizePrStrDataUMapQueue &peopleAttributeUniqueValues,
                    const TSizeVec &peopleToRemove) const
    {
        CBucketGatherer::remove(peopleToRemove,
                              CDataGatherer::SExtractPersonId(),
                              peopleAttributeUniqueValues);
    }
    void operator()(TSizeSizePrMeanAccumulatorUMapQueue &arrivalTimes,
                    std::size_t lowestPersonToRemove,
                    std::size_t endPeople) const
    {
        for (TSizeSizePrMeanAccumulatorUMapQueueItr itr = arrivalTimes.begin();
             itr != arrivalTimes.end();
             ++itr)
        {
            TSizeSizePrMeanAccumulatorUMap &bucket = *itr;
            for (TSizeSizePrMeanAccumulatorUMapCItr i = bucket.begin(); i != bucket.end(); /* */)
            {
                if (CDataGatherer::extractPersonId(*i) >= lowestPersonToRemove &&
                    CDataGatherer::extractPersonId(*i) < endPeople)
                {
                    i = bucket.erase(i);
                }
                else
                {
                    ++i;
                }
            }
        }
    }
    void operator()(TSizeSizePrMeanAccumulatorUMapQueue &arrivalTimes,
                     const TSizeVec &peopleToRemove) const
    {
        CBucketGatherer::remove(peopleToRemove,
                              CDataGatherer::SExtractPersonId(),
                              arrivalTimes);
    }
};

//! \brief Removes attributes from the feature data.
struct SRemoveAttributes
{
    void operator()(TSizeUSetVec &attributePeople,
                    std::size_t lowestAttributeToRemove) const
    {
        if (lowestAttributeToRemove < attributePeople.size())
        {
            attributePeople.erase(attributePeople.begin() + lowestAttributeToRemove,
                                  attributePeople.end());
        }
    }
    void operator()(TSizeUSetVec &attributePeople,
                    const TSizeVec &attributesToRemove) const
    {
        for (std::size_t i = 0u; i < attributesToRemove.size(); ++i)
        {
            attributePeople[attributesToRemove[i]].clear();
        }
    }
    void operator()(TSizeSizePrStrDataUMapQueue &peopleAttributeUniqueValues,
                    std::size_t lowestAttributeToRemove) const
    {
        for (TSizeSizePrStrDataUMapQueueItr itr = peopleAttributeUniqueValues.begin();
             itr != peopleAttributeUniqueValues.end();
             ++itr)
        {
            TSizeSizePrStrDataUMap &bucket = *itr;
            for (TSizeSizePrStrDataUMapItr i = bucket.begin(); i != bucket.end(); /* */)
            {
                if (CDataGatherer::extractAttributeId(*i) >= lowestAttributeToRemove)
                {
                    i = bucket.erase(i);
                }
                else
                {
                    ++i;
                }
            }
        }
    }
    void operator()(TSizeSizePrStrDataUMapQueue &peopleAttributeUniqueValues,
                    const TSizeVec &attributesToRemove) const
    {
        CBucketGatherer::remove(attributesToRemove,
                              CDataGatherer::SExtractAttributeId(),
                              peopleAttributeUniqueValues);
    }
    void operator()(TSizeSizePrMeanAccumulatorUMapQueue &arrivalTimes,
                    std::size_t lowestAttributeToRemove) const
    {
        for (TSizeSizePrMeanAccumulatorUMapQueueItr itr = arrivalTimes.begin();
             itr != arrivalTimes.end();
             ++itr)
        {
            TSizeSizePrMeanAccumulatorUMap &bucket = *itr;
            for (TSizeSizePrMeanAccumulatorUMapItr i = bucket.begin(); i != bucket.end(); /* */)
            {
                if (CDataGatherer::extractAttributeId(*i) >= lowestAttributeToRemove)
                {
                    i = bucket.erase(i);
                }
                else
                {
                    ++i;
                }
            }
        }
    }
    void operator()(TSizeSizePrMeanAccumulatorUMapQueue &arrivalTimes,
                     const TSizeVec &attributesToRemove) const
    {
        CBucketGatherer::remove(attributesToRemove,
                                CDataGatherer::SExtractAttributeId(),
                                arrivalTimes);
    }
};

//! \brief Computes a checksum for the feature data.
struct SChecksum
{
    void operator()(const TSizeUSetVec &attributePeople,
                    const CDataGatherer &gatherer,
                    TStrUInt64Map &hashes) const
    {
        typedef boost::reference_wrapper<const std::string> TStrCRef;
        typedef std::vector<TStrCRef> TStrCRefVec;

        for (std::size_t cid = 0u; cid < attributePeople.size(); ++cid)
        {
            if (gatherer.isAttributeActive(cid))
            {
                TStrCRefVec people;
                people.reserve(attributePeople[cid].size());
                for (TSizeUSetCItr personItr = attributePeople[cid].begin();
                     personItr != attributePeople[cid].end();
                     ++personItr)
                {
                    if (gatherer.isPersonActive(*personItr))
                    {
                        people.push_back(TStrCRef(gatherer.personName(*personItr)));
                    }
                }
                std::sort(people.begin(), people.end(),
                          maths::COrderings::SReferenceLess());
                uint64_t &hash = hashes[gatherer.attributeName(cid)];
                hash = maths::CChecksum::calculate(hash, people);
            }
        }
    }
    void operator()(const TSizeSizePrStrDataUMapQueue &peopleAttributeUniqueValues,
                    const CDataGatherer &gatherer,
                    TStrUInt64Map &hashes) const
    {
        for (TSizeSizePrStrDataUMapQueueCItr itr = peopleAttributeUniqueValues.begin();
             itr != peopleAttributeUniqueValues.end();
             ++itr)
        {
            this->checksum(*itr, gatherer, hashes);
        }
    }
    void operator()(const TSizeSizePrMeanAccumulatorUMapQueue &arrivalTimes,
                    const CDataGatherer &gatherer,
                    TStrUInt64Map &hashes) const
    {
        for (TSizeSizePrMeanAccumulatorUMapQueueCItr itr = arrivalTimes.begin();
             itr != arrivalTimes.end();
             ++itr)
        {
            this->checksum(*itr, gatherer, hashes);
        }
    }
    template<typename DATA>
    void checksum(const boost::unordered_map<TSizeSizePr, DATA> &bucket,
                  const CDataGatherer &gatherer,
                  TStrUInt64Map &hashes) const
    {
        typedef boost::unordered_map<std::size_t, TUInt64Vec> TSizeUInt64VecUMap;
        typedef TSizeUInt64VecUMap::iterator TSizeUInt64VecUMapItr;

        TSizeUInt64VecUMap attributeHashes;

        for (typename boost::unordered_map<TSizeSizePr, DATA>::const_iterator i = bucket.begin();
             i != bucket.end();
             ++i)
        {
            std::size_t pid = CDataGatherer::extractPersonId(*i);
            std::size_t cid = CDataGatherer::extractAttributeId(*i);
            if (gatherer.isPersonActive(pid) && gatherer.isAttributeActive(cid))
            {
                attributeHashes[cid].push_back(maths::CChecksum::calculate(0, i->second));
            }
        }

        for (TSizeUInt64VecUMapItr i = attributeHashes.begin();
             i != attributeHashes.end();
             ++i)
        {
            std::sort(i->second.begin(), i->second.end());
            uint64_t &hash = hashes[gatherer.attributeName(i->first)];
            hash = maths::CChecksum::calculate(hash, i->second);
        }
    }
};

//! \brief Resize the feature data to accommodate a specified
//! person and attribute identifier.
struct SResize
{
    void operator()(TSizeUSetVec &attributePeople,
                    std::size_t /*pid*/,
                    std::size_t cid) const
    {
        if (cid >= attributePeople.size())
        {
            attributePeople.resize(cid + 1);
        }
    }
    void operator()(TSizeSizePrStrDataUMapQueue &/*data*/,
                    std::size_t /*pid*/,
                    std::size_t /*cid*/) const
    {
        // Not needed
    }
    void operator()(const TSizeSizePrMeanAccumulatorUMapQueue &/*arrivalTimes*/,
                    std::size_t /*pid*/,
                    std::size_t /*cid*/) const
    {
        // Not needed
    }
};

//! \brief Updates the feature data with some aggregated records.
struct SAddValue
{
    void operator()(TSizeUSetVec &attributePeople,
                    std::size_t pid,
                    std::size_t cid,
                    core_t::TTime /*time*/,
                    std::size_t /*count*/,
                    const CEventData::TDouble1VecArray &/*values*/,
                    const CEventData::TOptionalStr &/*uniqueStrings*/,
                    const TStrPtrVec &/*influences*/) const
    {
        attributePeople[cid].insert(pid);
    }
    void operator()(TSizeSizePrStrDataUMapQueue &personAttributeUniqueCounts,
                    std::size_t pid,
                    std::size_t cid,
                    core_t::TTime time,
                    std::size_t /*count*/,
                    const CEventData::TDouble1VecArray &/*values*/,
                    const CEventData::TOptionalStr &uniqueString,
                    const TStrPtrVec &influences) const
    {
        if (!uniqueString)
        {
            return;
        }
        if (time > personAttributeUniqueCounts.latestBucketEnd())
        {
            LOG_ERROR("No queue item for time " << time);
            personAttributeUniqueCounts.push(TSizeSizePrStrDataUMap(1), time);
        }
        TSizeSizePrStrDataUMap &counts = personAttributeUniqueCounts.get(time);
        counts[std::make_pair(pid, cid)].insert(*uniqueString, influences);
    }
    void operator()(TSizeSizePrMeanAccumulatorUMapQueue &arrivalTimes,
                    std::size_t pid,
                    std::size_t cid,
                    core_t::TTime time,
                    std::size_t count,
                    const CEventData::TDouble1VecArray &values,
                    const CEventData::TOptionalStr &/*uniqueStrings*/,
                    const TStrPtrVec &/*influences*/) const
    {
        if (time > arrivalTimes.latestBucketEnd())
        {
            LOG_ERROR("No queue item for time " << time);
            arrivalTimes.push(TSizeSizePrMeanAccumulatorUMap(1), time);
        }
        TSizeSizePrMeanAccumulatorUMap &times = arrivalTimes.get(time);
        for (std::size_t i = 0; i < count; i++)
        {
            times[std::make_pair(pid, cid)].add(values[i][0]);
        }
    }
};

//! \brief Updates the feature data for the start of a new bucket.
struct SNewBucket
{
    void operator()(TSizeUSetVec &/*attributePeople*/,
                    core_t::TTime /*time*/) const
    {
    }
    void operator()(TSizeSizePrStrDataUMapQueue &personAttributeUniqueCounts,
                    core_t::TTime time) const
    {
        if (time > personAttributeUniqueCounts.latestBucketEnd())
        {
            personAttributeUniqueCounts.push(TSizeSizePrStrDataUMap(1), time);
        }
        else
        {
            personAttributeUniqueCounts.get(time).clear();
        }
    }
    void operator()(TSizeSizePrMeanAccumulatorUMapQueue &arrivalTimes,
                    core_t::TTime time) const
    {
        if (time > arrivalTimes.latestBucketEnd())
        {
            arrivalTimes.push(TSizeSizePrMeanAccumulatorUMap(1), time);
        }
        else
        {
            arrivalTimes.get(time).clear();
        }
    }
};

//! Nested tags.
const std::string DICTIONARY_WORD_TAG("a");
const std::string UNIQUE_WORD_TAG("b");

//! Persist a collection of unique strings.
void persistUniqueStrings(const CUniqueStringFeatureData::TWordStringUMap &map,
                          core::CStatePersistInserter &inserter)
{
    typedef std::vector<CUniqueStringFeatureData::TWord> TWordVec;

    if (!map.empty())
    {
        // Order the map keys to ensure consistent persistence
        TWordVec keys;
        keys.reserve(map.size());
        for (CUniqueStringFeatureData::TWordStringUMapCItr i = map.begin();
             i != map.end();
             ++i)
        {
            keys.push_back(i->first);
        }
        std::sort(keys.begin(), keys.end());

        for (std::size_t i = 0u; i != keys.size(); ++i)
        {
            inserter.insertValue(DICTIONARY_WORD_TAG, keys[i].toDelimited());
            inserter.insertValue(UNIQUE_WORD_TAG, map.at(keys[i]));
        }
    }
}

//! Restore a collection of unique strings.
bool restoreUniqueStrings(core::CStateRestoreTraverser &traverser,
                          CUniqueStringFeatureData::TWordStringUMap &map)
{
    CUniqueStringFeatureData::TWord word;
    do
    {
        const std::string &name = traverser.name();
        if (name == DICTIONARY_WORD_TAG)
        {
            if (word.fromDelimited(traverser.value()) == false)
            {
                LOG_ERROR("Failed to restore word " << traverser.value());
                return false;
            }
        }
        else if (name == UNIQUE_WORD_TAG)
        {
            map[word] = traverser.value();
        }
    }
    while (traverser.next());

    return true;
}

//! Persist influencer collections of unique strings.
void persistInfluencerUniqueStrings(const CUniqueStringFeatureData::TStrPtrWordSetUMap &map,
                                    core::CStatePersistInserter &inserter)
{
    typedef boost::shared_ptr<const std::string> TStrPtr;
    typedef std::vector<TStrPtr> TStrPtrVec;

    if (!map.empty())
    {
        // Order the map keys to ensure consistent persistence
        TStrPtrVec keys;
        keys.reserve(map.size());
        for (CUniqueStringFeatureData::TStrPtrWordSetUMapCItr i = map.begin();
             i != map.end();
             ++i)
        {
            keys.push_back(i->first);
        }
        std::sort(keys.begin(), keys.end(), maths::COrderings::SLess());

        for (std::size_t i = 0u; i < keys.size(); ++i)
        {
            inserter.insertValue(DICTIONARY_WORD_TAG, *keys[i]);
            for (CUniqueStringFeatureData::TWordSetCItr j = map.at(keys[i]).begin(),
                                                      end = map.at(keys[i]).end();
                 j != end;
                 ++j)
            {
                inserter.insertValue(UNIQUE_WORD_TAG, j->toDelimited());
            }
        }
    }
}

//! Restore influencer unique strings.
bool restoreInfluencerUniqueStrings(core::CStateRestoreTraverser &traverser,
                                    CUniqueStringFeatureData::TStrPtrWordSetUMap &data)
{
    std::string key;
    do
    {
        const std::string &name = traverser.name();
        if (name == DICTIONARY_WORD_TAG)
        {
            key = traverser.value();
        }
        else if (name == UNIQUE_WORD_TAG)
        {
            CUniqueStringFeatureData::TWord value;
            if (value.fromDelimited(traverser.value()) == false)
            {
                LOG_ERROR("Failed to restore word " << traverser.value());
                return false;
            }
            CUniqueStringFeatureData::TStrPtrWordSetUMap::iterator i = data.begin();
            for ( ; i != data.end(); ++i)
            {
                if (*i->first == key)
                {
                    i->second.insert(value);
                    break;
                }
            }
            if (i == data.end())
            {
                data[CStringStore::influencers().get(key)].insert(value);
            }
        }
    }
    while (traverser.next());

    return true;
}

} // unnamed::


CEventRateBucketGatherer::CEventRateBucketGatherer(CDataGatherer &dataGatherer,
                                                   const std::string &summaryCountFieldName,
                                                   const std::string &personFieldName,
                                                   const std::string &attributeFieldName,
                                                   const std::string &valueFieldName,
                                                   const TStrVec &influenceFieldNames,
                                                   core_t::TTime startTime) :
        CBucketGatherer(dataGatherer, startTime),
        m_BeginInfluencingFields(0),
        m_BeginValueField(0),
        m_BeginSummaryFields(0)
{
    this->initializeFieldNames(personFieldName,
                               attributeFieldName,
                               valueFieldName,
                               summaryCountFieldName,
                               influenceFieldNames);
    this->initializeFeatureData();
}

CEventRateBucketGatherer::CEventRateBucketGatherer(CDataGatherer &dataGatherer,
                                                   const std::string &summaryCountFieldName,
                                                   const std::string &personFieldName,
                                                   const std::string &attributeFieldName,
                                                   const std::string &valueFieldName,
                                                   const TStrVec &influenceFieldNames,
                                                   core::CStateRestoreTraverser &traverser) :
        CBucketGatherer(dataGatherer, 0),
        m_BeginInfluencingFields(0),
        m_BeginValueField(0),
        m_BeginSummaryFields(0)
{
    this->initializeFieldNames(personFieldName,
                               attributeFieldName,
                               valueFieldName,
                               summaryCountFieldName,
                               influenceFieldNames);
    traverser.traverseSubLevel(boost::bind(&CEventRateBucketGatherer::acceptRestoreTraverser, this, _1));
}

CEventRateBucketGatherer::CEventRateBucketGatherer(bool isForPersistence,
                                                   const CEventRateBucketGatherer &other) :
        CBucketGatherer(isForPersistence, other),
        m_FieldNames(other.m_FieldNames),
        m_BeginInfluencingFields(other.m_BeginInfluencingFields),
        m_BeginValueField(other.m_BeginValueField),
        m_BeginSummaryFields(other.m_BeginSummaryFields),
        m_FeatureData(other.m_FeatureData)
{
    if (!isForPersistence)
    {
        LOG_ABORT("This constructor only creates clones for persistence");
    }
}

bool CEventRateBucketGatherer::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    this->clear();
    do
    {
        const std::string &name = traverser.name();
        if (name == BASE_TAG)
        {
            if (traverser.traverseSubLevel(
                              boost::bind(&CBucketGatherer::baseAcceptRestoreTraverser,
                                          this,
                                          _1)) == false)
            {
                LOG_ERROR("Invalid data gatherer in " << traverser.value());
                break;
            }
        }
        else
        {
            if (restoreFeatureData(traverser, m_FeatureData, m_DataGatherer.params().s_LatencyBuckets,
                    this->bucketLength(), this->currentBucketStartTime()) == false)
            {
                LOG_ERROR("Invalid feature data in " << traverser.value());
                return false;
            }
        }
    }
    while (traverser.next());

    return true;
}

void CEventRateBucketGatherer::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertLevel(BASE_TAG,
                         boost::bind(&CBucketGatherer::baseAcceptPersistInserter,
                                     this,
                                     _1));

    persistFeatureData(m_FeatureData, inserter);
}

CBucketGatherer *CEventRateBucketGatherer::cloneForPersistence(void) const
{
    return new CEventRateBucketGatherer(true, *this);
}

const std::string &CEventRateBucketGatherer::persistenceTag(void) const
{
    return CBucketGatherer::EVENTRATE_BUCKET_GATHERER_TAG;
}

const std::string &CEventRateBucketGatherer::personFieldName(void) const
{
    return m_FieldNames[0];
}

const std::string &CEventRateBucketGatherer::attributeFieldName(void) const
{
    return m_DataGatherer.isPopulation() ? m_FieldNames[1] : EMPTY_STRING;
}

const std::string &CEventRateBucketGatherer::valueFieldName(void) const
{
    return m_BeginValueField != m_BeginSummaryFields ?
           m_FieldNames[m_BeginValueField] : EMPTY_STRING;
}

CEventRateBucketGatherer::TStrVecCItr CEventRateBucketGatherer::beginInfluencers(void) const
{
    return m_FieldNames.begin() + m_BeginInfluencingFields;
}

CEventRateBucketGatherer::TStrVecCItr CEventRateBucketGatherer::endInfluencers(void) const
{
    return m_FieldNames.begin() + m_BeginValueField;
}

const CEventRateBucketGatherer::TStrVec &CEventRateBucketGatherer::fieldsOfInterest(void) const
{
    return m_FieldNames;
}

std::string CEventRateBucketGatherer::description(void) const
{
    return function_t::name(function_t::function(m_DataGatherer.features()))
           + (m_BeginValueField == m_BeginSummaryFields ? "" : (" " + m_FieldNames[m_BeginValueField]))
           + (byField(m_DataGatherer.isPopulation(), m_FieldNames).empty() ? "" : " by ")
           + byField(m_DataGatherer.isPopulation(), m_FieldNames)
           + (overField(m_DataGatherer.isPopulation(), m_FieldNames).empty() ? "" : " over ")
           + overField(m_DataGatherer.isPopulation(), m_FieldNames)
           + (m_DataGatherer.partitionFieldName().empty() ? "" : " partition=")
           + m_DataGatherer.partitionFieldName();
}

bool CEventRateBucketGatherer::processFields(const TStrCPtrVec &fieldValues,
                                             CEventData &result,
                                             CResourceMonitor &resourceMonitor)
{
    typedef boost::optional<std::size_t> TOptionalSize;
    typedef boost::optional<std::string> TOptionalStr;

    if (fieldValues.size() != m_FieldNames.size())
    {
        LOG_ERROR("Unexpected field values: "
                  << core::CContainerPrinter::print(fieldValues) <<
                  ", for field names: "
                  << core::CContainerPrinter::print(m_FieldNames));
        return false;
    }

    const std::string *person = (fieldValues[0] == 0 && m_DataGatherer.useNull()) ?
                                &EMPTY_STRING :
                                fieldValues[0];
    if (person == 0)
    {
        // Just ignore: the "person" field wasn't present in the
        // record. Note that we don't warn here since we'll permit
        // a small fraction of records to having missing field
        // values.
        return false;
    }

    for (std::size_t i = m_DataGatherer.isPopulation() + 1; i < m_BeginValueField; ++i)
    {
        result.addInfluence(fieldValues[i] ?
                            TOptionalStr(*fieldValues[i]) :
                            TOptionalStr());
    }

    if (m_BeginValueField != m_BeginSummaryFields)
    {
        if (const std::string *value = fieldValues[m_BeginValueField])
        {
            result.stringValue(*value);
        }
    }

    std::size_t count = 1;
    if (m_DataGatherer.summaryMode() != model_t::E_None)
    {
        if (m_DataGatherer.extractCountFromField(m_FieldNames[m_BeginSummaryFields],
                                                 fieldValues[m_BeginSummaryFields],
                                                 count) == false)
        {
            result.addValue();
            return true;
        }
    }

    if (count == CDataGatherer::EXPLICIT_NULL_SUMMARY_COUNT)
    {
        result.setExplicitNull();
    }
    else
    {
        model_t::EFeature feature = m_DataGatherer.feature(0);
        if ((feature == model_t::E_IndividualTimeOfDayByBucketAndPerson) ||
            (feature == model_t::E_PopulationTimeOfDayByBucketPersonAndAttribute))
        {
            double t = static_cast<double>(result.time() % core::constants::DAY);
            result.addValue(TDouble1Vec(1, t));
        }
        else if ((feature == model_t::E_IndividualTimeOfWeekByBucketAndPerson) ||
                 (feature == model_t::E_PopulationTimeOfWeekByBucketPersonAndAttribute))
        {
            double t = static_cast<double>(result.time() % core::constants::WEEK);
            result.addValue(TDouble1Vec(1, t));
        }
        else
        {
            result.addCountStatistic(count);
        }
    }

    bool addedPerson = false;
    std::size_t personId = CDynamicStringIdRegistry::INVALID_ID;
    if (result.isExplicitNull())
    {
        m_DataGatherer.personId(*person, personId);
    }
    else
    {
        personId = m_DataGatherer.addPerson(*person, resourceMonitor, addedPerson);
    }

    if (personId == CDynamicStringIdRegistry::INVALID_ID)
    {
        if (!result.isExplicitNull())
        {
            LOG_TRACE("Couldn't create a person, over memory limit");
        }
        return false;
    }
    if (addedPerson)
    {
        (m_DataGatherer.isPopulation() ? core::CStatistics::stat(stat_t::E_NumberOverFields) :
                                         core::CStatistics::stat(stat_t::E_NumberByFields)).increment();
    }
    if (!result.person(personId))
    {
        LOG_ERROR("Bad by field value: " << *person);
        return false;
    }

    if (m_DataGatherer.isPopulation())
    {
        const std::string *attribute = (fieldValues[1] == 0 && m_DataGatherer.useNull()) ?
                                       &EMPTY_STRING :
                                       fieldValues[1];

        if (attribute == 0)
        {
            // Just ignore: the "by" field wasn't present in the
            // record. This doesn't necessarily stop us processing
            // the record by other models so we don't return false.
            // Note that we don't warn here since we'll permit a
            // small fraction of records to having missing field
            // values.
            result.addAttribute();
            return true;
        }

        bool addedAttribute = false;
        std::size_t newAttribute = CDynamicStringIdRegistry::INVALID_ID;
        if (result.isExplicitNull())
        {
            m_DataGatherer.attributeId(*attribute, newAttribute);
        }
        else
        {
            newAttribute = m_DataGatherer.addAttribute(*attribute, resourceMonitor, addedAttribute);
        }
        result.addAttribute(TOptionalSize(newAttribute));

        if (addedAttribute)
        {
            core::CStatistics::stat(stat_t::E_NumberByFields).increment();
        }
    }
    else
    {
        result.addAttribute(std::size_t(0));
    }

    return true;
}

void CEventRateBucketGatherer::recyclePeople(const TSizeVec &peopleToRemove)
{
    if (peopleToRemove.empty())
    {
        return;
    }

    apply(m_FeatureData, boost::bind<void>(SRemovePeople(), _1,
                                           boost::cref(peopleToRemove)));

    this->CBucketGatherer::recyclePeople(peopleToRemove);
}

void CEventRateBucketGatherer::removePeople(std::size_t lowestPersonToRemove)
{
    apply(m_FeatureData, boost::bind<void>(SRemovePeople(), _1,
                                           lowestPersonToRemove,
                                           m_DataGatherer.numberPeople()));
    this->CBucketGatherer::removePeople(lowestPersonToRemove);
}

void CEventRateBucketGatherer::recycleAttributes(const TSizeVec &attributesToRemove)
{
    if (attributesToRemove.empty())
    {
        return;
    }

    apply(m_FeatureData, boost::bind<void>(SRemoveAttributes(), _1,
                                           boost::cref(attributesToRemove)));

    this->CBucketGatherer::recycleAttributes(attributesToRemove);
}

void CEventRateBucketGatherer::removeAttributes(std::size_t lowestAttributeToRemove)
{
    apply(m_FeatureData, boost::bind<void>(SRemoveAttributes(), _1,
                                           lowestAttributeToRemove));
    this->CBucketGatherer::removeAttributes(lowestAttributeToRemove);
}

uint64_t CEventRateBucketGatherer::checksum(void) const
{
    uint64_t seed = this->CBucketGatherer::checksum();

    TStrUInt64Map hashes;
    apply(m_FeatureData, boost::bind<void>(SChecksum(), _1,
                                           boost::cref(m_DataGatherer),
                                           boost::ref(hashes)));
    LOG_TRACE("seed = " << seed);
    LOG_TRACE("hashes = " << core::CContainerPrinter::print(hashes));
    core::CHashing::CSafeMurmurHash2String64 hasher;
    return core::CHashing::hashCombine(
               seed,
               hasher(core::CContainerPrinter::print(hashes)));
}

void CEventRateBucketGatherer::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CPopulationEventRateDataGatherer");
    CBucketGatherer::debugMemoryUsage(mem->addChild());
    core::CMemoryDebug::dynamicSize("m_FieldNames", m_FieldNames, mem);
    core::CMemory::CAnyVisitor &visitor = core::CMemory::anyVisitor();
    visitor.registerCallback<TSizeUSetVec>();
    visitor.registerCallback<TSizeSizePrStrDataUMapQueue>();
    visitor.registerCallback<TSizeSizePrMeanAccumulatorUMapQueue>();
    core::CMemoryDebug::dynamicSize("m_FeatureData", m_FeatureData, mem);
}

std::size_t CEventRateBucketGatherer::memoryUsage(void) const
{
    std::size_t mem = CBucketGatherer::memoryUsage();
    mem += core::CMemory::dynamicSize(m_FieldNames);
    core::CMemory::CAnyVisitor &visitor = core::CMemory::anyVisitor();
    visitor.registerCallback<TSizeUSetVec>();
    visitor.registerCallback<TSizeSizePrStrDataUMapQueue>();
    visitor.registerCallback<TSizeSizePrMeanAccumulatorUMapQueue>();
    mem += core::CMemory::dynamicSize(m_FeatureData);
    return mem;
}

std::size_t CEventRateBucketGatherer::staticSize(void) const
{
    return sizeof(*this);
}

void CEventRateBucketGatherer::clear(void)
{
    this->CBucketGatherer::clear();
    m_FeatureData.clear();
    this->initializeFeatureData();
}

bool CEventRateBucketGatherer::resetBucket(core_t::TTime bucketStart)
{
    return this->CBucketGatherer::resetBucket(bucketStart);
}

void CEventRateBucketGatherer::sample(core_t::TTime time)
{
    // Merge smallest bucket into longer buckets, if they exist
    this->CBucketGatherer::sample(time);
}

void CEventRateBucketGatherer::featureData(core_t::TTime time, core_t::TTime /*bucketLength*/,
                                           TFeatureAnyPrVec &result) const
{
    result.clear();

    if (  !this->dataAvailable(time)
        || time >= this->currentBucketStartTime() + this->bucketLength())
    {
        LOG_DEBUG("No data available at " << time
                  << ", current bucket = " << this->printCurrentBucket());
        return;
    }

    for (std::size_t i = 0u, n = m_DataGatherer.numberFeatures(); i < n; ++i)
    {
        const model_t::EFeature feature = m_DataGatherer.feature(i);

        switch (feature)
        {
        case model_t::E_IndividualCountByBucketAndPerson:
            this->personCounts(feature, time, result);
            break;
        case model_t::E_IndividualNonZeroCountByBucketAndPerson:
        case model_t::E_IndividualTotalBucketCountByPerson:
            this->nonZeroPersonCounts(feature, time, result);
            break;
        case model_t::E_IndividualIndicatorOfBucketPerson:
            this->personIndicator(feature, time, result);
            break;
        case model_t::E_IndividualLowCountsByBucketAndPerson:
        case model_t::E_IndividualHighCountsByBucketAndPerson:
            this->personCounts(feature, time, result);
            break;
        case model_t::E_IndividualArrivalTimesByPerson:
        case model_t::E_IndividualLongArrivalTimesByPerson:
        case model_t::E_IndividualShortArrivalTimesByPerson:
            this->personArrivalTimes(feature, time, result);
            break;
        case model_t::E_IndividualLowNonZeroCountByBucketAndPerson:
        case model_t::E_IndividualHighNonZeroCountByBucketAndPerson:
            this->nonZeroPersonCounts(feature, time, result);
            break;
        case model_t::E_IndividualUniqueCountByBucketAndPerson:
        case model_t::E_IndividualLowUniqueCountByBucketAndPerson:
        case model_t::E_IndividualHighUniqueCountByBucketAndPerson:
            this->bucketUniqueValuesPerPerson(feature, time, result);
            break;
        case model_t::E_IndividualInfoContentByBucketAndPerson:
        case model_t::E_IndividualHighInfoContentByBucketAndPerson:
        case model_t::E_IndividualLowInfoContentByBucketAndPerson:
            this->bucketCompressedLengthPerPerson(feature, time, result);
            break;
        case model_t::E_IndividualTimeOfDayByBucketAndPerson:
        case model_t::E_IndividualTimeOfWeekByBucketAndPerson:
            this->bucketMeanTimesPerPerson(feature, time, result);
            break;

        CASE_INDIVIDUAL_METRIC:
            LOG_ERROR("Unexpected feature = " << model_t::print(feature));
            break;

        case model_t::E_PopulationAttributeTotalCountByPerson:
        case model_t::E_PopulationCountByBucketPersonAndAttribute:
            this->nonZeroAttributeCounts(feature, time, result);
            break;
        case model_t::E_PopulationIndicatorOfBucketPersonAndAttribute:
            this->attributeIndicator(feature, time, result);
            break;
        case model_t::E_PopulationUniquePersonCountByAttribute:
            this->peoplePerAttribute(feature, result);
            break;
        case model_t::E_PopulationUniqueCountByBucketPersonAndAttribute:
        case model_t::E_PopulationLowUniqueCountByBucketPersonAndAttribute:
        case model_t::E_PopulationHighUniqueCountByBucketPersonAndAttribute:
            this->bucketUniqueValuesPerPersonAttribute(feature, time, result);
            break;
        case model_t::E_PopulationLowCountsByBucketPersonAndAttribute:
        case model_t::E_PopulationHighCountsByBucketPersonAndAttribute:
            this->nonZeroAttributeCounts(feature, time, result);
            break;
        case model_t::E_PopulationInfoContentByBucketPersonAndAttribute:
        case model_t::E_PopulationLowInfoContentByBucketPersonAndAttribute:
        case model_t::E_PopulationHighInfoContentByBucketPersonAndAttribute:
            this->bucketCompressedLengthPerPersonAttribute(feature, time, result);
            break;
        case model_t::E_PopulationTimeOfDayByBucketPersonAndAttribute:
        case model_t::E_PopulationTimeOfWeekByBucketPersonAndAttribute:
            this->bucketMeanTimesPerPersonAttribute(feature, time, result);
            break;

        CASE_POPULATION_METRIC:
            LOG_ERROR("Unexpected feature = " << model_t::print(feature));
            break;

        case model_t::E_PeersAttributeTotalCountByPerson:
        case model_t::E_PeersCountByBucketPersonAndAttribute:
            this->nonZeroAttributeCounts(feature, time, result);
            break;
        case model_t::E_PeersUniqueCountByBucketPersonAndAttribute:
        case model_t::E_PeersLowUniqueCountByBucketPersonAndAttribute:
        case model_t::E_PeersHighUniqueCountByBucketPersonAndAttribute:
            this->bucketUniqueValuesPerPersonAttribute(feature, time, result);
            break;
        case model_t::E_PeersLowCountsByBucketPersonAndAttribute:
        case model_t::E_PeersHighCountsByBucketPersonAndAttribute:
            this->nonZeroAttributeCounts(feature, time, result);
            break;
        case model_t::E_PeersInfoContentByBucketPersonAndAttribute:
        case model_t::E_PeersLowInfoContentByBucketPersonAndAttribute:
        case model_t::E_PeersHighInfoContentByBucketPersonAndAttribute:
            this->bucketCompressedLengthPerPersonAttribute(feature, time, result);
            break;
        case model_t::E_PeersTimeOfDayByBucketPersonAndAttribute:
        case model_t::E_PeersTimeOfWeekByBucketPersonAndAttribute:
            this->bucketMeanTimesPerPersonAttribute(feature, time, result);
            break;

        CASE_PEERS_METRIC:
            LOG_ERROR("Unexpected feature = " << model_t::print(feature));
            break;
        }
    }
}

void CEventRateBucketGatherer::personCounts(model_t::EFeature feature,
                                            core_t::TTime time,
                                            TFeatureAnyPrVec &result_) const
{
    if (m_DataGatherer.isPopulation())
    {
        LOG_ERROR("Function does not support population analysis.");
        return;
    }

    result_.push_back(TFeatureAnyPr(feature, boost::any(TSizeFeatureDataPrVec())));
    TSizeFeatureDataPrVec &result =
            *boost::unsafe_any_cast<TSizeFeatureDataPrVec>(&result_.back().second);
    result.reserve(m_DataGatherer.numberActivePeople());

    for (std::size_t pid = 0u, n = m_DataGatherer.numberPeople(); pid < n; ++pid)
    {
        if (   !m_DataGatherer.isPersonActive(pid)
            || this->hasExplicitNullsOnly(time, pid, model_t::INDIVIDUAL_ANALYSIS_ATTRIBUTE_ID))
        {
            continue;
        }
        result.push_back(TSizeFeatureDataPr(pid, SEventRateFeatureData(0)));
    }

    const TSizeSizePrUInt64UMap &personAttributeCounts = this->bucketCounts(time);
    for (TSizeSizePrUInt64UMapCItr i = personAttributeCounts.begin();
         i != personAttributeCounts.end();
         ++i)
    {
        std::lower_bound(result.begin(),
                         result.end(),
                         CDataGatherer::extractPersonId(*i),
                         maths::COrderings::SFirstLess())->second.s_Count += CDataGatherer::extractData(*i);
    }

    this->addInfluencerCounts(time, result);
}

void CEventRateBucketGatherer::nonZeroPersonCounts(model_t::EFeature feature,
                                                   core_t::TTime time,
                                                   TFeatureAnyPrVec &result_) const
{
    result_.push_back(TFeatureAnyPr(feature, boost::any(TSizeFeatureDataPrVec())));
    TSizeFeatureDataPrVec &result =
            *boost::unsafe_any_cast<TSizeFeatureDataPrVec>(&result_.back().second);

    const TSizeSizePrUInt64UMap &personAttributeCounts = this->bucketCounts(time);
    result.reserve(personAttributeCounts.size());
    for (TSizeSizePrUInt64UMapCItr i = personAttributeCounts.begin();
         i != personAttributeCounts.end();
         ++i)
    {
        result.push_back(TSizeFeatureDataPr(CDataGatherer::extractPersonId(*i),
                         SEventRateFeatureData(CDataGatherer::extractData(*i))));
    }
    std::sort(result.begin(), result.end(), maths::COrderings::SFirstLess());

    this->addInfluencerCounts(time, result);
}

void CEventRateBucketGatherer::personIndicator(model_t::EFeature feature,
                                               core_t::TTime time,
                                               TFeatureAnyPrVec &result_) const
{
    result_.push_back(TFeatureAnyPr(feature, boost::any(TSizeFeatureDataPrVec())));
    TSizeFeatureDataPrVec &result =
            *boost::unsafe_any_cast<TSizeFeatureDataPrVec>(&result_.back().second);

    const TSizeSizePrUInt64UMap &personAttributeCounts = this->bucketCounts(time);
    result.reserve(personAttributeCounts.size());
    for (TSizeSizePrUInt64UMapCItr i = personAttributeCounts.begin();
         i != personAttributeCounts.end();
         ++i)
    {
        result.push_back(TSizeFeatureDataPr(CDataGatherer::extractPersonId(*i),
                         SEventRateFeatureData(1)));
    }
    std::sort(result.begin(), result.end(), maths::COrderings::SFirstLess());

    this->addInfluencerCounts(time, result);
}

void CEventRateBucketGatherer::personArrivalTimes(model_t::EFeature feature,
                                                  core_t::TTime /*time*/,
                                                  TFeatureAnyPrVec &result_) const
{
    // TODO
    result_.push_back(TFeatureAnyPr(feature, boost::any(TSizeFeatureDataPrVec())));
}

void CEventRateBucketGatherer::nonZeroAttributeCounts(model_t::EFeature feature,
                                                      core_t::TTime time,
                                                      TFeatureAnyPrVec &result_) const
{
    result_.push_back(TFeatureAnyPr(feature, boost::any(TSizeSizePrFeatureDataPrVec())));
    TSizeSizePrFeatureDataPrVec &result =
            *boost::unsafe_any_cast<TSizeSizePrFeatureDataPrVec>(&result_.back().second);

    const TSizeSizePrUInt64UMap &counts = this->bucketCounts(time);
    result.reserve(counts.size());
    for (TSizeSizePrUInt64UMapCItr i = counts.begin(); i != counts.end(); ++i)
    {
        if (CDataGatherer::extractData(*i) > 0)
        {
            result.push_back(TSizeSizePrFeatureDataPr(i->first, SEventRateFeatureData(i->second)));
        }
    }
    std::sort(result.begin(), result.end(), maths::COrderings::SFirstLess());

    this->addInfluencerCounts(time, result);
}

void CEventRateBucketGatherer::peoplePerAttribute(model_t::EFeature feature,
                                                  TFeatureAnyPrVec &result_) const
{
    result_.push_back(TFeatureAnyPr(feature, boost::any(TSizeSizePrFeatureDataPrVec())));
    TSizeSizePrFeatureDataPrVec &result =
            *boost::unsafe_any_cast<TSizeSizePrFeatureDataPrVec>(&result_.back().second);

    TCategoryAnyMapCItr itr = m_FeatureData.find(model_t::E_AttributePeople);
    if (itr == m_FeatureData.end())
    {
        return;
    }

    try
    {
        const TSizeUSetVec &attributePeople = boost::any_cast<const TSizeUSetVec&>(itr->second);
        result.reserve(attributePeople.size());
        for (std::size_t cid = 0u; cid < attributePeople.size(); ++cid)
        {
            if (m_DataGatherer.isAttributeActive(cid))
            {
                result.push_back(TSizeSizePrFeatureDataPr(
                                     std::make_pair(size_t(0), cid),
                                     SEventRateFeatureData(attributePeople[cid].size())));
            }
        }
    }
    catch (const std::exception &e)
    {
        LOG_ERROR("Failed to extract "
                  << model_t::print(model_t::E_PopulationUniquePersonCountByAttribute)
                  << ": " << e.what());
    }
}

void CEventRateBucketGatherer::attributeIndicator(model_t::EFeature feature,
                                                  core_t::TTime time,
                                                  TFeatureAnyPrVec &result_) const
{
    result_.push_back(TFeatureAnyPr(feature, boost::any(TSizeSizePrFeatureDataPrVec())));
    TSizeSizePrFeatureDataPrVec &result =
            *boost::unsafe_any_cast<TSizeSizePrFeatureDataPrVec>(&result_.back().second);

    const TSizeSizePrUInt64UMap &counts = this->bucketCounts(time);
    result.reserve(counts.size());
    for (TSizeSizePrUInt64UMapCItr i = counts.begin(); i != counts.end(); ++i)
    {
        if (CDataGatherer::extractData(*i) > 0)
        {
            result.push_back(TSizeSizePrFeatureDataPr(i->first, SEventRateFeatureData(1)));
        }
    }
    std::sort(result.begin(), result.end(), maths::COrderings::SFirstLess());

    this->addInfluencerCounts(time, result);
    for (std::size_t i = 0u; i < result.size(); ++i)
    {
        SEventRateFeatureData &data = result[i].second;
        for (std::size_t j = 0u; j < data.s_InfluenceValues.size(); ++j)
        {
            for (std::size_t k = 0u; k < data.s_InfluenceValues[j].size(); ++k)
            {
                data.s_InfluenceValues[j][k].second.first = TDoubleVec(1, 1.0);
            }
        }
    }
}

void CEventRateBucketGatherer::bucketUniqueValuesPerPerson(model_t::EFeature feature,
                                                           core_t::TTime time,
                                                           TFeatureAnyPrVec &result_) const
{
    result_.push_back(TFeatureAnyPr(feature, boost::any(TSizeFeatureDataPrVec())));
    TSizeFeatureDataPrVec &result =
        *boost::unsafe_any_cast<TSizeFeatureDataPrVec>(&result_.back().second);

    TCategoryAnyMapCItr i = m_FeatureData.find(model_t::E_UniqueValues);
    if (i == m_FeatureData.end())
    {
        return;
    }

    try
    {
        const TSizeSizePrStrDataUMap &personAttributeUniqueValues =
            boost::any_cast<const TSizeSizePrStrDataUMapQueue&>(i->second).get(time);
        result.reserve(personAttributeUniqueValues.size());
        for (TSizeSizePrStrDataUMapCItr j = personAttributeUniqueValues.begin();
             j != personAttributeUniqueValues.end();
             ++j)
        {
            result.push_back(TSizeFeatureDataPr(CDataGatherer::extractPersonId(*j),
                                                SEventRateFeatureData(0)));
            j->second.populateDistinctCountFeatureData(result.back().second);
        }
        std::sort(result.begin(), result.end(), maths::COrderings::SFirstLess());
    }
    catch (const std::exception &e)
    {
        LOG_ERROR("Failed to extract "
                  << model_t::print(model_t::E_IndividualUniqueCountByBucketAndPerson)
                  << ": " << e.what());
    }
}

void CEventRateBucketGatherer::bucketUniqueValuesPerPersonAttribute(model_t::EFeature feature,
                                                                    core_t::TTime time,
                                                                    TFeatureAnyPrVec &result_) const
{
    result_.push_back(TFeatureAnyPr(feature, boost::any(TSizeSizePrFeatureDataPrVec())));
    TSizeSizePrFeatureDataPrVec &result =
        *boost::unsafe_any_cast<TSizeSizePrFeatureDataPrVec>(&result_.back().second);

    TCategoryAnyMapCItr i = m_FeatureData.find(model_t::E_UniqueValues);
    if (i == m_FeatureData.end())
    {
        return;
    }

    try
    {
        const TSizeSizePrStrDataUMap &personAttributeUniqueValues =
            boost::any_cast<const TSizeSizePrStrDataUMapQueue&>(i->second).get(time);
        result.reserve(personAttributeUniqueValues.size());
        for (TSizeSizePrStrDataUMapCItr j = personAttributeUniqueValues.begin();
             j != personAttributeUniqueValues.end();
             ++j)
        {
            result.push_back(TSizeSizePrFeatureDataPr(j->first, SEventRateFeatureData(0)));
            j->second.populateDistinctCountFeatureData(result.back().second);
        }
        std::sort(result.begin(), result.end(), maths::COrderings::SFirstLess());
    }
    catch (const std::exception &e)
    {
        LOG_ERROR("Failed to extract "
                  << model_t::print(model_t::E_PopulationUniqueCountByBucketPersonAndAttribute)
                  << ": " << e.what());
    }
}

void CEventRateBucketGatherer::bucketCompressedLengthPerPerson(model_t::EFeature feature,
                                                               core_t::TTime time,
                                                               TFeatureAnyPrVec &result_) const
{
    result_.push_back(TFeatureAnyPr(feature, boost::any(TSizeFeatureDataPrVec())));
    TSizeFeatureDataPrVec &result =
        *boost::unsafe_any_cast<TSizeFeatureDataPrVec>(&result_.back().second);

    TCategoryAnyMapCItr i = m_FeatureData.find(model_t::E_UniqueValues);
    if (i == m_FeatureData.end())
    {
        return;
    }

    try
    {
        const TSizeSizePrStrDataUMap &personAttributeUniqueValues =
            boost::any_cast<const TSizeSizePrStrDataUMapQueue&>(i->second).get(time);
        result.reserve(personAttributeUniqueValues.size());

        for (TSizeSizePrStrDataUMapCItr j = personAttributeUniqueValues.begin();
             j != personAttributeUniqueValues.end();
             ++j)
        {
            result.push_back(TSizeFeatureDataPr(CDataGatherer::extractPersonId(*j),
                                                SEventRateFeatureData(0)));
            j->second.populateInfoContentFeatureData(result.back().second);
        }
        std::sort(result.begin(), result.end(), maths::COrderings::SFirstLess());
    }
    catch (const std::exception &e)
    {
        LOG_ERROR("Failed to extract "
                  << model_t::print(model_t::E_IndividualInfoContentByBucketAndPerson)
                  << ": " << e.what());
    }
}


void CEventRateBucketGatherer::bucketCompressedLengthPerPersonAttribute(model_t::EFeature feature,
                                                                        core_t::TTime time,
                                                                        TFeatureAnyPrVec &result_) const
{
    TSizeSizePrUInt64Map peopleAttributes;
    result_.push_back(TFeatureAnyPr(feature, boost::any(TSizeSizePrFeatureDataPrVec())));
    TSizeSizePrFeatureDataPrVec &result =
        *boost::unsafe_any_cast<TSizeSizePrFeatureDataPrVec>(&result_.back().second);

    TCategoryAnyMapCItr i = m_FeatureData.find(model_t::E_UniqueValues);
    if (i == m_FeatureData.end())
    {
        return;
    }

    try
    {
        const TSizeSizePrStrDataUMap &personAttributeUniqueValues =
            boost::any_cast<const TSizeSizePrStrDataUMapQueue&>(i->second).get(time);
        result.reserve(personAttributeUniqueValues.size());
        for (TSizeSizePrStrDataUMapCItr j = personAttributeUniqueValues.begin();
             j != personAttributeUniqueValues.end();
             ++j)
        {
            result.push_back(TSizeSizePrFeatureDataPr(j->first, SEventRateFeatureData(0)));
            j->second.populateInfoContentFeatureData(result.back().second);
        }
        std::sort(result.begin(), result.end(), maths::COrderings::SFirstLess());
    }
    catch (const std::exception &e)
    {
        LOG_ERROR("Failed to extract "
                  << model_t::print(model_t::E_PopulationInfoContentByBucketPersonAndAttribute)
                  << ": " << e.what());
    }
}

void CEventRateBucketGatherer::bucketMeanTimesPerPerson(model_t::EFeature feature,
                                                        core_t::TTime time,
                                                        TFeatureAnyPrVec &result_) const
{
    result_.push_back(TFeatureAnyPr(feature, boost::any(TSizeFeatureDataPrVec())));
    TSizeFeatureDataPrVec &result =
        *boost::unsafe_any_cast<TSizeFeatureDataPrVec>(&result_.back().second);

    TCategoryAnyMapCItr i = m_FeatureData.find(model_t::E_DiurnalTimes);
    if (i == m_FeatureData.end())
    {
        return;
    }

    try
    {
        const TSizeSizePrMeanAccumulatorUMap &arrivalTimes =
            boost::any_cast<const TSizeSizePrMeanAccumulatorUMapQueue&>(i->second).get(time);
        result.reserve(arrivalTimes.size());
        for (TSizeSizePrMeanAccumulatorUMapCItr j = arrivalTimes.begin(); j != arrivalTimes.end(); ++j)
        {
            result.push_back(TSizeFeatureDataPr(CDataGatherer::extractPersonId(*j),
                                                SEventRateFeatureData(static_cast<uint64_t>(
                                                        maths::CBasicStatistics::mean(CDataGatherer::extractData(*j))))));
        }
        std::sort(result.begin(), result.end(), maths::COrderings::SFirstLess());

        // We don't bother to gather the influencer bucket means
        // so the best we can do is use the person and attribute
        // bucket mean.
        this->addInfluencerCounts(time, result);
        for (std::size_t j = 0u; j < result.size(); ++j)
        {
            SEventRateFeatureData &data = result[j].second;
            for (std::size_t k = 0u; k < data.s_InfluenceValues.size(); ++k)
            {
                for (std::size_t l = 0u; l < data.s_InfluenceValues[k].size(); ++l)
                {
                    data.s_InfluenceValues[k][l].second.first =
                            TDouble1Vec(1, static_cast<double>(data.s_Count));
                }
            }
        }
    }
    catch (const std::exception &e)
    {
        LOG_ERROR("Failed to extract "
                  << model_t::print(model_t::E_DiurnalTimes)
                  << ": " << e.what());
    }
}

void CEventRateBucketGatherer::bucketMeanTimesPerPersonAttribute(model_t::EFeature feature,
                                                                 core_t::TTime time,
                                                                 TFeatureAnyPrVec &result_) const
{
    TSizeSizePrUInt64Map peopleAttributes;
    result_.push_back(TFeatureAnyPr(feature, boost::any(TSizeSizePrFeatureDataPrVec())));
    TSizeSizePrFeatureDataPrVec &result =
        *boost::unsafe_any_cast<TSizeSizePrFeatureDataPrVec>(&result_.back().second);

    TCategoryAnyMapCItr i = m_FeatureData.find(model_t::E_DiurnalTimes);
    if (i == m_FeatureData.end())
    {
        return;
    }

    try
    {
        const TSizeSizePrMeanAccumulatorUMap &arrivalTimes =
            boost::any_cast<const TSizeSizePrMeanAccumulatorUMapQueue&>(i->second).get(time);
        result.reserve(arrivalTimes.size());
        for (TSizeSizePrMeanAccumulatorUMapCItr j = arrivalTimes.begin(); j != arrivalTimes.end(); ++j)
        {
            result.push_back(TSizeSizePrFeatureDataPr(
                                 j->first,
                                 SEventRateFeatureData(static_cast<uint64_t>(
                                                  maths::CBasicStatistics::mean(CDataGatherer::extractData(*j))))));
        }
        std::sort(result.begin(), result.end(), maths::COrderings::SFirstLess());

        // We don't bother to gather the influencer bucket means
        // so the best we can do is use the person and attribute
        // bucket mean.
        this->addInfluencerCounts(time, result);
        for (std::size_t j = 0u; j < result.size(); ++j)
        {
            SEventRateFeatureData &data = result[j].second;
            for (std::size_t k = 0u; k < data.s_InfluenceValues.size(); ++k)
            {
                for (std::size_t l = 0u; l < data.s_InfluenceValues[k].size(); ++l)
                {
                    data.s_InfluenceValues[k][l].second.first =
                            TDouble1Vec(1, static_cast<double>(data.s_Count));
                }
            }
        }
    }
    catch (const std::exception &e)
    {
        LOG_ERROR("Failed to extract "
                  << model_t::print(model_t::E_DiurnalTimes)
                  << ": " << e.what());
    }
}

void CEventRateBucketGatherer::resize(std::size_t pid, std::size_t cid)
{
    apply(m_FeatureData, boost::bind<void>(SResize(), _1, pid, cid));
}

void CEventRateBucketGatherer::addValue(std::size_t pid,
                                        std::size_t cid,
                                        core_t::TTime time,
                                        const CEventData::TDouble1VecArray &values,
                                        std::size_t count,
                                        const CEventData::TOptionalStr &stringValue,
                                        const TStrPtrVec &influences)
{
    // Check that we are correctly sized - a person/attribute might have been added
    this->resize(pid, cid);
    apply(m_FeatureData, boost::bind<void>(SAddValue(), _1, pid, cid,
                                           time,
                                           count,
                                           boost::cref(values),
                                           boost::cref(stringValue),
                                           boost::cref(influences)));
}

void CEventRateBucketGatherer::startNewBucket(core_t::TTime time, bool /*skipUpdates*/)
{
    apply(m_FeatureData, boost::bind<void>(SNewBucket(), _1, time));
}

void CEventRateBucketGatherer::initializeFieldNames(const std::string &personFieldName,
                                                    const std::string &attributeFieldName,
                                                    const std::string &valueFieldName,
                                                    const std::string &summaryCountFieldName,
                                                    const TStrVec &influenceFieldNames)
{
    m_FieldNames.push_back(personFieldName);
    if (m_DataGatherer.isPopulation())
    {
        m_FieldNames.push_back(attributeFieldName);
    }

    m_BeginInfluencingFields = m_FieldNames.size();
    m_FieldNames.insert(m_FieldNames.end(),
                        influenceFieldNames.begin(),
                        influenceFieldNames.end());

    m_BeginValueField = m_FieldNames.size();
    if (!valueFieldName.empty())
    {
        m_FieldNames.push_back(valueFieldName);
    }

    m_BeginSummaryFields = m_FieldNames.size();
    switch (m_DataGatherer.summaryMode())
    {
    case model_t::E_None:
        break;
    case model_t::E_Manual:
        m_FieldNames.push_back(summaryCountFieldName);
        break;
    };

    // swap trick to reduce unused capacity
    TStrVec(m_FieldNames).swap(m_FieldNames);
}

void CEventRateBucketGatherer::initializeFeatureData(void)
{
    for (std::size_t i = 0u, n = m_DataGatherer.numberFeatures(); i < n; ++i)
    {
        switch (m_DataGatherer.feature(i))
        {
        case model_t::E_IndividualCountByBucketAndPerson:
        case model_t::E_IndividualNonZeroCountByBucketAndPerson:
        case model_t::E_IndividualTotalBucketCountByPerson:
        case model_t::E_IndividualIndicatorOfBucketPerson:
        case model_t::E_IndividualLowCountsByBucketAndPerson:
        case model_t::E_IndividualHighCountsByBucketAndPerson:
            // We always gather person counts.
            break;
        case model_t::E_IndividualArrivalTimesByPerson:
        case model_t::E_IndividualLongArrivalTimesByPerson:
        case model_t::E_IndividualShortArrivalTimesByPerson:
            // TODO
            break;
        case model_t::E_IndividualTimeOfDayByBucketAndPerson:
        case model_t::E_IndividualTimeOfWeekByBucketAndPerson:
            m_FeatureData[model_t::E_DiurnalTimes] =
                    TSizeSizePrMeanAccumulatorUMapQueue(m_DataGatherer.params().s_LatencyBuckets,
                                                        this->bucketLength(),
                                                        this->currentBucketStartTime());
            break;

        case model_t::E_IndividualLowNonZeroCountByBucketAndPerson:
        case model_t::E_IndividualHighNonZeroCountByBucketAndPerson:
            // We always gather person counts.
            break;
        case model_t::E_IndividualUniqueCountByBucketAndPerson:
        case model_t::E_IndividualLowUniqueCountByBucketAndPerson:
        case model_t::E_IndividualHighUniqueCountByBucketAndPerson:
        case model_t::E_IndividualInfoContentByBucketAndPerson:
        case model_t::E_IndividualHighInfoContentByBucketAndPerson:
        case model_t::E_IndividualLowInfoContentByBucketAndPerson:
            m_FeatureData[model_t::E_UniqueValues] =
                    TSizeSizePrStrDataUMapQueue(m_DataGatherer.params().s_LatencyBuckets,
                                                this->bucketLength(),
                                                this->currentBucketStartTime(),
                                                TSizeSizePrStrDataUMap(1));
            break;

        CASE_INDIVIDUAL_METRIC:
            LOG_ERROR("Unexpected feature = " << model_t::print(m_DataGatherer.feature(i)))
            break;

        case model_t::E_PopulationAttributeTotalCountByPerson:
        case model_t::E_PopulationCountByBucketPersonAndAttribute:
        case model_t::E_PopulationIndicatorOfBucketPersonAndAttribute:
        case model_t::E_PopulationLowCountsByBucketPersonAndAttribute:
        case model_t::E_PopulationHighCountsByBucketPersonAndAttribute:
            // We always gather person attribute counts.
            break;
        case model_t::E_PopulationUniquePersonCountByAttribute:
            m_FeatureData[model_t::E_AttributePeople] = TSizeUSetVec();
            break;
        case model_t::E_PopulationUniqueCountByBucketPersonAndAttribute:
        case model_t::E_PopulationLowUniqueCountByBucketPersonAndAttribute:
        case model_t::E_PopulationHighUniqueCountByBucketPersonAndAttribute:
        case model_t::E_PopulationInfoContentByBucketPersonAndAttribute:
        case model_t::E_PopulationLowInfoContentByBucketPersonAndAttribute:
        case model_t::E_PopulationHighInfoContentByBucketPersonAndAttribute:
            m_FeatureData[model_t::E_UniqueValues] =
                    TSizeSizePrStrDataUMapQueue(m_DataGatherer.params().s_LatencyBuckets,
                                                this->bucketLength(),
                                                this->currentBucketStartTime(),
                                                TSizeSizePrStrDataUMap(1));
            break;
        case model_t::E_PopulationTimeOfDayByBucketPersonAndAttribute:
        case model_t::E_PopulationTimeOfWeekByBucketPersonAndAttribute:
            m_FeatureData[model_t::E_DiurnalTimes] =
                    TSizeSizePrMeanAccumulatorUMapQueue(m_DataGatherer.params().s_LatencyBuckets,
                                                        this->bucketLength(),
                                                        this->currentBucketStartTime());
            break;

        CASE_POPULATION_METRIC:
            LOG_ERROR("Unexpected feature = " << model_t::print(m_DataGatherer.feature(i)))
            break;

        case model_t::E_PeersAttributeTotalCountByPerson:
        case model_t::E_PeersCountByBucketPersonAndAttribute:
        case model_t::E_PeersLowCountsByBucketPersonAndAttribute:
        case model_t::E_PeersHighCountsByBucketPersonAndAttribute:
            // We always gather person attribute counts.
            break;
        case model_t::E_PeersUniqueCountByBucketPersonAndAttribute:
        case model_t::E_PeersLowUniqueCountByBucketPersonAndAttribute:
        case model_t::E_PeersHighUniqueCountByBucketPersonAndAttribute:
        case model_t::E_PeersInfoContentByBucketPersonAndAttribute:
        case model_t::E_PeersLowInfoContentByBucketPersonAndAttribute:
        case model_t::E_PeersHighInfoContentByBucketPersonAndAttribute:
            m_FeatureData[model_t::E_UniqueValues] =
                    TSizeSizePrStrDataUMapQueue(m_DataGatherer.params().s_LatencyBuckets,
                                                this->bucketLength(),
                                                this->currentBucketStartTime(),
                                                TSizeSizePrStrDataUMap(1));
            break;
        case model_t::E_PeersTimeOfDayByBucketPersonAndAttribute:
        case model_t::E_PeersTimeOfWeekByBucketPersonAndAttribute:
            m_FeatureData[model_t::E_DiurnalTimes] =
                    TSizeSizePrMeanAccumulatorUMapQueue(m_DataGatherer.params().s_LatencyBuckets,
                                                        this->bucketLength(),
                                                        this->currentBucketStartTime());
            break;

        CASE_PEERS_METRIC:
            LOG_ERROR("Unexpected feature = " << model_t::print(m_DataGatherer.feature(i)))
            break;
        }
    }
}

void CEventRateBucketGatherer::addInfluencerCounts(core_t::TTime time,
                                                   TSizeFeatureDataPrVec &result) const
{
    const TSizeSizePrStrPtrPrUInt64UMapVec &influencers = this->influencerCounts(time);
    if (influencers.empty())
    {
        return;
    }

    for (std::size_t i = 0u; i < result.size(); ++i)
    {
        result[i].second.s_InfluenceValues.resize(influencers.size());
    }

    for (std::size_t i = 0u; i < influencers.size(); ++i)
    {
        for (TSizeSizePrStrPtrPrUInt64UMapCItr j = influencers[i].begin(); j != influencers[i].end(); ++j)
        {
            std::size_t pid = CDataGatherer::extractPersonId(j->first);
            TSizeFeatureDataPrVecItr k = std::lower_bound(result.begin(),
                                                          result.end(),
                                                          pid,
                                                          maths::COrderings::SFirstLess());
            if (k == result.end() || k->first != pid)
            {
                LOG_ERROR("Missing feature data for person " << m_DataGatherer.personName(pid));
                continue;
            }
            k->second.s_InfluenceValues[i].push_back(
                    TStrCRefDouble1VecDoublePrPr(
                            TStrCRef(*CDataGatherer::extractData(j->first)),
                            TDouble1VecDoublePr(TDouble1Vec(1, static_cast<double>(j->second)), 1.0)));
        }
    }
}

void CEventRateBucketGatherer::addInfluencerCounts(core_t::TTime time,
                                                   TSizeSizePrFeatureDataPrVec &result) const
{
    const TSizeSizePrStrPtrPrUInt64UMapVec &influencers = this->influencerCounts(time);
    if (influencers.empty())
    {
        return;
    }

    for (std::size_t i = 0u; i < result.size(); ++i)
    {
        result[i].second.s_InfluenceValues.resize(influencers.size());
    }

    for (std::size_t i = 0u; i < influencers.size(); ++i)
    {
        for (TSizeSizePrStrPtrPrUInt64UMapCItr j = influencers[i].begin(); j != influencers[i].end(); ++j)
        {
            TSizeSizePrFeatureDataPrVecItr k = std::lower_bound(result.begin(),
                                                                result.end(),
                                                                j->first.first,
                                                                maths::COrderings::SFirstLess());
            if (k == result.end() || k->first != j->first.first)
            {
                LOG_ERROR("Missing feature data for person "
                          << m_DataGatherer.personName(CDataGatherer::extractPersonId(j->first))
                          << " and attribute "
                          << m_DataGatherer.attributeName(CDataGatherer::extractAttributeId(j->first)));
                continue;
            }
            k->second.s_InfluenceValues[i].push_back(
                    TStrCRefDouble1VecDoublePrPr(
                            TStrCRef(*CDataGatherer::extractData(j->first)),
                            TDouble1VecDoublePr(TDouble1Vec(1, static_cast<double>(j->second)), 1.0)));
        }
    }
}

////// CUniqueStringFeatureData //////

void CUniqueStringFeatureData::insert(const std::string &value, const TStrPtrVec &influences)
{
    TWord valueHash = m_Dictionary1.word(value);
    m_UniqueStrings.emplace(valueHash, value);
    if (influences.size() > m_InfluencerUniqueStrings.size())
    {
        m_InfluencerUniqueStrings.resize(influences.size());
    }
    for (std::size_t i = 0; i < influences.size(); ++i)
    {
        // The influence strings are optional.
        if (influences[i])
        {
            m_InfluencerUniqueStrings[i][influences[i]].insert(valueHash);
        }
    }
}

void CUniqueStringFeatureData::populateDistinctCountFeatureData(SEventRateFeatureData &featureData) const
{
    featureData.s_Count = m_UniqueStrings.size();
    try
    {
        featureData.s_InfluenceValues.clear();
        featureData.s_InfluenceValues.reserve(m_InfluencerUniqueStrings.size());
        for (std::size_t i = 0u; i < m_InfluencerUniqueStrings.size(); ++i)
        {
            featureData.s_InfluenceValues.push_back(TStrCRefDouble1VecDoublePrPrVec());
            TStrCRefDouble1VecDoublePrPrVec &data = featureData.s_InfluenceValues.back();
            data.reserve(m_InfluencerUniqueStrings[i].size());
            for (TStrPtrWordSetUMapCItr j = m_InfluencerUniqueStrings[i].begin();
                 j != m_InfluencerUniqueStrings[i].end();
                 ++j)
            {
                data.push_back(TStrCRefDouble1VecDoublePrPr(
                                    TStrCRef(*j->first),
                                    TDouble1VecDoublePr(TDouble1Vec(1, static_cast<double>(j->second.size())), 1.0)));
            }
        }
    }
    catch (const std::exception &e)
    {
        LOG_ERROR("Failed to get distinct counts: " << e.what());
    }
}

void CUniqueStringFeatureData::populateInfoContentFeatureData(SEventRateFeatureData &featureData) const
{
    typedef std::vector<TStrCRef> TStrCRefVec;

    featureData.s_InfluenceValues.clear();
    core::CCompressUtils compressor(true);

    try
    {
        TStrCRefVec strings;

        strings.reserve(m_UniqueStrings.size());
        for (TWordStringUMapCItr itr = m_UniqueStrings.begin();
             itr != m_UniqueStrings.end();
             ++itr)
        {
            strings.push_back(TStrCRef(itr->second));
        }
        std::sort(strings.begin(), strings.end(), maths::COrderings::SLess());

        for (std::size_t i = 0u; i < strings.size(); ++i)
        {
            compressor.addString(strings[i]);
        }
        std::size_t length = 0u;
        if (compressor.compressedLength(true, length) == false)
        {
            LOG_ERROR("Failed to get compressed length");
            compressor.reset();
        }
        featureData.s_Count = length;

        featureData.s_InfluenceValues.reserve(m_InfluencerUniqueStrings.size());
        for (std::size_t i = 0u; i < m_InfluencerUniqueStrings.size(); ++i)
        {
            featureData.s_InfluenceValues.push_back(TStrCRefDouble1VecDoublePrPrVec());
            TStrCRefDouble1VecDoublePrPrVec &data = featureData.s_InfluenceValues.back();
            for (TStrPtrWordSetUMapCItr j = m_InfluencerUniqueStrings[i].begin();
                 j != m_InfluencerUniqueStrings[i].end();
                 ++j)
            {
                strings.clear();
                strings.reserve(j->second.size());
                for (TWordSetCItr k = j->second.begin();
                     k != j->second.end();
                     ++k)
                {
                    strings.push_back(TStrCRef(m_UniqueStrings.at(*k)));
                }
                std::sort(strings.begin(), strings.end(), maths::COrderings::SLess());

                for (std::size_t k = 0u; k < strings.size(); ++k)
                {
                    compressor.addString(strings[k]);
                }
                length = 0u;
                if (compressor.compressedLength(true, length) == false)
                {
                    LOG_ERROR("Failed to get compressed length");
                    compressor.reset();
                }
                data.push_back(TStrCRefDouble1VecDoublePrPr(
                                   TStrCRef(*j->first),
                                   TDouble1VecDoublePr(TDouble1Vec(1, static_cast<double>(length)), 1.0)));
            }
        }
    }
    catch (const std::exception &e)
    {
        LOG_ERROR("Failed to get info content: " << e.what());
    }
}

void CUniqueStringFeatureData::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertLevel(UNIQUE_STRINGS_TAG,
                         boost::bind(&persistUniqueStrings,
                                     boost::cref(m_UniqueStrings),
                                     _1));
    for (std::size_t i = 0u; i < m_InfluencerUniqueStrings.size(); ++i)
    {
        inserter.insertLevel(INFLUENCER_UNIQUE_STRINGS_TAG,
                             boost::bind(&persistInfluencerUniqueStrings,
                                         boost::cref(m_InfluencerUniqueStrings[i]),
                                         _1));
    }
}

bool CUniqueStringFeatureData::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        if (name == UNIQUE_STRINGS_TAG)
        {
            if (traverser.traverseSubLevel(boost::bind(&restoreUniqueStrings,
                                                       _1,
                                                       boost::ref(m_UniqueStrings))) == false)
            {
                LOG_ERROR("Failed to restore unique strings");
                return false;
            }
        }
        else if (name == INFLUENCER_UNIQUE_STRINGS_TAG)
        {
            m_InfluencerUniqueStrings.push_back(TStrPtrWordSetUMap());
            if (traverser.traverseSubLevel(boost::bind(&restoreInfluencerUniqueStrings,
                                                       _1,
                                                       boost::ref(m_InfluencerUniqueStrings.back()))) == false)
            {
                LOG_ERROR("Failed to restore influencer unique strings");
                return false;
            }
        }
    }
    while (traverser.next());

    return true;
}

uint64_t CUniqueStringFeatureData::checksum(void) const
{
    uint64_t seed = maths::CChecksum::calculate(0, m_UniqueStrings);
    return maths::CChecksum::calculate(seed, m_InfluencerUniqueStrings);
}

void CUniqueStringFeatureData::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    mem->setName("CUniqueStringFeatureData", sizeof(*this));
    core::CMemoryDebug::dynamicSize("s_NoInfluenceUniqueStrings", m_UniqueStrings, mem);
    core::CMemoryDebug::dynamicSize("s_InfluenceUniqueStrings", m_InfluencerUniqueStrings, mem);
}

std::size_t CUniqueStringFeatureData::memoryUsage(void) const
{
    std::size_t mem = sizeof(*this);
    mem += core::CMemory::dynamicSize(m_UniqueStrings);
    mem += core::CMemory::dynamicSize(m_InfluencerUniqueStrings);
    return mem;
}

std::string CUniqueStringFeatureData::print(void) const
{
    return "(" + core::CContainerPrinter::print(m_UniqueStrings) + ", " +
                 core::CContainerPrinter::print(m_InfluencerUniqueStrings) + ")";
}

}
}
