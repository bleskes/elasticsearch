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

package org.elasticsearch.integration;

import org.apache.http.Header;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Response;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.SecurityIntegTestCase;
import org.elasticsearch.test.SecuritySettingsSource;
import org.elasticsearch.xpack.XPackPlugin;
import org.elasticsearch.xpack.security.Security;
import org.elasticsearch.xpack.security.authc.support.SecuredString;
import org.elasticsearch.xpack.security.authc.support.UsernamePasswordToken;

import java.io.IOException;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class BulkUpdateTests extends SecurityIntegTestCase {

    @Override
    public Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(NetworkModule.HTTP_ENABLED.getKey(), true)
                .put(XPackPlugin.featureEnabledSetting(Security.DLS_FLS_FEATURE), randomBoolean())
                .build();
    }

    public void testThatBulkUpdateDoesNotLoseFields() {
        assertEquals(DocWriteResponse.Result.CREATED,
                client().prepareIndex("index1", "type").setSource("{\"test\": \"test\"}").setId("1").get().getResult());
        GetResponse getResponse = internalCluster().transportClient().prepareGet("index1", "type", "1").setFields("test").get();
        assertThat(getResponse.getField("test").getValue(), equalTo("test"));

        if (randomBoolean()) {
            flushAndRefresh();
        }

        // update with a new field
        assertEquals(DocWriteResponse.Result.UPDATED, internalCluster().transportClient().prepareUpdate("index1", "type", "1")
                .setDoc("{\"not test\": \"not test\"}").get().getResult());
        getResponse = internalCluster().transportClient().prepareGet("index1", "type", "1").setFields("test", "not test").get();
        assertThat(getResponse.getField("test").getValue(), equalTo("test"));
        assertThat(getResponse.getField("not test").getValue(), equalTo("not test"));

        // this part is important. Without this, the document may be read from the translog which would bypass the bug where
        // FLS kicks in because the request can't be found and only returns meta fields
        flushAndRefresh();

        // do it in a bulk
        BulkResponse response = internalCluster().transportClient().prepareBulk().add(client().prepareUpdate("index1", "type", "1")
                .setDoc("{\"bulk updated\": \"bulk updated\"}")).get();
        assertEquals(DocWriteResponse.Result.UPDATED, response.getItems()[0].getResponse().getResult());
        getResponse = internalCluster().transportClient().prepareGet("index1", "type", "1").
                setFields("test", "not test", "bulk updated").get();
        assertThat(getResponse.getField("test").getValue(), equalTo("test"));
        assertThat(getResponse.getField("not test").getValue(), equalTo("not test"));
        assertThat(getResponse.getField("bulk updated").getValue(), equalTo("bulk updated"));
    }

    public void testThatBulkUpdateDoesNotLoseFieldsHttp() throws IOException {
        final String path = "/index1/type/1";
        final Header basicAuthHeader = new BasicHeader("Authorization",
                UsernamePasswordToken.basicAuthHeaderValue(SecuritySettingsSource.DEFAULT_USER_NAME,
                        new SecuredString(SecuritySettingsSource.DEFAULT_PASSWORD.toCharArray())));

        StringEntity body = new StringEntity("{\"test\":\"test\"}", ContentType.APPLICATION_JSON);
        Response response = getRestClient().performRequest("PUT", path, Collections.emptyMap(), body, basicAuthHeader);
        assertThat(response.getStatusLine().getStatusCode(), equalTo(201));

        response = getRestClient().performRequest("GET", path, basicAuthHeader);
        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        assertThat(EntityUtils.toString(response.getEntity()), containsString("\"test\":\"test\""));

        if (randomBoolean()) {
            flushAndRefresh();
        }

        //update with new field
        body = new StringEntity("{\"doc\": {\"not test\": \"not test\"}}", ContentType.APPLICATION_JSON);
        response = getRestClient().performRequest("POST", path + "/_update", Collections.emptyMap(), body, basicAuthHeader);
        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));

        response = getRestClient().performRequest("GET", path, basicAuthHeader);
        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        String responseBody = EntityUtils.toString(response.getEntity());
        assertThat(responseBody, containsString("\"test\":\"test\""));
        assertThat(responseBody, containsString("\"not test\":\"not test\""));

        // this part is important. Without this, the document may be read from the translog which would bypass the bug where
        // FLS kicks in because the request can't be found and only returns meta fields
        flushAndRefresh();

        body = new StringEntity("{\"update\": {\"_index\": \"index1\", \"_type\": \"type\", \"_id\": \"1\"}}\n" +
                "{\"doc\": {\"bulk updated\":\"bulk updated\"}}\n", ContentType.APPLICATION_JSON);
        response = getRestClient().performRequest("POST", "/_bulk", Collections.emptyMap(), body, basicAuthHeader);
        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));

        response = getRestClient().performRequest("GET", path, basicAuthHeader);
        responseBody = EntityUtils.toString(response.getEntity());
        assertThat(responseBody, containsString("\"test\":\"test\""));
        assertThat(responseBody, containsString("\"not test\":\"not test\""));
        assertThat(responseBody, containsString("\"bulk updated\":\"bulk updated\""));
    }
}
