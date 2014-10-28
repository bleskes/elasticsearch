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

package com.prelert.server;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.prelert.dao.DataSourceDAO;
import com.prelert.data.MarketingMessages;
import com.prelert.server.MarketingMessageParser;
import com.prelert.service.MarketingMessageService;


/**
 * Server-side implementation of the service which provides 
 * marketing messages for the download diagnostics product.
 * @author Pete Harverson
 */
@SuppressWarnings("serial")
public class MarketingMessageServiceImpl extends RemoteServiceServlet 
	implements MarketingMessageService
{
	static Logger s_Logger = Logger.getLogger(MarketingMessageServiceImpl.class);
	
	private String	m_MessageURL = 
		"http://www.prelert.com/download/marketing_messages.html";
	
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
	public MarketingMessages getMessages()
	{
		MarketingMessages marketingMessages = new MarketingMessages();
		
		try
    	{
			// Read in the marketing messages from the marketing URL.
    		MarketingMessageParser parser = new MarketingMessageParser();
        	List<String> messages = parser.parseDivsFromURL(m_MessageURL);
        	s_Logger.debug("getMessages() read in: " + messages);
        	
        	marketingMessages.setMessages(messages);
    	}
    	catch (IOException ioe)
    	{
    		s_Logger.error("Error reading messages from marketing URL " +  m_MessageURL, ioe);
    	}
    	
    	// Get the license expiry time.
    	Date expiry = m_DataSourceDAO.getEndTime();
    	marketingMessages.setExpiryDate(expiry);
    	
    	return marketingMessages;
	}
	
	
	/**
	 * Sets the URL of the page of HTML which contains marketing messages.
	 * @param url the URL of the marketing message HTML page.
	 */
	public void setMessageURL(String url)
	{
		s_Logger.debug("Marketing message URL set to: " + url);
		m_MessageURL =  url;
	}

}
