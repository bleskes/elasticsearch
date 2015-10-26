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
import java.util.Objects;

/**
 * Influence field name and list of influence field values/score pairs
 */
public class Influence
{
    /**
     * Note all publicly exposed field names are "influencer" not "influence"
     */
    public static final String INFLUENCER_FIELD_NAME = "influencerFieldName";
    public static final String INFLUENCER_FIELD_VALUES = "influencerFieldValues";


    private String m_Field;
    private List<String> m_FieldValues;

    public Influence()
    {
        m_FieldValues = new ArrayList<String>();
    }

    public Influence(String field)
    {
        this();
        m_Field = field;
    }

    public String getInfluencerFieldName()
    {
        return m_Field;
    }

    public void setInfluencerFieldName(String field)
    {
        this.m_Field = field;
    }

    public List<String> getInfluencerFieldValues()
    {
        return m_FieldValues;
    }

    public void setInfluencerFieldValues(List<String> values)
    {
        this.m_FieldValues = values;
    }

    public void addInfluenceFieldValue(String value)
    {
        m_FieldValues.add(value);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_Field == null) ? 0 : m_Field.hashCode());
        result = prime * result
                + ((m_FieldValues == null) ? 0 : m_FieldValues.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj == null)
        {
            return false;
        }

        if (getClass() != obj.getClass())
        {
            return false;
        }

        Influence other = (Influence) obj;

        return Objects.equals(m_Field, other.m_Field) &&
                Objects.equals(m_FieldValues, other.m_FieldValues);
    }
}
