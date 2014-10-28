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

package com.prelert.dao.postgresql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.prelert.dao.AggregateEvidenceDAO;
import com.prelert.dao.spring.EvidenceListRowMapper;
import com.prelert.data.TimeFrame;
import com.prelert.data.Evidence;
import com.prelert.server.ServerUtil;


/**
 * Implementation for a PostgreSQL database of the AggregateEvidenceDAO interface  
 * which uses calls to functions to obtain information for history views.
 * @author Pete Harverson
 */
public class AggregateEvidencePostgreSQLDAO extends SimpleJdbcDaoSupport
        implements AggregateEvidenceDAO
{
	
	static Logger logger = Logger.getLogger(AggregateEvidencePostgreSQLDAO.class);
	
	private int m_PageSize = 20;
	

	@Override
	public List<String> getAllColumns(String dataType, TimeFrame timeFrame)
	{
		String qry = "";
		String viewName = "";
    	switch (timeFrame)
    	{
    		case ALL:
    		case WEEK:
    			viewName = "evidence_week_view";
    			break;
    			
    		case DAY:
    			viewName = "evidence_day_view";
    			break;
    			
    		case HOUR:
    			viewName = "evidence_hour_view";
    			break;
    			
    		case MINUTE:
    			
    		default:
    			// N.B. Should not use this DAO for SECOND time frame.
    			viewName = "evidence_minute_view";
    			break;
    	}
		
		RowMapper<String> mapper = new RowMapper<String>(){
			
			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {				
				return rs.getString(1);
            }
		};

		qry="select column_name from information_schema.columns WHERE table_name = ? ORDER BY ordinal_position;";
		return getSimpleJdbcTemplate().query(qry, mapper, viewName);

	}


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public List<Evidence> getFirstPage(String dataType, String source,
	        TimeFrame timeFrame, String description)
	{
		// Function is:	
    	// evidence_drill_down_first_page(dataType, source, timeFrame, description, pageSize)
		String query = "select * from evidence_drill_down_first_page(?, ?, ?, ?, ?)";
		logger.debug("getFirstPage() query: " + query);
		
		return getSimpleJdbcTemplate().queryForObject(query, new EvidenceListRowMapper(),
						dataType, source, timeFrame.toString().toLowerCase(), description, m_PageSize);

	}


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public List<Evidence> getLastPage(String dataType, String source,
	        TimeFrame timeFrame, String description)
	{
		// Function is:	
    	// evidence_drill_down_last_page(dataType, source, timeFrame, description, pageSize)
		String query = "select * from evidence_drill_down_last_page(?, ?, ?, ?, ?)";
		logger.debug("getLastPage() query: " + query);
		
		return getSimpleJdbcTemplate().queryForObject(query, new EvidenceListRowMapper(),
						dataType, source, timeFrame.toString().toLowerCase(), description, m_PageSize);
	}


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public List<Evidence> getNextPage(String dataType, String source,
	        TimeFrame timeFrame, Date bottomRowTime, String bottomRowDesc, boolean isFiltered)
	{
		// Function is:	
    	// evidence_drill_down_next_page(dataType, source, timeFrame, time, bottomRowDesc, isFiltered, pageSize)
		String query = "select * from evidence_drill_down_next_page(?, ?, ?, ?, ?, ?, ?)";
		logger.debug("getNextPage() query: " + query);
		
		return getSimpleJdbcTemplate().queryForObject(query, new EvidenceListRowMapper(), 
				dataType, source, timeFrame.toString().toLowerCase(), 
				bottomRowTime, bottomRowDesc, isFiltered, m_PageSize);
	}
	
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public List<Evidence> getPreviousPage(String dataType, String source,
	        TimeFrame timeFrame, Date topRowTime, String topRowDesc, boolean isFiltered)
	{
		// Function is:	
    	// evidence_drill_down_previous_page(dataType, source, timeFrame, topRowTime, topRowDesc, isFiltered, pageSize)	
		String query = "select * from evidence_drill_down_previous_page(?, ?, ?, ?, ?, ?, ?)";
		logger.debug("getPreviousPage() query: " + query);

		return getSimpleJdbcTemplate().queryForObject(query, new EvidenceListRowMapper(), 
						dataType, source, timeFrame.toString().toLowerCase(), 
						topRowTime, topRowDesc, isFiltered, m_PageSize);
	}
	
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public List<Evidence> getAtTime(String dataType, String source,
	        TimeFrame timeFrame, Date time, String description)
	{
		// Function is:		
		// evidence_drill_down_at_time(dataType, source, timeFrame, time, description, pageSize)
		String query = "select * from evidence_drill_down_at_time(?, ?, ?, ?, ?, ?);";
		
		String debugQuery = "select * from evidence_drill_down_at_time({0}, {1}, {2}, {3}, {4}, {5})";
		debugQuery = MessageFormat.format(
				debugQuery, dataType, source, timeFrame.toString().toLowerCase(), 
				ServerUtil.formatTimeField(time, TimeFrame.SECOND), description, m_PageSize);
		
		return getSimpleJdbcTemplate().queryForObject(query, new EvidenceListRowMapper(), 
				dataType, source, timeFrame.toString().toLowerCase(), 
				time, description, m_PageSize);
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
