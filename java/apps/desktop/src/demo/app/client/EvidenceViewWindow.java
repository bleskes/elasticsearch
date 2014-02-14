package demo.app.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.data.*;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.grid.*;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.menu.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;

import demo.app.data.*;
import demo.app.service.DatabaseServiceLocator;
import demo.app.service.EvidenceQueryServiceAsync;

/**
 * An extension of the Ext JS GXT Window for a Prelert Desktop Window displaying
 * a list of evidence records. The Window contains a Grid component for paging 
 * through a list of records retrieved from the database, with a toolbar that 
 * allows the users to page by date/time.
 * @author Pete Harverson
 */
public class EvidenceViewWindow extends ViewWindow
{
	private DesktopApp					m_Desktop;
	private EvidenceQueryServiceAsync 	m_QueryService = null;
	
	private String						m_FilterAttribute;
	private String						m_FilterValue;
	
	private Grid<EventRecord>			m_Grid;
	private EvidenceViewPagingLoader<DatePagingLoadResult> 	m_Loader;
	
	
	/**
	 * Constructs a new EvidenceViewWindow.
	 * @param desktop the parent Prelert desktop.
	 */
	public EvidenceViewWindow(DesktopApp desktop)
	{
		this(desktop, null, null);
	}
	
	
	public EvidenceViewWindow(DesktopApp desktop, 
			String filterAttribute, String filterValue)
	{
		m_FilterAttribute = filterAttribute;
		m_FilterValue = filterValue;
		
		m_Desktop = desktop;
		m_QueryService = DatabaseServiceLocator.getInstance().getEvidenceQueryService();
		
		setMinimizable(true);
		setMaximizable(true);
		setHeading("Evidence View");
		setSize(650, 525);
		setLayout(new FitLayout());
		setResizable(true);
		
		setIconStyle("list-sec-win-icon");
		
		initComponents();
	}
	
	
	/**
	 * Creates and initialises the graphical components in the window.
	 */
	protected void initComponents()
	{
		// Create the paging toolbar.
		createPagingToolBar();

		// Create the client-side cache of EventRecord objects displayed in the ListView Grid.
		ListStore<EventRecord> listStore = new ListStore<EventRecord>(m_Loader);
			
		// Create the Grid which displays the data.
		// Default columns at CS:
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
	    
	    ColumnModel columnModel = new ColumnModel(config);
	    m_Grid = new Grid<EventRecord>(listStore, columnModel);
	    m_Grid.setLoadMask(true);
	    m_Grid.setBorders(true);
	    m_Grid.setTrackMouseOver(false);
	    m_Grid.setView(new SeverityGridView(listStore));
	    m_Grid.getSelectionModel().setSelectionMode(Style.SelectionMode.SINGLE);
	    
	    // Configure the columns in the grid.
	    configureListViewColumns();
	    
	    
	    // Set up a right-click context menu and add tools for launching
	    // the History, Probable Cause and Usage windows.
	    Menu menu = new Menu();
	    
	    MenuItem probCauseItem = new MenuItem("Show Probable Cause");
	    menu.add(probCauseItem);
	    probCauseItem.addSelectionListener(new SelectionListener<MenuEvent>() {
	    	@Override
	    	public void componentSelected(MenuEvent ce) 
	    	{
	    		EventRecord selectedRecord = m_Grid.getSelectionModel().getSelectedItem();
	    		m_Desktop.openCausalityWindow(selectedRecord);
	    	}
	    });
	    
	    MenuItem historyItem = new MenuItem("Show History");
	    menu.add(historyItem);
	    historyItem.addSelectionListener(new SelectionListener<MenuEvent>() {
	    	@Override
	    	public void componentSelected(MenuEvent ce) 
	    	{
	    		EventRecord selectedRecord = m_Grid.getSelectionModel().getSelectedItem();
	    		m_Desktop.openHistoryWindow(selectedRecord);
	    	}
	    });
	    
	    MenuItem userUsageItem = new MenuItem("Show User Usage");
	    menu.add(userUsageItem);
	    userUsageItem.addSelectionListener(new SelectionListener<MenuEvent>() {
	    	@Override
	    	public void componentSelected(MenuEvent ce) 
	    	{
	    		EventRecord selectedRecord = m_Grid.getSelectionModel().getSelectedItem();
	    		openUserUsageWindow(selectedRecord);
	    	}
	    });
	    
	    MenuItem serviceUsageItem = new MenuItem("Show Service Usage");
	    menu.add(serviceUsageItem);
	    serviceUsageItem.addSelectionListener(new SelectionListener<MenuEvent>() {
	    	@Override
	    	public void componentSelected(MenuEvent ce) 
	    	{
	    		EventRecord selectedRecord = m_Grid.getSelectionModel().getSelectedItem();
	    		openServiceUsageWindow(selectedRecord);
	    	}
	    });
	    
	    MenuItem infoMenuItem = new MenuItem("Show Info");
	    menu.add(infoMenuItem);
	    infoMenuItem.addSelectionListener(new SelectionListener<MenuEvent>() {
	    	@Override
	    	public void componentSelected(MenuEvent ce) 
	    	{
	    		int rowId = m_Grid.getSelectionModel().getSelectedItem().getId();
	    		openInfoWindow(rowId);
	    	}
	    });
	    
	    m_Grid.setContextMenu(menu);
	    
	    add(m_Grid);
	}
	
	
	/**
	 * Creates the date paging toolbar for navigating through the evidence.
	 */
	protected void createPagingToolBar()
	{
		// Create the RpcProxy and PagingLoader to populate the list.
		GetEvidenceDataRpcProxy<DatePagingLoadResult> proxy = 
			new GetEvidenceDataRpcProxy(m_QueryService);
		
		EvidenceViewPagingToolBar toolBar = new EvidenceViewFilterToolBar(getFilterAttributes());
		
		m_Loader = new EvidenceViewPagingLoader<DatePagingLoadResult>(proxy);
		m_Loader.setTimeFrame(TimeFrame.SECOND);
		m_Loader.setFilterAttribute(m_FilterAttribute);
		m_Loader.setFilterValue(m_FilterValue);
		m_Loader.setSortField("time");
		m_Loader.setSortDir(Style.SortDir.DESC);
		m_Loader.setRemoteSort(true);
		m_Loader.addLoadListener(new LoadListener(){
		      public void loaderLoad(LoadEvent le) 
		      {
		          onLoad(le);
		      }
		});
		toolBar.bind(m_Loader);
		setTopComponent(toolBar);
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
    	m_Loader.loadAtTime(date, TimeFrame.SECOND);
    }

    
    /**
     * Loads the list of evidence, the top of row of which will match the 
	 * specified time and description.
	 * @param date date/time of evidence data to load.
	 * @param description the value of the description column to match on for the
	 * top row of evidence data that will be returned.
     */
    public void loadAtTime(Date date, String description)
    {
    	if (description != null)
    	{
    		m_Loader.loadForDescription(date, description, TimeFrame.SECOND);
    	}
    	else
    	{
    		loadAtTime(date);
    	}
    }
    
    
	/**
	 * Loads the list of evidence, the top of row of which will match the 
	 * specified id.
	 * @param evidenceId id for the top row of evidence data to be loaded.
	 */
    public void loadAtId(int evidenceId)
    {
    	m_Loader.loadAtId(evidenceId, TimeFrame.SECOND);
    }
    
	
	/**
	 * Updates the evidence window when new data is loaded.
	 * @param le LoadEvent received from the load operation.
	 */
	protected void onLoad(LoadEvent le) 
	{
		// TO DO: When a page loads, check to see if a row is to be selected.
		/*
		ListViewLoadResult<EvidenceLog> loadResult = (ListViewLoadResult<EvidenceLog>)(le.data);
		if (loadResult.getSelectedRowIndex() != -1)
		{
			m_Grid.getSelectionModel().select(loadResult.getSelectedRowIndex());
		}
		*/
		
		
		GWT.log("++++ EvidenceViewWindow.onLoad() number of columns: " + m_Grid.getColumnModel().getColumnCount(), null);
		GWT.log("++++ EvidenceViewWindow.onLoad() number of rows: " + m_Grid.getStore().getCount(), null);
		
		DatePagingLoadResult<EventRecord> loadResult = le.getData();
		m_Loader.setDate(loadResult.getDate());
		
		m_Grid.getView().getHeader().refresh();
	}
	
	
	/**
	 * Opens the User Usage window to display the corresponding usage for the
	 * time, source and username in the supplied evidence.
	 * @param evidenceRecord evidence data for which to display the user usage.
	 */
	protected void openUserUsageWindow(EventRecord evidenceRecord)
	{
		Date evidenceTime = ClientUtil.parseTimeField(evidenceRecord, TimeFrame.SECOND);
		String source = evidenceRecord.get("source");
		String user = evidenceRecord.get("username");
		m_Desktop.openUserUsageWindow(source, user, evidenceTime, TimeFrame.HOUR);
	}
	
	
	/**
	 * Opens the Service Usage window to display the corresponding 'serverload' 
	 * usage for the time, source and service in the supplied evidence.
	 * @param evidenceRecord evidence data for which to display the user usage.
	 */
	protected void openServiceUsageWindow(EventRecord evidenceRecord)
	{
		Date evidenceTime = ClientUtil.parseTimeField(evidenceRecord, TimeFrame.SECOND);
		String source = evidenceRecord.get("source");
		String service = evidenceRecord.get("service");
		m_Desktop.openServiceUsageWindow("serverload", source, service, 
				evidenceTime, TimeFrame.HOUR);
	}
	
	
	/**
	 * Loads and opens the 'Show Info' window for the record with the specified
	 * row ID in the given list View.
	 * @param rowId ID of the row in the View for which to create a 'Show Info' window.
	 */
	public void openInfoWindow(int rowId)
	{
		m_QueryService.getRowInfo(rowId, new AsyncCallback<List<GridRowInfo>>(){

	        public void onFailure(Throwable caught)
	        {
		        MessageBox.alert("Prelert - Error",
		                "Failed to get a response from the server.", null);
	        }


	        public void onSuccess(List<GridRowInfo> modelData)
	        {
	    		// Just hardcode list of active attributes.
	    		ArrayList<String> activeAttributes = new ArrayList<String>();
	    		activeAttributes.add("appid");
	    		activeAttributes.add("description");
	    		activeAttributes.add("ipaddress");
	    		activeAttributes.add("node");
	    		activeAttributes.add("service");
	    		activeAttributes.add("source");
	    		activeAttributes.add("username");
	        	
	        	ShowInfoWindow infoWindow = new ShowInfoWindow(m_Desktop);
	        	infoWindow.setModelData(modelData);
	        	infoWindow.setActiveAttributes(activeAttributes);

	    	    m_Desktop.addWindow(infoWindow);
	    	    infoWindow.show();
	        }
		});
	}
	
	
	/**
	 * Configures the columns for the grid in the specified list View.
	 * The method makes a call to the server to obtain the list of columns
	 * from the database table linked to the specified list View. It also
	 * creates custom renderers and listeners for any columns as appropriate
	 * e.g. the 'probable_cause' column.
	 */
	private void configureListViewColumns()
	{
		GWT.log("ooooooooo EvidenceViewWindow.configureListColumns() start", null);
		m_QueryService.getAllColumns(TimeFrame.SECOND, new AsyncCallback<List<String>>(){

	        public void onFailure(Throwable caught)
	        {
		        MessageBox.alert("Prelert - Error",
		                "Failed to get a response from the server.", null);
	        }
	        
	        
	        public void onSuccess(List<String> columns)
	        {
	        	List<ColumnConfig> columnConfig = new ArrayList<ColumnConfig>();
				
				ColumnConfig columnConf;
				String columnName;
				Iterator<String> colIterator = columns.iterator();
				while (colIterator.hasNext() == true)
				{
					columnName = colIterator.next();
					columnConf = new ColumnConfig(columnName, columnName, 120);
					columnConf.setHidden(false);
					
					// Disable column sorting.
					columnConf.setSortable(false);
					columnConfig.add(columnConf);
				}
				ColumnModel newColumnModel = new ColumnModel(columnConfig);
				m_Grid.reconfigure(m_Grid.getStore(), newColumnModel);
				m_Grid.unmask();


				GWT.log("EvidenceViewWindow.configureListColumns() completed, grid rows: " + m_Grid.getStore().getCount(), null);
	        }
        });

	}
	
	
	/**
	 * Returns the list of attributes that the evidence list can be filtered on.
	 * @return list of filter attributes.
	 */
	private List<String> getFilterAttributes()
	{
		// Just hardcode the list of filter attributes for now.
		ArrayList<String> attributes = new ArrayList<String>();
		attributes.add("appid");
		attributes.add("description");
		attributes.add("ipaddress");
		attributes.add("node");
		attributes.add("service");
		attributes.add("severity");
		attributes.add("source");
		attributes.add("username");
		
		return attributes;
	}

}
