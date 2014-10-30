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
import java.util.Date;

import com.extjs.gxt.ui.client.data.BaseModelData;

import demo.app.client.ClientUtil;
import demo.app.data.Severity;
import demo.app.data.TimeFrame;


/**
 * Extension of the GXT BaseModelData class for evidence data.
 * @author Pete Harverson
 */
public class EvidenceModel extends BaseModelData implements Serializable
{
	
	/**
	 * Creates a new empty Event record.
	 */
	public EvidenceModel()
	{
		
	}
	
	
	/**
	 * Returns the value of the <code>id</code> column for this item of evidence.
	 * @return the <code>id</code> of the evidence.
	 */
	public int getId()
	{
		Integer idInt = get("id", new Integer(-1));
    	return idInt.intValue();
	}
	
	
	/**
	 * Sets the id for this item of evidence data.
	 * @param id the <code>id</code> of the evidence.
	 */
	public void setId(int id)
	{
		set("id", new Integer(id));
	}


	/**
	 * Returns the name of the source (server) of this item of evidence.
	 * @return the source (server) name.
	 */
	public String getSource()
    {
    	return get("source");
    }


	/**
	 * Sets the name of the source (server) of this item of evidence.
	 * @param source the source (server) name.
	 */
	public void setSource(String source)
    {
    	set("source", source);
    }


	/**
	 * Returns the description of this item of evidence data.
	 * @return the description.
	 */
	public String getDescription()
    {
    	return get("description");
    }

	
	/**
	 * Sets the description of this item of evidence data.
	 * @param description the evidence description.
	 */
	public void setDescription(String description)
    {
    	set("description", description);
    }


	/**
	 * Returns the value of the <code>severity</code> column for this record.
	 * @return the <code>severity</code> of the event record, such as 'minor',
	 * 'major' or 'critical', or 'none' if there is no severity set.
	 */
	public Severity getSeverity()
    {
		String severityStr = get("severity", Severity.NONE.toString());
		return Enum.valueOf(Severity.class, severityStr.toUpperCase());
    }


	/**
	 * Sets the severity of this item of evidence.
	 * @param severity the <code>severity</code> of the event record, such as 'minor',
	 * 'major' or 'critical', or 'none' if there is no severity set.
	 */
	public void setSeverity(Severity severity)
    {
		// Mar 2010: Store the Severity as a String as otherwise errors may occur
		// when this object is transported via GWT RPC.
    	set("severity", severity.toString());
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
		return get(getTimeColumnName(timeFrame));
	}
	
	
	/**
	 * Sets the value of the time field for this item of evidence in the context 
	 * of the specified time frame.
	 * @param timeFrame	time frame context of record e.g. WEEK, DAY, 
	 * 			HOUR, MINUTE or SECOND.
	 * @param time recorded time of event.
	 */
	public void setTime(TimeFrame timeFrame, Date time)
	{
		String timeStr = ClientUtil.formatTimeField(time, timeFrame);
		set(getTimeColumnName(timeFrame), timeStr);
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
