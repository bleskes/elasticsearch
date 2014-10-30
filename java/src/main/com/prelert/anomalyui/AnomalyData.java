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

package com.prelert.anomalyui;

import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;
import com.fasterxml.jackson.annotation.*;


/**
 * Class encapsulating anomaly data at a particular time. It holds zero or more
 * <code>AnomalyRecord</code> objects and a score which is the aggregated
 * anomaly score of all its constituent records.
 * 
 * @author Pete Harverson
 */
@JsonIgnoreProperties({"revision"}) 
public class AnomalyData
{
	
	static Logger s_Logger = Logger.getLogger(AnomalyData.class);

	private Date m_Time;
	private float m_Score;
	private ArrayList<AnomalyRecord> m_Records;

	private String m_Id; // For CouchDB testing.
	
	private String m_Revision; 	// For CouchDB testing.
	private String m_Rev; 		// For CouchDB testing.
	
	private String 	m_RecordsLink;	// For Prelert API testing.
	private int 	m_RecordsCount;	// For Prelert API testing.


	/**
	 * Creates an empty item of anomaly data.
	 */
	public AnomalyData()
	{

	}
	
	
	/**
	 * Creates an item of anomaly data with the specified time and score.
	 * @param time time of the data.
	 * @param score aggregated anomaly score of all the constituent records.
	 */
	public AnomalyData(Date time, float score)
	{
		m_Time = time;
		m_Score = score;
	}


	@JsonProperty("_id")
	@JsonIgnore
	public String getId()
	{
		return m_Id;
	}


	@JsonProperty("_id")
	@JsonIgnore
	public void setId(String s)
	{
		m_Id = s;
	}


	@JsonProperty("_rev")
	@JsonIgnore
	public String getRevision()
	{
		return m_Revision;
	}


	@JsonProperty("_rev")
	@JsonIgnore
	public void setRevision(String rev)
	{
		s_Logger.info("setRevision called");
		m_Rev = rev;
	}
	
	
	@JsonProperty("rev")
	@JsonIgnore
	public String getRev()
	{
		return m_Rev;
	}


	@JsonProperty("rev")
	@JsonIgnore
	public void setRev(String rev)
	{
		s_Logger.info("setRev called");
		m_Rev = rev;
	}


	/**
	 * Sets the time of the anomaly data.
	 * @param time time of the data.
	 */
	@JsonIgnore
	public void setTime(Date time)
	{
		m_Time = time;
	}


	/**
	 * Returns the time of the anomaly data.
	 * @return time of the data.
	 */
	@JsonIgnore
	public Date getTime()
	{
		return m_Time;
	}


	/**
	 * Returns the time of the anomaly data as the number of milliseconds since
	 * 00:00 January 1 1970 GMT.
	 * @return the number of milliseconds, or -1 if the time has not been set.
	 */
	public long getTimestamp()
	{
		return (m_Time != null ? m_Time.getTime() : -1);
	}


	/**
	 * Sets the time of the anomaly data as the number of milliseconds since
	 * 00:00 January 1 1970 GMT.
	 * @param time  the number of milliseconds since 00:00 January 1 1970 GMT.
	 */
	public void setTimestamp(long time)
	{
		m_Time = new Date(time);
	}


	/**
	 * Sets the score of this anomaly data, representing the combined anomaly
	 * score of all the constituent records.
	 * @param score the score of this anomaly data.
	 */
	public void setScore(float score)
	{
		m_Score = score;
	}


	/**
	 * Returns the score of this anomaly data, representing the combined anomaly
	 * score of all the constituent records.
	 * 
	 * @return the score of this anomaly data.
	 */
	public float getScore()
	{
		return m_Score;
	}
	
	
	/**
	 * Sets the link for the REST endpoint for the anomaly records
	 * for this bucket.
	 * @param link URL for the REST endpoint.
	 */
	public void setRecordsLink(String link)
	{
		m_RecordsLink = link;
	}
	
	
	/**
	 * Returns the ink for tlhe REST endpoint for the anomaly records
	 * for this bucket.
	 * @return URL for the REST endpoint.
	 */
	public String getRecordsLink()
	{
		return m_RecordsLink;
	}
	
	
	/**
	 * Returns the number of anomaly records in this bucket.
	 * @return anomaly record count.
	 */
	public int getRecordsCount()
	{
		return m_RecordsCount;
	}


	/**
	 * Sets the number of anomaly records in this bucket.
	 * @param count anomaly record count.
	 */
	public void setRecordsCount(int count)
	{
		m_RecordsCount = count;
	}


	/**
	 * Sets the list of the constituent anomaly records.
	 * @param records list of {@link com.prelert.anomalyui.AnomalyRecord} objects.
	 */
	//@JsonIgnore
	public void setRecords(ArrayList<AnomalyRecord> records)
	{
		m_Records = records;
	}


	/**
	 * Returns the list of the constituent anomaly records.
	 * @return list of {@link com.prelert.anomalyui.AnomalyRecord} objects.
	 */
	//@JsonIgnore
	public ArrayList<AnomalyRecord> getRecords()
	{
		return m_Records;
	}


	@Override
	public String toString()
	{
		StringBuilder str = new StringBuilder();
		str.append("{timestamp=").append(m_Time).append(",score=").append(m_Score).append("}");
		return str.toString();
	}

}
