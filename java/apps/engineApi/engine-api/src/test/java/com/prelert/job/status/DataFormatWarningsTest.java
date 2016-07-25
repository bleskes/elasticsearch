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
 *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.job.status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.Detector;
import com.prelert.job.JobException;
import com.prelert.job.logging.JobLoggerFactory;
import com.prelert.job.persistence.JobDataCountsPersister;
import com.prelert.job.persistence.JobDataPersister;
import com.prelert.job.persistence.UsagePersister;
import com.prelert.job.persistence.none.NoneJobDataPersister;
import com.prelert.job.process.autodetect.ProcessManager;
import com.prelert.job.transform.TransformConfig;
import com.prelert.job.transform.TransformConfigs;
import com.prelert.job.usage.DummyUsageReporter;

public class DataFormatWarningsTest
{
    Logger LOGGER = Logger.getLogger(DataFormatWarningsTest.class);

    /**
     * Writes to nowhere
     */
    public class NullOutputStream extends OutputStream
    {
      @Override
      public void write(int b) throws IOException
      {
      }
    }


    /**
     * Test writing csv data with unparseble dates throws a
     * HighProportionOfBadTimestampsException
     * @throws JobException
     */
    @Test
    public void highProportionOfBadTimestampsCsvTest()
            throws JsonParseException, IOException, JobException
    {
        final String HEADER = "time,responsetime,sourcetype,airline\n";
        final String RECORD_TEMPLATE = "\"%s\",0.35,Farequote,AAL\n";

        final String DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";

        final int MAX_PERCENT_DATE_PARSE_ERRORS = 40;
        final int MAX_PERCENT_OUT_OF_ORDER_ERRORS = 30;

        // do for epoch, epochms, date format

        AnalysisConfig ac = new AnalysisConfig();
        Detector det = new Detector();
        det.setFieldName("responsetime");
        det.setByFieldName("airline");
        det.setPartitionFieldName("sourcetype");
        ac.setDetectors(Arrays.asList(det));

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(DATE_FORMAT);

        String [] apiDateFormats = new String [] {DATE_FORMAT, "epoch", "epoch_ms"};
        for (String apiDateFormat : apiDateFormats)
        {
            // create data
            boolean goodRecord = true;
            long startEpoch = Instant.now().toEpochMilli();

            StringBuilder sb = new StringBuilder(HEADER);
            for (long i=0; i<1000; i++)
            {
                if (goodRecord == false)
                {
                    sb.append(String.format(RECORD_TEMPLATE, ""));
                }
                else
                {
                    ZonedDateTime d = fromEpochMilli(startEpoch + i * 1000);

                    String record;
                    if (apiDateFormat.equals("epoch"))
                    {
                        record = String.format(RECORD_TEMPLATE, Long.toString(d.toEpochSecond()));
                    }
                    else if (apiDateFormat.equals("epoch_ms"))
                    {
                        record = String.format(RECORD_TEMPLATE, Long.toString(d.toEpochSecond() * 1000));
                    }
                    else
                    {
                        record = String.format(RECORD_TEMPLATE, dateFormat.format(d));
                    }

                    sb.append(record);
                }

                goodRecord = !goodRecord;
            }

            ProcessManager pm = createProcessManager();

            ByteArrayInputStream bis =
                    new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));

            DataDescription dd = new DataDescription();
            dd.setFieldDelimiter(',');
            dd.setTimeField("time");
            dd.setTimeFormat(apiDateFormat);


            System.setProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_PROP,
                    Integer.toString(MAX_PERCENT_DATE_PARSE_ERRORS));
            System.setProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS_PROP,
                    Integer.toString(MAX_PERCENT_OUT_OF_ORDER_ERRORS));

            JobDataCountsPersister countsPersister = Mockito.mock(JobDataCountsPersister.class);
            UsagePersister usagePersister = Mockito.mock(UsagePersister.class);

            DummyUsageReporter usageReporter = new DummyUsageReporter("test-job", usagePersister, LOGGER);
            StatusReporter statusReporter = new StatusReporter("test-job", usageReporter,
                    countsPersister, LOGGER, 1);
            JobDataPersister dp = new NoneJobDataPersister();

            assertEquals(MAX_PERCENT_DATE_PARSE_ERRORS,
                    statusReporter.getAcceptablePercentDateParseErrors());
            assertEquals(MAX_PERCENT_OUT_OF_ORDER_ERRORS,
                    statusReporter.getAcceptablePercentOutOfOrderErrors());

            try
            {
                pm.writeToJob(dd, ac, null, new TransformConfigs(Arrays.<TransformConfig>asList()), bis,
                        new NullOutputStream(),
                        statusReporter, dp, LOGGER);
                assertTrue(false); // should throw
            }
            catch (HighProportionOfBadTimestampsException e)
            {
                long percentBad = (e.getNumberBad() * 100 )/ e.getTotalNumber();

                Mockito.verify(countsPersister).persistDataCounts(Mockito.anyString(), Mockito.any());
                Mockito.verify(usagePersister, times(1)).persistUsage(anyString(), anyLong(), anyLong(), anyLong());

                assertEquals(statusReporter.getBytesRead(),
                        usageReporter.getTotalBytesRead());
                assertEquals(statusReporter.getInputFieldCount(),
                        usageReporter.getTotalFieldsRead());
                assertEquals(statusReporter.getInputRecordCount(),
                        usageReporter.getTotalRecordsRead());

                assertEquals(0, usageReporter.getBytesReadSinceLastReport());
                assertEquals(0, usageReporter.getFieldsReadSinceLastReport());
                assertEquals(0, usageReporter.getRecordsReadSinceLastReport());

                assertTrue(percentBad >= MAX_PERCENT_DATE_PARSE_ERRORS);
            }
        }
    }

    private static ZonedDateTime fromEpochMilli(long epochMilli)
    {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneOffset.systemDefault());
    }

    /**
     * Test writing JSON data with unparseble dates throws a
     * HighProportionOfBadTimestampsException
     * @throws JobException
     */
    @Test
    public void highProportionOfBadTimestampsJsonTest() throws JsonParseException,
            IOException, JobException
    {
        final String RECORD_TEMPLATE = "{\"time\":\"%s\","
                + "\"responsetime\":0.35,"
                + "\"sourcetype\":\"Farequote\","
                + "\"airline\":\"AAL\"}";

        final String DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";

        final int MAX_PERCENT_DATE_PARSE_ERRORS = 40;
        final int MAX_PERCENT_OUT_OF_ORDER_ERRORS = 30;

        // do for epoch, epochms, date format


        AnalysisConfig ac = new AnalysisConfig();
        Detector det = new Detector();
        det.setFieldName("responsetime");
        det.setByFieldName("airline");
        det.setPartitionFieldName("sourcetype");
        ac.setDetectors(Arrays.asList(det));

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(DATE_FORMAT);

        String [] apiDateFormats = new String [] {DATE_FORMAT, "epoch", "epoch_ms"};
        for (String apiDateFormat : apiDateFormats)
        {
            // create data
            boolean goodRecord = true;
            long startEpoch = Instant.now().toEpochMilli();

            StringBuilder sb = new StringBuilder();
            for (long i=0; i<1000; i++)
            {
                if (goodRecord == false)
                {
                    sb.append(String.format(RECORD_TEMPLATE, ""));
                }
                else
                {
                    ZonedDateTime d = fromEpochMilli(startEpoch + i * 1000);

                    String record;
                    if (apiDateFormat.equals("epoch"))
                    {
                        record = String.format(RECORD_TEMPLATE, Long.toString(d.toEpochSecond()));
                    }
                    else if (apiDateFormat.equals("epoch_ms"))
                    {
                        record = String.format(RECORD_TEMPLATE, Long.toString(d.toEpochSecond() * 1000));
                    }
                    else
                    {
                        record = String.format(RECORD_TEMPLATE, dateFormat.format(d));
                    }

                    sb.append(record);
                }

                goodRecord = !goodRecord;
            }

            ProcessManager pm = createProcessManager();

            ByteArrayInputStream bis =
                    new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));

            DataDescription dd = new DataDescription();
            dd.setFormat(DataFormat.JSON);
            dd.setTimeField("time");
            dd.setTimeFormat(apiDateFormat);

            System.setProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_PROP,
                    Integer.toString(MAX_PERCENT_DATE_PARSE_ERRORS));
            System.setProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS_PROP,
                    Integer.toString(MAX_PERCENT_OUT_OF_ORDER_ERRORS));

            JobDataCountsPersister countsPersister = Mockito.mock(JobDataCountsPersister.class);
            UsagePersister usagePersister = Mockito.mock(UsagePersister.class);

            DummyUsageReporter usageReporter = new DummyUsageReporter("test-job", usagePersister, LOGGER);
            StatusReporter statusReporter = new StatusReporter("test-job", usageReporter,
                    countsPersister, LOGGER, 1);
            JobDataPersister dp = new NoneJobDataPersister();

            assertEquals(MAX_PERCENT_DATE_PARSE_ERRORS,
                    statusReporter.getAcceptablePercentDateParseErrors());
            assertEquals(MAX_PERCENT_OUT_OF_ORDER_ERRORS,
                    statusReporter.getAcceptablePercentOutOfOrderErrors());

            try
            {
                pm.writeToJob(dd, ac, null, new TransformConfigs(Arrays.<TransformConfig>asList()), bis, new NullOutputStream(),
                        statusReporter, dp, LOGGER);
                assertTrue(false); // should throw
            }
            catch (HighProportionOfBadTimestampsException e)
            {
                long percentBad = (e.getNumberBad() * 100 )/ e.getTotalNumber();

                Mockito.verify(countsPersister).persistDataCounts(Mockito.any(), Mockito.any());
                Mockito.verify(usagePersister, times(1)).persistUsage(anyString(), anyLong(), anyLong(), anyLong());

                assertEquals(statusReporter.getBytesRead(),
                        usageReporter.getTotalBytesRead());
                assertEquals(statusReporter.getInputFieldCount(),
                        usageReporter.getTotalFieldsRead());
                assertEquals(statusReporter.getInputRecordCount(),
                        usageReporter.getTotalRecordsRead());

                assertEquals(0, usageReporter.getBytesReadSinceLastReport());
                assertEquals(0, usageReporter.getFieldsReadSinceLastReport());
                assertEquals(0, usageReporter.getRecordsReadSinceLastReport());

                assertTrue(percentBad >= MAX_PERCENT_DATE_PARSE_ERRORS);
            }
        }
    }


    /**
     * Test writing CSV with out of order records should throw an exception
     * @throws JobException
     */
    @Test
    public void OutOfOrderRecondsCsvTest() throws JsonParseException, IOException, JobException
    {
        final String HEADER = "time,responsetime,sourcetype,airline\n";
        final String RECORD_TEMPLATE = "\"%s\",0.35,Farequote,AAL\n";

        final String DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";

        final int MAX_PERCENT_DATE_PARSE_ERRORS = 40;
        final int MAX_PERCENT_OUT_OF_ORDER_ERRORS = 8;

        // do for epoch, epochms, date format


        AnalysisConfig ac = new AnalysisConfig();
        Detector det = new Detector();
        det.setFieldName("responsetime");
        det.setByFieldName("airline");
        det.setPartitionFieldName("sourcetype");
        ac.setDetectors(Arrays.asList(det));

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(DATE_FORMAT);

        String [] apiDateFormats = new String [] {DATE_FORMAT, "epoch", "epoch_ms"};
        for (String apiDateFormat : apiDateFormats)
        {
            // create data
            long startEpoch = Instant.now().toEpochMilli();

            StringBuilder sb = new StringBuilder(HEADER);
            for (long i=0; i<1000; i++)
            {
                // make 1 in 10 records a bad un
                boolean badRecord = i % 10 == 0;

                ZonedDateTime d = badRecord ? fromEpochMilli(startEpoch) : fromEpochMilli(startEpoch + i * 1000);

                String record;
                if (apiDateFormat.equals("epoch"))
                {
                    record = String.format(RECORD_TEMPLATE, Long.toString(d.toEpochSecond()));
                }
                else if (apiDateFormat.equals("epoch_ms"))
                {
                    record = String.format(RECORD_TEMPLATE, Long.toString(d.toEpochSecond() * 1000));
                }
                else
                {
                    record = String.format(RECORD_TEMPLATE, dateFormat.format(d));
                }

                sb.append(record);

            }

            ProcessManager pm = createProcessManager();

            ByteArrayInputStream bis =
                    new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));

            DataDescription dd = new DataDescription();
            dd.setFormat(DataFormat.DELIMITED);
            dd.setTimeField("time");
            dd.setFieldDelimiter(',');
            dd.setTimeFormat(apiDateFormat);

            System.setProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_PROP,
                    Integer.toString(MAX_PERCENT_DATE_PARSE_ERRORS));
            System.setProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS_PROP,
                    Integer.toString(MAX_PERCENT_OUT_OF_ORDER_ERRORS));

            JobDataCountsPersister countsPersister = Mockito.mock(JobDataCountsPersister.class);
            UsagePersister usagePersister = Mockito.mock(UsagePersister.class);

            DummyUsageReporter usageReporter = new DummyUsageReporter("test-job", usagePersister, LOGGER);
            StatusReporter statusReporter = new StatusReporter("test-job", usageReporter,
                    countsPersister, LOGGER, 1);
            JobDataPersister dp = new NoneJobDataPersister();

            assertEquals(MAX_PERCENT_DATE_PARSE_ERRORS,
                    statusReporter.getAcceptablePercentDateParseErrors());
            assertEquals(MAX_PERCENT_OUT_OF_ORDER_ERRORS,
                    statusReporter.getAcceptablePercentOutOfOrderErrors());

            try
            {
                pm.writeToJob(dd, ac, null, new TransformConfigs(Arrays.<TransformConfig>asList()), bis, new NullOutputStream(),
                        statusReporter, dp, LOGGER);
                assertTrue(false); // should throw
            }
            catch (OutOfOrderRecordsException e)
            {
                long percentBad = (e.getNumberOutOfOrder() * 100 )/ e.getTotalNumber();

                Mockito.verify(countsPersister).persistDataCounts(Mockito.anyString(), Mockito.any());
                Mockito.verify(usagePersister, times(1)).persistUsage(anyString(), anyLong(), anyLong(), anyLong());

                assertEquals(statusReporter.getBytesRead(),
                        usageReporter.getTotalBytesRead());
                assertEquals(statusReporter.getInputFieldCount(),
                        usageReporter.getTotalFieldsRead());
                assertEquals(statusReporter.getInputRecordCount(),
                        usageReporter.getTotalRecordsRead());

                assertEquals(0, usageReporter.getBytesReadSinceLastReport());
                assertEquals(0, usageReporter.getFieldsReadSinceLastReport());
                assertEquals(0, usageReporter.getRecordsReadSinceLastReport());

                assertTrue(percentBad >= MAX_PERCENT_OUT_OF_ORDER_ERRORS);
            }
        }
    }


    /**
     * Test writing JSON with out of order records should throw an exception
     * @throws JobException
     */
    @Test
    public void outOfOrderRecordsJsonTest() throws JsonParseException, IOException, JobException
    {
        final String RECORD_TEMPLATE = "{\"time\":\"%s\","
                + "\"responsetime\":0.35,"
                + "\"sourcetype\":\"Farequote\","
                + "\"airline\":\"AAL\"}";

        final String DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";

        final int MAX_PERCENT_DATE_PARSE_ERRORS = 40;
        final int MAX_PERCENT_OUT_OF_ORDER_ERRORS = 8;

        // do for epoch, epochms, date format

        AnalysisConfig ac = new AnalysisConfig();
        Detector det = new Detector();
        det.setFieldName("responsetime");
        det.setByFieldName("airline");
        det.setPartitionFieldName("sourcetype");
        ac.setDetectors(Arrays.asList(det));

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(DATE_FORMAT);

        String [] apiDateFormats = new String [] {DATE_FORMAT, "epoch", "epoch_ms"};
        for (String apiDateFormat : apiDateFormats)
        {
            // create data
            long startEpoch = Instant.now().toEpochMilli();

            StringBuilder sb = new StringBuilder();
            for (long i=0; i<1000; i++)
            {
                // make 1 in 10 records a bad un
                boolean badRecord = i % 10 == 0;

                ZonedDateTime d = badRecord ? fromEpochMilli(startEpoch) : fromEpochMilli(startEpoch + i * 1000);

                String record;
                if (apiDateFormat.equals("epoch"))
                {
                    record = String.format(RECORD_TEMPLATE, Long.toString(d.toEpochSecond()));
                }
                else if (apiDateFormat.equals("epoch_ms"))
                {
                    record = String.format(RECORD_TEMPLATE, Long.toString(d.toEpochSecond() * 1000));
                }
                else
                {
                    record = String.format(RECORD_TEMPLATE, dateFormat.format(d));
                }

                sb.append(record);

            }

            ProcessManager pm = createProcessManager();

            ByteArrayInputStream bis =
                    new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));

            DataDescription dd = new DataDescription();
            dd.setFormat(DataFormat.JSON);
            dd.setTimeField("time");
            dd.setTimeFormat(apiDateFormat);

            System.setProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_PROP,
                    Integer.toString(MAX_PERCENT_DATE_PARSE_ERRORS));
            System.setProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS_PROP,
                    Integer.toString(MAX_PERCENT_OUT_OF_ORDER_ERRORS));

            JobDataCountsPersister countsPersister = Mockito.mock(JobDataCountsPersister.class);
            UsagePersister usagePersister = Mockito.mock(UsagePersister.class);

            DummyUsageReporter usageReporter = new DummyUsageReporter("test-job", usagePersister, LOGGER);
            StatusReporter statusReporter = new StatusReporter("test-job", usageReporter,
                    countsPersister, LOGGER, 1);
            JobDataPersister dp = new NoneJobDataPersister();

            assertEquals(MAX_PERCENT_DATE_PARSE_ERRORS,
                    statusReporter.getAcceptablePercentDateParseErrors());
            assertEquals(MAX_PERCENT_OUT_OF_ORDER_ERRORS,
                    statusReporter.getAcceptablePercentOutOfOrderErrors());

            try
            {
                pm.writeToJob(dd, ac, null, new TransformConfigs(Arrays.<TransformConfig>asList()), bis, new NullOutputStream(),
                        statusReporter, dp, LOGGER);
                assertTrue(false); // should throw
            }
            catch (OutOfOrderRecordsException e)
            {
                long percentBad = (e.getNumberOutOfOrder() * 100 )/ e.getTotalNumber();

                Mockito.verify(countsPersister).persistDataCounts(Mockito.anyString(), Mockito.any());
                Mockito.verify(usagePersister, times(1)).persistUsage(anyString(), anyLong(), anyLong(), anyLong());

                assertEquals(statusReporter.getBytesRead(),
                        usageReporter.getTotalBytesRead());
                assertEquals(statusReporter.getInputFieldCount(),
                        usageReporter.getTotalFieldsRead());
                assertEquals(statusReporter.getInputRecordCount(),
                        usageReporter.getTotalRecordsRead());

                assertEquals(0, usageReporter.getBytesReadSinceLastReport());
                assertEquals(0, usageReporter.getFieldsReadSinceLastReport());
                assertEquals(0, usageReporter.getRecordsReadSinceLastReport());

                assertTrue(percentBad >= MAX_PERCENT_OUT_OF_ORDER_ERRORS);
            }
        }
    }

    private static ProcessManager createProcessManager()
    {
        return new ProcessManager(null, null, null, mock(JobLoggerFactory.class));
    }
}
