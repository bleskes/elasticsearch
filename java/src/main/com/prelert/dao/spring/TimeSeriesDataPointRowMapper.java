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

package com.prelert.dao.spring;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.prelert.data.Evidence;
import com.prelert.data.TimeSeriesDataPoint;


/**
 * RowMapper class for mapping time series query result sets to data points.
 */
public class TimeSeriesDataPointRowMapper implements RowMapper<TimeSeriesDataPoint>
{
	private String 	m_Metric;
	private boolean	m_CheckForFeatures;
	
	
	/**
	 * Creates a new RowMapper for creating TimeSeriesDataPoint objects.
	 * @param metric the name of the column that holds the value in the result set.
	 * @param checkForFeatures <code>true</code> if the RowMapper should check for
	 * 		and create features in the data points. If a feature is found, only the
	 * 		id field is set in the created feature Evidence object.
	 */
	public TimeSeriesDataPointRowMapper(String metric, boolean checkForFeatures)
	{
		m_Metric = metric;
		m_CheckForFeatures = checkForFeatures;
	}

	
	@Override
    public TimeSeriesDataPoint mapRow(ResultSet rs, int rowNum) throws SQLException
    {
		java.sql.Timestamp time = rs.getTimestamp("time");
		BigDecimal value = rs.getBigDecimal(m_Metric);	
		
		TimeSeriesDataPoint dataPoint = 
			new TimeSeriesDataPoint(time.getTime(), value.doubleValue());
		
		if (m_CheckForFeatures == true)
		{
			// Just set the id initially.
			// This needs to be populated via a call to getFeature(id).
			int evidenceId = rs.getInt("evidence_id");
			if (evidenceId > 0)
			{
				dataPoint.setFeature(new Evidence(evidenceId));
			}
		}

        return dataPoint;
    }
}
