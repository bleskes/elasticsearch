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

package com.prelert.splash;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FieldEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.form.TriggerField;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.CenterLayout;
import com.google.gwt.event.dom.client.KeyCodes;

import com.prelert.client.ClientUtil;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.RequestViewEvent;
import com.prelert.client.gxt.ModuleComponent;
import com.prelert.client.list.EvidenceSearchPanel;
import com.prelert.data.gxt.EvidenceModel;


/**
 * The Search module, allowing the user to search for notifications or time series
 * features whose attributes contain a specified string of text. The module consists
 * of a text box for entering the search text, with a results grid below.
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>OpenCausalityViewClick</b> : RequestViewEvent<br>
 * <div>Fires after a link to the probable cause view is selected.</div>
 * <ul>
 * <li>component : this</li>
 * <li>model: notification or time series feature whose causality data is being requested</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>OpenViewClick</b> : RequestViewEvent<br>
 * <div>Fires after a link to a data view is selected.</div>
 * <ul>
 * <li>component : this</li>
 * <li>model: notification or time series feature whose data is being requested</li>
 * </ul>
 * </dd>
 * 
 * </dl>
 * 
 * @author Pete Harverson
 */
public class SearchModule extends LayoutContainer implements ModuleComponent
{
	
	private TriggerField<String>		m_SearchField;
	private EvidenceSearchPanel 		m_SearchPanel;
	
	
	/**
	 * Creates the Search UI module.
	 */
	public SearchModule()
	{	
		// Add a search trigger field at the top.
		m_SearchField = new TriggerField<String>();
	    m_SearchField.setTriggerStyle("x-form-search-trigger");
	    m_SearchField.setWidth(300);
	    
	    m_SearchField.addListener(Events.TriggerClick, new Listener<FieldEvent>(){

			@Override
			public void handleEvent(FieldEvent fe)
            {
				m_SearchPanel.setContainsText(m_SearchField.getValue());
				m_SearchPanel.load();
            }
	    	
	    });
	    m_SearchField.addListener(Events.KeyPress, new Listener<FieldEvent>(){

			@Override
            public void handleEvent(FieldEvent fe)
            {
	           	if (fe.getKeyCode() == KeyCodes.KEY_ENTER)
	           	{
	           		m_SearchPanel.setContainsText(m_SearchField.getValue());
	           		m_SearchPanel.load();
	           	}
            }
	    	
	    });
	    
	    LayoutContainer searchFieldPanel = new LayoutContainer();
		searchFieldPanel.setLayout(new CenterLayout());
	    searchFieldPanel.add(m_SearchField);
	    
	    
	    // Add the EvidenceSearchPanel grid for displaying the results.
	    m_SearchPanel = new EvidenceSearchPanel();
	    m_SearchPanel.setSize(700, 360); 
		
		// Listen for events to open notification, time series and causality views.
		Listener<RequestViewEvent<EvidenceModel>> rveListener = new Listener<RequestViewEvent<EvidenceModel>>(){

            public void handleEvent(RequestViewEvent<EvidenceModel> rve)
            {
            	// Set the module as the source and then propagate the event.
            	rve.setSource(SearchModule.this);
        		fireEvent(rve.getType(), rve);
            }
			
		};
		
		m_SearchPanel.addListener(GXTEvents.OpenViewClick, rveListener);
		m_SearchPanel.addListener(GXTEvents.OpenCausalityViewClick, rveListener);
		
		
		// Add a LoadListener to enable/disable the Search trigger field 
		// before and after load operations.
		m_SearchPanel.addLoadListener(new LoadListener(){
			
			@Override
            public void loaderBeforeLoad(LoadEvent le)
			{
				m_SearchField.setEnabled(false);
			}
			
			
			@Override
			public void loaderLoad(LoadEvent le) 
			{
				m_SearchField.setEnabled(true);
			}


			@Override
            public void loaderLoadException(LoadEvent le)
			{
				m_SearchField.setEnabled(true);
			}
			
		});
		
		BorderLayout layout = new BorderLayout();   
		layout.setContainerStyle("prl-viewport");
		setLayout(layout); 
		
		BorderLayoutData northData = new BorderLayoutData(LayoutRegion.NORTH, 70);   
        northData.setMargins(new Margins(0)); 
		
		BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);   
		centerData.setMargins(new Margins(0));
		
		add(searchFieldPanel, northData);
		add(m_SearchPanel, centerData);
		
	}
	
	
	/**
     * Runs a search for the specified text within one or more of the evidence 
     * attribute values for the given data type.
     * @param containsText the text to search for within attribute values.
     * @param dataTypeName the name of the data type to be searched, 
	 * 		or <code>null</code> to search across all data types.
     */
	public void runSearch(String containsText, String dataType)
	{
		m_SearchField.setValue(containsText);
		m_SearchPanel.setContainsText(containsText);
		m_SearchPanel.setDataType(dataType);
		
		m_SearchPanel.load();
	}
	

	@Override
    public Component getComponent()
    {
		return this;
    }

	
	/**
	 * Returns id for the Search module.
	 * @return the Search module ID.
	 */
	@Override
    public String getModuleId()
    {
		return ClientUtil.CLIENT_CONSTANTS.search();
    }

}
