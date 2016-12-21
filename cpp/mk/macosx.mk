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

OS=MacOSX

CPP_PLATFORM_HOME=$(CPP_DISTRIBUTION_HOME)/platform/darwin-x86_64

CC=clang
CXX=clang++ -std=c++11 -stdlib=libc++

ifndef PRELERT_DEBUG
OPTCFLAGS=-O3
OPTCPPFLAGS=-DNDEBUG
endif

ifdef PRELERT_DEBUG
ifdef PRELERT_COVERAGE
COVERAGE=--coverage
endif
endif

# Start by enabling all warnings and then disable the really pointless/annoying ones
CFLAGS=-g $(OPTCFLAGS) -msse4.1 -Weverything -Werror-switch -Wno-deprecated -Wno-disabled-macro-expansion -Wno-documentation-deprecated-sync -Wno-float-equal -Wno-gnu -Wno-missing-prototypes -Wno-padded -Wno-sign-conversion -Wno-unreachable-code -Wno-used-but-marked-unused $(COVERAGE)
# Versions of clang newer than 3.2 moan about unknown Doxygen commands but don't
# have a very comprehensive list of them
ifneq ($(shell clang --version | head -1 | sed 's/.*LLVM //' | sed 's/[^0-9]//g'), 32)
CFLAGS+=-Wno-documentation-unknown-command
endif
CXXFLAGS=$(CFLAGS) -Wno-c++98-compat -Wno-c++98-compat-pedantic -Wno-exit-time-destructors -Wno-global-constructors -Wno-undefined-reinterpret-cast -Wno-unused-member-function -Wno-weak-vtables
CPPFLAGS=-isystem $(CPP_SRC_HOME)/3rd_party/include -isystem /usr/local/include -D$(OS) -DBOOST_MATH_NO_LONG_DOUBLE_MATH_FUNCTIONS -DEIGEN_MPL2_ONLY $(OPTCPPFLAGS)
ANALYZEFLAGS=--analyze
CDEPFLAGS=-MM
COMP_OUT_FLAG=-o 
ANALYZE_OUT_FLAG=-o 
LINK_OUT_FLAG=-o 
DEP_REFORMAT=sed 's,\($*\)\.o[ :]*,$(OBJS_DIR)\/\1.o $@ : ,g'
LOCALLIBS=
NETLIBS=
BOOSTVER=1_62
BOOSTCLANGVER=$(shell $(CXX) -dumpversion | awk -F. '{ print $$1$$2; }')
# Use -isystem instead of -I for Boost headers to suppress warnings from Boost
BOOSTINCLUDES=-isystem /usr/local/include/boost-$(BOOSTVER)
BOOSTREGEXLIBS=-lboost_regex-clang-darwin$(BOOSTCLANGVER)-mt-$(BOOSTVER)
BOOSTIOSTREAMSLIBS=-lboost_iostreams-clang-darwin$(BOOSTCLANGVER)-mt-$(BOOSTVER)
BOOSTPROGRAMOPTIONSLIBS=-lboost_program_options-clang-darwin$(BOOSTCLANGVER)-mt-$(BOOSTVER)
BOOSTTHREADLIBS=-lboost_thread-clang-darwin$(BOOSTCLANGVER)-mt-$(BOOSTVER) -lboost_system-clang-darwin$(BOOSTCLANGVER)-mt-$(BOOSTVER) -lboost_atomic-clang-darwin$(BOOSTCLANGVER)-mt-$(BOOSTVER)
BOOSTFILESYSTEMLIBS=-lboost_filesystem-clang-darwin$(BOOSTCLANGVER)-mt-$(BOOSTVER) -lboost_system-clang-darwin$(BOOSTCLANGVER)-mt-$(BOOSTVER)
BOOSTDATETIMELIBS=-lboost_date_time-clang-darwin$(BOOSTCLANGVER)-mt-$(BOOSTVER)
XMLINCLUDES=-isystem /usr/include/libxml2
XMLLIBLDFLAGS=-L/usr/lib
XMLLIBS=-lxml2
JAVANATIVEINCLUDES=-I`/usr/libexec/java_home`/include
JAVANATIVELDFLAGS=-L`/usr/libexec/java_home`/jre/lib/server
JAVANATIVELIBS=-ljvm
PRELERT_VERSION_NUM=$(shell cat $(CPP_SRC_HOME)/../gradle.properties | grep '^elasticsearchVersion' | awk -F= '{ print $$2 }' | xargs echo | sed 's/-.*//')
DYNAMICLIBLDFLAGS=-current_version $(PRELERT_VERSION_NUM) -compatibility_version $(PRELERT_VERSION_NUM) -dynamiclib -Wl,-dead_strip_dylibs $(COVERAGE) -Wl,-install_name,@rpath/$(notdir $(TARGET)) -L$(CPP_PLATFORM_HOME)/lib -Wl,-rpath,@loader_path/.
CPPUNITLIBS=-lcppunit
LOG4CXXLIBS=-llog4cxx
ZLIBLIBS=-lz
EXELDFLAGS=-bind_at_load -L$(CPP_PLATFORM_HOME)/lib $(COVERAGE) -Wl,-rpath,@loader_path/../lib
UTLDFLAGS=$(EXELDFLAGS) -Wl,-rpath,$(CPP_PLATFORM_HOME)/lib
OBJECT_FILE_EXT=.o
DYNAMIC_LIB_EXT=.dylib
DYNAMIC_LIB_DIR=lib
STATIC_LIB_EXT=.a
SHELL_SCRIPT_EXT=.sh
UT_TMP_DIR=/tmp/$(LOGNAME)
LIB_PRE_API=-lPreApi
LIB_PRE_CORE=-lPreCore
LIB_PRE_VER=-lPreVer
PRE_VER_LDFLAGS=-L$(CPP_SRC_HOME)/lib/ver/.objs
LIB_PRE_MATHS=-lPreMaths
LIB_PRE_CONFIG=-lPreConfig
LIB_PRE_MODEL=-lPreModel
LIB_PRE_TEST=-lPreTest

LIB_PATH+=-L/usr/local/lib

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

