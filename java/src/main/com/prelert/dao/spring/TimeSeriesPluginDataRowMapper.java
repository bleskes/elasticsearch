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

package com.prelert.dao.spring;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.prelert.data.DataSourceCategory;
import com.prelert.data.TimeSeriesInterpretation;
import com.prelert.proxy.data.ExternalTimeSeriesConfig;

public class TimeSeriesPluginDataRowMapper implements RowMapper<ExternalTimeSeriesConfig>
{
	@Override
    public ExternalTimeSeriesConfig mapRow(ResultSet rs, int rowNum) throws SQLException
    {
		ExternalTimeSeriesConfig config = new ExternalTimeSeriesConfig();
		config.setType(rs.getString("type"));
		config.setCategory(DataSourceCategory.getValue(rs.getString("category")));	
		config.setMetric(rs.getString("metric"));
		config.setUsualInterval(rs.getInt("usual_interval"));
		config.setInterpretation(
				TimeSeriesInterpretation.valueOf(
						rs.getString("interpretation").toUpperCase()));
		config.setExternalPlugin(rs.getString("external_plugin"));
		config.setExternalKey(rs.getString("external_key"));
		
		return config;
    }
}
