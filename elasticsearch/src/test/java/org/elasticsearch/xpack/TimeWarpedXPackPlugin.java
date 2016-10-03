/*
 * ELASTICSEARCH CONFIDENTIAL
 *  __________________
 *
 * [2014] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.support.clock.Clock;
import org.elasticsearch.xpack.support.clock.ClockMock;
import org.elasticsearch.xpack.watcher.test.TimeWarpedWatcher;

import java.io.IOException;

public class TimeWarpedXPackPlugin extends XPackPlugin {

    public TimeWarpedXPackPlugin(Settings settings) throws IOException {
        super(settings);
        watcher = new TimeWarpedWatcher(settings);
    }

    @Override
    protected Clock getClock() {
        return new ClockMock();
    }
}
