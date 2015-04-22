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

package org.elasticsearch.watcher.transform.chain;

import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.support.init.InitializingService;
import org.elasticsearch.watcher.transform.ExecutableTransform;
import org.elasticsearch.watcher.transform.Transform;
import org.elasticsearch.watcher.transform.TransformFactory;
import org.elasticsearch.watcher.transform.TransformRegistry;

import java.io.IOException;

/**
 *
 */
public class ChainTransformFactory extends TransformFactory<ChainTransform, ChainTransform.Result, ExecutableChainTransform> implements InitializingService.Initializable {

    private TransformRegistry registry;

    // used by guice
    public ChainTransformFactory(Settings settings) {
        super(Loggers.getLogger(ExecutableChainTransform.class, settings));
    }

    // used for tests
    public ChainTransformFactory(TransformRegistry registry) {
        super(Loggers.getLogger(ExecutableChainTransform.class));
        this.registry = registry;
    }

    // used for tests
    public ChainTransformFactory() {
        super(Loggers.getLogger(ExecutableChainTransform.class));
    }

    @Override
    public void init(Injector injector) {
        init(injector.getInstance(TransformRegistry.class));
    }

    public void init(TransformRegistry registry) {
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
    public ChainTransform.Result parseResult(String watchId, XContentParser parser) throws IOException {
        return ChainTransform.Result.parse(watchId, parser, registry);
    }

    @Override
    public ExecutableChainTransform createExecutable(ChainTransform chainTransform) {
        ImmutableList.Builder<ExecutableTransform> executables = ImmutableList.builder();
        for (Transform transform : chainTransform.getTransforms()) {
            TransformFactory factory = registry.factory(transform.type());
            executables.add(factory.createExecutable(transform));
        }
        return new ExecutableChainTransform(chainTransform, transformLogger, executables.build());
    }
}
