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

#include "CMetricPopulationModelTest.h"

#include <core/CContainerPrinter.h>
#include <core/CIEEE754.h>
#include <core/CLogger.h>
#include <core/CRapidXmlParser.h>
#include <core/CRapidXmlStatePersistInserter.h>
#include <core/CRapidXmlStateRestoreTraverser.h>
#include <core/CStringUtils.h>

#include <maths/CBasicStatistics.h>
#include <maths/CModelWeight.h>
#include <maths/COrderings.h>
#include <maths/CPrior.h>
#include <maths/CSampling.h>

#include <model/CAnnotatedProbabilityBuilder.h>
#include <model/CDataGatherer.h>
#include <model/CEventData.h>
#include <model/CModelConfig.h>
#include <model/CMetricPopulationModel.h>
#include <model/CMetricPopulationModelFactory.h>
#include <model/CModelDetailsView.h>
#include <model/CPartitioningFields.h>
#include <model/CResourceMonitor.h>

#include <test/CRandomNumbers.h>

#include <boost/range.hpp>

#include <algorithm>
#include <cstddef>
#include <string>
#include <utility>
#include <vector>

using namespace ml;
using namespace model;

namespace
{

typedef std::pair<std::size_t, std::size_t> TSizeSizePr;
typedef std::vector<TSizeSizePr> TSizeSizePrVec;
typedef std::vector<TSizeSizePrVec> TSizeSizePrVecVec;
typedef std::pair<double, double> TDoubleDoublePr;
typedef std::vector<TDoubleDoublePr> TDoubleDoublePrVec;
typedef std::pair<double, std::string> TDoubleStrPr;
typedef std::vector<TDoubleStrPr> TDoubleStrPrVec;
typedef std::vector<std::string> TStrVec;
typedef std::vector<unsigned int> TUIntVec;
typedef std::vector<double> TDoubleVec;
typedef std::vector<TDoubleVec> TDoubleVecVec;
typedef std::vector<std::size_t> TSizeVec;
typedef std::vector<TSizeVec> TSizeVecVec;
typedef maths::CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;
typedef maths::CBasicStatistics::COrderStatisticsStack<double, 1u> TMinAccumulator;
typedef maths::CBasicStatistics::COrderStatisticsStack<double, 1u, std::greater<double> > TMaxAccumulator;
typedef core::CSmallVector<double, 1> TDouble1Vec;

const std::string EMPTY_STRING;

struct SAnomaly
{
    SAnomaly(void) : s_Bucket(0u), s_Person(), s_Attributes() {}
    SAnomaly(std::size_t bucket,
             const std::string &person,
             const TDoubleStrPrVec &attributes) :
            s_Bucket(bucket),
            s_Person(person),
            s_Attributes(attributes)
    {
    }

    std::size_t s_Bucket;
    std::string s_Person;
    TDoubleStrPrVec s_Attributes;

    bool operator<(const SAnomaly &other) const
    {
        return s_Bucket < other.s_Bucket;
    }

    std::string print(void) const
    {
        std::ostringstream result;
        result << "[" << s_Bucket << ", " + s_Person << ",";
        for (std::size_t i = 0u; i < s_Attributes.size(); ++i)
        {
            if (s_Attributes[i].first < 0.01)
            {
                result << " " << s_Attributes[i].second;
            }
        }
        result << "]";
        return result.str();
    }
};

struct SMessage
{
    SMessage(core_t::TTime time,
             const std::string &person,
             const std::string &attribute,
             const TDouble1Vec &value) :
                 s_Time(time),
                 s_Person(person),
                 s_Attribute(attribute),
                 s_Value(value)
    {
    }

    bool operator<(const SMessage &other) const
    {
        return maths::COrderings::lexicographical_compare(s_Time,
                                                          s_Person,
                                                          s_Attribute,
                                                          other.s_Time,
                                                          other.s_Person,
                                                          other.s_Attribute);
    }

    core_t::TTime s_Time;
    std::string s_Person;
    std::string s_Attribute;
    TDouble1Vec s_Value;
};

typedef std::vector<SMessage> TMessageVec;

const std::size_t numberAttributes = 5u;
const std::size_t numberPeople = 10u;

double roundToNearestPersisted(double value)
{
    std::string valueAsString(core::CStringUtils::typeToStringPrecise(value, core::CIEEE754::E_DoublePrecision));
    double result = 0.0;
    core::CStringUtils::stringToType(valueAsString, result);
    return result;
}

void generateTestMessages(std::size_t dimension,
                          core_t::TTime startTime,
                          core_t::TTime bucketLength,
                          TMessageVec &messages)
{
    // The test case is as follows:
    //
    //      attribute    |    0    |    1    |    2    |    3    |    4
    // ------------------+---------+---------+---------+---------+--------
    //        rate       |   10    |    2    |   15    |    2    |    1
    // ------------------+---------+---------+---------+---------+--------
    //        mean       |    5    |   10    |    7    |    3    |   15
    // ------------------+---------+---------+---------+---------+--------
    //     variance      |    1    |   0.5   |    2    |   0.1   |    4
    // ------------------+---------+---------+---------+---------+--------
    //  metric anomaly   | (12,2), |    -    | (30,5), | (12,2), | (60,2)
    //  (bucket, people) | (15,3), |         | (44,9)  | (80,1)  |
    //                   | (40,6)  |         |         |         |
    //
    // There are 10 people, 4 attributes and 100 buckets.

    const std::size_t numberBuckets = 100u;

    TStrVec people;
    for (std::size_t i = 0u; i < numberPeople; ++i)
    {
        people.push_back("p" + core::CStringUtils::typeToString(i));
    }
    LOG_DEBUG("people = " << core::CContainerPrinter::print(people));

    TStrVec attributes;
    for (std::size_t i = 0u; i < numberAttributes; ++i)
    {
        attributes.push_back("c" + core::CStringUtils::typeToString(i));
    }
    LOG_DEBUG("attributes = " << core::CContainerPrinter::print(attributes));

    double attributeRates[] = { 10.0, 2.0, 15.0, 2.0, 1.0 };
    double means[] = { 5.0, 10.0, 7.0, 3.0, 15.0 };
    double variances[] = { 1.0, 0.5, 2.0, 0.1, 4.0 };

    TSizeSizePr attribute0AnomalyBucketPerson[] =
       {
           TSizeSizePr(40u, 6u),
           TSizeSizePr(15u, 3u),
           TSizeSizePr(12u, 2u)
       };
    TSizeSizePr attribute2AnomalyBucketPerson[] =
       {
           TSizeSizePr(44u, 9u),
           TSizeSizePr(30u, 5u)
       };
    TSizeSizePr attribute3AnomalyBucketPerson[] =
       {
           TSizeSizePr(80u, 1u),
           TSizeSizePr(12u, 2u)
       };
    TSizeSizePr attribute4AnomalyBucketPerson[] =
       {
           TSizeSizePr(60u, 2u)
       };

    TSizeSizePrVecVec anomalies;
    anomalies.push_back(TSizeSizePrVec(boost::begin(attribute0AnomalyBucketPerson),
                                       boost::end(attribute0AnomalyBucketPerson)));
    anomalies.push_back(TSizeSizePrVec());
    anomalies.push_back(TSizeSizePrVec(boost::begin(attribute2AnomalyBucketPerson),
                                       boost::end(attribute2AnomalyBucketPerson)));
    anomalies.push_back(TSizeSizePrVec(boost::begin(attribute3AnomalyBucketPerson),
                                       boost::end(attribute3AnomalyBucketPerson)));
    anomalies.push_back(TSizeSizePrVec(boost::begin(attribute4AnomalyBucketPerson),
                                       boost::end(attribute4AnomalyBucketPerson)));

    test::CRandomNumbers rng;

    for (std::size_t i = 0u; i < numberBuckets; ++i, startTime += bucketLength)
    {
        for (std::size_t j = 0u; j < numberAttributes; ++j)
        {
            TUIntVec samples;
            rng.generatePoissonSamples(attributeRates[j], numberPeople, samples);

            for (std::size_t k = 0u; k < numberPeople; ++k)
            {
                bool anomaly =   !anomalies[j].empty()
                               && anomalies[j].back().first == i
                               && anomalies[j].back().second == k;
                if (anomaly)
                {
                    samples[k] += 4;
                    anomalies[j].pop_back();
                }

                if (samples[k] == 0)
                {
                    continue;
                }

                TDoubleVec values;
                rng.generateNormalSamples(means[j], variances[j], dimension * samples[k], values);

                for (std::size_t l = 0u; l < values.size(); l += dimension)
                {
                    TDouble1Vec value(dimension);
                    for (std::size_t d = 0u; d < dimension; ++d)
                    {
                        double vd = values[l + d];
                        if (anomaly && (l % (2 * dimension)) == 0)
                        {
                            vd += 6.0 * ::sqrt(variances[j]);
                        }
                        value[d] = roundToNearestPersisted(vd);
                    }
                    core_t::TTime dt =  (static_cast<core_t::TTime>(l) * bucketLength)
                                       / static_cast<core_t::TTime>(values.size());
                    messages.push_back(SMessage(startTime + dt, people[k], attributes[j], value));
                }
            }
        }
    }

    LOG_DEBUG("# messages = " << messages.size());
    std::sort(messages.begin(), messages.end());
}

void dummyAnyToPersistInserter(const std::string &,
                               const boost::any &,
                               core::CStatePersistInserter &)
{
    // NO-OP
}

boost::any dummyRestoreFunc(core::CStateRestoreTraverser &)
{
    return boost::any();
}

std::string valueAsString(const TDouble1Vec &value)
{
    std::string result = core::CStringUtils::typeToStringPrecise(value[0], core::CIEEE754::E_DoublePrecision);
    for (std::size_t i = 1u; i < value.size(); ++i)
    {
        result +=  CModelConfig::DEFAULT_MULTIVARIATE_COMPONENT_DELIMITER
                 + core::CStringUtils::typeToStringPrecise(value[i], core::CIEEE754::E_DoublePrecision);
    }
    return result;
}

CEventData addArrival(const SMessage &message,
                      const CModelFactory::TDataGathererPtr &gatherer)
{
    CDataGatherer::TStrCPtrVec fields;
    fields.push_back(&message.s_Person);
    fields.push_back(&message.s_Attribute);
    std::string value = valueAsString(message.s_Value);
    fields.push_back(&value);
    CResourceMonitor resourceMonitor;

    CEventData result;
    result.time(message.s_Time);

    gatherer->addArrival(fields, result, resourceMonitor);

    return result;
}

void processBucket(core_t::TTime time,
                   core_t::TTime bucketLength,
                   std::size_t n,
                   const double *bucket,
                   const std::string *influencerValues,
                   CDataGatherer &gatherer,
                   CMetricPopulationModel &model,
                   SAnnotatedProbability &probability)
{
    CResourceMonitor resourceMonitor;
    const std::string person("p");
    const std::string attribute("a");
    for (std::size_t i = 0u; i < n; ++i)
    {
        //addArrival(gatherer, time, "p", bucket[i], TOptionalStr(influencerValues[i]));
        CDataGatherer::TStrCPtrVec fieldValues;
        fieldValues.push_back(&person);
        fieldValues.push_back(&attribute);
        fieldValues.push_back(&influencerValues[i]);
        std::string valueAsString(core::CStringUtils::typeToStringPrecise(
                                    bucket[i],
                                    core::CIEEE754::E_DoublePrecision));
        fieldValues.push_back(&valueAsString);

        CEventData eventData;
        eventData.time(time);

        gatherer.addArrival(fieldValues, eventData, resourceMonitor);
    }
    model.sample(time, time + bucketLength, resourceMonitor);
    CPartitioningFields partitioningFields(EMPTY_STRING, EMPTY_STRING);
    model.computeProbability(0/*pid*/, time, time + bucketLength, partitioningFields, 1, probability);
    LOG_DEBUG("influences = " << core::CContainerPrinter::print(probability.s_Influences));
}

}

void CMetricPopulationModelTest::testBasicAccessors(void)
{
    LOG_DEBUG("*** CMetricPopulationModelTest::testBasicAccessors ***");

    // Check that the correct data is read retrieved by the
    // basic model accessors.

    typedef boost::optional<uint64_t> TOptionalUInt64;
    typedef std::map<std::string, uint64_t> TStrUInt64Map;
    typedef TStrUInt64Map::const_iterator TStrUInt64MapCItr;
    typedef std::vector<TMeanAccumulator> TMeanAccumulatorVec;
    typedef std::vector<TMinAccumulator> TMinAccumulatorVec;
    typedef std::vector<TMaxAccumulator> TMaxAccumulatorVec;

    core_t::TTime startTime = 1367280000;
    const core_t::TTime bucketLength = 3600;

    TMessageVec messages;
    generateTestMessages(1, startTime, bucketLength, messages);
    LOG_DEBUG("# messages = " << messages.size());

    SModelParams params(bucketLength);
    CMetricPopulationModelFactory factory(params);
    CModelFactory::TFeatureVec features;
    features.push_back(model_t::E_PopulationMeanByPersonAndAttribute);
    features.push_back(model_t::E_PopulationMinByPersonAndAttribute);
    features.push_back(model_t::E_PopulationMaxByPersonAndAttribute);
    factory.features(features);
    CModelFactory::SGathererInitializationData gathererInitData(startTime);
    CModelFactory::TDataGathererPtr gatherer(dynamic_cast<CDataGatherer*>(
                                                 factory.makeDataGatherer(gathererInitData)));
    CModelFactory::SModelInitializationData modelInitData(gatherer);
    CModel::TModelPtr model(factory.makeModel(modelInitData));

    CPPUNIT_ASSERT_EQUAL(model_t::E_MetricOnline, model->category());
    CResourceMonitor resourceMonitor;

    TStrUInt64Map expectedBucketPersonCounts;
    TMeanAccumulatorVec expectedBucketMeans(numberPeople * numberAttributes);
    TMinAccumulatorVec expectedBucketMins(numberPeople * numberAttributes);
    TMaxAccumulatorVec expectedBucketMaxs(numberPeople * numberAttributes);

    for (std::size_t i = 0u; i < messages.size(); ++i)
    {
        if (messages[i].s_Time >= startTime + bucketLength)
        {
            model->sample(startTime, startTime + bucketLength, resourceMonitor);

            LOG_DEBUG("Testing bucket = [" << startTime
                      << "," << startTime + bucketLength << ")");

            CPPUNIT_ASSERT_EQUAL(numberPeople, gatherer->numberActivePeople());
            CPPUNIT_ASSERT_EQUAL(numberAttributes, gatherer->numberActiveAttributes());

            // Test the person and attribute invariants.
            for (std::size_t j = 0u; j < gatherer->numberActivePeople(); ++j)
            {
                const std::string &name = model->personName(j);
                std::size_t pid;
                CPPUNIT_ASSERT(gatherer->personId(name, pid));
                CPPUNIT_ASSERT_EQUAL(j, pid);
            }
            for (std::size_t j = 0u; j < gatherer->numberActiveAttributes(); ++j)
            {
                const std::string &name = model->attributeName(j);
                std::size_t cid;
                CPPUNIT_ASSERT(gatherer->attributeId(name, cid));
                CPPUNIT_ASSERT_EQUAL(j, cid);
            }

            LOG_DEBUG("expected counts = "
                      << core::CContainerPrinter::print(expectedBucketPersonCounts));

            TSizeVec expectedCurrentBucketPersonIds;

            // Test the person counts.
            for (TStrUInt64MapCItr countItr = expectedBucketPersonCounts.begin();
                 countItr != expectedBucketPersonCounts.end();
                 ++countItr)
            {
                std::size_t pid;
                CPPUNIT_ASSERT(gatherer->personId(countItr->first, pid));
                expectedCurrentBucketPersonIds.push_back(pid);
                TOptionalUInt64 count = model->currentBucketCount(pid, startTime);
                CPPUNIT_ASSERT(count);
                CPPUNIT_ASSERT_EQUAL(countItr->second, *count);
            }

            std::sort(expectedCurrentBucketPersonIds.begin(),
                      expectedCurrentBucketPersonIds.end());

            TSizeVec bucketPersonIds;
            model->currentBucketPersonIds(startTime, bucketPersonIds);

            CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(expectedCurrentBucketPersonIds),
                                 core::CContainerPrinter::print(bucketPersonIds));

            LOG_DEBUG("expected means = "
                      << core::CContainerPrinter::print(expectedBucketMeans));
            LOG_DEBUG("expected mins = "
                      << core::CContainerPrinter::print(expectedBucketMins));
            LOG_DEBUG("expected maxs = "
                      << core::CContainerPrinter::print(expectedBucketMaxs));

            for (std::size_t cid = 0u; cid < numberAttributes; ++cid)
            {
                for (std::size_t pid = 0u; pid < numberPeople; ++pid)
                {
                    const TMeanAccumulator &expectedMean =
                            expectedBucketMeans[pid * numberAttributes + cid];
                    const TMinAccumulator &expectedMin =
                            expectedBucketMins[pid * numberAttributes + cid];
                    const TMaxAccumulator &expectedMax =
                            expectedBucketMaxs[pid * numberAttributes + cid];

                    TDouble1Vec mean = model->currentBucketValue(
                                              model_t::E_PopulationMeanByPersonAndAttribute,
                                              pid, cid,
                                              startTime);
                    TDouble1Vec min  = model->currentBucketValue(
                                              model_t::E_PopulationMinByPersonAndAttribute,
                                              pid, cid,
                                              startTime);
                    TDouble1Vec max  = model->currentBucketValue(
                                              model_t::E_PopulationMaxByPersonAndAttribute,
                                              pid, cid,
                                              startTime);

                    CPPUNIT_ASSERT(   (!mean.empty() && maths::CBasicStatistics::count(expectedMean) > 0.0)
                                   || ( mean.empty() && maths::CBasicStatistics::count(expectedMean) == 0.0));
                    if (!mean.empty())
                    {
                        CPPUNIT_ASSERT_EQUAL(maths::CBasicStatistics::mean(expectedMean), mean[0]);
                    }
                    CPPUNIT_ASSERT(   (!min.empty() && expectedMin.count() > 0.0)
                                   || ( min.empty() && expectedMin.count() == 0.0));
                    if (!min.empty())
                    {
                        CPPUNIT_ASSERT_EQUAL(expectedMin[0], min[0]);
                    }
                    CPPUNIT_ASSERT(   (!max.empty() && expectedMax.count() > 0.0)
                                   || ( max.empty() && expectedMax.count() == 0.0));
                    if (!max.empty())
                    {
                        CPPUNIT_ASSERT_EQUAL(expectedMax[0], max[0]);
                    }
                }
            }

            expectedBucketMeans = TMeanAccumulatorVec(numberPeople * numberAttributes);
            expectedBucketMins = TMinAccumulatorVec(numberPeople * numberAttributes);
            expectedBucketMaxs = TMaxAccumulatorVec(numberPeople * numberAttributes);
            expectedBucketPersonCounts.clear();
            startTime += bucketLength;
        }

        const SMessage &m = messages[i];
        CEventData eventData = addArrival(m, gatherer);
        std::size_t pid = *eventData.personId();
        std::size_t cid = *eventData.attributeId();
        ++expectedBucketPersonCounts[m.s_Person];
        expectedBucketMeans[pid * numberAttributes + cid].add(m.s_Value[0]);
        expectedBucketMins[pid * numberAttributes + cid].add(m.s_Value[0]);
        expectedBucketMaxs[pid * numberAttributes + cid].add(m.s_Value[0]);
    }
}

void CMetricPopulationModelTest::testMinMaxAndMean(void)
{
    LOG_DEBUG("*** testMinMaxAndMean ***");

    // We check that the correct data is read from the gatherer
    // into the model on sample.

    typedef std::map<TSizeSizePr, TDoubleVec> TSizeSizePrDoubleVecMap;
    typedef TSizeSizePrDoubleVecMap::const_iterator TSizeSizePrDoubleVecMapCItr;
    typedef std::map<TSizeSizePr, TMeanAccumulator> TSizeSizePrMeanAccumulatorUMap;
    typedef std::map<TSizeSizePr, TMinAccumulator> TSizeSizePrMinAccumulatorMap;
    typedef std::map<TSizeSizePr, TMaxAccumulator> TSizeSizePrMaxAccumulatorMap;
    typedef boost::shared_ptr<maths::CPrior> TPriorPtr;
    typedef std::map<std::size_t, TPriorPtr> TSizePriorPtrMap;
    typedef std::pair<TDoubleVec, TDoubleVecVec> TDoubleVecDoubleVecVecPr;
    typedef std::map<std::size_t, TDoubleVecDoubleVecVecPr> TSizeDoubleVecDoubleVecVecPrMap;
    typedef TSizeDoubleVecDoubleVecVecPrMap::iterator TSizeDoubleVecDoubleVecVecPrMapItr;
    typedef std::map<std::size_t, TSizeDoubleVecDoubleVecVecPrMap> TSizeSizeDoubleVecDoubleVecVecPrMapMap;
    typedef TSizeSizeDoubleVecDoubleVecVecPrMapMap::iterator TSizeSizeDoubleVecDoubleVecVecPrMapMapItr;

    static const maths_t::TWeightStyleVec COUNT_WEIGHT(1, maths_t::E_SampleCountWeight);

    core_t::TTime startTime = 1367280000;
    const core_t::TTime bucketLength = 3600;

    TMessageVec messages;
    generateTestMessages(1, startTime, bucketLength, messages);

    SModelParams params(bucketLength);
    params.s_MaximumUpdatesPerBucket = 0.0;
    CMetricPopulationModelFactory factory(params);
    CModelFactory::TFeatureVec features;
    features.push_back(model_t::E_PopulationMeanByPersonAndAttribute);
    features.push_back(model_t::E_PopulationMinByPersonAndAttribute);
    features.push_back(model_t::E_PopulationMaxByPersonAndAttribute);
    factory.features(features);
    CModelFactory::SGathererInitializationData gathererInitData(startTime);
    CModelFactory::TDataGathererPtr gatherer(dynamic_cast<CDataGatherer*>(factory.makeDataGatherer(gathererInitData)));
    CModelFactory::SModelInitializationData modelInitData(gatherer);
    CModel::TModelPtr modelHolder(factory.makeModel(modelInitData));
    CMetricPopulationModel *model = dynamic_cast<CMetricPopulationModel*>(modelHolder.get());

    CResourceMonitor resourceMonitor;
    CModelFactory::TFeaturePriorPtrPrVec priors = factory.defaultPriors(features);
    CPPUNIT_ASSERT_EQUAL(features.size(), priors.size());
    CPPUNIT_ASSERT_EQUAL(features[0], priors[0].first);
    CPPUNIT_ASSERT_EQUAL(features[1], priors[1].first);
    CPPUNIT_ASSERT_EQUAL(features[2], priors[2].first);

    TSizeSizePrMeanAccumulatorUMap sampleMeanAccumulators;
    TSizeSizePrMinAccumulatorMap sampleMinAccumulators;
    TSizeSizePrMaxAccumulatorMap sampleMaxAccumulators;

    TSizeSizePrDoubleVecMap expectedSamples[] =
        {
            TSizeSizePrDoubleVecMap(),
            TSizeSizePrDoubleVecMap(),
            TSizeSizePrDoubleVecMap()
        };
    TSizePriorPtrMap expectedPopulationPriors[] =
        {
            TSizePriorPtrMap(),
            TSizePriorPtrMap(),
            TSizePriorPtrMap()
        };

    for (std::size_t i = 0u; i < messages.size(); ++i)
    {
        if (messages[i].s_Time >= startTime + bucketLength)
        {
            LOG_DEBUG("Updating bucket = [" << startTime
                      << "," << startTime + bucketLength << ")");

            model->sample(startTime, startTime + bucketLength, resourceMonitor);

            TSizeSizeDoubleVecDoubleVecVecPrMapMap populationWeightedSamples;
            for (std::size_t j = 0u; j < features.size(); ++j)
            {
                for (TSizeSizePrDoubleVecMapCItr itr = expectedSamples[j].begin();
                     itr != expectedSamples[j].end();
                     ++itr)
                {
                    std::size_t pid = itr->first.first;
                    std::size_t cid = itr->first.second;
                    TDoubleVecVec w(itr->second.size(), TDoubleVec(1, model->sampleRateWeight(pid, cid)));
                    TDoubleVec &samples = populationWeightedSamples[j][cid].first;
                    TDoubleVecVec &weights = populationWeightedSamples[j][cid].second;
                    samples.insert(samples.end(), itr->second.begin(), itr->second.end());
                    weights.insert(weights.end(), w.begin(), w.end());
                }
            }
            for (TSizeSizeDoubleVecDoubleVecVecPrMapMapItr itr = populationWeightedSamples.begin();
                 itr != populationWeightedSamples.end();
                 ++itr)
            {
                std::size_t j = itr->first;
                for (TSizeDoubleVecDoubleVecVecPrMapItr attributeItr = itr->second.begin();
                     attributeItr != itr->second.end();
                     ++attributeItr)
                {
                    std::size_t cid = attributeItr->first;
                    TDoubleVec &samples = attributeItr->second.first;
                    TDoubleVecVec &weights = attributeItr->second.second;
                    maths::COrderings::simultaneousSort(samples, weights);
                    TPriorPtr &populationPrior = expectedPopulationPriors[j][cid];
                    if (!populationPrior)
                    {
                        populationPrior = factory.defaultPrior(features[j]);
                    }
                    populationPrior->addSamples(COUNT_WEIGHT, samples, weights);
                }
            }

            LOG_DEBUG("Testing bucket = [" << startTime
                      << "," << startTime + bucketLength << ")");

            for (std::size_t j = 0u; j < features.size(); ++j)
            {
                LOG_DEBUG("Testing feature " << model_t::print(features[j]));

                for (std::size_t cid = 0u; cid < numberAttributes; ++cid)
                {
                    if (expectedPopulationPriors[j].count(cid) == 0)
                    {
                        continue;
                    }

                    LOG_DEBUG("Testing priors for " << model->attributeName(cid));
                    const maths::CPrior *populationPrior = model->details()->prior(features[j], cid);
                    CPPUNIT_ASSERT(populationPrior);
                    CPPUNIT_ASSERT_EQUAL(expectedPopulationPriors[j][cid]->checksum(),
                                         populationPrior->checksum());
                }
            }

            expectedSamples[0].clear();
            expectedSamples[1].clear();
            expectedSamples[2].clear();
            startTime += bucketLength;
        }

        const SMessage &m = messages[i];
        CEventData eventData = addArrival(m, gatherer);
        std::size_t pid = *eventData.personId();
        std::size_t cid = *eventData.attributeId();

        double sampleCount = gatherer->sampleCount(cid);
        if (sampleCount > 0.0)
        {
            TSizeSizePr key(pid, cid);
            sampleMeanAccumulators[key].add(m.s_Value[0]);
            sampleMinAccumulators[key].add(m.s_Value[0]);
            sampleMaxAccumulators[key].add(m.s_Value[0]);
            if (maths::CBasicStatistics::count(sampleMeanAccumulators[key]) == sampleCount)
            {
                expectedSamples[0][key].push_back(maths::CBasicStatistics::mean(sampleMeanAccumulators[key]));
                expectedSamples[1][key].push_back(sampleMinAccumulators[key][0]);
                expectedSamples[2][key].push_back(sampleMaxAccumulators[key][0]);
                sampleMeanAccumulators[key] = TMeanAccumulator();
                sampleMinAccumulators[key] = TMinAccumulator();
                sampleMaxAccumulators[key] = TMaxAccumulator();
            }
        }
    }
}

void CMetricPopulationModelTest::testLatLong(void)
{
    LOG_DEBUG("*** testLatLong ***");

    typedef std::map<TSizeSizePr, TDoubleVecVec> TSizeSizePrDoubleVecVecMap;
    typedef TSizeSizePrDoubleVecVecMap::const_iterator TSizeSizePrDoubleVecVecMapCItr;
    typedef maths::CVectorNx1<double, 2> TVector2;
    typedef maths::CBasicStatistics::SSampleMean<TVector2>::TAccumulator TMean2Accumulator;
    typedef std::map<TSizeSizePr, TMean2Accumulator> TSizeSizePrMean2AccumulatorUMap;
    typedef boost::shared_ptr<maths::CMultivariatePrior> TMultivariatePriorPtr;
    typedef std::map<std::size_t, TMultivariatePriorPtr> TSizeMultivariatePriorPtrMap;
    typedef std::vector<TDoubleVecVec> TDoubleVecVecVec;
    typedef std::pair<TDoubleVecVec, TDoubleVecVecVec> TDoubleVecVecDoubleVecVecVecPr;
    typedef std::map<std::size_t, TDoubleVecVecDoubleVecVecVecPr> TSizeDoubleVecVecDoubleVecVecVecPrMap;
    typedef TSizeDoubleVecVecDoubleVecVecVecPrMap::iterator TSizeDoubleVecVecDoubleVecVecVecPrMapItr;

    static const maths_t::TWeightStyleVec COUNT_WEIGHT(1, maths_t::E_SampleCountWeight);

    maths::CSampling::CScopeMockRandomNumberGenerator scopeMockRng;

    core_t::TTime startTime = 1367280000;
    const core_t::TTime bucketLength = 3600;

    TMessageVec messages;
    generateTestMessages(2, startTime, bucketLength, messages);

    SModelParams params(bucketLength);
    params.s_MaximumUpdatesPerBucket = 0.0;
    CMetricPopulationModelFactory factory(params);
    CModelFactory::TFeatureVec features;
    features.push_back(model_t::E_PopulationMeanLatLongByPersonAndAttribute);
    factory.features(features);
    CModelFactory::SGathererInitializationData gathererInitData(startTime);
    CModelFactory::TDataGathererPtr gatherer(dynamic_cast<CDataGatherer*>(factory.makeDataGatherer(gathererInitData)));
    CModelFactory::SModelInitializationData modelInitData(gatherer);
    CModel::TModelPtr modelHolder(factory.makeModel(modelInitData));
    CMetricPopulationModel *model = dynamic_cast<CMetricPopulationModel*>(modelHolder.get());

    CResourceMonitor resourceMonitor;
    CModelFactory::TFeatureMultivariatePriorPtrPrVec priors = factory.defaultMultivariatePriors(features);
    CPPUNIT_ASSERT_EQUAL(features.size(), priors.size());
    CPPUNIT_ASSERT_EQUAL(features[0], priors[0].first);

    TSizeSizePrMean2AccumulatorUMap sampleMeanAccumulators;
    TSizeSizePrDoubleVecVecMap expectedSamples;
    TSizeMultivariatePriorPtrMap expectedPopulationPriors;

    for (std::size_t i = 0u; i < messages.size(); ++i)
    {
        if (messages[i].s_Time >= startTime + bucketLength)
        {
            LOG_DEBUG("Updating bucket = [" << startTime
                      << "," << startTime + bucketLength << ")");

            model->sample(startTime, startTime + bucketLength, resourceMonitor);

            TSizeDoubleVecVecDoubleVecVecVecPrMap populationWeightedSamples;
            for (TSizeSizePrDoubleVecVecMapCItr itr = expectedSamples.begin();
                 itr != expectedSamples.end();
                 ++itr)
            {
                std::size_t pid = itr->first.first;
                std::size_t cid = itr->first.second;
                TDoubleVecVec &samples = populationWeightedSamples[cid].first;
                TDoubleVecVecVec &weights = populationWeightedSamples[cid].second;
                samples.insert(samples.end(), itr->second.begin(), itr->second.end());
                weights.resize(weights.size() + itr->second.size(),
                               TDoubleVecVec(1, TDoubleVec(2, model->sampleRateWeight(pid, cid))));
            }
            for (TSizeDoubleVecVecDoubleVecVecVecPrMapItr itr = populationWeightedSamples.begin();
                 itr != populationWeightedSamples.end();
                 ++itr)
            {
                std::size_t cid = itr->first;
                TDoubleVecVec &samples = itr->second.first;
                TDoubleVecVecVec &weights = itr->second.second;
                maths::COrderings::simultaneousSort(samples, weights);
                TMultivariatePriorPtr &populationPrior = expectedPopulationPriors[cid];
                if (!populationPrior)
                {
                    populationPrior = factory.defaultMultivariatePrior(features[0]);
                }
                for (std::size_t j = 0u; j < samples.size(); ++j)
                {
                    populationPrior->addSamples(COUNT_WEIGHT,
                                                TDoubleVecVec(1, samples[j]),
                                                TDoubleVecVecVec(1, weights[j]));
                }
                populationPrior->propagateForwardsByTime(1.0);
            }

            LOG_DEBUG("Testing bucket = [" << startTime
                      << "," << startTime + bucketLength << ")");
            LOG_DEBUG("Testing feature " << model_t::print(features[0]));

            for (std::size_t cid = 0u; cid < numberAttributes; ++cid)
            {
                if (expectedPopulationPriors.count(cid) == 0)
                {
                    continue;
                }

                LOG_DEBUG("Testing priors for " << model->attributeName(cid));
                const maths::CMultivariatePrior *populationPrior = model->details()->multivariatePrior(features[0], cid);
                CPPUNIT_ASSERT(populationPrior);
                CPPUNIT_ASSERT_EQUAL(expectedPopulationPriors[cid]->checksum(),
                                     populationPrior->checksum());
            }

            expectedSamples.clear();
            startTime += bucketLength;
        }

        const SMessage &m = messages[i];
        CEventData eventData = addArrival(m, gatherer);
        std::size_t pid = *eventData.personId();
        std::size_t cid = *eventData.attributeId();

        double sampleCount = gatherer->sampleCount(cid);
        if (sampleCount > 0.0)
        {
            TSizeSizePr key(pid, cid);
            sampleMeanAccumulators[key].add(TVector2(m.s_Value));
            if (maths::CBasicStatistics::count(sampleMeanAccumulators[key]) == sampleCount)
            {
                expectedSamples[key].push_back(
                        maths::CBasicStatistics::mean(sampleMeanAccumulators[key]).toVector<TDoubleVec>());
                sampleMeanAccumulators[key] = TMean2Accumulator();
            }
        }
    }
}

void CMetricPopulationModelTest::testVarp(void)
{
    LOG_DEBUG("*** testVarp ***");

    core_t::TTime startTime(3600);
    core_t::TTime bucketLength(3600);
    SModelParams params(bucketLength);
    CDataGatherer::TFeatureVec features;
    features.push_back(model_t::E_PopulationVarianceByPersonAndAttribute);
    CMetricPopulationModelFactory factory(params);
    factory.features(features);
    factory.fieldNames("", "P", "", "V", TStrVec(1, "I"));
    CModelFactory::SGathererInitializationData gathererInitData(startTime);
    CModelFactory::TDataGathererPtr gatherer(factory.makeDataGatherer(gathererInitData));
    CPPUNIT_ASSERT(gatherer->isPopulation());
    CModelFactory::SModelInitializationData initData(gatherer);
    CModel::TModelPtr model_(factory.makeModel(initData));
    CPPUNIT_ASSERT(model_);
    CPPUNIT_ASSERT_EQUAL(model_t::E_MetricOnline, model_->category());
    CMetricPopulationModel &model = static_cast<CMetricPopulationModel&>(*model_.get());

    double bucket1[]                 = {  1.0,  1.1,  1.01,  1.02 };
    std::string influencerValues1[]  = { "i1", "i1", "i2", "i2" };
    double bucket2[]                 = { 10.0 };
    std::string influencerValues2[]  = { "i1" };
    double bucket3[]                 = {  4.3,  4.4,  4.6,  4.2,  4.8 };
    std::string influencerValues3[]  = { "i1", "i1", "i1", "i1", "i3" };
    double bucket4[]                 = {  3.2,  3.3 };
    std::string influencerValues4[]  = { "i3", "i3" };
    double bucket5[]                 = { 20.1, 20.8, 20.9 };
    std::string influencerValues5[]  = { "i2", "i1", "i1" };
    double bucket6[]                 = {  4.1,  4.2,  3.9,  4.2 };
    std::string influencerValues6[]  = { "i1", "i2", "i2", "i2" };
    double bucket7[]                 = {  0.1,  0.3,  0.2 };
    std::string influencerValues7[]  = { "i1", "i1", "i3" };
    double bucket8[]                 = { 12.5, 12.3 };
    std::string influencerValues8[]  = { "i1", "i2" };
    double bucket9[]                 = {  6.9,  7.0,  7.1,  6.6,  7.1,  6.7 };
    std::string influencerValues9[]  = { "i1", "i2", "i3", "i4", "i5", "i6" };
    // This last bucket is much more improbable, with influencer i2 being responsible
    double bucket10[]                = {  0.3, 15.4, 77.62, 112.999, 5.1, 5.1, 5.1, 5.1, 5.1 };
    std::string influencerValues10[] = { "i2", "i2", "i2", "i2", "i1", "i1", "i1", "i1", "i1"};

    SAnnotatedProbability annotatedProbability;

    core_t::TTime time = startTime;
    processBucket(time, bucketLength, boost::size(bucket1), bucket1,
                  influencerValues1, *gatherer, model, annotatedProbability);
    CPPUNIT_ASSERT(annotatedProbability.s_Probability > 0.8);

    time += bucketLength;
    processBucket(time, bucketLength, boost::size(bucket2), bucket2,
                  influencerValues2, *gatherer, model, annotatedProbability);
    CPPUNIT_ASSERT(annotatedProbability.s_Probability > 0.8);

    time += bucketLength;
    processBucket(time, bucketLength, boost::size(bucket3), bucket3,
                  influencerValues3, *gatherer, model, annotatedProbability);
    CPPUNIT_ASSERT(annotatedProbability.s_Probability > 0.8);

    time += bucketLength;
    processBucket(time, bucketLength, boost::size(bucket4), bucket4,
                  influencerValues4, *gatherer, model, annotatedProbability);
    CPPUNIT_ASSERT(annotatedProbability.s_Probability > 0.8);

    time += bucketLength;
    processBucket(time, bucketLength, boost::size(bucket5), bucket5,
                  influencerValues5, *gatherer, model, annotatedProbability);
    CPPUNIT_ASSERT(annotatedProbability.s_Probability > 0.8);

    time += bucketLength;
    processBucket(time, bucketLength, boost::size(bucket6), bucket6,
                  influencerValues6, *gatherer, model, annotatedProbability);
    CPPUNIT_ASSERT(annotatedProbability.s_Probability > 0.8);

    time += bucketLength;
    processBucket(time, bucketLength, boost::size(bucket7), bucket7,
                  influencerValues7, *gatherer, model, annotatedProbability);
    CPPUNIT_ASSERT(annotatedProbability.s_Probability > 0.8);

    time += bucketLength;
    processBucket(time, bucketLength, boost::size(bucket8), bucket8,
                  influencerValues8, *gatherer, model, annotatedProbability);
    CPPUNIT_ASSERT(annotatedProbability.s_Probability > 0.8);

    time += bucketLength;
    processBucket(time, bucketLength, boost::size(bucket9), bucket9,
                  influencerValues9, *gatherer, model, annotatedProbability);
    CPPUNIT_ASSERT(annotatedProbability.s_Probability < 0.85);

    time += bucketLength;
    processBucket(time, bucketLength, boost::size(bucket10), bucket10,
                  influencerValues10, *gatherer, model, annotatedProbability);
    CPPUNIT_ASSERT(annotatedProbability.s_Probability < 0.1);
    CPPUNIT_ASSERT_EQUAL(std::size_t(1), annotatedProbability.s_Influences.size());
    CPPUNIT_ASSERT_EQUAL(std::string("I"), *annotatedProbability.s_Influences[0].first.first);
    CPPUNIT_ASSERT_EQUAL(std::string("i2"), *annotatedProbability.s_Influences[0].first.second);
    CPPUNIT_ASSERT_DOUBLES_EQUAL(1.0, annotatedProbability.s_Influences[0].second, 0.00001);
}

void CMetricPopulationModelTest::testComputeProbability(void)
{
    LOG_DEBUG("*** testComputeProbability ***");

    maths::CSampling::CScopeMockRandomNumberGenerator scopeMockRng;

    // Test that we correctly pick out synthetic the anomalies,
    // their people and attributes.

    typedef std::vector<SAnomaly> TAnomalyVec;
    typedef std::pair<double, SAnomaly> TDoubleAnomalyPr;
    typedef maths::CBasicStatistics::COrderStatisticsHeap<TDoubleAnomalyPr,
                                                          maths::COrderings::SFirstLess> TAnomalyAccumulator;

    core_t::TTime startTime = 1367280000;
    const core_t::TTime bucketLength = 3600;

    model_t::EFeature features_[] =
        {
            model_t::E_PopulationMaxByPersonAndAttribute,
            model_t::E_PopulationMeanLatLongByPersonAndAttribute
        };

    for (std::size_t i = 0u; i < boost::size(features_); ++i)
    {
        LOG_DEBUG("Testing " << model_t::print(features_[i]));

        TMessageVec messages;
        generateTestMessages(model_t::dimension(features_[i]), startTime, bucketLength, messages);

        SModelParams params(bucketLength);
        CMetricPopulationModelFactory factory(params);
        CModelFactory::TFeatureVec features(1, features_[i]);
        factory.features(features);
        CModelFactory::SGathererInitializationData gathererInitData(startTime);
        CModelFactory::TDataGathererPtr gatherer(factory.makeDataGatherer(gathererInitData));
        CModelFactory::SModelInitializationData modelInitData(gatherer);
        CModel::TModelPtr modelHolder(factory.makeModel(modelInitData));
        CMetricPopulationModel *model =
                dynamic_cast<CMetricPopulationModel*>(modelHolder.get());
        CResourceMonitor resourceMonitor;
    
        TAnomalyAccumulator anomalies(7);
    
        for (std::size_t j = 0u, bucket = 0u; j < messages.size(); ++j)
        {
            if (messages[j].s_Time >= startTime + bucketLength)
            {
                //LOG_DEBUG("Updating bucket = [" << startTime
                //          << "," << startTime + bucketLength << ")");
    
                model->sample(startTime, startTime + bucketLength, resourceMonitor);
    
                LOG_DEBUG("Testing bucket " << bucket
                          << " = [" << startTime << "," << startTime + bucketLength << ")");
    
                CPartitioningFields partitioningFields(EMPTY_STRING, EMPTY_STRING);
                SAnnotatedProbability annotatedProbability;
                for (std::size_t pid = 0u; pid < numberPeople; ++pid)
                {
                    model->computeProbability(pid, startTime, startTime + bucketLength,
                                              partitioningFields, 2, annotatedProbability);
    
                    LOG_DEBUG("person = " << model->personName(pid)
                              << ", probability = " << annotatedProbability.s_Probability);
    
                    std::string person = model->personName(pid);
                    TDoubleStrPrVec attributes;
                    for (std::size_t k = 0u; k < annotatedProbability.s_AttributeProbabilities.size(); ++k)
                    {
                        attributes.push_back(TDoubleStrPr(annotatedProbability.s_AttributeProbabilities[k].s_Probability,
                                                          *annotatedProbability.s_AttributeProbabilities[k].s_Attribute));
                    }
                    anomalies.add(TDoubleAnomalyPr(annotatedProbability.s_Probability, SAnomaly(bucket, person, attributes)));
                }
    
                startTime += bucketLength;
                ++bucket;
            }
    
            addArrival(messages[j], gatherer);
        }
    
        anomalies.sort();
        LOG_DEBUG("Anomalies = " << core::CContainerPrinter::print(anomalies.begin(), anomalies.end()));
    
        TAnomalyVec orderedAnomalies;
        for (std::size_t j = 0u; j < anomalies.count(); ++j)
        {
            orderedAnomalies.push_back(anomalies[j].second);
        }
        std::sort(orderedAnomalies.begin(), orderedAnomalies.end());
    
        LOG_DEBUG("orderedAnomalies = "
                  << core::CContainerPrinter::print(orderedAnomalies));
    
        std::string expectedAnomalies[] =
        {
                std::string("[12, p2, c0 c3]"),
                std::string("[15, p3, c0]"),
                std::string("[30, p5, c2]"),
                std::string("[40, p6, c0]"),
                std::string("[44, p9, c2]"),
                std::string("[60, p2, c4]"),
                std::string("[80, p1, c3]")
        };
    
        CPPUNIT_ASSERT_EQUAL(boost::size(expectedAnomalies), orderedAnomalies.size());
        for (std::size_t j = 0u; j < orderedAnomalies.size(); ++j)
        {
            CPPUNIT_ASSERT_EQUAL(expectedAnomalies[j],
                                 orderedAnomalies[j].print());
        }
    }
}

void CMetricPopulationModelTest::testPrune(void)
{
    LOG_DEBUG("*** testPrune ***");

    // This test has four people and five attributes. We expect
    // person 2 and attributes 1, 2 and 5 to be deleted.

    typedef std::pair<std::string, std::size_t> TStrSizePr;
    typedef std::vector<TStrSizePr> TStrSizePrVec;
    typedef std::vector<TStrSizePrVec> TStrSizePrVecVec;

    core_t::TTime startTime = 1367280000;
    const core_t::TTime bucketLength = 3600;
    const std::size_t numberBuckets = 1000u;

    std::string people[] =
        {
            std::string("p1"),
            std::string("p2"),
            std::string("p3"),
            std::string("p4")
        };
    std::string attributes[] =
        {
            std::string("c1"),
            std::string("c2"),
            std::string("c3"),
            std::string("c4"),
            std::string("c5")
        };

    TStrSizePrVecVec eventCounts[] =
        {
            TStrSizePrVecVec(),
            TStrSizePrVecVec(),
            TStrSizePrVecVec(),
            TStrSizePrVecVec()
        };
    {
        TStrSizePrVec attributeCounts;
        attributeCounts.push_back(TStrSizePr(attributes[0], 0));
        attributeCounts.push_back(TStrSizePr(attributes[4], 0));
        eventCounts[0].resize(numberBuckets, attributeCounts);
        eventCounts[0][0][0].second = 2; // p1, bucket 1,  c1
        eventCounts[0][2][0].second = 3; // p1, bucket 3,  c1
        eventCounts[0][4][0].second = 4; // p1, bucket 5,  c1
        eventCounts[0][8][0].second = 5; // p1, bucket 9,  c1
        eventCounts[0][9][0].second = 3; // p1, bucket 10, c1
        eventCounts[0][0][1].second = 4; // p1, bucket 1,  c5
        eventCounts[0][3][1].second = 1; // p1, bucket 4,  c5
        eventCounts[0][7][1].second = 1; // p1, bucket 8,  c5
        eventCounts[0][8][1].second = 3; // p1, bucket 9,  c5
    }
    {
        TStrSizePrVec attributeCounts;
        attributeCounts.push_back(TStrSizePr(attributes[0], 0));
        attributeCounts.push_back(TStrSizePr(attributes[1], 0));
        attributeCounts.push_back(TStrSizePr(attributes[2], 1));
        attributeCounts.push_back(TStrSizePr(attributes[4], 0));
        eventCounts[1].resize(numberBuckets, attributeCounts);
        eventCounts[1][1][0].second = 2; // p2, bucket 2, c1
        eventCounts[1][3][0].second = 4; // p2, bucket 3, c1
        eventCounts[1][4][0].second = 4; // p2, bucket 5, c1
        eventCounts[1][7][0].second = 3; // p2, bucket 8, c1
        eventCounts[1][5][1].second = 4; // p2, bucket 6, c2
        eventCounts[1][6][1].second = 4; // p2, bucket 7, c2
        eventCounts[1][7][1].second = 3; // p2, bucket 8, c2
        eventCounts[1][8][1].second = 3; // p2, bucket 9, c2
        eventCounts[1][0][3].second = 3; // p2, bucket 1, c5
        eventCounts[1][1][3].second = 3; // p2, bucket 2, c5
        eventCounts[1][2][3].second = 3; // p2, bucket 3, c5
        eventCounts[1][3][3].second = 3; // p2, bucket 4, c5
        eventCounts[1][4][3].second = 2; // p2, bucket 5, c5
        eventCounts[1][5][3].second = 2; // p2, bucket 6, c5
    }
    {
        TStrSizePrVec attributeCounts;
        attributeCounts.push_back(TStrSizePr(attributes[2], 0));
        attributeCounts.push_back(TStrSizePr(attributes[3], 2));
        eventCounts[2].resize(numberBuckets, attributeCounts);
        eventCounts[2][0][0].second = 1;   // p3, bucket 1,   c3
        eventCounts[2][20][0].second = 4;  // p3, bucket 21,  c3
        eventCounts[2][25][0].second = 6;  // p3, bucket 26,  c3
        eventCounts[2][80][0].second = 3;  // p3, bucket 81,  c3
        eventCounts[2][180][0].second = 7; // p3, bucket 181, c3
        eventCounts[2][200][0].second = 9; // p3, bucket 201, c3
        eventCounts[2][800][0].second = 2; // p3, bucket 801, c3
    }
    {
        TStrSizePrVec attributeCounts;
        attributeCounts.push_back(TStrSizePr(attributes[1], 0));
        attributeCounts.push_back(TStrSizePr(attributes[3], 3));
        eventCounts[3].resize(numberBuckets, attributeCounts);
        eventCounts[3][0][0].second = 2;  // p4, bucket 1,  c2
        eventCounts[3][1][0].second = 1;  // p4, bucket 2,  c2
        eventCounts[3][2][0].second = 5;  // p4, bucket 3,  c2
        eventCounts[3][15][0].second = 3; // p4, bucket 16, c2
        eventCounts[3][26][0].second = 1; // p4, bucket 27, c2
        eventCounts[3][70][0].second = 4; // p4, bucket 70, c2
    }

    const std::string expectedPeople[] = { people[1], people[2], people[3] };
    const std::string expectedAttributes[] = { attributes[2], attributes[3] };

    SModelParams params(bucketLength);
    params.s_DecayRate = 0.01;
    CMetricPopulationModelFactory factory(params);
    CDataGatherer::TFeatureVec features;
    features.push_back(model_t::E_PopulationMeanByPersonAndAttribute);
    features.push_back(model_t::E_PopulationMinByPersonAndAttribute);
    features.push_back(model_t::E_PopulationMaxByPersonAndAttribute);
    factory.features(features);
    CModelFactory::SGathererInitializationData gathererInitData(startTime);
    CModelFactory::TDataGathererPtr gatherer(factory.makeDataGatherer(gathererInitData));
    CModelFactory::SModelInitializationData modelInitData(gatherer);
    CModel::TModelPtr model(factory.makeModel(modelInitData));
    CPPUNIT_ASSERT(model);
    CModelFactory::TDataGathererPtr expectedGatherer(factory.makeDataGatherer(gathererInitData));
    CModelFactory::SModelInitializationData expectedModelInitData(expectedGatherer);
    CModel::TModelPtr expectedModel(factory.makeModel(expectedModelInitData));
    CPPUNIT_ASSERT(expectedModel);
    CResourceMonitor resourceMonitor;

    test::CRandomNumbers rng;

    TMessageVec messages;
    for (std::size_t i = 0u; i < boost::size(people); ++i)
    {
        core_t::TTime bucketStart = startTime;
        for (std::size_t j = 0u; j < numberBuckets; ++j, bucketStart += bucketLength)
        {
            const TStrSizePrVec &attributeEventCounts = eventCounts[i][j];
            for (std::size_t k = 0u; k < attributeEventCounts.size(); ++k)
            {
                if (attributeEventCounts[k].second == 0)
                {
                    continue;
                }

                std::size_t n = attributeEventCounts[k].second;

                TDoubleVec samples;
                rng.generateUniformSamples(0.0, 8.0, n, samples);

                core_t::TTime time = bucketStart;
                core_t::TTime dt = bucketLength / static_cast<core_t::TTime>(n);

                for (std::size_t l = 0u; l < n; ++l, time += dt)
                {
                    messages.push_back(SMessage(time,
                                                people[i],
                                                attributeEventCounts[k].first,
                                                TDouble1Vec(1, samples[l])));
                }
            }
        }
    }
    std::sort(messages.begin(), messages.end());

    TMessageVec expectedMessages;
    expectedMessages.reserve(messages.size());
    for (std::size_t i = 0u; i < messages.size(); ++i)
    {
        if (   std::binary_search(boost::begin(expectedPeople),
                                  boost::end(expectedPeople),
                                  messages[i].s_Person)
            && std::binary_search(boost::begin(expectedAttributes),
                                  boost::end(expectedAttributes),
                                  messages[i].s_Attribute))
        {
            expectedMessages.push_back(messages[i]);
        }
    }

    core_t::TTime bucketStart = startTime;
    for (std::size_t i = 0u; i < messages.size(); ++i)
    {
        if (messages[i].s_Time >= bucketStart + bucketLength)
        {
            model->sample(bucketStart, bucketStart + bucketLength, resourceMonitor);
            bucketStart += bucketLength;
        }
        addArrival(messages[i], gatherer);
    }
    model->sample(bucketStart, bucketStart + bucketLength, resourceMonitor);
    size_t maxDimensionBeforePrune(model->dataGatherer().maxDimension());
    model->prune();
    size_t maxDimensionAfterPrune(model->dataGatherer().maxDimension());
    CPPUNIT_ASSERT_EQUAL(maxDimensionBeforePrune, maxDimensionAfterPrune);

    bucketStart = startTime;
    for (std::size_t i = 0u; i < expectedMessages.size(); ++i)
    {
        if (expectedMessages[i].s_Time >= bucketStart + bucketLength)
        {
            expectedModel->sample(bucketStart, bucketStart + bucketLength, resourceMonitor);
            bucketStart += bucketLength;
        }
        addArrival(expectedMessages[i], expectedGatherer);
    }
    expectedModel->sample(bucketStart, bucketStart + bucketLength, resourceMonitor);

    LOG_DEBUG("checksum          = " << model->checksum());
    LOG_DEBUG("expected checksum = " << expectedModel->checksum());
    CPPUNIT_ASSERT_EQUAL(expectedModel->checksum(), model->checksum());

    // Now check that we recycle the person and attribute slots.

    bucketStart = gatherer->currentBucketStartTime() + bucketLength;

    SMessage newMessages[] =
        {
            SMessage(bucketStart + 10, "p1", "c2", TDouble1Vec(1, 20.0)),
            SMessage(bucketStart + 200, "p5", "c6", TDouble1Vec(1, 10.0)),
            SMessage(bucketStart + 2100, "p5", "c6", TDouble1Vec(1, 15.0))
        };

    for (std::size_t i = 0u; i < boost::size(newMessages); ++i)
    {
        addArrival(newMessages[i], gatherer);
        addArrival(newMessages[i], expectedGatherer);
    }
    model->sample(bucketStart, bucketStart + bucketLength, resourceMonitor);
    expectedModel->sample(bucketStart, bucketStart + bucketLength, resourceMonitor);

    LOG_DEBUG("checksum          = " << model->checksum());
    LOG_DEBUG("expected checksum = " << expectedModel->checksum());
    CPPUNIT_ASSERT_EQUAL(expectedModel->checksum(), model->checksum());

    // Test that calling prune on a cloned model which has seen no new data does nothing
    CModel::TModelPtr clonedModelHolder(model->cloneForPersistence());
    std::size_t numberOfPeopleBeforePrune(clonedModelHolder->dataGatherer().numberActivePeople());
    CPPUNIT_ASSERT(numberOfPeopleBeforePrune > 0);
    clonedModelHolder->prune(clonedModelHolder->defaultPruneWindow());
    CPPUNIT_ASSERT_EQUAL(numberOfPeopleBeforePrune, clonedModelHolder->dataGatherer().numberActivePeople());
}

void CMetricPopulationModelTest::testKey(void)
{
    LOG_DEBUG("*** testKey ***");

    function_t::EFunction countFunctions[] =
        {
            function_t::E_PopulationMetric,
            function_t::E_PopulationMetricMean,
            function_t::E_PopulationMetricMin,
            function_t::E_PopulationMetricMax,
            function_t::E_PopulationMetricSum
        };
    bool useNull[] = { true, false };
    std::string byField[] = { "", "by" };
    std::string partitionField[] = { "", "partition" };

    {
        CModelConfig config = CModelConfig::defaultConfig();

        int identifier = 0;
        for (std::size_t i = 0u; i < boost::size(countFunctions); ++i)
        {
            for (std::size_t j = 0u; j < boost::size(useNull); ++j)
            {
                for (std::size_t k = 0u; k < boost::size(byField); ++k)
                {
                    for (std::size_t l = 0u; l < boost::size(partitionField); ++l)
                    {
                        CSearchKey key(++identifier,
                                       countFunctions[i],
                                       useNull[j],
                                       model_t::E_XF_None,
                                       "value",
                                       byField[k],
                                       "over",
                                       partitionField[l]);

                        CModelConfig::TModelFactoryCPtr factory = config.factory(key);

                        LOG_DEBUG("expected key = " << key);
                        LOG_DEBUG("actual key   = " << factory->searchKey());
                        CPPUNIT_ASSERT(key == factory->searchKey());
                    }
                }
            }
        }
    }
}

void CMetricPopulationModelTest::testFrequency(void)
{
    LOG_DEBUG("*** CMetricPopulationModelTest::testFrequency ***");

    typedef maths::CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;

    // Test we correctly compute frequencies for people and attributes.

    const core_t::TTime bucketLength = 600;
    const std::string attributes[] = { "a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9", "a10" };
    const std::string people[] = { "p1",  "p2",  "p3",  "p4",  "p5",  "p6",  "p7",  "p8",  "p9", "p10" };
    std::size_t period[] = { 1u, 1u, 10u, 3u, 4u, 5u, 2u, 1u, 3u, 7u };

    core_t::TTime startTime = 0;

    TMessageVec messages;
    std::size_t bucket = 0u;
    for (core_t::TTime bucketStart = startTime;
         bucketStart < 100 * bucketLength;
         bucketStart += bucketLength, ++bucket)
    {
        for (std::size_t i = 0u; i < boost::size(people); ++i)
        {
            if (bucket % period[i] == 0)
            {
                for (std::size_t j = 0u; j < i+1; ++j)
                {
                    messages.push_back(SMessage(bucketStart + bucketLength / 2,
                                                people[i],
                                                attributes[j],
                                                TDouble1Vec(1, 0.0)));
                }
            }
        }
    }

    std::sort(messages.begin(), messages.end());
    LOG_DEBUG("# messages = " << messages.size());

    SModelParams params(bucketLength);
    params.s_DecayRate = 0.001;
    CMetricPopulationModelFactory factory(params);
    CModelFactory::TFeatureVec features;
    features.push_back(model_t::E_PopulationMeanByPersonAndAttribute);
    factory.features(features);
    factory.extraDataConversionFuncs(dummyAnyToPersistInserter, dummyRestoreFunc, model_t::TAnyMemoryFunc());
    CModelFactory::SGathererInitializationData gathererInitData(startTime);
    CModelFactory::TDataGathererPtr gatherer(factory.makeDataGatherer(gathererInitData));
    const model::CDataGatherer &populationGatherer(dynamic_cast<const model::CDataGatherer&>(*gatherer));

    CModelFactory::SModelInitializationData modelInitData(gatherer);
    CModel::TModelPtr model(factory.makeModel(modelInitData));

    CMetricPopulationModel *populationModel =
            dynamic_cast<CMetricPopulationModel*>(model.get());
    CPPUNIT_ASSERT(populationModel != 0);
    CResourceMonitor resourceMonitor;

    core_t::TTime time = startTime;
    for (std::size_t i = 0u; i < messages.size(); ++i)
    {
        if (messages[i].s_Time >= time + bucketLength)
        {
            populationModel->sample(time, time + bucketLength, resourceMonitor);

            LOG_DEBUG("Processing bucket = [" << time
                      << ',' << time + bucketLength << ')');

            time += bucketLength;
        }

        addArrival(messages[i], gatherer);
    }

    {
        TMeanAccumulator meanError;
        for (std::size_t i = 0u; i < boost::size(people); ++i)
        {
            LOG_DEBUG("*** person = " << people[i] << " ***");
            std::size_t pid;
            CPPUNIT_ASSERT(gatherer->personId(people[i], pid));
            LOG_DEBUG("frequency = " << populationModel->personFrequency(pid));
            LOG_DEBUG("expected frequency = " << 1.0 / static_cast<double>(period[i]));
            CPPUNIT_ASSERT_DOUBLES_EQUAL(1.0 / static_cast<double>(period[i]),
                                         populationModel->personFrequency(pid),
                                         0.1 / static_cast<double>(period[i]));
            meanError.add(::fabs(  populationModel->personFrequency(pid)
                                 - 1.0 / static_cast<double>(period[i])));
        }
        LOG_DEBUG("error = " << maths::CBasicStatistics::mean(meanError));
        CPPUNIT_ASSERT(maths::CBasicStatistics::mean(meanError) < 0.002);
    }
    {
        for (std::size_t i = 0u; i < boost::size(attributes); ++i)
        {
            LOG_DEBUG("*** attributes = " << attributes[i] << " ***");
            std::size_t cid;
            CPPUNIT_ASSERT(populationGatherer.attributeId(attributes[i], cid));
            LOG_DEBUG("frequency = " << populationModel->attributeFrequency(cid));
            LOG_DEBUG("expected frequency = " << (10.0 - static_cast<double>(i)) / 10.0);
            CPPUNIT_ASSERT_EQUAL((10.0 - static_cast<double>(i)) / 10.0,
                                 populationModel->attributeFrequency(cid));
        }
    }
}

void CMetricPopulationModelTest::testSampleRateWeight(void)
{
    LOG_DEBUG("*** CMetricPopulationModelTest::testSampleRateWeight ***");

    // Test that we correctly compensate for heavy hitters.

    // There are 10 attributes.
    //
    // People p1 and p5 generate messages for every attribute every bucket.
    // The remaining 18 people only generate one message per bucket, i.e.
    // one message per attribute per 10 buckets.

    const core_t::TTime bucketLength = 600;
    const std::string attributes[] = { "a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9", "a10" };
    const std::string people[] = {  "p1",  "p2",  "p3",  "p4",  "p5",  "p6",  "p7",  "p8",  "p9", "p10",
                                   "p11", "p12", "p13", "p14", "p15", "p16", "p17", "p18", "p19", "p20" };
    std::size_t heavyHitters[] = { 0u, 4u };
    std::size_t normal[] = { 1u, 2u, 3u, 5u, 6u, 7u, 8u, 9u, 10u, 11u, 12u, 13u, 14u, 15u, 16u, 17u, 18u, 19u };

    std::size_t messagesPerBucket = boost::size(heavyHitters) * boost::size(attributes) + boost::size(normal);

    test::CRandomNumbers rng;

    core_t::TTime startTime = 0;

    TMessageVec messages;
    for (core_t::TTime bucketStart = startTime;
         bucketStart < 100 * bucketLength;
         bucketStart += bucketLength)
    {
        TSizeVec times;
        rng.generateUniformSamples(static_cast<std::size_t>(bucketStart),
                                   static_cast<std::size_t>(bucketStart + bucketLength),
                                   messagesPerBucket,
                                   times);

        std::size_t m = 0u;
        for (std::size_t i = 0u; i < boost::size(attributes); ++i)
        {
            for (std::size_t j = 0u; j < boost::size(heavyHitters); ++j)
            {
                messages.push_back(SMessage(static_cast<core_t::TTime>(times[m++]),
                                            people[heavyHitters[j]],
                                            attributes[i],
                                            TDouble1Vec(1, 0.0)));
            }
        }

        TSizeVec attributeIndexes;
        rng.generateUniformSamples(0, boost::size(attributes),
                                   boost::size(normal),
                                   attributeIndexes);
        for (std::size_t i = 0u; i < boost::size(normal); ++i)
        {
            messages.push_back(SMessage(static_cast<core_t::TTime>(times[m++]),
                                        people[normal[i]],
                                        attributes[attributeIndexes[i]],
                                        TDouble1Vec(1, 0.0)));
        }
    }

    std::sort(messages.begin(), messages.end());

    SModelParams params(bucketLength);
    params.s_DecayRate = 0.001;
    CMetricPopulationModelFactory factory(params);
    CModelFactory::TFeatureVec features;
    features.push_back(model_t::E_PopulationSumByBucketPersonAndAttribute);
    factory.features(features);
    factory.extraDataConversionFuncs(dummyAnyToPersistInserter, dummyRestoreFunc, model_t::TAnyMemoryFunc());
    CModelFactory::SGathererInitializationData gathererInitData(startTime);
    CModelFactory::TDataGathererPtr gatherer(factory.makeDataGatherer(gathererInitData));

    CModelFactory::SModelInitializationData modelInitData(gatherer);
    CModel::TModelPtr model(factory.makeModel(modelInitData));

    CMetricPopulationModel *populationModel =
            dynamic_cast<CMetricPopulationModel*>(model.get());
    CPPUNIT_ASSERT(populationModel != 0);
    CResourceMonitor resourceMonitor;

    core_t::TTime time = startTime;
    for (std::size_t i = 0u; i < messages.size(); ++i)
    {
        if (messages[i].s_Time >= time + bucketLength)
        {
            populationModel->sample(time, time + bucketLength, resourceMonitor);

            LOG_DEBUG("Processing bucket = [" << time
                      << ',' << time + bucketLength << ')');

            time += bucketLength;
        }

        addArrival(messages[i], gatherer);
    }

    // The heavy hitters generate one value per attribute per bucket.
    // The rest generate one value per bucket. Therefore, we expect
    // the mean person rate per bucket to be:
    //     (  ("# people" - "# heavy hitters" / "# attributes")
    //      + ("# heavy hitters"))
    //   / "# people"

    double expectedRateWeight =   (  static_cast<double>(boost::size(normal))
                                   / static_cast<double>(boost::size(attributes))
                                   + static_cast<double>(boost::size(heavyHitters)))
                                / static_cast<double>(boost::size(people));
    LOG_DEBUG("expectedRateWeight = " << expectedRateWeight);

    for (std::size_t i = 0u; i < boost::size(heavyHitters); ++i)
    {
        LOG_DEBUG("*** person = " << people[heavyHitters[i]] << " ***");
        std::size_t pid;
        CPPUNIT_ASSERT(gatherer->personId(people[heavyHitters[i]], pid));
        for (std::size_t cid = 0u; cid < boost::size(attributes); ++cid)
        {
            double sampleRateWeight = populationModel->sampleRateWeight(pid, cid);
            LOG_DEBUG("attribute = " << populationModel->attributeName(cid)
                      << ", sampleRateWeight = " << sampleRateWeight);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(expectedRateWeight,
                                         sampleRateWeight,
                                         0.15 * expectedRateWeight);
        }
    }

    for (std::size_t i = 0u; i < boost::size(normal); ++i)
    {
        LOG_DEBUG("*** person = " << people[normal[i]] << " ***");
        std::size_t pid;
        CPPUNIT_ASSERT(gatherer->personId(people[normal[i]], pid));
        for (std::size_t cid = 0u; cid < boost::size(attributes); ++cid)
        {
            double sampleRateWeight = populationModel->sampleRateWeight(pid, cid);
            LOG_DEBUG("attribute = " << populationModel->attributeName(cid)
                      << ", sampleRateWeight = " << sampleRateWeight);
            CPPUNIT_ASSERT_EQUAL(1.0, sampleRateWeight);
        }
    }
}

void CMetricPopulationModelTest::testPeriodicity(void)
{
    LOG_DEBUG("*** testPeriodicity ***");

    // Create a daily periodic population and check that the
    // periodicity is learned and compensated (approximately).

    typedef std::map<std::string, double> TStrDoubleMap;
    typedef TStrDoubleMap::const_iterator TStrDoubleMapCItr;

    static const core_t::TTime HOUR = 3600;
    static const core_t::TTime DAY = 86400;

    const core_t::TTime bucketLength = 3600;
    double baseline[] =
        {
             1,  1,  2,  2,  3,  5,  6,  6,
            20, 21,  4,  3,  4,  4,  8, 25,
             7,  6,  5,  1,  1,  4,  1,  1
        };
    const std::string attributes[] = { "a1", "a2" };
    double scales[] = { 2.0, 3.0 };
    const std::string people[] = {  "p1",  "p2",  "p3",  "p4",  "p5",  "p6",  "p7",  "p8",  "p9", "p10" };

    test::CRandomNumbers rng;

    core_t::TTime startTime = 0;
    core_t::TTime endTime = 604800;

    TMessageVec messages;
    for (core_t::TTime time = startTime;
         time < endTime;
         time += bucketLength)
    {
        for (std::size_t i = 0u; i < boost::size(attributes); ++i)
        {
            TDoubleVec values;
            rng.generateNormalSamples(baseline[(time % DAY) / HOUR],
                                      scales[i] * scales[i],
                                      boost::size(people),
                                      values);

            for (std::size_t j = 0u; j < values.size(); ++j)
            {
                for (unsigned int t = 0; t < 4; ++t)
                {
                    messages.push_back(SMessage(time + (t * bucketLength) / 4,
                                                people[j],
                                                attributes[i],
                                                TDouble1Vec(1, values[j])));
                }
            }
        }
    }
    std::sort(messages.begin(), messages.end());

    SModelParams params(bucketLength);
    params.s_DecayRate = 0.001;
    CMetricPopulationModelFactory factory(params);
    CModelFactory::TFeatureVec features;
    features.push_back(model_t::E_PopulationMeanByPersonAndAttribute);
    factory.features(features);
    factory.extraDataConversionFuncs(dummyAnyToPersistInserter, dummyRestoreFunc, model_t::TAnyMemoryFunc());

    CModelFactory::SGathererInitializationData gathererInitData(startTime);
    CModelFactory::TDataGathererPtr gatherer(factory.makeDataGatherer(gathererInitData));

    CModelFactory::SModelInitializationData modelInitData(gatherer);
    CModel::TModelPtr model(factory.makeModel(modelInitData));
    CMetricPopulationModel *populationModel =
            dynamic_cast<CMetricPopulationModel*>(model.get());
    CPPUNIT_ASSERT(populationModel != 0);
    CResourceMonitor resourceMonitor;

    TStrDoubleMap personProbabilitiesWithoutPeriodicity;
    TStrDoubleMap personProbabilitiesWithPeriodicity;

    core_t::TTime time = startTime;
    for (std::size_t i = 0u; i < messages.size(); ++i)
    {
        if (messages[i].s_Time >= time + bucketLength)
        {
            populationModel->sample(time, time + bucketLength, resourceMonitor);

            LOG_DEBUG("Processing bucket = [" << time
                      << ',' << time + bucketLength << ')');

            for (std::size_t j = 0u; j < boost::size(people); ++j)
            {
                std::size_t pid;
                if (!gatherer->personId(people[j], pid))
                {
                    continue;
                }

                CPartitioningFields partitioningFields(EMPTY_STRING, EMPTY_STRING);
                SAnnotatedProbability annotatedProbability;
                if (populationModel->computeProbability(pid, time, time + bucketLength,
                                                        partitioningFields, 1, annotatedProbability) == false)
                {
                    continue;
                }

                if (time < startTime + 3 * DAY)
                {
                    double &minimumProbability =
                            personProbabilitiesWithoutPeriodicity.insert(
                                    TStrDoubleMap::value_type(people[j], 1.0)).first->second;
                    minimumProbability =
                            std::min(minimumProbability, annotatedProbability.s_Probability);
                }
                else if (time > startTime + 4 * DAY)
                {
                    double &minimumProbability =
                            personProbabilitiesWithPeriodicity.insert(
                                    TStrDoubleMap::value_type(people[j], 1.0)).first->second;
                    minimumProbability =
                            std::min(minimumProbability, annotatedProbability.s_Probability);
                }
            }
            time += bucketLength;
        }

        addArrival(messages[i], gatherer);
    }

    double totalw  = 0.0;
    double totalwo = 0.0;

    for (std::size_t i = 0u; i < boost::size(people); ++i)
    {
        TStrDoubleMapCItr wo = personProbabilitiesWithoutPeriodicity.find(people[i]);
        TStrDoubleMapCItr w  = personProbabilitiesWithPeriodicity.find(people[i]);
        LOG_DEBUG("person = " << people[i]);
        LOG_DEBUG("minimum probability with periodicity    = " << w->second);
        LOG_DEBUG("minimum probability without periodicity = " << wo->second);
        totalwo += wo->second;
        totalw  += w->second;
    }

    LOG_DEBUG("total minimum probability with periodicity    = " << totalw);
    LOG_DEBUG("total minimum probability without periodicity = " << totalwo);
    CPPUNIT_ASSERT(totalw > 4.0 * totalwo);
}

void CMetricPopulationModelTest::testPersistence(void)
{
    maths::CScopeDisableNormalizeOnRestore disabler;

    core_t::TTime startTime = 1367280000;
    const core_t::TTime bucketLength = 3600;

    TMessageVec messages;
    generateTestMessages(1, startTime, bucketLength, messages);

    SModelParams params(bucketLength);
    params.s_DecayRate = 0.001;
    CMetricPopulationModelFactory factory(params);
    CModelFactory::TFeatureVec features;
    features.push_back(model_t::E_PopulationMeanByPersonAndAttribute);
    features.push_back(model_t::E_PopulationMinByPersonAndAttribute);
    features.push_back(model_t::E_PopulationMaxByPersonAndAttribute);
    factory.features(features);
    factory.extraDataConversionFuncs(dummyAnyToPersistInserter, dummyRestoreFunc, model_t::TAnyMemoryFunc());
    CModelFactory::SGathererInitializationData gathererInitData(startTime);
    CModelFactory::TDataGathererPtr gatherer(factory.makeDataGatherer(gathererInitData));

    CModelFactory::SModelInitializationData modelInitData(gatherer);
    CModel::TModelPtr origModel(factory.makeModel(modelInitData));
    CResourceMonitor resourceMonitor;

    CMetricPopulationModel *populationModel =
            dynamic_cast<CMetricPopulationModel*>(origModel.get());
    CPPUNIT_ASSERT(populationModel != 0);

    for (std::size_t i = 0u; i < messages.size(); ++i)
    {
        if (messages[i].s_Time >= startTime + bucketLength)
        {
            LOG_DEBUG("Processing bucket = [" << startTime
                      << ',' << startTime + bucketLength << ')');
            origModel->sample(startTime, startTime + bucketLength, resourceMonitor);
            startTime += bucketLength;
        }
        addArrival(messages[i], gatherer);
    }

    std::string origXml;
    {
        core::CRapidXmlStatePersistInserter inserter("root");
        origModel->acceptPersistInserter(inserter);
        inserter.toXml(origXml);
    }

    LOG_TRACE("origXml = " << origXml);

    // Restore the XML into a new data gatherer
    core::CRapidXmlParser parser;
    CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(origXml));
    core::CRapidXmlStateRestoreTraverser traverser(parser);

    CModel::TModelPtr restoredModel(factory.makeModel(modelInitData, traverser));

    populationModel =
            dynamic_cast<CMetricPopulationModel*>(restoredModel.get());
    CPPUNIT_ASSERT(populationModel != 0);

    // The XML representation of the new data gatherer should be the same as the
    // original
    std::string newXml;
    {
        core::CRapidXmlStatePersistInserter inserter("root");
        restoredModel->acceptPersistInserter(inserter);
        inserter.toXml(newXml);
    }

    LOG_DEBUG("original checksum = " << origModel->checksum(false));
    LOG_DEBUG("restored checksum = " << restoredModel->checksum(false));
    CPPUNIT_ASSERT_EQUAL(origModel->checksum(false), restoredModel->checksum(false));
    CPPUNIT_ASSERT_EQUAL(origXml, newXml);
}

CppUnit::Test *CMetricPopulationModelTest::suite(void)
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CMetricPopulationModelTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricPopulationModelTest>(
                                   "CMetricPopulationModelTest::testBasicAccessors",
                                   &CMetricPopulationModelTest::testBasicAccessors) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricPopulationModelTest>(
                                   "CMetricPopulationModelTest::testMinMaxAndMean",
                                   &CMetricPopulationModelTest::testMinMaxAndMean) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricPopulationModelTest>(
                                   "CMetricPopulationModelTest::testLatLong",
                                   &CMetricPopulationModelTest::testLatLong) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricPopulationModelTest>(
                                   "CMetricPopulationModelTest::testComputeProbability",
                                   &CMetricPopulationModelTest::testComputeProbability) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricPopulationModelTest>(
                                   "CMetricPopulationModelTest::testPrune",
                                   &CMetricPopulationModelTest::testPrune) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricPopulationModelTest>(
                                   "CMetricPopulationModelTest::testKey",
                                   &CMetricPopulationModelTest::testKey) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricPopulationModelTest>(
                                   "CMetricPopulationModelTest::testFrequency",
                                   &CMetricPopulationModelTest::testFrequency) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricPopulationModelTest>(
                                   "CMetricPopulationModelTest::testSampleRateWeight",
                                   &CMetricPopulationModelTest::testSampleRateWeight) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricPopulationModelTest>(
                                   "CMetricPopulationModelTest::testPeriodicity",
                                   &CMetricPopulationModelTest::testPeriodicity) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricPopulationModelTest>(
                                   "CMetricPopulationModelTest::testPersistence",
                                   &CMetricPopulationModelTest::testPersistence) );

    return suiteOfTests;
}
