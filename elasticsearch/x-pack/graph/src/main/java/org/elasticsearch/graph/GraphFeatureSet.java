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

package org.elasticsearch.graph;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.XPackFeatureSet;

/**
 *
 */
public class GraphFeatureSet implements XPackFeatureSet {

    private final boolean enabled;
    private final GraphLicensee licensee;

    @Inject
    public GraphFeatureSet(Settings settings, @Nullable  GraphLicensee licensee) {
        this.enabled = Graph.enabled(settings);
        this.licensee = licensee;
    }

    @Override
    public String name() {
        return Graph.NAME;
    }

    @Override
    public String description() {
        return "Graph Data Exploration for the Elastic Stack";
    }

    @Override
    public boolean available() {
        return licensee != null && licensee.isAvailable();
    }

    @Override
    public boolean enabled() {
        return enabled;
    }
}
