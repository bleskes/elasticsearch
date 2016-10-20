
package org.elasticsearch.xpack.prelert.job.data;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.exceptions.UnknownJobException;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.DataLoadParams;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonParseException;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobInUseException;
import org.elasticsearch.xpack.prelert.job.exceptions.TooManyJobsException;
import org.elasticsearch.xpack.prelert.job.process.exceptions.MalformedJsonException;
import org.elasticsearch.xpack.prelert.job.process.exceptions.MissingFieldException;
import org.elasticsearch.xpack.prelert.job.process.exceptions.NativeProcessRunException;
import org.elasticsearch.xpack.prelert.job.status.HighProportionOfBadTimestampsException;
import org.elasticsearch.xpack.prelert.job.status.OutOfOrderRecordsException;

public class DataStreamerTest extends ESTestCase {

    public void testConstructor_GivenNullDataProcessor() {

        ESTestCase.expectThrows(NullPointerException.class, () -> new DataStreamer(null));
    }

    public void testStreamData_GivenNoContentEncodingAndNoPersistBaseDir()
            throws UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            TooManyJobsException, IOException, MalformedJsonException, JobException {

        DataProcessor dataProcessor = mock(DataProcessor.class);
        DataStreamer dataStreamer = new DataStreamer(dataProcessor);
        InputStream inputStream = mock(InputStream.class);
        DataLoadParams params = mock(DataLoadParams.class);

        when(dataProcessor.processData("foo", inputStream, params)).thenReturn(new DataCounts());

        dataStreamer.streamData("", "foo", inputStream, params);

        verify(dataProcessor).processData("foo", inputStream, params);
        Mockito.verifyNoMoreInteractions(dataProcessor);
    }

    public void testStreamData_ExpectsGzipButNotCompressed()
            throws JsonParseException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, TooManyJobsException, MalformedJsonException,
            IOException, JobException {
        DataProcessor dataProcessor = mock(DataProcessor.class);
        DataStreamer dataStreamer = new DataStreamer(dataProcessor);
        InputStream inputStream = mock(InputStream.class);
        DataLoadParams params = mock(DataLoadParams.class);

        try {
            dataStreamer.streamData("gzip", "foo", inputStream, params);
            fail("content encoding : gzip with uncompressed data should throw");
        } catch (JobException e) {
            assertEquals(ErrorCodes.UNCOMPRESSED_DATA, e.getErrorCode());
        }
    }

    public void testStreamData_ExpectsGzipUsesGZipStream()
            throws JsonParseException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, TooManyJobsException, MalformedJsonException,
            IOException, JobException {

        PipedInputStream pipedIn = new PipedInputStream();
        PipedOutputStream pipedOut = new PipedOutputStream(pipedIn);
        try (GZIPOutputStream gzip = new GZIPOutputStream(pipedOut)) {
            gzip.write("Hello World compressed".getBytes(StandardCharsets.UTF_8));

            DataProcessor dataProcessor = mock(DataProcessor.class);
            DataStreamer dataStreamer = new DataStreamer(dataProcessor);
            DataLoadParams params = mock(DataLoadParams.class);

            when(dataProcessor.processData(Mockito.anyString(),
                    Mockito.any(InputStream.class),
                    Mockito.any(DataLoadParams.class)))
                    .thenReturn(new DataCounts());

            dataStreamer.streamData("gzip", "foo", pipedIn, params);

            // submitDataLoadJob should be called with a GZIPInputStream
            ArgumentCaptor<InputStream> streamArg = ArgumentCaptor.forClass(InputStream.class);

            verify(dataProcessor).processData(Mockito.anyString(),
                    streamArg.capture(),
                    Mockito.any(DataLoadParams.class));

            assertTrue(streamArg.getValue() instanceof GZIPInputStream);
        }
    }
}
