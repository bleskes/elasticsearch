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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import org.springframework.jdbc.core.RowMapper;

import com.prelert.data.TimeSeriesConfig;
import com.prelert.server.ServerUtil;


/**
 * RowMapper class for mapping time series config result sets.
 */
public class TimeSeriesConfigRowMapper implements RowMapper<TimeSeriesConfig>
{

	private static Logger s_Logger = Logger.getLogger(TimeSeriesConfigRowMapper.class);


	/**
	 * Map a result row containing (at minimum) the following fields:
	 * - type
	 * - metric
	 * - source
	 * - attributes
	 * - external_key
	 * @return A <code>TimeSeriesConfig</code> object
	 */
	@Override
	public TimeSeriesConfig mapRow(ResultSet rs, int rowNum) throws SQLException
	{
		String dataType = rs.getString("type");
		String metric = rs.getString("metric");
		String source = rs.getString("source");
		String attributes = rs.getString("attributes");
		String externalKey = rs.getString("external_key");
		int timeSeriesId = rs.getInt("time_series_id");

		TimeSeriesConfig config =
			new TimeSeriesConfig(dataType, metric, source);

		if (attributes != null)
		{
			try
			{
				config.setAttributes(ServerUtil.parseAttributes(attributes));
			}
			catch (NoSuchElementException e)
			{
				s_Logger.error("Error parsing time_series_from_feature attributes '" +
								attributes + "': insufficient tokens");
			}
		}

		config.setTimeSeriesId(timeSeriesId);
		config.setExternalKey(externalKey);

		return config;
	}

}

