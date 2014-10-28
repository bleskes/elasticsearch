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

package com.prelert.proxy.plugin.introscope;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Logger;

import com.prelert.data.TimeSeriesDataPoint;

/**
 *  Limited capacity cache of <code>TimeSeriesDataPoint</code>s.
 */
public class IntroscopeDataPointCache  
{
	private static Logger s_Logger = Logger.getLogger(IntroscopeDataPointCache.class);
	
	/**
	 * Maximum number of time series which are held in the cache.
	 */
	public static final int CAPACITY = 600;
	
	Queue<String> m_SeriesKeys;
	Map<String, List<TimeSeriesDataPoint>> m_DataPointsByExternalKey;

	
	public IntroscopeDataPointCache()
	{
		m_SeriesKeys = new ArrayBlockingQueue<String>(CAPACITY);
		m_DataPointsByExternalKey = new HashMap<String, List<TimeSeriesDataPoint>>();
	}
	
	
	/**
	 * Insert points into the cache. If the capacity is exceeded then the 
	 * oldest set of <code>TimeSeriesDataPoint</code> will be dropped.
	 * @param externalKey
	 * @param start
	 * @param end
	 * @param points
	 */
	synchronized public void insertPoints(String externalKey, Date start, Date end,
								List<TimeSeriesDataPoint> points)
	{
		s_Logger.debug("insertPoints(" + externalKey + ", " + start + ", " + end + points.size() + " points");
		String key = createKey(externalKey, start, end);
		
		if (!m_SeriesKeys.offer(key))
		{
			s_Logger.debug("DataPoint cache capacity exceeded dropping: " + key);
			
			// we are out of capacity so drop the element and remove 
			// from the hash
			String outKey = m_SeriesKeys.remove();
			m_DataPointsByExternalKey.remove(outKey);
		}
		
		m_DataPointsByExternalKey.put(key, points);
	}
	
	
	/**
	 * Returns the <code>TimeSeriesDataPoints</code> for the given key and time span
	 * or <code>null</code> if they do not exist
	 * @param externalKey
	 * @param start
	 * @param end
	 * @return the TimeSeriesDataPoints or <code>null</code>
	 */
	synchronized public List<TimeSeriesDataPoint> getPoints(String externalKey, 
													Date start, Date end)
	{
		s_Logger.debug("getPoints(" + externalKey + ", " + start + ", " + end);
		
		String key = createKey(externalKey, start, end);
		
		return m_DataPointsByExternalKey.get(key);
	}
	
	
	/**
	 * Creates a key string by combining <code>externalKey</code>, 
	 * <code>start</code> & <code>end</code>.
	 * @param externalKey
	 * @param start
	 * @param end
	 * @return
	 */
	private String createKey(String externalKey, Date start, Date end)
	{
		return externalKey + start.toString() + end.toString();
	}
	
}
