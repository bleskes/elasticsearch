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

package org.elasticsearch.xpack.notification.email;

import org.elasticsearch.common.component.LifecycleComponent;

import javax.mail.MessagingException;

/**
 *
 */
public interface EmailService extends LifecycleComponent<EmailService>{

    EmailSent send(Email email, Authentication auth, Profile profile) throws MessagingException;

    EmailSent send(Email email, Authentication auth, Profile profile, String accountName) throws MessagingException;

    class EmailSent {

        private final String account;
        private final Email email;

        public EmailSent(String account, Email email) {
            this.account = account;
            this.email = email;
        }

        public String account() {
            return account;
        }

        public Email email() {
            return email;
        }
    }

}
