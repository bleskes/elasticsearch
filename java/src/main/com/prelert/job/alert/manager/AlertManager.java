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

package com.prelert.job.alert.manager;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.TimeoutHandler;

import org.apache.log4j.Logger;

import com.prelert.job.alert.Alert;
import com.prelert.job.alert.Severity;
import com.prelert.job.alert.persistence.AlertPersister;


public class AlertManager implements TimeoutHandler
{
	static public final Logger s_Logger = Logger.getLogger(AlertManager.class);
	
	
	private Map<AsyncResponse, AlertListener> m_AsyncRepsonses; 
	private AlertPersister m_AlertPersister;
	
	
	public class AlertListener
	{
		private AsyncResponse m_Response;
		private AlertManager m_Manager;
		
		private AlertListener(AsyncResponse response, AlertManager manager)
		{
			m_Response = response;
			m_Manager = manager;
		}
		
		public void fireAlert(Alert alert)
		{
			m_Manager.deregisterListener(this);
			m_Response.resume(alert);
		}
		
		public AsyncResponse getResponse()
		{
			return m_Response;
		}
	}
	
	
	public AlertManager(AlertPersister alertPersister)
	{
		m_AlertPersister = alertPersister;
		m_AsyncRepsonses = new HashMap<>();
	}
	
	
	public void registerRequest(AsyncResponse response, long timeout_secs)
	{
		response.setTimeout(timeout_secs, TimeUnit.SECONDS);
		response.setTimeoutHandler(this);
		
		AlertListener listener = this.new AlertListener(response, this);
		registerListener(listener);
	}

	
	/**
	 * AysncResponse timeout handler
	 */
	@Override
	public void handleTimeout(AsyncResponse response) 
	{
		removeResponseFromList(response);
		
		response.resume(new Alert()); // empty object
	}
	
	private void removeResponseFromList(AsyncResponse response)
	{
		synchronized(m_AsyncRepsonses)
		{
			AlertListener removed = m_AsyncRepsonses.remove(response);
//			if (removed == null)
//			{
//				throw new IllegalStateException("Unknown AsyncResponse removed");
//			}
		}
	}

	private void deregisterListener(AlertListener listener)
	{
		removeResponseFromList(listener.getResponse());
		
		// 
	}
	
	private void saveAlert(Alert alert)
	{
		try 
		{
			m_AlertPersister.persistAlert(alert.getJobId(), alert);
		}
		catch (IOException e) 
		{
		}
	}
	
	private void registerListener(final AlertListener listener)
	{
		synchronized(m_AsyncRepsonses)
		{
			m_AsyncRepsonses.put(listener.getResponse(), listener);
		}
					
		// add to whatever
		
		TimerTask task = new TimerTask() 
		{
			@Override
			public void run() 
			{
				listener.fireAlert(createDummyAlert());
			}
		};
		Timer t = new Timer();
		t.schedule(task, 10000);
	}
	
	public Alert createDummyAlert()
	{
		Alert alert = new Alert();
		alert.setJobId("farequte");
		alert.setReason("cos i said so");
		alert.setSeverity(Severity.WARNING);
		
		return alert;
	}
	
}
