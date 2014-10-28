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

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;

import com.prelert.data.CausalityAggregate;
import com.prelert.data.CausalityData;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.server.ServerUtil;


/**
 * Implementation of the Spring RowMapper interface for mapping incident summary
 * query result sets to aggregated causality data.
 * 
 * @author Pete Harverson
 */
public class CausalityAggregateRowMapper implements RowMapper<CausalityAggregate>
{	
	private static Logger s_Logger = Logger.getLogger(CausalityAggregateRowMapper.class);
	

	/**
	 * Maps the row to a CausalityAggregate object. Note that the only fields
	 * populated in the top item of CausalityData are:
	 * <ul>
	 * <li>startTime</li>
	 * <li>endTime</li>
	 * <li>count</li>
	 * <li>category part of dataSourceType</li>
	 * </ul>
	 */
	@Override
    public CausalityAggregate mapRow(ResultSet rs, int rowNum) throws SQLException
    {
		CausalityAggregate aggregate = new CausalityAggregate();
		
		// Map the fields in the row to the aggregated causality object properties.

		// Fields that refer to the overall aggregated row.
		aggregate.setAggregateValue(rs.getString("aggregate_value"));
		aggregate.setStartTime(rs.getTimestamp("min_time"));
		aggregate.setEndTime(rs.getTimestamp("max_time"));
		aggregate.setNotificationCount(rs.getInt("notification_count"));
		aggregate.setFeatureCount(rs.getInt("time_series_count"));
		aggregate.setSourceCount(rs.getInt("source_count"));
		String sourcesList = rs.getString("sources");
		if (sourcesList != null)
		{
			aggregate.setSourceNames(ServerUtil.parseStrings(sourcesList));
		}

		// Fields that refer to the 'top' item of causality data.
		aggregate.setTopEvidenceId(rs.getInt("top_evidence_id"));

		CausalityData topData = new CausalityData();
		topData.setStartTime(rs.getTimestamp("top_min_time"));
		topData.setEndTime(rs.getTimestamp("top_max_time"));
		topData.setCount(rs.getInt("top_count"));
		String topCategoryStr = rs.getString("top_category");
		DataSourceCategory topCategory = null;
		try
		{	
			topCategory = DataSourceCategory.getValue(topCategoryStr);
			DataSourceType topDataSourceType = new DataSourceType(null, topCategory);
			topData.setDataSourceType(topDataSourceType);
		}
		catch (Exception e)
		{
			s_Logger.error("Error extracting top data source category " +
					"from result set for aggregate " + aggregate, e);
		}

		if (topCategory == DataSourceCategory.TIME_SERIES_FEATURE)
		{
			topData.setTimeSeriesTypeId(rs.getInt("top_time_series_type_id"));
			topData.setTimeSeriesId(rs.getInt("top_time_series_id"));
			topData.setScalingFactor(rs.getDouble("top_max_scaling_factor"));
		}
		
		aggregate.setTopCausalityData(topData);

		return aggregate;
    }

}

