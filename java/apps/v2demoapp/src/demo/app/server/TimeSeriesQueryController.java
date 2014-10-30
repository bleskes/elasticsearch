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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.servlet.http.*;

import org.apache.log4j.Logger;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

import demo.app.dao.TimeSeriesDAO;
import demo.app.data.TimeSeriesConfig;
import demo.app.data.TimeSeriesDataPoint;


/**
 * Spring MultiActionController sub-class with handles requests for time series
 * data.
 * @author Pete Harverson
 */
public class TimeSeriesQueryController extends MultiActionController
{
	static Logger logger = Logger.getLogger(TimeSeriesQueryController.class);

	private TimeSeriesDAO	m_TimeSeriesDAO;
	
	
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
	 * Initializes the given binder instance with a custom editor for Date
	 * objects.
	 * @param request the current HTTP request.
	 * @param binder new binder instance.
	 */
    protected void initBinder(HttpServletRequest request,
            ServletRequestDataBinder binder) throws Exception
    {
    	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	dateFormat.setLenient(false);
    	binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
    }


	/**
	 * Returns the data points for a time series for the given configuration.
	 * The data is aggregated according to the time span in the configuration,
	 * so that a maximum of 500 points will be returned.
	 */
	public void getDataPoints(HttpServletRequest request,
	        HttpServletResponse response, TimeSeriesConfig config)
	{
		logger.debug("getDataPoints() called for config: " + config);
		
		// Need to get the date from the request config.
		Date minTime = config.getMinTime();
		Date maxTime = config.getMaxTime();
		
		String dataType = config.getDataType();
		String metric = config.getMetric();
		String source = config.getSource();
		String attributeName = config.getAttributeName();
		String attributeValue = config.getAttributeValue();
		
		List<TimeSeriesDataPoint> dataPoints = null;
		
		// If no max time is supplied, for example, on opening the time series view from
		// the desktop for the first time, show a week's worth of data, with the
		// last day being the latest usage in the DB.
		if (maxTime == null)
		{
			maxTime = m_TimeSeriesDAO.getLatestTime(dataType, source);
			
			if (maxTime != null)
			{				
				if (minTime == null)
				{
					// If no min time is supplied, set it to be the first day
					// of the most recent week's worth of data.
					GregorianCalendar calendar = new GregorianCalendar();
					calendar.setTime(maxTime);
					calendar.add(Calendar.DAY_OF_MONTH, -6);
					minTime = calendar.getTime();
				}
			}
		}
		
		if (minTime != null && maxTime != null)
		{
			dataPoints = m_TimeSeriesDAO.getDataPointsForTimeSpan(
					dataType, metric, 
					minTime, maxTime, 
					source, attributeName, attributeValue, false);

		}
		
		// TO DO: return earliest/latest date of time series data in database.
		Date startDate = m_TimeSeriesDAO.getEarliestTime(dataType, source);
		Date endDate = m_TimeSeriesDAO.getLatestTime(dataType, source);

		try
		{
			// Set the response code and write the response data.
			response.setStatus(HttpServletResponse.SC_OK);
			
			// Try writing Object data.
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(dataPoints);
			
			out.flush();
			out.close();
			
		}
		catch (IOException e)
		{
			try
			{
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().print(e.getMessage());
				response.getWriter().close();
			}
			catch (IOException ioe)
			{
			}
		}

	}
	
}
