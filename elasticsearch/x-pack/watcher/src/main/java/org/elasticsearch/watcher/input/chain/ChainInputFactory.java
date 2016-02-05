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

package org.elasticsearch.watcher.input.chain;

import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.input.ExecutableInput;
import org.elasticsearch.watcher.input.Input;
import org.elasticsearch.watcher.input.InputFactory;
import org.elasticsearch.watcher.input.InputRegistry;
import org.elasticsearch.watcher.support.init.InitializingService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChainInputFactory extends InputFactory<ChainInput, ChainInput.Result, ExecutableChainInput>
        implements InitializingService.Initializable {

    private InputRegistry inputRegistry;

    @Inject
    public ChainInputFactory(Settings settings) {
        super(Loggers.getLogger(ExecutableChainInput.class, settings));
    }

    @Override
    public String type() {
        return ChainInput.TYPE;
    }

    @Override
    public ChainInput parseInput(String watchId, XContentParser parser) throws IOException {
        return ChainInput.parse(watchId, parser, inputRegistry);
    }

    @Override
    public ExecutableChainInput createExecutable(ChainInput input) {
        List<Tuple<String, ExecutableInput>> executableInputs = new ArrayList<>();
        for (Tuple<String, Input> tuple : input.getInputs()) {
            ExecutableInput executableInput = inputRegistry.factories().get(tuple.v2().type()).createExecutable(tuple.v2());
            executableInputs.add(new Tuple<>(tuple.v1(), executableInput));
        }

        return new ExecutableChainInput(input, executableInputs, inputLogger);
    }

    @Override
    public void init(Injector injector) {
        init(injector.getInstance(InputRegistry.class));
    }

    void init(InputRegistry inputRegistry) {
        this.inputRegistry = inputRegistry;
    }
}
