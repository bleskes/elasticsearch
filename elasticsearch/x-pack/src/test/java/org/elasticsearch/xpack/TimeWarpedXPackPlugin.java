/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.xpack;

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.support.clock.Clock;
import org.elasticsearch.xpack.support.clock.ClockMock;
import org.elasticsearch.xpack.support.clock.ClockModule;
import org.elasticsearch.xpack.watcher.test.TimeWarpedWatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TimeWarpedXPackPlugin extends XPackPlugin {

    public TimeWarpedXPackPlugin(Settings settings) {
        super(settings);
        watcher = new TimeWarpedWatcher(settings);
    }

    @Override
    public Collection<Module> nodeModules() {
        List<Module> modules = new ArrayList<>(super.nodeModules());
        for (int i = 0; i < modules.size(); ++i) {
            Module module = modules.get(i);
            if (module instanceof ClockModule) {
                // replacing the clock module so we'll be able
                // to control time in tests
                modules.set(i, new MockClockModule());
            }
        }
        return modules;
    }

    public static class MockClockModule extends ClockModule {
        @Override
        protected void configure() {
            bind(ClockMock.class).asEagerSingleton();
            bind(Clock.class).to(ClockMock.class);
        }
    }
}
