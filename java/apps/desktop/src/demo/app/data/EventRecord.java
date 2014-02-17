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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
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

import com.extjs.gxt.ui.client.data.BaseModel;


/**
 * Class encapsulating a record of an event stored in the Prelert database
 * which has an associated severity value, such as a piece of evidence.
 * @author Pete Harverson
 */
public class EventRecord extends BaseModel implements Serializable
{
	
	/**
	 * Creates a new empty Event record.
	 */
	public EventRecord()
	{
		
	}
	
	
	/**
	 * Returns the value of the <code>id</code> column for this record.
	 * @return the <code>id</code> of the event record, or -1 if the event has
	 * no id value.
	 */
	public int getId()
	{
		return ((Integer)get("id")).intValue();
	}
	
	
	public void setId(int id)
	{
		set("id", new Integer(id));
	}


	public String getSource()
    {
    	return get("source");
    }


	public void setSource(String source)
    {
    	set("source", source);
    }


	public String getDescription()
    {
    	return get("description");
    }


	public void setDescription(String description)
    {
    	set("description", description);
    }


	/**
	 * Returns the value of the <code>severity</code> column for this record.
	 * @return the <code>severity</code> of the event record, such as 'minor',
	 * 'major' or 'critical', or 'none' if there is no severity set.
	 */
	public String getSeverity()
    {
    	return get("severity", "none");
    }


	public void setSeverity(String severity)
    {
    	set("severity", severity);
    }
	
	
	/**
	 * Returns the name of the data type of this evidence record, if one has been
	 * set e.g. 'p2pslog', 'mdhlog', 'apache_log'.
	 * @return the name of the data type, of <code>null</code> if no
	 * 	data type field has been set for this evidence record.
	 */
	public String getDataType()
	{
		return get("type");
	}
	
	
	/**
	 * Returns the value of the time field <b>as a String</b> for this 
	 * EventRecord in the context of the specified time frame.
	 * @param timeFrame	time frame context of record e.g. WEEK, DAY, 
	 * 			HOUR, MINUTE or SECOND.
	 * @return recorded time of event record.
	 */
	public String getTime(TimeFrame timeFrame)
	{
		String timeVal = null;

		switch (timeFrame)
		{
			case WEEK:
				timeVal = get("last_occurred");
				break;
			case DAY:
				timeVal = get("day");
				break;
			case HOUR:
				timeVal = get("hour");
				break;
			case MINUTE:
				timeVal = get("minute");
				break;
			case SECOND:
			default:
				timeVal = get("time");
				break;
		}
		
		
		return timeVal;
	}
	
	
	/**
     * Returns the name of the property in an EventRecord which holds the value
     * of its time.
     * @param timeFrame time frame context of the EventRecord e.g. WEEK, DAY, HOUR
     * MINUTE or SECOND.
     * @return the name of the time column.
     */
    public static String getTimeColumnName(TimeFrame timeFrame)
    {
    	String timeColumnName = "time";
    	switch (timeFrame)
    	{
    		case WEEK:
    			timeColumnName = "last_occurred";
    			break;
    			
			case DAY:
			case HOUR:
			case MINUTE:
				timeColumnName = timeFrame.toString().toLowerCase();
				break;
				
			case SECOND:
				timeColumnName = "time";
				break;
    	}
    	
    	return timeColumnName;
    }
	

	/**
	 * Returns a summary of this Event record.
	 * @return String representation of the Event Record, showing all fields
	 * and values.
	 */
    public String toString()
    {
		StringBuilder strRep = new StringBuilder();
		strRep.append(getProperties());
		
		return strRep.toString();
    }

}
