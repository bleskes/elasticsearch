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
package com.prelert.job.config.verification;

import com.prelert.job.ModelDebugConfig;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.messages.Messages;

public final class ModelDebugConfigVerifier
{
    private static final double MAX_PERCENTILE = 100.0;

    private ModelDebugConfigVerifier()
    {
    }

    /**
    /**
     * Checks the ModelDebugConfig is valid
     * <ol>
     * <li>If BoundsPercentile is set it must be $gt= 0.0 and &lt 100.0</li>
     * </ol>
     * @param config
     * @return
     * @throws JobConfigurationException
     */
    public static boolean verify(ModelDebugConfig config) throws JobConfigurationException
    {
        if (config.isEnabled() &&
                (config.getBoundsPercentile() < 0.0 || config.getBoundsPercentile() > MAX_PERCENTILE))
        {
            String msg = Messages.getMessage(
                    Messages.JOB_CONFIG_MODEL_DEBUG_CONFIG_INVALID_BOUNDS_PERCENTILE);
            throw new JobConfigurationException(msg, ErrorCodes.INVALID_VALUE);
        }
        return true;
    }
}
