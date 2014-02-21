package demo.app.data;

import java.util.Date;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.google.gwt.core.client.GWT;

import demo.app.client.ApplicationResponseHandler;
import demo.app.data.gxt.EvidenceModel;
import demo.app.service.GetExceptionDataRpcProxy;


public class ExceptionListPagingLoader<D extends DatePagingLoadResult<EvidenceModel>> 
	extends DatePagingLoader<DatePagingLoadResult<BaseModelData>>
{
	private GetExceptionDataRpcProxy<D> m_Proxy;
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
    	GWT.log("++++ ExceptionListPagingLoader.load()", null);
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
	
			m_Proxy.loadFirstPage((ExceptionPagingLoadConfig)loadConfig, callback);
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
					onLoadSuccess(loadConfig, (D)result);
				}
			};

			m_Proxy.loadLastPage((ExceptionPagingLoadConfig)loadConfig, callback);
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
					onLoadSuccess(loadConfig, (D)result);
				}
			};

			m_Proxy.loadNextPage((ExceptionPagingLoadConfig)loadConfig, rowId, callback);
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
					onLoadSuccess(loadConfig, (D)result);
				}
			};

			m_Proxy.loadPreviousPage((ExceptionPagingLoadConfig)loadConfig, rowId, callback);
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
					onLoadSuccess(loadConfig, (D)result);
				}
			};

			m_Proxy.loadAtTime((ExceptionPagingLoadConfig)loadConfig, callback);
		}
	}
	
	
	/**
	 * Use the specified LoadConfig for all load calls. The {@link #reuseConfig}
	 * will be set to true.
	 */
	public void useLoadConfig(Object loadConfig)
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
	protected Object newLoadConfig()
	{
		return new ExceptionPagingLoadConfig();
	}


	/**
	 * Template method to allow custom subclasses to prepare the load config
	 * prior to loading data
	 */
	protected Object prepareLoadConfig(Object config)
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
