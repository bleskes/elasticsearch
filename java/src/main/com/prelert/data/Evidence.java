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

package com.prelert.data;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;



/**
 * Class encapsulating the properties of an item of evidence.
 * @author Pete Harverson
 */

public class Evidence implements Serializable
{
	private static final long serialVersionUID = -6922923513284606384L;
	
	static final public int INVALID_EVIDENCE_ID = -1;
	public static final String COLUMN_NAME_TIME = "time";
	public static final String COLUMN_NAME_PROBABLE_CAUSE = "probable_cause";
	
	private HashMap<String, Object> m_HashMap;	
	
	@SuppressWarnings("unused")
	private Integer m_Integer; 	// DO NOT DELETE - custom GWT RPC field serializer.
	
	
	public Evidence()
	{
		m_HashMap = new HashMap<String, Object>();
		
		setSeverity(Severity.NONE);
	}
	
	
	/**
	 * Constructs an item of evidence with the specified id.
	 * @param id identifier for the evidence.
	 */
	public Evidence(int id)
	{
		this();
		setId(id);
	}
	
	public Evidence(Map<String, Object> properties)
	{
		this();
		m_HashMap.putAll(properties);
	}
	
	
	/**
	 * Returns the unique identifier for this item of evidence.
	 * @return the <code>id</code> of the item of evidence.
	 */
	public int getId()
	{
		Integer idInt = (Integer) m_HashMap.get("id");
		if (idInt == null)
		{
			idInt = new Integer(-1);
		}
		return idInt.intValue();
	}
	
	
	/**
	 * Sets the unique identifier for this item of evidence.
	 * @param id the <code>id</code> of the item of evidence.
	 */
	public void setId(int id)
	{
		m_HashMap.put("id", new Integer(id));
	}
	
	
	/**
	 * Returns the data type of this item of evidence, if one has been
	 * set e.g. 'p2pslog', 'mdhlog', 'apache_log'.
	 * @return the name of the data type, of <code>null</code> if no
	 * 	data type field has been set for this evidence record.
	 */
	public String getDataType()
	{
		return (String) m_HashMap.get("type");
	}
	
	
	
	/**
	 * Returns the data type of this item of evidence, if one has been
	 * set e.g. 'p2pslog', 'mdhlog', 'apache_log'.
	 * @return the name of the data type, of <code>null</code> if no
	 * 	data type field has been set for this evidence record.
	 */
	public void setDataType(String dataType)
	{
		m_HashMap.put("type", dataType);
	}
	
	
	/**
	 * Returns the <code>severity</code> of this item of evidence.
	 * @return the <code>severity</code> of the event record, such as 'minor',
	 * 'major' or 'critical', or 'none' if there is no severity set.
	 */
	public Severity getSeverity()
    {
		String severityStr = (String) m_HashMap.get("severity");
		if (severityStr == null)
		{
			severityStr = Severity.NONE.toString();
		}
		
		return Enum.valueOf(Severity.class, severityStr.toUpperCase());
    }


	/**
	 * Sets the severity of the item of evidence
	 * @param severity the event <code>severity</code> such as 'minor', 
	 * 'major' or 'critical'
	 */
	public void setSeverity(Severity severity)
    {
		m_HashMap.put("severity", severity.toString());
    }
	
	
	/**
	 * Returns the description of this item of evidence data.
	 * @return the description.
	 */
	public String getDescription()
    {
		return (String) m_HashMap.get("description");
    }


	/**
	 * Sets the description of this item of evidence data.
	 * @param description the evidence description.
	 */
	public void setDescription(String description)
    {
		m_HashMap.put("description", description);
    }
	
	/**
	 * Returns the name of the source (server) of this item of evidence.
	 * @return the source (server) name.
	 */
    public String getSource()
    {
    	return (String) m_HashMap.get("source");
    }


    /**
	 * Sets the name of the source (server) of this item of evidence.
	 * @param source the source (server) name.
	 */
    public void setSource(String source)
    {
    	m_HashMap.put("source", source);
    }
    
	
	/**
	 * Returns the time this item of evidence occurred.
	 * @return recorded time of item of evidence.
	 * @param timeFrame	time frame context of record e.g. WEEK, DAY, 
	 * 		HOUR, MINUTE or SECOND.
	 */
	public Date getTime()
	{
		Date time = (Date)m_HashMap.get(COLUMN_NAME_TIME);
		return time;
	}
	
	
	/**
	 * Sets the time this item of evidence occurred.
	 * @param time recorded time of item of evidence.
	 */
	public void setTime(Date time)
	{
		m_HashMap.put(COLUMN_NAME_TIME, time);
	}
	
	
	/**
	 * Returns the external plugin or null.
	 * @return The String representing the external plugin or null if this piece 
	 * of evidence is stored internally
	 */
	public String getExternalPlugin()
	{
		String plugin = (String)m_HashMap.get("external_plugin");
		return plugin;
	}
	
	
	/**
	 * Sets the external plugin.
	 * @param plugin
	 */
	public void setExternalPlugin(String plugin)
	{
		m_HashMap.put("external_plugin", plugin);
	}
	
	    
    /**
     * Get the value of the object property.
     * @param propertyName to lookup.
     * @return The value of property propertyName.
     */
    public Object get(String propertyName)
    {
    	return m_HashMap.get(propertyName);
    }
    
    /**
     * Set the value of the object property propertyName to propertyValue
     * @param propertyName to set.
     */
    public void set(String propertyName, Object propertyValue)
    {
    	m_HashMap.put(propertyName, propertyValue);
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
	 * Returns all the objects properties in a Map.
	 * @return Map of objects properties by string.
	 */
    public Map<String, Object> getProperties()
    {
    	return this.m_HashMap;
    }
    
    /**
     * Set all the objects properties. All current properties are lost and only
     * the new ones will be set.
     * @param properties
     */
    public void setProperties(Map<String, Object> properties)
    {
    	m_HashMap.clear();
    	m_HashMap.putAll(properties);
    }
    
    @Override
    public boolean equals(Object obj)
    {
    	if (this == obj)
    	{
    		return true;
    	}
    	
    	if (! (obj instanceof Evidence))
    	{
    		return false;
    	}
    	
    	Evidence other = (Evidence)obj;
    	
    	return m_HashMap.equals(other.getProperties());
    }
    
    
    /**
     * Tests equality between two <code>Evidence</code> objects.
     * All of the objects properties are tested for equality except the <code>
     * Evidence Id</code>. This is for cases where you want to establish 2 <code>
     * Evidence</code> objects are equal but the <code>Id</code> may not have
     * been set. 
     * @param other
     * @return True if both objects except the <code>Evidence Id</code> are equal.
     */
    public boolean isEqualExceptEvidenceId(Evidence other)
    {
    	boolean result = this.getDataType().equals(other.getDataType());
    	result = result && this.getSource().equals(other.getSource());
    	result = result && this.getDataType().equals(other.getDataType());
    	result = result && this.getDescription().equals(other.getDescription());    	
    	
    	return result;
    }
    
	/**
	 * Returns a summary of this Evidence record.
	 * @return String representation of the Evidence Record, showing all properties
	 * and values.
	 */
    public String toString()
    {
		StringBuilder strRep = new StringBuilder();
		strRep.append(getProperties());
		
		return strRep.toString();
    }

}
