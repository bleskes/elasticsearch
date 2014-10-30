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

import java.util.*;

import com.extjs.gxt.ui.client.Events;
import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.data.*;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.*;
import com.extjs.gxt.ui.client.widget.grid.*;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.menu.*;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.Desktop;
import com.prelert.client.ExtGWTWindowFactory;
import com.prelert.client.ViewWindow;
import com.prelert.client.event.ViewMenuItemListener;
import com.prelert.client.widget.ViewMenuItem;
import com.prelert.data.EventRecord;
import com.prelert.data.ListView;
import com.prelert.data.ListViewLoadResult;
import com.prelert.data.ListViewTool;
import com.prelert.data.ShowInfoTool;
import com.prelert.data.Tool;
import com.prelert.data.UsageViewTool;
import com.prelert.data.View;
import com.prelert.data.ViewTool;
import com.prelert.data.gxt.GridRowInfo;
import com.prelert.service.*;


/**
 * An extension of the Ext JS GXT Window for a Prelert Desktop List View Window.
 * The Window contains a Grid component for paging through a list of records 
 * retrieved from the database.
 * @author Pete Harverson
 */
public class ListViewWindow extends ViewWindow
{
	private Desktop					m_Desktop;
	private ListView				m_ListView;
	private Grid<EventRecord>		m_Grid;
	private ListStore<EventRecord>	m_ListStore;
	private BasePagingLoader<PagingLoadConfig, ListViewLoadResult<EventRecord>> m_Loader;
	
	private ListViewQueryServiceAsync 	m_ListViewQueryService = null;
	
	
	/**
	 * Constructs an empty ListViewWindow.
	 * @param desktop the parent Prelert desktop.
	 */
	public ListViewWindow(Desktop desktop)
	{
		m_Desktop = desktop;
		m_ListViewQueryService = DatabaseServiceLocator.getInstance().getListViewQueryService();
		
		setCloseAction(CloseAction.HIDE);
		setMinimizable(true);
		setMaximizable(true);
		setSize(650, 525);
		setLayout(new FitLayout());
		setResizable(true);
		
		setIconStyle("list-evidence-win-icon");
	}
	
	
	/**
	 * Constructs a ListViewWindow to display the specified view.
	 * @param desktop the parent Prelert desktop.
	 * @param listView the View to be displayed in the window.
	 */
	public ListViewWindow(Desktop desktop, ListView listView)
	{
		this(desktop, listView, null, null);
	}
	
	
	/**
	 * Constructs a ListViewWindow to display the specified view, and highlight
	 * the row corresponding to the supplied filter on initial load.
	 * @param desktop the parent Prelert desktop.
	 * @param listView the View to be displayed in the window.
	 * @param selectRowFilter filter to select the row of interest, containing
	 * 		placeholders ('?') for substitution by the supplied filter parameters.
	 * @param selectRowFilterParams list of parameters for substitution into the
	 * 		select row filter.
	 */
	public ListViewWindow(Desktop desktop, ListView listView, 
			String selectRowFilterAttribute, String selectRowFilterValue)
	{
		this(desktop);
		
		initViewParameters(listView);
		
		// Create the RpcProxy and PagingLoader to populate the list.
		GetListRecordsRpcProxy proxy = new GetListRecordsRpcProxy(listView, 
				selectRowFilterAttribute, selectRowFilterValue);
		m_Loader = new BasePagingLoader<PagingLoadConfig, ListViewLoadResult<EventRecord>>(proxy);
		m_Loader.setRemoteSort(true);

		// Create the client-side cache of EventRecord objects displayed in the ListView Grid.
		m_ListStore = new ListStore<EventRecord>(m_Loader);
			
		// Create the Grid which displays the data.
	    List<ColumnConfig> config = new ArrayList<ColumnConfig>();
	    ColumnModel columnModel = new ColumnModel(config);
	    m_Grid = new Grid<EventRecord>(m_ListStore, columnModel);
	    m_Grid.setLoadMask(true);
	    m_Grid.setBorders(true);
	    m_Grid.setTrackMouseOver(false);
	    m_Grid.setView(new SeverityGridView(m_ListStore));
	    m_Grid.getSelectionModel().setSelectionMode(Style.SelectionMode.SINGLE);
	    
	    // Configure the columns in the grid.
	    configureListViewColumns();
	    
	    // When a page loads, check to see if a row is to be selected.
	    m_Loader.addLoadListener(new LoadListener(){
			public void loaderLoad(LoadEvent le)
			{
				ListViewLoadResult<EventRecord> loadResult = (ListViewLoadResult<EventRecord>)(le.data);
				if (loadResult.getSelectedRowIndex() != -1)
				{
					m_Grid.getSelectionModel().select(loadResult.getSelectedRowIndex());
				}
			}
		});
	    
	    
	    // Configure the right-click context menu.
	    Menu menu = new Menu();
	    List<Tool> viewMenuItems = m_ListView.getContextMenuItems();
	    if (viewMenuItems != null)
	    {
	    	ViewMenuItem menuItem;
	    	ViewMenuItemListener<ComponentEvent> menuItemLsnr;
	    	
	    	for (int i = 0; i < viewMenuItems.size(); i++)
	    	{
	    	    menuItem = new ViewMenuItem(viewMenuItems.get(i));
	    	    menuItemLsnr = new ViewMenuItemListener<ComponentEvent>(menuItem, this);
	    	    menuItem.addSelectionListener(menuItemLsnr);
	    	    menu.add(menuItem);
	    	    
	    	    // Check if this menu item's tool should be run on double-click.
	    	    if (m_ListView.getDoubleClickTool().equals(menuItem.getTool().getName()))
	    	    {
	    	    	m_Grid.addListener(Events.CellDoubleClick, menuItemLsnr);
	    	    }
	    	}
	    }
	    
	    if (menu.getItemCount() > 0)
	    {
	    	m_Grid.setContextMenu(menu);
	    }
		
	    // Add a paging toolbar.
		PagingToolBar toolBar = new PagingToolBar(20);
		toolBar.getMessages().setDisplayMsg("Rows {0} - {1} of {2}");
		toolBar.bind(m_Loader);
	    
	    add(m_Grid);
	    setTopComponent(toolBar);
	}
	
	
	/**
	 * Loads the data in the ListViewWindow according to the current 
	 * configuration (offset, limit and sort information).
	 */
	public void load()
	{
		m_Loader.load(m_Loader.getOffset(), m_Loader.getLimit());
	}
	
	
	/**
	 * Runs a tool against the selected row of evidence. No action is taken if
	 * no row is currently selected.
	 * @param tool the tool that has been run by the user.
	 */
	public void runTool(Tool tool)
	{
		// Note that GWT does not support reflection, so need to use getClass().
		if (tool.getClass() == ListViewTool.class)
		{
			runViewTool((ListViewTool)tool);
		}
		else if (tool.getClass() == UsageViewTool.class)
		{
			runViewTool((UsageViewTool)tool);
		}
		else if (tool.getClass() == ShowInfoTool.class)
		{
			runShowInfoTool((ShowInfoTool)tool);
		}
	}
	
	
	/**
	 * Runs a View Tool against the selected row of evidence to launch a new Desktop View.
	 * No action is taken if no row is currently selected.
	 * @param tool the ViewTool that has been run by the user.
	 */
	public void runViewTool(ViewTool tool)
	{	
		// Get the selected row. If no row is selected, do nothing.
		EventRecord selectedRecord = m_Grid.getSelectionModel().getSelectedItem();
		if (selectedRecord == null)
		{
			return;
		}
		
		String filterAttribute = null;
		String filterValue = null;
		if (tool.getClass() == ListViewTool.class)
		{
			// Get the value of the list view filter.
			ListViewTool listViewTool = (ListViewTool)tool;
			filterAttribute = listViewTool.getFilterAttribute();
			String filterArg = listViewTool.getFilterArg();
			if (filterArg != null)
			{
				filterValue = selectedRecord.get(filterArg).toString();
			}
		}
		
		String drillDownViewName = tool.getViewToOpen();
		
		ViewDirectoryServiceAsync viewDirectoryService = 
			DatabaseServiceLocator.getInstance().getViewDirectoryService();
		viewDirectoryService.getDrillDownView(drillDownViewName, filterAttribute, 
				filterValue, new ApplicationResponseHandler<View>(){

	        public void uponFailure(Throwable caught)
	        {
		        MessageBox.alert("Prelert - Error",
		                "Unable to load new view.", null);
	        }


	        public void uponSuccess(View view)
	        {
	        	ViewWindow viewWindow = ExtGWTWindowFactory.createWindow(m_Desktop, view);
	        	viewWindow.setCloseAction(CloseAction.CLOSE);
	        	viewWindow.load();
	        	m_Desktop.addWindow(viewWindow);
	        	viewWindow.show();

	        }
        });
	}
	
	
	/**
	 * Loads and opens the 'Show Info' window for the record with the specified
	 * row ID in the given list View.
	 * @param rowId ID of the row in the View for which to create a 'Show Info' window.
	 */
	public void runShowInfoTool(ShowInfoTool tool)
	{	
		EventRecord selectedRow = m_Grid.getSelectionModel().getSelectedItem();
		if (selectedRow == null)
		{
			return;
		}
		
		final ShowInfoTool showInfoTool = tool;
		final int evidenceId = selectedRow.getId();
		
		m_ListViewQueryService.getRowInfo(m_ListView, evidenceId, 
				new ApplicationResponseHandler<List<GridRowInfo>>(){

	        public void uponFailure(Throwable caught)
	        {
	        	MessageBox.alert("Prelert - Error",
		                "Error retrieving evidence info.", null);
	        }


	        public void uponSuccess(List<GridRowInfo> modelData)
	        {
	        	// NB. Jan 2010: In future could pass on active attributes set in
	        	// tool via showInfoTool.getActiveAttributes().
	        	
	    	    //String windowTitle = m_ListView.getDataType();
	    	    String windowTitle = "Evidence";
	    	    windowTitle += " id ";
	        	windowTitle+= evidenceId;
	        	m_Desktop.openShowInfoWindow(windowTitle, modelData);
	        }
		});
		
	}
	
	
	/**
	 * Returns the View displayed in the Window.
	 * @return the list view displayed in the Window.
	 */
	public View getView()
	{
		return m_ListView;
	}
	
	
	/**
	 * Initialises the display parameters for the View (window heading
	 * and icon style).
	 * @param view the View to be displayed in the Window.
	 * @param selectRowFilter filter to select the row of interest, containing
	 * 		placeholders ('?') for substitution by the supplied filter parameters.
	 * @param selectRowFilterParams list of parameters for substitution into the
	 * 		select row filter.
	 */
	protected void initViewParameters(ListView view)
	{
		m_ListView = view;
		
		StringBuilder viewName = new StringBuilder(m_ListView.getName());
		Object filterValue = view.getFilterValue();
		if (filterValue != null)
		{
			viewName.append(" - ");
			String param = filterValue.toString();
			if (param.length() > 14)
			{
				param = param.substring(0, 14);
			}
			viewName.append(param);
		}
		setHeading(viewName.toString());
		
		if (m_ListView.getStyleId() != null)
	    {
			setIconStyle(m_ListView.getStyleId()+ "-win-icon");
	    }
	    else
	    {
	    	setIconStyle("list-evidence-win-icon");
	    }
	}
	
	
	/**
	 * Returns the Grid being displayed in this ListViewWindow.
	 * @return the Grid displaying the EventRecord objects in this List View.
	 */
	public Grid<EventRecord> getGrid()
	{
		return m_Grid;
	}
	
	
	/**
	 * Configures the columns for the grid in the specified list View.
	 * The method makes a call to the server to obtain the list of columns
	 * from the database table linked to the specified list View. It also
	 * creates custom renderers and listeners for any columns as appropriate
	 * e.g. the 'probable_cause' column.
	 * @param listView the List View for which to configure the Grid columns.
	 * @param listViewGrid Ext GWT Grid object contained within the specified View.
	 */
	private void configureListViewColumns()
	{
		m_ListViewQueryService.getAllColumns(m_ListView, 
				new ApplicationResponseHandler<List<String>>(){

	        public void uponFailure(Throwable caught)
	        {
		        MessageBox.alert("Prelert - Error",
		                "Failed to get a response from the server.", null);
	        }


	        public void uponSuccess(List<String> columns)
	        {
	        	List<ColumnConfig> newConfig = new ArrayList<ColumnConfig>();
				
				ColumnConfig columnConf;
				String columnName;
				Iterator<String> colIterator = columns.iterator();
				while (colIterator.hasNext() == true)
				{
					columnName = colIterator.next();
					columnConf = new ColumnConfig(columnName, columnName, 120);
					
					// Set a custom renderer for a probability column to display
					// the percent value in a progress bar.
					// TO DO: tidy up the hardcoded column reference.
					if (columnName.equals("probability"))
					{
						columnConf.setRenderer(ProbabilityCellRenderer.getInstance());
					}
					newConfig.add(columnConf);
					
				}
				ColumnModel newColumnModel = new ColumnModel(newConfig);
				m_Grid.reconfigure(m_Grid.getStore(), newColumnModel);
				m_Grid.unmask();
	        }
        });

	}
}
