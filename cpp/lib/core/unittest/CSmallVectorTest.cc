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

#include "CSmallVectorTest.h"

#include <core/CContainerPrinter.h>
#include <core/CLogger.h>
#include <core/CSmallVector.h>

using namespace prelert;

namespace
{

class CObjectWhichUsesMemory
{
    public:
        CObjectWhichUsesMemory(void)
        {
            m_Memory.reserve(100);
        }

        std::size_t memoryUsage(void) const
        {
            return size();
        }

        static std::size_t size(void)
        {
            return 100 * sizeof(char);
        }

    private:
        std::vector<char> m_Memory;
};

}

void CSmallVectorTest::testConstruction(void)
{
    // empty
    {
        core::CSmallVector<int, 4> test;
        CPPUNIT_ASSERT_EQUAL(std::size_t(4), test.capacity());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), test.size());
        CPPUNIT_ASSERT_EQUAL(true, test.empty());
    }

    // n duplicates
    {
        core::CSmallVector<int, 10> test(std::size_t(8), 6);
        CPPUNIT_ASSERT_EQUAL(std::size_t(10), test.capacity());
        CPPUNIT_ASSERT_EQUAL(std::size_t(8), test.size());
        CPPUNIT_ASSERT_EQUAL(false, test.empty());
        for (std::size_t i = 0u; i < 8; ++i)
        {
            CPPUNIT_ASSERT_EQUAL(6, test[i]);
        }
    }
    {
        core::CSmallVector<int, 5> test(std::size_t(8), 6);
        CPPUNIT_ASSERT(8 <= test.capacity());
        CPPUNIT_ASSERT_EQUAL(std::size_t(8), test.size());
        CPPUNIT_ASSERT_EQUAL(false, test.empty());
        for (std::size_t i = 0u; i < 8; ++i)
        {
            CPPUNIT_ASSERT_EQUAL(6, test[i]);
        }
    }

    // range
    double values[] = { 1.4, 2.7, 1.1, 7.2, 11.9, 1.2, 0.03 };
    {
        core::CSmallVector<double, 7> test(values, values + 7);
        CPPUNIT_ASSERT_EQUAL(std::size_t(7), test.capacity());
        CPPUNIT_ASSERT_EQUAL(std::size_t(7), test.size());
        CPPUNIT_ASSERT_EQUAL(false, test.empty());
        for (std::size_t i = 0u; i < 7; ++i)
        {
            CPPUNIT_ASSERT_EQUAL(values[i], test[i]);
        }
        std::ostringstream o;
        o << test;
        CPPUNIT_ASSERT_EQUAL(core::CContainerPrinter::print(values), o.str());
    }
    {
        core::CSmallVector<double, 4> test(values, values + 7);
        CPPUNIT_ASSERT(std::size_t(7) <= test.capacity());
        CPPUNIT_ASSERT_EQUAL(std::size_t(7), test.size());
        CPPUNIT_ASSERT_EQUAL(false, test.empty());
        for (std::size_t i = 0u; i < 7; ++i)
        {
            CPPUNIT_ASSERT_EQUAL(values[i], test[i]);
        }
    }

    // copy
    {
        core::CSmallVector<double, 7> test1(values, values + 7);
        core::CSmallVector<double, 7> test2(test1);
        CPPUNIT_ASSERT(test1 == test2);
    }
}

void CSmallVectorTest::testElementAccess(void)
{
    double values[] = { 3.8, 1.9, 2.5 };

    core::CSmallVector<double, 5> test(values, values + 3);

    // operator[] array
    CPPUNIT_ASSERT_EQUAL(values[0], test[0]);
    CPPUNIT_ASSERT_EQUAL(values[1], test[1]);
    CPPUNIT_ASSERT_EQUAL(values[2], test[2]);

    // at array
    CPPUNIT_ASSERT_EQUAL(values[0], test.at(0));
    CPPUNIT_ASSERT_EQUAL(values[1], test.at(1));
    CPPUNIT_ASSERT_EQUAL(values[2], test.at(2));
    CPPUNIT_ASSERT_THROW(test.at(3), std::out_of_range);

    // front array
    CPPUNIT_ASSERT_EQUAL(values[0], test.front());

    // back array
    CPPUNIT_ASSERT_EQUAL(values[2], test.back());

    double moreValues[] = { 4.9, 8.2, 7.1 };
    for (std::size_t i = 0u; i < 3; ++i)
    {
        test.push_back(moreValues[i]);
    }

    // operator[] vector
    CPPUNIT_ASSERT_EQUAL(values[0], test[0]);
    CPPUNIT_ASSERT_EQUAL(values[1], test[1]);
    CPPUNIT_ASSERT_EQUAL(values[2], test[2]);
    CPPUNIT_ASSERT_EQUAL(moreValues[0], test[3]);
    CPPUNIT_ASSERT_EQUAL(moreValues[1], test[4]);
    CPPUNIT_ASSERT_EQUAL(moreValues[2], test[5]);

    // at vector
    CPPUNIT_ASSERT_EQUAL(values[0], test.at(0));
    CPPUNIT_ASSERT_EQUAL(values[1], test.at(1));
    CPPUNIT_ASSERT_EQUAL(values[2], test.at(2));
    CPPUNIT_ASSERT_EQUAL(moreValues[0], test.at(3));
    CPPUNIT_ASSERT_EQUAL(moreValues[1], test.at(4));
    CPPUNIT_ASSERT_EQUAL(moreValues[2], test.at(5));
    CPPUNIT_ASSERT_THROW(test.at(8), std::out_of_range);

    // front vector
    CPPUNIT_ASSERT_EQUAL(values[0], test.front());

    // back vector
    CPPUNIT_ASSERT_EQUAL(moreValues[2], test.back());
}

void CSmallVectorTest::testCapacity(void)
{
    // empty array
    core::CSmallVector<double, 5> test;
    CPPUNIT_ASSERT_EQUAL(std::size_t(5), test.capacity());
    CPPUNIT_ASSERT_EQUAL(std::size_t(0), test.size());
    CPPUNIT_ASSERT_EQUAL(true, test.empty());

    // non-empty array
    test.push_back(3.2);
    CPPUNIT_ASSERT_EQUAL(std::size_t(5), test.capacity());
    CPPUNIT_ASSERT_EQUAL(std::size_t(1), test.size());
    CPPUNIT_ASSERT_EQUAL(false, test.empty());

    // resize array
    test.push_back(1.5);
    test.push_back(1.6);
    test.resize(2);
    CPPUNIT_ASSERT_EQUAL(std::size_t(5), test.capacity());
    CPPUNIT_ASSERT_EQUAL(std::size_t(2), test.size());
    CPPUNIT_ASSERT_EQUAL(false, test.empty());
    CPPUNIT_ASSERT_EQUAL(3.2, test[0]);
    CPPUNIT_ASSERT_EQUAL(1.5, test[1]);

    test.resize(0);
    CPPUNIT_ASSERT_EQUAL(std::size_t(5), test.capacity());
    CPPUNIT_ASSERT_EQUAL(std::size_t(0), test.size());
    CPPUNIT_ASSERT_EQUAL(true, test.empty());

    test.push_back(3.2);
    test.resize(4, 1.4);
    CPPUNIT_ASSERT_EQUAL(std::size_t(5), test.capacity());
    CPPUNIT_ASSERT_EQUAL(std::size_t(4), test.size());
    CPPUNIT_ASSERT_EQUAL(false, test.empty());
    CPPUNIT_ASSERT_EQUAL(3.2, test[0]);
    CPPUNIT_ASSERT_EQUAL(1.4, test[1]);
    CPPUNIT_ASSERT_EQUAL(1.4, test[2]);
    CPPUNIT_ASSERT_EQUAL(1.4, test[3]);

    // reserve
    test.reserve(8);
    CPPUNIT_ASSERT(std::size_t(8) <= test.capacity());
    CPPUNIT_ASSERT_EQUAL(std::size_t(4), test.size());
    CPPUNIT_ASSERT_EQUAL(false, test.empty());
    CPPUNIT_ASSERT_EQUAL(3.2, test[0]);
    CPPUNIT_ASSERT_EQUAL(1.4, test[1]);
    CPPUNIT_ASSERT_EQUAL(1.4, test[2]);
    CPPUNIT_ASSERT_EQUAL(1.4, test[3]);

    // resize vector
    test.resize(2);
    CPPUNIT_ASSERT(std::size_t(8) <= test.capacity());
    CPPUNIT_ASSERT_EQUAL(std::size_t(2), test.size());
    CPPUNIT_ASSERT_EQUAL(false, test.empty());
    CPPUNIT_ASSERT_EQUAL(3.2, test[0]);
    CPPUNIT_ASSERT_EQUAL(1.4, test[1]);

    // empty vector
    test.resize(0);
    CPPUNIT_ASSERT(std::size_t(8) <= test.capacity());
    CPPUNIT_ASSERT_EQUAL(std::size_t(0), test.size());
    CPPUNIT_ASSERT_EQUAL(true, test.empty());
}

void CSmallVectorTest::testModifiers(void)
{
    // assign range
    double values[] = { 1.2, 1.7, 2.8, 0.1, 6.4 };
    {
        core::CSmallVector<double, 5> test;
        test.assign(values, values + 5);
        CPPUNIT_ASSERT_EQUAL(std::size_t(5), test.capacity());
        CPPUNIT_ASSERT_EQUAL(std::size_t(5), test.size());
        for (std::size_t i = 0u; i < 5; ++i)
        {
            CPPUNIT_ASSERT_EQUAL(values[i], test[i]);
        }
    }
    {
        core::CSmallVector<double, 3> test(1, 2.3);
        test.assign(values, values + 5);
        CPPUNIT_ASSERT(std::size_t(5) <= test.capacity());
        CPPUNIT_ASSERT_EQUAL(std::size_t(5), test.size());
        for (std::size_t i = 0u; i < 5; ++i)
        {
            CPPUNIT_ASSERT_EQUAL(values[i], test[i]);
        }
    }

    // assign n copies
    {
        core::CSmallVector<double, 5> test(2, 3.1);
        test.assign(4, 1.8);
        CPPUNIT_ASSERT_EQUAL(std::size_t(5), test.capacity());
        CPPUNIT_ASSERT_EQUAL(std::size_t(4), test.size());
        for (std::size_t i = 0u; i < 4; ++i)
        {
            CPPUNIT_ASSERT_EQUAL(1.8, test[i]);
        }
    }
    {
        core::CSmallVector<double, 2> test;
        test.assign(4, 1.8);
        CPPUNIT_ASSERT(4 <= test.capacity());
        CPPUNIT_ASSERT_EQUAL(std::size_t(4), test.size());
        for (std::size_t i = 0u; i < 4; ++i)
        {
            CPPUNIT_ASSERT_EQUAL(1.8, test[i]);
        }
    }

    // push_back and pop_back
    {
        core::CSmallVector<double, 4> test;
        for (std::size_t i = 0u; i < 5; ++i)
        {
            test.push_back(values[i]);
            CPPUNIT_ASSERT_EQUAL(i + 1, test.size());
            for (std::size_t j = 0u; j <= i; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(values[j], test[j]);
            }
        }
        for (std::size_t i = 0u; i < 5; ++i)
        {
            test.pop_back();
            CPPUNIT_ASSERT_EQUAL(5 - i - 1, test.size());
            for (std::size_t j = 0u; j < test.size(); ++j)
            {
                CPPUNIT_ASSERT_EQUAL(values[j], test[j]);
            }
        }
    }

    // insert n values
    {
        core::CSmallVector<double, 6> test;
        core::CSmallVector<double, 6>::iterator i = test.insert(test.end(), 1.2);
        CPPUNIT_ASSERT_EQUAL(std::size_t(1), test.size());
        CPPUNIT_ASSERT_EQUAL(1.2, *i);
        test.push_back(2.0);
        test.push_back(3.0);
        test.push_back(2.1);
        i = test.insert(test.begin() + 2, 2, 1.8);
        CPPUNIT_ASSERT_EQUAL(std::size_t(6), test.size());
        CPPUNIT_ASSERT_EQUAL(1.8, *i);
        {
            double expected[] = { 1.2, 2.0, 1.8, 1.8, 3.0, 2.1 };
            for (std::size_t j = 0u; j < 6; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(expected[j], test[j]);
            }
        }
        i = test.insert(test.begin(), 0.1);
        CPPUNIT_ASSERT_EQUAL(std::size_t(7), test.size());
        CPPUNIT_ASSERT_EQUAL(0.1, *i);
        CPPUNIT_ASSERT_EQUAL(0.1, test.front());
        i = test.insert(test.begin() + 3, 0.2);
        CPPUNIT_ASSERT_EQUAL(std::size_t(8), test.size());
        CPPUNIT_ASSERT_EQUAL(0.2, *i);
        {
            double expected[] = { 0.1, 1.2, 2.0, 0.2, 1.8, 1.8, 3.0, 2.1 };
            for (std::size_t j = 0u; j < 8; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(expected[j], test[j]);
            }
        }
        CPPUNIT_ASSERT(7 == (test.insert(test.end() - 1, 3, 4.2) - test.begin()));
        CPPUNIT_ASSERT_EQUAL(std::size_t(11), test.size());
        {
            double expected[] = { 0.1, 1.2, 2.0, 0.2, 1.8, 1.8, 3.0, 4.2, 4.2, 4.2, 2.1 };
            for (std::size_t j = 0u; j < 11; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(expected[j], test[j]);
            }
        }
    }

    // insert range
    {
        core::CSmallVector<double, 7> test;
        test.push_back(100.2);
        test.push_back(11.1);
        core::CSmallVector<double, 7>::iterator i = test.insert(test.end(), &values[0], &values[2]);
        CPPUNIT_ASSERT_EQUAL(std::size_t(4), test.size());
        CPPUNIT_ASSERT_EQUAL(values[0], *i);
        CPPUNIT_ASSERT(2 == (i - test.begin()));
        {
            double expected[] = { 100.2, 11.1, values[0], values[1] };
            for (std::size_t j = 0u; j < 4; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(expected[j], test[j]);
            }
        }
        i = test.insert(test.begin(), &values[2], &values[5]);
        CPPUNIT_ASSERT_EQUAL(std::size_t(7), test.size());
        CPPUNIT_ASSERT_EQUAL(values[2], *i);
        CPPUNIT_ASSERT(0 == (i - test.begin()));
        {
            double expected[] =
                {
                    values[2],
                    values[3],
                    values[4],
                    100.2,
                    11.1,
                    values[0],
                    values[1]
                };
            for (std::size_t j = 0u; j < 7; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(expected[j], test[j]);
            }
        }
        i = test.insert(test.begin() + 3, &values[1], &values[2]);
        CPPUNIT_ASSERT_EQUAL(std::size_t(8), test.size());
        CPPUNIT_ASSERT_EQUAL(values[1], *i);
        CPPUNIT_ASSERT(3 == (i - test.begin()));
        {
            double expected[] =
                {
                    values[2],
                    values[3],
                    values[4],
                    values[1],
                    100.2,
                    11.1,
                    values[0],
                    values[1]
                };
            for (std::size_t j = 0u; j < 8; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(expected[j], test[j]);
            }
        }
        i = test.insert(test.begin() + 5, &values[2], &values[4]);
        CPPUNIT_ASSERT_EQUAL(std::size_t(10), test.size());
        CPPUNIT_ASSERT_EQUAL(values[2], *i);
        CPPUNIT_ASSERT(5 == (i - test.begin()));
        {
            double expected[] =
                {
                    values[2],
                    values[3],
                    values[4],
                    values[1],
                    100.2,
                    values[2],
                    values[3],
                    11.1,
                    values[0],
                    values[1]
                };
            for (std::size_t j = 0u; j < 10; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(expected[j], test[j]);
            }
        }
    }

    // erase
    {
        core::CSmallVector<double, 5> test(values, values + 5);
        core::CSmallVector<double, 5>::iterator i = test.erase(test.begin());
        CPPUNIT_ASSERT_EQUAL(std::size_t(4), test.size());
        CPPUNIT_ASSERT(0 == (i - test.begin()));
        CPPUNIT_ASSERT_EQUAL(values[1], *i);
        {
            double expected[] =
                {
                    values[1],
                    values[2],
                    values[3],
                    values[4]
                };
            for (std::size_t j = 0u; j < 4; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(expected[j], test[j]);
            }
        }
        i = test.erase(test.begin() + 1, test.begin() + 3);
        CPPUNIT_ASSERT_EQUAL(std::size_t(2), test.size());
        CPPUNIT_ASSERT(1 == (i - test.begin()));
        CPPUNIT_ASSERT_EQUAL(values[4], *i);
        {
            double expected[] = { values[1], values[4] };
            for (std::size_t j = 0u; j < 2; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(expected[j], test[j]);
            }
        }
        i = test.erase(test.begin(), test.end());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), test.size());
        CPPUNIT_ASSERT(0 == (i - test.begin()));
    }
    {
        core::CSmallVector<double, 3> test(values, values + 5);
        core::CSmallVector<double, 3>::iterator i = test.erase(test.begin());
        CPPUNIT_ASSERT_EQUAL(std::size_t(4), test.size());
        CPPUNIT_ASSERT(0 == (i - test.begin()));
        CPPUNIT_ASSERT_EQUAL(values[1], *i);
        {
            double expected[] =
                {
                    values[1],
                    values[2],
                    values[3],
                    values[4]
                };
            for (std::size_t j = 0u; j < 4; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(expected[j], test[j]);
            }
        }
        i = test.erase(test.begin() + 1, test.begin() + 3);
        CPPUNIT_ASSERT_EQUAL(std::size_t(2), test.size());
        CPPUNIT_ASSERT(1 == (i - test.begin()));
        CPPUNIT_ASSERT_EQUAL(values[4], *i);
        {
            double expected[] = { values[1], values[4] };
            for (std::size_t j = 0u; j < 2; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(expected[j], test[j]);
            }
        }
        i = test.erase(test.begin(), test.end());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), test.size());
        CPPUNIT_ASSERT(0 == (i - test.begin()));
    }

    // swap
    {
        core::CSmallVector<double, 5> test1;
        test1.push_back(2.1);
        test1.push_back(1.7);
        core::CSmallVector<double, 5> test2;
        test2.push_back(1.6);
        test2.push_back(1.9);
        test2.push_back(1.9);
        test1.swap(test2);
        CPPUNIT_ASSERT_EQUAL(1.6, test1[0]);
        CPPUNIT_ASSERT_EQUAL(1.9, test1[1]);
        CPPUNIT_ASSERT_EQUAL(1.9, test1[2]);
        CPPUNIT_ASSERT_EQUAL(2.1, test2[0]);
        CPPUNIT_ASSERT_EQUAL(1.7, test2[1]);
    }
    {
        core::CSmallVector<double, 3> test1;
        test1.push_back(2.1);
        test1.push_back(1.7);
        core::CSmallVector<double, 3> test2;
        test2.push_back(1.6);
        test2.push_back(1.9);
        test2.push_back(1.9);
        test2.push_back(1.1);
        test1.swap(test2);
        CPPUNIT_ASSERT_EQUAL(1.6, test1[0]);
        CPPUNIT_ASSERT_EQUAL(1.9, test1[1]);
        CPPUNIT_ASSERT_EQUAL(1.9, test1[2]);
        CPPUNIT_ASSERT_EQUAL(1.1, test1[3]);
        CPPUNIT_ASSERT_EQUAL(2.1, test2[0]);
        CPPUNIT_ASSERT_EQUAL(1.7, test2[1]);
        test1.swap(test2);
        CPPUNIT_ASSERT_EQUAL(2.1, test1[0]);
        CPPUNIT_ASSERT_EQUAL(1.7, test1[1]);
        CPPUNIT_ASSERT_EQUAL(1.6, test2[0]);
        CPPUNIT_ASSERT_EQUAL(1.9, test2[1]);
        CPPUNIT_ASSERT_EQUAL(1.9, test2[2]);
        CPPUNIT_ASSERT_EQUAL(1.1, test2[3]);
    }
    {
        core::CSmallVector<double, 1> test1;
        test1.push_back(2.1);
        test1.push_back(1.7);
        core::CSmallVector<double, 1> test2;
        test2.push_back(1.6);
        test2.push_back(1.9);
        test2.push_back(1.9);
        test1.swap(test2);
        CPPUNIT_ASSERT_EQUAL(1.6, test1[0]);
        CPPUNIT_ASSERT_EQUAL(1.9, test1[1]);
        CPPUNIT_ASSERT_EQUAL(1.9, test1[2]);
        CPPUNIT_ASSERT_EQUAL(2.1, test2[0]);
        CPPUNIT_ASSERT_EQUAL(1.7, test2[1]);
    }

    // clear
    {
        core::CSmallVector<double, 5> test;
        test.push_back(2.1);
        test.push_back(1.7);
        test.clear();
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), test.size());
        CPPUNIT_ASSERT_EQUAL(true, test.empty());
    }
    {
        core::CSmallVector<double, 1> test;
        test.push_back(2.1);
        test.push_back(1.7);
        test.clear();
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), test.size());
        CPPUNIT_ASSERT_EQUAL(true, test.empty());
    }
}

void CSmallVectorTest::testIterators(void)
{
    core::CSmallVector<double, 3> test;

    // Increment and decrement

    test.push_back(2.1);

    CPPUNIT_ASSERT_EQUAL(2.1, *test.begin());
    CPPUNIT_ASSERT_EQUAL(2.1, *(test.end() - 1));
    CPPUNIT_ASSERT((test.begin() + 1) == test.end());
    CPPUNIT_ASSERT_EQUAL(2.1, *test.rbegin());
    CPPUNIT_ASSERT_EQUAL(2.1, *(test.rend() - 1));
    CPPUNIT_ASSERT((test.rbegin() + 1) == test.rend());

    test.push_back(3.6);

    {
        core::CSmallVector<double, 3>::const_iterator i = test.begin();
        CPPUNIT_ASSERT_EQUAL(2.1, *i);
        CPPUNIT_ASSERT(i == i);
        ++i;
        CPPUNIT_ASSERT_EQUAL(3.6, *i);
        CPPUNIT_ASSERT(i == i);
        ++i;
        CPPUNIT_ASSERT(i == test.end());
        CPPUNIT_ASSERT(i == i);
    }
    {
        core::CSmallVector<double, 3>::const_iterator i = test.end();
        CPPUNIT_ASSERT(i == test.end());
        CPPUNIT_ASSERT(i == i);
        --i;
        CPPUNIT_ASSERT_EQUAL(3.6, *i);
        CPPUNIT_ASSERT(i == i);
        --i;
        CPPUNIT_ASSERT_EQUAL(2.1, *i);
        CPPUNIT_ASSERT(i == i);
    }

    {
        core::CSmallVector<double, 3>::const_reverse_iterator ir = test.rbegin();
        CPPUNIT_ASSERT_EQUAL(3.6, *ir);
        CPPUNIT_ASSERT(ir == ir);
        ++ir;
        CPPUNIT_ASSERT_EQUAL(2.1, *ir);
        CPPUNIT_ASSERT(ir == ir);
        ++ir;
        CPPUNIT_ASSERT(ir == test.crend());
        CPPUNIT_ASSERT(ir == ir);
    }
    {
        core::CSmallVector<double, 3>::const_reverse_iterator ir = test.rend();
        CPPUNIT_ASSERT(ir == test.crend());
        CPPUNIT_ASSERT(ir == ir);
        --ir;
        CPPUNIT_ASSERT_EQUAL(2.1, *ir);
        CPPUNIT_ASSERT(ir == ir);
        --ir;
        CPPUNIT_ASSERT_EQUAL(3.6, *ir);
        CPPUNIT_ASSERT(ir == ir);
    }

    test.push_back(1.3);

    CPPUNIT_ASSERT_EQUAL(2.1, *(test.begin() + 0));
    CPPUNIT_ASSERT_EQUAL(3.6, *(test.begin() + 1));
    CPPUNIT_ASSERT_EQUAL(1.3, *(test.begin() + 2));
    CPPUNIT_ASSERT_EQUAL(1.3, *(test.end() - 1));
    CPPUNIT_ASSERT_EQUAL(3.6, *(test.end() - 2));
    CPPUNIT_ASSERT_EQUAL(2.1, *(test.end() - 3));
    CPPUNIT_ASSERT(test.end() == (test.begin() + 3));
    CPPUNIT_ASSERT_EQUAL(1.3, *(test.rbegin() + 0));
    CPPUNIT_ASSERT_EQUAL(3.6, *(test.rbegin() + 1));
    CPPUNIT_ASSERT_EQUAL(2.1, *(test.rbegin() + 2));
    CPPUNIT_ASSERT_EQUAL(2.1, *(test.rend() - 1));
    CPPUNIT_ASSERT_EQUAL(3.6, *(test.rend() - 2));
    CPPUNIT_ASSERT_EQUAL(1.3, *(test.rend() - 3));
    CPPUNIT_ASSERT(test.rend() == (test.rbegin() + 3));

    // Comparison
    CPPUNIT_ASSERT((test.begin() + 1) < (test.begin() + 2));
    CPPUNIT_ASSERT((test.begin() + 2) > (test.begin() + 1));
    CPPUNIT_ASSERT((test.begin() + 1) != (test.begin() + 2));

    // Difference
    CPPUNIT_ASSERT(3 == (test.begin() + 3) - test.begin());
    CPPUNIT_ASSERT(2 == (test.begin() + 2) - test.begin());
    CPPUNIT_ASSERT(1 == (test.begin() + 1) - test.begin());
    CPPUNIT_ASSERT(0 == (test.begin() + 1) - (test.begin() + 1));

    // operator[]
    CPPUNIT_ASSERT_EQUAL(*(test.begin() + 0), test.begin()[0]);
    CPPUNIT_ASSERT_EQUAL(*(test.begin() + 1), test.begin()[1]);
    CPPUNIT_ASSERT_EQUAL(*(test.begin() + 2), test.begin()[2]);

    // Post increment decrement
    for (core::CSmallVector<double, 3>::const_iterator i = test.begin(), j = test.begin();
         i != test.end();
         ++i)
    {
        CPPUNIT_ASSERT_EQUAL(*i, *(j++));
    }
    for (core::CSmallVector<double, 3>::const_reverse_iterator i = test.rbegin(), j = test.rbegin();
         i != test.crend();
         ++i)
    {
        CPPUNIT_ASSERT_EQUAL(*i, *(j++));
    }
    for (core::CSmallVector<double, 3>::const_iterator i = test.end(), j = test.end() - 1;
         i != test.begin();
         /**/)
    {
        --i;
        CPPUNIT_ASSERT_EQUAL(*i, *(j--));
    }
    for (core::CSmallVector<double, 3>::const_reverse_iterator i = test.rend(), j = test.rend() - 1;
         i != test.crbegin();
         /**/)
    {
        --i;
        CPPUNIT_ASSERT_EQUAL(*i, *(j--));
    }

    // Modification through an iterator.
    *test.begin() = 5.6;
    CPPUNIT_ASSERT_EQUAL(5.6, test[0]);

    test.push_back(2.8);

    // Test switch to vector.
    CPPUNIT_ASSERT_EQUAL(5.6, *(test.begin() + 0));
    CPPUNIT_ASSERT_EQUAL(3.6, *(test.begin() + 1));
    CPPUNIT_ASSERT_EQUAL(1.3, *(test.begin() + 2));
    CPPUNIT_ASSERT_EQUAL(2.8, *(test.begin() + 3));
    CPPUNIT_ASSERT_EQUAL(2.8, *(test.end() - 1));
    CPPUNIT_ASSERT_EQUAL(1.3, *(test.end() - 2));
    CPPUNIT_ASSERT_EQUAL(3.6, *(test.end() - 3));
    CPPUNIT_ASSERT_EQUAL(5.6, *(test.end() - 4));
    CPPUNIT_ASSERT(test.end() == (test.begin() + 4));
    CPPUNIT_ASSERT_EQUAL(2.8, *(test.rbegin() + 0));
    CPPUNIT_ASSERT_EQUAL(1.3, *(test.rbegin() + 1));
    CPPUNIT_ASSERT_EQUAL(3.6, *(test.rbegin() + 2));
    CPPUNIT_ASSERT_EQUAL(5.6, *(test.rbegin() + 3));
    CPPUNIT_ASSERT_EQUAL(5.6, *(test.rend() - 1));
    CPPUNIT_ASSERT_EQUAL(3.6, *(test.rend() - 2));
    CPPUNIT_ASSERT_EQUAL(1.3, *(test.rend() - 3));
    CPPUNIT_ASSERT_EQUAL(2.8, *(test.rend() - 4));
    CPPUNIT_ASSERT(test.rend() == (test.rbegin() + 4));

    // Test works with STL.
    std::sort(test.begin(), test.end());
    CPPUNIT_ASSERT_EQUAL(1.3, *(test.begin() + 0));
    CPPUNIT_ASSERT_EQUAL(2.8, *(test.begin() + 1));
    CPPUNIT_ASSERT_EQUAL(3.6, *(test.begin() + 2));
    CPPUNIT_ASSERT_EQUAL(5.6, *(test.begin() + 3));
}

void CSmallVectorTest::testOperators(void)
{
    double values1[] = { 100.3, 1.2, 0.5, 7.1 };
    double values2[] = { 100.3, 1.2, 0.5, 7.2 };
    double values3[] = { 100.3, 1.2, 0.6, 6.0 };
    double values4[] = { 100.3, 1.3, 0.4, 6.0 };
    double values5[] = { 101.0, 1.1, 0.4, 6.0 };
    double values6[] = { 100.3, 1.2, 0.5, 7.1, 1.0 };

    // (not) equal
    {
        core::CSmallVector<double, 5> test1(values1, values1 + 4);
        core::CSmallVector<double, 5> test2(values1, values1 + 4);
        CPPUNIT_ASSERT(test1 == test2);
        CPPUNIT_ASSERT(!(test1 != test2));
    }
    {
        core::CSmallVector<double, 5> test1(values1, values1 + 4);
        core::CSmallVector<double, 5> test2(values2, values2 + 4);
        CPPUNIT_ASSERT(test1 != test2);
        CPPUNIT_ASSERT(!(test1 == test2));
    }
    {
        core::CSmallVector<double, 5> test1(values1, values1 + 4);
        core::CSmallVector<double, 5> test2(values6, values6 + 5);
        CPPUNIT_ASSERT(test1 != test2);
        CPPUNIT_ASSERT(!(test1 == test2));
    }
    {
        core::CSmallVector<double, 3> test1(values1, values1 + 4);
        core::CSmallVector<double, 3> test2(values1, values1 + 4);
        CPPUNIT_ASSERT(test1 == test2);
        CPPUNIT_ASSERT(!(test1 != test2));
    }
    {
        core::CSmallVector<double, 3> test1(values1, values1 + 4);
        core::CSmallVector<double, 3> test2(values2, values2 + 4);
        CPPUNIT_ASSERT(test1 != test2);
        CPPUNIT_ASSERT(!(test1 == test2));
    }
    {
        core::CSmallVector<double, 3> test1(values1, values1 + 4);
        core::CSmallVector<double, 3> test2(values6, values6 + 5);
        CPPUNIT_ASSERT(test1 != test2);
        CPPUNIT_ASSERT(!(test1 == test2));
    }

    // less (equal), greater (equal)
    {
        core::CSmallVector<double, 5> test1(values1, values1 + 4);
        core::CSmallVector<double, 5> test2(values1, values1 + 4);
        CPPUNIT_ASSERT(test1 <= test2);
        CPPUNIT_ASSERT(!(test1 < test2));
        CPPUNIT_ASSERT(test1 >= test2);
        CPPUNIT_ASSERT(!(test1 > test2));
    }
    {
        core::CSmallVector<double, 5> test1(values1, values1 + 4);
        core::CSmallVector<double, 5> test2(values2, values2 + 4);
        CPPUNIT_ASSERT(test1 <= test2);
        CPPUNIT_ASSERT(test1 < test2);
        CPPUNIT_ASSERT(!(test1 >= test2));
        CPPUNIT_ASSERT(!(test1 > test2));
    }
    {
        core::CSmallVector<double, 5> test1(values3, values3 + 4);
        core::CSmallVector<double, 5> test2(values1, values1 + 4);
        CPPUNIT_ASSERT(!(test1 <= test2));
        CPPUNIT_ASSERT(!(test1 < test2));
        CPPUNIT_ASSERT(test1 >= test2);
        CPPUNIT_ASSERT(test1 > test2);
    }
    {
        core::CSmallVector<double, 5> test1(values1, values1 + 4);
        core::CSmallVector<double, 5> test2(values4, values4 + 4);
        CPPUNIT_ASSERT(test1 <= test2);
        CPPUNIT_ASSERT(test1 < test2);
        CPPUNIT_ASSERT(!(test1 >= test2));
        CPPUNIT_ASSERT(!(test1 > test2));
    }
    {
        core::CSmallVector<double, 5> test1(values5, values5 + 4);
        core::CSmallVector<double, 5> test2(values1, values1 + 4);
        CPPUNIT_ASSERT(!(test1 <= test2));
        CPPUNIT_ASSERT(!(test1 < test2));
        CPPUNIT_ASSERT(test1 >= test2);
        CPPUNIT_ASSERT(test1 > test2);
    }
    {
        core::CSmallVector<double, 5> test1(values6, values6 + 5);
        core::CSmallVector<double, 5> test2(values1, values1 + 4);
        CPPUNIT_ASSERT(!(test1 <= test2));
        CPPUNIT_ASSERT(!(test1 < test2));
        CPPUNIT_ASSERT(test1 >= test2);
        CPPUNIT_ASSERT(test1 > test2);
    }
    {
        core::CSmallVector<double, 3> test1(values1, values1 + 4);
        core::CSmallVector<double, 3> test2(values1, values1 + 4);
        CPPUNIT_ASSERT(test1 <= test2);
        CPPUNIT_ASSERT(!(test1 < test2));
        CPPUNIT_ASSERT(test1 >= test2);
        CPPUNIT_ASSERT(!(test1 > test2));
    }
    {
        core::CSmallVector<double, 3> test1(values1, values1 + 4);
        core::CSmallVector<double, 3> test2(values2, values2 + 4);
        CPPUNIT_ASSERT(test1 <= test2);
        CPPUNIT_ASSERT(test1 < test2);
        CPPUNIT_ASSERT(!(test1 >= test2));
        CPPUNIT_ASSERT(!(test1 > test2));
    }
    {
        core::CSmallVector<double, 3> test1(values3, values3 + 4);
        core::CSmallVector<double, 3> test2(values1, values1 + 4);
        CPPUNIT_ASSERT(!(test1 <= test2));
        CPPUNIT_ASSERT(!(test1 < test2));
        CPPUNIT_ASSERT(test1 >= test2);
        CPPUNIT_ASSERT(test1 > test2);
    }
    {
        core::CSmallVector<double, 3> test1(values1, values1 + 4);
        core::CSmallVector<double, 3> test2(values4, values4 + 4);
        CPPUNIT_ASSERT(test1 <= test2);
        CPPUNIT_ASSERT(test1 < test2);
        CPPUNIT_ASSERT(!(test1 >= test2));
        CPPUNIT_ASSERT(!(test1 > test2));
    }
    {
        core::CSmallVector<double, 3> test1(values5, values5 + 4);
        core::CSmallVector<double, 3> test2(values1, values1 + 4);
        CPPUNIT_ASSERT(!(test1 <= test2));
        CPPUNIT_ASSERT(!(test1 < test2));
        CPPUNIT_ASSERT(test1 >= test2);
        CPPUNIT_ASSERT(test1 > test2);
    }
    {
        core::CSmallVector<double, 3> test1(values6, values6 + 5);
        core::CSmallVector<double, 3> test2(values1, values1 + 4);
        CPPUNIT_ASSERT(!(test1 <= test2));
        CPPUNIT_ASSERT(!(test1 < test2));
        CPPUNIT_ASSERT(test1 >= test2);
        CPPUNIT_ASSERT(test1 > test2);
    }
    {
        // Adding same capacity, same size vectors
        core::CSmallVector<double, 3> test1(values1, values1 + 4);
        core::CSmallVector<double, 3> test2(values2, values2 + 4);
        test1 += test2;
        CPPUNIT_ASSERT_EQUAL(std::size_t(4), test1.size());
        CPPUNIT_ASSERT_EQUAL(std::size_t(4), test2.size());
        for (std::size_t i = 0; i < test1.size(); ++i)
        {
            CPPUNIT_ASSERT_EQUAL(test1[i], values1[i] + values2[i]);
            CPPUNIT_ASSERT_EQUAL(test2[i], values2[i]);
        }
    }
    {
        // Adding different capacity vectors but same size
        core::CSmallVector<double, 3> test1(values1, values1 + 4);
        core::CSmallVector<double, 4> test2(values6, values6 + 4);
        test1 += test2;
        CPPUNIT_ASSERT_EQUAL(std::size_t(4), test1.size());
        for (std::size_t i = 0; i < test1.size(); ++i)
        {
            CPPUNIT_ASSERT_EQUAL(test1[i], values1[i] + values6[i]);
            CPPUNIT_ASSERT_EQUAL(test2[i], values6[i]);
        }
    }
    {
        // Adding different sized vectors should be a no-op.
        core::CSmallVector<double, 3> test1(values1, values1 + 4);
        core::CSmallVector<double, 3> test2(values6, values6 + 5);
        test1 += test2;
        CPPUNIT_ASSERT_EQUAL(std::size_t(4), test1.size());
        for (std::size_t i = 0; i < test1.size(); ++i)
        {
            test1[i] = values1[i];
        }
    }
    {
        // Subtracting same capacity, same size vectors
        core::CSmallVector<double, 3> test1(values1, values1 + 4);
        core::CSmallVector<double, 3> test2(values2, values2 + 4);
        test1 -= test2;
        CPPUNIT_ASSERT_EQUAL(std::size_t(4), test1.size());
        CPPUNIT_ASSERT_EQUAL(std::size_t(4), test2.size());
        for (std::size_t i = 0; i < test1.size(); ++i)
        {
            CPPUNIT_ASSERT_EQUAL(test1[i], values1[i] - values2[i]);
            CPPUNIT_ASSERT_EQUAL(test2[i], values2[i]);
        }
    }
    {
        // Subtracting different capacity vectors but same size
        core::CSmallVector<double, 3> test1(values1, values1 + 4);
        core::CSmallVector<double, 4> test2(values6, values6 + 4);
        test1 -= test2;
        CPPUNIT_ASSERT_EQUAL(std::size_t(4), test1.size());
        for (std::size_t i = 0; i < test1.size(); ++i)
        {
            CPPUNIT_ASSERT_EQUAL(test1[i], values1[i] - values6[i]);
            CPPUNIT_ASSERT_EQUAL(test2[i], values6[i]);
        }
    }
    {
        // Subtracting different sized vectors should be a no-op.
        core::CSmallVector<double, 3> test1(values1, values1 + 4);
        core::CSmallVector<double, 3> test2(values6, values6 + 5);
        test1 -= test2;
        CPPUNIT_ASSERT_EQUAL(std::size_t(4), test1.size());
        for (std::size_t i = 0; i < test1.size(); ++i)
        {
            test1[i] = values1[i];
        }
    }
}

void CSmallVectorTest::testVectorBool(void)
{
    // empty
    {
        core::CSmallVectorBool<4> test;
        CPPUNIT_ASSERT_EQUAL(std::size_t(4), test.capacity());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), test.size());
        CPPUNIT_ASSERT_EQUAL(true, test.empty());
    }

    // n duplicates
    {
        core::CSmallVectorBool<10> test(std::size_t(8), true);
        CPPUNIT_ASSERT_EQUAL(std::size_t(10), test.capacity());
        CPPUNIT_ASSERT_EQUAL(std::size_t(8), test.size());
        CPPUNIT_ASSERT_EQUAL(false, test.empty());
        for (std::size_t i = 0u; i < 8; ++i)
        {
            CPPUNIT_ASSERT_EQUAL(true, bool(test[i]));
        }
    }
    {
        core::CSmallVectorBool<5> test(std::size_t(8), false);
        CPPUNIT_ASSERT(8 <= test.capacity());
        CPPUNIT_ASSERT_EQUAL(std::size_t(8), test.size());
        CPPUNIT_ASSERT_EQUAL(false, test.empty());
        for (std::size_t i = 0u; i < 8; ++i)
        {
            CPPUNIT_ASSERT_EQUAL(false, bool(test[i]));
        }
    }

    {
        bool values[] = { true, false, false, true, true, false, false };

        // range
        {
            core::CSmallVectorBool<7> test(values, values + 7);
            CPPUNIT_ASSERT_EQUAL(std::size_t(7), test.capacity());
            CPPUNIT_ASSERT_EQUAL(std::size_t(7), test.size());
            CPPUNIT_ASSERT_EQUAL(false, test.empty());
            for (std::size_t i = 0u; i < 7; ++i)
            {
                CPPUNIT_ASSERT_EQUAL(values[i], bool(test[i]));
            }
            std::ostringstream o;
            o << test;
            CPPUNIT_ASSERT_EQUAL(std::string("[1, 0, 0, 1, 1, 0, 0]"), o.str());
        }
        {
            core::CSmallVectorBool<4> test(values, values + 7);
            CPPUNIT_ASSERT(std::size_t(7) <= test.capacity());
            CPPUNIT_ASSERT_EQUAL(std::size_t(7), test.size());
            CPPUNIT_ASSERT_EQUAL(false, test.empty());
            for (std::size_t i = 0u; i < 7; ++i)
            {
                CPPUNIT_ASSERT_EQUAL(values[i], bool(test[i]));
            }
        }

        // copy
        {
            core::CSmallVectorBool<7> test1(values, values + 7);
            core::CSmallVectorBool<7> test2(test1);
            CPPUNIT_ASSERT(test1 == test2);
        }

        // Modification through an iterator.
        {
            core::CSmallVectorBool<7> test(values, values + 7);
            CPPUNIT_ASSERT_EQUAL(true, bool(test[0]));
            CPPUNIT_ASSERT_EQUAL(true, bool(test[3]));
            *test.begin() = *(test.begin() + 3) = false;
            CPPUNIT_ASSERT_EQUAL(false, bool(test[0]));
            CPPUNIT_ASSERT_EQUAL(false, bool(test[3]));
        }

        // Modification through operator[].
        {
            core::CSmallVectorBool<7> test(values, values + 7);
            CPPUNIT_ASSERT_EQUAL(true, bool(test[0]));
            CPPUNIT_ASSERT_EQUAL(true, bool(test[3]));
            test[0] = test[3] = false;
            CPPUNIT_ASSERT_EQUAL(false, bool(test[0]));
            CPPUNIT_ASSERT_EQUAL(false, bool(test[3]));
        }
    }

    // assign range
    bool values[] = { true, false, true, true, false };
    {
        core::CSmallVectorBool<5> test;
        test.assign(values, values + 5);
        CPPUNIT_ASSERT_EQUAL(std::size_t(5), test.capacity());
        CPPUNIT_ASSERT_EQUAL(std::size_t(5), test.size());
        for (std::size_t i = 0u; i < 5; ++i)
        {
            CPPUNIT_ASSERT_EQUAL(values[i], bool(test[i]));
        }
    }
    {
        core::CSmallVectorBool<3> test(1, false);
        test.assign(values, values + 5);
        CPPUNIT_ASSERT(std::size_t(5) <= test.capacity());
        CPPUNIT_ASSERT_EQUAL(std::size_t(5), test.size());
        for (std::size_t i = 0u; i < 5; ++i)
        {
            CPPUNIT_ASSERT_EQUAL(values[i], bool(test[i]));
        }
    }

    // assign n copies
    {
        core::CSmallVectorBool<5> test(2, false);
        test.assign(4, true);
        CPPUNIT_ASSERT_EQUAL(std::size_t(5), test.capacity());
        CPPUNIT_ASSERT_EQUAL(std::size_t(4), test.size());
        for (std::size_t i = 0u; i < 4; ++i)
        {
            CPPUNIT_ASSERT_EQUAL(true, bool(test[i]));
        }
    }
    {
        core::CSmallVectorBool<2> test;
        test.assign(4, false);
        CPPUNIT_ASSERT(4 <= test.capacity());
        CPPUNIT_ASSERT_EQUAL(std::size_t(4), test.size());
        for (std::size_t i = 0u; i < 4; ++i)
        {
            CPPUNIT_ASSERT_EQUAL(false, bool(test[i]));
        }
    }

    // push_back and pop_back
    {
        core::CSmallVectorBool<4> test;
        for (std::size_t i = 0u; i < 5; ++i)
        {
            test.push_back(values[i]);
            CPPUNIT_ASSERT_EQUAL(i + 1, test.size());
            for (std::size_t j = 0u; j <= i; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(values[j], bool(test[j]));
            }
        }
        for (std::size_t i = 0u; i < 5; ++i)
        {
            test.pop_back();
            CPPUNIT_ASSERT_EQUAL(5 - i - 1, test.size());
            for (std::size_t j = 0u; j < test.size(); ++j)
            {
                CPPUNIT_ASSERT_EQUAL(values[j], bool(test[j]));
            }
        }
    }

    // insert n values
    {
        core::CSmallVectorBool<6> test;
        core::CSmallVectorBool<6>::iterator i = test.insert(test.end(), true);
        CPPUNIT_ASSERT_EQUAL(std::size_t(1), test.size());
        CPPUNIT_ASSERT_EQUAL(true, bool(*i));
        test.push_back(true);
        test.push_back(true);
        test.push_back(true);
        i = test.insert(test.begin() + 2, 2, false);
        CPPUNIT_ASSERT_EQUAL(std::size_t(6), test.size());
        CPPUNIT_ASSERT_EQUAL(false, bool(*i));
        {
            bool expected[] = { true, true, false, false, true, true };
            for (std::size_t j = 0u; j < 6; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(expected[j], bool(test[j]));
            }
        }
        i = test.insert(test.begin(), false);
        CPPUNIT_ASSERT_EQUAL(std::size_t(7), test.size());
        CPPUNIT_ASSERT_EQUAL(false, bool(*i));
        CPPUNIT_ASSERT_EQUAL(false, bool(test.front()));
        i = test.insert(test.begin() + 3, false);
        CPPUNIT_ASSERT_EQUAL(std::size_t(8), test.size());
        CPPUNIT_ASSERT_EQUAL(false, bool(*i));
        {
            bool expected[] = { false, true, true, false, false, false, true, true };
            for (std::size_t j = 0u; j < 8; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(expected[j], bool(test[j]));
            }
        }
        CPPUNIT_ASSERT(7 == (test.insert(test.end() - 1, 3, false) - test.begin()));
        CPPUNIT_ASSERT_EQUAL(std::size_t(11), test.size());
        {
            bool expected[] = { false, true, true, false, false, false, true, false, false, false, true };
            for (std::size_t j = 0u; j < 11; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(expected[j], bool(test[j]));
            }
        }
    }

    // insert range
    {
        core::CSmallVectorBool<7> test;
        test.push_back(true);
        test.push_back(false);
        core::CSmallVectorBool<7>::iterator i = test.insert(test.end(), &values[0], &values[2]);
        CPPUNIT_ASSERT_EQUAL(std::size_t(4), test.size());
        CPPUNIT_ASSERT_EQUAL(values[0], bool(*i));
        CPPUNIT_ASSERT(2 == (i - test.begin()));
        {
            bool expected[] = { true, false, values[0], values[1] };
            for (std::size_t j = 0u; j < 4; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(expected[j], bool(test[j]));
            }
        }
        i = test.insert(test.begin(), &values[2], &values[5]);
        CPPUNIT_ASSERT_EQUAL(std::size_t(7), test.size());
        CPPUNIT_ASSERT_EQUAL(values[2], bool(*i));
        CPPUNIT_ASSERT(0 == (i - test.begin()));
        {
            bool expected[] =
                {
                    values[2],
                    values[3],
                    values[4],
                    true,
                    false,
                    values[0],
                    values[1]
                };
            for (std::size_t j = 0u; j < 7; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(expected[j], bool(test[j]));
            }
        }
        i = test.insert(test.begin() + 3, &values[1], &values[2]);
        CPPUNIT_ASSERT_EQUAL(std::size_t(8), test.size());
        CPPUNIT_ASSERT_EQUAL(values[1], bool(*i));
        CPPUNIT_ASSERT(3 == (i - test.begin()));
        {
            bool expected[] =
                {
                    values[2],
                    values[3],
                    values[4],
                    values[1],
                    true,
                    false,
                    values[0],
                    values[1]
                };
            for (std::size_t j = 0u; j < 8; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(expected[j], bool(test[j]));
            }
        }
        i = test.insert(test.begin() + 5, &values[2], &values[4]);
        CPPUNIT_ASSERT_EQUAL(std::size_t(10), test.size());
        CPPUNIT_ASSERT_EQUAL(values[2], bool(*i));
        CPPUNIT_ASSERT(5 == (i - test.begin()));
        {
            bool expected[] =
                {
                    values[2],
                    values[3],
                    values[4],
                    values[1],
                    true,
                    values[2],
                    values[3],
                    false,
                    values[0],
                    values[1]
                };
            for (std::size_t j = 0u; j < 10; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(expected[j], bool(test[j]));
            }
        }
    }

    // erase
    {
        core::CSmallVectorBool<5> test(values, values + 5);
        core::CSmallVectorBool<5>::iterator i = test.erase(test.begin());
        CPPUNIT_ASSERT_EQUAL(std::size_t(4), test.size());
        CPPUNIT_ASSERT(0 == (i - test.begin()));
        CPPUNIT_ASSERT_EQUAL(values[1], bool(*i));
        {
            bool expected[] =
                {
                    values[1],
                    values[2],
                    values[3],
                    values[4]
                };
            for (std::size_t j = 0u; j < 4; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(expected[j], bool(test[j]));
            }
        }
        i = test.erase(test.begin() + 1, test.begin() + 3);
        CPPUNIT_ASSERT_EQUAL(std::size_t(2), test.size());
        CPPUNIT_ASSERT(1 == (i - test.begin()));
        CPPUNIT_ASSERT_EQUAL(values[4], bool(*i));
        {
            bool expected[] = { values[1], values[4] };
            for (std::size_t j = 0u; j < 2; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(expected[j], bool(test[j]));
            }
        }
        i = test.erase(test.begin(), test.end());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), test.size());
        CPPUNIT_ASSERT(0 == (i - test.begin()));
    }
    {
        core::CSmallVectorBool<3> test(values, values + 5);
        core::CSmallVectorBool<3>::iterator i = test.erase(test.begin());
        CPPUNIT_ASSERT_EQUAL(std::size_t(4), test.size());
        CPPUNIT_ASSERT(0 == (i - test.begin()));
        CPPUNIT_ASSERT_EQUAL(values[1], bool(*i));
        {
            bool expected[] =
                {
                    values[1],
                    values[2],
                    values[3],
                    values[4]
                };
            for (std::size_t j = 0u; j < 4; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(expected[j], bool(test[j]));
            }
        }
        i = test.erase(test.begin() + 1, test.begin() + 3);
        CPPUNIT_ASSERT_EQUAL(std::size_t(2), test.size());
        CPPUNIT_ASSERT(1 == (i - test.begin()));
        CPPUNIT_ASSERT_EQUAL(values[4], bool(*i));
        {
            bool expected[] = { values[1], values[4] };
            for (std::size_t j = 0u; j < 2; ++j)
            {
                CPPUNIT_ASSERT_EQUAL(expected[j], bool(test[j]));
            }
        }
        i = test.erase(test.begin(), test.end());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), test.size());
        CPPUNIT_ASSERT(0 == (i - test.begin()));
    }

    // swap
    {
        core::CSmallVectorBool<5> test1;
        test1.push_back(true);
        test1.push_back(true);
        core::CSmallVectorBool<5> test2;
        test2.push_back(false);
        test2.push_back(false);
        test2.push_back(false);
        test1.swap(test2);
        CPPUNIT_ASSERT_EQUAL(false, bool(test1[0]));
        CPPUNIT_ASSERT_EQUAL(false, bool(test1[1]));
        CPPUNIT_ASSERT_EQUAL(false, bool(test1[2]));
        CPPUNIT_ASSERT_EQUAL(true,  bool(test2[0]));
        CPPUNIT_ASSERT_EQUAL(true,  bool(test2[1]));
    }
    {
        core::CSmallVectorBool<3> test1;
        test1.push_back(false);
        test1.push_back(true);
        core::CSmallVectorBool<3> test2;
        test2.push_back(true);
        test2.push_back(false);
        test2.push_back(false);
        test2.push_back(true);
        test1.swap(test2);
        CPPUNIT_ASSERT_EQUAL(true,  bool(test1[0]));
        CPPUNIT_ASSERT_EQUAL(false, bool(test1[1]));
        CPPUNIT_ASSERT_EQUAL(false, bool(test1[2]));
        CPPUNIT_ASSERT_EQUAL(true,  bool(test1[3]));
        CPPUNIT_ASSERT_EQUAL(false, bool(test2[0]));
        CPPUNIT_ASSERT_EQUAL(true,  bool(test2[1]));
        test1.swap(test2);
        CPPUNIT_ASSERT_EQUAL(false, bool(test1[0]));
        CPPUNIT_ASSERT_EQUAL(true,  bool(test1[1]));
        CPPUNIT_ASSERT_EQUAL(true,  bool(test2[0]));
        CPPUNIT_ASSERT_EQUAL(false, bool(test2[1]));
        CPPUNIT_ASSERT_EQUAL(false, bool(test2[2]));
        CPPUNIT_ASSERT_EQUAL(true,  bool(test2[3]));
    }
    {
        core::CSmallVectorBool<1> test1;
        test1.push_back(false);
        test1.push_back(true);
        core::CSmallVectorBool<1> test2;
        test2.push_back(true);
        test2.push_back(false);
        test2.push_back(true);
        test1.swap(test2);
        CPPUNIT_ASSERT_EQUAL(true,  bool(test1[0]));
        CPPUNIT_ASSERT_EQUAL(false, bool(test1[1]));
        CPPUNIT_ASSERT_EQUAL(true,  bool(test1[2]));
        CPPUNIT_ASSERT_EQUAL(false, bool(test2[0]));
        CPPUNIT_ASSERT_EQUAL(true,  bool(test2[1]));
    }

    // clear
    {
        core::CSmallVectorBool<5> test;
        test.push_back(true);
        test.push_back(false);
        test.clear();
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), test.size());
        CPPUNIT_ASSERT_EQUAL(true, test.empty());
    }
    {
        core::CSmallVectorBool<1> test;
        test.push_back(true);
        test.push_back(true);
        test.clear();
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), test.size());
        CPPUNIT_ASSERT_EQUAL(true, test.empty());
    }
}

void CSmallVectorTest::testMemoryUsage(void)
{
    {
        core::CSmallVector<double, 5> test;
        for (std::size_t i = 0u; i < 5; ++i)
        {
            test.push_back(1.0);
            CPPUNIT_ASSERT_EQUAL(std::size_t(0), test.memoryUsage());
        }

        // Trigger creation of dynamic storage.
        test.push_back(1.0);

        LOG_DEBUG("memory usage = " << test.memoryUsage());
        CPPUNIT_ASSERT(test.memoryUsage() >= 6 * sizeof(double));
    }
    {
        // Test member object with dynamic size.
        core::CSmallVector<CObjectWhichUsesMemory, 3> test(3);

        CPPUNIT_ASSERT_EQUAL(std::size_t(300), test.memoryUsage());

        test.push_back(CObjectWhichUsesMemory());

        LOG_DEBUG("memory usage = " << test.memoryUsage());
        CPPUNIT_ASSERT(test.memoryUsage() >= 4 * CObjectWhichUsesMemory::size());
    }
}

CppUnit::Test *CSmallVectorTest::suite(void)
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CSmallVectorTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CSmallVectorTest>(
                                   "CSmallVectorTest::testConstruction",
                                   &CSmallVectorTest::testConstruction) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CSmallVectorTest>(
                                   "CSmallVectorTest::testCapacity",
                                   &CSmallVectorTest::testCapacity) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CSmallVectorTest>(
                                   "CSmallVectorTest::testElementAccess",
                                   &CSmallVectorTest::testElementAccess) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CSmallVectorTest>(
                                   "CSmallVectorTest::testModifiers",
                                   &CSmallVectorTest::testModifiers) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CSmallVectorTest>(
                                   "CSmallVectorTest::testIterators",
                                   &CSmallVectorTest::testIterators) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CSmallVectorTest>(
                                   "CSmallVectorTest::testOperators",
                                   &CSmallVectorTest::testOperators) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CSmallVectorTest>(
                                   "CSmallVectorTest::testVectorBool",
                                   &CSmallVectorTest::testVectorBool) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CSmallVectorTest>(
                                   "CSmallVectorTest::testMemoryUsage",
                                   &CSmallVectorTest::testMemoryUsage) );

    return suiteOfTests;
}
