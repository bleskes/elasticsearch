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

package com.prelert.proxy.inputmanager;


import com.prelert.data.TimeSeriesConfig;
import com.prelert.proxy.inputmanager.dao.InputManagerDAO;
import com.prelert.proxy.plugin.ExternalPlugin;
import com.prelert.proxy.plugin.Plugin;


/**
 * InputManager for External Plugins.
 */
public class InputManagerExternalTimeSeries extends InputManagerExternalPoints
{
	protected ExternalPlugin m_ExternalPlugin;
	

	public InputManagerExternalTimeSeries(InputManagerDAO inputManagerDAO)
	{
		super(inputManagerDAO);
	}
	
	
	/**
	 * If <code>plugin</code> does not implement the <code>ExternalPlugin</code>
	 * interface an UnsupportedOperationException is thrown.
	 * 
	 * @throws UnsupportedOperationException
	 */
	@Override 
	public void setPlugin(Plugin plugin)
	{
		super.setPlugin(plugin);
		
		if (plugin instanceof ExternalPlugin)
		{
			m_ExternalPlugin = (ExternalPlugin)plugin;
		}
		else
		{
			throw new UnsupportedOperationException("Plugin '" + plugin.getName() + 
						"' does not implement the ExternalPlugin interface. It cannot" +
						" be set on a External InputManager");
		}
	}
	
	
	/**
	 * Add the time series to the Prelert database.
	 */
	@Override
	protected int addExternalTimeSeries(TimeSeriesConfig config)
	{
		return m_InputManagerDAO.addExternalTimeSeries(config.getDataType(),
											config.getMetric(),
											config.getExternalKey());
	}
	

}
