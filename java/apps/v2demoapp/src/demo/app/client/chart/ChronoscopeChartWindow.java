package demo.app.client.chart;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import demo.app.client.ViewWindow;
import demo.app.data.DatePagingLoadConfig;
import demo.app.data.DatePagingLoadResult;
import demo.app.data.TimeFrame;
import demo.app.data.TimeSeriesConfig;
import demo.app.data.Tool;
import demo.app.data.UsageRecord;
import demo.app.data.View;
import demo.app.service.DatabaseServiceLocator;
import demo.app.service.UsageQueryServiceAsync;

import com.extjs.gxt.ui.client.util.DateWrapper;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import org.timepedia.chronoscope.client.browser.ChartPanel;
import org.timepedia.chronoscope.client.browser.Chronoscope;
import org.timepedia.chronoscope.client.XYDataset;


public class ChronoscopeChartWindow extends ViewWindow
{
	private UsageQueryServiceAsync	m_UsageQueryService;
	private List<TimeSeriesConfig>	m_DataSeries;
	
	private VerticalPanel			m_ContentPanel;
	
	private DateWrapper				m_TestStartTime; 
	private int						m_DataCounter = 0;
	private boolean					m_IsLoaded;
	
	
	public ChronoscopeChartWindow()
	{
		m_UsageQueryService = DatabaseServiceLocator.getInstance().getUsageQueryService("Services");
		m_DataSeries = new ArrayList<TimeSeriesConfig>();
		
		setIconStyle("line-graph-win-icon");
		setMinimizable(true);
		setMaximizable(true);
		setHeading("Chronoscope Chart");
		setSize(750, 520);
		setResizable(true);
		
		// Add in the data series for the charts.
	    addDataSeries("active", null, null);
		
		initComponents();
	}
	
	
	protected void initComponents()
	{
		setLayout(new FitLayout());
	
		Chronoscope.setFontBookRendering(true);
		
		m_ContentPanel = new VerticalPanel();
		m_ContentPanel.add(new Label("Chronoscope Chart"));
		add(m_ContentPanel);
	}
	
	
	public void addDataSeries(String metric, String source, String service)
	{
		TimeSeriesConfig dataSeries = new TimeSeriesConfig("p2psmon_servers", metric, source, "service", service); 
		m_DataSeries.add(dataSeries);
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
			
			GWT.log("ChronoscopeChartWindow load started at " + m_TestStartTime, null);
			
			for (TimeSeriesConfig dataSeries : m_DataSeries)
			{
				loadTimeSeries(dataSeries);
			}
			
			m_IsLoaded = true;
		}
	}
	

    public void loadTimeSeries(TimeSeriesConfig dataSeriesConfig)
    {
    	final TimeSeriesConfig config = dataSeriesConfig;
		
		DatePagingLoadConfig loadConfig = new DatePagingLoadConfig();
		loadConfig.setTimeFrame(TimeFrame.DAY);
		loadConfig.setDate((new DateWrapper(2009, 6, 6)).asDate());
		
		m_UsageQueryService.getUsageData(config.getMetric(), config.getSource(), 
				config.getAttributeValue(), loadConfig, new AsyncCallback<DatePagingLoadResult<UsageRecord>>()
		{
            public void onFailure(Throwable caught)
            {
            	MessageBox.alert("Prelert - Error",
		                "Failed to get a response from the server", null); 
            }

            public void onSuccess(DatePagingLoadResult<UsageRecord> result)
            {
            	List<UsageRecord> records = result.getData();
            	
	        	// Only update the chart once all results have been received.
	    		m_DataCounter++;  
	    	    if (m_DataCounter == m_DataSeries.size())
	    	    {
	    	    	UsageDataset[] datasets = new UsageDataset[1];
	    	    	datasets[0] = new UsageDataset("usage1", records);
	    	    	
	    	    	ChartPanel chartPanel = Chronoscope.createTimeseriesChart(datasets, 700, 500);
	    	    	m_ContentPanel.add(chartPanel);
	    	    }
            }
	
		});
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
    
	
	class UsageDataset implements XYDataset
	{
		private String m_Id;
		private List<UsageRecord> m_UsageRecords;
		private double m_MaxVal;
		
		public UsageDataset(String id, List<UsageRecord> records)
		{
			m_Id = id;
			m_UsageRecords = records;
			for (UsageRecord rec : m_UsageRecords)
			{
				m_MaxVal = Math.max(m_MaxVal, rec.getValue());
			}
		}


        public String getAxisId()
        {
	        return "time";
        }

        public String getIdentifier()
        {
	        return m_Id;
        }

        
        public int getNumSamples()
        {
			return m_UsageRecords.size();
        }

        
        public int getNumSamples(int level)
        {
			return getNumSamples();
        }

        
        public String getRangeLabel()
        {
	        return "Usage";
        }

        
        public double getRangeBottom()
        {
	        return 0;
        }
        
        
        public double getRangeTop()
        {
	        return m_MaxVal;
        }


        public double getX(int index)
        {
			UsageRecord rec = m_UsageRecords.get(index);
			Date time = rec.getTime();
			long msVal = time.getTime();
			
	        return (double)msVal;
        }


        public double getX(int index, int level)
        {
	        return getX(index);
        }


        public double getY(int index)
        {
	        return m_UsageRecords.get(index).getValue();
        }


        public double getY(int index, int level)
        {
	       return getY(index);
        }
		
	}

}
