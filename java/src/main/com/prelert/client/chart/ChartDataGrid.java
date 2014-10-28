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

package com.prelert.client.chart;

import java.util.ArrayList;
import java.util.List;

import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
import com.extjs.gxt.ui.client.widget.grid.GridView;

import com.prelert.client.ClientUtil;
import com.prelert.client.gxt.GridCheckBoxSelectionModel;


/**
 * Ext GWT (GXT) widget for displaying a grid of data shown in a chart. A checkbox
 * column allows the user to add or remove items from the chart, and a symbol column
 * displays the symbol used to represent that item of data in the chart.
 * @param <M> the model type
 * 
 * @author Pete Harverson
 */
public class ChartDataGrid<M extends ModelData> extends Grid<M>
{
	private GridCheckBoxSelectionModel<M> m_SelectionModel;
	
	
	/**
	 * Creates a grid widget for displaying chart data.
	 * @param store the store to hold the chart data.
	 * @param columns the list of columns to show in the grid.
	 */
	public ChartDataGrid(ListStore<M> store, List<ColumnConfig> columns)
	{
		m_SelectionModel = new GridCheckBoxSelectionModel<M>(true);
		
		ColumnModel columnModel = buildColumnModel(columns);
		
		this.store = store;
	    this.cm = columnModel;
	    this.view = new GridView();
	    disabledStyle = null;
	    baseStyle = "x-grid-panel";
	    disableTextSelection(true);
		
		setColumnReordering(true);
	    setLoadMask(true);
	    
	    setSelectionModel(m_SelectionModel);
		addPlugin(m_SelectionModel);
	}


	/**
	 * Deselects the specified item in the grid.
	 * @param item the item to be deselected.
	 * @param fireEvents <code>true</code> to fire a SelectionChangedEvent, 
	 * 	<code>false</code> to disable events.
	 */
	public void deselect(M item, boolean fireEvents)
	{	
		M storeModel = getStore().findModel(item);
		if (storeModel != null)
		{
			GridSelectionModel<M> selectionModel = getSelectionModel();
			
			selectionModel.setFiresEvents(fireEvents);
			selectionModel.deselect(storeModel);
			storeModel.set(ChartSymbolCellRenderer.DEFAULT_COLOR_PROPERTY, null);
			storeModel.set(ChartSymbolCellRenderer.DEFAULT_SHAPE_PROPERTY, null);
			getStore().update(storeModel);
			
			selectionModel.setFiresEvents(true);
		}
	
	}
	
	
	/**
	 * Deselects all selections in the grid.
	 * @param fireEvents <code>true</code> to fire a SelectionChangedEvent for
	 * 	each deselection, <code>false</code> to disable events.
	 */
	public void deselectAll(boolean fireEvents)
	{		
		List<M> selected = getSelectionModel().getSelectedItems();
		for (M model : selected)
		{
			deselect(model, fireEvents);
		}
	}
	
	
	/**
     * Sets the list of columns in the grid.
     * @param columns the list of columns for the data grid.
     */
	public void setColumns(List<ColumnConfig> columns)
	{
		ColumnModel columnModel = buildColumnModel(columns);
		
		reconfigure(getStore(), columnModel);
		if (isViewReady() == false) 
		{
			addListener(Events.ViewReady, new Listener<GridEvent<M>>(){

				@Override
                public void handleEvent(GridEvent<M> be)
                {
					// Ensure column header gets refreshed when view is ready.
					getView().refresh(true);
                }
				
			});
		     
		}
	}
	
	
	/**
	 * Builds the grid column model, adding checkbox and symbol columns to the
	 * specified list.
	 * @param columns list of data columns.
	 * @return the new grid column model.
	 */
	protected ColumnModel buildColumnModel(List<ColumnConfig> columns)
	{	
		// Set up the column model for the grid, inserting a checkbox column and
		// a symbol column to the supplied list.
		List<ColumnConfig> config = new ArrayList<ColumnConfig>();
		config.add(m_SelectionModel.getColumn());
		
		ColumnConfig symbolColumn = new ColumnConfig(
				 ChartSymbolCellRenderer.DEFAULT_COLOR_PROPERTY, 
				 ClientUtil.CLIENT_CONSTANTS.symbol(), 50);
		symbolColumn.setSortable(false);
		symbolColumn.setRenderer(new ChartSymbolCellRenderer());
		config.add(symbolColumn);
		
	    for (ColumnConfig column : columns)
		{
			config.add(column);
		}
	    
	    return new ColumnModel(config);
	}
	
}
