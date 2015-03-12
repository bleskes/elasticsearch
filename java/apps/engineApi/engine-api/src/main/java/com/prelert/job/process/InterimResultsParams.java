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

package com.prelert.job.process;

public class InterimResultsParams
{
    private final boolean m_ShouldCalculate;
    private final Long m_Start;
    private final Long m_End;

    public InterimResultsParams(boolean shouldCalculate, Long start, Long end)
    {
        m_ShouldCalculate = shouldCalculate;
        m_Start = start;
        m_End = end;
    }

    public boolean shouldCalculate()
    {
        return m_ShouldCalculate;
    }

    public String getStart()
    {
        return m_Start == null ? "" : String.valueOf(m_Start);
    }

    public String getEnd()
    {
        return m_End == null ? "" : String.valueOf(m_End);
    }
}
