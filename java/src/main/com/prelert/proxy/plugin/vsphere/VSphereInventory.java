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

import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.HostConfigSummary;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.VirtualMachineGuestSummary;
import com.vmware.vim25.VirtualMachineSummary;

/**
 * Helper class for traversing the VCentre hierarchy. 
 */
public class VSphereInventory 
{
	static Logger s_Logger = Logger.getLogger(VSphereInventory.class);
	
	private VSphereConnection m_Connection;
	
	VSphereInventory(VSphereConnection connection)
	{
		m_Connection = connection;
	}
	
	/**
	 * Returns a list of all the Managed object references to Data Centers
	 * in the VCentre hierarchy beneath the <code>rootFolder</code> parameter.
	 * @param rootFolder The root folder from which the traversal will take place
	 * @return List of Managed ojbect references to Data Centers.
	 */
	public List<ManagedObjectReference> getDatacenters(ManagedObjectReference rootFolder)
	{
		PropertyFilterSpec filterSpec = getFilterSpecForDatacenters(rootFolder);		
		
		return queryForManagedObjects(filterSpec);
	}
	
	/**
	 * Returns a list of all the Managed object references to Host Systems
	 * in the VCentre hierarchy beneath the <code>rootFolder</code> parameter.
	 * @param rootFolder The root folder from which the traversal will take place
	 * @return List of Managed object references to Host Systems.
	 */
	public List<ManagedObjectReference> getHostSystems(ManagedObjectReference rootFolder)
	{
		PropertyFilterSpec filterSpec = getFilterSpecForHostSystems(rootFolder);

		return queryForManagedObjects(filterSpec);
	}
	
	/**
	 * Returns a list of all the Managed object references to Virtual machines
	 * in the VCentre hierarchy beneath the <code>rootFolder</code> parameter.
	 * @param rootFolder The root folder from which the traversal will take place
	 * @return List of Managed ojbect references to Virtual machines.
	 */
	public List<ManagedObjectReference> getVirtualMachines(ManagedObjectReference rootFolder)
	{
		PropertyFilterSpec filterSpec = getFilterSpecForVirtualMachines(rootFolder);

		List<ManagedObjectReference> results = new ArrayList<ManagedObjectReference>(); 
		
		List<DynamicProperty> props = queryForManagedObjectProperties(filterSpec);
		for (DynamicProperty prop : props)
		{
			ArrayOfManagedObjectReference managedObjs = (ArrayOfManagedObjectReference)prop.getVal();
			
			for (ManagedObjectReference objRef : managedObjs.getManagedObjectReference())
			{
				results.add(objRef);
			}
		}
		
		return results;
	}

	/**
	 * Returns the Summary data for the specified Virtual Machine.
	 * @param vm
	 * @return
	 */
	public List<VirtualMachineSummary> getVirtualMachineSummary(ManagedObjectReference vm)
	{
		PropertyFilterSpec filterSpec = getFilterSpecForVirtualMachineSummary(vm);

		List<VirtualMachineSummary> results = new ArrayList<VirtualMachineSummary>(); 
		
		List<DynamicProperty> props = queryForManagedObjectProperties(filterSpec);
		for (DynamicProperty prop : props)
		{
			if (prop.getVal() instanceof VirtualMachineSummary)
			{
				VirtualMachineSummary summary = (VirtualMachineSummary)prop.getVal();
				results.add(summary);
			}
		}
		
		return results;		
	}
	
	
	/**
	 * For each of the virtual machine managed objects in 
	 * <code>vms</code> query for the VM's hostname and 
	 * return it in a map.
	 *   
	 * @param vm list of managed references to virtual machines
	 * @return Map of managed object entity name to the VM's hostname.
	 */
	public Map<String, String> updateVMHostnames(List<ManagedObjectReference> vms)
	{
		Map<String, String> entityIdToHostName = new HashMap<String, String>();
		
		if (vms.size() == 0)
		{
			return entityIdToHostName;
		}
		
		try 
		{
			RetrieveResult queryResult = VSphereObjectProperties.queryManagedObjectProperties(
											m_Connection,
											vms,
											new String[] {"summary.guest"});

			for (ObjectContent oc : queryResult.getObjects()) 
			{
				DynamicProperty[] props = oc.getPropSet();
				for (DynamicProperty prop : props)
				{
					if (prop.getVal() instanceof VirtualMachineGuestSummary)
					{
						VirtualMachineGuestSummary summary = (VirtualMachineGuestSummary)prop.getVal();
						if (summary.getHostName() != null)
						{
							entityIdToHostName.put(oc.getObj().get_value(), summary.getHostName());
						}
					}
				}
			}
		}
		catch (InvalidProperty e) 
		{
			s_Logger.error("Invalid property: " + e);
		}
		catch (RuntimeFault e) 
		{
			s_Logger.error("Runtime Fault: " + e);
		}
		catch (RemoteException e) 
		{
			s_Logger.error("Remote exception: " + e);
		}
		
		return entityIdToHostName;
	}
	

	/**
	 * For each of the Host System managed objects in 
	 * <code>hosts</code> query for the host system's name
	 * and return it in a map.
	 *   
	 * @param hosts list of managed references to Host Systems
	 * @return Map of managed object entity name to the Host Systems hostname.
	 */
	public Map<String, String> updateHostSystemNames(List<ManagedObjectReference> hosts)
	{
		Map<String, String> entityIdToHostName = new HashMap<String, String>();
		
		if (hosts.size() == 0)
		{
			return entityIdToHostName;
		}
		
		try 
		{
			RetrieveResult queryResult = VSphereObjectProperties.queryManagedObjectProperties(
											m_Connection,
											hosts,
											new String [] {"summary.config"});

			for (ObjectContent oc : queryResult.getObjects()) 
			{
				DynamicProperty[] props = oc.getPropSet();
				for (DynamicProperty prop : props)
				{
					if (prop.getVal() instanceof HostConfigSummary)
					{
						HostConfigSummary summary = (HostConfigSummary)prop.getVal();
						entityIdToHostName.put(oc.getObj().get_value(), summary.getName());
					}
				}
			}
		}
		catch (InvalidProperty e) 
		{
			s_Logger.error("Invalid property: " + e);
		}
		catch (RuntimeFault e) 
		{
			s_Logger.error("Runtime Fault: " + e);
		}
		catch (RemoteException e) 
		{
			s_Logger.error("Remote exception: " + e);
		}
		
		return entityIdToHostName;
	}
	
	
	/**
	 * For each of the Data Center managed objects in 
	 * <code>centers</code> query for the host system's name
	 * and return it in a map.
	 *   
	 * @param centers list of managed references to Data Centers
	 * @return Map of managed object entity name to the Host Systems hostname.
	 */
	public Map<String, String> updateDataCenterNames(List<ManagedObjectReference> centers)
	{
		Map<String, String> entityIdToHostName = new HashMap<String, String>();
		
		if (centers.size() == 0)
		{
			return entityIdToHostName;
		}
		
		try 
		{
			RetrieveResult queryResult = VSphereObjectProperties.queryManagedObjectProperties(
											m_Connection,
											centers,
											new String [] {"name"});

			for (ObjectContent oc : queryResult.getObjects()) 
			{
				DynamicProperty[] props = oc.getPropSet();
				for (DynamicProperty prop : props)
				{
					if (prop.getVal() instanceof String)
					{
						String name = (String)prop.getVal();
						entityIdToHostName.put(oc.getObj().get_value(), name);
					}
				}
			}
		}
		catch (InvalidProperty e) 
		{
			s_Logger.error("Invalid property: " + e);
		}
		catch (RuntimeFault e) 
		{
			s_Logger.error("Runtime Fault: " + e);
		}
		catch (RemoteException e) 
		{
			s_Logger.error("Remote exception: " + e);
		}
		
		return entityIdToHostName;
	}
	
	
	/**
	 * Returns a <code>PropertyFilterSpec</code> that will find Data Centre
	 * managed object references.
	 * @param root The root folder from which the traversal will take place
	 * @return PropertyFilterSpec
	 */
	private PropertyFilterSpec getFilterSpecForDatacenters(ManagedObjectReference root)
	{
		ObjectSpec objSpec = new ObjectSpec();
		objSpec.setObj(root);
		objSpec.setSkip(false);
		objSpec.setSelectSet(new SelectionSpec[] {VSphereTraversalSpecs.FOLDER_TO_CHILD,
										VSphereTraversalSpecs.DATACENTRE_TO_HOSTFOLDER,
										VSphereTraversalSpecs.COMPUTERESOURCE_TO_HOST, 
										VSphereTraversalSpecs.HOSTSYSTEM_TO_VM 
										});
		
		PropertySpec propSpec = new PropertySpec();
		propSpec.setAll(true);
		propSpec.setPathSet(new String[0]);
		propSpec.setType("Datacenter");
		
		PropertyFilterSpec result = new PropertyFilterSpec();
		result.setPropSet(new PropertySpec[]{propSpec});
		result.setObjectSet(new ObjectSpec[]{objSpec});
		result.setReportMissingObjectsInResults(true);
		
		return result;
	}
	
	private PropertyFilterSpec getFilterSpecForHostSystems(ManagedObjectReference root)
	{
		ObjectSpec objSpec = new ObjectSpec();
		objSpec.setObj(root);
		objSpec.setSkip(false);
		objSpec.setSelectSet(new SelectionSpec[] {VSphereTraversalSpecs.FOLDER_TO_CHILD,
											VSphereTraversalSpecs.DATACENTRE_TO_HOSTFOLDER,
											VSphereTraversalSpecs.COMPUTERESOURCE_TO_HOST, 
											VSphereTraversalSpecs.HOSTSYSTEM_TO_VM 
											});

		
		PropertySpec propSpec = new PropertySpec();
		propSpec.setAll(true);
		propSpec.setPathSet(new String[0]);
		propSpec.setType("HostSystem");
		
		PropertyFilterSpec result = new PropertyFilterSpec();
		result.setPropSet(new PropertySpec[]{propSpec});
		result.setObjectSet(new ObjectSpec[]{objSpec});
		result.setReportMissingObjectsInResults(true);
		
		return result;
	}
	
	/**
	 * Returns a <code>PropertyFilterSpec</code> that will find all the
	 * Virtual Machine managed object references.
	 * @param root The root folder from which the traversal will take place
	 * @return PropertyFilterSpec
	 */
	private PropertyFilterSpec getFilterSpecForVirtualMachines(ManagedObjectReference root)
	{
		ObjectSpec objSpec = new ObjectSpec();
		objSpec.setObj(root);
		objSpec.setSkip(false);
		objSpec.setSelectSet(new SelectionSpec[] {VSphereTraversalSpecs.FOLDER_TO_CHILD,
											VSphereTraversalSpecs.DATACENTRE_TO_HOSTFOLDER,
											VSphereTraversalSpecs.COMPUTERESOURCE_TO_HOST, 
											VSphereTraversalSpecs.HOSTSYSTEM_TO_VM 
											});
		
		PropertySpec propSpec = new PropertySpec();
		propSpec.setAll(false);
		propSpec.setPathSet(new String[] {"vm"});
		propSpec.setType("HostSystem");
		
		PropertyFilterSpec result = new PropertyFilterSpec();
		result.setPropSet(new PropertySpec[]{propSpec});
		result.setObjectSet(new ObjectSpec[]{objSpec});
		result.setReportMissingObjectsInResults(true);
		return result;
	}
	
	
	/**
	 * Returns a <code>PropertyFilterSpec</code> that will find query the summary
	 * property of a Virtual Machine.
	 * 
	 * @param vm a ManagedObjectReference to a Virtual Machine
	 * @return
	 */
	private PropertyFilterSpec getFilterSpecForVirtualMachineSummary(ManagedObjectReference vm)
	{
		ObjectSpec objSpec = new ObjectSpec();
		objSpec.setObj(vm);
		objSpec.setSkip(false);
		objSpec.setSelectSet(new SelectionSpec[0]) ;
		
		PropertySpec propSpec = new PropertySpec();
		propSpec.setAll(false);
		propSpec.setPathSet(new String[] {"summary"});
		propSpec.setType("VirtualMachine");
		
		PropertyFilterSpec result = new PropertyFilterSpec();
		result.setPropSet(new PropertySpec[]{propSpec});
		result.setObjectSet(new ObjectSpec[]{objSpec});
		result.setReportMissingObjectsInResults(true);
		return result;
	}
	
	/**
	 * Query all the properties for the given PropertyFilterSpec
	 * @param filterSpec Properties to query for.
	 * @return List of Dynamic properties.
	 */
	private List<DynamicProperty> queryForManagedObjectProperties(PropertyFilterSpec filterSpec)
	{
		ObjectContent[] objectContents = runQuery(filterSpec);
		
		List<DynamicProperty> results = new ArrayList<DynamicProperty>();
		
		for (ObjectContent obj : objectContents)
		{
			for (DynamicProperty prop : obj.getPropSet())
			{
				results.add(prop);
			}
		}		
		
		return results;
	}
	
	/**
	 * Query all the managed object references for the given PropertyFilterSpec
	 * @param filterSpec Managed objects to query for.
	 * @return List of Managed objects.
	 */
	private List<ManagedObjectReference> queryForManagedObjects(PropertyFilterSpec filterSpec)
	{
		ObjectContent[] objectContents = runQuery(filterSpec);
		
		List<ManagedObjectReference> results = new ArrayList<ManagedObjectReference>();
		
		for (ObjectContent obj : objectContents)
		{
			results.add(obj.getObj());
		}		
		
		return results;
	}
	
	/**
	 * Query for the given <code>PropertyFilterSpec</code>
	 * @param filterSpec
	 * @return
	 */
	private ObjectContent[] runQuery(PropertyFilterSpec filterSpec)
	{
		ObjectContent[] objects;
		try 
		{
			 objects = m_Connection.getService().retrieveProperties(
									m_Connection.getServiceContent().getPropertyCollector(), 
									new PropertyFilterSpec[] {filterSpec});
		} catch (RuntimeFault e) 
		{
			s_Logger.error(e);
			return new ObjectContent[0];
		}
		catch (InvalidProperty e) 
		{
			s_Logger.error(e);
			return new ObjectContent[0];
		}
		catch (RemoteException e) 
		{
			s_Logger.error(e);
			return new ObjectContent[0];
		}
		
		if (objects == null)
		{
			return new ObjectContent[0];
		}
		
		
		return objects;
	}
}











