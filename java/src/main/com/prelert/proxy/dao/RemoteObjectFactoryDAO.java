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

package com.prelert.proxy.dao;

import java.rmi.RemoteException;

import com.prelert.proxy.dao.configuration.RemoteConfigurationDAO;

/**
 * Interface for a Remote Factory which will return a Remote Server for
 * the parameter <code>originator</code> where <code>originator</code>
 * is optionally used to distinguish between multiple GUIs sharing the
 * same proxy.  Each GUI must have a unique <code>originator</code> name.
 * 
 * @author dkyle
 */
public interface RemoteObjectFactoryDAO extends java.rmi.Remote 
{
	public RemoteCausalityDAO getCausalityDAO(String originator) throws RemoteException;
	
	public RemoteDataSourceDAO getDataSourceDAO(String originator) throws RemoteException;
	
	public RemoteEvidenceDAO getEvidenceDAO(String originator) throws RemoteException;
	
	public RemoteIncidentDAO getIncidentDAO(String originator) throws RemoteException;
	
	public RemoteTimeSeriesDAO getTimeSeriesDAO(String originator) throws RemoteException;
	
	public RemoteUserDAO getUserDAO(String originator) throws RemoteException;
	
	public RemoteConfigurationDAO getConfigurationDAO(String originator) throws RemoteException;
}
