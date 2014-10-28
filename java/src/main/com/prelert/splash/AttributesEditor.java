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

package com.prelert.splash;

import java.util.ArrayList;
import java.util.List;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.FormButtonBinding;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.ListModelPropertyEditor;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;

import com.prelert.client.ClientUtil;
import com.prelert.client.event.AttributesEditorEvent;
import com.prelert.data.gxt.AttributeModel;


/**
 * Ext GWT (GXT) widget for editing a set of attributes. The user selects the value
 * for an attribute using a Combo box control, with buttons at the bottom of the
 * container for applying or cancelling changes. 
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>Submit</b> : AttributesEditorEvent(attributesEditor, oldAttributes, attributes)<br>
 * <div>Fires when the user presses the 'Apply' button to submit changes</div>
 * <ul>
 * <li>attributesEditor : this</li>
 * <li>oldAttributes : the set of attributes before the current set of changes</li>
 * <li>attributes : the new set of attributes</li>
 * </ul>
 * </dd>
 * 
 * </dl>
 * 
 * @author Pete Harverson
 */
public class AttributesEditor extends VerticalPanel
{
	private FormPanel		m_FormPanel;
	private List<ComboBox<AttributeModel>>	m_AttributeControls;
	
	private List<AttributeModel>	m_StoredSelections;		// List at last set or apply.
	
	protected static String		m_QTipTemplate = "<tpl for=\".\"><div class=\"prl-combo-list-item\" " +
			"<tpl if=\"attributeValue != null\">qtip=\"{attributeValue}\" qtitle=\"\">{attributeValue}</tpl>" +
			"<tpl if=\"attributeValue == null\">qtip=\"" + ClientUtil.CLIENT_CONSTANTS.optionAll() + 
				"\" qtitle=\"\">" + ClientUtil.CLIENT_CONSTANTS.optionAll() + "</tpl>" +
			"</div></tpl>";
	
	
	/**
	 * Creates a new attributes editor.
	 */
	public AttributesEditor()
	{
		m_AttributeControls = new ArrayList<ComboBox<AttributeModel>>();
		m_StoredSelections = new ArrayList<AttributeModel>();
		
		initComponents();
	}
	
	
	/**
	 * Creates and initialises the components in the attributes editor.
	 */
	protected void initComponents()
	{
		addStyleName("x-border-layout-ct");
		setScrollMode(Scroll.AUTO);
		setSpacing(10);
		
		// Create the FormPanel which will hold the combo boxes for 
		// editing attribute values.
		m_FormPanel = new FormPanel();
		m_FormPanel.setHeaderVisible(false);
		m_FormPanel.setBodyBorder(false);
		m_FormPanel.setPadding(20);
		
		
		// TODO - make label width adjust to width of attribute labels?
		FormLayout formLayout = new FormLayout();   
		formLayout.setLabelWidth(100);  
		m_FormPanel.setLayout(formLayout); 
	    
	    
	    // Add a listener for Resize events to size the attribute combo boxes
	    // to the available width.
	    Listener<ComponentEvent> resizeListener = new Listener<ComponentEvent>(){
			
			public void handleEvent(ComponentEvent be)
            {
				for (ComboBox<AttributeModel> combo : m_AttributeControls)
				{
					combo.setWidth(getWidth()- 185);
				}
				m_FormPanel.layout(true);
            }
		};
		addListener(Events.Resize, resizeListener);
		
		
	    // Add Apply and Cancel buttons.
	    Button applyButton = new Button(ClientUtil.CLIENT_CONSTANTS.apply());
	    applyButton.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
            {	
				List<AttributeModel> attributes = getSelectedAttributes();
				AttributesEditorEvent event = new AttributesEditorEvent(AttributesEditor.this);
				
				// Copy the current stored selections to pass as the old attributes.
				List<AttributeModel> oldAttributes = new ArrayList<AttributeModel>();
				for (AttributeModel storedAttribute : m_StoredSelections)
				{
					oldAttributes.add(new AttributeModel(
							storedAttribute.getAttributeName(), storedAttribute.getAttributeValue()));
				}
				
				event.setOldAttributes(oldAttributes);
				event.setAttributes(attributes);
				
				// Store the new selections - copy the Combo selected attributes.
				m_StoredSelections.clear();
				for (AttributeModel attribute : attributes)
				{
					m_StoredSelections.add(new AttributeModel(
							attribute.getAttributeName(), attribute.getAttributeValue()));
				}
				
				fireEvent(Events.Submit, event);
            }
		});
	    
	    // Add a FormButtonBinding to disable the Apply button if fields are blank.
	    FormButtonBinding binding = new FormButtonBinding(m_FormPanel);   
	    binding.addButton(applyButton);
	    
	    Button cancelButton = new Button(ClientUtil.CLIENT_CONSTANTS.cancel());
	    cancelButton.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
            {
				String attributeName;
				AttributeModel storedSelection;
				for (ComboBox<AttributeModel> combo : m_AttributeControls)
				{
					attributeName = combo.getFieldLabel();
					storedSelection = getStoredSelection(attributeName);
					if (storedSelection != null)
					{
						setSelectedAttribute(attributeName, 
								storedSelection.getAttributeValue());
					}
					else
					{
						combo.clearSelections();
					}
				}
            }
		});
	    
	    m_FormPanel.setButtonAlign(HorizontalAlignment.CENTER);   
	    m_FormPanel.addButton(applyButton);   
	    m_FormPanel.addButton(cancelButton); 
	    
	    add(m_FormPanel);
	}
	
	
	/**
	 * Adds an attribute to the editor.
	 * @param attributeName the name of the attribute.
	 * @param attributeValues list of possible values for the attribute, which
	 * 		may be an empty list.
	 */
	public void addAttribute(String attributeName, List<String> attributeValues)
	{
		ComboBox<AttributeModel> combo = new ComboBox<AttributeModel>();
		combo.setFieldLabel(attributeName);
		combo.setStore(new ListStore<AttributeModel>());
		combo.setListStyle("prl-combo-list");
		combo.addInputStyleName("prl-combo-selected");
		combo.setEditable(false);
		combo.setAllowBlank(false);
		combo.setEmptyText(ClientUtil.CLIENT_CONSTANTS.optionSelect());
		combo.setDisplayField("attributeValue");
		combo.setTriggerAction(TriggerAction.ALL); 
		
		// Add a custom PropertyEditor to display an 'All' option.
		combo.setPropertyEditor(new ListModelPropertyEditor<AttributeModel>()
		{
			public String getStringValue(AttributeModel attribute)
			{
				String attrValue = attribute.getAttributeValue();
				if (attrValue != null)
				{
					return attrValue;
				}
				else
				{
					return ClientUtil.CLIENT_CONSTANTS.optionAll();
				}
			}
			
			@Override
			public AttributeModel convertStringValue(String value)
			{
				// Override default to match the value displayed in the ComboBox 
				// text field to the values of the attributes in the store.	
				for (AttributeModel d : models)
				{
					String val = d.getAttributeValue();
					if (value.equals(val != null ? val.toString() : ClientUtil.CLIENT_CONSTANTS.optionAll()))
					{
						return d;
					}
				}
				
				return null;
			}
		});
			 
	       
	    // Specify a custom template for the ComboBox list items which uses
		// Quicktip tooltips, allowing the use to read long items.
		combo.setTemplate(m_QTipTemplate);
	    
	    ListStore<AttributeModel> valuesStore = combo.getStore();
	    for (String attributeValue : attributeValues)
    	{
    		valuesStore.add(new AttributeModel(attributeName, attributeValue));
    	}
	    
	    combo.setWidth(getWidth()- 185);
	    
	    m_FormPanel.add(combo);
	    m_AttributeControls.add(combo);
	}
	
	
	/**
	 * Sets the list of possible values for the attribute with the specified name,
	 * clearing any existing values set for the attribute.
	 * @param attributeName name of the attribute whose list of values is being set.
	 * @param attributeValues list of possible values for the attribute.
	 */
	public void setAttributeValues(String attributeName, List<String> attributeValues)
	{
		ComboBox<AttributeModel> combo = getAttributeComboBox(attributeName);
		if (combo != null)
		{
			if (combo.getFieldLabel().equals(attributeName))
			{
				// Clear the current store and selection.
				AttributeModel storedSelection = getStoredSelection(attributeName);
				if (storedSelection != null)
				{
					m_StoredSelections.remove(storedSelection);
				}
				
				combo.clearSelections();	
				ListStore<AttributeModel> valuesStore = combo.getStore();
				valuesStore.removeAll();
				
				for (String attributeValue : attributeValues)
	        	{
					valuesStore.add(new AttributeModel(attributeName, attributeValue));
	        	}
			}
		}
	}
	
	
	/**
	 * Returns the list of possible values for the attribute with the given name.
	 * @param attributeName name of the attribute.
	 * @return the list of attribute values.
	 */
	public List<String> getAttributeValues(String attributeName)
	{
		ArrayList<String> values = new ArrayList<String>();
		
		ComboBox<AttributeModel> combo = getAttributeComboBox(attributeName);
		if (combo != null)
		{
			ListStore<AttributeModel> valuesStore = combo.getStore();
			for (AttributeModel attribute : valuesStore.getModels())
			{
				values.add(attribute.getAttributeValue());
			}
		}
		
		return values;
	}
	
	
	/**
	 * Removes all attributes from the editor.
	 */
	public void removeAllAttributes()
	{
		for (ComboBox<AttributeModel> combo : m_AttributeControls)
		{
			m_FormPanel.remove(combo);
		}
		
		m_AttributeControls.clear();
		m_StoredSelections.clear();
	}
	
	
	/**
	 * Returns a list of all the attributes that have been selected.
	 * @return the selected attributes.
	 */
	public List<AttributeModel> getSelectedAttributes()
	{
		ArrayList<AttributeModel> selections = new ArrayList<AttributeModel>();
		
		for (ComboBox<AttributeModel> combo : m_AttributeControls)
		{
			List<AttributeModel> selectedItems = combo.getSelection();
			if (selectedItems.size() > 0)
			{
				selections.add(selectedItems.get(0));
			}
		}
		
		return selections;
	}
	
	
	/**
	 * Returns the value that is currently selected for the attribute with the
	 * specified name.
	 * @param attributeName name of the attribute for which to return the selected value.
	 * @return the currently selected value, or <code>null</code> if the 'All'
	 * 		option is selected.
	 */
	public String getSelectedAttribute(String attributeName)
	{
		String selectedValue = null;
		
		ComboBox<AttributeModel> combo = getAttributeComboBox(attributeName);
		
		if (combo != null)
		{
			List<AttributeModel> selectedItems = combo.getSelection();
			if (selectedItems.size() > 0)
			{
				selectedValue = selectedItems.get(0).getAttributeValue();
			}
		}
		
		return selectedValue;
	}
	
	
	/**
	 * Sets the selected value of the attribute with the specified name.
	 * @param attributeName the name of the attribute whose selected value is being set.
	 * @param attributeValue the value of the attribute to select.
	 */
	public void setSelectedAttribute(String attributeName, String attributeValue)
	{
		ComboBox<AttributeModel> combo = getAttributeComboBox(attributeName);
		
		if (combo != null)
		{
			if (combo.getFieldLabel().equals(attributeName))
			{
				ListStore<AttributeModel> valuesStore = combo.getStore();
				for (AttributeModel attribute : valuesStore.getModels())
				{
					if (attributeValue != null)
					{
						if (attributeValue.equals(attribute.getAttributeValue()))
						{
							combo.setValue(attribute);	
							break;
						}
					}
					else
					{
						if (attribute.getAttributeValue() == null)
						{
							// An attribute value of 'null' indicates 'All values'.
							combo.setValue(attribute);		
							break;
						}
					}
				}
			}

			setStoredSelection(attributeName, attributeValue);
		}
	}
	
	
	/**
	 * Returns the ComboBox control for the attribute with the specified name.
	 * @param attributeName attribute name.
	 * @return the control used to select the attribute value.
	 */
	protected ComboBox<AttributeModel> getAttributeComboBox(String attributeName)
	{
		ComboBox<AttributeModel> attributeCombo = null;
		
		for (ComboBox<AttributeModel> combo : m_AttributeControls)
		{
			if (combo.getFieldLabel().equals(attributeName))
			{
				attributeCombo = combo;
				break;
			}
		}
		
		return attributeCombo;
	}
	
	
	/**
	 * Records the stored selection for an attribute.
	 * @param attributeName name of the attribute to store.
	 * @param attributeValue value of the attribute to store.
	 */
	protected void setStoredSelection(String attributeName, String attributeValue)
	{
		AttributeModel storedSelection = getStoredSelection(attributeName);
		if (storedSelection != null)
		{
			storedSelection.setAttributeValue(attributeValue);
		}
		else
		{
			m_StoredSelections.add(new AttributeModel(attributeName, attributeValue));
		}
	}
	
	
	/**
	 * Returns the stored selection for the attribute with the specified name.
	 * @param attributeName name of the attribute.
	 * @return the stored attribute, or <code>null</code> if no attribute is stored.
	 */
	protected AttributeModel getStoredSelection(String attributeName)
	{
		AttributeModel storedSelection = null;
		
		for (AttributeModel attribute : m_StoredSelections)
		{
			if (attribute.getAttributeName().equals(attributeName))
			{
				storedSelection = attribute;
				break;
			}
		}
		
		return storedSelection;
	}
}
