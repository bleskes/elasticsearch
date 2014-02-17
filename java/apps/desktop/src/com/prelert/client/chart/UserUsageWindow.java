package com.prelert.client.chart;

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
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.LabelField;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData; 
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.GChart.Curve.Point;

import com.prelert.client.Desktop;
import com.prelert.client.ViewWindow;
import com.prelert.client.list.ListViewMenuItem;
import com.prelert.client.widget.DatePagingToolBar;
import com.prelert.data.*;
import com.prelert.service.DatabaseServiceLocator;
import com.prelert.service.UsageQueryServiceAsync;


/**
 * An extension of the Ext JS GXT Window for a Prelert Desktop Usage Usage Window.
 * The Window contains a usage chart and controls for configuring the usage data
 * to be displayed e.g. source, user and date.
 * @author Pete Harverson
 */
public class UserUsageWindow extends ViewWindow
{
	private Desktop					m_Desktop;
	private UsageView				m_UsageView;
	private UsageChart				m_UsageChart;
	
	private DatePagingLoader<DatePagingLoadConfig, DatePagingLoadResult> m_Loader;
	private TimeFrame				m_TimeFrame;
	
	private UsageQueryServiceAsync	m_UsageQueryService;
	
	private ComboBox<BaseModelData> m_SourceComboBox;
	private ComboBox<BaseModelData> m_UserComboBox;
	private SelectionChangedListener<BaseModelData>	m_UserComboListener;
		
	
	/**
	 * Constructs an empty user usage window.
	 * @param desktop the parent Prelert desktop.
	 */
	public UserUsageWindow(Desktop desktop)
	{
		m_Desktop = desktop;
		
		m_UsageQueryService = DatabaseServiceLocator.getInstance().getUsageQueryService("Users");
		
		setCloseAction(CloseAction.HIDE);
		setIconStyle("line-graph-win-icon");
		setMinimizable(true);
		setMaximizable(true);
		setHeading("User Usage");
		setSize(750, 520);
		setResizable(true);
		
		// Default TimeFrame is WEEK.
		m_TimeFrame = TimeFrame.WEEK;
		
		initComponents();
	}
	
	
	/**
	 * Constructs a window to display a chart of user usage.
	 * @param desktop the parent Prelert desktop.
	 * @param usageView the UserUsageView to display in the window.
	 */
	public UserUsageWindow(Desktop desktop, UsageView usageView)
	{
		this(desktop);
		
		m_UsageView = usageView;
		setHeading(m_UsageView.getName());
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
		    		  clearUsers();
		    	  }
		    	  else
		    	  {
		    		  String source = selectedSource.get("source");
		    		  populateUsers(source);
		    	  }
		      }
		});

		
		// Put the ComboBox inside a panel to get it to vertically align properly.
		HorizontalPanel serverComboBoxPanel = new HorizontalPanel();
		serverComboBoxPanel.add(m_SourceComboBox);
		toolPanel.add(serverComboBoxPanel);
		
		
		// Create a User drop-down.
		toolPanel.add(new LabelField("User: "));
		m_UserComboBox = new ComboBox<BaseModelData>();
		m_UserComboBox.setStore(new ListStore<BaseModelData>());
		m_UserComboBox.setEmptyText("Select user...");
		m_UserComboBox.setEditable(false);
		m_UserComboBox.setDisplayField("username");
		m_UserComboBox.setTypeAhead(true);
		m_UserComboListener = new SelectionChangedListener<BaseModelData>() {
		      public void selectionChanged(SelectionChangedEvent<BaseModelData> se) 
		      {
		    	  load();
		      }
		};
		m_UserComboBox.addSelectionChangedListener(m_UserComboListener);
		
		// Put the ComboBox inside a panel to get it to vertically align properly.
		HorizontalPanel userComboBoxPanel = new HorizontalPanel();
		userComboBoxPanel.add(m_UserComboBox);
		toolPanel.add(userComboBoxPanel);

		
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
	 * Creates the date paging toolbar for navigating through the week.
	 */
	protected void createPagingToolBar()
	{
		DataProxy<DatePagingLoadConfig, DatePagingLoadResult> proxy = 
			new GetUserUsageDataRpcProxy();
		
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
		
		//m_Loader.load();
	}
	
	
	/**
	 * Returns the View displayed in the Window.
	 * @return the list view displayed in the Window.
	 */
	public View getView()
	{
		return m_UsageView;
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
		
		// Build the title of the chart.
		String source = getSource();
		String user = getUsername();
		String chartTitle = m_UsageView.getName();
		if (source != null)
		{
			chartTitle += source;
		}
		else
		{
			chartTitle += m_UsageView.getAllSourcesText();
		}
		chartTitle += ", ";
		if (user != null)
		{
			chartTitle += user;
		}
		else
		{
			chartTitle += m_UsageView.getAllUsersText();
		}
		
		m_UsageChart.setUsageRecords(records);
		m_UsageChart.setTitleText(chartTitle);
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
	
	
	public void openDrillDownView(ListViewMenuItem menuItem, BaseModelData selectedRecord)
	{
		// To implement.
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
	public String getUsername()
	{		
		String username = null;
		
		List<BaseModelData> selectedUsers = m_UserComboBox.getSelection();
		
		if (selectedUsers.size() > 0)
		{
			BaseModelData userData = selectedUsers.get(0);
			if (m_UserComboBox.getStore().indexOf(userData) != 0)
	  	  	{
				username = userData.get("username");
	  	  	}
		}
		
		return username;
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
	        }
        });
	}
	
	
	/**
	 * Clears the list of users in the 'User' drop-down, and adds a single
	 * 'All Users' items into the drop-down combo box.
	 */
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
		m_UserComboBox.removeSelectionListener(m_UserComboListener);
		m_UserComboBox.setValue(allUsersData);  
		m_UserComboBox.addSelectionChangedListener(m_UserComboListener);
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

		m_UsageQueryService.getUsers(
				source, new AsyncCallback<List<String>>(){

	        public void onFailure(Throwable caught)
	        {
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
	        }
        });
	}
	
	
	/**
	 * RpcProxy that retrieves user usage data from the Usage query service via
	 * GWT remote procedure calls.
	 */
	class GetUserUsageDataRpcProxy<C extends DatePagingLoadConfig, D extends DatePagingLoadResult>
		extends RpcProxy<DatePagingLoadConfig, DatePagingLoadResult<UsageRecord>>
	{

	    protected void load(DatePagingLoadConfig loadConfig,
	            AsyncCallback<DatePagingLoadResult<UsageRecord>> callback)
	    {
	    	String source = getSource();
	    	String metric = "total";
	    	
	    	if (source != null)
	    	{
	    		String username = getUsername();
	    		if (username != null)
	    		{
	    			m_UsageQueryService.getUsageData(metric, source, username, 
	    					loadConfig, callback);
	    		}
	    		else
	    		{
	    			m_UsageQueryService.getUsageData(metric, source, 
	    					loadConfig, callback);
	    		}
	    	}
	    	else
	    	{
	    		m_UsageQueryService.getUsageData(metric, loadConfig, callback);
	    	}
	    }
	
	}


}
