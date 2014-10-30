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

import com.google.gwt.core.client.GWT;
import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.GChart.Curve.Point;

import com.prelert.data.Incident;


/**
 * Extension of the time-based chart for displaying incidents.
 * @author Pete Harverson
 */
public class IncidentGChart extends TimeAxisGChart
{
	private HashMap<Integer, Curve>	m_IncidentCurves; // Curve vs evidence id.
	
	private int				m_SelectedEvidenceId = -1;
	private Curve			m_SelectedCurve;	// Border effect around selected incident.
	
	/** Number of different symbol colours used for indicating anomaly score. */
	public static final int NUM_SYMBOL_COLOURS = 13;
	
	
	/**
	 * Creates a new chart for plotting incidents against time.
	 */
	public IncidentGChart()
	{	
		m_IncidentCurves = new HashMap<Integer, Curve>();
		
		setChartSize(800, 180);
		
		setClipToPlotArea(false);

		// Configure the value y-axis. 
	    setValueTickLabelsVisible(false);
        getYAxis().setAxisMin(0);
		getYAxis().setAxisMax(100);  
		getYAxis().setTickLength(0);
		setAutoValueRange(false);
		
		// Configure grey gridlines and border for plot area.
		setPlotAreaBorderColor("#cccccc");
		setPlotAreaBorderWidth(1);
		setGridColor("#cccccc");
		getXAxis().setHasGridlines(true);
		getYAxis().setHasGridlines(true);
		
		setLegendVisible(false);	
	}
	
	
	/**
	 * Adds an incident to the chart.
	 * @param incident the incident to add.
	 */
	public void addIncident(Incident incident)
	{		
		// Keep the timeline as the curve with the highest index.
		int indexToAdd = getNCurves();
		if (m_TimeLine != null)
		{
			indexToAdd--;
		}
		
		// Create the new curve.
		addCurve(indexToAdd);
		Curve curve = getCurve(indexToAdd);
		
		// Use the legend label to store the description.
		curve.setLegendLabel(incident.getDescription());
		
		Symbol symbol = curve.getSymbol();
		symbol.setSymbolType(SymbolType.BOX_NORTH);
		symbol.setBorderWidth(0);
		
		// Set symbol dimensions and colour according to the anomaly score.
		// High anomaly scores : red / large
		// Low anomaly scores : blue / small
		int symbolSize = getSymbolSize(incident);
		symbol.setWidth(symbolSize);
		symbol.setHeight(symbolSize);
		
		// Use images for symbols.
		String imageURL = getSymbolImageURL(incident);		
		symbol.setImageURL(imageURL);
		
		
		// Set the incident tooltip - line break around every 40 characters.
		StringBuilder desc = new StringBuilder(incident.getDescription());
		int clipWidth = desc.indexOf(" ", 40);
		int charIdx = clipWidth;
		while (charIdx >= 0)
		{
			desc = desc.replace(charIdx, charIdx+1, "<br>");
			charIdx = desc.indexOf(" ", charIdx + clipWidth);
		}
		
		String hoverTemplate = GChart.formatAsHovertext("<b>${x}</b><br>" +  desc);
		symbol.setHovertextTemplate(hoverTemplate);
		symbol.setHoverWidget(new CentringHoverAnnotation());
		
		double yPos = calculateIncidentY(incident.getTime(), symbolSize);
		curve.addPoint(incident.getTime().getTime(), yPos);
		
		// Try using sprite based images for symbols. 
//		String symbolHtml = getSymbolAnnotationText(incident, symbolSize);
//		curve.getPoint().setAnnotationLocation(AnnotationLocation.SOUTH);
//		curve.getPoint().setAnnotationXShift(-symbolSize/2);
//		curve.getPoint().setAnnotationYShift(symbolSize);
//		curve.getPoint().setAnnotationText(symbolHtml);
		
		m_IncidentCurves.put(incident.getEvidenceId(), curve);

	}
	
	
	/**
	 * Removes all incidents from the chart.
	 */
    public void removeAllIncidents()
    {
    	// Clear current selection, if any.
    	clearSelection();
    	
    	Iterator<Curve> curveIter = m_IncidentCurves.values().iterator();
		Curve curve;
		while (curveIter.hasNext())
		{
			curve = curveIter.next();
			removeCurve(curve);
		}
		
		m_IncidentCurves.clear();
    }
    
    
    /**
	 * Returns the incident that the mouse is currently touching
	 * i.e. the 'hovered over' point.
	 * @return the touched incident, or <code>null</code> if no 
	 * incident is currently hovered over. <b>Note</b> that the only fields set in
	 * the incident are:
	 * <ul>
	 * <li>description</li>
	 * <li>time</li>
	 * <li>evidence id</li>
	 * </ul>
	 */
    public Incident getTouchedIncident()
    {
    	Incident touchedIncident = null;  	
    	
    	Curve touchedCurve = getTouchedCurve();
        if (touchedCurve != null && m_IncidentCurves.containsValue(touchedCurve))
        {
        	Iterator<Integer> iter = m_IncidentCurves.keySet().iterator();
    		Integer id;
    		while (iter.hasNext())
    		{
    			id = iter.next();
    			if (m_IncidentCurves.get(id).equals(touchedCurve))
    			{
    				// Create an incident and set the description, time
    				// and evidence_id fields.
    				touchedIncident = new Incident();
    				touchedIncident.setEvidenceId(id.intValue());
    				touchedIncident.setTime(getTouchedPointTime());
    				touchedIncident.setDescription(touchedCurve.getLegendLabel());
    				break;
    			}
    		}
        }

		return touchedIncident;
    }
    
    
    /**
     * Returns the evidence id of the selected incident.
     * @return the evidence_id of the selected incident, 
     * 		or -1 if no incident is selected.
     */
    public int getSelectedEvidenceId()
    {
    	return m_SelectedEvidenceId;
    }
    
    
    /**
     * Marks the incident with the specified evidence id as selected.
     * If the chart does not contain an incident with the specified evidence id,
     * the current selection will not change.
     * @param evidenceId evidence id of the incident to select.
     */
    public void select(int evidenceId)
    {
    	Curve incidentCurve = m_IncidentCurves.get(evidenceId);
    	if (incidentCurve != null)
    	{
    		select(incidentCurve);
    	}
    }
    
    
    /**
     * Marks the specified incident curve as selected.
     * @param incidentCurve the incident to select.
     */
    protected void select(Curve incidentCurve)
    {
    	Symbol incidentSymbol = incidentCurve.getSymbol();
    	Point incident = incidentCurve.getPoint();
    	
    	Symbol selectionSymbol;
    	if (m_SelectedCurve == null)
    	{
    		addCurve();
    		m_SelectedCurve = getCurve();
    		
    		selectionSymbol = m_SelectedCurve.getSymbol();
    		selectionSymbol.setSymbolType(SymbolType.BOX_NORTH);
    		selectionSymbol.setBorderWidth(2);
    		selectionSymbol.setBorderColor("gray");
    		selectionSymbol.setHoverSelectionEnabled(false);
    		selectionSymbol.setHoverAnnotationEnabled(false);

    		m_SelectedCurve.addPoint(incident.getX(), incident.getY()-2.5);
    	}
    	else
    	{
    		m_SelectedCurve.getPoint().setX(incident.getX());
    		m_SelectedCurve.getPoint().setY(incident.getY()-2.5);
    		selectionSymbol = m_SelectedCurve.getSymbol();
    	}
    	
		selectionSymbol.setWidth(incidentSymbol.getWidth() + 8);
		selectionSymbol.setHeight(incidentSymbol.getHeight() + 8);
		
		// Get the evidence id of the selected incident.
		Iterator<Integer> iter = m_IncidentCurves.keySet().iterator();
		Integer id;
		while (iter.hasNext())
		{
			id = iter.next();
			if (m_IncidentCurves.get(id).equals(incidentCurve))
			{
				m_SelectedEvidenceId = id.intValue();
				break;
			}
		}
    	
    	update();
    }
    
    
	/**
	 * Deselects the selected incident, if one is currently selected.
	 */
	public void clearSelection()
	{
		if (m_SelectedCurve != null)
		{
			removeCurve(m_SelectedCurve);
			m_SelectedCurve = null;
			m_SelectedEvidenceId = -1;
		}
	}
    
    
	/**
	 * Calculates the y-position for an incident at the specified time. 
	 * The calculation takes into account other incidents with similar time 
	 * values, so that they do not overlap on the chart.
	 * @param time the time of the incident
	 * @param symbolSize the width/height of the symbol used for the incident.
	 * @return the y-position for the incident.
	 */
	protected double calculateIncidentY(Date time, int symbolSize)
	{	
		double yPos = 0;

		double xPixelToModel = (getXAxis().getAxisMax()- getXAxis().getAxisMin())/ getXChartSize();
		double symbolModelWidth = xPixelToModel * symbolSize;
		
		long ms1 = time.getTime();
		long ms2;
		ArrayList<Point> overlappingPoints = new ArrayList<Point>();
		
		Iterator<Curve> curveIter = m_IncidentCurves.values().iterator();
		Curve curve;
		Point point;
		int otherSymbolSize;
		double otherSymbolModelWidth;
		
		while (curveIter.hasNext())
		{
			curve = curveIter.next();
			otherSymbolSize = curve.getSymbol().getWidth();
			otherSymbolModelWidth = xPixelToModel * otherSymbolSize;
			
			point = curve.getPoint();
			ms2 = (long)point.getX();
			
			// Check if the time gap between points means that they would overlap.
			if (Math.abs(ms2-ms1) < (symbolModelWidth + otherSymbolModelWidth)/2)
			{
				overlappingPoints.add(point);
			}
			
		}
		
		if (overlappingPoints.size() > 0)
		{
			double yPixelToModel = (getYAxis().getAxisMax())/ getYChartSize();
			
			double overlapBottomY;	// y-coordinate of bottom of overlapping incident symbol.
			double overTopY;		// y-coordinate of top of overlapping incident symbol.
			
			double topYPos = yPos + (yPixelToModel * symbolSize); // y-coordinate of top of symbol for this incident.
			
			boolean isOverlapping = true;
			while (isOverlapping == true)	// Iterate at least once.
			{
				isOverlapping = false;
				
				for (Point coincidentPoint : overlappingPoints)
				{
					otherSymbolSize = coincidentPoint.getParent().getSymbol().getHeight();
					
					overlapBottomY = coincidentPoint.getY();
					overTopY = overlapBottomY + (yPixelToModel * otherSymbolSize);
					
					if ( (yPos >= overlapBottomY && yPos <= overTopY) ||
							(topYPos >= overlapBottomY && topYPos <= overTopY ) )
					{
						isOverlapping = true;
						
						yPos = overTopY + 1;	// Add vertical spacing of 1 unit.
						topYPos = yPos + (yPixelToModel * symbolSize);
						break;
					}
				}
			}
		}
		
		return yPos;
	}
	
	
	
	/**
	 * Returns the size of the symbol to use for the specified incident,
	 * which will be based on the value of the incident's anomaly score and the 
	 * current zoom level.
	 * @param incident the incident for which to return the symbol size.
	 * @return the size of the symbol.
	 */
	protected int getSymbolSize(Incident incident)
	{
		double anomalyScore = incident.getAnomalyScore();
		
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
		double size;
		double xAxisSpan = getDateAxisSpan()/60000;	// Span in minutes.
		if (xAxisSpan <= 15)
		{
			size = (25d + Math.round(anomalyScore/3.33d));
		}
		else if (xAxisSpan <= 30)
		{
			size = (22d + Math.round(anomalyScore/3.57d));
		}
		else if (xAxisSpan <= 60)
		{
			size = (19d + Math.round(anomalyScore/3.85d));
		}
		else if (xAxisSpan <= 180)
		{
			// 3 hours - 180 mins
			size = (16d + Math.round(anomalyScore/4.16d));
		}
		else if (xAxisSpan <= 360)
		{
			// 6 hours - 360 mins
			size = (14d + Math.round(anomalyScore/4.76d));
		}
		else if (xAxisSpan <= 720)
		{
			// 12 hours - 720 mins
			size = (12d + Math.round(anomalyScore/5.56d));
		}
		else if (xAxisSpan <= 1440)
		{
			// 1 day - 1440 mins
			size = (10d + Math.round(anomalyScore/6.67d));
		}
		else if (xAxisSpan <= 2880)
		{
			// 2 days - 2880 mins
			size = (9d + Math.round(anomalyScore/9.09d));
		}
		else if (xAxisSpan <= 5760)
		{
			// 4 days - 5760 mins
			size = (8d + Math.round(anomalyScore/12.5d));
		}
		else if (xAxisSpan <= 10080)
		{
			// 1 week - 10080 mins
			size = (7d + Math.round(anomalyScore/16.7d));
		}
		else if (xAxisSpan <= 20160)
		{
			// 2 weeks - 20160 mins
			size = (6d + Math.round(anomalyScore/25d));
		}
		else
		{
			// 4 weeks - 40320 mins.
			size = (5d + Math.round(anomalyScore/33.3d));
		}
		
		return (int)size;
	}
	
	
	/**
	 * Returns the URL that will be used for the image used in rendering the
	 * symbol for the specified incident. Note that the image will be scaled to
	 * different sizes depending on the anomaly score of the incident.
	 * @param incident the incident for which to return the symbol image URL.
	 * @return the url that defines the src property of the image used to draw 
	 * 			the symbol on the chart for the specified incident.
	 */
	protected String getSymbolImageURL(Incident incident)
	{
		// Use circle symbols. Colour of symbol is dependant on anomaly score.
		double anomalyScore = incident.getAnomalyScore() - 1;	// Anomaly score starts at 1.
		int multipleInt = (int)(Math.floor((anomalyScore/(100d/(double)NUM_SYMBOL_COLOURS))));
		
		return new String("images/circle_" + multipleInt + "_48.png");
	}
	
	
	/**
	 * Returns the annotation text, as HTML, to render a symbol for the specified
	 * incident.
	 * @param incident the incident for which to return annotation text.
	 * @param symbolSize size for the symbol, in pixels.
	 * @return the annotation text for rendering an incident symbol. 
	 */
	protected String getSymbolAnnotationText(Incident incident, int symbolSize)
	{
		double anomalyScore = incident.getAnomalyScore() - 1;	// Anomaly score starts at 1.
		double multipleInt = Math.floor((anomalyScore/(100d/13d)));
		int imgWidth = (int)((double)symbolSize/48d * 624d);
		int leftPos = (int)((multipleInt * 48d) * ((double)symbolSize/48d));
		
		// Use an <img> tag inside a <div> and set the width/height proportionally
		// to scale the sprite image.
		
		String html = "<html><div style=\"width:" + symbolSize + "px; height:" + symbolSize + "px; overflow:hidden; position:relative;\">";
	    html += "<img src=\"images/incident_symbols_48.png\" width=\"" + imgWidth + "\" height=\"" + symbolSize + "\" ";
	    html += "style=\"position:absolute; top: 0px; left: -" + leftPos + "px;\" /></div>";
	    
	    return html;
	}
	
}
