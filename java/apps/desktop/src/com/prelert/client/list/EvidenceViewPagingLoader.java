package com.prelert.client.list;

import java.util.Date;

import com.extjs.gxt.ui.client.data.BaseListLoader;
import com.extjs.gxt.ui.client.data.DataProxy;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.prelert.service.GetEvidenceRecordsRpcProxy;
import com.prelert.data.DatePagingLoadConfig;
import com.prelert.data.DatePagingLoadResult;
import com.prelert.data.TimeFrame;


public class EvidenceViewPagingLoader<C extends DatePagingLoadConfig, D extends DatePagingLoadResult> 
	extends BaseListLoader<C, D>
{
	private GetEvidenceRecordsRpcProxy m_Proxy;
	private TimeFrame 	m_TimeFrame;
	private Date		m_Date;
	
	
	public EvidenceViewPagingLoader(GetEvidenceRecordsRpcProxy<C, D> proxy)
    {
	    super((DataProxy<C, D>) proxy);
	    m_Proxy = proxy;
	    m_TimeFrame = TimeFrame.WEEK;
    }
	
	
	public TimeFrame getTimeFrame()
    {
    	return m_TimeFrame;
    }


	public void setTimeFrame(TimeFrame timeFrame)
    {
    	m_TimeFrame = timeFrame;
    }


	public Date getDate()
    {
    	return m_Date;
    }


	public void setDate(Date date)
    {	
    	m_Date = date;
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
		m_Date = date;
		m_TimeFrame = timeFrame;
		C config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final C loadConfig = config;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			AsyncCallback<D> callback = new AsyncCallback<D>()
			{
				public void onFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void onSuccess(D result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};

			m_Proxy.loadFirstPage(loadConfig, callback);
		}
	}
	
	
	public void loadLastPage(Date date, TimeFrame timeFrame)
	{
		m_Date = date;
		m_TimeFrame = timeFrame;
		C config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final C loadConfig = config;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			AsyncCallback<D> callback = new AsyncCallback<D>()
			{
				public void onFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void onSuccess(D result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};

			m_Proxy.loadLastPage(loadConfig, callback);
		}
	}
	
	
	public void loadNextPage(Date bottomRowDate, String bottomRowId, TimeFrame timeFrame)
	{
		m_Date = bottomRowDate;
		m_TimeFrame = timeFrame;
		C config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final C loadConfig = config;
		final String rowId = bottomRowId;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			AsyncCallback<D> callback = new AsyncCallback<D>()
			{
				public void onFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void onSuccess(D result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};

			m_Proxy.loadNextPage(loadConfig, rowId, callback);
		}
	}
	
	
	public void loadPreviousPage(Date topRowDate, String topRowId, TimeFrame timeFrame)
	{
		m_Date = topRowDate;
		m_TimeFrame = timeFrame;
		C config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final C loadConfig = config;
		final String rowId = topRowId;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			AsyncCallback<D> callback = new AsyncCallback<D>()
			{
				public void onFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void onSuccess(D result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};

			m_Proxy.loadPreviousPage(loadConfig, rowId, callback);
		}
	}
	
	
	public void loadAtTime(Date date, TimeFrame timeFrame)
	{
		m_Date = date;
		m_TimeFrame = timeFrame;
		C config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final C loadConfig = config;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			AsyncCallback<D> callback = new AsyncCallback<D>()
			{
				public void onFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void onSuccess(D result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};

			m_Proxy.loadAtTime(loadConfig, callback);
		}
	}
	
	
	public void loadForDescrption(Date date, String description, TimeFrame timeFrame)
	{
		m_Date = date;
		m_TimeFrame = timeFrame;
		C config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final C loadConfig = config;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			AsyncCallback<D> callback = new AsyncCallback<D>()
			{
				public void onFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void onSuccess(D result)
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
		
		config.setDate(m_Date);
		config.setTimeFrame(m_TimeFrame);
		
		return config;
	}



	  

}
