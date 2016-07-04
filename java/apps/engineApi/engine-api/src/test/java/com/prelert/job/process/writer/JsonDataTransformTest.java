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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.Detector;
import com.prelert.job.logging.JobLoggerFactory;
import com.prelert.job.persistence.DummyJobDataPersister;
import com.prelert.job.process.autodetect.ProcessManager;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.status.DummyStatusReporter;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.job.transform.TransformConfig;
import com.prelert.job.transform.TransformConfigs;
import com.prelert.job.usage.DummyUsageReporter;

public class JsonDataTransformTest
{
    private static Logger LOGGER = Logger.getLogger(JsonDataTransformTest.class);


    /**
     * Test transforming JSON without a time format to length encoded
     * with the extra fields not used in the analysis filtered out
     */
    @Test
    public void plainJsonToLengthEncoded() throws IOException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        String data = "{\"timestamp\": \"1350824400\", \"airline\": \"DJA\", \"junk_field\": \"nonsense\", \"responsetime\": \"622\", \"sourcetype\": \"flightcentre\"}" +
                    "{\"timestamp\": \"1350824401\", \"airline\": \"JQA\", \"junk_field\": \"nonsense\", \"responsetime\": \"1742\", \"sourcetype\": \"flightcentre\"}" +
                    "{\"timestamp\": \"1350824402\", \"airline\": \"GAL\", \"junk_field\": \"nonsense\", \"responsetime\": \"5339\", \"sourcetype\": \"flightcentre\"}" +
                    "{\"timestamp\": \"1350824403\", \"airline\": \"GAL\", \"junk_field\": \"nonsense\", \"responsetime\": \"3893\", \"sourcetype\": \"flightcentre\"}" +
                    "{\"timestamp\": \"1350824403\", \"airline\": \"JQA\", \"junk_field\": \"nonsense\", \"responsetime\": \"9\", \"sourcetype\": \"flightcentre\"}" +
                    "{\"timestamp\": \"1350824404\", \"airline\": \"DJA\", \"junk_field\": \"nonsense\", \"responsetime\": \"189\", \"sourcetype\": \"flightcentre\"}" +
                    "{\"timestamp\": \"1350824404\", \"airline\": \"JQA\", \"junk_field\": \"nonsense\", \"responsetime\": \"8\", \"sourcetype\": \"flightcentre\"}" +
                    "{\"timestamp\": \"1350824404\", \"airline\": \"DJA\", \"junk_field\": \"nonsense\", \"responsetime\": \"1200\", \"sourcetype\": \"flightcentre\"}";

        // The . is the control field name
        String[] header = new String[] {"timestamp", "airline", "responsetime", "sourcetype", "."};
        // The last blank is the control field value
        String[][] records = new String[][] {{"1350824400", "DJA", "622", "flightcentre", ""},
                                                {"1350824401", "JQA", "1742", "flightcentre", ""},
                                                {"1350824402", "GAL", "5339", "flightcentre", ""},
                                                {"1350824403", "GAL", "3893", "flightcentre", ""},
                                                {"1350824403", "JQA", "9", "flightcentre", ""},
                                                {"1350824404", "DJA", "189", "flightcentre", ""},
                                                {"1350824404", "JQA", "8", "flightcentre", ""},
                                                {"1350824404", "DJA", "1200", "flightcentre", ""}};

        // The first output field is time then fields are written
        // alphabetical order then finally the control field
        int [] fieldMap = new int [] {0, 1, 2, 3, 4};

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.JSON);
        dd.setTimeField("timestamp");

        AnalysisConfig ac = new AnalysisConfig();
        Detector det = new Detector();
        det.setFieldName("responsetime");
        det.setByFieldName("airline");
        det.setPartitionFieldName("sourcetype");
        ac.setDetectors(Arrays.asList(det));

        ProcessManager pm = createProcessManager();

        ByteArrayInputStream bis =
                new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

        DummyUsageReporter usageReporter = new DummyUsageReporter("job_id", LOGGER);
        DummyStatusReporter statusReporter = new DummyStatusReporter(usageReporter);
        DummyJobDataPersister dataPersister = new DummyJobDataPersister();

        pm.writeToJob(dd, ac, null, new TransformConfigs(Arrays.<TransformConfig>asList()), bis, bos, statusReporter,
                 dataPersister, LOGGER);
        ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());

        assertEquals(usageReporter.getTotalBytesRead(),
                data.getBytes(StandardCharsets.UTF_8).length - 1);
        assertEquals(8, usageReporter.getTotalRecordsRead() );
        assertEquals(8 * 4, usageReporter.getTotalFieldsRead() );

        assertEquals(8, statusReporter.getInputRecordCount() );
        assertEquals(8 * 4, statusReporter.getInputFieldCount() );
        assertEquals(8, statusReporter.getProcessedRecordCount() );
        assertEquals(8 * 3, statusReporter.getProcessedFieldCount() );
        assertEquals(0, statusReporter.getMissingFieldErrorCount());
        assertEquals(0, statusReporter.getDateParseErrorsCount());
        assertEquals(0, statusReporter.getOutOfOrderRecordCount());

        assertEquals(statusReporter.runningTotalStats(), statusReporter.incrementalStats());


        assertEquals(dataPersister.getRecordCount(), 8);

        // check header
        int numFields = bb.getInt();
        assertEquals(header.length, numFields);

        for (int i=0; i<numFields; i++)
        {
            int recordSize = bb.getInt();
            assertEquals(header[fieldMap[i]].length(), recordSize);
            byte [] charBuff = new byte[recordSize];
            for (int j=0; j<recordSize; j++)
            {
                charBuff[j] = bb.get();
            }

            String value = new String(charBuff, StandardCharsets.UTF_8);
            assertEquals(header[fieldMap[i]], value);
        }


        // check records
        for (String [] fields : records)
        {
            numFields = bb.getInt();
            assertEquals(fields.length, numFields);

            for (int i=0; i<numFields; i++)
            {
                int recordSize = bb.getInt();
                byte [] charBuff = new byte[recordSize];
                for (int j=0; j<recordSize; j++)
                {
                    charBuff[j] = bb.get();
                 }

                String value = new String(charBuff, StandardCharsets.UTF_8);

                assertEquals(fields[fieldMap[i]].length(), recordSize);
                assertEquals(fields[fieldMap[i]], value);
            }
        }
    }


    /**
     * Test transforming JSON with a time format to length encoded.
     */
    @Test
    public void jsonWithDateFormatToLengthEncoded() throws IOException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        // The json docs are have different field orders
        String data = "{\"airline\": \"DJA\", \"timestamp\": \"2012-10-21T14:00:00+01:00\", \"responsetime\": \"622\", \"sourcetype\": \"flightcentre\"}" +
                    "{\"timestamp\": \"2012-10-21T14:00:01+01:00\", \"airline\": \"JQA\", \"responsetime\": \"1742\", \"sourcetype\": \"flightcentre\"}" +
                    "{\"timestamp\": \"2012-10-21T14:00:02+01:00\", \"airline\": \"GAL\", \"responsetime\": \"5339\", \"sourcetype\": \"flightcentre\"}" +
                    "{\"timestamp\": \"2012-10-21T14:00:03+01:00\", \"responsetime\": \"3893\", \"airline\": \"GAL\", \"sourcetype\": \"flightcentre\"}" +
                    "{\"timestamp\": \"2012-10-21T14:00:03+01:00\", \"airline\": \"JQA\", \"responsetime\": \"9\", \"sourcetype\": \"flightcentre\"}" +
                    "{\"sourcetype\": \"flightcentre\", \"timestamp\": \"2012-10-21T14:00:04+01:00\", \"airline\": \"DJA\", \"responsetime\": \"189\" }" +
                    "{\"timestamp\": \"2012-10-21T14:00:04+01:00\", \"airline\": \"JQA\", \"responsetime\": \"8\", \"sourcetype\": \"flightcentre\"}" +
                    "{\"airline\": \"DJA\", \"timestamp\": \"2012-10-21T14:00:04+01:00\", \"responsetime\": \"1200\", \"sourcetype\": \"flightcentre\"}";

        // The . is the control field name
        String[] header = new String[] {"timestamp", "airline", "responsetime", "sourcetype", "."};
        // The last blank is the control field value
        String[][] records = new String[][] {{"1350824400", "DJA", "622", "flightcentre", ""},
                                                {"1350824401", "JQA", "1742", "flightcentre", ""},
                                                {"1350824402", "GAL", "5339", "flightcentre", ""},
                                                {"1350824403", "GAL", "3893", "flightcentre", ""},
                                                {"1350824403", "JQA", "9", "flightcentre", ""},
                                                {"1350824404", "DJA", "189", "flightcentre", ""},
                                                {"1350824404", "JQA", "8", "flightcentre", ""},
                                                {"1350824404", "DJA", "1200", "flightcentre", ""}};


        // The first output field is time then fields are written
        // alphabetical order then finally the control field
        int [] fieldMap = new int [] {0, 1, 2, 3, 4};

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.JSON);
        dd.setTimeField("timestamp");
        dd.setTimeFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

        AnalysisConfig ac = new AnalysisConfig();
        Detector det = new Detector();
        det.setFieldName("responsetime");
        det.setByFieldName("airline");
        det.setPartitionFieldName("sourcetype");
        ac.setDetectors(Arrays.asList(det));

        Set<String> analysisFields = new TreeSet<String>(Arrays.asList(new String [] {
                "responsetime", "airline", "sourcetype"}));

        for (String s : ac.analysisFields())
        {
            assertTrue(analysisFields.contains(s));
        }

        ProcessManager pm = createProcessManager();

        ByteArrayInputStream bis =
                new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

        DummyUsageReporter usageReporter = new DummyUsageReporter("job_id", LOGGER);
        DummyStatusReporter statusReporter = new DummyStatusReporter(usageReporter);
        DummyJobDataPersister dp = new DummyJobDataPersister();

        pm.writeToJob(dd, ac, null, new TransformConfigs(Arrays.<TransformConfig>asList()), bis, bos, statusReporter, dp, LOGGER);
        ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());

        assertEquals(usageReporter.getTotalBytesRead(), statusReporter.getBytesRead());
        assertEquals(8, statusReporter.getInputRecordCount() );
        assertEquals(8 * 3, statusReporter.getInputFieldCount() );
        assertEquals(8, statusReporter.getProcessedRecordCount() );
        assertEquals(8 * 3, statusReporter.getProcessedFieldCount() );
        assertEquals(0, statusReporter.getMissingFieldErrorCount());
        assertEquals(0, statusReporter.getDateParseErrorsCount());
        assertEquals(0, statusReporter.getOutOfOrderRecordCount());

        assertEquals(statusReporter.runningTotalStats(), statusReporter.incrementalStats());

        assertEquals(usageReporter.getTotalBytesRead(),
                data.getBytes(StandardCharsets.UTF_8).length - 1);
        assertEquals(usageReporter.getTotalBytesRead(),
                statusReporter.getBytesRead());

        assertEquals(dp.getRecordCount(), 8);

        // check header
        int numFields = bb.getInt();
        assertEquals(header.length, numFields);

        for (int i=0; i<numFields; i++)
        {
            int recordSize = bb.getInt();
            assertEquals(header[fieldMap[i]].length(), recordSize);
            byte [] charBuff = new byte[recordSize];
            for (int j=0; j<recordSize; j++)
            {
                charBuff[j] = bb.get();
            }

            String value = new String(charBuff, StandardCharsets.UTF_8);
            assertEquals(header[fieldMap[i]], value);
        }


        // check records
        for (String [] fields : records)
        {
            numFields = bb.getInt();
            assertEquals(fields.length, numFields);

            for (int i=0; i<numFields; i++)
            {
                int recordSize = bb.getInt();
                assertEquals(fields[fieldMap[i]].length(), recordSize);
                byte [] charBuff = new byte[recordSize];
                for (int j=0; j<recordSize; j++)
                {
                    charBuff[j] = bb.get();
                 }

                String value = new String(charBuff, StandardCharsets.UTF_8);
                assertEquals(fields[fieldMap[i]], value);
            }
        }
    }


    /**
     * Test transforming JSON with a time format
     * and extra fields to length encoded.
     */
    @Test
    public void jsonWithDateFormatAndExtraFieldsToLengthEncoded() throws IOException,
            MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, MalformedJsonException
    {
        // Document fields are not in the same order
        String data = "{\"extra_field\": \"extra\", \"timestamp\": \"2012-10-21T14:00:00+01:00\", \"airline\": \"DJA\", \"responsetime\": \"622\", \"sourcetype\": \"flightcentre\", \"junk_field\": \"nonsense\"}" +
                    "{\"extra_field\": \"extra\", \"airline\": \"JQA\", \"responsetime\": \"1742\", \"timestamp\": \"2012-10-21T14:00:01+01:00\", \"sourcetype\": \"flightcentre\", \"junk_field\": \"nonsense\"}" +
                    "{\"timestamp\": \"2012-10-21T14:00:02+01:00\", \"extra_field\": \"extra\", \"airline\": \"GAL\", \"junk_field\": \"nonsense\", \"responsetime\": \"5339\", \"sourcetype\": \"flightcentre\"}" +
                    "{\"timestamp\": \"2012-10-21T14:00:03+01:00\", \"extra_field\": \"extra\", \"airline\": \"GAL\", \"responsetime\": \"3893\", \"sourcetype\": \"flightcentre\", \"junk_field\": \"nonsense\"}" +
                    "{\"timestamp\": \"2012-10-21T14:00:03+01:00\", \"extra_field\": \"extra\", \"airline\": \"JQA\", \"responsetime\": \"9\", \"sourcetype\": \"flightcentre\", \"junk_field\": \"nonsense\"}" +
                    "{\"timestamp\": \"2012-10-21T14:00:04+01:00\", \"airline\": \"DJA\", \"extra_field\": \"extra\", \"responsetime\": \"189\", \"junk_field\": \"nonsense\", \"sourcetype\": \"flightcentre\"}" +
                    "{\"airline\": \"JQA\", \"timestamp\": \"2012-10-21T14:00:04+01:00\", \"extra_field\": \"extra\", \"responsetime\": \"8\", \"sourcetype\": \"flightcentre\", \"junk_field\": \"nonsense\"}" +
                    "{\"junk_field\": \"nonsense\", \"sourcetype\": \"flightcentre\", \"timestamp\": \"2012-10-21T14:00:04+01:00\", \"extra_field\": \"extra\", \"airline\": \"DJA\", \"responsetime\": \"1200\"}";

        // The . is the control field name
        String[] header = new String[] {"timestamp", "airline", "responsetime", "sourcetype", "."};
        // The last blank is the control field value
        String[][] records = new String[][] {{"1350824400", "DJA", "622", "flightcentre", ""},
                                                {"1350824401", "JQA", "1742", "flightcentre", ""},
                                                {"1350824402", "GAL", "5339", "flightcentre", ""},
                                                {"1350824403", "GAL", "3893", "flightcentre", ""},
                                                {"1350824403", "JQA", "9", "flightcentre", ""},
                                                {"1350824404", "DJA", "189", "flightcentre", ""},
                                                {"1350824404", "JQA", "8", "flightcentre", ""},
                                                {"1350824404", "DJA", "1200", "flightcentre", ""}};



        // The first output field is time then fields are written
        // alphabetical order then finally the control field
        int [] fieldMap = new int [] {0, 1, 2, 3, 4};

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.JSON);
        dd.setTimeField("timestamp");
        dd.setTimeFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

        AnalysisConfig ac = new AnalysisConfig();
        Detector det = new Detector();
        det.setFieldName("responsetime");
        det.setByFieldName("airline");
        det.setPartitionFieldName("sourcetype");
        ac.setDetectors(Arrays.asList(det));

        Set<String> analysisFields = new TreeSet<String>(Arrays.asList(new String [] {
                "responsetime", "airline", "sourcetype"}));

        for (String s : ac.analysisFields())
        {
            assertTrue(analysisFields.contains(s));
        }

        ProcessManager pm = createProcessManager();

        ByteArrayInputStream bis =
                new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

        DummyUsageReporter usageReporter = new DummyUsageReporter("job_id", LOGGER);
        DummyStatusReporter statusReporter = new DummyStatusReporter(usageReporter);
        DummyJobDataPersister dp = new DummyJobDataPersister();

        pm.writeToJob(dd, ac, null, new TransformConfigs(Arrays.<TransformConfig>asList()), bis, bos, statusReporter, dp, LOGGER);
        ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());

        assertEquals(usageReporter.getTotalBytesRead(),
                data.getBytes(StandardCharsets.UTF_8).length - 1);
        assertEquals(8, usageReporter.getTotalRecordsRead() );
        assertEquals(8 * 5, usageReporter.getTotalFieldsRead() );


        assertEquals(8, statusReporter.getInputRecordCount() );
        assertEquals(8 * 5, statusReporter.getInputFieldCount() );
        assertEquals(8, statusReporter.getProcessedRecordCount() );
        assertEquals(8 * 3, statusReporter.getProcessedFieldCount() );
        assertEquals(0, statusReporter.getMissingFieldErrorCount());
        assertEquals(0, statusReporter.getDateParseErrorsCount());
        assertEquals(0, statusReporter.getOutOfOrderRecordCount());

        assertEquals(statusReporter.runningTotalStats(), statusReporter.incrementalStats());

        assertEquals(usageReporter.getTotalBytesRead(),
                data.getBytes(StandardCharsets.UTF_8).length - 1);
        assertEquals(usageReporter.getTotalBytesRead(),
                statusReporter.getBytesRead());

        assertEquals(dp.getRecordCount(), 8);

        // check header
        int numFields = bb.getInt();
        assertEquals(header.length, numFields);

        for (int i=0; i<numFields; i++)
        {
            int recordSize = bb.getInt();
            assertEquals(header[fieldMap[i]].length(), recordSize);
            byte [] charBuff = new byte[recordSize];
            for (int j=0; j<recordSize; j++)
            {
                charBuff[j] = bb.get();
            }

            String value = new String(charBuff, StandardCharsets.UTF_8);
            assertEquals(header[fieldMap[i]], value);
        }


        // check records
        for (String [] fields : records)
        {
            numFields = bb.getInt();
            assertEquals(fields.length, numFields);

            for (int i=0; i<numFields; i++)
            {
                int recordSize = bb.getInt();
                assertEquals(fields[fieldMap[i]].length(), recordSize);
                byte [] charBuff = new byte[recordSize];
                for (int j=0; j<recordSize; j++)
                {
                    charBuff[j] = bb.get();
                 }

                String value = new String(charBuff, StandardCharsets.UTF_8);
                assertEquals(fields[fieldMap[i]], value);
            }
        }
    }


    /**
     * In this test the input JSON documents have their fields in
     * an inconsistent changing order.
     */
    @Test
    public void differentFieldsOrderJsonToLengthEncoded() throws IOException,
            MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, MalformedJsonException
    {
        String data = "{\"timestamp\": \"1350824400\", \"airline\": \"DJA\", \"junk_field\": \"nonsense\", \"responsetime\": \"622\", \"sourcetype\": \"flightcentre\"}" +
                    "{\"junk_field\": \"nonsense\", \"airline\": \"JQA\", \"timestamp\": \"1350824401\", \"responsetime\": \"1742\", \"sourcetype\": \"flightcentre\"}" +
                    "{\"timestamp\": \"1350824402\", \"responsetime\": \"5339\", \"airline\": \"GAL\", \"sourcetype\": \"flightcentre\", \"junk_field\": \"nonsense\"}" +
                    "{\"airline\": \"GAL\", \"junk_field\": \"nonsense\", \"responsetime\": \"3893\", \"sourcetype\": \"flightcentre\", \"timestamp\": \"1350824403\"}" +
                    "{\"airline\": \"JQA\", \"timestamp\": \"1350824403\", \"junk_field\": \"nonsense\", \"responsetime\": \"9\", \"sourcetype\": \"flightcentre\"}" +
                    "{\"timestamp\": \"1350824404\", \"junk_field\": \"nonsense\", \"responsetime\": \"189\", \"sourcetype\": \"flightcentre\", \"airline\": \"DJA\"}" +
                    "{\"responsetime\": \"8\", \"timestamp\": \"1350824404\", \"airline\": \"JQA\", \"junk_field\": \"nonsense\", \"sourcetype\": \"flightcentre\"}" +
                    "{\"timestamp\": \"1350824404\", \"airline\": \"DJA\", \"junk_field\": \"nonsense\", \"responsetime\": \"1200\", \"sourcetype\": \"flightcentre\"}";

        // The . is the control field name
        String[] header = new String[] {"timestamp", "airline", "responsetime", "sourcetype", "."};
        // The last blank is the control field value
        String[][] records = new String[][] {{"1350824400", "DJA", "622", "flightcentre", ""},
                                                {"1350824401", "JQA", "1742", "flightcentre", ""},
                                                {"1350824402", "GAL", "5339", "flightcentre", ""},
                                                {"1350824403", "GAL", "3893", "flightcentre", ""},
                                                {"1350824403", "JQA", "9", "flightcentre", ""},
                                                {"1350824404", "DJA", "189", "flightcentre", ""},
                                                {"1350824404", "JQA", "8", "flightcentre", ""},
                                                {"1350824404", "DJA", "1200", "flightcentre", ""}};

        // The first output field is time then fields are written
        // alphabetical order then finally the control field
        int [] fieldMap = new int [] {0, 1, 2, 3, 4};

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.JSON);
        dd.setTimeField("timestamp");

        AnalysisConfig ac = new AnalysisConfig();
        Detector det = new Detector();
        det.setFieldName("responsetime");
        det.setByFieldName("airline");
        det.setPartitionFieldName("sourcetype");
        ac.setDetectors(Arrays.asList(det));

        Set<String> analysisFields = new TreeSet<String>(Arrays.asList(new String [] {
                "responsetime", "airline", "sourcetype"}));

        for (String s : ac.analysisFields())
        {
            assertTrue(analysisFields.contains(s));
        }


        ProcessManager pm = createProcessManager();

        ByteArrayInputStream bis =
                new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

        DummyUsageReporter usageReporter = new DummyUsageReporter("job_id", LOGGER);
        DummyStatusReporter statusReporter = new DummyStatusReporter(usageReporter);
        DummyJobDataPersister dp = new DummyJobDataPersister();

        pm.writeToJob(dd, ac, null, new TransformConfigs(Arrays.<TransformConfig>asList()), bis, bos, statusReporter, dp, LOGGER);
        ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());

        assertEquals(usageReporter.getTotalBytesRead(),
                data.getBytes(StandardCharsets.UTF_8).length - 1);

        assertEquals(usageReporter.getTotalBytesRead(),
                data.getBytes(StandardCharsets.UTF_8).length - 1);
        assertEquals(8, usageReporter.getTotalRecordsRead() );
        assertEquals(8 * 4, usageReporter.getTotalFieldsRead() );

        assertEquals(8, statusReporter.getInputRecordCount() );
        assertEquals(8 * 4, statusReporter.getInputFieldCount() );
        assertEquals(8, statusReporter.getProcessedRecordCount() );
        assertEquals(8 * 3, statusReporter.getProcessedFieldCount() );
        assertEquals(0, statusReporter.getMissingFieldErrorCount());
        assertEquals(0, statusReporter.getDateParseErrorsCount());
        assertEquals(0, statusReporter.getOutOfOrderRecordCount());


        assertEquals(dp.getRecordCount(), 8);

        // check header
        int numFields = bb.getInt();
        assertEquals(header.length, numFields);

        for (int i=0; i<numFields; i++)
        {
            int recordSize = bb.getInt();
            assertEquals(header[fieldMap[i]].length(), recordSize);
            byte [] charBuff = new byte[recordSize];
            for (int j=0; j<recordSize; j++)
            {
                charBuff[j] = bb.get();
            }

            String value = new String(charBuff, StandardCharsets.UTF_8);
            assertEquals(header[fieldMap[i]], value);
        }


        // check records
        for (String [] fields : records)
        {
            numFields = bb.getInt();
            assertEquals(fields.length, numFields);

            for (int i=0; i<numFields; i++)
            {
                int recordSize = bb.getInt();
                assertEquals(fields[fieldMap[i]].length(), recordSize);
                byte [] charBuff = new byte[recordSize];
                for (int j=0; j<recordSize; j++)
                {
                    charBuff[j] = bb.get();
                 }

                String value = new String(charBuff, StandardCharsets.UTF_8);
                assertEquals(fields[fieldMap[i]], value);
            }
        }
    }


    /**
     * JSON documents have missing fields. Test conversion is robust and
     * passes records with empty strings for the missing fields.
     */
    @Test
    public void jsonMissingFieldsToLengthEncoded() throws IOException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
            {
        // Document fields are not in the same order
        String dateFormatData = "{\"timestamp\": \"2012-10-21T14:00:00+01:00\", \"airline\": \"DJA\", \"responsetime\": \"622\"}" +
                "{\"timestamp\": \"2012-10-21T14:00:01+01:00\", \"airline\": \"JQA\", \"responsetime\": \"1742\", \"sourcetype\": \"flightcentre\"}" +
                "{\"timestamp\": \"2012-10-21T14:00:02+01:00\", \"airline\": \"GAL\",                             \"sourcetype\": \"flightcentre\"}" +
                "{\"timestamp\": \"2012-10-21T14:00:03+01:00\", \"airline\": \"GAL\", \"responsetime\": \"3893\", \"sourcetype\": \"flightcentre\"}" +
                "{\"timestamp\": \"2012-10-21T14:00:03+01:00\",                          \"responsetime\": \"9\", \"sourcetype\": \"flightcentre\"}" +
                "{\"timestamp\": \"2012-10-21T14:00:04+01:00\", \"airline\": \"DJA\", \"responsetime\": \"189\", \"sourcetype\": \"flightcentre\"}" +
                "{\"timestamp\": \"2012-10-21T14:00:04+01:00\", \"airline\": \"JQA\", \"responsetime\": \"8\", \"sourcetype\": \"flightcentre\"}" +
                "{\"timestamp\": \"2012-10-21T14:00:04+01:00\", \"airline\": \"DJA\", \"responsetime\": \"1200\", \"sourcetype\": \"flightcentre\"}";

        String epochFormatData = "{\"timestamp\": 1350824400, \"airline\": \"DJA\", \"responsetime\": \"622\"}" +
                "{\"timestamp\": 1350824401, \"airline\": \"JQA\", \"responsetime\": \"1742\", \"sourcetype\": \"flightcentre\"}" +
                "{\"timestamp\": 1350824402, \"airline\": \"GAL\",                             \"sourcetype\": \"flightcentre\"}" +
                "{\"timestamp\": 1350824403, \"airline\": \"GAL\", \"responsetime\": \"3893\", \"sourcetype\": \"flightcentre\"}" +
                "{\"timestamp\": 1350824403,                          \"responsetime\": \"9\", \"sourcetype\": \"flightcentre\"}" +
                "{\"timestamp\": 1350824404, \"airline\": \"DJA\", \"responsetime\": \"189\", \"sourcetype\": \"flightcentre\"}" +
                "{\"timestamp\": 1350824404, \"airline\": \"JQA\", \"responsetime\": \"8\", \"sourcetype\": \"flightcentre\"}" +
                "{\"timestamp\": 1350824404, \"airline\": \"DJA\", \"responsetime\": \"1200\", \"sourcetype\": \"flightcentre\"}";

        String epochMsFormatData = "{\"timestamp\": 1350824400000, \"airline\": \"DJA\", \"responsetime\": \"622\"}" +
                "{\"timestamp\": 1350824401000, \"airline\": \"JQA\", \"responsetime\": \"1742\", \"sourcetype\": \"flightcentre\"}" +
                "{\"timestamp\": 1350824402000, \"airline\": \"GAL\",                             \"sourcetype\": \"flightcentre\"}" +
                "{\"timestamp\": 1350824403000, \"airline\": \"GAL\", \"responsetime\": \"3893\", \"sourcetype\": \"flightcentre\"}" +
                "{\"timestamp\": 1350824403000,                          \"responsetime\": \"9\", \"sourcetype\": \"flightcentre\"}" +
                "{\"timestamp\": 1350824404500, \"airline\": \"DJA\", \"responsetime\": \"189\", \"sourcetype\": \"flightcentre\"}" +
                "{\"timestamp\": 1350824404400, \"airline\": \"JQA\", \"responsetime\": \"8\", \"sourcetype\": \"flightcentre\"}" +
                "{\"timestamp\": 1350824404200, \"airline\": \"DJA\", \"responsetime\": \"1200\", \"sourcetype\": \"flightcentre\"}";


        // The . is the control field name
        String[] header = new String[] {"timestamp", "airline", "responsetime", "sourcetype", "."};
        // The last blank is the control field value
        String[][] records = new String[][] {{"1350824400", "DJA", "622", "", ""},
                {"1350824401", "JQA", "1742", "flightcentre", ""},
                {"1350824402", "GAL", "", "flightcentre", ""},
                {"1350824403", "GAL", "3893", "flightcentre", ""},
                {"1350824403", "", "9", "flightcentre", ""},
                {"1350824404", "DJA", "189", "flightcentre", ""},
                {"1350824404", "JQA", "8", "flightcentre", ""},
                {"1350824404", "DJA", "1200", "flightcentre", ""}};


        // The first output field is time then fields are written
        // alphabetical order then finally the control field
        int [] fieldMap = new int [] {0, 1, 2, 3, 4};

        DataDescription dateFormatDD = new DataDescription();
        dateFormatDD.setFormat(DataFormat.JSON);
        dateFormatDD.setTimeField("timestamp");
        dateFormatDD.setTimeFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

        DataDescription epochFormatDD = new DataDescription();
        epochFormatDD.setFormat(DataFormat.JSON);
        epochFormatDD.setTimeField("timestamp");

        DataDescription epochMsFormatDD = new DataDescription();
        epochMsFormatDD.setFormat(DataFormat.JSON);
        epochMsFormatDD.setTimeField("timestamp");
        epochMsFormatDD.setTimeFormat("epoch_ms");

        AnalysisConfig ac = new AnalysisConfig();
        Detector det = new Detector();
        det.setFieldName("responsetime");
        det.setByFieldName("airline");
        det.setPartitionFieldName("sourcetype");
        ac.setDetectors(Arrays.asList(det));


        Set<String> analysisFields = new TreeSet<String>(Arrays.asList(new String [] {
                "responsetime", "airline", "sourcetype"}));

        for (String s : ac.analysisFields())
        {
            assertTrue(analysisFields.contains(s));
        }

        DataDescription [] dds = new DataDescription [] {dateFormatDD, epochFormatDD,
                epochMsFormatDD};

        ProcessManager pm = createProcessManager();

        int count = 0;
        for (String data : new String [] {dateFormatData, epochFormatData, epochMsFormatData})
        {
            ByteArrayInputStream bis =
                    new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

            DummyUsageReporter usageReporter = new DummyUsageReporter("job_id", LOGGER);
            DummyStatusReporter statusReporter = new DummyStatusReporter(usageReporter);
            DummyJobDataPersister dp = new DummyJobDataPersister();

            DataDescription dd = dds[count++];

            pm.writeToJob(dd, ac, null, new TransformConfigs(Arrays.<TransformConfig>asList()), bis, bos, statusReporter, dp, LOGGER);
            ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());

            assertEquals(usageReporter.getTotalBytesRead(),
                    data.getBytes(StandardCharsets.UTF_8).length - 1);

            assertEquals(usageReporter.getTotalBytesRead(),
                    data.getBytes(StandardCharsets.UTF_8).length - 1);
            assertEquals(8, usageReporter.getTotalRecordsRead() );
            assertEquals(7 + 7 + 7, usageReporter.getTotalFieldsRead() );

            assertEquals(8, statusReporter.getInputRecordCount() );
            assertEquals(7 + 7 + 7, statusReporter.getInputFieldCount() );
            assertEquals(8, statusReporter.getProcessedRecordCount() );
            assertEquals(8 * 3 -3, statusReporter.getProcessedFieldCount() );
            assertEquals(3, statusReporter.getMissingFieldErrorCount());
            assertEquals(0, statusReporter.getDateParseErrorsCount());
            assertEquals(0, statusReporter.getOutOfOrderRecordCount());


            assertEquals(dp.getRecordCount(), 8);

            // check header
            int numFields = bb.getInt();
            assertEquals(header.length, numFields);
            for (int i=0; i<numFields; i++)
            {
                int recordSize = bb.getInt();
                assertEquals(header[fieldMap[i]].length(), recordSize);
                byte [] charBuff = new byte[recordSize];
                for (int j=0; j<recordSize; j++)
                {
                    charBuff[j] = bb.get();
                }

                String value = new String(charBuff, StandardCharsets.UTF_8);
                assertEquals(header[fieldMap[i]], value);
            }


            // check records
            for (String [] fields : records)
            {
                numFields = bb.getInt();
                assertEquals(fields.length, numFields);

                for (int i=0; i<numFields; i++)
                {
                    int recordSize = bb.getInt();
                    assertEquals(fields[fieldMap[i]].length(), recordSize);
                    byte [] charBuff = new byte[recordSize];
                    for (int j=0; j<recordSize; j++)
                    {
                        charBuff[j] = bb.get();
                    }

                    String value = new String(charBuff, StandardCharsets.UTF_8);

                    assertEquals(fields[fieldMap[i]], value);
                }
            }
        }
    }


    /**
     * JSON docs can contain nested documents. Test they are parsed properly
     */
    @Test
    public void nestedObjectTest() throws JsonParseException, MissingFieldException, IOException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        String epochData = "{\"name\":\"my.test.metric1\",\"tags\":{\"tag1\":\"blah\",\"tag2\":\"boo\"},\"time\":1350824400,\"value\":12345.678}"
                + "{\"name\":\"my.test.metric2\",\"tags\":{\"tag1\":\"blaah\",\"tag2\":\"booo\"},\"time\":1350824401,\"value\":12345.678}"
                + "{\"name\":\"my.test.metric3\",\"tags\":{\"tag1\":\"blaaah\",\"tag2\":\"boooo\"},\"time\":1350824402,\"value\":12345.678}"
                + "{\"name\":\"my.test.metric4\",\"time\":1350824402,\"value\":12345.678}";

        String timeFormatData = "{\"name\":\"my.test.metric1\",\"tags\":{\"tag1\":\"blah\",\"tag2\":\"boo\"},\"time\":\"2012-10-21T14:00:00+01:00\",\"value\":12345.678}"
                + "{\"name\":\"my.test.metric2\",\"tags\":{\"tag1\":\"blaah\",\"tag2\":\"booo\"},\"time\":\"2012-10-21T14:00:01+01:00\",\"value\":12345.678}"
                + "{\"name\":\"my.test.metric3\",\"tags\":{\"tag1\":\"blaaah\",\"tag2\":\"boooo\"},\"time\":\"2012-10-21T14:00:02+01:00\",\"value\":12345.678}"
                + "{\"name\":\"my.test.metric4\",\"time\":\"2012-10-21T14:00:02+01:00\",\"value\":12345.678}";

        String epochMsData = "{\"name\":\"my.test.metric1\",\"tags\":{\"tag1\":\"blah\",\"tag2\":\"boo\"},\"time\":1350824400000,\"value\":12345.678}"
                + "{\"name\":\"my.test.metric2\",\"tags\":{\"tag1\":\"blaah\",\"tag2\":\"booo\"},\"time\":1350824401000,\"value\":12345.678}"
                + "{\"name\":\"my.test.metric3\",\"tags\":{\"tag1\":\"blaaah\",\"tag2\":\"boooo\"},\"time\":1350824402000,\"value\":12345.678}"
                + "{\"name\":\"my.test.metric4\",\"time\":1350824402000,\"value\":12345.678}";

        // The . is the control field name
        String[] header = new String[] {"time", "name", "value", "tags.tag2", "."};
        // The last blank is the control field value
        String[][] records = new String[][] {{"1350824400", "my.test.metric1", "12345.678", "boo", ""},
                                                {"1350824401", "my.test.metric2", "12345.678", "booo", ""},
                                                {"1350824402", "my.test.metric3", "12345.678", "boooo", ""},
                                                {"1350824402", "my.test.metric4", "12345.678", "", ""}};

        // The first output field is time then fields are written
        // alphabetical order then finally the control field
        int [] fieldMap = new int [] {0, 1, 3, 2, 4};

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.JSON);
        dd.setTimeField("time");
        dd.setTimeFormat("epoch");

        AnalysisConfig ac = new AnalysisConfig();
        Detector det = new Detector();
        det.setFieldName("name");
        det.setByFieldName("value");
        det.setPartitionFieldName("tags.tag2");
        ac.setDetectors(Arrays.asList(det));

        Set<String> analysisFields = new TreeSet<String>(Arrays.asList(new String [] {
                "name", "value", "tags.tag2"}));

        for (String s : ac.analysisFields())
        {
            assertTrue(analysisFields.contains(s));
        }


        int loop = 0;
        for (String data : new String [] {epochData, timeFormatData, epochMsData})
        {
            if (loop == 1)
            {
                dd.setTimeFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
            }
            else if (loop == 2)
            {
                dd.setTimeFormat("epoch_ms");
            }
            loop++;

            ProcessManager pm = createProcessManager();

            ByteArrayInputStream bis =
                    new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

            DummyUsageReporter usageReporter = new DummyUsageReporter("job_id", LOGGER);
            DummyStatusReporter statusReporter = new DummyStatusReporter(usageReporter);
            DummyJobDataPersister dp = new DummyJobDataPersister();

            pm.writeToJob(dd, ac, null, new TransformConfigs(Arrays.<TransformConfig>asList()), bis, bos, statusReporter, dp, LOGGER);
            ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());

            assertEquals(usageReporter.getTotalBytesRead(),
                    data.getBytes(StandardCharsets.UTF_8).length - 1);

            assertEquals(usageReporter.getTotalBytesRead(),
                    data.getBytes(StandardCharsets.UTF_8).length - 1);
            assertEquals(4, usageReporter.getTotalRecordsRead() );
            assertEquals(14 , usageReporter.getTotalFieldsRead() );

            assertEquals(4, statusReporter.getInputRecordCount() );
            assertEquals(14, statusReporter.getInputFieldCount() );
            assertEquals(4, statusReporter.getProcessedRecordCount() );
            assertEquals(4 * 3 - 1, statusReporter.getProcessedFieldCount() );
            assertEquals(1, statusReporter.getMissingFieldErrorCount());
            assertEquals(0, statusReporter.getDateParseErrorsCount());
            assertEquals(0, statusReporter.getOutOfOrderRecordCount());


            assertEquals(dp.getRecordCount(), 4);

            // check header
            int numFields = bb.getInt();
            assertEquals(header.length, numFields);

            for (int i=0; i<numFields; i++)
            {
                int recordSize = bb.getInt();
                assertEquals(header[fieldMap[i]].length(), recordSize);
                byte [] charBuff = new byte[recordSize];
                for (int j=0; j<recordSize; j++)
                {
                    charBuff[j] = bb.get();
                }

                String value = new String(charBuff, StandardCharsets.UTF_8);
                assertEquals(header[fieldMap[i]], value);
            }

            // check records
            for (String [] fields : records)
            {
                numFields = bb.getInt();
                assertEquals(fields.length, numFields);

                for (int i=0; i<numFields; i++)
                {
                    int recordSize = bb.getInt();
                    assertEquals(fields[fieldMap[i]].length(), recordSize);
                    byte [] charBuff = new byte[recordSize];
                    for (int j=0; j<recordSize; j++)
                    {
                        charBuff[j] = bb.get();
                    }

                    String value = new String(charBuff, StandardCharsets.UTF_8);
                    assertEquals(fields[fieldMap[i]], value);
                }
            }
        }
    }

    /**
     * Test deeply nested docs
     */
    @Test
    public void moreNestedFieldsTest() throws JsonParseException, MissingFieldException,
            IOException, HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        String data = "{\"name\":\"my.test.metric1\",\"tags\":{\"tag1\":{\"key1\":\"value1\"}, \"tag2\":\"boo\"},\"time\":1350824400,\"value\":12345.678}"
                + "{\"name\":\"my.test.metric2\",\"tags\":{\"tag1\":{\"key1\":\"value1\"}, \"tag2\":\"booo\"},\"time\":1350824401,\"value\":12345.678}"
                + "{\"name\":\"my.test.metric3\",\"tags\":{\"tag1\":{\"key1\":\"value1\"}, \"tag2\":\"boooo\"},\"time\":1350824402,\"value\":12345.678}"
                + "{\"name\":\"my.test.metric4\",\"time\":1350824402,\"value\":12345.678}";


        // The . is the control field name
        String[] header = new String[] {"time", "name", "value", "tags.tag2", "tags.tag1.key1", "."};
        // The last blank is the control field value
        String[][] records = new String[][] {{"1350824400", "my.test.metric1", "12345.678", "boo", "value1", ""},
                                                {"1350824401", "my.test.metric2", "12345.678", "booo", "value1", ""},
                                                {"1350824402", "my.test.metric3", "12345.678", "boooo",  "value1", ""},
                                                {"1350824402", "my.test.metric4", "12345.678", "", "", ""}};


        // The first output field is time then fields are written
        // alphabetical order then finally the control field
        int [] fieldMap = new int [] {0, 1, 4, 3, 2, 5};

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.JSON);
        dd.setTimeField("time");
        dd.setTimeFormat("epoch");

        AnalysisConfig ac = new AnalysisConfig();
        Detector det = new Detector();
        det.setFieldName("name");
        det.setByFieldName("value");
        det.setPartitionFieldName("tags.tag2");

        Detector det2 = new Detector();
        det2.setFieldName("name");
        det2.setByFieldName("value");
        det2.setPartitionFieldName("tags.tag1.key1");

        ac.setDetectors(Arrays.asList(det, det2));

        Set<String> analysisFields = new TreeSet<String>(Arrays.asList(new String [] {
                "name", "value", "tags.tag2", "tags.tag1.key1"}));

        for (String s : ac.analysisFields())
        {
            assertTrue(analysisFields.contains(s));
        }

        ProcessManager pm = createProcessManager();

        ByteArrayInputStream bis =
                new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

        DummyUsageReporter usageReporter = new DummyUsageReporter("job_id", LOGGER);
        DummyStatusReporter statusReporter = new DummyStatusReporter(usageReporter);
        DummyJobDataPersister dp = new DummyJobDataPersister();

        pm.writeToJob(dd, ac, null, new TransformConfigs(Arrays.<TransformConfig>asList()), bis, bos, statusReporter, dp, LOGGER);
        ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());

        assertEquals(usageReporter.getTotalBytesRead(),
                data.getBytes(StandardCharsets.UTF_8).length - 1);
        assertEquals(usageReporter.getTotalBytesRead(), statusReporter.getBytesRead());
        assertEquals(4, usageReporter.getTotalRecordsRead() );
        assertEquals(14 , usageReporter.getTotalFieldsRead() );

        assertEquals(4, statusReporter.getInputRecordCount() );
        assertEquals(14, statusReporter.getInputFieldCount() );
        assertEquals(4, statusReporter.getProcessedRecordCount() );
        assertEquals(14, statusReporter.getProcessedFieldCount() );
        assertEquals(2, statusReporter.getMissingFieldErrorCount());
        assertEquals(0, statusReporter.getDateParseErrorsCount());
        assertEquals(0, statusReporter.getOutOfOrderRecordCount());

        assertEquals(statusReporter.runningTotalStats(), statusReporter.incrementalStats());

        assertEquals(dp.getRecordCount(), 4);

        // check header
        int numFields = bb.getInt();
        assertEquals(header.length, numFields);


        for (int i=0; i<numFields; i++)
        {
            int recordSize = bb.getInt();
            assertEquals(header[fieldMap[i]].length(), recordSize);
            byte [] charBuff = new byte[recordSize];
            for (int j=0; j<recordSize; j++)
            {
                charBuff[j] = bb.get();
            }

            String value = new String(charBuff, StandardCharsets.UTF_8);
            assertEquals(header[fieldMap[i]], value);
        }

        // check records
        for (String [] fields : records)
        {
            numFields = bb.getInt();
            assertEquals(fields.length, numFields);

            for (int i=0; i<numFields; i++)
            {
                int recordSize = bb.getInt();
                assertEquals(fields[fieldMap[i]].length(), recordSize);
                byte [] charBuff = new byte[recordSize];
                for (int j=0; j<recordSize; j++)
                {
                    charBuff[j] = bb.get();
                }

                String value = new String(charBuff, StandardCharsets.UTF_8);
                assertEquals(fields[fieldMap[i]], value);
            }
        }
    }


    /**
     * Test converting epoch times with a fraction of a second component.
     */
    @Test
    public void epochWithFractionTest() throws JsonParseException, MissingFieldException,
            IOException, HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        String epochData = "{\"name\":\"my.test.metric1\",\"tags\":{\"tag1\":\"blah\",\"tag2\":\"boo\"},\"time\":1350824400.4543154,\"value\":12345.678}"
                + "{\"name\":\"my.test.metric2\",\"tags\":{\"tag1\":\"blaah\",\"tag2\":\"booo\"},\"time\":1350824401.834431,\"value\":12345.678}"
                + "{\"name\":\"my.test.metric3\",\"tags\":{\"tag1\":\"blaaah\",\"tag2\":\"boooo\"},\"time\":1350824402.4864133,\"value\":12345.678}"
                + "{\"name\":\"my.test.metric4\",\"time\":1350824402.4846513,\"value\":12345.678}";

        String epochMsData = "{\"name\":\"my.test.metric1\",\"tags\":{\"tag1\":\"blah\",\"tag2\":\"boo\"},\"time\":1350824400000.484313,\"value\":12345.678}"
                + "{\"name\":\"my.test.metric2\",\"tags\":{\"tag1\":\"blaah\",\"tag2\":\"booo\"},\"time\":1350824401000.486138,\"value\":12345.678}"
                + "{\"name\":\"my.test.metric3\",\"tags\":{\"tag1\":\"blaaah\",\"tag2\":\"boooo\"},\"time\":1350824402000.4831844,\"value\":12345.678}"
                + "{\"name\":\"my.test.metric4\",\"time\":1350824402000.45633447,\"value\":12345.678}";

        String isoFormatData = "{\"name\":\"my.test.metric1\",\"tags\":{\"tag1\":\"blah\",\"tag2\":\"boo\"},\"time\":\"2012-10-21 13:00:00Z\",\"value\":12345.678}"
                + "{\"name\":\"my.test.metric2\",\"tags\":{\"tag1\":\"blaah\",\"tag2\":\"booo\"},\"time\":\"2012-10-21 13:00:01Z\",\"value\":12345.678}"
                + "{\"name\":\"my.test.metric3\",\"tags\":{\"tag1\":\"blaaah\",\"tag2\":\"boooo\"},\"time\":\"2012-10-21 13:00:02Z\",\"value\":12345.678}"
                + "{\"name\":\"my.test.metric4\",\"time\":\"2012-10-21 13:00:02Z\",\"value\":12345.678}";

        // The . is the control field name
        String[] header = new String[] {"time", "name", "value", "tags.tag2", "."};
        // The last blank is the control field value
        String[][] records = new String[][] {{"1350824400", "my.test.metric1", "12345.678", "boo", ""},
                                                {"1350824401", "my.test.metric2", "12345.678", "booo", ""},
                                                {"1350824402", "my.test.metric3", "12345.678", "boooo", ""},
                                                {"1350824402", "my.test.metric4", "12345.678", "", ""}};

        // The first output field is time then fields are written
        // alphabetical order then finally the control field
        int [] fieldMap = new int [] {0, 1, 3, 2, 4};

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.JSON);
        dd.setTimeField("time");
        dd.setTimeFormat("epoch");

        AnalysisConfig ac = new AnalysisConfig();
        Detector det = new Detector();
        det.setFieldName("name");
        det.setByFieldName("value");
        det.setPartitionFieldName("tags.tag2");
        ac.setDetectors(Arrays.asList(det));

        Set<String> analysisFields = new TreeSet<String>(Arrays.asList(new String [] {
                "name", "value", "tags.tag2"}));

        for (String s : ac.analysisFields())
        {
            assertTrue(analysisFields.contains(s));
        }

        int loop = 0;
        for (String data : new String [] {epochData,  epochMsData, isoFormatData})
        {
            if (loop == 0)
            {
                dd.setTimeFormat("epoch");
            }
            else if (loop == 1)
            {
                dd.setTimeFormat("epoch_ms");
            }
            else if (loop == 2)
            {
                dd.setTimeFormat("yyyy-MM-dd HH:mm:ssX");
            }
            loop++;

            ProcessManager pm = createProcessManager();

            ByteArrayInputStream bis =
                    new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

            DummyUsageReporter usageReporter = new DummyUsageReporter("job_id", LOGGER);
            DummyStatusReporter statusReporter = new DummyStatusReporter(usageReporter);
            DummyJobDataPersister dp = new DummyJobDataPersister();

            pm.writeToJob(dd, ac, null, new TransformConfigs(Arrays.<TransformConfig>asList()), bis, bos, statusReporter, dp, LOGGER);
            ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());

            assertEquals(usageReporter.getTotalBytesRead(),
                    data.getBytes(StandardCharsets.UTF_8).length - 1);
            assertEquals(4, usageReporter.getTotalRecordsRead() );
            assertEquals(14 , usageReporter.getTotalFieldsRead() );

            assertEquals(usageReporter.getTotalBytesRead(), statusReporter.getBytesRead());
            assertEquals(4, statusReporter.getInputRecordCount() );
            assertEquals(14, statusReporter.getInputFieldCount() );
            assertEquals(4, statusReporter.getProcessedRecordCount() );
            assertEquals(4 * 3 - 1, statusReporter.getProcessedFieldCount() );
            assertEquals(1, statusReporter.getMissingFieldErrorCount());
            assertEquals(0, statusReporter.getDateParseErrorsCount());
            assertEquals(0, statusReporter.getOutOfOrderRecordCount());

            assertEquals(dp.getRecordCount(), 4);

            // check header
            int numFields = bb.getInt();
            assertEquals(header.length, numFields);

            for (int i=0; i<numFields; i++)
            {
                int recordSize = bb.getInt();
                assertEquals(header[fieldMap[i]].length(), recordSize);
                byte [] charBuff = new byte[recordSize];
                for (int j=0; j<recordSize; j++)
                {
                    charBuff[j] = bb.get();
                }

                String value = new String(charBuff, StandardCharsets.UTF_8);
                assertEquals(header[fieldMap[i]], value);
            }

            // check records
            for (String [] fields : records)
            {
                numFields = bb.getInt();
                assertEquals(fields.length, numFields);

                for (int i=0; i<numFields; i++)
                {
                    int recordSize = bb.getInt();
                    byte [] charBuff = new byte[recordSize];
                    for (int j=0; j<recordSize; j++)
                    {
                        charBuff[j] = bb.get();
                    }

                    String value = new String(charBuff, StandardCharsets.UTF_8);
                    assertEquals(fields[fieldMap[i]].length(), recordSize);
                    assertEquals(fields[fieldMap[i]], value);
                }
            }
        }
    }

    private static ProcessManager createProcessManager()
    {
        return new ProcessManager(null, null, null, mock(JobLoggerFactory.class));
    }
}
