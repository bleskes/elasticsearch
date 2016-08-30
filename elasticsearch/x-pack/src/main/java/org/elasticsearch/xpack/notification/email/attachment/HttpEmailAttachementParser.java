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

package org.elasticsearch.xpack.notification.email.attachment;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.Supplier;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.common.http.HttpClient;
import org.elasticsearch.xpack.common.http.HttpRequest;
import org.elasticsearch.xpack.common.http.HttpRequestTemplate;
import org.elasticsearch.xpack.common.http.HttpResponse;
import org.elasticsearch.xpack.common.text.TextTemplateEngine;
import org.elasticsearch.xpack.notification.email.Attachment;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.support.Variables;
import org.elasticsearch.xpack.watcher.watch.Payload;

import java.io.IOException;
import java.util.Map;

public class HttpEmailAttachementParser implements EmailAttachmentParser<HttpRequestAttachment> {

    public interface Fields {
        ParseField INLINE = new ParseField("inline");
        ParseField REQUEST = new ParseField("request");
        ParseField CONTENT_TYPE = new ParseField("content_type");
    }

    public static final String TYPE = "http";
    private final HttpClient httpClient;
    private HttpRequestTemplate.Parser requestTemplateParser;
    private final TextTemplateEngine templateEngine;
    private final Logger logger;

    public HttpEmailAttachementParser(HttpClient httpClient, HttpRequestTemplate.Parser requestTemplateParser,
                                      TextTemplateEngine templateEngine) {
        this.httpClient = httpClient;
        this.requestTemplateParser = requestTemplateParser;
        this.templateEngine = templateEngine;
        this.logger = Loggers.getLogger(getClass());
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public HttpRequestAttachment parse(String id, XContentParser parser) throws IOException {
        boolean inline = false;
        String contentType = null;
        HttpRequestTemplate requestTemplate = null;

        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (ParseFieldMatcher.STRICT.match(currentFieldName, Fields.CONTENT_TYPE)) {
                contentType = parser.text();
            } else if (ParseFieldMatcher.STRICT.match(currentFieldName, Fields.INLINE)) {
                inline = parser.booleanValue();
            } else if (ParseFieldMatcher.STRICT.match(currentFieldName, Fields.REQUEST)) {
                requestTemplate = requestTemplateParser.parse(parser);
            } else {
                String msg = "Unknown field name [" + currentFieldName + "] in http request attachment configuration";
                throw new ElasticsearchParseException(msg);
            }
        }

        if (requestTemplate != null) {
            return new HttpRequestAttachment(id, requestTemplate, inline, contentType);
        }

        throw new ElasticsearchParseException("Could not parse http request attachment");
    }

    @Override
    public Attachment toAttachment(WatchExecutionContext context, Payload payload,
                                   HttpRequestAttachment attachment) throws ElasticsearchException {
        Map<String, Object> model = Variables.createCtxModel(context, payload);
        HttpRequest httpRequest = attachment.getRequestTemplate().render(templateEngine, model);

        try {
            HttpResponse response = httpClient.execute(httpRequest);
            // check for status 200, only then append attachment
            if (response.status() >= 200 && response.status() < 300) {
                if (response.hasContent()) {
                    String contentType = attachment.getContentType();
                    String attachmentContentType = Strings.hasLength(contentType) ? contentType : response.contentType();
                    return new Attachment.Bytes(attachment.id(), BytesReference.toBytes(response.body()), attachmentContentType,
                            attachment.inline());
                } else {
                    logger.error("Empty response body: [host[{}], port[{}], method[{}], path[{}]: response status [{}]", httpRequest.host(),
                            httpRequest.port(), httpRequest.method(), httpRequest.path(), response.status());
                }
            } else {
                logger.error("Error getting http response: [host[{}], port[{}], method[{}], path[{}]: response status [{}]",
                        httpRequest.host(), httpRequest.port(), httpRequest.method(), httpRequest.path(), response.status());
            }
        } catch (IOException e) {
            logger.error(
                    (Supplier<?>) () -> new ParameterizedMessage(
                            "Error executing HTTP request: [host[{}], port[{}], method[{}], path[{}]: [{}]",
                            httpRequest.host(),
                            httpRequest.port(),
                            httpRequest.method(),
                            httpRequest.path(),
                            e.getMessage()),
                    e);
        }

        throw new ElasticsearchException("Unable to get attachment of type [{}] with id [{}] in watch [{}] aborting watch execution",
                type(), attachment.id(), context.watch().id());
    }
}
