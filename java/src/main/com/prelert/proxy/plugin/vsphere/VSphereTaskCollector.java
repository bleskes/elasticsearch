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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.prelert.data.Notification;
import com.vmware.vim25.ArrayOfTaskInfo;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.ElementDescription;
import com.vmware.vim25.InvalidCollectorVersion;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.LocalizableMessage;
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
import com.vmware.vim25.TaskDescription;
import com.vmware.vim25.TaskFilterSpec;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.UpdateSet;
import com.vmware.vim25.WaitOptions;

/**
 * Monitors VCenter for Tasks.
 * 
 * The <code>waitForTasks()</code> method blocks so this class extends
 * <code>Thread</code> and updates tasks in an infinite loop. Call
 * <code>quit()</code> to stop the thread.
 * 
 *  VCenter Tasks are initiated by the user examples include backing up
 *  or cloning a VM.
 */
public class VSphereTaskCollector extends Thread
{
	static Logger s_Logger = Logger.getLogger(VSphereTaskCollector.class);
	
	volatile boolean m_Quit;
	
	// EventManager and EventHistoryCollector References
	private ManagedObjectReference m_TaskManager;
	private ManagedObjectReference m_TaskHistoryCollector;  
	private ManagedObjectReference m_PropertyCollector;
	private ManagedObjectReference m_PropertyFilter;
	private VSphereConnection m_Connection;
	
	private List<TaskInfo> m_Tasks;
	
	private Map<String, String> m_TaskDescriptionIdToLabel;
	
	private Map<String, String> m_EntityIdToResourceName;
	
	/**
	 * Create a new Task Collector with the supplied connection object.
	 * @param connection
	 * @throws RemoteException
	 */
	public VSphereTaskCollector(VSphereConnection connection) throws RemoteException
	{
		m_Connection = connection;
		
		initialiseServiceObjects();
		
		m_Quit = false;
		
		m_Tasks = new ArrayList<TaskInfo>();
		
		m_TaskDescriptionIdToLabel = new HashMap<String, String>();
		
		m_EntityIdToResourceName = new HashMap<String, String>();
		
		loadResourceToHostnameMap();
	}
		
	
	/**
	 * Create the service objects (PropertyCollector, TaskManager and
	 * TaskHistoryCollector).
	 * 
	 * @throws RemoteException
	 */
	private void initialiseServiceObjects() throws RemoteException
	{
		// Create a new PropertyCollector
		m_PropertyCollector = m_Connection.getService().createPropertyCollector(
								m_Connection.getServiceContent().getPropertyCollector());
		
		// task manager.
		m_TaskManager = m_Connection.getServiceContent().getTaskManager();
		
		// create the task collector
		TaskFilterSpec filterSpec = new TaskFilterSpec();
		m_TaskHistoryCollector = m_Connection.getService().createCollectorForTasks(
												m_TaskManager, filterSpec);
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
		
		loadTaskDescriptions();

		String version = "";
		
		// The first time waitForTasks is called it returns a list 
		// recent tasks. Ignore these and only collect new tasks
		// from now.
		version = waitForTasks(version, 120);			
		getTasksAndClearHistory();
		
		while (true)
		{
			if (m_Quit)
			{
				break;
			}
		
			version = waitForTasks(version, 120);			
		}
		
		destroyPropertyFilter();		
	}
	
	
	/**
	 * Create the filter spec for the TaskHistoryCollector with
	 * the <code>pathSet</code> property set to the default value of 'latestPage'.
	 * 
	 * @return
	 */
	private PropertyFilterSpec createTaskFilterSpec() 
	{
		return createTaskFilterSpec(new String[] {"latestPage"});
	}	
	
	/**
	 * Create the filter spec for the TaskHistoryCollector with
	 * the provided pathSet.
	 * 
	 * @param pathSet
	 * @return
	 */
	private PropertyFilterSpec createTaskFilterSpec(String[] pathSet) 
	{
		PropertySpec pSpec = new PropertySpec();
		pSpec.setAll(Boolean.FALSE); 
		pSpec.setType(m_TaskHistoryCollector.getType()); 
		pSpec.setPathSet(pathSet); 
		
		ObjectSpec oSpec = new ObjectSpec();
		oSpec.setObj(m_TaskHistoryCollector);
		oSpec.setSkip(Boolean.FALSE); 
		
		oSpec.setSelectSet(new SelectionSpec[] { });
		
		PropertyFilterSpec filterSpec = new PropertyFilterSpec();
		filterSpec.setPropSet(new PropertySpec[]{pSpec});
		filterSpec.setObjectSet(new ObjectSpec[]{oSpec}); 
		
		return filterSpec;
	}	
	
	
	/**
	 * Creates a new Property Filter to be used exclusively by this thread.
	 */
	private void createPropertyFilter()
	{
		PropertyFilterSpec filterSpec = createTaskFilterSpec();
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
	 * Load the TaskDescription objects. These map to the 
	 * <code>descriptionId</code> field of the TaskInfo object.
	 */
	private void loadTaskDescriptions()
	{
		ManagedObjectReference taskManager = m_Connection.getServiceContent().getTaskManager();
	
		try 
		{
			RetrieveResult props = VSphereObjectProperties.queryManagedObjectProperties(
												m_Connection, taskManager, 
												new String[]{"description"});
			
			for (ObjectContent oc : props.getObjects()) 
			{
				if (oc.getPropSet(0).getVal() instanceof TaskDescription)
				{
					TaskDescription taskDesc = (TaskDescription)oc.getPropSet(0).getVal();
					
					ElementDescription[] array = taskDesc.getMethodInfo(); 
					for (ElementDescription desc : array)
					{
						m_TaskDescriptionIdToLabel.put(desc.getKey(), desc.getLabel());
					}
				}
			}
		}
		catch (InvalidProperty e) 
		{
			s_Logger.error("loadTaskDescription(): " + e);
			return;
		} 
		catch (RuntimeFault e) 
		{
			s_Logger.error("loadTaskDescription(): " + e);
			return;
		} 
		catch (RemoteException e) 
		{
			s_Logger.error("loadTaskDescription(): " + e);
			return;
		}
	}
	
	
	/**
	 * This function blocks for a period of <code>timeOutSeconds</code> waiting 
	 * for any tasks to happen on the PropertyCollector <code>
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
	public String waitForTasks(String versionToken, int timeOutSeconds)
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
			s_Logger.error("waitForUpdatesEx() InvalidCollectorVersion: " + e);
			return versionToken;
		}
		catch (RuntimeFault e) 
		{
			s_Logger.error("waitForUpdatesEx() RuntimeFault: " + e);
			return versionToken;
		}
		catch (RemoteException e) 
		{
			s_Logger.error("waitForUpdatesEx() RemoteException: " + e);
			return versionToken;
		}
		
		if (updateSet == null)
		{
			return versionToken;
		}
		
		
		storeTasks(updateSet.getFilterSet());
		
		return updateSet.getVersion();
	}
	
	
	/**
	 * Looks up the TaskInfo object in Task history collectors 
	 * 'latestPage' property. If the Task is not in 'latestPage'
	 * <code>null</code> be returned.
	 * 
	 * The <code>taskPath</code> parameter will be contain a string 
	 * like 'latestPage[task-1]' which should be returned in a 
	 * Property Change Event.
	 * 
	 * This function returns <code>null</code> if the taskinfo object 
	 * cannot be found.
	 * 
	 * @param taskPath
	 * @return a <code>TaskInfo</code> object or <code>null</code> if error.
	 */
	private TaskInfo getTaskInfo(String taskPath)
	{
		try {
			RetrieveOptions retrieveOptions = new RetrieveOptions();
			retrieveOptions.setMaxObjects(1);
			
			PropertyFilterSpec filterSpec = createTaskFilterSpec(new String[] {taskPath});
			
			RetrieveResult queryResult = m_Connection.getService().retrievePropertiesEx(m_PropertyCollector, 
													new PropertyFilterSpec[] {filterSpec}, 
													retrieveOptions);
			
			for (ObjectContent oc : queryResult.getObjects()) 
			{
				DynamicProperty[] props = oc.getPropSet();
				for (DynamicProperty prop : props)
				{
					if (prop.getVal() instanceof TaskInfo)
					{
						return (TaskInfo)prop.getVal();
					}					
				}
			}
		} 
		catch (RuntimeFault e) 
		{
			s_Logger.error("getLataestTask():" + e);
			return null;
		}
		catch (RemoteException e) 
		{
			s_Logger.error("getLataestTask():" + e);
			return null;
		}
		
		return null;
	}

	/**
	 * Writes tasks to the local task store.
	 * @param updates
	 */
	private void storeTasks(PropertyFilterUpdate[] updates)
	{
		List<TaskInfo> newTasks = new ArrayList<TaskInfo>();
		
		for (PropertyFilterUpdate update : updates)
		{
			ObjectUpdate [] ous = update.getObjectSet();
			for (ObjectUpdate ou : ous)
			{
				for (PropertyChange change : ou.getChangeSet())
				{
					if (change.getVal() instanceof ArrayOfTaskInfo)
					{
						ArrayOfTaskInfo arrayTasks = (ArrayOfTaskInfo)change.getVal();
						TaskInfo[] tasks =  arrayTasks.getTaskInfo();
						
						newTasks.addAll(Arrays.asList(tasks));
					}
					else if (change.getVal() instanceof TaskInfo)
					{
						TaskInfo task = (TaskInfo)change.getVal();
						newTasks.add(task);
					}
					else if (change.getName().endsWith(".state"))
					{
						// State has changed from queued/running to error/success etc.
						
						String path = change.getName().substring(0, change.getName().lastIndexOf(".state"));
						TaskInfo task = getTaskInfo(path);
						if (task != null)
						{
							newTasks.add(task);
						}
					}
					
				}
			}
			
			updateMissingResourceNames(newTasks);
			
			synchronized(m_Tasks)
			{
				for (TaskInfo task: newTasks) 
				{
					updateTasksDescription(task);
					m_Tasks.add(task);
				}
			}

		}
	}
	
	
	/**
	 * If the entity which is the source of the task is not it the 
	 * EntityIdToResourceName then update the map.
	 * 
	 * @param tasks
	 */
	private void updateMissingResourceNames(List<TaskInfo> tasks)
	{
		VSphereInventory inventory = new VSphereInventory(m_Connection);
		
		List<ManagedObjectReference> unknownVMRefs = new ArrayList<ManagedObjectReference>();
		List<ManagedObjectReference> unknownHostSystemRefs = new ArrayList<ManagedObjectReference>();
		List<ManagedObjectReference> dataCenters = new ArrayList<ManagedObjectReference>();
		
		for (TaskInfo task : tasks)
		{
			ManagedObjectReference obj = task.getEntity();
		
			if (obj == null) 
			{
				continue;
			}
			
			if (m_EntityIdToResourceName.containsKey(obj.get_value()))
			{
				continue;
			}
			
			if (obj.getType().equals("VirtualMachine"))
			{
				unknownVMRefs.add(obj);
			}
			else if (obj.getType().equals("HostSystem"))
			{
				unknownHostSystemRefs.add(obj);
			}
			else if (obj.getType().equals("DataCenter"))
			{
				unknownHostSystemRefs.add(obj);
			}
		}
		
		m_EntityIdToResourceName.putAll(inventory.updateVMHostnames(unknownVMRefs));
		m_EntityIdToResourceName.putAll(inventory.updateHostSystemNames(unknownHostSystemRefs));
		m_EntityIdToResourceName.putAll(inventory.updateDataCenterNames(dataCenters));
	}
	
	/**
	 * Looks up the tasks description Id and uses sets the
	 * tasks description string from that.
	 * @param task
	 */
	private void updateTasksDescription(TaskInfo task)
	{
		if (m_TaskDescriptionIdToLabel.containsKey(task.getDescriptionId()))
		{
			if (task.getDescription() == null)
			{
				LocalizableMessage msg = new LocalizableMessage();
				msg.setMessage(m_TaskDescriptionIdToLabel.get(task.getDescriptionId()));
				task.setDescription(msg);
			}
		}
	}

	
	/**
	 * Returns all the tasks since this function was last called and clears
	 * the local store of tasks. This function can be called multiple times 
	 * will always return only new tasks each time it is called.
	 *    
	 * @return list of <code>TaskInfo</code>s that occurred since the last
	 * 		   time this function was called.
	 */
	public List<TaskInfo> getTasksAndClearHistory()
	{
		List<TaskInfo> result;
		
		synchronized (m_Tasks)
		{
			result = new ArrayList<TaskInfo>(m_Tasks);
			
			m_Tasks.clear();
		}
		
		return result;
	} 
	
	
	/**
	 * Returns all the Tasks as Prelert Notifications since this 
	 * function was last called and clears the local store of tasks. 
	 * 
	 * This function can be called multiple times and will only return 
	 * new tasks each time it is called.
	 *    
	 * @return list of <code>Notifications</code>s that occurred since the last
	 * 		   time this function was called.
	 */
	public List<Notification> getNotificationsAndClearHistory()
	{
		List<TaskInfo> tasks;
		
		synchronized (m_Tasks)
		{
			tasks = new ArrayList<TaskInfo>(m_Tasks);
			
			m_Tasks.clear();
		}
		
		List<Notification> results = new ArrayList<Notification>();
		
		for (TaskInfo task : tasks)
		{
			results.add(VSphereDataUtils.taskInfoToNotification(task, 
												m_EntityIdToResourceName));
		}
		
		return results;
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
	
}
