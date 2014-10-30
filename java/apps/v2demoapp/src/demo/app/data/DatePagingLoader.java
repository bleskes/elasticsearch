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

import com.extjs.gxt.ui.client.data.BaseListLoader;
import com.extjs.gxt.ui.client.data.DataProxy;

import demo.app.client.ApplicationResponseHandler;


/**
 * Custom BaseListLoader used for loading data into list pages which display data
 * over a configurable date range.
 * @author Pete Harverson
 *
 * @param <C> the DatePagingLoadConfig config type
 * @param <D> the DatePagingLoadResult result type
 */
public class DatePagingLoader<D extends DatePagingLoadResult> 
	extends BaseListLoader
{
	private String		m_DataType;
	private TimeFrame 	m_TimeFrame;
	private Date		m_Date;
	
	
	public DatePagingLoader(DataProxy<D> proxy)
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
	
	
	public void load(Date date, TimeFrame timeFrame)
	{
		m_Date = date;
		m_TimeFrame = timeFrame;
		load();
	}
	
    
    protected void loadData(final Object config)
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


	/**
	 * Use the specified LoadConfig for all load calls. The {@link #reuseConfig}
	 * will be set to true.
	 */
	public void useLoadConfig(Object loadConfig)
	{
		super.useLoadConfig(loadConfig);
		
		DatePagingLoadConfig pagingLoadConfig = (DatePagingLoadConfig)loadConfig;

		m_DataType = pagingLoadConfig.getDataType();
		m_TimeFrame = pagingLoadConfig.getTimeFrame();
		m_Date = pagingLoadConfig.getDate();
	}


	/**
	 * Template method to allow custom BaseLoader subclasses to provide their
	 * own implementation of LoadConfig
	 */
	protected Object newLoadConfig()
	{
		return new DatePagingLoadConfig();
	}


	/**
	 * Template method to allow custom subclasses to prepare the load config
	 * prior to loading data
	 */
	protected Object prepareLoadConfig(Object config)
	{
		super.prepareLoadConfig(config);
		
		DatePagingLoadConfig pagingLoadConfig = (DatePagingLoadConfig)config;
		pagingLoadConfig.setDataType(m_DataType);
		pagingLoadConfig.setDate(m_Date);
		pagingLoadConfig.setTimeFrame(m_TimeFrame);
		
		return config;
	}
	  

}
