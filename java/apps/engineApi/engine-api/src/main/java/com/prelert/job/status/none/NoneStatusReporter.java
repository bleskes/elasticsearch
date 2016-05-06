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
 *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/
package com.prelert.job.status.none;

import org.apache.log4j.Logger;

import com.prelert.job.persistence.none.NoneJobDataCountsPersister;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.job.status.StatusReporter;
import com.prelert.job.usage.none.NoneUsageReporter;

public class NoneStatusReporter extends StatusReporter
{
    private static final Logger LOGGER = Logger.getLogger(NoneStatusReporter.class);

    public NoneStatusReporter(String jobId)
    {
        super(jobId, new NoneUsageReporter(), new NoneJobDataCountsPersister(), LOGGER);
    }

    /**
     * Overrides the base class to ignore problems with bad dates, out of order
     * data, etc.
     *
     * @param totalRecords
     * @throws HighProportionOfBadTimestampsException
     * @throws OutOfOrderRecordsException
     */
    @Override
    protected void checkStatus(long totalRecords)
    throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    {
        // Don't throw exceptions for these conditions as we're supposed to be
        // not reporting any status
    }
}
