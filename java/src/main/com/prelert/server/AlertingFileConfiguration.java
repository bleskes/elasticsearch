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

package com.prelert.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.prelert.data.Alerter;


/**
 * Class for reading and writing the UI alerting configuration to a file on
 * the Prelert server.
 * @author Pete Harverson
 */
public class AlertingFileConfiguration
{
	private static Logger s_Logger = Logger.getLogger(AlertingFileConfiguration.class);
	
	/** Filename of the UI alerters XML configuration file */
	public static final String GUI_ALERTERS_FILENAME = "gui_alerters.xml";
	
	
	public static final String ELEMENT_NAME_ALERTERS 	= "alerters";
	public static final String ELEMENT_NAME_ALERTER 	= "alerter";
	public static final String ELEMENT_NAME_ENABLED 	= "enabled";
	public static final String ELEMENT_NAME_THRESHOLD 	= "threshold";
	public static final String ELEMENT_NAME_SCRIPT 		= "script";
	public static final String ATTRIBUTE_NAME_TYPE 		= "type";
	public static final String ATTRIBUTE_VALUE_SCRIPT 	= "script";
	
	
	/**
	 * Loads the Alerter configuration from the UI alerters configuration file 
	 * stored on the Prelert server.
	 * @return the <code>Alerter</code> stored in the configuration file.
	 * @throws FileNotFoundException if the UI alerters file could not 
	 * 	be found on the server.
	 * @throws IOException if an error occurred reading data from the configuration file.
	 */
	public Alerter load() throws FileNotFoundException, IOException
	{
		Alerter alerter = new Alerter();
		alerter.setType(Alerter.TYPE_SCRIPT);
		
		// Try to load from the GUI alerters config file under $PRELERT_HOME.
		String prelertConfigHome = ServerUtil.getPrelertConfigHome();
		if (prelertConfigHome != null)
		{
			File configFile = new File(prelertConfigHome + File.separatorChar + GUI_ALERTERS_FILENAME);
			if (configFile.exists())
			{
				try
		        {
					XMLConfiguration config = new XMLConfiguration(configFile);
					
					// Only script type alerters supported for now.
					// Read the type, enabled, threshold and script properties.
					String type = config.getString(ELEMENT_NAME_ALERTER + ".[@" + ATTRIBUTE_NAME_TYPE + "]");
					boolean isEnabled = config.getBoolean(
							ELEMENT_NAME_ALERTER + "." + ELEMENT_NAME_ENABLED, false);
					int threshold = config.getInt(
							ELEMENT_NAME_ALERTER + "." + ELEMENT_NAME_THRESHOLD, 
							Alerter.THRESHOLD_DISABLED);
					String scriptName = config.getString(ELEMENT_NAME_ALERTER + "." + ELEMENT_NAME_SCRIPT);
					
					if (type != null)
					{
						alerter.setType(type);
					}
					
					alerter.setEnabled(isEnabled);
					
					if (threshold > 0 && threshold <= 100)
					{
						alerter.setThreshold(threshold);
					}
					
					alerter.setScriptName(scriptName);
					s_Logger.debug("load() loaded alerter " + alerter + " from " + configFile);
		        }
		        catch (ConfigurationException e)
		        {
			        s_Logger.error("load() error loading configuration from XML file " + 
			        		configFile, e);
			        throw new IOException("Error loading configuration from XML file " + configFile);
		        }
			}
			else
			{
				throw new FileNotFoundException(configFile + " does not exist on the server");
			}
		}
		else
		{
			throw new FileNotFoundException("$PRELERT_HOME is undefined");
		}
		
		return alerter;
	}
	
	
	/**
	 * Saves the specified Alerter configuration to the UI alerters configuration 
	 * file stored on the Prelert server. If the file is not present, the method
	 * will create the file from scratch.
	 * @return the <code>Alerter</code> stored in the configuration file.
	 * @throws FileNotFoundException if the UI alerters file was not present on the 
	 * 	Prelert server and could not be created.
	 * @throws IOException if an error occurred writing data to the configuration file.
	 */
	public void save(Alerter alerter) throws FileNotFoundException, IOException
	{
		// Try to load from the GUI alerters config file under $PRELERT_HOME.
		String prelertConfigHome = ServerUtil.getPrelertConfigHome();
		if (prelertConfigHome != null)
		{
			File configFile = new File(prelertConfigHome + File.separatorChar + GUI_ALERTERS_FILENAME);
			if (configFile.exists() == false)
			{
				// Attempt to create gui_alerters.xml from scratch.
				try
				{
					createGUIAlertersFile(configFile);
				}
				catch (Exception e)
				{
					s_Logger.error("save() " + alerter + " " + configFile + 
							" does not exist and cannot be created.", e);
					throw new FileNotFoundException(configFile + 
							" does not exist and cannot be created");
				}
			}
			
			if (configFile.exists())
			{
				try
		        {
					XMLConfiguration config = new XMLConfiguration(configFile);
					
					// Only script type alerters supported for now.
					// Set the type, enabled, threshold and script properties.
					config.setProperty(
							ELEMENT_NAME_ALERTER + ".[@" + ATTRIBUTE_NAME_TYPE + "]", 
							alerter.getType());
					config.setProperty(ELEMENT_NAME_ALERTER + "." + ELEMENT_NAME_ENABLED, 
							alerter.isEnabled());
					config.setProperty(ELEMENT_NAME_ALERTER + "." + ELEMENT_NAME_THRESHOLD, alerter.getThreshold());
					
					String scriptName = alerter.getScriptName() != null ?
						alerter.getScriptName() : "";
					config.setProperty(ELEMENT_NAME_ALERTER + "." + ELEMENT_NAME_SCRIPT, scriptName);
					config.save();
					
					s_Logger.debug("save() alerter " + alerter + " saved to " + configFile);
		        }
		        catch (ConfigurationException e)
		        {
			        s_Logger.error("save() error saving alerter " + alerter + " to XML file " + 
			        		configFile, e);
			        throw new IOException("Error saving alerter to file " + configFile);
		        }
			}
			else
			{
				throw new FileNotFoundException(configFile + " does not exist");
			}
		}
		else
		{
			throw new FileNotFoundException("$PRELERT_HOME is undefined");
		}
	}
	
	
	/**
	 * Creates and saves the GUI alerters XML configuration file, for use when the 
	 * file created on install is found to be absent.
	 * @param alertersFile full File path to the GUI alerters XML configuration. 
	 * @throws ParserConfigurationException if an XML document builder could not be created.
	 * @throws TransformerException if an error occurs transforming the document to 
	 * 	XML file contents.
	 */
	protected void createGUIAlertersFile(File alertersFile) throws 
		ParserConfigurationException, TransformerException
	{
		// Contents of file is in this format:
		//	<alerters>
		//	    <alerter type="script">
		//	        <enabled>true</enabled>
		//	        <threshold>50</threshold>
		//	        <script>sendMail.bat</script>
		//	    </alerter>
		//	</alerters>
		
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// Root element.
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement(ELEMENT_NAME_ALERTERS);
		doc.appendChild(rootElement);

		// Create alerter element.
		Element alerter = doc.createElement(ELEMENT_NAME_ALERTER);
		rootElement.appendChild(alerter);

		// Add type attribute to alerter element.
		alerter.setAttribute(ATTRIBUTE_NAME_TYPE, ATTRIBUTE_VALUE_SCRIPT);

		// enabled element
		Element enabled = doc.createElement(ELEMENT_NAME_ENABLED);
		enabled.appendChild(doc.createTextNode("false"));
		alerter.appendChild(enabled);

		// threshold element
		Element threshold = doc.createElement(ELEMENT_NAME_THRESHOLD);
		alerter.appendChild(threshold);

		// script element
		Element script = doc.createElement(ELEMENT_NAME_SCRIPT);
		alerter.appendChild(script);

		// Write the content to file.
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(alertersFile);

		transformer.transform(source, result);

		s_Logger.debug("createGUIAlertersFile() created file " + alertersFile);
	}
}
