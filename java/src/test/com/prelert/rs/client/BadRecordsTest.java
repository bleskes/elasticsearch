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

import java.io.Closeable;
import java.io.IOException;
import java.io.PipedInputStream;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.prelert.job.JobConfiguration;
import com.prelert.rs.data.ApiError;
import com.prelert.rs.data.ErrorCode;


/**
 * Upload data that has a high proportion of records that have
 * unparseable dates or are not in ascending time order. 
 * Check the appropriate error code is returned.  
 */
public class BadRecordsTest implements Closeable
{
	static final private Logger s_Logger = Logger.getLogger(BadRecordsTest.class);
	
	/**
	 * The default base Url used in the test
	 */
	static final public String API_BASE_URL = "http://localhost:8080/engine/v1";
	
	private EngineApiClient m_EngineApiClient;
	
	private String m_BaseUrl;
	
	/**
	 * Creates a new http client call {@linkplain #close()} once finished
	 */
	public BadRecordsTest(String baseUrl)
	{
		m_BaseUrl = baseUrl;
		m_EngineApiClient = new EngineApiClient();
	}
	
	@Override
	public void close() throws IOException 
	{
		m_EngineApiClient.close();
	}
	
	
	/**
	 * Generate records with unparsable dates the streaming client
	 * should return {@link ErrorCode#TOO_MANY_BAD_DATES} error.
	 * 
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public void testUnparseableDates() 
	throws ClientProtocolException, IOException
	{
		PipedInputStream inputStream = new PipedInputStream();

		BadRecordProducer producer = new BadRecordProducer(
				BadRecordProducer.TestType.BAD_TIMESTAMP, inputStream);
		Thread producerThread = new Thread(producer, "Producer-Thread");

		JobConfiguration jc = producer.getJobConfiguration();
		jc.setDescription("Bad dates test");
		String jobId = m_EngineApiClient.createJob(m_BaseUrl, jc);
		
		producerThread.start();

		boolean success = m_EngineApiClient.streamingUpload(m_BaseUrl, jobId, inputStream, false);
		test(success == false);
		ApiError error = m_EngineApiClient.getLastError();
		test(error != null);
		test(error.getErrorCode() == ErrorCode.TOO_MANY_BAD_DATES);
		s_Logger.info(error);

		
		m_EngineApiClient.closeJob(m_BaseUrl, jobId);
		
		try 
		{
			producerThread.join();
		}
		catch (InterruptedException e) 
		{
			s_Logger.error(e);
		}
	}
	
	
	/**
	 * Generate records with that are not in ascending time order.
	 * 
	 * The client should return with 
	 * {@link ErrorCode#TOO_MANY_OUT_OF_ORDER_RECORDS} error.
	 * 
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public void testOutOfOrderDates() 
	throws ClientProtocolException, IOException
	{
		PipedInputStream inputStream = new PipedInputStream();

		BadRecordProducer producer = new BadRecordProducer(
				BadRecordProducer.TestType.OUT_OF_ORDER_RECORDS, inputStream);
		Thread producerThread = new Thread(producer, "Producer-Thread");

		JobConfiguration jc = producer.getJobConfiguration();
		jc.setDescription("Out of order records test");
		String jobId = m_EngineApiClient.createJob(m_BaseUrl, jc);
		
		producerThread.start();

		boolean success = m_EngineApiClient.streamingUpload(m_BaseUrl, jobId, inputStream, false);
		test(success == false);
		ApiError error = m_EngineApiClient.getLastError();
		test(error != null);
		test(error.getErrorCode() == ErrorCode.TOO_MANY_OUT_OF_ORDER_RECORDS);
		s_Logger.info(error);

		
		m_EngineApiClient.closeJob(m_BaseUrl, jobId);
		
		try 
		{
			producerThread.join();
		}
		catch (InterruptedException e) 
		{
			s_Logger.error(e);
		}
	}
	
	
	/**
	 * Throws an exception if <code>condition</code> is false.
	 * 
	 * @param condition
	 * @throws IllegalStateException
	 */
	public static void test(boolean condition) 
	throws IllegalStateException
	{
		if (condition == false)
		{
			throw new IllegalStateException();
		}
	}
	
	
	/**
	 * The program takes one argument which is the base Url of the RESTful API.
	 * If no arguments are given then {@value #API_BASE_URL} is used. 
	 * 
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) 
	throws IOException, InterruptedException
	{
		// configure log4j
		ConsoleAppender console = new ConsoleAppender(); 		
		console.setLayout(new PatternLayout("%d [%p|%c|%C{1}] %m%n")); 
		console.setThreshold(Level.INFO);
		console.activateOptions();
		Logger.getRootLogger().addAppender(console);
				
		String baseUrl = API_BASE_URL;
		if (args.length > 0)
		{
			baseUrl = args[0];
		}

		
		BadRecordsTest test = new BadRecordsTest(baseUrl);
		test.testUnparseableDates();
		test.testOutOfOrderDates();
		
		test.close();	
		
		s_Logger.info("All tests passed Ok");
	}
}
