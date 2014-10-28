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
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.dao.DataSourceDAO;
import com.prelert.dao.TimeSeriesDAO;
import com.prelert.data.DataSource;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.MetricTreeNode;
import com.prelert.proxy.dao.RemoteDataSourceDAO;
import com.prelert.proxy.plugin.DataSourcePlugin;
import com.prelert.proxy.plugin.ExternalPointsPlugin;
import com.prelert.proxy.pluginLocator.PluginLocator;


public class DataSourceServerRMI extends UnicastRemoteObject implements RemoteDataSourceDAO
{
	static Logger s_Logger = Logger.getLogger(DataSourceServerRMI.class);

	private static final long serialVersionUID = -3019743863787078428L;

	private String m_ServerName;
	private DataSourceDAO m_DataSourceDAO;
	private TimeSeriesDAO m_TimeSeriesDAO;
	private PluginLocator m_PluginLocator;
	
	public DataSourceServerRMI() throws RemoteException
	{
		super();
	}
	
	public PluginLocator getPluginLocator()
	{
		return m_PluginLocator;
	}

	public void setPluginLocator(PluginLocator pluginLocator)
	{
		m_PluginLocator = pluginLocator;
	}
	
	public DataSourceDAO getDataSourceDAO()
	{
		return m_DataSourceDAO;
	}
	
	public void setDataSourceDAO(DataSourceDAO dao)
	{
		m_DataSourceDAO = dao;
	}


	/**
	 * Get the time series DAO, which may be needed for looking up time series
	 * IDs from external keys.
	 */
	public TimeSeriesDAO getTimeSeriesDAO()
	{
		return m_TimeSeriesDAO;
	}


	/**
	 * Set the time series DAO, which may be needed for looking up time series
	 * IDs from external keys.
	 */
	public void setTimeSeriesDAO(TimeSeriesDAO dao)
	{
		m_TimeSeriesDAO = dao;
	}


	public String getServerName()
	{
		return m_ServerName;
	}
	
	public void setServerName(String serverName)
	{
		m_ServerName = serverName;
	}


	/**
	 * Obtain a list of all known data types.  For each data type, also return
	 * the category, which will be one of "notification", "time series" or "time
	 * series feature".  Also, if possible, return a count of data points for
	 * each type.  For external types, the appropriate plugin is queried for the
	 * count.  If it is not possible to obtain an accurate count of data points,
	 * the count is set to -1.
	 * @return A list of all known data types, together with their category and
	 *         count.
	 */
	@Override
	public List<DataSourceType> getDataSourceTypes() 
	{
		s_Logger.debug("getDataSourceTypes()");
		
		List<DataSourceType> dataSourceTypes = m_DataSourceDAO.getDataSourceTypes();
		
		for (DataSourceType type : dataSourceTypes)
		{
			// The result of this call is fundamental to the operation of the
			// GUI.  Therefore, in the event of an exception occurring for one
			// data type, it's better to return partial results than nothing at
			// all.  Hence the try block is inside the for loop.
			try
			{
				// The call to the data_types() database procedure will return
				// -1 for the count of external types.  We need to check for
				// this as well as checking whether a plugin exists for the
				// type, as external time series types will have internal time
				// series features with the same type name.
				boolean isExternalType = (type.getCount() == -1 &&
									m_PluginLocator.isExternalPoints(type));
				if (isExternalType)
				{
					type.setCount(getExternalDataTypeCount(type));
				}
			}
			catch (Exception e)
			{
				s_Logger.error("Error setting count for data type " +
								type, e);
			}
		}

		return dataSourceTypes;		
	}


	/**
	 * Obtain a list of sources for a given type.  For external types this is
	 * requested from the appropriate plugin.  Otherwise the list of sources is
	 * retrieved from the database.
	 * @param The type for which to get a list of sources.
	 * @return The list of sources for the given type.
	 */
	@Override
	public List<DataSource> getDataSources(DataSourceType type) 
	{
		s_Logger.debug("Getting sources for type " + type);

		boolean isExternalType = m_PluginLocator.isExternal(type);
		if (isExternalType)
		{
			try
			{
				DataSourcePlugin plugin =
					m_PluginLocator.getExternalPluginForDataType(type);
				if (plugin != null)
				{
					return plugin.getDataSources(type);
				}

				s_Logger.error("Null data source plugin for external type " +
								type);
			}
			catch (Exception e)
			{
				s_Logger.error("Error getting source list for data type " +
								type, e);
			}
		}

		List<DataSource> sources = m_DataSourceDAO.getDataSources(type);
	
		if (m_PluginLocator.isExternalPoints(type))
		{
			ExternalPointsPlugin plugin = m_PluginLocator.getExternalPointsPluginForDataType(type);
			// update the point count for each source.
			for (DataSource source : sources)
			{
				source.setCount(plugin.getDataSourceItemCount(source));
			}
		}
		
		return sources;
	}


	@Override
	public List<DataSource>	getAllDataSources() 
	{
		List<DataSource> dataSources = m_DataSourceDAO.getAllDataSources();		
		
		return dataSources;
	}


	/**
	 * Get the count to be displayed next to the data type on the GUI's
	 * "Analysed Data" page.
	 * @return The count to be displayed next to the data type on the GUI's 
	 *         "Analysed Data" page.  If this information is not available,
	 *         returns -1.
	 */
	public int getExternalDataTypeCount(DataSourceType type)
	{
		ExternalPointsPlugin plugin = m_PluginLocator.getExternalPointsPluginForDataType(type);
		if (plugin == null)
		{
			return -1;
		}

		return plugin.getDataTypeItemCount(type);
	}

	
	@Override
	public List<MetricTreeNode> getDataSourceTreeNextLevel(String type,
											String previousPath, String currentValue,
											int opaqueNum, String opaqueStr)
	throws RemoteException
	{
		s_Logger.debug("getDataSourceTreeNextLevel");

		if (type == null && previousPath == null && currentValue == null)
		{
			// Return the top level sources
			return m_DataSourceDAO.getDataSourceTreeNextLevel(null, null, null,
																null, null);	
		}

		DataSourceType datatype = new DataSourceType(type, DataSourceCategory.TIME_SERIES);
		
		boolean isExternalType = type != null && m_PluginLocator.isExternal(datatype);
		if (isExternalType)
		{
			try
			{
				DataSourcePlugin plugin =
					m_PluginLocator.getExternalPluginForDataType(datatype);
				if (plugin != null)
				{
					List<MetricTreeNode> metricTreeNodes =
							plugin.getDataSourceTreeNextLevel(type, previousPath, currentValue,
															opaqueNum, opaqueStr);

					// Where external keys have been set by the plugin, attempt
					// to find the time series IDs being used in our database
					for (MetricTreeNode metricTreeNode : metricTreeNodes)
					{
						String externalKey = metricTreeNode.getExternalKey();
						if (externalKey != null)
						{
							metricTreeNode.setTimeSeriesId(
								m_TimeSeriesDAO.getTimeSeriesIdFromExternalKey(type,
																			externalKey)
							);
						}
					}

					return metricTreeNodes;
				}

				s_Logger.error("Null data source plugin for external type " +
								type);
			}
			catch (Exception e)
			{
				s_Logger.error("Error getting source list for data type " +
								type, e);
			}
		}

		return m_DataSourceDAO.getDataSourceTreeNextLevel(type, previousPath,
					currentValue, opaqueNum, opaqueStr);
	}


	@Override
	public List<MetricTreeNode> getDataSourceTreePreviousLevel(String type,
							String previousPath, int opaqueNum, String opaqueStr)
	throws RemoteException
	{
		s_Logger.debug("getDataSourceTreePreviousLevel");
		
		if (previousPath == null || previousPath.isEmpty())
		{
			// Return the top level sources
			return m_DataSourceDAO.getDataSourceTreeNextLevel(null, null, null, null, null);
		}
		
		if (previousPath.equals(type))
		{
			// Path equals the datatype so the next level up will
			// be all the datatypes.
			return m_DataSourceDAO.getDataSourceTreeNextLevel(null, null, null, null, null);
		}

		DataSourceType datatype = new DataSourceType(type, DataSourceCategory.TIME_SERIES);
		
		boolean isExternalType = type != null && m_PluginLocator.isExternal(datatype);
		if (isExternalType)
		{
			try
			{
				DataSourcePlugin plugin =
					m_PluginLocator.getExternalPluginForDataType(datatype);
				if (plugin != null)
				{
					List<MetricTreeNode> metricTreeNodes =
							plugin.getDataSourceTreePreviousLevel(type, previousPath,
																opaqueNum, opaqueStr);

					// Where external keys have been set by the plugin, attempt
					// to find the time series IDs being used in our database
					for (MetricTreeNode metricTreeNode : metricTreeNodes)
					{
						String externalKey = metricTreeNode.getExternalKey();
						if (externalKey != null)
						{
							metricTreeNode.setTimeSeriesId(
								m_TimeSeriesDAO.getTimeSeriesIdFromExternalKey(type,
																			externalKey)
							);
						}
					}

					return metricTreeNodes;
				}

				s_Logger.error("Null data source plugin for external type " +
								type);
			}
			catch (Exception e)
			{
				s_Logger.error("Error getting source list for data type " +
								type, e);
			}
		}
		
		return m_DataSourceDAO.getDataSourceTreePreviousLevel(type, previousPath,
									 opaqueNum, opaqueStr);
	}


	/**
	 * If previousPath is null or empty then
	 * getDataSourceTreeNextLevel(null, null, null, null, null)
	 * is returned.
	 */
	@Override
	public List<MetricTreeNode> getDataSourceTreeCurrentLevel(String type,
											String previousPath, int opaqueNum, String opaqueStr)
	throws RemoteException
	{
		s_Logger.debug("getDataSourceTreeCurrentLevel");
		
		if (previousPath == null || previousPath.isEmpty())
		{
			// Return the top level sources
			return m_DataSourceDAO.getDataSourceTreeNextLevel(null, null, null, null, null);
		}
		
		DataSourceType datatype = new DataSourceType(type, DataSourceCategory.TIME_SERIES);
		
		boolean isExternalType = type != null && m_PluginLocator.isExternal(datatype);
		if (isExternalType)
		{
			try
			{
				DataSourcePlugin plugin =
					m_PluginLocator.getExternalPluginForDataType(datatype);
				if (plugin != null)
				{
					List<MetricTreeNode> metricTreeNodes =
							plugin.getDataSourceTreeCurrentLevel(type, previousPath,
																opaqueNum, opaqueStr);

					// Where external keys have been set by the plugin, attempt
					// to find the time series IDs being used in our database
					for (MetricTreeNode metricTreeNode : metricTreeNodes)
					{
						String externalKey = metricTreeNode.getExternalKey();
						if (externalKey != null)
						{
							metricTreeNode.setTimeSeriesId(
								m_TimeSeriesDAO.getTimeSeriesIdFromExternalKey(type,
																			externalKey)
							);
						}
					}

					return metricTreeNodes;
				}

				s_Logger.error("Null data source plugin for external type " +
								type);
			}
			catch (Exception e)
			{
				s_Logger.error("Error getting source list for data type " +
								type, e);
			}
		}

		return m_DataSourceDAO.getDataSourceTreeCurrentLevel(type, previousPath,
									 opaqueNum, opaqueStr);
	}


    @Override
    public Date getEndTime() throws RemoteException 
    {
    	// Returns the expiry time of the product license.
    	// NB. Placed in this DAO to hide its purpose from the user.
	    return m_DataSourceDAO.getEndTime();
    }


	@Override
	public String getCustomerId() throws RemoteException 
	{
		return m_DataSourceDAO.getCustomerId();
	}

}
