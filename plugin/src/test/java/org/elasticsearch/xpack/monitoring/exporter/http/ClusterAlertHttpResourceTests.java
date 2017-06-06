/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */
package org.elasticsearch.xpack.monitoring.exporter.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.elasticsearch.Version;
import org.elasticsearch.client.Response;
import org.elasticsearch.common.xcontent.XContent;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.xpack.monitoring.exporter.ClusterAlertsUtil;
import org.elasticsearch.xpack.monitoring.exporter.http.PublishableHttpResource.CheckResponse;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link ClusterAlertHttpResource}.
 */
public class ClusterAlertHttpResourceTests extends AbstractPublishableHttpResourceTestCase {

    private final XPackLicenseState licenseState = mock(XPackLicenseState.class);
    private final String watchId = randomFrom(ClusterAlertsUtil.WATCH_IDS);
    private final String watchValue = "{\"totally-valid\":{}}";
    private final int minimumVersion = randomFrom(ClusterAlertsUtil.LAST_UPDATED_VERSION, Version.CURRENT.id);

    private final ClusterAlertHttpResource resource = new ClusterAlertHttpResource(owner, licenseState, () -> watchId, () -> watchValue);

    public void testWatchToHttpEntity() throws IOException {
        final byte[] watchValueBytes = watchValue.getBytes(ContentType.APPLICATION_JSON.getCharset());
        final byte[] actualBytes = new byte[watchValueBytes.length];
        final HttpEntity entity = resource.watchToHttpEntity();

        assertThat(entity.getContentType().getValue(), is(ContentType.APPLICATION_JSON.toString()));

        final InputStream byteStream = entity.getContent();

        assertThat(byteStream.available(), is(watchValueBytes.length));
        assertThat(byteStream.read(actualBytes), is(watchValueBytes.length));
        assertArrayEquals(watchValueBytes, actualBytes);

        assertThat(byteStream.available(), is(0));
    }

    public void testDoCheckGetWatchExists() throws IOException {
        when(licenseState.isMonitoringClusterAlertsAllowed()).thenReturn(true);

        final HttpEntity entity = entityForClusterAlert(CheckResponse.EXISTS, minimumVersion);

        doCheckWithStatusCode(resource, "/_xpack/watcher/watch", watchId, successfulCheckStatus(),
                              CheckResponse.EXISTS, entity);
    }

    public void testDoCheckGetWatchDoesNotExist() throws IOException {
        when(licenseState.isMonitoringClusterAlertsAllowed()).thenReturn(true);

        if (randomBoolean()) {
            // it does not exist because it's literally not there
            assertCheckDoesNotExist(resource, "/_xpack/watcher/watch", watchId);
        } else {
            // it does not exist because we need to replace it
            final HttpEntity entity = entityForClusterAlert(CheckResponse.DOES_NOT_EXIST, minimumVersion);

            doCheckWithStatusCode(resource, "/_xpack/watcher/watch", watchId, successfulCheckStatus(),
                                  CheckResponse.DOES_NOT_EXIST, entity);
        }
    }

    public void testDoCheckWithExceptionGetWatchError() throws IOException {
        when(licenseState.isMonitoringClusterAlertsAllowed()).thenReturn(true);

        if (randomBoolean()) {
            // error because of a server error
            assertCheckWithException(resource, "/_xpack/watcher/watch", watchId);
        } else {
            // error because of a malformed response
            final HttpEntity entity = entityForClusterAlert(CheckResponse.ERROR, minimumVersion);

            doCheckWithStatusCode(resource, "/_xpack/watcher/watch", watchId, successfulCheckStatus(),
                                  CheckResponse.ERROR, entity);
        }
    }

    public void testDoCheckAsDeleteWatchExists() throws IOException {
        when(licenseState.isMonitoringClusterAlertsAllowed()).thenReturn(false);

        assertCheckAsDeleteExists(resource, "/_xpack/watcher/watch", watchId);
    }

    public void testDoCheckWithExceptionAsDeleteWatchError() throws IOException {
        when(licenseState.isMonitoringClusterAlertsAllowed()).thenReturn(false);

        assertCheckAsDeleteWithException(resource, "/_xpack/watcher/watch", watchId);
    }

    public void testDoPublishTrue() throws IOException {
        assertPublishSucceeds(resource, "/_xpack/watcher/watch", watchId, StringEntity.class);
    }

    public void testDoPublishFalse() throws IOException {
        assertPublishFails(resource, "/_xpack/watcher/watch", watchId, StringEntity.class);
    }

    public void testDoPublishFalseWithException() throws IOException {
        assertPublishWithException(resource, "/_xpack/watcher/watch", watchId, StringEntity.class);
    }

    public void testShouldReplaceClusterAlertRethrowsIOException() throws IOException {
        final Response response = mock(Response.class);
        final HttpEntity entity = mock(HttpEntity.class);
        final XContent xContent = mock(XContent.class);

        when(response.getEntity()).thenReturn(entity);
        when(entity.getContent()).thenThrow(new IOException("TEST - expected"));

        expectThrows(IOException.class, () -> resource.shouldReplaceClusterAlert(response, xContent, randomInt()));
    }

    public void testShouldReplaceClusterAlertThrowsExceptionForMalformedResponse() throws IOException {
        final Response response = mock(Response.class);
        final HttpEntity entity = entityForClusterAlert(CheckResponse.ERROR, randomInt());
        final XContent xContent = XContentType.JSON.xContent();

        when(response.getEntity()).thenReturn(entity);

        expectThrows(RuntimeException.class, () -> resource.shouldReplaceClusterAlert(response, xContent, randomInt()));
    }

    public void testShouldReplaceClusterAlertReturnsTrueVersionIsNotExpected() throws IOException {
        final int minimumVersion = randomInt();
        final Response response = mock(Response.class);
        final HttpEntity entity = entityForClusterAlert(CheckResponse.DOES_NOT_EXIST, minimumVersion);
        final XContent xContent = XContentType.JSON.xContent();

        when(response.getEntity()).thenReturn(entity);

        assertThat(resource.shouldReplaceClusterAlert(response, xContent, minimumVersion), is(true));
    }

    public void testShouldReplaceCheckAlertChecksVersion() throws IOException {
        final int minimumVersion = randomInt();
        final int version = randomInt();
        final boolean shouldReplace = version < minimumVersion;

        final Response response = mock(Response.class);
        final HttpEntity entity = entityForClusterAlert(CheckResponse.EXISTS, version);
        final XContent xContent = XContentType.JSON.xContent();

        when(response.getEntity()).thenReturn(entity);

        assertThat(resource.shouldReplaceClusterAlert(response, xContent, minimumVersion), is(shouldReplace));
    }

    public void testParameters() {
        final Map<String, String> parameters = new HashMap<>(resource.getParameters());

        assertThat(parameters.remove("filter_path"), is("metadata.xpack.version_created"));
        assertThat(parameters.isEmpty(), is(true));
    }

}
