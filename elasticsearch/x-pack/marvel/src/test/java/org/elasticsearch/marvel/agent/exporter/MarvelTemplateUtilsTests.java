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

package org.elasticsearch.marvel.agent.exporter;

import org.elasticsearch.test.ESTestCase;

import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

public class MarvelTemplateUtilsTests extends ESTestCase {

    public void testLoadTimestampedIndexTemplate() {
        byte[] template = MarvelTemplateUtils.loadTimestampedIndexTemplate();
        assertNotNull(template);
        assertThat(template.length, greaterThan(0));
    }

    public void testLoadDataIndexTemplate() {
        byte[] template = MarvelTemplateUtils.loadDataIndexTemplate();
        assertNotNull(template);
        assertThat(template.length, greaterThan(0));
    }

    public void testLoad() throws IOException {
        String resource = randomFrom(MarvelTemplateUtils.INDEX_TEMPLATE_FILE, MarvelTemplateUtils.DATA_TEMPLATE_FILE);
        byte[] template = MarvelTemplateUtils.load(resource);
        assertNotNull(template);
        assertThat(template.length, greaterThan(0));
    }

    public void testLoadTemplateVersion() {
        Integer version = MarvelTemplateUtils.loadTemplateVersion();
        assertNotNull(version);
        assertThat(version, greaterThan(0));
        assertThat(version, equalTo(MarvelTemplateUtils.TEMPLATE_VERSION));
    }

    public void testIndexTemplateName() {
        assertThat(MarvelTemplateUtils.indexTemplateName(),
                equalTo(MarvelTemplateUtils.INDEX_TEMPLATE_NAME_PREFIX + MarvelTemplateUtils.TEMPLATE_VERSION));
        int version = randomIntBetween(1, 100);
        assertThat(MarvelTemplateUtils.indexTemplateName(version), equalTo(".monitoring-es-" + version));
    }

    public void testDataTemplateName() {
        assertThat(MarvelTemplateUtils.dataTemplateName(),
                equalTo(MarvelTemplateUtils.DATA_TEMPLATE_NAME_PREFIX + MarvelTemplateUtils.TEMPLATE_VERSION));
        int version = randomIntBetween(1, 100);
        assertThat(MarvelTemplateUtils.dataTemplateName(version), equalTo(".monitoring-es-data-" + version));
    }
}
