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

import java.util.Date;

import com.extjs.gxt.ui.client.data.BaseListLoader;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.data.ModelData;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.data.gxt.DatePagingLoadResult;
import com.prelert.data.gxt.ModelDatePagingLoadConfig;


/**
 * Extension of the default GXT list loader for paging through model
 * data ordered by time.
 * @author Pete Harverson
 * @param <M> the type of ModelData being loaded.
 */
public class ModelDatePagingLoader<M extends ModelData> 
	extends BaseListLoader<DatePagingLoadResult<M>>
{
	private ModelDatePagingRpcProxy<M> m_Proxy;
	
	private int		m_PageSize = 20;
	private Date	m_Time;
	private int		m_RowId;
	
	
	/**
	 * Creates a new loader instance with the given RPC data proxy.
	 * @param proxy the data proxy used for loading data from the server.
	 */
	public ModelDatePagingLoader(ModelDatePagingRpcProxy<M> proxy)
    {
	    super(proxy);
	    m_Proxy = proxy;
    }
	
	
	/**
     * Returns the page size - the maximum number of data items displayed in a page.
     * @return the page size, which has a default of 20.
     */
	public int getPageSize()
	{
		return m_PageSize;
	}
	
	
	/**
     * Sets the page size - the maximum number of data items displayed in a page.
     * @param pageSize the page size.
     */
	public void setPageSize(int pageSize)
	{
		m_PageSize = pageSize;
	}
	
	
	/**
     * Returns the value of the 'time' property, used by certain paging operations 
     * (e.g. next or previous) to determine the start or end point of data to return.
     * @return the date / time.
     */
	public Date getTime()
	{
		return m_Time;
	}
	
	
	/**
     * Sets the 'time' property, used by certain paging operations (e.g. next 
     * or previous) to determine the start or end point of data to return.
     * @param time the date / time.
     */
	public void setTime(Date time)
	{
		m_Time = time;
	}
	
	
	/**
     * Returns the 'rowId' property, used by certain paging operations (e.g. next 
     * or previous) to uniquely identify the start or end point of data to return
     * for cases where multiple data items may occur at the same time.
     * @return unique row identifier.
     */
	public int getRowId()
	{
		return m_RowId;
	}
	
	
	/**
     * Sets the 'rowId' property, used by certain paging operations (e.g. next 
     * or previous) to uniquely identify the start or end point of data to return
     * for cases where multiple data items may occur at the same time.
     * @param id unique row identifier.
     */
	public void setRowId(int id)
	{
		m_RowId = id;
	}
	
	
	/**
	 * Loads the first page of model data.
	 */
	public void loadFirstPage()
    {
		setTime(null);
		
		Object config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final Object loadConfig = config;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<DatePagingLoadResult<M>> callback = 
				new ApplicationResponseHandler<DatePagingLoadResult<M>>()
			{
				public void uponFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void uponSuccess(DatePagingLoadResult<M> result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};
			
			m_Proxy.loadFirstPage((ModelDatePagingLoadConfig)loadConfig, callback);
		}
    }
	
	
	/**
	 * Loads the last page of model data.
	 */
	public void loadLastPage()
    {
		Object config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final Object loadConfig = config;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<DatePagingLoadResult<M>> callback = 
				new ApplicationResponseHandler<DatePagingLoadResult<M>>()
			{
				public void uponFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void uponSuccess(DatePagingLoadResult<M> result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};
			
			m_Proxy.loadLastPage((ModelDatePagingLoadConfig)loadConfig, callback);
		}
    }
	
	
	/**
     * Loads the next page of model data following on from the model with the 
     * specified time and id.
     * @param bottomRowTime date/time of the bottom row of data in the current page.
     * @param bottomRowId id of the bottom row of data in the current page.
     */
	public void loadNextPage(Date bottomRowTime, int bottomRowId)
    {
		setTime(bottomRowTime);
		setRowId(bottomRowId);
		
		Object config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final Object loadConfig = config;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<DatePagingLoadResult<M>> callback = 
				new ApplicationResponseHandler<DatePagingLoadResult<M>>()
			{
				public void uponFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void uponSuccess(DatePagingLoadResult<M> result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};

			m_Proxy.loadNextPage((ModelDatePagingLoadConfig)loadConfig, callback);
		}
    }
	
	
	/**
	 * Loads the previous page of model data to the model with the specified time and id.
	 * @param topRowTime date/time of the top row of data in the current page.
	 * @param topRowId id of the top row of data in the current page.
	 */
	public void loadPreviousPage(Date topRowTime, int topRowId)
    {
		setTime(topRowTime);
		setRowId(topRowId);
		
		Object config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final Object loadConfig = config;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<DatePagingLoadResult<M>> callback = 
				new ApplicationResponseHandler<DatePagingLoadResult<M>>()
			{
				public void uponFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void uponSuccess(DatePagingLoadResult<M> result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};

			m_Proxy.loadPreviousPage((ModelDatePagingLoadConfig)loadConfig, callback);
		}
    }
	

	/**
	 * Loads a page of data, whose top row will be closest in time, up to or before,
     * the specified time.
	 * @param time time of data to load.
	 */
	public void loadAtTime(Date time)
    {
		if (time != null)
		{
			setTime(time);
			
			Object config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
			config = prepareLoadConfig(config);
			
			final Object loadConfig = config;
			
			if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
			{
				lastConfig = config;
				
				ApplicationResponseHandler<DatePagingLoadResult<M>> callback = 
					new ApplicationResponseHandler<DatePagingLoadResult<M>>()
				{
					public void uponFailure(Throwable caught)
					{
						onLoadFailure(loadConfig, caught);
					}
	
	
					public void uponSuccess(DatePagingLoadResult<M> result)
					{
						onLoadSuccess(loadConfig, result);
					}
				};
	
				m_Proxy.loadAtTime((ModelDatePagingLoadConfig)loadConfig, callback);
			}
		}
		else
		{
			loadFirstPage();
		}
    }


	@Override
	public void useLoadConfig(Object loadConfig)
	{
		super.useLoadConfig(loadConfig);
		
		ModelDatePagingLoadConfig pagingLoadConfig = (ModelDatePagingLoadConfig)loadConfig;
		
		m_PageSize = pagingLoadConfig.getPageSize();
		m_Time = pagingLoadConfig.getTime();
		m_RowId = pagingLoadConfig.getRowId();
	}
	

	@Override
	protected Object newLoadConfig()
	{
		return new ModelDatePagingLoadConfig();
	}


	@Override
	protected Object prepareLoadConfig(Object config)
	{
		super.prepareLoadConfig(config);
		
		ModelDatePagingLoadConfig pagingLoadConfig = (ModelDatePagingLoadConfig)config;

		pagingLoadConfig.setPageSize(m_PageSize);
		pagingLoadConfig.setTime(m_Time);
		pagingLoadConfig.setRowId(m_RowId);
		
		return config;
	}

}
