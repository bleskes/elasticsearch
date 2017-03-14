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

import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;
import org.elasticsearch.test.rest.yaml.section.DoSection;
import org.elasticsearch.test.rest.yaml.section.ExecutableSection;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.either;

public class MlWithSecurityUserRoleIT extends MlWithSecurityIT {

    private final ClientYamlTestCandidate testCandidate;

    public MlWithSecurityUserRoleIT(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
        this.testCandidate = testCandidate;
    }

    @Override
    public void test() throws IOException {
        try {
            super.test();

            // We should have got here if and only if the test consisted entirely of GETs
            for (ExecutableSection section : testCandidate.getTestSection().getExecutableSections()) {
                if (section instanceof DoSection) {
                    if (((DoSection) section).getApiCallSection().getApi().startsWith("xpack.ml.get_") == false) {
                        fail("should have failed because of missing role");
                    }
                }
            }
        } catch (AssertionError ae) {
            assertThat(ae.getMessage(),
                    either(containsString("action [cluster:monitor/ml")).or(containsString("action [cluster:admin/ml")));
            assertThat(ae.getMessage(), containsString("returned [403 Forbidden]"));
            assertThat(ae.getMessage(), containsString("is unauthorized for user [ml_user]"));
        }
    }

    @Override
    protected String[] getCredentials() {
        return new String[]{"ml_user", "changeme"};
    }
}

