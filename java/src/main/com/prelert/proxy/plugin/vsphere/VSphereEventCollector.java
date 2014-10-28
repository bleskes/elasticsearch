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

package com.prelert.proxy.plugin.vsphere;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.prelert.data.Notification;
import com.vmware.vim25.ArrayOfEvent;
import com.vmware.vim25.Event;
import com.vmware.vim25.EventFilterSpec;
import com.vmware.vim25.InvalidCollectorVersion;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.ObjectUpdate;
import com.vmware.vim25.PropertyChange;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertyFilterUpdate;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.UpdateSet;
import com.vmware.vim25.WaitOptions;


/**
 * Monitors VCenter for Events.
 * 
 * The <code>waitForEvents()</code> method blocks so this class extends
 * <code>Thread</code> and updates events in an infinite loop. Call
 * <code>quit()</code> to stop the thread.
 */
public class VSphereEventCollector extends Thread 
{
	static Logger s_Logger = Logger.getLogger(VSphereEventCollector.class);
	
	volatile boolean m_Quit;
	
	// EventManager and EventHistoryCollector References
	private ManagedObjectReference m_EventManager;
	private ManagedObjectReference m_EventHistoryCollector;  
	private ManagedObjectReference m_PropertyCollector;
	private ManagedObjectReference m_PropertyFilter;
	private VSphereConnection m_Connection;
	
	private List<Event> m_Events;
	
	private Map<String, String> m_EntityIdToResourceName;
	

	public VSphereEventCollector(VSphereConnection connection) throws RemoteException
	{
		m_Connection = connection;
		
		initialiseServiceObjects();
		
		m_Quit = false;
		
		m_Events = new ArrayList<Event>();
		
		m_EntityIdToResourceName = new HashMap<String, String>();
		
		loadResourceToHostnameMap();
	}
	
	
	/**
	 * Create the service objects (PropertyCollector, EventManager and
	 * EventHistoryCollector).
	 * 
	 * @throws RemoteException
	 */
	private void initialiseServiceObjects() throws RemoteException
	{
		// Create a new PropertyCollector
		m_PropertyCollector = m_Connection.getService().createPropertyCollector(
								m_Connection.getServiceContent().getPropertyCollector());
		
		m_EventManager = m_Connection.getServiceContent().getEventManager();
		
		// Create the Event history collector.
		EventFilterSpec filterSpec = new EventFilterSpec();
		m_EventHistoryCollector = m_Connection.getService().createCollectorForEvents(
																	m_EventManager, 
																	filterSpec);
	}
	
	
	/**
	 * Queries vSphere for the list of Virtual machines then updates the 
	 * entity id to hostname map with the hostnames of the virtual machines,
	 * hosts and datacenters.
	 */
	private void loadResourceToHostnameMap()
	{
		VSphereInventory inventory = new VSphereInventory(m_Connection);
		
		List<ManagedObjectReference> refs = inventory.getVirtualMachines(
													m_Connection.getServiceContent().getRootFolder());
		m_EntityIdToResourceName.putAll(inventory.updateVMHostnames(refs));
		
		refs = inventory.getHostSystems(m_Connection.getServiceContent().getRootFolder());	
		
		m_EntityIdToResourceName.putAll(inventory.updateHostSystemNames(refs));

		refs = inventory.getDatacenters(m_Connection.getServiceContent().getRootFolder());	
		
		m_EntityIdToResourceName.putAll(inventory.updateDataCenterNames(refs));
	}
	
	/**
	 * The thread's run method. 
	 * Executes until the <code>quit()</code> is called.
	 */
	@Override
	public void run()
	{
		createPropertyFilter();		

		String version = "";

		// The first time waitForEvents is called it returns a list 
		// recent events. Ignore these and only collect new events
		// from now.
//		version = waitForEvents(version, 120);			
//		getEventsAndClearHistory();
		
		while (true)
		{
			if (m_Quit)
			{
				break;
			}
		
			version = waitForEvents(version, 120);			
		}
		
		
		destroyPropertyFilter();		
	}
	
	
	/**
	 * Creates a new Property Filter to be used exclusively by this thread.
	 */
	private void createPropertyFilter()
	{
		PropertyFilterSpec filterSpec = createEventFilterSpec();
		try 
		{
			m_PropertyFilter = m_Connection.getService().createFilter(m_PropertyCollector, 
										filterSpec, true);
		}
		catch (RuntimeFault e) 
		{		
			s_Logger.error("createFilter() RuntimeFault: " + e);
		} 
		catch (InvalidProperty e) 
		{
			s_Logger.error("createFilter() InvalidProperty: " + e);
		}
		catch (RemoteException e) 
		{
			s_Logger.error("createFilter() RemoteException: " + e);
		}		
	}
	
	private void destroyPropertyFilter()
	{
		try 
		{
			m_Connection.getService().destroyPropertyFilter(m_PropertyFilter);
		} 
		catch (RuntimeFault e) 
		{
			s_Logger.error("destroyPropertyFilter() RuntimeFault: " + e);
		}
		catch (RemoteException e) 
		{
			s_Logger.error("destroyPropertyFilter() RemoteException: " + e);
		}
	}
	
	/**
	 * Create the filter spec for the EventHistoryCollector.
	 * Reads the 'latestPage' property of the history collector.
	 * @return
	 */
	private PropertyFilterSpec createEventFilterSpec() 
	{
		// Set up a PropertySpec to use the latestPage attribute 
		// of the EventHistoryCollector
		PropertySpec propSpec = new PropertySpec();
		propSpec.setAll(Boolean.FALSE);
		propSpec.setType(m_EventHistoryCollector.getType());
		propSpec.setPathSet(new String[] { "latestPage" });

		// PropertySpecs are wrapped in a PropertySpec array
		PropertySpec[] propSpecAry = new PropertySpec[] { propSpec };

		// Set up an ObjectSpec with the above PropertySpec for the
		// EventHistoryCollector we just created
		// as the Root or Starting Object to get Attributes for.
		ObjectSpec objSpec = new ObjectSpec();
		objSpec.setObj(m_EventHistoryCollector);
		objSpec.setSkip(new Boolean(false));
		objSpec.setSelectSet(new SelectionSpec[] { });

		// ObjectSpecs are wrapped in an ObjectSpec array
		ObjectSpec[] objSpecAry = new ObjectSpec[] { objSpec };
		PropertyFilterSpec spec = new PropertyFilterSpec();
		spec.setPropSet(propSpecAry);
		spec.setObjectSet(objSpecAry);

		return spec;
	}

	/**
	 * This function blocks for a period of <code>timeOutSeconds</code> waiting 
	 * for any events to happen on the PropertyCollector <code>
	 * m_PropertyCollector</code>
	 * 
	 * @param versionToken An empty string the first time this function is
	 * 					   called. This function returns a token string which
	 *                     can be used as this parameter in subsequent calls
	 * @param timeOutSeconds Function will return after 
	 * 					     <code>timeOutSeconds</code> if no events are fired.
	 * @return The version token for the current set of events. If the function 
	 * 		   times out or errors the parameter <code>versionToken</code> is returned.
	 */
	public String waitForEvents(String versionToken, int timeOutSeconds)
	{
		WaitOptions waitOptions = new WaitOptions();
		waitOptions.setMaxWaitSeconds(timeOutSeconds);

		UpdateSet updateSet;
		try {
			updateSet = m_Connection.getService().waitForUpdatesEx(m_PropertyCollector, 
					versionToken, waitOptions);
		} 
		catch (InvalidCollectorVersion e) 
		{
			s_Logger.error("waitForUpdates() InvalidCollectorVersion(): " + e);
			return versionToken;
		}
		catch (RuntimeFault e) 
		{
			s_Logger.error("waitForUpdates() RuntimeFault(): " + e);
			return versionToken;
		}
		catch (RemoteException e) 
		{
			s_Logger.error("waitForUpdates() RemoteException(): " + e);
			return versionToken;
		}
		
		if (updateSet == null)
		{
			// no updates
			return versionToken;
		}
		
		
		PropertyFilterUpdate [] updates = updateSet.getFilterSet();

		for (PropertyFilterUpdate update : updates)
		{
			ObjectUpdate [] ous = update.getObjectSet();
			for (ObjectUpdate ou : ous)
			{
				for (PropertyChange change : ou.getChangeSet())
				{
					if (change.getVal() instanceof ArrayOfEvent)
					{
						ArrayOfEvent arrayEvents = (ArrayOfEvent)change.getVal();
						Event[] events =  arrayEvents.getEvent();

						synchronized (m_Events)
						{
							for (Event event : events) 
							{
								m_Events.add(event);
							}
						}
					}
					else if (change.getVal() instanceof Event)
					{
						Event event = (Event)change.getVal();
						synchronized (m_Events)
						{
							m_Events.add(event);
						}
					}
					else
					{
						s_Logger.error("Unknown Property change type: " + change.getVal() 
								+ " Op=" + change.getOp().getValue() 
								+ ", name = " + change.getName());
						s_Logger.error(VSphereDataUtils.propertyChangeToString(change));	
					}
					
					
				}
			}
		}


		if (updateSet.getTruncated() != null && updateSet.getTruncated())
		{
			// incomplete data get the rest.
		}

		return updateSet.getVersion();
	}

	/**
	 * Returns the latest page of events from the event history manager.
	 * 
	 * This method does not block it returns immediately. 
	 * @return list of recent events.
	 * @throws Exception
	 */
	public List<Event> getEvents() throws Exception
	{
		PropertyFilterSpec eventFilterSpec = createEventFilterSpec();
		RetrieveOptions retrieveOptions = new RetrieveOptions();
		
		// Get all Events returned from the EventHistoryCollector
		// This will result in a large number of events, depending on the
		// page size of the latestPage.
		
		RetrieveResult props = 
			m_Connection.getService().retrievePropertiesEx(m_PropertyCollector, 
					new PropertyFilterSpec[] { eventFilterSpec },
					retrieveOptions);

		// Add the events to the result list
		List<Event> result = new ArrayList<Event>();
		for (ObjectContent oc : props.getObjects()) 
		{
			if (oc.getPropSet(0).getVal() instanceof ArrayOfEvent)
			{
				ArrayOfEvent arrayEvents = (ArrayOfEvent)oc.getPropSet(0).getVal();
				Event[] events =  arrayEvents.getEvent();

				synchronized (m_Events)
				{
					for (Event event : events) 
					{
						result.add(event);
					}
				}
			}
			else if (oc.getPropSet(0).getVal() instanceof Event)
			{
				Event event = (Event)oc.getPropSet(0).getVal();
				result.add(event);
			}
		} 
		
		// get any other events
		while (props.getToken() != null)
		{
			props = m_Connection.getService().continueRetrievePropertiesEx(m_PropertyCollector, props.getToken());
			
			for (ObjectContent oc : props.getObjects()) 
			{
				ArrayOfEvent arrayEvents 
				= (ArrayOfEvent)oc.getPropSet(0).getVal();
				Event[] events =  arrayEvents.getEvent();

				for (Event event : events) 
				{
					result.add(event);
				}
			} 
		}
		
		return result;		
	}
	
	
	/**
	 * Sets the thread's quit flag to true and cancels the service waiting
	 * for events.
	 */
	public void quit()
	{
		m_Quit = true;
		
		try 
		{
			m_Connection.getService().cancelWaitForUpdates(m_PropertyCollector);
		}
		catch (RuntimeFault e) 
		{
			s_Logger.error("cancelWaitForUpdates() RuntimeFault: " + e);
		}
		catch (RemoteException e) 
		{
			s_Logger.error("cancelWaitForUpdates() RemoteException: " + e);
		}
	}
	
	
	/**
	 * Returns all the events since this function was last called and clears
	 * the local store of events. This function can be called multiple times 
	 * will only return new events each time it is called.
	 *    
	 * @return list of <code>Event</code>s that occurred since the last
	 * 		   time this function was called.
	 */
	public List<Event> getEventsAndClearHistory()
	{
		List<Event> result;
		
		synchronized (m_Events)
		{
			result = new ArrayList<Event>(m_Events);
			
			m_Events.clear();
		}
		
		return result;
	} 
	
	/**
	 * Returns all the events as Prelert Notifications since this 
	 * function was last called and clears the local store of events. 
	 * 
	 * This function can be called multiple times and will only return 
	 * new events each time it is called.
	 *    
	 * @return list of <code>Notifications</code>s that occurred since the last
	 * 		   time this function was called.
	 */
	public List<Notification> getNotificationsAndClearHistory()
	{
		List<Event> events;
		
		synchronized (m_Events)
		{
			events = new ArrayList<Event>(m_Events);
			
			m_Events.clear();
		}
		
		List<Notification> results = new ArrayList<Notification>();
		for (Event event : events)
		{
			results.add(VSphereDataUtils.eventToNotification(event, m_EntityIdToResourceName));
		}
		
		return results;
	} 
	
	
}
