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

import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.GChart.Curve.Point;

import com.prelert.client.CSSSeverityColors;
import com.prelert.client.CSSSymbolChart;
import com.prelert.client.ClientMessages;
import com.prelert.client.ClientUtil;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.Severity;
import com.prelert.data.TimeFrame;
import com.prelert.data.gxt.ProbableCauseModel;
import com.prelert.data.gxt.ProbableCauseModelCollection;


/**
 * Extension of the time series GChart widget for displaying causality data.
 * @author Pete Harverson
 */
public class CausalityGChart extends TimeSeriesGChart
{
	
	private HashMap<Integer, Curve>	m_Notifications; // Curve vs ProbableCauseModelCollection id.
	
	
	/**
	 * Creates a new chart for displaying causality data.
	 */
	public CausalityGChart()
	{
	
	}

	
	/**
	 * Adds the specified collection of notification type probable causes
	 * to the chart. Where the aggregation contains more than one notification,
	 * it is represented on the chart as a 'band' connecting the first and last
	 * notifications in the aggregation.
	 * @param collection notification type ProbableCauseModelCollection to add.
	 */
	public void addNotificationCollection(
            ProbableCauseModelCollection collection)
    {
		if (m_Notifications == null)
		{
			m_Notifications = new HashMap<Integer, Curve>();
		}
			
		// Keep the timeline as the curve with the highest index.
		int indexToAdd = getNCurves();
		if (m_TimeLine != null)
		{
			indexToAdd--;
		}

		// Create the new curve.
		addCurve(indexToAdd);
		Curve curve = getCurve(indexToAdd);
		
		// Use SymbolType.NONE and an HTML <img> tag as each point's
		// annotation text to render different symbol shapes.
		Symbol symbol = curve.getSymbol();
		Severity severity = collection.getSeverity();
		String symbolHtml = "<html>" + CSSSymbolChart.getInstance().getImageTag(collection.getId(), severity);
		symbol.setBorderWidth(0);
		symbol.setSymbolType(SymbolType.NONE);
		symbol.setBrushHeight(NOTIFICATION_SYMBOL_LENGTH);
		symbol.setBrushWidth(NOTIFICATION_SYMBOL_LENGTH);
		symbol.setHoverSelectionSymbolType(SymbolType.BOX_CENTER);
		symbol.setHoverSelectionWidth(NOTIFICATION_SYMBOL_LENGTH);
		symbol.setHoverSelectionHeight(NOTIFICATION_SYMBOL_LENGTH);
		
		symbol.setHovertextTemplate(getHovertextTemplate(collection)); 
		symbol.setHoverWidget(new CentringHoverAnnotation());
		
		double yPos = calculateNotificationCollectionY(collection);
		
		curve.addPoint(collection.getStartTime().getTime(), yPos);
		
		curve.getPoint().setAnnotationLocation(AnnotationLocation.CENTER);
		curve.getPoint().setAnnotationXShift(-NOTIFICATION_SYMBOL_LENGTH/2);
		curve.getPoint().setAnnotationText(symbolHtml);
		
		if (collection.getSize() > 1)
		{
			// Ensures connecting line border color matches severity.
			symbol.setBorderColor(CSSSeverityColors.getColor(severity));	
			
			symbol.setFillThickness(2);
			symbol.setFillSpacing(0);
			
			curve.addPoint(collection.getEndTime().getTime(), yPos);
			
			curve.getPoint().setAnnotationLocation(AnnotationLocation.CENTER);
			curve.getPoint().setAnnotationText(symbolHtml);
			curve.getPoint().setAnnotationXShift(-NOTIFICATION_SYMBOL_LENGTH/2);
		}
		
		m_Notifications.put(collection.getId(), curve);	
		
		// Associate the ProbableCauseModelCollection with the Curve.
		curve.setCurveData(collection);
    }
	
	
	/**
	 * Removes the specified collection of notification type probable causes from
	 * the chart.
	 * @param collection notification type ProbableCauseModelCollection to remove.
	 */
	public void removeNotificationCollection(ProbableCauseModelCollection collection)
	{
		if (m_Notifications != null)
		{
			int id = collection.getId();
			Curve curve = m_Notifications.remove(id);

			if (curve != null)
			{
				removeCurve(curve);
			}
		}
	}
	
	
    @Override
    public void removeAllNotifications()
    {
    	if (m_Notifications != null)
		{
			Iterator<Curve> curveIter = m_Notifications.values().iterator();
			Curve curve;
			while (curveIter.hasNext())
			{
				curve = curveIter.next();
				removeCurve(curve);
			}
			
			m_Notifications.clear();
		}
    }
    
    
    @Override
    public int getTouchedNotificationId()
    {
    	int evidenceId = -1;
		
    	ProbableCauseModel notification = getTouchedNotification();
    	if (notification != null)
    	{
    		evidenceId = notification.getEvidenceId();
    	}
		
		return evidenceId;
    }
    
    
    /**
	 * Returns the notification type probable cause that the mouse is currently 
	 * touching i.e. the 'hovered over' point.
	 * @return the touched notification, or <code>null</code> if no notification
	 * 		type probable is currently hovered over.
	 */
	public ProbableCauseModel getTouchedNotification()
	{
		ProbableCauseModel notification = null;
		
		Curve touchedCurve = getTouchedCurve();
		if (touchedCurve != null)
		{
			Object curveData = touchedCurve.getCurveData();
			if (curveData != null)
			{
				ProbableCauseModelCollection collection = (ProbableCauseModelCollection)curveData;
				
				if (collection.getDataSourceCategory() == DataSourceCategory.NOTIFICATION)
				{
					Point touchedPoint = getTouchedPoint();
					int collectionIndex = touchedCurve.getPointIndex(touchedPoint);
					notification = collection.getProbableCause(collectionIndex);
				}
			}
		}
		
		return notification;
	}


	/**
	 * Calculates the y-position for a notification type probable cause collection.
	 * The calculation takes into account other collections with similar time values, 
	 * so that they do not overlap on the chart.
	 * @param notification type ProbableCauseModelCollection to position.
	 * @return the y-position for the notification.
	 */
	protected double calculateNotificationCollectionY(ProbableCauseModelCollection collection)
	{
		// TODO - redo the layout if the chart size changes.
		
		double yPixelToModel = getYAxis().getAxisMax() / getYChartSize();
		double yPos = yPixelToModel * (NOTIFICATION_SYMBOL_LENGTH/2);
		
		if (m_Notifications.size() > 0)
		{
			// Calculate the minimum x value gap between points at which we
			// need to offset the y positions.
			// TODO - provide some spacing between groups.
			double xPixelToModel = (getXAxis().getAxisMax()- getXAxis().getAxisMin())/ getXChartSize();
			long xLength = (long)xPixelToModel*NOTIFICATION_SYMBOL_LENGTH;	// Width of symbol in model.
			
			long msStart1 = collection.getStartTime().getTime();
			long msEnd1 = collection.getEndTime().getTime() + xLength;
			
			// Find the y positions of all notification 'bands' which overlap in 
			// time with this ProbableCauseModelCollection.
			long msStart2;
			long msEnd2;
			ArrayList<Double> yPositions = new ArrayList<Double>();
			
			Iterator<Curve> curveIter = m_Notifications.values().iterator();
			Curve curve;
			Point startPoint;
			Point endPoint;
			while (curveIter.hasNext())
			{
				curve = curveIter.next();
				startPoint = curve.getPoint(0);
				endPoint = curve.getPoint();
				
				msStart2 = (long)startPoint.getX();
				msEnd2 = (long)endPoint.getX() + xLength;
				
				if (	(msStart1 >= msStart2 && msStart1 <= msEnd2) ||
						(msStart2 >= msStart1 && msStart2 <= msEnd1) ||
						(msEnd1 >= msStart2 && msEnd1 <= msEnd2) ||
						(msEnd2 >= msStart1 && msEnd2 <= msEnd1))
				{
					yPositions.add(startPoint.getY());
				}
			}
	
			if (yPositions.size() > 0)
			{
				// Calculate the y increment for overlapping times.
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
			
		}
		
		return yPos;
	}
	
	
	/**
     * Creates the hover text template for the specified collection of notification
     * type probable causes.
     * @param notifications ProbableCauseModelCollection for which to build the hover template.
     * @return hover text tooltip for the ProbableCauseModelCollection.
     */
    protected String getHovertextTemplate(ProbableCauseModelCollection notifications)
    {
    	ClientMessages messages = ClientUtil.CLIENT_CONSTANTS;
    	
    	StringBuilder template = new StringBuilder("<b>");
    	template.append(notifications.getDescription());
    	template.append("</b><br>");
    	
    	template.append(messages.type());
    	template.append('=');
    	template.append(notifications.getDataSourceName());
    	template.append("<br>");
    	
    	Date startTime = notifications.getStartTime();
    	Date endTime = notifications.getEndTime();
    	
    	template.append(messages.startTime());
    	template.append('=');
    	template.append(ClientUtil.formatTimeField(startTime, TimeFrame.SECOND));
    	template.append("<br>");
    	
    	template.append(messages.endTime());
    	template.append('=');
    	template.append(ClientUtil.formatTimeField(endTime, TimeFrame.SECOND));
    	template.append("<br>");
    	
    	template.append(messages.count());
    	template.append('=');
    	template.append(notifications.getCount());
    	template.append("<br>");
    	
    	template.append(messages.sourceCount());
    	template.append('=');
    	template.append(notifications.getSourceCount());
    	
    	return GChart.formatAsHovertext(template.toString());
    }
	
}
