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
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
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
import com.prelert.data.Incident;
import com.prelert.data.TimeFrame;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.IncidentQueryServiceAsync;


/**
 * The Incidents UI module. The container has two main sections:
 * <ul>
 * <li>An anomaly timeline representing anomalies by time and magnitude. The user 
 * can dynamically alter the 'anomaly threshold' allowing them to filter the 
 * size of anomaly to investigate.</li>
 * <li>A causality view showing why a selected anomaly occurred.</li>
 * </ul>
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
	private IncidentQueryServiceAsync	m_IncidentQueryService;
	
	private ToolBar						m_TimelineToolBar;
	private Slider						m_AnomalySlider;
	private Timer						m_SliderLoaderTimer;
	
	private ContentPanel				m_TimelineContainer;
	private ChartToolBar<Incident>		m_ChartTools;
	private IncidentChartWidget			m_TimelineWidget;
	
	private ContentPanel				m_DetailsPanel;
	private CausalityViewWidget 		m_CausalityViewWidget;
	
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

	    
	    // Create the components for the Causality View details panel.
	    m_DetailsPanel = createDetailsPanel();
	    
	    
	    // Listen for resize events to set the width of the timeline and details panel.
		Listener<ComponentEvent> resizeListener = new Listener<ComponentEvent>(){
			
			public void handleEvent(ComponentEvent be)
            {
				 fitComponentsToSize();
            }
		};	
		addListener(Events.Select, resizeListener);
		addListener(Events.Resize, resizeListener);
        
        BorderLayoutData northData = new BorderLayoutData(LayoutRegion.NORTH, 270, 200, 400);   
        northData.setSplit(true);    
        northData.setMargins(new Margins(0, 0, 5, 0)); 
        
        BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);   
	    centerData.setMargins(new Margins(0));  
	    
	    final Listener<SplitBarEvent> splitBarListener = new Listener<SplitBarEvent>()
		{
			public void handleEvent(SplitBarEvent sbe)
			{
				if (sbe.getSize() < 1)
				{
					return;
				}

				m_TimelineWidget.setChartHeight(m_TimelineContainer.getHeight() - 85);
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
	    add(m_DetailsPanel, centerData);
	}
	
	
	/**
	 * Creates the incident time line components.
	 * @return the incident time line.
	 */
	protected ContentPanel createTimeline()
	{
        VBoxLayout layout = new VBoxLayout();    
        layout.setVBoxLayoutAlign(VBoxLayoutAlign.STRETCH);   
        
        ContentPanel timelineContainer = new ContentPanel(layout);   
        timelineContainer.setHeading(ClientUtil.CLIENT_CONSTANTS.incidentTimelineHeading());
        
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
				new SelectionChangedListener<EvidenceModel>(){

			@Override
	        public void selectionChanged(SelectionChangedEvent<EvidenceModel> se)
	        {
	            setDetailsEvidence(se.getSelectedItem());
	        }
			
		});
		
		
		ChartWidgetListener widgetListener = new ChartWidgetListener()
		{
            @Override
            public void chartBeforePan(ChartWidgetEvent e)
            {
            	setTimelineAutoRefreshing(false);
            }


            @Override
            public void chartBeforeZoom(ChartWidgetEvent e)
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
        
        // Load latest incidents on opening.
		showLatestIncident();
        
        return timelineContainer;
	}
	
	
	/**
	 * Creates the toolbar for the Incident timeline.
	 */
	protected ToolBar createTimelineToolBar()
	{
		ToolBar toolbar = new ToolBar();
		toolbar.addStyleName("prl-incident-toolbar");
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
		
		// Test out export functionality
		Button exportPdfBtn = new Button("PDF");
		exportPdfBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
			{
				String baseURL = GWT.getModuleBaseURL();
				String url = baseURL + "services/causalityExport?fileType=pdf";	
				Window.open(url, "_self", "");
			}
		});
		
		Button exportCsvBtn = new Button("CSV");
		exportCsvBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
			{
				String baseURL = GWT.getModuleBaseURL();
				String url = baseURL + "services/causalityExport?fileType=csv";	
				Window.open(url, "_self", "");
			}
		});
		
		toolbar.add(exportPdfBtn);
		toolbar.add(exportCsvBtn);
		
		toolbar.add(new FillToolItem());
		
		// Add the zooming and panning tools.
		m_ChartTools = new ChartToolBar<Incident>(m_TimelineWidget, true);
		toolbar.add(m_ChartTools);
		
		return toolbar;
	}
	
	
	/**
	 * Creates the Causality View components for the incident details panel.
	 * @param <M> data associated with RequestViewEvents fired by the Causality View 
	 * 		i.e. the selected EvidenceModel or TimeSeriesConfig.
	 * @return the incident details panel.
	 */
	protected <M> ContentPanel createDetailsPanel()
	{
		ContentPanel detailsPanel = new ContentPanel();
	    detailsPanel.setHeading(ClientUtil.CLIENT_CONSTANTS.selectIncident());
	    
	    BorderLayout containerLayout = new BorderLayout();
		containerLayout.setContainerStyle("prl-viewport");
		
		BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);   
	    centerData.setMargins(new Margins(5));  
		
		detailsPanel.setLayout(containerLayout);
		
		// Create the Causality View widget but don't add it to the panel as
		// initially no incident is selected.
		m_CausalityViewWidget = new CausalityViewWidget(
				CausalityViewWidget.KeyPosition.RIGHT, false);
		
		// Add listeners to the widget for 'Show Data' events fired off
		// notifications or time series in the causality view chart.
		Listener<RequestViewEvent<M>> showDataListener = 
			new Listener<RequestViewEvent<M>>(){

            public void handleEvent(RequestViewEvent<M> rve)
            {
            	// Set the module as the source and then propagate the event.
            	rve.setSource(IncidentsModule.this);
        		fireEvent(rve.getType(), rve);
            }
			
		};
		
		m_CausalityViewWidget.addListener(GXTEvents.OpenNotificationViewClick, showDataListener);
		m_CausalityViewWidget.addListener(GXTEvents.OpenTimeSeriesViewClick, showDataListener);
		m_CausalityViewWidget.addListener(GXTEvents.OpenViewClick, showDataListener);
		
	    return detailsPanel;
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
	 * Returns id for the Incidents module.
	 * @return the Incidents module ID.
	 */
	@Override
	public String getModuleId()
	{
		return ClientUtil.CLIENT_CONSTANTS.incidents();
	}
	
	
	/**
	 * Loads incidents into timeline for the current anomaly threshold, so that
	 * the most recent incident in the database is displayed.
	 */
	protected void showLatestIncident()
	{
		// Obtain the time of the most recent incident, then load the data 
		// for the current time span in the chart.
		m_IncidentQueryService.getLatestTime(new ApplicationResponseHandler<Date>(){

			@Override
            public void uponFailure(Throwable problem)
            {
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
		        		ClientUtil.CLIENT_CONSTANTS.errorLoadingIncidentData(), null);
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
	 * Sets the item of evidence whose causality view should be shown in the
	 * details panel. The id, time and description fields must be set in the supplied
	 * EvidenceModel.
	 * @param evidence item of evidence to show the causality details, 
	 * 			or <code>null</code> to clear the details panel.
	 */
	protected void setDetailsEvidence(EvidenceModel evidence)
	{
		if (evidence != null)
        {
			if (m_CausalityViewWidget.isRendered() == false)
			{
				BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);   
			    centerData.setMargins(new Margins(5));
			    
				m_DetailsPanel.add(m_CausalityViewWidget, centerData);
				m_DetailsPanel.layout(true);
			}
			
			if (m_CausalityViewWidget.isVisible() == false)
			{
				m_CausalityViewWidget.setVisible(true);
				m_CausalityViewWidget.layout(true);
			}
			
        	m_CausalityViewWidget.setEvidence(evidence);
        	m_CausalityViewWidget.load();
        	
        	Date evidenceTime = evidence.getTime(TimeFrame.SECOND);
        	String formattedTime = ClientUtil.formatTimeField(evidenceTime, TimeFrame.SECOND);
        	m_DetailsPanel.setHeading(ClientUtil.CLIENT_CONSTANTS.detailsOnIncident(formattedTime, evidence.getDescription()));
        }
		else
		{
			m_DetailsPanel.setHeading(ClientUtil.CLIENT_CONSTANTS.selectIncident());
			m_CausalityViewWidget.setVisible(false);
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
				        		ClientUtil.CLIENT_CONSTANTS.errorLoadingIncidentData(), null);
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
