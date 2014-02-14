package demo.app.client;

import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.VerticalAlignment;
import com.extjs.gxt.ui.client.data.*;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.HorizontalPanel;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.LabelField;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData; 
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.GChart.Curve.Point;

import demo.app.data.*;


/**
 * An extension of the Ext JS GXT Window for a Prelert Desktop Server Usage Window.
 * The Window contains a usage chart and controls for configuring the usage data
 * to be displayed e.g. source, user, metric and date.
 * @author Pete Harverson
 */
public class ServerUsageWindow extends ViewWindow
{
	private DesktopApp				m_Desktop;
	private UsageChart				m_UsageChart;
	
	private DatePagingLoader<DatePagingLoadConfig, DatePagingLoadResult> m_Loader;
	private TimeFrame				m_TimeFrame;
	
	private ComboBox<BaseModelData> m_SourceComboBox;
	private ComboBox<BaseModelData> m_ServerComboBox;
	private ComboBox<BaseModelData> m_MetricComboBox;
	private SelectionChangedListener<BaseModelData>	m_ServerComboListener;
		
	
	/**
	 * Constructs an empty user usage window.
	 * @param desktop the parent Prelert desktop.
	 */
	public ServerUsageWindow(DesktopApp desktop)
	{
		m_Desktop = desktop;
		
		setCloseAction(CloseAction.HIDE);
		setIconStyle("line-graph-win-icon");
		setMinimizable(true);
		setMaximizable(true);
		setHeading("Server Usage");
		setSize(750, 520);
		setResizable(true);
		
		// Default TimeFrame is WEEK.
		m_TimeFrame = TimeFrame.WEEK;
		
		initComponents();
	}
	
	
	/**
	 * Creates and initialises the graphical components in the window.
	 */
	protected void initComponents()
	{
		setLayout(new BorderLayout());
		
	
		// Create the top panel which will hold the controls for setting the
		// parameters of the chart e.g. Server, Metric, time frame.
		HorizontalPanel toolPanel = new HorizontalPanel();
		toolPanel.addStyleName("x-panel-tc");
		toolPanel.setTableHeight("30px");
		toolPanel.setVerticalAlign(VerticalAlignment.MIDDLE);
		toolPanel.setSpacing(5);
		setTopComponent(toolPanel);		
		
		// Create a Server Name drop-down.
		// Populate with the list of available sources, plus an 'All sources'.
		toolPanel.add(new LabelField("Source: "));
		m_SourceComboBox = new ComboBox<BaseModelData>();
		m_SourceComboBox.setEditable(false);
		m_SourceComboBox.setEmptyText("Select server...");
		m_SourceComboBox.setDisplayField("source");
		m_SourceComboBox.setTypeAhead(true);
		m_SourceComboBox.setStore(new ListStore<BaseModelData>());
		populateSources();
		m_SourceComboBox.addSelectionChangedListener(new SelectionChangedListener<BaseModelData>() {
		      public void selectionChanged(SelectionChangedEvent<BaseModelData> se) 
		      {
		    	  load();
		    	  
		    	  BaseModelData selectedSource = se.getSelectedItem();
		    	  if (m_SourceComboBox.getStore().indexOf(selectedSource) == 0)
		    	  {
		    		  clearServers();
		    	  }
		    	  else
		    	  {
		    		  String source = selectedSource.get("source");
		    		  populateUsers(source);
		    	  }
		      }
		});

		
		// Put the ComboBox inside a panel to get it to vertically align properly.
		HorizontalPanel sourceComboBoxPanel = new HorizontalPanel();
		sourceComboBoxPanel.add(m_SourceComboBox);
		toolPanel.add(sourceComboBoxPanel);
		
		
		// Create a Server drop-down.
		toolPanel.add(new LabelField("Server: "));
		m_ServerComboBox = new ComboBox<BaseModelData>();
		m_ServerComboBox.setStore(new ListStore<BaseModelData>());
		m_ServerComboBox.setEmptyText("Select user...");
		m_ServerComboBox.setEditable(false);
		m_ServerComboBox.setDisplayField("server");
		m_ServerComboBox.setTypeAhead(true);
		m_ServerComboListener = new SelectionChangedListener<BaseModelData>() {
		      public void selectionChanged(SelectionChangedEvent<BaseModelData> se) 
		      {
		    	  load();
		      }
		};
		m_ServerComboBox.addSelectionChangedListener(m_ServerComboListener);
		
		// Put the ComboBox inside a panel to get it to vertically align properly.
		HorizontalPanel serverComboBoxPanel = new HorizontalPanel();
		serverComboBoxPanel.add(m_ServerComboBox);
		toolPanel.add(serverComboBoxPanel);
		
		
		// Create a Metric-type (e.g. Total, Pending) drop-down.
		toolPanel.add(new LabelField("Show: "));
		m_MetricComboBox = new ComboBox<BaseModelData>();
		m_MetricComboBox.setStore(getMetricList());
		m_MetricComboBox.setEmptyText("Select metric...");
		m_MetricComboBox.setEditable(false);
		m_MetricComboBox.setDisplayField("metric");
		m_MetricComboBox.setTypeAhead(true);
		
		// Put the ComboBox inside a panel to get it to vertically align properly.
		HorizontalPanel metricComboBoxPanel = new HorizontalPanel();
		metricComboBoxPanel.add(m_MetricComboBox);
		toolPanel.add(metricComboBoxPanel);
		
		m_MetricComboBox.setValue(m_MetricComboBox.getStore().getAt(0)); 
		m_MetricComboBox.addSelectionChangedListener(new SelectionChangedListener<BaseModelData>() {
		      public void selectionChanged(SelectionChangedEvent<BaseModelData> se) 
		      {
		    	  load();
		      }
		});
		

		
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
	    
	    infoMenuItem.addSelectionListener(new SelectionListener<ComponentEvent>() {
	    	@Override
	    	public void componentSelected(ComponentEvent ce) 
	    	{
	    		MessageBox.alert("Usage Chart", "Context menu event", null);
	    	}
	    });
	    
	    menu.add(infoMenuItem);
	    m_UsageChart.setContextMenu(menu);

	    // Listen for double click events for loading the drill-down view.
		m_UsageChart.addListener(Event.ONDBLCLICK, new Listener<BaseEvent>(){

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
		metrics1.set("metric", "active");
		metrics.add(metrics1);
		
		BaseModelData metrics2 = new BaseModelData();
		metrics2.set("metric", "pending");
		metrics.add(metrics2);
		
		BaseModelData metrics3 = new BaseModelData();
		metrics3.set("metric", "pendingOut");
		metrics.add(metrics3);
		
		BaseModelData metrics4 = new BaseModelData();
		metrics4.set("metric", "serverload");
		metrics.add(metrics4);
		
		return metrics;
	}
	
	
	/**
	 * Creates the date paging toolbar for navigating through the week.
	 */
	protected void createPagingToolBar()
	{
		DataProxy<DatePagingLoadConfig, DatePagingLoadResult> proxy = 
			new GetServerUsageDataRpcProxy(m_Desktop.getServerUsageQueryServiceInstance());
		
		DatePagingToolBar toolBar = new DatePagingToolBar(TimeFrame.WEEK);
		m_Loader = new DatePagingLoader<DatePagingLoadConfig, DatePagingLoadResult>(proxy);
		m_Loader.setTimeFrame(TimeFrame.WEEK);
		m_Loader.addLoadListener(new LoadListener(){
		      public void loaderLoad(LoadEvent le) 
		      {
		          onLoad(le);
		      }
		});
		toolBar.bind(m_Loader);
		setBottomComponent(toolBar);
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
	 * Updates the usage chart when new data is loaded into the window.
	 * @param le LoadEvent received from the load operation.
	 */
	protected void onLoad(LoadEvent<DatePagingLoadConfig, DatePagingLoadResult<UsageRecord>> le) 
	{
		DatePagingLoadConfig pageConfig = le.config;
		DatePagingLoadResult<UsageRecord> loadResult = le.data;
		List<UsageRecord> records = loadResult.getData();
		
		m_TimeFrame = loadResult.getTimeFrame();
		m_Loader.setTimeFrame(loadResult.getTimeFrame());
		m_Loader.setDate(loadResult.getDate());
		
		m_UsageChart.setUsageRecords(getSource(), getServer(), records);
		m_UsageChart.setXAxisRange(loadResult.getTimeFrame(), loadResult.getDate());    	
    	m_UsageChart.update();	
	}
	
	
	/**
	 * Loads usage data into the window.
	 */
	public void load()
	{
		m_Loader.load();
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
				m_Loader.load(usageDate, TimeFrame.DAY);
				setTimeFrame(TimeFrame.DAY);
				break;
				
			case DAY:
				m_Loader.load(usageDate, TimeFrame.HOUR);
				setTimeFrame(TimeFrame.HOUR);
				break;
				
			case HOUR:
				// Will need to load up Evidence View.
				break;
			
		}
	}
	
	
	/**
	 * Returns the currently selected source (server).
	 * @return the name of the source whose usage data is currently being viewed,
	 * or <code>null</code> if no specific source is currently selected
	 * i.e. 'All Sources' is selected.
	 */
	public String getSource()
	{		
		String sourceName = null;
		
		List<BaseModelData> selectedSources = m_SourceComboBox.getSelection();
		
		if (selectedSources.size() > 0)
		{
			BaseModelData sourceData = selectedSources.get(0);
			if (m_SourceComboBox.getStore().indexOf(sourceData) != 0)
	  	  	{
	  	  		sourceName = sourceData.get("source");
	  	  	}
		}
		
		return sourceName;
	}	
	
	
	/**
	 * Returns the currently selected username.
	 * @return the name of the user whose usage data is currently being viewed,
	 * or <code>null</code> if no specific user is currently selected
	 * i.e. 'All Users' is selected.
	 */
	public String getServer()
	{		
		String username = null;
		
		List<BaseModelData> selectedUsers = m_ServerComboBox.getSelection();
		
		if (selectedUsers.size() > 0)
		{
			BaseModelData userData = selectedUsers.get(0);
			if (m_ServerComboBox.getStore().indexOf(userData) != 0)
	  	  	{
				username = userData.get("username");
	  	  	}
		}
		
		return username;
	}	
	
	
	/**
	 * Returns the currently selected metric.
	 * @return the selected metric.
	 */
	public String getMetric()
	{		
		List<BaseModelData> selectedMetric = m_MetricComboBox.getSelection();
		BaseModelData metricData;
		
		if (selectedMetric.size() > 0)
		{
			metricData = selectedMetric.get(0);		
		}
		else
		{
			// Should always be a selection, but just in case, return the first metric.
			metricData = m_MetricComboBox.getStore().getAt(0);
		}
		
		return metricData.get("metric", "active");
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

		
		// Add an 'All sources' item at the top.
		BaseModelData allSourcesData = new BaseModelData();
		allSourcesData.set("source", "All Sources");
		sourcesStore.add(allSourcesData);
		
		m_Desktop.getServerUsageQueryServiceInstance().getSources(new AsyncCallback<List<String>>(){

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
	        }
        });
	}
	
	
	/**
	 * Clears the list of servers in the 'Server' drop-down, and adds a single
	 * 'All Servers' items into the drop-down combo box.
	 */
	public void clearServers()
	{
		// Clear out the Server ComboBox and repopulate with the servers for this source.
		m_ServerComboBox.clearSelections();
		ListStore<BaseModelData> usernamesStore = m_ServerComboBox.getStore();
		usernamesStore.removeAll();
		
		// Add an 'All Users' item at the top.
		BaseModelData allUsersData = new BaseModelData();
		allUsersData.set("server", "All Servers");
		usernamesStore.add(allUsersData);
		
		// Disable the SelectionChangedListener whilst setting the initial
		// value to ensure it does not trigger another query.
		m_ServerComboBox.removeSelectionListener(m_ServerComboListener);
		m_ServerComboBox.setValue(allUsersData);  
		m_ServerComboBox.addSelectionChangedListener(m_ServerComboListener);
	}
	
	
	/**
	 * Populates the 'Server' drop-down combo box with the list of servers for the
	 * supplied source.
	 * @param source source for which to obtain the servers.
	 */
	public void populateUsers(String source)
	{
		// Clear out the Server ComboBox and repopulate with the servers for this source.
		clearServers();
		final ListStore<BaseModelData> serversStore = m_ServerComboBox.getStore();

		m_Desktop.getServerUsageQueryServiceInstance().getUsers(
				source, new AsyncCallback<List<String>>(){

	        public void onFailure(Throwable caught)
	        {
		        MessageBox.alert("Prelert - Error",
		                "Failed to get a response from the server", null);
	        }


	        public void onSuccess(List<String> servers)
	        {
	        	BaseModelData serverData;
	        	
	        	for (String server : servers)
	        	{
	        		serverData = new BaseModelData();
	        		serverData.set("server", server);
	        		serversStore.add(serverData);
	        	}
	        }
        });
	}
	
	
	/**
	 * RpcProxy that retrieves user usage data from the Usage query service via
	 * GWT remote procedure calls.
	 */
	class GetServerUsageDataRpcProxy<C extends DatePagingLoadConfig, D extends DatePagingLoadResult>
		extends RpcProxy<DatePagingLoadConfig, DatePagingLoadResult<UsageRecord>>
	{
		private UsageQueryServiceAsync 	m_UsageQueryService;

		
		public GetServerUsageDataRpcProxy(UsageQueryServiceAsync queryService)
		{
			m_UsageQueryService = queryService;
		}
		
		
	    protected void load(DatePagingLoadConfig loadConfig,
	            AsyncCallback<DatePagingLoadResult<UsageRecord>> callback)
	    {
	    	String source = getSource();
	    	String metric = getMetric();
	    	GWT.log("load() - metric: " + metric, null);

	    	if (source != null)
	    	{
	    		String server = getServer();
	    		if (server != null)
	    		{
	    			m_UsageQueryService.getUsageData(metric, source, server, loadConfig, callback);
	    		}
	    		else
	    		{
	    			m_UsageQueryService.getUsageData(metric, source, loadConfig, callback);
	    		}
	    	}
	    	else
	    	{
	    		m_UsageQueryService.getUsageData(metric, loadConfig, callback);
	    	}
	    }
	
	}


}
