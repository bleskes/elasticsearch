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

package demo.app.splash.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.data.time.DateRange;
import static demo.app.data.DateTimeFormatPatterns.*;
import demo.app.data.TimeSeriesConfig;
import demo.app.data.TimeSeriesData;
import demo.app.data.TimeSeriesDataPoint;

import netscape.javascript.JSObject;


/**
 * Applet containing a chart for displaying time series data.
 * @author Pete Harverson
 */
public class TimeSeriesChartApplet extends JApplet
{
	private List<TimeSeriesConfig>		m_TimeSeriesConfigs;

	private TimeSeriesJFreeChart	m_JFreeChart;
	
	private Date					m_StartDate;
	private Date					m_EndDate;
	
	private List<String>			m_ViewToolKeys;
	private JSeparator				m_ViewToolsSeparator;
	private LinkedHashMap<String, JMenuItem>	m_ViewMenuItems;
	
	public static final String	CHARACTER_ENCODING = "UTF-8";	// TO DO: Move to separate I18N constants class.
	


	@Override
    public void init()
	{
		String chartId = getParameter("chartId");
		
		System.out.println("TimeSeriesChartApplet.init() " + chartId + 
				", codebase: " + getCodeBase());	// e.g. http://localhost:8080/splash/
		
		addComponentListener(new ComponentAdapter(){

            @Override
            public void componentShown(ComponentEvent e)
            {
	            System.out.println("TimeSeriesChartApplet componentShown()");
            }
		});
		
		// Create UI in the event-dispatching thread.
		try
		{
			SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					initComponents();
				}
			});
		}
		catch (Exception e)
		{
			System.err.println("initComponents() didn't complete successfully: " + e);
			System.err.println("Exception cause: " + e.getCause());
		}

		m_TimeSeriesConfigs = new ArrayList<TimeSeriesConfig>();
		m_ViewMenuItems = new LinkedHashMap<String, JMenuItem>();
		
		System.out.println("TimeSeriesChartApplet.init() complete " + hashCode());
	}
	
	
    @Override
    public void stop()
    {
    	String chartId = getParameter("chartId");
    	System.out.println("TimeSeriesChartApplet.stop() " + chartId + " " + hashCode());
    	
    	JSObject window = JSObject.getWindow(TimeSeriesChartApplet.this);
        window.call("notifyChartAppletStopped", new Object[]{chartId});
    }


	/**
	 * Initialises the components in the applet.
	 */
	protected void initComponents()
	{
		m_JFreeChart = new TimeSeriesJFreeChart();
		
		m_JFreeChart.addDateAxisChangeListener(new AxisChangeListener(){

			@Override
            public void axisChanged(AxisChangeEvent event)
            {
	            // Reload the data for the new date range.
				ValueAxis axis = (ValueAxis)(event.getAxis());
				
				DateRange dateRange = (DateRange)(axis.getRange());
				if (dateRange != null)
				{
					m_StartDate = dateRange.getLowerDate();
					m_EndDate = dateRange.getUpperDate();
					load();
				}
            }
			
		});
		
		// Set up a listener for the Popup menu.
		m_ViewToolsSeparator = new JSeparator();
		JPopupMenu popupMenu = m_JFreeChart.getPopupMenu();
		popupMenu.addPopupMenuListener(new PopupMenuListener(){

			@Override
            public void popupMenuCanceled(PopupMenuEvent e)
            {
	            
            }

			@Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
            {
				hideViewToolMenuItems();
            }

			@Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e)
            {
				if (m_JFreeChart.isDataItemSelected())
				{
					showViewToolMenuItems();
				}
            }
			
		});

		getContentPane().add(m_JFreeChart);
		
		System.out.println("TimeSeriesChartApplet initComponents() done");
	}
	
	
	/**
	 * Adds a tool for opening other data type views into the applet's right-click
	 * context menu.
	 * @param toolText the text label for the menu item.
	 * @param viewToOpenDataType the data type of the view to open e.g. p2pslogs, system_udp.
	 */
	public void addViewTool(String toolText, String viewToOpenDataType)
	{
		if (m_ViewMenuItems.containsKey(viewToOpenDataType) == false)
		{
			RunViewToolAction runAction = new RunViewToolAction(toolText, viewToOpenDataType);
			m_ViewMenuItems.put(viewToOpenDataType, new JMenuItem(runAction));
		}
	}
	
	
	/**
	 * Adds a time series to the chart in the applet.
	 * @param dataType data type of the time series e.g. system_udp, p2psmon_users.
	 * @param metric the time series metric
	 * @param source the name of the source (server), or <code>null</code> if the
	 * 		data is from all sources.
	 */
	public void addTimeSeries(String dataType, String metric, String source)
	{
		addTimeSeries(dataType, metric, source, null, null);
	}
	
	
	/**
	 * Adds a time series to the chart in the applet.
	 * @param dataType data type of the time series e.g. system_udp, p2psmon_users.
	 * @param metric the time series metric
	 * @param source the name of the source (server), or <code>null</code> if the
	 * 		data is from all sources.
	 * @param attributeName the name of the attribute, if any, for the time series.
	 * @param attributeValue the value of the attribute, if any.
	 */
	public void addTimeSeries(String dataType, String metric, String source,
			String attributeName, String attributeValue)
	{
		TimeSeriesConfig config = new TimeSeriesConfig(
				dataType, metric, source, attributeName, attributeValue); 
		System.out.println("addTimeSeries() config:" + config);
		m_TimeSeriesConfigs.add(config);
	}
	
	
	/**
	 * Remove all time series from the chart in the applet.
	 */
	public void removeAllTimeSeries()
    {
		System.out.println("removeAllTimeSeries()");
		m_TimeSeriesConfigs.clear();
		m_JFreeChart.removeAllTimeSeries();
    }
	
	
	/**
	 * Loads all the time series and notification data configured in the chart.
	 */
	public void load()
	{
		System.out.println("load()");
		
		
		if (isShowing() == false)
		{
			System.out.println("load() - not showing");
			return;
		}		
		
		
		try
        {
			String spec;
			
			SimpleDateFormat dateFormatter = new SimpleDateFormat(SECOND_PATTERN);
			
			for (TimeSeriesConfig config : m_TimeSeriesConfigs)
			{
				spec = "splash/services/timeSeriesQueryService?action=getDataPoints";
				spec += "&dataType=";
				spec += encodeURLParameter(config.getDataType());
				spec += "&metric=";
				spec += encodeURLParameter(config.getMetric());
				if (config.getSource() != null)
				{
					spec += "&source=";
					spec += config.getSource();
				}
				if (config.getAttributeName() != null && config.getAttributeValue() != null)
				{
					spec += "&attributeName=";
					spec += config.getAttributeName();
					spec += "&attributeValue=";
					spec += encodeURLParameter(config.getAttributeValue());
				}
				if (m_StartDate != null)
				{
					spec += "&minTime=";
					spec += encodeURLParameter(dateFormatter.format(m_StartDate));
				}
				if (m_EndDate != null)
				{
					spec += "&maxTime=";
					spec += encodeURLParameter(dateFormatter.format(m_EndDate));
				}

				
		        URL qryServiceUrl = new URL(getCodeBase(), spec);
		        System.out.println("qryServiceUrl: " + qryServiceUrl); 
		        
		        URLConnection conn = qryServiceUrl.openConnection();
		        
		        // Try reading in Object data.
		        ObjectInputStream in = new ObjectInputStream(conn.getInputStream());
		        ArrayList<TimeSeriesDataPoint> dataPoints = (ArrayList<TimeSeriesDataPoint>)(in.readObject());
		        System.out.println("Number of time series data points: " + dataPoints.size());
		        
		        // Load the data into the chart.
		        TimeSeriesData timeSeriesData = new TimeSeriesData(config, dataPoints);
		        m_JFreeChart.removeTimeSeries(config);
		        m_JFreeChart.addTimeSeries(timeSeriesData);
		        
		        if ( (dataPoints.size() <= 1) && (m_StartDate != null) && (m_EndDate != null) )
		        {
		        	// If there are no data points, manually set the range.
		        	m_JFreeChart.setDateRange(new DateRange(m_StartDate, m_EndDate), false);
		        }
		        
		        DateRange chartRange = m_JFreeChart.getDateRange();
		        if (chartRange != null)
		        {
		        	m_StartDate = chartRange.getLowerDate();
		        	m_EndDate = chartRange.getUpperDate();
		        }
			} 
        }
        catch (MalformedURLException e)
        {
	        System.err.println("Error creating services URL: " + e);
        }
        catch (IOException e)
        {
        	System.err.println("Error reading usage data: " + e);
        }
        catch (ClassNotFoundException e)
        {
        	System.err.println("Error reading usage data: " + e);
        }
	}
	
	
	/**
	 * Sets the date range for the chart. This defines the start and end times
	 * of the time series data to load.
	 * @param startTime start time for the chart.
	 * @param endTime end time for the chart.
	 */
	public void setDateRange(String startTime, String endTime)
	{
		System.out.println("setDateRange(" + startTime + "," + endTime + ")");
		
		SimpleDateFormat dateFormatter = new SimpleDateFormat(SECOND_PATTERN);
		
		try
		{
			Date start = dateFormatter.parse(startTime);
			Date end = dateFormatter.parse(endTime);
			
			m_StartDate = start;
			m_EndDate = end;
		}
		catch (ParseException e)
		{
			System.err.println("Error parsing supplied start and end times: " + 
					startTime + ", " + endTime);
		}
	}
	
	
    /**
     * Sets the chart title to the specified text.
     * @param text the text for the title.
     */
	public void setChartTitle(String text)
	{
		m_JFreeChart.setTitle(text);
	}
	
	
	/**
     * Sets the chart subtitle to the specified text.
     * @param text the text for the subtitle).
     */
	public void setChartSubtitle(String text)
	{
		System.out.println("setChartSubtitle(" + text + ")");
		
		m_JFreeChart.setSubtitle(text);
	}
	
	
	/**
	 * Example of how to call a Javascript method in the containing page.
	 */
	public void callJavascriptMethod()
	{
		int currentCount = 12345;
        
        JSObject window = JSObject.getWindow(TimeSeriesChartApplet.this);
        window.call("displayCounterValue", new Object[]{currentCount});
	}
	
	
	/**
	 * Shows the menu items for launching other view types.
	 */
	protected void showViewToolMenuItems()
	{
		JPopupMenu popupMenu = m_JFreeChart.getPopupMenu();
		
		if (m_ViewMenuItems.size() > 0)
		{
			popupMenu.add(m_ViewToolsSeparator);
			
			Iterator<JMenuItem> toolsIter = m_ViewMenuItems.values().iterator();
			while (toolsIter.hasNext())
			{
				popupMenu.add(toolsIter.next());
			}
		}
	}
	
	
	/**
	 * Hides the menu items for launching other view types.
	 */
	protected void hideViewToolMenuItems()
	{
		JPopupMenu popupMenu = m_JFreeChart.getPopupMenu();
		
		if (m_ViewMenuItems.size() > 0)
		{
			popupMenu.remove(m_ViewToolsSeparator);
			
			Iterator<JMenuItem> toolsIter = m_ViewMenuItems.values().iterator();
			while (toolsIter.hasNext())
			{
				popupMenu.remove(toolsIter.next());
			}
		}
	}
	
	
	/**
	 * Translates a string into application/x-www-form-urlencoded format using 
	 * the character encoding used by the application (UTF-8). 
	 * This method uses the encoding scheme to obtain the bytes for unsafe characters. 
	 * @param param String to be translated
	 * @return the translated String
	 * @throws UnsupportedEncodingException if the application character encoding
	 * is not supported.
	 */
	protected static String encodeURLParameter(String param) 
		throws UnsupportedEncodingException
	{
		return URLEncoder.encode(param, CHARACTER_ENCODING);
	}
	
	
	/**
	 * Runs a tool to open a view to show the given data source type. 
	 * @param dataTypeToOpen the data type of the view to open e.g. system_udp,
	 * 		or p2pslogs.
	 */
	protected void runViewToolAction(String dataTypeToOpen)
	{
		if (m_JFreeChart.isDataItemSelected() == true)
		{
			String source = m_JFreeChart.getSelectedSource();
			Date time = m_JFreeChart.getSelectedTime();
			
			SimpleDateFormat dateFormatter = new SimpleDateFormat(SECOND_PATTERN);
			String timeStr = dateFormatter.format(time);
			
	        JSObject window = JSObject.getWindow(TimeSeriesChartApplet.this);
	        window.call("appletViewToolRun", new Object[]{dataTypeToOpen, source, timeStr});
		}
	}
	
	
	class RunViewToolAction extends AbstractAction
	{
		
		public RunViewToolAction(String text, String dataTypeToOpen)
		{
			super(text);
			putValue("dataSourceName", dataTypeToOpen);
		}
		
		@Override
        public void actionPerformed(ActionEvent e)
        {
	        System.out.println("Go and show view " + getValue("dataSourceName"));
	        Object dataTypeToOpen = getValue("dataSourceName");
	        
	        runViewToolAction(dataTypeToOpen.toString());
        }
		
	}
	
}

