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

package com.prelert.proxy.pluginLocator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.proxy.data.ExternalTimeSeriesConfig;
import com.prelert.proxy.plugin.ExternalPlugin;
import com.prelert.proxy.plugin.ExternalPointsPlugin;
import com.prelert.proxy.plugin.Plugin;


/**
 * Plugin Locator class.
 * Talks to the database and to discover plugins and returns the desired
 * plugin for each request.
 *
 * RMI will create a separate thread for each GUI connected to the proxy, so
 * changes to the <code>m_PluginsByType</code> member of this class need to be
 * locked to prevent corruption.
 */
public class PluginLocator
{
	private static Logger s_Logger = Logger.getLogger(PluginLocator.class);
	
	/**
	 * The WildCard datatype. If a plugin handles this datatype then all
	 * external datatypes will be channelled through that plugin.
	 */
	private static final DataSourceType WILDCARD_DATATYPE = new DataSourceType("*", DataSourceCategory.TIME_SERIES);

	private HashMap<DataSourceType, ThreadLocalPlugin> m_ExternalPluginsByType;
	private HashMap<DataSourceType, ThreadLocalPlugin> m_ExternalPointsPluginsByType;
	private PluginLocatorDAO m_PluginLocatorDAO;
	
	private Set<String> m_TypesExternalInDatabase;


	/**
	 *  The <code>ThreadLocal<T></code> class returns a new clone 
	 *  of the given <code>Plugin</code> for each calling thread.
	 *  The method <code>initialValue()</code> returns a valid 
	 *  clone of the originating source <code>Plugin</code>.
	 *  <code>ThreadLocal<T></code> also handles GC when the thread dies.
	 */
	public class ThreadLocalPlugin extends ThreadLocal<Plugin>
	{
		private Plugin m_SourcePlugin;
		
		public ThreadLocalPlugin(Plugin sourcePlugin)
		{
			m_SourcePlugin = sourcePlugin;
		}
		
		/**
		 * For each thread the initial value is a clone of the source Plugin.
		 */
		@Override
		protected Plugin initialValue()
		{
			return m_SourcePlugin.duplicate();
		}
		
		public Plugin getSeedPlugin()
		{
			return m_SourcePlugin;
		}
	}


	public PluginLocator()
	{
		m_ExternalPluginsByType = new HashMap<DataSourceType, ThreadLocalPlugin>();
		m_ExternalPointsPluginsByType = new HashMap<DataSourceType, ThreadLocalPlugin>();
		
		m_TypesExternalInDatabase = new HashSet<String>();
	}


	/**
	 * Sets this objects DAO.
	 * @param dao 
	 */
	public void setPluginLocatorDAO(PluginLocatorDAO dao)
	{
		m_PluginLocatorDAO = dao;
	}


	/**
	 * Return true if the datatype is recorded as being an
	 * external type in the Prelert database and a external plugin
	 * is registered for the type.
	 *
	 * @param datatype can be either a Time Series or Evidence type.
	 * @return True if dataSourceType has an external source.
	 */
	public boolean isExternal(DataSourceType datatype)
	{
		boolean externalInDB = isExternalInDatabase(datatype);
		boolean externalPlugin = m_ExternalPluginsByType.containsKey(datatype) ||
										m_ExternalPluginsByType.containsKey(WILDCARD_DATATYPE);
		
		
		return externalInDB && externalPlugin;
	}

	
	/**
	 * Returns true if there is an external points plugin for 
	 * the <code>datatype</code> and <code>datatype</code> is
	 * an external type.
	 * 
	 * @param dataSourceType 
	 * @return 
	 */
	public boolean isExternalPoints(DataSourceType datatype)
	{
		boolean externalInDB = isExternalInDatabase(datatype);
		boolean externalPointsPlugin = m_ExternalPointsPluginsByType.containsKey(datatype) ||
												m_ExternalPointsPluginsByType.containsKey(WILDCARD_DATATYPE);
		
		if (externalInDB != externalPointsPlugin)
		{
			s_Logger.error(String.format("Inconsistent external type '%s'. " +
						"External In the database = %s, External in the pluginLocator = %s.",
						datatype,
						externalInDB, externalPointsPlugin));
		}
		
		return externalInDB && externalPointsPlugin;
	}
	
	
	/**
	 * Returns true if <code>datatype</code> is registered as
	 * external in the Prelert database. 
	 * 
	 * The function caches types that the database has said are
	 * External. If another external type is added this will be
	 * picked up by a call to the database function but if a type
	 * was formally external but is now internal this function 
	 * will still return true. This should never happen as an 
	 * external type should never be made internal. 
	 * 
	 * @param datatype
	 * @return
	 */
	private boolean isExternalInDatabase(DataSourceType datatype)
	{
		// If type is in local cache don't bother checking 
		// with the database again return true.
		boolean externalInDB = m_TypesExternalInDatabase.contains(datatype.getName());
		if (externalInDB == false)
		{
			// Ask the database if the type is external
			// and cache it if it is. 
			externalInDB = m_PluginLocatorDAO.isExternal(datatype.getName());
			if (externalInDB)
			{
				m_TypesExternalInDatabase.add(datatype.getName());
			}
		}
		
		return externalInDB;
	}
	

	/**
	 * Determines whether the data for <code>evidenceType</code> is stored
	 * in the internal Prelert database or externally and accessed through
	 * a plugin.  At present all evidence is internal, so this method always
	 * returns false.
	 * @param evidenceType Should be an Evidence type.
	 * @return false.
	 */
	public boolean isEvidenceTypeExternal(String evidenceType)
	{
		// At present we're not supporting external notifications, and time
		// series are always internal, so there's no point calling a database
		// procedure.
		return false;
	}


	/**
	 * Returns the <code>ExternalPlugin</code> for the given data type. 
	 * If no plugin has been registered for that datatype then 
	 * <code>null</code> is returned and an error logged.
	 * 
	 * The function uses ThreadLocal storage so that each calling thread 
	 * will receive a different new instance of the <code>ExternalPlugin</code>. 
	 * 
	 * @param dataType The data type to get a <code>ExternalPlugin</code> for
	 * @return The <code>ExternalPlugin</code> or <code>null</code> 
	 */
	public ExternalPlugin getExternalPluginForDataType(DataSourceType dataType) 
	{
		ThreadLocalPlugin  threadLocalPlugin = null;
		
		synchronized (m_ExternalPluginsByType)
		{
			threadLocalPlugin = m_ExternalPluginsByType.get(dataType);
		}

		if (threadLocalPlugin == null)
		{
			threadLocalPlugin = m_ExternalPluginsByType.get(WILDCARD_DATATYPE);
			
			if (threadLocalPlugin == null)
			{
				s_Logger.error("No ExternalPlugin has been registered for the datatype '" + dataType + "'.");
				return null;
			}
		}

		return (ExternalPlugin)threadLocalPlugin.get();
	}

	
	/**
	 * Returns the <code>ExternalPointsPlugin</code> for the given data type. 
	 * If no plugin has been registered for that datatype then 
	 * <code>null</code> is returned and an error logged.
	 * 
	 * The function uses ThreadLocal storage so that each calling thread 
	 * will receive a different new instance of the <code>ExternalPointsPlugin</code>. 
	 * 
	 * @param dataType The data type to get a <code>ExternalPointsPlugin</code> for
	 * @return The <code>ExternalPointsPlugin</code> or <code>null</code> 
	 */
	public ExternalPointsPlugin getExternalPointsPluginForDataType(DataSourceType dataType) 
	{
		ThreadLocalPlugin  threadLocalPlugin = null;
		
		synchronized (m_ExternalPointsPluginsByType)
		{
			threadLocalPlugin = m_ExternalPointsPluginsByType.get(dataType);
		}

		if (threadLocalPlugin == null)
		{
			threadLocalPlugin = m_ExternalPointsPluginsByType.get(WILDCARD_DATATYPE);
			
			if (threadLocalPlugin == null)
			{
				s_Logger.error("No ExternalPointsPlugin has been registered for the datatype '" + dataType + "'.");
				return null;
			}
		}

		return (ExternalPointsPlugin)threadLocalPlugin.get();
	}
	
	
	/**
	 * For the given Time Series Id which is an external Time Series return
	 * details of the Plugin used to access that Time Series's data.
	 * <code>timeSeriesId</code> must be < 0.
	 * @param timeSeriesId must be < 0
	 * @return Description of the time series and plugin used to access it.
	 */
	public ExternalTimeSeriesConfig getPluginDescriptionForTimeSeriesId(int timeSeriesId)
	{
		return m_PluginLocatorDAO.getPluginDescriptionForTimeSeriesId(timeSeriesId);
	}


	/**
	 * Register the given <code>Plugin</code> as the handler for the 
	 * <code>datatype</code> the plugin supports. A <code>Plugin</code> 
	 * can only handle a single datatype.
	 * 
	 * The Plugin is used as a source which is cloned for every thread that
	 * requests the plugin through the <code>getPluginForDataType(String)</code>
	 * function.
	 * 
	 * If a plugin has already been registered to handle a datatype then an 
	 * IllegalStateException is thrown.
	 * 
	 * @param plugin fully formed <code>Plugin</code> which should handle 
	 * 			at least one <code>datatype</code> as returned by the 
	 * 			function <code>plugin.getDataTypes()</code>.
	 * @return true if successful.
	 * @throws IllegalArgumentException If the plugin does not handle
	 * 		   a datatype.	
	 * @throws IllegalStateException If a plugin has already been registered 
	 * 	       to handle a datatype. 
	 */
	public boolean registerPlugin(Plugin plugin)
	{
		DataSourceType datatype = plugin.getDataSourceType();
		if (datatype == null)
		{
			throw new IllegalArgumentException("Plugin " + plugin.getName() +
					" does not support a datatype.\n" +
			"The plugin cannot be registered with the PluginLocator");
		}
		
		
		if (plugin instanceof ExternalPlugin)
		{
			// Add the Plugin's evidence types to the database.
			synchronized (m_ExternalPluginsByType)
			{
				if (!m_ExternalPluginsByType.containsKey(datatype))
				{
					m_PluginLocatorDAO.addEvidenceType(datatype.getName(), datatype.getDataCategory());

					ThreadLocalPlugin threadLocalPlugin = new ThreadLocalPlugin(plugin);

					m_ExternalPluginsByType.put(datatype, threadLocalPlugin);
				}
				else
				{
					throw new IllegalStateException(datatype.toString());
				}

			}
		}
		
		
		if (plugin instanceof ExternalPointsPlugin)
		{
			// Add the Plugin's evidence types to the database.
			synchronized (m_ExternalPointsPluginsByType)
			{
				if (!m_ExternalPointsPluginsByType.containsKey(datatype))
				{
					m_PluginLocatorDAO.addEvidenceType(datatype.getName(), datatype.getDataCategory());

					ThreadLocalPlugin threadLocalPlugin = new ThreadLocalPlugin(plugin);

					m_ExternalPointsPluginsByType.put(datatype, threadLocalPlugin);
				}
				else
				{
					throw new IllegalStateException(datatype.toString());
				}

			}
		}
		
		return true;
	}
	

	/**
	 * Get the plugins for all known external types.  Every time this
	 * method is called, check if any new types have become known.  This is
	 * done so that the proxy can dynamically react to new types of data
	 * without having to be restarted.
	 * 
	 * @return A unmodifiableCollection containing all known external plugins.
	 *         This collectin throws <code>UnsupportedOperationException</code>
	 *         on any attempt to modify it.
	 */
	public Collection<ThreadLocalPlugin> getPlugins()
	{
		Collection<ThreadLocalPlugin> result = null;

		synchronized (m_ExternalPluginsByType)
		{
			result = new ArrayList<ThreadLocalPlugin>(m_ExternalPluginsByType.values());
		}
		
		synchronized (m_ExternalPointsPluginsByType)
		{
			result.addAll(m_ExternalPointsPluginsByType.values());
		}
		

		return result;
	}

	
	/**
	 * Clears all the registered plugins (both External and
	 * External Points).
	 */
	public void clear()
	{
		s_Logger.debug("Clearing PluginLocator");
		
		synchronized (m_ExternalPluginsByType)
		{
			m_ExternalPluginsByType.clear();
		}
		
		synchronized (m_ExternalPointsPluginsByType)
		{
			m_ExternalPointsPluginsByType.clear();
		}
		
		synchronized (m_TypesExternalInDatabase)
		{
			m_TypesExternalInDatabase.clear();
		}
	}
}
