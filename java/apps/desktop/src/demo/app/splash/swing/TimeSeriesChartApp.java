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
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.log4j.Logger;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.data.time.DateRange;

import demo.app.dao.TimeSeriesMySQLDAO;
import demo.app.data.Severity;
import demo.app.data.TimeSeriesConfig;
import demo.app.data.TimeSeriesData;
import demo.app.data.TimeSeriesDataPoint;
import demo.app.data.Evidence;


public class TimeSeriesChartApp extends JFrame
{
	static Logger logger = Logger.getLogger(TimeSeriesChartApp.class);
	
	private TimeSeriesJFreeChart	m_JFreeChart;
	
	private TimeSeriesMySQLDAO		m_TimeSeriesDAO;
	
	private List<TimeSeriesConfig>		m_TimeSeriesConfigs;
	
	
	public TimeSeriesChartApp()
	{
		super("Time Series Chart Test");
		
		createDataSource();
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		setSize(700, 550);
		
		m_JFreeChart = new TimeSeriesJFreeChart();
		
		m_JFreeChart.addDateAxisChangeListener(new AxisChangeListener(){

			@Override
            public void axisChanged(AxisChangeEvent event)
            {
	            // Reload the data for the new date range.
				ValueAxis axis = (ValueAxis)(event.getAxis());
				System.out.println("Date range changed: " + axis.getRange().toString());
				
				DateRange dateRange = (DateRange)(axis.getRange());
				
				loadDataFromServer(dateRange.getLowerDate(), dateRange.getUpperDate());
            }
			
		});
		
		getContentPane().add(m_JFreeChart);
		
		// Test out adding a menu item to launch another view for the 
		// selected source/date.
		AbstractAction showEvidenceAction = new AbstractAction("Show evidence"){

			@Override
            public void actionPerformed(ActionEvent e)
            {
				if (m_JFreeChart.isDataItemSelected() == true)
				{
					System.out.println("Show evidence for " + m_JFreeChart.getSelectedSource() +
							" at time " + m_JFreeChart.getSelectedTime());
				}
            }
			
		};

		
		// Configure a context-sensitive Popup Menu.
		final JMenuItem showEvidenceMenuItem = new JMenuItem(showEvidenceAction);
		final JPopupMenu popupMenu = m_JFreeChart.getPopupMenu();
		final JSeparator viewToolsSeparator = new JSeparator();
		
		popupMenu.addPopupMenuListener(new PopupMenuListener(){

			@Override
            public void popupMenuCanceled(PopupMenuEvent e)
            {
	            
            }

			@Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
            {
				popupMenu.remove(showEvidenceMenuItem);
				popupMenu.remove(viewToolsSeparator);
            }

			@Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e)
            {
				if (m_JFreeChart.isDataItemSelected())
				{
					popupMenu.add(viewToolsSeparator);
					popupMenu.add(showEvidenceMenuItem);
				}
            }
			
		});
		
		m_TimeSeriesConfigs = new ArrayList<TimeSeriesConfig>();
	}
	
	
	protected void createDataSource()
	{
		Properties connProps = new Properties();
		connProps.put("driverClassName", "com.mysql.jdbc.Driver");
		connProps.put("url", "jdbc:mysql://localhost/cs_0210");
		connProps.put("username", "root");
		connProps.put("password", "root123");
		connProps.put("testOnBorrow", "true");
		connProps.put("validationQuery", "select 1");
		
		try
        {
	        DataSource dataSource = BasicDataSourceFactory.createDataSource(connProps);
	        m_TimeSeriesDAO = new TimeSeriesMySQLDAO();
	        m_TimeSeriesDAO.setDataSource(dataSource);
	        
	        logger.debug("Created data source: " + dataSource);
        }
        catch (Exception e)
        {
	        logger.error("Error creating data source", e);
        }
	}
	
	
	public void showTimeSeries()
	{
		// Add a time series.
		TimeSeriesData data = createTestTimeSeries();
		m_JFreeChart.addTimeSeries(data);
	}
	
	
	public void showNotifications()
	{
		// Add some notifications.
		List<Evidence> evidenceList = createTestNotifications();
		for (Evidence evidence : evidenceList)
		{
			m_JFreeChart.addNotification(evidence);
		}
	}
	
	
	public void addTestTimeSeries1()
	{
		// Load some test usage data.
		// For cs_0210 db:
		TimeSeriesConfig config1 = new TimeSeriesConfig("p2psmon_users", "total", "lnl00m-8201");
		TimeSeriesConfig config2 = new TimeSeriesConfig("p2psmon_users", "total", "lnl00m-8201", "appId", "THOR");
		TimeSeriesConfig config3 = new TimeSeriesConfig("p2psmon_users", "total", "lnl00m-8202");
		TimeSeriesConfig config4 = new TimeSeriesConfig("p2psmon_users", "total", "lnl00m-8202", "appId", "Smart Order Router");
		TimeSeriesConfig config5 = new TimeSeriesConfig("p2psmon_users", "total", "lnl00m-8205");
		
		m_TimeSeriesConfigs.add(config1);
		m_TimeSeriesConfigs.add(config2);	
		m_TimeSeriesConfigs.add(config3);		
		m_TimeSeriesConfigs.add(config4);	
		m_TimeSeriesConfigs.add(config5);	
	}
	
	
	public void addEmptyTimeSeries()
	{
		// Test out adding a time series which has no points.
		// For cs_0210 db:
		TimeSeriesConfig config1 = new TimeSeriesConfig("mdhmon", "active", null);
		
		m_TimeSeriesConfigs.add(config1);
	}
	
	
	public void addTestCausalitySeries()
	{
		// Load some test usage data.
		// For cs_0210 db:
		TimeSeriesConfig config1 = new TimeSeriesConfig("p2psmon_ipc", "inbound", "lnl00m-8221");
		TimeSeriesConfig config2 = new TimeSeriesConfig("system_udp", "packets_sent", "sol06m-8102");
		
		m_TimeSeriesConfigs.add(config1);
		m_TimeSeriesConfigs.add(config2);	
		
		GregorianCalendar calendar = new GregorianCalendar();
        Evidence evidence1 = new Evidence();
        calendar.set(2010, 1, 10, 15, 1);
        evidence1.setTime(calendar.getTime());
        evidence1.setSeverity(Severity.MAJOR);
        evidence1.setDescription("Transport node error");
        
        
        Evidence evidence2 = new Evidence();
        calendar.set(2010, 1, 10, 15, 0);
        evidence2.setTime(calendar.getTime());
        evidence2.setSeverity(Severity.MINOR);
        evidence2.setDescription("rrcp reboot");
        
        m_JFreeChart.addNotification(evidence1);
        m_JFreeChart.addNotification(evidence2);
        
        m_JFreeChart.setTitle("Diagnostics for rrcp reboot id=71718");
        m_JFreeChart.setSubtitle("time occurred: Wed Feb 10 15:00");
	}
	
	
	public void loadDataFromServer(Date minTime, Date maxTime)
	{

		try
        {
			
			for (TimeSeriesConfig config : m_TimeSeriesConfigs)
			{
				/*
				String serviceSpec = "http://localhost:8080/splash/splash/services/timeSeriesQueryService?action=getDataPoints";
				serviceSpec += "&dataType=";
				serviceSpec += config.getDataType();
				serviceSpec += "&metric=";
				serviceSpec += config.getMetric();
				if (config.getSource() != null)
				{
					serviceSpec += "&source=";
					serviceSpec += config.getSource();
				}
				if (config.getAttributeName() != null)
				{
					serviceSpec += "&attributeName=";
					serviceSpec += config.getAttributeName();
					
					serviceSpec += "&attributeValue=";
					serviceSpec += config.getAttributeValue();
				}
				
		        URL qryServiceUrl = new URL(serviceSpec);
		        System.out.println("qryServiceUrl: " + qryServiceUrl); 
		        
		        URLConnection conn = qryServiceUrl.openConnection();
		        
		        // Try reading in Object data.
		        ObjectInputStream in = new ObjectInputStream(conn.getInputStream());
		        ArrayList<TimeSeriesDataPoint> dataPoints = (ArrayList<TimeSeriesDataPoint>)(in.readObject());
		        System.out.println("Number of usage data items loaded: " + dataPoints.size());
		        */
		        
				List<TimeSeriesDataPoint> dataPoints = m_TimeSeriesDAO.getDataPointsForTimeSpan(config.getDataType(), 
						config.getMetric(), 
						minTime, 
						maxTime, 
						config.getSource(), 
						config.getAttributeName(), config.getAttributeValue(), 
						false);
				logger.debug("Number of time series data points: " + dataPoints.size());
				
		        // Load the data into the chart.
		        TimeSeriesData timeSeriesData = new TimeSeriesData(config, dataPoints);
		        m_JFreeChart.removeTimeSeries(config);
		        m_JFreeChart.addTimeSeries(timeSeriesData);
		        
		        // If there are no points, manually set the range.
		        if (dataPoints.size() <= 1)
		        {
		        	m_JFreeChart.setDateRange(new DateRange(minTime, maxTime), false);
		        }
			}
	        
        }
		catch (Exception e)
		{
			logger.error("Error loading data from database", e);
		}
		/*
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
        */
	}
	
	
	/**
	 * Creates a time series for testing.
	 * @return time series data.
	 */
    private static TimeSeriesData createTestTimeSeries()
    {
    	TimeSeriesConfig config = new TimeSeriesConfig("p2psmon_users", "total", "som30m-8201");
    	
    	List<TimeSeriesDataPoint> dataPoints = new ArrayList<TimeSeriesDataPoint>();
    	
    	GregorianCalendar calendar = new GregorianCalendar();
    	calendar.set(2008, 1, 1);
    	dataPoints.add(new TimeSeriesDataPoint(calendar.getTime(), 182));
    	calendar.set(2008, 2, 1);
    	dataPoints.add(new TimeSeriesDataPoint(calendar.getTime(), 172));
    	calendar.set(2008, 3, 1);
    	dataPoints.add(new TimeSeriesDataPoint(calendar.getTime(), 163));
    	calendar.set(2008, 4, 1);
    	dataPoints.add(new TimeSeriesDataPoint(calendar.getTime(), 193));
    	calendar.set(2008, 5, 1);
    	dataPoints.add(new TimeSeriesDataPoint(calendar.getTime(), 201));
    	calendar.set(2008, 6, 1);
    	dataPoints.add(new TimeSeriesDataPoint(calendar.getTime(), 233));
    	calendar.set(2008, 7, 1);
    	dataPoints.add(new TimeSeriesDataPoint(calendar.getTime(), 199));
    	calendar.set(2008, 8, 1);
    	dataPoints.add(new TimeSeriesDataPoint(calendar.getTime(), 219));
    	calendar.set(2008, 9, 1);
    	dataPoints.add(new TimeSeriesDataPoint(calendar.getTime(), 190));
    	calendar.set(2008, 10, 1);
    	dataPoints.add(new TimeSeriesDataPoint(calendar.getTime(), 199));
    	calendar.set(2008, 11, 1);
    	dataPoints.add(new TimeSeriesDataPoint(calendar.getTime(), 203));
    	calendar.set(2009, 0, 1);
    	dataPoints.add(new TimeSeriesDataPoint(calendar.getTime(), 206));
    	calendar.set(2009, 1, 1);
    	dataPoints.add(new TimeSeriesDataPoint(calendar.getTime(), 206));
    	calendar.set(2009, 2, 1);
    	dataPoints.add(new TimeSeriesDataPoint(calendar.getTime(), 182));
    	calendar.set(2009, 3, 1);
    	dataPoints.add(new TimeSeriesDataPoint(calendar.getTime(), 177));
    	calendar.set(2009, 4, 1);
    	dataPoints.add(new TimeSeriesDataPoint(calendar.getTime(), 178));
    	calendar.set(2009, 5, 1);
    	dataPoints.add(new TimeSeriesDataPoint(calendar.getTime(), 243));
    	calendar.set(2009, 6, 1);
    	dataPoints.add(new TimeSeriesDataPoint(calendar.getTime(), 211));

    	return new TimeSeriesData(config, dataPoints);
    }
    
    
    /**
     * Creates a list of evidence for testing.
     *
     * @return list of notification data.
     */
    private static List<Evidence> createTestNotifications() 
    {
    	List<Evidence> notifications = new ArrayList<Evidence>();
    	
    	GregorianCalendar calendar = new GregorianCalendar();
    	

        Evidence evidence1 = new Evidence();
        calendar.set(2010, 1, 10, 12, 0, 0);
        evidence1.setTime(calendar.getTime());
        evidence1.setSeverity(Severity.CRITICAL);
        evidence1.setDescription("rrcp congestion");
        evidence1.setSource("lon-web1");
        
        Evidence evidence2 = new Evidence();
        calendar.set(2010, 1, 10, 15, 0, 0);
        evidence2.setTime(calendar.getTime());
        evidence2.setSeverity(Severity.MAJOR);
        evidence2.setDescription("Output threshold breached");
        evidence2.setSource("lon-web2");
        
        Evidence evidence3 = new Evidence();
        calendar.set(2010, 1, 11, 0, 0, 0);
        evidence3.setTime(calendar.getTime());
        evidence3.setSeverity(Severity.MAJOR);
        evidence3.setDescription("Output threshold OK");
        evidence3.setSource("lon-web3");
        
        notifications.add(evidence1);
        notifications.add(evidence2);
        notifications.add(evidence3);

        return notifications;

    }
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		TimeSeriesChartApp chartApp = new TimeSeriesChartApp();
		
		chartApp.setVisible(true);
		
//		chartApp.showTimeSeries();
		chartApp.showNotifications();
		
		GregorianCalendar calendar = new GregorianCalendar();
		
		chartApp.addTestTimeSeries1();	
        calendar.set(2010, 1, 11, 8, 0);
        Date minTime = calendar.getTime();
        calendar.set(2010, 1, 11, 12, 0);
        Date maxTime = calendar.getTime();
		
//		chartApp.addTestCausalitySeries();
//		
//		calendar.set(2010, 1, 10, 14, 40);
//		Date minTime = calendar.getTime();
//		calendar.set(2010, 1, 10, 15, 20);
//		Date maxTime = calendar.getTime();
        
        
//        chartApp.addEmptyTimeSeries();
//        calendar.set(2010, 1, 10, 18, 40);
//        Date minTime = calendar.getTime();
//        calendar.set(2010, 1, 10, 23, 50);
//        Date maxTime = calendar.getTime();
		
        
		chartApp.loadDataFromServer(minTime, maxTime);
	}

}
