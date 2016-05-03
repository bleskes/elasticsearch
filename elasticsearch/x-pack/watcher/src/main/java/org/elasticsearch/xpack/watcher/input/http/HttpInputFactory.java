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

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.watcher.input.InputFactory;
import org.elasticsearch.xpack.watcher.support.http.HttpClient;
import org.elasticsearch.xpack.watcher.support.http.HttpRequestTemplate;
import org.elasticsearch.xpack.watcher.support.text.TextTemplateEngine;

import java.io.IOException;

/**
 *
 */
public final class HttpInputFactory extends InputFactory<HttpInput, HttpInput.Result, ExecutableHttpInput> {

    private final HttpClient httpClient;
    private final TextTemplateEngine templateEngine;
    private final HttpRequestTemplate.Parser requestTemplateParser;

    @Inject
    public HttpInputFactory(Settings settings, HttpClient httpClient, TextTemplateEngine templateEngine,
                            HttpRequestTemplate.Parser requestTemplateParser) {
        super(Loggers.getLogger(ExecutableHttpInput.class, settings));
        this.templateEngine = templateEngine;
        this.httpClient = httpClient;
        this.requestTemplateParser = requestTemplateParser;
    }

    @Override
    public String type() {
        return HttpInput.TYPE;
    }

    @Override
    public HttpInput parseInput(String watchId, XContentParser parser) throws IOException {
        return HttpInput.parse(watchId, parser, requestTemplateParser);
    }

    @Override
    public ExecutableHttpInput createExecutable(HttpInput input) {
        return new ExecutableHttpInput(input, inputLogger, httpClient, templateEngine);
    }
}
