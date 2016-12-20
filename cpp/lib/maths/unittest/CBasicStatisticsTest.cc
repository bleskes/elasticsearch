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
#include "CBasicStatisticsTest.h"

#include <core/CLogger.h>
#include <core/CRapidXmlParser.h>
#include <core/CRapidXmlStatePersistInserter.h>
#include <core/CRapidXmlStateRestoreTraverser.h>

#include <maths/CBasicStatistics.h>
#include <maths/CLinearAlgebra.h>
#include <maths/CSampling.h>

#include <test/CRandomNumbers.h>

#include <boost/range.hpp>

#include <stdlib.h>


CppUnit::Test *CBasicStatisticsTest::suite(void)
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CBasicStatisticsTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CBasicStatisticsTest>(
                                   "CBasicStatisticsTest::testMean",
                                   &CBasicStatisticsTest::testMean) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CBasicStatisticsTest>(
                                   "CBasicStatisticsTest::testVmr",
                                   &CBasicStatisticsTest::testVmr) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CBasicStatisticsTest>(
                                   "CBasicStatisticsTest::testCentralMoments",
                                   &CBasicStatisticsTest::testCentralMoments) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CBasicStatisticsTest>(
                                   "CBasicStatisticsTest::testCovariances",
                                   &CBasicStatisticsTest::testCovariances) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CBasicStatisticsTest>(
                                   "CBasicStatisticsTest::testCovariancesLedoitWolf",
                                   &CBasicStatisticsTest::testCovariancesLedoitWolf) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CBasicStatisticsTest>(
                                   "CBasicStatisticsTest::testSd",
                                   &CBasicStatisticsTest::testSd) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CBasicStatisticsTest>(
                                   "CBasicStatisticsTest::testMedian",
                                   &CBasicStatisticsTest::testMedian) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CBasicStatisticsTest>(
                                   "CBasicStatisticsTest::testQuartiles",
                                   &CBasicStatisticsTest::testQuartiles) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CBasicStatisticsTest>(
                                   "CBasicStatisticsTest::testQuartileRelationship",
                                   &CBasicStatisticsTest::testQuartileRelationship) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CBasicStatisticsTest>(
                                   "CBasicStatisticsTest::testOrderStatistics",
                                   &CBasicStatisticsTest::testOrderStatistics) );

    return suiteOfTests;
}

void CBasicStatisticsTest::testMean(void)
{
    LOG_DEBUG("+---------------------------------+");
    LOG_DEBUG("|  CBasicStatisticsTest::testMean |");
    LOG_DEBUG("+---------------------------------+");

    double sample[] = { 0.9, 10.0, 5.6, 1.23, -12.3, 445.2, 0.0, 1.2 };

    prelert::maths::CBasicStatistics::TDoubleVec sampleVec(sample, sample+sizeof(sample)/sizeof(sample[0]));

    double mean = prelert::maths::CBasicStatistics::mean(sampleVec);

    // Compare with hand calculated value
    CPPUNIT_ASSERT_EQUAL(56.47875, mean);
}

void CBasicStatisticsTest::testVmr(void)
{
    LOG_DEBUG("+---------------------------------+");
    LOG_DEBUG("|  CBasicStatisticsTest::testVmr  |");
    LOG_DEBUG("+---------------------------------+");

    double sample[] = { 0.9, 10.0, 5.6, 1.23, -12.3, 445.2, 0.0, 1.2 };

    prelert::maths::CBasicStatistics::TDoubleVec sampleVec(sample, sample+sizeof(sample)/sizeof(sample[0]));

    double mean(0.0);
    double vmr(0.0);

    CPPUNIT_ASSERT(prelert::maths::CBasicStatistics::sampleVarianceToMeanRatio(sampleVec, mean, vmr));

    // Compare with Excel calculated values (Excel values are correct to 5 decimal places)
    CPPUNIT_ASSERT_DOUBLES_EQUAL(56.47875, mean, 0.00001);
    CPPUNIT_ASSERT_DOUBLES_EQUAL(437.51135, vmr, 0.00001);
}

void CBasicStatisticsTest::testSd(void)
{
    LOG_DEBUG("+--------------------------------+");
    LOG_DEBUG("|  CBasicStatisticsTest::testSd  |");
    LOG_DEBUG("+--------------------------------+");

    double sample[] = { 0.9, 10.0, 5.6, 1.23, -12.3, 445.2, 0.0, 1.2 };

    prelert::maths::CBasicStatistics::TDoubleVec sampleVec(sample, sample+sizeof(sample)/sizeof(sample[0]));

    double mean = prelert::maths::CBasicStatistics::mean(sampleVec);
    double sd(0.0);

    CPPUNIT_ASSERT(prelert::maths::CBasicStatistics::sampleStandardDeviation(sampleVec, sd));

    // Compare with Excel calculated values (Excel values are correct to 5 decimal places)
    CPPUNIT_ASSERT_DOUBLES_EQUAL(56.47875, mean, 0.00001);
    CPPUNIT_ASSERT_DOUBLES_EQUAL(157.19445, sd, 0.00001);
}

typedef prelert::maths::CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;
typedef prelert::maths::CBasicStatistics::SSampleMeanVar<double>::TAccumulator TMeanVarAccumulator;
typedef prelert::maths::CBasicStatistics::SSampleMeanVarSkew<double>::TAccumulator TMeanVarSkewAccumulator;
typedef std::vector<TMeanAccumulator> TMeanAccumulatorVec;
typedef std::vector<TMeanVarAccumulator> TMeanVarAccumulatorVec;
typedef std::vector<TMeanVarSkewAccumulator> TMeanVarSkewAccumulatorVec;

bool restoreMeans(TMeanAccumulatorVec *restored,
                  std::string tag,
                  prelert::core::CStateRestoreTraverser &traverser)
{
    do
    {
        if (traverser.name() == tag)
        {
            if (prelert::maths::CBasicStatistics::restoreSampleCentralMoments(traverser, *restored) == false)
            {
                return false;
            }
        }
    }
    while (traverser.next());

    return true;
}

bool restoreMeanVars(TMeanVarAccumulatorVec *restored,
                     std::string tag,
                     prelert::core::CStateRestoreTraverser &traverser)
{
    do
    {
        if (traverser.name() == tag)
        {
            if (prelert::maths::CBasicStatistics::restoreSampleCentralMoments(traverser, *restored) == false)
            {
                return false;
            }
        }
    }
    while (traverser.next());

    return true;
}

bool restoreMeanVarSkews(TMeanVarSkewAccumulatorVec *restored,
                         std::string tag,
                         prelert::core::CStateRestoreTraverser &traverser)
{
    do
    {
        if (traverser.name() == tag)
        {
            if (prelert::maths::CBasicStatistics::restoreSampleCentralMoments(traverser, *restored) == false)
            {
                return false;
            }
        }
    }
    while (traverser.next());

    return true;
}

void CBasicStatisticsTest::testCentralMoments(void)
{
    LOG_DEBUG("+--------------------------------------------+");
    LOG_DEBUG("|  CBasicStatisticsTest::testCentralMoments  |");
    LOG_DEBUG("+--------------------------------------------+");

    typedef std::vector<double> TDoubleVec;

    LOG_DEBUG("Test mean double");
    {
        double samples[] = { 0.9, 10.0, 5.6, 1.23, -12.3, 7.2, 0.0, 1.2 };
        TMeanAccumulator acc;

        size_t count = sizeof(samples)/sizeof(samples[0]);
        acc = std::for_each(samples, samples + count, acc);

        CPPUNIT_ASSERT_EQUAL(count,
                             static_cast<size_t>(prelert::maths::CBasicStatistics::count(acc)));

        CPPUNIT_ASSERT_DOUBLES_EQUAL(1.72875,
                                     prelert::maths::CBasicStatistics::mean(acc),
                                     0.000005);
    }

    LOG_DEBUG("Test mean float");
    {
        typedef prelert::maths::CBasicStatistics::SSampleMean<float>::TAccumulator TFloatMeanAccumulator;

        float samples[] = { 0.9f, 10.0f, 5.6f, 1.23f, -12.3f, 7.2f, 0.0f, 1.2f };

        TFloatMeanAccumulator acc;

        size_t count = sizeof(samples)/sizeof(samples[0]);
        acc = std::for_each(samples, samples + count, acc);

        CPPUNIT_ASSERT_EQUAL(count,
                             static_cast<size_t>(prelert::maths::CBasicStatistics::count(acc)));

        CPPUNIT_ASSERT_DOUBLES_EQUAL(1.72875,
                                     prelert::maths::CBasicStatistics::mean(acc),
                                     0.000005);
    }

    LOG_DEBUG("Test mean and variance");
    {
        double samples[] = { 0.9, 10.0, 5.6, 1.23, -12.3, 7.2, 0.0, 1.2 };

        TMeanVarAccumulator acc;

        size_t count = sizeof(samples)/sizeof(samples[0]);
        acc = std::for_each(samples, samples + count, acc);

        CPPUNIT_ASSERT_EQUAL(count,
                             static_cast<size_t>(prelert::maths::CBasicStatistics::count(acc)));

        CPPUNIT_ASSERT_DOUBLES_EQUAL(1.72875,
                                     prelert::maths::CBasicStatistics::mean(acc),
                                     0.000005);

        CPPUNIT_ASSERT_DOUBLES_EQUAL(44.90633,
                                     prelert::maths::CBasicStatistics::variance(acc),
                                     0.000005);
    }

    LOG_DEBUG("Test mean, variance and skew");
    {
        double samples[] = { 0.9, 10.0, 5.6, 1.23, -12.3, 7.2, 0.0, 1.2 };

        TMeanVarSkewAccumulator acc;

        size_t count = sizeof(samples)/sizeof(samples[0]);
        acc = std::for_each(samples, samples + count, acc);

        CPPUNIT_ASSERT_EQUAL(count,
                             static_cast<size_t>(prelert::maths::CBasicStatistics::count(acc)));

        CPPUNIT_ASSERT_DOUBLES_EQUAL(1.72875,
                                     prelert::maths::CBasicStatistics::mean(acc),
                                     0.000005);

        CPPUNIT_ASSERT_DOUBLES_EQUAL(44.90633,
                                     prelert::maths::CBasicStatistics::variance(acc),
                                     0.000005);

        CPPUNIT_ASSERT_DOUBLES_EQUAL(-0.82216,
                                     prelert::maths::CBasicStatistics::skewness(acc),
                                     0.000005);
    }

    LOG_DEBUG("Test weighted update");
    {
        double samples[] = { 0.9, 1.0, 2.3, 1.5 };
        std::size_t weights[] = { 1, 4, 2, 3 };

        {
            TMeanAccumulator acc1;
            TMeanAccumulator acc2;

            for (size_t i = 0; i < boost::size(samples); ++i)
            {
                acc1.add(samples[i], static_cast<double>(weights[i]));
                for (std::size_t j = 0u; j < weights[i]; ++j)
                {
                    acc2.add(samples[i]);
                }
            }

            CPPUNIT_ASSERT_DOUBLES_EQUAL(prelert::maths::CBasicStatistics::mean(acc1),
                                         prelert::maths::CBasicStatistics::mean(acc2),
                                         1e-10);
        }

        {
            TMeanVarAccumulator acc1;
            TMeanVarAccumulator acc2;

            for (size_t i = 0; i < boost::size(samples); ++i)
            {
                acc1.add(samples[i], static_cast<double>(weights[i]));
                for (std::size_t j = 0u; j < weights[i]; ++j)
                {
                    acc2.add(samples[i]);
                }
            }

            CPPUNIT_ASSERT_DOUBLES_EQUAL(prelert::maths::CBasicStatistics::mean(acc1),
                                         prelert::maths::CBasicStatistics::mean(acc2),
                                         1e-10);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(prelert::maths::CBasicStatistics::variance(acc1),
                                         prelert::maths::CBasicStatistics::variance(acc2),
                                         1e-10);
        }

        {
            TMeanVarSkewAccumulator acc1;
            TMeanVarSkewAccumulator acc2;

            for (size_t i = 0; i < boost::size(samples); ++i)
            {
                acc1.add(samples[i], static_cast<double>(weights[i]));
                for (std::size_t j = 0u; j < weights[i]; ++j)
                {
                    acc2.add(samples[i]);
                }
            }

            CPPUNIT_ASSERT_DOUBLES_EQUAL(prelert::maths::CBasicStatistics::mean(acc1),
                                         prelert::maths::CBasicStatistics::mean(acc2),
                                         1e-10);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(prelert::maths::CBasicStatistics::variance(acc1),
                                         prelert::maths::CBasicStatistics::variance(acc2),
                                         1e-10);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(prelert::maths::CBasicStatistics::skewness(acc1),
                                         prelert::maths::CBasicStatistics::skewness(acc2),
                                         1e-10);
        }
    }

    LOG_DEBUG("Test addition");
    {
        // Test addition.
        double samples1[] = { 0.9, 10.0, 5.6, 1.23 };
        double samples2[] = { -12.3, 7.2, 0.0, 1.2 };

        size_t count1 = sizeof(samples1)/sizeof(samples1[0]);
        size_t count2 = sizeof(samples2)/sizeof(samples2[0]);

        {
            TMeanAccumulator acc1;
            TMeanAccumulator acc2;

            acc1 = std::for_each(samples1, samples1 + count1, acc1);
            acc2 = std::for_each(samples2, samples2 + count2, acc2);

            CPPUNIT_ASSERT_EQUAL(count1 + count2,
                                 static_cast<size_t>(prelert::maths::CBasicStatistics::count(acc1 + acc2)));

            CPPUNIT_ASSERT_DOUBLES_EQUAL(1.72875,
                                         prelert::maths::CBasicStatistics::mean(acc1 + acc2),
                                         0.000005);
        }

        {
            TMeanVarAccumulator acc1;
            TMeanVarAccumulator acc2;

            acc1 = std::for_each(samples1, samples1 + count1, acc1);
            acc2 = std::for_each(samples2, samples2 + count2, acc2);

            CPPUNIT_ASSERT_EQUAL(count1 + count2,
                                 static_cast<size_t>(prelert::maths::CBasicStatistics::count(acc1 + acc2)));

            CPPUNIT_ASSERT_DOUBLES_EQUAL(1.72875,
                                         prelert::maths::CBasicStatistics::mean(acc1 + acc2),
                                         0.000005);

            CPPUNIT_ASSERT_DOUBLES_EQUAL(44.90633,
                                         prelert::maths::CBasicStatistics::variance(acc1 + acc2),
                                         0.000005);
        }

        {
            TMeanVarSkewAccumulator acc1;
            TMeanVarSkewAccumulator acc2;

            acc1 = std::for_each(samples1, samples1 + count1, acc1);
            acc2 = std::for_each(samples2, samples2 + count2, acc2);

            CPPUNIT_ASSERT_EQUAL(count1 + count2,
                                 static_cast<size_t>(prelert::maths::CBasicStatistics::count(acc1 + acc2)));

            CPPUNIT_ASSERT_DOUBLES_EQUAL(1.72875,
                                         prelert::maths::CBasicStatistics::mean(acc1 + acc2),
                                         0.000005);

            CPPUNIT_ASSERT_DOUBLES_EQUAL(44.90633,
                                         prelert::maths::CBasicStatistics::variance(acc1 + acc2),
                                         0.000005);

            CPPUNIT_ASSERT_DOUBLES_EQUAL(-0.82216,
                                         prelert::maths::CBasicStatistics::skewness(acc1 + acc2),\
                                         0.000005);
        }
    }

    LOG_DEBUG("Test subtraction");
    {
        prelert::test::CRandomNumbers rng;

        LOG_DEBUG("Test mean");
        {
            TMeanAccumulator acc1;
            TMeanAccumulator acc2;

            TDoubleVec samples;
            rng.generateNormalSamples(2.0, 3.0, 40u, samples);

            for (std::size_t j = 1u; j < samples.size(); ++j)
            {
                LOG_DEBUG("split = " << j << "/" << samples.size() - j);

                for (std::size_t i = 0u; i < j; ++i)
                {
                    acc1.add(samples[i]);
                }
                for (std::size_t i = j; i < samples.size(); ++i)
                {
                    acc2.add(samples[i]);
                }

                TMeanAccumulator sum = acc1 + acc2;

                CPPUNIT_ASSERT_EQUAL(prelert::maths::CBasicStatistics::count(acc1),
                                     prelert::maths::CBasicStatistics::count(sum - acc2));
                CPPUNIT_ASSERT_DOUBLES_EQUAL(prelert::maths::CBasicStatistics::mean(acc1),
                                             prelert::maths::CBasicStatistics::mean(sum - acc2),
                                             1e-10);
                CPPUNIT_ASSERT_EQUAL(prelert::maths::CBasicStatistics::count(acc2),
                                     prelert::maths::CBasicStatistics::count(sum - acc1));
                CPPUNIT_ASSERT_DOUBLES_EQUAL(prelert::maths::CBasicStatistics::mean(acc2),
                                             prelert::maths::CBasicStatistics::mean(sum - acc1),
                                             1e-10);
            }
        }
        LOG_DEBUG("Test mean and variance");
        {
            TMeanVarAccumulator acc1;
            TMeanVarAccumulator acc2;

            TDoubleVec samples;
            rng.generateGammaSamples(3.0, 3.0, 40u, samples);

            for (std::size_t j = 1u; j < samples.size(); ++j)
            {
                LOG_DEBUG("split = " << j << "/" << samples.size() - j);

                for (std::size_t i = 0u; i < j; ++i)
                {
                    acc1.add(samples[i]);
                }
                for (std::size_t i = j; i < samples.size(); ++i)
                {
                    acc2.add(samples[i]);
                }

                TMeanVarAccumulator sum = acc1 + acc2;

                CPPUNIT_ASSERT_EQUAL(prelert::maths::CBasicStatistics::count(acc1),
                                     prelert::maths::CBasicStatistics::count(sum - acc2));
                CPPUNIT_ASSERT_DOUBLES_EQUAL(prelert::maths::CBasicStatistics::mean(acc1),
                                             prelert::maths::CBasicStatistics::mean(sum - acc2),
                                             1e-10);
                CPPUNIT_ASSERT_DOUBLES_EQUAL(prelert::maths::CBasicStatistics::variance(acc1),
                                             prelert::maths::CBasicStatistics::variance(sum - acc2),
                                             1e-10);
                CPPUNIT_ASSERT_EQUAL(prelert::maths::CBasicStatistics::count(acc2),
                                     prelert::maths::CBasicStatistics::count(sum - acc1));
                CPPUNIT_ASSERT_DOUBLES_EQUAL(prelert::maths::CBasicStatistics::mean(acc2),
                                             prelert::maths::CBasicStatistics::mean(sum - acc1),
                                             1e-10);
                CPPUNIT_ASSERT_DOUBLES_EQUAL(prelert::maths::CBasicStatistics::variance(acc2),
                                             prelert::maths::CBasicStatistics::variance(sum - acc1),
                                             1e-10);
            }
        }
        LOG_DEBUG("Test mean, variance and skew");
        {
            TMeanVarSkewAccumulator acc1;
            TMeanVarSkewAccumulator acc2;

            TDoubleVec samples;
            rng.generateLogNormalSamples(1.1, 1.0, 40u, samples);

            for (std::size_t j = 1u; j < samples.size(); ++j)
            {
                LOG_DEBUG("split = " << j << "/" << samples.size() - j);

                for (std::size_t i = 0u; i < j; ++i)
                {
                    acc1.add(samples[i]);
                }
                for (std::size_t i = j; i < samples.size(); ++i)
                {
                    acc2.add(samples[i]);
                }

                TMeanVarSkewAccumulator sum = acc1 + acc2;

                CPPUNIT_ASSERT_EQUAL(prelert::maths::CBasicStatistics::count(acc1),
                                     prelert::maths::CBasicStatistics::count(sum - acc2));
                CPPUNIT_ASSERT_DOUBLES_EQUAL(prelert::maths::CBasicStatistics::mean(acc1),
                                             prelert::maths::CBasicStatistics::mean(sum - acc2),
                                             1e-10);
                CPPUNIT_ASSERT_DOUBLES_EQUAL(prelert::maths::CBasicStatistics::variance(acc1),
                                             prelert::maths::CBasicStatistics::variance(sum - acc2),
                                             1e-10);
                CPPUNIT_ASSERT_DOUBLES_EQUAL(prelert::maths::CBasicStatistics::skewness(acc1),
                                             prelert::maths::CBasicStatistics::skewness(sum - acc2),
                                             1e-10);
                CPPUNIT_ASSERT_EQUAL(prelert::maths::CBasicStatistics::count(acc2),
                                     prelert::maths::CBasicStatistics::count(sum - acc1));
                CPPUNIT_ASSERT_DOUBLES_EQUAL(prelert::maths::CBasicStatistics::mean(acc2),
                                             prelert::maths::CBasicStatistics::mean(sum - acc1),
                                             1e-10);
                CPPUNIT_ASSERT_DOUBLES_EQUAL(prelert::maths::CBasicStatistics::variance(acc2),
                                             prelert::maths::CBasicStatistics::variance(sum - acc1),
                                             1e-10);
                CPPUNIT_ASSERT_DOUBLES_EQUAL(prelert::maths::CBasicStatistics::skewness(acc2),
                                             prelert::maths::CBasicStatistics::skewness(sum - acc1),
                                             1e-10);
            }
        }
    }

    LOG_DEBUG("test vector")
    {
        typedef prelert::maths::CBasicStatistics::SSampleMean<prelert::maths::CVectorNx1<double, 4> >::TAccumulator TVectorMeanAccumulator;
        typedef prelert::maths::CBasicStatistics::SSampleMeanVar<prelert::maths::CVectorNx1<double, 4> >::TAccumulator TVectorMeanVarAccumulator;
        typedef prelert::maths::CBasicStatistics::SSampleMeanVarSkew<prelert::maths::CVectorNx1<double, 4> >::TAccumulator TVectorMeanVarSkewAccumulator;

        prelert::test::CRandomNumbers rng;

        {
            LOG_DEBUG("Test mean");

            TDoubleVec samples;
            rng.generateNormalSamples(5.0, 1.0, 120, samples);

            TMeanAccumulator means[4];
            TVectorMeanAccumulator vectorMean;

            for (std::size_t i = 0u; i < samples.size(); ++i)
            {
                prelert::maths::CVectorNx1<double, 4> v;
                for (std::size_t j = 0u; j < 4; ++i, ++j)
                {
                    means[j].add(samples[i]);
                    v(j) = samples[i];
                }
                LOG_DEBUG("v = " << v);
                vectorMean.add(v);

                CPPUNIT_ASSERT_EQUAL(prelert::maths::CBasicStatistics::count(means[0]),
                                     prelert::maths::CBasicStatistics::count(vectorMean));
                for (std::size_t j = 0u; j < 4; ++j)
                {
                    CPPUNIT_ASSERT_EQUAL(prelert::maths::CBasicStatistics::mean(means[j]),
                                         (prelert::maths::CBasicStatistics::mean(vectorMean))(j));
                }
            }
        }
        {
            LOG_DEBUG("Test mean and variance");

            TDoubleVec samples;
            rng.generateNormalSamples(5.0, 1.0, 120, samples);

            TMeanVarAccumulator meansAndVariances[4];
            TVectorMeanVarAccumulator vectorMeanAndVariances;

            for (std::size_t i = 0u; i < samples.size(); ++i)
            {
                prelert::maths::CVectorNx1<double, 4> v;
                for (std::size_t j = 0u; j < 4; ++i, ++j)
                {
                    meansAndVariances[j].add(samples[i]);
                    v(j) = samples[i];
                }
                LOG_DEBUG("v = " << v);
                vectorMeanAndVariances.add(v);

                CPPUNIT_ASSERT_EQUAL(prelert::maths::CBasicStatistics::count(meansAndVariances[0]),
                                     prelert::maths::CBasicStatistics::count(vectorMeanAndVariances));
                for (std::size_t j = 0u; j < 4; ++j)
                {
                    CPPUNIT_ASSERT_EQUAL(prelert::maths::CBasicStatistics::mean(meansAndVariances[j]),
                                         (prelert::maths::CBasicStatistics::mean(vectorMeanAndVariances))(j));
                    CPPUNIT_ASSERT_EQUAL(prelert::maths::CBasicStatistics::variance(meansAndVariances[j]),
                                         (prelert::maths::CBasicStatistics::variance(vectorMeanAndVariances))(j));
                }
            }
        }
        {
            LOG_DEBUG("Test mean, variance and skew");

            TDoubleVec samples;
            rng.generateNormalSamples(5.0, 1.0, 120, samples);

            TMeanVarSkewAccumulator meansVariancesAndSkews[4];
            TVectorMeanVarSkewAccumulator vectorMeanVarianceAndSkew;

            for (std::size_t i = 0u; i < samples.size(); ++i)
            {
                prelert::maths::CVectorNx1<double, 4> v;
                for (std::size_t j = 0u; j < 4; ++i, ++j)
                {
                    meansVariancesAndSkews[j].add(samples[i]);
                    v(j) = samples[i];
                }
                LOG_DEBUG("v = " << v);
                vectorMeanVarianceAndSkew.add(v);

                CPPUNIT_ASSERT_EQUAL(prelert::maths::CBasicStatistics::count(meansVariancesAndSkews[0]),
                                     prelert::maths::CBasicStatistics::count(vectorMeanVarianceAndSkew));
                for (std::size_t j = 0u; j < 4; ++j)
                {
                    CPPUNIT_ASSERT_EQUAL(prelert::maths::CBasicStatistics::mean(meansVariancesAndSkews[j]),
                                         (prelert::maths::CBasicStatistics::mean(vectorMeanVarianceAndSkew))(j));
                    CPPUNIT_ASSERT_EQUAL(prelert::maths::CBasicStatistics::variance(meansVariancesAndSkews[j]),
                                         (prelert::maths::CBasicStatistics::variance(vectorMeanVarianceAndSkew))(j));
                    CPPUNIT_ASSERT_EQUAL(prelert::maths::CBasicStatistics::skewness(meansVariancesAndSkews[j]),
                                         (prelert::maths::CBasicStatistics::skewness(vectorMeanVarianceAndSkew))(j));
                }
            }
        }
    }

    LOG_DEBUG("Test persistence of collections");
    {
        LOG_DEBUG("Test means");
        {
            TMeanAccumulatorVec moments(1);
            moments[0].add(2.0);
            moments[0].add(3.0);

            {
                prelert::core::CRapidXmlStatePersistInserter inserter("root");
                prelert::maths::CBasicStatistics::persistSampleCentralMoments(moments, "moments", inserter);
                std::string xml;
                inserter.toXml(xml);
                LOG_DEBUG("Moments XML representation:\n" << xml);

                prelert::core::CRapidXmlParser parser;
                CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(xml));
                prelert::core::CRapidXmlStateRestoreTraverser traverser(parser);
                TMeanAccumulatorVec restored;
                traverser.traverseSubLevel(boost::bind(&restoreMeans, &restored, "moments", _1));
                LOG_DEBUG("restored = " << prelert::core::CContainerPrinter::print(restored));
                CPPUNIT_ASSERT_EQUAL(moments.size(), restored.size());
                for (std::size_t i = 0u; i < restored.size(); ++i)
                {
                    CPPUNIT_ASSERT_EQUAL(prelert::maths::CBasicStatistics::checksum(moments[i]),
                                         prelert::maths::CBasicStatistics::checksum(restored[i]));
                }
            }

            moments.push_back(TMeanAccumulator());
            moments.push_back(TMeanAccumulator());
            moments[1].add(3.0);
            moments[1].add(6.0);
            moments[2].add(10.0);
            moments[2].add(11.0);
            moments[2].add(12.0);

            {
                prelert::core::CRapidXmlStatePersistInserter inserter("root");
                prelert::maths::CBasicStatistics::persistSampleCentralMoments(moments, "moments", inserter);
                std::string xml;
                inserter.toXml(xml);
                LOG_DEBUG("Moments XML representation:\n" << xml);

                prelert::core::CRapidXmlParser parser;
                CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(xml));
                prelert::core::CRapidXmlStateRestoreTraverser traverser(parser);
                TMeanAccumulatorVec restored;
                traverser.traverseSubLevel(boost::bind(&restoreMeans, &restored, "moments", _1));
                LOG_DEBUG("restored = " << prelert::core::CContainerPrinter::print(restored));
                CPPUNIT_ASSERT_EQUAL(moments.size(), restored.size());
                for (std::size_t i = 0u; i < restored.size(); ++i)
                {
                    CPPUNIT_ASSERT_EQUAL(prelert::maths::CBasicStatistics::checksum(moments[i]),
                                         prelert::maths::CBasicStatistics::checksum(restored[i]));
                }
            }
        }
        LOG_DEBUG("Test means and variances");
        {
            TMeanVarAccumulatorVec moments(1);
            moments[0].add(2.0);
            moments[0].add(3.0);
            moments[0].add(3.5);
            {
                prelert::core::CRapidXmlStatePersistInserter inserter("root");
                prelert::maths::CBasicStatistics::persistSampleCentralMoments(moments, "moments", inserter);
                std::string xml;
                inserter.toXml(xml);
                LOG_DEBUG("Moments XML representation:\n" << xml);

                prelert::core::CRapidXmlParser parser;
                CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(xml));
                prelert::core::CRapidXmlStateRestoreTraverser traverser(parser);
                TMeanVarAccumulatorVec restored;
                traverser.traverseSubLevel(boost::bind(&restoreMeanVars, &restored, "moments", _1));
                LOG_DEBUG("restored = " << prelert::core::CContainerPrinter::print(restored));
                CPPUNIT_ASSERT_EQUAL(moments.size(), restored.size());
                for (std::size_t i = 0u; i < restored.size(); ++i)
                {
                    CPPUNIT_ASSERT_EQUAL(prelert::maths::CBasicStatistics::checksum(moments[i]),
                                         prelert::maths::CBasicStatistics::checksum(restored[i]));
                }
            }

            moments.push_back(TMeanVarAccumulator());
            moments.push_back(TMeanVarAccumulator());
            moments[1].add(3.0);
            moments[1].add(6.0);
            moments[1].add(6.0);
            moments[2].add(10.0);
            moments[2].add(11.0);
            moments[2].add(12.0);
            moments[2].add(12.0);
            {
                prelert::core::CRapidXmlStatePersistInserter inserter("root");
                prelert::maths::CBasicStatistics::persistSampleCentralMoments(moments, "moments", inserter);
                std::string xml;
                inserter.toXml(xml);
                LOG_DEBUG("Moments XML representation:\n" << xml);

                prelert::core::CRapidXmlParser parser;
                CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(xml));
                prelert::core::CRapidXmlStateRestoreTraverser traverser(parser);
                TMeanVarAccumulatorVec restored;
                traverser.traverseSubLevel(boost::bind(&restoreMeanVars, &restored, "moments", _1));
                LOG_DEBUG("restored = " << prelert::core::CContainerPrinter::print(restored));
                CPPUNIT_ASSERT_EQUAL(moments.size(), restored.size());
                for (std::size_t i = 0u; i < restored.size(); ++i)
                {
                    CPPUNIT_ASSERT_EQUAL(prelert::maths::CBasicStatistics::checksum(moments[i]),
                                         prelert::maths::CBasicStatistics::checksum(restored[i]));
                }
            }
        }
        LOG_DEBUG("Test means, variances and skews");
        {
            TMeanVarSkewAccumulatorVec moments(1);
            moments[0].add(2.0);
            moments[0].add(3.0);
            moments[0].add(3.5);
            {
                prelert::core::CRapidXmlStatePersistInserter inserter("root");
                prelert::maths::CBasicStatistics::persistSampleCentralMoments(moments, "moments", inserter);
                std::string xml;
                inserter.toXml(xml);
                LOG_DEBUG("Moments XML representation:\n" << xml);

                prelert::core::CRapidXmlParser parser;
                CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(xml));
                prelert::core::CRapidXmlStateRestoreTraverser traverser(parser);
                TMeanVarSkewAccumulatorVec restored;
                traverser.traverseSubLevel(boost::bind(&restoreMeanVarSkews, &restored, "moments", _1));
                LOG_DEBUG("restored = " << prelert::core::CContainerPrinter::print(restored));
                CPPUNIT_ASSERT_EQUAL(moments.size(), restored.size());
                for (std::size_t i = 0u; i < restored.size(); ++i)
                {
                    CPPUNIT_ASSERT_EQUAL(prelert::maths::CBasicStatistics::checksum(moments[i]),
                                         prelert::maths::CBasicStatistics::checksum(restored[i]));
                }
            }

            moments.push_back(TMeanVarSkewAccumulator());
            moments.push_back(TMeanVarSkewAccumulator());
            moments[1].add(3.0);
            moments[1].add(6.0);
            moments[1].add(6.0);
            moments[2].add(10.0);
            moments[2].add(11.0);
            moments[2].add(12.0);
            moments[2].add(12.0);
            {
                prelert::core::CRapidXmlStatePersistInserter inserter("root");
                prelert::maths::CBasicStatistics::persistSampleCentralMoments(moments, "moments", inserter);
                std::string xml;
                inserter.toXml(xml);
                LOG_DEBUG("Moments XML representation:\n" << xml);

                prelert::core::CRapidXmlParser parser;
                CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(xml));
                prelert::core::CRapidXmlStateRestoreTraverser traverser(parser);
                TMeanVarSkewAccumulatorVec restored;
                traverser.traverseSubLevel(boost::bind(&restoreMeanVarSkews, &restored, "moments", _1));
                LOG_DEBUG("restored = " << prelert::core::CContainerPrinter::print(restored));
                CPPUNIT_ASSERT_EQUAL(moments.size(), restored.size());
                for (std::size_t i = 0u; i < restored.size(); ++i)
                {
                    CPPUNIT_ASSERT_EQUAL(prelert::maths::CBasicStatistics::checksum(moments[i]),
                                         prelert::maths::CBasicStatistics::checksum(restored[i]));
                }
            }
        }
    }

    CPPUNIT_ASSERT_EQUAL(true, prelert::core::memory_detail::SDynamicSizeAlwaysZero<TMeanAccumulator>::value());
    CPPUNIT_ASSERT_EQUAL(true, prelert::core::memory_detail::SDynamicSizeAlwaysZero<TMeanVarAccumulator>::value());
    CPPUNIT_ASSERT_EQUAL(true, prelert::core::memory_detail::SDynamicSizeAlwaysZero<TMeanVarSkewAccumulator>::value());
}

void CBasicStatisticsTest::testCovariances(void)
{
    LOG_DEBUG("+-----------------------------------------+");
    LOG_DEBUG("|  CBasicStatisticsTest::testCovariances  |");
    LOG_DEBUG("+-----------------------------------------+");

    LOG_DEBUG("N(3,I)");
    {
        const double raw[][3] =
            {
                { 2.58894, 2.87211, 1.62609 },
                { 3.88246, 2.98577, 2.70981 },
                { 2.03317, 3.33715, 2.93560 },
                { 3.30100, 4.38844, 1.65705 },
                { 2.12426, 2.21127, 2.57000 },
                { 4.21041, 4.20745, 1.90752 },
                { 3.56139, 3.14454, 0.89316 },
                { 4.29444, 1.58715, 3.58402 },
                { 3.06731, 3.91581, 2.85951 },
                { 3.62798, 2.28786, 2.89994 },
                { 2.05834, 2.96137, 3.57654 },
                { 2.72185, 3.36003, 3.09708 },
                { 0.94924, 2.19797, 3.30941 },
                { 2.11159, 2.49182, 3.56793 },
                { 3.10364, 0.32747, 3.62487 },
                { 2.28235, 3.83542, 3.35942 },
                { 3.30549, 2.95951, 2.97006 },
                { 3.05787, 2.94188, 2.64095 },
                { 3.98245, 2.02892, 3.07909 },
                { 3.81189, 2.89389, 3.81389 },
                { 3.32811, 3.88484, 4.17866 },
                { 2.06964, 3.80683, 2.46835 },
                { 4.58989, 2.00321, 1.93029 },
                { 2.51484, 4.46106, 3.71248 },
                { 3.30729, 2.44768, 3.43241 },
                { 3.52222, 2.91724, 1.49631 },
                { 1.71826, 4.79752, 4.38398 },
                { 3.14173, 3.16237, 2.49654 },
                { 3.26538, 2.21858, 5.05477 },
                { 2.88352, 1.94396, 3.08744 }
            };

        const double expectedMean[] = { 3.013898, 2.952637, 2.964104 };
        const double expectedCovariances[][3] =
            {
                {  0.711903, -0.174535, -0.199460 },
                { -0.174535,  0.935285, -0.091192 },
                { -0.199460, -0.091192,  0.833710 }
            };

        prelert::maths::CBasicStatistics::SSampleCovariances<double, 3> covariances;

        for (std::size_t i = 0u; i < boost::size(raw); ++i)
        {
            prelert::maths::CVectorNx1<double, 3> v(raw[i]);
            LOG_DEBUG("v = " << v);
            covariances.add(v);
        }

        LOG_DEBUG("count = " << prelert::maths::CBasicStatistics::count(covariances));
        LOG_DEBUG("mean = " << prelert::maths::CBasicStatistics::mean(covariances));
        LOG_DEBUG("covariances = " << prelert::maths::CBasicStatistics::covariances(covariances));

        CPPUNIT_ASSERT_EQUAL(static_cast<double>(boost::size(raw)),
                             prelert::maths::CBasicStatistics::count(covariances));
        for (std::size_t i = 0u; i < 3; ++i)
        {
            CPPUNIT_ASSERT_DOUBLES_EQUAL(expectedMean[i],
                                         (prelert::maths::CBasicStatistics::mean(covariances))(i),
                                         2e-6);
            for (std::size_t j = 0u; j < 3; ++j)
            {
                CPPUNIT_ASSERT_DOUBLES_EQUAL(expectedCovariances[i][j],
                                             (prelert::maths::CBasicStatistics::covariances(covariances))(i, j),
                                             2e-6);
            }
        }

        bool dynamicSizeAlwaysZero = prelert::core::memory_detail::SDynamicSizeAlwaysZero<
                                         prelert::maths::CBasicStatistics::SSampleCovariances<double, 3> >::value();
        CPPUNIT_ASSERT_EQUAL(true, dynamicSizeAlwaysZero);
    }

    {
        typedef std::vector<prelert::maths::CVectorNx1<double, 4> > TVectorVec;

        double mean_[] = { 1.0, 3.0, 2.0, 7.0 };
        prelert::maths::CVectorNx1<double, 4> mean(mean_);

        double covariances1_[] = {  1.0,  1.0,  1.0, 1.0 };
        double covariances2_[] = { -1.0,  1.0,  0.0, 0.0 };
        double covariances3_[] = { -1.0, -1.0,  2.0, 0.0 };
        double covariances4_[] = { -1.0, -1.0, -1.0, 3.0 };

        prelert::maths::CVectorNx1<double, 4> covariances1(covariances1_);
        prelert::maths::CVectorNx1<double, 4> covariances2(covariances2_);
        prelert::maths::CVectorNx1<double, 4> covariances3(covariances3_);
        prelert::maths::CVectorNx1<double, 4> covariances4(covariances4_);

        prelert::maths::CSymmetricMatrixNxN<double, 4> covariance(
                  10.0 * prelert::maths::CSymmetricMatrixNxN<double, 4>(prelert::maths::E_OuterProduct,
                                                                        covariances1 / covariances1.euclidean())
                +  5.0 * prelert::maths::CSymmetricMatrixNxN<double, 4>(prelert::maths::E_OuterProduct,
                                                                        covariances2 / covariances2.euclidean())
                +  5.0 * prelert::maths::CSymmetricMatrixNxN<double, 4>(prelert::maths::E_OuterProduct,
                                                                        covariances3 / covariances3.euclidean())
                +  2.0 * prelert::maths::CSymmetricMatrixNxN<double, 4>(prelert::maths::E_OuterProduct,
                                                                        covariances4 / covariances4.euclidean()));

        std::size_t n = 10000u;

        TVectorVec samples;
        prelert::maths::CSampling::multivariateNormalSample(mean, covariance, n, samples);

        prelert::maths::CBasicStatistics::SSampleCovariances<double, 4> sampleCovariance;

        for (std::size_t i = 0u; i < n; ++i)
        {
            sampleCovariance.add(samples[i]);
        }

        LOG_DEBUG("expected mean = " << mean);
        LOG_DEBUG("expected covariances = " << covariance);

        LOG_DEBUG("mean = " << prelert::maths::CBasicStatistics::mean(sampleCovariance));
        LOG_DEBUG("covariances = " << prelert::maths::CBasicStatistics::covariances(sampleCovariance));

        for (std::size_t i = 0u; i < 4; ++i)
        {
            CPPUNIT_ASSERT_DOUBLES_EQUAL(mean(i),
                                         (prelert::maths::CBasicStatistics::mean(sampleCovariance))(i),
                                         0.05);
            for (std::size_t j = 0u; j < 4; ++j)
            {
                CPPUNIT_ASSERT_DOUBLES_EQUAL(covariance(i, j),
                                             (prelert::maths::CBasicStatistics::covariances(sampleCovariance))(i, j),
                                             0.15);

            }
        }
    }

    {
        prelert::test::CRandomNumbers rng;

        std::vector<double> coordinates;
        rng.generateUniformSamples(5.0, 10.0, 400, coordinates);

        std::vector<prelert::maths::CVectorNx1<double, 4> > points;
        for (std::size_t i = 0u; i < coordinates.size(); i += 4)
        {
            double c[] =
                {
                    coordinates[i+0],
                    coordinates[i+1],
                    coordinates[i+2],
                    coordinates[i+3]
                };
            points.push_back(prelert::maths::CVectorNx1<double, 4>(c));
        }

        prelert::maths::CBasicStatistics::SSampleCovariances<double, 4> expectedSampleCovariances;
        for (std::size_t i = 0u; i < points.size(); ++i)
        {
            expectedSampleCovariances.add(points[i]);
        }

        std::string expectedDelimited = expectedSampleCovariances.toDelimited();
        LOG_DEBUG("delimited = " << expectedDelimited);

        prelert::maths::CBasicStatistics::SSampleCovariances<double, 4> sampleCovariances;
        CPPUNIT_ASSERT(sampleCovariances.fromDelimited(expectedDelimited));

        CPPUNIT_ASSERT_EQUAL(prelert::maths::CBasicStatistics::checksum(expectedSampleCovariances),
                             prelert::maths::CBasicStatistics::checksum(sampleCovariances));

        std::string delimited = sampleCovariances.toDelimited();
        CPPUNIT_ASSERT_EQUAL(expectedDelimited, delimited);
    }
}

void CBasicStatisticsTest::testCovariancesLedoitWolf(void)
{
    LOG_DEBUG("+---------------------------------------------------+");
    LOG_DEBUG("|  CBasicStatisticsTest::testCovariancesLedoitWolf  |");
    LOG_DEBUG("+---------------------------------------------------+");

    typedef std::vector<double> TDoubleVec;
    typedef std::vector<TDoubleVec> TDoubleVecVec;
    typedef prelert::maths::CVectorNx1<double, 2> TVector2;
    typedef std::vector<TVector2> TVector2Vec;
    typedef prelert::maths::CSymmetricMatrixNxN<double, 2> TMatrix2;

    prelert::test::CRandomNumbers rng;

    double means[][2] =
        {
            {  10.0,  10.0  },
            {  20.0,  150.0 },
            { -10.0, -20.0  },
            { -20.0,  40.0  },
            {  40.0,  90.0  }
        };

    double covariances[][2][2] =
        {
            { {  40.0,   0.0 }, {   0.0, 40.0 } },
            { {  20.0,   5.0 }, {   5.0, 10.0 } },
            { { 300.0, -70.0 }, { -70.0, 60.0 } },
            { { 100.0,  20.0 }, {  20.0, 60.0 } },
            { {  50.0, -10.0 }, { -10.0, 60.0 } }
        };

    prelert::maths::CBasicStatistics::SSampleMean<double>::TAccumulator error;
    prelert::maths::CBasicStatistics::SSampleMean<double>::TAccumulator errorLW;

    for (std::size_t i = 0u; i < boost::size(means); ++i)
    {
        LOG_DEBUG("*** test " << i << " ***");

        TDoubleVec mean(boost::begin(means[i]), boost::end(means[i]));
        TDoubleVecVec covariance;
        for (std::size_t j = 0u; j < boost::size(covariances[i]); ++j)
        {
            covariance.push_back(TDoubleVec(boost::begin(covariances[i][j]),
                                            boost::end(covariances[i][j])));
        }
        TMatrix2 covExpected(covariance);
        LOG_DEBUG("cov expected = " << covExpected);

        TDoubleVecVec samples;
        rng.generateMultivariateNormalSamples(mean, covariance, 50, samples);

        // Test the frobenius norm of the error in the covariance matrix.

        for (std::size_t j = 3u; j < samples.size(); ++j)
        {
            TVector2Vec jsamples;
            for (std::size_t k = 0u; k < j; ++k)
            {
                jsamples.push_back(TVector2(samples[k]));
            }

            prelert::maths::CBasicStatistics::SSampleCovariances<double, 2> cov;
            cov.add(jsamples);

            prelert::maths::CBasicStatistics::SSampleCovariances<double, 2> covLW;
            prelert::maths::CBasicStatistics::covariancesLedoitWolf(jsamples, covLW);

            const TMatrix2 &covML   = prelert::maths::CBasicStatistics::maximumLikelihoodCovariances(cov);
            const TMatrix2 &covLWML = prelert::maths::CBasicStatistics::maximumLikelihoodCovariances(covLW);

            double errorML   = (covML - covExpected).frobenius();
            double errorLWML = (covLWML - covExpected).frobenius();

            if (j % 5 == 0)
            {
                LOG_DEBUG("cov ML   = " << covML);
                LOG_DEBUG("cov LWML = " << covLWML);
                LOG_DEBUG("error ML = " << errorML << ", error LWML = " << errorLWML);
            }
            CPPUNIT_ASSERT(errorLWML < 6.0 * errorML);
            error.add(errorML / covExpected.frobenius());
            errorLW.add(errorLWML / covExpected.frobenius());
        }
    }

    LOG_DEBUG("error    = " << error);
    LOG_DEBUG("error LW = " << errorLW);
    CPPUNIT_ASSERT(  prelert::maths::CBasicStatistics::mean(errorLW)
                   < 0.9 * prelert::maths::CBasicStatistics::mean(error));
}

void CBasicStatisticsTest::testMedian(void)
{
    LOG_DEBUG("+------------------------------------+");
    LOG_DEBUG("|  CBasicStatisticsTest::testMedian  |");
    LOG_DEBUG("+------------------------------------+");

    {
        prelert::maths::CBasicStatistics::TDoubleVec sampleVec;

        double median = prelert::maths::CBasicStatistics::median(sampleVec);

        CPPUNIT_ASSERT_EQUAL(0.0, median);
    }
    {
        double sample[] = { 1.0 };

        prelert::maths::CBasicStatistics::TDoubleVec sampleVec(sample, sample+sizeof(sample)/sizeof(sample[0]));

        double median = prelert::maths::CBasicStatistics::median(sampleVec);

        CPPUNIT_ASSERT_EQUAL(1.0, median);
    }
    {
        double sample[] = { 2.0, 1.0 };

        prelert::maths::CBasicStatistics::TDoubleVec sampleVec(sample, sample+sizeof(sample)/sizeof(sample[0]));

        double median = prelert::maths::CBasicStatistics::median(sampleVec);

        CPPUNIT_ASSERT_EQUAL(1.5, median);
    }
    {
        double sample[] = { 3.0, 1.0, 2.0 };

        prelert::maths::CBasicStatistics::TDoubleVec sampleVec(sample, sample+sizeof(sample)/sizeof(sample[0]));

        double median = prelert::maths::CBasicStatistics::median(sampleVec);

        CPPUNIT_ASSERT_EQUAL(2.0, median);
    }
    {
        double sample[] = { 3.0, 5.0, 9.0, 1.0, 2.0, 6.0, 7.0, 4.0, 8.0 };

        prelert::maths::CBasicStatistics::TDoubleVec sampleVec(sample, sample+sizeof(sample)/sizeof(sample[0]));

        double median = prelert::maths::CBasicStatistics::median(sampleVec);

        CPPUNIT_ASSERT_EQUAL(5.0, median);
    }
    {
        double sample[] = { 3.0, 5.0, 10.0, 2.0, 6.0, 7.0, 1.0, 9.0, 4.0, 8.0 };

        prelert::maths::CBasicStatistics::TDoubleVec sampleVec(sample, sample+sizeof(sample)/sizeof(sample[0]));

        double median = prelert::maths::CBasicStatistics::median(sampleVec);

        CPPUNIT_ASSERT_EQUAL(5.5, median);
    }
}

void CBasicStatisticsTest::testQuartiles(void)
{
    LOG_DEBUG("+---------------------------------------+");
    LOG_DEBUG("|  CBasicStatisticsTest::testQuartiles  |");
    LOG_DEBUG("+---------------------------------------+");

    {
        prelert::maths::CBasicStatistics::TDoubleVec sampleVec;

        double q1 = prelert::maths::CBasicStatistics::firstQuartile(sampleVec);
        double q3 = prelert::maths::CBasicStatistics::thirdQuartile(sampleVec);

        CPPUNIT_ASSERT_EQUAL(0.0, q1);
        CPPUNIT_ASSERT_EQUAL(0.0, q3);
    }
    {
        double sample[] = { 1.0 };

        prelert::maths::CBasicStatistics::TDoubleVec sampleVec(sample, sample+sizeof(sample)/sizeof(sample[0]));

        double q1 = prelert::maths::CBasicStatistics::firstQuartile(sampleVec);
        double q3 = prelert::maths::CBasicStatistics::thirdQuartile(sampleVec);

        CPPUNIT_ASSERT_EQUAL(1.0, q1);
        CPPUNIT_ASSERT_EQUAL(1.0, q3);
    }
    {
        double sample[] = { 2.0, 1.0 };

        prelert::maths::CBasicStatistics::TDoubleVec sampleVec(sample, sample+sizeof(sample)/sizeof(sample[0]));

        double q1 = prelert::maths::CBasicStatistics::firstQuartile(sampleVec);
        double q3 = prelert::maths::CBasicStatistics::thirdQuartile(sampleVec);

        CPPUNIT_ASSERT_EQUAL(1.0, q1);
        CPPUNIT_ASSERT_EQUAL(2.0, q3);
    }
    {
        double sample[] = { 3.0, 1.0, 2.0 };

        prelert::maths::CBasicStatistics::TDoubleVec sampleVec(sample, sample+sizeof(sample)/sizeof(sample[0]));

        double q1 = prelert::maths::CBasicStatistics::firstQuartile(sampleVec);
        double q3 = prelert::maths::CBasicStatistics::thirdQuartile(sampleVec);

        CPPUNIT_ASSERT_EQUAL(1.5, q1);
        CPPUNIT_ASSERT_EQUAL(2.5, q3);
    }
    {
        double sample[] = { 3.0, 5.0, 9.0, 1.0, 2.0, 6.0, 7.0, 4.0, 8.0 };

        prelert::maths::CBasicStatistics::TDoubleVec sampleVec(sample, sample+sizeof(sample)/sizeof(sample[0]));

        double q1 = prelert::maths::CBasicStatistics::firstQuartile(sampleVec);
        double q3 = prelert::maths::CBasicStatistics::thirdQuartile(sampleVec);

        CPPUNIT_ASSERT_EQUAL(3.0, q1);
        CPPUNIT_ASSERT_EQUAL(7.0, q3);
    }
    {
        double sample[] = { 3.0, 5.0, 10.0, 2.0, 6.0, 7.0, 1.0, 9.0, 4.0, 8.0 };

        prelert::maths::CBasicStatistics::TDoubleVec sampleVec(sample, sample+sizeof(sample)/sizeof(sample[0]));

        double q1 = prelert::maths::CBasicStatistics::firstQuartile(sampleVec);
        double q3 = prelert::maths::CBasicStatistics::thirdQuartile(sampleVec);

        CPPUNIT_ASSERT_EQUAL(3.0, q1);
        CPPUNIT_ASSERT_EQUAL(8.0, q3);
    }
    {
        double sample[] = { 3.0, 1.0, 5.0, 10.0, 2.0, 6.0, 7.0, 11.0, 9.0, 4.0, 8.0 };

        prelert::maths::CBasicStatistics::TDoubleVec sampleVec(sample, sample+sizeof(sample)/sizeof(sample[0]));

        double q1 = prelert::maths::CBasicStatistics::firstQuartile(sampleVec);
        double q3 = prelert::maths::CBasicStatistics::thirdQuartile(sampleVec);

        CPPUNIT_ASSERT_EQUAL(3.5, q1);
        CPPUNIT_ASSERT_EQUAL(8.5, q3);
    }
}

void CBasicStatisticsTest::testQuartileRelationship(void)
{
    LOG_DEBUG("+--------------------------------------------------+");
    LOG_DEBUG("|  CBasicStatisticsTest::testQuartileRelationship  |");
    LOG_DEBUG("+--------------------------------------------------+");

    // The first quartile of a set of numbers x1, ..., xN should be the negation
    // of the third quartile of -x1, ..., -xN

    // Test vectors of every size modulo 4
    for (size_t testSize = 10000; testSize < 10004; ++testSize)
    {
        LOG_DEBUG("Testing quartiles for vector of size " << testSize);

        prelert::maths::CBasicStatistics::TDoubleVec sampleVec1;
        sampleVec1.reserve(testSize);
        prelert::maths::CBasicStatistics::TDoubleVec sampleVec2;
        sampleVec2.reserve(testSize);

        for (size_t count = 0; count < testSize; ++count)
        {
            double value(static_cast<double>(::rand() - 100));
            sampleVec1.push_back(value);
            sampleVec2.push_back(-value);
        }

        double q1 = prelert::maths::CBasicStatistics::firstQuartile(sampleVec1);
        double q3 = prelert::maths::CBasicStatistics::thirdQuartile(sampleVec2);

        CPPUNIT_ASSERT_EQUAL(-q1, q3);
    }
}

namespace
{

const std::string TAG("a");

template<typename T>
bool restore(T &statistic,
             prelert::core::CStateRestoreTraverser &traverser)
{
    do
    {
        const std::string &name = traverser.name();
        if (name == TAG)
        {
            if (statistic.restore(traverser) == false)
            {
                LOG_ERROR("Invalid statistic in " << traverser.value());
                return false;
            }
        }
    }
    while (traverser.next());
    return true;
}

}

void CBasicStatisticsTest::testOrderStatistics(void)
{
    LOG_DEBUG("+---------------------------------------------+");
    LOG_DEBUG("|  CBasicStatisticsTest::testOrderStatistics  |");
    LOG_DEBUG("+---------------------------------------------+");

    // Test that the order statistics accumulators work for finding min and max
    // elements of a collection.

    typedef prelert::maths::CBasicStatistics::COrderStatisticsStack<double, 2u> TMinStatsStack;
    typedef prelert::maths::CBasicStatistics::COrderStatisticsStack<double, 3u, std::greater<double> > TMaxStatsStack;
    typedef prelert::maths::CBasicStatistics::COrderStatisticsHeap<double> TMinStatsHeap;
    typedef prelert::maths::CBasicStatistics::COrderStatisticsHeap<double, std::greater<double> > TMaxStatsHeap;

    {
        double data[] =
            {
                1.0, 2.3, 1.1, 1.0, 5.0, 3.0, 11.0, 0.2, 15.8, 12.3
            };

        TMinStatsStack minValues;
        TMaxStatsStack maxValues;
        TMinStatsStack minFirstHalf;
        TMinStatsStack minSecondHalf;

        for (size_t i = 0; i < boost::size(data); ++i)
        {
            minValues.add(data[i]);
            maxValues.add(data[i]);
            (2 * i < boost::size(data) ? minFirstHalf : minSecondHalf).add(data[i]);
        }

        std::sort(boost::begin(data), boost::end(data));
        minValues.sort();
        LOG_DEBUG("x_1 = " << minValues[0]
                  << ", x_2 = " << minValues[1]);
        CPPUNIT_ASSERT(std::equal(minValues.begin(), minValues.end(), data));

        std::sort(boost::begin(data), boost::end(data), std::greater<double>());
        maxValues.sort();
        LOG_DEBUG("x_n = " << maxValues[0]
                  << ", x_(n-1) = " << maxValues[1]
                  << ", x_(n-2) = " << maxValues[2]);
        CPPUNIT_ASSERT(std::equal(maxValues.begin(), maxValues.end(), data));

        CPPUNIT_ASSERT_EQUAL(static_cast<size_t>(2), minValues.count());
        CPPUNIT_ASSERT_EQUAL(static_cast<size_t>(3), maxValues.count());

        TMinStatsStack minFirstPlusSecondHalf = (minFirstHalf + minSecondHalf);
        minFirstPlusSecondHalf.sort();
        CPPUNIT_ASSERT(std::equal(minValues.begin(), minValues.end(),
                                  minFirstPlusSecondHalf.begin()));

        // Test persist is idempotent.

        std::string origXml;
        {
            prelert::core::CRapidXmlStatePersistInserter inserter("root");
            minValues.persist(TAG, inserter);
            inserter.toXml(origXml);
        }

        LOG_DEBUG("Stats XML representation:\n" << origXml);

        // Restore the XML into stats object.
        TMinStatsStack restoredMinValues;
        {
            prelert::core::CRapidXmlParser parser;
            CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(origXml));
            prelert::core::CRapidXmlStateRestoreTraverser traverser(parser);
            CPPUNIT_ASSERT(traverser.traverseSubLevel(boost::bind(restore<TMinStatsStack>,
                                                                  boost::ref(restoredMinValues),
                                                                  _1)));
        }

        // The XML representation of the new stats object should be unchanged.
        std::string newXml;
        {
            prelert::core::CRapidXmlStatePersistInserter inserter("root");
            restoredMinValues.persist(TAG, inserter);
            inserter.toXml(newXml);
        }
        CPPUNIT_ASSERT_EQUAL(origXml, newXml);
    }

    {
        double data[] =
            {
                1.0, 2.3, 1.1, 1.0, 5.0, 3.0, 11.0, 0.2, 15.8, 12.3
            };

        TMinStatsHeap min2Values(2);
        TMaxStatsHeap max3Values(3);
        TMaxStatsHeap max20Values(20);

        for (size_t i = 0; i < boost::size(data); ++i)
        {
            min2Values.add(data[i]);
            max3Values.add(data[i]);
            max20Values.add(data[i]);
        }

        std::sort(boost::begin(data), boost::end(data));
        min2Values.sort();
        LOG_DEBUG("x_1 = " << min2Values[0]
                  << ", x_2 = " << min2Values[1]);
        CPPUNIT_ASSERT(std::equal(min2Values.begin(), min2Values.end(), data));

        std::sort(boost::begin(data), boost::end(data), std::greater<double>());
        max3Values.sort();
        LOG_DEBUG("x_n = " << max3Values[0]
                  << ", x_(n-1) = " << max3Values[1]
                  << ", x_(n-2) = " << max3Values[2]);
        CPPUNIT_ASSERT(std::equal(max3Values.begin(), max3Values.end(), data));

        max20Values.sort();
        CPPUNIT_ASSERT_EQUAL(boost::size(data), max20Values.count());
        CPPUNIT_ASSERT(std::equal(max20Values.begin(), max20Values.end(), data));

        // Test persist is idempotent.

        std::string origXml;
        {
            prelert::core::CRapidXmlStatePersistInserter inserter("root");
            max20Values.persist(TAG, inserter);
            inserter.toXml(origXml);
        }

        LOG_DEBUG("Stats XML representation:\n" << origXml);

        // Restore the XML into stats object.
        TMinStatsHeap restoredMaxValues(20);
        {
            prelert::core::CRapidXmlParser parser;
            CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(origXml));
            prelert::core::CRapidXmlStateRestoreTraverser traverser(parser);
            CPPUNIT_ASSERT(traverser.traverseSubLevel(boost::bind(restore<TMinStatsHeap>,
                                                                  boost::ref(restoredMaxValues),
                                                                  _1)));
        }

        // The XML representation of the new stats object should be unchanged.
        std::string newXml;
        {
            prelert::core::CRapidXmlStatePersistInserter inserter("root");
            restoredMaxValues.persist(TAG, inserter);
            inserter.toXml(newXml);
        }
        CPPUNIT_ASSERT_EQUAL(origXml, newXml);
    }
    {
        // Test we correctly age the minimum value accumulator.
        TMinStatsStack test;
        test.add(15.0);
        test.age(0.5);
        CPPUNIT_ASSERT_EQUAL(30.0, test[0]);
    }
    {
        // Test we correctly age the maximum value accumulator.
        TMaxStatsStack test;
        test.add(15.0);
        test.age(0.5);
        CPPUNIT_ASSERT_EQUAL(7.5, test[0]);
    }

    CPPUNIT_ASSERT_EQUAL(true, prelert::core::memory_detail::SDynamicSizeAlwaysZero<TMinStatsStack>::value());
    CPPUNIT_ASSERT_EQUAL(true, prelert::core::memory_detail::SDynamicSizeAlwaysZero<TMaxStatsStack>::value());
    CPPUNIT_ASSERT_EQUAL(false, prelert::core::memory_detail::SDynamicSizeAlwaysZero<TMinStatsHeap>::value());
    CPPUNIT_ASSERT_EQUAL(false, prelert::core::memory_detail::SDynamicSizeAlwaysZero<TMaxStatsHeap>::value());
}
