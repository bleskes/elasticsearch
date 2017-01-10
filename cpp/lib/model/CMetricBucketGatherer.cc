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

#include <model/CMetricBucketGatherer.h>

#include <core/CContainerPrinter.h>
#include <core/CLogger.h>
#include <core/CStatistics.h>

#include <maths/CChecksum.h>
#include <maths/COrderings.h>
#include <maths/CPrior.h>

#include <model/CGathererTools.h>
#include <model/CSampleCounts.h>
#include <model/CSampleGatherer.h>
#include <model/CSearchKey.h>

#include <boost/any.hpp>
#include <boost/ref.hpp>
#include <boost/tuple/tuple.hpp>
#include <boost/unordered_map.hpp>

#include <map>
#include <utility>
#include <vector>

namespace ml
{
namespace model
{

namespace
{

typedef std::pair<std::size_t, std::size_t> TSizeSizePr;
typedef std::vector<double> TDoubleVec;
typedef std::vector<std::size_t> TSizeVec;
typedef std::vector<std::string> TStrVec;
typedef boost::reference_wrapper<const std::string> TStrCRef;
typedef std::map<TStrCRef, uint64_t, maths::COrderings::SReferenceLess> TStrCRefUInt64Map;
typedef std::vector<CSample> TSampleVec;
typedef std::vector<CGathererTools::TMeanGatherer> TMeanGathererVec;
typedef std::vector<CGathererTools::TMedianGatherer> TMedianGathererVec;
typedef std::vector<CGathererTools::TMinGatherer> TMinGathererVec;
typedef std::vector<CGathererTools::TMaxGatherer> TMaxGathererVec;
typedef std::vector<CGathererTools::TVarianceGatherer> TVarianceGathererVec;
typedef std::vector<CGathererTools::CSumGatherer> TSumGathererVec;
typedef std::vector<CGathererTools::TMultivariateMeanGatherer> TMultivariateMeanGathererVec;
typedef std::vector<CGathererTools::TMultivariateMinGatherer> TMultivariateMinGathererVec;
typedef std::vector<CGathererTools::TMultivariateMaxGatherer> TMultivariateMaxGathererVec;
typedef boost::unordered_map<std::size_t, CGathererTools::TMeanGatherer> TSizeMeanGathererUMap;
typedef std::vector<TSizeMeanGathererUMap> TSizeMeanGathererUMapVec;
typedef boost::unordered_map<std::size_t, CGathererTools::TMedianGatherer> TSizeMedianGathererUMap;
typedef std::vector<TSizeMedianGathererUMap> TSizeMedianGathererUMapVec;
typedef boost::unordered_map<std::size_t, CGathererTools::TMinGatherer> TSizeMinGathererUMap;
typedef std::vector<TSizeMinGathererUMap> TSizeMinGathererUMapVec;
typedef boost::unordered_map<std::size_t, CGathererTools::TMaxGatherer> TSizeMaxGathererUMap;
typedef std::vector<TSizeMaxGathererUMap> TSizeMaxGathererUMapVec;
typedef boost::unordered_map<std::size_t, CGathererTools::TVarianceGatherer> TSizeVarianceGathererUMap;
typedef std::vector<TSizeVarianceGathererUMap> TSizeVarianceGathererUMapVec;
typedef boost::unordered_map<std::size_t, CGathererTools::CSumGatherer> TSizeSumGathererUMap;
typedef std::vector<TSizeSumGathererUMap> TSizeSumGathererUMapVec;
typedef boost::unordered_map<std::size_t, CGathererTools::TMultivariateMeanGatherer> TSizeMultivariateMeanGathererUMap;
typedef std::vector<TSizeMultivariateMeanGathererUMap> TSizeMultivariateMeanGathererUMapVec;
typedef boost::unordered_map<std::size_t, CGathererTools::TMultivariateMinGatherer> TSizeMultivariateMinGathererUMap;
typedef std::vector<TSizeMultivariateMinGathererUMap> TSizeMultivariateMinGathererUMapVec;
typedef boost::unordered_map<std::size_t, CGathererTools::TMultivariateMaxGatherer> TSizeMultivariateMaxGathererUMap;
typedef std::vector<TSizeMultivariateMaxGathererUMap> TSizeMultivariateMaxGathererUMapVec;
typedef std::pair<std::size_t, SMetricFeatureData> TSizeFeatureDataPr;
typedef std::vector<TSizeFeatureDataPr> TSizeFeatureDataPrVec;
typedef std::pair<TSizeSizePr, SMetricFeatureData> TSizeSizePrFeatureDataPr;
typedef std::vector<TSizeSizePrFeatureDataPr> TSizeSizePrFeatureDataPrVec;
typedef CMetricBucketGatherer::TSizeSizePrUInt64UMap TSizeSizePrUInt64UMap;
typedef CMetricBucketGatherer::TSizeSizePrUInt64UMapCItr TSizeSizePrUInt64UMapCItr;
typedef CMetricBucketGatherer::TCategorySizePr TCategorySizePr;
typedef CMetricBucketGatherer::TCategorySizePrAnyMap TCategorySizePrAnyMap;
typedef CMetricBucketGatherer::TCategorySizePrAnyMapItr TCategorySizePrAnyMapItr;
typedef CMetricBucketGatherer::TCategorySizePrAnyMapCItr TCategorySizePrAnyMapCItr;
typedef CBucketGatherer::TStrPtrVec TStrPtrVec;

// We obfuscate the XML element names to avoid giving away too much
// information about our model.
const std::string BASE_TAG("a");
const std::string MEAN_TAG("e");
const std::string MIN_TAG("f");
const std::string MAX_TAG("g");
const std::string SUM_TAG("h");
const std::string MULTIVARIATE_MEAN_TAG("i");
const std::string MULTIVARIATE_MIN_TAG("j");
const std::string MULTIVARIATE_MAX_TAG("k");
const std::string MEDIAN_TAG("l");
const std::string VARIANCE_TAG("m");
const std::string EMPTY_STRING;
const TDoubleVec EMPTY_DOUBLE_VEC;

// Nested tags.
const std::string ATTRIBUTE_TAG("a");
const std::string DATA_TAG("b");


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

template<model_t::EMetricCategory, bool> struct SDataType {};
template<> struct SDataType<model_t::E_Mean,     true>          { typedef TSizeMeanGathererUMapVec Type; };
template<> struct SDataType<model_t::E_Mean,     false>         { typedef TMeanGathererVec Type; };
template<> struct SDataType<model_t::E_Median,   true>          { typedef TSizeMedianGathererUMapVec Type; };
template<> struct SDataType<model_t::E_Median,   false>         { typedef TMedianGathererVec Type; };
template<> struct SDataType<model_t::E_Min,      true>          { typedef TSizeMinGathererUMapVec Type; };
template<> struct SDataType<model_t::E_Min,      false>         { typedef TMinGathererVec Type; };
template<> struct SDataType<model_t::E_Max,      true>          { typedef TSizeMaxGathererUMapVec Type; };
template<> struct SDataType<model_t::E_Max,      false>         { typedef TMaxGathererVec Type; };
template<> struct SDataType<model_t::E_Sum,      true>          { typedef TSizeSumGathererUMapVec Type; };
template<> struct SDataType<model_t::E_Sum,      false>         { typedef TSumGathererVec Type; };
template<> struct SDataType<model_t::E_Variance, true>          { typedef TSizeVarianceGathererUMapVec Type; };
template<> struct SDataType<model_t::E_Variance, false>         { typedef TVarianceGathererVec Type; };
template<> struct SDataType<model_t::E_MultivariateMean, true>  { typedef TSizeMultivariateMeanGathererUMapVec Type; };
template<> struct SDataType<model_t::E_MultivariateMean, false> { typedef TMultivariateMeanGathererVec Type; };
template<> struct SDataType<model_t::E_MultivariateMin,  true>  { typedef TSizeMultivariateMinGathererUMapVec Type; };
template<> struct SDataType<model_t::E_MultivariateMin,  false> { typedef TMultivariateMinGathererVec Type; };
template<> struct SDataType<model_t::E_MultivariateMax,  true>  { typedef TSizeMultivariateMaxGathererUMapVec Type; };
template<> struct SDataType<model_t::E_MultivariateMax,  false> { typedef TMultivariateMaxGathererVec Type; };
template<typename ITR, typename T> struct SMaybeConst {};
template<typename T> struct SMaybeConst<TCategorySizePrAnyMapItr, T> { typedef T Type; };
template<typename T> struct SMaybeConst<TCategorySizePrAnyMapCItr, T> { typedef const T Type; };

//! Register the callbacks for computing the size of feature data gatherers.
void registerMemoryCallbacks(void)
{
    static bool once = true;
    if (once)
    {
        core::CMemory::CAnyVisitor &visitor = core::CMemory::anyVisitor();
        visitor.registerCallback<TMeanGathererVec>();
        visitor.registerCallback<TMedianGathererVec>();
        visitor.registerCallback<TMinGathererVec>();
        visitor.registerCallback<TMaxGathererVec>();
        visitor.registerCallback<TVarianceGathererVec>();
        visitor.registerCallback<TSumGathererVec>();
        visitor.registerCallback<TSizeMeanGathererUMapVec>();
        visitor.registerCallback<TSizeMedianGathererUMapVec>();
        visitor.registerCallback<TSizeMinGathererUMapVec>();
        visitor.registerCallback<TSizeMaxGathererUMapVec>();
        visitor.registerCallback<TSizeVarianceGathererUMapVec>();
        visitor.registerCallback<TSizeSumGathererUMapVec>();
        visitor.registerCallback<TMultivariateMeanGathererVec>();
        visitor.registerCallback<TMultivariateMinGathererVec>();
        visitor.registerCallback<TMultivariateMaxGathererVec>();
        visitor.registerCallback<TSizeMultivariateMeanGathererUMapVec>();
        visitor.registerCallback<TSizeMultivariateMinGathererUMapVec>();
        visitor.registerCallback<TSizeMultivariateMaxGathererUMapVec>();
        once = false;
    }
}

//! Apply a function \p f to all the gatherers held in [\p begin, \p end).
template<typename ITR, typename F>
bool apply(bool population, ITR begin, ITR end, const F &f)
{
#define CALL(category) if (population)                                                                           \
                       {                                                                                         \
                           typedef typename SDataType<category, true>::Type TDataType;                           \
                           f(i->first, boost::any_cast<typename SMaybeConst<ITR, TDataType>::Type&>(i->second)); \
                       }                                                                                         \
                       else                                                                                      \
                       {                                                                                         \
                           typedef typename SDataType<category, false>::Type TDataType;                          \
                           f(i->first, boost::any_cast<typename SMaybeConst<ITR, TDataType>::Type&>(i->second)); \
                       }

    for (ITR i = begin; i != end; ++i)
    {
        model_t::EMetricCategory category = i->first.first;
        try
        {
            switch (category)
            {
            case model_t::E_Mean:             CALL(model_t::E_Mean);             break;
            case model_t::E_Median:           CALL(model_t::E_Median);           break;
            case model_t::E_Min:              CALL(model_t::E_Min);              break;
            case model_t::E_Max:              CALL(model_t::E_Max);              break;
            case model_t::E_Variance:         CALL(model_t::E_Variance);         break;
            case model_t::E_Sum:              CALL(model_t::E_Sum);              break;
            case model_t::E_MultivariateMean: CALL(model_t::E_MultivariateMean); break;
            case model_t::E_MultivariateMin:  CALL(model_t::E_MultivariateMin);  break;
            case model_t::E_MultivariateMax:  CALL(model_t::E_MultivariateMax);  break;
            }
        }
        catch (const std::exception &e)
        {
            LOG_ERROR("Apply failed for " << category << ": " << e.what());
            return false;
        }
    }

#undef CALL

    return true;
}

//! Apply a function \p f to all the gatherers held in \p data.
template<typename T, typename F>
bool apply(bool population, T &data, const F &f)
{
    return apply(population, data.begin(), data.end(), f);
}

//! Persists the data gatherers (for individual metric categories).
class CPersistFeatureData
{
    public:
        template<typename T>
        void operator()(const TCategorySizePr &category,
                        const T &data,
                        core::CStatePersistInserter &inserter) const
        {
            if (data.empty())
            {
                inserter.insertValue(this->tagName(category), EMPTY_STRING);
                return;
            }
            for (std::size_t pid = 0; pid < data.size(); ++pid)
            {
                inserter.insertLevel(this->tagName(category),
                                     boost::bind<void>(SDoPersist(), boost::cref(data[pid]), _1));
            }
        }

    private:
        std::string tagName(const TCategorySizePr &category) const
        {
            switch (category.first)
            {
            case model_t::E_Mean:             return MEAN_TAG;
            case model_t::E_Median:           return MEDIAN_TAG;
            case model_t::E_Min:              return MIN_TAG;
            case model_t::E_Max:              return MAX_TAG;
            case model_t::E_Variance:         return VARIANCE_TAG;
            case model_t::E_Sum:              return SUM_TAG;
            case model_t::E_MultivariateMean: return  MULTIVARIATE_MEAN_TAG
                                                    + core::CStringUtils::typeToString(category.second);
            case model_t::E_MultivariateMin:  return  MULTIVARIATE_MIN_TAG
                                                    + core::CStringUtils::typeToString(category.second);
            case model_t::E_MultivariateMax:  return  MULTIVARIATE_MAX_TAG
                                                    + core::CStringUtils::typeToString(category.second);
            }
            return EMPTY_STRING;
        }

        struct SDoPersist
        {
            template<typename T>
            void operator()(const T &data,
                            core::CStatePersistInserter &inserter) const
            {
                inserter.insertLevel(DATA_TAG, boost::bind(&T::acceptPersistInserter, &data, _1));
            }

            template<typename T>
            void operator()(const boost::unordered_map<std::size_t, T> &data,
                            core::CStatePersistInserter &inserter) const
            {
                typedef std::pair<std::size_t, const T *> TSizeTPtrPr;
                typedef std::vector<TSizeTPtrPr> TSizeTPtrPrVec;

                if (!data.empty())
                {
                    // Persist the attribute identifiers in sorted order
                    // to make it easier to compare state records.

                    TSizeTPtrPrVec orderedData;
                    orderedData.reserve(data.size());
                    for (typename boost::unordered_map<std::size_t, T>::const_iterator i = data.begin();
                         i != data.end();
                         ++i)
                    {
                        orderedData.push_back(TSizeTPtrPr(i->first, &i->second));
                    }
                    std::sort(orderedData.begin(), orderedData.end(), maths::COrderings::SFirstLess());
                    for (std::size_t i = 0u; i < orderedData.size(); ++i)
                    {
                        inserter.insertValue(ATTRIBUTE_TAG, orderedData[i].first);
                        inserter.insertLevel(DATA_TAG,
                                             boost::bind(&T::acceptPersistInserter,
                                                         orderedData[i].second,
                                                         _1));
                    }
                }
            }
        };
};

//! Restores the data gatherers (for individual metric categories).
template<model_t::EMetricCategory CATEGORY>
class CRestoreFeatureData
{
    public:
        bool operator()(core::CStateRestoreTraverser &traverser,
                        std::size_t dimension,
                        bool isPopulation,
                        const CMetricBucketGatherer &gatherer,
                        TCategorySizePrAnyMap &result) const
        {
            boost::any &data = result[std::make_pair(CATEGORY, dimension)];
            return isPopulation ?
                   this->restore<true>(traverser, dimension, gatherer, data) :
                   this->restore<false>(traverser, dimension, gatherer, data);
        }

    private:
        //! Add a restored data gatherer to \p result.
        template<bool POPULATION>
        bool restore(core::CStateRestoreTraverser &traverser,
                     std::size_t dimension,
                     const CMetricBucketGatherer &gatherer,
                     boost::any &result) const
        {
            typedef typename SDataType<CATEGORY, POPULATION>::Type Type;
            if (result.empty())
            {
                result = Type();
            }
            Type &data = *boost::unsafe_any_cast<Type>(&result);

            // An empty sub-level implies a person with 100% invalid data.
            if (!traverser.hasSubLevel())
            {
                return true;
            }
            return traverser.traverseSubLevel(boost::bind<bool>(CDoRestore(dimension),
                                                                _1,
                                                                boost::cref(gatherer),
                                                                boost::ref(data)));
        }

        //! \brief Responsible for restoring individual gatherers.
        class CDoRestore
        {
            public:
                CDoRestore(std::size_t dimension) : m_Dimension(dimension) {}

                template<typename T>
                bool operator()(core::CStateRestoreTraverser &traverser,
                                const CMetricBucketGatherer &gatherer,
                                std::vector<T> &result) const
                {
                    insertEmpty(m_Dimension, gatherer, result);
                    do
                    {
                        const std::string &name = traverser.name();
                        if (name == DATA_TAG)
                        {
                            if (traverser.traverseSubLevel(boost::bind(&T::acceptRestoreTraverser,
                                                                       &result.back(),
                                                                       _1)) == false)
                            {
                                LOG_ERROR("Invalid data in " << traverser.value());
                                return false;
                            }
                        }
                    }
                    while (traverser.next());

                    return true;
                }

                template<typename T>
                bool operator()(core::CStateRestoreTraverser &traverser,
                                const CMetricBucketGatherer &gatherer,
                                std::vector<boost::unordered_map<std::size_t, T> > &result) const
                {
                    insertEmpty(m_Dimension, gatherer, result);

                    std::size_t lastCid(0);
                    bool seenCid(false);

                    do
                    {
                        const std::string &name = traverser.name();
                        if (name == ATTRIBUTE_TAG)
                        {
                            if (core::CStringUtils::stringToType(traverser.value(), lastCid) == false)
                            {
                                LOG_ERROR("Invalid attribute ID in " << traverser.value());
                                return false;
                            }
                            seenCid = true;
                        }
                        else if (name == DATA_TAG)
                        {
                            if (!seenCid)
                            {
                                LOG_ERROR("Incorrect format - data before attribute ID in " <<
                                          traverser.value());
                                return false;
                            }
                            T initial(gatherer.dataGatherer().params(),
                                      m_Dimension,
                                      gatherer.currentBucketStartTime(),
                                      gatherer.bucketLength(),
                                      gatherer.beginInfluencers(),
                                      gatherer.endInfluencers());
                            if (traverser.traverseSubLevel(boost::bind(&T::acceptRestoreTraverser,
                                                                       &initial,
                                                                       _1)) == false)
                            {
                                LOG_ERROR("Invalid data in " << traverser.value());
                                return false;
                            }
                            result.back().emplace(lastCid, initial);
                        }
                    }
                    while (traverser.next());

                    return true;
                }

            private:
                std::size_t m_Dimension;
        };

        template<typename T>
        static void insertEmpty(std::size_t dimension,
                                const CMetricBucketGatherer &gatherer,
                                std::vector<T> &result)
        {
            result.push_back(T(gatherer.dataGatherer().params(),
                               dimension,
                               gatherer.currentBucketStartTime(),
                               gatherer.bucketLength(),
                               gatherer.beginInfluencers(),
                               gatherer.endInfluencers()));
        }

        template<typename T>
        static void insertEmpty(std::size_t /*dimension*/,
                                const CMetricBucketGatherer &/*gatherer*/,
                                std::vector<boost::unordered_map<std::size_t, T> > &result)
        {
            result.push_back(boost::unordered_map<std::size_t, T>());
        }
};

//! Removes the people from the data gatherers.
struct SRemovePeople
{
    public:
        template<typename T>
        void operator()(const TCategorySizePr &/*category*/,
                        T &data,
                        std::size_t lowestPidToRemove) const
        {
            if (lowestPidToRemove < data.size())
            {
                data.erase(data.begin() + lowestPidToRemove, data.end());
            }
        }

        template<typename T>
        void operator()(const TCategorySizePr &category,
                        T &data,
                        const CMetricBucketGatherer &gatherer,
                        const TSizeVec &peopleToRemove) const
        {
            for (std::size_t i = 0u; i < peopleToRemove.size(); ++i)
            {
                std::size_t pid = peopleToRemove[i];
                if (pid < data.size())
                {
                    this->removePeople(category.second, data[pid], gatherer);
                }
            }
        }

    private:
        template<typename T>
        void removePeople(std::size_t dimension,
                          T &data,
                          const CMetricBucketGatherer &gatherer) const
        {
            data = T(gatherer.dataGatherer().params(),
                     dimension,
                     gatherer.currentBucketStartTime(),
                     gatherer.bucketLength(),
                     gatherer.beginInfluencers(),
                     gatherer.endInfluencers());
        }

        template<typename T>
        void removePeople(std::size_t /*dimension*/,
                          boost::unordered_map<std::size_t, T> &data,
                          const CMetricBucketGatherer &/*gatherer*/) const
        {
            data.clear();
        }
};

//! Removes attributes from the data gatherers.
struct SRemoveAttributes
{
    template<typename T>
    void operator()(const TCategorySizePr &/*category*/,
                    std::vector<T> &/*data*/,
                    std::size_t /*lowestAttributeToRemove*/,
                    std::size_t /*endAttributes*/) const
    {}

    template<typename T>
    void operator()(const TCategorySizePr &/*category*/,
                    std::vector<T> &/*data*/,
                    const TSizeVec &/*attributesToRemove*/) const
    {}

    template<typename T>
    void operator()(const TCategorySizePr &/*category*/,
                    std::vector<boost::unordered_map<std::size_t, T> > &data,
                    std::size_t begin,
                    std::size_t end) const
    {
        for (std::size_t pid = 0u; pid < data.size(); ++pid)
        {
            for (std::size_t cid = begin; cid < end; ++cid)
            {
                data[pid].erase(cid);
            }
        }
    }

    template<typename T>
    void operator()(const TCategorySizePr &/*category*/,
                    std::vector<boost::unordered_map<std::size_t, T> > &data,
                    const TSizeVec &attributesToRemove) const
    {
        for (std::size_t pid = 0u; pid < data.size(); ++pid)
        {
            for (std::size_t i = 0u; i < attributesToRemove.size(); ++i)
            {
                data[pid].erase(attributesToRemove[i]);
            }
        }
    }
};

//! Sample the metric statistics.
struct SDoSample
{
    public:
        template<typename T>
        void operator()(const TCategorySizePr &/*category*/,
                        T &data,
                        core_t::TTime time,
                        const CMetricBucketGatherer &gatherer,
                        CDataGatherer::TSampleCountsPtr sampleCounts) const
        {
            const TSizeSizePrUInt64UMap &counts = gatherer.bucketCounts(time);
            for (TSizeSizePrUInt64UMapCItr countItr = counts.begin();
                 countItr != counts.end();
                 ++countItr)
            {
                std::size_t pid = CDataGatherer::extractPersonId(*countItr);
                if (pid >= data.size())
                {
                    LOG_ERROR("Unexpected person identifier " << gatherer.dataGatherer().personName(pid));
                    continue;
                }
                std::size_t cid = CDataGatherer::extractAttributeId(*countItr);
                this->sample(data[pid], gatherer, pid, cid, time, sampleCounts);
            }
        }

    private:
        template<typename T>
        void sample(T &data,
                    const CMetricBucketGatherer &/*gatherer*/,
                    std::size_t pid,
                    std::size_t /*cid*/,
                    core_t::TTime time,
                    CDataGatherer::TSampleCountsPtr &sampleCounts) const
        {
            if (data.sample(time, sampleCounts->count(pid)))
            {
                sampleCounts->updateSampleVariance(pid);
            }
        }

        template<typename T>
        void sample(boost::unordered_map<std::size_t, T> &data,
                    const CMetricBucketGatherer &gatherer,
                    std::size_t pid,
                    std::size_t cid,
                    core_t::TTime time,
                    CDataGatherer::TSampleCountsPtr &sampleCounts) const
        {
            typename boost::unordered_map<std::size_t, T>::iterator i = data.find(cid);
            if (i == data.end())
            {
                LOG_ERROR("No gatherer for attribute " << gatherer.dataGatherer().attributeName(cid)
                          << " of person " << gatherer.dataGatherer().personName(pid));
            }
            else
            {
                if (i->second.sample(time, sampleCounts->count(cid)))
                {
                    sampleCounts->updateSampleVariance(cid);
                }
            }
        }
};

//! Stably hashes the a collection of data gatherers.
struct SHash
{
    public:
        typedef std::vector<uint64_t> TUInt64Vec;

    public:
        template<typename T>
        void operator()(const TCategorySizePr &/*category*/,
                        const T &data,
                        const CMetricBucketGatherer &gatherer,
                        TStrCRefUInt64Map &hashes) const
        {
            TUInt64Vec checksums;
            for (std::size_t pid = 0u; pid < data.size(); ++pid)
            {
                if (gatherer.dataGatherer().isPersonActive(pid))
                {
                    checksums.clear();
                    this->hash(data[pid], checksums);
                    std::sort(checksums.begin(), checksums.end());
                    uint64_t &hash = hashes[TStrCRef(gatherer.dataGatherer().personName(pid))];
                    hash = maths::CChecksum::calculate(hash, checksums);
                }
            }
        }

    private:
        template<typename T>
        void hash(const T &data, TUInt64Vec &checksums) const
        {
            checksums.push_back(data.checksum());
        }

        template<typename T>
        void hash(const boost::unordered_map<std::size_t, T> &data,
                  TUInt64Vec &checksums) const
        {
            for (typename boost::unordered_map<std::size_t, T>::const_iterator i = data.begin();
                 i != data.end();
                 ++i)
            {
                checksums.push_back(i->second.checksum());
            }
        }
};

//! Extracts feature data from a collection of gatherers.
struct SExtractFeatureData
{
    public:
        typedef std::pair<model_t::EFeature, boost::any> TFeatureAnyPr;
        typedef std::vector<TFeatureAnyPr> TFeatureAnyPrVec;

    public:
        template<typename T>
        void operator()(const TCategorySizePr &/*category*/,
                        const std::vector<T> &data,
                        const CMetricBucketGatherer &gatherer,
                        model_t::EFeature feature,
                        core_t::TTime time,
                        core_t::TTime bucketLength,
                        TFeatureAnyPrVec &result) const
        {
            result.push_back(TFeatureAnyPr(feature, boost::any(TSizeFeatureDataPrVec())));
            this->featureData(data, gatherer, time, bucketLength, this->isSum(feature),
                              *boost::unsafe_any_cast<TSizeFeatureDataPrVec>(&result.back().second));
        }

        template<typename T>
        void operator()(const TCategorySizePr &/*category*/,
                        const std::vector<boost::unordered_map<std::size_t, T> > &data,
                        const CMetricBucketGatherer &gatherer,
                        model_t::EFeature feature,
                        core_t::TTime time,
                        core_t::TTime bucketLength,
                        TFeatureAnyPrVec &result) const
        {
            result.push_back(TFeatureAnyPr(feature, boost::any(TSizeSizePrFeatureDataPrVec())));
            this->featureData(data, gatherer, time, bucketLength, this->isSum(feature),
                              *boost::unsafe_any_cast<TSizeSizePrFeatureDataPrVec>(&result.back().second));
        }

    private:
        static const TSampleVec ZERO_SAMPLE;

    private:
        bool isSum(model_t::EFeature feature) const
        {
            return    feature == model_t::E_IndividualSumByBucketAndPerson
                   || feature == model_t::E_IndividualLowSumByBucketAndPerson
                   || feature == model_t::E_IndividualHighSumByBucketAndPerson;
        }

        template<typename T, typename U>
        void featureData(const T &data,
                         const CMetricBucketGatherer &gatherer,
                         core_t::TTime time,
                         core_t::TTime bucketLength,
                         bool isSum,
                         U &result) const
        {
            result.clear();
            if (isSum)
            {
                result.reserve(data.size());
                for (std::size_t pid = 0u; pid < data.size(); ++pid)
                {
                    if (   gatherer.dataGatherer().isPersonActive(pid)
                        && gatherer.hasExplicitNullsOnly(time, pid, model_t::INDIVIDUAL_ANALYSIS_ATTRIBUTE_ID) == false)
                    {
                        this->featureData(data[pid], gatherer, pid, model_t::INDIVIDUAL_ANALYSIS_ATTRIBUTE_ID, time, bucketLength, result);
                    }
                }
            }
            else
            {
                const TSizeSizePrUInt64UMap &counts = gatherer.bucketCounts(time);
                result.reserve(counts.size());
                for (TSizeSizePrUInt64UMapCItr i = counts.begin(); i != counts.end(); ++i)
                {
                    std::size_t pid = CDataGatherer::extractPersonId(*i);
                    if (pid >= data.size())
                    {
                        LOG_ERROR("No gatherers for person " << gatherer.dataGatherer().personName(pid));
                        continue;
                    }
                    std::size_t cid = CDataGatherer::extractAttributeId(*i);
                    this->featureData(data[pid], gatherer, pid, cid, time, bucketLength, result);
                }
                std::sort(result.begin(), result.end(), maths::COrderings::SFirstLess());
            }
        }

        template<typename T>
        void featureData(const T &data,
                         const CMetricBucketGatherer &gatherer,
                         std::size_t pid,
                         std::size_t /*cid*/,
                         core_t::TTime time,
                         core_t::TTime bucketLength,
                         TSizeFeatureDataPrVec &result) const
        {
            result.push_back(TSizeFeatureDataPr(
                                 pid,
                                 this->featureData(data,
                                                   time,
                                                   bucketLength,
                                                   gatherer.dataGatherer().effectiveSampleCount(pid))));
        }

        template<typename T>
        void featureData(const boost::unordered_map<std::size_t, T> &data,
                         const CMetricBucketGatherer &gatherer,
                         std::size_t pid,
                         std::size_t cid,
                         core_t::TTime time,
                         core_t::TTime bucketLength,
                         TSizeSizePrFeatureDataPrVec &result) const
        {
            typename boost::unordered_map<std::size_t, T>::const_iterator i = data.find(cid);
            if (i == data.end())
            {
                LOG_ERROR("No gatherer for attribute " << gatherer.dataGatherer().attributeName(cid)
                          << " of person " << gatherer.dataGatherer().personName(pid));
                return;
            }
            result.push_back(TSizeSizePrFeatureDataPr(
                                 std::make_pair(pid, cid),
                                 this->featureData(i->second,
                                                   time,
                                                   bucketLength,
                                                   gatherer.dataGatherer().effectiveSampleCount(cid))));
        }

        SMetricFeatureData featureData(const CGathererTools::CSumGatherer &data,
                                       core_t::TTime time,
                                       core_t::TTime bucketLength,
                                       double /*effectiveSampleCount*/) const
        {
            return data.featureData(time, bucketLength, ZERO_SAMPLE);
        }

        template<typename T>
        inline SMetricFeatureData featureData(const T &data,
                                              core_t::TTime time,
                                              core_t::TTime bucketLength,
                                              double effectiveSampleCount) const
        {
            return data.featureData(time, bucketLength, effectiveSampleCount);
        }
};

const TSampleVec SExtractFeatureData::ZERO_SAMPLE(1, CSample(0, TDoubleVec(1, 0.0), 1.0, 1.0));

//! Resizes the container so that it is big enough for the
//! specified person identifier.
struct SResize
{
    template<typename T>
    void operator()(const TCategorySizePr &category,
                    std::vector<T> &data,
                    std::size_t pid,
                    const CMetricBucketGatherer &gatherer) const
    {
        if (pid >= data.size())
        {
            data.resize(pid + 1, T(gatherer.dataGatherer().params(),
                                   category.second,
                                   gatherer.currentBucketStartTime(),
                                   gatherer.bucketLength(),
                                   gatherer.beginInfluencers(),
                                   gatherer.endInfluencers()));
        }
    }

    template<typename T>
    void operator()(const TCategorySizePr &/*category*/,
                    std::vector<boost::unordered_map<std::size_t, T> > &data,
                    std::size_t pid,
                    const CMetricBucketGatherer &/*gatherer*/) const
    {
        if (pid >= data.size())
        {
            data.resize(pid + 1);
        }
    }
};

//! Adds a value to the specified data gatherers.
struct SAddValue
{
    struct SStatistic
    {
        core_t::TTime s_Time;
        const CEventData::TDouble1VecArray *s_Values;
        unsigned int s_Count;
        unsigned int s_SampleCount;
        const TStrPtrVec *s_Influences;
    };

    template<typename T>
    inline void operator()(const TCategorySizePr &category,
                           std::vector<T> &data,
                           std::size_t pid,
                           std::size_t /*cid*/,
                           const CMetricBucketGatherer &/*gatherer*/,
                           const SStatistic &stat) const
    {
        data[pid].add(stat.s_Time,
                      (*stat.s_Values)[category.first],
                      stat.s_Count,
                      stat.s_SampleCount,
                      *stat.s_Influences);
    }

    template<typename T>
    inline void operator()(const TCategorySizePr &category,
                           std::vector<boost::unordered_map<std::size_t, T> > &data,
                           std::size_t pid,
                           std::size_t cid,
                           const CMetricBucketGatherer &gatherer,
                           const SStatistic &stat) const
    {
        T &entry = data[pid].emplace(boost::unordered::piecewise_construct,
                                     boost::make_tuple(cid),
                                     boost::make_tuple(boost::cref(gatherer.dataGatherer().params()),
                                                       category.second,
                                                       gatherer.currentBucketStartTime(),
                                                       gatherer.bucketLength(),
                                                       gatherer.beginInfluencers(),
                                                       gatherer.endInfluencers())).first->second;
        entry.add(stat.s_Time,
                  (*stat.s_Values)[category.first],
                  stat.s_Count,
                  stat.s_SampleCount,
                  *stat.s_Influences);
    }
};

//! Updates gatherers with the start of a new bucket.
struct SStartNewBucket
{
    public:
        template<typename T>
        void operator()(const TCategorySizePr &/*category*/,
                        T &data,
                        core_t::TTime time) const
        {
            for (std::size_t pid = 0; pid < data.size(); ++pid)
            {
                this->startNewBucket(data[pid], time);
            }
        }

    private:
        template<typename T>
        inline void startNewBucket(T &data, core_t::TTime time) const
        {
            data.startNewBucket(time);
        }

        template<typename T>
        void startNewBucket(boost::unordered_map<std::size_t, T> &data,
                            core_t::TTime time) const
        {
            for (typename boost::unordered_map<std::size_t, T>::iterator i = data.begin();
                 i != data.end();
                 ++i)
            {
                i->second.startNewBucket(time);
            }
        }
};

//! Resets data stored for buckets containing a specified time.
struct SResetBucket
{
    public:
        template<typename T>
        void operator()(const TCategorySizePr &/*category*/,
                        T &data,
                        core_t::TTime bucketStart) const
        {
            for (std::size_t pid = 0; pid < data.size(); ++pid)
            {
                this->resetBucket(data[pid], bucketStart);
            }
        }

    private:
        template<typename T>
        inline void resetBucket(T &data, core_t::TTime bucketStart) const
        {
            data.resetBucket(bucketStart);
        }

        template<typename T>
        void resetBucket(boost::unordered_map<std::size_t, T> &data,
                         core_t::TTime bucketStart) const
        {
            for (typename boost::unordered_map<std::size_t, T>::iterator i = data.begin();
                 i != data.end();
                 ++i)
            {
                i->second.resetBucket(bucketStart);
            }
        }
};

} // unnamed::

CMetricBucketGatherer::CMetricBucketGatherer(CDataGatherer &dataGatherer,
                                             const std::string &summaryCountFieldName,
                                             const std::string &personFieldName,
                                             const std::string &attributeFieldName,
                                             const std::string &valueFieldName,
                                             const TStrVec &influenceFieldNames,
                                             core_t::TTime startTime) :
        CBucketGatherer(dataGatherer, startTime),
        m_ValueFieldName(valueFieldName),
        m_BeginInfluencingFields(0),
        m_BeginValueFields(0)
{
    this->initializeFieldNamesPart1(personFieldName, attributeFieldName, influenceFieldNames);
    this->initializeFieldNamesPart2(valueFieldName, summaryCountFieldName);
    this->initializeFeatureData();
}

CMetricBucketGatherer::CMetricBucketGatherer(CDataGatherer &dataGatherer,
                                             const std::string &summaryCountFieldName,
                                             const std::string &personFieldName,
                                             const std::string &attributeFieldName,
                                             const std::string &valueFieldName,
                                             const TStrVec &influenceFieldNames,
                                             core::CStateRestoreTraverser &traverser) :
        CBucketGatherer(dataGatherer, 0),
        m_ValueFieldName(valueFieldName),
        m_BeginValueFields(0)
{
    this->initializeFieldNamesPart1(personFieldName, attributeFieldName, influenceFieldNames);
    traverser.traverseSubLevel(boost::bind(&CMetricBucketGatherer::acceptRestoreTraverser, this, _1));
    this->initializeFieldNamesPart2(valueFieldName, summaryCountFieldName);
}

CMetricBucketGatherer::CMetricBucketGatherer(bool isForPersistence,
                                             const CMetricBucketGatherer &other) :
        CBucketGatherer(isForPersistence, other),
        m_ValueFieldName(other.m_ValueFieldName),
        m_FieldNames(other.m_FieldNames),
        m_BeginInfluencingFields(0),
        m_BeginValueFields(0),
        m_FeatureData(other.m_FeatureData)
{
    if (!isForPersistence)
    {
        LOG_ABORT("This constructor only creates clones for persistence");
    }
}

void CMetricBucketGatherer::acceptPersistInserter(core::CStatePersistInserter &inserter) const
{
    inserter.insertLevel(BASE_TAG, boost::bind(&CBucketGatherer::baseAcceptPersistInserter, this, _1));
    apply(m_DataGatherer.isPopulation(), m_FeatureData,
          boost::bind<void>(CPersistFeatureData(), _1, _2, boost::ref(inserter)));
}

bool CMetricBucketGatherer::acceptRestoreTraverser(core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        if (name == BASE_TAG)
        {
            if (traverser.traverseSubLevel(boost::bind(&CBucketGatherer::baseAcceptRestoreTraverser,
                                                       this, _1)) == false)
            {
                LOG_ERROR("Invalid data gatherer in " << traverser.value());
                return false;
            }
        }
        else if (this->acceptRestoreTraverserInternal(traverser) == false)
        {
            // Soldier on or we'll get a core dump later.
        }
    }
    while (traverser.next());

    return true;
}

bool CMetricBucketGatherer::acceptRestoreTraverserInternal(core::CStateRestoreTraverser &traverser)
{
    const std::string &name = traverser.name();
    if (name == MEAN_TAG)
    {
        CRestoreFeatureData<model_t::E_Mean> restore;
        if (restore(traverser, 1, m_DataGatherer.isPopulation(), *this, m_FeatureData) == false)
        {
            LOG_ERROR("Invalid mean data in " << traverser.value());
            return false;
        }
    }
    else if (name == MIN_TAG)
    {
        CRestoreFeatureData<model_t::E_Min> restore;
        if (restore(traverser, 1, m_DataGatherer.isPopulation(), *this, m_FeatureData) == false)
        {
            LOG_ERROR("Invalid min data in " << traverser.value());
            return false;
        }
    }
    else if (name == MAX_TAG)
    {
        CRestoreFeatureData<model_t::E_Max> restore;
        if (restore(traverser, 1, m_DataGatherer.isPopulation(), *this, m_FeatureData) == false)
        {
            LOG_ERROR("Invalid max data in " << traverser.value());
            return false;
        }
    }
    else if (name == SUM_TAG)
    {
        CRestoreFeatureData<model_t::E_Sum> restore;
        if (restore(traverser, 1, m_DataGatherer.isPopulation(), *this, m_FeatureData) == false)
        {
            LOG_ERROR("Invalid sum data in " << traverser.value());
            return false;
        }
    }
    else if (name == MEDIAN_TAG)
    {
        CRestoreFeatureData<model_t::E_Median> restore;
        if (restore(traverser, 1, m_DataGatherer.isPopulation(), *this, m_FeatureData) == false)
        {
            LOG_ERROR("Invalid median data in " << traverser.value());
            return false;
        }
    }
    else if (name == VARIANCE_TAG)
    {
        CRestoreFeatureData<model_t::E_Variance> restore;
        if (restore(traverser, 1, m_DataGatherer.isPopulation(), *this, m_FeatureData) == false)
        {
            LOG_ERROR("Invalid variance data in " << traverser.value());
            return false;
        }
    }
    else if (name.find(MULTIVARIATE_MEAN_TAG) != std::string::npos)
    {
        std::size_t dimension;
        if (core::CStringUtils::stringToType(name.substr(MULTIVARIATE_MEAN_TAG.length()),
                                             dimension) == false)
        {
            LOG_ERROR("Invalid dimension in " << name);
            return false;
        }
        CRestoreFeatureData<model_t::E_MultivariateMean> restore;
        if (restore(traverser, dimension, m_DataGatherer.isPopulation(), *this, m_FeatureData) == false)
        {
            LOG_ERROR("Invalid multivariate mean data in " << traverser.value());
            return false;
        }
    }
    else if (name.find(MULTIVARIATE_MIN_TAG) != std::string::npos)
    {
        std::size_t dimension;
        if (core::CStringUtils::stringToType(name.substr(MULTIVARIATE_MIN_TAG.length()),
                                             dimension) == false)
        {
            LOG_ERROR("Invalid dimension in " << name);
            return false;
        }
        CRestoreFeatureData<model_t::E_MultivariateMin> restore;
        if (restore(traverser, dimension, m_DataGatherer.isPopulation(), *this, m_FeatureData) == false)
        {
            LOG_ERROR("Invalid multivariate min data in " << traverser.value());
            return false;
        }
    }
    else if (name.find(MULTIVARIATE_MAX_TAG) != std::string::npos)
    {
        std::size_t dimension;
        if (core::CStringUtils::stringToType(name.substr(MULTIVARIATE_MAX_TAG.length()),
                                             dimension) == false)
        {
            LOG_ERROR("Invalid dimension in " << name);
            return false;
        }
        CRestoreFeatureData<model_t::E_MultivariateMax> restore;
        if (restore(traverser, dimension, m_DataGatherer.isPopulation(), *this, m_FeatureData) == false)
        {
            LOG_ERROR("Invalid multivariate max data in " << traverser.value());
            return false;
        }
    }

    return true;
}

CBucketGatherer *CMetricBucketGatherer::cloneForPersistence(void) const
{
    return new CMetricBucketGatherer(true, *this);
}

const std::string &CMetricBucketGatherer::persistenceTag(void) const
{
    return CBucketGatherer::METRIC_BUCKET_GATHERER_TAG;
}

const std::string &CMetricBucketGatherer::personFieldName(void) const
{
    return m_FieldNames[0];
}

const std::string &CMetricBucketGatherer::attributeFieldName(void) const
{
    return m_DataGatherer.isPopulation() ? m_FieldNames[1] : EMPTY_STRING;
}

const std::string &CMetricBucketGatherer::valueFieldName(void) const
{
    return m_ValueFieldName;
}

CMetricBucketGatherer::TStrVecCItr CMetricBucketGatherer::beginInfluencers(void) const
{
    return m_FieldNames.begin() + m_BeginInfluencingFields;
}

CMetricBucketGatherer::TStrVecCItr CMetricBucketGatherer::endInfluencers(void) const
{
    return m_FieldNames.begin() + m_BeginValueFields;
}

const TStrVec &CMetricBucketGatherer::fieldsOfInterest(void) const
{
    return m_FieldNames;
}

std::string CMetricBucketGatherer::description(void) const
{
    return function_t::name(function_t::function(m_DataGatherer.features()))
           + (m_ValueFieldName.empty() ? "" : " ") + m_ValueFieldName +
           + (byField(m_DataGatherer.isPopulation(), m_FieldNames).empty() ? "" : " by ")
           + byField(m_DataGatherer.isPopulation(), m_FieldNames)
           + (overField(m_DataGatherer.isPopulation(), m_FieldNames).empty() ? "" : " over ")
           + overField(m_DataGatherer.isPopulation(), m_FieldNames)
           + (m_DataGatherer.partitionFieldName().empty() ? "" : " partition=")
           + m_DataGatherer.partitionFieldName();
}

bool CMetricBucketGatherer::processFields(const TStrCPtrVec &fieldValues,
                                          CEventData &result,
                                          CResourceMonitor &resourceMonitor)
{
    typedef boost::optional<std::string> TOptionalStr;

    if (fieldValues.size() != m_FieldNames.size())
    {
        LOG_ERROR("Unexpected field values: "
                  << core::CContainerPrinter::print(fieldValues)
                  << ", for field names: "
                  << core::CContainerPrinter::print(m_FieldNames));
        return false;
    }

    const std::string *person = (fieldValues[0] == 0 && m_DataGatherer.useNull()) ?
                                &EMPTY_STRING :
                                fieldValues[0];
    if (person == 0)
    {
        // Just ignore: the "person" field wasn't present in the
        // record. Since all models in an aggregate share this
        // field we can't process this record further. Note that
        // we don't warn here since we'll permit a small fraction
        // of records to having missing field values.
        return false;
    }

    // The code below just ignores missing/invalid values. This
    // doesn't necessarily stop us processing the record by other
    // models so we don't return false.

    std::size_t i = m_BeginInfluencingFields;
    for (/**/; i < m_BeginValueFields; ++i)
    {
        result.addInfluence(fieldValues[i] ? TOptionalStr(*fieldValues[i]) : TOptionalStr());
    }
    if (m_DataGatherer.summaryMode() != model_t::E_None)
    {
        CEventData::TDouble1VecArraySizePr statistics;
        statistics.first.fill(TDouble1Vec(1, 0.0));
        if (m_DataGatherer.extractCountFromField(m_FieldNames[i],
                                                 fieldValues[i],
                                                 statistics.second) == false)
        {
            result.addValue();
            return true;
        }
        ++i;

        bool allOk = true;
        if (m_FieldNames.size() > statistics.first.size() + i)
        {
            LOG_ERROR("Inconsistency - more statistic field names than allowed "
                      << m_FieldNames.size() - i << " > " << statistics.first.size());
            allOk = false;
        }
        if (m_FieldNames.size() > m_FieldMetricCategories.size() + i)
        {
            LOG_ERROR("Inconsistency - more statistic field names than metric categories "
                      << m_FieldNames.size() - i << " > " << m_FieldMetricCategories.size());
            allOk = false;
        }
        for (std::size_t j = 0u; allOk && i < m_FieldNames.size(); ++i, ++j)
        {
            model_t::EMetricCategory category = m_FieldMetricCategories[j];
            if (   fieldValues[i] == 0
                || m_DataGatherer.extractMetricFromField(m_FieldNames[i],
                                                         *fieldValues[i],
                                                         statistics.first[category]) == false)
            {
                allOk = false;
            }
        }
        if (allOk)
        {
            if (statistics.second == CDataGatherer::EXPLICIT_NULL_SUMMARY_COUNT)
            {
                result.setExplicitNull();
            }
            else
            {
                result.addStatistics(statistics);
            }
        }
        else
        {
            result.addValue();
        }
    }
    else
    {
        TDouble1Vec value;
        if (   fieldValues[i] != 0
            && m_DataGatherer.extractMetricFromField(m_FieldNames[i], *fieldValues[i], value) == true)
        {
            result.addValue(value);
        }
        else
        {
            result.addValue();
        }
    }

    bool addedPerson = false;
    std::size_t pid = CDynamicStringIdRegistry::INVALID_ID;
    if (result.isExplicitNull())
    {
        m_DataGatherer.personId(*person, pid);
    }
    else
    {
        pid = m_DataGatherer.addPerson(*person, resourceMonitor, addedPerson);
    }

    if (pid == CDynamicStringIdRegistry::INVALID_ID)
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

    if (!result.person(pid))
    {
        LOG_ERROR("Bad by field value: " << *person);
        return false;
    }

    const std::string *attribute = (fieldValues[1] == 0 && m_DataGatherer.useNull()) ?
                                   &EMPTY_STRING :
                                   fieldValues[1];

    if (m_DataGatherer.isPopulation())
    {
        if (attribute == 0)
        {
            // Just ignore: the "by" field wasn't present in the
            // record. This doesn't necessarily stop us processing
            // the record by other models so we don't return false.
            // Note that we don't warn here since we'll permit a
            // small fraction of records to having missing field
            // values.
            result.addAttribute();
            result.addValue();
            return true;
        }

        bool addedAttribute = false;
        std::size_t cid = CDynamicStringIdRegistry::INVALID_ID;
        if (result.isExplicitNull())
        {
            m_DataGatherer.attributeId(*attribute, cid);
        }
        else
        {
            cid = m_DataGatherer.addAttribute(*attribute, resourceMonitor, addedAttribute);
        }
        result.addAttribute(cid);

        if (addedAttribute)
        {
            core::CStatistics::stat(stat_t::E_NumberByFields).increment();
        }
    }
    else
    {
        // Add the unique attribute.
        result.addAttribute(std::size_t(0));
    }

    return true;
}

void CMetricBucketGatherer::recyclePeople(const TSizeVec &peopleToRemove)
{
    if (peopleToRemove.empty())
    {
        return;
    }

    apply(m_DataGatherer.isPopulation(), m_FeatureData,
          boost::bind<void>(SRemovePeople(), _1, _2,
                            boost::cref(*this),
                            boost::cref(peopleToRemove)));

    this->CBucketGatherer::recyclePeople(peopleToRemove);
}

void CMetricBucketGatherer::removePeople(std::size_t lowestPersonToRemove)
{
    apply(m_DataGatherer.isPopulation(), m_FeatureData,
          boost::bind<void>(SRemovePeople(), _1, _2, lowestPersonToRemove));

    this->CBucketGatherer::removePeople(lowestPersonToRemove);
}

void CMetricBucketGatherer::recycleAttributes(const TSizeVec &attributesToRemove)
{
    if (attributesToRemove.empty())
    {
        return;
    }

    if (m_DataGatherer.isPopulation())
    {
        apply(m_DataGatherer.isPopulation(), m_FeatureData,
              boost::bind<void>(SRemoveAttributes(), _1, _2,
                                boost::cref(attributesToRemove)));
    }

    this->CBucketGatherer::recycleAttributes(attributesToRemove);
}

void CMetricBucketGatherer::removeAttributes(std::size_t lowestAttributeToRemove)
{
    if (m_DataGatherer.isPopulation())
    {
        apply(m_DataGatherer.isPopulation(), m_FeatureData,
              boost::bind<void>(SRemoveAttributes(), _1, _2,
                                lowestAttributeToRemove,
                                m_DataGatherer.numberAttributes()));
    }

    this->CBucketGatherer::removeAttributes(lowestAttributeToRemove);
}

uint64_t CMetricBucketGatherer::checksum(void) const
{
    uint64_t seed = this->CBucketGatherer::checksum();
    seed = maths::CChecksum::calculate(seed, m_DataGatherer.params().s_DecayRate);
    TStrCRefUInt64Map hashes;
    apply(m_DataGatherer.isPopulation(), m_FeatureData,
          boost::bind<void>(SHash(), _1, _2,
                            boost::cref(*this), boost::ref(hashes)));
    LOG_TRACE("seed = " << seed);
    LOG_TRACE("hashes = " << core::CContainerPrinter::print(hashes));
    return maths::CChecksum::calculate(seed, hashes);
}

void CMetricBucketGatherer::debugMemoryUsage(core::CMemoryUsage::TMemoryUsagePtr mem) const
{
    registerMemoryCallbacks();
    mem->setName("CMetricBucketGatherer");
    this->CBucketGatherer::debugMemoryUsage(mem->addChild());
    core::CMemoryDebug::dynamicSize("m_ValueFieldName", m_ValueFieldName, mem);
    core::CMemoryDebug::dynamicSize("m_FieldNames", m_FieldNames, mem);
    core::CMemoryDebug::dynamicSize("m_FieldMetricCategories", m_FieldMetricCategories, mem);
    core::CMemoryDebug::dynamicSize("m_FeatureData", m_FeatureData, mem);
}

std::size_t CMetricBucketGatherer::memoryUsage(void) const
{
    registerMemoryCallbacks();
    std::size_t mem = this->CBucketGatherer::memoryUsage();
    mem += core::CMemory::dynamicSize(m_ValueFieldName);
    mem += core::CMemory::dynamicSize(m_FieldNames);
    mem += core::CMemory::dynamicSize(m_FieldMetricCategories);
    mem += core::CMemory::dynamicSize(m_FeatureData);
    return mem;
}

std::size_t CMetricBucketGatherer::staticSize(void) const
{
    return sizeof(*this);
}

void CMetricBucketGatherer::clear(void)
{
    this->CBucketGatherer::clear();
    m_FeatureData.clear();
    this->initializeFeatureData();
}

bool CMetricBucketGatherer::resetBucket(core_t::TTime bucketStart)
{
    if (this->CBucketGatherer::resetBucket(bucketStart) == false)
    {
        return false;
    }
    apply(m_DataGatherer.isPopulation(), m_FeatureData,
          boost::bind<void>(SResetBucket(), _1, _2, bucketStart));
    return true;
}

void CMetricBucketGatherer::sample(core_t::TTime time)
{
    if (m_DataGatherer.sampleCounts())
    {
        apply(m_DataGatherer.isPopulation(), m_FeatureData,
              boost::bind<void>(SDoSample(), _1, _2,
                                time,
                                boost::cref(*this),
                                m_DataGatherer.sampleCounts()));
    }
    // Merge smallest bucket into longer buckets, if they exist
    this->CBucketGatherer::sample(time);
}

void CMetricBucketGatherer::featureData(core_t::TTime time, core_t::TTime bucketLength,
                                        TFeatureAnyPrVec &result) const
{
    result.clear();

    if (  !this->dataAvailable(time)
        || time >= this->currentBucketStartTime() + this->bucketLength())
    {
        LOG_DEBUG("No data available at " << time);
        return;
    }

    for (std::size_t i = 0u, n = m_DataGatherer.numberFeatures(); i < n; ++i)
    {
        model_t::EFeature feature = m_DataGatherer.feature(i);
        model_t::EMetricCategory category;
        if (model_t::metricCategory(feature, category))
        {
            std::size_t dimension = model_t::dimension(feature);
            TCategorySizePrAnyMapCItr begin =
                    m_FeatureData.find(std::make_pair(category, dimension));
            if (begin != m_FeatureData.end())
            {
                TCategorySizePrAnyMapCItr end = begin;
                ++end;
                apply(m_DataGatherer.isPopulation(), begin, end,
                      boost::bind<void>(SExtractFeatureData(), _1, _2,
                                        boost::cref(*this),
                                        feature, time, bucketLength,
                                        boost::ref(result)));
            }
            else
            {
                LOG_ERROR("No data for category " << model_t::print(category));
            }
        }
        else
        {
            LOG_ERROR("Unexpected feature " << model_t::print(feature));
        }
    }
}

void CMetricBucketGatherer::resize(std::size_t pid, std::size_t cid)
{
    if (m_DataGatherer.sampleCounts())
    {
        m_DataGatherer.sampleCounts()->resize(m_DataGatherer.isPopulation() ? cid : pid);
    }
    else
    {
        LOG_ERROR("Invalid sample counts for gatherer");
    }

    apply(m_DataGatherer.isPopulation(), m_FeatureData,
          boost::bind<void>(SResize(), _1, _2, pid, boost::cref(*this)));
}

void CMetricBucketGatherer::addValue(std::size_t pid,
                                     std::size_t cid,
                                     core_t::TTime time,
                                     const CEventData::TDouble1VecArray &values,
                                     std::size_t count,
                                     const CEventData::TOptionalStr &/*stringValue*/,
                                     const TStrPtrVec &influences)
{
    // Check that we are correctly sized - a person/attribute might have been added
    this->resize(pid, cid);

    SAddValue::SStatistic stat;
    stat.s_Time = time;
    stat.s_Values = &values;
    stat.s_Count = static_cast<unsigned int>(count);
    if (m_DataGatherer.sampleCounts())
    {
        stat.s_SampleCount = m_DataGatherer.sampleCounts()->count(m_DataGatherer.isPopulation() ? cid : pid);
    }
    else
    {
        LOG_ERROR("Invalid sample counts for gatherer");
        stat.s_SampleCount = 0.0;
    }

    stat.s_Influences = &influences;
    apply(m_DataGatherer.isPopulation(), m_FeatureData,
          boost::bind<void>(SAddValue(), _1, _2,
                            pid, cid,
                            boost::cref(*this),
                            boost::ref(stat)));
}

void CMetricBucketGatherer::startNewBucket(core_t::TTime time, bool skipUpdates)
{
    LOG_TRACE("StartNewBucket, " << time << " @ " << this);
    typedef std::vector<uint64_t> TUInt64Vec;
    typedef boost::unordered_map<std::size_t, TUInt64Vec> TSizeUInt64VecUMap;
    typedef TSizeUInt64VecUMap::iterator TSizeUInt64VecUMapItr;

    // Only update the sampleCounts if we are the primary bucket gatherer.
    // This is the only place where the bucket gatherer needs to know about its
    // status within the celestial plain, which is a bit ugly...
    if (!skipUpdates && time % this->bucketLength() == 0)
    {
        core_t::TTime earliestAvailableBucketStartTime = this->earliestBucketStartTime();
        if (this->dataAvailable(earliestAvailableBucketStartTime))
        {
            TSizeUInt64VecUMap counts;
            const TSizeSizePrUInt64UMap &counts_ = this->bucketCounts(earliestAvailableBucketStartTime);
            for (TSizeSizePrUInt64UMapCItr i = counts_.begin();
                 i != counts_.end();
                 ++i)
            {
                if (m_DataGatherer.isPopulation())
                {
                    counts[CDataGatherer::extractAttributeId(*i)].push_back(CDataGatherer::extractData(*i));
                }
                else
                {
                    counts.emplace(CDataGatherer::extractPersonId(*i),
                                   TUInt64Vec(1, 0)).first->second[0] += CDataGatherer::extractData(*i);
                }
            }
            double alpha = ::exp(-m_DataGatherer.params().s_DecayRate);

            for (TSizeUInt64VecUMapItr i = counts.begin(); i != counts.end(); ++i)
            {
                std::sort(i->second.begin(), i->second.end());
                std::size_t n = i->second.size() / 2;
                double median = i->second.size() % 2 == 0 ?
                                static_cast<double>(i->second[n - 1] + i->second[n]) / 2.0 :
                                static_cast<double>(i->second[n]);
                m_DataGatherer.sampleCounts()->updateMeanNonZeroBucketCount(i->first, median, alpha);
            }
            m_DataGatherer.sampleCounts()->refresh(m_DataGatherer);
        }
    }
    apply(m_DataGatherer.isPopulation(), m_FeatureData,
          boost::bind<void>(SStartNewBucket(), _1, _2, time));
}

void CMetricBucketGatherer::initializeFieldNamesPart1(const std::string &personFieldName,
                                                      const std::string &attributeFieldName,
                                                      const TStrVec &influenceFieldNames)
{
    switch (m_DataGatherer.summaryMode())
    {
    case model_t::E_None:
        m_FieldNames.reserve(  2
                             + static_cast<std::size_t>(m_DataGatherer.isPopulation())
                             + influenceFieldNames.size());
        m_FieldNames.push_back(personFieldName);
        if (m_DataGatherer.isPopulation()) m_FieldNames.push_back(attributeFieldName);
        m_BeginInfluencingFields = m_FieldNames.size();
        m_FieldNames.insert(m_FieldNames.end(),
                            influenceFieldNames.begin(),
                            influenceFieldNames.end());
        m_BeginValueFields = m_FieldNames.size();
        break;
    case model_t::E_Manual:
        m_FieldNames.reserve(  3
                             + static_cast<std::size_t>(m_DataGatherer.isPopulation())
                             + influenceFieldNames.size());
        m_FieldNames.push_back(personFieldName);
        if (m_DataGatherer.isPopulation()) m_FieldNames.push_back(attributeFieldName);
        m_BeginInfluencingFields = m_FieldNames.size();
        m_FieldNames.insert(m_FieldNames.end(),
                            influenceFieldNames.begin(),
                            influenceFieldNames.end());
        m_BeginValueFields = m_FieldNames.size();
        break;
    };
}

void CMetricBucketGatherer::initializeFieldNamesPart2(const std::string &valueFieldName,
                                                      const std::string &summaryCountFieldName)
{
    switch (m_DataGatherer.summaryMode())
    {
    case model_t::E_None:
        m_FieldNames.push_back(valueFieldName);
        break;
    case model_t::E_Manual:
        m_FieldNames.push_back(summaryCountFieldName);
        m_FieldNames.push_back(valueFieldName);
        m_DataGatherer.determineMetricCategory(m_FieldMetricCategories);
        break;
    };
}

void CMetricBucketGatherer::initializeFeatureData(void)
{
#define INITIALIZE(category) if (m_DataGatherer.isPopulation())                                                           \
                             {                                                                                            \
                                 m_FeatureData[std::make_pair(category, dimension)] = SDataType<category, true>::Type();  \
                             }                                                                                            \
                             else                                                                                         \
                             {                                                                                            \
                                 m_FeatureData[std::make_pair(category, dimension)] = SDataType<category, false>::Type(); \
                             }

    for (std::size_t i = 0u, n = m_DataGatherer.numberFeatures(); i < n; ++i)
    {
        const model_t::EFeature feature = m_DataGatherer.feature(i);
        model_t::EMetricCategory category;
        if (model_t::metricCategory(feature, category))
        {
            std::size_t dimension = model_t::dimension(feature);
            switch (category)
            {
            case model_t::E_Mean:             INITIALIZE(model_t::E_Mean);             break;
            case model_t::E_Median:           INITIALIZE(model_t::E_Median);           break;
            case model_t::E_Min:              INITIALIZE(model_t::E_Min);              break;
            case model_t::E_Max:              INITIALIZE(model_t::E_Max);              break;
            case model_t::E_Variance:         INITIALIZE(model_t::E_Variance);         break;
            case model_t::E_Sum:              INITIALIZE(model_t::E_Sum);              break;
            case model_t::E_MultivariateMean: INITIALIZE(model_t::E_MultivariateMean); break;
            case model_t::E_MultivariateMin:  INITIALIZE(model_t::E_MultivariateMin);  break;
            case model_t::E_MultivariateMax:  INITIALIZE(model_t::E_MultivariateMax);  break;
            }
        }
        else
        {
            LOG_ERROR("Unexpected feature = " << model_t::print(m_DataGatherer.feature(i)));
        }
    }

#undef INITIALIZE
}

}
}
