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

package demo.app.service;

import com.google.gwt.user.client.rpc.RemoteService;

import demo.app.data.DataSourceType;
import demo.app.data.DesktopViewConfig;
import demo.app.data.HistoryView;
import demo.app.data.View;

/**
 * Defines the methods for the interface to the View Directory service,
 * used to load views from the server.
 * @author Pete Harverson
 */
public interface ViewDirectoryService extends RemoteService
{
	/**
	 * Returns the Desktop View configuration that has been defined
	 * in the view configuration file.
	 */
	public DesktopViewConfig getDesktopViewConfig();
	
	
    /**
     * Creates and returns a new view, based on the view with the specified name,
     * and setting the supplied filter attribute and filter value on the new view.
     * @return new drill-down view based on the supplied view and filter.
     * @throws NullPointerException if there is no View in the View Directory with the
     * given name.
     */
	public View getDrillDownView(String viewName, String filterAttribute, String filterValue) 
		throws NullPointerException;
	
	
	/**
	 * Returns the Evidence History View to display the history of an item of evidence.
	 * @return the History View or <code>null</code> if no History View has been
	 * 			configured.
	 */
	public HistoryView getHistoryView();
	
}
