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

import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.GChart.Curve.Point;

import com.prelert.client.CSSColorChart;
import com.prelert.client.CSSSymbolChart;
import com.prelert.client.ClientMessages;
import com.prelert.client.ClientUtil;
import com.prelert.client.CSSSymbolChart.Shape;
import com.prelert.data.Attribute;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.TimeFrame;
import com.prelert.data.gxt.CausalityDataModel;
import com.prelert.data.gxt.ProbableCauseModel;
import com.prelert.data.gxt.ProbableCauseModelCollection;


/**
 * Extension of the time series GChart widget for displaying causality data.
 * @author Pete Harverson
 */
public class CausalityGChart extends TimeSeriesGChart
{
	
	private HashMap<Integer, Curve>	m_Notifications; // Curve vs ProbableCauseModelCollection id.
	
	private ArrayList<Curve>	m_NotificationEndPoints;	
	private ArrayList<Curve>	m_NotificationBands;
	
	
	/**
	 * Creates a new chart for displaying causality data.
	 */
	public CausalityGChart()
	{
		setAutoValueRange(false);
	}

	
	/**
	 * Adds the specified collection of notification type probable causes
	 * to the chart. Where the aggregation contains more than one notification,
	 * it is represented on the chart as a 'band' connecting the first and last
	 * notifications in the aggregation.
	 * @param collection notification type ProbableCauseModelCollection to add.
	 */
	public void addNotificationCollection(ProbableCauseModelCollection collection,
			String symbolColour, Shape symbolShape)
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
		
		CSSColorChart colorChart = CSSColorChart.getInstance();
		CSSSymbolChart symbolChart = CSSSymbolChart.getInstance();
		
		// Use SymbolType.NONE and an HTML <img> tag as each point's
		// annotation text to render different symbol shapes.
		Symbol symbol = curve.getSymbol();
		String symbolColorName = colorChart.getColorName(symbolColour);
		String symbolHtml = "<html>" + symbolChart.getImageTag(symbolShape, symbolColorName);
		symbol.setBorderWidth(0);
		symbol.setSymbolType(SymbolType.BOX_CENTER);
		
		symbol.setBrushHeight(NOTIFICATION_SYMBOL_LENGTH);
		symbol.setBrushWidth(NOTIFICATION_SYMBOL_LENGTH);
		symbol.setHoverSelectionSymbolType(SymbolType.BOX_CENTER);
		symbol.setHoverSelectionWidth(NOTIFICATION_SYMBOL_LENGTH);
		symbol.setHoverSelectionHeight(NOTIFICATION_SYMBOL_LENGTH);
		
		symbol.setHovertextTemplate(getHovertextTemplate(collection.getDescription(),
				collection.getDataSourceName(), collection.getStartTime(), 
				collection.getEndTime(), collection.getCount(), collection.getSourceCount(), 
				collection.getProbableCause(0).getSource(), collection.getProbableCause(0).getAttributes()));
		symbol.setHoverWidget(new GChartCentringHoverAnnotation());

		double yPos = calculateNotificationCollectionY(collection);
		
		curve.addPoint(collection.getStartTime().getTime(), yPos);
		
		curve.getPoint().setAnnotationLocation(AnnotationLocation.CENTER);
		curve.getPoint().setAnnotationXShift(-NOTIFICATION_SYMBOL_LENGTH/2);
		curve.getPoint().setAnnotationText(symbolHtml);
		
		if (collection.getSize() > 1)
		{
			// Ensures connecting line border color matches severity.
			symbol.setBorderColor(symbolColour);	
			
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
	
	
	/**
	 * Adds the specified notification type causality data to the chart. Where 
	 * the aggregation contains more than one notification, it is represented on 
	 * the chart as a 'band' connecting the first and last notifications.
	 * @param notificationData notification data to add to the chart.
	 * @param symbolColour symbol colour to use for plotting the data.
	 * @param symbolShape symbol shape to use for plotting the data.
	 */
	public void addNotifications(CausalityDataModel notificationData, 
			String symbolColour, Shape symbolShape)
    {	
		if (m_NotificationEndPoints == null)
		{
			m_NotificationEndPoints = new ArrayList<Curve>();
		}
		
		
		// Keep the timeline as the curve with the highest index.
		int indexToAdd = getNCurves();
		if (m_TimeLine != null)
		{
			indexToAdd--;
		}
		
		// Add two curves for each item of causality data:
		//	- one for the start and end points
		//	- one for the band between the start and end points, if startTime != endTime.
		// Store CausalityDataModel against each Curve.
		Date startTime = notificationData.getStartTime();
		Date endTime = notificationData.getEndTime();
		
		// Calculate the y position for the notification curve.
		double yPos = calculateNotificationY(notificationData);
		
		// Add curve for the end points.
		addCurve(indexToAdd);
		Curve endPoints = getCurve(indexToAdd);
		endPoints.setCurveData(notificationData);
		m_NotificationEndPoints.add(endPoints);
		
		// Use SymbolType.SOUTH (to ensure annotation text is placed correctly)
		// and an HTML <img> tag as the annotation text for the end points of the 
		// bands to render the different coloured shapes.
		Symbol symbol = endPoints.getSymbol();
		symbol.setSymbolType(SymbolType.BOX_SOUTH);
		symbol.setWidth(1);
		symbol.setBorderWidth(0);
		
		symbol.setBackgroundColor(symbolColour);
		symbol.setHoverSelectionEnabled(false);
		
		String hovertextTemplate = getHovertextTemplate(notificationData.getDescription(), 
				notificationData.getDataSourceName(), startTime, endTime, 
				notificationData.getCount(), 1, notificationData.getSource(),
				notificationData.getAttributes());


		endPoints.addPoint(startTime.getTime(), yPos);
		
		CSSColorChart colorChart = CSSColorChart.getInstance();
		CSSSymbolChart symbolChart = CSSSymbolChart.getInstance();
		
		String symbolColorName = colorChart.getColorName(symbolColour);
		String annotationImgHtml = "<html>" + symbolChart.getImageTag(symbolShape, symbolColorName);
		endPoints.getPoint().setAnnotationText(annotationImgHtml);
		endPoints.getPoint().setAnnotationLocation(AnnotationLocation.CENTER);
		endPoints.getPoint().setAnnotationXShift(-NOTIFICATION_SYMBOL_LENGTH/2);
		endPoints.getPoint().setAnnotationYShift(-1);
	

		if (startTime.equals(endTime) == false)
		{
			// Add a point at the end time.
			endPoints.addPoint(endTime.getTime(), yPos);	
			endPoints.getPoint().setAnnotationText(annotationImgHtml);
			endPoints.getPoint().setAnnotationLocation(AnnotationLocation.CENTER);
			endPoints.getPoint().setAnnotationXShift(-NOTIFICATION_SYMBOL_LENGTH/2);
			endPoints.getPoint().setAnnotationYShift(-1);
			
			
			// Turn off tooltips for the endpoints.
			symbol.setHoverAnnotationEnabled(false);
			
			
			if (m_NotificationBands == null)
			{
				m_NotificationBands = new ArrayList<Curve>();
			}
			
			// Add curve for the band.
			addCurve(indexToAdd+1);
			Curve band = getCurve(indexToAdd+1);
			band.setCurveData(notificationData);
			m_NotificationBands.add(band);
			
			symbol = band.getSymbol();
			symbol.setSymbolType(SymbolType.BOX_SOUTHEAST);	
			symbol.setBorderWidth(0);
			symbol.setHeight(NOTIFICATION_SYMBOL_LENGTH);
			symbol.setBackgroundColor(symbolColour);
			symbol.setBrushWidth(5);
			symbol.setHoverAnnotationSymbolType(SymbolType.ANCHOR_MOUSE);
			symbol.setHoverWidget(new GChartCentringHoverAnnotation());
			symbol.setHovertextTemplate(hovertextTemplate);
			symbol.setHoverSelectionEnabled(false);
			
			
			// Determine the length of the symbol used for the band.
			double xChartSize = getXChartSize();
			double xDateRange = getXAxis().getAxisMax() - getXAxis().getAxisMin();
			double pixelsPerMs =  xChartSize/xDateRange;
			double symbolLength = pixelsPerMs * (endTime.getTime() - startTime.getTime());
			int symbolWidth = (int)(Math.rint(symbolLength));
			
			symbol.setWidth(symbolWidth);
			symbol.setHoverSelectionWidth(symbolWidth+10);
			band.addPoint(startTime.getTime(), yPos);
		}
		else
		{
			symbol.setHoverWidget(new GChartCentringHoverAnnotation());
			symbol.setHovertextTemplate(hovertextTemplate);
			symbol.setBrushHeight(NOTIFICATION_SYMBOL_LENGTH);
			symbol.setBrushWidth(NOTIFICATION_SYMBOL_LENGTH);
			symbol.setHoverSelectionSymbolType(SymbolType.BOX_SOUTH);
			symbol.setHoverSelectionWidth(NOTIFICATION_SYMBOL_LENGTH);
			symbol.setHoverSelectionHeight(NOTIFICATION_SYMBOL_LENGTH);
		}
    }
	
	
	/**
	 * Removes the specified notification data from the chart.
	 * @param notificationData CausalityDataModel object defining the notifications
	 * 	to remove.
	 */
	public void removeNotifications(CausalityDataModel notificationData)
	{
		CausalityDataModel curveData;
		for (Curve endPoints : m_NotificationEndPoints)
		{
			curveData = (CausalityDataModel)(endPoints.getCurveData());
			if (curveData.equalsIgnoreMetrics(notificationData))
			{
				int index = getCurveIndex(endPoints);
				removeCurve(endPoints);
				m_NotificationEndPoints.remove(endPoints);
				
				if (notificationData.getStartTime().equals(notificationData.getEndTime()) == false)
				{
					// Remove the band.
					Curve band = getCurve(index);
					removeCurve(band);
					m_NotificationBands.remove(band);
				}
				
				break;
			}
		}
	}


	/**
	 * Returns whether the notification represented by the specified CausalityDataModel
	 * is displayed on the chart.
	 * @param causalityData notification type causality data model.
	 * @return <code>true</code> if the notification is currently displayed, 
	 * 	<code>false</code> otherwise.
	 */
	public boolean isNotificationOnChart(CausalityDataModel causalityData)
	{
		boolean onChart = false;
		
		if (m_NotificationEndPoints != null)
		{
			CausalityDataModel curveData;
			for (Curve endPoint : m_NotificationEndPoints)
			{
				curveData = (CausalityDataModel)(endPoint.getCurveData());
				if (curveData.equalsIgnoreMetrics(causalityData))
				{
					onChart = true;
					break;
				}
			}
		}
		
		return onChart;
	}
	
	
    @Override
    public void removeAll()
    {
    	removeAllNotifications();
    	
    	// Remove the time series and marker.
	    super.removeAll();
    }


	/**
     * Removes all notifications from the chart.
     */
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
    	
    	if (m_NotificationEndPoints != null)
    	{
    		for (Curve endPoints : m_NotificationEndPoints)
    		{
				removeCurve(endPoints);
    		}
    		
    		m_NotificationEndPoints.clear();
    	}
    	
    	if (m_NotificationBands != null)
    	{
    		for (Curve band : m_NotificationBands)
    		{
				removeCurve(band);
    		}
    		
    		m_NotificationBands.clear();
    	}
    }
    
    
    /**
	 * Returns the notification type causality data that the mouse is currently 
	 * touching i.e. the 'hovered over' point.
	 * @return the touched notification, or <code>null</code> if no notification
	 * 		type probable is currently hovered over.
	 */
    public CausalityDataModel getTouchedNotification()
	{
    	CausalityDataModel notification = null;
		
		Curve touchedCurve = getTouchedCurve();
		if (touchedCurve != null)
		{
			Object curveData = touchedCurve.getCurveData();
			if (curveData != null)
			{
				if (curveData.getClass() == CausalityDataModel.class)
				{
					CausalityDataModel causalityData = (CausalityDataModel)curveData;					
					if (causalityData.getDataSourceCategory() == DataSourceCategory.NOTIFICATION)
					{
						notification = causalityData;
					}
					
				}
				else if (curveData.getClass() == ProbableCauseModelCollection.class)
				{
					/*
					ProbableCauseModelCollection collection = (ProbableCauseModelCollection)curveData;
					
					if (collection.getDataSourceCategory() == DataSourceCategory.NOTIFICATION)
					{
						Point touchedPoint = getTouchedPoint();
						int collectionIndex = touchedCurve.getPointIndex(touchedPoint);
						ProbableCauseModel probCause = collection.getProbableCause(collectionIndex);
						
						notification = new EvidenceModel();
				    	notification.setId(probCause.getEvidenceId());
				    	notification.setDescription(probCause.getDescription());
				    	notification.setSource(probCause.getSource());
				    	notification.setDataType(probCause.getDataSourceName());
				    	notification.setTime(TimeFrame.SECOND, probCause.getTime());
					}
					*/
					// TODO - this can go once we no longer add ProbableCauseModelCollection objects.
					ProbableCauseModelCollection collection = (ProbableCauseModelCollection)curveData;
					
					if (collection.getDataSourceCategory() == DataSourceCategory.NOTIFICATION)
					{
						Point touchedPoint = getTouchedPoint();
						int collectionIndex = touchedCurve.getPointIndex(touchedPoint);
						ProbableCauseModel probCause = collection.getProbableCause(collectionIndex);
						
						notification = new CausalityDataModel();
				    	notification.setDescription(probCause.getDescription());
				    	notification.setSource(probCause.getSource());
				    	notification.setDataSourceType(probCause.getDataSourceType());
					}
				}
			}
		}
		
		return notification;
	}
	

    @Override
    public void setXChartSize(int xChartSize)
    {
	    super.setXChartSize(xChartSize);
	    
	    positionNotificationsX();
    }
    
    
    @Override
    public void setYChartSize(int yChartSize)
    {
	    super.setYChartSize(yChartSize);
	    
	    positionNotificationsY();
    }
    

    @Override
    public void setValueRange(double minValue, double maxValue)
    {
	    super.setValueRange(minValue, maxValue);
	    
	    positionNotificationsY();
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
	 * Adjusts the x positions of notification bands on the chart according to the
	 * current width of the chart.
	 */
	protected void positionNotificationsX()
	{
		// Resize notification bands according to width of chart.
	    if (m_NotificationBands != null)
	    {
	    	Curve endPoints;
	    	int endPointsIndex;
	    	CausalityDataModel notifications;
	    	Date startTime;
	    	Date endTime;
	    	int xChartSize = getXChartSize();
	    	
	    	for (Curve band : m_NotificationBands)
	    	{
	    		endPointsIndex = getCurveIndex(band) - 1;
	    		endPoints = getCurve(endPointsIndex);
	    		notifications = (CausalityDataModel)(endPoints.getCurveData());
	    		startTime = notifications.getStartTime();
	    		endTime = notifications.getEndTime();
				
				// Determine the length of the symbol used for the band.
				double xDateRange = getXAxis().getAxisMax() - getXAxis().getAxisMin();
				double pixelsPerMs =  xChartSize/xDateRange;
				double symbolLength = pixelsPerMs * (endTime.getTime() - startTime.getTime());
				int symbolWidth = (int)(Math.rint(symbolLength));
				
				Symbol symbol = band.getSymbol();
				symbol.setWidth(symbolWidth);
				symbol.setHoverSelectionWidth(symbolWidth+10);
	    	}
	    }
	}
	
	
	/**
	 * Adjusts the y positions of notification bands on the chart according to the
	 * current height of the chart.
	 */
	protected void positionNotificationsY()
	{
		// Set the y position of the notification bands.
	    if (m_NotificationEndPoints != null)
	    {
	    	ArrayList<Curve> workingList = new ArrayList<Curve>();
	    	workingList.addAll(m_NotificationEndPoints);
	    	
	    	m_NotificationEndPoints.clear();
	    	
	    	int bandIndex;
	    	Curve band;
	    	CausalityDataModel notifications;
	    	int numPoints;
	    	double yPos;
	    	for (Curve endPoints : workingList)
	    	{
	    		notifications = (CausalityDataModel)(endPoints.getCurveData());
	    		yPos = calculateNotificationY(notifications);
	    		
	    		numPoints = endPoints.getNPoints();
	    		endPoints.getPoint(0).setY(yPos);
	    		if (numPoints == 2)
	    		{
	    			endPoints.getPoint(1).setY(yPos);
	    			bandIndex = getCurveIndex(endPoints) + 1;
	    			band = getCurve(bandIndex);
	    			band.getPoint().setY(yPos);
	    		}
	    		
	    		m_NotificationEndPoints.add(endPoints);
	    	}
	    }
	}
	
	
	/**
	 * Calculates the y-position for a notification type probable cause collection.
	 * The calculation takes into account other collections with similar time values, 
	 * so that they do not overlap on the chart.
	 * @param notification type causality data to position.
	 * @return the y-position for the notification.
	 */
	protected double calculateNotificationY(CausalityDataModel notification)
	{
		double yPixelToModel = getYAxis().getAxisMax() / getYChartSize();
		double yPos = yPixelToModel * NOTIFICATION_SYMBOL_LENGTH;
		
		if (m_NotificationEndPoints.size() > 0)
		{
			// Calculate the minimum x value gap between points at which we
			// need to offset the y positions.
			double xPixelToModel = (getXAxis().getAxisMax()- getXAxis().getAxisMin())/ getXChartSize();
			long xLength = (long)xPixelToModel*NOTIFICATION_SYMBOL_LENGTH;	// Width of symbol in model.
			
			long msStart1 = notification.getStartTime().getTime();
			long msEnd1 = notification.getEndTime().getTime() + xLength;
			
			// Find the y positions of all notification 'bands' which overlap in 
			// time with this notification.
			long msStart2;
			long msEnd2;
			ArrayList<Double> yPositions = new ArrayList<Double>();
			
			Point startPoint;
			Point endPoint;
			for (Curve curve : m_NotificationEndPoints)
			{
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
	 * Creates the hover text template for a band of notification-type causality data
     * with the specified properties.
     * @return 
	 * @param description notification description.
	 * @param type data type.
	 * @param startTime time of the earliest notification.
	 * @param endTime time of the latest notification.
	 * @param count total notification count.
	 * @param sourceCount count of distinct sources (servers) on which the notification 
	 * 	has occurred.
	 * @param source if occurring on a single source, the name of that source (server).
	 * @return hover text tooltip for the notifications.
	 */
    protected String getHovertextTemplate(String description, String type, 
    		Date startTime, Date endTime, int count, int sourceCount, String source,
    		List<Attribute> attributes)
    {
    	ClientMessages messages = ClientUtil.CLIENT_CONSTANTS;
    	
    	StringBuilder template = new StringBuilder("<b>");
    	template.append(description);
    	template.append("</b><br>");
    	
    	template.append(messages.type());
    	template.append('=');
    	template.append(type);
    	template.append("<br>");
    	
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
    	template.append(count);
    	template.append("<br>");
    	
    	if (sourceCount == 1)
    	{
    		template.append(messages.source());
        	template.append('=');
        	template.append(source);
    	}
    	else
    	{
    		template.append(messages.sourceCount());
        	template.append('=');
        	template.append(sourceCount);
    	}
    	
    	if (attributes != null)
    	{
    		String attrVal;
    		for (Attribute attribute : attributes)
    		{
    			template.append("<br>");
    			attrVal = attribute.getAttributeValue();
    			if (attrVal != null)
    			{
		    		template.append(attribute.getAttributeName());
		    		template.append('=');
		    		template.append(attrVal);
    			}
    		}
    	}
    	
    	return GChart.formatAsHovertext(template.toString());
    }

}
