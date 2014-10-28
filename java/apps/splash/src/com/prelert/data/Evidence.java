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

package com.prelert.data;

import java.io.Serializable;
import java.util.Date;


/**
 * Class encapsulating the properties of an item of evidence.
 * @author Pete Harverson
 */
public class Evidence implements Serializable
{
	private int			m_Id;
	private String		m_DataType;
	private Severity	m_Severity;
	private String		m_Description;
	private Date		m_Time;
	private String		m_Source;
	
	
	public Evidence()
	{
		m_Severity = Severity.NONE;
	}
	
	
	/**
	 * Constructs an item of evidence with the specified id.
	 * @param id identifier for the evidence.
	 */
	public Evidence(int id)
	{
		this();
		m_Id = id;
	}
	
	
	/**
	 * Returns the unique identifier for this item of evidence.
	 * @return the <code>id</code> of the item of evidence.
	 */
	public int getId()
	{
		return m_Id;
	}
	
	
	/**
	 * Sets the unique identifier for this item of evidence.
	 * @param id the <code>id</code> of the item of evidence.
	 */
	public void setId(int id)
	{
		m_Id = id;
	}
	
	
	/**
	 * Returns the data type of this item of evidence, if one has been
	 * set e.g. 'p2pslog', 'mdhlog', 'apache_log'.
	 * @return the name of the data type, of <code>null</code> if no
	 * 	data type field has been set for this evidence record.
	 */
	public String getDataType()
	{
		return m_DataType;
	}
	
	
	/**
	 * Returns the data type of this item of evidence, if one has been
	 * set e.g. 'p2pslog', 'mdhlog', 'apache_log'.
	 * @return the name of the data type, of <code>null</code> if no
	 * 	data type field has been set for this evidence record.
	 */
	public String setDataType(String dataType)
	{
		return m_DataType = dataType;
	}
	
	
	/**
	 * Returns the <code>severity</code> of this item of evidence.
	 * @return the <code>severity</code> of the event record, such as 'minor',
	 * 'major' or 'critical', or 'none' if there is no severity set.
	 */
	public Severity getSeverity()
    {
    	return m_Severity;
    }


	/**
	 * Sets the severity of the item of evidence.
	 * @param severity the event <code>severity</code> such as 'minor', 
	 * 'major' or 'critical'
	 */
	public void setSeverity(Severity severity)
    {
    	m_Severity = severity;
    }
	
	
	/**
	 * Returns the description of this item of evidence data.
	 * @return the description.
	 */
	public String getDescription()
    {
    	return m_Description;
    }


	/**
	 * Sets the description of this item of evidence data.
	 * @param description the evidence description.
	 */
	public void setDescription(String description)
    {
		m_Description = description;
    }
	
	
	/**
	 * Returns the time this item of evidence occurred.
	 * @return recorded time of item of evidence.
	 */
	public Date getTime()
	{
		return m_Time;
	}
	
	
	/**
	 * Sets the time this item of evidence occurred.
	 * @param time recorded time of item of evidence.
	 */
	public void setTime(Date time)
	{
		m_Time = time;
	}


	/**
	 * Returns the name of the source (server) of this item of evidence.
	 * @return the source (server) name.
	 */
    public String getSource()
    {
    	return m_Source;
    }


    /**
	 * Sets the name of the source (server) of this item of evidence.
	 * @param source the source (server) name.
	 */
    public void setSource(String source)
    {
    	m_Source = source;
    }

}
