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

package com.prelert.data.gxt;

import com.extjs.gxt.ui.client.data.BaseModelData;


/**
 * Extension of the GXT BaseModelData class for the generic properties of a
 * connection to a data type.
 * @author Pete Harverson
 */
public class DataTypeConnectionModel extends BaseModelData
{
    private static final long serialVersionUID = -7060397188466833263L;

    
    /**
     * Returns the host of the data source.
     * @return the host.
     */
	public String getHost()
	{
		return get("host");
	}

	
	/**
	 * Sets the host of the data source.
	 * @param host the host.
	 */
	public void setHost(String host) 
	{
		set("host", host);
	}


	/**
	 * Returns the port number for the host.
	 * @return the port number, or 0 if no port has been set.
	 */
	public int getPort() 
	{
		int port = get("port", new Integer(0));
    	return port;
	}

	
	/**
	 * Sets the port number for the host.
	 * @param port the port number.
	 */
	public void setPort(int port) 
	{
		set("port", port);
	}

	
	/**
	 * Returns the username for connection to the data source.
	 * @return the user name.
	 */
	public String getUsername() 
	{
		return get("username");
	}

	
	/**
	 * Sets the username for connection to the data source.
	 * @param username the user name.
	 */
	public void setUsername(String username) 
	{
		set("username", username);
	}

	
	/**
	 * Returns the password for the configured user. 
	 * @return the password for the configured user, or a blank, zero length 
	 * 	String if no password has been set.
	 */
	public String getPassword() 
	{
		return get("password", "");
	}

	
	/**
	 * Sets the password for the configured user. 
	 * @param password the password for the configured user.
	 */
	public void setPassword(String password) 
	{
		set("password", password);
	}
	
	
	/**
	 * Returns the flag which indicates whether the configuration represents a 
	 * valid connection to the data source.
	 * @return <code>true</code> if the connection configuration is valid, or
	 * <false> if the connection is invalid or has yet to be validated.
	 */
	public boolean isValid()
	{
		boolean isValid = get("valid", new Boolean(false));
    	return isValid;
	}
	
	
	/**
	 * Sets the flag which indicates whether the configuration represents a 
	 * valid connection to the data source.
	 * @param isValid <code>true</code> if the connection configuration is valid, or
	 * <false> if the connection is invalid or has yet to be validated.
	 */
	public void setValid(boolean isValid)
	{
		set("valid", isValid);
	}
	
	
	/**
	 * Returns a summary of the configuration data for the connection.
	 * @return <code>String</code> representation of the connection.
	 */
	public String toString()
	{
		return getProperties().toString();
	}
}
