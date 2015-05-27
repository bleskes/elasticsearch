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
package com.prelert.job.transform.condition;

import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.job.transform.exceptions.TransformConfigurationException;
import com.prelert.rs.data.ErrorCode;

/**
 * A {@linkplain TransformType} condition.
 * Some transforms should only be applied if a condition
 * is met. One example is exclude a record if a value is
 * greater than some numeric constant.
 * The {@linkplain Operation} enum defines the available
 * comparisons a condition can use.
 */
public class Condition
{
    private static final Logger LOGGER = Logger.getLogger(Condition.class);
    private Operation m_Op;
    private Double    m_FilterValue;

    /**
     * If the args can't be parsed Operation defaults to
     * {@linkplain Operation#LT} and filter value to 0.0
     * @param args
     */
    public Condition(List<String> args)
    {
        m_Op = Operation.LT;
        m_FilterValue = 0.0;
        parseArgs(args);
    }

    public Condition(Operation op, double filterValue)
    {
        m_Op = op;
        m_FilterValue = filterValue;
    }

    public static boolean verifyArguments(List<String> args)
    throws TransformConfigurationException
    {
        if (args.size() < 2)
        {
            throw new TransformConfigurationException(
                    "Not enough arguments to create a condition",
                    ErrorCode.TRANSFORM_INVALID_ARGUMENT_COUNT);
        }

        try
        {
            Operation.fromString(args.get(0));
            try
            {
                Double.parseDouble(args.get(1));
            }
            catch (NumberFormatException nfe)
            {
                throw new TransformConfigurationException(
                        "Cannot parse a double from string " + args.get(1),
                        ErrorCode.TRANSFORM_INVALID_ARGUMENT);
            }
        }
        catch (TransformConfigurationException tce)
        {
            try
            {
                // try parsing as number
                Double.parseDouble(args.get(0));
                try
                {
                    // maybe the op is the second argument
                    Operation.fromString(args.get(1));
                }
                catch (TransformConfigurationException tce2)
                {
                    // give up
                    throw new TransformConfigurationException("Cannot extract a comparison operator" +
                                "from " + args.get(0) + " or " + args.get(1),
                                ErrorCode.TRANSFORM_INVALID_ARGUMENT);
                }
            }
            catch (NumberFormatException nfe)
            {
                // give up
                throw new TransformConfigurationException(
                        "Cannot extract a comparison operator or filter value from " + args.get(0),
                        ErrorCode.TRANSFORM_INVALID_ARGUMENT);
            }
        }

        return true;
    }


    private void parseArgs(List<String> args)
    {
        final String defaults = "Using default operator less than and filter value 0.0";
        if (args.size() < 2)
        {
            LOGGER.warn("Not enough arguments for the ExcludeFilterNumeric transform, "
                    +   defaults);
            return;
        }

        try
        {
            m_Op = Operation.fromString(args.get(0));
            try
            {
                m_FilterValue = Double.parseDouble(args.get(1));
            }
            catch (NumberFormatException nfe)
            {
                LOGGER.warn("Cannot parse filter value from string " + args.get(1) +
                        ". Using default of " + m_FilterValue);
            }
        }
        catch (TransformConfigurationException tce)
        {
            try
            {
                // try parsing as number
                m_FilterValue = Double.parseDouble(args.get(0));
                try
                {
                    // maybe the op is the second argument
                    m_Op = Operation.fromString(args.get(1));
                }
                catch (TransformConfigurationException tce2)
                {
                    // give up
                    LOGGER.warn("Cannot extract a comparison operator" +
                                "from " + args.get(0) + " or " + args.get(1) + ". " + defaults);
                }
            }
            catch (NumberFormatException nfe)
            {
                // give up
                LOGGER.warn("Cannot extract a comparison operation or filter value " +
                            "from " + args.get(0) + ". " + defaults);
            }
        }
    }


    public Operation getOp()
    {
        return m_Op;
    }

    public Double getFilterValue()
    {
        return m_FilterValue;
    }

}
