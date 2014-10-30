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

package com.prelert.dao.spring;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.RowMapper;

import com.prelert.data.Evidence;


/**
 * ParameterizedRowMapper class for mapping an evidence query result set to
 * a list of EvidenceModel objects. The RowMapper expects the evidence query
 * to return a <i>refcursor</i>, so that results are processed by first casting
 * the return type of getObject() to a ResultSet i.e.
 * <pre>
 * 	ResultSet refCursor = (ResultSet) rs.getObject(1);	
 * </pre> 
 * 
 * @author Pete Harverson
 */
public class EvidenceListRowMapper implements RowMapper<List<Evidence>>
{

	@Override
    public List<Evidence> mapRow(ResultSet rs, int rowNum) throws SQLException
    {
		List<Evidence> evidenceList = new ArrayList<Evidence>();
		

		ResultSet refCursor = (ResultSet) rs.getObject(1);
		ResultSetMetaData metaData = refCursor.getMetaData();

		Evidence evidence;
		String columnName;
		Object columnValue;
		
		while (refCursor.next())
		{
			evidence = new Evidence();
			for (int i = 1; i <= metaData.getColumnCount(); i++)
			{
				columnName = metaData.getColumnLabel(i);
				columnValue = refCursor.getObject(i);
				if (columnValue != null)
				{
					// Only add it in if the value is non-null.
					evidence.set(columnName, columnValue);
				}
			}
			
			evidenceList.add(evidence);
		}
		
        return evidenceList;
    }

}
