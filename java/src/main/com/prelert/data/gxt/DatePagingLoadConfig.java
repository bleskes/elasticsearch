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

package com.prelert.data.gxt;

import java.io.Serializable;
import java.util.Date;

import com.extjs.gxt.ui.client.data.BaseListLoadConfig;
import com.prelert.data.TimeFrame;


/**
 * Implementation of ListLoadConfig which adds support for a data type, 
 * time frame and a date for the page.
 * @author Pete Harverson
 */

@SuppressWarnings("serial")
public class DatePagingLoadConfig extends BaseListLoadConfig implements Serializable
{
	/**
	 * Creates a new Date paging load config.
	 */
	public DatePagingLoadConfig()
	{

	}
	
	
	/**
	 * Returns the data type, such as 'p2psmon_users' or 'system_udp', which is used
	 * to identify the particular type of data being displayed.
	 * @return the data type.
	 */
	public String getDataType()
    {
    	return get("dataType");
    }


	/**
	 * Sets the data type, such as 'p2psmon_users' or 'system_udp', which is used
	 * to identify the particular type of data being displayed.
	 * @param dataType the data type.
	 */
	public void setDataType(String dataType)
    {
    	set("dataType", dataType);
    }


	/**
	 * Returns the time frame for this load config.
	 * @return time frame of this load config e.g. week, day or hour.
	 */
	public TimeFrame getTimeFrame()
    {
		return get("timeFrame");
    }


	/**
	 * Sets the time frame for this load config.
	 * @param timeFrame time frame of this load config e.g. week, day or hour.
	 */
	public void setTimeFrame(TimeFrame timeFrame)
    {
		set("timeFrame", timeFrame);
    }


	/**
	 * Returns the date for this load config.
	 * @return the date for the results to be displayed in the page.
	 * 		For a WEEK time frame this should correspond to the first day
	 * 		of the week that will be displayed.
	 */
	public Date getDate()
    {
		return get("date");
    }

	
	/**
	 * Sets the date for this load config.
	 * @param date the date for the results to be displayed in the page.
	 * 		For a WEEK time frame this should correspond to the first day
	 * 		of the week that will be displayed.
	 */
	public void setDate(Date date)
    {
		set("date", date);
    }

	
	/**
	 * Returns a summary of this paging load config.
	 * @return String representation of the load config, showing all fields
	 * and values.
	 */
	public String toString()
	{
		StringBuilder strRep = new StringBuilder();
		strRep.append(getProperties());
		
		return strRep.toString();
	}

}
