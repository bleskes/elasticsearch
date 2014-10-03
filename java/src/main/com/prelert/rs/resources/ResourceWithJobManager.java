/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;

import com.prelert.job.alert.manager.AlertManager;
import com.prelert.job.manager.JobManager;
import com.prelert.job.normalisation.Normaliser;
import com.prelert.rs.data.Pagination;

/**
 * Abstract resource class that knows how to access a 
 * {@linkplain com.prelert.job.manager.JobManager} 
 */
abstract public class ResourceWithJobManager 
{
	// TODO This field is hidden in subclasses
	static private final Logger s_Logger = Logger.getLogger(ResourceWithJobManager.class);
	
	/**
	 * Date query param format
	 */
	static public final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX";
	/**
	 * Date query param format with ms
	 */
	static public final String ISO_8601_DATE_FORMAT_WITH_MS = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
	
	/**
	 * The filter 'start' query parameter
	 */
	static public final String START_QUERY_PARAM = "start";
	
	/**
	 * The filter 'end' query parameter
	 */
	static public final String END_QUERY_PARAM = "end";
	
	/**
	 * Format string for the un-parseable date error message
	 */
	static public final String BAD_DATE_FROMAT_MSG = "Error: Query param '%s' with value" 
			+ " '%s' cannot be parsed as a date or converted to a number (epoch)";
	
	
	/**
	 * Application context injected by the framework
	 */
	@Context 
	private Application m_RestApplication;
	
	private JobManager m_JobManager;
	private AlertManager m_AlertManager;
	private Normaliser m_Normaliser;
	
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
     * Get the Alert manager object from the application's set of singletons
     * 
     * @return
     */
    protected AlertManager alertManager()
    {   	
    	if (m_AlertManager != null)
    	{
    		return m_AlertManager;
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
			if (obj instanceof AlertManager)
			{
				m_AlertManager = (AlertManager)obj;
				break;
			}
		}
		
		if (m_AlertManager == null)
		{
			String msg = "Application singleton set doesn't contain an " +
					"instance of AlertManager";
			
			s_Logger.error(msg);
			throw new IllegalStateException(msg);
		}    	 
		
		return m_AlertManager;
    }
    
    /**
     * Get the results normaliser object from the application's set of singletons
     * @return
     */
    protected Normaliser normaliser()
    {   	
    	if (m_Normaliser != null)
    	{
    		return m_Normaliser;
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
			if (obj instanceof Normaliser)
			{
				m_Normaliser = (Normaliser)obj;
				break;
			}
		}
		
		if (m_Normaliser == null)
		{
			String msg = "Application singleton set doesn't contain an " +
					"instance of Normaliser";
			
			s_Logger.error(msg);
			throw new IllegalStateException(msg);
		}    	 
		
		return m_Normaliser;
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
	 * First tries to parse the date first as a Long and convert that 
	 * to an epoch time. If the long number has more than 10 digits 
	 * it is considered a time in milliseconds else if 10 or less digits
	 * it is in seconds. If that fails it tries to parse the string 
	 * using one of the DateForamts passed in the array.
	 * 
	 * If the date string cannot be parsed 0 is returned. 
	 * 
	 * @param dateFormats Try to parse the date string with these date formats.
	 * The array should be ordered the most likely to work first.
	 * @param date
	 * @return The epoch time in milliseconds or 0 if the date cannot be parsed.
	 */
	protected long paramToEpoch(String date, DateFormat dateFormats [])
	{
		try 
		{
			long epoch = Long.parseLong(date);
			if (date.trim().length() <= 10) // seconds
			{
				return epoch * 1000;
			}
			else
			{
				return epoch;
			}
		}
		catch (NumberFormatException nfe)
		{
			// not a number
		}
		
		for (DateFormat dateFormat : dateFormats)
		{
			// try parsing as a date string
			try 
			{
				Date d = dateFormat.parse(date);
				// TODO validate date
				return d.getTime();
			}
			catch (ParseException pe)
			{
				// not a date 
			}
		}
		
		// Could not do the conversion
		return 0;
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
