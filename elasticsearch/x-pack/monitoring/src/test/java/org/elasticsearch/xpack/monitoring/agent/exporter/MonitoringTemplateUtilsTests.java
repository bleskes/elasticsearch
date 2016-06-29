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

package org.elasticsearch.xpack.monitoring.agent.exporter;

import org.elasticsearch.test.ESTestCase;

import java.io.IOException;

import static org.elasticsearch.xpack.template.TemplateUtilsTests.assertTemplate;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

public class MonitoringTemplateUtilsTests extends ESTestCase {

    public void testLoadTemplate() throws IOException {
        String source = MonitoringTemplateUtils.loadTemplate("test");

        assertThat(source, notNullValue());
        assertThat(source.length(), greaterThan(0));
        assertTemplate(source, equalTo("{\n" +
                "  \"template\": \".monitoring-data-" + MonitoringTemplateUtils.TEMPLATE_VERSION + "\",\n" +
                "  \"mappings\": {\n" +
                "    \"type_1\": {\n" +
                "      \"_meta\": {\n" +
                "        \"template.version\": \"" + MonitoringTemplateUtils.TEMPLATE_VERSION + "\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}"));
    }
}
