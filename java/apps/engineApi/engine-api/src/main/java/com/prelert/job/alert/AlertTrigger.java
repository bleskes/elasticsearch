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

/**
 * Simple immutable class to encapsulate the alerting options
 */
public class AlertTrigger
{

    /** If null it means it was not specified. */
    private final Double m_AnomalyThreshold;

    /** If null it means it was not specified. */
    private final Double m_NormalisedThreshold;

    private final AlertType m_AlertType;

    private final boolean m_IncludeInterim;

    public AlertTrigger(Double normlizedProbThreshold, Double anomalyThreshold, AlertType type)
    {
        m_NormalisedThreshold = normlizedProbThreshold;
        m_AnomalyThreshold = anomalyThreshold;
        m_AlertType = type;
        m_IncludeInterim = false;
    }

    public AlertTrigger(Double normlizedProbThreshold, Double anomalyThreshold,
                        AlertType type, boolean includeInterim)
    {
        m_NormalisedThreshold = normlizedProbThreshold;
        m_AnomalyThreshold = anomalyThreshold;
        m_AlertType = type;
        m_IncludeInterim = includeInterim;
    }

    public Double getAnomalyThreshold()
    {
        return m_AnomalyThreshold;
    }

    public Double getNormalisedThreshold()
    {
        return m_NormalisedThreshold;
    }

    public AlertType getAlertType()
    {
        return m_AlertType;
    }

    public boolean isIncludeInterim()
    {
        return m_IncludeInterim;
    }
}
