
package org.elasticsearch.xpack.prelert.job.data;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DataStreamerThreadTest extends ESTestCase {
    private static final String JOB_ID = "foo";
    private static final String CONTENT_ENCODING = "application/json";

    @Mock
    private DataStreamer dataStreamer;
    @Mock
    private DataLoadParams params;
    @Mock
    private InputStream inputStream;

    private DataStreamerThread dataStreamerThread;

    @Before
    public void setUpMocks() {
        MockitoAnnotations.initMocks(this);
        dataStreamerThread = new DataStreamerThread(dataStreamer, JOB_ID, CONTENT_ENCODING, params, inputStream);
    }

    @After
    public void verifyInputStreamClosed() throws IOException {
        verify(inputStream).close();
    }

    public void testRun() throws Exception {
        DataCounts counts = new DataCounts();
        counts.setBucketCount(42L);
        when(dataStreamer.streamData(CONTENT_ENCODING, JOB_ID, inputStream, params)).thenReturn(counts);

        dataStreamerThread.run();

        assertEquals(JOB_ID, dataStreamerThread.getJobId());
        assertEquals(counts, dataStreamerThread.getDataCounts());
        assertFalse(dataStreamerThread.getIOException().isPresent());
        assertFalse(dataStreamerThread.getJobException().isPresent());
    }

    public void testRun_GivenIOException() throws Exception {
        when(dataStreamer.streamData(CONTENT_ENCODING, JOB_ID, inputStream, params)).thenThrow(new IOException("prelert"));

        dataStreamerThread.run();

        assertEquals(JOB_ID, dataStreamerThread.getJobId());
        assertNull(dataStreamerThread.getDataCounts());
        assertEquals("prelert", dataStreamerThread.getIOException().get().getMessage());
        assertFalse(dataStreamerThread.getJobException().isPresent());
    }

    public void testRun_GivenJobException() throws Exception {
        when(dataStreamer.streamData(CONTENT_ENCODING, JOB_ID, inputStream, params))
                .thenThrow(ExceptionsHelper.invalidRequestException("job failed", ErrorCodes.JOB_ID_TAKEN));

        dataStreamerThread.run();

        assertEquals(JOB_ID, dataStreamerThread.getJobId());
        assertNull(dataStreamerThread.getDataCounts());
        assertFalse(dataStreamerThread.getIOException().isPresent());
        assertEquals("job failed", dataStreamerThread.getJobException().get().getMessage());
    }
}
