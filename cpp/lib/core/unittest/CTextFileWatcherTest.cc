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
// NB: The tests in this file rely on msysgit translating the linefeeds in the
// test files to carriage return + linefeed on Windows machines.  This test may
// need to be redesigned if we change to a source code control system that
// doesn't do LF -> CRLF mapping automatically on Windows.

#include "CTextFileWatcherTest.h"

#include <core/CLogger.h>
#include <core/CoreTypes.h>
#include <core/CStringUtils.h>
#include <core/CTextFileWatcher.h>

#include <boost/bind.hpp>

#include <fstream>
#include <sstream>

#include <stdio.h>


CppUnit::Test *CTextFileWatcherTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CTextFileWatcherTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CTextFileWatcherTest>(
                                   "CTextFileWatcherTest::testInitStart",
                                   &CTextFileWatcherTest::testInitStart) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTextFileWatcherTest>(
                                   "CTextFileWatcherTest::testInitEnd1",
                                   &CTextFileWatcherTest::testInitEnd1) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTextFileWatcherTest>(
                                   "CTextFileWatcherTest::testInitEnd2",
                                   &CTextFileWatcherTest::testInitEnd2) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTextFileWatcherTest>(
                                   "CTextFileWatcherTest::testReadAllLines1",
                                   &CTextFileWatcherTest::testReadAllLines1) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTextFileWatcherTest>(
                                   "CTextFileWatcherTest::testReadAllLines2",
                                   &CTextFileWatcherTest::testReadAllLines2) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTextFileWatcherTest>(
                                   "CTextFileWatcherTest::testMultiLineMessages",
                                   &CTextFileWatcherTest::testMultiLineMessages) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTextFileWatcherTest>(
                                   "CTextFileWatcherTest::testFileRename",
                                   &CTextFileWatcherTest::testFileRename) );

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

void CTextFileWatcherTest::testInitStart(void)
{
    ml::core::CTextFileWatcher watcher;

    CPPUNIT_ASSERT(watcher.init("testfiles/CTextFileWatcherTest1.txt",
                                ml::core_t::LINE_ENDING,
                                ml::core::CTextFileWatcher::E_Start));
    CVisitor v;

    CPPUNIT_ASSERT(watcher.changes(boost::bind(&CVisitor::visit, &v, _1)));

    const TStrVec &actual = v.strings();

    std::ifstream ifs("testfiles/CTextFileWatcherTest1.txt");
    CPPUNIT_ASSERT(ifs.is_open());
    TStrVec expected;

    std::string line;
    while (std::getline(ifs, line))
    {
        expected.push_back(line);
    }

    CPPUNIT_ASSERT_EQUAL(expected.size(), actual.size());

    TStrVecCItr itr = actual.begin();
    TStrVecCItr jtr = expected.begin();
    for (; itr != actual.end(); ++itr, ++jtr)
    {
        CPPUNIT_ASSERT_EQUAL(*jtr, *itr);
    }

    // What delimiter did we actually see?
    CPPUNIT_ASSERT_EQUAL(std::string(ml::core_t::LINE_ENDING), watcher.exampleDelimiter());
}

void CTextFileWatcherTest::testInitEnd1(void)
{
    std::ofstream fs("testfiles/CTextFileWatcherTest2.txt",
                     std::fstream::out | std::fstream::trunc);
    CPPUNIT_ASSERT(fs.is_open());

    // Write some text
    for (int i = 0; i < 10; ++i)
    {
        fs << i << " syslog message <END>" << std::endl;
    }

    // Watcher should pick up nothing
    ml::core::CTextFileWatcher watcher;

    CPPUNIT_ASSERT(watcher.init("testfiles/CTextFileWatcherTest2.txt",
                                "<END>\r?\n",
                                ml::core::CTextFileWatcher::E_End));
    CVisitor v;
    {
        CPPUNIT_ASSERT(watcher.changes(boost::bind(&CVisitor::visit, &v, _1)));
        const TStrVec &actual = v.strings();
        CPPUNIT_ASSERT(actual.empty());
    }

    TStrVec expected;

    // Add 10 more lines
    for (int i = 10; i < 20; ++i)
    {
        std::ostringstream strm;

        fs << i << " New syslog message <END>" << std::endl;
        strm << i << " New syslog message ";
        expected.push_back(strm.str());
    }
    fs.flush();

    {
        CPPUNIT_ASSERT(watcher.changes(boost::bind(&CVisitor::visit, &v, _1)));
        const TStrVec &actual = v.strings();

        CPPUNIT_ASSERT_EQUAL(expected.size(), actual.size());

        TStrVecCItr itr = actual.begin();
        TStrVecCItr jtr = expected.begin();
        for (; itr != actual.end(); ++itr, ++jtr)
        {
            CPPUNIT_ASSERT_EQUAL(*jtr, *itr);
        }

        v.clear();
    }

    // Add a partial line
    fs << "New partial line";
    fs.flush();

    {
        CPPUNIT_ASSERT(watcher.changes(boost::bind(&CVisitor::visit, &v, _1)));
        const TStrVec &actual = v.strings();
        CPPUNIT_ASSERT(actual.empty());
    }

    // Complete the line
    fs << ", now completed<END>" << std::endl;
    fs.flush();
    expected.clear();
    expected.push_back(std::string("New partial line") + std::string(", now completed"));

    {
        CPPUNIT_ASSERT(watcher.changes(boost::bind(&CVisitor::visit, &v, _1)));
        const TStrVec &actual = v.strings();

        CPPUNIT_ASSERT_EQUAL(expected.size(), actual.size());

        TStrVecCItr itr = actual.begin();
        TStrVecCItr jtr = expected.begin();
        for (; itr != actual.end(); ++itr, ++jtr)
        {
            CPPUNIT_ASSERT_EQUAL(*jtr, *itr);
        }

        v.clear();
    }

    // What delimiter did we actually see?
    CPPUNIT_ASSERT_EQUAL(std::string("<END>") + ml::core_t::LINE_ENDING, watcher.exampleDelimiter());
}

void CTextFileWatcherTest::testInitEnd2(void)
{
    std::ofstream fs("testfiles/CTextFileWatcherTest5.txt",
                     std::fstream::out | std::fstream::trunc);
    CPPUNIT_ASSERT(fs.is_open());

    // Write some text
    for (int i = 0; i < 10; ++i)
    {
        fs << "Message " << i << std::endl;
    }

    ml::core::CTextFileWatcher watcher;

    CPPUNIT_ASSERT(watcher.init("testfiles/CTextFileWatcherTest5.txt",
                                "\r?\n",
                                ml::core::CTextFileWatcher::E_End));

    CVisitor v;

    // Read first line - should not affect current offset
    {
        CPPUNIT_ASSERT(watcher.readFirstLine(boost::bind(&CVisitor::visit, &v, _1)));
        const TStrVec &actual = v.strings();
        CPPUNIT_ASSERT_EQUAL(size_t(1), actual.size());
        LOG_DEBUG(actual.front());
        CPPUNIT_ASSERT_EQUAL(std::string("Message 0"), actual.front());
        v.clear();
    }

    // Watcher should pick up nothing, despite there being 9 lines after the
    // first one that was just read
    {
        CPPUNIT_ASSERT(watcher.changes(boost::bind(&CVisitor::visit, &v, _1)));
        const TStrVec &actual = v.strings();
        CPPUNIT_ASSERT(actual.empty());
    }

    TStrVec expected;

    // Add 10 more lines
    for (int i = 10; i < 20; ++i)
    {
        std::ostringstream strm;
        strm << "New message " << i;

        fs << strm.str() << std::endl;
        expected.push_back(strm.str());
    }
    fs.flush();

    {
        CPPUNIT_ASSERT(watcher.changes(boost::bind(&CVisitor::visit, &v, _1)));
        const TStrVec &actual = v.strings();

        CPPUNIT_ASSERT_EQUAL(expected.size(), actual.size());

        TStrVecCItr itr = actual.begin();
        TStrVecCItr jtr = expected.begin();
        for (; itr != actual.end(); ++itr, ++jtr)
        {
            CPPUNIT_ASSERT_EQUAL(*jtr, *itr);
        }

        v.clear();
    }

    // What delimiter did we actually see?
    CPPUNIT_ASSERT_EQUAL(std::string(ml::core_t::LINE_ENDING), watcher.exampleDelimiter());
}

void CTextFileWatcherTest::testReadAllLines1(void)
{

{
    ml::core::CTextFileWatcher watcher;

    CPPUNIT_ASSERT(watcher.init("testfiles/CTextFileWatcherTest1.txt",
                                "\r?\n",
                                ml::core::CTextFileWatcher::E_Start));
    CVisitor v;

    std::string remainder;
    CPPUNIT_ASSERT(watcher.readAllLines(boost::bind(&CVisitor::visit, &v, _1), remainder));
    CPPUNIT_ASSERT(remainder.empty());

    const TStrVec &actual = v.strings();

    std::ifstream ifs("testfiles/CTextFileWatcherTest1.txt");
    CPPUNIT_ASSERT(ifs.is_open());

    std::string line;
    std::string file;
    while (std::getline(ifs, line))
    {
        file += line;
        file += '\n';
    }

    ml::core::CStringUtils::TStrVec expected;

    ml::core::CStringUtils::tokenise("\n", file, expected, remainder);

    CPPUNIT_ASSERT(remainder.empty());

/*
    TStrVecCItr itr = actual.begin();
    TStrVecCItr jtr = expected.begin();

    for (; itr != actual.end(); ++itr, ++jtr)
    {
        LOG_DEBUG(*itr);
        LOG_DEBUG(*jtr);
    }
*/

    CPPUNIT_ASSERT_EQUAL(expected.size(), actual.size());

    TStrVecCItr itr = actual.begin();
    TStrVecCItr jtr = expected.begin();
    for (; itr != actual.end(); ++itr, ++jtr)
    {
        CPPUNIT_ASSERT_EQUAL(*jtr, *itr);
    }
}
{
    ml::core::CTextFileWatcher watcher;

    CPPUNIT_ASSERT(watcher.init("testfiles/CTextFileWatcherTest1.txt",
                                "\r?\n",
                                ml::core::CTextFileWatcher::E_End));
    CVisitor v;

    std::string remainder;
    CPPUNIT_ASSERT(watcher.readAllLines(boost::bind(&CVisitor::visit, &v, _1), remainder));
    CPPUNIT_ASSERT(remainder.empty());

    const TStrVec &actual = v.strings();

    std::ifstream ifs("testfiles/CTextFileWatcherTest1.txt");
    CPPUNIT_ASSERT(ifs.is_open());
    TStrVec expected;

    std::string line;
    while (std::getline(ifs, line))
    {
        expected.push_back(line);
    }

    TStrVecCItr itr = actual.begin();
    TStrVecCItr jtr = expected.begin();
    for (; itr != actual.end(); ++itr, ++jtr)
    {
        CPPUNIT_ASSERT_EQUAL(*jtr, *itr);
    }

    // What delimiter did we actually see?
    CPPUNIT_ASSERT_EQUAL(std::string(ml::core_t::LINE_ENDING), watcher.exampleDelimiter());
}

}

void CTextFileWatcherTest::testReadAllLines2(void)
{
    ml::core::CTextFileWatcher watcher;

    CPPUNIT_ASSERT(watcher.init("testfiles/CTextFileWatcherTest3.txt",
                                "<END>",
                                ml::core::CTextFileWatcher::E_Start));
    CVisitor v;

    std::string remainder;
    CPPUNIT_ASSERT(watcher.readAllLines(boost::bind(&CVisitor::visit, &v, _1), remainder));
    CPPUNIT_ASSERT_EQUAL(std::string(ml::core_t::LINE_ENDING), remainder);

    const TStrVec &actual = v.strings();

    std::ifstream ifs("testfiles/CTextFileWatcherTest3.txt");
    CPPUNIT_ASSERT(ifs.is_open());

    std::string line;
    std::string file;
    while (std::getline(ifs, line))
    {
        file += line;
        file += ml::core_t::LINE_ENDING;
    }

    ml::core::CStringUtils::TStrVec expected;

    ml::core::CStringUtils::tokenise("<END>", file, expected, remainder);

    CPPUNIT_ASSERT_EQUAL(std::string(ml::core_t::LINE_ENDING), remainder);

    CPPUNIT_ASSERT_EQUAL(expected.size(), actual.size());

    TStrVecCItr itr = actual.begin();
    TStrVecCItr jtr = expected.begin();

    for (; itr != actual.end(); ++itr, ++jtr)
    {
        CPPUNIT_ASSERT_EQUAL(*jtr, *itr);
    }

    // What delimiter did we actually see?
    CPPUNIT_ASSERT_EQUAL(std::string("<END>"), watcher.exampleDelimiter());
}

void CTextFileWatcherTest::testMultiLineMessages(void)
{
    ml::core::CTextFileWatcher watcher;

    // All messages begin with a date formatted along the lines of
    //
    //          Oct 11, 2008 3:11:51 PM
    //
    // so the regex that must follow a delimiter is based on that
    ml::core::CDelimiter delimiter("\n", "\\w+\\s+\\d+,\\s+\\d+\\s+\\d+:\\d+:\\d+\\s+\\w+", true);

    // There are 2373 lines in the file, making up 1106 messages.
    // Each message is spread over at least 2 lines, but some messages spread
    // over even more lines (e.g. Java exceptions).
    const size_t totalMessages = 1106;
    CPPUNIT_ASSERT(watcher.init("testfiles/CTextFileWatcherTest4.txt",
                                delimiter,
                                ml::core::CTextFileWatcher::E_Start));

    // The first time we ask what's changed in the file, we should get all but
    // the last message, because the regex doesn't follow the last message, and
    // it's the first time we've read the file.
    CVisitor v1;
    CPPUNIT_ASSERT(watcher.changes(boost::bind(&CVisitor::visit, &v1, _1)));

    const TStrVec &actual1 = v1.strings();
    CPPUNIT_ASSERT_EQUAL(totalMessages - 1, actual1.size());

    // All messages should be multi-line
    for (TStrVecCItr itr = actual1.begin();
         itr != actual1.end();
         ++itr)
    {
        LOG_DEBUG("Message is:\n" << *itr << '\n');
        CPPUNIT_ASSERT(itr->find("\n") != std::string::npos);
    }

    // The second time we ask what's changed in the file, we should get the last
    // message, because this time the delimiter alone is enough for the message
    // at the very end of the file.  This should happen even though the file
    // itself hasn't changed.
    CVisitor v2;
    CPPUNIT_ASSERT(watcher.changes(boost::bind(&CVisitor::visit, &v2, _1)));

    const TStrVec &actual2 = v2.strings();
    CPPUNIT_ASSERT_EQUAL(size_t(1), actual2.size());

    LOG_DEBUG("Last message is:\n" << actual2[0] << '\n');

    // Message should be multi-line
    CPPUNIT_ASSERT(actual2[0].find("\n") != std::string::npos);

    // What delimiter did we actually see?
    CPPUNIT_ASSERT_EQUAL(std::string("\n"), watcher.exampleDelimiter());
}

void CTextFileWatcherTest::testFileRename(void)
{
    // Clean up any previous failed runs of this test - DON'T assert the
    // results of this cleanup as it may not need to do anything
    ::remove("testfiles/CTextFileWatcherTest6.txt");
    ::remove("testfiles/CTextFileWatcherTest6.txt.1");
    ::remove("testfiles/CTextFileWatcherTest6.txt.2");

    std::ofstream fs("testfiles/CTextFileWatcherTest6.txt",
                     std::fstream::out | std::fstream::trunc);
    CPPUNIT_ASSERT(fs.is_open());

    // Write some text
    for (int i = 0; i < 10; ++i)
    {
        fs << i << " syslog message <END>" << std::endl;
    }

    // Watcher should pick up nothing
    // Use 3 retries if the file is not available after a rename
    ml::core::CTextFileWatcher watcher(3);

    CPPUNIT_ASSERT(watcher.init("testfiles/CTextFileWatcherTest6.txt",
                                "<END>\r?\n",
                                ml::core::CTextFileWatcher::E_End));
    CVisitor v;
    {
        CPPUNIT_ASSERT(watcher.changes(boost::bind(&CVisitor::visit, &v, _1)));
        const TStrVec &actual = v.strings();
        CPPUNIT_ASSERT(actual.empty());
    }

    TStrVec expected;

    // Add 10 more lines
    for (int i = 10; i < 20; ++i)
    {
        std::ostringstream strm;

        fs << i << " New syslog message <END>" << std::endl;
        strm << i << " New syslog message ";
        expected.push_back(strm.str());
    }
    fs.flush();

    {
        CPPUNIT_ASSERT(watcher.changes(boost::bind(&CVisitor::visit, &v, _1)));
        const TStrVec &actual = v.strings();

        CPPUNIT_ASSERT_EQUAL(expected.size(), actual.size());

        TStrVecCItr itr = actual.begin();
        TStrVecCItr jtr = expected.begin();
        for (; itr != actual.end(); ++itr, ++jtr)
        {
            CPPUNIT_ASSERT_EQUAL(*jtr, *itr);
        }
    }

    // Now rename the file - the watcher will still be reading the original file
    fs.close();
    CPPUNIT_ASSERT_EQUAL(0, ::rename("testfiles/CTextFileWatcherTest6.txt", "testfiles/CTextFileWatcherTest6.txt.1"));

    // We asked for 3 retries, so the missing file should not be reported as an
    // error
    CPPUNIT_ASSERT(watcher.changes(boost::bind(&CVisitor::visit, &v, _1)));
    CPPUNIT_ASSERT(watcher.changes(boost::bind(&CVisitor::visit, &v, _1)));
    CPPUNIT_ASSERT(watcher.changes(boost::bind(&CVisitor::visit, &v, _1)));

    // Now create the original file name again
    fs.open("testfiles/CTextFileWatcherTest6.txt");

    // This read should detect that the file has moved, and open the new file
    // with the original name
    CPPUNIT_ASSERT(watcher.changes(boost::bind(&CVisitor::visit, &v, _1)));

    // Write some text to the new file
    for (int i = 0; i < 10; ++i)
    {
        std::ostringstream strm;

        fs << i << " syslog message <END>" << std::endl;
        strm << i << " syslog message ";
        expected.push_back(strm.str());
    }
    fs.flush();

    {
        CPPUNIT_ASSERT(watcher.changes(boost::bind(&CVisitor::visit, &v, _1)));
        const TStrVec &actual = v.strings();

        CPPUNIT_ASSERT_EQUAL(expected.size(), actual.size());

        TStrVecCItr itr = actual.begin();
        TStrVecCItr jtr = expected.begin();
        for (; itr != actual.end(); ++itr, ++jtr)
        {
            CPPUNIT_ASSERT_EQUAL(*jtr, *itr);
        }
    }

    // Now rename the file again - the watcher will still be reading the second
    // file
    fs.close();
    CPPUNIT_ASSERT_EQUAL(0, ::rename("testfiles/CTextFileWatcherTest6.txt", "testfiles/CTextFileWatcherTest6.txt.2"));

    // We asked for 3 retries, so the missing file should be reported as an
    // error on the 4th unsuccessful read
    CPPUNIT_ASSERT(watcher.changes(boost::bind(&CVisitor::visit, &v, _1)));
    CPPUNIT_ASSERT(watcher.changes(boost::bind(&CVisitor::visit, &v, _1)));
    CPPUNIT_ASSERT(watcher.changes(boost::bind(&CVisitor::visit, &v, _1)));
    CPPUNIT_ASSERT(!watcher.changes(boost::bind(&CVisitor::visit, &v, _1)));

    // What delimiter did we actually see?
    CPPUNIT_ASSERT_EQUAL(std::string("<END>") + ml::core_t::LINE_ENDING, watcher.exampleDelimiter());

    // Assuming all the assertions were OK, clean up the renamed files
    ::remove("testfiles/CTextFileWatcherTest6.txt.1");
    ::remove("testfiles/CTextFileWatcherTest6.txt.2");
}

