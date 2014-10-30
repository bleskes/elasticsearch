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
import java.util.Map;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.data.RpcMap;

import com.prelert.data.Evidence;
import com.prelert.data.Severity;
import com.prelert.data.TimeFrame;


/**
 * Extension of the GXT BaseModelData class for evidence data.
 * @author Pete Harverson
 */

@SuppressWarnings("serial")
public class EvidenceModel extends BaseModelData 
{
	public EvidenceModel()
	{
		
	}
	
	/**
	 * Initialises a new EvidenceModel with the property set.
	 * @param properties to set.
	 */
	public EvidenceModel(Map<String, Object> properties)
	{
		this();
		setProperties(properties);
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
	 * Returns the data type of this item of evidence, if one has been
	 * set e.g. 'p2pslog', 'mdhlog', 'apache_log'.
	 * @return the name of the data type, of <code>null</code> if no
	 * 	data type field has been set for this evidence record.
	 */
	public String getDataType()
	{
		return get("type");
	}
	
	
	/**
	 * Sets the data type of this item of evidence e.g. 'p2pslog', 'mdhlog', 
	 * or 'apache_log'.
	 * @param dataType the name of the data type
	 */
	public void setDataType(String dataType)
	{
		set("type", dataType);
	}
	
	
	/**
	 * Returns the value of the time field for this item of evidence in the 
	 * context of the specified time frame.
	 * @param timeFrame	time frame context of record e.g. WEEK, DAY, 
	 * 			HOUR, MINUTE or SECOND.
	 * @return recorded time of event record.
	 */
	public Date getTime(TimeFrame timeFrame)
	{
		Date time = get(Evidence.getTimeColumnName(timeFrame));
		return time;
	}
	
	
	/**
	 * Sets the value of the time field for this item of evidence in the context 
	 * of the specified time frame.
	 * @param timeFrame	time frame context of record e.g. WEEK, DAY, 
	 * 			HOUR, MINUTE or SECOND.
	 * @param time 	recorded time of event.
	 */
	public void setTime(TimeFrame timeFrame, Date time)
	{
		set(Evidence.getTimeColumnName(timeFrame), time);
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


	/**
	 * Overridden method for getting a property that doesn't do anything special
	 * with square brackets.
	 * TODO - revisit if we ever need to make use of nested properties.
	 * @param property The property name.
	 * @return The current value associated with the property (or null if there
	 *         isn't one).
	 */
	@Override
	@SuppressWarnings({"unchecked"})
	public <X> X get(String property)
	{
		// map is a protected member variable from the base class
		if (map == null)
		{
			return null;
		}

		return (X)map.get(property);
	}


	/**
	 * Overridden method for setting a property that doesn't do anything special
	 * with square brackets.
	 * TODO - revisit if we ever need to make use of nested properties.
	 * @param property The property name.
	 * @param value The property value.
	 * @return The previous value associated with the property (or null if there
	 *         wasn't one).
	 */
	@Override
	@SuppressWarnings({"unchecked"})
	public <X> X set(String property, X value)
	{
		// map is a protected member variable from the base class
		if (map == null)
		{
			map = new RpcMap();
		}

		return (X)map.put(property, value);
	}

}
