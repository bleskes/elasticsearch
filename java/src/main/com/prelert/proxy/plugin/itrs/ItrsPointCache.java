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

package com.prelert.proxy.plugin.itrs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.itrsgroup.activemodel.dataview.ActiveDataView;
import com.itrsgroup.activemodel.dataview.ActiveDataViewChange;
import com.itrsgroup.activemodel.dataview.ActiveDataViewColumnHeader;
import com.itrsgroup.activemodel.dataview.ActiveDataViewData;
import com.itrsgroup.activemodel.dataview.ActiveDataViewEvent;
import com.itrsgroup.activemodel.dataview.ActiveDataViewListener;
import com.itrsgroup.activemodel.dataview.ActiveDataViewRow;
import com.prelert.data.Attribute;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesData;
import com.prelert.data.TimeSeriesDataPoint;

import static com.prelert.proxy.plugin.itrs.ItrsPlugin.ITRS_DATATYPE;

/**
 * Class which implements the ActiveDataViewListener call back
 * to receive data from the ITRS ActiveDataViewModel. 
 * 
 * The class caches ITRS data as TimeSeriesData.
 * 
 * ITRS data is always string data which is then parsed to extract 
 * a numerical value. Sometimes the data has an unit such as 'MHz' or 
 * '%' but this is ignored. Non-numerical data is ignored. 
 */
public class ItrsPointCache implements ActiveDataViewListener 
{
	public static final int POLLING_INTERVAL_MS = 5000;
	
	public static final String NUM_VALUE_REGEX = "\\s*([0-9]*\\.?[0-9]+)\\s*(.*)";
	public static final String PATH_REGEX = "/(\\w+)\\[\\(@name=\"([^\"]+)\"";
	
	public static final int DEFAULT_CACHE_AGE_SECONDS = 300;
	
	private static final Logger s_Logger = Logger.getLogger(ItrsPointCache.class);

	private Pattern m_ValueRegex;
	private Pattern m_PathNameRegex;
	
	private volatile long m_FirstPointTime;
	private volatile long m_LastPointTime;
	
	private int m_CacheAgeSeconds;
	
	private Map<String, TimeSeriesData> m_TsDataByPaths;

	public ItrsPointCache()
	{
		m_ValueRegex = Pattern.compile(NUM_VALUE_REGEX);
		m_PathNameRegex = Pattern.compile(PATH_REGEX);
		
		m_FirstPointTime = 0;
		m_LastPointTime = 0;
		
		m_CacheAgeSeconds = DEFAULT_CACHE_AGE_SECONDS;
		
		m_TsDataByPaths = new HashMap<String, TimeSeriesData>();
	}
	

	/**
	 * Parse the ITRS data event and create Prelert time series 
	 * from it.
	 * 
	 * ITRS data is always string data which is then parsed to extract 
	 * a numerical value. Sometimes the data has an unit such as 'MHz' or 
	 * '%' but this is ignored. Non-numerical data is ignored.
	 * 
	 * The column header is the metric and the source is the row header.
	 */
	@Override	
	public void onActiveDataViewEvent(ActiveDataViewEvent event) 
	{
		long epochTime = new Date().getTime();
		
		switch (event.getChange().getType())
		{
		case CELL_UPDATE: // value has changed data
			processCellUpdate(epochTime, event.getDataView().getPath(), event);
			break;
		case POPULATED:
			processPopulatedPoints(epochTime, event.getDataView().getPath(),
									event.getDataView().getContent());
			break;

		case SEVERITY_UPDATED:  // TODO what is this?
			break; 

		case UNPOPULATED:  
			// Remove metric from cache
			upPopulatePoints(event);
			break;

		case HEADLINE_UPDATED: // Update of aggregation stats???
		case HEADLINE_ADDED:
		case HEADLINE_REMOVED:
			return;

		case ROW_ADDED:   
		case ROW_REMOVED:
			// A new time series will be created when new data is seen.
			return;

		case COLUMN_HEADERS_CHANGED: 
			// Column header is the actual metric.
			// A new time series will be created when data is seen.
			return;
		}
	}
	
	
	/**
	 * Process the populated event. This means that there is a full 
	 * table of data to be processed and added to the point cache.
	 * 
	 * @param epochTime The time of the event.
	 * @param path The ITRS path
	 * @param event
	 */
	private void processPopulatedPoints(long epochTime, String path, ActiveDataViewData content)
	{
		m_LastPointTime = epochTime;

		List<ActiveDataViewColumnHeader> colHeaders = content.getColumnHeaders();	
		
		synchronized(m_TsDataByPaths)
		{
			for (ActiveDataViewRow row : content.getRows())
			{
				for (int i=0; i<row.getCells().size(); i++)
				{
					String value = row.getCells().get(i).getValue();

					if (value == null || value.isEmpty())
					{
						continue;
					}
					
					// Try to parse the value first
					Matcher matcher = m_ValueRegex.matcher(value);
					if (matcher.matches())
					{
						String digits = matcher.group(1);
						try
						{
							Double floatValue = Double.parseDouble(digits);
							TimeSeriesDataPoint newPoint = new TimeSeriesDataPoint(epochTime, floatValue); 
							
							String metric = colHeaders.get(i).getName();
							String fullPath = new StringBuilder().append(path).append("/").append(metric).append("/").append(row.getName()).toString();
							TimeSeriesData tsData = m_TsDataByPaths.get(fullPath);
							if (tsData == null)
							{
								TimeSeriesConfig config = createConfigFromEvent(path, metric, row.getName(), content);
								if (config == null)
								{
									return;
								}
								tsData = new TimeSeriesData(config, new ArrayList<TimeSeriesDataPoint>());
								m_TsDataByPaths.put(fullPath, tsData);
							}
							
							tsData.getDataPoints().add(newPoint);
							
							
							 if (epochTime < tsData.getDataPoints().get(tsData.getDataPoints().size() -1).getTime())
								 s_Logger.error("wrong order");
							 
							 assert epochTime >= tsData.getDataPoints().get(tsData.getDataPoints().size() -1).getTime();
						}
						catch (NumberFormatException nfe)
						{
							s_Logger.warn(String.format("Cannot parse %s as a number", digits));
						}
					}

				}
			}
		}
	}
	
	
	 
	/**
	 * Update a point for an single metric. 
	 *  
	 * @param epochTime The time of the event.
	 * @param path The ITRS dataview path.
	 * @param event
	 */
	private void processCellUpdate(long epochTime, String path, ActiveDataViewEvent event)
	{
		m_LastPointTime = epochTime;
		
		if (event.getDataView().getSampleInterval() == null)
		{
			s_Logger.error("NUll Sample Interval");
			return;
		}

		ActiveDataViewChange change = event.getChange();
		ActiveDataViewData content = event.getDataView().getContent();
		int sampleInterval = event.getDataView().getSampleInterval() * 1000; // TODO secs/ms?? 

		synchronized(m_TsDataByPaths)
		{
			String metric = content.getColumnHeaders().get(change.getColumn()).getName();
			ActiveDataViewRow row = content.getRows().get(change.getRow());
			String rowName = row.getName();

			 
			 Matcher matcher = m_ValueRegex.matcher(row.getCells().get(change.getColumn()).getValue());
			 if (matcher.matches())
			 {
				 String digits = matcher.group(1);
				 try
				 {
					 Double floatValue = Double.parseDouble(digits);
					 TimeSeriesDataPoint newPoint = new TimeSeriesDataPoint(epochTime, floatValue);
					 
					 String fullPath = new StringBuilder().append(path).append("/").append(metric).append("/").append(rowName).toString();
					 TimeSeriesData tsData = m_TsDataByPaths.get(fullPath);
					 if (tsData == null)
					 {
						 TimeSeriesConfig config = createConfigFromEvent(path, metric, rowName, content);
						 if (config == null)
						 {
							 return;
						 }
						 tsData = new TimeSeriesData(config, new ArrayList<TimeSeriesDataPoint>());
						 m_TsDataByPaths.put(fullPath, tsData);

						 // new time series so just add the point no need to fill missing values.
					 }
					 else
					 {
						 // Populate missing data points.
						 TimeSeriesDataPoint lastPt = tsData.getDataPoints().get(tsData.getDataPoints().size() -1);
						 
						 long lastPointTime = lastPt.getTime() + sampleInterval;
						 while (lastPointTime + sampleInterval < epochTime) // leave space for the new point 
						 {
							 TimeSeriesDataPoint pt = new TimeSeriesDataPoint(lastPointTime, lastPt.getValue());
							 tsData.getDataPoints().add(pt);

							 lastPointTime += sampleInterval;
						 }
					 }
					 
					 tsData.getDataPoints().add(newPoint);
					 
					 assert epochTime >= tsData.getDataPoints().get(tsData.getDataPoints().size() -1).getTime();
				 }
				 catch (NumberFormatException nfe)
				 {
					 s_Logger.warn(String.format("Cannot parse %s as a number", digits));
				 }
			 }			
		 }
	 }

	 
	 /**
	  * Remove the specified metric from the cache and don't
	  * maintain it's points.
	  * 
	  * @param event
	  */
	 private void upPopulatePoints(ActiveDataViewEvent event)
	 {
		 ActiveDataViewChange change = event.getChange();
		 ActiveDataViewData content = event.getDataView().getContent();
		 
		 synchronized(m_TsDataByPaths)
		 {
			 String path = event.getDataView().getPath();
			 
			 // TODO how to handle this when get an unpopulate event
			 // with no column or row header??
			 if (content.getColumnHeaders().size() == 0 ||
				 content.getRows().size() == 0)
			{
				return;
			}
			 
			 String metric = content.getColumnHeaders().get(change.getColumn()).getName();
			 String rowName = content.getRows().get(change.getRow()).getName();
			 
			 String fullPath = new StringBuilder().append(path).append("/").append(metric).append("/").append(rowName).toString();
			 
			 s_Logger.warn("Unpopulating path: " + fullPath);
			 m_TsDataByPaths.remove(fullPath);
		 }
	 }

	
	/**
	 * Create the time series data config.
	 * 
	 * This function will fail and return <code>null</code> if the nodes
	 * cannot be parsed out of itrsPath.
	 * 
	 * @param itrsPath - The dataview path.
	 * @param columnHeader
	 * @param rowName
	 * @param content
	 * @return <code>null</code> if itrsPath cannot be parsed for the node elements
	 * else the new config.
	 */
	private TimeSeriesConfig createConfigFromEvent(String itrsPath, String columnHeader, 
								String rowName, ActiveDataViewData content)
	{
		
		ItrsPath processedPath = parsePath(itrsPath);
		if (processedPath.isValid() == false)
		{
			s_Logger.warn(String.format("Cannot parse path %s. Elements gateway, probe and managedEntity are expected", itrsPath));
			return null;
		}

		int position = 1;
		List<Attribute> attrs = new ArrayList<Attribute>();
//		attrs.add(new Attribute("Gateway", processedPath.m_Gateway.m_Value, "/", position++));
//		attrs.add(new Attribute("Probe", processedPath.m_Probe.m_Value, "/", position++));
//		attrs.add(new Attribute("Entity", processedPath.m_Entity.m_Value, "/", position++));
		if (processedPath.m_Sampler != null)
		{
			attrs.add(new Attribute("Sampler", processedPath.m_Sampler.m_Value, "/", position++));
		}
		if (processedPath.m_DataView != null)
		{
			attrs.add(new Attribute("DataView", processedPath.m_DataView.m_Value, "/", position++));
		}
		attrs.add(new Attribute(content.getRowHeader(), rowName, "/", position));

		TimeSeriesConfig config = new TimeSeriesConfig(ITRS_DATATYPE, columnHeader, processedPath.m_Entity.m_Value, attrs);
		config.setSourcePosition(0);
		
		return config;
	}
	
	
	/**
	 * Extract the /pathelement[(@name="element name")] parameters from the path
	 * 
	 * For example in the path 
	 * '/geneos/gateway[(@name="Demo Gateway")]/directory/probe[(@name="Basic Probe")]'
	 * the following pairs need to be extracted. 
	 * 	<ul><li>gateway="Demo Gateway"</li>
	 * 	<li>probe="Basic Probe"</li></ul>
	 * 
	 * @param path
	 */
	private ItrsPath parsePath(String path)
	{
		ItrsPath orderedPath = new ItrsPath();
		
		Matcher matcher = m_PathNameRegex.matcher(path);
		
		while (matcher.find())
		{		
			if (matcher.groupCount() == 2)
			{
				String node = matcher.group(1);
				String value = matcher.group(2);
				
				orderedPath.addNode(node, value);
			}
		}
		
		return orderedPath;
	}
	
	
	/**
	 * Removes all the points from the cache older than
	 * timeNow - MaxCacheAge
	 * 
	 * @param timeNowMs
	 */
	private void filterOldPoints(long timeNowMs)
	{
		long maxAge = timeNowMs - m_CacheAgeSeconds * 1000;

		synchronized(m_TsDataByPaths)
		{
			for (TimeSeriesData tsData : m_TsDataByPaths.values())
			{
				long lastTime = 0;
				Iterator<TimeSeriesDataPoint> itr = tsData.getDataPoints().iterator();
				
				int startSize = tsData.getDataPoints().size();
				
				while (itr.hasNext())
				{
					TimeSeriesDataPoint pt = itr.next();
					
					try {
					assert pt.getTime() > lastTime;  // TODO remove if works
					}
					catch (AssertionError e)
					{
						s_Logger.error(e);
					}
					
					
					
					lastTime = pt.getTime();
					
					if (pt.getTime() <= maxAge)
					{
						if (itr.hasNext() == false)
						{
							// don't remove the last point just update the time
							pt.setTime(timeNowMs);
						}
						else
						{
							itr.remove();
						}
					}
				}
				
				if (tsData.getDataPoints().size() == 0 /*&& startSize > 0 */)
				{
					assert startSize == 0;
				}
			}
		}
	}
	
	
	/**
	 * Age of the oldest points in the cache.
	 * Defaults to DEFAULT_CACHE_AGE_SECONDS
	 * @return
	 */
	public int getMaxCacheAge()
	{
		return m_CacheAgeSeconds;
	}
	
	/**
	 * Set the limit on the age of the oldest points in the data 
	 * cache.
	 * @param ageSeconds
	 */
	public void setMaxCacheAge(int ageSeconds)
	{
		m_CacheAgeSeconds = ageSeconds;
	}
	
	
	/**
	 * Returns all the points the cache between start and end. 
	 * 
	 * @param start
	 * @param end
	 */
	public Collection<TimeSeriesData> getPoints(Date start, Date end)
	{
		if (m_LastPointTime == 0) // no data yet.
		{
			return Collections.emptyList();
		}
		
		if ((end.getTime() < m_FirstPointTime) || (start.getTime() > m_LastPointTime))
		{
			return Collections.emptyList();
		}
		
		
		long epochStart = start.getTime();
		long epochEnd = end.getTime();
		
		List<TimeSeriesData> results = new ArrayList<TimeSeriesData>();
			
		synchronized(m_TsDataByPaths)
		{
			for (TimeSeriesData tsData : m_TsDataByPaths.values())
			{
				assert tsData.getDataPoints().size() > 0;
				if (tsData.getDataPoints().size() == 0)
				{
					s_Logger.error("No points in time series");
					continue;
				}
				
				
				boolean gotPoints = false;
				List<TimeSeriesDataPoint> copyPoints = new ArrayList<TimeSeriesDataPoint>();
				
				for (TimeSeriesDataPoint pt : tsData.getDataPoints())
				{
					if (pt.getTime() >= epochStart && pt.getTime() < epochEnd)
					{
						copyPoints.add(pt);
						gotPoints = true;
					}			
					else if (pt.getTime() >= epochEnd)
					{
						break;
					}
				}
				
				
				// TODO debug only check no duplicate points.
				if (copyPoints.size() >= 2)
				{
					long lasttime = copyPoints.get(0).getTime();
					for (int i=1; i<copyPoints.size(); i++)
					{
						if (copyPoints.get(i).getTime() > lasttime)
							{
								s_Logger.error("feck");
							}
						lasttime = copyPoints.get(i).getTime();
					}
				}
				
				
				if (gotPoints)
				{
					results.add(new TimeSeriesData(tsData.getConfig(), copyPoints));
				}
			}
		}
		
		
		filterOldPoints(new Date().getTime() - 20000);
		
		return results;
	}	
	
	
	
	/**
	 * Polling thread reads data from an ActiveDataView
	 * sleeping for PollingInterval milli-seconds between
	 * requests.
	 */
	@SuppressWarnings("unused")
	private class ItrsPoller extends Thread
	{
		ActiveDataView m_ActiveDataView;
		
		private volatile int m_PollingInterval = POLLING_INTERVAL_MS;
		
		private volatile boolean m_Quit = false;
		
		
		private ItrsPoller(ActiveDataView activeDataView)
		{
			m_ActiveDataView = activeDataView;
		}
		
		
		/**
		 * Stop this thread from polling for data.
		 * Returns immediatley does not block until the thread
		 * terminates. 
		 */
		public void quit()
		{
			m_Quit = true;
			
			this.interrupt(); // wake if sleeping.
		}
		
		/**
		 * The the polling interval in milli-seconds.
		 * @return
		 */
		public int getPollingInterval()
		{
			return m_PollingInterval;
		}
		
		public void setPollingInterval(int intervalMs)
		{
			m_PollingInterval = intervalMs;
		}
		
		@Override
		public void run()
		{
			while (m_Quit == false)
			{
				ActiveDataViewData data = m_ActiveDataView.getContent();
						
				processPopulatedPoints(new Date().getTime(), m_ActiveDataView.getPath(), data);

				try
				{
					Thread.sleep(m_PollingInterval);
				}
				catch (InterruptedException e)
				{
					s_Logger.info("Sleeping ITRS poller thread interrupted", e);
				}
			}
		}
	}
	
	
	/**
	 * Class representing an ITRS dataview path. 
	 */
	private class ItrsPath
	{
		Node m_Gateway = null;
		Node m_Probe = null;
		Node m_Entity = null;
		Node m_Sampler = null;
		Node m_DataView = null;

		
		void addNode(String name, String value)
		{
			if  ("gateway".equals(name))
			{
				m_Gateway = new Node(name, value);
			}
			else if ("probe".equals(name))
			{
				m_Probe = new Node(name, value);
			}
			else if ("managedEntity".equals(name))
			{
				m_Entity = new Node(name, value);
			}
			else if ("sampler".equals(name))
			{
				m_Sampler = new Node(name, value);
			}
			else if ("dataview".equals(name))
			{
				m_DataView = new Node(name, value);
			}		
		}
		
		
		boolean isValid()
		{
			return m_Gateway != null && m_Probe != null && m_Entity != null ;
					//&& m_Sampler != null && m_DataView != null;
		}

		
		private class Node
		{
			@SuppressWarnings("unused")
			String m_Name;
			String m_Value;
			
			Node(String name, String value)
			{
				m_Name = name;
				m_Value = value;
			}
		}
	}
}
