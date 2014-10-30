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

package com.prelert.data;

import java.io.Serializable;

/**
 * Connection status enum with associated error message
 * if the connection failed. 
 */
public class ConnectionStatus implements Serializable
{
	private static final long serialVersionUID = 4234539167060223270L;

	public enum Status {CONNECTION_FAILED, CONNECTION_OK, MISSING_HEALTH_METRICS};
	
	private Status m_Status;
	private String m_ErrorMessage;
	
	
	public ConnectionStatus()
	{
	}
	
	public ConnectionStatus(Status status)
	{
		m_Status = status;
	}
	
	/**
	 * Get the connection status.
	 * @return
	 */
	public Status getStatus()
	{
		return m_Status;
	}
	
	/**
	 * If the connection status is not CONNECTION_OK then this 
	 * function will return the connection error message
	 * 
	 * @return <code>null</code> if the connection is ok
	 * 	else the error message.
	 */
	public String getErrorMessage()
	{
		return m_ErrorMessage;
	}
	
	/**
	 * Set the error message
	 * @param msg
	 */
	public void setErrorMessage(String msg)
	{
		m_ErrorMessage = msg;
	}


    @Override
    public String toString()
    {
    	StringBuilder strRep = new StringBuilder();
		
		strRep.append("{connectionStatus=");
		strRep.append(m_Status);
		
		if (m_ErrorMessage != null)
		{
			strRep.append(", errorMessage=");
			strRep.append(m_ErrorMessage);
		}
		
		strRep.append('}');
		
		return strRep.toString();
    }
}
