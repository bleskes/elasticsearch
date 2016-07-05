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

package org.elasticsearch.xpack.notification.hipchat;

import org.elasticsearch.common.component.LifecycleComponent;

/**
 *
 */
public interface HipChatService extends LifecycleComponent {

    /**
     * @return The default hipchat account.
     */
    HipChatAccount getDefaultAccount();

    /**
     * @return  The account identified by the given name. If the given name is {@code null} the default
     *          account will be returned.
     */
    HipChatAccount getAccount(String accountName);

}
