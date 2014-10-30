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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.Format;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.tips.QuickTip;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.data.TimeFrame;
import com.prelert.data.gxt.AttributeModel;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.EvidenceQueryServiceAsync;


/**
 * An extension of the Ext GWT Dialog to display the complete list of attributes
 * for an item of evidence. The dialog consists of a grid with Property and Value 
 * columns, and a button to close the dialog.
 * @author Pete Harverson
 */
public class EvidenceAttributesDialog extends Dialog
{
	private EvidenceQueryServiceAsync		m_EvidenceQueryService;
	
	private ListStore<AttributeModel> 		m_Store;
	private Grid<AttributeModel> 			m_Grid;
	
	private static String s_TimeColumnName = EvidenceModel.getTimeColumnName(TimeFrame.SECOND);
	
	
	/**
	 * Creates a new dialog for showing a list of attributes.
	 */
	public EvidenceAttributesDialog()
	{
		m_EvidenceQueryService = AsyncServiceLocator.getInstance().getEvidenceQueryService();
		
	    setBodyBorder(false);   
	    setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.icon_table()));   
	    setHeading(ClientUtil.CLIENT_CONSTANTS.summary());	// Default heading normally set on opening.
	    setWidth(450);   
	    setHeight(500);   
	    setButtons(Dialog.CLOSE);   
	    setHideOnButtonClick(true); 
	    setLayout(new FitLayout());
	    
	    m_Store = new ListStore<AttributeModel>();
		
	    ColumnConfig propColumn = new ColumnConfig("attributeName", 
	    		ClientUtil.CLIENT_CONSTANTS.attribute(), 150);
	    ColumnConfig valueColumn = new ColumnConfig("attributeValue", 
	    		ClientUtil.CLIENT_CONSTANTS.value(), 250);
	    
	    // Set a custom GridCellRenderer to display tooltips on the value column.
	    valueColumn.setRenderer(new GridCellRenderer<AttributeModel>() {

			@Override
            public Object render(AttributeModel model, String property,
                    ColumnData config, int rowIndex, int colIndex,
                    ListStore<AttributeModel> store, Grid<AttributeModel> grid)
            {
				String text = "";
				
				if (model.get(property) != null)
				{
					String attributeName = model.getAttributeName();
					String attributeValue = model.getAttributeValue();
					
					text += "<span qtip=\"";
					text += Format.htmlEncode(attributeValue);
					text += "\" qtitle=\"";
					text += Format.htmlEncode(attributeName);
					text += "\" >";
					text += attributeValue;
					text += "</span>";
				}
				
				return text;
            }
	    });

	    List<ColumnConfig> config = new ArrayList<ColumnConfig>();
	    config.add(propColumn);
	    config.add(valueColumn);

	    ColumnModel columnModel = new ColumnModel(config);
		
	    // Create the Grid itself and configure the QuickTip for value tooltips.
	    m_Grid = new Grid<AttributeModel>(m_Store, columnModel);
	    m_Grid.setLoadMask(true);
	    m_Grid.setBorders(true);
	    m_Grid.getView().setAutoFill(true);
	    QuickTip qtip = new QuickTip(m_Grid);

	    add(m_Grid);
	    
	}
	
	
	/**
	 * Sets the list of attributes for display in the dialog.
	 * @param data list of AttributeModel objects for display.
	 */
	public void setAttributes(List<AttributeModel> data)
	{
		m_Store.removeAll();
		m_Store.add(data);
	}
	
	
	/**
	 * Loads the data into the dialog for the item of evidence with the specified id.
	 * @param evidenceId id of item of evidence to display in the dialog.
	 */
	public void setEvidenceId(int evidenceId)
	{
		final int evId = evidenceId;
		
		m_Store.removeAll();
		
		m_EvidenceQueryService.getEvidenceAttributes(evidenceId, 
				new ApplicationResponseHandler<List<AttributeModel>>(){

	        public void uponFailure(Throwable caught)
	        {
	        	MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
		                ClientUtil.CLIENT_CONSTANTS.errorLoadingEvidenceData(), null);
	        }


	        public void uponSuccess(List<AttributeModel> attributes)
	        {
	        	for (AttributeModel attribute : attributes)
	        	{
	        		if (attribute.getAttributeName().equals(s_TimeColumnName))
	        		{
	        			// Time field is transported as a long 
	        			// - convert to a Date and format.
	        			String msStr = null;
	        			try
	        			{
	        				msStr = attribute.getAttributeValue();
	        				long ms = Long.parseLong(msStr);
	        				
	        				Date time = new Date(ms);
	        				String formattedTime = ClientUtil.formatTimeField(time, TimeFrame.SECOND);
	        				attribute.setAttributeValue(formattedTime);
	        			}
	        			catch (NumberFormatException nfe)
	        			{
	        				GWT.log("EvidenceAttributesDialog: Error parsing value of time field: " + msStr);
	        			}
	        			
	        		}
	        		else if (attribute.getAttributeName().equals("type"))
	        		{
        				// Extract the data type to use in the dialog heading.
	        			String dataType = attribute.getAttributeValue();
	        			setHeading(ClientUtil.CLIENT_CONSTANTS.evidenceDetailsHeading(
	    	        			dataType, evId));
	        		}

	        	}
	        	
	        	m_Store.add(attributes);
	        }
		});
	}
}
