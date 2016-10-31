
package org.elasticsearch.xpack.prelert.job.usage;


import org.elasticsearch.test.ESTestCase;


//NOCOMMIT fix this test to not use system properties
public class UsageReporterTests extends ESTestCase {
    // public void testUpdatePeriod() throws JobException {
    // Environment env = new Environment(
    // Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(),
    // createTempDir().toString()).build());
    // // set the update interval to 1 secs
    // System.setProperty(UsageReporter.UPDATE_INTERVAL_PROP, "1");
    //
    // UsagePersister persister = Mockito.mock(UsagePersister.class);
    // UsageReporter usage = new UsageReporter(env, "job1", persister,
    // Mockito.mock(Logger.class));
    //
    // usage.addBytesRead(10);
    // usage.addFieldsRecordsRead(5);
    //
    // assertEquals(10, usage.getBytesReadSinceLastReport());
    // assertEquals(5, usage.getFieldsReadSinceLastReport());
    // assertEquals(1, usage.getRecordsReadSinceLastReport());
    //
    // try {
    // Thread.sleep(1500);
    // } catch (InterruptedException e) {
    // assertTrue(false);
    // }
    //
    // usage.addBytesRead(50);
    // Mockito.verify(persister, Mockito.times(1)).persistUsage("job1", 60L, 5L,
    // 1L);
    //
    // assertEquals(0, usage.getBytesReadSinceLastReport());
    // assertEquals(0, usage.getFieldsReadSinceLastReport());
    // assertEquals(0, usage.getRecordsReadSinceLastReport());
    //
    //
    // // Write another
    // usage.addBytesRead(20);
    // usage.addFieldsRecordsRead(10);
    //
    // assertEquals(20, usage.getBytesReadSinceLastReport());
    // assertEquals(10, usage.getFieldsReadSinceLastReport());
    // assertEquals(1, usage.getRecordsReadSinceLastReport());
    //
    //
    // try {
    // Thread.sleep(1500);
    // } catch (InterruptedException e) {
    // assertTrue(false);
    // }
    //
    // usage.addBytesRead(10);
    // Mockito.verify(persister, Mockito.times(1)).persistUsage("job1", 30L,
    // 10L, 1L);
    //
    // assertEquals(0, usage.getBytesReadSinceLastReport());
    // assertEquals(0, usage.getFieldsReadSinceLastReport());
    // assertEquals(0, usage.getRecordsReadSinceLastReport());
    //
    // }
}
