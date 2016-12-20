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

#include "CStringStoreTest.h"

#include <core/CContainerPrinter.h>
#include <core/CLogger.h>
#include <core/CThread.h>

#include <model/CStringStore.h>

#include <cppunit/Exception.h>

#include <boost/shared_ptr.hpp>

using namespace prelert;
using namespace model;

namespace
{
typedef std::vector<std::size_t> TSizeVec;
typedef std::vector<std::string> TStrVec;
typedef CStringStore::TStrPtr TStrPtr;
typedef std::vector<TStrPtr> TStrPtrVec;
typedef boost::unordered_set<const std::string*> TStrCPtrUSet;

class CStringThread : public core::CThread
{
    public:
        typedef boost::shared_ptr<CppUnit::Exception> TCppUnitExceptionP;

    public:
        CStringThread(std::size_t i, const TStrVec &strings)
            : m_I(i),
              m_Strings(strings)
        {
        }

        void uniques(TStrCPtrUSet &result) const
        {
            result.insert(m_UniquePtrs.begin(), m_UniquePtrs.end());
        }

        void propagateLastThreadAssert(void)
        {
            if (m_LastException != 0)
            {
                throw *m_LastException;
            }
        }

        void clearPtrs(void)
        {
            m_UniquePtrs.clear();
            m_Ptrs.clear();
        }

    private:
        virtual void run(void)
        {
            try
            {
                std::size_t n = m_Strings.size();
                for (std::size_t i = m_I; i < 1000; ++i)
                {
                    m_Ptrs.push_back(TStrPtr());
                    m_Ptrs.back() = CStringStore::names().get(m_Strings[i % n]);
                    m_UniquePtrs.insert(m_Ptrs.back().get());
                    CPPUNIT_ASSERT_EQUAL(m_Strings[i % n], *m_Ptrs.back());
                }
                for (std::size_t i = m_I; i < 1000000; ++i)
                {
                    TStrPtr p = CStringStore::names().get(m_Strings[i % n]);
                    m_UniquePtrs.insert(p.get());
                    CPPUNIT_ASSERT_EQUAL(m_Strings[i % n], *p);
                }
            }
            // CppUnit won't automatically catch the exceptions thrown by
            // assertions in newly created threads, so propagate manually
            catch (CppUnit::Exception &e)
            {
                m_LastException.reset(new CppUnit::Exception(e));
            }
        }

        virtual void shutdown(void)
        {
        }

    private:
        std::size_t        m_I;
        TStrVec            m_Strings;
        TStrPtrVec         m_Ptrs;
        TStrCPtrUSet       m_UniquePtrs;
        TCppUnitExceptionP m_LastException;
};

}

void CStringStoreTest::testStringStore(void)
{
    CStringStore::names().clearEverythingTestOnly();
    CStringStore::influencers().clearEverythingTestOnly();

    TStrVec strings;
    strings.push_back("Milano");
    strings.push_back("Monza");
    strings.push_back("Amalfi");
    strings.push_back("Pompei");
    strings.push_back("Gragnano");
    strings.push_back("Roma");
    strings.push_back("Bologna");
    strings.push_back("Torina");
    strings.push_back("Napoli");
    strings.push_back("Rimini");
    strings.push_back("Genova");
    strings.push_back("Capri");
    strings.push_back("Ravello");
    strings.push_back("Reggio");
    strings.push_back("Palermo");
    strings.push_back("Focaccino");

    LOG_DEBUG("*** CStringStoreTest ***");
    {
        LOG_DEBUG("Testing basic insert");
        TStrPtr pG = CStringStore::names().get("Gragnano");
        CPPUNIT_ASSERT(pG);
        CPPUNIT_ASSERT_EQUAL(std::string("Gragnano"), *pG);

        TStrPtr pG2 = CStringStore::names().get("Gragnano");
        CPPUNIT_ASSERT(pG2);
        CPPUNIT_ASSERT_EQUAL(std::string("Gragnano"), *pG2);
        CPPUNIT_ASSERT_EQUAL(pG.get(), pG2.get());
        CPPUNIT_ASSERT_EQUAL(*pG, *pG2);

        CPPUNIT_ASSERT_EQUAL(std::size_t(1), CStringStore::names().m_Strings.size());
    }
    CPPUNIT_ASSERT_EQUAL(std::size_t(1), CStringStore::names().m_Strings.size());
    CStringStore::names().pruneNotThreadSafe();
    CPPUNIT_ASSERT_EQUAL(std::size_t(0), CStringStore::names().m_Strings.size());

    {
        LOG_DEBUG("Testing multi-threaded");

        typedef boost::shared_ptr<CStringThread> TThreadPtr;
        typedef std::vector<TThreadPtr> TThreadVec;
        TThreadVec threads;
        for (std::size_t i = 0; i < 20; ++i)
        {
            threads.push_back(TThreadPtr(new CStringThread(i, strings)));
        }
        for (std::size_t i = 0; i < threads.size(); ++i)
        {
            CPPUNIT_ASSERT(threads[i]->start());
        }
        for (std::size_t i = 0; i < threads.size(); ++i)
        {
            CPPUNIT_ASSERT(threads[i]->waitForFinish());
        }

        CPPUNIT_ASSERT_EQUAL(strings.size(), CStringStore::names().m_Strings.size());
        CStringStore::names().pruneNotThreadSafe();
        CPPUNIT_ASSERT_EQUAL(strings.size(), CStringStore::names().m_Strings.size());
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), CStringStore::influencers().m_Strings.size());

        for (std::size_t i = 0; i < threads.size(); ++i)
        {
            // CppUnit won't automatically catch the exceptions thrown by
            // assertions in newly created threads, so propagate manually
            threads[i]->propagateLastThreadAssert();
        }
        for (std::size_t i = 0; i < threads.size(); ++i)
        {
            threads[i]->clearPtrs();
        }

        CPPUNIT_ASSERT_EQUAL(strings.size(), CStringStore::names().m_Strings.size());
        CStringStore::names().pruneNotThreadSafe();
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), CStringStore::names().m_Strings.size());
        threads.clear();
        CPPUNIT_ASSERT_EQUAL(std::size_t(0), CStringStore::names().m_Strings.size());
    }
    {
        LOG_DEBUG("Testing multi-threaded string duplication rate");

        TStrVec lotsOfStrings;
        for (std::size_t i = 0u; i < 1000; ++i)
        {
            lotsOfStrings.push_back(core::CStringUtils::typeToString(i));
        }

        typedef boost::shared_ptr<CStringThread> TThreadPtr;
        typedef std::vector<TThreadPtr> TThreadVec;
        TThreadVec threads;
        for (std::size_t i = 0; i < 20; ++i)
        {
            threads.push_back(TThreadPtr(new CStringThread(i * 50, lotsOfStrings)));
        }
        for (std::size_t i = 0; i < threads.size(); ++i)
        {
            CPPUNIT_ASSERT(threads[i]->start());
        }
        for (std::size_t i = 0; i < threads.size(); ++i)
        {
            CPPUNIT_ASSERT(threads[i]->waitForFinish());
        }

        for (std::size_t i = 0; i < threads.size(); ++i)
        {
            // CppUnit won't automatically catch the exceptions thrown by
            // assertions in newly created threads, so propagate manually
            threads[i]->propagateLastThreadAssert();
        }

        TStrCPtrUSet uniques;
        for (std::size_t i = 0; i < threads.size(); ++i)
        {
            threads[i]->uniques(uniques);
        }
        LOG_DEBUG("unique counts = " << uniques.size());
        CPPUNIT_ASSERT(uniques.size() < 20000);

        // Tidy up
        for (std::size_t i = 0; i < threads.size(); ++i)
        {
            threads[i]->clearPtrs();
        }
        CStringStore::names().pruneNotThreadSafe();
    }
}

CppUnit::Test *CStringStoreTest::suite(void)
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CStringStoreTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CStringStoreTest>(
                                   "CStringStoreTest::testStringStore",
                                   &CStringStoreTest::testStringStore) );

    return suiteOfTests;
}
