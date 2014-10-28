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

package com.prelert.api.test;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;

import com.prelert.api.data.Compression;
import com.prelert.api.data.GzipByteArrayUtil;
import com.prelert.api.data.MetricFeed;
import static com.prelert.api.data.MetricFeed.*;


/**
 * Test the functions of the metric feed class to process
 * Json data and convert <-> to OProperties
 */
public class MetricFeedConversionTest 
{
	@Test
	@SuppressWarnings("unchecked")
	public void testJsonCompression()
	{
		String path1 = "Domains|myDomain|laivi02-745|Tomcat|Tomcat|Frontends|Apps|OrderEngin|URLs|Default|Called Backends|System localhost on port 6543:metric name0";
		Integer value1 = new Integer(101);
		String path2 = "Domains|myDomain|laivi02-745|Tomcat|Tomcat|Frontends|Apps|OrderEngin|URLs|Default|Called Backends|System localhost on port 6543:metric name1";
		Double value2  = new Double(102);
		String path3 = "Domains|myDomain|laivi02-745|Tomcat|Tomcat|Frontends|Apps|OrderEngin|URLs|Default|Called Backends|System localhost on port 6543:metric name2";
		Integer value3 = new Integer(103);
		
		JSONArray jsonArrayIn = new JSONArray();
		
		JSONObject obj1 = new JSONObject();
		obj1.put(METRIC, path1);
		obj1.put(VALUE, value1);
		jsonArrayIn.add(obj1);
		
		JSONObject obj2 = new JSONObject();
		obj2.put(METRIC, path2);
		obj2.put(VALUE, value2);
		jsonArrayIn.add(obj2);

		JSONObject obj3 = new JSONObject();
		obj3.put(METRIC, path3);
		obj3.put(VALUE, value3);
		jsonArrayIn.add(obj3);
		
		
		// test parsing metric paths
		MetricFeed mf = new MetricFeed();
		mf.setCompression("gzip");
		
		// gzip json
		mf.setData(GzipByteArrayUtil.compress(jsonArrayIn.toJSONString().getBytes(Charset.forName(MetricFeed.JSON_CHARACTER_SET))));
		List<MetricFeed.MetricData> metrics = mf.getMetricData();
		
		assertTrue(metrics.get(0).getMetricPath().equals(path1));
		assertTrue(Double.compare(metrics.get(0).getValue(), value1) == 0);

		assertTrue(metrics.get(1).getMetricPath().equals(path2));
		assertTrue(Double.compare(metrics.get(1).getValue(), value2) == 0);
		
		assertTrue(metrics.get(2).getMetricPath().equals(path3));
		assertTrue(Double.compare(metrics.get(2).getValue(), value3) == 0);
		
		
		// same again without gzip compression
		mf = new MetricFeed();
		mf.setCompression("plain");
		
		// gzip json
		mf.setData(jsonArrayIn.toJSONString().getBytes(Charset.forName(JSON_CHARACTER_SET)));
		metrics = mf.getMetricData();
		
		assertTrue(metrics.get(0).getMetricPath().equals(path1));
		assertTrue(Double.compare(metrics.get(0).getValue(), value1) == 0);

		assertTrue(metrics.get(1).getMetricPath().equals(path2));
		assertTrue(Double.compare(metrics.get(1).getValue(), value2) == 0);
		
		assertTrue(metrics.get(2).getMetricPath().equals(path3));
		assertTrue(Double.compare(metrics.get(2).getValue(), value3) == 0);		
		
		
		
		// test getting and setting the data
		mf = new MetricFeed();
		mf.setCompression("gzip");
		
		// gzip json
		byte[] gzipData = GzipByteArrayUtil.compress(jsonArrayIn.toJSONString().getBytes(Charset.forName(JSON_CHARACTER_SET)));
		mf.setData(gzipData);
		
		String jsonData = jsonArrayIn.toJSONString();
		
		JSONArray jsonArrayOut = (JSONArray)JSONValue.parse(jsonData);
		assertTrue("Arrays not equal", jsonArrayOut.toJSONString().equals(jsonArrayIn.toJSONString()));
		
		JSONObject obj1Out = (JSONObject)jsonArrayOut.get(0);
		assertTrue("Obj1 not equal", obj1Out.toJSONString().equals(obj1.toJSONString()));

		JSONObject obj2Out = (JSONObject)jsonArrayOut.get(1);
		assertTrue("Obj2 not equal", obj2Out.toJSONString().equals(obj2.toJSONString()));
		
		JSONObject obj3Out = (JSONObject)jsonArrayOut.get(2);
		assertTrue("Obj3 not equal", obj3Out.toJSONString().equals(obj3.toJSONString()));
		
		
		// same again without gzip compression
		mf = new MetricFeed();
		mf.setCompression("plain");
		
		// gzip json
		gzipData = jsonArrayIn.toJSONString().getBytes(Charset.forName(MetricFeed.JSON_CHARACTER_SET));
		mf.setData(gzipData);
		
		jsonData = new String(gzipData, Charset.forName(MetricFeed.JSON_CHARACTER_SET));
		
		jsonArrayOut = (JSONArray)JSONValue.parse(jsonData);
		assertTrue("Arrays not equal", jsonArrayOut.toJSONString().equals(jsonArrayIn.toJSONString()));
		
		obj1Out = (JSONObject)jsonArrayOut.get(0);
		assertTrue("Obj1 not equal", obj1Out.toJSONString().equals(obj1.toJSONString()));

		obj2Out = (JSONObject)jsonArrayOut.get(1);
		assertTrue("Obj2 not equal", obj2Out.toJSONString().equals(obj2.toJSONString()));
		
		obj3Out = (JSONObject)jsonArrayOut.get(2);
		assertTrue("Obj3 not equal", obj3Out.toJSONString().equals(obj3.toJSONString()));
	
	}
	
	
	@Test
	public void testCreateFromOPropertiesNoData()
	{
		List<OProperty<?>> dataProps = new ArrayList<OProperty<?>>();
		dataProps.add(OProperties.int32("Id", new Integer(501)));
		dataProps.add(OProperties.string("Source", "CA-APM"));
		DateTime date = new DateTime();
		dataProps.add(OProperties.datetimeOffset("CollectionTime", date));
		dataProps.add(OProperties.string("Compression", "gzip"));
		dataProps.add(OProperties.int32("Count", new Integer(3)));
		dataProps.add(OProperties.string("Format", "json"));
		dataProps.add(OProperties.string("UnkownProperty", "This prop will not be recognised"));
		byte [] binaryData = new byte[0];
		dataProps.add(OProperties.binary("Data", binaryData));
		
		MetricFeed mf = MetricFeed.fromOProperties(dataProps);
		assertTrue(mf.getId() == 501);
		assertTrue(mf.getSource().equals("CA-APM"));
		assertTrue(mf.getCollectionTime().isEqual(date));
		assertTrue(mf.getCompression().equals(Compression.GZIP.toString()));
		assertTrue(mf.getCount() == 3);
	}
	
	
	@Test
	@SuppressWarnings("unchecked")
	public void testCreateFromOProperties()
	{
		String path1 = "Domains|myDomain|laivi02-745|Tomcat|Tomcat|Frontends|Apps|OrderEngin|URLs|Default|Called Backends|System localhost on port 6543:metric name0";
		Integer value1 = new Integer(101);
		String path2 = "Domains|myDomain|laivi02-745|Tomcat|Tomcat|Frontends|Apps|OrderEngin|URLs|Default|Called Backends|System localhost on port 6543:metric name1";
		Double value2  = new Double(102);
		String path3 = "Domains|myDomain|laivi02-745|Tomcat|Tomcat|Frontends|Apps|OrderEngin|URLs|Default|Called Backends|System localhost on port 6543:metric name2";
		Integer value3 = new Integer(103);
		
		JSONArray jsonArrayIn = new JSONArray();
		
		JSONObject obj1 = new JSONObject();
		obj1.put(METRIC, path1);
		obj1.put(VALUE, value1);
		jsonArrayIn.add(obj1);
		
		JSONObject obj2 = new JSONObject();
		obj2.put(METRIC, path2);
		obj2.put(VALUE, value2);
		jsonArrayIn.add(obj2);

		JSONObject obj3 = new JSONObject();
		obj3.put(METRIC, path3);
		obj3.put(VALUE, value3);
		jsonArrayIn.add(obj3);
		
		String [] compressions = {"gzip", "plain"};
		for (String comp : compressions)
		{
			List<OProperty<?>> dataProps = new ArrayList<OProperty<?>>();
			dataProps.add(OProperties.int32("Id", new Integer(501)));
			dataProps.add(OProperties.string("Source", "CA-APM"));
			DateTime date = new DateTime();
			dataProps.add(OProperties.datetimeOffset("CollectionTime", date));
			dataProps.add(OProperties.int32("Count", new Integer(3)));
			dataProps.add(OProperties.string("Format", "json"));
			dataProps.add(OProperties.string("UnkownProperty", "This prop will not be recognised"));

			byte [] binaryData = {};
			if (comp.equals("gzip"))
			{
				binaryData = GzipByteArrayUtil.compress(jsonArrayIn.toJSONString().getBytes(Charset.forName(JSON_CHARACTER_SET)));
			}
			else 
			{
				binaryData = jsonArrayIn.toJSONString().getBytes(Charset.forName(JSON_CHARACTER_SET));
			}
			
			dataProps.add(OProperties.binary("Data", binaryData));
			dataProps.add(OProperties.string("Compression", comp));

			MetricFeed mf = MetricFeed.fromOProperties(dataProps);
			assertTrue(mf.getId() == 501);
			assertTrue(mf.getSource().equals("CA-APM"));
			assertTrue(mf.getCollectionTime().isEqual(date));
			assertTrue(mf.getCompression().equals(comp));
			assertTrue(mf.getCount() == 3);

			// test metric data
			List<MetricFeed.MetricData> metrics = mf.getMetricData();

			assertTrue(metrics.get(0).getMetricPath().equals(path1));
			assertTrue(Double.compare(metrics.get(0).getValue(), value1) == 0);

			assertTrue(metrics.get(1).getMetricPath().equals(path2));
			assertTrue(Double.compare(metrics.get(1).getValue(), value2) == 0);

			assertTrue(metrics.get(2).getMetricPath().equals(path3));
			assertTrue(Double.compare(metrics.get(2).getValue(), value3) == 0);		
		}
	}
}
