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

package com.prelert.splash;

import java.util.ArrayList;
import java.util.List;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.Style.Orientation;
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
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.grid.AggregationRowConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.SummaryType;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FlowData;
import com.extjs.gxt.ui.client.widget.layout.RowData;
import com.extjs.gxt.ui.client.widget.layout.RowLayout;
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
public class AnalysedDataWidget extends LayoutContainer
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
	    ContentPanel cp = new ContentPanel();   
	    cp.setSize(700, 400);   
	    cp.setHeading(ClientUtil.CLIENT_CONSTANTS.summary());
	    cp.setLayout(new RowLayout(Orientation.HORIZONTAL));   
	  
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
	    
	    cp.setTopComponent(m_ToolBar);   
	  
	    // Create the store and grid for data source types.
	    m_DataSourceTypesStore = new ListStore<DataSourceModel>(); 
	    m_DataSourceTypesStore.setSortField("dataSourceName");
	    m_DataSourceTypesStore.setSortDir(SortDir.ASC);
	    
	    
	    
	    ArrayList<ColumnConfig> dataTypesConfigs = new ArrayList<ColumnConfig>();   
	    
	    dataTypesConfigs.add(new ColumnConfig("dataSourceName", 
	    		ClientUtil.CLIENT_CONSTANTS.type(), 158));
	    
	    dataTypesConfigs.add(new ColumnConfig("dataSourceCategory", 
	    		ClientUtil.CLIENT_CONSTANTS.category(), 100));
	    
	    ColumnConfig countColumn = new ColumnConfig(
	    		"count", ClientUtil.CLIENT_CONSTANTS.count(), 75);
	    countColumn.setNumberFormat(NumberFormat.getDecimalFormat());
	    dataTypesConfigs.add(countColumn);
	    
	    ColumnModel dataTypesColumnModel = new ColumnModel(dataTypesConfigs);  
	    
	    // Add a total row at the bottom.
	    AggregationRowConfig<DataSourceModel> typesTotal = new AggregationRowConfig<DataSourceModel>();   
	    typesTotal.setHtml("dataSourceName", ClientUtil.CLIENT_CONSTANTS.total()); 
	    typesTotal.setSummaryType("count", SummaryType.SUM);   
		typesTotal.setSummaryFormat("count", NumberFormat.getDecimalFormat());
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
	    
	    RowData rowData = new RowData(.5, 1);   
	    rowData.setMargins(new Margins(6));   
	    cp.add(typesPanel, rowData);   
	    
	  
	    // Create the store and grid for sources.    
	    m_SourcesStore = new ListStore<DataSourceModel>();   
	    m_SourcesStore.setSortField("source");
	    m_SourcesStore.setSortDir(SortDir.ASC);
	    
	    ArrayList<ColumnConfig> sourcesConfigs = new ArrayList<ColumnConfig>();   
	    sourcesConfigs.add(new ColumnConfig("source", ClientUtil.CLIENT_CONSTANTS.source(), 239));   
	    countColumn = new ColumnConfig("count", ClientUtil.CLIENT_CONSTANTS.count(), 100);
	    countColumn.setNumberFormat(NumberFormat.getDecimalFormat());
	    sourcesConfigs.add(countColumn);   
	  
	    ColumnModel sourcesColumnModel = new ColumnModel(sourcesConfigs); 
	    
	    // Add a total row at the bottom.
	    AggregationRowConfig<DataSourceModel> sourcesTotal = new AggregationRowConfig<DataSourceModel>();   
	    sourcesTotal.setHtml("source", ClientUtil.CLIENT_CONSTANTS.total()); 
	    sourcesTotal.setSummaryType("count", SummaryType.SUM);   
	    sourcesTotal.setSummaryFormat("count", NumberFormat.getDecimalFormat());
	    sourcesColumnModel.addAggregationRow(sourcesTotal); 
	  
	    m_SourcesGrid = new Grid<DataSourceModel>(m_SourcesStore, sourcesColumnModel);    
	    m_SourcesGrid.setLoadMask(true);
	    
	    m_SourcesPanel = new ContentPanel();
	    m_SourcesPanel.setHeading(ClientUtil.CLIENT_CONSTANTS.selectDataSource());   
	    m_SourcesPanel.setLayout(new FitLayout());   
	    m_SourcesPanel.add(m_SourcesGrid); 
	       
	    rowData = new RowData(.5, 1);   
	    rowData.setMargins(new Margins(6, 6, 6, 0));   
	    cp.add(m_SourcesPanel, rowData);
	  
	    add(cp, new FlowData(0, 0, 10, 0));      
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
				m_DataSourceTypesStore.add(models);
				
				if (m_SelectedDataSourceType != null)
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
