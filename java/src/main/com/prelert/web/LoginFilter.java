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
import java.util.Date;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.prelert.dao.DataSourceDAO;


/**
 * Servlet filter which carries out various checks on the environment prior to log in.
 * @author Pete Harverson
 */
public class LoginFilter implements Filter
{
	static Logger s_Logger = Logger.getLogger(LoginFilter.class);
	
	private DataSourceDAO	m_DataSourceDAO;
	
	
	/**
	 * Sets the DataSourceDAO to be used to make queries on data sources.
	 * @param dataSourceDAO the data access object for Prelert data source information.
	 */
	public void setDataSourceDAO(DataSourceDAO dataSourceDAO)
	{
		m_DataSourceDAO = dataSourceDAO;
	}
	
	
	/**
	 * Returns the DataSourceDAO being used to make queries on data sources.
	 * @param dataSourceDAO the data access object for Prelert data source information.
	 */
	public DataSourceDAO getDataSourceDAO()
	{
		return m_DataSourceDAO;
	}
	
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
	        FilterChain filterChain) throws IOException, ServletException
	{
		// Filters the request by checking that the license hasn't expired.
		Date expiryDate = m_DataSourceDAO.getEndTime();
		if ((expiryDate != null) && (expiryDate.before(new Date())) )
		{
			HttpServletResponse httpResponse = (HttpServletResponse)response; 
			httpResponse.sendRedirect("license_expired.do");
		}
		
		filterChain.doFilter(request, response);
	}
	
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		// NB. Filter lifecycle methods not called by default when running within Spring framework.
	}
	

	@Override
	public void destroy()
	{
		// NB. Filter lifecycle methods not called by default when running within Spring framework.
	}
	
}
