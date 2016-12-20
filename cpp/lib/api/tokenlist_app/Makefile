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

TARGET=tokenlist_app$(EXE_EXT)

INSTALL_DIR=$(CPP_PLATFORM_HOME)/bin

PRELERT_LIBS=$(LIB_PRE_CORE) $(LIB_PRE_MODEL) $(LIB_PRE_API)

USE_BOOST=1

LIBS=$(PRELERT_LIBS)

all: build

SRCS= \
    Main.cc \

NO_BENCH_MARKS=1
NO_TEST_CASES=1

include $(CPP_SRC_HOME)/mk/stdapp.mk

