/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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

package com.prelert.rs.resources.data;

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

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.JobInUseException;
import com.prelert.job.TooManyJobsException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.process.MissingFieldException;
import com.prelert.job.process.NativeProcessRunException;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.rs.data.ErrorCode;
import com.prelert.rs.provider.RestApiException;
import com.prelert.rs.streaminginterceptor.StreamingInterceptor;

public abstract class AbstractDataStreamer {

    protected final Logger LOGGER = Logger.getLogger(getClass());

    /**
     * Persisted data files are named with this date format
     * e.g. Tue_22_Apr_2014_091033
     */
    private static final SimpleDateFormat PERSISTED_FILE_NAME_DATE_FORMAT =
            new SimpleDateFormat("EEE_d_MMM_yyyy_HHmmss");

    private final boolean m_ShouldPersistDataToDisk;
    private final String m_BaseDirectory;
    private final JobManager m_JobManager;

    public AbstractDataStreamer(JobManager jobManager)
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
     * @return
     * @throws IOException
     * @throws UnknownJobException
     * @throws NativeProcessRunException
     * @throws MissingFieldException
     * @throws JobInUseException if the data cannot be written to because
     * the job is already handling data
     * @throws HighProportionOfBadTimestampsException
     * @throws OutOfOrderRecordsException
     * @throws TooManyJobsException If the license is violated
     */
    public void streamData(HttpHeaders headers, String jobId, InputStream input)
            throws IOException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, TooManyJobsException
    {
        LOGGER.debug("Handle Post data to job = " + jobId);

        input = tryDecompressingInputStream(headers, jobId, input);
        if (m_ShouldPersistDataToDisk)
        {
            input = persistDataToDisk(jobId, input);
        }
        handleStream(jobId, input);

        LOGGER.debug("File uploaded to job " + jobId);
    }

    private InputStream tryDecompressingInputStream(HttpHeaders headers,
            String jobId, InputStream input) throws IOException
    {
        String contentEncoding = headers.getHeaderString(HttpHeaders.CONTENT_ENCODING);
        if (contentEncoding!= null && contentEncoding.equals("gzip"))
        {
            LOGGER.info("Decompressing post data in job = " + jobId);
            try
            {
                return new GZIPInputStream(input);
            }
            catch (ZipException ze)
            {
                throw new RestApiException("Content-Encoding = gzip "
                        + "but the data is not in gzip format",
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
     * @return
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
     */
    private boolean handleStream(String jobId, InputStream input) throws
            NativeProcessRunException, UnknownJobException, MissingFieldException,
            JsonParseException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, TooManyJobsException
    {
        if (shouldPersistJobData())
        {
            return m_JobManager.submitDataLoadAndPersistJob(jobId, input);
        }
        return m_JobManager.submitDataLoadJob(jobId, input);
    }

    protected abstract boolean shouldPersistJobData();
}
