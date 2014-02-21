package demo.app.client.chart;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;
import javax.swing.*;

import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.log4j.Logger;
import org.jfree.chart.*;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.*;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleInsets;

import com.extjs.gxt.ui.client.util.DateWrapper;

import demo.app.dao.ServiceUsageViewMySQLDAO;
import demo.app.data.TimeSeriesConfig;
import demo.app.data.UsageRecord;

public class JFreeChartTestApp
{
	static Logger logger = Logger.getLogger(JFreeChartTestApp.class);
	private static final Date TEST_DATE = (new DateWrapper(2009, 6, 6)).asDate();
	
	private ServiceUsageViewMySQLDAO	m_UsageDAO;
	private List<TimeSeriesConfig>		m_DataSeries;
	private TimeSeriesCollection 		m_Dataset;
	private TimeSeries 					m_TurquoiseSeries;
	
	public JFreeChartTestApp()
	{
		initialiseDataSource();
		
		m_DataSeries = new ArrayList<TimeSeriesConfig>();
	    addDataSeries("active", null, null);
	    addDataSeries("active", "sol30m-8201.1.p2ps", null);
	    addDataSeries("active", "sol30m-8202.1.p2ps", null);
	    addDataSeries("active", "sol30m-8203.1.p2ps", null);
		addDataSeries("serverload", null, null);
	    addDataSeries("serverload", "sol30m-8201.1.p2ps", null);
	    addDataSeries("serverload", "sol30m-8202.1.p2ps", null);
	    addDataSeries("serverload", "sol30m-8203.1.p2ps", null);
	    addDataSeries("serverload", "sol30m-8203.1.p2ps", "IDN_SELECTFEED");
	   // addDataSeries("serverload", "sol30m-8203.1.p2ps", "TEST_PRISM_TURQUOISE"); // 70 points
	}
	
	
	public void addDataSeries(String metric, String source, String service)
	{
		TimeSeriesConfig dataSeries = new TimeSeriesConfig(metric, source, service); 
		m_DataSeries.add(dataSeries);
	}
	
	
	public TimeSeriesCollection loadData()
	{
		List<UsageRecord> usageRecords;
		TimeSeries timeSeries;
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		
		for (TimeSeriesConfig dataSeries : m_DataSeries)
		{
			usageRecords = m_UsageDAO.getDailyUsageData(TEST_DATE, dataSeries.getMetric(), 
					dataSeries.getSource(), dataSeries.getAttributeValue());
			
			timeSeries = buildTimeSeries(getTimeSeriesLabel(dataSeries), usageRecords);
			dataset.addSeries(timeSeries);
		}

        return dataset;
	}
	
	
	public void setTurquoiseSeriesVisible(boolean visible)
	{
		// Create a separate data series for TEST_PRISM_TURQUOISE to test out
	    // dynamically adding/removing a series.
		if (m_TurquoiseSeries == null)
		{
			TimeSeriesConfig turquoiseConfig = 
				new TimeSeriesConfig("serverload", "sol30m-8203.1.p2ps", "TEST_PRISM_TURQUOISE");
			List<UsageRecord> usageRecords = m_UsageDAO.getDailyUsageData(
					TEST_DATE, turquoiseConfig.getMetric(), 
					turquoiseConfig.getSource(), turquoiseConfig.getAttributeValue());
			m_TurquoiseSeries = buildTimeSeries(getTimeSeriesLabel(turquoiseConfig), usageRecords);
		}
		
		Date startTime = new Date();
		
		if (visible == true)
		{
			m_Dataset.addSeries(m_TurquoiseSeries);
		}
		else
		{
			m_Dataset.removeSeries(m_TurquoiseSeries);
		}
		
		Date endTime = new Date();
		logger.debug("Time to show/hide series: " + (endTime.getTime() - startTime.getTime()) + "ms");
	}
	
	
	private TimeSeries buildTimeSeries(String name, List<UsageRecord> usageRecords)
	{
		TimeSeries timeSeries = new TimeSeries(name);
		
		for (UsageRecord rec: usageRecords)
		{
			timeSeries.add(new Minute(rec.getTime()), rec.getValue());
			
		}
        
        return timeSeries;
	}
	
	
	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event-dispatching thread.
	 */
	private void createAndShowGUI()
	{
		// Create and set up the window.
		JFrame frame = new JFrame("JFreeChart Test App");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		ChartPanel chartPanel = (ChartPanel) createDemoPanel();
        chartPanel.setPreferredSize(new java.awt.Dimension(750, 520));
        chartPanel.getPopupMenu().add(new JMenuItem("Show Evidence"));
		frame.getContentPane().add(chartPanel, BorderLayout.CENTER);
	
		JPanel checkboxPanel = new JPanel();
		final JCheckBox showExtraCb = new JCheckBox("Show TURQUOISE");
		showExtraCb.addActionListener(new ActionListener()
		{
            public void actionPerformed(ActionEvent actionevent)
            {
	            setTurquoiseSeriesVisible(showExtraCb.isSelected());
            }
			
		});
		checkboxPanel.add(showExtraCb);
		
		frame.getContentPane().add(checkboxPanel, BorderLayout.SOUTH);

		// Display the window.
		frame.pack();
		frame.setVisible(true);
	}
	
	
	/**
     * Creates a chart.
     *
     * @param dataset  a dataset.
     *
     * @return A chart.
     */
    private JFreeChart createChart(XYDataset dataset) {

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "Service Usage ",  	// title
            "Time",             // x-axis label
            "Usage",   			// y-axis label
            dataset,            // data
            false,        		// create legend?
            true,         		// generate tooltips?
            false      			// generate URLs?
        );

        chart.setBackgroundPaint(Color.white);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        
        // Add an IntervalMarker to demonstrate adding a 'timeline' band.
        IntervalMarker domainMarker = new IntervalMarker(
        		TEST_DATE.getTime() + 1000*60*60*11, TEST_DATE.getTime() + 1000*60*60*12);
        domainMarker.setLabelPaint(new Color(0x15428b));
        domainMarker.setLabel("Peak in usage rate");
        domainMarker.setLabelAnchor(RectangleAnchor.BOTTOM);
        domainMarker.setPaint(new Color(0xff8000));
        domainMarker.setAlpha(0.2f);
        plot.addDomainMarker(domainMarker);

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
    
    
    private static String getTimeSeriesLabel(TimeSeriesConfig dataSeries)
    {
    	String label = new String(dataSeries.getMetric());
    	String source = dataSeries.getSource();
    	String user = dataSeries.getAttributeValue();
    	
    	label += " - ";
    	if (source != null)
    	{
    		label+=source;
    	}
    	else
    	{
    		label+="All sources";
    	}
    	label += ", ";
    	if (user != null)
    	{
    		label+=user;
    	}
    	else
    	{
    		label+="All services";
    	}
    	
    	return label;
    }
    
    
    /**
     * Creates a dataset, consisting of two series of monthly data.
     *
     * @return The dataset.
     */
    private static XYDataset createStaticDataset() {

        TimeSeries s1 = new TimeSeries("L&G European Index Trust");
        s1.add(new Month(2, 2001), 181.8);
        s1.add(new Month(3, 2001), 167.3);
        s1.add(new Month(4, 2001), 153.8);
        s1.add(new Month(5, 2001), 167.6);
        s1.add(new Month(6, 2001), 158.8);
        s1.add(new Month(7, 2001), 148.3);
        s1.add(new Month(8, 2001), 153.9);
        s1.add(new Month(9, 2001), 142.7);
        s1.add(new Month(10, 2001), 123.2);
        s1.add(new Month(11, 2001), 131.8);
        s1.add(new Month(12, 2001), 139.6);
        s1.add(new Month(1, 2002), 142.9);
        s1.add(new Month(2, 2002), 138.7);
        s1.add(new Month(3, 2002), 137.3);
        s1.add(new Month(4, 2002), 143.9);
        s1.add(new Month(5, 2002), 139.8);
        s1.add(new Month(6, 2002), 137.0);
        s1.add(new Month(7, 2002), 132.8);

        TimeSeries s2 = new TimeSeries("L&G UK Index Trust");
        s2.add(new Month(2, 2001), 129.6);
        s2.add(new Month(3, 2001), 123.2);
        s2.add(new Month(4, 2001), 117.2);
        s2.add(new Month(5, 2001), 124.1);
        s2.add(new Month(6, 2001), 122.6);
        s2.add(new Month(7, 2001), 119.2);
        s2.add(new Month(8, 2001), 116.5);
        s2.add(new Month(9, 2001), 112.7);
        s2.add(new Month(10, 2001), 101.5);
        s2.add(new Month(11, 2001), 106.1);
        s2.add(new Month(12, 2001), 110.3);
        s2.add(new Month(1, 2002), 111.7);
        s2.add(new Month(2, 2002), 111.0);
        s2.add(new Month(3, 2002), 109.6);
        s2.add(new Month(4, 2002), 113.2);
        s2.add(new Month(5, 2002), 111.6);
        s2.add(new Month(6, 2002), 108.8);
        s2.add(new Month(7, 2002), 101.6);


        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(s1);
        dataset.addSeries(s2);

        return dataset;

    }
    

    /**
     * Creates a panel for the demo (used by SuperDemo.java).
     *
     * @return A panel.
     */
    public JPanel createDemoPanel() {
        //JFreeChart chart = createChart(createDataset());
    	m_Dataset = loadData();
    	JFreeChart chart = createChart(m_Dataset);
        ChartPanel panel = new ChartPanel(chart);
        panel.setFillZoomRectangle(true);
        panel.setMouseWheelEnabled(true);
        return panel;
    }
    
    
    private void initialiseDataSource()
	{
		try
        {
			// Set DB connection properties to read in sample service usage data.
			Properties connectionProps = new Properties();
			connectionProps.put("driverClassName", "");
			connectionProps.put("url", "jdbc:mysql://localhost/prelert");
			connectionProps.put("username", "root");
			connectionProps.put("password", "root123");
			
	        DataSource dataSource = BasicDataSourceFactory.createDataSource(connectionProps);
	        m_UsageDAO = new ServiceUsageViewMySQLDAO();
	        m_UsageDAO.setDataSource(dataSource);
	        
	        logger.debug("Created datasource: " + dataSource);
        }
        catch (Exception e)
        {
	        logger.error("Error creating data source", e);
        }
	}


	public static void main(String[] args)
	{
		final JFreeChartTestApp testApp = new JFreeChartTestApp();
		
		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				Date startTime = new Date();
				testApp.createAndShowGUI();
				Date endTime = new Date();
				
				logger.debug("Time to load chart: " + (endTime.getTime() - startTime.getTime()) + "ms");
			}
		});
	}

}
