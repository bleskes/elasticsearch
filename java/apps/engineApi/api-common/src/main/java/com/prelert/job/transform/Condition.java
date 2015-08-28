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
package com.prelert.job.transform;

import java.util.Objects;

/**
 * The Transform condition class.
 * Some transforms should only be applied if a condition
 * is met. One example is exclude a record if a value is
 * greater than some numeric constant.
 * The {@linkplain Operator} enum defines the available
 * comparisons a condition can use.
 */
public class Condition
{
    private Operator m_Op;
    private String m_FilterValue;

    /**
     * Operation defaults to {@linkplain Operator#NONE}
     * and the filter is an empty string
     * @param
     */
    public Condition()
    {
        m_Op = Operator.NONE;
    }

    public Condition(Operator op, String filterString)
    {
        m_Op = op;
        m_FilterValue = filterString;
    }

    public Operator getOperator()
    {
        return m_Op;
    }

    public void setOperator(Operator op)
    {
        m_Op = op;
    }

    public String getValue()
    {
        return m_FilterValue;
    }

    public void setValue(String value)
    {
        m_FilterValue = value;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((m_FilterValue == null) ? 0 : m_FilterValue.hashCode());
        result = prime * result + ((m_Op == null) ? 0 : m_Op.hashCode());
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

        Condition other = (Condition) obj;
        return Objects.equals(this.m_Op, other.m_Op) &&
                    Objects.equals(this.m_FilterValue, other.m_FilterValue);
    }
}
