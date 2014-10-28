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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.Events;
import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.data.*;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.menu.Menu;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.client.Desktop;
import com.prelert.client.ViewWindow;
import com.prelert.client.event.ViewMenuItemListener;
import com.prelert.client.widget.ViewMenuItem;
import com.prelert.data.*;
import com.prelert.data.gxt.GridRowInfo;
import com.prelert.service.*;


/**
 * An extension of the Ext JS GXT Window for a Prelert Desktop Window displaying
 * a list of evidence records. The Window contains a Grid component for paging 
 * through a list of records retrieved from the database, with a toolbar that 
 * allows the users to page by date/time.
 * @author Pete Harverson
 */
public class EvidenceViewWindow extends ViewWindow
{
	private Desktop					m_Desktop;
	private EvidenceView			m_EvidenceView;
	
	private EvidenceQueryServiceAsync 	m_QueryService = null;
	
	private Grid<EventRecord>		m_Grid;
	private EvidenceViewPagingLoader<EvidencePagingLoadConfig, DatePagingLoadResult> m_Loader;
	
	
	/**
	 * Constructs an empty EvidenceViewWindow.
	 * @param desktop the parent Prelert desktop.
	 */
	public EvidenceViewWindow(Desktop desktop)
	{
		m_Desktop = desktop;
		m_QueryService = DatabaseServiceLocator.getInstance().getEvidenceQueryService();
		
		setCloseAction(CloseAction.HIDE);
		setMinimizable(true);
		setMaximizable(true);
		setSize(650, 525);
		setLayout(new FitLayout());
		setResizable(true);
		
		setIconStyle("list-evidence-win-icon");
	}
	
	
	/**
	 * Constructs an EvidenceViewWindow to display the specified view.
	 * @param desktop the parent Prelert desktop.
	 * @param evidenceView the View to be displayed in the window.
	 */
	public EvidenceViewWindow(Desktop desktop, EvidenceView evidenceView)
	{
		this(desktop);
		
		m_EvidenceView = evidenceView;
		initViewParameters(evidenceView);
		
		// Create the paging toolbar.
		createPagingToolBar();

		// Create the client-side cache of EventRecord objects 
		// displayed in the EvidenceView Grid.
		ListStore<EventRecord> listStore = new ListStore<EventRecord>(m_Loader);
			
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
	    m_Grid = new Grid<EventRecord>(listStore, columnModel);
	    m_Grid.setLoadMask(true);
	    m_Grid.setBorders(true);
	    m_Grid.setTrackMouseOver(false);
	    m_Grid.setView(new SeverityGridView(listStore));
	    m_Grid.getSelectionModel().setSelectionMode(Style.SelectionMode.SINGLE);
	    
	    // Add a custom renderer for the Probable Cause column.
	    ColumnConfig probCauseCol = m_Grid.getColumnModel().getColumnById("probable_cause");
	    if (probCauseCol != null)
		{
	    	probCauseCol.setWidth(100);
	    	probCauseCol.setRenderer(ProbableCauseCellRenderer.getInstance());

		 	m_Grid.addListener(Events.CellClick, new Listener<GridEvent>(){
		 		public void handleEvent(GridEvent event) 
		 		{
		 			if(event.getTarget(".probcause", 1) != null)
		 			{
						ListStore<EventRecord> gridStore = m_Grid.getStore();
		 				final EventRecord selectedRec = gridStore.getAt(event.rowIndex);
		 				
		 				DeferredCommand.addCommand(new Command()
						{
							public void execute()
							{
				 				m_Desktop.openCausalityView(selectedRec);
							}
						});	
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
	    	ViewMenuItemListener<ComponentEvent> menuItemLsnr;
	    	
	    	for (int i = 0; i < viewMenuItems.size(); i++)
	    	{
	    	    menuItem = new ViewMenuItem(viewMenuItems.get(i));
	    	    menuItemLsnr = new ViewMenuItemListener<ComponentEvent>(menuItem, this);
	    	    menuItem.addSelectionListener(menuItemLsnr);
	    	    menu.add(menuItem);
	    	    
	    	    // Check if this menu item's tool should be run on double-click.
	    	    if (m_EvidenceView.getDoubleClickTool().equals(menuItem.getTool().getName()))
	    	    {
	    	    	m_Grid.addListener(Events.CellDoubleClick, menuItemLsnr);
	    	    }
	    	}
	    }
	    
	    if (menu.getItemCount() > 0)
	    {
	    	m_Grid.setContextMenu(menu);
	    }
	    
	    add(m_Grid);
	}
	
	
	/**
	 * Initialises the display parameters for the View (window heading
	 * and icon style).
	 * @param view the View to be displayed in the Window.
	 */
	protected void initViewParameters(EvidenceView view)
	{
		setHeading(m_EvidenceView.getName());
		
		if (m_EvidenceView.getStyleId() != null)
	    {
			setIconStyle(m_EvidenceView.getStyleId()+ "-win-icon");
	    }
	    else
	    {
	    	setIconStyle("list-evidence-win-icon");
	    }
	}
	
	
	/**
	 * Creates the date paging toolbar for navigating through the evidence.
	 */
	protected void createPagingToolBar()
	{
		// Create the RpcProxy and PagingLoader to populate the list.
		GetEvidenceRecordsRpcProxy<EvidencePagingLoadConfig, DatePagingLoadResult> proxy = 
			new GetEvidenceRecordsRpcProxy(m_QueryService);
		
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
		
		m_Loader = new EvidenceViewPagingLoader<EvidencePagingLoadConfig, DatePagingLoadResult>(proxy);
		m_Loader.setTimeFrame(m_EvidenceView.getTimeFrame());
		m_Loader.setDataType(m_EvidenceView.getDataType());
		m_Loader.setFilterAttribute(m_EvidenceView.getFilterAttribute());
		m_Loader.setFilterValue(m_EvidenceView.getFilterValue());

		
		// Flag the time sort field in the paging loader according to the 
		// ORDER BY column specified in the view.
		// Note that column sorting is disabled for the grid.
		String sortField = "time";
		List<SortInformation> viewSortInfo = m_EvidenceView.getDefaultOrderBy();
		if (viewSortInfo != null && viewSortInfo.size() > 0)
		{
			sortField = viewSortInfo.get(0).getColumnName();
		}
		m_Loader.setSortField(sortField);
		
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
		setTopComponent(toolBar);
	}
	
	
	/**
	 * Returns the View displayed in the Window.
	 * @return the list view displayed in the Window.
	 */
	@Override
    public EvidenceView getView()
	{
		return m_EvidenceView;
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
	@Override
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
    	m_Loader.loadAtTime(date, m_EvidenceView.getTimeFrame());
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
    		m_Loader.loadForDescription(date, description, m_EvidenceView.getTimeFrame());
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
	protected void onLoad(LoadEvent<DatePagingLoadConfig, DatePagingLoadResult<EventRecord>> le) 
	{
		// TO DO: When a page loads, check to see if a row is to be selected.
		/*
		ListViewLoadResult<EvidenceLog> loadResult = (ListViewLoadResult<EvidenceLog>)(le.data);
		if (loadResult.getSelectedRowIndex() != -1)
		{
			m_Grid.getSelectionModel().select(loadResult.getSelectedRowIndex());
		}
		*/


		// Set the date of the PagingLoader according to the date passed back in
		// the load result. This should correspond to the date of the first row of
		// evidence data returned.
		DatePagingLoadResult<EventRecord> loadResult = le.data;
		m_Loader.setDate(loadResult.getDate());
	}
	
	
	/**
	 * Runs a tool against the selected row of evidence. No action is taken if
	 * no row is currently selected.
	 * @param tool the tool that has been run by the user.
	 */
	@Override
    public void runTool(Tool tool)
	{
		// Note that GWT does not support reflection, so need to use getClass().
		if (tool.getClass() == ListViewTool.class)
		{
			runListViewTool((ListViewTool)tool);
		}
		else if (tool.getClass() == CausalityViewTool.class)
		{
			runCausalityViewTool((CausalityViewTool)tool);
		}
		else if (tool.getClass() == HistoryViewTool.class)
		{
			runHistoryViewTool((HistoryViewTool)tool);
		}
		else if (tool.getClass() == UsageViewTool.class)
		{
			runUsageViewTool((UsageViewTool)tool);
		}
		else if (tool.getClass() == ShowInfoTool.class)
		{
			runShowInfoTool((ShowInfoTool)tool);
		}
	}
	
	
	/**
	 * Runs a tool against the hovered over event record to open a list of
	 * evidence of a different data type. The other evidence window will open
	 * to display notifications at the time of the currently selected item of
	 * evidence.
	 * @param tool the ListViewTool that has been run by the user.
	 */
	public void runListViewTool(ListViewTool tool)
	{
		// Get the selected row. If no row is selected, do nothing.
		EventRecord selectedRow = m_Grid.getSelectionModel().getSelectedItem();
		if (selectedRow != null)
		{
			// Make sure the tool is for a different data type.
			String viewToOpen = tool.getViewToOpen();
			if (viewToOpen.equals(m_EvidenceView.getName()) == false)
			{
				Date date = ClientUtil.parseTimeField(
						selectedRow, m_EvidenceView.getTimeFrame());
				
				m_Desktop.openEvidenceView(viewToOpen, date, null);
			}
		}
	}
	
	
	/**
	 * Runs a tool against the selected row of evidence to launch the Causality View.
	 * No action is taken if no row is currently selected.
	 * @param tool the HistoryViewTool that has been run by the user.
	 */
	public void runCausalityViewTool(CausalityViewTool tool)
	{
		// Get the selected row. If no row is selected, do nothing.
		EventRecord selectedRow = m_Grid.getSelectionModel().getSelectedItem();
		if (selectedRow != null)
		{
			m_Desktop.openCausalityView(selectedRow);
		}
	}
	
	
	/**
	 * Runs a tool against the selected row of evidence to launch the History View.
	 * No action is taken if no row is currently selected.
	 * @param tool the HistoryViewTool that has been run by the user.
	 */
	public void runHistoryViewTool(HistoryViewTool tool)
	{
		// Get the selected row. If no row is selected, do nothing.
		EventRecord selectedRow = m_Grid.getSelectionModel().getSelectedItem();
		if (selectedRow != null)
		{
			Date date = ClientUtil.parseTimeField(
					selectedRow, m_EvidenceView.getTimeFrame());
			m_Desktop.openHistoryView(m_EvidenceView.getDataType(), tool.getTimeFrame(),
					selectedRow.getDescription(), date);
		}
	}
	
	
	/**
	 * Runs a View Tool against the selected row of evidence to open a Usage View.
	 * No action is taken if no row is currently selected.
	 * @param tool the UsageViewTool that has been run by the user.
	 */
	public void runUsageViewTool(UsageViewTool tool)
	{
		// Get the selected row. If no row is selected, do nothing.
		EventRecord selectedRow = m_Grid.getSelectionModel().getSelectedItem();
		if (selectedRow != null)
		{
			Date date = ClientUtil.parseTimeField(
					selectedRow, m_EvidenceView.getTimeFrame());
			
			// For the metric, look for a metric specified in the tool,
			// or a metric property in the selected row.
			String metric = tool.getMetric();
			if (metric == null)
			{
				metric = selectedRow.get("metric");
			}
			
			String source = null;
			if (tool.getSourceArg() != null)
			{
				source = selectedRow.get(tool.getSourceArg());
			}
			String attributeValue = null;
			if (tool.getAttributeValueArg() != null)
			{
				attributeValue = selectedRow.get(tool.getAttributeValueArg());
			}
			
			m_Desktop.openUsageView(tool.getViewToOpen(), metric, source,
					tool.getAttributeName(), attributeValue, date, tool.getTimeFrame());
		}
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

		final int evidenceId = selectedRow.getId();
		
		m_QueryService.getRowInfo(evidenceId, 
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
	        	
	    	    //String windowTitle = m_EvidenceView.getDataType();
	    	    String windowTitle = "Evidence";
	    	    windowTitle += " id ";
	        	windowTitle+= evidenceId;
	        	m_Desktop.openShowInfoWindow(windowTitle, modelData);
	        }
		});
		
	}

}
