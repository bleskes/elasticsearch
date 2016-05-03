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

package org.elasticsearch.xpack.watcher.support.xcontent;

import org.elasticsearch.common.Strings;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class ObjectPath {

    private ObjectPath() {
    }

    public static <T> T eval(String path, Object object) {
        return (T) evalContext(path, object);
    }

    private static Object evalContext(String path, Object ctx) {
        String[] parts = Strings.splitStringToArray(path, '.');
        StringBuilder resolved = new StringBuilder();
        for (String part : parts) {
            if (ctx == null) {
                return null;
            }
            if (ctx instanceof Map) {
                ctx = ((Map) ctx).get(part);
                if (resolved.length() != 0) {
                    resolved.append(".");
                }
                resolved.append(part);
            } else if (ctx instanceof List) {
                try {
                    int index = Integer.parseInt(part);
                    ctx = ((List) ctx).get(index);
                    if (resolved.length() != 0) {
                        resolved.append(".");
                    }
                    resolved.append(part);
                } catch (NumberFormatException nfe) {
                    return null;
                }
            } else if (ctx.getClass().isArray()) {
                try {
                    int index = Integer.parseInt(part);
                    ctx = Array.get(ctx, index);
                    if (resolved.length() != 0) {
                        resolved.append(".");
                    }
                    resolved.append(part);
                } catch (NumberFormatException nfe) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return ctx;
    }
}
