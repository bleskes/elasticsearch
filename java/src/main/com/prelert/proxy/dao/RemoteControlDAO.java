/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
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

package com.prelert.proxy.dao;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;

import com.prelert.data.CavStatus;
import com.prelert.data.ProcessStatus;
import com.prelert.proxy.data.DataTypeConfig;


/**
 * Interface defines RMI methods that are used to control the Proxy.
 */
public interface RemoteControlDAO extends java.rmi.Remote
{
	/**
	 * Graceful shutdown of the Proxy waits for all running threads to 
	 * terminate before closing the proxy.
	 * @return
	 * @throws RemoteException
	 */
	public boolean shutdown() throws RemoteException;
	
	/**
	 * Instantly closes the proxy forcing running threads to 
	 * terminate.
	 * @throws RemoteException
	 */
	public void kill() throws RemoteException;
	
	
	/**
	 * Runs the CAV by starting the Prelert processes 
	 * and the Inputmanagers.
	 *  
	 * @param timeOfIncident - Time of incident to be analysed.
	 * @param datatypes - List of datatype configs that will be used in the CAV.
	 * @return true if started the CAV successfully.
	 * @throws RemoteException
	 */
	public boolean startCav(Date timeOfIncident, List<DataTypeConfig> datatypes) 
	throws RemoteException;
	
	
	/**
	 * Stops the inputmanagers and the Prelert processes.
	 * 
	 * This is a blocking call that may take some time to 
	 * return.
	 * 
	 * @return
	 * @throws RemoteException
	 */
	public boolean stopCav() throws RemoteException;

	
	/**
	 * Pauses the InputManagers.
	 * @return true if successful
	 * @throws RemoteException
	 */
	public boolean pauseCav() throws RemoteException;
	
	/**
	 * Resumes the input managers after they have been paused.
	 * @return
	 * @throws RemoteException
	 */
	public boolean resumeCav() throws RemoteException;
			
	/**
	 * 
	 * @return
	 * @throws RemoteException
	 */
	public List<ProcessStatus> getProcessesStatus() throws RemoteException;
	
	
	/**
	 * Get the time of an incident around which CAV data will be collected.
	 * The result may be <code>null</code> if a time hasn't been set.
	 * 
	 * @return may be <code>null</code>
	 * @throws RemoteException
	 */
	public Date getCavTimeOfIncident() throws RemoteException;
	
	
	/**
	 * Returns true if the Proxy is collecting data in CAV mode.
	 * CAV mode is where the Proxy only pulls data between 
	 * 2 specific dates. 
	 * 
	 * @return
	 * @throws RemoteException
	 */
	public boolean isCavRunning() throws RemoteException;
	
	
	/**
	 * Returns true if the InputManagers have been running in 
	 * CAV mode and are now Finished.
	 * 
	 * @return
	 * @throws RemoteException
	 */
	public boolean isCavFinished() throws RemoteException;
	
	
	/**
	 * Returns a CavStatus object which details the CAV start/end
	 * dates, and progress.
	 * @return
	 * @throws RemoteException
	 */
	public CavStatus getCavStatus() throws RemoteException;
}
