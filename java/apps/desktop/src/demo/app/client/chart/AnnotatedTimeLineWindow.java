package demo.app.client.chart;

import java.util.ArrayList;
import java.util.List;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.util.DateWrapper;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.events.Handler;
import com.google.gwt.visualization.client.events.ReadyHandler;
import com.google.gwt.visualization.client.events.ReadyHandler.ReadyEvent;
import com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine;
import com.google.gwt.visualization.client.visualizations.Table;
import com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine.AnnotatedLegendPosition;
import com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine.ScaleType;
import com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine.WindowMode;

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

public class AnnotatedTimeLineWindow extends ViewWindow
{
	private UsageQueryServiceAsync	m_UsageQueryService;
	private List<TimeSeriesConfig>	m_DataSeries;
	private List<String>			m_LineColours;
	
	private AnnotatedTimeLine		m_TimeLineChart;
	private DataTable				m_DataTable;
	
	private DateWrapper				m_TestStartTime; 
	private int						m_DataCounter = 0;
	private boolean					m_IsLoaded;
	
	public AnnotatedTimeLineWindow()
	{		
		m_UsageQueryService = DatabaseServiceLocator.getInstance().getUsageQueryService("Services");
		m_DataSeries = new ArrayList<TimeSeriesConfig>();
		m_LineColours = new ArrayList<String>();		
		setIconStyle("line-graph-win-icon");
		setMinimizable(true);
		setMaximizable(true);
		setHeading("Annotated Timeline");
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
	    addDataSeries("active", null, null, "#0000FF");
	    addDataSeries("active", "sol30m-8201.1.p2ps", null, "#63B8FF");
	    addDataSeries("active", "sol30m-8202.1.p2ps", null, "#FFFF00");
	    addDataSeries("active", "sol30m-8203.1.p2ps", null, "#FF00FF");
	    addDataSeries("serverload", null, null, "#FF0000");
	    addDataSeries("serverload", "sol30m-8201.1.p2ps", null, "#00FF00");
	    addDataSeries("serverload", "sol30m-8202.1.p2ps", null, "#B429FF");
	    addDataSeries("serverload", "sol30m-8203.1.p2ps", null, "#B4FF29");
	    addDataSeries("serverload", "sol30m-8203.1.p2ps", "IDN_SELECTFEED", "#CCCC00");
//	    addDataSeries("serverload", "sol30m-8203.1.p2ps", "TEST_PRISM_TURQUOISE", "#67BACA"); // 70 points
	    
	    
		// Create the AnnotatedTimeLine widget.
	    Runnable googleVizCallback = new Runnable() 
	    {      
	    	public void run() 
	    	{    
	    		m_TimeLineChart = createTimeLine();
	    		add(m_TimeLineChart);
	    	}    
	    };
		
    	// Load the visualization api, passing the onLoadCallback to be called    
    	// when loading is done.    
	    VisualizationUtils.loadVisualizationApi(googleVizCallback, 
	    		Table.PACKAGE, 
	    		AnnotatedTimeLine.PACKAGE);
	    
	}
	
	
	/**
	 * Creates the annotated time line.
	 * @return the AnnotatedTimeLine widget.
	 */
	protected AnnotatedTimeLine createTimeLine()
	{
	    com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine.Options options = 
	    	com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine.Options.create();
	    options.setDisplayAnnotations(true);
	    options.setAnnotationsWidth(15);
	    options.setWindowMode(WindowMode.TRANSPARENT);
	    
	    // Maximize y-axis scale depending on max value.
	    options.setScaleType(ScaleType.MAXIMIZE);

	    // Create the data table.
	    //m_DataTable = DataTable.create();
	    //m_DataTable.addColumn(ColumnType.DATETIME, "Time");
	    //m_DataTable.addColumn(ColumnType.DATETIME, "Value");
		
	    //AnnotatedTimeLine timeLine = new AnnotatedTimeLine(m_DataTable, options, "730px", "480px");
	    AnnotatedTimeLine timeLine = new AnnotatedTimeLine("730px", "480px");
	    
	    Handler.addHandler(timeLine, "ready", new ReadyHandler(){

            public void onReady(ReadyEvent event)
            {
            	DateWrapper currentTime = new DateWrapper();
				long timeTaken = currentTime.getTime() - m_TestStartTime.getTime();
				GWT.log("AnnotatedTimeLineWindow load completed in " + 
						timeTaken + "ms at " + currentTime, null);
				
				MessageBox.confirm("ScatterGChartWindow", "Time since load start: " + 
						timeTaken + "ms", null);
            }
	    	
	    });
	    
	    return timeLine;
	}

	
	public void addDataSeries(String metric, String source, String service, String lineColour)
	{
		TimeSeriesConfig dataSeries = new TimeSeriesConfig(metric, source, service); 
		m_DataSeries.add(dataSeries);
		
		m_LineColours.add(lineColour);
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
			
			GWT.log("AnnotatedTimeLineWindow load started at " + m_TestStartTime, null);
			
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
            	if (m_DataTable == null)
            	{
            	    m_DataTable = DataTable.create();
            	    m_DataTable.addColumn(ColumnType.DATETIME, "Time");
            	}
            	
            	
            	int valueIdx = m_DataTable.getNumberOfColumns();
            	int titleIdx = (valueIdx + 1);
        	    m_DataTable.addColumn(ColumnType.NUMBER, config.getMetric());
        	    m_DataTable.addColumn(ColumnType.STRING, "title1");            	
            	
            	List<UsageRecord> records = result.getData();
	        	
	        	m_DataTable.setColumnLabel(1, config.getMetric());
	        	
	        	if (m_DataTable.getNumberOfRows() == 0)
	        	{
	        		//m_DataTable.removeColumn(1);
	        		m_DataTable.addRows(records.size());
	        	}
	        	
	        	int i = 0;
	        	for (UsageRecord record : records)
	        	{
	        		m_DataTable.setValue(i, 0, record.getTime());
	        		m_DataTable.setValue(i, valueIdx, record.getValue());
	        		
	        		i++;
	        	}
	        	
	        	// Need to show at least 1 annotation otherwise width of chart increases!
	        	m_DataTable.setValue(0, titleIdx, "Start time");
	        	
	        	
	        	// Only update the chart once all results have been received.
	    		m_DataCounter++;  
	    	    if (m_DataCounter == m_DataSeries.size())
	    	    {
	    	    	com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine.Options options = 
	    		    	com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine.Options.create();
	    		    options.setWindowMode(WindowMode.OPAQUE);
	    		    String[] colours = (String[]) m_LineColours.toArray();
	    		    options.setColors(colours);
	    		    options.setLegendPosition(AnnotatedLegendPosition.NEW_ROW);
	    		    
	    		    // Maximize y-axis scale depending on max value.
	    		    options.setScaleType(ScaleType.MAXIMIZE);
	    	    	
	    	    	m_TimeLineChart.draw(m_DataTable, options);
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
    
    
	/**
	 * Runs a tool against the selected row of evidence. No action is taken if
	 * no row is currently selected.
	 * @param tool the tool that has been run by the user.
	 */
	public void runTool(Tool tool)
	{
		// TO DO.
	}
	
	
}
