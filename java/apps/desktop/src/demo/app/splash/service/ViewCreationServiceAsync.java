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

package demo.app.splash.service;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import demo.app.data.DataSourceType;
import demo.app.data.View;


/**
 * Defines the methods to be implemented by the asynchronous client interface
 * to the view creation service.
 * @author Pete Harverson
 */
public interface ViewCreationServiceAsync
{
	/**
	 * Returns the configuration data for a view used to display information
	 * from the specified data source type.
	 * @param dataSourceType	data source type for which to return the
	 * 		view e.g. p2pslogs or UDP error data.
	 * @param callback callback object to receive a response from the 
	 * 		remote procedure call.
	 */
	public void getView(DataSourceType dataSourceType, AsyncCallback<View> callback);
	
	
	/**
	 * Returns the list of all the views used to display notification and time
	 * series data.
	 * @param callback callback object to receive a response from the 
	 * 		remote procedure call.
	 */
	public void getViews(AsyncCallback<List<View>> callback);
}
