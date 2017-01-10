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


using namespace ml;

namespace
{

typedef std::vector<double> TDoubleVec;
typedef std::pair<core_t::TTime, double> TTimeDoublePr;
typedef std::vector<TTimeDoublePr> TTimeDoublePrVec;
typedef maths::CTrendTests::TIntTimePr TIntTimePr;

const core_t::TTime FIVE_MINS = 300;
const core_t::TTime HALF_HOUR = 1800;
const core_t::TTime DAY = 86400;
const core_t::TTime WEEK = 604800;

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

    double pFalsePositive = 0.0;
    double meanTimeToDetect = 0.0;

    for (std::size_t trend = 0u; trend < 2; ++trend)
    {
        LOG_DEBUG("*** trend = " << trend << " ***");

        int trendCount = 0;
        core_t::TTime timeToDetect = 0;

        for (std::size_t t = 0u; t < 100; ++t)
        {
            TDoubleVec samples;
            rng.generateNormalSamples(1.0, 5.0, 600, samples);
            double scale = 0.03 * sqrt(5.0);

            maths::CTrendTests::CTrend trendTest(0.001);

            core_t::TTime testTimeToDetect = 7200 * samples.size();
            for (std::size_t i = 0u; i < samples.size(); ++i)
            {
                core_t::TTime time = static_cast<core_t::TTime>(i) * 7200;
                double x = (trend == 0 ? 0.0 : scale * static_cast<double>(i)) + samples[i];
                trendTest.add(time, x);
                trendTest.captureVariance(time, x);
                trendTest.propagateForwardsByTime(2.0);
                if (trendTest.test())
                {
                    testTimeToDetect = std::min(testTimeToDetect, time);
                    ++trendCount;
                }
            }
            timeToDetect += testTimeToDetect;
            if (trend == 1)
            {
                CPPUNIT_ASSERT_EQUAL(true, trendTest.test());
            }
        }

        if (trend == 0)
        {
            pFalsePositive = trendCount / 60000.0;
        }
        if (trend == 1)
        {
            meanTimeToDetect = timeToDetect / 100;
        }
    }

    LOG_DEBUG("[P(false positive)] = " << pFalsePositive);
    LOG_DEBUG("time to detect = " << meanTimeToDetect);
    CPPUNIT_ASSERT(pFalsePositive < 1e-4);
    CPPUNIT_ASSERT(meanTimeToDetect < 12 * DAY);

    for (std::size_t trend = 0u; trend < 2; ++trend)
    {
        LOG_DEBUG("*** trend = " << trend << " ***");

        std::size_t trendCount = 0u;
        core_t::TTime timeToDetect = 0;

        for (std::size_t t = 0u; t < 100; ++t)
        {
            TDoubleVec samples;
            rng.generateGammaSamples(50.0, 2.0, 600, samples);
            double scale = 0.03 * ::sqrt(200.0);

            maths::CTrendTests::CTrend trendTest(0.001);

            core_t::TTime testTimeToDetect = 7200 * samples.size();
            for (std::size_t i = 0u; i < samples.size(); ++i)
            {
                core_t::TTime time = static_cast<core_t::TTime>(i) * 7200;
                double x = (trend == 0 ? 0.0 : scale * static_cast<double>(i)) + samples[i];
                trendTest.add(time, x);
                trendTest.captureVariance(time, x);
                trendTest.propagateForwardsByTime(2.0);
                if (trendTest.test())
                {
                    testTimeToDetect = std::min(testTimeToDetect, time);
                    ++trendCount;
                }
            }
            timeToDetect += testTimeToDetect;
            if (trend == 1)
            {
                CPPUNIT_ASSERT_EQUAL(true, trendTest.test());
            }
        }

        if (trend == 0)
        {
            pFalsePositive = trendCount / 60000.0;
        }
        if (trend == 1)
        {
            meanTimeToDetect = timeToDetect / 100;
        }
    }

    LOG_DEBUG("[P(false positive)] = " << pFalsePositive);
    LOG_DEBUG("time to detect = " << meanTimeToDetect);
    CPPUNIT_ASSERT(pFalsePositive < 1e-4);
    CPPUNIT_ASSERT(meanTimeToDetect < 12 * DAY);

    for (std::size_t trend = 0u; trend < 2; ++trend)
    {
        LOG_DEBUG("*** trend = " << trend << " ***");

        std::size_t trendCount = 0u;
        core_t::TTime timeToDetect = 0;

        for (std::size_t t = 0u; t < 100; ++t)
        {
            TDoubleVec samples;
            rng.generateLogNormalSamples(2.5, 1.0, 600, samples);
            double scale = 3.0 * ::sqrt(693.20);

            maths::CTrendTests::CTrend trendTest(0.001);

            core_t::TTime testTimeToDetect = 7200 * samples.size();
            for (std::size_t i = 0u; i < samples.size(); ++i)
            {
                core_t::TTime time = static_cast<core_t::TTime>(i) * 7200;
                double x = (trend == 0 ? 0.0 : scale * ::sin(  boost::math::double_constants::two_pi
                                                             * static_cast<double>(i) / 600.0)) + samples[i];
                trendTest.add(time, x);
                trendTest.captureVariance(time, x);
                trendTest.propagateForwardsByTime(2.0);
                if (trendTest.test())
                {
                    testTimeToDetect = std::min(testTimeToDetect, time);
                    ++trendCount;
                }
            }

            timeToDetect += testTimeToDetect;
            if (trend == 1)
            {
                CPPUNIT_ASSERT_EQUAL(true, trendTest.test());
            }
        }

        if (trend == 0)
        {
            pFalsePositive = trendCount / 60000.0;
        }
        if (trend == 1)
        {
            meanTimeToDetect = timeToDetect / 100;
        }
    }

    LOG_DEBUG("[P(false positive)] = " << pFalsePositive);
    LOG_DEBUG("time to detect = " << meanTimeToDetect);
    CPPUNIT_ASSERT(pFalsePositive < 1e-4);
    CPPUNIT_ASSERT(meanTimeToDetect < 23 * DAY);
}

void CTrendTestsTest::testStepChange(void)
{
    LOG_DEBUG("+-----------------------------------+");
    LOG_DEBUG("|  CTrendTestsTest::testStepChange  |");
    LOG_DEBUG("+-----------------------------------+");

    // Test to see if we can successfully find knot points without
    // false positive in a piecewise constant approximation to a
    // variety of time series.

    typedef maths::CTrendTests::CStepChange::CResult TResult;

    test::CRandomNumbers rng;

    LOG_DEBUG("Test Constant");
    {
        TDoubleVec samples;

        LOG_DEBUG("Uniform");
        {
            rng.generateUniformSamples(0.0, 20.0, 1000, samples);
            maths::CTrendTests::CStepChange test(HALF_HOUR, 24, 0.5);
            core_t::TTime time = 0;
            for (std::size_t i = 0u; i < samples.size(); ++i, time += HALF_HOUR)
            {
                test.add(time, samples[i]);
                if (i % 12 == 0)
                {
                    CPPUNIT_ASSERT(test.captureVarianceAndTest().value() == maths::CTrendTests::E_False);
                }
            }
        }
        LOG_DEBUG("Normal");
        {
            rng.generateNormalSamples(0.0, 50.0, 1000, samples);
            maths::CTrendTests::CStepChange test(FIVE_MINS, 24, 0.1);
            core_t::TTime time = 0;
            for (std::size_t i = 0u; i < samples.size(); ++i, time += FIVE_MINS)
            {
                test.add(time, samples[i]);
                if (i % 60 == 0)
                {
                    CPPUNIT_ASSERT(test.captureVarianceAndTest().value() == maths::CTrendTests::E_False);
                }
            }
        }
        LOG_DEBUG("Log-Normal");
        {
            rng.generateLogNormalSamples(0.5, 3.0, 1000, samples);
            maths::CTrendTests::CStepChange test(HALF_HOUR, 24, 0.5);
            core_t::TTime time = 0;
            for (std::size_t i = 0u; i < samples.size(); ++i, time += HALF_HOUR)
            {
                test.add(time, samples[i]);
                if (i % 12 == 0)
                {
                    CPPUNIT_ASSERT(test.captureVarianceAndTest().value() == maths::CTrendTests::E_False);
                }
            }
        }
    }

    LOG_DEBUG("Test Steps");
    {
        double steps[]  = { 3 * DAY, 10 * DAY, 15 * DAY, 40 * DAY, 50 * DAY };
        double levels[] = { 10.0, 40.0, -5.0, 18.0, -40.0 };

        TDoubleVec noise;
        rng.generateUniformSamples(-5.0, 5.0, 50 * DAY / HALF_HOUR, noise);

        maths::CTrendTests::CStepChange test(HALF_HOUR, 24, 0.5);

        std::size_t shifts = 0u;

        for (core_t::TTime time = 0; time < 50 * DAY; time += HALF_HOUR)
        {
            double level = levels[std::lower_bound(boost::begin(steps),
                                                   boost::end(steps), time) - steps];
            test.add(time, level + noise[time / HALF_HOUR]);
            if (time % (12 * HALF_HOUR) == 0)
            {
                TResult result = test.captureVarianceAndTest();
                if (result.value() == maths::CTrendTests::E_True)
                {
                    LOG_DEBUG("Detected shift on day " << time / DAY);
                    LOG_DEBUG("New level = " << result.level());
                    ptrdiff_t l = std::max(std::lower_bound(boost::begin(steps),
                                                            boost::end(steps), time) - steps,
                                           ptrdiff_t(1)) - 1;
                    CPPUNIT_ASSERT_DOUBLES_EQUAL(steps[l], time, 36 * HALF_HOUR);
                    CPPUNIT_ASSERT_DOUBLES_EQUAL(levels[l+1], result.level(), 2.0);
                    ++shifts;
                }
            }
        }

        CPPUNIT_ASSERT_EQUAL(boost::size(levels) - 1, shifts);
    }

    LOG_DEBUG("Test Periodic");
    {
        double scales[] = { 1.0, 1.0, 1.0, 1.0, 1.0, 0.2, 0.1 };
        double step     = 3 * WEEK;
        double shift    = 80.0;

        TDoubleVec noise;
        rng.generateNormalSamples(0.0, 100.0, 5 * WEEK / FIVE_MINS, noise);

        maths::CTrendTests::CStepChange test(FIVE_MINS, 30, 0.1);

        std::size_t shifts = 0u;

        for (core_t::TTime time = 0; time < 5 * WEEK; time += FIVE_MINS)
        {
            double value =  20.0 * scales[(time % WEEK) / DAY]
                                 * (1.0 + ::sin(  boost::math::double_constants::two_pi
                                                * static_cast<double>(time) / static_cast<double>(DAY)))
                          + (time > step ? shift : 0.0)
                          + noise[time / FIVE_MINS];

            test.add(time, value);
            if (time % (16 * HALF_HOUR) == 0)
            {
                TResult result = test.captureVarianceAndTest();
                if (result.value() == maths::CTrendTests::E_True)
                {
                    LOG_DEBUG("Detected shift on day " << time / DAY);
                    LOG_DEBUG("New level = " << result.level());
                    CPPUNIT_ASSERT_DOUBLES_EQUAL(step, time, 36 * HALF_HOUR);
                    CPPUNIT_ASSERT_DOUBLES_EQUAL(shift + 20.0, result.level(), 10.0);
                    ++shifts;
                }
            }
        }

        CPPUNIT_ASSERT_EQUAL(std::size_t(1), shifts);
    }
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

        boost::scoped_ptr<maths::CTrendTests::CPeriodicity> test(maths::CTrendTests::dailyAndWeekly(FIVE_MINS));

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
    LOG_DEBUG("*** Ramp ***");
    {
        boost::scoped_ptr<maths::CTrendTests::CPeriodicity> test(maths::CTrendTests::dailyAndWeekly(HALF_HOUR));

        for (core_t::TTime time = 0; time < 10 * WEEK; time += HALF_HOUR)
        {
            test->add(time, static_cast<double>(time));

            if (time > DAY && time < 2 * WEEK && time % DAY == 0)
            {
                TResult result = test->test();
                LOG_DEBUG("detected = " << maths::CTrendTests::printDailyAndWeekly(result)
                          << ", time = " << result.startOfPartition());
                CPPUNIT_ASSERT(notPeriodic == result);
            }
            if (time > 2 * WEEK && time % (2 * WEEK) == 0)
            {
                TResult result = test->test();
                LOG_DEBUG("detected = " << maths::CTrendTests::printDailyAndWeekly(result)
                          << ", time = " << result.startOfPartition());
                CPPUNIT_ASSERT(notPeriodic == result);
            }
        }
    }

    LOG_DEBUG("");
    LOG_DEBUG("*** Synthetic: one period ***");
    {
        TDoubleVec samples;
        rng.generateNormalSamples(1.0, 5.0, 4032, samples);

        boost::scoped_ptr<maths::CTrendTests::CPeriodicity> test(maths::CTrendTests::dailyAndWeekly(FIVE_MINS));

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

        boost::scoped_ptr<maths::CTrendTests::CPeriodicity> test(maths::CTrendTests::dailyAndWeekly(FIVE_MINS));

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

        boost::scoped_ptr<maths::CTrendTests::CPeriodicity> test(maths::CTrendTests::dailyAndWeekly(FIVE_MINS));

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

        boost::scoped_ptr<maths::CTrendTests::CPeriodicity> test(maths::CTrendTests::dailyAndWeekly(FIVE_MINS));

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

        boost::scoped_ptr<maths::CTrendTests::CPeriodicity> test(maths::CTrendTests::dailyAndWeekly(FIVE_MINS));

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

        boost::scoped_ptr<maths::CTrendTests::CPeriodicity> test(maths::CTrendTests::dailyAndWeekly(HALF_HOUR));

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

        boost::scoped_ptr<maths::CTrendTests::CPeriodicity> test(maths::CTrendTests::dailyAndWeekly(HALF_HOUR));

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

void CTrendTestsTest::testScanningPeriodicity(void)
{
    LOG_DEBUG("+--------------------------------------------+");
    LOG_DEBUG("|  CTrendTestsTest::testScanningPeriodicity  |");
    LOG_DEBUG("+--------------------------------------------+");

    typedef maths::CTrendTests::CScanningPeriodicity::TPeriodicityResultPr TPeriodicityResultPr;

    test::CRandomNumbers rng;

    std::string partitions[2];
    std::string periods[2];

    LOG_DEBUG("Smooth")
    {
        TDoubleVec timeseries;
        for (core_t::TTime time = 0; time <= 2 * WEEK; time += HALF_HOUR)
        {
            double trend = 15.0 + 10.0 * ::sin(0.7 * boost::math::double_constants::two_pi
                                                   * static_cast<double>(time)
                                                   / static_cast<double>(DAY));
            timeseries.push_back(trend);
        }

        maths::CTrendTests::CScanningPeriodicity test(240, HALF_HOUR);
        test.initialize(0);

        core_t::TTime time = 0;
        for (std::size_t i = 0u; i < timeseries.size(); ++i, time += HALF_HOUR)
        {
            if (test.needToCompress(time))
            {
                TPeriodicityResultPr result = test.test();
                periods[0] = core::CStringUtils::typeToString(result.first.periods()[0]);
                periods[1] = core::CStringUtils::typeToString(result.first.periods()[1]);
                LOG_DEBUG("time = " << time);
                LOG_DEBUG("periods = " << result.second.print(partitions, periods));

                CPPUNIT_ASSERT(result.second.periodic());

                maths::CTrendTests::CPeriodicity::EPeriod period =
                        result.second.periods(maths::CTrendTests::CPeriodicity::E_FullInterval);
                CPPUNIT_ASSERT_EQUAL(maths::CTrendTests::CPeriodicity::E_LongPeriod, period);
                CPPUNIT_ASSERT_DOUBLES_EQUAL(static_cast<double>(DAY) / 0.7,
                                             static_cast<double>(result.first.periods()[1]),
                                             900.0);
                break;
            }
            test.add(time, timeseries[i]);
        }
    }

    LOG_DEBUG("Smooth + Noise")
    {
        TDoubleVec timeseries;
        for (core_t::TTime time = 0; time <= 2 * WEEK; time += HALF_HOUR)
        {
            double trend = 15.0 + 10.0 * ::sin(0.4 * boost::math::double_constants::two_pi
                                                   * static_cast<double>(time)
                                                   / static_cast<double>(DAY));
            timeseries.push_back(trend);
        }

        TDoubleVec noise;
        rng.generateNormalSamples(0.0, 4.0, timeseries.size(), noise);

        maths::CTrendTests::CScanningPeriodicity test(240, HALF_HOUR);
        test.initialize(0);

        core_t::TTime time = 0;
        std::size_t periodic = 0;
        for (std::size_t i = 0u; i < timeseries.size(); ++i, time += HALF_HOUR)
        {
            if (test.needToCompress(time))
            {
                TPeriodicityResultPr result = test.test();
                periods[0] = core::CStringUtils::typeToString(result.first.periods()[0]);
                periods[1] = core::CStringUtils::typeToString(result.first.periods()[1]);
                if (result.second.periodic())
                {
                    LOG_DEBUG("time = " << time);
                    LOG_DEBUG("periods = " << result.second.print(partitions, periods));
                    maths::CTrendTests::CPeriodicity::EPeriod period =
                            result.second.periods(maths::CTrendTests::CPeriodicity::E_FullInterval);
                    CPPUNIT_ASSERT_EQUAL(maths::CTrendTests::CPeriodicity::E_LongPeriod, period);
                    CPPUNIT_ASSERT_EQUAL(static_cast<double>(DAY) / 0.4,
                                         static_cast<double>(result.first.periods()[1]));
                    ++periodic;
                    break;
                }
            }
            test.add(time, timeseries[i] + noise[i]);
        }
        CPPUNIT_ASSERT_EQUAL(std::size_t(1), periodic);
    }


    LOG_DEBUG("Multiple");
    {
        TDoubleVec timeseries;
        for (core_t::TTime time = 0; time <= 8 * WEEK; time += HALF_HOUR)
        {
            double trend = 30.0 + 20.0 * ::sin(0.2 * boost::math::double_constants::two_pi
                                                   * static_cast<double>(time)
                                                   / static_cast<double>(DAY))
                                + 10.0 * ::sin(0.1 * boost::math::double_constants::two_pi
                                                   * static_cast<double>(time)
                                                   / static_cast<double>(DAY));
            timeseries.push_back(trend);
        }

        maths::CTrendTests::CScanningPeriodicity test(120, HALF_HOUR);
        test.initialize(0);

        core_t::TTime time = 0;
        for (std::size_t i = 0u; i < timeseries.size(); ++i, time += HALF_HOUR)
        {
            if (test.needToCompress(time))
            {
                TPeriodicityResultPr result = test.test();
                periods[0] = core::CStringUtils::typeToString(result.first.periods()[0]);
                periods[1] = core::CStringUtils::typeToString(result.first.periods()[1]);
                if (result.second.periodic())
                {
                    LOG_DEBUG("time = " << time);
                    LOG_DEBUG("periods = " << result.second.print(partitions, periods));
                    maths::CTrendTests::CPeriodicity::EPeriod period =
                            result.second.periods(maths::CTrendTests::CPeriodicity::E_FullInterval);
                    CPPUNIT_ASSERT_EQUAL(maths::CTrendTests::CPeriodicity::E_BothPeriods, period);
                    CPPUNIT_ASSERT_DOUBLES_EQUAL(static_cast<double>(DAY) / 0.2,
                                                 static_cast<double>(result.first.periods()[0]),
                                                 0.2 * static_cast<double>(DAY));
                    CPPUNIT_ASSERT_DOUBLES_EQUAL(static_cast<double>(DAY) / 0.1,
                                                 static_cast<double>(result.first.periods()[1]),
                                                 0.4 * static_cast<double>(DAY));
                }
            }
            test.add(time, timeseries[i]);
        }

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

    // Check that persistence is idempotent.

    LOG_DEBUG("Test CTrendTests::CTrend");
    {
        TDoubleVec timeseries;
        for (core_t::TTime time = 0; time <= 2 * WEEK; time += HALF_HOUR)
        {
            double daily = 15.0 + 10.0 * ::sin(boost::math::double_constants::two_pi
                                               * static_cast<double>(time)
                                               / static_cast<double>(DAY));
            timeseries.push_back(daily);
        }

        test::CRandomNumbers rng;
        TDoubleVec noise;
        rng.generateNormalSamples(20.0, 16.0, timeseries.size(), noise);

        maths::CTrendTests::CTrend orig;
        core_t::TTime time = 0;
        for (std::size_t i = 0u; i < timeseries.size(); ++i, time += HALF_HOUR)
        {
            orig.add(time, timeseries[i] + noise[i]);
            orig.captureVariance(time, timeseries[i] + noise[i]);
        }

        std::string origXml;
        {
            core::CRapidXmlStatePersistInserter inserter("root");
            orig.acceptPersistInserter(inserter);
            inserter.toXml(origXml);
        }

        LOG_DEBUG("XML representation:\n" << origXml);

        maths::CTrendTests::CTrend restored;
        {
            core::CRapidXmlParser parser;
            CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(origXml));
            core::CRapidXmlStateRestoreTraverser traverser(parser);
            CPPUNIT_ASSERT(traverser.traverseSubLevel(boost::bind(
                    &maths::CTrendTests::CTrend::acceptRestoreTraverser, &restored, _1)));
        }
        CPPUNIT_ASSERT_EQUAL(orig.checksum(), restored.checksum());

        std::string newXml;
        {
            core::CRapidXmlStatePersistInserter inserter("root");
            restored.acceptPersistInserter(inserter);
            inserter.toXml(newXml);
        }
        CPPUNIT_ASSERT_EQUAL(origXml, newXml);
    }

    LOG_DEBUG("Test CTrendTests::CRandomizedPeriodicity");
    {
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

        LOG_DEBUG("XML representation:\n" << origXml);

        // Restore the XML into a new test
        maths::CTrendTests::CRandomizedPeriodicity test2;
        {
            core::CRapidXmlParser parser;
            CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(origXml));
            core::CRapidXmlStateRestoreTraverser traverser(parser);
            CPPUNIT_ASSERT(traverser.traverseSubLevel(boost::bind(
                    &maths::CTrendTests::CRandomizedPeriodicity::acceptRestoreTraverser, &test2, _1)));
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
            CPPUNIT_ASSERT(traverser.traverseSubLevel(
                    &maths::CTrendTests::CRandomizedPeriodicity::staticsAcceptRestoreTraverser));
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

    LOG_DEBUG("Test CTrendTests::CPeriodicity");
    {
        TDoubleVec timeseries;
        for (core_t::TTime time = 0; time <= 2 * WEEK; time += HALF_HOUR)
        {
            double daily = 15.0 + 10.0 * ::sin(boost::math::double_constants::two_pi
                                               * static_cast<double>(time)
                                               / static_cast<double>(DAY));
            timeseries.push_back(daily);
        }

        test::CRandomNumbers rng;
        TDoubleVec noise;
        rng.generateNormalSamples(20.0, 16.0, timeseries.size(), noise);

        boost::scoped_ptr<maths::CTrendTests::CPeriodicity> orig(maths::CTrendTests::dailyAndWeekly(HALF_HOUR));

        core_t::TTime time = 0;
        for (std::size_t i = 0u; i < timeseries.size(); ++i, time += HALF_HOUR)
        {
            orig->add(time, timeseries[i] + noise[i]);
        }

        std::string origXml;
        {
            core::CRapidXmlStatePersistInserter inserter("root");
            orig->acceptPersistInserter(inserter);
            inserter.toXml(origXml);
        }

        LOG_DEBUG("XML representation:\n" << origXml);

        maths::CTrendTests::CPeriodicity restored;
        {
            core::CRapidXmlParser parser;
            CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(origXml));
            core::CRapidXmlStateRestoreTraverser traverser(parser);
            CPPUNIT_ASSERT(traverser.traverseSubLevel(boost::bind(
                    &maths::CTrendTests::CPeriodicity::acceptRestoreTraverser, &restored, _1)));
        }
        CPPUNIT_ASSERT_EQUAL(orig->checksum(), restored.checksum());

        std::string newXml;
        {
            core::CRapidXmlStatePersistInserter inserter("root");
            restored.acceptPersistInserter(inserter);
            inserter.toXml(newXml);
        }
        CPPUNIT_ASSERT_EQUAL(origXml, newXml);
    }

    LOG_DEBUG("Test CTrendTests::CStepChange");
    {
        TDoubleVec timeseries;
        for (core_t::TTime time = 0; time <= 2 * WEEK; time += HALF_HOUR)
        {
            double daily = 15.0 + 10.0 * ::sin(boost::math::double_constants::two_pi
                                               * static_cast<double>(time)
                                               / static_cast<double>(DAY));
            timeseries.push_back(daily);
        }

        test::CRandomNumbers rng;
        TDoubleVec noise;
        rng.generateNormalSamples(20.0, 16.0, timeseries.size(), noise);

        maths::CTrendTests::CStepChange orig(HALF_HOUR, 24, 0.5);
        core_t::TTime time = 0;
        for (std::size_t i = 0u; i < timeseries.size(); ++i, time += HALF_HOUR)
        {
            orig.add(time, timeseries[i] + noise[i]);
            if (time % 21600 == 0)
            {
                orig.captureVarianceAndTest();
            }
        }

        std::string origXml;
        {
            core::CRapidXmlStatePersistInserter inserter("root");
            orig.acceptPersistInserter(inserter);
            inserter.toXml(origXml);
        }

        LOG_DEBUG("XML representation:\n" << origXml);

        maths::CTrendTests::CStepChange restored(FIVE_MINS, 48, 0.16);
        {
            core::CRapidXmlParser parser;
            CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(origXml));
            core::CRapidXmlStateRestoreTraverser traverser(parser);
            CPPUNIT_ASSERT(traverser.traverseSubLevel(boost::bind(
                    &maths::CTrendTests::CStepChange::acceptRestoreTraverser, &restored, _1)));
        }
        CPPUNIT_ASSERT_EQUAL(orig.checksum(), restored.checksum());

        std::string newXml;
        {
            core::CRapidXmlStatePersistInserter inserter("root");
            restored.acceptPersistInserter(inserter);
            inserter.toXml(newXml);
        }
        CPPUNIT_ASSERT_EQUAL(origXml, newXml);
    }

    LOG_DEBUG("Test CTrendTests::CScanningPeriodicity");
    {
        maths::CTrendTests::CScanningPeriodicity orig(120, HALF_HOUR);
        orig.initialize(0);
        for (core_t::TTime time = 0; time <= 2 * WEEK; time += HALF_HOUR)
        {
            orig.add(time, 15.0 + 10.0 * ::sin(boost::math::double_constants::two_pi
                                               * static_cast<double>(time)
                                               / static_cast<double>(DAY)));
        }

        std::string origXml;
        {
            core::CRapidXmlStatePersistInserter inserter("root");
            orig.acceptPersistInserter(inserter);
            inserter.toXml(origXml);
        }

        LOG_DEBUG("XML representation:\n" << origXml);

        maths::CTrendTests::CScanningPeriodicity restored(10, FIVE_MINS);
        {
            core::CRapidXmlParser parser;
            CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(origXml));
            core::CRapidXmlStateRestoreTraverser traverser(parser);
            CPPUNIT_ASSERT(traverser.traverseSubLevel(boost::bind(
                    &maths::CTrendTests::CScanningPeriodicity::acceptRestoreTraverser, &restored, _1)));
        }
        CPPUNIT_ASSERT_EQUAL(orig.checksum(), restored.checksum());

        std::string newXml;
        {
            core::CRapidXmlStatePersistInserter inserter("root");
            restored.acceptPersistInserter(inserter);
            inserter.toXml(newXml);
        }
        CPPUNIT_ASSERT_EQUAL(origXml, newXml);
    }
}

CppUnit::Test *CTrendTestsTest::suite(void)
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CTrendTestsTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CTrendTestsTest>(
                                   "CTrendTestsTest::testTrend",
                                   &CTrendTestsTest::testTrend) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTrendTestsTest>(
                                   "CTrendTestsTest::testStepChange",
                                   &CTrendTestsTest::testStepChange) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTrendTestsTest>(
                                   "CTrendTestsTest::testRandomizedPeriodicity",
                                   &CTrendTestsTest::testRandomizedPeriodicity) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTrendTestsTest>(
                                   "CTrendTestsTest::testPeriodicity",
                                   &CTrendTestsTest::testPeriodicity) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTrendTestsTest>(
                                   "CTrendTestsTest::testScanningPeriodicity",
                                   &CTrendTestsTest::testScanningPeriodicity) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTrendTestsTest>(
                                   "CTrendTestsTest::testAutocorrelations",
                                   &CTrendTestsTest::testAutocorrelations) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTrendTestsTest>(
                                   "CTrendTestsTest::testPersist",
                                   &CTrendTestsTest::testPersist) );

    return suiteOfTests;
}
