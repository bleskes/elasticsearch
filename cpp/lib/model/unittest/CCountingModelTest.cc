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

#include "CCountingModelTest.h"

#include <core/CContainerPrinter.h>
#include <core/CoreTypes.h>
#include <core/CLogger.h>

#include <model/CDataGatherer.h>
#include <model/CCountingModelFactory.h>
#include <model/CCountingModel.h>
#include <model/CDataGatherer.h>
#include <model/CEventData.h>
#include <model/CResourceMonitor.h>
#include <model/ModelTypes.h>

#include <string>
#include <vector>

using namespace prelert;
using namespace model;


namespace
{
std::size_t addPerson(const std::string &p,
                      const CModelFactory::TDataGathererPtr &gatherer)
{
    CDataGatherer::TStrCPtrVec person;
    person.push_back(&p);
    CEventData result;
    CResourceMonitor resourceMonitor;
    gatherer->processFields(person, result, resourceMonitor);
    return *result.personId();
}

void addArrival(CDataGatherer &gatherer, core_t::TTime time, const std::string &person)
{
    CDataGatherer::TStrCPtrVec fieldValues;
    fieldValues.push_back(&person);

    CEventData eventData;
    eventData.time(time);
    CResourceMonitor resourceMonitor;
    gatherer.addArrival(fieldValues, eventData, resourceMonitor);
}
}

void CCountingModelTest::testSkipSampling(void)
{
    LOG_DEBUG("*** testSkipSampling ***");

    core_t::TTime startTime(100);
    core_t::TTime bucketLength(100);
    std::size_t maxAgeBuckets(1);

    SModelParams params(bucketLength);
    params.s_DecayRate = 0.001;
    CCountingModelFactory factory(params);
    model_t::TFeatureVec features(1u, model_t::E_IndividualCountByBucketAndPerson);
    factory.features(features);
    CResourceMonitor resourceMonitor;

    // Model where gap is not skipped
    {
        CModelFactory::SGathererInitializationData gathererNoGapInitData(startTime);
        CModelFactory::TDataGathererPtr gathererNoGap(factory.makeDataGatherer(gathererNoGapInitData));
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), addPerson("p", gathererNoGap));
        CModelFactory::SModelInitializationData modelNoGapInitData(gathererNoGap);
        CModel::TModelPtr modelHolderNoGap(factory.makeModel(modelNoGapInitData));
        CCountingModel *modelNoGap = dynamic_cast<CCountingModel*>(modelHolderNoGap.get());

        // |2|2|0|0|1| -> 1.0 mean count
        addArrival(*gathererNoGap, 100, "p");
        addArrival(*gathererNoGap, 110, "p");
        modelNoGap->sample(100, 200, resourceMonitor);
        addArrival(*gathererNoGap, 250, "p");
        addArrival(*gathererNoGap, 280, "p");
        modelNoGap->sample(200, 500, resourceMonitor);
        addArrival(*gathererNoGap, 500, "p");
        modelNoGap->sample(500, 600, resourceMonitor);

        CPPUNIT_ASSERT_EQUAL(1.0, *modelNoGap->baselineBucketCount(0));
    }

    // Model where gap is skipped
    {
        CModelFactory::SGathererInitializationData gathererWithGapInitData(startTime);
        CModelFactory::TDataGathererPtr gathererWithGap(factory.makeDataGatherer(gathererWithGapInitData));
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), addPerson("p", gathererWithGap));
        CModelFactory::SModelInitializationData modelWithGapInitData(gathererWithGap);
        CModel::TModelPtr modelHolderWithGap(factory.makeModel(modelWithGapInitData));
        CCountingModel *modelWithGap = dynamic_cast<CCountingModel*>(modelHolderWithGap.get());

        // |2|2|0|0|1|
        // |2|X|X|X|1| -> 1.5 mean count where X means skipped bucket
        addArrival(*gathererWithGap, 100, "p");
        addArrival(*gathererWithGap, 110, "p");
        modelWithGap->sample(100, 200, resourceMonitor);
        addArrival(*gathererWithGap, 250, "p");
        addArrival(*gathererWithGap, 280, "p");
        modelWithGap->skipSampling(500);
        modelWithGap->prune(maxAgeBuckets);
        CPPUNIT_ASSERT_EQUAL(std::size_t(1), gathererWithGap->numberActivePeople());
        addArrival(*gathererWithGap, 500, "p");
        modelWithGap->sample(500, 600, resourceMonitor);

        CPPUNIT_ASSERT_EQUAL(1.5, *modelWithGap->baselineBucketCount(0));
    }
}

CppUnit::Test *CCountingModelTest::suite(void)
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CCountingModelTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CCountingModelTest>(
                                   "CCountingModelTest::testSkipSampling",
                                   &CCountingModelTest::testSkipSampling) );
    return suiteOfTests;
}
