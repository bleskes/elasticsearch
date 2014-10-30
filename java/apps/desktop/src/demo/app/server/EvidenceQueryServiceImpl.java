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

package demo.app.server;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import demo.app.dao.EvidenceDAO;
import demo.app.data.DatePagingLoadConfig;
import demo.app.data.DatePagingLoadResult;
import demo.app.data.EvidencePagingLoadConfig;
import demo.app.data.TimeFrame;
import demo.app.data.gxt.EvidenceModel;
import demo.app.data.gxt.GridRowInfo;
import demo.app.service.EvidenceQueryService;


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
	
	
	public DatePagingLoadResult<EvidenceModel> getFirstPage(EvidencePagingLoadConfig loadConfig)
	{	
		String dataType = loadConfig.getDataType();
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		String source = loadConfig.getSource();
		String filterAttribute = loadConfig.getFilterAttribute();
		String filterValue = loadConfig.getFilterValue();
		
		List<EvidenceModel> evidenceList = m_EvidenceDAO.getFirstPage(
				dataType, source, timeFrame, filterAttribute, filterValue);
		
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		Date startDate = m_EvidenceDAO.getLatestDate(
				dataType, source, filterAttribute, filterValue);
		
		Date endDate = m_EvidenceDAO.getEarliestDate(
				dataType, source, filterAttribute, filterValue);
		
		return new DatePagingLoadResult<EvidenceModel>(evidenceList, timeFrame,
				getFirstRowTime(evidenceList, loadConfig), startDate, endDate);
	}
	
	
	public DatePagingLoadResult<EvidenceModel> getLastPage(EvidencePagingLoadConfig loadConfig)
	{	
		String dataType = loadConfig.getDataType();
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		String source = loadConfig.getSource();
		String filterAttribute = loadConfig.getFilterAttribute();
		String filterValue = loadConfig.getFilterValue();
		
		List<EvidenceModel> evidenceList = m_EvidenceDAO.getLastPage(
				dataType, source, timeFrame, filterAttribute, filterValue);
		
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		Date startDate = m_EvidenceDAO.getLatestDate(
				dataType, source, filterAttribute, filterValue);
		Date endDate = m_EvidenceDAO.getEarliestDate(
				dataType, source, filterAttribute, filterValue);
		
		return new DatePagingLoadResult<EvidenceModel>(evidenceList, timeFrame,
				getFirstRowTime(evidenceList, loadConfig), startDate, endDate);
	}
	
	
	public DatePagingLoadResult<EvidenceModel> getNextPage(
			EvidencePagingLoadConfig loadConfig, String bottomRowId)
	{
		String dataType = loadConfig.getDataType();
		String source = loadConfig.getSource();
		Timestamp bottomRowTimeStamp = new Timestamp(loadConfig.getDate().getTime());
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		String filterAttribute = loadConfig.getFilterAttribute();
		String filterValue = loadConfig.getFilterValue();
		
		List<EvidenceModel> evidenceList = m_EvidenceDAO.getNextPage(
				dataType, source, timeFrame, bottomRowTimeStamp, 
				bottomRowId, filterAttribute, filterValue);
		
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		Date startDate = m_EvidenceDAO.getLatestDate(
				dataType, source, filterAttribute, filterValue);
		Date endDate = m_EvidenceDAO.getEarliestDate(
				dataType, source, filterAttribute, filterValue);
		
		return new DatePagingLoadResult<EvidenceModel>(evidenceList, timeFrame,
				getFirstRowTime(evidenceList, loadConfig), startDate, endDate);
		
	}
	
	
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
		String filterAttribute = loadConfig.getFilterAttribute();
		String filterValue = loadConfig.getFilterValue();
		
		List<EvidenceModel> evidenceList = m_EvidenceDAO.getPreviousPage(
				dataType, source, timeFrame, topRowTime, 
				topRowId, filterAttribute, filterValue);
		
		// If empty, load the first page - the previous button is always enabled.
		if (evidenceList == null || evidenceList.size() == 0)
		{
			evidenceList = m_EvidenceDAO.getFirstPage(dataType, source, timeFrame, 
					filterAttribute, filterValue);
		}
		
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		Date startDate = m_EvidenceDAO.getLatestDate(
				dataType, source, filterAttribute, filterValue);
		Date endDate = m_EvidenceDAO.getEarliestDate(
				dataType, source, filterAttribute, filterValue);
		
		return new DatePagingLoadResult<EvidenceModel>(evidenceList, timeFrame,
				getFirstRowTime(evidenceList, loadConfig), startDate, endDate);

	}
	
	
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
		String filterAttribute = loadConfig.getFilterAttribute();
		String filterValue = loadConfig.getFilterValue();
		
		List<EvidenceModel> evidenceList = m_EvidenceDAO.getForDescription(
				dataType, source, timeFrame, timeStamp, description);
		
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		Date startDate = m_EvidenceDAO.getLatestDate(
				dataType, source, filterAttribute, filterValue);
		Date endDate = m_EvidenceDAO.getEarliestDate(
				dataType, source, filterAttribute, filterValue);
		
		return new DatePagingLoadResult<EvidenceModel>(evidenceList, timeFrame,
				loadConfig.getDate(), startDate, endDate);
	}
	
	
	public DatePagingLoadResult<EvidenceModel> getAtTime(EvidencePagingLoadConfig loadConfig)
	{
		String dataType = loadConfig.getDataType();
		String source = loadConfig.getSource();
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		Timestamp time = new Timestamp(loadConfig.getDate().getTime());
		String filterAttribute = loadConfig.getFilterAttribute();
		String filterValue = loadConfig.getFilterValue();
		
		List<EvidenceModel> evidenceList = m_EvidenceDAO.getAtTime(dataType, source, timeFrame,
				 time, loadConfig.getFilterAttribute(), loadConfig.getFilterValue());
		
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		Date startDate = m_EvidenceDAO.getLatestDate(
				dataType, source, filterAttribute, filterValue);
		Date endDate = m_EvidenceDAO.getEarliestDate(
				dataType, source, filterAttribute, filterValue);
		
		return new DatePagingLoadResult<EvidenceModel>(evidenceList, timeFrame,
				getFirstRowTime(evidenceList, loadConfig), startDate, endDate);
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
    	
		Date startDate = m_EvidenceDAO.getLatestDate(dataType, source, null, null);
		Date endDate = m_EvidenceDAO.getEarliestDate(dataType, source, null, null);
    	
		return new DatePagingLoadResult<EvidenceModel>(evidenceList, timeFrame,
				getFirstRowTime(evidenceList, loadConfig), startDate, endDate);
    }


	/**
	 * Returns the details on the row of evidence with the given id.
	 * @param id the value of the id column for the evidence to obtain information on.
	 * @return List of GridRowInfo objects for the row with the specified id.
	 */
	public List<GridRowInfo> getRowInfo(int rowId)
	{
		return m_EvidenceDAO.getRowInfo(rowId);
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
	 * @return list of all the distinct values in the evidence table for the
	 * 			given column.
	 */
	public List<String> getColumnValues(String dataType, String columnName)
	{
		List<String> columnValues = null;
		if (columnName.equalsIgnoreCase("severity") == false)
		{
			// 16-12-09: TO DO - obtain max rows from the client.
			columnValues = m_EvidenceDAO.getColumnValues(dataType, columnName, 200);
		}
		else
		{
			// For severity just return a hardcoded list of 
			columnValues = Arrays.asList("clear", "unknown", "warning", 
						"minor", "major", "critical");
		}
		
		logger.debug("getColumnValues() returning " + columnValues);
		
		return columnValues;
	}
	
	
	/**
	 * Returns the date of the earliest evidence record in the Prelert database.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @return date of earliest evidence record, or <code>null</code> if the
	 * database contains no evidence records.
	 */
	public Date getEarliestDate(String dataType, String source)
	{
		return m_EvidenceDAO.getEarliestDate(dataType, source, null, null);
	}
	
	
	/**
	 * Returns the date of the latest evidence record in the Prelert database.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @return date of latest evidence record, or <code>null</code> if the
	 * database contains no evidence records.
	 */
	public Date getLatestDate(String dataType, String source)
	{
		return m_EvidenceDAO.getLatestDate(dataType, source, null, null);
	}
	
	
	/**
	 * Returns the time of the first row in the specified results list.
	 * @return the time of the first row, or <code>null</code> if the
	 * supplied list is <code>null</code> or empty.
	 */
	private Date getFirstRowTime(List<EvidenceModel> evidenceList, DatePagingLoadConfig config)
	{
		Date firstRowTime = null;
		if (evidenceList == null || evidenceList.size() == 0)
		{
			return null;
		}
		
		EvidenceModel firstRow = evidenceList.get(0);
		TimeFrame timeFrame = config.getTimeFrame();
		
		// Identify the name of the 'time' column which should have been passed 
		// in the LoadConfig.
		String timeColumnName = "time";
		switch (timeFrame)
		{
			case WEEK:
				timeColumnName = "last_occurred";
				break;
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
