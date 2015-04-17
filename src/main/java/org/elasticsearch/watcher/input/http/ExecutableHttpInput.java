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

package org.elasticsearch.watcher.input.http;

import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.input.ExecutableInput;
import org.elasticsearch.watcher.support.Variables;
import org.elasticsearch.watcher.support.XContentFilterKeysUtils;
import org.elasticsearch.watcher.support.http.HttpClient;
import org.elasticsearch.watcher.support.http.HttpRequest;
import org.elasticsearch.watcher.support.http.HttpResponse;
import org.elasticsearch.watcher.support.template.TemplateEngine;
import org.elasticsearch.watcher.watch.Payload;

import java.io.IOException;
import java.util.Map;

/**
 */
public class ExecutableHttpInput extends ExecutableInput<HttpInput, HttpInput.Result> {

    private final HttpClient client;
    private final TemplateEngine templateEngine;

    public ExecutableHttpInput(HttpInput input, ESLogger logger, HttpClient client, TemplateEngine templateEngine) {
        super(input, logger);
        this.client = client;
        this.templateEngine = templateEngine;
    }

    @Override
    public HttpInput.Result execute(WatchExecutionContext ctx) throws IOException {
        Map<String, Object> model = Variables.createCtxModel(ctx, null);
        HttpRequest request = input.getRequest().render(templateEngine, model);

        HttpResponse response = client.execute(request);
        Payload payload;
        if (input.getExtractKeys() != null) {
            XContentParser parser = XContentHelper.createParser(response.body());
            Map<String, Object> filteredKeys = XContentFilterKeysUtils.filterMapOrdered(input.getExtractKeys(), parser);
            payload = new Payload.Simple(filteredKeys);
        } else {
            Tuple<XContentType, Map<String, Object>> result = XContentHelper.convertToMap(response.body(), true);
            payload = new Payload.Simple(result.v2());
        }
        return new HttpInput.Result(payload, request, response.status());
    }

}
