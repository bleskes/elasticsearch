/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
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

package com.prelert.data;

import java.util.Date;

import com.extjs.gxt.ui.client.data.DataProxy;
import com.extjs.gxt.ui.client.data.LoadEvent;


import com.prelert.client.ApplicationResponseHandler;
import com.prelert.service.GetEvidenceRecordsRpcProxy;


public class EvidenceViewPagingLoader<C extends EvidencePagingLoadConfig, D extends DatePagingLoadResult> 
	extends DatePagingLoader<C, D>
{
	private GetEvidenceRecordsRpcProxy m_Proxy;
	private String		m_FilterAttribute;
	private String		m_FilterValue;
	
	
	public EvidenceViewPagingLoader(GetEvidenceRecordsRpcProxy<C, D> proxy)
    {
	    super((DataProxy)proxy);
	    m_Proxy = proxy;
    }
	
	
	/**
     * @return the FilterAttribute
     */
    public String getFilterAttribute()
    {
    	return m_FilterAttribute;
    }


	/**
     * @param filterAttribute the FilterAttribute to set
     */
    public void setFilterAttribute(String filterAttribute)
    {
    	m_FilterAttribute = filterAttribute;
    }


	/**
     * @return the Filter Value
     */
    public String getFilterValue()
    {
    	return m_FilterValue;
    }


	/**
     * @param filterValue the Filter Value to set
     */
    public void setFilterValue(String filterValue)
    {
    	m_FilterValue = filterValue;
    }


	/**
	 * Refreshes the data, loading up the first (most recent) page of evidence data.
	 */
    public boolean load()
    {
	    return super.load();
    }
    
	
	public void loadFirstPage(Date date, TimeFrame timeFrame)
	{
		setDate(date);
		setTimeFrame(timeFrame);
		
		C config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final C loadConfig = config;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<D> callback = 
				new ApplicationResponseHandler<D>()
			{
				public void uponFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void uponSuccess(D result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};

			m_Proxy.loadFirstPage(loadConfig, callback);
		}
	}
	
	
	public void loadLastPage(Date date, TimeFrame timeFrame)
	{
		setDate(date);
		setTimeFrame(timeFrame);
		
		C config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final C loadConfig = config;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<D> callback = 
				new ApplicationResponseHandler<D>()
			{
				public void uponFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void uponSuccess(D result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};

			m_Proxy.loadLastPage(loadConfig, callback);
		}
	}
	
	
	public void loadNextPage(Date bottomRowDate, String bottomRowId, TimeFrame timeFrame)
	{
		setDate(bottomRowDate);
		setTimeFrame(timeFrame);
		
		C config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final C loadConfig = config;
		final String rowId = bottomRowId;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<D> callback = 
				new ApplicationResponseHandler<D>()
			{
				public void uponFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void uponSuccess(D result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};

			m_Proxy.loadNextPage(loadConfig, rowId, callback);
		}
	}
	
	
	public void loadPreviousPage(Date topRowDate, String topRowId, TimeFrame timeFrame)
	{
		setDate(topRowDate);
		setTimeFrame(timeFrame);
		
		C config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final C loadConfig = config;
		final String rowId = topRowId;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<D> callback = 
				new ApplicationResponseHandler<D>()
			{
				public void uponFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void uponSuccess(D result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};

			m_Proxy.loadPreviousPage(loadConfig, rowId, callback);
		}
	}
	
	
	public void loadAtTime(Date date, TimeFrame timeFrame)
	{
		setDate(date);
		setTimeFrame(timeFrame);
		
		C config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final C loadConfig = config;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<D> callback = 
				new ApplicationResponseHandler<D>()
			{
				public void uponFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void uponSuccess(D result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};

			m_Proxy.loadAtTime(loadConfig, callback);
		}
	}
	
	
    /**
     * Loads evidence data at the supplied time for the specified time frame.
     * @param date (ignored)
	 * @param evidenceId id for the top row of evidence data to be loaded. 
     * @param timeFrame time frame of data to be loaded e.g. day, hour, minute or second.
     */
	public void loadAtId(int evidenceId, TimeFrame timeFrame)
	{
		setTimeFrame(timeFrame);
		
		C config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final C loadConfig = config;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<D> callback = 
				new ApplicationResponseHandler<D>()
			{
				public void uponFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void uponSuccess(D result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};

			m_Proxy.loadAtId(loadConfig, evidenceId, callback);
		}
	}
	
	
	public void loadForDescription(Date date, String description, TimeFrame timeFrame)
	{
		setDate(date);
		setTimeFrame(timeFrame);
		
		C config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final C loadConfig = config;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<D> callback = 
				new ApplicationResponseHandler<D>()
			{
				public void uponFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void uponSuccess(D result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};

			m_Proxy.loadForDescription(loadConfig, description, callback);
		}
	}
	
	
	/**
	 * Use the specified LoadConfig for all load calls. The {@link #reuseConfig}
	 * will be set to true.
	 */
	public void useLoadConfig(C loadConfig)
	{
		super.useLoadConfig(loadConfig);

		m_FilterAttribute = loadConfig.getFilterAttribute();
		m_FilterValue = loadConfig.getFilterValue();
	}


	/**
	 * Template method to allow custom BaseLoader subclasses to provide their
	 * own implementation of LoadConfig
	 */
	protected C newLoadConfig()
	{
		return (C) new EvidencePagingLoadConfig();
	}


	/**
	 * Template method to allow custom subclasses to prepare the load config
	 * prior to loading data
	 */
	protected C prepareLoadConfig(C config)
	{
		super.prepareLoadConfig(config);
		
		config.setFilterAttribute(m_FilterAttribute);
		config.setFilterValue(m_FilterValue);
		
		return config;
	}

}
