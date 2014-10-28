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
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.dao.IncidentDAO;
import com.prelert.data.CausalityAggregate;
import com.prelert.data.Incident;
import com.prelert.data.MetricTreeNode;
import com.prelert.proxy.dao.RemoteIncidentDAO;
import com.prelert.proxy.dao.RemoteObjectFactoryDAO;


/**
 * Implementation for RMI (Remote Method Invocation) of the IncidentDAO 
 * interface. The class makes calls through RMI to a remote server which 
 * returns incident data.
 */
public class IncidentProxyDAO extends RemoteProxyDAO implements IncidentDAO
{
	static Logger s_Logger = Logger.getLogger(IncidentProxyDAO.class);

	private RemoteIncidentDAO m_RemoteDAO;

	public IncidentProxyDAO()
	{
		m_RemoteDAO = null;
	}

	
	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the list of
	 * 		incidents remotely through the Proxy.
	 */
	@Override
	public List<Incident> getIncidents(Date minTime, Date maxTime, int anomalyThreshold)
	{
		s_Logger.debug("getIncidents() RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getIncidents(minTime, maxTime, anomalyThreshold);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteIncidentDAO";
					s_Logger.error("getIncidents(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting incidents through RemoteIncidentDAO";
				s_Logger.error("getIncidentsAdaptive(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	

	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the list of
	 * 		incidents remotely through the Proxy.
	 */
	@Override
	public List<Incident> getIncidentsAdaptive(Date minTime, Date maxTime, int anomalyThreshold)
	{
		s_Logger.debug("getIncidentsAdaptive() RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getIncidentsAdaptive(minTime, maxTime, anomalyThreshold);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteIncidentDAO";
					s_Logger.error("getIncidentsAdaptive(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting incidents through RemoteIncidentDAO";
				s_Logger.error("getIncidentsAdaptive(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	@Override
	public List<Incident> getIncidentsInTimeRange(Date minTime,
			boolean minTimeIsOpen, Date maxTime, boolean maxTimeIsOpen,
			Date minFirstTime, boolean minFirstTimeIsOpen, Date maxFirstTime,
			boolean maxFirstTimeIsOpen, Date minLastTime,
			boolean minLastTimeIsOpen, Date maxLastTime,
			boolean maxLastTimeIsOpen, Date minUpdateTime,
			boolean minUpdateTimeIsOpen, Date maxUpdateTime,
			boolean maxUpdateTimeIsOpen, int anomalyThreshold,
			String metricPath, String likeMetricPath,
			String escapeChar) 
	{
		s_Logger.debug("getIncidentsInTimeRange() RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getIncidentsInTimeRange(minTime, minTimeIsOpen, 
						maxTime, maxTimeIsOpen,
						minFirstTime, minFirstTimeIsOpen, 
						maxFirstTime, maxFirstTimeIsOpen, 
						minLastTime, minLastTimeIsOpen,
						maxLastTime, maxLastTimeIsOpen, 
						minUpdateTime, minUpdateTimeIsOpen, 
						maxUpdateTime, maxUpdateTimeIsOpen, anomalyThreshold,
						metricPath, likeMetricPath, escapeChar);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteIncidentDAO";
					s_Logger.error("getIncidentsInTimeRange(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting incidents through RemoteIncidentDAO";
				s_Logger.error("getIncidentsInTimeRange(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	@Override
    public List<Incident> getFirstPage(int anomalyThreshold, int pageSize)
    {
		s_Logger.debug("getFirstPage() RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getFirstPage(anomalyThreshold, pageSize);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteIncidentDAO";
					s_Logger.error("getFirstPage(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting incidents through RemoteIncidentDAO";
				s_Logger.error("getFirstPage(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
    }


    @Override
    public List<Incident> getLastPage(int anomalyThreshold, int pageSize)
    {
    	s_Logger.debug("getLastPage() RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getLastPage(anomalyThreshold, pageSize);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteIncidentDAO";
					s_Logger.error("getLastPage(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting incidents through RemoteIncidentDAO";
				s_Logger.error("getLastPage(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
    }


    @Override
    public List<Incident> getNextPage(Date bottomRowTime,
            int bottomRowEvidenceId, int anomalyThreshold, int pageSize)
    {
    	s_Logger.debug("getNextPage() RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getNextPage(bottomRowTime, bottomRowEvidenceId, 
						anomalyThreshold, pageSize);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteIncidentDAO";
					s_Logger.error("getNextPage(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting incidents through RemoteIncidentDAO";
				s_Logger.error("getNextPage(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
    }


    @Override
    public List<Incident> getPreviousPage(Date topRowTime,
            int topRowEvidenceId, int anomalyThreshold, int pageSize)
    {
    	s_Logger.debug("getPreviousPage() RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getPreviousPage(topRowTime, topRowEvidenceId, 
						anomalyThreshold, pageSize);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteIncidentDAO";
					s_Logger.error("getPreviousPage(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting incidents through RemoteIncidentDAO";
				s_Logger.error("getPreviousPage(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
    }


	@Override
    public List<Incident> getAtTime(Date time, int anomalyThreshold,
									int pageSize, boolean orderAscending)
    {
		s_Logger.debug("getAtTime() RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getAtTime(time, anomalyThreshold,
												pageSize, orderAscending);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteIncidentDAO";
					s_Logger.error("getAtTime(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting incidents through RemoteIncidentDAO";
				s_Logger.error("getAtTime(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
    }


	/**
	 * Returns the date/time of the earliest incident in the database.
	 * @return date/time of earliest incident.
	 */
	@Override
	public Date getEarliestTime()
	{
		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getEarliestTime();
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteIncidentDAO";
					s_Logger.error("getEarliestTime(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting time of earliest incident through RemoteIncidentDAO";
				s_Logger.error("getEarliestTime(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * Returns the date/time of the latest incident in the database.
	 * @return date/time of latest incident.
	 */
	@Override
	public Date getLatestTime()
	{
		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getLatestTime();
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteIncidentDAO";
					s_Logger.error("getLatestTime(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting time of latest incident through RemoteIncidentDAO";
				s_Logger.error("getLatestTime(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the attribute
	 * 		names remotely through the Proxy.
	 */
	@Override
	public List<MetricTreeNode> getIncidentMetricPathNodes(int evidenceId)
    {
		s_Logger.debug("getIncidentMetricPathNodes() RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getIncidentMetricPathNodes(evidenceId);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteIncidentDAO";
					s_Logger.error("getIncidentMetricPathNodes(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting incident metric path names through RemoteIncidentDAO";
				s_Logger.error("getIncidentMetricPathNodes(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
    }


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the attribute
	 * 		names remotely through the Proxy.
	 */
	@Override
    public List<String> getIncidentAttributeNames(int evidenceId)
    {
		s_Logger.debug("getIncidentAttributeNames() RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getIncidentAttributeNames(evidenceId);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteIncidentDAO";
					s_Logger.error("getIncidentAttributeNames(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting incident attribute names through RemoteIncidentDAO";
				s_Logger.error("getIncidentAttributeNames(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
    }

	
	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the attribute
	 * 		values remotely through the Proxy.
	 */
    @Override
    public List<String> getIncidentAttributeValues(int evidenceId, String attributeName)
    {
		s_Logger.debug("getIncidentAttributeValues() RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getIncidentAttributeValues(evidenceId, attributeName);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteIncidentDAO";
					s_Logger.error("getIncidentAttributeValues(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting incident attribute values through RemoteIncidentDAO";
				s_Logger.error("getIncidentAttributeValues(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
    }


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the incident 
	 * 		summary remotely through the Proxy.
	 */
    @Override
    public List<CausalityAggregate> getIncidentSummary(int evidenceId,
            String aggregateBy, List<String> groupingAttributes)
    {
		s_Logger.debug("getIncidentSummary() RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getIncidentSummary(evidenceId,
														aggregateBy,
														groupingAttributes);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteIncidentDAO";
					s_Logger.error("getIncidentSummary(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting incident summary through RemoteIncidentDAO";
				s_Logger.error("getIncidentSummary(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
    }
    
    
    @Override
    public Incident getIncidentForId(int evidenceId)
    {
    	s_Logger.debug("getIncidentForId() RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getIncidentForId(evidenceId);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteIncidentDAO";
					s_Logger.error("getIncidentForId(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting incident through RemoteIncidentDAO";
				s_Logger.error("getIncidentForId(): " + errMsg, e);
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
	private RemoteIncidentDAO getRemoteDAO() throws AccessException, RemoteException, NotBoundException
	{
		if (m_RemoteDAO != null)
		{
			return m_RemoteDAO;
		}

		RemoteObjectFactoryDAO factory = getRemoteFactory();

		m_RemoteDAO = factory.getIncidentDAO(getOriginatorName());
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

}
