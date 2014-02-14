/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Inc 2006-2014     *
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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.rs.resources;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;

import com.prelert.job.JobManager;
import com.prelert.rs.data.Pagination;

/**
 * Abstract resource class that knows how to access a 
 * {@linkplain com.prelert.job.JobManager} 
 */
abstract public class ResourceWithJobManager 
{
	// TODO This field is hidden in subclasses
	static private final Logger s_Logger = Logger.getLogger(ResourceWithJobManager.class);
	
	/**
	 * Application context injected by the framework
	 */
	@Context 
	private Application m_RestApplication;
	private JobManager m_JobManager;
	
	/**
	 * 
	 */
	@Context
	protected UriInfo m_UriInfo;
	
	
    /**
     * Get the job manager object from the application's set of singletons
     * 
     * @return
     */
    protected JobManager jobManager()
    {   	
    	if (m_JobManager != null)
    	{
    		return m_JobManager;
    	}
    	    	
		if (m_RestApplication == null)
		{
			s_Logger.error("Application context has not been set in "
					+ "the jobs resource");
			
			throw new IllegalStateException("Application context has not been"
					+ " set in the jobs resource");
		}
		
		Set<Object> singletons = m_RestApplication.getSingletons();
		for (Object obj : singletons)
		{
			if (obj instanceof JobManager)
			{
				m_JobManager = (JobManager)obj;
				break;
			}
		}
		
		if (m_JobManager == null)
		{
			s_Logger.error("Application singleton set doesn't contain an " +
					"instance of JobManager");
			
			throw new IllegalStateException("Application singleton set doesn't "
					+ "contain an instance of JobManager");
		}    	 
		
		return m_JobManager;
    }
    
    /**
     * Set the previous and next page URLs if appropriate.
     * If there are more hits than the take value is set to the results 
     * will be paged else the next and previous page URLs will be 
     * <code>null</code>
     * 
     * @param path
     * @param page
     */
	protected void setPagingUrls(String path, Pagination<?> page)
	{
		setPagingUrls(path, page, Collections.<KeyValue>emptyList());
	}
	
    /**
     * Set the previous and next page URLs if appropriate.
     * If there are more hits than the take value is set to the results 
     * will be paged else the next and previous page URLs will be 
     * <code>null</code>. The list of extra query parameters will be
     * added to the paging Urls.
     * 
     * @param path
     * @param page
     * @param queryParams List of extra query parameters
     */
	protected void setPagingUrls(String path, Pagination<?> page, 
			List<KeyValue> queryParams)
	{
		if (page.isAllResults() == false)
		{
			// Is there a next page of results
			int remaining = (int)page.getHitCount() - 
					(page.getSkip() + page.getTake());
			if (remaining > 0)
			{
				int nextPageStart = page.getSkip() + page.getTake();
				UriBuilder uriBuilder = m_UriInfo.getBaseUriBuilder()
						.path(path)
						.queryParam("skip", nextPageStart)
						.queryParam("take", page.getTake());
				for (KeyValue pair : queryParams)
				{
					uriBuilder.queryParam(pair.getKey(), pair.getValue());
				}
				 
				 URI nextUri = uriBuilder.build();

				page.setNextPage(nextUri);
			}

			// previous page
			if (page.getSkip() > 0)
			{
				int prevPageStart = Math.max(0, page.getSkip() - page.getTake());

				UriBuilder uriBuilder = m_UriInfo.getBaseUriBuilder()
						.path(path)
						.queryParam("skip", prevPageStart)
						.queryParam("take", page.getTake());
				
				for (KeyValue pair : queryParams)
				{
					uriBuilder.queryParam(pair.getKey(), pair.getValue());
				}
				 
				 URI prevUri = uriBuilder.build();

				page.setPreviousPage(prevUri);
			}
		}
	}
	
	/**
	 * Simple class to pair key, value strings 
	 */
	protected class KeyValue
	{
		private String m_Key;
		private String m_Value;
		
		public KeyValue(String key, String value)
		{
			m_Key = key;
			m_Value = value;
		}
		
		public String getKey()
		{
			return m_Key;
		}
		
		public String getValue()
		{
			return m_Value;
		}
	}
}
