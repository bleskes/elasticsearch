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
package com.prelert.transforms;

import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.job.condition.Condition;
import com.prelert.job.condition.Operator;


/**
 * Parses a numeric value from a field and compares it against a hard
 * value using a certain {@link Operator}
 */
public class ExcludeFilterNumeric extends ExcludeFilter
{
    private double m_FilterValue;

    /**
     * The condition should have been verified by now but if they are not
     * valid then the default of < (less than) and filter of 0.0 are used
     * meaning that no values are excluded.
     *
     * @param condition
     * @param readIndicies
     * @param writeIndicies
     * @param logger
     */
    public ExcludeFilterNumeric(Condition condition, List<TransformIndex> readIndicies,
            List<TransformIndex> writeIndicies, Logger logger)
    {
        super(condition, readIndicies, writeIndicies, logger);

        parseFilterValue(getCondition().getValue());
    }

    /**
     * If no condition then the default is < (less than) and filter
     * value of 0.0 are used meaning that only -ve values are excluded.
     *
     * @param readIndicies
     * @param writeIndicies
     * @param logger
     */
    public ExcludeFilterNumeric(List<TransformIndex> readIndicies,
            List<TransformIndex> writeIndicies, Logger logger)
    {
        super(new Condition(Operator.LT, "0.0"),
                    readIndicies, writeIndicies, logger);
        m_FilterValue = 0.0;
    }

    private void parseFilterValue(String fieldValue)
    {
        m_FilterValue = 0.0;
        try
        {
            m_FilterValue = Double.parseDouble(fieldValue);
        }
        catch (NumberFormatException e)
        {
            m_Logger.warn("Exclude transform cannot parse a number from field '" +
                            fieldValue + "'. Using default 0.0");
        }
    }

    /**
     * Returns {@link TransformResult#EXCLUDE} if the value should be excluded
     */
    @Override
    public TransformResult transform(String[][] readWriteArea)
    throws TransformException
    {
        TransformResult result = TransformResult.OK;
        for (TransformIndex readIndex : m_ReadIndicies)
        {
            String field = readWriteArea[readIndex.array][readIndex.index];

            try
            {
                double value = Double.parseDouble(field);

                if (getCondition().getOperator().test(value, m_FilterValue))
                {
                    result = TransformResult.EXCLUDE;
                    break;
                }
            }
            catch (NumberFormatException e)
            {

            }
        }

        return result;
    }

    public double filterValue()
    {
        return m_FilterValue;
    }
}
