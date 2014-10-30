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

package com.prelert.service;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.prelert.data.Alerter;


/**
 * Defines the methods for the interface to the alerting configuration service.
 * @author Pete Harverson
 */
@RemoteServiceRelativePath("services/alertingConfigService")
public interface AlertingConfigService extends RemoteService
{
	/** Status code indicating operation on alerting config service succeeded. */
	public static final int STATUS_SUCCESS = 0;
	
	/** Status code indicating operation on alerting config service failed, with cause unknown. */
	public static final int STATUS_FAILURE_UNKNOWN = 101;
	
	/** Status code indicating the alerters config file could not be created. */
	public static final int STATUS_CANNOT_CREATE_FILE = 102;
	
	
	/**
	 * Returns the UI alerter configuration which is stored on the Prelert server.
	 * @return the Alerter configuration.
	 */
	public Alerter getAlerter();
	
	
	/**
	 * Sets the UI alerter configuration for saving on the Prelert server under
	 * $PRELERT_HOME/config.
	 * @param alerter the Alerter configuration to be set on the Prelert server.
	 * @return a status code, where zero indicates the alerter was saved successfully,
	 * 		and non-zero indicates the operation failed.
	 */
	public int setAlerter(Alerter alerter);
}
