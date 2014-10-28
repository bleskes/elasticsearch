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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.zip.GZIPOutputStream;

import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.consumer.ODataConsumers;
import org.odata4j.core.OEntities;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.format.FormatType;


/**
 * Test the throughput of the Prelert Metric Upload API. 
 * <p/>
 * This class creates 2 threads (producer & consumer), one to create the
 * metric data and the second to send it to the web service. Synchronisation 
 * is done through a {@link SynchronousQueue} object. The producer thread will
 * create a fixed number of points for a set number of iterations, handing off 
 * a new batch of points to the consumer thread once created and sleeping 
 * between iterations so that each iteration occurs at a fixed interval. 
 * The number of metrics and the frequency at which they are sent are
 * controlled by the static constants defined in the class.
 * <p/>
 * The following requirements are tested:
 * <table border=1>
 * <tr><th>Requirement</th><th>Test Case</th></tr>
 * <tr><td>Support an efficient protocol to be delivered over a RESTful interface</td>
 * 		<td></td></tr>
 * <tr><td>Report metrics into system via API pushed from APM</td>
 * 		<td></td></tr>
 * <tr><td>Scale to handling a total of 100k metrics. Metric updates will be sent every 15 seconds with 15 second granularity</td>
 * 		<td>{@link #runTests(String)}</td></tr> 
 * <tr><td>Have a payload format for the delivery of the metrics which can be efficiently encoded on the APM/EM server without placing undo load on it</td>
 * 		<td>{@link #runTests(String)}</td></tr> 
 * </table>
 * 
 * <p/>
 * This test requires the <a href=https://code.google.com/p/odata4j/>Odata4J</a> and 
 * <a href=https://code.google.com/p/json-simple/>Json-Simple</a> libraries (Apache Licence 2.0).
 */
public class MetricUploadTest 
{
	/**
	 * The Json data must be encoded in this charater set.
	 */
	public static final String JSON_CHARACTER_SET = "UTF-8";
	
	/**
	 * Number of data points in each iteration.
	 */
	public static final int NUM_ITEMS = 100000;
	
	/**
	 * The size of the batches the points are sent in.
	 * {@link #NUM_ITEMS} are sent in <code>NUM_ITEMS / BATCH_SIZE</code> batches
	 * each iteration.
	 */
	public static final int BATCH_SIZE = 10000;
	
	/**
	 * Number of times the data points are sent. 
	 * {@link #NUM_ITEMS} are sent in each iteration at intervals of 
	 * {@link #INTERVAL_MS}. 
	 */
	public static final int NUM_ITERATIONS = 5;
	
	/**
	 * The frequency of each iteration.
	 */
	public static final int INTERVAL_MS = 15000;
	
	
    /**
     * The Prelert namespace
     */
	public final static String NAMESPACE = "Prelert";

	/**
	 * The MetricFeed Entity set name
	 */
	public final static String METRIC_FEEDS_SET = "MetricFeeds";
	
    /**
     * The MetricFeed Entity type name
     */
	public final static String METRIC_FEED_ENTITY = "MetricFeed";
		
	/**
	 * Flag set to true when the producer thread has completed.
	 */
	private volatile boolean m_ProducerFinished = false;
	
	/**
	 * Synchronised queue.
	 */
	private final BlockingQueue<OEntity> m_DataObjectsQueue = new SynchronousQueue<OEntity>();
	

    private boolean m_TestFailed = false;
    
    
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
		
		
		MetricUploadTest test = new MetricUploadTest();
		test.runTests(serviceUri);
	}		
	
	
	/**
	 * Check the ODATA metadata and run the metric upload tests.
	 * <br/>
	 * Creates the producer and consumer threads to generate the data points
	 * and send them to the web service.   
	 * 
	 * @param serviceUri
     * @return true if the tests ran successfully 
	 */
	public boolean runTests(String serviceUri)
	{
        System.out.println("Running the Metric Upload tests.");
        System.out.println("Using host " + serviceUri);

		ODataConsumer oDataConsumer = ODataConsumers.newBuilder(serviceUri).setFormatType(FormatType.JSON).build();

		// Read and validate metadata
        testValidateMetaData(oDataConsumer.getMetadata());
		
		Producer producer = new Producer();
		producer.setMetricFeedEntitySet(oDataConsumer.getMetadata().findEdmEntitySet(METRIC_FEEDS_SET));

		Consumer consumer = new Consumer();
		consumer.setConsumer(oDataConsumer);
		
	
		
		Thread pt = new Thread(producer, "producer");
		pt.start();
		Thread ct = new Thread(consumer, "consumer");
		ct.start();
		
		try
		{
			pt.join();
			// interrupt the consumer in case it is blocking on
			// the SynchronousQueue.take() function. 
			ct.interrupt();
			ct.join();
		}
		catch (InterruptedException e) 
		{
			e.printStackTrace();
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
	 * Consumer class collects OEntities created by the producer and sends 
	 * it to the web service.
	 */
	public class Consumer implements Runnable 
	{
		private ODataConsumer m_Consumer;
		
		private int m_SentTotal;
		

		/**
		 * Send the <code>OEntity</code> to the web service.
		 * 
		 * @param ent
		 */
		public void consume(OEntity ent) 
		{
			OEntity createdEnt = m_Consumer.createEntity(METRIC_FEEDS_SET).properties(ent.getProperties()).execute();
			
			testCondition(createdEnt != null, "Failed to create entity");

			OProperty<?> idProp = createdEnt.getProperty("Id");
			testCondition(idProp != null, "Create should return an entity with the 'Id' property.");
			
			List<OProperty<?>> props = createdEnt.getProperties();
			testCondition(props.size() == 1, "Create should return an entity with 1 property.");
		}
		
		private boolean theEnd() 
		{
			return m_ProducerFinished && m_DataObjectsQueue.isEmpty();
		}

		/**
		 * This method will block on {@link SynchronousQueue#take()} until an 
		 * OEntity is added to the queue, then the new entity is sent to the
		 * web service.
		 */
		@Override
		public void run() 
		{
			m_SentTotal = 0;
			
			while (!theEnd()) 
			{
				try 
				{
					// blocks here until something is put into the queue.
					OEntity ent = m_DataObjectsQueue.take();

					long iterStart = System.currentTimeMillis();
					consume(ent);
					
					int sentCount = (Integer)ent.getProperty("Count").getValue();
					m_SentTotal += sentCount;
					

					long iterEnd = System.currentTimeMillis();
					System.out.println(String.format("Uploaded %d metrics in %d ms", sentCount, iterEnd - iterStart));	
				}
				catch (InterruptedException e) 
				{
				}

			}
			
			System.out.println("Total uploaded metric count = " + m_SentTotal);
		}
		
		
		/**
		 * Set the OData Consumer for creating the MetricFeed entities.
		 *  
		 * @param consumer
		 */
		public void setConsumer(ODataConsumer consumer)
		{
			m_Consumer = consumer;
		}
	}
    
	/**
	 * Creates the <code>OEntitys</code> for the consumer.
	 * Each entity contains {@link #BATCH_SIZE} metrics
	 */
	public class Producer implements Runnable 
	{
		private EdmEntitySet m_EntitySet;

		/**
		 * Create the metric feed entity.
		 * 
		 * @param id
		 * @return The created entity
		 */
		@SuppressWarnings("unchecked")
		public OEntity produce(int id) 
		{
			JSONArray jsonArrayIn = new JSONArray();
			
			for (int i=0; i<BATCH_SIZE; i++)
			{
				
				String path = "Domains|myDomain|laivi02-745|Tomcat|Tomcat|Frontends|Apps|OrderEngin|URLs|Default|Called Backends|System localhost on port 6543:metric name0";
				Integer value = new Integer(101);

				JSONObject obj = new JSONObject();
				obj.put("m", path);
				obj.put("d", value);
				jsonArrayIn.add(obj);	
			}
			byte [] data = gzipCompress(jsonArrayIn.toJSONString().getBytes(Charset.forName(JSON_CHARACTER_SET)));
			
			int metricCount = BATCH_SIZE;
			
			List<OProperty<?>> dataProps = new ArrayList<OProperty<?>>();
			dataProps.add(OProperties.int32("Id", id));
			dataProps.add(OProperties.string("Source", "CA-APM"));
			dataProps.add(OProperties.datetimeOffset("CollectionTime", new DateTime()));
			dataProps.add(OProperties.int32("Count", metricCount));
			dataProps.add(OProperties.string("Compression", "gzip"));
			dataProps.add(OProperties.binary("Data", data));

			OEntity ent = OEntities.create(m_EntitySet, OEntityKey.create(id), dataProps, null);
			return ent;
		}
		

		/**
		 * Create a new entity with {@link #BATCH_SIZE} batch size metrics 
		 * and insert into the synchronisation queue. This method blocks on 
		 * <code>SynchronousQueue&ltE&gt.put(E)</code> until the consumer removes the object.
		 */
		@Override
		public void run() 
		{
			for (int i=0; i<NUM_ITERATIONS; i++)
			{
				int count = 0;
				long iterStart = System.currentTimeMillis();

				// produce items
				try 
				{
					while (count < NUM_ITEMS)
					{
						OEntity ent = produce(count);

						count += BATCH_SIZE;

						// will block here until the consumer thread takes.
						m_DataObjectsQueue.put(ent); 
					}
				}
				catch (InterruptedException e) 
				{
					System.out.println("Producer interrupted inserting into queue");
					e.printStackTrace();
					break;
				}
			
				
				long iterEnd = System.currentTimeMillis();
				System.out.println(String.format("%d metrics uploaded", count));	
				System.out.println();	
				
				long sleepTime = (iterStart + INTERVAL_MS) - iterEnd;
				if (sleepTime > 0)
				{
					try
					{
						Thread.sleep(sleepTime);
					}
					catch (InterruptedException e) 
					{
						System.out.println("Producer interrupted sleeping");
						break;
					}
				}
				
			}

			m_ProducerFinished = true;

		}
		
		
		/**
		 * The entity set the MetricFeed entities live in. 
		 * @param set
		 */
		public void setMetricFeedEntitySet(EdmEntitySet set)
		{
			m_EntitySet = set;
		}
	}
	
	
    /**
     * Request the Entity Data Model and validate.
     * <p/>
     * Equivalent URI:
     * <ul>
     *      <li>http://localhost:8080/prelertApi/prelert.svc/$metadata</li>
     * </ul>
     * @param metaData
     */
    public void testValidateMetaData(EdmDataServices metaData)
    {
        System.out.println("Testing Metadata");
        
        testCondition(metaData != null, "No metadata returned");
        
        // Sets
        EdmEntitySet set = metaData.getEdmEntitySet(METRIC_FEEDS_SET);
        testCondition(set != null, "Missing " + METRIC_FEEDS_SET + " entity set.");
        
        int count = 0;
        for (@SuppressWarnings("unused") EdmEntitySet s : metaData.getEntitySets())
        {
            count++;
        }
        testCondition(count == 4, "Wrong number of entity sets = " + count + ". Should be 4");
        
        // Entity types
        count = 0;
        boolean gotMetricFeedType = false;
        for (EdmEntityType type : metaData.getEntityTypes())
        {
            if (type.getName().equals(METRIC_FEED_ENTITY))
            {
            	gotMetricFeedType = true;
            }
            
            count++;
        }
        
        testCondition(gotMetricFeedType, "Missing Entity type " + METRIC_FEED_ENTITY);
        testCondition(count == 4, "Wrong number of entity types = " + count + ". Should be 4");
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
    
    
    /**
     * Gzip compress <code>data</code>
     * 
     * @param data
     * @return
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
