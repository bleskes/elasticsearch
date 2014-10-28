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


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.itrsgroup.activemodel.list.ActiveList;
import com.itrsgroup.activemodel.list.ActiveListEvent;
import com.itrsgroup.activemodel.list.ActiveListItem;
import com.itrsgroup.activemodel.list.ActiveListListener;


/**
 * The class implements the ActiveListListener callback and stores 
 * the path of each list event item.
 * 
 * The {@link ItrsItemList#waitForResults()} method can be used
 * for a blocking wait on a call to list items method. To do this 
 * the functions need to be called in this order:
 * 
 * <code>
 *      itrsItemList.listItems(activeList);
 *		activeListModel.register(activeList);
 *		itrsItemList.waitForResults();
 * </code>
 * 
 * The actual path values depends on the path used in the query
 * for example '//managedEntity' will return paths to all the 
 * managed entities.
 */
public class ItrsItemList implements ActiveListListener
{
	private Set<String> m_ItemPaths = new HashSet<String>();
	
	private boolean m_HasPaths = false;
	volatile private boolean m_WaitingOnEvents = false;
	
	/**
	 * Workaround for a bug in the API that means the first time 
	 * the onActiveListEvent event is called the event parameter
	 * contains no data.
	 */
	volatile private boolean m_FirstTimeListEventCalled = true;
	
	private Object m_SyncObj = new Object();
	
	/**
	 * Callback interface - this method should not be called directly. 
	 * 
	 * Geneos does not return all the list items in one go, the first time
	 * this callback is called it will be with 1 item, 2 the second, etc.
	 * The enumeration of all the items isn't complete until the same number
	 * of items are returned twice
	 */
	@Override
	public void onActiveListEvent(ActiveListEvent event) 
	{		
		// There is a bug in the API that means the first time the
		// onActiveListEvent event is called the event parameter 
		// contains no data. Set this flag to ignore the first call. 
		if (m_FirstTimeListEventCalled)
		{
			m_FirstTimeListEventCalled = false;
			return;
		}
		
		synchronized(m_ItemPaths)
		{
			for (ActiveListItem item : event.getList().getContent().getItems())
			{
				m_ItemPaths.add(item.getPath());
				m_HasPaths = true;
			}

			// got this number of items last time so signal that we are done.
			synchronized(m_SyncObj)
			{
				// notify waiting threads
				m_WaitingOnEvents = false;
				m_SyncObj.notify();
			}
				
		}	
	}
	
	
	/**
	 * Adds this as a event listener to activeList and initialises 
	 * the synchronisation objects so {@link #waitForResults} can be 
	 * called for a blocking wait. This function does not return anything
	 * {@link #waitForResults} should be called after this then the results
	 * can be accessed.
	 * 
	 * @param activeList The list command should have its path already set.
	 */
	public void listItems(ActiveList activeList)
	{
		// There is a bug in the API that means the first time this event 
		// is called the event parameter has no content.
		// Ignore the first call and process the second time. 
		m_FirstTimeListEventCalled = true;
		
		synchronized(m_SyncObj)
		{
			m_WaitingOnEvents = true;
			activeList.addListener(this);
		}
	}
	
	
	/**
	 * Synchronisation function blocks until the {@link #onActiveListEvent(ActiveListEvent)}
	 * callback is complete. This function should only be called after {@link #listItems(ActiveList)}
	 * has been called.
	 */
	public void waitForResults()
	{
		synchronized(m_SyncObj)
		{
			while (m_WaitingOnEvents)
			{
				try
				{
					m_SyncObj.wait();
				}
				catch (InterruptedException ex) 
				{ 
					return; 
				} 
			}
		}
	}

	
	/**
	 * Returns true if a list event with list items in it
	 * has occurred.
	 * @return
	 */
	public boolean hasItemPaths()
	{
		return m_HasPaths;
	}
	
	
	/**
	 * Returns the list of all entity paths return by every ActiveListEvent. 
	 * @return
	 */
	public List<String> getItemPaths()
	{
		synchronized(m_ItemPaths)
		{
			return new ArrayList<String>(m_ItemPaths);
		}
	}

}
