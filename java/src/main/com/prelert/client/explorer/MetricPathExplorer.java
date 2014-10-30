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

package com.prelert.client.explorer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.data.BaseListLoadResult;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.store.StoreEvent;
import com.extjs.gxt.ui.client.store.StoreListener;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.CardLayout;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.tips.QuickTip;
import com.extjs.gxt.ui.client.widget.toolbar.LabelToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.RequestViewEvent;
import com.prelert.client.list.EvidenceViewPanel;
import com.prelert.data.Attribute;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.MetricPath;
import static com.prelert.data.PropertyNames.*;

import com.prelert.data.TimeFrame;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.data.gxt.MetricTreeNodeModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.TimeSeriesGXTPagingServiceAsync;
import com.prelert.splash.AnalysedDataWidget;


/**
 * Ext GWT (GXT) widget for exploring notification and time series data in the metric
 * path tree. The widget contains three main components:
 * <ul>
 * <li>a grid on the left listing the nodes in the current level of the metric path 
 * tree in 'folders'.</li>
 * <li>a work area on the right whose contents updates according to the category
 * of node selected in the 'folder' grid.</li>
 * <li>an 'address bar' across the top displaying the current full metric path.</li>
 * </ul> 
 * 
 * @author Pete Harverson
 */
public class MetricPathExplorer extends ContentPanel
{
	private TimeSeriesGXTPagingServiceAsync	m_TimeSeriesQueryService;
	
	private Grid<MetricTreeNodeModel> 		m_MetricPathGrid;
	private MetricPathListLoader			m_MetricPathLoader;
	private ListStore<MetricTreeNodeModel>	m_MetricPathStore;
	private String							m_MetricValueToSelect;
	private Date							m_TimeToShow;
	private EvidenceModel					m_EvidenceToShow;
	
	private LayoutContainer					m_WorkAreaPanel;
	private CardLayout 						m_CardLayout;
	
	private EvidenceViewPanel				m_EvidencePanel; 
	private TimeSeriesViewPanel				m_TimeSeriesPanel;
	private AnalysedDataWidget 				m_AnalysedDataWidget;
	
	private MetricPathExplorerIcons	m_TreeIcons;
	private Button				m_UpButton;
	private Label				m_PathField;
	
	
	/**
	 * Class providing icons for use in the metric path tree.
	 * @author Pete Harverson
	 */
	public static class MetricPathExplorerIcons
	{
		private AbstractImagePrototype m_Up = AbstractImagePrototype.create(
				ClientUtil.CLIENT_IMAGES.icon_folder_up());
		private AbstractImagePrototype m_UpDisabled = AbstractImagePrototype.create(
				ClientUtil.CLIENT_IMAGES.icon_folder_up_disabled());
    
		private String m_FolderClosedIconHtml = AbstractImagePrototype.create(
				ClientUtil.CLIENT_IMAGES.icon_folder_closed()).getHTML();
		private String m_NotificationIconHtml = AbstractImagePrototype.create(
				ClientUtil.CLIENT_IMAGES.icon_notification()).getHTML();
		private String m_TimeSeriesIconHtml = AbstractImagePrototype.create(
				ClientUtil.CLIENT_IMAGES.icon_time_series()).getHTML();
		private String m_SummaryIconHtml = AbstractImagePrototype.create(
				ClientUtil.CLIENT_IMAGES.icon_table()).getHTML();
	
		

		public AbstractImagePrototype getFolderUp()
		{
			return m_Up;
		}
		
		
		public AbstractImagePrototype getFolderUpDisabled()
		{
			return m_UpDisabled;
		}
		
		
		/**
		 * Returns the html used to render the closed folder icon.
		 * @return the html fragment to render the icon.
		 */
		public String getFolderClosedIconHtml()
		{
			return m_FolderClosedIconHtml;
		}
		
		
		/**
		 * Returns the html used to render a notification-type tree node.
		 * @return the html fragment to render the icon.
		 */
		public String getNotificationIconHtml()
		{
			return m_NotificationIconHtml;
		}
		
		
		/**
		 * Returns the html used to render a time series-type tree node.
		 * @return the html fragment to render the icon.
		 */
		public String getTimeSeriesIconHtml()
		{
			return m_TimeSeriesIconHtml;
		}
		
		
		/**
		 * Returns the html to render an icon for the specified metric tree node.
		 * @param treeNode <code>MetricTreeNodeModel</code> to return an icon.
		 * @return the html fragment to render the icon.
		 */
		public String getNodeIconHtml(MetricTreeNodeModel treeNode)
		{
			if (treeNode.isLeaf() == false)
            {
            	return m_FolderClosedIconHtml;	
            }
            else
            {
            	if (treeNode.getCategory() != null)
            	{
	            	if (treeNode.getCategory() == DataSourceCategory.NOTIFICATION)
	            	{
	            		return m_NotificationIconHtml;
	            	}
	            	else
	            	{
	            		return m_TimeSeriesIconHtml;
	            	}
            	}
            	else
            	{
            		return m_SummaryIconHtml;
            	}
            		
            }
		}
	}
	
	
	/**
	 * Creates a new widget for exploring notification and time series data via
	 * the metric path.
	 */
	public MetricPathExplorer()
	{
		m_TimeSeriesQueryService = AsyncServiceLocator.getInstance().getTimeSeriesGXTQueryService();
		
		m_TreeIcons = new MetricPathExplorerIcons();
		
		setHeaderVisible(false);
		
		BorderLayout contentLayout = new BorderLayout();   
		contentLayout.setContainerStyle("prl-border-layout-ct");
	    setLayout(contentLayout);
	    
		
		// Create the toolbar for navigating the metric path.
		final ToolBar pathToolBar = new ToolBar();
		pathToolBar.setEnableOverflow(false);
		pathToolBar.setSpacing(3);
		
		// Create the 'Up' button.
		m_UpButton = new Button();
		m_UpButton.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.icon_folder_up()));
		m_UpButton.setToolTip(ClientUtil.CLIENT_CONSTANTS.upOneLevel());
		m_UpButton.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
			{
				loadPreviousLevel();
			}
		});
		
		// Create the field for displaying the metric path.
		final LabelToolItem pathLabel = new LabelToolItem(ClientUtil.CLIENT_CONSTANTS.path());
		
		m_PathField = new Label();
		final LayoutContainer pathFieldContainer = new LayoutContainer();
		pathFieldContainer.add(m_PathField);
		pathFieldContainer.setBorders(true);
		pathFieldContainer.addStyleName("prl-title-label");	
	
		
		pathToolBar.add(m_UpButton);
		pathToolBar.add(pathLabel);
		pathToolBar.add(pathFieldContainer);
		
		// Auto resize metric path field to fill available width.
        Listener<ComponentEvent> resizeListener = new Listener<ComponentEvent>(){
			
			@Override
            public void handleEvent(ComponentEvent be)
            {
				pathFieldContainer.setWidth(pathToolBar.getWidth() - m_UpButton.getWidth() -
						pathLabel.getWidth() - 24);
            }
		};
		addListener(Events.Resize, resizeListener);
		
		setTopComponent(pathToolBar);
		
		
		// Create the folder grid listing the nodes at each level of the metric path.
		m_MetricPathGrid = createMetricPathGrid();
	    ContentPanel folderPanel = new ContentPanel(new FitLayout());
	    folderPanel.setHeaderVisible(false);
		folderPanel.add(m_MetricPathGrid);
		
		// Add a listener to the metric path store to select a node when the data changes.
		m_MetricPathStore.addStoreListener(new StoreListener<MetricTreeNodeModel>(){

            @Override
            public void storeDataChanged(StoreEvent<MetricTreeNodeModel> se)
            {
            	if (m_MetricPathStore.getCount() > 0 && m_MetricValueToSelect != null)
            	{
            		for (MetricTreeNodeModel treeNode : m_MetricPathStore.getModels())
            		{
            			if (treeNode.getValue().equals(m_MetricValueToSelect))
            			{
            				m_MetricPathGrid.getSelectionModel().select(false, treeNode);
            				break;
            			}
            		}
            		
            		m_MetricValueToSelect = null;
            	}
            }
	    	
	    });
		
		// Add a selection listener to the metric path grid to change the
		// content in the evidence grid.
		m_MetricPathGrid.getSelectionModel().addSelectionChangedListener(
	    		new SelectionChangedListener<MetricTreeNodeModel>(){

			@Override
            public void selectionChanged(SelectionChangedEvent<MetricTreeNodeModel> se)
            {
				// Only update the work area content for leaf nodes.
				MetricTreeNodeModel selectedNode = se.getSelectedItem();
				if ( (selectedNode != null) && (selectedNode.isLeaf() == true) )
				{
					showDataForMetricTreeNode(selectedNode);
				}
            }
	    });
	    		
		// Create the container to hold the main work component
		// - notification list, time series chart, or analysed data table.
		m_CardLayout = new CardLayout();
		m_WorkAreaPanel = new LayoutContainer();
		m_WorkAreaPanel.setLayout(m_CardLayout);  
		
		BorderLayoutData westGridData = new BorderLayoutData(LayoutRegion.WEST, 0.25f, 200, 800); 
	    westGridData.setSplit(true);   
	    westGridData.setFloatable(false);   
	    westGridData.setMargins(new Margins(5, 0, 5, 5)); 
	    
	    BorderLayoutData centerGridData = new BorderLayoutData(LayoutRegion.CENTER);   
	    centerGridData.setMargins(new Margins(5));
		
	    add(folderPanel, westGridData);
	    add(m_WorkAreaPanel, centerGridData);
	}
	
	
	/**
	 * Creates the GXT Grid component for listing the nodes at the current level 
	 * of the metric path.
	 * @return Grid widget displaying levels in the metric path.
	 */
	protected Grid<MetricTreeNodeModel> createMetricPathGrid()
	{
		m_MetricPathLoader = new MetricPathListLoader();
		m_MetricPathStore = new ListStore<MetricTreeNodeModel>(m_MetricPathLoader);
		
		// Use a custom cell renderer to show icons and tooltips.
		GridCellRenderer<MetricTreeNodeModel> cellRenderer = 
			new GridCellRenderer<MetricTreeNodeModel>()
		{
            @Override
            public Object render(MetricTreeNodeModel model, String property,
                    ColumnData config, int rowIndex, int colIndex,
                    ListStore<MetricTreeNodeModel> store, Grid<MetricTreeNodeModel> grid)
            {
            	StringBuilder sb = new StringBuilder();
            	sb.append("<span class=\"prl-metricPathTree-grid-cell\">");
            	sb.append(m_TreeIcons.getNodeIconHtml(model));
	            sb.append("</span>");  
	            
	            sb.append("<span qtip=\"");
	            sb.append(model.getValue());
	            if (model.getName() != null)
	            {
	            	sb.append("\" qtitle=\"");
	            	sb.append(model.getName());
	            }
	            sb.append("\" >");
	            sb.append(model.getValue());
				sb.append("</span>");
	            
	            return sb.toString();
            }
			
		};
		
		List<ColumnConfig> config = new ArrayList<ColumnConfig>();
		ColumnConfig valueColumn = new ColumnConfig(VALUE, 200);
		valueColumn.setRenderer(cellRenderer);
		config.add(valueColumn);
		ColumnModel columnModel = new ColumnModel(config);
		
		Grid<MetricTreeNodeModel> folderGrid = 
			new Grid<MetricTreeNodeModel>(m_MetricPathStore, columnModel);
		folderGrid.setHideHeaders(true);
		folderGrid.setAutoExpandColumn(VALUE); 
		folderGrid.setLoadMask(true);
		folderGrid.setSelectionModel(new MetricPathGridSelectionModel());
		
		// This reference to QuickTip is needed to enable tooltips.
	    @SuppressWarnings("unused")
	    QuickTip qtip = new QuickTip(folderGrid);
		
		
		// Double-click on a row to display the next level.
		folderGrid.addListener(Events.RowDoubleClick, 
	    		new Listener<GridEvent<MetricTreeNodeModel>>(){

			@Override
            public void handleEvent(GridEvent<MetricTreeNodeModel> ge)
            {
				MetricTreeNodeModel treeNode = ge.getModel();
				if (treeNode.isLeaf() == false)
				{
					m_MetricPathLoader.setType(treeNode.getType());
					m_MetricPathLoader.setPreviousPath(treeNode.getPartialPath());
					m_MetricPathLoader.setCurrentValue(treeNode.getValue());
					m_MetricPathLoader.setOpaqueNum(treeNode.getOpaqueNum());
					m_MetricPathLoader.setOpaqueString(treeNode.getOpaqueStr());
					m_MetricPathLoader.loadNextLevel();
				}
            }
	    	
	    });
		
		// Update the path field on each load event.
		m_MetricPathLoader.addLoadListener(new LoadListener(){

            @Override
            public void loaderBeforeLoad(LoadEvent le)
			{
            	m_UpButton.setEnabled(false);
            	m_UpButton.setIcon(m_TreeIcons.getFolderUpDisabled());
			}


            @Override
            public void loaderLoad(LoadEvent le)
            {
            	BaseListLoadResult<MetricTreeNodeModel> data = le.getData();
            	if (data != null)
            	{
            		List<MetricTreeNodeModel> treeNodes = data.getData();
            		if (treeNodes.size() > 0)
            		{
            			String partialPath = treeNodes.get(0).getPartialPath();
            			m_PathField.setText(partialPath);
            			
            			if (partialPath.isEmpty())
            			{
            				// Add a summary node.
            				MetricTreeNodeModel summaryNode = new MetricTreeNodeModel();
            				summaryNode.setIsLeaf(true);
            				summaryNode.setValue(ClientUtil.CLIENT_CONSTANTS.summary());
            				m_MetricPathStore.insert(summaryNode, 0);	
            			}
            			else
            			{
            				m_UpButton.setEnabled(true);
                        	m_UpButton.setIcon(m_TreeIcons.getFolderUp());
            			}
            		}
            	}
            }
            
            
            @Override
            public void loaderLoadException(LoadEvent le)
            {
            	GWT.log("MetricPathExplorer - error loading metric path data: " + le.exception);		
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						ClientUtil.CLIENT_CONSTANTS.errorLoadingDataSources(), null);
				
				if (m_PathField.getText().isEmpty() == false)
				{
					m_UpButton.setEnabled(true);
					m_UpButton.setIcon(m_TreeIcons.getFolderUp());
				}
            }
			
		});

		return folderGrid;
	}
	
	
	/**
	 * Refreshes the contents in the main work area to display data for the 
	 * supplied metric tree node.
	 * @param treeNode <code>MetricTreeNodeModel</code> whose data to display.
	 */
	protected void showDataForMetricTreeNode(MetricTreeNodeModel treeNode)
	{
		if (treeNode.getCategory() != null)
		{
			switch (treeNode.getCategory())
			{
				case NOTIFICATION:
					showNotificationData(treeNode);
					break;
					
				case TIME_SERIES_FEATURE:
					showTimeSeriesData(treeNode);
					break;
			}
		}
		else
		{
			showAnalysedData();
		}
	}
	
	
	/**
	 * Refreshes the contents in the main work area to shows a summary of data 
	 * that has been analysed by the Prelert engine.
	 */
	protected void showAnalysedData()
	{
		if (m_AnalysedDataWidget == null)
		{
			m_AnalysedDataWidget = new AnalysedDataWidget();
			m_WorkAreaPanel.add(m_AnalysedDataWidget);
		}
		
		m_CardLayout.setActiveItem(m_AnalysedDataWidget);
		m_AnalysedDataWidget.load();
	}
	
	
	/**
	 * Refreshes the contents in the main work area to display notification 
	 * data for the supplied metric tree node in an evidence grid.
	 * @param treeNode <code>MetricTreeNodeModel</code> representing notification
	 * 		data.
	 */
	protected void showNotificationData(MetricTreeNodeModel treeNode)
	{
		if (m_EvidencePanel == null)
		{
			m_EvidencePanel = new EvidenceViewPanel();
			m_EvidencePanel.getLoader().setPageSize(25);
			
			// Listen for events to open causality views.
			m_EvidencePanel.addListener(GXTEvents.OpenCausalityViewClick, 
					new Listener<RequestViewEvent<EvidenceModel>>(){

	            @Override
                public void handleEvent(RequestViewEvent<EvidenceModel> rve)
	            {
	            	// Propagate the event.
	        		fireEvent(rve.getType(), rve);
	            }
				
			});
			
			m_WorkAreaPanel.add(m_EvidencePanel);
		}
		
		m_CardLayout.setActiveItem(m_EvidencePanel);
		loadEvidenceGrid(treeNode);
	}
	
	
	/**
	 * Refreshes the contents in the main work area to display time series 
	 * data for the supplied metric tree node in a time series chart.
	 * @param treeNode <code>MetricTreeNodeModel</code> representing time series
	 * 		data.
	 */
	protected void showTimeSeriesData(MetricTreeNodeModel treeNode)
	{	
		if (m_TimeSeriesPanel == null)
		{
			m_TimeSeriesPanel = new TimeSeriesViewPanel();
			
			// Listen for events to open causality views.
			m_TimeSeriesPanel.addListener(GXTEvents.OpenCausalityViewClick, 
					new Listener<RequestViewEvent<EvidenceModel>>(){

	            @Override
                public void handleEvent(RequestViewEvent<EvidenceModel> rve)
	            {
	            	// Propagate the event.
	        		fireEvent(rve.getType(), rve);
	            }
				
			});
			
			m_WorkAreaPanel.add(m_TimeSeriesPanel);
		}
		
		m_CardLayout.setActiveItem(m_TimeSeriesPanel);
		
		// Hide the features list for series with wildcards e.g. all sources.
		boolean hasAnyWildcard = treeNode.hasAnyWildcard();
		m_TimeSeriesPanel.setShowFeaturesList(!hasAnyWildcard);
		
		TimeSeriesConfig config = new TimeSeriesConfig();
		config.setTimeSeriesId(treeNode.getTimeSeriesId());
		config.setDataType(treeNode.getType());
		config.setSource(treeNode.getSource());
		config.setMetric(treeNode.getMetric());
		config.setAttributes(treeNode.getAttributes());	
		m_TimeSeriesPanel.setTimeSeriesConfig(config);
		
		m_TimeSeriesPanel.setChartTitleText(treeNode.getPartialPath());
		m_TimeSeriesPanel.setChartSubtitleText(config.getMetric());
		
		if (m_TimeToShow != null)
		{
			m_TimeSeriesPanel.setLoadTime(m_TimeToShow);
			m_TimeToShow = null;
		}
		m_TimeSeriesPanel.load();
	}
	
	
	/**
	 * Loads the metric path explorer to show a summary of the data in the 
	 * <code>AnalysedDataWidget</code>. 
	 */
	public void loadAnalysedData()
	{
		// Load the top level, and select the 'Summary' node when it is added.
		m_MetricPathStore.addStoreListener(new StoreListener<MetricTreeNodeModel>(){

            @Override
            public void storeAdd(StoreEvent<MetricTreeNodeModel> se)
            {
        		for (MetricTreeNodeModel treeNode : m_MetricPathStore.getModels())
        		{
        			if (treeNode.getCategory() == null)
        			{
        				m_MetricPathGrid.getSelectionModel().select(false, treeNode);
        				m_MetricPathGrid.getView().setAutoFill(true);	// Needed to ensure value column stays full width.
        				m_MetricPathStore.removeStoreListener(this);
        				break;
        			}
        		}
            }
	    	
	    });
		
		loadTopLevel();
	}
	
	
	/**
	 * Loads the metric path explorer to show data for the specified notification
	 * or time series feature. The path and folder grid will be loaded to display
	 * the position of the evidence in the metric path, and the work area will
	 * refresh to display the appropriate notification list or time series chart.
	 * @param evidence notification or time series feature to be loaded. 
	 */
	public void loadForEvidence(EvidenceModel evidence)
	{
		final EvidenceModel loadEvidence = evidence;
		
    	// Need to obtain the metric path for this evidence. 
		ApplicationResponseHandler<MetricPath> callback = 
			new ApplicationResponseHandler<MetricPath>(){

				@Override
                public void uponSuccess(MetricPath metricPath)
                {		
					if (metricPath != null)
					{
						m_MetricPathLoader.setType(metricPath.getDatatype());
						m_MetricPathLoader.setPreviousPath(metricPath.getPartialPath());
						m_MetricPathLoader.setCurrentValue(metricPath.getLastLevelValue());
						m_MetricPathLoader.setOpaqueNum(metricPath.getOpaqueNum());
						m_MetricPathLoader.setOpaqueString(metricPath.getOpaqueStr());
						
						m_MetricValueToSelect = metricPath.getLastLevelValue();
						
						m_EvidenceToShow = loadEvidence;
						m_TimeToShow = loadEvidence.getTime(TimeFrame.SECOND);
						
						m_MetricPathLoader.loadCurrentLevel();
					}
					else
					{
						// Should always be non-null, but load at top level in case.
						m_MetricValueToSelect = loadEvidence.getDataType();
						loadTopLevel();
					}
                }

				@Override
                public void uponFailure(Throwable caught)
                {
					GWT.log("Error loading metric path for evidence with id: " + 
							m_EvidenceToShow.getId(), caught);
					MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
							ClientUtil.CLIENT_CONSTANTS.errorLoadingEvidenceData(), null);
                }
			
		};
		
		m_TimeSeriesQueryService.getMetricPathFromFeatureId(evidence.getId(), callback);
	}
	
	
	/**
	 * Loads the metric path explorer to show data for the specified time series.
	 * The path and folder grid will be loaded to display the position of the series
	 * in the metric path, and the work area will refresh to load the time series chart
	 * at the specified time.
	 * @param timeSeries time series to load.
	 * @param time date/time to load.
	 */
	public void loadForTimeSeries(TimeSeriesConfig timeSeries, Date time)
	{
		final TimeSeriesConfig config = timeSeries;
		final Date loadTime = time;
		
    	// Need to obtain the metric path for this time series. 
		ApplicationResponseHandler<MetricPath> callback = 
			new ApplicationResponseHandler<MetricPath>(){

				@Override
                public void uponSuccess(MetricPath metricPath)
                {
					if (metricPath != null)
					{
						m_MetricPathLoader.setType(metricPath.getDatatype());
						m_MetricPathLoader.setPreviousPath(metricPath.getPartialPath());
						m_MetricPathLoader.setCurrentValue(metricPath.getLastLevelValue());
						m_MetricPathLoader.setOpaqueNum(metricPath.getOpaqueNum());
						m_MetricPathLoader.setOpaqueString(metricPath.getOpaqueStr());
						
						m_MetricValueToSelect = metricPath.getLastLevelValue();
						m_TimeToShow = loadTime;
						
						m_MetricPathLoader.loadCurrentLevel();
					}
					else
					{
						MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
								ClientUtil.CLIENT_CONSTANTS.errorNoMetricPathForId(
										config.getTimeSeriesId()), null);
					}
                }

				@Override
                public void uponFailure(Throwable caught)
                {
					GWT.log("Error loading metric path for time series with id: " + 
							config.getTimeSeriesId(), caught);
					MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
							ClientUtil.CLIENT_CONSTANTS.errorLoadingTimeSeriesData(), null);
                }
			
		};
		
		m_TimeSeriesQueryService.getMetricPathFromTimeSeriesId(
				timeSeries.getTimeSeriesId(), callback);
	}
	
	
	/**
	 * Clears the current load time on the notification and time series widgets
	 * in the main work area. Unless explicitly set, the next time these components
	 * load will be to show the latest (most recent) page of data.
	 */
	public void clearLoadTime()
	{
		if (m_EvidencePanel != null)
		{
			m_EvidencePanel.getLoader().setTime(null);
		}
		
		if (m_TimeSeriesPanel != null)
		{
			m_TimeSeriesPanel.setLoadTime(null);
		}
	}
	
	
	/**
	 * Loads the top level of metric path data.
	 */
	protected void loadTopLevel()
	{
		m_MetricPathLoader.setType(null);
		m_MetricPathLoader.setPreviousPath(null);
		m_MetricPathLoader.setCurrentValue(null);
		m_MetricPathLoader.setOpaqueNum(0);
		m_MetricPathLoader.setOpaqueString(null);
		m_MetricPathLoader.loadNextLevel();
	}
	
	
	/**
	 * Loads the previous level in the metric path to that which is currently displayed.
	 */
	protected void loadPreviousLevel()
	{
		MetricTreeNodeModel firstNode = m_MetricPathStore.getAt(0);
		if (firstNode != null)
		{
			// Add a StoreListener to select the parent folder when load completes.
			final String partialPath = firstNode.getPartialPath();
			m_MetricPathStore.addStoreListener(new StoreListener<MetricTreeNodeModel>(){

	            @Override
	            public void storeDataChanged(StoreEvent<MetricTreeNodeModel> se)
	            {
            		for (MetricTreeNodeModel treeNode : m_MetricPathStore.getModels())
            		{
            			if (treeNode.getFullPath().equals(partialPath))
            			{
            				m_MetricPathGrid.getSelectionModel().select(false, treeNode);
            				break;
            			}
            		}
            		
            		m_MetricPathStore.removeStoreListener(this);
	            }
		    	
		    });
			
			m_MetricPathLoader.setType(firstNode.getType());
			m_MetricPathLoader.setPreviousPath(partialPath);
			m_MetricPathLoader.setCurrentValue(firstNode.getValue());
			m_MetricPathLoader.setOpaqueNum(firstNode.getOpaqueNum());
			m_MetricPathLoader.setOpaqueString(firstNode.getOpaqueStr());
			m_MetricPathLoader.loadPreviousLevel();
		}
	}
	
	
	/**
	 * Configures the evidence grid and then loads data for the specified metric
	 * tree node.
	 * @param metricPathNode <code>MetricTreeNodeModel</code> defining the evidence
	 * data to load.
	 */
	protected void loadEvidenceGrid(final MetricTreeNodeModel metricPathNode)
	{
		// Reconfigure the evidence grid.
		ArrayList<Attribute> filter = new ArrayList<Attribute>();
		DataSourceCategory category = metricPathNode.getCategory();
		if (category.equals(DataSourceCategory.TIME_SERIES_FEATURE))
		{
			filter.add(new Attribute(METRIC, metricPathNode.getMetric()));
		}
		
		List<Attribute> nodeAttributes = metricPathNode.getAttributes();
		if (nodeAttributes != null)
		{
			filter.addAll(nodeAttributes);
		}
		m_EvidencePanel.reconfigure(
				new DataSourceType(metricPathNode.getType(), category), 
						metricPathNode.getSource(), filter);
		
		if (m_EvidenceToShow != null)
		{
			// Clear any filter as it may not match specified item of evidence.
			m_EvidencePanel.clearToolBarFilter();
			m_EvidencePanel.loadAtId(m_EvidenceToShow.getId());
			m_EvidenceToShow = null;
		}
		else
		{
			m_EvidencePanel.load();
		}
	}
	
	
	/**
	 * Extension of <code>GridSelectionModel</code> which only allows for single
	 * selection of leaf nodes.
	 */
	class MetricPathGridSelectionModel extends GridSelectionModel<MetricTreeNodeModel>
	{
		public MetricPathGridSelectionModel()
		{
			setSelectionMode(SelectionMode.SINGLE);
		}
		
		@Override
        protected void handleMouseDown(GridEvent<MetricTreeNodeModel> e)
        {
        	if (e.getModel().isLeaf())
            {
        		super.handleMouseDown(e);
            }
        }


        @Override
        protected void onKeyDown(GridEvent<MetricTreeNodeModel> e)
        {
        	int rowIdx = listStore.indexOf(getLastFocused());
        	if (rowIdx + 1 < listStore.getCount() &&
        			listStore.getAt(rowIdx + 1).isLeaf()) 
            {
        		super.onKeyDown(e);
            }
        }
        
        
        @Override
        protected void onKeyUp(GridEvent<MetricTreeNodeModel> e)
        {
        	int rowIdx = listStore.indexOf(getLastFocused());
        	if (rowIdx > 0 && listStore.getAt(rowIdx - 1).isLeaf()) 
            {
        		super.onKeyUp(e);
            }
        }
	}

}
