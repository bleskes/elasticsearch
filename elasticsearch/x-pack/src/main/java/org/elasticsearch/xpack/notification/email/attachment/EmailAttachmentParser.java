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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.watch.Payload;
import org.elasticsearch.xpack.notification.email.Attachment;

import java.io.IOException;

/**
 * Marker interface for email attachments that have an additional execution step and are used by
 * EmailAttachmentParser class
 */
public interface EmailAttachmentParser<T extends EmailAttachmentParser.EmailAttachment> {

    interface EmailAttachment extends ToXContent {
        /**
         * @return A type to identify the email attachment, same as the parser identifier
         */
        String type();

        /**
         * @return The id of this attachment
         */
        String id();
    }

    /**
     * @return An identifier of this parser
     */
    String type();

    /**
     * A parser to create an EmailAttachment, that is serializable and does not execute anything
     *
     * @param id The id of this attachment, parsed from the outer content
     * @param parser The XContentParser used for parsing
     * @return A concrete EmailAttachment
     * @throws IOException in case parsing fails
     */
    T parse(String id, XContentParser parser) throws IOException;

    /**
     * Converts an email attachment to an attachment, potentially executing code like an HTTP request
     * @param context The WatchExecutionContext supplied with the whole watch execution
     * @param payload The Payload supplied with the action
     * @param attachment The typed attachment
     * @return An attachment that is ready to be used in a MimeMessage
     */
    Attachment toAttachment(WatchExecutionContext context, Payload payload, T attachment) throws ElasticsearchException;

}
