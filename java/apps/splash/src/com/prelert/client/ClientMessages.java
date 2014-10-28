/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
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
	@DefaultMessage("Analysed Data")
	String analysedData();
	
	@DefaultMessage("All data types")
	String allDataTypes();
	
	@DefaultMessage("All sources")
	String allSources();
	
	@DefaultMessage("Anomaly threshold: ")
	String anomalyThreshold();
	
	@DefaultMessage("attribute")
	String attribute();
	
	@DefaultMessage("Start automatic refresh")
	String autoRefreshStart();
	
	@DefaultMessage("Pause automatic refresh")
	String autoRefreshPause();
	
	@DefaultMessage("category")
	String category();
	
	@DefaultMessage("Causality")
	String causality();
	
	@DefaultMessage("Notification data for {0}")
	String causalityNotificationsHeading(String description);
	
	@DefaultMessage("count")
	String count();
	
	@DefaultMessage("{0} data sources")
	String dataSourcesForType(String dataSourceType);
	
	@DefaultMessage("Data types")
	String dataTypes();
	
	@DefaultMessage("description")
	String description();
	
	@DefaultMessage("Details on incident at {0} : {1}")
	String detailsOnIncident(String formattedTime, String description);
	
	@DefaultMessage("Diagnostics chart")
	String diagnosticsChart();
	
	@DefaultMessage("Diagnostics for")
	String diagnosticsFor();
	
	@DefaultMessage("end")
	String endTime();
	
	@DefaultMessage("{0} id {1}")
	String evidenceDetailsHeading(String dataType, int evidenceId);
	
	@DefaultMessage("Explorer")
	String explorer();
	
	@DefaultMessage("Filter: ")
	String fieldFilter();
	
	@DefaultMessage("for")
	String forLower();
	
	@DefaultMessage("Show: ")
	String fieldShow();
	
	@DefaultMessage("Time occurred: ")
	String fieldTimeOccurred();
	
	@DefaultMessage("id")
	String id();
	
	@DefaultMessage("Incident Heat Map")
	String incidentTimelineHeading();
	
	@DefaultMessage("Incidents")
	String incidents();
	
	@DefaultMessage("{0} Items")   
	@PluralText({"one", "1 Item"})   
	String itemCount(@PluralCount int count);
	
	@DefaultMessage("magnitude")
	String magnitude();
	
	@DefaultMessage("metric")
	String metric();
	
	@DefaultMessage("No data")
	String noData();

	@DefaultMessage("-- All --")
	String optionAll();
	
	@DefaultMessage("pan left")
	String panLeftLink();
	
	@DefaultMessage("pan right")
	String panRightLink();
	
	@DefaultMessage("Refine results to: ")
	String refineResults();
	
	@DefaultMessage("Search")
	String search();
	
	@DefaultMessage("Search analysed data")
	String searchAnalysedData();
	
	@DefaultMessage("Search results for ''{0}''")
	String searchResultsFor(String containsText);
	
	@DefaultMessage("Select data source ...")
	String selectDataSource();
	
	@DefaultMessage("Select incident to display details ...")
	String selectIncident();
	
	@DefaultMessage("Show")
	String show();
	
	@DefaultMessage("Show Data")
	String showData();
	
	@DefaultMessage("Show Details")
	String showDetails();
	
	@DefaultMessage("show key")
	String showKey();
	
	@DefaultMessage("Show Probable Cause")
	String showProbableCause();
	
	@DefaultMessage("source")
	String source();
	
	@DefaultMessage("sources")
	String sourceCount();
	
	@DefaultMessage("start")
	String startTime();
	
	@DefaultMessage("Summary")
	String summary();
	
	@DefaultMessage("symbol")
	String symbol();
	
	@DefaultMessage("Time")
	String time();
	
	@DefaultMessage("Features in {0} data")
	String timeSeriesFeaturesHeading(String dataType);
	
	@DefaultMessage("Total")
	String total();
	
	@DefaultMessage("type")
	String type();
	
	@DefaultMessage("value")
	String value();
	
	@DefaultMessage("zoom in")
	String zoomInLink();
	
	@DefaultMessage("zoom out")
	String zoomOutLink();
	
	@DefaultMessage("Zoom in")
	String zoomInMenuItem();
	
	@DefaultMessage("Zoom out")
	String zoomOutMenuItem();
	
	@DefaultMessage("prl-logoff")
	String logoffLinkStylename();
	
	@DefaultMessage("Prelert - Error")
	String errorTitle();
	
	@DefaultMessage("Error loading summary of analysed data.")
	String errorLoadingAnalysedData();
	
	@DefaultMessage("Error loading evidence data.")
	String errorLoadingEvidenceData();
	
	@DefaultMessage("Error loading incident data.")
	String errorLoadingIncidentData();
	
	@DefaultMessage("Error loading time series.")
	String errorLoadingTimeSeries();
	
	@DefaultMessage("Error loading view for data type: ")
	String errorLoadingViewForType();
	
	@DefaultMessage("Error loading views.")
	String errorLoadingViews();
	
	@DefaultMessage("Failed to get a response from the server.")
	String errorNoServerResponse();
	
	@DefaultMessage("No view has been configured for data type: ")
	String errorNoViewForType();
	
	@DefaultMessage("Error retrieving probable cause data from server.")
	String errorProbCauseData();
	
	@DefaultMessage("#,##0.#")
	String shortDecimalFormat();
}
