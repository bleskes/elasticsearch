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

package com.prelert.client.explorer;

import com.extjs.gxt.ui.client.data.BaseListLoadResult;
import com.extjs.gxt.ui.client.data.BaseListLoader;
import com.extjs.gxt.ui.client.data.LoadEvent;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.data.gxt.MetricPathListLoadConfig;
import com.prelert.data.gxt.MetricTreeNodeModel;


/**
 * Extension of the GXT <code>BaseListLoader</code> for loading a level of data
 * from the metric path tree for display in a list widget.
 * @author Pete Harverson
 */
public class MetricPathListLoader extends BaseListLoader<BaseListLoadResult<MetricTreeNodeModel>>
{
	private MetricPathListRpcProxy m_Proxy;
	
	private String 	m_Type;
	private String	m_PreviousPath;
	private String 	m_CurrentValue;
	private int 	m_OpaqueNum;
	private String	m_OpaqueString;
	
	
	/**
	 * Creates a new MetricPathListLoader for loading a list of metric path data.
	 */
	public MetricPathListLoader()
    {
	    super(new MetricPathListRpcProxy());
	    m_Proxy = (MetricPathListRpcProxy)proxy;
    }


	/**
	 * Returns the type of data being loaded.
     * @return the data type, or <code>null</code> if loading the top level of the
     * 	metric path tree.
     */
    public String getType()
    {
    	return m_Type;
    }


	/**
	 * Sets the type of data to be loaded.
     * @param type the data type, or <code>null</code> if loading the top level of the
     * 	metric path tree.
     */
    public void setType(String type)
    {
    	m_Type = type;
    }


	/**
	 * Returns the full metric path of the level above that being loaded.
     * @return the previous path to the level being loaded, or <code>null</code> 
     * 	if loading the top level of the metric path tree.
     */
    public String getPreviousPath()
    {
    	return m_PreviousPath;
    }


	/**
	 * Sets the full metric path of the level above that being loaded.
     * @param previousPath the previous path to the level being loaded, 
     * 	or <code>null</code> if loading the top level of the metric path tree.
     */
    public void setPreviousPath(String previousPath)
    {
    	m_PreviousPath = previousPath;
    }


	/**
	 * Returns the value of the attribute of the current level to be loaded,
	 * such as a type, source or attribute value.
     * @return the partial path value at the current level to load, or <code>null</code> 
     * 	if loading the top level of the metric path tree.
     */
    public String getCurrentValue()
    {
    	return m_CurrentValue;
    }


	/**
	 * Sets the value of the attribute of the current level to be loaded,
	 * such as a type, source or attribute value.
     * @param currentValue the partial path value at the current level to load, 
     * 	or <code>null</code> if loading the top level of the metric path tree.
     */
    public void setCurrentValue(String currentValue)
    {
    	m_CurrentValue = currentValue;
    }


	/**
	 * Returns the opaque integer ID representing the level being loaded, used 
	 * by some external plugins to obtain metric path data.
     * @return numeric ID, or 0 if loading the top level of the tree.
     */
    public int getOpaqueNum()
    {
    	return m_OpaqueNum;
    }


	/**
	 * Sets the opaque integer ID representing the level being loaded, used 
	 * by some external plugins to obtain metric path data.
     * @param opaqueNum numeric ID, or 0 if loading the top level of the tree.
     */
    public void setOpaqueNum(int opaqueNum)
    {
    	m_OpaqueNum = opaqueNum;
    }


	/**
	 * Returns the value of the textual field holding the GUID for the level being 
	 * loaded, used by some external plugins to obtain metric path data.
     * @return opaque textual GUID, or <code>null</code> if loading the top level 
     * 	of the metric path tree.
     */
    public String getOpaqueString()
    {
    	return m_OpaqueString;
    }


	/**
	 * Sets the value of the textual field holding the GUID for the level being 
	 * loaded, used by some external plugins to obtain metric path data.
     * @param opaqueString opaque textual GUID, or <code>null</code> if loading 
     * 	the top level of the metric path tree.
     */
    public void setOpaqueString(String opaqueString)
    {
    	m_OpaqueString = opaqueString;
    }

	
    /**
	 * Loads the next level in the metric path tree according to the current configuration.
	 */
	public void loadNextLevel()
    {
		Object config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, config)))
		{
			lastConfig = config;		
			MetricPathListLoadConfig loadConfig = (MetricPathListLoadConfig)config;
			m_Proxy.loadNextLevel(loadConfig, new MetricPathHandler(loadConfig));
		}
    }
	
	
	/**
	 * Loads the next level in the metric path tree according to the current configuration.
	 */
	public void loadPreviousLevel()
    {
		Object config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, config)))
		{
			lastConfig = config;		
			MetricPathListLoadConfig loadConfig = (MetricPathListLoadConfig)config;
			m_Proxy.loadPreviousLevel(loadConfig, new MetricPathHandler(loadConfig));
		}
    }
	
	
	/**
	 * Loads the current level in the metric path tree according to the current configuration.
	 */
	public void loadCurrentLevel()
    {
		Object config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, config)))
		{
			lastConfig = config;		
			MetricPathListLoadConfig loadConfig = (MetricPathListLoadConfig)config;
			m_Proxy.loadCurrentLevel(loadConfig, new MetricPathHandler(loadConfig));
		}
    }
	
	
	@Override
    protected Object newLoadConfig()
    {
	    return new MetricPathListLoadConfig();
    }


    @Override
    protected Object prepareLoadConfig(Object config)
    {
	    super.prepareLoadConfig(config);
	    
	    MetricPathListLoadConfig loadConfig = (MetricPathListLoadConfig)config;
	    loadConfig.setType(m_Type);
	    loadConfig.setPreviousPath(m_PreviousPath);
	    loadConfig.setCurrentValue(m_CurrentValue);
	    loadConfig.setOpaqueNum(m_OpaqueNum);
	    loadConfig.setOpaqueString(m_OpaqueString);
	    
	    return loadConfig;
    }


    @Override
    public void useLoadConfig(Object loadConfig)
    {
	    super.useLoadConfig(loadConfig);
	    
	    MetricPathListLoadConfig listLoadConfig = (MetricPathListLoadConfig)loadConfig;
		m_Type = listLoadConfig.getType();
		m_PreviousPath = listLoadConfig.getPreviousPath();
		m_CurrentValue = listLoadConfig.getCurrentValue();
		m_OpaqueNum = listLoadConfig.getOpaqueNum();
		m_OpaqueString = listLoadConfig.getOpaqueString();
    }
    
    
    /**
     * Response handler for queries to load data into the metric path tree.
     */
    class MetricPathHandler extends ApplicationResponseHandler<BaseListLoadResult<MetricTreeNodeModel>>
    {
    	private MetricPathListLoadConfig m_LoadConfig;
    	
    	
    	/**
    	 * Creates a response handler for loading metric path data for the specified
    	 * list load configuration.
    	 * @param loadConfig <code>MetricPathListLoadConfig</code> defining the level
    	 * 	at which data is being loaded.
    	 */
    	public MetricPathHandler(MetricPathListLoadConfig loadConfig)
    	{
    		m_LoadConfig = loadConfig;
    	}
    	
    	
    	@Override
        public void uponFailure(Throwable problem)
        {
        	onLoadFailure(m_LoadConfig, problem);
        }
    	
    	
        @Override
        public void uponSuccess(BaseListLoadResult<MetricTreeNodeModel> result)
        {
        	onLoadSuccess(m_LoadConfig, result);
        }  
    }
}
