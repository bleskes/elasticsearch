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

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.prelert.dao.EvidenceDAO;
import com.prelert.dao.TimeSeriesDAO;
import com.prelert.data.Attribute;
import com.prelert.data.MetricPath;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.service.TimeSeriesGXTPagingService;


/**
 * Server-side implementation of the service for retrieving time series data.
 * @author Pete Harverson
 */
@SuppressWarnings("serial")
public class TimeSeriesGXTPagingServiceImpl extends RemoteServiceServlet
        implements TimeSeriesGXTPagingService
{
	
	static Logger s_Logger = Logger.getLogger(TimeSeriesGXTPagingServiceImpl.class);
	
	private TimeSeriesDAO	m_TimeSeriesDAO;
	private EvidenceDAO 	m_EvidenceDAO;
	
	
	/**
	 * Returns the TimeSeriesDAO being used by the query service.
	 * @return the data access object for time series data.
	 */
	public TimeSeriesDAO getTimeSeriesDAO()
    {
    	return m_TimeSeriesDAO;
    }


	/**
	 * Sets the TimeSeriesDAO to be used by the query service.
	 * @param timeSeriesDAO the data access object for time series data.
	 */
	public void setTimeSeriesDAO(TimeSeriesDAO timeSeriesDAO)
    {
		m_TimeSeriesDAO = timeSeriesDAO;
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
	

	@Override
	public List<String> getAttributeValues(String dataType,
	        String attributeName, String source)
	{
		return m_TimeSeriesDAO.getAttributeValues(dataType, attributeName, source);
	}


	@Override
	public List<TimeSeriesDataPoint> getDataPoints(TimeSeriesConfig config, 
			boolean includeFeatures)
	{
		s_Logger.debug("getDataPoints() for " + config);

		// Need to get the date from the request config.
		Date minTime = config.getMinTime();
		Date maxTime = config.getMaxTime();
		
		String dataType = config.getDataType();
		String metric = config.getMetric();
		String source = config.getSource();
		List<Attribute> attributes = config.getAttributes();
		
		List<TimeSeriesDataPoint> dataPoints = null;
		
		// If no max time is supplied, for example, on opening a time series view
		// from the Explorer page for the first time, show an hour's worth of data, 
		// with the max time being the latest time stored in the DB.
		if (maxTime == null)
		{
			maxTime = m_TimeSeriesDAO.getLatestTime(dataType, source);
			if (maxTime != null)
			{				
				if (minTime == null)
				{
					// If no min time is supplied, set it to be an hour earlier
					// than the latest data point.
					minTime = new Date(maxTime.getTime() - (3600000l));
				}
			}
		}
		
		if (minTime != null && maxTime != null)
		{
			dataPoints = m_TimeSeriesDAO.getDataPointsForTimeSpan(
					dataType, metric, minTime, maxTime, 
					source, attributes, includeFeatures);
		}
		
		return dataPoints;
	}
	
	
	@Override
	public Date getLatestTime(TimeSeriesConfig config)
	{
		Date latestTime = m_TimeSeriesDAO.getLatestTime(config.getDataType(), config.getSource());
		s_Logger.debug("getLatestTime() returning " + latestTime + " for type=" + config.getDataType() + 
				", source=" + config.getSource()); 
		return latestTime;
	}
	

    @Override
    public TimeSeriesConfig getConfigurationForFeature(int id)
    {
	    return m_TimeSeriesDAO.getTimeSeriesFromFeature(id);
    }
    

    @Override
    public MetricPath getMetricPathFromTimeSeriesId(int id)
    {
    	return m_TimeSeriesDAO.getMetricPathFromTimeSeriesId(id);
    }
    

    @Override
    public MetricPath getMetricPathFromFeatureId(int evidenceId)
    {
	    return m_EvidenceDAO.getMetricPathFromEvidenceId(evidenceId);
    }
	
}
