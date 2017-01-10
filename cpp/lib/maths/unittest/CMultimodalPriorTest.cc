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

#include "CMultimodalPriorTest.h"

#include <core/CLogger.h>
#include <core/CRapidXmlParser.h>
#include <core/CRapidXmlStatePersistInserter.h>
#include <core/CRapidXmlStateRestoreTraverser.h>

#include <maths/CDistributionRestoreParams.h>
#include <maths/CGammaRateConjugate.h>
#include <maths/CLogNormalMeanPrecConjugate.h>
#include <maths/CMixtureDistribution.h>
#include <maths/CMultimodalPrior.h>
#include <maths/CNormalMeanPrecConjugate.h>
#include <maths/COneOfNPrior.h>
#include <maths/CPriorDetail.h>
#include <maths/CTools.h>
#include <maths/CXMeansOnline1d.h>

#include <test/CRandomNumbers.h>

#include "TestUtils.h"

#include <boost/math/distributions/gamma.hpp>
#include <boost/math/distributions/lognormal.hpp>
#include <boost/math/distributions/normal.hpp>
#include <boost/range.hpp>
#include <boost/shared_ptr.hpp>

#include <vector>

using namespace ml;
using namespace handy_typedefs;

namespace
{

typedef std::vector<double> TDoubleVec;
typedef std::pair<double, double> TDoubleDoublePr;
typedef std::vector<TDoubleDoublePr> TDoubleDoublePrVec;
typedef boost::shared_ptr<maths::CPrior> TPriorPtr;
typedef CPriorTestInterfaceMixin<maths::CGammaRateConjugate> CGammaRateConjugate;
typedef CPriorTestInterfaceMixin<maths::CLogNormalMeanPrecConjugate> CLogNormalMeanPrecConjugate;
typedef CPriorTestInterfaceMixin<maths::CNormalMeanPrecConjugate> CNormalMeanPrecConjugate;
typedef CPriorTestInterfaceMixin<maths::CMultimodalPrior> CMultimodalPrior;
typedef CPriorTestInterfaceMixin<maths::COneOfNPrior> COneOfNPrior;

//! Make the default mode prior.
COneOfNPrior makeModePrior(const double &decayRate = 0.0)
{
    CGammaRateConjugate gamma(
            maths::CGammaRateConjugate::nonInformativePrior(maths_t::E_ContinuousData,
                                                            0.01,
                                                            decayRate));
    CLogNormalMeanPrecConjugate logNormal(
            maths::CLogNormalMeanPrecConjugate::nonInformativePrior(maths_t::E_ContinuousData,
                                                                    0.01,
                                                                    decayRate));
    CNormalMeanPrecConjugate normal(
            maths::CNormalMeanPrecConjugate::nonInformativePrior(maths_t::E_ContinuousData,
                                                                 decayRate));

    COneOfNPrior::TPriorPtrVec priors;
    priors.push_back(COneOfNPrior::TPriorPtr(gamma.clone()));
    priors.push_back(COneOfNPrior::TPriorPtr(logNormal.clone()));
    priors.push_back(COneOfNPrior::TPriorPtr(normal.clone()));
    return COneOfNPrior(maths::COneOfNPrior(priors, maths_t::E_ContinuousData, decayRate));
}

//! Make a vanilla multimodal prior.
CMultimodalPrior makePrior(const maths::CPrior *modePrior,
                           const double &decayRate)
{
    maths::CXMeansOnline1d clusterer(maths_t::E_ContinuousData,
                                     maths::CAvailableModeDistributions::ALL,
                                     maths_t::E_ClustersFractionWeight,
                                     decayRate);

    if (modePrior)
    {
        return maths::CMultimodalPrior(maths_t::E_ContinuousData,
                                       clusterer,
                                       *modePrior,
                                       decayRate);
    }
    return maths::CMultimodalPrior(maths_t::E_ContinuousData,
                                   clusterer,
                                   makeModePrior(decayRate),
                                   decayRate);
}
CMultimodalPrior makePrior(const maths::CPrior *modePrior)
{
    return makePrior(modePrior, 0.0);
}
CMultimodalPrior makePrior(const double &decayRate)
{
    return makePrior(0, decayRate);
}
CMultimodalPrior makePrior(void)
{
    return makePrior(0, 0.0);
}

test::CRandomNumbers RNG;

void sample(const boost::math::normal_distribution<> &normal,
            std::size_t numberSamples,
            TDoubleVec &result)
{
    RNG.generateNormalSamples(boost::math::mean(normal),
                              boost::math::variance(normal),
                              numberSamples,
                              result);
}

void sample(const boost::math::lognormal_distribution<> &lognormal,
            std::size_t numberSamples,
            TDoubleVec &result)
{
    RNG.generateLogNormalSamples(lognormal.location(),
                                 lognormal.scale() * lognormal.scale(),
                                 numberSamples,
                                 result);
}

void sample(const boost::math::gamma_distribution<> &gamma,
            std::size_t numberSamples,
            TDoubleVec &result)
{
    RNG.generateGammaSamples(gamma.shape(),
                             gamma.scale(),
                             numberSamples,
                             result);
}
template<typename T>
void probabilityOfLessLikelySample(const maths::CMixtureDistribution<T> &mixture,
                                   const double &x,
                                   double &probability,
                                   double &deviation)
{
    typedef typename maths::CMixtureDistribution<T>::TModeVec TModeVec;

    static const double NUMBER_SAMPLES = 10000.0;

    probability = 0.0;

    double fx = pdf(mixture, x);
    const TDoubleVec &weights = mixture.weights();
    const TModeVec &modes = mixture.modes();
    for (std::size_t i = 0u; i < modes.size(); ++i)
    {
        TDoubleVec samples;
        sample(modes[i], static_cast<std::size_t>(NUMBER_SAMPLES * weights[i]), samples);
        for (std::size_t j = 0u; j < samples.size(); ++j)
        {
            if (pdf(mixture, samples[j]) < fx)
            {
                probability += 1.0 / NUMBER_SAMPLES;
            }
        }
    }

    // For a discussion of the deviation see the paper:
    //   "Anomaly Detection in Application Performance Monitoring Data"
    deviation = ::sqrt(probability * (1.0 - probability) / NUMBER_SAMPLES);
}

}

void CMultimodalPriorTest::testMultipleUpdate(void)
{
    LOG_DEBUG("+--------------------------------------------+");
    LOG_DEBUG("|  CMultimodalPriorTest::testMultipleUpdate  |");
    LOG_DEBUG("+--------------------------------------------+");

    // Test that we get the same result updating once with a vector of 100
    // samples of an R.V. verses updating individually 100 times.

    const maths_t::EDataType dataTypes[] =
        {
            maths_t::E_IntegerData,
            maths_t::E_ContinuousData
        };

    const double shape = 2.0;
    const double scale = 3.0;

    const double decayRate = 0.0;

    test::CRandomNumbers rng;

    TDoubleVec samples;
    rng.generateNormalSamples(shape, scale, 100, samples);

    for (size_t i = 0; i < boost::size(dataTypes); ++i)
    {
        maths::CXMeansOnline1d clusterer(dataTypes[i],
                                         maths::CAvailableModeDistributions::ALL,
                                         maths_t::E_ClustersFractionWeight);

        CMultimodalPrior filter1(maths::CMultimodalPrior(
                                     dataTypes[i],
                                     clusterer,
                                     maths::CNormalMeanPrecConjugate::nonInformativePrior(dataTypes[i],
                                                                                          decayRate)));
        CMultimodalPrior filter2(filter1);

        for (std::size_t j = 0; j < samples.size(); ++j)
        {
            filter1.addSamples(TDouble1Vec(1, samples[j]));
        }
        filter2.addSamples(samples);

        LOG_DEBUG("checksum 1 " << filter1.checksum());
        LOG_DEBUG("checksum 2 " << filter2.checksum());
        CPPUNIT_ASSERT_EQUAL(filter1.checksum(), filter2.checksum());
    }
}

void CMultimodalPriorTest::testPropagation(void)
{
    LOG_DEBUG("+-----------------------------------------+");
    LOG_DEBUG("|  CMultimodalPriorTest::testPropagation  |");
    LOG_DEBUG("+-----------------------------------------+");

    // Test that propagation doesn't affect the marginal likelihood
    // mean and the marginal likelihood confidence intervals increase
    // (due to influence of the prior uncertainty) after propagation.

    typedef std::pair<double, double> TDoubleDoublePr;

    double eps = 0.01;

    test::CRandomNumbers rng;

    TDoubleVec samples1;
    rng.generateNormalSamples(3.0, 1.0, 200, samples1);
    TDoubleVec samples2;
    rng.generateNormalSamples(10.0, 4.0, 200, samples2);
    TDoubleVec samples;
    samples.insert(samples.end(), samples1.begin(), samples1.end());
    samples.insert(samples.end(), samples2.begin(), samples2.end());

    rng.random_shuffle(samples.begin(), samples.end());

    const double decayRate = 0.1;
    CMultimodalPrior filter(makePrior(decayRate));

    for (std::size_t i = 0u; i < samples.size(); ++i)
    {
        filter.addSamples(TDouble1Vec(1, static_cast<double>(samples[i])));
        CPPUNIT_ASSERT(filter.checkInvariants());
    }

    double mean = filter.marginalLikelihoodMean();
    TDoubleDoublePr percentiles[] =
        {
            filter.marginalLikelihoodConfidenceInterval(60.0),
            filter.marginalLikelihoodConfidenceInterval(70.0),
            filter.marginalLikelihoodConfidenceInterval(80.0),
            filter.marginalLikelihoodConfidenceInterval(90.0)
        };

    filter.propagateForwardsByTime(40.0);
    CPPUNIT_ASSERT(filter.checkInvariants());

    double propagatedMean = filter.marginalLikelihoodMean();
    TDoubleDoublePr propagatedPercentiles[] =
        {
            filter.marginalLikelihoodConfidenceInterval(60.0),
            filter.marginalLikelihoodConfidenceInterval(70.0),
            filter.marginalLikelihoodConfidenceInterval(80.0),
            filter.marginalLikelihoodConfidenceInterval(90.0)
        };

    LOG_DEBUG("mean = " << mean << ", propagatedMean = " << propagatedMean);
    LOG_DEBUG("percentiles           = "
              << core::CContainerPrinter::print(percentiles));
    LOG_DEBUG("propagatedPercentiles = "
              << core::CContainerPrinter::print(propagatedPercentiles));

    CPPUNIT_ASSERT_DOUBLES_EQUAL(mean, propagatedMean, eps * mean);
    for (std::size_t i = 0u; i < boost::size(percentiles); ++i)
    {
        CPPUNIT_ASSERT(propagatedPercentiles[i].first < percentiles[i].first);
        CPPUNIT_ASSERT(propagatedPercentiles[i].second > percentiles[i].second);
    }
}

void CMultimodalPriorTest::testSingleMode(void)
{
    LOG_DEBUG("+----------------------------------------+");
    LOG_DEBUG("|  CMultimodalPriorTest::testSingleMode  |");
    LOG_DEBUG("+----------------------------------------+");

    // We test the log likelihood of the data for the estimated
    // distributions verses the generating distributions. Note
    // that the generating distribution doesn't necessarily have
    // a larger likelihood because we are using a finite sample.

    typedef maths::CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;

    test::CRandomNumbers rng;

    LOG_DEBUG("Gaussian");
    {
        COneOfNPrior modePrior(makeModePrior());
        CMultimodalPrior filter1(makePrior(&modePrior));
        COneOfNPrior filter2 = modePrior;

        const double mean = 10.0;
        const double variance = 2.0;

        TDoubleVec samples;
        rng.generateNormalSamples(mean, ::sqrt(variance), 1000, samples);

        for (std::size_t i = 0u; i < samples.size(); ++i)
        {
            TDouble1Vec sample(1, samples[i]);
            filter1.addSamples(sample);
            filter2.addSamples(sample);
            CPPUNIT_ASSERT(filter1.checkInvariants());
        }

        TMeanAccumulator L1G;
        TMeanAccumulator L12;
        TMeanAccumulator differentialEntropy;

        boost::math::normal_distribution<> f(mean, ::sqrt(variance));
        for (std::size_t i = 0u; i < samples.size(); ++i)
        {
            double fx = boost::math::pdf(f, samples[i]);
            TDouble1Vec sample(1, samples[i]);
            double l1;
            CPPUNIT_ASSERT_EQUAL(maths_t::E_FpNoErrors,
                                 filter1.jointLogMarginalLikelihood(sample, l1));
            L1G.add(::log(fx) - l1);
            double l2;
            CPPUNIT_ASSERT_EQUAL(maths_t::E_FpNoErrors,
                                 filter2.jointLogMarginalLikelihood(sample, l2));
            L12.add(l2 - l1);
            differentialEntropy.add(-::log(fx));
        }

        LOG_DEBUG("L1G = " << maths::CBasicStatistics::mean(L1G)
                  << ", L12 = " << maths::CBasicStatistics::mean(L12)
                  << ", differential entropy " << differentialEntropy);

        CPPUNIT_ASSERT(  maths::CBasicStatistics::mean(L1G)
                       / maths::CBasicStatistics::mean(differentialEntropy) < 0.0);
    }
    LOG_DEBUG("Log-Normal");
    {
        COneOfNPrior modePrior(makeModePrior());
        CMultimodalPrior filter1(makePrior(&modePrior));
        COneOfNPrior filter2 = modePrior;

        const double location = 1.5;
        const double squareScale = 0.9;

        TDoubleVec samples;
        rng.generateLogNormalSamples(location, squareScale, 1000, samples);

        for (std::size_t i = 0u; i < samples.size(); ++i)
        {
            TDouble1Vec sample(1, samples[i]);
            filter1.addSamples(sample);
            filter2.addSamples(sample);
            CPPUNIT_ASSERT(filter1.checkInvariants());
        }

        TMeanAccumulator L1G;
        TMeanAccumulator L12;
        TMeanAccumulator differentialEntropy;

        boost::math::lognormal_distribution<> f(location, ::sqrt(squareScale));

        for (std::size_t i = 0u; i < samples.size(); ++i)
        {
            double fx = boost::math::pdf(f, samples[i]);
            TDouble1Vec sample(1, samples[i]);
            double l1;
            CPPUNIT_ASSERT_EQUAL(maths_t::E_FpNoErrors,
                                 filter1.jointLogMarginalLikelihood(sample, l1));
            L1G.add(::log(fx) - l1);
            double l2;
            CPPUNIT_ASSERT_EQUAL(maths_t::E_FpNoErrors,
                                 filter2.jointLogMarginalLikelihood(sample, l2));
            L12.add(l2 - l1);
            differentialEntropy.add(-::log(fx));
        }

        LOG_DEBUG("L1G = " << maths::CBasicStatistics::mean(L1G)
                  << ", L12 = " << maths::CBasicStatistics::mean(L12)
                  << ", differential entropy " << differentialEntropy);

        CPPUNIT_ASSERT(  maths::CBasicStatistics::mean(L1G)
                       / maths::CBasicStatistics::mean(differentialEntropy) < 0.0);
    }
    LOG_DEBUG("Gamma");
    {
        COneOfNPrior modePrior(makeModePrior());
        CMultimodalPrior filter1(makePrior(&modePrior));
        COneOfNPrior filter2 = modePrior;

        const double shape = 1.0;
        const double scale = 0.5;

        TDoubleVec samples;
        rng.generateGammaSamples(shape, scale, 1000, samples);

        for (std::size_t i = 0u; i < samples.size(); ++i)
        {
            TDouble1Vec sample(1, samples[i]);
            filter1.addSamples(sample);
            filter2.addSamples(sample);
            CPPUNIT_ASSERT(filter1.checkInvariants());
        }

        TMeanAccumulator L1G;
        TMeanAccumulator L12;
        TMeanAccumulator differentialEntropy;

        boost::math::gamma_distribution<> f(shape, scale);

        for (std::size_t i = 0u; i < samples.size(); ++i)
        {
            double fx = boost::math::pdf(f, samples[i]);
            TDouble1Vec sample(1, samples[i]);
            double l1;
            CPPUNIT_ASSERT_EQUAL(maths_t::E_FpNoErrors,
                                 filter1.jointLogMarginalLikelihood(sample, l1));
            L1G.add(::log(fx) - l1);
            double l2;
            CPPUNIT_ASSERT_EQUAL(maths_t::E_FpNoErrors,
                                 filter2.jointLogMarginalLikelihood(sample, l2));
            L12.add(l2 - l1);
            differentialEntropy.add(-::log(fx));
        }

        LOG_DEBUG("L1G = " << maths::CBasicStatistics::mean(L1G)
                  << ", L12 = " << maths::CBasicStatistics::mean(L12)
                  << ", differential entropy " << differentialEntropy);

        CPPUNIT_ASSERT(  maths::CBasicStatistics::mean(L1G)
                       / maths::CBasicStatistics::mean(differentialEntropy) < 0.05);
    }
}

void CMultimodalPriorTest::testMultipleModes(void)
{
    LOG_DEBUG("+-------------------------------------------+");
    LOG_DEBUG("|  CMultimodalPriorTest::testMultipleModes  |");
    LOG_DEBUG("+-------------------------------------------+");

    // We check that for data generated from multiple modes
    // we get something close to the generating distribution.
    // In particular, we test the log likelihood of the data
    // for the estimated distribution verses the generating
    // distribution and verses an unclustered distribution.
    // Note that the generating distribution doesn't necessarily
    // have a larger likelihood because we are using a finite
    // sample.

    typedef maths::CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;

    test::CRandomNumbers rng;

    {
        LOG_DEBUG("Mixture Normals");

        const std::size_t n1 = 400u;
        const double mean1 = 10.0;
        const double variance1 = 2.0;

        const std::size_t n2 = 600u;
        const double mean2 = 20.0;
        const double variance2 = 5.0;

        TDoubleVec samples1;
        rng.generateNormalSamples(mean1, variance1, n1, samples1);
        TDoubleVec samples2;
        rng.generateNormalSamples(mean2, variance2, n2, samples2);

        TDoubleVec samples;
        samples.insert(samples.end(), samples1.begin(), samples1.end());
        samples.insert(samples.end(), samples2.begin(), samples2.end());

        LOG_DEBUG("# samples = " << samples.size());

        double w1 = n1 / static_cast<double>(n1 + n2);
        double w2 = n2 / static_cast<double>(n1 + n2);
        boost::math::normal_distribution<> mode1Distribution(mean1, ::sqrt(variance1));
        boost::math::normal_distribution<> mode2Distribution(mean2, ::sqrt(variance2));

        double loss = 0.0;
        TMeanAccumulator differentialEntropy_;
        for (std::size_t j = 0u; j < samples.size(); ++j)
        {
            double fx =  w1 * boost::math::pdf(mode1Distribution, samples[j])
                       + w2 * boost::math::pdf(mode2Distribution, samples[j]);
            differentialEntropy_.add(-::log(fx));
        }
        double differentialEntropy = maths::CBasicStatistics::mean(differentialEntropy_);

        for (std::size_t i = 0; i < 10; ++i)
        {
            rng.random_shuffle(samples.begin(), samples.end());

            COneOfNPrior modePrior(makeModePrior());
            CMultimodalPrior filter1(makePrior(&modePrior));
            COneOfNPrior filter2 = modePrior;

            for (std::size_t j = 0u; j < samples.size(); ++j)
            {
                TDouble1Vec sample(1, samples[j]);
                filter1.addSamples(sample);
                filter2.addSamples(sample);
                CPPUNIT_ASSERT(filter1.checkInvariants());
            }

            CPPUNIT_ASSERT_EQUAL(std::size_t(2), filter1.numberModes());

            TMeanAccumulator loss1G;
            TMeanAccumulator loss12;

            for (std::size_t j = 0u; j < samples.size(); ++j)
            {
                double fx =  w1 * boost::math::pdf(mode1Distribution, samples[j])
                           + w2 * boost::math::pdf(mode2Distribution, samples[j]);
                TDouble1Vec sample(1, samples[j]);
                double l1;
                CPPUNIT_ASSERT_EQUAL(maths_t::E_FpNoErrors,
                                     filter1.jointLogMarginalLikelihood(sample, l1));
                loss1G.add(::log(fx) - l1);
                double l2;
                CPPUNIT_ASSERT_EQUAL(maths_t::E_FpNoErrors,
                                     filter2.jointLogMarginalLikelihood(sample, l2));
                loss12.add(l2 - l1);
            }

            LOG_DEBUG("loss1G = " << maths::CBasicStatistics::mean(loss1G)
                      << ", loss12 = " << maths::CBasicStatistics::mean(loss12)
                      << ", differential entropy " << differentialEntropy);

            CPPUNIT_ASSERT(maths::CBasicStatistics::mean(loss12) < 0.0);
            CPPUNIT_ASSERT(maths::CBasicStatistics::mean(loss1G) / differentialEntropy < 0.0);
            loss += maths::CBasicStatistics::mean(loss1G);
        }

        loss /= 10.0;
        LOG_DEBUG("loss = " << loss
                  << ", differential entropy = " << differentialEntropy);
        CPPUNIT_ASSERT(loss / differentialEntropy < 0.0);
    }
    {
        LOG_DEBUG("Mixture Log-Normals");

        const std::size_t n1 = 600u;
        const double location1 = 2.0;
        const double squareScale1 = 0.04;

        const std::size_t n2 = 300u;
        const double location2 = 3.0;
        const double squareScale2 = 0.08;

        const std::size_t n3 = 100u;
        const double location3 = 4.0;
        const double squareScale3 = 0.01;

        TDoubleVec samples1;
        rng.generateLogNormalSamples(location1, squareScale1, n1, samples1);
        TDoubleVec samples2;
        rng.generateLogNormalSamples(location2, squareScale2, n2, samples2);
        TDoubleVec samples3;
        rng.generateLogNormalSamples(location3, squareScale3, n3, samples3);

        TDoubleVec samples;
        samples.insert(samples.end(), samples1.begin(), samples1.end());
        samples.insert(samples.end(), samples2.begin(), samples2.end());
        samples.insert(samples.end(), samples3.begin(), samples3.end());

        LOG_DEBUG("# samples = " << samples.size());

        double w1 = n1 / static_cast<double>(n1 + n2 + n3);
        double w2 = n2 / static_cast<double>(n1 + n2 + n3);
        double w3 = n3 / static_cast<double>(n1 + n2 + n3);
        boost::math::lognormal_distribution<> mode1Distribution(location1, ::sqrt(squareScale1));
        boost::math::lognormal_distribution<> mode2Distribution(location2, ::sqrt(squareScale2));
        boost::math::lognormal_distribution<> mode3Distribution(location3, ::sqrt(squareScale3));

        double loss = 0.0;
        TMeanAccumulator differentialEntropy_;
        for (std::size_t j = 0u; j < samples.size(); ++j)
        {
            double fx =  w1 * boost::math::pdf(mode1Distribution, samples[j])
                       + w2 * boost::math::pdf(mode2Distribution, samples[j])
                       + w3 * boost::math::pdf(mode3Distribution, samples[j]);
            differentialEntropy_.add(-::log(fx));
        }
        double differentialEntropy = maths::CBasicStatistics::mean(differentialEntropy_);

        for (std::size_t i = 0; i < 10; ++i)
        {
            rng.random_shuffle(samples.begin(), samples.end());

            COneOfNPrior modePrior(makeModePrior());
            CMultimodalPrior filter1(makePrior(&modePrior));
            COneOfNPrior filter2 = modePrior;

            for (std::size_t j = 0u; j < samples.size(); ++j)
            {
                TDouble1Vec sample(1, samples[j]);
                filter1.addSamples(sample);
                filter2.addSamples(sample);
                CPPUNIT_ASSERT(filter1.checkInvariants());
            }

            CPPUNIT_ASSERT_EQUAL(std::size_t(3), filter1.numberModes());

            TMeanAccumulator loss1G;
            TMeanAccumulator loss12;

            for (std::size_t j = 0u; j < samples.size(); ++j)
            {
                double fx =  w1 * boost::math::pdf(mode1Distribution, samples[j])
                           + w2 * boost::math::pdf(mode2Distribution, samples[j])
                           + w3 * boost::math::pdf(mode3Distribution, samples[j]);
                TDouble1Vec sample(1, samples[j]);
                double l1;
                CPPUNIT_ASSERT_EQUAL(maths_t::E_FpNoErrors,
                                     filter1.jointLogMarginalLikelihood(sample, l1));
                loss1G.add(::log(fx) - l1);
                double l2;
                CPPUNIT_ASSERT_EQUAL(maths_t::E_FpNoErrors,
                                     filter2.jointLogMarginalLikelihood(sample, l2));
                loss12.add(l2 - l1);
            }

            LOG_DEBUG("loss1G = " << maths::CBasicStatistics::mean(loss1G)
                      << ", loss12 = " << maths::CBasicStatistics::mean(loss12)
                      << ", differential entropy " << differentialEntropy);

            CPPUNIT_ASSERT(maths::CBasicStatistics::mean(loss12) < 0.0);
            CPPUNIT_ASSERT(maths::CBasicStatistics::mean(loss1G) / differentialEntropy < 0.001);
            loss += maths::CBasicStatistics::mean(loss1G);
        }

        loss /= 10.0;
        LOG_DEBUG("loss = " << loss
                  << ", differential entropy = " << differentialEntropy);
        CPPUNIT_ASSERT(loss / differentialEntropy < 0.0);
    }
    {
        LOG_DEBUG("Mixed Modes");

        const std::size_t n1 = 400u;
        const double mean1 = 10.0;
        const double variance1 = 1.0;

        const std::size_t n2 = 200u;
        const double location2 = 3.0;
        const double squareScale2 = 0.08;

        const std::size_t n3 = 400u;
        const double shape3 = 120.0;
        const double scale3 = 0.3;

        TDoubleVec samples1;
        rng.generateNormalSamples(mean1, variance1, n1, samples1);
        TDoubleVec samples2;
        rng.generateLogNormalSamples(location2, squareScale2, n2, samples2);
        TDoubleVec samples3;
        rng.generateGammaSamples(shape3, scale3, n3, samples3);

        TDoubleVec samples;
        samples.insert(samples.end(), samples1.begin(), samples1.end());
        samples.insert(samples.end(), samples2.begin(), samples2.end());
        samples.insert(samples.end(), samples3.begin(), samples3.end());

        LOG_DEBUG("# samples = " << samples.size());

        double w1 = n1 / static_cast<double>(n1 + n2 + n3);
        double w2 = n2 / static_cast<double>(n1 + n2 + n3);
        double w3 = n3 / static_cast<double>(n1 + n2 + n3);
        boost::math::normal_distribution<> mode1Distribution(mean1, ::sqrt(variance1));
        boost::math::lognormal_distribution<> mode2Distribution(location2, ::sqrt(squareScale2));
        boost::math::gamma_distribution<> mode3Distribution(shape3, scale3);

        double loss = 0.0;
        TMeanAccumulator differentialEntropy_;
        for (std::size_t j = 0u; j < samples.size(); ++j)
        {
            double fx =  w1 * boost::math::pdf(mode1Distribution, samples[j])
                       + w2 * boost::math::pdf(mode2Distribution, samples[j])
                       + w3 * boost::math::pdf(mode3Distribution, samples[j]);
            differentialEntropy_.add(-::log(fx));
        }
        double differentialEntropy = maths::CBasicStatistics::mean(differentialEntropy_);

        for (std::size_t i = 0; i < 10; ++i)
        {
            rng.random_shuffle(samples.begin(), samples.end());

            COneOfNPrior modePrior(makeModePrior());
            CMultimodalPrior filter1(makePrior(&modePrior));
            COneOfNPrior filter2 = modePrior;

            for (std::size_t j = 0u; j < samples.size(); ++j)
            {
                TDouble1Vec sample(1, samples[j]);
                filter1.addSamples(sample);
                filter2.addSamples(sample);
                CPPUNIT_ASSERT(filter1.checkInvariants());
            }

            CPPUNIT_ASSERT_EQUAL(std::size_t(3), filter1.numberModes());

            TMeanAccumulator loss1G;
            TMeanAccumulator loss12;

            for (std::size_t j = 0u; j < samples.size(); ++j)
            {
                double fx =  w1 * boost::math::pdf(mode1Distribution, samples[j])
                           + w2 * boost::math::pdf(mode2Distribution, samples[j])
                           + w3 * boost::math::pdf(mode3Distribution, samples[j]);
                TDouble1Vec sample(1, samples[j]);
                double l1;
                CPPUNIT_ASSERT_EQUAL(maths_t::E_FpNoErrors,
                                     filter1.jointLogMarginalLikelihood(sample, l1));
                loss1G.add(::log(fx) - l1);
                double l2;
                CPPUNIT_ASSERT_EQUAL(maths_t::E_FpNoErrors,
                                     filter2.jointLogMarginalLikelihood(sample, l2));
                loss12.add(l2 - l1);
            }

            LOG_DEBUG("loss1G = " << maths::CBasicStatistics::mean(loss1G)
                      << ", loss12 = " << maths::CBasicStatistics::mean(loss12)
                      << ", differential entropy " << differentialEntropy);

            CPPUNIT_ASSERT(maths::CBasicStatistics::mean(loss12) < 0.0);
            CPPUNIT_ASSERT(maths::CBasicStatistics::mean(loss1G) / differentialEntropy < 0.01);
            loss += maths::CBasicStatistics::mean(loss1G);
        }

        loss /= 10.0;
        LOG_DEBUG("loss = " << loss
                  << ", differential entropy = " << differentialEntropy);
        CPPUNIT_ASSERT(loss / differentialEntropy < 0.003);
    }
}

void CMultimodalPriorTest::testMarginalLikelihood(void)
{
    LOG_DEBUG("+------------------------------------------------+");
    LOG_DEBUG("|  CMultimodalPriorTest::testMarginalLikelihood  |");
    LOG_DEBUG("+------------------------------------------------+");

    typedef std::vector<boost::math::normal_distribution<> > TNormalVec;

    // Check that the c.d.f. <= 1 at extreme.
    {
        CMultimodalPrior filter(makePrior());

        const double shape = 1.0;
        const double scale = 1.0;
        const double location = 2.0;
        const double squareScale = 0.5;

        test::CRandomNumbers rng;

        TDoubleVec samples;
        rng.generateGammaSamples(shape, scale, 100, samples);
        filter.addSamples(samples);
        rng.generateLogNormalSamples(location, squareScale, 100, samples);
        filter.addSamples(samples);

        maths_t::ESampleWeightStyle weightStyles[] =
            {
                maths_t::E_SampleCountWeight,
                maths_t::E_SampleWinsorisationWeight,
                maths_t::E_SampleCountWeight
            };
        double weights[] = { 0.1, 1.0, 10.0 };

        for (std::size_t i = 0u; i < boost::size(weightStyles); ++i)
        {
            for (std::size_t j = 0u; j < boost::size(weights); ++j)
            {
                double lb, ub;
                filter.minusLogJointCdf(maths_t::TWeightStyleVec(1, weightStyles[i]),
                                        TDouble1Vec(1, 20000.0),
                                        TDouble4Vec1Vec(1, TDouble4Vec(1, weights[j])),
                                        lb, ub);
                LOG_DEBUG("-log(c.d.f) = " << (lb + ub) / 2.0);
                CPPUNIT_ASSERT(lb >= 0.0);
                CPPUNIT_ASSERT(ub >= 0.0);
            }
        }
    }

    // Check that the marginal likelihood and c.d.f. agree for some
    // test data and that the c.d.f. <= 1 and that the expected value
    // of the log likelihood tends to the differential entropy.

    const double decayRates[] = { 0.0, 0.001, 0.01 };

    unsigned int numberSamples[] = { 2u, 20u, 500u };
    const double tolerance = 0.01;

    test::CRandomNumbers rng;

    const double w1 = 0.5;
    const double mean1 = 10.0;
    const double variance1 = 1.0;
    const double w2 = 0.3;
    const double mean2 = 15.0;
    const double variance2 = 2.0;
    const double w3 = 0.2;
    const double mean3 = 25.0;
    const double variance3 = 3.0;
    TDoubleVec samples1;
    rng.generateNormalSamples(mean1, variance1,
                              static_cast<std::size_t>(w1 * 500.0),
                              samples1);
    TDoubleVec samples2;
    rng.generateNormalSamples(mean2, variance2,
                              static_cast<std::size_t>(w2 * 500.0),
                              samples2);
    TDoubleVec samples3;
    rng.generateNormalSamples(mean3, variance3,
                              static_cast<std::size_t>(w3 * 500.0),
                              samples3);
    TDoubleVec samples;
    samples.insert(samples.end(), samples1.begin(), samples1.end());
    samples.insert(samples.end(), samples2.begin(), samples2.end());
    samples.insert(samples.end(), samples3.begin(), samples3.end());
    rng.random_shuffle(samples.begin(), samples.end());

    for (size_t i = 0; i < boost::size(numberSamples); ++i)
    {
        for (size_t j = 0; j < boost::size(decayRates); ++j)
        {
            CMultimodalPrior filter(makePrior(decayRates[j]));

            for (std::size_t k = 0u; k < samples.size(); ++k)
            {
                filter.addSamples(TDouble1Vec(1, samples[k]));
                filter.propagateForwardsByTime(1.0);
                CPPUNIT_ASSERT(filter.checkInvariants());
            }
            LOG_DEBUG("# modes = " << filter.numberModes());

            // We'll check that the p.d.f. is close to the derivative of the
            // c.d.f. at a range of points on the p.d.f.

            const double eps = 1e-4;

            for (size_t k = 5; k < 31; ++k)
            {
                TDouble1Vec sample(1, static_cast<double>(k));

                LOG_DEBUG("number = " << numberSamples[i]
                          << ", sample = " << sample[0]);

                double logLikelihood = 0.0;
                CPPUNIT_ASSERT_EQUAL(maths_t::E_FpNoErrors,
                                     filter.jointLogMarginalLikelihood(sample, logLikelihood));
                double pdf = ::exp(logLikelihood);

                double lowerBound = 0.0, upperBound = 0.0;
                sample[0] -= eps;
                CPPUNIT_ASSERT(filter.minusLogJointCdf(sample, lowerBound, upperBound));
                CPPUNIT_ASSERT_DOUBLES_EQUAL(lowerBound, upperBound, 1e-3);
                double minusLogCdf = (lowerBound + upperBound) / 2.0;
                double cdfAtMinusEps = ::exp(-minusLogCdf);
                CPPUNIT_ASSERT(minusLogCdf >= 0.0);

                sample[0] += 2.0 * eps;
                CPPUNIT_ASSERT(filter.minusLogJointCdf(sample, lowerBound, upperBound));
                CPPUNIT_ASSERT_DOUBLES_EQUAL(lowerBound, upperBound, 1e-3);
                minusLogCdf = (lowerBound + upperBound) / 2.0;
                double cdfAtPlusEps = ::exp(-minusLogCdf);
                CPPUNIT_ASSERT(minusLogCdf >= 0.0);

                double dcdfdx = (cdfAtPlusEps - cdfAtMinusEps) / 2.0 / eps;

                LOG_DEBUG("pdf(x) = " << pdf << ", d(cdf)/dx = " << dcdfdx);

                CPPUNIT_ASSERT_DOUBLES_EQUAL(pdf, dcdfdx, tolerance);
            }
        }
    }

    {
        // Test that the sample expectation of the log likelihood tends
        // to the expected log likelihood, which is just the differential
        // entropy.

        CMultimodalPrior filter(makePrior());
        filter.addSamples(samples);
        LOG_DEBUG("# modes = " << filter.numberModes());

        TDoubleVec manySamples1;
        rng.generateNormalSamples(mean1, variance1,
                                  static_cast<std::size_t>(w1 * 100000.0),
                                  manySamples1);
        TDoubleVec manySamples2;
        rng.generateNormalSamples(mean2, variance2,
                                  static_cast<std::size_t>(w2 * 100000.0),
                                  manySamples2);
        TDoubleVec manySamples3;
        rng.generateNormalSamples(mean3, variance3,
                                  static_cast<std::size_t>(w3 * 100000.0),
                                  manySamples3);
        TDoubleVec manySamples;
        manySamples.insert(manySamples.end(), manySamples1.begin(), manySamples1.end());
        manySamples.insert(manySamples.end(), manySamples2.begin(), manySamples2.end());
        manySamples.insert(manySamples.end(), manySamples3.begin(), manySamples3.end());
        rng.random_shuffle(manySamples.begin(), manySamples.end());

        TDoubleVec weights;
        weights.push_back(w1);
        weights.push_back(w2);
        weights.push_back(w3);
        TNormalVec modes;
        modes.push_back(boost::math::normal_distribution<>(mean1, variance1));
        modes.push_back(boost::math::normal_distribution<>(mean2, variance2));
        modes.push_back(boost::math::normal_distribution<>(mean3, variance3));
        maths::CMixtureDistribution<boost::math::normal_distribution<> > f(weights, modes);
        double expectedDifferentialEntropy = maths::CTools::differentialEntropy(f);

        double differentialEntropy = 0.0;
        for (std::size_t i = 0u; i < manySamples.size(); ++i)
        {
            if (i % 1000 == 0)
            {
                LOG_DEBUG("Processed " << i << " samples");
            }
            TDouble1Vec sample(1, manySamples[i]);
            filter.addSamples(sample);
            double logLikelihood = 0.0;
            CPPUNIT_ASSERT_EQUAL(maths_t::E_FpNoErrors,
                                 filter.jointLogMarginalLikelihood(sample, logLikelihood));
            differentialEntropy -= logLikelihood;
        }

        differentialEntropy /= static_cast<double>(manySamples.size());

        LOG_DEBUG("differentialEntropy = " << differentialEntropy
                  << ", expectedDifferentialEntropy = " << expectedDifferentialEntropy);

        CPPUNIT_ASSERT_DOUBLES_EQUAL(expectedDifferentialEntropy,
                                     differentialEntropy,
                                     0.05 * expectedDifferentialEntropy);
    }
}

void CMultimodalPriorTest::testMarginalLikelihoodMode(void)
{
    LOG_DEBUG("+----------------------------------------------------+");
    LOG_DEBUG("|  CMultimodalPriorTest::testMarginalLikelihoodMode  |");
    LOG_DEBUG("+----------------------------------------------------+");

    // Test that the marginal likelihood mode is at a local
    // minimum of the likelihood function. And we don't find
    // a higher likelihood location with high probability.

    test::CRandomNumbers rng;

    double w1 = 0.1;
    double mean1 = 1.0;
    double variance1 = 1.0;
    double w2 = 0.9;
    double mean2 = 8.0;
    double variance2 = 1.5;
    TDoubleVec samples1;
    rng.generateNormalSamples(mean1, variance1,
                              static_cast<std::size_t>(w1 * 500.0),
                              samples1);
    TDoubleVec samples2;
    rng.generateNormalSamples(mean2, variance2,
                              static_cast<std::size_t>(w2 * 500.0),
                              samples2);
    TDoubleVec samples;
    samples.insert(samples.end(), samples1.begin(), samples1.end());
    samples.insert(samples.end(), samples2.begin(), samples2.end());
    rng.random_shuffle(samples.begin(), samples.end());

    const double varianceScales[] =
        {
            0.1, 0.2, 0.3, 0.4, 0.5,
            0.6, 0.7, 0.8, 0.9, 1.0,
            1.2, 1.5, 2.0, 2.5, 3.0,
            4.0, 5.0
        };

    CMultimodalPrior filter(makePrior());
    filter.addSamples(samples);

    maths_t::TWeightStyleVec weightStyle(1, maths_t::E_SampleCountVarianceScaleWeight);
    TDouble4Vec weight(1, 1.0);
    TDouble4Vec1Vec weights(1, weight);

    std::size_t totalCount = 0u;
    for (std::size_t i = 0u; i < boost::size(varianceScales); ++i)
    {
        double vs = varianceScales[i];
        weight[0] = vs;
        weights[0][0] = vs;
        LOG_DEBUG("*** vs = " << vs << " ***");
        double mode = filter.marginalLikelihoodMode(weightStyle, weight);
        LOG_DEBUG("marginalLikelihoodMode = " << mode);
        // Should be near 8.
        CPPUNIT_ASSERT_DOUBLES_EQUAL(8.0,
                                     filter.marginalLikelihoodMode(weightStyle, weight),
                                     2.0);
        double eps = 0.01;
        double modeMinusEps = mode - eps;
        double modePlusEps  = mode + eps;
        double fMode, fModeMinusEps, fModePlusEps;
        filter.jointLogMarginalLikelihood(weightStyle,
                                          TDouble1Vec(1, mode),
                                          weights,
                                          fMode);
        filter.jointLogMarginalLikelihood(weightStyle,
                                          TDouble1Vec(1, modeMinusEps),
                                          weights,
                                          fModeMinusEps);
        filter.jointLogMarginalLikelihood(weightStyle,
                                          TDouble1Vec(1, modePlusEps),
                                          weights,
                                          fModePlusEps);
        fMode = ::exp(fMode);
        fModeMinusEps = ::exp(fModeMinusEps);
        fModePlusEps  = ::exp(fModePlusEps);
        double gradient = (fModePlusEps - fModeMinusEps) / 2.0 / eps;
        LOG_DEBUG("f(mode) = " << fMode
                  << ", f(mode-eps) = " << fModeMinusEps
                  << ", f(mode + eps) = " << fModePlusEps);
        LOG_DEBUG("gradient = " << gradient);
        CPPUNIT_ASSERT(::fabs(gradient) < 0.05);
        CPPUNIT_ASSERT(fMode > 0.999 * fModeMinusEps);
        CPPUNIT_ASSERT(fMode > 0.999 * fModePlusEps);
        TDoubleVec trials;
        rng.generateUniformSamples(mean1, mean2, 500, trials);
        std::size_t count = 0u;
        TDoubleVec fTrials;
        for (std::size_t j = 0u; j < trials.size(); ++j)
        {
            double fTrial;
            filter.jointLogMarginalLikelihood(weightStyle,
                                              TDouble1Vec(1, trials[j]),
                                              weights,
                                              fTrial);
            fTrial = ::exp(fTrial);
            if (fTrial > fMode)
            {
                LOG_DEBUG("f(" << trials[j] << ") = " << fTrial << " > " << fMode);
                ++count;
            }
            fTrials.push_back(fTrial);
        }
        LOG_DEBUG("count = " << count);
        CPPUNIT_ASSERT(count < 5);
        totalCount += count;
    }

    LOG_DEBUG("totalCount = " << totalCount);
    CPPUNIT_ASSERT(totalCount < 7);
}

void CMultimodalPriorTest::testMarginalLikelihoodConfidenceInterval(void)
{
    LOG_DEBUG("+------------------------------------------------------------------+");
    LOG_DEBUG("|  CMultimodalPriorTest::testMarginalLikelihoodConfidenceInterval  |");
    LOG_DEBUG("+------------------------------------------------------------------+");

    // Test that marginal likelihood confidence intervals are
    // what we'd expect for various variance scales.

    typedef maths::CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;

    test::CRandomNumbers rng;

    double w1 = 0.2;
    double location1 = 0.1;
    double squareScale1 = 0.2;
    double w2 = 0.8;
    double mean2 = 8.0;
    double variance2 = 2.0;
    TDoubleVec samples1;
    rng.generateLogNormalSamples(location1, squareScale1,
                                 static_cast<std::size_t>(w1 * 2000.0),
                                 samples1);
    TDoubleVec samples2;
    rng.generateNormalSamples(mean2, variance2,
                              static_cast<std::size_t>(w2 * 2000.0),
                              samples2);
    TDoubleVec samples;
    samples.insert(samples.end(), samples1.begin(), samples1.end());
    samples.insert(samples.end(), samples2.begin(), samples2.end());
    rng.random_shuffle(samples.begin(), samples.end());

    const double varianceScales[] =
        {
            0.1, 0.2, 0.3, 0.4, 0.5,
            0.6, 0.7, 0.8, 0.9, 1.0,
            1.2, 1.5, 2.0, 2.5, 3.0,
            4.0, 5.0
        };

    const double percentages[] =
        {
            5.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 95.0, 99.0, 99.9, 99.99
        };

    CMultimodalPrior filter(makePrior());
    filter.addSamples(samples);

    for (std::size_t i = 0u; i < boost::size(varianceScales); ++i)
    {
        LOG_DEBUG("*** vs = " << varianceScales[i] << " ***");
        TMeanAccumulator error;
        for (std::size_t j = 0u; j < boost::size(percentages); ++j)
        {
            LOG_DEBUG("** percentage = " << percentages[j] << " **");
            double q1, q2;
            filter.marginalLikelihoodQuantileForTest(50.0 - percentages[j] / 2.0, 1e-3, q1);
            filter.marginalLikelihoodQuantileForTest(50.0 + percentages[j] / 2.0, 1e-3, q2);
            TDoubleDoublePr interval = filter.marginalLikelihoodConfidenceInterval(percentages[j]);
            LOG_DEBUG("[q1, q2] = [" << q1 << ", " << q2 << "]"
                      << ", interval = " << core::CContainerPrinter::print(interval));
            CPPUNIT_ASSERT_DOUBLES_EQUAL(q1, interval.first, 0.1);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(q2, interval.second, 0.05);
            error.add(::fabs(interval.first - q1));
            error.add(::fabs(interval.second - q2));
        }
        LOG_DEBUG("error = " << maths::CBasicStatistics::mean(error));
        CPPUNIT_ASSERT(maths::CBasicStatistics::mean(error) < 5e-3);
    }

    std::sort(samples.begin(), samples.end());
    TMeanAccumulator error;
    for (std::size_t i = 0u; i < boost::size(percentages); ++i)
    {
        LOG_DEBUG("** percentage = " << percentages[i] << " **");
        std::size_t i1 = static_cast<std::size_t>(
                             static_cast<double>(samples.size())
                             * (50.0 - percentages[i] / 2.0) / 100.0 + 0.5);
        std::size_t i2 = static_cast<std::size_t>(
                             static_cast<double>(samples.size())
                             * (50.0 + percentages[i] / 2.0) / 100.0 + 0.5);
        double q1 = samples[i1];
        double q2 = samples[std::min(i2, samples.size() - 1)];
        TDoubleDoublePr interval = filter.marginalLikelihoodConfidenceInterval(percentages[i]);
        LOG_DEBUG("[q1, q2] = [" << q1 << ", " << q2 << "]"
                  << ", interval = " << core::CContainerPrinter::print(interval));
        CPPUNIT_ASSERT_DOUBLES_EQUAL(q1, interval.first, std::max(0.1 * q1, 0.15));
        CPPUNIT_ASSERT_DOUBLES_EQUAL(q2, interval.second, 0.1 * q2);
        error.add(::fabs(interval.first - q1) / q1);
        error.add(::fabs(interval.second - q2) / q2);
    }
    LOG_DEBUG("error = " << maths::CBasicStatistics::mean(error));
    CPPUNIT_ASSERT(maths::CBasicStatistics::mean(error) < 0.05);
}

void CMultimodalPriorTest::testSampleMarginalLikelihood(void)
{
    LOG_DEBUG("+------------------------------------------------------+");
    LOG_DEBUG("|  CMultimodalPriorTest::testSampleMarginalLikelihood  |");
    LOG_DEBUG("+------------------------------------------------------+");

    // We're going to test two properties of the sampling:
    //   1) That the sample mean is equal to the marginal likelihood
    //      mean.
    //   2) That the sample percentiles match the distribution
    //      percentiles.
    //   3) That the sample mean, variance and skew are all close to
    //      the corresponding quantities in the training data.
    //
    // I want to cross check these with the implementations of the
    // jointLogMarginalLikelihood and minusLogJointCdf so use these
    // to compute the mean and percentiles.

    typedef maths::CBasicStatistics::SSampleMean<double>::TAccumulator TMeanAccumulator;
    typedef maths::CBasicStatistics::SSampleMeanVar<double>::TAccumulator TMeanVarAccumulator;
    typedef maths::CBasicStatistics::SSampleMeanVarSkew<double>::TAccumulator TMeanVarSkewAccumulator;

    const double eps = 1e-3;

    test::CRandomNumbers rng;

    TDoubleVec samples1;
    rng.generateNormalSamples(50.0, 1.0, 150, samples1);
    TDoubleVec samples2;
    rng.generateNormalSamples(57.0, 1.0, 100, samples2);
    TDoubleVec samples;
    samples.insert(samples.end(), samples1.begin(), samples1.end());
    samples.insert(samples.end(), samples2.begin(), samples2.end());
    rng.random_shuffle(samples.begin(), samples.end());

    CMultimodalPrior filter(makePrior());

    TDouble1Vec sampled;

    TMeanVarSkewAccumulator sampleMoments;

    for (std::size_t i = 0u; i < 3u; ++i)
    {
        LOG_DEBUG("sample = " << samples[i]);

        sampleMoments.add(samples[i]);
        filter.addSamples(TDouble1Vec(1, samples[i]));

        sampled.clear();
        filter.sampleMarginalLikelihood(10, sampled);
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), sampled.size());
    }

    TMeanAccumulator meanMeanError;
    TMeanAccumulator meanVarError;

    std::size_t numberSampled = 20u;
    for (std::size_t i = 3u; i < samples.size(); ++i)
    {
        LOG_DEBUG("sample = " << samples[i]);

        sampleMoments.add(samples[i]);
        filter.addSamples(TDouble1Vec(1, samples[i]));

        sampled.clear();
        filter.sampleMarginalLikelihood(numberSampled, sampled);
        CPPUNIT_ASSERT_EQUAL(numberSampled, sampled.size());

        {
            TMeanVarAccumulator sampledMoments;
            sampledMoments = std::for_each(sampled.begin(), sampled.end(), sampledMoments);

            LOG_DEBUG("expectedMean = " << filter.marginalLikelihoodMean()
                      << ", sampledMean = " << maths::CBasicStatistics::mean(sampledMoments));
            LOG_DEBUG("expectedVariance = " << filter.marginalLikelihoodVariance()
                      << ", sampledVariance = " << maths::CBasicStatistics::variance(sampledMoments));

            CPPUNIT_ASSERT_DOUBLES_EQUAL(filter.marginalLikelihoodMean(),
                                         maths::CBasicStatistics::mean(sampledMoments),
                                         0.005 * filter.marginalLikelihoodMean());
            CPPUNIT_ASSERT_DOUBLES_EQUAL(filter.marginalLikelihoodVariance(),
                                         maths::CBasicStatistics::variance(sampledMoments),
                                         0.2 * filter.marginalLikelihoodVariance());
            meanMeanError.add(  ::fabs(  filter.marginalLikelihoodMean()
                                       - maths::CBasicStatistics::mean(sampledMoments))
                              / filter.marginalLikelihoodMean());
            meanVarError.add(::fabs(  filter.marginalLikelihoodVariance()
                                    - maths::CBasicStatistics::variance(sampledMoments))
                             / filter.marginalLikelihoodVariance());
        }

        std::sort(sampled.begin(), sampled.end());
        for (std::size_t j = 1u; j < sampled.size(); ++j)
        {
            double q = 100.0 * static_cast<double>(j)
                             / static_cast<double>(sampled.size());

            double expectedQuantile;
            CPPUNIT_ASSERT(filter.marginalLikelihoodQuantileForTest(q, eps, expectedQuantile));

            LOG_DEBUG("quantile = " << q
                      << ", x_quantile = " << expectedQuantile
                      << ", quantile range = [" << sampled[j - 1] << "," << sampled[j] << "]");

            CPPUNIT_ASSERT(expectedQuantile >= 0.98 * sampled[j - 1]);
            CPPUNIT_ASSERT(expectedQuantile <= 1.02 * sampled[j]);
        }
    }

    LOG_DEBUG("mean mean error = " << maths::CBasicStatistics::mean(meanMeanError));
    CPPUNIT_ASSERT(maths::CBasicStatistics::mean(meanMeanError) < 0.0015);
    LOG_DEBUG("mean variance error = " << maths::CBasicStatistics::mean(meanVarError));
    CPPUNIT_ASSERT(maths::CBasicStatistics::mean(meanVarError) < 0.04);

    sampled.clear();
    filter.sampleMarginalLikelihood(numberSampled, sampled);
    TMeanVarSkewAccumulator sampledMoments;
    for (std::size_t i = 0u; i < sampled.size(); ++i)
    {
        sampledMoments.add(sampled[i]);
    }
    LOG_DEBUG("Sample moments = " << sampledMoments
              << ", sampled moments = " << sampleMoments);
    CPPUNIT_ASSERT_DOUBLES_EQUAL(maths::CBasicStatistics::mean(sampleMoments),
                                 maths::CBasicStatistics::mean(sampledMoments),
                                 1e-4 * maths::CBasicStatistics::mean(sampleMoments));
    CPPUNIT_ASSERT_DOUBLES_EQUAL(maths::CBasicStatistics::variance(sampleMoments),
                                 maths::CBasicStatistics::variance(sampledMoments),
                                 0.05 * maths::CBasicStatistics::variance(sampleMoments));
    CPPUNIT_ASSERT_DOUBLES_EQUAL(maths::CBasicStatistics::skewness(sampleMoments),
                                 maths::CBasicStatistics::skewness(sampledMoments),
                                 0.1 * maths::CBasicStatistics::skewness(sampleMoments));
}

void CMultimodalPriorTest::testCdf(void)
{
    LOG_DEBUG("+---------------------------------+");
    LOG_DEBUG("|  CMultimodalPriorTest::testCdf  |");
    LOG_DEBUG("+---------------------------------+");

    // Test error cases.
    //
    // Test some invariants:
    //   "cdf" + "cdf complement" = 1
    //    cdf x for x < 0 = 1
    //    cdf complement x for x < 0 = 0

    const double locations[] = { 1.0, 3.0 };
    const double squareScales[] = { 0.5, 0.3 };
    const std::size_t n[] = { 100u, 100u };

    test::CRandomNumbers rng;

    CGammaRateConjugate gamma(
            maths::CGammaRateConjugate::nonInformativePrior(maths_t::E_ContinuousData));
    CLogNormalMeanPrecConjugate logNormal(
            maths::CLogNormalMeanPrecConjugate::nonInformativePrior(maths_t::E_ContinuousData));
    COneOfNPrior::TPriorPtrVec priors;
    priors.push_back(COneOfNPrior::TPriorPtr(gamma.clone()));
    priors.push_back(COneOfNPrior::TPriorPtr(logNormal.clone()));
    COneOfNPrior modePrior(maths::COneOfNPrior(priors, maths_t::E_ContinuousData));
    CMultimodalPrior filter(makePrior(&modePrior));

    for (std::size_t i = 0u; i < boost::size(n); ++i)
    {
        TDoubleVec samples;
        rng.generateLogNormalSamples(locations[i], squareScales[i], n[i], samples);
        filter.addSamples(samples);
    }

    double lowerBound;
    double upperBound;
    CPPUNIT_ASSERT(!filter.minusLogJointCdf(TDouble1Vec(), lowerBound, upperBound));
    CPPUNIT_ASSERT(!filter.minusLogJointCdfComplement(TDouble1Vec(), lowerBound, upperBound));

    CPPUNIT_ASSERT(filter.minusLogJointCdf(TDouble1Vec(1, -1.0), lowerBound, upperBound));
    double f = (lowerBound + upperBound) / 2.0;
    CPPUNIT_ASSERT(filter.minusLogJointCdfComplement(TDouble1Vec(1, -1.0), lowerBound, upperBound));
    double fComplement = (lowerBound + upperBound) / 2.0;
    LOG_DEBUG("log(F(x)) = " << -f
              << ", log(1 - F(x)) = " << fComplement);
    CPPUNIT_ASSERT_DOUBLES_EQUAL(::log(std::numeric_limits<double>::min()), -f, 1e-8);
    CPPUNIT_ASSERT_DOUBLES_EQUAL(1.0, ::exp(-fComplement), 1e-8);

    for (std::size_t j = 1u; j < 1000; ++j)
    {
        double x = static_cast<double>(j) / 2.0;

        CPPUNIT_ASSERT(filter.minusLogJointCdf(TDouble1Vec(1, x), lowerBound, upperBound));
        f = (lowerBound + upperBound) / 2.0;
        CPPUNIT_ASSERT(filter.minusLogJointCdfComplement(TDouble1Vec(1, x), lowerBound, upperBound));
        fComplement = (lowerBound + upperBound) / 2.0;

        LOG_DEBUG("log(F(x)) = " << (f == 0.0 ? f : -f)
                  << ", log(1 - F(x)) = " << (fComplement == 0.0 ? fComplement : -fComplement));
        CPPUNIT_ASSERT_DOUBLES_EQUAL(1.0, ::exp(-f) + ::exp(-fComplement), 1e-8);
    }
}

void CMultimodalPriorTest::testProbabilityOfLessLikelySamples(void)
{
    LOG_DEBUG("+------------------------------------------------------------+");
    LOG_DEBUG("|  CMultimodalPriorTest::testProbabilityOfLessLikelySamples  |");
    LOG_DEBUG("+------------------------------------------------------------+");

    typedef std::vector<boost::math::normal_distribution<> > TNormalVec;
    typedef std::vector<boost::math::lognormal_distribution<> > TLogNormalVec;
    typedef std::vector<boost::math::gamma_distribution<> > TGammaVec;

    test::CRandomNumbers rng;

    {
        double weight1 = 0.5, weight2 = 0.5;
        double mean1 = 50.0, mean2 = 57.0;
        double variance1 = 1.0, variance2 = 1.0;

        TDoubleVec samples1;
        rng.generateNormalSamples(mean1, variance1,
                                  static_cast<std::size_t>(10000.0 * weight1),
                                  samples1);
        TDoubleVec samples2;
        rng.generateNormalSamples(mean2, variance2,
                                  static_cast<std::size_t>(10000.0 * weight2),
                                  samples2);
        TDoubleVec samples;
        samples.insert(samples.end(), samples1.begin(), samples1.end());
        samples.insert(samples.end(), samples2.begin(), samples2.end());
        rng.random_shuffle(samples.begin(), samples.end());

        TDoubleVec weights;
        weights.push_back(weight1);
        weights.push_back(weight2);
        TNormalVec modes;
        modes.push_back(boost::math::normal_distribution<>(mean1, variance1));
        modes.push_back(boost::math::normal_distribution<>(mean2, variance2));
        maths::CMixtureDistribution<boost::math::normal_distribution<> > mixture(weights, modes);

        CMultimodalPrior filter(makePrior());
        filter.addSamples(samples);
        LOG_DEBUG("# modes = " << filter.numberModes());

        double x[] = { 46.0, 49.0, 54.0, 55.0, 68.0 };

        double error = 0.0;

        for (std::size_t i = 0u; i < boost::size(x); ++i)
        {
            double expectedProbability;
            double deviation;
            probabilityOfLessLikelySample(mixture, x[i], expectedProbability, deviation);

            double lowerBound;
            double upperBound;
            filter.probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                  TDouble1Vec(1, x[i]),
                                                  lowerBound, upperBound);
            LOG_DEBUG("lowerBound = " << lowerBound
                      << ", upperBound = " << upperBound
                      << ", expectedProbability = " << expectedProbability
                      << ", deviation = " << deviation);

            double probability = (lowerBound + upperBound) / 2.0;
            error +=  probability < expectedProbability - 2.0 * deviation ?
                      (expectedProbability - 2.0 * deviation) - probability :
                     (probability > expectedProbability + 2.0 * deviation ?
                      probability - (expectedProbability + 2.0 * deviation) : 0.0);

            CPPUNIT_ASSERT_DOUBLES_EQUAL(expectedProbability,
                                         probability,
                                         std::max(3.0 * deviation, 3e-5));
        }

        error /= static_cast<double>(boost::size(x));
        LOG_DEBUG("error = " << error);
        CPPUNIT_ASSERT(error < 0.001);

        double lb, ub;
        maths_t::ETail tail;
        filter.probabilityOfLessLikelySamples(
                       maths_t::E_TwoSided,
                       maths_t::TWeightStyleVec(1, maths_t::E_SampleCountVarianceScaleWeight),
                       TDouble1Vec(1, 49.0),
                       TDouble4Vec1Vec(1, TDouble4Vec(1, 1.0)),
                       lb, ub, tail);
        CPPUNIT_ASSERT_EQUAL(maths_t::E_LeftTail, tail);
        filter.probabilityOfLessLikelySamples(
                       maths_t::E_TwoSided,
                       maths_t::TWeightStyleVec(1, maths_t::E_SampleCountVarianceScaleWeight),
                       TDouble1Vec(1, 54.0),
                       TDouble4Vec1Vec(1, TDouble4Vec(1, 1.0)),
                       lb, ub, tail);
        CPPUNIT_ASSERT_EQUAL(maths_t::E_MixedOrNeitherTail, tail);
        filter.probabilityOfLessLikelySamples(
                       maths_t::E_TwoSided,
                       maths_t::TWeightStyleVec(1, maths_t::E_SampleCountVarianceScaleWeight),
                       TDouble1Vec(1, 59.0),
                       TDouble4Vec1Vec(1, TDouble4Vec(1, 1.0)),
                       lb, ub, tail);
        CPPUNIT_ASSERT_EQUAL(maths_t::E_RightTail, tail);
    }
    {
        double weights[] = { 0.6, 0.2, 0.2 };
        double locations[] = { 1.0, 2.5, 4.0 };
        double squareScales[] = { 0.1, 0.05, 0.3 };

        TDoubleVec samples;
        samples.reserve(20000u);
        for (std::size_t i = 0u; i < boost::size(weights); ++i)
        {
            TDoubleVec modeSamples;
            rng.generateLogNormalSamples(locations[i], squareScales[i],
                                         static_cast<std::size_t>(20000.0 * weights[i]),
                                         modeSamples);
            samples.insert(samples.end(), modeSamples.begin(), modeSamples.end());
        }
        rng.random_shuffle(samples.begin(), samples.end());

        TDoubleVec mixtureWeights(boost::begin(weights), boost::end(weights));
        TLogNormalVec modes;
        modes.push_back(boost::math::lognormal_distribution<>(locations[0], ::sqrt(squareScales[0])));
        modes.push_back(boost::math::lognormal_distribution<>(locations[1], ::sqrt(squareScales[1])));
        modes.push_back(boost::math::lognormal_distribution<>(locations[2], ::sqrt(squareScales[2])));
        maths::CMixtureDistribution<boost::math::lognormal_distribution<> > mixture(mixtureWeights, modes);

        CMultimodalPrior filter(makePrior());
        filter.addSamples(samples);
        LOG_DEBUG("# modes = " << filter.numberModes());

        double x[] = { 2.0, 3.0, 9.0, 15.0, 18.0, 22.0, 40.0, 60.0, 80.0, 110.0 };

        double error = 0.0;

        for (std::size_t i = 0u; i < boost::size(x); ++i)
        {
            double expectedProbability;
            double deviation;
            probabilityOfLessLikelySample(mixture, x[i], expectedProbability, deviation);

            double lowerBound;
            double upperBound;
            filter.probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                  TDouble1Vec(1, x[i]),
                                                  lowerBound, upperBound);
            LOG_DEBUG("lowerBound = " << lowerBound
                      << ", upperBound = " << upperBound
                      << ", expectedProbability = " << expectedProbability
                      << ", deviation = " << deviation);

            double probability = (lowerBound + upperBound) / 2.0;
            error +=  probability < expectedProbability - 2.0 * deviation ?
                      (expectedProbability - 2.0 * deviation) - probability :
                     (probability > expectedProbability + 2.0 * deviation ?
                      probability - (expectedProbability + 2.0 * deviation) : 0.0);

            CPPUNIT_ASSERT_DOUBLES_EQUAL(expectedProbability,
                                         probability,
                                         std::min(0.2 * expectedProbability + std::max(3.0 * deviation, 1e-10), 0.06));
        }

        error /= static_cast<double>(boost::size(x));
        LOG_DEBUG("error = " << error);
        CPPUNIT_ASSERT(error < 0.009);
    }
    {
        double weights[] = { 0.6, 0.4 };
        double shapes[] = { 2.0, 300.0 };
        double scales[] = { 0.5, 1.5 };

        TDoubleVec samples;
        samples.reserve(20000u);
        for (std::size_t i = 0u; i < boost::size(weights); ++i)
        {
            TDoubleVec modeSamples;
            rng.generateGammaSamples(shapes[i], scales[i],
                                     static_cast<std::size_t>(20000.0 * weights[i]),
                                     modeSamples);
            samples.insert(samples.end(), modeSamples.begin(), modeSamples.end());
        }
        rng.random_shuffle(samples.begin(), samples.end());

        TDoubleVec mixtureWeights(boost::begin(weights), boost::end(weights));
        TGammaVec modes;
        modes.push_back(boost::math::gamma_distribution<>(shapes[0], scales[0]));
        modes.push_back(boost::math::gamma_distribution<>(shapes[1], scales[1]));
        maths::CMixtureDistribution<boost::math::gamma_distribution<> > mixture(mixtureWeights, modes);

        CMultimodalPrior filter(makePrior());
        filter.addSamples(samples);
        LOG_DEBUG("# modes = " << filter.numberModes());

        double x[] = { 0.5, 1.5, 3.0, 35.0, 100.0, 320.0, 340.0, 360.0, 380.0, 410.0 };

        double error = 0.0;

        for (std::size_t i = 0u; i < boost::size(x); ++i)
        {
            double expectedProbability;
            double deviation;
            probabilityOfLessLikelySample(mixture, x[i], expectedProbability, deviation);

            double lowerBound;
            double upperBound;
            filter.probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                  TDouble1Vec(1, x[i]),
                                                  lowerBound, upperBound);
            LOG_DEBUG("lowerBound = " << lowerBound
                      << ", upperBound = " << upperBound
                      << ", expectedProbability = " << expectedProbability
                      << ", deviation = " << deviation);

            double probability = (lowerBound + upperBound) / 2.0;
            error +=  probability < expectedProbability - 2.0 * deviation ?
                      (expectedProbability - 2.0 * deviation) - probability :
                     (probability > expectedProbability + 2.0 * deviation ?
                      probability - (expectedProbability + 2.0 * deviation) : 0.0);

            CPPUNIT_ASSERT_DOUBLES_EQUAL(expectedProbability,
                                         probability,
                                         0.18 * expectedProbability + std::max(2.5 * deviation, 1e-3));
        }

        error /= static_cast<double>(boost::size(x));
        LOG_DEBUG("error = " << error);
        CPPUNIT_ASSERT(error < 0.02);
    }
}

void CMultimodalPriorTest::testPersist(void)
{
    LOG_DEBUG("+-------------------------------------+");
    LOG_DEBUG("|  CMultimodalPriorTest::testPersist  |");
    LOG_DEBUG("+-------------------------------------+");

    maths::CScopeDisableNormalizeOnRestore disabler;

    test::CRandomNumbers rng;

    TDoubleVec samples1;
    rng.generateNormalSamples(5.0, 1.0, 100, samples1);
    TDoubleVec samples2;
    rng.generateLogNormalSamples(3.0, 0.1, 200, samples2);
    TDoubleVec samples;
    samples.insert(samples.end(), samples1.begin(), samples1.end());
    samples.insert(samples.end(), samples2.begin(), samples2.end());
    rng.random_shuffle(samples.begin(), samples.end());

    maths::CXMeansOnline1d clusterer(maths_t::E_ContinuousData,
                                     maths::CAvailableModeDistributions::ALL,
                                     maths_t::E_ClustersFractionWeight);
    maths::CGammaRateConjugate gamma =
            maths::CGammaRateConjugate::nonInformativePrior(maths_t::E_ContinuousData, 0.01);
    maths::CLogNormalMeanPrecConjugate logNormal =
            maths::CLogNormalMeanPrecConjugate::nonInformativePrior(maths_t::E_ContinuousData, 0.01);
    maths::CNormalMeanPrecConjugate normal =
            maths::CNormalMeanPrecConjugate::nonInformativePrior(maths_t::E_ContinuousData);

    COneOfNPrior::TPriorPtrVec priors;
    priors.push_back(COneOfNPrior::TPriorPtr(gamma.clone()));
    priors.push_back(COneOfNPrior::TPriorPtr(logNormal.clone()));
    priors.push_back(COneOfNPrior::TPriorPtr(normal.clone()));
    COneOfNPrior modePrior(maths::COneOfNPrior(priors, maths_t::E_ContinuousData));

    maths::CMultimodalPrior origFilter(maths_t::E_ContinuousData,
                                       clusterer,
                                       modePrior);
    for (std::size_t i = 0u; i < samples.size(); ++i)
    {
        origFilter.addSamples(maths_t::TWeightStyleVec(1, maths_t::E_SampleCountWeight),
                              TDouble1Vec(1, samples[i]),
                              TDouble4Vec1Vec(1, TDouble4Vec(1, 1.0)));
    }
    double decayRate = origFilter.decayRate();
    uint64_t checksum = origFilter.checksum();

    std::string origXml;
    {
        core::CRapidXmlStatePersistInserter inserter("root");
        origFilter.acceptPersistInserter(inserter);
        inserter.toXml(origXml);
    }

    LOG_DEBUG("Multimodal XML representation:\n" << origXml);

    // Restore the XML into a new filter
    core::CRapidXmlParser parser;
    CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(origXml));
    core::CRapidXmlStateRestoreTraverser traverser(parser);

    maths::SDistributionRestoreParams params(maths_t::E_ContinuousData,
                                             decayRate + 0.1,
                                             maths::MINIMUM_CLUSTER_SPLIT_FRACTION,
                                             maths::MINIMUM_CLUSTER_SPLIT_COUNT,
                                             maths::MINIMUM_CATEGORY_COUNT);
    maths::CMultimodalPrior restoredFilter(params, traverser);

    LOG_DEBUG("orig checksum = " << checksum
              << " restored checksum = " << restoredFilter.checksum());
    CPPUNIT_ASSERT_EQUAL(checksum, restoredFilter.checksum());

    // The XML representation of the new filter should be the same as the original
    std::string newXml;
    {
        ml::core::CRapidXmlStatePersistInserter inserter("root");
        restoredFilter.acceptPersistInserter(inserter);
        inserter.toXml(newXml);
    }
    CPPUNIT_ASSERT_EQUAL(origXml, newXml);
}

void CMultimodalPriorTest::testSeasonalVarianceScale(void)
{
    LOG_DEBUG("+---------------------------------------------------+");
    LOG_DEBUG("|  CMultimodalPriorTest::testSeasonalVarianceScale  |");
    LOG_DEBUG("+---------------------------------------------------+");

    // We are test:
    //   1) The marginal likelihood is normalized.
    //   2) E[(X - m)^2] w.r.t. the log-likelihood is scaled.
    //   3) E[(X - m)^2] is close to marginalLikelihoodVariance.
    //   4) dF/dx = exp(log-likelihood) with different scales.
    //   5) The probability of less likely sample transforms as
    //      expected.
    //   6) Updating with scaled samples behaves as expected.

    const double mean1 = 6.0;
    const double variance1 = 4.0;
    const double mean2 = 20.0;
    const double variance2 = 20.0;
    const double mean3 = 50.0;
    const double variance3 = 20.0;

    test::CRandomNumbers rng;

    TDoubleVec samples1;
    rng.generateNormalSamples(mean1, variance1, 100, samples1);
    TDoubleVec samples2;
    rng.generateNormalSamples(mean2, variance2, 100, samples2);
    TDoubleVec samples3;
    rng.generateNormalSamples(mean3, variance3, 100, samples3);

    double varianceScales[] = { 0.2, 0.5, 1.0, 2.0, 5.0 };
    maths_t::TWeightStyleVec weightStyle(1, maths_t::E_SampleSeasonalVarianceScaleWeight);
    TDouble4Vec weight(1, 1.0);
    TDouble4Vec1Vec weights(1, weight);

    double m;
    double v;

    {
        CMultimodalPrior filter(makePrior());
        filter.addSamples(samples1);
        filter.addSamples(samples2);
        filter.addSamples(samples3);
        LOG_DEBUG(filter.printMarginalLikelihoodFunction());

        m = filter.marginalLikelihoodMean();
        v = filter.marginalLikelihoodVariance();
        LOG_DEBUG("v = " << v);

        double points[] = { 0.5, 4.0, 12.0, 20.0, 40.0, 50.0, 60.0 };

        double unscaledExpectationVariance;
        filter.expectation(CVarianceKernel(filter.marginalLikelihoodMean()),
                           50,
                           unscaledExpectationVariance);
        LOG_DEBUG("unscaledExpectationVariance = " << unscaledExpectationVariance);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(v,
                                     unscaledExpectationVariance,
                                     1e-2 * unscaledExpectationVariance);

        for (std::size_t i = 0u; i < boost::size(varianceScales); ++i)
        {
            double vs = varianceScales[i];
            weight[0] = vs;
            weights[0][0] = vs;
            LOG_DEBUG("*** variance scale = " << vs << " ***");

            double Z;
            filter.expectation(C1dUnitKernel(), 50, Z, weightStyle, weight);
            LOG_DEBUG("Z = " << Z);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(1.0, Z, 1e-3);

            LOG_DEBUG("sv = " << filter.marginalLikelihoodVariance(weightStyle, weight));
            double expectationVariance;
            filter.expectation(CVarianceKernel(filter.marginalLikelihoodMean()),
                               50,
                               expectationVariance,
                               weightStyle,
                               weight);
            LOG_DEBUG("expectationVariance = " << expectationVariance);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(vs * unscaledExpectationVariance,
                                         expectationVariance,
                                         1e-3 * vs * unscaledExpectationVariance);
            CPPUNIT_ASSERT_DOUBLES_EQUAL(filter.marginalLikelihoodVariance(weightStyle, weight),
                                         expectationVariance,
                                         1e-3 * filter.marginalLikelihoodVariance(weightStyle, weight));

            TDouble1Vec sample(1, 0.0);
            for (std::size_t j = 0u; j < boost::size(points); ++j)
            {
                TDouble1Vec x(1, points[j]);
                double fx;
                filter.jointLogMarginalLikelihood(weightStyle, x, weights, fx);
                TDouble1Vec xMinusEps(1, points[j] - 1e-3);
                TDouble1Vec xPlusEps(1, points[j] + 1e-3);
                double lb, ub;
                filter.minusLogJointCdf(weightStyle, xPlusEps, weights, lb, ub);
                double FxPlusEps = ::exp(-(lb + ub) / 2.0);
                filter.minusLogJointCdf(weightStyle, xMinusEps, weights, lb, ub);
                double FxMinusEps = ::exp(-(lb + ub) / 2.0);
                LOG_DEBUG("x = " << points[j]
                          << ", log(f(x)) = " << fx
                          << ", log(dF/dx)) = " << ::log((FxPlusEps - FxMinusEps) / 2e-3));
                CPPUNIT_ASSERT_DOUBLES_EQUAL(fx, ::log((FxPlusEps - FxMinusEps) / 2e-3), 0.05 * ::fabs(fx));

                sample[0] = m + (points[j] - m) / ::sqrt(vs);
                weights[0][0] = 1.0;
                double expectedLowerBound;
                double expectedUpperBound;
                maths_t::ETail expectedTail;
                filter.probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                      weightStyle,
                                                      sample,
                                                      weights,
                                                      expectedLowerBound, expectedUpperBound, expectedTail);

                sample[0] = points[j];
                weights[0][0] = vs;
                double lowerBound;
                double upperBound;
                maths_t::ETail tail;
                filter.probabilityOfLessLikelySamples(maths_t::E_TwoSided,
                                                      weightStyle,
                                                      sample,
                                                      weights,
                                                      lowerBound, upperBound, tail);

                LOG_DEBUG("expectedLowerBound = " << expectedLowerBound);
                LOG_DEBUG("lowerBound         = " << lowerBound);
                LOG_DEBUG("expectedUpperBound = " << expectedUpperBound);
                LOG_DEBUG("upperBound         = " << upperBound);
                LOG_DEBUG("expectedTail       = " << expectedTail);
                LOG_DEBUG("tail               = " << tail);

                if ((expectedLowerBound + expectedUpperBound) < 0.02)
                {
                    CPPUNIT_ASSERT_DOUBLES_EQUAL(::log(expectedLowerBound),
                                                 ::log(lowerBound),
                                                 0.1 * ::fabs(::log(expectedLowerBound)));
                    CPPUNIT_ASSERT_DOUBLES_EQUAL(::log(expectedUpperBound),
                                                 ::log(upperBound),
                                                 0.1 * ::fabs(::log(expectedUpperBound)));
                }
                else
                {
                    CPPUNIT_ASSERT_DOUBLES_EQUAL(expectedLowerBound,
                                                 lowerBound,
                                                 0.05 * expectedLowerBound);
                    CPPUNIT_ASSERT_DOUBLES_EQUAL(expectedUpperBound,
                                                 upperBound,
                                                 0.05 * expectedUpperBound);
                }
                CPPUNIT_ASSERT_EQUAL(expectedTail, tail);
            }
        }
    }
    for (std::size_t i = 0u; i < boost::size(varianceScales); ++i)
    {
        double vs = varianceScales[i];

        TDouble1Vec samples(samples1.begin(), samples1.end());
        samples.insert(samples.end(), samples2.begin(), samples2.end());
        samples.insert(samples.end(), samples3.begin(), samples3.end());
        rng.random_shuffle(samples.begin(), samples.end());

        CMultimodalPrior filter(makePrior());
        weights[0][0] = vs;
        for (std::size_t j = 0u; j < samples.size(); ++j)
        {
            filter.addSamples(weightStyle, TDouble1Vec(1, samples[j]), weights);
        }

        double sm = filter.marginalLikelihoodMean();
        double sv = filter.marginalLikelihoodVariance();
        LOG_DEBUG("m  = " << m  << ", v  = " << v);
        LOG_DEBUG("sm = " << sm << ", sv = " << sv);

        CPPUNIT_ASSERT_DOUBLES_EQUAL(m, sm, 0.05 * m);
        CPPUNIT_ASSERT_DOUBLES_EQUAL(v / vs, sv, 0.06 * v / vs);
    }
}

CppUnit::Test *CMultimodalPriorTest::suite(void)
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CMultimodalPriorTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CMultimodalPriorTest>(
                                   "CMultimodalPriorTest::testMultipleUpdate",
                                   &CMultimodalPriorTest::testMultipleUpdate) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMultimodalPriorTest>(
                                   "CMultimodalPriorTest::testPropagation",
                                   &CMultimodalPriorTest::testPropagation) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMultimodalPriorTest>(
                                   "CMultimodalPriorTest::testSingleMode",
                                   &CMultimodalPriorTest::testSingleMode) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMultimodalPriorTest>(
                                   "CMultimodalPriorTest::testMultipleModes",
                                   &CMultimodalPriorTest::testMultipleModes) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMultimodalPriorTest>(
                                   "CMultimodalPriorTest::testMarginalLikelihood",
                                   &CMultimodalPriorTest::testMarginalLikelihood) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMultimodalPriorTest>(
                                   "CMultimodalPriorTest::testMarginalLikelihoodMode",
                                   &CMultimodalPriorTest::testMarginalLikelihoodMode) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMultimodalPriorTest>(
                                   "CMultimodalPriorTest::testMarginalLikelihoodConfidenceInterval",
                                   &CMultimodalPriorTest::testMarginalLikelihoodConfidenceInterval) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMultimodalPriorTest>(
                                   "CMultimodalPriorTest::testSampleMarginalLikelihood",
                                   &CMultimodalPriorTest::testSampleMarginalLikelihood) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMultimodalPriorTest>(
                                   "CMultimodalPriorTest::testCdf",
                                   &CMultimodalPriorTest::testCdf) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMultimodalPriorTest>(
                                   "CMultimodalPriorTest::testProbabilityOfLessLikelySamples",
                                   &CMultimodalPriorTest::testProbabilityOfLessLikelySamples) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMultimodalPriorTest>(
                                   "CMultimodalPriorTest::testSeasonalVarianceScale",
                                   &CMultimodalPriorTest::testSeasonalVarianceScale) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CMultimodalPriorTest>(
                                   "CMultimodalPriorTest::testPersist",
                                   &CMultimodalPriorTest::testPersist) );

    return suiteOfTests;
}
