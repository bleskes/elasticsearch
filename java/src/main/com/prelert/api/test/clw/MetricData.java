/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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

package com.prelert.api.test.clw;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Simple class stores the Introscope CLW metric data in a 
 * format useful for the Metric Uplaod API. Consists of a 
 * timestamp and a list of metric points (metric path & value)
 * pairs. All points in the list have the same timestamp 
 * {@link #getCollectionTime()}.
 * <p/>
 * The natural ordering for MetricData is by CollectionTime.
 */
public class MetricData implements Comparable<MetricData>
{
	private Date m_CollectionTime;
	private List<MetricData.Point> m_Points;
	
	public MetricData(Date collectionTime)
	{
		m_CollectionTime = collectionTime;
		m_Points = new ArrayList<MetricData.Point>();
	}
	
	/**
	 * Add a metric path/value data point to the list.
	 * @param path
	 * @param value
	 */
	public void addPoint(String path, double value)
	{
		MetricData.Point pt = this.new Point(path, value);
		m_Points.add(pt);
	}
	
	/**
	 * Return the list of metric path/value pairs.
	 * @return List of Metric data points.
	 */
	public List<MetricData.Point> getPoints()
	{
		return m_Points;
	}
	
	/**
	 * Get the collection time for all the points
	 * in this object.
	 * @return The collection timestamp for the objects data points.
	 */
	public Date getCollectionTime()
	{
		return m_CollectionTime;
	}
	
	/**
	 * Metric path and value
	 */
	public class Point
	{
		String m_Path;
		double m_Value;
		
		public Point(String path, double value)
		{
			m_Path = path;
			m_Value = value;
		}
		
		public String getPath()
		{
			return m_Path;
		}
		
		public double getValue()
		{
			return m_Value;
		}
	}

	/**
	 * Order by collection time
	 */
	@Override
	public int compareTo(MetricData o) 
	{
		return this.m_CollectionTime.compareTo(o.m_CollectionTime);
	};

}
