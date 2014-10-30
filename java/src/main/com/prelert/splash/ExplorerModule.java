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

import java.util.Date;

import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.MarginData;
import com.google.gwt.core.client.GWT;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.RequestViewEvent;
import com.prelert.client.explorer.MetricPathExplorer;
import com.prelert.client.gxt.ModuleComponent;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.EvidenceQueryServiceAsync;


/**
 * The Explorer UI module. This module allows the user to explore in depth the 
 * different types of data analysed by the Prelert engine via notification views
 * and time series views.
 * <p>
 * The container has two main sections:
 * <ul>
 * <li>A data sources tree which displays the types and sources of data analysed
 * by the Prelert engine.</li>
 * <li>A main work area tabbed panel holding the notification and time series views
 * for each data source type, plus the Analysed Data tab.</li>
 * </ul>
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>OpenCausalityViewClick</b> : RequestViewEvent<br>
 * <div>Fires after a link to the Analysis View is selected.</div>
 * <ul>
 * <li>component : this</li>
 * <li>model: notification or time series feature whose causality data is being requested</li>
 * </ul>
 * </dd>
 * 
 * </dl>
 * 
 * @author Pete Harverson
 */
public class ExplorerModule extends LayoutContainer implements ModuleComponent
{
	public static final String MODULE_ID = "explorer";

	private MetricPathExplorer			m_MetricPathExplorer;
	
	
	/**
	 * Creates the Explorer UI module.
	 */
	public ExplorerModule()
	{
		setLayout(new FitLayout());
	    
	    // Create components for the navigation area.
	    m_MetricPathExplorer = new MetricPathExplorer();
	    
	    // Listen for events to open causality views.
		Listener<RequestViewEvent<EvidenceModel>> rveListener = 
			new Listener<RequestViewEvent<EvidenceModel>>(){

            @Override
            public void handleEvent(RequestViewEvent<EvidenceModel> rve)
            {
            	// Set the module as the source and then propagate the event.
            	rve.setSource(ExplorerModule.this);
        		fireEvent(rve.getType(), rve);
            }
			
		};
		
		m_MetricPathExplorer.addListener(GXTEvents.OpenCausalityViewClick, rveListener);
	    
	    add(m_MetricPathExplorer, new MarginData(10));
	    m_MetricPathExplorer.loadAnalysedData();   
	}
	

	@Override
    public Component getComponent()
    {
	    return this;
    }

	
	@Override
    public String getModuleId()
    {
	    return MODULE_ID;
    }

	
	/**
	 * Shows a summary of data that has been analysed by the Prelert engine.
	 */
	public void showAnalysedData()
	{
		m_MetricPathExplorer.loadAnalysedData();
	}
	
	
	/**
	 * Displays the notification or time series feature corresponding to the
	 * specified item of evidence data in the module.
	 * @param data notification or time series feature to be displayed.
	 */
	public void showEvidence(EvidenceModel evidence)
	{
		m_MetricPathExplorer.loadForEvidence(evidence);
	}
	
	
	/**
	 * Displays the notification or time series feature with the specified id
	 * in the module.
	 * @param evidenceId id of notification or time series feature to display.
	 */
	public void showEvidence(int evidenceId)
	{
		final int evId = evidenceId;
		
		// Obtain the full evidence record for this id.
		ApplicationResponseHandler<EvidenceModel> evidenceCallback = 
			new ApplicationResponseHandler<EvidenceModel>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						ClientUtil.CLIENT_CONSTANTS.errorLoadingEvidenceData(), null);
			}


			@Override
            public void uponSuccess(EvidenceModel evidence)
			{	
				if (evidence != null)
				{
					showEvidence(evidence);
				}
				else
				{
					GWT.log(ClientUtil.CLIENT_CONSTANTS.errorNoEvidenceForId(evId));
					MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
							ClientUtil.CLIENT_CONSTANTS.errorNoEvidenceForId(evId), null);
				}
			}
		};
		
		EvidenceQueryServiceAsync queryService =
			AsyncServiceLocator.getInstance().getEvidenceQueryService();
		queryService.getEvidenceSingle(evidenceId, evidenceCallback);
	}
	
	
	/**
	 * Displays the specified time series in the module, loading the data 30 minutes
	 * either side of the specified time.
	 * @param timeSeries configuration object defining the time series to display.
	 * @param time date/time of data to load.
	 */
	public void showTimeSeries(TimeSeriesConfig timeSeries, Date time)
	{
		m_MetricPathExplorer.loadForTimeSeries(timeSeries, time);
	}
	
	
	/**
	 * Resets the view in the module, loading the summary of analysed data.
	 */
	public void resetView()
	{
		showAnalysedData();
		
		// Clear the current load time in the notification and time series widgets.
		m_MetricPathExplorer.clearLoadTime();
	}
}
