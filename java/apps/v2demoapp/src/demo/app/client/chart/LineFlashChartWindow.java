package demo.app.client.chart;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.extjs.gxt.charts.client.Chart;
import com.extjs.gxt.charts.client.event.ChartEvent;
import com.extjs.gxt.charts.client.event.ChartListener;
import com.extjs.gxt.charts.client.model.ChartModel;
import com.extjs.gxt.charts.client.model.Legend;
import com.extjs.gxt.charts.client.model.LineDataProvider;
import com.extjs.gxt.charts.client.model.Scale;
import com.extjs.gxt.charts.client.model.ScaleProvider;
import com.extjs.gxt.charts.client.model.Legend.Position;
import com.extjs.gxt.charts.client.model.axis.XAxis;
import com.extjs.gxt.charts.client.model.axis.YAxis;
import com.extjs.gxt.charts.client.model.charts.ChartConfig;
import com.extjs.gxt.charts.client.model.charts.LineChart;
import com.extjs.gxt.charts.client.model.charts.ScatterChart;
import com.extjs.gxt.charts.client.model.charts.ScatterChart.ScatterStyle;
import com.extjs.gxt.charts.client.model.charts.dots.BaseDot;
import com.extjs.gxt.charts.client.model.charts.dots.Dot;
import com.extjs.gxt.charts.client.model.charts.dots.SolidDot;
import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.DateWrapper;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import demo.app.client.ViewWindow;
import demo.app.data.DatePagingLoadConfig;
import demo.app.data.DatePagingLoadResult;
import demo.app.data.TimeFrame;
import demo.app.data.Tool;
import demo.app.data.UsageRecord;
import demo.app.data.View;
import demo.app.service.DatabaseServiceLocator;
import demo.app.service.UsageQueryServiceAsync;

public class LineFlashChartWindow extends ViewWindow
{
	
	private UsageQueryServiceAsync	m_UsageQueryService;
	
	private Chart					m_FlashChart;
	private ChartModel				m_ChartModel;
	private ChartListener 			m_ChartListener;

	private List<UsageDataStore>	m_DataStores;
	
	private DateWrapper				m_TestStartTime; 		
	
	private int 					m_LineCounter = 0;
	
	
	public static ScaleProvider FLASH_CHART_SCALE_PROVIDER = new ScaleProvider()
	{
		public Scale calcScale(double min, double max)
		{
			Scale scale = DEFAULT_SCALE_PROVIDER.calcScale(min, max);
			scale.setMin(0);
			scale.setInterval(50000);
			return scale;
		}
	};
	
	
	public LineFlashChartWindow()
	{
		m_UsageQueryService = m_UsageQueryService = DatabaseServiceLocator.getInstance().getUsageQueryService("Services");
		m_DataStores = new ArrayList<UsageDataStore>();
		
		setIconStyle("line-graph-win-icon");
		setMinimizable(true);
		setMaximizable(true);
		setHeading("Flash Line Chart");
		setSize(750, 520);
		setResizable(true);
		
		initComponents();
	}
	
	
	protected void initComponents()
	{
		setLayout(new FitLayout());
		
		String url = GWT.getModuleBaseURL() + "gxt/chart/open-flash-chart.swf";   
		m_FlashChart = new Chart(url);   
	    
	    m_ChartListener = new ChartListener()
		{
            public void chartClick(ChartEvent ce)
			{
				ChartConfig chart = ce.getChartConfig();
				//GWT.log("Selected data series: " + chart.getText() + ", value: " + ce.getValue(), null);
				
				DateWrapper currentTime = new DateWrapper();
				MessageBox.confirm("FlashChart", "Time since load start: " + 
						(currentTime.getTime() - m_TestStartTime.getTime()) + "ms", null);
			}
		};
	    
	    m_ChartModel = new ChartModel("Server Usage",   
        	"color: #15428b; font-size: 14px; font-family: Verdana; font-weight: bold; text-align: left;");   
	    m_ChartModel.setBackgroundColour("#ffffff");   
	    m_ChartModel.setLegend(new Legend(Position.TOP, true));     
	    m_ChartModel.getLegend().setBorderColour("#cccccc");

	    
	    //m_ChartModel.setScaleProvider(ScaleProvider.ROUNDED_NEAREST_SCALE_PROVIDER); 
	    m_ChartModel.setScaleProvider(FLASH_CHART_SCALE_PROVIDER); 

 
	    
	    XAxis xAxis = new XAxis();
	    xAxis.setSteps(10);
	    xAxis.setGridColour("#ffffff");
	    xAxis.getLabels().setSteps(30);
	    xAxis.setColour("c6c7c7");
	    m_ChartModel.setXAxis(xAxis);
	    
	    
	    YAxis yAxis = new YAxis();
	    yAxis.setGridColour("#ffffff");
	    yAxis.setColour("c6c7c7");
	    m_ChartModel.setYAxis(yAxis);
	    
	    
	    // Add in the data series for line charts.
//	    addLineDataSource("active", null, null, "#0000FF");
//	    addLineDataSource("active", "sol30m-8201.1.p2ps", null, "#63B8FF");
//	    addLineDataSource("active", "sol30m-8202.1.p2ps", null, "#FFFF00");
//	    addLineDataSource("active", "sol30m-8203.1.p2ps", null, "#FF00FF");
	    addLineDataSource("serverload", null, null, "#FF0000");
//	    addLineDataSource("serverload", "sol30m-8201.1.p2ps", null, "#00FF00");
//	    addLineDataSource("serverload", "sol30m-8202.1.p2ps", null, "#B429FF");
//	    addLineDataSource("serverload", "sol30m-8203.1.p2ps", null, "#B4FF29");
	    addLineDataSource("serverload", "sol30m-8203.1.p2ps", "IDN_SELECTFEED", "#CCCC00");
	    addLineDataSource("serverload", "sol30m-8203.1.p2ps", "TEST_PRISM_TURQUOISE", "#67BACA"); // 70 points

	    
	    
	    // Test out a scatter plot for evidence data.	    
	    ScatterChart scatterChart = new ScatterChart();
	    scatterChart.addPoints(getScatterData());
	    m_ChartModel.addChartConfig(scatterChart); 

	  
	    m_FlashChart.setChartModel(m_ChartModel); 
	    
	    // Create a toolbar with a Refresh button.
	    ToolBar toolBar = new ToolBar();   
		Button refreshBtn = new Button();
		refreshBtn.setIcon(GXT.IMAGES.paging_toolbar_refresh());
		refreshBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
			{
				//load();
				addDataSeries();
			}
		});
		toolBar.add(refreshBtn);
	    setTopComponent(toolBar);

		add(m_FlashChart);
	}
	

	@Override
    public void load()
	{
		m_TestStartTime = new DateWrapper();
		GWT.log("Timer started at:" + m_TestStartTime.toString(), null);
		
		for (UsageDataStore dataStore : m_DataStores)
		{
			dataStore.removeAll();
		}

		
		for (UsageDataStore dataStore : m_DataStores)
		{
			loadData(dataStore);
			//loadTimeSeriesData(dataStore);
		}
		
		
	}
	
	
	public void addDataSeries()
	{
		// Test out adding extra lines in stages.
		if (m_LineCounter == 0)
		{
			UsageDataStore dataStore = addLineDataSource("serverload", "sol30m-8201.1.p2ps", null, "#00FF00");
			loadData(dataStore);
			m_LineCounter++;
		}
		else if (m_LineCounter == 1)
		{
			UsageDataStore dataStore = addLineDataSource("serverload", "sol30m-8202.1.p2ps", null, "#B429FF");
			loadData(dataStore);
			m_LineCounter++;
		}
		else if (m_LineCounter == 2)
		{
			UsageDataStore dataStore = addLineDataSource("serverload", "sol30m-8203.1.p2ps", null, "#B4FF29");
			loadData(dataStore);
			m_LineCounter++;
		}
		
		m_FlashChart.setChartModel(m_ChartModel); 
		
	}
	
	
	public UsageDataStore addLineDataSource(String metric, String source, String service, String lineColour)
	{
		UsageDataStore dataStore = new UsageDataStore(metric, source, service); 
	    LineChart lineChart = new LineChart();   
	    lineChart.setText(metric + " - " + source);   
	    lineChart.setColour(lineColour);   
	    LineDataProvider lineProvider = new LineDataProvider("value"); 
	    lineProvider.bind(dataStore);   
	    lineChart.setDataProvider(lineProvider);   
	    lineChart.addChartListener(m_ChartListener);
	    lineChart.getDotStyle().setTooltip(metric + "<br>#val#"); 
	    m_ChartModel.addChartConfig(lineChart); 
	 	m_DataStores.add(dataStore);
	 	
	 	return dataStore;
	}
	
	
	public void addDataSource(String metric, String source, String service, String lineColour)
	{
		UsageDataStore dataStore = new UsageDataStore(metric, source, service); 
	    m_DataStores.add(dataStore);
	}
	
	
	protected void loadData(UsageDataStore store)
	{
		final UsageDataStore dataStore = store;
		
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
	        	
	        	GWT.log("FlashChartWindow loadData() - " + dataStore.getMetric() + 
	        			", " + dataStore.getSource() + ", " + dataStore.getService(), null);
	        	GWT.log("FlashChartWindow - " + records.size() + " points at " + new DateWrapper(), null);
	            
            }
	
		});
		
	}
	
	
	protected void loadTimeSeriesData(UsageDataStore store)
	{
		final UsageDataStore dataStore = store;
		
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
	    	    scatterChart.addChartListener(m_ChartListener);
	    	    
	    	    List<String> xAxisLabels = new ArrayList<String>();
	    	    
	    	    for (UsageRecord rec : records)
	    	    {
	    	    	scatterChart.addPoints(createTimeSeriesDot((int)rec.getTime().getTime(), rec.getValue(), 
	    	    			"#ff0000", dataStore.getMetric() + "<br>#val#"));
	    	    	GWT.log("Added point at x: " + (int)rec.getTime().getTime(),null);
	    	    }
	            
	    	    m_ChartModel.addChartConfig(scatterChart);
	    	    m_ChartModel.getXAxis().setRange( 1294284160, 1380684160);  // Range of 1 day.
	    	    m_ChartModel.getXAxis().setSteps(1000*60*60*4);				// Steps every 4 hours
	    	    m_ChartModel.getXAxis().getLabels().setSteps(1000*60*60*4);
            }
	
		});
		
	}
	
	
	private ArrayList<BaseDot> getScatterData()
	{
		ArrayList<BaseDot> dots = new ArrayList<BaseDot>();
		
		dots.add(createScatterDot(30, 2500, "#63B8FF", "rrcp congestion<br>10:02:11"));
		dots.add(createScatterDot(40, 2500, "#FFFF00", "rrcp congestion<br>10:02:31"));
		dots.add(createScatterDot(50, 2500, "#FFB429", "rrcp congestion<br>10:02:41"));
		dots.add(createScatterDot(50, 9000, "#FFFF00", "rrcp congestion<br>10:02:41"));
		
		dots.add(createScatterDot(60, 2500, "#63B8FF", "rrcp congestion<br>10:03:11"));
		dots.add(createScatterDot(60, 9000, "#FFFF00", "rrcp congestion<br>10:03:11"));
		dots.add(createScatterDot(70, 2500, "#FFB429", "rrcp congestion<br>10:03:41"));
		dots.add(createScatterDot(80, 9000, "#FFFF00", "rrcp congestion<br>10:03:51"));
		
		
		dots.add(createScatterDot(90, 2500, "#63B8FF", "rrcp congestion<br>10:04:11"));
		dots.add(createScatterDot(90, 9000, "#FFFF00", "rrcp congestion<br>10:04:11"));
		dots.add(createScatterDot(95, 2500, "#FFB429", "rrcp congestion<br>10:04:31"));
		dots.add(createScatterDot(100, 9000, "#FFFF00", "rrcp congestion<br>10:04:51"));
	    
	    return dots;
	}
	
	
	
	
	
	private SolidDot createScatterDot(int x, int y, String colour, String tooltip)
	{
		SolidDot dot = new SolidDot();
	    dot.setXY(x, y);
	    dot.setColour(colour);
	    dot.setTooltip(tooltip);

	    return dot;    
	}
	
	
	private Dot createTimeSeriesDot(int x, double y, String colour, String tooltip)
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
		
		UsageDataStore(String metric, String source)
		{
			this(metric, source, null);
		}
		
		
		UsageDataStore(String metric, String source, String service)
		{
			m_Metric = metric;
			m_Source = source;
			m_Service = service;
			
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
	}


}
