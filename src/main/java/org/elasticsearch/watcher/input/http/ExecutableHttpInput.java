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


import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.xcontent.XContentFactory;
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

        if (!response.hasContent()) {
            return new HttpInput.Result(Payload.EMPTY, request, response.status());
        }

        XContentType contentType = response.xContentType();
        if (input.getExpectedResponseXContentType() != null) {
            if (contentType != input.getExpectedResponseXContentType().contentType()) {
                logger.warn("[{}] [{}] input expected content type [{}] but read [{}] from headers", type(), ctx.id(), input.getExpectedResponseXContentType(), contentType);
            }

            if (contentType == null) {
                contentType = input.getExpectedResponseXContentType().contentType();
            }
        } else {
            //Attempt to auto detect content type
            if (contentType == null) {
                contentType = XContentFactory.xContentType(response.body());
            }
        }

        XContentParser parser = null;
        if (contentType != null) {
            try {
                parser = contentType.xContent().createParser(response.body());
            } catch (Exception e) {
                throw new HttpInputException("[{}] [{}] input could not parse response body [{}] it does not appear to be [{}]", type(), ctx.id(), response.body().toUtf8(), contentType.shortName());
            }
        }

        final Payload payload;
        if (input.getExtractKeys() != null) {
            Map<String, Object> filteredKeys = XContentFilterKeysUtils.filterMapOrdered(input.getExtractKeys(), parser);
            payload = new Payload.Simple(filteredKeys);
        } else {
            if (parser != null) {
                Map<String, Object> map = parser.mapOrderedAndClose();
                payload = new Payload.Simple(map);
            } else {
                payload = new Payload.Simple("_value", response.body().toUtf8());
            }
        }
        return new HttpInput.Result(payload, request, response.status());
    }
}
