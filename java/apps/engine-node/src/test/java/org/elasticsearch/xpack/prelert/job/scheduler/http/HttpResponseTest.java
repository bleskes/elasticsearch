package org.elasticsearch.xpack.prelert.job.scheduler.http;

import org.elasticsearch.test.ESTestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class HttpResponseTest extends ESTestCase {

    public void testGetResponseAsStream() throws IOException {
        InputStream stream = new ByteArrayInputStream("foo\nbar".getBytes(StandardCharsets.UTF_8));
        HttpResponse response = new HttpResponse(stream, 200);

        assertEquals("foo\nbar", response.getResponseAsString());
        assertEquals(200, response.getResponseCode());
    }

    public void testGetResponseAsStream_GivenStreamThrows() throws IOException {
        InputStream stream = mock(InputStream.class);
        HttpResponse response = new HttpResponse(stream, 200);

        try {
            response.getResponseAsString();
            fail();
        } catch (UncheckedIOException e) {
            verify(stream).close();
        }
    }
}
