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

package com.prelert.client.list;

import java.util.List;

import com.prelert.data.EvidenceView;
import com.prelert.data.TimeFrame;
import com.prelert.data.Tool;


/**
 * A GXT panel which displays a view of evidence data. The panel consists of
 * a grid component, with each item of evidence rendered as a row in the grid,
 * and a toolbar with paging and filter controls.
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>OpenCausalityViewClick</b> : RequestViewEvent<br>
 * <div>Fires after a link to the probable cause view is selected.</div>
 * <ul>
 * <li>component : this</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>OpenNotificationViewClick</b> : RequestViewEvent<br>
 * <div>Fires after a link to a notification view is selected.</div>
 * <ul>
 * <li>component : this</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>OpenTimeSeriesViewClick</b> : RequestViewEvent<br>
 * <div>Fires after a link to a time series view is selected.</div>
 * <ul>
 * <li>component : this</li>
 * </ul>
 * </dd>
 * </dl>
 * 
 * @author Pete Harverson
 */
public class EvidenceViewPanel extends EvidenceGridPanel
{

	private EvidenceView				m_EvidenceView;
	
	
	/**
	 * Constructs a new EvidenceViewPanel which displays a grid of evidence data.
	 * @param evidenceView 	the Evidence View to be displayed in the panel.
	 */
	public EvidenceViewPanel(EvidenceView evidenceView)
	{	
		super();
		
		setHeaderVisible(false);
		
		m_EvidenceView = evidenceView;
		
		// Create the toolbar depending on whether the view has filterable attributes.
		if (m_EvidenceView.getFilterableAttributes() != null &&
				m_EvidenceView.getFilterableAttributes().size() > 0)
		{
			m_ToolBar = new EvidenceViewFilterToolBar(m_EvidenceView);
		}
		else
		{
			m_ToolBar = new EvidenceViewPagingToolBar(m_EvidenceView.getTimeFrame());
		}
    	
    	setTopComponent(m_ToolBar);

		// Create the RpcProxy and PagingLoader to populate the list.
		GetEvidenceDataRpcProxy proxy = new GetEvidenceDataRpcProxy();
		
		EvidenceViewPagingLoader loader = new EvidenceViewPagingLoader(proxy);
		loader.setTimeFrame(TimeFrame.SECOND);
		loader.setDataType(m_EvidenceView.getDataType());
		loader.setFilterAttribute(m_EvidenceView.getFilterAttribute());
		loader.setFilterValue(m_EvidenceView.getFilterValue());
		loader.setRemoteSort(true);
		bind(loader);
		
		setColumns(m_EvidenceView.getColumns());
		
		// Add the 'Show Xxxxx View' context menu items configured in the View.
		List<Tool> viewTools = m_EvidenceView.getContextMenuItems();
	    if (viewTools != null)
	    {
	    	for (Tool viewTool : viewTools)
	    	{
	    		addGridTool(viewTool);
	    	}
	    }
	}
	
	
	/**
	 * Returns the View displayed in the Window.
	 * @return the list view displayed in the Window.
	 */
    public EvidenceView getView()
	{
		return m_EvidenceView;
	}
    
    
    /**
	 * Sets the name of the source (server) for the evidence data.
	 * <b>NB.</b> a separate call should be made to
	 * reload data into the window following the call to this method.
	 * @param source the name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 */
    public void setSource(String source)
    {
    	m_Loader.setSource(source);
    }
    
    
    /**
     * Returns the name of the source (server) of the evidence data in the list.
     * @return the name of the source (server) of evidence data or <code>null</code>
     * 		if the view is showing data from all sources.
     */
    public String getSource()
    {
    	return m_Loader.getSource();
    }
	
	
	/**
	 * Sets the filter for the evidence data. A separate call should be made to
	 * reload data into the window following the call to this method.
	 * @param filterAttribute 	attribute name on which the evidence should be filtered.	
	 * @param filterValue	attribute value on which the evidence should be filtered.
	 */
	public void setFilter(String filterAttributes, String filterValues)
	{
		m_Loader.setFilterAttribute(filterAttributes);
		m_Loader.setFilterValue(filterValues);
	}
	
	
	/**
	 * Loads the list of evidence using to its current configuration
	 * (date, source, filter etc).
	 */
    @Override
    public void load()
    {
    	m_Loader.loadAtTime(m_Loader.getDate(), TimeFrame.SECOND);
    }


	/**
	 * Loads the list of evidence, the top of row of which will match the 
	 * specified id.
	 * @param evidenceId id for the top row of evidence data to be loaded.
	 */
    public void loadAtId(int evidenceId)
    {
    	EvidenceViewPagingLoader pagingLoader = (EvidenceViewPagingLoader)m_Loader;
    	pagingLoader.loadAtId(evidenceId, TimeFrame.SECOND);
    }
	
}
