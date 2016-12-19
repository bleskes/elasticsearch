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

public class HttpResponseTests extends ESTestCase {

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
