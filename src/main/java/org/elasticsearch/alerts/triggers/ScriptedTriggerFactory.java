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


import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.ElasticsearchIllegalStateException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.xcontent.*;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.ScriptService;

import java.io.IOException;
import java.util.Map;

public class ScriptedTriggerFactory implements TriggerFactory {
    private final ScriptService scriptService;

    public ScriptedTriggerFactory(ScriptService service) {
        scriptService = service;
    }

    @Override
    public AlertTrigger createTrigger(XContentParser parser) throws IOException {
        String currentFieldName = null;
        XContentParser.Token token;
        String scriptLang = null;
        String script = null;
        ScriptService.ScriptType scriptType = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token.isValue()) {
                switch (currentFieldName) {
                    case "script_id" :
                        script = parser.text();
                        //@TODO assert script type was null or INDEXED already
                        scriptType = ScriptService.ScriptType.INDEXED;
                        break;
                    case "script" :
                        script = parser.text();
                        scriptType = ScriptService.ScriptType.INLINE;
                        break;
                    case "script_type" :
                        ScriptService.ScriptType tmpType = ScriptService.ScriptType.valueOf(parser.text());
                        if (scriptType == ScriptService.ScriptType.INDEXED && tmpType != scriptType) {
                            throw new ElasticsearchException("Unexpected script type for script_id [" + tmpType + "]");
                        } else {
                            scriptType = tmpType;
                        }
                        break;
                    case "script_lang" :
                        scriptLang = parser.text();
                        break;
                    default:
                        throw new ElasticsearchIllegalArgumentException("Unexpected field [" + currentFieldName + "]");
                }
            }
        }
        if (script == null) {
            throw new ElasticsearchException("Failed to parse ScriptedTrigger script:[" + script
                    + "] scriptLang:[" + scriptLang + "] scriptType:[" + scriptType + "]");
        }
        return new ScriptedTrigger(script, scriptType, scriptLang);
    }

    @Override
    public boolean isTriggered(AlertTrigger trigger, SearchRequest request, Map<String, Object> response) {
        if (! (trigger instanceof ScriptedTrigger) ){
            throw new ElasticsearchIllegalStateException("Failed to evaluate isTriggered expected type ["
                    + ScriptedTrigger.class + "] got [" + trigger.getClass() + "]");
        }

        ScriptedTrigger scriptedTrigger = (ScriptedTrigger)trigger;
        ExecutableScript executable = scriptService.executable(
                scriptedTrigger.getScriptLang(), scriptedTrigger.getScript(), scriptedTrigger.getScriptType(), response
        );
        Object returnValue = executable.run();
        if (returnValue instanceof Boolean) {
            return (Boolean) returnValue;
        } else {
            throw new ElasticsearchIllegalStateException("Trigger script [" + scriptedTrigger + "] did not return a Boolean");
        }
    }

}
