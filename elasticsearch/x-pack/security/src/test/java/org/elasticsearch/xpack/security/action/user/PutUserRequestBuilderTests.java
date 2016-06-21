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

package org.elasticsearch.xpack.security.action.user;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.test.ESTestCase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

public class PutUserRequestBuilderTests extends ESTestCase {

    public void testNullValuesForEmailAndFullName() throws IOException {
        final String json = "{\n" +
                "    \"roles\": [\n" +
                "      \"kibana4\"\n" +
                "    ],\n" +
                "    \"full_name\": null,\n" +
                "    \"email\": null,\n" +
                "    \"metadata\": {}\n" +
                "}";

        PutUserRequestBuilder builder = new PutUserRequestBuilder(mock(Client.class));
        builder.source("kibana4", new BytesArray(json.getBytes(StandardCharsets.UTF_8)));

        PutUserRequest request = builder.request();
        assertThat(request.username(), is("kibana4"));
        assertThat(request.roles(), arrayContaining("kibana4"));
        assertThat(request.fullName(), nullValue());
        assertThat(request.email(), nullValue());
        assertThat(request.metadata().isEmpty(), is(true));
    }

    public void testMissingEmailFullName() throws Exception {
        final String json = "{\n" +
                "    \"roles\": [\n" +
                "      \"kibana4\"\n" +
                "    ],\n" +
                "    \"metadata\": {}\n" +
                "}";

        PutUserRequestBuilder builder = new PutUserRequestBuilder(mock(Client.class));
        builder.source("kibana4", new BytesArray(json.getBytes(StandardCharsets.UTF_8)));

        PutUserRequest request = builder.request();
        assertThat(request.username(), is("kibana4"));
        assertThat(request.roles(), arrayContaining("kibana4"));
        assertThat(request.fullName(), nullValue());
        assertThat(request.email(), nullValue());
        assertThat(request.metadata().isEmpty(), is(true));
    }

    public void testWithFullNameAndEmail() throws IOException {
        final String json = "{\n" +
                "    \"roles\": [\n" +
                "      \"kibana4\"\n" +
                "    ],\n" +
                "    \"full_name\": \"Kibana User\",\n" +
                "    \"email\": \"kibana@elastic.co\",\n" +
                "    \"metadata\": {}\n" +
                "}";

        PutUserRequestBuilder builder = new PutUserRequestBuilder(mock(Client.class));
        builder.source("kibana4", new BytesArray(json.getBytes(StandardCharsets.UTF_8)));

        PutUserRequest request = builder.request();
        assertThat(request.username(), is("kibana4"));
        assertThat(request.roles(), arrayContaining("kibana4"));
        assertThat(request.fullName(), is("Kibana User"));
        assertThat(request.email(), is("kibana@elastic.co"));
        assertThat(request.metadata().isEmpty(), is(true));
    }

    public void testInvalidFullname() throws IOException {
        final String json = "{\n" +
                "    \"roles\": [\n" +
                "      \"kibana4\"\n" +
                "    ],\n" +
                "    \"full_name\": [ \"Kibana User\" ],\n" +
                "    \"email\": \"kibana@elastic.co\",\n" +
                "    \"metadata\": {}\n" +
                "}";

        PutUserRequestBuilder builder = new PutUserRequestBuilder(mock(Client.class));
        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class,
                () -> builder.source("kibana4", new BytesArray(json.getBytes(StandardCharsets.UTF_8))));
        assertThat(e.getMessage(), containsString("expected field [full_name] to be of type string"));
    }

    public void testInvalidEmail() throws IOException {
        final String json = "{\n" +
                "    \"roles\": [\n" +
                "      \"kibana4\"\n" +
                "    ],\n" +
                "    \"full_name\": \"Kibana User\",\n" +
                "    \"email\": [ \"kibana@elastic.co\" ],\n" +
                "    \"metadata\": {}\n" +
                "}";

        PutUserRequestBuilder builder = new PutUserRequestBuilder(mock(Client.class));
        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class,
                () -> builder.source("kibana4", new BytesArray(json.getBytes(StandardCharsets.UTF_8))));
        assertThat(e.getMessage(), containsString("expected field [email] to be of type string"));
    }
}
