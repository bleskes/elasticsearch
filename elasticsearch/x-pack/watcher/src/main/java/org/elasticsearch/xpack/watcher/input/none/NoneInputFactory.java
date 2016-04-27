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

package org.elasticsearch.xpack.watcher.input.none;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.watcher.input.InputFactory;

import java.io.IOException;

/**
 *
 */
public class NoneInputFactory extends InputFactory<NoneInput, NoneInput.Result, ExecutableNoneInput> {

    @Inject
    public NoneInputFactory(Settings settings) {
        super(Loggers.getLogger(ExecutableNoneInput.class, settings));
    }

    @Override
    public String type() {
        return NoneInput.TYPE;
    }

    @Override
    public NoneInput parseInput(String watchId, XContentParser parser) throws IOException {
        return NoneInput.parse(watchId, parser);
    }

    @Override
    public ExecutableNoneInput createExecutable(NoneInput input) {
        return new ExecutableNoneInput(inputLogger);
    }
}
