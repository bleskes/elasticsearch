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

package com.prelert.client.list;

import java.util.ArrayList;
import java.util.List;

import com.prelert.data.Attribute;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.data.gxt.EvidencePagingLoadConfig;


/**
 * Extension of ModelDatePagingLoader for paging through evidence data, adding
 * an data type, source and filter properties.
 * @author Pete Harverson
 */
public class EvidencePagingLoader extends ModelDatePagingLoader<EvidenceModel>
{
	private String		m_DataType;
	private String		m_Source;
	private List<Attribute>	m_FilterAttributes;
	private String		m_ContainsText;
		
	
	/**
	 * Creates a paging loader for a grid of evidence data.
	 * @param proxy proxy retrieving the evidence data from the server via GWT RPC.
	 */
	public EvidencePagingLoader(ModelDatePagingRpcProxy<EvidenceModel> proxy)
    {
		super(proxy);
		setRemoteSort(true);
    }
	
	
	/**
	 * Returns the data type, such as 'p2psmon_users' or 'system_udp', which is used
	 * to identify the particular type of data being loaded.
	 * @return the data type.
	 */
	public String getDataType()
    {
    	return m_DataType;
    }


	/**
	 * Sets the data type, such as 'p2psmon_users' or 'system_udp', which is used
	 * to identify the particular type of data being loaded.
	 * @param dataType the data type.
	 */
	public void setDataType(String dataType)
    {
		m_DataType =  dataType;
    }
	
	
	/**
	 * Returns the name of the source (server) for the time series.
	 * @return the name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 */
	public String getSource()
	{
		return m_Source;
	}
	
	
	/**
	 * Sets the name of the source (server) for the evidence data.
	 * @param source the name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 */
	public void setSource(String source)
	{
		m_Source = source;
	}
	
	
	/**
	 * Returns the list of attributes that the evidence is filtered on.
     * @return list of filter attributes, or <code>null</code> if the 
     * 		view is not filtered.
     */
    public List<Attribute> getFilter()
    {
    	return m_FilterAttributes;
    }
    
    
    /**
	 * Sets the value of the filter attribute with the specified name. If a filter attribute
	 * with that name does not already exist, the attribute will be added. A separate 
	 * call should be made to reload data into the window following the call to this method.
	 * @param attributeName 	filter attribute name to set.	
	 * @param attributeValue	filter attribute value.
	 */
    public void setFilterAttribute(String attributeName, String attributeValue)
    {
    	boolean inList = false;
    	
    	if (m_FilterAttributes != null)
    	{
    		for (Attribute attribute : m_FilterAttributes)
    		{
    			if (attribute.getAttributeName().equals(attributeName))
    			{
    				attribute.setAttributeValue(attributeValue);
    				inList = true;
    				break;
    			}
    		}
    	}
    	
    	if (inList == false)
    	{
    		if (m_FilterAttributes == null)
        	{
        		m_FilterAttributes = new ArrayList<Attribute>(); 
        		
        	}
    		m_FilterAttributes.add(new Attribute(attributeName, attributeValue));
    	}
    }


	/**
	 * Sets the list of attributes that the evidence is filtered on.
     * @param filterAttributes list of filter attributes, or <code>null</code> 
     * 		if the view is not filtered.
     */
    public void setFilter(List<Attribute> filterAttributes)
    {
    	m_FilterAttributes = filterAttributes;
    }
    
    
    /**
     * Clears any existing filter on the evidence data.
     */
    public void clearFilter()
    {
    	m_FilterAttributes = null;
    }
    
    
    /**
     * Returns the String, if any, that is contained within one or more of the
     * evidence attribute values.
     * @return the text contained within attribute values, or <code>null</code> 
     * 		if no search text is specified.
     */
    public String getContainsText()
    {
    	return m_ContainsText;
    }
    
    
    /**
     * Sets the String to search for within one or more of the evidence 
     * attribute values.
     * @param containsText the text to search for within attribute values.
     */
    public void setContainsText(String containsText)
    {
    	m_ContainsText = containsText;
    }
	
	
	/**
	 * Use the specified LoadConfig for all load calls. The {@link #reuseConfig}
	 * will be set to true.
	 */
	public void useLoadConfig(Object loadConfig)
	{
		super.useLoadConfig(loadConfig);
		
		EvidencePagingLoadConfig pagingLoadConfig = (EvidencePagingLoadConfig)loadConfig;
		
		m_DataType = pagingLoadConfig.getDataType();
		m_Source = pagingLoadConfig.getSource();
		
		List<String> attributeNames = pagingLoadConfig.getFilterAttributes();
		List<String> attributeValues = pagingLoadConfig.getFilterValues();
		if (attributeNames != null && attributeValues != null && 
				attributeNames.size() > 0 && 
				(attributeNames.size() == attributeValues.size()) )
		{
			int numAttributes = attributeNames.size();
			m_FilterAttributes = new ArrayList<Attribute>(numAttributes);
			for (int i = 0; i < numAttributes; i++)
			{
				m_FilterAttributes.add(new Attribute(
						attributeNames.get(i), attributeValues.get(i)));
			}
		}
		else
		{
			m_FilterAttributes = null;
		}
		
		m_ContainsText = pagingLoadConfig.getContainsText();
	}
	

	/**
	 * Template method to allow custom BaseLoader subclasses to provide their
	 * own implementation of LoadConfig
	 */
	protected Object newLoadConfig()
	{
		return new EvidencePagingLoadConfig();
	}


	/**
	 * Template method to allow custom subclasses to prepare the load config
	 * prior to loading data
	 */
	protected Object prepareLoadConfig(Object config)
	{
		super.prepareLoadConfig(config);
		
		EvidencePagingLoadConfig pagingLoadConfig = (EvidencePagingLoadConfig)config;

		pagingLoadConfig.setDataType(m_DataType);
		pagingLoadConfig.setSource(m_Source);
		
		List<String> attributeNames = null;
		List<String> attributeValues = null;
		String attributeName;
		String attributeValue;
		if (m_FilterAttributes != null)
		{
			attributeNames = new ArrayList<String>(m_FilterAttributes.size());
			attributeValues = new ArrayList<String>(m_FilterAttributes.size());
			for (Attribute attribute : m_FilterAttributes)
			{
				attributeName = attribute.getAttributeName();
				attributeValue = attribute.getAttributeValue();
				if ( (attributeName != null) && (attributeValue != null) )
				{
					// Only add in if both name and value are null.
					attributeNames.add(attribute.getAttributeName());
					attributeValues.add(attribute.getAttributeValue());
				}
			}
		}
		
		pagingLoadConfig.setFilterAttributes(attributeNames);
		pagingLoadConfig.setFilterValues(attributeValues);
		pagingLoadConfig.setContainsText(m_ContainsText);
		
		return config;
	}

}
