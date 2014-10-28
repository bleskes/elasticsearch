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

package com.prelert.dao.postgresql;

import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.dao.IncidentDAO;
import com.prelert.dao.IncidentRowMapper;
import com.prelert.dao.SpringJdbcTemplateDAO;
import com.prelert.data.Incident;
import com.prelert.data.TimeFrame;
import com.prelert.server.ServerUtil;


/**
 * Implementation for a PostgreSQL database of the IncidentDAO interface which 
 * uses calls to functions to obtain incident data.
 * @author Pete Harverson
 */
public class IncidentPostgreSQLDAO extends SpringJdbcTemplateDAO implements IncidentDAO
{
	static Logger logger = Logger.getLogger(IncidentPostgreSQLDAO.class);
	
	@Override
	public List<Incident> getIncidents(Date minTime, Date maxTime,
	        int anomalyThreshold)
	{
		String query = "select * from incident_timeline_adaptive(?, ?, ?)";
		
		String debugQuery = "select * from incident_timeline_adaptive({0}, {1}, {2})";
		debugQuery = MessageFormat.format(debugQuery, 
				ServerUtil.formatTimeField(minTime, TimeFrame.MINUTE), 
				ServerUtil.formatTimeField(maxTime, TimeFrame.MINUTE), 
				anomalyThreshold);
		logger.debug("getIncidents() query: " + debugQuery);

		return m_SimpleJdbcTemplate.query(query, new IncidentRowMapper(), 
				minTime, maxTime, anomalyThreshold);
	}
	
	
	@Override
	public Date getEarliestTime()
	{
		String query = "select * from incident_min_time()";
		return m_SimpleJdbcTemplate.queryForObject(query, java.sql.Timestamp.class);
	}


	@Override
	public Date getLatestTime()
	{
		String query = "select * from incident_max_time()";
		return m_SimpleJdbcTemplate.queryForObject(query, java.sql.Timestamp.class);
	}

}
