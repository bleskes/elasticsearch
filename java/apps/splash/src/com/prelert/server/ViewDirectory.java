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

package com.prelert.server;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.data.CausalityView;
import com.prelert.data.DataSourceType;
import com.prelert.data.EvidenceView;
import com.prelert.data.TimeSeriesView;
import com.prelert.data.View;

/**
 * Abstract view directory base class which acts as a directory of views which
 * are displayed in the Prelert GUI.
 * @author Pete Harverson
 */
public class ViewDirectory
{
	static Logger logger = Logger.getLogger(ViewDirectory.class);
	
	private List<View>				m_Views;
	private Hashtable<DataSourceType, View>	m_ViewsTable;
	
	private List<EvidenceView>		m_EvidenceViews;	// The Desktop evidence views.
	private List<TimeSeriesView>	m_TimeSeriesViews;	// The Desktop time series views.
	private CausalityView			m_CausalityView;	// The system Causality View.
	
	
	/**
	 * Creates a new View Directory.
	 */
	public ViewDirectory()
	{

	}
	
	
	/**
	 * Initialises the View Directory, creating data structures.
	 */
	public void init()
	{
		m_Views = new ArrayList<View>();
		m_ViewsTable = new Hashtable<DataSourceType, View>();
		m_EvidenceViews = new ArrayList<EvidenceView>();
		m_TimeSeriesViews = new ArrayList<TimeSeriesView>();
	}
	
	
	/**
	 * Adds a view to the View Directory.
	 * @param view the view to add.
	 */
	public void addView(View view)
	{
		m_Views.add(view);
		
		DataSourceType dsType = view.getDataSourceType();
		if (dsType != null)
		{
			m_ViewsTable.put(dsType, view);
		}
		
		if (view instanceof EvidenceView)
		{
			addEvidenceView((EvidenceView)view);
		}
		else if (view instanceof TimeSeriesView)
		{
			addTimeSeriesView((TimeSeriesView)view);
		}
		else if (view instanceof CausalityView)
		{
			setCausalityView((CausalityView)view);
		}
	}
	
	
	/**
	 * Removes the view with the specified name from the internal view list and map.
	 * @param viewName the name of the view to remove.
	 */
	public void removeView(DataSourceType dataSourceType)
	{
		View oldView = m_ViewsTable.remove(dataSourceType);
    	if (oldView != null)
    	{
    		m_Views.remove(oldView);
    		
    		if (oldView instanceof EvidenceView)
    		{
    			m_EvidenceViews.remove(oldView);
    		}
    		else if (oldView instanceof TimeSeriesView)
    		{
    			m_TimeSeriesViews.remove(oldView);
    		}	
    	}
	}
	
	
	/**
	 * Clears all views from the directory.
	 */
	public void clearViews()
	{
		if (m_Views != null)
		{
			m_Views.clear();
		}
		
		if (m_ViewsTable != null)
		{
			m_ViewsTable.clear();
		}
		
		if (m_EvidenceViews != null)
		{
			m_EvidenceViews.clear();
		}
		
		if (m_TimeSeriesViews != null)
		{
			m_TimeSeriesViews.clear();
		}
		
		m_CausalityView = null;
	}
	
	
	/**
	 * Returns the list of all views stored in the directory.
	 * @return the complete list of views in the directory.
	 */
	public List<View> getViews()
	{
		return m_Views;
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
	public View getView(DataSourceType dataSourceType)
	{
		return m_ViewsTable.get(dataSourceType);
	}
	
	
    /**
	 * Returns the list of desktop Evidence Views that have been configured 
	 * in the view configuration file.
     * @return the list of Evidence views.
     */
    public List<EvidenceView> getEvidenceViews()
    {
    	return m_EvidenceViews;
    }


	/**
	 * Sets the list of desktop Evidence Views that have been configured 
	 * in the view configuration file.
     * @param evidenceViews the list of desktop Evidence views.
     */
    public void setEvidenceViews(List<EvidenceView> evidenceViews)
    {
    	if (m_EvidenceViews != null)
    	{
    		// Remove the old Evidence Views from the internal data structures.
    		for (EvidenceView view : m_EvidenceViews)
    		{
    			removeView(view.getDataSourceType());
    		}
    	}
    	
		// Add the new Evidence Views into the internal data structures.
		for (EvidenceView newView : evidenceViews)
		{
			addView(newView);
		}
    }
    
    
    /**
     * Adds an evidence view to the list of desktop evidence views.
     * @param evidenceView evidence view to add.
     */
    private void addEvidenceView(EvidenceView evidenceView)
    {
    	if (m_EvidenceViews == null)
    	{
    		m_EvidenceViews = new ArrayList<EvidenceView>();
    	}
    	
    	m_EvidenceViews.add(evidenceView);
    }
    
    
    /**
	 * Returns the list of desktop Time Series Views that have been configured 
	 * in the view configuration file.
     * @return the list of Time Series views.
     */
    public List<TimeSeriesView> getTimeSeriesViews()
    {
    	return m_TimeSeriesViews;
    }


	/**
	 * Sets the list of desktop Time Series Views that have been configured 
	 * in the view configuration file.
     * @param timeSeriesViews the list of desktop Usage views.
     */
    protected void setTimeSeriesViews(List<TimeSeriesView> timeSeriesViews)
    {
    	if (m_TimeSeriesViews != null)
    	{
    		// Remove the old Time Series Views from the internal data structures.
    		for (TimeSeriesView view : m_TimeSeriesViews)
    		{
    			removeView(view.getDataSourceType());
    		}
    	}
    	
    	// Add the new Time Series Views into the internal data structures.
		for (TimeSeriesView newView : timeSeriesViews)
		{
			addView(newView);
		}
    }
    
    
    /**
     * Adds a view to the list of time series views.
     * @param timeSeriesView time series view to add.
     */
    private void addTimeSeriesView(TimeSeriesView timeSeriesView)
    {
    	if (m_TimeSeriesViews == null)
    	{
    		m_TimeSeriesViews = new ArrayList<TimeSeriesView>();
    	}
    	
    	m_TimeSeriesViews.add(timeSeriesView);
    }
    
    
	/**
	 * Returns the Causality View that has been configured in the view 
	 * configuration file.
	 * @return the Causality View or <code>null</code> if no Causality View has 
	 * 			been configured.
	 */
	public CausalityView getCausalityView()
	{
		return m_CausalityView;
	}
    
	
	/**
	 * Sets the reference to the Causality View that has been configured 
	 * in the view configuration file.
	 * @param causalityView the configured Causality View.
	 */
	public void setCausalityView(CausalityView causalityView)
	{
		m_CausalityView = causalityView;
	}
    
}
