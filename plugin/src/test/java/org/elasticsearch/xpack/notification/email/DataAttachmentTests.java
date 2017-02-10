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

package org.elasticsearch.xpack.notification.email;

import org.elasticsearch.common.io.Streams;
import org.elasticsearch.test.ESTestCase;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.is;

public class DataAttachmentTests extends ESTestCase {

    public void testCreateJson() throws Exception {
        Map<String, Object> data = singletonMap("key", "value");
        Attachment attachment = DataAttachment.JSON.create("data", data);
        InputStream input = attachment.bodyPart().getDataHandler().getInputStream();
        String content = Streams.copyToString(new InputStreamReader(input, StandardCharsets.UTF_8));
        assertThat(content, is("{\n  \"key\" : \"value\"\n}"));
    }

    public void testCreateYaml() throws Exception {
        Map<String, Object> data = singletonMap("key", "value");
        Attachment attachment = DataAttachment.YAML.create("data", data);
        InputStream input = attachment.bodyPart().getDataHandler().getInputStream();
        String content = Streams.copyToString(new InputStreamReader(input, StandardCharsets.UTF_8));
        // the yaml factory in es always emits unix line breaks
        // this seems to be a bug in jackson yaml factory that doesn't default to the platform line break
        assertThat(content, is("---\nkey: \"value\"\n"));
    }
}
