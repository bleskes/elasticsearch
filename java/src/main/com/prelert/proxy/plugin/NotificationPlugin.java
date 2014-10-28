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

package com.prelert.proxy.plugin;

import java.util.Date;
import java.util.List;

import com.prelert.data.Notification;
import com.prelert.proxy.inputmanager.querymonitor.QueryTookTooLongException;

/**
 * Interface for the Notification functions which should be 
 * implemented by any plugins which handle Notifications
 */
public interface NotificationPlugin 
{
	/**
	 * Returns a list of all the <code>Notifications</code> available to this 
	 * plugin that occurred int the time frame <code>start - end</code>
	 * @param start
	 * @param end
	 * @return
	 * @throws QueryTookTooLongException if the query takes too long.
	 */
	public List<Notification> getNotifications(Date start, Date end) 
	throws QueryTookTooLongException;

}
