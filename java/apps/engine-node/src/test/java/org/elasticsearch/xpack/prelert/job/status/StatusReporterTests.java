
package org.elasticsearch.xpack.prelert.job.status;

import org.elasticsearch.test.ESTestCase;

//NOCOMMIT fix this test to not use system properties
public class StatusReporterTests extends ESTestCase {
    // private static final String JOB_ID = "SR";
    // private static final int MAX_PERCENT_DATE_PARSE_ERRORS = 40;
    // private static final int MAX_PERCENT_OUT_OF_ORDER_ERRORS = 30;
    //
    // @Mock
    // private UsageReporter usageReporter;
    // @Mock
    // private JobDataCountsPersister jobDataCountsPersister;
    // @Mock
    // private Logger mockLogger;
    //
    // private StatusReporter statusReporter;
    //
    // @BeforeClass
    // public static void oneOffSetup() {
    // System.setProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_PROP,
    // Integer.toString(MAX_PERCENT_DATE_PARSE_ERRORS));
    // System.setProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS_PROP,
    // Integer.toString(MAX_PERCENT_OUT_OF_ORDER_ERRORS));
    // }
    //
    // @AfterClass
    // public static void oneOffTeardown() {
    // System.clearProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_DATE_PARSE_ERRORS_PROP);
    // System.clearProperty(StatusReporter.ACCEPTABLE_PERCENTAGE_OUT_OF_ORDER_ERRORS_PROP);
    // }
    //
    // @Before
    // public void setUpMocks() {
    // Environment env = new Environment(
    // Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(),
    // createTempDir().toString()).build());
    // MockitoAnnotations.initMocks(this);
    // statusReporter = new StatusReporter(env, JOB_ID, usageReporter,
    // jobDataCountsPersister, mockLogger, 10L);
    // }
    //
    //
    // public void testSettingAcceptablePercentages() {
    // assertEquals(statusReporter.getAcceptablePercentDateParseErrors(),
    // MAX_PERCENT_DATE_PARSE_ERRORS);
    // assertEquals(statusReporter.getAcceptablePercentOutOfOrderErrors(),
    // MAX_PERCENT_OUT_OF_ORDER_ERRORS);
    // }
    //
    //
    // public void testSimpleConstructor() throws Exception {
    // DataCounts stats = statusReporter.incrementalStats();
    // assertNotNull(stats);
    //
    // assertAllFieldsEqualZero(stats);
    // }
    //
    //
    // public void testComplexConstructor() throws Exception {
    // Environment env = new Environment(
    // Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(),
    // createTempDir().toString()).build());
    // DataCounts counts = new DataCounts();
    //
    // counts.setProcessedRecordCount(1);
    // counts.setInputBytes(2);
    // counts.setInvalidDateCount(3);
    // counts.setMissingFieldCount(4);
    // counts.setOutOfOrderTimeStampCount(5);
    // counts.setFailedTransformCount(6);
    // counts.setExcludedRecordCount(7);
    //
    // statusReporter = new StatusReporter(env, JOB_ID, counts, usageReporter,
    // jobDataCountsPersister, mockLogger, 1);
    // DataCounts stats = statusReporter.incrementalStats();
    // assertNotNull(stats);
    // assertAllFieldsEqualZero(stats);
    //
    // assertEquals(1, statusReporter.getProcessedRecordCount());
    // assertEquals(2, statusReporter.getBytesRead());
    // assertEquals(3, statusReporter.getDateParseErrorsCount());
    // assertEquals(4, statusReporter.getMissingFieldErrorCount());
    // assertEquals(5, statusReporter.getOutOfOrderRecordCount());
    // assertEquals(6, statusReporter.getFailedTransformCount());
    // assertEquals(7, statusReporter.getExcludedRecordCount());
    // }
    //
    //
    // public void testResetIncrementalCounts() throws Exception {
    // DataCounts stats = statusReporter.incrementalStats();
    // assertNotNull(stats);
    // assertAllFieldsEqualZero(stats);
    //
    // statusReporter.setAnalysedFieldsPerRecord(3);
    //
    // statusReporter.reportRecordWritten(5, 1000);
    // statusReporter.reportFailedTransform();
    // statusReporter.reportExcludedRecord(5);
    // assertEquals(2, statusReporter.incrementalStats().getInputRecordCount());
    // assertEquals(10, statusReporter.incrementalStats().getInputFieldCount());
    // assertEquals(1,
    // statusReporter.incrementalStats().getProcessedRecordCount());
    // assertEquals(3,
    // statusReporter.incrementalStats().getProcessedFieldCount());
    // assertEquals(1,
    // statusReporter.incrementalStats().getFailedTransformCount());
    // assertEquals(1,
    // statusReporter.incrementalStats().getExcludedRecordCount());
    // assertEquals(1000,
    // statusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());
    //
    // assertEquals(statusReporter.incrementalStats(),
    // statusReporter.runningTotalStats());
    //
    // statusReporter.startNewIncrementalCount();
    // stats = statusReporter.incrementalStats();
    // assertNotNull(stats);
    // assertAllFieldsEqualZero(stats);
    // }
    //
    //
    // public void testReportLatestTimeIncrementalStats() {
    // statusReporter.startNewIncrementalCount();
    // statusReporter.reportLatestTimeIncrementalStats(5001L);
    // assertEquals(5001L,
    // statusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());
    // }
    //
    //
    // public void testReportRecordsWritten()
    // throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    // {
    // statusReporter.setAnalysedFieldsPerRecord(3);
    //
    // statusReporter.reportRecordWritten(5, 2000);
    // assertEquals(1, statusReporter.incrementalStats().getInputRecordCount());
    // assertEquals(5, statusReporter.incrementalStats().getInputFieldCount());
    // assertEquals(1,
    // statusReporter.incrementalStats().getProcessedRecordCount());
    // assertEquals(3,
    // statusReporter.incrementalStats().getProcessedFieldCount());
    // assertEquals(2000,
    // statusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());
    //
    // statusReporter.reportRecordWritten(5, 3000);
    // statusReporter.reportMissingField();
    // assertEquals(2, statusReporter.incrementalStats().getInputRecordCount());
    // assertEquals(10, statusReporter.incrementalStats().getInputFieldCount());
    // assertEquals(2,
    // statusReporter.incrementalStats().getProcessedRecordCount());
    // assertEquals(5,
    // statusReporter.incrementalStats().getProcessedFieldCount());
    // assertEquals(3000,
    // statusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());
    //
    // assertEquals(statusReporter.incrementalStats(),
    // statusReporter.runningTotalStats());
    //
    // verify(jobDataCountsPersister, never()).persistDataCounts(anyString(),
    // any(DataCounts.class));
    // }
    //
    //
    // public void testReportRecordsWritten_Given100Records()
    // throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    // {
    // statusReporter.setAnalysedFieldsPerRecord(3);
    //
    // for (int i = 1; i <= 100; i++) {
    // statusReporter.reportRecordWritten(5, i);
    // }
    //
    // assertEquals(100,
    // statusReporter.incrementalStats().getInputRecordCount());
    // assertEquals(500,
    // statusReporter.incrementalStats().getInputFieldCount());
    // assertEquals(100,
    // statusReporter.incrementalStats().getProcessedRecordCount());
    // assertEquals(300,
    // statusReporter.incrementalStats().getProcessedFieldCount());
    // assertEquals(100,
    // statusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());
    //
    // verify(jobDataCountsPersister, times(1)).persistDataCounts(anyString(),
    // any(DataCounts.class));
    // }
    //
    //
    // public void testReportRecordsWritten_Given1000Records()
    // throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    // {
    // statusReporter.setAnalysedFieldsPerRecord(3);
    //
    // for (int i = 1; i <= 1000; i++) {
    // statusReporter.reportRecordWritten(5, i);
    // }
    //
    // assertEquals(1000,
    // statusReporter.incrementalStats().getInputRecordCount());
    // assertEquals(5000,
    // statusReporter.incrementalStats().getInputFieldCount());
    // assertEquals(1000,
    // statusReporter.incrementalStats().getProcessedRecordCount());
    // assertEquals(3000,
    // statusReporter.incrementalStats().getProcessedFieldCount());
    // assertEquals(1000,
    // statusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());
    //
    // verify(jobDataCountsPersister, times(10)).persistDataCounts(anyString(),
    // any(DataCounts.class));
    // }
    //
    //
    // public void testReportRecordsWritten_Given2000Records()
    // throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    // {
    // statusReporter.setAnalysedFieldsPerRecord(3);
    //
    // for (int i = 1; i <= 2000; i++) {
    // statusReporter.reportRecordWritten(5, i);
    // }
    //
    // assertEquals(2000,
    // statusReporter.incrementalStats().getInputRecordCount());
    // assertEquals(10000,
    // statusReporter.incrementalStats().getInputFieldCount());
    // assertEquals(2000,
    // statusReporter.incrementalStats().getProcessedRecordCount());
    // assertEquals(6000,
    // statusReporter.incrementalStats().getProcessedFieldCount());
    // assertEquals(2000,
    // statusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());
    //
    // verify(jobDataCountsPersister, times(11)).persistDataCounts(anyString(),
    // any(DataCounts.class));
    // }
    //
    //
    // public void testReportRecordsWritten_Given20000Records()
    // throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    // {
    // statusReporter.setAnalysedFieldsPerRecord(3);
    //
    // for (int i = 1; i <= 20000; i++) {
    // statusReporter.reportRecordWritten(5, i);
    // }
    //
    // assertEquals(20000,
    // statusReporter.incrementalStats().getInputRecordCount());
    // assertEquals(100000,
    // statusReporter.incrementalStats().getInputFieldCount());
    // assertEquals(20000,
    // statusReporter.incrementalStats().getProcessedRecordCount());
    // assertEquals(60000,
    // statusReporter.incrementalStats().getProcessedFieldCount());
    // assertEquals(20000,
    // statusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());
    //
    // verify(jobDataCountsPersister, times(29)).persistDataCounts(anyString(),
    // any(DataCounts.class));
    // }
    //
    //
    // public void testReportRecordsWritten_Given30000Records()
    // throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    // {
    // statusReporter.setAnalysedFieldsPerRecord(3);
    //
    // for (int i = 1; i <= 30000; i++) {
    // statusReporter.reportRecordWritten(5, i);
    // }
    //
    // assertEquals(30000,
    // statusReporter.incrementalStats().getInputRecordCount());
    // assertEquals(150000,
    // statusReporter.incrementalStats().getInputFieldCount());
    // assertEquals(30000,
    // statusReporter.incrementalStats().getProcessedRecordCount());
    // assertEquals(90000,
    // statusReporter.incrementalStats().getProcessedFieldCount());
    // assertEquals(30000,
    // statusReporter.incrementalStats().getLatestRecordTimeStamp().getTime());
    //
    // verify(jobDataCountsPersister, times(30)).persistDataCounts(anyString(),
    // any(DataCounts.class));
    // }
    //
    //
    // public void testFinishReporting()
    // throws HighProportionOfBadTimestampsException, OutOfOrderRecordsException
    // {
    // statusReporter.setAnalysedFieldsPerRecord(3);
    //
    // DataCounts dc = new DataCounts();
    // dc.setExcludedRecordCount(1L);
    // dc.setInputFieldCount(12L);
    // dc.setMissingFieldCount(1L);
    // dc.setProcessedFieldCount(5L);
    // dc.setProcessedRecordCount(2L);
    // dc.setLatestRecordTimeStamp(new Date(3000));
    //
    // statusReporter.reportRecordWritten(5, 2000);
    // statusReporter.reportRecordWritten(5, 3000);
    // statusReporter.reportMissingField();
    // statusReporter.reportExcludedRecord(2);
    // statusReporter.finishReporting();
    //
    // Mockito.verify(usageReporter, Mockito.times(1)).reportUsage();
    // Mockito.verify(jobDataCountsPersister,
    // Mockito.times(1)).persistDataCounts(eq("SR"), eq(dc));
    //
    // assertEquals(dc, statusReporter.incrementalStats());
    // }
    //
    // private void assertAllFieldsEqualZero(DataCounts stats) throws Exception
    // {
    // assertEquals(0L, stats.getBucketCount());
    // assertEquals(0L, stats.getProcessedRecordCount());
    // assertEquals(0L, stats.getProcessedFieldCount());
    // assertEquals(0L, stats.getInputBytes());
    // assertEquals(0L, stats.getInputFieldCount());
    // assertEquals(0L, stats.getInputRecordCount());
    // assertEquals(0L, stats.getInvalidDateCount());
    // assertEquals(0L, stats.getMissingFieldCount());
    // assertEquals(0L, stats.getOutOfOrderTimeStampCount());
    // assertEquals(0L, stats.getFailedTransformCount());
    // assertEquals(0L, stats.getExcludedRecordCount());
    // assertEquals(null, stats.getLatestRecordTimeStamp());
    // }
}
