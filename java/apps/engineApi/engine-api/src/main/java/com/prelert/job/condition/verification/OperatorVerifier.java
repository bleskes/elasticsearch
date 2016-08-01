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
package com.prelert.job.condition.verification;

import com.prelert.job.condition.Operator;
import com.prelert.job.condition.UnknownOperatorException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.messages.Messages;

public final class OperatorVerifier
{
    private OperatorVerifier()
    {
        // Hide default constructor
    }

    /**
     * Checks that the <code>name</code> string is a string
     * value of an Operator enum
     * @param name
     * @return
     * @throws JobConfigurationException
     */
    public static boolean verify(String name) throws JobConfigurationException
    {
        try
        {
            Operator.fromString(name);
        }
        catch (UnknownOperatorException e)
        {
            throw new JobConfigurationException(
                Messages.getMessage(Messages.JOB_CONFIG_CONDITION_UNKNOWN_OPERATOR, name),
                ErrorCodes.UNKNOWN_OPERATOR);
        }

        return true;
    }
}
