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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
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
 * Test the Prelert Metric Configuration API. 
 * <p/>
 * The following requirements are tested:
 * <table border=1>
 * <tr><th>Requirement</th><th>Test Case</th></tr>
 * <tr><td>Update which metric paths are being tracked (both add and remove)</td>
 * 		<td>{@link #testCreateMetricConfig(ODataConsumer)}</td></tr>
 * <tr><td>It must be able to do this dynamically so that a restart of the Prelert server is not required after an update</td>
 * 		<td>{@link #testCreateMetricConfig(ODataConsumer)}</td></tr>
 * </table>
 * 
 * <p/>
 * This test requires the <a href=https://code.google.com/p/odata4j/>Odata4J</a> and 
 * <a href=https://code.google.com/p/json-simple/>Json-Simple</a> libraries (Apache Licence 2.0).
 */
public class MetricConfigTest 
{   
	/**
	 * The Json data must be encoded in this charater set.
	 */
	public static final String JSON_CHARACTER_SET = "UTF-8";
	
    /**
     * The Prelert namespace
     */
    public final static String NAMESPACE = "Prelert";
    
    /**
     * The MetricConfig Entity type name
     */
	public final static String METRIC_CONFIG_ENTITY = "MetricConfig";
	
    /**
     * The MetricConfig Entity set name.
     */
	public final static String METRIC_CONFIG_SET = "MetricConfigs";
	
	
    private boolean m_TestFailed = false;
    
	
	/**
     * Main entry point, runs the configuration tests.
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
		
		
		MetricConfigTest test = new MetricConfigTest();
		test.runTests(serviceUri);
	}
	
	/**
	 * Run the configuration tests.
	 * @param serviceUri
     * @return true if the tests ran successfully 
	 */
	public boolean runTests(String serviceUri)
	{
        System.out.println("Running the Metric Configuration API tests.");
        System.out.println("Using host " + serviceUri);
        
        ODataConsumer consumer = ODataConsumers.newBuilder(serviceUri).setFormatType(FormatType.JSON).build();
        
        
        // Read and validate metadata
        testValidateMetaData(consumer.getMetadata());
        
        testCreateMetricConfig(consumer);
        
        testUpdateMetricConfig(consumer);
        
        
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
        EdmEntitySet set = metaData.getEdmEntitySet(METRIC_CONFIG_SET);
        testCondition(set != null, "Missing " + METRIC_CONFIG_SET + " entity set.");
        
        int count = 0;
        for (@SuppressWarnings("unused") EdmEntitySet s : metaData.getEntitySets())
        {
            count++;
        }
        testCondition(count == 4, "Wrong number of entity sets = " + count + ". Should be 4");
        
        // Entity types
        count = 0;
        boolean gotMetricConfigType = false;
        for (EdmEntityType type : metaData.getEntityTypes())
        {
            if (type.getName().equals(METRIC_CONFIG_ENTITY))
            {
                gotMetricConfigType = true;
            }
            
            count++;
        }
        
        testCondition(gotMetricConfigType, "Missing Entity type " + METRIC_CONFIG_ENTITY);
        testCondition(count == 4, "Wrong number of entity types = " + count + ". Should be 4");
    }
    
    
    /**
     * Update the metric configuration using the <code>create</code> endpoint.
     * <p/>
     * Equivalent URI:
     * <ul>
     *      <li>http://localhost:8080/prelertApi/prelert.svc/MetricConfigs(1)</li>
     * </ul>
     * @param consumer
     */
    @SuppressWarnings("unchecked")
	public void testCreateMetricConfig(ODataConsumer consumer)
    {
        System.out.println("Testing Create Metric config");
        
    	String path0 = "Host|Process|Agent:metric0";
    	String path1 = "Host|Process|Agent:metric1";
    	String path2 = "Host|Process1|Agent:metric2";
    	String path3 = "Host|Process3|Agent:metric3";
    	String path4 = "Host|Process|Agent4:metric4";
    	String path5 = "Host|Process|Agent4:metric5";
    	
		JSONArray jsonArray = new JSONArray();
		jsonArray.add(path0);
		jsonArray.add(path1);
		jsonArray.add(path2);
		jsonArray.add(path3);
		jsonArray.add(path4);
		jsonArray.add(path5);
		
    	byte [] data = gzipCompress(jsonArray.toJSONString().getBytes(Charset.forName(JSON_CHARACTER_SET)));
    	
    	final int id = 1;
    	
		List<OProperty<?>> dataProps = new ArrayList<OProperty<?>>();
		dataProps.add(OProperties.int32("Id", id));
		dataProps.add(OProperties.int32("Count", jsonArray.size()));
		dataProps.add(OProperties.string("Compression", "gzip"));
		dataProps.add(OProperties.binary("MetricNames", data));
		
		OEntity ent = consumer.createEntity(METRIC_CONFIG_SET).properties(dataProps).execute();
		testCondition(ent != null, "Update metric config returned null");
		
		
		OProperty<?> idProp = ent.getProperty("Id");
		testCondition(idProp != null, "Update config should return an entity with the 'Id' property.");
	
		List<OProperty<?>> props = ent.getProperties();
		testCondition(props.size() == 1, "Update config should return an entity with only 1 property.");
	
		
		// now read the config back.
		ent = consumer.getEntity(METRIC_CONFIG_SET, 1).execute();
		testCondition((Integer)ent.getProperty("Id").getValue() == 1, 
				"Get config should return an entity with the 'Id' = 1");
		testCondition(((String)ent.getProperty("Compression").getValue()).equals("gzip"),
				"Get config should return an entity with the 'Compression' = 'gzip'");
		
		byte [] metrics = (byte [])ent.getProperty("MetricNames").getValue();
		testCondition(metrics != null, "Get config returned null MetricNames");
		
		metrics = gzipUncompress(metrics);
		
		List<String> metricPaths = new ArrayList<String>();
		
		JSONArray jsonArrayIn = (JSONArray)JSONValue.parse(new String(metrics, 
				Charset.forName(JSON_CHARACTER_SET)));
		for (int i=0; i<jsonArrayIn.size(); i++)
		{
			String path = (String)jsonArrayIn.get(i);
			metricPaths.add(path);
		}
		
		testCondition(metricPaths.size() > 0, "No metrics returned by get metric config");
		testCondition(metricPaths.size() == 6, "Get metric config returned " + metricPaths.size() 
				+ " metric paths not 6");
		
		
		testCondition(metricPaths.get(0).equals(path0), "Metric Paths not equal");
		testCondition(metricPaths.get(1).equals(path1), "Metric Paths not equal");
		testCondition(metricPaths.get(2).equals(path2), "Metric Paths not equal");
		testCondition(metricPaths.get(3).equals(path3), "Metric Paths not equal");
		testCondition(metricPaths.get(4).equals(path4), "Metric Paths not equal");
		testCondition(metricPaths.get(5).equals(path5), "Metric Paths not equal");
    }
    
    
    /**
     * Update the metric configuration using the <code>update</code> endpoint.
     * <p/>
     * Equivalent URI:
     * <ul>
     *      <li>http://localhost:8080/prelertApi/prelert.svc/MetricConfigs(1)</li>
     * </ul>
     * @param consumer
     */
    @SuppressWarnings("unchecked")
	public void testUpdateMetricConfig(ODataConsumer consumer)
    {
        System.out.println("Testing Update Metric config");
        
    	String path0 = "Host|Process|Agent:metric0";
    	String path1 = "Host|Process|Agent:metric1";
    	String path2 = "Host|Process1|Agent:metric2";
    	
		JSONArray jsonArray = new JSONArray();
		jsonArray.add(path0);
		jsonArray.add(path1);
		jsonArray.add(path2);

		
    	byte [] data = gzipCompress(jsonArray.toJSONString().getBytes(Charset.forName(JSON_CHARACTER_SET)));
    	
    	final int id = 1;
    	
		List<OProperty<?>> dataProps = new ArrayList<OProperty<?>>();
		dataProps.add(OProperties.int32("Id", id));
		dataProps.add(OProperties.int32("Count", jsonArray.size()));
		dataProps.add(OProperties.string("Compression", "gzip"));
		dataProps.add(OProperties.binary("MetricNames", data));
		
		EdmEntitySet entitySet = consumer.getMetadata().findEdmEntitySet(METRIC_CONFIG_SET);
		testCondition(entitySet != null, "Cannot find " + METRIC_CONFIG_SET + " entity set");
		
		OEntity updateEnt = OEntities.create(entitySet, OEntityKey.create(id), dataProps, null);
		
		// Do the update
		consumer.updateEntity(updateEnt).execute();
	
		
		// now read the config back.
		OEntity ent = consumer.getEntity(METRIC_CONFIG_SET, 1).execute();
		testCondition((Integer)ent.getProperty("Id").getValue() == 1, 
				"Get config should return an entity with the 'Id' = 1");
		testCondition(((String)ent.getProperty("Compression").getValue()).equals("gzip"),
				"Get config should return an entity with the 'Compression' = 'gzip'");
		
		byte [] metrics = (byte [])ent.getProperty("MetricNames").getValue();
		testCondition(metrics != null, "Get config returned null MetricNames");
		
		metrics = gzipUncompress(metrics);
		
		List<String> metricPaths = new ArrayList<String>();
		
		JSONArray jsonArrayIn = (JSONArray)JSONValue.parse(new String(metrics, 
				Charset.forName(JSON_CHARACTER_SET)));
		for (int i=0; i<jsonArrayIn.size(); i++)
		{
			String path = (String)jsonArrayIn.get(i);
			metricPaths.add(path);
		}
		
		testCondition(metricPaths.size() > 0, "No metrics returned by get metric config");
		testCondition(metricPaths.size() == 3, "Get metric config returned " + metricPaths.size() 
				+ " metric paths not 6");
		
		
		testCondition(metricPaths.get(0).equals(path0), "Metric Paths not equal");
		testCondition(metricPaths.get(1).equals(path1), "Metric Paths not equal");
		testCondition(metricPaths.get(2).equals(path2), "Metric Paths not equal");
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
     * @return compressed data
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
    
    /**
     * Gzip uncompress <code>data</code>
     * 
     * @param data
     * @return the uncompressed data
     */
	private byte[] gzipUncompress(byte[] data) 
	{
		try 
		{
			byte[] buffer = new byte[5120];
			ByteArrayOutputStream oBuf = new ByteArrayOutputStream();
			ByteArrayInputStream iBuf = new ByteArrayInputStream(data);
			
			GZIPInputStream gIn = new GZIPInputStream(iBuf);
			for (int len = gIn.read(buffer); len != -1; len = gIn.read(buffer)) 
			{
				oBuf.write(buffer, 0, len);
			}
			
			gIn.close();
			iBuf.close();
			oBuf.close();
			
			return oBuf.toByteArray();
		} 
		catch (Exception e) 
		{
			System.out.println("ERROR: gzip decompress failed!");
		}
		return null;
	}
}
