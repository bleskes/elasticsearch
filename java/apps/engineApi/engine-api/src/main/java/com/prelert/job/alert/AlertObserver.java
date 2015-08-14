/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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

import com.prelert.job.results.Bucket;

/**
 * The observer class for alerting
 *
 * Abstract class, concrete sub-classes should implement {@linkplain #fire(Bucket)}
 */
public abstract class AlertObserver
{
    /** If null it means it was not specified. */
    private final Double m_AnomalyThreshold;

    /** If null it means it was not specified. */
    private final Double m_NormalisedThreshold;

    public AlertObserver(Double normlizedProbThreshold, Double anomalyThreshold)
    {
        m_AnomalyThreshold = anomalyThreshold;
        m_NormalisedThreshold = normlizedProbThreshold;
    }

    /**
     * Return true if the alert should be fired for these values.
     *
     * @param anomalyScore
     * @param normalisedProb
     * @return
     */
    public boolean evaluate(double anomalyScore, double normalisedProb)
    {
        return isGreaterOrEqual(normalisedProb, m_NormalisedThreshold)
                || isGreaterOrEqual(anomalyScore, m_AnomalyThreshold);
    }

    private static boolean isGreaterOrEqual(double value, Double threshold)
    {
        return threshold == null ? false : value >= threshold;
    }

    public boolean isAnomalyScoreAlert(double anomalyScore)
    {
        return isGreaterOrEqual(anomalyScore, m_AnomalyThreshold);
    }

    /**
     * Fire the alert with the bucket the alert came from
     *
     * @param bucket
     */
    public abstract void fire(Bucket bucket);

    public double getNormalisedProbThreshold()
    {
        return m_NormalisedThreshold == null ? 101.0 : m_NormalisedThreshold;
    }
}
