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

import java.util.Collection;
import java.util.Date;

import org.apache.log4j.Logger;

import com.prelert.data.TimeSeriesData;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.proxy.inputmanager.loadmonitor.LoadMonitor;
import com.prelert.proxy.plugin.introscope.ClwConnectionPool;

/**
 * Monitors the following Custom metrics for excessive load on the
 * Enterprise Manager.
 * A number of metrics including Harvest Duration and Overall Capacity
 * are watched, if one of them is high for a number of points then 
 * the ServiceState is set to <code>ServiceState.SERVICE_BAD</code>
 * else it is <code>ServiceState.SERVICE_OK</code>
 */
public class IntroscopeLoadMonitor implements LoadMonitor
{	
	public static String CUSTOM_METRIC_AGENT = "Custom Metric Host \\(Virtual\\)\\|" +
												"Custom Metric Process \\(Virtual\\)\\|" +
												"Custom Metric Agent \\(Virtual\\)";
	
	/**
	 * Metrics to monitor.
	 */
	public static String HARVEST_DURATION_METRIC = "Enterprise Manager\\|Tasks:Harvest Duration \\(ms\\)";
	public static String OVERALL_CAPACITY_METRIC = "Enterprise Manager:Overall Capacity \\(%\\)";
	public static String GC_CAPACITY_METRIC = "Enterprise Manager\\|Health:GC Capacity \\(%\\)";
	public static String CPU_CAPACITY_METRIC = "Enterprise Manager\\|Health:CPU Capacity \\(%\\)";
	public static String HEAP_CAPACITY_METRIC = "Enterprise Manager\\|Health:Heap Capacity \\(%\\)";
	public static String SMARTSTOR_CAPACITY_METRIC = "Enterprise Manager\\|Health:SmartStor Capacity \\(%\\)";
	public static String GC_DURATION_METRIC = "Enterprise Manager\\|GC Heap:GC Duration \\(ms\\)";
	
	private static final MetricLimit HARVEST_DURATION = new MetricLimit(HARVEST_DURATION_METRIC, 3500);
	private static final MetricLimit OVERALL_CAPACITY = new MetricLimit(OVERALL_CAPACITY_METRIC, 70);	
	private static final MetricLimit GC_CAPACITY = new MetricLimit(GC_CAPACITY_METRIC, 70);	
	private static final MetricLimit CPU_CAPACITY = new MetricLimit(CPU_CAPACITY_METRIC, 70);	
	private static final MetricLimit HEAP_CAPACITY = new MetricLimit(HEAP_CAPACITY_METRIC, 70);
	private static final MetricLimit SMARTSTOR_CAPACITY = new MetricLimit(SMARTSTOR_CAPACITY_METRIC, 70);
	private static final MetricLimit GC_DURATION = new MetricLimit(GC_DURATION_METRIC, 1000);
	


	private static Logger s_Logger = Logger.getLogger(IntroscopeLoadMonitor.class);
	
	private Thread m_MonitorThread;
	
	private int m_UpdateIntervalSecs;
	
	private ServiceState m_ServiceState;
	
	private MetricLimit m_DangerMetric;
	
	volatile private int m_HoursOffset;
	
	volatile private boolean m_Quit; 
	
	volatile private ClwConnectionPool m_ClwConnectionPool;
	
	
	public IntroscopeLoadMonitor()
	{
		m_UpdateIntervalSecs = 60; 
		
		m_ServiceState = ServiceState.SERVICE_OK;
		
		m_HoursOffset = 0;
		
		m_Quit = false;
	}
	
	
	/**
	 * Runs forever. Queries the values of the choosen metrics to monitor and updates 
	 * the <code>ServiceState</code> member if the metric threshold is blown for a
	 * number of consecutive points. 
	 * 
	 * The sleeps for <code>getUpdateInterval()</code> mille seconds.
	 * 
	 */
	@Override
	public void run() 
	{
		MetricLimit[] monitorMetrics = {HARVEST_DURATION, 
										OVERALL_CAPACITY, 
										GC_CAPACITY,
										CPU_CAPACITY,
										HEAP_CAPACITY, 
										SMARTSTOR_CAPACITY,
										GC_DURATION};
		
		while (m_Quit != true)
		{
			Date queryStart = new Date();
			
			synchronized(m_ServiceState)
			{
				m_ServiceState = ServiceState.SERVICE_OK;
				m_DangerMetric = null;
			}
			
			// Get the connection.
			IntroscopeConnection connection;
			try
			{
				connection = m_ClwConnectionPool.acquireConnection();

				try
				{
					// Get the monitor data
					for (MetricLimit metric : monitorMetrics)
					{
						int previousMins = ((getUpdateIntervalSecs() -1) / 60 ) + 1;
						
						Collection<TimeSeriesData> datas = connection.getMetricDataForLastNMinutes(
																		CUSTOM_METRIC_AGENT, metric.m_Metric, previousMins);

						for (TimeSeriesData data : datas)
						{	
							double peak = 0.0;
							long time = 0;
							for (TimeSeriesDataPoint pt : data.getDataPoints())
							{
								if (pt.getValue() >= peak)
								{
									peak = pt.getValue();
									time = pt.getTime();
								}

								metric.addDataPoint(pt.getValue());
								
								if (metric.isOverloaded())
								{
									synchronized(m_ServiceState)
									{
										m_ServiceState = ServiceState.SERVICE_BAD;
										m_DangerMetric = metric;

										String msg = "Introscope load Monitor threshold of " + metric.getThreshold() + 
																" exceeded for metric " + metric.getMetric();
										s_Logger.error(msg);
									}
								}
							}	

							s_Logger.info("Metric = " + metric.getMetric() + ", peak value = " + peak + " at time = " + new Date(time));
						}
					}
				}
				finally
				{
					m_ClwConnectionPool.releaseConnection(connection);
				}
			}
			catch (Exception e)
			{
				s_Logger.error("Could not acquire connection from pool: " + 
						m_ClwConnectionPool.getConnectionConfig());
			}

			Date queryFin = new Date();
			
			if (Thread.interrupted())
			{
				s_Logger.info("Introscope Monitor Thread interrupted.");
				break;
			}
			
			try 
			{
				int intervalMs = getUpdateIntervalSecs() * 1000;
				int queryDuration = (int)(queryFin.getTime() - queryStart.getTime()); 
				int sleepTime = intervalMs - queryDuration;
				if (sleepTime > 0)
				{
					Thread.sleep(sleepTime);
				}
			} 
			catch (InterruptedException e) 
			{
				s_Logger.info("Introscope Monitor Thread interrupted whilst sleeping");
				break;
			}
		}
		
		s_Logger.info("Introscope Monitor Thread exiting.");
	}
	
	
	/**
	 * Sleep interval between updating the metrics.
	 * @return
	 */
	public int getUpdateIntervalSecs()
	{
		return m_UpdateIntervalSecs;
	}
	
	public void setUpdateIntervalSecs(int intervalSecs)
	{
		m_UpdateIntervalSecs = intervalSecs;
	}

	
	@Override	
	public ServiceState getCurrentServiceState() 
	{
		synchronized(m_ServiceState)
		{
			return m_ServiceState;
		}
	}
	
	/**
	 * Returns the metric the exceeded its safe threshold value.
	 * @return
	 */
	public MetricLimit getExceededMetric()
	{
		synchronized(m_ServiceState)
		{
			return m_DangerMetric;
		}
	}
	
	/**
	 * Start the monitor thread.
	 */
	synchronized public void start()
	{
		if (m_MonitorThread == null)
		{
			m_MonitorThread = new Thread(this, "IntroscopeLoadMonitor");
		}
		
		if (m_MonitorThread != null)
		{
			Thread.State threadState = m_MonitorThread.getState();
			
			if (threadState == Thread.State.NEW)
			{
				m_MonitorThread.start();
			}
		}
	}
	
	/**
	 * Stop and join the monitor thread.
	 */
	public void stop()
	{
		m_Quit = true;

		if (m_MonitorThread != null)
		{
			m_MonitorThread.interrupt();

			try
			{
				m_MonitorThread.join();
			}
			catch (InterruptedException e)
			{
				s_Logger.warn("Introscope Load Monitor thread interrupted whilst stopping its thread.");
			}
		}
	}
	

	/**
	 * Stops the LoadMonitor Thread and rebuilds it. The 
	 * state of this object after the call is equivalent 
	 * to it's freshly constructed state.
	 */
	public void reset()
	{
		stop();
		
		m_MonitorThread = null;
		
		m_ServiceState = ServiceState.SERVICE_OK;
		
		m_Quit = false;
	}
	
	
	/**
	 * If the Introscope server is in a different time 
	 * zone or is returning data at a different time this value will
	 * be non-zero.
	 * 
	 * Add this value to the time you want to query for to get 
	 * the correct data.
	 * 
	 * @return
	 */
	public int getHoursOffset()
	{
		return m_HoursOffset;
	}
	
	
	/**
	 * The CLW connection pool.</br>
	 * This class monitors a specific Enterprise Manager 
	 * which is set by the connnection parameters on this 
	 * connection pool object.
	 * 
	 * @return
	 */
	public ClwConnectionPool getConnectionPool()
	{
		return m_ClwConnectionPool;
	}
	
	public void setClwConnectionPool(ClwConnectionPool pool)
	{
		m_ClwConnectionPool = pool;
	}
	
	
	/**
	 * Helper Class simply pairs a metric with a threshold value.
	 */
	static public class MetricLimit
	{
		static final int MAX_HIGH_VALUE_COUNT = 3;
		String m_Metric;
		double m_DangerThreshold;
		
		int m_HighValueCount;
		
		
		public MetricLimit(String metric, double threshold)
		{
			m_Metric = metric;
			m_DangerThreshold = threshold;
			
			m_HighValueCount = 0;
		}
		
		public void addDataPoint(double point)
		{
			if (point > m_DangerThreshold)
			{
				m_HighValueCount++;
			}
			else
			{
				m_HighValueCount = 0;
			}
		}
		
		
		/** 
		 * A metric is overloaded if the threshold value is exceeded 
		 * for <code>MAX_HIGH_VALUE_COUNT</code> consecutive points.
		 * @return
		 */
		public boolean isOverloaded()
		{
			return m_HighValueCount > MAX_HIGH_VALUE_COUNT;
		}
		
		public String getMetric()
		{
			return m_Metric;
		}
		
		public double getThreshold()
		{
			return m_DangerThreshold;
		}
		
	}

}
