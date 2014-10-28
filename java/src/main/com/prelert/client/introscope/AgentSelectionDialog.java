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

package com.prelert.client.introscope;

import java.util.ArrayList;
import java.util.List;

import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FieldEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.WindowEvent;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.TriggerField;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.PagingToolBar;
import com.google.gwt.event.dom.client.KeyCodes;

import com.prelert.client.ClientUtil;
import com.prelert.client.gxt.GridCheckBoxSelectionModel;
import com.prelert.data.gxt.AttributeModel;
import com.prelert.data.gxt.DataTypeConnectionModel;


/**
 * An extension of the Ext GWT Dialog for paging through all the agents in 
 * Introscope, allowing the user to select a list of agents for analysis by Prelert. 
 * The dialog consists of a grid listing the agents, with a toolbar above holding
 * controls for paging back and forth through the agents and a search box for filtering
 * the list of agents down to those whose name contains the entered text. 
 * Below the grid are 'OK' and 'Cancel' buttons.
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>AfterEdit</b> : ComponentEvent<br>
 * <div>Fires after the selection of agents has been edited.</div>
 * <ul>
 * <li>component : this</li>
 * </ul>
 * </dd>
 * 
 * </dl>
 * 
 * @author Pete Harverson
 */
public class AgentSelectionDialog extends Dialog
{
	private AgentPagingLoader			m_Loader;
	private ListStore<AttributeModel> 	m_ListStore;
	private Grid<AttributeModel> 		m_AgentGrid;
	
	private int 						m_MaxSelect;
	private List<String>				m_SelectedAgents;	// Cache of current selections.
	private List<String>				m_StoredSelection;	// Saved selections.
	
	public static final int PAGE_SIZE = 25;
	
	
	/**
	 * Creates a new dialog for selecting a list of Introscope agents for analysis, 
	 * with no limit on the number of agents that can be selected.
	 */
	public AgentSelectionDialog()
	{
		this(-1);
	}
	
	
	/**
	 * Creates a new dialog for selecting a list of Introscope agents for analysis.
	 * @param maxSelect the maximum number of agents that may be selected for
	 * 	analysis, or <code>-1</code> to allow unlimited selection.
	 */
	public AgentSelectionDialog(int maxSelect)
	{
		m_MaxSelect = maxSelect;
		m_SelectedAgents = new ArrayList<String>();
		m_StoredSelection = new ArrayList<String>();
		
		setSize(450, 550);
		setHeading(IntroscopeDiagnosticsUIBuilder.INTROSCOPE_MESSAGES.selectAgents());
		setLayout(new FitLayout());
		
		setButtons(Dialog.OKCANCEL); 
		setHideOnButtonClick(true);
		
		AgentPagingRpcProxy proxy = new AgentPagingRpcProxy();
		m_Loader = new AgentPagingLoader(proxy);
		
		// Create the paging toolbar. Hide the display label.
		PagingToolBar toolbar = new PagingToolBar(PAGE_SIZE);
		toolbar.getItem(9).hide();
		toolbar.getItem(10).hide();
		toolbar.getItem(12).hide();
		
		// Add a search trigger field on the right of the toolbar.
		final TriggerField<String> searchField = new TriggerField<String>();
		searchField.setTriggerStyle("x-form-search-trigger");
		searchField.setWidth(160);
		toolbar.add(searchField);
		
		searchField.addListener(Events.TriggerClick, new Listener<FieldEvent>(){

			@Override
			public void handleEvent(FieldEvent fe)
            {
				m_Loader.setContainsText(searchField.getValue());
				m_Loader.setOffset(0);
				m_Loader.load();
            }
	    	
	    });
		searchField.addListener(Events.KeyPress, new Listener<FieldEvent>(){

			@Override
            public void handleEvent(FieldEvent fe)
            {
	           	if (fe.getKeyCode() == KeyCodes.KEY_ENTER)
	           	{
	           		m_Loader.setContainsText(searchField.getValue());
					m_Loader.setOffset(0);
					m_Loader.load();
	           	}
            }
	    	
	    });
		
		// Add a listener on the BeforeHide event to save/revert changes.
		addListener(Events.BeforeHide, new Listener<WindowEvent>(){

			@Override
            public void handleEvent(WindowEvent be)
            {
				Button buttonClicked = be.getButtonClicked();
				if (getButtonById(Dialog.OK) == buttonClicked)
				{
					if (m_StoredSelection.equals(m_SelectedAgents) == false)
					{
						m_StoredSelection.clear();
						m_StoredSelection.addAll(m_SelectedAgents);
						fireEvent(Events.AfterEdit, new ComponentEvent(AgentSelectionDialog.this));
					}
				}
				else
				{
					m_SelectedAgents.clear();
					m_SelectedAgents.addAll(m_StoredSelection);
				}
            }
			
		});
	    
		toolbar.bind(m_Loader);
		setTopComponent(toolbar);
		
		m_AgentGrid = createGrid();
		add(m_AgentGrid);
	}
	
	
	/**
	 * Creates the grid for displaying the list of agents.
	 * @return the agent grid.
	 */
	protected Grid<AttributeModel> createGrid()
	{
		m_ListStore = new ListStore<AttributeModel>(m_Loader);

		// Add columns for the checkbox selector and the agent name.
		List<ColumnConfig> config = new ArrayList<ColumnConfig>();
		
		GridCheckBoxSelectionModel<AttributeModel> selectionModel = 
			new GridCheckBoxSelectionModel<AttributeModel>(false);
		
		// Update the list of selected agents (which may be spread over more
		// than one page) when the user selects/deselects a checkbox.
		selectionModel.addSelectionChangedListener(
				new SelectionChangedListener<AttributeModel>(){

			@Override
            public void selectionChanged(SelectionChangedEvent<AttributeModel> se)
            {
				processAgentSelections(se.getSelection());
            }
	    });
		
		m_Loader.addLoadListener(new LoadListener(){

            @Override
            public void loaderLoad(LoadEvent le)
            {
            	markSelectionsInGrid();
            }
	    	
	    });
	    
		ColumnConfig agentNameColumn = new ColumnConfig("attributeValue", "agentName", 150);
		config.add(selectionModel.getColumn());
		config.add(agentNameColumn);
	    
	    ColumnModel columnModel = new ColumnModel(config);
	    
	    Grid<AttributeModel> agentGrid = new Grid<AttributeModel>(m_ListStore, columnModel);
	    agentGrid.setAutoExpandColumn("attributeValue");
	    agentGrid.getView().setForceFit(true);
	    agentGrid.setLoadMask(true);
	    agentGrid.setHideHeaders(true);
	    
	    agentGrid.setSelectionModel(selectionModel);
	    agentGrid.addPlugin(selectionModel);

	    return agentGrid;
	}
	
	
	/**
     * Sets the configuration parameters for the connection to the EM for
     * which agents are being obtained.
     * @param connectionModel <code>DataTypeConnectionModel</code> encapsulating the 
	 * 	configuration properties of the connection to Introscope.
     */
    public void setConnectionConfig(DataTypeConnectionModel connectionModel)
    {
    	m_Loader.setConnectionConfig(connectionModel);
    }
	
	
	/**
	 * Loads the list of Introscope agents into the dialog.
	 */
	public void load()
	{
		m_Loader.load();
	}
	
	
	/**
	 * Returns the list of Introscope agents that have been selected for analysis.
	 * @return the list of selected agents.
	 */
	public List<String> getSelectedAgents()
	{
		return m_StoredSelection;
	}
	
	
	/**
	 * Sets the list of Introscope agents that have been selected for analysis.
	 * @param agents the list of selected agents.
	 */
	public void setSelectedAgents(List<String> agents)
	{
		m_StoredSelection.clear();
		m_SelectedAgents.clear();
		if (agents != null)
		{
			m_StoredSelection.addAll(agents);
			m_SelectedAgents.addAll(agents);
		}
		
		markSelectionsInGrid();
	}
	
	
	/**
	 * Processes agents have been selected in the grid by the user, adding 
	 * or removing items from the internal cache of selected agents.
	 * @param selectedItems list of agents selected in the current page.
	 */
	protected void processAgentSelections(List<AttributeModel> selectedItems)
	{
		GridSelectionModel<AttributeModel> selectionModel = m_AgentGrid.getSelectionModel();
		selectionModel.setFiresEvents(false);
		
		int alreadySelected = m_SelectedAgents.size();
		
		List<AttributeModel> agentsInPage = m_ListStore.getModels();
		for (AttributeModel model : agentsInPage)
		{
			String agentName = model.getAttributeValue();
			if (selectedItems.contains(model) == true)
			{
				if (m_SelectedAgents.contains(agentName) == false)
				{
					if ( (m_MaxSelect == -1) || (alreadySelected < m_MaxSelect) )
					{
						m_SelectedAgents.add(agentName);
					}
					else
					{
						MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.warningTitle(),
								IntroscopeDiagnosticsUIBuilder.INTROSCOPE_MESSAGES.warningMaximumAgents(m_MaxSelect), null);
						selectionModel.deselect(model);
					}
				}
			}
			else
			{
				// Remove any agents that have been deselected in the current page.
				m_SelectedAgents.remove(model.getAttributeValue());
			}
		}
		
		selectionModel.setFiresEvents(true);
	}
	
	
	/**
	 * Marks as selected any agents in the grid which the user has previously selected.
	 */
	protected void markSelectionsInGrid()
	{
		GridSelectionModel<AttributeModel> selectionModel = m_AgentGrid.getSelectionModel();
		selectionModel.setFiresEvents(false);
    	
    	List<AttributeModel> agentsInPage = m_ListStore.getModels();
    	for (AttributeModel model : agentsInPage)
    	{
    		if (m_SelectedAgents.contains(model.getAttributeValue()) == true)
	    	{
    			selectionModel.select(model, true);
    			m_ListStore.update(model);
	    	}
    	}
    	selectionModel.setFiresEvents(true);
	}
}
