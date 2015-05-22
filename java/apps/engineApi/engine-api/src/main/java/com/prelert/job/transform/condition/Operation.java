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

import java.util.EnumSet;
import java.util.Set;

import com.prelert.job.transform.TransformConfigurationException;
import com.prelert.rs.data.ErrorCode;

/**
 * Enum representing logical comparisons on doubles
 */
public enum Operation
{
    EQ
    {
        @Override
        public boolean test(double lhs, double rhs)
        {
            return Double.compare(lhs, rhs) == 0;
        }
    },
    GT
    {
        @Override
        public boolean test(double lhs, double rhs)
        {
            return Double.compare(lhs, rhs) > 0;
        }
    },
    GTE
    {
        @Override
        public boolean test(double lhs, double rhs)
        {
            return Double.compare(lhs, rhs) >= 0;
        }
    },
    LT
    {
        @Override
        public boolean test(double lhs, double rhs)
        {
            return Double.compare(lhs, rhs) < 0;
        }
    },
    LTE
    {
        @Override
        public boolean test(double lhs, double rhs)
        {
            return Double.compare(lhs, rhs) <= 0;
        }
    };

    public boolean test(double lhs, double rhs)
    {
        return false;
    }

    public boolean expectsANumericArgument()
    {
        return true;
    }

    public static Operation fromString(String name) throws TransformConfigurationException
    {
        Set<Operation> all = EnumSet.allOf(Operation.class);

        String ucName = name.toUpperCase();
        for (Operation type : all)
        {
            if (type.toString().equals(ucName))
            {
                return type;
            }
        }

        throw new TransformConfigurationException(
                                "Unknown operation '" + name + "'",
                                ErrorCode.UNKNOWN_TRANSFORM);
    }

 };
