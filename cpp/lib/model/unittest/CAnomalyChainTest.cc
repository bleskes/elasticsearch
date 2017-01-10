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

#include "CAnomalyChainTest.h"

#include <core/CContainerPrinter.h>
#include <core/CLogger.h>

#include <model/CAnomalyChain.h>

#include <test/CRandomNumbers.h>

#include <boost/optional.hpp>
#include <boost/range.hpp>

#include <vector>

using namespace ml;

void CAnomalyChainTest::testAll(void)
{
    LOG_DEBUG("*** CAnomalyChainTest::testAll ***");

    typedef std::vector<std::size_t> TSizeVec;
    typedef std::vector<double> TDoubleVec;
    typedef std::pair<std::size_t, std::size_t> TSizeSizePr;
    typedef std::vector<TSizeSizePr> TSizeSizePrVec;

    // The test data have 5 distinct anomalies.
    //
    // 1) People 1 and 2 buckets 10-15
    // 2) People 5, 17 and 18 (intermittent) buckets 40-50
    // 3) People 21, 23 and 29 (all intermittent) buckets 100-120
    // 4) Person 5 buckets 150-152
    // 5) People 2, 30, 31 (intermittent), 32 (intermittent), 34
    //    35 and 40 buckets 180-190

    double anomalies[][6] =
        {
            { 10,  15,  1.0, 1,  1e-6,  1e-4 },
            { 10,  15,  1.0, 2,  1e-7,  1e-3 },
            { 40,  50,  1.0, 5,  1e-6,  1e-4 },
            { 40,  50,  1.0, 17, 1e-60, 1e-50 },
            { 40,  50,  0.5, 18, 1e-6,  1e-5 },
            { 100, 120, 0.7, 21, 1e-7,  1e-6 },
            { 100, 120, 0.5, 23, 1e-12, 1e-10 },
            { 100, 120, 0.8, 29, 1e-5,  1e-4 },
            { 150, 152, 1.0, 5,  1e-8,  1e-6 },
            { 180, 193, 1.0, 2,  1e-5,  1e-3 },
            { 180, 193, 1.0, 30, 1e-5,  1e-3 },
            { 180, 193, 0.9, 31, 1e-5,  1e-3 },
            { 180, 193, 0.4, 32, 1e-5,  1e-3 },
            { 180, 193, 1.0, 34, 1e-5,  1e-3 },
            { 180, 193, 1.0, 35, 1e-5,  1e-3 },
            { 180, 193, 1.0, 40, 1e-5,  1e-3 }
        };

    model::CAnomalyScore::CComputer computer1(0.5, 0.5, 1, 5, 0.05);
    model::CAnomalyScore::CComputer computer2(0.0, 1.0, 1, 5, 0.05);

    test::CRandomNumbers rng;

    TSizeVec anomaly1;
    model::CAnomalyChain chain1;
    TSizeSizePrVec merged1;

    TSizeVec anomaly2;
    model::CAnomalyChain chain2;
    TSizeSizePrVec merged2;

    for (std::size_t b = 0u; b < 1000; ++b)
    {
        LOG_DEBUG("bucket " << b);

        TSizeVec pids;
        TDoubleVec probabilities;
        for (std::size_t i = 0u; i < boost::size(anomalies); ++i)
        {
            if (   static_cast<double>(b) >= anomalies[i][0]
                && static_cast<double>(b) <  anomalies[i][1])
            {
                TDoubleVec test;
                rng.generateUniformSamples(0.0, 1.0, 1, test);
                if (test[0] < anomalies[i][2])
                {
                    TDoubleVec probability;
                    rng.generateUniformSamples(anomalies[i][4],
                                               anomalies[i][5],
                                               1,
                                               probability);
                    pids.push_back(static_cast<std::size_t>(anomalies[i][3]));
                    probabilities.push_back(probability[0]);
                }
            }
        }

        if (!pids.empty())
        {
            LOG_DEBUG("True anomalies = " << core::CContainerPrinter::print(pids)
                      << " " << core::CContainerPrinter::print(probabilities));
        }

        for (std::size_t i = 0u; i < 41; ++i)
        {
            if (std::find(pids.begin(), pids.end(), i) == pids.end())
            {
                TDoubleVec test;
                rng.generateUniformSamples(0.0, 1.0, 1, test);
                if (test[0] < 0.8)
                {
                    pids.push_back(i);
                    TDoubleVec probability;
                    rng.generateUniformSamples(0.001, 1.0, 1, probability);
                    probabilities.push_back(probability[0]);
                }
            }
        }

        TSizeVec atom1 = model::CAnomalyChain::atom(computer1,
                                                    probabilities,
                                                    pids);
        if (chain1.match(atom1))
        {
            if (anomaly1.empty())
            {
                anomaly1.push_back(b - 1);
            }
            anomaly1.push_back(b);
        }
        else
        {
            if (anomaly1.size() > 1)
            {
                merged1.push_back(TSizeSizePr(anomaly1.front(),
                                              anomaly1.back()));
            }
            anomaly1.clear();
            if (!atom1.empty())
            {
                anomaly1.push_back(b);
            }
        }

        TSizeVec atom2 = model::CAnomalyChain::atom(computer2,
                                                    probabilities,
                                                    pids);
        if (chain2.match(atom2))
        {
            if (anomaly2.empty())
            {
                anomaly2.push_back(b - 1);
            }
            anomaly2.push_back(b);
        }
        else
        {
            if (anomaly2.size() > 1)
            {
                merged2.push_back(TSizeSizePr(anomaly2.front(),
                                              anomaly2.back()));
            }
            anomaly2.clear();
            if (!atom2.empty())
            {
                anomaly2.push_back(b);
            }
        }
    }

    TSizeSizePrVec optimal;
    optimal.push_back(TSizeSizePr(10, 14));
    optimal.push_back(TSizeSizePr(40, 49));
    optimal.push_back(TSizeSizePr(100, 119));
    optimal.push_back(TSizeSizePr(150, 151));
    optimal.push_back(TSizeSizePr(180, 192));

    LOG_DEBUG("optimal = " << core::CContainerPrinter::print(optimal));
    LOG_DEBUG("merged1 = " << core::CContainerPrinter::print(merged1));
    LOG_DEBUG("merged2 = " << core::CContainerPrinter::print(merged2));

    // Check we've merged out a reasonable fraction of the
    // related anomalous buckets.

    double improvement1 = 0.0;
    double improvement2 = 0.0;
    for (std::size_t i = 0u; i < optimal.size(); ++i)
    {
        for (std::size_t j = optimal[i].first;
             j < optimal[i].second;
             ++j)
        {
            std::size_t k = 0u;
            for (/**/; k < merged1.size(); ++k)
            {
                if (j >= merged1[k].first && j <= merged1[k].second)
                {
                    break;
                }
            }
            if (k == merged1.size())
            {
                improvement1 += 1.0;
            }

            k = 0u;
            for (/**/; k < merged2.size(); ++k)
            {
                if (j >= merged2[k].first && j <= merged2[k].second)
                {
                    break;
                }
            }
            if (k == merged2.size())
            {
                improvement2 += 1.0;
            }
        }
    }
    improvement1 += static_cast<double>(merged1.size());
    improvement1 = (50.0 - improvement1) / (50.0 - 5.0);
    improvement2 += static_cast<double>(merged2.size());
    improvement2 = (50.0 - improvement2) / (50.0 - 5.0);
    LOG_DEBUG("improvement1 = " << improvement1);
    LOG_DEBUG("improvement2 = " << improvement2);
    CPPUNIT_ASSERT(improvement1 > 0.59);
    CPPUNIT_ASSERT(improvement2 > 0.67);

    // Check we haven't merged any anomalies we shouldn't have.
    for (std::size_t i = 0u; i < merged1.size(); ++i)
    {
        bool ok = false;
        for (std::size_t j = 0u; !ok && j < optimal.size(); ++j)
        {
            if (   merged1[i].second >= optimal[j].first
                && merged1[i].first < optimal[j].second)
            {
                ok = true;
            }
        }
        CPPUNIT_ASSERT(ok);
    }
    for (std::size_t i = 0u; i < merged2.size(); ++i)
    {
        bool ok = false;
        for (std::size_t j = 0u; j < optimal.size(); ++j)
        {
            if (   merged2[i].second <  optimal[j].first
                || merged2[i].first >= optimal[j].second)
            {
                ok = true;
            }
        }
        CPPUNIT_ASSERT(ok);
    }
}

CppUnit::Test *CAnomalyChainTest::suite(void)
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CAnomalyChainTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CAnomalyChainTest>(
                                   "CAnomalyChainTest::testAll",
                                   &CAnomalyChainTest::testAll) );

    return suiteOfTests;
}
