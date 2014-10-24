package org.elasticsearch.alerts.triggers;

import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.ElasticsearchIllegalStateException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.alerts.Alert;
import org.elasticsearch.alerts.AlertManager;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.ScriptService;

import java.util.Locale;
import java.util.Map;


/*
 * TODO : The trigger classes need cleanup and refactoring to be similar to the AlertActions and be pluggable
 */
public class TriggerManager extends AbstractComponent {

    private final AlertManager alertManager;
    private final ScriptService scriptService;

    public static AlertTrigger parseTriggerFromMap(Map<String, Object> triggerMap) {
        for (Map.Entry<String,Object> entry : triggerMap.entrySet()){
            AlertTrigger.TriggerType type = AlertTrigger.TriggerType.fromString(entry.getKey());
            if (type == AlertTrigger.TriggerType.SCRIPT) {
                ScriptedAlertTrigger scriptedTrigger = parseScriptedTrigger(entry.getValue());
                return new AlertTrigger(scriptedTrigger);
            } else {
                AlertTrigger.SimpleTrigger simpleTrigger = AlertTrigger.SimpleTrigger.fromString(entry.getValue().toString().substring(0, 1));
                int value = Integer.valueOf(entry.getValue().toString().substring(1));
                return new AlertTrigger(simpleTrigger, type, value);
            }
        }
        throw new ElasticsearchIllegalArgumentException();
    }

    private static ScriptedAlertTrigger parseScriptedTrigger(Object value) {
        if (value instanceof Map) {
            Map<String,Object> valueMap = (Map<String,Object>)value;
            try {
                return new ScriptedAlertTrigger(valueMap.get("script").toString(),
                        ScriptService.ScriptType.valueOf(valueMap.get("script_type").toString().toUpperCase(Locale.ROOT)), ///TODO : Fix ScriptType to parse strings properly, currently only accepts uppercase versions of the enum names
                        valueMap.get("script_lang").toString());
            } catch (Exception e){
                throw new ElasticsearchIllegalArgumentException("Unable to parse " + value + " as a ScriptedAlertTrigger", e);
            }
        } else {
            throw new ElasticsearchIllegalArgumentException("Unable to parse " + value + " as a ScriptedAlertTrigger, not a Map");
        }
    }

    @Inject
    public TriggerManager(Settings settings, AlertManager alertManager, ScriptService scriptService) {
        super(settings);
        this.alertManager = alertManager;
        this.scriptService = scriptService;
    }

    public boolean doScriptTrigger(ScriptedAlertTrigger scriptTrigger, SearchResponse response) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            builder = response.toXContent(builder, ToXContent.EMPTY_PARAMS);
            builder.endObject();
            Map<String, Object> responseMap = XContentHelper.convertToMap(builder.bytes(), false).v2();

            ExecutableScript executable = scriptService.executable(scriptTrigger.scriptLang, scriptTrigger.script,
                    scriptTrigger.scriptType, responseMap);

            Object returnValue = executable.run();
            logger.warn("Returned [{}] from script", returnValue);
            if (returnValue instanceof Boolean) {
                return (Boolean) returnValue;
            } else {
                throw new ElasticsearchIllegalStateException("Trigger script [" + scriptTrigger.script + "] " +
                        "did not return a Boolean");
            }
        } catch (Exception e ){
            logger.error("Failed to execute script trigger", e);
        }
        return false;
    }

    public boolean isTriggered(String alertName, SearchResponse response) {
        Alert alert = this.alertManager.getAlertForName(alertName);
        if (alert == null){
            logger.warn("Could not find alert named [{}] in alert manager perhaps it has been deleted.", alertName);
            return false;
        }
        long testValue;
        switch (alert.trigger().triggerType()) {
            case NUMBER_OF_EVENTS:
                testValue = response.getHits().getTotalHits();
                break;
            case SCRIPT:
                return doScriptTrigger(alert.trigger().scriptedTrigger(), response);
            default:
                throw new ElasticsearchIllegalArgumentException("Bad value for trigger.triggerType [" + alert.trigger().triggerType() + "]");
        }
        int triggerValue = alert.trigger().value();
        //Move this to SimpleTrigger
        switch (alert.trigger().trigger()) {
            case GREATER_THAN:
                return testValue > triggerValue;
            case LESS_THAN:
                return testValue < triggerValue;
            case EQUAL:
                return testValue == triggerValue;
            case NOT_EQUAL:
                return testValue != triggerValue;
            case RISES_BY:
            case FALLS_BY:
                return false; //TODO FIX THESE
        }
        return false;
    }
}
