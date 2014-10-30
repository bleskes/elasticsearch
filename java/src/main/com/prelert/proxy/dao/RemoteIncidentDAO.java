/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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

package com.prelert.proxy.dao;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;

import com.prelert.data.CausalityAggregate;
import com.prelert.data.Incident;
import com.prelert.data.MetricTreeNode;

/**
 * Interface defining the remote methods that can be called via RMI.
 * This interface is a remote version of IncidentDAO and exactly matches 
 * that interface.
 */
public interface RemoteIncidentDAO extends java.rmi.Remote
{
	/**
	 * Returns a list of incidents for the specified time window and anomaly threshold.
	 * @param minTime minimum (earliest) date/time to include.
	 * @param maxTime maximum (latest) date/time to include.
	 * @param anomalyThreshold minimum anomaly score threshold of incidents to return,
	 * 	which should be a value between 1 and 100. A value of 1 will return all incidents,
	 * 	whilst a value of 100 will return only the most infrequent (most 'anomalous') incidents.
	 * @return a list of incidents matching the specified criteria.
	 */
	public List<Incident> getIncidents(Date minTime, Date maxTime, int anomalyThreshold) 
		throws RemoteException;
	
	
	/**
	 * Returns a list of incidents for the specified time window and anomaly threshold.
	 * This does not return all the incidents in the time range instead it filters them
	 * and tries to return a similar number as the date range (zoom level in the gui)
	 * changes. 
	 * @param minTime minimum (earliest) date/time to include.
	 * @param maxTime maximum (latest) date/time to include.
	 * @param anomalyThreshold minimum anomaly score threshold of incidents to return,
	 * 	which should be a value between 1 and 100. A value of 1 will return all incidents,
	 * 	whilst a value of 100 will return only the most infrequent (most 'anomalous') incidents.
	 * @return a list of incidents matching the specified criteria.
	 */
	public List<Incident> getIncidentsAdaptive(Date minTime, Date maxTime, int anomalyThreshold) 
		throws RemoteException;
	
	
	/**
	 * Get incidents and filter by the time ranges All the date parameters
	 * are optional, set to <code>null</code> if a filter parameter is not
	 * to be used.
	 * 
	 * @param minTime The earliest date of incidents by time field.
	 * @param minTimeIsOpen If true use > else use >=
	 * @param maxTime The latest date of incidents by time field.
	 * @param maxTimeIsOpen If true use < else use <=
	 * @param minFirstTime The earliest date of incidents by first_time field.
	 * @param minFirstTimeIsOpen If true use > else use >=
	 * @param maxFirstTime The latest date of incidents by first_time field.
	 * @param maxFirstTimeIsOpen If true use < else use <=
	 * @param minLastTime The earliest date of incidents by last_time field.
	 * @param minLastTimeIsOpen If true use > else use >=
	 * @param maxLastTime The latest date of incidents by last_time field.
	 * @param maxLastTimeIsOpen If true use < else use <=
	 * @param minUpdateTime The earliest date of incidents by update_time field.
	 * @param minUpdateTimeIsOpen If true use > else use >=
	 * @param maxUpdateTime The latest date of incidents by update_time field.
	 * @param maxUpdateTimeIsOpen If true use < else use <=
	 * @param anomalyThreshold minimum anomaly score threshold of incidents to return,
	 * 	which should be a value between 1 and 100. A value of 1 will return all incidents,
	 * 	whilst a value of 100 will return only the most infrequent (most 'anomalous') incidents.
	 * @param metricPath Used for an exact match on a metric path. Can be <code>null</code>
	 * @param likeMetricPath SQL like statement used to match metric paths. Can be <code>null</code>
	 * @param escapeChar SQL like statement escape character if the literals
	 * '%' or '_' are to be matched. Can be <code>null</code>	
	 * @return List of Incidents filtered by the time ranges. 
	 */
	public List<Incident> getIncidentsInTimeRange(Date minTime, boolean minTimeIsOpen,
			Date maxTime, boolean maxTimeIsOpen,
			Date minFirstTime, boolean minFirstTimeIsOpen,
			Date maxFirstTime, boolean maxFirstTimeIsOpen,
			Date minLastTime, boolean minLastTimeIsOpen,
			Date maxLastTime, boolean maxLastTimeIsOpen,
			Date minUpdateTime, boolean minUpdateTimeIsOpen,
			Date maxUpdateTime, boolean maxUpdateTimeIsOpen,
			int anomalyThreshold, String metricPath,
			String likeMetricPath, String escapeChar) 
		throws RemoteException;
	
	
	/**
	 * Returns the first page of incidents at or above the specified anomaly threshold.
	 * @param anomalyThreshold minimum anomaly score threshold of incidents to return,
	 * 	which should be a value between 1 and 100. A value of 1 will return all incidents,
	 * 	whilst a value of 100 will return only the most infrequent (most 'anomalous') incidents.
	 * @param pageSize maximum size of list to return.
	 * @return first page of incidents matching the specified criteria.
	 */
	public List<Incident> getFirstPage(int anomalyThreshold, int pageSize) 
		throws RemoteException;
	
	
	/**
	 * Returns the last page of incidents at or above the specified anomaly threshold.
	 * @param anomalyThreshold minimum anomaly score threshold of incidents to return,
	 * 	which should be a value between 1 and 100. A value of 1 will return all incidents,
	 * 	whilst a value of 100 will return only the most infrequent (most 'anomalous') incidents.
	 * @param pageSize maximum size of list to return.
	 * @return last page of incidents matching the specified criteria.
	 */
	public List<Incident> getLastPage(int anomalyThreshold, int pageSize) 
		throws RemoteException;
	
	
	/**
	 * Returns the next page of incident data following on from the incident 
	 * with the specified time and evidence id.
	 * @param bottomRowTime the time of the last incident in the current page.
	 * @param bottomRowEvidenceId the evidence id of the last incident in the current page.
	 * @param anomalyThreshold minimum anomaly score threshold of incidents to return,
	 * 	which should be a value between 1 and 100. A value of 1 will return all incidents,
	 * 	whilst a value of 100 will return only the most infrequent (most 'anomalous') incidents.
	 * @param pageSize maximum size of list to return.
	 * @return page of incidents matching the specified criteria.
	 */
	public List<Incident> getNextPage(Date bottomRowTime, int bottomRowEvidenceId, 
			int anomalyThreshold, int pageSize) throws RemoteException;
	
	
	/**
	 * Returns the previous page of incident data to the one with the specified 
	 * time and evidence id.
	 * @param topRowTime the time of the first incident in the current page.
	 * @param topRowEvidenceId the evidence id of the first incident in the current page.
	 * @param anomalyThreshold minimum anomaly score threshold of incidents to return,
	 * 	which should be a value between 1 and 100. A value of 1 will return all incidents,
	 * 	whilst a value of 100 will return only the most infrequent (most 'anomalous') incidents.
	 * @param pageSize maximum size of list to return.
	 * @return page of incidents matching the specified criteria.
	 */
	public List<Incident> getPreviousPage(Date topRowTime, int topRowEvidenceId, 
			int anomalyThreshold, int pageSize) throws RemoteException;
	
	
	/**
	 * Returns a page of incident data, whose top row will match the supplied time.
	 * @param time time to match on for the first row of incident data that will be returned.
	 * @param anomalyThreshold minimum anomaly score threshold of incidents to return,
	 * 	which should be a value between 1 and 100. A value of 1 will return all incidents,
	 * 	whilst a value of 100 will return only the most infrequent (most 'anomalous') incidents.
	 * @param pageSize maximum size of list to return.
	 * @return page of incidents matching the specified criteria.
	 */
	public List<Incident> getAtTime(Date time, int anomalyThreshold,
									int pageSize, boolean orderAscending)
		throws RemoteException;
	
	
	/**
	 * Returns the date/time of the earliest incident.
	 * @return date/time of earliest incident.
	 */
	public Date getEarliestTime() throws RemoteException;
	
	
	/**
	 * Returns the date/time of the latest incident.
	 * @return date/time of latest incident.
	 */
	public Date getLatestTime() throws RemoteException;


	/**
	 * Returns a list of the names and prefixes for the constituents of the
	 * longest metric path associated with the time series in a particular
	 * incident.  This method returns an empty list if the incident in question
	 * contains more than one type of data, because in that case it doesn't make
	 * sense to select names for a single metric path within it.
	 * @param evidenceId The evidence ID of a notification or time series
	 *                   feature in the incident.
	 * @return A list of partially populated <code>MetricTreeNode</code> objects
	 *         containing the name and prefix of each constituent of the metric
     *         path.
	 */
	public List<MetricTreeNode> getIncidentMetricPathNodes(int evidenceId) throws RemoteException;


	/**
	 * Returns a list of the names of the attributes by which the causality data
	 * in an incident can be aggregated.
	 * @param evidenceId evidence id of a notification or time series feature in 
	 * 	the incident.
	 * @return list of the attribute names by which causality data in an incident
	 * 	can be aggregated.
	 */
	public List<String> getIncidentAttributeNames(int evidenceId) throws RemoteException;


	/**
	 * Returns a list of the different values for the attribute with the given name
	 * across all the notifications and time series features from the incident containing 
	 * the specified item of evidence.
	 * @param evidenceId the id of an item of evidence from the incident of interest.
	 * @param attributeName name of the attribute for which to return the different values.
	 * @return a list of the distinct attribute values.
	 */
	public List<String> getIncidentAttributeValues(int evidenceId, String attributeName)
		throws RemoteException;


	/**
	 * Returns the summary of an incident by aggregating its constituent notification
	 * and time series causality data by the specified attribute.
	 * @param evidenceId evidence id of a notification or time series feature in
	 * 	the incident.
	 * @param aggregateBy name of the attribute by which the causality data in the
	 * 	incident should be aggregated e.g. type, source or service.
	 * @param groupingAttributes list of attributes that must be supplied
	 *  in order for multiple causes to be grouped when reporting the "top"
	 *  piece of evidence
	 * @return a summary of the incident as a list of aggregated causality objects.
	 */
	public List<CausalityAggregate> getIncidentSummary(int evidenceId,
													String aggregateBy,
													List<String> groupingAttributes) throws RemoteException;
	
	
	/**
	 * Returns the incident containing the notification or time series feature with
	 * the specified id.
	 * @return the incident, or <code>null</code> if there is no activity containing
	 * 	a notification or time series feature with the specified id.
	 */
	public Incident getIncidentForId(int evidenceId) throws RemoteException;

}
