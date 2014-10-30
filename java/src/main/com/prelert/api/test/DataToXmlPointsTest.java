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
 ***********************************************************/

package com.prelert.api.test;

import static com.prelert.api.data.MetricFeed.METRIC;
import static com.prelert.api.data.MetricFeed.VALUE;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.prelert.api.data.MetricFeed;

public class DataToXmlPointsTest 
{
	@SuppressWarnings("unchecked")
	@Test
	public void validateTimeSeriesPointsXml() throws IOException, ParserConfigurationException, SAXException
	{
//		String xsdFilename = "tagged_points.xsd";
//		InputStream xsdInputStream = this.getClass().getClassLoader().getResourceAsStream(xsdFilename);

		String prelertSrcHome = System.getenv("PRELERT_SRC_HOME");
		assertNotNull("$PRELERT_SRC_HOME not defined", prelertSrcHome);
		String xsdFilename = prelertSrcHome + "/config/xsd/tagged_points.xsd";
		File f = new File(xsdFilename);
		InputStream xsdInputStream = new FileInputStream(f);

		
		assertNotNull("Failed to load file " + xsdFilename, xsdInputStream);
		
	    // parse an XML document into a DOM tree
	    DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();

	    // create a SchemaFactory capable of understanding WXS schemas
	    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

	    // load a WXS schema, represented by a Schema instance
	    Source schemaFile = new StreamSource(xsdInputStream);
	    Schema schema = factory.newSchema(schemaFile);

    
	    MetricFeed feed = new MetricFeed();
	    feed.setCollectionTime(new DateTime());
	    feed.setCompression("plain");
	    feed.setCount(3);
	    feed.setId(1);
	    feed.setSource("CA-APM");
	    
	    
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
	    
		feed.setData(jsonArrayIn.toJSONString().getBytes(Charset.forName("UTF-8")));
	        

	    // validate the DOM tree
		for (int i=0; i<3; i++)
		{
			MetricFeed.MetricData md = feed.getMetricData().get(i);
			
			InputStream stream = new ByteArrayInputStream(feed.toXmlStringExternal(md, 1, 1, true).getBytes("UTF-8"));
			Document document = parser.parse(stream);
			
			// create a Validator instance, which can be used to validate an instance document
			Validator validator = schema.newValidator();
			
			// validate throws if invalid
			validator.validate(new DOMSource(document));
		}
	}

}
