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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.time.DateRange;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleInsets;

import demo.app.data.Evidence;
import demo.app.data.Severity;
import demo.app.data.TimeSeriesConfig;
import demo.app.data.TimeSeriesData;
import demo.app.data.TimeSeriesDataPoint;


/**
 * Swing panel holding a JFreeChart (see {@link http://www.jfree.org/jfreechart/}).
 * 
 * @author Pete Harverson
 */
public class TimeSeriesJFreeChart extends JPanel
{
	private JFreeChart					m_Chart;
	private ChartPanel					m_ChartPanel;
	private XYPlot						m_Plot;
	
	private TextTitle					m_Title;
	private TextTitle					m_Subtitle;
	
	private TimeSeriesCollection 		m_TimeSeriesCollection;
	private TimeSeriesCollection		m_NotificationsCollection;
	
	private XYItemEntity				m_SelectedEntity;
	
	
	/**
	 * Creates a new lightweight container holding a JFreeChart.
	 */
	public TimeSeriesJFreeChart()
	{
		super(new BorderLayout());
		
		setBackground(Color.WHITE);
		
		m_Chart = createChart();
		m_Plot = (XYPlot)(m_Chart.getPlot());
		
		m_ChartPanel = new ChartPanel(m_Chart);
		m_ChartPanel.setPreferredSize(new java.awt.Dimension(400, 300));
		m_ChartPanel.setFillZoomRectangle(true);
		
		add(m_ChartPanel, BorderLayout.CENTER);
		
		// Add a listener for 'selected' points.
		m_ChartPanel.addChartMouseListener(new ChartMouseListener(){

			@Override
            public void chartMouseClicked(ChartMouseEvent event)
            {
				ChartEntity entity = event.getEntity();
				if (entity != null && entity instanceof XYItemEntity)
				{
					m_SelectedEntity = (XYItemEntity) entity;
				}
				else
				{
					m_SelectedEntity = null;
				}
            }

			@Override
            public void chartMouseMoved(ChartMouseEvent event)
            {
	            // No action.
            }
			
		});
	}
	

	/**
	 * Creates the JFreeChart component itself.
	 * @return the JFreeChart.
	 */
    private JFreeChart createChart() 
    {
    	m_TimeSeriesCollection = new TimeSeriesCollection();
    	m_NotificationsCollection = new TimeSeriesCollection();

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "",  				// title
            "Time",             // x-axis label
            "Value",   			// y-axis label
            m_TimeSeriesCollection,            // data
            false,				// create legend?
            true,				// generate tooltips?
            false				// generate URLs?
        );
        
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setDataset(1, m_NotificationsCollection);

        chart.setBackgroundPaint(Color.white);
        
        m_Title = chart.getTitle();
        m_Title.setFont(new Font("Verdana", Font.BOLD, 12));
        m_Title.setPaint(new Color(0x15428b));
        m_Title.setHorizontalAlignment(HorizontalAlignment.LEFT);
        m_Title.setPadding(5, 5, 0, 0);
        
        m_Subtitle = new TextTitle("time occurred: Mon Jul 06 15:15");
        m_Subtitle.setFont(new Font("Verdana", Font.BOLD, 9));
        m_Subtitle.setPaint(new Color(0x15428b));
        m_Subtitle.setHorizontalAlignment(HorizontalAlignment.LEFT);
        m_Subtitle.setPadding(0, 5, 5, 0);
        chart.addSubtitle(m_Subtitle);
        
        plot.setBackgroundPaint(Color.white);
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);
      	plot.setAxisOffset(new RectangleInsets(0.0, 0.0, 0.0, 0.0));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        plot.setOutlineVisible(false);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        XYItemRenderer r = plot.getRenderer();
        if (r instanceof XYLineAndShapeRenderer) 
        {
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
            renderer.setBaseShapesVisible(false);
            renderer.setBaseShapesFilled(false);
            renderer.setDrawSeriesLineAsPath(true);
        }

        // Test out setting the colour of a series.
        r.setSeriesPaint(0, new Color(0x0000FF));
        r.setSeriesPaint(1, new Color(0x63B8FF));
        r.setSeriesPaint(2, new Color(0xFFFF00));
        r.setSeriesPaint(3, new Color(0xFF00FF));
        r.setSeriesPaint(4, new Color(0xFF0000));
        r.setSeriesPaint(5, new Color(0x00FF00));
        r.setSeriesPaint(6, new Color(0xB429FF));
        r.setSeriesPaint(7, new Color(0xB4FF29));
        r.setSeriesPaint(8, new Color(0xCCCC00));
        r.setSeriesPaint(9, new Color(0x67BACA));
        
  
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("MMM d HH:mm"));

        return chart;

    }
    
    
    /**
     * Returns the popup menu for the chart.
     * @return
     */
    public JPopupMenu getPopupMenu()
    {
    	return m_ChartPanel.getPopupMenu();
    }
    
    
    /**
     * Registers an object for notification of changes to the date axis,
     * such as when zooming or panning.
     * @param axisChangeListener the object that is being registered.
     */
    public void addDateAxisChangeListener(AxisChangeListener axisChangeListener)
    {
    	m_Plot.getDomainAxis().addChangeListener(axisChangeListener);
    }
    
    
    /**
     * Deregisters an object for notification of changes to the date axis. 
     * @param axisChangeListener the object to deregister.
     */
    public void removeDateAxisChangeListener(AxisChangeListener axisChangeListener)
    {
    	m_Plot.getDomainAxis().removeChangeListener(axisChangeListener);
    }


	/**
     * Adds a time series for display in the chart.
     * @param timeSeriesData the time series data to add to the chart.
     */
    public void addTimeSeries(TimeSeriesData timeSeriesData)
    {
    	TimeSeriesConfig config = timeSeriesData.getConfig();
    	List<TimeSeriesDataPoint> dataPoints = timeSeriesData.getDataPoints();
    	
    	// Create the new TimeSeries, and add the data items.
    	JFreeChartTimeSeries timeSeries = 
    		new JFreeChartTimeSeries(config);
    	for (TimeSeriesDataPoint dataPoint : dataPoints)
    	{
    		timeSeries.add(new Millisecond(dataPoint.getTime()), dataPoint.getValue());
    	}
    	
    	m_TimeSeriesCollection.addSeries(timeSeries);
    	
    	// If there is only one point, fill in the series shape.
    	int seriesIndex = m_TimeSeriesCollection.indexOf(timeSeries);
		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer)(m_Plot.getRenderer());
    	if (timeSeries.getItemCount() > 1)
    	{
    		renderer.setSeriesShapesVisible(seriesIndex, false);
    		renderer.setSeriesShapesFilled(seriesIndex, false);
    	}
    	else
    	{
    		renderer.setSeriesShapesVisible(seriesIndex, true);
    		renderer.setSeriesShapesFilled(seriesIndex, true);
    	}
    }
    
    
    /**
     * Removes the time series identified by the specified key from the chart.
     * @param seriesConfig
     */
    public void removeTimeSeries(TimeSeriesConfig seriesConfig)
    {
    	TimeSeries seriesToRemove = 
    		m_TimeSeriesCollection.getSeries(getTimeSeriesKey(seriesConfig));
    	if (seriesToRemove != null)
    	{
    		m_TimeSeriesCollection.removeSeries(seriesToRemove);
    	}
    }
    
    
    /**
     * Removes all time series from the chart.
     */
    public void removeAllTimeSeries()
    {
    	m_TimeSeriesCollection.removeAllSeries();
    	m_SelectedEntity = null;
    }
    
    
    /**
     * Adds the specified notification for display in the chart.
     * @param notification the notification to add to the chart.
     */
    public void addNotification(Evidence notification)
    {
        Severity severity = notification.getSeverity();
    	
        
        // TO DO: may need to create a method which takes:
        // public void addNotification(EventRecord notification, Shape shape)
    	// For now create a new time series for each severity.
        TimeSeries timeSeries = m_NotificationsCollection.getSeries(severity);
        boolean isNewSeverity = false;
        if (timeSeries == null)
        {
        	timeSeries = new TimeSeries(severity);
        	m_NotificationsCollection.addSeries(timeSeries);
        	isNewSeverity = true;
        }
        
        timeSeries.add(new NotificationChartDataItem(notification));
        
    	
    	// Create the renderer for the notifications if necessary.
        checkNotificationRenderer();
		
		if (isNewSeverity == true)
		{
			// Match the series paint colour to its severity.
			int numSeries = m_NotificationsCollection.getSeriesCount();
			m_Plot.getRenderer(1).setSeriesPaint(numSeries-1, 
					NotificationSeverityColours.getColor(severity));
		}
    	
    }
    
    
    /**
     * Removes all notifications from the chart.
     */
    public void removeAllNotifications()
    {
    	m_NotificationsCollection.removeAllSeries();
    	m_SelectedEntity = null;
    }
    
    
    /**
     * Removes all time series and notifications from the chart.
     */
    public void removeAll()
    {
    	removeAllTimeSeries();
    	removeAllNotifications();
    }
    
    
    /**
     * Sets the chart title to the specified text.
     * @param text the text for the title (<code>null</code> not permitted).
     */
    public void setTitle(String text)
    {
    	m_Title.setText(text);
    }
    
    
    /**
     * Sets the chart subtitle to the specified text.
     * @param text the text for the subtitle (<code>null</code> not permitted).
     */
    public void setSubtitle(String text)
    {
    	m_Subtitle.setText(text);
    }
    
    
    /**
     * Returns the date range of the chart plot.
     * @return the range of the date axis.
     */
    public DateRange getDateRange()
    {
    	return (DateRange)(m_Plot.getDomainAxis().getRange());
    }
    
    
    /**
     * Sets the date range of the chart plot.
     * @param range the range of the date axis.
     * @param notify a flag that controls whether or not an <code>AxisChangeEvent</code>
     * 	is sent to registered listeners.
     */
    public void setDateRange(DateRange range, boolean notify)
    {
    	m_Plot.getDomainAxis().setRange(range, true, false);
    }
    
    
    /**
     * Returns whether a notification or point in a time series is 'selected'.
     * @return <code>true</code> if an item is selected, <code>false</code> otherwise.
     */
    public boolean isDataItemSelected()
    {
    	return (m_SelectedEntity != null);
    }
    
    
    /**
     * Gets the source of the selected item.
     * @return the source (server), or <code>null</code> if no item is selected
     * 		or if an item from an 'all sources' time series is selected.
     */
    public String getSelectedSource()
    {
    	String source = null;
    	
    	 if(m_SelectedEntity != null)
    	 {
			TimeSeriesCollection dataset = (TimeSeriesCollection)(m_SelectedEntity.getDataset());
			int seriesIndex = m_SelectedEntity.getSeriesIndex();
			
			if (dataset == m_TimeSeriesCollection)
			{
				JFreeChartTimeSeries series = (JFreeChartTimeSeries)(dataset.getSeries(seriesIndex));
				source = series.getTimeSeriesConfig().getSource();
			}
			else if (dataset == m_NotificationsCollection)
			{
				TimeSeries series = dataset.getSeries(seriesIndex);
				int itemIndex = m_SelectedEntity.getItem();
				NotificationChartDataItem dataItem = 
					(NotificationChartDataItem)(series.getDataItem(itemIndex));
				source = dataItem.getNotification().getSource();
			}
    	 }
		
		return source;
    }
    
    
    /**
     * Gets the time of the selected item.
     * @return the time, or <code>null</code> if no item is selected.
     */
    public Date getSelectedTime()
    {
    	Date time = null;
    	
    	 if(m_SelectedEntity != null)
    	 {
			TimeSeriesCollection dataset = (TimeSeriesCollection)(m_SelectedEntity.getDataset());

			int seriesIndex = m_SelectedEntity.getSeriesIndex();
			TimeSeries series = dataset.getSeries(seriesIndex);
			
			if (dataset == m_TimeSeriesCollection)
			{
				int itemIndex = m_SelectedEntity.getItem();
				RegularTimePeriod timePeriod = series.getTimePeriod(itemIndex);
				time = new Date(timePeriod.getFirstMillisecond());
			}
			else if (dataset == m_NotificationsCollection)
			{
				int itemIndex = m_SelectedEntity.getItem();
				NotificationChartDataItem dataItem = 
					(NotificationChartDataItem)(series.getDataItem(itemIndex));
				time = dataItem.getNotification().getTime();
			}
    	 }
		
		return time;
    }
    
    
    /**
     * Creates a key for the specified TimeSeriesConfig to distinguish it from
     * other time series in the chart.
     * @param dataSeries config for the time series for which to generate a key.
     * @return a key for the time series.
     */
    protected String getTimeSeriesKey(TimeSeriesConfig config)
    {
    	String dataType = config.getDataType();
    	String metric = new String(config.getMetric());
    	String source = config.getSource();
    	String attributeName = config.getAttributeName();
    	String attributeValue = config.getAttributeValue();
    	
    	StringBuilder key = new StringBuilder(dataType);
    	key.append(',');
    	key.append(metric);
    	key.append(',');
    	
    	if (source != null)
    	{
    		key.append(source);
    	}
    	else
    	{
    		key.append("all sources");
    	}
    	
    	if ( (attributeName != null) && (attributeValue != null) )
    	{
    		key.append(',');
    		key.append(attributeName);
    		key.append('=');
    		key.append(attributeValue);
    	}
    	
    	return key.toString();
    }
    
    
    /**
     * Checks that a renderer has been created for displaying notifications,
     * and if not, creates it.
     */
    private void checkNotificationRenderer()
    {
    	// Create the renderer for the notifications if necessary.
		if (m_Plot.getRenderer(1) == null)
		{
			XYShapeRenderer notificationRenderer = new XYShapeRenderer();
			notificationRenderer.setBaseShape(new Rectangle(0, -10, 10, 10));
			notificationRenderer.setBaseToolTipGenerator(new NotificationToolTipGenerator());
			m_Plot.setRenderer(1, notificationRenderer);
		}
    }
    
    
    /**
     * Tooltip generator for notification data points.
     */
    class NotificationToolTipGenerator implements XYToolTipGenerator
    {

        public String generateToolTip(XYDataset dataset, int series, int item)
        {
	        String tooltip = "";
	        
	        NotificationChartDataItem dataItem = 
	        	(NotificationChartDataItem)(m_NotificationsCollection.getSeries(series).getDataItem(item));
	        
	        Evidence notification = dataItem.getNotification();
	        
	        String description = notification.getDescription();
	        Date time = notification.getTime();
	        if (description != null)
	        {
	        	tooltip += description;
	        	tooltip += ' ';
	        }
	        
	        if (time != null)
	        {
	        	tooltip += time;
	        }
	        
	        return tooltip;
        }
    	
    }
    
    
}
