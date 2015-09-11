############################################################
#                                                          #
# Contents of file Copyright (c) Prelert Ltd 2006-2015     #
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

ENGINEAPI_BUILD_DIR=$(PRELERT_SRC_HOME)/java/apps/engineApi

# Don't let Maven download dependencies during official builds
# To update the dependencies run "make MAVEN_ARGS="
#MAVEN_ARGS=--offline
MAVEN_ARGS=

ifdef PRELERT_TOOLS_HOME
# Override the default tools location if $PRELERT_TOOLS_HOME is set
# MAVEN_PROPS+= -Drepository.basedir=$(PRELERT_TOOLS_HOME)/maven
MAVEN_PROPS=
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

