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

package com.prelert.job.process.writer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataCounts;
import com.prelert.job.DataDescription;
import com.prelert.job.persistence.JobDataPersister;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.job.status.StatusReporter;
import com.prelert.job.transform.TransformConfigs;

/**
 * This writer is used for reading input data that are unstructured and
 * each record is a single line. The writer applies transforms and pipes
 * the records into length encoded output.
 *
 * This writer is expected only to be used in combination of transforms
 * that will extract the time and the other fields used in the analysis.
 *
 * Records for which no time can be extracted will be ignored.
 */
public class SingleLineDataToProcessWriter extends AbstractDataToProcessWriter
{
    private static final String RAW = "raw";

    protected SingleLineDataToProcessWriter(boolean includeControlField, RecordWriter recordWriter,
            DataDescription dataDescription, AnalysisConfig analysisConfig,
            TransformConfigs transformConfigs, StatusReporter statusReporter,
            JobDataPersister jobDataPersister, Logger logger)
    {
        super(includeControlField, recordWriter, dataDescription, analysisConfig, transformConfigs, statusReporter,
                jobDataPersister, logger);
    }

    @Override
    public DataCounts write(InputStream inputStream) throws IOException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    {
        m_StatusReporter.startNewIncrementalCount();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)))
        {
            String[] header = {RAW};
            buildTransformsAndWriteHeader(header);

            int numFields = outputFieldCount();
            String[] record = new String[numFields];

            for (String line = bufferedReader.readLine(); line !=null;
                    line = bufferedReader.readLine())
            {
                Arrays.fill(record, "");
                applyTransformsAndWrite(new String[] {line}, record, 1);
            }
            m_StatusReporter.finishReporting();
        }
        finally
        {
            // nothing in this finally block should throw
            // as it would suppress any exceptions from the try block
            m_JobDataPersister.flushRecords();
        }

        return m_StatusReporter.incrementalStats();
    }

    @Override
    protected boolean checkForMissingFields(Collection<String> inputFields,
            Map<String, Integer> inputFieldIndicies, String[] header) throws MissingFieldException
    {
        return true;
    }
}
