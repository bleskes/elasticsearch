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

package com.prelert.client;

import com.google.gwt.i18n.client.Messages;
import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;


/**
 * Interface defining locale-sensitive constants used in client applications, 
 * such as labels for UI controls.
 * @author Pete Harverson
 */
@DefaultLocale("en")
public interface ClientMessages extends Messages
{
	@DefaultMessage("Activity")
	String activity();
	
	@DefaultMessage("Select activity for analysis")
	String activitySelect();
	
	@DefaultMessage("Activity Heat Map")
	String activityTimelineHeading();
	
	@DefaultMessage("All data types")
	String allDataTypes();
	
	@DefaultMessage("All sources")
	String allSources();
	
	@DefaultMessage("Analysis")
	String analysis();
	
	@DefaultMessage("Analysis for activity at {0}")
	String analysisDataHeading(String formattedTime);
	
	@DefaultMessage("Analysis for {0} : {1}")
	String analysisFor(String description, String formattedTime);
	
	@DefaultMessage("Analyze")
	String analyse();
	
	@DefaultMessage("Analyzed Data")
	String analysedData();
	
	@DefaultMessage("Most common")
	String analyseOptionMostCommon();
	
	@DefaultMessage("Path order")
	String analyseOptionPathOrder();
	
	@DefaultMessage("anomaly score")
	String anomalyScore();
	
	@DefaultMessage("Anomaly threshold: ")
	String anomalyThreshold();
	
	@DefaultMessage("Apply")
	String apply();
	
	@DefaultMessage("attribute")
	String attribute();
	
	@DefaultMessage("Attributes")
	String attributesHeading();
	
	@DefaultMessage("Start automatic refresh")
	String autoRefreshStart();
	
	@DefaultMessage("Pause automatic refresh")
	String autoRefreshPause();
	
	@DefaultMessage("Cancel")
	String cancel();
	
	@DefaultMessage("category")
	String category();
	
	@DefaultMessage("Change password")
	String changePassword();
	
	@DefaultMessage("Clear")
	String clear();
	
	@DefaultMessage("analysis")
	String columnProbableCause();
	
	@DefaultMessage(": Contact Us")
	String contactPrelert();
	
	@DefaultMessage("&#169; Prelert Ltd 2006-{0}")
	String copyrightNotice(String buildYear);
	
	@DefaultMessage("count")
	String count();
	
	@DefaultMessage("{0} data sources")
	String dataSourcesForType(String dataSourceType);
	
	@DefaultMessage("Data types")
	String dataTypes();
	
	@DefaultMessage("description")
	String description();
	
	@DefaultMessage("Detailed analysis")
	String detailedAnalysis();
	
	@DefaultMessage("Details on activity at {0} : {1}")
	String detailsOnActivity(String formattedTime, String description);
	
	@DefaultMessage("Details on activity at {0}")
	String detailsOnActivityAtTime(String formattedTime);
	
	@DefaultMessage("Details on {0} data")
	String detailsOnData(String description);
	
	@DefaultMessage("Diagnostics chart")
	String diagnosticsChart();
	
	@DefaultMessage("Edit")
	String edit();
	
	@DefaultMessage("end")
	String endTime();
	
	@DefaultMessage("{0} id {1}")
	String evidenceDetailsHeading(String dataType, int evidenceId);
	
	@DefaultMessage("evidence id")
	String evidenceId();
	
	@DefaultMessage("Explorer")
	String explorer();
	
	@DefaultMessage("Export")
	String export();
	
	@DefaultMessage("To {0}")
	String exportTo(String format);
	
	@DefaultMessage("features")
	String featureCount();
	
	@DefaultMessage("Features")
	String features();
	
	@DefaultMessage("Analysis for: ")
	String fieldAnalysisFor();
	
	@DefaultMessage("Filter: ")
	String fieldFilter();
	
	@DefaultMessage("Show: ")
	String fieldShow();
	
	@DefaultMessage("Time occurred: ")
	String fieldTimeOccurred();
	
	@DefaultMessage("Group by:")
	String fieldGroupBy();
	
	@DefaultMessage("for")
	String forLower();
	
	@DefaultMessage("Help")
	String help();
	
	@DefaultMessage("Hide")
	String hide();
	
	@DefaultMessage("Hide help text")
	String hideHelpText();
	
	@DefaultMessage("id")
	String id();
	
	@DefaultMessage("influence")
	String influence();
	
	@DefaultMessage("{0} Items")   
	@AlternateMessage({"one", "1 Item"})   
	String itemCount(@PluralCount int count);
	
	@DefaultMessage("To link to the analysis of this activity, copy and paste the URL:")
	String linkToAnalysisInfo();
	
	@DefaultMessage("Link to {0}")
	String linkToModule(String moduleId);
	
	@DefaultMessage("Log out")
	String logOut();
	
	@DefaultMessage("magnitude")
	String magnitude();
	
	@DefaultMessage("metric")
	String metric();
	
	@DefaultMessage("Newer")
	String newer();
	
	@DefaultMessage("Newest")
	String newest();
	
	@DefaultMessage("No data")
	String noData();
	
	@DefaultMessage("not available")
	String notAvailable();
	
	@DefaultMessage("notifications")
	String notificationCount();
	
	@DefaultMessage("Notification data for {0}")
	String notificationDataHeading(String description);
	
	@DefaultMessage("Notification data for activity at {0}")
	String notificationDataForActivityHeading(String formattedTime);
	
	@DefaultMessage("not present")
	String notPresent();
	
	@DefaultMessage("Older")
	String older();
	
	@DefaultMessage("Oldest")
	String oldest();

	@DefaultMessage("-- All --")
	String optionAll();
	
	@DefaultMessage("-- Select --")
	String optionSelect();
	
	@DefaultMessage("other")
	String other();
	
	@DefaultMessage("Pan left")
	String panLeftLink();
	
	@DefaultMessage("Pan right")
	String panRightLink();
	
	@DefaultMessage("The password for {0} has been changed successfully.")
	String passwordChangedSuccess(String username);
	
	@DefaultMessage("Current password")
	String passwordCurrent();
	
	@DefaultMessage("The passwords you entered do not match. Please retype the password in both boxes.")
	String passwordsDoNotMatch();
	
	@DefaultMessage("You have entered your current password incorrectly. Please retype the password.")
	String passwordIncorrect();
	
	@DefaultMessage("New password")
	String passwordNew();
	
	@DefaultMessage("Confirm new password")
	String passwordNewConfirm();
	
	@DefaultMessage("Path:")
	String path();
	
	@DefaultMessage("Properties")
	String properties();
	
	@DefaultMessage("Refine results to: ")
	String refineResults();
	
	@DefaultMessage("Save")
	String save();
	
	@DefaultMessage("Scale to fit")
	String scaleToFit();
	
	@DefaultMessage("Search")
	String search();
	
	@DefaultMessage("Search analysed data")
	String searchAnalysedData();
	
	@DefaultMessage("Search results for ''{0}''")
	String searchResultsFor(String containsText);
	
	@DefaultMessage("Select activity to display details ...")
	String selectActivity();
	
	@DefaultMessage("Select data source ...")
	String selectDataSource();
	
	@DefaultMessage("Show Analysis")
	String showAnalysis();
	
	@DefaultMessage("Show Data")
	String showData();
	
	@DefaultMessage("Show {0}")
	String showDataType(String dataType);
	
	@DefaultMessage("Show Details")
	String showDetails();
	
	@DefaultMessage("Show help text")
	String showHelpText();
	
	@DefaultMessage("Show in activity heat map")
	String showInActivityHeatMap();
	
	@DefaultMessage("Show key")
	String showKey();
	
	@DefaultMessage("source")
	String source();
	
	@DefaultMessage("sources")
	String sourceCount();
	
	@DefaultMessage("Start")
	String start();
	
	@DefaultMessage("start")
	String startTime();
	
	@DefaultMessage("Summary")
	String summary();
	
	@DefaultMessage("symbol")
	String symbol();
	
	@DefaultMessage("time")
	String time();
	
	@DefaultMessage("Features in {0} data")
	String timeSeriesFeaturesHeading(String dataType);
	
	@DefaultMessage("Please set {0} properties or select a feature to load data into the chart")
	String timeSeriesChartSelectToLoad(String dataType);
	
	@DefaultMessage("Total")
	String total();
	
	@DefaultMessage("type")
	String type();
	
	@DefaultMessage("Up one level")
	String upOneLevel();
	
	@DefaultMessage("Link")
	String urlLink();
	
	@DefaultMessage("value")
	String value();
	
	@DefaultMessage("Version {0} Build {1}")
	String versionBuild(String versionNumber, String buildNumber);
	
	@DefaultMessage("Welcome")
	String welcome();
	
	@DefaultMessage("Zoom in")
	String zoomIn();
	
	@DefaultMessage("Zoom out")
	String zoomOut();
	
	@DefaultMessage("Prelert - Error")
	String errorTitle();
	
	@DefaultMessage("Prelert - Information")
	String infoTitle();
	
	@DefaultMessage("Prelert - Warning")
	String warningTitle();
	
	@DefaultMessage("Failed to download application module. Please check that you still have network access.")
	String errorDownloadingModule();
	
	@DefaultMessage("Error exporting analysis data.")
	String errorExportingAnalysisData();
	
	@DefaultMessage("{0} is not a valid id.")
	String errorInvalidId(String id);
	
	@DefaultMessage("Error loading activity data.")
	String errorLoadingActivityData();
	
	@DefaultMessage("Error loading summary of analysed data.")
	String errorLoadingAnalysedData();

	@DefaultMessage("Error loading analysis data.")
	String errorLoadingAnalysisData();
	
	@DefaultMessage("Error loading information on data sources.")
	String errorLoadingDataSources();
	
	@DefaultMessage("Error loading page of data.")
	String errorLoadingPage();
	
	@DefaultMessage("Error loading evidence data.")
	String errorLoadingEvidenceData();
	
	@DefaultMessage("Error loading time series data.")
	String errorLoadingTimeSeriesData();
	
	@DefaultMessage("Error loading user data.")
	String errorLoadingUserData();
	
	@DefaultMessage("Error loading view configuration for {0} data.")
	String errorLoadingViewForType(String typeName);
	
	@DefaultMessage("Error loading views.")
	String errorLoadingViews();
	
	@DefaultMessage("No activity found containing a notification or time series feature with id {0}.")
	String errorNoActivityForId(int evidenceId);
	
	@DefaultMessage("No notification or time series feature found with id {0}.")
	String errorNoEvidenceForId(int evidenceId);
	
	@DefaultMessage("No metric path data found matching requested id {0}.")
	String errorNoMetricPathForId(int id);
	
	@DefaultMessage("Failed to get a response from the server.")
	String errorNoServerResponse();
	
	@DefaultMessage("No time series found containing a feature with id {0}.")
	String errorNoTimeSeriesForId(int evidenceId);
	
	@DefaultMessage("No view has been configured for data type: ")
	String errorNoViewForType();
	
	@DefaultMessage("Error obtaining analysis data from server.")
	String errorAnalysisData();
	
	@DefaultMessage("An error occurred saving the user password.")
	String errorSavingPassword();
	
	@DefaultMessage("#,##0.#")
	String shortDecimalFormat();
}
