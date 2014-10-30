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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.google.gwt.core.client.GWT;
import com.googlecode.gchart.client.GChart.TouchedPointUpdateOption;

import com.prelert.client.CSSColorChart;
import com.prelert.client.ClientMessages;
import com.prelert.client.CSSSymbolChart.Shape;
import com.prelert.client.ClientUtil;
import com.prelert.client.event.ChartWidgetEvent;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.RequestViewEvent;
import com.prelert.client.list.AttributeListDialog;
import com.prelert.data.Attribute;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.TimeFrame;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.gxt.AttributeModel;
import com.prelert.data.gxt.CausalityDataModel;
import com.prelert.data.gxt.ProbableCauseModel;
import com.prelert.data.gxt.ProbableCauseModelCollection;


/**
 * Implementation of the CausalityChartWidget which uses the GChart Google
 * Web Toolkit (GWT) extension for the charting component 
 * (see {@link http://code.google.com/p/clientsidegchart/}).
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>BeforeLoad</b> : LoadEvent(loader, config)<br>
 * <div>Fires before notification or time series data is loaded into the chart.
 * <ul>
 * <li>loader : this</li>
 * <li>config : object representing data that is about to be loaded</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>Load</b> : LoadEvent(loader, config, result)<br>
 * <div>Fires after notification or time series data is loaded into the chart.</div>
 * <ul>
 * <li>loader : this</li>
 * <li>config : object representing data that was loaded</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>OpenNotificationViewClick</b> : RequestViewEvent<br>
 * <div>Fires after a link to a notification view is selected.</div>
 * <ul>
 * <li>component : this</li>
 * <li>model: CausalityDataModel for notification data from which event was fired</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>OpenTimeSeriesViewClick</b> : RequestViewEvent<br>
 * <div>Fires after a link to a time series view is selected.</div>
 * <ul>
 * <li>component : this</li>
 * <li>model: TimeSeriesConfig of time series from which event was fired</li>
 * </ul>
 * </dd>
 * 
 * </dl>
 * 
 * @author Pete Harverson
 */
public class CausalityGChartWidget extends TimeSeriesGChartWidget
	implements CausalityChartWidget
{

	/** Full height value for the causality chart. */
	public static final double			DEFAULT_FULL_SCALE = 1000d;
	
	private Map<Integer, Double> 		m_PeakValuesByTypeId;
	private HashMap<String, String>		m_NotificationColours;	// Description vs hex colour.
	
	private List<CausalityDataModel>	m_CausalityData;
	
	
	/**
	 * Creates a new causality chart widget which uses a GChart GWT component
	 * for plotting the causality data.
	 */
	public CausalityGChartWidget()
	{
		this(true);
	}
	
	
	/**
	 * Creates a new causality chart widget which uses a GChart GWT component
	 * for plotting the causality data.
	 * @param hideMenuItem <code>true</code> (the default) to add a menu item 
	 * 	to hide items on the chart.
	 */
	public CausalityGChartWidget(boolean hideMenuItem)
	{
		super(new CausalityGChart());
		
		m_NotificationColours = new HashMap<String, String>();
		m_CausalityData = new ArrayList<CausalityDataModel>();
		
        setValueTickLabelsVisible(false);
        setValueRange(0, calculateYAxisMax(DEFAULT_FULL_SCALE));	// Allow for width of plot line.
        
        if (hideMenuItem == true)
        {
        	MenuItem hideItem = new MenuItem(ClientUtil.CLIENT_CONSTANTS.hide());
    	    hideItem.addSelectionListener(new SelectionListener<MenuEvent>(){

    			@Override
                public void componentSelected(MenuEvent ce)
                {
    				CausalityDataModel causalityData = getSelectedNotification();
    	            if (causalityData == null)
    				{	
    					TimeSeriesConfig timeSeries = getSelectedTimeSeries();
    					if (timeSeries != null)
    					{
    						causalityData = new CausalityDataModel();
    						causalityData.setDataSourceType(new DataSourceType(timeSeries.getDataType(),
    								DataSourceCategory.TIME_SERIES_FEATURE));
    						causalityData.setDescription(timeSeries.getMetric());
    						causalityData.setSource(timeSeries.getSource());
    						causalityData.setAttributes(timeSeries.getAttributes());
    						causalityData.setTimeSeriesId(timeSeries.getTimeSeriesId());
    					}
    				}
    	            
    	            if (causalityData != null)
    	            {
    	            	removeCausalityData(causalityData);
    	            }
                }
    	    	
    	    });
    	    
    	    getChart().getContextMenu().insert(hideItem, 2);
        }

	}
	

	/**
	 * Initialises the right-click context menu, adding items specific to causality charts.
	 */
    @Override
    protected void initContextMenu()
    {
    	super.initContextMenu();
    	
    	// Add 'Show Data' and 'Remove' items at the top of the 
    	// existing menu (which `will already contain zoom in and zoom out items).
	    Menu menu = getChart().getContextMenu();
	    
	    MenuItem showDataItem = new MenuItem(ClientUtil.CLIENT_CONSTANTS.showData());
		SelectionListener<MenuEvent> showDataListener = new SelectionListener<MenuEvent>()
	    {
			@Override
            public void componentSelected(MenuEvent ce)
            {
				CausalityDataModel notification = getSelectedNotification();
				if (notification != null)
				{	
					RequestViewEvent<CausalityDataModel> rve = 
						new RequestViewEvent<CausalityDataModel>(m_Chart);
					rve.setViewToOpenDataType(notification.getDataSourceType());
					rve.setSourceName(notification.getSource());	
					rve.setModel(notification);	
					fireEvent(GXTEvents.OpenNotificationViewClick, rve);
				}
				else 
				{
					TimeSeriesConfig timeSeries = getSelectedTimeSeries();
					if (timeSeries != null)
					{
						DataSourceType dsType = new DataSourceType(
								timeSeries.getDataType(), DataSourceCategory.TIME_SERIES);
						
						RequestViewEvent<TimeSeriesConfig> rve = 
							new RequestViewEvent<TimeSeriesConfig>(m_Chart);
						rve.setViewToOpenDataType(dsType);
						rve.setOpenAtTime(getSelectedTime());
						rve.setSourceName(timeSeries.getSource());
						rve.setModel(timeSeries);
						fireEvent(GXTEvents.OpenTimeSeriesViewClick, rve);
					}
				}
			}
			
	    };
	    showDataItem.addSelectionListener(showDataListener);

	    menu.remove(menu.getItem(0));
	    menu.insert(showDataItem, 1);  
    }


	@Override
    public void addNotifications(ProbableCauseModelCollection collection)
    {
		fireEvent(BeforeLoad, new LoadEvent(this, collection));
		
		
		// Get the line colour for the notification, based on description.
		String lineColor = m_NotificationColours.get(collection.getDescription());
		if (lineColor == null)
		{
    		// Find the next available line colour from the chart.
    		CSSColorChart colorChart = CSSColorChart.getInstance();
    		int numColoursInChart = colorChart.getNumberOfColors();
    		for (int i = 0; i < numColoursInChart; i++)
    		{
    			lineColor = colorChart.getColor(i);
    			if (m_NotificationColours.containsValue(lineColor) == false)
    			{
    				break;
    			}
    		}
    		
    		m_NotificationColours.put(collection.getDescription(), lineColor);
		}
		
		getChart().addNotificationCollection(collection, lineColor, Shape.DIAMOND);
		getChart().update();
		
		LoadEvent evt = new LoadEvent(CausalityGChartWidget.this, collection, collection);
	    fireEvent(Load, evt);
    }
	

	@Override
    public void removeNotifications(ProbableCauseModelCollection collection)
    {
		getChart().removeNotificationCollection(collection);
    }
	

    @Override
    public void addTimeSeries(ProbableCauseModelCollection collection,
    		ProbableCauseModel probCause)
    {
		if (probCause.getDisplay() == true)
		{
			List<Attribute> attributes = probCause.getAttributes();
			
			TimeSeriesConfig timeSeriesConfig = new TimeSeriesConfig(
					probCause.getDataSourceName(),
					probCause.getMetric(),
					probCause.getSource(),
					attributes);
			
			timeSeriesConfig.setDescription(probCause.getDescription());
			timeSeriesConfig.setAttributeLabel(probCause.getAttributeLabel());
			timeSeriesConfig.setScalingFactor(probCause.getScalingFactor() * DEFAULT_FULL_SCALE);
			
			// Obtain the colour to use for this time series in the chart.
			// IDs for time series ProbableCauseModelCollection are generated
			// sequentially to map to a unique color chart index.
			int index = collection.getId();
			String color = CSSColorChart.getInstance().getColor(index);
			addTimeSeries(timeSeriesConfig, color);
			load(timeSeriesConfig);
		}
    }


    @Override
    public void removeTimeSeries(ProbableCauseModelCollection collection,
    		ProbableCauseModel probCause)
    {
    	List<Attribute> attributes = probCause.getAttributes();
		
		TimeSeriesConfig timeSeriesConfig = new TimeSeriesConfig(
				probCause.getDataSourceName(),
				probCause.getMetric(),
				probCause.getSource(),
				attributes);

		timeSeriesConfig.setDescription(probCause.getDescription());
		timeSeriesConfig.setAttributeLabel(probCause.getAttributeLabel());
		removeTimeSeries(timeSeriesConfig);
    }
    

    @Override
    public void removeAll()
    {
    	ArrayList<CausalityDataModel> copyList = new ArrayList<CausalityDataModel>(m_CausalityData);
    	for (CausalityDataModel causalityData : copyList)
    	{
    		removeCausalityData(causalityData);
    	}
	    
	    setValueRange(0, calculateYAxisMax(DEFAULT_FULL_SCALE));	// Allow for width of plot line.
    }
    
    
    @Override
    public void setPeakValuesByTypeId(Map<Integer, Double> peakValuesByTypeId)
    {
    	m_PeakValuesByTypeId = peakValuesByTypeId;
    }


	@Override
    public void addCausalityData(CausalityDataModel causalityData)
    {
    	GWT.log("CausalityGChartWidget - addCausalityData(): " + causalityData);
    	
    	// TODO : Only add items which aren't already on the chart.
		if (causalityData.getDataSourceCategory() == DataSourceCategory.NOTIFICATION)
		{
			fireEvent(BeforeLoad, new LoadEvent(this, causalityData));
			
			// Get the line colour for the notification, based on description.
			String lineColor = getLineColour(causalityData);
			if (lineColor == null)
			{
	    		// Find the next available line colour from the chart.
	    		CSSColorChart colorChart = CSSColorChart.getInstance();
	    		int numColoursInChart = colorChart.getNumberOfColors();
	    		for (int i = 0; i < numColoursInChart; i++)
	    		{
	    			lineColor = colorChart.getColor(i);
	    			if (m_NotificationColours.containsValue(lineColor) == false)
	    			{
	    				break;
	    			}
	    		}
	    		
	    		m_NotificationColours.put(causalityData.getDescription(), lineColor);
			}
			
			// TODO - do we want other symbol shapes?
			getChart().addNotifications(causalityData, lineColor, Shape.DIAMOND);
			getChart().update();
			
			LoadEvent evt = new LoadEvent(this, causalityData);
		    fireEvent(Load, evt);
		}
		else if (causalityData.getDataSourceCategory() == DataSourceCategory.TIME_SERIES_FEATURE)
		{
			final TimeSeriesConfig timeSeriesConfig = new TimeSeriesConfig();
			timeSeriesConfig.setDataType(causalityData.getDataSourceName());
			timeSeriesConfig.setMetric(causalityData.getDescription());
			timeSeriesConfig.setSource(causalityData.getSource());
			timeSeriesConfig.setAttributes(causalityData.getAttributes());
			timeSeriesConfig.setScalingFactor(causalityData.getScalingFactor());
			timeSeriesConfig.setTimeSeriesId(causalityData.getTimeSeriesId());

			double seriesScalingFactor = timeSeriesConfig.getScalingFactor() * DEFAULT_FULL_SCALE;
			
			Double peakValueForType = m_PeakValuesByTypeId.get(causalityData.getTimeSeriesTypeId());
			double peakValueForTypeVal = 0;
			if (peakValueForType != null)
			{
				peakValueForTypeVal = peakValueForType.doubleValue();
				GWT.log("get peak value for " + timeSeriesConfig + "=" + peakValueForTypeVal);
			}
			
			// Peak value for type may be 0 as the back-end uses the time of the 
			// 'headline' feature or notification when calculating the peak value 
			// for a probable cause, so the feature may be outside of the window 
			// which has the incident time at its centre.
			if (peakValueForTypeVal != 0d)
			{
				timeSeriesConfig.setScalingFactor(seriesScalingFactor/peakValueForTypeVal);
			}
			
			addTimeSeries(timeSeriesConfig);
			load(timeSeriesConfig);
		}
		
		m_CausalityData.add(causalityData);
    }


    @Override
    public void removeCausalityData(CausalityDataModel causalityData)
    {
    	GWT.log("CausalityGChartWidget - removeCausalityData(): " + causalityData);
		
		if (causalityData.getDataSourceCategory() == DataSourceCategory.NOTIFICATION)
		{
			// TODO - only remove colour if no other notifications with same description.
			m_NotificationColours.remove(causalityData.getDescription());
			getChart().removeNotifications(causalityData);
		}
		else if (causalityData.getDataSourceCategory() == DataSourceCategory.TIME_SERIES_FEATURE)
		{
			TimeSeriesConfig timeSeriesConfig = new TimeSeriesConfig();
			timeSeriesConfig.setDataType(causalityData.getDataSourceName());
			timeSeriesConfig.setMetric(causalityData.getDescription());
			timeSeriesConfig.setSource(causalityData.getSource());
			timeSeriesConfig.setAttributes(causalityData.getAttributes());
			timeSeriesConfig.setTimeSeriesId(causalityData.getTimeSeriesId());
			
			removeTimeSeries(timeSeriesConfig);
		}
		
		getChart().update(TouchedPointUpdateOption.TOUCHED_POINT_CLEARED);
		
		// Remove from store of chart data.
		CausalityDataModel modelToRemove = null;
		for (CausalityDataModel data : m_CausalityData)
		{
			if (data.equalsIgnoreMetrics(causalityData) == true)
			{
				modelToRemove = data;
				break;
			}
		}
		
		if (modelToRemove != null)
		{
			m_CausalityData.remove(modelToRemove);
		}
		
		// Fire a Remove event.
		ChartWidgetEvent<CausalityDataModel> cwe = 
        	new ChartWidgetEvent<CausalityDataModel>(this, Events.Remove);
		cwe.setModel(causalityData);
        fireEvent(Events.Remove, cwe);
    }


	/**
     * Returns the notification that is currently 'selected' in the chart e.g. when
     * a context menu item has been run against a notification type probable cause.
     * Note that only the key fields in the model will be set (id, data type, source, time).
     * @return the notification that is selected, or <code>null</code> if no 
     * 		notification is currently selected.
     */
    public CausalityDataModel getSelectedNotification()
    {
	    return getChart().getTouchedNotification();
    }
    
    
    @Override
    public List<CausalityDataModel> getCausalityData()
    {
	    return m_CausalityData;
    }


	@Override
    public boolean isDisplayedOnChart(CausalityDataModel causalityData)
    {
    	boolean onChart = false;
    	DataSourceCategory category = causalityData.getDataSourceCategory();
	    if (category == DataSourceCategory.NOTIFICATION)
	    {
	    	onChart = getChart().isNotificationOnChart(causalityData);
	    }
	    else
	    {
	    	onChart = (getLineColour(causalityData) != null);
	    }
	    
	    return onChart;
    }
    
    
    @Override
    public String getLineColour(ProbableCauseModelCollection collection)
    {
    	// TODO - this can be removed once CausalityViewWidget is removed.
    	
    	String lineColor = null;
	    DataSourceCategory category = collection.getDataSourceCategory();
	    if (category == DataSourceCategory.NOTIFICATION)
	    {
	    	lineColor = m_NotificationColours.get(collection.getDescription());
	    }
	    else
	    {
	    	int index = collection.getId();
	    	lineColor = CSSColorChart.getInstance().getColor(index);
	    }
	    
	    return lineColor;
    }


	@Override
    public String getLineColour(CausalityDataModel causalityData)
    {
	    String lineColor = null;
	    DataSourceCategory category = causalityData.getDataSourceCategory();
	    if (category == DataSourceCategory.NOTIFICATION)
	    {
	    	lineColor = m_NotificationColours.get(causalityData.getDescription());
	    }
	    else
	    {
	    	TimeSeriesConfig timeSeriesConfig = new TimeSeriesConfig();
			timeSeriesConfig.setDataType(causalityData.getDataSourceName());
			timeSeriesConfig.setMetric(causalityData.getDescription());
			timeSeriesConfig.setSource(causalityData.getSource());
			timeSeriesConfig.setAttributes(causalityData.getAttributes());
			timeSeriesConfig.setTimeSeriesId(causalityData.getTimeSeriesId());
			
			lineColor = getLineColour(timeSeriesConfig);
	    }
	    
	    return lineColor;
    }
    

    @Override
    public Shape getSymbolShape(CausalityDataModel causalityData)
    {
	    Shape shape = null;
	    
	    DataSourceCategory category = causalityData.getDataSourceCategory();
	    if (category == DataSourceCategory.NOTIFICATION)
	    {
	    	shape = Shape.DIAMOND;
	    }
	    
	    return shape;
    }


	/**
	 * Returns the CausalityGChart component which is displaying the causality data.
	 * @return the CausalityGChart GWT widget.
	 */
    @Override
    public CausalityGChart getChart()
    {
	    return (CausalityGChart)m_Chart;
    }
    
    
    @Override
    public void zoomInDateAxis(Date centreOnTime)
    {
	    super.zoomInDateAxis(centreOnTime);
	    
	    if (m_NotificationColours.size() > 0)
	    {
	    	getChart().positionNotificationsX();
	    	getChart().positionNotificationsY();
	    	getChart().update();
	    }
    }


    @Override
    public void zoomOutDateAxis(Date centreOnTime)
    {
	    super.zoomOutDateAxis(centreOnTime);
	    
	    if (m_NotificationColours.size() > 0)
	    {
	    	getChart().positionNotificationsX();
	    	getChart().positionNotificationsY();
	    	getChart().update();
	    }
    }


    @Override
    public void scaleToFit()
    {
    	double maxTimeSeriesVal = getChart().getMaxTimeSeriesValue();
    	if (maxTimeSeriesVal > 0)
    	{
    		double maxAxisVal = calculateYAxisMax(maxTimeSeriesVal);
    		
    		double currentYAxisMax = m_Chart.getYAxis().getAxisMax();
    		setValueRange(0, maxAxisVal); 
    		
    		if (maxTimeSeriesVal > currentYAxisMax)
    		{
    			// Reload the raw data points, rather than just rescaling series 
    			// which may contain clipped points.
    			getChart().removeAllTimeSeries();
    			load();
    		}
    		else
    		{
    			getChart().update();
    		}
    	}
    }
    
    
    @Override
	protected void showDetailsOnSelectedSeries()
	{
    	CausalityDataModel notification = getSelectedNotification();
		if (notification != null)
		{	
			ClientMessages messages = ClientUtil.CLIENT_CONSTANTS;
			
			String desc = notification.getDescription();
			
			ArrayList<AttributeModel> attributes = new ArrayList<AttributeModel>();
			attributes.add(new AttributeModel(messages.type(), notification.getDataSourceName()));
			attributes.add(new AttributeModel(messages.description(), notification.getDescription()));
			
			attributes.add(new AttributeModel(messages.startTime(), 
					ClientUtil.formatTimeField(notification.getStartTime(), TimeFrame.SECOND)));
			attributes.add(new AttributeModel(messages.endTime(), 
					ClientUtil.formatTimeField(notification.getEndTime(), TimeFrame.SECOND)));

			attributes.add(new AttributeModel(messages.count(), 
					Integer.toString(notification.getCount())));
			
			attributes.add(new AttributeModel(messages.source(), notification.getSource()));
	    	
			List<Attribute> otherAttr = notification.getAttributes();
	    	if (otherAttr != null)
	    	{
	    		for (Attribute attribute : otherAttr)
	    		{
	    			attributes.add(new AttributeModel(
	    					attribute.getAttributeName(), attribute.getAttributeValue()));
	    		}
	    	}
	    	
	    	AttributeListDialog dialog = AttributeListDialog.getInstance();
			dialog.setHeading(messages.detailsOnData(desc));
			dialog.setAttributes(attributes);
			dialog.show();
			dialog.toFront();
		}
		else 
		{
			super.showDetailsOnSelectedSeries();
		}
	}


	/**
     * Calculates the maximum y axis value to set for the specified maximum data
     * value, taking into account the thickness of the lines used to render time series.
     * @param maxDataValue maximum raw data value.
     * @return maximum value to set for the chart's y-axis so that the full thickness
     * 	of the line plot is visible.
     */
    protected double calculateYAxisMax(double maxDataValue)
    {
    	double chartHeightPixels = m_Chart.getYChartSize();
    	return (maxDataValue)/((chartHeightPixels - TimeSeriesGChart.TIME_SERIES_LINE_THICKNESS)/chartHeightPixels);
    }


    @Override
    public double getYAxisScalingFactor()
    {
    	// Add two pixels worth of value to allow for the width of the plot lines.
		double defaultAxisMax = calculateYAxisMax(DEFAULT_FULL_SCALE);
		double currentAxisMax = m_Chart.getYAxis().getAxisMax();

	    return defaultAxisMax/currentAxisMax;
    }

}
