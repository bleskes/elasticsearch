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

package com.prelert.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;

import com.prelert.data.Incident;
import com.prelert.data.Severity;

/**
 * ParameterizedRowMapper class for mapping incident query result sets to Incident objects.
 * 
 * @author Pete Harverson
 */
public class IncidentRowMapper implements RowMapper<Incident>
{
	static Logger logger = Logger.getLogger(IncidentRowMapper.class);
	
	@Override
    public Incident mapRow(ResultSet rs, int rowNum) throws SQLException
    {	
		Incident incident = new Incident();
		
		incident.setTime(rs.getTimestamp("time"));
		incident.setDescription(rs.getString("description"));
		incident.setAnomalyScore(rs.getInt("anomaly_score"));
		incident.setEvidenceId(rs.getInt("evidence_id"));
		
		String severityStr = rs.getString("severity");
		try
		{	
			Severity severity = Enum.valueOf(Severity.class, severityStr.toUpperCase());
			incident.setSeverity(severity);
		}
		catch (Exception e)
		{
			logger.error("Error extracting severity " +
					"from result set for incident with value " + severityStr, e);
		}
		
        return incident;
    }
}
