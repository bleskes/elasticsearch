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

package com.prelert.job.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.DataCounts;
import com.prelert.job.JobException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.LicenseViolationException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.messages.Messages;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.params.DataLoadParams;
import com.prelert.job.scheduler.DataProcessor;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.settings.PrelertSettings;

public class DataStreamer
{
    private static final Logger LOGGER = Logger.getLogger(DataStreamer.class);

    /**
     * Persisted data files are named with this date format
     * e.g. 2014_01_28_091033_748
     */
    private static final DateTimeFormatter PERSISTED_FILE_NAME_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy_MM_dd_HHmmss_SSS");

    private final boolean m_ShouldPersistDataToDisk;
    private final String m_BaseDirectory;
    private final DataProcessor m_DataProccesor;

    public DataStreamer(DataProcessor dataProcessor)
    {
        // should we save uploaded data and where
        m_ShouldPersistDataToDisk = PrelertSettings.isSet("persistbasedir");
        m_BaseDirectory = PrelertSettings.getSettingOrDefault("persistbasedir", ".");
        m_DataProccesor = Objects.requireNonNull(dataProcessor);
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
     * @throws LicenseViolationException If the license is violated
     * @throws MalformedJsonException If JSON data is malformed and we cannot recover
     */
    public DataCounts streamData(String contentEncoding, String jobId, InputStream input, DataLoadParams params)
            throws IOException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, LicenseViolationException, MalformedJsonException, JobException
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
            String jobId, InputStream input) throws IOException, JobException
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
                throw new JobException(Messages.getMessage(Messages.REST_GZIP_ERROR),
                        ErrorCodes.UNCOMPRESSED_DATA);
            }
        }
        return input;
    }

    private InputStream persistDataToDisk(String jobId, InputStream input)
            throws IOException
    {
        try
        {
            Files.createDirectory(FileSystems.getDefault().getPath(m_BaseDirectory, jobId));
        }
        catch (FileAlreadyExistsException e)
        {
            // continue
        }

        java.nio.file.Path filePath = FileSystems.getDefault().getPath(
                m_BaseDirectory, jobId, PERSISTED_FILE_NAME_DATE_FORMAT.format(LocalDateTime.now()) + ".gz");

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
     * @throws LicenseViolationException If the license is violated
     * @throws TooManyJobsException If too many jobs for the number of CPU cores
     * @throws MalformedJsonException If JSON data is malformed and we cannot recover
     */
    private DataCounts handleStream(String jobId, InputStream input, DataLoadParams params) throws
            NativeProcessRunException, UnknownJobException, MissingFieldException,
            JsonParseException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, LicenseViolationException, TooManyJobsException,
            MalformedJsonException
    {
        return m_DataProccesor.submitDataLoadJob(jobId, input, params);
    }
}
