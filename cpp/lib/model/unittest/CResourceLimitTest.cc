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
#include "CResourceLimitTest.h"

#include <core/CRegex.h>

#include <maths/CIntegerTools.h>

#include <model/CAnomalyDetector.h>
#include <model/CDataGatherer.h>
#include <model/CEventRateModel.h>
#include <model/CEventRateModelFactory.h>
#include <model/CEventRatePopulationModel.h>
#include <model/CEventRatePopulationModelFactory.h>
#include <model/CHierarchicalResults.h>
#include <model/CHierarchicalResultsAggregator.h>
#include <model/CHierarchicalResultsPopulator.h>
#include <model/CHierarchicalResultsProbabilityFinalizer.h>
#include <model/CLimits.h>
#include <model/CMetricModel.h>
#include <model/CMetricModelFactory.h>
#include <model/CModelConfig.h>
#include <model/CResourceMonitor.h>

#include <boost/tuple/tuple.hpp>
#include <boost/tuple/tuple_io.hpp>

#include <vector>

using namespace ml;
using namespace model;

typedef std::vector<std::string> TStrVec;

class CResultWriter : public ml::model::CHierarchicalResultsVisitor
{
    public:
        typedef boost::tuple<core_t::TTime,
                             double /* probability */,
                             std::string /* byFieldName*/,
                             std::string /* overFieldName */,
                             std::string /* partitionFieldName */> TResultsTp;
        typedef std::vector<TResultsTp> TResultsVec;

    public:
        CResultWriter(const CModelConfig &modelConfig,
                      const CLimits &limits) :
                m_ModelConfig(modelConfig),
                m_Limits(limits)
        {
        }

        void operator()(CAnomalyDetector &detector,
                        core_t::TTime start,
                        core_t::TTime end)
        {
            CHierarchicalResults results;
            detector.buildResults(start, end, results);
            results.buildHierarchy();
            CHierarchicalResultsAggregator aggregator(m_ModelConfig);
            results.bottomUpBreadthFirst(aggregator);
            model::CHierarchicalResultsProbabilityFinalizer finalizer;
            results.bottomUpBreadthFirst(finalizer);
            model::CHierarchicalResultsPopulator populator(m_Limits);
            results.bottomUpBreadthFirst(populator);
            results.bottomUpBreadthFirst(*this);
        }

        virtual void visit(const ml::model::CHierarchicalResults &results,
                           const ml::model::CHierarchicalResults::TNode &node,
                           bool pivot)
        {
            if (pivot)
            {
                return;
            }
            if (!this->shouldWriteResult(m_Limits, results, node, pivot))
            {
                return;
            }
            if (this->isSimpleCount(node))
            {
                return;
            }
            if (!this->isLeaf(node))
            {
                return;
            }

            LOG_DEBUG("Got anomaly @ " << node.s_BucketStartTime
                      << ": " << node.probability());

            ml::model::SAnnotatedProbability::TAttributeProbability1Vec &attributes =
                node.s_AnnotatedProbability.s_AttributeProbabilities;

            m_Results.push_back(TResultsTp(node.s_BucketStartTime,
                                           node.probability(),
                                           (attributes.empty() ? "" : *attributes[0].s_Attribute),
                                           *node.s_Spec.s_PersonFieldValue,
                                           *node.s_Spec.s_PartitionFieldValue));
        }

        bool operator()(ml::core_t::TTime time,
                       const ml::model::CHierarchicalResults::TNode &node,
                       bool isBucketInfluencer)
        {
            LOG_DEBUG((isBucketInfluencer ? "BucketInfluencer" :  "Influencer ")
                    << node.s_Spec.print() << " initial score " << node.probability()
                    << ", time:  " << time);

            return true;
        }

        const TResultsVec &results(void) const
        {
            return m_Results;
        }

    private:
        const CModelConfig &m_ModelConfig;
        const CLimits &m_Limits;
        TResultsVec m_Results;
};

CppUnit::Test* CResourceLimitTest::suite(void)
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CResourceLimitTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CResourceLimitTest>(
                                   "CResourceLimitTest::testLimitBy",
                                   &CResourceLimitTest::testLimitBy) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CResourceLimitTest>(
                                   "CResourceLimitTest::testLimitByOver",
                                   &CResourceLimitTest::testLimitByOver) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CResourceLimitTest>(
                                   "CResourceLimitTest::testLargeAllocations",
                                   &CResourceLimitTest::testLargeAllocations) );
    return suiteOfTests;
}

void CResourceLimitTest::testLimitBy(void)
{
    // Check that we can get some results from a test data set, then
    // turn on resource limiting and still get the same results

    static const core_t::TTime BUCKET_LENGTH(3600);
    static const core_t::TTime FIRST_TIME(maths::CIntegerTools::ceil(core_t::TTime(1407428000),
                                                                     BUCKET_LENGTH));
    ::CResultWriter::TResultsVec results;

    {
        CModelConfig modelConfig = CModelConfig::defaultConfig(BUCKET_LENGTH);
        CLimits limits;
        CSearchKey key(1, // identifier
                       function_t::E_IndividualMetric,
                       false,
                       model_t::E_XF_None,
                       "value", "colour");
        CAnomalyDetector detector(1, // identifier
                                  limits,
                                  modelConfig,
                                  "",
                                  FIRST_TIME,
                                  modelConfig.factory(key));
        ::CResultWriter writer(modelConfig, limits);

        importCsvDataWithLimiter(FIRST_TIME,
                                 BUCKET_LENGTH,
                                 writer,
                                 "testfiles/resource_limits_8_series.csv",
                                 detector,
                                 std::numeric_limits<std::size_t>::max(),
                                 limits.resourceMonitor());

        results = writer.results();

        // expect there to be 2 anomalies
        CPPUNIT_ASSERT_EQUAL(std::size_t(2), results.size());
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(1407571200), results[0].get<0>());
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(1407715200), results[1].get<0>());
        CPPUNIT_ASSERT_EQUAL(std::size_t(8), detector.numberActivePeople());
    }
    {
        // This time, repeat the test but set a resource limit to prevent more than 2
        // "by" fields being added - we expect the same results...
        CModelConfig modelConfig = CModelConfig::defaultConfig(BUCKET_LENGTH);
        CLimits limits;
        CSearchKey key(1, // identifier
                       function_t::E_IndividualMetric,
                       false,
                       model_t::E_XF_None,
                       "value", "colour");
        CAnomalyDetector detector(1, // identifier
                                  limits,
                                  modelConfig,
                                  "",
                                  FIRST_TIME,
                                  modelConfig.factory(key));
        ::CResultWriter writer(modelConfig, limits);

        importCsvDataWithLimiter(FIRST_TIME,
                                 BUCKET_LENGTH,
                                 writer,
                                 "testfiles/resource_limits_8_series.csv",
                                 detector,
                                 1,
                                 limits.resourceMonitor());

        const ::CResultWriter::TResultsVec &secondResults = writer.results();

        // expect there to be 2 anomalies
        CPPUNIT_ASSERT_EQUAL(std::size_t(2), results.size());
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(1407571200), secondResults[0].get<0>());
        CPPUNIT_ASSERT_DOUBLES_EQUAL(results[0].get<1>(),
                                     secondResults[0].get<1>(),
                                     results[1].get<1>() * 0.01);
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(1407715200), secondResults[1].get<0>());
        CPPUNIT_ASSERT_DOUBLES_EQUAL(results[1].get<1>() ,
                                     secondResults[1].get<1>() ,
                                     results[1].get<1>()  * 0.01);
        CPPUNIT_ASSERT_EQUAL(std::size_t(2), detector.numberActivePeople());
    }
}

void CResourceLimitTest::testLimitByOver(void)
{
    // Check that we can get some results from a test data set, then
    // turn on resource limiting and still get the results from
    // non-limited data, but not results from limited data

    static const core_t::TTime BUCKET_LENGTH(3600);
    static const core_t::TTime FIRST_TIME(maths::CIntegerTools::ceil(core_t::TTime(1407441600),
                                          BUCKET_LENGTH));
    ::CResultWriter::TResultsVec results;

    core_t::TTime redPetuniaTime = 0;
    {
        CModelConfig modelConfig = CModelConfig::defaultConfig(BUCKET_LENGTH);
        CLimits limits;
        CSearchKey key(1, // identifier
                       function_t::E_PopulationMetric,
                       false,
                       model_t::E_XF_None,
                       "value", "colour", "species");
        CAnomalyDetector detector(1, // identifier
                                  limits,
                                  modelConfig,
                                  "",
                                  FIRST_TIME,
                                  modelConfig.factory(key));
        ::CResultWriter writer(modelConfig, limits);

        importCsvDataWithLimiter(FIRST_TIME,
                                 BUCKET_LENGTH,
                                 writer,
                                 "testfiles/resource_limits_8_2over.csv",
                                 detector,
                                 std::numeric_limits<std::size_t>::max(),
                                 limits.resourceMonitor());

        results = writer.results();

        for (std::size_t i = 0; i < results.size(); i++)
        {
            LOG_DEBUG("Result: " << results[i]);
            if (results[i].get<2>() == "red" && results[i].get<3>() == "petunia")
            {
                redPetuniaTime = results[i].get<0>();
            }
        }

        // check we have the expected 4 anomalies
        CPPUNIT_ASSERT_EQUAL(std::size_t(4), results.size());
        CPPUNIT_ASSERT_EQUAL(std::size_t(2), detector.numberActivePeople());
        CPPUNIT_ASSERT_EQUAL(std::size_t(3), detector.numberActiveAttributes());
    }

    // Now limit after 1 sample, so only expect red petunias
    CModelConfig modelConfig = CModelConfig::defaultConfig(BUCKET_LENGTH);
    CLimits limits;
    CSearchKey key(1, // identifier
                   function_t::E_PopulationMetric,
                   false,
                   model_t::E_XF_None,
                   "value", "colour", "species");
    CAnomalyDetector detector(1, // identifier
                              limits,
                              modelConfig,
                              "",
                              FIRST_TIME,
                              modelConfig.factory(key));
    ::CResultWriter writer(modelConfig, limits);

    importCsvDataWithLimiter(FIRST_TIME,
                             BUCKET_LENGTH,
                             writer,
                             "testfiles/resource_limits_8_2over.csv",
                             detector,
                             1,
                             limits.resourceMonitor());

    const ::CResultWriter::TResultsVec &secondResults = writer.results();

    // should only have red flowers as results now
    CPPUNIT_ASSERT_EQUAL(std::size_t(2), secondResults.size());
    for (std::size_t i = 0; i < secondResults.size(); i++)
    {
        LOG_DEBUG("Result: " << secondResults[i]);
        const std::string &colour = secondResults[i].get<2>();
        if (!colour.empty())
        {
            CPPUNIT_ASSERT_EQUAL(std::string("red"), secondResults[i].get<2>());
        }
        if (secondResults[i].get<3>() == "petunia")
        {
            CPPUNIT_ASSERT_EQUAL(redPetuniaTime, secondResults[i].get<0>());
        }
    }

    CPPUNIT_ASSERT_EQUAL(std::size_t(2), detector.numberActivePeople());
    CPPUNIT_ASSERT_EQUAL(std::size_t(1), detector.numberActiveAttributes());
}

namespace
{

//! A test wrapper around a real model that tracks calls to createNewModels
//! and simulates taking lots of memory
class CMockEventRateModel : public ml::model::CEventRateModel
{
    public:
        CMockEventRateModel(const SModelParams &params,
                            const TDataGathererPtr &dataGatherer,
                            const TFeaturePriorPtrPrVec &newPriors,
                            const maths::CMultinomialConjugate &personProbabilityPrior,
                            const TFeatureDecompositionCPtrVecPrVec &newDecompositions,
                            const TFeatureInfluenceCalculatorCPtrPrVecVec &influenceCalculators,
                            CResourceMonitor &resourceMonitor) :
            CEventRateModel(params,
                            dataGatherer,
                            newPriors,
                            TFeatureMultivariatePriorPtrPrVec(),
                            TFeatureMultivariatePriorPtrPrVec(),
                            personProbabilityPrior,
                            newDecompositions,
                            influenceCalculators),
            m_ResourceMonitor(resourceMonitor),
            m_NewPeople(0),
            m_NewAttributes(0),
            m_MemoryBlown(false),
            m_Call(0)
        {}

        void blowMemory(void)
        {
            m_MemoryBlown = true;
        }

        virtual std::size_t memoryUsage(void) const
        {
            if (m_MemoryBlown)
            {
                return m_ResourceMonitor.allocationLimit() * (++m_Call);
            }
            return this->CEventRateModel::memoryUsage();
        }

        virtual std::size_t estimateMemoryUsage(std::size_t numberPeople,
                                                std::size_t numberAttributes,
                                                std::size_t numberCorrelations) const
        {
            if (m_MemoryBlown)
            {
                return m_ResourceMonitor.allocationLimit() * (++m_Call);
            }
            return this->CEventRateModel::estimateMemoryUsage(numberPeople,
                                                              numberAttributes,
                                                              numberCorrelations);
        }

        virtual void updateRecycledModels(void)
        {
            // Do nothing
        }

        virtual void createNewModels(std::size_t n, std::size_t m)
        {
            m_NewPeople += n;
            m_NewAttributes += m;
            this->CEventRateModel::createNewModels(n, m);
        }

        void test(core_t::TTime time)
        {
            this->createUpdateNewModels(time, m_ResourceMonitor);
        }

        std::size_t getNewPeople(void) const
        {
            return m_NewPeople;
        }

        std::size_t getNewAttributes(void) const
        {
            return m_NewAttributes;
        }

    private:
        CResourceMonitor &m_ResourceMonitor;
        std::size_t m_NewPeople;
        std::size_t m_NewAttributes;
        bool m_MemoryBlown;
        mutable int m_Call;
};

//! A test wrapper around a real model that tracks calls to createNewModels
//! and simulates taking lots of memory
class CMockMetricModel : public ml::model::CMetricModel
{
    public:
        CMockMetricModel(const SModelParams &params,
                         const TDataGathererPtr &dataGatherer,
                         const TFeaturePriorPtrPrVec &newPriors,
                         const TFeatureDecompositionCPtrVecPrVec &newDecompositions,
                         const TFeatureInfluenceCalculatorCPtrPrVecVec &influenceCalculators,
                         CResourceMonitor &resourceMonitor) :
            CMetricModel(params,
                         dataGatherer,
                         newPriors,
                         TFeatureMultivariatePriorPtrPrVec(),
                         TFeatureMultivariatePriorPtrPrVec(),
                         newDecompositions,
                         influenceCalculators),
            m_ResourceMonitor(resourceMonitor),
            m_NewPeople(0),
            m_NewAttributes(0),
            m_MemoryBlown(false),
            m_Call(0)
        {}

        void blowMemory(void)
        {
            m_MemoryBlown = true;
        }


        virtual std::size_t memoryUsage(void) const
        {
            if (m_MemoryBlown)
            {
                return m_ResourceMonitor.allocationLimit() * (++m_Call);
            }
            return this->CMetricModel::memoryUsage();
        }

        virtual std::size_t estimateMemoryUsage(std::size_t numberPeople,
                                                std::size_t numberAttributes,
                                                std::size_t numberCorrelations) const
        {
            if (m_MemoryBlown)
            {
                return m_ResourceMonitor.allocationLimit() * (++m_Call);
            }
            return this->CMetricModel::estimateMemoryUsage(numberPeople,
                                                                 numberAttributes,
                                                                 numberCorrelations);
        }

        virtual void updateRecycledModels(void)
        {
            // Do nothing
        }

        virtual void createNewModels(std::size_t n, std::size_t m)
        {
            m_NewPeople += n;
            m_NewAttributes += m;
            this->CMetricModel::createNewModels(n, m);
        }

        void test(core_t::TTime time)
        {
            this->createUpdateNewModels(time, m_ResourceMonitor);
        }

        std::size_t getNewPeople(void) const
        {
            return m_NewPeople;
        }

        std::size_t getNewAttributes(void) const
        {
            return m_NewAttributes;
        }

    private:
        CResourceMonitor &m_ResourceMonitor;
        std::size_t m_NewPeople;
        std::size_t m_NewAttributes;
        bool m_MemoryBlown;
        mutable int m_Call;
};

void addArrival(core_t::TTime time,
                const std::string &p,
                CDataGatherer &gatherer,
                CResourceMonitor &resourceMonitor)
{
    CDataGatherer::TStrCPtrVec fields;
    fields.push_back(&p);
    CEventData result;
    result.time(time);
    gatherer.addArrival(fields, result, resourceMonitor);
}

void addPersonData(std::size_t start,
                   std::size_t end,
                   core_t::TTime time,
                   CDataGatherer &gatherer,
                   CResourceMonitor &resourceMonitor)
{
    for (std::size_t i = start; i < end; i++)
    {
        std::ostringstream ssA;
        ssA << "person" << i;
        addArrival(time, ssA.str(), gatherer, resourceMonitor);
    }
}

const std::string VALUE("23");

void addMetricArrival(core_t::TTime time,
                      const std::string &p,
                      CDataGatherer &gatherer,
                      CResourceMonitor &resourceMonitor)
{
    CDataGatherer::TStrCPtrVec fields;
    fields.push_back(&p);
    fields.push_back(&VALUE);
    CEventData result;
    result.time(time);
    gatherer.addArrival(fields, result, resourceMonitor);
}

void addPersonMetricData(std::size_t start,
                         std::size_t end,
                         core_t::TTime time,
                         CDataGatherer &gatherer,
                         CResourceMonitor &resourceMonitor)
{
    for (std::size_t i = start; i < end; i++)
    {
        std::ostringstream ssA;
        ssA << "person" << i;
        addMetricArrival(time, ssA.str(), gatherer, resourceMonitor);
    }
}

}

void CResourceLimitTest::testLargeAllocations(void)
{
    {
        // Test CEventRateModel::createUpdateNewModels()
        const std::string EMPTY_STRING("");
        const core_t::TTime FIRST_TIME(358556400);
        const core_t::TTime BUCKET_LENGTH(3600);

        SModelParams params(BUCKET_LENGTH);
        params.s_DecayRate = 0.001;
        CEventRateModelFactory factory(params);
        factory.identifier(1);
        factory.fieldNames(EMPTY_STRING, EMPTY_STRING, "pers", EMPTY_STRING, TStrVec());
        CModelFactory::TFeatureVec features;
        features.push_back(model_t::E_IndividualCountByBucketAndPerson);
        factory.features(features);
        CModelFactory::SGathererInitializationData gathererInitData(FIRST_TIME);

        CModelFactory::TDataGathererPtr gatherer(dynamic_cast<CDataGatherer*>(
                                                 factory.makeDataGatherer(gathererInitData)));

        CResourceMonitor resourceMonitor;
        const maths::CMultinomialConjugate conjugate;
        ::CMockEventRateModel model(factory.modelParams(),
                                    gatherer,
                                    factory.defaultPriors(features),
                                    conjugate,
                                    factory.defaultDecompositions(features, BUCKET_LENGTH),
                                    CModel::TFeatureInfluenceCalculatorCPtrPrVecVec(),
                                    resourceMonitor);

        CPPUNIT_ASSERT_EQUAL(model_t::E_EventRateOnline, model.category());
        CPPUNIT_ASSERT(model.isPopulation() == false);
        core_t::TTime time = FIRST_TIME;

        CPPUNIT_ASSERT(resourceMonitor.areAllocationsAllowed());

        // Add some people & attributes to the gatherer
        // Run a sample
        // Check that the models can create the right number of people/attributes
        ::addPersonData(0, 400, time, *gatherer, resourceMonitor);

        CPPUNIT_ASSERT_EQUAL(std::size_t(400), gatherer->numberActivePeople());

        LOG_DEBUG("Testing for 1st time");
        model.test(time);
        CPPUNIT_ASSERT_EQUAL(std::size_t(400), gatherer->numberActivePeople());
        CPPUNIT_ASSERT_EQUAL(std::size_t(400), model.getNewPeople());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model.getNewAttributes());
        time += BUCKET_LENGTH;

        ::addPersonData(400, 1400, time, *gatherer, resourceMonitor);
        model.test(time);

        model.blowMemory();
        ::addPersonData(1400, 4400, time, *gatherer, resourceMonitor);

        LOG_DEBUG("Testing for 2nd time");
        model.test(time);
        CPPUNIT_ASSERT_EQUAL(std::size_t(1900), model.getNewPeople());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model.getNewAttributes());
        CPPUNIT_ASSERT_EQUAL(std::size_t(1900), gatherer->numberActivePeople());

        // Adding a small number of new people should be fine though,
        // as there are allowed in
        time += BUCKET_LENGTH;
        ::addPersonData(4400, 4899, time, *gatherer, resourceMonitor);

        LOG_DEBUG("Testing for 3rd time");
        model.test(time);
        CPPUNIT_ASSERT_EQUAL(std::size_t(2399), model.getNewPeople());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model.getNewAttributes());
        CPPUNIT_ASSERT_EQUAL(std::size_t(2399), gatherer->numberActivePeople());
    }
    {
        // Test CMetricModel::createUpdateNewModels()
        const std::string EMPTY_STRING("");
        const core_t::TTime FIRST_TIME(358556400);
        const core_t::TTime BUCKET_LENGTH(3600);

        SModelParams params(BUCKET_LENGTH);
        params.s_DecayRate = 0.001;
        CMetricModelFactory factory(params);
        factory.identifier(1);
        factory.fieldNames(EMPTY_STRING, EMPTY_STRING, "peep", "val", TStrVec());
        factory.useNull(true);
        CModelFactory::TFeatureVec features;
        features.push_back(model_t::E_IndividualMeanByPerson);
        features.push_back(model_t::E_IndividualMinByPerson);
        features.push_back(model_t::E_IndividualMaxByPerson);
        factory.features(features);
        factory.bucketLength(BUCKET_LENGTH);
        CModelFactory::SGathererInitializationData gathererInitData(FIRST_TIME);

        CModelFactory::TDataGathererPtr gatherer(factory.makeDataGatherer(gathererInitData));

        CResourceMonitor resourceMonitor;
        ::CMockMetricModel model(factory.modelParams(),
                                 gatherer,
                                 factory.defaultPriors(features),
                                 factory.defaultDecompositions(features, BUCKET_LENGTH),
                                 CModel::TFeatureInfluenceCalculatorCPtrPrVecVec(),
                                 resourceMonitor);

        CPPUNIT_ASSERT_EQUAL(model_t::E_MetricOnline, model.category());
        CPPUNIT_ASSERT(model.isPopulation() == false);
        core_t::TTime time = FIRST_TIME;

        CPPUNIT_ASSERT(resourceMonitor.areAllocationsAllowed());

        // Add some people & attributes to the gatherer
        // Run a sample
        // Check that the models can create the right number of people/attributes
        ::addPersonMetricData(0, 400, time, *gatherer, resourceMonitor);

        CPPUNIT_ASSERT_EQUAL(std::size_t(400), gatherer->numberActivePeople());

        LOG_DEBUG("Testing for 1st time");
        model.test(time);
        CPPUNIT_ASSERT_EQUAL(std::size_t(400), gatherer->numberActivePeople());
        CPPUNIT_ASSERT_EQUAL(std::size_t(400), model.getNewPeople());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model.getNewAttributes());
        time += BUCKET_LENGTH;

        ::addPersonMetricData(400, 1900, time, *gatherer, resourceMonitor);
        model.test(time);

        model.blowMemory();
        ::addPersonMetricData(1900, 4400, time, *gatherer, resourceMonitor);

        LOG_DEBUG("Testing for 2nd time");
        model.test(time);
        CPPUNIT_ASSERT_EQUAL(std::size_t(2400), model.getNewPeople());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model.getNewAttributes());
        CPPUNIT_ASSERT_EQUAL(std::size_t(2400), gatherer->numberActivePeople());

        // Adding a small number of new people should be fine though,
        // as they are are allowed in
        time += BUCKET_LENGTH;
        ::addPersonMetricData(4400, 4555, time, *gatherer, resourceMonitor);

        LOG_DEBUG("Testing for 3rd time");
        model.test(time);
        CPPUNIT_ASSERT_EQUAL(std::size_t(2555), model.getNewPeople());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), model.getNewAttributes());
        CPPUNIT_ASSERT_EQUAL(std::size_t(2555), gatherer->numberActivePeople());
    }
}

void CResourceLimitTest::importCsvDataWithLimiter(core_t::TTime firstTime,
                                                  core_t::TTime bucketLength,
                                                  CResultWriter &outputResults,
                                                  const std::string &fileName,
                                                  CAnomalyDetector &detector,
                                                  std::size_t limitCutoff,
                                                  CResourceMonitor &resourceMonitor)
{

    typedef boost::shared_ptr<std::ifstream> TifstreamPtr;
    TifstreamPtr ifs(new std::ifstream(fileName.c_str()));
    CPPUNIT_ASSERT(ifs->is_open());

    core::CRegex regex;
    CPPUNIT_ASSERT(regex.init(","));

    std::string line;
    // read the header
    CPPUNIT_ASSERT(std::getline(*ifs, line));

    core_t::TTime lastBucketTime = firstTime;

    std::size_t i = 0;
    while (std::getline(*ifs, line))
    {
        if (i == limitCutoff)
        {
            LOG_INFO("Setting Limit cuttoff now");
            resourceMonitor.m_ByteLimitHigh = 0;
            resourceMonitor.m_ByteLimitLow = 0;
        }

        LOG_TRACE("Got string: " << line);
        core::CRegex::TStrVec tokens;
        regex.split(line, tokens);

        core_t::TTime time;
        CPPUNIT_ASSERT(core::CStringUtils::stringToType(tokens[0], time));

        for (/**/;
             lastBucketTime + bucketLength <= time;
             lastBucketTime += bucketLength)
        {
            outputResults(detector,
                          lastBucketTime,
                          lastBucketTime + bucketLength);
        }

        CAnomalyDetector::TStrCPtrVec fieldValues;
        for (std::size_t t = tokens.size() - 1; t > 0; t--)
        {
            fieldValues.push_back(&tokens[t]);
        }

        detector.addRecord(time, fieldValues);
        ++i;
    }

    outputResults(detector,
                  lastBucketTime,
                  lastBucketTime + bucketLength);

    ifs.reset();
}
