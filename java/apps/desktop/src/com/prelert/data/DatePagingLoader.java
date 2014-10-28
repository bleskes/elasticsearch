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

import com.extjs.gxt.ui.client.data.BaseListLoader;
import com.extjs.gxt.ui.client.data.DataProxy;
import com.extjs.gxt.ui.client.data.LoadEvent;

import com.prelert.client.ApplicationResponseHandler;


/**
 * Custom BaseListLoader used for loading data into list pages which display data
 * over a configurable date range.
 * @author Pete Harverson
 *
 * @param <C> the DatePagingLoadConfig config type
 * @param <D> the DatePagingLoadResult result type
 */
public class DatePagingLoader<C extends DatePagingLoadConfig, D extends DatePagingLoadResult> 
	extends BaseListLoader<C, D>
{
	private String		m_DataType;
	private TimeFrame 	m_TimeFrame;
	private Date		m_Date;
	
	
	/**
	 * Creates a new DatePagingLoader for loading data into pages which display
	 * data over a date range.
	 * @param proxy the data proxy.
	 */
	public DatePagingLoader(DataProxy<C, D> proxy)
    {
	    super(proxy);
	    m_TimeFrame = TimeFrame.WEEK;
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
	 * Returns the time frame for this loader.
	 * @return time frame of this loader e.g. week, day or hour.
	 */
	public TimeFrame getTimeFrame()
    {
    	return m_TimeFrame;
    }


	/**
	 * Sets the time frame for this loader.
	 * @param timeFrame time frame of this loader e.g. week, day or hour.
	 */
	public void setTimeFrame(TimeFrame timeFrame)
    {
    	m_TimeFrame = timeFrame;
    }


	/**
	 * Returns the date for this loader.
	 * @return the date for the results to be loaded in the page.
	 */
	public Date getDate()
    {
    	return m_Date;
    }


	/**
	 * Sets the date for this loader.
	 * @param date the date for the results to be loaded in the page.
	 */
	public void setDate(Date date)
    {	
    	m_Date = date;
    }
	
	
	/**
	 * Loads the data for the specified date and time frame.
	 * @param date the date for the results to be loaded into the page.
	 * @param timeFrame time frame for the data to be loaded e.g. week, day or hour.
	 */
	public void load(Date date, TimeFrame timeFrame)
	{
		m_Date = date;
		m_TimeFrame = timeFrame;
		
		load();
	}
	
	
	protected void loadData(final C config)
	{	
    	ApplicationResponseHandler<D> callback = 
			new ApplicationResponseHandler<D>()
		{
			public void uponFailure(Throwable caught)
			{
				onLoadFailure(config, caught);
			}


			public void uponSuccess(D result)
			{
				onLoadSuccess(config, result);
			}
		};

		proxy.load(reader, config, callback);
	}
	
	/*
	 public boolean load()
    {
    	C config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
        config = prepareLoadConfig(config);
        
        final C loadConfig = config;
        
        if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = loadConfig;

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

			proxy.load(reader, config, callback);

			return true;
		}
		return false;
    }
	*/
	
	
	/**
	 * Use the specified LoadConfig for all load calls. reuseLoadConfig will be
	 * set to true.
	 */
	public void useLoadConfig(C loadConfig)
	{
		super.useLoadConfig(loadConfig);

		m_DataType = loadConfig.getDataType();
		m_TimeFrame = loadConfig.getTimeFrame();
		m_Date = loadConfig.getDate();
	}


	/**
	 * Template method to allow custom BaseLoader subclasses to provide their
	 * own implementation of LoadConfig
	 */
	protected C newLoadConfig()
	{
		return (C) new DatePagingLoadConfig();
	}


	/**
	 * Template method to allow custom subclasses to prepare the load config
	 * prior to loading data
	 */
	protected C prepareLoadConfig(C config)
	{
		super.prepareLoadConfig(config);
		
		config.setDataType(m_DataType);
		config.setDate(m_Date);
		config.setTimeFrame(m_TimeFrame);
		
		return config;
	}
}
