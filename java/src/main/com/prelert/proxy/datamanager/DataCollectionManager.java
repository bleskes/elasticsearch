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

package com.prelert.proxy.datamanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import com.prelert.data.CavStatus;
import com.prelert.data.CavStatus.CavRunState;
import com.prelert.data.TimeMarker;
import com.prelert.process.ProcessManager;
import com.prelert.proxy.Proxy;
import com.prelert.proxy.data.DataTypeConfig;
import com.prelert.proxy.datamanager.UsageDataSender;
import com.prelert.proxy.inputmanager.InputManager;
import com.prelert.proxy.inputmanager.InputManagerFactory;
import com.prelert.proxy.inputmanager.InputManagerInternalTimeSeries;
import com.prelert.proxy.inputmanager.InputManagerThread;
import com.prelert.proxy.inputmanager.DataCollectionMode;
import com.prelert.proxy.inputmanager.dao.InputManagerDAO;
import com.prelert.proxy.inputmanager.querymonitor.HistoricalQueryMonitor;
import com.prelert.proxy.plugin.Plugin;
import com.prelert.proxy.plugin.Plugin.InvalidPluginPropertyException;
import com.prelert.proxy.pluginLocator.PluginLocator;
import com.prelert.proxy.runpolicy.RunPolicy;


/**
 * Manager for the Proxy Data Collection processes.
 * 
 * Manages a collection of InputManagers and synchronises starting/stopping
 * and the state of the managers.
 * 
 * Can be configured to collect data between two historical dates as it would
 * in a CAV or to collect data in realtime. 
 * 
 * The ApplicationContext and PluginLocator properties must 
 * be set on this class before either the {@link #startCav(Date, List)} or
 * {@link #startDataCollection(List)} methods can be called.
 */
public class DataCollectionManager
{
	private static Logger s_Logger = Logger.getLogger(DataCollectionManager.class);
	
	private static final String STATUS_FILENAME = "cavstatus.properties";
	
	/**
	 * The number of hours to pull data for before the CAV incident.
	 */
	private static final int DEFAULT_LEARNING_PERIOD_HOURS = 72;
	private static volatile int s_LearningPeriodHours = DEFAULT_LEARNING_PERIOD_HOURS;
	
	/**
	 * The number of hours to pull data after the CAV incident.
	 */
	private static final int DEFAULT_BUFFER_PERIOD_HOURS = 0;
	private static volatile int s_BufferPeriodHours = DEFAULT_BUFFER_PERIOD_HOURS;

	/**
	 * The number of hours for which to create activities prior to the supplied
	 * incident time.
	 */
	private static final int DEFAULT_ACTIVITY_PERIOD_HOURS = 27;
	private static volatile int s_ActivityPeriodHours = DEFAULT_ACTIVITY_PERIOD_HOURS;

	private List<InputManager> m_InputManagers;
	private List<InputManagerThread> m_InputManagerThreads;
	
	/**
	 * The application context used to create plugins etc.
	 */
	private ApplicationContext m_ApplicationContext;
	
	/**
	 * The plugin locator.
	 */
	private PluginLocator m_PluginLocator;
	
	/**
	 * Database manager.
	 */
	private DatabaseManager m_DatabaseManager;
	
	/**
	 * Tracks the status of the CAV.
	 */
	private CavStatus m_CavStatus;

	/**
	 * Sends usage data to the Prelert website.
	 */
	volatile private UsageDataSender m_UsageDataSender;

	volatile private int m_ManagersFinishedCount;


	/**
	 * Construct in such a way that no usage data will be sent.
	 */
	public DataCollectionManager()
	{
		this(null);
	}


	/**
	 * Construct with a customer ID that will be included in usage data.
	 * @param customerId The customer ID to be included in usage data.  If this
	 *                   is null, no usage data will be sent.
	 */
	public DataCollectionManager(String customerId)
	{
		m_InputManagers = new ArrayList<InputManager>();
		m_InputManagerThreads = new ArrayList<InputManagerThread>();
		
		validateSavedCavStatusOnStartup();

		m_UsageDataSender = new UsageDataSender(customerId);

		m_ManagersFinishedCount = 0;
	}


	/**
	 * Set the customer ID after initial construction.
	 * @param customerId The customer ID to be included in usage data.  If this
	 *                   is null, no usage data will be sent.
	 */
	public void setCustomerId(String customerId)
	{
		m_UsageDataSender = new UsageDataSender(customerId);
	}


	/**
	 * Perform a sanity check on any saved CAV status.
	 * If the Proxy didn't shutdown cleaning it may have left its
	 * status as running. On startup this is clearly wrong so set 
	 * the status to stopped.
	 */
	private void validateSavedCavStatusOnStartup()
	{
		m_CavStatus = loadCavStatus();
		if (m_CavStatus != null)
		{
			if (m_CavStatus.getRunState() == CavRunState.CAV_RUNNING)
			{
				// This is an error Cav cannot be running if the 
				// DataCollectionManager is under construction.
				
				m_CavStatus.setRunState(CavRunState.CAV_STOPPED);
				saveCavStatus();
			}
		}		
		else 
		{
			// set status to default.
			m_CavStatus = new CavStatus();
		}
	}
	
	
	/**
	 * The CavRunState property. 
	 * @param state
	 */
	private void setCavRunState(CavRunState state)
	{
		m_CavStatus.setRunState(state);
	}


	private CavRunState getCavRunState()
	{
		return m_CavStatus.getRunState();
	}


	/**
	 * Create and configure the InputManagers and Plugins as 
	 * defined in the configuration objects.
	 * 
	 * @param configs - The configuration objects to create the 
	 * 	InputManagers and plugins from.
	 * @throws InvalidPluginPropertyException
	 */
	private void createInputManagersAndPlugins(List<DataTypeConfig> configs)
	throws InvalidPluginPropertyException
	{
		InputManagerDAO inputManagerDAO;
		try
		{
			inputManagerDAO = m_ApplicationContext.getBean("inputManagerDAO", InputManagerDAO.class);
		}
		catch (BeansException be)
		{
			s_Logger.fatal("Could not load application bean 'inputManagerDAO'");
			s_Logger.fatal(be);
			
			throw be;
		}	
		
		for (DataTypeConfig config : configs)
		{
			Plugin plugin = m_ApplicationContext.getBean(config.getPluginName(), Plugin.class);
			plugin.setName(config.getPluginName());
			plugin.setDataType(config.getDataType());
			
			Properties pluginProps = new Properties();
			pluginProps.putAll(config.getPluginProperties());
			pluginProps.put("DataType", config.getDataType());
			try
			{
				plugin.configure(config.getSourceConnectionConfig(), pluginProps);
			}
			catch (InvalidPluginPropertyException ppe)
			{
				s_Logger.error("Invalid property for plugin '" + plugin.getName() + "'");
				throw ppe;
			}

			
			// Only register this plugin with the PluginLocator if
			// is it used only for the GUI or if it is an external type.
			boolean isPluginForGui = (config.getInputManagerConfig() == null);
			if (isPluginForGui)
			{
				plugin.initialiseForGui(null);
			}
			else if (RunPolicy.isCannedDemo())
			{
				// For a canned demo, the GUI may display more metrics than are
				// being pulled for analysis, so we can't rely on the standard
				// queries to populate the metric cache used for the explorer view.
				plugin.initialiseForGui(null);
			}

			// Do we have an input manager for this plugin?
			boolean isExternalPlugin = false;
			if (config.getInputManagerConfig() != null)
			{		
				InputManager inputManager = InputManagerFactory.newInputManager(config.getInputManagerConfig(), 
						inputManagerDAO); 

				isExternalPlugin = config.getInputManagerConfig().getInputManagerType().isExternalType();

				boolean isHistoricalInputManager = inputManager.getDataCollectionMode().equals(DataCollectionMode.HISTORICAL);
				if (isHistoricalInputManager || RunPolicy.isDownloadProduct())
				{
					plugin.setQueryMonitorPolicy(new HistoricalQueryMonitor());
				}

				inputManager.setPlugin(plugin);
				addInputManager(inputManager);
			}
			else
			{		
				s_Logger.info("No InputManager set for datatype " + config.getDataType());
			}
				
			// Set the plugin as the seed plugin for its dataype 
			// with the plugin locator.
			if (isPluginForGui || isExternalPlugin)
			{
				// Register plugin with the plugin locator.
				try 
				{
					m_PluginLocator.registerPlugin(plugin);
				}
				catch (IllegalStateException e)
				{
					s_Logger.error("Invalid config file " + config.getDataType() +
							". A existing plugin has already been " +
							"defined to handle data type '" + e.getMessage() +
					"'.  Only one plugin can handle each data type.");	
				}
			}
			
			
			String msg = "Registered plugin '" + plugin.getName() +	"' for data type '" + plugin.getDataSourceType();
			if (config.getInputManagerConfig() != null)
			{
				msg = msg + "' with a new input manager of type " + config.getInputManagerConfig().getInputManagerType();
			}
			
			s_Logger.info(msg);
					
		}		
	}
	
	
	/**
	 * Add an Input manager to be managed by this object.
	 * @param inputManager
	 */
	private void addInputManager(InputManager inputManager)
	{
		final DataCollectionManager dataManager = this;
		inputManager.setFinishedCallback(new InputManager.ManagerFinished() {
			@Override
			public void doFinished(InputManager manager) 
			{
				dataManager.inputManagerFinished(manager);
			}
		});
		
				
		m_InputManagers.add(inputManager);
		m_InputManagerThreads.add(new InputManagerThread(inputManager));
	}


	/**
	 * Notify the plugins that they are about to start so any 
	 * extra initialisation or processes can be setup.
	 * 
	 * @return true if a plugin was started.
	 */
	private boolean startPlugins()
	{
		for (InputManager manager : m_InputManagers)
		{
			manager.getPlugin().start();
		}

		return m_InputManagers.size()  > 0;
	}
	
	/**
	 * Notify the plugins the we are shutting down so they can
	 * stop any processes.
	 * 
	 * @return
	 */
	private boolean stopPlugins()
	{
		boolean result = m_InputManagerThreads.size() > 0;

		s_Logger.info("About to stop " + m_InputManagers.size() + " plugins");

		for (InputManager manager : m_InputManagers)
		{
			manager.getPlugin().stop();
		}

		if (result == true)
		{
			s_Logger.info("Stopped " + m_InputManagers.size() + " plugins");
		}

		return result;
	}
	
	
	/**
	 * Start collecting data for a CAV in the period leading 
	 * up to <code>timeOfIncident</code>.
	 * 
	 * Creates the plugins and inputManagers for the specified 
	 * datatypes.
	 * </b>
	 * Can only be called when the RunPolicy isDownLoadProduct.
	 * 
	 * @param timeOfIncident - time of the CAV incident to analyse. 
	 * @param datatypes - List of datatypes that will be used in the CAV.
	 * @return true if the CAV started successfully.
	 * @throws InvalidPluginPropertyException if the Plugin properties aren't
	 * 	set correctly.
	 * @throws UnsupportedOperationException if this is the <i>Full</i> product. 
	 */
	public boolean startCav(Date timeOfIncident, List<DataTypeConfig> datatypes)
	throws InvalidPluginPropertyException
	{
		if (RunPolicy.isDownloadProduct() == false)
		{
			throw new UnsupportedOperationException("startCAV() can only be called in " +
										"the download product.");
		}
		
		// Stop the processes in case they are running.
		ProcessManager.stopProcesses(); 

		boolean started = false;
		
		boolean reset =	resetInputManagers();
		if (reset)
		{
			// Reset any static data in the Plugins
			// Reset the latest time and data points counts.
			for (PluginLocator.ThreadLocalPlugin plugin : m_PluginLocator.getPlugins())
			{
				plugin.getSeedPlugin().reset();
			}
			
			// Remove all previous plugins from the pluginlocator.
			m_PluginLocator.clear();
			try
			{
				// Set the CAV dates.
				if (timeOfIncident != null)
				{
					m_CavStatus.setTimeOfIncident(timeOfIncident);
					
					DataCollectionManager.CavStartEnd cavDates = calcCavStartAndEndFromIncidentTime(timeOfIncident);

					m_CavStatus.setFirstCavQueryDate(cavDates.getStart());
					m_CavStatus.setLastCavQueryDate(cavDates.getEnd());
					
					for (DataTypeConfig config : datatypes)
					{
						config.getInputManagerConfig().setStartDate(cavDates.getStart());
						config.getInputManagerConfig().setEndDate(cavDates.getEnd());
					}
				}
				
				createInputManagersAndPlugins(datatypes);
				
				// Ask the input manager to update the gui columns
				// after the CAV has started and got some data
				for (InputManager inputManager : m_InputManagers)
				{
					inputManager.setUpdateDisplayColumnsOnStart(true);
					inputManager.setDatabaseManager(m_DatabaseManager);
				}
			}
			catch (InvalidPluginPropertyException e)
			{
				s_Logger.error(e);
				throw e;
			}
			
			started = ProcessManager.startProcesses();
			if (started)
			{
				sendStartMarker(m_CavStatus.getFirstCavQueryDate());
			}
								
			started = started && startPlugins() && startInputManagers();
		}
	
	
		if (reset && started)
		{
			setCavRunState(CavRunState.CAV_RUNNING);
		}
		saveCavStatus();
		
		
		return reset && started;
	}

	
	/**
	 * If a CAV has already run and completed then configure the plugins
	 * to service the gui but don't create any InputManagers. 
	 * 
	 * @param datatypes - The list of datatypes which have plugins
	 * 	that need to be setup for the GUI.
	 * @return true if the plugin(s) were setup for the gui.
	 */
	public boolean startForGUI(List<DataTypeConfig> datatypes)
	{
		// copy the datatyps and set the inputmanager config to
		// null so no inputmanager is created.
		List<DataTypeConfig> copiedTypes = new ArrayList<DataTypeConfig>();
		for (DataTypeConfig type : datatypes)
		{
			DataTypeConfig copy = new DataTypeConfig(type);
			copy.setInputManagerConfig(null);
			
			copiedTypes.add(copy);
		}
		
		m_PluginLocator.clear();
		try
		{		
			createInputManagersAndPlugins(copiedTypes);
		}
		catch (InvalidPluginPropertyException e)
		{
			String msg = "Error creating the plugins for the GUI of a previously run analysis. " +
						"Error message=" + e;
			s_Logger.error(msg);
			return false;
		}

		return (copiedTypes.size() > 0) && startPlugins();
	}
	
	

	/**
	 * Cancel a CAV, for example if it's using too much CPU or memory on the
	 * machine it's running on.
	 *
	 * Can only be called when the RunPolicy isDownLoadProduct.
	 *
	 * @return true if the CAV is successfully cancelled, otherwise false.
	 */
	public boolean stopCav()
	{
		if (RunPolicy.isDownloadProduct() == false)
		{
			throw new UnsupportedOperationException("stopCav() can only be called in " +
										"the download product.");
		}

		// Stop collecting data.
		boolean stoppedDataCollection = stopDataCollection();

		// Stop the processes if they are running.
		boolean stoppedProcesses = ProcessManager.stopProcesses();

		return stoppedDataCollection && stoppedProcesses;
	}


	/**
	 * Creates the plugins and inputManagers for the specified 
	 * datatypes and starts the new inputManagers.
	 * 
	 * Can only be called when the RunPolicy is 
	 * <em>not</em> isDownLoadProduct. This function does
	 * not control the Prelert processes.
	 * 
	 * @param datatypes - List of datatypes that will be used in the CAV.
	 * @return true if the CAV started successfully.
	 * @throws InvalidPluginPropertyException if the Plugin properties aren't
	 * 	set correctly.
	 * @throws UnsupportedOperationException if this is the <i>Download</i> product.
	 */
	public boolean startDataCollection(List<DataTypeConfig> datatypes)
	throws InvalidPluginPropertyException
	{
		if (RunPolicy.isDownloadProduct() == true)
		{
			throw new UnsupportedOperationException("startDataCollection() can only be called in " +
										"the full product.");
		}

		boolean reset = resetInputManagers();
		if (reset)
		{
			// Remove all previous plugins from the pluginlocator.
			m_PluginLocator.clear();
			try
			{
				createInputManagersAndPlugins(datatypes);
			}
			catch (InvalidPluginPropertyException e)
			{
				s_Logger.error(e);
				throw e;
			}
		}
		
		return reset &&	startPlugins() && startInputManagers();
	}


	/**
	 * Pause the data collection.
	 * 
	 * @return true if successful.
	 */
	public boolean pauseDataCollection()
	{
		return pauseInputManagers();
	}


	/**
	 * Resume a paused data collection process.
	 * 
	 * @return true if successful.
	 */
	public boolean resumeDataCollection()
	{
		return resumeInputManagers();
	}


	/**
	 * Stops the Plugins and the InputManagers.
	 * @return
	 */
	public boolean stopDataCollection()
	{
		boolean inputManagersStopped = stopInputManagers();
		boolean pluginsStopped = stopPlugins();

		setCavRunState(CavRunState.CAV_STOPPED);

		return inputManagersStopped && pluginsStopped;
	}


	/**
	 * Starts the Input manager threads only if the InputManager collects
	 * data in real time <em>or</em> if the managers are in historical mode  
	 * and are configured (has start & end dates set).
	 */
	private boolean startInputManagers()
	{
		m_ManagersFinishedCount = 0;
		s_Logger.debug("Reset finished input manager count to 0");
		
		if (m_InputManagerThreads.size() > 0)
		{
			String mode = m_InputManagerThreads.get(0).getDataCollectionMode().toString();
			s_Logger.info("Starting InputManagers in " + mode + " mode.");
		}	
		else
		{
			s_Logger.info("No InputManagers to Start");
		}
		
		boolean result = m_InputManagerThreads.size() > 0;

		
		for (InputManagerThread managerThread : m_InputManagerThreads)
		{
			if (managerThread.getDataCollectionMode() == DataCollectionMode.REALTIME)
			{
				managerThread.start();
			}
			else if (managerThread.getDataCollectionMode() == DataCollectionMode.HISTORICAL &&
										managerThread.isConfigured())
			{
				managerThread.start();
			}
			else
			{
				result = false;
			}

			// now sleep for a little bit so all the managers don't start at once.
			try 
			{
				Thread.sleep(5000); 
			}
			catch (InterruptedException e) 
			{
				s_Logger.error("Proxy interrupted whilst start the inputmanagers");
				return false;
			}
		}

		if (result == true)
		{
			s_Logger.info("Started " + m_InputManagerThreads.size() + " input managers");
		}

		return result;
	}
	
	
	/**
	 * Pauses the input managers. Call <code>resumeInputManagers()</code> to
	 * restart them.
	 * 
	 * @return true if the inputmanagers were running and the pause flag
	 * 		   has been set.
	 */
	private boolean pauseInputManagers()
	{
		if (getCavRunState().equals(CavRunState.CAV_RUNNING))
		{
			s_Logger.info("Pausing the InputManagers");
			
			for (InputManagerThread thread : m_InputManagerThreads)
			{
				thread.pauseInputManager();
			}

			setCavRunState(CavRunState.CAV_PAUSED);
			
			return true;
		}

		return false;
	}


	/**
	 * Resumes paused inputmanagers.
	 * @return true if the managers were running and have now been resumed.
	 */
	private boolean resumeInputManagers()
	{
		if (getCavRunState() == CavRunState.CAV_PAUSED)
		{
			s_Logger.info("Resuming paused InputManagers");
			
			for (InputManagerThread thread : m_InputManagerThreads)
			{
				thread.resumeInputManager();
			}
			
			setCavRunState(CavRunState.CAV_RUNNING);
	
			return true;
		}

		return false;
	}
	
	
	/**
	 * Stops the Input manager threads.
	 * Non-blocking call, returns immediately.
	 */
	@SuppressWarnings("unused")
	private boolean stopInputManagerAsync()
	{
		s_Logger.info("Stopping the InputManagers asynchronously");
		
		for (InputManagerThread thread : m_InputManagerThreads)
		{
			thread.stopInputManager();
			thread.interrupt();			
		}
		
		if (getCavRunState() != CavRunState.CAV_FINISHED)
		{
			setCavRunState(CavRunState.CAV_STOPPED);
		}
		
		// Save the CAV Status as stopped.
		saveCavStatus();
		
		return true;
	}
	
	
	/**
	 * Stops the Input manager threads.
	 * Blocking call: waits for each input manager to terminate.
	 * 
	 * @return true
	 */
	private boolean stopInputManagers()
	{
		s_Logger.info("Stopping the InputManagers");
		
		for (InputManagerThread thread : m_InputManagerThreads)
		{
			thread.stopInputManager();
			thread.interrupt();			
		}
		
		for (InputManagerThread thread : m_InputManagerThreads)
		{
			try
			{
				s_Logger.info("About to join input manager thread");
				thread.join();
			}
			catch (InterruptedException e)
			{
				s_Logger.debug("Main thread interrupted.", e);
			}		
		}
		
		if (getCavRunState() != CavRunState.CAV_FINISHED)
		{
			setCavRunState(CavRunState.CAV_STOPPED);
		}

		// Save the CAV Status as stopped.
		getCavStatus();
		
		return true;
	}
	
	
	/**
	 * Callback called by each of the inputmanagers when
	 * they have finished executing. If all inputmanagers have
	 * finished 
	 *
	 * @param manager
	 */
	private void inputManagerFinished(InputManager manager)
	{
		++m_ManagersFinishedCount;

		s_Logger.info("An input manager finished" +
					" - finished count now " + m_ManagersFinishedCount +
					" - total number of input managers " + m_InputManagers.size());

		if (m_ManagersFinishedCount == m_InputManagers.size())
		{
			inputManagersFinished();
		}
	}
	
	
	/**
	 * Performs house keeping once all the inputmanagers have finished.
	 * Sets the status to finished, updates the usage info and stops
	 * the plugins.
	 */
	private void inputManagersFinished()
	{
		s_Logger.info("All InputManagers have finished");

		// Must get the usage data BEFORE stopping the plugins
		s_Logger.info("About to attempt usage data send");
		for (InputManager manager : m_InputManagers)
		{
			// Only send the data if the input manager stopped because it
			// finished its queries, NOT if it stopped for another reason
			if (manager.queriesHaveFinished())
			{
				Map<String, String> pluginArgs = manager.getPlugin().getPluginSpecificUsageData();
				m_UsageDataSender.sendEndMessage(pluginArgs);
			}
		}

		sendEndMarker();
		
		stopPlugins();
	}
	

	/**
	 * Reset all the inputmanager threads.
	 *  
	 * The inputmanager threads cannot be re-used once they have terminated. 
	 * This method clears the threads and the inputmanagers which will have 
	 * to be created again.
	 * 
	 * If any of the old threads are still running this function will 
	 * return false. It will only succeed if the threads have not yet started 
	 * or have finished executing.
	 * 
	 * @return true if all the InputManagerThreads were reset.
	 */
	private boolean resetInputManagers()
	{		
		for (InputManagerThread thread : m_InputManagerThreads)
		{
			if (thread.isRunning())
			{
				s_Logger.error("Cannot reset the InputManagers when they are running");
				return false;
			}
		}			

		
		m_InputManagers = new ArrayList<InputManager>();
		m_InputManagerThreads = new ArrayList<InputManagerThread>();		
		
				
		return true;
	}
	
	
	/**
	 * In the download product, tell each historical input manager to
	 * send a time marker containing the CAV start time before its own
     * data stream.  In the full product we don't do this because the
	 * setup may be more complex, with data being fed into the back-end
	 * via ULFs and/or detectives as well as the proxy, and we have no
	 * no idea when they started and where they're up to.  For the
	 * download product the assumption is that data is ONLY being
	 * gathered via the proxy.
	 * 
	 * @param startTime -The CAV start time.
	 */
	private void sendStartMarker(Date startTime)
	{
		if (RunPolicy.isDownloadProduct())
		{
			for (InputManager inputManager : m_InputManagers)
			{
				if (inputManager.getDataCollectionMode() == DataCollectionMode.HISTORICAL)
				{
					TimeMarker startMarker = new TimeMarker(startTime);
					inputManager.setInitialMessage(startMarker.toXml());
				}
			}

		}
	}
	
	/**
	 * If the download product then send the end marker to the 
	 * Prelert processes. This should only be called once all
	 * InputManagers have finished.
	 * 
	 * If there are a mixture of internal (sending to the point writer) 
	 * and external (sending to the feature detector) input managers,
	 * the end-of-time marker should be sent via an internal input manager,
	 * to ensure it passes through the point writer.
	 * 
	 * In the full product we don't do this because the setup 
	 * may be more complex, with data being fed into the back-end 
	 * via ULFs and/or detectives as well as the proxy, and 
	 * since we have no idea where they're up to, we can't
	 * say that all data has been collected.  For the download product
	 * the assumption is that data is ONLY being gathered via the
	 * proxy.
	 */
	private void sendEndMarker()
	{
		if (RunPolicy.isDownloadProduct())
		{
			TimeMarker endOfTime = new TimeMarker(TimeMarker.SpecialTime.END_OF_TIME);
			
			boolean sentViaInternalManager = false;
			for (InputManager manager : m_InputManagers)
			{
				if (manager instanceof InputManagerInternalTimeSeries)
				{
					manager.transferMessage(false, endOfTime.toXml());
					sentViaInternalManager = true;
					break;
				}
			}
			
			if (sentViaInternalManager == false)
			{
				// Send the message by the first available manager.
				if (m_InputManagers.size() > 0)
				{
					m_InputManagers.get(0).transferMessage(false, endOfTime.toXml());
				}
			}

		}
	}
	
	
	/**
	 * Returns the time of the incident set for the CAV.
	 * If no incident time has been set the result may be <code>null</code>.
	 * 
	 * @return may be <code>null</code>
	 */
	public Date getCavTimeOfIncident()
	{
		return m_CavStatus.getTimeOfIncident();
	}

	
	/**
	 * Returns true only if <i>all</i> InputManagers are
	 * in CAV mode.
	 * 
	 * @return true if all InputManagers are running in CAV mode.
	 */
	public boolean isCavRunning()
	{
		boolean isCavRunning = m_InputManagerThreads.size() > 0;
		
		for (InputManagerThread inputManagerThread : m_InputManagerThreads)
		{
			isCavRunning = isCavRunning && inputManagerThread.isCavRunning();
		}
		
		if (isCavRunning)
		{
			setCavRunState(CavRunState.CAV_RUNNING);
		}
		
		return isCavRunning;
	}
	
	
	/**
	 * Returns true only if <em>all</em> InputManagers have finished
	 * the CAV. 
	 * 
	 * @return true if all InputManagers are running in CAV mode.
	 */
	public boolean isCavFinished()
	{
		boolean isCavFin = m_InputManagerThreads.size() > 0;
		
		for (InputManagerThread inputManagerThread : m_InputManagerThreads)
		{
			isCavFin = isCavFin && inputManagerThread.isCavFinished();
		}
		
		if (isCavFin)
		{
			setCavRunState(CavRunState.CAV_FINISHED);
		}
		
		return isCavFin;	
	}
		

	/**
	 * Returns the CAV status. 
	 * If no CAV is running a default constructed CavStatus object is returned.
	 * If a CAV has previously been run and no new CAV has been started 
	 * then the previous CAV status is returned.
	 * 
	 * The returned status object is also saved to disk.
	 * 
	 * @return
	 */
	public CavStatus getCavStatus()
	{
		if (m_InputManagerThreads.size() > 0)
		{
			InputManagerThread manager = m_InputManagerThreads.get(0);
			
			m_CavStatus.setFatalErrorMessage("");
			
			m_CavStatus.setDateCavStarted(manager.getCavStartedDate());
			m_CavStatus.setCurrentCavQueryDate(manager.getNextCavQueryDate());
			
			float progressPercent = 0.0f;
			
			boolean running = manager.isCavRunning();
			boolean finished = manager.isCavFinished();
			// manager is stopped if the cav is finished but
			//  all the queries weren't completed.
			boolean stopped = finished && manager.getInputManager().queriesHaveFinished() == false; 

			if (stopped)
			{
				m_CavStatus.setRunState(CavRunState.CAV_STOPPED);
				progressPercent = calcProgress(m_CavStatus.getFirstCavQueryDate(), 
											m_CavStatus.getLastCavQueryDate(),
											m_CavStatus.getCurrentCavQueryDate());
			}
			else if (!running && !finished)
			{
				m_CavStatus.setRunState(CavRunState.CAV_NOT_STARTED);
			}
			else if (finished)
			{
				m_CavStatus.setRunState(CavRunState.CAV_FINISHED);
				progressPercent = 100.0f;
			}
			else
			{
				m_CavStatus.setRunState(CavRunState.CAV_RUNNING);	
				
				progressPercent = calcProgress(m_CavStatus.getFirstCavQueryDate(),
												m_CavStatus.getLastCavQueryDate(),
												m_CavStatus.getCurrentCavQueryDate());
				
				// don't let progress get above 99% if not finished yet.
				progressPercent = Math.min(progressPercent, 99.0f);
			}

			m_CavStatus.setProgressPercent(progressPercent);
			
			saveCavStatus();
		}
		
		// Return a copy of the status object 
		// with the current date time stamp.
		return new CavStatus(m_CavStatus);
	}


	/**
	 * Returns the progress as a percentage. 
	 * 
	 * If any of the parameters are null 0.0 is returned.
	 * 
	 * @param firstQuery
	 * @param lastQuery
	 * @param currentQuery
	 * @return
	 */
	private float calcProgress(Date firstQuery, Date lastQuery, Date currentQuery)
	{
		if (firstQuery == null || lastQuery == null || currentQuery == null)
		{
			return 0.0f;
		}
		
		long scale = lastQuery.getTime() - firstQuery.getTime();
		long progress = currentQuery.getTime() - firstQuery.getTime();
		
		return ((float)progress / (float)scale) * 100f;
	}


	/**
	 * Adds <code>s_BufferPeriodHours</code> and subtracts 
	 * <code>s_LearningPeriodHours</code> to the incident 
	 * time and returns this as the time range to collect
	 * data for the CAV.
	 * 
	 *  It will not return a date in the future. 
	 *  
	 * @param timeOfIncident
	 * @return
	 */
	static public CavStartEnd calcCavStartAndEndFromIncidentTime(Date timeOfIncident)
	{
		Calendar calendar = Calendar.getInstance();

		if (RunPolicy.isCannedDemo())
		{
			Date start;
			Date end;

			try
			{
				DateFormat df = new SimpleDateFormat("dd MMM yyyy kk:mm:ss z");
				start = df.parse("29 Feb 2012 00:00:00 GMT");
				end = df.parse("29 Feb 2012 13:00:00 GMT");
			}
			catch (ParseException e)
			{
				s_Logger.error(e);
				return null;
			}

			s_Logger.warn("HARD CODED DATE RANGE FOR DEMO " + start + " to " + end);

			return new CavStartEnd(start, end);
		}

		// Get the end time.
		calendar.setTime(timeOfIncident);
		calendar.add(Calendar.HOUR_OF_DAY, s_BufferPeriodHours);
		
		// Cannot have a incident time in the future.
		if (calendar.getTime().getTime() > new Date().getTime())
		{
			calendar.setTime(new Date());
		}
		Date end = calendar.getTime();
		
		
		// Get the start time.
		calendar.setTime(timeOfIncident);
		if (calendar.getTime().getTime() > new Date().getTime())
		{
			calendar.setTime(new Date());
		}
		calendar.add(Calendar.HOUR_OF_DAY, -s_LearningPeriodHours);
		Date start = calendar.getTime();
		
		return new CavStartEnd(start, end);
	}


	/**
	 * Sets the number of hours data will be analysed for <em>before</em>  
	 * an incident.
	 * @return
	 */
	public void setCavTrainingPeriodHours(int value)
	{
		s_LearningPeriodHours = value;
	}


	/**
	 * Sets the number of hours data will be analysed for <em>after</em> 
	 * an incident.
	 * @return
	 */
	public void setCavDataBufferPeriodHours(int value)
	{
		s_BufferPeriodHours = value;
	}


	/**
	 * Sets the number of hours <em>before</em> the alleged incident time that
	 * Prelert should create activities.
	 */
	public void setCavActivityPeriodHours(int value)
	{
		s_ActivityPeriodHours = value;
	}


	/**
	 * Gets the earliest time that Prelert should create an activity, given the
	 * time of an alleged incident that we're analysing.
	 * @return The earliest time that Prelert should create an activity at.
	 */
	public Date getMinActivityTime(Date timeOfIncident)
	{
		Calendar calendar = Calendar.getInstance();

		if (RunPolicy.isCannedDemo())
		{
			try
			{
				DateFormat df = new SimpleDateFormat("dd MMM yyyy kk:mm:ss z");
				calendar.setTime(df.parse("29 Feb 2012 13:00:00 GMT"));
				calendar.add(Calendar.HOUR_OF_DAY, -s_ActivityPeriodHours);
				return calendar.getTime();
			}
			catch (ParseException e)
			{
				s_Logger.error(e);
			}
		}

		calendar.setTime(timeOfIncident);
		calendar.add(Calendar.HOUR_OF_DAY, -s_ActivityPeriodHours);
		return calendar.getTime();
	}
	
	
	/**
	 * The application context object.
	 * @return
	 */
	public ApplicationContext getApplicationContext()
	{
		return m_ApplicationContext;
	}
	
	public void setApplicationContext(ApplicationContext value)
	{
		m_ApplicationContext = value;
	}
	
	
	/**
	 * The plugin locator.
	 * @return
	 */
	public PluginLocator getPluginLocator()
	{
		return m_PluginLocator;
	}

	public void setPluginLocator(PluginLocator value)
	{
		m_PluginLocator = value;
	}
	
	
	/**
	 * The database manager.
	 * @return
	 */
	public DatabaseManager getDatabaseManager()
	{
		return m_DatabaseManager;
	}

	public void setDatabaseManager(DatabaseManager value)
	{
		m_DatabaseManager = value;
	}
	
	
	/**
	 * Loads the CavStatus from its properties file if the 
	 * file exits.
	 *  
	 * @return CavStatus object or <code>null</code>.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	static private CavStatus loadCavStatus()  
	{		
		File propsFile = new File(Proxy.getProxyConfigDirectory(), STATUS_FILENAME);
		InputStream inputStream;
		
		CavStatus status = null;
		try 
		{
			inputStream = new FileInputStream(propsFile);
			Properties props = new Properties();
			props.load(inputStream);
			
			status = cavStatusFromProperties(props);
		} 
		catch (FileNotFoundException e1) 
		{
			// not an error if the file does not exist yet.
		}
		catch (IOException e)
		{
		} 

		return status;
	}


	/**
	 * Write the Cav Status as properties to STATUS_FILENAME.
	 * @param status
	 */
	private void saveCavStatus()
	{
		File propsFile = new File(Proxy.getProxyConfigDirectory(), STATUS_FILENAME);
		
		try 
		{
			propsFile.createNewFile();
			OutputStream stream = new FileOutputStream(propsFile);

			Properties props = cavStatusToProperties(m_CavStatus);
			
			props.store(stream, "");
		} 
		catch (IOException e)
		{
			s_Logger.error("Error saving Proxy properties." + e);
		}
	}
	

	/**
	 * Convert the status object to a set of properties.
	 * @param status
	 * @return
	 */
	static public Properties cavStatusToProperties(CavStatus status)
	{	
		Properties props = new Properties();
		
		props.setProperty("State", status.getRunState().toString());
		
		if (status.getDateCavStarted() != null) 
		{
			props.setProperty("StartedTime", DateFormat.getInstance().format(status.getDateCavStarted()));
		}
		if (status.getFirstCavQueryDate() != null)
		{
			props.setProperty("FirstCavQueryTime", DateFormat.getInstance().format(status.getFirstCavQueryDate()));
		}
		if (status.getCurrentCavQueryDate() != null)
		{
			props.setProperty("CurrentQueryTime", DateFormat.getInstance().format(status.getCurrentCavQueryDate()));
		}
		if (status.getLastCavQueryDate() != null)
		{
			props.setProperty("LastQueryTime", DateFormat.getInstance().format(status.getLastCavQueryDate()));
		}
		if (status.getTimeOfIncident() != null)
		{
			props.setProperty("TimeOfIncident", DateFormat.getInstance().format(status.getTimeOfIncident()));
		}
		
		props.setProperty("Progress", Float.toString(status.getProgressPercent()));
		props.setProperty("FatalErrorMessage", status.getFatalErrorMessage());
		
		return props;    	
	}


	/**
	 * Load a CavStatus object from a set of properties.
	 * Returns null if State is not defined.
	 * 
	 * @param props
	 * @return result may be null.
	 */
	static public CavStatus cavStatusFromProperties(Properties props) 
	{
		CavStatus status = new CavStatus();
		
		if (props.getProperty("State") == null)
		{
			return null;
		}
		
		try
		{
			if (props.getProperty("State") != null)
			{
				status.setRunState(CavRunState.valueOf(props.getProperty("State")));
			}
			if (props.getProperty("StartedTime") != null)
			{
				status.setDateCavStarted(DateFormat.getInstance().parse(props.getProperty("StartedTime")));
			}
			if (props.getProperty("FirstCavQueryTime") != null)
			{
				status.setFirstCavQueryDate(DateFormat.getInstance().parse(props.getProperty("FirstCavQueryTime")));
			}
			if (props.getProperty("CurrentQueryTime") != null)
			{
				status.setCurrentCavQueryDate(DateFormat.getInstance().parse(props.getProperty("CurrentQueryTime")));
			}
			if (props.getProperty("LastQueryTime") != null)
			{
				status.setLastCavQueryDate(DateFormat.getInstance().parse(props.getProperty("LastQueryTime")));
			}
			if (props.getProperty("TimeOfIncident") != null)
			{
				status.setTimeOfIncident(DateFormat.getInstance().parse(props.getProperty("TimeOfIncident")));
			}
			if (props.getProperty("Progress") != null)
			{
				status.setProgressPercent(Float.parseFloat(props.getProperty("Progress")));
			}
			status.setFatalErrorMessage(props.getProperty("FatalErrorMessage", ""));
		}
		catch (ParseException e) 
		{
			s_Logger.error("Error reading CAV Status properties", e);
			return null;
		}
		catch (NumberFormatException e)
		{
			s_Logger.error("Error reading CAV Status properties", e);
			return null;
		}
		catch (IllegalArgumentException e)
		{
			s_Logger.error("Error reading CAV Status properties", e);
			return null;
		}
		catch (NullPointerException e)
		{
			s_Logger.error("Error reading CAV Status properties", e);
			return null;
		}
		
		
		return status;
	}
	
	
	/**
	 * Helper class to group CAV start & end dates. 
	 */
	static public class CavStartEnd
	{
		private final Date m_CavStart;
		private final Date m_CavEnd;
		
		public CavStartEnd(Date start, Date end)
		{
			m_CavStart = start;
			m_CavEnd = end;
		}
		
		public Date getStart()
		{
			return m_CavStart;
		}
		
		public Date getEnd()
		{
			return m_CavEnd;
		}
	}
}
