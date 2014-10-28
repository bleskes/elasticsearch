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

package com.prelert.splash;

import java.util.ArrayList;
import java.util.List;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.SortDir;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.IconHelper;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.grid.AggregationRenderer;
import com.extjs.gxt.ui.client.widget.grid.AggregationRowConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.grid.SummaryType;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.data.gxt.DataSourceModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.DataSourceQueryServiceAsync;


/**
 * Ext GWT (GXT) widget for displaying a summary of the data analysed by the 
 * Prelert engine. The widget consists of two grids for listing the data types
 * and the sources for the selected data type.
 * @author Pete Harverson
 */
public class AnalysedDataWidget extends ContentPanel
{
	private DataSourceQueryServiceAsync 	m_DataSourceQueryService;
	
	private ListStore<DataSourceModel>		m_DataSourceTypesStore;
	private ListStore<DataSourceModel>		m_SourcesStore;
	
	private ToolBar							m_ToolBar;
	private Button 							m_RefreshBtn;
	private Grid<DataSourceModel> 			m_TypesGrid;
	private Grid<DataSourceModel> 			m_SourcesGrid;
	private ContentPanel 					m_SourcesPanel;
	
	private DataSourceModel					m_SelectedDataSourceType;
	
	
	/**
	 * Creates a new widget for displaying a summary of the data that has been
	 * analysed by the Prelert engine.
	 */
	public AnalysedDataWidget()
	{
		m_DataSourceQueryService = AsyncServiceLocator.getInstance().getDataSourceQueryService();
	    
	    initComponents();
	}
	
	
	/**
	 * Initialises the components in the widget.
	 */
	protected void initComponents()
	{
		BorderLayout contentLayout = new BorderLayout();   
		contentLayout.setContainerStyle("prl-border-layout-ct");
	    setLayout(contentLayout);
		setHeaderVisible(false);
	  
	    // Create a toolbar to hold a Refresh button.
	    m_ToolBar = new ToolBar();   
	    m_RefreshBtn = new Button();
	    m_RefreshBtn.setIcon(GXT.IMAGES.paging_toolbar_refresh());
	    m_RefreshBtn.setToolTip(GXT.MESSAGES.pagingToolBar_refreshText());
	    m_RefreshBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
			{
				load();
			}
		});    
	    m_ToolBar.add(m_RefreshBtn);
	    
	    setTopComponent(m_ToolBar);  
	    
	    // Create a custom renderer for the count column to render
	    // values of -1 as 'not available'.
	    GridCellRenderer<DataSourceModel> countRenderer = new GridCellRenderer<DataSourceModel>() {

	    	@Override
			public String render(DataSourceModel model, String property,
			        ColumnData config, int rowIndex, int colIndex,
			        ListStore<DataSourceModel> store, Grid<DataSourceModel> grid)
			{
	    		if (model.getCount() >= 0)
	    		{
	    			return NumberFormat.getDecimalFormat().format(model.getCount());
	    		}
	    		else
	    		{
	    			return ClientUtil.CLIENT_CONSTANTS.notAvailable();
	    		}	
			}   
	   
	    };
	    
	    // Create a custom renderer for the count aggregation so that values
	    // of -1, indicating 'count not available' do not contribute to the total.
	    AggregationRenderer<DataSourceModel> totalRenderer = 
	    	new AggregationRenderer<DataSourceModel>() {

			@Override
            public Object render(Number value, int colIndex,
                    Grid<DataSourceModel> grid, ListStore<DataSourceModel> store)
            {
				int total = 0;
				if (store != null)
				{
					List<DataSourceModel> models = store.getModels();
					int count;
					for (DataSourceModel dataSource : models)
					{
						count = dataSource.getCount();
						if (count > 0)
						{
							total += count;
						}
					}
				}
	            return NumberFormat.getDecimalFormat().format(total);
            }   
		        
	    };
	  
	    // Create the store and grid for data source types.
	    m_DataSourceTypesStore = new ListStore<DataSourceModel>(); 
	    m_DataSourceTypesStore.setSortField("dataSourceName");
	    m_DataSourceTypesStore.setSortDir(SortDir.ASC);

	    ArrayList<ColumnConfig> dataTypesConfigs = new ArrayList<ColumnConfig>();   
	    
	    dataTypesConfigs.add(new ColumnConfig("dataSourceName", 
	    		ClientUtil.CLIENT_CONSTANTS.type(), 158));
	    
	    dataTypesConfigs.add(new ColumnConfig("dataSourceCategory", 
	    		ClientUtil.CLIENT_CONSTANTS.category(), 90));
	    
	    ColumnConfig countColumn = new ColumnConfig(
	    		"count", ClientUtil.CLIENT_CONSTANTS.count(), 90);
	    countColumn.setRenderer(countRenderer);
	    dataTypesConfigs.add(countColumn);
	    
	    ColumnModel dataTypesColumnModel = new ColumnModel(dataTypesConfigs);  
	    
	    // Add a total row at the bottom.
	    AggregationRowConfig<DataSourceModel> typesTotal = new AggregationRowConfig<DataSourceModel>();   
	    typesTotal.setHtml("dataSourceName", ClientUtil.CLIENT_CONSTANTS.total()); 
	    typesTotal.setSummaryType("count", SummaryType.SUM);   
		typesTotal.setRenderer("count", totalRenderer);
	    dataTypesColumnModel.addAggregationRow(typesTotal); 
	    
	  
	    m_TypesGrid = new Grid<DataSourceModel>(m_DataSourceTypesStore, dataTypesColumnModel);     
	    m_TypesGrid.getSelectionModel().setSelectionMode(Style.SelectionMode.SINGLE); 
	    m_TypesGrid.getSelectionModel().addSelectionChangedListener(
     			new SelectionChangedListener<DataSourceModel>(){

			@Override
            public void selectionChanged(SelectionChangedEvent<DataSourceModel> se)
            {
				DataSourceModel selectedDsType = se.getSelectedItem();
				if (selectedDsType != null)
				{
					loadSources(selectedDsType);
				}
            }
     	});

	    
	    ContentPanel typesPanel = new ContentPanel();
	    typesPanel.setHeading(ClientUtil.CLIENT_CONSTANTS.dataTypes());   
	    typesPanel.setLayout(new FitLayout());   
	    typesPanel.add(m_TypesGrid); 

	    
	    // Create the store and grid for sources.    
	    m_SourcesStore = new ListStore<DataSourceModel>();   
	    m_SourcesStore.setSortField("source");
	    m_SourcesStore.setSortDir(SortDir.ASC);
	    
	    ArrayList<ColumnConfig> sourcesConfigs = new ArrayList<ColumnConfig>();   
	    sourcesConfigs.add(new ColumnConfig("source", ClientUtil.CLIENT_CONSTANTS.source(), 230));   
	    countColumn = new ColumnConfig("count", ClientUtil.CLIENT_CONSTANTS.count(), 100);
	    countColumn.setRenderer(countRenderer);
	    sourcesConfigs.add(countColumn);   
	  
	    ColumnModel sourcesColumnModel = new ColumnModel(sourcesConfigs); 
	    
	    // Add a total row at the bottom.
	    AggregationRowConfig<DataSourceModel> sourcesTotal = new AggregationRowConfig<DataSourceModel>();   
	    sourcesTotal.setHtml("source", ClientUtil.CLIENT_CONSTANTS.total()); 
	    sourcesTotal.setSummaryType("count", SummaryType.SUM);   
	    sourcesTotal.setRenderer("count", totalRenderer);
	    sourcesColumnModel.addAggregationRow(sourcesTotal); 
	  
	    m_SourcesGrid = new Grid<DataSourceModel>(m_SourcesStore, sourcesColumnModel);    
	    m_SourcesGrid.setLoadMask(true);
	    
	    m_SourcesPanel = new ContentPanel();
	    m_SourcesPanel.setHeading(ClientUtil.CLIENT_CONSTANTS.selectDataSource());   
	    m_SourcesPanel.setLayout(new FitLayout());   
	    m_SourcesPanel.add(m_SourcesGrid);   
	    
	    BorderLayoutData westGridData = new BorderLayoutData(LayoutRegion.WEST, 0.5f, 200, 800); 
	    westGridData.setSplit(true);   
	    westGridData.setFloatable(false);   
	    westGridData.setMargins(new Margins(5, 0, 5, 5)); 
	    
	    BorderLayoutData centerGridData = new BorderLayoutData(LayoutRegion.CENTER);   
	    centerGridData.setMargins(new Margins(5));
		
	    add(typesPanel, westGridData);
	    add(m_SourcesPanel, centerGridData);
	}
	
	
	/**
	 * Loads the summary of analysed data into the widget.
	 */
	public void load()
	{
		// Ensure the Data by Type grid is ready as otherwise the aggregation row
		// may not refresh properly on IE.
		if (m_TypesGrid.isViewReady() == true)
    	{
			loadSourceTypes();
    	}
    	else
    	{
    		m_TypesGrid.addListener(Events.ViewReady,new Listener<ComponentEvent>(){

				@Override
                public void handleEvent(ComponentEvent be)
                {
					removeListener(Events.ViewReady, this);
					loadSourceTypes(); 
                }
    			
    		});
    	}
	}
	
	
	/**
	 * Loads the list of data source types.
	 */
	public void loadSourceTypes()
	{
		ApplicationResponseHandler<List<DataSourceModel>> callback = 
			new ApplicationResponseHandler<List<DataSourceModel>>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log("Error loading summary of analysed data", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
						ClientUtil.CLIENT_CONSTANTS.errorLoadingAnalysedData(), null);
			}


			@Override
            public void uponSuccess(List<DataSourceModel> models)
			{
				m_DataSourceTypesStore.removeAll();	// In case two responses received since clear.
				m_DataSourceTypesStore.add(models);
				
				// See if previously selected data type is still in the store.
				if ( (m_SelectedDataSourceType != null) && 
						(m_DataSourceTypesStore.indexOf(m_SelectedDataSourceType) != -1) )
				{
					m_TypesGrid.getSelectionModel().select(false, m_SelectedDataSourceType);
				}
				else
				{
					if (models.size() > 0)
					{
						m_TypesGrid.getSelectionModel().select(false, models.get(0));
					}
				}
					
			}
		};

		m_DataSourceTypesStore.removeAll();
		
		m_DataSourceQueryService.getDataSourceTypes(callback);
	}
	
	
	/**
	 * Loads the list of sources for the specified data source type.
	 */
	public void loadSources(DataSourceModel dataSourceType)
	{
		ApplicationResponseHandler<List<DataSourceModel>> callback = 
			new ApplicationResponseHandler<List<DataSourceModel>>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				m_SourcesGrid.unmask();
				m_RefreshBtn.setIcon(IconHelper.createStyle("x-tbar-refresh"));
				m_ToolBar.setEnabled(true);
				
				GWT.log("Error loading summary of analysed data", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
						ClientUtil.CLIENT_CONSTANTS.errorLoadingAnalysedData(), null);
			}


			@Override
            public void uponSuccess(List<DataSourceModel> models)
			{
				m_SourcesStore.removeAll();	// In case two responses received since clear.
				m_SourcesStore.add(models);
				
				m_SourcesGrid.unmask();
				m_RefreshBtn.setIcon(IconHelper.createStyle("x-tbar-refresh"));
				m_ToolBar.setEnabled(true);
			}
		};
		
		m_SelectedDataSourceType = dataSourceType;
		m_ToolBar.setEnabled(false);
		m_RefreshBtn.setIcon(IconHelper.createStyle("x-tbar-loading"));
		m_SourcesGrid.mask(GXT.MESSAGES.loadMask_msg());
		
		m_SourcesStore.removeAll();
		m_SourcesPanel.setHeading(
				ClientUtil.CLIENT_CONSTANTS.dataSourcesForType(dataSourceType.getDataSourceName()));
		
		m_DataSourceQueryService.getDataSources(dataSourceType.getDataSourceType(), callback);
	}

}
