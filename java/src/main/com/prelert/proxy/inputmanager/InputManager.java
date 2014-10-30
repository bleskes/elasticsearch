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
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.data.Attribute;
import com.prelert.data.DataSourceCategory;
import com.prelert.proxy.datamanager.DatabaseManager;
import com.prelert.proxy.inputmanager.PrelertBackendTCPClient;
import com.prelert.proxy.inputmanager.dao.InputManagerDAO;
import com.prelert.proxy.inputmanager.querydatetime.CurrentQueryDateTimeProducer;
import com.prelert.proxy.inputmanager.querydatetime.QueryDateTimeProducer;
import com.prelert.proxy.inputmanager.querymonitor.RealTimeQueryMonitor;
import com.prelert.proxy.plugin.Plugin;

/**
 * The <code>InputManager</code> is responsible for updating the Prelert database.
 * It runs in an infinite loop reading new data from the proxy and sending it to 
 * one of the Prelert back end processes.
 * 
 * The method <code>setHostAndPort()</code> sets the host machine and port the
 * Prelert back end process is running (feature detector, evidence gatherer) on.
 * The <code>InputManager</code> starts a Prelert TCP client to try to
 * communicate with a server at this address.
 */

abstract public class InputManager implements Runnable
{
	/**
	 *  Interface for a callback that will be called when the InputManager
	 *  finishes.
	 */
	public interface ManagerFinished
	{
		public void doFinished(InputManager manager);
	}
	
	static protected Logger s_Logger = Logger.getLogger(InputManager.class);

	static final InputManagerSync s_InputManagerSync = new InputManagerSync();
	
	
	/**
	 * The client that will be used to communicate with the Prelert back end
	 * processes.
	 */
	private PrelertBackendTCPClient m_Client;
	
	/**
	 * Database manager.
	 */
	private DatabaseManager m_DatabaseManager;

	volatile protected boolean m_Quit;
	volatile private boolean m_HasStarted;
	volatile private boolean m_IsFinished; 
	final private Object m_PauseSyncObj = new Object();
	volatile private boolean m_IsPaused;
	
	protected InputManagerDAO m_InputManagerDAO;
	
	private Plugin m_Plugin;
	
	private String m_InitialMessage;
	
	volatile private Date m_StartedDate;
	
	private ManagerFinished m_FinishedCallback;
	
	private boolean m_UpdateDisplayColumnsOnStart;
	
	/**
	 * Object tracks the start & end times of the 
	 * queries made by the input manager.
	 */
	protected QueryDateTimeProducer m_QueryDateTimeProducer;


	/**
	 * Create an <code>InputManager</code>. 
	 * 
	 * @param inputManagerDAO InputManager's database access object.
	 */
	public InputManager(InputManagerDAO inputManagerDAO)
	{
		m_InputManagerDAO = inputManagerDAO;
		
		m_Quit = false;
		m_HasStarted = false;
		m_IsFinished = false;
		m_UpdateDisplayColumnsOnStart = false;
		
		// Add self to the sync object.
		s_InputManagerSync.addInputManager(this);
	}


	/**
	 * In subclasses this function will collect data from the plugin
	 * according the subclasses' type (Notification, Time Series, ...)
	 * between <code>startTime</code> and <code>endTime</code> then
	 * forward the data on.
	 * 
	 * @param startTime Data query start time. 
	 * @param endTime Data query end time.
	 * @param updateDisplayColumns If true update the GUIs display columns
	 * 	using the attributes of the data collected in this function.
	 * @return true if any data was collected and sent. 
	 */
	abstract protected boolean collectAndSendData(Date startTime, Date endTime, boolean updateDisplayColumns);


	/**
	 * Runs continuously collecting generating query start and end times then
	 * asks the plugin for data for that time period and forwards it on.
	 * 
	 * In the case of a historical inputmanger this function will either return
	 * when the queries are finished or transition into a real-time input manager.
	 */
	@Override
	public void run()
	{	
		if (m_QueryDateTimeProducer == null)
		{
			throw new IllegalStateException("Cannot start InputManager without a QueryDateTimeProducer.");
		}
		
		
		m_StartedDate = new Date();
		
		m_HasStarted = true;
		
		// Start the TCP client message sending worker thread.
		m_Client.start();

		try
		{
			// Attempt to send the initial message (if any)
			if (m_InitialMessage != null)
			{
				transferMessage(false, m_InitialMessage);
			}

			while (!m_Quit)
			{
				waitIfPaused();
				
				Date startTime = getQueryStartDate();
				Date endTime = getQueryEndDate();

				Date queryStarted = new Date();
				
				// Sync with the other input managers.
				s_InputManagerSync.waitUntilInSync(this, startTime);
				
				// Collect the data and send it on.
				boolean dataSent = collectAndSendData(startTime, endTime, m_UpdateDisplayColumnsOnStart);
				// don't update the columns again after we have seen some data.
				if (m_UpdateDisplayColumnsOnStart && dataSent)
				{
					m_UpdateDisplayColumnsOnStart = false;
				}

				
				waitIfPaused();
				

				if (!m_Quit)
				{			
					try
					{
						setLastQueryEndTime(endTime);

						Date now = new Date();
						long queryDuration = now.getTime() - queryStarted.getTime();
						long sleepBetweenQueriesDurationMs = getSleepTimeMs(queryDuration);
						if (sleepBetweenQueriesDurationMs > 0)
						{
							try
							{
								Thread.sleep(sleepBetweenQueriesDurationMs);
							}
							catch (InterruptedException e)
							{
								break;
							}
						}
					}
					catch (InputManagerQueriesCompleteException e)
					{
						s_Logger.debug(e.getMessage());
						if (e.isContinueCollectionRealTime())
						{
							s_Logger.info("Historical queries have finished. " +
									"Starting real-time query from time = " + e.getTimeOfLastQueryDataPoint() +
									". Finished " + new Date());

							// Set a realtime query time producer for this inputmanager.
							CurrentQueryDateTimeProducer queryTimes = new CurrentQueryDateTimeProducer();
							queryTimes.setLastQueryEndTime(e.getTimeOfLastQueryDataPoint());
							
							m_QueryDateTimeProducer = queryTimes;

							// Set the plugin's performance monitor to be a real-time monitor.
							m_Plugin.setQueryMonitorPolicy(new RealTimeQueryMonitor());

							long queryDuration = new Date().getTime() - e.getTimeOfLastQueryDataPoint().getTime();
							long sleepBetweenQueriesDurationMs = getSleepTimeMs(queryDuration);
							
							if (sleepBetweenQueriesDurationMs > 0)
							{
								try
								{
									Thread.sleep(sleepBetweenQueriesDurationMs);
								}
								catch (InterruptedException ie)
								{
									break;
								}
							}
							
						}
						else
						{
							s_Logger.info("Historical queries started at " + m_StartedDate + " have finished. Terminating thread.");
							quit();
						}
					}
				}
			}
		}
		catch (Throwable t)
		{
			s_Logger.error("Exception in InputManager", t);

		    if (t instanceof RuntimeException)
		    {
		        throw (RuntimeException) t;
		    }
		    if (t instanceof Error)
		    {
		        throw (Error) t;
		    }
		}
		finally
		{
			// Wait for all of our messages to be sent before calling the
			// finished callback, so that we don't say we're finished until
			// all data generated by this input manager has been sent
			if (m_Client != null)
			{
				m_Client.waitUntilAllMessagesSent();
			}

			m_IsFinished = true;
			
			if (m_FinishedCallback != null)
			{
				s_Logger.info("Input manager about to call finish callback");
				m_FinishedCallback.doFinished(this);
			}
			else
			{
				s_Logger.warn("No finish callback has been set");
			}

			// Shut down the client after calling the finished callback, in case
			// the data collection manager needed to use it to send a time
			// marker
			if (m_Client != null)
			{
				m_Client.waitUntilAllMessagesSent();
				s_Logger.info("Input manager about to tell client to quit");
				m_Client.quit();
			}
			else
			{
				s_Logger.info("Input manager finished with no client set");
			}

			s_Logger.info("InputManager has finished");
		}
	}


	/**
	 * Transfer a message to the Prelert back end TCP client.  This will send
	 * the message in a different thread, to avoid unnecessarily blocking the
	 * threads that pull data from 3rd party systems.
	 * @param firstInBatch Set to true if the message to be transferred is the
	 *                     first in a batch. If true then the backlog handler 
	 *                     will be called which clears any unsent messages.
	 * @param message The message to be sent
	 */
	public void transferMessage(boolean firstInBatch, String message)
	{
		if (m_Client == null)
		{
			s_Logger.error("Cannot send message before Prelert TCP client is initialised - message lost : "
							+ message);
			return;
		}

		if (firstInBatch)
		{
			m_Client.backlogHandler(getDataCollectionMode());
		}

		m_Client.queueMessage(message);
	}


	/**
	 * Sets the exit flag for the run loop and stops the
	 * synchronisation object.
	 */
	public void quit()
	{
		s_Logger.info("Quiting InputManager");
		m_Quit = true;
		
		s_InputManagerSync.stopSynchronising();
	}
	
	
	/**
	 * Waits on the pause sync object if the pause flag
	 * has been set.
	 */
	private void waitIfPaused()
	{
		synchronized (m_PauseSyncObj)
		{
			while (m_IsPaused && m_Quit == false)
			{
				try
				{
					m_PauseSyncObj.wait();
				}
				catch (InterruptedException e)
				{
					s_Logger.info("InputManager interrupted whilst paused");
				}
			}
		}
	}
	
	
	/**
	 * Set the InputManager's plugin.
	 * This function will throw an exception if:
	 * <ol>
	 * <li>The thread has started.</li>
	 * <li>The InputManagerDAO has not been set.</li>
	 * <li>A plugin has already been set.</li>
	 * </ol>
	 * @param plugin
	 */
	public void setPlugin(Plugin plugin)
	{
		if (m_HasStarted)
		{
			throw new IllegalStateException("You can't register a plugin now you " +
											"should have done it before you started the thread");
		}

		if (m_InputManagerDAO == null)
		{
			throw new IllegalStateException("A InputManagerDAO needs to be assigned " +
											"before a plugin can be registered.");
		}

		if (m_Plugin != null)
		{
			throw new UnsupportedOperationException("Once a Plugin has been registered " +
												"with the InputManager another cannot be set.");
		}

		m_Plugin = plugin;
	}
	
	
	/**
	 * Returns the plugin associated with this object.
	 * 
	 * Throws an exception if this object is running.
	 * @return
	 */
	public Plugin getPlugin()
	{
		if (isRunning())
		{
			throw new UnsupportedOperationException("You cannot access the Plugin whilst the " +
									"InputManager is running.");			
		}
		
		return m_Plugin;
	}
	
	
	/**
	 * Returns the plugin's name.
	 * @return
	 */
	public String getPluginName()
	{
		return m_Plugin.getName();
	}
	

	/**
	 * Sets the destination host and port to which this object will try to
	 * to connect its Prelert TCP client.  Time Series and notification data
	 * will be sent via this client.
	 * @param host
	 * @param port
	 */
	final public void createBackEndClient(String host, int port)
	{
		if (m_QueryDateTimeProducer == null)
		{
			throw new IllegalStateException("A QueryDateTimeProducer must be set before " +
									"the host and port can be set.");
		}
		
		// The third argument, true, says we'll accept back pressure from
		// the server if it's being overwhelmed with data.
		
		m_Client = new PrelertBackendTCPClient(host, port, true, 
									getDataCollectionMode());
	}
	

	/**
	 * Sets the date time producer for the input manager.
	 * 
	 * This function is not thread safe, once this object is running
	 * it should only be called by itself.
	 * 
 	 * Throws an IllegalStateException if this has started running.
	 * @param value
	 */
	final public void setQueryDateTimeProducer(QueryDateTimeProducer value)
	{		
		if (m_HasStarted)
		{
			throw new IllegalStateException("You cannot set a QueryDateTimeProducer " +
					"on a running InputManager");
		}
		
		m_QueryDateTimeProducer = value;
	}


	/**
	 * Sets the initial message to be sent on startup for this input manager.
	 * 
 	 * Throws an IllegalStateException if this has started running.
	 * @param initialMessage The message to be sent.
	 */
	final public void setInitialMessage(String initialMessage)
	{		
		if (m_HasStarted)
		{
			throw new IllegalStateException("You cannot set an initial message " +
					"on a running InputManager");
		}

		m_InitialMessage = initialMessage;
	}
	
	
	/**
	 * If UpdateDisplayColumnsOnStart is true when this InputManager 
	 * first starts running it will update the GUI display columns 
	 * with a call to {@link Databasemanager#populateDisplayColumns()}
	 * 
	 * UpdateDisplayColumnsOnStart cannot be modified once 
	 * the InputManager has started.
	 * @param UpdateDisplayColumnsOnStart - defaults to false.
	 * @throws IllegalStateException if this is called after
	 * 		the InputManager has started.
	 */
	public void setUpdateDisplayColumnsOnStart(boolean value)
	{
		if (m_HasStarted)
		{
			throw new IllegalStateException("setUpdateDisplayColumnsOnStart() cannot be " +
					"called once the InputManager thread has started.");
		}
		
		m_UpdateDisplayColumnsOnStart = value;
	}


	/**
	 * Return the inputmanagers QueryDateTimeProducer.
	 * @return
	 */
	public QueryDateTimeProducer getQueryDateTimeProducer()
	{
		return m_QueryDateTimeProducer;
	}


	/**
	 * Returns true if this object is configured.
	 * A configured InputManager has a QueryDateTimeProducer.
	 * @return
	 */
	public boolean isConfigured()
	{
		return m_QueryDateTimeProducer != null;
	}


	/**
	 * Sets the time window size for the sync object.
	 * 
	 * Throws an IllegalStateException if this has started running.
	 * 
	 * @param windowSizeSeconds
	 */
	final public void setSyncWindowSize(int windowSizeSeconds)
	{
		if (m_HasStarted)
		{
			throw new IllegalStateException("You cannot set the setSyncWindowSize " +
					"on a running InputManager");
		}
		
		s_InputManagerSync.setMaxWindowSize(windowSizeSeconds);
	}


	/**
	 * Return the query start date.
	 * @return
	 */
	final private Date getQueryStartDate()
	{
		synchronized(m_QueryDateTimeProducer)
		{
			return m_QueryDateTimeProducer.getQueryStartDate();
		}
	}
	
	
	/**
	 * Return the query end date.
	 * @return
	 */
	final private Date getQueryEndDate()
	{
		synchronized(m_QueryDateTimeProducer)
		{
			return m_QueryDateTimeProducer.getQueryEndDate();
		}
	}
	
	
	/**
	 * Set the end time of the last query.
	 * @param value
	 */
	public void setLastQueryEndTime(Date value) throws InputManagerQueriesCompleteException
	{
		synchronized(m_QueryDateTimeProducer)
		{
			m_QueryDateTimeProducer.setLastQueryEndTime(value);
		}
	}


	/**
	 * Get the period in ms at which this <code>InputManager</code> should update.
	 * @return 
	 */
	private long getSleepTimeMs(long lastQueryDurationMs)
	{
		synchronized(m_QueryDateTimeProducer)
		{
			return m_QueryDateTimeProducer.getSleepTimeMs(lastQueryDurationMs);
		}
	}
	
	
	/**
	 * Returns true if this InputManager is running.
	 * @return
	 */
	public boolean isRunning()
	{
		return m_HasStarted && !m_IsFinished;
	}
	
	
	/**
	 * Returns true if the input manager is paused.
	 * @return
	 */
	public boolean isPaused()
	{
		return m_IsPaused;
	}


	/**
	 * Returns true if this has finished.
	 * Finished means the thread has terminated 
	 * so cannot be restarted.
	 * 
	 * @return
	 */
	public boolean isFinished()
	{
		return m_IsFinished;
	}
	
	
	/**
	 * Sets the pause flag. The thread will not pause immediately
	 * only the next time it checks the pause flag. 
	 */
	public void pause()
	{
		synchronized(m_PauseSyncObj)
		{
			m_IsPaused = true;
		}
	}


	/**
	 * Resumes a paused inputmanager.
	 */
	public void resume() 
	{
		synchronized(m_PauseSyncObj)
		{
			m_IsPaused = false;
			m_PauseSyncObj.notify();
		}
	}
	
	
	/**
	 * Returns the collection mode of this inputmanager.
	 * 
	 * If a <code>QueryDateTimeProducer</code> has not been set 
	 * then DataCollectionMode.REALTIME is returned.
	 * @return
	 */
	public DataCollectionMode getDataCollectionMode()
	{
		if (m_QueryDateTimeProducer == null)
		{
			return DataCollectionMode.REALTIME;
		}

		return m_QueryDateTimeProducer.getDataCollectionMode();
	}
		

	/**
	 * Returns true if this manager has finished collecting
	 * historical data.
	 * @return
	 */
	public boolean queriesHaveFinished()
	{
		if (m_QueryDateTimeProducer == null)
		{
			return false;
		}

		return m_QueryDateTimeProducer.queriesHaveFinished();
	}


	/**
	 * Returns the date the CAV started.
	 * If the CAV has not started <code>null</code> will be returned.
	 * 
	 * @return
	 */
	public Date getCavStartedDate()
	{
		return m_StartedDate;
	}


	/**
	 * Returns the date the first CAV query will ask data for.
	 * 
	 * @return A Date or <code>null</code> if not configured.
	 */
	public Date getCavQueriesStartDate()
	{
		if (m_QueryDateTimeProducer == null)
		{
			return null;
		}
		
		synchronized(m_QueryDateTimeProducer)
		{
			return m_QueryDateTimeProducer.getFirstQueryStartDate();
		}
	}


	/**
	 * Returns the Date of the last query that has to be made to 
	 * complete the CAV.
	 * 
	 * If this InputManager is not in CAV mode or running historical
	 * queries <code>null</code> will be returned.
	 * 
	 * @return A Date or <code>null</code>
	 */
	public Date getCavQueriesEndDate()
	{
		if (m_QueryDateTimeProducer == null)
		{
			return null;
		}
		
		synchronized(m_QueryDateTimeProducer)
		{
			return m_QueryDateTimeProducer.getFinalQueryEndDate();
		}
	}
	
	
	/**
	 * Returns the date of the next query to be made.
	 * @return A Date or <code>null</code> if not configured.
	 */
	public Date getNextCavQueryDate()
	{
		if (m_QueryDateTimeProducer == null)
		{
			return null;
		}
		
		synchronized(m_QueryDateTimeProducer)
		{
			return m_QueryDateTimeProducer.getQueryStartDate();
		}
	}

	
	/**
	 * Returns the plugins configured status.
	 * @return
	 */
	public boolean isPluginConfigured()
	{
		return m_Plugin.isConfigured();
	}
	
	
	/**
	 * Set the callback interface that will be called 
	 * when the inputmanager finishes.
	 * @param callback
	 */
	public void setFinishedCallback(ManagerFinished callback)
	{
		m_FinishedCallback = callback;
	}


	/**
	 * Resets the Inputmanager its initialised state.
	 * 
	 * All the state flags (Quit, HasStarted, IsFinished, IsPaused) 
	 * are set to false. 
	 * 
	 * A new TCP backend client is created with the old ones 
	 * settings.
	 */
	public void reset()
	{
		if (isRunning())
		{
			throw new UnsupportedOperationException("The InputManager cannot be reset when it is " +
										"still running.");			
		}
		
		m_StartedDate = null;
		
		m_Quit = false;
		m_HasStarted = false;
		m_IsFinished = false; 
		m_IsPaused = false;
		
		if (m_Client != null)
		{
			m_Client.quit();
			m_Client = new PrelertBackendTCPClient(m_Client.getHost(), m_Client.getPort(), true,
													getDataCollectionMode());
		}
	}
	
	
	/**
	 * Set the database manager.
	 * A database manager is required to set the GUI display columns.
	 * 
	 * @param databaseManager
	 */
	public void setDatabaseManager(DatabaseManager databaseManager)
	{
		m_DatabaseManager = databaseManager;
	}
	
	
	/**
	 * Sets the GUI display columns for <code>datatype</code>.
	 * This should only be called once for each datatype as some 
	 * point during a CAV.
	 * 
	 * @param datatype 
	 * @param category 
	 * @param attributes The attributes that will be the display column headers.
	 */
	protected void populateGuiDisplayColumns(String datatype, DataSourceCategory category, 
							List<Attribute> attributes)
	{
		if (m_DatabaseManager != null)
		{
			m_DatabaseManager.populateDisplayColumns(datatype, category, attributes);
		}
	}
}
