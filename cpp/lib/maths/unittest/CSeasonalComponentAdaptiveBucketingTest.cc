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

#include "CSeasonalComponentAdaptiveBucketingTest.h"

#include <core/CContainerPrinter.h>
#include <core/CLogger.h>
#include <core/CRapidXmlParser.h>
#include <core/CRapidXmlStatePersistInserter.h>
#include <core/CRapidXmlStateRestoreTraverser.h>

#include <maths/CSeasonalComponentAdaptiveBucketing.h>
#include <maths/CSeasonalTime.h>

#include <test/CRandomNumbers.h>

#include <boost/math/constants/constants.hpp>
#include <boost/range.hpp>

#include <string>
#include <vector>

using namespace ml;

namespace
{
typedef std::vector<double> TDoubleVec;
typedef std::vector<maths::CFloatStorage> TFloatVec;
typedef maths::CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;
typedef maths::CBasicStatistics::SSampleMeanVar<double>::TAccumulator TMeanVarAccumulator;
}

void CSeasonalComponentAdaptiveBucketingTest::testInitialize(void)
{
    LOG_DEBUG("+-----------------------------------------------------------+");
    LOG_DEBUG("|  CSeasonalComponentAdaptiveBucketingTest::testInitialize  |");
    LOG_DEBUG("+-----------------------------------------------------------+");

    maths::CDiurnalTime time(0, 1, 101, 100);
    maths::CSeasonalComponentAdaptiveBucketing bucketing(time);

    CPPUNIT_ASSERT(!bucketing.initialize(10.0, 1.0, 10));
    CPPUNIT_ASSERT(!bucketing.initialize(1.0, 10.0, 0));

    const std::string expectedEndpoints("[1, 11, 21, 31, 41, 51, 61, 71, 81, 91, 101]");
    const std::string expectedKnots("[1, 6, 16, 26, 36, 46, 56, 66, 76, 86, 96, 101]");
    const std::string expectedValues("[51, 6, 16, 26, 36, 46, 56, 66, 76, 86, 96, 51]");

    CPPUNIT_ASSERT(bucketing.initialize(1.0, 101.0, 10));
    const TFloatVec &endpoints = bucketing.endpoints();
    CPPUNIT_ASSERT_EQUAL(expectedEndpoints,
                         core::CContainerPrinter::print(endpoints));

    for (std::size_t i = 6u; i < 106; i += 10)
    {
        bucketing.add(static_cast<core_t::TTime>(i), static_cast<double>(i));
    }
    TDoubleVec knots;
    TDoubleVec values;
    TDoubleVec variances;
    bucketing.knots(1, maths::CSplineTypes::E_Periodic, knots, values, variances);
    CPPUNIT_ASSERT_EQUAL(expectedKnots, core::CContainerPrinter::print(knots));
    CPPUNIT_ASSERT_EQUAL(expectedValues, core::CContainerPrinter::print(values));
    LOG_DEBUG("variances = " << core::CContainerPrinter::print(variances))
}

void CSeasonalComponentAdaptiveBucketingTest::testSwap(void)
{
    LOG_DEBUG("+-----------------------------------------------------+");
    LOG_DEBUG("|  CSeasonalComponentAdaptiveBucketingTest::testSwap  |");
    LOG_DEBUG("+-----------------------------------------------------+");

    maths::CDiurnalTime time1(0, 0, 100, 100);
    maths::CSeasonalComponentAdaptiveBucketing bucketing1(time1, 0.05);

    test::CRandomNumbers rng;

    bucketing1.initialize(0.0, 100.0, 10);
    for (std::size_t p = 0; p < 50; ++p)
    {
        TDoubleVec noise;
        rng.generateNormalSamples(0.0, 2.0, 100, noise);

        for (std::size_t i = 0u; i < 100; ++i)
        {
            core_t::TTime x = static_cast<core_t::TTime>(100 * p + i);
            double y = 0.02 * (static_cast<double>(i) - 50.0)
                            * (static_cast<double>(i) - 50.0) + noise[i];
            bucketing1.add(x, y);
        }
        bucketing1.refine(static_cast<core_t::TTime>(100 * (p + 1)));
        bucketing1.propagateForwardsByTime(1.0);
    }

    maths::CDiurnalTime time2(10, 10, 120, 110);
    maths::CSeasonalComponentAdaptiveBucketing bucketing2(time2, 0.1);

    uint64_t checksum1 = bucketing1.checksum();
    uint64_t checksum2 = bucketing2.checksum();

    bucketing1.swap(bucketing2);

    LOG_DEBUG("checksum 1 = " << checksum1);
    LOG_DEBUG("checksum 2 = " << checksum2);

    CPPUNIT_ASSERT_EQUAL(checksum1, bucketing2.checksum());
    CPPUNIT_ASSERT_EQUAL(checksum2, bucketing1.checksum());
}

void CSeasonalComponentAdaptiveBucketingTest::testRefine(void)
{
    LOG_DEBUG("+-------------------------------------------------------+");
    LOG_DEBUG("|  CSeasonalComponentAdaptiveBucketingTest::testRefine  |");
    LOG_DEBUG("+-------------------------------------------------------+");

    // Test that the variance in each bucket is approximately equal.

    // The test function is y = (x - 50)^2 / 50.

    maths::CDiurnalTime time(0, 0, 100, 100);
    maths::CSeasonalComponentAdaptiveBucketing bucketing(time, 0.05);

    test::CRandomNumbers rng;

    bucketing.initialize(0.0, 100.0, 10);
    for (std::size_t p = 0; p < 50; ++p)
    {
        TDoubleVec noise;
        rng.generateNormalSamples(0.0, 2.0, 100, noise);

        for (std::size_t i = 0u; i < 100; ++i)
        {
            core_t::TTime x = static_cast<core_t::TTime>(100 * p + i);
            double y = 0.02 * (static_cast<double>(i) - 50.0)
                            * (static_cast<double>(i) - 50.0) + noise[i];
            bucketing.add(x, y);
        }
        bucketing.refine(static_cast<core_t::TTime>(100 * (p + 1)));
        bucketing.propagateForwardsByTime(1.0);
    }

    const TFloatVec &endpoints = bucketing.endpoints();
    TDoubleVec values = bucketing.values(5100);
    TDoubleVec variances = bucketing.variances();
    LOG_DEBUG("endpoints = " << core::CContainerPrinter::print(endpoints));
    LOG_DEBUG("values    = " << core::CContainerPrinter::print(values));
    LOG_DEBUG("variances = " << core::CContainerPrinter::print(variances));

    TMeanAccumulator meanError;
    TMeanAccumulator varianceError;
    TMeanVarAccumulator avgError;

    for (std::size_t i = 1u; i < endpoints.size(); ++i)
    {
        double a = endpoints[i-1];
        double b = endpoints[i];
        LOG_DEBUG("bucket = [" << a << "," << b << "]");

        a -= 50.0;
        b -= 50.0;

        double m = values[i-1];
        double v = variances[i-1];

        // Function mean and variance.
        double m_ = ::fabs(a) < ::fabs(b) ?
                    0.02 / 3.0 * ::pow(b, 3.0)
                    * (1.0 - ::pow(a/b, 3.0)) / (b-a) :
                    0.02 / 3.0 * ::pow(a, 3.0)
                    * (::pow(b/a, 3.0) - 1.0) / (b-a);
        double v_ = (::fabs(a) < ::fabs(b) ?
                     0.0004 / 5.0 * ::pow(b, 5.0)
                     * (1.0 - ::pow(a/b, 5.0)) / (b-a) :
                     0.0004 / 5.0 * ::pow(a, 5.0)
                     * (::pow(b/a, 5.0) - 1.0) / (b-a))
                    - m_ * m_ + 2.0;
        LOG_DEBUG("m = " << m
                  << ", m_ = " << m_
                  << ", absolute error = " << ::fabs(m - m_));
        LOG_DEBUG("v = " << v
                  << ", v_ = " << v_
                  << ", relative error = " << ::fabs(v - v_) / v_);

        CPPUNIT_ASSERT_DOUBLES_EQUAL(m_, m, 0.7);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(v_, v, 0.21 * v_);
        meanError.add(::fabs(m_ - m) / m_);
        varianceError.add(::fabs(v_ - v) / v_);

        if (i == 1 || i == endpoints.size() - 1)
        {
            continue;
        }
        if ((b * b / 50.0 - m) * (a * a / 50.0 - m) < 0.0)
        {
            // Root.
            double c = b < 0.0 ? -::sqrt(50.0 * m) : +::sqrt(50.0 * m);
            // Left and right partial averaging errors.
            double l = ::fabs(c) < ::fabs(a) ?
                       0.02 / 3.0 * a * a * a
                       * ((c/a) * (c/a) * (c/a) - 1.0) - m * (c-a) :
                       0.02 / 3.0 * c * c * c
                       * (1.0 - (a/c) * (a/c) * (a/c)) - m * (c-a);
            double r = ::fabs(c) < ::fabs(b) ?
                       0.02 / 3.0 * b * b * b
                       * (1.0 - (c/b) * (c/b) * (c/b)) - m * (b-c) :
                       0.02 / 3.0 * c * c * c
                       * ((b/c) * (b/c) * (b/c) - 1.0) - m * (b-c);
            LOG_DEBUG("c = " << c
                      << ", l = " << l << " r = " << r
                      << ", error = " << ::fabs(l) + ::fabs(r));
            avgError.add(::fabs(l) + ::fabs(r));
        }
        else
        {
            avgError.add(::fabs((m_ - m) * (b - a)));
        }
    }

    double meanError_ = maths::CBasicStatistics::mean(meanError);
    double varianceError_ = maths::CBasicStatistics::mean(varianceError);
    LOG_DEBUG("meanError = " << meanError_
              << ", varianceError = " << varianceError_);

    CPPUNIT_ASSERT(meanError_ < 0.04);
    CPPUNIT_ASSERT(varianceError_ < 0.1);

    double avgErrorMean = maths::CBasicStatistics::mean(avgError);
    double avgErrorStd = ::sqrt(maths::CBasicStatistics::variance(avgError));
    LOG_DEBUG("avgErrorMean = " << avgErrorMean
              << ", avgErrorStd = " << avgErrorStd);

    CPPUNIT_ASSERT(avgErrorStd / avgErrorMean < 0.5);
}

void CSeasonalComponentAdaptiveBucketingTest::testPropagateForwardsByTime(void)
{
    LOG_DEBUG("+------------------------------------------------------------------------+");
    LOG_DEBUG("|  CSeasonalComponentAdaptiveBucketingTest::testPropagateForwardsByTime  |");
    LOG_DEBUG("+------------------------------------------------------------------------+");

    // Check no error is introduced by the aging process to
    // the bucket values and that the rate at which the total
    // count is reduced uniformly.

    maths::CDiurnalTime time(0, 0, 100, 100);
    maths::CSeasonalComponentAdaptiveBucketing bucketing(time, 0.2);

    bucketing.initialize(0.0, 100.0, 10u);
    for (std::size_t p = 0; p < 10; ++p)
    {
        for (std::size_t i = 0u; i < 100; ++i)
        {
            core_t::TTime x = static_cast<core_t::TTime>(100 * p + i);
            double y = 0.02 * (static_cast<double>(i) - 50.0)
                            * (static_cast<double>(i) - 50.0);
            bucketing.add(x, y);
        }
        bucketing.refine(static_cast<core_t::TTime>(100 * (p + 1)));
        bucketing.propagateForwardsByTime(1.0);
    }

    double lastCount = bucketing.count();
    TDoubleVec errors;
    {
        const TFloatVec &endpoints = bucketing.endpoints();
        TDoubleVec values = bucketing.values(1100);
        LOG_DEBUG("endpoints = " << core::CContainerPrinter::print(endpoints));
        LOG_DEBUG("values    = " << core::CContainerPrinter::print(values));
        for (std::size_t i = 0u; i < values.size(); ++i)
        {
            double a = endpoints[i] - 50.0;
            double b = endpoints[i+1] - 50.0;

            // Function mean.
            double m = ::fabs(a) < ::fabs(b) ?
                       0.02 / 3.0 * b * b * b
                       * (1.0 - (a/b) * (a/b) * (a/b)) / (b-a) :
                       0.02 / 3.0 * a * a * a
                       * ((b/a) * (b/a) * (b/a) - 1.0) / (b-a);
            double v = values[i];
            errors.push_back(::fabs(v - m));
        }
        LOG_DEBUG("errors = " << core::CContainerPrinter::print(errors));
    }

    for (std::size_t i = 0u; i < 20; ++i)
    {
        bucketing.propagateForwardsByTime(1.0);

        const TFloatVec &endpoints = bucketing.endpoints();
        TDoubleVec values = bucketing.values(1100);
        LOG_DEBUG("endpoints = " << core::CContainerPrinter::print(endpoints));
        LOG_DEBUG("values = " << core::CContainerPrinter::print(values));

        for (std::size_t j = 0u; j < values.size(); ++j)
        {
            double a = endpoints[j] - 50.0;
            double b = endpoints[j+1] - 50.0;

            // Function mean.
            double m = ::fabs(a) < ::fabs(b) ?
                       0.02 / 3.0 * b * b * b
                       * (1.0 - (a/b) * (a/b) * (a/b)) / (b-a) :
                       0.02 / 3.0 * a * a * a
                       * ((b/a) * (b/a) * (b/a) - 1.0) / (b-a);
            double v = values[j];
            LOG_DEBUG("v = " << v
                      << ", m = " << m
                      << ", error = " << ::fabs(v - m));

            CPPUNIT_ASSERT(::fabs(v - m) <= 0.4 + errors[j]);
        }

        double count = bucketing.count();
        LOG_DEBUG("count = " << count
                  << ", lastCount = " << lastCount
                  << " count/lastCount = " << count/lastCount);
        CPPUNIT_ASSERT(count < lastCount);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(0.81873, count/lastCount, 5e-6);
        lastCount = count;
    }
}

void CSeasonalComponentAdaptiveBucketingTest::testMinimumBucketLength(void)
{
    LOG_DEBUG("+--------------------------------------------------------------------+");
    LOG_DEBUG("|  CSeasonalComponentAdaptiveBucketingTest::testMinimumBucketLength  |");
    LOG_DEBUG("+--------------------------------------------------------------------+");

    const double bucketLength = 3600.0;
    const double function[] = { 0.0, 0.0, 10.0, 12.0, 11.0, 16.0, 15.0, 1.0, 0.0, 0.0 };
    std::size_t n = boost::size(function);

    test::CRandomNumbers rng;

    core_t::TTime period = static_cast<core_t::TTime>(n)
                         * static_cast<core_t::TTime>(bucketLength);
    maths::CDiurnalTime time(0, 0, period, period);
    maths::CSeasonalComponentAdaptiveBucketing bucketing1(time, 0.0, 0.0);
    maths::CSeasonalComponentAdaptiveBucketing bucketing2(time, 0.0, 2000.0);
    bucketing1.initialize(0.0, static_cast<double>(period), n);
    bucketing2.initialize(0.0, static_cast<double>(period), n);

    for (std::size_t i = 0u; i < 20; ++i)
    {
        for (std::size_t j = 0u; j < n; ++j)
        {
            TDoubleVec values;
            rng.generateNormalSamples(function[j], 1.0, 5, values);

            TDoubleVec times;
            rng.generateUniformSamples(0.0, bucketLength, values.size(), times);
            std::sort(times.begin(), times.end());

            for (std::size_t k = 0u; k < times.size(); ++k)
            {
                core_t::TTime t = static_cast<core_t::TTime>(i) * period
                                + static_cast<core_t::TTime>(static_cast<double>(j) * bucketLength)
                                + static_cast<core_t::TTime>(times[k]);
                bucketing1.add(t, values[k]);
                bucketing2.add(t, values[k]);
            }
        }
        bucketing1.refine(static_cast<core_t::TTime>(i) * period);
        bucketing2.refine(static_cast<core_t::TTime>(i) * period);

        const TFloatVec &endpoints1 = bucketing1.endpoints();
        const TFloatVec &endpoints2 = bucketing2.endpoints();

        CPPUNIT_ASSERT_EQUAL(endpoints1.size(), endpoints2.size());
        double minimumBucketLength1 = std::numeric_limits<double>::max();
        double minimumBucketLength2 = std::numeric_limits<double>::max();
        double minimumTotalError = 0.0;
        for (std::size_t j = 1u; j < endpoints1.size(); ++j)
        {
            minimumBucketLength1 = std::min(minimumBucketLength1,
                                            endpoints1[j] - endpoints1[j-1]);
            minimumBucketLength2 = std::min(minimumBucketLength2,
                                            endpoints2[j] - endpoints2[j-1]);
            double minimumShift = std::max(2000.0 - (endpoints1[j] - endpoints1[j-1]), 0.0) / 2.0;
            minimumTotalError += minimumShift * minimumShift;
        }
        LOG_DEBUG("minimumBucketLength1 = " << minimumBucketLength1);
        LOG_DEBUG("minimumBucketLength2 = " << minimumBucketLength2);
        CPPUNIT_ASSERT(minimumBucketLength2 >= 2000.0);

        double totalError = 0.0;
        for (std::size_t j = 1u; j+1 < endpoints1.size(); ++j)
        {
            double error = endpoints2[j] - endpoints1[j];
            totalError += error * error;
        }
        LOG_DEBUG("minimumTotalError = " << minimumTotalError);
        LOG_DEBUG("totalError        = " << totalError);
        CPPUNIT_ASSERT(totalError <= 4.5 * minimumTotalError);
    }
}

void CSeasonalComponentAdaptiveBucketingTest::testUnintialized(void)
{
    LOG_DEBUG("+-------------------------------------------------------------+");
    LOG_DEBUG("|  CSeasonalComponentAdaptiveBucketingTest::testUnintialized  |");
    LOG_DEBUG("+-------------------------------------------------------------+");

    // Check that all the functions work and return the expected
    // values on an uninitialized bucketing.

    maths::CDiurnalTime time(0, 0, 10, 10);
    maths::CSeasonalComponentAdaptiveBucketing bucketing(time, 0.1);

    bucketing.add(0, 1.0);
    bucketing.add(1, 2.0);

    CPPUNIT_ASSERT(!bucketing.initialized());
    bucketing.propagateForwardsByTime(1.0);
    bucketing.refine(10);
    TDoubleVec knots;
    TDoubleVec values;
    TDoubleVec variances;
    bucketing.knots(10, maths::CSplineTypes::E_Periodic, knots, values, variances);
    CPPUNIT_ASSERT(knots.empty());
    CPPUNIT_ASSERT(values.empty());
    CPPUNIT_ASSERT(variances.empty());
    CPPUNIT_ASSERT_EQUAL(0.0, bucketing.count());
    CPPUNIT_ASSERT(bucketing.endpoints().empty());
    CPPUNIT_ASSERT(bucketing.values(100).empty());
    CPPUNIT_ASSERT(bucketing.variances().empty());

    bucketing.initialize(0.0, 10.0, 10);
    CPPUNIT_ASSERT(bucketing.initialized());
    for (std::size_t i = 0u; i < 10; ++i)
    {
        core_t::TTime x = static_cast<core_t::TTime>(i);
        bucketing.add(x, static_cast<double>(x * x));
    }
    bucketing.clear();
    CPPUNIT_ASSERT(!bucketing.initialized());
    bucketing.knots(10, maths::CSplineTypes::E_Periodic, knots, values, variances);
    CPPUNIT_ASSERT(knots.empty());
    CPPUNIT_ASSERT(values.empty());
    CPPUNIT_ASSERT(variances.empty());
    CPPUNIT_ASSERT_EQUAL(0.0, bucketing.count());
    CPPUNIT_ASSERT(bucketing.endpoints().empty());
    CPPUNIT_ASSERT(bucketing.values(100).empty());
    CPPUNIT_ASSERT(bucketing.variances().empty());
}


void CSeasonalComponentAdaptiveBucketingTest::testKnots(void)
{
    LOG_DEBUG("+------------------------------------------------------+");
    LOG_DEBUG("|  CSeasonalComponentAdaptiveBucketingTest::testKnots  |");
    LOG_DEBUG("+------------------------------------------------------+");

    // Check prediction errors in values and variances.

    test::CRandomNumbers rng;

    LOG_DEBUG("*** Values ***");
    {
        maths::CDiurnalTime time(0, 0, 86400, 86400);
        maths::CSeasonalComponentAdaptiveBucketing bucketing(time, 0.1, 864.0);

        bucketing.initialize(0.0, 86400.0, 20);
        for (std::size_t p = 0; p < 5; ++p)
        {
            TDoubleVec noise;
            rng.generateNormalSamples(0.0, 4.0, 100, noise);

            for (std::size_t i = 0u; i < 100; ++i)
            {
                core_t::TTime x = static_cast<core_t::TTime>(p * 86400 + 864 * i);
                double y = 0.02 * (static_cast<double>(i) - 50.0)
                                * (static_cast<double>(i) - 50.0) + noise[i];
                bucketing.add(x, y);
            }
            bucketing.refine(static_cast<core_t::TTime>(86400 * (p + 1)));

            TDoubleVec knots;
            TDoubleVec values;
            TDoubleVec variances;
            bucketing.knots(static_cast<core_t::TTime>(86400 * (p + 1)),
                            maths::CSplineTypes::E_Periodic,
                            knots, values, variances);
            LOG_DEBUG("knots  = " << core::CContainerPrinter::print(knots));
            LOG_DEBUG("values = " << core::CContainerPrinter::print(values));

            TMeanAccumulator meanError;
            TMeanAccumulator meanValue;
            for (std::size_t i = 0u; i < knots.size(); ++i)
            {
                double x = knots[i] / 864.0;
                double expectedValue = 0.02 * (x - 50.0) * (x - 50.0);
                LOG_DEBUG("expected = " << expectedValue
                          << ", value = " << values[i]);
                CPPUNIT_ASSERT_DOUBLES_EQUAL(expectedValue, values[i], 15.0);
                meanError.add(::fabs(values[i] - expectedValue));
                meanValue.add(::fabs(expectedValue));
            }
            LOG_DEBUG("meanError = " << maths::CBasicStatistics::mean(meanError));
            LOG_DEBUG("meanValue = " << maths::CBasicStatistics::mean(meanValue));
            CPPUNIT_ASSERT(  maths::CBasicStatistics::mean(meanError)
                           / maths::CBasicStatistics::mean(meanValue) < 0.32 / static_cast<double>(p+1));
        }
    }
    LOG_DEBUG("*** Variances ***");
    {
        maths::CDiurnalTime time(0, 0, 86400, 86400);
        maths::CSeasonalComponentAdaptiveBucketing bucketing(time, 0.1, 864.0);

        bucketing.initialize(0.0, 86400.0, 20);
        for (std::size_t p = 0; p < 50; ++p)
        {
            TDoubleVec noise;

            for (std::size_t i = 0u; i < 100; ++i)
            {
                core_t::TTime x = static_cast<core_t::TTime>(p * 86400 + 864 * i);
                double v = 0.01 * (static_cast<double>(i) - 50.0)
                                * (static_cast<double>(i) - 50.0);
                rng.generateNormalSamples(0.0, v, 1, noise);
                bucketing.add(x, noise[0]);
            }
            bucketing.refine(static_cast<core_t::TTime>(86400 * (p + 1)));

            if ((p+1) % 10 == 0)
            {
                TDoubleVec knots;
                TDoubleVec values;
                TDoubleVec variances;
                bucketing.knots(static_cast<core_t::TTime>(86400 * (p + 1)),
                                maths::CSplineTypes::E_Periodic,
                                knots, values, variances);
                LOG_DEBUG("knots     = " << core::CContainerPrinter::print(knots));
                LOG_DEBUG("variances = " << core::CContainerPrinter::print(variances));

                TMeanAccumulator meanError;
                TMeanAccumulator meanVariance;
                for (std::size_t i = 0u; i < knots.size(); ++i)
                {
                    double x = knots[i] / 864.0;
                    double expectedVariance = 0.01 * (x - 50.0) * (x - 50.0);
                    LOG_DEBUG("expected = " << expectedVariance
                              << ", variance = " << variances[i]);
                    CPPUNIT_ASSERT_DOUBLES_EQUAL(expectedVariance,
                                                 variances[i],
                                                 15.0);
                    meanError.add(::fabs(variances[i] - expectedVariance));
                    meanVariance.add(::fabs(expectedVariance));
                }
                LOG_DEBUG("meanError    = " << maths::CBasicStatistics::mean(meanError));
                LOG_DEBUG("meanVariance = " << maths::CBasicStatistics::mean(meanVariance));
                CPPUNIT_ASSERT(  maths::CBasicStatistics::mean(meanError)
                               / maths::CBasicStatistics::mean(meanVariance)
                                     < 0.35 / ::sqrt(static_cast<double>((p+1))/10));
            }
        }
    }
}

void CSeasonalComponentAdaptiveBucketingTest::testLongTermTrendKnots(void)
{
    LOG_DEBUG("+-------------------------------------------------------------------+");
    LOG_DEBUG("|  CSeasonalComponentAdaptiveBucketingTest::testLongTermTrendKnots  |");
    LOG_DEBUG("+-------------------------------------------------------------------+");

    // Check prediction errors in values.

    test::CRandomNumbers rng;

    maths::CDiurnalTime time(0, 0, 86400, 86400);
    maths::CSeasonalComponentAdaptiveBucketing bucketing(time, 0.1, 864.0);
    maths::CSeasonalComponentAdaptiveBucketing::TTimeTimePrMeanVarPrVec empty;

    bucketing.initialize(0.0, 86400.0, 20);
    bucketing.initialValues(0, 0, empty);

    for (std::size_t p = 0; p < 100 ; ++p)
    {
        TDoubleVec noise;
        rng.generateNormalSamples(0.0, 100.0, 144, noise);

        for (std::size_t i = 0u; i < 144; ++i)
        {
            double x = static_cast<double>(i) / 144.0;
            double y = 10.0 * (  std::min(static_cast<double>(p+1) + x, 50.0)
                               - std::max(static_cast<double>(p+1) + x - 50.0, 0.0)
                               + 10.0 * ::sin(boost::math::double_constants::two_pi * x));
            bucketing.add(static_cast<core_t::TTime>(86400 * p + 600 * i), y + noise[i]);
        }
        bucketing.refine(static_cast<core_t::TTime>(86400 * (p + 1)));
        bucketing.propagateForwardsByTime(1.0);

        if (p > 14 && (p + 1) % 5 == 0)
        {
            TDoubleVec knots;
            TDoubleVec values;
            TDoubleVec variances;
            bucketing.knots(static_cast<core_t::TTime>(86400 * (p+1)),
                            maths::CSplineTypes::E_Periodic,
                            knots, values, variances);
            LOG_DEBUG("knots     = " << core::CContainerPrinter::print(knots));
            LOG_DEBUG("values = " << core::CContainerPrinter::print(values));
            LOG_DEBUG("variances = " << core::CContainerPrinter::print(variances));

            TMeanAccumulator meanError;
            TMeanAccumulator meanValue;
            for (std::size_t i = 0u; i < knots.size(); ++i)
            {
                double x = knots[i] / 86400.0;
                double expectedValue = 10.0 * (  std::min(static_cast<double>(p+1) + x, 50.0)
                                               - std::max(static_cast<double>(p+1) + x - 50.0, 0.0)
                                               + 10.0 * ::sin(boost::math::double_constants::two_pi * x));
                LOG_DEBUG("expected = " << expectedValue << ", value = " << values[i]);
                CPPUNIT_ASSERT_DOUBLES_EQUAL(expectedValue, values[i], 70.0);
                meanError.add(::fabs(values[i] - expectedValue));
                meanValue.add(::fabs(expectedValue));
            }
            LOG_DEBUG("meanError = " << maths::CBasicStatistics::mean(meanError));
            LOG_DEBUG("meanValue = " << maths::CBasicStatistics::mean(meanValue));
            CPPUNIT_ASSERT(  maths::CBasicStatistics::mean(meanError)
                           / maths::CBasicStatistics::mean(meanValue) < 0.15);
        }
    }
}

void CSeasonalComponentAdaptiveBucketingTest::testShiftValue(void)
{
    LOG_DEBUG("+-----------------------------------------------------------+");
    LOG_DEBUG("|  CSeasonalComponentAdaptiveBucketingTest::testShiftValue  |");
    LOG_DEBUG("+-----------------------------------------------------------+");

    // Test that applying a shift translates the predicted values
    // but doesn't alter the slope or predicted variances.

    maths::CDiurnalTime time(0, 0, 86400, 86400);
    maths::CSeasonalComponentAdaptiveBucketing bucketing(time, 0.1, 600.0);
    maths::CSeasonalComponentAdaptiveBucketing::TTimeTimePrMeanVarPrVec empty;

    bucketing.initialize(0.0, 86400.0, 20);
    bucketing.initialValues(0, 0, empty);

    core_t::TTime t = 0;
    for (/**/; t < 40 * 86400; t += 600)
    {
        double x = static_cast<double>(t) / 86400.0;
        double y = x + 20.0 + 20.0 * ::sin(boost::math::double_constants::two_pi * x);
        bucketing.add(t, y);
        if (t % 86400 == 0)
        {
            bucketing.refine(t);
            bucketing.propagateForwardsByTime(1.0);
        }
    }

    TDoubleVec knots1;
    TDoubleVec values1;
    TDoubleVec variances1;
    bucketing.knots(t + 7*86400, maths::CSplineTypes::E_Natural, knots1, values1, variances1);

    bucketing.shiftValue(20.0);

    TDoubleVec knots2;
    TDoubleVec values2;
    TDoubleVec variances2;
    bucketing.knots(t + 7*86400, maths::CSplineTypes::E_Natural, knots2, values2, variances2);

    CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(knots1), core::CContainerPrinter::print(knots2));
    CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(variances1), core::CContainerPrinter::print(variances2));

    for (std::size_t i = 0u; i < values1.size(); ++i)
    {
        LOG_DEBUG("values = " << values1[i] << " vs " << values2[i]);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(20.0 + values1[i], values2[i], 1e-6 * values1[i]);
    }
}

void CSeasonalComponentAdaptiveBucketingTest::testPersist(void)
{
    LOG_DEBUG("+--------------------------------------------------------+");
    LOG_DEBUG("|  CSeasonalComponentAdaptiveBucketingTest::testPersist  |");
    LOG_DEBUG("+--------------------------------------------------------+");

    // Check that serialization is idempotent.

    double decayRate = 0.1;
    double minimumBucketLength = 1.0;

    maths::CDiurnalTime time(0, 0, 86400, 86400);
    maths::CSeasonalComponentAdaptiveBucketing bucketing(time, decayRate, minimumBucketLength);

    bucketing.initialize(0.0, 86400.0, 10);
    for (std::size_t p = 0; p < 10; ++p)
    {
        for (std::size_t i = 0u; i < 100; ++i)
        {
            core_t::TTime x = static_cast<core_t::TTime>(p * 86400 + 864 * i);
            double y = 0.02 * (static_cast<double>(i) - 50.0)
                            * (static_cast<double>(i) - 50.0);
            bucketing.add(x, y);
        }
        bucketing.refine(static_cast<core_t::TTime>(86400 * (p + 1)));
    }

    uint64_t checksum = bucketing.checksum();

    std::string origXml;
    {
        core::CRapidXmlStatePersistInserter inserter("root");
        bucketing.acceptPersistInserter(inserter);
        inserter.toXml(origXml);
    }

    LOG_DEBUG("Bucketing XML representation:\n" << origXml);

    core::CRapidXmlParser parser;
    CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(origXml));
    core::CRapidXmlStateRestoreTraverser traverser(parser);

    // Restore the XML into a new bucketing.
    maths::CSeasonalComponentAdaptiveBucketing restoredBucketing(decayRate + 0.1,
                                                                 minimumBucketLength,
                                                                 traverser);

    LOG_DEBUG("orig checksum = " << checksum
              << " restored checksum = " << restoredBucketing.checksum());
    CPPUNIT_ASSERT_EQUAL(checksum, restoredBucketing.checksum());

    // The XML representation of the new bucketing should be the
    // same as the original.
    std::string newXml;
    {
        core::CRapidXmlStatePersistInserter inserter("root");
        restoredBucketing.acceptPersistInserter(inserter);
        inserter.toXml(newXml);
    }
    CPPUNIT_ASSERT_EQUAL(origXml, newXml);
}

CppUnit::Test *CSeasonalComponentAdaptiveBucketingTest::suite(void)
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CSeasonalComponentAdaptiveBucketingTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CSeasonalComponentAdaptiveBucketingTest>(
                                   "CSeasonalComponentAdaptiveBucketingTest::testInitialize",
                                   &CSeasonalComponentAdaptiveBucketingTest::testInitialize) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CSeasonalComponentAdaptiveBucketingTest>(
                                   "CSeasonalComponentAdaptiveBucketingTest::testSwap",
                                   &CSeasonalComponentAdaptiveBucketingTest::testSwap) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CSeasonalComponentAdaptiveBucketingTest>(
                                   "CSeasonalComponentAdaptiveBucketingTest::testRefine",
                                   &CSeasonalComponentAdaptiveBucketingTest::testRefine) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CSeasonalComponentAdaptiveBucketingTest>(
                                   "CSeasonalComponentAdaptiveBucketingTest::testPropagateForwardsByTime",
                                   &CSeasonalComponentAdaptiveBucketingTest::testPropagateForwardsByTime) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CSeasonalComponentAdaptiveBucketingTest>(
                                   "CSeasonalComponentAdaptiveBucketingTest::testMinimumBucketLength",
                                   &CSeasonalComponentAdaptiveBucketingTest::testMinimumBucketLength) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CSeasonalComponentAdaptiveBucketingTest>(
                                   "CSeasonalComponentAdaptiveBucketingTest::testUnintialized",
                                   &CSeasonalComponentAdaptiveBucketingTest::testUnintialized) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CSeasonalComponentAdaptiveBucketingTest>(
                                   "CSeasonalComponentAdaptiveBucketingTest::testKnots",
                                   &CSeasonalComponentAdaptiveBucketingTest::testKnots) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CSeasonalComponentAdaptiveBucketingTest>(
                                   "CSeasonalComponentAdaptiveBucketingTest::testLongTermTrendKnots",
                                   &CSeasonalComponentAdaptiveBucketingTest::testLongTermTrendKnots) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CSeasonalComponentAdaptiveBucketingTest>(
                                   "CSeasonalComponentAdaptiveBucketingTest::testShiftValue",
                                   &CSeasonalComponentAdaptiveBucketingTest::testShiftValue) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CSeasonalComponentAdaptiveBucketingTest>(
                                   "CSeasonalComponentAdaptiveBucketingTest::testPersist",
                                   &CSeasonalComponentAdaptiveBucketingTest::testPersist) );

    return suiteOfTests;
}
