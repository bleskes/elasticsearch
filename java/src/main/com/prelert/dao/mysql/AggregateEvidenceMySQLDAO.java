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

package com.prelert.dao.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcDaoSupport;

import com.prelert.dao.AggregateEvidenceDAO;
import com.prelert.dao.spring.EvidenceRowMapper;
import com.prelert.data.TimeFrame;
import com.prelert.data.Evidence;


/**
 * Implementation for a MySQL database of the AggregateEvidenceDAO interface  
 * which predominantly uses calls to stored procedures to obtain information for 
 * history views.
 * @author Pete Harverson
 */
public class AggregateEvidenceMySQLDAO extends SimpleJdbcDaoSupport 
	implements AggregateEvidenceDAO
{
	
	static Logger logger = Logger.getLogger(AggregateEvidenceMySQLDAO.class);
	
	private int m_PageSize = 20;
	

	@Override
	public List<String> getAllColumns(String dataType, TimeFrame timeFrame)
	{
    	String qry = "";
    	switch (timeFrame)
    	{
    		case ALL:
    		case WEEK:
    			qry = "desc evidence_week_view";
    			break;
    			
    		case DAY:
    			qry = "desc evidence_day_view";
    			break;
    			
    		case HOUR:
    			qry = "desc evidence_hour_view";
    			break;
    			
    		case MINUTE:
    		default:
    			// N.B. Should not use this DAO for SECOND time frame.
    			qry = "desc evidence_minute_view";
    			break;
    	}
		
		RowMapper<String> mapper = new RowMapper<String>(){
			
			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {				
				return rs.getString(1);
            }
		};

		return getSimpleJdbcTemplate().query(qry, mapper);
	}
	

	@Override
	public List<Evidence> getFirstPage(String dataType, String source,
	        TimeFrame timeFrame, String description)
	{
		// Query calls a stored procedure in the form:
    	// CALL evidence_drill_down_first_page('apache_logs', 'lon-server1', 
		// 		'minute', 'service has shutdown', 20)
		String query = "CALL evidence_drill_down_first_page(?, ?, ?, ?, ?)";
		logger.debug("getFirstPage() query: " + query);
		
		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(),
						dataType, source, timeFrame.toString().toLowerCase(), 
						description, m_PageSize);
	}


	@Override
	public List<Evidence> getLastPage(String dataType, String source,
	        TimeFrame timeFrame, String description)
	{
		// Query calls a stored procedure in the form:
		// CALL evidence_drill_down_last_page('apache_logs', 'lon-server1', 
		// 		'minute', 'service has shutdown', 20)
		String query = "CALL evidence_drill_down_last_page(?, ?, ?, ?, ?)";
		logger.debug("getLastPage() query: " + query);

		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(), 
				dataType, source, timeFrame.toString().toLowerCase(), description, m_PageSize);

	}


	@Override
	public List<Evidence> getNextPage(String dataType, String source,
	        TimeFrame timeFrame, Date bottomRowTime, String bottomRowDesc, boolean isFiltered)
	{
		// Query calls a stored procedure in the form:
    	// CALL evidence_drill_down_next_page('apache_logs', 'lon-webserver1', 
		// 		'minute', timeForBottomRow, bottom row id, false, 20)				
		String query = "CALL evidence_drill_down_next_page(?, ?, ?, ?, ?, ?, ?)";
		logger.debug("getNextPage() query: " + query);
		
		Timestamp bottomRowTimeStamp = new Timestamp(bottomRowTime.getTime());
		
		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(), 
						dataType, source, timeFrame.toString().toLowerCase(), 
						bottomRowTimeStamp, bottomRowDesc, isFiltered, m_PageSize);

	}


	@Override
	public List<Evidence> getPreviousPage(
			String dataType, String source, TimeFrame timeFrame, Date topRowTime, 
			String topRowDesc, boolean isFiltered)
	{
		// Query calls a stored procedure in the form:
		// CALL evidence_drill_down_previous_page('apache_logs', 'lon-webserver1', 
		// 'minute', timeForTopRow, top row id, false, 20)	
		String query = "CALL evidence_drill_down_previous_page(?, ?, ?, ?, ?, ?, ?)";
		logger.debug("getPreviousPage() query: " + query);
		
		Timestamp topRowTimeStamp = new Timestamp(topRowTime.getTime());

		return getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(), 
				dataType, source, timeFrame.toString().toLowerCase(), 
				topRowTimeStamp, topRowDesc, isFiltered, m_PageSize);
	}
	
	
	@Override
	public List<Evidence> getAtTime(String dataType, String source,
	        TimeFrame timeFrame, Date time, String description)
	{
		// Query calls a stored procedure in the form:
    	// CALL evidence_drill_down_at_time('apache_logs', 'lon-webserver1', 'minute', 
		// time, 'service has shutdown', 20)				
		String query = "CALL evidence_drill_down_at_time(?, ?, ?, ?, ?, ?)";
		
		Timestamp timeStamp = new Timestamp(time.getTime());
		
		String debugQuery = "CALL evidence_drill_down_at_time({0}, {1}, {2}, {3}, {4}, {5})";
		debugQuery = MessageFormat.format(
				debugQuery, dataType, source, timeFrame.toString().toLowerCase(), 
				timeStamp, description, m_PageSize);

		return  getSimpleJdbcTemplate().query(query, new EvidenceRowMapper(), 
						dataType, source, timeFrame.toString().toLowerCase(), 
						timeStamp, description, m_PageSize);
	}

	
	@Override
    public int getPageSize()
    {
	    return m_PageSize;
    }


	@Override
    public void setPageSize(int pageSize)
    {
		m_PageSize = pageSize;
    }

}
