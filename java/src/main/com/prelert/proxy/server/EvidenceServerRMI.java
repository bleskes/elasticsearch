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

package com.prelert.proxy.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.List;

import com.prelert.dao.EvidenceDAO;
import com.prelert.data.Attribute;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.Evidence;
import com.prelert.data.MetricPath;
import com.prelert.proxy.dao.RemoteEvidenceDAO;
import com.prelert.proxy.plugin.TimeSeriesPlugin;
import com.prelert.proxy.pluginLocator.PluginLocator;

public class EvidenceServerRMI extends UnicastRemoteObject implements RemoteEvidenceDAO
{
	private static final long serialVersionUID = -7762885182971883139L;

	private String m_ServerName;
	private EvidenceDAO m_EvidenceDAO;
	
	private PluginLocator m_PluginLocator;
	
	
	public EvidenceServerRMI() throws RemoteException
	{
		super();
		
	}
		
	public EvidenceDAO getEvidenceDAO()
	{
		return m_EvidenceDAO;
	}
	
	public void setEvidenceDAO(EvidenceDAO dao)
	{
		m_EvidenceDAO = dao;
	}
	
	public String getServerName()
	{
		return m_ServerName;
	}
	
	public void setServerName(String serverName)
	{
		m_ServerName = serverName;
	}
	
	public PluginLocator getPluginLocator()
	{
		return m_PluginLocator;
	}

	public void setPluginLocator(PluginLocator pluginLocator)
	{
		m_PluginLocator = pluginLocator;
	}
	

	@Override
	public List<Evidence> getAtTime(String dataType, String source, 
								Date time, List<String> filterAttributes, 
								List<String> filterValues, int pageSize) 
	{

		return m_EvidenceDAO.getAtTime(dataType, source, time, 
						filterAttributes, filterValues, pageSize);
	}

	
	/**
	 * For the Time Series which can be uniquely defined by <code>datatype</code>,
	 * <code>metric</code>, <code>source</code> and <code>attributes</code> this 
	 * function returns any evidence features which occurred between the time period 
	 * <code>startTime</code> and <code>endTime</code>.
	 * 
	 * @param datatype 
	 * @param metric
	 * @param source
	 * @param attributes Filter by attributes
	 * @param startTime
	 * @param endTime
	 * @return List of <code>Evidence</code> features, if any, for the time series 
	 * defined by datatype, metric, source in the time period.
	 * The only the <code>Id</code> and <code>Time</code> fields will be populated
	 * in the returned <code>Evidence</code> objects. 
	 */
	@Override
	public List<Evidence> getEvidenceInTimeSeries(String datatype, String metric,
												String source, List<Attribute> attributes,
												Date startTime, Date endTime)
	{
		if (datatype == null)
		{
			throw new IllegalArgumentException("datatype cannot be null");
		}

		return m_EvidenceDAO.getEvidenceInTimeSeries(datatype, metric, source,
												attributes, startTime, endTime);
	}	
	
	
	/**
	 * For an external Time Series which can be uniquely defined by <code>datatype</code>,
	 * <code>metric</code>, <code>source</code> and <code>attributes</code> this 
	 * function returns any evidence features which occurred between the time period 
	 * <code>startTime</code> and <code>endTime</code>.
	 * 
	 * @param dataType Must be an external datatype.
	 * @param metric Time Series metric.
	 * @param source Data source.
	 * @param attributes Attributes to filter on
	 * @param startTime
	 * @param endTime
	 * @return Returns any <code>Evidence</code> for the the time series <code>
	 * datatype</code> or an empty list if <code>datatype</code> is not an
	 * external type.
	 * @throws IllegalArgumentException
	 */
	@Override
	public List<Evidence> getEvidenceInExternalTimeSeries(String datatype, String metric,
												String source, List<Attribute> attributes,
												Date startTime, Date endTime)
	{
		if (datatype == null)
		{
			throw new IllegalArgumentException("datatype cannot be null");
		}

		return m_EvidenceDAO.getEvidenceInExternalTimeSeries(datatype, metric, source,
												attributes, startTime, endTime);
	}


	@Override
	public List<String> getColumnValues(String dataType, String columnName, int maxRows) 
	{
		return m_EvidenceDAO.getColumnValues(dataType, columnName, maxRows);
	}
	

	@Override
	public List<Evidence> getFirstPage(String dataType, String source, 
			List<String> filterAttributes, List<String> filterValues, int pageSize) 
	{		
		return m_EvidenceDAO.getFirstPage(dataType, source, 
								filterAttributes, filterValues, pageSize);
	}
	
	
	@Override
	public List<Evidence> getLastPage(String dataType, String source, 
				List<String> filterAttributes, List<String> filterValues, int pageSize) 
	{

		return m_EvidenceDAO.getLastPage(dataType, source, 
						filterAttributes, filterValues, pageSize);	
	}
	
	
	@Override
	public List<Evidence> getNextPage(String dataType, String source, 
								Date bottomRowTime, int bottomRowId, List<String> filterAttributes, 
								List<String> filterValues, int pageSize) 
	{
		return m_EvidenceDAO.getNextPage(dataType, source, bottomRowTime, 
									bottomRowId, filterAttributes, filterValues, pageSize);
	}
	
	
	@Override
	public List<Evidence> getPreviousPage(String dataType, String source, 
			Date topRowTime, int topRowId, 
			List<String> filterAttributes, List<String> filterValues, int pageSize) 
	{
		return m_EvidenceDAO.getPreviousPage(dataType, source, topRowTime, topRowId, 
						filterAttributes, filterValues, pageSize);
	}
	
	
	@Override
	public Evidence getEarliestEvidence(String dataType, String source,
			List<String> filterAttributes, List<String> filterValues) 
	{

		return m_EvidenceDAO.getEarliestEvidence(dataType, source, 
													filterAttributes, filterValues);
	}
	

	@Override
	public Evidence getLatestEvidence(String dataType, String source,
			List<String> filterAttributes, List<String> filterValues) 
	{
		return m_EvidenceDAO.getLatestEvidence(dataType, source, 
										filterAttributes, filterValues);
	}
	
	
	@Override
	public Evidence getEvidenceSingle(int id) 
	{
		return m_EvidenceDAO.getEvidenceSingle(id);
	}


	/**
	 * Returns all attributes for the item of evidence with the specified ID
	 * (instead of just the current display columns).
	 * @param id The value of the id column for the evidence to obtain
	 *           information on.
	 * @return List of <code>Attribute</code> objects for the row with the
	 *         specified ID.  Note that values for time fields are transported
	 *         as a <code>String</code> representation of the number of
	 *         milliseconds since January 1, 1970, 00:00:00 GMT.
	 */
	@Override
	public List<Attribute> getEvidenceAttributes(int id)
	{
		return m_EvidenceDAO.getEvidenceAttributes(id);
	}
	
	
	@Override
	public List<Evidence> getIdPage(String dataType, String source, int id, int pageSize) 
	{
		return m_EvidenceDAO.getIdPage(dataType, source, id, pageSize);
	}
	
	
	@Override
	public List<String> getAllColumns(String dataType) 
	{
		return m_EvidenceDAO.getAllColumns(dataType);
	}

	
	@Override
	public List<String> getFilterableColumns(String dataType, 
			boolean getCompulsory, boolean getOptional) 
	{
		return m_EvidenceDAO.getFilterableColumns(dataType, getCompulsory, getOptional);
	}


	/*
	 * Search functions 
	 */

	@Override
	public List<Evidence> searchAtTime(String dataType, String source, 
							Date time, String containsText, int pageSize) 
	{	
		return m_EvidenceDAO.searchAtTime(dataType, source, time, 
											containsText, pageSize);
	}		
	
	@Override
	public List<Evidence> searchFirstPage(String dataType, String source, 
			String containsText, int pageSize) 
	{
		return m_EvidenceDAO.searchFirstPage(dataType, source, containsText, pageSize);
	}
	
	
	@Override
	public List<Evidence> searchLastPage(
			String dataType, String source, String containsText, int pageSize) 
	{
		return m_EvidenceDAO.searchLastPage(dataType, source, containsText, pageSize);
	}
	

	@Override
	public List<Evidence> searchNextPage(
			String dataType, String source, Date bottomRowTime, 
			int bottomRowId, String containsText, int pageSize) 
	{

		return m_EvidenceDAO.searchNextPage(dataType, source, 
									bottomRowTime, bottomRowId, containsText, pageSize); 
	}
	
	
	@Override
	public List<Evidence> searchPreviousPage(
			String dataType, String source, Date topRowTime, 
			int topRowId, String containsText, int pageSize) 
	{
		return m_EvidenceDAO.searchPreviousPage(dataType, source, 
									topRowTime, topRowId, containsText, pageSize); 
	}

	
	@Override
	public Evidence searchEarliestEvidence(
			String dataType, String source, String containsText) 
	{
		return m_EvidenceDAO.searchEarliestEvidence(dataType, source, containsText); 			
	}


	@Override
	public Evidence searchLatestEvidence(
			String dataType, String source, String containsText) 
	{
		return m_EvidenceDAO.searchLatestEvidence(dataType, source, 
																containsText);
	}
	

	/**
	 * Calls the database procedure then checks the returned MetricPath.
	 * If it has an external key the relative plugin is found and asked 
	 * to translate that external key into a MetricPath.
	 */
	@Override
	public MetricPath getMetricPathFromEvidenceId(int id) 
	{
		MetricPath path = m_EvidenceDAO.getMetricPathFromEvidenceId(id);	
		
		if (path.getExternalKey() != null && path.getExternalKey().isEmpty() == false)
		{
			DataSourceType datatype = new DataSourceType(path.getDatatype(), DataSourceCategory.TIME_SERIES);
			TimeSeriesPlugin plugin = m_PluginLocator.getExternalPluginForDataType(datatype);
			if (plugin != null)
			{
				path = plugin.metricPathFromExternalKey(
										path.getDatatype(), path.getExternalKey());
			}
		}
		
		return path;
	}
}
