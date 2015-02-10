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

package org.elasticsearch.alerts.actions.email.service.support;

import org.elasticsearch.common.xcontent.ToXContent;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

/**
 *
 */
public abstract class BodyPartSource implements ToXContent {

    protected final String id;
    protected final String name;
    protected final String description;

    public BodyPartSource(String id) {
        this(id, id);
    }

    public BodyPartSource(String id, String name) {
        this(id, name, name);
    }

    public BodyPartSource(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public abstract MimeBodyPart bodyPart() throws MessagingException;

}
