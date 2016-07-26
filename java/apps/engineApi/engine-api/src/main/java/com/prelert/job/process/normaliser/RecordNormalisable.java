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

package com.prelert.job.process.normaliser;

import java.util.Objects;

import com.prelert.job.results.AnomalyRecord;

class RecordNormalisable extends AbstractLeafNormalisable
{
    private final AnomalyRecord m_Record;

    public RecordNormalisable(AnomalyRecord record)
    {
        m_Record = Objects.requireNonNull(record);
    }

    @Override
    public Level getLevel()
    {
        return Level.LEAF;
    }

    @Override
    public String getPartitionFieldName()
    {
        return m_Record.getPartitionFieldName();
    }

    @Override
    public String getPartitionFieldValue()
    {
        return m_Record.getPartitionFieldValue();
    }

    @Override
    public String getPersonFieldName()
    {
        String over = m_Record.getOverFieldName();
        return over != null ? over : m_Record.getByFieldName();
    }

    @Override
    public String getFunctionName()
    {
        return m_Record.getFunction();
    }

    @Override
    public String getValueFieldName()
    {
        return m_Record.getFieldName();
    }

    @Override
    public double getProbability()
    {
        return m_Record.getProbability();
    }

    @Override
    public double getNormalisedScore()
    {
        return m_Record.getNormalizedProbability();
    }

    @Override
    public void setNormalisedScore(double normalisedScore)
    {
        m_Record.setNormalizedProbability(normalisedScore);
    }

    @Override
    public void setParentScore(double parentScore)
    {
        m_Record.setAnomalyScore(parentScore);
    }

    @Override
    public void resetBigChangeFlag()
    {
        m_Record.resetBigNormalisedUpdateFlag();
    }

    @Override
    public void raiseBigChangeFlag()
    {
        m_Record.raiseBigNormalisedUpdateFlag();
    }
}
