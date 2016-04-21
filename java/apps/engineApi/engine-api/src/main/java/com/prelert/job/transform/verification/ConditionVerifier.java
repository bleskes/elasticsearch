/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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
package com.prelert.job.transform.verification;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.messages.Messages;
import com.prelert.job.transform.Condition;
import com.prelert.job.transform.Operator;
import com.prelert.job.transform.TransformConfigurationException;

public final class ConditionVerifier
{
    private ConditionVerifier()
    {
        // Hide default constructor
    }

    /**
     * Check that the condition has an operator and the operator
     * operand is valid. In the case of numerical operators this means
     * the operand can be parsed as a number else is is a Regex match
     * so check the operand is a valid regex.
     *
     * @param condition
     * @return
     * @throws TransformConfigurationException
     */
    public static boolean verify(Condition condition)
    throws TransformConfigurationException
    {
        OperatorVerifier.verify(condition.getOperator().name());
        if (condition.getOperator() == Operator.NONE)
        {
            throw new TransformConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_TRANSFORM_CONDITION_INVALID_OPERATOR),
                    ErrorCodes.CONDITION_INVALID_ARGUMENT);
        }

        if (condition.getValue() == null)
        {
            throw new TransformConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_TRANSFORM_CONDITION_INVALID_VALUE_NULL),
                    ErrorCodes.CONDITION_INVALID_ARGUMENT);
        }

        if (condition.getOperator().expectsANumericArgument())
        {
            try
            {
                Double.parseDouble(condition.getValue());
            }
            catch (NumberFormatException nfe)
            {
                String msg = Messages.getMessage(
                        Messages.JOB_CONFIG_TRANSFORM_CONDITION_INVALID_VALUE_NUMBER, condition.getValue());
                throw new TransformConfigurationException(msg, ErrorCodes.CONDITION_INVALID_ARGUMENT);
            }
        }
        else
        {
            try
            {
                Pattern.compile(condition.getValue());
            }
            catch (PatternSyntaxException e)
            {
                String msg = Messages.getMessage(
                                Messages.JOB_CONFIG_TRANSFORM_CONDITION_INVALID_VALUE_REGEX,
                                condition.getValue());
                throw new TransformConfigurationException(msg, ErrorCodes.CONDITION_INVALID_ARGUMENT);
            }
        }

        return true;
    }

}
