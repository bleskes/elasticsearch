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

package com.prelert.job.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.DataCounts;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.exceptions.UnknownJobException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.messages.Messages;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.params.DataLoadParams;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.rs.data.ErrorCode;
import com.prelert.rs.provider.RestApiException;

public class DataStreamer
{
    private static final Logger LOGGER = Logger.getLogger(DataStreamer.class);

    /**
     * Persisted data files are named with this date format
     * e.g. Tue_22_Apr_2014_091033
     */
    private static final SimpleDateFormat PERSISTED_FILE_NAME_DATE_FORMAT =
            new SimpleDateFormat("EEE_d_MMM_yyyy_HHmmss");

    private final boolean m_ShouldPersistDataToDisk;
    private final String m_BaseDirectory;
    private final JobManager m_JobManager;

    public DataStreamer(JobManager jobManager)
    {
        // should we save uploaded data and where
        m_BaseDirectory = System.getProperty("persistbasedir");
        m_ShouldPersistDataToDisk = m_BaseDirectory != null;
        m_JobManager = Objects.requireNonNull(jobManager);
    }

    /**
     * Persists the data to disk if property 'persistbasedir' is set and streams the data to the
     * native process.
     *
     * @param headers
     * @param jobId
     * @param input
     * @param params
     * @return Count of records, fields, bytes, etc written
     *
     * @throws IOException
     * @throws UnknownJobException
     * @throws NativeProcessRunException
     * @throws MissingFieldException
     * @throws JobInUseException if the data cannot be written to because
     * the job is already handling data
     * @throws HighProportionOfBadTimestampsException
     * @throws OutOfOrderRecordsException
     * @throws TooManyJobsException If the license is violated
     * @throws MalformedJsonException If JSON data is malformed and we cannot recover
     */
    public DataCounts streamData(String contentEncoding, String jobId, InputStream input, DataLoadParams params)
            throws IOException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, TooManyJobsException, MalformedJsonException
    {
        LOGGER.debug("Handle Post data to job = " + jobId);

        input = tryDecompressingInputStream(contentEncoding, jobId, input);
        if (m_ShouldPersistDataToDisk)
        {
            input = persistDataToDisk(jobId, input);
        }
        DataCounts stats = handleStream(jobId, input, params);
        // set the bucket count to null so it doesn't appear in the output
        stats.setBucketCount(null);

        LOGGER.debug("File uploaded to job " + jobId);

        return stats;
    }

    private InputStream tryDecompressingInputStream(String contentEncoding,
            String jobId, InputStream input) throws IOException
    {
        if ("gzip".equals(contentEncoding))
        {
            LOGGER.info("Decompressing post data in job = " + jobId);
            try
            {
                return new GZIPInputStream(input);
            }
            catch (ZipException ze)
            {
                LOGGER.debug("Failed to decompress data file", ze);
                throw new RestApiException(Messages.getMessage(Messages.REST_GZIP_ERROR),
                        ErrorCode.UNCOMPRESSED_DATA,
                        Response.Status.BAD_REQUEST);
            }
        }
        return input;
    }

    private InputStream persistDataToDisk(String jobId, InputStream input)
            throws IOException
    {
        try
        {
            Files.createDirectory(FileSystems.getDefault().getPath(
                    m_BaseDirectory, jobId));
        }
        catch (FileAlreadyExistsException e)
        {
            // continue
        }

        java.nio.file.Path filePath = FileSystems.getDefault().getPath(
                m_BaseDirectory, jobId, PERSISTED_FILE_NAME_DATE_FORMAT.format(new Date()) + ".gz");

        LOGGER.info("Data will be persisted to: " + filePath);

        // Create the interceptor for writing data to disk
        // and start running in a new thread.
        final StreamingInterceptor si = new StreamingInterceptor(filePath);
        final InputStream uploadStream = input;

        input = si.createStream();

        new Thread() {
            @Override
            public void run()
            {
                si.pump(uploadStream);
            }
        }.start();
        return input;
    }

    /**
     * Pass the data stream to the native process.
     *
     * @param jobId
     * @param input
     * @param params
     * @return Count of records, fields, bytes, etc written
     *
     * @throws NativeProcessRunException If there is an error starting the native
     * process
     * @throws UnknownJobException If the jobId is not recognised
     * @throws MissingFieldException If a configured field is missing from
     * the CSV header
     * @throws JsonParseException
     * @throws JobInUseException if the data cannot be written to because
     * the job is already handling data
     * @throws HighProportionOfBadTimestampsException
     * @throws OutOfOrderRecordsException
     * @throws TooManyJobsException If the license is violated
     * @throws MalformedJsonException If JSON data is malformed and we cannot recover
     */
    private DataCounts handleStream(String jobId, InputStream input, DataLoadParams params) throws
            NativeProcessRunException, UnknownJobException, MissingFieldException,
            JsonParseException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, TooManyJobsException, MalformedJsonException
    {
        return m_JobManager.submitDataLoadJob(jobId, input, params);
    }
}
