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

package com.prelert.dao.mysql;

import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcDaoSupport;

import com.prelert.dao.IncidentDAO;
import com.prelert.data.CausalityAggregate;
import com.prelert.data.Incident;
import com.prelert.data.MetricTreeNode;
import com.prelert.data.TimeFrame;
import com.prelert.dao.spring.CausalityAggregateRowMapper;
import com.prelert.dao.spring.IncidentRowMapper;
import com.prelert.dao.spring.MetricTreeNodePartialRowMapper;
import com.prelert.server.ServerUtil;


/**
 * Implementation for a MySQL database of the IncidentDAO interface which 
 * predominantly uses calls to stored procedures to obtain incident data.
 * @author Pete Harverson
 */
public class IncidentMySQLDAO extends SimpleJdbcDaoSupport implements IncidentDAO
{
	static Logger s_Logger = Logger.getLogger(IncidentMySQLDAO.class);

	@Override
	public List<Incident> getIncidents(Date minTime, Date maxTime, int anomalyThreshold)
	{
		String query = "CALL incident_timeline(?, ?, ?)";
		
		String debugQuery = "CALL incident_timeline({0}, {1}, {2})";
		debugQuery = MessageFormat.format(debugQuery, 
				ServerUtil.formatTimeField(minTime, TimeFrame.MINUTE), 
				ServerUtil.formatTimeField(maxTime, TimeFrame.MINUTE), 
				anomalyThreshold);
		s_Logger.debug("getIncidents() query: " + debugQuery);

		return getSimpleJdbcTemplate().query(query, 
					new IncidentRowMapper(), minTime, maxTime, anomalyThreshold);
	}
	
	
	@Override
	public List<Incident> getIncidentsAdaptive(Date minTime, Date maxTime, int anomalyThreshold)
	{
		String query = "CALL incident_timeline_adaptive(?, ?, ?)";
		
		String debugQuery = "CALL incident_timeline_adaptive({0}, {1}, {2})";
		debugQuery = MessageFormat.format(debugQuery, 
				ServerUtil.formatTimeField(minTime, TimeFrame.MINUTE), 
				ServerUtil.formatTimeField(maxTime, TimeFrame.MINUTE), 
				anomalyThreshold);
		s_Logger.debug("getIncidentsAdaptive() query: " + debugQuery);

		return getSimpleJdbcTemplate().query(query, 
					new IncidentRowMapper(), minTime, maxTime, anomalyThreshold);
	}
	
	
	@Override
	public List<Incident> getIncidentsInTimeRange(Date minTime, boolean minTimeIsOpen,
			Date maxTime, boolean maxTimeIsOpen,
			Date minFirstTime, boolean minFirstTimeIsOpen,
			Date maxFirstTime, boolean maxFirstTimeIsOpen,
			Date minLastTime, boolean minLastTimeIsOpen,
			Date maxLastTime, boolean maxLastTimeIsOpen,
			Date minUpdateTime, boolean minUpdateTimeIsOpen,
			Date maxUpdateTime, boolean maxUpdateTimeIsOpen,
			int anomalyThreshold,
			String metricPath, String likeMetricPath,
			String escapeChar)
	{
		String query = "CALL from incident_range_api(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		String debugQuery = "CALL from incident_range_api({0}, {1}, {2}, {3}, {4}, {5}, {6}, {7}" +
				"{8}, {9}, {10}, {11}, {12}, {13}, {14}, {15}, {16}, {17}, {18}, {19})";
		debugQuery = MessageFormat.format(debugQuery, minTime, minTimeIsOpen,
				maxTime, maxTimeIsOpen,
				minFirstTime, minFirstTimeIsOpen,
				maxFirstTime, maxFirstTimeIsOpen,
				minLastTime, minLastTimeIsOpen,
				maxLastTime, maxLastTimeIsOpen,
				minUpdateTime, minUpdateTimeIsOpen,
				maxUpdateTime, maxUpdateTimeIsOpen,
				metricPath,
				likeMetricPath, escapeChar,
				anomalyThreshold);
		s_Logger.debug("getIncidentInTimeRange() query: " + debugQuery);

		return getSimpleJdbcTemplate().query(query, new IncidentRowMapper(),
				minTime, minTimeIsOpen,
				maxTime, maxTimeIsOpen,
				minFirstTime, minFirstTimeIsOpen,
				maxFirstTime, maxFirstTimeIsOpen,
				minLastTime, minLastTimeIsOpen,
				maxLastTime, maxLastTimeIsOpen,
				minUpdateTime, minUpdateTimeIsOpen,
				maxUpdateTime, maxUpdateTimeIsOpen,
				metricPath,
				likeMetricPath, escapeChar,
				anomalyThreshold);
	}


	@Override
    public List<Incident> getFirstPage(int anomalyThreshold, int pageSize)
    {
		String query = "CALL incident_first_page(?, ?);";
		
		String debugQuery = "CALL incident_first_page({0}, {1})";
		debugQuery = MessageFormat.format(debugQuery, anomalyThreshold, pageSize);
		s_Logger.debug("getFirstPage() query: " + debugQuery);
				
		return getSimpleJdbcTemplate().query(query, new IncidentRowMapper(), 
				anomalyThreshold, pageSize);
    }


    @Override
    public List<Incident> getLastPage(int anomalyThreshold, int pageSize)
    {
    	String query = "CALL incident_last_page(?, ?);";
		
		String debugQuery = "CALL incident_last_page({0}, {1})";
		debugQuery = MessageFormat.format(debugQuery, anomalyThreshold, pageSize);
		s_Logger.debug("getLastPage() query: " + debugQuery);
				
		return getSimpleJdbcTemplate().query(query, new IncidentRowMapper(), 
				anomalyThreshold, pageSize);
    }


    @Override
    public List<Incident> getNextPage(Date bottomRowTime,
            int bottomRowEvidenceId, int anomalyThreshold, int pageSize)
    {
    	String query = "CALL incident_next_page(?, ?, ?, ?);";
		
		String debugQuery = "CALL incident_next_page({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(debugQuery, 
				bottomRowTime, bottomRowEvidenceId, anomalyThreshold, pageSize);
		s_Logger.debug("getNextPage() query: " + debugQuery);
				
		return getSimpleJdbcTemplate().query(query, new IncidentRowMapper(), 
				bottomRowTime, bottomRowEvidenceId, anomalyThreshold, pageSize);
    }


    @Override
    public List<Incident> getPreviousPage(Date topRowTime,
            int topRowEvidenceId, int anomalyThreshold, int pageSize)
    {
    	String query = "CALL incident_previous_page(?, ?, ?, ?);";
		
		String debugQuery = "CALL incident_previous_page({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(debugQuery, 
				topRowTime, topRowEvidenceId, anomalyThreshold, pageSize);
		s_Logger.debug("getPreviousPage() query: " + debugQuery);
				
		return getSimpleJdbcTemplate().query(query, new IncidentRowMapper(), 
				topRowTime, topRowEvidenceId, anomalyThreshold, pageSize);
    }


	@Override
    public List<Incident> getAtTime(Date time, int anomalyThreshold,
            int pageSize,  boolean orderAscending)
    {
		String query = "CALL incident_at_time(?, ?, ?, ?);";
		
		String debugQuery = "CALL incident_at_time({0}, {1}, {2}, {3})";
		debugQuery = MessageFormat.format(debugQuery, time, anomalyThreshold,
				pageSize, orderAscending);
		s_Logger.debug("getAtTime() query: " + debugQuery);
				
		return getSimpleJdbcTemplate().query(query, new IncidentRowMapper(), 
				time, anomalyThreshold, pageSize, anomalyThreshold);
    }

	
	/**
	 * Returns the date/time of the earliest incident in the database.
	 * @return date/time of earliest incident.
	 */
	@Override
	public Date getEarliestTime()
	{
		String query = "CALL incident_min_time()";
		return getSimpleJdbcTemplate().queryForObject(query, java.sql.Timestamp.class);
	}
	

	/**
	 * Returns the date/time of the latest incident in the database.
	 * @return date/time of latest incident.
	 */
	@Override
	public Date getLatestTime()
	{
		String query = "CALL incident_max_time()";
		return getSimpleJdbcTemplate().queryForObject(query, java.sql.Timestamp.class);
	}


	@Override
	public List<MetricTreeNode> getIncidentMetricPathNodes(int evidenceId)
    {
    	String query = "CALL incident_summary_metric_path_names(?)";
		return getSimpleJdbcTemplate().query(query,
				new MetricTreeNodePartialRowMapper(), evidenceId);
    }


	@Override
    public List<String> getIncidentAttributeNames(int evidenceId)
    {
    	String query = "CALL incident_summary_attribute_names(?)";
		return getSimpleJdbcTemplate().query(query, 
				new SingleColumnRowMapper<String>(), evidenceId);
    }
	
	
    @Override
    public List<String> getIncidentAttributeValues(int evidenceId, String attributeName)
    {
    	String query = "CALL incident_summary_attribute_values(?, ?)";
		return getSimpleJdbcTemplate().query(query, 
				new SingleColumnRowMapper<String>(), evidenceId, attributeName);
    }


	@Override
    public List<CausalityAggregate> getIncidentSummary(int evidenceId,
													String aggregateBy,
													List<String> groupingAttributes)
    {
    	String query = "CALL incident_summary_list(?, ?, ?)";
    	
    	String attributesArg = ServerUtil.prepareFilterArgument(groupingAttributes);
		
		String debugQuery = "CALL incident_summary_list({0}, {1}, {2})";
		debugQuery = MessageFormat.format(debugQuery, evidenceId,
											aggregateBy, attributesArg);
		s_Logger.debug("getIncidentSummary() query: " + debugQuery);

		return getSimpleJdbcTemplate().query(query, new CausalityAggregateRowMapper(), 
				evidenceId, aggregateBy, attributesArg);
    }
	
	
	@Override
    public Incident getIncidentForId(int evidenceId)
    {
	    String query = "CALL incident_for_id(?)";
    	
	    String debugQuery = "CALL incident_for_id({0})";
		debugQuery = MessageFormat.format(debugQuery, evidenceId);
		s_Logger.debug("getIncidentForId() query: " + debugQuery);
		
		Incident incident = null;
		List<Incident> results = getSimpleJdbcTemplate().query(
				query, new IncidentRowMapper(), evidenceId);
		if (results.size() > 0)
		{
			incident = results.get(0);
		}
		
		return incident;
    }

}
