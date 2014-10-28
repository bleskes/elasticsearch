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

import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionEvent;
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
import com.prelert.client.gxt.ModuleComponent;
import com.prelert.client.incident.ActivityListDialog;
import com.prelert.data.CausalityView;
import com.prelert.data.TimeFrame;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.data.gxt.IncidentModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.IncidentQueryServiceAsync;


/**
 * The Analysis UI module, presenting the results of Prelert's analytics.
 * The module holds a chart displaying the causal relationships in an activity,
 * providing a graphical representation of the correlated notifications and time
 * series features. Below the chart are two grid components - one summarising the
 * causality data by attribute, and one allowing the user to analyse and select
 * the notifications and time series displayed on the chart.
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>OpenActivityViewClick</b> : RequestViewEvent&lt;EvidenceModel&gt;<br>
 * <div>Fires after a link to the Activity View is selected.</div>
 * <ul>
 * <li>component : this</li>
 * <li>model: notification or feature whose analysis is being requested</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>OpenNotificationViewClick</b> : RequestViewEvent&lt;EvidenceModel&gt;<br>
 * <div>Fires after a link to a notification view is selected.</div>
 * <ul>
 * <li>component : this</li>
 * <li>model: notification whose data is being requested</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>OpenTimeSeriesViewClick</b> : RequestViewEvent&lt;TimeSeriesConfig&gt;<br>
 * <div>Fires after a link to a time series view is selected.</div>
 * <ul>
 * <li>component : this</li>
 * <li>model: TimeSeriesConfig for the time series that is being requested</li>
 * <li>openAtTime: time to display</li> 
 * </ul>
 * </dd>
 * 
 * </dl>
 * 
 * @author Pete Harverson
 */
public class AnalysisModule extends LayoutContainer implements ModuleComponent
{
	
	private CausalityExplorerWidget		m_CausalityExplorer;
	
	private ActivityListDialog 			m_ActivityListDialog;	
	
	public static final String MODULE_ID = "analysis";
	
	
	/**
	 * Creates the analysis module.
	 */
	public AnalysisModule()
	{
		setLayout(new FitLayout());
		
		// Create the Causality Explorer, and add listeners for 'Open Xxxx view' events.
		m_CausalityExplorer = new CausalityExplorerWidget();
		
		
		// Listen for events to open activity, notification and time series views.
		Listener<RequestViewEvent<?>> rveListener = new Listener<RequestViewEvent<?>>(){

            public void handleEvent(RequestViewEvent<?> rve)
            {
            	// Set the module as the source and then propagate the event.
            	rve.setSource(AnalysisModule.this);
        		fireEvent(rve.getType(), rve);
            }
			
		};
		
		m_CausalityExplorer.addListener(GXTEvents.OpenActivityViewClick, rveListener);
		m_CausalityExplorer.addListener(GXTEvents.OpenNotificationViewClick, rveListener);
		m_CausalityExplorer.addListener(GXTEvents.OpenTimeSeriesViewClick, rveListener);
		
		m_CausalityExplorer.addListener(GXTEvents.ShowActivitySelect, new Listener<ButtonEvent>()
		{
			@Override
            public void handleEvent(ButtonEvent be)
            {
				showSelectActivityDialog();
            }
		});
		
		m_CausalityExplorer.addListener(GXTEvents.ShowShareLinkClick, new Listener<ComponentEvent>()
		{
			@Override
            public void handleEvent(ComponentEvent ce)
            {
				String linkToURL = SyncServiceLocator.getOpenInModuleURL(MODULE_ID, 
						m_CausalityExplorer.getEvidence().getId());
				
				ShowLinkDialog linkDialog = ShowLinkDialog.getInstance();
				linkDialog.setLink(ClientUtil.CLIENT_CONSTANTS.linkToModule(MODULE_ID), 
						ClientUtil.CLIENT_CONSTANTS.linkToAnalysisInfo(), linkToURL);
				linkDialog.show();
            }
		});
		
		add(m_CausalityExplorer, new MarginData(10));
	}
	

	@Override
	public String getModuleId()
	{
		return MODULE_ID;
	}


	@Override
	public Component getComponent()
	{
		return this;
	}
	
	
	/**
	 * Presents the analysis the specified activity.
	 * @param activity the activity to analyse.
	 */
	public void analyseActivity(IncidentModel activity)
	{
		// Create an item of evidence, with ID, time and description
		// set to those of the selection activity.
		EvidenceModel evidence = new EvidenceModel();
		evidence.setId(activity.getEvidenceId());
		evidence.setDescription(activity.getDescription());
		evidence.setTime(TimeFrame.SECOND, activity.getTime());
		analyseEvidence(evidence);
	}
	
	
	/**
	 * Presents the analysis of the activity containing the specified item of evidence.
	 * @param evidenceId ID of the notification or time series feature whose causal
	 * 	relationships are to be analysed in the module.
	 */
	public void analyseActivity(int evidenceId)
	{
		final int evId = evidenceId;
		
		// Obtain the activity containing this item of evidence.
		ApplicationResponseHandler<IncidentModel> callback = 
			new ApplicationResponseHandler<IncidentModel>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log(ClientUtil.CLIENT_CONSTANTS.errorLoadingActivityData() + ": ", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						ClientUtil.CLIENT_CONSTANTS.errorLoadingActivityData(), null);

				unmask();
			}


			@Override
            public void uponSuccess(IncidentModel activity)
			{	
				if (activity != null)
				{
					analyseActivity(activity);
				}
				else
				{
					GWT.log(ClientUtil.CLIENT_CONSTANTS.errorNoActivityForId(evId));
					MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
							ClientUtil.CLIENT_CONSTANTS.errorNoActivityForId(evId), null);
				}
			}
		};
		
		IncidentQueryServiceAsync incidentQueryService =
			AsyncServiceLocator.getInstance().getIncidentQueryService();
		incidentQueryService.getIncident(evidenceId, callback);
	}
	
	
	/**
	 * Presents the analysis of the specified item of evidence.
	 * <p>The following evidence properties must be set:
     * <ul>
     * <li>id - to load the causality data</li>
     * <li>time - to set the time marker and window of data loaded</li>
     * <li>description - to set the widget title</li>
     * </ul>
	 * @param evidence the notification or time series feature whose causal
	 * 	relationships are to be analysed in the module.
	 */
	public void analyseEvidence(EvidenceModel evidence)
	{
		analyseEvidence(evidence, null);
	}
	
	
	/**
	 * Presents the analysis of the specified item of evidence, grouping 
	 * causal data for summary by the specified attribute.
	 * <p>The following evidence properties must be set:
     * <ul>
     * <li>id - to load the causality data</li>
     * <li>time - to set the time marker and window of data loaded</li>
     * <li>description - to set the widget title</li>
     * </ul>
	 * @param evidence the notification or time series feature whose causal
	 * 	relationships are to be analysed in the module.
	 * @param summaryGroupBy the name of the attribute by which causality data
	 * 		should be aggregated and summarised. If <code>null</code> the module 
	 * 		will maintain the current 'Group by' setting, or use data 'type' if
	 * 		the current setting is not applicable for the specified evidence.
	 */
	public void analyseEvidence(EvidenceModel evidence, String summaryGroupBy)
	{
		m_CausalityExplorer.loadForEvidence(evidence, summaryGroupBy);
	}
	
	
	/**
	 * Presents the analysis of the specified item of evidence, grouping 
	 * causal data for summary by the specified attribute, supplying pre-built 
	 * configuration data.
	 * <p>The following evidence properties must be set:
     * <ul>
     * <li>id - to load the causality data</li>
     * <li>time - to set the time marker and window of data loaded</li>
     * <li>description - to set the widget title</li>
     * </ul>
	 * @param evidence the notification or time series feature whose causal
	 * 	relationships are to be analysed in the module.
	 * @param causalityView pre-built configuration data used by the components
	 * 	in the module.
	 * @param summaryGroupBy the name of the attribute by which causality data
	 * 		should be aggregated and summarised. If <code>null</code> the module 
	 * 		will maintain the current 'Group by' setting, or use data 'type' if
	 * 		the current setting is not applicable for the specified evidence.
	 */
	public void analyseEvidence(EvidenceModel evidence, CausalityView causalityView, 
			String summaryGroupBy)
	{
		m_CausalityExplorer.loadForEvidence(evidence, causalityView, summaryGroupBy);
	}
	
	
	/**
	 * Clears all data out of the Analysis Module.
	 */
	public void clearAll()
	{
		m_CausalityExplorer.clearAll();
		m_CausalityExplorer.setEnabledDisplays(false);
		if ( (m_ActivityListDialog != null) && (m_ActivityListDialog.isVisible()) )
		{
			m_ActivityListDialog.hide();
		}
	}
	
	
	/**
	 * Show the Activity List dialog, allowing the user to select an activity
	 * for analysis.
	 */
	protected void showSelectActivityDialog()
	{
		if (m_ActivityListDialog == null)
		{
			m_ActivityListDialog = new ActivityListDialog();
			m_ActivityListDialog.addListener(Events.Select, 
					new Listener<SelectionEvent<IncidentModel>>(){

				@Override
                public void handleEvent(SelectionEvent<IncidentModel> be)
                {
					analyseActivity(be.getModel());
                }
			
			});
		}
		
		// If showing, load at time of currently analysed evidence.
		EvidenceModel current = m_CausalityExplorer.getEvidence();
		if (current != null)
		{
			m_ActivityListDialog.loadAtTime(current.getTime(TimeFrame.SECOND));
		}
		else
		{
			m_ActivityListDialog.load();
		}
		
		m_ActivityListDialog.show();
	}

}
