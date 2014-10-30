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

package com.prelert.web;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.log4j.Logger;

import static com.prelert.service.ApplicationResponseMessages.*;


/**
 * Simple servlet filter which checks that there is an HttpSession
 * associated with each request. If the HttpSession has timed out then an
 * {@link InvalidSessionException} is thrown.
 *
 * @author Pete Harverson
 */
public class ValidSessionFilter implements Filter
{
	static Logger logger = Logger.getLogger(ValidSessionFilter.class);
	
	public void init(FilterConfig filterConfig) throws ServletException
	{
		logger.debug("init()");
	}
	

	/** 
	 * Called by the servlet container each time a request/response pair is 
	 * passed through the chain due to a client request for a resource at the 
	 * end of the chain. The method checks that there is a valid HttpSession
	 * associated with the request, and if not throws an {@link InvalidSessionException}.
	 */
	public void doFilter(ServletRequest request, ServletResponse response,
	        FilterChain chain) throws IOException, ServletException
	{
		HttpServletRequest httpRequest = (HttpServletRequest)request;
		HttpSession session = httpRequest.getSession(false);
		
		if (session != null)
		{
			chain.doFilter(request, response);
		}
		else
		{
			logger.debug("Invalid HttpSession - session has timed out.");
			throw new InvalidSessionException(SESSION_TIMEOUT);
		}	
		
	}
	
	
	public void destroy()
	{

	}

}
