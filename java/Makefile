############################################################
#                                                          #
# Contents of file Copyright (c) Prelert Ltd 2006-2014     #
#                                                          #
#----------------------------------------------------------#
#----------------------------------------------------------#
# WARNING:                                                 #
# THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               #
# SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     #
# PARENT OR SUBSIDIARY COMPANIES.                          #
# PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         #
#                                                          #
# This source code is confidential and any person who      #
# receives a copy of it, or believes that they are viewing #
# it without permission is asked to notify Prelert Ltd     #
# on +44 (0)20 3567 1249 or email to legal@prelert.com.    #
# All intellectual property rights in this source code     #
# are owned by Prelert Ltd.  No part of this source code   #
# may be reproduced, adapted or transmitted in any form or #
# by any means, electronic, mechanical, photocopying,      #
# recording or otherwise.                                  #
#                                                          #
#----------------------------------------------------------#
#                                                          #
#                                                          #
############################################################
include $(PRELERT_SRC_HOME)/mk/defines.mk

V3GUI_BUILD_DIR=$(PRELERT_SRC_HOME)/java/apps/v3gui
INTROSCOPE_DOWNLOAD_UI_BUILD_DIR=$(PRELERT_SRC_HOME)/java/apps/diagnosticsCA
OPENAPI_DOWNLOAD_UI_BUILD_DIR=$(PRELERT_SRC_HOME)/java/apps/openAPI
DEMO_UI_BUILD_DIR=$(PRELERT_SRC_HOME)/java/apps/demoUI
PROXY_BUILD_DIR=$(PRELERT_SRC_HOME)/java/apps/proxy
DEVUTILS_BUILD_DIR=$(PRELERT_SRC_HOME)/java/apps/devutils
GWTCLIENT_BUILD_DIR=$(PRELERT_SRC_HOME)/java/apps/gwtClient
PRELERTAPI_BUILD_DIR=$(PRELERT_SRC_HOME)/java/apps/prelertAPI
ENGINEAPI_BUILD_DIR=$(PRELERT_SRC_HOME)/java/apps/engineApi
SPLUNK_GUI_INSTALL_DIR=$(PRELERT_HOME)/splunk_app/prelert/appserver/static

# Some operating systems ship with Ant, and put a config file for it in /etc,
# which we never want to use
ANT_ARGS=--noconfig

# For full builds we should never be picking up the $CLASSPATH environment
# variable
ANT_PROPS=-Dbuild.sysclasspath=ignore

# Don't let Maven download dependencies during offical builds
MAVEN_ARGS=--offline

ifdef PRELERT_TOOLS_HOME
# Override the hardcoded tools location (in the Ant properties) if
# $PRELERT_TOOLS_HOME is set
ANT_PROPS+= -Dlibs.basedir=$(PRELERT_TOOLS_HOME)/java
MAVEN_PROPS+= -Drepository.basedir=$(PRELERT_TOOLS_HOME)/maven
endif


ifdef BUILD_ENGINE_API

# Maven install includes compile so don't bother with a separate compile target
all: install

install:
	cd $(ENGINEAPI_BUILD_DIR) && mvn $(MAVEN_ARGS) $(MAVEN_PROPS) -DskipTests=true install

test:
	cd $(ENGINEAPI_BUILD_DIR) && mvn $(MAVEN_ARGS) $(MAVEN_PROPS) $(SONAR_INIT_STEP) test $(SONAR_MAIN_STEP)

clean:
	cd $(ENGINEAPI_BUILD_DIR) && mvn $(MAVEN_ARGS) $(MAVEN_PROPS) clean

deptree:
	cd $(ENGINEAPI_BUILD_DIR) && mvn $(MAVEN_ARGS) $(MAVEN_PROPS) dependency:tree

analyze:
	cd $(ENGINEAPI_BUILD_DIR) && mvn $(MAVEN_ARGS) $(MAVEN_PROPS) site

endif
ifdef OEM_NAME

all: compile install

compile:
	cd $(PRELERTAPI_BUILD_DIR) && ant $(ANT_ARGS) $(ANT_PROPS) build

# This target must depend on compile, or make -j could try to install at the
# same time as compiling.
install: compile
	cd $(PRELERTAPI_BUILD_DIR) && ant $(ANT_ARGS) $(ANT_PROPS) prelert-home-install

test:
	cd $(PRELERTAPI_BUILD_DIR) && ant $(ANT_ARGS) $(ANT_PROPS) test

clean:
	cd $(PRELERTAPI_BUILD_DIR) && ant $(ANT_ARGS) $(ANT_PROPS) clean

endif
ifdef FULL_BUILD

all: compile install

compile_v3gui:
	# Choose one of build_all_proxy, build_all_postgres, build_all_mysql
	cd $(V3GUI_BUILD_DIR) && ant $(ANT_ARGS) $(ANT_PROPS) build_all_postgres

compile_demoUI:
	cd $(DEMO_UI_BUILD_DIR) && ant $(ANT_ARGS) $(ANT_PROPS) build

compile_diagnosticsCA:
	cd $(INTROSCOPE_DOWNLOAD_UI_BUILD_DIR) && ant $(ANT_ARGS) $(ANT_PROPS) build

compile_openAPI:
	cd $(OPENAPI_DOWNLOAD_UI_BUILD_DIR) && ant $(ANT_ARGS) $(ANT_PROPS) build

compile_proxy:
	# Choose one of build-download-product, build-full-product
	cd $(PROXY_BUILD_DIR) && ant $(ANT_ARGS) $(ANT_PROPS) build-full-product

compile_devutils:
	cd $(DEVUTILS_BUILD_DIR) && ant $(ANT_ARGS) $(ANT_PROPS) build

compile_gwtclient:
	cd $(GWTCLIENT_BUILD_DIR) && ant $(ANT_ARGS) $(ANT_PROPS) build

# The sub-dependencies of the compile target will be built in parallel if
# make -j is used, so must not depend on each other
compile: compile_v3gui compile_demoUI compile_diagnosticsCA compile_openAPI compile_proxy compile_devutils

# Install the UI, demo UI, Proxy and individual plugins.
# Although we build all the real-time and download UIs, only one of them must
# be installed (otherwise whichever is installed first will be overwritten).
# This target must depend on compile, or make -j could try to install at the
# same time as compiling.
install: compile
	# Choose which GUI to install by altering the directory we change to
	cd $(V3GUI_BUILD_DIR) && ant $(ANT_ARGS) $(ANT_PROPS) prelert-home-install
	# Always additionally install the demo UI
	cd $(DEMO_UI_BUILD_DIR) && ant $(ANT_ARGS) $(ANT_PROPS) prelert-home-install
	# Choose between prelert-home-install-download, prelert-home-install-full
	cd $(PROXY_BUILD_DIR) && ant $(ANT_ARGS) $(ANT_PROPS) prelert-home-install-full
	# Here there should be one install line per proxy plugin to be shipped
	cd $(PROXY_BUILD_DIR) && ant $(ANT_ARGS) $(ANT_PROPS) -f build_plugin_introscope.xml prelert-home-install

test:
	cd $(PROXY_BUILD_DIR) && ant $(ANT_ARGS) $(ANT_PROPS) proxy-tests

clean:
	cd $(V3GUI_BUILD_DIR) && ant $(ANT_ARGS) $(ANT_PROPS) clean
	cd $(DEMO_UI_BUILD_DIR) && ant $(ANT_ARGS) $(ANT_PROPS) clean
	cd $(INTROSCOPE_DOWNLOAD_UI_BUILD_DIR) && ant $(ANT_ARGS) $(ANT_PROPS) clean
	cd $(OPENAPI_DOWNLOAD_UI_BUILD_DIR) && ant $(ANT_ARGS) $(ANT_PROPS) clean
	cd $(PROXY_BUILD_DIR) && ant $(ANT_ARGS) $(ANT_PROPS) clean-all
	cd $(DEVUTILS_BUILD_DIR) && ant $(ANT_ARGS) $(ANT_PROPS) clean

endif

