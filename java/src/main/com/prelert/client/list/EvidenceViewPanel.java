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

package com.prelert.client.list;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.SimpleComboBox;
import com.extjs.gxt.ui.client.widget.form.SimpleComboValue;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.extjs.gxt.ui.client.widget.toolbar.FillToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.LabelToolItem;
import com.google.gwt.core.client.GWT;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.RequestViewEvent;
import com.prelert.data.Attribute;
import com.prelert.data.DataSourceType;
import com.prelert.data.Evidence;
import com.prelert.data.EvidenceView;
import com.prelert.data.View;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.EvidenceQueryServiceAsync;
import com.prelert.service.ViewCreationServiceAsync;


/**
 * A GXT panel which displays a view of evidence data. The panel consists of
 * a grid component, with each item of evidence rendered as a row in the grid,
 * and a toolbar with paging and filter controls.
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>OpenCausalityViewClick</b> : RequestViewEvent<br>
 * <div>Fires after a link to the probable cause view is selected.</div>
 * <ul>
 * <li>component : this</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>OpenNotificationViewClick</b> : RequestViewEvent<br>
 * <div>Fires after a link to a notification view is selected.</div>
 * <ul>
 * <li>component : this</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>OpenTimeSeriesViewClick</b> : RequestViewEvent<br>
 * <div>Fires after a link to a time series view is selected.</div>
 * <ul>
 * <li>component : this</li>
 * </ul>
 * </dd>
 * </dl>
 * 
 * @author Pete Harverson
 */
public class EvidenceViewPanel extends EvidenceGridPanel
{
	private String						m_FilterAttrName;
	private String						m_FilterAttrValue;
	
	protected SimpleComboBox<String> 	m_FilterAttrCombo;
	protected ComboBox<BaseModelData>	m_FilterValueCombo;
	private SelectionChangedListener<SimpleComboValue<String>> m_FilterAttributeComboListener;
	private SelectionChangedListener<BaseModelData>	m_FilterValueComboListener;
	protected LabelToolItem 			m_FilterOperator;
	
	private static HashMap<String, EvidenceView>	s_EvidenceViewsByType = new HashMap<String, EvidenceView>();
	
	private boolean						m_ViewReady = true;
	

	/**
	 * Creates a new panel to display a grid of evidence data.
	 */
	public EvidenceViewPanel()
	{	
		// Create the RpcProxy and PagingLoader to populate the list.
		super(new EvidenceViewPagingLoader(new GetEvidenceDataRpcProxy()));
		
		setHeaderVisible(false);
		addFilterControls();

		getLoader().addLoadListener(new FilterComboLoadListener());
		
		// Add a 'Show Analysis' item at the top of the context menu.
		final MenuItem probCauseMenuItem = new MenuItem(ClientUtil.CLIENT_CONSTANTS.showAnalysis());
		SelectionListener<MenuEvent> showCauseListener = new SelectionListener<MenuEvent>()
	    {
			@Override
            public void componentSelected(MenuEvent ce)
            {
				EvidenceModel selectedRow = getSelectedEvidence();
				if (selectedRow != null)
				{
					RequestViewEvent<EvidenceModel> rve = 
						new RequestViewEvent<EvidenceModel>(EvidenceViewPanel.this);
					rve.setModel(selectedRow);	
					fireEvent(GXTEvents.OpenCausalityViewClick, rve);
				}
            }
			
	    };
	    
	    probCauseMenuItem.addSelectionListener(showCauseListener);
	    m_GridContextMenu.insert(probCauseMenuItem, 0);
	    
	    // When the menu is shown, enable or disable the 'Show Probable Cause' 
	    // item depending on what is selected.
	    m_GridContextMenu.addListener(Events.BeforeShow, new Listener<MenuEvent>()
		{
			@Override
            public void handleEvent(MenuEvent be)
            {	
				boolean hasProbableCause = false;
				EvidenceModel selectedRow = getSelectedEvidence();
				if (selectedRow != null)
				{
					hasProbableCause = selectedRow.get(
							Evidence.COLUMN_NAME_PROBABLE_CAUSE, false);
				}
				probCauseMenuItem.setEnabled(hasProbableCause);
            }
	
		});
	}
	
	
	/**
	 * Grids a new panel to display a grid of evidence data, with the initial data type,
	 * columns, and filterable attributes set to the supplied values.
	 * @param evidenceView <code>EvidenceView</code> defining the properties of
	 * 	the evidence grid that will initially be displayed.
	 */
	public EvidenceViewPanel(EvidenceView evidenceView)
	{
		this();
		
		getLoader().setDataType(evidenceView.getDataType());
		
		if (evidenceView.getColumns() != null)
		{
			setColumns(evidenceView.getColumns());
		}
		
		if (evidenceView.getFilterableAttributes() != null)
		{
			setFilterableAttributes(evidenceView.getFilterableAttributes());
		}
	}


	/**
	 * Adds the filter controls into the panel's toolbar.
	 */
	protected void addFilterControls()
	{
		m_FilterAttrCombo = new SimpleComboBox<String>();
		m_FilterAttrCombo.setEditable(false);
		m_FilterAttrCombo.setListStyle("prl-combo-list");
		m_FilterAttrCombo.setWidth(90);
		m_FilterAttrCombo.setTriggerAction(TriggerAction.ALL);  
		
		m_FilterAttributeComboListener = new SelectionChangedListener<SimpleComboValue<String>>() {
			@Override
            public void selectionChanged(SelectionChangedEvent<SimpleComboValue<String>> se) 
			{
				if (m_FilterAttrName != null)
				{
					// This clears the filter on the previously set attribute name.
					getLoader().setFilterAttribute(m_FilterAttrName, null);
				}
				
				m_FilterAttrName = null;
		    	  
				if (m_FilterAttrCombo.getSelectedIndex() > 0)
				{
					m_FilterAttrName = m_FilterAttrCombo.getSimpleValue(); 
					getLoader().setFilterAttribute(m_FilterAttrName, null);
				}

				m_Loader.loadAtTime(m_Loader.getTime());
				populateFilterValues(m_FilterAttrName);
			}
		};
		
		m_FilterAttrCombo.addListener(Events.SelectionChange, m_FilterAttributeComboListener);
		
		m_FilterOperator = new LabelToolItem();
		m_FilterOperator.setStyleAttribute("text-align", "center");
		m_FilterOperator.setWidth("15px");
		
		// Use a ComboBox for the filter value as we need an extra display
		// property to swap double quotes in values to single ones.
		m_FilterValueCombo = new ComboBox<BaseModelData>();
		m_FilterValueCombo.setEditable(false);
		m_FilterValueCombo.setListStyle("prl-combo-list");
		m_FilterValueCombo.setDisplayField("filterValue");
		ListStore<BaseModelData> valuesStore = new ListStore<BaseModelData>();
		m_FilterValueCombo.setStore(valuesStore);
		m_FilterValueCombo.setTriggerAction(TriggerAction.ALL); 
		m_FilterValueCombo.setWidth(180);
		m_FilterValueComboListener = new SelectionChangedListener<BaseModelData>() {
			@Override
            public void selectionChanged(SelectionChangedEvent<BaseModelData> se) 
			{
				BaseModelData selectedValue = se.getSelectedItem();
				String filterValue = null;
				if (m_FilterValueCombo.getStore().indexOf(selectedValue) > 0)
				{
					filterValue = selectedValue.get("filterValue");
				}
				m_FilterAttrValue = filterValue;
				
				getLoader().setFilterAttribute(
				        m_FilterAttrCombo.getSimpleValue(), filterValue);

				m_Loader.loadAtTime(m_Loader.getTime());
			}
		};
		m_FilterValueCombo.addSelectionChangedListener(m_FilterValueComboListener);
		
		// Specify a custom template for the drop-down list items which uses
		// Quicktip tooltips, allowing the use to read long items.
		StringBuilder valToolTip = new StringBuilder();
		valToolTip.append("<tpl for=\".\"><div class=\"prl-combo-list-item\" qtip=\"{filterValueTooltip");
		valToolTip.append("}\" qtitle=\"\">{filterValueTooltip}</div></tpl>");
		m_FilterValueCombo.setTemplate(valToolTip.toString());
		
		addFilterValueAll();
		
		m_ToolBar.add(new FillToolItem());
		m_ToolBar.add(new LabelToolItem(ClientUtil.CLIENT_CONSTANTS.fieldFilter()));
		m_ToolBar.add(m_FilterAttrCombo);
		m_ToolBar.add(m_FilterOperator);
		m_ToolBar.add(m_FilterValueCombo);
	}


	/**
	 * Sets the list of attribute names on which the view can be filtered,
	 * such as severity or description.
	 * @param filterableAttribute the list of filterable attribute names, 
	 * or <code>null</code> if this view cannot be filtered.
     */
    public void setFilterableAttributes(List<String> attributeNames)
    {
    	m_FilterAttrCombo.disableEvents(true);
    	m_FilterAttrCombo.removeAll();
    	
    	m_FilterAttrCombo.add(ClientUtil.CLIENT_CONSTANTS.optionAll());
		if (attributeNames != null)
		{
			m_FilterAttrCombo.add(attributeNames);
		}
		
		m_FilterAttrCombo.setValue(m_FilterAttrCombo.getStore().getAt(0));
		
		m_FilterAttrCombo.enableEvents(true);
		
		m_FilterOperator.setLabel("");
    }
    
    
    /**
     * Reconfigures the grid for showing evidence data for the specified data type,
     * source and filter.
     * @param dataType dataType the data type, such as 'p2psmon_users' or 'system_udp',
	 * 	or <code>null</code> to load evidence across all data types.
     * @param source name of the source (server) for the data to load, or 
     * 	<code>null</code> to load evidence across all sources.
     * @param filter optional list of attributes on which to filter the list.
     */
    public void reconfigure(DataSourceType dataType, String source, List<Attribute> filter)
    {
    	final String typeName = dataType.getName();
    	final DataSourceType dsType = dataType;
    	final String sourceName = source;
    	final List<Attribute> filterAttrs = filter;
		EvidenceView evidenceView = s_EvidenceViewsByType.get(typeName);
		if (evidenceView == null)
		{
			// Get the view config from the ViewCreationService.
			ViewCreationServiceAsync viewService = 
				AsyncServiceLocator.getInstance().getViewCreationService();
			
			ApplicationResponseHandler<View> callback = new ApplicationResponseHandler<View>(){

				@Override
                public void uponFailure(Throwable caught)
                {
					GWT.log("Error loading evidence view for data type: " + typeName, caught);
					MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
							ClientUtil.CLIENT_CONSTANTS.errorLoadingViewForType(typeName), null);
                }
				
				
				@Override
                public void uponSuccess(View evidenceView)
                {
					s_EvidenceViewsByType.put(typeName, (EvidenceView) evidenceView);
					reconfigure(dsType, sourceName, filterAttrs);
					m_ViewReady = true;
					fireEvent(Events.ViewReady);
                }
			};
			
			m_ViewReady = false;
			viewService.getView(dataType, callback);
		}
		else
		{
			String currentType = m_Loader.getDataType();
			if ( (typeName != null && (typeName.equals(currentType) == false) ) ||
					(typeName == null && currentType != null) )
			{
				// Data type has changed, set the type and columns, 
				// and clear the toolbar filter.
				setDataType(typeName);
				setColumns(evidenceView.getColumns());
				clearToolBarFilter();
			}
			
			List<String> filterableAttributes = evidenceView.getFilterableAttributes();
			ArrayList<String> attributesToAdd = new ArrayList<String>();
			if (filterableAttributes != null)
			{
				attributesToAdd.addAll(filterableAttributes);
			}
			if (filter != null)
			{
				String attributeName; 
				for (Attribute attribute : filter)
				{
					attributeName = attribute.getAttributeName();
					attributesToAdd.remove(attributeName);
				}
			}
			// Retain the toolbar filter if the attribute is in the supplied list.
			String toolbarFilterName = getToolBarFilterAttributeName();
			if (filterableAttributes.contains(toolbarFilterName))
			{
				filter.add(new Attribute(
						toolbarFilterName, getToolBarFilterAttributeValue()));
			}
			
			setFilterableAttributes(attributesToAdd);
			m_Loader.setSource(source);
			m_Loader.setFilter(filter);
		}
    }
    
    
    @Override
    public void load()
    {
    	if (m_ViewReady == true)
    	{
			super.load();
    	}
    	else
    	{
    		addListener(Events.ViewReady,new Listener<ComponentEvent>(){

				@Override
                public void handleEvent(ComponentEvent be)
                {
					removeListener(Events.ViewReady, this);
					load();
                }
    			
    		});
    	}
    }


	/**
	 * Loads the list of evidence, the top of row of which will match the 
	 * specified id.
	 * @param evidenceId id for the top row of evidence data to be loaded.
	 */
    public void loadAtId(final int evidenceId)
    {
    	if (m_ViewReady == true)
    	{
    		EvidenceViewPagingLoader pagingLoader = (EvidenceViewPagingLoader)m_Loader;
        	pagingLoader.loadAtId(evidenceId);
    	}
    	else
    	{
    		addListener(Events.ViewReady,new Listener<ComponentEvent>(){

				@Override
                public void handleEvent(ComponentEvent be)
                {
					removeListener(Events.ViewReady, this);
					EvidenceViewPagingLoader pagingLoader = (EvidenceViewPagingLoader)m_Loader;
		        	pagingLoader.loadAtId(evidenceId);
                }
    			
    		});
    	}
    }
	
    
    /**
	 * Populates the filter value combo box with the list of possible values
	 * for the supplied attribute name.
	 * @param filterAttribute attribute name for which to obtain the possible values
	 * e.g. 'source', 'description' or 'username'.
	 */
	protected void populateFilterValues(String filterAttribute)
	{
		// Clear out the Filter Value ComboBox and repopulate 
		// with the values for this filter attribute.
		clearFilterValues();
		addFilterValueAll();

		if (filterAttribute != null)
		{
			m_FilterValueCombo.disableEvents(true);
			EvidenceQueryServiceAsync queryService = 
				AsyncServiceLocator.getInstance().getEvidenceQueryService();
			queryService.getColumnValues(getLoader().getDataType(), filterAttribute, Integer.MAX_VALUE,
					new ApplicationResponseHandler<List<String>>(){
	
		        public void uponFailure(Throwable caught)
		        {
		        	MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
			        		ClientUtil.CLIENT_CONSTANTS.errorNoServerResponse(), null);
		        }
	
	
		        public void uponSuccess(List<String> filterValues)
		        {        	
		        	ListStore<BaseModelData> valuesStore = m_FilterValueCombo.getStore();
		        	BaseModelData filterValueData;
		        	
		        	for (String filterValue : filterValues)
		        	{
		        		if (filterValue != null)
		        		{     
		        			// NB. 12-08-09
		        			// Don't add in 'null' value as DB procs currently
		        			// return all rows (non-null and null) when filter value is null
		        			// e.g. CALL evidence_first_page('service', null).
		        			filterValueData = new BaseModelData();
		        			filterValueData.set("filterValue", filterValue);
		        			
		        			// Add in a field for the tooltip - need to swap double quotes
		        			// as they mess up the Quicktip tooltip.
		        			filterValueData.set("filterValueTooltip", filterValue.replace('"', '\''));
		        			
		        			valuesStore.add(filterValueData);
		        		}
		        	}
		        	
		        	m_FilterValueCombo.enableEvents(true);
		        }
	        });
		}
	}
	
	
	/**
	 * Returns the name of the attribute set in the toolbar filter controls.
	 * @return the toolbar filter attribute name, or <code>null</code> if no
	 * 	filter is currently set.
	 */
	public String getToolBarFilterAttributeName()
	{
		return m_FilterAttrName;
	}
	
	
	/**
	 * Returns the value of the attribute set in the toolbar filter controls.
	 * @return the toolbar filter attribute value, or <code>null</code> if no
	 * 	filter value is currently set.
	 */
	public String getToolBarFilterAttributeValue()
	{
		return m_FilterAttrValue;
	}
	
	
	/**
	 * Clears the filter currently set in the toolbar controls.
	 */
	public void clearToolBarFilter()
	{
		setFilterAttributeSelection(null);
		setFilterValueSelection(null);
	}
	
	
	/**
	 * Clears the list of values in the 'Filter value' drop-down.
	 */
	protected void clearFilterValues()
	{
		m_FilterValueCombo.clearSelections();
		m_FilterAttrValue = null;
		ListStore<BaseModelData> valuesStore = m_FilterValueCombo.getStore();
		valuesStore.removeAll();
	}
	
	
	/**
	 * Adds a single 'All Values' items into the filter value combo box.
	 */
	protected void addFilterValueAll()
	{
		// Add an 'All Values' item at the top.
		BaseModelData allValuesData = new BaseModelData();
		allValuesData.set("filterValue", ClientUtil.CLIENT_CONSTANTS.optionAll());
		allValuesData.set("filterValueTooltip", ClientUtil.CLIENT_CONSTANTS.optionAll());
		m_FilterValueCombo.getStore().add(allValuesData);
		
		// Disable the SelectionChangedListener whilst setting the initial
		// value to ensure it does not trigger another query.
		m_FilterValueCombo.disableEvents(true);
		m_FilterValueCombo.setValue(allValuesData);  
		m_FilterValueCombo.enableEvents(true);
	}
	
	
	/**
	 * Sets the filter attribute ComboBox to the selected attribute. Note that this 
	 * simply updates the ComboBox field, and does not update the evidence view.
	 * @param attribute filter attribute name.
	 */
	protected void setFilterAttributeSelection(String attribute)
	{
		ListStore<SimpleComboValue<String>> attributeStore = m_FilterAttrCombo.getStore();
		if (attributeStore.getCount() > 0)
		{
			m_FilterAttrCombo.disableEvents(true);
			
			// Check that the attribute exists in the Attribute Combo's Store. 
			// If not, set to first 'All' selection.
			if ( (attribute != null) && (m_FilterAttrCombo.findModel(attribute) != null) )
			{
				m_FilterOperator.setLabel("=");
				m_FilterAttrCombo.setSimpleValue(attribute);
				m_FilterAttrName = attribute;
			}
			else
			{
				m_FilterOperator.setLabel("");
				m_FilterAttrCombo.setValue(attributeStore.getAt(0));
				m_FilterAttrName = null;
			}
			
			m_FilterAttrCombo.enableEvents(true);
		}
	}
	
	
	/**
	 * Sets the filter value ComboBox to the selected value. Note that this 
	 * simply updates the ComboBox field, and does not update the evidence view.
	 * @param value filter value to set.
	 */
	protected void setFilterValueSelection(String value)
	{
		ListStore<BaseModelData> valueStore = m_FilterValueCombo.getStore();
		if (valueStore.getCount() > 0)
		{
			m_FilterValueCombo.disableEvents(true);
			
			if (value == null)
			{
				m_FilterValueCombo.setValue(valueStore.getAt(0));
				m_FilterAttrValue = null;
			}
			else
			{
				List<BaseModelData> selectedValues = m_FilterValueCombo.getSelection();
				String currentlySelectedVal = null;
				if (selectedValues.size() > 0)
				{
					BaseModelData selectedValData = selectedValues.get(0);
					if (m_FilterValueCombo.getStore().indexOf(selectedValData) != 0)
			  	  	{
						currentlySelectedVal = selectedValData.get("filterValue");
			  	  	}
				}
				
				if (value.equals(currentlySelectedVal) == false)
				{
					BaseModelData valueData;
					for (int i = 0; i < valueStore.getCount(); i++)
					{
						valueData = valueStore.getAt(i);
						if (valueData.get("filterValue").equals(value))
						{
							m_FilterValueCombo.setValue(valueData);
							m_FilterAttrValue = value;
							break;
						}
					}
				}
			}
			
			m_FilterValueCombo.enableEvents(true);
		}
	}
	
	
	/**
	 * <code>LoadListener</code> for adding to the grid's loader which sets the
	 * toolbar filter controls to the correct values after the grid has loaded.
	 */
	class FilterComboLoadListener extends LoadListener
	{

        @Override
        public void loaderLoad(LoadEvent le)
        {
        	// TODO - check behaviour when filter values load after the grid.
        	setFilterAttributeSelection(m_FilterAttrName);
        	if (m_FilterAttrName == null)
        	{
        		// This attribute is no longer available. Clear the values.
        		clearFilterValues();
        		addFilterValueAll();
        	}
        	else
        	{
        		setFilterValueSelection(m_FilterAttrValue);
        	}
        }
		
	}
}
