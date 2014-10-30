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

import java.util.Date;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.util.DateWrapper;
import com.extjs.gxt.ui.client.util.IconHelper;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.util.Padding;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayoutData;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout.HBoxLayoutAlign;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout.VBoxLayoutAlign;
import com.google.gwt.core.client.GWT;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.client.chart.ChartToolBar;
import com.prelert.client.chart.TimeSeriesChartWidget;
import com.prelert.client.chart.TimeSeriesGChartWidget;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.RequestViewEvent;
import com.prelert.data.MetricPath;
import com.prelert.data.TimeFrame;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.TimeSeriesGXTPagingServiceAsync;


/**
 * A GXT panel which displays a time series chart. The panel consists of a charting
 * component, rendering a single time series as a line plot, with a toolbar containing 
 * buttons for zooming, panning and refreshing the chart.
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>OpenCausalityViewClick</b> : RequestViewEvent<br>
 * <div>Fires after a link to the Analysis View is selected.</div>
 * <ul>
 * <li>component : this</li>
 * <li>model: time series feature whose causality data is being requested</li>
 * </ul>
 * </dd>
 * 
 * @author Pete Harverson
 */
public class TimeSeriesChartPanel extends ContentPanel
{
	private TimeSeriesGXTPagingServiceAsync	m_TimeSeriesQueryService;
	
	private TimeSeriesConfig		m_Config;
	
	private TimeSeriesChartWidget	m_ChartWidget;
	private ChartToolBar<TimeSeriesDataPoint> m_ChartTools;
	
	private Label 					m_TitleLabel;
	private Label					m_SubtitleLabel;
	
	private LayoutContainer			m_TopCont;
	protected Button 				m_RefreshBtn;
	
	
	/**
	 * Creates a new panel holding a chart for plotting time series data.
	 */
	public TimeSeriesChartPanel()
	{
		m_TimeSeriesQueryService = AsyncServiceLocator.getInstance().getTimeSeriesGXTQueryService();
		
		setHeaderVisible(false);
		
		VBoxLayout layout = new VBoxLayout(); 
        layout.setVBoxLayoutAlign(VBoxLayoutAlign.STRETCH);   
        setLayout(layout);  
        
        initComponents();
	}
	
	
	/**
	 * Creates and initialises the graphical components in the panel.
	 */
	protected void initComponents()
	{
		// Create a panel at the top to hold the chart labels,
        // and the zoom and refresh controls.
        m_TopCont = new LayoutContainer();   
        m_TopCont.setHeight(45);
        HBoxLayout hblayout = new HBoxLayout();   
        hblayout.setPadding(new Padding(0, 3, 0, 3));    
        hblayout.setHBoxLayoutAlign(HBoxLayoutAlign.TOP);   
        m_TopCont.setLayout(hblayout); 
        HBoxLayoutData flex = new HBoxLayoutData();   
    	flex.setFlex(1);  

        
    	// Create the labels for displaying the title and subtitle.
    	m_TitleLabel = new Label();
    	m_TitleLabel.addStyleName("prl-timeSeriesChart-title");
        
    	m_SubtitleLabel = new Label();
    	m_SubtitleLabel.addStyleName("prl-timeSeriesChart-subtitle");
        
        VerticalPanel labelPanel = new VerticalPanel();
        labelPanel.add(m_TitleLabel);
        labelPanel.add(m_SubtitleLabel);
        
        flex.setMargins(new Margins(5, 5, 0, 0)); 
        m_TopCont.add(labelPanel, flex); 
        
        
        m_ChartWidget = new TimeSeriesGChartWidget();
        m_ChartWidget.setHighlightingFeatures(true);
        
        // Listen for events to open causality views from features.
		m_ChartWidget.addListener(GXTEvents.OpenCausalityViewClick, 
				new Listener<RequestViewEvent<EvidenceModel>>(){

            public void handleEvent(RequestViewEvent<EvidenceModel> rve)
            {
            	// Propagate the event.
        		fireEvent(rve.getType(), rve);
            }
			
		});

        
        // Add the zooming and panning tools, and the refresh button.
        m_ChartTools = new ChartToolBar<TimeSeriesDataPoint>(m_ChartWidget, true);
        m_ChartTools.setWidth(126);
        
        m_RefreshBtn = new Button();
		m_RefreshBtn.setIcon(GXT.IMAGES.paging_toolbar_refresh());
		m_RefreshBtn.setToolTip(GXT.MESSAGES.pagingToolBar_refreshText());
		m_RefreshBtn.setMouseEvents(false);		// Disable mouse events for plain white toolbar.
		m_RefreshBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
			{
				refresh();
			}
		});
		m_ChartTools.add(m_RefreshBtn);
		m_ChartTools.setEnabled(false);	// Disabled till a chart is loaded.
        
        m_TopCont.add(m_ChartTools);
        
        add(m_TopCont);
        add(m_ChartWidget.getChartWidget()); 
        
        LoadListener loadListener = new LoadListener()
		{
			@Override
            public void loaderBeforeLoad(LoadEvent le)
			{
				m_RefreshBtn.setIcon(IconHelper.createStyle("x-tbar-loading"));
				m_ChartTools.setEnabled(false);
				mask(GXT.MESSAGES.loadMask_msg());
			}


			@Override
            public void loaderLoad(LoadEvent le)
			{
				m_RefreshBtn.setIcon(IconHelper.createStyle("x-tbar-refresh"));
				m_ChartTools.setEnabled(true);
				unmask();
			}


			@Override
            public void loaderLoadException(LoadEvent le)
			{
				m_RefreshBtn.setIcon(IconHelper.createStyle("x-tbar-refresh"));
				m_ChartTools.setEnabled(true);
				unmask();
			}
		};
		m_ChartWidget.addLoadListener(loadListener);
	}
	
	
	/**
	 * Sets the time series for plotting in the chart panel.
	 * @param config configuration of time series to display.
	 */
	public void setTimeSeriesConfig(TimeSeriesConfig config)
	{
		m_Config = config;
	}
	
	
	/**
	 * Loads the chart for the time series containing the feature with the specified id.
	 * @param feature time series feature to load.
	 */
	public void loadFeature(EvidenceModel feature)
	{		
		final EvidenceModel evidence = feature;
		
		// Need to obtain the TimeSeriesConfig for this evidence id. 
		ApplicationResponseHandler<TimeSeriesConfig> callback = 
			new ApplicationResponseHandler<TimeSeriesConfig>(){

				@Override
                public void uponSuccess(TimeSeriesConfig config)
                {
					if (config != null)
					{
						setTimeSeriesConfig(config);
						setLoadTime(evidence.getTime(TimeFrame.SECOND));
						load();
						setLabelsForTimeSeriesId(m_Config.getTimeSeriesId());
					}
					else
					{
						MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
								ClientUtil.CLIENT_CONSTANTS.errorNoTimeSeriesForId(evidence.getId()), null);
					}
                }

				@Override
                public void uponFailure(Throwable caught)
                {
					GWT.log("Error loading time series feature with id: " + evidence.getId(), caught);
					MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
							ClientUtil.CLIENT_CONSTANTS.errorLoadingTimeSeriesData(), null);
                }
			
		};
		
		m_TimeSeriesQueryService.getConfigurationForFeature(feature.getId(), callback);
	}
	
	
	/**
	 * Loads the chart according to its current configuration.
	 */
	public void load()
	{			
		m_ChartWidget.removeAllTimeSeries();
		m_ChartWidget.addTimeSeries(m_Config);
		
		Date endTime = m_ChartWidget.getEndTime();
		if (endTime == null)
		{
			// Load the latest hour of data.
			refresh();
		}
		else
		{
			m_ChartWidget.load(m_Config);
		}
	}
	
	
	/**
	 * Sets the time span to use for the next load operation to be 30 minutes 
	 * either side of the specified time.
	 * @param time date/time to use for the next load operation. If <code>null</code>
	 * 	the date on the chart is set back to the default of January 1, 1970, 00:00:00 GMT
	 * 	and the time marker cleared.
	 */
	public void setLoadTime(Date time)
	{
		if (time != null)
		{
			// Load 30 mins before/after the supplied time.
			DateWrapper startTime = new DateWrapper(time);
			startTime = startTime.addMinutes(-30);
			DateWrapper endTime = new DateWrapper(time);
			endTime = endTime.addMinutes(30);
			
			m_ChartWidget.setDateRange(startTime.asDate(), endTime.asDate());
			
			m_ChartWidget.setTimeMarker(time);
		}
		else
		{
			m_ChartWidget.setDateRange(null, null);
			m_ChartWidget.clearTimeMarker();
		}
	}
	
	
	/**
	 * Refreshes the chart for the current time series, loading up the most recent 
	 * data for the currently configured time span.
	 */
	public void refresh()
	{
		// Obtain the time of the most recent data for this time series,
		// then load the data for the current time span in the chart.
		m_TimeSeriesQueryService.getLatestTime(m_Config, new ApplicationResponseHandler<Date>(){

			@Override
            public void uponFailure(Throwable problem)
            {
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
		        		ClientUtil.CLIENT_CONSTANTS.errorNoServerResponse(), null);
            }

			@Override
            public void uponSuccess(Date latestTime)
            {
	            if (latestTime != null)
	            {
		            Date startTime = m_ChartWidget.getStartTime();
		            Date endTime = m_ChartWidget.getEndTime();
		            
		            // Default time span of 1 hour.
		            long timeSpan = 60 * 60 * 1000;
		            if (startTime != null && endTime != null)
		            {
		            	timeSpan = endTime.getTime() - startTime.getTime();         	
		            }
		            
		            Date newStart = new Date(latestTime.getTime()-timeSpan);
	    			m_ChartWidget.setDateRange(newStart, latestTime); 
	    			load();
	            }    
            }
		
		});
	}
	
	
	/**
	 * Overrides setSize(int, int) to set the width of the chart component to
	 * fill the available space.
	 * @param width the new width to set
	 * @param height the new height to set
	 */
    @Override
    public void setSize(int width, int height)
    {
	    super.setSize(width, height);

	    // For height, subtract height for the labels.
	    m_ChartWidget.setChartSize(width, getHeight() - m_TopCont.getHeight());
    }
    
    
    /**
     * Sets the text for the chart title label.
     * @param text new title label text
     */
    public void setTitleLabelText(String text)
    {
    	m_TitleLabel.setText(text);
    }
    
    
    /**
     * Sets the text for the chart subtitle label.
     * @param text new subtitle label text.
     */
    public void setSubtitleLabelText(String text)
    {
    	m_SubtitleLabel.setText(text);	
    }
    
    
    /**
     * Updates the labels on the chart for the time series with the specified id.
     * @param timeSeriesId time series id.
     */
    protected void setLabelsForTimeSeriesId(final int timeSeriesId)
    {
    	// Need to obtain the metric path for this time series id. 
		ApplicationResponseHandler<MetricPath> callback = 
			new ApplicationResponseHandler<MetricPath>(){

				@Override
                public void uponSuccess(MetricPath metricPath)
                {
					if (metricPath != null)
					{
						m_TitleLabel.setText(metricPath.getPartialPath());
						m_SubtitleLabel.setText(metricPath.getLastLevelValue());		
					}
					else
					{
						MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
								ClientUtil.CLIENT_CONSTANTS.errorNoMetricPathForId(timeSeriesId), null);
					}
                }

				@Override
                public void uponFailure(Throwable caught)
                {
					GWT.log("Error loading metric path for time series with id: " + timeSeriesId, caught);
					MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
							ClientUtil.CLIENT_CONSTANTS.errorLoadingTimeSeriesData(), null);
                }
			
		};
		
		m_TimeSeriesQueryService.getMetricPathFromTimeSeriesId(timeSeriesId, callback);
    }
}
