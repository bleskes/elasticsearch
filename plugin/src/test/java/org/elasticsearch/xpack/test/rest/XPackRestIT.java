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
import org.elasticsearch.common.Strings;
import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;
import org.elasticsearch.test.rest.yaml.ClientYamlTestResponse;
import org.elasticsearch.xpack.ml.MachineLearningTemplateRegistry;
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
        new MlRestTestStateCleaner(logger, adminClient(), this).clearMlMetadata();
    }

    public XPackRestIT(ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    /**
     * Waits for the Security template to be created by the {@link SecurityTemplateService} and
     * the Machine Learning templates to be created by {@link MachineLearningTemplateRegistry}
     */
    @Before
    public void waitForTemplates() throws Exception {
        waitForTemplate(SecurityTemplateService.SECURITY_TEMPLATE_NAME);
        waitForTemplate(Strings.arrayToCommaDelimitedString(MachineLearningTemplateRegistry.TEMPLATE_NAMES));
    }

    private void waitForTemplate(String templateName) throws Exception {
        Map<String, String> params = singletonMap("name", templateName);
        AtomicReference<IOException> exceptionHolder = new AtomicReference<>();
        awaitBusy(() -> {
            try {
                ClientYamlTestResponse response = getAdminExecutionContext().callApi("indices.exists_template",
                        params, emptyList(), emptyMap());
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
            throw new IllegalStateException("Exception when waiting for [" + templateName + "] template to be created", exception);
        }
    }
}
