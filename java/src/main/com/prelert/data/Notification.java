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

import java.util.List;
import java.util.ArrayList;

import com.prelert.data.Attribute;


/**
 * Class encapsulating Prelert Notifications.
 */
public class Notification
{
	private String m_Type;
	private DataSourceType m_DataSource;
	private String m_Description;
	private long m_TimeMs;
	private int m_Count;
	private int m_Severity;
	private List<Attribute> m_Attributes;
	
	
	public Notification(String type)
	{
		setType(type);
		m_Attributes = new ArrayList<Attribute>();
	}


	public Notification(String type, String sourceName)
	{
		setType(type);
		m_DataSource = new DataSourceType(sourceName, DataSourceCategory.NOTIFICATION);
		m_Attributes = new ArrayList<Attribute>();
	}
	
	
	public List<Attribute> getAttributes()
	{
		return m_Attributes;
	}


	public void setAttributes(List<Attribute> values)
	{
		m_Attributes = values;
	}


	public void addAttribute(Attribute value)
	{
		m_Attributes.add(value);
	}


	public DataSourceType getDataSource()
	{
		return m_DataSource;
	}


	public void setDataSource(DataSourceType dataSource)
	{
		this.m_DataSource = dataSource;
	}


	public String getDescription()
	{
		return m_Description;
	}


	public void setDescription(String description)
	{
		this.m_Description = description;
	}


	public long getTimeMs()
	{
		return m_TimeMs;
	}


	public void setTimeMs(long timeMs)
	{
		this.m_TimeMs = timeMs;
	}


	public int getCount()
	{
		return m_Count;
	}


	public void setCount(int count)
	{
		this.m_Count = count;
	}


	/**
	 * Severity values: 	
	 * <ul>
	 * <li>Clear = 1</li>
	 * <li>Unknown = 2</li>
	 * <li>Minor = 4</li>
	 * <li>Critical = 6</li>
	 * </ul>
	 * @return
	 */
	public int getSeverity()
	{
		return m_Severity;
	}


	public void setSeverity(int severity)
	{
		this.m_Severity = severity;
	}

	
	public String getType()
	{
		return m_Type;
	}


	public void setType(String value)
	{
		m_Type = value;
	}


	/**
	 * Returns a string reperesentations of this object in the Prelert notification
	 * Xml format.
	 * @return xml string.
	 */
	public String toXmlString()
	{
		StringBuilder builder = new StringBuilder("<notification>\n");

		builder.append("<type>").append(XmlStringEscaper.escapeXmlString(m_Type)).append("</type>\n");
		builder.append("<description>").append(XmlStringEscaper.escapeXmlString(m_Description)).append("</description>\n");
		builder.append("<severity>").append(m_Severity).append("</severity>\n");
		builder.append("<count>").append(m_Count).append("</count>\n");
		// Time needs to be seconds since the epoch rather than milliseconds
		builder.append("<time>").append(m_TimeMs / 1000l).append("</time>\n");
		builder.append("<source>").append(XmlStringEscaper.escapeXmlString(m_DataSource.getName())).append("</source>\n");

		for (Attribute attr : m_Attributes)
		{
			builder.append(attr.toXmlTagInternal()).append('\n');
		}

		builder.append("</notification>\n");

		return builder.toString();
	}


}
