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

package com.prelert.proxy.plugin.introscope;

import java.util.Stack;

import org.apache.log4j.Logger;

import com.prelert.proxy.data.SourceConnectionConfig;
import com.prelert.proxy.data.introscope.IntroscopeConnectionConfig;

/**
 * Connection Pool class to manage Introsocope CLW connections.
 *
 * The class is thread safe so multiple threads can call the 
 * {@link #acquireConnection()}, 
 * {@link #releaseConnection(IntroscopeConnection)},
 * {@link #setConnectionConfig(SourceConnectionConfig)}, {@link #reset()} and
 * {@link #testConnectionConfig(SourceConnectionConfig)} methods concurrently.
 */
public class ClwConnectionPool 
{
	private static Logger s_Logger = Logger.getLogger(ClwConnectionPool.class);
	
	public static final int MIN_OPEN_CONNECTIONS = 4;
	public static final int MAX_OPEN_CONNECTIONS = 10;
	
	private int m_CurrentOpenConnectionCount;
	
	private Stack<IntroscopeConnection> m_Connections;
	
	private IntroscopeConnectionConfig m_ConnectConfig;
	
	private volatile boolean m_IsValidConfig;
	
	private int m_MinOpenConnectionCount;
	

	public ClwConnectionPool()
	{
		m_CurrentOpenConnectionCount = 0;
		
		m_Connections = new Stack<IntroscopeConnection>();
		
		m_ConnectConfig = new IntroscopeConnectionConfig(); 
		
		m_IsValidConfig = false;
		
		m_MinOpenConnectionCount = MIN_OPEN_CONNECTIONS;
	}
	
	
	/**
	 * Returns a Connection object.
	 * @return
	 * @throws Exception 
	 */
	synchronized public IntroscopeConnection acquireConnection() throws Exception
	{
		if (m_Connections.size() > 0)
		{
			return m_Connections.pop();
		}
		
		// Create a new connection.
		IntroscopeConnection newConnect = createNewConnection(m_ConnectConfig);
		
		m_CurrentOpenConnectionCount++;
		
		s_Logger.debug("New Connection. Current open count=" + m_CurrentOpenConnectionCount + 
						", number connections = " + m_Connections.size());
		
		return newConnect;
	}
	
	
	/**
	 * Releases the connection back into the pool if the connection is
	 * valid and if the maximum number of open connections has not been
	 * exceeded. Open connection objects created by {@link #acquireConnection()}
	 * should be released here.
	 * 
	 * If the connection is broken then it is not returned to the pool.
	 * 
	 * If the connection's connect parameters are different to the current 
	 * parameters in the pool then the parameters have changed so the 
	 * returned connection is out of date. The connection is closed then
	 * rejected.
	 */
	synchronized public void releaseConnection(IntroscopeConnection connection) 
	{
		s_Logger.debug("Release Connection. Current open count=" + m_CurrentOpenConnectionCount + 
				", number connections = " + m_Connections.size());
		
		if (connection == null)
		{
			return; 
		}
		
		// If not a valid connection then drop it from the pool.
		if (connection.isConnected() == false)
		{
			s_Logger.info("Broken Connection returned to pool- not accepted.");
			--m_CurrentOpenConnectionCount;
			return;
		}
		
		if (connection.getConnectionConfig().equals(m_ConnectConfig) == false)
		{
			// The connection setting have changed so discard this connection.
			s_Logger.info("Connection rejected as returned to pool with different connection settings");

			connection.logoff();
			return;
		}
		
		// if we have enough open connections close this one
		// and let it die.
		if (m_CurrentOpenConnectionCount > m_MinOpenConnectionCount)
		{
			s_Logger.debug("logging off connection.");
			connection.logoff();
			--m_CurrentOpenConnectionCount;
		}
		else
		{
			s_Logger.debug("stashing connection");
			m_Connections.push(connection);
		}
		
	}
	
	
	/**
	 * Creates a new connection object and connects to it.
	 * @return
	 * @throws Exception 
	 */
	private IntroscopeConnection createNewConnection(SourceConnectionConfig config) 
	throws Exception
	{
		// Create a new connecton.
		String version = CLWorkstationCmdFactory.getCLWjarFileVersion();
		IntroscopeConnection newConnect = 
						CLWorkstationCmdFactory.newWorkstationConnection(version);
			
		newConnect.connect(config);
		
		return newConnect;
	}
	
	
	/**
	 * Get the connection parameters. All new connections 
	 * are made with these parameters.
	 * 
	 * Returns a copy the config for thread safety.
	 * @return
	 */
	synchronized public IntroscopeConnectionConfig getConnectionConfig()
	{
		return new IntroscopeConnectionConfig(m_ConnectConfig);
	}
	
	
	/**
	 * Sets the new config if it is different to the current config.
	 * 
	 * @param config
	 * @return false only if the config is different and it's invalid.
	 */
	synchronized public boolean setConnectionConfig(SourceConnectionConfig config)
	{
		
		if (!m_ConnectConfig.equals(config))
		{
			if (config.getHost() == null || config.getUsername() == null)
			{
				s_Logger.error("Cannot use connection: " + config);
				return false;
			}

			m_ConnectConfig = new IntroscopeConnectionConfig(config);

			IntroscopeConnection testedConnection = testConnectionConfig(m_ConnectConfig);
			
			m_IsValidConfig = testedConnection != null;
			if (m_IsValidConfig)
			{
				// Delete any old connections and insert the test one.
				
				while (!m_Connections.empty())
				{
					IntroscopeConnection oldConfigConnection = m_Connections.pop();
					oldConfigConnection.logoff();
				}
				
				// and push the newly created connection
				m_Connections.push(testedConnection);
				
				// Update the open connection count.
				m_CurrentOpenConnectionCount = 1;
			}
		}
		
		return m_IsValidConfig;		
	}
	
	/**
	 * Returns a connection made with the config parameter or 
	 * <code>null</code> if the connection can't be made.
	 * 
	 * Any valid connection object returned here is not tracked
	 * by the pool so the <code>logoff()</code> method
	 * should always be called in a try.. finally statement after it
	 * has been used. <em>Never</em> return this object to the pool
	 * using {@linkplain #releaseConnection(IntroscopeConnection)} always 
	 * use the <code>logoff()</code> of the actual connection.
	 * 
	 * @param config - the connection parameters.
	 * @return a valid connection object or <code>null</code> if 
	 * 	       test failed.
	 */
	synchronized public IntroscopeConnection testConnectionConfig(SourceConnectionConfig config)
	{
		try
		{
			IntroscopeConnection connection = createNewConnection(config);
			
			return connection;
		}
		catch (Exception e)
		{
			s_Logger.error("Test Connection Failed with parameters: " + config);
			return null;
		}
	}
	

	/**
	 * Returns true the pool has valid connection parameters 
	 * to an Enterprise Manager.
	 * @return true if valid connection config.
	 */
	public boolean isConfigured()
	{
		return m_IsValidConfig;
	}

	
	/**
	 * Resets the connection configuration and drops any open connections
     */
	synchronized public void reset()
	{
		m_ConnectConfig = new IntroscopeConnectionConfig();
		
		while (!m_Connections.empty())
		{
			IntroscopeConnection connection = m_Connections.pop();
			connection.logoff();
		}
		
		m_IsValidConfig = false;
	}


	/**
	 * The minimum number of connections that will always be
	 * held open. 
	 * 
	 * @return
	 */
	public int getMinOpenConnectionCount()
	{
		return m_MinOpenConnectionCount;
	}
	
	public void setMinOpenConnectionCount(int value)
	{
		m_MinOpenConnectionCount = value;
	}
	
	
	/**
	 * Cannot clone a singleton.
	 */
	public Object clone() throws CloneNotSupportedException 
	{
		throw new CloneNotSupportedException();
	}
}
