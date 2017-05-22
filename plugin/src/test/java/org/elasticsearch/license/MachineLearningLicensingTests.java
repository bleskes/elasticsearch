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

package org.elasticsearch.license;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.action.support.PlainListenableActionFuture;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.license.License.OperationMode;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.test.junit.annotations.TestLogging;
import org.elasticsearch.transport.Transport;
import org.elasticsearch.xpack.TestXPackTransportClient;
import org.elasticsearch.xpack.XPackPlugin;
import org.elasticsearch.xpack.ml.action.CloseJobAction;
import org.elasticsearch.xpack.ml.action.DeleteDatafeedAction;
import org.elasticsearch.xpack.ml.action.DeleteJobAction;
import org.elasticsearch.xpack.ml.action.GetDatafeedsStatsAction;
import org.elasticsearch.xpack.ml.action.GetJobsStatsAction;
import org.elasticsearch.xpack.ml.action.OpenJobAction;
import org.elasticsearch.xpack.ml.action.PutDatafeedAction;
import org.elasticsearch.xpack.ml.action.PutJobAction;
import org.elasticsearch.xpack.ml.action.StartDatafeedAction;
import org.elasticsearch.xpack.ml.action.StopDatafeedAction;
import org.elasticsearch.xpack.ml.client.MachineLearningClient;
import org.elasticsearch.xpack.ml.datafeed.DatafeedState;
import org.elasticsearch.xpack.ml.job.config.JobState;
import org.elasticsearch.xpack.ml.support.BaseMlIntegTestCase;
import org.elasticsearch.xpack.persistent.PersistentTasksCustomMetaData;
import org.junit.Before;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

@TestLogging("org.elasticsearch.xpack.ml.action:DEBUG")
public class MachineLearningLicensingTests extends BaseMlIntegTestCase {

    @Before
    public void resetLicensing() {
        enableLicensing();

        ensureStableCluster(1);
        ensureYellow();
    }

    @Override
    protected boolean addMockTransportService() {
        return false;
    }

    public void testMachineLearningPutJobActionRestricted() throws Exception {
        String jobId = "testmachinelearningputjobactionrestricted";
        // Pick a license that does not allow machine learning
        License.OperationMode mode = randomInvalidLicenseType();
        enableLicensing(mode);
        assertMLAllowed(false);
        // test that license restricted apis do not work
        try (TransportClient client = new TestXPackTransportClient(internalCluster().transportClient().settings())) {
            client.addTransportAddress(internalCluster().getDataNodeInstance(Transport.class).boundAddress().publishAddress());
            PlainListenableActionFuture<PutJobAction.Response> listener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).putJob(new PutJobAction.Request(createJob(jobId)), listener);
            listener.actionGet();
            fail("put job action should not be enabled!");
        } catch (ElasticsearchSecurityException e) {
            assertThat(e.status(), is(RestStatus.FORBIDDEN));
            assertThat(e.getMessage(), containsString("non-compliant"));
            assertThat(e.getMetadata(LicenseUtils.EXPIRED_FEATURE_METADATA), hasItem(XPackPlugin.MACHINE_LEARNING));
        }

        // Pick a license that does allow machine learning
        mode = randomValidLicenseType();
        enableLicensing(mode);
        assertMLAllowed(true);
        // test that license restricted apis do now work
        try (TransportClient client = new TestXPackTransportClient(internalCluster().transportClient().settings())) {
            client.addTransportAddress(internalCluster().getDataNodeInstance(Transport.class).boundAddress().publishAddress());
            PlainListenableActionFuture<PutJobAction.Response> listener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).putJob(new PutJobAction.Request(createJob(jobId)), listener);
            PutJobAction.Response response = listener.actionGet();
            assertNotNull(response);
        }
    }

    public void testMachineLearningOpenJobActionRestricted() throws Exception {
        String jobId = "testmachinelearningopenjobactionrestricted";
        assertMLAllowed(true);
        // test that license restricted apis do now work
        try (TransportClient client = new TestXPackTransportClient(internalCluster().transportClient().settings())) {
            client.addTransportAddress(internalCluster().getDataNodeInstance(Transport.class).boundAddress().publishAddress());
            PlainListenableActionFuture<PutJobAction.Response> putJobListener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).putJob(new PutJobAction.Request(createJob(jobId)), putJobListener);
            PutJobAction.Response response = putJobListener.actionGet();
            assertNotNull(response);
        }
        
        // Pick a license that does not allow machine learning
        License.OperationMode mode = randomInvalidLicenseType();
        enableLicensing(mode);
        assertMLAllowed(false);
        // test that license restricted apis do not work
        try (TransportClient client = new TestXPackTransportClient(internalCluster().transportClient().settings())) {
            client.addTransportAddress(internalCluster().getDataNodeInstance(Transport.class).boundAddress().publishAddress());
            PlainListenableActionFuture<OpenJobAction.Response> listener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).openJob(new OpenJobAction.Request(jobId), listener);
            listener.actionGet();
            fail("open job action should not be enabled!");
        } catch (ElasticsearchSecurityException e) {
            assertThat(e.status(), is(RestStatus.FORBIDDEN));
            assertThat(e.getMessage(), containsString("non-compliant"));
            assertThat(e.getMetadata(LicenseUtils.EXPIRED_FEATURE_METADATA), hasItem(XPackPlugin.MACHINE_LEARNING));
        }

        // Pick a license that does allow machine learning
        mode = randomValidLicenseType();
        enableLicensing(mode);
        assertMLAllowed(true);

        // now that the license is invalid, the job should get closed:
        assertBusy(() -> {
            JobState jobState = getJobStats(jobId).getState();
            assertEquals(JobState.CLOSED, jobState);
        });

        // test that license restricted apis do now work
        try (TransportClient client = new TestXPackTransportClient(internalCluster().transportClient().settings())) {
            client.addTransportAddress(internalCluster().getDataNodeInstance(Transport.class).boundAddress().publishAddress());
            PlainListenableActionFuture<OpenJobAction.Response> listener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).openJob(new OpenJobAction.Request(jobId), listener);
            OpenJobAction.Response response = listener.actionGet();
            assertNotNull(response);
        }
    }

    public void testMachineLearningPutDatafeedActionRestricted() throws Exception {
        String jobId = "testmachinelearningputdatafeedactionrestricted";
        String datafeedId = jobId + "-datafeed";
        assertMLAllowed(true);
        // test that license restricted apis do now work
        try (TransportClient client = new TestXPackTransportClient(internalCluster().transportClient().settings())) {
            client.addTransportAddress(internalCluster().getDataNodeInstance(Transport.class).boundAddress().publishAddress());
            PlainListenableActionFuture<PutJobAction.Response> putJobListener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).putJob(new PutJobAction.Request(createJob(jobId)), putJobListener);
            PutJobAction.Response putJobResponse = putJobListener.actionGet();
            assertNotNull(putJobResponse);
        }
        
        // Pick a license that does not allow machine learning
        License.OperationMode mode = randomInvalidLicenseType();
        enableLicensing(mode);
        assertMLAllowed(false);
        // test that license restricted apis do not work
        try (TransportClient client = new TestXPackTransportClient(internalCluster().transportClient().settings())) {
            client.addTransportAddress(internalCluster().getDataNodeInstance(Transport.class).boundAddress().publishAddress());
            PlainListenableActionFuture<PutDatafeedAction.Response> listener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).putDatafeed(
                    new PutDatafeedAction.Request(createDatafeed(datafeedId, jobId, Collections.singletonList(jobId))), listener);
            listener.actionGet();
            fail("put datafeed action should not be enabled!");
        } catch (ElasticsearchSecurityException e) {
            assertThat(e.status(), is(RestStatus.FORBIDDEN));
            assertThat(e.getMessage(), containsString("non-compliant"));
            assertThat(e.getMetadata(LicenseUtils.EXPIRED_FEATURE_METADATA), hasItem(XPackPlugin.MACHINE_LEARNING));
        }

        // Pick a license that does allow machine learning
        mode = randomValidLicenseType();
        enableLicensing(mode);
        assertMLAllowed(true);
        // test that license restricted apis do now work
        try (TransportClient client = new TestXPackTransportClient(internalCluster().transportClient().settings())) {
            client.addTransportAddress(internalCluster().getDataNodeInstance(Transport.class).boundAddress().publishAddress());
            PlainListenableActionFuture<PutDatafeedAction.Response> listener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).putDatafeed(
                    new PutDatafeedAction.Request(createDatafeed(datafeedId, jobId, Collections.singletonList(jobId))), listener);
            PutDatafeedAction.Response response = listener.actionGet();
            assertNotNull(response);
        }
    }

    public void testAutoCloseJobWithDatafeed() throws Exception {
        String jobId = "testautoclosejobwithdatafeed";
        String datafeedId = jobId + "-datafeed";
        assertMLAllowed(true);
        String datafeedIndex = jobId + "-data";
        createIndex(datafeedIndex);
        try (TransportClient client = new TestXPackTransportClient(internalCluster().transportClient().settings())) {
            client.addTransportAddress(internalCluster().getDataNodeInstance(Transport.class).boundAddress().publishAddress());
            // put job
            PlainListenableActionFuture<PutJobAction.Response> putJobListener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).putJob(new PutJobAction.Request(createJob(jobId)), putJobListener);
            PutJobAction.Response putJobResponse = putJobListener.actionGet();
            assertNotNull(putJobResponse);
            // put datafeed
            PlainListenableActionFuture<PutDatafeedAction.Response> putDatafeedListener = new PlainListenableActionFuture<>(
                    client.threadPool());
            new MachineLearningClient(client).putDatafeed(
                    new PutDatafeedAction.Request(createDatafeed(datafeedId, jobId,
                            Collections.singletonList(datafeedIndex))), putDatafeedListener);
            PutDatafeedAction.Response putDatafeedResponse = putDatafeedListener.actionGet();
            assertNotNull(putDatafeedResponse);
            // open job
            PlainListenableActionFuture<OpenJobAction.Response> openJobListener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).openJob(new OpenJobAction.Request(jobId), openJobListener);
            OpenJobAction.Response openJobResponse = openJobListener.actionGet();
            assertNotNull(openJobResponse);
            // start datafeed
            PlainListenableActionFuture<StartDatafeedAction.Response> listener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).startDatafeed(new StartDatafeedAction.Request(datafeedId, 0L), listener);
            listener.actionGet();
        }

        if (randomBoolean()) {
            enableLicensing(randomInvalidLicenseType());
        } else {
            disableLicensing();
        }
        assertMLAllowed(false);

        // now that the license is invalid, the job should be closed and datafeed stopped:
        assertBusy(() -> {
            JobState jobState = getJobStats(jobId).getState();
            assertEquals(JobState.CLOSED, jobState);

            DatafeedState datafeedState = getDatafeedStats(datafeedId).getDatafeedState();
            assertEquals(DatafeedState.STOPPED, datafeedState);

            ClusterState state = client().admin().cluster().prepareState().get().getState();
            PersistentTasksCustomMetaData tasks = state.metaData().custom(PersistentTasksCustomMetaData.TYPE);
            assertEquals(0, tasks.taskMap().size());
        });

        enableLicensing(randomValidLicenseType());
        assertMLAllowed(true);

        try (TransportClient client = new TestXPackTransportClient(internalCluster().transportClient().settings())) {
            client.addTransportAddress(internalCluster().getDataNodeInstance(Transport.class).boundAddress().publishAddress());
            // open job
            PlainListenableActionFuture<OpenJobAction.Response> openJobListener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).openJob(new OpenJobAction.Request(jobId), openJobListener);
            OpenJobAction.Response openJobResponse = openJobListener.actionGet();
            assertNotNull(openJobResponse);
            // start datafeed
            PlainListenableActionFuture<StartDatafeedAction.Response> listener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).startDatafeed(new StartDatafeedAction.Request(datafeedId, 0L), listener);
            listener.actionGet();
        }

        assertBusy(() -> {
            JobState jobState = getJobStats(jobId).getState();
            assertEquals(JobState.OPENED, jobState);

            DatafeedState datafeedState = getDatafeedStats(datafeedId).getDatafeedState();
            assertEquals(DatafeedState.STARTED, datafeedState);

            ClusterState state = client().admin().cluster().prepareState().get().getState();
            PersistentTasksCustomMetaData tasks = state.metaData().custom(PersistentTasksCustomMetaData.TYPE);
            assertEquals(2, tasks.taskMap().size());
        });

        if (randomBoolean()) {
            enableLicensing(randomInvalidLicenseType());
        } else {
            disableLicensing();
        }
        assertMLAllowed(false);

        // now that the license is invalid, the job should be closed and datafeed stopped:
        assertBusy(() -> {
            JobState jobState = getJobStats(jobId).getState();
            assertEquals(JobState.CLOSED, jobState);

            DatafeedState datafeedState = getDatafeedStats(datafeedId).getDatafeedState();
            assertEquals(DatafeedState.STOPPED, datafeedState);

            ClusterState state = client().admin().cluster().prepareState().get().getState();
            PersistentTasksCustomMetaData tasks = state.metaData().custom(PersistentTasksCustomMetaData.TYPE);
            assertEquals(0, tasks.taskMap().size());
        });
    }

    public void testMachineLearningStartDatafeedActionRestricted() throws Exception {
        String jobId = "testmachinelearningstartdatafeedactionrestricted";
        String datafeedId = jobId + "-datafeed";
        assertMLAllowed(true);
        String datafeedIndex = jobId + "-data";
        createIndex(datafeedIndex);
        // test that license restricted apis do now work
        try (TransportClient client = new TestXPackTransportClient(internalCluster().transportClient().settings())) {
            client.addTransportAddress(internalCluster().getDataNodeInstance(Transport.class).boundAddress().publishAddress());
            PlainListenableActionFuture<PutJobAction.Response> putJobListener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).putJob(new PutJobAction.Request(createJob(jobId)), putJobListener);
            PutJobAction.Response putJobResponse = putJobListener.actionGet();
            assertNotNull(putJobResponse);
            PlainListenableActionFuture<PutDatafeedAction.Response> putDatafeedListener = new PlainListenableActionFuture<>(
                    client.threadPool());
            new MachineLearningClient(client).putDatafeed(
                    new PutDatafeedAction.Request(createDatafeed(datafeedId, jobId,
                            Collections.singletonList(datafeedIndex))), putDatafeedListener);
            PutDatafeedAction.Response putDatafeedResponse = putDatafeedListener.actionGet();
            assertNotNull(putDatafeedResponse);
            PlainListenableActionFuture<OpenJobAction.Response> openJobListener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).openJob(new OpenJobAction.Request(jobId), openJobListener);
            OpenJobAction.Response openJobResponse = openJobListener.actionGet();
            assertNotNull(openJobResponse);
        }

        // Pick a license that does not allow machine learning
        License.OperationMode mode = randomInvalidLicenseType();
        enableLicensing(mode);
        assertMLAllowed(false);

        // now that the license is invalid, the job should get closed:
        assertBusy(() -> {
            JobState jobState = getJobStats(jobId).getState();
            assertEquals(JobState.CLOSED, jobState);
            ClusterState state = client().admin().cluster().prepareState().get().getState();
            PersistentTasksCustomMetaData tasks = state.metaData().custom(PersistentTasksCustomMetaData.TYPE);
            assertEquals(0, tasks.taskMap().size());
        });

        // test that license restricted apis do not work
        try (TransportClient client = new TestXPackTransportClient(internalCluster().transportClient().settings())) {
            client.addTransportAddress(internalCluster().getDataNodeInstance(Transport.class).boundAddress().publishAddress());
            PlainListenableActionFuture<StartDatafeedAction.Response> listener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).startDatafeed(new StartDatafeedAction.Request(datafeedId, 0L), listener);
            listener.actionGet();
            fail("start datafeed action should not be enabled!");
        } catch (ElasticsearchSecurityException e) {
            assertThat(e.status(), is(RestStatus.FORBIDDEN));
            assertThat(e.getMessage(), containsString("non-compliant"));
            assertThat(e.getMetadata(LicenseUtils.EXPIRED_FEATURE_METADATA), hasItem(XPackPlugin.MACHINE_LEARNING));
        }

        // Pick a license that does allow machine learning
        mode = randomValidLicenseType();
        enableLicensing(mode);
        assertMLAllowed(true);
        // test that license restricted apis do now work
        try (TransportClient client = new TestXPackTransportClient(internalCluster().transportClient().settings())) {
            client.addTransportAddress(internalCluster().getDataNodeInstance(Transport.class).boundAddress().publishAddress());
            // re-open job now that the license is valid again
            PlainListenableActionFuture<OpenJobAction.Response> openJobListener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).openJob(new OpenJobAction.Request(jobId), openJobListener);
            OpenJobAction.Response openJobResponse = openJobListener.actionGet();
            assertNotNull(openJobResponse);

            PlainListenableActionFuture<StartDatafeedAction.Response> listener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).startDatafeed(new StartDatafeedAction.Request(datafeedId, 0L), listener);
            StartDatafeedAction.Response response = listener.actionGet();
            assertNotNull(response);
        }
    }

    public void testMachineLearningStopDatafeedActionNotRestricted() throws Exception {
        String jobId = "testmachinelearningstopdatafeedactionnotrestricted";
        String datafeedId = jobId + "-datafeed";
        assertMLAllowed(true);
        String datafeedIndex = jobId + "-data";
        createIndex(datafeedIndex);
        // test that license restricted apis do now work
        try (TransportClient client = new TestXPackTransportClient(internalCluster().transportClient().settings())) {
            client.addTransportAddress(internalCluster().getDataNodeInstance(Transport.class).boundAddress().publishAddress());
            PlainListenableActionFuture<PutJobAction.Response> putJobListener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).putJob(new PutJobAction.Request(createJob(jobId)), putJobListener);
            PutJobAction.Response putJobResponse = putJobListener.actionGet();
            assertNotNull(putJobResponse);
            PlainListenableActionFuture<PutDatafeedAction.Response> putDatafeedListener = new PlainListenableActionFuture<>(
                    client.threadPool());
            new MachineLearningClient(client).putDatafeed(
                    new PutDatafeedAction.Request(createDatafeed(datafeedId, jobId,
                            Collections.singletonList(datafeedIndex))), putDatafeedListener);
            PutDatafeedAction.Response putDatafeedResponse = putDatafeedListener.actionGet();
            assertNotNull(putDatafeedResponse);
            PlainListenableActionFuture<OpenJobAction.Response> openJobListener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).openJob(new OpenJobAction.Request(jobId), openJobListener);
            OpenJobAction.Response openJobResponse = openJobListener.actionGet();
            assertNotNull(openJobResponse);
            PlainListenableActionFuture<StartDatafeedAction.Response> startDatafeedListener = new PlainListenableActionFuture<>(
                    client.threadPool());
            new MachineLearningClient(client).startDatafeed(new StartDatafeedAction.Request(datafeedId, 0L), startDatafeedListener);
            StartDatafeedAction.Response startDatafeedResponse = startDatafeedListener.actionGet();
            assertNotNull(startDatafeedResponse);
        }

        boolean invalidLicense = randomBoolean();
        if (invalidLicense) {
            enableLicensing(randomInvalidLicenseType());
        } else {
            enableLicensing(randomValidLicenseType());
        }

        try (TransportClient client = new TestXPackTransportClient(internalCluster().transportClient().settings())) {
            client.addTransportAddress(internalCluster().getDataNodeInstance(Transport.class).boundAddress().publishAddress());
            PlainListenableActionFuture<StopDatafeedAction.Response> listener = new PlainListenableActionFuture<>(
                    client.threadPool());
            new MachineLearningClient(client).stopDatafeed(new StopDatafeedAction.Request(datafeedId), listener);
            if (invalidLicense) {
                // the stop datafeed due to invalid license happens async, so check if the datafeed turns into stopped state:
                assertBusy(() -> {
                    GetDatafeedsStatsAction.Response response =
                            new MachineLearningClient(client).getDatafeedsStats(
                                    new GetDatafeedsStatsAction.Request(datafeedId)).actionGet();
                    assertEquals(DatafeedState.STOPPED, response.getResponse().results().get(0).getDatafeedState());
                });
            } else {
                listener.actionGet();
            }
        }
    }

    public void testMachineLearningCloseJobActionNotRestricted() throws Exception {
        String jobId = "testmachinelearningclosejobactionnotrestricted";
        assertMLAllowed(true);
        // test that license restricted apis do now work
        try (TransportClient client = new TestXPackTransportClient(internalCluster().transportClient().settings())) {
            client.addTransportAddress(internalCluster().getDataNodeInstance(Transport.class).boundAddress().publishAddress());
            PlainListenableActionFuture<PutJobAction.Response> putJobListener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).putJob(new PutJobAction.Request(createJob(jobId)), putJobListener);
            PutJobAction.Response putJobResponse = putJobListener.actionGet();
            assertNotNull(putJobResponse);
            PlainListenableActionFuture<OpenJobAction.Response> openJobListener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).openJob(new OpenJobAction.Request(jobId), openJobListener);
            OpenJobAction.Response openJobResponse = openJobListener.actionGet();
            assertNotNull(openJobResponse);
        }

        boolean invalidLicense = randomBoolean();
        if (invalidLicense) {
            enableLicensing(randomInvalidLicenseType());
        } else {
            enableLicensing(randomValidLicenseType());
        }

        try (TransportClient client = new TestXPackTransportClient(internalCluster().transportClient().settings())) {
            client.addTransportAddress(internalCluster().getDataNodeInstance(Transport.class).boundAddress().publishAddress());
            PlainListenableActionFuture<CloseJobAction.Response> listener = new PlainListenableActionFuture<>(client.threadPool());
            CloseJobAction.Request request = new CloseJobAction.Request(jobId);
            request.setCloseTimeout(TimeValue.timeValueSeconds(20));
            if (invalidLicense) {
                // the close due to invalid license happens async, so check if the job turns into closed state:
                assertBusy(() -> {
                    GetJobsStatsAction.Response response =
                            new MachineLearningClient(client).getJobsStats(new GetJobsStatsAction.Request(jobId)).actionGet();
                    assertEquals(JobState.CLOSED, response.getResponse().results().get(0).getState());
                });
            } else {
                new MachineLearningClient(client).closeJob(request, listener);
                listener.actionGet();
            }
        }
    }

    public void testMachineLearningDeleteJobActionNotRestricted() throws Exception {
        String jobId = "testmachinelearningclosejobactionnotrestricted";
        assertMLAllowed(true);
        // test that license restricted apis do now work
        try (TransportClient client = new TestXPackTransportClient(internalCluster().transportClient().settings())) {
            client.addTransportAddress(internalCluster().getDataNodeInstance(Transport.class).boundAddress().publishAddress());
            PlainListenableActionFuture<PutJobAction.Response> putJobListener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).putJob(new PutJobAction.Request(createJob(jobId)), putJobListener);
            PutJobAction.Response putJobResponse = putJobListener.actionGet();
            assertNotNull(putJobResponse);
        }

        // Pick a random license
        License.OperationMode mode = randomLicenseType();
        enableLicensing(mode);

        try (TransportClient client = new TestXPackTransportClient(internalCluster().transportClient().settings())) {
            client.addTransportAddress(internalCluster().getDataNodeInstance(Transport.class).boundAddress().publishAddress());
            PlainListenableActionFuture<DeleteJobAction.Response> listener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).deleteJob(new DeleteJobAction.Request(jobId), listener);
            listener.actionGet();
        }
    }

    public void testMachineLearningDeleteDatafeedActionNotRestricted() throws Exception {
        String jobId = "testmachinelearningdeletedatafeedactionnotrestricted";
        String datafeedId = jobId + "-datafeed";
        assertMLAllowed(true);
        // test that license restricted apis do now work
        try (TransportClient client = new TestXPackTransportClient(internalCluster().transportClient().settings())) {
            client.addTransportAddress(internalCluster().getDataNodeInstance(Transport.class).boundAddress().publishAddress());
            PlainListenableActionFuture<PutJobAction.Response> putJobListener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).putJob(new PutJobAction.Request(createJob(jobId)), putJobListener);
            PutJobAction.Response putJobResponse = putJobListener.actionGet();
            assertNotNull(putJobResponse);
            PlainListenableActionFuture<PutDatafeedAction.Response> putDatafeedListener = new PlainListenableActionFuture<>(
                    client.threadPool());
            new MachineLearningClient(client).putDatafeed(
                    new PutDatafeedAction.Request(createDatafeed(datafeedId, jobId,
                            Collections.singletonList(jobId))), putDatafeedListener);
            PutDatafeedAction.Response putDatafeedResponse = putDatafeedListener.actionGet();
            assertNotNull(putDatafeedResponse);
        }

        // Pick a random license
        License.OperationMode mode = randomLicenseType();
        enableLicensing(mode);

        try (TransportClient client = new TestXPackTransportClient(internalCluster().transportClient().settings())) {
            client.addTransportAddress(internalCluster().getDataNodeInstance(Transport.class).boundAddress().publishAddress());
            PlainListenableActionFuture<DeleteDatafeedAction.Response> listener = new PlainListenableActionFuture<>(client.threadPool());
            new MachineLearningClient(client).deleteDatafeed(new DeleteDatafeedAction.Request(datafeedId), listener);
            listener.actionGet();
        }
    }

    private static OperationMode randomInvalidLicenseType() {
        return randomFrom(License.OperationMode.GOLD, License.OperationMode.STANDARD, License.OperationMode.BASIC);
    }

    private static OperationMode randomValidLicenseType() {
        return randomFrom(License.OperationMode.TRIAL, License.OperationMode.PLATINUM);
    }

    private static OperationMode randomLicenseType() {
        return randomFrom(License.OperationMode.values());
    }

    private static void assertMLAllowed(boolean expected) {
        for (XPackLicenseState licenseState : internalCluster().getInstances(XPackLicenseState.class)) {
            assertEquals(licenseState.isMachineLearningAllowed(), expected);
        }
    }

    public static void disableLicensing() {
        disableLicensing(randomValidLicenseType());
    }

    public static void disableLicensing(License.OperationMode operationMode) {
        for (XPackLicenseState licenseState : internalCluster().getInstances(XPackLicenseState.class)) {
            licenseState.update(operationMode, false);
        }
    }

    public static void enableLicensing() {
        enableLicensing(randomValidLicenseType());
    }

    public static void enableLicensing(License.OperationMode operationMode) {
        for (XPackLicenseState licenseState : internalCluster().getInstances(XPackLicenseState.class)) {
            licenseState.update(operationMode, true);
        }
    }
}
