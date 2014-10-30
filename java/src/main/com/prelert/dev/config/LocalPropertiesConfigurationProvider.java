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
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;


/**
 * Concrete subclass of <code>AbstractFileConfigurationProvider</code> which provides 
 * access to configuration data held in a properties file on the local file system. 
 * <p>
 * A properties file is one which contains a list of key / value pairs in a simple 
 * line-oriented format, with the key and value separated by  <code>'='</code>, 
 * <code>':'</code> or any white space character. Examples:
 * <pre>
 *  key1 = value1
 *  key2 : value2
 *  key3   value3
 *  </pre>
 * 
 * The class is built around the <code>PropertiesConfiguration</code> class from the 
 * Apache Commons library {@link http://commons.apache.org/configuration/}, so that 
 * the layout of the properties file, including empty lines and comments, is largely 
 * preserved when saving additions and modifications to the list of properties.
 * @author Pete Harverson
 */
public class LocalPropertiesConfigurationProvider extends AbstractFileConfigurationProvider
{
	static Logger s_Logger = Logger.getLogger(LocalPropertiesConfigurationProvider.class);
	
	
	/**
	 * Creates a new <code>ConfigurationProvider</code> for accessing configuration 
	 * data held in a properties file on the local file system. 
	 * @param fileName the name of the file holding the configuration properties.
	 * @throws IOException if an error occurs loading properties from the specified
	 * 	location.
	 */
	public LocalPropertiesConfigurationProvider(String fileName) throws IOException
	{
		try
        {
	        m_Config = new PropertiesConfiguration(fileName);
        }
        catch (ConfigurationException e)
        {
	        throw new IOException("Error loading properties from file " + 
	        		fileName + ": " + e.getMessage());
        }
        
        s_Logger.debug("Loaded properties file " + fileName);
	}


	public static void main(String[] args)
	{
		String propsFileName = "C:/tmp/prelert/config/jdbc.properties";
		try
		{
			LocalPropertiesConfigurationProvider propertiesProvider = 
				new LocalPropertiesConfigurationProvider(propsFileName);
			
			// Test out getting a property.
			String jdbcURL = propertiesProvider.getString("jdbc.url");
	        s_Logger.debug("jdbc.url : " + jdbcURL);
	        
	        // Test out setting a property, and saving back to file.
	        propertiesProvider.setProperty("jdbc.username", "postgres");
	        propertiesProvider.save();
		}
		catch (IOException e)
		{
			s_Logger.debug("Error carrying out test operations on " + 
					propsFileName + ": " + e.getMessage(), e);
		}
	}
}
