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

package com.prelert.dao.mysql;

import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.dao.*;
import com.prelert.data.Incident;
import com.prelert.data.TimeFrame;
import com.prelert.server.ServerUtil;


/**
 * Implementation for a MySQL database of the IncidentDAO interface which 
 * predominantly uses calls to stored procedures to obtain incident data.
 * @author Pete Harverson
 */
public class IncidentMySQLDAO extends SpringJdbcTemplateDAO implements IncidentDAO
{
	static Logger logger = Logger.getLogger(IncidentMySQLDAO.class);
	

	/**
	 * Returns a list of incidents for the specified time window and anomaly threshold.
	 * @param minTime minimum (earliest) date/time to include.
	 * @param maxTime maximum (latest) date/time to include.
	 * @param anomalyThreshold minimum anomaly score threshold of incidents to return,
	 * 	which should be a value between 1 and 100. A value of 1 will return all incidents,
	 * 	whilst a value of 100 will return only the most infrequent (most 'anomalous') incidents.
	 * @return a list of incidents matching the specified criteria.
	 */
	@Override
	public List<Incident> getIncidents(Date minTime, Date maxTime, int anomalyThreshold)
	{
		String query = "CALL incident_timeline_adaptive(?, ?, ?)";
		
		String debugQuery = "CALL incident_timeline_adaptive({0}, {1}, {2})";
		debugQuery = MessageFormat.format(debugQuery, 
				ServerUtil.formatTimeField(minTime, TimeFrame.MINUTE), 
				ServerUtil.formatTimeField(maxTime, TimeFrame.MINUTE), 
				anomalyThreshold);
		logger.debug("getIncidents() query: " + debugQuery);

		return m_SimpleJdbcTemplate.query(query, 
					new IncidentRowMapper(), minTime, maxTime, anomalyThreshold);
	}

	
	/**
	 * Returns the date/time of the earliest incident in the database.
	 * @return date/time of earliest incident.
	 */
	@Override
	public Date getEarliestTime()
	{
		String query = "CALL incident_min_time()";
		return m_SimpleJdbcTemplate.queryForObject(query, java.sql.Timestamp.class);
	}
	

	/**
	 * Returns the date/time of the latest incident in the database.
	 * @return date/time of latest incident.
	 */
	@Override
	public Date getLatestTime()
	{
		String query = "CALL incident_max_time()";
		return m_SimpleJdbcTemplate.queryForObject(query, java.sql.Timestamp.class);
	}

}



