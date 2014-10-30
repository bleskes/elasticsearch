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

package demo.app.splash.gxt;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.BaseObservable;
import com.extjs.gxt.ui.client.event.EventType;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.Observable;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;
import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.HoverParameterInterpreter;
import com.googlecode.gchart.client.GChart.Curve;
import com.googlecode.gchart.client.GChart.SymbolType;
import com.googlecode.gchart.client.GChart.Curve.Point;

import demo.app.client.CSSSeverityColors;
import demo.app.client.ClientUtil;
import demo.app.data.Evidence;
import demo.app.data.Severity;
import demo.app.data.TimeSeriesConfig;
import demo.app.data.TimeSeriesData;
import demo.app.data.TimeSeriesDataPoint;


/**
 * Extension of the Google GChart for plotting time series data.
 * @author Pete Harverson
 */
public class TimeSeriesGChart extends GChart implements Observable
{
	
	private Menu 					m_ContextMenu;
	
	private HashMap<TimeSeriesConfig, Curve>	m_TimeSeriesCurves; 
	private HashMap<TimeSeriesConfig, List<GChartFeatureCurve>>	m_FeaturesCurves; 
	
	private HashMap<Integer, Curve>	m_NotificationCurves; // Curve vs evidence id.
	
	private Curve					m_TimeLine;
	
	private boolean					m_AutoValueRange;
	
	private BaseObservable 			m_Observable;
	
	protected static final int NOTIFICATION_SYMBOL_LENGTH = 10;
	
	
	
	/**
	 * Creates a new chart for displaying time series data.
	 */
	public TimeSeriesGChart()
	{
		addStyleName("time-series-gchart");

		setBackgroundColor(USE_CSS);
		setBorderStyle(USE_CSS); 
		
		setChartSize(800, 410);
		setChartTitleThickness(0);
		setPadding("3px");
		setClipToPlotArea(true);
		
		// Set up the template for displaying tooltips on points.
		setHoverParameterInterpreter(new ChartHoverParameterInterpreter());
		
		// Configure the date/time x-axis.
		getXAxis().setAxisLabel("<b>" + ClientUtil.CLIENT_CONSTANTS.time() + "</b>");
		getXAxis().setHasGridlines(false);
		getXAxis().setTickCount(8);
		getXAxis().setTickLength(4);
		getXAxis().setTicksPerLabel(2);
		getXAxis().setAxisLabelThickness(18);
		// Except for "=(Date)", a standard GWT DateTimeFormat string
		getXAxis().setTickLabelFormat("=(Date)MMM dd HH:mm");
		
		m_AutoValueRange = true;


		// Configure the value y-axis.
	    getYAxis().setHasGridlines(false);
	    getYAxis().setTickCount(5);
	    getYAxis().setTickLength(4); 
	    getYAxis().setTickLabelFormat("#,##0");
	    getYAxis().setTickLabelThickness(100);
	    getYAxis().setAxisMin(0);
	    
	    m_Observable = new BaseObservable();
	}
	
	
	/**
	 * Sets the chart's context menu.
	 * @param menu the context menu
	 */
	public void setContextMenu(Menu menu)
	{
		m_ContextMenu = menu;
		sinkEvents(GXT.isSafari && GXT.isMac ? Event.ONMOUSEDOWN : Event.ONMOUSEUP);
		sinkEvents(Event.ONCONTEXTMENU);
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
		symbol.setHovertextTemplate(getHovertextTemplate(config));
		
		// Removes the symbols themselves, leaving only the line.
		symbol.setHeight(0);
		symbol.setWidth(0);
		symbol.setBrushSize(10, 10);
		
	    // Add the dataset.
	    List<TimeSeriesDataPoint> dataPoints = timeSeriesData.getDataPoints();
	    
		for (TimeSeriesDataPoint dataPoint : dataPoints)
		{
			curve.addPoint(dataPoint.getTime().getTime(), dataPoint.getValue());
			if (dataPoint.getFeature() != null)
			{
				addTimeSeriesFeature(config, dataPoint);
			}
		}
		
		m_TimeSeriesCurves.put(config, curve);
		
		if (m_AutoValueRange == true)
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
		String hoverTemplate = GChart.formatAsHovertext(feature.getDescription() + ", ${fullTime}");
		symbol.setHovertextTemplate(hoverTemplate);
		
		curve.addPoint(dataPoint.getTime().getTime(), dataPoint.getValue());
		
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
		//String hoverTemplate = GChart.formatAsHovertext(
		//		notification.getDescription() + ", " + notification.getSource() + ", ${fullTime}");
		
		String hoverTemplate = GChart.formatAsHovertext(notification.getDescription() + ", ${fullTime}");
		
		symbol.setHovertextTemplate(hoverTemplate);
		
		double yPos = calculateNotificationY(notification.getTime());
		curve.addPoint(notification.getTime().getTime(), yPos);
		
		m_NotificationCurves.put(notification.getId(), curve);	
		
		if (m_AutoValueRange == true)
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
		
		if (m_AutoValueRange == true)
		{
			double yAxisMax = calculateYAxisMax();
			getYAxis().setAxisMax(yAxisMax);
		}
	}
	
	
	/**
	 * Adds a 'timeline' to the usage chart to indicate a specific time.
	 * @param date date for the time line.
	 */
	public void setTimeMarker(Date startTime, Date endTime)
	{
		if (endTime == null)
		{
			endTime = new Date(startTime.getTime() + (1000*60*15) );
			
			// TO DO: Mark on as a band between start and end times.
			long timeGap = endTime.getTime() - startTime.getTime();

			double startmodelClient = getXAxis().modelToPixel(startTime.getTime());
			double endtmodelClient = getXAxis().modelToPixel(endTime.getTime());
		}
		
		
		// Create the timeline if one has not yet been created.
		if (m_TimeLine == null)
		{
			addCurve();
			m_TimeLine = getCurve();
			getCurve().getSymbol().setSymbolType(SymbolType.BOX_CENTER);
		    getCurve().getSymbol().setWidth(2);
		    getCurve().getSymbol().setHeight(2);
		    getCurve().getSymbol().setBorderWidth(0);
		    getCurve().getSymbol().setBackgroundColor("navy");
		    getCurve().getSymbol().setFillThickness(2);
		    getCurve().getSymbol().setFillSpacing(5);
			getCurve().getSymbol().setHovertextTemplate("${x}");
		}
		else
		{
			m_TimeLine.clearPoints();
		}
		
		m_TimeLine.addPoint(startTime.getTime(), 0d);
		
		double yAxisMax;
		if (m_AutoValueRange == true)
		{
			yAxisMax = calculateYAxisMax();
		}
		else
		{
			yAxisMax = getYAxis().getAxisMax();
		}
		m_TimeLine.addPoint(startTime.getTime(), yAxisMax);
		
	}
	
	
	/**
	 * Removes the timeline if one has been added to the chart.
	 */
	public void clearTimeMarker()
	{
		if (m_TimeLine != null)
		{
			removeCurve(m_TimeLine);
			m_TimeLine = null;
		}
	}
	
	
	/**
	 * Sets the text for the title of the chart.
	 * @param title text to use for the title of the chart.
	 * @param subTitle text, if any, to use for the subtitle of the chart.
	 */
	public void setTitleText(String title, String subTitle)
	{
		String txt = (title != null ? title : "");
		
		String chartTitle = "<h1 class=\"time-series-gchart\">";
		chartTitle += txt;
		chartTitle += "</h1>";
		
		if (subTitle != null)
		{
			chartTitle += "<h2 class=\"time-series-gchart\">";
			chartTitle += subTitle;
			chartTitle += "</h2>";
		}
		
		setChartTitle(chartTitle);
	}
	
	
	/**
	 * Returns the time value of the point that the mouse is currently touching
	 * i.e. the 'hovered over' point.
	 * @return the time of the touched point, or <code>null</code> if no point
	 * is currently hovered over.
	 */
	public Date getTouchedPointTime()
	{
		Date touchedPointTime = null;
		
		Point touchedPoint = getTouchedPoint();
		if (touchedPoint != null)
		{
			double touchedPointX = touchedPoint.getX();
			touchedPointTime = new Date((long)touchedPointX);
		}
		
		return touchedPointTime;
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
	 * Returns the time corresponding to the start of the date axis.
	 * @return the starting date on the x-axis.
	 */
	public Date getDateAxisStart()
	{
		double xAxisMin = getXAxis().getAxisMin();
		return new Date((long)xAxisMin);
	}
	
	
	/**
	 * Specifies the starting point of the date axis.
	 * @param start the start date/time for the x-axis.
	 */
	public void setDateAxisStart(Date start)
	{
		// Note that any overlapping notifications will not be re-positioned.
		getXAxis().setAxisMin(start.getTime());
	}
	
	
	/**
	 * Returns the time corresponding to the end of the date axis.
	 * @return the end date on the x-axis.
	 */
	public Date getDateAxisEnd()
	{
		// Note that any overlapping notifications will not be re-positioned.
		double xAxisMax = getXAxis().getAxisMax();
		return new Date((long)xAxisMax);
	}
	
	
	/**
	 * Sets the end point of the date axis.
	 * @param end the end date/time for the x-axis.
	 */
	public void setDateAxisEnd(Date end)
	{
		getXAxis().setAxisMax(end.getTime());
	}
	
	
	/**
	 * Sets a flag that determines whether or not the value axis range is 
	 * automatically adjusted to fit the data.
	 * @param auto auto range flag.
	 */
	public void setAutoValueRange(boolean auto)
	{
		m_AutoValueRange = auto;
	}
	
	
	public void addListener(EventType eventType, Listener listener)
	{
		m_Observable.addListener(eventType, listener);
	}


    public boolean fireEvent(EventType eventType, BaseEvent be)
    {
	    return m_Observable.fireEvent(eventType, be);
    }
    
    
    public List<Listener<? extends BaseEvent>> getListeners(EventType eventType)
    {
    	return m_Observable.getListeners(eventType);
    }


	public boolean hasListeners()
    {
	    return m_Observable.hasListeners();
    }


    public boolean hasListeners(EventType eventType)
    {
	    return m_Observable.hasListeners(eventType);
    }


    public void removeAllListeners()
    {
		m_Observable.removeAllListeners();
    }


    public void removeListener(EventType eventType, Listener listener)
    {
		m_Observable.removeListener(eventType, listener);
    }
	
	
	/**
     * Delegates handling of any 'sunk' browser events to any registered listeners.
     * @param event the browser event that has been received.
     */
    public void onBrowserEvent(Event event) 
    {
    	super.onBrowserEvent(event);
    	
        //if (event.getTypeInt() == (GXT.isSafari && GXT.isMac ? Event.ONMOUSEDOWN : Event.ONMOUSEUP)
		//        && isRightClick(event))
    	// Note that the ONCONTEXTMENU does not fire in Opera.
    	if (event.getTypeInt() == Event.ONCONTEXTMENU)
		{
        	onContextMenu(event);
		}
    	
    	if (event.getTypeInt() == Event.ONDBLCLICK)
    	{
    		// NB. Use com.extjs.gxt.ui.client.event.Events constant due to 
    		// behaviour of BaseObservable.getListeners(EventType) and lack of
    		// equals() method in com.extjs.gxt.ui.client.event.EventType.
    		fireEvent(Events.OnDoubleClick, new BaseEvent(this));
    	}
    	else
    	{
    		fireEvent(new EventType(event.getTypeInt()), new BaseEvent(this));
    	}
    	
    	// Pass event on (a GChart should respond to mouse activity  - but never eat it).
        //super.onBrowserEvent(event);
    }
    
    
    /**
     * Fires when a context menu event is received, displaying the context
     * menu at the coordinates on the chart that the event was triggered.
     * @param event the context menu event.
     */
    protected void onContextMenu(Event event)
	{
		if (m_ContextMenu != null)
		{
			event.stopPropagation();
        	event.preventDefault();
        	
        	Point touchedPoint = getTouchedPoint();
        	if (touchedPoint != null)
        	{        		
				final int x = event.getClientX();
				final int y = event.getClientY();
	
					DeferredCommand.addCommand(new Command()
					{
						public void execute()
						{
							m_ContextMenu.showAt(x, y);
						}
					});
        	}
		}
	}
    
    
    /**
	 * Returns <code>true</code> if the event is a right click.
	 * 
	 * @return the right click state
	 */
	protected boolean isRightClick(Event event)
	{
		if (event != null)
		{
			if (DOM.eventGetButton(event) == Event.BUTTON_RIGHT
			        || (GXT.isMac && DOM.eventGetCtrlKey(event)))
			{
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * Calculates the maximum value to be displayed on the y axis.
	 * @param records the records being displayed in the chart.
	 * @return minimum value visible on the y axis.
	 */
	protected double calculateYAxisMax()
	{
		// Minimum max value on the y-axis is 10 for the case where there are 
		// no records, or the maximum usage value is less than 10.
		double maxVal = Math.max(9, getYAxis().getDataMax()); 	
		
		double powerFloor = Math.floor(Math.log10(maxVal)); // e.g. gives 5 for 416521
		double raiseToPower = Math.pow(10, powerFloor); // e.g. 100000 for 416521
		
		double nearestMax = (Math.floor(maxVal/raiseToPower) + 1) * raiseToPower; // e.g. 500000 for 416521
	
		return nearestMax;
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
    	String dataType = config.getDataType();
    	String metric = new String(config.getMetric());
    	String source = config.getSource();
    	String attributeName = config.getAttributeName();
    	String attributeValue = config.getAttributeValue();
    	
    	StringBuilder template = new StringBuilder(dataType);
    	template.append(',');
    	template.append(metric);
    	template.append(',');
    	
    	if (source != null)
    	{
    		template.append(source);
    	}
    	else
    	{
    		template.append("all sources");
    	}
    	
    	if ( (attributeName != null) && (attributeValue != null) )
    	{
    		template.append(',');
    		template.append(attributeName);
    		template.append('=');
    		template.append(attributeValue);
    	}
    	
    	template.append(", ${fullTime}");
    	
    	if (config.getScalingFactor() == 1)
    	{
    		// TO DO: show value for scaled time series
    		// - need to fix rounding errors for double-int conversion.
    		template.append(", ${y}");
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
						result = "" + value;
					}
				}
			}

			return result;
		}

	}
	
	
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
