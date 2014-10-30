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

package com.prelert.client.diagnostics;


import com.google.gwt.i18n.client.Messages;
import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;


/**
 * Interface defining locale-sensitive constants used in the Prelert Diagnostics UIs.
 * @author Pete Harverson
 */
@DefaultLocale("en")
public interface DiagnosticsMessages extends Messages
{
	@DefaultMessage("Run")
	String configModuleLabel();
	
	@DefaultMessage("Database name")
	String databaseName();
	
	@DefaultMessage("Data type")
	String dataType();
	
	@DefaultMessage("Estimate")
	String estimate();
	
	@DefaultMessage("Estimated time to analyze data:")
	String estimateText();
	
	@DefaultMessage("Estimated time to completion: <span class=\"prl-configLabelStress\">{0} hours {1} minutes</span>")
	@AlternateMessage({
		"=0|=0", "Estimated time to completion:",
		"one|other", "Estimated time to completion: <span class=\"prl-configLabelStress\">1 hour {1} minutes</span>",
		"other|one", "Estimated time to completion: <span class=\"prl-configLabelStress\">{0} hours 1 minute</span>",
		"=0|one", "Estimated time to completion: <span class=\"prl-configLabelStress\">0 hours 1 minute</span>",
		"one|=0", "Estimated time to completion: <span class=\"prl-configLabelStress\">1 hour 0 minutes</span>",
		"one|one", "Estimated time to completion: <span class=\"prl-configLabelStress\">1 hour 1 minute</span>"
		}) 
	String estimatedTimeLeft(@PluralCount int hours, @PluralCount int minutes);
	
	@DefaultMessage("<img style=\"vertical-align:middle\" src=\"gxt/images/default/grid/loading.gif\" /> " + "Estimating. Please wait...")
	String estimatingWait();
	
	@DefaultMessage("{0} hours {1} minutes")   
	@AlternateMessage({
		"=0|=0", "",
		"=0|one", "0 hours 1 minute",
		"one|other", "1 hour {1} minutes",
		"one|=0", "1 hour 0 minutes",
		"one|one", "1 hour 1 minute",
		"other|one", "{0} hours 1 minute"
		}) 
	String estimateTime(@PluralCount int hours, @PluralCount int minutes);
	
	@DefaultMessage("Explore Results")
	String exploreResults();
	
	@DefaultMessage("Host")
	String host();
	
	@DefaultMessage("Your evaluation license has expired. " +
			"Please contact <a href=\"mailto:support@prelert.com\">Customer Support</a>.")
	String licenseExpired();
	
	@DefaultMessage("You have {0} days left on your evaluation license")
	@AlternateMessage({
	       "=0|other", "You have {1} hours left on your evaluation license",
	       "=0|one", "You have 1 hour left on your evaluation license",
	       "one|other", "You have 1 day left on your evaluation license",
	       "one|one", "You have {1} day left on your evaluation license"
	   })
	String licenseTimeLeft(@PluralCount int days, @PluralCount int hours);
	
	@DefaultMessage("<img style=\"vertical-align:middle\" src=\"gxt/images/default/grid/loading.gif\" /> " + "Loading. Please wait...")
	String loadingWait();
	
	@DefaultMessage("Password")
	String password();
	
	@DefaultMessage("{0}% complete")
	String percentComplete(int percent);
	
	@DefaultMessage("Port")
	String port();
	
	@DefaultMessage("Date")
	String problemTime();
	
	@DefaultMessage("Prelert Diagnostics&trade;")
	String productName();
	
	@DefaultMessage("Progress:")
	String progressFieldLabel();
	
	@DefaultMessage("Pulling data for {0}")
	String queryingDataForTime(String formattedTime);
	
	@DefaultMessage("Talk to an Engineer")
	String resourceEngineerTitle();
	
	@DefaultMessage("Read the FAQs")
	String resourceFAQsTitle();
	
	@DefaultMessage("Take a Video Tour")
	String resourceVideoTitle();
	
	@DefaultMessage("Run Analysis")
	String runAnalysis();
	
	@DefaultMessage("Run New Analysis")
	String runNewAnalysis();
	
	@DefaultMessage("<img style=\"vertical-align:middle\" src=\"gxt/images/default/grid/loading.gif\" /> " + "Saving. Please wait...")
	String savingWait();
	
	@DefaultMessage("See an Example")
	String seeExample();
	
	@DefaultMessage("Cancelling analysis ...")
	String stageCancelling();
	
	@DefaultMessage("Completed")
	String stageCompleted();
	
	@DefaultMessage("Error reported during analysis.")
	String stageError();
	
	@DefaultMessage("The analysis is starting up ...")
	String stageStarting();
	
	@DefaultMessage("The analysis has been stopped.")
	String stageStopped();
	
	@DefaultMessage("Analysis not yet started ...")
	String stageNotYetStarted();
	
	@DefaultMessage("For technical support email <a href=\"mailto:support@prelert.com\">support@prelert.com</a> " +
			"or phone 1-888 PRELERT (773-5378)")
	String technicalSupport();
	
	@DefaultMessage("Test")
	String test();
	
	@DefaultMessage("<img style=\"vertical-align:middle\" src=\"gxt/images/default/grid/loading.gif\" /> " + "Testing. Please wait...")
	String testingWait();
	
	@DefaultMessage("Username")
	String username();
	
	@DefaultMessage("View Progress of Analysis")
	String viewAnalysisProgress();
	
	@DefaultMessage("Use Prelert Diagnostics to analyze the troubleshooting data you have collected for a " +
			"recent performance issue. It will self-learn your environment&#39;s normal behavior, identify " +
			"the problem and tell you which metrics were involved and how they behaved to cause the problem.")
	String welcomeIntro();
	
	@DefaultMessage("Resources")
	String welcomeResourcesTitle();
	
	@DefaultMessage("Analyze the Cause of Application Problems")
	String welcomeTitle();
	
	@DefaultMessage("Prelert has completed the analysis of your data. Explore the results in the Activity page.")
	String wizardCompletedInstructions();
	
	@DefaultMessage("{0}.")
	String wizardCompletedSectionLabel(int stepNumber);
	
	@DefaultMessage("Data to analyze")
	String wizardDataToAnalyzeLabel();
	
	@DefaultMessage("{0}. Enter the date of the problem that you want Prelert to analyze, " +
			"then press the ''Estimate'' button to obtain an estimate of how long it will " +
			"take Prelert to perform the analysis.")
	String wizardDateSettingsInstructions(int stepNumber);
	
	@DefaultMessage("{0}. To run the analysis, you need to specify the type of data " +
		"that you want to analyze and how Prelert should connect to this data source.")
	String wizardDataSourceInstructions(int stepNumber);
	
	@DefaultMessage("Data source")
	String wizardDataSourceLabel();
	
	@DefaultMessage("{0}. Start the analysis and follow its progress. " +
			"The analysis will begin with data two days prior to the selected date " +
			"in order to provide an accurate baseline for activities.")
	String wizardProgressInstructions(int stepNumber);
	
	@DefaultMessage("Analysis status")
	String wizardProgressLabel();

	
	@DefaultMessage("An error has been reported during the analysis:<br>{0}<br>" +
			"Email <a href=\"mailto:support@prelert.com\">support@prelert.com</a> " +
			"or phone 1-888 PRELERT (773-5378), option 2 for technical help.")
	String errorAnalysisStatus(String errorDetails);
	
	@DefaultMessage("An error occurred cancelling the analysis.<br>" + 
			"Email <a href=\"mailto:support@prelert.com\">support@prelert.com</a> " +
			"or phone 1-888 PRELERT (773-5378), option 2 for technical help.")
	String errorCancellingAnalysis();
	
	@DefaultMessage("An error occurred estimating the length of time it will take the analysis to complete.<br>" + 
			"Email <a href=\"mailto:support@prelert.com\">support@prelert.com</a> " +
			"or phone 1-888 PRELERT (773-5378), option 2 for technical help.")
	String errorEstimatedAnalysisDuration();
	
	@DefaultMessage("Error obtaining status of the analysis from the server.<br>" + 
			"Email <a href=\"mailto:support@prelert.com\">support@prelert.com</a> " +
			"or phone 1-888 PRELERT (773-5378), option 2 for technical help.")
	String errorLoadingAnalysisStatus();
	
	@DefaultMessage("Error loading configuration data from the server.<br>" + 
			"Email <a href=\"mailto:support@prelert.com\">support@prelert.com</a> " +
			"or phone 1-888 PRELERT (773-5378), option 2 for technical help.")
	String errorLoadingConfigurationData();
	
	@DefaultMessage("Error saving the configuration of the connection to the data source. " +
			"Please check the details you entered for the data source are correct.")
	String errorSavingConfigurationData();

	@DefaultMessage("An error occurred starting the analysis. " +
			"Please check that all the configuration details you entered are correct.<br>" +
			"Email <a href=\"mailto:support@prelert.com\">support@prelert.com</a> " +
			"or phone 1-888 PRELERT (773-5378), option 2 for technical help.")
	String errorStartingAnalysis();
	
	@DefaultMessage("Error testing the configuration of the connection to the data source.<br>" +
			"Email <a href=\"mailto:support@prelert.com\">support@prelert.com</a> " +
			"or phone 1-888 PRELERT (773-5378), option 2 for technical help.")
	String errorTestingConfigurationData();
	
	@DefaultMessage("Invalid configuration data for the connection to the data source. " +
			"Please check the details you entered are correct.")
	String warningInvalidConfigurationData();
	
	@DefaultMessage("Unable to connect to data source. </br>The connection failed with message ''{0}''. " +
			"</br></br>Please check the details you entered are correct.")
	String warningConnectionError(String errorDetails);
	
	@DefaultMessage("The analysis will need to be restarted to get any further results. " +
			"Are you sure you wish to cancel the analysis?")
	String warningCancelAnalysis();
	
	@DefaultMessage("No data is available at the specified time. " +
			"Please pick a different date.")
	String warningNoEstimateData();
	
	@DefaultMessage("The analysis requires data points to be at a maximum interval of {0} seconds. " +
			"The data on the selected date is at intervals of {1} seconds. Please pick a later date.")
	@AlternateMessage({
		"one|other", "The analysis requires data points to be at a maximum interval of {0} second. " +
					"The data on the selected date is at intervals of {1} seconds. Please pick a later date.",
		"other|one", "The analysis requires data points to be at a maximum interval of {0} seconds. " +
					"The data on the selected date is at intervals of {1} second. Please pick a later date.",
		"one|one", "The analysis requires data points to be at a maximum interval of {0} second. " +
					"The data on the selected date is at intervals of {1} second. Please pick a later date."
	}) 		
	String warningEstimateDataPointIntervalTooLarge(@PluralCount int maxDataPointInterval, 
				@PluralCount int actualInterval);	
	
	@DefaultMessage("Invalid configuration data for the connection to the data source. " + 
		"Unable to obtain an estimate for the length of time it would take the analysis to complete.")
	String warningNoEstimateConnectionFailure();
	
	@DefaultMessage("Error estimating the analysis duration. </br>The estimate failed with message ''{0}''. " + 
			"</br></br>Unable to obtain an estimate for the length of time it would take the analysis to complete.")
	String warningEstimateDurationError(String errorDetails);
	
	@DefaultMessage("All existing results will be cleared on starting the new analysis. " +
			"Do you wish to continue?")
	String warningStartAnalysis();
}
