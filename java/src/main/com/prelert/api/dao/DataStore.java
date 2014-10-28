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
 ************************************************************/

package com.prelert.api.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.api.data.Activity;
import com.prelert.api.data.RelatedMetric;
import com.prelert.dao.CausalityDAO;
import com.prelert.dao.EvidenceDAO;
import com.prelert.dao.IncidentDAO;
import com.prelert.data.Evidence;
import com.prelert.data.Incident;
import com.prelert.data.ProbableCause;

public class DataStore
{
	private static Logger s_Logger = Logger.getLogger(DataStore.class);

	private CausalityDAO m_CausalityDAO;	
	private IncidentDAO m_IncidentDAO;
	private EvidenceDAO m_EvidenceDAO;
	
	
	/**
	 * The causality data access object
	 * @return
	 */
	public CausalityDAO getCausalityDAO()
	{
		return m_CausalityDAO;
	}
	
	public void setCausalityDAO(CausalityDAO dao)
	{
		m_CausalityDAO = dao;
	}
	
	
	/**
	 * The incident data access object
	 * @return
	 */
	public IncidentDAO getIncidentDAO()
	{
		return m_IncidentDAO;
	}
	
	public void setIncidentDAO(IncidentDAO dao)
	{
		m_IncidentDAO = dao;
	}
	
	
	/**
	 * The evidence data access object
	 * @return
	 */
	public EvidenceDAO getEvidenceDAO()
	{
		return m_EvidenceDAO;
	}
	
	public void setEvidenceDAO(EvidenceDAO dao)
	{
		m_EvidenceDAO = dao;
	}

	
	/**
	 * Query activities using any combination of the date parameters
	 * and the optional metric path filters.
	 * 
	 * The ..IsOpen parameters means use > or < instead of >= or <=.
	 * 
	 * @param minTime. Can be <code>null</code> 
	 * @param minTimeIsOpen
	 * @param maxTime. Can be <code>null</code> 
	 * @param maxTimeIsOpen
	 * @param minFirstTime. Can be <code>null</code>
	 * @param minFirstTimeIsOpen
	 * @param maxFirstTime. Can be <code>null</code>
	 * @param maxFirstTimeIsOpen
	 * @param minLastTime. Can be <code>null</code>
	 * @param minLastTimeIsOpen
	 * @param maxLastTime. Can be <code>null</code>
	 * @param maxLastTimeIsOpen
	 * @param minUpdateTime. Can be <code>null</code>
	 * @param minUpdateTimeIsOpen
	 * @param maxUpdateTime. Can be <code>null</code>
	 * @param maxUpdateTimeIsOpen
	 * @param anomalyThreshold
	 * @param metricPath Used for an exact match on a metric path. Can be <code>null</code>
	 * @param likeMetricPath SQL like statement used to match metric paths. Can be <code>null</code>
	 * @param escapeChar SQL like statement escape character if the literals
	 * '%' or '_' are to be matched. Can be <code>null</code>
	 * 
	 * @return List of activities matching the query options.
	 */
	public List<Activity> getActivitiesRange(Date minTime, boolean minTimeIsOpen,
			Date maxTime, boolean maxTimeIsOpen,
			Date minFirstTime, boolean minFirstTimeIsOpen,
			Date maxFirstTime, boolean maxFirstTimeIsOpen,
			Date minLastTime, boolean minLastTimeIsOpen,
			Date maxLastTime, boolean maxLastTimeIsOpen,
			Date minUpdateTime, boolean minUpdateTimeIsOpen,
			Date maxUpdateTime, boolean maxUpdateTimeIsOpen,
			int anomalyThreshold, String metricPath, 
			String likeMetricPath, String escapeChar)
	{
		s_Logger.debug(String.format("getActivitiesRange(%s, %s, %s, %s, %s, %s, %s, %s, " +
				"%s, %s, %s, %s, %s, %s, %s, %s, %d, %s, %s, %s)", 
				minTime, minTimeIsOpen,
				maxTime, maxTimeIsOpen,
				minFirstTime, minFirstTimeIsOpen,
				maxFirstTime, maxFirstTimeIsOpen,
				minLastTime, minLastTimeIsOpen,
				maxLastTime, maxLastTimeIsOpen,
				minUpdateTime, minUpdateTimeIsOpen,
				maxUpdateTime, maxUpdateTimeIsOpen,
				anomalyThreshold,
				metricPath, likeMetricPath, escapeChar));
		
		List<Incident> incidents = m_IncidentDAO.getIncidentsInTimeRange(
				minTime, minTimeIsOpen,
				maxTime, maxTimeIsOpen,
				minFirstTime, minFirstTimeIsOpen,
				maxFirstTime, maxFirstTimeIsOpen,
				minLastTime, minLastTimeIsOpen,
				maxLastTime, maxLastTimeIsOpen,
				minUpdateTime, minUpdateTimeIsOpen,
				maxUpdateTime, maxUpdateTimeIsOpen,
				anomalyThreshold, metricPath,
				likeMetricPath, escapeChar);
		
		List<Activity> activities = new ArrayList<Activity>();
		for (Incident incident : incidents)
		{
			activities.add(Activity.createFromIncident(incident));
		}
		
		return activities;
	}
	
	
	/**
	 * Returns a single Activity with the top evidence Id ActivityId
	 * 
	 * @param activityId
	 * @return The activity or <code>null</code> if no activity exists.
	 */
	public Activity getActivity(int activityId)
	{
		s_Logger.debug(String.format("getActivities(%d)", activityId));
		
		Incident inc = m_IncidentDAO.getIncidentForId(activityId);
		if (inc == null)
		{
			return null;
		}
		return Activity.createFromIncident(inc);
	}
	

	/**
	 * Returns the time of the earliest Activity in Prelert
	 * @return
	 */
	public Date getEarliestActivityTime()
	{
		s_Logger.debug("getEarliestTime()"); 
		
		return m_IncidentDAO.getEarliestTime();
	}
	
	
	/**
	 * Returns the time of the latest Activity in Prelert
	 * 
	 * @return
	 */
	public Date getLatestActivityTime()
	{
		s_Logger.debug("getLatestTime()"); 
		
		return m_IncidentDAO.getLatestTime();
	}
	

	/**
	 * Returns a list of metric paths related to the Activity with 
	 * the specified Id.
	 *  
	 * @param activityId The ID of the Activity to get the probable causes for
	 * @return
	 */
	public List<RelatedMetric> getRelatedMetrics(int activityId)
	{
		s_Logger.debug(String.format("getRelatedMetrics(%d)", activityId)); 
		
		List<ProbableCause> causes = m_CausalityDAO.getProbableCauses(activityId, 0, true);
		
		List<RelatedMetric> related = new ArrayList<RelatedMetric>();
		for (ProbableCause pc : causes)
		{
			related.add(RelatedMetric.createFromProbableCause(pc));
		}
		
		return related;
	}
	
	
	/**
	 * For every item in the list of activity ids get all the 
	 * related metrics.  
	 * 
	 * @param evidenceIds Evidence Ids for each of the activities. 
	 * @return A Map of activity Id to related metrics.
	 */
	public Map<Integer, List<RelatedMetric>> getRelatedMetricsBulk(List<Integer> evidenceIds)
	{
		s_Logger.debug("getRelatedMetricsBulk()"); 
		
		Map<Integer, List<RelatedMetric>> relatedMetricByTopEvidenceId = 
			new HashMap<Integer, List<RelatedMetric>>();
		
		if (evidenceIds == null || evidenceIds.isEmpty())
		{
			return relatedMetricByTopEvidenceId;
		}

		List<ProbableCause> causes = m_CausalityDAO.getProbableCausesInBulk(evidenceIds, true);
		
		for (ProbableCause pc : causes)
		{
			List<RelatedMetric> related = relatedMetricByTopEvidenceId.get(pc.getTopEvidenceId());
			if (related == null)
			{
				related = new ArrayList<RelatedMetric>();
				relatedMetricByTopEvidenceId.put(pc.getTopEvidenceId(), related);
			}
			related.add(RelatedMetric.createFromProbableCause(pc));
		}
		
		return relatedMetricByTopEvidenceId;
	}
	
	
	/**
	 * Returns the RelatedMetric for the evidence Id.
	 *  
	 * @param evidenceId The ID of the evidence contained in the 
	 * RelatedMetric
	 * @return the RelatedMetric or <code>null</code>
	 */
	public RelatedMetric getRelatedMetric(int evidenceId)
	{
		s_Logger.debug(String.format("getRelatedMetric(%d)", evidenceId)); 
		
		Evidence ev = m_EvidenceDAO.getEvidenceSingle(evidenceId);
		if (ev == null)
		{
			return null;
		}
		
		RelatedMetric rm = RelatedMetric.createFromEvidence(ev);
		
		return rm;		
	}
	
}
