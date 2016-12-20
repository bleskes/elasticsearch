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

#include "CRegressionTest.h"

#include <core/CContainerPrinter.h>
#include <core/CLogger.h>
#include <core/CRapidXmlParser.h>
#include <core/CRapidXmlStatePersistInserter.h>
#include <core/CRapidXmlStateRestoreTraverser.h>

#include <maths/CRegression.h>

#include <test/CRandomNumbers.h>

using namespace prelert;

typedef std::vector<double> TDoubleVec;
typedef maths::CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;

namespace
{

template<typename T>
T sum(const T &params, const T &delta)
{
    T result;
    for (std::size_t i = 0u; i < params.size(); ++i)
    {
        result[i] = params[i] + delta[i];
    }
    return result;
}

template<typename T>
double squareResidual(const T &params,
                      const TDoubleVec &x,
                      const TDoubleVec &y)
{
    double result = 0.0;
    for (std::size_t i = 0u; i < x.size(); ++i)
    {
        double yi = 0.0;
        double xi = 1.0;
        for (std::size_t j = 0u; j < params.size(); ++j, xi *= x[i])
        {
            yi += params[j] * xi;
        }
        result += (y[i] - yi) * (y[i] - yi);
    }
    return result;
}

typedef boost::array<double, 2> TDoubleArray2;
typedef boost::array<double, 3> TDoubleArray3;
typedef boost::array<double, 4> TDoubleArray4;

}

void CRegressionTest::testLeastSquareOnlineInvariants(void)
{
    LOG_DEBUG("+---------------------------------------------------+");
    LOG_DEBUG("|  CRegressionTest::testLeastSquareOnlineInvariants |");
    LOG_DEBUG("+---------------------------------------------------+");

    // Test at (local) minimum of quadratic residuals.

    test::CRandomNumbers rng;

    std::size_t n = 50;

    double intercept = 0.0;
    double slope = 2.0;
    double curvature = 0.2;

    for (std::size_t t = 0u; t < 100; ++t)
    {
        maths::CRegression::CLeastSquaresOnline<2, double> ls;

        TDoubleVec increments;
        rng.generateUniformSamples(1.0, 2.0, n, increments);
        TDoubleVec errors;
        rng.generateNormalSamples(0.0, 2.0, n, errors);

        TDoubleVec xs;
        TDoubleVec ys;
        double x = 0.0;
        for (std::size_t i = 0u; i < n; ++i)
        {
            x += increments[i];
            double y = curvature * x * x + slope * x + intercept + errors[i];
            ls.add(x, y);
            xs.push_back(x);
            ys.push_back(y);
        }

        TDoubleArray3 params;
        CPPUNIT_ASSERT(ls.parameters(params));

        double residual = squareResidual(params, xs, ys);

        if (t % 10 == 0)
        {
            LOG_DEBUG("params   = " << core::CContainerPrinter::print(params));
            LOG_DEBUG("residual = " << residual);
        }

        TDoubleVec delta;
        rng.generateUniformSamples(-1e-4, 1e-4, 15, delta);
        for (std::size_t j = 0u; j < delta.size(); j += 3)
        {
            TDoubleArray3 deltaj;
            deltaj[0] = delta[j];
            deltaj[1] = delta[j+1];
            deltaj[2] = delta[j+2];

            double residualj = squareResidual(sum(params, deltaj), xs, ys);

            if (t % 10 == 0)
            {
                LOG_DEBUG("  delta residual " << residualj);
            }

            CPPUNIT_ASSERT(residualj > residual);
        }
    }
}

void CRegressionTest::testLeastSquareOnlineFit(void)
{
    LOG_DEBUG("+---------------------------------------------+");
    LOG_DEBUG("|  CRegressionTest::testLeastSquareOnlineFit  |");
    LOG_DEBUG("+---------------------------------------------+");

    test::CRandomNumbers rng;

    std::size_t n = 50;

    {
        double intercept = 0.0;
        double slope = 2.0;

        TMeanAccumulator interceptError;
        TMeanAccumulator slopeError;

        for (std::size_t t = 0u; t < 100; ++t)
        {
            maths::CRegression::CLeastSquaresOnline<1> ls;

            TDoubleVec increments;
            rng.generateUniformSamples(1.0, 5.0, n, increments);
            TDoubleVec errors;
            rng.generateNormalSamples(0.0, 2.0, n, errors);

            double x = 0.0;
            for (std::size_t i = 0u; i < n; ++i)
            {
                double y = slope * x + intercept + errors[i];
                ls.add(x, y);
                x += increments[i];
            }

            TDoubleArray2 params;
            CPPUNIT_ASSERT(ls.parameters(params));

            if (t % 10 == 0)
            {
                LOG_DEBUG("params = " << core::CContainerPrinter::print(params));
            }

            CPPUNIT_ASSERT_DOUBLES_EQUAL(intercept, params[0], 1.3);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(slope, params[1], 0.015);
            interceptError.add(::fabs(params[0] - intercept));
            slopeError.add(::fabs(params[1] - slope));
        }

        LOG_DEBUG("intercept error = " << interceptError);
        LOG_DEBUG("slope error = " << slopeError);
        CPPUNIT_ASSERT(maths::CBasicStatistics::mean(interceptError) < 0.35);
        CPPUNIT_ASSERT(maths::CBasicStatistics::mean(slopeError) < 0.04);
    }

    // Test a variety of the randomly generated polynomial fits.

    {
        for (std::size_t t = 0u; t < 10; ++t)
        {
            maths::CRegression::CLeastSquaresOnline<2, double> ls;

            TDoubleVec curve;
            rng.generateUniformSamples(0.0, 2.0, 3, curve);

            TDoubleVec increments;
            rng.generateUniformSamples(1.0, 2.0, n, increments);

            double x = 0.0;
            for (std::size_t i = 0u; i < n; ++i)
            {
                double y = curve[2] * x * x + curve[1] * x + curve[0];
                ls.add(x, y);
                x += increments[i];
            }

            TDoubleArray3 params;
            ls.parameters(params);

            LOG_DEBUG("curve  = " << core::CContainerPrinter::print(curve));
            LOG_DEBUG("params = " << core::CContainerPrinter::print(params));
            for (std::size_t i = 0u; i < curve.size(); ++i)
            {
                CPPUNIT_ASSERT_DOUBLES_EQUAL(curve[i], params[i], 0.03 * curve[i]);
            }
        }
    }
}

void CRegressionTest::testLeastSquareOnlineShift(void)
{
    LOG_DEBUG("+-----------------------------------------------+");
    LOG_DEBUG("|  CRegressionTest::testLeastSquareOnlineShift  |");
    LOG_DEBUG("+-----------------------------------------------+");

    {
        double intercept = 5.0;
        double slope = 2.0;

        maths::CRegression::CLeastSquaresOnline<1> ls;
        maths::CRegression::CLeastSquaresOnline<1> lss;

        for (std::size_t i = 0u; i < 100; ++i)
        {
            double x = static_cast<double>(i);
            ls.add(x, slope * x + intercept);
            lss.add((x - 50.0), slope * x + intercept);
        }

        TDoubleArray2 params1;
        ls.parameters(params1);

        ls.shift(-50.0);
        TDoubleArray2 params2;
        ls.parameters(params2);

        TDoubleArray2 paramss;
        lss.parameters(paramss);

        LOG_DEBUG("params 1 = " << core::CContainerPrinter::print(params1));
        CPPUNIT_ASSERT_DOUBLES_EQUAL(intercept, params1[0], 1e-3 * intercept);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(slope, params1[1], 1e-3 * slope);

        LOG_DEBUG("params 2 = " << core::CContainerPrinter::print(params2));
        LOG_DEBUG("params s = " << core::CContainerPrinter::print(paramss));
        CPPUNIT_ASSERT_DOUBLES_EQUAL(paramss[0], params2[0], 1e-3 * paramss[0]);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(paramss[1], params2[1], 1e-3 * paramss[1]);
    }

    {
        double intercept = 5.0;
        double slope = 2.0;
        double curvature = 0.1;

        maths::CRegression::CLeastSquaresOnline<2, double> ls;
        maths::CRegression::CLeastSquaresOnline<2, double> lss;

        for (std::size_t i = 0u; i < 100; ++i)
        {
            double x = static_cast<double>(i);
            ls.add(x, curvature * x * x + slope * x + intercept);
            lss.add(x - 50.0, curvature * x * x + slope * x + intercept);
        }

        TDoubleArray3 params1;
        ls.parameters(params1);

        ls.shift(-50.0);
        TDoubleArray3 params2;
        ls.parameters(params2);

        TDoubleArray3 paramss;
        lss.parameters(paramss);

        LOG_DEBUG(core::CContainerPrinter::print(params1));
        CPPUNIT_ASSERT_DOUBLES_EQUAL(intercept, params1[0], 2e-3 * intercept);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(slope, params1[1], 2e-3 * slope);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(curvature, params1[2], 2e-3 * curvature);

        LOG_DEBUG(core::CContainerPrinter::print(params2));
        LOG_DEBUG(core::CContainerPrinter::print(paramss));
        LOG_DEBUG("params 2 = " << core::CContainerPrinter::print(params2));
        LOG_DEBUG("params s = " << core::CContainerPrinter::print(paramss));
        CPPUNIT_ASSERT_DOUBLES_EQUAL(paramss[0], params2[0], 1e-3 * paramss[0]);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(paramss[1], params2[1], 1e-3 * paramss[1]);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(paramss[2], params2[2], 1e-3 * paramss[2]);
    }
}

void CRegressionTest::testLeastSquareOnlineAge(void)
{
    LOG_DEBUG("+---------------------------------------------+");
    LOG_DEBUG("|  CRegressionTest::testLeastSquareOnlineAge  |");
    LOG_DEBUG("+---------------------------------------------+");

    // Test that the regression is mean reverting.

    double intercept = 5.0;
    double slope = 2.0;
    double curvature = 0.2;

    {
        maths::CRegression::CLeastSquaresOnline<1> ls;

        for (std::size_t i = 0u; i <= 100; ++i)
        {
            double x = static_cast<double>(i);
            ls.add(x, slope * x + intercept, 5.0);
        }

        TDoubleArray2 params;
        TDoubleArray2 lastParams;

        ls.parameters(params);
        LOG_DEBUG("params(0) = " << core::CContainerPrinter::print(params));

        lastParams = params;
        ls.age(exp(-0.01), true);
        ls.parameters(params);
        LOG_DEBUG("params(0.01) = " << core::CContainerPrinter::print(params));
        CPPUNIT_ASSERT(params[0] > lastParams[0]);
        CPPUNIT_ASSERT(params[0] < 105.0);
        CPPUNIT_ASSERT(params[1] < lastParams[0]);
        CPPUNIT_ASSERT(params[1] > 0.0);

        lastParams = params;
        ls.age(exp(-0.49), true);
        ls.parameters(params);
        LOG_DEBUG("params(0.5) = " << core::CContainerPrinter::print(params));
        CPPUNIT_ASSERT(params[0] > lastParams[0]);
        CPPUNIT_ASSERT(params[0] < 105.0);
        CPPUNIT_ASSERT(params[1] < lastParams[0]);
        CPPUNIT_ASSERT(params[1] > 0.0);

        lastParams = params;
        ls.age(exp(-0.5), true);
        ls.parameters(params);
        LOG_DEBUG("params(1.0) = " << core::CContainerPrinter::print(params));
        CPPUNIT_ASSERT(params[0] > lastParams[0]);
        CPPUNIT_ASSERT(params[0] < 105.0);
        CPPUNIT_ASSERT(params[1] < lastParams[0]);
        CPPUNIT_ASSERT(params[1] > 0.0);

        lastParams = params;
        ls.age(exp(-4.0), true);
        ls.parameters(params);
        LOG_DEBUG("params(5.0) = " << core::CContainerPrinter::print(params));
        CPPUNIT_ASSERT(params[0] > lastParams[0]);
        CPPUNIT_ASSERT(params[0] < 105.0);
        CPPUNIT_ASSERT(params[1] < lastParams[0]);
        CPPUNIT_ASSERT(params[1] > 0.0);
    }

    {
        maths::CRegression::CLeastSquaresOnline<2, double> ls;

        for (std::size_t i = 0u; i <= 100; ++i)
        {
            double x = static_cast<double>(i);
            ls.add(x, curvature * x * x + slope * x + intercept, 5.0);
        }

        TDoubleArray3 params;
        TDoubleArray3 lastParams;

        ls.parameters(params);
        LOG_DEBUG("params(0) = " << core::CContainerPrinter::print(params));

        lastParams = params;
        ls.age(exp(-0.01), true);
        ls.parameters(params);
        LOG_DEBUG("params(0.01) = " << core::CContainerPrinter::print(params));
        CPPUNIT_ASSERT(params[0] > lastParams[0]);
        CPPUNIT_ASSERT(params[0] < 775.0);
        CPPUNIT_ASSERT(params[1] < lastParams[0]);
        CPPUNIT_ASSERT(params[1] > 0.0);
        CPPUNIT_ASSERT(params[2] < lastParams[0]);
        CPPUNIT_ASSERT(params[2] > 0.0);

        lastParams = params;
        ls.age(exp(-0.49), true);
        ls.parameters(params);
        LOG_DEBUG("params(0.5) = " << core::CContainerPrinter::print(params));
        CPPUNIT_ASSERT(params[0] > lastParams[0]);
        CPPUNIT_ASSERT(params[0] < 775.0);
        CPPUNIT_ASSERT(params[1] < lastParams[0]);
        CPPUNIT_ASSERT(params[1] > 0.0);
        CPPUNIT_ASSERT(params[2] < lastParams[0]);
        CPPUNIT_ASSERT(params[2] > 0.0);

        lastParams = params;
        ls.age(exp(-0.5), true);
        ls.parameters(params);
        LOG_DEBUG("params(1.0) = " << core::CContainerPrinter::print(params));
        CPPUNIT_ASSERT(params[0] > lastParams[0]);
        CPPUNIT_ASSERT(params[0] < 775.0);
        CPPUNIT_ASSERT(params[1] < lastParams[0]);
        CPPUNIT_ASSERT(params[1] > 0.0);
        CPPUNIT_ASSERT(params[2] < lastParams[0]);
        CPPUNIT_ASSERT(params[2] > 0.0);

        lastParams = params;
        ls.age(exp(-4.0), true);
        ls.parameters(params);
        LOG_DEBUG("params(5.0) = " << core::CContainerPrinter::print(params));
        CPPUNIT_ASSERT(params[0] > lastParams[0]);
        CPPUNIT_ASSERT(params[0] < 775.0);
        CPPUNIT_ASSERT(params[1] < lastParams[0]);
        CPPUNIT_ASSERT(params[1] > 0.0);
        CPPUNIT_ASSERT(params[2] < lastParams[0]);
        CPPUNIT_ASSERT(params[2] > 0.0);
    }
}

void CRegressionTest::testPrediction(void)
{
    LOG_DEBUG("+-----------------------------------+");
    LOG_DEBUG("|  CRegressionTest::testPrediction  |");
    LOG_DEBUG("+-----------------------------------+");

    // Check we get successive better predictions of a power
    // series function, i.e. x -> sin(x), using higher order
    // approximations.

    double pi = 3.14159265358979;

    TMeanAccumulator m;
    maths::CRegression::CLeastSquaresOnline<1> ls1;
    maths::CRegression::CLeastSquaresOnline<2> ls2;
    maths::CRegression::CLeastSquaresOnline<3> ls3;

    TMeanAccumulator em;
    TMeanAccumulator e2;
    TMeanAccumulator e3;
    TMeanAccumulator e4;

    double x0 = 0.0;
    for (std::size_t i = 0u; i <= 400; ++i)
    {
        double x = 0.005 * pi * static_cast<double>(i);
        double y = ::sin(x);

        m.add(y);
        m.age(0.95);

        ls1.add(x - x0, y);
        ls1.age(0.95);

        ls2.add(x - x0, y);
        ls2.age(0.95);

        ls3.add(x - x0, y);
        ls3.age(0.95);

        if (x > x0 + 2.0)
        {
            ls1.shift(-2.0);
            ls2.shift(-2.0);
            ls3.shift(-2.0);
            x0 += 2.0;
        }

        TDoubleArray2 params2;
        ls1.parameters(params2);
        double y2 = params2[1] * (x - x0) + params2[0];

        TDoubleArray3 params3;
        ls2.parameters(params3);
        double y3 =  params3[2] * (x - x0) * (x - x0)
                   + params3[1] * (x - x0)
                   + params3[0];

        TDoubleArray4 params4;
        ls3.parameters(params4);
        double y4 =  params4[3] * (x - x0) * (x - x0) * (x - x0)
                   + params4[2] * (x - x0) * (x - x0)
                   + params4[1] * (x - x0)
                   + params4[0];

        if (i % 10 == 0)
        {
            LOG_DEBUG("y = " << y
                      << ", m = " << maths::CBasicStatistics::mean(m)
                      << ", y2 = " << y2
                      << ", y3 = " << y3
                      << ", y4 = " << y4);
        }

        em.add((y - maths::CBasicStatistics::mean(m)) * (y - maths::CBasicStatistics::mean(m)));
        e2.add((y - y2) * (y - y2));
        e3.add((y - y3) * (y - y3));
        e4.add((y - y4) * (y - y4));
    }

    LOG_DEBUG("em = " << maths::CBasicStatistics::mean(em)
              << ", e2 = " << maths::CBasicStatistics::mean(e2)
              << ", e3 = " << maths::CBasicStatistics::mean(e3)
              << ", e4 = " << maths::CBasicStatistics::mean(e4));
    CPPUNIT_ASSERT(maths::CBasicStatistics::mean(e2) < 0.27 * maths::CBasicStatistics::mean(em));
    CPPUNIT_ASSERT(maths::CBasicStatistics::mean(e3) < 0.08 * maths::CBasicStatistics::mean(em));
    CPPUNIT_ASSERT(maths::CBasicStatistics::mean(e4) < 0.025 * maths::CBasicStatistics::mean(em));
}

void CRegressionTest::testCombination(void)
{
    LOG_DEBUG("+------------------------------------+");
    LOG_DEBUG("|  CRegressionTest::testCombination  |");
    LOG_DEBUG("+------------------------------------+");

    // Test that we can combine regressions on two subsets of
    // the points to get the same result as the regression on
    // the full collection of points.

    double intercept = 1.0;
    double slope = 1.0;
    double curvature = -0.2;

    std::size_t n = 50;

    test::CRandomNumbers rng;

    TDoubleVec errors;
    rng.generateNormalSamples(0.0, 2.0, n, errors);

    maths::CRegression::CLeastSquaresOnline<2> lsA;
    maths::CRegression::CLeastSquaresOnline<2> lsB;
    maths::CRegression::CLeastSquaresOnline<2> ls;

    for (std::size_t i = 0u; i < (2 * n) / 3; ++i)
    {
        double x = static_cast<double>(i);
        double y = curvature * x * x + slope * x + intercept + errors[i];
        lsA.add(x, y);
        ls.add(x, y);
    }
    for (std::size_t i = (2 * n) / 3; i < n; ++i)
    {
        double x = static_cast<double>(i);
        double y = curvature * x * x + slope * x + intercept + errors[i];
        lsB.add(x, y);
        ls.add(x, y);
    }

    maths::CRegression::CLeastSquaresOnline<2> lsAPlusB = lsA + lsB;

    TDoubleArray3 paramsA;
    lsA.parameters(paramsA);
    TDoubleArray3 paramsB;
    lsB.parameters(paramsB);
    TDoubleArray3 params;
    ls.parameters(params);
    TDoubleArray3 paramsAPlusB;
    lsAPlusB.parameters(paramsAPlusB);

    LOG_DEBUG("params A     = " <<core::CContainerPrinter::print(paramsA));
    LOG_DEBUG("params B     = " <<core::CContainerPrinter::print(paramsB));
    LOG_DEBUG("params       = " <<core::CContainerPrinter::print(params));
    LOG_DEBUG("params A + B = " <<core::CContainerPrinter::print(paramsAPlusB));

    for (std::size_t i = 0u; i < params.size(); ++i)
    {
        CPPUNIT_ASSERT_DOUBLES_EQUAL(params[i], paramsAPlusB[i], 5e-3 * ::fabs(params[i]));
    }
}

void CRegressionTest::testSingular(void)
{
    LOG_DEBUG("+---------------------------------+");
    LOG_DEBUG("|  CRegressionTest::testSingular  |");
    LOG_DEBUG("+---------------------------------+");

    // Test that we get the highest order polynomial regression
    // available for the points added at any time. In particular,
    // one needs at least n + 1 points to be able to determine
    // the parameters of a order n polynomial.

    {
        maths::CRegression::CLeastSquaresOnline<2> regression;
        regression.add(0.0, 1.0);

        TDoubleArray3 params;
        regression.parameters(params);
        LOG_DEBUG("params = " << core::CContainerPrinter::print(params));
        CPPUNIT_ASSERT_DOUBLES_EQUAL(params[0], 1.0, 1e-6);
        CPPUNIT_ASSERT_EQUAL(params[1], 0.0);
        CPPUNIT_ASSERT_EQUAL(params[2], 0.0);

        regression.add(1.0, 2.0);

        regression.parameters(params);
        LOG_DEBUG("params = " << core::CContainerPrinter::print(params));
        CPPUNIT_ASSERT_DOUBLES_EQUAL(params[0], 1.0, 1e-6);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(params[1], 1.0, 1e-6);
        CPPUNIT_ASSERT_EQUAL(params[2], 0.0);

        regression.add(2.0, 3.0);

        LOG_DEBUG(regression.print());
        regression.parameters(params);
        LOG_DEBUG("params = " << core::CContainerPrinter::print(params));
        CPPUNIT_ASSERT_DOUBLES_EQUAL(params[0], 1.0, 5e-6);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(params[1], 1.0, 5e-6);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(params[2], 0.0, 5e-6);
    }
    {
        maths::CRegression::CLeastSquaresOnline<2> regression;
        regression.add(0.0, 1.0);

        TDoubleArray3 params;
        regression.parameters(params);
        LOG_DEBUG("params = " << core::CContainerPrinter::print(params));
        CPPUNIT_ASSERT_DOUBLES_EQUAL(params[0], 1.0, 1e-6);
        CPPUNIT_ASSERT_EQUAL(params[1], 0.0);
        CPPUNIT_ASSERT_EQUAL(params[2], 0.0);

        regression.add(1.0, 2.0);

        regression.parameters(params);
        LOG_DEBUG("params = " << core::CContainerPrinter::print(params));
        CPPUNIT_ASSERT_DOUBLES_EQUAL(params[0], 1.0, 1e-6);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(params[1], 1.0, 1e-6);
        CPPUNIT_ASSERT_EQUAL(params[2], 0.0);

        regression.add(2.0, 5.0);

        LOG_DEBUG(regression.print());
        regression.parameters(params);
        LOG_DEBUG("params = " << core::CContainerPrinter::print(params));
        CPPUNIT_ASSERT_DOUBLES_EQUAL(params[0], 1.0, 5e-6);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(params[1], 0.0, 5e-6);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(params[2], 1.0, 5e-6);
    }
    {
        maths::CRegression::CLeastSquaresOnline<1, double> regression1;
        maths::CRegression::CLeastSquaresOnline<2, double> regression2;
        maths::CRegression::CLeastSquaresOnline<3, double> regression3;

        regression1.add(0.0, 1.5 + 2.0 * 0.0 + 1.1 * 0.0 * 0.0 + 3.3 * 0.0 * 0.0 * 0.0);
        regression2.add(0.0, 1.5 + 2.0 * 0.0 + 1.1 * 0.0 * 0.0 + 3.3 * 0.0 * 0.0 * 0.0);
        regression3.add(0.0, 1.5 + 2.0 * 0.0 + 1.1 * 0.0 * 0.0 + 3.3 * 0.0 * 0.0 * 0.0);

        TDoubleArray4 params3;
        regression3.parameters(params3);
        LOG_DEBUG("params3 = " << core::CContainerPrinter::print(params3));
        CPPUNIT_ASSERT_DOUBLES_EQUAL(params3[0], 1.5, 5e-6);
        CPPUNIT_ASSERT_EQUAL(params3[1], 0.0);
        CPPUNIT_ASSERT_EQUAL(params3[2], 0.0);
        CPPUNIT_ASSERT_EQUAL(params3[3], 0.0);

        regression1.add(0.5, 1.5 + 2.0 * 0.5 + 1.1 * 0.5 * 0.5 + 3.3 * 0.5 * 0.5 * 0.5);
        regression2.add(0.5, 1.5 + 2.0 * 0.5 + 1.1 * 0.5 * 0.5 + 3.3 * 0.5 * 0.5 * 0.5);
        regression3.add(0.5, 1.5 + 2.0 * 0.5 + 1.1 * 0.5 * 0.5 + 3.3 * 0.5 * 0.5 * 0.5);

        TDoubleArray2 params1;
        regression1.parameters(params1);
        LOG_DEBUG("params1 = " << core::CContainerPrinter::print(params3));
        regression3.parameters(params3);
        LOG_DEBUG("params3 = " << core::CContainerPrinter::print(params3));
        CPPUNIT_ASSERT_DOUBLES_EQUAL(params3[0], 1.5, 5e-6);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(params3[1], params1[1], 1e-6);
        CPPUNIT_ASSERT_EQUAL(params3[2], 0.0);
        CPPUNIT_ASSERT_EQUAL(params3[3], 0.0);

        regression2.add(1.0, 1.5 + 2.0 * 1.0 + 1.1 * 1.0 * 1.0 + 3.3 * 1.0 * 1.0 * 1.0);
        regression3.add(1.0, 1.5 + 2.0 * 1.0 + 1.1 * 1.0 * 1.0 + 3.3 * 1.0 * 1.0 * 1.0);

        TDoubleArray3 params2;
        regression2.parameters(params2);
        LOG_DEBUG("params2 = " << core::CContainerPrinter::print(params2));
        regression3.parameters(params3);
        LOG_DEBUG("params3 = " << core::CContainerPrinter::print(params3));
        CPPUNIT_ASSERT_DOUBLES_EQUAL(params3[0], 1.5, 5e-6);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(params3[1], params2[1], 1e-6);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(params3[2], params2[2], 1e-6);
        CPPUNIT_ASSERT_EQUAL(params3[3], 0.0);

        regression3.add(1.5, 1.5 + 2.0 * 1.5 + 1.1 * 1.5 * 1.5 + 3.3 * 1.5 * 1.5 * 1.5);

        LOG_DEBUG(regression3.print());
        regression3.parameters(params3);
        LOG_DEBUG("params3 = " << core::CContainerPrinter::print(params3));
        CPPUNIT_ASSERT_DOUBLES_EQUAL(params3[0], 1.5, 5e-5);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(params3[1], 2.0, 5e-5);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(params3[2], 1.1, 5e-5);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(params3[3], 3.3, 5e-5);
    }
}

void CRegressionTest::testScale(void)
{
    LOG_DEBUG("+------------------------------+");
    LOG_DEBUG("|  CRegressionTest::testScale  |");
    LOG_DEBUG("+------------------------------+");

    // Test that scale reduces the count in the regression statistic

    maths::CRegression::CLeastSquaresOnline<1, double> regression;

    for (std::size_t i = 0u; i < 20; ++i)
    {
        double x = static_cast<double>(i);
        regression.add(x, 5.0 + 0.3 * x);
    }

    LOG_DEBUG("statistic = " << regression.statistic());
    TDoubleArray2 params1;
    regression.parameters(params1);
    CPPUNIT_ASSERT_EQUAL(maths::CBasicStatistics::count(regression.statistic()), 20.0);

    maths::CRegression::CLeastSquaresOnline<1, double> regression2 = regression.scaled(0.5);
    LOG_DEBUG("statistic = " << regression2.statistic());
    TDoubleArray2 params2;
    regression2.parameters(params2);
    CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(params1),
                         core::CContainerPrinter::print(params2));
    CPPUNIT_ASSERT_EQUAL(maths::CBasicStatistics::count(regression2.statistic()), 10.0);

    maths::CRegression::CLeastSquaresOnline<1, double> regression3 = regression2.scaled(0.5);
    LOG_DEBUG("statistic = " << regression3.statistic());
    TDoubleArray2 params3;
    regression3.parameters(params3);
    CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(params1),
                         core::CContainerPrinter::print(params3));
    CPPUNIT_ASSERT_EQUAL(maths::CBasicStatistics::count(regression3.statistic()), 5.0);
}

void CRegressionTest::testCovariances(void)
{
    LOG_DEBUG("+------------------------------------+");
    LOG_DEBUG("|  CRegressionTest::testCovariances  |");
    LOG_DEBUG("+------------------------------------+");

    // Test the covariance matrix of the regression parameters
    // agree with the observed sample covariances of independent
    // fits to a matched model.

    typedef maths::CVectorNx1<double, 2> TVector2;
    typedef maths::CSymmetricMatrixNxN<double, 2> TMatrix2;
    typedef maths::CVectorNx1<double, 3> TVector3;
    typedef maths::CSymmetricMatrixNxN<double, 3> TMatrix3;

    test::CRandomNumbers rng;

    LOG_DEBUG("linear");
    {
        double n = 75.0;
        double variance = 16.0;

        maths::CBasicStatistics::SSampleCovariances<double, 2> covariances;
        for (std::size_t i = 0u; i < 500; ++i)
        {
            TDoubleVec noise;
            rng.generateNormalSamples(0.0, variance, static_cast<std::size_t>(n), noise);
            maths::CRegression::CLeastSquaresOnline<1, double> regression;
            for (double x = 0.0; x < n; x += 1.0)
            {
                regression.add(x, 1.5 * x + noise[static_cast<std::size_t>(x)]);
            }
            TDoubleArray2 params;
            regression.parameters(params);
            covariances.add(TVector2(params));
        }
        TMatrix2 expected = maths::CBasicStatistics::covariances(covariances);

        maths::CRegression::CLeastSquaresOnline<1, double> regression;
        for (double x = 0.0; x < n; x += 1.0)
        {
            regression.add(x, 1.5 * x);
        }
        TMatrix2 actual;
        regression.covariances(variance, actual);

        LOG_DEBUG("expected = " << expected);
        LOG_DEBUG("actual   = " << actual);
        CPPUNIT_ASSERT((actual - expected).frobenius() / expected.frobenius() < 0.05);
    }

    LOG_DEBUG("quadratic");
    {
        double n = 75.0;
        double variance = 16.0;

        maths::CBasicStatistics::SSampleCovariances<double, 3> covariances;
        for (std::size_t i = 0u; i < 500; ++i)
        {
            TDoubleVec noise;
            rng.generateNormalSamples(0.0, variance, static_cast<std::size_t>(n), noise);
            maths::CRegression::CLeastSquaresOnline<2, double> regression;
            for (double x = 0.0; x < n; x += 1.0)
            {
                regression.add(x, 0.25 * x * x + 1.5 * x + noise[static_cast<std::size_t>(x)]);
            }
            TDoubleArray3 params;
            regression.parameters(params);
            covariances.add(TVector3(params));
        }
        TMatrix3 expected = maths::CBasicStatistics::covariances(covariances);

        maths::CRegression::CLeastSquaresOnline<2, double> regression;
        for (double x = 0.0; x < n; x += 1.0)
        {
            regression.add(x, 0.25 * x * x + 1.5 * x);
        }
        TMatrix3 actual;
        regression.covariances(variance, actual);

        LOG_DEBUG("expected = " << expected);
        LOG_DEBUG("actual   = " << actual);
        CPPUNIT_ASSERT((actual - expected).frobenius() / expected.frobenius() < 0.075);
    }
}

void CRegressionTest::testPersist(void)
{
    LOG_DEBUG("+--------------------------------+");
    LOG_DEBUG("|  CRegressionTest::testPersist  |");
    LOG_DEBUG("+--------------------------------+");

    // Test that persistence is idempotent.

    maths::CRegression::CLeastSquaresOnline<2, double> origRegression;

    for (std::size_t i = 0u; i < 20; ++i)
    {
        double x = static_cast<double>(i);
        origRegression.add(x, 5.0 + 0.3 * x + 0.5 * x * x);
    }

    std::string origXml;
    {
        core::CRapidXmlStatePersistInserter inserter("root");
        origRegression.acceptPersistInserter(inserter);
        inserter.toXml(origXml);
    }

    LOG_DEBUG("Regression XML representation:\n" << origXml);

    // Restore the XML into a new regression.
    core::CRapidXmlParser parser;
    CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(origXml));
    core::CRapidXmlStateRestoreTraverser traverser(parser);

    maths::CRegression::CLeastSquaresOnline<2, double> restoredRegression;
    CPPUNIT_ASSERT(traverser.traverseSubLevel(boost::bind(&maths::CRegression::CLeastSquaresOnline<2, double>::acceptRestoreTraverser,
                                                          &restoredRegression,
                                                          _1)));

    CPPUNIT_ASSERT_EQUAL(origRegression.checksum(),
                         restoredRegression.checksum());

    std::string restoredXml;
    {
        core::CRapidXmlStatePersistInserter inserter("root");
        restoredRegression.acceptPersistInserter(inserter);
        inserter.toXml(restoredXml);
    }
    CPPUNIT_ASSERT_EQUAL(origXml, restoredXml);
}

CppUnit::Test *CRegressionTest::suite(void)
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CRegressionTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CRegressionTest>(
                                   "CRegressionTest::testLeastSquareOnlineInvariants",
                                   &CRegressionTest::testLeastSquareOnlineInvariants) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CRegressionTest>(
                                   "CRegressionTest::testLeastSquareOnlineFit",
                                   &CRegressionTest::testLeastSquareOnlineFit) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CRegressionTest>(
                                   "CRegressionTest::testLeastSquareOnlineShift",
                                   &CRegressionTest::testLeastSquareOnlineShift) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CRegressionTest>(
                                   "CRegressionTest::testLeastSquareOnlineAge",
                                   &CRegressionTest::testLeastSquareOnlineAge) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CRegressionTest>(
                                   "CRegressionTest::testPrediction",
                                   &CRegressionTest::testPrediction) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CRegressionTest>(
                                   "CRegressionTest::testCombination",
                                   &CRegressionTest::testCombination) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CRegressionTest>(
                                   "CRegressionTest::testSingular",
                                   &CRegressionTest::testSingular) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CRegressionTest>(
                                   "CRegressionTest::testScale",
                                   &CRegressionTest::testScale) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CRegressionTest>(
                                   "CRegressionTest::testCovariances",
                                   &CRegressionTest::testCovariances) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CRegressionTest>(
                                   "CRegressionTest::testPersist",
                                   &CRegressionTest::testPersist) );

    return suiteOfTests;
}
