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

package com.prelert.client.incident;


import java.util.List;

import com.extjs.gxt.ui.client.data.BasePagingLoader;
import com.extjs.gxt.ui.client.data.PagingLoadResult;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.data.Attribute;
import com.prelert.data.gxt.CausalityDataModel;
import com.prelert.data.gxt.CausalityDataPagingLoadConfig;


/**
 * Loader for a grid of CausalityDataModel, used for paging through the sets of
 * notifications and time series features which have been causally related
 * across metrics or resources. It adds support for setting the id of an item of evidence
 * from the aggregated set of data, and primary and secondary attributes by which the 
 * causality data should be filtered e.g. type='p2pslog' and source='lon-data01'.
 * @author Pete Harverson
 */
public class CausalityDataPagingLoader extends BasePagingLoader<PagingLoadResult<CausalityDataModel>>
{
	private int m_EvidenceId = -1;
	
	private List<String> m_ReturnAttributes;
	
	private List<Attribute> m_PrimaryFilterAttributes;
	
	private String m_SecondaryFilterName;
	private String m_SecondaryFilterValue;

	
	/**
	 * Creates a new loader for paging through a grid of CausalityDataModel objects.
	 */
	public CausalityDataPagingLoader()
    {
	    super(new CausalityDataPagingRpcProxy());
    }
	
	
	/**
	 * Sets the id of an item of evidence from the aggregated set of causality
	 * data for which related items are being loaded.
	 * @param evidenceId the id of an item of evidence from the aggregated causality data.
	 */
	public void setEvidenceId(int evidenceId)
	{
		m_EvidenceId = evidenceId;
	}
	
	
	/**
	 * Returns the id of an item of evidence from the aggregated set of causality
	 * data for which related items are being loaded.
	 * @return the id of an item of evidence from the aggregated causality data.
	 */
	public int getEvidenceId()
	{
		return m_EvidenceId;
	}
	
	
	/**
	 * Returns a list of the names of the attributes that should be included in 
	 * the load results.
     * @return  list of the names of any additional attributes to be included 
     * 	in the output, or <code>null</code> to return no extra attributes.
     */
    public List<String> getReturnAttributes()
    {
    	return m_ReturnAttributes;
    }


	/**
	 * Sets an optional list of the names of the attributes that should be included in 
	 * the load results.
     * @param returnAttributes list of the names of any additional attributes to be included 
     * 	in the output, or <code>null</code> to return no extra attributes.
     */
    public void setReturnAttributes(List<String> returnAttributes)
    {
    	m_ReturnAttributes = returnAttributes;
    }


	/**
	 * Sets the list of attributes to use as the primary filter. Attribute values 
	 * may be either non-<code>null</code> or <code>null</code>.
	 * @param attributes list of primary filter attributes.
	 */
	public void setPrimaryFilterAttributes(List<Attribute> attributes)
    {
		m_PrimaryFilterAttributes = attributes;
    }
    
    
	/**
	 * Returns the list of attributes to use as the primary filter. Attribute values 
	 * may be either non-<code>null</code> or <code>null</code>.
	 * @return list of primary filter attributes.
	 */
    public List<Attribute> getPrimaryFilterAttributes()
    {
    	return m_PrimaryFilterAttributes;
    }
    
    
    /**
	 * Optionally sets the name of the attribute to use as the secondary filter.
	 * @param attributeName name of the secondary filter attribute. If <code>null</code>
	 * 	then no secondary filter will be applied.
	 */
	public void setSecondaryFilterName(String attributeName)
    {
		m_SecondaryFilterName = attributeName;
    }
    
    
	/**
	 * Returns the name of the attribute to use as the secondary filter.
	 * @return attributeName name of the secondary filter attribute. If <code>null</code>
	 * 	then no secondary filter will be applied.
	 */
    public String getSecondaryFilterName()
    {
    	return m_SecondaryFilterName;
    }
    
    
    /**
     * Sets the value of the optional secondary filter attribute.
     * @param attributeValue value of the secondary filter attribute. If <code>null</code>
	 * 	then no secondary filter will be applied.
     */
    public void setSecondaryFilterValue(String attributeValue)
    {
    	m_SecondaryFilterValue = attributeValue;
    }
    
    
    /**
     * Returns the value of the optional secondary filter attribute.
     * @return value of the secondary filter attribute. If <code>null</code>
	 * 	then no secondary filter will be applied.
     */
    public String getSecondaryFilterValue()
    {
    	return m_SecondaryFilterValue;
    }
	

    @Override
    protected void loadData(final Object config)
    {
    	ApplicationResponseHandler<PagingLoadResult<CausalityDataModel>> callback = 
			new ApplicationResponseHandler<PagingLoadResult<CausalityDataModel>>()
		{
			public void uponFailure(Throwable caught)
			{
				onLoadFailure(config, caught);
			}


			public void uponSuccess(PagingLoadResult<CausalityDataModel> result)
			{
				onLoadSuccess(config, result);
			}
		};

		proxy.load(reader, config, callback);
    }


	@Override
    protected Object newLoadConfig()
    {
	    return new CausalityDataPagingLoadConfig();
    }


    @Override
    protected Object prepareLoadConfig(Object config)
    {
	    super.prepareLoadConfig(config);
	    
	    CausalityDataPagingLoadConfig loadConfig = (CausalityDataPagingLoadConfig)config;
	    loadConfig.setEvidenceId(m_EvidenceId);
	    loadConfig.setReturnAttributes(m_ReturnAttributes);
	    loadConfig.setPrimaryFilterAttributes(m_PrimaryFilterAttributes);
	    loadConfig.setSecondaryFilterName(m_SecondaryFilterName);
	    loadConfig.setSecondaryFilterValue(m_SecondaryFilterValue);
	    
	    return loadConfig;
    }


    @Override
    public void useLoadConfig(Object loadConfig)
    {
	    super.useLoadConfig(loadConfig);
	    
	    CausalityDataPagingLoadConfig pagingLoadConfig = (CausalityDataPagingLoadConfig)loadConfig;
	    m_EvidenceId = pagingLoadConfig.getEvidenceId();
	    m_ReturnAttributes = pagingLoadConfig.getReturnAttributes();
	    m_PrimaryFilterAttributes = pagingLoadConfig.getPrimaryFilterAttributes();
	    m_SecondaryFilterName = pagingLoadConfig.getSecondaryFilterName();
	    m_SecondaryFilterValue = pagingLoadConfig.getSecondaryFilterValue();
    }
    

}
