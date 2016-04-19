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

package com.prelert.job.scheduler;

import java.io.InputStream;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.DataCounts;
import com.prelert.job.UnknownJobException;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.LicenseViolationException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.params.DataLoadParams;
import com.prelert.job.process.params.InterimResultsParams;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;

public interface DataProcessor
{
    /**
     * Passes data to the native process. If the process is not running a new
     * one is started.
     * This is a blocking call that won't return until all the data has been
     * written to the process. A new thread is launched to parse the process's
     * output
     *
     * @param jobId
     * @param input
     * @param params
     * @return
     * @throws NativeProcessRunException If there is an error starting the native
     * process
     * @throws UnknownJobException If the jobId is not recognised
     * @throws MissingFieldException If a configured field is missing from
     * the CSV header
     * @throws JsonParseException
     * @throws JobInUseException if the job cannot be written to because
     * it is already handling data
     * @throws HighProportionOfBadTimestampsException
     * @throws OutOfOrderRecordsException
     * @throws LicenseViolationException If the license is violated
     * @throws TooManyJobsException If too many jobs for the number of CPU cores
     * @throws MalformedJsonException If JSON data is malformed and we cannot recover
     * @return Count of records, fields, bytes, etc written
     */
    DataCounts submitDataLoadJob(String jobId, InputStream input, DataLoadParams params)
            throws UnknownJobException, NativeProcessRunException, MissingFieldException,
            JsonParseException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, LicenseViolationException, TooManyJobsException,
            MalformedJsonException;

    /**
     * Flush the running job, ensuring that the native process has had the
     * opportunity to process all data previously sent to it with none left
     * sitting in buffers.
     *
     * @param jobId The job to flush
     * @param interimResultsParams Parameters about whether interim results calculation
     * should occur and for which period of time
     * @throws UnknownJobException
     * @throws NativeProcessRunException
     * @throws JobInUseException if a data upload is part way through
     */
    void flushJob(String jobId, InterimResultsParams interimResultsParams)
            throws UnknownJobException, NativeProcessRunException, JobInUseException;

    /**
     * Stop the running job and mark it as finished.<br>
     *
     * @param jobId The job to stop
     * @throws UnknownJobException
     * @throws NativeProcessRunException
     * @throws JobInUseException if the job cannot be closed because data is
     * being streamed to it
     */
    void closeJob(String jobId) throws UnknownJobException, NativeProcessRunException,
            JobInUseException;
}

