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
package com.prelert.job.results;

import java.util.ArrayList;
import java.util.List;

/**
 * Influence field name and list of influence field values/score pairs
 */
public class Influence
{
    public static final String INFLUENCE_FIELD = "influenceField";
    public static final String INFLUENCE_FIELD_VALUE = "fieldValue";
    public static final String SCORE = "score";
    public static final String INFLUENCE_SCORES = "influenceScores";

    private String m_Field;
    private List<InfluenceScore> m_Scores;

    public Influence()
    {
        m_Scores = new ArrayList<InfluenceScore>();
    }

    public Influence(String field)
    {
        this();
        m_Field = field;
    }

    public String getInfluenceField()
    {
        return m_Field;
    }

    public void setInfluenceField(String field)
    {
        this.m_Field = field;
    }

    public List<InfluenceScore> getInfluenceScores()
    {
        return m_Scores;
    }

    public void setInfluenceScores(List<InfluenceScore> scores)
    {
        this.m_Scores = scores;
    }

    public void addInfluenceScore(InfluenceScore score)
    {
        m_Scores.add(score);
    }
}
