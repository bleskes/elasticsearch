/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2009     *
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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.prelert.dao.CausalityViewDAO;
import com.prelert.data.CausalityEpisode;
import com.prelert.data.CausalityEpisodeLayoutData;
import com.prelert.data.EventRecord;
import com.prelert.data.TimeFrame;
import com.prelert.data.gxt.GridRowInfo;
import com.prelert.service.CausalityQueryService;

import com.prelert.data.ProbableCause;
import com.prelert.data.gxt.ProbableCauseModel;


/**
 * Server-side implementation of the service for retrieving causality
 * information from the Prelert database.
 * @author Pete Harverson
 */
public class CausalityQueryServiceImpl extends RemoteServiceServlet 
	implements CausalityQueryService
{

	static Logger logger = Logger.getLogger(CausalityQueryServiceImpl.class);
	
	private CausalityViewDAO 	m_CausalityDAO;
	private TransactionTemplate	m_TxTemplate;
	
	
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
	 * Sets the transaction manager to be used when running queries and updates
	 * to the Prelert database within transactions.
	 * @param txManager Spring PlatformTransactionManager to manage database transactions.
	 */
	public void setTransactionManager(PlatformTransactionManager txManager)
	{
		m_TxTemplate = new TransactionTemplate(txManager);
		m_TxTemplate.setReadOnly(true);
		m_TxTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
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
		final int evId = evidenceId;
		
		// Run all the DB queries within a transaction.
		Object episodes = m_TxTemplate.execute(new TransactionCallback(){
			
			@Override
            public List<CausalityEpisode> doInTransaction(TransactionStatus status)
            {
				return m_CausalityDAO.getEpisodes(evId);
            }
		});
				
		return (List<CausalityEpisode>)episodes;
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
		List<CausalityEpisode> episodes = getEpisodes(evidenceId);
		List<CausalityEpisodeLayoutData> layouts = layoutEpisodes(episodes,
				chartWidth, chartHeight, symbolSize, minXSpacing, minYSpacing);
		
		return layouts;
	}
	
	
	/**
	 * Returns the list of probable causes for the item of evidence with the specified id.
	 * @param evidenceId the id of the item of evidence for which to obtain
	 * 	the probable causes.
	 * @return list of probable causes. This list will be empty if the item of
	 * 	evidence has no probable causes.
	 */
    public List<ProbableCauseModel> getProbableCauses(int evidenceId)
    {
    	// Time span not used - just pass 60 seconds as a dummy value.
    	List<ProbableCause> probableCauses = m_CausalityDAO.getProbableCauses(evidenceId, 60);
    	
    	List<ProbableCauseModel> modelList = createProbableCauseModelData(probableCauses);
    	return modelList;
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
		HashMap<Integer, EventRecord> allEvidence = new HashMap<Integer, EventRecord>();
		
		List<EventRecord> evidenceList;
		CausalityEpisodeLayoutData layoutData;
		
		for (CausalityEpisode episode : episodes)
    	{
    		evidenceList = episode.getEvidenceList();
    		for (EventRecord evidence : evidenceList)
    		{
    			allEvidence.put(evidence.getId(), evidence);	
    		}
    		
    		layoutData = new CausalityEpisodeLayoutData();
    		layoutData.setEpisode(episode);
    		layouts.add(layoutData);
    	}
		
		
		// Sort evidence according to time/order in episode.
		ArrayList<EventRecord> sortedEvidence = 
			new ArrayList<EventRecord>(allEvidence.values());
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
        
        HashMap<Integer, Double> allXCoords = new HashMap<Integer, Double>();
        int idx = 0;
        EventRecord nextEv;
        double gap;
        Date thisTime;
        Date nextTime;
        double xPos = minXSpacing;
		for (EventRecord thisEv : sortedEvidence)
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
    		
    		for (EventRecord evidence : evidenceList)
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
				model = new ProbableCauseModel();
				
				model.setDataSourceType(probCause.getDataSourceType());
				model.setTime(probCause.getTime());
				model.setDescription(probCause.getDescription());
				model.setSource(probCause.getSource());
				model.setSignificance(probCause.getSignificance());
				
				if (probCause.getAttributeName() != null)
				{
					model.setAttributeName(probCause.getAttributeName());
					model.setAttributeValue(probCause.getAttributeValue());
				}
				
				// attribute_label will be non-null for time series features 
				// even if there are no attributes - it has the time series feature id.
				model.setAttributeLabel(probCause.getAttributeLabel());
				
				if (probCause.getMetric() != null)
				{
					model.setMetric(probCause.getMetric());
				}
				
				modelList.add(model);
			}
		}
		
		return modelList;
		
	}
	
	
	/**
	 * Comparator class to compare two items of evidence in an episode based 
	 * on their time of occurrence.
	 */
	class EpisodeEvidenceComparator implements Comparator<EventRecord>
	{
		List<CausalityEpisode> m_Episodes;
		
		EpisodeEvidenceComparator(List<CausalityEpisode> episodes)
		{
			m_Episodes = episodes;
		}

		
        public int compare(EventRecord ev1, EventRecord ev2)
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
	        	List<EventRecord> evidenceList;
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
