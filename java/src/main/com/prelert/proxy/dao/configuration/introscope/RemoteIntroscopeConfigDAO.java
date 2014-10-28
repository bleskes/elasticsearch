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

package com.prelert.proxy.dao.configuration.introscope;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;

import com.prelert.data.AnalysisDuration;
import com.prelert.data.ConnectionStatus;
import com.prelert.proxy.data.CavAvailableDateRange;
import com.prelert.proxy.data.SourceConnectionConfig;
import com.prelert.proxy.data.introscope.MetricGroup;


/**
 * RMI remote methods interface for the Introscope Configuration object.
 */
public interface RemoteIntroscopeConfigDAO extends Remote
{
	/**
	 * Returns the connection details for Introscope.
	 * @return
	 * @throws RemoteException
	 */
	public SourceConnectionConfig getConnectionConfig() throws RemoteException;
	
	
	/**
	 * Sets the connection parameters for Introscope plugins.
	 * @param config
	 * @throws RemoteException
	 */
	public boolean setConnectionConfig(SourceConnectionConfig config) throws RemoteException;
	
	
	/**
	 * Resets the configuration parameters. 
	 * 
	 * Removes the list of agents and metric queries, resets the
	 * connection settings used by the Introscope plugins and sets
	 * the UsualDataPointInterval back to the default.
	 * 
	 * @return
	 * @throws RemoteException
	 */
	public boolean resetConfiguration() throws RemoteException;
	
	/**
	 * Returns the start and end dates of the valid data collection period.
	 * 
	 * Data older than a certain time may not be available at the required 
	 * granularity or not available at all.
	 * @throws RemoteException
	 */
	public CavAvailableDateRange getValidDateRange() throws RemoteException;
	
	
	/**
	 * Validates the connection parameters returning CONNECTION_OK
	 * the connection is successful. 
	 * If a connection can be made but the Enterprise Manager's health
	 * metrics cannot be read then MISSING_HEALTH_METRICS is returned.
	 * 
	 * @param config
	 * @return
	 * @throws RemoteException
	 */
	public ConnectionStatus testConnection(SourceConnectionConfig config) throws RemoteException;
	
	/**
	 * Returns a list of all the agents available in Introscope filtered
	 * by agentRegex. 
	 * 
	 * @param agentRegex - ignored if <code>null</code> or empty.
	 * @param connectionParams - The enterprise manager connection details.
	 * @return
	 * @throws RemoteException
	 */
	public List<String> listAgentsOnEM(String agentRegex,
			SourceConnectionConfig connectionParam) throws RemoteException;
	

	/**
	 * Returns a list of all the Agents currently being monitored 
	 * by the Proxy. If not configured this list will be empty.
	 * 
	 * If the connectionParams show which EM the agents should have been
	 * selected on. If the connection params don't match then an empty 
	 * list is returned.
	 * 
	 * @param connectionParams - The connection for which the agents were selected.
	 * 
	 * @return
	 * @throws RemoteException
	 */
	public List<String> getAgents(SourceConnectionConfig connectionParams) throws RemoteException;
	

	/**
	 * For the list of agents calculate how long it will take to 
	 * collect all the data for the CAV period.
	 * 
	 * @param agents
	 * @param timeOfIncident
	 * @param connectionParams 
	 * @return - An object encapsulating the calculated period in ms and 
	 * 			the optimal query length.
	 * @throws RemoteException
	 */
	public AnalysisDuration estimateCompletionTime(List<String> agents, Date timeOfIncident,
			SourceConnectionConfig connectionParams) throws RemoteException;
	
	/**
	 * For the list of queries calculate how long it will take to 
	 * collect all the data for the CAV period.
	 * 
	 * @param queries - the queries to run.
	 * @param timeOfIncident
	 * @param connectionParams
	 * @return - An object encapsulating the calculated period in ms and 
	 * 			the optimal query length.
	 */
	public AnalysisDuration estimateCompletionTimeForQueries(List<MetricGroup> queries, Date timeOfIncident,
			SourceConnectionConfig connectionParams) throws RemoteException;
}
