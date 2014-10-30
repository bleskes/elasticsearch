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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;


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
public class URLReader implements Runnable
{
	static Logger s_Logger = Logger.getLogger(URLReader.class);

	/**
	 * Number of milliseconds in six hours.
	 */
	private static final long SIX_HOUR_MS = 21600000l;

	/**
	 * Number of hours it takes for a 12 hour clock to wrap.
	 */
	private static final int TWELVE_HOUR_CLOCK_HOURS = 12;

	/**
	 * Plugin to send points to.
	 */
	private OLBMPlugin m_Plugin;

	/**
	 * URL to scrape.
	 */
	private URL m_URL;

	/**
	 * Which graph label is valid for this particular URL?
	 */
	private String m_GraphLabel;

	/**
	 * Interval (in milliseconds) between OLBM refreshes.
	 */
	private long m_IntervalMS;

	/**
	 * Regexes used to extract data from OLBM HTML pages.
	 */
	private Pattern m_TypeRegex;
	private Pattern m_BarchartRegex;
	private Pattern m_MetricRegex;
	private Pattern m_SourcesRegex;
	private Pattern m_SourceRegex;
	private Pattern m_TimeRegex;
	private Pattern m_ValueRegex;

	/**
	 * When did we last get data?  We may get the same data twice, but don't
	 * want to pass it on twice, so we need to remember this.
	 */
	private long m_LastDataTime;


	/**
	 * Construct a plugin - this isn't suitable for use until the properties
	 * have been loaded.
	 */
	public URLReader(OLBMPlugin plugin,
					String url,
					String graphLabel,
					int intervalSecs,
					Pattern typeRegex,
					Pattern barchartRegex,
					Pattern metricRegex,
					Pattern sourcesRegex,
					Pattern sourceRegex,
					Pattern timeRegex,
					Pattern valueRegex) throws MalformedURLException
	{
		m_Plugin = plugin;
		m_URL = new URL(url);
		m_GraphLabel = graphLabel;
		m_IntervalMS = intervalSecs * 1000l;
		m_TypeRegex = typeRegex;
		m_BarchartRegex = barchartRegex;
		m_MetricRegex = metricRegex;
		m_SourcesRegex = sourcesRegex;
		m_SourceRegex = sourceRegex;
		m_TimeRegex = timeRegex;
		m_ValueRegex = valueRegex;
		m_LastDataTime = 0l;
	}


	/**
	 * Main thread loop.  Repeatedly read from the URL and parse the text until
	 * the thread is interrupted.  Text is retrieved from the URL twice per
	 * refresh interval, to avoid missing any updates.
	 */
	public void run()
	{
		boolean priorFailure = false;

		s_Logger.info("Starting URL reader for site '" + m_GraphLabel +
						"' reading from URL " + m_URL);

		while (!Thread.interrupted())
		{
			try
			{
				String urlText = getURLText();

				s_Logger.debug("Read " + urlText.length() +
								" characters from URL " + m_URL);

				try
				{
					parseURLText(urlText);

					// This debug is here to give reassurance that we've
					// successfully recovered from a transient failure condition
					if (priorFailure)
					{
						s_Logger.info("Successful read from URL " + m_URL +
										" following prior failure");
						priorFailure = false;
					}
				}
				catch (Exception e)
				{
					s_Logger.error("Problem parsing text from URL " + m_URL);
					s_Logger.error(e);
					priorFailure = true;
				}
			}
			catch (Exception e)
			{
				s_Logger.error("Problem reading from URL " + m_URL);
				s_Logger.error(e);
				priorFailure = true;
			}

			try
			{
				// Read the URL twice per interval
				Thread.sleep(m_IntervalMS / 2);
			}
			catch (InterruptedException e)
			{
				break;
			}
		}

		s_Logger.info("Exiting URL reader for URL " + m_URL);
	}


	/**
	 * Read all the text from our URL.  All linebreaks in the text are removed,
	 * i.e. the output is a single line.
	 * @return The text read from our URL.
	 */
	private String getURLText() throws Exception
	{
		StringBuilder urlPage = new StringBuilder();

		InputStream inputStream = m_URL.openStream();
		try
		{
			InputStreamReader inputStreamReader =
											new InputStreamReader(inputStream);
			try
			{
				BufferedReader bufferedReader =
										new BufferedReader(inputStreamReader);
				try
				{
					String line = bufferedReader.readLine();
					while (line != null)
					{
						urlPage.append(line);
						line = bufferedReader.readLine();
					}
				}
				finally
				{
					bufferedReader.close();
				}
			}
			finally
			{
				inputStreamReader.close();
			}
		}
		finally
		{
			inputStream.close();
		}

		return urlPage.toString();
	}


	/**
	 * Parse the text from our URL, generating data points to pass on to the
	 * plugin.
	 * @param urlText The text read from our URL.
	 */
	private void parseURLText(String urlText) throws Exception
	{
		// First match the overall data type
		Matcher typeMatcher = m_TypeRegex.matcher(urlText);
		if (typeMatcher.find() == false)
		{
			s_Logger.warn("Type regex " + m_TypeRegex.pattern() +
							" did not match URL text");
			s_Logger.debug("Non-matching URL text was " + urlText);
			return;
		}

		String type = typeMatcher.group(1);
		if (type == null)
		{
			throw new IllegalArgumentException("Type regex " +
												m_TypeRegex.pattern() +
												" did not capture a group");
		}

		ArrayList<OLBMDataPoint> points = new ArrayList<OLBMDataPoint>();
		long lastDataTime = 0l;
		long firstDataTime = 0l;

		// Repeatedly pull bar charts from the text
		Matcher barchartMatcher = m_BarchartRegex.matcher(urlText);
		while (barchartMatcher.find() == true)
		{
			String barchart = barchartMatcher.group(1);
			if (barchart == null)
			{
				throw new IllegalArgumentException("Bar chart regex " +
													m_BarchartRegex.pattern() +
													" did not capture a group");
			}

			if (!barchart.contains(m_GraphLabel))
			{
				// It will often be the case that a bar chart has no information
				// for the correct site, so don't spam the log here
				continue;
			}

			// For each bar chart, attempt to get metric, sources and time
			Matcher metricMatcher = m_MetricRegex.matcher(barchart);
			if (metricMatcher.find() == false)
			{
				s_Logger.warn("Metric regex " + m_MetricRegex.pattern() +
								" did not match bar chart text");
				s_Logger.debug("Non-matching bar chart text was " + barchart);
				continue;
			}

			String metric = metricMatcher.group(1);
			if (metric == null)
			{
				throw new IllegalArgumentException("Metric regex " +
													m_MetricRegex.pattern() +
													" did not capture a group");
			}

			Matcher sourcesMatcher = m_SourcesRegex.matcher(barchart);
			if (sourcesMatcher.find() == false)
			{
				s_Logger.warn("Sources regex " + m_SourcesRegex.pattern() +
								" did not match bar chart text");
				s_Logger.debug("Non-matching bar chart text was " + barchart);
				continue;
			}

			String sources = sourcesMatcher.group(1);
			if (sources == null)
			{
				throw new IllegalArgumentException("Sources regex " +
													m_SourcesRegex.pattern() +
													" did not capture a group");
			}

			Matcher timeMatcher = m_TimeRegex.matcher(barchart);
			if (timeMatcher.find() == false)
			{
				s_Logger.warn("Time regex " + m_TimeRegex.pattern() +
								" did not match bar chart text");
				s_Logger.debug("Non-matching bar chart text was " + barchart);
				continue;
			}

			// To improve the chance of finding a valid time, the time matcher
			// may match more than one group (but it must still match at least
			// one group)
			int timeGroups = timeMatcher.groupCount();
			if (timeGroups < 1)
			{
				throw new IllegalArgumentException("Time regex " +
													m_TimeRegex.pattern() +
													" did not capture any groups");
			}

			long timeMS = -1l;
			long adjustment = 0l;
			for (int timeGroup = timeGroups; timeGroup > 0; --timeGroup)
			{
				timeMS = convertTime(timeMatcher.group(timeGroup));
				if (timeMS < -1l)
				{
					// Unexpected format
					break;
				}

				if (timeMS >= 0l)
				{
					timeMS += adjustment;
					break;
				}

				// If we're going further back to get a valid time from the
				// graph, it will need adjusting
				adjustment += m_IntervalMS;
			}

			// -1 means we'll assume the time is the same as a valid time read
			// from another bar chart
			if (timeMS != -1l)
			{
				// If we've already collected data from this time then skip the
				// rest of this bar chart
				if (timeMS <= m_LastDataTime)
				{
					continue;
				}

				if (firstDataTime == 0l || timeMS < firstDataTime)
				{
					firstDataTime = timeMS;
				}

				if (timeMS > lastDataTime)
				{
					lastDataTime = timeMS;
				}
			}

			// Now get the specific sources and the value for our source
			Matcher sourceMatcher = m_SourceRegex.matcher(sources);
			Matcher valueMatcher = m_ValueRegex.matcher(barchart);
			while (sourceMatcher.find() == true &&
					valueMatcher.find() == true)
			{
				String source = sourceMatcher.group(1);
				if (source == null)
				{
					throw new IllegalArgumentException("Source regex " +
													m_SourceRegex.pattern() +
													" did not capture a group");
				}

				String valueStr = valueMatcher.group(1);
				if (valueStr == null)
				{
					throw new IllegalArgumentException("Value regex " +
													m_ValueRegex.pattern() +
													" did not capture a group");
				}

				if (!source.equals(m_GraphLabel))
				{
					// Every bar chart will have more graph labels than the one
					// we want, so don't spam the log here
					continue;
				}

				try
				{
					double value = Double.parseDouble(valueStr);

					// Finally, we have all the required information for one
					// point
					OLBMDataPoint point = new OLBMDataPoint(type, metric,
														source, timeMS, value);
					points.add(point);
				}
				catch (NumberFormatException e)
				{
					s_Logger.warn("Invalid value '" + valueStr +
								"' extracted from bar chart text by regex " +
								m_ValueRegex.pattern());
					s_Logger.debug("Bar chart text containing invalid value was " +
								barchart);
				}
			}
		}

		// Did we get at least 1 point with a valid time?
		if (lastDataTime > 0 && !points.isEmpty())
		{
			if (firstDataTime != lastDataTime)
			{
				// If this happens then the loop below that replaces "xx" times
				// is on thin ice
				s_Logger.warn("First data time (" + firstDataTime +
								") and last data time (" + lastDataTime +
								") are different for URL " + m_URL);
			}

			// Set the correct time on any points that originally had "xx" for
			// their time
			for (OLBMDataPoint point : points)
			{
				if (point.getTimeMS() == -1l)
				{
					if (point.getValue() != 0d)
					{
						// Based on what we've been told about the workings of
						// OLBM, we'd expect "xx" values to always be 0, so warn
						// if this isn't the case
						s_Logger.warn("Point with time \"xx\" but non-zero value : " +
										point);
					}
					point.setTimeMS(lastDataTime);
				}
			}

			// Pass the points (if any) to the plugin
			m_Plugin.addPoints(points);
			m_LastDataTime = lastDataTime;
		}
	}


	/**
	 * Convert a time string to an epoch time (in milliseconds).  An input
	 * string of "xx" is possible, and means no data has been counted so far.
	 * Assuming no data is counted subsequently, this can be interpreted as a
	 * zero value at the time of the corresponding lines on adjacent graphs.
	 * Otherwise, the time is assumed to be in the 24 hour clock in the current
	 * current time zone, and for the nearest date to today.
	 * @param timeStr A string representing a time, or "xx".
	 * @return An epoch time (in milliseconds), or -1 if the time string is
	 *         "xx", or -2 if an error occurs.
	 */
	long convertTime(String timeStr)
	{
		if (timeStr == null)
		{
			return -2l;
		}

		if (timeStr.equals("xx"))
		{
			return -1l;
		}

		String[] hourMin = timeStr.split(":");
		if (hourMin.length != 2)
		{
			s_Logger.error("Time string '" + timeStr +
							"' not in expected format HH:MM");
			return -2l;
		}

		Date now = new Date();
		Calendar calendar = Calendar.getInstance();
		try
		{
			calendar.setTime(now);
			calendar.set(Calendar.MINUTE, Integer.parseInt(hourMin[1]));
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);

			// The times in the HTML use the 12 hour clock, but with no
			// indication of AM or PM.  So, we assume the time is close to the
			// current time on the machine we're running on.
			calendar.set(Calendar.HOUR,
						Integer.parseInt(hourMin[0]) % TWELVE_HOUR_CLOCK_HOURS);
			if (calendar.getTimeInMillis() - SIX_HOUR_MS > now.getTime())
			{
				// Time is more than 6 hours in the future, so subtract 12
				// hours.  (This would be expected if the current time is 12am
				// and we've just read the points from 11:59pm.)
				calendar.add(Calendar.HOUR, -TWELVE_HOUR_CLOCK_HOURS);
			}
			else if (calendar.getTimeInMillis() + SIX_HOUR_MS < now.getTime())
			{
				// Time is more than 6 hours in the past, so add 12 hours.
				// (This would be expected if we've read points from the
				// afternoon and the current time is still on the same day.)
				calendar.add(Calendar.HOUR, TWELVE_HOUR_CLOCK_HOURS);
			}
		}
		catch (NumberFormatException e)
		{
			s_Logger.error("Time string '" + timeStr +
							"' not in expected format HH:MM");
			return -2l;
		}

		return calendar.getTimeInMillis();
	}

}
