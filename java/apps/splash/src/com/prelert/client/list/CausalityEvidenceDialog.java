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

import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.core.client.GWT;
import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.RequestViewEvent;
import com.prelert.data.TimeFrame;
import com.prelert.data.ViewTool;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.data.gxt.ProbableCauseModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.CausalityQueryServiceAsync;


/**
 * An extension of the Ext GWT Dialog for paging through a list of notifications 
 * with a particular description from a probable cause incident
 * The dialog consists of an evidence grid and a 'Close' button.
 * @author Pete Harverson
 */
public class CausalityEvidenceDialog extends Dialog
{
	private CausalityQueryServiceAsync 		m_CausalityQueryService;
	
	private EvidenceGridPanel 				m_EvidencePanel;
	private CausalityEvidencePagingLoader 	m_Loader;
	
	
	/**
	 * Creates a dialog for paging through notifications matching a particular
	 * description from an incident.
	 */
	public CausalityEvidenceDialog()
	{
		setBodyBorder(false);
		setSize(850, 510);
		setLayout(new FitLayout());
		
		setButtons(Dialog.CLOSE);   
	    setHideOnButtonClick(true); 
	    
	    // Create the evidence grid and add a 'Show Data' tool to the context menu.
	    m_EvidencePanel = new EvidenceGridPanel();
	    m_EvidencePanel.setHeaderVisible(false);
	    ViewTool showDataTool = new ViewTool();
	    showDataTool.setName(ClientUtil.CLIENT_CONSTANTS.showData());
	    m_EvidencePanel.addGridTool(showDataTool);
	    
	    // Listen for events to 'Show Data'.
		Listener<RequestViewEvent<EvidenceModel>> rveListener = new Listener<RequestViewEvent<EvidenceModel>>(){

            public void handleEvent(RequestViewEvent<EvidenceModel> rve)
            {
            	// Propagate the event.
        		fireEvent(rve.getType(), rve);
            }
			
		};
		
		m_EvidencePanel.addListener(GXTEvents.OpenViewClick, rveListener);
	    
	    add(m_EvidencePanel);
	    
	    
	    // Create the RpcProxy and PagingLoader to populate the list.
	    m_CausalityQueryService = AsyncServiceLocator.getInstance().getCausalityQueryService();
	    CausalityEvidencePagingRpcProxy proxy = new CausalityEvidencePagingRpcProxy();
		m_Loader = new CausalityEvidencePagingLoader(proxy);
		m_Loader.setTimeFrame(TimeFrame.SECOND);
		m_Loader.setRemoteSort(true);
		m_EvidencePanel.bind(m_Loader);
	        
	}

	
	/**
	 * Sets the Probable Cause whose notifications are to be displayed in the dialog.
	 * The dialog will page through notifications with the same data type and description
	 * as the item of evidence in the supplied probable cause.
	 * @param probableCause Probable Cause whose notifications are to be displayed.
	 */
	public void setProbableCause(ProbableCauseModel probableCause)
    {
		final ProbableCauseModel probCause = probableCause;
		
		// Get the list of columns to use for the results grid, then create the grid.
		ApplicationResponseHandler<List<String>> callback = 
			new ApplicationResponseHandler<List<String>>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log("Error loading evidence data for probable cause " + probCause, caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
						ClientUtil.CLIENT_CONSTANTS.errorLoadingEvidenceData(), null);
			}


			@Override
            public void uponSuccess(List<String> columns)
			{
				m_EvidencePanel.setColumns(columns);
				
				m_Loader.setEvidenceId(probCause.getEvidenceId());
				m_EvidencePanel.load();
				setHeading(ClientUtil.CLIENT_CONSTANTS.causalityNotificationsHeading(
						probCause.getDescription()));
			}
		};
		
		
		m_CausalityQueryService.getEvidenceColumns(probCause.getDataSourceName(), callback);
    }
}
