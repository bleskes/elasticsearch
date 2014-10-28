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

package com.prelert.client.introscope;

import com.google.gwt.i18n.client.Messages;
import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;


/**
 * Interface defining locale-sensitive constants used in the Prelert Diagnostics 
 * for CA APM (Introscope) UI.
 * @author Pete Harverson
 */
@DefaultLocale("en")
public interface IntroscopeMessages extends Messages
{
	@DefaultMessage("Agents")
	String agents();
	
	@DefaultMessage("{0,number} agents selected")   
	@AlternateMessage({"one", "1 agent selected"})   
	String agentCountSelected(@PluralCount int itemCount); 
	
	@DefaultMessage("Select agents")
	String selectAgents();
	
	@DefaultMessage("Prelert Diagnostics employs the same powerful self-learning, predictive algorithms " +
			"that power Prelert&#39;s real-time products. These advanced analytics operate in batch mode on " +
			"historical data retrieved from Introscope to uncover the anomalous activity patterns. " +
			"Prelert gives you insight into the related metric features you need to determine the cause of a problem.")
	String welcomeIntroscopeIntro();
	
	@DefaultMessage("2. Enter the date of the problem and the list of CA Introscope agents that you want Prelert to analyze. " +
			"You can import data for a maximum of {0} agents.  The amount of time required " +
			"for the analysis depends on the amount of data collected and the processing power of " +
			"your EM server.  This can be adjusted by reducing the number of agents.")
	String wizardDataSettingsInstructions(int maxAgents);
	
	@DefaultMessage("1. To run the analysis, you need to specify how to access the " +
			"CA Introscope Enterprise Manager that you want Prelert to analyze data from.")
	String wizardEMSettingsInstructions();
	
	@DefaultMessage("CA Introscope Enterprise Manager")
	String wizardEMSettingsLabel();
	
	
	@DefaultMessage("An error occurred cancelling the analysis of Introscope data.<br>" + 
			"Email <a href=\"mailto:support@prelert.com\">support@prelert.com</a> " +
			"or phone 1-888 PRELERT (773-5378), option 2 for technical help.")
	String errorCancellingAnalysis();
	
	@DefaultMessage("Error loading details of the time and agents to be analyzed from Introscope.<br>" + 
			"Email <a href=\"mailto:support@prelert.com\">support@prelert.com</a> " +
			"or phone 1-888 PRELERT (773-5378), option 2 for technical help.")
	String errorLoadingAnalysisSettings();
	
	@DefaultMessage("Error loading configuration data for the Introscope connection.<br>" + 
			"Email <a href=\"mailto:support@prelert.com\">support@prelert.com</a> " +
			"or phone 1-888 PRELERT (773-5378), option 2 for technical help.")
	String errorLoadingConfigurationData();
	
	@DefaultMessage("Template configuration data for Introscope not found on the server.<br>" + 
			"Email <a href=\"mailto:support@prelert.com\">support@prelert.com</a> " +
			"or phone 1-888 PRELERT (773-5378), option 2 for technical help.")
	String errorMissingDataTypeConfiguration();
	
	@DefaultMessage("Error saving configuration settings for the analysis of Introscope data. " +
			"Please check the details you entered for the Enterprise Manager are correct.<br>" +
			"Email <a href=\"mailto:support@prelert.com\">support@prelert.com</a> " +
			"or phone 1-888 PRELERT (773-5378), option 2 for technical help.")
	String errorSavingConfigurationData();
	
	@DefaultMessage("Error testing the configuration data for the Introscope connection.<br>" +
			"Email <a href=\"mailto:support@prelert.com\">support@prelert.com</a> " +
			"or phone 1-888 PRELERT (773-5378), option 2 for technical help.")
	String errorTestingConfigurationData();
	
	@DefaultMessage("Invalid configuration data for the Introscope connection. " +
			"Please check the details you entered for the Enterprise Manager are correct.")
	String warningInvalidConfigurationData();
	
	@DefaultMessage("You may only select a maximum of {0} agents for analysis. " +
	"Please deselect an agent before selecting another.")
	String warningMaximumAgents(int maxSelect);
	
	@DefaultMessage("Unable to access the Enterprise Manager&#39;s health metrics. " + 
			"Prelert requires access to the Overall Capacity metric to monitor performance.")
	String warningMissingHealthMetrics();
	
	@DefaultMessage("Invalid connection to the Enterprise Manager. " + 
			"Unable to obtain an estimate for the length of time it would take the analysis to complete.")
	String warningNoEstimateConnectionFailure();
}
