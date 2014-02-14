package demo.app.client.list;

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

import demo.app.client.ClientUtil;
import demo.app.client.DesktopApp;
import demo.app.client.ProbableCauseRenderer;
import demo.app.client.ShowInfoWindow;
import demo.app.client.ViewMenuItem;
import demo.app.client.ViewWindow;
import demo.app.client.event.ViewMenuItemListener;
import demo.app.data.*;
import demo.app.data.gxt.EvidenceModel;
import demo.app.data.gxt.GridRowInfo;
import demo.app.service.DatabaseServiceLocator;
import demo.app.service.EvidenceQueryServiceAsync;
import demo.app.service.GetEvidenceDataRpcProxy;

/**
 * An extension of the Ext JS GXT Window for a Prelert Desktop Window displaying
 * a list of evidence records. The Window contains a Grid component for paging 
 * through a list of records retrieved from the database, with a toolbar that 
 * allows the users to page by date/time.
 * 
 * @author Pete Harverson
 */
public class EvidenceViewWindow extends ViewWindow
{
	private DesktopApp					m_Desktop;
	private EvidenceQueryServiceAsync 	m_QueryService = null;
	
	private EvidenceView				m_EvidenceView;
	
	private Grid<EvidenceModel>			m_Grid;
	private EvidenceViewPagingLoader<DatePagingLoadResult<EvidenceModel>> 	m_Loader;
	
	
	
	/**
	 * Constructs a new EvidenceViewWindow.
	 * @param desktop the parent Prelert desktop.
	 */
	public EvidenceViewWindow(DesktopApp desktop)
	{	
		m_Desktop = desktop;
		m_QueryService = DatabaseServiceLocator.getInstance().getEvidenceQueryService();
		
		setMinimizable(true);
		setMaximizable(true);
		setHeading("Evidence View");
		setSize(650, 525);
		setLayout(new FitLayout());
		setResizable(true);
		
		setIconStyle("list-sec-win-icon");
	}
	
	
	public EvidenceViewWindow(DesktopApp desktop, EvidenceView evidenceView)
	{
		this(desktop);
		
		m_EvidenceView = evidenceView;
		initViewParameters(evidenceView);
		
		// Create the paging toolbar.
		createPagingToolBar();

		// Create the client-side cache of EventRecord objects 
		// displayed in the EvidenceView Grid.
		ListStore<EvidenceModel> listStore = new ListStore<EvidenceModel>(m_Loader);
			
		// Create the Grid which displays the data.
	    List<ColumnConfig> config = new ArrayList<ColumnConfig>();
	    List<String> allColumns = m_EvidenceView.getColumns();
	    ColumnConfig columnConf;
	    for (String columnName : allColumns)
		{
			columnConf = new ColumnConfig(columnName, columnName, 120);
			
			// Disable column sorting.
			columnConf.setSortable(false);
			config.add(columnConf);
		}
	    
	    ColumnModel columnModel = new ColumnModel(config);
	    m_Grid = new Grid<EvidenceModel>(listStore, columnModel);
	    m_Grid.setLoadMask(true);
	    m_Grid.setBorders(true);
	    m_Grid.setTrackMouseOver(false);
	    m_Grid.setView(new SeverityGridView(listStore));
	    m_Grid.getSelectionModel().setSelectionMode(Style.SelectionMode.SINGLE); 
	    
	    // Add a custom renderer for the Probable Cause column.
	    ColumnConfig probCauseCol = m_Grid.getColumnModel().getColumnById("probable_cause");
	    if (probCauseCol != null)
		{
	    	probCauseCol.setWidth(100);
	    	probCauseCol.setRenderer(ProbableCauseRenderer.getInstance());

		 	m_Grid.addListener(Events.CellClick, new Listener<GridEvent<EvidenceModel>>(){
		 		public void handleEvent(GridEvent<EvidenceModel> event) 
		 		{
		 			if(event.getTarget(".probcause", 1) != null)
		 			{
		 				EvidenceModel selectedRec = event.getModel();
		 				m_Desktop.openCausalityWindow(selectedRec);
		 			}
		 		} 
		 	});
		}
	    
	    
	    // Configure the right-click context menu.
	    Menu menu = new Menu();
	    List<Tool> viewMenuItems = m_EvidenceView.getContextMenuItems();
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
	    	    
	    	    // Check if this menu item's tool should be run on double-click.
	    	    if (m_EvidenceView.getDoubleClickTool().equals(menuItem.getTool().getName()))
	    	    {
	    	    	m_Grid.addListener(Events.CellDoubleClick, menuItemLsnr);
	    	    }
	    	}
	    }
	    
	    if (menu.getItemCount() > 0)
	    {
	    	m_Grid.setContextMenu(menu);
	    }
	    
	    add(m_Grid);
	}
	
	
	/**
	 * Initialises the display parameters for the View (window heading
	 * and icon style).
	 * @param view the View to be displayed in the Window.
	 */
	protected void initViewParameters(EvidenceView view)
	{
		setHeading(m_EvidenceView.getName());
		
		if (m_EvidenceView.getStyleId() != null)
	    {
			setIconStyle(m_EvidenceView.getStyleId()+ "-win-icon");
	    }
	    else
	    {
	    	setIconStyle("list-evidence-win-icon");
	    }
	}
	
	
	/**
	 * Creates the date paging toolbar for navigating through the evidence.
	 */
	protected void createPagingToolBar()
	{
		// Create the RpcProxy and PagingLoader to populate the list.
		GetEvidenceDataRpcProxy<DatePagingLoadResult<EvidenceModel>> proxy = 
			new GetEvidenceDataRpcProxy(m_QueryService);
		
		EvidenceViewPagingToolBar toolBar;
		if (m_EvidenceView.getFilterableAttributes() != null &&
				m_EvidenceView.getFilterableAttributes().size() > 0)
		{
			toolBar = new EvidenceViewFilterToolBar(m_EvidenceView);
		}
		else
		{
			toolBar = new EvidenceViewPagingToolBar(
					m_EvidenceView.getDataType(), m_EvidenceView.getTimeFrame());
		}
		
		m_Loader = new EvidenceViewPagingLoader<DatePagingLoadResult<EvidenceModel>>(proxy);
		m_Loader.setTimeFrame(TimeFrame.SECOND);
		m_Loader.setDataType(m_EvidenceView.getDataType());
		m_Loader.setFilterAttribute(m_EvidenceView.getFilterAttribute());
		m_Loader.setFilterValue(m_EvidenceView.getFilterValue());
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
	 * Returns the View displayed in the Window.
	 * @return the list view displayed in the Window.
	 */
	@Override
    public EvidenceView getView()
	{
		return m_EvidenceView;
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
		
		DatePagingLoadResult<EvidenceModel> loadResult = le.getData();
		m_Loader.setDate(loadResult.getDate());
		
		m_Grid.getView().getHeader().refresh();
	}
	
	
	/**
	 * Runs a tool against the selected row of evidence. No action is taken if
	 * no row is currently selected.
	 * @param tool the tool that has been run by the user.
	 */
	public void runTool(Tool tool)
	{
		// Note that GWT does not support reflection, so need to use getClass().
		if (tool.getClass() == CausalityViewTool.class)
		{
			runCausalityViewTool((CausalityViewTool)tool);
		}
		else if (tool.getClass() == HistoryViewTool.class)
		{
			runHistoryViewTool((HistoryViewTool)tool);
		}
		else if (tool.getClass() == ListViewTool.class)
		{
			runListViewTool((ListViewTool)tool);
		}
		else if (tool.getClass() == UsageViewTool.class)
		{
			runUsageViewTool((UsageViewTool)tool);
		}
	}
	
	
	/**
	 * Runs a tool against the selected row of evidence to launch the Causality View.
	 * No action is taken if no row is currently selected.
	 * @param tool the CausalityViewTool that has been run by the user.
	 */
	public void runCausalityViewTool(CausalityViewTool tool)
	{
		// Get the selected row. If no row is selected, do nothing.
		EvidenceModel selectedRow = m_Grid.getSelectionModel().getSelectedItem();
		if (selectedRow != null)
		{
			m_Desktop.openCausalityWindow(selectedRow);
		}
	}
	
	
	/**
	 * Runs a tool against the selected row of evidence to launch the History View.
	 * No action is taken if no row is currently selected.
	 * @param tool the HistoryViewTool that has been run by the user.
	 */
	public void runHistoryViewTool(HistoryViewTool tool)
	{
		// Get the selected row. If no row is selected, do nothing.
		EvidenceModel selectedRow = m_Grid.getSelectionModel().getSelectedItem();
		if (selectedRow != null)
		{
			Date date = ClientUtil.parseTimeField(
					selectedRow, m_EvidenceView.getTimeFrame());
			m_Desktop.openHistoryWindow(m_EvidenceView.getDataType(),
					tool.getTimeFrame(), selectedRow.getDescription(), date);
		}
	}
	
	
	/**
	 * Runs a tool against the hovered over event record to open a list of
	 * evidence of a different data type. The other evidence window will open
	 * to display notifications at the time of the currently selected item of
	 * evidence.
	 * @param tool the ListViewTool that has been run by the user.
	 */
	public void runListViewTool(ListViewTool tool)
	{
		// Get the selected row. If no row is selected, do nothing.
		EvidenceModel selectedRow = m_Grid.getSelectionModel().getSelectedItem();
		if (selectedRow != null)
		{
			// Make sure the tool is for a different data type.
			String viewToOpen = tool.getViewToOpen();
			if (viewToOpen.equals(m_EvidenceView.getName()) == false)
			{
				Date date = ClientUtil.parseTimeField(
						selectedRow, m_EvidenceView.getTimeFrame());
				
				m_Desktop.openEvidenceWindow(viewToOpen, date, null);
			}
		}
	}
	
	
	/**
	 * Runs a View Tool against the selected row of evidence to open a Usage View.
	 * No action is taken if no row is currently selected.
	 * @param tool the UsageViewTool that has been run by the user.
	 */
	public void runUsageViewTool(UsageViewTool tool)
	{
		// Get the selected row. If no row is selected, do nothing.
		EvidenceModel selectedRow = m_Grid.getSelectionModel().getSelectedItem();
		if (selectedRow != null)
		{
			Date date = ClientUtil.parseTimeField(
					selectedRow, m_EvidenceView.getTimeFrame());
			
			// For the metric, look for a metric specified in the tool,
			// or a metric property in the selected row.
			String metric = tool.getMetric();
			if (metric == null)
			{
				metric = selectedRow.get("metric");
			}
			
			String source = null;
			if (tool.getSourceArg() != null)
			{
				source = selectedRow.get(tool.getSourceArg());
			}
			String attributeValue = null;
			if (tool.getAttributeValueArg() != null)
			{
				attributeValue = selectedRow.get(tool.getAttributeValueArg());
			}
			
			m_Desktop.openUsageWindow(tool.getViewToOpen(), metric, source,
					tool.getAttributeName(), attributeValue, date, tool.getTimeFrame());
		}
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
		m_QueryService.getAllColumns(m_EvidenceView.getDataType(), TimeFrame.SECOND, 
				new AsyncCallback<List<String>>(){

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
					
					// Disable column sorting.
					columnConf.setSortable(false);
					columnConfig.add(columnConf);
				}
				
				final ColumnModel newColumnModel = new ColumnModel(columnConfig);
				
				
				DeferredCommand.addCommand(new Command()
				{
					public void execute()
					{
						m_Grid.reconfigure(m_Grid.getStore(), newColumnModel);
						m_Grid.setHideHeaders(false);
						m_Grid.unmask();
					}
				});
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
