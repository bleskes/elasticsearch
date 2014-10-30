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
 ************************************************************/

package com.prelert.dao.spring;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.prelert.data.ExternalTimeSeriesDetails;


/**
 * Row mapper for the {@link ExternalTimeSeriesDetails} class.
 */
public class ExternalTimeSeriesDetailsRowMapper implements RowMapper<ExternalTimeSeriesDetails>
{
	@Override
    public ExternalTimeSeriesDetails mapRow(ResultSet rs, int rowNum) throws SQLException
    {
		ExternalTimeSeriesDetails dets = new ExternalTimeSeriesDetails();
		dets.setTimeSeriesId(rs.getInt("time_series_id"));	
		dets.setTimeSeriesTypeId(rs.getInt("time_series_type_id"));	
		dets.setType(rs.getString("type"));
		dets.setMetric(rs.getString("metric"));
		dets.setExternalKey(rs.getString("external_key"));
		dets.setActive(rs.getBoolean("active"));
		
		return dets;
    }
}
