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

package com.prelert.data.gxt;

import java.util.Date;

import com.extjs.gxt.ui.client.data.BaseListLoadConfig;


/**
 * Extension of the default GXT list load configuration for paging through model
 * data ordered by time.
 * @author Pete Harverson
 */
public class ModelDatePagingLoadConfig extends BaseListLoadConfig
{
    private static final long serialVersionUID = -5197034806430443310L;
    
    
    /**
     * Sets the page size - the maximum number of data items displayed in a page.
     * @param pageSize the page size.
     */
    public void setPageSize(int pageSize)
    {
    	set("pageSize", pageSize);
    }
    
    
    /**
     * Returns the page size - the maximum number of data items displayed in a page.
     * @return the page size, which has a default of 20.
     */
    public int getPageSize()
    {
    	int pageSize = get("pageSize", new Integer(20));
    	return pageSize;
    }
    
    
    /**
     * Sets the 'time' property, used by certain paging operations (e.g. next 
     * or previous) to determine the start or end point of data to return.
     * @param time the date / time.
     */
    public void setTime(Date time)
    {
    	set("time", time);
    }
    
    
    /**
     * Returns the value of the 'time' property, used by certain paging operations 
     * (e.g. next or previous) to determine the start or end point of data to return.
     * @return the date / time.
     */
    public Date getTime()
    {
    	return get("time");
    }
    
    
    /**
     * Sets the 'rowId' property, used by certain paging operations (e.g. next 
     * or previous) to uniquely identify the start or end point of data to return
     * for cases where multiple data items may occur at the same time.
     * @param rowId unique row identifier.
     */
    public void setRowId(int rowId)
    {
    	set("rowId", rowId);
    }
    
    
    /**
     * Returns the 'rowId' property, used by certain paging operations (e.g. next 
     * or previous) to uniquely identify the start or end point of data to return
     * for cases where multiple data items may occur at the same time.
     * @return unique row identifier.
     */
    public int getRowId()
    {
    	int rowId = get("rowId", new Integer(0));
    	return rowId;
    }
    
    
    /**
	 * Returns a summary of this paging load config.
	 * @return String representation of the load config, showing all fields
	 * 	and values.
	 */
    @Override
	public String toString()
	{
		StringBuilder strRep = new StringBuilder();
		strRep.append(getProperties());
		
		return strRep.toString();
	}

}
