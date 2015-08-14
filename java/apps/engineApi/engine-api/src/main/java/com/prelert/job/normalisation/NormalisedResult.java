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

package com.prelert.job.normalisation;

/**
 * Store the output of the normaliser process
 *
 * {"rawScore":"0.0","normalizedScore":"0"}
 *
 */
public class NormalisedResult
{
    public static final String RAW_SCORE = "rawScore";
    public static final String NORMALIZED_SCORE = "normalizedScore";

    private double m_RawScore;
    private double m_NormalizedScore;

    public double getRawScore()
    {
        return m_RawScore;
    }

    public void setRawScore(double rawScore)
    {
        this.m_RawScore = rawScore;
    }

    public double getNormalizedScore()
    {
        return m_NormalizedScore;
    }

    public void setNormalizedScore(double normalizedScore)
    {
        this.m_NormalizedScore = normalizedScore;
    }
}
