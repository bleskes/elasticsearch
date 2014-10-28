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
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.toolbar.LabelToolItem;

import com.google.gwt.user.client.Element;
import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.data.EvidenceView;
import com.prelert.data.TimeFrame;
import com.prelert.service.AsyncServiceLocator;
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
	protected LabelToolItem 			m_FilterOperator;
	
	
	/**
	 * Creates a new filter toolbar for use in the specified evidence view.
	 * @param evidenceView evidence view for which this toolbar will be used.
	 */
	public EvidenceViewFilterToolBar(EvidenceView evidenceView)
	{
		this(evidenceView.getTimeFrame(), evidenceView.getFilterableAttributes());
	}
	
	
	/**
	 * Creates a new filter toolbar for use in an evidence view.
	 * @param timeFrame time frame of the list of evidence e.g. SECOND, MINUTE, HOUR.
	 * @param filterAttributes list of attributes on which the list of
	 * evidence can be filtered.
	 */
	public EvidenceViewFilterToolBar(TimeFrame timeFrame, 
			List<String> filterAttributes)
	{
		super(timeFrame);
		
		m_FilterAttributes = filterAttributes;
		
		m_FilterAttrCombo = new ComboBox<BaseModelData>();
		m_FilterAttrCombo.setEditable(false);
		m_FilterAttrCombo.setListStyle("prl-combo-list");
		m_FilterAttrCombo.setDisplayField("filterAttribute");
		m_FilterAttrCombo.setWidth(90);
		m_FilterAttrCombo.setTriggerAction(TriggerAction.ALL);  
		
		ListStore<BaseModelData> filterAttrs = new ListStore<BaseModelData>();
		BaseModelData attr0 = new BaseModelData();
		attr0.set("filterAttribute", ClientUtil.CLIENT_CONSTANTS.optionAll());
		filterAttrs.add(attr0);
		
		BaseModelData attr;
		if (m_FilterAttributes != null)
		{
			for (String filterAttribute : m_FilterAttributes)
			{
				attr = new BaseModelData();
				attr.set("filterAttribute", filterAttribute);
				filterAttrs.add(attr);
			}
		}
		m_FilterAttrCombo.setStore(filterAttrs);	
		
		m_FilterAttributeComboListener = new SelectionChangedListener<BaseModelData>() {
		      @Override
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

		    	  moveToTime(m_Loader.getDate());
		    	  populateFilterValues(selectedAttrName);
		      }
		};
		m_FilterAttrCombo.addSelectionChangedListener(m_FilterAttributeComboListener);
		
		m_FilterOperator = new LabelToolItem();
		m_FilterOperator.setStyleAttribute("text-align", "center");
		m_FilterOperator.setWidth("15px");
		
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
		    	  if (m_FilterValueCombo.getStore().indexOf(selectedValue) == 0)
		    	  {
		    		  m_Loader.setFilterValue(null);
		    	  }
		    	  else
		    	  {
		    		  String filterValue = selectedValue.get("filterValue");
			    	  m_Loader.setFilterValue(filterValue);
		    	  }

		    	  moveToTime(m_Loader.getDate());
		      }
		};
		m_FilterValueCombo.addSelectionChangedListener(m_FilterValueComboListener);
		
		// Specify a custom template for the drop-down list items which uses
		// Quicktip tooltips, allowing the use to read long items.
		StringBuilder valToolTip = new StringBuilder();
		valToolTip.append("<tpl for=\".\"><div class=\"prl-combo-list-item\" qtip=\"{filterValueTooltip");
		valToolTip.append("}\" qtitle=\"\">{filterValueTooltip}</div></tpl>");
		m_FilterValueCombo.setTemplate(valToolTip.toString());
		
		add(new LabelToolItem(ClientUtil.CLIENT_CONSTANTS.fieldFilter()));
		
		add(m_FilterAttrCombo);
		add(m_FilterOperator);
		add(m_FilterValueCombo);
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
			m_FilterValueCombo.disableEvents(true);
			EvidenceQueryServiceAsync queryService = 
				AsyncServiceLocator.getInstance().getEvidenceQueryService();
			queryService.getColumnValues(m_Loader.getDataType(), filterAttribute, Integer.MAX_VALUE,
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
		        	
		        	if (m_Loader.getFilterValue() != null)
		        	{
		        		// Set the Filter Value ComboBox in case when the window is first
		        		// loaded this call completes AFTER the data is loaded.
		        		setFilterValueSelection(m_Loader.getFilterValue());
		        	}
		        	
		        	m_FilterValueCombo.enableEvents(true);
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
		allValuesData.set("filterValue", ClientUtil.CLIENT_CONSTANTS.optionAll());
		allValuesData.set("filterValueTooltip", ClientUtil.CLIENT_CONSTANTS.optionAll());
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

		populateFilterValues(m_Loader.getFilterAttribute());  // Must be done post-render.

	}
	
	
	@Override
    protected void onLoad(LoadEvent event)
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
			BaseModelData attributeToSelect = null;
			
			if (attribute != null)
			{
				// Check that the attribute exists in the Attribute Combo's Store. 
				// If not, set to first 'All' selection.
				for (BaseModelData storeAttribute : attributeStore.getModels())
				{
					if (storeAttribute.get("filterAttribute").equals(attribute))
					{
						attributeToSelect = storeAttribute;
						m_FilterAttrCombo.setValue(attributeToSelect);
						break;
					}
				}
			}
			
			if (attributeToSelect != null)
			{
				m_FilterOperator.setLabel("=");
				m_FilterAttrCombo.setValue(attributeToSelect);
			}
			else
			{
				m_FilterOperator.setLabel("");
				m_FilterAttrCombo.setValue(attributeStore.getAt(0));
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
