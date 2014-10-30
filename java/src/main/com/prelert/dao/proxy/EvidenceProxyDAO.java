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

import com.prelert.dao.EvidenceDAO;
import com.prelert.data.Evidence;
import com.prelert.data.Attribute;
import com.prelert.data.MetricPath;

import com.prelert.proxy.dao.RemoteEvidenceDAO;
import com.prelert.proxy.dao.RemoteObjectFactoryDAO;

/**
 * Implementation for RMI (Remote Method Invocation) of the EvidenceDAO 
 * interface. The class makes calls through RMI to a remote server which 
 * returns details of evidence used in Prelert's analysis. 
 * 
 * See EvidenceDAO for documentation.
 */
public class EvidenceProxyDAO extends RemoteProxyDAO implements EvidenceDAO
{
	static Logger s_Logger = Logger.getLogger(EvidenceProxyDAO.class);

	private RemoteEvidenceDAO m_RemoteDAO;

	public EvidenceProxyDAO()
	{
		m_RemoteDAO = null;
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the list of
	 * columns remotely through the Proxy.
	 */
	@Override
	public List<String> getAllColumns(String dataType)
	{
		s_Logger.debug("getAllColumns RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getAllColumns(dataType);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteEvidenceDAO";
					s_Logger.error("getAllColumns(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting " + dataType + " columns through RemoteEvidenceDAO";
				s_Logger.error("getAllColumns(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the list of
	 * filterable columns remotely through the Proxy.
	 */
	@Override
	public List<String> getFilterableColumns(String dataType, boolean getCompulsory, 
			boolean getOptional)
	{
		s_Logger.debug("getFilterableColumns RMI call");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getFilterableColumns(dataType, getCompulsory, getOptional);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteEvidenceDAO";
					s_Logger.error("getFilterableColumns(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting " + dataType + " filterable columns through RemoteEvidenceDAO";
				s_Logger.error("getFilterableColumns(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the column
	 * values remotely through the Proxy.
	 */
	@Override
	public List<String> getColumnValues(String dataType, String columnName, int maxRows)
	{
		s_Logger.debug("getColumnValues RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getColumnValues(dataType, columnName, maxRows);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteEvidenceDAO";
					s_Logger.error("getColumnValues(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting values for " + columnName + " through RemoteEvidenceDAO";
				s_Logger.error("getColumnValues(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the first page
	 * of evidence data remotely through the Proxy.
	 */
	@Override
	public List<Evidence> getFirstPage(String dataType, String source, 
			List<String> filterAttributes, List<String> filterValues, int pageSize)
	{
		s_Logger.debug("getFirstPage RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getFirstPage(
						dataType, source, filterAttributes, filterValues, pageSize);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteEvidenceDAO";
					s_Logger.error("getFirstPage(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting first page for " + dataType + " through RemoteEvidenceDAO";
				s_Logger.error("getFirstPage(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the last page
	 * of evidence data remotely through the Proxy.
	 */
	@Override
	public List<Evidence> getLastPage(String dataType, String source, 
			List<String> filterAttributes, List<String> filterValues, int pageSize)
	{
		s_Logger.debug("getLastPage RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getLastPage(
						dataType, source, filterAttributes, filterValues, pageSize);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteEvidenceDAO";
					s_Logger.error("getLastPage(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting last page for " + dataType + " through RemoteEvidenceDAO";
				s_Logger.error("getLastPage(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the next page
	 * of evidence data remotely through the Proxy.
	 */
	@Override
	public List<Evidence> getNextPage(String dataType, String source, 
			Date bottomRowTime, int bottomRowId,
			List<String> filterAttributes, List<String> filterValues, int pageSize)
	{
		s_Logger.debug("getNextPage RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getNextPage(dataType, source, bottomRowTime, bottomRowId, 
						filterAttributes, filterValues, pageSize);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteEvidenceDAO";
					s_Logger.error("getNextPage(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting next page for " + dataType + " through RemoteEvidenceDAO";
				s_Logger.error("getNextPage(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the previous page
	 * of evidence data remotely through the Proxy.
	 */
	@Override
	public List<Evidence> getPreviousPage(String dataType, String source, 
			Date topRowTime, int topRowId, 
			List<String> filterAttributes, List<String> filterValues, int pageSize)
	{
		s_Logger.debug("getPreviousPage RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getPreviousPage(dataType, source, topRowTime, topRowId, 
						filterAttributes, filterValues, pageSize);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteEvidenceDAO";
					s_Logger.error("getPreviousPage(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting previous page for " + dataType + " through RemoteEvidenceDAO";
				s_Logger.error("getPreviousPage(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the page
	 * of evidence data remotely through the Proxy.
	 */
	@Override
	public List<Evidence> getAtTime(String dataType, String source, Date time,
			List<String> filterAttributes, List<String> filterValues, int pageSize)
	{
		s_Logger.debug("getAtTime RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getAtTime(dataType, source, time,
						filterAttributes, filterValues, pageSize);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteEvidenceDAO";
					s_Logger.error("getAtTime(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting page for " + dataType + " through RemoteEvidenceDAO";
				s_Logger.error("getAtTime(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the page
	 * of evidence data remotely through the Proxy.
	 */
	@Override
	public List<Evidence> getIdPage(String dataType, String source, int id, int pageSize)
	{
		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getIdPage(dataType, source, id, pageSize);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteEvidenceDAO";
					s_Logger.error("getIdPage(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting page for " + dataType + " through RemoteEvidenceDAO";
				s_Logger.error("getIdPage(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the first page
	 * of evidence data remotely through the Proxy.
	 */
	@Override
	public List<Evidence> searchFirstPage(String dataType, String source, 
			String containsText, int pageSize)
	{
		s_Logger.debug("searchFirstPage RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().searchFirstPage(dataType, source, containsText, pageSize);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteEvidenceDAO";
					s_Logger.error("searchFirstPage(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting first page for " + dataType + " through RemoteEvidenceDAO";
				s_Logger.error("searchFirstPage(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the first page
	 * of evidence data remotely through the Proxy.
	 */
	@Override
	public List<Evidence> searchLastPage(String dataType, String source, 
			String containsText, int pageSize)
	{
		s_Logger.debug("searchLastPage RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().searchLastPage(dataType, source, containsText, pageSize);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteEvidenceDAO";
					s_Logger.error("searchLastPage(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting last page for " + dataType + " through RemoteEvidenceDAO";
				s_Logger.error("searchLastPage(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the first page
	 * of evidence data remotely through the Proxy.
	 */
	@Override
	public List<Evidence> searchNextPage(String dataType, String source, Date bottomRowTime, 
			int bottomRowId, String containsText, int pageSize)
	{
		s_Logger.debug("searchNextPage RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().searchNextPage(dataType, source, 
						bottomRowTime, bottomRowId, containsText, pageSize);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteEvidenceDAO";
					s_Logger.error("searchNextPage(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting next page for " + dataType + " through RemoteEvidenceDAO";
				s_Logger.error("searchNextPage(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the first page
	 * of evidence data remotely through the Proxy.
	 */
	@Override
	public List<Evidence> searchPreviousPage(String dataType, String source, Date topRowTime,
			int topRowId, String containsText, int pageSize)
	{
		s_Logger.debug("searchPreviousPage RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().searchPreviousPage(dataType, source, 
						topRowTime, topRowId, containsText, pageSize);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteEvidenceDAO";
					s_Logger.error("searchPreviousPage(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting previous page for " + dataType + " through RemoteEvidenceDAO";
				s_Logger.error("searchPreviousPage(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the first page
	 * of evidence data remotely through the Proxy.
	 */
	@Override
	public List<Evidence> searchAtTime(String dataType, String source, 
			Date time, String containsText, int pageSize)
	{
		s_Logger.debug("searchAtTime RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().searchAtTime(dataType, source, time, 
						containsText, pageSize);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteEvidenceDAO";
					s_Logger.error("searchAtTime(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting page for " + dataType + " through RemoteEvidenceDAO";
				s_Logger.error("searchAtTime(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining 
	 * 	evidence data remotely through the Proxy.
	 */
	@Override
	public Evidence getEvidenceSingle(int id)
	{
		s_Logger.debug("getEvidenceSingle RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getEvidenceSingle(id);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteEvidenceDAO";
					s_Logger.error("getEvidenceSingle(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting evidence for id " + id + " through RemoteEvidenceDAO";
				s_Logger.error("getEvidenceSingle(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * Returns all attributes for the item of evidence with the specified id 
	 * (instead of just the current display columns).
	 * @param id the value of the id column for the evidence to obtain information on.
	 * @return List of Attribute objects for the row with the specified id.
	 * 		Note that values for time fields are transported as a String representation
	 * 		of the number of milliseconds since January 1, 1970, 00:00:00 GMT.
	 * @throws ProxyDataAccessException if an error occurs obtaining evidence
	 * 		attributes remotely through the Proxy.
	 */
	@Override
	public List<Attribute> getEvidenceAttributes(int id)
	{
		s_Logger.debug("getEvidenceAttributes RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getEvidenceAttributes(id);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteEvidenceDAO";
					s_Logger.error("getEvidenceAttributes(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting attributes for evidence id " + id + " through RemoteEvidenceDAO";
				s_Logger.error("getEvidenceAttributes(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @return earliest evidence record for the specified properties. Note that the
	 * 	returned evidence will only contain id and time fields.
	 * @throws ProxyDataAccessException if an error occurs obtaining evidence
	 * 		data remotely through the Proxy.
	 */
	@Override
	public Evidence getEarliestEvidence(String dataType, String source,
			List<String> filterAttributes, List<String> filterValues)
	{
		s_Logger.debug("getEarliestEvidence RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getEarliestEvidence(dataType, source, filterAttributes, filterValues);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteEvidenceDAO";
					s_Logger.error("getEarliestEvidence(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting evidence data through RemoteEvidenceDAO";
				s_Logger.error("getEarliestEvidence(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @return latest evidence record for the specified properties. Note that the
	 * 	returned evidence will only contain id and time fields.
	 * @throws ProxyDataAccessException if an error occurs obtaining evidence
	 * 		data remotely through the Proxy.
	 */
	@Override
	public Evidence getLatestEvidence(String dataType, String source,
			List<String> filterAttributes, List<String> filterValues)
	{
		s_Logger.debug("getLatestEvidence RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getLatestEvidence(dataType, source, filterAttributes, filterValues);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteEvidenceDAO";
					s_Logger.error("getLatestEvidence(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting evidence data through RemoteEvidenceDAO";
				s_Logger.error("getLatestEvidence(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @return earliest evidence record containing specified text. Note that the
	 * 	returned evidence will only contain id and time fields.
	 * @throws ProxyDataAccessException if an error occurs obtaining evidence
	 * 		attributes remotely through the Proxy.
	 */
	@Override
	public Evidence searchEarliestEvidence(String dataType, String source, String containsText)
	{
		s_Logger.debug("searchEarliestEvidence RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().searchEarliestEvidence(dataType, source, containsText);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteEvidenceDAO";
					s_Logger.error("searchEarliestEvidence(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting evidence data through RemoteEvidenceDAO";
				s_Logger.error("searchEarliestEvidence(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * @return latest evidence record containing specified text. Note that the
	 * 	returned evidence will only contain id and time fields.
	 * @throws ProxyDataAccessException if an error occurs obtaining evidence
	 * 		attributes remotely through the Proxy.
	 */
	@Override
	public Evidence searchLatestEvidence(String dataType, String source, String containsText)
	{
		s_Logger.debug("searchLatestEvidence RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().searchLatestEvidence(dataType, source, containsText);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteEvidenceDAO";
					s_Logger.error("searchLatestEvidence(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting evidence data through RemoteEvidenceDAO";
				s_Logger.error("searchLatestEvidence(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
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
		s_Logger.debug("getEvidenceInTimeSeries RMI call." );

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getEvidenceInTimeSeries(datatype, metric,
														source, attributes,
														startTime, endTime);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteEvidenceDAO";
					s_Logger.error("getEvidenceInTimeSeries(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting evidence data through RemoteEvidenceDAO";
				s_Logger.error("getEvidenceInTimeSeries(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}


	/**
	 * For a Time Series which can be uniquely defined by <code>datatype</code>,
	 * <code>metric</code> and <code>source</code> this function returns any 
	 * evidence features which occurred between the time period <code>startTime</code>
	 * and <code>endTime</code>.
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
	 * @throws ProxyDataAccessException if an error occurs obtaining evidence
	 * 	data remotely through the Proxy.
	 */
	@Override
	public List<Evidence> getEvidenceInExternalTimeSeries(String datatype, String metric,
												String source, List<Attribute> attributes,
												Date startTime, Date endTime)
	{
		s_Logger.debug("getEvidenceInExternalTimeSeries RMI call." );

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getEvidenceInExternalTimeSeries(datatype, metric,
														source, attributes,
														startTime, endTime);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteEvidenceDAO";
					s_Logger.error("getEvidenceInExternalTimeSeries(): " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting evidence data through RemoteEvidenceDAO";
				s_Logger.error("getEvidenceInExternalTimeSeries(): " + errMsg, e);
				throw new ProxyDataAccessException(errMsg, e);
			}
		}
	}
	
	
	/**
	 * If evidenceId is a time series feature the metric path for 
	 * that time series will be returned or <code>null</code> 
	 * if it can't be found.
	 */
	@Override
	public MetricPath getMetricPathFromEvidenceId(int evidenceId) 
	{
		String debug= "getMetricPathFromEvidenceId({0}) RMI call";
		debug = MessageFormat.format(debug, evidenceId);
		s_Logger.debug(debug);

		boolean alreadyReset = false;
		while (true)
		{
			try
			{
				return getRemoteDAO().getMetricPathFromEvidenceId(evidenceId);
			}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteEvidenceDAO";
					s_Logger.error("getMetricPathFromEvidenceId: " + errMsg, ce);
					throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
			catch (Exception e)
			{
				String errMsg = "Error getting metric path for evidence id = " + evidenceId 
								+ " through RemoteEvidenceDAO";
				s_Logger.error("getMetricPathFromEvidenceId(): " + errMsg, e);
				
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
	private RemoteEvidenceDAO getRemoteDAO() throws AccessException, RemoteException, NotBoundException
	{
		if (m_RemoteDAO != null)
		{
			return m_RemoteDAO;
		}

		RemoteObjectFactoryDAO factory = getRemoteFactory();

		m_RemoteDAO = factory.getEvidenceDAO(getOriginatorName());
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
