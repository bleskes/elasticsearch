package demo.app.client.splash.applet;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.Month;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleInsets;

import demo.app.data.TimeSeriesConfig;
import demo.app.data.UsageDataItem;

import netscape.javascript.JSObject;

public class ChartApplet extends JApplet
{
	
	private List<TimeSeriesConfig>		m_TimeSeriesConfigs;
	private TimeSeriesCollection 		m_Dataset;
	

	private JTextField m_Counter;
	
	private JFreeChart	m_Chart;
	
	private JLabel m_SourceLabel;


	public void init()
	{
		System.out.println("Codebase: " + getCodeBase());	// e.g. http://localhost:8080/splash/
		
		
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
			System.err.println("initComponents() didn't complete successfully");
		}
		
		
		m_TimeSeriesConfigs = new ArrayList<TimeSeriesConfig>();
	    addTimeSeriesConfig("serverload", null, null, "#FF0000");
	    addTimeSeriesConfig("serverload", "sol30m-8203.1.p2ps", null, "#B4FF29");
	    addTimeSeriesConfig("serverload", "sol30m-8203.1.p2ps", "IDN_SELECTFEED", "#CCCC00");
	    
	    m_Dataset = new TimeSeriesCollection();
	}
	
	
	protected void initComponents()
	{
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.setBackground(Color.WHITE);
		contentPanel.setBorder(BorderFactory.createTitledBorder("Applet Demo"));
		
		JPanel labelPanel = new JPanel();
		labelPanel.setBackground(Color.WHITE);
		m_SourceLabel = new JLabel("Source: london-web1");
		labelPanel.add(m_SourceLabel);

		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBackground(Color.WHITE);
		
		
		JPanel counterPanel = new JPanel();
		counterPanel.setBackground(Color.WHITE);
		m_Counter = new JTextField(20);
		m_Counter.setHorizontalAlignment(JTextField.CENTER);
		m_Counter.setText("0");
		m_Counter.setEditable(false);
		counterPanel.add(new JLabel("Current count : "));
		counterPanel.add(m_Counter);
		
		// Add a button to demonstrate Applet->Window communication.
		JButton displayBtn = new JButton("Send to window");
		displayBtn.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent e)
            {
				int currentCount = getCurrentValue();
	            
	            JSObject window = JSObject.getWindow(ChartApplet.this);
	            window.call("displayCounterValue", new Object[]{currentCount});

            }
		});
		counterPanel.add(displayBtn);
		
		// Create the chart.
		m_Chart = createChart(createStaticDataset());
		ChartPanel chartPanel = new ChartPanel(m_Chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(400, 300));
		chartPanel.setFillZoomRectangle(true);
		chartPanel.setMouseWheelEnabled(true);
		
		mainPanel.add(counterPanel, BorderLayout.NORTH);
		mainPanel.add(chartPanel, BorderLayout.CENTER);
				
		
		// Add a button to get usage data from the server to display in the chart.
		JPanel btnPanel = new JPanel();
		btnPanel.setBackground(Color.WHITE);
		
		JButton getUsageBtn = new JButton("Load data");
		getUsageBtn.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent e)
            {
				loadUsageData();
            }
			
		});
		btnPanel.add(getUsageBtn);
		
		
		contentPanel.add(labelPanel, BorderLayout.NORTH);
		contentPanel.add(mainPanel, BorderLayout.CENTER);
		contentPanel.add(btnPanel, BorderLayout.SOUTH);

		getContentPane().add(contentPanel);
	}
	
	
	/**
     * Creates the usage chart.
     *
     * @param dataset  a dataset.
     *
     * @return A chart.
     */
    private JFreeChart createChart(XYDataset dataset) 
    {

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "Service Usage ",  	// title
            "Time",             // x-axis label
            "Usage",   			// y-axis label
            dataset,            // data
            false,				// create legend?
            true,				// generate tooltips?
            false				// generate URLs?
        );

        chart.setBackgroundPaint(Color.white);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        

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


	public void increment()
	{
		int currentCount = getCurrentValue();
		currentCount++;
		m_Counter.setText(currentCount + "");
	}


	public void decrement()
	{
		int currentCount = getCurrentValue();
		currentCount--;
		m_Counter.setText(currentCount + "");
	}


	public int getCurrentValue()
	{
		int currentCount = Integer.parseInt(m_Counter.getText());
		return currentCount;
	}
	
	
	public void setSourceName(String name)
	{
		m_SourceLabel.setText(name);
	}
	
	
	public void addTimeSeriesConfig(String metric, String source, String service, String lineColour)
	{
		TimeSeriesConfig config = new TimeSeriesConfig(metric, source, service); 
		config.setLineColour(lineColour);
		m_TimeSeriesConfigs.add(config);
	}
	
	
	public void loadUsageData()
	{
		XYPlot plot = (XYPlot)(m_Chart.getPlot());
		plot.setDataset(m_Dataset);
		
		try
        {
			// Load some test usage data.
//		    addDataSource("serverload", "sol30m-8203.1.p2ps", null, "#B4FF29");
//		    addDataSource("serverload", "sol30m-8203.1.p2ps", "IDN_SELECTFEED", "#CCCC00");
			
			String serviceSpec;
			for (TimeSeriesConfig config : m_TimeSeriesConfigs)
			{
				serviceSpec = "services/usageQueryService?action=getUsageData";
				serviceSpec += "&metric=";
				serviceSpec += config.getMetric();
				if (config.getSource() != null)
				{
					serviceSpec += "&source=";
					serviceSpec += config.getSource();
				}
				if (config.getUser() != null)
				{
					serviceSpec += "&user=";
					serviceSpec += config.getUser();
				}
				
		        URL qryServiceUrl = new URL(getCodeBase(), serviceSpec);
		        System.out.println("qryServiceUrl: " + qryServiceUrl); 
		        
		        URLConnection conn = qryServiceUrl.openConnection();
		        
		        // Try reading in Object data.
		        ObjectInputStream in = new ObjectInputStream(conn.getInputStream());
		        ArrayList<UsageDataItem> usageData = (ArrayList<UsageDataItem>)(in.readObject());
		        System.out.println("Number of usage data items loaded: " + usageData.size());
		        
		        // Load the data into the chart.
		        addTimeSeries(getTimeSeriesLabel(config), usageData);
			}
			
			
			

	        
	        // Try reading in character data.
//	        BufferedReader in = new BufferedReader(
//	                                new InputStreamReader(conn.getInputStream()));
//	        String inputLine;
//
//	        while ((inputLine = in.readLine()) != null) 
//	        {
//	            System.out.println(inputLine);
//	        }
//	        in.close();
	        
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
	
	
	public void addTimeSeries(String label, ArrayList<UsageDataItem> usageData)
	{
		TimeSeries timeSeries = buildTimeSeries(label, usageData);
		m_Dataset.addSeries(timeSeries);		
	}
	
	
	private TimeSeries buildTimeSeries(String name, List<UsageDataItem> usageData)
	{
		TimeSeries timeSeries = new TimeSeries(name);
		
		for (UsageDataItem dataItem: usageData)
		{
			timeSeries.add(new Minute(dataItem.getTime()), dataItem.getValue());
			
		}
        
        return timeSeries;
	}
	
	
    private String getTimeSeriesLabel(TimeSeriesConfig dataSeries)
    {
    	String label = new String(dataSeries.getMetric());
    	String source = dataSeries.getSource();
    	String user = dataSeries.getUser();
    	
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
    private static XYDataset createStaticDataset() 
    {

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
	
}

