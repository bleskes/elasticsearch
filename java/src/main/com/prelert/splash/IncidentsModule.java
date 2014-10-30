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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.event.SliderEvent;
import com.extjs.gxt.ui.client.event.SplitBarEvent;
import com.extjs.gxt.ui.client.util.DateWrapper;
import com.extjs.gxt.ui.client.util.IconHelper;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.util.Padding;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.Slider;
import com.extjs.gxt.ui.client.widget.SplitBar;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout.VBoxLayoutAlign;
import com.extjs.gxt.ui.client.widget.toolbar.FillToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.LabelToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.client.chart.ChartToolBar;
import com.prelert.client.chart.IncidentChartWidget;
import com.prelert.client.chart.IncidentGChartWidget;
import com.prelert.client.event.ChartWidgetEvent;
import com.prelert.client.event.ChartWidgetListener;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.RequestViewEvent;
import com.prelert.client.gxt.ModuleComponent;
import com.prelert.client.incident.ActivitySummaryTreeWidget;
import com.prelert.data.TimeFrame;
import com.prelert.data.gxt.IncidentModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.IncidentQueryServiceAsync;


/**
 * The Incidents UI module. The container has two main sections:
 * <ul>
 * <li>An anomaly timeline representing anomalies by time and magnitude. The user 
 * can dynamically alter the 'anomaly threshold' allowing them to filter the 
 * size of anomaly to investigate.</li>
 * <li>An activity summary view showing why a selected anomaly occurred.</li>
 * </ul>
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>OpenCausalityViewClick</b> : RequestViewEvent<br>
 * <div>Fires when a request is made to show the analysis of an activity.</div>
 * <ul>
 * <li>component : this</li>
 * <li>model: notification or time series feature whose causality data is being requested</li>
 * </ul>
 * </dd>
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
 * <div>Fires after a generic link to a data view is selected.</div>
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
public class IncidentsModule extends LayoutContainer implements ModuleComponent
{
	public static final String MODULE_ID = "activity";
	
	private IncidentQueryServiceAsync	m_IncidentQueryService;
	
	private ToolBar						m_TimelineToolBar;
	private Slider						m_AnomalySlider;
	private Timer						m_SliderLoaderTimer;
	
	private ContentPanel				m_TimelineContainer;
	private ChartToolBar<IncidentModel>	m_ChartTools;
	private IncidentChartWidget			m_TimelineWidget;
	
	private ContentPanel				m_SummaryPanel;
	private ActivitySummaryTreeWidget	m_SummaryViewWidget;
	
	protected Button 					m_RefreshBtn;
	private TimelineRefreshTimer		m_RefreshTimer;
	private boolean						m_IsTimelineAutoRefreshing;
	
	
	/**
	 * Creates the UI module for the Proactive Incidents View.
	 */
	public IncidentsModule()
	{
		m_IncidentQueryService = AsyncServiceLocator.getInstance().getIncidentQueryService();
		
		BorderLayout layout = new BorderLayout();   
		layout.setContainerStyle("prl-viewport");
	    setLayout(layout);  
	    
	    // Create the components for the Incident Timeline.
	    m_TimelineContainer = createTimeline(); 

	    
	    // Create the components for the Incident summary panel.   
	    m_SummaryPanel = createSummaryPanel();
	    
	    
	    // Listen for resize events to set the width of the timeline and details panel.
		Listener<ComponentEvent> resizeListener = new Listener<ComponentEvent>(){
			
			public void handleEvent(ComponentEvent be)
            {
				fitComponentsToSize();
            }
		};	
		addListener(Events.Resize, resizeListener);
        
		BorderLayoutData northData = new BorderLayoutData(LayoutRegion.NORTH, 270, 200, 400);   
        northData.setSplit(true);    
        northData.setMargins(new Margins(10, 10, 5, 10)); 
        
        BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);   
	    centerData.setMargins(new Margins(0, 10, 10, 10));  
	    
	    final Listener<SplitBarEvent> splitBarListener = new Listener<SplitBarEvent>()
		{
			public void handleEvent(SplitBarEvent sbe)
			{
				if (sbe.getSize() < 1)
				{
					return;
				}

				m_TimelineWidget.setChartHeight(m_TimelineContainer.getInnerHeight()
						- m_TimelineToolBar.getHeight());
			}
		};
		
		// Add a listener to the BorderLayout splitbar to resize the timeline on drag end.
		m_TimelineContainer.addListener(Events.Attach, new Listener<ComponentEvent>(){

			@Override
            public void handleEvent(ComponentEvent be)
            {
				SplitBar splitBar = m_TimelineContainer.getData("splitBar");
		        if (splitBar != null)
		        {
		        	splitBar.addListener(Events.DragEnd, splitBarListener);
		        	m_TimelineContainer.removeListener(Events.Attach, this);
		        }
            }
			
		});
		
		
		add(m_TimelineContainer, northData);   
		add(m_SummaryPanel, centerData);
	}
	
	
	/**
	 * Creates the incident time line components.
	 * @return the incident time line.
	 */
	protected ContentPanel createTimeline()
	{
        VBoxLayout layout = new VBoxLayout();    
        layout.setVBoxLayoutAlign(VBoxLayoutAlign.STRETCH);   
        layout.setPadding(new Padding(0, 3, 0, 3));
        
        ContentPanel timelineContainer = new ContentPanel(layout);   
        timelineContainer.setHeading(ClientUtil.CLIENT_CONSTANTS.activityTimelineHeading());
        
        // Create the chart widget for the timeline.
	    m_TimelineWidget = new IncidentGChartWidget();
        
        // Create the toolbar holding the slider, refresh button and zoom/pan tools.
        m_TimelineToolBar = createTimelineToolBar();

        timelineContainer.add(m_TimelineToolBar);
        timelineContainer.add(m_TimelineWidget.getChartWidget());
        
        LoadListener loadListener = new LoadListener()
		{
			@Override
            public void loaderBeforeLoad(LoadEvent le)
			{
				m_RefreshBtn.setIcon(IconHelper.createStyle("x-tbar-loading"));
				m_ChartTools.setEnabled(false);
			}


			@Override
            public void loaderLoad(LoadEvent le)
			{
				if (m_IsTimelineAutoRefreshing == true)
				{
					m_RefreshBtn.setToolTip(ClientUtil.CLIENT_CONSTANTS.autoRefreshPause());
					m_RefreshBtn.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.toolbar_pause()));
				}
				else
				{
					m_RefreshBtn.setToolTip(ClientUtil.CLIENT_CONSTANTS.autoRefreshStart());
					m_RefreshBtn.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.toolbar_play()));
				}
				m_ChartTools.setEnabled(true);
			}


			@Override
            public void loaderLoadException(LoadEvent le)
			{
				if (m_IsTimelineAutoRefreshing == true)
				{
					m_RefreshBtn.setToolTip(ClientUtil.CLIENT_CONSTANTS.autoRefreshPause());
					m_RefreshBtn.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.toolbar_pause()));
				}
				else
				{
					m_RefreshBtn.setToolTip(ClientUtil.CLIENT_CONSTANTS.autoRefreshStart());
					m_RefreshBtn.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.toolbar_play()));
				}
				m_ChartTools.setEnabled(true);
			}
		};
		m_TimelineWidget.addLoadListener(loadListener);
		
		m_TimelineWidget.addSelectionChangedListener(
				new SelectionChangedListener<IncidentModel>(){

			@Override
	        public void selectionChanged(SelectionChangedEvent<IncidentModel> se)
	        {
				setSummaryIncident(se.getSelectedItem());
	        }
			
		});
		
		
		ChartWidgetListener<IncidentModel>  widgetListener = 
			new ChartWidgetListener<IncidentModel> ()
		{
            @Override
            public void chartBeforePan(ChartWidgetEvent<IncidentModel> e)
            {
            	setTimelineAutoRefreshing(false);
            }


            @Override
            public void chartBeforeZoom(ChartWidgetEvent<IncidentModel> e)
            {
            	setTimelineAutoRefreshing(false);
            }
			
		};
		m_TimelineWidget.addChartWidgetListener(widgetListener);
		
		// Pause time line auto-refresh when the module is hidden.
		addListener(Events.Hide, new Listener<ComponentEvent>(){

			@Override
            public void handleEvent(ComponentEvent be)
            {
				pauseTimelineAutoRefreshing();
            }
			
		});
		
		// Resume time line auto-refresh when the module is shown.
		addListener(Events.Show, new Listener<ComponentEvent>(){

			@Override
            public void handleEvent(ComponentEvent be)
            {
				resumeTimelineAutoRefreshing();
            }
			
		});
        
        return timelineContainer;
	}
	
	
	/**
	 * Creates the toolbar for the Incident timeline.
	 */
	protected ToolBar createTimelineToolBar()
	{
		ToolBar toolbar = new ToolBar();
		toolbar.addStyleName("prl-internal-toolbar");
		toolbar.setBorders(false);
		toolbar.setSpacing(2);
		
		// Add a Slider for setting the noise level.
		// Add a 0.5s timer delay on loading to prevent unnecessary incremental
		// loads as the slider is moved back and forth.
		m_AnomalySlider = new Slider();
		m_AnomalySlider.setWidth(210);
		m_AnomalySlider.setIncrement(1);
		m_AnomalySlider.setMinValue(1);
		m_AnomalySlider.setMaxValue(100);
		m_AnomalySlider.setUseTip(false);
		m_AnomalySlider.addListener(Events.Change, new Listener<SliderEvent>()
		{
            public void handleEvent(SliderEvent se)
            {
				m_SliderLoaderTimer.schedule(500);
            }

		});

		LayoutContainer sliderCont = new LayoutContainer();   
		sliderCont.setWidth(210);
		sliderCont.addStyleName("prl-anomaly-slider");
		sliderCont.add(m_AnomalySlider);
		
		m_SliderLoaderTimer = new Timer(){
			@Override
			public void run() 
			{
				m_TimelineWidget.setAnomalyThreshold(m_AnomalySlider.getValue());
				m_TimelineWidget.load();
			}
	    };
		
		m_RefreshBtn = new Button();
		m_RefreshBtn.setMouseEvents(false);		// Disable mouse events for plain white toolbar.
		m_RefreshBtn.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.toolbar_play()));
		m_RefreshBtn.setToolTip(ClientUtil.CLIENT_CONSTANTS.autoRefreshStart());
		m_RefreshBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
			{
				setTimelineAutoRefreshing(!m_IsTimelineAutoRefreshing);
				if (m_IsTimelineAutoRefreshing == true)
				{
					m_RefreshBtn.setToolTip(ClientUtil.CLIENT_CONSTANTS.autoRefreshPause());
					m_RefreshBtn.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.toolbar_pause()));
				}
				else
				{
					m_RefreshBtn.setToolTip(ClientUtil.CLIENT_CONSTANTS.autoRefreshStart());
					m_RefreshBtn.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.toolbar_play()));
				}
			}
		});
		
		toolbar.add(new LabelToolItem(ClientUtil.CLIENT_CONSTANTS.anomalyThreshold()));
		toolbar.add(sliderCont);
		toolbar.add(new SeparatorToolItem());
		toolbar.add(m_RefreshBtn);
		toolbar.add(new FillToolItem());
		
		// Add the zooming and panning tools.
		m_ChartTools = new ChartToolBar<IncidentModel>(m_TimelineWidget, true);
		toolbar.add(m_ChartTools);
		
		return toolbar;
	}
	
	
	/**
	 * Creates the components for the incident summary panel.
	 * @return the panel holding the incident summary view.
	 */
	protected ContentPanel createSummaryPanel()
	{
		ContentPanel summaryPanel = new ContentPanel();
		summaryPanel.setHeading(ClientUtil.CLIENT_CONSTANTS.selectActivity());
	    
	    BorderLayout containerLayout = new BorderLayout();
	    containerLayout.setContainerStyle("prl-border-layout-ct");
		
	    summaryPanel.setLayout(containerLayout);
		
		// Create the Incident Summary widget in a split point as 
	    // it is unlikely to be displayed initially.
		GWT.runAsync(new RunAsyncCallback()
		{
			public void onFailure(Throwable caught)
			{
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						ClientUtil.CLIENT_CONSTANTS.errorDownloadingModule(), null);
			}


			public void onSuccess()
			{
				// Create the widget, but don't add it to the panel until an
				// incident is selected for the first time.
				m_SummaryViewWidget = new ActivitySummaryTreeWidget();
				
				// Add listeners to the widget for OpenCausalityViewClick,
				// OpenNotificationViewClick and OpenTimeSeriesViewClick events.
				Listener<RequestViewEvent<?>> showDataListener = 
					new Listener<RequestViewEvent<?>>(){

		            public void handleEvent(RequestViewEvent<?> rve)
		            {
		            	// Set the module as the source and then propagate the event.
		            	rve.setSource(IncidentsModule.this);
		        		fireEvent(rve.getType(), rve);
		            }
					
				};
				
				m_SummaryViewWidget.addListener(GXTEvents.OpenNotificationViewClick, showDataListener);
				m_SummaryViewWidget.addListener(GXTEvents.OpenTimeSeriesViewClick, showDataListener);
				m_SummaryViewWidget.addListener(GXTEvents.OpenCausalityViewClick, showDataListener);
				
				// If an incident has already been selected, display the summary.
				List<IncidentModel> selected = m_TimelineWidget.getSelection();
				if ( (selected != null) && (selected.size() > 0) )
				{
					setSummaryIncident(selected.get(0));
				}
			}
		});
		
	    return summaryPanel;
	}
	
	
	@Override
	public Component getComponent()
	{
		return this;
	}


	@Override
	public String getModuleId()
	{
		return MODULE_ID;
	}


	/**
	 * Loads incidents into timeline for the current anomaly threshold, so that
	 * the most recent incident in the database is displayed.
	 */
	public void showLatestIncident()
	{
		// Obtain the time of the most recent incident, then load the data 
		// for the current time span in the chart.
		m_IncidentQueryService.getLatestTime(new ApplicationResponseHandler<Date>(){

			@Override
            public void uponFailure(Throwable problem)
            {
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
		        		ClientUtil.CLIENT_CONSTANTS.errorLoadingActivityData(), null);
            }

			@Override
            public void uponSuccess(Date latestTime)
            {
	            if (latestTime != null)
	            {
	            	setTimelineEndDate(latestTime);
	            } 
	            else
	            {
	            	// No incidents in DB 
	            	// 	- set the date range of the chart to show the last 24 hours.
	            	setTimelineEndDate(new Date());
	            }
	            
            }
		
		});
	}
	
	
	/**
	 * Resizes the components in the container to fit the available width and height.
	 */
	protected void fitComponentsToSize()
	{	
		// Only need to set the chart width.
		// Chart height is unaltered as the timeline is in North position.
		m_TimelineWidget.setChartWidth(getWidth());
	}
	
	
	/**
	 * Displays the incident containing the notification or time series feature 
	 * with the specified id in the time line, and then automatically selects
	 * that activity to display its summary in the lower part of the module.
	 * @param evidenceId evidence ID of notification or time series feature.
	 */
	public void showIncidentForEvidence(int evidenceId)
	{
		// Check to see if there is incident with this headline ID on the time line.
		IncidentModel incident = m_TimelineWidget.getIncident(evidenceId);
		if (incident != null)
		{
			m_TimelineWidget.setSelection(Collections.singletonList(incident));
		}
		else
		{
			final int evId = evidenceId;
			
			// Obtain the activity containing this item of evidence.
			ApplicationResponseHandler<IncidentModel> callback = 
				new ApplicationResponseHandler<IncidentModel>()
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
	            public void uponSuccess(IncidentModel activity)
				{	
					if (activity != null)
					{
						showIncident(activity);
					}
					else
					{
						GWT.log(ClientUtil.CLIENT_CONSTANTS.errorNoActivityForId(evId));
						MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
								ClientUtil.CLIENT_CONSTANTS.errorNoActivityForId(evId), null);
						
						showLatestIncident();
					}
				}
			};
			
			IncidentQueryServiceAsync incidentQueryService =
				AsyncServiceLocator.getInstance().getIncidentQueryService();
			incidentQueryService.getIncident(evidenceId, callback);
		}
	}
	
	
	/**
	 * Displays the specified incident in the time line, and then automatically 
	 * selects that activity to display its summary in the lower part of the module.
	 * @param incident activity to display in the module.
	 */
	public void showIncident(IncidentModel incident)
	{
		// Check to see if this incident is already on the time line.
		if (m_TimelineWidget.getIncident(incident.getEvidenceId()) != null)
		{
			m_TimelineWidget.setSelection(Collections.singletonList(incident));
		}
		else
		{
			// To ensure the activity is displayed, the anomaly slider must be set
			// at a value less than or equal to the anomaly score of the activity.
			int anomalySetting = m_TimelineWidget.getAnomalyThreshold();
			int anomalyScore = incident.getAnomalyScore();
			if (anomalySetting > anomalyScore)
			{
				m_TimelineWidget.setAnomalyThreshold(anomalyScore);
			}
			
			// Zoom level must be set to less than 1 hour - set time range to be 
			// 15 minutes either side of the incident time.
			Date incidentTime = incident.getTime();
			Date startTime = new Date((incidentTime.getTime() - (15 * 60000) ));
			Date endTime = new Date((incidentTime.getTime() + (15 * 60000) ));
			m_TimelineWidget.setDateRange(startTime, endTime);
			
			// Select activity in time line after loading.
			final IncidentModel activity = incident;
			m_TimelineWidget.addLoadListener(new LoadListener(){
	
	            @Override
	            public void loaderLoad(LoadEvent le)
	            {
		            m_TimelineWidget.setSelection(Collections.singletonList(activity));
		            m_TimelineWidget.removeLoadListener(this);
	            }
				
			});
			
			m_TimelineWidget.load();
		}
	}
	
	
	/**
	 * Sets the end point of the incidents time line to the supplied value, 
	 * adjusting the start point so that the current time span of the time line
	 * is preserved. Incident data is then reloaded for the new time span.
	 * @param endTime end time of the incident data to show. Note that some padding
	 * 			is added to the end so that the time marker is in clear view.
	 */
	public void setTimelineEndDate(Date endTime)
	{
		// Preserve the time span on the chart.
    	// Default time span of 24 hours if data has yet to be loaded.
        long timeSpan = 24 * 60 * 60 * 1000;
        Date currentStartTime = m_TimelineWidget.getStartTime();
        Date currentEndTime =  m_TimelineWidget.getEndTime();
        if (currentStartTime != null && currentEndTime != null)
        {
        	timeSpan = currentEndTime.getTime() - currentStartTime.getTime();         	
        }
    	
    	// Add an hour / minute to the end time so that the time marker is 
        // a little way in from the right hand end.
        DateWrapper endTimeWrapper = new DateWrapper(endTime);
    	if (timeSpan > 10800000)
    	{
    		endTimeWrapper = endTimeWrapper.addHours(1);
    	}
    	else
    	{
    		endTimeWrapper = endTimeWrapper.addMinutes(1);
    	}
    	
    	Date end = endTimeWrapper.asDate();  
        Date start = new Date(end.getTime()-timeSpan); 
        
        m_TimelineWidget.setDateRange(start, end); 
    	m_TimelineWidget.load();
	}
	
	
	/**
	 * Sets the incident to display in the summary view.
	 * The evidence id, time and description fields must be set in the supplied incident.
	 * @param incident incident for which to show a summary, or <code>null</code> 
	 * 		to clear the summary panel.
	 */
	protected void setSummaryIncident(IncidentModel incident)
	{
		// Note the Summary View widget is made in a splitpoint, so may not be initialised.
		if (m_SummaryViewWidget != null)
		{
			if (incident != null)
	        {	
				if (m_SummaryViewWidget.isRendered() == false)
				{
					BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);   
				    centerData.setMargins(new Margins(5));
				    
				    m_SummaryPanel.add(m_SummaryViewWidget, centerData);
				    m_SummaryPanel.layout(true);
				    m_SummaryPanel.getHeader().disableTextSelection(false);	// Must be done post-render.
				}
				
				if (m_SummaryViewWidget.isVisible() == false)
				{
					m_SummaryViewWidget.setVisible(true);
					m_SummaryViewWidget.layout(true);	
				}
				
				m_SummaryViewWidget.setIncident(incident);
	        	
	        	Date incidentTime = incident.getTime();
	        	String formattedTime = ClientUtil.formatTimeField(incidentTime, TimeFrame.SECOND);
	        	String heading = ClientUtil.CLIENT_CONSTANTS.detailsOnActivity(
	        			formattedTime, incident.getDescription());
	        	m_SummaryPanel.setHeading(heading);
	        }
			else
			{
				m_SummaryPanel.setHeading(ClientUtil.CLIENT_CONSTANTS.selectActivity());
				m_SummaryViewWidget.setVisible(false);
			}
		}
	}
	
	
	/**
	 * Enables or disables auto-refreshing on the time line.
	 * @param doAutoRefresh <code>true</code> to enable auto-refreshing,
	 * 			<code>false</code> to disable it.
	 */
	public void setTimelineAutoRefreshing(boolean doAutoRefresh)
	{
		if (m_RefreshTimer == null)
		{
			if (doAutoRefresh == true)
			{
				// Obtain the refresh frequency from the server, and start the timer.
				m_IncidentQueryService.getTimelineAutoRefreshFrequency(new ApplicationResponseHandler<Integer>(){
	
					@Override
		            public void uponFailure(Throwable problem)
		            {
						MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
				        		ClientUtil.CLIENT_CONSTANTS.errorLoadingActivityData(), null);
		            }
	
					@Override
		            public void uponSuccess(Integer frequency)
		            {
			            m_RefreshTimer = new TimelineRefreshTimer(frequency * 1000);
			            setTimelineAutoRefreshing(true);
		            }
				
				});
			}
		}
		else
		{
			if (m_IsTimelineAutoRefreshing != doAutoRefresh)
			{
				m_IsTimelineAutoRefreshing = doAutoRefresh;		
				m_RefreshTimer.setRepeating(doAutoRefresh);
			}
		}
	}
	
	
	/**
	 * Returns whether the time line has been set to auto-refresh.
	 * @return <code>true</code> if the time line has been set to auto-refresh,
	 * 			<code>false</code> otherwise.
	 */
	public boolean isTimelineAutoRefreshing()
	{
		return m_IsTimelineAutoRefreshing;
	}
	
	
	/**
	 * Pauses auto-refreshing if the time line has been set to auto-refresh.
	 */
	public void pauseTimelineAutoRefreshing()
	{
		if (isTimelineAutoRefreshing())
		{
			m_RefreshTimer.setRepeating(false);
		}
	}
	
	
	/**
	 * Resumes auto-refreshing if the time line has been set to auto-refresh.
	 */
	public void resumeTimelineAutoRefreshing()
	{
		if (isTimelineAutoRefreshing())
		{
			m_RefreshTimer.setRepeating(true);
		}
	}
	
	
	/**
	 * Shows or hides the button which turns the time line auto-refresh on or off. 
	 * @param show <code>true</code> to show the time line auto-refresh button, 
	 * 	<code>false</code> to hide.
	 */
	public void showTimelineAutoRefreshButton(boolean show)
	{
		int separatorIdx = m_TimelineToolBar.indexOf(m_RefreshBtn) - 1;
		
		if (show == true)
		{
			m_TimelineToolBar.getItem(separatorIdx).show();
			m_RefreshBtn.show();
		}
		else
		{
			m_TimelineToolBar.getItem(separatorIdx).hide();
			m_RefreshBtn.hide();
		}
	}
	
	
	/**
	 * Timer class to provide automatic refresh on the incident time line.
	 */
	class TimelineRefreshTimer extends Timer
	{
		private int m_Frequency; // Refresh frequency, in millis.
		
		
		/**
		 * Creates a new Timer to automatically refresh at the specified frequency.
		 * @param frequencyMillis refresh frequency, in milliseconds.
		 */
		public TimelineRefreshTimer(int frequencyMillis)
		{
			m_Frequency = frequencyMillis;
		}
		
		
		/**
		 * Sets whether the timer should be elapsing repeatedly.
		 * @param repeating <code>true</code> to elapse repeatedly, 
		 * 		<code>false</code> to stop the timer.
		 */
		public void setRepeating(boolean repeating)
		{
			if (repeating == true)
			{
				scheduleRepeating(m_Frequency);
				
				// Kick off a refresh immediately.
				run();
			}
			else
			{
				cancel();
			}
		}
		
		
        public void run()
        {
        	setTimelineEndDate(new Date());
        }
	}

}
