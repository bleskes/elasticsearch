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

package com.prelert.client.incident;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.core.El;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.store.Store;
import com.extjs.gxt.ui.client.store.StoreEvent;
import com.extjs.gxt.ui.client.store.StoreListener;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.form.SimpleComboBox;
import com.extjs.gxt.ui.client.widget.form.SimpleComboValue;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.LabelToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Anchor;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.client.chart.ChartSymbolCellRenderer;
import com.prelert.client.list.CausalityEvidenceDialog;
import com.prelert.data.Attribute;
import com.prelert.data.gxt.CausalityAggregateModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.IncidentQueryServiceAsync;


/**
 * Ext GWT (GXT) widget for displaying a summary of an incident. The widget
 * consists of a toolbar allowing the user to group the causality data
 * from an incident by data type, source, description, plus any other attributes
 * by which the notifications and time series features have been correlated.
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>Store.BeforeDataChanged</b> : StoreEvent(store)<br>
 * <div>Fires just before the data cache in the summary grid is changed.</div>
 * <ul>
 * <li>store : the summary grid's store</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>Store.DataChange</b> : StoreEvent(store)<br>
 * <div>Fires when the data cache in the summary grid has changed, and a widget which 
 * is using the store as a ModelData cache should refresh its view.</div>
 * <ul>
 * <li>store : the summary grid's store</li>
 * </ul>
 * </dd>
 * 
 * </dl>
 * 
 * 
 * @author Pete Harverson
 */
public class IncidentSummaryGrid extends ContentPanel
{
	private IncidentQueryServiceAsync	m_IncidentQueryService;
	private int							m_EvidenceId;
	private List<String>				m_GroupingAttributes;
	private ListStore<CausalityAggregateModel>		m_DataStore;
	
	private ToolBar							m_GridToolBar;
	private SimpleComboBox<String>			m_GroupByCombo;
	private boolean							m_AddShowAll;
	private Grid<CausalityAggregateModel> 	m_Grid;
	private int 							m_MaxGridRows = -1;
	
	
	/**
	 * Creates a new grid for summarising the data in an incident by different attributes.
	 * @param inExplorer <code>true</code> if the grid is to be used in the Causality
	 * 	Explorer. If <code>false</code>, a symbol column is added, using a 
	 * 	ChartSymbolCellRenderer to indicate the colour and shape of symbols used for 
	 * 	plotting each aggregate row in a chart.
	 * @see ChartSymbolCellRenderer
	 */
	public IncidentSummaryGrid(boolean inExplorer)
	{
		super(new FitLayout());
		
		m_AddShowAll = inExplorer;
		m_IncidentQueryService = AsyncServiceLocator.getInstance().getIncidentQueryService();
		
		setHeaderVisible(false);
		
		// Add a toolbar to hold the 'Group by' ComboBox.
		m_GridToolBar = new ToolBar();
		m_GridToolBar.setSpacing(2);
		
		m_GroupByCombo = new SimpleComboBox<String>();
		m_GroupByCombo.setWidth(130);
		m_GroupByCombo.setTriggerAction(TriggerAction.ALL); 
		m_GroupByCombo.setEditable(false);
		m_GroupByCombo.setListStyle("prl-combo-list");
		
		m_GroupByCombo.addListener(Events.SelectionChange, new SelectionChangedListener<SimpleComboValue<String>>(){

	        @Override
	        public void selectionChanged(SelectionChangedEvent<SimpleComboValue<String>> se)
	        {
	        	load();
	        }
        });
		m_GroupByCombo.enableEvents(false);
		
		
		m_GridToolBar.add(new LabelToolItem(ClientUtil.CLIENT_CONSTANTS.fieldGroupBy()));
		m_GridToolBar.add(m_GroupByCombo);
		setTopComponent(m_GridToolBar);

		List<ColumnConfig> config = new ArrayList<ColumnConfig>();
	    
		ColumnConfig summaryColumn = new ColumnConfig("summary", "summary", 150);
		
		// If not in the Causality Explorer, 
		if (inExplorer == false)
		{
		    ColumnConfig symbolColumn = new ColumnConfig(
					 ChartSymbolCellRenderer.DEFAULT_COLOR_PROPERTY, 
					 ClientUtil.CLIENT_CONSTANTS.symbol(), 50);
			symbolColumn.setSortable(false);
			symbolColumn.setFixed(true);
			symbolColumn.setRenderer(new ChartSymbolCellRenderer());
			config.add(symbolColumn);
			
			// Add hyperlink to the summary column to open the causality evidence dialog.
			summaryColumn.setRenderer(new GridCellRenderer<CausalityAggregateModel>() {

				@Override
	            public Object render(final CausalityAggregateModel model,
	                    String property, ColumnData config, int rowIndex,
	                    int colIndex, ListStore<CausalityAggregateModel> store,
	                    Grid<CausalityAggregateModel> grid)
	            {
					final String aggrValue = model.getAggregateValue();
					if ( (aggrValue != null) || (model.getAggregateBy() == null) )
					{
						Anchor summaryLink = new Anchor(model.getSummary(), true);
						summaryLink.setStyleName("prl-textLink");
						summaryLink.addClickHandler(new ClickHandler(){
							
							@Override
				            public void onClick(ClickEvent event)
				            {	
								showEvidenceDialog(model);
				            }
				        });
						
						return summaryLink;
					}
					else
					{
						// Do not add a link for an 'Others' row.
						return model.getSummary();
					}
	            }
			});
		}
		
		config.add(summaryColumn);
	    
	    ColumnModel columnModel = new ColumnModel(config);
	    m_DataStore = new ListStore<CausalityAggregateModel>();  
	    m_Grid = new Grid<CausalityAggregateModel>(m_DataStore, columnModel);
	    m_Grid.setAutoExpandColumn("summary");
	    m_Grid.getView().setForceFit(true);
	    m_Grid.setHideHeaders(true);
	    
	    // Double-click on a row to open the Causality Evidence dialog.
	    m_Grid.addListener(Events.RowDoubleClick, 
	    		new Listener<GridEvent<CausalityAggregateModel>>(){

			@Override
            public void handleEvent(GridEvent<CausalityAggregateModel> ge)
            {
				showEvidenceDialog(ge.getModel());
            }
	    	
	    });
	    
	    add(m_Grid);
	}
	
	
	/**
	 * Sets the maximum number of rows for the grid. Once set, if the number 
	 * of results exceeds this limit, any remaining results are aggregated into 
	 * an 'Others' row.
	 * @param maxRows maximum number of rows to show in the grid, or <code>-1</code>
	 * 	to place no limit on the number of rows that can be added.
	 */
	public void setMaxRows(int maxRows)
	{
		m_MaxGridRows = maxRows;
	}
	
	
	/**
	 * Loads a summary of the causality data for an incident containing the specified
	 * item of evidence.
	 * @param evidenceId id of an item of evidence from the incident.
	 * @param groupingAttributes list of additional attribute names (not including
	 * 	type, source and description) used to load the 'top' item of evidence
	 * 	for each row of aggregated causality data.
	 */
	public void loadForEvidenceId(int evidenceId, List<String> groupingAttributes)
	{
		loadForEvidenceId(evidenceId, null, groupingAttributes);
	}
	
	
	/**
	 * Loads a summary of the causality data for an incident containing the specified
	 * item of evidence, grouping by the specified attribute.
	 * @param evidenceId id of an item of evidence from the incident.
	 * @param groupBy the name of the attribute by which causality data should be 
	 * 		aggregated. If <code>null</code> the current setting will be used. 
	 * @param groupingAttributes list of additional attribute names (not including
	 * 	type, source and description) used to load the 'top' item of evidence
	 * 	for each row of aggregated causality data.
	 */
	public void loadForEvidenceId(int evidenceId, String groupBy, 
			List<String> groupingAttributes)
	{
		m_EvidenceId = evidenceId;
		m_GroupingAttributes = groupingAttributes;
		
		// Group by the specified attribute, or if not supplied, use current setting.
		String viewBy;
		if (groupBy != null)
		{
			viewBy = groupBy;
		}
		else
		{
			viewBy = m_GroupByCombo.getSimpleValue();
			if (viewBy == null || viewBy.length() == 0)
			{
				viewBy = "type";
			}
		}
		
		final String useViewBy = viewBy;
		
		m_GroupByCombo.clearSelections();
		m_GroupByCombo.removeAll();
		mask(GXT.MESSAGES.loadMask_msg());
		
		// Populate the 'View by' combo with the list of aggregation attribute names.
		ApplicationResponseHandler<List<String>> callback = 
			new ApplicationResponseHandler<List<String>>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log(ClientUtil.CLIENT_CONSTANTS.errorLoadingActivityData() + ": ", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						ClientUtil.CLIENT_CONSTANTS.errorLoadingActivityData(), null);

				unmask();
			}


			@Override
            public void uponSuccess(List<String> attributeNames)
			{	
				if (m_AddShowAll == true)
				{
					m_GroupByCombo.add(ClientUtil.CLIENT_CONSTANTS.optionAll());
				}
				
				m_GroupByCombo.add(attributeNames);
				
					// If available, set the 'View by' value to the previous setting.
					// Otherwise set to first value in list.
					if (m_GroupByCombo.findModel(useViewBy) != null)
					{
						m_GroupByCombo.setSimpleValue(useViewBy);
					}
					else
					{
						if (attributeNames.size() > 0)
						{
							m_GroupByCombo.setSimpleValue(attributeNames.get(0));
						}
					}
				
					load();
				
					m_GroupByCombo.enableEvents(true);
			}
		};
		
		m_GroupByCombo.enableEvents(false);
		m_IncidentQueryService.getIncidentAttributeNames(m_EvidenceId, callback);
	}
	
	
	/**
	 * Returns the name of the attribute by which the summary data is currently 
	 * being grouped.
	 * @return the current 'Group by' aggregate name, or <code>null</code> if
	 * 	summary data is not being grouped by an attribute.
	 */
	public String getGroupBy()
	{
		String groupBy = null;
		
		String comboValue = m_GroupByCombo.getSimpleValue();
		if (comboValue.equals(ClientUtil.CLIENT_CONSTANTS.optionAll()) == false)
		{
			groupBy = comboValue;
		}
		
		return groupBy;
	}
	
	
	/**
	 * Returns a reference to the toolbar component at the top of the widget.
	 * @return
	 */
	public ToolBar getToolBar()
	{
		return m_GridToolBar;
	}
	
	
	/**
	 * Returns the top items of aggregated causality data according to the field
	 * by which the grid is currently sorted.
	 * @param limit maximum number of items to return.
	 * @param includeEvidenceId evidence ID of a 'headline' notification or time series
	 * 	feature which should be included in the returned list, if available.
	 * @return the top items of aggregated causality data from the summary grid.
	 */
	public List<CausalityAggregateModel> getTopCausalityData(int limit, int includeEvidenceId)
	{
		ArrayList<CausalityAggregateModel> topList = 
			new ArrayList<CausalityAggregateModel>();
		int storeCount = m_DataStore.getCount();
		int numItems = Math.min(storeCount, limit);
		for (int i = 0; i < numItems; i++)
		{
			topList.add(m_DataStore.getAt(i));
		}
		
		if (includeEvidenceId != 0)
		{
			boolean includedHeadline = false;
			
			for (CausalityAggregateModel aggregate : topList)
			{
				if (aggregate.getTopEvidenceId() == includeEvidenceId)
				{
					includedHeadline = true;
					break;
				}
			}
			
			if (includedHeadline == false)
			{
				CausalityAggregateModel aggregate;
				CausalityAggregateModel headlineAggregate = null;
				for (int i = numItems; i < storeCount; i++)
				{
					aggregate = m_DataStore.getAt(i);
					if (aggregate.getTopEvidenceId() == includeEvidenceId)
					{
						headlineAggregate = aggregate;
						break;
					}
				}
				
				if (headlineAggregate != null)
				{
					topList.remove(topList.size() - 1);
					topList.add(headlineAggregate);
				}
			}
		}
		
		return topList;
	}
	
	
	/**
	 * Returns the selection model for the summary grid.
	 * @return the GridSelectionModel
	 */
	public GridSelectionModel<CausalityAggregateModel> getSelectionModel()
	{
		return m_Grid.getSelectionModel();
	}
	
	
	/**
	 * Returns the summary grid's store of aggregated causality data.
	 * @return the store of CausalityAggregateModel data.
	 */
	public ListStore<CausalityAggregateModel> getStore()
	{
		return m_Grid.getStore();
	}
	
	
	/**
	 * Adds a GXT <code>StoreListener</code> to the grid to listen for StoreEvents
	 * fired by the grid's data store.
	 * @param listener the listener to add.
	 */
	public void addStoreListener(StoreListener<CausalityAggregateModel> listener)
	{
		m_DataStore.addStoreListener(listener);
	}
	
	
	/**
	 * Removes a GXT <code>StoreListener</code> from the grid.
	 * @param listener the listener to remover.
	 */
	public void removeStoreListener(StoreListener<CausalityAggregateModel> listener)
	{
		m_DataStore.removeStoreListener(listener);
	}
	
	
	/**
	 * Loads a summary of the incident into the widget. 
	 */
	protected void load()
	{
		// Load the aggregate causality data for the 
		// current incident and 'aggregate by' attribute.
		ApplicationResponseHandler<List<CausalityAggregateModel>> callback = 
			new ApplicationResponseHandler<List<CausalityAggregateModel>>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log(ClientUtil.CLIENT_CONSTANTS.errorLoadingActivityData() + ": ", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						ClientUtil.CLIENT_CONSTANTS.errorLoadingActivityData(), null);
				
				unmask();
			}


			@Override
            public void uponSuccess(List<CausalityAggregateModel> aggregateList)
			{	
				m_DataStore.add(aggregateList);
				m_GroupByCombo.enableEvents(true);
				
				unmask();
				
				m_DataStore.fireEvent(Store.DataChanged, 
						new StoreEvent<CausalityAggregateModel>(m_DataStore));
			}
		};
		
		m_GroupByCombo.enableEvents(false);
		
		m_DataStore.fireEvent(Store.BeforeDataChanged, 
				new StoreEvent<CausalityAggregateModel>(m_DataStore));
		
		m_DataStore.removeAll();
		
		mask(GXT.MESSAGES.loadMask_msg());
		
		m_IncidentQueryService.getIncidentSummary(m_EvidenceId, getGroupBy(), 
				m_GroupingAttributes, m_MaxGridRows, callback);
	}
	
	
	/**
	 * Opens the dialog which pages through the notifications and features which 
	 * have been causally related in an activity.
	 * @param evidence item of evidence to display in the dialog.
	 */
	protected void showEvidenceDialog(CausalityAggregateModel model)
	{	
		String aggrValue = model.getAggregateValue();
		
		// Do not open for 'Others' row or when the value of the aggregated attribute is null,
		// as back end procs do not support 'IN' or 'is null' type queries.
		if ( (aggrValue != null) || (model.getAggregateBy() == null) )
		{
			CausalityEvidenceDialog evidenceDialog = CausalityEvidenceDialog.getInstance();
			
			String valueDesc = (aggrValue != null ? aggrValue : model.getSummary());
			evidenceDialog.setHeading(
					ClientUtil.CLIENT_CONSTANTS.notificationDataHeading(valueDesc));
			
			evidenceDialog.setDataSourceName(model.getDataSourceName());
			evidenceDialog.setEvidenceId(model.getTopEvidenceId());
			evidenceDialog.setSingleDescription(false);
			
			// Use 'aggregate by' attribute name from model as some aggregate
			// data is flagged specially e.g. description=Time series feature.
			Attribute filterAttribute = new Attribute(model.getAggregateBy(), aggrValue);
			evidenceDialog.setFilter(Collections.singletonList(filterAttribute));
			evidenceDialog.load();
			evidenceDialog.show();
		}
	}


    @Override
    public El mask()
    {
    	m_GridToolBar.disable();
		return m_Grid.mask();
    }
    
    
    @Override
    public El mask(String message)
    {
    	m_GridToolBar.disable();
		return m_Grid.mask(message);
    }


    @Override
    public void unmask()
    {
    	m_GridToolBar.enable();
    	m_Grid.unmask();
    }

}
