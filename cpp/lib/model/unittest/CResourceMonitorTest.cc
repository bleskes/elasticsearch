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
#include "CResourceMonitorTest.h"


#include <model/CAnomalyDetector.h>
#include <model/CHierarchicalResults.h>
#include <model/CLimits.h>
#include <model/CMetricModelFactory.h>
#include <model/CModelConfig.h>
#include <model/CModelEnsemble.h>
#include <model/CResourceMonitor.h>

#include <string>

using namespace prelert;
using namespace model;


CppUnit::Test *CResourceMonitorTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CResourceMonitorTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CResourceMonitorTest>(
                                   "CResourceMonitorTest::testMonitor",
                                   &CResourceMonitorTest::testMonitor) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CResourceMonitorTest>(
                                   "CResourceMonitorTest::testPruning",
                                   &CResourceMonitorTest::testPruning) );
    return suiteOfTests;
}

void CResourceMonitorTest::testMonitor(void)
{
    SModelParams params(1);
    {
        // Test default constructor
        CResourceMonitor mon;
        CPPUNIT_ASSERT(mon.m_ByteLimitHigh > 0);
        CPPUNIT_ASSERT_EQUAL(mon.m_ByteLimitLow + 1024, mon.m_ByteLimitHigh);
        CPPUNIT_ASSERT(mon.m_ByteLimitHigh > mon.m_ByteLimitLow);
        CPPUNIT_ASSERT(mon.m_AllowAllocations);
        LOG_DEBUG("Resource limit is: " << mon.m_ByteLimitHigh);
        if (sizeof(std::size_t) == 4)
        {
            // 32-bit platform
            CPPUNIT_ASSERT_EQUAL(std::size_t(1024ull * 1024 * 1024), mon.m_ByteLimitHigh);
        }
        else if (sizeof(std::size_t) == 8)
        {
            // 64-bit platform
            CPPUNIT_ASSERT_EQUAL(std::size_t(4096ull * 1024 * 1024), mon.m_ByteLimitHigh);
        }
        else
        {
            // Unexpected platform
            CPPUNIT_ASSERT(false);
        }
    }
    {
        // Test size constructor
        CResourceMonitor mon(543);
        CPPUNIT_ASSERT_EQUAL(std::size_t(569376768), mon.m_ByteLimitHigh);
        CPPUNIT_ASSERT_EQUAL(std::size_t(569375744), mon.m_ByteLimitLow);
        CPPUNIT_ASSERT(mon.m_AllowAllocations);

        // Test memoryLimit
        mon.memoryLimit(987);
        CPPUNIT_ASSERT_EQUAL(std::size_t(1034944512ull), mon.m_ByteLimitHigh);
        CPPUNIT_ASSERT_EQUAL(std::size_t(1034943488ull), mon.m_ByteLimitLow);
    }
    {
        // Test adding and removing a ModelEnsemble
        CResourceMonitor mon;
        CModelConfig config(CModelConfig::defaultConfig());
        CModelEnsemble::TModelFactoryCPtr factory(new CMetricModelFactory(params));
        CModelEnsemble models(config, factory, 0);

        std::size_t mem = models.memoryUsage();
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), mon.m_Models.size());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), mon.m_Current);
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), mon.m_Previous);

        mon.registerComponent(models);
        CPPUNIT_ASSERT_EQUAL(std::size_t(1), mon.m_Models.size());

        mon.refresh(models, 0);
        CPPUNIT_ASSERT_EQUAL(mem, mon.m_Current);
        CPPUNIT_ASSERT_EQUAL(mem, mon.m_Previous);

        mon.unRegisterComponent(models);
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), mon.m_Models.size());
    }
    {
        // Check that High limit can be breached and then gone back
        CResourceMonitor mon;
        CModelConfig config(CModelConfig::defaultConfig());
        CModelEnsemble::TModelFactoryCPtr factory(new CMetricModelFactory(params));

        CModelEnsemble models(config, factory, 0);
        std::size_t mem = models.memoryUsage();
        CPPUNIT_ASSERT(mem > 5);        // This SHOULD be Ok


        // Let's go above the low but below the high limit
        mon.m_ByteLimitHigh = mem + 1;
        mon.m_ByteLimitLow = mem - 1;

        mon.registerComponent(models);
        mon.refresh(models, 0);
        CPPUNIT_ASSERT_EQUAL(mem, mon.m_Current);
        CPPUNIT_ASSERT_EQUAL(true, mon.areAllocationsAllowed());

        // Let's go above the high and low limit
        mon.m_ByteLimitHigh = mem - 1;
        mon.m_ByteLimitLow = mem - 2;

        mon.refresh(models, 0);
        CPPUNIT_ASSERT_EQUAL(mem, mon.m_Current);
        CPPUNIT_ASSERT_EQUAL(false, mon.areAllocationsAllowed());

        // Let's go above the low limit but above the high limit
        mon.m_ByteLimitHigh = mem + 1;
        mon.m_ByteLimitLow = mem - 1;

        mon.refresh(models, 0);
        CPPUNIT_ASSERT_EQUAL(mem, mon.m_Current);
        CPPUNIT_ASSERT_EQUAL(false, mon.areAllocationsAllowed());

        // Let's go below the high and low limit
        mon.m_ByteLimitHigh = mem + 2;
        mon.m_ByteLimitLow = mem + 1;

        mon.refresh(models, 0);
        CPPUNIT_ASSERT_EQUAL(mem, mon.m_Current);
        CPPUNIT_ASSERT_EQUAL(true, mon.areAllocationsAllowed());
    }
    {
        // Test the report callback
        CResourceMonitor mon;
        CModelConfig config(CModelConfig::defaultConfig());
        CModelEnsemble::TModelFactoryCPtr factory(new CMetricModelFactory(params));
        CModelEnsemble models(config, factory, 0);
        std::size_t mem = models.memoryUsage();
        mon.registerComponent(models);

        mon.memoryUsageReporter(
            boost::bind(&CResourceMonitorTest::reportCallback, this, _1));
        m_CallbackResults.s_Usage = 0;
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), m_CallbackResults.s_Usage);

        mon.refresh(models, 0);
        CPPUNIT_ASSERT_EQUAL(mem, m_CallbackResults.s_Usage);
        CPPUNIT_ASSERT_EQUAL(model_t::E_MemoryStatusOk, m_CallbackResults.s_MemoryStatus);
    }
    {
        // Test the report callback for allocation failures
        CResourceMonitor mon;
        CModelConfig config(CModelConfig::defaultConfig());
        CModelEnsemble::TModelFactoryCPtr factory(new CMetricModelFactory(params));
        CModelEnsemble models(config, factory, 0);
        std::size_t mem = models.memoryUsage();
        mon.registerComponent(models);

        mon.memoryUsageReporter(
            boost::bind(&CResourceMonitorTest::reportCallback, this, _1));
        m_CallbackResults.s_AllocationFailures = 0;
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), m_CallbackResults.s_AllocationFailures);

        // Set a soft-limit degraded status
        mon.acceptPruningResult();

        // This refresh should trigger a report
        mon.refresh(models, 0);
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), m_CallbackResults.s_AllocationFailures);
        CPPUNIT_ASSERT_EQUAL(mem, m_CallbackResults.s_Usage);
        CPPUNIT_ASSERT_EQUAL(model_t::E_MemoryStatusSoftLimit, m_CallbackResults.s_MemoryStatus);

        // Set some canary values
        m_CallbackResults.s_AllocationFailures = 12345;
        m_CallbackResults.s_ByFields = 54321;
        m_CallbackResults.s_OverFields = 23456;
        m_CallbackResults.s_PartitionFields = 65432;
        m_CallbackResults.s_Usage = 1357924;

        // This should not trigger a report
        mon.refresh(models, 0);
        CPPUNIT_ASSERT_EQUAL(std::size_t(12345), m_CallbackResults.s_AllocationFailures);
        CPPUNIT_ASSERT_EQUAL(std::size_t(54321), m_CallbackResults.s_ByFields);
        CPPUNIT_ASSERT_EQUAL(std::size_t(23456), m_CallbackResults.s_OverFields);
        CPPUNIT_ASSERT_EQUAL(std::size_t(65432), m_CallbackResults.s_PartitionFields);
        CPPUNIT_ASSERT_EQUAL(std::size_t(1357924), m_CallbackResults.s_Usage);

        // Add some memory failures, which should be reported
        mon.acceptAllocationFailureResult(14400000);
        mon.acceptAllocationFailureResult(14400000);
        mon.acceptAllocationFailureResult(14401000);
        mon.acceptAllocationFailureResult(14402000);

        CPPUNIT_ASSERT_EQUAL(std::size_t(3), mon.m_AllocationFailures.size());
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(0), mon.m_LastAllocationFailureReport);
        mon.refresh(models, 0);
        CPPUNIT_ASSERT_EQUAL(std::size_t(3), m_CallbackResults.s_AllocationFailures);
        CPPUNIT_ASSERT_EQUAL(mem, m_CallbackResults.s_Usage);
        CPPUNIT_ASSERT_EQUAL(core_t::TTime(14402000), mon.m_LastAllocationFailureReport);
        CPPUNIT_ASSERT_EQUAL(model_t::E_MemoryStatusHardLimit, m_CallbackResults.s_MemoryStatus);

        // Set some canary values
        m_CallbackResults.s_AllocationFailures = 12345;
        m_CallbackResults.s_ByFields = 54321;
        m_CallbackResults.s_OverFields = 23456;
        m_CallbackResults.s_PartitionFields = 65432;
        m_CallbackResults.s_Usage = 1357924;

        // As nothing has changed, nothing should be reported
        mon.refresh(models, 0);
        CPPUNIT_ASSERT_EQUAL(std::size_t(12345), m_CallbackResults.s_AllocationFailures);
        CPPUNIT_ASSERT_EQUAL(std::size_t(54321), m_CallbackResults.s_ByFields);
        CPPUNIT_ASSERT_EQUAL(std::size_t(23456), m_CallbackResults.s_OverFields);
        CPPUNIT_ASSERT_EQUAL(std::size_t(65432), m_CallbackResults.s_PartitionFields);
        CPPUNIT_ASSERT_EQUAL(std::size_t(1357924), m_CallbackResults.s_Usage);

    }
    {
        // Test the need to report usage based on a change in levels, up and down
        CResourceMonitor mon;
        mon.memoryUsageReporter(
            boost::bind(&CResourceMonitorTest::reportCallback, this, _1));
        CPPUNIT_ASSERT(!mon.needToSendReport());

        // Go up to 10 bytes, triggering a need
        mon.m_Current = 10;
        CPPUNIT_ASSERT(mon.needToSendReport());
        mon.sendMemoryUsageReport(0);
        CPPUNIT_ASSERT_EQUAL(std::size_t(10), m_CallbackResults.s_Usage);

        // Nothing new added, so no report
        CPPUNIT_ASSERT(!mon.needToSendReport());

        // 10% increase should trigger a need
        mon.m_Current = 11;
        CPPUNIT_ASSERT(mon.needToSendReport());
        mon.sendMemoryUsageReport(0);
        CPPUNIT_ASSERT_EQUAL(std::size_t(11), m_CallbackResults.s_Usage);

        // Huge increase should trigger a need
        mon.m_Current = 1000;
        CPPUNIT_ASSERT(mon.needToSendReport());
        mon.sendMemoryUsageReport(0);
        CPPUNIT_ASSERT_EQUAL(std::size_t(1000), m_CallbackResults.s_Usage);

        // 0.1% increase should not trigger a need
        mon.m_Current = 1001;
        CPPUNIT_ASSERT(!mon.needToSendReport());

        // A decrease should trigger a need
        mon.m_Current = 989;
        CPPUNIT_ASSERT(mon.needToSendReport());
        mon.sendMemoryUsageReport(0);
        CPPUNIT_ASSERT_EQUAL(std::size_t(989), m_CallbackResults.s_Usage);

        // A tiny decrease should not trigger a need
        mon.m_Current = 988;
        CPPUNIT_ASSERT(!mon.needToSendReport());
    }
}

void CResourceMonitorTest::testPruning(void)
{
    const std::string EMPTY_STRING;
    const core_t::TTime FIRST_TIME(358556400);
    const core_t::TTime BUCKET_LENGTH(3600);

    model::CModelConfig modelConfig = model::CModelConfig::defaultConfig(BUCKET_LENGTH);
    model::CLimits limits;

    CSearchKey key(1, // identifier
                   function_t::E_IndividualMetric,
                   false,
                   model_t::E_XF_None,
                   "value", "colour");

    CResourceMonitor &monitor = limits.resourceMonitor();
    monitor.memoryLimit(70);

    CAnomalyDetector detector(1, // identifier
                              limits,
                              modelConfig,
                              EMPTY_STRING,
                              FIRST_TIME,
                              modelConfig.factory(key));

    core_t::TTime bucket = FIRST_TIME;
    std::size_t startOffset = 10;

    // Add a couple of buckets of data
    this->addTestData(bucket, BUCKET_LENGTH, 2, 5, startOffset, detector, monitor);
    CPPUNIT_ASSERT_EQUAL(false, monitor.pruneIfRequired(bucket));
    CPPUNIT_ASSERT_EQUAL(false, monitor.m_HasPruningStarted);
    CPPUNIT_ASSERT_EQUAL(model_t::E_MemoryStatusOk, monitor.m_MemoryStatus);

    LOG_DEBUG("Saturating the pruner");
    // Add enough data to saturate the pruner
    this->addTestData(bucket, BUCKET_LENGTH, 1100, 5, startOffset, detector, monitor);

    LOG_DEBUG(monitor.m_PruneWindow);
    CPPUNIT_ASSERT_EQUAL(true, monitor.m_HasPruningStarted);
    CPPUNIT_ASSERT(monitor.m_PruneWindow < std::size_t(1000));
    CPPUNIT_ASSERT_EQUAL(model_t::E_MemoryStatusSoftLimit, monitor.m_MemoryStatus);
    CPPUNIT_ASSERT_EQUAL(true, monitor.m_AllowAllocations);

    LOG_DEBUG("Allowing pruner to relax");
    // Add no new people and see that the window relaxes away from the minimum window
    this->addTestData(bucket, BUCKET_LENGTH, 100, 0, startOffset, detector, monitor);
    LOG_DEBUG("Window is now: " << monitor.m_PruneWindow);
    std::size_t level = monitor.m_PruneWindow;
    CPPUNIT_ASSERT_EQUAL(model_t::E_MemoryStatusSoftLimit, monitor.m_MemoryStatus);
    CPPUNIT_ASSERT_EQUAL(true, monitor.m_AllowAllocations);
    CPPUNIT_ASSERT(monitor.m_Current < monitor.m_PruneThreshold);

    LOG_DEBUG("Testing fine-grained control");
    // Check that the window keeps growing now
    this->addTestData(bucket, BUCKET_LENGTH, 50, 0, startOffset, detector, monitor);
    CPPUNIT_ASSERT(monitor.m_PruneWindow > level);
    level = monitor.m_PruneWindow;
    this->addTestData(bucket, BUCKET_LENGTH, 50, 0, startOffset, detector, monitor);
    CPPUNIT_ASSERT(monitor.m_PruneWindow > level);
    level = monitor.m_PruneWindow;

    // Check that adding a bunch of new data shrinks the window again
    this->addTestData(bucket, BUCKET_LENGTH, 200, 10, startOffset, detector, monitor);
    CPPUNIT_ASSERT(monitor.m_PruneWindow < level);
    level = monitor.m_PruneWindow;

    // And finally that it grows again
    this->addTestData(bucket, BUCKET_LENGTH, 700, 0, startOffset, detector, monitor);
    CPPUNIT_ASSERT(monitor.m_PruneWindow > level);
}

void CResourceMonitorTest::addTestData(core_t::TTime &firstTime,
                                       const core_t::TTime bucketLength,
                                       const std::size_t buckets,
                                       const std::size_t newPeoplePerBucket,
                                       std::size_t &startOffset,
                                       CAnomalyDetector &detector,
                                       CResourceMonitor &monitor)
{
    std::string numberValue("100");
    core_t::TTime bucketStart = firstTime;
    CHierarchicalResults results;
    std::string pervasive("IShouldNotBeRemoved");

    std::size_t numBuckets = 0;

    for (core_t::TTime time = firstTime;
         time < static_cast<core_t::TTime>(firstTime + bucketLength * buckets);
         time += (bucketLength / std::max(std::size_t(1), newPeoplePerBucket)))
    {
        bool newBucket = false;
        for (; bucketStart + bucketLength <= time; bucketStart += bucketLength)
        {
            detector.buildResults(bucketStart, bucketStart + bucketLength, results);
            monitor.pruneIfRequired(bucketStart);
            numBuckets++;
            newBucket = true;
        }

        if (newBucket)
        {
            CAnomalyDetector::TStrCPtrVec fieldValues;
            fieldValues.push_back(&pervasive);
            fieldValues.push_back(&numberValue);
            detector.addRecord(time, fieldValues);
        }

        if (newPeoplePerBucket > 0)
        {
            CAnomalyDetector::TStrCPtrVec fieldValues;
            std::ostringstream ss1;
            ss1 << "person" << startOffset++;
            std::string value(ss1.str());

            fieldValues.push_back(&value);
            fieldValues.push_back(&numberValue);
            detector.addRecord(time, fieldValues);
        }
    }
    firstTime = bucketStart;
}

void CResourceMonitorTest::reportCallback(const CResourceMonitor::SResults &results)
{
    m_CallbackResults = results;
}

