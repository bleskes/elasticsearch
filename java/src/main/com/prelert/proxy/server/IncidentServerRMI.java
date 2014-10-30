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

package com.prelert.proxy.server;


import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.dao.IncidentDAO;
import com.prelert.data.CausalityAggregate;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.Incident;
import com.prelert.data.MetricTreeNode;
import com.prelert.proxy.Proxy;
import com.prelert.proxy.dao.RemoteIncidentDAO;
import com.prelert.proxy.plugin.TimeSeriesPlugin;
import com.prelert.proxy.pluginLocator.PluginLocator;


public class IncidentServerRMI extends UnicastRemoteObject implements RemoteIncidentDAO
{
	private static final long serialVersionUID = 367760070005318266L;
	
	private static Logger s_Logger = Logger.getLogger(IncidentServerRMI.class);

	private String m_ServerName;
	private IncidentDAO m_IncidentDAO;
	private PluginLocator m_PluginLocator;
	
	public IncidentServerRMI() throws RemoteException
	{
		super();
	}


	public IncidentServerRMI(Proxy proxy) throws RemoteException
	{
		this();
	}


	public PluginLocator getPluginLocator()
	{
		return m_PluginLocator;
	}


	public void setPluginLocator(PluginLocator pluginLocator)
	{
		m_PluginLocator = pluginLocator;
	}


	public IncidentDAO getIncidentDAO()
	{
		return m_IncidentDAO;
	}


	public void setIncidentDAO(IncidentDAO dao)
	{
		m_IncidentDAO = dao;
	}


	public String getServerName()
	{
		return m_ServerName;
	}


	public void setServerName(String serverName)
	{
		m_ServerName = serverName;
	}

	@Override
	public List<Incident> getIncidents(Date minTime, Date maxTime, int anomalyThreshold)
	{
		List<Incident> incidents = m_IncidentDAO.getIncidents(minTime, maxTime, anomalyThreshold);
		
		return incidents;
	}
	
	
	@Override
	public List<Incident> getIncidentsAdaptive(Date minTime, Date maxTime, int anomalyThreshold)
	{
		List<Incident> incidents = m_IncidentDAO.getIncidentsAdaptive(minTime, maxTime, anomalyThreshold);
		
		return incidents;
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
		List<Incident> incidents = m_IncidentDAO.getIncidentsInTimeRange(minTime, minTimeIsOpen, 
				maxTime, maxTimeIsOpen,
				minFirstTime, minFirstTimeIsOpen, 
				maxFirstTime, maxFirstTimeIsOpen, 
				minLastTime, minLastTimeIsOpen,
				maxLastTime, maxLastTimeIsOpen, 
				minUpdateTime, minUpdateTimeIsOpen, 
				maxUpdateTime, maxUpdateTimeIsOpen, anomalyThreshold,
				metricPath, likeMetricPath, escapeChar);
		
		return incidents;
	}
	
	
	@Override
	public List<Incident> getFirstPage(int anomalyThreshold, int pageSize)
			throws RemoteException
	{
		return m_IncidentDAO.getFirstPage(anomalyThreshold, pageSize);
	}


	@Override
	public List<Incident> getLastPage(int anomalyThreshold, int pageSize)
			throws RemoteException
	{
		return m_IncidentDAO.getLastPage(anomalyThreshold, pageSize);
	}


	@Override
	public List<Incident> getNextPage(Date bottomRowTime,
			int bottomRowEvidenceId, int anomalyThreshold, int pageSize)
			throws RemoteException
	{
		return m_IncidentDAO.getNextPage(bottomRowTime, bottomRowEvidenceId,
				anomalyThreshold, pageSize);
	}


	@Override
	public List<Incident> getPreviousPage(Date topRowTime,
			int topRowEvidenceId, int anomalyThreshold, int pageSize)
			throws RemoteException
	{
		return m_IncidentDAO.getPreviousPage(topRowTime, topRowEvidenceId,
				anomalyThreshold, pageSize);
	}


	@Override
	public List<Incident> getAtTime(Date time, int anomalyThreshold,
			int pageSize, boolean orderAscending) throws RemoteException
	{
		return m_IncidentDAO.getAtTime(time, anomalyThreshold, pageSize,
										orderAscending);
	}

	
	@Override
	public Date getEarliestTime()
	{
		return m_IncidentDAO.getEarliestTime();
	}
	
	@Override
	public Date getLatestTime()
	{
		return m_IncidentDAO.getLatestTime();
	}


	/**
	 * Returns a list of the names and prefixes for the constituents of the
	 * longest metric path associated with the time series in a particular
	 * incident.  This method returns an empty list if the incident in question
	 * contains more than one type of data, because in that case it doesn't make
	 * sense to select names for a single metric path within it.
	 * 
	 * For internal data, the results of the database procedure are returned
	 * unaltered.  However, for external data, things are more complex.  The
	 * database procedure returns the external keys of the time series involved
	 * in the incident, and these must be passed to the appropriate plugin, for
	 * it to choose the one that corresponds to the longest metric path, and
	 * return the names from this.
	 *
	 * @param evidenceId The evidence ID of a notification or time series
	 *                   feature in the incident.
	 * @return A list of partially populated <code>MetricTreeNode</code> objects
	 *         containing the name and prefix of each constituent of the metric
	 *         path.
	 */
	@Override
	public List<MetricTreeNode> getIncidentMetricPathNodes(int evidenceId)
	{
		List<MetricTreeNode> daoResult = m_IncidentDAO.getIncidentMetricPathNodes(evidenceId);
		
		if (daoResult.size() > 0)
		{
			String type = daoResult.get(0).getType();
			for (int i=1; i<daoResult.size(); i++)
			{
				if (type != null && 
						type.equals(daoResult.get(i).getType()) == false)
				{
					s_Logger.error("The incident contains more than one data type: '"
							+ type + "' and '" + daoResult.get(i).getType() + "'");
					return Collections.emptyList();
				}
			}
		}
		
		
		// Assume that if the first item in the returned list doesn't have an
		// external key, nothing in the list will.  This certainly should be
		// true, as any single type is either internal or external, and the DAO
		// is specified to return nothing for incidents containing more than one
		// type.
		if (daoResult.isEmpty() == false && daoResult.get(0).getExternalKey() != null)
		{
			// If its an external type the associated plugin will 
			// build the new MetricTreeNodes.
			DataSourceType datatype = new DataSourceType(daoResult.get(0).getType(),
										DataSourceCategory.TIME_SERIES);
			
			if (m_PluginLocator.isExternal(datatype))
			{
				TimeSeriesPlugin plugin = m_PluginLocator.getExternalPluginForDataType(datatype);
				if (plugin != null)
				{
					List<MetricTreeNode> pluginResult = plugin.metricPathNodesFromExternalKeys(daoResult);
					return pluginResult;
				}
			}
		}
		
		return daoResult;
	}


	@Override
	public List<String> getIncidentAttributeNames(int evidenceId)
	{
		return m_IncidentDAO.getIncidentAttributeNames(evidenceId);
	}
	
	
	@Override
	public List<String> getIncidentAttributeValues(int evidenceId, String attributeName)
	{
		return m_IncidentDAO.getIncidentAttributeValues(evidenceId, attributeName);
	}
	

	@Override
	public List<CausalityAggregate> getIncidentSummary(int evidenceId,
													String aggregateBy,
													List<String> groupingAttributes)
	{
		return m_IncidentDAO.getIncidentSummary(evidenceId,
												aggregateBy,
												groupingAttributes);
	}


	@Override
	public Incident getIncidentForId(int evidenceId) throws RemoteException
	{
		return m_IncidentDAO.getIncidentForId(evidenceId);
	}

}
