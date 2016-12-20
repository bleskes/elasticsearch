#
# ELASTICSEARCH CONFIDENTIAL
#
# Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
#
# Notice: this software, and all information contained
# therein, is the exclusive property of Elasticsearch BV
# and its licensors, if any, and is protected under applicable
# domestic and foreign law, and international treaties.
#
# Reproduction, republication or distribution without the
# express written consent of Elasticsearch BV is
# strictly prohibited.
#
include $(CPP_SRC_HOME)/mk/defines.mk

TARGET=pre_test$(EXE_EXT)

USE_BOOST=1
USE_BOOST_FILESYSTEM_LIBS=1
USE_NET=1
USE_XML=1

LIBS:=$(LIB_PRE_MATHS) $(LIB_PRE_MODEL) $(LIB_PRE_API)

all: build

SRCS=\
	Main.cc \
	CAnomalyDetectorLimitTest.cc \
	CAnomalyDetectorTest.cc \
	CBackgroundPersisterTest.cc \
	CBaseTokenListDataTyperTest.cc \
	CBoxPlotDataJsonWriterTest.cc \
	CCategoryExamplesCollectorTest.cc \
	CConfigUpdaterTest.cc \
	CCsvInputParserTest.cc \
	CCsvOutputWriterTest.cc \
	CDetectionRulesJsonParserTest.cc \
	CFieldConfigTest.cc \
	CFieldDataTyperTest.cc \
	CIoManagerTest.cc \
	CJsonOutputWriterTest.cc \
	CLengthEncodedInputParserTest.cc \
	CLineifiedJsonInputParserTest.cc \
	CLineifiedJsonOutputWriterTest.cc \
	CLineifiedXmlInputParserTest.cc \
	CMockDataAdder.cc \
	CMockDataProcessor.cc \
	CMockSearcher.cc \
	CMultiFileDataAdderTest.cc \
	COutputChainerTest.cc \
	CResultNormalizerTest.cc \
	CSingleStreamDataAdderTest.cc \
	CStringStoreTest.cc \
	CTermQuoterTest.cc \
	CTokenListDataTyperTest.cc \
	CTokenListReverseSearchCreatorTest.cc \


include $(CPP_SRC_HOME)/mk/stdcppunit.mk

