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

OS=Linux

CPP_PLATFORM_HOME=$(CPP_DISTRIBUTION_HOME)/platform/linux-x86_64

CC=gcc
CXX=g++ -std=gnu++0x

ifndef PRELERT_DEBUG
OPTCFLAGS=-O3 -Wdisabled-optimization
OPTCPPFLAGS=-DNDEBUG
endif

ifdef PRELERT_DEBUG
ifdef PRELERT_COVERAGE
COVERAGE=--coverage
endif
endif

PLATPICFLAGS=-fPIC
CFLAGS=$(OPTCFLAGS) -msse3 -mfpmath=sse -fno-math-errno -Wall -Wcast-align -Wconversion -Wextra -Winit-self -Wparentheses -Wpointer-arith -Wswitch-enum $(COVERAGE)
CXXFLAGS=$(CFLAGS) -Wno-ctor-dtor-privacy -Wno-deprecated-declarations -fvisibility-inlines-hidden
CPPFLAGS=-isystem $(CPP_SRC_HOME)/3rd_party/include -isystem /usr/local/gcc62/include -D$(OS) -DLINUX=2 -D_REENTRANT -DBOOST_MATH_NO_LONG_DOUBLE_MATH_FUNCTIONS -DEIGEN_MPL2_ONLY $(OPTCPPFLAGS)
CDEPFLAGS=-MM
COMP_OUT_FLAG=-o 
LINK_OUT_FLAG=-o 
DEP_REFORMAT=sed 's,\($*\)\.o[ :]*,$(OBJS_DIR)\/\1.o $@ : ,g'
LOCALLIBS=-lm -lpthread -ldl -lrt
NETLIBS=-lnsl
BOOSTVER=1_62
BOOSTGCCVER=$(shell $(CXX) -dumpversion | awk -F. '{ print $$1$$2; }')
# Use -isystem instead of -I for Boost headers to suppress warnings from Boost
BOOSTINCLUDES=-isystem /usr/local/gcc62/include/boost-$(BOOSTVER)
BOOSTREGEXLIBS=-lboost_regex-gcc$(BOOSTGCCVER)-mt-$(BOOSTVER)
BOOSTIOSTREAMSLIBS=-lboost_iostreams-gcc$(BOOSTGCCVER)-mt-$(BOOSTVER)
BOOSTPROGRAMOPTIONSLIBS=-lboost_program_options-gcc$(BOOSTGCCVER)-mt-$(BOOSTVER)
BOOSTTHREADLIBS=-lboost_thread-gcc$(BOOSTGCCVER)-mt-$(BOOSTVER) -lboost_system-gcc$(BOOSTGCCVER)-mt-$(BOOSTVER) -lboost_atomic-gcc$(BOOSTGCCVER)-mt-$(BOOSTVER)
BOOSTFILESYSTEMLIBS=-lboost_filesystem-gcc$(BOOSTGCCVER)-mt-$(BOOSTVER) -lboost_system-gcc$(BOOSTGCCVER)-mt-$(BOOSTVER)
BOOSTDATETIMELIBS=-lboost_date_time-gcc$(BOOSTGCCVER)-mt-$(BOOSTVER)
XMLINCLUDES=`/usr/local/gcc62/bin/xml2-config --cflags`
XMLLIBS=`/usr/local/gcc62/bin/xml2-config --libs`
DYNAMICLIBLDFLAGS=$(PLATPICFLAGS) -shared -Wl,--as-needed -L$(CPP_PLATFORM_HOME)/lib $(COVERAGE) -Wl,-rpath,'$$ORIGIN/.'
JAVANATIVEINCLUDES=-I$(JAVA_HOME)/include
JAVANATIVELDFLAGS=-L$(JAVA_HOME)/jre/lib/server
JAVANATIVELIBS=-ljvm
CPPUNITLIBS=-lcppunit
LOG4CXXLIBS=-llog4cxx
ZLIBLIBS=-lz
EXELDFLAGS=-L$(CPP_PLATFORM_HOME)/lib $(COVERAGE) -Wl,-rpath,'$$ORIGIN/../lib'
UTLDFLAGS=$(EXELDFLAGS) -Wl,-rpath,$(CPP_PLATFORM_HOME)/lib
OBJECT_FILE_EXT=.o
DYNAMIC_LIB_EXT=.so
DYNAMIC_LIB_DIR=lib
STATIC_LIB_EXT=.a
SHELL_SCRIPT_EXT=.sh
UT_TMP_DIR=/tmp/$(LOGNAME)
LIB_PRE_CORE=-lPreCore
LIB_PRE_VER=-lPreVer
PRE_VER_LDFLAGS=-L$(CPP_SRC_HOME)/lib/ver/.objs
LIB_PRE_API=-lPreApi
LIB_PRE_MATHS=-lPreMaths
LIB_PRE_CONFIG=-lPreConfig
LIB_PRE_MODEL=-lPreModel
LIB_PRE_TEST=-lPreTest

LIB_PATH+=-L/usr/local/gcc62/lib

# Using cp instead of install here, to avoid every file being given execute
# permissions
INSTALL=cp
CP=cp
MKDIR=mkdir -p
RM=rm -f
RMDIR=rm -rf
MV=mv -f
SED=sed
ECHO=echo
CAT=cat
LN=ln
AR=ar -rus
ID=/usr/bin/id -u

