/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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

package com.prelert.dao.proxy;

import java.rmi.AccessException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
 
import org.apache.log4j.Logger;

import com.prelert.dao.TimeSeriesDAO;
import com.prelert.data.Attribute;
import com.prelert.data.DataSourceType;
import com.prelert.data.Evidence;
import com.prelert.data.MetricPath;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.proxy.dao.RemoteObjectFactoryDAO;
import com.prelert.proxy.dao.RemoteTimeSeriesDAO;

/**
 * Implementation for RMI (Remote Method Invocation) of the TimeSeriesDAO 
 * interface. The class makes calls through RMI to a remote server which 
 * returns time series data.
 */
public class TimeSeriesProxyDAO extends RemoteProxyDAO implements TimeSeriesDAO
{
	static Logger s_Logger = Logger.getLogger(TimeSeriesProxyDAO.class);

	private RemoteTimeSeriesDAO m_RemoteDAO;

	public TimeSeriesProxyDAO()
	{
		m_RemoteDAO = null;
	}


	/**
	 * Is time series aggregation supported for the given data type, i.e.
	 * querying for time series points without specifying a value for every
	 * possible attribute?
	 * @param dataType The name of the data type, e.g. system_udp or
	 *                 p2psmon_servers.
	 * @return true if aggregation is supported; false if it's not.
	 */
	@Override
	public boolean isAggregationSupported(String dataType)
	{
		s_Logger.debug("isAggregationSupported RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().isAggregationSupported(dataType);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteTimeSeriesDAO";
					s_Logger.error("isAggregationSupported(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error checking whether aggregation is supported through RemoteTimeSeriesDAO";
				s_Logger.error("isAggregationSupported(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs getting the page of
	 * 		evidence data remotely through the Proxy.
	 */
	@Override
	public List<String> getSourcesOrderByName(DataSourceType dataSourceType)
	{
		s_Logger.debug("getSourcesOrderByName RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getSourcesOrderByName(dataSourceType);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteTimeSeriesDAO";
					s_Logger.error("getSourcesOrderByName(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting list of sources through RemoteTimeSeriesDAO";
				s_Logger.error("getSourcesOrderByName(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs getting the page of
	 * 		evidence data remotely through the Proxy.
	 */
	@Override
	public List<String> getSourcesOrderByCount(DataSourceType dataSourceType)
	{
		s_Logger.debug("getSourcesOrderByCount RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getSourcesOrderByName(dataSourceType);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteTimeSeriesDAO";
					s_Logger.error("getSourcesOrderByCount(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting list of sources through RemoteTimeSeriesDAO";
				s_Logger.error("getSourcesOrderByCount(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs getting the page of
	 * 		evidence data remotely through the Proxy.
	 */
	@Override
	public List<String> getAttributeNames(String dataType)
	{
		s_Logger.debug("getAttributeNames RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getAttributeNames(dataType);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteTimeSeriesDAO";
					s_Logger.error("getAttributeNames(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting attributes for " + dataType + " through RemoteTimeSeriesDAO";
				s_Logger.error("getAttributeNames(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs getting the page of
	 * 		evidence data remotely through the Proxy.
	 */
	@Override
	public List<String>	getAttributeValues(String dataType, String attributeName, String source)
	{
		s_Logger.debug("getAttributeValues RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getAttributeValues(dataType, attributeName, source);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteTimeSeriesDAO";
					s_Logger.error("getAttributeValues(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting attribute values for " + attributeName + " through RemoteTimeSeriesDAO";
				s_Logger.error("getAttributeValues(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs getting the page of
	 * 		evidence data remotely through the Proxy.
	 */
	@Override
	public List<String> getMetrics(String dataType)
	{
		s_Logger.debug("getMetrics RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getMetrics(dataType);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteTimeSeriesDAO";
					s_Logger.error("getMetrics(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting metrics for " + dataType + " through RemoteTimeSeriesDAO";
				s_Logger.error("getMetrics(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}

	/**
	 * @throws ProxyDataAccessException if an error occurs getting the page of
	 * 		evidence data remotely through the Proxy.
	 */
	@Override
	public List<TimeSeriesDataPoint> getDataPoints(String dataType, String metric, 
			Date minTime, Date maxTime, String source, 
			List<Attribute> attributes, boolean includeFeatures)
	{
		s_Logger.debug("getDataPoints RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getDataPoints(dataType, metric, minTime,
						maxTime, source, attributes, includeFeatures);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteTimeSeriesDAO";
					s_Logger.error("getDataPoints(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting data points for " + dataType + " through RemoteTimeSeriesDAO";
				s_Logger.error("getDataPoints(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs getting the page of
	 * 		evidence data remotely through the Proxy.
	 */
	@Override
	public List<TimeSeriesDataPoint> getDataPointsForDay(String dataType,
			String metric, Date minTime, Date maxTime, String source,
			List<Attribute> attributes)
	{
		s_Logger.debug("getDataPointsForDay RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getDataPointsForDay(dataType, metric,
						minTime, maxTime, source, attributes);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteTimeSeriesDAO";
					s_Logger.error("getDataPointsForDay(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting data points for " + dataType + " through RemoteTimeSeriesDAO";
				s_Logger.error("getDataPointsForDay(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs getting the page of
	 * 		evidence data remotely through the Proxy.
	 */
	@Override
	public List<TimeSeriesDataPoint> getDataPointsForWeek(String dataType, String metric, 
			Date minTime, Date maxTime, String source, List<Attribute> attributes)
	{
		s_Logger.debug("getDataPointsForWeek RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getDataPointsForWeek(dataType, metric, minTime, maxTime, source, attributes);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteTimeSeriesDAO";
					s_Logger.error("getDataPointsForWeek(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting data points for " + dataType + " through RemoteTimeSeriesDAO";
				s_Logger.error("getDataPointsForWeek(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs getting the page of
	 * 		evidence data remotely through the Proxy.
	 */
	@Override
	public List<TimeSeriesDataPoint> getDataPointsForTimeSpan(String dataType, String metric, 
			Date minTime, Date maxTime, String source, 
			List<Attribute> attributes, boolean includeFeatures)
	{
		s_Logger.debug("getDataPointsForTimeSpan RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getDataPointsForTimeSpan(dataType, metric, minTime, maxTime, source, attributes, includeFeatures);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteTimeSeriesDAO";
					s_Logger.error("getDataPointsForTimeSpan(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting data points for " + dataType + " through RemoteTimeSeriesDAO";
				s_Logger.error("getDataPointsForTimeSpan(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs getting the page of
	 * 		evidence data remotely through the Proxy.
	 */
	@Override
	public List<TimeSeriesDataPoint> getDataPointsRaw(int timeSeriesId,
			Date minTime, Date maxTime)
	{
		s_Logger.debug("getDataPointsRaw RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getDataPointsRaw(timeSeriesId, minTime, maxTime);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteTimeSeriesDAO";
					s_Logger.error("getDataPointsRaw(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting data points for time series id " + timeSeriesId +
								" through RemoteTimeSeriesDAO";
				s_Logger.error("getDataPointsRaw(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs getting the page of
	 * 		evidence data remotely through the Proxy.
	 */
	@Override
	public Date getLatestTime(String dataType, String source)
	{
		s_Logger.debug("getLatestTime RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getLatestTime(dataType, source);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteTimeSeriesDAO";
					s_Logger.error("getLatestTime(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting latest time for " + dataType +
					" through RemoteTimeSeriesDAO";
				s_Logger.error("getLatestTime(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * Returns the data model for a time series feature with the specified id.
	 * @param id the unique identifier for the time series feature.
	 * @return full data model for the evidence item with the specified id.
	 * @throws ProxyDataAccessException if an error occurs getting the page of
	 * 		evidence data remotely through the Proxy.
	 */
	@Override
	public Evidence getFeature(int id)
	{
		s_Logger.debug("getFeature RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getFeature(id);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteTimeSeriesDAO";
					s_Logger.error("getFeature(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting feature with ID " + id +
					" through RemoteTimeSeriesDAO";
				s_Logger.error("getFeature(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * Returns the config of the time series corresponding to a specified
	 * feature id.
	 * @param id the unique identifier for the time series feature.
	 * @return config of the corresponding time series (or null if the
	 *         input id wasn't found).
	 */
	@Override
	public TimeSeriesConfig getTimeSeriesFromFeature(int id)
	{
		s_Logger.debug("getTimeSeriesFromFeature RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getTimeSeriesFromFeature(id);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteTimeSeriesDAO";
					s_Logger.error("getTimeSeriesFromFeature(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting time series config for feature with ID " +
					id + " through RemoteTimeSeriesDAO";
				s_Logger.error("getTimeSeriesFromFeature(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	/**
	 * Returns the unique metric path for the Time series identified 
	 * by the parameter <code>id</code>. 
	 * If the metric path cannot be found <code>null</code> is
	 * returned. 
	 * 
	 * @param id - The unique evidence id.
	 * @return MetricPath or <code>null</code>
	 */
	@Override
	public MetricPath getMetricPathFromTimeSeriesId(int id)
	{
		String debug= "getMetricPathFromTimeSeriesId({0}) RMI call";
		debug = MessageFormat.format(debug, id);
		s_Logger.debug(debug);

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getMetricPathFromTimeSeriesId(id);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteTimeSeriesDAO";
					s_Logger.error("getMetricPathFromTimeSeriesId: " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting metric path for time series with id = " + id +
									" through RemoteTimeSeriesDAO";
				s_Logger.error("getMetricPathFromTimeSeriesId(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * Returns the time series ID corresponding to a given external key.
	 * If no such time series ID is found <code>null</code> is returned.
	 *
	 * @param dataType The data type to which the external key belongs.
	 * @param externalKey The external key to be looked up.
	 * @return Time series ID or <code>null</code>
	 */
	@Override
	public Integer getTimeSeriesIdFromExternalKey(String dataType, String externalKey)
	{
		String debug= "getTimeSeriesIdFromExternalKey({0}, {1}) RMI call";
		debug = MessageFormat.format(debug, dataType, externalKey);
		s_Logger.debug(debug);

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getTimeSeriesIdFromExternalKey(dataType, externalKey);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteTimeSeriesDAO";
					s_Logger.error("getTimeSeriesIdFromExternalKey: " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting time series id from external key " + externalKey +
									" through RemoteTimeSeriesDAO";
				s_Logger.error("getTimeSeriesIdFromExternalKey(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * Returns the external key corresponding to a given time series ID.
	 * If the time series ID does not correspond to an external time series,
	 * <code>null</code> is returned.
	 *
	 * @param timeSeriesId The time series ID to look up.
	 * @return An external key or <code>null</code>
	 */
	@Override
	public String getExternalKeyFromTimeSeriesId(int timeSeriesId)
	{
		String debug= "getExternalKeyFromTimeSeriesId({0}) RMI call";
		debug = MessageFormat.format(debug, timeSeriesId);
		s_Logger.debug(debug);

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getExternalKeyFromTimeSeriesId(timeSeriesId);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteTimeSeriesDAO";
					s_Logger.error("getExternalKeyFromTimeSeriesId: " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting external key from time series id " + timeSeriesId +
									" through RemoteTimeSeriesDAO";
				s_Logger.error("getExternalKeyFromTimeSeriesId(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * Returns a valid remote object or throws and exception.
	 * 
	 * This uses RMI to connect to a <code>RemoteObjectFactoryDAO</code> and
	 * queries it for the remote data object.
	 * 
	 * @return A valid remote object.
	 * @throws AccessException
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	private RemoteTimeSeriesDAO getRemoteDAO() throws AccessException, RemoteException, NotBoundException
	{
		if (m_RemoteDAO != null)
		{
			return m_RemoteDAO;
		}

		RemoteObjectFactoryDAO factory = getRemoteFactory();

		m_RemoteDAO = factory.getTimeSeriesDAO(getOriginatorName());
		return m_RemoteDAO;
	}


	/**
	 * Sets the m_RemoteDAO member to null. 
	 * This will force a new connection to any calls to <code>getRemoteDAO</code>
	 * to try to make a new connection.
	 */
	private void resetRemoteDAO()
	{
		m_RemoteDAO = null;
	}

	/**
	 * This function is only supported by the database DAOs for now.
	 */
	@Override
	public String getExternalKeyFromTimeSeriesDetails(String datatype,
			String metric, String source, List<Attribute> attributes) 
	{
		throw new UnsupportedOperationException();
	}
}
