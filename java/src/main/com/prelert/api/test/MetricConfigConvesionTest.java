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

import static org.junit.Assert.*;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.junit.Test;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;

import com.prelert.api.data.BeanToOProperties;
import com.prelert.api.data.GzipByteArrayUtil;
import com.prelert.api.data.MetricConfig;

public class MetricConfigConvesionTest 
{
	/**
	 * The character set the Json data is encoded in.
	 */
	public static final String JSON_DATA_CHARSET = "UTF-8";
	
	@Test
	@SuppressWarnings("unchecked")
	public void testJsonToMetricPathStrings()
	{
		String path1 = "Domains|myDomain|laivi02-745|Tomcat|Tomcat|Frontends|Apps|OrderEngin|URLs|Default|Called Backends|System localhost on port 6543:metric name0";
		String path2 = "Domains|myDomain|laivi02-745|Tomcat|Tomcat|Frontends|Apps|OrderEngin|URLs|Default|Called Backends|System localhost on port 6543:metric name1";
		String path3 = "Domains|myDomain|laivi02-745|Tomcat|Tomcat|Frontends|Apps|OrderEngin|URLs|Default|Called Backends|System localhost on port 6543:metric name2";

		JSONArray jsonArrayIn = new JSONArray();
		jsonArrayIn.add(path1);
		jsonArrayIn.add(path2);
		jsonArrayIn.add(path3);
		
		MetricConfig mc = new MetricConfig();
		mc.setCompression("gzip");
		mc.setMetricNames(GzipByteArrayUtil.compress(jsonArrayIn.toJSONString().getBytes(Charset.forName(JSON_DATA_CHARSET))));
		
		List<String> metricPaths = mc.getMetricPaths();
		
		assertTrue(metricPaths.size() == 3);
		assertTrue(metricPaths.get(0).equals(path1));
		assertTrue(metricPaths.get(1).equals(path2));
		assertTrue(metricPaths.get(2).equals(path3));
		
		
		// Same again without compression
		mc = new MetricConfig();
		mc.setCompression("plain");
		mc.setMetricNames(jsonArrayIn.toJSONString().getBytes(Charset.forName(JSON_DATA_CHARSET)));
		
		metricPaths = mc.getMetricPaths();
		
		assertTrue(metricPaths.size() == 3);
		assertTrue(metricPaths.get(0).equals(path1));
		assertTrue(metricPaths.get(1).equals(path2));
		assertTrue(metricPaths.get(2).equals(path3));
	}
	
	
	@Test
	@SuppressWarnings("unchecked")
	public void testCreateFromOProperties()
	{
		String path1 = "Domains|myDomain|laivi02-745|Tomcat|Tomcat|Frontends|Apps|OrderEngin|URLs|Default|Called Backends|System localhost on port 6543:metric name0";
		String path2 = "Domains|myDomain|laivi02-745|Tomcat|Tomcat|Frontends|Apps|OrderEngin|URLs|Default|Called Backends|System localhost on port 6543:metric name1";
		String path3 = "Domains|myDomain|laivi02-745|Tomcat|Tomcat|Frontends|Apps|OrderEngin|URLs|Default|Called Backends|System localhost on port 6543:metric name2";

		JSONArray jsonArrayIn = new JSONArray();
		jsonArrayIn.add(path1);
		jsonArrayIn.add(path2);
		jsonArrayIn.add(path3);
		
		
		String [] compressions = {"gzip", "plain"};
		for (String comp : compressions)
		{
			List<OProperty<?>> dataProps = new ArrayList<OProperty<?>>();
			dataProps.add(OProperties.int32("Id", new Integer(501)));
			dataProps.add(OProperties.int32("Count", new Integer(3)));
			dataProps.add(OProperties.string("UnkownProperty", "This prop will not be recognised"));

			byte [] binaryData = {};
			if (comp.equals("gzip"))
			{
				binaryData = GzipByteArrayUtil.compress(jsonArrayIn.toJSONString().getBytes(Charset.forName(JSON_DATA_CHARSET)));
			}
			else 
			{
				binaryData = jsonArrayIn.toJSONString().getBytes(Charset.forName(JSON_DATA_CHARSET));
			}
			
			dataProps.add(OProperties.binary("MetricNames", binaryData));
			dataProps.add(OProperties.string("Compression", comp));
			
			MetricConfig mc = MetricConfig.fromOProperties(dataProps);
			assertTrue(mc.getId() == 501);
			assertTrue(mc.getCompression().equals(comp));
			assertTrue(mc.getCount() == 3);
			
			List<String> metricPaths = mc.getMetricPaths();
			
			assertTrue(metricPaths.size() == 3);
			assertTrue(metricPaths.get(0).equals(path1));
			assertTrue(metricPaths.get(1).equals(path2));
			assertTrue(metricPaths.get(2).equals(path3));
		}
	}
	
	@Test
	public void testBeanToOProperties()
	{
		List<String> metricPaths = new ArrayList<String>();
		metricPaths.add("Domains|myDomain|laivi02-745|Tomcat|Tomcat|Frontends|Apps|OrderEngin|URLs|Default|Called Backends|System localhost on port 6543:metric name0");
		metricPaths.add("Domains|myDomain|laivi02-745|Tomcat|Tomcat|Frontends|Apps|OrderEngin|URLs|Default|Called Backends|System localhost on port 6543:metric name1");
		metricPaths.add("Domains|myDomain|laivi02-745|Tomcat|Tomcat|Frontends|Apps|OrderEngin|URLs|Default|Called Backends|System localhost on port 6543:metric name2");
		

		List<OProperty<?>> dataProps = BeanToOProperties.metricConfigOProperties(metricPaths);
		
		MetricConfig mc = MetricConfig.fromOProperties(dataProps);
		assertTrue(mc.getId() == 1);
		assertTrue(mc.getCompression().equals("gzip"));
		assertTrue(mc.getCount() == 3);
		
		List<String> metricPathsOut = mc.getMetricPaths();
		
		assertTrue(metricPaths.size() == metricPathsOut.size());
		assertTrue(metricPaths.get(0).equals(metricPathsOut.get(0)));
		assertTrue(metricPaths.get(1).equals(metricPathsOut.get(1)));
		assertTrue(metricPaths.get(2).equals(metricPathsOut.get(2)));
	}
	
}
