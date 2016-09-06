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

package org.elasticsearch.xpack.watcher.test;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.common.text.TextTemplate;
import org.elasticsearch.xpack.common.text.TextTemplateEngine;

import java.util.Map;

public class MockTextTemplateEngine extends TextTemplateEngine {
    public MockTextTemplateEngine() {
        super(Settings.EMPTY, null);
    }

    @Override
    public String render(TextTemplate textTemplate, Map<String, Object> model) {
        if (textTemplate == null ) {
            return null;
        }

        return textTemplate.getTemplate();
    }
}
