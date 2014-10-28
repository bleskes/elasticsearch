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
import com.prelert.data.ProbableCause;
import com.prelert.server.ServerUtil;


/**
 * Extension of {@link ProbableCauseRowMapper} for mapping result sets from queries
 * for ProbableCause data for export. It performs some customised processing for 
 * export fields such as notification and time series attributes.
 * 
 * @author Pete Harverson
 */
public class ProbableCauseExportRowMapper extends ProbableCauseRowMapper
{
	
	static Logger logger = Logger.getLogger(ProbableCauseExportRowMapper.class);


    @Override
    protected void mapNotificationFields(ResultSet rs, int rowNum,
            ProbableCause probCause) throws SQLException
    {
    	// Read in the severity.
    	super.mapNotificationFields(rs, rowNum, probCause);
    	
    	// Read in attributes.
    	String attributes = rs.getString("attributes");
		if (attributes != null)
		{
			try
			{
				probCause.setAttributes(ServerUtil.parseAttributes(attributes));
			}
			catch (NoSuchElementException e)
			{
				logger.error("Error parsing attributes for probable cause with evidence_id " +
						probCause.getEvidenceId() + ", insufficient tokens:" + attributes);
			}
		}
    }


    @Override
    protected void mapTimeSeriesFields(ResultSet rs, int rowNum,
            ProbableCause probCause) throws SQLException
    {
    	// Just read time series id, metric and attributes 
    	// 	- no scaling factor or peak value.
    	int timeSeriesId = rs.getInt("time_series_id");
		String metric = rs.getString("metric");
		probCause.setTimeSeriesId(timeSeriesId);
		probCause.setMetric(metric);
		
		String attributes = rs.getString("attributes");
		if (attributes != null)
		{
			try
			{
				probCause.setAttributes(ServerUtil.parseAttributes(attributes));
			}
			catch (NoSuchElementException e)
			{
				logger.error("Error parsing attributes for probable cause with evidence_id " +
						probCause.getEvidenceId() + ", insufficient tokens:" + attributes);
			}
		}
    }
	
}
