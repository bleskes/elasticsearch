package demo.app.client.chart;

import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.VerticalAlignment;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.data.DataProxy;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.*;
import com.extjs.gxt.ui.client.widget.form.*;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.layout.*;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.googlecode.gchart.client.GChart.Curve.Point;

import demo.app.client.ApplicationResponseHandler;
import demo.app.client.DatePagingToolBar;
import demo.app.client.DesktopApp;
import demo.app.client.ViewMenuItem;
import demo.app.client.ViewWindow;
import demo.app.client.event.ViewMenuItemListener;
import demo.app.data.*;
import demo.app.service.DatabaseServiceLocator;
import demo.app.service.TimeSeriesGXTPagingServiceAsync;


/**
 * An extension of the Ext JS GXT Window for a Prelert Desktop Chart View Window.
 * The Window contains a GChart component for displaying server monitoring data
 * versus time.
 * @author Pete Harverson
 */
public class UsageWindow extends ViewWindow
{
	private DesktopApp				m_Desktop;
	private UsageView				m_UsageView;
	private TimeSeriesGXTPagingServiceAsync	m_TimeSeriesPagingService;
	
	private UsageChart				m_UsageChart;
	
	private DatePagingToolBar		m_ToolBar;
	private DatePagingLoader<DatePagingLoadResult<UsageRecord>> m_Loader;
	private TimeFrame				m_TimeFrame;
	private String					m_Source;
	private String					m_AttributeName;
	private String					m_AttributeValue;
	private String					m_Metric;
	
	private ComboBox<BaseModelData> m_SourceComboBox;
	private ComboBox<BaseModelData> m_AttrNameComboBox;
	private ComboBox<BaseModelData> m_AttrValueComboBox;
	private ComboBox<BaseModelData> m_MetricComboBox;
	private SelectionChangedListener<BaseModelData>	m_SourceComboListener;
	private SelectionChangedListener<BaseModelData>	m_MetricComboListener;
	private SelectionChangedListener<BaseModelData>	m_AttrNameComboListener;
	private SelectionChangedListener<BaseModelData>	m_AttrValueComboListener;

	
	/**
	 * Constructs a window to display a usage chart.
	 * @param desktop the parent Prelert desktop.
	 * @param usageView the usage view to display in the window.
	 */
	public UsageWindow(DesktopApp desktop, UsageView usageView)
	{
		m_Desktop = desktop;
		m_UsageView = usageView;
		
		m_TimeSeriesPagingService = DatabaseServiceLocator.getInstance().getTimeSeriesGXTPagingService();
		
		setHeading(m_UsageView.getName());
		if (m_UsageView.getStyleId() != null)
	    {
			setIconStyle(m_UsageView.getStyleId()+ "-win-icon");
	    }
	    else
	    {
	    	setIconStyle("line-graph-win-icon");
	    }
		
		setMinimizable(true);
		setMaximizable(true);
		
		setSize(750, 520);
		setResizable(true);
		
		// Default TimeFrame is WEEK.
		m_TimeFrame = TimeFrame.WEEK;
		
		initComponents();
		
		// Add a window listener to stop/start auto-refresh as the window
		// is hidden or shown.
		addWindowListener(new WindowListener(){
			
            @Override
            public void windowShow(WindowEvent we)
            {
            	m_ToolBar.resumeAutoRefreshing();
            }


            @Override
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
		//setLayout(new BorderLayout());
		setLayout(new FitLayout());
	
		// Create the controls (Source, Attribute, Metric drop-downs).
		Component controlsPanel = createControlComponents();
		setTopComponent(controlsPanel);		
		
		// Create the chart.
		m_UsageChart = new UsageChart();
		
		// Configure the right-click context menu.
	    Menu menu = new Menu();
	    List<Tool> viewMenuItems = m_UsageView.getContextMenuItems();
	    if (viewMenuItems != null)
	    {
	    	ViewMenuItem menuItem;
	    	ViewMenuItemListener<MenuEvent> menuItemLsnr;
	    	
	    	for (int i = 0; i < viewMenuItems.size(); i++)
	    	{
	    	    menuItem = new ViewMenuItem(viewMenuItems.get(i));
	    	    menuItemLsnr = new ViewMenuItemListener<MenuEvent>(menuItem, this);
	    	    menuItem.addSelectionListener(menuItemLsnr);
	    	    menu.add(menuItem);
	    	}
	    }

	    if (menu.getItemCount() > 0)
	    {
	    	m_UsageChart.setContextMenu(menu);
	    }
		
		
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
		
		BorderLayoutData centerLayoutData = new BorderLayoutData(LayoutRegion.CENTER);
		centerLayoutData.setMargins(new Margins(0, 0, 0, 0));
		
		ContentPanel center = new ContentPanel();
		center.setHeaderVisible(false);
		center.add(m_UsageChart);
		add(center, centerLayoutData); 
		
		// Create the paging toolbar.
		createPagingToolBar();
	}
	
	
	/**
	 * Creates the source, attribute and value controls.
	 */
	protected Component createControlComponents()
	{
		// Create the top panel which will hold the controls for setting the
		// parameters of the chart e.g. Server, Metric, time frame.
		HorizontalPanel toolPanel = new HorizontalPanel();
		toolPanel.addStyleName("x-small-editor");
		toolPanel.setTableHeight("30px");
		toolPanel.setTableWidth("100%");
		toolPanel.setVerticalAlign(VerticalAlignment.MIDDLE);
		
		
		// Create a Server Name drop-down.
		// Populate with the list of available sources, plus an 'All sources'.
		m_SourceComboBox = new ComboBox<BaseModelData>();
		m_SourceComboBox.setListStyle("prelert-combo-list");
		m_SourceComboBox.setEditable(false);
		m_SourceComboBox.setEmptyText(m_UsageView.getSelectSourceText());
		m_SourceComboBox.setDisplayField("source");
		m_SourceComboBox.setTypeAhead(true);
		m_SourceComboBox.setTriggerAction(TriggerAction.ALL);
		m_SourceComboBox.setStore(new ListStore<BaseModelData>());
		
		populateSources();
		m_SourceComboListener = new SelectionChangedListener<BaseModelData>() {
		      @Override
            public void selectionChanged(SelectionChangedEvent<BaseModelData> se) 
		      {
		    	  BaseModelData selectedSource = se.getSelectedItem();
		    	  GWT.log("Source selectionChanged()", null);
		    	  
		    	  m_AttributeValue = null;
		    	  
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
		
		LabelField sourceLabel = new LabelField(m_UsageView.getSourceFieldText() + ":");
		sourceLabel.setStyleName("prelert-combo-label");
		
		toolComboBoxPanel.add(sourceLabel);
		toolComboBoxPanel.add(m_SourceComboBox);
		toolPanel.add(toolComboBoxPanel);
		
		
		// Create attribute name and value drop-downs if the View has a 'User' field.
		if (m_UsageView.hasAttributes() == true)
		{
			m_AttrNameComboBox = new ComboBox<BaseModelData>();
			m_AttrNameComboBox.setListStyle("prelert-combo-list");
			m_AttrNameComboBox.setWidth(90);
			m_AttrNameComboBox.setDisplayField("attributeName");
			m_AttrNameComboBox.setTriggerAction(TriggerAction.ALL);
			m_AttrNameComboBox.setStore(getAttributeNameList());
			m_AttributeName = getAttributeNameList().getAt(0).get("attributeName");
			
			m_AttrNameComboListener = new SelectionChangedListener<BaseModelData>() {

				public void selectionChanged(SelectionChangedEvent<BaseModelData> se) 
				{    	  
					m_AttributeValue = null;
					
					BaseModelData selectedAttr = se.getSelectedItem();
					String attributeName = selectedAttr.get("attributeName");
					setAttributeName(attributeName);
				}
			};
			m_AttrNameComboBox.addSelectionChangedListener(m_AttrNameComboListener);
			
			m_AttrValueComboBox = new ComboBox<BaseModelData>();
			m_AttrValueComboBox.setListStyle("prelert-combo-list");
			ListStore<BaseModelData> attrValStore = new ListStore<BaseModelData>();
			BaseModelData allUsersData = new BaseModelData();
			allUsersData.set("attributeValue", m_UsageView.getAllAttributeValuesText());
			attrValStore.add(allUsersData);
			m_AttrValueComboBox.setStore(attrValStore);
			m_AttrValueComboBox.setEmptyText(m_UsageView.getSelectUserText());
			m_AttrValueComboBox.setDisplayField("attributeValue");
			m_AttrValueComboBox.setTypeAhead(true);
			m_AttrValueComboBox.setTriggerAction(TriggerAction.ALL);
			m_AttrValueComboBox.setStore(new ListStore<BaseModelData>());
	
			m_AttrValueComboListener = new SelectionChangedListener<BaseModelData>() {
			      @Override
                public void selectionChanged(SelectionChangedEvent<BaseModelData> se) 
			      {    	  
			    	  BaseModelData selectedUser = se.getSelectedItem();
			    	  
			    	  if (m_AttrValueComboBox.getStore().indexOf(selectedUser) == 0)
			    	  {
			    		  setAttributeValue(null);  		  
			    	  }
			    	  else
			    	  {
			    		  String username = selectedUser.get("attributeValue");
			    		  setAttributeValue(username);
			    	  } 
			      }
			};
			m_AttrValueComboBox.addSelectionChangedListener(m_AttrValueComboListener);
			
			LabelField spacerLabel = new LabelField(" ");
			spacerLabel.setWidth("10px");
			
			LabelField operatorLabel = new LabelField("=");
			operatorLabel.setStyleName("prelert-combo-label");
			
			toolComboBoxPanel.add(spacerLabel);
			toolComboBoxPanel.add(m_AttrNameComboBox);
			toolComboBoxPanel.add(operatorLabel);
			toolComboBoxPanel.add(m_AttrValueComboBox);
		}
		
		
		// Create a Metric-type (e.g. Total, Pending) drop-down.
		m_MetricComboBox = new ComboBox<BaseModelData>();
		m_MetricComboBox.setListStyle("prelert-combo-list");
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
		
		LabelField metricLabel = new LabelField("Show:");
		metricLabel.setStyleName("prelert-combo-label");
		
		metricComboBoxPanel.add(metricLabel);
		metricComboBoxPanel.add(m_MetricComboBox);
		
		TableData td = new TableData();
		td.setHorizontalAlign(HorizontalAlignment.RIGHT);
		toolPanel.add(metricComboBoxPanel, td);
		
		m_MetricComboListener = new SelectionChangedListener<BaseModelData>() {
		      @Override
            public void selectionChanged(SelectionChangedEvent<BaseModelData> se) 
		      {
		    	  BaseModelData selectedMetric = se.getSelectedItem();
		    	  String metric = selectedMetric.get("metric");
		    	  setMetric(metric);
		      }
		};
		m_MetricComboBox.addSelectionChangedListener(m_MetricComboListener);
		
		return toolPanel;
	}
	
	
	/**
	 * Returns the list of metric names.
	 * @return store of usage metrics for this window.
	 */
	protected ListStore<BaseModelData> getMetricList()
	{
		ListStore<BaseModelData> metrics = new ListStore<BaseModelData>();
		
		List<String> metricNames = m_UsageView.getMetrics();
		
		for (String metricName : metricNames)
		{
			BaseModelData metric = new BaseModelData();
			metric.set("metric", metricName);
			metrics.add(metric);
		}
		
		return metrics;
	}
	
	
	/**
	 * Returns the list of attribute names.
	 * @return store of attribute names for this time series window.
	 */
	protected ListStore<BaseModelData> getAttributeNameList()
	{
		ListStore<BaseModelData> names = new ListStore<BaseModelData>();
		
		if (m_UsageView.hasAttributes() == true)
		{
			List<String> attributeNames = m_UsageView.getAttributeNames();
			
			for (String attributeName : attributeNames)
			{
				BaseModelData name = new BaseModelData();
				name.set("attributeName", attributeName);
				names.add(name);
			}
		}
		
		return names;
	}
	
	
	protected void createPagingToolBar()
	{
		DataProxy<DatePagingLoadResult<UsageRecord>> proxy = 
			new GetUsageDataRpcProxy<DatePagingLoadResult<UsageRecord>>();
		
		m_ToolBar = new DatePagingToolBar(TimeFrame.WEEK, 
				m_UsageView.getAutoRefreshFrequency());
		m_Loader = new DatePagingLoader<DatePagingLoadResult<UsageRecord>>(proxy);
		m_Loader.setTimeFrame(TimeFrame.WEEK);
		m_Loader.setDataType(m_UsageView.getDataType());
		m_Loader.addLoadListener(new LoadListener(){
			
            @Override
            public void loaderBeforeLoad(LoadEvent le)
            {
            	m_SourceComboBox.disableEvents(true);
            	m_MetricComboBox.disableEvents(true);
            	if (m_UsageView.hasAttributes() == true)
            	{
            		m_AttrValueComboBox.disableEvents(true);	
            	}
            }

			@Override
            public void loaderLoad(LoadEvent le) 
			{
				onLoad(le);
				
				m_SourceComboBox.enableEvents(true);
				m_MetricComboBox.enableEvents(true);
				
				if (m_UsageView.hasAttributes() == true)
            	{
					m_AttrValueComboBox.enableEvents(true);
            	}
			}
			
		});
		m_ToolBar.bind(m_Loader);
		setBottomComponent(m_ToolBar);
	}
	
	
	/**
	 * Returns the View displayed in the Window.
	 * @return the usage view displayed in the Window.
	 */
	public UsageView getView()
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
		setSource(source, true);
	}
	
	
	/**
	 * Sets the name of the source (server), and reloads the usage data in the 
	 * window if desired.
	 * @param source the name of the source (server) whose usage data is to be
	 * 		displayed. If <code>null</code>, data from all sources will be displayed.
	 * @param reload <code>true</code> to reload the data, <code>false</code> otherwise.
	 */
	protected void setSource(String source, boolean reload)
	{
		if (source != null && source.equals(m_Source) == true)
		{
			// No change to current setting.
			return;
		}
		
		// Three possibilities:
		// 1. Source is non-null and is in the source combo store.
		// 2. Source is null - load data from all sources.
		// 3. Source is non-null, but does not appear in the sources combo - 
		// 			load data from all sources.
		boolean needsReloading = false;
		if (source != null)
		{
			boolean inStore = false;
			ListStore<BaseModelData> sourcesStore = m_SourceComboBox.getStore();
			
			BaseModelData sourceData;
			for (int i = 0; i < sourcesStore.getCount(); i++)
			{
				sourceData = sourcesStore.getAt(i);
				if (sourceData.get("source").equals(source))
				{
					// i.e. option 1.
					m_Source = source;
					needsReloading = true;
					inStore = true;
					break;
				}
			}
			
			if (inStore == false)
			{
				// Supplied source is not in the store - set to All Sources instead.
				m_Source = null;
				needsReloading = true;
			}
		}
		else
		{
			if (m_Source != null)
			{
				m_Source = null;
				needsReloading = true;
			}
		}
		
		if (reload == true && needsReloading == true)
		{
			if (reload == true)
			{
				load();
			}
		}
		
		// Re-populate the attribute value drop-down for the new Source value.
		populateAttributeValues(m_AttributeName, m_Source);
	}
	
	
	/**
	 * Returns the attribute name for this usage window.
	 * @return the attribute name. If <code>null</code> then the usage view has
	 * no attributes e.g. IPC data.
	 */
	public String getAttributeName()
	{
		return m_AttributeName;
	}
	
	
	/**
	 * Sets the attribute name.
	 * @param name attribute name. If <code>null</code> then the attribute name
	 * 	drop-down will be set to the first item in the list.
	 */
	public void setAttributeName(String name)
	{
		setAttributeName(name, true);
	}
	
	
	/**
	 * Sets the attribute name.
	 * @param name attribute name. If <code>null</code> then the attribute name
	 * 	drop-down will be set to the first item in the list.
	 * @param reload <code>true</code> to reload the data in the chart, <code>false</code> otherwise.
	 */
	protected void setAttributeName(String name, boolean reload)
	{
		if ( (m_UsageView.hasAttributes() == false) || 
				(name != null && name.equals(m_AttributeName) == true) )
		{
			// No change to current setting.
			return;
		}
		
		// Three possibilities:
		// 1. Name is non-null and is in the name combo store.
		// 2. Name is null - set value to first one in store.
		// 3. Name is non-null, but does not appear in the name combo store - 
		// 			set value to first one in store.
		boolean chartNeedsReloading = false;
		boolean attrValuesNeedReloading = false;
		ListStore<BaseModelData> namesStore = m_AttrNameComboBox.getStore();
		if (name != null)
		{
			boolean inStore = false;
			
			BaseModelData namesData;
			for (int i = 0; i < namesStore.getCount(); i++)
			{
				namesData = namesStore.getAt(i);
				if (namesData.get("attributeName").equals(name))
				{
					// i.e. option 1.
					m_AttributeName = name;
					chartNeedsReloading = true;
					attrValuesNeedReloading = true;
					inStore = true;
					break;
				}
			}
			
			if (inStore == false)
			{
				// i.e. option 3 - supplied attribute name is not in the store - 
				// set to the first value in the Combo instead.
				String firstAttrName = namesStore.getAt(0).get("attributeName");
				if ( (m_AttributeName != null) && 
						(m_AttributeName.equals(firstAttrName) == false) )
				{
					attrValuesNeedReloading = true;
				}
				
				m_AttributeName = firstAttrName;
				chartNeedsReloading = true;
			}
		}
		else
		{
			if (m_AttributeName != null)
			{
				// i.e. option 2 
				// set to the first value in the Combo instead.
				String firstAttrName = namesStore.getAt(0).get("attributeName");
				if (m_AttributeName.equals(firstAttrName) == false)
				{
					attrValuesNeedReloading = true;
				}
				
				m_AttributeName = namesStore.getAt(0).get("attributeName");
				chartNeedsReloading = true;
			}
		}
		
		if (reload == true && chartNeedsReloading == true)
		{
			load();	
		}
		
		if (attrValuesNeedReloading == true)
		{
			populateAttributeValues(m_AttributeName, m_Source);
		}
	}
	
	
	/**
	 * Returns the currently selected attribute value.
	 * @return the current attribute value, which may be <code>null</code>.
	 */
	public String getAttributeValue()
	{		
		return m_AttributeValue;
	}
	
	
	/**
	 * Sets the attribute value, and reloads the usage data in the window.
	 * @param value attribute value, which may be <code>null</code>.
	 */
	public void setAttributeValue(String value)
	{
		setAttributeValue(value, true);
	}
	
	
	/**
	 * Sets the attribute value, and reloads the usage data in the window. if desired.
	 * @param value attribute value, which may be <code>null</code>.
	 * @param reload <code>true</code> to reload the data in the chart, <code>false</code> otherwise.
	 */
	protected void setAttributeValue(String value, boolean reload)
	{
		if ( (m_UsageView.hasAttributes() == false) || 
				(value != null && value.equals(m_AttributeValue) == true) ||
				(value == null && m_AttributeValue == null) )
		{
			// No change to current setting.
			return;
		}
		
		// Three possibilities:
		// 1. User is non-null and is in the user combo store.
		// 2. User is null - load data from all attribute values.
		// 3. User is non-null, but does not appear in the users combo - 
		// 		set to new value anyway as the attribute name may have changed too.
		
		// Jan 2010: TO DO: validate that the attribute value is valid for the
		// current source and attribute name. May have to be done in server-side load.
		
		m_AttributeValue = value;
		
		if (reload == true)
		{
			load();
		}
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
		setMetric(metric, true);
	}
	
	
	/**
	 * Sets the name of the metric, and reloads the usage data in the window if desired.
	 * @param metric the name of the usage metric to display.
	 * @param reload <code>true</code> to reload the data, <code>false</code> otherwise.
	 */
	protected void setMetric(String metric, boolean reload)
	{
		if (metric != null && metric.equals(m_Metric) == false)
		{
			ListStore<BaseModelData> metricsStore = m_MetricComboBox.getStore();
			
			BaseModelData metricData;
			for (int i = 0; i < metricsStore.getCount(); i++)
			{
				metricData = metricsStore.getAt(i);
				if (metricData.get("metric").equals(metric))
				{
					m_Metric = metric;
					if (reload == true)
					{
						load();
					}
					break;
				}
			}
		}
	}
	
	
	/**
	 * Convenience method to set all usage window properties in one go, before 
	 * re-loading the usage data in the window.
	 * @param metric the metric to display.
	 * @param source name of the source (server) for which to display the usage.
	 * 		If <code>null</code> the total user usage across all sources and 
	 * 		servers will be displayed.
	 * @param attributeName optional attribute name for the time series to display.
	 * @param attributeValue optional attribute value for the time series to display.
	 * @param date date/time of usage to display.
	 * @param timeFrame time frame of usage to display i.e. WEEK, DAY or HOUR.
	 */
	public void setUsageProperties(String metric, String source, 
			String attributeName, String attributeValue,
			Date date, TimeFrame timeFrame)
	{	
		setMetric(metric, false);
		setSource(source, false);
		
		if (m_UsageView.hasAttributes())
		{
			setAttributeName(attributeName, false);
			setAttributeValue(attributeValue, false);
		}
		
		m_TimeFrame = timeFrame;
		
		// Turn off auto-refresh.
		m_ToolBar.setAutoRefreshing(false);
		
		// Load for the supplied date and time frame.
		m_Loader.load(date, timeFrame);

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
		String chartTitle = m_UsageView.getName();
		chartTitle += " - ";
		if (source != null)
		{
			chartTitle += source;
		}
		else
		{
			chartTitle += m_UsageView.getAllSourcesText();
		}
		
		if (m_UsageView.hasAttributes() == true)
		{
			setAttributeNameSelection(m_AttributeName);
			setAttributeValueSelection(m_AttributeValue);
			
			if (m_AttributeValue != null)
			{
				chartTitle += ", ";
				chartTitle += m_AttributeName;
				chartTitle += "=";
				chartTitle += m_AttributeValue;
			}
		}
		
		m_UsageChart.setUsageRecords(records);
		m_UsageChart.setTitleText(chartTitle);
		m_UsageChart.setXAxisRange(loadResult.getTimeFrame(), loadResult.getDate());    	
		//m_UsageChart.setYAxisLabelText(getMetric());	
    	m_UsageChart.update();	
	}
	
	
	/**
	 * Runs a tool against the hovered over usage record in the View.
	 * @param tool the tool to run.
	 */
	@Override
    public void runTool(Tool tool)
	{
		if (tool.getClass() == ListViewTool.class)
		{
			runListViewTool((ListViewTool)tool);
		}
		else if (tool.getClass() == UsageViewTool.class)
		{
			runUsageViewTool((UsageViewTool)tool);
		}
	}
	
	
	/**
	 * Runs a tool against the hovered over usage record to open a List View.
	 * Currently only the opening of the Evidence View is supported, showing
	 * evidence at the time of the hovered over usage record.
	 * @param tool the tool to run.
	 */
	public void runListViewTool(ListViewTool tool)
	{
		// Get the time of the usage record that was selected. 
		// If no point is selected, do nothing.
		Date selectedTime = m_UsageChart.getTouchedPointTime();
		if (selectedTime != null)
		{
    		m_Desktop.openEvidenceWindow(tool.getViewToOpen(), selectedTime, null);
		}
	}
	
	
	/**
	 * Runs a tool against the hovered over usage record to open another Usage View.
	 * No action is taken if no row is currently selected.
	 * @param tool the UsageViewTool that has been run by the user.
	 */
	public void runUsageViewTool(UsageViewTool tool)
	{
		// Get the time of the usage record that was selected. 
		// If no point is selected, do nothing.
		Date selectedTime = m_UsageChart.getTouchedPointTime();
		if (selectedTime != null)
		{
			String source = getSource();

			// Pass in attribute value if this Usage View and the
			// target view both have the same attribute.
			String attributeValue = null;
			String toolAttributeName = tool.getAttributeName();
			
			if ( (m_AttributeName != null) && (toolAttributeName != null) &&
					(toolAttributeName.equals(m_AttributeName)) )
			{
				attributeValue = getAttributeValue();
			}
			
			m_Desktop.openUsageWindow(tool.getViewToOpen(), tool.getMetric(), source, 
					toolAttributeName, attributeValue, selectedTime, tool.getTimeFrame());
		}
	}
	
	
	/**
	 * Loads usage data into the window.
	 */
	@Override
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
		allSourcesData.set("source", m_UsageView.getAllSourcesText());
		sourcesStore.add(allSourcesData);
		
		m_TimeSeriesPagingService.getSourcesOrderByName(m_UsageView.getDataType(), 
				new ApplicationResponseHandler<List<String>>(){

	        public void uponFailure(Throwable caught)
	        {
		        MessageBox.alert("Prelert - Error",
		                "Failed to get a response from the server", null);
	        }


	        public void uponSuccess(List<String> sources)
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
	        	
	        	populateAttributeValues(m_AttributeName, m_Source);
	        }
        });
	}
	
	
	/**
	 * Clears the list of values in the attribute value drop-down, and adds a single
	 * 'All' items into the drop-down combo box.
	 */
	public void clearAttributeValues()
	{	
		if (m_UsageView.hasAttributes() == true)
		{
			// Clear out the User ComboBox and repopulate with the users for this source.
			m_AttrValueComboBox.clearSelections();
			ListStore<BaseModelData> valuesStore = m_AttrValueComboBox.getStore();
			valuesStore.removeAll();
			
			// Add an 'All' item at the top.
			BaseModelData allValuesData = new BaseModelData();
			allValuesData.set("attributeValue", m_UsageView.getAllAttributeValuesText());
			valuesStore.add(allValuesData);
			
			// Disable the SelectionChangedListener whilst setting the initial
			// value to ensure it does not trigger another query.
			m_AttrValueComboBox.disableEvents(true);
			m_AttrValueComboBox.setValue(allValuesData);  
			m_AttrValueComboBox.enableEvents(true);
		}
	}
	
	
	/**
	 * Populates the attribute value combo box with the list of values for the
	 * supplied attribute name and source (server).
	 * @param attributeName attribute name for which to obtain the values.
	 * @param source optional source (server) for which to obtain the attribute values.
	 */
	public void populateAttributeValues(String attributeName, String source)
	{
		if (m_UsageView.hasAttributes() == true)
		{
		
			// Clear out the User ComboBox and repopulate with the users for this source.
			clearAttributeValues();
			final ListStore<BaseModelData> usernamesStore = m_AttrValueComboBox.getStore();
			
			m_AttrValueComboBox.disableEvents(true);
			
			m_TimeSeriesPagingService.getAttributeValues(m_UsageView.getDataType(), 
					attributeName, source, new ApplicationResponseHandler<List<String>>(){
	
		        public void uponFailure(Throwable caught)
		        {
		        	 m_AttrValueComboBox.enableEvents(true);
		        	
			        MessageBox.alert("Prelert - Error",
			                "Failed to get a response from the server", null);
		        }
	
	
		        public void uponSuccess(List<String> usernames)
		        {
		        	BaseModelData userData;
		        	
		        	for (String username : usernames)
		        	{
		        		userData = new BaseModelData();
		        		userData.set("attributeValue", username);
		        		usernamesStore.add(userData);
		        	}
		        	
		        	if (m_AttributeValue != null)
		        	{
		        		// Set the User ComboBox in case when the window is first
		        		// loaded this call completes AFTER the data is loaded.
		        		setAttributeValueSelection(m_AttributeValue);
		        	}
		        	
		        	m_AttrValueComboBox.enableEvents(true);
		        }
	        });
		
		}
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
	 * Sets the attribute name Combo Box to the specified name. Note that this simply
	 * updates the Combo Box field, and does not update the chart.
	 * @param attributeName name to set.
	 */
	protected void setAttributeNameSelection(String attributeName)
	{
		ListStore<BaseModelData> namesStore = m_AttrNameComboBox.getStore();
		if (namesStore.getCount() > 0)
		{
			m_AttrNameComboBox.disableEvents(true);
			
			List<BaseModelData> selectedNames = m_AttrNameComboBox.getSelection();
			String currentlySelectedName = null;
			if (selectedNames.size() > 0)
			{
				BaseModelData selectedNameData = selectedNames.get(0);
				if (m_AttrNameComboBox.getStore().indexOf(selectedNameData) != 0)
		  	  	{
					currentlySelectedName = selectedNameData.get("attributeName");
		  	  	}
			}
			
			if (attributeName.equals(currentlySelectedName) == false)
			{
				BaseModelData nameData;
				for (int i = 0; i < namesStore.getCount(); i++)
				{
					nameData = namesStore.getAt(i);
					if (nameData.get("attributeName").equals(attributeName))
					{
						m_AttrNameComboBox.setValue(nameData);
						break;
					}
				}
			}
			
			m_AttrNameComboBox.enableEvents(true);
		}
	}
	
	
	/**
	 * Sets the attribute value Combo Box to the selected value. Note that this simply
	 * updates the Combo Box field, and does not update the chart.
	 * @param attributeValue value to set.
	 */
	protected void setAttributeValueSelection(String attributeValue)
	{	
		
		if (m_UsageView.hasAttributes() == true)
		{
		
			ListStore<BaseModelData> usersStore = m_AttrValueComboBox.getStore();	
			if (usersStore.getCount() > 0)
			{	
				if (attributeValue == null)
				{
					m_AttrValueComboBox.disableEvents(true);
					m_AttrValueComboBox.setValue(usersStore.getAt(0));
					m_AttrValueComboBox.enableEvents(true);
				}
				else
				{	
					List<BaseModelData> selectedServices = m_AttrValueComboBox.getSelection();
					String currentlySelectedUser = null;
					if (selectedServices.size() > 0)
					{
						BaseModelData selectedServiceData = selectedServices.get(0);
						if (m_AttrValueComboBox.getStore().indexOf(selectedServiceData) != 0)
				  	  	{
							currentlySelectedUser = selectedServiceData.get("attributeValue");
				  	  	}
					}
					
					if (attributeValue.equals(currentlySelectedUser) == false)
					{
						BaseModelData userData;
						for (int i = 0; i < usersStore.getCount(); i++)
						{
							userData = usersStore.getAt(i);
							if (userData.get("attributeValue").equals(attributeValue))
							{
								m_AttrValueComboBox.disableEvents(true);
								m_AttrValueComboBox.setValue(userData);		
								m_AttrValueComboBox.enableEvents(true);
								break;
							}
						}
					}
				}
				
			}

		}
	}
	
	
	/**
	 * RpcProxy that retrieves user usage data from the Usage query service via
	 * GWT remote procedure calls.
	 */
	class GetUsageDataRpcProxy<D extends DatePagingLoadResult>
		extends RpcProxy<DatePagingLoadResult<UsageRecord>>
	{

		
	    @Override
        protected void load(Object loadConfig,
	            AsyncCallback<DatePagingLoadResult<UsageRecord>> callback)
	    {
	    	String metric = getMetric();
	    	String source = getSource();
	    	String attributeName = getAttributeName();
	    	String attributeValue = getAttributeValue();
	    	
	    	GWT.log("GetUsageDataRpcProxy.load() for " + m_UsageView.getDataType(), null);
	    	GWT.log("\tmetric:" + metric, null);
	    	GWT.log("\tsource:" + source, null);
	    	GWT.log("\tattributeName:" + attributeName, null);
	    	GWT.log("\tattributeValue:" + attributeValue, null);
	    	
	    	m_TimeSeriesPagingService.getDataPoints(
	    			m_UsageView.getDataType(), metric, (DatePagingLoadConfig)loadConfig, 
	    			source, attributeName, attributeValue, callback);
	    }
	
	}
	

	
}
