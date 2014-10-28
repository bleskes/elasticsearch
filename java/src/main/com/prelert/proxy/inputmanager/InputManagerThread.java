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

package com.prelert.proxy.inputmanager;

import java.util.Date;

import com.prelert.proxy.inputmanager.DataCollectionMode;
import com.prelert.proxy.inputmanager.querydatetime.QueryDateTimeProducer;

/**
 * Thread to run a <code>Runnable</code> InputManager. 
 */
public class InputManagerThread extends Thread
{
	private InputManager m_InputManager;
	
	public InputManagerThread(InputManager inputManager)
	{
		super(inputManager, "InputManager");

		m_InputManager = inputManager;
		
		// If this object is created by another Daemon thread
		// for example a RMI thread then it will be a daemon 
		// thread. Ensure the thread is not a daemon.
		this.setDaemon(false);
	}

	
	/**
	 * Returns the inputmanager associated with this thread.
	 * 
	 * Throws an exception if the InputManager is accessed whilst
	 * the thread is running.
	 * @return
	 */
	public InputManager getInputManager()
	{
		if (isRunning())
		{
			throw new UnsupportedOperationException("You cannot access the InputManager " +
								"whilst it is running.");
		}
		
		return m_InputManager;
	}
	
	/**
	 * Stops the InputManager asynchronously. 
	 */
	public void stopInputManager()
	{
		m_InputManager.quit();
	}
	
	/**
	 * Pause a inputmanager. 
	 * the manager may continue to run for a while before it is paused.
	 */
	public void pauseInputManager()
	{
		m_InputManager.quit();
	}
	
	/**
	 * Resumes a paused input manager. 
	 */
	public void resumeInputManager()
	{
		m_InputManager.resume();
	}
	
	
	/**
	 * Returns true if the InputManager is running.
	 * @return
	 */
	public boolean isRunning()
	{
		return m_InputManager.isRunning();
	}
	
	public boolean isPaused()
	{
		return m_InputManager.isPaused();
	}
	
	public boolean isFinished()
	{
		return m_InputManager.isFinished();
	}
	
	
	/**
	 * Returns true if this has been configured.
	 * 
	 * @return
	 */
	public boolean isConfigured()
	{
		return m_InputManager.isConfigured();
	}
	
	/**
	 * Returns true if the inputmanager is running <em>and</em> is 
	 * running in realtime mode. 
	 * 
	 * Realtime mode means only current data is collected
	 * as it is created. 
	 * 
	 * @return
	 */
	public boolean isRunningRealTime()
	{
		return m_InputManager.isRunning() && 
					m_InputManager.getDataCollectionMode() == DataCollectionMode.REALTIME;
	}
	
	
	/**
	 * Returns the collection mode of this inputmanager
	 * (either HISTORICAL, REALTIME).
	 * @return
	 */
	public DataCollectionMode getDataCollectionMode()
	{
		return m_InputManager.getDataCollectionMode();
	}
	

	/**
	 * Returns true if the InputManager is running <em>and</em> is 
	 * running in CAV mode. 
	 * 
	 * CAV mode is defined as when the input manager is collecting
	 * data between 2 historical dates as opposed to real-time mode.
	 * 
	 * @return
	 */
	public boolean isCavRunning()
	{
		return m_InputManager.isRunning() && 
					m_InputManager.getDataCollectionMode() == DataCollectionMode.HISTORICAL;
	}
	
	
	/**
	 * Returns true if the InputManager has finished <em>and</em> was 
	 * running in CAV mode. 
	 * 
	 * Finished means the thread has terminated and cannot be restarted.
	 * 
	 * @return
	 */
	public boolean isCavFinished()
	{
		return m_InputManager.isFinished() && 
					m_InputManager.getDataCollectionMode() == DataCollectionMode.HISTORICAL;
	}
	
	
	/**
	 * Returns the date the CAV started.
	 * If the CAV has not started <code>null</code> will be returned.
	 * 
	 * @return
	 */
	public Date getCavStartedDate()
	{
		return m_InputManager.getCavStartedDate();
	}
	
	/**
	 * Returns the date of the first CAV query.
	 * @return
	 */
	public Date getCavQueriesStartDate()
	{
		return m_InputManager.getCavQueriesStartDate();
	}
	
	/**
	 * Returns the date of the last CAV query.
	 * @return
	 */
	public Date getCavQueriesEndDate()
	{	
		return m_InputManager.getCavQueriesEndDate();
	}
	
	/**
	 * Returns the date of the next CAV query.
	 * @return
	 */
	public Date getNextCavQueryDate()
	{
		return m_InputManager.getNextCavQueryDate();
	}
	
	
	/**
	 * Sets the QueryDateTimeProducer on the InputManager.
	 * 
	 * The InputManager is configured one it has a date time producer set.
	 * 
	 * @param value
	 */
	public void setQueryDateTimeProducer(QueryDateTimeProducer value)
	{
		m_InputManager.setQueryDateTimeProducer(value);
	}

}
