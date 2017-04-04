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

package org.elasticsearch.xpack.watcher.input.chain;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.execution.Wid;
import org.elasticsearch.xpack.watcher.input.ExecutableInput;
import org.elasticsearch.xpack.watcher.input.Input;
import org.elasticsearch.xpack.watcher.input.simple.SimpleInput;
import org.elasticsearch.xpack.watcher.watch.Payload;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.IOException;
import java.util.Arrays;

import static org.elasticsearch.xpack.watcher.input.Input.Result.Status;
import static org.elasticsearch.xpack.watcher.test.WatcherTestUtils.mockExecutionContextBuilder;
import static org.hamcrest.Matchers.is;

public class ExecutableChainInputTests extends ESTestCase {

    public void testFailedResultHandling() throws Exception {
        WatchExecutionContext ctx = createWatchExecutionContext();
        ChainInput chainInput = new ChainInput(Arrays.asList(new Tuple<>("whatever", new SimpleInput(Payload.EMPTY))));

        Tuple<String, ExecutableInput> tuple = new Tuple<>("whatever", new FailingExecutableInput());
        ExecutableChainInput executableChainInput = new ExecutableChainInput(chainInput, Arrays.asList(tuple), logger);
        ChainInput.Result result = executableChainInput.execute(ctx, Payload.EMPTY);
        assertThat(result.status(), is(Status.SUCCESS));
    }

    private class FailingExecutableInput extends ExecutableInput<SimpleInput, Input.Result> {

        protected FailingExecutableInput() {
            super(new SimpleInput(Payload.EMPTY), ExecutableChainInputTests.this.logger);
        }

        @Override
        public Input.Result execute(WatchExecutionContext ctx, @Nullable Payload payload) {
            return new FailingExecutableInputResult(new RuntimeException("foo"));
        }
    }

    private static class FailingExecutableInputResult extends Input.Result {

        protected FailingExecutableInputResult(Exception e) {
            super("failing", e);
        }

        @Override
        protected XContentBuilder typeXContent(XContentBuilder builder, Params params) throws IOException {
            return builder;
        }
    }

    private WatchExecutionContext createWatchExecutionContext() {
        DateTime now = DateTime.now(DateTimeZone.UTC);
        Wid wid = new Wid(randomAlphaOfLength(5), now);
        return mockExecutionContextBuilder(wid.watchId())
                .wid(wid)
                .payload(new Payload.Simple())
                .time(wid.watchId(), now)
                .buildMock();
    }

}
