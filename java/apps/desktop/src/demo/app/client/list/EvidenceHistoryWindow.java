package demo.app.client.list;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.Listener;
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

import demo.app.client.ClientUtil;
import demo.app.client.DesktopApp;
import demo.app.client.ProbableCauseRenderer;
import demo.app.client.ViewMenuItem;
import demo.app.client.ViewWindow;
import demo.app.client.event.ViewMenuItemListener;
import demo.app.data.CausalityViewTool;
import demo.app.data.DatePagingLoadConfig;
import demo.app.data.DatePagingLoadResult;
import demo.app.data.EvidencePagingLoadConfig;
import demo.app.data.EvidenceView;
import demo.app.data.EvidenceViewPagingLoader;
import demo.app.data.HistoryView;
import demo.app.data.ListViewTool;
import demo.app.data.TimeFrame;
import demo.app.data.Tool;
import demo.app.data.UsageViewTool;
import demo.app.data.gxt.EvidenceModel;
import demo.app.service.DatabaseServiceLocator;
import demo.app.service.EvidenceQueryServiceAsync;
import demo.app.service.GetEvidenceDataRpcProxy;


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
	private HistoryView					m_HistoryView;
	
	private EvidenceQueryServiceAsync 	m_QueryService;
	
	private TimeFrame					m_TimeFrame;
	
	private Grid<EvidenceModel>			m_Grid;
	private Menu						m_ContextMenu;
	private EvidenceViewPagingLoader<DatePagingLoadResult> m_Loader;

	
	/**
	 * Constructs an empty Evidence History window.
	 * @param desktop the parent Prelert desktop.
	 * @param historyView the History View to display in the window.
	 */
	public EvidenceHistoryWindow(DesktopApp desktop, TimeFrame timeFrame, 
			HistoryView historyView)
	{
		this(desktop, timeFrame, historyView, null, null);
	}
	
	
	/**
	 * Constructs an empty Evidence History window to display occurrences of
	 * evidence matching the specified filter.
	 * @param desktop the parent Prelert desktop.
	 * @param historyView the History View to display in the window.
	 * @param filterAttribute 	the name of the attribute on which the evidence
	 * 	should be filtered.	
	 * @param filterValue		the value of the filter on which the evidence
	 * 	should be filtered.
	 * @param desktop the parent Prelert desktop.
	 */
	public EvidenceHistoryWindow(DesktopApp desktop, TimeFrame timeFrame, 
			HistoryView historyView, String filterAttribute, String filterValue)
	{
		m_Desktop = desktop;
		m_HistoryView = historyView;
		m_TimeFrame = timeFrame;
		
		m_QueryService = DatabaseServiceLocator.getInstance().getEvidenceQueryService();
		
		setMinimizable(true);
		setMaximizable(true);
		setSize(650, 525);
		setLayout(new FitLayout());
		setResizable(true);
		
		setHeading(m_HistoryView.getName());
		
		if (m_HistoryView.getStyleId() != null)
	    {
			setIconStyle(m_HistoryView.getStyleId()+ "-win-icon");
	    }
	    else
	    {
	    	setIconStyle("list-evidence-win-icon");
	    }
		
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
		ListStore<EvidenceModel> listStore = new ListStore<EvidenceModel>(m_Loader);
			
		// Create the Grid which displays the data.
	    ColumnModel columnModel = buildColumnModel(m_TimeFrame);
	    m_Grid = new Grid<EvidenceModel>(listStore, columnModel);
	    m_Grid.setLoadMask(true);
	    m_Grid.setBorders(true);
	    m_Grid.setTrackMouseOver(false);
	    m_Grid.setView(new SeverityGridView(listStore));
	    m_Grid.getSelectionModel().setSelectionMode(Style.SelectionMode.SINGLE);
	    
	    // Listen for clicks on a Probable Cause icon.
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
	    
	    
	    // Configure the right-click context menu.
	    // Currently tools only run off the SECOND time frame.
	    m_ContextMenu = new Menu();
	    EvidenceView secondView = m_HistoryView.getEvidenceView(TimeFrame.SECOND);
	    if (secondView != null)
	    {
		    List<Tool> viewMenuItems = secondView.getContextMenuItems();
		    if (viewMenuItems != null)
		    {
		    	ViewMenuItem menuItem;
		    	ViewMenuItemListener<MenuEvent> menuItemLsnr;
		    	
		    	for (int i = 0; i < viewMenuItems.size(); i++)
		    	{
		    	    menuItem = new ViewMenuItem(viewMenuItems.get(i));
		    	    menuItemLsnr = new ViewMenuItemListener<MenuEvent>(menuItem, this);
		    	    menuItem.addSelectionListener(menuItemLsnr);
		    	    m_ContextMenu.add(menuItem);
		    	}
		    }
	    }
	    
	    m_Grid.setContextMenu(m_ContextMenu);
	    
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
	 * Returns the View displayed in the Window.
	 * @return the list view displayed in the Window.
	 */
	@Override
    public HistoryView getView()
	{
		return m_HistoryView;
	}
	
	
	/**
	 * Returns the data type of the evidence that will be loaded into the
	 * History Window.
	 * @return the evidence data type e.g. p2pslog, mdhlog.
	 */
	public String getDataType()
	{
		return m_Loader.getDataType();
	}
	
	
	/**
	 * Sets the data type of the evidence that will be loaded into the
	 * History Window.
	 * @param dataType the evidence data type e.g. p2pslog, mdhlog.
	 */
	public void setDataType(String dataType)
	{
		m_Loader.setDataType(dataType);
	}
	
	
	/**
	 * Sets the evidence view that will be displayed for the SECOND time frame.
	 * This controls what columns will be shown in the grid for the SECOND view.
	 * @param evidenceView the evidence view for the SECOND time frame.
	 */
	public void setEvidenceSecondView(EvidenceView evidenceView)
	{
		m_HistoryView.addEvidenceView(evidenceView);
		
		// Empty the grid and reconfigure the grid columns for the new View.
		if (m_TimeFrame == TimeFrame.SECOND)
		{
			m_Grid.getStore().removeAll();
			ColumnModel newColumnModel = buildColumnModel(m_TimeFrame);
			m_Grid.reconfigure(m_Grid.getStore(), newColumnModel);
			m_Grid.unmask();
		}
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
			
			// Empty the grid and reconfigure the grid columns.
			m_Grid.getStore().removeAll();
			ColumnModel newColumnModel = buildColumnModel(m_TimeFrame);
			m_Grid.reconfigure(m_Grid.getStore(), newColumnModel);
			m_Grid.unmask();
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
		DatePagingLoadResult<EvidenceModel> loadResult = le.getData();
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
	 * Runs a tool against the selected row of evidence. No action is taken if
	 * no row is currently selected.
	 * @param tool the tool that has been run by the user.
	 */
	public void runTool(Tool tool)
	{
		// Note that GWT does not support reflection, so need to use getClass().
		if (tool.getClass() == ListViewTool.class)
		{
			runListViewTool((ListViewTool)tool);
		}
		else if (tool.getClass() == CausalityViewTool.class)
		{
			runCausalityViewTool((CausalityViewTool)tool);
		}
		else if (tool.getClass() == UsageViewTool.class)
		{
			runUsageViewTool((UsageViewTool)tool);
		}
	}
	
	
	/**
	 * Runs a tool against the hovered over event record to open a List View.
	 * Currently only the opening of the Evidence View is supported, showing
	 * evidence at the time of the selected record.
	 * @param tool the ViewTool that has been run by the user.
	 */
	public void runListViewTool(ListViewTool tool)
	{
		// Get the selected row. If no row is selected, do nothing.
		EvidenceModel selectedRecord = m_Grid.getSelectionModel().getSelectedItem();
		if (selectedRecord != null)
		{
			m_Desktop.openEvidenceWindowForType(getDataType(), selectedRecord.getId());
		}
	}
	
	
	/**
	 * Runs a tool against the selected row of evidence to launch the Causality View.
	 * No action is taken if no row is currently selected.
	 * @param tool the HistoryViewTool that has been run by the user.
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
			Date date = ClientUtil.parseTimeField(selectedRow, m_TimeFrame);
			
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
			
			m_Desktop.openUsageWindow(tool.getViewToOpen(), tool.getMetric(), source,
				tool.getAttributeName(), attributeValue, date, tool.getTimeFrame());
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
		m_QueryService.getAllColumns(getDataType(), m_TimeFrame, 
				new AsyncCallback<List<String>>(){

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
	
	
	/**
	 * Builds the column model to use for the grid for the specified time frame.
	 * @param timeFrame time frame for the grid columns e.g. DAY, HOUR, MINUTE, SECOND.
	 * @return the column model for the supplied time frame.
	 */
	protected ColumnModel buildColumnModel(TimeFrame timeFrame)
	{
		EvidenceView evidenceView = m_HistoryView.getEvidenceView(timeFrame);
		List<String> allColumns = evidenceView.getColumns();
		
    	List<ColumnConfig> newConfig = new ArrayList<ColumnConfig>();
		
		ColumnConfig columnConf;
		for (String columnName : allColumns)
		{
			columnConf = new ColumnConfig(columnName, columnName, 120);
			
		    // Add a custom renderer for the Probable Cause column.
		    if (columnName.equals("probable_cause") )
			{
		    	columnConf.setWidth(100);
		    	columnConf.setRenderer(ProbableCauseRenderer.getInstance());
			}
			
			// Disable column sorting.
			columnConf.setSortable(false);
			newConfig.add(columnConf);
			
		}
		
		return new ColumnModel(newConfig);
	}
	
}
