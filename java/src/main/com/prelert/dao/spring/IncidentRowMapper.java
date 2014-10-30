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
import com.prelert.data.Incident;
import com.prelert.server.ServerUtil;


/**
 * ParameterizedRowMapper class for mapping incident query result sets to Incident objects.
 *
 * @author Pete Harverson
 */
public class IncidentRowMapper implements RowMapper<Incident>
{
	static Logger s_Logger = Logger.getLogger(IncidentRowMapper.class);

	@Override
	public Incident mapRow(ResultSet rs, int rowNum) throws SQLException
	{
		// Fields returned are as follows:
		// 1. first_time - time of the earliest piece of evidence in the incident
		// 2. time - time of the incident
		// 3. last_time - time of the latest piece of evidence in the incident
		// 4. update_time - wall clock time when the incident was last updated
		// 5. anomaly_score - anomaly score of the incident
		// 6. description - overall description of the incident
		// 7. top_evidence_id - evidence ID for the 'headline' item of evidence
		// 8. top_type - type of the headline piece of evidence
		// 9. top_category - category of the headline piece of evidence
		// 10. shared_fields - delimited string containing alternating field names
		//		and values that are the same for every piece of evidence within the incident
		// 11. common_field_name - the field name of the field, excluding type, that
		//		exhibits most commonality among the evidence within the activity,
		//		but is not the same for every piece of evidence within the activity
		// 12. common_field_value_count - the number of distinct values for the
		//		common field returned above
		// 13. common_field_top_values - delimited string containing the three most
		//		common values for the common field returned above (or two if there
		//		are only two different values)
		// 14. notification_count - the total count of notifications in the activity
		// 15. notification_type_count - count of distinct notification data types in the activity
		// 16. time_series_count - the count of distinct time series in the activity
		//		multiple features in the same time series are only counted once)
		// 17. time_series_type_count - count of distinct time series data types in the activity
		// 18. source_count - count of distinct sources in the activity

		Incident incident = new Incident();

		incident.setFirstTime(rs.getTimestamp("first_time"));
		incident.setTime(rs.getTimestamp("time"));
		incident.setLastTime(rs.getTimestamp("last_time"));
		incident.setUpdateTime(rs.getTimestamp("update_time"));
		incident.setDescription(rs.getString("description"));
		incident.setAnomalyScore(rs.getInt("anomaly_score"));
		incident.setTopEvidenceId(rs.getInt("top_evidence_id"));

		String topTypeName = rs.getString("top_type");
		String topCategoryStr = rs.getString("top_category");
		DataSourceCategory category = null;
		try
		{
			category = DataSourceCategory.getValue(topCategoryStr);
			DataSourceType dataSourceType = new DataSourceType(topTypeName, category);

			incident.setTopDataSourceType(dataSourceType);
		}
		catch (Exception e)
		{
			s_Logger.error("Error extracting data source type " +
					"from result set for type " + topTypeName, e);
		}

		// Get the list of shared attributes.
		String sharedFields = rs.getString("shared_fields");
		if (sharedFields != null && sharedFields.length() > 0)
		{
			try
			{
				incident.setSharedAttributes(ServerUtil.parseAttributes(sharedFields));
			}
			catch (NoSuchElementException e)
			{
				s_Logger.error("Error parsing shared_fields for activity " +
						" with top_evidence_id " + incident.getTopEvidenceId() +
						", insufficient tokens: " + sharedFields);
			}
		}

		// Set fields for the common attribute.
		incident.setCommonAttributeName(rs.getString("common_field_name"));
		incident.setCommonAttributeValueCount(rs.getInt("common_field_value_count"));

		String topValues = rs.getString("common_field_top_values");
		if (topValues != null)
		{
			incident.setCommonFieldTopValues(ServerUtil.parseStrings(topValues));
		}

		incident.setNotificationCount(rs.getInt("notification_count"));
		incident.setNotificationTypeCount(rs.getInt("notification_type_count"));
		incident.setTimeSeriesCount(rs.getInt("time_series_count"));
		incident.setTimeSeriesTypeCount(rs.getInt("time_series_type_count"));
		incident.setSourceCount(rs.getInt("source_count"));

		return incident;
	}
}
