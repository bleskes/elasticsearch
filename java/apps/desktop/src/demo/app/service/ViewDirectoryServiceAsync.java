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

import com.google.gwt.user.client.rpc.AsyncCallback;

import demo.app.data.*;

/**
 * Defines the methods to be implemented by the asynchronous client interface
 * to the View Directory service.
 * @author Pete Harverson
 */
public interface ViewDirectoryServiceAsync
{
	public void getDesktopViewConfig(AsyncCallback<DesktopViewConfig> callback);
	
	public void getDrillDownView(String viewName, String filterAttribute, 
			String filterValue, AsyncCallback<View> callback);
	
	public void getHistoryView(AsyncCallback<HistoryView> callback);
	
}
