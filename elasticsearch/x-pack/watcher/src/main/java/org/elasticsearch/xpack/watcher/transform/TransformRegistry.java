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

package org.elasticsearch.xpack.watcher.transform;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.watcher.transform.chain.ChainTransform;
import org.elasticsearch.xpack.watcher.transform.chain.ChainTransformFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class TransformRegistry {

    private final Map<String, TransformFactory> factories;

    @Inject
    public TransformRegistry(Settings settings, Map<String, TransformFactory> factories) {
        Map<String, TransformFactory> map = new HashMap<>(factories);
        map.put(ChainTransform.TYPE, new ChainTransformFactory(settings, this));
        this.factories = Collections.unmodifiableMap(map);
    }

    public TransformFactory factory(String type) {
        return factories.get(type);
    }

    public ExecutableTransform parse(String watchId, XContentParser parser) throws IOException {
        String type = null;
        XContentParser.Token token;
        ExecutableTransform transform = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                type = parser.currentName();
            } else if (type != null) {
                transform = parse(watchId, type, parser);
            }
        }
        return transform;
    }

    public ExecutableTransform parse(String watchId, String type, XContentParser parser) throws IOException {
        TransformFactory factory = factories.get(type);
        if (factory == null) {
            throw new ElasticsearchParseException("could not parse transform for watch [{}], unknown transform type [{}]", watchId, type);
        }
        return factory.parseExecutable(watchId, parser);
    }

    public Transform parseTransform(String watchId, String type, XContentParser parser) throws IOException {
        TransformFactory factory = factories.get(type);
        if (factory == null) {
            throw new ElasticsearchParseException("could not parse transform for watch [{}], unknown transform type [{}]", watchId, type);
        }
        return factory.parseTransform(watchId, parser);
    }
}
