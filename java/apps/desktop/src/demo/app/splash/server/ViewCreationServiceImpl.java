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

package demo.app.splash.server;

import java.util.List;

import org.apache.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import demo.app.data.*;
import demo.app.server.ViewDirectory;
import demo.app.splash.service.ViewCreationService;

public class ViewCreationServiceImpl extends RemoteServiceServlet 
	implements ViewCreationService
{
	
	static Logger logger = Logger.getLogger(ViewCreationServiceImpl.class);
	
	private ViewDirectory		m_ViewDirectory;
	
	
	/**
	 * Returns the directory of configured views.
	 * @return ViewDirectory.
	 */
    public ViewDirectory getViewDirectory()
    {
	    return m_ViewDirectory;
    }


	/**
	 * Sets the directory of configured views.
	 * @param ViewDirectory of configured views.
	 */
    public void setViewDirectory(ViewDirectory directory)
    {
	    m_ViewDirectory = directory;
    }
	
	
	/**
	 * Returns the configuration data for a view used to display information
	 * from the specified data source type.
	 * @param dataSourceType	data source type for which to return the
	 * 		view e.g. p2pslogs or UDP error data.
	 * @return View object encapsulating configuration properties for a view
	 * 		of the specified data source type.
	 */
	public View getView(DataSourceType dataSourceType)
	{
		// View name is same as data source type name.
		String viewName = dataSourceType.getName();
		
		return m_ViewDirectory.getView(viewName);
	}
	

	/**
	 * Returns the list of all the views used to display notification and time
	 * series data.
	 * @return the complete list of notification and time series views.
	 */
	public List<View> getViews()
	{
		return m_ViewDirectory.getViews();
	}
	
}
