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

import com.prelert.job.DataDescription;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.messages.Messages;
import com.prelert.utils.time.DateTimeFormatterTimestampConverter;

public final class DataDescriptionVerifier
{
    private DataDescriptionVerifier()
    {
    }

    /**
     * Verify the data description configuration
     * @param dd DataDescription
     * <ol>
     * <li>Check the timeFormat - if set - is either {@value #EPOCH},
     * {@value #EPOCH_MS} or a valid format string</li>
     * <li></li>
     * </ol>
     */
    public static boolean verify(DataDescription dd) throws JobConfigurationException
    {
        if (dd.getTimeFormat() != null && dd.getTimeFormat().isEmpty() == false)
        {
            if (dd.getTimeFormat().equals(DataDescription.EPOCH) || dd.getTimeFormat().equals(DataDescription.EPOCH_MS))
            {
                return true;
            }

            try
            {
                DateTimeFormatterTimestampConverter.ofPattern(dd.getTimeFormat());
            }
            catch (IllegalArgumentException e)
            {
                String message = Messages.getMessage(Messages.JOB_CONFIG_INVALID_TIMEFORMAT,
                                                    dd.getTimeFormat());
                throw new JobConfigurationException(message,  ErrorCodes.INVALID_DATE_FORMAT, e);
            }
        }

        return true;
    }
}
