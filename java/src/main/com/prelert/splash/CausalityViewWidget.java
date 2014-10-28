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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.SortDir;
import com.extjs.gxt.ui.client.Style.VerticalAlignment;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FieldEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.event.SplitBarEvent;
import com.extjs.gxt.ui.client.store.GroupingStore;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.store.StoreSorter;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.ComponentManager;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.HorizontalPanel;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.SplitBar;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.WidgetComponent;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.CheckBox;
import com.extjs.gxt.ui.client.widget.form.HiddenField;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.TableData;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout.VBoxLayoutAlign;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.extjs.gxt.ui.client.widget.toolbar.FillToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.LabelToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.CSSColorChart;
import com.prelert.client.CSSSymbolChart;
import com.prelert.client.CSSSymbolChart.Shape;
import com.prelert.client.ClientUtil;
import com.prelert.client.chart.CausalityChartWidget;
import com.prelert.client.chart.CausalityGChartWidget;
import com.prelert.client.chart.ChartSymbolCellRenderer;
import com.prelert.client.chart.ChartToolBar;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.RequestViewEvent;
import com.prelert.client.gxt.ExtendedRowExpander;
import com.prelert.client.gxt.GroupingSelectGrid;
import com.prelert.client.list.CausalityEvidenceDialog;
import com.prelert.client.list.AttributeListDialog;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.Severity;
import com.prelert.data.TimeFrame;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.data.gxt.CausalityDataModel;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.data.gxt.ProbableCauseModel;
import com.prelert.data.gxt.ProbableCauseModelCollection;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.CausalityQueryServiceAsync;
import com.prelert.service.EvidenceQueryServiceAsync;


/**
 * Ext GWT (GXT) widget for displaying a causality view. The widget consists of
 * a chart displaying time series and notification data, with a grid listing the
 * probable causes and symptoms for an item of evidence or time series discord.
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
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
public class CausalityViewWidget extends LayoutContainer
{
	
	/**
	 * Enumeration KeyPosition for the location of the key relative to the chart.
	 */
	public static enum KeyPosition { BOTTOM, RIGHT }
	
	private CausalityQueryServiceAsync 	m_CausalityQueryService;
	private EvidenceQueryServiceAsync 	m_EvidenceQueryService;
	
	private EvidenceModel				m_Model;		// The item of evidence or time series feature.
	
	private KeyPosition					m_KeyPosition;
	private ContentPanel				m_ChartContainer;
	private ChartToolBar<TimeSeriesDataPoint>	m_ChartTools;
	private CausalityChartWidget		m_ChartWidget;
	private boolean 					m_ShowChartLabels = true;
	
	private FormPanel					m_ExportForm;
	private FlowPanel					m_ExportFormWidgetPanel;
	private HiddenField<String>			m_ExportTitleField;
	
	private Grid<ProbableCauseModelCollection> 	m_Grid;
	private GridSelectionModel<ProbableCauseModelCollection> m_CheckboxSm;
	private ExtendedRowExpander<ProbableCauseModelCollection> m_RowExpander;
	private String								m_CheckboxIdPrefix;
	private Listener<SplitBarEvent>				m_SplitBarListener;
	
	/** Full height value for the causality chart. */
	public static final double			CHART_VALUE_RANGE = 1000d;
	
	public static final String[] DATA_EXPORT_FORMATS = {"CSV", "PDF"};
	
	
	/**
	 * Creates a new widget for displaying causality data.
	 * @param keyPosition	the position of the key relative to the causality chart,
	 * 						either to the bottom or the right.
	 * @param showChartLabels <code>true</code> to add a title and sub-title to the
	 * 						chart, <code>false</code> otherwise.
	 */
	public CausalityViewWidget(KeyPosition keyPosition, boolean showChartLabels)
	{
		m_CausalityQueryService = AsyncServiceLocator.getInstance().getCausalityQueryService();
		m_EvidenceQueryService = AsyncServiceLocator.getInstance().getEvidenceQueryService();
		
		m_CheckboxIdPrefix = new String(hashCode() + "cb");	// Unique id prefix for time series checkboxes.
		
		initComponents(keyPosition, showChartLabels);
	}
	
	
	/**
	 * Creates and initialises the graphical components in the window.
	 */
	protected void initComponents(KeyPosition keyPosition, boolean showChartLabels)
	{
		m_KeyPosition = keyPosition;
		m_ShowChartLabels = showChartLabels;
		
		BorderLayout layout = new BorderLayout();   
		layout.setContainerStyle("prl-border-layout-ct");
	    setLayout(layout); 
	    
		m_ChartContainer = createChartComponents();
		
		GroupingStore<ProbableCauseModelCollection> store = 
			new GroupingStore<ProbableCauseModelCollection>();   
		
		store.groupBy("dataSourceName");
		store.setStoreSorter(new StoreSorter<ProbableCauseModelCollection>(
				ClientUtil.CASE_INSENSITIVE_COMPARATOR));
		store.setDefaultSort("description", SortDir.ASC);
		
		
		// Create a row expander plugin to list the probable causes for 
		// each type/time/description aggregation.		  
		m_RowExpander = new ExtendedRowExpander<ProbableCauseModelCollection>(new ProbableCauseCollectionRenderer());
	    
	    ColumnConfig symbolColumn = new ColumnConfig("symbol", 
	    		ClientUtil.CLIENT_CONSTANTS.symbol(), 50);
	    symbolColumn.setRenderer(new ChartSymbolCellRenderer());
	    
	    ColumnConfig typeColumn = new ColumnConfig("dataSourceName", 
	    		ClientUtil.CLIENT_CONSTANTS.type(), 80);
	    ColumnConfig descColumn = new ColumnConfig("description", 
	    		ClientUtil.CLIENT_CONSTANTS.description(), 200);
	    descColumn.setRenderer(new GridCellRenderer<ProbableCauseModelCollection>() {

			@Override
			public Object render(ProbableCauseModelCollection collection,
	                String property, ColumnData config, int rowIndex, int colIndex,
	                ListStore<ProbableCauseModelCollection> store,
	                Grid<ProbableCauseModelCollection> grid)
            {
				final ProbableCauseModel probableCause = collection.getProbableCause(0);
				Anchor descLink = new Anchor(collection.getDescription(), true);
				descLink.setStyleName("prl-textLink");
				descLink.addClickHandler(new ClickHandler(){
					
					@Override
		            public void onClick(ClickEvent event)
		            {
						CausalityEvidenceDialog evidenceDialog = 
							CausalityEvidenceDialog.getInstance();
						evidenceDialog.setHeading(
								ClientUtil.CLIENT_CONSTANTS.notificationDataHeading(
								probableCause.getDescription()));
						evidenceDialog.setDataSourceName(probableCause.getDataSourceName());
						evidenceDialog.setEvidenceId(probableCause.getEvidenceId());
						evidenceDialog.setSingleDescription(true);
						evidenceDialog.setFilter(null);
						evidenceDialog.load();
						evidenceDialog.show();
		            }
		        	
		        });
				
				return descLink;
            }
	    });
	    
	    
	    ColumnConfig countColumn = new ColumnConfig("count", 
	    		ClientUtil.CLIENT_CONSTANTS.count(), 50);
	    ColumnConfig sourceColumn = new ColumnConfig("sourceCount", 
	    		ClientUtil.CLIENT_CONSTANTS.sourceCount(), 50);
	    

	    List<ColumnConfig> columnConfig = new ArrayList<ColumnConfig>();
	    columnConfig.add(m_RowExpander);
	    columnConfig.add(symbolColumn);
	    columnConfig.add(typeColumn);
	    columnConfig.add(descColumn);
	    columnConfig.add(countColumn);
	    columnConfig.add(sourceColumn);
	    
	    m_Grid = new GroupingSelectGrid<ProbableCauseModelCollection>(store, columnConfig)
	    {
            @Override
            protected void afterRenderView()
            {
	            super.afterRenderView();
	            
	            // Add a listener to the BorderLayout splitbar to resize the chart on drag end.
	            SplitBar splitBar = m_Grid.getData("splitBar");
	            if (splitBar != null)
	            {
	            	splitBar.addListener(Events.DragEnd, m_SplitBarListener);
	            }
            }
	    	
	    };

	    m_Grid.setBorders(true);
	    m_Grid.addPlugin(m_RowExpander);   
	    
	    m_CheckboxSm = m_Grid.getSelectionModel();
	    m_CheckboxSm.addSelectionChangedListener(
			new SelectionChangedListener<ProbableCauseModelCollection>(){

                @Override
                public void selectionChanged(
                        SelectionChangedEvent<ProbableCauseModelCollection> se)
                {
                	processSelectedItems(se.getSelection());
                }
			
		});
	    
	    
	    // Lay out the top-level container depending on the position of the key.
	    BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);   
	    centerData.setMargins(new Margins(0));
	    
	    BorderLayoutData keyData; 
	    
        switch (m_KeyPosition)
        {
        	case RIGHT:
        		keyData = new BorderLayoutData(LayoutRegion.EAST, 0.5f, 300, 800); 
        		keyData.setSplit(true);   
        		keyData.setFloatable(false);   
        		keyData.setMargins(new Margins(0, 0, 0, 5)); 
        		break; 		
        		
        	case BOTTOM:
        	default:
        		keyData = new BorderLayoutData(LayoutRegion.SOUTH, 280, 150, 450);   
        		keyData.setSplit(true);   
        		keyData.setFloatable(false);   
        		keyData.setMargins(new Margins(5, 0, 0, 0));  
        		break; 		
        }
        
        add(m_ChartContainer, centerData);   
        add(m_Grid, keyData); 
	    

	    m_SplitBarListener = new Listener<SplitBarEvent>()
		{
			public void handleEvent(SplitBarEvent sbe)
			{
				if (sbe.getSize() < 1)
				{
					return;
				}

				sizeChartToFit();
			}
		};      
	}


	/**
	 * Creates the graphical components for the chart.
	 * @return ContentPanel holding the chart component.
	 */
    protected ContentPanel createChartComponents()
	{  
        VBoxLayout layout = new VBoxLayout();    
        layout.setVBoxLayoutAlign(VBoxLayoutAlign.STRETCH);   
        ContentPanel chartContainer = new ContentPanel(layout);  
        
        if (m_ShowChartLabels == true)
        {
        	chartContainer.setHeading(ClientUtil.CLIENT_CONSTANTS.diagnosticsChart());
        }
        else
        {
        	chartContainer.setHeaderVisible(false);
        }
        
        
        // Create the chart widget for plotting notifications and time series.
        m_ChartWidget = new CausalityGChartWidget();
        m_ChartWidget.setChartHeight(410);

        
        // Listen for events to open notification and time series views.
		Listener<RequestViewEvent<?>> chartListener = new Listener<RequestViewEvent<?>>(){

            @Override
            public void handleEvent(RequestViewEvent<?> rve)
            {
            	// Propagate the event.
            	if (rve.getViewToOpenDataType().getDataCategory() == DataSourceCategory.NOTIFICATION)
            	{
            		CausalityDataModel causalityData = (CausalityDataModel)(rve.getModel());
            		fireOpenNotificationViewClick(causalityData);
            	}
            	else
            	{
            		fireEvent(rve.getType(), rve);
            	}
            }
			
		};
		
		m_ChartWidget.addListener(GXTEvents.OpenNotificationViewClick, chartListener);
		m_ChartWidget.addListener(GXTEvents.OpenTimeSeriesViewClick, chartListener);
        
        // Create the chart toolbar.
        ToolBar chartToolBar = createChartToolBar();
        
        LoadListener loadListener = new LoadListener()
		{
			@Override
            public void loaderBeforeLoad(LoadEvent le)
			{
				m_ChartTools.setEnabled(false);
			}


			@Override
            public void loaderLoad(LoadEvent le)
			{
				m_ChartTools.setEnabled(true);
			}


			@Override
            public void loaderLoadException(LoadEvent le)
			{
				m_ChartTools.setEnabled(true);
			}
		};
		m_ChartWidget.addLoadListener(loadListener);
             
        
        chartContainer.add(chartToolBar);
        chartContainer.add(m_ChartWidget.getChartWidget());
     	
     	return chartContainer;
	}
    
    
    /**
     * Creates the toolbar for the chart component.
     */
    public ToolBar createChartToolBar()
    {
    	ToolBar toolbar = new ToolBar(); 
        toolbar.addStyleName("prl-internal-toolbar");
        toolbar.setBorders(false);
        toolbar.setSpacing(8);
        
        // Add show key checkbox and label.
        final CheckBox showKeyToggle = new CheckBox();
        showKeyToggle.setValue(true);
        showKeyToggle.addListener(Events.Change, new Listener<FieldEvent>(){

			@Override
            public void handleEvent(FieldEvent be)
            {
				m_Grid.setVisible(showKeyToggle.getValue());
				sizeChartToFit();
				
				SplitBar splitBar = m_Grid.getData("splitBar");
	            if (splitBar != null)
	            {
	            	if (showKeyToggle.getValue() == true)
					{
						splitBar.addListener(Events.DragEnd, m_SplitBarListener);
					}
					else
					{
						splitBar.removeListener(Events.DragEnd, m_SplitBarListener);
					}
	            }  
            }
        	
        });
        
        LabelToolItem showKeyLabel = new LabelToolItem(ClientUtil.CLIENT_CONSTANTS.showKey());
        showKeyLabel.addStyleName("prl-checkbox-label");

        HorizontalPanel showKeyControls = new HorizontalPanel();
        showKeyControls.add(showKeyToggle);
        showKeyControls.add(showKeyLabel);
 
        
        // Add an Export button with CSV and PDF options.
		Button exportBtn = new Button(ClientUtil.CLIENT_CONSTANTS.export());
		exportBtn.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.toolbar_export()));
		Menu exportMenu = new Menu();  
		exportBtn.setMenu(exportMenu);
		
		MenuItem csvItem = new MenuItem(ClientUtil.CLIENT_CONSTANTS.exportTo("CSV"));
		csvItem.addSelectionListener(new SelectionListener<MenuEvent>()
		{
			@Override
            public void componentSelected(MenuEvent ce)
            {
				exportToCSV();
                
            }
		});
		exportMenu.add(csvItem);
		
		MenuItem pdfItem = new MenuItem(ClientUtil.CLIENT_CONSTANTS.exportTo("PDF"));
		pdfItem.addSelectionListener(new SelectionListener<MenuEvent>()
		{
			@Override
            public void componentSelected(MenuEvent ce)
            {
				exportToPDF();
            }
		});
		exportMenu.add(pdfItem);

		
		// Add a form panel to submit the export request,
		// with some hidden fields for passing chart parameters.
    	m_ExportForm = new FormPanel();
    	m_ExportForm.setMethod(FormPanel.METHOD_POST);
    	m_ExportForm.setEncoding(FormPanel.ENCODING_URLENCODED);
    	m_ExportForm.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler()
        {
	        public void onSubmitComplete(SubmitCompleteEvent event)
	        {
		        // This will only be called when the response is of type text/html,
		        // rather than the expected CSV or PDF format, which indicates that
		        // an error has occurred.
		        MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
		        		ClientUtil.CLIENT_CONSTANTS.errorExportingAnalysisData(), null);
	        }
        });
    	
    	m_ExportFormWidgetPanel = new FlowPanel();
    	m_ExportForm.add(m_ExportFormWidgetPanel);
    	
		m_ExportTitleField = new HiddenField<String>();
		m_ExportTitleField.setName("title");
		m_ExportFormWidgetPanel.add(m_ExportTitleField);
	    
	    // Add the zooming tools.
	    m_ChartTools = new ChartToolBar<TimeSeriesDataPoint>(m_ChartWidget, false);
        
		toolbar.add(showKeyControls);
        toolbar.add(exportBtn);
        toolbar.add(new WidgetComponent(m_ExportForm));
        toolbar.add(new FillToolItem());
        toolbar.add(m_ChartTools);
        
        return toolbar;
    }
    

	/**
	 * Loads the list of probable causes into the window.
	 */
    public void load()
	{
		if (m_Model != null)
		{
			m_Grid.mask(GXT.MESSAGES.loadMask_msg());
			m_ChartContainer.mask(GXT.MESSAGES.loadMask_msg());
			m_CheckboxSm.setFiresEvents(false);
			m_Grid.getStore().removeAll();
			m_CheckboxSm.setFiresEvents(true);
			m_ChartWidget.removeAll();	
			
			// Centre the causality chart in a fifteen minute window around the feature.
			Date modelTime = m_Model.getTime(TimeFrame.SECOND);	
			m_ChartWidget.setTimeMarker(modelTime);
			m_ChartWidget.setDateRange(
					new Date(modelTime.getTime() - (ClientUtil.CAUSALITY_METRICS_TIME_SPAN*1000)/2), 
					new Date(modelTime.getTime() + (ClientUtil.CAUSALITY_METRICS_TIME_SPAN*1000)/2));
			
			// Load the list of probable causes.
			ApplicationResponseHandler<List<ProbableCauseModelCollection>> callback = 
				new ApplicationResponseHandler<List<ProbableCauseModelCollection>>()
			{
				@Override
                public void uponFailure(Throwable caught)
				{
					GWT.log(ClientUtil.CLIENT_CONSTANTS.errorLoadingAnalysisData() + ": ", caught);
					MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
							ClientUtil.CLIENT_CONSTANTS.errorLoadingAnalysisData(), null);
					m_ChartContainer.unmask();
					m_Grid.unmask();
				}
	
	
				@Override
                public void uponSuccess(List<ProbableCauseModelCollection> aggregatedList)
				{	
					GWT.log("CausalityViewWidget.load() uponSuccess()");
					
					attachCollectionIds(aggregatedList);
					
					ListStore<ProbableCauseModelCollection> store = m_Grid.getStore();
					store.add(aggregatedList);
					m_Grid.unmask();
					
					selectDefault();
					m_ChartContainer.unmask();
				}
			};
			
			GWT.log("CausalityViewWidget.load() request data");
			m_CausalityQueryService.getAggregatedProbableCauses(
					m_Model.getId(), ClientUtil.CAUSALITY_METRICS_TIME_SPAN, callback);
			

			// Update the chart heading.
			if (m_ShowChartLabels)
	    	{
	        	final String formattedTime = ClientUtil.formatTimeField(modelTime, TimeFrame.SECOND);
	    		
	    		if (m_Model.getDescription() != null)
				{
	    			setChartHeading(ClientUtil.CLIENT_CONSTANTS.analysisFor(
							m_Model.getDescription(), formattedTime));
				}
				else
				{
					setChartHeading(ClientUtil.CLIENT_CONSTANTS.diagnosticsChart());
					
					// Obtain the full evidence record so we can get its description.
					ApplicationResponseHandler<EvidenceModel> evidenceCallback = 
						new ApplicationResponseHandler<EvidenceModel>()
					{
						@Override
		                public void uponFailure(Throwable caught)
						{
							MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
									ClientUtil.CLIENT_CONSTANTS.errorLoadingEvidenceData(), null);
						}
			
			
						@Override
		                public void uponSuccess(EvidenceModel evidence)
						{	
							setEvidence(evidence);
							setChartHeading(ClientUtil.CLIENT_CONSTANTS.analysisFor(
									m_Model.getDescription(), formattedTime));
						}
					};
					
					m_EvidenceQueryService.getEvidenceSingle(m_Model.getId(), evidenceCallback);
				}
	    	}
		}
	}
    
    
    /**
     * Updates the chart title and subtitle for the current item of evidence.
     */
    public void setChartHeading(String heading)
    {
    	m_ChartContainer.setHeading(heading);
    }
    
    
	/**
     * Sets the item of evidence or time series feature whose probable causes 
     * are to be displayed in the widget. The following evidence properties are
     * used by the causality view:
     * <ul>
     * <li>id - to load the causality data</li>
     * <li>time - to set the time marker and window of data loaded</li>
     * <li>description - to set the chart title</li>
     * </ul>
     * @param model the item of evidence whose probable causes are being displayed.
     */
	public void setEvidence(EvidenceModel model)
	{
		m_Model = model;
	}
	
	
    /**
     * Returns the item of evidence or time series feature whose 
     * probable causes are being displayed in the widget.
     * @return the data model whose probable causes are being displayed.
     */
	public EvidenceModel getEvidence()
	{
		return m_Model;
	}
	
	
	/**
	 * Selects the default list of probable causes to display in the causality
	 * chart when it is first opened. This will consist of the source item of
	 * evidence or time series, all notification types, plus the top time series
	 * for each data type (by normalised by peak value).
	 */
	public void selectDefault()
	{
		List<ProbableCauseModelCollection> aggregatedList = m_Grid.getStore().getModels();
		
		List<ProbableCauseModelCollection> listToSelect = 
			new ArrayList<ProbableCauseModelCollection>();
		
		for (ProbableCauseModelCollection model : aggregatedList)
		{
			if (model.getDisplay() == true)
			{
				// Directly add them to the chart as otherwise there can be a
				// 1-2 sec delay on IE till the CheckBoxSelectionModel event fires.
				addToChart(model);	
				listToSelect.add(model);
			}
		}
		
		// Select the checkboxes for the displayed ProbableCauseModelCollections.
		m_CheckboxSm.setFiresEvents(false);
		m_CheckboxSm.select(listToSelect, false);
		m_CheckboxSm.setFiresEvents(true);
	}
	
	
	/**
	 * Attaches ids to the supplied list of ProbableCauseModelCollection objects
	 * so that they can be readily identified during UI operations.
	 * @param aggregatedList list of aggregated probable causes. 
	 */
	protected void attachCollectionIds(List<ProbableCauseModelCollection> aggregatedList)
	{
		int idCounter = 0;
		
		ArrayList<ProbableCauseModelCollection> notificationTypes = 
			new ArrayList<ProbableCauseModelCollection>();

		// Sort out the notification and time series types.
		for (ProbableCauseModelCollection collection : aggregatedList)
		{
			DataSourceCategory category = collection.getDataSourceCategory();
			switch (category)
			{
				case NOTIFICATION:
					notificationTypes.add(collection);
					break;
					
				case TIME_SERIES:
					// This is used to map to a unique color chart index.
					collection.setId(idCounter);
					idCounter++;
					
					// Display the first ProbableCauseModel in the aggregated list.
					List<ProbableCauseModel> probCauses = collection.getProbableCauses();
					if (probCauses != null && probCauses.size() > 0)
					{
						probCauses.get(0).setDisplay(true);
					}
					
					break;
			}
		}
		
		if (notificationTypes.size() > 0)
		{
			// Sort notifications by severity to ensure each severity makes use 
			// of the full range of symbols available.
			Collections.sort(notificationTypes, new Comparator<ProbableCauseModelCollection>(){

				@Override
                public int compare(ProbableCauseModelCollection collection1, 
                		ProbableCauseModelCollection collection2)
                {
	                return collection1.getSeverity().compareTo(collection2.getSeverity());
                }
			});
			
			// Set id counter to match with start of symbol set for each severity.
			int numSymbols = CSSSymbolChart.getInstance().getNumberOfSymbols();
			int remaining = numSymbols - (idCounter % numSymbols);
			idCounter = idCounter + remaining;
			
			Severity prevSev = notificationTypes.get(0).getSeverity();
			Severity severity;
			for (ProbableCauseModelCollection model : notificationTypes)
			{
				severity = model.getSeverity();
				
				if (severity != prevSev)
				{
					remaining = numSymbols - (idCounter % numSymbols);
					idCounter = idCounter + remaining;
				}
				
				model.setId(idCounter);
				
				idCounter++;
				prevSev = severity;
			}
		}
		
	}
	
	
	/**
	 * Processes the items that have been selected in the list, adding or removing
	 * corresponding notifications and time series to the chart.
	 * @param items list of probable causes that have been selected in the list.
	 */
	protected void processSelectedItems(List<ProbableCauseModelCollection> items)
	{
		// Remove any probable causes which have been deselected.
		List<ProbableCauseModelCollection> allItems = m_Grid.getStore().getModels();
		for (ProbableCauseModelCollection model : allItems)
		{
			if ( (model.getDisplay() == true) && (items.contains(model) == false) )
			{
				model.setDisplay(false);
				removeFromChart(model);
			}
		}
		
		// Add new selections to the chart.
		for (ProbableCauseModelCollection model : items)
		{
			if (model.getDisplay() == false)
			{
				model.setDisplay(true);
				addToChart(model);
			}
		}
		
	}
	
	
	/**
	 * Adds the probable cause from the specified ProbableCauseModelCollection
	 * to the causality chart.
	 * @param collection aggregated collection containing the probable cause to 
	 * 		add to the chart.
	 */
	protected void addToChart(ProbableCauseModelCollection collection)
	{
		GWT.log("Add to chart: " + collection.getDescription());
		
		if (collection.getDataSourceCategory() == DataSourceCategory.NOTIFICATION)
		{
			m_ChartWidget.addNotifications(collection);
			collection.set(ChartSymbolCellRenderer.DEFAULT_SHAPE_PROPERTY, Shape.DIAMOND);
		}
		else if (collection.getDataSourceCategory() == DataSourceCategory.TIME_SERIES)
		{	
			List<ProbableCauseModel> probCauses = collection.getProbableCauses();
			boolean oneShown = false;
			for (ProbableCauseModel probCause : probCauses)
			{
				if (probCause.getDisplay() == true)
				{
					oneShown = true;
					m_ChartWidget.addTimeSeries(collection, probCause);
				}
			}	
			
			// Display the first probable cause if nothing else is selected.
			if (oneShown == false)
			{
				ProbableCauseModel probCause = collection.getProbableCause(0);
				CheckBox cb = (CheckBox)(ComponentManager.get().get(m_CheckboxIdPrefix + probCause.getEvidenceId()));
				if (cb != null)
				{
					cb.setValue(true);
				}
				
			}
		}
		
		// Set the symbolColor property to display the appropriate line colour in the key.
    	String hexColor = m_ChartWidget.getLineColour(collection);
    	if (hexColor != null)
    	{
    		String colorName = CSSColorChart.getInstance().getColorName(hexColor);
    		collection.set(ChartSymbolCellRenderer.DEFAULT_COLOR_PROPERTY, colorName);
    	}
		m_Grid.getStore().update(collection);
	}
	
	
	/**
	 * Removes the specified collection of probable causes from the chart.
	 * @param collection ProbableCauseModelCollection to remove from the chart.
	 */
	protected void removeFromChart(ProbableCauseModelCollection collection)
	{
		GWT.log("Remove from chart: " + collection.getDescription());
		
		if (collection.getDataSourceCategory() == DataSourceCategory.NOTIFICATION)
		{
			m_ChartWidget.removeNotifications(collection);
		}
		else if (collection.getDataSourceCategory() == DataSourceCategory.TIME_SERIES)
		{
			List<ProbableCauseModel> probCauses = collection.getProbableCauses();
			for (ProbableCauseModel probCause : probCauses)
			{
				if (probCause.getDisplay() == true)
				{
					m_ChartWidget.removeTimeSeries(collection, probCause);
				}
			}
		}
	}
	
	
	/**
	 * Overrides setWidth(int) to set the width of the chart and grid to the 
	 * specified size. This method fires the <i>Resize</i> event.
	 * @param width the new width to set.
	 */
	@Override
    public void setWidth(int width)
	{	
		super.setWidth(width);

		m_ChartContainer.setWidth(width);
		m_ChartWidget.setChartWidth(width);
		m_Grid.setWidth(width);
	}
	
	
	/**
	 * Overrides setSize(int, int) to set the size of the chart and grid according
	 * to the specified dimensions. This method fires the <i>Resize</i> event.
	 * @param width the new width to set.
	 * @param height the new height to set.
     */
    @Override
    public void setSize(int width, int height)
    {
	    super.setSize(width, height);
	    sizeChartToFit();
    }
    
    
    /**
     * Exports the data currently shown in the Causality View to CSV file format.
     */
    protected void exportToCSV()
    {
    	DateTimeFormat format = DateTimeFormat.getFormat("vvvv");
		String timeZoneId  = format.format(m_Model.getTime(TimeFrame.SECOND));
		GWT.log("CSV item - export time zone is " + timeZoneId);
        
        String url = SyncServiceLocator.getCausalityDataExportURL(
        		"CSV", m_Model.getId(), ClientUtil.CAUSALITY_METRICS_TIME_SPAN,
        		m_ChartWidget.getStartTime(), m_ChartWidget.getEndTime(), timeZoneId);
        
        m_ExportForm.setAction(url);
        m_ExportForm.submit();


     //   DOM.setElementAttribute(RootPanel.get("__download").getElement(), 
     //   		"src", url); 

        
     //   Window.open(url, "prlExport", "toolbar=no, location=no, directories=no," +
     //   	"status=yes, menubar=no, scrollbars=no, width=420, height=320");
        
    }
    
    
    /**
     * Exports the data currently shown in the Causality View to CSV file format.
     */
    protected void exportToPDF()
    {
    	DateTimeFormat format = DateTimeFormat.getFormat("vvvv");
		String timeZoneId  = format.format(m_Model.getTime(TimeFrame.SECOND));
        
        String url = SyncServiceLocator.getCausalityDataExportURL(
        		"PDF", m_Model.getId(), ClientUtil.CAUSALITY_METRICS_TIME_SPAN,
        		m_ChartWidget.getStartTime(), m_ChartWidget.getEndTime(), timeZoneId);
        
    	
    	// Pass the list of ids of notifications and time series to show 
        // via temporary  hidden fields in the POSTed Form 
        // (max HTTP GET URL is approx 2000 characters on IE).
    	List<ProbableCauseModelCollection> selectedList = m_CheckboxSm.getSelectedItems();
    	List<ProbableCauseModel> probCauses;
    	for (ProbableCauseModelCollection collection : selectedList)
    	{
    		DataSourceCategory category = collection.getDataSourceCategory();
			switch (category)
			{
				case NOTIFICATION:
					// Just pass the first notification id.
					m_ExportFormWidgetPanel.add(new Hidden("show", 
							"" + collection.getProbableCause(0).getEvidenceId()));
					break;
					
				case TIME_SERIES:
					probCauses = collection.getProbableCauses();
		    		for (ProbableCauseModel model : probCauses)
		    		{
		    			if (model.getDisplay() == true)
		    			{
		    				m_ExportFormWidgetPanel.add(
		    						new Hidden("show", "" + model.getEvidenceId()));
		    			}
		    		}
					
					break;
			}

    	}
        
        // Set the value of the hidden field for the chart title.
        m_ExportTitleField.setValue(m_ChartContainer.getHeading());
        m_ExportForm.setAction(url);
        
        m_ExportForm.submit();  
        
        // Remove all the temporary hidden 'show' fields.
        m_ExportFormWidgetPanel.clear();
        m_ExportFormWidgetPanel.add(m_ExportTitleField);
    }
    
    
    /**
     * Sizes the chart widget to fill the available space in its container.
     */
    protected void sizeChartToFit()
    {
    	// For height, subtract height for the toolbar.
    	int chartHeight = m_ChartContainer.getInnerHeight() - 25;
    	m_ChartWidget.setChartSize(m_ChartContainer.getWidth(), chartHeight);
    }

    
    /**
     * Fires an OpenNotificationViewClick event for the specified notification type
     * causality data.
     * @param causalityData notification data for which to fire an event.
     */
    protected void fireOpenNotificationViewClick(CausalityDataModel causalityData)
    {
    	// Need to obtain the evidence ID for the latest notification in the set.
    	final CausalityDataModel notificationData = causalityData;
    	
    	ApplicationResponseHandler<Integer> callback = 
			new ApplicationResponseHandler<Integer>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log(ClientUtil.CLIENT_CONSTANTS.errorLoadingEvidenceData() + ": ", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						ClientUtil.CLIENT_CONSTANTS.errorLoadingEvidenceData(), null);
			}


			@Override
            public void uponSuccess(Integer evidenceId)
			{	
				EvidenceModel notification = new EvidenceModel();
				notification.setId(evidenceId);
				
				RequestViewEvent<EvidenceModel> rve = 
					new RequestViewEvent<EvidenceModel>(m_ChartWidget.getChartWidget());
				rve.setViewToOpenDataType(notificationData.getDataSourceType());
				rve.setSourceName(notificationData.getSource());	
				rve.setModel(notification);	
				fireEvent(GXTEvents.OpenNotificationViewClick, rve);
			}
		};
    	
    	m_CausalityQueryService.getLatestEvidenceId(m_Model.getId(), 
    			causalityData.getDataSourceName(), causalityData.getDescription(), 
    			causalityData.getSource(), causalityData.getAttributes(), callback);
    }
    

	/**
	 * Called when the user clicks on probable cause selector in the grid.
	 * @param probCauseCollectionId id of the probable cause collection containing
	 * 	the probable cause that has been checked.
	 * @param evidenceId evidence id of the probable cause that has been checked.
	 */
	protected void onTimeSeriesChecked(ProbableCauseModelCollection collection,
			ProbableCauseModel probCause)
	{
		List<ProbableCauseModelCollection> selectedList = m_CheckboxSm.getSelectedItems();
		if (selectedList.contains(collection))
		{
			if (probCause.getDisplay() == true)
			{ 
				m_ChartWidget.addTimeSeries(collection, probCause);
			}
			else
			{
				m_ChartWidget.removeTimeSeries(collection, probCause);
				
				// If no other probable causes in the aggregation are displayed,
				// deselect the ProbableCauseModelCollection checkbox.
				boolean oneShown = false;
				List<ProbableCauseModel> probCauses = collection.getProbableCauses();
				for (ProbableCauseModel probableCause : probCauses)
				{
					if (probableCause.getDisplay() == true)
					{
						oneShown = true;
						break;
					}
				}
				if (oneShown == false)
				{
					m_CheckboxSm.setFiresEvents(false);
					m_CheckboxSm.deselect(collection);
					collection.setDisplay(false);
					m_CheckboxSm.setFiresEvents(true);
				}
			}
		}
		
	}
	
	
	/**
	 * Custom GridCellRenderer used to render the list of aggregated 
	 * ProbableCauseCollection objects in an expanded row in the key for the
	 * Causality Chart.
	 */
	class ProbableCauseCollectionRenderer implements GridCellRenderer<ProbableCauseModelCollection>
	{
		/**
		 * Returns the HTML to be used in a grid cell displaying a collection of
		 * probable cause model objects.
		 * @return  the HTML to be used in a grid cell. 
		 */
		@Override
        public Object render(ProbableCauseModelCollection collection,
                String property, ColumnData config, int rowIndex, int colIndex,
                ListStore<ProbableCauseModelCollection> store,
                Grid<ProbableCauseModelCollection> grid)
        {	
			if (collection.getDataSourceCategory() == DataSourceCategory.TIME_SERIES)
			{			
				return renderTimeSeries(collection);
			}
			else
			{
				return "";
			}
        }
		
		
		/**
		 * Returns the HTML (source name and id link) to be used in cells displaying 
		 * a collection of notification probable causes.
		 */
        protected Object renderNotifications(ProbableCauseModelCollection collection)
        {
        	VerticalPanel vPanel = new VerticalPanel();
        	
        	List<ProbableCauseModel> probCauses = collection.getProbableCauses();
        	
        	HorizontalPanel hp;
        	Label notificationLabel;
    		String sourceText;
    		String idText;
    		
        	for (ProbableCauseModel probCause : probCauses)
        	{
        		hp = new HorizontalPanel();
    			hp.setTableWidth("100%");
    			
    			final int evidenceId = probCause.getEvidenceId();
        		sourceText = ClientUtil.CLIENT_CONSTANTS.source() + 
					"=" + probCause.getSource();
        		
        		// Add a label for the source name.
	        	notificationLabel = new Label(sourceText);
				notificationLabel.addStyleName("prl-causalityList-option");
				TableData td = new TableData();
				td.setWidth("150px");
				td.setStyleName("prl-causalityList-option");
				hp.add(notificationLabel, td);
				
				// Add a link for the evidence id to open up the EvidenceAttributesDialog.
				idText = ClientUtil.CLIENT_CONSTANTS.id() + "=" + evidenceId;
				
				Anchor idLink = new Anchor(idText, true);
				idLink.setStyleName("prl-textLink");
				idLink.addClickHandler(new ClickHandler(){
					
					@Override
		            public void onClick(ClickEvent event)
		            {
						AttributeListDialog dialog = AttributeListDialog.getInstance();
						dialog.showEvidenceAttributes(evidenceId);
		            }
		        	
		        });
				
				hp.add(idLink);
				
				vPanel.add(hp);
        	}
			
			return vPanel;
        }
        
        
        /**
		 * Returns the HTML (radio button control with source name and attribute label) 
		 * to be used in cells displaying a collection of time series probable causes.
		 */
        protected Object renderTimeSeries(ProbableCauseModelCollection collection)
        {	
        	VerticalPanel vPanel = new VerticalPanel();
        	
        	final ProbableCauseModelCollection probCauseCollection = collection;
        	List<ProbableCauseModel> probCauses = collection.getProbableCauses();
        	 
        	HorizontalPanel hp;
        	CheckBox timeSeriesCheck = null;
    		String sourceText;
    		String timeSeriesAttributeLabel;
    		String magnitudeText;
    		Label magnitudeLabel;
    		double magnitude;
    		Label attributesLabel;
    		int counter = 0;
    		
        	for (ProbableCauseModel probCause : probCauses)
        	{
        		final ProbableCauseModel probableCause = probCause;
        		
        		hp = new HorizontalPanel();
    			hp.setTableWidth("100%");
        	
	        	// Add a CheckBox selector for time series.
				// Don't use the box label as the style can only be set by
				// overriding the GXT x-form-cb-label style class.
    			timeSeriesCheck = new CheckBox();  
    			timeSeriesCheck.setId(m_CheckboxIdPrefix + probCause.getEvidenceId());
				if (probCause.getDisplay() == true)
				{
					timeSeriesCheck.setValue(true);  
				}
				
				timeSeriesCheck.addListener(Events.Change, new Listener<FieldEvent>(){
	
					@Override
	                public void handleEvent(FieldEvent be)
	                {
						Boolean selectorVal = (Boolean)(be.getValue());
						probableCause.setDisplay(selectorVal.booleanValue());
						onTimeSeriesChecked(probCauseCollection, probableCause);
	                }
					
				}); 
		    
				TableData selectortd = new TableData();
				selectortd.setWidth("20px");
				selectortd.setVerticalAlign(VerticalAlignment.MIDDLE);
				hp.add(timeSeriesCheck, selectortd);
				
				// Add the source name label.
				sourceText = ClientUtil.CLIENT_CONSTANTS.source() +  "=" + 
					probCause.getSource();
				Label sourceLabel = new Label(sourceText);
				sourceLabel.addStyleName("prl-causalityList-option");
				if (counter == 0)
				{
					sourceLabel.addStyleName("prl-causalityList-option-first");
				}
				TableData sourcetd = new TableData();
				sourcetd.setWidth("120px");
				sourcetd.setVerticalAlign(VerticalAlignment.MIDDLE);
				hp.add(sourceLabel, sourcetd);
				
				// Add a label for the attributes, if any.
				timeSeriesAttributeLabel = probCause.getAttributeLabel();
				if (timeSeriesAttributeLabel != null)
				{
					attributesLabel = new Label(timeSeriesAttributeLabel);
					attributesLabel.addStyleName("prl-causalityList-option");
					if (counter == 0)
					{
						attributesLabel.addStyleName("prl-causalityList-option-first");
					}
					
					TableData attributetd = new TableData();
					attributetd.setWidth("200px");
					attributetd.setVerticalAlign(VerticalAlignment.MIDDLE);	
					hp.add(attributesLabel, attributetd);
				}
				
				// Add the magnitude label, with appropriate number of decimal places.
				magnitude = probCause.getMagnitude();
				magnitudeText = ClientUtil.CLIENT_CONSTANTS.magnitude() + "=";
				if (magnitude >= 1)
				{
					magnitudeText += ClientUtil.getShortDecimalFormat().format(magnitude);
				}
				else
				{
					magnitudeText += NumberFormat.getDecimalFormat().format(magnitude);
				}
				
				magnitudeLabel = new Label(magnitudeText);
				magnitudeLabel.addStyleName("prl-causalityList-option");
				if (counter == 0)
				{
					magnitudeLabel.addStyleName("prl-causalityList-option-first");
				}
				
				TableData magnitudetd = new TableData();
				magnitudetd.setVerticalAlign(VerticalAlignment.MIDDLE);
				hp.add(magnitudeLabel, magnitudetd);
				
				vPanel.add(hp);
				counter++;
        	}
        	
        	if (collection.getSize() == 1)
        	{
        		timeSeriesCheck.disable();
        	}
        	
        	return vPanel;
        }
		
	}
	
}
