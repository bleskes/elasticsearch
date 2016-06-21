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

package org.elasticsearch.xpack.watcher.transform.chain;

import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.watcher.transform.ExecutableTransform;
import org.elasticsearch.xpack.watcher.transform.Transform;
import org.elasticsearch.xpack.watcher.transform.TransformFactory;
import org.elasticsearch.xpack.watcher.transform.TransformRegistry;

import java.io.IOException;
import java.util.ArrayList;

public final class ChainTransformFactory extends TransformFactory<ChainTransform, ChainTransform.Result, ExecutableChainTransform> {

    private final TransformRegistry registry;

    public ChainTransformFactory(Settings settings, TransformRegistry registry) {
        super(Loggers.getLogger(ExecutableChainTransform.class, settings));
        this.registry = registry;
    }

    @Override
    public String type() {
        return ChainTransform.TYPE;
    }

    @Override
    public ChainTransform parseTransform(String watchId, XContentParser parser) throws IOException {
        return ChainTransform.parse(watchId, parser, registry);
    }

    @Override
    public ExecutableChainTransform createExecutable(ChainTransform chainTransform) {
        ArrayList<ExecutableTransform> executables = new ArrayList<>();
        for (Transform transform : chainTransform.getTransforms()) {
            TransformFactory factory = registry.factory(transform.type());
            executables.add(factory.createExecutable(transform));
        }
        return new ExecutableChainTransform(chainTransform, transformLogger, executables);
    }
}
