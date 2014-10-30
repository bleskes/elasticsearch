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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.prelert.data.Evidence;


/**
 * ParameterizedRowMapper class for mapping evidence query result sets to
 * EvidenceModel objects.
 * 
 * @author Pete Harverson
 */
public class EvidenceRowMapper implements RowMapper<Evidence>
{
	public EvidenceRowMapper()
	{
		
	}
	
	
	/**
	 * Returns the EvidenceModel representation of the current row in supplied
	 * ResultSet.
	 * @param rs the ResultSet to map (pre-initialized for the current row).
	 * @param rowNum the number of the current row.
	 * @return EvidenceModel object for the current row.
	 */
    public Evidence mapRow(ResultSet rs, int rowNum) throws SQLException
    {
		Evidence evidence = new Evidence();
		
		ResultSetMetaData metaData = rs.getMetaData();

		String columnName;
		Object columnValue;
		for (int i = 1; i <= metaData.getColumnCount(); i++)
		{	
			columnName = metaData.getColumnLabel(i);
			columnValue = rs.getObject(i);	
			evidence.set(columnName, columnValue);
		}
		
		// MySQL has no boolean value, so check is:
		// value==0 - no probable cause
		// value==1 - has probable cause
		// TODO : Remove this code when MySQL is no longer supported.
		Object probCauseVal = evidence.get(Evidence.COLUMN_NAME_PROBABLE_CAUSE);
		if (probCauseVal != null)
		{
			boolean hasProbableCause = ( (probCauseVal.toString().equals("1"))
					|| (probCauseVal.toString().equals("true")) );
			evidence.set(Evidence.COLUMN_NAME_PROBABLE_CAUSE, hasProbableCause);
		}
		
        return evidence;
    }

}
