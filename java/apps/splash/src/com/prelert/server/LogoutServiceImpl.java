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

package com.prelert.server;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.prelert.service.LogoutService;


/**
 * Server-side implementation of the service for logging a user out of the application.
 * @author Pete Harverson
 */
public class LogoutServiceImpl extends RemoteServiceServlet implements LogoutService
{
	static Logger logger = Logger.getLogger(LogoutServiceImpl.class);
	
	
	/**
	 * Logs the user out of the application.
	 */
	@Override
	public void logout()
	{
		// Invalidate session.
		HttpSession session = getThreadLocalRequest().getSession(false);
		if (session != null)
		{
			logger.debug("Invalidate session ID: " + session.getId());
			session.invalidate();
		}
	}

}
