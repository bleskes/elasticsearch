/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2016] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.upgrades;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;
import com.carrotsearch.randomizedtesting.annotations.TimeoutSuite;
import org.apache.http.HttpStatus;
import org.apache.lucene.util.TimeUnits;
import org.elasticsearch.common.CheckedFunction;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;
import org.elasticsearch.test.rest.yaml.ClientYamlTestResponse;
import org.elasticsearch.xpack.ml.MachineLearningTemplateRegistry;
import org.elasticsearch.xpack.security.SecurityClusterClientYamlTestCase;
import org.junit.Before;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;

@TimeoutSuite(millis = 5 * TimeUnits.MINUTE) // to account for slow as hell VMs
public class UpgradeClusterClientYamlTestSuiteIT extends SecurityClusterClientYamlTestCase {

    /**
     * Waits for the Machine Learning templates to be created by {@link MachineLearningTemplateRegistry}
     */
    @Before
    public void waitForTemplates() throws Exception {
        List<String> templates = new ArrayList<>();
        templates.addAll(Arrays.asList(MachineLearningTemplateRegistry.TEMPLATE_NAMES));

        for (String template : templates) {
            awaitCallApi("indices.exists_template", singletonMap("name", template), emptyList(),
                    response -> true,
                    () -> "Exception when waiting for [" + template + "] template to be created");
        }
    }

    @Override
    protected boolean preserveIndicesUponCompletion() {
        return true;
    }

    public UpgradeClusterClientYamlTestSuiteIT(ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() throws Exception {
        return createParameters();
    }

    @Override
    protected Settings restClientSettings() {
        String token = "Basic " + Base64.getEncoder().encodeToString("elastic:changeme".getBytes(StandardCharsets.UTF_8));
        return Settings.builder()
                .put(ThreadContext.PREFIX + ".Authorization", token)
                .build();
    }

    @Override
    protected boolean randomizeContentType() {
        return false;
    }

    /**
     * Executes an API call using the admin context, waiting for it to succeed.
     */
    private void awaitCallApi(String apiName,
                              Map<String, String> params,
                              List<Map<String, Object>> bodies,
                              CheckedFunction<ClientYamlTestResponse, Boolean, IOException> success,
                              Supplier<String> error) throws Exception {

        AtomicReference<IOException> exceptionHolder = new AtomicReference<>();
        awaitBusy(() -> {
            try {
                ClientYamlTestResponse response = getAdminExecutionContext().callApi(apiName, params, bodies, Collections.emptyMap()
                );
                if (response.getStatusCode() == HttpStatus.SC_OK) {
                    exceptionHolder.set(null);
                    return success.apply(response);
                }
                return false;
            } catch (IOException e) {
                exceptionHolder.set(e);
            }
            return false;
        });

        IOException exception = exceptionHolder.get();
        if (exception != null) {
            throw new IllegalStateException(error.get(), exception);
        }
    }
}
