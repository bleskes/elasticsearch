/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.rs.client;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.log4j.Logger;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;

public class BadRecordProducer implements Runnable
{	
	static private final Logger s_Logger = Logger.getLogger(BadRecordProducer.class);
	
	static public final String HEADER = "time,metric,value";
	
	private PipedOutputStream m_OutputStream;
	
	
	private long m_NumIterations;
	
	public BadRecordProducer(PipedInputStream sink) 
	throws IOException
	{
		m_NumIterations = 100000;
		m_OutputStream = new PipedOutputStream(sink);
	}
	
	
	/**
	 * Create a new job configuration each call rather than
	 * a member that could be mutated.
	 * @return
	 */
	public JobConfiguration getJobConfiguration()
	{
		Detector d = new Detector();
		d.setFieldName("metric");
		d.setByFieldName("value");
		
		AnalysisConfig ac = new AnalysisConfig();
		ac.setDetectors(Arrays.asList(d));
		
		DataDescription dd = new DataDescription();
		dd.setFieldDelimiter(',');
		dd.setTimeField("time");

		JobConfiguration jc = new JobConfiguration(ac);
		jc.setDataDescription(dd);
		
		return jc;
	}
	
	public void setNumIterations(long numIter)
	{
		m_NumIterations = numIter;
	}
	
	@Override
	public void run()
	{					
		// HACK wait for the parent thread to open the connection 
		// before writing
		try 
		{
			Thread.sleep(1000);
		}
		catch (InterruptedException e1) 
		{
			s_Logger.error("Producer interruputed pausing before write start");
		}

		try
		{
			writeHeader();
			long epoch = new Date().getTime();

			int iterationCount = 0;
			while (++iterationCount <= m_NumIterations)
			{
				writeTimeSeriesRow(1, epoch);
				writeTimeSeriesBadTimestamp(1);

				synchronized (this) 
				{
					this.notify();
				}
				
				epoch++;
			}
			
			System.out.println("done " + m_NumIterations );
			
			Thread.sleep(5000);
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally 
		{				
			try 
			{
				m_OutputStream.close();
			}
			catch (IOException e) {
				s_Logger.error("Error closing pipedoutputstream", e);
			}
		}

	}


	private void writeHeader()
	{
		try 
		{
			m_OutputStream.write(HEADER.getBytes(StandardCharsets.UTF_8));
			m_OutputStream.write(10); // newline char
		} 
		catch (IOException e) 
		{
			s_Logger.error("Error writing csv header", e);
		}
	}

	/**
	 * Generate a random value for the time series using ThreadLocalRandom
	 * @param timeSeriesId
	 * @param epoch
	 */
	private void writeTimeSeriesRow(long timeSeriesId, long epoch)
	{
		String timeSeries = "metric" + timeSeriesId;
		int value = ThreadLocalRandom.current().nextInt(512);

		String row = String.format("%d,%s,%d", epoch, timeSeries, value);
		try 
		{
			m_OutputStream.write(row.getBytes(StandardCharsets.UTF_8));
			m_OutputStream.write(10); // newline char
		} 
		catch (IOException e) 
		{
			s_Logger.error("Error writing csv row", e);
		}			
	}
	
	/**
	 * Write a time series record with an unreadable timestamp
	 * 
	 * @param timeSeriesId
	 */
	private void writeTimeSeriesBadTimestamp(long timeSeriesId)
	{
		String timeSeries = "metric" + timeSeriesId;
		int value = ThreadLocalRandom.current().nextInt(512);

		String row = String.format("%s,%s,%d", "", timeSeries, value);
		try 
		{
			m_OutputStream.write(row.getBytes(StandardCharsets.UTF_8));
			m_OutputStream.write(10); // newline char
		} 
		catch (IOException e) 
		{
			s_Logger.error("Error writing csv row", e);
		}	
	}
}