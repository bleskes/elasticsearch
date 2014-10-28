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
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;

import com.prelert.data.CausalityData;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import static com.prelert.data.PropertyNames.*;
import com.prelert.server.ServerUtil;


/**
 * Implementation of the Spring RowMapper interface for mapping causality
 * query result sets to {@link CausalityData} objects.
 * 
 * @author Pete Harverson
 */
public class CausalityDataRowMapper implements RowMapper<CausalityData>
{

	private static Logger s_Logger = Logger.getLogger(CausalityDataRowMapper.class);
	
	
	@Override
    public CausalityData mapRow(ResultSet rs, int rowNum) throws SQLException
    {
		CausalityData data = new CausalityData();
		
		String categoryStr = rs.getString(CATEGORY);
		String typeName = rs.getString(TYPE);
		
		DataSourceCategory category = null;
		try
		{	
			category = DataSourceCategory.getValue(categoryStr);
			DataSourceType dataSourceType = new DataSourceType(typeName, category);
			
			data.setDataSourceType(dataSourceType);
		}
		catch (Exception e)
		{
			s_Logger.error("Error extracting data source type " +
					"from result set for type " + typeName, e);
		}
		
		data.setDescription(rs.getString(DESCRIPTION));
		data.setSource(rs.getString(SOURCE));
		data.setStartTime(rs.getTimestamp("min_time"));
		data.setEndTime(rs.getTimestamp("max_time"));
		data.setCount(rs.getInt(COUNT));
		data.setSignificance(rs.getInt("max_significance"));
		data.setMagnitude(rs.getDouble("max_magnitude"));
		
		String attributes = rs.getString(ATTRIBUTES);
		if (attributes != null && attributes.length() > 0)
		{
			try
			{				
				data.setAttributes(ServerUtil.parseAttributes(attributes));
			}
			catch (NoSuchElementException e)
			{
				s_Logger.error("Error parsing attributes for causality data, " +
						"insufficient tokens: " + attributes);
			}
		}
		
		if (category == DataSourceCategory.TIME_SERIES_FEATURE)
		{
			data.setTimeSeriesTypeId(rs.getInt("time_series_type_id"));
			data.setTimeSeriesId(rs.getInt("time_series_id"));
			data.setScalingFactor(rs.getDouble("max_scaling_factor"));
		}

		return data;

    }

}
