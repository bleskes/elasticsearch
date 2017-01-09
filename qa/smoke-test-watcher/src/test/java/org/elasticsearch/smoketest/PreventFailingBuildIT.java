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

package org.elasticsearch.smoketest;

import org.elasticsearch.test.ESIntegTestCase;

public class PreventFailingBuildIT extends ESIntegTestCase {

    public void testSoThatTestsDoNotFail() {
        // Noop

        // This is required because SmokeTestWatcherClientYamlTestSuiteIT
        // requires network access, so if network tests are not enable no
        // tests will be run in the entire project and all tests will fail.
    }
}
