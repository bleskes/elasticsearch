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
#include "CTokenListDataTyperTest.h"

#include <core/CLogger.h>
#include <core/CRapidXmlParser.h>
#include <core/CRapidXmlStatePersistInserter.h>
#include <core/CRapidXmlStateRestoreTraverser.h>
#include <core/CWordDictionary.h>

#include <api/CTokenListDataTyper.h>


namespace
{

typedef prelert::api::CTokenListDataTyper<true,  // Warping
                                          true,  // Underscores
                                          true,  // Dots
                                          true,  // Dashes
                                          true,  // Ignore leading digit
                                          true,  // Ignore hex
                                          true,  // Ignore date words
                                          false, // Ignore field names
                                          2,     // Min dictionary word length
                                          prelert::core::CWordDictionary::TWeightVerbs5Other2>
        TTokenListDataTyperKeepsFields;

const TTokenListDataTyperKeepsFields::TTokenListReverseSearchCreatorIntfCPtr NO_REVERSE_SEARCH_CREATOR;

}

CppUnit::Test *CTokenListDataTyperTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CTokenListDataTyperTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CTokenListDataTyperTest>(
                                   "CTokenListDataTyperTest::testHexData",
                                   &CTokenListDataTyperTest::testHexData) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTokenListDataTyperTest>(
                                   "CTokenListDataTyperTest::testRmdsData",
                                   &CTokenListDataTyperTest::testRmdsData) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTokenListDataTyperTest>(
                                   "CTokenListDataTyperTest::testTele2Data",
                                   &CTokenListDataTyperTest::testTele2Data) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTokenListDataTyperTest>(
                                   "CTokenListDataTyperTest::testNtrsData",
                                   &CTokenListDataTyperTest::testNtrsData) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTokenListDataTyperTest>(
                                   "CTokenListDataTyperTest::testSavvisData",
                                   &CTokenListDataTyperTest::testSavvisData) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTokenListDataTyperTest>(
                                   "CTokenListDataTyperTest::testFidelityData",
                                   &CTokenListDataTyperTest::testFidelityData) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTokenListDataTyperTest>(
                                   "CTokenListDataTyperTest::testVmwareData",
                                   &CTokenListDataTyperTest::testVmwareData) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTokenListDataTyperTest>(
                                   "CTokenListDataTyperTest::testWellsFargoData",
                                   &CTokenListDataTyperTest::testWellsFargoData) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTokenListDataTyperTest>(
                                   "CTokenListDataTyperTest::testSolarwindsData",
                                   &CTokenListDataTyperTest::testSolarwindsData) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CTokenListDataTyperTest>(
                                   "CTokenListDataTyperTest::testPersist",
                                   &CTokenListDataTyperTest::testPersist) );

    return suiteOfTests;
}

void CTokenListDataTyperTest::setUp(void)
{
    // Enable trace level logging for these unit tests
    prelert::core::CLogger::instance().setLoggingLevel(prelert::core::CLogger::E_Trace);
}

void CTokenListDataTyperTest::tearDown(void)
{
    // Revert to debug level logging for any subsequent unit tests
    prelert::core::CLogger::instance().setLoggingLevel(prelert::core::CLogger::E_Debug);
}

void CTokenListDataTyperTest::testHexData(void)
{
    TTokenListDataTyperKeepsFields typer(NO_REVERSE_SEARCH_CREATOR, 0.7);

    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "[0x0000000800000000 ", 500));
    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "0x0000000800000000", 500));
    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, " 0x0000000800000000,", 500));
    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "0x0000000800000000)", 500));
    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, " 0x0000000800000000,", 500));
}

void CTokenListDataTyperTest::testRmdsData(void)
{
    TTokenListDataTyperKeepsFields typer(NO_REVERSE_SEARCH_CREATOR, 0.7);

    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "<sol13m-8608.1.p2ps: Info: > Source AES_SERVICE2 on 33122:967 has shut down.", 500));
    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "<sol13m-8602.1.p2ps: Info: > Source EUROBROKER on 33112:836 has shut down.", 500));
    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "<lnl13m-8606.1.p2ps: Info: > Source PRISM_LIQUIDNET on 33188:1010 has shut down.", 500));
    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "<sol13m-8608.1.p2ps: Info: > Source AES SERVICE2 on 33122:967 has shut down.", 500));
    CPPUNIT_ASSERT_EQUAL(2, typer.computeType(false, "<sol13m-8602.1.p2ps: Info: > Source EUROBROKER on 33112:836 has started.", 500));
    CPPUNIT_ASSERT_EQUAL(2, typer.computeType(false, "<sol13m-8608.1.p2ps: Info: > Source AES_SERVICE2 on 33122:967 has started.", 500));
    CPPUNIT_ASSERT_EQUAL(3, typer.computeType(false, "<lnl00m-8201.1.p2ps: Info: > Service PRISM_CHIX, id of 732, has started.", 500));
    CPPUNIT_ASSERT_EQUAL(3, typer.computeType(false, "<sol00m-8601.1.p2ps: Info: > Service PRISM_IDEM, id of 632, has started.", 500));
    CPPUNIT_ASSERT_EQUAL(3, typer.computeType(false, "<sol00m-8601.1.p2ps: Info: > Service PRISM_IDEM, id of 632, has started.", 500));
    CPPUNIT_ASSERT_EQUAL(4, typer.computeType(false, "<lnl00m-8201.1.p2ps: Info: > Service PRISM_CHIX has shut down.", 500));
}

void CTokenListDataTyperTest::testTele2Data(void)
{
    TTokenListDataTyperKeepsFields typer(NO_REVERSE_SEARCH_CREATOR, 0.7);

    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, " [1094662464] INFO  transaction <3c26701d3140-kn8n1c8f5d2o> - Transaction TID: z9hG4bKy6aEy6aEy6aEaUgi!UmU-Ma.9-6bf50ea062.218.251.8SUBSCRIBE deleted", 500));
    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, " [1091504448] INFO  transaction <3c26701ad775-1cref2zy3w9e> - Transaction TID: z9hG4bK_UQA_UQA_UQAsO0i!OG!yYK.25-5bee09e062.218.251.8SUBSCRIBE deleted", 500));
    CPPUNIT_ASSERT_EQUAL(2, typer.computeType(false, " [1094662464] INFO  transactionuser <6508700927200972648@10.10.18.82> - ---------------- DESTROYING RegistrationServer ---------------", 500));
    CPPUNIT_ASSERT_EQUAL(3, typer.computeType(false, " [1111529792] INFO  proxy <45409105041220090733@62.218.251.123> - +++++++++++++++ CREATING ProxyCore ++++++++++++++++", 500));
    CPPUNIT_ASSERT_EQUAL(4, typer.computeType(false, " [1091504448] INFO  transactionuser <3c26709ab9f0-iih26eh8pxxa> - +++++++++++++++ CREATING PresenceAgent ++++++++++++++++", 500));
    CPPUNIT_ASSERT_EQUAL(5, typer.computeType(false, " [1111529792] INFO  session <45409105041220090733@62.218.251.123> - ----------------- PROXY Session DESTROYED --------------------", 500));
    CPPUNIT_ASSERT_EQUAL(5, typer.computeType(false, " [1094662464] INFO  session <ch6z1bho8xeprb3z4ty604iktl6c@alex.utafon.at> - ----------------- PROXY Session DESTROYED --------------------", 500));
}

void CTokenListDataTyperTest::testNtrsData(void)
{
    TTokenListDataTyperKeepsFields typer(NO_REVERSE_SEARCH_CREATOR, 0.7);

    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "<L_MSG MN=\"up02220\" PID=\"w010_managed4\" TID=\"asyncDelivery41\" DT=\"\" PT=\"ERROR\" AP=\"wts\" DN=\"\" SN=\"\" SR=\"com.ntrs.wts.session.ejb.FxCoverSessionBean\">javax.ejb.FinderException - findFxCover([]): null</L_MSG>", 500));
    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "<L_MSG MN=\"up02213\" PID=\"w010_managed2\" TID=\"asyncDelivery44\" DT=\"\" PT=\"ERROR\" AP=\"wts\" DN=\"\" SN=\"\" SR=\"com.ntrs.wts.session.ejb.FxCoverSessionBean\">javax.ejb.FinderException - findFxCover([]): null</L_MSG>", 500));
}

void CTokenListDataTyperTest::testSavvisData(void)
{
    TTokenListDataTyperKeepsFields typer(NO_REVERSE_SEARCH_CREATOR, 0.7);

    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, " org.apache.coyote.http11.Http11BaseProtocol destroy", 500));
    CPPUNIT_ASSERT_EQUAL(2, typer.computeType(false, " org.apache.coyote.http11.Http11BaseProtocol init", 500));
    CPPUNIT_ASSERT_EQUAL(3, typer.computeType(false, " org.apache.coyote.http11.Http11BaseProtocol start", 500));
    CPPUNIT_ASSERT_EQUAL(4, typer.computeType(false, " org.apache.coyote.http11.Http11BaseProtocol stop", 500));
}

void CTokenListDataTyperTest::testFidelityData(void)
{
    TTokenListDataTyperKeepsFields typer(NO_REVERSE_SEARCH_CREATOR, 0.7);

    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "AUDIT  ; tomcat-http--16; ee96c0c4567c0c11d6b90f9bc8b54aaa77; REQ4e42023e0a0328d020003e460005aa33; oltxapplnx311.fmr.com; ; Request Complete: /ftgw/fbc/ofsummary/summary [T=283ms,EDBCUSTPREF-EDB_WEB_ACCOUNT_PREFERENCES=95,MAUI-ETSPROF2=155,NBMSG-NB_MESSAGING_SERVICE=164,CustAcctProfile=BRK=2;NB=0;FILI=0;CESG=0;CC=0;AcctTotal=2,migrated=2]", 500));
    CPPUNIT_ASSERT_EQUAL(2, typer.computeType(false, "AUDIT  ; tomcat-http--39; ee763e95747c0b11d6b90f9bc8b54aaa77; REQ4e42023e0a0429a020000c6f0002aa33; oltxapplnx411.fmr.com; ; Request Complete: /ftgw/fbc/ofaccounts/brokerageAccountHistory [T=414ms,EDBCUSTPREF-EDB_INS_PERSON_WEB_ACCT_PREFERENCES=298,MAUI-PSL04XD=108]", 500));
    CPPUNIT_ASSERT_EQUAL(3, typer.computeType(false, "AUDIT  ; tomcat-http--39; ee256201da7c0c11d6b90f9bc8b54aaa77; REQ4e42023b0a022925200027180002aa33; oltxapplnx211.fmr.com; ; Request Complete: /ftgw/fbc/ofpositions/brokerageAccountPositionsIframe [T=90ms,CacheStore-GetAttribute=5,MAUI-ECAPPOS=50,RR-QUOTE_TRANSACTION=11]", 500));
}

void CTokenListDataTyperTest::testVmwareData(void)
{
    TTokenListDataTyperKeepsFields typer(NO_REVERSE_SEARCH_CREATOR, 0.7);

    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "Vpxa: [49EC0B90 verbose 'VpxaHalCnxHostagent' opID=WFU-ddeadb59] [WaitForUpdatesDone] Received callback", 103));
    CPPUNIT_ASSERT_EQUAL(2, typer.computeType(false, "Vpxa: [49EC0B90 verbose 'Default' opID=WFU-ddeadb59] [VpxaHalVmHostagent] 11: GuestInfo changed 'guest.disk", 107));
    CPPUNIT_ASSERT_EQUAL(3, typer.computeType(false, "Vpxa: [49EC0B90 verbose 'VpxaHalCnxHostagent' opID=WFU-ddeadb59] [WaitForUpdatesDone] Completed callback", 104));
    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "Vpxa: [49EC0B90 verbose 'VpxaHalCnxHostagent' opID=WFU-35689729] [WaitForUpdatesDone] Received callback", 103));
    CPPUNIT_ASSERT_EQUAL(2, typer.computeType(false, "Vpxa: [49EC0B90 verbose 'Default' opID=WFU-35689729] [VpxaHalVmHostagent] 15: GuestInfo changed 'guest.disk", 107));
    CPPUNIT_ASSERT_EQUAL(3, typer.computeType(false, "Vpxa: [49EC0B90 verbose 'VpxaHalCnxHostagent' opID=WFU-35689729] [WaitForUpdatesDone] Completed callback", 104));
}

void CTokenListDataTyperTest::testWellsFargoData(void)
{
    TTokenListDataTyperKeepsFields typer(NO_REVERSE_SEARCH_CREATOR, 0.7);

    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "INFO  [com.wf.fx.fxcore.settlement.synchronization.PaymentFlowProcessorImpl] Process payment flow for tradeId=10894728 and backOfficeId=2354474", 500));
    CPPUNIT_ASSERT_EQUAL(2, typer.computeType(false, "INFO  [com.wf.fx.fxcore.settlement.synchronization.PaymentFlowProcessorImpl] Synchronization of payment flow is complete for tradeId=10013186 and backOfficeId=965573", 500));

    // This is not great, but it's tricky when only 1 word differs from the
    // first type
    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "INFO  [com.wf.fx.fxcore.settlement.synchronization.PaymentFlowProcessorImpl] Synchronize payment flow for tradeId=10894721 and backOfficeId=2354469", 500));
}

void CTokenListDataTyperTest::testSolarwindsData(void)
{
    TTokenListDataTyperKeepsFields typer(NO_REVERSE_SEARCH_CREATOR, 0.7);

    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "2016-04-27T19:57:43.644-0700: 1922084.903: [GC", 46));
    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "2016-04-28T19:57:43.644-0700: 1922084.903: [GC", 46));
    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "2016-04-29T19:57:43.644-0700: 1922084.903: [GC", 46));
    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "2016-04-30T19:57:43.644-0700: 1922084.903: [GC", 46));
    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "2016-04-30T19:57:43.644-0700: 1922084.904: [GC", 46));
    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "2016-04-30T19:57:43.644-0700: 1922084.905: [GC", 46));
    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "2016-04-30T19:57:43.644-0700: 1922084.906: [GC", 46));
    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "2016-04-30T19:57:43.644-0700: 1922085.906: [GC", 46));
    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "2016-04-30T19:57:43.644-0700: 1922086.906: [GC", 46));
    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "2016-04-30T19:57:43.644-0700: 1922087.906: [GC", 46));
    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "2016-04-30T19:57:43.645-0700: 1922087.906: [GC", 46));
    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "2016-04-30T19:57:43.646-0700: 1922087.906: [GC", 46));
    CPPUNIT_ASSERT_EQUAL(1, typer.computeType(false, "2016-04-30T19:57:43.647-0700: 1922087.906: [GC", 46));

    CPPUNIT_ASSERT_EQUAL(2, typer.computeType(false, "PSYoungGen      total 2572800K, used 1759355K [0x0000000759500000, 0x0000000800000000, 0x0000000800000000)", 106));
    CPPUNIT_ASSERT_EQUAL(2, typer.computeType(false, "PSYoungGen      total 2572801K, used 1759355K [0x0000000759500000, 0x0000000800000000, 0x0000000800000000)", 106));
    CPPUNIT_ASSERT_EQUAL(2, typer.computeType(false, "PSYoungGen      total 2572802K, used 1759355K [0x0000000759500000, 0x0000000800000000, 0x0000000800000000)", 106));
    CPPUNIT_ASSERT_EQUAL(2, typer.computeType(false, "PSYoungGen      total 2572803K, used 1759355K [0x0000000759500000, 0x0000000800000000, 0x0000000800000000)", 106));
    CPPUNIT_ASSERT_EQUAL(2, typer.computeType(false, "PSYoungGen      total 2572803K, used 1759355K [0x0000000759600000, 0x0000000800000000, 0x0000000800000000)", 106));
    CPPUNIT_ASSERT_EQUAL(2, typer.computeType(false, "PSYoungGen      total 2572803K, used 1759355K [0x0000000759700000, 0x0000000800000000, 0x0000000800000000)", 106));
    CPPUNIT_ASSERT_EQUAL(2, typer.computeType(false, "PSYoungGen      total 2572803K, used 1759355K [0x0000000759800000, 0x0000000800000000, 0x0000000800000000)", 106));
}

void CTokenListDataTyperTest::testPersist(void)
{
    TTokenListDataTyperKeepsFields origTyper(NO_REVERSE_SEARCH_CREATOR, 0.7);

    origTyper.computeType(false, "<sol13m-8608.1.p2ps: Info: > Source AES_SERVICE2 on 33122:967 has shut down.", 500);
    origTyper.computeType(false, "<sol13m-8602.1.p2ps: Info: > Source EUROBROKER on 33112:836 has shut down.", 500);
    origTyper.computeType(false, "<lnl13m-8606.1.p2ps: Info: > Source PRISM_LIQUIDNET on 33188:1010 has shut down.", 500);
    origTyper.computeType(false, "<sol13m-8608.1.p2ps: Info: > Source AES SERVICE2 on 33122:967 has shut down.", 500);
    origTyper.computeType(false, "<sol13m-8602.1.p2ps: Info: > Source EUROBROKER on 33112:836 has started.", 500);
    origTyper.computeType(false, "<sol13m-8608.1.p2ps: Info: > Source AES_SERVICE2 on 33122:967 has started.", 500);
    origTyper.computeType(false, "<lnl00m-8201.1.p2ps: Info: > Service PRISM_CHIX, id of 732, has started.", 500);
    origTyper.computeType(false, "<sol00m-8601.1.p2ps: Info: > Service PRISM_IDEM, id of 632, has started.", 500);
    origTyper.computeType(false, "<sol00m-8601.1.p2ps: Info: > Service PRISM_IDEM, id of 632, has started.", 500);
    origTyper.computeType(false, "<lnl00m-8201.1.p2ps: Info: > Service PRISM_CHIX has shut down.", 500);

    std::string origXml;
    {
        prelert::core::CRapidXmlStatePersistInserter inserter("root");
        origTyper.acceptPersistInserter(inserter);
        inserter.toXml(origXml);
    }

    LOG_DEBUG("Typer XML representation:\n" << origXml);

    // Restore the XML into a new typer
    TTokenListDataTyperKeepsFields restoredTyper(NO_REVERSE_SEARCH_CREATOR, 0.7);
    {
        prelert::core::CRapidXmlParser parser;
        CPPUNIT_ASSERT(parser.parseStringIgnoreCdata(origXml));
        prelert::core::CRapidXmlStateRestoreTraverser traverser(parser);
        CPPUNIT_ASSERT(traverser.traverseSubLevel(boost::bind(&TTokenListDataTyperKeepsFields::acceptRestoreTraverser,
                                                              &restoredTyper,
                                                              _1)));
    }

    // The XML representation of the new typer should be the same as the original
    std::string newXml;
    {
        prelert::core::CRapidXmlStatePersistInserter inserter("root");
        restoredTyper.acceptPersistInserter(inserter);
        inserter.toXml(newXml);
    }
    CPPUNIT_ASSERT_EQUAL(origXml, newXml);
}

