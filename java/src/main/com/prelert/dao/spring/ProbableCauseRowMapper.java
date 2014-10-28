/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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

import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.ProbableCause;
import com.prelert.data.Severity;
import com.prelert.server.ServerUtil;


/**
 * ParameterizedRowMapper class for mapping probable cause query result
 * sets to ProbableCause objects.
 *
 * @author Pete Harverson
 */
public class ProbableCauseRowMapper implements RowMapper<ProbableCause>
{

	private static Logger s_Logger = Logger.getLogger(ProbableCauseRowMapper.class);


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
			s_Logger.error("Error extracting data source type " +
					"from result set for type " + typeName, e);
		}
	
		int evidenceId = rs.getInt("evidence_id");
		java.sql.Timestamp time = rs.getTimestamp("time");
		String description = rs.getString("description");
		String source = rs.getString("source");
		int count = rs.getInt("count");
		int significance = rs.getInt("significance");
		double magnitude = rs.getDouble("magnitude");
		String externalKey = rs.getString("external_key");

		cause.setEvidenceId(evidenceId);
		cause.setTime(time);
		cause.setDescription(description);
		cause.setSource(source);
		cause.setCount(count);
		cause.setSignificance(significance);
		cause.setMagnitude(magnitude);
		cause.setExternalKey(externalKey);

		if (category == DataSourceCategory.NOTIFICATION)
		{
			mapNotificationFields(rs, rowNum, cause);
		}
		else
		{
			mapTimeSeriesFields(rs, rowNum, cause);
		}

		try
		{
			int topEvidenceId = rs.getInt("top_evidence_id");
			cause.setTopEvidenceId(topEvidenceId);
		}
		catch (SQLException e)
		{
			// Only the probable_cause_list_api procedure returns a
			// top_evidence_id field, so this is OK
		}

		return cause;
	}


	/**
	 * Performs the notification-specific field mapping of the ResultSet to a
	 * ProbableCause object.
	 * @param rs the ResultSet to map.
	 * @param rowNum the number of the current row.
	 * @param probCause ProbableCause object whose fields are to be populated.
	 * @throws SQLException if an SQLException is encountered getting column values.
	 */
	protected void mapNotificationFields(ResultSet rs, int rowNum,
			ProbableCause probCause) throws SQLException
	{
		String severity = rs.getString("severity");
		if (severity != null)
		{
			probCause.setSeverity(Enum.valueOf(Severity.class, severity.toUpperCase()));
		}
		else
		{
			probCause.setSeverity(Severity.NONE);
		}
	}


	/**
	 * Performs the time series-specific field mapping of the ResultSet to a
	 * ProbableCause object.
	 * @param rs the ResultSet to map.
	 * @param rowNum the number of the current row.
	 * @param probCause ProbableCause object whose fields are to be populated.
	 * @throws SQLException if an SQLException is encountered getting column values.
	 */
	protected void mapTimeSeriesFields(ResultSet rs, int rowNum,
			ProbableCause probCause) throws SQLException
	{
		// Set metric and stats for time series probable causes.
		int typeId = rs.getInt("time_series_type_id");
		int timeSeriesId = rs.getInt("time_series_id");
		double scalingFactor = rs.getDouble("scaling_factor");
		String metric = rs.getString("metric");

		probCause.setTimeSeriesTypeId(typeId);
		probCause.setTimeSeriesId(timeSeriesId);
		probCause.setMetric(metric);
		probCause.setScalingFactor(scalingFactor);

		try
		{
			double peakValue = rs.getDouble("peak_value");
			probCause.setPeakValue(peakValue);

			String attributes = rs.getString("time_series_attributes");
			if (attributes != null)
			{
				try
				{
					probCause.setAttributes(ServerUtil.parseAttributes(attributes));
				}
				catch (NoSuchElementException e)
				{
					s_Logger.error("Error parsing time_series_attributes for probable cause " +
							" with evidence_id " + probCause.getEvidenceId() +
							", insufficient tokens: " + attributes);
				}
			}
		}
		catch (SQLException e)
		{
			// The probable_cause_list_api procedure does not return peak_value
			// or time_series_attributes fields, so this is OK
		}
	}

}

