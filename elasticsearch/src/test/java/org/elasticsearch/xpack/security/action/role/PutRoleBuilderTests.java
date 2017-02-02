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

package org.elasticsearch.xpack.security.action.role;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.client.NoOpClient;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;

public class PutRoleBuilderTests extends ESTestCase {
    // test that we reject a role where field permissions are stored in 2.x format (fields:...)
    public void testBWCFieldPermissions() throws Exception {
        Path path = getDataPath("roles2xformat.json");
        byte[] bytes = Files.readAllBytes(path);
        String roleString = new String(bytes, Charset.defaultCharset());
        try (Client client = new NoOpClient("testBWCFieldPermissions")) {
            ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class,
                    () -> new PutRoleRequestBuilder(client).source("role1", new BytesArray(roleString), XContentType.JSON));
            assertThat(e.getDetailedMessage(), containsString("\"fields\": [...]] format has changed for field permissions in role " +
                    "[role1], use [\"field_security\": {\"grant\":[...],\"except\":[...]}] instead"));
        }
    }
}