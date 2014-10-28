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

package com.prelert.proxy.plugin.olbm;

import com.prelert.data.TimeSeriesConfig;


/**
 * Class to group all the properties of a single OLBM data point.
 *
 * @author David Roberts
 */
public class OLBMDataPoint
{
	private String m_Title;
	private String m_Metric;
	private String m_Source;
	private long m_TimeMS;
	private double m_Value;


	/**
	 * Construct from the string representation of an external key.
	 * @param externalKey The external key string representation
	 */
	public OLBMDataPoint(String title, String metric, String source,
							long timeMS, double value)
	{
		if (title == null || metric == null || source == null)
		{
			throw new NullPointerException("OLBMDataPoint constructed with null argument " +
						"title = " + title +
						"metric = " + metric +
						"source = " + source);
		}

		m_Title = title;
		m_Metric = metric;
		m_Source = source;
		m_TimeMS = timeMS;
		m_Value = value;
	}


	/**
	 * Access to title.
	 * @return The point's title.
	 */
	public String getTitle()
	{
		return m_Title;
	}


	/**
	 * Access to metric.
	 * @return The point's metric.
	 */
	public String getMetric()
	{
		return m_Metric;
	}


	/**
	 * Access to source.
	 * @return The point's source.
	 */
	public String getSource()
	{
		return m_Source;
	}


	/**
	 * Access to time (in milliseconds since the epoch).
	 * @return The point's time (in milliseconds since the epoch).
	 */
	public long getTimeMS()
	{
		return m_TimeMS;
	}


	/**
	 * Set the time (in milliseconds since the epoch).
	 * @param timeMS The time to set (in milliseconds since the epoch).
	 */
	public void setTimeMS(long timeMS)
	{
		m_TimeMS = timeMS;
	}


	/**
	 * Access to value.
	 * @return The point's value.
	 */
	public double getValue()
	{
		return m_Value;
	}


	/**
	 * Convert the point to a string representation.
	 * @return A string representation of the point.
	 */
	public String toString()
	{
		StringBuilder strRep = new StringBuilder("{ title = ");
		strRep.append(m_Title);
		strRep.append(", metric = ");
		strRep.append(m_Metric);
		strRep.append(", source = ");
		strRep.append(m_Source);
		strRep.append(", time = ");
		strRep.append(m_TimeMS);
		strRep.append(", value = ");
		strRep.append(m_Value);
		strRep.append(" }");

		return strRep.toString();
	}


	/**
	 * Get a key that uniquely identifies the non-time-specific parameters of
	 * this point.
	 * @return A key that uniquely identifies the non-time-specific parameters
	 *         of this point.
	 */
	public String toExternalKey()
	{
		StringBuilder key = new StringBuilder(m_Title);
		key.append('\t');
		key.append(m_Metric);
		key.append('\t');
		key.append(m_Source);

		return key.toString();
	}


	/**
	 * Create a <code>TimeSeriesConfig</code> object that represents the
	 * non-time-specific parameters of this point.
	 * @return A <code>TimeSeriesConfig</code> object reflecting the
	 *         non-time-specific parameters of this data point.
	 */
	public TimeSeriesConfig toTimeSeriesConfig()
	{
		// Note that we can ONLY get away with creating the type in this way
		// because the OLBM plugin only supplies data to be stored internally
		// within the Prelert database.  This WOULD NOT be acceptable in a
		// plugin that had to service the GUI.
		TimeSeriesConfig timeSeriesConfig =
							new TimeSeriesConfig(m_Title, m_Metric, m_Source);

		timeSeriesConfig.setExternalKey(toExternalKey());

		return timeSeriesConfig;
	}

}

