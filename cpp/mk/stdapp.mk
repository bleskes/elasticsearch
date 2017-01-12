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

include $(CPP_SRC_HOME)/mk/stdmodule.mk

LDFLAGS:=$(EXELDFLAGS) $(LDFLAGS) $(LIB_PATH) $(ML_VER_LDFLAGS)
LIBS:=$(LOCALLIBS) $(LIB_ML_VER) $(LIBS)

ifndef INSTALL_DIR
INSTALL_DIR=$(CPP_PLATFORM_HOME)/bin
endif

ifndef CONF_INSTALL_DIR
ifdef TARGET_CONF
CONF_INSTALL_DIR=$(CPP_DISTRIBUTION_HOME)/resources
endif
endif

ifndef NO_INSTALL
INSTALL_CMD=$(MKDIR) $(INSTALL_DIR); $(INSTALL) $(TARGET) $(INSTALL_DIR)
ifdef TARGET_CONF
#CONF_INSTALL_CMD=$(MKDIR) $(CONF_INSTALL_DIR); $(INSTALL) $(TARGET_CONF) $(CONF_INSTALL_DIR)
endif
else
INSTALL_CMD=@ echo "$(TARGET) IS NOT INSTALLED"
endif

$(TARGET): $(OBJS) $(RESOURCE_FILE)
	$(CXX) $(LINK_OUT_FLAG)$@ $(PDB_FLAGS) $(OBJS) $(RESOURCE_FILE) $(LDFLAGS) $(LIBS)

test:
	+$(TEST_CMDS)

testobjcompile:
	+$(TEST_OBJ_COMPILE_CMDS)

objcompile: $(OBJS) testobjcompile

analyze: $(ANALYZEOBJS)

relink:
	$(RM) $(TARGET)
	$(RM) $(INSTALL_DIR)/$(notdir $(TARGET))
	$(MAKE) -f $(filter-out %.mk %.d,$(MAKEFILE_LIST)) build
	+$(RELINK_CMDS)

install:
	$(INSTALL_CMD)
	$(CONF_INSTALL_CMD)

build: $(TARGET)
	$(MAKE) -f $(filter-out %.mk %.d,$(MAKEFILE_LIST)) install

clean:
	$(RM) $(OBJS_DIR)/*$(OBJECT_FILE_EXT) $(OBJS_DIR)/*.d* $(OBJS_DIR)/*.plist $(OBJS_DIR)/*.res $(APP_CLEAN) core core.* $(TARGET) $(basename $(TARGET)).pdb $(basename $(TARGET)).map $(basename $(TARGET)).exp
	+$(CLEAN_CMDS)
	$(RMDIR) results

