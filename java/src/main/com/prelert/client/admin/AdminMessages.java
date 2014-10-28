/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ***********************************************************/

package com.prelert.client.admin;

import com.google.gwt.i18n.client.Messages;
import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;


/**
 * Interface defining locale-sensitive messages used in the Administration module, 
 * such as labels for UI controls.
 * @author Pete Harverson
 */
@DefaultLocale("en")
public interface AdminMessages extends Messages
{
	@DefaultMessage("Add new user")
	String addNewUserHeading();
	
	@DefaultMessage("Add")
	String add();
	
	@DefaultMessage("Admin")
	String admin();
	
	@DefaultMessage("Alerting")
	String alerting();
	
	@DefaultMessage("Prelert can run a script when an activity with an anomaly score " +
			"higher than a specified threshold is generated.<br><br>"+
			"To prevent security vulnerabilities, " +
			"all alerting scripts must be placed in the {0} directory on the Prelert server.<br><br>" +
			"The script must be a directly executable file. On Unix, the file " + 
			"must have its execute permission bit set and must either be a native program, or (more likely) " +
			"a text based script where the first line specifies the command interpreter (e.g. #!/usr/bin/perl " +
			"or #!/bin/bash). On Windows, the file must have an extension that the Windows command " +
			"interpreter is prepared to execute (by default one of .COM, .EXE, .BAT, .CMD, .VBS, .VBE, " +
			".JS, .JSE, .WSF, .WSH or .MSC).")
	String alertingInstructions(String alertScriptDir);
	
	@DefaultMessage("Alerting configuration successfully updated on server.")
	String alertingDataSavedSuccess();
	
	@DefaultMessage("Anomaly threshold")
	String anomalyThreshold();
	
	@DefaultMessage("Delete")
	String delete();
	
	@DefaultMessage("Confirm Delete")
	String deleteConfirm();
	
	@DefaultMessage("Enable alerts")
	String enableAlerts();
	
	@DefaultMessage("First name")
	String firstName();
	
	@DefaultMessage("Last name")
	String lastName();
	
	@DefaultMessage("Role")
	String role();
	
	@DefaultMessage("Script file")
	String scriptFile();
	
	@DefaultMessage("Script filename")
	String scriptFilename();
	
	@DefaultMessage("You cannot delete your own user account.")
	String userCannotDeleteSelf();
	
	@DefaultMessage("Are you sure you want to delete user {0}?")
	String userDeleteConfirm(String username);
	
	@DefaultMessage("User details")
	String userDetails();
	
	@DefaultMessage("Details for {0}")
	String userDetailsHeading(String username);
	
	@DefaultMessage("Prelert Users")
	String userListHeading();
	
	@DefaultMessage("Username")
	String username();
	
	@DefaultMessage("Users")
	String users();
	
	@DefaultMessage("An error occurred whilst loading alerting data.")
	String errorLoadingAlertingData();
	
	@DefaultMessage("An error occurred whilst saving alerting data.")
	String errorSavingAlertingData();
	
	@DefaultMessage("An error occurred whilst trying to delete the user.")
	String errorDeletingUser();
	
	@DefaultMessage("An error occurred whilst loading user data.")
	String errorLoadingUserData();
	
	@DefaultMessage("An error occurred whilst saving user data.")
	String errorSavingUserData();
	
	@DefaultMessage("Please enter the name of the script.")
	String warningNoScriptName();
}
