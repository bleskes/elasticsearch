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

package com.prelert.proxy.data.introscope;

import java.io.Serializable;

import com.prelert.proxy.data.SourceConnectionConfig;


/**
 * Holds the connection parameters for Introscope. 
 * This class if fundamentally the same as its parent class except it 
 * has a default value for the port setting.
 */
public class IntroscopeConnectionConfig extends SourceConnectionConfig implements Serializable
{
	private static final long serialVersionUID = -2440640606324389171L;
	
	public static final int DEFAULT_CLW_PORT = 5001;

	
	public IntroscopeConnectionConfig()
	{
		setPort(DEFAULT_CLW_PORT);
	}
	
	public IntroscopeConnectionConfig(SourceConnectionConfig config)
	{
		super(config);
	}
	
	public IntroscopeConnectionConfig(String host, Integer port, String username, String password)
	{
		super(host, port, username, password);
	}
}
