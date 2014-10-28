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

package com.prelert.proxy.plugin.vantage;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.rmi.RemoteException;

import org.apache.axis.client.Stub;
import org.apache.log4j.Logger;

import adlex.delta.ws.DMIData;
import adlex.delta.ws.DMIData2;
import adlex.delta.ws.DMIService;
import adlex.delta.ws.DMIServiceError;
import adlex.delta.ws.DMIServiceServiceLocator;


/**
 * Encapsulates a connection to the CompuWare Vantage family of applications,
 * e.g. ClientVantage Agentless Monitoring (CVAM) or Vantage for Real User
 * Monitoring (VRUM).  Automatically reconnects when the connection is dropped.
 *
 * Since most of the calls are simply wrappers around the Vantage web services
 * API, refer to CVAMWebSvcGetStarted.pdf or VRUMWebSvcGetStarted.pdf for more
 * information about what the calls return.
 *
 * @author David Roberts
 */
public class VantageConnection
{
	static Logger s_Logger = Logger.getLogger(VantageConnection.class);

	private int m_Port;
	private String m_Host;
	private String m_Username;
	private String m_Password;

	/**
	 * The client we use to talk to Vantage via web services.  The DMIService
	 * class was generated from WSDL using Apache Axis.
	 */
	private DMIService m_DMIService;


	/**
	 * Construct a Vantage connection.
	 * @param host The host name or IP address of the machine where Vantage is
	 *             running.
	 * @param port The TCP port number to connect to Vantage on.
	 * @param username The user name to use to log into Vantage.
	 * @param password The password to use to log into Vantage.
	 */
	public VantageConnection(String host, int port, String username,
							String password) throws ConnectException
	{
		m_Host = host;
		m_Port = port;
		m_Username = username;
		m_Password = password;

		createClient();
	}


	/**
	 * Attempt to create a web services client to connect to Vantage.
	 */
	private void createClient() throws ConnectException
	{
		String connType = ((m_DMIService == null) ? "Connect" : "Reconnect");

		StringBuilder serviceURL = new StringBuilder("http://");
		try
		{
			serviceURL.append(m_Host);
			serviceURL.append(':');
			serviceURL.append(m_Port);
			serviceURL.append("/services/DMIService");

			DMIServiceServiceLocator locator = new DMIServiceServiceLocator();
			m_DMIService = locator.getDMIService(new URL(serviceURL.toString()));

			((Stub)m_DMIService).setUsername(m_Username);
			((Stub)m_DMIService).setPassword(m_Password);

			String serverUUID = m_DMIService.getServerUUID();
			s_Logger.info(connType + "ed to Vantage server with UUID " +
							serverUUID);
		}
		catch (Exception e)
		{
			s_Logger.error(connType + "ion to Vantage client failed", e);

			throw new ConnectException("Cannot connect to Vantage URL " +
										serviceURL);
		}
	}


	/**
	 * Get a list of the available Vantage applications.
	 * @return The list of available Vantage applications.  Each nested array
	 *         contains two values 0 = internal ID, 1 = human readable name.
	 */
	public String[][] getApplications() throws Exception
	{
		String[][] result = null;

		do
		{
			try
			{
				result = m_DMIService.getApplications();
			}
			catch (RemoteException e)
			{
				// We want exceptions related to the web service arguments
				// themselves to propagate, whilst exceptions related to the TCP
				// connection should trigger a reconnection attempt.
				reconnectOrRethrow(e);
			}
		}
		while (result == null);

		return result;
	}


	/**
	 * Get the current time on the Vantage server for a given application, view
	 * and resolution.
	 * @param appId Application ID (as returned by getApplications()).
	 * @param viewId View ID (as returned by getDataViews()).
	 * @param resolution Data resolution ID (as returned by getResolutions()).
	 * @return The current time on the Vantage server measured in milliseconds
	 *         since the epoch.
	 */
	public long getCurrentTime(String appId, String viewId, String resolution)
																throws Exception
	{
		long result = -1;

		do
		{
			try
			{
				result = m_DMIService.getCurrentTime(appId, viewId, resolution);
			}
			catch (RemoteException e)
			{
				// We want exceptions related to the web service arguments
				// themselves to propagate, whilst exceptions related to the TCP
				// connection should trigger a reconnection attempt.
				reconnectOrRethrow(e);
			}
		}
		while (result < 0);

		return result;
	}


	/**
	 * Get a list of the available Vantage data sources for a given application
	 * and view.
	 * @param appId Application ID (as returned by getApplications()).
	 * @param viewId View ID (as returned by getDataViews()).
	 * @return The list of available Vantage data sources.  Each nested array
	 *         contains two values 0 = internal ID, 1 = human readable name.
	 */
	public String[][] getDataSources(String appId, String viewId)
																throws Exception
	{
		String[][] result = null;

		do
		{
			try
			{
				result = m_DMIService.getDataSources(appId, viewId);
			}
			catch (RemoteException e)
			{
				// We want exceptions related to the web service arguments
				// themselves to propagate, whilst exceptions related to the TCP
				// connection should trigger a reconnection attempt.
				reconnectOrRethrow(e);
			}
		}
		while (result == null);

		return result;
	}


	/**
	 * Get a list of the available Vantage views for a given application.
	 * @param appId Application ID (as returned by getApplications()).
	 * @return The list of available Vantage views.  Each nested array
	 *         contains two values 0 = internal ID, 1 = human readable name.
	 */
	public String[][] getDataViews(String appId) throws Exception
	{
		String[][] result = null;

		do
		{
			try
			{
				result = m_DMIService.getDataViews(appId);
			}
			catch (RemoteException e)
			{
				// We want exceptions related to the web service arguments
				// themselves to propagate, whilst exceptions related to the TCP
				// connection should trigger a reconnection attempt.
				reconnectOrRethrow(e);
			}
		}
		while (result == null);

		return result;
	}


	/**
	 * Get a list of the available dimensions for a given application, view and
	 * resolution.
	 * @param appId Application ID (as returned by getApplications()).
	 * @param viewId View ID (as returned by getDataViews()).
	 * @param resolution Data resolution ID (as returned by getResolutions()).
	 * @return The list of available dimensions.  Each nested array contains
	 *         two values 0 = internal ID, 1 = human readable name.
	 */
	public String[][] getDimensions(String appId, String viewId,
									String resolution) throws Exception
	{
		String[][] result = null;

		do
		{
			try
			{
				result = m_DMIService.getDimensions(appId, viewId, resolution);
			}
			catch (RemoteException e)
			{
				// We want exceptions related to the web service arguments
				// themselves to propagate, whilst exceptions related to the TCP
				// connection should trigger a reconnection attempt.
				reconnectOrRethrow(e);
			}
		}
		while (result == null);

		return result;
	}


	/**
	 * Get a list of possible values for a given dimension.
	 * @param appId Application ID (as returned by getApplications()).
	 * @param viewId View ID (as returned by getDataViews()).
	 * @param dimId Dimension ID (as returned by getDimensions()).
	 * @param dataSourceId Data source ID (as returned by getDataSources()).
	 * @param quant Maximum number of values to return (0 or null implies all
	 *              values).
	 * @param filter Filter expression to narrow dimension value selection.  You
	 *               can use wildcard characters "?" and "*" and combine filter
	 *               expressions with the pipe character "|" interpreted as
	 *               logical OR.
	 * @return The list of possible dimension values.
	 */
	public String[] getDimensionValues(String appId, String viewId,
										String dimId, String dataSourceId,
										Integer quant, String filter)
																throws Exception
	{
		String[] result = null;

		do
		{
			try
			{
				result = m_DMIService.getDimensionValues(appId, viewId, dimId,
															dataSourceId, quant,
															filter);
			}
			catch (RemoteException e)
			{
				// We want exceptions related to the web service arguments
				// themselves to propagate, whilst exceptions related to the TCP
				// connection should trigger a reconnection attempt.
				reconnectOrRethrow(e);
			}
		}
		while (result == null);

		return result;
	}


	/**
	 * Get a list of the available Vantage applications.  (The difference
	 * between <code>getDMIData</code> and <code>getDMIData2</code> is that the
	 * latter method returns an object that contains GUI labels and units of
	 * measure for returned data.)
	 * @param appId Application ID (as returned by getApplications()).
	 * @param viewId View ID (as returned by getDataViews()).
	 * @param dataSourceId Data source ID (as returned by getDataSources()).
	 * @param dimensionIds Dimensions IDs to return as output columns.
	 * @param metricIds IDs of metrics to return in the output.
	 * @param dimFilters Array of dimension filters.  The nested array elements
	 *                   are 0 = dimension ID, 1 = filter condition, 2 (if
	 *                   present) = filter negation trigger.
	 * @param metricFilters Array of metric filters.  The nested array elements
	 *                      are 0 = metric ID, 1 = operator, 2 = value, 3 (if
	 *                      present) = apply to results.
	 * @param sort Sort order.  Outer array should only contain one element.
	 *             The nested array elements are 0 = metric or dimension ID, 1
	 *             (if present) = ASC/DESC.
	 * @param top Number of rows to return.  0 means everything.  null or
	 *            negative means 1000.
	 * @param resolution Data resolution ID (as returned by getResolutions()).
	 * @param timePeriod Period size to be covered by the report - set to null
	 *                   if using timeBegin and timeEnd.
	 * @param numberOfPeriods Number of periods of size timePeriod - set to null
	 *                        if using timeBegin and timeEnd.
	 * @param timeBegin Start time for data (measured in milliseconds since the
	 *                  epoch) - set to null if using timePeriod and
	 *                  numberOfPeriods.
	 * @param timeEnd End time for data (measured in milliseconds since the
	 *                epoch) - set to null if using timePeriod and
	 *                numberOfPeriods.
	 * @param timeout How long (in milliseconds) to wait for a response.
	 * @return The results of the query wrapped in a <code>DMIData</code>
	 *         object.
	 */
	public DMIData getDMIData(String appId, String viewId, String dataSourceId,
								String[] dimensionIds, String[] metricIds,
								String[][] dimFilters, String[][] metricFilters,
								String[][] sort, Integer top, String resolution,
								String timePeriod, Integer numberOfPeriods,
								Long timeBegin, Long timeEnd, Long timeout)
																throws Exception
	{
		DMIData result = null;

		do
		{
			try
			{
				result = m_DMIService.getDMIData(appId, viewId, dataSourceId,
													dimensionIds, metricIds,
													dimFilters, metricFilters,
													sort, top, resolution,
													timePeriod, numberOfPeriods,
													timeBegin, timeEnd,
													timeout);
			}
			catch (RemoteException e)
			{
				// We want exceptions related to the web service arguments
				// themselves to propagate, whilst exceptions related to the TCP
				// connection should trigger a reconnection attempt.
				reconnectOrRethrow(e);
			}
		}
		while (result == null);

		logDMIErrors(result);

		return result;
	}


	/**
	 * Get a list of the available Vantage applications.  (The difference
	 * between <code>getDMIData</code> and <code>getDMIData2</code> is that the
	 * latter method returns an object that contains GUI labels and units of
	 * measure for returned data.)
	 * @param appId Application ID (as returned by getApplications()).
	 * @param viewId View ID (as returned by getDataViews()).
	 * @param dataSourceId Data source ID (as returned by getDataSources()).
	 * @param dimensionIds Dimensions IDs to return as output columns.
	 * @param metricIds IDs of metrics to return in the output.
	 * @param dimFilters Array of dimension filters.  The nested array elements
	 *                   are 0 = dimension ID, 1 = filter condition, 2 (if
	 *                   present) = filter negation trigger.
	 * @param metricFilters Array of metric filters.  The nested array elements
	 *                      are 0 = metric ID, 1 = operator, 2 = value, 3 (if
	 *                      present) = apply to results.
	 * @param sort Sort order.  Outer array should only contain one element.
	 *             The nested array elements are 0 = metric or dimension ID, 1
	 *             (if present) = ASC/DESC.
	 * @param top Number of rows to return.  0 means everything.  null or
	 *            negative means 1000.
	 * @param resolution Data resolution ID (as returned by getResolutions()).
	 * @param timePeriod Period size to be covered by the report - set to null
	 *                   if using timeBegin and timeEnd.
	 * @param numberOfPeriods Number of periods of size timePeriod - set to null
	 *                        if using timeBegin and timeEnd.
	 * @param timeBegin Start time for data (measured in milliseconds since the
	 *                  epoch) - set to null if using timePeriod and
	 *                  numberOfPeriods.
	 * @param timeEnd End time for data (measured in milliseconds since the
	 *                epoch) - set to null if using timePeriod and
	 *                numberOfPeriods.
	 * @param timeout How long (in milliseconds) to wait for a response.
	 * @return The results of the query wrapped in a <code>DMIData2</code>
	 *         object.
	 */
	public DMIData2 getDMIData2(String appId, String viewId, String dataSourceId,
								String[] dimensionIds, String[] metricIds,
								String[][] dimFilters, String[][] metricFilters,
								String[][] sort, Integer top, String resolution,
								String timePeriod, Integer numberOfPeriods,
								Long timeBegin, Long timeEnd, Long timeout)
																throws Exception
	{
		DMIData2 result = null;

		do
		{
			try
			{
				result = m_DMIService.getDMIData2(appId, viewId, dataSourceId,
													dimensionIds, metricIds,
													dimFilters, metricFilters,
													sort, top, resolution,
													timePeriod, numberOfPeriods,
													timeBegin, timeEnd,
													timeout);
			}
			catch (RemoteException e)
			{
				// We want exceptions related to the web service arguments
				// themselves to propagate, whilst exceptions related to the TCP
				// connection should trigger a reconnection attempt.
				reconnectOrRethrow(e);
			}
		}
		while (result == null);

		logDMIErrors(result);

		return result;
	}


	/**
	 * Get a the time of the last sample for a given resolution.
	 * @param appId Application ID (as returned by getApplications()).
	 * @param viewId View ID (as returned by getDataViews()).
	 * @param resolution Data resolution ID (as returned by getResolutions()).
	 * @return The last sample time for the given resolution on the Vantage
	 *         server measured in milliseconds since the epoch.
	 */
	public long getLastSampleTime(String appId, String viewId,
									String resolution) throws Exception
	{
		long result = -1;

		do
		{
			try
			{
				result = m_DMIService.getLastSampleTime(appId, viewId,
														resolution);
			}
			catch (RemoteException e)
			{
				// We want exceptions related to the web service arguments
				// themselves to propagate, whilst exceptions related to the TCP
				// connection should trigger a reconnection attempt.
				reconnectOrRethrow(e);
			}
		}
		while (result < 0);

		return result;
	}


	/**
	 * Get a list of the available Vantage metrics.
	 * @param appId Application ID (as returned by getApplications()).
	 * @param viewId View ID (as returned by getDataViews()).
	 * @param resolution Data resolution ID (as returned by getResolutions()).
	 * @return The list of available Vantage metrics.  Each nested array
	 *         contains two values 0 = internal ID, 1 = human readable name.
	 */
	public String[][] getMetrics(String appId, String viewId, String resolution)
																throws Exception
	{
		String[][] result = null;

		do
		{
			try
			{
				result = m_DMIService.getMetrics(appId, viewId, resolution);
			}
			catch (RemoteException e)
			{
				// We want exceptions related to the web service arguments
				// themselves to propagate, whilst exceptions related to the TCP
				// connection should trigger a reconnection attempt.
				reconnectOrRethrow(e);
			}
		}
		while (result == null);

		return result;
	}


	/**
	 * Get a list of the available Vantage resolutions.
	 * @param appId Application ID (as returned by getApplications()).
	 * @param viewId View ID (as returned by getDataViews()).
	 * @return The list of available Vantage resolutions.
	 */
	public String[] getResolutions(String appId, String viewId) throws Exception
	{
		String[] result = null;

		do
		{
			try
			{
				result = m_DMIService.getResolutions(appId, viewId);
			}
			catch (RemoteException e)
			{
				// We want exceptions related to the web service arguments
				// themselves to propagate, whilst exceptions related to the TCP
				// connection should trigger a reconnection attempt.
				reconnectOrRethrow(e);
			}
		}
		while (result == null);

		return result;
	}


	/**
	 * Log any errors contained within a <code>DMIData</code> or
	 * <code>DMIData2</code> object.
	 * @param data A <code>DMIData</code> or <code>DMIData2</code> object to
	 *             check for errors.
	 * @return Were any errors found?
	 */
	private static <T extends DMIData> boolean logDMIErrors(T data)
	{
		DMIServiceError[] dmiServiceError = data.getDmiServiceError();
		if (dmiServiceError == null)
		{
			return false;
		}

		for (DMIServiceError error : dmiServiceError)
		{
			String[] descriptions = error.getErrorDescriptions();
			if (descriptions != null)
			{
				for (String description : descriptions)
				{
					s_Logger.error(description);
				}
			}

			descriptions = error.getWarningDescriptions();
			if (descriptions != null)
			{
				for (String description : descriptions)
				{
					s_Logger.warn(description);
				}
			}

			descriptions = error.getInfoDescriptions();
			if (descriptions != null)
			{
				for (String description : descriptions)
				{
					s_Logger.info(description);
				}
			}
		}

		return true;
	}


	/**
	 * Check the underlying cause of a <code>RemoteException</code>.  If it was
	 * caused by a problem with the TCP connection, try to create a new
	 * connection to the server.  If there is a nested <code>Exception</code>,
	 * propagate that nested <code>Exception</code>.  If the cause is not known
	 * or not an <code>Exception</code> (e.g. if it's a problem with the JVM
	 * itself), propagate the original <code>RemoteException</code>.
	 * @param re A <code>RemoteException</code> to examine.
	 */
	private void reconnectOrRethrow(RemoteException re) throws Exception
	{
		Throwable throwable = re.getCause();

		if (throwable instanceof IOException)
		{
			// Nested exception involves the TCP connection
			s_Logger.error("Disconnected from Vantage", (IOException)throwable);
			createClient();
		}
		else if (throwable instanceof Exception)
		{
			// Nested exception not TCP related, so propagate it
			s_Logger.error(re);
			throw (Exception)throwable;
		}
		else
		{
			// This also caters for the case where throwable is null
			throw re;
		}
	}

}
