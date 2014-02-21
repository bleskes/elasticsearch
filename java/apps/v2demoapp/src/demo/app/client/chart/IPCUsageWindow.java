package demo.app.client.chart;

import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.VerticalAlignment;
import com.extjs.gxt.ui.client.data.*;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.HorizontalPanel;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.LabelField;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData; 
import com.extjs.gxt.ui.client.widget.layout.TableData;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.extjs.gxt.ui.client.widget.toolbar.FillToolItem;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.googlecode.gchart.client.GChart.Curve.Point;

import demo.app.client.DatePagingToolBar;
import demo.app.client.DesktopApp;
import demo.app.client.ViewWindow;
import demo.app.data.*;
import demo.app.service.DatabaseServiceLocator;
import demo.app.service.UsageQueryServiceAsync;


/**
 * An extension of the Ext JS GXT Window for a Prelert Desktop IPC Usage Window.
 * The Window contains a usage chart and controls for configuring the usage data
 * to be displayed e.g. source, metric and date.
 * @author Pete Harverson
 */
public class IPCUsageWindow extends ViewWindow
{
	private DesktopApp				m_Desktop;
	private UsageQueryServiceAsync	m_UsageQueryService;
	private UsageChart				m_UsageChart;
	
	private DatePagingLoader<DatePagingLoadResult> m_Loader;
	private DatePagingToolBar		m_ToolBar;
	private TimeFrame				m_TimeFrame;
	private String					m_Metric;
	private String					m_Source;
	
	private ComboBox<BaseModelData> m_SourceComboBox;
	private ComboBox<BaseModelData> m_MetricComboBox;
	private SelectionChangedListener<BaseModelData>	m_SourceComboListener;
	private SelectionChangedListener<BaseModelData>	m_MetricComboListener;
		
	
	/**
	 * Constructs an empty IPC usage window.
	 * @param desktop the parent Prelert desktop.
	 */
	public IPCUsageWindow(DesktopApp desktop)
	{
		m_Desktop = desktop;
		
		m_UsageQueryService = DatabaseServiceLocator.getInstance().getUsageQueryService("IPC");
		
		setIconStyle("line-graph-win-icon");
		setMinimizable(true);
		setMaximizable(true);
		setHeading("IPC Usage");
		setSize(750, 520);
		setResizable(true);
		
		// Default TimeFrame is WEEK.
		m_TimeFrame = TimeFrame.WEEK;
		
		initComponents();
		
		// Add a window listener to stop/start auto-refresh as the window
		// is hidden or shown.
		addWindowListener(new WindowListener(){
			
            public void windowShow(WindowEvent we)
            {
            	m_ToolBar.resumeAutoRefreshing();
            }


            public void windowHide(WindowEvent we)
            {
            	m_ToolBar.pauseAutoRefreshing();
            }

		});
	}
	
	
	/**
	 * Creates and initialises the graphical components in the window.
	 */
	protected void initComponents()
	{
		setLayout(new BorderLayout());
		
	
		// Create the top panel which will hold the controls for setting the
		// parameters of the chart e.g. Source, Metric, time frame.
		HorizontalPanel toolPanel = new HorizontalPanel();
		toolPanel.addStyleName("x-panel-tc");
		toolPanel.setTableHeight("30px");
		toolPanel.setTableWidth("100%");
		toolPanel.setVerticalAlign(VerticalAlignment.MIDDLE);
		setTopComponent(toolPanel);		
		
		// Create a Source Name drop-down.
		// Populate with the list of available sources, plus an 'All sources'.
		m_SourceComboBox = new ComboBox<BaseModelData>();
		m_SourceComboBox.setEditable(false);
		m_SourceComboBox.setEmptyText("Select server...");
		m_SourceComboBox.setDisplayField("source");
		m_SourceComboBox.setTypeAhead(true);
		m_SourceComboBox.setTriggerAction(TriggerAction.ALL);
		m_SourceComboBox.setStore(new ListStore<BaseModelData>());
		populateSources();
		m_SourceComboListener = new SelectionChangedListener<BaseModelData>() {
		      public void selectionChanged(SelectionChangedEvent<BaseModelData> se) 
		      {  
		    	  BaseModelData selectedSource = se.getSelectedItem();
		    	  
		    	  if (m_SourceComboBox.getStore().indexOf(selectedSource) == 0)
		    	  {
		    		  setSource(null);
		    	  }
		    	  else
		    	  {
		    		  String source = selectedSource.get("source");
		    		  setSource(source); 
		    	  }
		      }
		};
		m_SourceComboBox.addSelectionChangedListener(m_SourceComboListener);

		
		// Put the ComboBox inside a panel to get it to vertically align properly.
		HorizontalPanel sourceComboBoxPanel = new HorizontalPanel();
		sourceComboBoxPanel.setSpacing(5);
		sourceComboBoxPanel.add(new LabelField("Source: "));
		sourceComboBoxPanel.add(m_SourceComboBox);
		toolPanel.add(sourceComboBoxPanel);
		
		
		// Create a Metric-type (e.g. Total, Pending) drop-down.
		m_MetricComboBox = new ComboBox<BaseModelData>();
		m_MetricComboBox.setStore(getMetricList());
		m_Metric = getMetricList().getAt(0).get("metric");
		m_MetricComboBox.setEmptyText("Select metric...");
		m_MetricComboBox.setEditable(false);
		m_MetricComboBox.setDisplayField("metric");
		m_MetricComboBox.setTypeAhead(true);
		m_MetricComboBox.setTriggerAction(TriggerAction.ALL);
		
		// Put the Metric ComboBox inside a panel to align it on the right.
		HorizontalPanel metricComboBoxPanel = new HorizontalPanel();
		metricComboBoxPanel.setSpacing(5);
		metricComboBoxPanel.add(new LabelField("Show: "));
		metricComboBoxPanel.add(m_MetricComboBox);
		TableData td = new TableData();
		td.setHorizontalAlign(HorizontalAlignment.RIGHT);
		toolPanel.add(metricComboBoxPanel, td);
		
		m_MetricComboListener = new SelectionChangedListener<BaseModelData>() {
		      public void selectionChanged(SelectionChangedEvent<BaseModelData> se) 
		      {
		    	  BaseModelData selectedMetric = se.getSelectedItem();
		    	  String metric = selectedMetric.get("metric");
		    	  setMetric(metric);
		      }
		};
		m_MetricComboBox.addSelectionChangedListener(m_MetricComboListener);
		

		
		// Add the chart to the centre of the window.
		BorderLayoutData centerLayoutData = new BorderLayoutData(LayoutRegion.CENTER);
		centerLayoutData.setMargins(new Margins(0, 0, 0, 0));
		
		ContentPanel center = new ContentPanel();
		center.setHeaderVisible(false);
		
		// Create the Usage chart itself, and listen for click events to
		// open the drill-down view.
		m_UsageChart = new UsageChart();
		
		// Test out adding in a right-click context menu.
		Menu menu = new Menu();
	    MenuItem infoMenuItem = new MenuItem("Show evidence");
	    
	    infoMenuItem.addSelectionListener(new SelectionListener<MenuEvent>() {
	    	@Override
	    	public void componentSelected(MenuEvent e) 
	    	{
	    		MessageBox.alert("Usage Chart", "Context menu event", null);
	    	}
	    });
	    
	    menu.add(infoMenuItem);
	    m_UsageChart.setContextMenu(menu);

	    // Listen for double click events for loading the drill-down view.
		m_UsageChart.addListener(Events.OnDoubleClick, new Listener<BaseEvent>(){

            public void handleEvent(BaseEvent be)
            {
            	// Test out clicking on a point to open an Evidence Window.
        		Point touchedPoint = m_UsageChart.getTouchedPoint();
        		if (touchedPoint != null)
        		{	
        			double xCoord = touchedPoint.getX();
        			final Date usageDate = new Date((long)xCoord);
        			
        			// Run in a DeferredCommand to ensure all Event Handlers have completed
        			// so that the Evidence Window can come to the front.
        			DeferredCommand.addCommand(new Command()
                    {
                        public void execute()
                        {
                        	loadDrillDownView(usageDate);
                        }
                    });

        		}
            }
			
		});
		
		center.add(m_UsageChart);
		
		add(center, centerLayoutData); 
		
		createPagingToolBar();
	}
	
	
	/**
	 * Returns the list of metric names.
	 * @return metrics for server usage data.
	 */
	protected ListStore<BaseModelData> getMetricList()
	{
		// Just hardcode the list of metrics for server usage.
		ListStore<BaseModelData> metrics = new ListStore<BaseModelData>();
		
		BaseModelData metrics1 = new BaseModelData();
		metrics1.set("metric", "inbound");
		metrics.add(metrics1);
		
		BaseModelData metrics2 = new BaseModelData();
		metrics2.set("metric", "outbound");
		metrics.add(metrics2);
		
		return metrics;
	}
	
	
	/**
	 * Creates the date paging toolbar for navigating through the week.
	 */
	protected void createPagingToolBar()
	{
		DataProxy<DatePagingLoadResult> proxy = 
			new GetIPCUsageDataRpcProxy(m_UsageQueryService);
		
		m_ToolBar = new DatePagingToolBar(TimeFrame.WEEK);
		m_Loader = new DatePagingLoader<DatePagingLoadResult>(proxy);
		m_Loader.setTimeFrame(TimeFrame.WEEK);
		m_Loader.addLoadListener(new LoadListener(){
		      public void loaderLoad(LoadEvent le) 
		      {
		          onLoad(le);
		      }
		});
		m_ToolBar.bind(m_Loader);
		setBottomComponent(m_ToolBar);
	}
	
	
	/**
	 * Returns the chart being displayed in the window.
	 * @return the chart being displayed in the window.
	 */
	public UsageChart getChart()
	{
		return m_UsageChart;
	}
	
	
	/**
	 * Returns the time frame currently being displayed in the window.
	 * @return time frame of the current chart e.g. week, day or hour.
	 */
	public TimeFrame getTimeFrame()
    {
    	return m_TimeFrame;
    }


	/**
	 * Sets the time frame currently being displayed in the window.
	 * @param timeFrame time frame of the current chart e.g. week, day or hour.
	 */
	public void setTimeFrame(TimeFrame timeFrame)
    {
    	m_TimeFrame = timeFrame;
    }
	
	
	/**
	 * Convenience method to set all usage window properties in one go, before 
	 * re-loading the usage data in the window.
	 * @param metric the metric to display.
	 * @param source name of the source (server) for which to display the usage.
	 * 		If <code>null</code> the total service usage across all sources and 
	 * 		servers will be displayed.
	 * @param date date/time of usage to display.
	 * @param timeFrame time frame of usage to display i.e. WEEK, DAY or HOUR.
	 */
	public void setUsageProperties(String metric, String source, 
			Date date, TimeFrame timeFrame)
	{
		// Validate metric value passed in.
		if (metric.equals(m_Metric) == false)
		{
			ListStore<BaseModelData> metricsStore = m_MetricComboBox.getStore();
			
			BaseModelData metricData;
			for (int i = 0; i < metricsStore.getCount(); i++)
			{
				metricData = metricsStore.getAt(i);
				if (metricData.get("metric").equals(metric))
				{
					m_Metric = metric;
					break;
				}
			}
		}
		
		m_Source = source;
		m_TimeFrame = timeFrame;
		
		// Load for the supplied date and time frame.
		m_ToolBar.setAutoRefreshing(false);
		m_Loader.load(date, timeFrame);
	}
	
	
	/**
	 * Returns the currently selected source (server).
	 * @return the name of the source whose usage data is currently being viewed,
	 * or <code>null</code> if no specific source is currently selected
	 * i.e. 'All Sources' is selected.
	 */
	public String getSource()
	{	
		return m_Source;
	}
	
	
	/**
	 * Sets the name of the source (server), and reloads the usage data in the
	 * window.
	 * @param source the name of the source (server) whose usage data is to be
	 * displayed. If <code>null</code>, data from all sources will be displayed.
	 */
	public void setSource(String source)
	{
		m_Source = source;
		load();
	}
	
	
	/**
	 * Returns the currently selected metric.
	 * @return the selected metric.
	 */
	public String getMetric()
	{		
		return m_Metric;
	}
	
	
	/**
	 * Sets the name of the metric, and reloads the usage data in the window.
	 * @param metric the name of the usage metric to display.
	 */
	public void setMetric(String metric)
	{
		if (metric.equals(m_Metric) == false)
		{
			ListStore<BaseModelData> metricsStore = m_MetricComboBox.getStore();
			
			BaseModelData metricData;
			for (int i = 0; i < metricsStore.getCount(); i++)
			{
				metricData = metricsStore.getAt(i);
				if (metricData.get("metric").equals(metric))
				{
					m_Metric = metric;
					load();
					break;
				}
			}
		}
	}
	
	
	/**
	 * Adds a 'timeline' to the window's usage chart to indicate a specific time.
	 * @param date date for the time line.
	 */
	public void addTimeLine(Date date)
	{
		m_UsageChart.addTimeLine(date);
	}
	
	
	/**
	 * Removes the timeline from the window's usage chart if one has been added.
	 */
	public void clearTimeLine()
	{
		m_UsageChart.clearTimeLine();
	}
	
	
	/**
	 * Updates the usage chart when new data is loaded into the window.
	 * @param le LoadEvent received from the load operation.
	 */
	protected void onLoad(LoadEvent le) 
	{	
		DatePagingLoadConfig pageConfig = le.getConfig();
		DatePagingLoadResult<UsageRecord> loadResult = le.getData();
		List<UsageRecord> records = loadResult.getData();
		
		m_TimeFrame = loadResult.getTimeFrame();
		m_Loader.setTimeFrame(loadResult.getTimeFrame());
		m_Loader.setDate(loadResult.getDate());
		
		setMetricComboSelection(m_Metric);
		setSourceComboSelection(m_Source);	
		
		// Build the title of the chart.
		String source = getSource();
		String chartTitle = "IPC Usage";
		chartTitle += " - ";
		if (source != null)
		{
			chartTitle += source;
		}
		else
		{
			chartTitle += "All Sources";
		}
		
		m_UsageChart.setUsageRecords(records);
		m_UsageChart.setTitleText(chartTitle);	
		m_UsageChart.setXAxisRange(loadResult.getTimeFrame(), loadResult.getDate());   
    	
		m_UsageChart.update();	  	
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
	
	
	/**
	 * Loads usage data into the window.
	 */
	public void load()
	{
		m_Loader.load();
	}
	
	
	/**
	 * Loads usage data into the window for the given time.
	 * @param date date/time of usage data to load.
	 */
    public void loadAtTime(Date date)
    {
    	m_ToolBar.setAutoRefreshing(false);
    	m_Loader.load(date, m_TimeFrame);
    }
	
	
	/**
	 * Drills down the view in the window for the specified usage time.
	 * For example, if the window is currently showing usage data for a weekly
	 * time frame, a chart showing usage over a day will be loaded.
	 * @param usageDate time of usage record
	 */
	public void loadDrillDownView(Date usageDate)
	{  	  	
		switch (m_TimeFrame)
		{
			case WEEK:
				setTimeFrame(TimeFrame.DAY);
				loadAtTime(usageDate);
				break;
				
			case DAY:
				setTimeFrame(TimeFrame.HOUR);
				loadAtTime(usageDate);
				break;
				
			case HOUR:
				// Will need to load up Evidence View.
				break;
			
		}
	}
	
	
	/**
	 * Populates the source ComboBox by querying the database for the list of
	 * of available sources (servers) of usage data.
	 */
	public void populateSources()
	{
		// Clear out the Source ComboBox and repopulate.
		m_SourceComboBox.clearSelections();
		final ListStore<BaseModelData> sourcesStore = m_SourceComboBox.getStore();
		sourcesStore.removeAll();
		m_Source = null;
		
		// Add an 'All sources' item at the top.
		BaseModelData allSourcesData = new BaseModelData();
		allSourcesData.set("source", "All Sources");
		sourcesStore.add(allSourcesData);
		
		m_UsageQueryService.getSources(new AsyncCallback<List<String>>(){

	        public void onFailure(Throwable caught)
	        {
		        MessageBox.alert("Prelert - Error",
		                "Failed to get a response from the server", null);
	        }


	        public void onSuccess(List<String> sources)
	        {
	        	BaseModelData sourceData;
	        	
	        	for (String source : sources)
	        	{
	        		sourceData = new BaseModelData();
	        		sourceData.set("source", source);
	        		sourcesStore.add(sourceData);
	        	}
	        	
	        	if (m_Source != null)
	        	{
	        		// Set the Source ComboBox in case when the window is first
	        		// loaded this call completes AFTER the data is loaded.
	        		setSourceComboSelection(m_Source);
	        	}
	        }
        });
	}
	
	
	/**
	 * Sets the Metric Combo Box to the selected value. Note that this simply
	 * updates the Combo Box field, and does not update the chart.
	 * @param metric value to set.
	 */
	protected void setMetricComboSelection(String metric)
	{
		ListStore<BaseModelData> metricsStore = m_MetricComboBox.getStore();
		if (metricsStore.getCount() > 0)
		{
			m_MetricComboBox.disableEvents(true);
			
			List<BaseModelData> selectedMetrics = m_MetricComboBox.getSelection();
			String currentlySelectedMetric = null;
			if (selectedMetrics.size() > 0)
			{
				BaseModelData selectedMetricData = selectedMetrics.get(0);
				if (m_MetricComboBox.getStore().indexOf(selectedMetricData) != 0)
		  	  	{
					currentlySelectedMetric = selectedMetricData.get("metric");
		  	  	}
			}
			
			if (metric.equals(currentlySelectedMetric) == false)
			{
				BaseModelData metricData;
				for (int i = 0; i < metricsStore.getCount(); i++)
				{
					metricData = metricsStore.getAt(i);
					if (metricData.get("metric").equals(metric))
					{
						m_MetricComboBox.setValue(metricData);
						break;
					}
				}
			}
			
			m_MetricComboBox.enableEvents(true);
		}
	}
	
	
	/**
	 * Sets the Source Combo Box to the selected value. Note that this simply
	 * updates the Combo Box field, and does not update the chart.
	 * @param source value to set.
	 */
	protected void setSourceComboSelection(String source)
	{
		ListStore<BaseModelData> sourcesStore = m_SourceComboBox.getStore();
		if (sourcesStore.getCount() > 0)
		{
			m_SourceComboBox.disableEvents(true);
			
			if (source == null)
			{
				m_SourceComboBox.setValue(sourcesStore.getAt(0));
			}
			else
			{
				List<BaseModelData> selectedSources = m_SourceComboBox.getSelection();
				String currentlySelectedSource = null;
				if (selectedSources.size() > 0)
				{
					BaseModelData selectedSourceData = selectedSources.get(0);
					if (m_SourceComboBox.getStore().indexOf(selectedSourceData) != 0)
			  	  	{
						currentlySelectedSource = selectedSourceData.get("source");
			  	  	}
				}
				
				if (source.equals(currentlySelectedSource) == false)
				{
					BaseModelData sourceData;
					for (int i = 0; i < sourcesStore.getCount(); i++)
					{
						sourceData = sourcesStore.getAt(i);
						if (sourceData.get("source").equals(source))
						{
							m_SourceComboBox.setValue(sourceData);
							break;
						}
					}
				}
			}
			
			m_SourceComboBox.enableEvents(true);
		}
	}
	
	
	/**
	 * RpcProxy that retrieves user usage data from the Usage query service via
	 * GWT remote procedure calls.
	 */
	class GetIPCUsageDataRpcProxy<D extends DatePagingLoadResult>
		extends RpcProxy<DatePagingLoadResult<UsageRecord>>
	{
		private UsageQueryServiceAsync 	m_UsageQueryService;

		
		public GetIPCUsageDataRpcProxy(UsageQueryServiceAsync queryService)
		{
			m_UsageQueryService = queryService;
		}
		
		
	    protected void load(Object loadConfig,
	            AsyncCallback<DatePagingLoadResult<UsageRecord>> callback)
	    {
	    	String source = getSource();
	    	String metric = getMetric();
    		m_UsageQueryService.getUsageData(metric, source, 
    				(DatePagingLoadConfig) loadConfig, callback);
	    }
	
	}


}
