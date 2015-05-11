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

import com.prelert.rs.data.Bucket;

/**
 * The observer class for alerting
 *
 * Abstract class, concrete sub-classes should implement {@linkplain #fire(Bucket)}
 */
public abstract class AlertObserver
{
	private double m_AnomalyThreshold;
	private double m_NormalisedThreshold;

	public AlertObserver(double normlizedProbThreshold, double anomalyThreshold)
	{
		m_AnomalyThreshold = anomalyThreshold;
		m_NormalisedThreshold = normlizedProbThreshold;
	}

	/**
	 * Return true if the alert should be fired for these values.
	 *
	 * @param normalisedProb
	 * @param anomalyScore
	 * @return
	 */
	public boolean evaluate(double normalisedProb, double anomalyScore)
	{
		return normalisedProb >= m_NormalisedThreshold ||
				anomalyScore  >= m_AnomalyThreshold;
	}

	public boolean isAnomalyScoreAlert(double anomalyScore)
	{
		return anomalyScore >= m_AnomalyThreshold;
	}

	/**
	 * Fire the alert with the bucket the alert came from
	 *
	 * @param bucket
	 */
	public abstract void fire(Bucket bucket);


	public double getAnomalyThreshold()
	{
		return m_AnomalyThreshold;
	}

	public void setAnomalyThreshold(double anomalyThreshold)
	{
		m_AnomalyThreshold = anomalyThreshold;
	}

	public double getNormalisedProbThreshold()
	{
		return m_NormalisedThreshold;
	}

	public void setNormalisedProbThreshold(double normalisedThreshold)
	{
		m_NormalisedThreshold = normalisedThreshold;
	}
}
