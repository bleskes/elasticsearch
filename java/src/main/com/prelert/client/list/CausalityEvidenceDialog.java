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
import java.util.List;

import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.google.gwt.core.client.GWT;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.RequestViewEvent;
import com.prelert.data.Attribute;
import com.prelert.data.DataSourceCategory;
import static com.prelert.data.PropertyNames.*;
import com.prelert.data.gxt.CausalityDataModel;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.CausalityQueryServiceAsync;


/**
 * An extension of the Ext GWT Dialog for paging through the notifications and
 * time series features which have been causally related into an activity.
 * The dialog consists of an evidence grid and a 'Close' button.
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>OpenViewClick</b> : RequestViewEvent<br>
 * <div>Fires after a link to a data view is selected in the dialog.</div>
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
public class CausalityEvidenceDialog extends Dialog
{
	private static CausalityEvidenceDialog	s_Instance;
	
	private CausalityQueryServiceAsync 		m_CausalityQueryService;
	
	private EvidenceGridPanel 				m_EvidencePanel;
	private CausalityEvidencePagingLoader 	m_Loader;
	private String							m_DataSourceName;
	
	
	/**
	 * Returns the application-wide instance of the evidence dialog used for
	 * paging through notifications and time series features which have
	 * been correlated together into an incident.
	 * @return application-wide instance of the Causality Evidence dialog.
	 */
	public static CausalityEvidenceDialog getInstance()
	{
		if (s_Instance == null)
		{
			s_Instance = new CausalityEvidenceDialog();
		}
		
		return s_Instance;
	}
	
	
	/**
	 * Creates a dialog for paging through the evidence data from an incident.
	 */
	private CausalityEvidenceDialog()
	{
		setBodyBorder(false);
		setSize(850, 510);
		setLayout(new FitLayout());
		
		setButtons(Dialog.CLOSE);   
	    setHideOnButtonClick(true); 
	    
	    
	    // Create the RpcProxy and PagingLoader to populate the list.
	    m_CausalityQueryService = AsyncServiceLocator.getInstance().getCausalityQueryService();
	    CausalityEvidencePagingRpcProxy proxy = new CausalityEvidencePagingRpcProxy();
		m_Loader = new CausalityEvidencePagingLoader(proxy);
		m_Loader.setRemoteSort(true);
	    
	    // Create the evidence grid and add a 'Show Data' tool to the context menu.
	    m_EvidencePanel = new EvidenceGridPanel(m_Loader);
	    m_EvidencePanel.setHeaderVisible(false);
	    
	    MenuItem showDataMenuItem = new MenuItem(ClientUtil.CLIENT_CONSTANTS.showData());    
	    showDataMenuItem.addSelectionListener(new SelectionListener<MenuEvent>()
	    {
			@Override
            public void componentSelected(MenuEvent ce)
            {
				EvidenceModel selectedRow = m_EvidencePanel.getSelectedEvidence();
				if (selectedRow != null)
				{
					RequestViewEvent<EvidenceModel> rve = 
						new RequestViewEvent<EvidenceModel>(CausalityEvidenceDialog.this);
					rve.setModel(selectedRow);	
					fireEvent(GXTEvents.OpenViewClick, rve);
				}
            }
			
	    });
	    m_EvidencePanel.getGridContextMenu().add(showDataMenuItem);
	    add(m_EvidencePanel);
	}
	
	
	/**
	 * Sets the id of an item of evidence from the aggregated causality data. 
	 * <p>
	 * A separate call should be made to reload data into the dialog following 
	 * the call to this method.
	 * @param evidenceId the id of an item of evidence from the aggregated causality data.
	 */
	public void setEvidenceId(int evidenceId)
	{
		m_Loader.setRowId(evidenceId);
	}
	
	
	/**
	 * Sets whether only evidence with the same description and data type as
	 * that of the specified item of evidence should be loaded.
	 * <p>
	 * A separate call should be made to reload data into the dialog following 
	 * the call to this method.
	 * @param singleDescription <code>true</code> to limit loading to a single 
	 * 	description, <code>false</code> otherwise.
	 * @see #setEvidenceId(int)
	 */
	public void setSingleDescription(boolean singleDescription)
	{
		m_Loader.setSingleDescription(singleDescription);
	}
	
	
	/**
	 * Sets an optional filter for the evidence data. 
	 * <p>
	 * A separate call should be made to reload data into the window following the call
	 * to this method.
	 * @param filterAttribute 	attribute name on which the evidence should be filtered.	
	 * @param filterValue	attribute value on which the evidence should be filtered.
	 */
	public void setFilter(List<Attribute> filterAttributes)
	{
		m_Loader.setFilter(filterAttributes);
	}
	
	
	/**
	 * Sets the name of the data source (e.g. 'p2pslog' or 'system_udp') of the 
	 * evidence data to be shown in the dialog.
	 * <p>
	 * A separate call should be made to reload data into the dialog following 
	 * the call to this method.
	 * @param dataSourceName the name of the data source, or <code>null</code> if
	 * 	displaying evidence from a range of data types.
	 */
	public void setDataSourceName(String dataSourceName)
	{
		m_DataSourceName = dataSourceName;
	}
	
	
	/**
	 * Loads the dialog to show the constituent notifications or time series
	 * features for the specified causality data. The dialog title is also updated
	 * according to the data that is being loaded.
	 * @param causalityData item of causality data for which to display evidence.
	 * @param evidenceId id of any notification or time series feature from the 
	 * 		activity containing the specified causality data.
	 */
	public void loadForCausalityData(CausalityDataModel causalityData, int evidenceId)
	{
		setEvidenceId(evidenceId);
		setDataSourceName(causalityData.getDataSourceName());
		setSingleDescription(false);
		
		ArrayList<Attribute> filterAttributes = new ArrayList<Attribute>();
		String description = causalityData.getDescription();
		if (causalityData.getDataSourceCategory() == DataSourceCategory.NOTIFICATION)
		{
			filterAttributes.add(new Attribute(DESCRIPTION, description));
			setHeading(
					ClientUtil.CLIENT_CONSTANTS.notificationDataHeading(description));
		}
		else
		{
			filterAttributes.add(new Attribute(METRIC, description));
			setHeading(
					ClientUtil.CLIENT_CONSTANTS.timeSeriesFeaturesHeading(description));
		}
		filterAttributes.add(new Attribute(SOURCE, causalityData.getSource()));
		
		// Add in type to make sure the columns returned by the paging procs are
		// correct for the type of the causality data.
		filterAttributes.add(new Attribute(TYPE, m_DataSourceName));
		
		List<Attribute> attributes = causalityData.getAttributes();
		if (attributes != null)
		{
			filterAttributes.addAll(attributes);
		}
		
		setFilter(filterAttributes);
		
		load();
	}
	
	
	/**
	 * Loads the list of evidence for the current configuration (evidence id, 
	 * description, filter), displaying the first (most recent) page of data.
	 */
	public void load()
	{
		// Load the list of columns for the results grid, then load the grid.
		ApplicationResponseHandler<List<String>> callback = 
			new ApplicationResponseHandler<List<String>>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log("Error loading list of columns for " + m_DataSourceName, caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
						ClientUtil.CLIENT_CONSTANTS.errorLoadingEvidenceData(), null);
			}


			@Override
            public void uponSuccess(List<String> columns)
			{
				m_EvidencePanel.setColumns(columns);
				m_EvidencePanel.loadFirstPage();
			}
		};
		
		// NB. The columns returned by the cause_list_notifications_xxx_page procs
		// are determined by whether the filter attribute passed to the procs is
		// 'type'. So be very wary of changing the logic used to obtain the list
		// of columns for the grid from this approach based on type name.
		m_CausalityQueryService.getEvidenceColumns(m_DataSourceName, callback);
	}
}
