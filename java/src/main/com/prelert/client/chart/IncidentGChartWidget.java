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

package com.prelert.client.chart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.event.SelectionProvider;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.extjs.gxt.ui.client.widget.menu.SeparatorMenuItem;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.GChart.Axis;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.client.list.AttributeListDialog;
import com.prelert.data.gxt.IncidentModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.IncidentQueryServiceAsync;


/**
 * Implementation of the IncidentChartWidget which uses the GChart Google
 * Web Toolkit (GWT) extension for the charting component
 * (see {@link http://code.google.com/p/clientsidegchart/}).
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>SelectionChange</b> : SelectionEvent(source, selection)<br>
 * <div>Fires after the incident selection changes.</div>
 * <ul>
 * <li>source : this</li>
 * <li>selection : IncidentModel with evidence id, time and description of selected
 * incident. If <code>null</code> then the chart no longer contains a selection.</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>BeforeLoad</b> : LoadEvent(loader, config)<br>
 * <div>Fires before a load operation.</div>
 * <ul>
 * <li>loader : this</li>
 * <li>config : anomaly threshold</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>Load</b> : LoadEvent(loader, config)<br>
 * <div>Fires after the incidents have been loaded and the timeline updated.</div>
 * <ul>
 * <li>loader : this</li>
 * <li>config : anomaly threshold</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>LoadException</b> : LoadEvent(loader, config, throwable)<br>
 * <div>Fires when an error occurs during the load operation.</div>
 * <ul>
 * <li>loader : this</li>
 * <li>config : anomaly threshold</li>
 * <li>throwable : error that has occurred</li>
 * </ul>
 * </dd>
 * 
 * </dl>
 * 
 * @author Pete Harverson
 */
public class IncidentGChartWidget extends GChartWidget<IncidentModel> 
	implements IncidentChartWidget, SelectionProvider<IncidentModel>
{
	private IncidentQueryServiceAsync	m_IncidentQueryService;
	
	private	int				m_AnomalyThreshold;
	
	
	/**
	 * Creates a new incident chart widget which uses a GChart GWT component
	 * for plotting the incidents on a timeline.
	 */
	public IncidentGChartWidget()
	{
		super(new IncidentGChart());
		
		setMaxZoom(30);
		
		m_IncidentQueryService = AsyncServiceLocator.getInstance().getIncidentQueryService();
		
		// Listen for click events on incidents so that they can be marked
		// as selected and then a SelectionChangedEvent fired.
		// Add a listener to mark clicked incidents as selected.
		m_Chart.addClickHandler(new ClickHandler(){

			@Override
            public void onClick(ClickEvent ev)
            {
				IncidentModel incident = getChart().getTouchedIncident();
				if (incident != null)
				{
					setSelection(Collections.singletonList(incident));
				}
            }
			
		});
		
		initContextMenu();
		
	}
	
	
	/**
	 * Initialises the right-click context menu, adding items specific to the
	 * incident timeline chart.
	 */
	protected void initContextMenu()
	{
		// Add a 'Show Details' item at the top of the existing menu 
		// (which `will already contain zoom in and zoom out items).
	    Menu menu = getChart().getContextMenu();
	    
	    final MenuItem showDetailsMenuItem = new MenuItem(ClientUtil.CLIENT_CONSTANTS.showDetails());
	    SelectionListener<MenuEvent> showDetailsListener = new SelectionListener<MenuEvent>()
	    {
			@Override
            public void componentSelected(MenuEvent ce)
            {
				IncidentModel selected = getChart().getTouchedIncident();
				if (selected != null)
				{
					AttributeListDialog dialog = AttributeListDialog.getInstance();
					dialog.showActivityAttributes(selected.getEvidenceId());
				}
            }
			
	    };
	    
	    showDetailsMenuItem.addSelectionListener(showDetailsListener);
	    menu.insert(showDetailsMenuItem, 0);
	    menu.insert(new SeparatorMenuItem(), 1);
	}
	
	
	/**
	 * Returns the IncidentGChart component which is displaying the incidents 
	 * on a timeline.
	 * @return the IncidentGChart GWT widget.
	 */
    @Override
    public IncidentGChart getChart()
    {
	    return (IncidentGChart)m_Chart;
    }


    @Override
	public void addIncident(IncidentModel incident)
	{
		getChart().addIncident(incident);
	}
	
	
    @Override
    public void removeAllIncidents()
    {
    	getChart().removeAllIncidents();
    }
    
    
    @Override
    public IncidentModel getIncident(int evidenceId)
    {
		return getChart().getIncident(evidenceId);
    }


	@Override
	public int getAnomalyThreshold()
	{
		return m_AnomalyThreshold;
	}
	
	
	@Override
	public void setAnomalyThreshold(int threshold)
	{
		m_AnomalyThreshold = threshold;
	}
	
	
	/**
	 * Loads the data in the widget according to its current configuration.
	 * The lower and upper time bounds for the data loaded will be
	 * set according to the current time span of the chart.
	 * @return <code>true</code> if the load was requested.
	 */
	@Override
    public boolean load()
	{
		fireEvent(BeforeLoad, new LoadEvent(this, new Integer(m_AnomalyThreshold)));

		IncidentQueryCallback callback = new IncidentQueryCallback();
		
		// Make the call to the Incident query service.
		m_IncidentQueryService.getIncidents(m_StartTime, m_EndTime, m_AnomalyThreshold, callback);
		
		return true;
	}
	
	
	/**
	 * Loads the data in the widget for the specified anomaly threshold.
	 * The lower and upper time bounds for the data loaded will be
	 * set according to the current time span of the chart.
	 * @return <code>true</code> if the load was requested.
	 */
    @Override
    public boolean load(Object anomalyThreshold)
    {
    	if (anomalyThreshold != null && anomalyThreshold.getClass() == Integer.class)
    	{
    		m_AnomalyThreshold = ((Integer)anomalyThreshold).intValue();
    		return load();
    	}
    	else
    	{
    		return false;
    	}
    }
    

    @Override
    public void panLeft()
    {
	    super.panLeft();
	    
		load();
    }


    @Override
    public void panRight()
    {
	    super.panRight();
	    
		load();
    }


	@Override
    public void zoomInDateAxis(Date centreOnTime)
    {
	    super.zoomInDateAxis(centreOnTime);
	    
		load();
    }


    @Override
    public void zoomOutDateAxis(Date centreOnTime)
    {
	    super.zoomOutDateAxis(centreOnTime);
	    
		load();
    }
    
    
    @Override
    public void addSelectionChangedListener(SelectionChangedListener<IncidentModel> listener)
    {
    	addListener(Events.SelectionChange, listener);
    }
    
    
    @Override
    public void removeSelectionListener(SelectionChangedListener<IncidentModel> listener)
    {
    	removeListener(Events.SelectionChange, listener);
    }


    /**
     * Returns a list of the currently selected incidents in the chart widget.
     * @return list of the currently selected incidents. Note that the only fields
     * 	set in each incident are the description, time and the headline evidence id. 
     * 	If no incident is currently selected, an empty list is returned.
     */
    @Override
    public List<IncidentModel> getSelection()
    {
	    ArrayList<IncidentModel> selected = new ArrayList<IncidentModel>();
	    
	    IncidentModel selectedIncident = getChart().getSelectedIncident();
	    if (selectedIncident != null)
	    {
	    	selected.add(selectedIncident);
	    }
	    
	    return selected;
    }


    @Override
    public void setSelection(List<IncidentModel> selection)
    {
	    // Select the first item in the list.
    	if (selection != null && selection.size() > 0)
    	{
    		IncidentModel prevSelected = getChart().getSelectedIncident();
    		
    		IncidentModel incident = selection.get(0);
    		int idToSelect = incident.getEvidenceId();
    		
    		if (prevSelected == null || (prevSelected.getEvidenceId() != idToSelect) )
    		{
    			getChart().select(idToSelect);
    			fireSelectionChangedEvent(incident);
    		}
    	}
    }
    
    
    /**
     * Fires a SelectionChangedEvent for the specified incident.
     * @param incident incident which has been selected, or
     * 		<code>null</code> to indicate no selection in the chart.
     */
    protected void fireSelectionChangedEvent(IncidentModel incident)
    {
    	SelectionChangedEvent<IncidentModel> se = 
    		new SelectionChangedEvent<IncidentModel>(IncidentGChartWidget.this, incident);
    	
        fireEvent(Events.SelectionChange, se);
    }
    
    
	@Override
    public void setChartWidth(int width)
    {	
    	// Account for y-axis ticks and labels.
		int xChartSize = width - (m_Chart.getYAxis().getTickLabelThickness()+50);
    	if (xChartSize < 100)
    	{
    		xChartSize = 100;
    	}
    	
    	m_Chart.setXChartSize(xChartSize);
    	m_Chart.update();
    }
    
    
    @Override
    public void setChartHeight(int height)
    {
    	// Leave space for the ticks, tick labels, plus some padding. 
    	int xLabelThickness = m_Chart.getXAxis().getTickLength() + 
    				(2*m_Chart.getXAxis().getTickLabelThickness()) + 10;
    	
       	int yChartSize = height - xLabelThickness;
    	if (yChartSize < 100)
    	{
    		yChartSize = 100;
    	}
    	
    	// Alter the y axis max to keep the ratio of model units : y pixels 
    	// the same so that the spacing of incidents remains the same.
    	double yModelToPixelRatio = (m_Chart.getYAxis().getAxisMax())/ m_Chart.getYChartSize();
    	m_Chart.getYAxis().setAxisMax(yModelToPixelRatio * yChartSize);
    	
    	m_Chart.getYAxis().clearTicks();
    	for (int i = 0; i <= m_Chart.getYAxis().getAxisMax(); i +=25 )
    	{
    		m_Chart.getYAxis().addTick(i);
    	}
    	
    	m_Chart.setYChartSize(yChartSize);
    	m_Chart.update();
    }
    

    @Override
    public void setDateRange(Date startTime, Date endTime)
    {
	    // Override GChartWidget.setDateRange() to adjust the number of
    	// gridlines according to the time span of the chart.
    	
	    super.setDateRange(startTime, endTime);
	    addTicksToChart(startTime, endTime);
    }
    
    
    /**
     * Add ticks and tick labels at a spacing dependent on the time span of
     * the chart.
     * @param startTime start time for the chart.
	 * @param endTime end time for the chart.
     */
    protected void addTicksToChart(Date startTime, Date endTime)
    {
		// Zoom levels are:
		// 4 weeks - 40320 mins
		// 2 weeks - 20160 mins
		// 1 week - 10080 mins
		// 4 days - 5760 mins
		// 2 days - 2880 mins
		// 1 day - 1440 mins
		// 12 hours - 720 mins
		// 6 hours - 360 mins
		// 3 hours - 180 mins
		// 1 hour - 60 mins
		// 30 mins
		// 15 mins
	    double xAxisSpan = m_Chart.getDateAxisSpan()/60000;	// Time span in minutes.
		int spacingMins = 60;
		int ticksPerLabel = 1;
		if (xAxisSpan <= 15)
		{
			spacingMins = 5;
		}
		else if (xAxisSpan <= 30)
		{
			spacingMins = 5;
		}
		else if (xAxisSpan <= 60)
		{
			spacingMins = 5;
		}
		else if (xAxisSpan <= 180)
		{
			spacingMins = 60;
		}
		else if (xAxisSpan <= 360)
		{
			spacingMins = 60;
		}
		else if (xAxisSpan <= 720)
		{
			spacingMins = 60;
			ticksPerLabel = 4;
		}
		else if (xAxisSpan <= 1440)
		{
			spacingMins = 60;
			ticksPerLabel = 4;
		}
		else if (xAxisSpan <= 2880)
		{
			spacingMins = 1440;
		}
		else if (xAxisSpan <= 5760)
		{
			spacingMins = 1440;
		}
		else if (xAxisSpan <= 10080)
		{
			spacingMins = 1440;
		}
		else if (xAxisSpan <= 20160)
		{
			spacingMins = 1440;
			ticksPerLabel = 2;
		}
		else
		{
			// 4 weeks - 40320 mins.
			spacingMins = 1440;
			ticksPerLabel = 7;
		}
    	
    	Axis xAxis = m_Chart.getXAxis();
    	xAxis.clearTicks();
    	
    	// Calculate the position of the first tick,
    	// e.g. start time of 10:42 for hourly spacing, first tick will be at 11:00.
    	long startTimeMs = startTime.getTime();
    	long endTimeMs = endTime.getTime();
    	long intervalMs = spacingMins*60*1000;
    	
    	long multiple = startTimeMs / intervalMs;
    	long mod = startTimeMs % intervalMs;
    	if (mod > 0)
    	{
    		multiple++;
    	}
    	
    	long tickMs = multiple * spacingMins * 60000;
    	
    	// Add the ticks and tick labels.
    	int counter = 0;
    	String tickLabel;
    	while (tickMs <= endTimeMs)
    	{
    		if (counter % ticksPerLabel == 0)
    		{
    			tickLabel = xAxis.formatAsTickLabel(tickMs);
    		}
    		else
    		{
    			tickLabel = null;
    		}
    		
    		xAxis.addTick(tickMs, tickLabel, GChart.NAI, GChart.NAI);
    		tickMs += (spacingMins * 60000);
    		counter++;
    	}
    }


	/**
 	 * Response handler for incident queries. 
 	 * The handler makes the load request, then updates the chart with the new
 	 * incident data.
     */
    class IncidentQueryCallback extends ApplicationResponseHandler<List<IncidentModel>>
    {
		@Override
        public void uponFailure(Throwable caught)
        {
			clearTimeMarker();
			removeAllIncidents();
			getChart().update();
			
			GWT.log("Error loading incidents for timeline.", caught);
			
			LoadEvent evt = new LoadEvent(IncidentGChartWidget.this, 
					new Integer(m_AnomalyThreshold), caught);
		    fireEvent(LoadException, evt);
			
			MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
					ClientUtil.CLIENT_CONSTANTS.errorLoadingActivityData(), null);
        }

		@Override
        public void uponSuccess(List<IncidentModel> incidents)
        {
	        GWT.log("Loaded " + incidents.size() + " incidents into chart.");
	        
	        // Store current selection and reselect if incident is in new data set.
	        int prevSelectedId = getChart().getSelectedEvidenceId();
	        
	        clearTimeMarker();
	        removeAllIncidents();
	        
	        for (IncidentModel incident : incidents)
	        {
	        	addIncident(incident);
	        }

	        if (prevSelectedId != 0)
	        {
	        	getChart().select(prevSelectedId);
	        	
	        	// Check if the previous selected incident is still on the timeline.
	        	if (getChart().getSelectedEvidenceId() == 0)
	        	{
	        		fireSelectionChangedEvent(null);
	        	}
	        }
	        
	        // Add time marker at current time if it is in the chart range.
	        Date chartStartTime = m_Chart.getDateAxisStart();
	        Date chartEndTime = m_Chart.getDateAxisEnd();
	        Date now = new Date();
	        if (now.after(chartStartTime) && now.before(chartEndTime))
	        {
	        	m_Chart.setTimeMarker(now);
	        }
	        
	        m_Chart.update();
	        
	        if (m_StartTime == null && incidents.size() > 0)
			{
	        	m_StartTime = chartStartTime;
	        	m_EndTime = chartEndTime;
			}
	        
	        LoadEvent evt = new LoadEvent(
	        		IncidentGChartWidget.this, new Integer(m_AnomalyThreshold));
		    fireEvent(Load, evt);
        }
    	
    }
  
}
