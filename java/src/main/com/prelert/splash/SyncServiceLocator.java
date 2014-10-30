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

package com.prelert.splash;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.http.client.UrlBuilder;
import com.google.gwt.user.client.Window;

import com.prelert.client.URLParameterNames;


/**
 * Service locator for returning the appropriate URL to call when making a 
 * <i>synchronous</i> HTTP GET or POST request for data from a Java servlet component.
 * 
 * @author Pete Harverson
 */
public class SyncServiceLocator
{
	/**
	 * Returns the URL to call to export causality data to the specified file type.
	 * @param fileType export data file type - CSV or PDF are currently supported.
	 * @param evidenceId id of the notification or time series feature for which
	 * 		to export causality data.
	 * @param metricsTimeSpan time span, in seconds, that is used for calculating 
	 * 		metrics for probable causes that are features in time series.
	 * @param minTime the minimum time of data to be included in the export.
	 * @param maxTime the maximum time of data to be included in the export.
	 * @param timeZoneID ID of the time zone to use when outputting times to
	 * 		the export file.
	 * @return the full causality data export URL, including all query parameters.
	 */
	public static String getCausalityDataExportURL(String fileType, int evidenceId, 
			int metricsTimeSpan, Date minTime, Date maxTime, String timeZoneID)
	{	
		StringBuilder urlBuilder = new StringBuilder(GWT.getModuleBaseURL());
		urlBuilder.append("services/causalityDataExport?");
		
		append(urlBuilder, "fileType", fileType.toLowerCase());
		append(urlBuilder.append('&'), "evidenceId", evidenceId);
		append(urlBuilder.append('&'), "metricsTimeSpan", metricsTimeSpan);
		append(urlBuilder.append('&'), "minTime", minTime.getTime());
		append(urlBuilder.append('&'), "maxTime", maxTime.getTime());
		
		if (timeZoneID != null)
		{
			append(urlBuilder.append('&'), "timeZoneID", URL.encodeQueryString(timeZoneID));
		}
		
		return urlBuilder.toString();
	}
	
	
	/**
	 * Returns the URL to open the UI directly in a module displaying data for 
	 * a specific item of evidence.
	 * @param moduleId ID of the module to open.
	 * @param evidenceId id of the notification or time series feature to display.
	 * @return the URL for opening the UI at the specified module. 
	 */
	public static String getOpenInModuleURL(String moduleId, int evidenceId)
	{
		StringBuilder urlBuilder = new StringBuilder(GWT.getHostPageBaseURL());
		append(urlBuilder.append('?'), URLParameterNames.MODULE, moduleId);
		append(urlBuilder.append('&'), URLParameterNames.ID, evidenceId);
		
		return urlBuilder.toString();
	}
	
	
	/**
	 * Opens a new page showing the Prelert Contact page.
	 */
	public static void openPrelertContactPage()
	{
		Window.open("http://www.prelert.com/contact.html", "prlContact", "");
	}
	
	
	/**
	 * Opens a popup window showing details of how to contact Prelert to
	 * get technical support.
	 */
	public static void openSupportDetailsPopup()
	{
		Window.open(GWT.getHostPageBaseURL() + "contact_engineer.do", 
				"prlEngineer", "toolbar=no, location=no, directories=no," +
				"status=yes, menubar=no, scrollbars=no, resizable=yes, width=840, height=445");
	}
	
	
	/**
	 * Opens a link to the demo UI in a new tab or window (dependent on the user's 
	 * browser settings).
	 */
	public static void openDemoUI()
	{
		UrlBuilder urlBuilder = new UrlBuilder();
		urlBuilder.setHost(Window.Location.getHost());
		urlBuilder.setPath("prelert_demo");

		String port = Window.Location.getPort();
		if (port.isEmpty() == false)
		{
			urlBuilder.setPort(Integer.parseInt(port));
		}

		Window.open(urlBuilder.buildString(), "prlDemoUI", "");
	}
	
	
	/**
	 * Appends a parameter to a StringBuilder used for building up a URL.
	 * @param strBuilder StringBuilder to append to.
	 * @param parameterName name of the parameter to append.
	 * @param parameterValue value of the parameter to append.
	 * @return the StringBuilder with appended parameter.
	 */
	private static StringBuilder append(StringBuilder strBuilder, String parameterName, Object parameterValue)
	{
		return strBuilder.append(parameterName).append('=').append(parameterValue);
	}
	
}
