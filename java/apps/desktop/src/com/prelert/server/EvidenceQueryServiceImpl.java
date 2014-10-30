/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.data.SortInfo;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.prelert.dao.*;
import com.prelert.data.*;
import com.prelert.data.gxt.GridRowInfo;
import com.prelert.service.EvidenceQueryService;

import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;


/**
 * Server-side implementation of the service for retrieving evidence data from the
 * Prelert database.
 * @author Pete Harverson
 */
public class EvidenceQueryServiceImpl extends RemoteServiceServlet 
	implements EvidenceQueryService
{
	static Logger logger = Logger.getLogger(EvidenceQueryServiceImpl.class);
	
	private EvidenceViewDAO 	m_EvidenceDAO;
	private TransactionTemplate	m_TxTemplate;
	
	
	/**
	 * Sets the EvidenceViewDAO to be used by the evidence query service.
	 * @param evidenceDAO the data access object for evidence views.
	 */
	public void setEvidenceDAO(EvidenceViewDAO evidenceDAO)
	{
		m_EvidenceDAO = evidenceDAO;
	}
	
	
	/**
	 * Returns the EvidenceViewDAO being used by the evidence query service.
	 * @return the data access object for evidence views.
	 */
	public EvidenceViewDAO getEvidenceDAO()
	{
		return m_EvidenceDAO;
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
	 * Returns a list of all of the columns in an Evidence View with the specified
	 * time frame.
	 * @param dataType identifier for the type of evidence data.
	 * @param timeFrame time frame for which to return the list of columns.
	 * @return list of all of the columns for an Evidence View with the specified
	 * time frame.
	 */
	public List<String> getAllColumns(String dataType, TimeFrame timeFrame)
    {
	    return m_EvidenceDAO.getAllColumns(dataType, timeFrame);
    }
    
    
	/**
	 * Returns the list of values in the evidence table for the specified column.
	 * @param dataType identifier for the type of evidence data.
	 * @param columnName name of the column for which to return the values.
	 * @return list of all the distinct values in the evidence table for the
	 * 			given column.
	 */
	public List<String> getColumnValues(String dataType, String columnName)
	{
		List<String> columnValues = null;
		if (columnName.equalsIgnoreCase("severity") == false)
		{
			// 16-12-09: TO DO - obtain max rows from the client.
			columnValues = m_EvidenceDAO.getColumnValues(dataType, columnName, 100);
		}
		else
		{
			// For severity just return a hardcoded list of 
			columnValues = Arrays.asList("clear", "unknown", "warning", 
						"minor", "major", "critical");
		}
		
		logger.debug("getColumnValues() returning: " + columnValues);
		
		return columnValues;
	}


	/**
	 * Returns the first page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (i.e. time frame)
	 * to obtain.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EventRecord> getFirstPage(EvidencePagingLoadConfig config)
	{
		final String dataType = config.getDataType();
		final TimeFrame timeFrame = config.getTimeFrame();
		final EvidencePagingLoadConfig loadConfig = config;
		
		// Run all the DB queries within a transaction.
		Object pagingLoadResult = m_TxTemplate.execute(new TransactionCallback(){

			@Override
            public Object doInTransaction(TransactionStatus status)
            {
				String filterAttribute = loadConfig.getFilterAttribute();
				String filterValue = loadConfig.getFilterValue();
				List<EventRecord> evidenceList = m_EvidenceDAO.getFirstPage(
						dataType, timeFrame, 
						loadConfig.getFilterAttribute(), loadConfig.getFilterValue());
				
				// Note that the list is ordered in DESCENDING order, so the first page
				// corresponds to the latest date in the database.
				Date startDate = m_EvidenceDAO.getLatestDate(
						dataType, filterAttribute, filterValue);
				Date endDate = m_EvidenceDAO.getEarliestDate(
						dataType, filterAttribute, filterValue);
				
				return new DatePagingLoadResult<EventRecord>(evidenceList, timeFrame,
						getFirstRowTime(evidenceList, loadConfig), startDate, endDate);
            }
		});
		
		return (DatePagingLoadResult<EventRecord>)(pagingLoadResult);
	}
	
	
	/**
	 * Returns the last page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (i.e. time frame)
	 * to obtain.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EventRecord> getLastPage(EvidencePagingLoadConfig config)
	{
		final String dataType = config.getDataType();
		final TimeFrame timeFrame = config.getTimeFrame();
		final EvidencePagingLoadConfig loadConfig = config;
		
		// Run all the DB queries within a transaction.
		Object pagingLoadResult = m_TxTemplate.execute(new TransactionCallback(){
			
			@Override
            public Object doInTransaction(TransactionStatus status)
            {
				String filterAttribute = loadConfig.getFilterAttribute();
				String filterValue = loadConfig.getFilterValue();
				List<EventRecord> evidenceList = m_EvidenceDAO.getLastPage(
						dataType, timeFrame,
						loadConfig.getFilterAttribute(), loadConfig.getFilterValue());
				
				// Note that the list is ordered in DESCENDING order, so the first page
				// corresponds to the latest date in the database.
				Date startDate = m_EvidenceDAO.getLatestDate(
						dataType, filterAttribute, filterValue);
				Date endDate = m_EvidenceDAO.getEarliestDate(
						dataType, filterAttribute, filterValue);
				
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
			EvidencePagingLoadConfig config, String bottomRowId)
	{
		final String dataType = config.getDataType();
		final TimeFrame timeFrame = config.getTimeFrame();
		final Date bottomRowTime = config.getDate();
		final String bottomId = bottomRowId;
		final EvidencePagingLoadConfig loadConfig = config;
		
		// Run all the DB queries within a transaction.
		Object pagingLoadResult = m_TxTemplate.execute(new TransactionCallback(){
			
			@Override
            public Object doInTransaction(TransactionStatus status)
            {
				String filterAttribute = loadConfig.getFilterAttribute();
				String filterValue = loadConfig.getFilterValue();
				List<EventRecord> evidenceList = m_EvidenceDAO.getNextPage(
						dataType, timeFrame, bottomRowTime, bottomId, 
						loadConfig.getFilterAttribute(), loadConfig.getFilterValue());
				
				// Note that the list is ordered in DESCENDING order, so the first page
				// corresponds to the latest date in the database.
				Date startDate = m_EvidenceDAO.getLatestDate(
						dataType, filterAttribute, filterValue);
				Date endDate = m_EvidenceDAO.getEarliestDate(
						dataType, filterAttribute, filterValue);
				
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
			EvidencePagingLoadConfig config, String topRowId)
	{
		// Check if there is a date in the Load Config (if not, the calling
		// page would have been empty). 
		if (config.getDate() == null)
		{
			return getFirstPage(config);
		}
		
		final String dataType = config.getDataType();
		final TimeFrame timeFrame = config.getTimeFrame();
		final Date topRowTime = config.getDate();
		final String topId = topRowId;
		final EvidencePagingLoadConfig loadConfig = config;
		
		// Run all the DB queries within a transaction.
		Object pagingLoadResult = m_TxTemplate.execute(new TransactionCallback(){

			@Override
            public Object doInTransaction(TransactionStatus status)
            {
				String filterAttribute = loadConfig.getFilterAttribute();
				String filterValue = loadConfig.getFilterValue();
				List<EventRecord> evidenceList = m_EvidenceDAO.getPreviousPage(
						dataType, timeFrame, topRowTime, topId, 
						loadConfig.getFilterAttribute(), loadConfig.getFilterValue());
				
				// If empty, load the first page - the previous button is always enabled.
				if (evidenceList == null || evidenceList.size() == 0)
				{
					evidenceList = m_EvidenceDAO.getFirstPage(dataType, timeFrame, 
							loadConfig.getFilterAttribute(), loadConfig.getFilterValue());
				}
				
				// Note that the list is ordered in DESCENDING order, so the first page
				// corresponds to the latest date in the database.
				Date startDate = m_EvidenceDAO.getLatestDate(
						dataType, filterAttribute, filterValue);
				Date endDate = m_EvidenceDAO.getEarliestDate(
						dataType, filterAttribute, filterValue);
				
				return new DatePagingLoadResult<EventRecord>(evidenceList, timeFrame,
						getFirstRowTime(evidenceList, loadConfig), startDate, endDate);
            }
				
		});
		
		return (DatePagingLoadResult<EventRecord>)(pagingLoadResult);
	}
	
	
	/**
	 * Returns a page of evidence data, the top row of which matches the specified
	 * description, and whose time corresponds to the date in the supplied config.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig will correspond to the value of the time column 
	 * for the first row of evidence data that will be returned.
	 * @param description the value of the description column to match on for the
	 * top row of evidence data that will be returned.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EventRecord> getForDescription(
			EvidencePagingLoadConfig config, String description)
	{
		final String dataType = config.getDataType();
		final TimeFrame timeFrame = config.getTimeFrame();
		final Date time = config.getDate();
		final String desc = description;
		final DatePagingLoadConfig loadConfig = config;
		
		// Run all the DB queries within a transaction.
		Object pagingLoadResult = m_TxTemplate.execute(new TransactionCallback(){

			@Override
            public Object doInTransaction(TransactionStatus status)
            {
				List<EventRecord> evidenceList = m_EvidenceDAO.getForDescription(
						dataType, timeFrame, time, desc);
				
				
				// Note that the list is ordered in DESCENDING order, so the first page
				// corresponds to the latest date in the database.
				Date startDate = m_EvidenceDAO.getLatestDate(dataType, null, null);
				Date endDate = m_EvidenceDAO.getEarliestDate(dataType, null, null);
				
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
	public DatePagingLoadResult<EventRecord> getAtTime(EvidencePagingLoadConfig config)
	{
		final String dataType = config.getDataType();
		final TimeFrame timeFrame = config.getTimeFrame();
		final Date time = config.getDate();
		final EvidencePagingLoadConfig loadConfig = config;
		
		// Run all the DB queries within a transaction.
		Object pagingLoadResult = m_TxTemplate.execute(new TransactionCallback(){

			@Override
            public Object doInTransaction(TransactionStatus status)
            {
				String filterAttribute = loadConfig.getFilterAttribute();
				String filterValue = loadConfig.getFilterValue();
				List<EventRecord> evidenceList = m_EvidenceDAO.getAtTime(
						dataType, timeFrame, time,
						loadConfig.getFilterAttribute(), loadConfig.getFilterValue());
				
				// Note that the list is ordered in DESCENDING order, so the first page
				// corresponds to the latest date in the database.
				Date startDate = m_EvidenceDAO.getLatestDate(
						dataType, filterAttribute, filterValue);
				Date endDate = m_EvidenceDAO.getEarliestDate(
						dataType, filterAttribute, filterValue);
				
				return new DatePagingLoadResult<EventRecord>(evidenceList, timeFrame,
						getFirstRowTime(evidenceList, loadConfig), startDate, endDate);
            }
				
		});
		
		return (DatePagingLoadResult<EventRecord>)(pagingLoadResult);
	}
	
	
	/**
	 * Returns a page of evidence data, the top row of which matches the specified id.
	 * @param config load config specifying the range of data to obtain 
	 * 	e.g. the time frame.
	 * @param id evidence id for the top row of evidence data that will be returned.
	 * @return load result containing the requested evidence data. The date
	 * in this DatePagingLoadResult will correspond to the value of the time column 
	 * for the first row of evidence data that will be returned.
	 */
    public DatePagingLoadResult<EventRecord> getIdPage(EvidencePagingLoadConfig config, int id)
    {
    	final String dataType = config.getDataType();
    	final int evidenceId = id;
		final TimeFrame timeFrame = config.getTimeFrame();
		final EvidencePagingLoadConfig loadConfig = config;
		
		// Run all the DB queries within a transaction.
		Object pagingLoadResult = m_TxTemplate.execute(new TransactionCallback(){

			@Override
            public Object doInTransaction(TransactionStatus status)
            {
				List<EventRecord> evidenceList = m_EvidenceDAO.getIdPage(dataType, evidenceId);	
				
				// Note that the list is ordered in DESCENDING order, so the first page
				// corresponds to the latest date in the database.
				Date startDate = m_EvidenceDAO.getLatestDate(dataType, null, null);
				Date endDate = m_EvidenceDAO.getEarliestDate(dataType, null, null);
				
				return new DatePagingLoadResult<EventRecord>(evidenceList, timeFrame,
						getFirstRowTime(evidenceList, loadConfig), startDate, endDate);
            }
				
		});
		
		return (DatePagingLoadResult<EventRecord>)(pagingLoadResult);
    }
	
	
	/**
	 * Returns the details on the row of evidence with the given id.
	 * @param id the value of the id column for the row to obtain information on.
	 * @return List of GridRowInfo objects for the row with the specified id.
	 */
	public List<GridRowInfo> getRowInfo(int id)
	{
		return m_EvidenceDAO.getRowInfo(id);
	}
	
	
	/**
	 * Returns the date of the earliest evidence record in the Prelert database.
	 * @param dataType identifier for the type of evidence data.
	 * @return date of earliest evidence record, or <code>null</code> if the
	 * database contains no evidence records.
	 */
	public Date getEarliestDate(String dataType)
	{
		return m_EvidenceDAO.getEarliestDate(dataType, null, null);
	}
	
	
	/**
	 * Returns the date of the latest evidence record in the Prelert database.
	 * @param dataType identifier for the type of evidence data.
	 * @return date of latest evidence record, or <code>null</code> if the
	 * database contains no evidence records.
	 */
	public Date getLatestDate(String dataType)
	{
		return m_EvidenceDAO.getLatestDate(dataType, null, null);
	}
	
	
	/**
	 * Returns the time of the first row in the specified results list.
	 * @return the time of the first row, or <code>null</code> if the supplied
	 * list is <code>null</code> or empty.
	 */
	private Date getFirstRowTime(List<EventRecord> evidenceList, DatePagingLoadConfig config)
	{
		Date firstRowTime = null;
		if (evidenceList == null || evidenceList.size() == 0)
		{
			return null;
		}
		
		EventRecord firstRow = evidenceList.get(0);
		
		TimeFrame timeFrame = config.getTimeFrame();
		
		// Identify the name of the 'time' column which should have been passed 
		// in the LoadConfig.
		String timeColumnName = "time";
		SortInfo configSortInfo = config.getSortInfo();
		if (configSortInfo != null)
		{
			timeColumnName = configSortInfo.getSortField();
		}
		else
		{
			switch (timeFrame)
			{
				case DAY:
					timeColumnName = "day";
					break;
				case HOUR:
					timeColumnName = "hour";
					break;
				case MINUTE:
					timeColumnName = "minute";
					break;
				case SECOND:
				default:
					timeColumnName = "time";
					break;
			}
		}
			
		try
        {
			SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			switch (timeFrame)
			{
				case DAY:
					dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
					break;
				case HOUR:
					dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:00-59");
					break;
				case MINUTE:
					dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
					break;
				case SECOND:
					dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					break;
			}
			
			firstRowTime = dateFormatter.parse((String)firstRow.get(timeColumnName));
        }
        catch (ParseException e)
        {
	        logger.debug("getFirstRowTime() - unable to parse time value to Date: " + firstRow.get(timeColumnName));
        }
        
        return firstRowTime;
	}
	
	
}
