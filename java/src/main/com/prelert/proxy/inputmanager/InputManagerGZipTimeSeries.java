/************************************************************
 *                                                          *
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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

package com.prelert.proxy.inputmanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import com.prelert.data.TimeSeriesData;
import com.prelert.proxy.inputmanager.dao.InputManagerDAO;
import com.prelert.proxy.inputmanager.querymonitor.QueryTookTooLongException;


/**
 * A Time series input manager which gzips the time series points 
 * data (in prelert Xml format) and writes them out to a file 
 * with the named by the query start and end times. 
 * i.e. start--end.xml.gz 
 */
public class InputManagerGZipTimeSeries extends InputManagerExternalTimeSeries
{
	private String m_OutputDirectory;
	
	public InputManagerGZipTimeSeries(InputManagerDAO inputManagerDAO)
	{
		super(inputManagerDAO);
		
		m_OutputDirectory = "";
	}
	
	public String getOutputDirectory()
	{
		return m_OutputDirectory;
	}
	
	/**
	 * Set the 
	 * @param directory
	 */
	public void setOutputDirectory(String directory)
	{
		m_OutputDirectory = directory;
	}

	
	/**
	 * @param updateDisplayColumns Unused.
	 */
	@Override
	protected boolean collectAndSendData(Date startTime, Date endTime, boolean updateDisplayColumns)
	{
		Collection<TimeSeriesData> timeSeriesData;
		
		try
		{
			// Get the internal time series data points
			timeSeriesData = 
				m_ExternalPlugin.getAllDataPointsForTimeSpan(startTime, endTime, 
						m_ExternalPlugin.getUsualPointIntervalSecs());

			gzipAndWriteXml(timeSeriesData, createFileName(startTime, endTime));

			return timeSeriesData.size() > 0;
		}
		catch (QueryTookTooLongException e)
		{
			s_Logger.warn(e);
		}
		
		return false;
	}
	
	
	/**
	 * Converts the <code>timeSeriesData</code> to Xml then gzips
	 * it and streams it to a file on the fly.
	 * 
	 * @param timeSeriesData
	 * @param filename
	 */
	private void gzipAndWriteXml(Collection<TimeSeriesData> timeSeriesData, String filename) 
	{
		File file = new File(m_OutputDirectory, filename);
		
		FileOutputStream fos;
		try 
		{
			file.createNewFile();
			fos = new FileOutputStream(file);
		}
		catch (IOException e) 
		{
			s_Logger.fatal("Could not open or create file '" + file.getAbsolutePath() + "'");
			return;
		}
		
		
		GZIPOutputStream gzip;
		try 
		{
			gzip = new GZIPOutputStream(fos);
		}
		catch (IOException e) 
		{
			s_Logger.fatal(e);
			return;
		}
		
		
		OutputStreamWriter writer = new OutputStreamWriter(gzip);
		
		try 
		{
			writer.write("<points>");
			
			for (TimeSeriesData data : timeSeriesData)
			{
				writer.write(data.toXmlStringInternal(false));
			}
			
			writer.write("</points>");

			writer.flush();
			gzip.flush();

			writer.close();
			gzip.close();
		} 
		catch (IOException e) 
		{
			s_Logger.error(e);
			return;
		}
		
		
	}

	
	/**
	 * Create a file name from the start and end dates ie.
	 * points-yyyy-MM-DD-hh-mm--yyyy-MM-DD-hh-mm.xml.gz
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	private String createFileName(Date start, Date end)
	{
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
		
		StringBuilder builder = new StringBuilder("points-");
		builder.append(dateFormat.format(start));
		builder.append("--");
		builder.append(dateFormat.format(end));
		builder.append(".xml.gz");
		
		return builder.toString();
	}
}
