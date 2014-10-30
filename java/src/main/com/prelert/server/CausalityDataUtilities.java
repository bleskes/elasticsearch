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

package com.prelert.server;

import static com.prelert.data.PropertyNames.METRIC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.prelert.dao.CausalityDAO;
import com.prelert.data.Attribute;
import com.prelert.data.CausalityAggregate;
import com.prelert.data.CausalityData;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.Evidence;
import com.prelert.data.ProbableCause;
import com.prelert.data.ProbableCauseCollection;
import com.prelert.data.gxt.AttributeModel;
import com.prelert.data.gxt.CausalityDataModel;
import com.prelert.data.gxt.ProbableCauseModel;


/**
 * Utility class for performing operations on causality data. The class contains
 * methods for activities such as aggregating a list of probable causes by type
 * and description, or normalising the values in time series type probable causes.
 * @author Pete Harverson
 */
public class CausalityDataUtilities
{
	static Logger s_Logger = Logger.getLogger(CausalityDataUtilities.class);
	
	private static CausalityDataUtilities s_Instance;
	
	
	/**
	 * Returns an instance of the causality data utilities class.
	 * @return the CausalityDataUtilities instance.
	 */
	public static CausalityDataUtilities getInstance()
	{
		if (s_Instance == null)
		{
			s_Instance = new CausalityDataUtilities();
		}
		
		return s_Instance;
	}
	
	
	private CausalityDataUtilities()
	{
		
	}
	
	
	/**
	 * Combines a list of aggregated causality data into a single aggregated object
	 * across multiple values of the aggregate attribute. This is used to combine a
	 * set of distinct results for a single 'Other results' type row.
	 * @param causalityData list of of aggregated causality data, where each item 
	 * 	represents a common attribute value e.g. source=server1 or source=server2
	 * @return a single item of aggregated causality data e.g. across server1 and server2.
	 */
	public CausalityAggregate aggregateCausalityData(List<CausalityAggregate> causalityData)
	{
		CausalityAggregate aggregate = new CausalityAggregate();
		
		// Need to set properties for a single aggregate object:
		// start time
		// end time
		// notification count
		// feature count
		// source count
		// source names
		CausalityAggregate first = causalityData.get(0);
		long startTime = first.getStartTime().getTime();
		long endTime = first.getEndTime().getTime();
		int notificationCount = first.getNotificationCount();
		int featureCount = first.getFeatureCount();
		List<String> sourceNames = first.getSourceNames();
		
		CausalityAggregate aggr;
		List<String> sources;
		for (int i = 1; i < causalityData.size(); i++)
		{
			aggr = causalityData.get(i);

			startTime = Math.min(startTime, aggr.getStartTime().getTime());
			endTime = Math.max(endTime, aggr.getEndTime().getTime());
			notificationCount += aggr.getNotificationCount();
			featureCount += aggr.getFeatureCount();
			
			// Add in any sources not already in the aggregated list.
			sources = aggr.getSourceNames();		
			for (String sourceName : sources)
			{
				if (sourceNames.contains(sourceName) == false)
				{
					sourceNames.add(sourceName);
				}
			}

		}
		
		if (causalityData.size() > 1 || first.getAggregateValue() != null)
		{
			aggregate.setAggregateValueNull(false);
		}
		
		aggregate.setStartTime(new Date(startTime));
		aggregate.setEndTime(new Date(endTime));
		aggregate.setNotificationCount(notificationCount);
		aggregate.setFeatureCount(featureCount);
		aggregate.setSourceCount(sourceNames.size());
		aggregate.setSourceNames(sourceNames);
		
		return aggregate;
	}
	
	
	/**
	 * Aggregates a list of probable causes by data type and description.
	 * @param probableCauses the list of ProbableCause objects to aggregate.
	 * @param evidenceId the id of the notification or time series feature whose
	 * 	probable causes are being aggregated.
	 * @return a list of aggregated ProbableCauseCollection objects.
	 */
	public List<ProbableCauseCollection> aggregateProbableCauses(
			List<ProbableCause> probableCauses, int evidenceId)
    {
    	s_Logger.debug("aggregateProbableCauses(" + evidenceId + 
    			") - number of probable causes to aggregate = " + probableCauses.size());
    	
    	normaliseTimeSeriesData(probableCauses);
    	
    	
    	// Aggregate by type and then description to create a list of
    	// 'aggregated' ProbableCauseCollection objects with properties:
    	// 	- 	DataSourceType
    	//	- 	Start time		(single time for TIME SERIES features)
    	//	- 	End time 		(applicable to NOTIFICATION types only)
    	//	-   Description
    	//	- 	Count 			(total number of notifications/time series)
    	//	- 	Source count
    	// 	- 	List<ProbableCauseModel> probable causes
    	List<ProbableCauseCollection> aggregatedList = 
    		new ArrayList<ProbableCauseCollection>();
    	
    	// Hash by DataSourceType e.g. p2pslog, system_udp
    	HashMap<DataSourceType, List<ProbableCause>> mapByType = 
    		new HashMap<DataSourceType, List<ProbableCause>>();
    	DataSourceType dsType;
    	List<ProbableCause> listForType;
    	for (ProbableCause probableCause : probableCauses)
    	{	
    		dsType = probableCause.getDataSourceType();
    		listForType = mapByType.get(dsType);
    		if (listForType == null)
    		{
    			listForType = new ArrayList<ProbableCause>();	
    			mapByType.put(dsType, listForType);
    		}
    		listForType.add(probableCause);
    	}
    	
    	// Aggregate by DataSourceType and description.
    	HashMap<String, List<ProbableCause>> mapByDesc;
    	List<ProbableCause> listForDsDesc;
    	String desc;
    	Iterator<DataSourceType> byTypeIter = mapByType.keySet().iterator();
    	while (byTypeIter.hasNext())
    	{
    		dsType = byTypeIter.next();
    		listForType = mapByType.get(dsType);
    		
    		mapByDesc = new HashMap<String, List<ProbableCause>>();
    		
    		for (ProbableCause probableCause : listForType)
    		{
    			dsType = probableCause.getDataSourceType();
    			
    			// Key on description.
    			desc = probableCause.getDescription();
    			
    			listForDsDesc = mapByDesc.get(desc);
    			if (listForDsDesc == null)
        		{
    				listForDsDesc = new ArrayList<ProbableCause>();
    				mapByDesc.put(desc, listForDsDesc);
        		}
    			listForDsDesc.add(probableCause);
    		}
    		
    		// Create the aggregated ProbableCauseModelCollection objects.
    		Iterator<String> byDescIter = mapByDesc.keySet().iterator();
    		ProbableCauseCollection probCauseCollection;
    		List<ProbableCause> modelList;
        	while (byDescIter.hasNext())
        	{
        		desc = byDescIter.next();
        		listForDsDesc = mapByDesc.get(desc);
        		
        		probCauseCollection = createProbableCauseCollection(listForDsDesc);
        		
        		// For TIME SERIES types, 
        		// sort the list of ProbableCauseModels by value of magnitude field.
        		if (dsType.getDataCategory() == DataSourceCategory.TIME_SERIES)
        		{
        			modelList = probCauseCollection.getProbableCauses();
        			Collections.sort(modelList, new ProbableCauseMagnitudeComparator(evidenceId));
        		}
        		
        		aggregatedList.add(probCauseCollection);
        	}
    		
    	}
    	
    	s_Logger.debug("aggregatedProbableCauses(" + evidenceId + ") - returning " + 
    			aggregatedList.size() + "  ProbableCauseModelCollection objects");
    	
    	return aggregatedList;
    }
	
	
	/**
	 * For the incident containing the specified item of evidence, the method returns 
	 * the first (earliest) notification with the specified type, description,
	 * source and attributes.
	 * @param evidenceId id of a notification or feature from the incident of incident.
	 * @param dataSourceName name of the data source of the evidence to return.
	 * @param description description of the evidence to return.
	 * @param source name of the source (server) of the evidence to return.
	 * @param attributes any additional attributes of the evidence to return.
	 * @param causalityDAO causality data access object to use for the query.
	 * @return earliest notification with specified attributes.
	 */
	public Evidence getEarliestEvidence(int evidenceId, String dataSourceName,
    		String description, String source, List<Attribute> attributes, 
    		CausalityDAO causalityDAO)
    {
    	// Call cause_list_notifications_last_page, passing the full list
    	// of attributes as the filter.
		ArrayList<String> filterAttributes = new ArrayList<String>();
		ArrayList<String> filterValues = new ArrayList<String>();
		
		// Add in standard attributes.
		filterAttributes.add("description");
		filterAttributes.add("type");
		filterAttributes.add("source");
		
		filterValues.add(description);
		filterValues.add(dataSourceName);
		filterValues.add(source);
		
		if (attributes != null && attributes.size() > 0)
		{
			// Add in additional attributes.
			for (Attribute attribute : attributes)
			{
				filterAttributes.add(attribute.getAttributeName());
				filterValues.add(attribute.getAttributeValue());
			}
		}
		
		List<Evidence> evidenceList = causalityDAO.getLastPage(
				false, evidenceId, filterAttributes, filterValues, 1);
		
		Evidence earliestEvidence = null;
		
		if (evidenceList != null && evidenceList.size() > 0)
		{
			earliestEvidence = evidenceList.get(evidenceList.size()-1);
		}
		
	    return earliestEvidence;
    }
	
	
	/**
	 * For the incident containing the specified item of evidence, the method returns 
	 * the last (latest) notification with the specified type, description,
	 * source and attributes.
	 * @param evidenceId id of a notification or feature from the incident of incident.
	 * @param dataSourceName name of the data source of the evidence to return.
	 * @param description description of the evidence to return.
	 * @param source name of the source (server) of the evidence to return.
	 * @param attributes any additional attributes of the evidence to return.
	 * @param causalityDAO causality data access object to use for the query.
	 * @return earliest notification with specified attributes.
	 */
	public Evidence getLatestEvidence(int evidenceId, String dataSourceName,
    		String description, String source, List<Attribute> attributes, 
    		CausalityDAO causalityDAO)
    {
    	// Call cause_list_notifications_first_page, passing the full list
    	// of attributes as the filter.
		ArrayList<String> filterAttributes = new ArrayList<String>();
		ArrayList<String> filterValues = new ArrayList<String>();
		
		// Add in standard attributes.
		filterAttributes.add("description");
		filterAttributes.add("type");
		filterAttributes.add("source");
		
		filterValues.add(description);
		filterValues.add(dataSourceName);
		filterValues.add(source);
		
		if (attributes != null && attributes.size() > 0)
		{
			// Add in additional attributes.
			for (Attribute attribute : attributes)
			{
				filterAttributes.add(attribute.getAttributeName());
				filterValues.add(attribute.getAttributeValue());
			}
		}
		
		List<Evidence> evidenceList = causalityDAO.getFirstPage(
				false, evidenceId, filterAttributes, filterValues, 1);
		
		Evidence latestEvidence = null;
		
		if (evidenceList != null && evidenceList.size() > 0)
		{
			latestEvidence = evidenceList.get(0);
		}
		
	    return latestEvidence;
    }
	
	
	/**
	 * Normalises the time series data in the supplied list of probable causes.
	 * @param aggregatedList list of probable causes containing the data
	 * 	to be normalised.
	 */
	protected void normaliseTimeSeriesData(List<ProbableCause> probableCauses)
	{
		Map<Integer, Double> peakValuesByTypeId = getPeakValuesByTypeId(probableCauses);
		if (peakValuesByTypeId.size() > 0)
		{
			int timeSeriesTypeId;
			double peakValueForType;
			double scalingFactor;
			
			for (ProbableCause probableCause : probableCauses)
			{
				if (probableCause.getDataSourceType().getDataCategory() == DataSourceCategory.TIME_SERIES)
				{
					timeSeriesTypeId = probableCause.getTimeSeriesTypeId();
					peakValueForType = peakValuesByTypeId.get(timeSeriesTypeId);
					
					// Peak value for type may be 0 as the back-end uses the time of the 
					// 'headline' feature or notification when calculating the peak value 
					// for a probable cause, so the feature may be outside of the window 
					// which has the incident time at its centre.
					if (peakValueForType != 0)
					{
						scalingFactor = probableCause.getScalingFactor();
						probableCause.setScalingFactor(scalingFactor/peakValueForType);
					}
				}
			}
		}
	}
	
	
	/**
	 * Builds a map of peak values against time series type id for the supplied
	 * list of probable cause data.
	 * @param probableCauses list of ProbableCause data.
	 * @return a map of peak values against time series type id.
	 */
	public Map<Integer, Double> getPeakValuesByTypeId(
			List<ProbableCause> probableCauses)
	{
		// Build a map of time series type id against probable causes with that id.
		HashMap<Integer, List<ProbableCause>> mapByType = 
			new HashMap<Integer, List<ProbableCause>>();
		
		int timeSeriesTypeId;
		List<ProbableCause> byTypeList;
		for (ProbableCause probableCause : probableCauses)
		{
			if (probableCause.getDataSourceType().getDataCategory() == DataSourceCategory.TIME_SERIES)
			{
				timeSeriesTypeId = probableCause.getTimeSeriesTypeId();
				
				byTypeList = mapByType.get(timeSeriesTypeId);
				if (byTypeList == null)
				{
					byTypeList = new ArrayList<ProbableCause>(); 
					mapByType.put(timeSeriesTypeId, byTypeList);
				}
				byTypeList.add(probableCause);	
			}
		}
		
		HashMap<Integer, Double> peakValuesByType = new HashMap<Integer, Double>();
		
		// Determine the peak value for each time series type id
		// (combines type and metric e.g system_udp/packets_received).
		Iterator<Integer> byTypeIter = mapByType.keySet().iterator();
		
		double peakValueForType;
		while (byTypeIter.hasNext())
		{
			timeSeriesTypeId = byTypeIter.next();
			byTypeList = mapByType.get(timeSeriesTypeId);
			
			peakValueForType = getMaximumPeak(byTypeList);
			peakValuesByType.put(timeSeriesTypeId, peakValueForType);
		}
		
		return peakValuesByType;
	}
	
	
	/**
	 * Returns a map of lists of causality data by the value of a particular
	 * attribute.
	 * @param causalityDataList list of <code>CausalityDataModel</code> objects
	 * 	hashed against the value of the specified attribute. A zero length key is
	 * 	used for causality data where the value of the attribute is <code>null</code>.
	 * @param attributeName name of the attribute for which to build the map by value.
	 * @return map of lists of causality data by the value of a particular	attribute.
	 */
	public HashMap<String, List<CausalityDataModel>> getMapByAttributeValue(
			List<CausalityData> causalityDataList, String attributeName)
	{
		HashMap<String, List<CausalityDataModel>> groups = 
			new HashMap<String, List<CausalityDataModel>>();
		String attributeValue;
		CausalityDataModel model;
		List<CausalityDataModel> modelList;
		
		for (CausalityData causalityData : causalityDataList)
		{
			model = createCausalityDataModel(causalityData);
			
			attributeValue = model.get(attributeName, "");
			modelList = groups.get(attributeValue);
			if (modelList == null)
			{
				modelList = new ArrayList<CausalityDataModel>();
				groups.put(attributeValue, modelList);
			}
			modelList.add(model);
		}
		
		return groups;
	}
	
	
	/**
	 * Returns the causality data matching the specified 'headline' item of evidence
	 * from the supplied list. If a match is found, the evidence ID in the matching
	 * causality data will be set to the ID of the headline evidence.
	 * @param modelList list of <code>CausalityDataModel</code> objects to search.
	 * @param headline the headline notification or time series feature.
	 * @param causalityAttributeNames list of causality data attributes to be compared
	 * 	with the headline evidence.
	 * @param attributeName name of attribute whose values must match (or both be null)
	 * 	in the causality data and headline evidence.
	 * @return the causality data corresponding to the headline feature or notification,
	 * 	or <code>null</code> if no data in the supplied list matches the headline.
	 */
	public CausalityDataModel getHeadlineCausalityData(List<CausalityDataModel> modelList,
			Evidence headline, List<String> causalityAttributeNames, String attributeName)
	{
		CausalityDataModel headlineData = null;
		
		String evValue = (String)(headline.get(attributeName));
		
		String headlineType = headline.getDataType();
    	String headlineDesc = headline.getDescription();
    	String headlineMetric = (String)(headline.get(METRIC));
    	if (headline.get(METRIC) != null)
    	{
    		headlineDesc = headlineMetric;
    	}
    	String headlineSource = headline.getSource();
    	List<Attribute> headlineAttributes = null;
    	if (causalityAttributeNames != null)
    	{
    		String attrVal;
	    	for (String attrName : causalityAttributeNames)
	    	{
	    		attrVal = (String)(headline.get(attrName));
	    		if (attrVal != null)
	    		{
	    			if (headlineAttributes == null)
	    			{
	    				headlineAttributes = new ArrayList<Attribute>();
	    			}
	    			headlineAttributes.add(new Attribute(attrName, attrVal));
	    		}
	    	}
    	}
		
		boolean result = false;
		String causalityValue;
		for (CausalityDataModel causalityData : modelList)
		{
			causalityValue = causalityData.get(attributeName);
			
			result = (evValue == null && causalityValue == null);
    		if (result == false)
    		{
    			boolean bothNonNull = (evValue != null && causalityValue != null);
    			result = bothNonNull && (evValue).equals(causalityValue);
    		}
    		
    		if (result == true)
    		{
    	    	// Compare the properties of the headline evidence to the set of causality data.
	    		result = headlineType.equals(causalityData.getDataSourceType().getName());
	    		result = result && headlineDesc.equals(causalityData.getDescription());
	    		result = result && headlineSource.equals(causalityData.getSource());
	    		
	    		boolean bothNull = (headlineAttributes == null && causalityData.getAttributes() == null);
	    		if (!bothNull)
	    		{
	    			boolean bothNonNull = (headlineAttributes != null && causalityData.getAttributes() != null);
	    			result = result && bothNonNull && (headlineAttributes).equals(causalityData.getAttributes());
	    		}
	    		
	    		if (result == true)
	    		{
	    			headlineData = causalityData;
	    			headlineData.setEvidenceId(headline.getId());
	    			break;
	    		}
    		}
		}
		
		return headlineData;
	}
	
	
	/**
	 * Returns the maximum peak value in the supplied list of time series
	 * probable causes.
	 * @param aggregatedList list of time series probable causes.
	 * @return maximum peak value.
	 */
	protected double getMaximumPeak(List<ProbableCause> probableCauses)
	{
		double maxPeak = 0;
		
		for (ProbableCause probableCause : probableCauses)
		{
			maxPeak = Math.max(maxPeak, probableCause.getPeakValue());
		}
		
		return maxPeak;
	}
	
	
	/**
	 * Returns the maximum peak value in the supplied list of aggregated time series
	 * probable causes.
	 * @param aggregatedList list of aggregated time series probable causes.
	 * @return maximum peak value.
	 */
	protected double getMaximumPeakValue(List<ProbableCauseCollection> aggregatedList)
	{
		double maxPeak = 0;
		
		List<ProbableCause> probableCauses;
		for (ProbableCauseCollection aggregated : aggregatedList)
		{
			probableCauses = aggregated.getProbableCauses();
			
			for (ProbableCause probableCause : probableCauses)
			{
				maxPeak = Math.max(maxPeak, probableCause.getPeakValue());
			}
		}
		
		return maxPeak;
	}
	
	
	/**
	 * Creates an aggregate ProbableCauseCollection object from a list
	 * of ProbableCause objects, setting all the required fields.
	 * @param probableCauses list of ProbableCause objects.
	 * @return a ProbableCauseCollection object.
	 */
	protected ProbableCauseCollection createProbableCauseCollection(
			List<ProbableCause> probableCauses)
	{
		ProbableCauseCollection probCauseCollection = 
			new ProbableCauseCollection();
		
		// Sort the full list by time so that for notifications we only store the
		// first and last probable causes in the aggregation.
		int numProbCauses = probableCauses.size();
		if (numProbCauses > 1)
		{	
			// Sort by time, and set the start and end times.
			Collections.sort(probableCauses, new ProbableCauseTimeComparator());
		}
		
		// Convert the ProbableCause objects to ProbableCauseModel objects,
		// and calculate the ProbableCauseModelCollection count.
		DataSourceCategory dsCategory = probableCauses.get(0).getDataSourceType().getDataCategory();
		int index = 0;
		int count = 0;
		ArrayList<ProbableCause> probCauseList = new ArrayList<ProbableCause>();
		TreeSet<String> sourcesSet = new TreeSet<String>();
		for (ProbableCause probCause : probableCauses)
		{
			count += (probCause.getCount());
			sourcesSet.add(probCause.getSource());
			
			// For notifications, only store the first and last probable causes.
			if (dsCategory == DataSourceCategory.TIME_SERIES || index == 0 || 
					(index == numProbCauses-1) )
			{
				probCauseList.add(probCause);
			}
			
			index++;
		}

		probCauseCollection.setSize(numProbCauses);
		probCauseCollection.setCount(count);
		probCauseCollection.setSourceCount(sourcesSet.size());
		probCauseCollection.setProbableCauses(probCauseList);
		
		return probCauseCollection;
	}
	
	
	/**
	 * Converts a ProbableCause to a GXT ProbableCauseModel object.
	 * @return ProbableCauseModel object.
	 */
	protected ProbableCauseModel createProbableCauseModel(ProbableCause probCause)
	{
		ProbableCauseModel model = new ProbableCauseModel();
		
		model.setDataSourceType(probCause.getDataSourceType());
		model.setEvidenceId(probCause.getEvidenceId());
		model.setTime(probCause.getTime());
		model.setDescription(probCause.getDescription());
		model.setSource(probCause.getSource());
		model.setCount(probCause.getCount());
		model.setSignificance(probCause.getSignificance());
		model.setMagnitude(probCause.getMagnitude());
		
		if (probCause.getDataSourceType().getDataCategory() == DataSourceCategory.TIME_SERIES)
		{
			// Set metric and stats for time series probable causes.
			model.setTimeSeriesTypeId(probCause.getTimeSeriesTypeId());
			model.setMetric(probCause.getMetric());
			model.setScalingFactor(probCause.getScalingFactor());
			model.setPeakValue(probCause.getPeakValue());
			
			// Set the time series attributes and build the attribute label.
			List<Attribute> attributes = probCause.getAttributes();

			if (attributes != null)
			{
				ArrayList<AttributeModel> attributeModels = new ArrayList<AttributeModel>();
				StringBuilder label = new StringBuilder();
				String attrName;
				String attrVal;
				for (Attribute attribute : attributes)
				{
					attrName = attribute.getAttributeName();
					attrVal = attribute.getAttributeValue();
					
					attributeModels.add(new AttributeModel(attrName, attrVal));
					
					if (label.length() > 0)
					{
						label.append(", ");
					}
					label.append(attrName);
					label.append('=');
					label.append(attrVal);
				
				}
				
				model.setAttributes(attributes);
				model.setAttributeLabel(label.toString());
			}
		}
		else
		{
			model.setSeverity(probCause.getSeverity());
		}
		
		return model;
	}
	
	
	/**
	 * Converts a CausalityData object to a GXT ModelData object.
	 * @param causalityData CausalityData object to convert.
	 * @return GXT CausalityDataModel object.
	 */
	public CausalityDataModel createCausalityDataModel(CausalityData causalityData)
	{
		CausalityDataModel model = new CausalityDataModel();
		DataSourceType dsType = causalityData.getDataSourceType();
		model.setDataSourceType(dsType);
		model.setDescription(causalityData.getDescription());
		model.setSource(causalityData.getSource());
		model.setStartTime(causalityData.getStartTime());
		model.setEndTime(causalityData.getEndTime());
		model.setSignificance(causalityData.getSignificance());
		model.setCount(causalityData.getCount());
		model.setAttributes(causalityData.getAttributes());
		
		// For time series features, description stores the name of the metric.
		if ( (dsType != null) && (dsType.getDataCategory().equals(DataSourceCategory.TIME_SERIES_FEATURE)) )
		{
			model.set(METRIC, causalityData.getDescription());
		}
		
		// Round magnitude to the nearest int.
		double magnitude = causalityData.getMagnitude();
		model.setMagnitude((int)Math.floor(magnitude + 0.5d));
		
		if (model.getDataSourceCategory() == DataSourceCategory.TIME_SERIES_FEATURE)
		{
			model.setTimeSeriesTypeId(causalityData.getTimeSeriesTypeId());
			model.setTimeSeriesId(causalityData.getTimeSeriesId());
			model.setScalingFactor(causalityData.getScalingFactor());
		}
		
		return model;
	}
	
	
	/**
     * Comparator which sorts ProbableCause objects by:
     * <ul>
     * <li>first if it is the source evidence id / time series feature</li>
     * <li>decreasing value of the magnitude property.
     * </ul>
     */
    class ProbableCauseMagnitudeComparator implements Comparator<ProbableCause>
    {
    	private int m_EvidenceId;
    	
    	/**
    	 * Creates a new comparator which sorts probable causes by magnitude.
    	 * @param evidenceId the id of the item of evidence whose probable causes are
    	 * 		being displayed.
    	 */
    	ProbableCauseMagnitudeComparator(int evidenceId)
    	{
    		m_EvidenceId = evidenceId;
    	}
    	
        public int compare(ProbableCause probCause1, ProbableCause probCause2)
        {
        	if (probCause1.getEvidenceId() == m_EvidenceId)
        	{
        		return -1;
        	}
        	else if (probCause2.getEvidenceId() == m_EvidenceId)
        	{
        		return 1;
        	}
        	else
        	{
        		double magnitude1 =  probCause1.getMagnitude();
        		double magnitude2 =  probCause2.getMagnitude();
        		return (int) (magnitude2 - magnitude1);  
        	}
        }
    }
	
	
	/**
     * Comparator which sorts ProbableCause objects by time of occurrence
     * (earliest first).
     */
    class ProbableCauseTimeComparator implements Comparator<ProbableCause>
    {
    	
        public int compare(ProbableCause probCause1, ProbableCause probCause2)
        {
        	Date time1 = probCause1.getTime();
        	Date time2 = probCause2.getTime();
        	
        	return time1.compareTo(time2);
        }
    }
}
