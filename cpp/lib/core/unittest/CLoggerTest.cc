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
#include "CLoggerTest.h"

#include <core/CLogger.h>

#include <ios>
#include <stdexcept>


CppUnit::Test *CLoggerTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CLoggerTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CLoggerTest>(
                                   "CLoggerTest::testLogging",
                                   &CLoggerTest::testLogging) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CLoggerTest>(
                                   "CLoggerTest::testReconfiguration",
                                   &CLoggerTest::testReconfiguration) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CLoggerTest>(
                                   "CLoggerTest::testSetLevel",
                                   &CLoggerTest::testSetLevel) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CLoggerTest>(
                                   "CLoggerTest::testLogEnvironment",
                                   &CLoggerTest::testLogEnvironment) );

    return suiteOfTests;
}

void CLoggerTest::testLogging(void)
{
    std::string t("Test message");

    LOG_TRACE("Trace");
    LOG_AT_LEVEL("TRACE", "Dynamic TRACE " << 1);
    LOG_DEBUG("Debug");
    LOG_AT_LEVEL("DEBUG", "Dynamic DEBUG " << 2.0);
    LOG_INFO("Info " << std::boolalpha << true);
    LOG_AT_LEVEL("INFO", "Dynamic INFO " << false);
    LOG_WARN("Warn " << t);
    LOG_AT_LEVEL("WARN", "Dynamic WARN " << "abc");
    LOG_ERROR("Error " << 1000 << ' ' << 0.23124F);
    LOG_AT_LEVEL("ERROR", "Dynamic ERROR");
    LOG_FATAL("Fatal - application to handle exit");
    LOG_AT_LEVEL("FATAL", "Dynamic FATAL " << t);
    try
    {
        LOG_ABORT("Throwing exception " << 1221U << ' ' << 0.23124);

        CPPUNIT_ASSERT(false);
    }
    catch (std::runtime_error &)
    {
        CPPUNIT_ASSERT(true);
    }
}

void CLoggerTest::testReconfiguration(void)
{
    ml::core::CLogger &logger = ml::core::CLogger::instance();

    LOG_DEBUG("Starting logger reconfiguration test");

    LOG_TRACE("This shouldn't be seen because the hardcoded default log level is DEBUG");
    CPPUNIT_ASSERT(!logger.hasBeenReconfigured());

    CPPUNIT_ASSERT(!logger.reconfigureFromFile("nonexistantfile"));

    CPPUNIT_ASSERT(logger.reconfigureLogJson());
    LOG_INFO("This should be logged as JSON!");

    // The test log4cxx.properties is very similar to the hardcoded default, but
    // with the level set to TRACE rather than DEBUG
    CPPUNIT_ASSERT(logger.reconfigureFromFile("testfiles/log4cxx.properties"));

    LOG_TRACE("This should be seen because the reconfigured log level is TRACE");
    CPPUNIT_ASSERT(logger.hasBeenReconfigured());
}

void CLoggerTest::testSetLevel(void)
{
    ml::core::CLogger &logger = ml::core::CLogger::instance();

    LOG_DEBUG("Starting logger level test");

    CPPUNIT_ASSERT(logger.setLoggingLevel(ml::core::CLogger::E_Error));

    LOG_TRACE("SHOULD NOT BE SEEN");
    LOG_DEBUG("SHOULD NOT BE SEEN");
    LOG_INFO("SHOULD NOT BE SEEN");
    LOG_WARN("SHOULD NOT BE SEEN");
    LOG_ERROR("Should be seen");
    LOG_FATAL("Should be seen");

    CPPUNIT_ASSERT(logger.setLoggingLevel(ml::core::CLogger::E_Info));

    LOG_TRACE("SHOULD NOT BE SEEN");
    LOG_DEBUG("SHOULD NOT BE SEEN");
    LOG_INFO("Should be seen");
    LOG_WARN("Should be seen");
    LOG_ERROR("Should be seen");
    LOG_FATAL("Should be seen");

    CPPUNIT_ASSERT(logger.setLoggingLevel(ml::core::CLogger::E_Trace));

    LOG_TRACE("Should be seen");
    LOG_DEBUG("Should be seen");
    LOG_INFO("Should be seen");
    LOG_WARN("Should be seen");
    LOG_ERROR("Should be seen");
    LOG_FATAL("Should be seen");

    CPPUNIT_ASSERT(logger.setLoggingLevel(ml::core::CLogger::E_Warn));

    LOG_TRACE("SHOULD NOT BE SEEN");
    LOG_DEBUG("SHOULD NOT BE SEEN");
    LOG_INFO("SHOULD NOT BE SEEN");
    LOG_WARN("Should be seen");
    LOG_ERROR("Should be seen");
    LOG_FATAL("Should be seen");

    CPPUNIT_ASSERT(logger.setLoggingLevel(ml::core::CLogger::E_Fatal));

    LOG_TRACE("SHOULD NOT BE SEEN");
    LOG_DEBUG("SHOULD NOT BE SEEN");
    LOG_INFO("SHOULD NOT BE SEEN");
    LOG_WARN("SHOULD NOT BE SEEN");
    LOG_ERROR("SHOULD NOT BE SEEN");
    LOG_FATAL("Should be seen");

    CPPUNIT_ASSERT(logger.setLoggingLevel(ml::core::CLogger::E_Debug));

    LOG_DEBUG("Finished logger level test");
}

void CLoggerTest::testLogEnvironment(void)
{
    ml::core::CLogger::instance().logEnvironment();
}

