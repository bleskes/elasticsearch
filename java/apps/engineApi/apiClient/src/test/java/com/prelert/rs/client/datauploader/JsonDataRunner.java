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
 ************************************************************/

package com.prelert.rs.client.datauploader;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;

import com.prelert.rs.client.EngineApiClient;
import com.prelert.rs.data.ApiError;



/**
 * Creates a simple job with a detector for for 'metric(metric_value) by metric_field'
 * then generates random JSON data and uploads it to the job.<br/>
 */
public class JsonDataRunner implements Runnable
{
	private static final Logger s_Logger = Logger.getLogger(JsonDataRunner.class);
	
	/**
	 * Job configuration as a format string.
	 * The bucketSpan value should be replaced
	 */
	public static final String JOB_CONFIG_TEMPLATE = "{"
			+ "\"analysisConfig\" : {"
				+ "\"bucketSpan\":%d,"  
				+ "\"detectors\" :[{\"function\":\"metric\",\"fieldName\":\"metric_value\",\"byFieldName\":\"metric_field\"}] "
			+ "},"
			+ "\"dataDescription\":{"
				+ "\"format\":\"JSON\", \"timeField\":\"time\", \"timeFormat\":\"yyyy-MM-dd'T'HH:mm:ssX\"} "
			+ "}"
			+ "}";
	
	/**
	 * Job configuration with an ID field as a format string.
	 * id and bucketSpan should be replaced
	 */
	public static final String JOB_CONFIG_WITH_ID_TEMPLATE = "{"
		    + "\"id\":\"%s\","
			+ "\"analysisConfig\" : {"
				+ "\"bucketSpan\":%d,"  
				+ "\"detectors\" :[{\"function\":\"metric\",\"fieldName\":\"metric_value\",\"byFieldName\":\"metric_field\"}] "
			+ "},"
			+ "\"dataDescription\":{"
				+ "\"format\":\"JSON\", \"timeField\":\"time\", \"timeFormat\":\"yyyy-MM-dd'T'HH:mm:ssX\"} "
			+ "}"
			+ "}";
	
	
	public static final String JSON_DOC_TEMPLATE = "{"
				+ "\"time\":\"%s\", \"metric_field\":\"%s\", \"metric_value\":%d"
			+ "}";
	
	public static final long DEFAULT_NUMBER_TIME_SERIES = 100000;
	public static final long DEFAULT_TIME_SERIES_POINT_INTERVAL_SECS = 15;
	public static final long DEFAULT_NUMBER_ITERATIONS = 100;
	public static final long DEFAULT_BUCKETSPAN_SECS = 300;
	

	private EngineApiClient m_ApiClient;
	private String m_BaseUrl;
	
	private String m_JobId;
	
	// members are final as read by multiple threads
	final private long m_NumTimeSeries;
	final private long m_NumIterations;
	final private long m_PointIntervalSecs;
	final private long m_BucketSpan;
	
	volatile private boolean m_Stop;


	/**
	 * Create the data generator with default settings.
	 * 
	 * @param baseUrl REST API url e.g. <code>http://localhost:8080/engine/version/</code>
	 */
	public JsonDataRunner(String baseUrl)
	{
		this(baseUrl, DEFAULT_NUMBER_TIME_SERIES, DEFAULT_NUMBER_ITERATIONS,
				DEFAULT_TIME_SERIES_POINT_INTERVAL_SECS, DEFAULT_BUCKETSPAN_SECS);
	}


	/**
	 * 
	 * @param baseUrl REST API url e.g. <code>http://localhost:8080/engine/version/</code>
	 * @param numberTimeSeries Number of time series to create
	 * @param numIterations A value <= 0 means there is no limit and the thread 
	 * will run indefinitely 
	 * @param pointIntervalSecs The time between writing each new data point 
	 * for each time series. 
	 * @param bucketSpanSecs The job bucketSpan
	 */
	public JsonDataRunner(String baseUrl, long numberTimeSeries, long numIterations,
			long pointIntervalSecs, long bucketSpanSecs)
	{
		m_NumTimeSeries = numberTimeSeries;
		m_NumIterations = numIterations;
		m_PointIntervalSecs = pointIntervalSecs;
		m_BucketSpan = bucketSpanSecs;
		
		m_ApiClient = new EngineApiClient();
		m_BaseUrl = baseUrl;
		
		m_Stop = false;
	}

	
	public String createJob() 
	throws ClientProtocolException, IOException
	{
		String jobConfig = String.format(JOB_CONFIG_TEMPLATE, m_BucketSpan);
		m_JobId = m_ApiClient.createJob(m_BaseUrl, jobConfig);
		
		return m_JobId;
	}
	
	
	public String createJob(String jobName) 
	throws ClientProtocolException, IOException
	{
		m_ApiClient.deleteJob(m_BaseUrl, jobName);
		
		String jobConfig = String.format(JOB_CONFIG_WITH_ID_TEMPLATE, jobName, m_BucketSpan);
		m_JobId = m_ApiClient.createJob(m_BaseUrl, jobConfig);
		
		return m_JobId;
	}
	
	/**
	 * Stop the thread running (eventually)
	 */
	public void cancel()
	{
		m_Stop = true;
	}

	@Override
	public void run()
	{		
		if (m_JobId == null || m_JobId.isEmpty())
		{
			String msg = "Job must be created before the thread is started " 
					+ "call createJob() first";
			s_Logger.error(msg);
			throw new IllegalStateException(msg);
		}



		PipedInputStream inputStream = new PipedInputStream();
		SoakTestProducer producer = new SoakTestProducer(inputStream);

		Thread producerThread = new Thread(producer, "Producer-Thread");
		producerThread.start();

		try 
		{
			boolean ok = m_ApiClient.streamingUpload(m_BaseUrl, m_JobId, inputStream, false);
			if (!ok)
			{
				ApiError error = m_ApiClient.getLastError();
				s_Logger.error(error.toJson());
			}
		}
		catch (IOException e) 
		{
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

	/**
	 * Producer thread.
	 * Writes to a PipedOuptstream connected to the PipedInputStream 
	 * passed in the constructor
	 */
	private class SoakTestProducer implements Runnable
	{		
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

			
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
			try
			{
				int iterationCount = 0;
				while (++iterationCount <= m_NumIterations || m_NumIterations <= 0)
				{
					if (m_Stop)
					{
						break;
					}
					
					long iterStartMs = System.currentTimeMillis();

					Date now = new Date();
					String dateStr = format.format(now);


					long timeSeriesCount = 0;
					while (++timeSeriesCount <= m_NumTimeSeries)
					{
						//writeTimeSeriesJsonDoc(timeSeriesCount, epoch);		
						writeTimeSeriesJsonDoc(timeSeriesCount, dateStr);	
					}


					long iterEndMs = System.currentTimeMillis();
					s_Logger.info(String.format("%d metrics uploaded in  %d ms", 
							m_NumTimeSeries, iterEndMs - iterStartMs));


					if (iterationCount >= m_NumIterations && m_NumIterations != 0)
					{
						// don't bother sleeping in the last loop
						break;
					}

					long sleepTime = (iterStartMs + (m_PointIntervalSecs * 1000)) - iterEndMs;
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
					
					
					synchronized (JsonDataRunner.this) 
					{
						JsonDataRunner.this.notify();
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
		
		

		/**
		 * Generate a random value for the time series using ThreadLocalRandom
		 * @param timeSeriesId
		 * @param epoch
		 */
		private void writeTimeSeriesJsonDoc(long timeSeriesId, long epoch)
		{
			String timeSeries = "metric" + timeSeriesId;
			int value = ThreadLocalRandom.current().nextInt(512);

			String row = String.format(JSON_DOC_TEMPLATE, epoch, timeSeries, value);
			try 
			{
				m_OutputStream.write(row.getBytes(StandardCharsets.UTF_8));
				//m_OutputStream.write(10); // newline char
			} 
			catch (IOException e) 
			{
				s_Logger.error("Error writing JSON document", e);
			}			
		}
		
		
		/**
		 * Generate a random value for the time series using ThreadLocalRandom
		 * @param timeSeriesId
		 * @param date
		 * 
		 */
		private void writeTimeSeriesJsonDoc(long timeSeriesId, String date)
		{
			String timeSeries = "metric" + timeSeriesId;
			int value = ThreadLocalRandom.current().nextInt(512);

			String row = String.format(JSON_DOC_TEMPLATE, date, timeSeries, value);
			try 
			{
				m_OutputStream.write(row.getBytes(StandardCharsets.UTF_8));
				//m_OutputStream.write(10); // newline char
			} 
			catch (IOException e) 
			{
				s_Logger.error("Error writing JSON document", e);
			}			
		}

	}
}

