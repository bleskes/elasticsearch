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

#include "CTrendTestsTest.h"

#include <core/CoreTypes.h>
#include <core/CLogger.h>
#include <core/CRapidXmlParser.h>
#include <core/CRapidXmlStatePersistInserter.h>
#include <core/CRapidXmlStateRestoreTraverser.h>

#include <maths/CTimeSeriesTestData.h>
#include <maths/CTrendTests.h>

#include <test/CRandomNumbers.h>

#include <boost/bind.hpp>
#include <boost/math/constants/constants.hpp>
#include <boost/range.hpp>
#include <boost/ref.hpp>
#include <boost/scoped_ptr.hpp>

#include <vector>


using namespace prelert;

namespace
{

typedef std::vector<double> TDoubleVec;
typedef std::pair<core_t::TTime, double> TTimeDoublePr;
typedef std::vector<TTimeDoublePr> TTimeDoublePrVec;
typedef maths::CTrendTests::TIntTimePr TIntTimePr;

static const core_t::TTime HALF_HOUR = 1800;
static const core_t::TTime DAY = 86400;
static const core_t::TTime WEEK = 604800;

double constant(core_t::TTime /*time*/)
{
    return 0.0;
}

double ramp(core_t::TTime time)
{
    return 0.1 * static_cast<double>(time) / static_cast<double>(WEEK);
}

double markov(core_t::TTime time)
{
    static double state = 0.2;
    if (time % WEEK == 0)
    {
        core::CHashing::CMurmurHash2BT<core_t::TTime> hasher;
        state =  2.0 * static_cast<double>(hasher(time))
                     / static_cast<double>(std::numeric_limits<std::size_t>::max());
    }
    return state;
}

double smoothDaily(core_t::TTime time)
{
    return ::sin(  boost::math::double_constants::two_pi
                 * static_cast<double>(time)
                 / static_cast<double>(DAY));
}

double smoothWeekly(core_t::TTime time)
{
    return ::sin(  boost::math::double_constants::two_pi
                 * static_cast<double>(time)
                 / static_cast<double>(WEEK));
}

double spikeyDaily(core_t::TTime time)
{
    double pattern[] =
        {
            1.0, 0.1, 0.1, 0.1, 0.1, 0.2, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.2,
            0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.1
        };
    return pattern[(time % DAY) / HALF_HOUR];
}

double spikeyWeekly(core_t::TTime time)
{
    double pattern[] =
        {
            1.0, 0.1, 0.1, 0.1, 0.1, 0.2, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.2,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.2,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.1,
            0.0, 0.1, 0.1, 0.1, 0.1, 0.2, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.2,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.2,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.1,
            0.0, 0.1, 0.1, 0.1, 0.1, 0.2, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.2,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.2,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.1,
            0.0, 0.1, 0.1, 0.1, 0.1, 0.2, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.2,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.2,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.1,
            0.0, 0.1, 0.1, 0.1, 0.1, 0.2, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.2,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.2,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.1,
            0.0, 0.1, 0.1, 0.1, 0.1, 0.2, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.2,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.2,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.1,
            0.0, 0.1, 0.1, 0.1, 0.1, 0.2, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.2,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.2,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1.0, 0.1, 0.1, 0.1, 0.1, 0.1
        };
    return pattern[(time % WEEK) / HALF_HOUR];
}

double weekends(core_t::TTime time)
{
    double amplitude[] = { 1.0, 0.9, 0.9, 0.9, 1.0, 0.2, 0.1 };
    return amplitude[(time % WEEK) / DAY]
           * ::sin(  boost::math::double_constants::two_pi
                   * static_cast<double>(time)
                   / static_cast<double>(DAY));
}

}

void CTrendTestsTest::testTrend(void)
{
    LOG_DEBUG("+------------------------------+");
    LOG_DEBUG("|  CTrendTestsTest::testTrend  |");
    LOG_DEBUG("+------------------------------+");

    // Test that there is a high probability that a stationary
    // time series tests negative for having a trend and a high
    // probability that a time series with a trend tests positive
    // for having a trend.
    //
    // For the sake of concreteness, in the following test we
    // assume that the null hypothesis is that the series has
    // no trend. In the first test case the time series have
    // no trend so we are testing probability of type I error.
    // In the second test case the time series have a significant
    // trend so we are testing the probability of type II error.

    test::CRandomNumbers rng;

    double pError[2] = {};
    double power = 0.0;

    for (std::size_t trend = 0u; trend < 2; ++trend)
    {
        LOG_DEBUG("*** trend = " << trend << " ***");

        std::size_t trendCount = 0u;
        std::size_t noTrendCount = 0u;
        for (std::size_t test = 0u; test < 100; ++test)
        {
            TDoubleVec samples;
            rng.generateNormalSamples(1.0, 5.0, 600, samples);
            double scale = 3.0 * sqrt(5.0);

            maths::CTrendTests::CTrend trendTest;
            for (std::size_t i = 0u; i < samples.size(); ++i)
            {
                double x = (trend == 0 ?
                            0.0 : scale * ::sin(boost::math::double_constants::two_pi
                                                * static_cast<double>(i) / 100.0))
                           + samples[i];
                trendTest.add(x);
                if (i > 99)
                {
                    switch (trendTest.hasTrend())
                    {
                    case maths::CTrendTests::E_False: ++noTrendCount; break;
                    case maths::CTrendTests::E_True: ++trendCount; break;
                    case maths::CTrendTests::E_Undetermined: break;
                    }
                }
            }
        }

        LOG_DEBUG("trendCount = " << trendCount
                  << ", noTrendCount = " << noTrendCount
                  << ", undetermined = " << (50000 - trendCount - noTrendCount));

        pError[trend] = static_cast<double>(trend == 0 ?
                                            trendCount :
                                            noTrendCount) / 50000.0;
        if (trend == 1)
        {
            power = double(trendCount) / 50000.0;
        }
    }

    LOG_DEBUG("[P(type I error), P(type II error)] = "
              << core::CContainerPrinter::print(pError));
    LOG_DEBUG("power = " << power);
    CPPUNIT_ASSERT(pError[0] < 1e-4);
    CPPUNIT_ASSERT(pError[1] < 1e-4);
    CPPUNIT_ASSERT(power > 0.99);

    for (std::size_t trend = 0u; trend < 2; ++trend)
    {
        LOG_DEBUG("*** trend = " << trend << " ***");

        std::size_t trendCount = 0u;
        std::size_t noTrendCount = 0u;
        for (std::size_t t = 0u; t < 100; ++t)
        {
            TDoubleVec samples;
            rng.generateGammaSamples(50.0, 2.0, 600, samples);
            double scale = 3.0 * ::sqrt(200.0);

            maths::CTrendTests::CTrend trendTest;
            for (std::size_t i = 0u; i < samples.size(); ++i)
            {
                double x = (trend == 0 ?
                            0.0 : scale * ::sin(boost::math::double_constants::two_pi
                                                * static_cast<double>(i) / 100.0))
                           + samples[i];
                trendTest.add(x);
                if (i > 99)
                {
                    switch (trendTest.hasTrend())
                    {
                    case maths::CTrendTests::E_False: ++noTrendCount; break;
                    case maths::CTrendTests::E_True: ++trendCount; break;
                    case maths::CTrendTests::E_Undetermined: break;
                    }
                }
            }
        }

        LOG_DEBUG("trendCount = " << trendCount
                  << ", noTrendCount = " << noTrendCount
                  << ", undetermined = " << (50000 - trendCount - noTrendCount));

        pError[trend] = static_cast<double>(trend == 0 ?
                                            trendCount :
                                            noTrendCount) / 50000.0;
        if (trend == 1)
        {
            power = double(trendCount) / 50000.0;
        }
    }

    LOG_DEBUG("[P(type I error), P(type II error)] = "
              << core::CContainerPrinter::print(pError));
    LOG_DEBUG("power = " << power);
    CPPUNIT_ASSERT(pError[0] < 1e-4);
    CPPUNIT_ASSERT(pError[1] < 1e-4);
    CPPUNIT_ASSERT(power > 0.99);

    for (std::size_t trend = 0u; trend < 2; ++trend)
    {
        LOG_DEBUG("*** trend = " << trend << " ***");

        std::size_t trendCount = 0u;
        std::size_t noTrendCount = 0u;
        for (std::size_t t = 0u; t < 100; ++t)
        {
            TDoubleVec samples;
            rng.generateLogNormalSamples(2.5, 1.0, 600, samples);
            double scale = 3.0 * ::sqrt(693.20);

            maths::CTrendTests::CTrend trendTest;
            for (std::size_t i = 0u; i < samples.size(); ++i)
            {
                double x = (trend == 0 ?
                            0.0 : scale * ::sin(boost::math::double_constants::two_pi
                                                * static_cast<double>(i) / 100.0))
                           + samples[i];
                trendTest.add(x);
                if (i > 99)
                {
                    switch (trendTest.hasTrend())
                    {
                    case maths::CTrendTests::E_False: ++noTrendCount; break;
                    case maths::CTrendTests::E_True: ++trendCount; break;
                    case maths::CTrendTests::E_Undetermined: break;
                    }
                }
            }
        }

        LOG_DEBUG("trendCount = " << trendCount
                  << ", noTrendCount = " << noTrendCount
                  << ", undetermined = " << (50000 - trendCount - noTrendCount));

        pError[trend] = static_cast<double>(trend == 0 ?
                                            trendCount :
                                            noTrendCount) / 50000.0;
        if (trend == 1)
        {
            power = double(trendCount) / 50000.0;
        }
    }

    LOG_DEBUG("[P(type I error), P(type II error)] = "
              << core::CContainerPrinter::print(pError));
    LOG_DEBUG("power = " << power);
    CPPUNIT_ASSERT(pError[0] < 2e-3);
    CPPUNIT_ASSERT(pError[1] < 1e-4);
    CPPUNIT_ASSERT(power > 0.98);
}

void CTrendTestsTest::testRandomizedPeriodicity(void)
{
    LOG_DEBUG("+----------------------------------------------+");
    LOG_DEBUG("|  CTrendTestsTest::testRandomizedPeriodicity  |");
    LOG_DEBUG("+----------------------------------------------+");

    typedef maths::CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;

    test::CRandomNumbers rng;

    TMeanAccumulator typeI;
    TMeanAccumulator typeII;
    for (std::size_t t = 0u; t < 5; ++t)
    {
        LOG_DEBUG("*** test = " << t << " ***");

        typedef maths::CBasicStatistics::SSampleMeanVar<double>::TAccumulator TMeanVarAccumulator;
        typedef maths::CBasicStatistics::COrderStatisticsStack<double, 1, std::greater<double> > TMaxAccumulator;
        typedef double (*TFunction)(core_t::TTime);

        core_t::TTime time = 0;
        core_t::TTime day = 0;

        TDoubleVec samples;
        rng.generateLogNormalSamples(1.0, 4.0, 84000, samples);

        maths::CTrendTests::CRandomizedPeriodicity::reset();

        maths::CTrendTests::CRandomizedPeriodicity rtests[8];
        double falsePositives[3] = { 0.0, 0.0, 0.0 };
        double trueNegatives[3]  = { 0.0, 0.0, 0.0 };
        double truePositives[5]  = { 0.0, 0.0, 0.0, 0.0, 0.0 };
        double falseNegatives[5] = { 0.0, 0.0, 0.0, 0.0, 0.0 };
        TMeanVarAccumulator timeToDetectionMoments[5];
        TMaxAccumulator timeToDetectionMax[5];
        core_t::TTime lastTruePositive[5] = { time, time, time, time, time };
        TFunction functions[] =
            {
                &constant,
                &ramp,
                &markov,
                &smoothDaily,
                &smoothWeekly,
                &spikeyDaily,
                &spikeyWeekly,
                &weekends
            };

        for (std::size_t i = 0u; i < samples.size(); ++i)
        {
            for (std::size_t j = 0u; j < boost::size(functions); ++j)
            {
                rtests[j].add(time, 600.0 * (functions[j])(time) + samples[i]);
            }
            if (time >= day + DAY)
            {
                for (std::size_t j = 0u; j < boost::size(rtests); ++j)
                {
                    if (j < 3)
                    {
                        (rtests[j].test() ? falsePositives[j] : trueNegatives[j]) += 1.0;
                    }
                    else
                    {
                        (rtests[j].test() ? truePositives[j - 3] : falseNegatives[j - 3]) += 1.0;
                        if (rtests[j].test())
                        {
                            timeToDetectionMoments[j - 3].add(time - lastTruePositive[j - 3]);
                            timeToDetectionMax[j - 3].add(static_cast<double>(time - lastTruePositive[j - 3]));
                            lastTruePositive[j - 3] = time;
                        }
                    }
                }
                day += DAY;
            }
            time += HALF_HOUR;
        }

        LOG_DEBUG("falsePositives = " << core::CContainerPrinter::print(falsePositives));
        LOG_DEBUG("trueNegatives = " << core::CContainerPrinter::print(trueNegatives));
        for (std::size_t i = 0u; i < boost::size(falsePositives); ++i)
        {
            CPPUNIT_ASSERT(falsePositives[i] / trueNegatives[i] < 0.1);
            typeI.add(falsePositives[i] / trueNegatives[i]);
        }
        LOG_DEBUG("truePositives = " << core::CContainerPrinter::print(truePositives));
        LOG_DEBUG("falseNegatives = " << core::CContainerPrinter::print(falseNegatives));
        for (std::size_t i = 0u; i < boost::size(falsePositives); ++i)
        {
            CPPUNIT_ASSERT(falseNegatives[i] / truePositives[i] < 0.2);
            typeII.add(falseNegatives[i] / truePositives[i]);
        }

        for (std::size_t i = 0u; i < boost::size(timeToDetectionMoments); ++i)
        {
            LOG_DEBUG("time to detect moments = " << timeToDetectionMoments[i]);
            LOG_DEBUG("maximum time to detect = " << timeToDetectionMax[i][0]);
            CPPUNIT_ASSERT(maths::CBasicStatistics::mean(timeToDetectionMoments[i]) < 1.5 * DAY);
            CPPUNIT_ASSERT(::sqrt(maths::CBasicStatistics::variance(timeToDetectionMoments[i])) < 5 * DAY);
            CPPUNIT_ASSERT(timeToDetectionMax[i][0] <= 27 * WEEK);
        }
    }
    LOG_DEBUG("type I  = " << maths::CBasicStatistics::mean(typeI));
    LOG_DEBUG("type II = " << maths::CBasicStatistics::mean(typeII));
    CPPUNIT_ASSERT(maths::CBasicStatistics::mean(typeI) < 0.015);
    CPPUNIT_ASSERT(maths::CBasicStatistics::mean(typeII) < 0.05);
}

void CTrendTestsTest::testPeriodicity(void)
{
    LOG_DEBUG("+------------------------------------+");
    LOG_DEBUG("|  CTrendTestsTest::testPeriodicity  |");
    LOG_DEBUG("+------------------------------------+");

    typedef maths::CTrendTests::CPeriodicity::CResult TResult;

    test::CRandomNumbers rng;

    TResult notPeriodic;
    TResult dailyPeriodic;
    TResult weeklyPeriodic;
    TResult weekendDailyPeriodic;
    TResult weekendWeeklyPeriodic;
    TResult weekdayDailyPeriodic;
    dailyPeriodic.addIf(        true, maths::CTrendTests::SELECT_WEEK,     maths::CTrendTests::SELECT_DAILY);
    weeklyPeriodic.addIf(       true, maths::CTrendTests::SELECT_WEEK,     maths::CTrendTests::SELECT_WEEKLY);
    weekendDailyPeriodic.addIf( true, maths::CTrendTests::SELECT_WEEKEND,  maths::CTrendTests::SELECT_DAILY);
    weekendWeeklyPeriodic.addIf(true, maths::CTrendTests::SELECT_WEEKEND,  maths::CTrendTests::SELECT_WEEKLY);
    weekdayDailyPeriodic.addIf( true, maths::CTrendTests::SELECT_WEEKDAYS, maths::CTrendTests::SELECT_DAILY);

    LOG_DEBUG("");
    LOG_DEBUG("*** Synthetic: no periods ***");
    {
        TDoubleVec samples;
        rng.generateNormalSamples(1.0, 5.0, 16128, samples);

        boost::scoped_ptr<maths::CTrendTests::CPeriodicity> test(maths::CTrendTests::dailyAndWeekly(300));

        core_t::TTime time = 0;
        core_t::TTime day = DAY;
        core_t::TTime week = 13 * DAY;

        for (std::size_t i = 0u; i < samples.size(); ++i)
        {
            if (time > day && time < 13 * DAY)
            {
                TResult result = test->test();
                LOG_DEBUG("detected = " << maths::CTrendTests::printDailyAndWeekly(result)
                          << ", time = " << result.startOfPartition());
                CPPUNIT_ASSERT(notPeriodic == result);
                day += DAY;
            }
            if (time >= week)
            {
                TResult result = test->test();
                LOG_DEBUG("detected = " << maths::CTrendTests::printDailyAndWeekly(result)
                          << ", time = " << result.startOfPartition());
                CPPUNIT_ASSERT(notPeriodic == result);
                week += WEEK;
            }
            test->add(time, samples[i]);
            time += HALF_HOUR;
        }
    }

    LOG_DEBUG("");
    LOG_DEBUG("*** Synthetic: one period ***");
    {
        TDoubleVec samples;
        rng.generateNormalSamples(1.0, 5.0, 4032, samples);

        boost::scoped_ptr<maths::CTrendTests::CPeriodicity> test(maths::CTrendTests::dailyAndWeekly(300));

        core_t::TTime time = 0;
        core_t::TTime day  = 3 * DAY;
        core_t::TTime week = 13 * DAY;

        for (std::size_t i = 0u; i < samples.size(); ++i)
        {
            if (time > day && time < 13 * DAY)
            {
                TResult result = test->test();
                LOG_DEBUG("detected = " << maths::CTrendTests::printDailyAndWeekly(result)
                          << ", time = " << result.startOfPartition());
                CPPUNIT_ASSERT(dailyPeriodic == result);
                day += DAY;
            }
            if (time >= week)
            {
                TResult result = test->test();
                LOG_DEBUG("detected = " << maths::CTrendTests::printDailyAndWeekly(result)
                          << ", time = " << result.startOfPartition());
                CPPUNIT_ASSERT(dailyPeriodic == result);
                week += WEEK;
            }
            double x = 2.0 * ::sqrt(5.0) * sin(  static_cast<double>(i)
                                               / 48.0 * boost::math::double_constants::two_pi);
            test->add(time, x + samples[i]);
            time += HALF_HOUR;
        }

        CPPUNIT_ASSERT(dailyPeriodic == test->test());
    }

    LOG_DEBUG("");
    LOG_DEBUG("*** Synthetic: two periods weekday/weekend ***");
    {
        TDoubleVec samples;
        rng.generateNormalSamples(1.0, 5.0, 4032, samples);

        boost::scoped_ptr<maths::CTrendTests::CPeriodicity> test(maths::CTrendTests::dailyAndWeekly(300));

        core_t::TTime time = 0;
        core_t::TTime day  = 3 * DAY;
        core_t::TTime week = 13 * DAY;

        for (std::size_t i = 0u; i < samples.size(); ++i)
        {
            if (time > day && time < 13 * DAY)
            {
                TResult result = test->test();
                LOG_DEBUG("detected = " << maths::CTrendTests::printDailyAndWeekly(result)
                          << ", time = " << result.startOfPartition());
                CPPUNIT_ASSERT(dailyPeriodic == result);
                day += DAY;
            }
            if (time > week)
            {
                TResult result = test->test();
                LOG_DEBUG("detected = " << maths::CTrendTests::printDailyAndWeekly(result)
                          << ", time = " << result.startOfPartition());
                CPPUNIT_ASSERT_EQUAL(maths::CTrendTests::printDailyAndWeekly(  weekdayDailyPeriodic
                                                                             + weekendDailyPeriodic),
                                     maths::CTrendTests::printDailyAndWeekly(result));
                week += WEEK;
            }
            double scale = 1.0;
            switch (((time + DAY) % WEEK) / DAY)
            {
            case 0:
            case 1:
                scale = 0.1; break;
            default:
                break;
            }
            double x = 10.0 * ::sqrt(5.0) * sin(  static_cast<double>(i)
                                                / 48.0 * boost::math::double_constants::two_pi);
            test->add(time, scale * (x + samples[i]));
            time += HALF_HOUR;
        }

        CPPUNIT_ASSERT_EQUAL(maths::CTrendTests::printDailyAndWeekly(  weekdayDailyPeriodic
                                                                     + weekendDailyPeriodic),
                             maths::CTrendTests::printDailyAndWeekly(test->test()));
    }

    LOG_DEBUG("");
    LOG_DEBUG("*** Synthetic: weekly ***");
    {
        TDoubleVec samples;
        rng.generateNormalSamples(1.0, 5.0, 4032, samples);

        boost::scoped_ptr<maths::CTrendTests::CPeriodicity> test(maths::CTrendTests::dailyAndWeekly(300));

        core_t::TTime time = 0;
        core_t::TTime day  = DAY;
        core_t::TTime week = 13 * DAY;
        std::size_t errors = 0u;
        std::size_t calls  = 0u;

        for (std::size_t i = 0u; i < samples.size(); ++i)
        {
            if (time > day && time < 13 * DAY)
            {
                TResult result = test->test();
                LOG_DEBUG("detected = " << maths::CTrendTests::printDailyAndWeekly(result)
                          << ", time = " << result.startOfPartition());
                CPPUNIT_ASSERT(notPeriodic == result);
                day += DAY;
            }
            if (time >= week)
            {
                TResult result = test->test();
                LOG_DEBUG("detected = " << maths::CTrendTests::printDailyAndWeekly(result)
                          << ", time = " << result.startOfPartition());
                if (result != weeklyPeriodic)
                {
                    ++errors;
                }
                ++calls;
                week += WEEK;
            }
            double x = 3.0 * ::sqrt(5.0) * sin(  static_cast<double>(i)
                                               / 336.0 * boost::math::double_constants::two_pi);
            test->add(time, x + samples[i]);
            time += HALF_HOUR;
        }
        LOG_DEBUG("errors = " << static_cast<double>(errors) / static_cast<double>(calls));
        CPPUNIT_ASSERT(weeklyPeriodic == test->test());
        CPPUNIT_ASSERT_DOUBLES_EQUAL(0.0, static_cast<double>(errors) / static_cast<double>(calls), 0.1);
    }

    LOG_DEBUG("");
    LOG_DEBUG("*** EMC: daily ***");
    {
        TTimeDoublePrVec timeseries;
        core_t::TTime startTime;
        core_t::TTime endTime;
        CPPUNIT_ASSERT(maths::CTimeSeriesTestData::parse("testfiles/emc.csv",
                                                         timeseries,
                                                         startTime,
                                                         endTime,
                                                         maths::CTimeSeriesTestData::CSV_UNIX_REGEX));
        CPPUNIT_ASSERT(!timeseries.empty());

        LOG_DEBUG("timeseries = " << core::CContainerPrinter::print(timeseries.begin(),
                                                                    timeseries.begin() + 10)
                  << " ...");

        boost::scoped_ptr<maths::CTrendTests::CPeriodicity> test(maths::CTrendTests::dailyAndWeekly(300));

        core_t::TTime day  = startTime + 3 * DAY;
        core_t::TTime week = startTime + 13 * DAY;

        for (std::size_t i = 0u; i < timeseries.size(); ++i)
        {
            if (timeseries[i].first > day && timeseries[i].first < startTime + 13 * DAY)
            {
                TResult result = test->test();
                LOG_DEBUG("detected = " << maths::CTrendTests::printDailyAndWeekly(result)
                          << ", time = " << result.startOfPartition());
                CPPUNIT_ASSERT(dailyPeriodic == result);
                day += DAY;
            }
            if (timeseries[i].first >= week)
            {
                TResult result = test->test();
                LOG_DEBUG("detected = " << maths::CTrendTests::printDailyAndWeekly(result)
                          << ", time = " << result.startOfPartition());
                CPPUNIT_ASSERT(dailyPeriodic == result);
                week += WEEK;
            }
            test->add(timeseries[i].first, timeseries[i].second, 1.0 / 6.0);
        }

        CPPUNIT_ASSERT(dailyPeriodic == test->test());
    }

    LOG_DEBUG("");
    LOG_DEBUG("*** Bluecoat: daily and weekends ***");
    {
        TTimeDoublePrVec timeseries;
        core_t::TTime startTime;
        core_t::TTime endTime;
        CPPUNIT_ASSERT(maths::CTimeSeriesTestData::parse("testfiles/bluecoat.csv",
                                                         timeseries,
                                                         startTime,
                                                         endTime,
                                                         maths::CTimeSeriesTestData::CSV_UNIX_REGEX));
        CPPUNIT_ASSERT(!timeseries.empty());

        LOG_DEBUG("timeseries = " << core::CContainerPrinter::print(timeseries.begin(),
                                                                    timeseries.begin() + 10)
                  << " ...");

        boost::scoped_ptr<maths::CTrendTests::CPeriodicity> test(maths::CTrendTests::dailyAndWeekly(300));

        core_t::TTime day  = startTime + 5 * DAY;
        core_t::TTime week = startTime + 13 * DAY;

        for (std::size_t i = 0u; i < timeseries.size(); ++i)
        {
            if (timeseries[i].first > 2 * day && timeseries[i].first < startTime + 13 * DAY)
            {
                TResult result = test->test();
                LOG_DEBUG("detected = " << maths::CTrendTests::printDailyAndWeekly(result)
                          << ", time = " << result.startOfPartition());
                CPPUNIT_ASSERT(dailyPeriodic == result);
                day += DAY;
            }
            if (timeseries[i].first > week)
            {
                TResult result = test->test();
                LOG_DEBUG("detected = " << maths::CTrendTests::printDailyAndWeekly(result)
                          << ", time = " << result.startOfPartition());
                CPPUNIT_ASSERT_EQUAL(maths::CTrendTests::printDailyAndWeekly(  weekdayDailyPeriodic
                                                                             + weekendDailyPeriodic
                                                                             + weekendWeeklyPeriodic),
                                     maths::CTrendTests::printDailyAndWeekly(result));
                week += WEEK;
            }
            test->add(timeseries[i].first, timeseries[i].second, 1.0 / 6.0);
        }

        CPPUNIT_ASSERT_EQUAL(maths::CTrendTests::printDailyAndWeekly(  weekdayDailyPeriodic
                                                                     + weekendDailyPeriodic
                                                                     + weekendWeeklyPeriodic),
                             maths::CTrendTests::printDailyAndWeekly(test->test()));
    }

    LOG_DEBUG("");
    LOG_DEBUG("*** Zeppelin: no periods ***");
    {
        TTimeDoublePrVec timeseries;
        core_t::TTime startTime;
        core_t::TTime endTime;
        CPPUNIT_ASSERT(maths::CTimeSeriesTestData::parse("testfiles/zeppelin.csv",
                                                         timeseries,
                                                         startTime,
                                                         endTime,
                                                         maths::CTimeSeriesTestData::CSV_SPLUNK_REGEX,
                                                         maths::CTimeSeriesTestData::CSV_SPLUNK_DATE_FORMAT));
        CPPUNIT_ASSERT(!timeseries.empty());

        LOG_DEBUG("timeseries = " << core::CContainerPrinter::print(timeseries.begin(),
                                                                    timeseries.begin() + 10)
                  << " ...");

        boost::scoped_ptr<maths::CTrendTests::CPeriodicity> test(maths::CTrendTests::dailyAndWeekly(1800));

        core_t::TTime day  = startTime + DAY;
        core_t::TTime week = startTime + WEEK;

        for (std::size_t i = 0u; i < timeseries.size(); ++i)
        {
            if (timeseries[i].first > day && timeseries[i].first < startTime + 12 * DAY)
            {
                TResult result = test->test();
                LOG_DEBUG("detected = " << maths::CTrendTests::printDailyAndWeekly(result)
                          << ", time = " << result.startOfPartition());
                CPPUNIT_ASSERT(notPeriodic == result);
                day += DAY;
            }
            if (timeseries[i].first > week)
            {
                TResult result = test->test();
                LOG_DEBUG("detected = " << maths::CTrendTests::printDailyAndWeekly(result)
                          << ", time = " << result.startOfPartition());
                CPPUNIT_ASSERT(notPeriodic == result);
                week += WEEK;
            }
            test->add(timeseries[i].first, timeseries[i].second);
        }
    }

    LOG_DEBUG("");
    LOG_DEBUG("*** University of Michigan: daily weekly and weekends ***");
    {
        TTimeDoublePrVec timeseries;
        core_t::TTime startTime;
        core_t::TTime endTime;
        CPPUNIT_ASSERT(maths::CTimeSeriesTestData::parse("testfiles/umich.csv",
                                                         timeseries,
                                                         startTime,
                                                         endTime,
                                                         maths::CTimeSeriesTestData::CSV_SPLUNK_REGEX,
                                                         maths::CTimeSeriesTestData::CSV_SPLUNK_DATE_FORMAT));
        CPPUNIT_ASSERT(!timeseries.empty());

        LOG_DEBUG("timeseries = " << core::CContainerPrinter::print(timeseries.begin(),
                                                                    timeseries.begin() + 10)
                  << " ...");

        boost::scoped_ptr<maths::CTrendTests::CPeriodicity> test(maths::CTrendTests::dailyAndWeekly(1800));

        core_t::TTime day  = startTime + DAY;
        core_t::TTime week = startTime + 2 * WEEK;

        for (std::size_t i = 0u; i < timeseries.size(); ++i)
        {
            if (timeseries[i].first > day && timeseries[i].first < startTime + 7 * DAY)
            {
                if (timeseries[i].first > startTime + 3 * DAY)
                {
                    TResult result = test->test();
                    LOG_DEBUG("detected = " << maths::CTrendTests::printDailyAndWeekly(result)
                              << ", time = " << result.startOfPartition());
                    CPPUNIT_ASSERT(dailyPeriodic == result);
                }
                day += DAY;
            }
            if (timeseries[i].first > week)
            {
                TResult result = test->test();
                LOG_DEBUG("detected = " << maths::CTrendTests::printDailyAndWeekly(result)
                          << ", time = " << result.startOfPartition());
                CPPUNIT_ASSERT_EQUAL(maths::CTrendTests::printDailyAndWeekly(  weekdayDailyPeriodic
                                                                             + weekendWeeklyPeriodic),
                                     maths::CTrendTests::printDailyAndWeekly(result));
                week += WEEK;
            }
            test->add(timeseries[i].first, timeseries[i].second);
        }

        CPPUNIT_ASSERT_EQUAL(maths::CTrendTests::printDailyAndWeekly(  weekdayDailyPeriodic
                                                                     + weekendWeeklyPeriodic),
                             maths::CTrendTests::printDailyAndWeekly(test->test()));
    }
}

void CTrendTestsTest::testAutocorrelations(void)
{
    LOG_DEBUG("+-----------------------------------------+");
    LOG_DEBUG("|  CTrendTestsTest::testAutocorrelations  |");
    LOG_DEBUG("+-----------------------------------------+");

    typedef std::vector<std::size_t> TSizeVec;

    test::CRandomNumbers rng;

    TSizeVec sizes;
    rng.generateUniformSamples(10, 30, 100, sizes);

    for (std::size_t t = 0u; t < sizes.size(); ++t)
    {
        TDoubleVec values_;
        rng.generateUniformSamples(-10.0, 10.0, sizes[t], values_);

        maths::CTrendTests::TFloatMeanAccumulatorVec values(sizes[t]);
        for (std::size_t i = 0u; i < values_.size(); ++i)
        {
            values[i].add(values_[i]);
        }

        TDoubleVec expected;
        for (std::size_t offset = 1; offset < values.size(); ++offset)
        {
            expected.push_back(maths::CTrendTests::autocorrelation(offset, values));
        }

        TDoubleVec actual;
        maths::CTrendTests::autocorrelations(values, actual);

        if (t % 10 == 0)
        {
            LOG_DEBUG("expected = " << core::CContainerPrinter::print(expected));
            LOG_DEBUG("actual   = " << core::CContainerPrinter::print(actual));
        }
        CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(expected),
                             core::CContainerPrinter::print(actual));
    }
}

void CTrendTestsTest::testPersist(void)
{
    LOG_DEBUG("+--------------------------------+");
    LOG_DEBUG("|  CTrendTestsTest::testPersist  |");
    LOG_DEBUG("+--------------------------------+");

    // Check that serialization is idempotent.

    {
        const core_t::TTime halfHour = 1800;
        const core_t::TTime day = 86400;
        const core_t::TTime week = 604800;

        TDoubleVec timeseries;
        for (core_t::TTime time = 0; time < 2 * week + 1; time += halfHour)
        {
            double daily = 15.0 + 10.0 * ::sin(boost::math::double_constants::two_pi
                                               * static_cast<double>(time)
                                               / static_cast<double>(day));
            timeseries.push_back(daily);
        }

        test::CRandomNumbers rng;
        TDoubleVec noise;
        rng.generateNormalSamples(20.0, 16.0, timeseries.size(), noise);

        maths::CTrendTests::CTrend origTrendTest;
        for (std::size_t i = 0u; i < timeseries.size(); ++i)
        {
            origTrendTest.add(timeseries[i] + noise[i]);
        }

        std::string origXml;
        {
            core::CRapidXmlStatePersistInserter inserter("root");
            origTrendTest.acceptPersistInserter(inserter);
            inserter.toXml(origXml);
        }

        LOG_DEBUG("seasonal component XML representation:\n" << origXml);

        maths::CTrendTests::CTrend restoredTrendTest;
        {
            core::CRapidXmlParser parser;
            CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(origXml));
            core::CRapidXmlStateRestoreTraverser traverser(parser);
            CPPUNIT_ASSERT(traverser.traverseSubLevel(boost::bind(&maths::CTrendTests::CTrend::acceptRestoreTraverser,
                                                                  &restoredTrendTest,
                                                                  _1)));
        }
        CPPUNIT_ASSERT_EQUAL(origTrendTest.checksum(), restoredTrendTest.checksum());

        std::string newXml;
        {
            core::CRapidXmlStatePersistInserter inserter("root");
            restoredTrendTest.acceptPersistInserter(inserter);
            inserter.toXml(newXml);
        }
        CPPUNIT_ASSERT_EQUAL(origXml, newXml);
    }
    {
        // Test the CRandomizedPeriodic class
        maths::CTrendTests::CRandomizedPeriodicity test;
        for (core_t::TTime i = 1400000000; i < 1400050000; i += 5000)
        {
            test.add(i, 0.2);
        }

        std::string origXml;
        {
            core::CRapidXmlStatePersistInserter inserter("root");
            test.acceptPersistInserter(inserter);
            inserter.toXml(origXml);
        }

        std::string origStaticsXml;
        {
            core::CRapidXmlStatePersistInserter inserter("root");
            test.staticsAcceptPersistInserter(inserter);
            inserter.toXml(origStaticsXml);
        }

        // Check that the static state is also preserved
        uint64_t origNextRandom = test.ms_Rng();

        LOG_DEBUG("CRandomizedPeriodic XML representation:\n" << origXml);
        LOG_DEBUG("CRandomizedPeriodic statics XML representation:\n" <<  origStaticsXml);

        // Restore the XML into a new test
        maths::CTrendTests::CRandomizedPeriodicity test2;
        {
            core::CRapidXmlParser parser;
            CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(origXml));
            core::CRapidXmlStateRestoreTraverser traverser(parser);
            CPPUNIT_ASSERT(traverser.traverseSubLevel(boost::bind(&maths::CTrendTests::CRandomizedPeriodicity::acceptRestoreTraverser,
                                                                  &test2,
                                                                  _1)));
        }
        std::string newXml;
        {
            core::CRapidXmlStatePersistInserter inserter("root");
            test2.acceptPersistInserter(inserter);
            inserter.toXml(newXml);
        }
        CPPUNIT_ASSERT_EQUAL(origXml, newXml);

        {
            core::CRapidXmlParser parser;
            CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(origStaticsXml));
            core::CRapidXmlStateRestoreTraverser traverser(parser);
            CPPUNIT_ASSERT(traverser.traverseSubLevel(&maths::CTrendTests::CRandomizedPeriodicity::staticsAcceptRestoreTraverser));
        }
        std::string newStaticsXml;
        {
            core::CRapidXmlStatePersistInserter inserter("root");
            test2.staticsAcceptPersistInserter(inserter);
            inserter.toXml(newStaticsXml);
        }
        CPPUNIT_ASSERT_EQUAL(origStaticsXml, newStaticsXml);

        uint64_t newNextRandom = test2.ms_Rng();
        CPPUNIT_ASSERT_EQUAL(origNextRandom, newNextRandom);
    }
}

CppUnit::Test *CTrendTestsTest::suite(void)
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CTrendTestsTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CTrendTestsTest>(
                                   "CTrendTestsTest::testTrend",
                                   &CTrendTestsTest::testTrend) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTrendTestsTest>(
                                   "CTrendTestsTest::testRandomizedPeriodicity",
                                   &CTrendTestsTest::testRandomizedPeriodicity) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTrendTestsTest>(
                                   "CTrendTestsTest::testPeriodicity",
                                   &CTrendTestsTest::testPeriodicity) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTrendTestsTest>(
                                   "CTrendTestsTest::testAutocorrelations",
                                   &CTrendTestsTest::testAutocorrelations) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTrendTestsTest>(
                                   "CTrendTestsTest::testPersist",
                                   &CTrendTestsTest::testPersist) );

    return suiteOfTests;
}
