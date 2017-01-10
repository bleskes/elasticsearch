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

#include "CCounterTest.h"

#include <core/CLogger.h>
#include <core/CRapidXmlParser.h>
#include <core/CRapidXmlStatePersistInserter.h>
#include <core/CRapidXmlStateRestoreTraverser.h>

#include <maths/CCounter.h>

#include <boost/range.hpp>

#include <iterator>
#include <numeric>

#include <math.h>

using namespace ml;
using namespace maths;

namespace
{

typedef std::vector<unsigned int> TUIntVec;
typedef TUIntVec::const_iterator TUIntVecCItr;

double mean(TUIntVecCItr begin, TUIntVecCItr end)
{
    if (begin == end)
    {
        return 0.0;
    }
    unsigned int sum = std::accumulate(begin, end, 0u);
    return static_cast<double>(sum) / static_cast<double>(std::distance(begin, end));
}

}

void CCounterTest::testAll(void)
{
    {
        // Test basic functionality.

        core_t::TTime data[] =
            {
                2, 3, 5, 5, 8, 12, 100, 101, 102, 104, 1000, 1005, 1009
            };

        core_t::TTime startTimes[] = { 0, 1 };
        core_t::TTime bucketLengths[] = { 2, 5, 20, 50 };

        double eps = 1e-10;

        for (size_t i = 0; i < boost::size(startTimes); ++i)
        {
            for (size_t j = 0; j < boost::size(bucketLengths); ++j)
            {
                CCounter counter(startTimes[i], bucketLengths[j]);

                TUIntVec counts(1u, 0u);

                core_t::TTime time = startTimes[i];
                for (size_t k = 0; k < boost::size(data); /**/)
                {
                    if (data[k] < time + bucketLengths[j])
                    {
                        counter.addArrival(data[k]);
                        ++counts.back();
                        ++k;
                    }
                    else
                    {
                        CPPUNIT_ASSERT_EQUAL(static_cast<uint64_t>(counts.back()),
                                             counter.currentBucketCount(time));

                        TUIntVecCItr begin = counts.begin();
                        TUIntVecCItr last = counts.end() - 1;
                        CPPUNIT_ASSERT_DOUBLES_EQUAL(mean(begin, last),
                                                     counter.meanBucketCount(time),
                                                     eps);

                        time += bucketLengths[j];

                        counts.push_back(0);
                    }
                }
            }
        }
    }

    {
        // Test the history.

        core_t::TTime data[] =
            {
                1, 2, 3, 8, 9, 10, 11, 12, 15, 16, 36, 38, 39
            };

        core_t::TTime startTime = 0;
        core_t::TTime bucketLength = 3;

        uint64_t expectedCounts[] =
            {
                2, 1, 1, 3, 1, 2, 0, 0, 0, 0, 0, 0, 2, 1
            };

        std::size_t memoryCapacity = 4u;
        CCounter::TBucketCirBufPtr memory(new CCounter::TBucketCirBuf(memoryCapacity));

        CCounter counter(startTime, bucketLength, memory);

        for (size_t i = 0; i < boost::size(data); ++i)
        {
            counter.addArrival(data[i]);

            core_t::TTime time = data[i] - bucketLength;
            for (std::size_t j = 0; j < memoryCapacity && time > 0; ++j, time -= bucketLength)
            {
                // Compute the bucket corresponding to time accounting
                // for the fact buckets are open above.

                core_t::TTime k = (time - startTime) / bucketLength;

                CPPUNIT_ASSERT_EQUAL(expectedCounts[k],
                                     counter.currentBucketCount(time));
            }
        }
    }
}

void CCounterTest::testPersist(void)
{
    {
        core_t::TTime data[] = { 100, 101, 102, 104, 1000, 1005, 1009 };

        CCounter origCounter(60, 30);
        for (size_t i = 0; i < boost::size(data); ++i)
        {
            origCounter.addArrival(data[i]);
        }

        std::string origXml;
        {
            core::CRapidXmlStatePersistInserter inserter("root");
            origCounter.acceptPersistInserter(inserter);
            inserter.toXml(origXml);
        }

        LOG_DEBUG("Counter XML representation:\n" << origXml);

        // Restore the XML into a new counter
        ml::core::CRapidXmlParser parser;
        CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(origXml));
        core::CRapidXmlStateRestoreTraverser traverser(parser);

        CCounter restoredCounter(traverser);

        // The XML representation of the new counter should be the same as the original
        std::string newXml;
        {
            ml::core::CRapidXmlStatePersistInserter inserter("root");
            restoredCounter.acceptPersistInserter(inserter);
            inserter.toXml(newXml);
        }
        CPPUNIT_ASSERT_EQUAL(origXml, newXml);
    }
    {
        core_t::TTime data[] = { 100, 101, 102, 104, 1000, 1005, 1009, 1500, 1600, 1700 };

        CCounter::TBucketCirBufPtr origMemory(new CCounter::TBucketCirBuf(4));
        CCounter origCounter(60, 30, origMemory);
        for (size_t i = 0; i < boost::size(data); ++i)
        {
            origCounter.addArrival(data[i]);
        }

        std::string origXml;
        {
            core::CRapidXmlStatePersistInserter inserter("root");
            origCounter.acceptPersistInserter(inserter);
            inserter.toXml(origXml);
        }

        LOG_DEBUG("Counter with history XML representation:\n" << origXml);

        // Restore the XML into a new counter
        ml::core::CRapidXmlParser parser;
        CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(origXml));
        core::CRapidXmlStateRestoreTraverser traverser(parser);

        CCounter::TBucketCirBufPtr restoredMemory(new CCounter::TBucketCirBuf(4));
        CCounter restoredCounter(restoredMemory, traverser);

        // The XML representation of the new counter should be the same as the original
        std::string newXml;
        {
            ml::core::CRapidXmlStatePersistInserter inserter("root");
            restoredCounter.acceptPersistInserter(inserter);
            inserter.toXml(newXml);
        }
        CPPUNIT_ASSERT_EQUAL(origXml, newXml);
    }
}

CppUnit::Test* CCounterTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CCounterTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CCounterTest>(
                                   "CCounterTest::testAll",
                                   &CCounterTest::testAll) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CCounterTest>(
                                   "CCounterTest::testPersist",
                                   &CCounterTest::testPersist) );

    return suiteOfTests;
}
