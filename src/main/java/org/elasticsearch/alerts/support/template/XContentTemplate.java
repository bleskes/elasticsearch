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

package org.elasticsearch.alerts.support.template;

import org.elasticsearch.common.xcontent.XContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.common.xcontent.yaml.YamlXContent;

import java.io.IOException;
import java.util.Map;

/**
 *
 */
public class XContentTemplate implements Template {

    public static XContentTemplate YAML = new XContentTemplate(YamlXContent.yamlXContent);
    public static XContentTemplate JSON = new XContentTemplate(JsonXContent.jsonXContent);

    private final XContent xContent;

    private XContentTemplate(XContent xContent) {
        this.xContent = xContent;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject().endObject();
    }

    @Override
    public String render(Map<String, Object> model) {
        try {
            return XContentBuilder.builder(xContent).map(model).bytes().toUtf8();
        } catch (IOException ioe) {
            throw new TemplateException("could not render [" + xContent.type().name() + "] xcontent template", ioe);
        }
    }

}
