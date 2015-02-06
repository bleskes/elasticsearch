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

package com.prelert.job.process.writer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.input.CountingInputStream;
import com.prelert.job.input.LengthEncodedWriter;
import com.prelert.job.persistence.JobDataPersister;
import com.prelert.job.process.dateparsing.DateTransformer;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.job.status.StatusReporter;

/**
 * A writer for transforming and piping CSV data from an
 * inputstream to outputstream.
 * The data writtin to output is length encoded each record
 * consists of number of fields followed by length/value pairs.
 * See CLengthEncodedInputParser.h in the C++ code for a more
 * detailed description.
 * A control field is added to the end of each length encoded
 * line.
 */
class CsvDataToProcessWriter extends AbstractDataToProcessWriter
{

    public CsvDataToProcessWriter(LengthEncodedWriter lengthEncodedWriter,
            DataDescription dataDescription, AnalysisConfig analysisConfig,
            StatusReporter statusReporter, JobDataPersister jobDataPersister, Logger logger,
            DateTransformer dateTransformer)
    {
        super(lengthEncodedWriter, dataDescription, analysisConfig, statusReporter,
                jobDataPersister, logger, dateTransformer);
    }

    /**
     * Read the csv input, transform to length encoded values and pipe
     * to the OutputStream.
     * No transformation is applied to the data the timestamp is expected
     * in seconds from the epoch.
     * If any of the fields in <code>analysisFields</code> or the
     * <code>DataDescription</code>s timeField is missing from the CSV header
     * a <code>MissingFieldException</code> is thrown
     *
     * @throws IOException
     * @throws MissingFieldException If any fields are missing from the CSV header
     * @throws HighProportionOfBadTimestampsException If a large proportion
     * of the records read have missing fields
     * @throws OutOfOrderRecordsException
     */
    @Override
    public void write(InputStream inputStream) throws IOException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    {
        CsvPreference csvPref = new CsvPreference.Builder(
                m_DataDescription.getQuoteCharacter(),
                m_DataDescription.getFieldDelimiter(),
                new String(new char[] {DataDescription.LINE_ENDING})).build();

        int recordsWritten = 0;
        int lineCount = 0;

        List<String> analysisFields = m_AnalysisConfig.analysisFields();
        m_StatusReporter.setAnalysedFieldsPerRecord(analysisFields.size());

        CountingInputStream countingStream = new CountingInputStream(inputStream, m_StatusReporter);

        try (CsvListReader csvReader = new CsvListReader(
                new InputStreamReader(countingStream, StandardCharsets.UTF_8),
                csvPref))
        {
            String[] header = csvReader.getHeader(true);
            long inputFieldCount = Math.max(header.length - 1, 0); // time fields doesn't count


            List<Pair<String, Integer>> fieldIndexes =
                    findFieldIndexes(header, analysisFields);

            int maxIndex = 0;
            Iterator<Pair<String, Integer>> iter = fieldIndexes.iterator();
            while (iter.hasNext())
            {
                Pair<String, Integer> p = iter.next();

                if (p.m_Second > maxIndex)
                {
                    maxIndex = p.m_Second;
                }

                if (p.m_Second < 0)
                {
                    String msg = String.format("Field configured for analysis "
                            + "'%s' is not in the CSV header '%s'",
                            p.m_First, Arrays.toString(header));
                    m_Logger.error(msg);

                    throw new MissingFieldException(p.m_First, msg);
                }
            }

            // filtered header is all the analysis fields + the time field + control field
            String[] filteredHeader = new String[fieldIndexes.size() + 1];
            int i = 0;
            for (Pair<String, Integer> p : fieldIndexes)
            {
                filteredHeader[i++] = p.m_First;
            }
            filteredHeader[filteredHeader.length - 1] = LengthEncodedWriter.CONTROL_FIELD_NAME;

            int timeFieldIndex = Arrays.asList(filteredHeader).indexOf(
                    m_DataDescription.getTimeField());
            if (timeFieldIndex < 0)
            {
                String message = String.format("Cannot find timestamp field '%s'"
                        + " in CSV header '%s'", m_DataDescription.getTimeField(),
                        Arrays.toString(filteredHeader));
                m_Logger.error(message);
                throw new MissingFieldException(m_DataDescription.getTimeField(), message);
            }

            m_JobDataPersister.setFieldMappings(m_AnalysisConfig.fields(),
                    m_AnalysisConfig.byFields(), m_AnalysisConfig.overFields(),
                    m_AnalysisConfig.partitionFields(), filteredHeader);

            m_LengthEncodedWriter.writeRecord(filteredHeader);

            // The + 1 is for the control field
            int numFields = fieldIndexes.size() + 1;
            List<String> line;

            String[] record = new String[numFields];
            // Control field is always empty for real input
            record[numFields - 1] = "";

            while ((line = csvReader.read()) != null)
            {
                lineCount++;

                i = 0;
                if (maxIndex >= line.size())
                {
                    m_Logger.warn("Not enough fields in csv record, expected at least "  + maxIndex
                    		+ ". "+ line);

                    Arrays.fill(record, "");
                    for (Pair<String, Integer> p : fieldIndexes)
                    {
                        if (p.m_Second >= line.size())
                        {
                            m_StatusReporter.reportMissingField();
                            i++;
                            continue;
                        }

                        String field = line.get(p.m_Second);
                        record[i] = (field == null) ? "" : field;
                        i++;
                    }
                }
                else
                {
                    for (Pair<String, Integer> p : fieldIndexes)
                    {
                        String field = line.get(p.m_Second);
                        record[i] = (field == null) ? "" : field;
                        i++;
                    }
                }

                Long epoch = transformTimeAndWrite(record, timeFieldIndex, inputFieldCount);
                if (epoch != null)
                {
                    recordsWritten++;
                }
            }

            // This function can throw and the exceptions thrown
            m_StatusReporter.finishReporting();

            m_LengthEncodedWriter.flush();
        }
        finally
        {
            // nothing in this finally block should throw
            // as it would suppress any exceptions from the try block
            m_JobDataPersister.flushRecords();
        }

        m_Logger.debug(String.format("Transferred %d of %d CSV records to autodetect.",
                recordsWritten, lineCount));
    }

    /**
     * Finds the indexes of the analysis fields and the
     * timestamp field in <code>header</code>.
     *
     * @param header
     * @param analysisFields
     * @return
     */
    private List<Pair<String, Integer>> findFieldIndexes(String[] header,
            List<String> analysisFields)
    {
        List<String> headerList = Arrays.asList(header);  // TODO header could be empty

        List<Pair<String, Integer>> fieldIndexes = new ArrayList<>();

        String timeField = m_DataDescription.getTimeField();
        // time field
        Pair<String, Integer> p = new Pair<>(timeField,
                headerList.indexOf(timeField));
        fieldIndexes.add(p);
        m_Logger.info("Index of field " + p.m_First + " is " + p.m_Second);

        for (String field : analysisFields)
        {
            p = new Pair<>(field, headerList.indexOf(field));
            fieldIndexes.add(p);
            m_Logger.info("Index of field " + p.m_First + " is " + p.m_Second);
        }

        return fieldIndexes;
    }

    /**
     * Generic helper class
     *
     * @param <T>
     * @param <U>
     */
    private static class Pair<T,U>
    {
        public final T m_First;
        public final U m_Second;
        public Pair(T first, U second)
        {
            this.m_First = first;
            this.m_Second = second;
        }
    }
}
