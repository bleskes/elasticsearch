package demo.app.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.VerticalAlignment;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.*;
import com.extjs.gxt.ui.client.widget.form.*;
import com.extjs.gxt.ui.client.widget.layout.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import demo.app.data.UsageRecord;


/**
 * An extension of the Ext JS GXT Window for a Prelert Desktop Chart View Window.
 * The Window contains a GChart component for displaying server monitoring data
 * versus time on a daily basis i.e. usage over a 24 hour period.
 * @author Pete Harverson
 */
public class DailyUsageChartWindow extends Window
{
	private DesktopApp				m_Desktop;
	
	private UsageChart				m_LineChart;
	
	private ComboBox<BaseModelData> m_SourceComboBox;
	private ComboBox<BaseModelData> m_UserComboBox;
	private SelectionChangedListener<BaseModelData>	m_UserComboListener;
	
	
	private Date m_Date; 	// Current day being displayed. Need to replace with some sort of Page Loader.
	
	public DailyUsageChartWindow(DesktopApp desktop)
	{
		m_Desktop = desktop;
		
		setCloseAction(CloseAction.HIDE);
		setIconStyle("line-graph-win-icon");
		setMinimizable(true);
		setMaximizable(true);
		setHeading("Daily User Usage");
		setSize(750, 520);
		setResizable(true);
		
		// Set default date as now.
		m_Date = new Date();
		
		initComponents();
	}
	
	
	/**
	 * Returns the chart being displayed in the window.
	 * @return the chart being displayed in the window.
	 */
	public UsageChart getChart()
	{
		return m_LineChart;
	}
	
	
	public Date getDate()
    {
    	return m_Date;
    }


	public void setDate(Date date)
    {
    	m_Date = date;
    }
	
	
	public void load()
	{
		showTotalUsage();
	}


	/**
	 * Updates the chart to display usage data from all servers for the currently configured day.
	 */
	public void showTotalUsage()
	{
		m_Desktop.getUserUsageQueryServiceInstance().getTotalDailyUsageData(
				m_Date, "total", new AsyncCallback<List<UsageRecord>>(){

	        public void onFailure(Throwable caught)
	        {
		        MessageBox.alert("Prelert - Error",
		                "Failed to get a response from the server", null);
	        }


	        public void onSuccess(List<UsageRecord> records)
	        {
	        	// Set the records.
	        	m_LineChart.setUsageRecords(records);
	        	
	        	// Set the x axis range for the currently specified day.
	        	Date startDate = new Date(m_Date.getYear(), m_Date.getMonth(), m_Date.getDate());
	        	Date endDate = new Date(startDate.getTime() + (24*60*60*1000));
	        	
	        	m_LineChart.setXAxisRange(startDate, endDate, 9, 2);
	        	
	        	m_LineChart.update();
	        }
        });
	}
	
	
	/**
	 * Loads usage data into the window for <i>all users</i> on the specified source.
	 * @param source the name of the source for which to display usage data.
	 */
	public void load(String source, boolean repopulateUsers)
	{
		final String sourceName = source;
		final boolean populateUsers = repopulateUsers;
		
		m_Desktop.getUserUsageQueryServiceInstance().getWeeklyUsageData("total", 
				sourceName, new AsyncCallback<List<UsageRecord>>(){

	        public void onFailure(Throwable caught)
	        {
		        MessageBox.alert("Prelert - Error",
		                "Failed to get a response from the server", null);
	        }


	        public void onSuccess(List<UsageRecord> records)
	        {
	        	m_LineChart.setUsageRecords(records);
	        	m_LineChart.update();
	        	
	        	if (populateUsers == true)
	        	{
	        		// Update the list of users for this source.
	        		populateUsers(sourceName);
	        	}
	        }
        });
	}
	
	
	/**
	 * Loads usage data into the window for the specified source and user.
	 * @param source the name of the source for which to display usage data.
	 * @param user the username for which to display usage data.
	 */
	public void load(String source, String user, boolean repopulateUsers)
	{
		if (user == null)
		{
			load(source, repopulateUsers);
			return;
		}
		
		final String sourceName = source;
		final String username = user;
		
		m_Desktop.getUserUsageQueryServiceInstance().getWeeklyUsageData(sourceName, username, 
				new AsyncCallback<List<UsageRecord>>(){

	        public void onFailure(Throwable caught)
	        {
		        MessageBox.alert("Prelert - Error",
		                "Failed to get a response from the server", null);
	        }


	        public void onSuccess(List<UsageRecord> records)
	        {
	        	m_LineChart.setUsageRecords(records);
	        	m_LineChart.update();
	        }
        });
	}
	
	
	/**
	 * Returns the currently selected source (server).
	 * @return the name of the source whose usage data is currently being viewed,
	 * or <code>null</code> if no source is currently selected.
	 */
	public String getSource()
	{
		String sourceName = null;
		List<BaseModelData> selectedSources = m_SourceComboBox.getSelection();
		
		if (selectedSources.size() > 0)
		{
			sourceName = selectedSources.get(0).get("source");
		}
			
		return sourceName;
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
		    	  BaseModelData selectedSource = se.getSelectedItem();
		    	  
		    	  if (m_SourceComboBox.getStore().indexOf(selectedSource) == 0)
		    	  {
		    		  showTotalUsage();
		    	  }
		    	  else
		    	  {
		    		  String source = selectedSource.get("source");
		    		  load(source, true);
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
		    	  BaseModelData selectedUser = se.getSelectedItem();
		    	  
		    	  if (m_UserComboBox.getStore().indexOf(selectedUser) == 0)
		    	  {
		    		  load(getSource(), null, false);
		    	  }
		    	  else
		    	  {
		    		  String username = selectedUser.get("username");
		    		  load(getSource(), username, false);
		    	  }
		      }
		};
		m_UserComboBox.addSelectionChangedListener(m_UserComboListener);
		
		// Put the ComboBox inside a panel to get it to vertically align properly.
		HorizontalPanel userComboBoxPanel = new HorizontalPanel();
		userComboBoxPanel.add(m_UserComboBox);
		toolPanel.add(userComboBoxPanel);
		
		
		// Create a Metric-type (e.g. Total, Pending) drop-down.
		toolPanel.add(new LabelField("Show: "));
		ComboBox<BaseModelData> metricComboBox = new ComboBox<BaseModelData>();
		metricComboBox.setStore(getTestMetricList());
		metricComboBox.setEmptyText("Select metric...");
		metricComboBox.setEditable(false);
		metricComboBox.setDisplayField("metricName");
		metricComboBox.setTypeAhead(true);
		
		// Put the ComboBox inside a panel to get it to vertically align properly.
		HorizontalPanel metricComboBoxPanel = new HorizontalPanel();
		metricComboBoxPanel.add(metricComboBox);
		toolPanel.add(metricComboBoxPanel);

		
		// Create radio buttons for setting the time frame of the chart
		// e.g. Week, Daily, Hourly.
		RadioGroup timeFrameGroup = new RadioGroup();
		Radio weekRadio = new Radio();
		weekRadio.addInputStyleName("tool-radio-button");
		weekRadio.setName("graphTimeFrame");
		weekRadio.setBoxLabel("Week");
		timeFrameGroup.add(weekRadio);
		
		Radio dayRadio = new Radio();
		dayRadio.setName("graphTimeFrame");
		dayRadio.addInputStyleName("tool-radio-button");
		dayRadio.setBoxLabel("Daily");
		timeFrameGroup.add(dayRadio);
		
		Radio hourRadio = new Radio();
		hourRadio.setName("graphTimeFrame");
		hourRadio.addInputStyleName("tool-radio-button");
		hourRadio.setBoxLabel("Hourly");
		timeFrameGroup.add(hourRadio);
		
		TableData td = new TableData();
		td.setHorizontalAlign(HorizontalAlignment.RIGHT);
		td.setWidth("100%");
		toolPanel.add(timeFrameGroup, td);
		
		
		// Add the chart to the centre of the window.
		BorderLayoutData centerLayoutData = new BorderLayoutData(LayoutRegion.CENTER);
		centerLayoutData.setMargins(new Margins(0, 0, 0, 0));
		
		ContentPanel center = new ContentPanel();
		center.setHeaderVisible(false);
		
		m_LineChart = new UsageChart();
		m_LineChart.setDesktop(m_Desktop);
		center.add(m_LineChart);
		
		m_LineChart.update();
		
		add(center, centerLayoutData); 
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
		
		m_Desktop.getUserUsageQueryServiceInstance().getSources(new AsyncCallback<List<String>>(){

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
	
	
	public void populateUsers(String source)
	{
		// Clear out the User ComboBox and repopulate with the users for this source.
		m_UserComboBox.clearSelections();
		final ListStore<BaseModelData> usernamesStore = m_UserComboBox.getStore();
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
		
		m_Desktop.getUserUsageQueryServiceInstance().getUsers(
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
	 * Returns a dummy list of metric names.
	 * @return
	 */
	private ListStore<BaseModelData> getTestMetricList()
	{
		ListStore<BaseModelData> metrics = new ListStore<BaseModelData>();
		
		BaseModelData metrics1 = new BaseModelData();
		metrics1.set("metricName", "Total");
		metrics.add(metrics1);
		
		BaseModelData metrics2 = new BaseModelData();
		metrics2.set("metricName", "Pending");
		metrics.add(metrics2);
		
		return metrics;
	}
}

