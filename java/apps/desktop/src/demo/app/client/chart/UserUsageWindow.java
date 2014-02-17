package demo.app.client.chart;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.VerticalAlignment;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.data.BasePagingLoader;
import com.extjs.gxt.ui.client.data.DataProxy;
import com.extjs.gxt.ui.client.data.ListLoadConfig;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.data.PagingLoadConfig;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.*;
import com.extjs.gxt.ui.client.widget.form.*;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.layout.*;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.googlecode.gchart.client.GChart.Curve.Point;

import demo.app.client.DatePagingToolBar;
import demo.app.client.DesktopApp;
import demo.app.client.ViewWindow;
import demo.app.data.*;
import demo.app.service.DatabaseServiceLocator;
import demo.app.service.GetUserUsageDataRpcProxy;
import demo.app.service.UsageQueryServiceAsync;


/**
 * An extension of the Ext JS GXT Window for a Prelert Desktop Chart View Window.
 * The Window contains a GChart component for displaying server monitoring data
 * versus time.
 * @author Pete Harverson
 */
@SuppressWarnings("deprecation")
public class UserUsageWindow extends ViewWindow
{
	private DesktopApp				m_Desktop;
	private UsageQueryServiceAsync	m_UsageQueryService;
	
	private UsageChart				m_UsageChart;
	
	private DatePagingToolBar		m_ToolBar;
	private DatePagingLoader<DatePagingLoadResult> m_Loader;
	private TimeFrame				m_TimeFrame;
	private String					m_Source;
	private String					m_Username;
	
	private ComboBox<BaseModelData> m_SourceComboBox;
	private ComboBox<BaseModelData> m_UserComboBox;
	private SelectionChangedListener<BaseModelData>	m_SourceComboListener;
	//private Listener<FieldEvent>	m_SourceComboListener;
	private SelectionChangedListener<BaseModelData>	m_UserComboListener;

	
	public UserUsageWindow(DesktopApp desktop)
	{
		m_Desktop = desktop;
		
		m_UsageQueryService = DatabaseServiceLocator.getInstance().getUsageQueryService("Users");
		
		setIconStyle("line-graph-win-icon");
		setMinimizable(true);
		setMaximizable(true);
		setHeading("User Usage");
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
		// parameters of the chart e.g. Server, Metric, time frame.
		HorizontalPanel toolPanel = new HorizontalPanel();
		toolPanel.addStyleName("x-panel-tc");
		toolPanel.setTableHeight("30px");
		toolPanel.setTableWidth("100%");
		toolPanel.setVerticalAlign(VerticalAlignment.MIDDLE);
		setTopComponent(toolPanel);		
		
		// Create a Server Name drop-down.
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
		    	  GWT.log("Source selectionChanged()", null);
		    	  
		    	  m_Username = null;
		    	  
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
		HorizontalPanel toolComboBoxPanel = new HorizontalPanel();
		toolComboBoxPanel.setSpacing(5);
		toolComboBoxPanel.add(new LabelField("Source: "));
		toolComboBoxPanel.add(m_SourceComboBox);
		toolPanel.add(toolComboBoxPanel);
		
		
		// Create a User drop-down.
		m_UserComboBox = new ComboBox<BaseModelData>();
		m_UserComboBox.setEditable(false);
		m_UserComboBox.setEmptyText("Select user...");
		m_UserComboBox.setDisplayField("username");
		m_UserComboBox.setTypeAhead(true);
		m_UserComboBox.setTriggerAction(TriggerAction.ALL);
		m_UserComboBox.setStore(new ListStore<BaseModelData>());
		//populateUsers(m_Source);

		m_UserComboListener = new SelectionChangedListener<BaseModelData>() {
		      public void selectionChanged(SelectionChangedEvent<BaseModelData> se) 
		      {    	  
		    	  BaseModelData selectedUser = se.getSelectedItem();
		    	  
		    	  if (m_UserComboBox.getStore().indexOf(selectedUser) == 0)
		    	  {
		    		  setUsername(null);  		  
		    	  }
		    	  else
		    	  {
		    		  String username = selectedUser.get("username");
		    		  setUsername(username);
		    	  } 
		      }
		};
		m_UserComboBox.addSelectionChangedListener(m_UserComboListener);
		
		toolComboBoxPanel.add(new LabelField("User: "));
		toolComboBoxPanel.add(m_UserComboBox);
		
		
		// Create a Metric-type (e.g. Total, Pending) drop-down.
		ComboBox<BaseModelData> metricComboBox = new ComboBox<BaseModelData>();
		metricComboBox.setStore(getMetricList());
		metricComboBox.setEmptyText("Select metric...");
		metricComboBox.setEditable(false);
		metricComboBox.setDisplayField("metricName");
		metricComboBox.setTypeAhead(true);
		metricComboBox.setTriggerAction(TriggerAction.ALL);
		
		// Put the Metric ComboBox inside a panel to align it on the right.
		HorizontalPanel metricComboBoxPanel = new HorizontalPanel();
		metricComboBoxPanel.setSpacing(5);
		metricComboBoxPanel.add(new LabelField("Show: "));
		metricComboBoxPanel.add(metricComboBox);
		TableData td = new TableData();
		td.setHorizontalAlign(HorizontalAlignment.RIGHT);
		toolPanel.add(metricComboBoxPanel, td);

		
		// Add the chart to the centre of the window.
		BorderLayoutData centerLayoutData = new BorderLayoutData(LayoutRegion.CENTER);
		centerLayoutData.setMargins(new Margins(0, 0, 0, 0));
		
		ContentPanel center = new ContentPanel();
		center.setHeaderVisible(false);
		
		m_UsageChart = new UsageChart();
		center.add(m_UsageChart);
		
		
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
		
		//m_UsageChart.update();
		
		add(center, centerLayoutData); 
		
		createPagingToolBar();
	}
	
	
	protected void createPagingToolBar()
	{
		DataProxy<DatePagingLoadResult> proxy = 
			new GetUserUsageDataRpcProxy(this, m_UsageQueryService);
		
		m_ToolBar = new DatePagingToolBar(TimeFrame.WEEK);
		m_Loader = new DatePagingLoader<DatePagingLoadResult>(proxy);
		m_Loader.setTimeFrame(TimeFrame.WEEK);
		m_Loader.addLoadListener(new LoadListener(){
			
            public void loaderBeforeLoad(LoadEvent le)
            {
            	m_SourceComboBox.disableEvents(true);
            	m_UserComboBox.disableEvents(true);
            }

			public void loaderLoad(LoadEvent le) 
			{
				onLoad(le);
				
				m_SourceComboBox.enableEvents(true);
				m_UserComboBox.enableEvents(true);
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
	
	
	public TimeFrame getTimeFrame()
    {
    	return m_TimeFrame;
    }


	public void setTimeFrame(TimeFrame timeFrame)
    {
    	m_TimeFrame = timeFrame;
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
		
		// Re-populate the User drop-down for the Source.
		populateUsers(m_Source);
	}
	
	
	/**
	 * Returns the currently selected username.
	 * @return the name of the user whose usage data is currently being viewed,
	 * or <code>null</code> if no specific user is currently selected
	 * i.e. 'All Users' is selected.
	 */
	public String getUsername()
	{		
		return m_Username;
	}
	
	
	/**
	 * Sets the name of the user, and reloads the usage data in the window.
	 * @param username the name of the user whose usage data is to be
	 * displayed. If <code>null</code>, data from all users for the currently
	 * selected source will be displayed.
	 */
	public void setUsername(String username)
	{
		m_Username = username;
		load();
	}
	
	
	/**
	 * Convenience method to set all usage window properties in one go, before 
	 * re-loading the usage data in the window.
	 * @param source name of the source (server) for which to display the usage.
	 * 		If <code>null</code> the total user usage across all sources and 
	 * 		servers will be displayed.
	 * @param username name of the user for which to display the usage.
	 * 		If <code>null</code> the total user usage for the supplied source 
	 * 		will be displayed.
	 * @param date date/time of usage to display.
	 * @param timeFrame time frame of usage to display i.e. WEEK, DAY or HOUR.
	 */
	public void setUsageProperties(String source, String username, 
			Date date, TimeFrame timeFrame)
	{
		m_Source = source;
		m_Username = username;
		m_TimeFrame = timeFrame;
		
		// Load for the supplied date and time frame.
		m_ToolBar.setAutoRefreshing(false);
		m_Loader.load(date, timeFrame);
		
		// Re-populate the User drop-down for the Source.
		populateUsers(m_Source);
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
	
	
	protected void onLoad(LoadEvent le) 
	{	
		DatePagingLoadConfig pageConfig = le.getConfig();
		DatePagingLoadResult<UsageRecord> loadResult = le.getData();
		List<UsageRecord> records = loadResult.getData();
		
		m_TimeFrame = loadResult.getTimeFrame();
		m_Loader.setTimeFrame(loadResult.getTimeFrame());
		m_Loader.setDate(loadResult.getDate());
		
		setSourceComboSelection(m_Source);	
		setUserComboSelection(m_Username);
		
		// Build the title of the chart.
		String source = getSource();
		String user = getUsername();
		String chartTitle = "User Usage";
		chartTitle += " - ";
		if (source != null)
		{
			chartTitle += source;
		}
		else
		{
			chartTitle += "All Sources";
		}
		chartTitle += ", ";
		if (user != null)
		{
			chartTitle += user;
		}
		else
		{
			chartTitle += "All Usets";
		}
		
		
		m_UsageChart.setUsageRecords(records);
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
	        		//populateUsers(m_Source);
	        	}
	        	
	        	populateUsers(m_Source);
	        }
        });
	}
	
	
	public void clearUsers()
	{	
		// Clear out the User ComboBox and repopulate with the users for this source.
		m_UserComboBox.clearSelections();
		ListStore<BaseModelData> usernamesStore = m_UserComboBox.getStore();
		usernamesStore.removeAll();
		
		// Add an 'All Users' item at the top.
		BaseModelData allUsersData = new BaseModelData();
		allUsersData.set("username", "All Users");
		usernamesStore.add(allUsersData);
		
		// Disable the SelectionChangedListener whilst setting the initial
		// value to ensure it does not trigger another query.
		m_UserComboBox.disableEvents(true);
		m_UserComboBox.setValue(allUsersData);  
		m_UserComboBox.enableEvents(true);
	}
	
	
	/**
	 * Populates the 'User' drop-down combo box with the list of users for the
	 * supplied source (server).
	 * @param source source (server) for which to obtain the users.
	 */
	public void populateUsers(String source)
	{
		// Clear out the User ComboBox and repopulate with the users for this source.
		clearUsers();
		final ListStore<BaseModelData> usernamesStore = m_UserComboBox.getStore();
		
		m_UserComboBox.disableEvents(true);
		
		m_UsageQueryService.getUsers(
				source, new AsyncCallback<List<String>>(){

	        public void onFailure(Throwable caught)
	        {
	        	 m_UserComboBox.enableEvents(true);
	        	
		        MessageBox.alert("Prelert - Error",
		                "Failed to get a response from the server", null);
	        }


	        public void onSuccess(List<String> usernames)
	        {
	        	BaseModelData usernameData;
	        	
	        	for (String username : usernames)
	        	{
	        		usernameData = new BaseModelData();
	        		usernameData.set("username", username);
	        		usernamesStore.add(usernameData);
	        	}
	        	
	        	if (m_Username != null)
	        	{
	        		// Set the User ComboBox in case when the window is first
	        		// loaded this call completes AFTER the data is loaded.
	        		setUserComboSelection(m_Username);
	        	}
	        	
	        	m_UserComboBox.enableEvents(true);
	        }
        });
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
			//m_SourceComboBox.removeSelectionListener(m_SourceComboListener);
			m_SourceComboBox.disableEvents(true);
			GWT.log("setSourceComboSelection() m_SourceComboListener removed", null);
			
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
			
			//m_SourceComboBox.addSelectionChangedListener(m_SourceComboListener);
			m_SourceComboBox.enableEvents(true);
			GWT.log("setSourceComboSelection() m_SourceComboListener added", null);
		}
	}
	
	
	/**
	 * Sets the User Combo Box to the selected value. Note that this simply
	 * updates the Combo Box field, and does not update the chart.
	 * @param username value to set.
	 */
	protected void setUserComboSelection(String username)
	{		
		ListStore<BaseModelData> usersStore = m_UserComboBox.getStore();	
		if (usersStore.getCount() > 0)
		{	
			if (username == null)
			{
				//m_UserComboBox.removeSelectionListener(m_UserComboListener);	
				m_UserComboBox.disableEvents(true);
				m_UserComboBox.setValue(usersStore.getAt(0));
				//m_UserComboBox.addSelectionChangedListener(m_UserComboListener);
				m_UserComboBox.enableEvents(true);
			}
			else
			{	
				List<BaseModelData> selectedServices = m_UserComboBox.getSelection();
				String currentlySelectedUser = null;
				if (selectedServices.size() > 0)
				{
					BaseModelData selectedServiceData = selectedServices.get(0);
					if (m_UserComboBox.getStore().indexOf(selectedServiceData) != 0)
			  	  	{
						currentlySelectedUser = selectedServiceData.get("username");
			  	  	}
				}
				
				if (username.equals(currentlySelectedUser) == false)
				{
					BaseModelData userData;
					for (int i = 0; i < usersStore.getCount(); i++)
					{
						userData = usersStore.getAt(i);
						if (userData.get("username").equals(username))
						{
							//m_UserComboBox.removeSelectionListener(m_UserComboListener);
							m_UserComboBox.disableEvents(true);
							m_UserComboBox.setValue(userData);		
							//m_UserComboBox.addSelectionChangedListener(m_UserComboListener);
							m_UserComboBox.enableEvents(true);
							break;
						}
					}
				}
			}
			
			
		}
	}
	
	
	/**
	 * Returns a list of the available metrics for user usage.
	 * @return list of user usage metrics.
	 */
	private ListStore<BaseModelData> getMetricList()
	{
		ListStore<BaseModelData> metrics = new ListStore<BaseModelData>();
		
		BaseModelData metrics1 = new BaseModelData();
		metrics1.set("metricName", "total");
		metrics.add(metrics1);
		
		return metrics;
	}
	
}
