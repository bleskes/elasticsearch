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

package org.elasticsearch.xpack.monitoring;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.xpack.XPackFeatureSet;
import org.elasticsearch.xpack.monitoring.agent.exporter.Exporter;
import org.elasticsearch.xpack.monitoring.agent.exporter.Exporters;

/**
 *
 */
public class MonitoringFeatureSet implements XPackFeatureSet {

    private final boolean enabled;
    private final XPackLicenseState licenseState;
    private final Exporters exporters;

    @Inject
    public MonitoringFeatureSet(Settings settings, @Nullable XPackLicenseState licenseState, @Nullable Exporters exporters) {
        this.enabled = MonitoringSettings.ENABLED.get(settings);
        this.licenseState = licenseState;
        this.exporters = exporters;
    }

    @Override
    public String name() {
        return Monitoring.NAME;
    }

    @Override
    public String description() {
        return "Monitoring for the Elastic Stack";
    }

    @Override
    public boolean available() {
        return licenseState != null && licenseState.isMonitoringAllowed();
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public XPackFeatureSet.Usage usage() {
        return new Usage(available(), enabled(), exportersUsage(exporters));
    }

    static Map<String, Object> exportersUsage(Exporters exporters) {
        if (exporters == null) {
            return null;
        }
        Map<String, Object> usage = new HashMap<>();
        for (Exporter exporter : exporters) {
            if (exporter.config().enabled()) {
                String type = exporter.config().type();
                int count = (Integer) usage.getOrDefault(type, 0);
                usage.put(type, count + 1);
            }
        }
        return usage;
    }

    public static class Usage extends XPackFeatureSet.Usage {

        private static final String ENABLED_EXPORTERS_XFIELD = "enabled_exporters";

        @Nullable private Map<String, Object> exporters;

        public Usage(StreamInput in) throws IOException {
            super(in);
            exporters = in.readMap();
        }

        public Usage(boolean available, boolean enabled, Map<String, Object> exporters) {
            super(Monitoring.NAME, available, enabled);
            this.exporters = exporters;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeMap(exporters);
        }

        @Override
        protected void innerXContent(XContentBuilder builder, Params params) throws IOException {
            super.innerXContent(builder, params);
            if (exporters != null) {
                builder.field(ENABLED_EXPORTERS_XFIELD, exporters);
            }
        }
    }

}
