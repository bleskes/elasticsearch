/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ***********************************************************/

package demo.app.client.list;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.Style.SortDir;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.store.GroupingStore;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.store.Store;
import com.extjs.gxt.ui.client.store.StoreSorter;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.grid.GridGroupRenderer;
import com.extjs.gxt.ui.client.widget.grid.GroupColumnData;
import com.extjs.gxt.ui.client.widget.grid.GroupingView;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.treegrid.TreeGridCellRenderer;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;

import demo.app.client.ApplicationResponseHandler;
import demo.app.client.CausalityViewWindow;
import demo.app.client.ClientUtil;
import demo.app.client.DesktopApp;
import demo.app.client.ProgressBarCellRenderer;
import demo.app.data.CausalityView;
import demo.app.data.TimeFrame;
import demo.app.data.Tool;
import demo.app.data.gxt.EvidenceModel;
import demo.app.data.gxt.GridRowInfo;
import demo.app.data.gxt.ProbableCauseModel;
import demo.app.service.DatabaseServiceLocator;
import demo.app.service.EvidenceQueryServiceAsync;


/**
 * An extension of the Ext JS GXT Window for a Prelert Probable Causes list.
 * The Window contains a grid which lists the probable causes for an item of
 * evidence or a discord in time series data.
 * 
 * @author Pete Harverson
 */
public class ProbableCausesListWindow extends CausalityViewWindow
{
	private DesktopApp					m_Desktop;
	private EvidenceQueryServiceAsync 	m_EvidenceQueryService;
	
	private Grid<ProbableCauseModel> 	m_Grid;

	
	/**
	 * Creates a new window for displaying causally related notifications and
	 * time series discords in a grid.
	 */
	public ProbableCausesListWindow(DesktopApp desktop, CausalityView causalityView)
	{
		super(causalityView);
		
		m_Desktop = desktop;
		m_EvidenceQueryService = DatabaseServiceLocator.getInstance().getEvidenceQueryService();
		
		setSize(800, 400);
		setLayout(new FitLayout());
		
		initComponents();
	}
	
	
	/**
	 * Creates and initialises the graphical components in the window.
	 */
	protected void initComponents()
	{
		// Group the Grid store by data source type.
		GroupingStore<ProbableCauseModel> store = new GroupingStore<ProbableCauseModel>();   
		store.groupBy("dataSourceName");
		
		// Default search field of significance.
		store.setSortField("significance");
		store.setSortDir(SortDir.DESC);
		store.setStoreSorter(new ProbableCauseStoreSorter());
		
		ProbableCauseListCellRenderer cellRenderer = new ProbableCauseListCellRenderer();
		
	    ColumnConfig timeColumn = new ColumnConfig("time", "time", 120);
	    timeColumn.setRenderer(cellRenderer);
	    ColumnConfig typeColumn = new ColumnConfig("dataSourceName", "type", 100);
	    typeColumn.setRenderer(cellRenderer);
	    ColumnConfig descColumn = new ColumnConfig("description", "description", 170);
	    descColumn.setRenderer(cellRenderer);
	    ColumnConfig sourceColumn = new ColumnConfig("source", "source", 110);
	    sourceColumn.setRenderer(cellRenderer);
	    ColumnConfig attributeColumn = new ColumnConfig("attributeLabel", "attributes", 170);
	    attributeColumn.setRenderer(cellRenderer);
	    
	    ColumnConfig significanceColumn = new ColumnConfig("significance", "significance", 100);
	    significanceColumn.setRenderer(ProgressBarCellRenderer.getInstance());

	    List<ColumnConfig> config = new ArrayList<ColumnConfig>();
	    config.add(typeColumn);
	    config.add(timeColumn);
	    config.add(descColumn);
	    config.add(sourceColumn);
	    config.add(attributeColumn);
	    config.add(significanceColumn);

	    final ColumnModel columnModel = new ColumnModel(config);
	    
	    GroupingView typeGroupingView = new GroupingView();   
	    typeGroupingView.setShowGroupedColumn(false);   
	    typeGroupingView.setForceFit(true);   
	    typeGroupingView.setGroupRenderer(new GridGroupRenderer()
		{
			public String render(GroupColumnData data)
			{
				String typeHeader = columnModel.getColumnById(data.field).getHeader();
				String itemsLabel = data.models.size() == 1 ? "Item" : "Items";
				return typeHeader + ": " + data.group + " (" + data.models.size() + 
							" " + itemsLabel + ")";
			}
		}); 
		
	    m_Grid = new Grid<ProbableCauseModel>(store, columnModel);
	    m_Grid.setView(typeGroupingView);
	    m_Grid.setLoadMask(true);
	    m_Grid.setBorders(true);
	    
	    m_Grid.addListener(Events.CellClick, new Listener<GridEvent<ProbableCauseModel>>(){
	 		public void handleEvent(GridEvent<ProbableCauseModel> event) 
	 		{
	 			if(event.getTarget(".probcauseListLink", 1) != null)
	 			{
	 				showProbableCause(event.getModel());
	 			}
	 		} 
	 	});

	    add(m_Grid);
	}
	
	
	/**
	 * Loads the list of probable causes into the window.
	 */
    public void load()
	{
    	EvidenceModel evidence = getEvidence();
		
		if (evidence != null)
		{
			m_Grid.getStore().removeAll();
			
			// Load the episodes and their layout positions for the chart.
			ApplicationResponseHandler<List<ProbableCauseModel>> callback = 
				new ApplicationResponseHandler<List<ProbableCauseModel>>()
			{
				public void uponFailure(Throwable caught)
				{
					MessageBox.alert("Prelert - Error",
			                "Error retrieving probable cause data from server (" + 
			                caught.getClass().getName() + ")", null);
				}
	
	
				public void uponSuccess(List<ProbableCauseModel> probableCauses)
				{	
					ListStore<ProbableCauseModel> store = m_Grid.getStore();
					store.add(probableCauses);	
				}
			};
			
			// Time span not used - just pass 60 seconds as a dummy value.
			m_CausalityQueryService.getProbableCauses(evidence.getId(), 60, callback);

		}
	}
    
    
    /**
     * Opens a window to display details for the specified probable cause.
     * @param probableCause probable cause to display.
     */
    protected void showProbableCause(ProbableCauseModel probableCause)
    {
    	switch (probableCause.getDataSourceCategory())
    	{
    		case NOTIFICATION:
    			showNotificationProbableCause(probableCause);
    			break;
    			
    		case TIME_SERIES:
    			showTimeSeriesProbableCause(probableCause);
    			break;
    	}
    }
    
    
    /**
     * Opens a window to display details for the specified notification type
     * probable cause.
     * @param probableCause notification probable cause to display.
     */
    protected void showNotificationProbableCause(ProbableCauseModel probableCause)
    {
    	final ProbableCauseModel notificationCause = probableCause;
    	String evidenceIdAttr = probableCause.getAttributeValue();
    	
    	if (evidenceIdAttr != null)
    	{
    		int evidenceIdVal = 0;
    		try
    		{
    			evidenceIdVal = ClientUtil.parseInteger(evidenceIdAttr);
    		}
    		catch (NumberFormatException e)
    		{
    			MessageBox.alert("Prelert - Error",
		                "Probable Cause does not have a valid id.", null);
    			return;
    		}
    		
    		final int evidenceId = evidenceIdVal;
    		
	    	m_EvidenceQueryService.getRowInfo(evidenceId, 
	    			new ApplicationResponseHandler<List<GridRowInfo>>(){
	
		        public void uponFailure(Throwable caught)
		        {
			        MessageBox.alert("Prelert - Error",
			                "Error retrieving evidence info.", null);
		        }
	
	
		        public void uponSuccess(List<GridRowInfo> modelData)
		        { 	
		        	String windowTitle = notificationCause.getDataSourceName();
		        	windowTitle+= " id ";
		        	windowTitle+= evidenceId;
		        	m_Desktop.openShowInfoWindow(windowTitle, modelData);
		        }
			});
    	
    	}

    }
    
    
    /**
     * Opens a window to display details for the specified time series type 
     * probable cause.
     * @param probableCause time series probable cause to display.
     */
    protected void showTimeSeriesProbableCause(ProbableCauseModel probableCause)
    {
    	m_Desktop.openUsageWindowForType(
				probableCause.getDataSourceName(), 
				probableCause.getMetric(), 
				probableCause.getSource(), 
				probableCause.getAttributeName(),
				probableCause.getAttributeValue(), 
				probableCause.getTime(), 
				TimeFrame.HOUR);
    }


	/**
	 * Runs a tool against the currently selected probable cause.
	 * @param tool the tool that has been run by the user.
	 */
    public void runTool(Tool tool)
	{
		// No tool functionality enabled.
	}
	
	
	/**
	 * Creates some test data to display in the Probable Causes list.
	 */
	private ListStore<BaseModelData> getTestData()
	{
		// time (30s interval near congestion message) | usage_mdhmon | significant increase |  lnl06m-8102 | PRISM_LSE 
		// time (30s interval near congestion message) | usage_mdhmon | significant drop |  sol06m-8102 | PRISM_LSE 
		// time | p2pslog | Transport read error |  sol06m-8102 
		// time (30s interval) | UDP sent | significant increase | sol06m-8102 
		// time (30s interval) | UDP recv | significant increase | sol06m-8102 
		// time (30s interval) | UDP errors | significant increase | sol06m-8102 
		// time (5min interval) | TcpRcvCollapsed | significant increase |sol06m-8102
		
		BaseModelData cause1 = new BaseModelData();
		cause1.set("time", "2009-07-06 20:58:00-20:58:30");
		cause1.set("type", "usage_mdhmon");
		cause1.set("description", "significant increase");
		cause1.set("source", "lnl06m-8102");
		cause1.set("service", "PRISM_LSE");
		cause1.set("significance", 100);
		
		BaseModelData cause2 = new BaseModelData();
		cause2.set("time", "2009-07-06 20:58:00-20:58:30");
		cause2.set("type", "usage_mdhmon");
		cause2.set("description", "significant drop");
		cause2.set("source", "sol06m-8102");
		cause2.set("service", "PRISM_LSE");
		cause2.set("significance", 98);
		
		BaseModelData cause3 = new BaseModelData();
		cause3.set("time", "2009-07-06 20:58:05");
		cause3.set("type", "p2pslog");
		cause3.set("description", "Transport read error");
		cause3.set("source", "sol06m-8102");
		cause3.set("significance", 95);
		
		BaseModelData cause4 = new BaseModelData();
		cause4.set("time", "2009-07-06 20:57:45-20:58:15");
		cause4.set("type", "UDP sent");
		cause4.set("description", "significant increase");
		cause4.set("source", "sol06m-8102");
		cause4.set("significance", 93);
		
		BaseModelData cause5 = new BaseModelData();
		cause5.set("time", "2009-07-06 20:57:45-20:58:15");
		cause5.set("type", "UDP recv");
		cause5.set("description", "significant increase");
		cause5.set("source", "sol06m-8102");
		cause5.set("significance", 90);
		
		BaseModelData cause6 = new BaseModelData();
		cause6.set("time", "2009-07-06 20:57:45-20:58:15");
		cause6.set("type", "UDP errors");
		cause6.set("description", "significant increase");
		cause6.set("source", "sol06m-8102");
		cause6.set("significance", 82);
		
		BaseModelData cause7 = new BaseModelData();
		cause7.set("time", "2009-07-06 20:55:00-21:00:00");
		cause7.set("type", "TcpRcvCollapsed");
		cause7.set("description", "significant increase");
		cause7.set("source", "sol06m-8102");
		cause7.set("significance", 80);
		
		
		ListStore<BaseModelData> listStore = new ListStore<BaseModelData>();
		listStore.add(cause1);
		listStore.add(cause2);
		listStore.add(cause3);
		listStore.add(cause4);
		listStore.add(cause5);
		listStore.add(cause6);
		listStore.add(cause7);
		
		return listStore;
		
	}
	
	
	/**
	 * Custom GridCellRenderer which adds a hyperlink around the cell value.
	 */
	class ProbableCauseListCellRenderer implements GridCellRenderer<ProbableCauseModel>
	{
        public Object render(ProbableCauseModel model, String property,
                ColumnData config, int rowIndex, int colIndex,
                ListStore<ProbableCauseModel> store, Grid<ProbableCauseModel> grid)
        {
        	if (model.get(property) != null)
    		{
        		if (property != "time")
        		{
        			return "<a class=\"probcauseListLink cursor-pointer\" >"  + model.get(property) + "</a>";  
        		}
        		else
        		{
        			// Format time value into appropriate format for GUI.
        			Date dateTime = model.getTime();
        			return "<a class=\"probcauseListLink cursor-pointer\" >"  + 
        				ClientUtil.formatTimeField(dateTime, TimeFrame.SECOND) + "</a>"; 
        		}
    		}
        	else
        	{
        		return("");
        	}
        }

	}
	
	
	/**
	 * Custom GridCellRenderer for the attribute column which displays the attribute
	 * name and value in a single column.
	 */
	class AttributeCellRenderer implements GridCellRenderer<ProbableCauseModel>
	{

        public Object render(ProbableCauseModel model, String property,
                ColumnData config, int rowIndex, int colIndex,
                ListStore<ProbableCauseModel> store, Grid<ProbableCauseModel> grid)
        {
        	String attributeName = model.getAttributeName();
        	String attributeValue = model.getAttributeValue();
        	
        	if (attributeName != null && attributeValue != null)
    		{
        		return "<a class=\"probcauseListLink cursor-pointer\" >"  + 
        					attributeName + ":" + attributeValue + "</a>";  
    		}
        	else
        	{
        		return("");
        	}
        }
		
	}
}
