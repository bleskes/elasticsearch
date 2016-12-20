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
#include "CEuclideanDistanceTest.h"

#include <core/CLogger.h>

#include <maths/CEuclideanDistance.h>


CppUnit::Test *CEuclideanDistanceTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CEuclideanDistanceTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CEuclideanDistanceTest>(
                                   "CEuclideanDistanceTest::testDistances",
                                   &CEuclideanDistanceTest::testDistances) );

    return suiteOfTests;
}

void CEuclideanDistanceTest::testDistances(void)
{
    // 1D test
    // Obviously the distance from 0 to 10 is 10
    {
        prelert::maths::CEuclideanDistance::TDoubleVec coordinates1;
        coordinates1.push_back(0.0);

        prelert::maths::CEuclideanDistance::TDoubleVec coordinates2;
        coordinates2.push_back(10.0);

        double distance = 0.0;
        CPPUNIT_ASSERT(prelert::maths::CEuclideanDistance::euclideanDistance(coordinates1.begin(),
                                                                             coordinates1.end(),
                                                                             coordinates2.begin(),
                                                                             coordinates2.end(),
                                                                             distance));

        CPPUNIT_ASSERT(distance == 10.0);
    }

    // 2D test
    // By Pythagorous's theorem, the distance from (0, 0) to a point with
    // co-ordinates (3, 4) should be 5
    {
        prelert::maths::CEuclideanDistance::TDoubleVec coordinates1;
        coordinates1.push_back(0.0);
        coordinates1.push_back(0.0);

        prelert::maths::CEuclideanDistance::TDoubleVec coordinates2;
        coordinates2.push_back(3.0);
        coordinates2.push_back(4.0);

        double distance = 0.0;
        CPPUNIT_ASSERT(prelert::maths::CEuclideanDistance::euclideanDistance(coordinates1.begin(),
                                                                             coordinates1.end(),
                                                                             coordinates2.begin(),
                                                                             coordinates2.end(),
                                                                             distance));

        CPPUNIT_ASSERT(distance == 5.0);
    }

    // 3D test
    // By Pythagorous's theorem applied twice, the distance from (0, 0, 0) to a
    // point with co-ordinates (3, 4, 12) should be 13
    {
        prelert::maths::CEuclideanDistance::TDoubleVec coordinates1;
        coordinates1.push_back(0.0);
        coordinates1.push_back(0.0);
        coordinates1.push_back(0.0);

        prelert::maths::CEuclideanDistance::TDoubleVec coordinates2;
        coordinates2.push_back(3.0);
        coordinates2.push_back(4.0);
        coordinates2.push_back(12.0);

        double distance = 0.0;
        CPPUNIT_ASSERT(prelert::maths::CEuclideanDistance::euclideanDistance(coordinates1.begin(),
                                                                             coordinates1.end(),
                                                                             coordinates2.begin(),
                                                                             coordinates2.end(),
                                                                             distance));

        CPPUNIT_ASSERT(distance == 13.0);
    }

    // Now the same 3 tests again, but with an offset applied to each of the
    // coordinates - the distances should be the same as before
    {
        prelert::maths::CEuclideanDistance::TDoubleVec coordinates1;
        coordinates1.push_back(7.3);

        prelert::maths::CEuclideanDistance::TDoubleVec coordinates2;
        coordinates2.push_back(17.3);

        double distance = 0.0;
        CPPUNIT_ASSERT(prelert::maths::CEuclideanDistance::euclideanDistance(coordinates1.begin(),
                                                                             coordinates1.end(),
                                                                             coordinates2.begin(),
                                                                             coordinates2.end(),
                                                                             distance));

        CPPUNIT_ASSERT(distance == 10.0);
    }

    {
        prelert::maths::CEuclideanDistance::TDoubleVec coordinates1;
        coordinates1.push_back(4.2);
        coordinates1.push_back(-80.0);

        prelert::maths::CEuclideanDistance::TDoubleVec coordinates2;
        coordinates2.push_back(7.2);
        coordinates2.push_back(-76.0);

        double distance = 0.0;
        CPPUNIT_ASSERT(prelert::maths::CEuclideanDistance::euclideanDistance(coordinates1.begin(),
                                                                             coordinates1.end(),
                                                                             coordinates2.begin(),
                                                                             coordinates2.end(),
                                                                             distance));

        CPPUNIT_ASSERT(distance == 5.0);
    }

    {
        prelert::maths::CEuclideanDistance::TDoubleVec coordinates1;
        coordinates1.push_back(-0.5);
        coordinates1.push_back(100000.0);
        coordinates1.push_back(-12.0);

        prelert::maths::CEuclideanDistance::TDoubleVec coordinates2;
        coordinates2.push_back(2.5);
        coordinates2.push_back(100004.0);
        coordinates2.push_back(0.0);

        double distance = 0.0;
        CPPUNIT_ASSERT(prelert::maths::CEuclideanDistance::euclideanDistance(coordinates1.begin(),
                                                                             coordinates1.end(),
                                                                             coordinates2.begin(),
                                                                             coordinates2.end(),
                                                                             distance));

        CPPUNIT_ASSERT(distance == 13.0);
    }
}

