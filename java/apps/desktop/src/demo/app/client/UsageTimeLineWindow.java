package demo.app.client;

import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.VerticalAlignment;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.HorizontalPanel;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.LabelField;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.events.Handler;
import com.google.gwt.visualization.client.events.RangeChangeHandler;
import com.google.gwt.visualization.client.events.ReadyHandler;
import com.google.gwt.visualization.client.events.SelectHandler;
import com.google.gwt.visualization.client.*;
import com.google.gwt.visualization.client.visualizations.*;
import com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine.ScaleType;
import com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine.WindowMode;

import demo.app.data.UsageRecord;
import demo.app.service.DatabaseServiceLocator;
import demo.app.service.UsageQueryServiceAsync;


/**
 * An extension of the Ext JS GXT Window for a Prelert Desktop Service Usage Window.
 * The Window contains a usage time line, built around the Google Visualizations 
 * Annotated Time Line, with controls for configuring the usage data to be displayed 
 * e.g. source, user, metric and date.
 * @author Pete Harverson
 */
public class UsageTimeLineWindow extends ViewWindow
{
	private DesktopApp				m_Desktop;
	private UsageQueryServiceAsync	m_UsageQueryService;
	
	private AnnotatedTimeLine		m_TimeLineChart;
	private DataTable				m_DataTable;
	
	private String					m_Metric;
	private String					m_Source;
	private String					m_Service;
	
	private ComboBox<BaseModelData> m_SourceComboBox;
	private ComboBox<BaseModelData> m_ServiceComboBox;
	private ComboBox<BaseModelData> m_MetricComboBox;
	private SelectionChangedListener<BaseModelData>	m_SourceComboListener;
	private SelectionChangedListener<BaseModelData>	m_ServiceComboListener;
	private SelectionChangedListener<BaseModelData>	m_MetricComboListener;
	
	
	/**
	 * Constructs an empty usage time line window.
	 * @param desktop the parent Prelert desktop.
	 */
	public UsageTimeLineWindow(DesktopApp desktop)
	{
		m_Desktop = desktop;
		
		m_UsageQueryService = DatabaseServiceLocator.getInstance().getServiceUsageQueryService();
		
		setIconStyle("line-graph-win-icon");
		setMinimizable(true);
		setMaximizable(true);
		setHeading("Service Usage TimeLine");
		setSize(760, 550);
		setResizable(true);
		
		initComponents();
			
	}
	
	
	/**
	 * Creates and initialises the graphical components in the window.
	 */
	protected void initComponents()
	{
		setLayout(new FitLayout()); 
		
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
		m_SourceComboListener = new SelectionChangedListener<BaseModelData>() {
		      public void selectionChanged(SelectionChangedEvent<BaseModelData> se) 
		      {
		    	  BaseModelData selectedSource = se.getSelectedItem();
		    	  
		    	  if (m_SourceComboBox.getStore().indexOf(selectedSource) == 0)
		    	  {
		    		  m_Service = null;
		    		  setSource(null);
		    		  clearServices();
		    	  }
		    	  else
		    	  {
		    		  m_Service = null;
		    		  String source = selectedSource.get("source");
		    		  setSource(source);
		    		  populateServices(source);
		    	  }
		      }
		};
		m_SourceComboBox.addSelectionChangedListener(m_SourceComboListener);

		
		// Put the ComboBox inside a panel to get it to vertically align properly.
		HorizontalPanel sourceComboBoxPanel = new HorizontalPanel();
		sourceComboBoxPanel.add(m_SourceComboBox);
		toolPanel.add(sourceComboBoxPanel);
		
		
		// Create a Service drop-down, and populate initially with an 'All Services' item.
		toolPanel.add(new LabelField("Service: "));
		m_ServiceComboBox = new ComboBox<BaseModelData>();
		ListStore<BaseModelData> servicesStore = new ListStore<BaseModelData>();
		BaseModelData allServicesData = new BaseModelData();
		allServicesData.set("server", "All Services");
		servicesStore.add(allServicesData);
		m_ServiceComboBox.setStore(servicesStore);
		m_ServiceComboBox.setEmptyText("Select service...");
		m_ServiceComboBox.setEditable(false);
		m_ServiceComboBox.setDisplayField("server");
		m_ServiceComboBox.setTypeAhead(true);
		m_ServiceComboListener = new SelectionChangedListener<BaseModelData>() {
		      public void selectionChanged(SelectionChangedEvent<BaseModelData> se) 
		      {
		    	  BaseModelData selectedService = se.getSelectedItem();
		    	  
		    	  if (m_ServiceComboBox.getStore().indexOf(selectedService) == 0)
		    	  {
		    		  setService(null);
		    	  }
		    	  else
		    	  {
		    		  String service = selectedService.get("server");
		    		  setService(service);
		    	  } 
		      }
		};
		m_ServiceComboBox.addSelectionChangedListener(m_ServiceComboListener);
		
		// Put the ComboBox inside a panel to get it to vertically align properly.
		HorizontalPanel serviceComboBoxPanel = new HorizontalPanel();
		serviceComboBoxPanel.add(m_ServiceComboBox);
		toolPanel.add(serviceComboBoxPanel);
		
		
		// Create a Metric-type (e.g. Total, Pending) drop-down.
		toolPanel.add(new LabelField("Show: "));
		m_MetricComboBox = new ComboBox<BaseModelData>();
		m_MetricComboBox.setStore(getMetricList());
		m_Metric = getMetricList().getAt(0).get("metric");
		m_MetricComboBox.setEmptyText("Select metric...");
		m_MetricComboBox.setEditable(false);
		m_MetricComboBox.setDisplayField("metric");
		m_MetricComboBox.setTypeAhead(true);
		
		// Put the ComboBox inside a panel to get it to vertically align properly.
		HorizontalPanel metricComboBoxPanel = new HorizontalPanel();
		metricComboBoxPanel.add(m_MetricComboBox);
		toolPanel.add(metricComboBoxPanel);
		
		//m_MetricComboBox.setValue(m_MetricComboBox.getStore().getAt(0)); 
		m_MetricComboListener = new SelectionChangedListener<BaseModelData>() {
		      public void selectionChanged(SelectionChangedEvent<BaseModelData> se) 
		      {
		    	  BaseModelData selectedMetric = se.getSelectedItem();
		    	  String metric = selectedMetric.get("metric");
		    	  setMetric(metric);
		      }
		};
		m_MetricComboBox.addSelectionChangedListener(m_MetricComboListener);
		
		
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
	    VisualizationUtils.loadVisualizationApi(googleVizCallback, PieChart.PACKAGE, 
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

	    m_DataTable = DataTable.create();
	    
	    m_DataTable.addColumn(ColumnType.DATETIME, "Date");
	    m_DataTable.addColumn(ColumnType.NUMBER, "serverload");
	    m_DataTable.addColumn(ColumnType.STRING, "title1");
		
	    final AnnotatedTimeLine timeLine = new AnnotatedTimeLine(m_DataTable, options, "730px", "480px");
	    Selection.addSelectHandler(timeLine, new SelectHandler(){

			@Override
            public void onSelect(SelectEvent event)
            {
				JsArray<Selection> selections = Selection.getSelections(timeLine);
				Selection selected = selections.get(0);
				int selectedRow = selected.getRow();
				MessageBox.alert("Show Evidence", "Show evidence at time: " + 
						m_DataTable.getValueDate(selectedRow, 0), null);
            }

	    });
	    
	    timeLine.addRangeChangeHandler(new RangeChangeHandler()
		{
	    	
			@Override
			public void onRangeChange(RangeChangeEvent event)
			{
				load(event.getStart(), event.getEnd());
			}
		});
	    
	    Handler.addHandler(timeLine, "ready", new ReadyHandler(){

            public void onReady(ReadyEvent event)
            {
        		setMetricComboSelection(m_Metric);
        		setSourceComboSelection(m_Source);	
        		setServiceComboSelection(m_Service);
        		
	    	    m_TimeLineChart.setWidth("730px");
	    	    m_TimeLineChart.setHeight("480px");
	    	    

            }
	    	
	    });
	    
	    return timeLine;
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
	 * Returns the currently selected service.
	 * @return the name of the service whose usage data is currently being viewed,
	 * or <code>null</code> if no specific service is currently selected
	 * i.e. 'All Services' is selected.
	 */
	public String getService()
	{	
		return m_Service;
	}
	
	
	/**
	 * Sets the name of the service, and reloads the usage data in the window.
	 * @param service the name of the service whose usage data is to be
	 * displayed. If <code>null</code>, data from all services for the currently
	 * selected source will be displayed.
	 */
	public void setService(String service)
	{
		m_Service = service;
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
	 * Loads the full range of available usage data into the window, displayed
	 * at coarsest granularity.
	 */
	public void load()
	{
		// Loads the full range, with WEEK granularity.
	    load(null, null);
	}
	
	
	/**
	 * Loads usage data into the window, zoomed between the specified start and
	 * end dates.
	 * @param zoomStartDate start date/time for the zoom period. If <code>null</code>
	 * the entire usage data will be displayed at coarsest granularity.
	 * @param zoomEndDate end date/time for the zoom period. If <code>null</code>
	 * the entire usage data will be displayed at coarsest granularity.
	 */
	public void load(Date zoomStartDate, Date zoomEndDate)
	{	
		final Date zoomStart = zoomStartDate;
		final Date zoomEnd = zoomEndDate;
		
		
		// Workaround to get round in bug in Google Visualization code (1.0.2) in
		// reading values from zoom range.
		// http://groups.google.com/group/google-visualization-api/browse_thread/thread/cbe55119c22ac45a
		// Need to adjust for timezone offset to ensure correct time gets passed to query.
		Date qryZoomStart = zoomStartDate;
		Date qryZoomEnd = zoomEndDate;
		if (zoomStartDate != null && zoomEndDate != null)
		{
			qryZoomStart = new Date(zoomStartDate.getTime() + (zoomStartDate.getTimezoneOffset()*60*1000));
			qryZoomEnd = new Date(zoomEndDate.getTime() + (zoomEndDate.getTimezoneOffset()*60*1000));
		}
		
		GWT.log("UsageTimeLineWindow adjusted zoom: " + qryZoomStart + " to " + qryZoomEnd, null);
		
		m_UsageQueryService.getTimeLineUsageData(
				qryZoomStart, qryZoomEnd, getMetric(), getSource(), getService(), 
				new AsyncCallback<List<UsageRecord>>(){

	        public void onFailure(Throwable caught)
	        {
		        MessageBox.alert("Prelert - Error",
		                "Failed to get a response from the server", null);
	        }


	        @SuppressWarnings("deprecation")
            public void onSuccess(List<UsageRecord> records)
	        {	
	        	GWT.log("Size of data set: " + records.size(), null);
	        	
	        	m_DataTable.setColumnLabel(1, getMetric());
	        	
	        	if (m_DataTable.getNumberOfRows() > 0)
	        	{
	        		m_DataTable.removeRows(0, m_DataTable.getNumberOfRows());
	        	}
	        	
	        	m_DataTable.addRows(records.size());
	        	
	        	int i = 0;
	        	for (UsageRecord record : records)
	        	{
	        		m_DataTable.setValue(i, 0, record.getTime());
	        		m_DataTable.setValue(i, 1, record.getValue());
	        		
	        		i++;
	        	}
	        	
	        	// Need to show at least 1 annotation otherwise width of chart increases!
	        	m_DataTable.setValue(0, 2, "Evidence");
	        	
	    	    com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine.Options options = 
	    	    	com.google.gwt.visualization.client.visualizations.AnnotatedTimeLine.Options.create();
	    	    options.setDisplayAnnotations(true);
	    	    options.setAnnotationsWidth(15);
	    	    options.setWindowMode(WindowMode.TRANSPARENT);
	    	    
	    	    // Maximize y-axis scale depending on max value.
	    	    options.setScaleType(ScaleType.MAXIMIZE);
	    	    
	    	    if (zoomStart != null && zoomEnd != null)
	    	    {
	    	    	options.setZoomStartTime(zoomStart);
	    	    	options.setZoomEndTime(zoomEnd);
	    	    }
	    	    
	        	m_TimeLineChart.draw(m_DataTable, options);
	        }
	        
	        
        });
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
	        		populateServices(m_Source);
	        	}
	        }
        });
	}
	
	
	/**
	 * Clears the list of servers in the 'Server' drop-down, and adds a single
	 * 'All Servers' items into the drop-down combo box.
	 */
	public void clearServices()
	{
		// Clear out the Service ComboBox and repopulate with the services for this source.
		m_ServiceComboBox.clearSelections();
		ListStore<BaseModelData> usernamesStore = m_ServiceComboBox.getStore();
		usernamesStore.removeAll();
		
		// Add an 'All Services' item at the top.
		BaseModelData allUsersData = new BaseModelData();
		allUsersData.set("server", "All Services");
		usernamesStore.add(allUsersData);
		
		// Disable the SelectionChangedListener whilst setting the initial
		// value to ensure it does not trigger another query.
		m_ServiceComboBox.removeSelectionListener(m_ServiceComboListener);
		m_ServiceComboBox.setValue(allUsersData);  
		m_ServiceComboBox.addSelectionChangedListener(m_ServiceComboListener);
	}
	
	
	/**
	 * Populates the 'Service' drop-down combo box with the list of servers for the
	 * supplied source.
	 * @param source source for which to obtain the services.
	 */
	public void populateServices(String source)
	{
		// Clear out the Server ComboBox and repopulate with the servers for this source.
		clearServices();
		final ListStore<BaseModelData> servicesStore = m_ServiceComboBox.getStore();

		m_UsageQueryService.getUsers(source, new AsyncCallback<List<String>>(){

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
	        		servicesStore.add(serverData);
	        	}
	        	
	        	if (m_Service != null)
	        	{
	        		// Set the Service ComboBox in case when the window is first
	        		// loaded this call completes AFTER the data is loaded.
	        		setServiceComboSelection(m_Service);
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
			m_MetricComboBox.removeSelectionListener(m_MetricComboListener);
			
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
			
			m_MetricComboBox.addSelectionChangedListener(m_MetricComboListener);
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
			m_SourceComboBox.removeSelectionListener(m_SourceComboListener);
			
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
			
			m_SourceComboBox.addSelectionChangedListener(m_SourceComboListener);
		}
	}
	
	
	/**
	 * Sets the Service Combo Box to the selected value. Note that this simply
	 * updates the Combo Box field, and does not update the chart.
	 * @param service value to set.
	 */
	protected void setServiceComboSelection(String service)
	{		
		ListStore<BaseModelData> servicesStore = m_ServiceComboBox.getStore();	
		if (servicesStore.getCount() > 0)
		{	
			if (service == null)
			{
				m_ServiceComboBox.removeSelectionListener(m_ServiceComboListener);		
				m_ServiceComboBox.setValue(servicesStore.getAt(0));
				m_ServiceComboBox.addSelectionChangedListener(m_ServiceComboListener);
			}
			else
			{	
				List<BaseModelData> selectedServices = m_ServiceComboBox.getSelection();
				String currentlySelectedService = null;
				if (selectedServices.size() > 0)
				{
					BaseModelData selectedServiceData = selectedServices.get(0);
					if (m_ServiceComboBox.getStore().indexOf(selectedServiceData) != 0)
			  	  	{
						currentlySelectedService = selectedServiceData.get("server");
			  	  	}
				}
				
				if (service.equals(currentlySelectedService) == false)
				{
					BaseModelData serviceData;
					for (int i = 0; i < servicesStore.getCount(); i++)
					{
						serviceData = servicesStore.getAt(i);
						if (serviceData.get("server").equals(service))
						{
							m_ServiceComboBox.removeSelectionListener(m_ServiceComboListener);
							m_ServiceComboBox.setValue(serviceData);		
							m_ServiceComboBox.addSelectionChangedListener(m_ServiceComboListener);
							break;
						}
					}
				}
			}
			
			
		}
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
}
