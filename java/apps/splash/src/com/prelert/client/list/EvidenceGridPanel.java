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

import com.extjs.gxt.ui.client.Style;
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
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.extjs.gxt.ui.client.widget.table.DateTimeCellRenderer;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;

import com.prelert.client.ClientUtil;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.RequestViewEvent;
import com.prelert.client.gxt.ViewMenuItem;
import com.prelert.data.CausalityViewTool;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.ListViewTool;
import com.prelert.data.TimeFrame;
import com.prelert.data.TimeSeriesViewTool;
import com.prelert.data.Tool;
import com.prelert.data.ViewTool;
import com.prelert.data.gxt.DatePagingLoadResult;
import com.prelert.data.gxt.EvidenceModel;


/**
 * Base class for a GXT panel which displays a grid of evidence data. The panel 
 * consists of a grid component, with each item of evidence rendered as a row in
 * the grid, and a toolbar with paging controls.
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
 * <dd><b>OpenViewClick</b> : RequestViewEvent<br>
 * <div>Fires after a link to a data view is selected.</div>
 * <ul>
 * <li>component : this</li>
 * </ul>
 * </dd>
 * 
 * </dl>
 * 
 * @author Pete Harverson
 */
public class EvidenceGridPanel extends ContentPanel
{
	protected EvidenceViewPagingToolBar 	m_ToolBar;
	protected Grid<EvidenceModel>			m_Grid;
	protected Menu							m_GridContextMenu;
	private SelectionListener<MenuEvent>	m_ContextMenuToolListener;
	protected EvidencePagingLoader 			m_Loader;
	private LoadListener					m_LoadListener;
	
	private TimeFrame					m_TimeFrame;			
	
	
	/**
	 * Constructs a new panel for searching for evidence matching criteria to
	 * be entered by the user.
	 */
	public EvidenceGridPanel()
	{	
		m_TimeFrame = TimeFrame.SECOND;
		
		setSize(650, 510);
		setLayout(new FitLayout());
		setBodyBorder(true);
		
		m_ToolBar = new EvidenceViewPagingToolBar(m_TimeFrame);	
		setTopComponent(m_ToolBar);
		
		// Create the default right-click context menu.
	    // containing a single 'Show Details' menu item.
		m_GridContextMenu = new Menu();
	    
	    MenuItem showDetailsItem = new MenuItem(ClientUtil.CLIENT_CONSTANTS.showDetails());
	    SelectionListener<MenuEvent> showDetailsListener = new SelectionListener<MenuEvent>()
	    {
			@Override
            public void componentSelected(MenuEvent ce)
            {
				EvidenceModel selected = getSelectedEvidence();
				if (selected != null)
				{
					showAttributesDialog(selected);
				}
            }
			
	    };
	    
	    showDetailsItem.addSelectionListener(showDetailsListener);
	    m_GridContextMenu.add(showDetailsItem);
	}
	
	
	/**
	 * Binds the EvidenceGridPanel to the specified paging loader.
	 * 
	 * @param loader the EvidencePagingLoader to load data for the grid.
	 */
	public void bind(EvidencePagingLoader loader)
	{
		if (m_Loader != null)
		{
			m_Loader.removeLoadListener(m_LoadListener);
		}
		
		m_Loader = loader;
		if (loader != null)
		{
			if (m_LoadListener == null)
			{
				m_LoadListener = new LoadListener()
				{
					@Override
					public void loaderLoad(LoadEvent le) 
					{
						onLoad(le);
					}
				};
			}
			
			m_Loader.addLoadListener(m_LoadListener);
		}
		
		m_ToolBar.bind(loader);
	}
	
	
	/**
	 * Creates the results grid.
	 * @param columnModel the ColumnModel for the grid.
	 */
	protected void createResultsGrid(ColumnModel columnModel)
	{
		// Create the EvidenceModel ListStore to hold the search results.
		ListStore<EvidenceModel> listStore = new ListStore<EvidenceModel>(m_Loader);
	    
	    m_Grid = new Grid<EvidenceModel>(listStore, columnModel);
	    m_Grid.setColumnReordering(true);
	    m_Grid.setLoadMask(true);
	    m_Grid.setTrackMouseOver(false);
	    m_Grid.setView(new SeverityGridView(listStore));
	    m_Grid.getSelectionModel().setSelectionMode(Style.SelectionMode.SINGLE); 
	    

	    // Add a custom renderer for the Probable Cause column.
	    ColumnConfig probCauseCol = columnModel.getColumnById(
	    		EvidenceModel.COLUMN_NAME_PROBABLE_CAUSE);
	    if (probCauseCol != null)
		{
	    	// Listen for clicks on the Probable Cause icon.
		 	m_Grid.addListener(Events.CellClick, new Listener<GridEvent<EvidenceModel>>(){
		 		public void handleEvent(GridEvent<EvidenceModel> event) 
		 		{
		 			if(event.getTarget(".probcause", 1) != null)
		 			{
		 				EvidenceModel selectedRow = event.getModel();
		 				
		 				if (selectedRow != null)
		 				{
			 				RequestViewEvent<EvidenceModel> rve = 
								new RequestViewEvent<EvidenceModel>(EvidenceGridPanel.this);
	
							rve.setModel(selectedRow);
							fireEvent(GXTEvents.OpenCausalityViewClick, rve);
		 				}
		 			}
		 		} 
		 	});
		}
	    
	    // Double-click on a row to open the Evidence Attributes dialog.
	    m_Grid.addListener(Events.RowDoubleClick, new Listener<GridEvent<EvidenceModel>>(){

			@Override
            public void handleEvent(GridEvent<EvidenceModel> be)
            {
				EvidenceModel selected = be.getModel();
				if (selected != null)
				{
					showAttributesDialog(selected);
				}
            }
	    	
	    });
	    
	    m_Grid.setContextMenu(m_GridContextMenu);

	    add(m_Grid);
	    
	    layout();
	}
	
	
	/**
	 * Adds the specified tool to the grid for running an action against a
	 * selected item of evidence.
	 * @param tool tool to add to the grid's context menu.
	 */
	public void addGridTool(Tool tool)
	{
		if (m_ContextMenuToolListener == null)
		{
			m_ContextMenuToolListener = new SelectionListener<MenuEvent>()
    	    {
				@Override
                public void componentSelected(MenuEvent ce)
                {	
					ViewMenuItem vmenuItem = (ViewMenuItem)(ce.getItem());
					fireViewMenuItemEvent(vmenuItem);	
                }
				
    	    };
		}
		
	    ViewMenuItem menuItem = new ViewMenuItem(tool);
	    menuItem.addSelectionListener(m_ContextMenuToolListener);
	    
	    m_GridContextMenu.add(menuItem);
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
	 * Updates the evidence window when new data is loaded.
	 * @param le LoadEvent received from the load operation.
	 */
	protected void onLoad(LoadEvent le) 
	{
		DatePagingLoadResult<EvidenceModel> loadResult = le.getData();
		
		// Set the date in the PagingLoader - generally to the date of the top row.
		Date loadResultDate = loadResult.getDate();
		if (loadResult.getDate() != null)
		{
			m_Loader.setDate(loadResultDate);
		}
	}
	
	
	/**
	 * Returns the evidence grid right-click context menu.
	 * @return the grid context menu.
	 */
	public Menu getGridContextMenu()
	{
		return m_GridContextMenu;
	}
	
	
	public TimeFrame getTimeFrame()
	{
		return m_TimeFrame;
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
	 * Selects the row of evidence in the grid with the specified id.
	 * @param evidenceId id of the evidence to select. If the grid does not contain
	 * an item of evidence matching this id, no action is taken.
	 */
	public void setSelectedEvidence(int evidenceId)
	{
		ListStore<EvidenceModel> listStore = m_Grid.getStore();
		EvidenceModel evidence = listStore.findModel("id", evidenceId);
		if (evidence != null)
		{
			getSelectionModel().select(evidence, false);
		}
	}
	
	
	/**
	 * Sets the list of columns that are displayed in the grid.
	 * @param columns the columns to display in the grid.
	 */
	public void setColumns(List<String> columns)
	{
		// Create the Grid which displays the data.
	    List<ColumnConfig> config = new ArrayList<ColumnConfig>();
	    ColumnConfig columnConf;
	    for (String columnName : columns)
		{
			columnConf = new ColumnConfig(columnName, columnName, 120);
			
			// Disable column sorting.
			columnConf.setSortable(false);
			config.add(columnConf);
		}
	    
	    ColumnModel columnModel = new ColumnModel(config);
	    
	    // Add a custom renderer for the 'time' column.
	    ColumnConfig timeCol = 
	    	columnModel.getColumnById(EvidenceModel.getTimeColumnName(m_TimeFrame));
	    if (timeCol != null)
	    {
	    	DateTimeFormat dateFormat = ClientUtil.getDateTimeFormat(m_TimeFrame);
	    	final DateTimeCellRenderer<Grid<EvidenceModel>> timeRenderer = 
	    		new DateTimeCellRenderer<Grid<EvidenceModel>>(dateFormat);
	    	GridCellRenderer<EvidenceModel> timeCellRenderer = new GridCellRenderer<EvidenceModel>() {

				@Override
                public Object render(EvidenceModel model, String property,
                        ColumnData config, int rowIndex, int colIndex,
                        ListStore<EvidenceModel> store, Grid<EvidenceModel> grid)
                {
					return timeRenderer.render(null, property, model.get(property));
                }   
	    		
	    	};
	    	timeCol.setRenderer(timeCellRenderer);
	    }
	    
	    // Add a custom renderer for the Probable Cause column.
	    ColumnConfig probCauseCol = columnModel.getColumnById(
	    		EvidenceModel.COLUMN_NAME_PROBABLE_CAUSE);
	    if (probCauseCol != null)
		{
	    	probCauseCol.setWidth(100);
	    	probCauseCol.setRenderer(ProbableCauseRenderer.getInstance());
		}
	    
	    if (m_Grid == null)
	    {
	    	createResultsGrid(columnModel);
	    }
	    else
	    {
	    	m_Grid.reconfigure(m_Grid.getStore(), columnModel);
	    }
	}
	
	
    /**
     * Adds a listener for load events from the evidence loader. 
     */
    public void addLoadListener(LoadListener listener)
    {
    	m_Loader.addLoadListener(listener);
    }


    /**
     * Removes a listener for load events from the evidence loader.
     */
    public void removeLoadListener(LoadListener listener)
    {
    	m_Loader.removeLoadListener(listener);
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
			Date date = selectedRow.getTime(m_TimeFrame);
			
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
			else if (tool.getClass() == ViewTool.class)
			{
				fireEvent(GXTEvents.OpenViewClick, rve);
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
			else if (tool.getClass() == TimeSeriesViewTool.class)
			{
				TimeSeriesViewTool timeSeriesViewTool = (TimeSeriesViewTool)tool;
				String viewToOpen = timeSeriesViewTool.getViewToOpen();
				
				DataSourceType dsType = new DataSourceType(
						viewToOpen, DataSourceCategory.TIME_SERIES);
				
				rve.setViewToOpenDataType(dsType);
				fireEvent(GXTEvents.OpenTimeSeriesViewClick, rve);
			}
		}
	}
	
	
	/**
	 * Opens the 'Show Attributes' dialog to display the details of the 
	 * specified item of evidence.
	 * @param evidence item of evidence to display in the dialog.
	 */
	protected void showAttributesDialog(EvidenceModel evidence)
	{	
		EvidenceAttributesDialog dialog = ClientUtil.getEvidenceAttributesDialog();
		dialog.setEvidenceId(evidence.getId());
		dialog.show();
	}
	
}
