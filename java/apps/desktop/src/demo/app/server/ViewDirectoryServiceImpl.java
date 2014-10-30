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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import demo.app.dao.EvidenceDAO;
import demo.app.data.CausalityView;
import demo.app.data.DataSourceType;
import demo.app.data.DesktopViewConfig;
import demo.app.data.EvidenceView;
import demo.app.data.ExceptionView;
import demo.app.data.HistoryView;
import demo.app.data.ListView;
import demo.app.data.TimeFrame;
import demo.app.data.UsageView;
import demo.app.data.View;
import demo.app.service.ViewDirectoryService;


/**
 * Server-side implementation of the service for retrieving information from
 * the Prelert View directory.
 * @author Pete Harverson
 */
public class ViewDirectoryServiceImpl extends RemoteServiceServlet 
	implements ViewDirectoryService
{
	static Logger logger = Logger.getLogger(ViewDirectoryServiceImpl.class);
	
	private ViewDirectory		m_ViewDirectory;
	
	private EvidenceDAO 		m_EvidenceDAO;
    
    
	/**
	 * Returns the desktop view configuration which encapsulates details on the
	 * views that will be created on the Prelert desktop.
	 * @return the view configuration to load on desktop initialisation.
	 */
	public DesktopViewConfig getDesktopViewConfig()
	{
		DesktopViewConfig desktopConfig = new DesktopViewConfig();
		
		List<String> evidenceColumns = null;
		
		List<EvidenceView> evidenceViews = m_ViewDirectory.getEvidenceViews();
		if (evidenceViews != null)
		{
			// Set the list of display columns and filterable columns
			// in each evidence view.
			List<String> filterableAttributes = null;
			
			for (EvidenceView evidenceView : evidenceViews)
			{
				evidenceColumns = m_EvidenceDAO.getAllColumns(
						evidenceView.getDataType(), evidenceView.getTimeFrame());
				filterableAttributes = m_EvidenceDAO.getFilterableColumns(
						evidenceView.getDataType(), true, true);
				evidenceView.setColumns(evidenceColumns);
				evidenceView.setFilterableAttributes(filterableAttributes);
			}
			
			desktopConfig.setEvidenceViews(evidenceViews);
		}
		
		ExceptionView exceptionView = m_ViewDirectory.getExceptionView();
		if (exceptionView != null)
		{
			// Set the list of display columns in the exception view.
			if (evidenceColumns == null)
			{
				evidenceColumns = m_EvidenceDAO.getAllColumns(
						exceptionView.getDataType(), TimeFrame.SECOND);
			}
			
			exceptionView.setColumns(evidenceColumns);
			desktopConfig.setExceptionView(exceptionView);
		}
		
		CausalityView causalityView = m_ViewDirectory.getCausalityView();
		desktopConfig.setCausalityView(causalityView);
		
		HistoryView historyView = m_ViewDirectory.getHistoryView();
		if (historyView != null)
		{
			// Set the list of display columns for each of the Evidence Views.
			// Note that the SECOND view columns will depend on which data type
			// is being viewed.
			HashMap<String, EvidenceView> viewsMap = historyView.getEvidenceViews();
			Collection<EvidenceView> historyEvViews = viewsMap.values();
			Iterator<EvidenceView> viewIter = historyEvViews.iterator();
			EvidenceView historyEvView;
			while (viewIter.hasNext())
			{
				historyEvView = viewIter.next();
				List<String> viewColumns = m_EvidenceDAO.getAllColumns(
						historyView.getDataType(),historyEvView.getTimeFrame());
				historyEvView.setColumns(viewColumns);
			}
			
			desktopConfig.setHistoryView(historyView);
		}
		
		List<UsageView> timeSeriesViews = m_ViewDirectory.getTimeSeriesViews();
		if (timeSeriesViews != null && timeSeriesViews.size() > 0)
		{
			desktopConfig.setTimeSeriesViews(timeSeriesViews);
		}
		
		return desktopConfig;
	}
    
   
    /**
     * Creates and returns a new view, based on the view with the specified name,
     * and setting the supplied filter attribute and filter value on the new view.
     * @return new drill-down view based on the supplied view and filter.
     * @throws NullPointerException if there is no View in the View Directory with the
     * given name.
     */
    public View getDrillDownView(String viewName, String filterAttribute,
            String filterValue) throws NullPointerException
    {
		// Need to create a new View based on the one specified, appending
    	// specified filter attribute and value.
    	logger.debug("getDrillDownView(" + viewName + "," + filterAttribute + "," + filterValue + ")");
    	View drillDownView = null;
    	
		View baseView = m_ViewDirectory.getView(viewName);
		logger.debug("getDrillDownView() baseView: " + baseView);
		
		if (baseView != null)
		{
			drillDownView = baseView.createCopyAndAppendFilter(filterAttribute, filterValue);
			logger.debug("getDrillDownView() returning view " + drillDownView);
		}
		else
		{
			logger.warn("getDrillDownView() - no view found in the directory with name: " + viewName);
			throw new NullPointerException("No view found in directory with name: " + viewName);
		}

		return drillDownView;
    }
    
    
	/**
	 * Returns the Evidence History View from the View Directory.
	 * @return the History View or <code>null</code> if no History View has been
	 * 			configured.
	 */
    public HistoryView getHistoryView()
    {
    	return m_ViewDirectory.getHistoryView();
    }


	/**
	 * Returns the directory of configured views.
	 * @return ViewDirectory.
	 */
    public ViewDirectory getViewDirectory()
    {
	    return m_ViewDirectory;
    }


	/**
	 * Sets the directory of configured views.
	 * @param ViewDirectory of configured views.
	 */
    public void setViewDirectory(ViewDirectory directory)
    {
	    m_ViewDirectory = directory;
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

}
