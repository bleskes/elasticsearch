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
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

/**
 * Simple Logout Controller implementation, which invalidates the user's
 * HttpSession and returns them to the Login page.
 * @author Pete Harverson
 */
public class LogoutController implements Controller
{

	static Logger logger = Logger.getLogger(LogoutController.class);
	
	private String	m_LoginPage;
	
	
	/**
	 * Processes the logout request, and then returns the user to the 
	 * application login page.
	 */
    public ModelAndView handleRequest(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) throws Exception
    {
	    
		// Invalidate session.
		HttpSession session = httpRequest.getSession(false);
		if (session != null)
		{
			logger.debug("Invalidate session ID: " + session.getId());
			session.invalidate();
		}
		
		// Redirect back to Home page, which will bring up the login page 
		// via FORM-based authentication currently in use.
	    return new ModelAndView(m_LoginPage);
    }
	
	
    /**
     * Sets the URL of the login page to redirect the HttpResponse to once the
     * necessary logout operations have occurred.
     * @param loginPage URL of the login page, relative to the 
     * 			current servlet context.
     */
    public void setLoginPage(String loginPage)
    {
    	m_LoginPage = loginPage;
    }
    
    
    /**
     * Returns the login page to redirect the HttpResponse to once the
     * necessary logout operations have occurred.
     * @param loginPage URL of the login page, relative to the current servlet context.
     */
    public String getLoginPage()
    {
    	return m_LoginPage;
    }

}
