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
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientMessages;
import com.prelert.client.ClientUtil;
import com.prelert.data.Attribute;
import com.prelert.data.Evidence;
import com.prelert.data.TimeFrame;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.data.gxt.AttributeModel;
import com.prelert.data.gxt.IncidentModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.EvidenceQueryServiceAsync;
import com.prelert.service.IncidentQueryServiceAsync;


/**
 * An extension of the GXT Dialog to display a list of <code>AttributeModel</code>
 * objects. The dialog consists of a grid with Property and Value columns, and 
 * a button to close the dialog.
 * @see AttributeModel
 * @author Pete Harverson
 */
public class AttributeListDialog extends Dialog
{
	private static AttributeListDialog		s_Instance;
	private EvidenceQueryServiceAsync		m_EvidenceQueryService;
	
	private ListStore<AttributeModel> 		m_Store;
	private Grid<AttributeModel> 			m_Grid;
	
	private static String s_TimeColumnName = Evidence.getTimeColumnName(TimeFrame.SECOND);
	
	
	/**
	 * Returns the application-wide instance of the Attribute List dialog, 
	 * used for displaying the full details of a notification or time series feature.
	 * @return application-wide instance of the <code>AttributeListDialog</code>.
	 */
	public static AttributeListDialog getInstance()
	{
		if (s_Instance == null)
		{
			s_Instance = new AttributeListDialog();
		}
		
		return s_Instance;
	}
	
	
	/**
	 * Creates a new dialog for showing a list of attributes.
	 */
	private AttributeListDialog()
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
					
					// Escape < and > as otherwise grid cell is not displayed properly.
					String escapedValue = attributeValue.replace(">", "&gt;").replace("<", "&lt;");
					
					text += "<span qtip=\"";
					text += Format.htmlEncode(escapedValue);
					text += "\" qtitle=\"";
					text += Format.htmlEncode(attributeName);
					text += "\" >";
					text += escapedValue;
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
	    // Add custom style so that long textual values are wrapped in cells.
	    m_Grid = new Grid<AttributeModel>(m_Store, columnModel);
	    m_Grid.setLoadMask(true);
	    m_Grid.setBorders(true);
	    m_Grid.disableTextSelection(false);
	    m_Grid.getView().setAutoFill(true);
	    m_Grid.addStyleName("prl-multilineGrid");
	    
	    // This reference to QuickTip is needed to enable the tooltips on the value column.
	    @SuppressWarnings("unused")
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
	 * Loads the attributes into the dialog for the item of evidence with the specified id,
	 * and then brings the dialog to the front.
	 * @param evidenceId id of item of evidence to display in the dialog.
	 */
	public void showEvidenceAttributes(int evidenceId)
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
	        				GWT.log("AttributeDialog: Error parsing value of time field: " + msStr);
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
	        	show();
	    		toFront();
	        }
		});
	}
	
	
	/**
	 * Loads the attributes into the dialog for the activity containing the item of
	 * evidence with the specified id, and then brings the dialog to the front.
	 * @param evidenceId id of an item of evidence from the activity.
	 */
    public void showActivityAttributes(int evidenceId)
    {
		// Obtain the full details of the activity containing this item of evidence.
		ApplicationResponseHandler<IncidentModel> callback = 
			new ApplicationResponseHandler<IncidentModel>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log(ClientUtil.CLIENT_CONSTANTS.errorLoadingActivityData() + ": ", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						ClientUtil.CLIENT_CONSTANTS.errorLoadingActivityData(), null);
			}


			@Override
            public void uponSuccess(IncidentModel activity)
			{	
				if (activity != null)
				{
					Date activityTime = activity.getTime();
					String formattedTime = ClientUtil.formatTimeField(activityTime, TimeFrame.SECOND);
					
					ArrayList<AttributeModel> attributes = new ArrayList<AttributeModel>();
					attributes.add(new AttributeModel(ClientUtil.CLIENT_CONSTANTS.time(), formattedTime));
					attributes.add(new AttributeModel(ClientUtil.CLIENT_CONSTANTS.description(), activity.getDescription()));
					attributes.add(new AttributeModel(ClientUtil.CLIENT_CONSTANTS.anomalyScore(), 
							Integer.toString(activity.getAnomalyScore())));
					attributes.add(new AttributeModel(ClientUtil.CLIENT_CONSTANTS.evidenceId(), 
							Integer.toString(activity.getEvidenceId())));
					
					setHeading(ClientUtil.CLIENT_CONSTANTS.detailsOnActivityAtTime(formattedTime));
					setAttributes(attributes);
					show();
					toFront();
					
				}
			}
		};
		
		IncidentQueryServiceAsync incidentQueryService =
			AsyncServiceLocator.getInstance().getIncidentQueryService();
		incidentQueryService.getIncident(evidenceId, callback);
    }
    
    
    /**
     * Displays the attributes for the specified time series, bringing the dialog to the front.
     * @param config <code>TimeSeriesConfig</code> for which to display the attributes.
     * @param point optional <code>TimeSeriesDataPoint</code> parameter. If supplied the time
     * 	and value of the point are added to the list of attributes displayed in the dialog.
     */
    public void showTimeSeriesAttributes(TimeSeriesConfig config, TimeSeriesDataPoint point)
    {
    	ClientMessages messages = ClientUtil.CLIENT_CONSTANTS;
    	
    	// Display all the attributes of the selected series.
    	String metric = config.getMetric();
    	String source = config.getSource();
    	
    	ArrayList<AttributeModel> attributes = new ArrayList<AttributeModel>();
		attributes.add(new AttributeModel(messages.type(), config.getDataType()));
		attributes.add(new AttributeModel(messages.metric(), metric));
		
    	if (source != null)
    	{
    		attributes.add(new AttributeModel(messages.source(), source));
    	}
    	else
    	{
    		attributes.add(new AttributeModel(messages.source(), messages.allSources()));
    	}
		
    	List<Attribute> otherAttr = config.getAttributes();
    	if (otherAttr != null)
    	{
    		for (Attribute attribute : otherAttr)
    		{
    			attributes.add(new AttributeModel(
    					attribute.getAttributeName(), attribute.getAttributeValue()));
    		}
    	}
    	
    	if (point != null)
    	{
    		// Add attributes for the time and value.
	    	Date pointTime = new Date(point.getTime());
	    	
	    	attributes.add(new AttributeModel(messages.time(), 
	    			ClientUtil.formatTimeField(pointTime, TimeFrame.SECOND)));
	    	attributes.add(new AttributeModel(messages.value(), 
	    			NumberFormat.getDecimalFormat().format(point.getValue())));
    	}
    	
		setHeading(messages.detailsOnData(metric));
		setAttributes(attributes);
		show();
		toFront();
    }
    
}
