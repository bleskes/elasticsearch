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

package com.prelert.service;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.prelert.data.DataSourceType;
import com.prelert.data.View;


/**
 * Defines the methods for the interface to the View Creation service,
 * used for creating objects which define configuration for UI views.
 * @author Pete Harverson
 */
public interface ViewCreationService extends RemoteService
{
	/**
	 * Returns the configuration data for a view used to display information
	 * from the specified data source type.
	 * @param dataSourceType	data source type for which to return the
	 * 		view e.g. p2pslogs or UDP error data.
	 * @return View object encapsulating configuration properties for a view
	 * 		of the specified data source type.
	 */
	public View getView(DataSourceType dataSourceType);
	
	
	/**
	 * Returns the list of all the views used to display notification and time
	 * series data.
	 * @return the complete list of notification and time series views.
	 */
	public List<View> getViews();
}
