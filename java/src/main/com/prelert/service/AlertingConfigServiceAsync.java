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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.prelert.data.Alerter;


/**
 * Defines the methods to be implemented by the asynchronous client interface
 * to the to the alerting configuration service.
 * @author Pete Harverson
 */
public interface AlertingConfigServiceAsync
{
	
	/**
	 * Returns the UI alerter configuration which is stored on the Prelert server.
	 * @param callback callback object to receive the Alerter from
	 * 	 the remote procedure call.
	 */
	public void getAlerter(AsyncCallback<Alerter> callback); 
	
	
	/**
	 * Sets the UI alerter configuration for saving on the Prelert server under
	 * $PRELERT_HOME/config.
	 * @param alerter the Alerter configuration to be set on the Prelert server.
	 * @param callback callback object to receive the status code response from
	 * 	 the remote procedure call.
	 */
	public void setAlerter(Alerter alerter, AsyncCallback<Integer> callback); 
}
