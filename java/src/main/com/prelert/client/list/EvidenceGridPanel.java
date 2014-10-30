/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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
import com.extjs.gxt.ui.client.widget.Component;
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
import com.extjs.gxt.ui.client.widget.menu.SeparatorMenuItem;
import com.extjs.gxt.ui.client.widget.table.DateTimeCellRenderer;
import com.google.gwt.i18n.client.DateTimeFormat;

import com.prelert.client.ClientUtil;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.RequestViewEvent;
import com.prelert.client.gxt.ViewMenuItem;
import com.prelert.data.DataSourceType;
import com.prelert.data.Evidence;
import com.prelert.data.PropertyNames;
import com.prelert.data.TimeFrame;
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
 * </dl>
 * 
 * @author Pete Harverson
 */
public class EvidenceGridPanel extends ContentPanel
{
	protected ModelDatePagingToolBar<EvidenceModel> m_ToolBar;
	protected Grid<EvidenceModel>			m_Grid;
	protected Menu							m_GridContextMenu;
	private SelectionListener<MenuEvent>	m_ViewMenuItemListener;
	protected EvidencePagingLoader 			m_Loader;
	private LoadListener					m_LoadListener;
	
	private TimeFrame						m_TimeFrame;			
	
	
	/**
	 * Constructs a new panel for searching for evidence matching criteria to
	 * be entered by the user.
	 * @param loader <code>EvidencePagingLoader</code> that will be used to load
	 * 	evidence data for the grid.
	 */
	public EvidenceGridPanel(EvidencePagingLoader loader)
	{	
		m_TimeFrame = TimeFrame.SECOND;
		
		setSize(650, 510);
		setLayout(new FitLayout());
		setBodyBorder(true);
		
		m_Loader = loader;
		m_LoadListener = new LoadListener()
		{
			@Override
			public void loaderLoad(LoadEvent le) 
			{
				onLoad(le);
			}
		};
		
		m_Loader.addLoadListener(m_LoadListener);
		
		m_ToolBar = new ModelDatePagingToolBar<EvidenceModel>(
				Evidence.getTimeColumnName(m_TimeFrame), PropertyNames.ID);
		m_ToolBar.bind(loader);
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
	    
	    createResultsGrid(new ColumnModel(new ArrayList<ColumnConfig>()));
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
	    m_Grid.setView(new SeverityGridView<EvidenceModel>(listStore));
	    m_Grid.getSelectionModel().setSelectionMode(Style.SelectionMode.SINGLE); 
	    
    	// Listen for clicks on a 'Show Analysis' icon.
	 	m_Grid.addListener(Events.CellClick, new Listener<GridEvent<EvidenceModel>>(){
	 		public void handleEvent(GridEvent<EvidenceModel> event) 
	 		{
	 			if(event.getTarget(".prl-analysis-link", 1) != null)
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
	    m_Grid.addListener(Events.ContextMenu, new Listener<GridEvent<EvidenceModel>>()
		{
			public void handleEvent(GridEvent<EvidenceModel> be)
			{
				// Only show the context menu if a row is clicked on.
				if (be.getRowIndex() == -1)
				{
					be.setCancelled(true);
				}
			}
		});

	    add(m_Grid);
	    
	    layout();
	    
	    m_Grid.addListener(Events.ViewReady, new Listener<GridEvent<EvidenceModel>>(){

			@Override
            public void handleEvent(GridEvent<EvidenceModel> be)
            {
				// Ensure column header gets refreshed when view is ready.
				m_Grid.getView().refresh(true);
            }
			
		});
	}
	
	
	/**
	 * Returns the paging loader being used to load evidence into the grid.
	 * @return the <code>EvidencePagingLoader</code> being used by the grid.
	 */
	public EvidencePagingLoader getLoader()
	{
		return m_Loader;
	}
	

	/**
     * Sets the list of data types that can be linked to from row a selected row
     * of evidence, adding a set of 'Show <data type>' items to the context menu.
     * @param dataSourceTypes list of data types which should be accessible from the grid.
     */
    public void setLinkToDataTypes(List<DataSourceType> dataSourceTypes)
    {
    	if (dataSourceTypes.size() > 0)
    	{
	    	if (m_ViewMenuItemListener == null)
			{
				m_ViewMenuItemListener = new SelectionListener<MenuEvent>()
	    	    {
					@Override
	                public void componentSelected(MenuEvent ce)
	                {	
						ViewMenuItem vmenuItem = (ViewMenuItem)(ce.getItem());
						fireViewMenuItemEvent(vmenuItem);	
	                }
					
	    	    };
	    	    
	    	    m_GridContextMenu.add(new SeparatorMenuItem());
			}
	    	else
	    	{
	    		// Remove any existing view menu items.
	    		List<Component> menuItems = m_GridContextMenu.getItems();
	    		List<Component> viewMenuItems = new ArrayList<Component>();
	    		for(Component item : menuItems)
	    		{
	    			if (item.getClass() == ViewMenuItem.class)
	    			{
	    				viewMenuItems.add(item);
	    			}
	    		}
	    		
	    		for(Component viewMenuItem : viewMenuItems)
	    		{
	    			m_GridContextMenu.remove(viewMenuItem);
	    		}
	    			 
	    	}
	    	
	    	// Add an item for each type to the end of context menu.
	    	ViewMenuItem menuItem;
	    	for (DataSourceType dataType : dataSourceTypes)
	    	{
	    	    menuItem = new ViewMenuItem(dataType);
	    	    menuItem.addSelectionListener(m_ViewMenuItemListener);
	    	    m_GridContextMenu.add(menuItem);
	    	}
    	}
    }
	
	
	/**
	 * Loads the list of evidence for the current configuration.
	 */
	public void load()
	{
		m_Loader.load();
	}
	
	
	/**
	 * Loads the first page of evidence for the current configuration.
	 */
	public void loadFirstPage()
	{
		m_Loader.loadFirstPage();
	}
	
	
	/**
	 * Loads the list of evidence, the top of row of which will match the 
	 * specified time.
	 * @param time date/time of evidence data to load.
	 */
    public void loadAtTime(Date time)
    {
    	m_Loader.setTime(time);
    	load();
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
			m_Loader.setTime(loadResultDate);
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
	 * Returns the data type of evidence being loaded in the grid.
	 * @return the data type, such as 'p2psmon_users' or 'system_udp',
	 * 	or <code>null</code> if loading evidence across all data types.
	 */
	public String getDataType()
	{
		return m_Loader.getDataType();
	}
	
	
	/**
	 * Sets the data type of evidence being displayed in the grid, which will
	 * be used from the next load operation.
	 * @param dataType the data type, such as 'p2psmon_users' or 'system_udp',
	 * 	or <code>null</code> to load evidence across all data types.
	 */
	public void setDataType(String dataType)
	{
		m_Loader.setDataType(dataType);
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
	    	columnModel.getColumnById(Evidence.getTimeColumnName(m_TimeFrame));
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
	    
	    // Add a custom renderer for the 'analysis' column to link to the Analysis view.
	    ColumnConfig probCauseCol = columnModel.getColumnById(
	    		com.prelert.data.Evidence.COLUMN_NAME_PROBABLE_CAUSE);
	    if (probCauseCol != null)
		{
	    	probCauseCol.setWidth(100);
	    	probCauseCol.setHeader(ClientUtil.CLIENT_CONSTANTS.columnProbableCause());
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
	 * Rebuilds the grid view, including the header, using the current 
	 * configuration and data.
	 */
	public void refreshGridView()
    {
    	m_Grid.getView().refresh(true);
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
			DataSourceType dataType = viewMenuItem.getDataType();
			Date date = selectedRow.getTime(m_TimeFrame);
			
			RequestViewEvent<EvidenceModel> rve = 
				new RequestViewEvent<EvidenceModel>(this);
			rve.setOpenAtTime(date);
			rve.setSourceName(selectedRow.getSource());
			rve.setModel(selectedRow);
			
			switch (dataType.getDataCategory())
			{
				case NOTIFICATION:
					rve.setViewToOpenDataType(dataType);
					fireEvent(GXTEvents.OpenNotificationViewClick, rve);
					break;
					
				case TIME_SERIES:
					rve.setViewToOpenDataType(dataType);
					fireEvent(GXTEvents.OpenTimeSeriesViewClick, rve);
					break;
					
				case TIME_SERIES_FEATURE:
					break;
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
		AttributeListDialog dialog = AttributeListDialog.getInstance();
		dialog.showEvidenceAttributes(evidence.getId());
	}
	
}
