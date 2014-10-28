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

import com.prelert.data.MetricPath;

/**
 * Metric Path spring row mapper class. 
 */
public class MetricPathRowMapper implements RowMapper<MetricPath>
{
	@Override
	public MetricPath mapRow(ResultSet rs, int rowNum) throws SQLException
	{
		MetricPath path = new MetricPath();
		
		path.setDatatype(rs.getString("type"));
		path.setLastLevelName(rs.getString("last_level_name"));
		path.setLastLevelValue(rs.getString("last_level_value"));
		path.setLastLevelPrefix(rs.getString("last_level_prefix"));
		path.setOpaqueNum(rs.getInt("opaque_num"));
		path.setOpaqueStr(rs.getString("opaque_str"));
		path.setPartialPath(rs.getString("partial_metric_path"));
		path.setExternalKey(rs.getString("external_key"));

		return path;
	}
}

