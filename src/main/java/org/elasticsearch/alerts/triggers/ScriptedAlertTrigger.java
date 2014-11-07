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

package org.elasticsearch.alerts.triggers;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.script.ScriptService;

import java.io.IOException;

public class ScriptedAlertTrigger implements ToXContent {
    public String script;
    public ScriptService.ScriptType scriptType;
    public String scriptLang;


    public ScriptedAlertTrigger(String script, ScriptService.ScriptType scriptType, String scriptLang) {
        this.script = script;
        this.scriptType = scriptType;
        this.scriptLang = scriptLang;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("script",script);
        builder.field("script_type", scriptType);
        builder.field("script_lang", scriptLang);
        builder.endObject();
        return builder;
    }
}
