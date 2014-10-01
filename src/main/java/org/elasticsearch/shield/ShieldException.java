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

package org.elasticsearch.shield;

import org.elasticsearch.ElasticsearchException;

import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.shield.plugin.ShieldPlugin;

import java.util.List;

/**
 *
 */
public class ShieldException extends ElasticsearchException.WithRestHeaders {

    public static final ImmutableMap<String, List<String>> HEADERS = ImmutableMap.<String, List<String>>builder()
            .put("WWW-Authenticate", Lists.newArrayList("Basic realm=\""+ ShieldPlugin.NAME +"\""))
            .build();

    public ShieldException(String msg) {
        super(msg, HEADERS);
    }

    public ShieldException(String msg, Throwable cause) {
        super(msg, cause, HEADERS);
    }
}
