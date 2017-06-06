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

package org.elasticsearch.xpack.monitoring.resolver.ml;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.ml.action.GetJobsStatsAction.Response.JobStats;
import org.elasticsearch.xpack.monitoring.MonitoredSystem;
import org.elasticsearch.xpack.monitoring.collector.ml.JobStatsMonitoringDoc;
import org.elasticsearch.xpack.monitoring.resolver.MonitoringIndexNameResolver;

import java.io.IOException;

public class JobStatsResolver extends MonitoringIndexNameResolver.Timestamped<JobStatsMonitoringDoc> {

    public JobStatsResolver(MonitoredSystem system, Settings settings) {
        super(system, settings);
    }

    @Override
    protected void buildXContent(JobStatsMonitoringDoc document, XContentBuilder builder, ToXContent.Params params) throws IOException {
        final JobStats jobStats = document.getJobStats();

        builder.startObject(document.getType());
        {
            jobStats.toUnwrappedXContent(builder);
        }
        builder.endObject();
    }

}
