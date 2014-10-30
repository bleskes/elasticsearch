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

package com.prelert.client.list;

import java.util.List;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.messages.MyMessages;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.toolbar.AdapterToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.LabelToolItem;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.data.DatePagingLoadConfig;
import com.prelert.data.DatePagingLoadResult;
import com.prelert.data.EventRecord;
import com.prelert.data.EvidenceView;
import com.prelert.data.EvidenceViewPagingLoader;
import com.prelert.service.DatabaseServiceLocator;
import com.prelert.service.EvidenceQueryServiceAsync;


/**
 * A specialized toolbar for use in an Evidence Window, containing controls for
 * configuring a filter to be applied to the list of evidence.
 * <p>
 * It is bound to an {@link EvidenceViewPagingLoader} and provides automatic paging
 * controls to navigate through the date range of evidence in the Prelert database. 
 * The toolbar contains a DateTimePicker for the selecting the date of
 * the evidence displayed, and two ComboBox controls for selecting the filter 
 * attribute and value e.g. 'description = service has shutdown'.
 * 
 * <dl>
 * <dt>Inherited Events:</dt>
 * <dd>Component Enable</dd>
 * <dd>Component Disable</dd>
 * <dd>Component BeforeHide</dd>
 * <dd>Component Hide</dd>
 * <dd>Component BeforeShow</dd>
 * <dd>Component Show</dd>
 * <dd>Component Attach</dd>
 * <dd>Component Detach</dd>
 * <dd>Component BeforeRender</dd>
 * <dd>Component Render</dd>
 * <dd>Component BrowserEvent</dd>
 * <dd>Component BeforeStateRestore</dd>
 * <dd>Component StateRestore</dd>
 * <dd>Component BeforeStateSave</dd>
 * <dd>Component SaveState</dd>
 * </dl>
 */
public class EvidenceViewFilterToolBar extends EvidenceViewPagingToolBar
{
	private List<String> 				m_FilterAttributes;
	
	protected ComboBox<BaseModelData> 	m_FilterAttrCombo;
	protected ComboBox<BaseModelData>	m_FilterValueCombo;
	private SelectionChangedListener<BaseModelData>	m_FilterAttributeComboListener;
	private SelectionChangedListener<BaseModelData>	m_FilterValueComboListener;
	protected Label 					m_FilterOperator;
	
	
	/**
	 * Creates a new filter toolbar for use in the specified evidence view.
	 * @param evidenceView evidence view for which this toolbar will be used.
	 */
	public EvidenceViewFilterToolBar(EvidenceView evidenceView)
	{
		super(evidenceView.getDataType(), evidenceView.getTimeFrame());
		
		m_FilterAttributes = evidenceView.getFilterableAttributes();
	}
	
	
	/**
	 * Populates the filter value combo box with the list of possible values
	 * for the supplied attribute name.
	 * @param filterAttribute attribute name for which to obtain the possible values
	 * e.g. 'source', 'description' or 'username'.
	 */
	public void populateFilterValues(String filterAttribute)
	{
		// Clear out the Filter Value ComboBox and repopulate 
		// with the values for this filter attribute.
		clearFilterValues();

		if (filterAttribute != null)
		{
			EvidenceQueryServiceAsync queryService = 
				DatabaseServiceLocator.getInstance().getEvidenceQueryService();
			queryService.getColumnValues(m_DataType, filterAttribute,  
					new ApplicationResponseHandler<List<String>>(){
	
		        public void uponFailure(Throwable caught)
		        {
			        MessageBox.alert("Prelert - Error",
			                "Failed to get a response from the server", null);
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
		        	
		        	if (m_Loader.getFilterValue() != null)
		        	{
		        		// Set the Filter Value ComboBox in case when the window is first
		        		// loaded this call completes AFTER the data is loaded.
		        		setFilterValueSelection(m_Loader.getFilterValue());
		        	}
		        }
	        });
		}
	}
	
	
	/**
	 * Clears the list of values in the 'Filter value' drop-down, and adds a single
	 * 'All Values' items into the combo box.
	 */
	public void clearFilterValues()
	{
		m_FilterValueCombo.clearSelections();
		ListStore<BaseModelData> valuesStore = m_FilterValueCombo.getStore();
		valuesStore.removeAll();
		
		// Add an 'All Values' item at the top.
		BaseModelData allValuesData = new BaseModelData();
		allValuesData.set("filterValue", "-- All --");
		allValuesData.set("filterValueTooltip", "-- All --");
		valuesStore.add(allValuesData);
		
		// Disable the SelectionChangedListener whilst setting the initial
		// value to ensure it does not trigger another query.
		m_FilterValueCombo.disableEvents(true);
		m_FilterValueCombo.setValue(allValuesData);  
		m_FilterValueCombo.enableEvents(true);
	}
	
	
	@Override
	protected void onRender(Element target, int index)
	{
		super.onRender(target, index);
		
		m_FilterAttrCombo = new ComboBox<BaseModelData>();
		m_FilterAttrCombo.setEditable(false);
		m_FilterAttrCombo.setListStyle("prelert-combo-list");
		m_FilterAttrCombo.setDisplayField("filterAttribute");
		m_FilterAttrCombo.setWidth(90);
		
		ListStore<BaseModelData> filterAttributes = new ListStore<BaseModelData>();
		BaseModelData attr0 = new BaseModelData();
		attr0.set("filterAttribute", "-- All --");
		filterAttributes.add(attr0);
		
		BaseModelData attr;
		if (m_FilterAttributes != null)
		{
			for (String filterAttribute : m_FilterAttributes)
			{
				attr = new BaseModelData();
				attr.set("filterAttribute", filterAttribute);
				filterAttributes.add(attr);
			}
		}
		m_FilterAttrCombo.setStore(filterAttributes);	
		
		m_FilterAttributeComboListener = new SelectionChangedListener<BaseModelData>() {
		      public void selectionChanged(SelectionChangedEvent<BaseModelData> se) 
		      {
		    	  BaseModelData selectedAttr = se.getSelectedItem();
		    	  String selectedAttrName = null;
		    	  
		    	  if (m_FilterAttrCombo.getStore().indexOf(selectedAttr) > 0)
		    	  {
		    		  selectedAttrName = selectedAttr.get("filterAttribute"); 
		    	  }
		    	  
		    	  m_Loader.setFilterAttribute(selectedAttrName);
		    	  m_Loader.setFilterValue(null);
		    	  first();
		    	  populateFilterValues(selectedAttrName);
		      }
		};
		m_FilterAttrCombo.addSelectionChangedListener(m_FilterAttributeComboListener);
		
		m_FilterOperator = new Label();
		m_FilterOperator.setWidth("15px");
		m_FilterOperator.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		
		m_FilterValueCombo = new ComboBox<BaseModelData>();
		m_FilterValueCombo.setEditable(false);
		m_FilterValueCombo.setListStyle("prelert-combo-list");
		m_FilterValueCombo.setDisplayField("filterValue");
		ListStore<BaseModelData> valuesStore = new ListStore<BaseModelData>();
		m_FilterValueCombo.setStore(valuesStore);
		m_FilterValueCombo.setWidth(130);
		
		// Specify a custom template for the drop-down list items which uses
		// Quicktip tooltips, allowing the use to read long items.
		StringBuilder toolTip = new StringBuilder();
        toolTip.append("<tpl for=\".\"><div class=\"prelert-combo-list-item\" qtip=\"{filterValueTooltip");
        toolTip.append("}\" qtitle=\" ");
        toolTip.append(" \">{filterValueTooltip");
        toolTip.append("}</div></tpl>");
		m_FilterValueCombo.setTemplate(toolTip.toString());
		
		m_FilterValueComboListener = new SelectionChangedListener<BaseModelData>() {
		      public void selectionChanged(SelectionChangedEvent<BaseModelData> se) 
		      {
		    	  BaseModelData selectedValue = se.getSelectedItem();
		    	  
		    	  if (m_FilterValueCombo.getStore().indexOf(selectedValue) == 0)
		    	  {
		    		  m_Loader.setFilterValue(null);
		    	  }
		    	  else
		    	  {
		    		  String filterValue = selectedValue.get("filterValue");
			    	  m_Loader.setFilterValue(filterValue);
		    	  }
		    	  
		    	  first();
		      }
		};
		m_FilterValueCombo.addSelectionChangedListener(m_FilterValueComboListener);

		m_ToolBar.add(new AdapterToolItem(new LabelToolItem("Filter:  ")));
		m_ToolBar.add(new AdapterToolItem(m_FilterAttrCombo));
		m_ToolBar.add(new AdapterToolItem(new AdapterToolItem(m_FilterOperator)));
		m_ToolBar.add(new AdapterToolItem(m_FilterValueCombo));

		m_ToolBar.render(target, index);
		//setElement(m_ToolBar.getElement());
		
		populateFilterValues(m_Loader.getFilterAttribute());  // Must be done post-render.
	}
	
	
	protected void onLoad(
	        LoadEvent<DatePagingLoadConfig, DatePagingLoadResult<EventRecord>> event)
	{
		super.onLoad(event);
		
		setFilterAttributeSelection(m_Loader.getFilterAttribute());
		setFilterValueSelection(m_Loader.getFilterValue());
	}
	
	
	/**
	 * Sets the filter attribute ComboBox to the selected attribute. Note that this 
	 * simply updates the ComboBox field, and does not update the evidence view.
	 * @param attribute filter attribute to set.
	 */
	protected void setFilterAttributeSelection(String attribute)
	{
		ListStore<BaseModelData> attributeStore = m_FilterAttrCombo.getStore();
		if (attributeStore.getCount() > 0)
		{
			m_FilterAttrCombo.disableEvents(true);
			
			if (attribute == null)
			{
				m_FilterAttrCombo.setValue(attributeStore.getAt(0));
				m_FilterOperator.setText("");
			}
			else
			{
				// All filter attributes use the equality operator.
				m_FilterOperator.setText("=");
				
				List<BaseModelData> selectedAttributes = m_FilterAttrCombo.getSelection();
				String currentlySelectedAttr = null;
				if (selectedAttributes.size() > 0)
				{
					BaseModelData selectedAttrData = selectedAttributes.get(0);
					if (m_FilterAttrCombo.getStore().indexOf(selectedAttrData) != 0)
			  	  	{
						currentlySelectedAttr = selectedAttrData.get("filterAttribute");
			  	  	}
				}
				
				if (attribute.equals(currentlySelectedAttr) == false)
				{
					BaseModelData attributeData;
					for (int i = 0; i < attributeStore.getCount(); i++)
					{
						attributeData = attributeStore.getAt(i);
						if (attributeData.get("filterAttribute").equals(attribute))
						{
							m_FilterAttrCombo.setValue(attributeData);
							break;
						}
					}
				}
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
							break;
						}
					}
				}
			}
			
			m_FilterValueCombo.enableEvents(true);
		}
	}
}
