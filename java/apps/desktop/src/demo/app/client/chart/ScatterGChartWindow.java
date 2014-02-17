package demo.app.client.chart;

import java.util.ArrayList;
import java.util.List;

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


public class ScatterGChartWindow extends ViewWindow
{
	private UsageChart				m_UsageChart;
	
	private UsageQueryServiceAsync	m_UsageQueryService;
	private List<UsageDataStore>	m_DataStores;
	
	private DateWrapper				m_TestStartTime; 
	private int						m_DataCounter = 0;
	private boolean					m_IsLoaded;
	
	
	public ScatterGChartWindow()
	{
		m_UsageQueryService = DatabaseServiceLocator.getInstance().getUsageQueryService("Services");
		m_DataStores = new ArrayList<UsageDataStore>();
		
		setIconStyle("line-graph-win-icon");
		setMinimizable(true);
		setMaximizable(true);
		setHeading("GChart Scatter Chart");
		setSize(750, 520);
		setResizable(true);
		
		initComponents();	
	}
	
	
	protected void initComponents()
	{
		setLayout(new FitLayout());
		
	    // Create a toolbar with a Refresh button.
	    ToolBar toolBar = new ToolBar();   
		Button refreshBtn = new Button();
		refreshBtn.setIcon(GXT.IMAGES.paging_toolbar_refresh());
		refreshBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
			{
				load();
			}
		});
		
		toolBar.add(refreshBtn);
	    setTopComponent(toolBar);
	 
	    
		// Add in the data series for the charts.
	    addDataSource("active", null, null, "#0000FF");
	    addDataSource("active", "sol30m-8201.1.p2ps", null, "#63B8FF");
//	    addDataSource("active", "sol30m-8202.1.p2ps", null, "#FFFF00");
//	    addDataSource("active", "sol30m-8203.1.p2ps", null, "#FF00FF");
//	    addDataSource("serverload", null, null, "#FF0000");
//	    addDataSource("serverload", "sol30m-8201.1.p2ps", null, "#00FF00");
//	    addDataSource("serverload", "sol30m-8202.1.p2ps", null, "#B429FF");
//	    addDataSource("serverload", "sol30m-8203.1.p2ps", null, "#B4FF29");
//	    addDataSource("serverload", "sol30m-8203.1.p2ps", "IDN_SELECTFEED", "#CCCC00");
	    addDataSource("serverload", "sol30m-8203.1.p2ps", "TEST_PRISM_TURQUOISE", "#67BACA"); // 70 points
	    
	    
		// Create the Usage chart itself.
		m_UsageChart = new UsageChart();
		m_UsageChart.setChartSize(600, 350);
		add(m_UsageChart);
	}
	
	
	public void addDataSource(String metric, String source, String service, String lineColour)
	{
		UsageDataStore dataStore = new UsageDataStore(metric, source, service); 
		dataStore.setLineColour(lineColour);
	    m_DataStores.add(dataStore);
	}
	
	
	/**
	 * Loads usage data into the window.
	 */
	@Override
    public void load()
	{
		if (m_IsLoaded == false)
		{
			m_TestStartTime = new DateWrapper();
			GWT.log("ScatterGChartWindow load started at- " + m_TestStartTime, null);
			
			for (UsageDataStore dataStore : m_DataStores)
			{
				loadTimeSeriesData(dataStore);
			}
			
			m_IsLoaded = true;
		}
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
	        	
	        	GWT.log("ScatterGChartWindow - loadTimeSeriesData() " + dataStore.getMetric() + 
	        			", " + dataStore.getSource() + ", " + dataStore.getService(), null);
	        	GWT.log("ScatterGChartWindow - " + records.size() + " points at " + new DateWrapper(), null);
	    	    
	    		m_UsageChart.addTimeSeries(records, dataStore.getLineColour());
	    		
	    		if (m_DataCounter == 0)
	    		{
	    			m_UsageChart.setXAxisRange(TimeFrame.DAY, result.getDate());
	    		}

	    		// Only update the chart once all results have been received.
	    		m_DataCounter++;  
	    	    if (m_DataCounter == m_DataStores.size())
	    	    {
	    	    	m_UsageChart.update();	
	    	    	
	    	    	DateWrapper currentTime = new DateWrapper();
					long timeTaken = currentTime.getTime() - m_TestStartTime.getTime();
					GWT.log("ScatterGChartWindow load completed in " + 
							timeTaken + "ms at " + currentTime, null);
					
					MessageBox.confirm("ScatterGChartWindow", "Time since load start: " + 
							timeTaken + "ms", null);
	    	    }
            }
	
		});
		
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
