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
package com.prelert.job.config.verification;

import com.prelert.job.AnalysisLimits;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.messages.Messages;

public class AnalysisLimitsVerifier
{
    /**
    /**
     * Checks the limits configuration is valid
     * <ol>
     * <li>CategorizationExamplesLimit cannot be &lt 0</li>
     * </ol>
     *
     * @param al
     * @return
     * @throws JobConfigurationException
     */
    public static boolean verify(AnalysisLimits al) throws JobConfigurationException
    {
        if (al.getCategorizationExamplesLimit() != null && al.getCategorizationExamplesLimit() < 0)
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_NEGATIVE_FIELD_VALUE,
                    AnalysisLimits.CATEGORIZATION_EXAMPLES_LIMIT, al.getCategorizationExamplesLimit());
            throw new JobConfigurationException(msg, ErrorCodes.INVALID_VALUE);
        }
        return true;
    }
}
