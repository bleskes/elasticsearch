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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.HoverParameterInterpreter;
import com.googlecode.gchart.client.GChart.Curve.Point;

import com.prelert.client.ClientMessages;
import com.prelert.client.ClientUtil;
import com.prelert.client.CSSSeverityColors;
import com.prelert.data.Attribute;
import com.prelert.data.Evidence;
import com.prelert.data.Severity;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesData;
import com.prelert.data.TimeSeriesDataPoint;


/**
 * Extension of the time-based chart for displaying time series
 * and notification data.
 * @author Pete Harverson
 */
public class TimeSeriesGChart extends TimeAxisGChart
{
	private HashMap<TimeSeriesConfig, Curve>	m_TimeSeriesCurves; 
	private HashMap<TimeSeriesConfig, List<GChartFeatureCurve>>	m_FeaturesCurves; 
	
	private HashMap<Integer, Curve>	m_NotificationCurves; // Curve vs evidence id.
	
	protected static final int NOTIFICATION_SYMBOL_LENGTH = 10;

	
	/**
	 * Creates a new chart for displaying time series data.
	 */
	public TimeSeriesGChart()
	{
		// Set up the template for displaying tooltips on points.
		setHoverParameterInterpreter(new ChartHoverParameterInterpreter());

		// Configure the value y-axis for displaying numerical values.
	    getYAxis().setTickLabelFormat("#,##0");
	    getYAxis().setTickLabelThickness(100);
	}
	
	
	/**
	 * Adds a time series to the chart.
	 * @param dataPoints the data points in the time series.
	 * @param lineColour CSS colour literal for the colour of the line.
	 */
	public void addTimeSeries(TimeSeriesData timeSeriesData, String lineColour)
	{
		if (m_TimeSeriesCurves == null)
		{
			m_TimeSeriesCurves = new HashMap<TimeSeriesConfig, Curve>();
		}
		
		TimeSeriesConfig config = timeSeriesData.getConfig();	
		
		// Make sure that the chart doesn't contain this TimeSeriesConfig already.
		removeTimeSeries(config);
		
		// Keep the timeline as the curve with the highest index.
		int indexToAdd = getNCurves();
		if (m_TimeLine != null)
		{
			indexToAdd--;
		}
		
		// Create the new curve.
		addCurve(indexToAdd);
		Curve curve = getCurve(indexToAdd);
		Symbol symbol = curve.getSymbol();
		curve.setYAxis(Y_AXIS);
		symbol.setBorderColor(lineColour);
		symbol.setBackgroundColor(lineColour);
		symbol.setSymbolType(SymbolType.LINE);
		symbol.setFillThickness(2);
		symbol.setFillSpacing(0);
		symbol.setHoverWidget(new CentringHoverAnnotation());
		symbol.setHovertextTemplate(getHovertextTemplate(config));
		
		// Removes the symbols themselves, leaving only the line.
		symbol.setHeight(0);
		symbol.setWidth(0);
		
		// Set up properties for hover selection for tooltip display.
		symbol.setBrushSize(30, 30);
		symbol.setHoverSelectionWidth(5);
		symbol.setHoverSelectionHeight(5);
		symbol.setHoverSelectionBackgroundColor(lineColour);
		symbol.setHoverSelectionBorderColor(lineColour);
		
	    // Add the dataset.
	    List<TimeSeriesDataPoint> dataPoints = timeSeriesData.getDataPoints();
	    
		for (TimeSeriesDataPoint dataPoint : dataPoints)
		{
			curve.addPoint(dataPoint.getTime(), dataPoint.getValue());
			
			if (dataPoint.getFeature() != null)
			{
				addTimeSeriesFeature(config, dataPoint);
			}
		}
		
		m_TimeSeriesCurves.put(config, curve);
		
		if (isAutoValueRange() == true)
		{
			if (m_TimeLine != null)
			{
				m_TimeLine.getPoint().setY(0);
			}
			
			double yAxisMax = calculateYAxisMax();
			getYAxis().setAxisMax(yAxisMax);
			
			// If a timeline has been added, make it full height.
			if (m_TimeLine != null)
			{
				m_TimeLine.getPoint().setY(yAxisMax);
			}
		}
		
		
	}
	
	
	/**
	 * Marks a feature in a time series by adding a new curve with a single point at the
	 * time of the discord.
	 * @param timeSeriesConfig configuration data for the time series containing the feature.
	 * @param dataPoint the time series data point mapped to the feature. 
	 */
	protected void addTimeSeriesFeature(TimeSeriesConfig timeSeriesConfig, 
			TimeSeriesDataPoint dataPoint)
	{
		if (m_FeaturesCurves == null)
		{
			m_FeaturesCurves = new HashMap<TimeSeriesConfig, List<GChartFeatureCurve>>();
		}
		
		Evidence feature = dataPoint.getFeature();
		
		// Add discords in front of time series curve itself so that the feature
		// shows up as the touched curve for mouse over/click events.
		int indexToAdd = getNCurves();
		if (m_TimeLine != null)
		{
			indexToAdd--;
		}
		
		// Create the new curve.
		addCurve(indexToAdd);
		Curve curve = getCurve(indexToAdd);
		Symbol symbol = curve.getSymbol();
		symbol.setSymbolType(SymbolType.BOX_CENTER);
		symbol.setWidth(NOTIFICATION_SYMBOL_LENGTH);
		symbol.setHeight(NOTIFICATION_SYMBOL_LENGTH);
		symbol.setBorderWidth(0);
		symbol.setBackgroundColor(CSSSeverityColors.getColor(feature.getSeverity()));
		String hoverTemplate = GChart.formatAsHovertext("<b>${fullTime}</b><br>" + feature.getDescription());
		symbol.setHoverWidget(new CentringHoverAnnotation());
		symbol.setHovertextTemplate(hoverTemplate);
		
		curve.addPoint(dataPoint.getTime(), dataPoint.getValue());
		
		List<GChartFeatureCurve> curveList = m_FeaturesCurves.get(timeSeriesConfig);
		if (curveList == null)
		{
			curveList = new ArrayList<GChartFeatureCurve>();
			m_FeaturesCurves.put(timeSeriesConfig, curveList);
		}
		
		curveList.add(new GChartFeatureCurve(feature.getId(), curve));
		
	}
	
	
	/**
	 * Removes the time series from the chart.
	 * @param timeSeriesConfig TimeSeriesConfig object defining the properties of
	 * 		the time series to remove (data type, metric, source, attributes).
	 */
	public void removeTimeSeries(TimeSeriesConfig timeSeriesConfig)
	{
		if (m_TimeSeriesCurves != null)
		{
			Curve curve = m_TimeSeriesCurves.remove(timeSeriesConfig);

			if (curve != null)
			{
				removeCurve(curve);
			}
		}
		
		if (m_FeaturesCurves != null)
		{
			List<GChartFeatureCurve> curveList = m_FeaturesCurves.remove(timeSeriesConfig);
			
			if (curveList != null)
			{
				Curve curve;
				for (GChartFeatureCurve featureCurve : curveList)
				{
					curve = featureCurve.getFeatureCurve();
					removeCurve(curve);
				}
			}
		}
	}
	
	
	/**
	 * Adds the specified notification data into the chart. Note that this may 
	 * not represent a single item of evidence but a number of items aggregated by
	 * time and description.
	 * @param notification notification to add to the chart.
	 */
	public void addNotification(Evidence notification)
	{
		if (m_NotificationCurves == null)
		{
			m_NotificationCurves = new HashMap<Integer, Curve>();
		}
		
		Severity severity = notification.getSeverity();
			
		// Keep the timeline as the curve with the highest index.
		int indexToAdd = getNCurves();
		if (m_TimeLine != null)
		{
			indexToAdd--;
		}
		
		
		// Create the new curve.
		addCurve(indexToAdd);
		Curve curve = getCurve(indexToAdd);
		Symbol symbol = curve.getSymbol();
		symbol.setSymbolType(SymbolType.BOX_NORTH);
		symbol.setWidth(NOTIFICATION_SYMBOL_LENGTH);
		symbol.setHeight(NOTIFICATION_SYMBOL_LENGTH);
		symbol.setBorderWidth(0);
		symbol.setBackgroundColor(CSSSeverityColors.getColor(severity));
	//	symbol.setImageURL("images/triangle_critical.png");	// To use images for symbol
		
		String hoverTemplate = GChart.formatAsHovertext(notification.getDescription() + ", ${fullTime}");
		symbol.setHovertextTemplate(hoverTemplate);
		
		double yPos = calculateNotificationY(notification.getTime());
		curve.addPoint(notification.getTime().getTime(), yPos);
		
		m_NotificationCurves.put(notification.getId(), curve);	
		
		if (isAutoValueRange() == true)
		{
			if (m_TimeLine != null)
			{
				m_TimeLine.getPoint().setY(0);
			}
			
			double yAxisMax = calculateYAxisMax();
			getYAxis().setAxisMax(yAxisMax);
			
			// If a timeline has been added, make it full height.
			if (m_TimeLine != null)
			{
				m_TimeLine.getPoint().setY(getYAxis().getAxisMax());
			}
		}

	}
	
	
	/**
	 * Removes the specified notification data into the chart. Note that this may 
	 * not represent a single item of evidence but a number of items aggregated by
	 * time and description.
	 * @param notification notification to remove from the chart.
	 */
	public void removeNotification(Evidence notification)
	{
		if (m_NotificationCurves != null)
		{
			int id = notification.getId();
			Curve curve = m_NotificationCurves.remove(id);

			if (curve != null)
			{
				removeCurve(curve);
			}
		}
	}
	
	
	/**
	 * Removes all time series from the chart.
	 */
	public void removeAllTimeSeries()
	{
		if (m_TimeSeriesCurves != null)
		{				
			Iterator<Curve> curveIter = m_TimeSeriesCurves.values().iterator();
			Curve curve;
			while (curveIter.hasNext())
			{
				curve = curveIter.next();
				removeCurve(curve);
			}
			
			m_TimeSeriesCurves.clear();
		}
		
		
		if (m_FeaturesCurves != null)
		{
			Iterator<List<GChartFeatureCurve>> featureCurvesIter = m_FeaturesCurves.values().iterator();
			List<GChartFeatureCurve> curveList;
			Curve curve;
			while (featureCurvesIter.hasNext())
			{
				curveList = featureCurvesIter.next();
				
				for (GChartFeatureCurve featureCurve : curveList)
				{
					curve = featureCurve.getFeatureCurve();
					if (curve != null)
					{
						removeCurve(curve);
					}
				}
			}
			
			m_FeaturesCurves.clear();
		}
	}
	
	
	/**
	 * Removes all notifications from the chart.
	 */
	public void removeAllNotifications()
	{
		if (m_NotificationCurves != null)
		{
			Iterator<Curve> curveIter = m_NotificationCurves.values().iterator();
			Curve curve;
			while (curveIter.hasNext())
			{
				curve = curveIter.next();
				removeCurve(curve);
			}
			
			m_NotificationCurves.clear();
		}
	}
	
	
	/**
	 * Removes all time series, notifications and the time marker (if set)
	 * from the chart.
	 */
	public void removeAll()
	{
		removeAllNotifications();
		removeAllTimeSeries();
		clearTimeMarker();
		
		if (isAutoValueRange() == true)
		{
			double yAxisMax = calculateYAxisMax();
			getYAxis().setAxisMax(yAxisMax);
		}
	}
	
	
	/**
	 * Returns the ID of the notification that the mouse is currently touching
	 * i.e. the 'hovered over' point.
	 * @return the id of the touched notification, or <code>-1</code> if no 
	 * notification is currently hovered over.
	 */
	public int getTouchedNotificationId()
	{
		int evidenceId = -1;
		
		if (m_NotificationCurves != null)
		{
			Curve touchedCurve = getTouchedCurve();
			if (touchedCurve != null && m_NotificationCurves.containsValue(touchedCurve))
			{
				Iterator<Integer> iter = m_NotificationCurves.keySet().iterator();
				Integer id;
				while (iter.hasNext())
				{
					id = iter.next();
					if (m_NotificationCurves.get(id).equals(touchedCurve))
					{
						evidenceId = id.intValue();
						break;
					}
				}
			}
		}
		
		return evidenceId;
	}
	
	
	/**
	 * Returns the ID of the time series feature that the mouse is currently 
	 * touching i.e. the 'hovered over' point.
	 * @return the id of the touched feature, or <code>-1</code> if no 
	 * time series feature is currently hovered over.
	 */
	public int getTouchedTimeSeriesFeatureId()
	{
		int featureId = -1;
		
		if (m_FeaturesCurves != null)
		{
			Curve touchedCurve = getTouchedCurve();
			if (touchedCurve != null)
			{
				Iterator<List<GChartFeatureCurve>> iter = m_FeaturesCurves.values().iterator();
				List<GChartFeatureCurve> curveList;
				while (iter.hasNext())
				{
					curveList = iter.next();
					for (GChartFeatureCurve featureCurve : curveList)
					{
						if (featureCurve.getFeatureCurve().equals(touchedCurve))
						{
							featureId = featureCurve.getFeatureId();
							return featureId;
						}
					}
				}
			}
		}
		
		return featureId;
	}
	
	
	/**
	 * Returns the time series that the mouse is currently touching
	 * i.e. the 'hovered over' point.
	 * @return TimeSeriesConfig object defining the properties of the time series
	 *  	or <code>null</code> if no time series is currently hovered over.
	 */
	public TimeSeriesConfig getTouchedTimeSeries()
	{
		TimeSeriesConfig config = null;
		
		if (m_TimeSeriesCurves != null)
		{
			Curve touchedCurve = getTouchedCurve();
			if (touchedCurve != null && m_TimeSeriesCurves.containsValue(touchedCurve))
			{
				Iterator<TimeSeriesConfig> iter = m_TimeSeriesCurves.keySet().iterator();
				TimeSeriesConfig timeSeries;
				while (iter.hasNext())
				{
					timeSeries = iter.next();
					if (m_TimeSeriesCurves.get(timeSeries).equals(touchedCurve))
					{
						config = timeSeries;
						break;
					}
				}
			}
		}
		
		return config;
	}
	
	
	/**
	 * Calculates the y-position for a notification at the specified time. 
	 * The calculation takes into account other notification with similar time 
	 * values, so that they do not overlap on the chart.
	 * @param time time of notification.
	 * @return the y-position for the notification.
	 */
	protected double calculateNotificationY(Date time)
	{
		double yPos = 0;

		// Calculate the minimum x value gap between points at which we
		// need to offset the y positions.
		double xPixelToModel = (getXAxis().getAxisMax()- getXAxis().getAxisMin())/ getXChartSize();
		double xGap = xPixelToModel*NOTIFICATION_SYMBOL_LENGTH;
		
		long ms1 = time.getTime();
		long ms2;
		ArrayList<Double> yPositions = new ArrayList<Double>();
		
		Iterator<Curve> curveIter = m_NotificationCurves.values().iterator();
		Curve curve;
		Point point;
		while (curveIter.hasNext())
		{
			curve = curveIter.next();
			point = curve.getPoint();
			
			ms2 = (long)point.getX();
			if (Math.abs(ms2-ms1) <= xGap)
			{
				yPositions.add(point.getY());
			}
		}

		if (yPositions.size() > 0)
		{
			// Calculate the y increment for overlapping times.
			double yPixelToModel = getYAxis().getAxisMax() / getYChartSize();
			double yIncrement = yPixelToModel*(NOTIFICATION_SYMBOL_LENGTH + 3);
			
			boolean foundFreePos = false;
			while (foundFreePos == false)
			{
				if (yPositions.contains(yPos))
				{
					yPos += yIncrement;
				}
				else
				{
					foundFreePos = true;
				}
			}
		}
		
		return yPos;
	}
	
	
	/**
     * Creates the hover text template for the specified TimeSeriesConfig.
     * @param dataSeries config for the time series for which to generate a key.
     * @return a key for the time series.
     */
    protected String getHovertextTemplate(TimeSeriesConfig config)
    {
    	ClientMessages messages = ClientUtil.CLIENT_CONSTANTS;
    	
    	StringBuilder template = new StringBuilder("<b>${fullTime}</b><br>");
    	
    	template.append(messages.type());
    	template.append('=');
    	template.append(config.getDataType());
    	template.append("<br>");
    	
    	template.append(messages.metric());
    	template.append('=');
    	template.append(config.getMetric());
    	template.append("<br>");
    	
    	String source = config.getSource();
    	if (source != null)
    	{
        	template.append(messages.source());
        	template.append('=');
    		template.append(source);
    	}
    	else
    	{
    		template.append(ClientUtil.CLIENT_CONSTANTS.allSources());
    	}
    	template.append("<br>");
    	
    	List<Attribute> attributes = config.getAttributes();
    	if (attributes != null)
    	{
    		String attrVal;
    		for (Attribute attribute : attributes)
    		{
    			attrVal = attribute.getAttributeValue();
    			if (attrVal != null)
    			{
		    		template.append(attribute.getAttributeName());
		    		template.append('=');
		    		template.append(attrVal);
		    		template.append("<br>");
    			}
    		}
    	}
    	
    	template.append(messages.value());
    	if (config.getScalingFactor() == 1)
    	{
    		// Show value with up to 3 decimal places.
    		template.append("=${decimalValue}");
    	}	
    	else
    	{
    		// Show actual data value, rather than scaled value used in plot.
    		template.append("=${dataValue}");
    	}
    	
    	return GChart.formatAsHovertext(template.toString());
    }
	
	
	/** 
	 * Custom GChart HoverParameterInterpreter which returns text dependent on
	 * whether the hovered over point is time series or notification data.
	 */
	 class ChartHoverParameterInterpreter implements
	        HoverParameterInterpreter
	{

		public String getHoverParameter(String paramName,
		        GChart.Curve.Point hoveredOver)
		{
			// Returning null tells GChart "I don't know how to expand that
			// parameter name". The built-in parameters (${x}, ${y}, etc.) won't
			// be processed correctly unless you return null for this "no
			// matching parameter" case.
			String result = null;
			if (paramName.equals("fullTime"))
			{
				// Display the value of the time down to seconds.
				DateTimeFormat dateFormatter = DateTimeFormat.getFormat("MMM dd HH:mm:ss");
				double value = hoveredOver.getX();
				Date transDate = new Date((long) value);
		        result = dateFormatter.format(transDate);
			}
			else if (paramName.equals("decimalValue"))
			{
				// Displays value with up to 3 decimal places.
				result = NumberFormat.getDecimalFormat().format(hoveredOver.getY());
			}
			else if (paramName.equals("dataValue"))
			{
				// Display actual value for scaled time series values.
				Curve parent = hoveredOver.getParent();
				Iterator<TimeSeriesConfig> iter = m_TimeSeriesCurves.keySet().iterator();
				while (iter.hasNext())
				{
					TimeSeriesConfig config = iter.next();
					if (m_TimeSeriesCurves.get(config).equals(parent))
					{
						double value = hoveredOver.getY() / config.getScalingFactor();
						result = NumberFormat.getDecimalFormat().format(value);
					}
				}
			}

			return result;
		}

	}
	
	
	 /**
	  * Class encapsulating a curve (essentially a single point) for a 
	  * time series feature, tagging the GChart Curve with the feature id.
	  */
	class GChartFeatureCurve
	{
		private int 	m_FeatureId;
		private Curve 	m_Curve;
		
		
		GChartFeatureCurve(int featureId, Curve curve)
		{
			m_FeatureId = featureId;
			m_Curve = curve;
		}
		
		
		public int getFeatureId()
		{
			return m_FeatureId;
		}
		
		
		public Curve getFeatureCurve()
		{
			return m_Curve;
		}

	}
}
