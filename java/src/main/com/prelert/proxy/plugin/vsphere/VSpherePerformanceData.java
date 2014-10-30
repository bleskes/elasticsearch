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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.vmware.vim25.ArrayOfPerfCounterInfo;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.PerfCompositeMetric;
import com.vmware.vim25.PerfCounterInfo;
import com.vmware.vim25.PerfEntityMetric;
import com.vmware.vim25.PerfEntityMetricBase;
import com.vmware.vim25.PerfMetricId;
import com.vmware.vim25.PerfProviderSummary;
import com.vmware.vim25.PerfQuerySpec;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RuntimeFault;

/**
 * Class for collecting performance data from VCenter
 * 
 * Returned performance data references a performance counter. The
 * name/type/desciption of this counter has to be extracted and linked 
 * to the actual data before it is useful. See <code>getPerfCounterInfo()</code>
 * and <code>queryPerfData</code>
 */
public class VSpherePerformanceData 
{
	static Logger s_Logger = Logger.getLogger(VSpherePerformanceData.class);
	
	/**
	 * Class groups vSphere perf data with a map of <code>PerfCounterInfo</code>.
	 * Each PerfMetricSeries in the PerfEntityMetric has a <code>counterId</code>, 
	 * the supplied map <code>counterIdToPerfCounterInfo</code> relates the counterId
	 * to the counter details.  
	 */
	public class PerformanceData
	{
		private PerfEntityMetric m_EntityMetric;
		private Map<Integer, PerfCounterInfo> m_CounterIdToPerfCounterInfo;
				
		public PerformanceData(PerfEntityMetric entityMetric,
								Map<Integer, PerfCounterInfo> counterIdToPerfCounterInfo) 
		{
			m_EntityMetric = entityMetric;
			m_CounterIdToPerfCounterInfo = counterIdToPerfCounterInfo;
		}
		
		public PerfEntityMetric getPerfEntityMetric()
		{
			return m_EntityMetric;
		}
		
		public Map<Integer, PerfCounterInfo> getCounterInfoMap()
		{
			return this.m_CounterIdToPerfCounterInfo;
		}		
	}
	
	private VSphereConnection m_Connection;
	private VSphereInventory m_Inventory;
	private ManagedObjectReference m_PerformanceManager;
	
	private List<PerfProviderSummary> m_PerfProviderSummaries;
	private Map<Integer, PerfCounterInfo> m_CounterIdToPerfCounterInfo;

	private Map<String, String> m_EntityIdToHostName;
	private List<VSphereResourceSelection> m_ResourceSelectionFilters;
	
	private Map<String, PerfMetricId[]> m_EntityIdToPerfMetricIds;
	
	
	/**
	 * Public constructor queries vCenter for all defined performance 
	 * counters on creation.
	 * 
	 * @param connection
	 * @param selections Specify if performance data should only be 
	 *   	             collected for a set of VMs or Data Centers.
	 */
	public VSpherePerformanceData(VSphereConnection connection, 
									List<VSphereResourceSelection> selections)
	{
		m_Connection = connection;
		m_Inventory = new VSphereInventory(m_Connection);
		m_PerformanceManager = connection.getServiceContent().getPerfManager();
		
		m_CounterIdToPerfCounterInfo = new HashMap<Integer, PerfCounterInfo>();
		m_PerfProviderSummaries = new ArrayList<PerfProviderSummary>();
		
		m_ResourceSelectionFilters = selections;
		
		m_EntityIdToHostName = new HashMap<String, String>();
		
		m_EntityIdToPerfMetricIds = new HashMap<String, PerfMetricId[]>();
		
		loadResourceToHostnameMap();
		
		updatePerfCountersInfo();
		updatePerfProviderSummaries();
		updatePerfMetricsForEntities();
	}		
	
	
	/**
	 * Returns the performance data for all the Virtual Machines
	 * and Host Systems that have performance summaries.
	 * 
	 * Filters out any performance counters that do any sort of 
	 * aggregation of time data.
	 * 
	 * @param start 
	 * @param end
	 * @return The raw metric data and details of the performance counter objects.
	 */
	public List<PerformanceData> getPerformanceData(Date start, Date end)
	{		
 		List<PerformanceData> perfData = new ArrayList<PerformanceData>();

		PerfQuerySpec querySpec = createPerfQuerySpec(null, start, end);
		for (PerfProviderSummary summary : m_PerfProviderSummaries)
		{
			PerfMetricId[] metricIds = m_EntityIdToPerfMetricIds.get(summary.getEntity().get_value()); 
			if (metricIds == null)
			{
				metricIds = queryAvailablePerfMetrics(summary);
				m_EntityIdToPerfMetricIds.put(summary.getEntity().get_value(), metricIds);
			}
			
			List<PerfMetricId> metricIdsToQuery = new ArrayList<PerfMetricId>();
			for (PerfMetricId metricId : metricIds)
			{
				PerfCounterInfo info = m_CounterIdToPerfCounterInfo.get(metricId.getCounterId());
				
				// Metrics with an empty counter instance value are 
				// either aggregations or Memory types.
				if ("".equals(metricId.getInstance()))
				{
					if (info != null && !info.getGroupInfo().getKey().equals("mem"))
					{
						continue;
					}
				}
							
				if (info != null)
				{
					metricIdsToQuery.add(metricId);
				}
			}
			
			querySpec.setMetricId(metricIdsToQuery.toArray(new PerfMetricId[0]));
			
			querySpec.setEntity(summary.getEntity());
			querySpec.setIntervalId(summary.getRefreshRate());
			
			perfData.addAll(queryPerfData(querySpec));
		}
		
		updateMissingHostnames(perfData);
		updateMissingPerfCountersInfo(perfData);
		
		
		return perfData;
	}
	
	
	/**
	 * Queries vSphere for the list of Virtual machines then updates the 
	 * entity id to hostname map with the hostnames of the virtual machines. 
	 */
	private void loadResourceToHostnameMap()
	{
		List<ManagedObjectReference> dataCenters = m_Inventory.getDatacenters(
										m_Connection.getServiceContent().getRootFolder());
		m_EntityIdToHostName.putAll(m_Inventory.updateDataCenterNames(dataCenters));

		List<ManagedObjectReference> vms = m_Inventory.getVirtualMachines(
													m_Connection.getServiceContent().getRootFolder());
		m_EntityIdToHostName.putAll(m_Inventory.updateVMHostnames(vms));
		
		List<ManagedObjectReference> hosts = m_Inventory.getHostSystems(
										m_Connection.getServiceContent().getRootFolder());	
		
		m_EntityIdToHostName.putAll(m_Inventory.updateHostSystemNames(hosts));
	}
	
	
	/**
	 * If the <code>perfData</code>
	 * @param perfData
	 */
	private void updateMissingHostnames(List<PerformanceData> perfData)
	{
		// Get any missing VMs for the hostname map
		List<ManagedObjectReference> unknownVMRefs = new ArrayList<ManagedObjectReference>();
		List<ManagedObjectReference> unknownHostSystemRefs = new ArrayList<ManagedObjectReference>();
		
		for (PerformanceData pData : perfData)
		{					
			ManagedObjectReference obj = pData.getPerfEntityMetric().getEntity();
			if (m_EntityIdToHostName.containsKey(obj.get_value()) == false)
			{
				if (obj.getType().equals("VirtualMachine"))
				{
					unknownVMRefs.add(obj);
				}
				else if (obj.getType().equals("HostSystem"))
				{
					unknownHostSystemRefs.add(obj);
				}
			}
		}
		
        if (unknownVMRefs.size() > 0)
        {
        	s_Logger.info("Updating VM summary");
    		m_EntityIdToHostName.putAll(m_Inventory.updateVMHostnames(unknownVMRefs));   		
        }
        
        if (unknownHostSystemRefs.size() > 0)
        {
        	s_Logger.info("Updating Host System summary");
        	m_EntityIdToHostName.putAll(m_Inventory.updateHostSystemNames(unknownHostSystemRefs));   	
        }
	}
	
	/**
	 * If the parameter <code>perfData</code> contains data from performance
	 * counters which are not currently known this function makes a query to 
	 * get the performance counter info for those counters.
	 * 
	 * @param perfData
	 */
	private void updateMissingPerfCountersInfo(List<PerformanceData> perfData)
	{		
		boolean updatePerformceCounterInfo = false;
		
		for (PerformanceData perf : perfData)
		{
			// Update details for any unrecognised performance counters. 
			int counterId = perf.getPerfEntityMetric().getValue(0).getId().getCounterId();
			if (m_CounterIdToPerfCounterInfo.containsKey(counterId) ==  false)
			{
				updatePerformceCounterInfo = true;
				break;
			}
		}		
		
        if (updatePerformceCounterInfo)
        {
        	s_Logger.info("Updating PerformanceCounterInfo.");
        	updatePerfCountersInfo();
        }
	}
	
	
	/**
	 * For every virtual machine and every host system in vCenter 
	 * this function gets the performance provider summary objects 
	 * for the VM and Hosts then updates a local list.
	 * 
	 * PerfProviderSummaries contain details of the counters type and refresh
	 * rate etc.
	 */
	private void updatePerfProviderSummaries()
	{
		m_PerfProviderSummaries.clear();
		
		List<ManagedObjectReference> dataCenters = m_Inventory.getDatacenters(
							m_Connection.getServiceContent().getRootFolder());
		
		for (ManagedObjectReference dataCenter : dataCenters)
		{
			String dataCenterName = m_EntityIdToHostName.get(dataCenter.get_value());
			if (dataCenterName == null)
			{
				s_Logger.error("updatePerfProviderSummaries(): Unrecognised entity name=" + dataCenter.get_value());
				continue;
			}
			
			// If no filters have been set include everything.
			boolean includeThisDataCenter = m_ResourceSelectionFilters.size() == 0;
			
			for (VSphereResourceSelection selection : m_ResourceSelectionFilters)
			{
				if (dataCenterName.matches(selection.getDataCenterRegEx()))
				{
					includeThisDataCenter = true;
					break;
				}
			}
			
			if (!includeThisDataCenter)
			{
				continue;
			}
			
			List<ManagedObjectReference> perfProviders = m_Inventory.getVirtualMachines(dataCenter);
			for (ManagedObjectReference objRef : perfProviders)
			{
				String hostname = m_EntityIdToHostName.get(objRef.get_value());
				if (hostname == null)
				{
					s_Logger.error("updatePerfProviderSummaries(): Unrecognised entity name=" + objRef.get_value());
					continue;
				}
				
				boolean addSummary = m_ResourceSelectionFilters.size() == 0;
				
				for (VSphereResourceSelection selection : m_ResourceSelectionFilters)
				{
					if (hostname.matches(selection.getVmRegex()))
					{
						addSummary = true;
						break;
					}				
				}
				
				if (addSummary)
				{
					// include all- no filters to apply
					PerfProviderSummary summary = queryPerfProviderSummary(objRef);
					if (summary.isCurrentSupported())
					{
						m_PerfProviderSummaries.add(summary);
					}	
				}
			}


			perfProviders = m_Inventory.getHostSystems(dataCenter);
			for (ManagedObjectReference objRef : perfProviders)
			{
				String hostname = m_EntityIdToHostName.get(objRef.get_value());
				if (hostname == null)
				{
					s_Logger.error("updatePerfProviderSummaries(): Unrecognised entity name=" + objRef.get_value());
					continue;
				}
				
				boolean addSummary = m_ResourceSelectionFilters.size() == 0;
				

				for (VSphereResourceSelection selection : m_ResourceSelectionFilters)
				{
					if (hostname.matches(selection.getHostSystemRegex()))
					{
						addSummary = true;
					}
				}

				
				if (addSummary)
				{
					// include all- no filters to apply.
					PerfProviderSummary summary = queryPerfProviderSummary(objRef);
					if (summary.isCurrentSupported())
					{
						m_PerfProviderSummaries.add(summary);
					}
				}
			}
		}
	}
	
	
	/**
	 * For each of the Performance Providers in this function
	 * gets a list of metric Ids supported by that provider and
	 * caches them in a map.
	 */
	private void updatePerfMetricsForEntities()
	{
		for (PerfProviderSummary summary : m_PerfProviderSummaries)
		{
			PerfMetricId[] metricIds = queryAvailablePerfMetrics(summary);
			
			m_EntityIdToPerfMetricIds.put(summary.getEntity().get_value(), metricIds);
		}
	}
	
	
	/**
	 * Creates a query spec to query all performance counters owned 
	 * by the managed object <code>object</code>. 
	 * 
	 * @param object
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	private PerfQuerySpec createPerfQuerySpec(ManagedObjectReference object, 
												Date startDate, Date endDate)
	{
		PerfQuerySpec spec = new PerfQuerySpec();
		
		Calendar start = Calendar.getInstance();
		start.setTime(startDate);
		spec.setStartTime(start);
		
		Calendar end = Calendar.getInstance();
		end.setTime(endDate);		
		spec.setEndTime(end);
		
		spec.setIntervalId(20); 
		spec.setEntity(object);
				
		return spec;
	}
	

	/**
	 * Returns the list of Ids of performance metrics the managed
	 * object <code>ojb</code> supports.
	 * 
	 * The <code>counterId</code> of the <code>PerfmetricId</code> value
	 * maps to the key value of a <code>PerfCounterInfo</code> object.
	 * 
	 * @param summary Provider summary object
	 * @return list of performance metric ids.
	 */
	private PerfMetricId[] queryAvailablePerfMetrics(PerfProviderSummary summary)
	{
        try 
        {
			return m_Connection.getService().queryAvailablePerfMetric(
												m_PerformanceManager, 
												summary.getEntity(), 
												null, null, 
												summary.getRefreshRate());
		}
        catch (RuntimeFault e)
		{
        	s_Logger.error(e);
        	return new PerfMetricId[0];
		}
        catch (RemoteException e) 
        {
        	s_Logger.error(e);
        	return new PerfMetricId[0];
		} 
	}
	
	
	/**
	 * For the managed object <code>obj</code> this function returns
	 * its performance provider summary.
	 * 
	 * @param obj
	 * @return Performance provider summary for the <code>obj</code>
	 */
	private PerfProviderSummary queryPerfProviderSummary(ManagedObjectReference obj)
	{
        try 
        {
			return m_Connection.getService().queryPerfProviderSummary(
												m_PerformanceManager, obj);
		}
        catch (RuntimeFault e)
		{
        	s_Logger.error(e);
        	return new PerfProviderSummary();
		}
        catch (RemoteException e) 
        {
        	s_Logger.error(e);
        	return new PerfProviderSummary();
		}               
	}
	
	/**
	 * Run the performance data query.
	 * 
	 * If the query returns a metric with a perf counter id that
	 * is not contained in the local map of perfCounterInfo by counter
	 * id then <code>getPerfCounterInfo()</code> is called to refresh
	 * counter info.
	 * 
	 * @param querySpec
	 * @return
	 */
	private List<PerformanceData> queryPerfData(PerfQuerySpec querySpec)
	{
		List<PerformanceData> perfData = new ArrayList<PerformanceData>();
		
		PerfEntityMetricBase[] metrics;
        try 
        {
			metrics = m_Connection.getService().queryPerf(m_PerformanceManager, 
										new PerfQuerySpec[] {querySpec});
		}
        catch (RuntimeFault e)
		{
        	s_Logger.error(e);
        	return perfData;
		}
        catch (RemoteException e) 
        {
        	s_Logger.error(e);
        	return perfData;
		}        
        
        if (metrics == null)
        {
        	return perfData;
        }
                
        for (PerfEntityMetricBase metric : metrics)
        {
        	if (metric instanceof PerfEntityMetric)
        	{
        		PerfEntityMetric entityMetric = (PerfEntityMetric)metric;
        		        		       		
        		perfData.add(new PerformanceData(entityMetric, 
        								m_CounterIdToPerfCounterInfo)); 
        	}
        }
           
        return perfData;
	}
	
	
	/**
	 * Run the performance data query for composite data.
	 * This query should only be run on host systems and returns data
	 * for the host and all its Virtual machines.
	 * 
	 * If the query returns a metric with a perf counter id that
	 * is not contained in the local map of perfCounterInfo by counter
	 * id then <code>getPerfCounterInfo()</code> is called to refresh
	 * counter info.
	 * 
	 * @param querySpec
	 * @return
	 */
	@SuppressWarnings("unused")
	private List<PerformanceData> queryPerfDataComposite(PerfQuerySpec querySpec)
	{
		List<PerformanceData> perfData = new ArrayList<PerformanceData>();
		
		PerfCompositeMetric compositeMetric;
        try 
        {
        	compositeMetric = m_Connection.getService().queryPerfComposite(m_PerformanceManager, 
													querySpec);
		}
        catch (RuntimeFault e)
		{
        	s_Logger.error(e);
        	return perfData;
		}
        catch (RemoteException e) 
        {
        	s_Logger.error(e);
        	return perfData;
		}        
        
        if (compositeMetric == null)
        {
        	s_Logger.debug("No composite performance data");
        	return perfData;
        }
        

        // Host system data
        if (compositeMetric.getEntity() != null)
        {
        	if (compositeMetric.getEntity() instanceof PerfEntityMetric)
        	{
        		PerfEntityMetric entityMetric = (PerfEntityMetric)compositeMetric.getEntity();
        		
        		perfData.add(new PerformanceData(entityMetric, 
        									m_CounterIdToPerfCounterInfo)); 
        	}
        }
        
        // Child Virtual Machine's data        
        PerfEntityMetricBase[] childMetrics = compositeMetric.getChildEntity();
        if (childMetrics == null)
        {
        	return perfData;
        }

        for (PerfEntityMetricBase metric : childMetrics)
        {
        	if (metric instanceof PerfEntityMetric)
        	{
        		PerfEntityMetric entityMetric = (PerfEntityMetric)metric;
        		       		       		
        		perfData.add(new PerformanceData(entityMetric, 
        										m_CounterIdToPerfCounterInfo));
        	}
        }
             
        return perfData;
	}
	
	
	/**
	 * Query the performance manager details of all its performance counters.
	 * Counter info is added to a local map indexed by its counterId.
	 */
	private void updatePerfCountersInfo()
	{	
		try 
		{
			RetrieveResult props = VSphereObjectProperties.queryManagedObjectProperties(
											m_Connection,
											m_PerformanceManager,
											new String[] {"perfCounter"});

			for (ObjectContent oc : props.getObjects()) 
			{
				ArrayOfPerfCounterInfo array = (ArrayOfPerfCounterInfo)oc.getPropSet(0).getVal();
				for (PerfCounterInfo pci : array.getPerfCounterInfo())
				{
					m_CounterIdToPerfCounterInfo.put(pci.getKey(), pci);					
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
	}
	
	
	public Map<String, String> getEntityIdToHostnameMap()
	{
		return m_EntityIdToHostName;
	}
}
