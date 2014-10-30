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

package demo.app.server;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;

import demo.app.data.CausalityView;
import demo.app.data.EvidenceView;
import demo.app.data.ExceptionView;
import demo.app.data.HistoryView;
import demo.app.data.UsageView;
import demo.app.data.View;

/**
 * Abstract view directory base class which acts as a directory of views which
 * are displayed in the Prelert GUI.
 * @author Pete Harverson
 */
public class ViewDirectory
{
	static Logger logger = Logger.getLogger(ViewDirectory.class);
	
	private List<View>				m_Views;
	private Hashtable<String, View>	m_ViewsTable;	// Table of views, hashed on view name.
	
	private List<EvidenceView>		m_EvidenceViews;	// The Desktop evidence views.
	private ExceptionView			m_ExceptionView;	// The 'real-time' evidence view.
	private List<UsageView>			m_TimeSeriesViews;	// The Desktop time series views.
	private CausalityView			m_CausalityView;	// The system Causality View.
	private HistoryView				m_HistoryView;		// The system History View.
	
	
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
		m_ViewsTable = new Hashtable<String, View>();
	}
	
	
	/**
	 * Adds a view to the View Directory.
	 * @param view the view to add.
	 */
	public void addView(View view)
	{
		m_Views.add(view);
		m_ViewsTable.put(view.getName(), view);
		
		if (view instanceof EvidenceView)
		{
			addEvidenceView((EvidenceView)view);
		}
		else if (view instanceof UsageView)
		{
			addTimeSeriesView((UsageView)view);
		}
	}
	
	
	/**
	 * Removes the view with the specified name from the internal view list and map.
	 * @param viewName the name of the view to remove.
	 */
	private void removeView(String viewName)
	{
		View oldView = m_ViewsTable.remove(viewName);
    	if (oldView != null)
    	{
    		m_Views.remove(oldView);
    	}
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
	 * Returns the view with the specified name.
	 * @param viewName the name of the View to return.
	 * @return the View with the specified name, or <code>null</code>
	 * if there is no View in the directory with a matching name.
	 */
	public View getView(String viewName)
	{
		return m_ViewsTable.get(viewName);
	}
	
	
	/**
	 * Returns the list of views from the directory that have been configured
	 * as having desktop shortcuts.
	 * @return the list of views in the directory.
	 */
	public List<View> getDesktopViews()
	{
		ArrayList<View> desktopViews = new ArrayList<View>();
		
		for (View view : m_Views) 
		{
			if (view.isDesktopShortcut() == true)
			{
				desktopViews.add(view);
			}
		}
		
		return desktopViews;
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
    			removeView(view.getName());
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
	 * Returns the Exception View that has been configured in the view 
	 * configuration file.
	 * @return the Exception View.
     */
    public ExceptionView getExceptionView()
    {
    	return m_ExceptionView;
    }


	/**
     * Sets the reference to the Exception View that has been configured 
	 * in the view configuration file.
	 * @param exceptionView the configured Exception View.
     */
    public void setExceptionView(ExceptionView exceptionView)
    {
    	m_ExceptionView = exceptionView;
    	
    	// Remove the old Exception View from the combined list and map.
    	removeView(exceptionView.getName());
    	
    	addView(m_ExceptionView);
    }
    
    
    /**
	 * Returns the list of desktop Time Series Views that have been configured 
	 * in the view configuration file.
     * @return the list of Time Series views.
     */
    public List<UsageView> getTimeSeriesViews()
    {
    	return m_TimeSeriesViews;
    }


	/**
	 * Sets the list of desktop Time Series Views that have been configured 
	 * in the view configuration file.
     * @param timeSeriesViews the list of desktop Usage views.
     */
    protected void setTimeSeriesViews(List<UsageView> timeSeriesViews)
    {
    	if (m_TimeSeriesViews != null)
    	{
    		// Remove the old Time Series Views from the internal data structures.
    		for (UsageView view : m_TimeSeriesViews)
    		{
    			removeView(view.getName());
    		}
    	}
    	
    	// Add the new Time Series Views into the internal data structures.
		for (UsageView newView : timeSeriesViews)
		{
			addView(newView);
		}
    }
    
    
    /**
     * Adds a usage view to the list of desktop usage views.
     * @param timeSeriesView time series view to add.
     */
    private void addTimeSeriesView(UsageView timeSeriesView)
    {
    	if (m_TimeSeriesViews == null)
    	{
    		m_TimeSeriesViews = new ArrayList<UsageView>();
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
		
		// Remove the old m_CausalityView View from the combined list and map.
		removeView(causalityView.getName());
		
		addView(m_CausalityView);
	}
    
    
	/**
	 * Returns the Evidence History View that has been configured in the view 
	 * configuration file.
	 * @return the History View or <code>null</code> if no History View has been
	 * 			configured.
	 */
	public HistoryView getHistoryView()
	{
		return m_HistoryView;
	}
    
	
	/**
	 * Sets the reference to the Evidence History View that has been configured 
	 * in the view configuration file.
	 * @param historyView the configured History View.
	 */
	public void setHistoryView(HistoryView historyView)
	{
		m_HistoryView = historyView;
		
		// Remove the old History View from the combined list and map.
    	removeView(historyView.getName());
		
		addView(m_HistoryView);
	}
	
}
