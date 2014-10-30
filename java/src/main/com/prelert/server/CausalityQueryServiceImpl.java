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

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.extjs.gxt.ui.client.Style.SortDir;
import com.extjs.gxt.ui.client.data.BasePagingLoadResult;
import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.extjs.gxt.ui.client.util.DefaultComparator;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.prelert.dao.CausalityDAO;
import com.prelert.dao.EvidenceDAO;
import com.prelert.dao.IncidentDAO;
import com.prelert.data.Attribute;
import com.prelert.data.CausalityData;
import com.prelert.data.CausalityView;
import com.prelert.data.Evidence;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.Incident;
import com.prelert.data.MetricTreeNode;
import com.prelert.data.ProbableCause;
import com.prelert.data.ProbableCauseCollection;
import static com.prelert.data.PropertyNames.*;
import com.prelert.data.TimeFrame;
import com.prelert.data.gxt.CausalityDataModel;
import com.prelert.data.gxt.CausalityDataPagingLoadConfig;
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
@SuppressWarnings("serial")
public class CausalityQueryServiceImpl extends RemoteServiceServlet 
	implements CausalityQueryService
{

	static Logger s_Logger = Logger.getLogger(CausalityQueryServiceImpl.class);
	
	private CausalityDAO 	m_CausalityDAO;
	private EvidenceDAO 	m_EvidenceDAO;
	private IncidentDAO 	m_IncidentDAO;
	
	private int				m_SelectionGridPageSize = 20;

	/**
	 * For consistency, these constants should exactly match the ones used in
	 * the <code>get_time_series_id</code> database procedure
	 */
	public static final String DATATYPE_SUFFIX = " : ";
	public static final String DEFAULT_METRIC_PATH_PREFIX = ", ";

	/** 
	 * Number of probable causes to display on opening. Those with the highest
	 * peak value will be displayed, ensuring that the highest peak from each 
	 * data type is displayed. The entrance point time series feature will also
	 * be displayed if it is not in this 'top' list.
	 */
	public static final int				NUM_TIME_SERIES_ON_OPENING = 5;
	
	
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
	 * Sets the evidence data access object being used by the query service.
	 * @param evidenceDAO the data access object for evidence data.
	 */
	public void setEvidenceDAO(EvidenceDAO evidenceDAO)
	{
		m_EvidenceDAO = evidenceDAO;
	}
	
	
	/**
	 * Returns the evidence data access object being used by the query service.
	 * @return the data access object for evidence data.
	 */
	public EvidenceDAO getEvidenceDAO()
	{
		return m_EvidenceDAO;
	}
	
	
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
     * Sets the page size for the analysis view chart selection grid i.e. the
     * number of rows to return in each load operation.
     * @param pageSize the page size.
     */
    public void setSelectionGridPageSize(int pageSize)
    {
    	s_Logger.debug("selectionGridPageSize set to " + pageSize);
    	m_SelectionGridPageSize = pageSize;
    }
    

	@Override
	public int getSelectionGridPageSize()
	{
		return m_SelectionGridPageSize;
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
    		m_CausalityDAO.getProbableCauses(evidenceId, timeSpanSecs, true);
    	
    	// Convert to list of GXT ProbableCauseModel objects.
    	ArrayList<ProbableCauseModel> modelList = new ArrayList<ProbableCauseModel>();
		if (probableCauses != null)
		{
			ProbableCauseModel model;
			for (ProbableCause probCause : probableCauses)
			{
				model = CausalityDataUtilities.getInstance().createProbableCauseModel(probCause);
				modelList.add(model);
			}
		}
    	return modelList;
    }
    
    
    @Override
    public CausalityView getViewConfiguration(int evidenceId, int timeSpanSecs)
    {
    	// Get the incident containing this item of evidence.
    	Incident incident = m_IncidentDAO.getIncidentForId(evidenceId);
    	
    	// Get the list of tree nodes in the metric path.
    	List<MetricTreeNode> pathTreeNodes = m_IncidentDAO.getIncidentMetricPathNodes(evidenceId);
    	
    	if (pathTreeNodes.size() == 0)
    	{
    		// Data is for more than one type - manually create the metric path nodes
    		// so that something is displayed in the analysis view.
    		// TODO - need to look at treatment for these systems.
    		pathTreeNodes = new ArrayList<MetricTreeNode>();
    		
    		// Add nodes for type and source.
    		MetricTreeNode type = new MetricTreeNode();
    		type.setName(TYPE);
    		type.setPrefix("");
    		
    		MetricTreeNode source = new MetricTreeNode();
    		source.setName(SOURCE);
    		source.setPrefix(DATATYPE_SUFFIX);
    		
    		pathTreeNodes.add(type);
    		pathTreeNodes.add(source);
    		
    		List<String> attributeNames = m_IncidentDAO.getIncidentAttributeNames(evidenceId);
        	
        	// Strip out type, source and description.
        	attributeNames.remove(TYPE);
        	attributeNames.remove(SOURCE);
        	attributeNames.remove(DESCRIPTION);	
        	MetricTreeNode attribute;
        	for (String attributeName : attributeNames)
        	{
        		attribute = new MetricTreeNode();
        		attribute.setName(attributeName);
        		attribute.setPrefix(DEFAULT_METRIC_PATH_PREFIX);
        		pathTreeNodes.add(attribute);
        	}
    	}
    	
    	// Build the map of time series type id versus peak values.
    	List<ProbableCause> probableCauses = 
    		m_CausalityDAO.getProbableCauses(evidenceId, timeSpanSecs, true);
    	Map<Integer, Double> peakValues = 
    		CausalityDataUtilities.getInstance().getPeakValuesByTypeId(probableCauses);
    	
    	CausalityView view = new CausalityView();
    	view.setEvidenceId(evidenceId);
    	view.setMetricPathTreeNodes(pathTreeNodes);
    	view.setPeakValuesByTypeId(peakValues);
    	view.setActivityAnomalyScore(incident.getAnomalyScore());
    	
    	return view;
    }


	@Override
    public PagingLoadResult<CausalityDataModel> getCausalityDataPage(
            CausalityDataPagingLoadConfig config)
    {
		// Extract the non-null and null primary filter attributes.
		ArrayList<String> primaryFilterNamesNull = new ArrayList<String>();
		ArrayList<String> primaryFilterNamesNonNull = new ArrayList<String>();
		ArrayList<String> primaryFilterValues = new ArrayList<String>();
		
		
		List<Attribute> primaryFilterAttributes = config.getPrimaryFilterAttributes();
		String value;
		for (Attribute attribute : primaryFilterAttributes)
		{
			value = attribute.getAttributeValue();
			if (value != null)
			{
				primaryFilterNamesNonNull.add(attribute.getAttributeName());
				primaryFilterValues.add(value);
			}
			else
			{
				primaryFilterNamesNull.add(attribute.getAttributeName());
			}
		}
		
		
	    List<CausalityData> causalityDataList = m_CausalityDAO.getCausalityData(config.getEvidenceId(), 
	    		config.getReturnAttributes(), primaryFilterNamesNull, 
	    		primaryFilterNamesNonNull, primaryFilterValues,
	    		config.getSecondaryFilterName(), config.getSecondaryFilterValue());
	    
	    // Convert to list of GXT CausalityDataModel objects.
    	List<CausalityDataModel> modelList = new ArrayList<CausalityDataModel>();
		if (causalityDataList != null)
		{
			CausalityDataModel model;
			for (CausalityData data : causalityDataList)
			{
				model = CausalityDataUtilities.getInstance().createCausalityDataModel(data);
				modelList.add(model);
			}
			
			// Sort the results if necessary.
			final String sortField = config.getSortField();
			if (sortField != null)
			{
				final DefaultComparator<CausalityDataModel> comparator = 
					new DefaultComparator<CausalityDataModel>();
				Collections.sort(modelList, new Comparator<CausalityDataModel>(){

					@Override
			        public int compare(CausalityDataModel model1,  CausalityDataModel model2)
			        {
		        	      Object v1 = model1.get(sortField);
		        	      Object v2 = model2.get(sortField);
		        	      return comparator.compare(v1, v2);
			        }
				});

				SortDir sortDir = config.getSortDir();
				if (sortDir == SortDir.DESC)
				{
					Collections.reverse(modelList);
				}
			}
		}
		
		// Return the requested page of results. 
		int offset = config.getOffset();
		int limit = config.getLimit();
		int totalLength = modelList.size();
		List<CausalityDataModel> returnList;
		if (totalLength > limit)
		{
			int toIndex = Math.min(offset+limit, totalLength);
			returnList = new ArrayList<CausalityDataModel>();
			for (int i = offset; i < toIndex; i++)
			{
				returnList.add(modelList.get(i));
			}
		}
		else
		{
			returnList = modelList;
		}
		
		s_Logger.debug("getCausalityDataPage() returning " + returnList.size());
	    return new BasePagingLoadResult<CausalityDataModel>(returnList, offset, totalLength);
    }
	
	
    @Override
    public List<String> getCausalityDataColumnValues(int evidenceId,
            String attributeName)
    {
    	List<String> attributeValues = 
    		m_IncidentDAO.getIncidentAttributeValues(evidenceId, attributeName);
    	
    	// Sort alphabetically before returning.
    	Collections.sort(attributeValues, Collator.getInstance());
    	
    	return attributeValues;
    }
    

    @Override
    public int getLatestEvidenceId(int evidenceId, String dataSourceName,
    		String description, String source, List<Attribute> attributes)
    {
		int latestEvId = evidenceId;	// Return incident evidence ID if no match.
		
		Evidence latestEvidence = CausalityDataUtilities.getInstance().getLatestEvidence(
				evidenceId, dataSourceName, description, source, attributes, m_CausalityDAO);
		if (latestEvidence != null)
		{
			latestEvId = latestEvidence.getId();
		}
		
	    return latestEvId;
    }


	@Override
    public List<String> getEvidenceColumns(String dataType)
    {
	    List<String> columns = m_EvidenceDAO.getAllColumns(dataType);
	    
	    // Don't show the Probable Cause column.
	    columns.remove(Evidence.COLUMN_NAME_PROBABLE_CAUSE);
	    
	    return columns;
    }


	@Override
    public DatePagingLoadResult<EvidenceModel> getFirstPage(
    		CausalityEvidencePagingLoadConfig loadConfig)
    {
		boolean isSingleDesc = loadConfig.isSingleDescription();
    	int evidenceId = loadConfig.getRowId();
		List<String> filterAttributes = loadConfig.getFilterAttributes();
		List<String> filterValues = loadConfig.getFilterValues();
		
		List<Evidence> evidenceList = m_CausalityDAO.getFirstPage(isSingleDesc, 
				evidenceId, filterAttributes, filterValues, loadConfig.getPageSize());
		
		return createDatePagingLoadResult(evidenceList, loadConfig);
    }
    
    
    @Override
    public DatePagingLoadResult<EvidenceModel> getLastPage(
    		CausalityEvidencePagingLoadConfig loadConfig)
    {
    	boolean isSingleDesc = loadConfig.isSingleDescription();
    	int evidenceId = loadConfig.getRowId();
		List<String> filterAttributes = loadConfig.getFilterAttributes();
		List<String> filterValues = loadConfig.getFilterValues();
		
		List<Evidence> evidenceList = m_CausalityDAO.getLastPage(isSingleDesc, 
				evidenceId, filterAttributes, filterValues, loadConfig.getPageSize());
		
		return createDatePagingLoadResult(evidenceList, loadConfig);
    }
    
    
    @Override
    public DatePagingLoadResult<EvidenceModel> getNextPage(
    		CausalityEvidencePagingLoadConfig loadConfig)
    {
    	boolean isSingleDesc = loadConfig.isSingleDescription();
    	int bottomRowId = loadConfig.getRowId();
    	Date bottomRowTime = loadConfig.getTime();
    	List<String> filterAttributes = loadConfig.getFilterAttributes();
		List<String> filterValues = loadConfig.getFilterValues();
		
		List<Evidence> evidenceList = m_CausalityDAO.getNextPage(
				isSingleDesc, bottomRowId, bottomRowTime, 
				filterAttributes, filterValues, loadConfig.getPageSize());
		
		return createDatePagingLoadResult(evidenceList, loadConfig);
    }
    

    @Override
    public DatePagingLoadResult<EvidenceModel> getPreviousPage(
			CausalityEvidencePagingLoadConfig loadConfig)
    {
    	boolean isSingleDesc = loadConfig.isSingleDescription();
    	int topRowId = loadConfig.getRowId();
    	Date topRowTime = loadConfig.getTime();
    	List<String> filterAttributes = loadConfig.getFilterAttributes();
		List<String> filterValues = loadConfig.getFilterValues();
		
		List<Evidence> evidenceList = m_CausalityDAO.getPreviousPage(
				isSingleDesc, topRowId, topRowTime, 
				filterAttributes, filterValues, loadConfig.getPageSize());
		
		// If empty, load the first page - the previous button is always enabled.
		if (evidenceList == null || evidenceList.size() == 0)
		{
			evidenceList = m_CausalityDAO.getFirstPage(isSingleDesc,
					topRowId, filterAttributes, filterValues, loadConfig.getPageSize());
		}
		
		return createDatePagingLoadResult(evidenceList, loadConfig);
    }

    
    @Override
    public DatePagingLoadResult<EvidenceModel> getAtTime(
    		CausalityEvidencePagingLoadConfig loadConfig)
    {
    	boolean isSingleDesc = loadConfig.isSingleDescription();
    	int evidenceId = loadConfig.getRowId();
    	Date time = loadConfig.getTime();
		List<String> filterAttributes = loadConfig.getFilterAttributes();
		List<String> filterValues = loadConfig.getFilterValues();
		
		List<Evidence> evidenceList = m_CausalityDAO.getAtTime(isSingleDesc, 
				evidenceId, time, filterAttributes, filterValues, loadConfig.getPageSize());
		
		return createDatePagingLoadResult(evidenceList, loadConfig);
    }
	
	
	@Override
	public List<ProbableCauseModelCollection> getAggregatedProbableCauses(
    		int evidenceId, int timeSpanSecs)
    {
    	List<ProbableCause> probableCauses = 
    		m_CausalityDAO.getProbableCauses(evidenceId, timeSpanSecs, true);
    	s_Logger.debug("getAggregatedProbableCauses(" + evidenceId + 
    			") - number of probable causes to aggregate = " + probableCauses.size());
    	
    	// For time series probable causes, set a generic, localized 
    	// 'Features in xxx metric' description for aggregation.
    	Locale requestLocale = getThreadLocalRequest().getLocale();
		ResourceBundle bundle = ResourceBundle.getBundle("prelert_messages", requestLocale);
		String featuresPattern = bundle.getString("incident.description.featuresIn");
		MessageFormat featuresFormat;
    	for (ProbableCause probCause : probableCauses)
    	{
    		if (probCause.getDataSourceType().getDataCategory() == DataSourceCategory.TIME_SERIES)
    		{
    			Object[] metricArg = {probCause.getMetric()};
    			featuresFormat = new MessageFormat(featuresPattern, requestLocale);
    			probCause.setDescription(featuresFormat.format(metricArg));
    		}
    	}	
    	
    	// Aggregate probable causes by type and description.
    	CausalityDataUtilities dataFactory = CausalityDataUtilities.getInstance();
    	List<ProbableCauseCollection> collectionList = 
    		dataFactory.aggregateProbableCauses(probableCauses, evidenceId);
    	
    	// Convert to a GXT ProbableCauseModelCollection.
    	List<ProbableCauseModelCollection> aggregatedList = 
    		new ArrayList<ProbableCauseModelCollection>();
    	ProbableCauseModelCollection modelCollection;
    	for (ProbableCauseCollection collection : collectionList)
    	{
    		modelCollection = createProbableCauseModelCollection(collection);
    		aggregatedList.add(modelCollection);
    	}
    	
    	// Sort the list to work out which to display when the view opens.
    	sortForDisplay(evidenceId, aggregatedList);
    	s_Logger.debug("getAggregatedProbableCauses(" + evidenceId + ") - returning " + 
    			aggregatedList.size() + "  ProbableCauseModelCollection objects");
    	
    	return aggregatedList;
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
		
		// Sort each data type by normalised peak value and add the highest 
		// (i.e. the time series which has the highest value on the chart will be displayed).
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
	 * Creates an aggregate ProbableCauseModelCollection object from a list
	 * of ProbableCause objects, setting all the required fields.
	 * @param probableCauses list of ProbableCause model objects.
	 * @return a ProbableCauseModelCollection object.
	 */
	protected ProbableCauseModelCollection createProbableCauseModelCollection(
			ProbableCauseCollection collection)
	{
		ProbableCauseModelCollection modelCollection = 
			new ProbableCauseModelCollection();
		
		
		// Convert the ProbableCause objects to ProbableCauseModel objects,
		// and calculate the ProbableCauseModelCollection count.
		List<ProbableCause> probableCauses = collection.getProbableCauses();
		ProbableCauseModel model;
		ArrayList<ProbableCauseModel> modelList = new ArrayList<ProbableCauseModel>();
		for (ProbableCause probCause : probableCauses)
		{
			model = CausalityDataUtilities.getInstance().createProbableCauseModel(probCause);
			modelList.add(model);
		}

		model = modelList.get(0);
		modelCollection.setProbableCauses(modelList);
		modelCollection.setDataSourceType(model.getDataSourceType());
		modelCollection.setDescription(model.getDescription());
		modelCollection.setSize(collection.getSize());
		modelCollection.setCount(collection.getCount());
		modelCollection.setSourceCount(collection.getSourceCount());
		
		// For notifications, set the severity property.
		if (modelCollection.getDataSourceCategory() == DataSourceCategory.NOTIFICATION)
		{
			modelCollection.setSeverity(model.getSeverity());
		}
		
		return modelCollection;
	}
	
	
	/**
	 * Creates a DatePagingLoadResult object for the supplied list of evidence data
	 * and load criteria.
	 * @param evidenceList	list of evidence data to send back in the load result.
	 * @param loadConfig load config specifying the data that was requested.
	 * @return the DatePagingLoadResult with the requested evidence data.
	 */
	protected DatePagingLoadResult<EvidenceModel> createDatePagingLoadResult(
			List<Evidence> evidenceList, CausalityEvidencePagingLoadConfig loadConfig)
	{
		boolean isSingleDesc = loadConfig.isSingleDescription();
    	int evidenceId = loadConfig.getRowId();
		List<String> filterAttributes = loadConfig.getFilterAttributes();
		List<String> filterValues = loadConfig.getFilterValues();
		
		// Note that the list is ordered in DESCENDING order, so the first page
		// corresponds to the latest date in the database.
		Evidence startEvidence = m_CausalityDAO.getLatestEvidence(
				isSingleDesc, evidenceId, filterAttributes, filterValues);
		
		Date startDate = null;
		if (startEvidence != null)
		{
			startDate = startEvidence.getTime();
		}
		
		Evidence endEvidence = m_CausalityDAO.getEarliestEvidence(
				isSingleDesc, evidenceId, filterAttributes, filterValues);
		Date endDate = null;
		if (endEvidence != null)
		{
			endDate = endEvidence.getTime();
		}
		
		Date firstRowTime = null;
		if (evidenceList != null && evidenceList.size() > 0)
		{
			Evidence firstRow = evidenceList.get(0);
			firstRowTime = firstRow.getTime();
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
		
		// Convert Evidence to EvidenceModels
		Vector<EvidenceModel> modelList = new Vector<EvidenceModel>();
		
		for (Evidence evidence : evidenceList)
		{
			EvidenceModel model = new EvidenceModel(evidence.getProperties());
			modelList.add(model);
		}
		
		return new DatePagingLoadResult<EvidenceModel>(modelList, TimeFrame.SECOND,
				firstRowTime, startDate, endDate, isEarlierResults);
	}
    
    
    /**
     * Comparator which sorts ProbableCauseModelCollections in descending order of
     * normalised peak value i.e. the peak value as they will appear in the Causality chart.
     */
    class CollectionPeakValueComparator implements Comparator<ProbableCauseModelCollection>
    {

		@Override
        public int compare(ProbableCauseModelCollection model1,
                ProbableCauseModelCollection model2)
        {
			// Compare by peak value as they will appear on the chart.
			// ProbableCauses have already been sorted by order of magnitude, so
			// select the first item in the collection.
        	ProbableCauseModel probCause1 = model1.getProbableCause(0);
        	ProbableCauseModel probCause2 = model2.getProbableCause(0);
        	
        	double maxY1 = probCause1.getScalingFactor() * probCause1.getPeakValue();
        	double maxY2 = probCause2.getScalingFactor() * probCause2.getPeakValue();
        	
        	return Double.compare(maxY2, maxY1);
        }
    	
    }

}
