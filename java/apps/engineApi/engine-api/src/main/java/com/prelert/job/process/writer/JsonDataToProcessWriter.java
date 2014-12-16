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
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
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
 * A writer for transforming and piping JSON data from an
 * inputstream to outputstream.
 * The data writtin to output is length encoded each record
 * consists of number of fields followed by length/value pairs.
 * See CLengthEncodedInputParser.h in the C++ code for a more
 * detailed description.
 */
class JsonDataToProcessWriter extends AbstractDataToProcessWriter
{

    public JsonDataToProcessWriter(LengthEncodedWriter lengthEncodedWriter,
            DataDescription dataDescription, AnalysisConfig analysisConfig,
            StatusReporter statusReporter, JobDataPersister jobDataPersister, Logger logger,
            DateTransformer dateTransformer)
    {
        super(lengthEncodedWriter, dataDescription, analysisConfig, statusReporter,
                jobDataPersister, logger, dateTransformer);
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
     */
    @Override
    public void write(InputStream inputStream) throws IOException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    {
        CountingInputStream countingStream = new CountingInputStream(inputStream, m_StatusReporter);
        m_StatusReporter.setAnalysedFieldsPerRecord(m_AnalysisConfig.analysisFields().size());

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
    }

    private void writeJson(JsonParser parser) throws IOException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    {
        List<String> analysisFields = m_AnalysisConfig.analysisFields();

        // record is all the analysis fields + the time field + control field
        String[] allFields = new String[analysisFields.size() + 2];

        int i = 0;
        for (String s : analysisFields)
        {
            allFields[i] = s;
            i++;
        }

        // time field is the penultimate item
        int timeFieldIndex = allFields.length - 2;
        allFields[timeFieldIndex] = m_DataDescription.getTimeField();

        // control field is the last item
        int controlFieldIndex = allFields.length - 1;
        allFields[controlFieldIndex] = LengthEncodedWriter.CONTROL_FIELD_NAME;

        String [] record = new String[allFields.length];
        // We never expect to get the control field, hence - 1 here
        boolean [] gotFields = new boolean[allFields.length - 1];

        Map<String, Integer> fieldMap = new HashMap<>();
        for (i = 0; i < allFields.length - 1; i++)
        {
            fieldMap.put(allFields[i], new Integer(i));
        }

        m_JobDataPersister.setFieldMappings(m_AnalysisConfig.fields(),
                m_AnalysisConfig.byFields(), m_AnalysisConfig.overFields(),
                m_AnalysisConfig.partitionFields(), allFields);


        // write header
        m_LengthEncodedWriter.writeRecord(allFields);

        int recordsWritten = 0;
        int recordCount = 0;
        long lastEpoch = 0;

        long inputFieldCount = readJsonRecord(parser, record, fieldMap, gotFields);
        while (inputFieldCount > 0)
        {
            inputFieldCount = Math.max(inputFieldCount - 1, 0); // time field doesn't count

            if (gotFields[timeFieldIndex])
            {
                long missing = missingFieldCount(gotFields);
                if (missing > 0)
                {
                    m_StatusReporter.reportMissingFields(missing);
                }

                Long epoch = transformTimeAndWrite(record, timeFieldIndex, lastEpoch, inputFieldCount);
                if (epoch != null)
                {
                    recordsWritten++;
                    lastEpoch = epoch;
                }
            }
            else
            {
                m_Logger.warn("Missing time field from JSON document");
                m_StatusReporter.reportMissingField();
            }

            ++recordCount;

            inputFieldCount = readJsonRecord(parser, record, fieldMap, gotFields);
        }

        m_Logger.debug(String.format("Transferred %d of %d Json records to autodetect.",
                recordsWritten, recordCount));
    }

    /**
     * Read the JSON object and write to the record array.
     * Nested objects are flattened with the field names separated by
     * a '.'.
     * e.g. for a record with a nested 'tags' object:
     *  "{"name":"my.test.metric1","tags":{"tag1":"blah","tag2":"boo"},"time":1350824400,"value":12345.678}"
     * use 'tags.tag1' to reference the tag1 field in the nested object
     *
     * Array fields in the JSON are ignored
     *
     * @param parser
     * @param record Read fields are written to this array
     * @param fieldMap Map to field name to record array index position
     * @param gotFields boolean array each element is true if that field
     * was read
     *
     * @return The number of fields in the JSON doc
     * @throws IOException
     * @throws JsonParseException
     */
    private long readJsonRecord(JsonParser parser, String[] record, Map<String, Integer> fieldMap,
            boolean[] gotFields) throws JsonParseException, IOException
    {
        Arrays.fill(gotFields, false);
        Arrays.fill(record, "");

        int nestedLevel = 0;
        Deque<String> stack = new ArrayDeque<String>();

        long fieldCount = 0;
        String nestedSuffix = "";

        JsonToken token = parser.nextToken();
        while (!(token == JsonToken.END_OBJECT && nestedLevel == 0))
        {
            if (token == null)
            {
                break;
            }
            if (token == JsonToken.END_OBJECT)
            {
                nestedLevel--;
                String objectFieldName = stack.pop();

                int lastIndex = nestedSuffix.length() - objectFieldName.length() -1;
                nestedSuffix = nestedSuffix.substring(0, lastIndex);
            }
            else if (token == JsonToken.FIELD_NAME)
            {
                String fieldName = parser.getCurrentName();
                token = parser.nextToken();

                if (token == JsonToken.START_OBJECT)
                {
                    nestedLevel++;
                    stack.push(fieldName);

                    nestedSuffix = nestedSuffix + fieldName + ".";
                }
                else if (token == JsonToken.START_ARRAY)
                {
                    // consume the whole array but do nothing with it
                    while (token != JsonToken.END_ARRAY)
                    {
                        token = parser.nextToken();
                    }
                    m_Logger.warn("Ignoring array field");
                }
                else
                {
                    ++fieldCount;

                    String fieldValue = parser.getText();

                    Integer index = fieldMap.get(nestedSuffix + fieldName);
                    if (index != null)
                    {
                        record[index] = fieldValue;
                        gotFields[index] = true;
                    }
                }
            }

            token = parser.nextToken();
        }

        return fieldCount;
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
