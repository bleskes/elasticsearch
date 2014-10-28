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

package com.prelert.dao.proxy;


/**
 * Runtime exception for errors occurring whilst attempting to access data 
 * through the Proxy. 
 * @author Pete Harverson
 */
public class ProxyDataAccessException extends RuntimeException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7915417254075183764L;


	/**
	 * Constructs a <tt>ProxyDataAccessException</tt> with the specified detail
	 * message.
	 * @param message the detail message.
	 */
	public ProxyDataAccessException(String message)
	{
		super(message);
	}
	
	
	/**
	 * Constructs a <tt>ProxyDataAccessException</tt> with the specified detail
	 * message and cause.
	 * @param message the detail message.
	 * @param cause the cause.
	 */
	public ProxyDataAccessException(String message, Throwable cause)
	{
		super(message, cause);
	}
	
}
