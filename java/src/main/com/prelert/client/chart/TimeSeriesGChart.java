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
import java.util.Iterator;
import java.util.List;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.GChart.Curve.Point;
import com.googlecode.gchart.client.HoverParameterInterpreter;

import com.prelert.client.ClientMessages;
import com.prelert.client.ClientUtil;
import com.prelert.client.CSSSeverityColors;
import com.prelert.data.Attribute;
import com.prelert.data.Evidence;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesData;
import com.prelert.data.TimeSeriesDataPoint;


/**
 * Extension of the time-based chart for displaying time series data.
 * @author Pete Harverson
 */
public class TimeSeriesGChart extends TimeAxisGChart
{
	private HashMap<TimeSeriesConfig, Curve>	m_TimeSeriesCurves; 
	private HashMap<TimeSeriesConfig, List<GChartFeatureCurve>>	m_FeaturesCurves; 
	
	protected static final int NOTIFICATION_SYMBOL_LENGTH = 10;
	protected static final int TIME_SERIES_LINE_THICKNESS = 2;

	
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
	 * @param timeSeriesData time series data to add to the chart.
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
		symbol.setFillThickness(TIME_SERIES_LINE_THICKNESS);
		symbol.setFillSpacing(0);
		symbol.setHoverWidget(new GChartCentringHoverAnnotation());
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
		double value;
		double maxValue = 0d;	// Store the max value as the curve data.
	    if (isAutoValueRange() == true)
	    {
			for (TimeSeriesDataPoint dataPoint : dataPoints)
			{
				value = dataPoint.getValue();
				maxValue = Math.max(maxValue, value);
				
				curve.addPoint(dataPoint.getTime(), value);
				
				if (dataPoint.getFeature() != null)
				{
					addTimeSeriesFeature(config, dataPoint);
				}
			}
			
			curve.setCurveData(maxValue);

			if (m_TimeLine != null)
			{
				// Set y coord of top of timeline to 0 to ensure new y-axis max is
				// calculated only on basis of the data series (and not the timeline).
				m_TimeLine.getPoint().setY(0);
			}
			
			double yAxisMax = calculateYAxisMax();
			getYAxis().setAxisMax(yAxisMax);
			
			String tickLabelFormat = calculateYAxisTickLabelFormat(yAxisMax);
			getYAxis().setTickLabelFormat(tickLabelFormat);
			
			// If a timeline has been added, make it full height.
			if (m_TimeLine != null)
			{
				m_TimeLine.getPoint().setY(yAxisMax);
			}
		}
		else
		{
			// Even after setting the y axis OutOfBoundsMultiplier, part or all
			// of a series containing points that fall a long way off the plot area
			// may fail to be rendered by gChart. To prevent this, clip points to
			// 80% of the y axis OutOfBoundsMultiplier.
			double clipValue = getYAxis().getAxisMax() * 
				(0.8d * getYAxis().getOutOfBoundsMultiplier());
			
			for (TimeSeriesDataPoint dataPoint : dataPoints)
			{
				value = dataPoint.getValue();
				maxValue = Math.max(maxValue, value);
				
				value = Math.min(value, clipValue);
				curve.addPoint(dataPoint.getTime(), value);
				
				if (dataPoint.getFeature() != null)
				{
					addTimeSeriesFeature(config, dataPoint);
				}
				
				curve.setCurveData(maxValue);
			}
	    }
		
		m_TimeSeriesCurves.put(config, curve);
		
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
		symbol.setHoverWidget(new GChartCentringHoverAnnotation());
		symbol.setHovertextTemplate(hoverTemplate);
		
		curve.addPoint(dataPoint.getTime(), dataPoint.getValue());
		
		List<GChartFeatureCurve> curveList = m_FeaturesCurves.get(timeSeriesConfig);
		if (curveList == null)
		{
			curveList = new ArrayList<GChartFeatureCurve>();
			m_FeaturesCurves.put(timeSeriesConfig, curveList);
		}
		
		curveList.add(new GChartFeatureCurve(feature, curve));
		
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
	 * Removes all time series, notifications and the time marker (if set)
	 * from the chart.
	 */
	public void removeAll()
	{
		removeAllTimeSeries();
		clearTimeMarker();
		
		if (isAutoValueRange() == true)
		{
			double yAxisMax = calculateYAxisMax();
			getYAxis().setAxisMax(yAxisMax);
			
			String tickLabelFormat = calculateYAxisTickLabelFormat(yAxisMax);
			getYAxis().setTickLabelFormat(tickLabelFormat);
		}
	}
	
	
	/**
	 * Returns the time series feature that the mouse is currently touching
	 * i.e. the 'hovered over' point.
	 * @return the touched feature, or <code>null</code> if no 
	 * time series feature is currently hovered over.
	 */
	public Evidence getTouchedTimeSeriesFeature()
	{
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
							return featureCurve.getFeature();
						}
					}
				}
			}
		}
		
		return null;
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
	 * Returns the raw data value of the point that the mouse is currently touching
	 * i.e. the 'hovered over' point. This value may be different from the y-axis
	 * value of the point as plotted on the chart if the time series containing
	 * the point is being scaled (such as for normalised series on a causality chart).
	 * @return the time of the touched point. A value of zero is returned if no 
	 * 	point is currently hovered over.
	 */
	public double getTouchedPointDataValue()
	{
		double value = 0d;
		
		Point touchedPoint = getTouchedPoint();
		if (touchedPoint != null)
		{
			value = touchedPoint.getY();
			
			TimeSeriesConfig touchedSeries = getTouchedTimeSeries();
			if (touchedSeries != null && touchedSeries.getScalingFactor() != 1.0d)
			{
				value = value/touchedSeries.getScalingFactor();
			}
		}
		
		return value;
	}
	
	
	/**
	 * Returns the maximum data value across the time series currently displayed
	 * on the chart. Note this may differ from the maximum value plotted, as points
	 * that lie a long way outside the plot area may have been clipped.
	 * @return
	 */
	public double getMaxTimeSeriesValue()
	{
		if (isAutoValueRange() == true)
		{
			return getYAxis().getDataMax();
		}
		else
		{
			// Max values are stored as the curve data.
			double maxValue = 0d;
			Iterator<Curve> curveIter = m_TimeSeriesCurves.values().iterator();
			Curve curve;
			Double curveMax;
			while (curveIter.hasNext())
			{
				curve = curveIter.next();
				curveMax = (Double)(curve.getCurveData());
				if (curveMax != null)
				{
					maxValue = Math.max(maxValue, curveMax);
				}
			}
			
			return maxValue;
		}
	}
	
	
	/**
     * Creates the hover text template for the specified TimeSeriesConfig.
     * @param config configuration of the time series for which to generate hover text.
     * @return the hover text template for the specified time series. 
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
	 * Custom GChart HoverParameterInterpreter which returns text showing the time
	 * and value of the hovered over point.
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
		private Evidence 	m_Feature;
		private Curve 		m_Curve;
		
		
		GChartFeatureCurve(Evidence feature, Curve curve)
		{
			m_Feature = feature;
			m_Curve = curve;
		}
		
		
		public Evidence getFeature()
		{
			return m_Feature;
		}
		
		
		public Curve getFeatureCurve()
		{
			return m_Curve;
		}

	}
}
