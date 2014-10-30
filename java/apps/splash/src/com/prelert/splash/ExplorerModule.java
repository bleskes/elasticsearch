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

import java.util.Date;

import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.TabPanelEvent;
import com.extjs.gxt.ui.client.store.Store;
import com.extjs.gxt.ui.client.store.StoreEvent;
import com.extjs.gxt.ui.client.store.StoreListener;
import com.extjs.gxt.ui.client.store.TreeStore;
import com.extjs.gxt.ui.client.store.TreeStoreEvent;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.core.client.GWT;

import com.prelert.client.ClientUtil;
import com.prelert.client.gxt.ModuleComponent;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.gxt.DataSourceTreeModel;
import com.prelert.data.gxt.EvidenceModel;


/**
 * The Explorer UI module. This module allows the user to explore the different
 * types of data analysed by the Prelert engine via notification views, time
 * series views and the causality view.
 * <p>
 * The container has two main sections:
 * <ul>
 * <li>A data sources tree which displays the types and sources of data analysed
 * by the Prelert engine.</li>
 * <li>A main work area tabbed panel holding the notification and time series views
 * for each data source type, plus the causality view.</li>
 * </ul>
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>ViewReady</b> : ComponentEvent(component)<br>
 * <div>Fires when module has completed its initial set-up operations and is ready
 * to use.</div>
 * <ul>
 * <li>component : this</li>
 * </ul>
 * </dd>
 * 
 * </dl>
 * 
 * @author Pete Harverson
 */
public class ExplorerModule extends LayoutContainer implements ModuleComponent
{
	
	private DataSourceTree 				m_DataSourceTree;
	private SelectionChangedListener<DataSourceTreeModel> m_TreeSelectionListener;
	private ExplorerTabPanel			m_WorkAreaTabPanel;
	
	
	private boolean		m_IsViewReady;
	
	
	/**
	 * Creates the Explorer UI module.
	 */
	public ExplorerModule()
	{		
		BorderLayout containerLayout = new BorderLayout();
		containerLayout.setContainerStyle("prl-viewport");
		
		setLayout(containerLayout);
		
		
		// Create components for the Data sources tree.
		LayoutContainer dataSourcePanel = createDataSourcePanel();
		
		// Create the main tabbed work area.
		LayoutContainer workAreaPanel = createWorkAreaPanel();
		
		// Add a listener to the data source tree to switch to the appropriate
		// tab in the work area on selection.
		m_TreeSelectionListener = new SelectionChangedListener<DataSourceTreeModel>(){

            @Override
            public void selectionChanged(
                    SelectionChangedEvent<DataSourceTreeModel> se)
            {
            	DataSourceTreeModel selectedDataSource = se.getSelectedItem();
            	
            	if (selectedDataSource != null)
            	{
            		DataSourceType dsType = selectedDataSource.getDataSourceType(); 
	            	if (dsType != null)
	            	{
	            		String source = selectedDataSource.getSource();   	
	            		m_WorkAreaTabPanel.showDataTab(dsType, source);
	            	}
	            	else
	            	{
	            		m_WorkAreaTabPanel.showAnalysedDataTab();
	            	}
            	}
            }
			
		};

		
		m_DataSourceTree.getSelectionModel().addSelectionChangedListener(m_TreeSelectionListener);
		m_DataSourceTree.setTreeWidth(248);
		
		// Add a listener to the work area tab panel to select the corresponding
		// node in the data sources tree.
		m_WorkAreaTabPanel.addListener(Events.Select, new Listener<TabPanelEvent>(){

            public void handleEvent(TabPanelEvent be)
            {
            	final DataSourceType selectedType = m_WorkAreaTabPanel.getSelectedDataSourceType();
            	GWT.log("ExplorerModule tab panel Select, selectedType: " + selectedType);
            	if (selectedType != null || m_WorkAreaTabPanel.isAnalysedDataTabSelected())
            	{
	            	String selectedSource = m_WorkAreaTabPanel.getSelectedSource();
		            GWT.log("ExplorerModule tab panel selection listener, tab selected: " + 
		            		selectedType + ", " + selectedSource);
		            
		            selectInDataSourceTree(selectedType, selectedSource);
            	}
            }
			
		});
        
        BorderLayoutData westData = new BorderLayoutData(LayoutRegion.WEST, 270);   
        westData.setCollapsible(true);   
        westData.setMargins(new Margins(0, 10, 0, 0));   
        
        BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);   
        centerData.setMargins(new Margins(0, 0, 0, 0));   
		  
        add(dataSourcePanel, westData);    
        add(workAreaPanel, centerData);
        

        // Add a listener to set the view ready flag when the tree has loaded the data source types.
        final TreeStore<DataSourceTreeModel> dataSourceTreeStore = m_DataSourceTree.getTreeStore();
        dataSourceTreeStore.addListener(Store.DataChanged, 
        		new Listener<TreeStoreEvent<DataSourceTreeModel>>(){

            public void handleEvent(TreeStoreEvent<DataSourceTreeModel> be)
            { 
            	int numTreeItems = dataSourceTreeStore.getAllItems().size();
            	
            	if (numTreeItems > 1)
            	{
            		dataSourceTreeStore.removeListener(Store.DataChanged, this);

            		m_IsViewReady = true;
            		fireEvent(Events.ViewReady);
            	}
            }
			
		});

	}

	
	/**
	 * Returns the top-level container which holds all the graphical components
	 * for the module.
	 * @return top-level container for the module which will be displayed in the UI.
	 */
	@Override
    public Component getComponent()
    {
	    return this;
    }

	
	/**
	 * Returns id for the Explorer module.
	 * @return the Explorer module ID.
	 */
	@Override
    public String getModuleId()
    {
	    return ClientUtil.CLIENT_CONSTANTS.explorer();
    }
	
	
	/**
	 * Returns <code>true</code> if the module has completed its initial set-up 
	 * operations and is ready to use.
	 * @return <code>true</code> if it is ready to use, <code>false</code> otherwise.
	 */
	public boolean isViewReady()
	{
		return m_IsViewReady;
	}
	
	
	/**
	 * Creates the panel holding the data sources tree.
	 * @return data sources panel.
	 */
	protected LayoutContainer createDataSourcePanel()
	{	
		VerticalPanel dataSourcePanel = new VerticalPanel();
		dataSourcePanel.setScrollMode(Style.Scroll.AUTO);
		
		dataSourcePanel.addStyleName("prl-dataSourcePanel");
		
		//Label titleLabel = new Label(ClientUtil.CLIENT_CONSTANTS.analysedData());		
		//titleLabel.addStyleName("prl-headerLabels");
		
		m_DataSourceTree = new DataSourceTree();
		
		//dataSourcePanel.add(titleLabel);
		dataSourcePanel.add(m_DataSourceTree);
		
		return dataSourcePanel;
	}
	
	
	/**
	 * Creates the panel holding the main tabbed work area.
	 * @return work area panel.
	 */
	protected LayoutContainer createWorkAreaPanel()
	{
		LayoutContainer workAreaPanel = new LayoutContainer();
		workAreaPanel.setLayout(new FitLayout());

		m_WorkAreaTabPanel = new ExplorerTabPanel();
		workAreaPanel.add(m_WorkAreaTabPanel);
		
		return workAreaPanel;
	}
	
	
	/**
	 * Shows a summary of data that has been analysed by the Prelert engine.
	 */
	public void showAnalysedData()
	{
		if (isViewReady() == true)
    	{
    		selectInDataSourceTree(null, null);
    		m_WorkAreaTabPanel.showAnalysedDataTab(); 
    	}
    	else
    	{
    		addListener(Events.ViewReady,new Listener<ComponentEvent>(){

				@Override
                public void handleEvent(ComponentEvent be)
                {
					removeListener(Events.ViewReady, this);
					showAnalysedData(); 
                }
    			
    		});
    	}
	}
	
	
	/**
	 * Displays the notification or time series feature corresponding to the
	 * specified item of evidence data in the module.
	 * @param data notification or time series feature to be displayed.
	 */
	public void showData(EvidenceModel data)
	{
		if (isViewReady() == true)
    	{
			String dataType = data.getDataType();
			DataSourceType dataSourceType = m_WorkAreaTabPanel.getDataSourceType(dataType);
			String source = data.getSource();
			
    		selectInDataSourceTree(dataSourceType, source);
    		m_WorkAreaTabPanel.showDataTab(dataSourceType, source, data.getId());
    	}
    	else
    	{
    		final EvidenceModel finalData = data;
    		
    		addListener(Events.ViewReady,new Listener<ComponentEvent>(){

				@Override
                public void handleEvent(ComponentEvent be)
                {
					removeListener(Events.ViewReady, this);
					showData(finalData); 
                }
    			
    		});
    	}
	}
	
	
	/**
	 * Displays the specified time series in the module, loading the data 30 minutes
	 * either side of the specified time.
	 * @param timeSeries configuration object defining the time series to display.
	 * @param time date/time of data to load.
	 */
	public void showTimeSeries(TimeSeriesConfig timeSeries, Date time)
	{
		if (isViewReady() == true)
    	{
			DataSourceType dataSourceType = new DataSourceType(timeSeries.getDataType(),
					DataSourceCategory.TIME_SERIES);
			
    		selectInDataSourceTree(dataSourceType, timeSeries.getSource());
    		m_WorkAreaTabPanel.showTimeSeriesTab(
    				dataSourceType, timeSeries.getMetric(), 
    				timeSeries.getSource(), 
    				timeSeries.getAttributes(), time);
    	}
    	else
    	{
    		final TimeSeriesConfig config = timeSeries;
    		final Date finalTime = time;
    		
    		addListener(Events.ViewReady,new Listener<ComponentEvent>(){

				@Override
                public void handleEvent(ComponentEvent be)
                {
					removeListener(Events.ViewReady, this);
					showTimeSeries(config, finalTime); 
                }
    			
    		});
    	}
	}
	
	
	/**
	 * Shows the causality data for the specified item of evidence or 
	 * time series discord.
	 * @param evidence the notification or time series features whose probable 
	 *  	causes are to be displayed.
	 */
	public void showCausalityData(EvidenceModel evidence)
	{
		if (isViewReady() == true)
    	{
    		selectInDataSourceTree(null, null);
    		m_WorkAreaTabPanel.showCausalityTab(evidence);
    	}
    	else
    	{
    		final EvidenceModel evidenceModel = evidence;
    		addListener(Events.ViewReady,new Listener<ComponentEvent>(){

				@Override
                public void handleEvent(ComponentEvent be)
                {
					removeListener(Events.ViewReady, this);
					showCausalityData(evidenceModel); 
                }
    			
    		});
    	}
	}


	/**
	 * Selects the given data type and source in the tree.
	 * @param dataSourceType the DataSourceType for which to show the tab, or
	 * 	<code>null</code> to select the Analysed Data node.
	 * @param source the name of the source (server) to select, or <code>null</code>
	 * 	to select the 'All sources' node.
	 */
	protected void selectInDataSourceTree(DataSourceType dataSourceType, String source)
	{
		GWT.log("ExplorerModule.selectInDataSourceTree(" + dataSourceType + ", " + source + ")");
		
        boolean loadedInTree = (m_DataSourceTree.findModel(dataSourceType, source) != null);
        if (loadedInTree == true)
        {
        	// Data for this source type already loaded in the tree - just select source.
        	m_DataSourceTree.getSelectionModel().removeSelectionListener(m_TreeSelectionListener);
            m_DataSourceTree.selectModel(dataSourceType, source);
            m_DataSourceTree.getSelectionModel().addSelectionChangedListener(m_TreeSelectionListener);
        }
        else
        {
        	// Data for this source type NOT yet loaded in the tree - load data, then select source.
        	final DataSourceType finalDsType = dataSourceType;
        	final String finalSource = source;
        	m_DataSourceTree.getTreeStore().addStoreListener(new StoreListener<DataSourceTreeModel>(){

                @Override
                public void storeDataChanged(
                		StoreEvent<DataSourceTreeModel> se)
                {
                	m_DataSourceTree.getSelectionModel().removeSelectionListener(m_TreeSelectionListener);
		            m_DataSourceTree.selectModel(finalDsType, finalSource);
		            m_DataSourceTree.getSelectionModel().addSelectionChangedListener(m_TreeSelectionListener);
                	m_DataSourceTree.removeListener(Store.DataChanged, this);
                }	
			});          
        	
        	if (source != null)
        	{
        		m_DataSourceTree.expandDataSource(dataSourceType);
        	}
        }
	}

}
