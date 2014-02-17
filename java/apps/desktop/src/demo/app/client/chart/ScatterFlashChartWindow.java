package demo.app.client.chart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.extjs.gxt.charts.client.Chart;
import com.extjs.gxt.charts.client.event.ChartEvent;
import com.extjs.gxt.charts.client.event.ChartListener;
import com.extjs.gxt.charts.client.model.BarDataProvider;
import com.extjs.gxt.charts.client.model.ChartModel;
import com.extjs.gxt.charts.client.model.DataProvider;
import com.extjs.gxt.charts.client.model.Legend;
import com.extjs.gxt.charts.client.model.LineDataProvider;
import com.extjs.gxt.charts.client.model.Scale;
import com.extjs.gxt.charts.client.model.ScaleProvider;
import com.extjs.gxt.charts.client.model.Legend.Position;
import com.extjs.gxt.charts.client.model.axis.XAxis;
import com.extjs.gxt.charts.client.model.axis.YAxis;
import com.extjs.gxt.charts.client.model.charts.BarChart;
import com.extjs.gxt.charts.client.model.charts.ChartConfig;
import com.extjs.gxt.charts.client.model.charts.DataConfig;
import com.extjs.gxt.charts.client.model.charts.LineChart;
import com.extjs.gxt.charts.client.model.charts.ScatterChart;
import com.extjs.gxt.charts.client.model.charts.Shape;
import com.extjs.gxt.charts.client.model.charts.BarChart.BarStyle;
import com.extjs.gxt.charts.client.model.charts.ScatterChart.ScatterStyle;
import com.extjs.gxt.charts.client.model.charts.dots.Anchor;
import com.extjs.gxt.charts.client.model.charts.dots.BaseDot;
import com.extjs.gxt.charts.client.model.charts.dots.Dot;
import com.extjs.gxt.charts.client.model.charts.dots.HollowDot;
import com.extjs.gxt.charts.client.model.charts.dots.SolidDot;
import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.data.BaseModel;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.DateWrapper;
import com.extjs.gxt.ui.client.util.DelayedTask;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import demo.app.client.ViewWindow;
import demo.app.client.chart.LineFlashChartWindow.UsageDataStore;
import demo.app.data.DatePagingLoadConfig;
import demo.app.data.DatePagingLoadResult;
import demo.app.data.TimeFrame;
import demo.app.data.Tool;
import demo.app.data.UsageRecord;
import demo.app.data.View;
import demo.app.service.DatabaseServiceLocator;
import demo.app.service.UsageQueryServiceAsync;

public class ScatterFlashChartWindow extends ViewWindow
{
	
	private UsageQueryServiceAsync	m_UsageQueryService;
	
	private Chart					m_FlashChart;
	private ChartModel				m_ChartModel;
	private ChartListener 			m_ChartListener;
	private ScatterChart			m_ScatterChart;
	
	private List<UsageDataStore>	m_DataStores;
	private double					m_MaxValue;
	
	private DateWrapper				m_TestStartTime; 	
	
	private int						m_DataCounter = 0;
	private boolean					m_IsLoaded;
	
	
	public static ScaleProvider FLASH_CHART_SCALE_PROVIDER = new ScaleProvider()
	{
		public Scale calcScale(double min, double max)
		{
			Scale scale = DEFAULT_SCALE_PROVIDER.calcScale(min, max);
			scale.setMin(0);
			scale.setMax(max);
			scale.setInterval(50000);
			return scale;
		}
	};
	
	
	public ScatterFlashChartWindow()
	{
		m_UsageQueryService = DatabaseServiceLocator.getInstance().getUsageQueryService("Services");
		m_DataStores = new ArrayList<UsageDataStore>();
		
		setIconStyle("line-graph-win-icon");
		setMinimizable(true);
		setMaximizable(true);
		setHeading("Flash Scatter Chart");
		setSize(750, 520);
		setResizable(true);
		
		initTimeScatterComponents();
	}
	

	
	
	protected void initTimeScatterComponents()
	{
		setLayout(new FitLayout());
		
		String url = GWT.getModuleBaseURL() + "gxt/chart/open-flash-chart.swf";   
		m_FlashChart = new Chart(url);   
	    
	    m_ChartListener = new ChartListener()
		{
			public void chartClick(ChartEvent ce)
			{
				DateWrapper currentTime = new DateWrapper();
				MessageBox.confirm("Scatter Chart", "Time since load start: " + 
						(currentTime.getTime() - m_TestStartTime.getTime()) + "ms", null);
			}
		};
	    
	    m_ChartModel = new ChartModel("Service Usage",   
        	"color: #15428b; font-size: 14px; font-family: Verdana; font-weight: bold; text-align: left;"){
	    	
	    	 public void updateYScale() 
	    	 {
	    		 Scale scale = m_ChartModel.getScaleProvider().calcScale(0, m_MaxValue);

	    		 YAxis yAxis = getYAxis();
	    		 yAxis.setMin(scale.getMin());
	    		 yAxis.setMax(scale.getMax());
	    		 yAxis.setSteps(scale.getInterval());
			}
	    	
	    };   
	    m_ChartModel.setBackgroundColour("#ffffff");   
	    m_ChartModel.setLegend(new Legend(Position.TOP, true));     
	    m_ChartModel.getLegend().setBorderColour("#cccccc");
	    
	    m_ChartModel.setScaleProvider(FLASH_CHART_SCALE_PROVIDER); 

  
	    XAxis xAxis = new XAxis();
	    xAxis.setSteps(60*60*2);		// i.e. a step of 2 hours
	    xAxis.setGridColour("#ffffff");
	    xAxis.getLabels().setSteps(60*60*6); 	// i.e. labels every 6 hours
	    xAxis.getLabels().setText("#date:Y-m-d H:i:s#" ); // This works with values in secs i.e. NOT ms!!
	    xAxis.setColour("c6c7c7");
	    xAxis.setRange((new DateWrapper(2009, 6, 6)).asDate().getTime()/1000, 
	   		(new DateWrapper(2009, 6, 7)).asDate().getTime()/1000);
	    m_ChartModel.setXAxis(xAxis);
	    
	    
	    YAxis yAxis = new YAxis();
	    yAxis.setGridColour("#ffffff");
	    yAxis.setColour("c6c7c7");
	    m_ChartModel.setYAxis(yAxis);
	    
	    
	    // Test out a scatter plot for evidence data.	    
	    m_ScatterChart = new ScatterChart();
	    m_ScatterChart.addPoints(getNotificationData());
	    m_ChartModel.addChartConfig(m_ScatterChart); 
	    m_ScatterChart.addChartListener(m_ChartListener);
	    
	    // Add in the data series for scatter charts.
	    addDataSource("active", null, null, "#0000FF");
	    addDataSource("active", "sol30m-8201.1.p2ps", null, "#63B8FF");
//	    addDataSource("active", "sol30m-8202.1.p2ps", null, "#FFFF00");
//	    addDataSource("active", "sol30m-8203.1.p2ps", null, "#FF00FF");
	    addDataSource("serverload", null, null, "#FF0000");
//	    addDataSource("serverload", "sol30m-8201.1.p2ps", null, "#00FF00");
//	    addDataSource("serverload", "sol30m-8202.1.p2ps", null, "#B429FF");
//	    addDataSource("serverload", "sol30m-8203.1.p2ps", null, "#B4FF29");
//	    addDataSource("serverload", "sol30m-8203.1.p2ps", "IDN_SELECTFEED", "#CCCC00");
	    addDataSource("serverload", "sol30m-8203.1.p2ps", "TEST_PRISM_TURQUOISE", "#67BACA"); // 70 points

	  
	    m_FlashChart.setChartModel(m_ChartModel); 
	    
		add(m_FlashChart);
	}
	

	public void load()
	{
		if (m_IsLoaded == false)
		{
			m_TestStartTime = new DateWrapper();
			
			for (UsageDataStore dataStore : m_DataStores)
			{
				loadTimeSeriesData(dataStore);
			}
			
			m_IsLoaded = true;
		}
	}
	
	
	protected void loadTimeSeriesData(UsageDataStore store)
	{
		final UsageDataStore dataStore = store;
		final String scatterColour = store.getLineColour();
		
		DatePagingLoadConfig loadConfig = new DatePagingLoadConfig();
		loadConfig.setTimeFrame(TimeFrame.DAY);
		loadConfig.setDate((new DateWrapper(2009, 6, 6)).asDate());
		
		m_UsageQueryService.getUsageData(store.getMetric(), store.getSource(), 
				store.getService(), loadConfig, new AsyncCallback<DatePagingLoadResult<UsageRecord>>()
		{
            public void onFailure(Throwable caught)
            {
            	MessageBox.alert("Prelert - Error",
		                "Failed to get a response from the server", null); 
            }

            public void onSuccess(DatePagingLoadResult<UsageRecord> result)
            {                   	
            	List<UsageRecord> records = result.getData();
	        	dataStore.add(records);
	        	
	        	GWT.log("FlashChartWindow - loadTimeSeriesData() " + dataStore.getMetric() + 
	        			", " + dataStore.getSource() + ", " + dataStore.getService(), null);
	        	GWT.log("FlashChartWindow - " + records.size() + " points at " + new DateWrapper(), null);
	        	
	        	ScatterChart scatterChart = new ScatterChart(ScatterStyle.LINE);
	        	scatterChart.setColour(scatterColour);
	    		scatterChart.addChartListener(m_ChartListener);
	    	    Collection<BaseDot> dots = new ArrayList<BaseDot>();
	    	    
	    	    double recValue;
	    	    for (UsageRecord rec : records)
	    	    {
	    			recValue = rec.getValue();
	    			m_MaxValue = Math.max(m_MaxValue, recValue);
	    			
	    			if (recValue < 135000)
	    			{
	    				dots.add(createTimeSeriesDot(rec.getTime().getTime()/1000, rec.getValue(), 
	    					scatterColour, dataStore.getMetric() + "<br>#val#"));
	    			}
	    			else
	    			{
	    				// Test out highlighting discords.
	    				dots.add(createNotificationDot(rec.getTime().getTime()/1000, rec.getValue(), 
		    					"#ff8000", dataStore.getMetric() + "<br>#val#"));
	    			}
	    	    }
	    	    
	    	    scatterChart.addPoints(dots);
	    	    m_ChartModel.addChartConfig(scatterChart);
	    	    
	    	    m_DataCounter++;
	    	    
	    	    if (m_DataCounter == m_DataStores.size())
	    	    {
	    	    	m_ChartModel.addChartConfig(getTimelineShape());
	    	    	
	    	    	m_FlashChart.refresh();
	    	    	GWT.log("FlashChartWindow finished refresh at: " + new DateWrapper(), null);
	    	    }
            }
	
		});
		
	}
	
	
	public void addDataSource(String metric, String source, String service, String lineColour)
	{
		UsageDataStore dataStore = new UsageDataStore(metric, source, service); 
		dataStore.setLineColour(lineColour);
	    m_DataStores.add(dataStore);
	}
	
	
	private ArrayList<BaseDot> getNotificationData()
	{
		ArrayList<BaseDot> dots = new ArrayList<BaseDot>();
		
		DateWrapper baseTime = new DateWrapper(2009, 6, 6);
		
		long timeMs = baseTime.getTime()/1000;
		long hours = 60*60;
		
		dots.add(createNotificationDot(timeMs + (2*hours), 2500, "#63B8FF", 
				"rrcp congestion<br>#date:Y-m-d H:i:s#"));
		dots.add(createNotificationDot(timeMs + (4*hours), 2500, "#FFB429", 
				"rrcp congestion<br>#date:Y-m-d H:i:s#"));
		dots.add(createNotificationDot(timeMs + (8*hours), 2500, "#FFFF00", 
				"Source has shutdown<br>#date:Y-m-d H:i:s#"));
		
		dots.add(createNotificationDot(timeMs + (10*hours), 9000, "#FFFF00", 
				"Service has shutdown<br>#date:Y-m-d H:i:s#"));
		dots.add(createNotificationDot(timeMs + (10*hours), 2500, "#FFB429", 
				"rrcp congestion<br>#date:Y-m-d H:i:s#"));
		dots.add(createNotificationDot(timeMs + (12*hours), 2500, "#FFFF00", 
				"Service has started<br>#date:Y-m-d H:i:s#"));
		
		dots.add(createNotificationDot(timeMs + (14*hours), 2500, "#63B8FF", 
				"Source has started<br>#date:Y-m-d H:i:s#"));
		dots.add(createNotificationDot(timeMs + (15*hours), 2500, "#FFB429", 
				"rrcp congestion<br>#date:Y-m-d H:i:s#"));
		dots.add(createNotificationDot(timeMs + (16*hours), 2500, "#FFFF00", 
				"rrcp congestion<br>#date:Y-m-d H:i:s#"));
		
		m_MaxValue = 9000;
	    
	    return dots;
	}
	
	
	private Shape getTimelineShape()
	{
		Shape timeLine = new Shape();
		timeLine.setColour("#ff8000");
		timeLine.setAlpha(0.2f);
		
		DateWrapper baseTime = new DateWrapper(2009, 6, 6);
		long timeMs = baseTime.getTime()/1000;
		long hours = 60*60;
		
    	timeLine.addPoint(timeMs + (11*hours) + 1800, 0);
    	timeLine.addPoint(timeMs + (13*hours) - 1800, 0);
    	timeLine.addPoint(timeMs + (13*hours) - 1800, m_MaxValue);
    	timeLine.addPoint(timeMs + (11*hours) + 1800, m_MaxValue);	
	    
	    return timeLine;
	}
	
	
	private SolidDot createNotificationDot(long x, double y, String colour, String tooltip)
	{
		SolidDot dot = new SolidDot();
	    dot.setXY(x, y);
	    dot.setColour(colour);
	    dot.setTooltip(tooltip);

	    return dot;    
	}
	
	
	private Dot createTimeSeriesDot(long x, double y, String colour, String tooltip)
	{
		Dot dot = new Dot();
	    dot.setXY(x, y);
	    dot.setColour(colour);
	    dot.setTooltip(tooltip);

	    return dot;    
	}
	
	
	/**
	 * Returns the View displayed in the Window.
	 * @return the view displayed in the Window.
	 */
	public View getView()
	{
		// TO DO.
		return null;
	}
	
	
	public void runTool(Tool tool)
	{
		// TO DO.
	}
	
	
	class UsageDataStore extends ListStore<UsageRecord>
	{
		private String m_Metric;
		private String m_Source;
		private String m_Service;
		
		private String m_LineColour;
		
		UsageDataStore(String metric, String source)
		{
			this(metric, source, null);
		}
		
		
		UsageDataStore(String metric, String source, String service)
		{
			m_Metric = metric;
			m_Source = source;
			m_Service = service;
			
			m_LineColour = "#ff0000";
			
			// Add a record to initialise store.
			UsageRecord rec = new UsageRecord();
			rec.set("time", new Date());
			rec.set("value", 0);
			
			add(rec);
		}
		
		
		public String getSource()
		{
			return m_Source;
		}
		
		
		public String getMetric()
		{
			return m_Metric;
		}
		
		
		public String getService()
		{
			return m_Service;
		}
		
		
		public void setLineColour(String lineColour)
		{
			m_LineColour = lineColour;
		}
		
		
		public String getLineColour()
		{
			return m_LineColour;
		}
	}


}
