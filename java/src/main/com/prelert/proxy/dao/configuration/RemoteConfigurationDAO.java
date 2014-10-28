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

package com.prelert.proxy.dao.configuration;

import java.rmi.RemoteException;
import java.rmi.Remote;
import java.util.Date;
import java.util.List;

import com.prelert.data.AnalysisDuration;
import com.prelert.data.ConnectionStatus;
import com.prelert.proxy.data.CavAvailableDateRange;
import com.prelert.proxy.data.DataTypeConfig;
import com.prelert.proxy.plugin.Plugin;

/**
 * Interface to the Configuration DAO. 
 * The server can be queried to get a list of plugins that need 
 * to be configured.
 */
public interface RemoteConfigurationDAO extends java.rmi.Remote
{
	/**
	 * Returns a list of the names of the configured DataType.
	 * @return
	 * @throws RemoteException
	 */
	public List<String> getConfiguredDataTypeNames() throws RemoteException;
	
	
	/**
	 * Get the configuration for the specified DataType.
	 * @param type
	 * @return
	 * @throws RemoteException
	 */
	public DataTypeConfig getConfiguredDataType(String type) throws RemoteException;
	
	
	/**
	 * Returns the configurations for all the DataType.
	 * @return
	 * @throws RemoteException
	 */
	public List<DataTypeConfig> getConfiguredDataTypes() throws RemoteException;
	
	
	/**
	 * Add a configuration for a new DataType. 
	 * @param config
	 * @return
	 * @throws RemoteException
	 */
	public boolean addConfiguredDataType(DataTypeConfig config) throws RemoteException;
	
	
	/**
	 * Remove the configuration for datatype. 
	 * 
	 * @param datatype Type to remove
	 * @return True if datatype is present and it is then deleted.
	 * @throws RemoteException
	 */
	public boolean removeConfiguredDataType(String datatype) throws RemoteException;
	
	
	/**
	 * Returns a list of the data type names of the template DataType.
	 * @return
	 * @throws RemoteException
	 */
	public List<String> getTemplateDataTypeNames() throws RemoteException;
	
	
	/**
	 * Get the configuration for the specified templated DataType.
	 * @param type
	 * @return
	 * @throws RemoteException
	 */
	public DataTypeConfig getTemplateDataType(String type) throws RemoteException;
	
	
	/**
	 * Returns the configurations for all the templated DataTypes.
	 * @return
	 * @throws RemoteException
	 */
	public List<DataTypeConfig> getTemplateDataTypes() throws RemoteException;
	
	
	/**
	 * Wipes and reloads the cached data type configurations.
	 * @return
	 * @throws RemoteException
	 */
	public boolean reloadDataTypes() throws RemoteException;
	
	
	/**
	 * Test the plugin's connection to its data source
	 * for the given DataTypeConfig. The connection might be via JDBC 
	 * driver or the Introscope CLW or any other source.
	 * 
	 * The <code>SourceConnectionConfig</code> and any required plugin
	 * properties should be set in <code>config</code>. 
	 * 
	 * If the connection test passes the returned ConnectionStatus object
	 * will have a status value of CONNECTION_OK
	 * 
	 * @param config
	 * @return A connection status object. 
	 * @throws RemoteException
	 */
	public ConnectionStatus testConnection(DataTypeConfig config) throws RemoteException;
	
	
	/**
	 * Estimate how long it will take to collect all the data 
	 * for analysis with this config and at this timeOfIncident.
	 * If there is a problem and the estimate cannot be run then
	 * the returned object will be in an error state.
	 * 
	 * @param config The configuration object.
	 * @param timeOfIncident Data will be collected for this time. 
	 * @return A <code>AnalysisDuration</code> object detailing the analysis 
	 * 	duration, the optimal query length and other details.
	 * @throws RemoteException
	 */
	public AnalysisDuration estimateCompletionTime(DataTypeConfig config, Date timeOfIncident) 
	throws RemoteException;
	
	
	/**
	 * Returns the start and end dates of the valid data collection period.
	 * i.e. this the time frame from which data can be pulled for analysis.
	 * 
	 * @throws RemoteException
	 */
	public CavAvailableDateRange getValidDateRange() throws RemoteException;
	
	
	/**
	 * Returns the configuration server remote object for the plugin class
	 * or <code>null</code>
	 * 
	 * The returned Remote object will have to be cast to the configuration
	 * object type. 
	 * 
	 * @param pluginClass
	 * @return
	 * @throws RemoteException
	 */
	public Remote getConfigServer(Class<? extends Plugin> pluginClass) throws RemoteException;

	/**
	 * Overloaded method, uses the plugins class name as a parameter.
	 * Expects the class name i.e. com.prelert.etc
	 * @return
	 * @throws RemoteException
	 */
	public Remote getConfigServer(String classname) throws RemoteException;
}
