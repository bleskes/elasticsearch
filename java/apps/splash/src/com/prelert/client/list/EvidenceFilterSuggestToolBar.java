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
import com.extjs.gxt.ui.client.widget.WidgetComponent;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.toolbar.LabelToolItem;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.data.EvidenceView;
import com.prelert.data.TimeFrame;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.EvidenceQueryServiceAsync;


/**
 * A specialized toolbar for use in an Evidence Window, containing controls for
 * configuring a filter to be applied to the list of evidence. It is intended for
 * use in situations where filter attributes have many values (in excess of 200)
 * using a Suggest Box as the control for the filter value.
 * <p>
 * It is bound to an {@link EvidenceViewPagingLoader} and provides automatic paging
 * controls to navigate through the date range of evidence in the Prelert database. 
 * The toolbar contains a DateTimePicker for the selecting the date of
 * the evidence displayed, a ComboBox controls for selecting the filter 
 * attribute and a Suggest Box for the value e.g. 'username = asmith'.
 */
public class EvidenceFilterSuggestToolBar extends EvidenceViewPagingToolBar
{

	private List<String> 				m_FilterAttributes;
	
	protected ComboBox<BaseModelData> 	m_FilterAttrCombo;
	protected SuggestBox				m_FilterSuggestBox;
	protected MultiWordSuggestOracle	m_ValueOracle;
	private SelectionChangedListener<BaseModelData>	m_FilterAttributeComboListener;

	protected LabelToolItem 			m_FilterOperator;
	
	
	/**
	 * Creates a new filter toolbar for use in the specified evidence view.
	 * @param evidenceView evidence view for which this toolbar will be used.
	 */
	public EvidenceFilterSuggestToolBar(EvidenceView evidenceView)
	{
		this(evidenceView.getDataType(), evidenceView.getTimeFrame(), 
				evidenceView.getFilterableAttributes());
	}
	
	
	/**
	 * Creates a new filter toolbar for use in an evidence view.
	 * @param dataType the data type, such as 'apache_logs' or 'error_logs', which
	 * 	is used to identify the particular type of evidence data being displayed.
	 * @param timeFrame time frame of the list of evidence e.g. SECOND, MINUTE, HOUR.
	 * @param filterAttributes list of attributes on which the list of
	 * evidence can be filtered.
	 */
	public EvidenceFilterSuggestToolBar(String dataType, TimeFrame timeFrame, 
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

		    	  first();
		    	  populateFilterValues(selectedAttrName);
		      }
		};
		m_FilterAttrCombo.addSelectionChangedListener(m_FilterAttributeComboListener);
		
		m_FilterOperator = new LabelToolItem();
		m_FilterOperator.setStyleAttribute("text-align", "center");
		m_FilterOperator.setWidth("15px");
		
		m_ValueOracle = new MultiWordSuggestOracle();  

		m_FilterSuggestBox = new SuggestBox(m_ValueOracle);
		m_FilterSuggestBox.setWidth("130px");
		m_FilterSuggestBox.setPopupStyleName("prl-suggestBoxPopup");
		
		m_FilterSuggestBox.addSelectionHandler(new SelectionHandler<Suggestion>(){

			@Override
            public void onSelection(SelectionEvent<Suggestion> event)
            {
				GWT.log("Selection changed to " + m_FilterSuggestBox.getValue(), null);		
				String filterValue = m_FilterSuggestBox.getValue();
				if (filterValue.length() > 0)
				{
					m_Loader.setFilterValue(filterValue);
					m_FilterSuggestBox.getTextBox().setTitle(filterValue);
					m_FilterSuggestBox.setTitle(filterValue);
				}
				else
				{
					m_Loader.setFilterValue(null);
					m_FilterSuggestBox.getTextBox().setTitle("");
				}
				
				first();
            }
			
		});
		
		
		add(new LabelToolItem(ClientUtil.CLIENT_CONSTANTS.fieldFilter()));
		
		add(m_FilterAttrCombo);
		add(m_FilterOperator);
		add(new WidgetComponent(m_FilterSuggestBox));
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
				AsyncServiceLocator.getInstance().getEvidenceQueryService();
			queryService.getColumnValues(m_DataType, filterAttribute, Integer.MAX_VALUE,
					new ApplicationResponseHandler<List<String>>(){
	
		        public void uponFailure(Throwable caught)
		        {
			        MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
			        		ClientUtil.CLIENT_CONSTANTS.errorNoServerResponse(), null);
		        }
	
	
		        public void uponSuccess(List<String> filterValues)
		        {        	
 	
		        	for (String filterValue : filterValues)
		        	{
		        		m_ValueOracle.add(filterValue);
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
	 * Clears the oracle for the filter value SuggestBox.
	 */
	public void clearFilterValues()
	{
		m_FilterSuggestBox.setValue("", false);
		m_ValueOracle.clear();
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
			
			if (attribute == null)
			{
				m_FilterOperator.setLabel("");
				
				m_FilterAttrCombo.setValue(attributeStore.getAt(0));
			}
			else
			{
				// All filter attributes use the equality operator.
				m_FilterOperator.setLabel("=");
				
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
		if (value != null)
		{
			m_FilterSuggestBox.setValue(value, false);
		}
		else
		{
			m_FilterSuggestBox.setValue("", false);
		}
	}

}
