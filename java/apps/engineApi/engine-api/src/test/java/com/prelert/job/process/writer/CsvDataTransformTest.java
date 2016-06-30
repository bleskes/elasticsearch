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
import static org.junit.Assert.fail;

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

public class CsvDataTransformTest
{
    private static Logger LOGGER = Logger.getLogger(CsvDataTransformTest.class);

    /**
     * Test transforming csv data with time in epoch format
     */
    @Test
    public void plainCSVToLengthEncoded() throws IOException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        String data = "airline,responsetime,sourcetype,time\n" +
                    "DJA,622,flightcentre,1350824400\n" +
                    "JQA,1742,flightcentre,1350824401\n" +
                    "GAL,5339,flightcentre,1350824402\n" +
                    "GAL,3893,flightcentre,1350824403\n" +
                    "JQA,9,flightcentre,1350824403\n" +
                    "DJA,189,flightcentre,1350824404\n" +
                    "JQA,8,flightcentre,1350824404\n" +
                    "DJA,1200,flightcentre,1350824404";

        AnalysisConfig ac = new AnalysisConfig();
        Detector d = new Detector();
        d.setFieldName("responsetime");
        d.setByFieldName("airline");
        d.setPartitionFieldName("sourcetype");
        ac.setDetectors(Arrays.asList(d));


        // data is written in the order of the required fields
        // which is alphabetical but with time as the first element
        int [] fieldMap = new int [] {3, 0, 1, 2};

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

        DataDescription dd = new DataDescription();
        dd.setFieldDelimiter(',');
        DummyUsageReporter usageReporter = new DummyUsageReporter("job_id", LOGGER);
        DummyStatusReporter statusReporter = new DummyStatusReporter(usageReporter);
        DummyJobDataPersister dataPersister = new DummyJobDataPersister();

        pm.writeToJob(dd, ac, null, new TransformConfigs(Arrays.<TransformConfig>asList()), bis, bos, statusReporter, dataPersister, LOGGER);

        ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());

        assertEquals(usageReporter.getTotalBytesRead(),
                data.getBytes(StandardCharsets.UTF_8).length - 2);
        assertEquals(8 * 3, usageReporter.getTotalFieldsRead());
        assertEquals(8, usageReporter.getTotalRecordsRead());


        assertEquals(dataPersister.getRecordCount(), 8);

        assertEquals(usageReporter.getTotalBytesRead(), statusReporter.getBytesRead());
        assertEquals(8 * 3, usageReporter.getTotalFieldsRead());
        assertEquals(8, usageReporter.getTotalRecordsRead());

        assertEquals(usageReporter.getTotalFieldsRead(), statusReporter.getInputFieldCount() );
        assertEquals(usageReporter.getTotalRecordsRead(), statusReporter.getInputRecordCount() );
        assertEquals(8 * 3, statusReporter.getProcessedFieldCount() );
        assertEquals(8, statusReporter.getProcessedRecordCount() );
        assertEquals(0, statusReporter.getMissingFieldErrorCount());
        assertEquals(0, statusReporter.getDateParseErrorsCount());
        assertEquals(0, statusReporter.getOutOfOrderRecordCount());

        assertEquals(statusReporter.runningTotalStats(), statusReporter.incrementalStats());

        String [] lines = data.split("\\n");

        boolean isHeader = true;
        for (String line : lines)
        {
            int numFields = bb.getInt();
            String [] fields = line.split(",");
            // The + 1 is for the control field
            assertEquals(fields.length + 1, numFields);

            for (int i = 0; i < numFields - 1; i++)
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
            // Control field
            int recordSize = bb.getInt();
            assertEquals(isHeader ? 1 : 0, recordSize);
            if (isHeader)
            {
                bb.get();
                isHeader = false;
            }
        }
    }

    /**
     * Test transforming csv data with time in epoch format
     * and a non-standard quote character
     */
    @Test
    public void quotedCSVToLengthEncoded() throws IOException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        // ? is the quote char
        String data = "airline,responsetime,sourcetype,time\n" +
                    "DJA,622,?flight,centre?,1350824400\n" +
                    "JQA,1742,?flightcentre?,1350824401\n" +
                    "GAL,5339,?flight\ncentre?,1350824402\n" +
                    "GAL,3893,flightcentre,1350824403";

        // same as the data above but split into fields
        // this is to test escaping the newline char in the quoted field
        String [][] lines = new String [] [] {{"airline","responsetime","sourcetype","time"},
                {"DJA","622","flight,centre","1350824400"},
                {"JQA","1742","flightcentre","1350824401"},
                {"GAL","5339","flight\ncentre","1350824402"},
                {"GAL","3893","flightcentre","1350824403"}};


        AnalysisConfig ac = new AnalysisConfig();
        Detector d = new Detector();
        d.setFieldName("responsetime");
        d.setByFieldName("airline");
        d.setPartitionFieldName("sourcetype");
        ac.setDetectors(Arrays.asList(d));


        // data is written in the order of the required fields
        // which is alphabetical but with time as the first element
        int [] fieldMap = new int [] {3, 0, 1, 2};

        Set<String> analysisFields = new TreeSet<String>(Arrays.asList(new String [] {
                "responsetime", "airline", "sourcetype"}));

        for (String s : ac.analysisFields())
        {
            assertTrue(analysisFields.contains(s));
        }

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setFieldDelimiter(',');
        dd.setQuoteCharacter('?');

        ProcessManager pm = createProcessManager();

        ByteArrayInputStream bis =
                new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

        DummyUsageReporter usageReporter = new DummyUsageReporter("job_id", LOGGER);
        DummyStatusReporter statusReporter = new DummyStatusReporter(usageReporter);
        DummyJobDataPersister dp = new DummyJobDataPersister();

        pm.writeToJob(dd, ac, null, new TransformConfigs(Arrays.<TransformConfig>asList()), bis, bos, statusReporter, dp, LOGGER);

        assertEquals(usageReporter.getTotalBytesRead(),
                data.getBytes(StandardCharsets.UTF_8).length - 2);
        assertEquals(4 * 3, usageReporter.getTotalFieldsRead());
        assertEquals(4, usageReporter.getTotalRecordsRead());

        assertEquals(dp.getRecordCount(), 4);

        assertEquals(usageReporter.getTotalBytesRead(), statusReporter.getBytesRead());
        assertEquals(4 * 3, statusReporter.getInputFieldCount() );
        assertEquals(4, statusReporter.getInputRecordCount() );
        assertEquals(4 * 3, statusReporter.getProcessedFieldCount() );
        assertEquals(4, statusReporter.getProcessedRecordCount() );
        assertEquals(0, statusReporter.getMissingFieldErrorCount());
        assertEquals(0, statusReporter.getDateParseErrorsCount());
        assertEquals(0, statusReporter.getOutOfOrderRecordCount());

        assertEquals(dp.getRecordCount(), 4);

        assertEquals(statusReporter.runningTotalStats(), statusReporter.incrementalStats());

        ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());

        boolean isHeader = true;
        for (int l=0; l<lines.length; l++)
        {
            String [] fields = lines[l];
            int numFields = bb.getInt();
            // The + 1 is for the control field
            assertEquals(fields.length + 1, numFields);
            for (int i = 0; i < numFields - 1; i++)
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
            // Control field
            int recordSize = bb.getInt();
            assertEquals(isHeader ? 1 : 0, recordSize);
            if (isHeader)
            {
                bb.get();
                isHeader = false;
            }
        }
    }

    /**
     * Test transforming csv data with a time format
     */
    @Test
    public void csvWithDateFormat() throws IOException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        // ? is the quote char
        String data = "date,airline,responsetime,sourcetype\n" +
                    "2014-01-28 00:00:00+00:00,AAL,132.2046,farequote\n" +
                    "2014-01-28 00:00:00+00:00,JZA,990.4628,farequote\n" +
                    "2014-01-28 00:00:00+00:00,JBU,877.5927,farequote\n" +
                    "2014-01-28 00:00:00+00:00,KLM,1355.4812,farequote\n" +
                    "2014-01-28 01:00:00+00:00,NKS,9991.3981,farequote\n" +
                    "2014-01-28 01:00:00+00:00,TRS,412.1948,farequote\n" +
                    "2014-01-28 01:00:00+00:00,DAL,401.4948,farequote\n" +
                    "2014-01-28 01:00:00+00:00,FFT,181.5529,farequote";

        String [] epochTimes = new String [] {"", "1390867200", "1390867200",
                "1390867200", "1390867200", "1390870800", "1390870800",
                "1390870800", "1390870800"};

        AnalysisConfig ac = new AnalysisConfig();
        Detector d = new Detector();
        d.setFieldName("responsetime");
        d.setByFieldName("airline");
        d.setPartitionFieldName("sourcetype");
        ac.setDetectors(Arrays.asList(d));


        // data is written in the order of the required fields
        // which is alphabetical but with time as the first element
        int [] fieldMap = new int [] {0, 1, 2, 3};

        Set<String> analysisFields = new TreeSet<String>(Arrays.asList(new String [] {
                "responsetime", "airline", "sourcetype"}));

        for (String s : ac.analysisFields())
        {
            assertTrue(analysisFields.contains(s));
        }

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setFieldDelimiter(',');
        dd.setTimeField("date");
        dd.setTimeFormat("yyyy-MM-dd HH:mm:ssXXX");


        ProcessManager pm = createProcessManager();

        ByteArrayInputStream bis =
                new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

        DummyUsageReporter usageReporter = new DummyUsageReporter("job_id", LOGGER);
        DummyStatusReporter statusReporter = new DummyStatusReporter(usageReporter);
        DummyJobDataPersister dp = new DummyJobDataPersister();

        pm.writeToJob(dd, ac, null, new TransformConfigs(Arrays.<TransformConfig>asList()), bis, bos, statusReporter, dp, LOGGER);
        ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());

        assertEquals(dp.getRecordCount(), 8);

        assertEquals(usageReporter.getTotalBytesRead(),
                data.getBytes(StandardCharsets.UTF_8).length - 2);
        assertEquals(8 * 3, usageReporter.getTotalFieldsRead());
        assertEquals(8, usageReporter.getTotalRecordsRead());

        assertEquals(usageReporter.getTotalBytesRead(), statusReporter.getBytesRead());
        assertEquals(usageReporter.getTotalFieldsRead(), statusReporter.getInputFieldCount() );
        assertEquals(usageReporter.getTotalRecordsRead(), statusReporter.getInputRecordCount() );
        assertEquals(8 * 3, statusReporter.getProcessedFieldCount() );
        assertEquals(8, statusReporter.getProcessedRecordCount() );
        assertEquals(0, statusReporter.getMissingFieldErrorCount());
        assertEquals(0, statusReporter.getDateParseErrorsCount());
        assertEquals(0, statusReporter.getOutOfOrderRecordCount());

        assertEquals(statusReporter.runningTotalStats(), statusReporter.incrementalStats());

        String [] lines = data.split("\\n");

        boolean isHeader = true;
        for (int currentLine=0; currentLine<lines.length; currentLine++)
        {
            String [] fields = lines[currentLine].split(",");
            int numFields = bb.getInt();
            // The + 1 is for the control field
            assertEquals(fields.length + 1, numFields);

            final int DATE_FIELD = 0;
            for (int i = 0; i < numFields - 1; i++)
            {
                int recordSize = bb.getInt();

                if (isHeader == false && i == DATE_FIELD)
                {
                    assertEquals(epochTimes[currentLine].length(), recordSize);

                    byte [] charBuff = new byte[recordSize];
                    for (int j=0; j<recordSize; j++)
                    {
                        charBuff[j] = bb.get();
                    }

                    String value = new String(charBuff, StandardCharsets.UTF_8);
                    assertEquals(epochTimes[currentLine], value);
                }
                else
                {
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
            // Control field
            int recordSize = bb.getInt();
            assertEquals(isHeader ? 1 : 0, recordSize);
            if (isHeader)
            {
                bb.get();
                isHeader = false;
            }
        }
    }

    /**
     * Write CSV data with extra fields that should be filtered out
     */
    @Test
    public void plainCsvWithExtraFields() throws IOException, MissingFieldException,
        HighProportionOfBadTimestampsException, OutOfOrderRecordsException, MalformedJsonException
    {
        String data = "airline,responsetime,sourcetype,airport,time,baggage\n" +
                    "DJA,622,flightcentre,MAN,1350824400,none\n" +
                    "JQA,1742,flightcentre,GAT,1350824401,none\n" +
                    "GAL,5339,flightcentre,SYN,1350824402,some\n" +
                    "GAL,3893,flightcentre,CHM,1350824403,some\n" +
                    "JQA,9,flightcentre,CHM,1350824403,none\n" +
                    "DJA,189,flightcentre,GAT,1350824404,lost\n" +
                    "JQA,8,flightcentre,GAT,1350824404,none\n" +
                    "DJA,1200,flightcentre,MAN,1350824404,none";


        AnalysisConfig ac = new AnalysisConfig();
        Detector d = new Detector();
        d.setFieldName("responsetime");
        d.setByFieldName("airline");
        d.setPartitionFieldName("sourcetype");
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setFieldDelimiter(',');

        // data is written in the order of the required fields
        // which is alphabetical but with time as the first element
        int [] fieldMap = new int [] {4, 0, 1, 2};

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
                data.getBytes(StandardCharsets.UTF_8).length - 2);

        assertEquals(usageReporter.getTotalBytesRead(), statusReporter.getBytesRead());
        assertEquals(8 * 5, usageReporter.getTotalFieldsRead());
        assertEquals(8, usageReporter.getTotalRecordsRead());

        assertEquals(usageReporter.getTotalBytesRead(),
                statusReporter.getBytesRead());
        assertEquals(usageReporter.getTotalFieldsRead(), statusReporter.getInputFieldCount() );
        assertEquals(usageReporter.getTotalRecordsRead(), statusReporter.getInputRecordCount() );
        assertEquals(8 * 3, statusReporter.getProcessedFieldCount() );
        assertEquals(8, statusReporter.getProcessedRecordCount() );
        assertEquals(0, statusReporter.getMissingFieldErrorCount());
        assertEquals(0, statusReporter.getDateParseErrorsCount());
        assertEquals(0, statusReporter.getOutOfOrderRecordCount());

        assertEquals(statusReporter.runningTotalStats(), statusReporter.incrementalStats());

        assertEquals(dp.getRecordCount(), 8);

        String [] lines = data.split("\\n");

        boolean isHeader = true;
        for (String line : lines)
        {
            int numFields = bb.getInt();
            String [] fields = line.split(",");
            assertEquals(analysisFields.size() + 2, numFields);

            for (int i = 0; i < numFields - 1; i++)
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
            // Control field
            int recordSize = bb.getInt();
            assertEquals(isHeader ? 1 : 0, recordSize);
            if (isHeader)
            {
                bb.get();
                isHeader = false;
            }
        }
    }

    /**
     * Write CSV data with the time field missing this should throw a MissingFieldException
     */
    @Test
    public void plainCsvWithMissingTimeField() throws IOException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        // no time field
        String data = "airline,responsetime,airport,sourcetype,baggage\n" +
                    "DJA,622,flightcentre,MAN,none\n" +
                    "JQA,1742,flightcentre,GAT,none\n" +
                    "GAL,5339,flightcentre,SYN,some\n" +
                    "GAL,3893,flightcentre,CHM,some\n" +
                    "JQA,9,flightcentre,CHM,none\n" +
                    "DJA,189,flightcentre,GAT,lost\n" +
                    "JQA,8,flightcentre,GAT,none\n" +
                    "DJA,1200,flightcentre,MAN,none";

        AnalysisConfig ac = new AnalysisConfig();
        Detector d = new Detector();
        d.setFieldName("responsetime");
        d.setByFieldName("airline");
        d.setPartitionFieldName("sourcetype");
        ac.setDetectors(Arrays.asList(d));


        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setFieldDelimiter(',');

        ProcessManager pm = createProcessManager();

        ByteArrayInputStream bis =
                new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

        DummyUsageReporter usageReporter = new DummyUsageReporter("job_id", LOGGER);
        DummyStatusReporter statusReporter = new DummyStatusReporter(usageReporter);
        DummyJobDataPersister dp = new DummyJobDataPersister();

        try
        {
            pm.writeToJob(dd, ac, null, new TransformConfigs(Arrays.<TransformConfig>asList()),
                    bis, bos, statusReporter, dp, LOGGER);
            fail(); // should throw
        }
        catch (MissingFieldException e)
        {
            assertEquals(e.getMissingFieldName(), "time");
            assertEquals(statusReporter.getBytesRead(),
                    usageReporter.getBytesReadSinceLastReport());
        }

        // Do the same again but with a time format configured
        // so a different code path will be taken
        //
        dd.setTimeField("timestamp");
        dd.setTimeFormat(DataDescription.EPOCH_MS);


        bis = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        bos = new ByteArrayOutputStream(1024);

        usageReporter = new DummyUsageReporter("job_id", LOGGER);
        statusReporter = new DummyStatusReporter(usageReporter);
        try
        {
            pm.writeToJob(dd, ac, null, new TransformConfigs(Arrays.<TransformConfig>asList()), bis, bos, statusReporter, dp, LOGGER);
            assertTrue(false); // should throw
        }
        catch (MissingFieldException e)
        {
            assertEquals(e.getMissingFieldName(), "timestamp");

            assertEquals(usageReporter.getBytesReadSinceLastReport(), statusReporter.getBytesRead());
            assertEquals(0, usageReporter.getTotalFieldsRead());
            assertEquals(0, usageReporter.getTotalRecordsRead());

            assertEquals(0, statusReporter.getInputFieldCount() );
            assertEquals(0, statusReporter.getInputRecordCount() );
            assertEquals(0, statusReporter.getProcessedFieldCount() );
            assertEquals(0, statusReporter.getProcessedRecordCount() );
            assertEquals(0, statusReporter.getMissingFieldErrorCount());
            assertEquals(0, statusReporter.getDateParseErrorsCount());
            assertEquals(0, statusReporter.getOutOfOrderRecordCount());

            assertEquals(statusReporter.runningTotalStats(), statusReporter.incrementalStats());
        }
    }



    /**
     * Write CSV data with an analysis field missing this should throw a MissingFieldException
     */
    @Test
    public void plainCsvWithMissingField() throws IOException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        String data = "airline,responsetime,airport,sourcetype,time,baggage\n" +
                "DJA,622,flightcentre,MAN,1350824400,none\n" +
                "JQA,1742,flightcentre,GAT,1350824401,none\n" +
                "GAL,5339,flightcentre,SYN,1350824402,some\n" +
                "GAL,3893,flightcentre,CHM,1350824403,some\n" +
                "JQA,9,flightcentre,CHM,1350824403,none\n" +
                "DJA,189,flightcentre,GAT,1350824404,lost\n" +
                "JQA,8,flightcentre,GAT,1350824404,none\n" +
                "DJA,1200,flightcentre,MAN,1350824404,none";

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setFieldDelimiter(',');

        AnalysisConfig ac = new AnalysisConfig();
        Detector d = new Detector();
        d.setFieldName("responsetime");
        d.setByFieldName("airline");
        d.setPartitionFieldName("missing_field");
        ac.setDetectors(Arrays.asList(d));

        ProcessManager pm = createProcessManager();

        ByteArrayInputStream bis =
                new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);

        DummyUsageReporter usageReporter = new DummyUsageReporter("job_id", LOGGER);
        DummyStatusReporter statusReporter = new DummyStatusReporter(usageReporter);
        DummyJobDataPersister dp = new DummyJobDataPersister();


        try
        {
            pm.writeToJob(dd, ac, null, new TransformConfigs(Arrays.<TransformConfig>asList()), bis, bos, statusReporter, dp, LOGGER);
            assertTrue(false); // should throw
        }
        catch (MissingFieldException e)
        {
            assertEquals(e.getMissingFieldName(), "missing_field");

            assertEquals(usageReporter.getBytesReadSinceLastReport(), statusReporter.getBytesRead());
            assertEquals(0, usageReporter.getTotalFieldsRead());
            assertEquals(0, usageReporter.getTotalRecordsRead());

            assertEquals(0, statusReporter.getInputFieldCount() );
            assertEquals(0, statusReporter.getInputRecordCount() );
            assertEquals(0, statusReporter.getProcessedFieldCount() );
            assertEquals(0, statusReporter.getProcessedRecordCount() );
            assertEquals(0, statusReporter.getMissingFieldErrorCount());
            assertEquals(0, statusReporter.getDateParseErrorsCount());
            assertEquals(0, statusReporter.getOutOfOrderRecordCount());

            assertEquals(statusReporter.runningTotalStats(), statusReporter.incrementalStats());
        }

        // Do the same again but with a time format configured
        // so a different code path will be taken
        //
        dd.setTimeFormat(DataDescription.EPOCH_MS);

        bis = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        bos = new ByteArrayOutputStream(1024);

        usageReporter = new DummyUsageReporter("job_id", LOGGER);
        statusReporter = new DummyStatusReporter(usageReporter);

        try
        {
            pm.writeToJob(dd, ac, null, new TransformConfigs(Arrays.<TransformConfig>asList()), bis, bos, statusReporter, dp, LOGGER);
            assertTrue(false); // should throw
        }
        catch (MissingFieldException e)
        {
            assertEquals(e.getMissingFieldName(), "missing_field");
            assertEquals(usageReporter.getBytesReadSinceLastReport(),
                    statusReporter.getBytesRead());
        }
    }


    /**
     * Tests writing csv records where some records have
     * missing values. Tests for epoch, epoch_ms and timeformat
     */
    @Test
    public void plainCsvWithIncompleteRecords()
    throws IOException, MissingFieldException, HighProportionOfBadTimestampsException,
        OutOfOrderRecordsException, MalformedJsonException
    {
        String epoch_data = "time,airline,responsetime,sourcetype,airport,baggage\n" +
                "1350824400,DJA,622,flightcentre,MAN,none\n" +
                "1350824401,JQA,1742,,\n" +  // this field is't written
                "1350824402,GAL,,flightcentre,SYN,some\n" +
                "1350824403,GAL,3893,flightcentre,CHM,some\n" +
                "1350824403,\n" +   // 2 fields missing here
                "1350824404,DJA,189,flightcentre,GAT,lost";

        String epoch_ms_data = "time,airline,responsetime,sourcetype,airport,baggage\n" +
                "1350824400000,DJA,622,flightcentre,MAN,none\n" +
                "1350824401000,JQA,1742,,\n" + // this field is't written
                "1350824402000,GAL,,flightcentre,SYN,some\n" +
                "1350824403000,GAL,3893,flightcentre,CHM,some\n" +
                "1350824403000,\n" +   // 2 fields missing here
                "1350824404000,DJA,189,flightcentre,GAT,lost";

        String epoch_timeformat_data = "time,airline,responsetime,sourcetype,airport,baggage\n" +
                "2012-10-21 13:00:00 Z,DJA,622,flightcentre,MAN,none\n" +
                "2012-10-21 13:00:01 Z,JQA,1742,,\n" + // this field is't written
                "2012-10-21 13:00:02 Z,GAL,,flightcentre,SYN,some\n" +
                "2012-10-21 13:00:03 Z,GAL,3893,flightcentre,CHM,some\n" +
                "2012-10-21 13:00:03 Z,\n" +   // 2 fields missing here
                "2012-10-21 13:00:04 Z,DJA,189,flightcentre,GAT,lost";


        // data is written in the order of the required fields
        // which is alphabetical but with time as the first element
        int [] fieldMap = new int [] {0, 1, 3, 2};

        Set<String> analysisFields = new TreeSet<String>(Arrays.asList(new String [] {
                "responsetime", "airline", "baggage"}));


        String [][] lines = new String [] [] {{"time","airline", "responsetime","baggage"},
                {"1350824400", "DJA", "622", "none"},
                {"1350824401", "JQA", "1742", ""},
                {"1350824402", "GAL", "", "some"},
                {"1350824403", "GAL", "3893", "some"},
                {"1350824403", "", "", ""},
                {"1350824404", "DJA", "189", "lost"}};


        int loop = 0;
        for (String data : new String[] {epoch_data , epoch_ms_data, epoch_timeformat_data})
        {
            loop++;

            AnalysisConfig ac = new AnalysisConfig();
            Detector d = new Detector();
            d.setFieldName("responsetime");
            d.setByFieldName("airline");
            d.setPartitionFieldName("baggage");
            ac.setDetectors(Arrays.asList(d));

            DataDescription dd = new DataDescription();
            dd.setFormat(DataFormat.DELIMITED);
            dd.setFieldDelimiter(',');
            if (loop == 2)
            {
                dd.setTimeFormat("epoch_ms");
            }
            else if (loop == 3)
            {
                dd.setTimeFormat("yyyy-MM-dd HH:mm:ss X");
            }

            for (String s : ac.analysisFields())
            {
                assertTrue(analysisFields.contains(s));
            }

            ProcessManager pm = createProcessManager();

            ByteArrayInputStream bis =
                    new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream bos = new ByteArrayOutputStream(128);

            DummyUsageReporter usageReporter = new DummyUsageReporter("job_id", LOGGER);
            DummyStatusReporter statusReporter = new DummyStatusReporter(usageReporter);
            DummyJobDataPersister dp = new DummyJobDataPersister();

            pm.writeToJob(dd, ac, null, new TransformConfigs(Arrays.<TransformConfig>asList()), bis, bos, statusReporter, dp, LOGGER);

            assertEquals(usageReporter.getTotalBytesRead(),
                    data.getBytes(StandardCharsets.UTF_8).length - 2);
            assertEquals(6, usageReporter.getTotalRecordsRead() );
            assertEquals(6 * 5, usageReporter.getTotalFieldsRead() );

            assertEquals(usageReporter.getTotalBytesRead(), statusReporter.getBytesRead());
            assertEquals(usageReporter.getTotalRecordsRead(), statusReporter.getInputRecordCount() );
            assertEquals(usageReporter.getTotalFieldsRead(), statusReporter.getInputFieldCount() );
            assertEquals(6, statusReporter.getProcessedRecordCount() );
            assertEquals(6 * 3 -3, statusReporter.getProcessedFieldCount() );
            assertEquals(3, statusReporter.getMissingFieldErrorCount());
            assertEquals(0, statusReporter.getDateParseErrorsCount());
            assertEquals(0, statusReporter.getOutOfOrderRecordCount());

            assertEquals(statusReporter.runningTotalStats(), statusReporter.incrementalStats());


            assertEquals(dp.getRecordCount(), 6);

            ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());

            boolean isHeader = true;
            for (String [] fields : lines)
            {
                int numFields = bb.getInt();
                assertEquals(analysisFields.size() + 2, numFields);

                for (int i = 0; i < numFields - 1; i++)
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
                // Control field
                int recordSize = bb.getInt();
                assertEquals(isHeader ? 1 : 0, recordSize);
                if (isHeader)
                {
                    bb.get();
                    isHeader = false;
                }
            }
        }
    }

    /**
     * Test converting timestamps with fractional components
     */
    @Test
    public void epochWithFractionTest() throws IOException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        String epochData = "airline,responsetime,sourcetype,airport,_time,baggage\n" +
                "DJA,622,flightcentre,MAN,1350824400.115846484,none\n" +
                "JQA,1742,flightcentre,GAT,1350824401.45843543,none\n" +
                "GAL,5339,flightcentre,SYN,1350824402.154835435,some\n" +
                "GAL,3893,flightcentre,CHM,1350824403.0,some\n" +
                "JQA,9,flightcentre,CHM,1350824403.879,none\n" +
                "DJA,189,flightcentre,GAT,1350824404.8676458,lost\n" +
                "JQA,8,flightcentre,GAT,1350824404.86764,none\n" +
                "DJA,,flightcentre,MAN,1350824404.4688,none";

        String epochMsData = "airline,responsetime,sourcetype,airport,_time,baggage\n" +
                "DJA,622,flightcentre,MAN,1350824400000.115846484,none\n" +
                "JQA,1742,flightcentre,GAT,1350824401000.45843543,none\n" +
                "GAL,5339,flightcentre,SYN,1350824402000.154835435,some\n" +
                "GAL,3893,flightcentre,CHM,1350824403000.0,some\n" +
                "JQA,9,flightcentre,CHM,1350824403000.879,none\n" +
                "DJA,189,flightcentre,GAT,1350824404000.8676458,lost\n" +
                "JQA,8,flightcentre,GAT,1350824404000.86764,none\n" +
                "DJA,,flightcentre,MAN,1350824404000.4688,none";

        String [] epochTimes = new String [] {"_time", "1350824400", "1350824401",
                "1350824402", "1350824403", "1350824403", "1350824404",
                "1350824404", "1350824404"};


        // data is written in the order of the required fields
        // which is alphabetical but with time as the first element
        int [] fieldMap = new int [] {2, 0, 1};

        Set<String> analysisFields = new TreeSet<String>(Arrays.asList(new String [] {
                "responsetime", "airline"}));

        int loop = 0;
        for (String data : new String[] {epochData, epochMsData})
        {
            AnalysisConfig ac = new AnalysisConfig();
            Detector d = new Detector();
            d.setFieldName("responsetime");
            d.setByFieldName("airline");
            ac.setDetectors(Arrays.asList(d));

            DataDescription dd = new DataDescription();
            dd.setFormat(DataFormat.DELIMITED);
            dd.setTimeField("_time");
            dd.setFieldDelimiter(',');
            if (loop == 0)
            {
                dd.setTimeFormat("epoch");
            }
            else if (loop == 1)
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

            assertEquals(usageReporter.getTotalBytesRead(), statusReporter.getBytesRead());
            assertEquals(8 * 5, usageReporter.getTotalFieldsRead());
            assertEquals(8, usageReporter.getTotalRecordsRead());

            assertEquals(usageReporter.getTotalRecordsRead(), statusReporter.getInputRecordCount() );
            assertEquals(usageReporter.getTotalFieldsRead(), statusReporter.getInputFieldCount() );
            assertEquals(8, statusReporter.getProcessedRecordCount() );
            assertEquals(8 * 2, statusReporter.getProcessedFieldCount() );
            assertEquals(0, statusReporter.getMissingFieldErrorCount());
            assertEquals(0, statusReporter.getDateParseErrorsCount());
            assertEquals(0, statusReporter.getOutOfOrderRecordCount());

            assertEquals(usageReporter.getTotalBytesRead(),
                    data.getBytes(StandardCharsets.UTF_8).length - 2);
            assertEquals(usageReporter.getTotalBytesRead(),
                    statusReporter.getBytesRead());

            assertEquals(dp.getRecordCount(), 8);

            String [] lines = data.split("\\n");

            final int TIME_FIELD = 0;
            int lineCount = 0;
            for (String line : lines)
            {
                int numFields = bb.getInt();
                String [] fields = line.split(",");
                assertEquals(analysisFields.size() + 2, numFields);

                for (int i = 0; i < numFields - 1; i++)
                {
                    int recordSize = bb.getInt();

                    byte [] charBuff = new byte[recordSize];
                    for (int j=0; j<recordSize; j++)
                    {
                        charBuff[j] = bb.get();
                    }

                    String value = new String(charBuff, StandardCharsets.UTF_8);

                    if (i == TIME_FIELD)
                    {
                        assertEquals(epochTimes[lineCount].length(), recordSize);
                        assertEquals(epochTimes[lineCount], value);
                    }
                    else
                    {
                        assertEquals(fields[fieldMap[i]].length(), recordSize);
                        assertEquals(fields[fieldMap[i]], value);
                    }
                }
                // Control field
                int recordSize = bb.getInt();
                assertEquals((lineCount == 0) ? 1 : 0, recordSize);
                if (lineCount == 0)
                {
                    bb.get();
                }

                lineCount++;
            }
        }
    }

    private static ProcessManager createProcessManager()
    {
        return new ProcessManager(null, null, null, mock(JobLoggerFactory.class));
    }
}
