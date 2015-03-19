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

package com.prelert.job.process.writer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataCounts;
import com.prelert.job.DataDescription;
import com.prelert.job.TransformConfigs;
import com.prelert.job.input.CountingInputStream;
import com.prelert.job.input.LengthEncodedWriter;
import com.prelert.job.persistence.JobDataPersister;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.job.status.StatusReporter;

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

    public JsonDataToProcessWriter(LengthEncodedWriter lengthEncodedWriter,
            DataDescription dataDescription, AnalysisConfig analysisConfig,
            TransformConfigs transforms, StatusReporter statusReporter,
            JobDataPersister jobDataPersister, Logger logger)
    {
        super(lengthEncodedWriter, dataDescription, analysisConfig, transforms,
                statusReporter, jobDataPersister, logger);
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
        CountingInputStream countingStream = new CountingInputStream(inputStream, m_StatusReporter);
        m_StatusReporter.setAnalysedFieldsPerRecord(m_AnalysisConfig.analysisFields().size());

        m_StatusReporter.startNewIncrementalCount();

        try (JsonParser parser = new JsonFactory().createParser(countingStream))
        {
            writeJson(parser);
            m_LengthEncodedWriter.flush();
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

        buildTransforms(analysisFields.toArray(new String[0]));

        writeHeader();

        int numFields = m_OutFieldIndexes.size();
        String[] input = new String[numFields];
        String[] record = new String[numFields];
        record[record.length -1] = ""; // The control field is always an empty string

        // We never expect to get the control field
        boolean [] gotFields = new boolean[analysisFields.size()];


        int recordsWritten = 0;
        int recordCount = 0;

        int timeFieldIndex = m_InFieldIndexes.get(m_DataDescription.getTimeField());

        JsonRecordReader recordReader = new JsonRecordReader(parser, m_InFieldIndexes, m_Logger);
        long inputFieldCount = recordReader.read(input, gotFields);
        while (inputFieldCount > 0)
        {
            Arrays.fill(record, "");

            inputFieldCount = Math.max(inputFieldCount - 1, 0); // time field doesn't count


            if (gotFields[timeFieldIndex])
            {
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

                if (applyTransformsAndWrite(input, record, inputFieldCount))
                {
                    ++recordsWritten;
                }
            }
            else
            {
                m_Logger.warn("Missing time field from JSON document");
                m_StatusReporter.reportMissingField();
            }

            ++recordCount;

            inputFieldCount = recordReader.read(input, gotFields);
        }

        m_Logger.debug(String.format("Transferred %d of %d Json records to autodetect.",
                recordsWritten, recordCount));
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
