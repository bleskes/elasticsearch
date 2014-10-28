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

package com.prelert.client.incident;

import java.util.Date;
import java.util.List;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Widget;
import com.googlecode.gchart.client.GChart;

import com.prelert.client.CSSColorChart;
import com.prelert.client.CSSSymbolChart;
import com.prelert.client.CSSSymbolChart.Shape;
import com.prelert.client.ClientMessages;
import com.prelert.client.ClientUtil;
import com.prelert.client.chart.GChartCentringHoverAnnotation;
import com.prelert.data.gxt.CausalityAggregateModel;


/**
 * Extension of the client-side GChart GWT widget 
 * (see {@link http://code.google.com/p/clientsidegchart/}) which implements the
 * IncidentSummaryChartWidget interface for charting the summary of an incident
 * as a set of bands in a time line.
 * @author Pete Harverson
 */
public class IncidentSummaryGChartWidget extends GChart 
	implements IncidentSummaryChartWidget
{
	private static DateTimeFormat s_AxisTimeFormatter = DateTimeFormat.getFormat("HH:mm:ss");
	private static DateTimeFormat s_FullTimeFormatter = DateTimeFormat.getFormat("MMM dd HH:mm:ss");
	
	private static final int X_PADDING = 10;
	private static final int Y_PADDING = 25;
	private static final int SYMBOL_HEIGHT = 10;
	private static final int NUM_TICKS = 8;
	
	private int m_RowHeight;
	private int m_RowPadding;
	
	
	/**
	 * Creates a new incident summary chart widget, using a GChart GWT component
	 * for plotting a list of aggregated causality data as bands in a time line.
	 * @param rowHeight height of a row, in pixels, for each time line band.
	 */
	public IncidentSummaryGChartWidget(int rowHeight)
	{
		m_RowHeight = rowHeight;
		m_RowPadding = ((rowHeight-SYMBOL_HEIGHT)/2)+2;
		
		addStyleName("prl-timeSeriesChart");
		setBackgroundColor(USE_CSS);
		setBorderStyle(USE_CSS); 
		
		setChartSize(800, 300);
		setChartTitleThickness(0);
		setPadding("25px 5px 5px 0px");
		setClipToPlotArea(true);
		setPlotAreaBorderWidth(10);
		
		
		// Configure grey gridlines and border for plot area.
		setPlotAreaBorderColor("#cccccc");
		setPlotAreaBorderWidth(1);
		setGridColor("#cccccc");
		
		
		// Configure the date/time x-axis.
		// Ticks will be added manually.
		getXAxis().setTickLocation(TickLocation.INSIDE);
		getXAxis().setAxisLabelThickness(0);
		getXAxis().setTickLabelThickness(8);
		getXAxis().setTickCount(NUM_TICKS);
		getXAxis().setTickLabelFormat("=(Date)HH:mm:ss");
		

		// Configure the value y-axis.
		getYAxis().setAxisLabelThickness(0);
		getYAxis().setTickLabelThickness(2);
		getYAxis().setTickLabelFontColor("white");	// Fudge to 'hide' labels.
		getYAxis().setHasGridlines(true);
	    getYAxis().setTickCount(2);
	    getYAxis().setTickLength(0); 
	    getYAxis().setAxisMin(0);
	}
	

	@Override
	public void setCausalityData(List<CausalityAggregateModel> aggregateList)
	{
		removeAll();	
		
		// Get the date range of the causality data.
		CausalityAggregateModel first = aggregateList.get(0);
		long startTime = first.getStartTime().getTime();
		long endTime = first.getEndTime().getTime();
		
		CausalityAggregateModel aggr;
		for (int i = 1; i < aggregateList.size(); i++)
		{
			aggr = aggregateList.get(i);
			startTime = Math.min(startTime, aggr.getStartTime().getTime());
			endTime = Math.max(endTime, aggr.getEndTime().getTime());
		}
		
		// Set x axis range to span causality data, with a minimum date range of 1 minute.
		double xDateRange = Math.max(endTime - startTime, 60000);
		
		// Add on width of a symbol to x axis range, so that the 
		// left/right hand ends of line are fully visible at chart edges.
		double xChartSize = getXChartSize();
		double pixelsPerMs =  (xChartSize-SYMBOL_HEIGHT)/xDateRange;
		
		double axisStart = startTime - (5d/pixelsPerMs);
		double axisEnd = startTime + xDateRange + (5d/pixelsPerMs);
		
		xDateRange = axisEnd - axisStart;
		
		getXAxis().setAxisMin(axisStart);
		getXAxis().setAxisMax(axisEnd);
		
		// Add the ticks and ticks labels.
		double tickIntervalMs = xDateRange/(NUM_TICKS-1);
		long tickRoundedMs;
		for (double i = 0; i < NUM_TICKS-1; i++)
		{
			// Round tick label to nearest second.
			tickRoundedMs = (long)(axisStart + (i* tickIntervalMs) + 500d);
			getXAxis().addTick(axisStart + (i* tickIntervalMs), "<html><span style=\"padding-left:45px;\">" + 
					s_AxisTimeFormatter.format(new Date(tickRoundedMs)) + "</span>");
		}
		
		// No label for the last tick.
		getXAxis().addTick(axisEnd, "");
		
		addCausalityDataToChart(aggregateList);
	}
	
	
	@Override
	public void removeAll()
	{
		clearCurves();
		getXAxis().clearTicks();
	}
	
	
	@Override
	public Widget getChartWidget()
	{
		return this;
	}
	
	
    @Override
    public void setChartSize(int width, int height)
    {
    	int xChartSize = width - X_PADDING;
    	if (xChartSize < 100)
    	{
    		xChartSize = 100;
    	}
    	
    	
    	// For height, leave space for the ticks, tick labels, plus some padding. 
       	int yChartSize = height - Y_PADDING;
    	if (yChartSize < 100)
    	{
    		yChartSize = 100;
    	}
    	
	    setXChartSize(xChartSize);
	    setYChartSize(yChartSize);
	    
	    getYAxis().setAxisMax(yChartSize);
	    
	    // Reposition the grid lines, tick marks and tick labels.
	    getXAxis().setTickLength(yChartSize+3);
	    getXAxis().setTickLabelPadding(-1 * (yChartSize+16));
	    
	    // Reposition the bands.
	    if (getNCurves() > 0)
	    {
	    	positionCausalityDataCurves();
	    }
	    
	    update();
    }
	
	
	@Override
	public void setChartWidth(int width)
	{
		setChartSize(width, getYChartSize() + Y_PADDING);
	}


	@Override
	public void setChartHeight(int height)
	{
		setChartSize(getXChartSize() + X_PADDING, height);
	}
	
	
	/**
	 * Adds the specified data to the chart, plotting a band for each of item of
	 * aggregated causality data between its start and end times.
	 * @param aggregateList list of aggregated causality data to plot on the chart.
	 */
	protected void addCausalityDataToChart(List<CausalityAggregateModel> aggregateList)
	{	
		// Add two curve for each item of causality data:
		//	- one for the start and end points
		//	- one for the band between the start and end points.
		// Store CausalityAggregateModel against each Curve.
		CausalityAggregateModel aggr;
		Symbol symbol;
		Curve curve;
		Date startTime;
		Date endTime;
		for (int i = 0; i < aggregateList.size(); i++)
		{
			aggr = aggregateList.get(i);
			startTime = aggr.getStartTime();
			endTime = aggr.getEndTime();
			
			// Add curve for the end points.
			addCurve();
			curve = getCurve();
			curve.setCurveData(aggr);
			
			// Use SymbolType.SOUTH (to ensure annotation text is placed correctly)
			// and an HTML <img> tag as the annotation text for the end points of the 
			// bands to render the different coloured diamond shapes.
			symbol = curve.getSymbol();
			symbol.setSymbolType(SymbolType.BOX_SOUTH);
//			symbol.setWidth(SYMBOL_HEIGHT);
//			symbol.setHeight(SYMBOL_HEIGHT);
			symbol.setWidth(1);
			
			symbol.setBorderWidth(0);
			
			symbol.setBackgroundColor(CSSColorChart.getInstance().getColor(i));
			symbol.setHoverSelectionEnabled(false);

			if (startTime.equals(endTime) == false)
			{
				// Turn off tooltips for the endpoints.
				symbol.setHoverAnnotationEnabled(false);
				
				// Add curve for the band.
				addCurve();
				curve = getCurve();
				
				symbol = curve.getSymbol();
				
				symbol.setSymbolType(SymbolType.BOX_SOUTHEAST);
				
				symbol.setBorderWidth(0);
				symbol.setHeight(SYMBOL_HEIGHT);
				symbol.setBackgroundColor(CSSColorChart.getInstance().getColor(i));
				symbol.setBrushWidth(5);
				symbol.setHoverAnnotationSymbolType(SymbolType.ANCHOR_MOUSE);
				symbol.setHoverWidget(new GChartCentringHoverAnnotation());
				symbol.setHovertextTemplate(getHovertextTemplate(aggr));
				symbol.setHoverSelectionEnabled(false);
			}
			else
			{
				symbol.setHoverWidget(new GChartCentringHoverAnnotation());
				symbol.setHovertextTemplate(getHovertextTemplate(aggr));
				symbol.setBrushHeight(SYMBOL_HEIGHT);
				symbol.setBrushWidth(SYMBOL_HEIGHT);
				symbol.setHoverSelectionSymbolType(SymbolType.BOX_SOUTH);
				symbol.setHoverSelectionWidth(SYMBOL_HEIGHT);
				symbol.setHoverSelectionHeight(SYMBOL_HEIGHT);
			}
		}
		
		// Position the curves for the current chart size.
		positionCausalityDataCurves();
	}
	
	
	/**
	 * Positions the causality 'bands' to account for the current chart size.
	 */
	protected void positionCausalityDataCurves()
	{
		double xChartSize = getXChartSize();
		double xDateRange = getXAxis().getAxisMax() - getXAxis().getAxisMin();
		double pixelsPerMs =  xChartSize/xDateRange;
		
		double yAxisMax = getYAxis().getAxisMax();
		
		CausalityAggregateModel aggr;
		Date startTime = null;
		Date endTime = null;
		Curve curve;
		Symbol symbol;
		double symbolLength;
		int symbolWidth;
		
		CSSColorChart colorChart = CSSColorChart.getInstance();
		CSSSymbolChart symbolChart = CSSSymbolChart.getInstance();
		String symbolColorName;
		String annotationImgHtml;
		
		int numCurves = getNCurves();
		int dataIndex = -1;
		for (int i = 0; i < numCurves; i++)
		{
			curve = getCurve(i);
			curve.clearPoints();
			
			aggr = (CausalityAggregateModel)(curve.getCurveData());
			if (aggr != null)
			{
				// End point(s).
				dataIndex++;
				startTime = aggr.getStartTime();
				endTime = aggr.getEndTime();

				curve.addPoint(startTime.getTime(), 
						yAxisMax - (dataIndex*m_RowHeight) - m_RowPadding);
				
				symbolColorName = colorChart.getColorName(dataIndex);
				annotationImgHtml = "<html>" + symbolChart.getImageTag(Shape.DIAMOND, symbolColorName);
				curve.getPoint().setAnnotationText(annotationImgHtml);
				curve.getPoint().setAnnotationLocation(AnnotationLocation.CENTER);
				curve.getPoint().setAnnotationXShift(-SYMBOL_HEIGHT/2);
				curve.getPoint().setAnnotationYShift(-1);
				
				if (startTime.equals(endTime) == false)
				{
					curve.addPoint(endTime.getTime(), 
							yAxisMax - (dataIndex*m_RowHeight) - m_RowPadding);
					
					curve.getPoint().setAnnotationText(annotationImgHtml);
					curve.getPoint().setAnnotationLocation(AnnotationLocation.CENTER);
					curve.getPoint().setAnnotationXShift(-SYMBOL_HEIGHT/2);
					curve.getPoint().setAnnotationYShift(-1);
				}
			}
			else
			{
				// Band
				symbol = curve.getSymbol();
				
				// Determine the length of the symbol used for the band.
				symbolLength = pixelsPerMs * (endTime.getTime() - startTime.getTime());
				symbolWidth = (int)(Math.rint(symbolLength));
				
				symbol.setWidth(symbolWidth);
				symbol.setHoverSelectionWidth(symbolWidth+10);
				curve.addPoint(startTime.getTime(), 
						yAxisMax - (dataIndex*m_RowHeight) - m_RowPadding);
			}
		}
	}
	
	
	/**
     * Creates the hover text template for the specified aggregated causality data.
     * @param aggregate aggregated causality data for which to generate hover text.
     * @return the hover text template to use for the specified causality data. 
     */
    protected String getHovertextTemplate(CausalityAggregateModel aggregate)
    {
    	ClientMessages messages = ClientUtil.CLIENT_CONSTANTS;
    	
    	// Display time span.
    	Date startTime = aggregate.getStartTime();
    	Date endTime = aggregate.getEndTime();
    	
    	StringBuilder template = new StringBuilder("<b>");
    	template.append(s_FullTimeFormatter.format(startTime));
    	
    	if (endTime.equals(startTime) == false)
    	{
    		template.append(" - ");
    		template.append(s_AxisTimeFormatter.format(endTime));
    	}
    	
    	template.append("</b><br>");
    	
    	// Add aggregate by/value.
    	String aggregateBy = aggregate.getAggregateBy();
    	String aggregateValue = aggregate.getAggregateValue();
    	template.append(aggregateBy);
    	template.append('=');
    	
    	if (aggregateValue != null)
    	{
    		template.append(aggregate.getAggregateValue());
    	}
    	else
    	{
    		// Differentiate between the 'Others' row and the case where
			// the value of the aggregate attribute is null in the original notification/feature.
    		if (aggregate.isAggregateValueNull() == true)
    		{
    			template.append(messages.notPresent());
    		}
    		else
    		{
    			template.append(messages.other());
    		}
    	}
    	template.append("<br>");
    	
    	
    	if (aggregate.getAggregateBy().equals("source") == false)
    	{
    		// Add in source name / count
        	// TODO - do we want to list more top sources?
        	int sourceCount = aggregate.getSourceCount();
        	if (sourceCount == 1)
        	{
        		template.append(messages.source());
    	    	template.append('=');
    	    	template.append(aggregate.getTopSourceName());
    	    	template.append("<br>");
        	}
        	else
        	{
        		template.append(messages.sourceCount());
    	    	template.append('=');
    	    	template.append(sourceCount);
    	    	template.append("<br>");
        	}
    	}
    	
    	// Add in notification and feature count.
    	int notificationCount = aggregate.getNotificationCount();
    	int featureCount = aggregate.getFeatureCount();
    	if (notificationCount > 0)
    	{
	    	template.append(messages.notificationCount());
	    	template.append('=');
	    	template.append(notificationCount);
	    	template.append("<br>");
    	}
    	if (featureCount > 0)
    	{
	    	template.append(messages.featureCount());
	    	template.append('=');
	    	template.append(featureCount);
	    	template.append("<br>");
    	}
    	
    	return GChart.formatAsHovertext(template.toString());
    }

}
