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

import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.data.ListLoadResult;
import com.extjs.gxt.ui.client.data.ListLoader;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.BorderLayoutEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.LayoutEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.event.SplitBarEvent;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.store.StoreEvent;
import com.extjs.gxt.ui.client.store.StoreListener;
import com.extjs.gxt.ui.client.store.TreeStore;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.CollapsePanel;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.SplitBar;
import com.extjs.gxt.ui.client.widget.WidgetComponent;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.HiddenField;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
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
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent; 
import com.google.gwt.user.client.ui.Hidden;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.CSSColorChart;
import com.prelert.client.ClientUtil;
import com.prelert.client.chart.CausalityChartWidget;
import com.prelert.client.chart.CausalityGChartWidget;
import com.prelert.client.chart.ChartDataGrid;
import com.prelert.client.chart.ChartSymbolCellRenderer;
import com.prelert.client.chart.ChartToolBar;
import com.prelert.client.chart.IncidentGChart;
import com.prelert.client.event.ChartWidgetEvent;
import com.prelert.client.event.ChartWidgetListener;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.RequestViewEvent;
import com.prelert.client.incident.AnalysisSummaryTreeWidget;
import com.prelert.data.CausalityView;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.TimeFrame;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.data.gxt.ActivityTreeModel;
import com.prelert.data.gxt.CausalityDataModel;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.data.gxt.IncidentModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.CausalityQueryServiceAsync;


/**
 * Ext GWT (GXT) widget for exploring causality data. The widget consists of
 * a chart displaying time series and notification data, a grid summarising the
 * causality data by attribute, and a grid for listing and selecting the time
 * series features and notifications in each summary grouping.
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>OpenActivityViewClick</b> : RequestViewEvent&lt;EvidenceModel&gt;<br>
 * <div>Fires after a link to the Activity View is selected.</div>
 * <ul>
 * <li>component : this</li>
 * <li>model: notification or feature whose analysis is being requested</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>OpenNotificationViewClick</b> : RequestViewEvent<br>
 * <div>Fires after a link to a notification view is selected.</div>
 * <ul>
 * <li>component : this</li>
 * <li>model: EvidenceModel of notification to show</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>OpenTimeSeriesViewClick</b> : RequestViewEvent<br>
 * <div>Fires after a link to a time series view is selected.</div>
 * <ul>
 * <li>component : this</li>
 * <li>model: TimeSeriesConfig of time series to show</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>ShowActivitySelect</b> : ButtonEvent(button)<br>
 * <div>Fires after the button to open the Activity lookup was pressed.</div>
 * <ul>
 * <li>button : Activity Select button</li>
 * </ul>
 * </dd>
 * 
 * </dl>
 * 
 * @author Pete Harverson
 */
public class CausalityExplorerWidget extends LayoutContainer
{
	private CausalityQueryServiceAsync 	m_CausalityQueryService;
	
	private EvidenceModel				m_Model;		// The item of evidence or time series feature.
	
	private ContentPanel				m_ChartContainer;
	private Anchor						m_TitleField;
	private ToolBar						m_ChartToolBar;
	private CausalityChartWidget		m_ChartWidget;
	
	private AnalysisSummaryTreeWidget 	m_SummaryWidget;

	private FormPanel					m_ExportForm;
	private FlowPanel					m_ExportFormWidgetPanel;
	private HiddenField<String>			m_ExportTitleField;

	
	/**
	 * Creates a new widget for exploring causality data.
	 */
	public CausalityExplorerWidget()
	{	
		m_CausalityQueryService = AsyncServiceLocator.getInstance().getCausalityQueryService();
		
		initComponents();
		
		setEnabledDisplays(false);
	}
	
	
	/**
	 * Creates and initialises the graphical components in the widget.
	 */
	protected void initComponents()
	{
		BorderLayout containerLayout = new BorderLayout();   
		containerLayout.setContainerStyle("prl-viewport");
	    setLayout(containerLayout); 
	    
	    m_ChartContainer = createChartComponents();
	    
	    // Create the widget holding the summary tree and the chart selection grid..
	    m_SummaryWidget = createSummaryWidget();
	    
	    // Add the header, chart, and summary widget to the container.
	    BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);   
	    centerData.setMargins(new Margins(0));
	    
	    BorderLayoutData southData = new BorderLayoutData(LayoutRegion.SOUTH, 280, 150, 450);   
	    southData.setSplit(true);
	    southData.setCollapsible(true); 
	    southData.setFloatable(false);  
	    southData.setMargins(new Margins(5, 0, 0, 0)); 
	    
	    BorderLayoutData northData = new BorderLayoutData(LayoutRegion.NORTH, 27);   
	    northData.setFloatable(false);  
	    
	    add(createHeaderToolBar(), northData);
        add(m_ChartContainer, centerData);   
        add(m_SummaryWidget, southData); 
        
        
        // Add listeners to the layout to resize the chart on splitbar drag, 
        // collapse and expand events.
        final Listener<SplitBarEvent> splitBarListener = new Listener<SplitBarEvent>()
		{
			@Override
            public void handleEvent(SplitBarEvent sbe)
			{
				if (sbe.getSize() < 1)
				{
					return;
				}

				sizeChartToFit();
			}
		}; 
        
		containerLayout.addListener(Events.AfterLayout, new Listener<LayoutEvent>(){

			@Override
            public void handleEvent(LayoutEvent be)
            {	
				SplitBar splitBar = m_SummaryWidget.getData("splitBar");
		        if (splitBar != null)
		        {
		        	splitBar.addListener(Events.DragEnd, splitBarListener);
		        	CausalityExplorerWidget.this.removeListener(Events.AfterLayout, this);
		        }
            }
			
		});
        
        containerLayout.addListener(Events.Collapse, new Listener<BorderLayoutEvent>(){
        	 
 			@Override
             public void handleEvent(BorderLayoutEvent be)
             {
 				sizeChartToFit();
 				
 				SplitBar splitBar = m_SummaryWidget.getData("splitBar");
 	            if (splitBar != null)
 	            {
 	            	splitBar.removeListener(Events.DragEnd, splitBarListener);
 	            }  
             }
 			
 		});
 		
      	containerLayout.addListener(Events.Expand, new Listener<BorderLayoutEvent>(){
 
 			@Override
             public void handleEvent(BorderLayoutEvent be)
             {
 				sizeChartToFit();
 				
 				SplitBar splitBar = m_SummaryWidget.getData("splitBar");
 	            if (splitBar != null)
 	            {
 	            	splitBar.addListener(Events.DragEnd, splitBarListener);
 	            }  
 	            
 	            // Refresh the selection grid which re-renders the paging toolbar
 				// which may not render properly on expand in Safari and Chrome.
 	            m_SummaryWidget.getTree().getView().refresh(true);
 	            m_SummaryWidget.getSelectionGrid().getView().refresh(true);
 	            m_SummaryWidget.loadSelectionGrid();
             }
 			
 		});
	}
	
	
	/**
	 * Creates the header toolbar, with title field and 'Select activity' button.
	 * @return the header toolbar.
	 */
	protected ToolBar createHeaderToolBar()
	{
		final ToolBar headerToolBar = new ToolBar();
        headerToolBar.setEnableOverflow(false);
        headerToolBar.setStyleAttribute("border-bottom", "none");
        headerToolBar.setBorders(true);
        
        final Button searchBtn = new Button();  
    	searchBtn.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.toolbar_search()));
        searchBtn.setToolTip(ClientUtil.CLIENT_CONSTANTS.activitySelect());
        searchBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
			{
				fireEvent(GXTEvents.ShowActivitySelect, new ButtonEvent(searchBtn));
			}
		});
        
        // Create the title field as a link to show the activity in the Activity View.
        m_TitleField = new Anchor(ClientUtil.CLIENT_CONSTANTS.activitySelect(), true);
        m_TitleField.setStyleName("prl-textLink");
        m_TitleField.addClickHandler(new ClickHandler(){
			
			@Override
            public void onClick(ClickEvent event)
            {
				if (m_Model != null)
				{
					RequestViewEvent<EvidenceModel> rve = 
						new RequestViewEvent<EvidenceModel>(CausalityExplorerWidget.this);
					rve.setModel(m_Model);
					fireEvent(GXTEvents.OpenActivityViewClick, rve);
				}
				else
				{
					fireEvent(GXTEvents.ShowActivitySelect, new ButtonEvent(searchBtn));
				}
            }
        });
        
        final LayoutContainer titleFieldContainer = new LayoutContainer();
        titleFieldContainer.add(m_TitleField);
        titleFieldContainer.setBorders(true);
        titleFieldContainer.addStyleName("prl-title-label");
        
        final LabelToolItem analysisForLabel = new LabelToolItem(ClientUtil.CLIENT_CONSTANTS.fieldAnalysisFor());
        headerToolBar.add(analysisForLabel);
        headerToolBar.add(searchBtn); 
        headerToolBar.add(titleFieldContainer);
        
        // Auto resize title text field to fill available width.
        Listener<ComponentEvent> resizeListener = new Listener<ComponentEvent>(){
			
			@Override
            public void handleEvent(ComponentEvent be)
            {
				titleFieldContainer.setWidth(headerToolBar.getWidth() - searchBtn.getWidth() -
						analysisForLabel.getWidth() - 18);
            }
		};
		addListener(Events.Resize, resizeListener);

		return headerToolBar;
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
        chartContainer.setHeaderVisible(false);
        
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
		
		m_ChartWidget.addListener(Events.Remove, new ChartWidgetListener<CausalityDataModel>(){

            @Override
            public void chartRemove(ChartWidgetEvent<CausalityDataModel> e)
            {
            	// Deselect the item from the grid.
            	CausalityDataModel causalityData = e.getModel();
            	ChartDataGrid<CausalityDataModel> selectionGrid = m_SummaryWidget.getSelectionGrid();
            	selectionGrid.deselect(causalityData, false);
            }
			
		});
        
        // Create the chart toolbar.
        m_ChartToolBar = createChartToolBar();
        
        LoadListener loadListener = new LoadListener()
		{
			@Override
            public void loaderBeforeLoad(LoadEvent le)
			{
				m_ChartContainer.mask(GXT.MESSAGES.loadMask_msg());
				m_ChartToolBar.setEnabled(false);
			}


			@Override
            public void loaderLoad(LoadEvent le)
			{
				m_ChartContainer.unmask();
				m_ChartToolBar.setEnabled(true);
			}


			@Override
            public void loaderLoadException(LoadEvent le)
			{
				m_ChartContainer.unmask();
				m_ChartToolBar.setEnabled(true);
			}
		};
		m_ChartWidget.addLoadListener(loadListener);
             
        
        chartContainer.add(m_ChartToolBar);
        chartContainer.add(m_ChartWidget.getChartWidget());
     	
     	return chartContainer;
	}
    
    
    /**
     * Creates the toolbar for the chart component.
     */
    protected ToolBar createChartToolBar()
    {
    	ChartToolBar<TimeSeriesDataPoint> chartTools = 
	    	new ChartToolBar<TimeSeriesDataPoint>(m_ChartWidget, false);
        
        // Create a Clear button.
        Button clearBtn = new Button(ClientUtil.CLIENT_CONSTANTS.clear(),
        		AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.toolbar_chart_clear()));
        clearBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent be)
            {
				// Clear chart, firing events which update the selection grid.
				m_ChartWidget.removeAll();
				m_ChartWidget.setTimeMarker(m_Model.getTime(TimeFrame.SECOND));
            }
		});
 
        
        // Create an Export button with CSV and PDF options.
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
		
		MenuItem linkItem = new MenuItem(ClientUtil.CLIENT_CONSTANTS.exportTo(
				ClientUtil.CLIENT_CONSTANTS.urlLink()));
		linkItem.addSelectionListener(new SelectionListener<MenuEvent>()
		{
			@Override
            public void componentSelected(MenuEvent ce)
            {
				fireEvent(GXTEvents.ShowShareLinkClick, ce);
            }
		});
		exportMenu.add(linkItem);
		
		// Create a form panel to submit the export request,
		// with some hidden fields for passing chart parameters.
    	m_ExportForm = new FormPanel();
    	m_ExportForm.setMethod(FormPanel.METHOD_POST);
    	m_ExportForm.setEncoding(FormPanel.ENCODING_URLENCODED);
    	m_ExportForm.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler()
        {
	        @Override
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
		
		// Insert clear, export buttons at the start.
		chartTools.insert(clearBtn, 0);
		chartTools.insert(exportBtn, 1);
		chartTools.insert(new WidgetComponent(m_ExportForm), 2);
		chartTools.insert(new FillToolItem(), 3);

		// Add a Scale to Fit button.
		Button scaleBtn = new Button();
		scaleBtn.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.toolbar_scale_to_fit()));
		scaleBtn.setToolTip(ClientUtil.CLIENT_CONSTANTS.scaleToFit());
		scaleBtn.setMouseEvents(false);		// Disable mouse events for plain white toolbar.
		scaleBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
			{
				m_ChartWidget.scaleToFit();
			}
		});
		chartTools.add(scaleBtn);

		return chartTools;
    }
    
    
    protected AnalysisSummaryTreeWidget createSummaryWidget()
    {
    	AnalysisSummaryTreeWidget summaryTreeWidget = new AnalysisSummaryTreeWidget();
    	
    	// Listen for load events on the selection grid, to mark selected any
	    // rows which are displayed in the chart.
	    final ChartDataGrid<CausalityDataModel> selectionGrid = 
	    	summaryTreeWidget.getSelectionGrid();
	    final ListStore<CausalityDataModel> selectionStore = selectionGrid.getStore();
	    final GridSelectionModel<CausalityDataModel> selectionModel = 
	    	selectionGrid.getSelectionModel();
	    
	    
	    ListLoader<? extends ListLoadResult<CausalityDataModel>> loader = 
	    	selectionStore.getLoader();
	    loader.addLoadListener(new LoadListener(){

			@Override
            public void loaderLoad(LoadEvent le)
            {
				// Mark as selected any rows which are displayed in the chart.	
            	selectionModel.setFiresEvents(false);
            	
            	List<CausalityDataModel> allItems = selectionStore.getModels();
            	for (CausalityDataModel causalityData : allItems)
            	{
            		if (m_ChartWidget.isDisplayedOnChart(causalityData) == true)
	    	    	{
            			selectionModel.select(causalityData, true);
	    	    		String hexColor = m_ChartWidget.getLineColour(causalityData);
	    	    		String colorName = CSSColorChart.getInstance().getColorName(hexColor);
	    	    		causalityData.set(ChartSymbolCellRenderer.DEFAULT_COLOR_PROPERTY, colorName);
	    	    		causalityData.set(ChartSymbolCellRenderer.DEFAULT_SHAPE_PROPERTY, 
		    	    			m_ChartWidget.getSymbolShape(causalityData));
	    	    		selectionStore.update(causalityData);
	    	    	}
            	}
            	selectionModel.setFiresEvents(true);
            }
	    	
	    });
	    
	    selectionModel.addSelectionChangedListener(
     			new SelectionChangedListener<CausalityDataModel>(){

			@Override
            public void selectionChanged(SelectionChangedEvent<CausalityDataModel> se)
            {
				processChartDataSelections(se.getSelection());
            }
     	});
	    
	    return summaryTreeWidget;
    }
    
    
    /**
     * Loads causality data into the summary grid and the chart for the specified 
     * notification or time series feature.
     * <p>The following evidence properties are used:
     * <ul>
     * <li>id - to load the causality data</li>
     * <li>time - to set the time marker and window of data loaded</li>
     * <li>description - to set the widget title</li>
     * </ul>
     * @param model the item of evidence whose causality data is to be displayed.
     * @param summaryGroupBy the name of the attribute by which data in the summary
	 * 		grid should be aggregated. If <code>null</code> the default setting 
	 * 		will be used. 
     */
	public void loadForEvidence(EvidenceModel model, String summaryGroupBy)
	{
		loadForEvidence(model, null, summaryGroupBy);
	}
	
	
	/**
     * Loads causality data into the summary grid and the chart for the specified 
     * notification or time series feature, supplying pre-built configuration data.
     * <p>The following evidence properties are used:
     * <ul>
     * <li>id - to load the causality data</li>
     * <li>time - to set the time marker and window of data loaded</li>
     * <li>description - to set the widget title</li>
     * </ul>
     * @param model the item of evidence whose causality data is to be displayed.
     * @param causalityView configuration data used by the chart and grids.
     * @param summaryGroupBy the name of the attribute by which data in the summary
	 * 		grid should be aggregated. If <code>null</code> the default setting 
	 * 		will be used. 
     */
	public void loadForEvidence(EvidenceModel model, CausalityView causalityView, 
			String summaryGroupBy)
	{
		m_Model = model;
		
		m_ChartWidget.removeAll();
		
		// Centre the causality chart in a fifteen minute window 
		// around the feature / notification.
		Date modelTime = m_Model.getTime(TimeFrame.SECOND);	
		m_ChartWidget.setDateRange(
				new Date(modelTime.getTime() - (ClientUtil.CAUSALITY_METRICS_TIME_SPAN*1000)/2), 
				new Date(modelTime.getTime() + (ClientUtil.CAUSALITY_METRICS_TIME_SPAN*1000)/2));
		m_ChartWidget.setTimeMarker(modelTime);
		
		// Update the title of the grids container, AND in the corresponding CollapsePanel.
		String formattedTime = ClientUtil.formatTimeField(modelTime, TimeFrame.SECOND);
		String analysisHeading = ClientUtil.CLIENT_CONSTANTS.analysisDataHeading(formattedTime);
		
		m_SummaryWidget.setHeading(analysisHeading);
		CollapsePanel cp = (CollapsePanel)(m_SummaryWidget.getData("collapse"));
		if (cp != null)
		{
			cp.el().selectNode(".x-panel-header-text").update(analysisHeading);
		}
		
		m_SummaryWidget.setEnabledDisplays(false, GXT.MESSAGES.loadMask_msg());
		m_ChartContainer.mask(GXT.MESSAGES.loadMask_msg());
		
		if (causalityView == null)
		{
			// Update the title field with the description, and a transparent 
			// holder for the anomaly score image whilst the full config loads.
			String anchorText = "<img class=\"prl-anomaly\" src=\"images/shared/chart_boxes_transp.png\" " +
				"alt=\"" + ClientUtil.CLIENT_CONSTANTS.showInActivityHeatMap() + 
				"\" />&nbsp;" + m_Model.getDescription();
			m_TitleField.setHTML(anchorText);
			
			final String groupBy = summaryGroupBy;
			
			// Load configuration data (attributes, peak values).
			ApplicationResponseHandler<CausalityView> callback = 
				new ApplicationResponseHandler<CausalityView>()
			{
				@Override
	            public void uponFailure(Throwable caught)
				{
					GWT.log(ClientUtil.CLIENT_CONSTANTS.errorLoadingAnalysisData() + ": ", caught);
					MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
							ClientUtil.CLIENT_CONSTANTS.errorLoadingAnalysisData(), null);
					
					m_SummaryWidget.setEnabledDisplays(true, null);
					m_ChartContainer.unmask();
				}


				@Override
	            public void uponSuccess(CausalityView causalityView)
				{	
					GWT.log("CausalityExplorerWidget - loaded CausalityView: " + causalityView);
					reconfigureView(causalityView, groupBy);
				}
			};

			m_CausalityQueryService.getViewConfiguration(m_Model.getId(), 
					ClientUtil.CAUSALITY_METRICS_TIME_SPAN, callback);
		}
		else
		{
			reconfigureView(causalityView, summaryGroupBy);
		}
	}
	
	
	/**
	 * Reconfigures the causality chart, summary grid, and selection grid for the 
	 * current item of evidence with the supplied configuration data.
	 * The summary grid is then populated and the top items of causality data are 
	 * automatically added to the chart.
	 * @param causalityView configuration data used by the chart and grids.
	 * @param analyzeBy the name of the attribute by which data in the summary
	 * 		grid should be analyzed. If <code>null</code> the default setting 
	 * 		will be used. 
	 */
	protected void reconfigureView(CausalityView causalityView, String analyzeBy)
	{
		// Update the title field with the indicator of anomaly score.
		int anomalyScore = causalityView.getActivityAnomalyScore();
		String symbolURL = IncidentGChart.getSymbolImageURL(anomalyScore);
		String anchorText = "<img class=\"prl-anomaly\" src=\"" + symbolURL + "\" " +
			"alt=\"" + ClientUtil.CLIENT_CONSTANTS.showInActivityHeatMap() + 
			"\" />&nbsp;" + m_Model.getDescription();
		m_TitleField.setHTML(anchorText);
		m_TitleField.setEnabled(true);
		m_TitleField.setTitle(ClientUtil.CLIENT_CONSTANTS.showInActivityHeatMap());
		
		m_ChartWidget.setPeakValuesByTypeId(causalityView.getPeakValuesByTypeId());
		
		// Load the summary tree widget, and add default items to chart once loaded.
		IncidentModel incident = new IncidentModel();
		incident.setEvidenceId(m_Model.getId());
		incident.setTime(m_Model.getTime(TimeFrame.SECOND));
		incident.setDescription(m_Model.getDescription());
		
		final TreeStore<ActivityTreeModel> treeStore = 
			m_SummaryWidget.getTree().getTreeStore();
		treeStore.addStoreListener(new StoreListener<ActivityTreeModel>(){

            @Override
            public void storeDataChanged(StoreEvent<ActivityTreeModel> se)
            {
            	// Wait for the first branch point to be reached
            	List<CausalityDataModel> topItems = m_SummaryWidget.getTree().getTopCausalityData(
            		ClientUtil.CAUSALITY_MAX_DISPLAY_ITEMS, m_Model.getId());
            	if (topItems.size() > 0)
            	{
            		treeStore.removeStoreListener(this);
                	
            		m_ChartWidget.removeAll();	
            		Date modelTime = m_Model.getTime(TimeFrame.SECOND);	
            		m_ChartWidget.setTimeMarker(modelTime);
            		
        	    	for (CausalityDataModel topCausalityData : topItems)
        	    	{
            			addToChart(topCausalityData);
        	    	}
            	}
            	else
            	{
            		if (treeStore.getModels().size() == 0)
            		{
            			// No causality data - unmask the chart.
                		m_ChartContainer.unmask();
            		}
            	}
            }
            
	    });
		
		m_SummaryWidget.load(analyzeBy, incident, causalityView);
	}
	
	
	/**
     * Returns the notification or time series feature for which causality data
     * is being displayed.
     * @return the item of evidence whose causality data is being displayed.
     */
	public EvidenceModel getEvidence()
	{
		return m_Model;
	}
	
	
	/**
	 * Processes the items that have been selected in the selection grid, adding 
	 * or removing corresponding notifications and time series to the chart.
	 * @param items list of causality data that has been selected in the grid.
	 */
	protected void processChartDataSelections(List<CausalityDataModel> selectedItems)
	{
		// Remove any probable causes from the chart which have been deselected.
		ChartDataGrid<CausalityDataModel> selectionGrid = 
	    	m_SummaryWidget.getSelectionGrid();
		
		List<CausalityDataModel> allItems = selectionGrid.getStore().getModels();
		for (CausalityDataModel model : allItems)
		{
			if (selectedItems.contains(model) == true)
			{
				addToChart(model);
			}
			else
			{
				removeFromChart(model);
			}
		}
	}
	
	
	/**
	 * Adds the specified item of causality data to the chart.
	 * @param causalityData the notification or time series feature to add.
	 */
	protected void addToChart(CausalityDataModel causalityData)
	{
		GWT.log("addToChart() : " + causalityData);
		if (m_ChartWidget.isDisplayedOnChart(causalityData) == false)
		{
			if (causalityData.getDataSourceCategory() == DataSourceCategory.NOTIFICATION
					&& causalityData.getEvidenceId() == 0)
			{
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
						notificationData.setEvidenceId(evidenceId);
						addToChart(notificationData);
					}
				};
		    	
		    	m_CausalityQueryService.getLatestEvidenceId(m_Model.getId(), 
		    			causalityData.getDataSourceName(), causalityData.getDescription(), 
		    			causalityData.getSource(), causalityData.getAttributes(), callback);
			}
			else
			{
				m_ChartWidget.addCausalityData(causalityData);
				
				// Update the symbol column in the chart data grid, if it has loaded yet.
				ChartDataGrid<CausalityDataModel> selectionGrid = m_SummaryWidget.getSelectionGrid();
				ListStore<CausalityDataModel> chartDataGridStore = selectionGrid.getStore();
				if (chartDataGridStore.getCount() > 0)
				{		
					// Set the symbolColor property to display the appropriate line colour in the key.
			    	String hexColor = m_ChartWidget.getLineColour(causalityData);
			    	if (hexColor != null)
			    	{
			    		String colorName = CSSColorChart.getInstance().getColorName(hexColor);
			    		causalityData.set(ChartSymbolCellRenderer.DEFAULT_COLOR_PROPERTY, colorName);
			    	}
			    	
			    	causalityData.set(ChartSymbolCellRenderer.DEFAULT_SHAPE_PROPERTY, 
			    			m_ChartWidget.getSymbolShape(causalityData));
			
			    	selectionGrid.getStore().update(causalityData);
				}
			}
		}
	}
	
	
	/**
	 * Removes the specified item of causality data from the chart.
	 * @param causalityData the notification or time series feature to remove.
	 */
	protected void removeFromChart(CausalityDataModel causalityData)
	{
		if (m_ChartWidget.isDisplayedOnChart(causalityData) == true)
		{
			m_ChartWidget.removeCausalityData(causalityData);
		}
	}
    

	/**
	 * Enables or disables the chart and grid displays in the widget.
	 * @param enableDisplays <code>true</code> to enable the displays,
	 * 	<code>false</code> to disable.
	 */
	public void setEnabledDisplays(boolean enableDisplays)
	{
		if (enableDisplays == false)
		{
			m_ChartContainer.mask();
		}
		else
		{
			m_ChartContainer.unmask();
		}
		
		m_SummaryWidget.setEnabledDisplays(enableDisplays, null);
	}
	
	
	/**
	 * Clears all data out of the causality explorer.
	 */
	public void clearAll()
	{
		m_TitleField.setText(ClientUtil.CLIENT_CONSTANTS.activitySelect());
		m_ChartWidget.removeAll();
		
		m_SummaryWidget.setHeading(ClientUtil.CLIENT_CONSTANTS.analysis());
		m_SummaryWidget.getTree().getTreeStore().removeAll(); // Fires clear of selection grid and path field.
		
		m_Model = null;
	}
	

	/**
	 * Overrides setSize(int, int) to set the size of the components to fill the 
	 * space available. This method fires the <i>Resize</i> event.
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
    	Date modelTime = m_Model.getTime(TimeFrame.SECOND);
    	DateTimeFormat format = DateTimeFormat.getFormat("vvvv");
		String timeZoneId  = format.format(modelTime);
        
        String url = SyncServiceLocator.getCausalityDataExportURL(
        		"PDF", m_Model.getId(), ClientUtil.CAUSALITY_METRICS_TIME_SPAN,
        		m_ChartWidget.getStartTime(), m_ChartWidget.getEndTime(), timeZoneId);
        
        // Pass the list of ids to show and the y axis scaling
        // via temporary  hidden fields in the POSTed Form 
        // (max HTTP GET URL is approx 2000 characters on IE).
        GWT.log("exportToPdf - number on chart: " + m_ChartWidget.getCausalityData().size());
        List<CausalityDataModel> dataOnChart = m_ChartWidget.getCausalityData();
        
        DataSourceCategory category;
        for (CausalityDataModel causalityData : dataOnChart)
        {
        	category = causalityData.getDataSourceCategory();
			switch (category)
			{
				case NOTIFICATION:
					m_ExportFormWidgetPanel.add(new Hidden("showNotifications", "" + 
	        				causalityData.getEvidenceId()));
					break;
					
				case TIME_SERIES_FEATURE:
					m_ExportFormWidgetPanel.add(new Hidden("showSeries", "" + 
	        				causalityData.getTimeSeriesId()));
					GWT.log("exportToPdf showing time series ID " + causalityData.getTimeSeriesId());
					break;
			}
        }
        
        double yAxisScaling = m_ChartWidget.getYAxisScalingFactor();
        if (yAxisScaling != 1.0d)
        {
        	m_ExportFormWidgetPanel.add(new Hidden("yAxisScaling", "" + yAxisScaling));
        }
        
        // Set the value of the hidden field for the chart title.
        String formattedTime = ClientUtil.formatTimeField(modelTime, TimeFrame.SECOND);
        m_ExportTitleField.setValue(ClientUtil.CLIENT_CONSTANTS.analysisFor(
				m_Model.getDescription(), formattedTime));
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
				notification.setDataType(notificationData.getDataSourceName());
				notification.setSource(notificationData.getSource());
				
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
}
