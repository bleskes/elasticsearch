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

package com.prelert.proxy.inputmanager;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.prelert.data.Attribute;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.Notification;
import com.prelert.proxy.inputmanager.dao.InputManagerDAO;
import com.prelert.proxy.inputmanager.querymonitor.QueryTookTooLongException;
import com.prelert.proxy.plugin.NotificationPlugin;
import com.prelert.proxy.plugin.Plugin;

public class InputManagerNotification extends InputManager
{
	NotificationPlugin m_NotificationPlugin;
	
	public InputManagerNotification(InputManagerDAO inputManagerDAO)
	{
		super(inputManagerDAO);
	}

	/**
	 * If <code>plugin</code> does not implement the <code>NotificationPlugin</code> 
	 * interface an UnsupportedOperationException is thrown.
	 * 
	 * @throws UnsupportedOperationException
	 */
	@Override 
	public void setPlugin(Plugin plugin)
	{
		super.setPlugin(plugin);
		
		if (plugin instanceof NotificationPlugin)
		{
			m_NotificationPlugin = (NotificationPlugin)plugin;
		}
		else
		{
			throw new UnsupportedOperationException("Plugin '" + plugin.getName() + 
					"' does not implement the NotificationPlugin interface. It cannot" +
					" be set on a Notification InputManager");
		}
	}

	@Override
	protected boolean collectAndSendData(Date startTime, Date endTime, boolean updateDisplayColumns)
	{
		int queryTookTooLongWaitTimeMs = 0;
		
		List<Notification> notifications;
		try
		{
			// Query for notifications
			s_Logger.debug("Requesting Notifications");
			
			notifications = m_NotificationPlugin.getNotifications(startTime, endTime);
			
		}
		catch (QueryTookTooLongException e)
		{
			// May be overloading the server so back of for.
			queryTookTooLongWaitTimeMs += e.getWaitMs();
			
			try 
			{
				Thread.sleep(queryTookTooLongWaitTimeMs);
			}
			catch (InterruptedException e1) 
			{
				s_Logger.info("Interrupted whilst sleeping after a QueryTookTooLongException");
			}
			
			notifications = e.getNotificationsPartialResults();
			s_Logger.warn(e);
		}
		
		
		// and send each one to the evidence gatherer

		s_Logger.debug("Sending Notifications");
		
		// The Notification.toXmlString() function may produce a very 
		// large string. In order to keep memory utilisation down remove each 
		// time series from the collection after it has been sent. This makes 
		// memory available for garbage collection. This will only work with an
		// iterator it cannot be done in a for-each loop.
		
		int numberOfNotifications = notifications.size();
		
		Iterator<Notification> itr = notifications.iterator();

		Date timerStart = new Date();
		boolean isFirst = true;
		while (itr.hasNext())
		{
			if (m_Quit)
			{
				break;
			}

			Notification notification = itr.next();
			
			if (updateDisplayColumns && isFirst)
			{
				List<Attribute> attributes = notification.getAttributes();
				populateGuiDisplayColumns(notification.getType(), 
										DataSourceCategory.NOTIFICATION,
										attributes);
			}

			transferMessage(isFirst, notification.toXmlString());
			isFirst = false;

			// Make available for garbage collection.
			itr.remove();
		}
		
		Date timerEnd = new Date();
		long duration = timerEnd.getTime() - timerStart.getTime();
		
		s_Logger.info(numberOfNotifications
				+ " Notifications transferred to the backend TCP client in "
				+ duration + "ms.");
		
		
		return numberOfNotifications > 0;
	}

}
