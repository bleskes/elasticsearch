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

import com.prelert.proxy.data.ExternalDataTypeConfig;


/**
 * Row mapper class for reading plugin descriptions from the database.
 * @author dkyle
 *
 */
public class PluginDataRowMapper implements RowMapper<ExternalDataTypeConfig>
{
	@Override
    public ExternalDataTypeConfig mapRow(ResultSet rs, int rowNum) throws SQLException
    {
		ExternalDataTypeConfig dataType = new ExternalDataTypeConfig();
		dataType.setType(rs.getString("type"));
		dataType.setCategory(rs.getString("category"));	
		
		String externalPlugin = rs.getString("external_plugin");
		boolean isNullOrEmpty = externalPlugin == null || externalPlugin.isEmpty();
		
		dataType.setIsExternal(!isNullOrEmpty);
		dataType.setExternalPlugin(externalPlugin);
		
		return dataType;
    }

}
