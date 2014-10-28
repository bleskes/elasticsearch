/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2011     *
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

package com.prelert.dev.config;

import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;


/**
 * Concrete subclass of <code>AbstractFileConfigurationProvider</code> which provides 
 * access to configuration data held in an XML file on the local file system. 
 * <p>
 * 
 * The class is built around the <code>PropertiesConfiguration</code> class from the 
 * Apache Commons library {@link http://commons.apache.org/configuration/}, so that 
 * the layout of the XML file, including empty lines and comments, is largely 
 * preserved when saving additions and modifications to the list of properties.
 * @author Pete Harverson
 */
public class LocalXMLConfigurationProvider extends AbstractFileConfigurationProvider
{
	static Logger s_Logger = Logger.getLogger(LocalXMLConfigurationProvider.class);
	
	
	/**
	 * Creates a new <code>ConfigurationProvider</code> for accessing configuration 
	 * data held in an XML file on the local file system. 
	 * @param fileName the name of the XML file holding the configuration properties.
	 * @throws IOException if an error occurs loading properties from the specified
	 * 	location.
	 */
	public LocalXMLConfigurationProvider(String fileName) throws IOException
	{
		try
        {
			m_Config = new XMLConfiguration(fileName);
        }
        catch (ConfigurationException e)
        {
	        throw new IOException("Error loading configuration from XML file " + 
	        		fileName + ": " + e.getMessage());
        }
        
        s_Logger.debug("Loaded XML file " + fileName);
	}
	
	
	public static void main(String[] args)
	{
		String propsFileName = "C:/tmp/prelert/config/wily_evidence_gatherer_orig.xml";
		try
		{
			LocalXMLConfigurationProvider xmlConfigProvider = 
				new LocalXMLConfigurationProvider(propsFileName);
			
			// Test out getting a property.
			String hostName = xmlConfigProvider.getString("tcp_server.host_name");
	        s_Logger.debug("tcp_server host_name : " + hostName);
	        
	        // Test out setting a property, and saving back to new file.
	        String tcpPort = xmlConfigProvider.getString("tcp_server.port");
	        s_Logger.debug("tcp_server port was : " + tcpPort);
	        xmlConfigProvider.setProperty("tcp_server.port", "12345");
	        
	        s_Logger.debug("tcp_server port now : " + xmlConfigProvider.getString("tcp_server.port"));
	        xmlConfigProvider.save("C:/tmp/prelert/config/wily_evidence_gatherer.xml");
		}
		catch (IOException e)
		{
			s_Logger.debug("Error carrying out test operations on " + 
					propsFileName + ": " + e.getMessage(), e);
		}
	}

}
