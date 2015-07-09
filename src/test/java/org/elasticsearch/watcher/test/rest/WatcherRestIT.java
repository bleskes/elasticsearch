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

package org.elasticsearch.watcher.test.rest;

import org.elasticsearch.test.rest.RestTestCandidate;

/** Runs rest tests against external cluster */
public class WatcherRestIT extends WatcherRestTests {

    public WatcherRestIT(RestTestCandidate testCandidate) {
        super(testCandidate);
    }

    @Override
    protected boolean enableShield() {
        return false;
    }
}
