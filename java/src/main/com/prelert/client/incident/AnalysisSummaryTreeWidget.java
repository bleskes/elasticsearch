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

import static com.prelert.data.PropertyNames.DESCRIPTION;
import static com.prelert.data.PropertyNames.END_TIME;
import static com.prelert.data.PropertyNames.MAGNITUDE;
import static com.prelert.data.PropertyNames.SIGNIFICANCE;
import static com.prelert.data.PropertyNames.START_TIME;

import java.util.ArrayList;
import java.util.List;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.Style.SortDir;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.data.ModelComparer;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.LayoutEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.store.StoreEvent;
import com.extjs.gxt.ui.client.store.StoreListener;
import com.extjs.gxt.ui.client.store.TreeStore;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.form.SimpleComboBox;
import com.extjs.gxt.ui.client.widget.form.SimpleComboValue;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.FillToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.LabelToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.PagingToolBar;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientMessages;
import com.prelert.client.ClientUtil;
import com.prelert.client.chart.ChartDataGrid;
import com.prelert.client.list.CausalityEvidenceDialog;
import com.prelert.data.Attribute;
import com.prelert.data.CausalityView;
import static com.prelert.data.PropertyNames.METRIC;
import com.prelert.data.TimeFrame;
import com.prelert.data.gxt.ActivityTreeModel;
import com.prelert.data.gxt.CausalityDataModel;
import com.prelert.data.gxt.IncidentModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.CausalityQueryServiceAsync;


/**
 * Ext GWT (GXT) widget for displaying the summary of an activity in the Analysis view. 
 * The widget consists of a tree component on the left, displaying an analysis of the 
 * activity by shared attributes, and a grid for listing and selecting the time series 
 * features and notifications in the selected summary tree row.
 * 
 * @author Pete Harverson
 */
public class AnalysisSummaryTreeWidget extends ContentPanel
{
	private CausalityQueryServiceAsync 	m_CausalityQueryService;
	
	private IncidentModel				m_Incident;
	
	private ToolBar 					m_PathToolBar;
	private ActivityMetricPathLabel		m_PathField;
	private SimpleComboBox<String>		m_AnalyzeByCombo;
	private ActivitySummaryTreeGrid		m_SummaryTree;
	private TreeStore<ActivityTreeModel> m_SummaryTreeStore;
	private ActivitySummaryTreeLoader 	m_SummaryTreeLoader;
	
	private ChartDataGrid<CausalityDataModel>	m_ChartDataGrid;
	private PagingToolBar 				m_ChartDataGridToolBar;
	private CausalityDataPagingLoader 	m_ChartDataGridLoader;
	private List<String>				m_ChartDataGridAttributes;
	private SimpleComboBox<String> 		m_FilterNameCombo;
	private LabelToolItem 				m_FilterOperator;
	private SimpleComboBox<String> 		m_FilterValueCombo;
	
	
	/**
	 * Creates a new widget for displaying the summary of an activity in the Analysis view.
	 */
	public AnalysisSummaryTreeWidget()
	{
		m_CausalityQueryService = AsyncServiceLocator.getInstance().getCausalityQueryService();
		
		initComponents();
	}
	
	
	/**
	 * Creates and initialises the graphical components in the widget.
	 */
	protected void initComponents()
	{
		setScrollMode(Scroll.NONE);
		
		// Create the toolbar displaying the metric path.
		m_PathToolBar = new ToolBar();
		m_PathToolBar.setEnableOverflow(false);
		m_PathToolBar.setSpacing(3);
		m_PathToolBar.setBorders(true);
		
		final LabelToolItem pathLabel = new LabelToolItem("Path:");
		final LabelToolItem viewLabel = new LabelToolItem("Analyze by:");
		
		
		m_AnalyzeByCombo = new SimpleComboBox<String>();
		m_AnalyzeByCombo.setWidth(130);
		m_AnalyzeByCombo.setTriggerAction(TriggerAction.ALL); 
		m_AnalyzeByCombo.setEditable(false);
		m_AnalyzeByCombo.setListStyle("prl-combo-list");
		m_AnalyzeByCombo.addListener(Events.SelectionChange, 
				new SelectionChangedListener<SimpleComboValue<String>>(){

	        @Override
	        public void selectionChanged(SelectionChangedEvent<SimpleComboValue<String>> se)
	        {
	        	String useAnalyzeBy = getAnalyzeBy();	    	
	    		boolean metricPathOrder = useAnalyzeBy.equals(
	    				ClientUtil.CLIENT_CONSTANTS.analyseOptionPathOrder());
	    		if ( (metricPathOrder == true) || 
	    				(useAnalyzeBy.equals(ClientUtil.CLIENT_CONSTANTS.analyseOptionMostCommon()) == true) )
	    		{
	    			useAnalyzeBy = null;
	    		}
	    		m_SummaryTreeLoader.setAnalyzeBy(useAnalyzeBy);
	    		m_SummaryTreeLoader.setLoadInMetricPathOrder(metricPathOrder);
	    		
	    		loadTreeToBranchPoint();
	        }
        });
		m_AnalyzeByCombo.enableEvents(false);
		
		
		m_PathField = new ActivityMetricPathLabel();
		final LayoutContainer pathFieldContainer = new LayoutContainer();
		pathFieldContainer.add(m_PathField);
		pathFieldContainer.setBorders(true);
		pathFieldContainer.addStyleName("prl-title-label");	
	
		
		m_PathToolBar.add(pathLabel);
		m_PathToolBar.add(pathFieldContainer);
		
		m_PathToolBar.add(viewLabel);
		m_PathToolBar.add(m_AnalyzeByCombo);
		
		
		// Create the tree to display a summary of the activity.
		m_SummaryTreeLoader = new ActivitySummaryTreeLoader();
		m_SummaryTreeLoader.addLoadListener(new LoadListener(){

			@Override
            public void loaderBeforeLoad(LoadEvent le)
            {
            	m_PathToolBar.setEnabled(false);
            	
            	ActivityTreeModel loadConfig = (ActivityTreeModel)(le.getConfig());
            	if (loadConfig != null)
            	{
            		ActivityTreeModel parent = m_SummaryTreeStore.getParent(loadConfig);
					ArrayList<String> pathAttributeValues = new ArrayList<String>();
					if (loadConfig.getAttributeName() != null)
					{
						pathAttributeValues.add(loadConfig.getAttributeValue());
					}
					while (parent != null)
					{
						if (parent.getAttributeName() != null)
						{
							pathAttributeValues.add(0, parent.getAttributeValue());
						}
						parent = m_SummaryTreeStore.getParent(parent);
					}
					loadConfig.setPathAttributeValues(pathAttributeValues);
            	}
            }
			
			
            @SuppressWarnings("unchecked")
            @Override
            public void loaderLoad(LoadEvent le)
            {
            	// Don't unmask the tree on initial (empty) loads.
            	List<ActivityTreeModel> data = (List<ActivityTreeModel>)le.getData();
            	if (data != null && data.size() > 0)
            	{
            		m_SummaryTree.unmask();
            		m_PathToolBar.setEnabled(true);
            	}
            }
            

            @Override
            public void loaderLoadException(LoadEvent le)
            {
	            GWT.log("AnalysisSummaryTreeWidget error loading tree: " + le.exception);
	            le.exception.printStackTrace();
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
		                ClientUtil.CLIENT_CONSTANTS.errorLoadingAnalysisData(), null);
				m_PathField.setText("");
            } 
			
		});
		m_SummaryTreeStore = new TreeStore<ActivityTreeModel>(m_SummaryTreeLoader);
		m_SummaryTree = new ActivitySummaryTreeGrid(m_SummaryTreeStore, true);

		m_SummaryTree.setBorders(true);
		
		m_SummaryTree.getSelectionModel().addSelectionChangedListener(
     			new SelectionChangedListener<ActivityTreeModel>(){

			@Override
            public void selectionChanged(SelectionChangedEvent<ActivityTreeModel> se)
            {
				// Update metric path field to indicate selected node and reload selection grid.
				ActivityTreeModel selectedNode = m_SummaryTree.getSelectionModel().getSelectedItem();
				if (selectedNode != null)
				{
					ActivityTreeModel node = selectedNode;
					ActivityTreeModel parent = m_SummaryTreeStore.getParent(selectedNode);
					ArrayList<ActivityTreeModel> pathToRoot = new ArrayList<ActivityTreeModel>();
					while (parent != null)
					{
						pathToRoot.add(node);
						node = parent;
						parent = m_SummaryTreeStore.getParent(node);
					}
					
					m_PathField.setPartialPathTreeNode(pathToRoot);
					loadSelectionGrid();
				}
            }
     	});
	    
		m_SummaryTreeStore.addStoreListener(new StoreListener<ActivityTreeModel>(){

			@Override
            public void storeClear(StoreEvent<ActivityTreeModel> se)
            {
				m_PathField.setText("");
				m_ChartDataGrid.getStore().removeAll();
            }
            
	    });


		// Create the selection grid.
		ContentPanel chartDataGrid = createSelectionGrid();
		
		final BorderLayout borderLayout = new BorderLayout();   
		borderLayout.setContainerStyle("prl-border-layout-ct");
	    setLayout(borderLayout); 
	    
	    BorderLayoutData northData = new BorderLayoutData(LayoutRegion.NORTH, 28); 
	    northData.setSplit(false);   
	    northData.setFloatable(false);   
	    northData.setMargins(new Margins(5, 5, 0, 5)); 
	    
	    BorderLayoutData westData = new BorderLayoutData(LayoutRegion.WEST, 0.4f, 300, 800); 
	    westData.setSplit(true);   
	    westData.setFloatable(false);   
	    westData.setMargins(new Margins(0, 5, 5, 5)); 
	    
	    BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);   
	    centerData.setMargins(new Margins(0, 5, 5, 0));
	    
	    add(m_PathToolBar, northData);
	    add(m_SummaryTree, westData);
	    add(chartDataGrid, centerData);   
	    
	    // Auto resize metric path field to fill available width.
	    borderLayout.addListener(Events.AfterLayout, new Listener<LayoutEvent>(){

			@Override
            public void handleEvent(LayoutEvent be)
            {	
				pathFieldContainer.setWidth(m_PathToolBar.getWidth() - m_AnalyzeByCombo.getWidth() -
						pathLabel.getWidth() - viewLabel.getWidth() - 20);
            }
			
		});
	}
	
	
	/**
	 * Creates the grid component used for displaying and selecting the
	 * causality data to add to the chart.
	 * @return ContentPanel holding the chart data grid.
	 */
    protected ContentPanel createSelectionGrid()
    {	
    	// Create the chart data grid and its loader.
	    m_ChartDataGridLoader = new CausalityDataPagingLoader();
	    m_ChartDataGridLoader.setRemoteSort(true);
	    ListStore<CausalityDataModel> listStore = new ListStore<CausalityDataModel>(m_ChartDataGridLoader);
	    listStore.setModelComparer(new ModelComparer<CausalityDataModel>(){

			@Override
            public boolean equals(CausalityDataModel m1, CausalityDataModel m2)
            {
	           return m1.equalsIgnoreMetrics(m2);
            }
	    	
	    });
	    
	    m_ChartDataGridLoader.addLoadListener(new LoadListener(){

            @Override
            public void loaderLoadException(LoadEvent le)
            {
            	GWT.log("AnalysisSummaryTreeWidget chart selection grid loaderLoadException: " + 
            			le.exception);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
		                ClientUtil.CLIENT_CONSTANTS.errorLoadingAnalysisData(), null);
            	
				m_ChartDataGrid.unmask();
				m_ChartDataGridToolBar.setEnabled(true);
            }
	    	
	    });
	    
	    // Grid columns are configured each time a new evidence is supplied.
	    m_ChartDataGrid = new ChartDataGrid<CausalityDataModel>(
	    		listStore, new ArrayList<ColumnConfig>());
	    
	    // Create the paging toolbar with a default page size of 20, and then get
	    // the configured page size from the server (defined in client.properties).
	    m_ChartDataGridToolBar = new PagingToolBar(20){

            @Override
            protected void onLoad(LoadEvent event)
            {
            	setEnabled(true);
	            super.onLoad(event);
            }
	    	
	    };
	    
	    m_CausalityQueryService.getSelectionGridPageSize(new ApplicationResponseHandler<Integer>(){

			@Override
            public void uponFailure(Throwable problem)
            {
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
		        		ClientUtil.CLIENT_CONSTANTS.errorAnalysisData(), null);
            }

			@Override
            public void uponSuccess(Integer pageSize)
            {
				m_ChartDataGridToolBar.setPageSize(pageSize);
	        	m_ChartDataGridLoader.setLimit(pageSize);
            }
		
		});
	    
	    // Hide the refresh button and display label from the paging toolbar.
	    m_ChartDataGridToolBar.getItem(9).hide();
	    m_ChartDataGridToolBar.getItem(10).hide();
	    m_ChartDataGridToolBar.getItem(11).hide();
	    m_ChartDataGridToolBar.getItem(12).hide();

	    // Add filter controls to the data grid's toolbar.
		m_FilterNameCombo = new SimpleComboBox<String>();
		m_FilterNameCombo.add(ClientUtil.CLIENT_CONSTANTS.optionAll());
		m_FilterNameCombo.setEditable(false);
		m_FilterNameCombo.setListStyle("prl-combo-list");
		m_FilterNameCombo.setWidth(120);
		m_FilterNameCombo.setTriggerAction(TriggerAction.ALL);
		m_FilterNameCombo.setSimpleValue(ClientUtil.CLIENT_CONSTANTS.optionAll());
		m_FilterNameCombo.addListener(Events.SelectionChange, new SelectionChangedListener<SimpleComboValue<String>>(){

	        @Override
	        public void selectionChanged(SelectionChangedEvent<SimpleComboValue<String>> se)
	        {
	        	populateSelectionGridFilterValues();
	        }
        });
		
		m_FilterOperator = new LabelToolItem();
		m_FilterOperator.setStyleAttribute("text-align", "center");
		m_FilterOperator.setWidth("15px");
		
		m_FilterValueCombo = new SimpleComboBox<String>();
		m_FilterValueCombo.add(ClientUtil.CLIENT_CONSTANTS.optionAll());
		m_FilterValueCombo.setEditable(false);
		m_FilterValueCombo.setListStyle("prl-combo-list");
		m_FilterValueCombo.setWidth(180);
		m_FilterValueCombo.setTriggerAction(TriggerAction.ALL);  
		m_FilterValueCombo.setSimpleValue(ClientUtil.CLIENT_CONSTANTS.optionAll());
		m_FilterValueCombo.addListener(Events.SelectionChange, new SelectionChangedListener<SimpleComboValue<String>>(){

	        @Override
	        public void selectionChanged(SelectionChangedEvent<SimpleComboValue<String>> se)
	        {
	        	loadSelectionGrid();
	        }
        });
		
		// Specify a custom template for the drop-down list items which uses
		// Quicktip tooltips, allowing the use to read long items.
		StringBuilder valToolTip = new StringBuilder();
		valToolTip.append("<tpl for=\".\"><div class=\"prl-combo-list-item\" qtip=\"{value");
		valToolTip.append("}\" qtitle=\"\">{value}</div></tpl>");
		m_FilterValueCombo.setTemplate(valToolTip.toString());
		
		m_ChartDataGridToolBar.add(new FillToolItem());
		m_ChartDataGridToolBar.add(new LabelToolItem(ClientUtil.CLIENT_CONSTANTS.fieldFilter()));
		m_ChartDataGridToolBar.add(m_FilterNameCombo);
		m_ChartDataGridToolBar.add(m_FilterOperator);
		m_ChartDataGridToolBar.add(m_FilterValueCombo);
		m_ChartDataGridToolBar.bind(m_ChartDataGridLoader);
		
		ContentPanel gridPanel = new ContentPanel();
		gridPanel.setHeaderVisible(false);
		gridPanel.setLayout(new FitLayout());
		gridPanel.setTopComponent(m_ChartDataGridToolBar);
		gridPanel.add(m_ChartDataGrid);
	   
	    
	    // Double-click on a row to open the Causality Evidence dialog.
	    m_ChartDataGrid.addListener(Events.RowDoubleClick, 
	    		new Listener<GridEvent<CausalityDataModel>>(){

			@Override
            public void handleEvent(GridEvent<CausalityDataModel> ge)
            {
				CausalityEvidenceDialog evidenceDialog = 
					CausalityEvidenceDialog.getInstance();
				evidenceDialog.loadForCausalityData(ge.getModel(), m_Incident.getEvidenceId());
				evidenceDialog.show();
            }
	    	
	    });
	    
	    return gridPanel;
    }
      

    /**
     * Loads causality data into the summary tree widget for the specified incident.
     * @param analyzeBy the name of the attribute by which causality data should be 
	 * 		analysed, or {@link com.prelert.client.ClientMessages#analyseOptionMostCommon()} 
	 * 		to use the default analysis based on count, or
	 * 		{@link com.prelert.client.ClientMessages#analyseOptionPathOrder()} to analyze the
	 * 		attributes in metric path order.
     * @param incident incident for which to display a summary.
     * @param causalityView configuration data for the incident.
     */
    public void load(String analyzeBy, IncidentModel incident, CausalityView causalityView)
	{
		m_Incident = incident;
		
		m_PathField.setMetricPathTemplate(causalityView.getMetricPathTreeNodes());
		setAnalyzeByAttributes(causalityView.getAttributes(), analyzeBy);
		
		// Load new data into the tree.
		m_SummaryTree.setIncident(incident);
		
		String useAnalyzeBy = getAnalyzeBy();
		if (analyzeBy != null)
		{
			useAnalyzeBy = analyzeBy;
		}
		
		m_SummaryTreeLoader.setEvidenceId(incident.getEvidenceId());
		m_SummaryTreeLoader.setTreeAttributeNames(causalityView.getAttributes());
		boolean metricPathOrder = useAnalyzeBy.equals(ClientUtil.CLIENT_CONSTANTS.analyseOptionPathOrder());
		if ( (metricPathOrder == true) || 
				(useAnalyzeBy.equals(ClientUtil.CLIENT_CONSTANTS.analyseOptionMostCommon()) == true) )
		{
			useAnalyzeBy = null;
		}
		
		m_SummaryTreeLoader.setAnalyzeBy(useAnalyzeBy);
		
		m_SummaryTreeLoader.setLoadInMetricPathOrder(metricPathOrder);
		
		//m_SummaryTree.getView().refresh(true);
		loadTreeToBranchPoint();

		
		// Reconfigure selection grid with new columns.
		m_ChartDataGridAttributes = new ArrayList<String>(causalityView.getAttributes());
		m_ChartDataGridAttributes.remove(METRIC);
		m_ChartDataGrid.setColumns(getSelectionGridColumns());
		populateSelectionGridFilterNames();
		
		// Sort selection grid by significance if no other sort field is set.
		boolean applyDefaultSort = true;
		String sortField = m_ChartDataGridLoader.getSortField();
		if (sortField != null)
		{
			ColumnModel selectionGridColumns = m_ChartDataGrid.getColumnModel();
			applyDefaultSort = (selectionGridColumns.getColumnById(sortField) == null);
		}
		if (applyDefaultSort == true)
		{
			m_ChartDataGridLoader.setSortDir(SortDir.DESC);
			m_ChartDataGridLoader.setSortField(SIGNIFICANCE);
		}
	}
    
    
    /**
     * Loads the tree, automatically expanding the levels in the tree until the
     * first node which has more than one child.
     */
    protected void loadTreeToBranchPoint()
    {
    	m_SummaryTreeLoader.addLoadListener(new LoadListener(){

            @Override
            public void loaderLoad(LoadEvent le)
            {
            	List<ActivityTreeModel> nodes = le.getData();
            	if (nodes != null && nodes.size() > 0)
            	{
            		ActivityTreeModel firstNode = nodes.get(0);
            		if (nodes.size() == 1 && firstNode.isLeaf() == false)
            		{
            			m_SummaryTree.setExpanded(firstNode, true);
            		}
            		else
            		{
            			m_SummaryTree.getSelectionModel().select(0, false);
                    	m_SummaryTreeLoader.removeLoadListener(this);                    	
            		}
            		
            	}
            }     	
		});	
    	
		m_SummaryTree.getTreeStore().removeAll();
		m_SummaryTreeLoader.load();

    }
	
	
	/**
	 * Loads a page of causality data into the chart selection grid according to the
	 * current configuration (evidence id, selected analyis tree node, filter, sort).
	 */
	public void loadSelectionGrid()
	{
		ActivityTreeModel selectedNode = m_SummaryTree.getSelectionModel().getSelectedItem();
		if (selectedNode != null)
		{
			m_ChartDataGridLoader.setEvidenceId(m_Incident.getEvidenceId());
			m_ChartDataGridLoader.setReturnAttributes(m_ChartDataGridAttributes);

			List<Attribute> primaryFilterAttributes = new ArrayList<Attribute>();
			ActivityTreeModel node = selectedNode;
			ActivityTreeModel parent = m_SummaryTreeStore.getParent(selectedNode);
			while (parent != null)
			{
				primaryFilterAttributes.add(
						new Attribute(node.getAttributeName(), node.getAttributeValue()));

				node = parent;
				parent = m_SummaryTreeStore.getParent(node);
			}
			
			
			m_ChartDataGridLoader.setPrimaryFilterAttributes(primaryFilterAttributes);
			
			String secondaryFilterName = getSelectionGridFilterName();
			String secondaryFilterValue = getSelectionGridFilterValue();
			
			m_ChartDataGridLoader.setSecondaryFilterName(secondaryFilterName);
			m_ChartDataGridLoader.setSecondaryFilterValue(secondaryFilterValue);
			
			m_ChartDataGridLoader.load();			
		}
	}
	
	
	/**
	 * Returns the tree component of the widget which displays an analysis of 
	 * the activity in a tree grid.
	 * @return the <code>ActivitySummaryTreeGrid</code> component.
	 */
	public ActivitySummaryTreeGrid getTree()
	{
		return m_SummaryTree;
	}
	
	
	/**
	 * Returns the <code>ChartDataGrid</code> component of the widget which allows
	 * the user to select from the time series and notifications in the activity.
	 * @return the <code>ChartDataGrid</code> component.
	 */
	public ChartDataGrid<CausalityDataModel> getSelectionGrid()
	{
		return m_ChartDataGrid;
	}
	
	
	/**
	 * Returns the selected value of the 'Analyze by' combo box.
	 * @return the current 'Analyze by' attribute name,
	 * {@link com.prelert.client.ClientMessages#analyseOptionMostCommon()} or
	 * {@link com.prelert.client.ClientMessages#analyseOptionPathOrder()}.
	 */
	public String getAnalyzeBy()
	{
		return m_AnalyzeByCombo.getSimpleValue();
	}
	
	
	/**
	 * Enables or disables components in the widget.
	 * @param enableDisplays <code>true</code> to enable the displays,
	 * 	<code>false</code> to disable.
	 * @param maskMessage optional loading mask message to display when 
	 * 	disabling components.
	 */
	public void setEnabledDisplays(boolean enableDisplays, String maskMessage)
	{
		if (enableDisplays == false)
		{
			m_SummaryTree.mask(maskMessage);
			m_ChartDataGrid.mask(maskMessage);
		}
		else
		{
			m_SummaryTree.unmask();
			m_ChartDataGrid.unmask();
		}
		
		m_PathToolBar.setEnabled(enableDisplays);
		m_ChartDataGridToolBar.setEnabled(enableDisplays);
	}


	/**
	 * Sets the list of attributes in the 'Analyse by' ComboBox.
	 * @param attributeNames list of names of the attributes in the metric path.
	 * @param analyzeBy	optional value to set in the ComboBox. If <code>null</null>
	 * 	the previous setting will be retained if it is in the supplied list.
	 */
	protected void setAnalyzeByAttributes(List<String> attributeNames, String analyzeBy)
	{
		// Add options for 'Path order' and 'Most common'.
		ArrayList<String> fullList = new ArrayList<String>(attributeNames);		
		fullList.add(0, ClientUtil.CLIENT_CONSTANTS.analyseOptionPathOrder());
		fullList.add(1, ClientUtil.CLIENT_CONSTANTS.analyseOptionMostCommon());
		
		String viewBy = (analyzeBy != null ? analyzeBy : m_AnalyzeByCombo.getSimpleValue());
		if (viewBy == null || viewBy.length() == 0 ||
				fullList.indexOf(viewBy) == -1)
		{
			viewBy = ClientUtil.CLIENT_CONSTANTS.analyseOptionPathOrder();
		}
		
		m_AnalyzeByCombo.enableEvents(false);
		m_AnalyzeByCombo.clearSelections();
		m_AnalyzeByCombo.removeAll();
		m_AnalyzeByCombo.add(fullList);
		m_AnalyzeByCombo.setSimpleValue(viewBy);
		m_AnalyzeByCombo.enableEvents(true);
	}
    
    
    /**
	 * Populates the filter attribute name ComboBox in the selection grid toolbar
	 * with values according to the summary grid 'Group by' setting.
	 */
	protected void populateSelectionGridFilterNames()
	{
		m_FilterNameCombo.clearSelections();
		m_FilterNameCombo.removeAll();
		
		// Add an 'All' option, description, then the attributes in order from the metric path.
		m_FilterNameCombo.add(ClientUtil.CLIENT_CONSTANTS.optionAll());
		m_FilterNameCombo.add(DESCRIPTION);
		if (m_ChartDataGridAttributes != null)
		{
			m_FilterNameCombo.add(m_ChartDataGridAttributes);
		}
		
		
		// TODO
		// Remove values for all the levels in the selected node.
		//m_FilterNameCombo.remove(getAnalyzeBy());
		
		m_FilterNameCombo.setSimpleValue(ClientUtil.CLIENT_CONSTANTS.optionAll());
		
		populateSelectionGridFilterValues();
	}
	
	
	/**
	 * Populates the filter attribute value ComboBox in the selection grid toolbar
	 * with the list of values for the currently selected node in the summary tree.
	 */
	protected void populateSelectionGridFilterValues()
	{
		boolean disableEvents = (getSelectionGridFilterValue() == null);
		
		m_FilterValueCombo.disableEvents(disableEvents);
		m_FilterValueCombo.clearSelections();
		m_FilterValueCombo.removeAll();
		
		m_FilterValueCombo.add(ClientUtil.CLIENT_CONSTANTS.optionAll());
		m_FilterValueCombo.setSimpleValue(ClientUtil.CLIENT_CONSTANTS.optionAll());
		m_FilterValueCombo.enableEvents(true);
		
		String attributeName = getSelectionGridFilterName();
		if (attributeName != null)
		{
				m_FilterOperator.setLabel("=");
				
				// Get the list of values for this attribute.
				ApplicationResponseHandler<List<String>> callback = 
					new ApplicationResponseHandler<List<String>>()
				{
					@Override
		            public void uponFailure(Throwable caught)
					{
						GWT.log(ClientUtil.CLIENT_CONSTANTS.errorLoadingAnalysisData() + ": ", caught);
						MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
								ClientUtil.CLIENT_CONSTANTS.errorLoadingAnalysisData(), null);
					}


					@Override
		            public void uponSuccess(List<String> attributeValues)
					{	
						m_FilterValueCombo.add(attributeValues);
					}
				};
				
				m_CausalityQueryService.getCausalityDataColumnValues(
						m_Incident.getEvidenceId(), attributeName, callback);
		}
		else
		{
			m_FilterOperator.setLabel("");
		}
	}
	
	
	/**
	 * Returns the current setting of the filter attribute name ComboBox in the
	 * selection grid toolbar.
	 * @return the selected attribute name, or <code>null</code> if the 'All' option
	 * 	is selected.
	 */
	protected String getSelectionGridFilterName()
	{
		String gridFilterName = null;
		
		String comboValue = m_FilterNameCombo.getSimpleValue();
		if ( (comboValue != null) && 
				(comboValue.equals(ClientUtil.CLIENT_CONSTANTS.optionAll()) == false) )
		{
			gridFilterName = comboValue;
		}

		return gridFilterName;
	}
	
	
	/**
	 * Returns the current setting of the filter attribute value ComboBox in the
	 * selection grid toolbar.
	 * @return the selected attribute value, or <code>null</code> if the 'All' option
	 * 	is selected.
	 */
	protected String getSelectionGridFilterValue()
	{
		String gridFilterValue = null;

		String comboValue = m_FilterValueCombo.getSimpleValue();
		if ( (comboValue != null) && 
				(comboValue.equals(ClientUtil.CLIENT_CONSTANTS.optionAll()) == false) )
		{
			gridFilterValue = comboValue;
		}

		return gridFilterValue;
	}
	
	
	/**
	 * Returns the list of selection grid columns for the current item of evidence.
	 * @return list of GXT ColumnConfig objects.
	 */
	protected List<ColumnConfig> getSelectionGridColumns()
	{
		ClientMessages messages = ClientUtil.CLIENT_CONSTANTS;
		
		// Add description, significance, magnitude, then the rest of the metric path.
		List<ColumnConfig> config = new ArrayList<ColumnConfig>();
		config.add(new ColumnConfig(DESCRIPTION, messages.description(), 150));
		config.add(new ColumnConfig(SIGNIFICANCE, messages.influence(),120));
		config.add(new ColumnConfig(MAGNITUDE, messages.magnitude(), 120));
		if (m_ChartDataGridAttributes != null)
		{
			for (String columnName : m_ChartDataGridAttributes)
			{
				config.add(new ColumnConfig(columnName, columnName, 120));
			}
		}
	    
	    // Add columns for the start and end times, with custom date/time format.
	    DateTimeFormat dateFormat = ClientUtil.getDateTimeFormat(TimeFrame.SECOND);

	    ColumnConfig startTime = new ColumnConfig(START_TIME, messages.startTime(), 120);
	    startTime.setDateTimeFormat(dateFormat);  
	    
	    ColumnConfig endTime = new ColumnConfig(END_TIME, messages.endTime(), 120);
	    endTime.setDateTimeFormat(dateFormat);   
	    
	    config.add(startTime);
	    config.add(endTime);

	    return config;
	}
}
