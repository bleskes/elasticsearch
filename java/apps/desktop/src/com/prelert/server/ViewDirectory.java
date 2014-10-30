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

import java.io.*;
import java.util.*;

import org.apache.commons.digester.*;
import org.apache.log4j.*;

import com.prelert.data.CausalityView;
import com.prelert.data.EvidenceView;
import com.prelert.data.ExceptionView;
import com.prelert.data.HistoryView;
import com.prelert.data.UsageView;
import com.prelert.data.View;


/**
 * Directory holding views for display in the Prelert GUI.
 * At initialisation, the class reads in views which are defined in an external 
 * XML configuration file.
 * @author Pete Harverson
 */
public class ViewDirectory
{
	static Logger logger = Logger.getLogger(ViewDirectory.class);
	
	private List<View>				m_Views;
	private Hashtable<String, View>	m_ViewsTable;		// Table of views, hashed on view name.
	
	private List<EvidenceView>		m_EvidenceViews;	// The Desktop evidence views.
	private ExceptionView			m_ExceptionView;	// The 'real-time' evidence view.
	private List<UsageView>			m_TimeSeriesViews;	// The Desktop time series views.
	private CausalityView			m_CausalityView;	// The system Causality View.
	private HistoryView				m_HistoryView;		// The system History View.
	
	private List<View>				m_UserViews;		// User-defined views.
	
	private String					m_ConfigFile;
	
	
	/**
	 * Creates a new View Directory.
	 */
	public ViewDirectory()
	{

	}
	
	
	/**
	 * Initialises the View Directory, reading in the views which have been
	 * configured in an external XML configuration file.
	 */
	public void init()
	{
		try
		{
			// Read in the views defined in the view configuration file.
			m_Views = new ArrayList<View>();
			m_ViewsTable = new Hashtable<String, View>();
			m_UserViews = new ArrayList<View>();
			parseViewFile(m_ConfigFile);
		}
		catch (Exception e)
		{
			logger.error("Error loading view config file: ", e);
		}
	}
	
	
	/**
	 * Adds a view to the internal view list and map.
	 * @param view the view to add.
	 */
	private void addView(View view)
	{
		m_Views.add(view);
		m_ViewsTable.put(view.getName(), view);
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
    	
    	m_EvidenceViews = evidenceViews;
    }
    
    
    /**
     * Adds a usage view to the list of desktop usage views.
     * @param timeSeriesView time series view to add.
     */
    public void addEvidenceView(EvidenceView evidenceView)
    {
    	if (m_EvidenceViews == null)
    	{
    		m_EvidenceViews = new ArrayList<EvidenceView>();
    	}
    	
    	m_EvidenceViews.add(evidenceView);
    	addView(evidenceView);
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
    	
    	m_TimeSeriesViews = timeSeriesViews;
    }
    
    
    /**
     * Adds a usage view to the list of desktop usage views.
     * @param timeSeriesView time series view to add.
     */
    public void addTimeSeriesView(UsageView timeSeriesView)
    {
    	if (m_TimeSeriesViews == null)
    	{
    		m_TimeSeriesViews = new ArrayList<UsageView>();
    	}
    	
    	m_TimeSeriesViews.add(timeSeriesView);
    	addView(timeSeriesView);
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
	 * @param historyView the configured History View.
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
	
	
    /**
     * Adds a user-defined view to the list of desktop views.
     * @param view user-defined view to add.
     */
	public void addUserDefinedView(View view)
	{
    	m_UserViews.add(view);
    	addView(view);
	}
	
	
	/**
	 * Returns the list of user-defined views that have been configured 
	 * in the view configuration file.
     * @return the list of Usage views.
     */
    public List<View> getUserViews()
    {
    	return m_UserViews;
    }
	
	
	/**
	 * Returns the name of the view configuration file being used by the directory.
	 * @return the view configuration file, which will be loaded from the classpath.
	 */
	public String getConfigFile()
    {
    	return m_ConfigFile;
    }


	/**
	 * Sets the name of the view configuration file being used by the directory.
	 * @param configFile the view configuration file, which will be loaded 
	 * from the classpath.
	 */
	public void setConfigFile(String configFile)
    {
    	m_ConfigFile = configFile;
    }


	/**
	 * Parses the specified view configuration file into a list of Prelert Views.
	 * @param viewFile the file to parse, which will be loaded from the classpath.
	 * @return the list of views.
	 */
	protected void parseViewFile(String viewFile)
	{
		logger.info("Parsing view config file " + viewFile);
		
		InputStream stream = getClass().getClassLoader().getResourceAsStream(viewFile);
		
		Digester digester = new Digester();
		digester.setValidating(false);
		
		// Set the ViewDirectory object itself as the top object in the stack.
		digester.push(this);
		
		// Read in the Prelert-defined desktop views.
		digester.addRuleSet(new EvidenceViewDigesterRuleSet("prelertViews/evidenceView"));
		digester.addRuleSet(new ExceptionViewDigesterRuleSet("prelertViews/exceptionView"));
		digester.addRuleSet(new CausalityViewDigesterRuleSet("prelertViews/causalityView"));
		digester.addRuleSet(new HistoryViewDigesterRuleSet("prelertViews/historyView"));
		digester.addRuleSet(new UsageViewDigesterRuleSet("prelertViews/usageView"));
		
		digester.addSetNext("prelertViews/evidenceView", "addEvidenceView", "com.prelert.data.EvidenceView");
		digester.addSetNext("prelertViews/exceptionView", "setExceptionView", "com.prelert.data.ExceptionView");
		digester.addSetNext("prelertViews/causalityView", "setCausalityView", "com.prelert.data.CausalityView");
		digester.addSetNext("prelertViews/historyView", "setHistoryView", "com.prelert.data.HistoryView");
		digester.addSetNext("prelertViews/usageView", "addTimeSeriesView", "com.prelert.data.UsageView");
		
		
		// Read in any user-defined list or usage views.
		digester.addRuleSet(new ListViewDigesterRuleSet("prelertViews/userViews/listView"));
		digester.addRuleSet(new EvidenceViewDigesterRuleSet("prelertViews/userViews/evidenceView"));
		digester.addRuleSet(new UsageViewDigesterRuleSet("prelertViews/userViews/usageView"));
		
		digester.addSetNext("prelertViews/userViews/listView", "addUserDefinedView", "com.prelert.data.ListView");
		digester.addSetNext("prelertViews/userViews/evidenceView", "addUserDefinedView", "com.prelert.data.EvidenceView");
		digester.addSetNext("prelertViews/userViews/usageView", "addUserDefinedView", "com.prelert.data.UsageView");
		
		try
		{
			digester.parse(stream);
			
			if (getEvidenceViews() != null)
			{
				for (EvidenceView evidenceView : getEvidenceViews())
				{
					logger.info("Loaded Evidence View: " + evidenceView);
				}
			}
			logger.info("Loaded Exception View: " + getExceptionView());
			logger.info("Loaded Causality View: " + getCausalityView());
			logger.info("Loaded History View: " + getHistoryView());
			if (getTimeSeriesViews() != null)
			{
				for (UsageView usageView : getTimeSeriesViews())
				{
					logger.info("Loaded Time Series View: " + usageView);
				}
			}
			
			if (getUserViews() != null)
			{
				for (View userView : getUserViews())
				{
					logger.info("Loaded user-defined view: " + userView);
				}
			}
			
			logger.info("Total number of views loaded into directory: " + m_Views.size());
		}
		catch (Exception e)
		{
			logger.error("Error parsing " + viewFile, e);
		}
	}
	
	
	public static void main(String[] args)
	{
		// main() method for standalone testing.
		
		// Configure the log4j logging properties.
		PropertyConfigurator.configure("C:/eclipse/workspace/Desktop/config/log4j.properties");
		
		ViewDirectory viewDirectory = new ViewDirectory();
		viewDirectory.setConfigFile("config/views.xml");
		viewDirectory.init();
	}
}
