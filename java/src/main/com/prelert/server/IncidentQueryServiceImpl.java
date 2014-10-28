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

package com.prelert.server;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import static com.prelert.data.PropertyNames.*;

import com.prelert.dao.CausalityDAO;
import com.prelert.dao.EvidenceDAO;
import com.prelert.dao.IncidentDAO;
import com.prelert.data.Attribute;
import com.prelert.data.CausalityAggregate;
import com.prelert.data.CausalityData;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.Evidence;
import com.prelert.data.Incident;
import com.prelert.data.TimeFrame;
import com.prelert.data.gxt.ActivityTreeModel;
import com.prelert.data.gxt.CausalityAggregateModel;
import com.prelert.data.gxt.CausalityDataModel;
import com.prelert.data.gxt.DatePagingLoadResult;
import com.prelert.data.gxt.IncidentModel;
import com.prelert.data.gxt.ActivityPagingLoadConfig;
import com.prelert.server.ActivityAnalysisTree;
import com.prelert.server.ActivityAnalysisTree.AnalysisTreeNode;
import com.prelert.service.IncidentQueryService;


/**
 * Server-side implementation of the service for retrieving incident data from the
 * Prelert database.
 * @author Pete Harverson
 */
@SuppressWarnings("serial")
public class IncidentQueryServiceImpl extends RemoteServiceServlet
	implements IncidentQueryService
{
	static Logger s_Logger = Logger.getLogger(IncidentQueryServiceImpl.class);

	private IncidentDAO		m_IncidentDAO;
	private CausalityDAO	m_CausalityDAO;
	private EvidenceDAO		m_EvidenceDAO;
	
	private int m_TimelineAutoRefreshFrequency = 30;
	
	
	/**
	 * Returns the incident data access object being used by the query service.
     * @return the data access object for incident data.
     */
    public IncidentDAO getIncidentDAO()
    {
    	return m_IncidentDAO;
    }


	/**
	 * Sets the incident data access object to be used by the query service.
     * @param incidentDAO the data access object for incident data.
     */
    public void setIncidentDAO(IncidentDAO incidentDAO)
    {
    	m_IncidentDAO = incidentDAO;
    }
    
    
    /**
	 * Sets the causality data access object to be used by the query service.
	 * @param causalityDAO the data access object for causality data.
	 */
	public void setCausalityDAO(CausalityDAO causalityDAO)
	{
		m_CausalityDAO = causalityDAO;
	}
	
	
	/**
	 * Returns the causality data access object being used by the query service.
	 * @return the data access object for causality data.
	 */
	public CausalityDAO getCausalityDAO()
	{
		return m_CausalityDAO;
	}
	
	
	/**
	 * Sets the EvidenceDAO to be used to obtain evidence data.
	 * @param evidenceDAO the data access object for evidence data.
	 */
	public void setEvidenceDAO(EvidenceDAO evidenceDAO)
	{
		m_EvidenceDAO = evidenceDAO;
	}
	
	
	/**
	 * Returns the EvidenceDAO being used to obtain evidence data.
	 * @return the data access object for evidence data.
	 */
	public EvidenceDAO getEvidenceDAO()
	{
		return m_EvidenceDAO;
	}
    
    
    @Override
    public int getTimelineAutoRefreshFrequency()
    {
    	return m_TimelineAutoRefreshFrequency;
    }
    
    
    /**
     * Sets the frequency for automatic refresh of the incidents time line, 
	 * in seconds.
     * @param frequency the time line automatic refresh frequency, in seconds.
     */
    public void setTimelineAutoRefreshFrequency(int frequency)
    {
    	s_Logger.debug("TimelineAutoRefreshFrequency set to " + frequency);
    	m_TimelineAutoRefreshFrequency = frequency;
    }


	@Override
	public List<IncidentModel> getIncidents(Date minTime, Date maxTime, int anomalyThreshold)
	{
		List<IncidentModel> modelList = new ArrayList<IncidentModel>();
		
		Date qryMinTime = minTime;
		Date qryMaxTime = maxTime;
		
		// If no max time is supplied, for example, on opening the incident view
		// for the first time, show a 24 hours' worth of data, with the last hour
		// being the latest usage in the DB.
		if (qryMaxTime == null)
		{
			qryMaxTime = m_IncidentDAO.getLatestTime();
			
			if (qryMaxTime == null)
			{
				qryMaxTime = new Date();
			}
			
			if (qryMaxTime != null)
			{				
				if (qryMinTime == null)
				{
					// If no min time is supplied, set it to be the first hour of
					// of the most recent 24 hours' worth of data.
					// e.g. latest time Tues 10:42 - set min time to Mon 11:00
					GregorianCalendar maxCalendar = new GregorianCalendar();
					maxCalendar.setTime(qryMaxTime);
					
					GregorianCalendar minCalendar = new GregorianCalendar();
					minCalendar.set(maxCalendar.get(Calendar.YEAR), 
							maxCalendar.get(Calendar.MONTH), maxCalendar.get(Calendar.DAY_OF_MONTH), 
							maxCalendar.get(Calendar.HOUR_OF_DAY), 0);
					minCalendar.add(Calendar.DAY_OF_MONTH, -1);
					minCalendar.add(Calendar.HOUR_OF_DAY, 1);
					qryMinTime = minCalendar.getTime();
				}
			}
			
		}
		
		if (qryMinTime != null && qryMaxTime != null)
		{
			List<Incident> incidents = m_IncidentDAO.getIncidentsAdaptive(qryMinTime, qryMaxTime, anomalyThreshold);
			
			// Sort by descending anomaly score.
			Collections.sort(incidents, new AnomalyScoreComparator());
			
			// Convert to an IncidentModel, with description for request locale.
			IncidentModel incidentModel;
			for (Incident incident : incidents)
			{				
				incidentModel = createIncidentModel(incident);
				modelList.add(incidentModel);
			}
		}
		
		return modelList;
	}
	

    @Override
    public IncidentModel getIncident(int evidenceId)
    {
    	Incident incident = m_IncidentDAO.getIncidentForId(evidenceId);
    	
    	IncidentModel incidentForId = null;
    	if (incident != null)
    	{
    		incidentForId = createIncidentModel(incident);
    	}
    	
	    return incidentForId;
    }


	@Override
    public DatePagingLoadResult<IncidentModel> getFirstPage(
            ActivityPagingLoadConfig config)
    {
    	List<Incident> incidents = m_IncidentDAO.getFirstPage(
    			config.getAnomalyThreshold(), config.getPageSize());
    	
    	return createDatePagingLoadResult(incidents, config, incidents, null);
    }


    @Override
    public DatePagingLoadResult<IncidentModel> getLastPage(
    		ActivityPagingLoadConfig config)
    {
    	List<Incident> incidents = m_IncidentDAO.getLastPage(
    			config.getAnomalyThreshold(), config.getPageSize());
    	
    	return createDatePagingLoadResult(incidents, config, null, incidents);
    }


    @Override
    public DatePagingLoadResult<IncidentModel> getNextPage(
            ActivityPagingLoadConfig config)
    {
    	List<Incident> incidents = m_IncidentDAO.getNextPage(
    			config.getTime(), config.getRowId(), 
    			config.getAnomalyThreshold(), config.getPageSize());
    	
    	return createDatePagingLoadResult(incidents, config, null, null);
    }


    @Override
    public DatePagingLoadResult<IncidentModel> getPreviousPage(
            ActivityPagingLoadConfig config)
    {
    	List<Incident> incidents = m_IncidentDAO.getPreviousPage(
    			config.getTime(), config.getRowId(), 
    			config.getAnomalyThreshold(), config.getPageSize());
    	
    	
    	// If empty, load the first page - the previous button is always enabled.
    	List<Incident> latest = null;
		if (incidents == null || incidents.size() == 0)
		{
			incidents = m_IncidentDAO.getFirstPage(
					config.getAnomalyThreshold(), config.getPageSize());
			latest = incidents;
		}
    	
		return createDatePagingLoadResult(incidents, config, latest, null);
    }


    @Override
    public DatePagingLoadResult<IncidentModel> getAtTime(
    		ActivityPagingLoadConfig config)
    {
    	// Check if there is a time in the loadConfig. 
    	Date time = config.getTime();
		if (time == null)
		{
			return getFirstPage(config);
		}
		
		List<Incident> incidents = m_IncidentDAO.getAtTime(time, 
				config.getAnomalyThreshold(), config.getPageSize(), false);
    	
    	return createDatePagingLoadResult(incidents, config, null, null);
    }


	@Override
	public Date getEarliestTime()
	{
		return m_IncidentDAO.getEarliestTime();
	}
	
	
	@Override
	public Date getLatestTime()
	{
		return m_IncidentDAO.getLatestTime();
	}
	
	
    @Override
    public List<String> getIncidentAttributeNames(int evidenceId)
    {
	    return m_IncidentDAO.getIncidentAttributeNames(evidenceId);
    }


	@Override
    public List<CausalityAggregateModel> getIncidentSummary(int evidenceId, 
    		String aggregateBy, List<String> groupingAttributes, int maxResults)
    {
    	List<CausalityAggregateModel> modelList = new ArrayList<CausalityAggregateModel>();
    	
    	// Remove 'metric' if present???
    	ArrayList<String> causalityAttributeNames = new ArrayList<String>();
    	if (groupingAttributes != null)
    	{
			causalityAttributeNames.addAll(groupingAttributes);
			causalityAttributeNames.remove(TYPE);
			causalityAttributeNames.remove(SOURCE);
			causalityAttributeNames.remove(METRIC);
    	}
    	
    	
    	List<CausalityAggregate> causalityData =
    		m_IncidentDAO.getIncidentSummary(evidenceId, aggregateBy, causalityAttributeNames);
    	s_Logger.debug("getIncidentSummary() returned " + causalityData.size());
    	
    	
    	// Get the causality data corresponding to the 'headline' item of evidence
    	// to make sure it is displayed as one of the 'top' items.
    	Evidence headline = m_EvidenceDAO.getEvidenceSingle(evidenceId);
    	s_Logger.debug("getIncidentSummary() headline evidence: " + headline);
    	String aggregateValue = (String)(headline.get(aggregateBy));
    	
		ArrayList<String> primaryFilterNamesNull = new ArrayList<String>();
		ArrayList<String> primaryFilterNamesNonNull = new ArrayList<String>();
		ArrayList<String> primaryFilterValues = new ArrayList<String>();
		
		if (aggregateValue != null)
		{
			primaryFilterNamesNonNull.add(aggregateBy);
			primaryFilterValues.add(aggregateValue);
		}
		else
		{
			primaryFilterNamesNull.add(aggregateBy);
		}
    	
    	List<CausalityData> headlineSet = m_CausalityDAO.getCausalityData(evidenceId, 
    			causalityAttributeNames, primaryFilterNamesNull, 
    			primaryFilterNamesNonNull, primaryFilterValues, null, null);
    	
    	ArrayList<CausalityDataModel> headlineModels = new ArrayList<CausalityDataModel>();
    	for (CausalityData data : headlineSet)
    	{
    		headlineModels.add(CausalityDataUtilities.getInstance().createCausalityDataModel(data));
    	}
    	CausalityDataModel headlineData = CausalityDataUtilities.getInstance().getHeadlineCausalityData(
    			headlineModels, headline, causalityAttributeNames, aggregateBy);
    	
    	s_Logger.debug("getIncidentSummary() causality data for headline: " + headlineData);
    	
    	// Convert to a list of GXT CausalityAggregateModel objects.
    	// NB. Proc will have ordered results by:
    	// 1. Total count
    	// 2. Max significance
    	// 3. Aggregate value
    	int numRows = causalityData.size();
    	if (maxResults > -1)
    	{
    		numRows = Math.min(causalityData.size(), maxResults);
    	}
    	CausalityAggregate aggregate;
    	CausalityAggregateModel aggregateModel;
    	for (int i = 0; i < numRows; i++)
    	{
    		if ( (i < (numRows-1)) || (numRows == causalityData.size()) )
    		{
    			aggregate = causalityData.get(i);
    		}
    		else
    		{
    			// Number of rows exceeds the maximum setting, so create an 'Others' row.
    			List<CausalityAggregate> others = 
    				causalityData.subList(i, causalityData.size());
    			aggregate = CausalityDataUtilities.getInstance().aggregateCausalityData(others);
    		}
    		
			aggregateModel = createCausalityAggregateModel(headlineData, aggregate, 
				aggregateBy, causalityAttributeNames);		
			modelList.add(aggregateModel);
    		
    	}
	
    	// Sort by aggregate value, with the 'Others' row at the end.
    	Collections.sort(modelList, new CausalityAggregateValueComparator());

	    return modelList;
    }
	

    @Override
    public ActivityTreeModel getSummaryTree(int evidenceId, 
    		List<String> treeAttributeNames, boolean metricPathOrder, 
    		String analyzeBy, int maxLeafRows)
    {
    	s_Logger.debug("getSummaryTree() analyze ID " + evidenceId + 
    			" by " + analyzeBy + " with attributes " + treeAttributeNames);
    	
    	List<ActivityTreeModel> prunedRoot = getAnalysisTreeData(evidenceId, null,
        		treeAttributeNames, metricPathOrder, analyzeBy, maxLeafRows);
    	
    	// Mark the leaf nodes as leaf nodes - 
    	// we don't want them to show as folders in the client.
    	ActivityTreeModel rootNode = null;
    	if (prunedRoot.size() > 0)
    	{
    		rootNode = prunedRoot.get(0);
    		
    		ActivityTreeModel treeModel = rootNode;
    		while (treeModel != null)
    		{
    			int childCount = treeModel.getChildCount();
    			if (treeModel.getChildCount() == 1 && treeModel.isLeaf() == false)
    			{
    				treeModel = (ActivityTreeModel)(treeModel.getChild(0));
    			}
    			else
    			{
    				for (int i = 0; i < childCount; i++)
    				{
    					((ActivityTreeModel)(treeModel.getChild(i))).setLeaf(true);
    				}
    				break;
    			}
    		}
    	}
    	
    	return rootNode;
    }
    
    
    @Override
    public List<ActivityTreeModel> getAnalysisTreeData(int evidenceId, ActivityTreeModel node,
    		List<String> treeAttributeNames, boolean metricPathOrder, 
    		String analyzeBy, int maxLeafRows)
    {
    	s_Logger.debug("getAnalysisTreeData() analyze ID " + evidenceId + 
    			" by " + analyzeBy + " for node " + node);
    	
    	if (evidenceId == 0)
    	{
    		return new ArrayList<ActivityTreeModel>();
    	}
    	
		// Build Analysis Tree for specified evidence ID.
    	ActivityAnalysisTree analysisTree = new ActivityAnalysisTree(treeAttributeNames);

    	// Strip out type, metric and source from attributes as they are 
    	// treated separately by the proc.	
		ArrayList<String> causalityAttributeNames = new ArrayList<String>();
		causalityAttributeNames.addAll(treeAttributeNames);
		causalityAttributeNames.remove(TYPE);
		causalityAttributeNames.remove(SOURCE);
		causalityAttributeNames.remove(METRIC);
		
		List<CausalityData> causalityDataList = m_CausalityDAO.getCausalityData(
				evidenceId, causalityAttributeNames, null, null, null, null, null);
		s_Logger.debug("Size of causality data: " + causalityDataList.size());
		for (CausalityData causalityData : causalityDataList)
		{
			analysisTree.addCausalityData(causalityData);
		}
		
		try
		{
			if (metricPathOrder == true)
			{
				analysisTree.buildFixedTree();
			}
			else
			{
				int groupByIndex = -1;
				if (analyzeBy != null)
				{
					groupByIndex = treeAttributeNames.indexOf(analyzeBy);
				}
				analysisTree.buildTree(groupByIndex);
			}
		}
		catch (Exception e)
		{
			s_Logger.error("Error building activity tree", e);
		}
		
		AnalysisTreeNode rootNode = analysisTree.getRoot();
	
		ArrayList<ActivityTreeModel> childData = new ArrayList<ActivityTreeModel>();
		if (node != null)
		{
			List<String> argPath = node.getPathAttributeValues();
			
			// Find the node in the tree matching the one supplied in the load config.
			@SuppressWarnings("unchecked")
	        Enumeration<AnalysisTreeNode> treeEnum = rootNode.breadthFirstEnumeration();
			
			AnalysisTreeNode treeNode;
			String attrName;
			String attrVal;
			String findNodeName = node.getAttributeName();
			String findNodeVal = node.getAttributeValue();
			while (treeEnum.hasMoreElements())
			{
				treeNode = treeEnum.nextElement();
				attrName = treeNode.getAttributeName();
				attrVal = treeNode.getAttributeValue();
				
				if ( (attrName == null && findNodeName == null) || 
						(attrName != null && findNodeName != null && attrName.equals(node.getAttributeName())) )
				{
					
					if ( ( (attrVal != null && findNodeVal != null && attrVal.equals(findNodeVal)) ||
							(attrVal.equals("") && findNodeVal == null) ) && 
						(argPath.equals(treeNode.getPathAttributeValues())))
					{						
						int numChildren = treeNode.getChildCount();
						HashMap<String, List<CausalityDataModel>> groups = null;
						if (numChildren > 1 || (numChildren == 1 && treeNode.getFirstChild().isLeaf()))
						{
							// Set the top item of causality data for the child nodes.
							String leafAttributeName = ((AnalysisTreeNode)(treeNode.getFirstChild())).getAttributeName();
							groups = CausalityDataUtilities.getInstance().getMapByAttributeValue(
										causalityDataList, leafAttributeName);
						}
						
						AnalysisTreeNode childNode;
						ActivityTreeModel childModel;
						for (int i = 0; i < numChildren; i++)
						{
							childNode = (AnalysisTreeNode)(treeNode.getChildAt(i));
							childModel = createActivityTreeModel(childNode, null, groups);
							childData.add(childModel);
						}
						
						break;
					}
				}
			}
		}
		else
		{
			// Return the tree down to the first level in the tree 
			// where there are multiple values for an attribute.
			AnalysisTreeNode prunedRoot = analysisTree.pruneToBranch(rootNode, true);
			
			String leafAttributeName = ((AnalysisTreeNode)(prunedRoot.getFirstLeaf())).getAttributeName();
			HashMap<String, List<CausalityDataModel>> groups =
				CausalityDataUtilities.getInstance().getMapByAttributeValue(
						causalityDataList, leafAttributeName);

			// Set the evidence ID in the causality data corresponding to the 
			// 'headline' item of evidence to make sure it is displayed as one 
			// of the 'top' items.
	    	Evidence headline = m_EvidenceDAO.getEvidenceSingle(evidenceId);
	    	
	    	Collection<List<CausalityDataModel>> causalityLists = groups.values();
	    	Iterator<List<CausalityDataModel>> listIter = causalityLists.iterator();
	    	CausalityDataModel headlineData = null;
	    	while (listIter.hasNext())
	    	{
	    		headlineData = CausalityDataUtilities.getInstance().getHeadlineCausalityData(
	    				listIter.next(), headline, causalityAttributeNames, leafAttributeName);
	    		if (headlineData != null)
	    		{
	    			s_Logger.debug("getAnalysisTreeData() causality data for headline: " + headlineData);
	    			break;
	    		}
	    	}
			
			ActivityTreeModel rootTreeModel = 
				createActivityTreeBranch(prunedRoot, headline, groups, maxLeafRows);
			childData.add(rootTreeModel);
		}
		
	    return childData;
    }
    
    
    /**
     * Creates a GXT <code>ActivityTreeModel</code> object, with children, 
     * from the supplied <code>AnalysisTreeNode</code>.
     * @param treeNode node from the analysis tree.
     * @param headline the 'headline' feature or notification whose activity 
     * 	data is being requested.
     * @param groups map of groups of causality data, hashed on the attribute
     * 	value of the leaf nodes in the tree.
     * @param maxChildren maximum number of child nodes, or <code>-1</code> 
	 *  to return the complete list of data. If a limit is specified and the number 
	 *  of children exceeds this limit, the last child added will represent the 
	 *  remaining items aggregated into an 'Others' object.
     * @return <code>ActivityTreeModel</code> with children.
     */
    public ActivityTreeModel createActivityTreeBranch(
    		AnalysisTreeNode treeNode, Evidence headline,  
    		HashMap<String, List<CausalityDataModel>> groups, int maxChildren)
	{	
    	ActivityTreeModel treeModel = createActivityTreeModel(treeNode, headline, groups);
    	
    	int childCount = treeNode.getChildCount();
    	if (childCount > 0)
    	{
	    	int numLeaves = childCount;
	    	if (maxChildren > -1)
	    	{
	    		numLeaves = Math.min(childCount, maxChildren);
	    	}

	    	AnalysisTreeNode childNode;
	    	ActivityTreeModel childTreeModel;
	    	for (int i = 0; i < numLeaves; i++)
	    	{
	    		childNode = (AnalysisTreeNode)(treeNode.getChildAt(i));
	    		
	    		if ( (i < (numLeaves-1)) || (numLeaves == childCount) )
	    		{
					childTreeModel = createActivityTreeBranch(
							childNode, headline, groups, maxChildren);
					
	    		}
	    		else
	    		{
	    			// Number of leaves exceeds the maximum setting, turn this
	    			// model into an aggregated 'Others' row with combined count.
	    			Locale requestLocale = getThreadLocalRequest().getLocale();
	        		ResourceBundle bundle = ResourceBundle.getBundle("prelert_messages", requestLocale);
	    			String otherPattern = bundle.getString("incident.summary.attributeOtherValues");
	    			childNode.setValue(MessageFormat.format(otherPattern, childNode.getAttributeName()));
	    			
	    			int count = childNode.getCount();
	    			AnalysisTreeNode otherNode;
	    			for (int j = i+1; j < childCount; j++)
	    			{
	    				otherNode = (AnalysisTreeNode)(treeNode.getChildAt(j));
	    				count += otherNode.getCount();
	    			}
	    			childNode.setCount(count);
	    			
	    			childTreeModel = createActivityTreeModel(childNode, headline, groups);
	    			childTreeModel.setAttributeValue(null);
	    			childTreeModel.setDisplayValue(MessageFormat.format(otherPattern, childNode.getAttributeName()));
	    		}
	    		
	    		treeModel.add(childTreeModel);
	    	}
    	}
    	
    	return treeModel;
	}
    
    
    /**
     * Creates a GXT <code>ActivityTreeModel</code> object from the supplied 
     * <code>AnalysisTreeNode</code>.
     * @param treeNode node from the analysis tree.
     * @param headline the 'headline' feature or notification whose activity 
     * 	data is being requested.
     * @param groups map of groups of causality data, hashed on the attribute
     * 	value of the leaf nodes in the tree.
     * @return <code>ActivityTreeModel</code>.
     */
    public ActivityTreeModel createActivityTreeModel(AnalysisTreeNode treeNode,
    		Evidence headline, HashMap<String, List<CausalityDataModel>> groups)
	{	
    	ActivityTreeModel treeModel = new ActivityTreeModel();
    	treeModel.setAttributeName(treeNode.getAttributeName());
    	treeModel.setAttributeValue(treeNode.getAttributeValue());
    	treeModel.setCount(treeNode.getCount());
    	treeModel.setLeaf(!treeNode.hasMoreLevels());
    	
    	String value = treeModel.getAttributeValue();

		// Set the 'top' item of causality data if groups data supplied.
		if (groups != null)
		{
    		List<CausalityDataModel> modelList = groups.get(value);
    		if (modelList != null)
    		{
    			Collections.sort(modelList, new TopCausalityDataComparator(headline));
    			treeModel.setTopCausalityData(modelList.get(0));
    		}
		}
    	
    	if (value.equals(""))
    	{
    		Locale requestLocale = getThreadLocalRequest().getLocale();
    		ResourceBundle bundle = ResourceBundle.getBundle("prelert_messages", requestLocale);
    		String absentPattern = bundle.getString("incident.summary.attributeAbsent");
    		treeModel.setAttributeValue(null);
    		treeModel.setDisplayValue(MessageFormat.format(absentPattern, treeNode.getAttributeName()));
    	}
    	
    	return treeModel;
	}


	/**
	 * Creates a GXT IncidentModel object from the supplied Incident data,
	 * building the description of the incident for the request locale.
	 * @param incident incident data
	 * @return GXT BaseModelData subclass.
	 */
	protected IncidentModel createIncidentModel(Incident incident)
	{
		IncidentModel model = new IncidentModel();
		
		try
		{
			// Build the description of the incident for the request locale,  
			// Uses default locale of the server if no Accept-Language header is supplied.
			Locale requestLocale = getThreadLocalRequest().getLocale();
			String localizedDesc =  ActivityMessageFormat.formatIncidentDescription(
					incident, requestLocale);
			model.setDescription(localizedDesc);
		}
		catch (Exception e)
		{
			// Use the non-localized description returned by the proc.
			s_Logger.error("createIncidentModel() - error building localized description: ", e);
			model.setDescription(incident.getDescription());
		}
		
		model.setTime(incident.getTime());
		model.setAnomalyScore(incident.getAnomalyScore());
		model.setEvidenceId(incident.getTopEvidenceId());
		
		return model;
	}
	
	
	/**
	 * Creates a GXT CausalityAggregateModel object from the supplied CausalityAggregate data.
	 * @param aggregate aggregated causality data.
	 * @param aggregateBy name of the attribute by which the causality data in the
	 * 	incident should be aggregated e.g. type, source or service.
	 * @param attributesToSet  list of attributes that need to be set in the "top"
	 *  piece of evidence.
	 * @return aggregated causality data model.
	 */
	protected CausalityAggregateModel createCausalityAggregateModel(
			CausalityDataModel headlineData, CausalityAggregate aggregate, 
			String aggregateBy, List<String> attributesToSet)
	{
		CausalityAggregateModel model = new CausalityAggregateModel();	
		
		// When aggregating by description, mark time series features as being
		// aggregated by 'category', so cause_list_notifications_xxx_page procs
		// are not passed the artificial 'Time series feature' description.
		if ( (aggregateBy != null) && aggregateBy.equals("description") && 
				aggregate.getNotificationCount() == 0)
		{
			model.setAggregateBy("category");
			model.setAggregateValue(DataSourceCategory.TIME_SERIES_FEATURE.toString());
		}
		else
		{
			model.setAggregateBy(aggregateBy);
			model.setAggregateValue(aggregate.getAggregateValue());
		}
		model.setAggregateValueNull(aggregate.isAggregateValueNull());
		model.setStartTime(aggregate.getStartTime());
		model.setEndTime(aggregate.getEndTime());
		model.setTopEvidenceId(aggregate.getTopEvidenceId());
		if ( (aggregateBy != null) && (aggregateBy.equals("type")) )
		{
			model.setDataSourceName(aggregate.getAggregateValue());
		}
		
		// Uses default locale of the server if no Accept-Language header is supplied.
		Locale requestLocale = getThreadLocalRequest().getLocale();
		model.setSummary(ActivityMessageFormat.formatCausalitySummary(
				aggregate, aggregateBy, requestLocale));
		
		model.setNotificationCount(aggregate.getNotificationCount());
		model.setFeatureCount(aggregate.getFeatureCount());
		model.setSourceCount(aggregate.getSourceCount());
		model.setTopSourceName(aggregate.getSourceNames().get(0));
		
		// Set the 'top' item of causality data.
		// Use the headline evidence if it matches the aggregate value.
		boolean useHeadlineAsTopData = false;
		if (model.getAggregateBy() != null)
		{
			String headlineAggrVal = headlineData.get(model.getAggregateBy());
			String aggregateVal = model.getAggregateValue();
			if ( (headlineAggrVal != null && headlineAggrVal.equals(aggregateVal)) ||
					(headlineAggrVal == null && aggregateVal == null) )
			{
				useHeadlineAsTopData = true;
			}
		}
		else
		{
			// There is only one row - use the headline as the top data.
			useHeadlineAsTopData = true;
		}
		
		if (useHeadlineAsTopData == true)
		{
			model.setTopCausalityData(headlineData);
			model.setTopEvidenceId(headlineData.getEvidenceId());
		}
		else
		{
			CausalityData topData = aggregate.getTopCausalityData();
			if (topData != null)
			{
				// NB. An aggregated 'Others' row will not have a top causality data set
				// as how would we decide which of the constituent 'top' items to use?
				CausalityDataModel topCausalityData = new CausalityDataModel();
				topCausalityData.setStartTime(topData.getStartTime());
				topCausalityData.setEndTime(topData.getEndTime());
				topCausalityData.setCount(topData.getCount());
				topCausalityData.setEvidenceId(aggregate.getTopEvidenceId());
				
				// Set the other fields in the top item of cauality data.
				Evidence topEvidence = m_EvidenceDAO.getEvidenceSingle(model.getTopEvidenceId());
				DataSourceCategory category = topData.getDataSourceType().getDataCategory();
				topCausalityData.setDataSourceType(
						new DataSourceType(topEvidence.getDataType(), category));
				topCausalityData.setSource(topEvidence.getSource());
				if (category == DataSourceCategory.NOTIFICATION)
				{
					topCausalityData.setDescription(topEvidence.getDescription());
				}
				else
				{
					String metric = (String)(topEvidence.get("metric"));
					topCausalityData.setDescription(metric);
					topCausalityData.setTimeSeriesTypeId(topData.getTimeSeriesTypeId());
					topCausalityData.setTimeSeriesId(topData.getTimeSeriesId());
					topCausalityData.setScalingFactor(topData.getScalingFactor());
				}
				
				// Set the additional attributes.
				if (attributesToSet != null && attributesToSet.size() > 0)
				{
					ArrayList<Attribute> attributes = new ArrayList<Attribute>();
					Object attributeVal;
					for (String attributeName : attributesToSet)
					{
						attributeVal = topEvidence.get(attributeName);
						if (attributeVal != null)
						{
							attributes.add(new Attribute(attributeName, attributeVal.toString()));
						}
					}
					
					if (attributes.size() > 0)
					{
						topCausalityData.setAttributes(attributes);
					}
				}
	
				model.setTopCausalityData(topCausalityData);
			}
		}
		
		return model;
	}
	
	
	/**
	 * Creates a DatePagingLoadResult object for the supplied list of incident data
	 * and load criteria.
	 * @param incidentList	list of incident data to send back in the load result.
	 * @param loadConfig load config specifying the data that was requested.
	 * @param latestIncidents latest (most recent) set of incidents, if known.
	 * @param earliest earliest (furthest back in time) set of incidents, if known.
	 * @return the DatePagingLoadResult with the requested evidence data.
	 */
	protected DatePagingLoadResult<IncidentModel> createDatePagingLoadResult(
			List<Incident> incidentList, ActivityPagingLoadConfig loadConfig,
			List<Incident> latestIncidents, List<Incident> earliestIncidents)
	{
		int anomalyThreshold = loadConfig.getAnomalyThreshold();

		// Get the latest and earliest incidents in the DB.
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		List<Incident> startIncidents = latestIncidents;
		if (startIncidents == null)
		{
			startIncidents = m_IncidentDAO.getFirstPage(anomalyThreshold, 1);
			
		}

		Date startDate = null;
		if (startIncidents != null && startIncidents.size() > 0)
		{
			startDate = startIncidents.get(0).getTime();
		}
		
		List<Incident> endIncidents = earliestIncidents;
		if (endIncidents == null)
		{
			endIncidents = m_IncidentDAO.getLastPage(anomalyThreshold, 1);
		}
		Incident endIncident = null;
		Date endDate = null;
		if (endIncidents != null && endIncidents.size() > 0)
		{
			endIncident = endIncidents.get(endIncidents.size() - 1);
			endDate = endIncident.getTime();
		}
		
		Date firstRowTime = null;
		if (incidentList != null && incidentList.size() > 0)
		{
			Incident firstRow = incidentList.get(0);
			firstRowTime = firstRow.getTime();
		}
		
		// Set the flag indicating if there is an earlier load result for this 
		// paging config. i.e. false if the result set is null/empty or the last
		// row in the result set is the record that is furthest back in time for this config.
		boolean isEarlierResults = false;
		if (incidentList != null && incidentList.size() > 0)
		{
			int lastIdInResults = incidentList.get(incidentList.size()-1).getTopEvidenceId();	
			int earliestId = endIncident.getTopEvidenceId();
			
			if (lastIdInResults != earliestId)
			{
				isEarlierResults = true;
			}
		}

		// Convert to a list of IncidentModels, with description for request locale.
		ArrayList<IncidentModel> modelList = new ArrayList<IncidentModel>();
		IncidentModel incidentModel;
		for (Incident incident : incidentList)
		{				
			incidentModel = createIncidentModel(incident);
			modelList.add(incidentModel);
		}
		
		s_Logger.debug("createDatePagingLoadResult returning " + modelList.size());
		
		return new DatePagingLoadResult<IncidentModel>(modelList, TimeFrame.SECOND,
				firstRowTime, startDate, endDate, isEarlierResults);
	}
	
	
    /**
     * Comparator which sorts Incidents by descending anomaly score.
     */
    class AnomalyScoreComparator implements Comparator<Incident>
    {

		@Override
        public int compare(Incident incident1, Incident incident2)
        {
        	int anomaly1 = incident1.getAnomalyScore();
        	int anomaly2 = incident2.getAnomalyScore();
        	
        	return anomaly2 - anomaly1;
        }
    	
    }
    
    
    /**
     * Comparator which sorts CausalityAggregateModel data by the value of the
     * aggregation attribute, with any 'Others' row placed at the end. 
     */
    class CausalityAggregateValueComparator implements Comparator<CausalityAggregateModel>
    {
    	@Override
        public int compare(CausalityAggregateModel aggr1,
                CausalityAggregateModel aggr2)
        {
    		int compVal = 0;
    		
    		String value1 = aggr1.getAggregateValue();
    		String value2 = aggr2.getAggregateValue();
    		
    		
    		if (value1 != null && value2 != null)
    		{
    			compVal = value1.compareToIgnoreCase(value2);
    		}
    		else
    		{
    			// Place 'Others' row at the end.
	    		if (value1 == null && aggr1.isAggregateValueNull() == false)
		        {
		        	compVal = 1;
		        }
	    		else
		        {
		        	if (value2 == null && aggr2.isAggregateValueNull() == false)
		        	{
		        		compVal = -1;
		        	}
		        	else
		        	{
		        		// Place row where aggregate value is null at end (but before 'Others' row).
		        		if (value1 == null)
		        		{
		        			compVal = 1;
		        		}
		        		else
		        		{
		        			if (value2 == null)
		        			{
		        				compVal = -1;
		        			}
		        		}
		        	}
		        }
    		}
    		
    		return compVal;
        }
    }
    
    
    /**
     * Comparator which sorts CausalityDataModel data to find the 'top' item of
     * causality data within an activity.
     */
    class TopCausalityDataComparator implements Comparator<CausalityDataModel>
    {
    	private Evidence m_ShowEvidence;
    	
    	/**
    	 * Creates a new comparator to sort causality data.
    	 * @param showEvidence evidence whose causality data must be shown.
    	 */
    	public TopCausalityDataComparator(Evidence showEvidence)
    	{
    		m_ShowEvidence = showEvidence;
    	}

    	
		@Override
        public int compare(CausalityDataModel model1, CausalityDataModel model2)
        { 
			if (m_ShowEvidence != null)
			{
				// Ensure the causality data corresponding to the specified item
				// of evidence is shown. Note that evidence ID is likely to only
				// be set in that matching causality data - see getAnalysisTreeData().
				int showEvID = m_ShowEvidence.getId();
				if (model1.getEvidenceId() == showEvID)
				{
					return -1;
				}
				
				if (model2.getEvidenceId() == showEvID)
				{
					return 1;
				}
			}

			// Sort by magnitude, significance, and then by start time.
			int comp = model2.getMagnitude() - model1.getMagnitude();
			if (comp == 0)
			{
				comp = model2.getSignificance() - model1.getSignificance();
			}

			if (comp == 0)
			{
				comp = model2.getStartTime().compareTo(model1.getStartTime());
			}

			return comp;
        }
    	
    }
}
