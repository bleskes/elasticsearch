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

package org.elasticsearch.xpack.watcher.input.http;


import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.logging.DeprecationLogger;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.xpack.common.http.HttpClient;
import org.elasticsearch.xpack.common.http.HttpRequest;
import org.elasticsearch.xpack.common.http.HttpResponse;
import org.elasticsearch.xpack.common.text.TextTemplateEngine;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.input.ExecutableInput;
import org.elasticsearch.xpack.watcher.support.Variables;
import org.elasticsearch.xpack.watcher.support.XContentFilterKeysUtils;
import org.elasticsearch.xpack.watcher.watch.Payload;
import org.elasticsearch.xpack.watcher.watch.Watch;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.xpack.watcher.input.http.HttpInput.TYPE;

public class ExecutableHttpInput extends ExecutableInput<HttpInput, HttpInput.Result> {

    private final HttpClient client;
    private final TextTemplateEngine templateEngine;

    public ExecutableHttpInput(HttpInput input, Logger logger, HttpClient client, TextTemplateEngine templateEngine) {
        super(input, logger);
        this.client = client;
        this.templateEngine = templateEngine;
    }

    public HttpInput.Result execute(WatchExecutionContext ctx, Payload payload) {
        HttpRequest request = null;
        try {
            Map<String, Object> model = Variables.createCtxModel(ctx, payload);
            request = input.getRequest().render(templateEngine, model);
            checkUrlDepreciation(ctx.watch(), "http input", request, logger);
            return doExecute(ctx, request);
        } catch (Exception e) {
            logger.error("failed to execute [{}] input for watch [{}], reason [{}]", TYPE, ctx.watch().id(), e.getMessage());
            return new HttpInput.Result(request, e);
        }
    }

    HttpInput.Result doExecute(WatchExecutionContext ctx, HttpRequest request) throws Exception {
        HttpResponse response = client.execute(request);
        Map<String, List<String>> headers = response.headers();
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("_status_code", response.status());
        if (headers.isEmpty() == false) {
            payloadMap.put("_headers", headers);
        }

        if (!response.hasContent()) {
            return new HttpInput.Result(request, response.status(), new Payload.Simple(payloadMap));
        }

        final XContentType contentType;
        XContentType responseContentType = response.xContentType();
        if (input.getExpectedResponseXContentType() == null) {
            //Attempt to auto detect content type, if not set in response
            contentType = responseContentType != null ? responseContentType : XContentFactory.xContentType(response.body());
        } else {
            contentType = input.getExpectedResponseXContentType().contentType();
            if (responseContentType != contentType) {
                logger.warn("[{}] [{}] input expected content type [{}] but read [{}] from headers, using expected one", type(), ctx.id(),
                        input.getExpectedResponseXContentType(), responseContentType);
            }
        }

        if (contentType != null) {
            // EMPTY is safe here because we never use namedObject
            try (XContentParser parser = contentType.xContent().createParser(NamedXContentRegistry.EMPTY, response.body())) {
                if (input.getExtractKeys() != null) {
                    payloadMap.putAll(XContentFilterKeysUtils.filterMapOrdered(input.getExtractKeys(), parser));
                } else {
                    // special handling if a list is returned, i.e. JSON like [ {},{} ]
                    XContentParser.Token token = parser.nextToken();
                    if (token == XContentParser.Token.START_ARRAY) {
                        payloadMap.put("data", parser.listOrderedMap());
                    } else {
                        payloadMap.putAll(parser.mapOrdered());
                    }
                }
            } catch (Exception e) {
                throw new ElasticsearchParseException("could not parse response body [{}] it does not appear to be [{}]", type(), ctx.id(),
                        response.body().utf8ToString(), contentType.shortName());
            }
        } else {
            payloadMap.put("_value", response.body().utf8ToString());
        }

        return new HttpInput.Result(request, response.status(), new Payload.Simple(payloadMap));
    }

    /**
     * A special method to check if a watch uses unencoded parts of an URL - because of https://github.com/elastic/x-pack/pull/4434
     * this will break with 6.0 and we use the deprecation logger to warn people about this.
     *
     */
    public static void checkUrlDepreciation(Watch watch, String desc, HttpRequest request, Logger logger) {
        if (Strings.isNullOrEmpty(request.path()) == false) {
            try {
                new URL(request.scheme().scheme(), request.host(), request.port(), request.path()).toURI();
            } catch (MalformedURLException | URISyntaxException e) {
                DeprecationLogger deprecationLogger = new DeprecationLogger(logger);
                deprecationLogger.deprecated("watch [{}] url [{}://{}:{}/{}] in [{}] needs to be a valid URI (properly encoded) in 6.0",
                        watch.id(), request.scheme().scheme(), request.host(), request.port(), request.path(), desc);
            }
        }
    }
}
