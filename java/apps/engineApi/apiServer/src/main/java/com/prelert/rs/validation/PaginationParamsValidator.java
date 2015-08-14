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

package com.prelert.rs.validation;

import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.messages.Messages;
import com.prelert.rs.exception.InvalidParametersException;

public class PaginationParamsValidator
{
    private final int m_Skip;
    private final int m_Take;

    public PaginationParamsValidator(int skip, int take)
    {
        m_Skip = skip;
        m_Take = take;
    }

    public void validate()
    {
        if (m_Skip < 0)
        {
            throw new InvalidParametersException(Messages.getMessage(Messages.REST_INVALID_SKIP),
                    ErrorCodes.INVALID_SKIP_PARAM);
        }
        if (m_Take < 0)
        {
            throw new InvalidParametersException(Messages.getMessage(Messages.REST_INVALID_TAKE),
                    ErrorCodes.INVALID_TAKE_PARAM);
        }
    }
}
