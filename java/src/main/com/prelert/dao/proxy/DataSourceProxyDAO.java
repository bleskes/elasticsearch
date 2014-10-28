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

package com.prelert.dao.proxy;

import java.rmi.AccessException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.dao.DataSourceDAO;
import com.prelert.data.DataSource;
import com.prelert.data.DataSourceType;
import com.prelert.data.MetricTreeNode;
import com.prelert.proxy.dao.RemoteDataSourceDAO;
import com.prelert.proxy.dao.RemoteObjectFactoryDAO;


/**
 * Implementation for RMI (Remote Method Invocation) of the DataSourceDAO 
 * interface. The class makes calls through RMI to a remote server which 
 * returns information on Prelert data sources.
 */
public class DataSourceProxyDAO extends RemoteProxyDAO implements DataSourceDAO
{
	static Logger s_Logger = Logger.getLogger(DataSourceProxyDAO.class);

	private RemoteDataSourceDAO m_RemoteDAO;


	public DataSourceProxyDAO()
	{
		m_RemoteDAO = null;
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the list of
	 * 		data source types remotely through the Proxy.
	 */
	@Override
	public List<DataSourceType> getDataSourceTypes()
	{
		s_Logger.debug("getDataSourceTypes RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getDataSourceTypes();
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteDataSourceDAO";
	        		s_Logger.error("getDataSourceTypes(): " + errMsg, ce);
	        		throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting data source types through RemoteDataSourceDAO";
	        	s_Logger.error("getDataSourceTypes(): " + errMsg, e);
	        	throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the list of
	 * 		data sources remotely through the Proxy.
	 */
	@Override
	public List<DataSource> getDataSources(DataSourceType dataSourceType)
	{
		s_Logger.debug("getDataSources RMI call: Type = " + dataSourceType.toString());

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getDataSources(dataSourceType);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteDataSourceDAO";
	        		s_Logger.error("getDataSources(): " + errMsg, ce);
	        		throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting data sources through RemoteDataSourceDAO";
	        	s_Logger.error("getDataSources(): " + errMsg, e);
	        	throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the list of
	 * 		data sources remotely through the Proxy.
	 */
	@Override
	public List<DataSource>	getAllDataSources()
	{
		s_Logger.debug("getAllDataSources RMI call");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getAllDataSources();
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteDataSourceDAO";
	        		s_Logger.error("getAllDataSources(): " + errMsg, ce);
	        		throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting data sources through RemoteDataSourceDAO";
	        	s_Logger.error("getAllDataSources(): " + errMsg, e);
	        	throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	@Override
	public List<MetricTreeNode> getDataSourceTreeNextLevel(String type, String previousPath,
												String currentValue, Integer opaqueNum,
												String opaqueStr)
	{
		String debug= "getDataSourceTreeNextLevel({0}, {1}, {2}, {3}, {4}) RMI call";
		debug = MessageFormat.format(debug, type, previousPath, currentValue, 
				opaqueNum, opaqueStr);
		s_Logger.debug(debug);

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getDataSourceTreeNextLevel(type, previousPath,
														currentValue, opaqueNum, opaqueStr);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteDataSourceDAO";
	        		s_Logger.error("getDataSourceTreeNextLevel(): " + errMsg, ce);
	        		throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting data sources through RemoteDataSourceDAO";
	        	s_Logger.error("getDataSourceTreeNextLevel(): " + errMsg, e);
	        	throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	

	/**
	 * Returns the <code>MetricTreeNode</code> for the specified 
	 * paths parent node. Returns the equivalent of calling 
	 * <code>getDataSourceTreeNextLevel()</code> for the nodes parent's
	 * parent.
	 */
	@Override
	public List<MetricTreeNode> getDataSourceTreePreviousLevel(String type, String previousPath,
															Integer opaqueNum,String opaqueStr)
	{
		s_Logger.debug("getDataSourceTreePreviousLevel() RMI call");
		
		String debug= "getDataSourceTreePreviousLevel({0}, {1}, {2}, {3}) RMI call";
		debug = MessageFormat.format(debug, type, previousPath, opaqueNum, opaqueStr);
		s_Logger.debug(debug);

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getDataSourceTreePreviousLevel(type, previousPath, 
														opaqueNum, opaqueStr);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteDataSourceDAO";
	        		s_Logger.error("getDataSourceTreePreviousLevel(): " + errMsg, ce);
	        		throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting data sources through RemoteDataSourceDAO";
	        	s_Logger.error("getDataSourceTreePreviousLevel(): " + errMsg, e);
	        	throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}

	
	/**
	 * Return all the child tree nodes of the path denoted
	 * by the <code>previousPath</code> argument. 
	 */
	@Override
	public List<MetricTreeNode> getDataSourceTreeCurrentLevel(String type,
										String previousPath, Integer opaqueNum, String opaqueStr) 
	{
		String debug= "getDataSourceTreeCurrentLevel({0}, {1}, {2}, {3}) RMI call";
		debug = MessageFormat.format(debug, type, previousPath, opaqueNum, opaqueStr);
		s_Logger.debug(debug);

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getDataSourceTreeCurrentLevel(type, previousPath, 
														opaqueNum, opaqueStr);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteDataSourceDAO";
	        		s_Logger.error("getDataSourceTreeCurrentLevel(): " + errMsg, ce);
	        		throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting data sources through RemoteDataSourceDAO";
	        	s_Logger.error("getDataSourceTreeCurrentLevel(): " + errMsg, e);
	        	throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	@Override
    public Date getEndTime()
    {
	    // Returns the expiry time of the product license.
    	// NB. Placed in this DAO to hide its purpose from the user.

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getEndTime();
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteDataSourceDAO";
					s_Logger.error("getEndTime(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting end time through RemoteDataSourceDAO";
				s_Logger.error("getEndTime(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
    }


	@Override
	public String getCustomerId()
	{
		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getCustomerId();
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteDataSourceDAO";
					s_Logger.error("getEndTime(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting customer ID through RemoteDataSourceDAO";
				s_Logger.error("getEndTime(): " + errMsg, e);
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
	private RemoteDataSourceDAO getRemoteDAO() throws AccessException, RemoteException, NotBoundException
	{
		if (m_RemoteDAO != null)
		{
			return m_RemoteDAO;
		}

		RemoteObjectFactoryDAO factory = getRemoteFactory();
		m_RemoteDAO = factory.getDataSourceDAO(getOriginatorName());
		
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
