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

#include "CModelEnsembleTest.h"

#include <core/CContainerPrinter.h>
#include <core/CoreTypes.h>

#include <maths/CPrior.h>
#include <maths/CBasicStatistics.h>

#include <model/CDataGatherer.h>
#include <model/CEventData.h>
#include <model/CModelEnsemble.h>
#include <model/CResourceMonitor.h>

#include <boost/range.hpp>

#include <map>
#include <utility>
#include <vector>

using namespace prelert;
using namespace model;

typedef std::pair<core_t::TTime, std::size_t> TTimeSizePr;
typedef std::vector<std::size_t> TSizeVec;
typedef std::vector<model_t::EFeature> TFeatureVec;
typedef std::map<std::size_t, uint64_t> TSizeUInt64Map;
typedef std::vector<TSizeUInt64Map> TSizeUInt64MapVec;

namespace
{

std::size_t addPerson(const std::string &p,
                      CModelEnsemble &ensemble)
{
    CModelEnsemble::TStrCPtrVec person;
    person.push_back(&p);
    CEventData result;
    CResourceMonitor resourceMonitor;
    ensemble.processFields(person, result, resourceMonitor);
    return *result.personId();
}

}

void CModelEnsembleTest::testAccessors(void)
{
    const core_t::TTime bucketLength = 3600;
    const core_t::TTime startTime = 1366963200;

    CModelConfig config = CModelConfig::defaultConfig(bucketLength);
    CModelEnsemble ensemble(config, config.factory(1, function_t::E_IndividualCount), startTime);

    std::size_t p1 = addPerson("p1", ensemble);
    addPerson("p2", ensemble);
    std::size_t p3 = addPerson("p3", ensemble);

    CPPUNIT_ASSERT_EQUAL(static_cast<std::size_t>(3), ensemble.numberActivePeople());

    {
        boost::any p1ExtraData(std::string("foo"));
        ensemble.extraData(startTime, p1, p1ExtraData);
    }
    {
        boost::any p3ExtraData(std::string("bar"));
        ensemble.extraData(startTime, p3, p3ExtraData);
    }

    CPPUNIT_ASSERT_EQUAL(static_cast<std::size_t>(1), ensemble.numberBatches());
    CPPUNIT_ASSERT_EQUAL(static_cast<std::size_t>(0), ensemble.batch(startTime));
    CPPUNIT_ASSERT_EQUAL(startTime, ensemble.comparedTime(startTime));

    CModel *model = ensemble.model(startTime);
    CPPUNIT_ASSERT(model);
    boost::any p1ExtraData = model->extraData(p1);
    CPPUNIT_ASSERT_EQUAL(std::string("foo"),
                         boost::any_cast<std::string>(p1ExtraData));
    boost::any p3ExtraData = model->extraData(p3);
    CPPUNIT_ASSERT_EQUAL(std::string("bar"),
                         boost::any_cast<std::string>(p3ExtraData));

    ensemble.clearModels();
    CPPUNIT_ASSERT_EQUAL(static_cast<std::size_t>(0), ensemble.numberActivePeople());
    CPPUNIT_ASSERT_EQUAL(static_cast<std::size_t>(0), ensemble.numberBatches());
}

void CModelEnsembleTest::testPruneModels(void)
{
    // TODO
}

void CModelEnsembleTest::testPersistence(void)
{
    // TODO
}

CppUnit::Test *CModelEnsembleTest::suite(void)
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CModelEnsembleTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CModelEnsembleTest>(
                                   "CModelEnsembleTest::testAccessors",
                                   &CModelEnsembleTest::testAccessors) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CModelEnsembleTest>(
                                   "CModelEnsembleTest::testPruneModels",
                                   &CModelEnsembleTest::testPruneModels) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CModelEnsembleTest>(
                                   "CModelEnsembleTest::testPersistence",
                                   &CModelEnsembleTest::testPersistence) );

    return suiteOfTests;
}
