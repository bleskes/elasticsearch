/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
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

package org.elasticsearch.smoketest;

import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;

import org.apache.http.HttpStatus;
import org.elasticsearch.action.admin.cluster.node.tasks.list.ListTasksAction;
import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;
import org.elasticsearch.test.rest.yaml.ClientYamlTestResponse;
import org.elasticsearch.test.rest.yaml.ESClientYamlSuiteTestCase;
import org.elasticsearch.xpack.ml.MachineLearningTemplateRegistry;
import org.elasticsearch.xpack.ml.integration.MlRestTestStateCleaner;
import org.elasticsearch.xpack.ml.job.persistence.AnomalyDetectorsIndex;
import org.elasticsearch.xpack.security.SecurityTemplateService;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.elasticsearch.xpack.security.authc.support.UsernamePasswordToken.basicAuthHeaderValue;


public class MlWithSecurityIT extends ESClientYamlSuiteTestCase {

    private static final String TEST_ADMIN_USERNAME = "test_admin";
    private static final String TEST_ADMIN_PASSWORD = "changeme";

    @After
    public void clearMlState() throws IOException, InterruptedException {
        new MlRestTestStateCleaner(logger, adminClient(), this).clearMlMetadata();
        waitForPendingTasks();
    }

    private void waitForPendingTasks() throws InterruptedException {
        AtomicReference<IOException> exceptionHolder = new AtomicReference<>();
        Map<String, String> headers = Collections.singletonMap("Authorization",
                basicAuthHeaderValue(TEST_ADMIN_USERNAME, new SecureString(TEST_ADMIN_PASSWORD.toCharArray())));
        awaitBusy(() -> {
            try {
                ClientYamlTestResponse response = getAdminExecutionContext().callApi("cat.tasks",
                        Collections.emptyMap(), Collections.emptyList(), headers);
                // Check to see if there are tasks still active. We exclude the list tasks
                // actions tasks form this otherwise we will always fail
                if (response.getStatusCode() == HttpStatus.SC_OK) {
                    String tasksListString = response.getBodyAsString();
                    String[] tasksLines = tasksListString.split("\n");
                    int activeTasks = 0;
                    for (String line : tasksLines) {
                        if (line.startsWith(ListTasksAction.NAME) == false) {
                            activeTasks++;
                        }
                    }
                    if (activeTasks == 0) {
                        exceptionHolder.set(null);
                        return true;
                    }
                    logger.info(activeTasks
                            + " active tasks found, waiting for them to complete:\n"
                            + tasksListString);
                }
            } catch (IOException e) {
                exceptionHolder.set(e);
            }
            return false;
        }, 10, TimeUnit.SECONDS);

        IOException exception = exceptionHolder.get();
        if (exception != null) {
            throw new IllegalStateException("Exception when waiting for pending tasks to complete",
                    exception);
        }
    }

    public MlWithSecurityIT(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    /**
     * Waits for the Security and .ml-anomalies templates to be created by the {@link SecurityTemplateService}
     * and {@link MachineLearningTemplateRegistry}.
     */
    @Before
    public void waitForIndexTemplates() throws Exception {
        String templateApi = "indices.exists_template";
        Map<String, String> securityParams = Collections.singletonMap("name", SecurityTemplateService.SECURITY_TEMPLATE_NAME);
        Map<String, String> anomaliesParams = Collections.singletonMap("name", AnomalyDetectorsIndex.jobResultsIndexPrefix());
        Map<String, String> headers = Collections.singletonMap("Authorization",
                basicAuthHeaderValue(TEST_ADMIN_USERNAME, new SecureString(TEST_ADMIN_PASSWORD.toCharArray())));

        for (Map<String, String> params : Arrays.asList(securityParams, anomaliesParams)) {
            AtomicReference<IOException> exceptionHolder = new AtomicReference<>();
            awaitBusy(() -> {
                try {
                    ClientYamlTestResponse response = getAdminExecutionContext().callApi(templateApi, params, Collections.emptyList(),
                            headers);
                    if (response.getStatusCode() == HttpStatus.SC_OK) {
                        exceptionHolder.set(null);
                        return true;
                    }
                } catch (IOException e) {
                    exceptionHolder.set(e);
                }
                return false;
            });

            IOException exception = exceptionHolder.get();
            if (exception != null) {
                throw new IllegalStateException("Exception when waiting for index template to be created", exception);
            }
        }
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() throws Exception {
        return ESClientYamlSuiteTestCase.createParameters();
    }

    protected String[] getCredentials() {
        return new String[]{"ml_admin", "changeme"};
    }

    @Override
    protected Settings restClientSettings() {
        String[] creds = getCredentials();
        String token = basicAuthHeaderValue(creds[0], new SecureString(creds[1].toCharArray()));
        return Settings.builder()
                .put(ThreadContext.PREFIX + ".Authorization", token)
                .build();
    }

    @Override
    protected Settings restAdminSettings() {
        String token = basicAuthHeaderValue(TEST_ADMIN_USERNAME, new SecureString(TEST_ADMIN_PASSWORD.toCharArray()));
        return Settings.builder()
            .put(ThreadContext.PREFIX + ".Authorization", token)
            .build();
    }
}
