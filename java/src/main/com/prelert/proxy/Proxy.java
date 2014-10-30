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

package com.prelert.proxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.prelert.data.BuildInfo;
import com.prelert.data.CavStatus;
import com.prelert.data.ProcessStatus;
import com.prelert.data.CavStatus.CavRunState;
import com.prelert.process.ProcessManager;
import com.prelert.proxy.configuration.ConfigurationManager;
import com.prelert.proxy.datamanager.DataCollectionManager;
import com.prelert.proxy.datamanager.DatabaseManager;
import com.prelert.proxy.inputmanager.InputManagerFactory;
import static com.prelert.proxy.inputmanager.InputManagerSync.MAX_WINDOW_SIZE_MS;
import com.prelert.proxy.pluginLocator.PluginLocator;
import com.prelert.proxy.runpolicy.RunPolicy;
import com.prelert.proxy.server.ControlServerRMI;
import com.prelert.proxy.server.ObjectFactoryServerRMI;


/**
 * Main Proxy class. Contains members which are registered for RMI and implement
 * the remote interfaces for the Causality, DataSource, Evidence, Incident, 
 * TimeSeries and User DAOs.
 * 
 * This class contains a main method which sets up the proxy then returns. The RMI
 * thread keeps the program alive but all references to the proxy are lost thus the
 * proxy may be garbage collected. RMI uses weak references to objects so will not 
 * stop an object being GC'd.
 * The RMI objects <code>ControlServerRMI</code> and <code>ObjectFactoryServerRMI</code>
 * are static members so they exist for the life of the program can never be GC'd
 * and so always be available for RMI calls. 
 * 
 * When the Proxy is invoked additional parameters have to be added to the java
 * command line:
 * The security policy has to be set when using the Security manager which is  
 * required for RMI.
 * -Djava.security.policy=/path/to/security.policy 
 * 
 * If the Proxy is using Postgres then pg.user should be defined e.g.
 * -Dpg.user=$PGUSER
 */
public class Proxy
{
	private static Logger s_Logger = Logger.getLogger(Proxy.class);

	public static final String PROPERTIES_FILE = "proxy.properties";
	
	/**
	 * RMI Objects. These objects are static so a reference to them
	 * should be maintained for the lifetime of the program. This 
	 * should prevent the objects being Garbage collected as RMI does
	 * not keep them alive.
	 */
	static private ControlServerRMI s_ControlServer = null;
	static private ObjectFactoryServerRMI s_RemoteObjectFactory = null;
	
	private Registry m_Registry;
	private Properties m_Properties;
	
	private DataCollectionManager m_DataCollectionManager;
	private ProcessManager m_ProcessManager;
	
	private PluginLocator m_PluginLocator;
	
	private DatabaseManager m_DatabaseManager;
	
	private ConfigurationManager m_ConfigManager;


	public Proxy() throws FileNotFoundException, IOException
	{
		loadProperties();

		// This call must come before any other processing of the properties
		checkForCannedDemo();

		setupInputManagerFactory();
		setupProcessAndDataManagers();
	}


	/**
	 * Check if this is a canned demo - if it is, we'll use a whole raft of
	 * hardcoded hacks.
	 */
	private void checkForCannedDemo()
	{
		String propStr = m_Properties.getProperty("CannedDemo");
		if (propStr != null)
		{
			try
			{
				int isDemo = Integer.parseInt(propStr);
				RunPolicy.setCannedDemo(isDemo != 0);
				s_Logger.info("Applied setting 'CannedDemo' = " + isDemo);
			}
			catch (NumberFormatException e)
			{
				s_Logger.error("Invalid value for 'CannedDemo' = " + propStr);
			}
		}
	}


	/**
	 * The following Proxy specific properties can optionally be defined.
	 * 
	 * 'registryPort' - Defaults to 1099. Change it here to use a different port.
	 * 
	 * 'syncWindowSizeSecs' - For InputManagers to progress they have to be 
	 * 						  requesting data within this time window of each
	 *    					  other.
	 * 'CAVPeriodHours' - The number of hours of data that will be pulled 
	 * 			               before a CAV incident is analysed. 
	 * 'CAVPeriodFutureHours' - The number of hours of data <em>after</em> the
	 * 							incident that data will be pulled for.
	 * 'CAVActivityPeriodHours' - The number of hours before the incident time 
	 * 							activities will be shown for. 
	 * 'log4jPort' - The log4j socket appender port.
	 */
	private void loadProperties() throws FileNotFoundException, IOException
	{
		File propsFile = new File(getProxyConfigDirectory(), PROPERTIES_FILE);
		InputStream inputStream;
		
		try 
		{
			inputStream = new FileInputStream(propsFile);
		} 
		catch (FileNotFoundException e1) 
		{
			s_Logger.fatal("Cannot locate file '" + propsFile);
			throw e1;
		}
		
		m_Properties = new Properties();
		try 
		{
			m_Properties.load(inputStream);
		}
		catch (IOException e)
		{
			s_Logger.fatal("Could not load properties file '" + PROPERTIES_FILE + "'. Error: " + e);
			throw e;
		}
	}
	
	
	/**
	 * Creates and sets up the Data collection manager and
	 * the process manager.
	 */
	private void setupProcessAndDataManagers() throws IOException
	{
		m_DataCollectionManager = new DataCollectionManager();

		String propStr = m_Properties.getProperty("CAVPeriodHours");
		if (propStr != null)
		{
			try
			{
				int trainingPeriod = Integer.parseInt(propStr);
				m_DataCollectionManager.setCavTrainingPeriodHours(trainingPeriod);
				s_Logger.info("Applied setting 'CAVPeriodHours' = " + trainingPeriod);
			}
			catch (NumberFormatException e)
			{
				s_Logger.error("Invalid value for 'CAVPeriodHours' = " + propStr);
			}
		}
		
		propStr = m_Properties.getProperty("CAVPeriodFutureHours");
		if (propStr != null)
		{
			try
			{
				int bufferPeriod = Integer.parseInt(propStr);
				m_DataCollectionManager.setCavDataBufferPeriodHours(bufferPeriod);
				s_Logger.info("Applied setting 'CAVPeriodFutureHours' = " + bufferPeriod);
			}
			catch (NumberFormatException e)
			{
				s_Logger.error("Invalid value for 'CAVPeriodFutureHours' = " + propStr);
			}
		}

		propStr = m_Properties.getProperty("CAVActivityPeriodHours");
		if (propStr != null)
		{
			try
			{
				int activityPeriod = Integer.parseInt(propStr);
				m_DataCollectionManager.setCavActivityPeriodHours(activityPeriod);
				s_Logger.info("Applied setting 'CAVActivityPeriodHours' = " + activityPeriod);
			}
			catch (NumberFormatException e)
			{
				s_Logger.error("Invalid value for 'CAVActivityPeriodHours' = " + propStr);
			}
		}

		
		// Create the process manager.
		propStr = m_Properties.getProperty("log4jPort", "");
		try
		{
			int port = Integer.parseInt(propStr);
			m_ProcessManager = new ProcessManager(port);
		}
		catch (NumberFormatException e)
		{
			if (!propStr.isEmpty())
			{
				s_Logger.error("Invalid value for 'log4jPort' = " + propStr);
			}
			m_ProcessManager = new ProcessManager();
		}
	}
	
	
	/**
	 * Sets the InputManagers' sync window size if the property
	 * has been set.
	 * 
	 * When multiple InputManagers are running they should stay 
	 * within this period of each other when collecting data.
	 */
	private void setupInputManagerFactory()
	{		
		Integer syncWindowSize = null;

		String sizeStr = m_Properties.getProperty("syncWindowSizeSecs");
		if (sizeStr != null)
		{
			try
			{
				syncWindowSize = Integer.parseInt(sizeStr);
			}
			catch (NumberFormatException e)
			{
				s_Logger.error("Cannot parse syncWindowSizeSecs = '" + sizeStr + "'." +
						"Using default of " + MAX_WINDOW_SIZE_MS / 1000);
			}
		} 

		if (syncWindowSize != null)
		{
			InputManagerFactory.setInputManagerWindowSize(syncWindowSize);
		}	
	}
	
	
	/**
	 * Load the template and full data source configs into the 
	 * configuraton manager.
	 */
	private void loadConfigurations()
	{
		m_ConfigManager.loadConfigurations(new File(getDataTypesConfigDirectory()));
		m_ConfigManager.loadTemplateConfigurations(new File(getTemplateDataTypesConfigDirectory()));
	}
		

	/**
	 * Shut down the proxy. Unbinds the RMI servers and stops the 
	 * Input Manager thread. This should be enough to stop the app
	 * but a call to System.exit() is required.
	 * @return
	 */
	public boolean shutdown()
	{
		s_Logger.info("Proxy is shutting down");
		
		// Disconnect the RMI servers.
		try
		{
			m_Registry.unbind(s_ControlServer.getServerName());
			m_Registry.unbind(s_RemoteObjectFactory.getServerName());
			
			s_ControlServer = null;
			s_RemoteObjectFactory = null;
			m_Registry = null;
		}
		catch (NoSuchObjectException e)
		{
			s_Logger.error("Proxy shutdown error: " + e);
		}
		catch (Exception e)
		{
			s_Logger.error("Proxy shutdown error: " + e);			
		}
		
		m_DataCollectionManager.stopDataCollection();
		if (RunPolicy.isDownloadProduct()) 
		{
			ProcessManager.stopProcesses();
		}

		m_ProcessManager.stopProcessMonitor();
		
		// Have to force the system to shut down by calling System.exit();
		// TODO shouldn't have to quit this why, figure out why.
		 
		// Shut down from a timer so this function can still return- maybe, hopefully.
		class ShutdownTask extends TimerTask 
		{
			public void run() { System.exit(0); }
		}
		
		ShutdownTask shutdownTask = new ShutdownTask();
		
		Timer timer = new Timer();
		timer.schedule(shutdownTask, 200);

		return true;
	}
	
	/**
	 * Kills the proxy and returns immediately without waiting 
	 * for threads to complete.
	 * Calls System.exit().
	 */
	public void kill()
	{
		s_Logger.error("Killing the Proxy");
		// Disconnect the RMI servers.
		try
		{
			m_Registry.unbind(s_ControlServer.getServerName());
			m_Registry.unbind(s_RemoteObjectFactory.getServerName());
			
			s_ControlServer = null;
			s_RemoteObjectFactory = null;
			m_Registry = null;
		}
		catch (NoSuchObjectException e)
		{
			
		}
		catch (Exception e)
		{
			
		}
		
		System.exit(0);
	}

	
	/**
	 * Returns the directory for the Proxy config files.
	 * 
	 * Gets the config directory location from the 
	 * <code>prelert.config.dir</code> system property
	 * 
	 * @return
	 */
	static public String getProxyConfigDirectory()
	{
		String configDirectory = System.getProperty("prelert.config.dir");
		if (configDirectory == null)
		{
			s_Logger.error("The prelert.config.dir property is not defined.");
			configDirectory = "";
		}
		
		return configDirectory;
	}
	
	
	/**
	 * Returns the directory for the Proxy Plugin config files.
	 * Convenience function returns a sub-directory of 
	 * getProxyConfigDirectory()
	 * 
	 * @return
	 */
	static public String getPluginsConfigDirectory()
	{
		String configDirectory = getProxyConfigDirectory();
		String pluginsConfigDir = configDirectory + File.separator + "plugins";
		
		return pluginsConfigDir;
	}
	
	/**
	 * Returns the full path to the data source config directory.
	 * @return
	 */
	static public String getDataTypesConfigDirectory()
	{
		String configDirectory = getProxyConfigDirectory();
		String pluginsConfigDir = configDirectory + File.separator + "datatypes";
		
		return pluginsConfigDir;	
	}
	
	/**
	 * Returns the full path to the template data source config directory.
	 * @return
	 */
	static public String getTemplateDataTypesConfigDirectory()
	{
		String configDirectory = getProxyConfigDirectory();
		String pluginsConfigDir = configDirectory + File.separator + "templates";
		
		return pluginsConfigDir;	
	}
	
	/**
	 * Returns the status of the prelert processes.
	 * @return
	 */
	public List<ProcessStatus> getProcessesStatus()
	{
		return m_ProcessManager.getProcessesStatus();
	}
	
	/**
	 * Returns the data collection manager.
	 * @return
	 */
	public DataCollectionManager getDataCollectionManager()
	{
		return m_DataCollectionManager;
	}

	
	/**
	 * Returns the CAV status. 
	 * If the process manager has seen a fatal error then the returned 
	 * status is always CAV_ERROR.
	 * 
	 * @return
	 */
	public CavStatus getCavStatus()
	{
		CavStatus cavStatus =  m_DataCollectionManager.getCavStatus();
		if (m_ProcessManager.hasFatalError())
		{
			cavStatus.setRunState(CavRunState.CAV_ERROR);
			cavStatus.setFatalErrorMessage(m_ProcessManager.getFatalErrorMessage());
		}
		
		return cavStatus;
	}
			
	/**
	 * Get the port on which the RMI registry should run from the properties
	 * file.  If there is no registryPort entry in the properties file, or
	 * if it is invalid, return the default RMI port (1099).
	 * @return The TCP port to use for RMI.
	 */
	public int getRegistryPort()
	{
		int registryPort = Registry.REGISTRY_PORT;

		String portStr = m_Properties.getProperty("registryPort");
		if (portStr != null)
		{
			try
			{
				registryPort = Integer.parseInt(portStr);
			}
			catch (NumberFormatException e)
			{
				s_Logger.error("RMI registry port '" + portStr +
							"' cannot be parsed to an integer - defaulting to " +
							registryPort, e);
			}
		}
		else
		{
			s_Logger.info("registryPort not specified in properties file - " +
							"defaulting to " + registryPort);
		}

		return registryPort;
	}
	
	
	/**
	 * Get the RMI registry object, setting one up if necessary.
	 * If a registry needs to be set up, first try to create a new one, and if
	 * that doesn't work, try to connect to an existing one.
	 * @return The RMI registry object, or null if it's not possible to set one
	 *         up.
	 */
	public Registry getRegistry()
	{
		// If we've already set up a registry, just return the existing one
		if (m_Registry != null)
		{
			return m_Registry;
		}

		// We haven't already set up a registry, so do it now
		int registryPort = getRegistryPort();
		try
		{
			s_Logger.info("Attempting to start an RMI registry on port " +
							registryPort);
			m_Registry = LocateRegistry.createRegistry(registryPort);
			s_Logger.info("Started RMI registry on port " +
							registryPort);
		}
		catch (RemoteException remEx1)
		{
			try
			{
				s_Logger.info("Failed to start RMI registry on port " +
							registryPort +
							" - attempting to connect to an existing one");
				m_Registry = LocateRegistry.getRegistry(registryPort);

				// We need to interact with the external registry to confirm
				// whether it's actually an RMI registry and not some other TCP
				// server
				@SuppressWarnings("unused")
				String [] methods = m_Registry.list();
				s_Logger.info("Connected to RMI registry on port " +
								registryPort);
			}
			catch (RemoteException remEx2)
			{
				s_Logger.fatal("Failed to connect to an RMI registry on port " +
							registryPort +
							" - maybe this port is in use by another program?",
							remEx2);

				// If the registry was partially set up, it may have created a
				// thread that will stop the JVM from exiting, so stop this
				try
				{
					UnicastRemoteObject.unexportObject(m_Registry, true);
				}
				catch (Exception e)
				{
					// No need to tell the user about any exceptions here
				}
				m_Registry = null;
			}
		}

		return m_Registry;
	}
	
	
	/**
	 * Plugin locator property.
	 * @return
	 */
	public PluginLocator getPluginLocator()
	{
		return m_PluginLocator;
	}


	public void setPluginLocator(PluginLocator pluginLocator)
	{
		m_PluginLocator = pluginLocator;
	}


	/**
	 * Database manager property.
	 * @return
	 */
	public DatabaseManager getDatabaseManager()
	{
		return m_DatabaseManager;
	}


	public void setDatabaseManager(DatabaseManager databaseManager)
	{
		m_DatabaseManager = databaseManager;
	}
	

	/**
	 * Configuration manager property.
	 * @return
	 */
	public ConfigurationManager getConfigurationManager()
	{
		return m_ConfigManager;
	}

	public void setConfigurationManager(ConfigurationManager configurationManager)
	{
		m_ConfigManager = configurationManager;
	}
	

	/**
	 * Main entry point for the Prelert Proxy.
	 * 
	 * @param args
	 * @throws RemoteException
	 * @throws IOException
	 */
	public static void main(String args[])
	{
		// Configure logging
		BasicConfigurator.configure();

		// Log the copyright and version
		s_Logger.info(BuildInfo.fullInfo("Proxy"));

		// Create and install a security manager
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new RMISecurityManager());
		}
		
		
		ApplicationContext applicationContext;
		Proxy proxy;
		try
		{

			applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");	

			proxy = applicationContext.getBean("proxy", Proxy.class);

			if (proxy.m_DatabaseManager != null)
			{
				proxy.m_DataCollectionManager.setCustomerId(proxy.m_DatabaseManager.getCustomerId());
			}
			else
			{
				s_Logger.error("Proxy database manager is null");
			}
		}
		catch (Exception e)
		{
			s_Logger.fatal("Error intialising the Proxy.", e);
			System.exit(1);
			return;
		}

		try
		{
			// Setup static server objects.
			Proxy.s_ControlServer = new ControlServerRMI(proxy);
			Proxy.s_RemoteObjectFactory = new ObjectFactoryServerRMI();
		}
		catch (RemoteException e)
		{
			s_Logger.fatal("Error create RMI server objects");
			System.exit(1);
		}

		Registry registry = proxy.getRegistry();
		if (registry != null)
		{
			try
			{
				Proxy.s_RemoteObjectFactory.setApplicationContext(applicationContext);

				registry.bind(Proxy.s_ControlServer.getServerName(),
								Proxy.s_ControlServer);
				registry.bind(Proxy.s_RemoteObjectFactory.getServerName(),
								Proxy.s_RemoteObjectFactory);

				
				proxy.loadConfigurations();
				proxy.m_DataCollectionManager.setApplicationContext(applicationContext);
				proxy.m_DataCollectionManager.setPluginLocator(proxy.m_PluginLocator);
				proxy.m_DataCollectionManager.setDatabaseManager(proxy.m_DatabaseManager);
				
							
				proxy.m_ProcessManager.startProcessMonitor();

				// Start automatically if not the download product else wait
				// for the start command.
				if (RunPolicy.isDownloadProduct() == false)
				{
					s_Logger.info("Start the Prelert full product");

					boolean started = proxy.m_DataCollectionManager.startDataCollection(
											proxy.m_ConfigManager.getDataTypeConfigs());
					if (!started)
					{
						s_Logger.error("Failed to start the InputManagers");
					}
				}
				else
				{
					// Check if a CAV has run previously and completed
					// successfully. If so set up the plugins to support 
					// the last CAV
					if (proxy.m_DataCollectionManager.getCavStatus().getRunState() 
							== CavStatus.CavRunState.CAV_FINISHED)
					{
						s_Logger.info("Initialising the GUI for a previously run analysis.");
						
						proxy.m_DataCollectionManager.startForGUI(
								proxy.m_ConfigManager.getDataTypeConfigs());
					}
				}

				s_Logger.info("ProxyServer bound in registry");
			}
			catch (AlreadyBoundException abe)
			{
				s_Logger.fatal("Proxy objects are already bound in the Registry");
				s_Logger.fatal("Cannot start Proxy instance");
				
				// This is required as the RMI thread may keep the process alive.
				System.exit(1);
			}
			catch (Throwable t)
			{
				s_Logger.fatal("Proxy Fatal Error", t);

			    // This is required as the RMI thread may keep the process alive.
			    System.exit(1);
			}
		}

		return;
	}

}
