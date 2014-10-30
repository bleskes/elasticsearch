/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2009     *
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;



/**
 * Simple Spring HandlerInterceptor which checks that there is an HttpSession
 * associated with each request. If the HttpSession has timed out then the
 * response is redirected to the application login page.
 * @author Pete Harverson
 */
public class ValidSessionInterceptor extends HandlerInterceptorAdapter
{
	static Logger logger = Logger.getLogger(ValidSessionInterceptor.class);
	
	private String	m_LoginPage;
	
	
	/**
	 * Executes before the request handler, checking that the HttpSession is 
	 * still alive and has not been timed out. If there is no session associated
	 * with the request, the response is redirected to the login page.
	 */
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response, Object handler) throws Exception
    {
    	HttpSession session = request.getSession(false);
    	
    	if (session != null)
		{
    		logger.debug("Verified session ID " + session.getId());
			return true;
		}
		else
		{
			logger.debug("Invalid session, redirecting to login page.");
			
			throw new InvalidSessionException("HttpSession is null");
			
			//response.sendRedirect(m_LoginPage);
			//return false;			
		}
    }
    
    
    /**
     * Sets the URL of the login page to redirect the HttpResponse to if 
     * the session is no longer valid.
     * @param loginPage URL of the login page, relative to the 
     * 			servlet container root.
     */
    public void setLoginPage(String loginPage)
    {
    	m_LoginPage = loginPage;
    }
    
    
    /**
     * Returns the login page to redirect the HttpResponse to if the session is no
     * longer valid.
     * @param loginPage URL of the login page, relative to the servlet container root.
     */
    public String getLoginPage()
    {
    	return m_LoginPage;
    }

}
