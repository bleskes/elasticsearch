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
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;

import com.prelert.data.Attribute;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.ProbableCause;


/**
 * ParameterizedRowMapper class for mapping probable cause query result 
 * sets to ProbableCause objects.
 * 
 * @author Pete Harverson
 */
public class ProbableCauseRowMapper implements RowMapper<ProbableCause>
{
	
	static Logger logger = Logger.getLogger(ProbableCauseRowMapper.class);
	

	@Override
    public ProbableCause mapRow(ResultSet rs, int rowNum) throws SQLException
    {	
		ProbableCause cause = new ProbableCause();
		
		String categoryStr = rs.getString("category");
		String typeName = rs.getString("type");
		
		DataSourceCategory category = null;
		try
		{	
			category = DataSourceCategory.getValue(categoryStr);
			DataSourceType dataSourceType = new DataSourceType(typeName, category);
			
			cause.setDataSourceType(dataSourceType);
		}
		catch (Exception e)
		{
			logger.error("Error extracting data source type " +
					"from result set for type " + typeName, e);
		}
		
		int evidenceId = rs.getInt("evidence_id");
		java.sql.Timestamp time = rs.getTimestamp("time");
		String description = rs.getString("description");
		String source = rs.getString("source");
		int count = rs.getInt("count");
		int significance = rs.getInt("significance");
		double magnitude = rs.getDouble("magnitude");
		
		cause.setEvidenceId(evidenceId);
		cause.setTime(time);
		cause.setDescription(description);	
		cause.setSource(source);
		cause.setCount(count);
		cause.setSignificance(significance);
		cause.setMagnitude(magnitude);
		
		if (category == DataSourceCategory.TIME_SERIES)
		{
			// Set metric and stats for time series probable causes.
			int typeId = rs.getInt("time_series_type_id");
			double scalingFactor = rs.getDouble("scaling_factor");
			double peakValue = rs.getDouble("peak_value");
			String metric = rs.getString("metric");
			
			cause.setTimeSeriesTypeId(typeId);
			cause.setMetric(metric);
			cause.setScalingFactor(scalingFactor);
			cause.setPeakValue(peakValue);
			
			// Set a generic 'Features in xxx metric' description for aggregation.
			// TODO - can this be obtained via the procs?
			cause.setDescription("Features in " + metric + " metric");
		
			String attributes = rs.getString("time_series_attributes");
			if (attributes != null)
			{
				try
				{
					cause.setAttributes(parseAttributes(attributes));	
				}
				catch (NoSuchElementException e)
				{
					logger.error("Error parsing time_series_attributes for probable cause " +
							" with evidence_id " + evidenceId + ", insufficient tokens:" + attributes);
				}
			}
		}
		
        return cause;
    }
	
	
	/**
	 * Parses the time series attributes in the specified semi-colon delimited String
	 * in the format 'name1;value1;name2;value2;...'
	 * to a list of Attribute objects.
	 * @param attributes semi-colon separated list e.g. appId;PRISM_Sink_Dist_App;username;257_PRISM_VOBR_0
	 * @return the list of Attributes.
	 * @throws NoSuchElementException if the String does not contain an even number of tokens.
	 */
	protected List<Attribute> parseAttributes(String attributes) throws NoSuchElementException
	{
		StringTokenizer tokenizer = new StringTokenizer(attributes, ";");
		String attributeName;
		String attributeValue;
		
		ArrayList<Attribute> attributeList = new ArrayList<Attribute>();
		while (tokenizer.hasMoreTokens())
		{
			attributeName = tokenizer.nextToken();
			attributeValue = tokenizer.nextToken();
			
			attributeList.add(new Attribute(attributeName, attributeValue));
		}
		
		return attributeList;
	}

}
