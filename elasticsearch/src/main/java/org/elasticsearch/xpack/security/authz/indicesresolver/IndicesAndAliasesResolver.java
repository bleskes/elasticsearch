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

package org.elasticsearch.xpack.security.authz.indicesresolver;

import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.xpack.security.user.User;
import org.elasticsearch.transport.TransportRequest;

import java.util.Set;

/**
 *
 */
public interface IndicesAndAliasesResolver<Request extends TransportRequest> {

    Class<Request> requestType();

    Set<String> resolve(User user, String action, Request request, MetaData metaData);

}
