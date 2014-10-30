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

package com.prelert.client.chart;

import java.util.List;

import com.extjs.gxt.ui.client.data.LoadEvent;
import com.prelert.client.CSSColorChart;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.RequestViewEvent;
import com.prelert.client.gxt.ViewMenuItem;
import com.prelert.data.Attribute;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.TimeFrame;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.Tool;
import com.prelert.data.ViewTool;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.data.gxt.ProbableCauseModel;
import com.prelert.data.gxt.ProbableCauseModelCollection;


/**
 * Implementation of the CausalityChartWidget which uses the GChart Google
 * Web Toolkit (GWT) extension for the charting component 
 * (see {@link http://code.google.com/p/clientsidegchart/}).
 * @author Pete Harverson
 */
public class CausalityGChartWidget extends TimeSeriesGChartWidget
	implements CausalityChartWidget
{

	/** Full height value for the causality chart. */
	public static final double			CHART_VALUE_RANGE = 1000d;
	
	
	/**
	 * Creates a new causality chart widget which uses a GChart GWT component
	 * for plotting the causality data.
	 */
	public CausalityGChartWidget()
	{
		super(new CausalityGChart());
		
        setValueTickLabelsVisible(false);
        setAutoValueRange(false);
        setValueRange(0, CHART_VALUE_RANGE + 2);	// Allow for width of plot line.
	}
	
	
	@Override
    public void addNotifications(ProbableCauseModelCollection collection)
    {
		fireEvent(BeforeLoad, new LoadEvent(this, collection));
		
		getChart().addNotificationCollection(collection);
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
			timeSeriesConfig.setScalingFactor(probCause.getScalingFactor() * CHART_VALUE_RANGE);
			
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
    public EvidenceModel getSelectedNotification()
    {
	    EvidenceModel notification = null;
	    
	    ProbableCauseModel touchedNotification = getChart().getTouchedNotification();
	    if (touchedNotification != null)
	    {
	    	notification = new EvidenceModel();
	    	notification.setId(touchedNotification.getEvidenceId());
	    	notification.setDescription(touchedNotification.getDescription());
	    	notification.setSource(touchedNotification.getSource());
	    	notification.setDataType(touchedNotification.getDataSourceName());
	    	notification.setTime(TimeFrame.SECOND, touchedNotification.getTime());
	    }
	    
	    return notification;
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
    @SuppressWarnings("unchecked")
    protected <M> void fireViewMenuItemEvent(ViewMenuItem viewMenuItem)
    {
    	Tool tool = viewMenuItem.getTool();
    	
    	if (tool.getClass() == ViewTool.class)
		{
    		RequestViewEvent<M> rve = new RequestViewEvent<M>(m_Chart);
    		
			EvidenceModel notification = getSelectedNotification();
			if (notification != null)
			{
				DataSourceType dsType = new DataSourceType(
						notification.getDataType(), DataSourceCategory.NOTIFICATION);
				
				rve.setViewToOpenDataType(dsType);
				rve.setSourceName(notification.getSource());	
				rve.setModel((M) notification);	
				fireEvent(GXTEvents.OpenNotificationViewClick, rve);
			}
			else 
			{
				TimeSeriesConfig timeSeries = getSelectedTimeSeries();
				if (timeSeries != null)
				{
					DataSourceType dsType = new DataSourceType(
							timeSeries.getDataType(), DataSourceCategory.TIME_SERIES);
					rve.setViewToOpenDataType(dsType);
					rve.setOpenAtTime(getSelectedTime());
					rve.setSourceName(timeSeries.getSource());
					rve.setModel((M) timeSeries);
					fireEvent(GXTEvents.OpenTimeSeriesViewClick, rve);
				}
			}
		}
    }


}
