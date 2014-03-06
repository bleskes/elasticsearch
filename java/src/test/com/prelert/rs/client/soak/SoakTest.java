/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Inc 2006-2014     *
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

package com.prelert.rs.client.soak;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;

import com.prelert.rs.client.AutodetectRsClient;

/**
 * Creates a number of producer threads to write data to the Engine REST API
 */
public class SoakTest 
{
	static final private Logger s_Logger = Logger.getLogger(SoakTest.class);
	
	static final public String DEFAULT_CHARSET = "UTF-8";
	
	/**
	 * The default base Url used in the test
	 */
	static final public String JOBS_URL = "http://localhost:8080/engine/beta/";
	
	
	/**
	 * Creates a job then opens a connection to the streaming data
	 * endpoint. The producer thread then writes to that open connection
	 */
	static private class SoakTestRunner implements Runnable
	{
		private AutodetectRsClient m_ApiClient;
		private String m_BaseUrl;
		private String m_CreateJobPayload;
		
		private long m_NumTimeSeries;
		private long m_NumIterations;
		private long m_PointInterval;
	
	
		public SoakTestRunner(String baseUrl, String createJobPayload)
		{
			m_ApiClient = new AutodetectRsClient();
			m_BaseUrl = baseUrl;
			m_CreateJobPayload = createJobPayload;
		}
		
		
		public void setNumTimeSeries(long value)
		{
			m_NumTimeSeries = value;
		}
		
		public void setNumIterations(long value)
		{
			m_NumIterations = value;
		}
		
		public void setPointInterval(long value)
		{
			m_PointInterval = value;
		}
		

		@Override
		public void run()
		{		
			String jobUrl;
			try 
			{
				jobUrl = m_ApiClient.createJob(m_BaseUrl +"/jobs", m_CreateJobPayload);
			}
			catch (ClientProtocolException e) 
			{
				s_Logger.error(e);
				s_Logger.error("Exiting thread");
				return;
			}
			catch (IOException e)
			{
				s_Logger.error(e);
				s_Logger.error("Exiting thread");
				return;				
			}
			
			
			PipedInputStream inputStream = new PipedInputStream();
			SoakTestProducer producer = new SoakTestProducer(inputStream);
			
			producer.setNumberOfTimeSeriesToSimulate(m_NumTimeSeries);
			producer.setNumberIterations(m_NumIterations);
			producer.setTimeSeriesPointIntervalSeconds(m_PointInterval);
			
			Thread producerThread = new Thread(producer, "Producer-Thread");
			producerThread.start();
			
			try {
				m_ApiClient.streamingUpload(jobUrl, inputStream, false);
			} catch (IOException e) {
				s_Logger.error("Error streaming data", e);
			}
			
	
			try
			{
				producerThread.join();
			}
			catch (InterruptedException e) 
			{
				s_Logger.error("Interupted joining producer thread", e);
			}
		}
	}
	
	/**
	 * Producer thread.
	 * Writes to a PipedOuptstream connected to the PipedInputStream 
	 * passed in the constructor
	 */
	static private class SoakTestProducer implements Runnable
	{		
		public static final long DEFAULT_NUMBER_TIME_SERIES = 200;
		public static final long DEFAULT_TIME_SERIES_POINT_INTERVAL_SECS = 15;
		//public static final long DEFAULT_NUMBER_ITERATIONS = 1;
		public static final long DEFAULT_NUMBER_ITERATIONS = 60 * (60 / DEFAULT_TIME_SERIES_POINT_INTERVAL_SECS);
		
		private long m_NumberTimeSeries = DEFAULT_NUMBER_TIME_SERIES;
		private long m_PointIntervalMs = DEFAULT_TIME_SERIES_POINT_INTERVAL_SECS * 1000;
		private long m_NumberIterations = DEFAULT_NUMBER_ITERATIONS;
		
		private PipedOutputStream m_OutputStream;
		
		public SoakTestProducer(PipedInputStream sink)
		{
			try
			{
				m_OutputStream = new PipedOutputStream(sink);
			}
			catch (IOException e)
			{
				s_Logger.error(e);
				m_OutputStream = null;
			}
		}
		
		public void setNumberOfTimeSeriesToSimulate(long value)
		{
			m_NumberTimeSeries = value;
		}	
		public void setTimeSeriesPointIntervalSeconds(long value)
		{
			m_PointIntervalMs = value * 1000;
		}
		/**
		 * Value of 0 means there is no limit
		 * @param value
		 */
		public void setNumberIterations(long value)
		{
			m_NumberIterations = value;
		}
		
		
		@Override
		public void run()
		{					
			// try to do this in real time  
			try
			{
				writeHeader();

				int iterationCount = 0;
				while (++iterationCount <= m_NumberIterations || m_NumberIterations == 0)
				{
					long iterStart = System.currentTimeMillis();
					long epoch = iterStart / 1000;


					long timeSeriesCount = 0;
					while (++timeSeriesCount <= m_NumberTimeSeries)
					{
						writeTimeSeriesRow(timeSeriesCount, epoch);					
					}


					long iterEnd = System.currentTimeMillis();
					s_Logger.info(String.format("%d metrics uploaded in  %d ms", 
							m_NumberTimeSeries, iterEnd - iterStart));
					
					
					if (iterationCount >= m_NumberIterations && m_NumberIterations != 0)
					{
						// don't bother sleeping in the last loop
						break;
					}

					long sleepTime = (iterStart + m_PointIntervalMs) - iterEnd;
					s_Logger.info(String.format("Sleeping for %d ms", sleepTime));
					if (sleepTime > 0)
					{
						try
						{
							Thread.sleep(sleepTime);
						}
						catch (InterruptedException e) 
						{
							s_Logger.info("Producer interrupted while sleeping");
							break;
						}
					}

				}
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
			String header = "_time,metric_field,metric_value";
			try 
			{
				m_OutputStream.write(header.getBytes(Charset.forName(DEFAULT_CHARSET)));
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
				m_OutputStream.write(row.getBytes(Charset.forName(DEFAULT_CHARSET)));
			} 
			catch (IOException e) 
			{
				s_Logger.error("Error writing csv row", e);
			}			
		}
		
	}
	
	
	public static void main(String[] args) 
	throws FileNotFoundException, IOException
	{
		final String SIMPLE_JOB_CONFIG = "{\"analysisConfig\" : {"
				+ "\"bucketSpan\":600,"  
				+ "\"detectors\" :" 
				+ "[{\"fieldName\":\"metric_field\",\"byFieldName\":\"metric_value\"}] },"
				+ "\"dataDescription\":{\"fieldDelimiter\":\",\"} }}";
		
		if (args.length == 0)
		{
			s_Logger.error("This program has one argument the path to the properties file " 
					+ "containing the settings");
			return;
		}
		
		String filename = args[0];
		Properties props = new Properties();
		props.load(new FileReader(filename));
		
		String serviceUrl = props.getProperty("service_url");
		int numProducers = Integer.parseInt(props.getProperty("num_producers"));
		long numTimeSeries = Long.parseLong(props.getProperty("num_time_series"));
		long pointInterval = Long.parseLong(props.getProperty("time_series_point_interval_secs"));
		long numInteration = Long.parseLong(props.getProperty("num_iterations"));
		
		List<Thread> threads = new ArrayList<>();
		for (int i=0; i<numProducers; i++)
		{
			SoakTestRunner test = new SoakTestRunner(serviceUrl, SIMPLE_JOB_CONFIG);
			test.setNumIterations(numInteration);
			test.setNumTimeSeries(numTimeSeries);
			test.setPointInterval(pointInterval);
			
			Thread testThread = new Thread(test);
			testThread.start();
			threads.add(testThread);
		}
		
		
		
		for (Thread th : threads)
		{
			try
			{
				th.join();
			}
			catch (InterruptedException e) 
			{
				s_Logger.error("Interupted joining test thread", e);
			}
		}
	}

}
