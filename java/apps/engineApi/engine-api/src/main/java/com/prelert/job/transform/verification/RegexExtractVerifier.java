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

import java.util.List;
import java.util.regex.Pattern;

import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.messages.Messages;
import com.prelert.job.transform.TransformConfig;

public class RegexExtractVerifier implements ArgumentVerifier
{
    @Override
    public void verify(String arg, TransformConfig tc) throws JobConfigurationException
    {
        new RegexPatternVerifier().verify(arg, tc);

        Pattern pattern = Pattern.compile(arg);
        int groupCount = pattern.matcher("").groupCount();
        List<String> outputs = tc.getOutputs();
        int outputCount = outputs == null ? 0 : outputs.size();
        if (groupCount != outputCount)
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_TRANSFORM_EXTRACT_GROUPS_SHOULD_MATCH_OUTPUT_COUNT,
                    tc.getTransform(), outputCount, arg, groupCount);
            throw new JobConfigurationException(msg, ErrorCodes.TRANSFORM_INVALID_ARGUMENT);
        }
    }
}
