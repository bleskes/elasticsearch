/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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
 ************************************************************/

package com.prelert.api;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.data.ExternalTimeSeriesDetails;
import com.prelert.proxy.inputmanager.dao.InputManagerDAO;


/** 
 * Tracks the configured metric paths.
 * 
 * The metrics can be initially read from the database {@link #loadActiveMetrics()}
 * or set by a client. 
 * <br/>
 * The list does not necessarily reflect the actual time series that 
 * are being pushed through the C++ processes.
 * 
 */
public class ConfigurationManager 
{
	static final public Logger s_Logger = Logger.getLogger(ConfigurationManager.class);
	
	
	private List<String> m_TrackedMetrics;
	
	/**
	 * Database DAO object
	 */
	private InputManagerDAO m_InputManagerDao;
	
	public ConfigurationManager()
	{
		m_TrackedMetrics = new ArrayList<String>();
	}
	
	
	/**
	 * Read from the database all the external time series that
	 * are marked active. This is the initial set of tracked 
	 * metrics. 
	 */
	public void loadActiveMetrics()
	{
		List<ExternalTimeSeriesDetails> details = m_InputManagerDao.getExternalTimeSeriesDetails(true);
		
		s_Logger.info(String.format("Read %d tracked metrics", details.size()));
		
		for (ExternalTimeSeriesDetails detail : details)
		{
			m_TrackedMetrics.add(detail.getExternalKey());
		}
	}
	
	/**
	 * The InputManager data access object.
	 * @return
	 */
	public InputManagerDAO getInputManagerDAO()
	{
		return m_InputManagerDao;
	}
	
	/**
	 * Set the InputManager data access object.
	 */	
	public void setInputManagerDAO(InputManagerDAO dao)
	{
		m_InputManagerDao = dao;
	}
	
	
	/**
	 * Returns the list of metrics configured to be active.
	 * This does not necessary reflect the actual time series that 
	 * are being pushed through the C++ processes.
	 * <br/>
	 * This list is initially read from the database, thereafter it
	 * may be updated by the client.
	 * 
	 * @return
	 */
	public List<String> getTrackedMetrics()
	{
		return m_TrackedMetrics;
	}

	/**
	 * Store the list of active metrics in memory overwriting 
	 * the current list.
	 * Does <em>not</em> update the database.
	 * @param metrics
	 */
	public void setTrackedMetrics(List<String> metrics)
	{
		m_TrackedMetrics = metrics;
	}
	
}
