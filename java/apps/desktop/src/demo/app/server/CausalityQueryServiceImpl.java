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

package demo.app.server;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import demo.app.dao.CausalityViewDAO;
import demo.app.dao.EvidenceDAO;
import demo.app.data.CausalityEpisode;
import demo.app.data.CausalityEpisodeLayoutData;
import demo.app.data.DataSourceCategory;
import demo.app.data.DataSourceType;
import demo.app.data.ProbableCause;
import demo.app.data.Severity;
import demo.app.data.TimeFrame;
import demo.app.data.gxt.EvidenceModel;
import demo.app.data.gxt.GridRowInfo;
import demo.app.data.gxt.ProbableCauseModel;
import demo.app.data.gxt.ProbableCauseModelCollection;
import demo.app.service.CausalityQueryService;


/**
 * Server-side implementation of the service for retrieving causality data from the
 * Prelert database.
 * @author Pete Harverson
 */
public class CausalityQueryServiceImpl extends RemoteServiceServlet 
	implements CausalityQueryService
{

	static Logger logger = Logger.getLogger(CausalityQueryServiceImpl.class);
	
	private CausalityViewDAO 	m_CausalityDAO;
	private EvidenceDAO 		m_EvidenceDAO;
	
	
	/**
	 * Sets the CausalityViewDAO to be used by the causality query service.
	 * @param causalityDAO the data access object for causality views.
	 */
	public void setCausalityDAO(CausalityViewDAO causalityDAO)
	{
		m_CausalityDAO = causalityDAO;
	}
	
	
	/**
	 * Returns the CausalityViewDAO being used by the causality query service.
	 * @return the data access object for causality views.
	 */
	public CausalityViewDAO getCausalityDAO()
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
	 * Returns the list of probable cause episodes for the item of evidence with
	 * the specified id.
	 * @param evidenceId the id of the item of evidence for which to obtain
	 * 	the causality episodes.
	 * @return list of probable cause episodes.
	 */
	public List<CausalityEpisode> getEpisodes(int evidenceId)
	{	
		return m_CausalityDAO.getEpisodes(evidenceId);
	}
	
	
	/**
	 * Returns the list of probable cause episodes for the item of evidence with
	 * the specified id, encapsulated into layout data for display in a graphical
	 * chart with the specified parameters.
	 * @param evidenceId the id of the item of evidence for which to obtain
	 * 	the causality episodes.
	 * @param chartWidth width of plotting area, in pixels, for Probable Cause chart.
	 * @param chartHeight height of plotting area, in pixels, for Probable Cause chart.
	 * @param symbolSize width and height of symbol used to denote an item of evidence.
	 * @param minXSpacing minimum spacing, in pixels, between items on the x-axis.
	 * @param minYSpacing minimum spacing, in pixels, between items on the x-axis.
	 * @return list of probable cause episodes encapsulated with layout data for
	 *  placement in a chart with the specified parameters.
	 */
	public List<CausalityEpisodeLayoutData> getEpisodeLayoutData(int evidenceId,
			int chartWidth, int chartHeight, int symbolSize, int minXSpacing, int minYSpacing)
	{
		List<CausalityEpisode> episodes = m_CausalityDAO.getEpisodes(evidenceId);
		List<CausalityEpisodeLayoutData> layouts = layoutEpisodes(episodes,
				chartWidth, chartHeight, symbolSize, minXSpacing, minYSpacing);
		
		return layouts;
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
    
    
    /**
	 * Returns a list of probable causes which have been aggregated across shared data
	 * source type, time, and description values. For example:<br>
	 *  - system_udp, Mon Mar 15 2010, 'feature in packets_sent metric'<br>
	 *  which would contain a list probable cause objects for a range of servers.
	 * @param evidenceId the id of the item of evidence for which to obtain
	 * 	the aggregated probable causes.
	 * @param timeSpanSecs time span, in seconds, that is used for calculating 
	 * 	metrics for probable causes that are features in time series e.g. peak 
	 * 	value in time window of interest.
	 * @return list of ProbableCauseModelCollection objects. This list will be empty 
	 * 	if the item of evidence has no probable causes.
	 */
    public List<ProbableCauseModelCollection> getAggregatedProbableCauses(
    		int evidenceId, int timeSpanSecs)
    {
    	List<ProbableCause> probableCauses = 
    		m_CausalityDAO.getProbableCauses(evidenceId, timeSpanSecs);
    	
    	normaliseTimeSeriesData(probableCauses);
    	
    	
    	// Aggregate by type, time and then description
    	// to create a list of AggregateProbableCause objects with properties:
    	// 	- 	DataSourceType 	m_DataSourceType;
    	//	- 	Date 			m_Time;
    	//	-   String			m_Description;
    	// 	- 	List			m_ProbableCauses;
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
    	
    	// Aggregate by DataSourceType and time.
    	HashMap<DataSourceType, HashMap<Date, List<ProbableCause>>> mapByTypeTime =
    		new HashMap<DataSourceType, HashMap<Date, List<ProbableCause>>>();
    	HashMap<Date, List<ProbableCause>> mapByTime;
    	List<ProbableCause> listForDsTime;
    	Date time;
    	Iterator<DataSourceType> byTypeIter = mapByType.keySet().iterator();
    	while (byTypeIter.hasNext())
    	{
    		dsType = byTypeIter.next();
    		listForType = mapByType.get(dsType);
    		
    		mapByTime = new HashMap<Date, List<ProbableCause>>();
    		
    		for (ProbableCause probableCause : listForType)
    		{
    			dsType = probableCause.getDataSourceType();
    			
    			// Key on java.util.Date, and NOT the java.sql.Timestamp that is
    			// returned from the database since Timestamp.equals(Object) method 
    			// never returns true.
    			time = new Date(probableCause.getTime().getTime());
    			
    			listForDsTime = mapByTime.get(time);
    			if (listForDsTime == null)
        		{
    				listForDsTime = new ArrayList<ProbableCause>();
    				mapByTime.put(time, listForDsTime);
        		}
    			listForDsTime.add(probableCause);
    		}
    		
    		mapByTypeTime.put(dsType, mapByTime);
    		
    		
    		// Aggregate by DataSourceType, Time and Description.
    		HashMap<String, List<ProbableCause>> mapByDesc;
    		Iterator<Date> byTimeIter = mapByTime.keySet().iterator();
    		String desc;
    		List<ProbableCause> listForDsTimeDesc = null;
        	while (byTimeIter.hasNext())
        	{
        		time = byTimeIter.next();
        		listForDsTime = mapByTime.get(time);
        		
        		mapByDesc = new HashMap<String, List<ProbableCause>>();
        		
        		for (ProbableCause probableCause : listForDsTime)
        		{
        			desc = probableCause.getDescription();
        			
        			listForDsTimeDesc = mapByDesc.get(desc);
        			if (listForDsTimeDesc == null)
            		{
        				listForDsTimeDesc = new ArrayList<ProbableCause>();
        				mapByDesc.put(desc, listForDsTimeDesc);
            		}
        			listForDsTimeDesc.add(probableCause);
        		}
        		
        		Iterator<String> byDescIter = mapByDesc.keySet().iterator();
        		List<ProbableCauseModel> modelList;
        		ProbableCauseModel probCauseModel;
        		int id;
            	while (byDescIter.hasNext())
            	{
            		desc = byDescIter.next();
            		logger.debug("Desc: " + desc + ", number at time " + mapByDesc.get(desc).size());
            		
            		ProbableCauseModelCollection probCauseCollection = 
            			new ProbableCauseModelCollection();
            		probCauseCollection.setDataSourceType(dsType);
            		probCauseCollection.setTime(time);
            		probCauseCollection.setDescription(desc);
            		
            		modelList = createProbableCauseModelData(mapByDesc.get(desc));
            		Collections.sort(modelList, new ProbableCauseMagnitudeComparator());
            		probCauseCollection.setProbableCauses(modelList);		
            		
            		// For notifications, set the severity property.
            		if (dsType.getDataCategory() == DataSourceCategory.NOTIFICATION)
            		{
            			probCauseModel = modelList.get(0);
            			id = Integer.parseInt(probCauseModel.getAttributeValue());
            			
            			Severity severity = m_EvidenceDAO.getEvidenceSingle(id).getSeverity();
            			probCauseCollection.setSeverity(severity);
            		}
            		
            		aggregatedList.add(probCauseCollection);
            	}
        	}
    	}
    	
    	return aggregatedList;
    }


	/**
	 * Returns the details on the item of evidence with the given id from the
	 * specified probable cause episode.
	 * @param episodeId id of the episode for which to return the evidence. A value
	 * 		of 0 indicates that there is no episode associated with this item of evidence.
	 * @param evidenceId the id of the item of evidence within the specified episode
	 * 		for which episode information is being requested. This must be greater than 0.
	 * @return List of GridRowInfo objects for the item of evidence from the 
	 * 		specified episode.
	 */
	public List<GridRowInfo> getEvidenceInfo(int episodeId, int evidenceId)
	{
		return m_CausalityDAO.getEvidenceInfo(episodeId, evidenceId);
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
		
		// Determine the peak value for each time series type (e.g system_udp/packets_received).
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
		
		HashMap<String, Integer> peaksByType = 
			new HashMap<String, Integer>();
		
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
	 * Returns the maximum peak value in the supplied list of time series
	 * probable causes.
	 * @param aggregatedList list of aggregated time series probable causes.
	 * @return maximum peak value.
	 */
	protected int getMaximumPeak(List<ProbableCause> probableCauses)
	{
		int maxPeak = 0;
		
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
	protected int getMaximumPeakValue(List<ProbableCauseModelCollection> aggregatedList)
	{
		int maxPeak = 0;
		
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
	 * Lays out the episodes for display on a chart with the specified parameters.
	 * @param episodes causality episodes to lay out.
	 * @param chartWidth width of plotting area, in pixels, for Probable Cause chart.
	 * @param chartHeight height of plotting area, in pixels, for Probable Cause chart.
	 * @param symbolSize width and height of symbol used to denote an item of evidence.
	 * @param minXSpacing minimum spacing, in pixels, between items on the x-axis.
	 * @param minYSpacing minimum spacing, in pixels, between items on the x-axis.
	 * @return list of probable cause episodes encapsulated with layout data for
	 *  placement in a chart with the specified parameters.
	 */
	protected List<CausalityEpisodeLayoutData> layoutEpisodes(List<CausalityEpisode> episodes,
			int chartWidth, int chartHeight, int symbolSize, int minXSpacing, int minYSpacing)
	{	
		ArrayList<CausalityEpisodeLayoutData> layouts = 
			new ArrayList<CausalityEpisodeLayoutData>();

		// Build list of all distinct evidence across all episodes. 
		// These will be spaced out on x-axis so that there is the 
		// specified minimum gap between each item of evidence.
		HashMap<Integer, EvidenceModel> allEvidence = new HashMap<Integer, EvidenceModel>();
		
		List<EvidenceModel> evidenceList;
		CausalityEpisodeLayoutData layoutData;
		
		for (CausalityEpisode episode : episodes)
    	{
    		evidenceList = episode.getEvidenceList();
    		for (EvidenceModel evidence : evidenceList)
    		{
    			allEvidence.put(evidence.getId(), evidence);	
    		}
    		
    		layoutData = new CausalityEpisodeLayoutData();
    		layoutData.setEpisode(episode);
    		layouts.add(layoutData);
    	}
		
		
		// Sort evidence according to time/order in episode.
		ArrayList<EvidenceModel> sortedEvidence = 
			new ArrayList<EvidenceModel>(allEvidence.values());
		Collections.sort(sortedEvidence, new EpisodeEvidenceComparator(episodes));
		int numPoints = sortedEvidence.size();
		
		
		// Calculate x-axis position.
		
		// Get start and end time for x axis.	
        Date minTime = new Date();
        Date maxTime = new Date();
		try
        {
			minTime = ServerUtil.parseTimeField(sortedEvidence.get(0), TimeFrame.SECOND);
			maxTime = ServerUtil.parseTimeField(sortedEvidence.get(numPoints-1), TimeFrame.SECOND);
        }
        catch (ParseException e)
        {
        	logger.debug("layoutEpisodes() error parsing time in evidence: " + e);
        }
		
        // Calculate time span of evidence in episode.
        // If all evidence is at the same time, use nominal 1 sec time span.
        double timeSpan = Math.max(maxTime.getTime() - minTime.getTime(), 1000);
        
        double chartPixelsPerMs = (chartWidth - (2 * minXSpacing))/timeSpan;
        double spareWidth = (chartWidth-minXSpacing) - (numPoints * (symbolSize + minXSpacing));
        
        logger.debug("layoutEpisodes() evidence min/max: " + minTime + " to " + maxTime);
        logger.debug("layoutEpisodes() using time span: " + timeSpan + "ms over " + 
        		episodes.size() + " episodes");
        
        double xPos = minXSpacing;
        
        HashMap<Integer, Double> allXCoords = new HashMap<Integer, Double>();
        int idx = 0;
        EvidenceModel nextEv;
        double gap;
        Date thisTime;
        Date nextTime;
		for (EvidenceModel thisEv : sortedEvidence)
		{
			logger.debug("layoutEpisodes() Evidence id: " + thisEv.getId() + ", time: " + thisEv.get("time") + 
							", x-coord:" + xPos);
			allXCoords.put(thisEv.getId(), minTime.getTime() + xPos/chartPixelsPerMs);
			
			xPos += symbolSize;
			
			// Calculate time gap to next point.
			// Gap is the minimum x spacing plus fraction of 'spare' width in
			// proportion to the time gap to the next item of evidence in the episode.
			if (idx < (numPoints-1) )
			{
				nextEv = sortedEvidence.get(idx+1);

	            try
	            {
	            	thisTime = ServerUtil.parseTimeField(thisEv, TimeFrame.SECOND);
	            	nextTime = ServerUtil.parseTimeField(nextEv, TimeFrame.SECOND);
	            	
	            	gap = nextTime.getTime() - thisTime.getTime(); // Gap to next point in ms.
	            	xPos += (minXSpacing + (spareWidth * (gap/timeSpan)));
	            }
	            catch (ParseException e)
	            {
	            	logger.debug("layoutEpisodes() error parsing time in evidence: " + e);
	            }
			}

			idx++;
		}
		
		
		// Build HashMaps of x-coordinates vs evidence id for each Causality Episode. 
		int evidenceId;
		HashMap<Integer, Double> layoutXCoords;
		for (CausalityEpisodeLayoutData layout : layouts)
    	{
    		evidenceList = layout.getEpisode().getEvidenceList();
    		layoutXCoords = new HashMap<Integer, Double>();
    		
    		for (EvidenceModel evidence : evidenceList)
    		{
    			evidenceId = evidence.getId();
    			layoutXCoords.put(evidenceId, allXCoords.get(evidenceId));
    		}
    		
    		layout.setXCoords(layoutXCoords);
    	}
		
		
		// Calculate y-axis position for each episode.

		// Calculate min/max of y-axis so that they are rounded up/down
		// to nearest unit of 5 from first/last episode.
		double minProb = episodes.get(episodes.size() -1).getProbability();
		double maxProb = episodes.get(0).getProbability();
		int remainder = (int) (minProb % 5);
		if (remainder == 0)
		{
			minProb-=5;
		}
		else
		{
			minProb-=remainder;
		}
		
		remainder = (int)(maxProb % 5);
		if (remainder > 0)
		{
			maxProb += (5-remainder);
		}
		
		logger.debug("layoutEpisodes() probability span: " + minProb + " to " + maxProb);
		
		double probSpan = maxProb - minProb;
		double chartPixelsPerPercent = chartHeight/probSpan;
		double minGap = minYSpacing/chartPixelsPerPercent;

		
		// Fix position of first (highest probability) episode, and then space
		// out lower probability episodes so that the gap is >= minimum y spacing.
        CausalityEpisodeLayoutData firstData = layouts.get(0);
        firstData.setY(firstData.getEpisode().getProbability());
        
        double prevYPos = firstData.getY();
        double thisYPos;
        int numEpisodes = layouts.size();
        for (int i = 1; i < numEpisodes; i++)
        {
        	layoutData = layouts.get(i);
        	thisYPos = layoutData.getEpisode().getProbability();
        	
        	if (prevYPos - thisYPos < minGap)
        	{
        		thisYPos = prevYPos - minGap;
        	}
        	
        	layoutData.setY(thisYPos);	
        	prevYPos = thisYPos;
        }    
		
		return layouts;
	}
	
	
	/**
	 * Converts a list of ProbableCause objects to a list of ProbableCauseModel objects.
	 * @return list of ProbableCauseModel objects.
	 */
	protected List<ProbableCauseModel> createProbableCauseModelData(List<ProbableCause> probableCauses)
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
		model.setTime(probCause.getTime());
		model.setDescription(probCause.getDescription());
		model.setSource(probCause.getSource());
		model.setSignificance(probCause.getSignificance());
		model.setMagnitude(probCause.getMagnitude());
		
		if (probCause.getAttributeName() != null)
		{
			model.setAttributeName(probCause.getAttributeName());
			model.setAttributeValue(probCause.getAttributeValue());
		}
		
		// attribute_label will be non-null for time series features 
		// even if there are no attributes - it has the time series feature id.
		model.setAttributeLabel(probCause.getAttributeLabel());
		
		String metric = probCause.getMetric();
		if (metric != null)
		{
			// Set metric and stats for time series probable causes.
			model.setTimeSeriesTypeId(probCause.getTimeSeriesTypeId());
			model.setMetric(metric);
			model.setScalingFactor(probCause.getScalingFactor());
			model.setPeakValue(probCause.getPeakValue());
		}
		
		return model;
	}
	
	
    /**
     * Comparator which sorts ProbableCause objects in descending order of the
     * value of the magnitude property.
     */
    class ProbableCauseMagnitudeComparator implements Comparator<ProbableCauseModel>
    {
    	
        public int compare(ProbableCauseModel probCause1, ProbableCauseModel probCause2)
        {
	        double magnitude1 =  probCause1.getMagnitude();
	        double magnitude2 =  probCause2.getMagnitude();
	        
	        return (int) (magnitude2 - magnitude1);   
        }
    }
	
	
	/**
	 * Comparator class to compare two items of evidence in an episode based 
	 * on their time of occurrence.
	 */
	class EpisodeEvidenceComparator implements Comparator<EvidenceModel>
	{
		List<CausalityEpisode> m_Episodes;
		
		EpisodeEvidenceComparator(List<CausalityEpisode> episodes)
		{
			m_Episodes = episodes;
		}

		
        public int compare(EvidenceModel ev1, EvidenceModel ev2)
        {     	
	        Date time1 = new Date();
	        Date time2 = new Date();

            try
            {
            	time1 = ServerUtil.parseTimeField(ev1, TimeFrame.SECOND);
            	time2 = ServerUtil.parseTimeField(ev2, TimeFrame.SECOND);
            }
            catch (ParseException e)
            {
            	logger.debug("EpisodeEvidenceComparator error parsing time for record: " + e);
            }
	        
	        int compVal = 0;
	        
	        // First try and compare by time.
	        if (time1.before(time2))
	        {
	        	compVal = -1;
	        }
	        else if (time1.after(time2))
	        {
	        	compVal = 1;
	        }
	        else
	        {
	        	// If at the same time, look to see if they appear within an episode -
	        	//  - the DB procs return them in order by time DESC.
	        	List<EvidenceModel> evidenceList;
	        	int idx1;
	        	int idx2;
	        	for (CausalityEpisode episode : m_Episodes)
	        	{
	        		evidenceList = episode.getEvidenceList();
	        		if (evidenceList.contains(ev1) && evidenceList.contains(ev2))
	        		{      			
	        			idx1 = evidenceList.indexOf(ev1);
	        			idx2 = evidenceList.indexOf(ev2);
	        			if (idx1 < idx2)
	        			{
	        				compVal = 1;
	        			}
	        			else
	        			{
	        				compVal = -1;
	        			}

	        			break;
	        		}
	        	}
	        	
	        	// If they do not appear within an episode, just order by ID.
	        	if (compVal == 0)
	        	{        		
	        		if (ev1.getId() < ev2.getId())
	        		{
	        			compVal = -1;
	        		}
	        		else
	        		{
	        			compVal = 1;
	        		}
	        	}
	        }

	        return compVal;
        }
		
	}

}
