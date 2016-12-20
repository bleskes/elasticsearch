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

OS=Windows

CPP_PLATFORM_HOME=$(CPP_DISTRIBUTION_HOME)/platform/windows-x86_64

CC=cl
CXX=cl

# Generally we'll want to build with a DLL version of the C runtime library, but
# occasionally we may need to override this
ifndef CRT_OPT
CRT_OPT=-MD
endif

ifndef PRELERT_DEBUG
OPTCFLAGS=-O2 -Qfast_transcendentals -GS-
OPTCFLAGS+= -Qvec-report:1
OPTCPPFLAGS=-DNDEBUG
endif

# On 64 bit Windows Visual Studio 2013 is in C:\Program Files (x86) aka C:\PROGRA~2
VCINCLUDES=-I$(LOCAL_DRIVE):/PROGRA~2/MICROS~1.0/VC/include
VCLDFLAGS=-LIBPATH:$(LOCAL_DRIVE):/PROGRA~2/MICROS~1.0/VC/lib/amd64
WINSDKINCLUDES=-I$(LOCAL_DRIVE):/PROGRA~2/WINDOW~4/8.1/Include/shared -I$(LOCAL_DRIVE):/PROGRA~2/WINDOW~4/8.1/Include/um
WINSDKLDFLAGS=-LIBPATH:$(LOCAL_DRIVE):/PROGRA~2/WINDOW~4/8.1/Lib/winv6.3/um/x64
SHELL=$(LOCAL_DRIVE):/PROGRA~1/Git/bin/bash.exe
CFLAGS=-nologo $(OPTCFLAGS) -W4 $(CRT_OPT) -EHsc -Zi -Gw -FS -Zc:inline
CXXFLAGS=-TP $(CFLAGS) -Zc:rvalueCast -Zc:strictStrings -wd4127 -we4150 -wd4201 -wd4231 -wd4251 -wd4355 -wd4512 -wd4702 -bigobj
ANALYZEFLAGS=-nologo -analyze:only -analyze:stacksize100000 $(CRT_OPT)

CPPFLAGS=-X -I$(CPP_SRC_HOME)/3rd_party/include -I$(LOCAL_DRIVE):/usr/local/include $(VCINCLUDES) $(WINSDKINCLUDES) -D$(OS) -D_CRT_SECURE_NO_WARNINGS -D_CRT_NONSTDC_NO_DEPRECATE -DWIN32_LEAN_AND_MEAN -DNTDDI_VERSION=0x06010000 -D_WIN32_WINNT=0x0601 -DBOOST_ALL_DYN_LINK -DBOOST_ALL_NO_LIB -DBOOST_MATH_NO_LONG_DOUBLE_MATH_FUNCTIONS -DEIGEN_MPL2_ONLY -DCPPUNIT_DLL -DBUILDING_$(basename $(notdir $(TARGET))) $(OPTCPPFLAGS)
# -MD defines _DLL and _MT - for dependency determination we must define these
# otherwise the Boost headers will throw errors during preprocessing
ifeq ($(CRT_OPT),-MD)
CDEPFLAGS=-nologo -E -D_DLL -D_MT
else
CDEPFLAGS=-nologo -E -D_MT
endif
COMP_OUT_FLAG=-Fo
ANALYZE_OUT_FLAG=-analyze:log
LINK_OUT_FLAG=-Fe
AR_OUT_FLAG=-OUT:
# Get the dependencies that aren't under C:\usr\local or C:\Program Files*, on
# the assumption that the 3rd party tools won't change very often, and if they
# do then we'll rebuild everything from scratch
DEP_FILTER= 2>/dev/null | egrep "^.line .*(\\.h|$<)" | tr -s '\\\\' '/' | awk -F'"' '{ print $$2 }' | egrep -i -v "usr.local|$(LOCAL_DRIVE)..progra" | sed 's~/[a-z]*/\.\./~/~g' | sort -f -u | sort -t. -k2 | tr '\r\n\t' ' ' | sed 's/  / /g' | sed 's/^ //' | sed 's/ $$//'
DEP_REFORMAT=sed 's,$<,$(basename $@)$(OBJECT_FILE_EXT) $@ : $<,'
LOCALLIBS=AdvAPI32.lib shell32.lib Version.lib
NETLIBS=WS2_32.lib
BOOSTVER=1_62
BOOSTVCVER=$(shell $(CXX) 2>&1 | head -1 | sed 's/.*Version //' | awk -F. '{ print ($$1 - 6)($$2 / 10); }')
BOOSTINCLUDES=-I$(LOCAL_DRIVE):/usr/local/include/boost-$(BOOSTVER)
BOOSTREGEXLIBS=boost_regex-vc$(BOOSTVCVER)-mt-$(BOOSTVER).lib
BOOSTIOSTREAMSLIBS=boost_iostreams-vc$(BOOSTVCVER)-mt-$(BOOSTVER).lib
BOOSTPROGRAMOPTIONSLIBS=boost_program_options-vc$(BOOSTVCVER)-mt-$(BOOSTVER).lib
BOOSTTHREADLIBS=boost_thread-vc$(BOOSTVCVER)-mt-$(BOOSTVER).lib boost_system-vc$(BOOSTVCVER)-mt-$(BOOSTVER).lib boost_atomic-vc$(BOOSTVCVER)-mt-$(BOOSTVER).lib
BOOSTFILESYSTEMLIBS=boost_filesystem-vc$(BOOSTVCVER)-mt-$(BOOSTVER).lib boost_system-vc$(BOOSTVCVER)-mt-$(BOOSTVER).lib
BOOSTDATETIMELIBS=boost_date_time-vc$(BOOSTVCVER)-mt-$(BOOSTVER).lib
XMLINCLUDES=-I$(LOCAL_DRIVE):/usr/local/include/libxml
XMLLIBLDFLAGS=-LIBPATH:$(LOCAL_DRIVE):/usr/local/lib
XMLLIBS=libxml2.lib
DYNAMICLIBLDFLAGS=-nologo -Zi $(CRT_OPT) -LD -link -MAP -OPT:REF -INCREMENTAL:NO -LIBPATH:$(CPP_PLATFORM_HOME)/$(IMPORT_LIB_DIR)
CPPUNITLIBS=cppunit_dll.lib
LOG4CXXLIBS=log4cxx.lib
ZLIBLIBS=zdll.lib
STRPTIMELIBS=strptime.lib
EXELDFLAGS=-nologo -Zi $(CRT_OPT) -link -MAP -OPT:REF -SUBSYSTEM:CONSOLE,6.1 -INCREMENTAL:NO -LIBPATH:$(CPP_PLATFORM_HOME)/$(IMPORT_LIB_DIR)
UTLDFLAGS=$(EXELDFLAGS)
OBJECT_FILE_EXT=.obj
DYNAMIC_LIB_EXT=.dll
DYNAMIC_LIB_DIR=bin
IMPORT_LIB_DIR=lib
RESOURCE_FILE=$(OBJS_DIR)/prelert.res
STATIC_LIB_EXT=.lib
SHELL_SCRIPT_EXT=.bat
# This temp directory assumes we're running in a Unix-like shell such as Git bash
UT_TMP_DIR=/tmp
EXE_EXT=.exe
INSTALL=cp
CP=cp
RC=rc -nologo
LIB_PRE_CORE=libPreCore.lib
LIB_PRE_VER=libPreVer.lib
PRE_VER_LDFLAGS=-LIBPATH:$(CPP_SRC_HOME)/lib/ver/.objs
LIB_PRE_API=libPreApi.lib
LIB_PRE_MATHS=libPreMaths.lib
LIB_PRE_CONFIG=libPreConfig.lib
LIB_PRE_MODEL=libPreModel.lib
LIB_PRE_TEST=libPreTest.lib

LIB_PATH+=-LIBPATH:$(LOCAL_DRIVE):/usr/local/lib $(VCLDFLAGS) $(WINSDKLDFLAGS)
PDB_FLAGS=-Fd$(basename $(TARGET)).pdb

MKDIR=mkdir -p
RM=rm -f
RMDIR=rm -rf
MV=mv -f
SED=sed
ECHO=echo
CAT=cat
LN=ln
AR=lib -NOLOGO
ID=id -u

OS_LIBRARY_PATH=PATH
