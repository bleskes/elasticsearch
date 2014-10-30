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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import com.prelert.data.ConnectionStatus;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesData;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.proxy.data.SourceConnectionConfig;
import com.prelert.proxy.plugin.InternalPlugin;
import com.prelert.proxy.plugin.Plugin;


/**
 * Plugin for Bank of America's Online Banking Monitor (OLBM).
 *
 * Time series data is scraped from HTML pages, which refresh every minute.  As
 * a result, data cannot be re-retrieved, and must be stored internally within
 * the Prelert database.  Hence this plugin will never have to service the GUI,
 * and the majority of plugin methods do not need to be implemented.
 *
 * @author David Roberts
 */
public class OLBMPlugin extends Plugin implements InternalPlugin
{
	static Logger s_Logger = Logger.getLogger(OLBMPlugin.class);

	/**
	 * Data types are created from the HTML page titles, but this is what we
	 * report as an overall type.  Note that we can ONLY get away with creating
	 * the type in this way because the OLBM plugin only supplies data to be
	 * stored internally within the Prelert database.  This WOULD NOT be
	 * acceptable in a plugin that had to service the GUI.
	 */
	private static final String REPORTED_DATA_TYPE = "OLBM";

	/**
	 * Interval (in seconds) between OLBM refreshes.
	 */
	private int m_IntervalSecs;

	/**
	 * Set of URLs to scrape.
	 */
	private Set<String> m_URLs;

	/**
	 * Threads running the URL readers.
	 */
	private List<Thread> m_URLReaderThreads;

	/**
	 * Regexes used to extract data from OLBM HTML pages.
	 */
	private String m_TypeRegex;
	private String m_BarchartRegex;
	private String m_MetricRegex;
	private String m_SourcesRegex;
	private String m_SourceRegex;
	private String m_TimeRegex;
	private String m_ValueRegex;

	/**
	 * Identifying information about the different sites OLBM is monitoring.
	 */
	private List<SiteInfo> m_Sites;

	/**
	 * Store of data points received from URL readers and available for
	 * retrieval by an input manager.
	 */
	private ArrayList<OLBMDataPoint> m_Points;

	/**
	 * Local copy of the Properties passed to loadProperties(Properties).
	 * Kept for the duplicate() function.
	 */
	private Properties m_Properties;


	/**
	 * Construct a plugin - this isn't suitable for use until the properties
	 * have been loaded.
	 */
	public OLBMPlugin()
	{
		super();
		
		setDataSourceType(new DataSourceType(REPORTED_DATA_TYPE, DataSourceCategory.TIME_SERIES));

		m_URLs = new TreeSet<String>();
		m_URLReaderThreads = new ArrayList<Thread>();
		m_Sites = new ArrayList<SiteInfo>();
		m_Points = new ArrayList<OLBMDataPoint>();
	}


	/**
	 * Make sure the URL reader threads are stopped when this object is
	 * destroyed.
	 */
	@Override
	protected void finalize() throws Throwable
	{
		try
		{
			stopURLReaders();
		}
		finally
		{
			super.finalize();
		}
	}


	/**
	 * Shut down URL reader threads when the input manager associated with this
	 * plugin is stopped.
	 */
	@Override
	public void stop()
	{
		try
		{
			stopURLReaders();
		}
		catch (InterruptedException e)
		{
			s_Logger.warn("OLBM plugin interrupted whilst stopping URL reader threads");
		}
	}


	/**
	 * Shut down URL reader threads.
	 */
	private void stopURLReaders() throws InterruptedException
	{
		synchronized (m_URLReaderThreads)
		{
			// Stop the URL reader threads
			for (Thread thread : m_URLReaderThreads)
			{
				thread.interrupt();
			}

			for (Thread thread : m_URLReaderThreads)
			{
				thread.join();
			}

			// Clear the list so that this method will do nothing if called
			// again
			m_URLReaderThreads.clear();
		}
	}

	

	/**
	 * Load the settings required by this plugin from its properties file.
	 * Compulsory settings are:
	 *  1) interval
	 *  2) url# (one or more properties beginning with "url")
	 *  3) site# (one or more properties beginning with "site")
	 *  4) typeRegex
	 *  5) barchartRegex
	 *  6) metricRegex
	 *  7) sourcesRegex
	 *  8) sourceRegex
	 *  9) timeRegex
	 * 10) valueRegex
	 * 
	 * @param config Isn't used here.
	 */
	@Override
	public boolean configure(SourceConnectionConfig config, Properties props)
	throws InvalidPluginPropertyException
	{
		m_Properties = props;

		String intervalStr = getCompulsoryProperty(props, "interval");
		try
		{
			m_IntervalSecs = Integer.parseInt(intervalStr);
		}
		catch (NumberFormatException e)
		{
			throw new InvalidPluginPropertyException("OLBM interval '" + intervalStr +
											"' cannot be parsed to an integer");
		}

		// Get the properties that begin with "url" and "site"
		Set<String> propNames = props.stringPropertyNames();
		for (String propName : propNames)
		{
			if (propName.startsWith("url"))
			{
				String url = props.getProperty(propName);
				if (url == null)
				{
					s_Logger.error("Properties inconsistency: " + propName);
				}
				else
				{
					m_URLs.add(url);
				}
			}
			else if (propName.startsWith("site"))
			{
				String siteStr = props.getProperty(propName);
				if (siteStr == null)
				{
					s_Logger.error("Properties inconsistency: " + propName);
				}
				else
				{
					String[] parts = siteStr.split(",");
					if (parts.length != 2 ||
						parts[0].trim().length() == 0 ||
						parts[1].trim().length() == 0)
					{
						throw new InvalidPluginPropertyException("Site '" +
								propName +
								"' does not contain two comma separated parts : " +
								siteStr);
					}

					SiteInfo siteInfo = this.new SiteInfo(parts[0].trim(),
															parts[1].trim());
					m_Sites.add(siteInfo);

					s_Logger.info("Added site info : " + siteInfo);
				}
			}
		}

		if (m_URLs.isEmpty())
		{
			throw new InvalidPluginPropertyException("No URLs specified in OLBM " +
					"properties file - URL property names must begin with 'url'");
		}

		if (m_Sites.isEmpty())
		{
			throw new InvalidPluginPropertyException("No sites specified in OLBM " +
					"properties file - site property names must begin with 'site'");
		}

		// Read in regexes
		m_TypeRegex = getCompulsoryProperty(props, "typeRegex");
		m_BarchartRegex = getCompulsoryProperty(props, "barchartRegex");
		m_MetricRegex = getCompulsoryProperty(props, "metricRegex");
		m_SourcesRegex = getCompulsoryProperty(props, "sourcesRegex");
		m_SourceRegex = getCompulsoryProperty(props, "sourceRegex");
		m_TimeRegex = getCompulsoryProperty(props, "timeRegex");
		m_ValueRegex = getCompulsoryProperty(props, "valueRegex");

		// Make sure all the regexes compile
		Pattern typeRegex = compileRegex(m_TypeRegex, "typeRegex");
		Pattern barchartRegex = compileRegex(m_BarchartRegex, "barchartRegex");
		Pattern metricRegex = compileRegex(m_MetricRegex, "metricRegex");
		Pattern sourcesRegex = compileRegex(m_SourcesRegex, "sourcesRegex");
		Pattern sourceRegex = compileRegex(m_SourceRegex, "sourceRegex");
		Pattern timeRegex = compileRegex(m_TimeRegex, "timeRegex");
		Pattern valueRegex = compileRegex(m_ValueRegex, "valueRegex");

		try
		{
			startURLReaders(typeRegex,
							barchartRegex,
							metricRegex,
							sourcesRegex,
							sourceRegex,
							timeRegex,
							valueRegex);
		}
		catch (Exception e)
		{
			throw new InvalidPluginPropertyException("OLBM plugin failed to set up URL scraper threads");
		}
		
		
		return true;
	}

	
	/**
	 * Test the connection by opening the connections to all
	 * the URLs specified in properties (properties that start
	 * with "url").
	 * 
	 * WARNING this function hasn't been tested.
	 */
	@Override
	public ConnectionStatus testConnection(SourceConnectionConfig config, Properties properties)
	{
		if (properties.size() == 0)
		{
			return new ConnectionStatus(ConnectionStatus.Status.CONNECTION_FAILED);
		}
		
		Set<String> propNames = properties.stringPropertyNames();
		for (String propName : propNames)
		{
			if (propName.startsWith("url"))
			{
				String urlStr = properties.getProperty(propName);
				if (urlStr != null)
				{
					try
					{
						URL url = new URL(urlStr);
						url.openConnection();
					}
					catch (MalformedURLException me)
					{
						return new ConnectionStatus(ConnectionStatus.Status.CONNECTION_FAILED);
					}
					catch (IOException ioe)
					{
						return new ConnectionStatus(ConnectionStatus.Status.CONNECTION_FAILED);
					}
				}
			}
		}
		
		return new ConnectionStatus(ConnectionStatus.Status.CONNECTION_OK);
	}

	/**
	 * Attempt to start a thread per URL to scrape data from it.
	 */
	private void startURLReaders(Pattern typeRegex,
								Pattern barchartRegex,
								Pattern metricRegex,
								Pattern sourcesRegex,
								Pattern sourceRegex,
								Pattern timeRegex,
								Pattern valueRegex) throws Exception
	{
		synchronized (m_URLReaderThreads)
		{
			// Next create all the URL readers
			for (String url : m_URLs)
			{
				String graphLabel = null;

				for (SiteInfo siteInfo : m_Sites)
				{
					if (url.contains(siteInfo.getURLSubstring()))
					{
						graphLabel = siteInfo.getGraphLabel();

						// Each URL must correspond to a single site
						break;
					}
				}

				if (graphLabel == null)
				{
					s_Logger.warn("A configured URL does not contain the " +
								"substring for any of the configured sites : " +
								url);
					continue;
				}

				URLReader reader = new URLReader(this,
												url,
												graphLabel,
												m_IntervalSecs,
												typeRegex,
												barchartRegex,
												metricRegex,
												sourcesRegex,
												sourceRegex,
												timeRegex,
												valueRegex);

				Thread thread = new Thread(reader);

				m_URLReaderThreads.add(thread);
			}

			if (m_URLReaderThreads.isEmpty())
			{
				s_Logger.warn("No valid URLs were specified - " +
								"OLBM plugin has nothing to do");
			}

			// If the creation worked, start all the threads
			for (Thread thread : m_URLReaderThreads)
			{
				thread.start();
			}
		}
	}


	/**
	 * Try to compile a regular expression.  If it fails to compile, report
	 * which config file property it came from.
	 * @param regex The regular expression to compile.
	 * @param propName The name of the property the regular expression was read
	 *                 from.
	 * @return The compiled regular expression.
	 */
	private Pattern compileRegex(String regex, String propName)
										throws InvalidPluginPropertyException
	{
		Pattern result;
		try
		{
			result = Pattern.compile(regex);
		}
		catch (PatternSyntaxException e)
		{
			s_Logger.error(e);
			throw new InvalidPluginPropertyException("OLBM property '" +
									propName +
									"' contains an invalid regular expression");
		}

		return result;
	}


	/**
	 * Get a property from a properties object, throwing an exception if it's
	 * not present.
	 * @param props The properties to search.
	 * @param propName The name of the property to search for.
	 * @return The property value.
	 */
	private String getCompulsoryProperty(Properties props, String propName)
										throws InvalidPluginPropertyException
	{
		String propValue = props.getProperty(propName);
		if (propValue == null)
		{
			throw new InvalidPluginPropertyException("'" + propName +
									"' property not specified for OLBM plugin");
		}

		return propValue;
	}


	/**
	 * Retrieve points from all available time series up to a given time.
	 * Points returned will NOT be available for re-retrieval subsequently.
	 * Note that the minimum time is ignored by this plugin - all points earlier
	 * than the maximum time are returned.
	 * @param minTime Ignored by this plugin.
	 * @param maxTime The latest time for which to return a point.
	 * @param intervalSecs Ignored by this plugin.
	 * @return Collection of <code>TimeSeriesData</code> containing the
	 *         available points.
	 */
	@Override
	public Collection<TimeSeriesData> getAllDataPointsForTimeSpan(
													Date minTime, Date maxTime,
													int intervalSecs)
	{
		// Call the synchronized method to remove the points to be returned from
		// our store as quickly as possible
		Collection<OLBMDataPoint> points = removePointsToTime(maxTime);

		s_Logger.info("OLBM plugin has accumulated " + points.size() +
						" points up to time " + maxTime.getTime());

		// Now the points are removed from the store we don't need to lock out
		// the URL readers, and can restructure the points into the required
		// data structures at our leisure.

		HashMap<String, TimeSeriesData> externalKeyToDataMap =
										new HashMap<String, TimeSeriesData>();

		for (OLBMDataPoint point : points)
		{
			String key = point.toExternalKey();
			TimeSeriesData timeSeriesData = externalKeyToDataMap.get(key);
			if (timeSeriesData == null)
			{
				TimeSeriesConfig config = point.toTimeSeriesConfig();
				List<TimeSeriesDataPoint> dataPoints =
										new ArrayList<TimeSeriesDataPoint>();
				timeSeriesData = new TimeSeriesData(config, dataPoints);
				externalKeyToDataMap.put(key, timeSeriesData);
			}

			TimeSeriesDataPoint dataPoint =
									new TimeSeriesDataPoint(point.getTimeMS(),
															point.getValue());
			timeSeriesData.getDataPoints().add(dataPoint);
		}

		Collection<TimeSeriesData> result = externalKeyToDataMap.values();

		// Set the min and max times in the results
		for (TimeSeriesData timeSeries : result)
		{
			TimeSeriesConfig config = timeSeries.getConfig();
			List<TimeSeriesDataPoint> dataPoints = timeSeries.getDataPoints();
			if (config != null && dataPoints != null)
			{
				int lastPoint = dataPoints.size() - 1;
				if (lastPoint >= 0)
				{
					config.setMinTime(new Date(dataPoints.get(0).getTime()));
					config.setMaxTime(new Date(dataPoints.get(lastPoint).getTime()));
				}
				else
				{
					s_Logger.warn("Time series data with config " + config +
									" has no points");
				}
			}
		}

		return result;
	}


	/**
	 * Get the usual interval (in seconds) between OLBM data points.
	 * @param datatype Not used (because OLBM doesn't have different intervals for
	 *               different metrics).
	 * @param metric Not used (because OLBM doesn't have different intervals for
	 *               different metrics).
	 * @return The usual interval in seconds.
	 */
	@Override
	public int getUsualPointIntervalSecs()
	{
		return m_IntervalSecs;
	}
	
	
	/**
	 * Set the usual interval (in seconds) between OLBM data points.
	 */
	@Override
	public void setUsualPointIntervalSecs(int value)
	{
		m_IntervalSecs = value;
	}


	/**
	 * Add some points to the store.  This method is synchronized, as it will be
	 * called from multiple URL reader threads.  To prevent excessive locking
	 * and unlocking, a collection of points is added rather than a single
	 * point.
	 * @param points The collection of points to be added.
	 */
	synchronized void addPoints(Collection<OLBMDataPoint> points)
	{
		m_Points.addAll(points);
	}


	/**
	 * Retrieve points from the store up to a specified time.  Any given point
	 * may only be retrieved once.  Retrieved points are NOT available for
	 * subsequent re-retrieval.  This method is synchronized to prevent the URL
	 * reader threads adding more points while it's running.
	 * @param maxTime The maximum time for which to retrieve points.
	 * @return A collection of points with times less than or equal to the
	 *         specified time.
	 */
	private synchronized Collection<OLBMDataPoint> removePointsToTime(Date maxTime)
	{
		ArrayList<OLBMDataPoint> result = new ArrayList<OLBMDataPoint>(m_Points.size());
		ArrayList<OLBMDataPoint> newer = new ArrayList<OLBMDataPoint>(m_Points.size());

		for (OLBMDataPoint point : m_Points)
		{
			if (point.getTimeMS() > maxTime.getTime())
			{
				newer.add(point);
			}
			else
			{
				result.add(point);
			}
		}

		m_Points = newer;

		return result;
	}


	/**
	 * Returns a new instance of a <code>OLBMPlugin</code> which has all
	 * its members set to copies of this object's members.
	 */
	@Override
	public OLBMPlugin duplicate()
	{
		s_Logger.info("Duplicating OLBM Plugin");

		OLBMPlugin clone = new OLBMPlugin();

		clone.setName(getName());
		try
		{
			clone.configure(null, m_Properties);
		}
		catch (InvalidPluginPropertyException e)
		{
			s_Logger.error("Error duplicating OLBMPlugin");
			s_Logger.error(e);
		}

		return clone;
	}


	/**
	 * Inner class to store the site keys for a single site.  Different keys are
	 * used in the URL and for the graph labels.
	 */
	private class SiteInfo
	{

		private String m_URLSubstring;
		private String m_GraphLabel;


		/**
		 * Construct from a URL substring and graph label.  These must not be
		 * null.
		 */
		public SiteInfo(String urlSubstring, String graphLabel)
		{
			if (urlSubstring == null)
			{
				throw new NullPointerException("null passed as urlSubstring");
			}

			if (graphLabel == null)
			{
				throw new NullPointerException("null passed as graphLabel");
			}

			m_URLSubstring = urlSubstring;
			m_GraphLabel = graphLabel;
		}


		/**
		 * Get the URL substring.
		 * @return The URL substring.
		 */
		public String getURLSubstring()
		{
			return m_URLSubstring;
		}


		/**
		 * Get the graph label.
		 * @return The graph label.
		 */
		public String getGraphLabel()
		{
			return m_GraphLabel;
		}


		/**
		 * Create a string representation of this object.
		 * @return A string representation of this object.
		 */
		public String toString()
		{
			StringBuilder strRep = new StringBuilder("{ url substring = ");
			strRep.append(m_URLSubstring);
			strRep.append(", graph label = ");
			strRep.append(m_GraphLabel);
			strRep.append(" }");

			return strRep.toString();
		}

	}

}
