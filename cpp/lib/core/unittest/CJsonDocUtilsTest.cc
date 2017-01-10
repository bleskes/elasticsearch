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
#include "CJsonDocUtilsTest.h"

#include <core/CJsonDocUtils.h>
#include <core/CLogger.h>
#include <core/CStringUtils.h>

#include <rapidjson/document.h>
#include <rapidjson/linewriter.h>
#include <rapidjson/GenericWriteStream.h>

#include <limits>
#include <sstream>


CppUnit::Test *CJsonDocUtilsTest::suite()
{
    CppUnit::TestSuite *suiteOfTests = new CppUnit::TestSuite("CJsonDocUtilsTest");

    suiteOfTests->addTest( new CppUnit::TestCaller<CJsonDocUtilsTest>(
                                   "CJsonDocUtilsTest::testAddFields",
                                   &CJsonDocUtilsTest::testAddFields) );
    suiteOfTests->addTest( new CppUnit::TestCaller<CJsonDocUtilsTest>(
                                   "CJsonDocUtilsTest::testRemoveMemberIfPresent",
                                   &CJsonDocUtilsTest::testRemoveMemberIfPresent) );

    return suiteOfTests;
}

namespace
{
const std::string STR_NAME("str");
const std::string EMPTY1_NAME("empty1");
const std::string EMPTY2_NAME("empty2");
const std::string DOUBLE_NAME("double");
const std::string NAN_NAME("nan");
const std::string INFINITY_NAME("infinity");
const std::string BOOL_NAME("bool");
const std::string INT_NAME("int");
const std::string UINT_NAME("uint");
const std::string STR_ARRAY_NAME("str[]");
const std::string DOUBLE_ARRAY_NAME("double[]");
const std::string NAN_ARRAY_NAME("nan[]");
const std::string TTIME_ARRAY_NAME("TTime[]");
}

void CJsonDocUtilsTest::testAddFields(void)
{
    static const size_t FIXED_BUFFER_SIZE = 4096;
    char fixedBuffer[FIXED_BUFFER_SIZE];
    rapidjson::MemoryPoolAllocator<> allocator(fixedBuffer, FIXED_BUFFER_SIZE);

    rapidjson::Document doc;
    doc.SetObject();

    ml::core::CJsonDocUtils::addStringFieldToObj(STR_NAME,
                                                      "hello",
                                                      doc,
                                                      allocator);

    ml::core::CJsonDocUtils::addStringFieldToObj(EMPTY1_NAME,
                                                      "",
                                                      doc,
                                                      allocator);

    ml::core::CJsonDocUtils::addStringFieldToObj(EMPTY2_NAME,
                                                      "",
                                                      doc,
                                                      allocator,
                                                      true);

    ml::core::CJsonDocUtils::addDoubleFieldToObj(DOUBLE_NAME,
                                                      1.77e-156,
                                                      doc,
                                                      allocator);

    ml::core::CJsonDocUtils::addDoubleFieldToObj(NAN_NAME,
                                                      std::numeric_limits<double>::quiet_NaN(),
                                                      doc,
                                                      allocator);

    ml::core::CJsonDocUtils::addDoubleFieldToObj(INFINITY_NAME,
                                                      std::numeric_limits<double>::infinity(),
                                                      doc,
                                                      allocator);

    ml::core::CJsonDocUtils::addBoolFieldToObj(BOOL_NAME,
                                                    false,
                                                    doc,
                                                    allocator);

    ml::core::CJsonDocUtils::addIntFieldToObj(INT_NAME,
                                                   -9,
                                                   doc,
                                                   allocator);

    ml::core::CJsonDocUtils::addUIntFieldToObj(UINT_NAME,
                                                    999999999999999ull,
                                                    doc,
                                                    allocator);

    ml::core::CJsonDocUtils::addStringArrayFieldToObj(STR_ARRAY_NAME,
                                                           ml::core::CJsonDocUtils::TStrVec(3, "blah"),
                                                           doc,
                                                           allocator);

    ml::core::CJsonDocUtils::addDoubleArrayFieldToObj(DOUBLE_ARRAY_NAME,
                                                           ml::core::CJsonDocUtils::TDoubleVec(10, 1.5),
                                                           doc,
                                                           allocator);

    ml::core::CJsonDocUtils::addDoubleArrayFieldToObj(NAN_ARRAY_NAME,
                                                           ml::core::CJsonDocUtils::TDoubleVec(2, std::numeric_limits<double>::quiet_NaN()),
                                                           doc,
                                                           allocator);

    ml::core::CJsonDocUtils::addTimeArrayFieldToObj(TTIME_ARRAY_NAME,
                                                         ml::core::CJsonDocUtils::TTimeVec(2, 1421421421),
                                                         doc,
                                                         allocator);

    std::ostringstream strm;
    rapidjson::GenericWriteStream writeStream(strm);
    typedef rapidjson::LineWriter<rapidjson::GenericWriteStream> TGenericLineWriter;
    TGenericLineWriter writer(writeStream);
    doc.Accept(writer);

    std::string printedDoc(strm.str());
    ml::core::CStringUtils::trimWhitespace(printedDoc);

    LOG_DEBUG("Printed doc is: " << printedDoc);

    std::string expectedDoc("{"
                                "\"str\":\"hello\","
                                "\"empty2\":\"\","
                                "\"double\":1.77e-156,"
                                "\"nan\":0,"
                                "\"infinity\":0,"
                                "\"bool\":false,"
                                "\"int\":-9,"
                                "\"uint\":999999999999999,"
                                "\"str[]\":[\"blah\",\"blah\",\"blah\"],"
                                "\"double[]\":[1.5,1.5,1.5,1.5,1.5,1.5,1.5,1.5,1.5,1.5],"
                                "\"nan[]\":[0,0],"
                                "\"TTime[]\":[1421421421,1421421421]"
                            "}");

    CPPUNIT_ASSERT_EQUAL(expectedDoc, printedDoc);
}

void CJsonDocUtilsTest::testRemoveMemberIfPresent(void)
{
    rapidjson::Document doc;
    doc.SetObject();
    rapidjson::Document::AllocatorType &allocator = doc.GetAllocator();
    std::string foo("foo");

    ml::core::CJsonDocUtils::addStringFieldToObj(foo, "42", doc, allocator);
    CPPUNIT_ASSERT(doc.HasMember(foo.c_str()));

    ml::core::CJsonDocUtils::removeMemberIfPresent(foo, doc);
    CPPUNIT_ASSERT(doc.HasMember(foo.c_str()) == false);

    ml::core::CJsonDocUtils::removeMemberIfPresent(foo, doc);
    CPPUNIT_ASSERT(doc.HasMember(foo.c_str()) == false);
}
