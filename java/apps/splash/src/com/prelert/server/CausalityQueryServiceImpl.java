/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.prelert.dao.CausalityDAO;
import com.prelert.dao.EvidenceDAO;
import com.prelert.data.Attribute;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.ProbableCause;
import com.prelert.data.Severity;
import com.prelert.data.TimeFrame;
import com.prelert.data.gxt.AttributeModel;
import com.prelert.data.gxt.CausalityEvidencePagingLoadConfig;
import com.prelert.data.gxt.DatePagingLoadResult;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.data.gxt.ProbableCauseModel;
import com.prelert.data.gxt.ProbableCauseModelCollection;
import com.prelert.service.CausalityQueryService;


/**
 * Server-side implementation of the service for retrieving causality data from the
 * Prelert database.
 * @author Pete Harverson
 */
public class CausalityQueryServiceImpl extends RemoteServiceServlet 
	implements CausalityQueryService
{

	static Logger logger = Logger.getLogger(CausalityQueryServiceImpl.class);
	
	private CausalityDAO 	m_CausalityDAO;
	private EvidenceDAO 	m_EvidenceDAO;
	
	/** 
	 * Number of probable causes to display on opening. Those with the highest
	 * peak value will be displayed, ensuring that the highest peak from each 
	 * data type is displayed. The entrance point time series feature will also
	 * be displayed if it is not in this 'top' list.
	 */
	public static final int				NUM_TIME_SERIES_ON_OPENING = 5;
	
	
	/**
	 * Sets the CausalityViewDAO to be used by the causality query service.
	 * @param causalityDAO the data access object for causality views.
	 */
	public void setCausalityDAO(CausalityDAO causalityDAO)
	{
		m_CausalityDAO = causalityDAO;
	}
	
	
	/**
	 * Returns the CausalityViewDAO being used by the causality query service.
	 * @return the data access object for causality views.
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
	
	
	/**
	 * Returns the list of probable causes for the item of evidence with the specified id.
	 * @param evidenceId the id of the item of evidence for which to obtain
	 * 	the probable causes.
	 * @param timeSpanSecs time span, in seconds, that is used for calculating 
	 * 	metrics for probable causes that are features in time series e.g. peak 
	 * 	value in time window of interest.
	 * @return list of probable causes. This list will be empty if the item of
	 * 	evidence has no probable causes.
	 */
    public List<ProbableCauseModel> getProbableCauses(int evidenceId, int timeSpanSecs)
    {
    	List<ProbableCause> probableCauses = 
    		m_CausalityDAO.getProbableCauses(evidenceId, timeSpanSecs);
    	
    	List<ProbableCauseModel> modelList = createProbableCauseModelData(probableCauses);
    	return modelList;
    }


    @Override
    public List<String> getEvidenceColumns(String dataType)
    {
	    List<String> columns = m_EvidenceDAO.getAllColumns(dataType, TimeFrame.SECOND);
	    
	    // Don't show the Probable Cause column.
	    columns.remove(EvidenceModel.COLUMN_NAME_PROBABLE_CAUSE);
	    
	    return columns;
    }


	@Override
    public DatePagingLoadResult<EvidenceModel> getFirstPage(
    		CausalityEvidencePagingLoadConfig loadConfig)
    {
    	int evidenceId = loadConfig.getEvidenceId();
		List<String> filterAttributes = loadConfig.getFilterAttributes();
		List<String> filterValues = loadConfig.getFilterValues();
		
		List<EvidenceModel> evidenceList = m_CausalityDAO.getFirstPage(
				evidenceId, filterAttributes, filterValues);
		
		return createDatePagingLoadResult(evidenceList, evidenceId, 
				filterAttributes, filterValues);
    }
    
    
    @Override
    public DatePagingLoadResult<EvidenceModel> getLastPage(
    		CausalityEvidencePagingLoadConfig loadConfig)
    {
    	int evidenceId = loadConfig.getEvidenceId();
		List<String> filterAttributes = loadConfig.getFilterAttributes();
		List<String> filterValues = loadConfig.getFilterValues();
		
		List<EvidenceModel> evidenceList = m_CausalityDAO.getLastPage(
				evidenceId, filterAttributes, filterValues);
		
		return createDatePagingLoadResult(evidenceList, evidenceId, 
				filterAttributes, filterValues);
    }
    
    
    @Override
    public DatePagingLoadResult<EvidenceModel> getNextPage(
    		CausalityEvidencePagingLoadConfig loadConfig)
    {
    	int bottomRowId = loadConfig.getEvidenceId();
    	Date bottomRowTime = loadConfig.getDate();
    	List<String> filterAttributes = new ArrayList<String>();
		List<String> filterValues = new ArrayList<String>();
		
		List<EvidenceModel> evidenceList = m_CausalityDAO.getNextPage(
				bottomRowId, bottomRowTime, filterAttributes, filterValues);
		
		return createDatePagingLoadResult(evidenceList, bottomRowId, 
				filterAttributes, filterValues);
    }
    

    @Override
    public DatePagingLoadResult<EvidenceModel> getPreviousPage(
			CausalityEvidencePagingLoadConfig loadConfig)
    {
    	int topRowId = loadConfig.getEvidenceId();
    	Date topRowTime = loadConfig.getDate();
    	List<String> filterAttributes = loadConfig.getFilterAttributes();
		List<String> filterValues = loadConfig.getFilterValues();
		
		List<EvidenceModel> evidenceList = m_CausalityDAO.getPreviousPage(
				topRowId, topRowTime, filterAttributes, filterValues);
		
		// If empty, load the first page - the previous button is always enabled.
		if (evidenceList == null || evidenceList.size() == 0)
		{
			evidenceList = m_CausalityDAO.getFirstPage(
					topRowId, filterAttributes, filterValues);
		}
		
		return createDatePagingLoadResult(evidenceList, topRowId, 
				filterAttributes, filterValues);
    }

    
    @Override
    public DatePagingLoadResult<EvidenceModel> getAtTime(
    		CausalityEvidencePagingLoadConfig loadConfig)
    {
    	int evidenceId = loadConfig.getEvidenceId();
    	Date time = loadConfig.getDate();
		List<String> filterAttributes = loadConfig.getFilterAttributes();
		List<String> filterValues = loadConfig.getFilterValues();
		
		List<EvidenceModel> evidenceList = m_CausalityDAO.getAtTime(
				evidenceId, time, filterAttributes, filterValues);
		
		return createDatePagingLoadResult(evidenceList, evidenceId, 
				filterAttributes, filterValues);
    }
    

	/**
	 * Normalises the time series data in the supplied list of probable causes.
	 * @param aggregatedList list of probable causes containing the data
	 * 	to be normalised.
	 */
	protected void normaliseTimeSeriesData(List<ProbableCause> probableCauses)
	{
		// Build a map of time series type ids against aggregated probable causes.
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
		
		// Determine the peak value for each time series type id
		// (combines type and metric e.g system_udp/packets_received).
		Iterator<Integer> byTypeIter = mapByType.keySet().iterator();
		
		double peakValueForType;
		double scalingFactor;
		while (byTypeIter.hasNext())
		{
			timeSeriesTypeId = byTypeIter.next();
			byTypeList = mapByType.get(timeSeriesTypeId);
			
			peakValueForType = getMaximumPeak(byTypeList);
			
			for (ProbableCause probableCause : byTypeList)
			{
				scalingFactor = probableCause.getScalingFactor();
				probableCause.setScalingFactor(scalingFactor/peakValueForType);
			}
		}
	}
	
	
	public List<ProbableCauseModelCollection> getAggregatedProbableCauses(
    		int evidenceId, int timeSpanSecs)
    {
    	List<ProbableCause> probableCauses = 
    		m_CausalityDAO.getProbableCauses(evidenceId, timeSpanSecs);
    	logger.debug("getAggregatedProbableCauses(" + evidenceId + 
    			") - number of probable causes to aggregate = " + probableCauses.size());

    	normaliseTimeSeriesData(probableCauses);
    	
    	
    	// Aggregate by type and then description to create a list of
    	// 'aggregated' ProbableCauseModelCollection objects with properties:
    	// 	- 	DataSourceType
    	//	- 	Start time		(single time for TIME SERIES features)
    	//	- 	End time 		(applicable to NOTIFICATION types only)
    	//	-   Description
    	//	- 	Count 			(total number of notifications/time series)
    	//	- 	Source count
    	// 	- 	List<ProbableCauseModel> probable causes
    	List<ProbableCauseModelCollection> aggregatedList = 
    		new ArrayList<ProbableCauseModelCollection>();
    	
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
    		ProbableCauseModelCollection probCauseCollection;
    		List<ProbableCauseModel> modelList;
        	while (byDescIter.hasNext())
        	{
        		desc = byDescIter.next();
        		listForDsDesc = mapByDesc.get(desc);
        		
        		probCauseCollection = createProbableCauseModelCollection(listForDsDesc);
        		
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
    	
    	// Process the list to work out which to display when the view opens.
    	sortForDisplay(evidenceId, aggregatedList);
    	logger.debug("getAggregatedProbableCauses(" + evidenceId + ") - returning " + 
    			aggregatedList.size() + "  ProbableCauseModelCollection objects");
    	
    	return aggregatedList;
    }
	
	
	/**
	 * Normalises the time series data in the supplied list of aggregated probable causes.
	 * @param aggregatedList list of aggregated probable causes containing the data
	 * 	to be normalised.
	 */
	protected void normaliseTimeSeriesDataByType(List<ProbableCauseModelCollection> aggregatedList)
	{
		// Build a map of data source types against aggregated probable causes.
		HashMap<String, List<ProbableCauseModelCollection>> mapByType = 
			new HashMap<String, List<ProbableCauseModelCollection>>();

		String dsTypeName;
		List<ProbableCauseModelCollection> byTypeList;
		for (ProbableCauseModelCollection aggregated : aggregatedList)
		{
			if (aggregated.getDataSourceCategory() == DataSourceCategory.TIME_SERIES)
			{
				dsTypeName = aggregated.getDataSourceName();
				
				byTypeList = mapByType.get(dsTypeName);
				if (byTypeList == null)
				{
					byTypeList = new ArrayList<ProbableCauseModelCollection>(); 
					mapByType.put(dsTypeName, byTypeList);
				}
				byTypeList.add(aggregated);	
			}
		}
		
		// Determine the peak value for each data source type (e.g system_udp, p2psmon_ipc).
		Iterator<String> byTypeIter = mapByType.keySet().iterator();
		
		HashMap<String, Double> peaksByType = 
			new HashMap<String, Double>();
		
		while (byTypeIter.hasNext())
		{
			dsTypeName = byTypeIter.next();
			byTypeList = mapByType.get(dsTypeName);
			
			peaksByType.put(dsTypeName, getMaximumPeakValue(byTypeList));
		}
		
		// Set the scaling factor of each probable cause to be in relation to other
		// probable causes (scaling_factor) and the peak for its data source type.
		List<ProbableCauseModel> probableCauses;
		double peakValueForType;
		double scalingFactor;
		for (ProbableCauseModelCollection aggregated : aggregatedList)
		{
			if (aggregated.getDataSourceCategory() == DataSourceCategory.TIME_SERIES)
			{
				probableCauses = aggregated.getProbableCauses();
				peakValueForType = peaksByType.get(aggregated.getDataSourceName());
				
				for (ProbableCauseModel probableCause : probableCauses)
				{
					scalingFactor = probableCause.getScalingFactor();
					probableCause.setScalingFactor(scalingFactor/peakValueForType);
				}
			}
		}
	}
	
	
	/**
	 * Organises the aggregated probable causes for the item of evidence with the
	 * specified id to determine which are displayed when the Causality View is 
	 * first opened.
	 * @param evidenceId the id of the item of evidence whose probable causes are
	 * 		being displayed.
	 * @param aggregatedList list of aggregated probable causes that are being displayed.
	 */
	protected void sortForDisplay(int evidenceId, List<ProbableCauseModelCollection> aggregatedList)
	{
		int numTimeSeriesDisplayed = 0;
		boolean foundSourceId = false;
		
		// For time series, build a map of data type against aggregated probable causes.
		HashMap<DataSourceType, List<ProbableCauseModelCollection>> byDataType = 
			new HashMap<DataSourceType, List<ProbableCauseModelCollection>>();

		DataSourceType dataType;
		List<ProbableCauseModelCollection> byTypeList;
		for (ProbableCauseModelCollection model : aggregatedList)
		{
			if (model.getDataSourceCategory() == DataSourceCategory.TIME_SERIES)
			{
				dataType = model.getProbableCause(0).getDataSourceType();
				
				byTypeList = byDataType.get(dataType);
				if (byTypeList == null)
				{
					byTypeList = new ArrayList<ProbableCauseModelCollection>(); 
					byDataType.put(dataType, byTypeList);
				}
				byTypeList.add(model);	
			}
			else
			{
				// All notifications get displayed.
				model.setDisplay(true);
			}
			
			if (foundSourceId == false)
			{
				ProbableCauseModel sourceProbCause = model.getProbableCauseById(evidenceId);
				if (sourceProbCause != null)
				{
					model.setDisplay(true);
					foundSourceId = true;
				}
			}
		}
		
		// Sort each data type by peak value and add the highest.
		CollectionPeakValueComparator peakValueComparator = new CollectionPeakValueComparator();
		Iterator<List<ProbableCauseModelCollection>> byTypeIter = 
			byDataType.values().iterator();
		
		while (byTypeIter.hasNext())
		{
			byTypeList = byTypeIter.next();
			
			Collections.sort(byTypeList, peakValueComparator);
			byTypeList.get(0).setDisplay(true);
			
			numTimeSeriesDisplayed++;
		}
		
		if (numTimeSeriesDisplayed < NUM_TIME_SERIES_ON_OPENING)
		{
			Collections.sort(aggregatedList, peakValueComparator);
			
			Iterator<ProbableCauseModelCollection> iter = aggregatedList.iterator();
			ProbableCauseModelCollection model;
			while ( (iter.hasNext()) && (numTimeSeriesDisplayed < NUM_TIME_SERIES_ON_OPENING) )
			{
				model = iter.next();
				if (model.getDisplay() == false)
				{
					model.setDisplay(true);
					numTimeSeriesDisplayed++;
				}
			}
		}
	}
	
	
	/**
	 * Returns the maximum peak value in the supplied list of time series
	 * probable causes.
	 * @param aggregatedList list of aggregated time series probable causes.
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
	protected double getMaximumPeakValue(List<ProbableCauseModelCollection> aggregatedList)
	{
		double maxPeak = 0;
		
		List<ProbableCauseModel> probableCauses;
		for (ProbableCauseModelCollection aggregated : aggregatedList)
		{
			probableCauses = aggregated.getProbableCauses();
			
			for (ProbableCauseModel probableCause : probableCauses)
			{
				maxPeak = Math.max(maxPeak, probableCause.getPeakValue());
			}
		}
		
		return maxPeak;
	}
	
	
	/**
	 * Creates an aggregate ProbableCauseModelCollection object from a list
	 * of ProbableCause objects, setting all the required fields.
	 * @param probableCauses list of ProbableCause model objects.
	 * @return a ProbableCauseModelCollection object.
	 */
	protected ProbableCauseModelCollection createProbableCauseModelCollection(
			List<ProbableCause> probableCauses)
	{
		ProbableCauseModelCollection probCauseCollection = 
			new ProbableCauseModelCollection();
		
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
		ArrayList<ProbableCauseModel> modelList = new ArrayList<ProbableCauseModel>();
		ProbableCauseModel model;
		TreeSet<String> sourcesSet = new TreeSet<String>();
		for (ProbableCause probCause : probableCauses)
		{
			count += (probCause.getCount());
			sourcesSet.add(probCause.getSource());
			
			// For notifications, only store the first and last probable causes.
			if (dsCategory == DataSourceCategory.TIME_SERIES || index == 0 || 
					(index == numProbCauses-1) )
			{
				model = createProbableCauseModel(probCause);
				modelList.add(model);
			}
			
			index++;
		}

		model = modelList.get(0);
		probCauseCollection.setDataSourceType(model.getDataSourceType());
		probCauseCollection.setDescription(model.getDescription());
		probCauseCollection.setSize(numProbCauses);
		probCauseCollection.setCount(count);
		probCauseCollection.setSourceCount(sourcesSet.size());
		probCauseCollection.setProbableCauses(modelList);
		
		// For notifications, set the severity property.
		if (probCauseCollection.getDataSourceCategory() == DataSourceCategory.NOTIFICATION)
		{
			int id = model.getEvidenceId();
			Severity severity = m_EvidenceDAO.getEvidenceSingle(id).getSeverity();
			probCauseCollection.setSeverity(severity);
		}
		
		return probCauseCollection;
	}

	
	/**
	 * Converts a list of ProbableCause objects to a list of ProbableCauseModel objects.
	 * @return list of ProbableCauseModel objects.
	 */
	protected List<ProbableCauseModel> createProbableCauseModelData(
			List<ProbableCause> probableCauses)
	{
		ArrayList<ProbableCauseModel> modelList = new ArrayList<ProbableCauseModel>();
		
		if (probableCauses != null)
		{
			ProbableCauseModel model;
			for (ProbableCause probCause : probableCauses)
			{
				model = createProbableCauseModel(probCause);
				modelList.add(model);
			}
		}
		
		return modelList;
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
		
		return model;
	}
	
	
	/**
	 * Creates a DatePagingLoadResult object for the supplied list of evidence data
	 * and load criteria.
	 * @return the DatePagingLoadResult.
	 */
	protected DatePagingLoadResult<EvidenceModel> createDatePagingLoadResult(
			List<EvidenceModel> evidenceList, int evidenceId, 
			List<String> filterAttributes, List<String> filterValues)
	{
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		EvidenceModel startEvidence = m_CausalityDAO.getLatestEvidence(
				evidenceId, filterAttributes, filterValues);
		
		Date startDate = null;
		if (startEvidence != null)
		{
			startDate = startEvidence.getTime(TimeFrame.SECOND);
		}
		
		EvidenceModel endEvidence = m_CausalityDAO.getEarliestEvidence(
				evidenceId, filterAttributes, filterValues);
		Date endDate = null;
		if (endEvidence != null)
		{
			endDate = endEvidence.getTime(TimeFrame.SECOND);
		}
		
		Date firstRowTime = null;
		if (evidenceList != null && evidenceList.size() > 0)
		{
			EvidenceModel firstRow = evidenceList.get(0);
			firstRowTime = firstRow.getTime(TimeFrame.SECOND);
		}
		
		// Set the flag indicating if there is an earlier load result for this 
		// paging config. i.e. false if the result set is null/empty or the last
		// row in the result set is the record that is furthest back in time for this config.
		boolean isEarlierResults = false;
		if (evidenceList != null && evidenceList.size() > 0)
		{
			int lastIdInResults = evidenceList.get(evidenceList.size()-1).getId();	
			int earliestId = endEvidence.getId();
			
			if (lastIdInResults != earliestId)
			{
				isEarlierResults = true;
			}
		}
		
		return new DatePagingLoadResult<EvidenceModel>(evidenceList, TimeFrame.SECOND,
				firstRowTime, startDate, endDate, isEarlierResults);
	}
	
	
    /**
     * Comparator which sorts ProbableCause objects by:
     * <ul>
     * <li>first if it is the source evidence id / time series feature</li>
     * <li>decreasing value of the magnitude property.
     * </ul>
     */
    class ProbableCauseMagnitudeComparator implements Comparator<ProbableCauseModel>
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
    	
        public int compare(ProbableCauseModel probCause1, ProbableCauseModel probCause2)
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
    
    
    /**
     * Comparator which sorts ProbableCauseModelCollection in descending order 
     * of normalised peak value.
     */
    class CollectionPeakValueComparator implements Comparator<ProbableCauseModelCollection>
    {

		@Override
        public int compare(ProbableCauseModelCollection model1,
                ProbableCauseModelCollection model2)
        {
			// Compare by peak value as they will appear on the chart.
        	ProbableCauseModel probCause1 = model1.getProbableCause(0);
        	ProbableCauseModel probCause2 = model2.getProbableCause(0);
        	
        	double maxY1 = probCause1.getScalingFactor() * probCause1.getPeakValue();
        	double maxY2 = probCause2.getScalingFactor() * probCause2.getPeakValue();
        	
        	return Double.compare(maxY2, maxY1);
        }
    	
    }

}
