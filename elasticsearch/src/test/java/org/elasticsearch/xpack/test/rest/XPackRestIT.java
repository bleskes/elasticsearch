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

package org.elasticsearch.xpack.test.rest;

import org.apache.http.HttpStatus;
import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;
import org.elasticsearch.test.rest.yaml.ClientYamlTestResponse;
import org.elasticsearch.xpack.ml.integration.MlRestTestStateCleaner;
import org.elasticsearch.xpack.security.SecurityTemplateService;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

/** Runs rest tests against external cluster */
public class XPackRestIT extends XPackRestTestCase {

    @After
    public void clearMlState() throws IOException {
        new MlRestTestStateCleaner(logger, client(), this).clearMlMetadata();
    }

    public XPackRestIT(ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    /**
     * Waits for the Security template to be created by the {@link SecurityTemplateService}.
     */
    @Before
    public void waitForSecurityTemplate() throws Exception {
        String templateApi = "indices.exists_template";
        Map<String, String> params = singletonMap("name", SecurityTemplateService.SECURITY_TEMPLATE_NAME);

        AtomicReference<IOException> exceptionHolder = new AtomicReference<>();
        awaitBusy(() -> {
            try {
                ClientYamlTestResponse response = getAdminExecutionContext().callApi(templateApi, params, emptyList(), emptyMap());
                // We don't check the version of the template - it is the right one when testing documentation.
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
            throw new IllegalStateException("Exception when waiting for security template to be created", exception);
        }
    }
}
