/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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

package com.prelert.dev.activity.client;

import java.util.Arrays;
import java.util.List;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.data.BaseTreeModel;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.store.TreeStore;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.SimpleComboBox;
import com.extjs.gxt.ui.client.widget.form.SimpleComboValue;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.tips.QuickTip;
import com.extjs.gxt.ui.client.widget.toolbar.LabelToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.extjs.gxt.ui.client.widget.treegrid.TreeGrid;
import com.extjs.gxt.ui.client.widget.treegrid.TreeGridCellRenderer;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.data.gxt.ActivityTreeModel;

import static com.prelert.data.PropertyNames.*;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.IncidentQueryServiceAsync;


/**
 * Dialog which displays the analysis of an activity via an attribute tree.
 * @author Pete Harverson
 */
public class AnalysisTreeDialog extends Dialog
{
	private static AnalysisTreeDialog	s_Instance;
	
	private IncidentQueryServiceAsync	m_IncidentQueryService;
	
	private int							m_EvidenceId;
	private List<String>				m_TreeAttributeNames;
	
	private TreeStore<BaseTreeModel> 	m_TreeStore;
	private SimpleComboBox<String>		m_GroupByCombo;
	private ListStore<BaseTreeModel>	m_ListStore;
	
	private String m_FolderClosedIconHtml = AbstractImagePrototype.create(
			ClientUtil.CLIENT_IMAGES.icon_folder_closed()).getHTML();
	private String m_TimeSeriesIconHtml = AbstractImagePrototype.create(
			ClientUtil.CLIENT_IMAGES.icon_time_series()).getHTML();
	
	
	/**
	 * Returns the application-wide instance of the dialog used for
	 * displaying the analysis of an activity via an attribute tree.
	 * @return application-wide instance of the Activity Analysis Tree dialog.
	 */
	public static AnalysisTreeDialog getInstance()
	{
		if (s_Instance == null)
		{
			s_Instance = new AnalysisTreeDialog();
		}
		
		return s_Instance;
	}
	
	
	/**
	 * Creates the dialog for displaying the analysis of an activity in a tree.
	 */
	private AnalysisTreeDialog()
	{
		m_IncidentQueryService = AsyncServiceLocator.getInstance().getIncidentQueryService();
		
		setBodyBorder(false);
		setSize(800, 510);
		setHeading("Activity Analysis");
		
		BorderLayout borderLayout = new BorderLayout();   
		borderLayout.setContainerStyle("prl-border-layout-ct");
	    setLayout(borderLayout); 
	    
	    BorderLayoutData westData = new BorderLayoutData(LayoutRegion.WEST, 0.5f, 300, 800); 
	    westData.setSplit(true);   
	    westData.setFloatable(false);   
	    westData.setMargins(new Margins(0, 5, 0, 0)); 
	    
	    BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);   
	    centerData.setMargins(new Margins(0)); 
		
		setButtons(Dialog.CLOSE); 
		setHideOnButtonClick(true); 
	    
		ContentPanel treePanel = createTreeComponent();
		ContentPanel gridPanel = createGridComponent();
		//add(treePanel);
		
	    add(treePanel, westData);
	    add(gridPanel, centerData);
	    
	}
	
	
	/**
	 * Creates the <code>ContentPanel</code> holding the <code>TreeGrid</code> display.
	 * @return tree grid ContentPanel.
	 */
	protected ContentPanel createTreeComponent()
	{
		ContentPanel treePanel = new ContentPanel();
		treePanel.setHeaderVisible(false);
		treePanel.setLayout(new FitLayout());
		
		// Add a toolbar to hold the 'Group by' ComboBox.
		ToolBar treeToolBar = new ToolBar();
		treeToolBar.setSpacing(2);
		
		// Create the 'Group by' ComboBox, and populate with hard-coded 
		// list of attributes.
		m_GroupByCombo = new SimpleComboBox<String>();
		m_GroupByCombo.setWidth(130);
		m_GroupByCombo.setTriggerAction(TriggerAction.ALL); 
		m_GroupByCombo.setEditable(false);
		m_GroupByCombo.setListStyle("prl-combo-list");
		
		m_GroupByCombo.add("-- Auto --");
		m_GroupByCombo.add("Type");
		m_GroupByCombo.add("Metric");
		m_GroupByCombo.add("Source");
		m_GroupByCombo.add("Domain");
		m_GroupByCombo.add("Process");
		m_GroupByCombo.add("AgentName");
		m_GroupByCombo.add("RecordType");
		m_GroupByCombo.add("Resource0");
		m_GroupByCombo.add("Resource1");
		m_GroupByCombo.add("Resource2");
		m_GroupByCombo.add("Resource3");
		m_GroupByCombo.add("Resource4");
		m_GroupByCombo.add("Resource5");
		
		m_GroupByCombo.setSimpleValue("-- Auto --");
		
		m_GroupByCombo.addListener(Events.SelectionChange, 
				new SelectionChangedListener<SimpleComboValue<String>>(){

	        @Override
	        public void selectionChanged(SelectionChangedEvent<SimpleComboValue<String>> se)
	        {
	        	load();
	        }
        });
		
		treeToolBar.add(new LabelToolItem("Analyze by:"));
		treeToolBar.add(m_GroupByCombo);
		
		treePanel.setTopComponent(treeToolBar);
		
		m_TreeStore = new TreeStore<BaseTreeModel>();   
		  
	    ColumnConfig value = new ColumnConfig("value", "value", 100);   
	    value.setRenderer(new TreeGridCellRenderer<ModelData>(){

            @Override
            protected String getText(TreeGrid<ModelData> grid, ModelData model,
                    String property, int rowIndex, int colIndex)
            {
            	// Override to display a tooltip showing attribute name and value.
            	
            	StringBuilder sb = new StringBuilder();
	            sb.append("<span qtip=\"");
	            sb.append(model.get("value"));
	            if (model.get("name") != null)
	            {
	            	sb.append("\" qtitle=\"");
	            	sb.append(model.get("name"));
	            }
	            sb.append("\" >");
	            sb.append(model.get("value"));
				sb.append("</span>");
				
				return sb.toString();
            }
	    	
	    });   
	    ColumnConfig count = new ColumnConfig("count", "count", 100);   
	    ColumnModel cm = new ColumnModel(Arrays.asList(value, count));   
	  

	    TreeGrid<BaseTreeModel> tree = new TreeGrid<BaseTreeModel>(m_TreeStore, cm);    
	    tree.getStyle().setLeafIcon(AbstractImagePrototype.create(
	    				ClientUtil.CLIENT_IMAGES.icon_time_series()));   
	    tree.setAutoExpandColumn("value");   
	    tree.setTrackMouseOver(false); 
	    
	    // This reference to QuickTip is needed to enable tooltips.
	    @SuppressWarnings("unused")
	    QuickTip qtip = new QuickTip(tree);
	    
	    treePanel.add(tree);
	    
	    return treePanel;
	}
	
	
	/**
	 * Creates the <code>ContentPanel</code> holding the <code>Grid</code> display.
	 * @return grid ContentPanel, displaying a single level of the tree at a time.
	 */
	protected ContentPanel createGridComponent()
	{
		ContentPanel gridPanel = new ContentPanel();
		gridPanel.setHeaderVisible(false);
		gridPanel.setLayout(new FitLayout());
		
		m_ListStore = new ListStore<BaseTreeModel>();
		
		// Add a toolbar to hold an 'Up a level' button.
		ToolBar pathToolBar = new ToolBar();
		pathToolBar.setEnableOverflow(false);
		pathToolBar.setSpacing(3);
		gridPanel.setTopComponent(pathToolBar);
		
		
		// Create the 'Up' button.
		Button upButton = new Button();
		upButton.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.icon_folder_up()));
		upButton.setToolTip(ClientUtil.CLIENT_CONSTANTS.upOneLevel());
		upButton.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
			{
				BaseTreeModel firstNode = m_ListStore.getAt(0);
				BaseTreeModel parentNode = (BaseTreeModel)(firstNode.getParent());
				if (parentNode != null)
				{
					m_ListStore.removeAll();
					
					BaseTreeModel grandParentNode = (BaseTreeModel)(parentNode.getParent());
					if (grandParentNode == null)
					{
						// NB. Assumes a single top-level node.
						m_ListStore.add(parentNode);
					}
					else
					{
						int childCount = grandParentNode.getChildCount();
						for (int i = 0; i < childCount; i++)
						{
							m_ListStore.add((BaseTreeModel)(grandParentNode.getChild(i)));
						}
					}
				}
			}
		});
		pathToolBar.add(upButton);

		
		// Use a custom cell renderer to show icons and tooltips.
		GridCellRenderer<BaseTreeModel> cellRenderer = 
			new GridCellRenderer<BaseTreeModel>()
		{
            @Override
            public Object render(BaseTreeModel model, String property,
                    ColumnData config, int rowIndex, int colIndex,
                    ListStore<BaseTreeModel> store, Grid<BaseTreeModel> grid)
            {
            	StringBuilder sb = new StringBuilder();
            	sb.append("<span class=\"prl-metricPathTree-grid-cell\">");
            	sb.append(getNodeIconHtml(model));
	            sb.append("</span>");  
	            
	            sb.append("<span qtip=\"");
	            sb.append(model.get("value"));
	            if (model.get("name") != null)
	            {
	            	sb.append("\" qtitle=\"");
	            	sb.append(model.get("name"));
	            }
	            sb.append("\" >");
	            sb.append(model.get("value"));
				sb.append("</span>");
	            
	            return sb.toString();
            }
			
		};
		
		ColumnConfig value = new ColumnConfig(VALUE, VALUE, 100);   
	    value.setRenderer(new TreeGridCellRenderer<ModelData>());
	    value.setRenderer(cellRenderer);
	    
	    ColumnConfig count = new ColumnConfig(COUNT, COUNT, 100);   
	    ColumnModel cm = new ColumnModel(Arrays.asList(value, count)); 
		
		Grid<BaseTreeModel> folderGrid = 
			new Grid<BaseTreeModel>(m_ListStore, cm);
		folderGrid.setAutoExpandColumn(VALUE); 
		folderGrid.setLoadMask(true);

		
		// This reference to QuickTip is needed to enable tooltips.
	    @SuppressWarnings("unused")
	    QuickTip qtip = new QuickTip(folderGrid);
		
		
		// Double-click on a row to display the next level.
		folderGrid.addListener(Events.RowDoubleClick, 
	    		new Listener<GridEvent<BaseTreeModel>>(){

			@Override
            public void handleEvent(GridEvent<BaseTreeModel> ge)
            {
				BaseTreeModel node = ge.getModel();
				
				if (node.isLeaf() == false)
				{
					m_ListStore.removeAll();
					
					int childCount = node.getChildCount();
					for (int i = 0; i < childCount; i++)
					{
						m_ListStore.add((BaseTreeModel)(node.getChild(i)));
					}
				}
            }
	    	
	    });
		
		gridPanel.add(folderGrid);

		return gridPanel;
	}
	
	
	/**
	 * Returns the name of the attribute by which the summary data is currently 
	 * being grouped.
	 * @return the current 'Group by' aggregate name, or <code>null</code> if
	 * 	summary data is not being grouped by an attribute.
	 */
	public String getGroupBy()
	{
		String groupBy = null;
		
		String comboValue = m_GroupByCombo.getSimpleValue();
		if (comboValue.equals("-- Auto --") == false) 
		{
			groupBy = comboValue;
		}
		
		return groupBy;
	}
	
	
	/**
	 * Sets the ID of an item of evidence for the activity to be analysed.
	 * @param evidenceId evidence ID of a notification or feature from 
	 * 	the activity to analyse.
	 */
	public void setEvidenceId(int evidenceId)
	{
		m_EvidenceId = evidenceId;
	}
	
	
	/**
	 * Sets the names of attributes to be analysed in the tree.
	 * @param attributeNames <code>List</code> of attribute names.
	 */
	public void setTreeAttributeNames(List<String> attributeNames)
	{
		m_TreeAttributeNames = attributeNames;
		
		m_GroupByCombo.removeAll();
		m_GroupByCombo.add("-- Auto --");
		for (String attributeName : attributeNames)
		{
			m_GroupByCombo.add(attributeName);
		}
	}
	
	
	/**
	 * Loads the analysis data for the activity into the dialog.
	 */
	public void load()
	{
		ApplicationResponseHandler<ActivityTreeModel> callback = 
			new ApplicationResponseHandler<ActivityTreeModel>()
		{

			@Override
            public void uponSuccess(ActivityTreeModel rootNode)
            {
				m_TreeStore.add(rootNode, true);
				m_ListStore.add(rootNode);
            }

			@Override
            public void uponFailure(Throwable caught)
            {
				GWT.log(ClientUtil.CLIENT_CONSTANTS.errorLoadingActivityData() + ": ", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						ClientUtil.CLIENT_CONSTANTS.errorLoadingActivityData(), null);
            }
			
		};
		
		m_TreeStore.removeAll();
		m_ListStore.removeAll();
		
		m_IncidentQueryService.getSummaryTree(
				m_EvidenceId, m_TreeAttributeNames, false, getGroupBy(), -1, callback);
	}
	
	
	/**
	 * Returns the HTML used to display an icon for the specified node.
	 * @param treeNode
	 * @return the partial HTML for displaying a folder or leaf node.
	 */
	protected String getNodeIconHtml(BaseTreeModel treeNode)
	{
		if (treeNode.isLeaf() == false)
        {
        	return m_FolderClosedIconHtml;	
        }
        else
        {
            return m_TimeSeriesIconHtml;	
        }
	}
}
