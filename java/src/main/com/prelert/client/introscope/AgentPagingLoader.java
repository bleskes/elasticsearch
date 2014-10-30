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

package com.prelert.client.introscope;

import com.extjs.gxt.ui.client.data.BasePagingLoader;
import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.prelert.client.ApplicationResponseHandler;

import com.prelert.data.gxt.AttributeModel;
import com.prelert.data.gxt.DataTypeConnectionModel;


/**
 * Extension of the GXT <code>BasePagingLoader</code> for paging through 
 * the list of available Introscope agents, adding a property for search text
 * contained within the agent name.
 * @author Pete Harverson
 */
public class AgentPagingLoader extends BasePagingLoader<PagingLoadResult<AttributeModel>>
{
	private DataTypeConnectionModel	m_ConnectionConfig;
	private String	m_ContainsText;
	
	
	/**
	 * Creates a new agent paging loader instance with the given RPC data proxy.
	 * @param proxy the data proxy used for loading a page of agents from the server.
	 */
	public AgentPagingLoader(AgentPagingRpcProxy proxy)
    {
	    super(proxy);
    }
	
	
	/**
     * Sets the configuration parameters for the connection to the EM for
     * which agents are being obtained.
     * @param connectionModel <code>DataTypeConnectionModel</code> encapsulating the 
	 * 	configuration properties of the connection to Introscope.
     */
    public void setConnectionConfig(DataTypeConnectionModel connectionModel)
    {
    	m_ConnectionConfig = connectionModel;
    }
    
    
    /**
     * Returns the configuration parameters for the connection to the EM for
     * which agents are being obtained.
     * @return <code>DataTypeConnectionModel</code> encapsulating the 
	 * 	configuration properties of the connection to Introscope.
     */
    public DataTypeConnectionModel getConnectionConfig()
    {
    	return m_ConnectionConfig;
    }
	
	
	/**
     * Sets a String to search for, using a case-insensitive match, within the agent name.
     * @param text the text to search for within the agent name. 
     * 		If <code>null</code> all agents will be loaded.
     */
	public void setContainsText(String text)
    {
		m_ContainsText = text;
    }
    
    
	/**
     * Returns the optional String to search for, using a case-insensitive match, 
     * within the agent name.
     * @return the text to search for within the agent name. If <code>null</code>
     * 		all agents will be loaded.
     */
    public String getContainsText()
    {
    	return m_ContainsText;
    }

    
    @Override
    protected Object newLoadConfig()
    {
	    return new AgentPagingLoadConfig();
    }


    @Override
    protected Object prepareLoadConfig(Object config)
    {
    	super.prepareLoadConfig(config);
    	
    	AgentPagingLoadConfig loadConfig = (AgentPagingLoadConfig)config;
    	loadConfig.setConnectionConfig(m_ConnectionConfig);
    	loadConfig.setContainsText(m_ContainsText);
    	
    	return loadConfig;
    }

	
    @Override
    public void useLoadConfig(Object loadConfig)
    {
	    super.useLoadConfig(loadConfig);
	    
	    AgentPagingLoadConfig pagingLoadConfig = (AgentPagingLoadConfig)loadConfig;
	    m_ConnectionConfig = pagingLoadConfig.getConnectionConfig();
	    m_ContainsText = pagingLoadConfig.getContainsText();
    }
    

    @Override
    protected void loadData(final Object config)
    {
    	ApplicationResponseHandler<PagingLoadResult<AttributeModel>> callback = 
			new ApplicationResponseHandler<PagingLoadResult<AttributeModel>>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				onLoadFailure(config, caught);
			}


			@Override
            public void uponSuccess(PagingLoadResult<AttributeModel> result)
			{
				onLoadSuccess(config, result);
			}
		};

		proxy.load(reader, config, callback);
    }
}
