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

import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.common.SuppressLoggerChecks;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.xcontent.XContent;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.xpack.monitoring.exporter.http.PublishableHttpResource.CheckResponse;

import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.function.Supplier;

import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests {@link PublishableHttpResource}.
 */
public class PublishableHttpResourceTests extends AbstractPublishableHttpResourceTestCase {

    private final String ownerType = "ownerthing";
    private final String resourceBasePath = "/_fake";
    private final String resourceName = ".my_thing";
    private final String resourceType = "thingamajig";
    private final Logger logger = mock(Logger.class);
    private final HttpEntity entity = mock(HttpEntity.class);
    private final Supplier<HttpEntity> body = () -> entity;

    private final PublishableHttpResource resource =
            new MockHttpResource(owner, masterTimeout, PublishableHttpResource.NO_BODY_PARAMETERS);

    public void testCheckForResourceExists() throws IOException {
        assertCheckForResource(successfulCheckStatus(), CheckResponse.EXISTS, "{} [{}] found on the [{}] {}");
    }

    public void testCheckForResourceDoesNotExist() throws IOException {
        assertCheckForResource(notFoundCheckStatus(), CheckResponse.DOES_NOT_EXIST, "{} [{}] does not exist on the [{}] {}");
    }

    public void testCheckForResourceUnexpectedResponse() throws IOException {
        final String endpoint = concatenateEndpoint(resourceBasePath, resourceName);
        final RestStatus failedStatus = failedCheckStatus();
        final Response response = response("GET", endpoint, failedStatus);

        when(client.performRequest("GET", endpoint, getParameters(resource.getParameters()))).thenReturn(response);

        sometimesAssertSimpleCheckForResource(client, logger, resourceBasePath, resourceName, resourceType, CheckResponse.ERROR, response);

        verify(logger).trace("checking if {} [{}] exists on the [{}] {}", resourceType, resourceName, owner, ownerType);
        verify(client).performRequest("GET", endpoint, getParameters(resource.getParameters()));
        verify(logger).error(any(org.apache.logging.log4j.util.Supplier.class), any(ResponseException.class));

        verifyNoMoreInteractions(client, logger);
    }

    public void testVersionCheckForResourceExists() throws IOException {
        assertVersionCheckForResource(successfulCheckStatus(), CheckResponse.EXISTS, randomInt(), "{} [{}] found on the [{}] {}");
    }

    public void testVersionCheckForResourceDoesNotExist() throws IOException {
        if (randomBoolean()) {
            // it literally does not exist
            assertVersionCheckForResource(notFoundCheckStatus(), CheckResponse.DOES_NOT_EXIST,
                                          randomInt(), "{} [{}] does not exist on the [{}] {}");
        } else {
            // it DOES exist, but the version needs to be replaced
            assertVersionCheckForResource(successfulCheckStatus(), CheckResponse.DOES_NOT_EXIST,
                                          randomInt(), "{} [{}] found on the [{}] {}");
        }
    }

    public void testVersionCheckForResourceUnexpectedResponse() throws IOException {
        final String endpoint = concatenateEndpoint(resourceBasePath, resourceName);
        final RestStatus failedStatus = failedCheckStatus();
        final Response response = response("GET", endpoint, failedStatus);
        final XContent xContent = mock(XContent.class);
        final int minimumVersion = randomInt();

        when(client.performRequest("GET", endpoint, getParameters(resource.getParameters()))).thenReturn(response);

        assertThat(resource.versionCheckForResource(client, logger,
                                                    resourceBasePath, resourceName, resourceType, owner, ownerType,
                                                    xContent, minimumVersion),
                   is(CheckResponse.ERROR));

        verify(logger).trace("checking if {} [{}] exists on the [{}] {}", resourceType, resourceName, owner, ownerType);
        verify(client).performRequest("GET", endpoint, getParameters(resource.getParameters()));
        verify(logger).error(any(org.apache.logging.log4j.util.Supplier.class), any(ResponseException.class));

        verifyNoMoreInteractions(client, logger);
    }

    public void testVersionCheckForResourceMalformedResponse() throws IOException {
        final String endpoint = concatenateEndpoint(resourceBasePath, resourceName);
        final RestStatus okStatus = successfulCheckStatus();
        final int minimumVersion = randomInt();
        final HttpEntity entity = entityForResource(CheckResponse.ERROR, resourceName, minimumVersion);
        final Response response = response("GET", endpoint, okStatus, entity);
        final XContent xContent = mock(XContent.class);

        when(client.performRequest("GET", endpoint, getParameters(resource.getParameters()))).thenReturn(response);

        assertThat(resource.versionCheckForResource(client, logger,
                                                    resourceBasePath, resourceName, resourceType, owner, ownerType,
                                                    xContent, minimumVersion),
                   is(CheckResponse.ERROR));

        verify(logger).trace("checking if {} [{}] exists on the [{}] {}", resourceType, resourceName, owner, ownerType);
        verify(logger).debug("{} [{}] found on the [{}] {}", resourceType, resourceName, owner, ownerType);
        verify(client).performRequest("GET", endpoint, getParameters(resource.getParameters()));
        verify(logger).error(any(org.apache.logging.log4j.util.Supplier.class), any(ResponseException.class));

        verifyNoMoreInteractions(client, logger);
    }

    public void testCheckForResourceErrors() throws IOException {
        final String endpoint = concatenateEndpoint(resourceBasePath, resourceName);
        final RestStatus failedStatus = failedCheckStatus();
        final ResponseException responseException = responseException("GET", endpoint, failedStatus);
        final Exception e = randomFrom(new IOException("expected"), new RuntimeException("expected"), responseException);
        final Response response = e == responseException ? responseException.getResponse() : null;

        when(client.performRequest("GET", endpoint, getParameters(resource.getParameters()))).thenThrow(e);

        sometimesAssertSimpleCheckForResource(client, logger, resourceBasePath, resourceName, resourceType, CheckResponse.ERROR, response);

        verify(logger).trace("checking if {} [{}] exists on the [{}] {}", resourceType, resourceName, owner, ownerType);
        verify(client).performRequest("GET", endpoint, getParameters(resource.getParameters()));
        verify(logger).error(any(org.apache.logging.log4j.util.Supplier.class), eq(e));

        verifyNoMoreInteractions(client, logger);
    }

    public void testPutResourceTrue() throws IOException {
        assertPutResource(successfulPublishStatus(), true);
    }

    public void testPutResourceFalse() throws IOException {
        assertPutResource(failedPublishStatus(), false);
    }

    public void testPutResourceFalseWithException() throws IOException {
        final String endpoint = concatenateEndpoint(resourceBasePath, resourceName);
        final Exception e = randomFrom(new IOException("expected"), new RuntimeException("expected"));

        when(client.performRequest("PUT", endpoint, resource.getParameters(), entity)).thenThrow(e);

        assertThat(resource.putResource(client, logger, resourceBasePath, resourceName, body, resourceType, owner, ownerType), is(false));

        verify(logger).trace("uploading {} [{}] to the [{}] {}", resourceType, resourceName, owner, ownerType);
        verify(client).performRequest("PUT", endpoint, resource.getParameters(), entity);
        verify(logger).error(any(org.apache.logging.log4j.util.Supplier.class), eq(e));

        verifyNoMoreInteractions(client, logger);
    }

    public void testDeleteResourceTrue() throws IOException {
        final RestStatus status = randomFrom(successfulCheckStatus(), notFoundCheckStatus());

        assertDeleteResource(status, true);
    }

    public void testDeleteResourceFalse() throws IOException {
        assertDeleteResource(failedCheckStatus(), false);
    }

    public void testDeleteResourceErrors() throws IOException {
        final String endpoint = concatenateEndpoint(resourceBasePath, resourceName);
        final RestStatus failedStatus = failedCheckStatus();
        final ResponseException responseException = responseException("DELETE", endpoint, failedStatus);
        final Exception e = randomFrom(new IOException("expected"), new RuntimeException("expected"), responseException);
        final Map<String, String> deleteParameters = deleteParameters(resource.getParameters());

        when(client.performRequest("DELETE", endpoint, deleteParameters)).thenThrow(e);

        assertThat(resource.deleteResource(client, logger, resourceBasePath, resourceName, resourceType, owner, ownerType), is(false));

        verify(logger).trace("deleting {} [{}] from the [{}] {}", resourceType, resourceName, owner, ownerType);
        verify(client).performRequest("DELETE", endpoint, deleteParameters);
        verify(logger).error(any(org.apache.logging.log4j.util.Supplier.class), eq(e));

        verifyNoMoreInteractions(client, logger);
    }

    public void testParameters() {
        assertParameters(resource);
    }

    public void testDoCheckAndPublishIgnoresPublishWhenCheckErrors() {
        final PublishableHttpResource resource =
                new MockHttpResource(owner, masterTimeout, PublishableHttpResource.NO_BODY_PARAMETERS, CheckResponse.ERROR, true);

        assertThat(resource.doCheckAndPublish(client), is(false));
    }

    public void testDoCheckAndPublish() {
        // not an error (the third state)
        final PublishableHttpResource.CheckResponse exists = randomBoolean() ? CheckResponse.EXISTS : CheckResponse.DOES_NOT_EXIST;
        final boolean publish = randomBoolean();

        final PublishableHttpResource resource =
                new MockHttpResource(owner, masterTimeout, PublishableHttpResource.NO_BODY_PARAMETERS, exists, publish);

        assertThat(resource.doCheckAndPublish(client), is(exists == CheckResponse.EXISTS || publish));
    }

    public void testShouldReplaceResourceRethrowsIOException() throws IOException {
        final Response response = mock(Response.class);
        final HttpEntity entity = mock(HttpEntity.class);
        final XContent xContent = mock(XContent.class);

        when(response.getEntity()).thenReturn(entity);
        when(entity.getContent()).thenThrow(new IOException("TEST - expected"));

        expectThrows(IOException.class, () -> resource.shouldReplaceResource(response, xContent, resourceName, randomInt()));
    }

    public void testShouldReplaceResourceThrowsExceptionForMalformedResponse() throws IOException {
        final Response response = mock(Response.class);
        final HttpEntity entity = entityForResource(CheckResponse.ERROR, resourceName, randomInt());
        final XContent xContent = XContentType.JSON.xContent();

        when(response.getEntity()).thenReturn(entity);

        expectThrows(RuntimeException.class, () -> resource.shouldReplaceResource(response, xContent, resourceName, randomInt()));
    }

    public void testShouldReplaceResourceReturnsTrueVersionIsNotExpected() throws IOException {
        final int minimumVersion = randomInt();
        final Response response = mock(Response.class);
        final HttpEntity entity = entityForResource(CheckResponse.DOES_NOT_EXIST, resourceName, minimumVersion);
        final XContent xContent = XContentType.JSON.xContent();

        when(response.getEntity()).thenReturn(entity);

        assertThat(resource.shouldReplaceResource(response, xContent, resourceName, minimumVersion), is(true));
    }

    public void testShouldReplaceResourceChecksVersion() throws IOException {
        final int minimumVersion = randomInt();
        final int version = randomInt();
        final boolean shouldReplace = version < minimumVersion;

        final Response response = mock(Response.class);
        // { "resourceName": { "version": randomLong } }
        final HttpEntity entity =
                new StringEntity("{\"" + resourceName + "\":{\"version\":" + version + "}}", ContentType.APPLICATION_JSON);
        final XContent xContent = XContentType.JSON.xContent();

        when(response.getEntity()).thenReturn(entity);

        assertThat(resource.shouldReplaceResource(response, xContent, resourceName, minimumVersion), is(shouldReplace));
    }

    @SuppressLoggerChecks(reason = "mock logger used")
    private void assertCheckForResource(final RestStatus status, final CheckResponse expected, final String debugLogMessage)
            throws IOException {
        final String endpoint = concatenateEndpoint(resourceBasePath, resourceName);
        final Response response = response("GET", endpoint, status);

        when(client.performRequest("GET", endpoint, getParameters(resource.getParameters()))).thenReturn(response);

        sometimesAssertSimpleCheckForResource(client, logger, resourceBasePath, resourceName, resourceType, expected, response);

        verify(logger).trace("checking if {} [{}] exists on the [{}] {}", resourceType, resourceName, owner, ownerType);
        verify(client).performRequest("GET", endpoint, getParameters(resource.getParameters()));

        if (expected == CheckResponse.EXISTS || expected == CheckResponse.DOES_NOT_EXIST) {
            verify(response).getStatusLine();
        } else {
            verify(response).getStatusLine();
            verify(response).getRequestLine();
            verify(response).getHost();
            verify(response).getEntity();
        }

        verify(logger).debug(debugLogMessage, resourceType, resourceName, owner, ownerType);

        verifyNoMoreInteractions(client, response, logger);
    }

    @SuppressLoggerChecks(reason = "mock logger used")
    private void assertVersionCheckForResource(final RestStatus status, final CheckResponse expected,
                                               final int minimumVersion,
                                               final String debugLogMessage)
            throws IOException {

        final String endpoint = concatenateEndpoint(resourceBasePath, resourceName);
        final boolean shouldReplace = status == RestStatus.OK && expected == CheckResponse.DOES_NOT_EXIST;
        final HttpEntity entity = status == RestStatus.OK ? entityForResource(expected, resourceName, minimumVersion) : null;
        final Response response = response("GET", endpoint, status, entity);
        final XContent xContent = XContentType.JSON.xContent();

        when(client.performRequest("GET", endpoint, getParameters(resource.getParameters()))).thenReturn(response);

        assertThat(resource.versionCheckForResource(client, logger,
                                                    resourceBasePath, resourceName, resourceType, owner, ownerType,
                                                    xContent, minimumVersion),
                   is(expected));

        verify(logger).trace("checking if {} [{}] exists on the [{}] {}", resourceType, resourceName, owner, ownerType);
        verify(client).performRequest("GET", endpoint, getParameters(resource.getParameters()));

        if (shouldReplace || expected == CheckResponse.EXISTS) {
            verify(response).getStatusLine();
            verify(response).getEntity();
        } else if (expected == CheckResponse.DOES_NOT_EXIST) {
            verify(response).getStatusLine();
        } else {
            verify(response).getStatusLine();
            verify(response).getRequestLine();
            verify(response).getHost();
            verify(response).getEntity();
        }

        verify(logger).debug(debugLogMessage, resourceType, resourceName, owner, ownerType);

        verifyNoMoreInteractions(client, response, logger);
    }

    private void assertPutResource(final RestStatus status, final boolean expected) throws IOException {
        final String endpoint = concatenateEndpoint(resourceBasePath, resourceName);
        final Response response = response("PUT", endpoint, status);

        when(client.performRequest("PUT", endpoint, resource.getParameters(), entity)).thenReturn(response);

        assertThat(resource.putResource(client, logger, resourceBasePath, resourceName, body, resourceType, owner, ownerType),
                   is(expected));

        verify(client).performRequest("PUT", endpoint, resource.getParameters(), entity);
        verify(response).getStatusLine();

        verify(logger).trace("uploading {} [{}] to the [{}] {}", resourceType, resourceName, owner, ownerType);

        if (expected) {
            verify(logger).debug("{} [{}] uploaded to the [{}] {}", resourceType, resourceName, owner, ownerType);
        } else {
            ArgumentCaptor<RuntimeException> e = ArgumentCaptor.forClass(RuntimeException.class);

            verify(logger).error(any(org.apache.logging.log4j.util.Supplier.class), e.capture());

            assertThat(e.getValue().getMessage(),
                       is("[" + resourceBasePath + "/" + resourceName + "] responded with [" + status.getStatus() + "]"));
        }

        verifyNoMoreInteractions(client, response, logger, entity);
    }

    private void sometimesAssertSimpleCheckForResource(final RestClient client, final Logger logger,
                                                       final String resourceBasePath,
                                                       final String resourceName, final String resourceType,
                                                       final CheckResponse expected, final Response response) {
        // sometimes use the simple check
        if (randomBoolean()) {
            assertThat(resource.simpleCheckForResource(client, logger, resourceBasePath, resourceName, resourceType, owner, ownerType),
                    is(expected));
        } else {
            final Tuple<CheckResponse, Response> responseTuple =
                    resource.checkForResource(client, logger, resourceBasePath, resourceName, resourceType, owner, ownerType,
                                              PublishableHttpResource.GET_EXISTS, PublishableHttpResource.GET_DOES_NOT_EXIST);

            assertThat(responseTuple.v1(), is(expected));
            assertThat(responseTuple.v2(), is(response));
        }
    }

    private void assertDeleteResource(final RestStatus status, final boolean expected) throws IOException {
        final String endpoint = concatenateEndpoint(resourceBasePath, resourceName);
        final Response response = response("DELETE", endpoint, status);
        final Map<String, String> deleteParameters = deleteParameters(resource.getParameters());

        when(client.performRequest("DELETE", endpoint, deleteParameters)).thenReturn(response);

        assertThat(resource.deleteResource(client, logger, resourceBasePath, resourceName, resourceType, owner, ownerType), is(expected));

        verify(client).performRequest("DELETE", endpoint, deleteParameters);
        verify(response).getStatusLine();

        verify(logger).trace("deleting {} [{}] from the [{}] {}", resourceType, resourceName, owner, ownerType);

        if (expected) {
            verify(logger).debug("{} [{}] deleted from the [{}] {}", resourceType, resourceName, owner, ownerType);
        } else {
            ArgumentCaptor<RuntimeException> e = ArgumentCaptor.forClass(RuntimeException.class);

            verify(logger).error(any(org.apache.logging.log4j.util.Supplier.class), e.capture());

            assertThat(e.getValue().getMessage(),
                       is("[" + resourceBasePath + "/" + resourceName + "] responded with [" + status.getStatus() + "]"));
        }

        verifyNoMoreInteractions(client, response, logger, entity);
    }

}
