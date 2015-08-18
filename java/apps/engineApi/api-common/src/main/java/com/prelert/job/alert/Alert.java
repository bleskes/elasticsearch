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
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;

/**
 * Encapsulate an Engine API alert. Alerts have:
 * <ol>
 *  <li>Job Id - The source of the alert</li>
 *  <li>Timestamp - The time of the alert </li>
 *  <li>Bucket - The bucket that caused the alert if the alert was based on
 *  anomaly score</li>
 *  <li>Records - The records that caused the alert if the alert was based on a
 *  normalized probability threshold</li>
 * </ol>
 */
@JsonInclude(Include.NON_NULL)
public class Alert
{
	public static final String TYPE = "alert";

	public static final String JOB_ID = "JobId";
	public static final String TIMESTAMP = "timestamp";
	public static final String URI = "uri";


	private String m_JobId;
	private Date m_Timestamp;
	private URI m_Uri;
	private double m_AnomalyScore;
	private double m_MaxNormalizedProb;
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

	public double getAnomalyScore()
	{
		return m_AnomalyScore;
	}

	public void setAnomalyScore(double anomalyScore)
	{
		m_AnomalyScore = anomalyScore;
	}

	public double getMaxNormalizedProbability()
	{
		return m_MaxNormalizedProb;
	}

	public void setMaxNormalizedProbability(double prob)
	{
		m_MaxNormalizedProb = prob;
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
