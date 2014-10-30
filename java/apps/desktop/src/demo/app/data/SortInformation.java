/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2009     *
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

package demo.app.data;

import java.io.Serializable;

public class SortInformation implements Serializable
{
	public enum SortDirection { NONE, ASC, DESC }
	
	private String m_ColumnName;
	private SortDirection m_SortDir;
	
	public SortInformation()
	{
		
	}
	
	
	public SortInformation(String columnName, SortDirection sortDirection)
	{
		m_ColumnName = columnName;
		m_SortDir = sortDirection;
	}


	public String getColumnName()
    {
    	return m_ColumnName;
    }


	public void setColumnName(String columnName)
    {
    	m_ColumnName = columnName;
    }


	public SortDirection getSortDirection()
    {
    	return m_SortDir;
    }


	public void setSortDirection(SortDirection sortDir)
    {
    	m_SortDir = sortDir;
    }
	
	
	public void setSortDirection(String sortDir)
	{
		m_SortDir = SortDirection.valueOf(sortDir);
	}
	
}
