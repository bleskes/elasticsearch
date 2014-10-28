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

package com.prelert.client.admin;

import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.TabItem;
import com.extjs.gxt.ui.client.widget.TabPanel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.MarginData;
import com.google.gwt.core.client.GWT;

import com.prelert.client.gxt.ModuleComponent;


/**
 * The Administration UI module. This module allows users to perform various
 * administration tasks, such as the management of Prelert users.
 * <p>
 * The container holds a GXT TabPanel with separate tabs for each of the main
 * administrative functions.
 * 
 * @author Pete Harverson
 */
public class AdminModule extends LayoutContainer implements ModuleComponent
{
	/**
	 * Locale-sensitive messages used in the Admin module, such as labels for
	 * UI controls.
	 */
	public static AdminMessages ADMIN_MESSAGES = GWT.create(AdminMessages.class);
	
	public static final String MODULE_ID = "admin";
	
	
	/**
	 * Creates the Admin UI module.
	 */
	public AdminModule()
	{
		setLayout(new FitLayout());
		
		// Hold contents in a tabbed panel.
		TabPanel tabPanel = new TabPanel();
		tabPanel.setPlain(true);
		tabPanel.addStyleName("prl-workAreaTabPanel");
		add(tabPanel, new MarginData(10));
		add(tabPanel);
		
		// Add the Alerting tab.
		tabPanel.add(createAlertingTab());
		
		// Add the Users tab.
		tabPanel.add(createUsersTab());
	}
	

	
	/**
	 * Creates the tab holding the User Admin widget.
	 * @return the Users tab.
	 */
	protected TabItem createUsersTab()
	{
		UserAdminWidget userWidget = new UserAdminWidget();
		
		TabItem tab = new TabItem();
		tab.setClosable(false);
		tab.addStyleName("prl-workAreaTabItem");
		tab.setLayout(new FitLayout());
		tab.setText(ADMIN_MESSAGES.users());
		tab.add(userWidget);
		
		return tab;
	}
	
	
	/**
	 * Creates the tab holding the Alerting configuration widget.
	 * @return the Alerting tab.
	 */
	protected TabItem createAlertingTab()
	{
		AlertingAdminWidget alertingWidget = new AlertingAdminWidget();
		
		TabItem tab = new TabItem();
		tab.setClosable(false);
		tab.setLayout(new FitLayout());
		tab.setText(ADMIN_MESSAGES.alerting());
		tab.add(alertingWidget);
		
		return tab;
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

}
