/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2009     *
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

import com.extjs.gxt.ui.client.data.LoadEvent;
import com.google.gwt.core.client.GWT;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.service.GetExceptionDataRpcProxy;


public class ExceptionListPagingLoader<C extends ExceptionPagingLoadConfig, D extends DatePagingLoadResult<EventRecord>> 
	extends DatePagingLoader<C, D>
{
	private GetExceptionDataRpcProxy<C, D> m_Proxy;
	private int 		m_NoiseLevel;
	private TimeFrame 	m_TimeWindow;
	
	
	public ExceptionListPagingLoader(GetExceptionDataRpcProxy proxy)
    {
		super(proxy);
		m_Proxy = proxy;
    }
	
	
	/**
	 * Returns the level of noise to act as the filter for the exception list.
	 * @return the noise level, a value from 0 to 100.
	 */
	public int getNoiseLevel()
	{
		return m_NoiseLevel;
	}


	/**
	 * Sets the level of noise to act as the filter for the exception list.
	 * @param noiseLevel the noise level, a value from 0 to 100.
	 */
	public void setNoiseLevel(int noiseLevel)
	{
		m_NoiseLevel = noiseLevel;
	}


	/**
	 * Returns the time window for the Exception List in which an item of
	 * evidence is examined to see if it is an 'exception'.
	 * @return the time window e.g. WEEK, DAY or HOUR.
	 */
	public TimeFrame getTimeWindow()
	{
		return m_TimeWindow;
	}


	/**
	 * Sets the time window for the Exception List in which an item of
	 * evidence is examined to see if it is an 'exception'.
	 * @param timeWindow the time window e.g. WEEK, DAY or HOUR.
	 */
	public void setTimeWindow(TimeFrame timeWindow)
	{
		m_TimeWindow = timeWindow;
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
		C config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final C loadConfig = config;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<D> callback = new ApplicationResponseHandler<D>()
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
	
			m_Proxy.loadFirstPage((ExceptionPagingLoadConfig)loadConfig, 
					(ApplicationResponseHandler<DatePagingLoadResult<EventRecord>>)callback);
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
		C config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final C loadConfig = config;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<D> callback = new ApplicationResponseHandler<D>()
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

			m_Proxy.loadLastPage((ExceptionPagingLoadConfig)loadConfig, 
					(ApplicationResponseHandler<DatePagingLoadResult<EventRecord>>)callback);
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
		C config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final C loadConfig = config;
		final String rowId = bottomRowId;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<D> callback = new ApplicationResponseHandler<D>()
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

			m_Proxy.loadNextPage((ExceptionPagingLoadConfig)loadConfig, rowId, 
					(ApplicationResponseHandler<DatePagingLoadResult<EventRecord>>)callback);
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
		C config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final C loadConfig = config;
		final String rowId = topRowId;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<D> callback = new ApplicationResponseHandler<D>()
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

			m_Proxy.loadPreviousPage((ExceptionPagingLoadConfig)loadConfig, rowId,
					(ApplicationResponseHandler<DatePagingLoadResult<EventRecord>>)callback);
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
		C config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final C loadConfig = config;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<D> callback = new ApplicationResponseHandler<D>()
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

			m_Proxy.loadAtTime((ExceptionPagingLoadConfig)loadConfig,
					(ApplicationResponseHandler<DatePagingLoadResult<EventRecord>>)callback);
		}
	}
	
	
	/**
	 * Use the specified LoadConfig for all load calls. The {@link #reuseConfig}
	 * will be set to true.
	 */
	public void useLoadConfig(C loadConfig)
	{
		super.useLoadConfig(loadConfig);
		
		ExceptionPagingLoadConfig pagingLoadConfig = (ExceptionPagingLoadConfig)loadConfig;
	
		setTimeFrame(pagingLoadConfig.getTimeFrame());
		setDate(pagingLoadConfig.getDate());
		
		m_NoiseLevel = pagingLoadConfig.getNoiseLevel();
		m_TimeWindow = pagingLoadConfig.getTimeWindow();
	}


	/**
	 * Template method to allow custom BaseLoader subclasses to provide their
	 * own implementation of LoadConfig
	 */
	protected C newLoadConfig()
	{
		return (C) new ExceptionPagingLoadConfig();
	}


	/**
	 * Template method to allow custom subclasses to prepare the load config
	 * prior to loading data
	 */
	protected C prepareLoadConfig(C config)
	{
		super.prepareLoadConfig(config);
		
		ExceptionPagingLoadConfig pagingLoadConfig = (ExceptionPagingLoadConfig)config;

		pagingLoadConfig.setDate(getDate());
		pagingLoadConfig.setTimeFrame(getTimeFrame());
		pagingLoadConfig.setNoiseLevel(m_NoiseLevel);
		pagingLoadConfig.setTimeWindow(m_TimeWindow);
		
		return config;
	}
}
