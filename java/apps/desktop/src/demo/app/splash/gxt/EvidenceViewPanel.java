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

package demo.app.splash.gxt;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.google.gwt.core.client.GWT;

import demo.app.client.ClientUtil;
import demo.app.client.ProbableCauseRenderer;
import demo.app.client.ViewMenuItem;
import demo.app.client.event.GXTEvents;
import demo.app.client.event.RequestViewEvent;
import demo.app.client.list.EvidenceViewFilterToolBar;
import demo.app.client.list.EvidenceViewPagingToolBar;
import demo.app.client.list.SeverityGridView;
import demo.app.data.CausalityViewTool;
import demo.app.data.DataSourceCategory;
import demo.app.data.DataSourceType;
import demo.app.data.DatePagingLoadResult;
import demo.app.data.EvidenceView;
import demo.app.data.EvidenceViewPagingLoader;
import demo.app.data.ListViewTool;
import demo.app.data.TimeFrame;
import demo.app.data.Tool;
import demo.app.data.UsageViewTool;
import demo.app.data.gxt.EvidenceModel;
import demo.app.service.EvidenceQueryServiceAsync;
import demo.app.service.GetEvidenceDataRpcProxy;
import demo.app.splash.service.QueryServiceLocator;

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
 * 
 * @author Pete Harverson
 */
public class EvidenceViewPanel extends ContentPanel
{
	private EvidenceQueryServiceAsync 	m_QueryService = null;
	
	private EvidenceView				m_EvidenceView;
	
	private Grid<EvidenceModel>			m_Grid;
	private EvidenceViewPagingLoader<DatePagingLoadResult<EvidenceModel>> 	m_Loader;
	
	
	
	/**
	 * Constructs a new EvidenceViewPanel which displays a grid of evidence data.
	 * @param evidenceView 	the Evidence View to be displayed in the panel.
	 * @param toolbarRegion	the region of the panel in which to add the paging
	 * 	toolbar - either NORTH or SOUTH.
	 */
	public EvidenceViewPanel(EvidenceView evidenceView, Style.LayoutRegion toolbarRegion)
	{	
		m_EvidenceView = evidenceView;
		
		m_QueryService = QueryServiceLocator.getInstance().getEvidenceQueryService();
		
		setHeaderVisible(false);
		setSize(650, 500);
		setLayout(new FitLayout());
		setBodyBorder(true);
		
		
		// Create the paging toolbar.
		createPagingToolBar(toolbarRegion);

		// Create the client-side cache of EventRecord objects 
		// displayed in the EvidenceView Grid.
		ListStore<EvidenceModel> listStore = new ListStore<EvidenceModel>(m_Loader);
			
		// Create the Grid which displays the data.
	    List<ColumnConfig> config = new ArrayList<ColumnConfig>();
	    List<String> allColumns = m_EvidenceView.getColumns();
	    ColumnConfig columnConf;
	    for (String columnName : allColumns)
		{
			columnConf = new ColumnConfig(columnName, columnName, 120);
			
			// Disable column sorting.
			columnConf.setSortable(false);
			config.add(columnConf);
		}
	    
	    ColumnModel columnModel = new ColumnModel(config);
	    m_Grid = new Grid<EvidenceModel>(listStore, columnModel);
	    m_Grid.setLoadMask(true);
	    m_Grid.setTrackMouseOver(false);
	    m_Grid.setView(new SeverityGridView(listStore));
	    m_Grid.getSelectionModel().setSelectionMode(Style.SelectionMode.SINGLE); 
	    
	    // Add a custom renderer for the Probable Cause column.
	    ColumnConfig probCauseCol = m_Grid.getColumnModel().getColumnById("probable_cause");
	    if (probCauseCol != null)
		{
	    	probCauseCol.setWidth(100);
	    	probCauseCol.setRenderer(ProbableCauseRenderer.getInstance());

		 	m_Grid.addListener(Events.CellClick, new Listener<GridEvent<EvidenceModel>>(){
		 		public void handleEvent(GridEvent<EvidenceModel> event) 
		 		{
		 			if(event.getTarget(".probcause", 1) != null)
		 			{
		 				EvidenceModel selectedRow = event.getModel();
		 				
		 				if (selectedRow != null)
		 				{
			 				RequestViewEvent<EvidenceModel> rve = 
								new RequestViewEvent<EvidenceModel>(EvidenceViewPanel.this);
	
							rve.setModel(selectedRow);
							fireEvent(GXTEvents.OpenCausalityViewClick, rve);
		 				}
		 			}
		 		} 
		 	});
		}
	    
	    
	    // Configure the right-click context menu.
	    Menu menu = new Menu();
	    
	    List<Tool> viewMenuItems = m_EvidenceView.getContextMenuItems();
	    if (viewMenuItems != null)
	    {
	    	ViewMenuItem menuItem;
	    	SelectionListener<MenuEvent> menuItemLsnr = new SelectionListener<MenuEvent>()
    	    {
				@Override
                public void componentSelected(MenuEvent ce)
                {	
					ViewMenuItem vmenuItem = (ViewMenuItem)(ce.getItem());
					fireViewMenuItemEvent(vmenuItem);	
                }
				
    	    };
	    	
	    	for (int i = 0; i < viewMenuItems.size(); i++)
	    	{
	    	    menuItem = new ViewMenuItem(viewMenuItems.get(i));
	    	    menuItem.addSelectionListener(menuItemLsnr);
	    	    menu.add(menuItem);
	    	}
	    }
	    
	    if (menu.getItemCount() > 0)
	    {
	    	m_Grid.setContextMenu(menu);
	    }
	    
	    add(m_Grid);
	}
	
	
	/**
	 * Creates the date paging toolbar for navigating through the evidence.
	 * @param toolbarRegion	the region of the panel in which to add the paging
	 * 	toolbar - either NORTH or SOUTH.
	 */
	protected void createPagingToolBar(Style.LayoutRegion toolbarRegion)
	{
		// Create the RpcProxy and PagingLoader to populate the list.
		GetEvidenceDataRpcProxy<DatePagingLoadResult<EvidenceModel>> proxy = 
			new GetEvidenceDataRpcProxy(m_QueryService);
		
		EvidenceViewPagingToolBar toolBar;
		if (m_EvidenceView.getFilterableAttributes() != null &&
				m_EvidenceView.getFilterableAttributes().size() > 0)
		{
			toolBar = new EvidenceViewFilterToolBar(m_EvidenceView);
		}
		else
		{
			toolBar = new EvidenceViewPagingToolBar(
					m_EvidenceView.getDataType(), m_EvidenceView.getTimeFrame());
		}
		
		m_Loader = new EvidenceViewPagingLoader<DatePagingLoadResult<EvidenceModel>>(proxy);
		m_Loader.setTimeFrame(TimeFrame.SECOND);
		m_Loader.setDataType(m_EvidenceView.getDataType());
		m_Loader.setFilterAttribute(m_EvidenceView.getFilterAttribute());
		m_Loader.setFilterValue(m_EvidenceView.getFilterValue());
		m_Loader.setSortField("time");
		m_Loader.setSortDir(Style.SortDir.DESC);
		m_Loader.setRemoteSort(true);
		m_Loader.addLoadListener(new LoadListener(){
		      @Override
            public void loaderLoad(LoadEvent le) 
		      {
		          onLoad(le);
		      }
		});
		toolBar.bind(m_Loader);
		
		if (toolbarRegion == LayoutRegion.SOUTH)
		{
			setBottomComponent(toolBar);
		}
		else
		{
			setTopComponent(toolBar);
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
	 * @param filterAttribute 	the name of the attribute on which the evidence
	 * 	should be filtered.	
	 * @param filterValue		the value of the filter on which the evidence
	 * 	should be filtered.
	 */
	public void setFilter(String filterAttribute, String filterValue)
	{
		m_Loader.setFilterAttribute(filterAttribute);
		m_Loader.setFilterValue(filterValue);
	}
	
	
	/**
	 * Refreshes the list of evidence, displaying the first (most recent) page 
	 * of evidence data.
	 */
	public void load()
	{
		m_Loader.load();
	}
	
	
	/**
	 * Loads the list of evidence, the top of row of which will match the 
	 * specified time.
	 * @param date date/time of evidence data to load.
	 */
    public void loadAtTime(Date date)
    {
    	m_Loader.loadAtTime(date, TimeFrame.SECOND);
    }

    
    /**
     * Loads the list of evidence, the top of row of which will match the 
	 * specified time and description.
	 * @param date date/time of evidence data to load.
	 * @param description the value of the description column to match on for the
	 * top row of evidence data that will be returned.
     */
    public void loadAtTime(Date date, String description)
    {
    	if (description != null)
    	{
    		m_Loader.loadForDescription(date, description, TimeFrame.SECOND);
    	}
    	else
    	{
    		loadAtTime(date);
    	}
    }
    
    
	/**
	 * Loads the list of evidence, the top of row of which will match the 
	 * specified id.
	 * @param evidenceId id for the top row of evidence data to be loaded.
	 */
    public void loadAtId(int evidenceId)
    {
    	m_Loader.loadAtId(evidenceId, TimeFrame.SECOND);
    }
    
	
	/**
	 * Updates the evidence window when new data is loaded.
	 * @param le LoadEvent received from the load operation.
	 */
	protected void onLoad(LoadEvent le) 
	{
		DatePagingLoadResult<EvidenceModel> loadResult = le.getData();
		m_Loader.setDate(loadResult.getDate());
		
		m_Grid.getView().getHeader().refresh();
	}
	
	
	/**
	 * Returns the selection model for the grid of evidence.
	 * @return the GridSelectionModel
	 */
	public GridSelectionModel<EvidenceModel> getSelectionModel()
	{
		return m_Grid.getSelectionModel();
	}
	
	
	/**
	 * Returns the item of evidence currently selected in the grid.
	 * @return the currently selected evidence, or <code>null</code> if no
	 * 		item of evidence is currently selected.
	 */
	public EvidenceModel getSelectedEvidence()
	{
		return getSelectionModel().getSelectedItem();
	}
	
	
	/**
	 * Fires an event from a ViewMenuItem against the selected row of evidence.
	 * @param viewMenuItem the ViewMenuItem that has been selected.
	 */
	protected void fireViewMenuItemEvent(ViewMenuItem viewMenuItem)
	{
		EvidenceModel selectedRow = getSelectedEvidence();
		if (selectedRow != null)
		{
			Tool tool = viewMenuItem.getTool();
			Date date = ClientUtil.parseTimeField(selectedRow, m_EvidenceView.getTimeFrame());
			
			RequestViewEvent<EvidenceModel> rve = 
				new RequestViewEvent<EvidenceModel>(this);
			rve.setOpenAtTime(date);
			rve.setSourceName(selectedRow.getSource());
			rve.setModel(selectedRow);
			
			// Note that GWT does not support reflection, so need to use getClass().
			if (tool.getClass() == CausalityViewTool.class)
			{
				fireEvent(GXTEvents.OpenCausalityViewClick, rve);
			}
			else if (tool.getClass() == ListViewTool.class)
			{
				ListViewTool listViewTool = (ListViewTool)tool;
				String viewToOpen = listViewTool.getViewToOpen();
				
				DataSourceType dsType = new DataSourceType(
						viewToOpen, DataSourceCategory.NOTIFICATION);
				rve.setViewToOpenDataType(dsType);
				fireEvent(GXTEvents.OpenNotificationViewClick, rve);
			}
			else if (tool.getClass() == UsageViewTool.class)
			{
				UsageViewTool timeSeriesViewTool = (UsageViewTool)tool;
				String viewToOpen = timeSeriesViewTool.getViewToOpen();
				
				DataSourceType dsType = new DataSourceType(
						viewToOpen, DataSourceCategory.TIME_SERIES);
				
				rve.setViewToOpenDataType(dsType);
				fireEvent(GXTEvents.OpenTimeSeriesViewClick, rve);
			}
		}
	}

}
