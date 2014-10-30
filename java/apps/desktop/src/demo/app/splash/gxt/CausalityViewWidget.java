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

package demo.app.splash.gxt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.Style.SortDir;
import com.extjs.gxt.ui.client.core.El;
import com.extjs.gxt.ui.client.core.XTemplate;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.store.GroupingStore;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.util.Padding;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.grid.CheckBoxSelectionModel;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.grid.GridGroupRenderer;
import com.extjs.gxt.ui.client.widget.grid.GroupColumnData;
import com.extjs.gxt.ui.client.widget.grid.GroupingView;
import com.extjs.gxt.ui.client.widget.grid.RowExpander;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayoutData;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.BoxLayout.BoxLayoutPack;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout.HBoxLayoutAlign;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout.VBoxLayoutAlign;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Widget;

import demo.app.client.ApplicationResponseHandler;
import demo.app.client.CSSColorChart;
import demo.app.client.ClientUtil;
import demo.app.client.ViewWidget;
import demo.app.client.event.GXTEvents;
import demo.app.client.event.RequestViewEvent;
import demo.app.data.CausalityView;
import demo.app.data.DataSourceCategory;
import demo.app.data.Evidence;
import demo.app.data.TimeFrame;
import demo.app.data.TimeSeriesConfig;
import demo.app.data.Tool;
import demo.app.data.View;
import demo.app.data.gxt.EvidenceModel;
import demo.app.data.gxt.ProbableCauseModel;
import demo.app.data.gxt.ProbableCauseModelCollection;
import demo.app.service.CausalityQueryServiceAsync;
import demo.app.service.EvidenceQueryServiceAsync;
import demo.app.splash.service.QueryServiceLocator;


/**
 * Ext GWT (GXT) widget for displaying a causality view. The widget consists of
 * a chart displaying time series and notification data, with a grid listing the
 * probable causes and symptoms for an item of evidence or time series discord.
 * @author Pete Harverson
 */
public class CausalityViewWidget extends VerticalPanel implements ViewWidget
{
	private CausalityView				m_CausalityView;
	
	private CausalityQueryServiceAsync 	m_CausalityQueryService;
	private EvidenceQueryServiceAsync 	m_EvidenceQueryService;
	
	private EvidenceModel				m_Model;		// The item of evidence or time series feature.
	
	private LayoutContainer				m_ChartContainer;
	private TimeSeriesChartWidget 		m_ChartWidget;
	private LayoutContainer 			m_ChartlabelPanel;
	private Label						m_ChartTitleLabel;
	private Label 						m_ChartSubtitleLabel;
	
	private int							m_TimeSpanSecs = 900;	// Time span of chart - 15 minutes.
	
	private Grid<ProbableCauseModelCollection> 	m_Grid;
	private ProbableCauseRowExpander			m_RowExpander;
	private List<ProbableCauseModelCollection>	m_SelectedList;
	private HashMap<Integer, ProbableCauseModel>	m_CheckedProbCauses;
	
	private CheckBoxSelectionModel<ProbableCauseModelCollection> m_CheckboxSm;
	
	/** Full height value for the causality chart. */
	public static final int				CHART_VALUE_RANGE = 1000;
	
	
	/**
	 * Creates a new widget for displaying causality data.
	 */
	public CausalityViewWidget(CausalityView causalityView)
	{
		// Define the JSNI bridge methods.
		defineBridgeMethod();
		
		m_CausalityView = causalityView;
		
		m_CausalityQueryService = QueryServiceLocator.getInstance().getCausalityQueryService();
		m_EvidenceQueryService = QueryServiceLocator.getInstance().getEvidenceQueryService();
		
		m_SelectedList = new ArrayList<ProbableCauseModelCollection>();
		
		// Create a map of probable cause collection ids against the probable cause
		// model that is selected (via a radio button) for each.
		m_CheckedProbCauses = new HashMap<Integer, ProbableCauseModel>();
		
		initComponents();
	}
	
	
	/**
	 * Creates and initialises the graphical components in the window.
	 */
	protected void initComponents()
	{	
		m_ChartContainer = createChartComponents();
		m_ChartContainer.setSize(750, 500);
		
		GroupingStore<ProbableCauseModelCollection> store = 
			new GroupingStore<ProbableCauseModelCollection>();   
		
		store.groupBy("dataSourceName");
		store.setSortField("time");
		store.setSortDir(SortDir.DESC);
		
		// Add a checkbox column for selecting probable causes to display in the chart.
		m_CheckboxSm = new CheckBoxSelectionModel<ProbableCauseModelCollection>();  
		m_CheckboxSm.setSelectionMode(SelectionMode.SIMPLE);
		
		m_CheckboxSm.addSelectionChangedListener(
			new SelectionChangedListener<ProbableCauseModelCollection>(){

                @Override
                public void selectionChanged(
                        SelectionChangedEvent<ProbableCauseModelCollection> se)
                {
                	processSelectedItems(se.getSelection());
                }
			
		});
		
		// Create a row expander plugin to list the probable causes for 
		// each type/time/description aggregation.		  
		XTemplate tpl = XTemplate.create(getAggregatedRowTemplateWithRadio());
		m_RowExpander = new ProbableCauseRowExpander();
		m_RowExpander.setTemplate(tpl);     
		
	    ColumnConfig timeColumn = new ColumnConfig("time", "time", 130);
	    timeColumn.setRenderer(new GridCellRenderer<ProbableCauseModelCollection>(){

			@Override
            public Object render(ProbableCauseModelCollection model,
                    String property, ColumnData config, int rowIndex,
                    int colIndex,
                    ListStore<ProbableCauseModelCollection> store,
                    Grid<ProbableCauseModelCollection> grid)
            {
				// Format the time field so that it is consistent with evidence lists.
				String html = "";
	            
				if (model != null)
				{
					html = ClientUtil.formatTimeField(model.getTime(), TimeFrame.SECOND);
				}
				
	            return html;
            }
	    	
	    });
	    
	    ColumnConfig typeColumn = new ColumnConfig("dataSourceName", "type", 110);
	    ColumnConfig descColumn = new ColumnConfig("description", "description", 130);
	    
	    ColumnConfig symbolColumn = new ColumnConfig("symbol", "symbol", 80);
	    symbolColumn.setRenderer(ChartSymbolCellRenderer.getInstance());

	    List<ColumnConfig> config = new ArrayList<ColumnConfig>();
	    config.add(m_CheckboxSm.getColumn());
	    config.add(m_RowExpander);
	    config.add(typeColumn);
	    config.add(timeColumn);
	    config.add(descColumn);
	    config.add(symbolColumn);

	    final ColumnModel columnModel = new ColumnModel(config);
	    
	    GroupingView view = new GroupingView();   
	    view.setShowGroupedColumn(false);   
	    view.setForceFit(true);   
	    view.setGroupRenderer(new GridGroupRenderer()
		{
			public String render(GroupColumnData data)
			{
				String f = columnModel.getColumnById(data.field).getHeader();
				String l = data.models.size() == 1 ? "Item" : "Items";
				return f + ": " + data.group + " (" + data.models.size() + " " +
				   			l + ")";
			}
		}); 
		
	    m_Grid = new Grid<ProbableCauseModelCollection>(store, columnModel);
	    m_Grid.setLoadMask(true);
	    m_Grid.setBorders(true);
	    m_Grid.setSize(750, 300);
	    m_Grid.setSelectionModel(m_CheckboxSm);
	    m_Grid.addPlugin(m_CheckboxSm);
	    m_Grid.addPlugin(m_RowExpander);   
	    m_Grid.setView(view);
	    
	    add(m_ChartContainer);
	    add(m_Grid);
	}
	
	
	/**
	 * Creates the graphical components for the chart.
	 * @return LayoutContainer holding the chart component.
	 */
	
    protected LayoutContainer createChartComponents()
	{
		LayoutContainer chartContainer = new LayoutContainer();   
        VBoxLayout layout = new VBoxLayout();    
        layout.setVBoxLayoutAlign(VBoxLayoutAlign.STRETCH);   
        chartContainer.setLayout(layout);  
  
        
        // Create a panel at the top to hold the chart title and subtitle,
        // and the zoom in / zoom out controls.
        m_ChartlabelPanel = new LayoutContainer();
        VBoxLayout labelPanelLayout = new VBoxLayout();
        labelPanelLayout.setVBoxLayoutAlign(VBoxLayoutAlign.LEFT);
        m_ChartlabelPanel.setLayout(labelPanelLayout);  
        m_ChartlabelPanel.setHeight(50);
        
        m_ChartTitleLabel = new Label(ClientUtil.CLIENT_CONSTANTS.diagnosticsChart());
        m_ChartTitleLabel.addStyleName("prl-timeSeriesChart-title");
        
        m_ChartSubtitleLabel = new Label("");
        m_ChartSubtitleLabel.addStyleName("prl-timeSeriesChart-subtitle");
        
        m_ChartlabelPanel.add(m_ChartTitleLabel);
        m_ChartlabelPanel.add(m_ChartSubtitleLabel);
        
        
        LayoutContainer zoomBtnsC = new LayoutContainer();   
        HBoxLayout zbcLayout = new HBoxLayout();   
        zbcLayout.setPadding(new Padding(5));   
        zbcLayout.setHBoxLayoutAlign(HBoxLayoutAlign.TOP);   
        zbcLayout.setPack(BoxLayoutPack.END);   
        zoomBtnsC.setLayout(zbcLayout);   
        zoomBtnsC.setHeight(50);
        zoomBtnsC.setWidth(150);
        
        Anchor zoomInAnchor = new Anchor("<img src=\"images/zoom_in.png\" width=\"16\" height=\"16\" >" +
        		ClientUtil.CLIENT_CONSTANTS.zoomInLink(), true);
        zoomInAnchor.setStyleName("prl-timeSeriesChart-zoomText");
        zoomInAnchor.addClickHandler(new ClickHandler(){

			@Override
            public void onClick(ClickEvent event)
            {
	            m_ChartWidget.zoomInDateAxis();
            }
        	
        });
        
        Anchor zoomOutAnchor = new Anchor("<img src=\"images/zoom_out.png\" width=\"16\" height=\"16\" />" +
        		ClientUtil.CLIENT_CONSTANTS.zoomOutLink(), true);
        zoomOutAnchor.addStyleName("prl-timeSeriesChart-zoomText");
        zoomOutAnchor.addClickHandler(new ClickHandler(){

			@Override
            public void onClick(ClickEvent event)
            {
	            m_ChartWidget.zoomOutDateAxis();
            }
        	
        });
        
        zoomBtnsC.add(zoomInAnchor, new HBoxLayoutData(new Margins(0, 10, 0, 0)));    
        zoomBtnsC.add(zoomOutAnchor, new HBoxLayoutData(new Margins(0)));   
        
        
        LayoutContainer zoomPanel = new LayoutContainer();
        VBoxLayout zoomPanelLayout = new VBoxLayout();
        zoomPanelLayout.setVBoxLayoutAlign(VBoxLayoutAlign.RIGHT);
        zoomPanel.setLayout(zoomPanelLayout);
        zoomPanel.setHeight(50);
        zoomPanel.setWidth(160);
        zoomPanel.add(zoomBtnsC);
        
        
        LayoutContainer topCont = new LayoutContainer();   
        topCont.setHeight(50);
        HBoxLayout hblayout = new HBoxLayout();   
        hblayout.setPadding(new Padding(5));   
        hblayout.setHBoxLayoutAlign(HBoxLayoutAlign.MIDDLE);   
        topCont.setLayout(hblayout);   
  
        HBoxLayoutData flex = new HBoxLayoutData(new Margins(0, 5, 0, 0));   
        flex.setFlex(1);   
        topCont.add(m_ChartlabelPanel, flex);    
        topCont.add(zoomPanel);   
        
        chartContainer.add(topCont);
        
        m_ChartWidget = new TimeSeriesGChartWidget();
        m_ChartWidget.setChartHeight(410);
        m_ChartWidget.setValueTickLabelsVisible(false);
        m_ChartWidget.setAutoValueRange(false);
        m_ChartWidget.setValueRange(0, CHART_VALUE_RANGE + 2);	// Allow for width of plot line.
        
        // Listen for events to open notification, time series and causality views.
        @SuppressWarnings("unchecked")
		Listener<RequestViewEvent> chartListener = new Listener<RequestViewEvent>(){

            public void handleEvent(RequestViewEvent rve)
            {
            	Date selectedTime = m_ChartWidget.getSelectedTime();
            	
            	// If a notification is selected, set the source name in the event.
        		int evidenceId = m_ChartWidget.getSelectedNotificationId();
        		if (evidenceId != -1)
        		{
        			// Find the Probable Cause corresponding to the selected notification.
        			Iterator<ProbableCauseModel> probCausesIter = 
        				m_CheckedProbCauses.values().iterator();
        			ProbableCauseModel probCause;
        			EvidenceModel evidence;
        			while (probCausesIter.hasNext())
        			{
        				probCause = probCausesIter.next();
        				if ( (probCause.getDataSourceCategory() == DataSourceCategory.NOTIFICATION)
        						&& (evidenceId == Integer.parseInt(probCause.getAttributeValue())) )
        				{
        					evidence = new EvidenceModel();
        					evidence.setId(evidenceId);
        					evidence.setSource(probCause.getSource());
        					evidence.set(EvidenceModel.getTimeColumnName(TimeFrame.SECOND), 
        							selectedTime);

        					rve.setSourceName(probCause.getSource());
        					rve.setModel(evidence);
        					
        					break;
        				}
        			}	
        		}
            	
        		fireEvent(rve.getType(), rve);
            }
			
		};
		
		m_ChartWidget.addListener(GXTEvents.OpenNotificationViewClick, chartListener);
		m_ChartWidget.addListener(GXTEvents.OpenTimeSeriesViewClick, chartListener);
		m_ChartWidget.addListener(GXTEvents.OpenCausalityViewClick, chartListener);
        
        // Configure context menu tools for launching other view types.
        List<Tool> viewTools = m_CausalityView.getContextMenuItems();
        m_ChartWidget.setViewTools(viewTools);
        
        chartContainer.add(m_ChartWidget.getChartWidget()); 
     	
     	return chartContainer;
	}
	
	
	/**
	 * Loads the list of probable causes into the window.
	 */
    public void load()
	{
		if (m_Model != null)
		{
			m_CheckboxSm.setFiresEvents(false);
			m_Grid.getStore().removeAll();
			m_CheckboxSm.setFiresEvents(true);
			m_ChartWidget.removeAll();
			m_SelectedList.clear();
			m_CheckedProbCauses.clear();
			
			// Centre the causality chart in a fifteen minute window around the feature.
			Date modelTime = ClientUtil.parseTimeField(m_Model, TimeFrame.SECOND);	
			m_ChartWidget.setTimeMarker(modelTime, null);
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
					MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
							ClientUtil.CLIENT_CONSTANTS.errorProbCauseData(), null);
				}
	
	
				@Override
                public void uponSuccess(List<ProbableCauseModelCollection> aggregatedList)
				{	
					attachProbableCauseIds(aggregatedList);
					ListStore<ProbableCauseModelCollection> store = m_Grid.getStore();
					store.add(aggregatedList);
					
					selectDefault();
				}
			};
			
			m_CausalityQueryService.getAggregatedProbableCauses(
					m_Model.getId(), m_TimeSpanSecs, callback);
			
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
								ClientUtil.CLIENT_CONSTANTS.errorEvidenceData(), null);
					}
		
		
					@Override
	                public void uponSuccess(EvidenceModel evidence)
					{	
						setEvidence(evidence);
						m_ChartTitleLabel.setText(ClientUtil.CLIENT_CONSTANTS.diagnosticsFor() + 
								" " + m_Model.getDescription());
						m_ChartlabelPanel.layout(true);
					}
				};
				
				m_EvidenceQueryService.getEvidenceSingle(m_Model.getId(), evidenceCallback);
			}
			
			m_ChartSubtitleLabel.setText(ClientUtil.CLIENT_CONSTANTS.fieldTimeOccurred() + 
					m_Model.getTime(TimeFrame.SECOND));
			
		}
	}
    
    
    /**
	 * Returns the View displayed in the Widget.
	 * @return the view displayed in the Widget.
	 */
    public View getView()
    {
	    // TODO Auto-generated method stub
	    return null;
    }


    /**
	 * Returns the user interface Widget sub-class itself.
	 * @return the Widget which is added in the user interface.
	 */
    public Widget getWidget()
    {
	    return this;
    }


    /**
	 * Runs a tool on the view in the widget.
	 * @param tool the tool to run.
	 */
    public void runTool(Tool tool)
    {
    	// No longer used. Needs to be removed from ViewWidget interface.
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
	 * chart. This will consist of all notification types, and the top 6 time series
	 * type (by scaling factor).
	 */
	public void selectDefault()
	{
		// Select all notifications.
		// Select the top 6 time series (by scaling factor).
		List<ProbableCauseModelCollection> aggregatedList = m_Grid.getStore().getModels();
		
		List<ProbableCauseModelCollection> listToSelect = 
			new ArrayList<ProbableCauseModelCollection>();
		
		List<ProbableCauseModelCollection> timeSeries = 
			new ArrayList<ProbableCauseModelCollection>();
		
		for (ProbableCauseModelCollection model : aggregatedList)
		{
			switch (model.getDataSourceCategory())
			{
				case NOTIFICATION:
					listToSelect.add(model);
					break;
				
				case TIME_SERIES:
					timeSeries.add(model);
					break;
			}
		}
		
		if (timeSeries.size() > 0)
		{
			Collections.sort(timeSeries, new Comparator<ProbableCauseModelCollection>(){

                public int compare(ProbableCauseModelCollection model1,
                        ProbableCauseModelCollection model2)
                {
	                double scalingFactor1 = model1.getProbableCause(0).getScalingFactor();
	                double scalingFactor2 = model2.getProbableCause(0).getScalingFactor();
	                
	                return Double.compare(scalingFactor2, scalingFactor1);
                }
				
			});
			
			int numToAdd = Math.min(6, timeSeries.size());
			{
				for (int i = 0; i < numToAdd; i++)
				{
					listToSelect.add(timeSeries.get(i));
				}
			}
		}
		
		m_CheckboxSm.select(listToSelect, false);
		
	}
	
	
	/**
	 * Attaches ids to the supplied list of ProbableCauseModelCollection objects
	 * and their constituent ProbableCauseModel objects so that they can be 
	 * readily identified during UI operations.
	 * @param aggregatedList list of aggregated probable causes. 
	 */
	protected void attachProbableCauseIds(List<ProbableCauseModelCollection> aggregatedList)
	{
		int timeSeriesIndex = 0;
		int notificationIndex = aggregatedList.size();	// To ensure no overlap with time series.
		
		int probCauseCounterIndex = 0;

		for (ProbableCauseModelCollection model : aggregatedList)
		{
			// Attach id to the ProbableCauseModelCollection.
			DataSourceCategory category = model.getDataSourceCategory();
			switch (category)
			{
				case NOTIFICATION:
					model.setId(notificationIndex);
					notificationIndex++;
					break;
					
				case TIME_SERIES:
					// This is used to map to a unique color chart index.
					model.setId(timeSeriesIndex);
					timeSeriesIndex++;
					break;
			}
			
			probCauseCounterIndex = 0;
			
			List<ProbableCauseModel> probCauses = model.getProbableCauses();
			if (probCauses != null && probCauses.size() > 0)
			{
				// First probable cause is checked by default.
				m_CheckedProbCauses.put(model.getId(), probCauses.get(0));
				
				// Set a 'checked' property on the Probable Cause that is selected
				// for each aggregate collection.
				probCauses.get(0).set("checked", "checked");
				
				for (ProbableCauseModel probCause : probCauses)
				{
					probCause.setId(probCauseCounterIndex);
					probCauseCounterIndex++;
				}
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
		// Compare this new list with the previous one to work out which items
		// need to be added or removed from the chart.	
		ProbableCauseModel probCause;
		
		for (ProbableCauseModelCollection model : m_SelectedList)
		{
			if (items.contains(model) == false)
			{
				GWT.log("Remove from chart: " + model, null);
				probCause = m_CheckedProbCauses.get(model.getId());
				removeFromChart(probCause);
			}
		}

		for (ProbableCauseModelCollection model : items)
		{
			if (m_SelectedList.contains(model) == false)
			{
				GWT.log("Add to chart: " + model, null);
				probCause = m_CheckedProbCauses.get(model.getId());
				
				addToChart(model, probCause);
			}
		}
		
		m_SelectedList.clear();
		m_SelectedList.addAll(items);
	}
	
	
	/**
	 * Adds the probable cause from the specified ProbableCauseModelCollection
	 * to the causality chart.
	 * @param collection aggregated collection containing the probable cause to 
	 * 		add to the chart.
	 * @param probCause ProbableCauseModel to add to the chart.
	 */
	protected void addToChart(ProbableCauseModelCollection collection,
			ProbableCauseModel probCause)
	{
		if (probCause.getDataSourceCategory() == DataSourceCategory.NOTIFICATION)
		{
			Evidence notification = new Evidence();
			notification.setId(Integer.parseInt(probCause.getAttributeValue()));
			notification.setTime(probCause.getTime());
			
			int numNotifications = collection.getSize();
			if (numNotifications == 1)
			{
				notification.setDescription(probCause.getDescription() + ", " + probCause.getSource());
			}
			else
			{
				notification.setDescription(probCause.getDescription() + " (x" + numNotifications + ")");
			}
			
			notification.setSeverity(collection.getSeverity());
			m_ChartWidget.addNotification(notification);
		}
		else if (probCause.getDataSourceCategory() == DataSourceCategory.TIME_SERIES)
		{	
			TimeSeriesConfig timeSeriesConfig = new TimeSeriesConfig(
					probCause.getDataSourceName(),
					probCause.getMetric(),
					probCause.getSource(),
					probCause.getAttributeName(),
					probCause.getAttributeValue());
			timeSeriesConfig.setDescription(probCause.getDescription());
			timeSeriesConfig.setAttributeLabel(probCause.getAttributeLabel());
			timeSeriesConfig.setScalingFactor(probCause.getScalingFactor() * CHART_VALUE_RANGE);
			
			// Obtain the colour to use for this time series in the chart.
			// IDs for time series ProbableCauseModelCollection are generated
			// sequentially to map to a unique color chart index.
			int index = collection.getId();
			String color = CSSColorChart.getInstance().getColor(index);
			m_ChartWidget.addTimeSeries(timeSeriesConfig, color);
			m_ChartWidget.load(timeSeriesConfig);
		}
	}
	
	
	/**
	 * Removes the specified probable cause from the chart.
	 * @param probCause ProbableCauseModel to remove from the chart.
	 */
	protected void removeFromChart(ProbableCauseModel probCause)
	{
		if (probCause.getDataSourceCategory() == DataSourceCategory.NOTIFICATION)
		{
			Evidence notification = new Evidence();
			notification.setId(Integer.parseInt(probCause.getAttributeValue()));
			m_ChartWidget.removeNotification(notification);
		}
		else if (probCause.getDataSourceCategory() == DataSourceCategory.TIME_SERIES)
		{	
			TimeSeriesConfig timeSeriesConfig = new TimeSeriesConfig(
					probCause.getDataSourceName(),
					probCause.getMetric(),
					probCause.getSource(),
					probCause.getAttributeName(),
					probCause.getAttributeValue());
			timeSeriesConfig.setDescription(probCause.getDescription());
			timeSeriesConfig.setAttributeLabel(probCause.getAttributeLabel());
			m_ChartWidget.removeTimeSeries(timeSeriesConfig);
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
	 * Overrides setHeight(int) to set the height of the grid so that it
	 * maximises the available height. This method fires the <i>Resize</i> event.
	 * @param height the new height to set.
	 */
	@Override
    public void setHeight(int height)
	{
		super.setHeight(height);

		int availableHeight = (height - m_ChartContainer.getHeight());
		int gridHeight = Math.max(90, availableHeight);
		m_Grid.setHeight(gridHeight);
	}
	
	
	/**
	 * Called when the user clicks on probable cause radio button in the grid.
	 * @param probCauseCollectionId id of the probable cause collection containing
	 * 	the probable cause that has been checked.
	 * @param probCauseId id of the probable cause that has been checked.
	 */
	protected void onProbableCauseChecked(int probCauseCollectionId, int probCauseId)
	{
		ProbableCauseModelCollection collection = 
			m_Grid.getStore().findModel("id", probCauseCollectionId);
		
		// Is this a change to the Probable Cause that is checked?
		ProbableCauseModel currentChecked = m_CheckedProbCauses.get(probCauseCollectionId);
		int currentId = currentChecked.getId();
		
		if (probCauseId != currentId)
		{
			// Update the 'checked' property, used by the RowExpander template
			// for the radio buttons.
			ProbableCauseModel newProbCause = collection.getProbableCause(probCauseId);
			newProbCause.set("checked", "checked");
			
			ProbableCauseModel currentProbCause = 
				m_CheckedProbCauses.put(probCauseCollectionId, newProbCause);
			currentProbCause.set("checked", "");
			
			if (m_SelectedList.contains(collection))
			{
				removeFromChart(currentProbCause);
				addToChart(collection, newProbCause);
			}
			
			m_RowExpander.updateGridRowOnClick(collection);
		}
		
	}
	
	
	public native void defineBridgeMethod() /*-{    
	var causalityWidget = this;  
	$wnd.onProbableCauseChecked = function(probCauseCollectionId, probCauseId)  {          
		causalityWidget.@demo.app.splash.gxt.CausalityViewWidget::onProbableCauseChecked(II)(probCauseCollectionId, probCauseId);       
	}    
}-*/;
	
	
	/**
	 * Returns the template to use for the grid row expander plug-in containing
	 * a radio button for time series aggregated probable causes.
	 * @return the html fragment of the template.
	 */
	private native String getAggregatedRowTemplateWithRadio() /*-{  

    var html = [  
    '<table class="prl-causalityList">', 
    '<tpl for="probableCauses">',
	'<tpl if="dataSourceCategory == &quot;NOTIFICATION&quot;">',
	'<tr><td style="width: 20px;">&nbsp;</td><td style="width: 130px;">source={source}</td><td>{attributeLabel}</td></tr></tpl>',  
	    
	'<tpl if="dataSourceCategory == &quot;TIME_SERIES&quot;">',   
	'<tpl if="xindex == 1"><tr>',
    '<td style="width: 20px;"><input type="radio" name="{parent.id}" value="{id}" onClick="onProbableCauseChecked({parent.id}, {id})" {checked} /></td>',
    '<td style="width: 130px;"><b>source={source}</b></td><td><b>{attributeLabel}</b></td></tr></tpl>',  
    '<tpl if="xindex &gt; 1"><tr>',
    '<td style="width: 20px;"><input type="radio" name="{parent.id}" value="{id}" onClick="onProbableCauseChecked({parent.id}, {id})" {checked}/></td>',
    '<td style="width: 130px;">source={source}</td><td>{attributeLabel} </td></tr></tpl>',  
    '</tpl>',
    
    '</tpl>',
    '<table>'
    ];  
    return html.join("");  
  }-*/;
	
	
	/**
	 * Returns the template to use for the grid row expander plug-in containing
	 * a hyperlink.
	 * @return the html fragment of the template.
	 */
	private native String getAggregatedRowTemplateWithLink() /*-{  
    var html = [  
    '<table class="prl-causalityList">', 
    '<tpl for="probableCauses">',  
    '<tpl if="xindex == 1"><tr><td style="width: 130px;"><a class=\"probcauseListLink cursor-pointer\">',
    '<b>source={source}</b></a></td><td><b>{attributeLabel}</b></td></tr></tpl>',  
    '<tpl if="xindex &gt; 1"><tr><td style="width: 130px;">source={source}</td><td>{attributeLabel}</td></tr></tpl>',  
    '</tpl>',
    '<table>'
    ];  
    return html.join("");  
  }-*/;
	
	
	
	/**
	 * Custom grid RowExpander to ensure the HTML is updated when the selection of
	 * a ProbableCauseModel for display is changed through radio button click.
	 */
	class ProbableCauseRowExpander extends RowExpander
	{
		protected void updateGridRowOnClick(ProbableCauseModelCollection collection)
		{
			// Get the element for this row.
			com.google.gwt.dom.client.Element row = m_Grid.getView().getRow(collection);
			El rowEl = El.fly(row);
			com.google.gwt.user.client.Element body = rowEl.childElement("div.x-grid3-row-body");
			
			// Update the HTML
			// NB. pass dummy row index - not used.
			if (body != null)
			{
				body.setInnerHTML(getBodyContent(collection, 0));
			}
		}
	}

}
