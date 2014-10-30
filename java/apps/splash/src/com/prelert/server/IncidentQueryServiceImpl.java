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
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.prelert.dao.IncidentDAO;
import com.prelert.data.Incident;
import com.prelert.service.IncidentQueryService;


/**
 * Server-side implementation of the service for retrieving incident data from the
 * Prelert database.
 * @author Pete Harverson
 */
public class IncidentQueryServiceImpl extends RemoteServiceServlet
	implements IncidentQueryService
{
	static Logger logger = Logger.getLogger(IncidentQueryServiceImpl.class);

	private IncidentDAO	m_IncidentDAO;
	
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
    	logger.debug("TimelineAutoRefreshFrequency set to " + frequency);
    	m_TimelineAutoRefreshFrequency = frequency;
    }


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
	{
		/*
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		logger.debug("Principal is " + principal);
		if (principal instanceof UserDetails)
		{
			String username = ((UserDetails) principal).getUsername();
		}
		else
		{
			String username = principal.toString();
		}
		*/
		
		List<Incident> incidents = null;
		
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
			incidents = m_IncidentDAO.getIncidents(qryMinTime, qryMaxTime, anomalyThreshold);
			
			// Sort by descending anomaly score.
			Collections.sort(incidents, new AnomalyScoreComparator());
		}
		else
		{
			// No incident data.
			incidents = new ArrayList<Incident>();
		}
		
		return incidents;
	}
	
	
	/**
	 * Returns the date/time of the earliest incident in the database.
	 * @return date/time of earliest incident.
	 */
	public Date getEarliestTime()
	{
		return m_IncidentDAO.getEarliestTime();
	}
	
	
	/**
	 * Returns the date/time of the latest incident in the database.
	 * @return date/time of latest incident.
	 */
	public Date getLatestTime()
	{
		return m_IncidentDAO.getLatestTime();
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
}
