/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2009     *
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
import java.util.Iterator;
import java.util.List;

import com.extjs.gxt.ui.client.Events;
import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

import com.prelert.client.ClientUtil;
import com.prelert.client.Desktop;
import com.prelert.client.ViewWindow;
import com.prelert.client.event.ViewMenuItemListener;
import com.prelert.client.widget.ViewMenuItem;
import com.prelert.data.CausalityViewTool;
import com.prelert.data.DatePagingLoadConfig;
import com.prelert.data.DatePagingLoadResult;
import com.prelert.data.EventRecord;
import com.prelert.data.ExceptionListPagingLoader;
import com.prelert.data.ExceptionPagingLoadConfig;
import com.prelert.data.ExceptionView;
import com.prelert.data.HistoryViewTool;
import com.prelert.data.ListViewTool;
import com.prelert.data.TimeFrame;
import com.prelert.data.Tool;
import com.prelert.data.UsageViewTool;
import com.prelert.data.View;
import com.prelert.service.DatabaseServiceLocator;
import com.prelert.service.ExceptionQueryServiceAsync;
import com.prelert.service.GetExceptionDataRpcProxy;


/**
 * An extension of the Ext JS GXT Window for a Prelert Desktop Window displaying
 * an 'Exception List'. An Exception List is an evidence list that is automatically
 * filtered by the Prelert engine so only exceptions or anomalies at the defined
 * noise level, compared against the configured time window, are shown.
 * <p>
 * The Window contains a Grid component for paging through the list of evidence
 * retrieved from the database, with a toolbar that allows the users to page by 
 * date/time and select the noise level and exception time window e.g. Week, Day,
 * Hour or Minute.
 * 
 * @author Pete Harverson
 */
public class ExceptionListWindow extends ViewWindow
{
	private Desktop						m_Desktop;
	private ExceptionView				m_ExceptionView;
	private ExceptionQueryServiceAsync 	m_QueryService;
	
	private Grid<EventRecord>			m_Grid;
	private ExceptionListPagingLoader<ExceptionPagingLoadConfig, 
		DatePagingLoadResult<EventRecord>> m_Loader;

	
	
	/**
	 * Constructs a Window to display the specified exception view.
	 * @param desktop the parent Prelert desktop.
	 * @param exceptionView the View to be displayed in the window.
	 */
	public ExceptionListWindow(Desktop desktop, ExceptionView exceptionView)
	{
		m_Desktop = desktop;
		m_ExceptionView = exceptionView;
		
		m_QueryService = DatabaseServiceLocator.getInstance().getExceptionQueryService();
		
		setMinimizable(true);
		setMaximizable(true);
		setHeading(m_ExceptionView.getName());
		
		setSize(700, 525);
		setLayout(new FitLayout());
		setResizable(true);
		
		if (m_ExceptionView.getStyleId() != null)
	    {
			setIconStyle(m_ExceptionView.getStyleId()+ "-win-icon");
	    }
	    else
	    {
	    	setIconStyle("list-exception-win-icon");
	    }
		
		initComponents();
	}
	
	
	/**
	 * Creates and initialises the graphical components in the window.
	 */
	protected void initComponents()
	{
		// Create the paging loader.
		createLoader(m_ExceptionView.getNoiseLevel(), m_ExceptionView.getTimeWindow());
		
		ExceptionListToolBar toolBar = new ExceptionListToolBar(
				m_ExceptionView.getDataType(), TimeFrame.SECOND);
		toolBar.bind(m_Loader);
		setTopComponent(toolBar);

		// Create the client-side cache of EventRecord objects displayed in the ListView Grid.
		ListStore<EventRecord> listStore = new ListStore<EventRecord>(m_Loader);
		
		// Create the Grid which displays the data.
	    List<ColumnConfig> config = new ArrayList<ColumnConfig>();
	    List<String> allColumns = m_ExceptionView.getColumns();
	    ColumnConfig columnConf;
	    for (String columnName : allColumns)
		{
			columnConf = new ColumnConfig(columnName, columnName, 120);
			
			// Disable column sorting.
			columnConf.setSortable(false);
			config.add(columnConf);
		}
			
		// Create the Grid which displays the data.
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
	    List<Tool> viewMenuItems = m_ExceptionView.getContextMenuItems();
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
	    	    if (m_ExceptionView.getDoubleClickTool().equals(menuItem.getTool().getName()))
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
	 * Creates the paging loader responsible for loading of evidence data into
	 * the Exception List.
	 */
	protected void createLoader(int noiseLevel, TimeFrame timeWindow)
	{
		// Create the RpcProxy and PagingLoader to populate the list.
		GetExceptionDataRpcProxy<ExceptionPagingLoadConfig, DatePagingLoadResult<EventRecord>> proxy = 
			new GetExceptionDataRpcProxy<ExceptionPagingLoadConfig, DatePagingLoadResult<EventRecord>>(m_QueryService);
		
		m_Loader = new ExceptionListPagingLoader<ExceptionPagingLoadConfig, 
								DatePagingLoadResult<EventRecord>>(proxy);
		m_Loader.setDataType(m_ExceptionView.getDataType());
		m_Loader.setTimeFrame(TimeFrame.SECOND);
		m_Loader.setSortField("time");
		m_Loader.setDate(new Date());
		m_Loader.setSortDir(Style.SortDir.DESC);
		m_Loader.setRemoteSort(true);
		m_Loader.setNoiseLevel(noiseLevel);
		m_Loader.setTimeWindow(timeWindow);	
	}
	
	
	/**
	 * Returns the level of noise to act as the filter for the exception list.
	 * @return the noise level, a value from 0 to 100.
	 */
	public int getNoiseLevel()
	{
		return m_Loader.getNoiseLevel();
	}


	/**
	 * Sets the level of noise to act as the filter for the exception list.
	 * @param noiseLevel the noise level, a value from 0 to 100.
	 */
	public void setNoiseLevel(int noiseLevel)
	{
		m_Loader.setNoiseLevel(noiseLevel);
	}
	
	
	/**
	 * Returns the exception time window currently used in the window.
	 * @return exception time window of the current list e.g. week, day or hour.
	 */
	public TimeFrame getTimeWindow()
    {
    	return m_Loader.getTimeWindow();
    }


	/**
	 * Sets the exception time window. 
	 * Note that a separate call should be made to reload data into
	 * the window following the call to this method.
	 * @param timeWindow time window for the exceptions e.g. week, day or hour.
	 */
	public void setTimeWindow(TimeFrame timeWindow)
    {	
		m_Loader.setTimeWindow(timeWindow);
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
    	if (date != null)
    	{
    		m_Loader.loadAtTime(date, TimeFrame.SECOND);
    	}
    	else
    	{
    		m_Loader.load();
    	}
    }


	/**
	 * Returns the View displayed in the Window.
	 * @return the exception view displayed in the Window.
	 */
    public View getView()
    {
	    return m_ExceptionView;
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
	}
	
	
	/**
	 * Runs a tool against the hovered over usage record to open a List View.
	 * Currently only the opening of the Evidence View is supported, showing
	 * evidence at the time of the selected record.
	 * @param tool the ListViewTool that has been run by the user.
	 */
	public void runListViewTool(ListViewTool tool)
	{
		// Get the selected row. If no row is selected, do nothing.
		EventRecord selectedEvRecord = m_Grid.getSelectionModel().getSelectedItem();
		if (selectedEvRecord != null)
		{
	    	m_Desktop.openEvidenceView(tool.getViewToOpen(), selectedEvRecord.getId());
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
			Date date = ClientUtil.parseTimeField(selectedRow, TimeFrame.SECOND);
			m_Desktop.openHistoryView(m_ExceptionView.getDataType(), tool.getTimeFrame(), 
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
			Date date = ClientUtil.parseTimeField(selectedRow, TimeFrame.SECOND);
			
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
			
			m_Desktop.openUsageView(tool.getViewToOpen(), tool.getMetric(), source,
					tool.getAttributeName(), attributeValue, date, tool.getTimeFrame());
		}
	}

}
