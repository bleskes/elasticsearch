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

package com.prelert.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.springframework.util.DefaultPropertiesPersister;


/**
 * Extension of the Spring framework <code>DefaultPropertiesPersister</code>,
 * overriding the <code>loadFromXml(Properties props, InputStream is)</code> method
 * to use the Apache Commons Configuration <code>XMLConfiguration</code> class
 * to parse an XML document to a <code>Properties</code> object. The names of the 
 * attributes in the XML file will form the keys in the <code>Properties</code> object.
 * For example an XML file whose contents are:
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;dbconfig&gt;
 *   &lt;type&gt;postgres&lt;/type&gt;
 *   &lt;username&gt;pete&lt;/username&gt;
 *   &lt;password&gt;&lt;/password&gt;
 *   &lt;host&gt;/home/pete/prelert/cots/pgsql/data&lt;/host&gt;
 *   &lt;port&gt;5432&lt;/port&gt;
 *   &lt;dbname&gt;prelert&lt;/dbname&gt;
 * &lt;/dbconfig&gt;
 * </pre>
 * will result in a Properties object of {port=5432, password=, type=postgres, 
 * host=/home/pete/prelert/cots/pgsql/data, dbname=prelert, username=pete}
 * @author Pete Harverson
 */
public class XMLFilePropertiesPersister extends DefaultPropertiesPersister
{
	
	static Logger s_Logger = Logger.getLogger(XMLFilePropertiesPersister.class);
	

	@Override
	public void loadFromXml(Properties props, InputStream is)
			throws IOException
	{
		// Parse XML document to Properties using Apache Commons Configuration.
		XMLConfiguration xmlConfig = new XMLConfiguration();
		try
		{
			xmlConfig.load(is);
			
			Iterator<String> keys = xmlConfig.getKeys();
			String key;
			while (keys.hasNext())
			{
				key = keys.next();
				props.put(key, xmlConfig.getString(key));
			}
		} 
		catch (ConfigurationException e)
		{
			s_Logger.error("loadFromXml() error loading properties from inputstream", e);
		}
		
		s_Logger.debug("loadFromXml() loaded properties: " + props);
	}

}
