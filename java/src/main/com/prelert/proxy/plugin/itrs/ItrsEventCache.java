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

package com.prelert.proxy.plugin.itrs;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.itrsgroup.activemodel.list.ActiveListEvent;
import com.itrsgroup.activemodel.list.ActiveListItem;
import com.itrsgroup.activemodel.list.ActiveListListener;
import com.itrsgroup.activemodel.severity.Severity;

public class ItrsEventCache implements ActiveListListener
{
	
	private Map<String, Event> m_Events = new HashMap<String, Event>();

	@Override
	public void onActiveListEvent(ActiveListEvent event) 
	{
		for (ActiveListItem item : event.getList().getContent().getItems() )
		{
			String path = item.getPath() + ":" + item.getName();
			
			Event ev = m_Events.get(path);
			if (ev == null)
			{
				ev = new Event(new Date().getTime(), item.getSeverity());
				m_Events.put(path, ev);
				System.out.println(path);
			}
			
			if (item.getSeverity() != ev.m_Severity)
			{
				ev = new Event(new Date().getTime(), item.getSeverity());
				m_Events.put(path, ev);
				System.out.println("Severity changed: " + item.getSeverity());
			}
		}
	}

	
	private class Event
	{
		long m_Time;
		Severity m_Severity;
		
		Event(long time, Severity severity)
		{
			m_Time = time;
			m_Severity = severity;  
		}
	}
}
