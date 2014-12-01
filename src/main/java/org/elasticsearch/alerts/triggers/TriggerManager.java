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

import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.ElasticsearchIllegalStateException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.alerts.Alert;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.joda.FormatDateTimeFormatter;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.mapper.core.DateFieldMapper;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.SearchHit;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class TriggerManager extends AbstractComponent {

    private static final String FIRE_TIME_VARIABLE_NAME = "FIRE_TIME";
    private static final String SCHEDULED_FIRE_TIME_VARIABLE_NAME = "SCHEDULED_FIRE_TIME";
    public static final FormatDateTimeFormatter dateTimeFormatter = DateFieldMapper.Defaults.DATE_TIME_FORMATTER;

    private final Client client;
    private final ScriptService scriptService;
    private volatile ImmutableOpenMap<String, TriggerFactory> triggersImplemented;

    @Inject
    public TriggerManager(Settings settings, Client client, ScriptService scriptService) {
        super(settings);
        this.client = client;
        this.scriptService = scriptService;
        triggersImplemented = ImmutableOpenMap.<String, TriggerFactory>builder()
                .fPut("script", new ScriptedTriggerFactory(scriptService))
                .build();
    }

    /**
     * Registers an AlertTrigger so that it can be instantiated by name
     * @param name The name of the trigger
     * @param actionFactory The factory responsible for trigger instantiation
     */
    public void registerTrigger(String name, TriggerFactory actionFactory){
        triggersImplemented = ImmutableOpenMap.builder(triggersImplemented)
                .fPut(name, actionFactory)
                .build();
    }

    /**
     * Reads the contents of parser to create the correct Trigger
     * @param parser The parser containing the trigger definition
     * @return a new AlertTrigger instance from the parser
     * @throws IOException
     */
    public AlertTrigger instantiateAlertTrigger(XContentParser parser) throws IOException {
        ImmutableOpenMap<String, TriggerFactory> triggersImplemented = this.triggersImplemented;
        String triggerFactoryName = null;
        XContentParser.Token token;
        AlertTrigger trigger = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                triggerFactoryName = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT && triggerFactoryName != null) {
                TriggerFactory factory = triggersImplemented.get(triggerFactoryName);
                if (factory != null) {
                    trigger = factory.createTrigger(parser);
                } else {
                    throw new ElasticsearchIllegalArgumentException("No trigger exists with the name [" + triggerFactoryName + "]");
                }
            }
        }
        return trigger;
    }

    /**
     * Tests to see if an alert will trigger for a given fireTime and scheduleFire time
     *
     * @param alert The Alert to test
     * @param scheduledFireTime The time the alert was scheduled to run
     * @param fireTime The time the alert actually ran
     * @return The TriggerResult representing the trigger state of the alert
     * @throws IOException
     */
    public TriggerResult isTriggered(Alert alert, DateTime scheduledFireTime, DateTime fireTime) throws IOException {
        SearchRequest request = prepareTriggerSearch(alert, scheduledFireTime, fireTime);
        if (logger.isTraceEnabled()) {
            logger.trace("For alert [{}] running query for [{}]", alert.getAlertName(), XContentHelper.convertToJson(request.source(), false, true));
        }

        SearchResponse response = client.search(request).actionGet(); // actionGet deals properly with InterruptedException
        if (logger.isDebugEnabled()) {
            logger.debug("Ran alert [{}] and got hits : [{}]", alert.getAlertName(), response.getHits().getTotalHits());
            for (SearchHit hit : response.getHits()) {
                logger.debug("Hit: {}", XContentHelper.toString(hit));
            }

        }
        XContentBuilder builder = jsonBuilder().startObject().value(response).endObject();
        Map<String, Object> responseMap = XContentHelper.convertToMap(builder.bytes(), false).v2();
        return isTriggered(alert.getTrigger(), request, responseMap);
    }

    protected TriggerResult isTriggered(AlertTrigger trigger, SearchRequest request, Map<String, Object> response) {
        TriggerFactory factory = triggersImplemented.get(trigger.getTriggerName());
        if (factory == null) {
            throw new ElasticsearchIllegalArgumentException("No trigger exists with the name [" + trigger.getTriggerName() + "]");
        }

        boolean triggered = factory.isTriggered(trigger, request,response);
        return new TriggerResult(triggered, request, response, trigger);
    }

    private SearchRequest prepareTriggerSearch(Alert alert, DateTime scheduledFireTime, DateTime fireTime) throws IOException {
        SearchRequest triggerSearchRequest = new SearchRequest(alert.getSearchRequest())
                .indicesOptions(alert.getSearchRequest().indicesOptions())
                .indices(alert.getSearchRequest().indices());
        if (Strings.hasLength(alert.getSearchRequest().source())) {
            Map<String, String> templateParams = new HashMap<>();
            templateParams.put(SCHEDULED_FIRE_TIME_VARIABLE_NAME, dateTimeFormatter.printer().print(scheduledFireTime));
            templateParams.put(FIRE_TIME_VARIABLE_NAME, dateTimeFormatter.printer().print(fireTime));
            String requestSource = XContentHelper.convertToJson(alert.getSearchRequest().source(), false);
            ExecutableScript script = scriptService.executable("mustache", requestSource, ScriptService.ScriptType.INLINE, templateParams);
            triggerSearchRequest.source((BytesReference) script.unwrap(script.run()), false);
        } else if (alert.getSearchRequest().templateName() != null) {
            MapBuilder<String, String> templateParams = MapBuilder.newMapBuilder(alert.getSearchRequest().templateParams())
                    .put(SCHEDULED_FIRE_TIME_VARIABLE_NAME, dateTimeFormatter.printer().print(scheduledFireTime))
                    .put(FIRE_TIME_VARIABLE_NAME, dateTimeFormatter.printer().print(fireTime));
            triggerSearchRequest.templateParams(templateParams.map());
            triggerSearchRequest.templateName(alert.getSearchRequest().templateName());
            triggerSearchRequest.templateType(alert.getSearchRequest().templateType());
        } else {
            throw new ElasticsearchIllegalStateException("Search requests needs either source or template name");
        }
        return triggerSearchRequest;
    }

}
