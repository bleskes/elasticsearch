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

import com.prelert.dao.CausalityDAO;
import com.prelert.data.CausalityData;
import com.prelert.data.ProbableCause;
import com.prelert.data.Evidence;
import com.prelert.proxy.dao.RemoteCausalityDAO;
import com.prelert.proxy.dao.RemoteObjectFactoryDAO;


/**
 * Implementation for RMI (Remote Method Invocation) of the CausalityDAO
 * interface. The class makes calls through RMI to a remote server which 
 * returns causality information
 * 
 * @author dkyle
 *
 */
public class CausalityProxyDAO extends RemoteProxyDAO implements CausalityDAO
{
	static Logger s_Logger = Logger.getLogger(CausalityProxyDAO.class);

	private RemoteCausalityDAO m_RemoteDAO;


	public CausalityProxyDAO()
	{
		m_RemoteDAO = null;
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the list of
	 * 		probable causes remotely through the Proxy.
	 */
	@Override
	public List<ProbableCause> getProbableCauses(int evidenceId, int timeSpanSecs,
												boolean maxOneFeaturePerSeries)
	{
		s_Logger.debug("getProbableCause RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getProbableCauses(evidenceId,
														timeSpanSecs,
														maxOneFeaturePerSeries);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteCausalityDAO";
					s_Logger.error("getProbableCauses(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting probable causes through RemoteCausalityDAO";
				s_Logger.error("getProbableCauses(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the list of
	 * 		probable causes remotely through the Proxy.
	 */
	@Override
	public List<ProbableCause> getProbableCausesForExport(int evidenceId, int timeSpanSecs,
														boolean maxOneFeaturePerSeries)
	{
		s_Logger.debug("getProbableCausesForExport RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getProbableCausesForExport(evidenceId,
																timeSpanSecs,
																maxOneFeaturePerSeries);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteCausalityDAO";
					s_Logger.error("getProbableCausesForExport(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting probable causes through RemoteCausalityDAO";
				s_Logger.error("getProbableCausesForExport(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the list of
	 * 		probable causes remotely through the Proxy.
	 */
	@Override
	public List<ProbableCause> getProbableCausesInBulk(List<Integer> evidenceIds,
														boolean maxOneFeaturePerSeries)
	{
		s_Logger.debug("getProbableCausesInBulk RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getProbableCausesInBulk(evidenceIds,
															maxOneFeaturePerSeries);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteCausalityDAO";
					s_Logger.error("getProbableCausesInBulk(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting probable causes through RemoteCausalityDAO";
				s_Logger.error("getProbableCausesInBulk(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	@Override
    public List<CausalityData> getCausalityData(int evidenceId, 
			List<String> returnAttributes, List<String> primaryFilterNamesNull,
			List<String> primaryFilterNamesNotNull, List<String> primaryFilterValues,
			String secondaryFilterName, String secondaryFilterValue)
    {
		s_Logger.debug("getCausalityData RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getCausalityData(evidenceId, returnAttributes, 
						primaryFilterNamesNull, primaryFilterNamesNotNull, primaryFilterValues,
						secondaryFilterName, secondaryFilterValue);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteCausalityDAO";
					s_Logger.error("getCausalityData(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting causality data through RemoteCausalityDAO";
				s_Logger.error("getCausalityData(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
    }


	/**
	 * @throws ProxyDataAccessException if an error occurs getting the page of
	 * 		evidence data remotely through the Proxy.
	 */
	@Override
	public List<Evidence> getFirstPage(boolean singleDescription, int evidenceId, 
			List<String> filterAttributes, List<String> filterValues, int pageSize)
	{
		s_Logger.debug("getFirstPage RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getFirstPage(singleDescription, evidenceId, 
						filterAttributes, filterValues, pageSize);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteCausalityDAO";
					s_Logger.error("getFirstPage(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting first page of evidence through RemoteCausalityDAO";
				s_Logger.error("getFirstPage(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs getting the page of
	 * 		evidence data remotely through the Proxy.
	 */
	@Override
	public List<Evidence> getLastPage(boolean singleDescription, int evidenceId, 
			List<String> filterAttributes, List<String> filterValues, int pageSize)
	{
		s_Logger.debug("getLastPage RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getLastPage(singleDescription, evidenceId, 
						filterAttributes, filterValues, pageSize);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteCausalityDAO";
					s_Logger.error("getLastPage(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting last page of evidence through RemoteCausalityDAO";
				s_Logger.error("getLastPage(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs getting the page of
	 * 		evidence data remotely through the Proxy.
	 */
	@Override
	public List<Evidence> getNextPage(boolean singleDescription, int bottomRowId, 
			Date bottomRowTime, List<String> filterAttributes, List<String> filterValues, int pageSize)
	{
		s_Logger.debug("getNextPage RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getNextPage(singleDescription, bottomRowId, 
						bottomRowTime, filterAttributes, filterValues, pageSize);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteCausalityDAO";
					s_Logger.error("getNextPage(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting next page of evidence through RemoteCausalityDAO";
				s_Logger.error("getNextPage(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs getting the page of
	 * 		evidence data remotely through the Proxy.
	 */
	@Override
	public List<Evidence> getPreviousPage(boolean singleDescription, int topRowId, 
			Date topRowTime, List<String> filterAttributes, List<String> filterValues, int pageSize)
	{
		s_Logger.debug("getPreviousPage RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getPreviousPage(singleDescription, topRowId, topRowTime, 
						filterAttributes, filterValues, pageSize);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteCausalityDAO";
					s_Logger.error("getPreviousPage(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting previous page of evidence through RemoteCausalityDAO";
				s_Logger.error("getPreviousPage(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs getting the page of
	 * 		evidence data remotely through the Proxy.
	 */
	@Override
	public List<Evidence> getAtTime(boolean singleDescription, int evidenceId, Date time,
			List<String> filterAttributes, List<String> filterValues, int pageSize)
	{
		s_Logger.debug("getAtTime RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getAtTime(singleDescription, evidenceId, time,
												filterAttributes, filterValues, pageSize);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteCausalityDAO";
					s_Logger.error("getAtTime(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting page of evidence through RemoteCausalityDAO";
				s_Logger.error("getAtTime(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs getting evidence data
	 * remotely through the Proxy.
	 */
	@Override
	public Evidence getEarliestEvidence(boolean singleDescription, int evidenceId,
			List<String> filterAttributes, List<String> filterValues)
	{
		s_Logger.debug("getEarliestEvidence RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getEarliestEvidence(singleDescription,
									evidenceId, filterAttributes, filterValues);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteCausalityDAO";
					s_Logger.error("getEarliestEvidence(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting evidence data through RemoteCausalityDAO";
				s_Logger.error("getEarliestEvidence(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs getting evidence data
	 * remotely through the Proxy.
	 */
	@Override
	public Evidence getLatestEvidence(boolean singleDescription, int evidenceId,
			List<String> filterAttributes, List<String> filterValues)
	{
		s_Logger.debug("getLatestEvidence RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getLatestEvidence(singleDescription,
									evidenceId, filterAttributes, filterValues);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteCausalityDAO";
					s_Logger.error("getLatestEvidence(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting evidence data through RemoteCausalityDAO";
				s_Logger.error("getLatestEvidence(): " + errMsg, e);
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
	private RemoteCausalityDAO getRemoteDAO() throws AccessException, RemoteException, NotBoundException
	{
		if (m_RemoteDAO != null)
		{
			return m_RemoteDAO;
		}

		RemoteObjectFactoryDAO factory = getRemoteFactory();
		m_RemoteDAO = factory.getCausalityDAO(getOriginatorName());

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
