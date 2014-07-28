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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
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

import java.util.Collections;
import java.util.List;

import com.prelert.rs.data.AnomalyRecord;

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
public class Alert 
{
	private String m_JobId;
	
	private String m_FilterName;
	
	private Severity m_Severity;	
	private double m_NormalisedScore;
	private long m_BucketTime;
	
	private List<AnomalyRecord> m_AnomalyRecords;
	
	private String m_Reason; 
	
	
	public Alert()
	{
		m_AnomalyRecords = Collections.emptyList();
	}
	
	public Alert(String jobId, String filterName, double score)
	{
		m_JobId = jobId;
		m_FilterName = filterName;
		m_NormalisedScore = score;
		m_AnomalyRecords = Collections.emptyList();
	}
	
	public String getJobId()
	{
		return m_JobId;
	}

	public void setJobId(String jobId) 
	{
		this.m_JobId = jobId;
	}

	public String getFilterName()
	{
		return m_FilterName;
	}
	
	public void setFilterName(String filterName)
	{
		m_FilterName = filterName;
	}	
	
	public Severity getSeverity() 
	{
		// TODO severity is a funtion of normalised score?
		return m_Severity;
	}

	public void setSeverity(Severity severity) 
	{
		this.m_Severity = severity;
	}

	public double getNormalisedScore() 
	{
		return m_NormalisedScore;
	}

	public void setNormalisedScore(double normalisedScore) 
	{
		this.m_NormalisedScore = normalisedScore;
	}

	/**
	 * The start time of the bucket this alert originated from. 
	 * The alert must have occured between this time and bucketTime + 
	 * bucketSpan.
	 * 
	 * @return
	 */
	public long getBucketTime() 
	{
		return m_BucketTime;
	}

	public void setBucketTime(long bucketTime) 
	{
		this.m_BucketTime = bucketTime;
	}

	public List<AnomalyRecord> getAnomalyRecords() 
	{
		return m_AnomalyRecords;
	}

	public void setAnomalyRecords(List<AnomalyRecord> anomalyRecords) 
	{
		this.m_AnomalyRecords = anomalyRecords;
	}

	public String getReason() 
	{
		return m_Reason;
	}

	public void setReason(String reason) 
	{
		this.m_Reason = reason;
	}
	
	
}
