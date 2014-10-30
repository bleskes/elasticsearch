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

package demo.app.data.gxt;

import java.io.Serializable;

import com.extjs.gxt.ui.client.data.BaseModelData;

public class GridRowInfo extends BaseModelData implements Serializable
{
	public GridRowInfo()
	{
		
	}
	
	
	public GridRowInfo(String columnName, String columnValue)
	{
		setColumnName(columnName);
		setColumnValue(columnValue);
	}
	
	
	public String getColumnName()
    {
    	return get("columnName");
    }


	public void setColumnName(String columnName)
    {
    	set("columnName", columnName);
    }


	public String getColumnValue()
    {
    	return get("columnValue");
    }


	public void setColumnValue(String columnValue)
    {
    	set("columnValue", columnValue);
    }
	
	
	@Override
    public String toString()
    {
		StringBuilder strRep = new StringBuilder();
		strRep.append('{');
		strRep.append(getColumnName());
		strRep.append('=');
		strRep.append(getColumnValue());
		strRep.append('}');
		
		return strRep.toString();
    }
}
