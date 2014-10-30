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
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SplitBarEvent;
import com.extjs.gxt.ui.client.store.GroupingStore;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.store.StoreSorter;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.util.Padding;
import com.extjs.gxt.ui.client.widget.ComponentManager;
import com.extjs.gxt.ui.client.widget.HorizontalPanel;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.SplitBar;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.form.CheckBox;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayoutData;
import com.extjs.gxt.ui.client.widget.layout.TableData;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayoutData;
import com.extjs.gxt.ui.client.widget.layout.BoxLayout.BoxLayoutPack;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout.HBoxLayoutAlign;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout.VBoxLayoutAlign;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Anchor;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.CSSSymbolChart;
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
import com.prelert.client.list.EvidenceAttributesDialog;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.TimeFrame;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.data.Tool;
import com.prelert.data.ViewTool;
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
 * <dd><b>OpenViewClick</b> : RequestViewEvent<br>
 * <div>Fires after a link to a data view is selected in the popup evidence list dialog.</div>
 * <ul>
 * <li>component : this</li>
 * <li>model: notification or time series feature whose data is being requested</li>
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
	private LayoutContainer				m_ChartContainer;
	private CausalityChartWidget		m_ChartWidget;
	
	private boolean 					m_ShowChartLabels = true;
	private LayoutContainer 			m_ChartLabelPanel;
	private Label						m_ChartTitleLabel;
	private Label 						m_ChartSubtitleLabel;
	
	private int							m_TimeSpanSecs = 900;	// Time span of chart - 15 minutes.
	
	private Grid<ProbableCauseModelCollection> 	m_Grid;
	private GridSelectionModel<ProbableCauseModelCollection> m_CheckboxSm;
	private ExtendedRowExpander					m_RowExpander;
	private String								m_CheckboxIdPrefix;
	private Listener<SplitBarEvent>				m_SplitBarListener;
	
	private static CausalityEvidenceDialog 		s_EvidenceDialog;
	
	/** Full height value for the causality chart. */
	public static final double			CHART_VALUE_RANGE = 1000d;
	
	
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
		m_RowExpander = new ExtendedRowExpander(new ProbableCauseCollectionRenderer());
	    
	    ColumnConfig symbolColumn = new ColumnConfig("symbol", 
	    		ClientUtil.CLIENT_CONSTANTS.symbol(), 50);
	    symbolColumn.setRenderer(ChartSymbolCellRenderer.getInstance());
	    
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
						CausalityEvidenceDialog evidenceDialog = getEvidenceDialog();
						evidenceDialog.setProbableCause(probableCause);
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
        		keyData = new BorderLayoutData(LayoutRegion.SOUTH, 300, 150, 450);   
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

				m_ChartWidget.setChartSize(m_ChartContainer.getWidth(), 
			    		m_ChartContainer.getHeight() - 50);
			}
		};
		
		
		// Listen for events to show data selected in the popup evidence dialog.
		Listener<RequestViewEvent<EvidenceModel>> rveListener = 
			new Listener<RequestViewEvent<EvidenceModel>>(){

            public void handleEvent(RequestViewEvent<EvidenceModel> rve)
            {
            	// Propagate the event.
        		fireEvent(rve.getType(), rve);
            }
			
		};
		getEvidenceDialog().addListener(GXTEvents.OpenViewClick, rveListener);
	      
	}


	/**
	 * Creates the graphical components for the chart.
	 * @return LayoutContainer holding the chart component.
	 */
    protected LayoutContainer createChartComponents()
	{
    	final LayoutContainer chartContainer = new LayoutContainer();   
        VBoxLayout layout = new VBoxLayout();    
        layout.setVBoxLayoutAlign(VBoxLayoutAlign.STRETCH);   
        chartContainer.setLayout(layout);  
        chartContainer.setBorders(true);
        
        m_ChartWidget = new CausalityGChartWidget();
        m_ChartWidget.setChartHeight(410);

        
        // Listen for events to open notification and time series views.
        @SuppressWarnings("unchecked")
		Listener<RequestViewEvent> chartListener = new Listener<RequestViewEvent>(){

            public void handleEvent(RequestViewEvent rve)
            {
            	// Propagate the event.
        		fireEvent(rve.getType(), rve);
            }
			
		};
		
		m_ChartWidget.addListener(GXTEvents.OpenNotificationViewClick, chartListener);
		m_ChartWidget.addListener(GXTEvents.OpenTimeSeriesViewClick, chartListener);
        
        // Add a 'Show Data' item to the chart widget's context menu.
        List<Tool> viewTools = new ArrayList<Tool>();
        ViewTool showDataTool = new ViewTool();
        showDataTool.setName(ClientUtil.CLIENT_CONSTANTS.showData());
	    viewTools.add(showDataTool);
        m_ChartWidget.setViewTools(viewTools);
        
        
        // Create a panel at the top to hold optionally the chart title and subtitle,
        // and the zoom in / zoom out controls.      
        HBoxLayout hblayout = new HBoxLayout();   
        hblayout.setPack(BoxLayoutPack.END); 
        hblayout.setHBoxLayoutAlign(HBoxLayoutAlign.MIDDLE);   
        LayoutContainer topCont = new LayoutContainer(hblayout);   
        topCont.setHeight(27);
        
        
        // Create the chart title and subtitle labels if required.
        if (m_ShowChartLabels == true)
        {
        	topCont.setHeight(32);
	        m_ChartLabelPanel = new LayoutContainer();
	        VBoxLayout labelPanelLayout = new VBoxLayout();
	        labelPanelLayout.setVBoxLayoutAlign(VBoxLayoutAlign.LEFT);
	        m_ChartLabelPanel.setLayout(labelPanelLayout);  
	        m_ChartLabelPanel.setHeight(32);
	        
	        m_ChartTitleLabel = new Label(ClientUtil.CLIENT_CONSTANTS.diagnosticsChart());
	        m_ChartTitleLabel.addStyleName("prl-timeSeriesChart-title");
	        
	        m_ChartSubtitleLabel = new Label("");
	        m_ChartSubtitleLabel.addStyleName("prl-timeSeriesChart-subtitle");
	        
	        m_ChartLabelPanel.add(m_ChartTitleLabel);
	        m_ChartLabelPanel.add(m_ChartSubtitleLabel);
	        
	        hblayout.setPack(BoxLayoutPack.START);
	        HBoxLayoutData flex = new HBoxLayoutData(new Margins(0, 5, 0, 0));   
	        flex.setFlex(1); 
	        topCont.add(m_ChartLabelPanel, flex);      
        }
        
        
        // Create show key checkbox, with label inside <div> (GXT 2.1.1 only way to style label!).
        final CheckBox showKeyToggle = new CheckBox();
        showKeyToggle.setBoxLabel("<div class=\"prl-timeSeriesChart-cb-label\">" + 
        		ClientUtil.CLIENT_CONSTANTS.showKey() + "</div>");
        showKeyToggle.setValue(true);
        showKeyToggle.addListener(Events.Change, new Listener<FieldEvent>(){

			@Override
            public void handleEvent(FieldEvent be)
            {
				m_Grid.setVisible(showKeyToggle.getValue());
				
				m_ChartWidget.setChartSize(m_ChartContainer.getWidth(), 
			    		m_ChartContainer.getHeight() - 50);
				
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
        
        // Create the zoom in/out tools.
        final ChartToolBar<TimeSeriesDataPoint> zoomTools = 
        	new ChartToolBar<TimeSeriesDataPoint>(m_ChartWidget, false);
        
        HBoxLayout zoomPanelLayout = new HBoxLayout();    
        zoomPanelLayout.setHBoxLayoutAlign(HBoxLayoutAlign.MIDDLE);      
        zoomPanelLayout.setPadding(new Padding(0, 3, 0, 3));    
        zoomPanelLayout.setPack(BoxLayoutPack.END);   
        
        LayoutContainer zoomPanel = new LayoutContainer(zoomPanelLayout);
        zoomPanel.setSize(240, 25);
        zoomPanel.add(showKeyToggle);
        zoomPanel.add(zoomTools);
        
        topCont.add(zoomPanel);   
        
        
        chartContainer.add(topCont); 
        chartContainer.add(m_ChartWidget.getChartWidget(), new VBoxLayoutData(0, 0, 0, 5)); 
        
        LoadListener loadListener = new LoadListener()
		{
			@Override
            public void loaderBeforeLoad(LoadEvent le)
			{
				zoomTools.setEnabled(false);
			}


			@Override
            public void loaderLoad(LoadEvent le)
			{
				zoomTools.setEnabled(true);
			}


			@Override
            public void loaderLoadException(LoadEvent le)
			{
				zoomTools.setEnabled(true);
			}
		};
		m_ChartWidget.addLoadListener(loadListener);
     	
     	return chartContainer;
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
					new Date(modelTime.getTime() - (m_TimeSpanSecs*1000)/2), 
					new Date(modelTime.getTime() + (m_TimeSpanSecs*1000)/2));
			
			// Load the episodes and their layout positions for the chart.
			ApplicationResponseHandler<List<ProbableCauseModelCollection>> callback = 
				new ApplicationResponseHandler<List<ProbableCauseModelCollection>>()
			{
				@Override
                public void uponFailure(Throwable caught)
				{
					GWT.log(ClientUtil.CLIENT_CONSTANTS.errorProbCauseData() + ": ", caught);
					MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
							ClientUtil.CLIENT_CONSTANTS.errorProbCauseData(), null);
					m_ChartContainer.unmask();
					m_Grid.unmask();
				}
	
	
				@Override
                public void uponSuccess(List<ProbableCauseModelCollection> aggregatedList)
				{	
					GWT.log("CausalityViewWidget.load() uponSuccess()");
					
					attachProbableCauseIds(aggregatedList);
					
					ListStore<ProbableCauseModelCollection> store = m_Grid.getStore();
					store.add(aggregatedList);
					m_Grid.unmask();
					
					selectDefault();
					m_ChartContainer.unmask();
				}
			};
			
			GWT.log("CausalityViewWidget.load() request data");
			m_CausalityQueryService.getAggregatedProbableCauses(
					m_Model.getId(), m_TimeSpanSecs, callback);

			updateChartLabels();
		}
	}
    
    
    /**
     * Updates the chart title and subtitle for the current item of evidence.
     */
    protected void updateChartLabels()
    {
    	if (m_ShowChartLabels)
    	{
    		if (m_Model.getDescription() != null)
			{
				m_ChartTitleLabel.setText(ClientUtil.CLIENT_CONSTANTS.diagnosticsFor() + 
						" " + m_Model.getDescription());
			}
			else
			{
				m_ChartTitleLabel.setText("");
				
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
						m_ChartTitleLabel.setText(ClientUtil.CLIENT_CONSTANTS.diagnosticsFor() + 
								" " + m_Model.getDescription());
						m_ChartLabelPanel.layout(true);
					}
				};
				
				m_EvidenceQueryService.getEvidenceSingle(m_Model.getId(), evidenceCallback);
			}
			
    		Date evidenceTime = m_Model.getTime(TimeFrame.SECOND);
        	String formattedTime = ClientUtil.formatTimeField(evidenceTime, TimeFrame.SECOND);
			m_ChartSubtitleLabel.setText(ClientUtil.CLIENT_CONSTANTS.fieldTimeOccurred() + 
					formattedTime);
    	}
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
	protected void attachProbableCauseIds(List<ProbableCauseModelCollection> aggregatedList)
	{
		int idCounter = 0;
		
		ArrayList<ProbableCauseModelCollection> notificationTypes = 
			new ArrayList<ProbableCauseModelCollection>();

		// Sort out the notification and time series types.
		for (ProbableCauseModelCollection model : aggregatedList)
		{
			DataSourceCategory category = model.getDataSourceCategory();
			switch (category)
			{
				case NOTIFICATION:
					notificationTypes.add(model);
					break;
					
				case TIME_SERIES:
					// This is used to map to a unique color chart index.
					model.setId(idCounter);
					idCounter++;
					
					// Display the first ProbableCauseModel in the aggregated list.
					List<ProbableCauseModel> probCauses = model.getProbableCauses();
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
                public int compare(ProbableCauseModelCollection model1, 
                		ProbableCauseModelCollection model2)
                {
	                return model1.getSeverity().compareTo(model2.getSeverity());
                }
			});
			
			// Set id counter to match with start of symbol set.
			int numSymbols = CSSSymbolChart.getInstance().getNumberOfSymbols();
			int modIndex = idCounter % numSymbols;
			if (modIndex != 0)
			{
				idCounter = idCounter + (numSymbols - modIndex);
			}
			
			for (ProbableCauseModelCollection model : notificationTypes)
			{
				model.setId(idCounter);
				idCounter++;
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
	 * @param probCause ProbableCauseModel to add to the chart.
	 */
	protected void addToChart(ProbableCauseModelCollection collection)
	{
		GWT.log("Add to chart: " + collection.getDescription());
		
		if (collection.getDataSourceCategory() == DataSourceCategory.NOTIFICATION)
		{
			m_ChartWidget.addNotifications(collection);
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
	    
	    m_ChartWidget.setChartSize(m_ChartContainer.getWidth(), 
	    		m_ChartContainer.getHeight() - 50);
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
	 * Returns the application-wide instance of causality evidence dialog, used for
	 * paging through a list of notifications matching a particular description from
	 * a probable cause incident.
	 * @return application-wide instance of the Causality Evidence dialog.
	 */
	protected static CausalityEvidenceDialog getEvidenceDialog()
	{
		if (s_EvidenceDialog == null)
		{
			s_EvidenceDialog = new CausalityEvidenceDialog();
		}
		
		return s_EvidenceDialog;
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
						EvidenceAttributesDialog dialog = ClientUtil.getEvidenceAttributesDialog();
						dialog.setEvidenceId(evidenceId);
						dialog.show();
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
				
				// Add the magnitude label.
				magnitudeText = ClientUtil.CLIENT_CONSTANTS.magnitude() + "=" + 
					ClientUtil.getShortDecimalFormat().format(probCause.getMagnitude());
				Label magnitudeLabel = new Label(magnitudeText);
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
