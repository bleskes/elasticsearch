/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.ml.job.process;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.ml.job.persistence.JobDataCountsPersister;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.DataCounts;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DataCountsReporterTests extends ESTestCase {
    private static final String JOB_ID = "SR";
    private static final int MAX_PERCENT_DATE_PARSE_ERRORS = 40;
    private static final int MAX_PERCENT_OUT_OF_ORDER_ERRORS = 30;

    private JobDataCountsPersister jobDataCountsPersister;
    private ThreadPool threadPool;
    private Settings settings;

    @Before
    public void setUpMocks() {
        settings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString())
                .put(DataCountsReporter.ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_SETTING.getKey(), MAX_PERCENT_DATE_PARSE_ERRORS)
                .put(DataCountsReporter.ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS_SETTING.getKey(), MAX_PERCENT_OUT_OF_ORDER_ERRORS)
                .build();
        jobDataCountsPersister = Mockito.mock(JobDataCountsPersister.class);
        threadPool = Mockito.mock(ThreadPool.class);

        when(threadPool.scheduleWithFixedDelay(any(Runnable.class), any(), any())).thenReturn(new ThreadPool.Cancellable() {
            @Override
            public void cancel() {
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        });
    }

    public void testSettingAcceptablePercentages() throws IOException {
        try (DataCountsReporter dataCountsReporter = new DataCountsReporter(threadPool, settings, JOB_ID, new DataCounts(JOB_ID),
                jobDataCountsPersister)) {
            assertEquals(dataCountsReporter.getAcceptablePercentDateParseErrors(), MAX_PERCENT_DATE_PARSE_ERRORS);
            assertEquals(dataCountsReporter.getAcceptablePercentOutOfOrderErrors(), MAX_PERCENT_OUT_OF_ORDER_ERRORS);
        }
    }

    public void testSimpleConstructor() throws Exception {
        try (DataCountsReporter dataCountsReporter = new DataCountsReporter(threadPool, settings, JOB_ID, new DataCounts(JOB_ID),
                jobDataCountsPersister)) {
            DataCounts stats = dataCountsReporter.incrementalStats();
            assertNotNull(stats);
            assertAllCountFieldsEqualZero(stats);
        }
    }

    public void testComplexConstructor() throws Exception {
        DataCounts counts = new DataCounts("foo", 1L, 1L, 2L, 0L, 3L, 4L, 5L, 6L, 7L, 8L, 
                new Date(), new Date(), new Date(), new Date(), new Date());

        try (DataCountsReporter dataCountsReporter =
                new DataCountsReporter(threadPool, settings, JOB_ID, counts, jobDataCountsPersister)) {
            DataCounts stats = dataCountsReporter.incrementalStats();
            assertNotNull(stats);
            assertAllCountFieldsEqualZero(stats);

            assertEquals(1, dataCountsReporter.getProcessedRecordCount());
            assertEquals(2, dataCountsReporter.getBytesRead());
            assertEquals(3, dataCountsReporter.getDateParseErrorsCount());
            assertEquals(4, dataCountsReporter.getMissingFieldErrorCount());
            assertEquals(5, dataCountsReporter.getOutOfOrderRecordCount());
            assertEquals(6, dataCountsReporter.getEmptyBucketCount());
            assertEquals(7, dataCountsReporter.getSparseBucketCount());
            assertEquals(8, dataCountsReporter.getBucketCount());
            assertNull(stats.getEarliestRecordTimeStamp());
        }
    }

    public void testResetIncrementalCounts() throws Exception {
        try (DataCountsReporter dataCountsReporter = new DataCountsReporter(threadPool, settings, JOB_ID, new DataCounts(JOB_ID),
                jobDataCountsPersister)) {
            DataCounts stats = dataCountsReporter.incrementalStats();
            assertNotNull(stats);
            assertAllCountFieldsEqualZero(stats);

            dataCountsReporter.setAnalysedFieldsPerRecord(3);

            dataCountsReporter.reportRecordWritten(5, 1000);
            dataCountsReporter.reportRecordWritten(5, 1000);
            assertEquals(2, dataCountsReporter.incrementalStats().getInputRecordCount());
            assertEquals(10, dataCountsReporter.incrementalStats().getInputFieldCount());
            assertEquals(2, dataCountsReporter.incrementalStats().getProcessedRecordCount());
            assertEquals(6, dataCountsReporter.incrementalStats().getProcessedFieldCount());
            assertEquals(1000, dataCountsReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

            assertEquals(dataCountsReporter.incrementalStats(), dataCountsReporter.runningTotalStats());

            dataCountsReporter.startNewIncrementalCount();
            stats = dataCountsReporter.incrementalStats();
            assertNotNull(stats);
            assertAllCountFieldsEqualZero(stats);
        }
    }

    public void testReportLatestTimeIncrementalStats() throws IOException {
        try (DataCountsReporter dataCountsReporter = new DataCountsReporter(threadPool, settings, JOB_ID, new DataCounts(JOB_ID),
                jobDataCountsPersister)) {
            dataCountsReporter.startNewIncrementalCount();
            dataCountsReporter.reportLatestTimeIncrementalStats(5001L);
            assertEquals(5001L, dataCountsReporter.incrementalStats().getLatestRecordTimeStamp().getTime());
        }
    }

    public void testReportRecordsWritten() {
        try (DataCountsReporter dataCountsReporter = new DataCountsReporter(threadPool, settings, JOB_ID, new DataCounts(JOB_ID),
                jobDataCountsPersister)) {
            dataCountsReporter.setAnalysedFieldsPerRecord(3);

            dataCountsReporter.reportRecordWritten(5, 2000);
            assertEquals(1, dataCountsReporter.incrementalStats().getInputRecordCount());
            assertEquals(5, dataCountsReporter.incrementalStats().getInputFieldCount());
            assertEquals(1, dataCountsReporter.incrementalStats().getProcessedRecordCount());
            assertEquals(3, dataCountsReporter.incrementalStats().getProcessedFieldCount());
            assertEquals(2000, dataCountsReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

            dataCountsReporter.reportRecordWritten(5, 3000);
            dataCountsReporter.reportMissingField();
            assertEquals(2, dataCountsReporter.incrementalStats().getInputRecordCount());
            assertEquals(10, dataCountsReporter.incrementalStats().getInputFieldCount());
            assertEquals(2, dataCountsReporter.incrementalStats().getProcessedRecordCount());
            assertEquals(5, dataCountsReporter.incrementalStats().getProcessedFieldCount());
            assertEquals(3000, dataCountsReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

            assertEquals(dataCountsReporter.incrementalStats(), dataCountsReporter.runningTotalStats());

            verify(jobDataCountsPersister, never()).persistDataCounts(anyString(), any(DataCounts.class), any());
        }
    }

    public void testReportRecordsWritten_Given100Records() {
        try (DummyDataCountsReporter dataCountsReporter = new DummyDataCountsReporter()) {
            dataCountsReporter.setAnalysedFieldsPerRecord(3);

            for (int i = 1; i <= 101; i++) {
                dataCountsReporter.reportRecordWritten(5, i);
            }

            assertEquals(101, dataCountsReporter.incrementalStats().getInputRecordCount());
            assertEquals(505, dataCountsReporter.incrementalStats().getInputFieldCount());
            assertEquals(101, dataCountsReporter.incrementalStats().getProcessedRecordCount());
            assertEquals(303, dataCountsReporter.incrementalStats().getProcessedFieldCount());
            assertEquals(101, dataCountsReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

            assertEquals(1, dataCountsReporter.getLogStatusCallCount());
        }
    }

    public void testReportRecordsWritten_Given1000Records() {
        try (DummyDataCountsReporter dataCountsReporter = new DummyDataCountsReporter()) {

            dataCountsReporter.setAnalysedFieldsPerRecord(3);

            for (int i = 1; i <= 1001; i++) {
                dataCountsReporter.reportRecordWritten(5, i);
            }

            assertEquals(1001, dataCountsReporter.incrementalStats().getInputRecordCount());
            assertEquals(5005, dataCountsReporter.incrementalStats().getInputFieldCount());
            assertEquals(1001, dataCountsReporter.incrementalStats().getProcessedRecordCount());
            assertEquals(3003, dataCountsReporter.incrementalStats().getProcessedFieldCount());
            assertEquals(1001, dataCountsReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

            assertEquals(10, dataCountsReporter.getLogStatusCallCount());
        }

    }

    public void testReportRecordsWritten_Given2000Records() {
        try (DummyDataCountsReporter dataCountsReporter = new DummyDataCountsReporter()) {
            dataCountsReporter.setAnalysedFieldsPerRecord(3);

            for (int i = 1; i <= 2001; i++) {
                dataCountsReporter.reportRecordWritten(5, i);
            }

            assertEquals(2001, dataCountsReporter.incrementalStats().getInputRecordCount());
            assertEquals(10005, dataCountsReporter.incrementalStats().getInputFieldCount());
            assertEquals(2001, dataCountsReporter.incrementalStats().getProcessedRecordCount());
            assertEquals(6003, dataCountsReporter.incrementalStats().getProcessedFieldCount());
            assertEquals(2001, dataCountsReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

            assertEquals(11, dataCountsReporter.getLogStatusCallCount());
        }
    }

    public void testReportRecordsWritten_Given20000Records() {
        try (DummyDataCountsReporter dataCountsReporter = new DummyDataCountsReporter()) {
            dataCountsReporter.setAnalysedFieldsPerRecord(3);

            for (int i = 1; i <= 20001; i++) {
                dataCountsReporter.reportRecordWritten(5, i);
            }

            assertEquals(20001, dataCountsReporter.incrementalStats().getInputRecordCount());
            assertEquals(100005, dataCountsReporter.incrementalStats().getInputFieldCount());
            assertEquals(20001, dataCountsReporter.incrementalStats().getProcessedRecordCount());
            assertEquals(60003, dataCountsReporter.incrementalStats().getProcessedFieldCount());
            assertEquals(20001, dataCountsReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

            assertEquals(29, dataCountsReporter.getLogStatusCallCount());
        }
    }

    public void testReportRecordsWritten_Given30000Records() {
        try (DummyDataCountsReporter dataCountsReporter = new DummyDataCountsReporter()) {
            dataCountsReporter.setAnalysedFieldsPerRecord(3);

            for (int i = 1; i <= 30001; i++) {
                dataCountsReporter.reportRecordWritten(5, i);
            }

            assertEquals(30001, dataCountsReporter.incrementalStats().getInputRecordCount());
            assertEquals(150005, dataCountsReporter.incrementalStats().getInputFieldCount());
            assertEquals(30001, dataCountsReporter.incrementalStats().getProcessedRecordCount());
            assertEquals(90003, dataCountsReporter.incrementalStats().getProcessedFieldCount());
            assertEquals(30001, dataCountsReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

            assertEquals(30, dataCountsReporter.getLogStatusCallCount());
        }
    }

    public void testFinishReporting() {
        try (DataCountsReporter dataCountsReporter = new DataCountsReporter(threadPool, settings, JOB_ID, new DataCounts(JOB_ID),
                 jobDataCountsPersister)) {

            dataCountsReporter.setAnalysedFieldsPerRecord(3);
            Date now = new Date();
            DataCounts dc = new DataCounts(JOB_ID, 2L, 5L, 0L, 10L, 0L, 1L, 0L, 0L, 0L, 0L, new Date(2000), new Date(3000), 
                    now, (Date) null, (Date) null);
            dataCountsReporter.reportRecordWritten(5, 2000);
            dataCountsReporter.reportRecordWritten(5, 3000);
            dataCountsReporter.reportMissingField();
            dataCountsReporter.finishReporting();

            long lastReportedTimeMs = dataCountsReporter.incrementalStats().getLastDataTimeStamp().getTime();
            // check last data time is equal to now give or take a second
            assertTrue(lastReportedTimeMs >= now.getTime() && lastReportedTimeMs <= now.getTime() +1);
            assertEquals(dataCountsReporter.incrementalStats().getLastDataTimeStamp(),
                    dataCountsReporter.runningTotalStats().getLastDataTimeStamp());

            dc.setLastDataTimeStamp(dataCountsReporter.incrementalStats().getLastDataTimeStamp());
            Mockito.verify(jobDataCountsPersister, Mockito.times(1)).persistDataCounts(eq("SR"), eq(dc), any());
            assertEquals(dc, dataCountsReporter.incrementalStats());
        }
    }

    public void testPersistenceTimeOut() {

        ThreadPool mockThreadPool = Mockito.mock(ThreadPool.class);
        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);

        when(mockThreadPool.scheduleWithFixedDelay(argumentCaptor.capture(), any(), any())).thenReturn(new ThreadPool.Cancellable() {
            @Override
            public void cancel() {

            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        });

        try (DataCountsReporter dataCountsReporter = new DataCountsReporter(mockThreadPool, settings, JOB_ID, new DataCounts(JOB_ID),
                jobDataCountsPersister)) {

            dataCountsReporter.setAnalysedFieldsPerRecord(3);

            dataCountsReporter.reportRecordWritten(5, 2000);
            dataCountsReporter.reportRecordWritten(5, 3000);

            Mockito.verify(jobDataCountsPersister, Mockito.times(0)).persistDataCounts(eq("SR"), any(), any());
            argumentCaptor.getValue().run();
            dataCountsReporter.reportRecordWritten(5, 4000);
            Mockito.verify(jobDataCountsPersister, Mockito.times(1)).persistDataCounts(eq("SR"), any(), any());
        }
    }

    private void assertAllCountFieldsEqualZero(DataCounts stats) throws Exception {
        assertEquals(0L, stats.getProcessedRecordCount());
        assertEquals(0L, stats.getProcessedFieldCount());
        assertEquals(0L, stats.getInputBytes());
        assertEquals(0L, stats.getInputFieldCount());
        assertEquals(0L, stats.getInputRecordCount());
        assertEquals(0L, stats.getInvalidDateCount());
        assertEquals(0L, stats.getMissingFieldCount());
        assertEquals(0L, stats.getOutOfOrderTimeStampCount());
        assertEquals(0L, stats.getEmptyBucketCount());
        assertEquals(0L, stats.getSparseBucketCount());;
    }
}
