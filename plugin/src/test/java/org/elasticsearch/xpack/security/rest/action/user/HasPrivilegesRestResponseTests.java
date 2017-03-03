package org.elasticsearch.xpack.security.rest.action.user;

/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2017] Elasticsearch Incorporated. All Rights Reserved.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;

import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.security.action.user.HasPrivilegesResponse;
import org.elasticsearch.xpack.security.rest.action.user.RestHasPrivilegesAction.HasPrivilegesRestResponseBuilder;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.mock;

public class HasPrivilegesRestResponseTests extends ESTestCase {

    public void testBuildValidJsonResponse() throws Exception {
        final HasPrivilegesRestResponseBuilder response = new HasPrivilegesRestResponseBuilder("daredevil", mock(RestChannel.class));
        final HasPrivilegesResponse actionResponse = new HasPrivilegesResponse(false,
                Collections.singletonMap("manage", true),
                Arrays.asList(
                        new HasPrivilegesResponse.IndexPrivileges("staff",
                                MapBuilder.<String, Boolean>newMapBuilder(new LinkedHashMap<>())
                                        .put("read", true).put("index", true).put("delete", false).put("manage", false).map()),
                        new HasPrivilegesResponse.IndexPrivileges("customers",
                                MapBuilder.<String, Boolean>newMapBuilder(new LinkedHashMap<>())
                                        .put("read", true).put("index", true).put("delete", true).put("manage", false).map())
                ));
        final XContentBuilder builder = XContentBuilder.builder(XContentType.JSON.xContent());
        final RestResponse rest = response.buildResponse(actionResponse, builder);

        assertThat(rest, instanceOf(BytesRestResponse.class));

        final String json = rest.content().utf8ToString();
        assertThat(json, equalTo("{" +
                "\"username\":\"daredevil\"," +
                "\"has_all_requested\":false," +
                "\"cluster\":{\"manage\":true}," +
                "\"index\":{" +
                "\"staff\":{\"read\":true,\"index\":true,\"delete\":false,\"manage\":false}," +
                "\"customers\":{\"read\":true,\"index\":true,\"delete\":true,\"manage\":false}" +
                "}}"));
    }
}