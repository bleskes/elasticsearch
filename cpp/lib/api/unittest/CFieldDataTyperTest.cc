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

#include "CFieldDataTyperTest.h"

#include <core/CDataAdder.h>
#include <core/CDataSearcher.h>

#include <model/CLimits.h>

#include <api/CFieldConfig.h>
#include <api/CFieldDataTyper.h>
#include <api/CJsonOutputWriter.h>
#include <api/COutputHandler.h>

#include <sstream>


using namespace prelert;
using namespace api;

namespace
{

class CTestOutputHandler : public COutputHandler
{
    public:
        CTestOutputHandler(void) : COutputHandler(), m_NewStream(false),
            m_HandledSettings(false), m_Finalised(false), m_Records(0)
        {
        }

        virtual ~CTestOutputHandler(void)
        {
        }

        virtual void finalise(void)
        {
            m_Finalised = true;
        }

        bool hasFinalised(void) const
        {
            return m_Finalised;
        }

        virtual void newOutputStream(void)
        {
            m_NewStream = true;
        }

        bool isNewStream(void) const
        {
            return m_NewStream;
        }

        virtual bool settings(const TStrStrUMap &/*settings*/)
        {
            m_HandledSettings = true;
            return true;
        }

        bool handledSettings(void) const
        {
            return m_HandledSettings;
        }

        virtual bool fieldNames(const TStrVec &/*fieldNames*/,
                                const TStrVec &/*extraFieldNames*/)
        {
            return true;
        }

        virtual const TStrVec &fieldNames(void) const
        {
            return m_FieldNames;
        }

        virtual bool writeRow(bool /*isDryRun*/,
                              const TStrStrUMap &/*dataRowFields*/,
                              const TStrStrUMap &/*overrideDataRowFields*/)
        {
            m_Records++;
            return true;
        }

        uint64_t getNumRows(void) const
        {
            return m_Records;
        }

    private:
        TStrVec m_FieldNames;

        bool m_NewStream;

        bool m_HandledSettings;

        bool m_Finalised;

        uint64_t m_Records;
};

class CTestDataSearcher : public core::CDataSearcher
{
    public:
        CTestDataSearcher(const std::string &data)
            : m_Stream(new std::istringstream(data))
        {
        }

        virtual TIStreamP search(size_t /*currentDocNum*/, size_t /*limit*/)
        {
            return m_Stream;
        }

    private:
        TIStreamP m_Stream;
};

class CTestDataAdder : public core::CDataAdder
{
    public:
        CTestDataAdder(void)
            : m_Stream(new std::ostringstream)
        {
        }

        virtual TOStreamP addStreamed(const std::string &/*index*/,
                                      const std::string &/*sourceType*/,
                                      const std::string &/*key*/)
        {
            return m_Stream;
        }

        virtual bool streamComplete(TOStreamP &/*strm*/, bool /*force*/)
        {
            return true;
        }

        TOStreamP getStream(void)
        {
            return m_Stream;
        }

    private:
        TOStreamP m_Stream;
};

}

void CFieldDataTyperTest::testAll(void)
{
    model::CLimits limits;
    CFieldConfig config("count", "prelertcategory");
    CTestOutputHandler handler;
    CJsonOutputWriter writer("job");

    CFieldDataTyper typer("job", config, limits, handler, writer);
    CPPUNIT_ASSERT_EQUAL(false, handler.isNewStream());
    typer.newOutputStream();
    CPPUNIT_ASSERT_EQUAL(true, handler.isNewStream());

    CPPUNIT_ASSERT_EQUAL(false, handler.handledSettings());
    CFieldDataTyper::TStrStrUMap settings;
    typer.handleSettings(settings);
    CPPUNIT_ASSERT_EQUAL(true, handler.handledSettings());

    CPPUNIT_ASSERT_EQUAL(false, handler.hasFinalised());
    CPPUNIT_ASSERT_EQUAL(uint64_t(0), typer.numRecordsHandled());

    CFieldDataTyper::TStrVec fieldNames;
    fieldNames.push_back("_raw");
    fieldNames.push_back("two");

    CFieldDataTyper::TStrStrUMap dataRowFields;
    dataRowFields["_raw"] = "thing";
    dataRowFields["two"] = "other";

    CPPUNIT_ASSERT(typer.handleRecord(false, fieldNames, dataRowFields));

    CPPUNIT_ASSERT_EQUAL(uint64_t(1), typer.numRecordsHandled());
    CPPUNIT_ASSERT_EQUAL(typer.numRecordsHandled(), handler.getNumRows());

    // try a couple of erroneous cases
    dataRowFields.clear();
    CPPUNIT_ASSERT(typer.handleRecord(false, fieldNames, dataRowFields));

    dataRowFields["thing"] = "bling";
    dataRowFields["thang"] = "wing";
    CPPUNIT_ASSERT(typer.handleRecord(false, fieldNames, dataRowFields));

    dataRowFields["_raw"] = "";
    dataRowFields["thang"] = "wing";
    CPPUNIT_ASSERT(typer.handleRecord(false, fieldNames, dataRowFields));

    CPPUNIT_ASSERT_EQUAL(uint64_t(4), typer.numRecordsHandled());
    CPPUNIT_ASSERT_EQUAL(typer.numRecordsHandled(), handler.getNumRows());

    typer.finalise();
    CPPUNIT_ASSERT(handler.hasFinalised());

    // do a persist / restore
    std::string origJson;
    {
        CTestDataAdder adder;
        typer.persistState(adder);
        std::ostringstream &ss = dynamic_cast<std::ostringstream &>(*adder.getStream());
        origJson = ss.str();
    }

    std::string newJson;
    LOG_DEBUG("origJson = " << origJson);
    {
        model::CLimits limits2;
        CFieldConfig config2("x", "y");
        CTestOutputHandler handler2;
        CJsonOutputWriter writer2("job");

        CFieldDataTyper newTyper("job", config2, limits2, handler2, writer2);
        CTestDataSearcher restorer(origJson);
        core_t::TTime time = 0;
        newTyper.restoreState(restorer, time);

        CTestDataAdder adder;
        newTyper.persistState(adder);
        std::ostringstream &ss = dynamic_cast<std::ostringstream &>(*adder.getStream());
        newJson = ss.str();
    }
    CPPUNIT_ASSERT_EQUAL(origJson, newJson);

}


CppUnit::Test* CFieldDataTyperTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CFieldDataTyperTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CFieldDataTyperTest>(
                                   "CFieldDataTyperTest::testAll",
                                   &CFieldDataTyperTest::testAll) );
    return suiteOfTests;
}

