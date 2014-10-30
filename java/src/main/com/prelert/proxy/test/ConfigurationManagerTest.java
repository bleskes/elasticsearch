/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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

package com.prelert.proxy.test;


import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.prelert.proxy.configuration.ConfigurationManager;
import com.prelert.proxy.data.DataTypeConfig;
import com.prelert.proxy.data.InputManagerConfig;
import com.prelert.proxy.data.SourceConnectionConfig;
import com.prelert.proxy.inputmanager.InputManagerType;

public class ConfigurationManagerTest 
{
	/**
	 * Test configuration manager functions particulary the password
	 * encryption when the configs are saved to file.
	 * 
	 * Writes files to temp dir then deletes them once the test completes.
	 */
	@Test
	public void saveConfigTest()
	{
		File tempDir = new File(System.getProperty("java.io.tmpdir"));
		
		InputManagerConfig imConfig = new InputManagerConfig();
		imConfig.setInputManagerType(InputManagerType.INTERNAL);
		imConfig.setHost("localhost");
		imConfig.setPort(49996);
		imConfig.setStartDate(new Date());
		imConfig.setQueryLengthSecs(120);
		imConfig.setUpdateIntervalSecs(120);
		imConfig.setDelaySecs(0);

		SourceConnectionConfig source = new SourceConnectionConfig();
		source.setHost("localhost");
		source.setPort(1433);
		source.setUsername("user");
		source.setPassword("pass");

		DataTypeConfig config = new DataTypeConfig("TestType1", "testPlugin");
		config.setInputManagerConfig(imConfig);
		config.setSourceConnectionConfig(source);

		config.addPluginProperty("key1", "value1");
		config.addPluginProperty("key2", "value2");
		config.addPluginProperty("key3", "value3");
		
		// Test saving the config then reloading it equals the original
		ConfigurationManager configManager = new ConfigurationManager();
		configManager.addDataTypeConfig(config);
		configManager.saveConfigurations(tempDir);
		
		configManager.loadConfigurations(tempDir);
		configManager.reload();
		DataTypeConfig reloaded = configManager.getDataTypeConfig("TestType1");
		assertEquals(config, reloaded);
		
		
		// Change password and test again (config manager encrypts password)
		config.setDataType("TestType2");
		config.getSourceConnectionConfig().setPassword("");
		configManager.addDataTypeConfig(config);
		configManager.saveConfigurations(tempDir);
		
		configManager.reload();
		reloaded = configManager.getDataTypeConfig("TestType2");
		assertEquals(config, reloaded);
		
		// set password to null and test again (config manager encrypts password)
		config.setDataType("TestType3");
		config.getSourceConnectionConfig().setPassword(null);
		configManager.addDataTypeConfig(config);
		configManager.saveConfigurations(tempDir);
		
		configManager.reload();
		reloaded = configManager.getDataTypeConfig("TestType3");
		assertEquals(config, reloaded);
		
		new File(tempDir, "TestType1.xml").delete();
		new File(tempDir, "TestType2.xml").delete();
		new File(tempDir, "TestType3.xml").delete();
	}
	
	
	/**
	 * Test writing configs to Xml and loading it back again.
	 * Writes file to temp dir then deletes it once the test completes.
	 * @throws SAXException
	 * @throws IOException
	 */
	@Test
	public void toXmlTest() throws SAXException, IOException
	{
		InputManagerConfig imConfig = new InputManagerConfig();
		imConfig.setInputManagerType(InputManagerType.INTERNAL);
		imConfig.setHost("localhost");
		imConfig.setPort(49996);
		imConfig.setStartDate(new Date());
		imConfig.setQueryLengthSecs(120);
		imConfig.setUpdateIntervalSecs(120);

		SourceConnectionConfig source = new SourceConnectionConfig();
		source.setHost("localhost");
		source.setPort(1433);
		source.setUsername("user");
		source.setPassword("pass");

		DataTypeConfig config = new DataTypeConfig("TestType3", "testPlugin");
		config.setInputManagerConfig(imConfig);
		config.setSourceConnectionConfig(source);

		config.addPluginProperty("key1", "value1");
		config.addPluginProperty("key2", "value2");
		config.addPluginProperty("key3", "value3");

		
		String tempDir = System.getProperty("java.io.tmpdir");
		String filename = "TestDataTypeConfig.xml";

		try 
		{
			File file = new File(tempDir, filename);
			Result result = new StreamResult(file);

			Document toDoc = config.toXml();
			
			Source domSource = new DOMSource(toDoc);

			// Write the DOM document to the file
			Transformer xformer = TransformerFactory.newInstance().newTransformer();
			xformer.setOutputProperty(OutputKeys.INDENT, "yes");
			xformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			xformer.transform(domSource, result);
			
			// now read it back in
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document fromDoc = docBuilder.parse(file);
			
			DataTypeConfig reloadedConfig = DataTypeConfig.fromXml(fromDoc);
			assertEquals(config, reloadedConfig);
			
			reloadedConfig.addPluginProperty("key4", "value4");
			assertFalse(config.equals(reloadedConfig));
			
			// clean up
			file.delete();
		}
		catch (ParserConfigurationException e) 
		{
			e.printStackTrace();
		}
		catch (TransformerConfigurationException e) 
		{
			e.printStackTrace();
		}
		catch (TransformerFactoryConfigurationError e) 
		{
			e.printStackTrace();
		}
		catch (TransformerException e) 
		{
			e.printStackTrace();
 		}  
		catch (ParseException e) 
		{
			e.printStackTrace();
		}

		
	}

}
