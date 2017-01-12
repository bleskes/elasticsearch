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

TARGET=$(OBJS_DIR)/libMlApi$(DYNAMIC_LIB_EXT)

USE_BOOST=1
USE_BOOST_FILESYSTEM_LIBS=1
USE_NET=1

LIBS:=$(LIB_ML_CORE) $(LIB_ML_MATHS) $(LIB_ML_MODEL)

all: build

SRCS= \
CAnomalyDetector.cc \
CAnomalyDetectorUtils.cc \
CBackgroundPersister.cc \
CBaseTokenListDataTyper.cc \
CBenchMarker.cc \
CBoxPlotDataJsonWriter.cc \
CCmdSkeleton.cc \
CCategoryExamplesCollector.cc \
CConfigUpdater.cc \
CCsvInputParser.cc \
CCsvOutputWriter.cc \
CDataProcessor.cc \
CDataTyper.cc \
CDetectionRulesJsonParser.cc \
CFieldConfig.cc \
CFieldDataTyper.cc \
CHierarchicalResultsWriter.cc \
CInputParser.cc \
CIoManager.cc \
CJsonOutputWriter.cc \
CLengthEncodedInputParser.cc \
CLineifiedInputParser.cc \
CLineifiedJsonInputParser.cc \
CLineifiedJsonOutputWriter.cc \
CLineifiedXmlInputParser.cc \
CLineifiedXmlOutputWriter.cc \
CModelVisualisationJsonWriter.cc \
CMultiFileDataAdder.cc \
CMultiFileSearcher.cc \
CTokenListReverseSearchCreator.cc \
COutputChainer.cc \
COutputHandler.cc \
CResultNormalizer.cc \
CSingleStreamDataAdder.cc \
CSingleStreamSearcher.cc \
CTermQuoter.cc \
CTokenListReverseSearchCreatorIntf.cc \
CTokenListType.cc \

NO_BENCH_MARKS=1

include $(CPP_SRC_HOME)/mk/dynamiclib.mk

