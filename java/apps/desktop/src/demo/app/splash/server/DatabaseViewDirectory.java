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

package demo.app.splash.server;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import demo.app.dao.EvidenceDAO;
import demo.app.dao.TimeSeriesDAO;
import demo.app.data.CausalityView;
import demo.app.data.CausalityViewTool;
import demo.app.data.DataSourceCategory;
import demo.app.data.DataSourceType;
import demo.app.data.EvidenceView;
import demo.app.data.ListViewTool;
import demo.app.data.TimeFrame;
import demo.app.data.Tool;
import demo.app.data.UsageView;
import demo.app.data.UsageViewTool;
import demo.app.data.View;
import demo.app.server.ViewDirectory;
import demo.app.splash.dao.DataSourceDAO;


/**
 * Extension of the ViewDirectory base class that constructs the directory of
 * views at initialisation by querying the database for the range of source 
 * types (e.g. notifications and time series) and sources (servers) from which
 * data has been analysed by the Prelert engine.
 * @author Pete Harverson
 */
public class DatabaseViewDirectory extends ViewDirectory
{
	static Logger logger = Logger.getLogger(DatabaseViewDirectory.class);
	
	
	private DataSourceDAO		m_DataSourceDAO;
	private EvidenceDAO 		m_EvidenceDAO;
	private TimeSeriesDAO		m_TimeSeriesDAO;
	
	
	/**
	 * Initialises the View Directory, constructing the directory by querying the
	 * database for the range of source types that are available.
	 */
	public void init()
	{
		super.init();
		
		try
		{
			// Get the list of data source types from the database.
			List<DataSourceType> sourceTypes = m_DataSourceDAO.getDataSourceTypes();
			
			// Create a view for each notification and time series data source type.
			View view;
			for (DataSourceType sourceType : sourceTypes)
			{
				view = createView(sourceType);
				if (view != null)
				{
					addView(view);
				}
			}
			
			// Configure the context menus for Evidence and Time Series Views.
			if (getEvidenceViews() != null)
			{
				for (EvidenceView evidenceView : getEvidenceViews())
				{
					evidenceView.setContextMenuItems(getContextMenuItems(evidenceView));
					logger.info("Loaded Evidence View: " + evidenceView);
				}
			}

			if (getTimeSeriesViews() != null)
			{
				for (UsageView usageView : getTimeSeriesViews())
				{
					usageView.setContextMenuItems(getContextMenuItems(usageView));
					logger.info("Loaded Time Series View: " + usageView);
				}
			}
			
			// Create the causality view.
			CausalityView causalityView = new CausalityView();
			causalityView.setName("Probable Causes");
			causalityView.setDisplayAsEpisodes(false);
			setCausalityView(causalityView);
			causalityView.setContextMenuItems(getContextMenuItems(causalityView));
			
			logger.info("Loaded Causality View: " + causalityView);
			
			logger.info("Total number of views loaded into directory: " + getViews().size());
			
		}
		catch (Exception e)
		{
			logger.error("Error initialising view directory: ", e);
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
	 * Creates a View for the specified data source type.
	 * Currently views are only created for notification and time series 
	 * data source categories.
	 * @param dataSourceType source type for which to create a view.
	 * @return View for the supplied source type.
	 */
	protected View createView(DataSourceType dataSourceType)
	{
		View view = null;

		switch (dataSourceType.getDataCategory())
		{
			case NOTIFICATION:
				view = createEvidenceView(dataSourceType);
				break;
				
			case TIME_SERIES:
				view = createTimeSeriesView(dataSourceType);
				break;
				
			case TIME_SERIES_FEATURE:
				view = createTimeSeriesFeatureView(dataSourceType);
				break;
		}
		
		return view;
	}
	
	
	/**
	 * Creates an EvidenceView for the specified notification-type data source.
	 * @param dataSourceType notification-type data source.
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
		List<String> evidenceColumns = m_EvidenceDAO.getAllColumns(
				dataSourceType.getName(), evidenceView.getTimeFrame());
		List<String> filterableAttributes = 
			m_EvidenceDAO.getFilterableColumns(dataSourceType.getName(), true, true);
		evidenceView.setColumns(evidenceColumns);
		evidenceView.setFilterableAttributes(filterableAttributes);
		
		return evidenceView;
	}
	
	
	/**
	 * Creates a Time Series view for the specified time series data source.
	 * @param dataSourceType time series data source.
	 * @return UsageView for the supplied data source.
	 */
	protected UsageView createTimeSeriesView(DataSourceType dataSourceType)
	{
		UsageView timeSeriesView = new UsageView();
		timeSeriesView.setName(dataSourceType.getName());
		timeSeriesView.setDataType(dataSourceType.getName());
		
		// Set the list of attribute and metrics for the view.
		List<String> metrics = m_TimeSeriesDAO.getMetrics(dataSourceType.getName());
		List<String> attributeNames = m_TimeSeriesDAO.getAttributeNames(dataSourceType.getName());
		
		timeSeriesView.setMetrics(metrics);
		timeSeriesView.setAttributeNames(attributeNames);
		
		return timeSeriesView;
	}
	
	
	/**
	 * Creates an EvidenceView for the specified time series feature data source type.
	 * @param dataSourceType time series feature type data source.
	 * @return EvidenceView for the supplied data source.
	 */
	protected EvidenceView createTimeSeriesFeatureView(DataSourceType dataSourceType)
	{
		EvidenceView evidenceView = new EvidenceView();
		evidenceView.setName(dataSourceType.getName() + " features"); // This is only used internally.
		evidenceView.setDataType(dataSourceType.getName());
		evidenceView.setDataCategory(dataSourceType.getDataCategory());
		evidenceView.setTimeFrame(TimeFrame.SECOND);
		
		// Set the list of display columns.
		List<String> evidenceColumns = m_EvidenceDAO.getAllColumns(
				dataSourceType.getName(), evidenceView.getTimeFrame());
		evidenceView.setColumns(evidenceColumns);
		
		return evidenceView;
	}
	
	
	/**
	 * Builds the list of context menu items for the specified Evidence or
	 * Time Series view.
	 * @param view
	 * @return the list of context menu items.
	 */
	protected ArrayList<Tool> getContextMenuItems(View view)
	{
		// Build the list of menu items:
		// - Show Probable Cause
		// - Show other notification and time series data types
		ArrayList<Tool> contextMenuItems = new ArrayList<Tool>();
		if (view != getCausalityView())
		{
			contextMenuItems.add(new CausalityViewTool("Show Probable Cause"));
		}
		
		
		ListViewTool listViewTool;
		UsageViewTool usageViewTool;
		for (View otherView : getViews())
		{
			if (otherView.getDataType() != view.getDataType())
			{
				switch (otherView.getDataCategory())
				{
					case NOTIFICATION:
						listViewTool = new ListViewTool();
						listViewTool.setName("Show " + otherView.getDataType());
						listViewTool.setViewToOpen(otherView.getDataType());
						contextMenuItems.add(listViewTool);
						break;
						
					case TIME_SERIES:
						usageViewTool = new UsageViewTool();
						usageViewTool.setName("Show " + otherView.getDataType());
						usageViewTool.setViewToOpen(otherView.getDataType());
						usageViewTool.setTimeFrame(TimeFrame.HOUR);
						contextMenuItems.add(usageViewTool);
						break;
						
					case TIME_SERIES_FEATURE:
						break;
				}
			}
		}
		
		return contextMenuItems;
	}
}
