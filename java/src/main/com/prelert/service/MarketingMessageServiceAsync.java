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
import com.prelert.data.MarketingMessages;


/**
 * Defines the methods to be implemented by the asynchronous client interface to the
 * service which provides marketing messages for the download diagnostics product.
 * @author Pete Harverson
 */
public interface MarketingMessageServiceAsync
{
	/**
	 * Returns the marketing messages for display in the download UI.
	 * @param callback  callback object to receive the marketing messages from the
	 * 	remote procedure call.
	 */
	public void getMessages(AsyncCallback<MarketingMessages> callback);
}
