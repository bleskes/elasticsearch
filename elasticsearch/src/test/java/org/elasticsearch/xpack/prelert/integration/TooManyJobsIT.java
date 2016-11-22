/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.integration;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.util.concurrent.EsRejectedExecutionException;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.xpack.prelert.PrelertPlugin;
import org.elasticsearch.xpack.prelert.action.PostDataAction;
import org.elasticsearch.xpack.prelert.action.PutJobAction;
import org.elasticsearch.xpack.prelert.action.ScheduledJobsIT;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.Job;
import org.junit.After;

import java.util.Collection;
import java.util.Collections;

@ESIntegTestCase.ClusterScope(numDataNodes = 1)
public class TooManyJobsIT extends ESIntegTestCase {

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Collections.singleton(PrelertPlugin.class);
    }

    @Override
    protected Collection<Class<? extends Plugin>> transportClientPlugins() {
        return nodePlugins();
    }

    @After
    public void clearPrelertMetadata() throws Exception {
        ScheduledJobsIT.clearPrelertMetadata(client());
    }

    public void testCannotStartTooManyAnalyticalProcesses() throws Exception {
        String jsonLine = "{\"time\": \"0\"}";
        int maxNumJobs = 1000;
        for (int i = 1; i <= maxNumJobs; i++) {
            Job.Builder job = createJob(Integer.toString(i));
            PutJobAction.Request putJobRequest = new PutJobAction.Request(job.build(true));
            PutJobAction.Response putJobResponse = client().execute(PutJobAction.INSTANCE, putJobRequest).get();
            assertTrue(putJobResponse.isAcknowledged());

            // triggers creating autodetect process:
            PostDataAction.Request postDataRequest = new PostDataAction.Request(job.getId());
            postDataRequest.setContent(new BytesArray(jsonLine));
            try {
                PostDataAction.Response postDataResponse = client().execute(PostDataAction.INSTANCE, postDataRequest).get();
                assertEquals(1, postDataResponse.getDataCounts().getInputRecordCount());
                logger.info("Posted data {} times", i);
            } catch (Exception e) {
                Throwable cause = ExceptionsHelper.unwrapCause(e.getCause());
                assertEquals(ElasticsearchStatusException.class, cause.getClass());
                assertEquals(RestStatus.TOO_MANY_REQUESTS, ((ElasticsearchStatusException) cause).status());
                logger.info("good news everybody --> reached threadpool capacity after starting {}th analytical process", i);

                // now manually clean things up and see if we can succeed to start one new job
                clearPrelertMetadata();
                putJobResponse = client().execute(PutJobAction.INSTANCE, putJobRequest).get();
                assertTrue(putJobResponse.isAcknowledged());
                PostDataAction.Response postDataResponse = client().execute(PostDataAction.INSTANCE, postDataRequest).get();
                assertEquals(1, postDataResponse.getDataCounts().getInputRecordCount());
                return;
            }
        }

        fail("shouldn't be able to add [" + maxNumJobs + "] jobs");
    }

    private Job.Builder createJob(String id) {
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setFormat(DataDescription.DataFormat.JSON);
        dataDescription.setTimeFormat(DataDescription.EPOCH_MS);

        Detector.Builder d = new Detector.Builder("count", null);
        AnalysisConfig.Builder analysisConfig = new AnalysisConfig.Builder(Collections.singletonList(d.build()));

        Job.Builder builder = new Job.Builder();
        builder.setId(id);

        builder.setAnalysisConfig(analysisConfig);
        builder.setDataDescription(dataDescription);
        return builder;
    }

}
