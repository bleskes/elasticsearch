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

import com.prelert.data.MetricTreeNode;
import com.prelert.server.ServerUtil;


/**
 * Implementation of the Spring RowMapper interface for mapping metric
 * path tree query result sets to {@link MetricTreeNode} objects.
 */
public class MetricTreeNodeRowMapper implements RowMapper<MetricTreeNode>
{
	static Logger s_Logger = Logger.getLogger(MetricTreeNodeRowMapper.class);


	@Override
	public MetricTreeNode mapRow(ResultSet rs, int rowNum) throws SQLException
	{
		MetricTreeNode treeNode = new MetricTreeNode();

		treeNode.setName(rs.getString("next_level_name"));
		treeNode.setValue(rs.getString("next_level_value"));	
		treeNode.setPrefix(rs.getString("next_level_prefix"));
		treeNode.setPartialPath(rs.getString("partial_metric_path"));
		treeNode.setOpaqueNum(rs.getInt("opaque_num"));
		treeNode.setOpaqueStr(rs.getString("opaque_str"));
		treeNode.setType(rs.getString("type"));
		treeNode.setSource(rs.getString("source"));
		treeNode.setCategory(rs.getString("category"));
		treeNode.setMetric(rs.getString("metric"));
		if (rs.getObject("time_series_id") != null)
		{
			treeNode.setTimeSeriesId(rs.getInt("time_series_id"));
		}
		treeNode.setIsLeaf(rs.getBoolean("is_leaf"));
		treeNode.setIsWildcard(rs.getBoolean("is_wildcard"));
		treeNode.setHasAnyWildcard(rs.getBoolean("has_any_wildcard"));

		// process attributes
		String attributeStr = rs.getString("attributes");
		if (attributeStr != null && attributeStr.length() > 0)
		{
			try
			{				
				treeNode.setAttributes(ServerUtil.parseAttributes(attributeStr));
			}
			catch (NoSuchElementException e)
			{
				s_Logger.error("Error parsing attributes for metric path tree node, " +
						"insufficient tokens: " + attributeStr);
			}
		}

		return treeNode;
	}
}
