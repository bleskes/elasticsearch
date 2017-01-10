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

#include "CMetricModelTest.h"

#include <core/CContainerPrinter.h>
#include <core/CLogger.h>
#include <core/CoreTypes.h>
#include <core/CRapidXmlParser.h>
#include <core/CRapidXmlStatePersistInserter.h>
#include <core/CRapidXmlStateRestoreTraverser.h>

#include <maths/CBasicStatistics.h>
#include <maths/CCounter.h>
#include <maths/CModelWeight.h>
#include <maths/CMultivariatePrior.h>
#include <maths/CPrior.h>
#include <maths/CSampling.h>
#include <maths/CTimeSeriesTestData.h>

#include <model/CAnnotatedProbability.h>
#include <model/CDataGatherer.h>
#include <model/CEventData.h>
#include <model/CMetricModel.h>
#include <model/CMetricModelFactory.h>
#include <model/CMetricPopulationModel.h>
#include <model/CMetricPopulationModelFactory.h>
#include <model/CModelConfig.h>
#include <model/CModelDetailsView.h>
#include <model/CPartitioningFields.h>
#include <model/CResourceMonitor.h>
#include <model/ModelTypes.h>

#include <test/CRandomNumbers.h>

#include <boost/any.hpp>
#include <boost/optional.hpp>
#include <boost/range.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/tuple/tuple.hpp>

#include <vector>
#include <utility>

using namespace ml;
using namespace model;

namespace
{

typedef std::pair<double, double> TDoubleDoublePr;
typedef std::pair<std::size_t, double> TSizeDoublePr;
typedef std::pair<double, std::size_t> TDoubleSizePr;
typedef std::vector<double> TDoubleVec;
typedef std::vector<TDoubleVec> TDoubleVecVec;
typedef std::vector<TDoubleDoublePr> TDoubleDoublePrVec;
typedef std::vector<std::string> TStrVec;
typedef boost::optional<uint64_t> TOptionalUInt64;
typedef boost::optional<double> TOptionalDouble;
typedef std::vector<TOptionalDouble> TOptionalDoubleVec;
typedef boost::optional<std::string> TOptionalStr;
typedef std::pair<core_t::TTime, double> TTimeDoublePr;
typedef boost::optional<TTimeDoublePr> TOptionalTimeDoublePr;
typedef maths::CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;
typedef maths::CBasicStatistics::COrderStatisticsStack<double, 1u> TMinAccumulator;
typedef maths::CBasicStatistics::COrderStatisticsStack<double, 1u, std::greater<double> > TMaxAccumulator;
typedef boost::shared_ptr<maths::CPrior> TPriorPtr;
typedef boost::shared_ptr<maths::CMultivariatePrior> TMultivariatePriorPtr;
typedef std::pair<double, std::string> TDoubleStrPr;
typedef core::CSmallVector<double, 1> TDouble1Vec;
typedef core::CSmallVector<double, 4> TDouble4Vec;
typedef core::CSmallVector<TDouble4Vec, 1> TDouble4Vec1Vec;
typedef std::pair<std::size_t, double> TSizeDoublePr;
typedef core::CSmallVector<TSizeDoublePr, 1> TSizeDoublePr1Vec;
typedef std::vector<std::string> TStrVec;
typedef std::pair<core_t::TTime, TStrVec> TTimeStrVecPr;
typedef std::vector<TTimeStrVecPr> TTimeStrVecPrVec;

const std::string EMPTY_STRING;

class CTimeLess
{
    public:
        bool operator()(const CEventData &lhs,
                        const CEventData &rhs) const
        {
            return lhs.time() < rhs.time();
        }
};

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

void makeModel(CMetricModelFactory &factory,
               const CDataGatherer::TFeatureVec &features,
               core_t::TTime startTime,
               core_t::TTime bucketLength,
               CModelFactory::TDataGathererPtr &gatherer,
               CModel::TModelPtr &model,
               unsigned int *sampleCount = 0)
{
    factory.features(features);
    factory.bucketLength(bucketLength);
    factory.extraDataConversionFuncs(dummyAnyToPersistInserter,
                                     dummyRestoreFunc,
                                     model_t::TAnyMemoryFunc());
    CModelFactory::SGathererInitializationData gathererInitData(startTime);
    if (sampleCount)
    {
        gathererInitData.s_SampleOverrideCount = *sampleCount;
    }
    gatherer.reset(factory.makeDataGatherer(gathererInitData));
    CModelFactory::SModelInitializationData initData(gatherer);
    model.reset(factory.makeModel(initData));
    CPPUNIT_ASSERT(model);
    CPPUNIT_ASSERT_EQUAL(model_t::E_MetricOnline, model->category());
    CPPUNIT_ASSERT_EQUAL(bucketLength, model->bucketLength());
}

std::size_t addPerson(const std::string &p,
                      const CModelFactory::TDataGathererPtr &gatherer)
{
    CDataGatherer::TStrCPtrVec person;
    person.push_back(&p);
    person.resize(gatherer->fieldsOfInterest().size(), 0);
    CEventData result;
    CResourceMonitor resourceMonitor;
    gatherer->processFields(person, result, resourceMonitor);
    return *result.personId();
}

void addArrival(CDataGatherer &gatherer,
                core_t::TTime time,
                const std::string &person,
                double value,
                const TOptionalStr &inf1 = TOptionalStr(),
                const TOptionalStr &inf2 = TOptionalStr(),
                const TOptionalStr &count = TOptionalStr())
{
    CDataGatherer::TStrCPtrVec fieldValues;
    fieldValues.push_back(&person);
    if (inf1)
    {
        fieldValues.push_back(&(inf1.get()));
    }
    if (inf2)
    {
        fieldValues.push_back(&(inf2.get()));
    }
    if (count)
    {
        fieldValues.push_back(&(count.get()));
    }
    std::string valueAsString(core::CStringUtils::typeToStringPrecise(
                                  value,
                                  core::CIEEE754::E_DoublePrecision));
    fieldValues.push_back(&valueAsString);

    CEventData eventData;
    eventData.time(time);

    CResourceMonitor resourceMonitor;

    gatherer.addArrival(fieldValues, eventData, resourceMonitor);
}

void addArrival(CDataGatherer &gatherer,
                core_t::TTime time,
                const std::string &person,
                double lat,
                double lng,
                const TOptionalStr &inf1 = TOptionalStr(),
                const TOptionalStr &inf2 = TOptionalStr())
{
    CDataGatherer::TStrCPtrVec fieldValues;
    fieldValues.push_back(&person);
    if (inf1)
    {
        fieldValues.push_back(&(inf1.get()));
    }
    if (inf2)
    {
        fieldValues.push_back(&(inf2.get()));
    }
    std::string valueAsString;
    valueAsString += core::CStringUtils::typeToStringPrecise(
                         lat,
                         core::CIEEE754::E_DoublePrecision);
    valueAsString += model::CModelConfig::DEFAULT_MULTIVARIATE_COMPONENT_DELIMITER;
    valueAsString += core::CStringUtils::typeToStringPrecise(
                         lng,
                         core::CIEEE754::E_DoublePrecision);
    fieldValues.push_back(&valueAsString);

    CEventData eventData;
    eventData.time(time);

    CResourceMonitor resourceMonitor;

    gatherer.addArrival(fieldValues, eventData, resourceMonitor);
}

CEventData makeEventData(core_t::TTime time,
                         std::size_t pid,
                         double value,
                         const TOptionalStr &influence = TOptionalStr())
{
    CEventData result;
    result.time(time);
    result.person(pid);
    result.addAttribute(std::size_t(0));
    result.addValue(TDouble1Vec(1, value));
    result.addInfluence(influence);
    return result;
}

TDouble1Vec featureData(const CMetricModel &model,
                        model_t::EFeature feature,
                        std::size_t pid,
                        core_t::TTime time)
{
    const CMetricModel::TFeatureData *data = model.featureData(feature, pid, time);
    if (!data)
    {
        return TDouble1Vec();
    }
    return data->s_BucketValue ? data->s_BucketValue->value() : TDouble1Vec();
}

TDouble1Vec multivariateFeatureData(const CMetricModel &model,
                                    model_t::EFeature feature,
                                    std::size_t pid,
                                    core_t::TTime time)
{
    const CMetricModel::TFeatureData *data = model.featureData(feature, pid, time);
    if (!data)
    {
        return TDouble1Vec();
    }
    return data->s_BucketValue ? data->s_BucketValue->value() : TDouble1Vec();
}

void processBucket(core_t::TTime time,
                   core_t::TTime bucketLength,
                   std::size_t n,
                   const double *bucket,
                   const std::string *influencerValues,
                   CDataGatherer &gatherer,
                   CMetricModel &model,
                   SAnnotatedProbability &probability)
{
    CResourceMonitor resourceMonitor;
    for (std::size_t i = 0u; i < n; ++i)
    {
        addArrival(gatherer, time, "p", bucket[i], TOptionalStr(influencerValues[i]));
    }
    model.sample(time, time + bucketLength, resourceMonitor);
    CPartitioningFields partitioningFields(EMPTY_STRING, EMPTY_STRING);
    model.computeProbability(0/*pid*/, time, time + bucketLength,
                             partitioningFields, 1, probability);
    LOG_DEBUG("influences = " << core::CContainerPrinter::print(probability.s_Influences));
}

void processBucket(core_t::TTime time,
                   core_t::TTime bucketLength,
                   std::size_t n,
                   const double *bucket,
                   CDataGatherer &gatherer,
                   CMetricModel &model,
                   SAnnotatedProbability &probability,
                   SAnnotatedProbability &probability2)
{
    CResourceMonitor resourceMonitor;
    const std::string person("p");
    const std::string person2("q");
    for (std::size_t i = 0u; i < n; ++i)
    {
        CDataGatherer::TStrCPtrVec fieldValues;
        if (i % 2 == 0)
        {
            fieldValues.push_back(&person);
        }
        else
        {
            fieldValues.push_back(&person2);
        }

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
    model.computeProbability(0/*pid*/, time, time + bucketLength,
                             partitioningFields, 1, probability);
    model.computeProbability(1/*pid*/, time, time + bucketLength,
                             partitioningFields, 1, probability2);
}

const maths_t::TWeightStyleVec COUNT_WEIGHT(1, maths_t::E_SampleCountWeight);
const TDouble4Vec1Vec UNIT_WEIGHT(1, TDouble4Vec(1, 1.0));
const TSizeDoublePr1Vec NO_CORRELATES;

}

void CMetricModelTest::testOnlineSample(void)
{
    LOG_DEBUG("*** testOnlineSample ***");

    maths::CScopeDisableNormalizeOnRestore disabler;

    core_t::TTime startTime(45);
    core_t::TTime bucketLength(5);
    SModelParams params(bucketLength);
    params.s_MaximumUpdatesPerBucket = 0.0;
    CMetricModelFactory factory(params);
    CResourceMonitor resourceMonitor;

    // Check basic sampling.
    {
        TTimeDoublePr data[] =
            {
                TTimeDoublePr(49, 1.5),
                TTimeDoublePr(60, 1.3),
                TTimeDoublePr(61, 1.3),
                TTimeDoublePr(62, 1.6),
                TTimeDoublePr(65, 1.7),
                TTimeDoublePr(66, 1.33),
                TTimeDoublePr(68, 1.5),
                TTimeDoublePr(84, 1.58),
                TTimeDoublePr(87, 1.99),
                TTimeDoublePr(157, 1.6),
                TTimeDoublePr(164, 1.66),
                TTimeDoublePr(199, 1.28),
                TTimeDoublePr(202, 1.0),
                TTimeDoublePr(204, 1.5)
            };

        unsigned int sampleCounts[] = { 2, 1 };
        unsigned int expectedSampleCounts[] = { 2, 1 };

        for (std::size_t i = 0; i < boost::size(sampleCounts); ++i)
        {
            CDataGatherer::TFeatureVec features;
            features.push_back(model_t::E_IndividualMeanByPerson);
            features.push_back(model_t::E_IndividualMinByPerson);
            features.push_back(model_t::E_IndividualMaxByPerson);
            CModelFactory::TDataGathererPtr gatherer;
            CModel::TModelPtr model_;
            makeModel(factory, features, startTime, bucketLength, gatherer, model_, &sampleCounts[i]);
            CMetricModel &model = static_cast<CMetricModel&>(*model_.get());
            CPPUNIT_ASSERT_EQUAL(std::size_t(0), addPerson("p", gatherer));

            // Bucket values.
            maths::CCounter expectedCounter(startTime, bucketLength);
            TMeanAccumulator baselineMeanError;
            TMeanAccumulator expectedMean;
            TMeanAccumulator expectedBaselineMean;
            TMinAccumulator expectedMin;
            TMaxAccumulator expectedMax;

            // Sampled values.
            TMeanAccumulator expectedMeanSample;
            TMinAccumulator expectedMinSample;
            TMaxAccumulator expectedMaxSample;
            std::size_t numberSamples = 0u;
            TDouble1Vec expectedMeanSamples;
            TDouble1Vec expectedMinSamples;
            TDouble1Vec expectedMaxSamples;
            TPriorPtr expectedMeanPrior = factory.defaultPrior(model_t::E_IndividualMeanByPerson);
            TPriorPtr expectedMinPrior  = factory.defaultPrior(model_t::E_IndividualMinByPerson);
            TPriorPtr expectedMaxPrior  = factory.defaultPrior(model_t::E_IndividualMaxByPerson);

            std::size_t j = 0;
            core_t::TTime time = startTime;
            for (;;)
            {
                if (j < boost::size(data) && data[j].first < time + bucketLength)
                {
                    LOG_DEBUG("Adding " << data[j].second << " at " << data[j].first);

                    addArrival(*gatherer, data[j].first, "p", data[j].second);

                    expectedCounter.addArrival(data[j].first);
                    expectedMean.add(data[j].second);
                    expectedMin.add(data[j].second);
                    expectedMax.add(data[j].second);

                    expectedMeanSample.add(data[j].second);
                    expectedMinSample.add(data[j].second);
                    expectedMaxSample.add(data[j].second);

                    ++j;

                    if (j % expectedSampleCounts[i] == 0)
                    {
                        ++numberSamples;
                        expectedMeanSamples.push_back(maths::CBasicStatistics::mean(expectedMeanSample));
                        expectedMinSamples.push_back(expectedMinSample[0]);
                        expectedMaxSamples.push_back(expectedMaxSample[0]);
                        expectedMeanSample = TMeanAccumulator();
                        expectedMinSample  = TMinAccumulator();
                        expectedMaxSample  = TMaxAccumulator();
                    }
                }
                else
                {
                    LOG_DEBUG("Sampling [" << time << ", " << time + bucketLength << ")");

                    model.sample(time, time + bucketLength, resourceMonitor);
                    if (maths::CBasicStatistics::count(expectedMean) > 0.0)
                    {
                        expectedBaselineMean.add(maths::CBasicStatistics::mean(expectedMean));
                    }
                    if (numberSamples > 0)
                    {
                        LOG_DEBUG("Adding mean samples = " << core::CContainerPrinter::print(expectedMeanSamples)
                                  << ", min samples = " << core::CContainerPrinter::print(expectedMinSamples)
                                  << ", max samples = " << core::CContainerPrinter::print(expectedMaxSamples));

                        expectedMeanPrior->dataType(maths_t::E_ContinuousData);
                        expectedMinPrior->dataType(maths_t::E_ContinuousData);
                        expectedMaxPrior->dataType(maths_t::E_ContinuousData);
                        expectedMeanPrior->addSamples(COUNT_WEIGHT,
                                                      expectedMeanSamples,
                                                      TDouble4Vec1Vec(expectedMeanSamples.size(), TDouble4Vec(1, 1.0)));
                        expectedMinPrior->addSamples(COUNT_WEIGHT,
                                                     expectedMinSamples,
                                                     TDouble4Vec1Vec(expectedMinSamples.size(), TDouble4Vec(1, 1.0)));
                        expectedMaxPrior->addSamples(COUNT_WEIGHT,
                                                     expectedMaxSamples,
                                                     TDouble4Vec1Vec(expectedMaxSamples.size(), TDouble4Vec(1, 1.0)));
                        numberSamples = 0u;
                        expectedMeanSamples.clear();
                        expectedMinSamples.clear();
                        expectedMaxSamples.clear();
                    }

                    model_t::CResultType type(model_t::CResultType::E_Unconditional | model_t::CResultType::E_Final);
                    TOptionalUInt64 currentCount = model.currentBucketCount(0, time);
                    TDouble1Vec bucketMean   = model.currentBucketValue(model_t::E_IndividualMeanByPerson, 0, 0, time);
                    TDouble1Vec baselineMean = model.baselineBucketMean(model_t::E_IndividualMeanByPerson, 0, 0,
                                                                        type, NO_CORRELATES, time);

                    LOG_DEBUG("bucket count = " << core::CContainerPrinter::print(currentCount));
                    LOG_DEBUG("current bucket mean = " << core::CContainerPrinter::print(bucketMean)
                              << ", expected baseline bucket mean = " << maths::CBasicStatistics::mean(expectedBaselineMean)
                              << ", baseline bucket mean = " << core::CContainerPrinter::print(baselineMean));

                    CPPUNIT_ASSERT(currentCount);
                    CPPUNIT_ASSERT_EQUAL(expectedCounter.currentBucketCount(time), *currentCount);

                    TDouble1Vec mean = maths::CBasicStatistics::count(expectedMean) > 0.0 ?
                                       TDouble1Vec(1, maths::CBasicStatistics::mean(expectedMean)) :
                                       TDouble1Vec();
                    TDouble1Vec min  = expectedMin.count() > 0 ?
                                       TDouble1Vec(1, expectedMin[0]) : TDouble1Vec();
                    TDouble1Vec max  = expectedMax.count() > 0 ?
                                       TDouble1Vec(1, expectedMax[0]) : TDouble1Vec();

                    CPPUNIT_ASSERT(mean == bucketMean);
                    if (!baselineMean.empty())
                    {
                        baselineMeanError.add(::fabs(baselineMean[0] - maths::CBasicStatistics::mean(expectedBaselineMean)));
                    }

                    CPPUNIT_ASSERT(mean == featureData(model, model_t::E_IndividualMeanByPerson, 0, time));
                    CPPUNIT_ASSERT(min  == featureData(model, model_t::E_IndividualMinByPerson, 0, time));
                    CPPUNIT_ASSERT(max  == featureData(model, model_t::E_IndividualMaxByPerson, 0, time));

                    CPPUNIT_ASSERT_EQUAL(expectedMeanPrior->checksum(),
                                         model.details()->prior(model_t::E_IndividualMeanByPerson, 0)->checksum());
                    CPPUNIT_ASSERT_EQUAL(expectedMinPrior->checksum(),
                                         model.details()->prior(model_t::E_IndividualMinByPerson, 0)->checksum());
                    CPPUNIT_ASSERT_EQUAL(expectedMaxPrior->checksum(),
                                         model.details()->prior(model_t::E_IndividualMaxByPerson, 0)->checksum());

                    // Test persistence. (We check for idempotency.)
                    std::string origXml;
                    {
                        core::CRapidXmlStatePersistInserter inserter("root");
                        model.acceptPersistInserter(inserter);
                        inserter.toXml(origXml);
                    }

                    // Restore the XML into a new filter
                    core::CRapidXmlParser parser;
                    CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(origXml));
                    core::CRapidXmlStateRestoreTraverser traverser(parser);

                    CModelFactory::SModelInitializationData initData(gatherer);
                    CModel::TModelPtr restoredModel(factory.makeModel(initData, traverser));

                    // The XML representation of the new filter should be the same as the original
                    std::string newXml;
                    {
                        ml::core::CRapidXmlStatePersistInserter inserter("root");
                        restoredModel->acceptPersistInserter(inserter);
                        inserter.toXml(newXml);
                    }

                    uint64_t origChecksum = model.checksum(false);
                    LOG_DEBUG("original checksum = " << origChecksum);
                    uint64_t restoredChecksum = restoredModel->checksum(false);
                    LOG_DEBUG("restored checksum = " << restoredChecksum);
                    CPPUNIT_ASSERT_EQUAL(origChecksum, restoredChecksum);
                    CPPUNIT_ASSERT_EQUAL(origXml, newXml);

                    expectedMean = TMeanAccumulator();
                    expectedMin  = TMinAccumulator();
                    expectedMax  = TMaxAccumulator();

                    if (j >= boost::size(data))
                    {
                        break;
                    }

                    time += bucketLength;
                }
            }
            LOG_DEBUG("baseline mean error = " << maths::CBasicStatistics::mean(baselineMeanError));
            CPPUNIT_ASSERT(maths::CBasicStatistics::mean(baselineMeanError) < 0.25);
        }
    }

    // Check we correctly handle negative values.
    {
        CDataGatherer::TFeatureVec features(1, model_t::E_IndividualMeanByPerson);
        CModelFactory::TDataGathererPtr gatherer;
        CModel::TModelPtr model_;
        unsigned int sampleCount = 1;
        makeModel(factory, features, startTime, bucketLength, gatherer, model_, &sampleCount);
        CMetricModel &model = static_cast<CMetricModel&>(*model_.get());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), addPerson("p", gatherer));

        TTimeDoublePr data[] =
            {
                TTimeDoublePr(45, 1.0),
                TTimeDoublePr(46, 0.3),
                TTimeDoublePr(48, 0.8),
                TTimeDoublePr(49, 0.5),
                TTimeDoublePr(50, 1.2),
                TTimeDoublePr(51, 0.1),
                TTimeDoublePr(52, 0.2),
                TTimeDoublePr(53, 0.5),
                TTimeDoublePr(54, 1.3),
                TTimeDoublePr(55, 0.9),
                TTimeDoublePr(56, 1.6),
                TTimeDoublePr(58, 0.7),
                TTimeDoublePr(59, 0.9),
                TTimeDoublePr(60, 0.8),
                TTimeDoublePr(61, 1.4),
                TTimeDoublePr(62, 1.2),
                TTimeDoublePr(63, 0.3),
                TTimeDoublePr(64, 0.9),
                TTimeDoublePr(65, -1.19),
                TTimeDoublePr(66, 0.4)
            };

        core_t::TTime time = startTime;

        for (std::size_t i = 0u; i < boost::size(data); ++i)
        {
            if (data[i].first >= time + bucketLength)
            {
                LOG_DEBUG("Sampling [" << time << ", " << time + bucketLength << ")");
                model.sample(time, time + bucketLength, resourceMonitor);
                time += bucketLength;
            }

            LOG_DEBUG("Adding " << data[i].second << " at " << data[i].first);
            addArrival(*gatherer, data[i].first, "p", data[i].second);
        }

        LOG_DEBUG("Sampling [" << time << ", " << time + bucketLength << ")");
        model.sample(time, time + bucketLength, resourceMonitor);

        TPriorPtr expectedPrior(factory.defaultPrior(model_t::E_IndividualMeanByPerson, 2.8, 0.0, 0.01, 12.0, 0.8));
        for (std::size_t i = 0u; i < boost::size(data); ++i)
        {
            expectedPrior->addSamples(COUNT_WEIGHT, TDouble1Vec(1, data[i].second), UNIT_WEIGHT);
        }
        const maths::CPrior *prior = model.details()->prior(model_t::E_IndividualMeanByPerson, 0);

        double confidenceIntervals[] = { 25.0, 50.0, 75.0, 99.0 };
        for (std::size_t i = 0; i < boost::size(confidenceIntervals); ++i)
        {
            TDoubleDoublePr expectedInterval =
                    expectedPrior->marginalLikelihoodConfidenceInterval(confidenceIntervals[i]);
            TDoubleDoublePr interval =
                    prior->marginalLikelihoodConfidenceInterval(confidenceIntervals[i]);

            LOG_DEBUG("Testing " << confidenceIntervals[i] << "% interval");
            LOG_DEBUG("expected interval = " << core::CContainerPrinter::print(expectedInterval));
            LOG_DEBUG("Interval          = " << core::CContainerPrinter::print(interval));
            CPPUNIT_ASSERT_DOUBLES_EQUAL(expectedInterval.first,
                                         interval.first,
                                         0.07 * (expectedInterval.second - expectedInterval.first));
            CPPUNIT_ASSERT_DOUBLES_EQUAL(expectedInterval.second,
                                         interval.second,
                                         0.07 * (expectedInterval.second - expectedInterval.first));
        }
    }
}

void CMetricModelTest::testOnlineMultivariateSample(void)
{
    LOG_DEBUG("*** testOnlineMultivariateSample ***");

    typedef std::vector<TDoubleVecVec> TDoubleVecVecVec;
    typedef maths::CVectorNx1<double, 2> TVector2;
    typedef maths::CBasicStatistics::SSampleMean<TVector2>::TAccumulator TMean2Accumulator;
    typedef std::pair<core_t::TTime, boost::array<double, 2> > TTimeDouble2AryPr;
    typedef std::vector<TTimeDouble2AryPr> TTimeDouble2AryPrVec;

    core_t::TTime startTime(45);
    core_t::TTime bucketLength(5);
    SModelParams params(bucketLength);
    params.s_MaximumUpdatesPerBucket = 0.0;
    CMetricModelFactory factory(params);
    CResourceMonitor resourceMonitor;

    double data_[][3] =
        {
            { 49,  1.5,  1.1 },
            { 60,  1.3,  1.2 },
            { 61,  1.3,  2.1 },
            { 62,  1.6,  1.5 },
            { 65,  1.7,  1.4 },
            { 66,  1.33, 1.6 },
            { 68,  1.5,  1.37},
            { 84,  1.58, 1.42},
            { 87,  1.99, 2.2 },
            { 157, 1.6,  1.6 },
            { 164, 1.66, 1.55},
            { 199, 1.28, 1.4 },
            { 202, 1.0,  0.7 },
            { 204, 1.5,  1.8 }
        };
    TTimeDouble2AryPrVec data;
    for (std::size_t i = 0u; i < boost::size(data_); ++i)
    {
        boost::array<double, 2> values = { { data_[i][1], data_[i][2] } };
        data.push_back(TTimeDouble2AryPr(static_cast<core_t::TTime>(data_[i][0]), values));
    }

    unsigned int sampleCounts[] = { 2u, 1u };
    unsigned int expectedSampleCounts[] = { 2u, 1u };

    for (std::size_t i = 0; i < boost::size(sampleCounts); ++i)
    {
        LOG_DEBUG("*** sample count = " << sampleCounts[i] << " ***");

        CDataGatherer::TFeatureVec features(1, model_t::E_IndividualMeanLatLongByPerson);
        CModelFactory::TDataGathererPtr gatherer;
        CModel::TModelPtr model_;
        makeModel(factory, features, startTime, bucketLength, gatherer, model_, &sampleCounts[i]);
        CMetricModel &model = static_cast<CMetricModel&>(*model_.get());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), addPerson("p", gatherer));

        // Bucket values.
        maths::CCounter expectedCounter(startTime, bucketLength);
        TMean2Accumulator baselineLatLongError;
        TMean2Accumulator expectedLatLong;
        TMean2Accumulator expectedBaselineLatLong;

        // Sampled values.
        TMean2Accumulator expectedLatLongSample;
        std::size_t numberSamples = 0u;
        TDoubleVecVec expectedLatLongSamples;
        TMultivariatePriorPtr expectedMeanPrior = factory.defaultMultivariatePrior(model_t::E_IndividualMeanLatLongByPerson);

        std::size_t j = 0;
        core_t::TTime time = startTime;
        for (;;)
        {
            if (j < data.size() && data[j].first < time + bucketLength)
            {
                LOG_DEBUG("Adding " << data[j].second[0] << "," << data[j].second[1] << " at " << data[j].first);

                addArrival(*gatherer, data[j].first, "p", data[j].second[0], data[j].second[1]);

                expectedCounter.addArrival(data[j].first);

                expectedLatLong.add(TVector2(data[j].second));
                expectedLatLongSample.add(TVector2(data[j].second));

                ++j;

                if (j % expectedSampleCounts[i] == 0)
                {
                    ++numberSamples;
                    expectedLatLongSamples.push_back(
                            TDoubleVec(maths::CBasicStatistics::mean(expectedLatLongSample).begin(),
                                       maths::CBasicStatistics::mean(expectedLatLongSample).end()));
                    expectedLatLongSample = TMean2Accumulator();
                }
            }
            else
            {
                LOG_DEBUG("Sampling [" << time << ", " << time + bucketLength << ")");

                model.sample(time, time + bucketLength, resourceMonitor);
                if (maths::CBasicStatistics::count(expectedLatLong) > 0.0)
                {
                    expectedBaselineLatLong.add(maths::CBasicStatistics::mean(expectedLatLong));
                }
                if (numberSamples > 0)
                {
                    LOG_DEBUG("Adding mean samples = " << core::CContainerPrinter::print(expectedLatLongSamples));

                    expectedMeanPrior->dataType(maths_t::E_ContinuousData);
                    expectedMeanPrior->addSamples(COUNT_WEIGHT,
                                                  expectedLatLongSamples,
                                                  TDoubleVecVecVec(expectedLatLongSamples.size(),
                                                                   TDoubleVecVec(1, TDoubleVec(2, 1.0))));
                    numberSamples = 0u;
                    expectedLatLongSamples.clear();
                }

                model_t::CResultType type(model_t::CResultType::E_Unconditional | model_t::CResultType::E_Final);
                TOptionalUInt64 currentCount = model.currentBucketCount(0, time);
                TDouble1Vec bucketLatLong    = model.currentBucketValue(model_t::E_IndividualMeanLatLongByPerson, 0, 0, time);
                TDouble1Vec baselineLatLong  = model.baselineBucketMean(model_t::E_IndividualMeanLatLongByPerson, 0, 0,
                                                                        type, NO_CORRELATES, time);

                LOG_DEBUG("bucket count = " << core::CContainerPrinter::print(currentCount));
                LOG_DEBUG("current bucket mean = " << core::CContainerPrinter::print(bucketLatLong)
                          << ", expected baseline bucket mean = " << maths::CBasicStatistics::mean(expectedBaselineLatLong)
                          << ", baseline bucket mean = " << core::CContainerPrinter::print(baselineLatLong));

                CPPUNIT_ASSERT(currentCount);
                CPPUNIT_ASSERT_EQUAL(expectedCounter.currentBucketCount(time), *currentCount);

                TDouble1Vec latLong;
                if (maths::CBasicStatistics::count(expectedLatLong) > 0.0)
                {
                    latLong.push_back(maths::CBasicStatistics::mean(expectedLatLong)(0));
                    latLong.push_back(maths::CBasicStatistics::mean(expectedLatLong)(1));
                }
                CPPUNIT_ASSERT(latLong == bucketLatLong);
                if (!baselineLatLong.empty())
                {
                    baselineLatLongError.add(fabs(  TVector2(baselineLatLong)
                                                  - maths::CBasicStatistics::mean(expectedBaselineLatLong)));
                }
                CPPUNIT_ASSERT(latLong == multivariateFeatureData(model, model_t::E_IndividualMeanLatLongByPerson, 0, time));
                CPPUNIT_ASSERT_EQUAL(expectedMeanPrior->checksum(),
                                     model.details()->multivariatePrior(model_t::E_IndividualMeanLatLongByPerson, 0)->checksum());

                // Test persistence. (We check for idempotency.)
                std::string origXml;
                {
                    core::CRapidXmlStatePersistInserter inserter("root");
                    model.acceptPersistInserter(inserter);
                    inserter.toXml(origXml);
                }

                // Restore the XML into a new filter
                core::CRapidXmlParser parser;
                CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(origXml));
                core::CRapidXmlStateRestoreTraverser traverser(parser);

                CModelFactory::SModelInitializationData initData(gatherer);
                CModel::TModelPtr restoredModel(factory.makeModel(initData, traverser));

                // The XML representation of the new filter should be the same as the original
                std::string newXml;
                {
                    ml::core::CRapidXmlStatePersistInserter inserter("root");
                    restoredModel->acceptPersistInserter(inserter);
                    inserter.toXml(newXml);
                }

                uint64_t origChecksum = model.checksum(false);
                LOG_DEBUG("original checksum = " << origChecksum);
                uint64_t restoredChecksum = restoredModel->checksum(false);
                LOG_DEBUG("restored checksum = " << restoredChecksum);
                CPPUNIT_ASSERT_EQUAL(origChecksum, restoredChecksum);
                CPPUNIT_ASSERT_EQUAL(origXml, newXml);

                expectedLatLong = TMean2Accumulator();

                if (j >= boost::size(data))
                {
                    break;
                }

                time += bucketLength;
            }
        }
        LOG_DEBUG("baseline mean error = " << maths::CBasicStatistics::mean(baselineLatLongError));
        CPPUNIT_ASSERT(maths::CBasicStatistics::mean(baselineLatLongError)(0) < 0.25);
        CPPUNIT_ASSERT(maths::CBasicStatistics::mean(baselineLatLongError)(1) < 0.25);
    }
}

void CMetricModelTest::testOnlineProbabilityCalculationForMetric(void)
{
    LOG_DEBUG("*** testOnlineProbabilityCalculationForMetric ***");

    typedef maths::CBasicStatistics::COrderStatisticsHeap<TDoubleSizePr> TMinAccumulator;

    core_t::TTime startTime(0);
    core_t::TTime bucketLength(10);
    SModelParams params(bucketLength);
    CMetricModelFactory factory(params);

    std::size_t bucketCounts[] = { 5, 6, 3, 5, 0, 7, 8, 5, 4, 3, 5, 5, 6 };

    double mean = 5.0;
    double variance = 2.0;
    std::size_t anomalousBucket = 12u;
    double anomaly = 5 * ::sqrt(variance);

    CDataGatherer::TFeatureVec features;
    features.push_back(model_t::E_IndividualMeanByPerson);
    features.push_back(model_t::E_IndividualMinByPerson);
    features.push_back(model_t::E_IndividualMaxByPerson);
    CModelFactory::TDataGathererPtr gatherer;
    CModel::TModelPtr model_;
    makeModel(factory, features, startTime, bucketLength, gatherer, model_);
    CMetricModel &model = static_cast<CMetricModel&>(*model_.get());
    CPPUNIT_ASSERT_EQUAL(std::size_t(0), addPerson("p", gatherer));
    CResourceMonitor resourceMonitor;

    TMinAccumulator minProbabilities(2u);

    test::CRandomNumbers rng;

    core_t::TTime time = startTime;
    for (std::size_t i = 0u; i < boost::size(bucketCounts); ++i)
    {
        LOG_DEBUG("Processing bucket [" << time << ", " << time + bucketLength << ")");

        TDoubleVec values;
        rng.generateNormalSamples(mean, variance, bucketCounts[i], values);
        LOG_DEBUG("values = " << core::CContainerPrinter::print(values));
        LOG_DEBUG("i = " << i << ", anomalousBucket = " << anomalousBucket
                  << ", offset = " << (i == anomalousBucket ? anomaly : 0.0));

        for (std::size_t j = 0u; j < values.size(); ++j)
        {
            addArrival(*gatherer, time + static_cast<core_t::TTime>(j), "p",
                       values[j] + (i == anomalousBucket ? anomaly : 0.0));
        }
        model.sample(time, time + bucketLength, resourceMonitor);

        CPartitioningFields partitioningFields(EMPTY_STRING, EMPTY_STRING);
        SAnnotatedProbability annotatedProbability;
        if (model.computeProbability(0/*pid*/, time, time + bucketLength,
                                     partitioningFields, 1, annotatedProbability) == false)
        {
            continue;
        }
        LOG_DEBUG("probability = " << annotatedProbability.s_Probability);
        if (*model.currentBucketCount(0, time) > 0)
        {
            minProbabilities.add(TDoubleSizePr(annotatedProbability.s_Probability, i));
        }
        time += bucketLength;
    }

    minProbabilities.sort();
    LOG_DEBUG("minProbabilities = "
              << core::CContainerPrinter::print(minProbabilities.begin(),
                                                minProbabilities.end()));
    CPPUNIT_ASSERT_EQUAL(anomalousBucket, minProbabilities[0].second);
    CPPUNIT_ASSERT(minProbabilities[0].first / minProbabilities[1].first < 0.05);
}

void CMetricModelTest::testOnlineProbabilityCalculationForMedian(void)
{
    LOG_DEBUG("*** testOnlineProbabilityCalculationForMedian ***");

    typedef maths::CBasicStatistics::COrderStatisticsHeap<TDoubleSizePr> TMinAccumulator;

    core_t::TTime startTime(0);
    core_t::TTime bucketLength(10);
    SModelParams params(bucketLength);
    CMetricModelFactory factory(params);

    std::size_t bucketCounts[] = { 5, 6, 3, 5, 0, 7, 8, 5, 4, 3, 5, 5, 6 };

    double mean = 5.0;
    double variance = 2.0;
    std::size_t anomalousBucket = 12u;

    CDataGatherer::TFeatureVec features(1, model_t::E_IndividualMedianByPerson);
    CModelFactory::TDataGathererPtr gatherer;
    CModel::TModelPtr model_;
    makeModel(factory, features, startTime, bucketLength, gatherer, model_);
    CMetricModel &model = static_cast<CMetricModel&>(*model_.get());
    CPPUNIT_ASSERT_EQUAL(std::size_t(0), addPerson("p", gatherer));
    CResourceMonitor resourceMonitor;

    TMinAccumulator minProbabilities(2u);

    test::CRandomNumbers rng;

    core_t::TTime time = startTime;
    for (std::size_t i = 0u; i < boost::size(bucketCounts); ++i)
    {
        LOG_DEBUG("Processing bucket [" << time << ", " << time + bucketLength << "]");
        LOG_DEBUG("i = " << i << ", anomalousBucket = " << anomalousBucket);


        TDoubleVec values;
        if (i == anomalousBucket)
        {
            values.push_back(0.0);
            values.push_back(mean * 3.0);
            values.push_back(mean * 3.0);
        }
        else
        {
            rng.generateNormalSamples(mean, variance, bucketCounts[i], values);
        }

        LOG_DEBUG("values = " << core::CContainerPrinter::print(values));

        for (std::size_t j = 0u; j < values.size(); ++j)
        {
            addArrival(*gatherer, time + static_cast<core_t::TTime>(j), "p", values[j]);
        }


        model.sample(time, time + bucketLength, resourceMonitor);

        CPartitioningFields partitioningFields(EMPTY_STRING, EMPTY_STRING);
        SAnnotatedProbability annotatedProbability;
        if (model.computeProbability(0/*pid*/, time, time + bucketLength,
                                     partitioningFields, 1, annotatedProbability) == false)
        {
            continue;
        }

        LOG_DEBUG("probability = " << annotatedProbability.s_Probability);
        if (*model.currentBucketCount(0, time) > 0)
        {
            minProbabilities.add(TDoubleSizePr(annotatedProbability.s_Probability, i));
        }
        time += bucketLength;
    }

    minProbabilities.sort();
    LOG_DEBUG("minProbabilities = "
              << core::CContainerPrinter::print(minProbabilities.begin(),
                                                minProbabilities.end()));
    CPPUNIT_ASSERT_EQUAL(anomalousBucket, minProbabilities[0].second);
    CPPUNIT_ASSERT(minProbabilities[0].first / minProbabilities[1].first < 0.05);



    std::size_t pid(0);
    const CMetricModel::TFeatureData *fd = model.featureData(
                                    ml::model_t::E_IndividualMedianByPerson, pid,
                                    time - bucketLength);

    // assert there is only 1 value in the last bucket and its the median
    CPPUNIT_ASSERT_EQUAL(fd->s_BucketValue->value()[0], mean * 3.0);
    CPPUNIT_ASSERT_EQUAL(fd->s_BucketValue->value().size(), std::size_t(1));
}

void CMetricModelTest::testOnlineProbabilityCalculationForLowMean(void)
{
    LOG_DEBUG("*** testOnlineProbabilityCalculationForLowMean ***");

    core_t::TTime startTime(0);
    core_t::TTime bucketLength(10);
    SModelParams params(bucketLength);
    CMetricModelFactory factory(params);

    std::size_t numberOfBuckets = 100;
    std::size_t bucketCount = 5;
    std::size_t lowMeanBucket = 60u;
    std::size_t highMeanBucket = 80u;

    double mean = 5.0;
    double variance = 0.00001;
    double lowMean = 2.0;
    double highMean = 10.0;

    CDataGatherer::TFeatureVec features(1, model_t::E_IndividualLowMeanByPerson);
    CModelFactory::TDataGathererPtr gatherer;
    CModel::TModelPtr model_;
    makeModel(factory, features, startTime, bucketLength, gatherer, model_);
    CMetricModel &model = static_cast<CMetricModel&>(*model_.get());
    CPPUNIT_ASSERT_EQUAL(std::size_t(0), addPerson("p", gatherer));
    CResourceMonitor resourceMonitor;

    TOptionalDoubleVec probabilities;
    test::CRandomNumbers rng;
    core_t::TTime time = startTime;
    for (std::size_t i = 0u; i < numberOfBuckets; ++i)
    {
        LOG_DEBUG("Processing bucket [" << time << ", " << time + bucketLength << ")");

        double meanForBucket = mean;
        if (i == lowMeanBucket)
        {
            meanForBucket = lowMean;
        }
        if (i == highMeanBucket)
        {
            meanForBucket = highMean;
        }
        TDoubleVec values;
        rng.generateNormalSamples(meanForBucket, variance, bucketCount, values);
        LOG_DEBUG("values = " << core::CContainerPrinter::print(values));

        for (std::size_t j = 0u; j < values.size(); ++j)
        {
            addArrival(*gatherer, time + static_cast<core_t::TTime>(j), "p", values[j]);
        }
        model.sample(time, time + bucketLength, resourceMonitor);

        CPartitioningFields partitioningFields(EMPTY_STRING, EMPTY_STRING);
        SAnnotatedProbability annotatedProbability;
        CPPUNIT_ASSERT(model.computeProbability(0/*pid*/, time, time + bucketLength,
                                                partitioningFields, 1, annotatedProbability));
        LOG_DEBUG("probability = " << annotatedProbability.s_Probability);
        probabilities.push_back(annotatedProbability.s_Probability);

        time += bucketLength;
    }

    LOG_DEBUG("probabilities = " << core::CContainerPrinter::print(probabilities.begin(),
            probabilities.end()));

    CPPUNIT_ASSERT(probabilities[lowMeanBucket] < 0.01);
    CPPUNIT_ASSERT(probabilities[highMeanBucket] > 0.1);
}

void CMetricModelTest::testOnlineProbabilityCalculationForHighMean(void)
{
    LOG_DEBUG("*** testOnlineProbabilityCalculationForHighMean ***");

    core_t::TTime startTime(0);
    core_t::TTime bucketLength(10);
    SModelParams params(bucketLength);
    CMetricModelFactory factory(params);

    std::size_t numberOfBuckets = 100;
    std::size_t bucketCount = 5;
    std::size_t lowMeanBucket = 60;
    std::size_t highMeanBucket = 80;

    double mean = 5.0;
    double variance = 0.00001;
    double lowMean = 2.0;
    double highMean = 10.0;

    CDataGatherer::TFeatureVec features(1, model_t::E_IndividualHighMeanByPerson);
    CModelFactory::TDataGathererPtr gatherer;
    CModel::TModelPtr model_;
    makeModel(factory, features, startTime, bucketLength, gatherer, model_);
    CMetricModel &model = static_cast<CMetricModel&>(*model_.get());
    CPPUNIT_ASSERT_EQUAL(std::size_t(0), addPerson("p", gatherer));
    CResourceMonitor resourceMonitor;

    TOptionalDoubleVec probabilities;
    test::CRandomNumbers rng;
    core_t::TTime time = startTime;
    for (std::size_t i = 0u; i < numberOfBuckets; ++i)
    {
        LOG_DEBUG("Processing bucket [" << time << ", " << time + bucketLength << ")");

        double meanForBucket = mean;
        if (i == lowMeanBucket)
        {
            meanForBucket = lowMean;
        }
        if (i == highMeanBucket)
        {
            meanForBucket = highMean;
        }
        TDoubleVec values;
        rng.generateNormalSamples(meanForBucket, variance, bucketCount, values);
        LOG_DEBUG("values = " << core::CContainerPrinter::print(values));

        for (std::size_t j = 0u; j < values.size(); ++j)
        {
            addArrival(*gatherer, time + static_cast<core_t::TTime>(j), "p", values[j]);
        }
        model.sample(time, time + bucketLength, resourceMonitor);

        CPartitioningFields partitioningFields(EMPTY_STRING, EMPTY_STRING);
        SAnnotatedProbability annotatedProbability;
        CPPUNIT_ASSERT(model.computeProbability(0/*pid*/, time, time + bucketLength,
                                                partitioningFields, 1, annotatedProbability));
        LOG_DEBUG("probability = " << annotatedProbability.s_Probability);
        probabilities.push_back(annotatedProbability.s_Probability);

        time += bucketLength;
    }

    LOG_DEBUG("probabilities = " << core::CContainerPrinter::print(probabilities));

    CPPUNIT_ASSERT(probabilities[lowMeanBucket] > 0.1);
    CPPUNIT_ASSERT(probabilities[highMeanBucket] < 0.01);
}

void CMetricModelTest::testOnlineProbabilityCalculationForLowSum(void)
{
    LOG_DEBUG("*** testOnlineProbabilityCalculationForLowSum ***");

    core_t::TTime startTime(0);
    core_t::TTime bucketLength(10);
    SModelParams params(bucketLength);
    CMetricModelFactory factory(params);

    std::size_t numberOfBuckets = 100;
    std::size_t bucketCount = 5;
    std::size_t lowSumBucket = 60u;
    std::size_t highSumBucket = 80u;

    double mean = 50.0;
    double variance = 5.0;
    double lowMean = 5.0;
    double highMean = 95.0;

    CDataGatherer::TFeatureVec features(1, model_t::E_IndividualLowSumByBucketAndPerson);
    CModelFactory::TDataGathererPtr gatherer;
    CModel::TModelPtr model_;
    makeModel(factory, features, startTime, bucketLength, gatherer, model_);
    CMetricModel &model = static_cast<CMetricModel&>(*model_.get());
    CPPUNIT_ASSERT_EQUAL(std::size_t(0), addPerson("p", gatherer));
    CResourceMonitor resourceMonitor;

    TOptionalDoubleVec probabilities;
    test::CRandomNumbers rng;
    core_t::TTime time = startTime;
    for (std::size_t i = 0u; i < numberOfBuckets; ++i)
    {
        LOG_DEBUG("Processing bucket [" << time << ", " << time + bucketLength << ")");

        double meanForBucket = mean;
        if (i == lowSumBucket)
        {
            meanForBucket = lowMean;
        }
        if (i == highSumBucket)
        {
            meanForBucket = highMean;
        }
        TDoubleVec values;
        rng.generateNormalSamples(meanForBucket, variance, bucketCount, values);
        LOG_DEBUG("values = " << core::CContainerPrinter::print(values));

        for (std::size_t j = 0u; j < values.size(); ++j)
        {
            addArrival(*gatherer, time + static_cast<core_t::TTime>(j), "p", values[j]);
        }
        model.sample(time, time + bucketLength, resourceMonitor);

        CPartitioningFields partitioningFields(EMPTY_STRING, EMPTY_STRING);
        SAnnotatedProbability annotatedProbability;
        CPPUNIT_ASSERT(model.computeProbability(0/*pid*/, time, time + bucketLength,
                                                partitioningFields, 1, annotatedProbability));
        LOG_DEBUG("probability = " << annotatedProbability.s_Probability);
        probabilities.push_back(annotatedProbability.s_Probability);

        time += bucketLength;
    }

    LOG_DEBUG("probabilities = " << core::CContainerPrinter::print(probabilities));
    CPPUNIT_ASSERT(probabilities[lowSumBucket] < 0.01);
    CPPUNIT_ASSERT(probabilities[highSumBucket] > 0.1);
}

void CMetricModelTest::testOnlineProbabilityCalculationForHighSum(void)
{
    LOG_DEBUG("*** testOnlineProbabilityCalculationForLowSum ***");

    core_t::TTime startTime(0);
    core_t::TTime bucketLength(10);
    SModelParams params(bucketLength);
    CMetricModelFactory factory(params);

    std::size_t numberOfBuckets = 100;
    std::size_t bucketCount = 5;
    std::size_t lowSumBucket = 60u;
    std::size_t highSumBucket = 80u;

    double mean = 50.0;
    double variance = 5.0;
    double lowMean = 5.0;
    double highMean = 95.0;

    CDataGatherer::TFeatureVec features(1, model_t::E_IndividualHighSumByBucketAndPerson);
    CModelFactory::TDataGathererPtr gatherer;
    CModel::TModelPtr model_;
    makeModel(factory, features, startTime, bucketLength, gatherer, model_);
    CMetricModel &model = static_cast<CMetricModel&>(*model_.get());
    CPPUNIT_ASSERT_EQUAL(std::size_t(0), addPerson("p", gatherer));
    CResourceMonitor resourceMonitor;

    TOptionalDoubleVec probabilities;
    test::CRandomNumbers rng;
    core_t::TTime time = startTime;
    for (std::size_t i = 0u; i < numberOfBuckets; ++i)
    {
        LOG_DEBUG("Processing bucket [" << time << ", " << time + bucketLength << ")");

        double meanForBucket = mean;
        if (i == lowSumBucket)
        {
            meanForBucket = lowMean;
        }
        if (i == highSumBucket)
        {
            meanForBucket = highMean;
        }
        TDoubleVec values;
        rng.generateNormalSamples(meanForBucket, variance, bucketCount, values);
        LOG_DEBUG("values = " << core::CContainerPrinter::print(values));

        for (std::size_t j = 0u; j < values.size(); ++j)
        {
            addArrival(*gatherer, time + static_cast<core_t::TTime>(j), "p", values[j]);
        }
        model.sample(time, time + bucketLength, resourceMonitor);

        CPartitioningFields partitioningFields(EMPTY_STRING, EMPTY_STRING);
        SAnnotatedProbability annotatedProbability;
        CPPUNIT_ASSERT(model.computeProbability(0/*pid*/, time, time + bucketLength,
                                                partitioningFields, 1, annotatedProbability));
        LOG_DEBUG("probability = " << annotatedProbability.s_Probability);
        probabilities.push_back(annotatedProbability.s_Probability);

        time += bucketLength;
    }

    LOG_DEBUG("probabilities = " << core::CContainerPrinter::print(probabilities));
    CPPUNIT_ASSERT(probabilities[lowSumBucket] > 0.1);
    CPPUNIT_ASSERT(probabilities[highSumBucket] < 0.01);
}

void CMetricModelTest::testOnlineProbabilityCalculationForLatLong(void)
{
    LOG_DEBUG("*** testOnlineProbabilityCalculationForLatLong ***");

    // TODO
}

void CMetricModelTest::testMinInfluence(void)
{
    LOG_DEBUG("*** testMinInfluence ***");

    typedef maths::CBasicStatistics::COrderStatisticsStack<TDoubleStrPr,
                                                           1,
                                                           maths::COrderings::SFirstLess> TMinAccumulator;

    core_t::TTime startTime(0);
    core_t::TTime bucketLength(10);
    SModelParams params(bucketLength);
    CMetricModelFactory factory(params);

    std::size_t numberOfBuckets = 50;
    std::size_t bucketCount = 5;

    double mean = 5.0;
    double variance = 1.0;

    std::string influencer = "I";
    std::string influencerValues[] = { "i1", "i2", "i3", "i4", "i5" };

    CDataGatherer::TFeatureVec features(1, model_t::E_IndividualMinByPerson);
    factory.features(features);
    factory.bucketLength(bucketLength);
    factory.fieldNames("", "", "P", "V", TStrVec(1, "I"));
    CModelFactory::SGathererInitializationData gathererInitData(startTime);
    CModelFactory::TDataGathererPtr gatherer(factory.makeDataGatherer(gathererInitData));
    CPPUNIT_ASSERT_EQUAL(std::size_t(0), addPerson("p", gatherer));
    CModelFactory::SModelInitializationData initData(gatherer);
    CModel::TModelPtr model_(factory.makeModel(initData));
    CPPUNIT_ASSERT(model_);
    CPPUNIT_ASSERT_EQUAL(model_t::E_MetricOnline, model_->category());
    CMetricModel &model = static_cast<CMetricModel&>(*model_.get());
    CResourceMonitor resourceMonitor;

    test::CRandomNumbers rng;
    core_t::TTime time = startTime;
    for (std::size_t i = 0u; i < numberOfBuckets; ++i, time += bucketLength)
    {
        TDoubleVec samples;
        rng.generateNormalSamples(mean, variance, bucketCount, samples);

        TMinAccumulator min;
        for (std::size_t j = 0u; j < samples.size(); ++j)
        {
            addArrival(*gatherer, time, "p", samples[j], TOptionalStr(influencerValues[j]));
            min.add(TDoubleStrPr(samples[j], influencerValues[j]));
        }

        model.sample(time, time + bucketLength, resourceMonitor);

        CPartitioningFields partitioningFields(EMPTY_STRING, EMPTY_STRING);
        SAnnotatedProbability annotatedProbability;
        model.computeProbability(0/*pid*/, time, time + bucketLength,
                                 partitioningFields, 1, annotatedProbability);

        LOG_DEBUG("influences = " << core::CContainerPrinter::print(annotatedProbability.s_Influences));
        if (!annotatedProbability.s_Influences.empty())
        {
            std::size_t j = 0u;
            for (/**/; j < annotatedProbability.s_Influences.size(); ++j)
            {
                if (   *annotatedProbability.s_Influences[j].first.second == min[0].second
                    && ::fabs(annotatedProbability.s_Influences[j].second - 1.0) < 1e-10)
                {
                    break;
                }
            }
            CPPUNIT_ASSERT(j < annotatedProbability.s_Influences.size());
        }
    }
}

void CMetricModelTest::testMeanInfluence(void)
{
    LOG_DEBUG("*** testMeanInfluence ***");

    core_t::TTime startTime(0);
    core_t::TTime bucketLength(10);
    SModelParams params(bucketLength);
    CMetricModelFactory factory(params);

    CDataGatherer::TFeatureVec features(1, model_t::E_IndividualMeanByPerson);
    factory.features(features);
    factory.bucketLength(bucketLength);
    factory.fieldNames("", "", "P", "V", TStrVec(1, "I"));
    CModelFactory::SGathererInitializationData gathererInitData(startTime);
    CModelFactory::TDataGathererPtr gatherer(factory.makeDataGatherer(gathererInitData));
    CPPUNIT_ASSERT_EQUAL(std::size_t(0), addPerson("p", gatherer));
    CModelFactory::SModelInitializationData initData(gatherer);
    CModel::TModelPtr model_(factory.makeModel(initData));
    CPPUNIT_ASSERT(model_);
    CPPUNIT_ASSERT_EQUAL(model_t::E_MetricOnline, model_->category());
    CMetricModel &model = static_cast<CMetricModel&>(*model_.get());

    double bucket1[]                 = {  1.0,  2.3,  2.1 };
    std::string influencerValues1[]  = { "i1", "i1", "i2", "i2" };
    double bucket2[]                 = { 8.0 };
    std::string influencerValues2[]  = { "i1" };
    double bucket3[]                 = {  4.3,  5.2,  3.4 };
    std::string influencerValues3[]  = { "i1", "i1", "i1" };
    double bucket4[]                 = {  3.2,  3.9 };
    std::string influencerValues4[]  = { "i3", "i3" };
    double bucket5[]                 = { 20.1,  2.8,  3.9 };
    std::string influencerValues5[]  = { "i2", "i1", "i1" };
    double bucket6[]                 = { 12.1,  4.2,  5.7,  3.2 };
    std::string influencerValues6[]  = { "i1", "i2", "i2", "i2" };
    double bucket7[]                 = {  0.1,  0.3,  5.4 };
    std::string influencerValues7[]  = { "i1", "i1", "i3" };
    double bucket8[]                 = { 40.5,  7.3 };
    std::string influencerValues8[]  = { "i1", "i2" };
    double bucket9[]                 = {  6.4,  7.0,  7.1,  6.6,  7.1,  6.7 };
    std::string influencerValues9[]  = { "i1", "i2", "i3", "i4", "i5", "i6" };
    double bucket10[]                = {  0.3 };
    std::string influencerValues10[] = { "i2" };

    SAnnotatedProbability annotatedProbability;

    core_t::TTime time = startTime;
    processBucket(time, bucketLength,
                  boost::size(bucket1), bucket1, influencerValues1,
                  *gatherer, model, annotatedProbability);

    time += bucketLength;
    processBucket(time, bucketLength,
                  boost::size(bucket2), bucket2, influencerValues2,
                  *gatherer, model, annotatedProbability);

    time += bucketLength;
    processBucket(time, bucketLength,
                  boost::size(bucket3), bucket3, influencerValues3,
                  *gatherer, model, annotatedProbability);

    time += bucketLength;
    processBucket(time, bucketLength,
                  boost::size(bucket4), bucket4, influencerValues4,
                  *gatherer, model, annotatedProbability);

    time += bucketLength;
    processBucket(time, bucketLength,
                  boost::size(bucket5), bucket5, influencerValues5,
                  *gatherer, model, annotatedProbability);

    time += bucketLength;
    processBucket(time, bucketLength,
                  boost::size(bucket6), bucket6, influencerValues6,
                  *gatherer, model, annotatedProbability);

    time += bucketLength;
    processBucket(time, bucketLength,
                  boost::size(bucket7), bucket7, influencerValues7,
                  *gatherer, model, annotatedProbability);
    CPPUNIT_ASSERT_EQUAL(std::string("[((I, i1), 0.7724379)]"),
                         core::CContainerPrinter::print(annotatedProbability.s_Influences));

    time += bucketLength;
    processBucket(time, bucketLength,
                  boost::size(bucket8), bucket8, influencerValues8,
                  *gatherer, model, annotatedProbability);
    CPPUNIT_ASSERT_EQUAL(std::string("[((I, i1), 0.883169)]"),
                         core::CContainerPrinter::print(annotatedProbability.s_Influences));

    time += bucketLength;
    processBucket(time, bucketLength,
                  boost::size(bucket9), bucket9, influencerValues9,
                  *gatherer, model, annotatedProbability);
    CPPUNIT_ASSERT(annotatedProbability.s_Influences.empty());

    time += bucketLength;
    processBucket(time, bucketLength,
                  boost::size(bucket10), bucket10, influencerValues10,
                  *gatherer, model, annotatedProbability);
    CPPUNIT_ASSERT_EQUAL(std::string("[((I, i2), 1)]"),
                         core::CContainerPrinter::print(annotatedProbability.s_Influences));
}

void CMetricModelTest::testMaxInfluence(void)
{
    LOG_DEBUG("*** testMaxInfluence ***");

    typedef maths::CBasicStatistics::COrderStatisticsStack<TDoubleStrPr,
                                                           1,
                                                           maths::COrderings::SFirstGreater> TMaxAccumulator;

    core_t::TTime startTime(0);
    core_t::TTime bucketLength(10);
    SModelParams params(bucketLength);
    CMetricModelFactory factory(params);

    std::size_t numberOfBuckets = 50;
    std::size_t bucketCount = 5;

    double mean = 5.0;
    double variance = 1.0;

    std::string influencer = "I";
    std::string influencerValues[] = { "i1", "i2", "i3", "i4", "i5" };

    CDataGatherer::TFeatureVec features(1, model_t::E_IndividualMaxByPerson);
    factory.features(features);
    factory.bucketLength(bucketLength);
    factory.fieldNames("", "", "P", "V", TStrVec(1, "I"));
    CModelFactory::SGathererInitializationData gathererInitData(startTime);
    CModelFactory::TDataGathererPtr gatherer(factory.makeDataGatherer(gathererInitData));
    CPPUNIT_ASSERT_EQUAL(std::size_t(0), addPerson("p", gatherer));
    CModelFactory::SModelInitializationData initData(gatherer);
    CModel::TModelPtr model_(factory.makeModel(initData));
    CPPUNIT_ASSERT(model_);
    CPPUNIT_ASSERT_EQUAL(model_t::E_MetricOnline, model_->category());
    CMetricModel &model = static_cast<CMetricModel&>(*model_.get());
    CResourceMonitor resourceMonitor;

    test::CRandomNumbers rng;
    core_t::TTime time = startTime;
    for (std::size_t i = 0u; i < numberOfBuckets; ++i, time += bucketLength)
    {
        TDoubleVec samples;
        rng.generateNormalSamples(mean, variance, bucketCount, samples);

        TMaxAccumulator max;
        for (std::size_t j = 0u; j < samples.size(); ++j)
        {
            addArrival(*gatherer, time, "p", samples[j], TOptionalStr(influencerValues[j]));
            max.add(TDoubleStrPr(samples[j], influencerValues[j]));
        }

        model.sample(time, time + bucketLength, resourceMonitor);

        CPartitioningFields partitioningFields(EMPTY_STRING, EMPTY_STRING);
        SAnnotatedProbability annotatedProbability;
        model.computeProbability(0/*pid*/, time, time + bucketLength,
                                 partitioningFields, 1, annotatedProbability);

        LOG_DEBUG("influences = " << core::CContainerPrinter::print(annotatedProbability.s_Influences));
        if (!annotatedProbability.s_Influences.empty())
        {
            std::size_t j = 0u;
            for (/**/; j < annotatedProbability.s_Influences.size(); ++j)
            {
                if (   *annotatedProbability.s_Influences[j].first.second == max[0].second
                    && ::fabs(annotatedProbability.s_Influences[j].second - 1.0) < 1e-10)
                {
                    break;
                }
            }
            CPPUNIT_ASSERT(j < annotatedProbability.s_Influences.size());
        }
    }
}

void CMetricModelTest::testSumInfluence(void)
{
    LOG_DEBUG("*** testSumInfluence ***");

    core_t::TTime startTime(0);
    core_t::TTime bucketLength(10);
    SModelParams params(bucketLength);
    CMetricModelFactory factory(params);

    CDataGatherer::TFeatureVec features(1, model_t::E_IndividualSumByBucketAndPerson);
    factory.features(features);
    factory.bucketLength(bucketLength);
    factory.fieldNames("", "", "P", "V", TStrVec(1, "I"));
    CModelFactory::SGathererInitializationData gathererInitData(startTime);
    CModelFactory::TDataGathererPtr gatherer(factory.makeDataGatherer(gathererInitData));
    CPPUNIT_ASSERT_EQUAL(std::size_t(0), addPerson("p", gatherer));
    CModelFactory::SModelInitializationData initData(gatherer);
    CModel::TModelPtr model_(factory.makeModel(initData));
    CPPUNIT_ASSERT(model_);
    CPPUNIT_ASSERT_EQUAL(model_t::E_MetricOnline, model_->category());
    CMetricModel &model = static_cast<CMetricModel&>(*model_.get());

    double bucket1[]                 = {  1.0,  2.3,  2.1,  5.9 };
    std::string influencerValues1[]  = { "i1", "i1", "i2", "i2" };
    double bucket2[]                 = { 10.0 };
    std::string influencerValues2[]  = { "i1" };
    double bucket3[]                 = {  4.3,  5.2,  3.4,  6.2,  7.8 };
    std::string influencerValues3[]  = { "i1", "i1", "i1", "i1", "i3" };
    double bucket4[]                 = {  3.2,  3.9 };
    std::string influencerValues4[]  = { "i3", "i3" };
    double bucket5[]                 = { 20.1,  2.8,  3.9 };
    std::string influencerValues5[]  = { "i2", "i1", "i1" };
    double bucket6[]                 = { 12.1,  4.2,  5.7,  3.2 };
    std::string influencerValues6[]  = { "i1", "i2", "i2", "i2" };
    double bucket7[]                 = {  0.1,  0.3,  5.4 };
    std::string influencerValues7[]  = { "i1", "i1", "i3" };
    double bucket8[]                 = { 40.5,  12.3 };
    std::string influencerValues8[]  = { "i1", "i2" };
    double bucket9[]                 = {  6.4,  7.0,  7.1,  6.6,  7.1,  6.7 };
    std::string influencerValues9[]  = { "i1", "i2", "i3", "i4", "i5", "i6" };
    double bucket10[]                = {  0.3 };
    std::string influencerValues10[] = { "i2" };

    SAnnotatedProbability annotatedProbability;

    core_t::TTime time = startTime;
    processBucket(time, bucketLength,
                  boost::size(bucket1), bucket1, influencerValues1,
                  *gatherer, model, annotatedProbability);

    time += bucketLength;
    processBucket(time, bucketLength,
                  boost::size(bucket2), bucket2, influencerValues2,
                  *gatherer, model, annotatedProbability);

    time += bucketLength;
    processBucket(time, bucketLength,
                  boost::size(bucket3), bucket3, influencerValues3,
                  *gatherer, model, annotatedProbability);

    time += bucketLength;
    processBucket(time, bucketLength,
                  boost::size(bucket4), bucket4, influencerValues4,
                  *gatherer, model, annotatedProbability);

    time += bucketLength;
    processBucket(time, bucketLength,
                  boost::size(bucket5), bucket5, influencerValues5,
                  *gatherer, model, annotatedProbability);
    CPPUNIT_ASSERT_EQUAL(std::string("[((I, i2), 1), ((I, i1), 0.6455305)]"),
                         core::CContainerPrinter::print(annotatedProbability.s_Influences));

    time += bucketLength;
    processBucket(time, bucketLength,
                  boost::size(bucket6), bucket6, influencerValues6,
                  *gatherer, model, annotatedProbability);
    CPPUNIT_ASSERT_EQUAL(std::string("[((I, i1), 1), ((I, i2), 1)]"),
                         core::CContainerPrinter::print(annotatedProbability.s_Influences));

    time += bucketLength;
    processBucket(time, bucketLength,
                  boost::size(bucket7), bucket7, influencerValues7,
                  *gatherer, model, annotatedProbability);
    CPPUNIT_ASSERT(annotatedProbability.s_Influences.empty());

    time += bucketLength;
    processBucket(time, bucketLength,
                  boost::size(bucket8), bucket8, influencerValues8,
                  *gatherer, model, annotatedProbability);
    CPPUNIT_ASSERT_EQUAL(std::string("[((I, i1), 1)]"),
                         core::CContainerPrinter::print(annotatedProbability.s_Influences));

    time += bucketLength;
    processBucket(time, bucketLength,
                  boost::size(bucket9), bucket9, influencerValues9,
                  *gatherer, model, annotatedProbability);
    CPPUNIT_ASSERT(annotatedProbability.s_Influences.empty());

    time += bucketLength;
    processBucket(time, bucketLength,
                  boost::size(bucket10), bucket10, influencerValues10,
                  *gatherer, model, annotatedProbability);
    CPPUNIT_ASSERT(annotatedProbability.s_Influences.empty());
}

void CMetricModelTest::testLatLongInfluence(void)
{
    LOG_DEBUG("*** testLatLongInfluence ***");

    // TODO
}

void CMetricModelTest::testPrune(void)
{
    LOG_DEBUG("*** testPrune ***");

    maths::CSampling::CScopeMockRandomNumberGenerator scopeMockRng;

    typedef std::vector<std::size_t> TSizeVec;
    typedef std::vector<TSizeVec> TSizeVecVec;
    typedef std::vector<CEventData> TEventDataVec;
    typedef std::map<std::size_t, std::size_t> TSizeSizeMap;

    const core_t::TTime startTime = 1346968800;
    const core_t::TTime bucketLength = 3600;

    const std::string people[] =
        {
            std::string("p1"),
            std::string("p2"),
            std::string("p3"),
            std::string("p4"),
            std::string("p5"),
            std::string("p6"),
            std::string("p7"),
            std::string("p8")
        };

    TSizeVecVec eventCounts;
    eventCounts.push_back(TSizeVec(1000u, 0));
    eventCounts[0][0] = 4;
    eventCounts[0][1] = 3;
    eventCounts[0][2] = 5;
    eventCounts[0][4] = 2;
    eventCounts.push_back(TSizeVec(1000u, 1));
    eventCounts.push_back(TSizeVec(1000u, 0));
    eventCounts[2][1] = 10;
    eventCounts[2][2] = 13;
    eventCounts[2][8] = 5;
    eventCounts[2][15] = 2;
    eventCounts.push_back(TSizeVec(1000u, 0));
    eventCounts[3][2] = 13;
    eventCounts[3][8] = 9;
    eventCounts[3][15] = 12;
    eventCounts.push_back(TSizeVec(1000u, 2));
    eventCounts.push_back(TSizeVec(1000u, 1));
    eventCounts.push_back(TSizeVec(1000u, 0));
    eventCounts[6][0] = 4;
    eventCounts[6][1] = 3;
    eventCounts[6][2] = 5;
    eventCounts[6][4] = 2;
    eventCounts.push_back(TSizeVec(1000u, 0));
    eventCounts[7][2] = 13;
    eventCounts[7][8] = 9;
    eventCounts[7][15] = 12;

    const std::size_t expectedPeople[] = { 1, 4, 5 };

    SModelParams params(bucketLength);
    params.s_DecayRate = 0.01;
    CMetricModelFactory factory(params);
    CDataGatherer::TFeatureVec features;
    features.push_back(model_t::E_IndividualMeanByPerson);
    features.push_back(model_t::E_IndividualMinByPerson);
    features.push_back(model_t::E_IndividualMaxByPerson);
    factory.features(features);
    CModelFactory::SGathererInitializationData gathererInitData(startTime);
    CModelFactory::TDataGathererPtr gatherer(factory.makeDataGatherer(gathererInitData));
    CModelFactory::SModelInitializationData modelInitData(gatherer);
    CModel::TModelPtr model_(factory.makeModel(modelInitData));
    CMetricModel *model = dynamic_cast<CMetricModel*>(model_.get());
    CPPUNIT_ASSERT(model);
    CModelFactory::TDataGathererPtr expectedGatherer(factory.makeDataGatherer(gathererInitData));
    CModelFactory::SModelInitializationData expectedModelInitData(expectedGatherer);
    CModel::TModelPtr expectedModelHolder(factory.makeModel(expectedModelInitData));
    CMetricModel *expectedModel = dynamic_cast<CMetricModel*>(expectedModelHolder.get());
    CPPUNIT_ASSERT(expectedModel);
    CResourceMonitor resourceMonitor;

    test::CRandomNumbers rng;

    TEventDataVec events;
    core_t::TTime bucketStart = startTime;
    for (std::size_t i = 0u; i < eventCounts.size(); ++i, bucketStart = startTime)
    {
        for (std::size_t j = 0u; j < eventCounts[i].size(); ++j, bucketStart += bucketLength)
        {
            core_t::TTime n = static_cast<core_t::TTime>(eventCounts[i][j]);
            if (n > 0)
            {
                TDoubleVec samples;
                rng.generateUniformSamples(0.0, 5.0, static_cast<size_t>(n), samples);

                for (core_t::TTime k = 0, time = bucketStart, dt = bucketLength / n;
                     k < n;
                     ++k, time += dt)
                {
                    std::size_t pid = addPerson(people[i], gatherer);
                    events.push_back(makeEventData(time, pid, samples[static_cast<size_t>(k)]));
                }
            }
        }
    }
    std::sort(events.begin(), events.end(), CTimeLess());

    TEventDataVec expectedEvents;
    expectedEvents.reserve(events.size());
    TSizeSizeMap mapping;
    for (std::size_t i = 0u; i < boost::size(expectedPeople); ++i)
    {
        std::size_t pid = addPerson(people[expectedPeople[i]], expectedGatherer);
        mapping[expectedPeople[i]] = pid;
    }
    for (std::size_t i = 0u; i < events.size(); ++i)
    {
        if (std::binary_search(boost::begin(expectedPeople),
                               boost::end(expectedPeople),
                               events[i].personId()))
        {
            expectedEvents.push_back(makeEventData(events[i].time(),
                                                   mapping[*events[i].personId()],
                                                   events[i].values()[0][0]));
        }
    }

    bucketStart = startTime;
    for (std::size_t i = 0u; i < events.size(); ++i)
    {
        if (events[i].time() >= bucketStart + bucketLength)
        {
            model->sample(bucketStart, bucketStart + bucketLength, resourceMonitor);
            bucketStart += bucketLength;
        }
        addArrival(*gatherer,
                   events[i].time(),
                   gatherer->personName(events[i].personId().get()),
                   events[i].values()[0][0]);
    }
    model->sample(bucketStart, bucketStart + bucketLength, resourceMonitor);
    size_t maxDimensionBeforePrune(model->dataGatherer().maxDimension());
    model->prune(model->defaultPruneWindow());
    size_t maxDimensionAfterPrune(model->dataGatherer().maxDimension());
    CPPUNIT_ASSERT_EQUAL(maxDimensionBeforePrune, maxDimensionAfterPrune);

    bucketStart = startTime;
    for (std::size_t i = 0u; i < expectedEvents.size(); ++i)
    {
        if (expectedEvents[i].time() >= bucketStart + bucketLength)
        {
            expectedModel->sample(bucketStart, bucketStart + bucketLength, resourceMonitor);
            bucketStart += bucketLength;
        }

        addArrival(*expectedGatherer,
                   expectedEvents[i].time(),
                   expectedGatherer->personName(expectedEvents[i].personId().get()),
                   expectedEvents[i].values()[0][0]);
    }
    expectedModel->sample(bucketStart, bucketStart + bucketLength, resourceMonitor);

    LOG_DEBUG("checksum          = " << model->checksum());
    LOG_DEBUG("expected checksum = " << expectedModel->checksum());
    CPPUNIT_ASSERT_EQUAL(expectedModel->checksum(), model->checksum());

    // Now check that we recycle the person slots.

    bucketStart = gatherer->currentBucketStartTime() + bucketLength;
    std::string newPersons[] = {"p9", "p10", "p11", "p12", "13"};
    for (std::size_t i = 0u; i < boost::size(newPersons); ++i)
    {
        std::size_t newPid = addPerson(newPersons[i], gatherer);
        CPPUNIT_ASSERT(newPid < 8);

        std::size_t expectedNewPid = addPerson(newPersons[i], expectedGatherer);

        addArrival(*gatherer, bucketStart + 1, gatherer->personName(newPid), 10.0);
        addArrival(*gatherer, bucketStart + 2000, gatherer->personName(newPid), 15.0);
        addArrival(*expectedGatherer, bucketStart + 1, expectedGatherer->personName(expectedNewPid), 10.0);
        addArrival(*expectedGatherer, bucketStart + 2000, expectedGatherer->personName(expectedNewPid), 15.0);
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

void CMetricModelTest::testKey(void)
{
    function_t::EFunction countFunctions[] =
        {
            function_t::E_IndividualMetric,
            function_t::E_IndividualMetricMean,
            function_t::E_IndividualMetricMin,
            function_t::E_IndividualMetricMax,
            function_t::E_IndividualMetricSum
        };
    bool useNull[] = { true, false };
    std::string byField[] = { "", "by" };
    std::string partitionField[] = { "", "partition" };

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
                                   "",
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

void CMetricModelTest::testSkipSampling(void)
{
    LOG_DEBUG("*** testSkipSampling ***");

    core_t::TTime startTime(100);
    core_t::TTime bucketLength(100);
    SModelParams params(bucketLength);
    CMetricModelFactory factory(params);

    CDataGatherer::TFeatureVec features;
    features.push_back(model_t::E_IndividualSumByBucketAndPerson);
    factory.features(features);
    factory.fieldNames("", "", "P", "V", TStrVec(1, "I"));

    CModelFactory::SGathererInitializationData gathererNoGapInitData(startTime);
    CModelFactory::TDataGathererPtr gathererNoGap(factory.makeDataGatherer(gathererNoGapInitData));
    CPPUNIT_ASSERT_EQUAL(std::size_t(0), addPerson("p", gathererNoGap));
    CModelFactory::SModelInitializationData initDataNoGap(gathererNoGap);
    CModel::TModelPtr modelNoGapPtr(factory.makeModel(initDataNoGap));
    CPPUNIT_ASSERT(modelNoGapPtr);
    CPPUNIT_ASSERT_EQUAL(model_t::E_MetricOnline, modelNoGapPtr->category());
    CMetricModel &modelNoGap = static_cast<CMetricModel&>(*modelNoGapPtr.get());

    {
        std::string influencerValues1[]  = { "i1" };
        double bucket1[]                 = { 1.0 };
        double bucket2[]                 = { 5.0 };
        double bucket3[]                 = { 10.0 };

        SAnnotatedProbability annotatedProbability;

        core_t::TTime time = startTime;
        processBucket(time, bucketLength,
                      boost::size(bucket1), bucket1, influencerValues1,
                      *gathererNoGap, modelNoGap, annotatedProbability);

        time += bucketLength;
        processBucket(time, bucketLength,
                      boost::size(bucket2), bucket2, influencerValues1,
                      *gathererNoGap, modelNoGap, annotatedProbability);

        time += bucketLength;
        processBucket(time, bucketLength,
                      boost::size(bucket3), bucket3, influencerValues1,
                      *gathererNoGap, modelNoGap, annotatedProbability);
    }

    CModelFactory::SGathererInitializationData gathererWithGapInitData(startTime);
    CModelFactory::TDataGathererPtr gathererWithGap(factory.makeDataGatherer(gathererWithGapInitData));
    CPPUNIT_ASSERT_EQUAL(std::size_t(0), addPerson("p", gathererWithGap));
    CModelFactory::SModelInitializationData initDataWithGap(gathererWithGap);
    CModel::TModelPtr modelWithGapPtr(factory.makeModel(initDataWithGap));
    CPPUNIT_ASSERT(modelWithGapPtr);
    CPPUNIT_ASSERT_EQUAL(model_t::E_MetricOnline, modelWithGapPtr->category());
    CMetricModel &modelWithGap = static_cast<CMetricModel&>(*modelWithGapPtr.get());
    core_t::TTime gap(bucketLength * 10);

    {
        std::string influencerValues1[]  = { "i1" };
        double bucket1[]                 = { 1.0 };
        double bucket2[]                 = { 5.0 };
        double bucket3[]                 = { 10.0 };

        SAnnotatedProbability annotatedProbability;

        core_t::TTime time = startTime;
        processBucket(time, bucketLength,
                      boost::size(bucket1), bucket1, influencerValues1,
                      *gathererWithGap, modelWithGap, annotatedProbability);

        CResourceMonitor resourceMonitor;
        time += gap;
        modelWithGap.skipSampling(time);
        LOG_DEBUG("Calling sample over skipped interval should do nothing except print some ERRORs");
        modelWithGap.sample(startTime + bucketLength, time, resourceMonitor);

        processBucket(time, bucketLength,
                      boost::size(bucket2), bucket2, influencerValues1,
                      *gathererWithGap, modelWithGap, annotatedProbability);

        time += bucketLength;
        processBucket(time, bucketLength,
                      boost::size(bucket3), bucket3, influencerValues1,
                      *gathererWithGap, modelWithGap, annotatedProbability);
    }

    CPPUNIT_ASSERT_EQUAL(
            modelNoGap.details()->prior(model_t::E_IndividualSumByBucketAndPerson, 0)->checksum(),
            modelWithGap.details()->prior(model_t::E_IndividualSumByBucketAndPerson, 0)->checksum());
}

void CMetricModelTest::testExplicitNulls(void)
{
    LOG_DEBUG("*** testExplicitNulls ***");

    core_t::TTime startTime(100);
    core_t::TTime bucketLength(100);
    SModelParams params(bucketLength);
    std::string summaryCountField("count");
    CMetricModelFactory factory(params, model_t::E_Manual, summaryCountField);
    CResourceMonitor resourceMonitor;

    CDataGatherer::TFeatureVec features;
    features.push_back(model_t::E_IndividualSumByBucketAndPerson);
    factory.features(features);
    factory.fieldNames("", "", "P", "V", TStrVec(1, "I"));

    CModelFactory::SGathererInitializationData gathererSkipGapInitData(startTime);
    CModelFactory::TDataGathererPtr gathererSkipGap(factory.makeDataGatherer(gathererSkipGapInitData));
    CModelFactory::SModelInitializationData initDataSkipGap(gathererSkipGap);
    CModel::TModelPtr modelSkipGapPtr(factory.makeModel(initDataSkipGap));
    CPPUNIT_ASSERT(modelSkipGapPtr);
    CPPUNIT_ASSERT_EQUAL(model_t::E_MetricOnline, modelSkipGapPtr->category());
    CMetricModel &modelSkipGap = static_cast<CMetricModel&>(*modelSkipGapPtr.get());

    // The idea here is to compare a model that has a gap skipped against a model
    // that has explicit nulls for the buckets that sampling was skipped.

    // p1: |(1, 42.0)|(1, 1.0)|(1, 1.0)|X|X|(1, 42.0)|
    // p2: |(1, 42.)|(0, 0.0)|(0, 0.0)|X|X|(0, 0.0)|
    addArrival(*gathererSkipGap, 100, "p1", 42.0, TOptionalStr("i1"), TOptionalStr(), TOptionalStr("1"));
    addArrival(*gathererSkipGap, 100, "p2", 42.0, TOptionalStr("i2"), TOptionalStr(), TOptionalStr("1"));
    modelSkipGap.sample(100, 200, resourceMonitor);
    addArrival(*gathererSkipGap, 200, "p1", 1.0, TOptionalStr("i1"), TOptionalStr(), TOptionalStr("1"));
    modelSkipGap.sample(200, 300, resourceMonitor);
    addArrival(*gathererSkipGap, 300, "p1", 1.0, TOptionalStr("i1"), TOptionalStr(), TOptionalStr("1"));
    modelSkipGap.sample(300, 400, resourceMonitor);
    modelSkipGap.skipSampling(600);
    addArrival(*gathererSkipGap, 600, "p1", 42.0, TOptionalStr("i1"), TOptionalStr(), TOptionalStr("1"));
    modelSkipGap.sample(600, 700, resourceMonitor);

    CModelFactory::SGathererInitializationData gathererExNullInitData(startTime);
    CModelFactory::TDataGathererPtr gathererExNull(factory.makeDataGatherer(gathererExNullInitData));
    CModelFactory::SModelInitializationData initDataExNull(gathererExNull);
    CModel::TModelPtr modelExNullPtr(factory.makeModel(initDataExNull));
    CPPUNIT_ASSERT(modelExNullPtr);
    CPPUNIT_ASSERT_EQUAL(model_t::E_MetricOnline, modelExNullPtr->category());
    CMetricModel &modelExNullGap = static_cast<CMetricModel&>(*modelExNullPtr.get());

    // p1: |(1, 42.0), ("", 42.0), (null, 42.0)|(1, 1.0)|(1, 1.0)|(null, 100.0)|(null, 100.0)|(1, 42.0)|
    // p2: |(1, 42.0), ("", 42.0)|(0, 0.0)|(0, 0.0)|(null, 100.0)|(null, 100.0)|(0, 0.0)|
    addArrival(*gathererExNull, 100, "p1", 42.0, TOptionalStr("i1"), TOptionalStr(), TOptionalStr("1"));
    addArrival(*gathererExNull, 100, "p1", 42.0, TOptionalStr("i1"), TOptionalStr(), TOptionalStr(""));
    addArrival(*gathererExNull, 100, "p1", 42.0, TOptionalStr("i1"), TOptionalStr(), TOptionalStr("null"));
    addArrival(*gathererExNull, 100, "p2", 42.0, TOptionalStr("i2"), TOptionalStr(), TOptionalStr("1"));
    addArrival(*gathererExNull, 100, "p2", 42.0, TOptionalStr("i2"), TOptionalStr(), TOptionalStr(""));
    modelExNullGap.sample(100, 200, resourceMonitor);
    addArrival(*gathererExNull, 200, "p1", 1.0, TOptionalStr("i1"), TOptionalStr(), TOptionalStr("1"));
    modelExNullGap.sample(200, 300, resourceMonitor);
    addArrival(*gathererExNull, 300, "p1", 1.0, TOptionalStr("i1"), TOptionalStr(), TOptionalStr("1"));
    modelExNullGap.sample(300, 400, resourceMonitor);
    addArrival(*gathererExNull, 400, "p1", 100.0, TOptionalStr("i1"), TOptionalStr(), TOptionalStr("null"));
    addArrival(*gathererExNull, 400, "p2", 100.0, TOptionalStr("i2"), TOptionalStr(), TOptionalStr("null"));
    modelExNullGap.sample(400, 500, resourceMonitor);
    addArrival(*gathererExNull, 500, "p1", 100.0, TOptionalStr("i1"), TOptionalStr(), TOptionalStr("null"));
    addArrival(*gathererExNull, 500, "p2", 100.0, TOptionalStr("i2"), TOptionalStr(), TOptionalStr("null"));
    modelExNullGap.sample(500, 600, resourceMonitor);
    addArrival(*gathererExNull, 600, "p1", 42.0, TOptionalStr("i1"), TOptionalStr(), TOptionalStr("1"));
    modelExNullGap.sample(600, 700, resourceMonitor);

    CPPUNIT_ASSERT_EQUAL(
            modelSkipGap.details()->prior(model_t::E_IndividualSumByBucketAndPerson, 0)->checksum(),
            modelExNullGap.details()->prior(model_t::E_IndividualSumByBucketAndPerson, 0)->checksum());
}

void CMetricModelTest::testVarp(void)
{
    LOG_DEBUG("*** testVarp ***");

    core_t::TTime startTime(500000);
    core_t::TTime bucketLength(1000);
    SModelParams params(bucketLength);

    CDataGatherer::TFeatureVec features;
    features.push_back(model_t::E_IndividualVarianceByPerson);
    CMetricModelFactory factory(params);
    factory.features(features);
    factory.bucketLength(bucketLength);
    factory.fieldNames("", "", "P", "V", TStrVec());
    CModelFactory::SGathererInitializationData gathererInitData(startTime);
    CModelFactory::TDataGathererPtr gatherer(factory.makeDataGatherer(gathererInitData));
    CPPUNIT_ASSERT(!gatherer->isPopulation());
    CPPUNIT_ASSERT_EQUAL(std::size_t(0), addPerson("p", gatherer));
    CPPUNIT_ASSERT_EQUAL(std::size_t(1), addPerson("q", gatherer));
    CModelFactory::SModelInitializationData initData(gatherer);
    CModel::TModelPtr model_(factory.makeModel(initData));
    CPPUNIT_ASSERT(model_);
    CPPUNIT_ASSERT_EQUAL(model_t::E_MetricOnline, model_->category());
    CMetricModel &model = static_cast<CMetricModel&>(*model_.get());

    double bucket1[]                 = { 1.0, 1.1 };
    double bucket2[]                 = { 10.0, 10.1 };
    double bucket3[]                 = { 4.3, 4.45 };
    double bucket4[]                 = { 3.2, 3.303 };
    double bucket5[]                 = { 20.1, 20.8, 20.9, 20.8 };
    double bucket6[]                 = { 4.1, 4.2 };
    double bucket7[]                 = { 0.1, 0.3, 0.2, 0.4 };
    double bucket8[]                 = { 12.5, 12.3 };
    double bucket9[]                 = { 6.9, 7.0, 7.1, 6.6, 7.1, 6.7 };
    double bucket10[]                = { 0.3, 0.2 };
    double bucket11[]                = { 0.0 };

    SAnnotatedProbability annotatedProbability;
    SAnnotatedProbability annotatedProbability2;

    core_t::TTime time = startTime;
    processBucket(time, bucketLength, boost::size(bucket1), bucket1,
                  *gatherer, model, annotatedProbability, annotatedProbability2);
    CPPUNIT_ASSERT(annotatedProbability.s_Probability > 0.8);
    CPPUNIT_ASSERT(annotatedProbability2.s_Probability > 0.8);
    LOG_DEBUG("P1 " << annotatedProbability.s_Probability << ", P2 " << annotatedProbability2.s_Probability);

    time += bucketLength;
    processBucket(time, bucketLength, boost::size(bucket2), bucket2,
                  *gatherer, model, annotatedProbability, annotatedProbability2);
    CPPUNIT_ASSERT(annotatedProbability.s_Probability > 0.8);
    CPPUNIT_ASSERT(annotatedProbability2.s_Probability > 0.8);
    LOG_DEBUG("P1 " << annotatedProbability.s_Probability << ", P2 " << annotatedProbability2.s_Probability);

    time += bucketLength;
    processBucket(time, bucketLength, boost::size(bucket3), bucket3,
                  *gatherer, model, annotatedProbability, annotatedProbability2);
    CPPUNIT_ASSERT(annotatedProbability.s_Probability > 0.8);
    CPPUNIT_ASSERT(annotatedProbability2.s_Probability > 0.8);
    LOG_DEBUG("P1 " << annotatedProbability.s_Probability << ", P2 " << annotatedProbability2.s_Probability);

    time += bucketLength;
    processBucket(time, bucketLength, boost::size(bucket4), bucket4,
                  *gatherer, model, annotatedProbability, annotatedProbability2);
    CPPUNIT_ASSERT(annotatedProbability.s_Probability > 0.8);
    CPPUNIT_ASSERT(annotatedProbability2.s_Probability > 0.8);
    LOG_DEBUG("P1 " << annotatedProbability.s_Probability << ", P2 " << annotatedProbability2.s_Probability);

    time += bucketLength;
    processBucket(time, bucketLength, boost::size(bucket5), bucket5,
                  *gatherer, model, annotatedProbability, annotatedProbability2);
    CPPUNIT_ASSERT(annotatedProbability.s_Probability > 0.8);
    CPPUNIT_ASSERT(annotatedProbability2.s_Probability > 0.8);
    LOG_DEBUG("P1 " << annotatedProbability.s_Probability << ", P2 " << annotatedProbability2.s_Probability);

    time += bucketLength;
    processBucket(time, bucketLength, boost::size(bucket6), bucket6,
                  *gatherer, model, annotatedProbability, annotatedProbability2);
    CPPUNIT_ASSERT(annotatedProbability.s_Probability > 0.8);
    CPPUNIT_ASSERT(annotatedProbability2.s_Probability > 0.8);
    LOG_DEBUG("P1 " << annotatedProbability.s_Probability << ", P2 " << annotatedProbability2.s_Probability);

    time += bucketLength;
    processBucket(time, bucketLength, boost::size(bucket7), bucket7,
                  *gatherer, model, annotatedProbability, annotatedProbability2);
    CPPUNIT_ASSERT(annotatedProbability.s_Probability > 0.8);
    CPPUNIT_ASSERT(annotatedProbability2.s_Probability > 0.8);
    LOG_DEBUG("P1 " << annotatedProbability.s_Probability << ", P2 " << annotatedProbability2.s_Probability);

    time += bucketLength;
    processBucket(time, bucketLength, boost::size(bucket8), bucket8,
                  *gatherer, model, annotatedProbability, annotatedProbability2);
    CPPUNIT_ASSERT(annotatedProbability.s_Probability > 0.5);
    CPPUNIT_ASSERT(annotatedProbability2.s_Probability > 0.5);
    LOG_DEBUG("P1 " << annotatedProbability.s_Probability << ", P2 " << annotatedProbability2.s_Probability);

    time += bucketLength;
    processBucket(time, bucketLength, boost::size(bucket9), bucket9,
                  *gatherer, model, annotatedProbability, annotatedProbability2);
    CPPUNIT_ASSERT(annotatedProbability.s_Probability > 0.5);
    CPPUNIT_ASSERT(annotatedProbability2.s_Probability > 0.5);
    LOG_DEBUG("P1 " << annotatedProbability.s_Probability << ", P2 " << annotatedProbability2.s_Probability);

    time += bucketLength;
    processBucket(time, bucketLength, boost::size(bucket10), bucket10,
                  *gatherer, model, annotatedProbability, annotatedProbability2);
    CPPUNIT_ASSERT(annotatedProbability.s_Probability > 0.5);
    CPPUNIT_ASSERT(annotatedProbability2.s_Probability > 0.5);
    LOG_DEBUG("P1 " << annotatedProbability.s_Probability << ", P2 " << annotatedProbability2.s_Probability);

    time += bucketLength;
    processBucket(time, bucketLength, 0, bucket11,
                  *gatherer, model, annotatedProbability, annotatedProbability2);
    CPPUNIT_ASSERT(annotatedProbability.s_Probability > 0.5);
    CPPUNIT_ASSERT(annotatedProbability2.s_Probability > 0.5);
    LOG_DEBUG("P1 " << annotatedProbability.s_Probability << ", P2 " << annotatedProbability2.s_Probability);
}

void CMetricModelTest::testInterimCorrections(void)
{
    LOG_DEBUG("*** testInterimCorrections ***");

    core_t::TTime startTime(3600);
    core_t::TTime bucketLength(3600);
    SModelParams params(bucketLength);
    CMetricModelFactory factory(params);
    CDataGatherer::TFeatureVec features;
    features.push_back(model_t::E_IndividualSumByBucketAndPerson);
    factory.features(features);
    factory.fieldNames("", "", "P", "V", TStrVec(1, "I"));

    CModelFactory::SGathererInitializationData gathererInitData(startTime);
    CModelFactory::TDataGathererPtr gatherer(factory.makeDataGatherer(gathererInitData));
    CPPUNIT_ASSERT_EQUAL(std::size_t(0), addPerson("p", gatherer));
    CModelFactory::SModelInitializationData initData(gatherer);
    CModel::TModelPtr model_(factory.makeModel(initData));
    CPPUNIT_ASSERT(model_);
    CPPUNIT_ASSERT_EQUAL(model_t::E_MetricOnline, model_->category());
    CMetricModel &model = static_cast<CMetricModel&>(*model_.get());

    std::size_t pid1 = addPerson("p1", gatherer);
    std::size_t pid2 = addPerson("p2", gatherer);
    std::size_t pid3 = addPerson("p3", gatherer);

    core_t::TTime now = startTime;
    core_t::TTime endTime(now + 2 * 24 * bucketLength);
    test::CRandomNumbers rng;
    TDoubleVec samples(3, 0.0);
    CResourceMonitor resourceMonitor;
    while (now < endTime)
    {
        rng.generateUniformSamples(50.0, 70.0, std::size_t(3), samples);
        for (std::size_t i = 0; i < samples[0]; ++i)
        {
            addArrival(*gatherer, now, "p1", 1.0, TOptionalStr("i1"));
        }
        for (std::size_t i = 0; i < samples[1]; ++i)
        {
            addArrival(*gatherer, now, "p2", 1.0, TOptionalStr("i2"));
        }
        for (std::size_t i = 0; i < samples[2]; ++i)
        {
            addArrival(*gatherer, now, "p3", 1.0, TOptionalStr("i3"));
        }
        model.sample(now, now + bucketLength, resourceMonitor);
        now += bucketLength;
    }
    for (std::size_t i = 0; i < 35; ++i)
    {
        addArrival(*gatherer, now, "p1", 1.0, TOptionalStr("i1"));
    }
    for (std::size_t i = 0; i < 1; ++i)
    {
        addArrival(*gatherer, now, "p2", 1.0, TOptionalStr("i2"));
    }
    for (std::size_t i = 0; i < 100; ++i)
    {
        addArrival(*gatherer, now, "p3", 1.0, TOptionalStr("i3"));
    }
    model.sampleBucketStatistics(now, now + bucketLength, resourceMonitor);

    CPartitioningFields partitioningFields(EMPTY_STRING, EMPTY_STRING);
    model_t::CResultType type(model_t::CResultType::E_Unconditional | model_t::CResultType::E_Interim);
    SAnnotatedProbability annotatedProbability1;
    annotatedProbability1.s_ResultType = type;
    CPPUNIT_ASSERT(model.computeProbability(pid1, now, now + bucketLength,
                                            partitioningFields, 1, annotatedProbability1));
    SAnnotatedProbability annotatedProbability2;
    annotatedProbability2.s_ResultType = type;
    CPPUNIT_ASSERT(model.computeProbability(pid2, now, now + bucketLength,
                                            partitioningFields, 1, annotatedProbability2));
    SAnnotatedProbability annotatedProbability3;
    annotatedProbability3.s_ResultType = type;
    CPPUNIT_ASSERT(model.computeProbability(pid3, now, now + bucketLength,
                                            partitioningFields, 1, annotatedProbability3));

    TDouble1Vec p1Baseline = model.baselineBucketMean(model_t::E_IndividualSumByBucketAndPerson,
                                                      pid1, 0, type, NO_CORRELATES, now);
    TDouble1Vec p2Baseline = model.baselineBucketMean(model_t::E_IndividualSumByBucketAndPerson,
                                                      pid2, 0, type, NO_CORRELATES, now);
    TDouble1Vec p3Baseline = model.baselineBucketMean(model_t::E_IndividualSumByBucketAndPerson,
                                                      pid3, 0, type, NO_CORRELATES, now);

    LOG_DEBUG("p1 probability = " << annotatedProbability1.s_Probability);
    LOG_DEBUG("p2 probability = " << annotatedProbability2.s_Probability);
    LOG_DEBUG("p3 probability = " << annotatedProbability3.s_Probability);
    LOG_DEBUG("p1 baseline = " << p1Baseline[0]);
    LOG_DEBUG("p2 baseline = " << p2Baseline[0]);
    LOG_DEBUG("p3 baseline = " << p3Baseline[0]);

    CPPUNIT_ASSERT(annotatedProbability1.s_Probability > 0.05);
    CPPUNIT_ASSERT(annotatedProbability2.s_Probability < 0.05);
    CPPUNIT_ASSERT(annotatedProbability3.s_Probability < 0.05);
    CPPUNIT_ASSERT(p1Baseline[0] > 44.0 && p1Baseline[0] < 46.0);
    CPPUNIT_ASSERT(p2Baseline[0] > 45.0 && p2Baseline[0] < 46.0);
    CPPUNIT_ASSERT(p3Baseline[0] > 59.0 && p3Baseline[0] < 61.0);
}

void CMetricModelTest::testInterimCorrectionsWithCorrelations(void)
{
    LOG_DEBUG("*** testInterimCorrectionsWithCorrelations ***");

    core_t::TTime startTime(3600);
    core_t::TTime bucketLength(3600);
    SModelParams params(bucketLength);
    params.s_MultivariateByFields = true;
    CMetricModelFactory factory(params);

    CDataGatherer::TFeatureVec features;
    features.push_back(model_t::E_IndividualSumByBucketAndPerson);
    factory.features(features);
    factory.fieldNames("", "", "P", "V", TStrVec(1, "I"));

    CModelFactory::SGathererInitializationData gathererInitData(startTime);
    CModelFactory::TDataGathererPtr gatherer(factory.makeDataGatherer(gathererInitData));
    CModelFactory::SModelInitializationData initData(gatherer);
    CModel::TModelPtr modelPtr(factory.makeModel(initData));
    CPPUNIT_ASSERT(modelPtr);
    CPPUNIT_ASSERT_EQUAL(model_t::E_MetricOnline, modelPtr->category());
    CMetricModel &model = static_cast<CMetricModel&>(*modelPtr.get());

    std::size_t pid1 = addPerson("p1", gatherer);
    std::size_t pid2 = addPerson("p2", gatherer);
    std::size_t pid3 = addPerson("p3", gatherer);

    core_t::TTime now = startTime;
    core_t::TTime endTime(now + 2 * 24 * bucketLength);
    test::CRandomNumbers rng;
    TDoubleVec samples(1, 0.0);
    CResourceMonitor resourceMonitor;
    while (now < endTime)
    {
        rng.generateUniformSamples(80.0, 100.0, std::size_t(1), samples);
        for (std::size_t i = 0; i < samples[0]; ++i)
        {
            addArrival(*gatherer, now, "p1", 1.0, TOptionalStr("i1"));
        }
        for (std::size_t i = 0; i < samples[0] + 10; ++i)
        {
            addArrival(*gatherer, now, "p2", 1.0, TOptionalStr("i2"));
        }
        for (std::size_t i = 0; i < samples[0] - 10; ++i)
        {
            addArrival(*gatherer, now, "p3", 1.0, TOptionalStr("i3"));
        }
        model.sample(now, now + bucketLength, resourceMonitor);
        now += bucketLength;
    }
    for (std::size_t i = 0; i < 9; ++i)
    {
        addArrival(*gatherer, now, "p1", 1.0, TOptionalStr("i1"));
    }
    for (std::size_t i = 0; i < 10; ++i)
    {
        addArrival(*gatherer, now, "p2", 1.0, TOptionalStr("i2"));
    }
    for (std::size_t i = 0; i < 8; ++i)
    {
        addArrival(*gatherer, now, "p3", 1.0, TOptionalStr("i3"));
    }
    model.sampleBucketStatistics(now, now + bucketLength, resourceMonitor);

    CPartitioningFields partitioningFields(EMPTY_STRING, EMPTY_STRING);
    model_t::CResultType type(model_t::CResultType::E_Conditional | model_t::CResultType::E_Interim);
    SAnnotatedProbability annotatedProbability1;
    annotatedProbability1.s_ResultType = type;
    CPPUNIT_ASSERT(model.computeProbability(pid1, now, now + bucketLength,
                                            partitioningFields, 1, annotatedProbability1));
    SAnnotatedProbability annotatedProbability2;
    annotatedProbability2.s_ResultType = type;
    CPPUNIT_ASSERT(model.computeProbability(pid2, now, now + bucketLength,
                                            partitioningFields, 1, annotatedProbability2));
    SAnnotatedProbability annotatedProbability3;
    annotatedProbability3.s_ResultType = type;
    CPPUNIT_ASSERT(model.computeProbability(pid3, now, now + bucketLength,
                                            partitioningFields, 1, annotatedProbability3));

    TDouble1Vec p1Baseline = model.baselineBucketMean(model_t::E_IndividualSumByBucketAndPerson,
                                                      pid1, 0, type,
                                                      annotatedProbability1.s_AttributeProbabilities[0].s_Correlated,
                                                      now);
    TDouble1Vec p2Baseline = model.baselineBucketMean(model_t::E_IndividualSumByBucketAndPerson,
                                                      pid2, 0, type,
                                                      annotatedProbability2.s_AttributeProbabilities[0].s_Correlated,
                                                      now);
    TDouble1Vec p3Baseline = model.baselineBucketMean(model_t::E_IndividualSumByBucketAndPerson,
                                                      pid3, 0, type,
                                                      annotatedProbability3.s_AttributeProbabilities[0].s_Correlated,
                                                      now);

    LOG_DEBUG("p1 probability = " << annotatedProbability1.s_Probability);
    LOG_DEBUG("p2 probability = " << annotatedProbability2.s_Probability);
    LOG_DEBUG("p3 probability = " << annotatedProbability3.s_Probability);
    LOG_DEBUG("p1 baseline = " << p1Baseline[0]);
    LOG_DEBUG("p2 baseline = " << p2Baseline[0]);
    LOG_DEBUG("p3 baseline = " << p3Baseline[0]);

    CPPUNIT_ASSERT(annotatedProbability1.s_Probability > 0.7);
    CPPUNIT_ASSERT(annotatedProbability2.s_Probability > 0.7);
    CPPUNIT_ASSERT(annotatedProbability3.s_Probability > 0.7);
    CPPUNIT_ASSERT(p1Baseline[0] > 8.4 && p1Baseline[0] < 8.6);
    CPPUNIT_ASSERT(p2Baseline[0] > 9.4 && p2Baseline[0] < 9.6);
    CPPUNIT_ASSERT(p3Baseline[0] > 7.4 && p3Baseline[0] < 7.6);
}

void CMetricModelTest::testCorrelatePersist(void)
{
    LOG_DEBUG("*** testCorrelatePersist ***");

    maths::CScopeDisableNormalizeOnRestore disabler;

    typedef maths::CVectorNx1<double, 2> TVector2;
    typedef maths::CSymmetricMatrixNxN<double, 2> TMatrix2;

    const core_t::TTime startTime = 0;
    const core_t::TTime bucketLength = 600;
    const double means[] = { 10.0, 20.0 };
    const double covariances[] = { 3.0, 2.0, 2.0 };
    TVector2 mean(means, means + 2);
    TMatrix2 covariance(covariances, covariances + 3);

    test::CRandomNumbers rng;

    TDoubleVecVec samples;
    rng.generateMultivariateNormalSamples(mean.toVector<TDoubleVec>(),
                                          covariance.toVectors<TDoubleVecVec>(),
                                          10000,
                                          samples);

    SModelParams params(bucketLength);
    params.s_DecayRate = 0.001;
    params.s_MultivariateByFields = true;
    CMetricModelFactory factory(params);
    CDataGatherer::TFeatureVec features(1, model_t::E_IndividualMeanByPerson);
    CModelFactory::TDataGathererPtr gatherer;
    CModel::TModelPtr model;
    makeModel(factory, features, startTime, bucketLength, gatherer, model);

    addPerson("p1", gatherer);
    addPerson("p2", gatherer);

    core_t::TTime time   = startTime;
    core_t::TTime bucket = time + bucketLength;
    CResourceMonitor resourceMonitor;
    for (std::size_t i = 0u; i < samples.size(); ++i, time += 60)
    {
        if (time >= bucket)
        {
            model->sample(bucket - bucketLength, bucket, resourceMonitor);
            bucket += bucketLength;
        }
        addArrival(*gatherer, time, "p1", samples[i][0]);
        addArrival(*gatherer, time, "p2", samples[i][0]);

        if ((i + 1) % 1000 == 0)
        {
            // Test persistence. (We check for idempotency.)
            std::string origXml;
            {
                core::CRapidXmlStatePersistInserter inserter("root");
                model->acceptPersistInserter(inserter);
                inserter.toXml(origXml);
            }

            // Restore the XML into a new filter
            core::CRapidXmlParser parser;
            CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(origXml));
            core::CRapidXmlStateRestoreTraverser traverser(parser);

            CModelFactory::SModelInitializationData initData(gatherer);
            CModel::TModelPtr restoredModel(factory.makeModel(initData, traverser));

            // The XML representation of the new filter should be the same as the original
            std::string newXml;
            {
                ml::core::CRapidXmlStatePersistInserter inserter("root");
                restoredModel->acceptPersistInserter(inserter);
                inserter.toXml(newXml);
            }

            uint64_t origChecksum = model->checksum(false);
            LOG_DEBUG("original checksum = " << origChecksum);
            uint64_t restoredChecksum = restoredModel->checksum(false);
            LOG_DEBUG("restored checksum = " << restoredChecksum);
            CPPUNIT_ASSERT_EQUAL(origChecksum, restoredChecksum);
            CPPUNIT_ASSERT_EQUAL(origXml, newXml);
        }
    }
}

void CMetricModelTest::testSummaryCountZeroRecordsAreIgnored(void)
{
    LOG_DEBUG("*** testSummaryCountZeroRecordsAreIgnored ***");

    core_t::TTime startTime(100);
    core_t::TTime bucketLength(100);
    SModelParams params(bucketLength);
    std::string summaryCountField("count");
    CMetricModelFactory factory(params, model_t::E_Manual, summaryCountField);
    CResourceMonitor resourceMonitor;

    CDataGatherer::TFeatureVec features;
    features.push_back(model_t::E_IndividualSumByBucketAndPerson);
    factory.features(features);
    factory.bucketLength(bucketLength);
    factory.fieldNames("", "", "P", "V", TStrVec(1, "I"));

    CModelFactory::SGathererInitializationData gathererWithZerosInitData(startTime);
    CModelFactory::TDataGathererPtr gathererWithZeros(factory.makeDataGatherer(gathererWithZerosInitData));
    CModelFactory::SModelInitializationData initDataWithZeros(gathererWithZeros);
    CModel::TModelPtr modelWithZerosPtr(factory.makeModel(initDataWithZeros));
    CPPUNIT_ASSERT(modelWithZerosPtr);
    CPPUNIT_ASSERT_EQUAL(model_t::E_MetricOnline, modelWithZerosPtr->category());
    CMetricModel &modelWithZeros = static_cast<CMetricModel&>(*modelWithZerosPtr.get());

    CModelFactory::SGathererInitializationData gathererNoZerosInitData(startTime);
    CModelFactory::TDataGathererPtr gathererNoZeros(factory.makeDataGatherer(gathererNoZerosInitData));
    CModelFactory::SModelInitializationData initDataNoZeros(gathererNoZeros);
    CModel::TModelPtr modelNoZerosPtr(factory.makeModel(initDataNoZeros));
    CPPUNIT_ASSERT(modelNoZerosPtr);
    CPPUNIT_ASSERT_EQUAL(model_t::E_MetricOnline, modelNoZerosPtr->category());
    CMetricModel &modelNoZeros = static_cast<CMetricModel&>(*modelNoZerosPtr.get());

    // The idea here is to compare a model that has records with summary count of zero
    // against a model that has no records at all where the first model had the zero-count records.

    core_t::TTime now = 100;
    core_t::TTime end = now + 50 * bucketLength;
    test::CRandomNumbers rng;
    double mean = 5.0;
    double variance = 2.0;
    TDoubleVec values;
    std::string summaryCountZero("0");
    std::string summaryCountOne("1");
    while (now < end)
    {
        for (std::size_t i = 0; i < 10; ++i)
        {
            rng.generateNormalSamples(mean, variance, 1, values);
            double value = values[0];
            rng.generateUniformSamples(0.0, 1.0, 1, values);
            if (values[0] < 0.05)
            {
                addArrival(*gathererWithZeros, now, "p1", value, TOptionalStr("i1"), TOptionalStr(), TOptionalStr(summaryCountZero));
            }
            else
            {
                addArrival(*gathererWithZeros, now, "p1", value, TOptionalStr("i1"), TOptionalStr(), TOptionalStr(summaryCountOne));
                addArrival(*gathererNoZeros, now, "p1", value, TOptionalStr("i1"), TOptionalStr(), TOptionalStr(summaryCountOne));
            }
        }
        modelWithZeros.sample(now, now + bucketLength, resourceMonitor);
        modelNoZeros.sample(now, now + bucketLength, resourceMonitor);
        now += bucketLength;
    }

    CPPUNIT_ASSERT_EQUAL(modelWithZeros.checksum(), modelNoZeros.checksum());
}

void CMetricModelTest::testDecayRateControl(void)
{
    LOG_DEBUG("*** testDecayRateControl ***");

    core_t::TTime startTime = 0;
    core_t::TTime bucketLength = 1800;

    model_t::EFeature feature = model_t::E_IndividualMeanByPerson;
    model_t::TFeatureVec features(1, feature);

    SModelParams params(bucketLength);
    params.s_DecayRate = 0.001;
    params.s_MinimumModeFraction = model::CModelConfig::DEFAULT_INDIVIDUAL_MINIMUM_MODE_FRACTION;

    test::CRandomNumbers rng;

    LOG_DEBUG("*** Test anomaly ***");
    {
        // Test we don't adapt the decay rate if there is a short-lived
        // anomaly. We should get essentially identical prediction errors
        // with and without decay control.

        params.s_ControlDecayRate = true;
        params.s_DecayRate = 0.001;
        CMetricModelFactory factory(params);
        CModelFactory::TDataGathererPtr gatherer;
        CModel::TModelPtr model;
        makeModel(factory, features, startTime, bucketLength, gatherer, model);

        params.s_ControlDecayRate = false;
        params.s_DecayRate = 0.0001;
        CMetricModelFactory referenceFactory(params);
        CModelFactory::TDataGathererPtr referenceGatherer;
        CModel::TModelPtr referenceModel;
        makeModel(referenceFactory, features, startTime, bucketLength, referenceGatherer, referenceModel);

        CResourceMonitor resourceMonitor;

        TMeanAccumulator meanPredictionError;
        TMeanAccumulator meanReferencePredictionError;
        model_t::CResultType type(model_t::CResultType::E_Unconditional | model_t::CResultType::E_Interim);
        for (core_t::TTime t = 0; t < 4 * core::constants::WEEK; t += bucketLength)
        {
            if (t % core::constants::WEEK == 0)
            {
                LOG_DEBUG("week " << t / core::constants::WEEK + 1);
            }

            TDoubleVec value;
            rng.generateUniformSamples(0.0, 10.0, 1, value);
            value[0] += 20.0 * (t > 3 * core::constants::WEEK && t < core::constants::WEEK + 4 * 3600 ? 1.0 : 0.0);
            addArrival(*gatherer, t + bucketLength / 2, "p1", value[0]);
            addArrival(*referenceGatherer, t + bucketLength / 2, "p1", value[0]);
            model->sample(t, t + bucketLength, resourceMonitor);
            referenceModel->sample(t, t + bucketLength, resourceMonitor);
            meanPredictionError.add(::fabs(
                    model->currentBucketValue(feature, 0, 0, t + bucketLength / 2)[0]
                  - model->baselineBucketMean(feature, 0, 0, type,
                                              NO_CORRELATES, t + bucketLength / 2)[0]));
            meanReferencePredictionError.add(::fabs(
                    referenceModel->currentBucketValue(feature, 0, 0, t + bucketLength / 2)[0]
                  - referenceModel->baselineBucketMean(feature, 0, 0, type,
                                                       NO_CORRELATES, t + bucketLength / 2)[0]));
        }
        LOG_DEBUG("mean = " << maths::CBasicStatistics::mean(meanPredictionError));
        LOG_DEBUG("reference = " << maths::CBasicStatistics::mean(meanReferencePredictionError));
        CPPUNIT_ASSERT_DOUBLES_EQUAL(maths::CBasicStatistics::mean(meanReferencePredictionError),
                                     maths::CBasicStatistics::mean(meanPredictionError),
                                     0.01);
    }

    LOG_DEBUG("*** Test step change ***");
    {
        // Test a step change in a stable signal is detected and we get a
        // significant reduction in the prediction error.

        params.s_ControlDecayRate = true;
        params.s_DecayRate = 0.001;
        CMetricModelFactory factory(params);
        CModelFactory::TDataGathererPtr gatherer;
        CModel::TModelPtr model;
        makeModel(factory, features, startTime, bucketLength, gatherer, model);

        params.s_ControlDecayRate = false;
        params.s_DecayRate = 0.001;
        CMetricModelFactory referenceFactory(params);
        CModelFactory::TDataGathererPtr referenceGatherer;
        CModel::TModelPtr referenceModel;
        makeModel(referenceFactory, features, startTime, bucketLength, referenceGatherer, referenceModel);

        CResourceMonitor resourceMonitor;

        TMeanAccumulator meanPredictionError;
        TMeanAccumulator meanReferencePredictionError;
        model_t::CResultType type(model_t::CResultType::E_Unconditional | model_t::CResultType::E_Interim);
        for (core_t::TTime t = 0; t < 10 * core::constants::WEEK; t += bucketLength)
        {
            if (t % core::constants::WEEK == 0)
            {
                LOG_DEBUG("week " << t / core::constants::WEEK + 1);
            }

            double value = 10.0 * (1.0 + ::sin(  boost::math::double_constants::two_pi
                                               * static_cast<double>(t)
                                               / static_cast<double>(core::constants::DAY)))
                                * (t < 5 * core::constants::WEEK ? 1.0 : 2.0);
            TDoubleVec noise;
            rng.generateUniformSamples(0.0, 3.0, 1, noise);
            addArrival(*gatherer, t + bucketLength / 2, "p1", value + noise[0]);
            addArrival(*referenceGatherer, t + bucketLength / 2, "p1", value + noise[0]);
            model->sample(t, t + bucketLength, resourceMonitor);
            referenceModel->sample(t, t + bucketLength, resourceMonitor);
            meanPredictionError.add(::fabs(
                    model->currentBucketValue(feature, 0, 0, t + bucketLength / 2)[0]
                  - model->baselineBucketMean(feature, 0, 0, type,
                                              NO_CORRELATES, t + bucketLength / 2)[0]));
            meanReferencePredictionError.add(::fabs(
                    referenceModel->currentBucketValue(feature, 0, 0, t + bucketLength / 2)[0]
                  - referenceModel->baselineBucketMean(feature, 0, 0, type,
                                                       NO_CORRELATES, t + bucketLength / 2)[0]));
        }
        LOG_DEBUG("mean = " << maths::CBasicStatistics::mean(meanPredictionError));
        LOG_DEBUG("reference = " << maths::CBasicStatistics::mean(meanReferencePredictionError));
        CPPUNIT_ASSERT(        maths::CBasicStatistics::mean(meanPredictionError)
                       < 0.82 * maths::CBasicStatistics::mean(meanReferencePredictionError));
    }

    LOG_DEBUG("*** Test unmodelled cyclic component ***");
    {
        // This modulates the event rate using a sine with period 10 weeks
        // effectively there are significant "manoeuvres" in the event rate
        // every 5 weeks at the function turning points. We check we get a
        // significant reduction in the prediction error.

        params.s_ControlDecayRate = true;
        params.s_DecayRate = 0.001;
        CMetricModelFactory factory(params);
        CModelFactory::TDataGathererPtr gatherer;
        CModel::TModelPtr model;
        makeModel(factory, features, startTime, bucketLength, gatherer, model);

        params.s_ControlDecayRate = false;
        params.s_DecayRate = 0.001;
        CMetricModelFactory referenceFactory(params);
        CModelFactory::TDataGathererPtr referenceGatherer;
        CModel::TModelPtr referenceModel;
        makeModel(referenceFactory, features, startTime, bucketLength, referenceGatherer, referenceModel);

        CResourceMonitor resourceMonitor;

        TMeanAccumulator meanPredictionError;
        TMeanAccumulator meanReferencePredictionError;
        model_t::CResultType type(model_t::CResultType::E_Unconditional | model_t::CResultType::E_Interim);
        for (core_t::TTime t = 0; t < 20 * core::constants::WEEK; t += bucketLength)
        {
            if (t % core::constants::WEEK == 0)
            {
                LOG_DEBUG("week " << t / core::constants::WEEK + 1);
            }

            double value = 10.0 * (1.0 + ::sin(  boost::math::double_constants::two_pi
                                               * static_cast<double>(t)
                                               / static_cast<double>(core::constants::DAY)))
                                * (1.0 + ::sin(  boost::math::double_constants::two_pi
                                               * static_cast<double>(t)
                                               / 10.0 / static_cast<double>(core::constants::WEEK)));
            TDoubleVec noise;
            rng.generateUniformSamples(0.0, 3.0, 1, noise);
            addArrival(*gatherer, t + bucketLength / 2, "p1", value + noise[0]);
            addArrival(*referenceGatherer, t + bucketLength / 2, "p1", value + noise[0]);
            model->sample(t, t + bucketLength, resourceMonitor);
            referenceModel->sample(t, t + bucketLength, resourceMonitor);
            meanPredictionError.add(::fabs(
                    model->currentBucketValue(feature, 0, 0, t + bucketLength / 2)[0]
                  - model->baselineBucketMean(feature, 0, 0, type,
                                              NO_CORRELATES, t + bucketLength / 2)[0]));
            meanReferencePredictionError.add(::fabs(
                    referenceModel->currentBucketValue(feature, 0, 0, t + bucketLength / 2)[0]
                  - referenceModel->baselineBucketMean(feature, 0, 0, type,
                                                       NO_CORRELATES, t + bucketLength / 2)[0]));
        }
        LOG_DEBUG("mean = " << maths::CBasicStatistics::mean(meanPredictionError));
        LOG_DEBUG("reference = " << maths::CBasicStatistics::mean(meanReferencePredictionError));
        CPPUNIT_ASSERT(        maths::CBasicStatistics::mean(meanPredictionError)
                       < 0.8 * maths::CBasicStatistics::mean(meanReferencePredictionError));
    }
}

CppUnit::Test *CMetricModelTest::suite(void)
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CMetricModelTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testOnlineSample",
                                   &CMetricModelTest::testOnlineSample) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testOnlineMultivariateSample",
                                   &CMetricModelTest::testOnlineMultivariateSample) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testOnlineProbabilityCalculationForMetric",
                                   &CMetricModelTest::testOnlineProbabilityCalculationForMetric) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testOnlineProbabilityCalculationForMedian",
                                   &CMetricModelTest::testOnlineProbabilityCalculationForMedian) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testOnlineProbabilityCalculationForLowMean",
                                   &CMetricModelTest::testOnlineProbabilityCalculationForLowMean) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testOnlineProbabilityCalculationForHighMean",
                                   &CMetricModelTest::testOnlineProbabilityCalculationForHighMean) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testOnlineProbabilityCalculationForLowSum",
                                   &CMetricModelTest::testOnlineProbabilityCalculationForLowSum) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testOnlineProbabilityCalculationForHighSum",
                                   &CMetricModelTest::testOnlineProbabilityCalculationForHighSum) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testOnlineProbabilityCalculationForLatLong",
                                   &CMetricModelTest::testOnlineProbabilityCalculationForLatLong) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testMinInfluence",
                                   &CMetricModelTest::testMinInfluence) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testMeanInfluence",
                                   &CMetricModelTest::testMeanInfluence) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testMaxInfluence",
                                   &CMetricModelTest::testMaxInfluence) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testSumInfluence",
                                   &CMetricModelTest::testSumInfluence) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testLatLongInfluence",
                                   &CMetricModelTest::testLatLongInfluence) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testPrune",
                                   &CMetricModelTest::testPrune) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testKey",
                                   &CMetricModelTest::testKey) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testSkipSampling",
                                   &CMetricModelTest::testSkipSampling) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testExplicitNulls",
                                   &CMetricModelTest::testExplicitNulls) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testVarp",
                                   &CMetricModelTest::testVarp) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testInterimCorrections",
                                   &CMetricModelTest::testInterimCorrections) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testInterimCorrectionsWithCorrelations",
                                   &CMetricModelTest::testInterimCorrectionsWithCorrelations) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testCorrelatePersist",
                                   &CMetricModelTest::testCorrelatePersist) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testSummaryCountZeroRecordsAreIgnored",
                                   &CMetricModelTest::testSummaryCountZeroRecordsAreIgnored) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testSummaryCountZeroRecordsAreIgnored",
                                   &CMetricModelTest::testSummaryCountZeroRecordsAreIgnored) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMetricModelTest>(
                                   "CMetricModelTest::testDecayRateControl",
                                   &CMetricModelTest::testDecayRateControl) );

    return suiteOfTests;
}
