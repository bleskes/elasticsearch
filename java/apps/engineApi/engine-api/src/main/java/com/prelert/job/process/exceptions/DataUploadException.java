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

package com.prelert.job.process.exceptions;

import com.prelert.job.DataCounts;

/**
 * This exception is meant to be a wrapper around RuntimeExceptions
 * that may be thrown during data upload. The exception message
 * provides info of the status of the upload up until the point
 * it failed in order to facilitate users to recover.
 */
public class DataUploadException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    private static final String MSG_FORMAT = "An error occurred after processing %d records. "
            + "(invalidDateCount = %d, missingFieldCount = %d, outOfOrderTimeStampCount = %d)";

    public DataUploadException(DataCounts dataCounts, Throwable cause)
    {
        super(String.format(MSG_FORMAT,
                dataCounts.getInputRecordCount(),
                dataCounts.getInvalidDateCount(),
                dataCounts.getMissingFieldCount(),
                dataCounts.getOutOfOrderTimeStampCount()),
                cause);
    }
}
