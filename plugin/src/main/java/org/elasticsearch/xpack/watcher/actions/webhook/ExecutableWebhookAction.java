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


package org.elasticsearch.xpack.watcher.actions.webhook;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.xpack.common.http.HttpClient;
import org.elasticsearch.xpack.common.http.HttpRequest;
import org.elasticsearch.xpack.common.http.HttpResponse;
import org.elasticsearch.xpack.common.text.TextTemplateEngine;
import org.elasticsearch.xpack.watcher.actions.Action;
import org.elasticsearch.xpack.watcher.actions.ExecutableAction;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.support.Variables;
import org.elasticsearch.xpack.watcher.watch.Payload;

import java.util.Map;

import static org.elasticsearch.xpack.watcher.input.http.ExecutableHttpInput.checkUrlDepreciation;

/**
 */
public class ExecutableWebhookAction extends ExecutableAction<WebhookAction> {

    private final HttpClient httpClient;
    private final TextTemplateEngine templateEngine;

    public ExecutableWebhookAction(WebhookAction action, Logger logger, HttpClient httpClient, TextTemplateEngine templateEngine) {
        super(action, logger);
        this.httpClient = httpClient;
        this.templateEngine = templateEngine;
    }

    @Override
    public Action.Result execute(String actionId, WatchExecutionContext ctx, Payload payload) throws Exception {
        Map<String, Object> model = Variables.createCtxModel(ctx, payload);
        HttpRequest request = action.requestTemplate.render(templateEngine, model);
        checkUrlDepreciation(ctx.watch(), "webhook action", request, logger);

        if (ctx.simulateAction(actionId)) {
            return new WebhookAction.Result.Simulated(request);
        }

        HttpResponse response = httpClient.execute(request);

        int status = response.status();
        if (status >= 400) {
            logger.warn("received http status [{}] when connecting to watch action [{}/{}/{}]", status, ctx.watch().id(), type(), actionId);
            return new WebhookAction.Result.Failure(request, response);
        }
        return new WebhookAction.Result.Success(request, response);
    }
}
