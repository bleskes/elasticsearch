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

#include "CForecastTest.h"

#include <core/CContainerPrinter.h>
#include <core/CLogger.h>
#include <core/CoreTypes.h>

#include <maths/CForecast.h>
#include <maths/CIntegerTools.h>
#include <maths/CLogNormalMeanPrecConjugate.h>
#include <maths/CNormalMeanPrecConjugate.h>
#include <maths/CTimeSeriesDecomposition.h>
#include <maths/CTimeSeriesTestData.h>

#include <test/CRandomNumbers.h>

#include "TestUtils.h"

#include <boost/range.hpp>

using namespace prelert;
using namespace handy_typedefs;

typedef std::vector<double> TDoubleVec;

void CForecastTest::testDailyNoLongTermTrend(void)
{
}

void CForecastTest::testDailyConstantLongTermTrend(void)
{
    LOG_DEBUG("+---------------------------------------------+");
    LOG_DEBUG("|  CForecastTest::testComplexNoLongTermTrend  |");
    LOG_DEBUG("+---------------------------------------------+");

    double y[] =
        {
            0.0,    2.0,   2.0,   4.0,   8.0,  10.0, 15.0, 20.0,
            80.0, 100.0, 110.0, 120.0, 110.0, 100.0, 90.0, 80.0,
            30.0,  15.0,  10.0,   8.0,   5.0,   3.0,  2.0,  0.0
        };
    core_t::TTime bucketLength = 3600;

    test::CRandomNumbers rng;

    maths::CTimeSeriesDecomposition trend(0.01, 3600, 24);
    maths::CNormalMeanPrecConjugate prior =
            maths::CNormalMeanPrecConjugate::nonInformativePrior(maths_t::E_ContinuousData);

    LOG_DEBUG("*** learn ***");

    core_t::TTime time = 0;
    double dy = 0.0;
    for (std::size_t d = 0u; d < 56; ++d)
    {
        TDoubleVec noise;
        rng.generateNormalSamples(0.0, 64.0, boost::size(y), noise);

        for (std::size_t i = 0u; i < boost::size(y); ++i, time += bucketLength, dy += 0.25)
        {
            double yi = dy + y[i] + noise[i];

            trend.addPoint(time, yi);
            trend.propagateForwardsTo(time);

            if (d > 28)
            {
                prior.addSamples(maths::CConstantWeights::COUNT,
                                 TDouble1Vec(1, yi - maths::CBasicStatistics::mean(trend.baseline(time, 0.0))),
                                 maths::CConstantWeights::SINGLE_UNIT);
            }
        }
    }
    LOG_DEBUG(prior.print());

    LOG_DEBUG("*** forecast ***");

    maths::TErrorBarVec prediction;
    maths::forecast(&trend,
                    prior,
                    time,
                    time + 2 * core::constants::WEEK,
                    bucketLength,
                    80.0,
                    0.001,
                    prediction);

    TDoubleVec ly;
    TDoubleVec my;
    TDoubleVec uy;
    for (std::size_t i = 0u; i < prediction.size(); ++i)
    {
        ly.push_back(prediction[i].s_LowerBound);
        my.push_back(prediction[i].s_Predicted);
        uy.push_back(prediction[i].s_UpperBound);
    }

    std::ofstream file;
    file.open("results.m");
    file << "ly = " << core::CContainerPrinter::print(ly) << ";\n";
    file << "my = " << core::CContainerPrinter::print(my) << ";\n";
    file << "uy = " << core::CContainerPrinter::print(uy) << ";\n";
}

void CForecastTest::testDailyVaryingLongTermTrend(void)
{

}

void CForecastTest::testComplexNoLongTermTrend(void)
{
    LOG_DEBUG("+---------------------------------------------+");
    LOG_DEBUG("|  CForecastTest::testComplexNoLongTermTrend  |");
    LOG_DEBUG("+---------------------------------------------+");
}

void CForecastTest::testComplexConstantLongTermTrend(void)
{
    LOG_DEBUG("+---------------------------------------------------+");
    LOG_DEBUG("|  CForecastTest::testComplexConstantLongTermTrend  |");
    LOG_DEBUG("+---------------------------------------------------+");
}

void CForecastTest::testComplexVaryingLongTermTrend(void)
{
    LOG_DEBUG("+--------------------------------------------------+");
    LOG_DEBUG("|  CForecastTest::testComplexVaryingLongTermTrend  |");
    LOG_DEBUG("+--------------------------------------------------+");

}

CppUnit::Test *CForecastTest::suite(void)
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CForecastTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CForecastTest>(
                                   "CForecastTest::testDailyNoLongTermTrend",
                                   &CForecastTest::testDailyNoLongTermTrend) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CForecastTest>(
                                   "CForecastTest::testDailyConstantLongTermTrend",
                                   &CForecastTest::testDailyConstantLongTermTrend) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CForecastTest>(
                                   "CForecastTest::testDailyVaryingLongTermTrend",
                                   &CForecastTest::testDailyVaryingLongTermTrend) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CForecastTest>(
                                   "CForecastTest::testComplexNoLongTermTrend",
                                   &CForecastTest::testComplexNoLongTermTrend) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CForecastTest>(
                                   "CForecastTest::testComplexConstantLongTermTrend",
                                   &CForecastTest::testComplexConstantLongTermTrend) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CForecastTest>(
                                   "CForecastTest::testComplexVaryingLongTermTrend",
                                   &CForecastTest::testComplexVaryingLongTermTrend) );

    return suiteOfTests;
}
