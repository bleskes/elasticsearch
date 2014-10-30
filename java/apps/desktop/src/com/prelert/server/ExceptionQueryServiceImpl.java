/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2009     *
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

package com.prelert.server;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.prelert.dao.ExceptionViewDAO;
import com.prelert.data.DatePagingLoadConfig;
import com.prelert.data.DatePagingLoadResult;
import com.prelert.data.EventRecord;
import com.prelert.data.ExceptionPagingLoadConfig;
import com.prelert.data.TimeFrame;
import com.prelert.service.ExceptionQueryService;



public class ExceptionQueryServiceImpl extends RemoteServiceServlet
	implements ExceptionQueryService
{
	static Logger logger = Logger.getLogger(ExceptionQueryServiceImpl.class);
	
	private ExceptionViewDAO 		m_ExceptionDAO;
	private TransactionTemplate	m_TxTemplate;
	
	
	/**
	 * Sets the EvidenceDAO to be used by the evidence query service.
	 * @param evidenceDAO the data access object for evidence views.
	 */
	public void setExceptionDAO(ExceptionViewDAO evidenceDAO)
	{
		m_ExceptionDAO = evidenceDAO;
	}
	
	
	/**
	 * Returns the EvidenceViewDAO being used by the evidence query service.
	 * @return the data access object for evidence views.
	 */
	public ExceptionViewDAO getExceptionDAO()
	{
		return m_ExceptionDAO;
	}
	
	
	/**
	 * Sets the transaction manager to be used when running queries and updates
	 * to the Prelert database within transactions.
	 * @param txManager Spring PlatformTransactionManager to manage database transactions.
	 */
	public void setTransactionManager(PlatformTransactionManager txManager)
	{
		m_TxTemplate = new TransactionTemplate(txManager);
		m_TxTemplate.setReadOnly(true);
		m_TxTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
	}
	
	
	/**
	 * Returns the first page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (i.e. noise level
	 * 		and time window) to obtain.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EventRecord> getFirstPage(ExceptionPagingLoadConfig config)
	{
		final ExceptionPagingLoadConfig loadConfig = config;
		
		// Run all the DB queries within a transaction.
		Object pagingLoadResult = m_TxTemplate.execute(new TransactionCallback(){

			@Override
            public Object doInTransaction(TransactionStatus status)
            {
				String dataType = loadConfig.getDataType();
				Date time = loadConfig.getDate();
				int noiseLevel = loadConfig.getNoiseLevel();
				TimeFrame timeWindow = loadConfig.getTimeWindow();
				
				List<EventRecord> evidenceList = m_ExceptionDAO.getFirstPage(
						dataType, time, noiseLevel, timeWindow);
				
				// Note that the list is ordered in DESCENDING order, so the first page
				// corresponds to the latest date in the database.
				TimeFrame timeFrame = loadConfig.getTimeFrame();
				Date startDate = m_ExceptionDAO.getLatestDate(
						dataType, time, noiseLevel, timeWindow);
				Date endDate = m_ExceptionDAO.getEarliestDate(
						dataType, time,noiseLevel, timeWindow);
				
				return new DatePagingLoadResult<EventRecord>(evidenceList, timeFrame,
						getFirstRowTime(evidenceList, loadConfig), startDate, endDate);
            }
		});
		
		return (DatePagingLoadResult<EventRecord>)(pagingLoadResult);
		
	}
	
	
	/**
	 * Returns the last page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (i.e. noise level
	 * 		and time window) to obtain.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EventRecord> getLastPage(ExceptionPagingLoadConfig config)
	{		
		final ExceptionPagingLoadConfig loadConfig = config;
		
		// Run all the DB queries within a transaction.
		Object pagingLoadResult = m_TxTemplate.execute(new TransactionCallback(){

			@Override
            public Object doInTransaction(TransactionStatus status)
            {
				String dataType = loadConfig.getDataType();
				Date time = loadConfig.getDate();
				int noiseLevel = loadConfig.getNoiseLevel();
				TimeFrame timeWindow = loadConfig.getTimeWindow();
				
				List<EventRecord> evidenceList = m_ExceptionDAO.getLastPage(
						dataType, time, noiseLevel, timeWindow);
				
				// Note that the list is ordered in DESCENDING order, so the first page
				// corresponds to the latest date in the database.
				TimeFrame timeFrame = loadConfig.getTimeFrame();
				Date startDate = m_ExceptionDAO.getLatestDate(
						dataType, time, noiseLevel, timeWindow);
				Date endDate = m_ExceptionDAO.getEarliestDate(
						dataType, time, noiseLevel, timeWindow);
				
				return new DatePagingLoadResult<EventRecord>(evidenceList, timeFrame,
						getFirstRowTime(evidenceList, loadConfig), startDate, endDate);
            }
		});
		
		return (DatePagingLoadResult<EventRecord>)(pagingLoadResult);
	}
	
	
	/**
	 * Returns the next page of evidence data following on from the row with the
	 * specified id.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig must correspond to the value of the time column 
	 * for the bottom row of evidence in the current page.
	 * @param bottomRowId the value of the id column for the bottom row of evidence 
	 * in the current page.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EventRecord> getNextPage(
			ExceptionPagingLoadConfig config, String bottomRowId)
	{	
		final Date bottomRowTime = config.getDate();
		final String bottomId = bottomRowId;
		final ExceptionPagingLoadConfig loadConfig = config;
		
		// Run all the DB queries within a transaction.
		Object pagingLoadResult = m_TxTemplate.execute(new TransactionCallback(){

			@Override
            public Object doInTransaction(TransactionStatus status)
            {
				String dataType = loadConfig.getDataType();
				int noiseLevel = loadConfig.getNoiseLevel();
				TimeFrame timeWindow = loadConfig.getTimeWindow();
				
				List<EventRecord> evidenceList = m_ExceptionDAO.getNextPage(
						dataType, bottomRowTime, bottomId, noiseLevel, timeWindow);
				
				// Note that the list is ordered in DESCENDING order, so the first page
				// corresponds to the latest date in the database.
				TimeFrame timeFrame = loadConfig.getTimeFrame();
				Date startDate = m_ExceptionDAO.getLatestDate(
						dataType, bottomRowTime, noiseLevel, timeWindow);
				Date endDate = m_ExceptionDAO.getEarliestDate(
						dataType, bottomRowTime, noiseLevel, timeWindow);
				
				return new DatePagingLoadResult<EventRecord>(evidenceList, timeFrame,
						getFirstRowTime(evidenceList, loadConfig), startDate, endDate);
            }
		});
		
		return (DatePagingLoadResult<EventRecord>)(pagingLoadResult);
		
	}
	
	
	/**
	 * Returns the previous page of evidence data to the row with the specified id.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig must correspond to the value of the time column 
	 * for the top row of evidence in the current page.
	 * @param topRowId the value of the id column for the top row of evidence 
	 * in the current page.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EventRecord> getPreviousPage(
			ExceptionPagingLoadConfig config, String topRowId)
	{	
		final Date topRowTime = config.getDate();
		final String topId = topRowId;
		final ExceptionPagingLoadConfig loadConfig = config;
		
		// Run all the DB queries within a transaction.
		Object pagingLoadResult = m_TxTemplate.execute(new TransactionCallback(){

			@Override
            public Object doInTransaction(TransactionStatus status)
            {
				String dataType = loadConfig.getDataType();
				int noiseLevel = loadConfig.getNoiseLevel();
				TimeFrame timeWindow = loadConfig.getTimeWindow();
				
				List<EventRecord> evidenceList = m_ExceptionDAO.getPreviousPage(
						dataType, topRowTime, topId, noiseLevel, timeWindow);
				
				// If empty, load the first page - the previous button is always enabled.
				if (evidenceList == null || evidenceList.size() == 0)
				{
					evidenceList = m_ExceptionDAO.getFirstPage(
							dataType, topRowTime, noiseLevel, timeWindow);
				}
				
				// Note that the list is ordered in DESCENDING order, so the first page
				// corresponds to the latest date in the database.
				TimeFrame timeFrame = loadConfig.getTimeFrame();
				Date startDate = m_ExceptionDAO.getLatestDate(
						dataType, topRowTime, noiseLevel, timeWindow);
				Date endDate = m_ExceptionDAO.getEarliestDate(
						dataType, topRowTime, noiseLevel, timeWindow);
				
				return new DatePagingLoadResult<EventRecord>(evidenceList, timeFrame,
						getFirstRowTime(evidenceList, loadConfig), startDate, endDate);
            }
		});
		
		return (DatePagingLoadResult<EventRecord>)(pagingLoadResult);
	}
	
	
	/**
	 * Returns a page of evidence data, whose top row will match the date in
	 * the supplied config.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig will correspond to the value of the time column 
	 * for the first row of evidence data that will be returned.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EventRecord> getAtTime(ExceptionPagingLoadConfig config)
	{	
		final ExceptionPagingLoadConfig loadConfig = config;
		
		// Run all the DB queries within a transaction.
		Object pagingLoadResult = m_TxTemplate.execute(new TransactionCallback(){

			@Override
            public Object doInTransaction(TransactionStatus status)
            {
				String dataType = loadConfig.getDataType();
				Date time = loadConfig.getDate();
				int noiseLevel = loadConfig.getNoiseLevel();
				TimeFrame timeWindow = loadConfig.getTimeWindow();
				
				List<EventRecord> evidenceList = m_ExceptionDAO.getAtTime(
						dataType, time, noiseLevel, timeWindow);
				
				// Note that the list is ordered in DESCENDING order, so the first page
				// corresponds to the latest date in the database.
				TimeFrame timeFrame = loadConfig.getTimeFrame();
				Date startDate = m_ExceptionDAO.getLatestDate(
						dataType, time, noiseLevel, timeWindow);
				Date endDate = m_ExceptionDAO.getEarliestDate(
						dataType, time, noiseLevel, timeWindow);
				
				return new DatePagingLoadResult<EventRecord>(evidenceList, timeFrame,
						getFirstRowTime(evidenceList, loadConfig), startDate, endDate);
            }
		});
		
		return (DatePagingLoadResult<EventRecord>)(pagingLoadResult);
	}
	
	
	/**
	 * Returns a list of all of the columns in an Exception List.
	 * @param dataType identifier for the type of evidence data.
	 * @return list of all of the columns for an Exception List.
	 */
	public List<String> getAllColumns(String dataType)
	{
		return m_ExceptionDAO.getAllColumns(dataType);
	}
	
	
	/**
	 * Returns the time of the first row in the specified results list.
	 * @return the time of the first row, or <code>null</code> if the
	 * supplied list is <code>null</code> or empty.
	 */
	private Date getFirstRowTime(List<EventRecord> evidenceList, DatePagingLoadConfig config)
	{
		Date firstRowTime = null;
		if (evidenceList == null || evidenceList.size() == 0)
		{
			return null;
		}
		
		EventRecord firstRow = evidenceList.get(0);
		String timeStr = firstRow.getTime(TimeFrame.SECOND);
			
		try
        {
			SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			firstRowTime = dateFormatter.parse(timeStr);
        }
        catch (ParseException e)
        {
	        logger.debug("getFirstRowTime() - unable to parse time value to Date: " + timeStr);
        }
        
        return firstRowTime;
	}

}
