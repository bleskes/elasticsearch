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

package org.elasticsearch.xpack.ml;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.XPackFeatureSet;
import org.elasticsearch.xpack.XPackFeatureSet.Usage;
import org.elasticsearch.xpack.XPackPlugin;
import org.elasticsearch.xpack.ml.action.GetDatafeedsStatsAction;
import org.elasticsearch.xpack.ml.action.GetJobsStatsAction;
import org.elasticsearch.xpack.ml.action.util.QueryPage;
import org.elasticsearch.xpack.ml.datafeed.DatafeedConfig;
import org.elasticsearch.xpack.ml.datafeed.DatafeedState;
import org.elasticsearch.xpack.ml.job.config.AnalysisConfig;
import org.elasticsearch.xpack.ml.job.config.Detector;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.config.JobState;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.ModelSizeStats;
import org.elasticsearch.xpack.watcher.support.xcontent.XContentSource;
import org.junit.Before;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MachineLearningFeatureSetTests extends ESTestCase {

    private ClusterService clusterService;
    private Client client;
    private XPackLicenseState licenseState;

    @Before
    public void init() throws Exception {
        clusterService = mock(ClusterService.class);
        client = mock(Client.class);
        licenseState = mock(XPackLicenseState.class);
        givenJobs(Collections.emptyList(), Collections.emptyList());
        givenDatafeeds(Collections.emptyList());
    }

    public void testAvailable() throws Exception {
        MachineLearningFeatureSet featureSet = new MachineLearningFeatureSet(Settings.EMPTY, clusterService, client, licenseState);
        boolean available = randomBoolean();
        when(licenseState.isMachineLearningAllowed()).thenReturn(available);
        assertThat(featureSet.available(), is(available));
        PlainActionFuture<Usage> future = new PlainActionFuture<>();
        featureSet.usage(future);
        XPackFeatureSet.Usage usage = future.get();
        assertThat(usage.available(), is(available));

        BytesStreamOutput out = new BytesStreamOutput();
        usage.writeTo(out);
        XPackFeatureSet.Usage serializedUsage = new MachineLearningFeatureSet.Usage(out.bytes().streamInput());
        assertThat(serializedUsage.available(), is(available));
    }

    public void testEnabled() throws Exception {
        boolean useDefault = randomBoolean();
        boolean enabled = true;
        Settings.Builder settings = Settings.builder();
        if (useDefault == false) {
            enabled = randomBoolean();
            settings.put("xpack.ml.enabled", enabled);
        }
        boolean expected = enabled || useDefault;
        MachineLearningFeatureSet featureSet = new MachineLearningFeatureSet(settings.build(), clusterService, client, licenseState);
        assertThat(featureSet.enabled(), is(expected));
        PlainActionFuture<Usage> future = new PlainActionFuture<>();
        featureSet.usage(future);
        XPackFeatureSet.Usage usage = future.get();
        assertThat(usage.enabled(), is(expected));

        BytesStreamOutput out = new BytesStreamOutput();
        usage.writeTo(out);
        XPackFeatureSet.Usage serializedUsage = new MachineLearningFeatureSet.Usage(out.bytes().streamInput());
        assertThat(serializedUsage.enabled(), is(expected));
    }

    public void testUsage() throws Exception {
        when(licenseState.isMachineLearningAllowed()).thenReturn(true);
        Settings.Builder settings = Settings.builder();
        settings.put("xpack.ml.enabled", true);

        Job opened1 = buildJob("opened1", Arrays.asList(buildMinDetector("foo")));
        GetJobsStatsAction.Response.JobStats opened1JobStats = buildJobStats("opened1", JobState.OPENED, 100L);
        Job opened2 = buildJob("opened2", Arrays.asList(buildMinDetector("foo"), buildMinDetector("bar")));
        GetJobsStatsAction.Response.JobStats opened2JobStats = buildJobStats("opened2", JobState.OPENED, 200L);
        Job closed1 = buildJob("closed1", Arrays.asList(buildMinDetector("foo"), buildMinDetector("bar"), buildMinDetector("foobar")));
        GetJobsStatsAction.Response.JobStats closed1JobStats = buildJobStats("closed1", JobState.CLOSED, 300L);
        givenJobs(Arrays.asList(opened1, opened2, closed1),
                Arrays.asList(opened1JobStats, opened2JobStats, closed1JobStats));

        givenDatafeeds(Arrays.asList(
                buildDatafeedStats(DatafeedState.STARTED),
                buildDatafeedStats(DatafeedState.STARTED),
                buildDatafeedStats(DatafeedState.STOPPED)
        ));

        MachineLearningFeatureSet featureSet = new MachineLearningFeatureSet(settings.build(), clusterService, client, licenseState);
        PlainActionFuture<Usage> future = new PlainActionFuture<>();
        featureSet.usage(future);
        XPackFeatureSet.Usage mlUsage = future.get();

        BytesStreamOutput out = new BytesStreamOutput();
        mlUsage.writeTo(out);
        XPackFeatureSet.Usage serializedUsage = new MachineLearningFeatureSet.Usage(out.bytes().streamInput());

        for (XPackFeatureSet.Usage usage : Arrays.asList(mlUsage, serializedUsage)) {
            assertThat(usage, is(notNullValue()));
            assertThat(usage.name(), is(XPackPlugin.MACHINE_LEARNING));
            assertThat(usage.enabled(), is(true));
            assertThat(usage.available(), is(true));
            XContentSource source;
            try (XContentBuilder builder = XContentFactory.jsonBuilder()) {
                usage.toXContent(builder, ToXContent.EMPTY_PARAMS);
                source = new XContentSource(builder);
            }

            assertThat(source.getValue("jobs._all.count"), equalTo(3));
            assertThat(source.getValue("jobs._all.detectors.min"), equalTo(1.0));
            assertThat(source.getValue("jobs._all.detectors.max"), equalTo(3.0));
            assertThat(source.getValue("jobs._all.detectors.total"), equalTo(6.0));
            assertThat(source.getValue("jobs._all.detectors.avg"), equalTo(2.0));
            assertThat(source.getValue("jobs._all.model_size.min"), equalTo(100.0));
            assertThat(source.getValue("jobs._all.model_size.max"), equalTo(300.0));
            assertThat(source.getValue("jobs._all.model_size.total"), equalTo(600.0));
            assertThat(source.getValue("jobs._all.model_size.avg"), equalTo(200.0));

            assertThat(source.getValue("jobs.opened.count"), equalTo(2));
            assertThat(source.getValue("jobs.opened.detectors.min"), equalTo(1.0));
            assertThat(source.getValue("jobs.opened.detectors.max"), equalTo(2.0));
            assertThat(source.getValue("jobs.opened.detectors.total"), equalTo(3.0));
            assertThat(source.getValue("jobs.opened.detectors.avg"), equalTo(1.5));
            assertThat(source.getValue("jobs.opened.model_size.min"), equalTo(100.0));
            assertThat(source.getValue("jobs.opened.model_size.max"), equalTo(200.0));
            assertThat(source.getValue("jobs.opened.model_size.total"), equalTo(300.0));
            assertThat(source.getValue("jobs.opened.model_size.avg"), equalTo(150.0));

            assertThat(source.getValue("jobs.closed.count"), equalTo(1));
            assertThat(source.getValue("jobs.closed.detectors.min"), equalTo(3.0));
            assertThat(source.getValue("jobs.closed.detectors.max"), equalTo(3.0));
            assertThat(source.getValue("jobs.closed.detectors.total"), equalTo(3.0));
            assertThat(source.getValue("jobs.closed.detectors.avg"), equalTo(3.0));
            assertThat(source.getValue("jobs.closed.model_size.min"), equalTo(300.0));
            assertThat(source.getValue("jobs.closed.model_size.max"), equalTo(300.0));
            assertThat(source.getValue("jobs.closed.model_size.total"), equalTo(300.0));
            assertThat(source.getValue("jobs.closed.model_size.avg"), equalTo(300.0));

            assertThat(source.getValue("jobs.opening"), is(nullValue()));
            assertThat(source.getValue("jobs.closing"), is(nullValue()));
            assertThat(source.getValue("jobs.failed"), is(nullValue()));

            assertThat(source.getValue("datafeeds._all.count"), equalTo(3));
            assertThat(source.getValue("datafeeds.started.count"), equalTo(2));
            assertThat(source.getValue("datafeeds.stopped.count"), equalTo(1));
        }
    }

    public void testUsageGivenMlMetadataNotInstalled() throws Exception {
        when(licenseState.isMachineLearningAllowed()).thenReturn(true);
        Settings.Builder settings = Settings.builder();
        settings.put("xpack.ml.enabled", true);
        when(clusterService.state()).thenReturn(ClusterState.EMPTY_STATE);

        MachineLearningFeatureSet featureSet = new MachineLearningFeatureSet(settings.build(),
                clusterService, client, licenseState);
        PlainActionFuture<Usage> future = new PlainActionFuture<>();
        featureSet.usage(future);
        XPackFeatureSet.Usage usage = future.get();

        assertThat(usage.available(), is(true));
        assertThat(usage.enabled(), is(true));

        XContentSource source;
        try (XContentBuilder builder = XContentFactory.jsonBuilder()) {
            usage.toXContent(builder, ToXContent.EMPTY_PARAMS);
            source = new XContentSource(builder);
            assertThat(source.getValue("jobs"), equalTo(Collections.emptyMap()));
            assertThat(source.getValue("datafeeds"), equalTo(Collections.emptyMap()));
        }
    }

    private void givenJobs(List<Job> jobs, List<GetJobsStatsAction.Response.JobStats> jobsStats) {
        MlMetadata.Builder mlMetadataBuilder = new MlMetadata.Builder();
        for (Job job : jobs) {
            mlMetadataBuilder.putJob(job, false);
        }
        ClusterState clusterState = new ClusterState.Builder(ClusterState.EMPTY_STATE)
                .metaData(new MetaData.Builder()
                        .putCustom(MlMetadata.TYPE, mlMetadataBuilder.build()))
                .build();
        when(clusterService.state()).thenReturn(clusterState);

        doAnswer(invocationOnMock -> {
            ActionListener<GetJobsStatsAction.Response> listener =
                    (ActionListener<GetJobsStatsAction.Response>) invocationOnMock.getArguments()[2];
            listener.onResponse(new GetJobsStatsAction.Response(
                    new QueryPage<>(jobsStats, jobsStats.size(), Job.RESULTS_FIELD)));
            return Void.TYPE;
        }).when(client).execute(same(GetJobsStatsAction.INSTANCE), any(), any());
    }

    private void givenDatafeeds(List<GetDatafeedsStatsAction.Response.DatafeedStats> datafeedStats) {
        doAnswer(invocationOnMock -> {
            ActionListener<GetDatafeedsStatsAction.Response> listener =
                    (ActionListener<GetDatafeedsStatsAction.Response>) invocationOnMock.getArguments()[2];
            listener.onResponse(new GetDatafeedsStatsAction.Response(
                    new QueryPage<>(datafeedStats, datafeedStats.size(), DatafeedConfig.RESULTS_FIELD)));
            return Void.TYPE;
        }).when(client).execute(same(GetDatafeedsStatsAction.INSTANCE), any(), any());
    }

    private static Detector buildMinDetector(String fieldName) {
        Detector.Builder detectorBuilder = new Detector.Builder();
        detectorBuilder.setFunction("min");
        detectorBuilder.setFieldName(fieldName);
        return detectorBuilder.build();
    }

    private static Job buildJob(String jobId, List<Detector> detectors) {
        AnalysisConfig.Builder analysisConfig = new AnalysisConfig.Builder(detectors);
        return new Job.Builder(jobId)
                .setAnalysisConfig(analysisConfig)
                .build(new Date(randomNonNegativeLong()));
    }

    private static GetJobsStatsAction.Response.JobStats buildJobStats(String jobId, JobState state, long modelBytes) {
        ModelSizeStats.Builder modelSizeStats = new ModelSizeStats.Builder(jobId);
        modelSizeStats.setModelBytes(modelBytes);
        GetJobsStatsAction.Response.JobStats jobStats = mock(GetJobsStatsAction.Response.JobStats.class);
        when(jobStats.getJobId()).thenReturn(jobId);
        when(jobStats.getModelSizeStats()).thenReturn(modelSizeStats.build());
        when(jobStats.getState()).thenReturn(state);
        return jobStats;
    }

    private static GetDatafeedsStatsAction.Response.DatafeedStats buildDatafeedStats(DatafeedState state) {
        GetDatafeedsStatsAction.Response.DatafeedStats stats = mock(GetDatafeedsStatsAction.Response.DatafeedStats.class);
        when(stats.getDatafeedState()).thenReturn(state);
        return stats;
    }
}
