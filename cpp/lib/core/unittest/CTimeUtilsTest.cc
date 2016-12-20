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
#include "CTimeUtilsTest.h"

#include <core/CCTimeR.h>
#include <core/CLogger.h>
#include <core/CSleep.h>
#include <core/CTimeUtils.h>
#include <core/CTimezone.h>

#include <time.h>


CppUnit::Test *CTimeUtilsTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CTimeUtilsTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CTimeUtilsTest>(
                                   "CTimeUtilsTest::testNow",
                                   &CTimeUtilsTest::testNow) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTimeUtilsTest>(
                                   "CTimeUtilsTest::testToIso8601",
                                   &CTimeUtilsTest::testToIso8601) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTimeUtilsTest>(
                                   "CTimeUtilsTest::testToLocal",
                                   &CTimeUtilsTest::testToLocal) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTimeUtilsTest>(
                                   "CTimeUtilsTest::testStrptime",
                                   &CTimeUtilsTest::testStrptime) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTimeUtilsTest>(
                                   "CTimeUtilsTest::testTimezone",
                                   &CTimeUtilsTest::testTimezone) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTimeUtilsTest>(
                                   "CTimeUtilsTest::testDateWords",
                                   &CTimeUtilsTest::testDateWords) );

    return suiteOfTests;
}

void CTimeUtilsTest::testNow(void)
{
    prelert::core_t::TTime t1(prelert::core::CTimeUtils::now());
    prelert::core::CSleep::sleep(1001);
    prelert::core_t::TTime t2(prelert::core::CTimeUtils::now());

    CPPUNIT_ASSERT(t2 > t1);
}

void CTimeUtilsTest::testToIso8601(void)
{
    // These tests assume UK time.  In case they're ever run outside the UK,
    // we'll explicitly set the timezone for the purpose of these tests.
    CPPUNIT_ASSERT(prelert::core::CTimezone::setTimezone("Europe/London"));

    {
        prelert::core_t::TTime t(1227710437);
        std::string expected("2008-11-26T14:40:37+0000");

        const std::string strRep = prelert::core::CTimeUtils::toIso8601(t);

        CPPUNIT_ASSERT_EQUAL(expected, strRep);
    }
    {
        prelert::core_t::TTime t(1207925624);
        std::string expected("2008-04-11T15:53:44+0100");

        const std::string strRep = prelert::core::CTimeUtils::toIso8601(t);

        CPPUNIT_ASSERT_EQUAL(expected, strRep);
    }
}

void CTimeUtilsTest::testToLocal(void)
{
    // These tests assume UK time.  In case they're ever run outside the UK,
    // we'll explicitly set the timezone for the purpose of these tests.
    CPPUNIT_ASSERT(prelert::core::CTimezone::setTimezone("Europe/London"));

    {
        prelert::core_t::TTime t(1227710437);
#ifdef Windows
        std::string expected("11/26/08 14:40:37");
#else
        std::string expected("Wed Nov 26 14:40:37 2008");
#endif

        const std::string strRep = prelert::core::CTimeUtils::toLocalString(t);

        CPPUNIT_ASSERT_EQUAL(expected, strRep);
    }
    {
        prelert::core_t::TTime t(1207925624);
#ifdef Windows
        std::string expected("04/11/08 15:53:44");
#else
        std::string expected("Fri Apr 11 15:53:44 2008");
#endif

        const std::string strRep = prelert::core::CTimeUtils::toLocalString(t);

        CPPUNIT_ASSERT_EQUAL(expected, strRep);
    }
    {
        prelert::core_t::TTime t(1207925624);
        std::string expected("15:53:44");

        const std::string strRep = prelert::core::CTimeUtils::toTimeString(t);

        CPPUNIT_ASSERT_EQUAL(expected, strRep);
    }
}

void CTimeUtilsTest::testStrptime(void)
{
    // These tests assume UK time.  In case they're ever run outside the UK,
    // we'll explicitly set the timezone for the purpose of these tests.
    CPPUNIT_ASSERT(prelert::core::CTimezone::setTimezone("Europe/London"));

{
    // This time is deliberately chosen to be during daylight saving time
    std::string dateTime("1122334455");

    std::string format("%s");

    prelert::core_t::TTime expected(1122334455);
    prelert::core_t::TTime actual(0);

    CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(format, dateTime, actual));
    CPPUNIT_ASSERT_EQUAL(expected, actual);
}
{
    std::string dateTime("2008-11-26 14:40:37");

    std::string format("%Y-%m-%d %H:%M:%S");

    prelert::core_t::TTime expected(1227710437);
    prelert::core_t::TTime actual(0);

    CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(format, dateTime, actual));
    CPPUNIT_ASSERT_EQUAL(expected, actual);

    std::string badDateTime("2008-11-26 25:40:37");
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::strptime(format, badDateTime, actual));
}
{
    std::string dateTime("10/31/2008 3:15:00 AM");

    std::string format("%m/%d/%Y %I:%M:%S %p");

    prelert::core_t::TTime expected(1225422900);
    prelert::core_t::TTime actual(0);

    CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(format, dateTime, actual));
    CPPUNIT_ASSERT_EQUAL(expected, actual);
    LOG_DEBUG(actual);
}
{
    std::string dateTime("Fri Oct 31  3:15:00 AM GMT 08");

    std::string format("%a %b %d %I:%M:%S %p %Z %y");

    prelert::core_t::TTime expected(1225422900);
    prelert::core_t::TTime actual(0);

    CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(format, dateTime, actual));
    CPPUNIT_ASSERT_EQUAL(expected, actual);
    LOG_DEBUG(actual);
}
{
    std::string dateTime("Tue Jun 23  17:24:55 2009");

    std::string format("%a %b %d %T %Y");

    prelert::core_t::TTime expected(1245774295);
    prelert::core_t::TTime actual(0);

    CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(format, dateTime, actual));
    CPPUNIT_ASSERT_EQUAL(expected, actual);
    LOG_DEBUG(actual);
}
{
    std::string dateTime("Tue Jun 23  17:24:55 BST 2009");

    std::string format("%a %b %d %T %Z %Y");

    prelert::core_t::TTime expected(1245774295);
    prelert::core_t::TTime actual(0);

    CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(format, dateTime, actual));
    CPPUNIT_ASSERT_EQUAL(expected, actual);
    LOG_DEBUG(actual);
}
{
    // This time is in summer, but explicitly specifies a GMT offset of 0,
    // so we should get 1245777895 instead of 1245774295
    std::string dateTime("Tue Jun 23  17:24:55 2009 +0000");

    std::string format("%a %b %d %T %Y %z");

    prelert::core_t::TTime expected(1245777895);
    prelert::core_t::TTime actual(0);

    CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(format, dateTime, actual));
    CPPUNIT_ASSERT_EQUAL(expected, actual);
    LOG_DEBUG(actual);

    std::string badDateTime1("Tue Jun 23  17:24:55 2009");
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::strptime(format, badDateTime1, actual));

    std::string badDateTime2("Tue Jun 23  17:24:55 2009 0000");
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::strptime(format, badDateTime2, actual));
}
{
    // Test what happens when no year is given
    std::string dateTime("Jun 23  17:24:55");

    std::string format("%b %d %T");

    prelert::core_t::TTime actual(0);

    CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(format, dateTime, actual));
    LOG_DEBUG(actual);

    // This test is only approximate (assuming leap year with leap second), so
    // print a warning too
    CPPUNIT_ASSERT(actual >= prelert::core::CTimeUtils::now() - 366 * 24 * 60 * 60 - 1);
    char buf[128] = { '\0' };
    LOG_WARN("If the following date is not within the last year then something is wrong: " <<
             prelert::core::CCTimeR::cTimeR(&actual, buf));

    // Allow small tolerance in case of clock discrepancies between machines
    CPPUNIT_ASSERT(actual <= prelert::core::CTimeUtils::now() + prelert::core::CTimeUtils::MAX_CLOCK_DISCREPANCY);
}
{
    // Test what happens when no year is given
    std::string dateTime("Jan 01  01:24:55");

    std::string format("%b %d %T");

    prelert::core_t::TTime actual(0);

    CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(format, dateTime, actual));
    LOG_DEBUG(actual);

    // This test is only approximate (assuming leap year with leap second), so
    // print a warning too
    CPPUNIT_ASSERT(actual >= prelert::core::CTimeUtils::now() - 366 * 24 * 60 * 60 - 1);
    char buf[128] = { '\0' };
    LOG_WARN("If the following date is not within the last year then something is wrong: " <<
             prelert::core::CCTimeR::cTimeR(&actual, buf));

    // Allow small tolerance in case of clock discrepancies between machines
    CPPUNIT_ASSERT(actual <= prelert::core::CTimeUtils::now() + prelert::core::CTimeUtils::MAX_CLOCK_DISCREPANCY);
}
{
    // Test what happens when no year is given
    std::string dateTime("Dec 31  23:24:55");

    std::string format("%b %d %T");

    prelert::core_t::TTime actual(0);

    CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(format, dateTime, actual));
    LOG_DEBUG(actual);

    // This test is only approximate (assuming leap year with leap second), so
    // print a warning too
    CPPUNIT_ASSERT(actual >= prelert::core::CTimeUtils::now() - 366 * 24 * 60 * 60 - 1);
    char buf[128] = { '\0' };
    LOG_WARN("If the following date is not within the last year then something is wrong: " <<
             prelert::core::CCTimeR::cTimeR(&actual, buf));

    // Allow small tolerance in case of clock discrepancies between machines
    CPPUNIT_ASSERT(actual <= prelert::core::CTimeUtils::now() + prelert::core::CTimeUtils::MAX_CLOCK_DISCREPANCY);
}


}

void CTimeUtilsTest::testTimezone(void)
{
    static const prelert::core_t::TTime SECONDS_PER_HOUR = 3600;

    // These convert the same date/time to a Unix time, but in a variety of
    // different timezones.  Since Unix times represent seconds since the epoch
    // UTC, the timezone will change the results.

    std::string format("%Y-%m-%d %H:%M:%S");
    std::string dateTime("2008-11-26 14:40:37");

    // Additionally, for each timezone, we'll try converting the same time,
    // but with UTC explicitly specified.  This should always come up with
    // the utcExpected time.  Also, to exercise the time convertor, we'll
    // explicitly specify 2 hours behind GMT (although it's unlikely this
    // would ever occur in a real log file).

    std::string formatExplicit("%Y-%m-%d %H:%M:%S %z");

    std::string dateTimeUtc("2008-11-26 14:40:37 +0000");
    prelert::core_t::TTime utcExpected(1227710437);

    std::string dateTimeTwoHoursBehindUtc("2008-11-26 14:40:37 -0200");
    prelert::core_t::TTime twoHoursBehindUtc(utcExpected + 2 * SECONDS_PER_HOUR);

    // UK first
    CPPUNIT_ASSERT(prelert::core::CTimezone::setTimezone("Europe/London"));
    {
        prelert::core_t::TTime expected(utcExpected);
        prelert::core_t::TTime actual(0);

        CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(format, dateTime, actual));
        CPPUNIT_ASSERT_EQUAL(expected, actual);

        CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(formatExplicit, dateTimeUtc, actual));
        CPPUNIT_ASSERT_EQUAL(utcExpected, actual);

        CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(formatExplicit, dateTimeTwoHoursBehindUtc, actual));
        CPPUNIT_ASSERT_EQUAL(twoHoursBehindUtc, actual);
    }

    // US eastern time: 5 hours behind the UK (except during daylight saving
    // time switchover)
    CPPUNIT_ASSERT(prelert::core::CTimezone::setTimezone("America/New_York"));
    {
        // The Unix time is in UTC, and UTC will be 5 hours ahead of US eastern
        // time at this time of the year (UTC is only 4 hours ahead in summer).
        prelert::core_t::TTime expected(utcExpected + 5 * SECONDS_PER_HOUR);
        prelert::core_t::TTime actual(0);

        CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(format, dateTime, actual));
        CPPUNIT_ASSERT_EQUAL(expected, actual);

        CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(formatExplicit, dateTimeUtc, actual));
        CPPUNIT_ASSERT_EQUAL(utcExpected, actual);

        CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(formatExplicit, dateTimeTwoHoursBehindUtc, actual));
        CPPUNIT_ASSERT_EQUAL(twoHoursBehindUtc, actual);
    }

    // US Pacific time: 8 hours behind the UK (except during daylight saving
    // time switchover)
    CPPUNIT_ASSERT(prelert::core::CTimezone::setTimezone("America/Los_Angeles"));
    {
        prelert::core_t::TTime expected(utcExpected + 8 * SECONDS_PER_HOUR);
        prelert::core_t::TTime actual(0);

        CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(format, dateTime, actual));
        CPPUNIT_ASSERT_EQUAL(expected, actual);

        CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(formatExplicit, dateTimeUtc, actual));
        CPPUNIT_ASSERT_EQUAL(utcExpected, actual);

        CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(formatExplicit, dateTimeTwoHoursBehindUtc, actual));
        CPPUNIT_ASSERT_EQUAL(twoHoursBehindUtc, actual);
    }

    // Australian central time: 9.5 hours ahead of GMT all year around in the
    // Northern Territory; in South Australia, 9.5 hours ahead of GMT in the
    // (southern hemisphere) winter and 10.5 hours ahead of GMT in the (southern
    // hemisphere) summer.

    // Northern Territory first
    CPPUNIT_ASSERT(prelert::core::CTimezone::setTimezone("Australia/Darwin"));
    {
        prelert::core_t::TTime expected(utcExpected - static_cast<prelert::core_t::TTime>(9.5 * SECONDS_PER_HOUR));
        prelert::core_t::TTime actual(0);

        CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(format, dateTime, actual));
        CPPUNIT_ASSERT_EQUAL(expected, actual);

        CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(formatExplicit, dateTimeUtc, actual));
        CPPUNIT_ASSERT_EQUAL(utcExpected, actual);

        CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(formatExplicit, dateTimeTwoHoursBehindUtc, actual));
        CPPUNIT_ASSERT_EQUAL(twoHoursBehindUtc, actual);
    }

    // Now South Australia - remember, 26th November is summer in Australia,
    // so daylight saving is in force
    CPPUNIT_ASSERT(prelert::core::CTimezone::setTimezone("Australia/Adelaide"));
    {
        prelert::core_t::TTime expected(utcExpected - static_cast<prelert::core_t::TTime>(10.5 * SECONDS_PER_HOUR));
        prelert::core_t::TTime actual(0);

        CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(format, dateTime, actual));
        CPPUNIT_ASSERT_EQUAL(expected, actual);

        CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(formatExplicit, dateTimeUtc, actual));
        CPPUNIT_ASSERT_EQUAL(utcExpected, actual);

        CPPUNIT_ASSERT(prelert::core::CTimeUtils::strptime(formatExplicit, dateTimeTwoHoursBehindUtc, actual));
        CPPUNIT_ASSERT_EQUAL(twoHoursBehindUtc, actual);
    }

    // Set the timezone back to nothing, i.e. let the operating system decide
    // what to use
    CPPUNIT_ASSERT(prelert::core::CTimezone::setTimezone(""));
}

void CTimeUtilsTest::testDateWords(void)
{
    // These tests assume they're being run in an English speaking country

    LOG_DEBUG("Checking day of week abbreviations");
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Mon"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Tue"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Wed"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Thu"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Fri"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Sat"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Sun"));

    LOG_DEBUG("Checking full days of week");
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Monday"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Tuesday"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Wednesday"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Thursday"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Friday"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Saturday"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Sunday"));

    LOG_DEBUG("Checking non-days of week");
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Money"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Tues"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Wedding"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Thug"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Fried"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Satanic"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Sunburn"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Prelert"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Dave"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Hello"));

    LOG_DEBUG("Checking month abbreviations");
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Jan"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Feb"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Mar"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Apr"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("May"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Jun"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Jul"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Aug"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Sep"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Oct"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Nov"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("Dec"));

    LOG_DEBUG("Checking full months");
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("January"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("February"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("March"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("April"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("May"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("June"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("July"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("August"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("September"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("October"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("November"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("December"));

    LOG_DEBUG("Checking non-months");
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Jane"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Febrile"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Market"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Apricot"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Maybe"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Junk"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Juliet"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Augment"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Separator"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Octet"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Novel"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Decadent"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Table"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Chair"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("Laptop"));

    LOG_DEBUG("Checking time zones");
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("GMT"));
    CPPUNIT_ASSERT(prelert::core::CTimeUtils::isDateWord("UTC"));

    LOG_DEBUG("Checking space");
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord(""));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord(" "));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord("\t"));
    CPPUNIT_ASSERT(!prelert::core::CTimeUtils::isDateWord(" \t"));
}

