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

package com.prelert.service;


/**
 * Constants class containing a number of messages which may be found in
 * responses to requests from the client, commonly used to indicate a particular
 * error condition.
 * @author Pete Harverson
 */
public final class ApplicationResponseMessages
{
	/**
	 * Message indicating that a GWT-RPC service has been called with an invalid 
	 * Http session which has timed out.
	 */
	public static final String SESSION_TIMEOUT = "Session timeout";
	
	
	/**
	 * Message indicating that the security framework has detected that the user
	 * must log back into the application, commonly caused by the session timing out.
	 */
	public static final String LOGIN_REQUIRED = "j_spring_security_check";
	
	
	private ApplicationResponseMessages()
	{
		
	}
}
