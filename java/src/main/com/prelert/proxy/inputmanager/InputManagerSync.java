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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Class used by the InputManagers to synchronise their data collection so
 * they all send data for the same time period together. 
 * 
 * The Prelert engine can only correlate evidence in a certain time window,
 * if old evidence from 2 hours ago arrives at the engine at the same time
 * as new evidence from now the engine cannot correlate it.
 * 
 *  This class stops the InputManagers from getting more than 
 *  MaxWindowSize mille seconds apart.
 */
public class InputManagerSync 
{
	private static Logger s_Logger = Logger.getLogger(InputManagerSync.class);
	
	final static public int MAX_WINDOW_SIZE_MS = 1000 * 60 * 3;
	
	private List<ManagersLastTime> m_ManagersLastTime;
	
	private Object m_SyncObject;
	
	volatile boolean m_Stop;
	
	private int m_MaxWindowSizeMs;
	
	public InputManagerSync()
	{
		m_SyncObject = new Object();
		m_ManagersLastTime = new ArrayList<ManagersLastTime>();
		
		m_Stop = false;
		
		m_MaxWindowSizeMs = MAX_WINDOW_SIZE_MS;
	}
	
	
	/**
	 * Adds the InputManager to the list of managers to be synchronised.
	 * @param manager
	 */
	public void addInputManager(InputManager manager)
	{
		m_ManagersLastTime.add(new ManagersLastTime(manager));
	}
	
	
	/**
	 * This function will block until all the inputmanagers are
	 * querying data within MaxWindowSize seconds of each other.
	 * 
	 * @param inputManager
	 * @param startTime
	 */
	public void waitUntilInSync(InputManager inputManager, Date startTime)
	{
		long timeMs = startTime.getTime();
		long oldestTime = Long.MAX_VALUE;
		
		synchronized(m_ManagersLastTime)
		{
			boolean managerFound = false;
			
			// find the oldest time
			for (ManagersLastTime managerLastTime : m_ManagersLastTime)
			{
				if (inputManager == managerLastTime.getInputManager())
				{
					managerLastTime.setTimeMs(timeMs);
					
					managerFound = true;
				}
				
				if (managerLastTime.isTimeSet() && managerLastTime.getTimeMs() < oldestTime)
				{
					oldestTime = managerLastTime.getTimeMs();
				}
			}
			
			if (!managerFound)
			{
				s_Logger.error("Could not match inputmanger");
			}
		}
		
		boolean okToProceed = (startTime.getTime() - oldestTime) < m_MaxWindowSizeMs;
		
		if (okToProceed)
		{
			synchronized (m_SyncObject) 
			{
				m_SyncObject.notifyAll();
			}
		}
		else
		{
			synchronized (m_SyncObject) 
			{
				while (continueToWait())
				{
					try
					{
						m_SyncObject.wait();
					}
					catch (InterruptedException e)
					{
						s_Logger.error("InputManagerSync interuppted in wait()");
					}
				}
			}
		}
		
	}
	
	
	/**
	 * If the stopSynchronising() method has been called this
	 * function returns false.
	 * 
	 * If the last requested time for all the inputmanagers is
	 * within m_MaxWindowSize then true is returned.
	 * 
	 * @return
	 */
	private boolean continueToWait()
	{
		if (m_Stop)
		{
			return false;
		}
		
		if (m_ManagersLastTime.size() <= 1)
		{
			return false;
		}
		
		long oldestTime = Long.MAX_VALUE;
		long latestTime = Long.MIN_VALUE;
		
		synchronized(m_ManagersLastTime)
		{
			// find the oldest and youngest times
			for (ManagersLastTime managerLastTime : m_ManagersLastTime)
			{		
				if (managerLastTime.isTimeSet() && managerLastTime.getTimeMs() < oldestTime)
				{
					oldestTime = managerLastTime.getTimeMs();
				}
				
				if (managerLastTime.isTimeSet() && managerLastTime.getTimeMs() > latestTime)
				{
					latestTime = managerLastTime.getTimeMs();
				}
			}			
		}
		
		return (latestTime - oldestTime) > m_MaxWindowSizeMs;
	}
	
	
	/**
	 * Wakes all waiting objects and sets a flag so they 
	 * won't be synchronised again.
	 */
	public void stopSynchronising()
	{
		m_Stop = true;
		
		synchronized (m_SyncObject) 
		{
			m_SyncObject.notifyAll();
		}
		
		s_Logger.info("InputManager Synchronising stopped");
	}
	
	
	/**
	 * The window size in Seconds property.
	 * InputManagers can proceed when they are within this period 
	 * of each other.
	 * @return
	 */
	public int getMaxWindowSize()
	{
		return m_MaxWindowSizeMs / 1000;
	}
	
	public void setMaxWindowSize(int windowSizeSecs)
	{
		m_MaxWindowSizeMs = windowSizeSecs * 1000;
	}
	
	
	/**
	 * Helper class
	 * Records the time of the last query for each inputmanager.
	 */
	private class ManagersLastTime
	{
		long m_TimeMs;
		final InputManager m_InputManager;
		boolean m_TimeIsSet;
		
		
		ManagersLastTime(InputManager inputManager)
		{
			m_InputManager = inputManager;
		}
		
		public void setTimeMs(long timeMs)
		{
			m_TimeMs = timeMs;
			m_TimeIsSet = true;
		}
		
		public long getTimeMs()
		{
			return m_TimeMs;
		}
		
		public boolean isTimeSet()
		{
			return m_TimeIsSet;
		}
		
		InputManager getInputManager()
		{
			return m_InputManager;
		}
		
	}
	
}
