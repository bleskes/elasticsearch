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

package demo.app.client;

import com.google.gwt.i18n.client.Constants;


/**
 * Interface defining locale-sensitive constants used in client applications, 
 * such as labels for UI controls.
 * @author Pete Harverson
 */
public interface ClientConstants extends Constants
{
	@DefaultStringValue("Analysed Data")
	String analysedData();
	
	@DefaultStringValue("Diagnostics chart")
	String diagnosticsChart();
	
	@DefaultStringValue("Diagnostics for")
	String diagnosticsFor();
	
	@DefaultStringValue("features")
	String features();
	
	@DefaultStringValue("Filter: ")
	String fieldFilter();
	
	@DefaultStringValue("Show: ")
	String fieldShow();
	
	@DefaultStringValue("Time occurred: ")
	String fieldTimeOccurred();
	
	@DefaultStringValue("No data")
	String noData();
	
	@DefaultStringValue("-- All --")
	String optionAll();
	
	@DefaultStringValue("Show")
	String show();
	
	@DefaultStringValue("Time")
	String time();
	
	@DefaultStringValue("Show Probable Cause")
	String showProbableCause();
	
	@DefaultStringValue("pan left")
	String panLeftLink();
	
	@DefaultStringValue("pan right")
	String panRightLink();
	
	@DefaultStringValue("zoom in")
	String zoomInLink();
	
	@DefaultStringValue("zoom out")
	String zoomOutLink();
	
	@DefaultStringValue("Zoom in")
	String zoomInMenuItem();
	
	@DefaultStringValue("Zoom out")
	String zoomOutMenuItem();
	
	@DefaultStringValue("prl-logoff")
	String logoffLinkStylename();
	
	@DefaultStringValue("Prelert - Error")
	String errorTitle();
	
	@DefaultStringValue("Error retrieving probable cause data from server.")
	String errorProbCauseData();
	
	@DefaultStringValue("Failed to get a response from the server.")
	String errorEvidenceData();
	
	@DefaultStringValue("Error loading time series.")
	String errorLoadingTimeSeries();
	
	@DefaultStringValue("Error loading view for data type: ")
	String errorLoadingViewForType();
	
	@DefaultStringValue("Error loading views.")
	String errorLoadingViews();
	
	@DefaultStringValue("Failed to get a response from the server.")
	String errorNoServerResponse();
	
	@DefaultStringValue("No view has been configured for data type: ")
	String errorNoViewForType();
}
