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

package com.prelert.proxy.plugin.vantage;

import java.util.ArrayList;
import java.util.List;

import com.prelert.data.Attribute;
import com.prelert.data.TimeSeriesConfig;


/**
 * Vantage external keys have string representations of the form:
 * metric source attr1=val1 attr2=val2
 *
 * The gaps between the fields are tabs, not spaces.  There can be zero or more
 * attributes.
 *
 * @author David Roberts
 */
public class VantageExternalKey
{
	private String m_ExternalKey;
	private String m_Metric;
	private String m_Source;
	private List<Attribute> m_Attributes;


	/**
	 * Construct from the string representation of an external key.
	 * @param externalKey The external key string representation
	 */
	public VantageExternalKey(String externalKey)
						throws NullPointerException, IllegalArgumentException
	{
		if (externalKey == null)
		{
			throw new NullPointerException("external key string is null");
		}

		m_ExternalKey = externalKey;

		String[] tokens = m_ExternalKey.split("\t");
		if (tokens.length < 2)
		{
			throw new IllegalArgumentException("external key string contains too few tab separated fields: " +
																externalKey);
		}

		int count = 0;
		m_Metric = tokens[count++];
		m_Source = tokens[count++];

		m_Attributes = new ArrayList<Attribute>();
		while (count < tokens.length)
		{
			String attrStr = tokens[count];

			int splitAt = attrStr.indexOf('=');
			if (splitAt == -1)
			{
				throw new IllegalArgumentException("external key string attribute format invalid: " +
																attrStr);
			}

			Attribute attribute = new Attribute(attrStr.substring(0, splitAt),
												attrStr.substring(splitAt + 1));
			m_Attributes.add(attribute);

			++count;
		}
	}


	/**
	 * Construct from a metric, source and attributes.
	 * @param metric The metric in the form "human readable [ID]"
	 * @param source The name of the source machine
	 * @param attributes List of zero or more attributes
	 */
	public VantageExternalKey(String metric, String source,
						List<Attribute> attributes) throws NullPointerException
	{
		if (metric == null)
		{
			throw new NullPointerException("metric is null");
		}

		if (source == null)
		{
			throw new NullPointerException("source is null");
		}

		m_Metric = metric;
		m_Source = source;
		m_Attributes = attributes;

		StringBuilder strRep = new StringBuilder(m_Metric);
		strRep.append('\t');
		strRep.append(m_Source);

		if (m_Attributes != null)
		{
			for (Attribute attribute : m_Attributes)
			{
				strRep.append('\t');
				strRep.append(attribute.getAttributeName());
				strRep.append('=');
				strRep.append(attribute.getAttributeValue());
			}
		}

		m_ExternalKey = strRep.toString();
	}


	/**
	 * Get the complete external key in string form
	 * @return The external key in string form
	 */
	public String toString()
	{
		return m_ExternalKey;
	}


	/**
	 * Get the hash code for this external key.  This is taken from the hash
	 * code of the external key in string form.
	 * @return The hash code for this external key.
	 */
	public int hashCode()
	{
		return m_ExternalKey.hashCode();
	}


	/**
	 * Is this object equal to another object?  We only need compare the full
	 * external key string, as the other members are subsequences of the overall
	 * key.
	 * @param obj The object to compare with this one.
	 * @return true if the two objects are equal; otherwise false.
	 */
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}

		if (!(obj instanceof VantageExternalKey))
		{
			return false;
		}

		return m_ExternalKey.equals(obj.toString());
	}


	/**
	 * Access to the metric in the form "human readable [ID]"
	 * @return The name of the metric
	 */
	public String getMetric()
	{
		return m_Metric;
	}


	/**
	 * Access to the source
	 * @return The name of the source
	 */
	public String getSource()
	{
		return m_Source;
	}


	/**
	 * Access to attributes
	 * @return List of zero or more attributes
	 */
	public List<Attribute> getAttributes()
	{
		return m_Attributes;
	}


	/**
	 * Convert to a <code>TimeSeriesConfig</code> object.
	 * @param type The data type to assign to the time series config.
	 * @return A <code>TimeSeriesConfig</code> object reflecting the contents of
	 *         this external key.
	 */
	public TimeSeriesConfig toTimeSeriesConfig(String type)
	{
		TimeSeriesConfig timeSeriesConfig =
				new TimeSeriesConfig(type, m_Metric, m_Source, m_Attributes);

		timeSeriesConfig.setExternalKey(m_ExternalKey);

		return timeSeriesConfig;
	}

}
