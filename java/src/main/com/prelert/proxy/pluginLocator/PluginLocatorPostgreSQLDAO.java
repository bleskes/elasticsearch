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

package com.prelert.proxy.pluginLocator;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.SimpleJdbcDaoSupport;

import com.prelert.dao.spring.PluginDataRowMapper;
import com.prelert.dao.spring.TimeSeriesPluginDataRowMapper;
import com.prelert.data.DataSourceCategory;
import com.prelert.proxy.data.ExternalDataTypeConfig;
import com.prelert.proxy.data.ExternalTimeSeriesConfig;

/**
 * This class is the Postgres implementation of <code>PluginLocatorDAO</code>.
 *
 * @author dkyle
 */
public class PluginLocatorPostgreSQLDAO extends SimpleJdbcDaoSupport implements PluginLocatorDAO
{
	static private Logger s_Logger = Logger.getLogger(PluginLocatorPostgreSQLDAO.class);

	static private PluginDataRowMapper s_RowMapper = new PluginDataRowMapper();
	static private TimeSeriesPluginDataRowMapper s_TimeSeriesPluginDataRowMapper = new TimeSeriesPluginDataRowMapper();

	public PluginLocatorPostgreSQLDAO()
	{
	}


	/**
	 * Determines whether the data for <code>dataType</code> is stored in the
	 * the internal Prelert database or externally and accessed through a
	 * plugin.  Searches Time Series and Evidence types for
	 * <code>dataType</code>.
	 * @param dataType can be either a Time Series or Evidence type.
	 * @return True if dataType is served by an external plugin.
	 */
	@Override
	public boolean isExternal(String dataType)
	{
		return (getPluginName(dataType) != null);
	}


	/**
	 * Returns details of each external datatype and its <code>Plugin</code>
	 * registered with the Prelert database. 
	 * @return List of configurations for each external data type.
	 */
	@Override
	public List<ExternalDataTypeConfig> getExternalPluginsDescriptions()
	{
		String query = "select * from data_types_with_external_plugins()";

		s_Logger.trace("getExternalDataTypes() : " + query);

		return getSimpleJdbcTemplate().query(query, s_RowMapper);
	}


	/**
	 * Queries the database to determine the type of external plugin that needs
	 * to be used to access data externally.  If the data type is internal (or
	 * unknown) then null is returned.
	 * @param dataType Can be either a Time Series or Evidence type.
	 * @return The name of the plugin for external types; null otherwise.
	 */
	@Override
	public String getPluginName(String dataType)
	{
		String query = "select * from plugin_for_data_type(?)";
		s_Logger.trace("getPluginName(" + dataType + ") : " + query);

		return (String)getSimpleJdbcTemplate().queryForObject(query, String.class, dataType);
	}


	/**
	 * For the given Time Series Id which is an external Time Series return
	 * details of the Plugin used to access that Time Series's data.
	 * <code>timeSeriesId</code> must be < 0.
	 * @param timeSeriesId must be < 0.
	 * @return null if no plugins were found else the description of how to
	 *         access the external time series, i.e. plugin and key.
	 */
	@Override
	public ExternalTimeSeriesConfig getPluginDescriptionForTimeSeriesId(
													int timeSeriesId)
	{
		String query = "select * from plugin_for_time_series_id(?)";

		s_Logger.trace("getPluginDescriptionForTimeSeriesId(" + timeSeriesId +
						") : " + query);

		// We expect the list to have 0 or 1 elements
		List<ExternalTimeSeriesConfig> list = getSimpleJdbcTemplate().query(query,
												s_TimeSeriesPluginDataRowMapper,
												timeSeriesId);
		if (list.isEmpty())
		{
			return null;
		}

		if (list.size() > 1)
		{
			s_Logger.warn(query + " with argument " + timeSeriesId +
							" returned " + list.size() + " rows - expected 1");
		}

		return list.get(0);
	}
	
	
	/**
	 * Adds the evidence type to the database.
	 */
	@Override
	public int addEvidenceType(String type, DataSourceCategory category) 
	{
		String query = "select * from add_evidence_type(?, ?)";

		s_Logger.trace("addEvidenceType(" + type + ", " + category +	")");

		return getSimpleJdbcTemplate().queryForInt(query, type, category.toString());
	}

}
