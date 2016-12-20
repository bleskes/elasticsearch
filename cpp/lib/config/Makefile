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

TARGET=$(OBJS_DIR)/libPreConfig$(DYNAMIC_LIB_EXT)

USE_BOOST=1

LIBS:=$(LIB_PRE_CORE) $(LIB_PRE_MATHS) $(LIB_PRE_MODEL) $(LIB_PRE_API)

all: build

SRCS= \
CAutoconfigurer.cc \
CAutoconfigurerDetectorPenalties.cc \
CAutoconfigurerFieldRolePenalties.cc \
CAutoconfigurerParams.cc \
CDataCountStatistics.cc \
CDataSemantics.cc \
CDataSummaryStatistics.cc \
CDetectorEnumerator.cc \
CDetectorFieldRolePenalty.cc \
CDetectorRecord.cc \
CDetectorSpecification.cc \
CFieldRolePenalty.cc \
CFieldStatistics.cc \
CLongTailPenalty.cc \
CLowInformationContentPenalty.cc \
CLowVariationPenalty.cc \
Constants.cc \
CNotEnoughDataPenalty.cc \
ConfigTypes.cc \
CPenalty.cc \
CPolledDataPenalty.cc \
CReportWriter.cc \
CSpanTooSmallForBucketLengthPenalty.cc \
CSparseCountPenalty.cc \
CTools.cc \
CTooMuchDataPenalty.cc \

NO_BENCH_MARKS=1

include $(CPP_SRC_HOME)/mk/dynamiclib.mk
