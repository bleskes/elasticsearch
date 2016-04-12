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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

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
 * A writer for transforming and piping CSV data from an
 * inputstream to outputstream.
 * The data written to output is length encoded each record
 * consists of number of fields followed by length/value pairs.
 * See CLengthEncodedInputParser.h in the C++ code for a more
 * detailed description.
 * A control field is added to the end of each length encoded
 * line.
 */
class CsvDataToProcessWriter extends AbstractDataToProcessWriter
{
    /**
     * Maximum number of lines allowed within a single CSV record.
     *
     * In the scenario where there is a misplaced quote, there is
     * the possibility that it results to a single record expanding
     * over many lines. Supercsv will eventually deplete all memory
     * from the JVM. We set a limit to an arbitrary large number
     * to prevent that from happening. Unfortunately, supercsv
     * throws an exception which means we cannot recover and continue
     * reading new records from the next line.
     */
    private static final int MAX_LINES_PER_RECORD = 10000;

    public CsvDataToProcessWriter(boolean includeControlField, RecordWriter recordWriter,
            DataDescription dataDescription, AnalysisConfig analysisConfig,
            TransformConfigs transforms, StatusReporter statusReporter,
            JobDataPersister jobDataPersister, Logger logger)
    {
        super(includeControlField, recordWriter, dataDescription, analysisConfig, transforms,
                statusReporter, jobDataPersister, logger);
    }

    /**
     * Read the csv input, transform to length encoded values and pipe
     * to the OutputStream.
     * If any of the expected fields in the transform inputs, analysis input or
     * if the expected time field is missing from the CSV header
     * a <code>MissingFieldException</code> is thrown
     *
     * @throws IOException
     * @throws MissingFieldException If any fields are missing from the CSV header
     * @throws HighProportionOfBadTimestampsException If a large proportion
     * of the records read have missing fields
     * @throws OutOfOrderRecordsException
     */
    @Override
    public DataCounts write(InputStream inputStream) throws IOException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    {
        CsvPreference csvPref = new CsvPreference.Builder(
                m_DataDescription.getQuoteCharacter(),
                m_DataDescription.getFieldDelimiter(),
                new String(new char[] {DataDescription.LINE_ENDING}))
                .maxLinesPerRow(MAX_LINES_PER_RECORD).build();

        m_StatusReporter.startNewIncrementalCount();

        try (CsvListReader csvReader = new CsvListReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8),
                csvPref))
        {
            String[] header = csvReader.getHeader(true);
            if (header == null) // null if EoF
            {
                return new DataCounts();
            }

            long inputFieldCount = Math.max(header.length -1, 0); // time field doesn't count

            buildTransformsAndWriteHeader(header);

            //backing array for the input
            String [] inputRecord = new String[header.length];

            int maxIndex = 0;
            for (Integer index : m_InFieldIndexes.values())
            {
                maxIndex = Math.max(index, maxIndex);
            }

            int numFields = outputFieldCount();
            String[] record = new String[numFields];

            List<String> line;
            while ((line = csvReader.read()) != null)
            {
                Arrays.fill(record, "");

                if (maxIndex >= line.size())
                {
                    m_Logger.warn("Not enough fields in csv record, expected at least "  + maxIndex
                            + ". "+ line);

                    for (InputOutputMap inOut : m_InputOutputMap)
                    {
                        if (inOut.m_Input >= line.size())
                        {
                            m_StatusReporter.reportMissingField();
                            continue;
                        }

                        String field = line.get(inOut.m_Input);
                        record[inOut.m_Output] = (field == null) ? "" : field;
                    }
                }
                else
                {
                    for (InputOutputMap inOut : m_InputOutputMap)
                    {
                        String field = line.get(inOut.m_Input);
                        record[inOut.m_Output] = (field == null) ? "" : field;
                    }
                }

                fillRecordFromLine(line, inputRecord);
                applyTransformsAndWrite(inputRecord, record, inputFieldCount);
            }

            // This function can throw
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

    private static void fillRecordFromLine(List<String> line, String[] record)
    {
        Arrays.fill(record, "");
        for (int i = 0; i < Math.min(line.size(), record.length); i++)
        {
            String value = line.get(i);
            if (value != null)
            {
                record[i] = value;
            }
        }
    }

    @Override
    protected boolean checkForMissingFields(Collection<String> inputFields,
                                            Map<String, Integer> inputFieldIndicies,
                                            String [] header)
    throws MissingFieldException
    {
        for (String field : inputFields)
        {
            if (AnalysisConfig.AUTO_CREATED_FIELDS.contains(field))
            {
                continue;
            }
            Integer index = inputFieldIndicies.get(field);
            if (index == null)
            {
                String msg = String.format("Field configured for analysis "
                        + "'%s' is not in the CSV header '%s'",
                        field, Arrays.toString(header));

                m_Logger.error(msg);
                throw new MissingFieldException(field, msg);
            }
        }

        return true;
    }
}
