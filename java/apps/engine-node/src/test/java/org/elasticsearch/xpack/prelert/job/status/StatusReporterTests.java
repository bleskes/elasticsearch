/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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
package org.elasticsearch.xpack.prelert.job.status;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.persistence.JobDataCountsPersister;
import org.elasticsearch.xpack.prelert.job.usage.UsageReporter;
import org.junit.Before;
import org.mockito.Mockito;
import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class StatusReporterTests extends ESTestCase {
    private static final String JOB_ID = "SR";
    private static final int MAX_PERCENT_DATE_PARSE_ERRORS = 40;
    private static final int MAX_PERCENT_OUT_OF_ORDER_ERRORS = 30;

    private UsageReporter usageReporter;
    private JobDataCountsPersister jobDataCountsPersister;
    private Logger mockLogger;

    private StatusReporter statusReporter;
    private Settings settings;

    @Before
    public void setUpMocks() {
        settings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString())
                .put(StatusReporter.ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_SETTING.getKey(), MAX_PERCENT_DATE_PARSE_ERRORS)
                .put(StatusReporter.ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS_SETTING.getKey(), MAX_PERCENT_OUT_OF_ORDER_ERRORS).build();
        Environment env = new Environment(settings);
        usageReporter = Mockito.mock(UsageReporter.class);
        jobDataCountsPersister = Mockito.mock(JobDataCountsPersister.class);
        mockLogger = Mockito.mock(Logger.class);
        statusReporter = new StatusReporter(env, settings, JOB_ID, usageReporter, jobDataCountsPersister, mockLogger, 10L);
    }

    public void testSettingAcceptablePercentages() {
        assertEquals(statusReporter.getAcceptablePercentDateParseErrors(), MAX_PERCENT_DATE_PARSE_ERRORS);
        assertEquals(statusReporter.getAcceptablePercentOutOfOrderErrors(), MAX_PERCENT_OUT_OF_ORDER_ERRORS);
    }

    public void testSimpleConstructor() throws Exception {
        DataCounts stats = statusReporter.incrementalStats();
        assertNotNull(stats);

        assertAllCountFieldsEqualZero(stats);
    }

    public void testComplexConstructor() throws Exception {
        Environment env = new Environment(
                Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString()).build());
        DataCounts counts = new DataCounts(0L, 1L, 1L, 2L, 0L, 3L, 4L, 5L, 6L, 7L, new Date());

        statusReporter = new StatusReporter(env, settings, JOB_ID, counts, usageReporter, jobDataCountsPersister, mockLogger, 1);
        DataCounts stats = statusReporter.incrementalStats();
        assertNotNull(stats);
        assertAllCountFieldsEqualZero(stats);

        assertEquals(1, statusReporter.getProcessedRecordCount());
        assertEquals(2, statusReporter.getBytesRead());
        assertEquals(3, statusReporter.getDateParseErrorsCount());
        assertEquals(4, statusReporter.getMissingFieldErrorCount());
        assertEquals(5, statusReporter.getOutOfOrderRecordCount());
        assertEquals(6, statusReporter.getFailedTransformCount());
        assertEquals(7, statusReporter.getExcludedRecordCount());
    }

    public void testResetIncrementalCounts() throws Exception {
        DataCounts stats = statusReporter.incrementalStats();
        assertNotNull(stats);
        assertAllCountFieldsEqualZero(stats);

        statusReporter.setAnalysedFieldsPerRecord(3);

        statusReporter.reportRecordWritten(5, 1000);
        statusReporter.reportFailedTransform();
        statusReporter.reportExcludedRecord(5);
        assertEquals(2, statusReporter.incrementalStats().getInputRecordCount());
        assertEquals(10, statusReporter.incrementalStats().getInputFieldCount());
        assertEquals(1, statusReporter.incrementalStats().getProcessedRecordCount());
        assertEquals(3, statusReporter.incrementalStats().getProcessedFieldCount());
        assertEquals(1, statusReporter.incrementalStats().getFailedTransformCount());
        assertEquals(1, statusReporter.incrementalStats().getExcludedRecordCount());
        assertEquals(1000, statusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

        assertEquals(statusReporter.incrementalStats(), statusReporter.runningTotalStats());

        statusReporter.startNewIncrementalCount();
        stats = statusReporter.incrementalStats();
        assertNotNull(stats);
        assertAllCountFieldsEqualZero(stats);
    }

    public void testReportLatestTimeIncrementalStats() {
        statusReporter.startNewIncrementalCount();
        statusReporter.reportLatestTimeIncrementalStats(5001L);
        assertEquals(5001L, statusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());
    }

    public void testReportRecordsWritten() {
        statusReporter.setAnalysedFieldsPerRecord(3);

        statusReporter.reportRecordWritten(5, 2000);
        assertEquals(1, statusReporter.incrementalStats().getInputRecordCount());
        assertEquals(5, statusReporter.incrementalStats().getInputFieldCount());
        assertEquals(1, statusReporter.incrementalStats().getProcessedRecordCount());
        assertEquals(3, statusReporter.incrementalStats().getProcessedFieldCount());
        assertEquals(2000, statusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

        statusReporter.reportRecordWritten(5, 3000);
        statusReporter.reportMissingField();
        assertEquals(2, statusReporter.incrementalStats().getInputRecordCount());
        assertEquals(10, statusReporter.incrementalStats().getInputFieldCount());
        assertEquals(2, statusReporter.incrementalStats().getProcessedRecordCount());
        assertEquals(5, statusReporter.incrementalStats().getProcessedFieldCount());
        assertEquals(3000, statusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

        assertEquals(statusReporter.incrementalStats(), statusReporter.runningTotalStats());

        verify(jobDataCountsPersister, never()).persistDataCounts(anyString(), any(DataCounts.class));
    }

    public void testReportRecordsWritten_Given100Records() {
        statusReporter.setAnalysedFieldsPerRecord(3);

        for (int i = 1; i <= 100; i++) {
            statusReporter.reportRecordWritten(5, i);
        }

        assertEquals(100, statusReporter.incrementalStats().getInputRecordCount());
        assertEquals(500, statusReporter.incrementalStats().getInputFieldCount());
        assertEquals(100, statusReporter.incrementalStats().getProcessedRecordCount());
        assertEquals(300, statusReporter.incrementalStats().getProcessedFieldCount());
        assertEquals(100, statusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

        verify(jobDataCountsPersister, times(1)).persistDataCounts(anyString(), any(DataCounts.class));
    }

    public void testReportRecordsWritten_Given1000Records() {
        statusReporter.setAnalysedFieldsPerRecord(3);

        for (int i = 1; i <= 1000; i++) {
            statusReporter.reportRecordWritten(5, i);
        }

        assertEquals(1000, statusReporter.incrementalStats().getInputRecordCount());
        assertEquals(5000, statusReporter.incrementalStats().getInputFieldCount());
        assertEquals(1000, statusReporter.incrementalStats().getProcessedRecordCount());
        assertEquals(3000, statusReporter.incrementalStats().getProcessedFieldCount());
        assertEquals(1000, statusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

        verify(jobDataCountsPersister, times(10)).persistDataCounts(anyString(), any(DataCounts.class));
    }

    public void testReportRecordsWritten_Given2000Records() {
        statusReporter.setAnalysedFieldsPerRecord(3);

        for (int i = 1; i <= 2000; i++) {
            statusReporter.reportRecordWritten(5, i);
        }

        assertEquals(2000, statusReporter.incrementalStats().getInputRecordCount());
        assertEquals(10000, statusReporter.incrementalStats().getInputFieldCount());
        assertEquals(2000, statusReporter.incrementalStats().getProcessedRecordCount());
        assertEquals(6000, statusReporter.incrementalStats().getProcessedFieldCount());
        assertEquals(2000, statusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

        verify(jobDataCountsPersister, times(11)).persistDataCounts(anyString(), any(DataCounts.class));
    }

    public void testReportRecordsWritten_Given20000Records() {
        statusReporter.setAnalysedFieldsPerRecord(3);

        for (int i = 1; i <= 20000; i++) {
            statusReporter.reportRecordWritten(5, i);
        }

        assertEquals(20000, statusReporter.incrementalStats().getInputRecordCount());
        assertEquals(100000, statusReporter.incrementalStats().getInputFieldCount());
        assertEquals(20000, statusReporter.incrementalStats().getProcessedRecordCount());
        assertEquals(60000, statusReporter.incrementalStats().getProcessedFieldCount());
        assertEquals(20000, statusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

        verify(jobDataCountsPersister, times(29)).persistDataCounts(anyString(), any(DataCounts.class));
    }

    public void testReportRecordsWritten_Given30000Records() {
        statusReporter.setAnalysedFieldsPerRecord(3);

        for (int i = 1; i <= 30000; i++) {
            statusReporter.reportRecordWritten(5, i);
        }

        assertEquals(30000, statusReporter.incrementalStats().getInputRecordCount());
        assertEquals(150000, statusReporter.incrementalStats().getInputFieldCount());
        assertEquals(30000, statusReporter.incrementalStats().getProcessedRecordCount());
        assertEquals(90000, statusReporter.incrementalStats().getProcessedFieldCount());
        assertEquals(30000, statusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());

        verify(jobDataCountsPersister, times(30)).persistDataCounts(anyString(), any(DataCounts.class));
    }

    public void testFinishReporting() {
        statusReporter.setAnalysedFieldsPerRecord(3);

        DataCounts dc = new DataCounts(0L, 2L, 5L, 0L, 12L, 0L, 1L, 0L, 0L, 1L, new Date(3000));
        statusReporter.reportRecordWritten(5, 2000);
        statusReporter.reportRecordWritten(5, 3000);
        statusReporter.reportMissingField();
        statusReporter.reportExcludedRecord(2);
        statusReporter.finishReporting();

        Mockito.verify(usageReporter, Mockito.times(1)).reportUsage();
        Mockito.verify(jobDataCountsPersister, Mockito.times(1)).persistDataCounts(eq("SR"), eq(dc));

        assertEquals(dc, statusReporter.incrementalStats());
    }

    private void assertAllCountFieldsEqualZero(DataCounts stats) throws Exception {
        assertEquals(0L, stats.getBucketCount());
        assertEquals(0L, stats.getProcessedRecordCount());
        assertEquals(0L, stats.getProcessedFieldCount());
        assertEquals(0L, stats.getInputBytes());
        assertEquals(0L, stats.getInputFieldCount());
        assertEquals(0L, stats.getInputRecordCount());
        assertEquals(0L, stats.getInvalidDateCount());
        assertEquals(0L, stats.getMissingFieldCount());
        assertEquals(0L, stats.getOutOfOrderTimeStampCount());
        assertEquals(0L, stats.getFailedTransformCount());
        assertEquals(0L, stats.getExcludedRecordCount());
    }
}
