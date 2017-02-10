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

package org.elasticsearch.xpack.watcher.condition;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.support.clock.Clock;

import java.io.IOException;

/**
 * Parses xcontent to a concrete condition of the same type.
 */
public interface ConditionFactory {

    /**
     * Parses the given xcontent and creates a concrete condition
     *  @param watchId                   The id of the watch
     * @param parser                    The parsing that contains the condition content
     * @param upgradeConditionSource    Whether to upgrade the source related to condition if in legacy format
     *                                  Note: depending on the version, only conditions implementations that have a
     */
    Condition parse(Clock clock, String watchId, XContentParser parser, boolean upgradeConditionSource) throws IOException;

}
