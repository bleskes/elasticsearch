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

package com.prelert.data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;


/**
 * Class encapsulating the properties for a causality data export, such as the
 * export file type, time zone to use for outputting times, and the IDs of the
 * notifications and time series features whose causality data is being exported.
 * @author Pete Harverson
 */
public class CausalityExportConfig implements Serializable
{
	private static final long serialVersionUID = -3841347274968842022L;
	
	private String 				m_FileType;
	private int 				m_EvidenceId = -1;
	private int					m_MetricsTimeSpanSecs;
	private long				m_MinTimeMs;
	private long				m_MaxTimeMs;
	private List<Integer>		m_NotificationIdsToShow;
	private List<Integer>		m_TimeSeriesIdsToShow;
	private String				m_Title;
	private String 				m_TimeZoneID;
	private double				m_YAxisScaling = 1d;
	
	
	/**
	 * Creates a new CausalityExportConfig object.
	 */
	public CausalityExportConfig()
	{

	}
	
	
	/**
	 * Sets the file type to which data is being exported.
	 * @param fileType export data file type - CSV or PDF are currently supported.
	 */
	public void setFileType(String fileType)
	{
		m_FileType = fileType;
	}
	
	
	/**
	 * Returns the file type to which data is being exported.
	 * @return the file extension of the the export data file type, such as
	 * 		CSV or PDF.
	 */
	public String getFileType()
	{
		return m_FileType;
	}
	
	
	/**
	 * Sets the evidence id of the notification or time series feature whose 
	 * causality data is being exported.
	 * @param id evidence id.
	 */
	public void setEvidenceId(int id)
	{
		m_EvidenceId = id;
	}
	
	
	/**
	 * Returns the evidence id of the notification or time series feature whose 
	 * causality data is being exported.
	 * @return the evidence id.
	 */
	public int getEvidenceId()
	{
		return m_EvidenceId;
	}
	
	
	/**
	 * Sets the time span, in seconds, used for calculating metrics for probable
	 * causes that are features in time series e.g. peak value in time window of
	 * interest.
	 * @param spanSecs time span, in seconds. 
	 */
	public void setMetricsTimeSpan(int spanSecs)
	{
		m_MetricsTimeSpanSecs = spanSecs;
	}
	
	
	/**
	 * Returns the time span, in seconds, used for calculating metrics for probable
	 * causes that are features in time series e.g. peak value in time window of
	 * interest.
	 * @return time span, in seconds. 
	 */
	public int getMetricsTimeSpan()
	{
		return m_MetricsTimeSpanSecs;
	}
	
	
	/**
	 * Sets the minimum time for data to be included in the export.
	 * @param minTimeMs minimum time, as the number of milliseconds since 
	 * 		January 1, 1970, 00:00:00 GMT.
	 */
	public void setMinTime(long minTimeMs)
	{
		m_MinTimeMs = minTimeMs;
	}
	
	
	/**
	 * Returns the minimum time for data to be included in the export.
	 * @return minimum time, as the number of milliseconds since 
	 * 		January 1, 1970, 00:00:00 GMT.
	 */
	public long getMinTime()
	{
		return m_MinTimeMs;
	}
	
	
	/**
	 * Sets the maximum time for data to be included in the export.
	 * @param maxTimeMs maximum time, as the number of milliseconds since 
	 * 		January 1, 1970, 00:00:00 GMT.
	 */
	public void setMaxTime(long maxTimeMs)
	{
		m_MaxTimeMs = maxTimeMs;
	}
	
	
	/**
	 * Returns the maximum time for data to be included in the export.
	 * @return maximum time, as the number of milliseconds since 
	 * 		January 1, 1970, 00:00:00 GMT.
	 */
	public long getMaxTime()
	{
		return m_MaxTimeMs;
	}
	
	
	/**
	 * Optionally sets the list of notification data that will be included in the export.
	 * @param idsToShow list of evidence ids identifying the notifications to show 
	 * 	in the export file.
	 */
	public void setShowNotifications(List<Integer> idsToShow)
	{
		m_NotificationIdsToShow = idsToShow;
	}
	
	
	/**
	 * Returns the list of notification data that will be included in the export.
	 * @return idsToShow list of evidence ids identifying the notifications to show 
	 * 	in the export file.
	 */
	public List<Integer> getShowNotifications()
	{
		return m_NotificationIdsToShow;
	}
	
	
	/**
	 * Optionally sets the list of ids of the time series that will be included in 
	 * the export.
	 * @param idsToShow list of time series ids to show in the export file.
	 */
	public void setShowSeries(List<Integer> idsToShow)
	{
		m_TimeSeriesIdsToShow = idsToShow;
	}
	
	
	/**
	 * Returns the list of the ids of the time series to include in the export.
	 * @return list of time series ids identifying the time series to show 
	 * 	in the export file.
	 */
	public List<Integer> getShowSeries()
	{
		return m_TimeSeriesIdsToShow;
	}
	
	
	/**
	 * Optionally sets the factor by which y axis values should be scaled for 
	 * plotting on a chart in the export.
	 * @param scaling factor by which values should be scaled. If not explicitly
	 * 	set, the values will be unscaled i.e. scaling = 1.
	 */
	public void setYAxisScaling(double scaling)
	{
		m_YAxisScaling = scaling;
	}
	
	
	/**
	 * Returns the factor by which y axis values should be scaled for 
	 * plotting on a chart in the export.
	 * @return scaling factor by which values should be scaled. If not explicitly
	 * 	set, the values will be unscaled i.e. scaling = 1.
	 */
	public double getYAxisScaling()
	{
		return m_YAxisScaling;
	}
	
	
	/**
	 * Sets a title to display in the export file.
	 * @param title a title to use in the export file
	 */
	public void setTitle(String title)
	{
		m_Title = title;
	}
	
	
	/**
	 * Returns the title to be displayed in the export file.
	 * @return the title for the export file.
	 */
	public String getTitle()
	{
		return m_Title;
	}
	
	
	/**
	 * Sets the ID of the time zone to use when outputting times to the export file.
	 * @param id  time zone ID.
	 */
	public void setTimeZoneID(String id)
	{
		m_TimeZoneID = id;
	}
	
	
	/**
	 * Returns the ID of the time zone being used when outputting times to the
	 * export file.
	 * @return the time zone ID.
	 */
	public String getTimeZoneID()
	{
		return m_TimeZoneID;
	}


	/**
	 * Returns a summary of this causality export configuration object.
	 * @return String representation of the CausalityExportConfig.
	 */
    @Override
    public String toString()
    {
    	StringBuilder strRep = new StringBuilder("{file type=");
		strRep.append(m_FileType);

		strRep.append(", evidenceId=");
		strRep.append(m_EvidenceId);
		
		strRep.append(", metrics time span=");
		strRep.append(m_MetricsTimeSpanSecs);
		
		strRep.append(", min time=");
		strRep.append(new Date(m_MinTimeMs));
		
		strRep.append(", max time=");
		strRep.append(new Date(m_MaxTimeMs));
		
		if (m_NotificationIdsToShow != null)
		{
			strRep.append(", notifications=");
			strRep.append(m_NotificationIdsToShow);
		}
		
		if (m_TimeSeriesIdsToShow != null)
		{
			strRep.append(", time series=");
			strRep.append(m_TimeSeriesIdsToShow);
		}
		
		if (m_YAxisScaling != 1d)
		{
			strRep.append(", y axis scaling=");
			strRep.append(m_YAxisScaling);
		}
		
		strRep.append(", title=");
		strRep.append(m_Title);
		
		if (m_TimeZoneID != null)
		{
			strRep.append(", timezone id=");
			strRep.append(m_TimeZoneID);
		}
		
		strRep.append('}');
		
		return strRep.toString();
    }
}
