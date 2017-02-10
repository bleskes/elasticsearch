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

package org.elasticsearch.xpack.security.audit;

import org.elasticsearch.action.IndicesRequest;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.transport.TransportMessage;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class AuditUtil {

    public static String restRequestContent(RestRequest request) {
        if (request.hasContent()) {
            try {
                return XContentHelper.convertToJson(request.content(), false, false, request.getXContentType());
            } catch (IOException ioe) {
                return "Invalid Format: " + request.content().utf8ToString();
            }
        }
        return "";
    }

    public static Set<String> indices(TransportMessage message) {
        if (message instanceof IndicesRequest) {
            return arrayToSetOrNull(((IndicesRequest) message).indices());
        }
        return null;
    }

    private static Set<String> arrayToSetOrNull(String[] indices) {
        return indices == null ? null : new HashSet<>(Arrays.asList(indices));
    }
}
