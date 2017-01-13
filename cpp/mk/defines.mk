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

# OS and makefile suffices
# solaris2
sunOS=SunOS

# Mac
macOS=Darwin

# Linux
linuxOS=Linux

# Windows
windows=MINGW

OS=$(shell uname)
REV=$(shell uname -r)

# Will return FQDN (Fully Qualified Domain Name) on all platforms except
# Solaris and Windows (MinGW).  On Solaris and Windows (MinGW) it will return
# the name of the machine.
HOSTNAME=$(shell uname -n)

CPP_DISTRIBUTION_HOME=$(CPP_SRC_HOME)/../cppdistribution

# Detect Solaris
ifeq ($(OS),$(sunOS))
include $(CPP_SRC_HOME)/mk/solaris.mk
endif

# Detect Linux
ifeq ($(OS),$(linuxOS))
ifdef CPP_CROSS_COMPILE
include $(CPP_SRC_HOME)/mk/linux_crosscompile_$(CPP_CROSS_COMPILE).mk
else
include $(CPP_SRC_HOME)/mk/linux.mk
endif
endif

# Detect MacOSX
ifeq ($(OS),$(macOS))
include $(CPP_SRC_HOME)/mk/macosx.mk
endif

# Detect Windows
ifeq ($(patsubst $(windows)%,$(windows),$(OS)),$(windows))
# If this default local drive letter is wrong, it can be overridden using an
# environment variable
LOCAL_DRIVE=C
include $(CPP_SRC_HOME)/mk/windows.mk
endif

OBJS_DIR=.objs

