package demo.app.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import demo.app.data.DatePagingLoadConfig;
import demo.app.data.DatePagingLoadResult;
import demo.app.data.EventRecord;
import demo.app.data.EvidencePagingLoadConfig;
import demo.app.data.EvidenceViewPagingLoader;
import demo.app.data.TimeFrame;
import demo.app.service.DatabaseServiceLocator;
import demo.app.service.EvidenceQueryServiceAsync;


/**
 * An extension of the Ext JS GXT Window for a Prelert Desktop Window displaying
 * the history of an evidence record. The Window contains a Grid component for  
 * paging through a list of records retrieved from the database, with a toolbar  
 * that allows the users to page by date/time and select the time frame of records
 * displayed i.e. Week, Day, Hour, Minute, Second.
 * 
 * @author Pete Harverson
 */
public class EvidenceHistoryWindow extends ViewWindow
{
	private DesktopApp					m_Desktop;
	private EvidenceQueryServiceAsync 	m_QueryService;
	
	private TimeFrame					m_TimeFrame;
	
	private Grid<EventRecord>			m_Grid;
	private Menu						m_ContextMenu;
	private EvidenceViewPagingLoader<DatePagingLoadResult> m_Loader;

	
	/**
	 * Constructs an empty Evidence History window with a SECOND time frame.
	 * @param desktop the parent Prelert desktop.
	 */
	public EvidenceHistoryWindow(DesktopApp desktop)
	{
		this(desktop, TimeFrame.SECOND, null, null);
	}
	
	
	/**
	 * Constructs an empty Evidence History window.
	 * @param desktop the parent Prelert desktop.
	 * @param timeFrame initial time frame for the window,  e.g. week, day or hour.
	 */
	public EvidenceHistoryWindow(DesktopApp desktop, TimeFrame timeFrame)
	{
		this(desktop, timeFrame, null, null);
	}
	
	
	/**
	 * Constructs an empty Evidence History window to display occurrences of
	 * evidence matching the specified filter.
	 * @param desktop the parent Prelert desktop.
	 * @param timeFrame initial time frame for the window,  e.g. week, day or hour.
	 * @param filterAttribute 	the name of the attribute on which the evidence
	 * 	should be filtered.	
	 * @param filterValue		the value of the filter on which the evidence
	 * 	should be filtered.
	 */
	public EvidenceHistoryWindow(DesktopApp desktop, TimeFrame timeFrame,
			String filterAttribute, String filterValue)
	{
		m_Desktop = desktop;
		m_TimeFrame = timeFrame;
		
		m_QueryService = DatabaseServiceLocator.getInstance().getEvidenceQueryService();
		
		setMinimizable(true);
		setMaximizable(true);
		setHeading("History View");
		
		setSize(650, 525);
		setLayout(new FitLayout());
		setResizable(true);
		
		setIconStyle("list-sec-win-icon");
		
		initComponents(filterAttribute, filterValue);
	}
	
	
	/**
	 * Creates and initialises the graphical components in the window.
	 */
	protected void initComponents(String filterAttribute, String filterValue)
	{
		// Create the paging loader.
		createLoader(filterAttribute, filterValue);
		
		EvidenceHistoryPagingToolBar toolBar = new EvidenceHistoryPagingToolBar(this);
		toolBar.bind(m_Loader);
		setTopComponent(toolBar);

		// Create the client-side cache of EventRecord objects displayed in the ListView Grid.
		ListStore<EventRecord> listStore = new ListStore<EventRecord>(m_Loader);
		
		// Create the Grid which displays the data.
		// Default columns at CS for SECOND view:
		// time, description, severity, count, appid, ipaddress, 
		// node, service, source, username, message, id.
	    List<ColumnConfig> config = new ArrayList<ColumnConfig>();
	    config.add(new ColumnConfig("time", "time", 120));
	    config.add(new ColumnConfig("description", "description", 120));
	    config.add(new ColumnConfig("severity", "severity", 120));
	    config.add(new ColumnConfig("count", "count", 120));
	    config.add(new ColumnConfig("appid", "appid", 120));
	    config.add(new ColumnConfig("ipaddress", "ipaddress", 120));
	    config.add(new ColumnConfig("node", "node", 120));
	    config.add(new ColumnConfig("service", "service", 120));
	    config.add(new ColumnConfig("source", "source", 120));
	    config.add(new ColumnConfig("username", "username", 120));
	    config.add(new ColumnConfig("message", "message", 120));
	    config.add(new ColumnConfig("id", "id", 120));
			
		// Create the Grid which displays the data.
	    ColumnModel columnModel = new ColumnModel(config);
	    m_Grid = new Grid<EventRecord>(listStore, columnModel);
	    m_Grid.setLoadMask(true);
	    m_Grid.setBorders(true);
	    m_Grid.setTrackMouseOver(false);
	    m_Grid.setView(new SeverityGridView(listStore));
	    m_Grid.getSelectionModel().setSelectionMode(Style.SelectionMode.SINGLE);
	    
	    // Configure the columns in the grid.
	    configureGridColumns();
	    
	    // Set up a right-click context menu and add tools for launching
	    // the Probable Cause, Evidence and Usage windows.
	    m_ContextMenu = new Menu();
	    
	    MenuItem probCauseItem = new MenuItem("Show Probable Cause");
	    m_ContextMenu.add(probCauseItem);
	    probCauseItem.addSelectionListener(new SelectionListener<MenuEvent>() {
	    	@Override
	    	public void componentSelected(MenuEvent e) 
	    	{
	    		EventRecord selectedRecord = m_Grid.getSelectionModel().getSelectedItem();
	    		m_Desktop.openCausalityWindow(selectedRecord);
	    	}
	    });
	    
	    MenuItem evidenceItem = new MenuItem("Show Evidence");
	    m_ContextMenu.add(evidenceItem);
	    evidenceItem.addSelectionListener(new SelectionListener<MenuEvent>() {
	    	@Override
	    	public void componentSelected(MenuEvent e) 
	    	{
	    		EventRecord selectedRecord = m_Grid.getSelectionModel().getSelectedItem();
	    		
	    		if (m_TimeFrame == TimeFrame.SECOND)
	    		{
	    			m_Desktop.openEvidenceWindow(selectedRecord.getId());
	    		}
	    		else
	    		{
	    			Date evidenceTime = ClientUtil.parseTimeField(selectedRecord, m_TimeFrame);
	    			m_Desktop.openEvidenceWindow(evidenceTime, selectedRecord.getDescription());
	    		}
	    	}
	    });
	    
	    MenuItem userUsageItem = new MenuItem("Show User Usage");
	    m_ContextMenu.add(userUsageItem);
	    userUsageItem.addSelectionListener(new SelectionListener<MenuEvent>() {
	    	@Override
	    	public void componentSelected(MenuEvent e) 
	    	{
	    		EventRecord selectedRecord = m_Grid.getSelectionModel().getSelectedItem();
	    		
	    		Date evidenceTime = ClientUtil.parseTimeField(selectedRecord, m_TimeFrame);
	    		String source = selectedRecord.get("source");
	    		String user = selectedRecord.get("username");
	    		
	    		m_Desktop.openUserUsageWindow(source, user, evidenceTime, TimeFrame.HOUR);
	    	}
	    });
	    
	    MenuItem serviceUsageItem = new MenuItem("Show Service Usage");
	    m_ContextMenu.add(serviceUsageItem);
	    serviceUsageItem.addSelectionListener(new SelectionListener<MenuEvent>() {
	    	@Override
	    	public void componentSelected(MenuEvent e) 
	    	{
	    		EventRecord selectedRecord = m_Grid.getSelectionModel().getSelectedItem();
	    		
	    		Date evidenceTime = ClientUtil.parseTimeField(selectedRecord, m_TimeFrame);
	    		String source = selectedRecord.get("source");
	    		String user = selectedRecord.get("service");
	    		
	    		m_Desktop.openServiceUsageWindow("serverload", source, user, 
	    				evidenceTime, TimeFrame.HOUR);
	    	}
	    });
	    
	    m_Grid.setContextMenu(m_ContextMenu);
	    
	    if (m_TimeFrame == TimeFrame.SECOND)
	    {
	    	m_Grid.setContextMenu(m_ContextMenu);
	    }
	    else
	    {
	    	m_Grid.setContextMenu(null);
	    }
	    
	    add(m_Grid);
	}
	
	
	/**
	 * Creates the paging loader responsible for loading of evidence data into
	 * History window.
	 */
	protected void createLoader(String filterAttribute, String filterValue)
	{
		// Create the RpcProxy to make calls to the server to populate the list.
		GetEvidenceDataRpcProxy<DatePagingLoadResult> proxy = 
			new GetEvidenceDataRpcProxy(m_QueryService);
		
		m_Loader = new EvidenceViewPagingLoader<DatePagingLoadResult>(proxy);
		m_Loader.setTimeFrame(m_TimeFrame);
		m_Loader.setFilterAttribute(filterAttribute);
		m_Loader.setFilterValue(filterValue);
		m_Loader.setSortDir(Style.SortDir.DESC);
		m_Loader.setRemoteSort(true);
		
    	switch (m_TimeFrame)
    	{
    		case WEEK:
    			m_Loader.setSortField("last_occurred");
    			break;
    			
    		case DAY:
    			m_Loader.setSortField("day");
    			break;
    			
    		case HOUR:
    			m_Loader.setSortField("hour");
    			break;
    			
    		case MINUTE:
    			m_Loader.setSortField("minute");
    			break;
    			
    		case SECOND:
    			m_Loader.setSortField("time");
    			break;
    	}
		
		m_Loader.addLoadListener(new LoadListener(){
		      public void loaderLoad(LoadEvent le) 
		      {
		          onLoad(le);
		      }
		});
		
	}
	
	
	/**
	 * Returns the time frame currently being displayed in the window.
	 * @return time frame of the current list e.g. week, day or hour.
	 */
	public TimeFrame getTimeFrame()
    {
    	return m_TimeFrame;
    }


	/**
	 * Sets the time frame currently being displayed in the window. 
	 * Note that a separate call should be made to reload data into
	 * the window following the call to this method.
	 * @param timeFrame time frame of the current list e.g. week, day or hour.
	 */
	public void setTimeFrame(TimeFrame timeFrame)
    {
		if (timeFrame != m_TimeFrame)
		{			
			m_TimeFrame = timeFrame;
			m_Loader.setTimeFrame(timeFrame);
			
			switch (m_TimeFrame)
			{
	    		case WEEK:
	    			m_Loader.setSortField("last_occurred");
	    			break;
	    			
	    		case DAY:
	    			m_Loader.setSortField("day");
	    			break;
	    			
	    		case HOUR:
	    			m_Loader.setSortField("hour");
	    			break;
	    			
	    		case MINUTE:
	    			m_Loader.setSortField("minute");
	    			break;
	    			
	    		case SECOND:
	    			m_Loader.setSortField("time");
	    			break;
			}
			
			configureGridColumns();
			
			m_Grid.getStore().removeAll();
		}
    }
	
	
	/**
	 * Sets the filter for the evidence data. A separate call should be made to
	 * reload data into the window following the call to this method.
	 * @param filterAttribute 	the name of the attribute on which the evidence
	 * 	should be filtered.	
	 * @param filterValue		the value of the filter on which the evidence
	 * 	should be filtered.
	 */
	public void setFilter(String filterAttribute, String filterValue)
	{
		m_Loader.setFilterAttribute(filterAttribute);
		m_Loader.setFilterValue(filterValue);
	}
	
	
	/**
	 * Refreshes the list of evidence, displaying the first (most recent) page 
	 * of evidence data.
	 */
	public void load()
	{
		m_Loader.load();
	}
	
	
	/**
	 * Loads the list of evidence, the top of row of which will match the 
	 * specified time.
	 * @param date date/time of evidence data to load.
	 */
    public void loadAtTime(Date date)
    {
    	if (date != null)
    	{
    		m_Loader.loadAtTime(date, m_TimeFrame);
    	}
    	else
    	{
    		m_Loader.load();
    	}
    }

	
	/**
	 * Updates the History window when new data is loaded.
	 * @param le LoadEvent received from the load operation.
	 */
	protected void onLoad(LoadEvent le) 
	{
		DatePagingLoadResult<EventRecord> loadResult = le.getData();
		m_Loader.setDate(loadResult.getDate());
		
		if (m_TimeFrame == TimeFrame.SECOND)
		{
			m_Grid.setContextMenu(m_ContextMenu);
		}
		else
		{
			m_Grid.setContextMenu(null);
		}
	}

	
	/**
	 * Configures the columns for the grid.
	 * The method makes a call to the server to obtain the list of columns
	 * from the database table linked to the current time frame. It also
	 * creates custom renderers and listeners for any columns as appropriate
	 * e.g. the 'probable_cause' column.
	 */
	private void configureGridColumns()
	{
		m_QueryService.getAllColumns(m_TimeFrame, new AsyncCallback<List<String>>(){

	        public void onFailure(Throwable caught)
	        {
		        MessageBox.alert("Prelert - Error",
		                "Failed to get a response from the server.", null);
	        }


	        public void onSuccess(List<String> columns)
	        {
	        	List<ColumnConfig> newConfig = new ArrayList<ColumnConfig>();
				
				ColumnConfig columnConf;
				String columnName;
				Iterator<String> colIterator = columns.iterator();
				while (colIterator.hasNext() == true)
				{
					columnName = colIterator.next();
					columnConf = new ColumnConfig(columnName, columnName, 120);
					
					// Disable column sorting.
					columnConf.setSortable(false);
					newConfig.add(columnConf);
					
				}
				ColumnModel newColumnModel = new ColumnModel(newConfig);
				m_Grid.reconfigure(m_Grid.getStore(), newColumnModel);
				m_Grid.unmask();
				
	        }
        });

	}
	
}
