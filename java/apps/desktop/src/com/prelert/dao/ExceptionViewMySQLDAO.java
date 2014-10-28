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

package com.prelert.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import com.prelert.data.EventRecord;
import com.prelert.data.TimeFrame;


public class ExceptionViewMySQLDAO extends SpringJdbcTemplateDAO implements ExceptionViewDAO
{
	static Logger logger = Logger.getLogger(ExceptionViewMySQLDAO.class);
	
	static SimpleDateFormat s_LogDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	
	/**
	 * Returns a list of all of the column names to be displayed in an Exception List.
	 * @param dataType identifier for the type of evidence data.
	 * @return List of column names for an Exception List.
	 */
	public List<String> getAllColumns(String dataType)
	{
		// PROC is: call display_columns(type, getCompulsory, getOptional)
		String qry = "call display_columns(?, 1, 1)";
		logger.debug("getAllColumns() query: " + qry);
		
		ParameterizedRowMapper<String> mapper = new ParameterizedRowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString(1);
            }
		};
		
		return m_SimpleJdbcTemplate.query(qry, mapper, dataType);
	}
	
	
	/**
	 * Returns the first page of evidence data for a view with the specified
	 * time, noise level and time window.
	 * @param dataType identifier for the type of evidence data.
	 * @param time time defining the start and end time of the exception time
	 * 		window.
	 * @param noiseLevel noise filter, where a high value represents the most
	 * 		likely exceptions and a low value indicates all data items.
	 * @param timeWindow time window for the exception calculation 
	 * 		e.g. MINUTE, HOUR or DAY.
	 * @return List of Evidence records comprising the first page of data.
	 */
	public List<EventRecord> getFirstPage(
			String dataType, Date time, int noiseLevel, TimeFrame timeWindow)
	{
		String query = "CALL exception_first_page(?, ?, ?, ?)";
		
		Timestamp timeStamp = null;
		if (time != null)
		{
			timeStamp = new Timestamp(time.getTime());
		}
		
		String debugQuery = "CALL exception_first_page({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(
				debugQuery, dataType, s_LogDateFormatter.format(timeStamp), 
				noiseLevel, timeWindow.toString());
		logger.debug("getFirstPage() query: " + debugQuery);
		
		List<EventRecord> evidenceList = m_SimpleJdbcTemplate.query(
			query, new EventRecordRowMapper(), 
			dataType, timeStamp, noiseLevel, timeWindow.toString());
		
		return evidenceList;
	}
	
	
	/**
	 * Returns the last page of evidence data for a view with the specified
	 * time, noise level and time window.
	 * @param dataType identifier for the type of evidence data.
	 * @param time time defining the start and end time of the exception time
	 * 		window.
	 * @param noiseLevel noise filter, where a high value represents the most
	 * 		likely exceptions and a low value indicates all data items.
	 * @param timeWindow time window for the exception calculation 
	 * 		e.g. MINUTE, HOUR or DAY.
	 * @return List of Evidence records comprising the last page of data.
	 */
	public List<EventRecord> getLastPage(
			String dataType, Date time, int noiseLevel, TimeFrame timeWindow)
	{
		String query = "CALL exception_last_page(?, ?, ?, ?)";
		
		Timestamp timeStamp = null;
		if (time != null)
		{
			timeStamp = new Timestamp(time.getTime());
		}
		
		String debugQuery = "CALL exception_last_page({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(
				debugQuery, dataType, s_LogDateFormatter.format(timeStamp), 
				noiseLevel, timeWindow.toString());
		logger.debug("getLastPage() query: " + debugQuery);
		
		List<EventRecord> evidenceList = m_SimpleJdbcTemplate.query(
			query, new EventRecordRowMapper(), 
			dataType, timeStamp, noiseLevel, timeWindow.toString());
		
		return evidenceList;
	}
	
	
	/**
	 * Returns the next page of evidence data following on from the row with the
	 * specified time and id.
	 * @param dataType identifier for the type of evidence data.
	 * @param bottomRowTime the value of the time column for the bottom row of 
	 * evidence in the current page.
	 * @param bottomRowId the value of the id column for the bottom row of evidence 
	 * in the current page.
	 * @param noiseLevel noise filter, where a high value represents the most
	 * 		likely exceptions and a low value indicates all data items.
	 * @param timeWindow time window for the exception calculation 
	 * 		e.g. MINUTE, HOUR or DAY.
	 * @return List of Evidence records comprising the next page of data.
	 */
	public List<EventRecord> getNextPage(
			String dataType, Date bottomRowTime, String bottomRowId,
			int noiseLevel, TimeFrame timeWindow)
	{
		String query = "CALL exception_next_page(?, ?, ?, ?, ?)";
		
		Timestamp bottomRowTimeStamp = new Timestamp(bottomRowTime.getTime());
		
		String debugQuery = "CALL exception_next_page({0}, {1}, {2}, {3}, {4})";
		debugQuery = MessageFormat.format(debugQuery, dataType, 
				s_LogDateFormatter.format(bottomRowTimeStamp), 
				Integer.parseInt(bottomRowId), noiseLevel, timeWindow.toString());
		logger.debug("getNextPage() query: " + debugQuery);


		List<EventRecord> evidenceList = m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), 
						dataType, bottomRowTimeStamp, Integer.parseInt(bottomRowId), 
						noiseLevel, timeWindow.toString());

		return evidenceList;
	}
	
	
	/**
	 * Returns the previous page of evidence data to the row with the specified 
	 * time and id.
	 * @param dataType identifier for the type of evidence data.
	 * @param topRowTime the value of the time column for the top row of 
	 * evidence in the current page.
	 * @param topRowId the value of the id column for the top row of evidence 
	 * in the current page.
	 * @param noiseLevel noise filter, where a high value represents the most
	 * 		likely exceptions and a low value indicates all data items.
	 * @param timeWindow time window for the exception calculation 
	 * 		e.g. MINUTE, HOUR or DAY.
	 * @return List of Evidence records comprising the previous page of data.
	 */
	public List<EventRecord> getPreviousPage(
			String dataType, Date topRowTime, String topRowId,
			int noiseLevel, TimeFrame timeWindow)
	{
		String query = "CALL exception_previous_page(?, ?, ?, ?, ?)";
		
		Timestamp topRowTimeStamp = new Timestamp(topRowTime.getTime());
		
		String debugQuery = "CALL exception_previous_page({0}, {1}, {2}, {3}, {4})";
		debugQuery = MessageFormat.format(debugQuery, dataType,
				s_LogDateFormatter.format(topRowTimeStamp), 
				Integer.parseInt(topRowId), noiseLevel, timeWindow.toString());
		logger.debug("getPreviousPage() query: " + debugQuery);

		List<EventRecord> evidenceList = m_SimpleJdbcTemplate.query(query, new EventRecordRowMapper(), 
				dataType, topRowTimeStamp, Integer.parseInt(topRowId), 
				noiseLevel, timeWindow.toString());

		return evidenceList;
	}
	
	
	/**
	 * Returns a page of evidence data, whose top row will match the supplied time.
	 * @param dataType identifier for the type of evidence data.
	 * @param time value of the time column to match on for the first row of 
	 * evidence data that will be returned.
	 * @param noiseLevel noise filter, where a high value represents the most
	 * 		likely exceptions and a low value indicates all data items.
	 * @param timeWindow time window for the exception calculation 
	 * 		e.g. MINUTE, HOUR or DAY.
	 * @return List of Evidence records, sorted descending on time.
	 */
	public List<EventRecord> getAtTime(
			String dataType, Date time, int noiseLevel, TimeFrame timeWindow)
	{
		String query = "CALL exception_at_time(?, ?, ?, ?)";
		
		Timestamp timeStamp = new Timestamp(time.getTime());	
		
		String debugQuery = "CALL exception_at_time({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(debugQuery, dataType, 
				s_LogDateFormatter.format(timeStamp), 
				noiseLevel, timeWindow.toString());
		logger.debug("getAtTime() query: " + debugQuery);
		
		List<EventRecord> evidenceList = m_SimpleJdbcTemplate.query(query, 
				new EventRecordRowMapper(), 
				dataType, timeStamp, noiseLevel, timeWindow.toString());
		
		return evidenceList;
	}
	
	
	/**
	 * Returns the date of the earliest evidence record in the Prelert database
	 * for the given time, noise level and time window.
	 * @param dataType identifier for the type of evidence data.
	 * @param time time defining the start and end time of the exception time
	 * 		window.
	 * @param noiseLevel noise filter, where a high value represents the most
	 * 		likely exceptions and a low value indicates all data items.
	 * @param timeWindow time window for the exception calculation 
	 * 		e.g. MINUTE, HOUR or DAY.
	 * @return date of earliest evidence record.
	 */
	public Date getEarliestDate(
			String dataType, Date time, int noiseLevel, TimeFrame timeWindow)
	{
		String query = "CALL exception_min_time(?, ?, ?, ?)";
		
		Timestamp timeStamp = null;
		if (time != null)
		{
			timeStamp = new Timestamp(time.getTime());	
		}
		
		return m_SimpleJdbcTemplate.queryForObject(query.toString(), 
				java.sql.Timestamp.class, dataType, 
				timeStamp, noiseLevel, timeWindow.toString());
	}

	
	/**
	 * Returns the date of the latest evidence record in the Prelert database
	 * for the given time, noise level and time window.
	 * @param dataType identifier for the type of evidence data.
	 * @param time time defining the start and end time of the exception time
	 * 		window.
	 * @param noiseLevel noise filter, where a high value represents the most
	 * 		likely exceptions and a low value indicates all data items.
	 * @param timeWindow time window for the exception calculation 
	 * 		e.g. MINUTE, HOUR or DAY.
	 * @return date of latest evidence record.
	 */
	public Date getLatestDate(
			String dataType, Date time, int noiseLevel, TimeFrame timeWindow)
	{
		String query = "CALL exception_max_time(?, ?, ?, ?)";
		
		Timestamp timeStamp = null;
		if (time != null)
		{
			timeStamp = new Timestamp(time.getTime());	
		}	
		
		return m_SimpleJdbcTemplate.queryForObject(query.toString(), 
				java.sql.Timestamp.class, dataType, 
				timeStamp, noiseLevel, timeWindow.toString());
	}
}
