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

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.prelert.dao.EvidenceDAO;
import com.prelert.data.TimeFrame;
import com.prelert.data.gxt.AttributeModel;
import com.prelert.data.gxt.DatePagingLoadResult;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.data.gxt.EvidencePagingLoadConfig;
import com.prelert.service.EvidenceQueryService;


/**
 * Server-side implementation of the service for retrieving evidence data from the
 * Prelert database for display in evidence list views.
 * @author Pete Harverson
 */
public class EvidenceQueryServiceImpl extends RemoteServiceServlet
	implements EvidenceQueryService
{
	static Logger logger = Logger.getLogger(EvidenceQueryServiceImpl.class);
	
	private EvidenceDAO 		m_EvidenceDAO;
	
	
	
	/**
	 * Sets the EvidenceDAO to be used by the evidence query service.
	 * @param evidenceDAO the data access object for evidence views.
	 */
	public void setEvidenceDAO(EvidenceDAO evidenceDAO)
	{
		m_EvidenceDAO = evidenceDAO;
	}
	
	
	/**
	 * Returns the EvidenceViewDAO being used by the evidence query service.
	 * @return the data access object for evidence views.
	 */
	public EvidenceDAO getEvidenceDAO()
	{
		return m_EvidenceDAO;
	}
	
	
	/**
	 * Returns the first page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (i.e. time frame)
	 * to obtain.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> getFirstPage(EvidencePagingLoadConfig loadConfig)
	{	
		String dataType = loadConfig.getDataType();
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		String source = loadConfig.getSource();
		List<String> filterAttributes = loadConfig.getFilterAttributes();
		List<String> filterValues = loadConfig.getFilterValues();
		
		List<EvidenceModel> evidenceList = m_EvidenceDAO.getFirstPage(
				dataType, source, timeFrame, filterAttributes, filterValues);
		
		return createDatePagingLoadResult(evidenceList, timeFrame, 
				dataType, source, filterAttributes, filterValues);
	}
	
	
	/**
	 * Returns the last page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (i.e. time frame)
	 * to obtain.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> getLastPage(EvidencePagingLoadConfig loadConfig)
	{	
		String dataType = loadConfig.getDataType();
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		String source = loadConfig.getSource();
		List<String> filterAttributes = loadConfig.getFilterAttributes();
		List<String> filterValues = loadConfig.getFilterValues();
		
		List<EvidenceModel> evidenceList = m_EvidenceDAO.getLastPage(
				dataType, source, timeFrame, filterAttributes, filterValues);
		
		return createDatePagingLoadResult(evidenceList, timeFrame, 
				dataType, source, filterAttributes, filterValues);
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
	public DatePagingLoadResult<EvidenceModel> getNextPage(
			EvidencePagingLoadConfig loadConfig, String bottomRowId)
	{
		String dataType = loadConfig.getDataType();
		String source = loadConfig.getSource();
		Timestamp bottomRowTimeStamp = new Timestamp(loadConfig.getDate().getTime());
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		List<String> filterAttributes = loadConfig.getFilterAttributes();
		List<String> filterValues = loadConfig.getFilterValues();
		
		List<EvidenceModel> evidenceList = m_EvidenceDAO.getNextPage(
				dataType, source, timeFrame, bottomRowTimeStamp, 
				bottomRowId, filterAttributes, filterValues);
		
		return createDatePagingLoadResult(evidenceList, timeFrame, 
				dataType, source, filterAttributes, filterValues);
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
	public DatePagingLoadResult<EvidenceModel> getPreviousPage(
			EvidencePagingLoadConfig loadConfig, String topRowId)
	{
		// Check if there is a date in the loadConfig (if not, the calling
		// page would have been empty). 
		if (loadConfig.getDate() == null)
		{
			return getFirstPage(loadConfig);
		}
		
		String dataType = loadConfig.getDataType();
		String source = loadConfig.getSource();
		Timestamp topRowTime = new Timestamp(loadConfig.getDate().getTime());
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		List<String> filterAttributes = loadConfig.getFilterAttributes();
		List<String> filterValues = loadConfig.getFilterValues();
		
		List<EvidenceModel> evidenceList = m_EvidenceDAO.getPreviousPage(
				dataType, source, timeFrame, topRowTime, 
				topRowId, filterAttributes, filterValues);
		
		// If empty, load the first page - the previous button is always enabled.
		if (evidenceList == null || evidenceList.size() == 0)
		{
			evidenceList = m_EvidenceDAO.getFirstPage(dataType, source, timeFrame, 
					filterAttributes, filterValues);
		}
		
		return createDatePagingLoadResult(evidenceList, timeFrame, 
				dataType, source, filterAttributes, filterValues);
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
	public DatePagingLoadResult<EvidenceModel> getForDescription(
			EvidencePagingLoadConfig loadConfig, String description)
	{
		// CALL evidence_at_description_minute(
		// timeIn DATETIME, /* The date without seconds e.g. 2009-04-16 14:47:00 */
		// descriptionIn VARCHAR(255));
		String query = "CALL evidence_at_description_minute(?, ?)";
		logger.debug("getForDescription() query: " + query);
		
		String dataType = loadConfig.getDataType();
		String source = loadConfig.getSource();
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		Timestamp timeStamp = new Timestamp(loadConfig.getDate().getTime());
		List<String> filterAttributes = loadConfig.getFilterAttributes();
		List<String> filterValues = loadConfig.getFilterValues();
		
		List<EvidenceModel> evidenceList = m_EvidenceDAO.getForDescription(
				dataType, source, timeFrame, timeStamp, description);
		
		return createDatePagingLoadResult(evidenceList, timeFrame, 
				dataType, source, filterAttributes, filterValues);
	}
	
	
	/**
	 * Returns a page of evidence data, whose top row will match the date in
	 * the supplied config.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig will correspond to the value of the time column 
	 * for the first row of evidence data that will be returned.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> getAtTime(EvidencePagingLoadConfig loadConfig)
	{
		// Check if there is a date in the loadConfig. 
		if (loadConfig.getDate() == null)
		{
			return getFirstPage(loadConfig);
		}
		
		String dataType = loadConfig.getDataType();
		String source = loadConfig.getSource();
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		Timestamp time = new Timestamp(loadConfig.getDate().getTime());
		List<String> filterAttributes = loadConfig.getFilterAttributes();
		List<String> filterValues = loadConfig.getFilterValues();
		
		List<EvidenceModel> evidenceList = m_EvidenceDAO.getAtTime(dataType, source, timeFrame,
				 time, filterAttributes, filterValues);
		
		return createDatePagingLoadResult(evidenceList, timeFrame, 
				dataType, source, filterAttributes, filterValues);
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
    public DatePagingLoadResult<EvidenceModel> getIdPage(EvidencePagingLoadConfig loadConfig, int id)
    {
    	String dataType = loadConfig.getDataType();
    	String source = loadConfig.getSource();
    	
    	List<EvidenceModel> evidenceList = m_EvidenceDAO.getIdPage(dataType, source, id);
    	
    	// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
    	TimeFrame timeFrame = loadConfig.getTimeFrame();
    	
    	return createDatePagingLoadResult(evidenceList, timeFrame, 
				dataType, source, null, null);
    }
    
    
    /**
	 * Runs a search to return the first page of evidence data where one or more
	 * attributes contain the text in the specified load config.
	 * @param config load config specifying the text to search for in evidence attributes.
	 * @return load result containing the first page of matching evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> searchFirstPage(EvidencePagingLoadConfig config)
	{
		String dataType = config.getDataType();
		String source = config.getSource();
		String containsText = config.getContainsText();
		
		List<EvidenceModel> evidenceList = 
			m_EvidenceDAO.searchFirstPage(dataType, source, containsText);
		
		return createDatePagingLoadResult(
				evidenceList, dataType, source, containsText);
	}
	
	
	/**
	 * Runs a search to return the last page of evidence data where one or more
	 * attributes contain the text in the specified load config.
	 * @param config load config specifying the text to search for in evidence attributes.
	 * @return load result containing the last page of matching evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> searchLastPage(EvidencePagingLoadConfig config)
	{
		String dataType = config.getDataType();
		String source = config.getSource();
		String containsText = config.getContainsText();
		
		List<EvidenceModel> evidenceList = 
			m_EvidenceDAO.searchLastPage(dataType, source, containsText);
		
		return createDatePagingLoadResult(
				evidenceList, dataType, source, containsText);
	}
	
	
	/**
	 * Runs a search to return the next page of evidence data, following on from the row
	 * with the specified id, where one or more attributes contain the text in the 
	 * specified load config.
	 * @param config load config specifying the text to search for in evidence attributes.
	 * @param bottomRowId the value of the id column for the bottom row of evidence 
	 * in the current page.
	 * @return load result containing the next page of matching evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> searchNextPage(
			EvidencePagingLoadConfig config, String bottomRowId)
	{
		String dataType = config.getDataType();
		String source = config.getSource();
		Timestamp bottomRowTime = new Timestamp(config.getDate().getTime());
		String containsText = config.getContainsText();
		
		List<EvidenceModel> evidenceList = m_EvidenceDAO.searchNextPage(
				dataType, source, bottomRowTime, Integer.parseInt(bottomRowId), containsText);
		
		return createDatePagingLoadResult(
				evidenceList, dataType, source, containsText);
	}
	
	
	/**
	 * Runs a search to return the previous page of evidence data to the one
	 * with the specified id, where one or more attributes contain the text in the 
	 * specified load config.
	 * @param config load config specifying the text to search for in evidence attributes.
	 * @param topRowId the value of the id column for the top row of evidence 
	 * in the current page.
	 * @return load result containing the previous page of matching evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> searchPreviousPage(
			EvidencePagingLoadConfig config, String topRowId)
	{
		// Check if there is a date in the loadConfig (if not, the calling
		// page would have been empty). 
		if (config.getDate() == null)
		{
			return searchFirstPage(config);
		}
		
		String dataType = config.getDataType();
		String source = config.getSource();
		Timestamp topRowTime = new Timestamp(config.getDate().getTime());
		String containsText = config.getContainsText();
		
		List<EvidenceModel> evidenceList = m_EvidenceDAO.searchPreviousPage(
				dataType, source, topRowTime, Integer.parseInt(topRowId), containsText);
		
		// If empty, load the first page - the previous button is always enabled.
		if (evidenceList == null || evidenceList.size() == 0)
		{
			evidenceList = m_EvidenceDAO.searchFirstPage(dataType, source, containsText);
		}
		
		return createDatePagingLoadResult(
				evidenceList, dataType, source, containsText);
	}
	
	
	/**
	 * Runs a search to return a page of evidence data, whose top row will match the date
	 * in the supplied config and where one or more attributes contain the text in the 
	 * specified load config.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig will correspond to the value of the time column 
	 * for the first row of evidence data that will be returned.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> searchAtTime(EvidencePagingLoadConfig config)
	{
		// Check if there is a date in the loadConfig. 
		if (config.getDate() == null)
		{
			return searchFirstPage(config);
		}
		
		String dataType = config.getDataType();
		String source = config.getSource();
		Timestamp time = new Timestamp(config.getDate().getTime());
		String containsText = config.getContainsText();
		
		List<EvidenceModel> evidenceList = m_EvidenceDAO.searchAtTime(
				dataType, source, time, containsText);
		
		return createDatePagingLoadResult(
				evidenceList, dataType, source, containsText);
	}


	/**
	 * Returns all attributes for the item of evidence with the specified id 
  	 * (instead of just the current display columns).
	 * @param id the value of the id column for the evidence to obtain information on.
	 * @return List of AttributeModel objects for the row with the specified id.
	 * 		Note that values for time fields are transported as a String representation
	 * 		of the number of milliseconds since January 1, 1970, 00:00:00 GMT.
	 */
	public List<AttributeModel> getEvidenceAttributes(int id)
	{
		return m_EvidenceDAO.getEvidenceAttributes(id);
	}
	
	
	/**
	 * Returns the data model for the single item of evidence with the given id.
	 * @param id id of the item of evidence to return.
	 * @return the complete data model for the item of evidence with the given id.
	 */
	public EvidenceModel getEvidenceSingle(int id)
	{
		return m_EvidenceDAO.getEvidenceSingle(id);
	}
	
	
	/**
	 * Returns a list of all of the columns in an Evidence View with the specified
	 * time frame.
	 * @param dataType identifier for the type of evidence data.
	 * @param view time frame for which to return the list of columns.
	 * @return list of all of the columns for an Evidence View with the specified
	 * time frame.
	 */
    public List<String> getAllColumns(String dataType, TimeFrame timeFrame)
    {
    	return m_EvidenceDAO.getAllColumns(dataType, timeFrame);
    }
    
    
	/**
	 * Returns a list of the names of the columns that support filtering.
	 * @param dataType identifier for the type of evidence data.
	 * @param getCompulsory <code>true</code> to return compulsory columns, 
	 * 		<code>false</code> otherwise.
	 * @param getOptional <code>true</code> to return optional columns, 
	 * 		<code>false</code> otherwise.
	 * @return a list of the filterable columns.
	 */
	public List<String> getFilterableColumns(String dataType, 
			boolean getCompulsory, boolean getOptional)
	{
		return m_EvidenceDAO.getFilterableColumns(dataType, getCompulsory, getOptional);
	}


	/**
	 * Returns the list of values in the evidence table for the specified column.
	 * @param dataType identifier for the type of evidence data.
	 * @param columnName name of the column for which to return the values.
	 * @param maxRows maximum number of values to return.
	 * @return list of all the distinct values in the evidence table for the
	 * 			given column.
	 */
	public List<String> getColumnValues(String dataType, String columnName, int maxRows)
	{
		List<String> columnValues = null;
		if (columnName.equalsIgnoreCase("severity") == false)
		{
			columnValues = m_EvidenceDAO.getColumnValues(dataType, columnName, maxRows);
		}
		else
		{
			// For severity just return a hardcoded list of 
			columnValues = Arrays.asList("clear", "unknown", "warning", 
						"minor", "major", "critical");
		}
		
		logger.debug("getColumnValues(" + dataType + "," + columnName + 
				") returning: " + columnValues.size());
		
		return columnValues;
	}
	
	
	/**
	 * Creates a DatePagingLoadResult object for the supplied list of evidence data
	 * and load criteria.
	 * @return the DatePagingLoadResult.
	 */
	protected DatePagingLoadResult<EvidenceModel> createDatePagingLoadResult(
			List<EvidenceModel> evidenceList, TimeFrame timeFrame, 
			String dataType, String source, 
			List<String> filterAttributes, List<String> filterValues)
	{
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		EvidenceModel startEvidence = m_EvidenceDAO.getLatestEvidence(
				dataType, source, filterAttributes, filterValues);
		Date startDate = null;
		if (startEvidence != null)
		{
			startDate = startEvidence.getTime(TimeFrame.SECOND);
		}
		
		EvidenceModel endEvidence = m_EvidenceDAO.getEarliestEvidence(
				dataType, source, filterAttributes, filterValues);
		Date endDate = null;
		if (endEvidence != null)
		{
			endDate = endEvidence.getTime(TimeFrame.SECOND);
		}
		
		Date firstRowTime = null;
		if (evidenceList != null && evidenceList.size() > 0)
		{
			EvidenceModel firstRow = evidenceList.get(0);
			firstRowTime = firstRow.getTime(timeFrame);
		}
		
		// Set the flag indicating if there is an earlier load result for this 
		// paging config. i.e. false if the result set is null/empty or the last
		// row in the result set is the record that is furthest back in time for this config.
		boolean isEarlierResults = false;
		if (evidenceList != null && evidenceList.size() > 0)
		{
			int lastIdInResults = evidenceList.get(evidenceList.size()-1).getId();
			int earliestId = endEvidence.getId();

			if (lastIdInResults != earliestId)
			{
				isEarlierResults = true;
			}
		}
		
		return new DatePagingLoadResult<EvidenceModel>(evidenceList, TimeFrame.SECOND,
				firstRowTime, startDate, endDate, isEarlierResults);
	}
	
	
	/**
	 * Creates a DatePagingLoadResult object for the supplied list of evidence data
	 * and search criteria.
	 * @return the DatePagingLoadResult.
	 */
	protected DatePagingLoadResult<EvidenceModel> createDatePagingLoadResult(
			List<EvidenceModel> evidenceList, String dataType, 
			String source, String containsText)
	{
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		EvidenceModel startEvidence = m_EvidenceDAO.searchLatestEvidence(
				dataType, source, containsText);
		Date startDate = null;
		if (startEvidence != null)
		{
			startDate = startEvidence.getTime(TimeFrame.SECOND);
		}
		
		EvidenceModel endEvidence = m_EvidenceDAO.searchEarliestEvidence(
				dataType, source, containsText);
		Date endDate = null;
		if (endEvidence != null)
		{
			endDate = endEvidence.getTime(TimeFrame.SECOND);
		}
		
		Date firstRowTime = null;
		if (evidenceList != null && evidenceList.size() > 0)
		{
			EvidenceModel firstRow = evidenceList.get(0);
			firstRowTime = firstRow.getTime(TimeFrame.SECOND);
		}
		
		// Set the flag indicating if this is the last result for this paging config.
		// i.e. true if the result set is null/empty or the last row in the result set
		// is the record that is furthest back in time for this config.
		boolean isEarlierResults = false;
		if (evidenceList != null && evidenceList.size() > 0)
		{
			int lastIdInResults = evidenceList.get(evidenceList.size()-1).getId();
			int earliestId = endEvidence.getId();

			if (lastIdInResults != earliestId)
			{
				isEarlierResults = true;
			}
		}
		
		return new DatePagingLoadResult<EvidenceModel>(evidenceList, TimeFrame.SECOND,
				firstRowTime, startDate, endDate, isEarlierResults);
	}
}
