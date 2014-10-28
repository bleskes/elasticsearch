/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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

package com.prelert.api.test.consumer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.consumer.ODataConsumers;
import org.odata4j.core.OEntity;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.format.FormatType;



/**
 * Test the ODataProducers ability to handle concurrent uploads.
 * <p/>
 * The test runs {@link #NUM_UPLOADERS} threads simultaneously each
 * of which uploads {@link #NUM_METRICS} metrics {@link #NUM_ITERATIONS} 
 * times without pausing.
 * <p/>
 * The test cannot be comprehensive as thread scheduling cannot be 
 * predicted but on a multi-core machine some simultaneous processing
 * should occur.   
 * <p/> 
 * This test runs very quickly uploading large amounts of junk data
 * so it's advisable to stop the Prelert backend processes 
 * (ts_feature_detector) to prevent data being written to the database
 * or engine. Run the Prelert server program on port 49996 so
 * the upload web service has something to connect to. The server
 * program simply redirects its output to std out.
 * 
 * <p/>
 * This test requires the <a href=https://code.google.com/p/odata4j/>Odata4J</a> and 
 * <a href=https://code.google.com/p/json-simple/>Json-Simple</a> libraries (Apache Licence 2.0).
 */
public class ConcurrentUploadTest 
{
    /**
     * The Prelert namespace
     */
	public final static String NAMESPACE = "Prelert";

	/**
	 * The MetricFeed Entity set name
	 */
	public final static String METRIC_FEEDS_SET = "MetricFeeds";
	
	
	/**
	 * Number of metrics to upload in each iteration.
	 */
	public static final int NUM_METRICS = 1000;
	
	
	/**
	 * Number of times the data points are sent by each
	 * uploader thread.
	 */
	public static final int NUM_ITERATIONS = 100;
	
	
	/**
	 * The number of uploader threads to create.
	 */
	public static final int NUM_UPLOADERS = 4;
	
	
	/**
	 * The character set the Json data is encoded in.
	 */
	public static final String JSON_DATA_CHARSET = "UTF-8";
	
	
	volatile private boolean m_TestFailed = false;  
	
	/**
     * Main entry point, runs the upload tests.
     * 
     * @param args 1 optional argument is expected which is the service URI.
     * If not set the default <code>http://localhost:8080/prelertApi/prelert.svc</code> is used.
	 */
	public static void main(String[] args)
	{
		
		String serviceUri = "http://localhost:8080/prelertApi/prelert.svc";
		if (args.length > 0)
		{
			serviceUri = args[0];
		}
		
		ConcurrentUploadTest test = new ConcurrentUploadTest();
		test.runTests(serviceUri);
		
	}
	
	
	/**
	 * Run the concurrent upload test.
	 * <br/>
	 * Create {@link #NUM_UPLOADERS} ODataConsumers and threads then start those
	 * threads running uploading {@link #NUM_METRICS} for {@link #NUM_ITERATIONS}.
	 * 
	 * @param serviceUri
	 * @return True if the test was successful 
	 */
	public boolean runTests(String serviceUri)
	{
        System.out.println("Running the Concurrent Metric Upload test.");
        System.out.println(String.format("Creating %d uploaders using host %s", 
        		NUM_UPLOADERS, serviceUri));

        List<MetricUploader> uploaders = new ArrayList<MetricUploader>();

        for (int i=0; i<NUM_UPLOADERS; i++)
		{
        	ODataConsumer oDataConsumer = ODataConsumers.newBuilder(serviceUri).setFormatType(FormatType.JSON).build();
        	uploaders.add(new MetricUploader(oDataConsumer));
		}

        
        System.out.println(String.format("Starting %d threads uploading %d metrics %d times each", 
        		NUM_UPLOADERS, NUM_METRICS, NUM_ITERATIONS));
        
        List<Thread> threads = new ArrayList<Thread>();
        for (int i=0; i<NUM_UPLOADERS; i++)
        {
        	Thread thread = new Thread(uploaders.get(i), "Uploader-" + i);
        	threads.add(thread);
        	thread.start();
        }
        
        for (Thread thrd : threads)
        {
        	try 
        	{
				thrd.join();
			}
        	catch (InterruptedException e) 
			{
				System.out.println(e);
			}
        } 
		
		
        if (m_TestFailed)
        {
        	System.out.println("TEST FAILED");
        }
        else
        {
        	System.out.println("TEST SUCCESSFUL");
        }
        
        return !m_TestFailed;
	}
	
	
	/**
	 * Runnable object to upload the MetricFeed to the web service. 
	 */
	public class MetricUploader implements Runnable 
	{
		private ODataConsumer m_OdataConsumer;
		private List<OProperty<?>> m_MetricProps;
		@SuppressWarnings("unused")
		private String m_ThreadName;
		
		/**
		 * Creates the metric feed data that will be uploaded.
		 * 
		 * @param oDataConsumer The OData consumer for creating 
		 * the <code>MetricFeed</code> entity. 
		 */
		public MetricUploader(ODataConsumer oDataConsumer)
		{
			m_OdataConsumer = oDataConsumer;
			
			m_MetricProps = createMetricFeed();
		}
		
		
		/**
		 * Runs in a loop {@link ConcurrentUploadTest#NUM_ITERATIONS} 
		 * times uploading the data.
		 */
		@Override
		public void run()
		{
			m_ThreadName = Thread.currentThread().getName();
			
			int uploadCount = 0;
			while (uploadCount++ < NUM_ITERATIONS)
			{
				upload(m_MetricProps);
			}
		}
		
		
		/**
		 * Create the MetricFeed OEntity properties.
		 * <br/>
		 * The properties will contain 
		 * {@link ConcurrentUploadTest#NUM_ITERATIONS} metrics. 
		 * 
		 * @return  List of the OEntity properties. 
		 */
		@SuppressWarnings("unchecked")
		public List<OProperty<?>> createMetricFeed() 
		{
			JSONArray jsonArrayIn = new JSONArray();
			
			for (int i=0; i<NUM_METRICS; i++)
			{
				
				String path = "Domains|myDomain|laivi02-745|Tomcat|Tomcat|Frontends|Apps|OrderEngin|URLs|Default|Called Backends|System localhost on port 6543:metric name0";
				Integer value = new Integer(101);

				JSONObject obj = new JSONObject();
				obj.put("m", path);
				obj.put("d", value);
				jsonArrayIn.add(obj);	
			}
			byte [] data = gzipCompress(jsonArrayIn.toJSONString().getBytes(Charset.forName(JSON_DATA_CHARSET)));
			
			int metricCount = NUM_METRICS;
			
			int id = 1;
			List<OProperty<?>> dataProps = new ArrayList<OProperty<?>>();
			dataProps.add(OProperties.int32("Id", id));
			dataProps.add(OProperties.string("Source", "CA-APM"));
			dataProps.add(OProperties.datetimeOffset("CollectionTime", new DateTime()));
			dataProps.add(OProperties.int32("Count", metricCount));
			dataProps.add(OProperties.string("Compression", "gzip"));
			dataProps.add(OProperties.binary("Data", data));

			return dataProps;
		}
		
		
		/**
		 * Send the <code>MetricFeed OEntity</code> to the web service.
		 * Tests the return value and logs the error if the upload 
		 * (OData create entity) failed. 
		 * 
		 * @param properties The MetricFeed OEntity properties.
		 */
		public void upload(List<OProperty<?>> properties) 
		{
			OEntity createdEnt = m_OdataConsumer.createEntity(METRIC_FEEDS_SET).properties(properties).execute();
			
			testCondition(createdEnt != null, "Failed to create entity");

			OProperty<?> idProp = createdEnt.getProperty("Id");
			testCondition(idProp != null, "Create should return an entity with the 'Id' property.");
			
			List<OProperty<?>> props = createdEnt.getProperties();
			testCondition(props.size() == 1, "Create should return an entity with 1 property.");
			
			//System.out.println(String.format("Thread '%s' uploaded %d metrics", m_ThreadName, NUM_METRICS));
		}
		
		
	    /**
	     * Gzip compress <code>data</code>.
	     * 
	     * @param data
	     * @return The compressed data.
	     */
	    private byte[] gzipCompress(byte[] data)
	    {
			ByteArrayOutputStream oBuf = new ByteArrayOutputStream();
			GZIPOutputStream gBuf;
			try 
			{
				gBuf = new GZIPOutputStream(oBuf);
				gBuf.write(data);
				gBuf.close();
				oBuf.close();
				
				return oBuf.toByteArray();
			}
			catch (IOException e) 
			{
				System.out.println("ERROR: gzip compression failed!");
			}

			return null;
	    }
		
	}
	
	
    /**
     * If <code>condition</code> is false then print the message to <code>System.out</code> 
     * and log the failure.
     * 
     * @param condition if false then log <code>failureMessage</code>
     * and exit.
     * @param failureMessage
     */
    private void testCondition(boolean condition, String failureMessage)
    {
        if (condition == false)
        {
            System.out.println("ERROR: " + failureMessage);
            System.out.println("TEST FAILED");
            m_TestFailed = true;
        }
    }
	
}
