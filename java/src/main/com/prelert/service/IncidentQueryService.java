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

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.prelert.data.gxt.ActivityTreeModel;
import com.prelert.data.gxt.CausalityAggregateModel;
import com.prelert.data.gxt.DatePagingLoadResult;
import com.prelert.data.gxt.IncidentModel;
import com.prelert.data.gxt.ActivityPagingLoadConfig;


/**
 * Defines the methods for the interface to the Incident Query service.
 * @author Pete Harverson
 */
public interface IncidentQueryService extends RemoteService
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
	public List<IncidentModel> getIncidents(Date minTime, Date maxTime, int anomalyThreshold);
	
	
	
	/**
	 * Returns the incident containing the notification or time series feature
	 * with the specified ID.
	 * @param evidenceId evidence ID of the notification or time series feature.
	 * @return the incident containing the item of evidence, or <code>null</code> 
	 * if there is no activity containing the specified evidence.
	 */
	public IncidentModel getIncident(int evidenceId);
	
	
	/**
	 * Returns the first page of incident data matching the specified load config 
	 * (i.e. anomaly threshold, page size).
	 * @param config load config specifying the data to obtain.
	 * @return load result containing the first page of incidents.
	 */
	public DatePagingLoadResult<IncidentModel> getFirstPage(ActivityPagingLoadConfig config);
	
	
	/**
	 * Returns the last page of incident data matching the specified load config 
	 * (i.e. anomaly threshold, page size).
	 * @param config load config specifying the data to obtain.
	 * @return load result containing the last page of incidents.
	 */
	public DatePagingLoadResult<IncidentModel> getLastPage(ActivityPagingLoadConfig config);
	
	
	/**
	 * Returns the next page of incident data following the incident in the specified 
	 * load config (i.e. anomaly threshold, time, evidence id, page size).
	 * @param config load config specifying the data to obtain.
	 * @return load result containing the next page of incidents.
	 */
	public DatePagingLoadResult<IncidentModel> getNextPage(ActivityPagingLoadConfig config);
	
	
	/**
	 * Returns the previous page of incident data to the incident in the specified 
	 * load config (i.e. anomaly threshold, time, evidence id, page size).
	 * @param config load config specifying the data to obtain.
	 * @return load result containing the next page of incidents.
	 */
	public DatePagingLoadResult<IncidentModel> getPreviousPage(ActivityPagingLoadConfig config);
	
	
	/**
	 * Returns a page of incident data whose top row will match the date in the 
	 * supplied config (i.e. anomaly threshold, time, page size).
	 * @param config load config specifying the data to obtain.
	 * @return load result containing a page of incidents.
	 */
	public DatePagingLoadResult<IncidentModel> getAtTime(ActivityPagingLoadConfig config);
	
	
	/**
	 * Returns the date/time of the earliest incident in the database.
	 * @return date/time of earliest incident.
	 */
	public Date getEarliestTime();
	
	
	/**
	 * Returns the date/time of the latest incident in the database.
	 * @return date/time of latest incident.
	 */
	public Date getLatestTime();
	
	
	/**
	 * Returns the frequency for automatic refresh of the incidents time line, 
	 * in seconds.
	 * @return the time line automatic refresh frequency, in seconds.
	 */
	public int getTimelineAutoRefreshFrequency();
	
	
	/**
	 * Returns a list of the names of the attributes by which the causality data
	 * in an incident can be aggregated.
	 * @param evidenceId evidence id of a notification or time series feature in 
	 * 	the incident.
	 * @return list of the attribute names by which causality data in an incident
	 * 	can be aggregated.
	 */
	public List<String> getIncidentAttributeNames(int evidenceId);


	/**
	 * Returns the summary of an incident by aggregating its constituent  
	 * notification and time series causality data by the specified attribute.
	 * @param evidenceId evidence id of a notification or time series feature in 
	 * 	the incident.
	 * @param aggregateBy name of the attribute by which the causality data in the
	 * 	incident should be aggregated e.g. type, source or service.
	 * @param groupingAttributes  list of attributes that must be supplied
	 *  in order for multiple causes to be grouped when reporting the "top"
	 *  piece of evidence.
	 * @param maxResults maximum number of aggregated causality objects to return,
	 * 	or <code>-1</code> to return the complete list of data. If a limit is specified
	 * 	and the number of results exceeds this limit, the last item returned will
	 * 	represent the remaining items aggregated into an 'Others' object.
	 * @return a summary of the incident as a list of aggregated causality objects.
	 */
	public List<CausalityAggregateModel> getIncidentSummary(int evidenceId, 
			String aggregateBy, List<String> groupingAttributes, int maxResults);
	
	
	/**
	 * Returns a summary of an incident as a tree of attribute data, with the 
	 * levels in the tree built down to the first 'branch point' i.e. where a 
	 * given attribute has more than one value across the data in the activity.
	 * @param evidenceId evidence id of a notification or time series feature in 
	 * 	the incident.
	 * @param treeAttributeNames list of attributes to be analysed.
	 * @param metricPathOrder <code>true</code> to analyse the attributes in
	 * 	metric path order, <code>false</code> to analyze by attribute value count.
	 * @param analyzeBy optional name of the attribute to force to the 'front' of
	 * 	the analysis, or <code>null</code> to use the natural ordering based on count.
	 * @param maxLeafRows maximum number of leaf rows to return, or <code>-1</code> 
	 *  to return the complete list of data. If a limit is specified and the number 
	 *  of leaf rows exceeds this limit, the last leaf returned will
	 * 	represent the remaining items aggregated into an 'Others' object.
	 * @return root node of the analysis tree with child levels down to the first 
	 * 	branch point.
	 */
	public ActivityTreeModel getSummaryTree(
			int evidenceId, List<String> treeAttributeNames, 
			boolean metricPathOrder, String analyzeBy, int maxLeafRows);
	
	
	/**
	 * Returns tree data for an incident based on an analysis of the shared attribute
	 * values across the time series features and notifications that make up the activity.
	 * @param evidenceId evidence id of a notification or time series feature in 
	 * 	the incident.
	 * @param node node in the tree for which to return the child nodes, or <code>null</code>
	 * 	to return the root node.
	 * @param treeAttributeNames list of attributes to be analysed.
	 * @param metricPathOrder <code>true</code> to analyse the attributes in
	 * 	metric path order, <code>false</code> to analyze by attribute value count.
	 * @param analyzeBy optional name of the attribute to force to the 'front' of
	 * 	the analysis, or <code>null</code> to use the natural ordering based on count.
	 * @param maxLeafRows maximum number of leaf rows to return, or <code>-1</code> 
	 *  to return the complete list of data. If a limit is specified and the number 
	 *  of leaf rows exceeds this limit, the last leaf returned will
	 * 	represent the remaining items aggregated into an 'Others' object.
	 * @return list of child nodes for the supplied node.
	 */
	public List<ActivityTreeModel> getAnalysisTreeData(int evidenceId, ActivityTreeModel node,
    		List<String> treeAttributeNames, boolean metricPathOrder, 
    		String analyzeBy, int maxLeafRows);
}
