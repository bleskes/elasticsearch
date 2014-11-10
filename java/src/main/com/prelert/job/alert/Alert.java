/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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
package com.prelert.job.alert;

import java.net.URI;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;

/**
 * Encapsulate an Engine API alert. Alerts have:
 * <ol>
 *  <li>Job Id - The source of the alert</li>
 *  <li>Filter - The normalisation filter</li>
 *  <li>Severity - See {@linkplain Severity}</li>
 *  <li>Normalised Score - The normalised anomaly score</li>
 *  <li>Bucket Time - The time of the alert </li>
 *  <li>Anomaly Records - The anomalies that caused the alert</li>
 *  <li>Reason - </li>
 * </ol>
 */
@JsonInclude(Include.NON_NULL)
public class Alert
{
	static final public String TYPE = "alert";

	static final public String ID = "id";
	static final public String JOB_ID = "JobId";
	static final public String TIMESTAMP = "timestamp";
	static final public String URI = "uri";


	private String m_AlertId;
	private String m_JobId;
	private Date m_Timestamp;
	private URI m_Uri;
	private double m_AnomalyScore;
	private double m_NormalizedProb;
	private boolean m_IsTimeout;
	private Bucket m_Bucket;
	private List<AnomalyRecord> m_Records;


	public String getJobId()
	{
		return m_JobId;
	}

	public void setJobId(String jobId)
	{
		this.m_JobId = jobId;
	}

	public Date getTimestamp()
	{
		return m_Timestamp;
	}

	public void setTimestamp(Date timestamp)
	{
		m_Timestamp = timestamp;
	}

	public String getId()
	{
		return m_AlertId;
	}

	public void setId(String value)
	{
		m_AlertId = value;
	}

	public double getAnomalyScore()
	{
		return m_AnomalyScore;
	}

	public void setAnomalyScore(double anomalyScore)
	{
		m_AnomalyScore = anomalyScore;
	}

	public double getNormalizedProbability()
	{
		return m_NormalizedProb;
	}

	public void setNormalizedProbability(double prob)
	{
		m_NormalizedProb = prob;
	}

	public URI getUri()
	{
		return m_Uri;
	}

	public void setUri(URI uri)
	{
		m_Uri = uri;
	}

	public boolean isTimeout()
	{
		return m_IsTimeout;
	}

	public void setTimeout(boolean timeout)
	{
		m_IsTimeout = timeout;
	}

	public Bucket getBucket()
	{
		return m_Bucket;
	}

	public void setBucket(Bucket bucket)
	{
		m_Bucket = bucket;
	}

	public List<AnomalyRecord> getRecords()
	{
		return m_Records;
	}

	public void setRecords(List<AnomalyRecord> records)
	{
		m_Records = records;
	}

}
