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

OS=SOLARIS

CPP_PLATFORM_HOME=$(CPP_DISTRIBUTION_HOME)/platform/sunos-x86_64
OPTCPUFLAGS=-xarch=sse3 -xchip=sandybridge -nofstore

CC=cc
CXX=CC -std=c++11

ifndef ML_DEBUG
OPTCFLAGS=-fsimple=1 -O4 -xbuiltin=%all -xlibmil -xlibmopt $(OPTCPUFLAGS)
OPTCPPFLAGS=-DNDEBUG
endif

PLATPICFLAGS=-KPIC
CFLAGS=-m64 -g $(OPTCFLAGS) -xalias_level=layout -xsecure_code_analysis=no
# Something in the Eigen library used by several source files in the maths
# library crashes the Solaris Studio name demangler so we can't build these
# files with debug symbols
CXXFLAGS=-m64 -g0 $(OPTCFLAGS) -xalias_level=compatible -features=extensions -errtags +w -erroff=ambigfuncdecl,anonnotype,anonstruct,arrowrtn2,badargtype2w,hidef,notemsource,wbadasg,wbadlkginit -xsecure_code_analysis=no
CPPFLAGS=-mt -I$(CPP_SRC_HOME)/3rd_party/include -I/usr/local/include -D$(OS) -D_POSIX_PTHREAD_SEMANTICS -DBOOST_MATH_NO_LONG_DOUBLE_MATH_FUNCTIONS -DEIGEN_MPL2_ONLY -DEIGEN_DONT_USE_RESTRICT_KEYWORD $(OPTCPPFLAGS)
CDEPFLAGS=-m64 -xM1
COMP_OUT_FLAG=-o 
LINK_OUT_FLAG=-o 
# Filter out dependencies on headers under /usr/local
DEP_FILTER= | grep -v ' /usr/local/' | nawk -F':' '{ if (index($$2, ".h") == 0 && index($$2, "Eigen") == 0) printf("%s:", $$1); if (++hash[$$2] == 1) printf("%s", $$2); }; END { print "" }'
DEP_REFORMAT=sed 's,\($*\)\.o[ :]*,$(OBJS_DIR)\/\1.o $@ : ,g'
# On Solaris, multi-threaded programs run much faster when linked with
# libumem, which contains an optimised version of malloc(), but this must
# be the first library that the program loads
LOCALLIBS=-lumem
NETLIBS=
BOOSTVER=1_62
BOOSTINCLUDES=-I/usr/local/include/boost-$(BOOSTVER)
BOOSTREGEXLIBS=-lboost_regex-sw-mt-$(BOOSTVER)
BOOSTIOSTREAMSLIBS=-lboost_iostreams-sw-mt-$(BOOSTVER)
BOOSTPROGRAMOPTIONSLIBS=-lboost_program_options-sw-mt-$(BOOSTVER)
BOOSTTHREADLIBS=-lboost_thread-sw-mt-$(BOOSTVER) -lboost_system-sw-mt-$(BOOSTVER) -lboost_atomic-sw-mt-$(BOOSTVER)
BOOSTFILESYSTEMLIBS=-lboost_filesystem-sw-mt-$(BOOSTVER) -lboost_system-sw-mt-$(BOOSTVER)
BOOSTDATETIMELIBS=-lboost_date_time-sw-mt-$(BOOSTVER)
XMLINCLUDES=-I/usr/local/include/libxml2
XMLLIBLDFLAGS=-L/usr/local/lib
XMLLIBS=-lxml2
DYNAMICLIBLDFLAGS=-m64 -G $(OPTCFLAGS) $(PLATPICFLAGS) -i -norunpath -R'$$ORIGIN/.' -z ignore -L$(CPP_PLATFORM_HOME)/lib
EXELDFLAGS=-m64 $(OPTCFLAGS) -i -norunpath -R'$$ORIGIN/../lib' -L$(CPP_PLATFORM_HOME)/lib
UTLDFLAGS=$(EXELDFLAGS) -R$(CPP_PLATFORM_HOME)/lib
CPPUNITLIBS=-lcppunit
LOG4CXXLIBS=-llog4cxx
ZLIBLIBS=-lz
LIB_PRE_CORE=-lPreCore
LIB_PRE_VER=-lPreVer
PRE_VER_LDFLAGS=-L$(CPP_SRC_HOME)/lib/ver/.objs
LIB_PRE_API=-lPreApi
LIB_PRE_MATHS=-lPreMaths
LIB_PRE_CONFIG=-lPreConfig
LIB_PRE_MODEL=-lPreModel
LIB_PRE_TEST=-lPreTest
OBJECT_FILE_EXT=.o
DYNAMIC_LIB_EXT=.so
DYNAMIC_LIB_DIR=lib
STATIC_LIB_EXT=.a
SHELL_SCRIPT_EXT=.sh
UT_TMP_DIR=/tmp/$(LOGNAME)

LIB_PATH+=-L/usr/local/lib

#INSTALL=/usr/sbin/install
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

