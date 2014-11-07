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


import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.script.ScriptService;

import java.io.IOException;

public class ScriptedTrigger implements AlertTrigger{

    private final String script;
    private final ScriptService.ScriptType scriptType;
    private final String scriptLang;

    public ScriptedTrigger(String script, ScriptService.ScriptType scriptType, String scriptLang) {
        this.script = script;
        this.scriptType = scriptType;
        this.scriptLang = scriptLang;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("script", script);
        builder.field("script_type", scriptType);
        builder.field("script_lang", scriptLang);
        builder.endObject();
        return builder;
    }


    @Override
    public String getTriggerName() {
        return "script";
    }

    /**
     * The script to run
     * @return the script as a String
     */
    public String getScript() {
        return script;
    }

    /**
     * The type (INDEXED,INLINE,FILE) of the script
     * @return the type
     */
    public ScriptService.ScriptType getScriptType() {
        return scriptType;
    }

    /**
     * The language of the script (null for default language) as a String
     * @return the langauge
     */
    public String getScriptLang() {
        return scriptLang;
    }

    @Override
    public String toString() {
        return "ScriptedTrigger{" +
                "script='" + script + '\'' +
                ", scriptType=" + scriptType +
                ", scriptLang='" + scriptLang + '\'' +
                '}';
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScriptedTrigger that = (ScriptedTrigger) o;

        if (!script.equals(that.script)) return false;
        if (scriptLang != null ? !scriptLang.equals(that.scriptLang) : that.scriptLang != null) return false;
        if (scriptType != that.scriptType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = script.hashCode();
        result = 31 * result + (scriptType != null ? scriptType.hashCode() : 0);
        result = 31 * result + (scriptLang != null ? scriptLang.hashCode() : 0);
        return result;
    }




}
