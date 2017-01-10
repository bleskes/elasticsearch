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
#include "CTextFileWatcherTest.h"

#include <core/CSleep.h>
#include <core/CTextFileWatcher.h>

#include <boost/bind.hpp>

#include <fstream>


CppUnit::Test *CTextFileWatcherTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CTextFileWatcherTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CTextFileWatcherTest>(
                                   "CTextFileWatcherTest::testSyslog",
                                   &CTextFileWatcherTest::testSyslog) );

    return suiteOfTests;
}

namespace
{

typedef std::vector<std::string> TStrVec;
typedef TStrVec::iterator        TStrVecItr;
typedef TStrVec::const_iterator  TStrVecCItr;

class CVisitor
{
    public:
        CVisitor(void)
        {
        }

        void clear(void)
        {
            m_Strings.clear();
        }

        bool visit(const std::string &s)
        {
            m_Strings.push_back(s);

            return true;
        }

        const TStrVec strings(void) const
        {
            return m_Strings;
        }

    private:
        TStrVec m_Strings;

    private:
        CVisitor(const CVisitor &);
        CVisitor &operator=(const CVisitor &);
};

}

void CTextFileWatcherTest::testSyslog(void)
{
    ml::core::CTextFileWatcher watcher;

    CPPUNIT_ASSERT(watcher.init("files/mltest.log", "<END>\n",
                    ml::core::CTextFileWatcher::E_End));
    CVisitor v;

    for (;;)
    {
        CPPUNIT_ASSERT(watcher.changes(boost::bind(&CVisitor::visit, &v, _1)));

        const TStrVec &actual = v.strings();

        LOG_DEBUG("Received " << actual.size() << " strings");

        if (actual.size() == 60000)
        {
            LOG_DEBUG("Done");
            return;
        }

        ml::core::CSleep::sleep(1000);
    }

}

