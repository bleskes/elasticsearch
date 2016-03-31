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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataCounts;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.persistence.JobDataPersister;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.job.status.StatusReporter;
import com.prelert.job.transform.TransformConfigs;

/**
 * A writer for transforming and piping JSON data from an
 * inputstream to outputstream.
 * The data written to output is length encoded each record
 * consists of number of fields followed by length/value pairs.
 * See CLengthEncodedInputParser.h in the C++ code for a more
 * detailed description.
 */
class JsonDataToProcessWriter extends AbstractDataToProcessWriter
{
    private static final String ELASTICSEARCH_SOURCE_FIELD = "_source";
    private static final String ELASTICSEARCH_FIELDS_FIELD = "fields";

    /**
     * Scheduler config.  May be <code>null</code>.
     */
    private SchedulerConfig m_SchedulerConfig;

    public JsonDataToProcessWriter(RecordWriter recordWriter,
            DataDescription dataDescription, AnalysisConfig analysisConfig,
            SchedulerConfig schedulerConfig, TransformConfigs transforms,
            StatusReporter statusReporter, JobDataPersister jobDataPersister, Logger logger)
    {
        super(recordWriter, dataDescription, analysisConfig, transforms,
                statusReporter, jobDataPersister, logger);
        m_SchedulerConfig = schedulerConfig;
    }

    /**
     * Read the JSON input, transform to length encoded values and pipe
     * to the OutputStream.
     * No transformation is applied to the data the timestamp is expected
     * in seconds from the epoch.
     * If any of the fields in <code>analysisFields</code> or the
     * <code>DataDescription</code>s timeField is missing from the JOSN input
     * a <code>MissingFieldException</code> is thrown
     *
     * @throws IOException
     * @throws MissingFieldException If any fields are missing from the JSON records
     * @throws HighProportionOfBadTimestampsException If a large proportion
     * of the records read have missing fields
     * @throws OutOfOrderRecordsException
     * @throws MalformedJsonException
     */
    @Override
    public DataCounts write(InputStream inputStream) throws IOException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException, MalformedJsonException
    {
        m_StatusReporter.startNewIncrementalCount();

        try (JsonParser parser = new JsonFactory().createParser(inputStream))
        {
            writeJson(parser);

            // this line can throw and will be propagated
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

    private void writeJson(JsonParser parser) throws IOException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        Collection<String> analysisFields = inputFields();

        buildTransformsAndWriteHeader(analysisFields.toArray(new String[0]));

        int numFields = outputFieldCount();
        String[] input = new String[numFields];
        String[] record = new String[numFields];
        record[record.length - 1] = ""; // The control field is always an empty string

        // We never expect to get the control field
        boolean [] gotFields = new boolean[analysisFields.size()];

        JsonRecordReader recordReader = makeRecordReader(parser);
        long inputFieldCount = recordReader.read(input, gotFields);
        while (inputFieldCount >= 0)
        {
            Arrays.fill(record, "");

            inputFieldCount = Math.max(inputFieldCount - 1, 0); // time field doesn't count

            long missing = missingFieldCount(gotFields);
            if (missing > 0)
            {
                m_StatusReporter.reportMissingFields(missing);
            }

            for (InputOutputMap inOut : m_InputOutputMap)
            {
                String field = input[inOut.m_Input];
                record[inOut.m_Output] = (field == null) ? "" : field;
            }

            applyTransformsAndWrite(input, record, inputFieldCount);

            inputFieldCount = recordReader.read(input, gotFields);
        }
    }

    private String getRecordHoldingField()
    {
        if (m_DataDescription.getFormat().equals(DataFormat.ELASTICSEARCH))
        {
            if (m_SchedulerConfig != null)
            {
                if (m_SchedulerConfig.getAggregationsOrAggs() != null)
                {
                    return SchedulerConfig.AGGREGATIONS;
                }
                if (!Boolean.TRUE.equals(m_SchedulerConfig.getRetrieveWholeSource()))
                {
                    return ELASTICSEARCH_FIELDS_FIELD;
                }
            }
            return ELASTICSEARCH_SOURCE_FIELD;
        }
        return "";
    }

    private JsonRecordReader makeRecordReader(JsonParser parser)
    {
        List<String> nestingOrder = (m_SchedulerConfig != null) ?
                m_SchedulerConfig.buildAggregatedFieldList() : Collections.emptyList();
        return nestingOrder.isEmpty() ? new SimpleJsonRecordReader(parser, m_InFieldIndexes,
                getRecordHoldingField(), m_Logger) : new AggregatedJsonRecordReader(parser,
                m_InFieldIndexes, getRecordHoldingField(), m_Logger, nestingOrder);
    }

    /**
     * Don't enforce the check that all the fields are present in JSON docs.
     * Always returns true
     */
    @Override
    protected boolean checkForMissingFields(Collection<String> inputFields,
                                                Map<String, Integer> inputFieldIndicies,
                                                String [] header)
    throws MissingFieldException
    {
        return true;
    }

    /**
     * Return the number of missing fields
     *
     * @param requiredFields
     * @param gotFieldFlags
     * @return
     */
    private static long missingFieldCount(boolean [] gotFieldFlags)
    {
        long count = 0;

        for (int i=0; i<gotFieldFlags.length; i++)
        {
            if  (gotFieldFlags[i] == false)
            {
                ++count;
            }
        }

        return count;
    }
}
