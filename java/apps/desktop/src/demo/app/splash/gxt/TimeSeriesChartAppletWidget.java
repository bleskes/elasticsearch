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
import java.util.List;

import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.widget.Html;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.google.gwt.core.client.GWT;
//import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

import demo.app.client.ClientUtil;
import demo.app.data.Evidence;
import demo.app.data.ListViewTool;
import demo.app.data.TimeFrame;
import demo.app.data.TimeSeriesConfig;
import demo.app.data.Tool;
import demo.app.data.UsageViewTool;


public class TimeSeriesChartAppletWidget extends Html implements TimeSeriesChartWidget
{
	private String 			m_ChartAppletId; 
	private final static int APPLET_WIDTH = 850;
	private final static int APPLET_HEIGHT = 500;
	
	private List<Tool> 		m_ViewTools;
	private boolean m_IsContextMenuBuilt = false;
	
	private List<TimeSeriesConfig>		m_TimeSeriesConfigs;
	

	/**
	 * Creates a new widget holding a time series chart applet.
	 * @param appletId id of the applet to use in the HTML <object> tag.
	 */
	public TimeSeriesChartAppletWidget(String appletId)
	{	
		m_ChartAppletId = appletId;
		
		m_TimeSeriesConfigs = new ArrayList<TimeSeriesConfig>();
		
		if (GWT.isScript() == false)
		{
			//setHTML("<h1>TimeSeriesChartAppletWidget applet id:" + m_ChartAppletId + "</h1>");
			setHtml("<h1>TimeSeriesChartAppletWidget applet id:" + m_ChartAppletId + "</h1>");
		}
		else
		{
			//setHTML(createAppletHTML());
			setHtml(createAppletHTML());
		}
	}
	
	
	/**
	 * Returns the user interface Widget itself.
	 * @return the Widget which is added in the user interface.
	 */
	public Widget getChartWidget()
	{
		return this;
	}
	
	
	/**
	 * Adds a time series to the chart.
	 * @param timeSeriesConfig TimeSeriesConfig object defining the properties of
	 * 		the time series to display (data type, metric, source, attributes etc).
	 */
    public void addTimeSeries(TimeSeriesConfig timeSeriesConfig)
    {
    	m_TimeSeriesConfigs.add(timeSeriesConfig);
    	
    	if (GWT.isScript() == false)
		{
			setHtml("<h1>TimeSeriesChartAppletWidget addTimeSeries() " + timeSeriesConfig + 
					" to applet id:" + m_ChartAppletId + "</h1>");
		}
		else
		{
			String attributeName = timeSeriesConfig.getAttributeName();
			String attributeValue = timeSeriesConfig.getAttributeValue();
			
			if (attributeName != null && attributeValue != null)
			{
				addTimeSeriesToApplet(timeSeriesConfig.getDataType(), 
						timeSeriesConfig.getMetric(),
						timeSeriesConfig.getSource(),
						attributeName, attributeValue);
			}
			else
			{
				addTimeSeriesToApplet(timeSeriesConfig.getDataType(), 
						timeSeriesConfig.getMetric(),
						timeSeriesConfig.getSource(), null, null);
			}
			
		}
    }
    
    
    /**
	 * Adds a time series to the chart, with the line connecting the data points
	 * drawn in the specified colour.
	 * @param timeSeriesConfig TimeSeriesConfig object defining the properties of
	 * 		the time series to display (data type, metric, source, attributes etc).
	 * @param color  line colour, specified using the CSS hex colour notation e.g '#ff0000'. 
	 */
	public void addTimeSeries(TimeSeriesConfig timeSeriesConfig, String color)
	{
		// TO DO.
	}
    
    
	/**
	 * Removes the time series from the chart.
	 * @param timeSeriesConfig TimeSeriesConfig object defining the properties of
	 * 		the time series to remove (data type, metric, source, attributes).
	 */
	public void removeTimeSeries(TimeSeriesConfig timeSeriesConfig)
	{
		// TO DO.
	}
    
    
	/**
	 * Adds the specified notification data into the chart. Note that this may 
	 * not represent a single item of evidence but a number of items aggregated by
	 * time and description.
	 * @param notification notification to add to the chart.
	 */
	public void addNotification(Evidence notification)
	{
		// TO DO.
	}
	
	
	/**
	 * Removes the specified notification data into the chart. Note that this may 
	 * not represent a single item of evidence but a number of items aggregated by
	 * time and description.
	 * @param notification notification to remove from the chart.
	 */
	public void removeNotification(Evidence notification)
	{
		// TO DO.
	}
    
    
    /**
	 * Removes all time series from the chart.
	 */
    public void removeAllTimeSeries()
    {
    	m_TimeSeriesConfigs.clear();
    	
    	if (GWT.isScript() == true)
    	{
    		removeAllTimeSeriesFromApplet();
    	}
    }
    
    
    /**
	 * Removes all notifications from the chart.
	 */
    public void removeAllNotifications()
    {
    	// TO DO.
    }
    
    
	/**
	 * Removes all data from the chart i.e. time series, notifications and
	 * time marker.
	 */
	public void removeAll()
	{
		removeAllTimeSeries();
		clearTimeMarker();
	}
    
    
	/**
	 * Adds a marker to the chart to highlight a particular time interval.
	 * @param startTime the lower bound of the time interval to mark.
	 * @param endTime the upper bound of the time interval to mark.
	 */
	public void setTimeMarker(Date startTime, Date endTime)
	{
		// TO DO.
	}
	
	
	/**
	 * Clears a time marker from the chart, if one has been added.
	 */
	public void clearTimeMarker()
	{
		// TO DO.
	}


    /**
	 * Loads the data in the time series chart widget according to its 
	 * current configuration.
	 */
	public void load()
	{
		if (GWT.isScript() == false)
		{

		}
		else
		{
			// Check that the context menu items have been added to the applet.
			if (m_IsContextMenuBuilt == false)
			{
				addViewToolsToApplet();
				//m_IsContextMenuBuilt = true;
			}
			
			loadDataInApplet();
		}
	}
	
	
	/**
	 * Loads the data for the specified time series configuration into the
	 * chart. The lower and upper time bounds for the data loaded will be
	 * set according to the current time span of the chart.
	 */
	public void load(TimeSeriesConfig timeSeriesConfig)
	{
		// TO DO.
	}
	
	
	/**
	 * Sets the flag that determines whether or not the tick labels on the value
	 * axis are visible.
	 * @param visible <code>true</code> for the tick labels to be visible (the
	 * 	default), <code>false</code> otherwise.
	 */
	public void setValueTickLabelsVisible(boolean visible)
	{
		// TO DO.
	}
	
	
	/**
	 * Sets a flag that determines whether or not the value axis range is 
	 * automatically adjusted to fit the data.
	 * @param auto auto range flag.
	 */
	public void setAutoValueRange(boolean auto)
	{
		// TO DO.
	}
	
	
	/**
	 * Sets the value range for the chart. This defines the lower and upper bounds
	 * displayed on the value (y) axis.
	 * @param minValue minimum value visible on the value axis.
	 * @param maxValue maximum value visible on the value axis.
	 */
	public void setValueRange(double minValue, double maxValue)
	{
		// TO DO.
	}
	
	
	/**
	 * Sets the date range for the chart. This defines the start and end times
	 * of the time series data to load.
	 * @param startTime start time for the chart.
	 * @param endTime end time for the chart.
	 */
	public void setDateRange(Date startTime, Date endTime)
	{
		if (GWT.isScript() == true)
    	{
			String start = ClientUtil.formatTimeField(startTime, TimeFrame.SECOND);
			String end = ClientUtil.formatTimeField(endTime, TimeFrame.SECOND);
			setDateRangeInApplet(start, end);
    	}
		else
		{
			GWT.log("TimeSeriesChartAppletWidget setDateRange() - " + 
					startTime + " - " + endTime, null);
		}
	}
	
	
	/**
	 * Returns whether time series features should be highlighted on the chart.
     * @return <code>true</code> if any features are to be highlighted, 
     * 	<code>false</false> otherwise.
     */
    public boolean isHighlightingFeatures()
    {
    	return false;
    }


	/**
	 * Sets whether time series features should be highlighted on the chart.
     * @param markFeatues <code>true</code> if any features are to be highlighted, 
     * 	<code>false</false> otherwise.
     */
    public void setHighlightingFeatures(boolean highlight)
    {
    	// TO DO.
    }
	
	
	/**
	 * Zooms in the date axis, centred around the current midpoint of the date range.
	 */
	public void zoomInDateAxis()
	{
		// TO DO.
	}
	
	
	/**
	 * Zooms out the date axis, centred around the current midpoint of the date range.
	 */
	public void zoomOutDateAxis()
	{
		// TO DO.
	}
	
	
	/**
	 * Pans the chart to the left.
	 */
	public void panLeft()
	{
		// TO DO.
	}
	
	
	/**
	 * Pans the chart to the right.
	 */
	public void panRight()
	{
		// TO DO.
	}
	
	
    /**
     * Sets the list of tools for launching other views from the chart.
     * @param tools the list of tools for launching other view types.
     */
    public void setViewTools(List<Tool> tools)
    {
    	m_ViewTools = tools;
    	
    	// Flag up that the context menu tools need adding to the applet.
    	m_IsContextMenuBuilt = false; 
    }
    
    
    /**
     * Returns the date/time of the point in the chart that is currently 'selected' 
     * e.g. when a context menu item has been run against a point in the chart.
     * @return the date/time of the selected point, or <code>null</code> if no
     * 		point is currently selected.
     */
    public Date getSelectedTime()
    {
    	// TO DO.
    	return null;
    }
    
    
    /**
     * Returns the id of the notification that is currently 'selected' in the
     * chart e.g. when a context menu item has been run against a notification.
     * @return the id of the notification that is selected, or -1 if no notification
     * 		is currently selected.
     */
    public int getSelectedNotificationId()
    {
    	// TO DO.
    	return -1;
    }
    
    
    /**
	 * Returns the time series that is currently 'selected' in the chart 
	 * e.g. when a context menu item has been run against a time series data point.
	 * @return TimeSeriesConfig object defining the properties of the time series
	 *  	or <code>null</code> if no time series is currently selected.
	 */
    public TimeSeriesConfig getSelectedTimeSeries()
    {
    	// TO DO.
    	return null;
    }
    
    
    /**
     * Returns the id of the time series feature that is currently 'selected' in the
     * chart e.g. when a context menu item has been run against a feature.
     * @return the id of the feature that is selected, or -1 if no feature
     * 		is currently selected.
     */
    public int getSelectedTimeSeriesFeatureId()
    {
    	// TO DO.
    	return -1;
    }
    
    
    /**
     * Adds the tools for launching other views into the applet's right-click
     * context menu once the applet is loaded.
     */
    protected void addViewToolsToApplet()
    {
    	if (m_ViewTools != null)
    	{
	    	for (Tool tool : m_ViewTools)
	    	{
	    		if (tool.getClass() == ListViewTool.class)
	    		{
	    			ListViewTool listTool = (ListViewTool)tool;
	    			GWT.log("TimeSeriesChartAppletWidget add tool - " + 
	    					listTool.getName() + " to open type " + listTool.getViewToOpen(), null);
	    			if (GWT.isScript() == true)
	    			{
	    				addViewToolToApplet(listTool.getName(), listTool.getViewToOpen());
	    			}
	    		}
	    		else if (tool.getClass() == UsageViewTool.class)
	    		{
	    			UsageViewTool timeSeriesTool = (UsageViewTool)tool;
	    			GWT.log("TimeSeriesChartAppletWidget add tool - " + 
	    					timeSeriesTool.getName() + " to open type " + timeSeriesTool.getViewToOpen(), null);
	    			if (GWT.isScript() == true)
	    			{
	    				addViewToolToApplet(timeSeriesTool.getName(), 
	    						timeSeriesTool.getViewToOpen());
	    			}
	    		}
	    	}
    	}
    }
    
	
    /**
     * Sets the chart title to the specified text.
     * @param text the text for the title.
     */
    @Override
    public void setChartTitle(String text)
    {
    	if (GWT.isScript() == true)
    	{
    		try
    		{
    			setChartTitleInApplet(text);
    		}
    		catch (Exception e)
    		{
    			MessageBox.alert("setChartTitle", "Exception: " + e.getMessage(), null);
    		}
    	}
    }


    /**
     * Sets the chart subtitle to the specified text.
     * @param text the text for the subtitle).
     */
	@Override
    public void setChartSubtitle(String text)
    {
		if (GWT.isScript() == true)
    	{
			setChartSubtitleInApplet(text);
    	}
    }
	

    public void setChartWidth(int width)
    {
	    // TODO Auto-generated method stub
    }
    
    
    public void setChartHeight(int height)
    {
	    // TODO Auto-generated method stub
    }


	public void appletViewToolRun(String viewToOpenDataType)
	{
		MessageBox.alert("TimeSeriesChartApplet", "Applet - " + m_ChartAppletId + 
				", " + viewToOpenDataType, null);
	}


	/**
	 * Adds the time series defined by the specified parameters into the applet.
	 */
	public native void addTimeSeriesToApplet(String dataType, String metric, String source,
			String attributeName, String attributeValue) /*-{ 
		var appletId = this.@demo.app.splash.gxt.TimeSeriesChartAppletWidget::m_ChartAppletId;
    	$doc.getElementById(appletId).addTimeSeries(dataType, metric, source, attributeName, attributeValue); 
	}-*/;
	
	
	/**
	 * Removes all time series from the applet.
	 */
	public native void removeAllTimeSeriesFromApplet() /*-{ 
		var appletId = this.@demo.app.splash.gxt.TimeSeriesChartAppletWidget::m_ChartAppletId;
		$doc.getElementById(appletId).removeAllTimeSeries(); 
}-*/;
	
	
	/**
	 * Loads the data in the chart applet.
	 */
	public native void loadDataInApplet() /*-{ 
		var appletId = this.@demo.app.splash.gxt.TimeSeriesChartAppletWidget::m_ChartAppletId;
    	$doc.getElementById(appletId).load(); 
}-*/;
	
	
	/**
	 * Sets the date range in the chart applet.
	 */
	public native void setDateRangeInApplet(String startTime, String endTime) /*-{ 
		var appletId = this.@demo.app.splash.gxt.TimeSeriesChartAppletWidget::m_ChartAppletId;
    	$doc.getElementById(appletId).setDateRange(startTime, endTime); 
	}-*/;
	
	
	/**
	 * Adds a tool for opening other views into the applet.
	 */
	public native void addViewToolToApplet(String toolText, String viewToOpenDataType) /*-{ 
		var appletId = this.@demo.app.splash.gxt.TimeSeriesChartAppletWidget::m_ChartAppletId;
    	$doc.getElementById(appletId).addViewTool(toolText, viewToOpenDataType); 
	}-*/;
	
	
	/**
	 * Sets the title in the chart applet.
	 */
	public native void setChartTitleInApplet(String title) /*-{ 
		try
		{
			var appletId = this.@demo.app.splash.gxt.TimeSeriesChartAppletWidget::m_ChartAppletId;
    		$doc.getElementById(appletId).setChartTitle(title); 
    	}
    	catch (e)
    	{
    		alert("JS error: " + e);
    	}
	}-*/;
	
	
	/**
	 * Sets the title in the chart applet.
	 */
	public native void setChartSubtitleInApplet(String title) /*-{ 
		var appletId = this.@demo.app.splash.gxt.TimeSeriesChartAppletWidget::m_ChartAppletId;
    	$doc.getElementById(appletId).setChartSubtitle(title); 
	}-*/;
	
	
	/**
	 * Creates the HTML to embed the applet in the web page.
	 * @return the applet HTML.
	 */
	private String createAppletHTML() 
	{ 
		
		String archiveList = createAppletJarsList();
		
        StringBuffer buff = new StringBuffer(); 
        
        buff.append("<!--[if !IE]> -->"); 
        buff.append("<object classid= \"java:demo.app.splash.swing.TimeSeriesChartApplet.class\""); 
        buff.append(" type=\"application/x-java-applet\""); 
        buff.append(" archive=\"" + archiveList + "\""); 
        buff.append(" height=\"" + APPLET_HEIGHT+ "\" width=\"" + APPLET_WIDTH + "\""); 
        buff.append(" id=\"" + m_ChartAppletId + "\" name=\"" + m_ChartAppletId + "\""); 
        buff.append(" >"); 
        //buff.append("<param name=\"code\" value= \"com.optilab.sargas.applets.MyApplet.class\" />"); 
        buff.append("<param name=\"archive\" value=\"" + archiveList + "\" />"); 
        //buff.append("<param name=\"persistState\" value=\"false\" / >"); 
        buff.append("<param name=\"persistState\" value=\"true\" / >");
        buff.append("<param name=\"mayscript\" value=\"yes\" />"); 
        buff.append("<param name=\"scriptable\" value=\"true\" />"); 
        buff.append("<param name=\"servletUrl\" value=\"" + GWT.getModuleBaseURL() + "someservlet\" />"); 
        buff.append("<param name=\"chartId\" value=\"" + m_ChartAppletId + "\" />");
        buff.append("<center>"); 
        buff.append("<p><strong>ChartApplet content requires Java 1.5 or higher, which your browser does not appear to have.</strong></p>"); 
        buff.append("<p><a href=\"http://www.java.com/en/download/index.jsp\">Get the latest Java Plug-in.</a></p>"); 
        buff.append("</center>"); 
        buff.append("</object>"); 
        buff.append("<!--<![endif]-->"); 
        
        buff.append("<!--[if IE]>"); 
        buff.append("<object classid=\"clsid:8AD9C840-044E-11D1-B3E9-00805F499D93\""); 
        buff.append(" codebase=\"http://java.sun.com/products/plugin/autodl/jinstall-1_4-windows-i586.cab#Version=1,4,0,0\""); 
        buff.append(" height=\"" + APPLET_HEIGHT+ "\" width=\"" + APPLET_WIDTH + "\"");  
        buff.append(" id=\"" + m_ChartAppletId + "\" name=\"" + m_ChartAppletId + "\""); 
        buff.append(" >"); 
        buff.append("<param name=\"code\" value=\"demo.app.splash.swing.TimeSeriesChartApplet.class\" />"); 
        buff.append("<param name=\"archive\" value=\"" + archiveList + "\" />"); 
       // buff.append("<param name=\"persistState\" value=\"false\" />"); 
        buff.append("<param name=\"persistState\" value=\"true\" />"); 
        buff.append("<param name=\"mayscript\" value=\"yes\" />"); 
        buff.append("<param name=\"scriptable\" value=\"true\" />"); 
        buff.append("<param name=\"servletUrl\" value=\"" + GWT.getModuleBaseURL() + "someservlet\" />"); 
        buff.append("<param name=\"chartId\" value=\"" + m_ChartAppletId + "\" />");
        buff.append("<center>"); 
        buff.append("<p><strong>ChartApplet content requires Java 1.5 or higher, which your browser does not appear to have.</strong></p>"); 
        buff.append("<p><a href=\"http://www.java.com/en/download/index.jsp\">Get the latest Java Plug-in.</a></p>"); 
        buff.append("</center>"); 
        buff.append("</object>"); 
        buff.append("<![endif]-->"); 
        
        return buff.toString(); 
    } 
	
	
	/**
	 * Creates the list of JAR files for inclusion in the chart applet <object> tag.
	 * @return comma-separated list of applet JAR files.
	 */
	private String createAppletJarsList()
	{
		StringBuffer buff = new StringBuffer(); 
		
		buff.append(GWT.getHostPageBaseURL());
		buff.append("splashApplet.jar, ");
		buff.append(GWT.getHostPageBaseURL());
		buff.append("jfreechart-1.0.13.jar, ");
		buff.append(GWT.getHostPageBaseURL());
		buff.append("jcommon-1.0.16.jar");
		
		return buff.toString(); 
	}

}
