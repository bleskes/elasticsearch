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

package com.prelert.data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Class encapsulating an incident.
 * @author Pete Harverson
 */

public class Incident implements Serializable
{
	private static final long serialVersionUID = -6129408571347743307L;

	private Date 			m_FirstTime;
	private Date 			m_Time;
	private Date 			m_LastTime;
	private Date 			m_UpdateTime;
	private String 			m_Description;
	private int				m_AnomalyScore = 1;

	private int				m_TopEvidenceId;
	private DataSourceType	m_TopDataSourceType;

	private List<Attribute>	m_SharedAttributes;
	private String			m_CommonAttributeName;
	private int				m_CommonAttributeValueCount;
	private List<String>	m_CommonFieldTopValues;

	private int				m_NotificationCount;
	private int				m_NotificationTypeCount;
	private int				m_TimeSeriesCount;
	private int				m_TimeSeriesTypeCount;
	private int				m_SourceCount;


	/**
	 * Creates a new, blank incident.
	 */
	public Incident()
	{

	}


	/**
	 * Returns the earliest evidence time for this incident.
	 * @return the earliest evidence time for this incident.
	 */
	public Date getFirstTime()
	{
		return m_FirstTime;
	}


	/**
	 * Sets the earliest evidence time for this incident.
	 * @param time the earliest evidence time for this incident.
	 */
	public void setFirstTime(Date firstTime)
	{
		m_FirstTime = firstTime;
	}


	/**
	 * Returns the time of the incident.
	 * @return the time of the incident.
	 */
	public Date getTime()
	{
		return m_Time;
	}


	/**
	 * Sets the time of this incident.
	 * @param time	the time of the incident.
	 */
	public void setTime(Date time)
	{
		m_Time = time;
	}


	/**
	 * Returns the latest evidence time for this incident.
	 * @return the latest evidence time for this incident.
	 */
	public Date getLastTime()
	{
		return m_LastTime;
	}


	/**
	 * Sets the latest evidence time for this incident.
	 * @param time the latest evidence time for this incident.
	 */
	public void setLastTime(Date lastTime)
	{
		m_LastTime = lastTime;
	}


	/**
	 * Returns the wall clock time when the incident was last updated.
	 * @return the wall clock time when the incident was last updated.
	 */
	public Date getUpdateTime()
	{
		return m_UpdateTime;
	}


	/**
	 * Sets the wall clock time when the incident was last updated.
	 * @param time the wall clock time when the incident was last updated.
	 */
	public void setUpdateTime(Date updateTime)
	{
		m_UpdateTime = updateTime;
	}


	/**
	 * Returns the description of the incident.
	 * @return the m_Description
	 */
	public String getDescription()
	{
		return m_Description;
	}


	/**
	 * Sets the description of this incident.
	 * @param description description of the incident.
	 */
	public void setDescription(String description)
	{
		m_Description = description;
	}


	/**
	 * Returns the anomaly score of the incident, a value between 1 and 100.
	 * This represents the probability of the incident occurring within the time
	 * frame that is being considered.
	 * @return the anomaly score, between 1 and 100.
	 */
	public int getAnomalyScore()
	{
		return m_AnomalyScore;
	}


	/**
	 * Sets the anomaly score. This represents the probability of the incident
	 * occurring within the time frame that is being considered.
	 * @param anomalyScore the anomaly score, between 1 and 100.
	 */
	public void setAnomalyScore(int anomalyScore)
	{
		m_AnomalyScore = anomalyScore;
	}


	/**
	 * Returns the ID of the 'headline' item of evidence in the incident.
	 * Note that for recently created incidents, the headline evidence might
	 * change before requesting for the list of causality data.
	 * @return the ID of the 'headline' notification or time series feature in the incident.
	 */
	public int getTopEvidenceId()
	{
		return m_TopEvidenceId;
	}


	/**
	 * Sets the ID of the 'headline' item of evidence in the incident.
	 * @param evidenceId the ID of the 'headline' notification or time series
	 * 	feature in the incident.
	 */
	public void setTopEvidenceId(int evidenceId)
	{
		m_TopEvidenceId = evidenceId;
	}


	/**
	 * Returns the data source type of the top 'headline' notification or time
	 * series feature in the incident.
	 * @return the data source type of the headline notification or time series
	 * 	feature e.g. system_udp or win_event.
	 */
	public DataSourceType getTopDataSourceType()
	{
		return m_TopDataSourceType;
	}


	/**
	 * Sets the data source type of the top 'headline' notification or time
	 * series feature in the incident e.g. system_udp or win_event.
	 * @param dataSourceType the data source type of the headline notification
	 * 	or time series feature e.g. system_udp or win_event.
	 */
	public void setTopDataSourceType(DataSourceType dataSourceType)
	{
		m_TopDataSourceType = dataSourceType;
	}


	/**
	 * Returns the list of attributes that are the same for every piece of
	 * evidence within the incident.
	 * @return the list of shared attributes.
	 */
	public List<Attribute> getSharedAttributes()
	{
		return m_SharedAttributes;
	}


	/**
	 * Sets the list of attributes that are the same for every piece of
	 * evidence within the incident.
	 * @param attributes the list of shared attributes.
	 */
	public void setSharedAttributes(List<Attribute> attributes)
	{
		m_SharedAttributes = attributes;
	}


	/**
	 * Returns the name of the attribute, excluding type, that exhibits most
	 * commonality among the evidence within the activity, but which is not the
	 * same for every item of evidence within the activity.
	 * @return the name of the attribute exhibiting most commonality.
	 */
	public String getCommonAttributeName()
	{
		return m_CommonAttributeName;
	}


	/**
	 * Sets the name of the attribute, excluding type, that exhibits most
	 * commonality among the evidence within the activity, but which is not the
	 * same for every item of evidence within the activity.
	 * @param attributeName the name of the attribute exhibiting most commonality.
	 */
	public void setCommonAttributeName(String attributeName)
	{
		m_CommonAttributeName = attributeName;
	}


	/**
	 * Returns the number of distinct values for the attribute that exhibits most
	 * commonality.
	 * @return the number of distinct values.
	 */
	public int getCommonAttributeValueCount()
	{
		return m_CommonAttributeValueCount;
	}


	/**
	 * Sets the number of distinct values for the attribute that exhibits most
	 * commonality.
	 * @param count the number of distinct values.
	 */
	public void setCommonAttributeValueCount(int count)
	{
		m_CommonAttributeValueCount = count;
	}


	/**
	 * Returns the three most common values for the attribute that exhibits most
	 * commonality (up to a maximum of three depending on number of distinct values).
	 * @return the most common attribute values (maximum of 3).
	 */
	public List<String> getCommonFieldTopValues()
	{
		return m_CommonFieldTopValues;
	}


	/**
	 * Sets the most common values for the attribute that exhibits most
	 * commonality (should not exceed a maximum of three depending on number
	 * of distinct values).
	 * @param topValues the most common attribute values (maximum of 3).
	 */
	public void setCommonFieldTopValues(List<String> topValues)
	{
		m_CommonFieldTopValues = topValues;
	}


	/**
	 * Returns the total count of notifications in the incident.
	 * @return the count of notifications in the incident.
	 */
	public int getNotificationCount()
	{
		return m_NotificationCount;
	}


	/**
	 * Sets the total count of notifications in the incident.
	 * @param count the count of notifications in the incident.
	 */
	public void setNotificationCount(int count)
	{
		m_NotificationCount = count;
	}


	/**
	 * Returns the count of distinct notification data types in the activity.
	 * @return the number of notification data types in the incident.
	 */
	public int getNotificationTypeCount()
	{
		return m_NotificationTypeCount;
	}


	/**
	 * Sets the count of distinct notification data types in the incident.
	 * @param count the number of notification data types in the incident.
	 */
	public void setNotificationTypeCount(int count)
	{
		m_NotificationTypeCount = count;
	}


	/**
	 * Returns the count of distinct time series in the incident (multiple
	 * features in the same time series are only counted once).
	 * @return the number of time series features in the incident.
	 */
	public int getTimeSeriesCount()
	{
		return m_TimeSeriesCount;
	}


	/**
	 * Sets the count of distinct time series in the incident (multiple
	 * features in the same time series are only counted once).
	 * @param count the number of time series features in the incident.
	 */
	public void setTimeSeriesCount(int count)
	{
		m_TimeSeriesCount = count;
	}


	/**
	 * Returns the count of distinct time series data types in the incident.
	 * @return the number of time series data types in the incident.
	 */
	public int getTimeSeriesTypeCount()
	{
		return m_TimeSeriesTypeCount;
	}


	/**
	 * Sets the count of distinct time series data types in the incident.
	 * @param count the number of time series data types in the incident.
	 */
	public void setTimeSeriesTypeCount(int count)
	{
		m_TimeSeriesTypeCount = count;
	}


	/**
	 * Returns count of distinct sources in the incident.
	 * @return the number of sources in the incident.
	 */
	public int getSourceCount()
	{
		return m_SourceCount;
	}


	/**
	 * Sets the count of distinct sources in the incident.
	 * @param count the number of sources in the incident.
	 */
	public void setSourceCount(int count)
	{
		m_SourceCount = count;
	}


	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
		{
			return false;
		}

		if (this == obj)
		{
			return true;
		}

		if ((obj instanceof Incident) == false)
		{
			return false;
		}

		Incident other = (Incident)obj;

		// Compare the time, description, anomaly score and evidence id.
		// The other fields are just used to build an internationalised
		// incident description for the client.
		boolean result = m_Time.equals(other.getTime());
		result = result && (m_Description.equals(other.getDescription()));
		result = result && (m_AnomalyScore == other.getAnomalyScore());
		result = result && (m_TopEvidenceId == other.getTopEvidenceId());

		return result;
	}


	/**
	 * Returns a summary of this incident.
	 * @return String representation of this incident.
	 */
	public String toString()
	{
		StringBuilder strRep = new StringBuilder('{');

		strRep.append("time=").append(m_Time);
		strRep.append(", description=").append(m_Description);
		strRep.append(", anomaly score=").append(m_AnomalyScore);
		strRep.append(", top evidence id=").append(m_TopEvidenceId);
		strRep.append(", top data type=").append(m_TopDataSourceType);
		if (m_SharedAttributes != null)
		{
			strRep.append(", shared attributes=").append(m_SharedAttributes);
		}
		strRep.append(", common attribute=").append(m_CommonAttributeName);
		strRep.append(", common attribute values=").append(m_CommonAttributeValueCount);
		strRep.append(", top common values=").append(m_CommonFieldTopValues);

		strRep.append(", notifications=").append(m_NotificationCount);
		strRep.append(", notification types=").append(m_NotificationTypeCount);
		strRep.append(", time series=").append(m_TimeSeriesCount);
		strRep.append(", time series types=").append(m_TimeSeriesTypeCount);
		strRep.append(", sources=").append(m_SourceCount);

		strRep.append('}');

		return strRep.toString();
	}
}
