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

import java.util.Date;


/**
 * Class encapsulating the data model for an anomaly.
 * @author Pete Harverson
 */
public class AnomalyRecord
{
	private Date	m_Time;
	private String	m_FieldName;
	private String	m_FieldValue;
	private float	m_AnomalyFactor;
	private float 	m_Probability;
	private String	m_MetricField;
	private float	m_CurrentMean;
	private float	m_BaselineMean;


	/**
	 * Returns the time of the anomaly.
	 * @return anomaly time.
	 */
	public Date getTime()
	{
		return m_Time;
	}


	/**
	 * Sets the time of the anomaly.
	 * @param time anomaly time.
	 */
	public void setTime(Date time)
	{
		m_Time = time;
	}


	/**
	 * Returns the name of the field found to be anomalous.
	 * @return the field name.
	 */
	public String getFieldName()
	{
		return m_FieldName;
	}


	/**
	 * Sets the name of the field found to be anomalous.
	 * @param fieldName the field name.
	 */
	public void setFieldName(String fieldName)
	{
		m_FieldName = fieldName;
	}


	/**
	 * Returns the value of the field found to be anomalous.
	 * @return the field value.
	 */
	public String getFieldValue()
	{
		return m_FieldValue;
	}


	/**
	 * Sets the value of the field found to be anomalous.
	 * @param fieldValue the field value.
	 */
	public void setFieldValue(String fieldValue)
	{
		m_FieldValue = fieldValue;
	}
	
	
	/**
	 * Returns the anomaly factor, with a higher score representing more anomalous records.
	 * @return the anomaly factor.
	 */
	public float getAnomalyFactor()
	{
		return m_AnomalyFactor;
	}

	
	/** 
	 * Sets the anomaly factor, with a higher score representing more anomalous records.
	 * @param anomalyFactor the anomaly factor.
	 */
	public void setAnomalyFactor(float anomalyFactor)
	{
		m_AnomalyFactor = anomalyFactor;
	}


	/**
	 * Returns the probability of this anomaly, with a lower value representing 
	 * more anomalous records.
	 * @return the probability, a percentage value between 0 and 100.
	 */
	public float getProbability()
	{
		return m_Probability;
	}


	/**
	 * Sets the probability of this anomaly, with a lower value representing 
	 * more anomalous records.
	 * @param probability the probability, a percentage value between 0 and 100.
	 */
	public void setProbability(float probability)
	{
		m_Probability = probability;
	}

	
	/**
	 * Returns the name of the metric field for this anomaly.
	 * @return the metric field name.
	 */
	public String getMetricField()
	{
		return m_MetricField;
	}

	
	/**
	 * Sets the name of the metric field for this anomaly.
	 * @param metricField the metric field name.
	 */
	public void setMetricField(String metricField)
	{
		m_MetricField = metricField;
	}

	
	/**
	 * Returns the currently observed mean value of the data.
	 * @return the current mean.
	 */
	public float getCurrentMean()
	{
		return m_CurrentMean;
	}

	
	/**
	 * Sets the currently observed mean value of the data. 
	 * @param mean the current mean.
	 */
	public void setCurrentMean(float mean)
	{
		m_CurrentMean = mean;
	}

	
	/**
	 * Returns the baseline mean value of the data.
	 * @return the baseline mean value.
	 */
	public float getBaselineMean()
	{
		return m_BaselineMean;
	}

	
	/**
	 * Sets the baseline mean value of the data.
	 * @param mean the baseline mean value.
	 */
	public void setBaselineMean(float mean)
	{
		m_BaselineMean = mean;
	}
	
}
