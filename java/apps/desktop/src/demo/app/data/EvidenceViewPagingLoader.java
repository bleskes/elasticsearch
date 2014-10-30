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

package demo.app.data;

import java.util.Date;

import com.extjs.gxt.ui.client.data.DataProxy;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.google.gwt.core.client.GWT;

import demo.app.client.ApplicationResponseHandler;
import demo.app.data.gxt.EvidenceModel;
import demo.app.service.GetEvidenceDataRpcProxy;


public class EvidenceViewPagingLoader<D extends DatePagingLoadResult> 
	extends DatePagingLoader
{
	private GetEvidenceDataRpcProxy<D> m_Proxy;
	private String		m_Source;
	private String		m_FilterAttribute;
	private String		m_FilterValue;
		
	
	public EvidenceViewPagingLoader(GetEvidenceDataRpcProxy<D> proxy)
    {
		super((DataProxy<D>) proxy);
	    m_Proxy = proxy;
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
	 * Returns the name of the attribute that the evidence list is filtered on.
     * @return the filter attribute, or <code>null</code> if the view is not filtered.
     */
    public String getFilterAttribute()
    {
    	return m_FilterAttribute;
    }


	/**
	 * Sets the name of the attribute that the evidence list is filtered on.
     * @param filterAttribute the filter attribute, or <code>null</code> 
     * 		if the view is not filtered.
     */
    public void setFilterAttribute(String filterAttribute)
    {
    	m_FilterAttribute = filterAttribute;
    }


	/**
	 * Returns the value of the attribute that the evidence list is filtered on.
     * @return the filter value, or <code>null</code> if the view is not filtered.
     */
    public String getFilterValue()
    {
    	return m_FilterValue;
    }


	/**
	 * Sets the value of the attribute that the evidence list is filtered on.
     * @param filterValue the filter value, or <code>null</code> 
     * 		if the view is not filtered.
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
    
	
    /**
     * Loads the first page of evidence data for the specified time frame.
     * @param date
     * @param timeFrame time frame of data to be loaded e.g. day, hour, minute or second.
     */
	public void loadFirstPage(Date date, TimeFrame timeFrame)
	{
		setDate(date);
		setTimeFrame(timeFrame);
		
		Object config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final Object loadConfig = config;
		
		GWT.log("++++ EvidenceViewPagingLoader.loadFirstPage() source: " +
				((EvidencePagingLoadConfig)loadConfig).getSource(), null);
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<DatePagingLoadResult<EvidenceModel>> callback = 
				new ApplicationResponseHandler<DatePagingLoadResult<EvidenceModel>>()
			{
				public void uponFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void uponSuccess(DatePagingLoadResult<EvidenceModel> result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};
			
			m_Proxy.loadFirstPage((EvidencePagingLoadConfig)loadConfig, callback);
		}
	}
	
	
    /**
     * Loads the last page of evidence data for the specified time frame.
     * @param date
     * @param timeFrame time frame of data to be loaded e.g. day, hour, minute or second.
     */
	public void loadLastPage(Date date, TimeFrame timeFrame)
	{
		setDate(date);
		setTimeFrame(timeFrame);
		
		Object config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final Object loadConfig = config;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<DatePagingLoadResult<EvidenceModel>> callback = 
				new ApplicationResponseHandler<DatePagingLoadResult<EvidenceModel>>()
			{
				public void uponFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void uponSuccess(DatePagingLoadResult<EvidenceModel> result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};

			m_Proxy.loadLastPage((EvidencePagingLoadConfig)loadConfig, callback);
		}
	}
	
	
    /**
     * Loads the next page of evidence data for the specified time frame following
     * on from the item of evidence data with specified date and id.
     * @param bottomRowDate date of the bottom row of evidence data in the current page.
     * @param bottomRowId id of the bottom row of evidence data in the current page.
     * @param timeFrame time frame of data to be loaded e.g. day, hour, minute or second.
     */
	public void loadNextPage(Date bottomRowDate, String bottomRowId, TimeFrame timeFrame)
	{
		setDate(bottomRowDate);
		setTimeFrame(timeFrame);
		
		Object config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final Object loadConfig = config;
		final String rowId = bottomRowId;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<DatePagingLoadResult<EvidenceModel>> callback = 
				new ApplicationResponseHandler<DatePagingLoadResult<EvidenceModel>>()
			{
				public void uponFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void uponSuccess(DatePagingLoadResult<EvidenceModel> result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};

			m_Proxy.loadNextPage((EvidencePagingLoadConfig)loadConfig, rowId, callback);
		}
	}
	
	
    /**
     * Loads the previous page of evidence data for the specified time frame before
     * the item of evidence data with specified date and id.
     * @param topRowDate date of the top row of evidence data in the current page.
     * @param topRowId id of the top row of evidence data in the current page.
     * @param timeFrame time frame of data to be loaded e.g. day, hour, minute or second.
     */
	public void loadPreviousPage(Date topRowDate, String topRowId, TimeFrame timeFrame)
	{
		setDate(topRowDate);
		setTimeFrame(timeFrame);
		
		Object config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final Object loadConfig = config;
		final String rowId = topRowId;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<DatePagingLoadResult<EvidenceModel>> callback = 
				new ApplicationResponseHandler<DatePagingLoadResult<EvidenceModel>>()
			{
				public void uponFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void uponSuccess(DatePagingLoadResult<EvidenceModel> result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};

			m_Proxy.loadPreviousPage((EvidencePagingLoadConfig)loadConfig, rowId, callback);
		}
	}
	
	
    /**
     * Loads evidence data at the supplied time for the specified time frame.
     * @param date date of the evidence data to load.
     * @param timeFrame time frame of data to be loaded e.g. day, hour, minute or second.
     */
	public void loadAtTime(Date date, TimeFrame timeFrame)
	{
		setDate(date);
		setTimeFrame(timeFrame);
		
		Object config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final Object loadConfig = config;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<DatePagingLoadResult<EvidenceModel>> callback = 
				new ApplicationResponseHandler<DatePagingLoadResult<EvidenceModel>>()
			{
				public void uponFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void uponSuccess(DatePagingLoadResult<EvidenceModel> result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};

			m_Proxy.loadAtTime((EvidencePagingLoadConfig)loadConfig, callback);
		}
	}
	
	
    /**
     * Loads evidence data starting at the supplied id for the specified time frame.
	 * @param evidenceId id for the top row of evidence data to be loaded. 
     * @param timeFrame time frame of data to be loaded e.g. day, hour, minute or second.
     */
	public void loadAtId(int evidenceId, TimeFrame timeFrame)
	{
		setTimeFrame(timeFrame);
		
		Object config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final Object loadConfig = config;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<DatePagingLoadResult<EvidenceModel>> callback = 
				new ApplicationResponseHandler<DatePagingLoadResult<EvidenceModel>>()
			{
				public void uponFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void uponSuccess(DatePagingLoadResult<EvidenceModel> result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};

			m_Proxy.loadAtId((EvidencePagingLoadConfig)loadConfig, evidenceId, callback);
		}
	}
	
	
    /**
     * Loads evidence data matching the supplied time and description for
     * the specified time frame.
     * @param date date of the evidence data to load.
     * @param description evidence description to match on for the first row of
     * data to load.
     * @param timeFrame time frame of data to be loaded e.g. day, hour, minute or second.
     */
	public void loadForDescription(Date date, String description, TimeFrame timeFrame)
	{
		setDate(date);
		setTimeFrame(timeFrame);
		
		Object config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final Object loadConfig = config;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<DatePagingLoadResult<EvidenceModel>> callback = 
				new ApplicationResponseHandler<DatePagingLoadResult<EvidenceModel>>()
			{
				public void uponFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void uponSuccess(DatePagingLoadResult<EvidenceModel> result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};

			m_Proxy.loadForDescription((EvidencePagingLoadConfig)loadConfig, description, callback);
		}
	}
	
	
	/**
	 * Use the specified LoadConfig for all load calls. The {@link #reuseConfig}
	 * will be set to true.
	 */
	public void useLoadConfig(Object loadConfig)
	{
		super.useLoadConfig(loadConfig);
		
		EvidencePagingLoadConfig pagingLoadConfig = (EvidencePagingLoadConfig)loadConfig;
		
		m_Source = pagingLoadConfig.getSource();
		m_FilterAttribute = pagingLoadConfig.getFilterAttribute();
		m_FilterValue = pagingLoadConfig.getFilterValue();
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

		pagingLoadConfig.setSource(m_Source);
		pagingLoadConfig.setFilterAttribute(m_FilterAttribute);
		pagingLoadConfig.setFilterValue(m_FilterValue);
		
		return config;
	}

}
