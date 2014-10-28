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
import org.apache.commons.configuration.FileConfiguration;
import org.apache.log4j.Logger;


/**
 * Partial implementation of the <code>ConfigurationProvider</code> interface which
 * provides base functionality for accessing configuration data stored in a file.
 * @author Pete Harverson
 */
public abstract class AbstractFileConfigurationProvider implements ConfigurationProvider
{
	protected static Logger s_Logger;
	protected FileConfiguration m_Config;
	

	@Override
	public String getString(String key)
	{
		return m_Config.getString(key);
	}
	
	
	@Override
	public void setProperty(String key, Object value)
	{
		m_Config.setProperty(key, value);
	}
	
	
	@Override
	public void save() throws IOException
	{
		save(m_Config.getFileName());
	}
	
	
    @Override
    public void save(String fileName) throws IOException
    {
    	try
        {
            m_Config.save(fileName);
        }
        catch (ConfigurationException e)
        {
            throw new IOException("Error saving configuration to file: " + fileName + 
            		e.getMessage(), e);
        }
    }

}
