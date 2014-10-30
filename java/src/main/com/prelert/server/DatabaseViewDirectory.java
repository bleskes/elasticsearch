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

package com.prelert.server;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.dao.DataSourceDAO;
import com.prelert.dao.EvidenceDAO;
import com.prelert.dao.TimeSeriesDAO;
import com.prelert.data.DataSourceType;
import com.prelert.data.EvidenceView;
import com.prelert.data.TimeFrame;
import com.prelert.data.TimeSeriesView;
import com.prelert.data.View;


/**
 * Extension of the ViewDirectory base class that constructs the directory of views 
 * by querying the database for the range of source types (e.g. notifications and
 * time series) from which data has been analysed by the Prelert engine.
 * @author Pete Harverson
 */
public class DatabaseViewDirectory extends ViewDirectory
{
	static Logger s_Logger = Logger.getLogger(DatabaseViewDirectory.class);
	
	
	private boolean				m_IsStoringViews;
	
	private DataSourceDAO		m_DataSourceDAO;
	private EvidenceDAO 		m_EvidenceDAO;
	private TimeSeriesDAO		m_TimeSeriesDAO;
	
	
	/**
	 * Creates a new view directory that obtains its information by querying
	 * the database for the range of data source types. The view directory will
	 * be populated at initialisation and a cache of views stored to service 
	 * all subsequent requests.
	 */
	public DatabaseViewDirectory()
	{
		this(true);
	}
	
	
	/**
	 * Creates a new view directory that obtains its information by querying
	 * the database for the range of data source types.
	 * @param storeViews <code>true</code> to populate the view directory at
	 * 	initialisation and store the list of views to service all subsequent
	 * 	requests, or <code>false</code> to query the database every time a
	 * 	request for view(s) is received.
	 */
	public DatabaseViewDirectory(boolean storeViews)
	{
		m_IsStoringViews = storeViews;
	}
	
	
	/**
	 * Initialises the View Directory, constructing the directory by querying the
	 * database for the range of source types that are available.
	 */
	public void init()
	{
		super.init();
		
		if (m_IsStoringViews == true)
		{
			buildViews();
		}
	}
	
	
	/**
	 * Sets the DataSourceDAO to be used to make queries on data sources.
	 * @param dataSourceDAO the data access object for Prelert data source information.
	 */
	public void setDataSourceDAO(DataSourceDAO dataSourceDAO)
	{
		m_DataSourceDAO = dataSourceDAO;
	}
	
	
	/**
	 * Returns the DataSourceDAO being used to make queries on data sources.
	 * @param dataSourceDAO the data access object for Prelert data source information.
	 */
	public DataSourceDAO getDataSourceDAO()
	{
		return m_DataSourceDAO;
	}
	
	
	/**
	 * Sets the EvidenceDAO to be used to obtain evidence data.
	 * @param evidenceDAO the data access object for evidence data.
	 */
	public void setEvidenceDAO(EvidenceDAO evidenceDAO)
	{
		m_EvidenceDAO = evidenceDAO;
	}
	
	
	/**
	 * Returns the EvidenceDAO being used to obtain evidence data.
	 * @return the data access object for evidence data.
	 */
	public EvidenceDAO getEvidenceDAO()
	{
		return m_EvidenceDAO;
	}
	
	
	/**
	 * Sets the data access object to be used to obtain time series data.
	 * @param evidenceDAO the data access object for evidence data.
     * @param timeSeriesDAO the data access object for time series data.
     */
    public void setTimeSeriesDAO(TimeSeriesDAO timeSeriesDAO)
    {
    	m_TimeSeriesDAO = timeSeriesDAO;
    }
	
	
	/**
	 * Returns the TimeSeriesDAO being used to obtain time series data.
	 * @return the data access object for time series data.
	 */
    public TimeSeriesDAO getTimeSeriesDAO()
    {
    	return m_TimeSeriesDAO;
    }
    
    
    /**
     * Returns whether the directory is servicing requests for view(s) from a
     * store which was populated when the object was created.
     * @param isStoringViews
     * @return
     */
    public boolean isStoringViews(boolean isStoringViews)
    {
    	return m_IsStoringViews;
    }
    
    
    @Override
    public List<View> getViews()
    {
    	List<View> allViews = null;
    	
    	if (m_IsStoringViews == true)
    	{
    		allViews = super.getViews();
    		if  (allViews.size() == 0)
    		{
    			allViews = buildViews();
    		}
    	}
    	else
    	{
    		allViews = buildViews();
    	}
    	
    	return allViews;
    }
    

    /**
	 * Returns the configuration data for a view used to display information
	 * from the specified data source type.
	 * @param dataSourceType	data source type for which to return the
	 * 		view e.g. p2pslogs or UDP error data.
	 * @return View object encapsulating configuration properties for a view
	 * 		of the specified data source type, or <code>null</code>
	 * if there is no View in the directory with a matching name.
	 */
    @Override
    public View getView(DataSourceType dataSourceType)
    {	
    	View view = null;
    	if (m_IsStoringViews == true)
    	{
    		view = super.getView(dataSourceType);
    	}
    	else
    	{
    		view = createView(dataSourceType);
    	}
    	
    	return view;
    }


	/**
     * Builds the list of views.
     * @return the complete list of views that have been built from information
     * in the Prelert database.
     */
	protected List<View> buildViews()
	{
		ArrayList<View> allViews = new ArrayList<View>();
		
		// Get the list of data source types from the database.
		List<DataSourceType> sourceTypes = m_DataSourceDAO.getDataSourceTypes();
		
		// Create a view for each notification and time series data source type.
		View view;
		for (DataSourceType sourceType : sourceTypes)
		{
			view = createView(sourceType);
			if (view != null)
			{
				allViews.add(view);
				
				if (m_IsStoringViews == true)
				{
					addView(view);
				}
			}
		}
		
		s_Logger.info("Total number of views built by directory: " + allViews.size());
		
		return allViews;

	}


	/**
	 * Creates a View for the specified data source type.
	 * Currently views are only created for notification and time series 
	 * data source categories.
	 * @param dataSourceType source type for which to create a view.
	 * @param sourceTypes complete list of source types which are available
	 * 		for interaction with the view that is being created.
	 * @return View for the supplied source type.
	 */
	protected View createView(DataSourceType dataSourceType)
	{
		View view = null;

		switch (dataSourceType.getDataCategory())
		{
			case NOTIFICATION:
				view = createEvidenceView(dataSourceType);
				s_Logger.info("Created NOTIFICATION view: " + view);
				break;
				
			case TIME_SERIES:
				view = createTimeSeriesView(dataSourceType);
				s_Logger.info("Created TIME_SERIES view: " + view);
				break;
				
			case TIME_SERIES_FEATURE:
				view = createTimeSeriesFeatureView(dataSourceType);
				s_Logger.info("Created TIME_SERIES_FEATURE view: " + view);
				break;
		}
		
		return view;
	}
	
	
	/**
	 * Creates an EvidenceView for the specified notification-type data source.
	 * @param dataSourceType notification-type data source.
	 * @param sourceTypes complete list of source types which are available
	 * 		for interaction with the view that is being created.
	 * @return EvidenceView for the supplied data source.
	 */
	protected EvidenceView createEvidenceView(DataSourceType dataSourceType)
	{
		// Set the list of display columns and filterable columns.
		EvidenceView evidenceView = new EvidenceView();
		evidenceView.setName(dataSourceType.getName());
		evidenceView.setDataType(dataSourceType.getName());
		evidenceView.setTimeFrame(TimeFrame.SECOND);
		
		// Set the list of display columns and filterable columns in the
		// evidence view.
		List<String> evidenceColumns = m_EvidenceDAO.getAllColumns(dataSourceType.getName());
		List<String> filterableAttributes = 
			m_EvidenceDAO.getFilterableColumns(dataSourceType.getName(), true, true);
		evidenceView.setColumns(evidenceColumns);
		evidenceView.setFilterableAttributes(filterableAttributes);
		
		return evidenceView;
	}
	
	
	/**
	 * Creates a Time Series view for the specified time series data source.
	 * @param dataSourceType time series data source.
	 * @param sourceTypes complete list of source types which are available
	 * 		for interaction with the view that is being created.
	 * @return Time Series View for the supplied data source.
	 */
	protected TimeSeriesView createTimeSeriesView(DataSourceType dataSourceType)
	{
		String dataTypeName = dataSourceType.getName();
		TimeSeriesView timeSeriesView = new TimeSeriesView();
		timeSeriesView.setName(dataTypeName);
		timeSeriesView.setDataType(dataTypeName);
		
		// Set the list of attribute and metrics for the view.
		List<String> metrics = m_TimeSeriesDAO.getMetrics(dataTypeName);
		List<String> attributeNames = m_TimeSeriesDAO.getAttributeNames(dataTypeName);
		
		timeSeriesView.setMetrics(metrics);
		timeSeriesView.setAttributeNames(attributeNames);
		
		// Set the flag to indicate whether the data type supports aggregate views.
		boolean isAggregationSupported = m_TimeSeriesDAO.isAggregationSupported(dataTypeName);
		timeSeriesView.setAggregationSupported(isAggregationSupported);
		
		// Create the EvidenceView for the features list.
		EvidenceView featuresView = createTimeSeriesFeatureView(dataSourceType);
		timeSeriesView.setFeaturesView(featuresView);
		
		return timeSeriesView;
	}
	
	
	/**
	 * Creates an EvidenceView for the specified time series feature data source type.
	 * @param dataSourceType time series feature type data source.
	 * @param sourceTypes complete list of source types which are available
	 * 		for interaction with the view that is being created.
	 * @return EvidenceView for the supplied data source.
	 */
	protected EvidenceView createTimeSeriesFeatureView(DataSourceType dataSourceType)
	{
		EvidenceView evidenceView = new EvidenceView();
		evidenceView.setName(dataSourceType.getName() + " features"); // This is only used internally.
		evidenceView.setDataType(dataSourceType.getName());
		evidenceView.setDataCategory(dataSourceType.getDataCategory());
		evidenceView.setTimeFrame(TimeFrame.SECOND);
		
		// Set the list of display columns and filterable columns in the
		// evidence view.
		List<String> evidenceColumns = m_EvidenceDAO.getAllColumns(dataSourceType.getName());
		List<String> filterableAttributes = 
			m_EvidenceDAO.getFilterableColumns(dataSourceType.getName(), true, true);
		evidenceView.setColumns(evidenceColumns);
		evidenceView.setFilterableAttributes(filterableAttributes);
		
		return evidenceView;
	}
}
